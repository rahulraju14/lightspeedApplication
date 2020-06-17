SET foreign_key_checks = on;
/*
This file contains all the procedures used to copy a single production
from one database to another database.  See 'runCopyProduction.sql' for
an example of executing the copy.

The copy process is fairly complex due to (a) the number of interrelated tables 
and (b) the fact that when the copied records are added to the output database 
they will be assigned new 'id' (primary key) fields.

Several temporary tables are created in the output database during the copy.
These table names all begin with "temp_".  They may be dropped after verifying
that the copy has completed successfully.

A critical table in this process is 'temp_map'.  As each record is copied to
the output database, an entry is placed in temp_map containing the name of the
table involved, the 'oldKey' -- the record's id in the input database, and the
'newKey' -- the id of the copy placed in the output database.  This information
is used by many of the procedures for determining what related records need
to be copied, and for updating the foreign key fields in the copied records.

NOTE: the following items are NOT yet copied:
	- chains of folders/documents (archived callsheets)
			...only the root folder of the production and any top-level folders under this
			are currently copied.
	- Callsheet
	- Dpr

*/
DROP PROCEDURE IF EXISTS `do_stmt`;
DROP PROCEDURE IF EXISTS `read_record`;
DROP PROCEDURE IF EXISTS `write_record`;
DROP PROCEDURE IF EXISTS `copy_record`;
DROP PROCEDURE IF EXISTS `copy_many2one_records`;
DROP PROCEDURE IF EXISTS `copy_mapped_many2one_records`;
DROP PROCEDURE IF EXISTS `copy_one2one_records`;
DROP PROCEDURE IF EXISTS `copy_mapped_one2one_records`;
DROP PROCEDURE IF EXISTS `copy_noId_records`;
DROP PROCEDURE IF EXISTS `update_users`;
DROP PROCEDURE IF EXISTS `copy_users`;
DROP PROCEDURE IF EXISTS `copy_location_interest`;
DROP PROCEDURE IF EXISTS `copy_scene_script_elements`;
DROP PROCEDURE IF EXISTS `update_acct`;
DROP PROCEDURE IF EXISTS `update_key`;
DROP PROCEDURE IF EXISTS `update_key_keep`;
DROP PROCEDURE IF EXISTS `update_all_accts`;
DROP PROCEDURE IF EXISTS `update_all_keys`;
DROP PROCEDURE IF EXISTS `clean_up_temp`;
DROP PROCEDURE IF EXISTS `copy_production`;

DELIMITER $$  
-- ***************************************************************************************************************
CREATE PROCEDURE `do_stmt`(IN statmt varchar(500), OUT retn integer)
BEGIN
	IF statmt is not null THEN
		set @retn = -1;
		set @temp_st = statmt;
		prepare stmt1 from @temp_st ;
		execute stmt1;
		set @lastInsertId = LAST_INSERT_ID();
		if @retn is not null THEN
			set retn = @retn;
			insert into temp_msg values(null, concat(curtime(), ' stmt=', statmt, '; return=', convert(@retn,char)));
		ELSE
			insert into temp_msg values(null, concat(curtime(), ' stmt=', statmt, '; return=NULL'));
		END IF;
		deallocate prepare stmt1;
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' statement is null'));
	END IF;
END $$
-- ***************************************************************************************************************
CREATE PROCEDURE `read_record`(IN intable varchar(100), IN oldid integer) 
BEGIN
	drop table if exists temp_add ;
	-- copy the record into a temporary table
	call do_stmt( concat('create table temp_add collate utf8_general_ci select * from ', intable, ' where id =', oldid), @retn);

	-- drop the id column
	alter table temp_add drop id;
END $$
-- ***************************************************************************************************************
CREATE PROCEDURE `write_record`(IN outtable varchar(100), OUT newid integer) 
BEGIN
	set @newid= -1;
	call do_stmt( concat('insert into ', outtable, ' select 0, temp_add.* from temp_add'), @retn);

	-- set output param to id of newly-inserted record
	set newid = @lastInsertId;
END $$
-- ***************************************************************************************************************
CREATE PROCEDURE `copy_record`(IN tablename varchar(50), IN indb varchar(50), IN outdb varchar(50), 
				IN oldid integer, OUT newid integer, IN updateStmt varchar(200))
BEGIN
	set @newid = -1;
	call read_record(concat(indb,'.',tablename), oldid);
	IF updateStmt is not  null THEN
		call do_stmt(concat('update temp_add ', updateStmt), @retn);
	END IF;
	call write_record(concat(outdb,'.',tablename), @newid);
	-- set output param to id of newly-inserted record
	set newid = @newid;
	insert into temp_map values ( 0, tablename, oldid, newid );
END $$
-- ***************************************************************************************************************
/*
For each record in childTbl (in inDb) where parentKeyFld = oldParentKey, copy that record
from indb to outdb, and replace the value in the parentKeyFld with newParentKey.
The old/new key pair (for childTbl) will be stored in temp_map.

Example:
	call copy_many2one_records( 'contact', 'production_id', oldProdId, newProdId, indb, outdb );

*/
CREATE PROCEDURE `copy_many2one_records`(IN childTbl varchar(50), IN parentKeyFld varchar(50),
																		IN oldParentKey varchar(20), IN newParentKey varchar(20),
																		IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare recId integer;
	Declare cnt integer;
	Declare loopCnt integer;
	Declare updtStmt varchar(200);

	-- Create statement to update parentKeyFld in new copies of child records -- argument to copy_record
	set updtStmt = concat(' set ', parentKeyFld, '=', newParentKey, ' where ', parentKeyFld, '=', oldParentKey);

	drop table if exists temp_add2 ;
	-- copy the record(s) into a temporary table
	call do_stmt( concat('create table temp_add2 collate utf8_general_ci select id from ', indb, '.', childTbl, ' where ', parentKeyFld, ' = ', oldParentKey), @retn);
	select count(*) from temp_add2 into cnt;
	IF cnt > 0 THEN
		set loopCnt = cnt;
x: 	LOOP -- copy each record in temp_add2; new ids will be saved in temp_map
			IF loopCnt <= 0 THEN
				leave x;
			END IF;
			set loopCnt = loopCnt - 1;
			select id from temp_add2 limit 1 into recId;
			call copy_record(childTbl, indb, outdb, recId, @newid, updtStmt); -- copy it and save new key in temp_map
			delete from temp_add2 where id = recId;
		END LOOP;
		insert into temp_msg values(null, concat(curtime(), ' ', cnt, ' records in ', childTbl, ' where ', parentKeyFld, ' was ', oldParentKey, ' were copied'));
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' No records exist in ', childTbl, ' where ', parentKeyFld, ' was ', oldParentKey));
	END IF;

END $$
-- ***************************************************************************************************************
/*
For each record in childTbl (in inDb) where parentKeyFld = oldParentKey, copy that record
from indb to outdb, and replace the value in the parentKeyFld with newParentKey.
There is NO old/new id key pair -- this method is for tables without an ID field, typically
those used (by Hibernate) for a many-to-many relationship mapping.

Example:
	call copy_noId_records( 'production_contract', 'production_id', oldProdId, newProdId, indb, outdb );

*/
CREATE PROCEDURE `copy_noId_records`(IN childTbl varchar(50), IN parentKeyFld varchar(50),
																		IN oldParentKey varchar(20), IN newParentKey varchar(20),
																		IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare cnt integer;
	Declare updtStmt varchar(200);

	drop table if exists temp_add2 ;
	-- copy the record(s) into a temporary table
	call do_stmt( concat('create table temp_add2 collate utf8_general_ci select * from ', indb, '.', childTbl, ' where ', parentKeyFld, ' = ', oldParentKey), @retn);

	select count(*) from temp_add2 into cnt;
	IF cnt > 0 THEN
		-- Create statement to update parentKeyFld in new copies of child records
		set updtStmt = concat('update temp_add2 set ', parentKeyFld, '=', newParentKey, ' where ', parentKeyFld, '=', oldParentKey);
		call do_stmt(updtStmt, @retn);
		call do_stmt( concat('insert into ', childTbl, ' select * from temp_add2'), @retn);
		insert into temp_msg values(null, concat(curtime(), ' ', cnt, ' records in ', childTbl, ' where ', parentKeyFld, ' was ', oldParentKey, ' were copied'));
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' No records exist in ', childTbl, ' where ', parentKeyFld, ' was ', oldParentKey));
	END IF;

END $$
-- ***************************************************************************************************************
/*
For each record "R" in our temporary key-mapping table (temp_map) that is for the table 'mappedTbl',
call the copy_many2one_records() procedure to copy all the children of the record described by "R" that 
are related via the many-to-one relation of childTbl.parentKeyFld = "R.oldKey".  
(That procedure will also take care of updating the copied records so the foreign key points to the 
new copy of the parent, previously created, whose key is R.newKey.)

Example:
	call copy_mapped_many2one_records( 'Contact', 'Start_form', 'contact_id', indb, outdb);

This would find all the Start_form records that are associated with all the Contacts that have already
been copied (as documented in the temp_map table), and copy them to the outdb.
*/
CREATE PROCEDURE `copy_mapped_many2one_records`(IN mappedTbl varchar(50),
																		IN childTbl varchar(50), IN parentKeyFld varchar(50),
																		IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare recId integer;
	Declare oldParentId integer;
	Declare newParentId integer;
	
	Declare cnt integer;
	Declare loopCnt integer;
	drop table if exists temp_add_mp ;
	-- copy the "mapping" record(s) into a temporary table
	create table temp_add_mp collate utf8_general_ci select * from temp_map where tableName = mappedTbl;
	select count(*) from temp_add_mp into cnt;
	IF cnt > 0 THEN
		set loopCnt = cnt;
x: 	LOOP -- for each record in temp_add_mp, copy its 'many2one' child records
			IF loopCnt <= 0 THEN
				leave x;
			END IF;
			set loopCnt = loopCnt - 1;
			select id from temp_add_mp limit 1 into recId;
			select oldKey from temp_add_mp limit 1 into oldParentId;
			select newKey from temp_add_mp limit 1 into newParentId;

			call copy_many2one_records( childTbl, parentKeyFld, oldParentId, newParentId, indb, outdb);
			delete from temp_add_mp where id = recId;
		END LOOP;
		insert into temp_msg values(null, concat(curtime(), ' ', cnt, ' mapped ', mappedTbl, ' records had their ', childTbl, ' children copied'));
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' No map records exist for ', mappedTbl));
	END IF;

END $$
-- ***************************************************************************************************************
/*
For each record in outdb.parentTbl where parentKeyFld = parentKey, childKeyFld is a key to childTbl. Copy that record
in childTbl from indb to outdb, and update the childKeyFld value in the parent record to have the new key.
The old/new key pair (for childTbl) will be stored in temp_map.

Example:
  call copy_one2one_records('Real_World_Element', 'production_id', newprodid, 'map_id', 'image', indb, outdb);
*/
CREATE PROCEDURE `copy_one2one_records`(IN parentTbl varchar(50), IN parentKeyFld varchar(50),
																		IN parentKey integer, IN childKeyFld varchar(50), IN childTbl varchar(50),
																		IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare recId integer;
	Declare cnt integer;
	Declare childId integer;
	drop table if exists temp_add2 ;
	call do_stmt( concat('create table temp_add2 collate utf8_general_ci select * from ', outdb, '.', parentTbl, ' where ', parentKeyFld, ' = ', parentKey), @retn);
	select count(*) from temp_add2 into cnt;

x: LOOP -- copy the child record of each parent record in temp_add2, updating reference to new id
		IF cnt = 0 THEN
			leave x; -- finished
		END IF;
		set cnt = cnt - 1;
		-- get a parent record id
		select id from temp_add2 limit 1 into recId;
		-- get the child key from that parent record
		call do_stmt( concat('select ', childKeyFld, ' from temp_add2 where id = ', recId, ' into @retn'), @retn);
		IF @retn is not null and @retn > 0 THEN
			set childId = @retn; -- key to child record
			call copy_record(childTbl, indb, outdb, childId, @newid, null); -- copy it & get new key
			insert into temp_msg values(null, concat(curtime(), ' source=', recId, '; old ', childKeyFld, '=', childId, '; new=', @newid));
			-- update parent to point to new copy of child
			call do_stmt(concat('update ', outdb, '.', parentTbl, ' set ', childKeyFld, '=', @newid, ' where id =', recId), @retn);
		END IF;
		delete from temp_add2 where id = recId; -- finished one parent
	END LOOP;

END $$
-- ***************************************************************************************************************
/*
For each record "R" in our temporary key-mapping table (temp_map) that is for the table 'mappedTbl',
call the copy_one2one_records() procedure to copy the child of the record described by "R" that 
is related via the one-to-one relation of mappedTbl.childKeyFld = childTbl.id.  
(That procedure will also take care of updating the parent record so the foreign key points to the 
new copy of the child.)

Example:
		-- Address's of Start_Form's
		call copy_mapped_one2one_records( 'Start_form', 'address_id', 'address', indb, outdb);

This would find all the Start_form records that have already been copied (as documented in the temp_map table),
and copy each one's Address record, found via the Start_form.address_id field, to outdb, and update
the Start_form.address_id field (in outdb) to have the key of the newly-inserted copy.
*/
CREATE PROCEDURE `copy_mapped_one2one_records`(IN mappedTbl varchar(50),
																		IN childKeyFld varchar(50), IN childTbl varchar(50),
																		IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare recId integer;
	Declare parentId integer;
	
	Declare cnt integer;
	Declare loopCnt integer;
	drop table if exists temp_add_mp ;
	-- copy the "mapping" record(s) into a temporary table
	create table temp_add_mp collate utf8_general_ci select * from temp_map where tableName = mappedTbl;
	select count(*) from temp_add_mp into cnt;
	IF cnt > 0 THEN
		set loopCnt = cnt;
x: 	LOOP -- for each record in temp_add_mp, copy its 'one2one' child record
			IF loopCnt <= 0 THEN
				leave x;
			END IF;
			set loopCnt = loopCnt - 1;
			select id from temp_add_mp limit 1 into recId;
			select newKey from temp_add_mp limit 1 into parentId;
			call copy_one2one_records(mappedTbl, 'id', parentId, childKeyFld, childTbl, indb, outdb);
			delete from temp_add_mp where id = recId;
		END LOOP;
		insert into temp_msg values(null, concat(curtime(), ' ', cnt, ' mapped ', mappedTbl, ' records had their ', childKeyFld, '-linked children copied'));
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' No map records exist for ', mappedTbl));
	END IF;

END $$
-- ***************************************************************************************************************
/*

*/
CREATE PROCEDURE `update_users`( IN oldProdId integer, IN userAcctPrefix varchar(10),
																IN indb varchar(50), IN outdb varchar(50))
BEGIN
	
	-- generate copies of users in source production that are not in target db
	call copy_users( oldProdId, userAcctPrefix, indb, outdb );
		-- copy Images ref'd via image.user_id
		call copy_mapped_many2one_records( 'User', 'Image', 'user_id', indb, outdb);
		-- copy Address records linked to added users
		call copy_mapped_one2one_records( 'User', 'home_address_id', 'address', indb, outdb);
		call copy_mapped_one2one_records( 'User', 'business_address_id', 'address', indb, outdb);
		call copy_mapped_one2one_records( 'User', 'agency_address_id', 'address', indb, outdb);
		call copy_mapped_one2one_records( 'User', 'loan_out_address_id', 'address', indb, outdb);
		-- update foreign keys to other users
		call update_key( 'User',		'manager_id',		'user', 	outdb );
		call update_key( 'User',		'agent_id',			'user', 	outdb );

	-- Generate old/new key mappings for Users, where contact.user in indb matches existing User in outdb,
	-- based on matching email addresses. Use 'insert ignore' to skip duplication of entries created by
	-- the 'copy_users' process.
	call do_stmt( concat(
		"insert ignore into ", outdb, ".temp_map ",
		" select null, 'user', u1.id, u2.id ",
			" from ", indb, ".user u1 inner join ", outdb, ".user u2 on u1.email_address = u2.email_address ",
			" where u1.id in ",
			"( select id from ", indb, ".user where ", 
				"id in ( select user_id from ", indb, ".contact where production_id = ", oldProdId, " ) ",
				" and email_address in (select email_address from ", outdb, ".user)",
			")" ),
		@retn );

	-- Generate a mapping of old-to-new user_account values.
	call do_stmt( concat(
		"insert into temp_map_user ",
		" select null, u1.account_number, u2.account_number ",
			" from ", indb, ".user u1, ", outdb, ".user u2, temp_map",
			" where temp_map.tableName = 'user' ",
				" and u1.id = temp_map.oldKey ",
				" and u2.id = temp_map.newKey " ),
		@retn );
				

END $$
-- ***************************************************************************************************************
/*
Copy User records from indb to outdb, where those User's have Contact records in production 'oldProdId', 
and do not have existing entries in outdb. An 'existing entry' is one that has the same email_address.
*/
CREATE PROCEDURE `copy_users`( IN oldProdId integer, IN userAcctPrefix varchar(10),
																IN indb varchar(50), IN outdb varchar(50))
BEGIN
	Declare recId integer;
	Declare cnt integer;
	Declare loopCnt integer;

	-- Generate list of id's for Users that are members of selected production but not in target
	drop table if exists temp_user_id;
	call do_stmt( concat(
		"create table temp_user_id collate utf8_general_ci ",
		" select id from ", indb, ".user where id in ",
			"( select user_id from ", indb, ".contact where production_id = ", oldProdId, " )",
			" and email_address not in (select email_address from ", outdb, ".user )" ),
		@retn );

	-- Use copy_record to add copies of User records to target db
	select count(*) from temp_user_id into cnt;
	IF cnt > 0 THEN
		set loopCnt = cnt;
u1: 	LOOP -- copy each User keyed by id in temp table; new ids will be saved in temp_map
			IF loopCnt <= 0 THEN
				leave u1;
			END IF;
			set loopCnt = loopCnt - 1;
			select id from temp_user_id limit 1 into recId;
			call copy_record('user', indb, outdb, recId, @newid, null); -- copy it and save new key in temp_map
			set @newacct = concat(userAcctPrefix, right(concat('000', @newid), 4));
			update user set account_number = @newacct, pin = null, locked_out = false where id = @newid;
			delete from temp_user_id where id = recId;
		END LOOP;
		insert into temp_msg values(null, concat(curtime(), ' ', cnt, ' User records were copied.'));
	ELSE
		insert into temp_msg values(null, concat(curtime(), ' No new User records needed to be copied.'));
	END IF;

END $$
-- ***************************************************************************************************************
/*
Copy the location_interest table from one database to another.  This is different than most other
tables because there is no 'id' column (no AUTO_INCREMENT field). Note that this relies on the 
temp_map table, which should have oldKey/newKey pairs for all the real_world_element and Point_of_interest
entries that have already been copied from the input database to the output database.

Example:
		call copy_location_interest( indb, outdb );

*/
CREATE PROCEDURE `copy_location_interest`( IN indb varchar(50), IN outdb varchar(50))

BEGIN

insert into temp_msg values(null, concat(curtime(), ' Copy location_interest entries.'));

drop table if exists temp_poi_keys;
create table temp_poi_keys collate utf8_general_ci 
	select * from temp_map where tablename = 'point_of_interest';

drop table if exists temp_loc;

call do_stmt( concat('create table temp_loc collate utf8_general_ci select * from ', indb, 
	'.location_interest where interest_id in (select oldKey from temp_poi_keys)' ), @retn);

update temp_loc
	left join temp_poi_keys on temp_poi_keys.oldKey = temp_loc.interest_id
	set interest_id = newKey;

update temp_loc
	left join temp_map on temp_map.oldKey = temp_loc.location_id and temp_map.tableName='real_world_element'
	set location_id = newKey;

insert into location_interest select * from temp_loc;

END $$
-- ***************************************************************************************************************
/*
Copy the scene_script_element table from one database to another.  This is different than most other
tables because there is no 'id' column (no AUTO_INCREMENT field).

Example:
		call copy_scene_script_elements( indb, outdb );

*/
CREATE PROCEDURE `copy_scene_script_elements`( IN indb varchar(50), IN outdb varchar(50))

BEGIN

insert into temp_msg values(null, concat(curtime(), ' Copy scene_script_elements entries.'));

drop table if exists temp_scene_keys;
create table temp_scene_keys collate utf8_general_ci 
	select oldKey from temp_map where tablename = 'scene';

drop table if exists temp_sse;

call do_stmt( concat('create table temp_sse collate utf8_general_ci select * from ', indb, 
	'.scene_script_element where scene_id in (select oldKey from temp_scene_keys)' ), @retn);

update temp_sse
	left join temp_map on temp_map.oldKey = temp_sse.scene_id and temp_map.tableName='scene'
	set scene_id = newKey;

update temp_sse
	left join temp_map on temp_map.oldKey = temp_sse.script_element_id and temp_map.tableName='script_element'
	set script_element_id = newKey;

insert into scene_script_element select * from temp_sse;

END $$

-- ***************************************************************************************************************
/*
For each record "R" in our temporary user_account mapping table (temp_map_user) that is for the table 'parentTbl',
update the user-account-number field named 'childKeyFld'.

Example:
		call update_acct( 'weekly_time_card',  'User_Account', 'lsdbtest' );

This procedure leaves key values that are not in the temp_map_user table unchanged.

Note that this process could be accomplished with a single update statement,
and that worked OK in many cases. But in a case with 12K audit_event records, it ran for 6 HOURS!
This revised procedure did the same update in less than 5 SECONDS.
*/
CREATE PROCEDURE `update_acct`(IN parentTbl varchar(50),
																IN childKeyFld varchar(50), IN outdb varchar(50))
BEGIN

insert into temp_msg values(null, concat(curtime(), ' Update Accts(b) for ', parentTbl, ', key ', childKeyFld));

-- First create a subset of "temp_map_user" that has only the mapping entries we need.
-- This doesn't take long, and speeds up the 'left join' in the next step.
drop table if exists temp_map_user_acct;

call do_stmt( concat(
	' create table temp_map_user_acct collate utf8_general_ci ',
	' select * from temp_map_user where oldAcct in ', 
		' (select distinct ', childKeyFld, ' from ', parentTbl, ')' ),
	@retn);

-- Create a temporary table identical to 'parentTbl', with 'newAcct' column added having matching value.
-- Note that 'left join' is used to keep all original rows.
drop table if exists temp_update_acct;

call do_stmt( concat( 
	'create table temp_update_acct collate utf8_general_ci ',
	'select pt.*, tmp.newAcct from ', parentTbl, ' pt ',
		' left join temp_map_user_acct tmp on tmp.oldAcct = ', outdb, '.pt.', childKeyFld ),
	@retn);

-- Update target key field with 'newAcct' values; newAcct is null if there was no match on oldAcct.
call do_stmt( concat(
	'update temp_update_acct set ', childKeyFld, ' = newAcct where newAcct is not null' ),
	@retn);

-- drop 'newAcct'; now the temp table matches the parentTbl structure
alter table temp_update_acct drop newAcct;

-- remove all rows from the parentTbl
call do_stmt( concat(
	'delete from ', outdb, '.', parentTbl ),
	@retn);

-- finally copy all rows from temp table into parentTbl
call do_stmt( concat(
	'insert into ', outdb, '.', parentTbl, ' select * from temp_update_acct'),
	@retn);

END $$

-- ***************************************************************************************************************
/*
For each record "R" in our temporary key-mapping table (temp_map) that is for the table 'parentTbl',
...

Example:
		call update_key( 'project',  'current_script_id', 'script', 'lsdbtest' );

This procedure replaces key values that are not in the temp_map table with NULL.

******************************
** NOTE ** THIS PROCEDURE RUNS VERRRRRRYYYY SLOWLY if the "parentTbl" has a large number (>10k?) rows, probably
due to the LEFT JOIN with Temp_map.  E.g., Text_element on production db has 3M records, and this step never
completed before SSH session timed out (maybe an hour?).  It needs to be re-written along the lines of
"update_acct", by creating a temporary table containing only the records from the parentTbl that actually
require updating, updating the keys in the temporary table, then replacing those rows in the parent table.
(dh 6/5/15)
******************************

*/
CREATE PROCEDURE `update_key`(IN parentTbl varchar(50),
																		IN childKeyFld varchar(50), IN childTbl varchar(50),
																		IN outdb varchar(50))
BEGIN

insert into temp_msg values(null, concat(curtime(), ' Update Keys for ', parentTbl, ', key ', childKeyFld, ' references ', childTbl));

call do_stmt( concat( 
	"update ", outdb, '.', parentTbl,
	" left join temp_map on temp_map.oldKey = ", outdb, '.', parentTbl, ".", childKeyFld, 
			" and temp_map.tableName='", childTbl, "'",
	" set ", childKeyFld, " = newKey ",
	" where ", parentTbl, ".id in ",
		"( select newKey from temp_map where tableName = '", parentTbl, "')" ),  
	@retn);

/* An example of the generated statement would be:
	update lsdbtest.project
		left join temp_map on temp_map.oldKey = lsdbtest.project.current_script_id and temp_map.tableName='script'
		set current_script_id = newKey 
		where project.id in
			(select newKey from temp_map where tableName='project')
*/

END $$

-- ***************************************************************************************************************
/*
For each record "R" in our temporary key-mapping table (temp_map) that is for the table 'parentTbl',
...

Example:
		call update_key_keep( 'project',  'current_script_id', 'script', 'lsdbtest' );

This procedure preserves key values that are not in the temp_map table.
*/
CREATE PROCEDURE `update_key_keep`(IN parentTbl varchar(50),
																		IN childKeyFld varchar(50), IN childTbl varchar(50),
																		IN outdb varchar(50))
BEGIN

insert into temp_msg values(null, concat(curtime(), ' Update Keys for ', parentTbl, ', key ', childKeyFld, ' references ', childTbl));

call do_stmt( concat( 
	"update ", outdb, '.', parentTbl,
	" inner join temp_map on temp_map.oldKey = ", outdb, '.', parentTbl, ".", childKeyFld, 
			" and temp_map.tableName='", childTbl, "'",
	" set ", childKeyFld, " = newKey ",
	" where ", parentTbl, ".id in ",
		"( select newKey from temp_map where tableName = '", parentTbl, "')" ),  
	@retn);

/* An example of the generated statement would be:
	update lsdbtest.project
		inner join temp_map on temp_map.oldKey = lsdbtest.project.current_script_id and temp_map.tableName='script'
		set current_script_id = newKey 
		where project.id in
			(select newKey from temp_map where tableName='project')
*/

END $$

-- ***************************************************************************************************************
CREATE PROCEDURE `update_all_accts`(IN outdb varchar(50))
BEGIN

	-- Update user account numbers
	call update_acct( 'Audit_Event',			'User_Account', outdb );
	call update_acct( 'Contact',					'Created_by', outdb );
	call update_acct( 'Production',				'Owning_Account', outdb );
	call update_acct( 'Time_Card_Event',	'User_Account', outdb );
	call update_acct( 'User',							'Created_by', outdb );
	call update_acct( 'weekly_time_card',	'User_Account', outdb );

	-- call update_acct( 'Start_Form_Event',  'User_Account', outdb ); -- not used yet (2.9.5151)
	-- call update_acct( 'Weekly_Batch_Event', 'User_Account', outdb ); -- not used yet (2.9.5151)

END $$

-- ***************************************************************************************************************
CREATE PROCEDURE `update_all_keys`(IN outdb varchar(50))
BEGIN

	-- Update various key fields based on the old/new key mappings generated during the copy operation
	call update_key( 'approval_anchor',		'first_approver_id',				'approver',			outdb );
	call update_key( 'approval_anchor',		'project_id',								'project',			outdb );
	call update_key( 'approver',		 			'next_approver_id',					'approver',			outdb );
	call update_key( 'audit_event',				'parent_id',								'audit_event',	outdb );
	call update_key( 'audit_event',				'related_object_id',				'weekly_time_card',	outdb );
	call update_key( 'Contact',						'user_id',									'user',					outdb );
	call update_key( 'Contact',						'assistant_id',							'Contact',			outdb );
	call update_key( 'Contact',						'default_project_id',				'project',			outdb );
	call update_key( 'Department',				'project_id',								'project',			outdb );
	call update_key( 'Department',				'contact_id',								'contact',			outdb );
	call update_key( 'production',				'approver_id',							'approver',			outdb );
	call update_key( 'production',				'default_project_id',				'project',			outdb );
	call update_key( 'production_batch',	'project_id',								'project',			outdb );
	call update_key( 'project', 					'approver_id',							'approver',			outdb );
	call update_key( 'project',						'current_script_id',				'script',				outdb );
	call update_key( 'project',						'current_stripboard_id',		'stripboard',		outdb );
	call update_key( 'project_member',		'unit_id',									'unit',					outdb );
	call update_key( 'real_link',					'script_element_id',				'script_element',outdb );
	call update_key( 'real_world_element','contact_id',								'contact',			outdb );
	call update_key( 'real_world_element','management_id',						'contact',			outdb );
	call update_key( 'start_form',				'project_id',								'project',			outdb );
	call update_key( 'start_form',				'production_batch_id',			'production_batch',outdb );
	call update_key( 'start_form',				'project_member_id',				'project_member',	outdb );
	call update_key( 'weekly_batch',			'project_id',								'project',			outdb );
	call update_key( 'weekly_time_card',	'weekly_batch_id',					'weekly_batch',	outdb );
	call update_key( 'weekly_time_card',	'approver_id',							'approver',			outdb );
	call update_key( 'weekly_time_card',	'start_form_id',						'start_form',		outdb );

	call update_key( 'Text_element',			'scene_id',									'Scene',				outdb );
	call update_key( 'Script_Element',		'responsible_party_id',			'contact',			outdb );
	call update_key( 'scene',							'Set_id',										'Script_Element',outdb );
	call update_key( 'Stripboard',				'Saved_by_id',							'User',					outdb );
	call update_key( 'Strip',							'Unit_id',									'Unit',					outdb );
	call update_key( 'Unit_Stripboard',		'Unit_id',									'Unit',					outdb );

	-- update Role keys - don't change missing ones to null, they're probably standard roles
	call update_key_keep( 'Contact',					'role_id',									'role', 				outdb );
	call update_key_keep( 'project_member',		'role_id',									'role', 				outdb );

	-- update department keys - don't change missing ones to null, they're probably standard department ids
	call update_key_keep( 'approval_anchor',	'department_id',						'department', 	outdb );
	call update_key_keep( 'role',							'department_id',						'department', 	outdb );
	call update_key_keep( 'weekly_time_card',	'department_id',						'department', 	outdb );
	
	-- same for role_id's - keep existing values if not mapped
	call update_key_keep( 'Contact',  				'role_id',									'role', 				outdb );

END $$
-- ***************************************************************************************************************
/*
	Drops all the temporary tables created by the various procedures in this file.
*/
CREATE PROCEDURE `clean_up_temp`()
BEGIN

drop table if exists temp_add;
drop table if exists temp_add2;
drop table if exists temp_add_mp;
drop table if exists temp_loc;
drop table if exists temp_map;
drop table if exists temp_map_user;
drop table if exists temp_map_user_acct;
drop table if exists temp_msg;
drop table if exists temp_poi_keys;
drop table if exists temp_scene_keys;
drop table if exists temp_sse;
drop table if exists temp_update_acct;
drop table if exists temp_user_id;

END $$

-- ***************************************************************************************************************
-- ***************************************************************************************************************
/*
This is the main procedure for copying a production from one database to another database.

6/5/15 See note on update_key procedure regarding LONG run time on a production database target.

*/

CREATE PROCEDURE `copy_production`(IN indb varchar(50), IN outdb varchar(50),
			IN oldProdId integer, IN newprodAid varchar(10), IN userAcctPrefix varchar(10))
			
Proc: BEGIN
	declare oldprodAid varchar(10);
	declare newprodAid_quoted varchar(10);
	declare recId integer;
	declare cnt integer;
	declare loopCnt integer;

	set @newid = -1;
	set @lastInsertId = -1;

	SET foreign_key_checks = off;

	DROP Table if exists	temp_msg	;		
	CREATE Table	temp_msg	(		
		Id	Integer	NOT NULL PRIMARY KEY	AUTO_INCREMENT,	
		text	varchar(1000)	
	)  charset utf8 collate utf8_general_ci;

	DROP Table if exists	temp_map	;		
	CREATE Table	temp_map	(		
		Id	Integer	NOT NULL PRIMARY KEY	AUTO_INCREMENT,	
		tableName	varchar(50),
		oldKey integer,
		newKey integer,
		unique (tableName, oldKey)
	) charset utf8 collate utf8_general_ci;

	DROP Table if exists	temp_map_user	;		
	CREATE Table	temp_map_user	(		
		Id	Integer	NOT NULL PRIMARY KEY	AUTO_INCREMENT,	
		oldAcct varchar(50),
		newAcct varchar(50)
	) charset utf8 collate utf8_general_ci;

	-- Copy the Production record
	call read_record( concat(indb,'.production'), oldProdId);
	select prod_id from temp_add into oldprodAid;
	set oldprodAid = concat("'", oldprodAid, "'");
	set newprodAid_quoted = concat("'", newprodAid, "'");

	update temp_add set prod_id = newprodAid;
	call write_record( concat(outdb,'.production'), @newid);
  set @newprodid = @newid;
	insert into temp_msg values(null, concat(curtime(), ' New production id=', convert(@newprodid,char)));
	insert into temp_map values (null, 'production', oldProdId, @newprodid);

	-- Copy referenced users not already in target, and build account mapping table
	call update_users( oldProdId, userAcctPrefix, indb, outdb );

-- copy all records that are keyed to the production
-- (a) production field -> key of child

	-- Payroll_Preference_id
	call copy_one2one_records('production', 'id', @newprodid, 'payroll_preference_id', 'Payroll_Preference', indb, outdb);

	-- Address_id
	call copy_one2one_records('production', 'id', @newprodid, 'address_id', 'address', indb, outdb);

	-- Logo_id (Image)
	call copy_one2one_records('production', 'id', @newprodid, 'logo_id', 'image', indb, outdb);

	-- repository_id (Folder)
	call copy_one2one_records('production', 'id', @newprodid, 'repository_id', 'folder', indb, outdb);
		-- get 2nd-level child folders; can't easily get others
		call copy_mapped_many2one_records( 'folder', 'folder', 'parent_id', indb, outdb);
		-- get 1st & 2nd-level child documents; can't easily get others
		call copy_mapped_many2one_records( 'folder', 'document', 'folder_id', indb, outdb);

-- (b) child record(s) point to production
	-- Project
	call copy_many2one_records( 'project', 'production_id', oldProdId, @newprodid, indb, outdb);
		-- Units mapped by unit.project_id
		call copy_mapped_many2one_records( 'project', 'unit', 'project_id', indb, outdb);
			-- schedules of unit's
			call copy_mapped_one2one_records( 'unit', 'schedule_id', 'project_schedule', indb, outdb);
		-- PayrollPreference records for each project
		call copy_one2one_records( 'project', 'production_id', @newprodid, 'payroll_preference_id', 'payroll_preference', indb, outdb);

	-- Contact
	call copy_many2one_records( 'contact', 'production_id', oldProdId, @newprodid, indb, outdb);
		-- Home_Address_id -- not used anymore
		update contact set Home_Address_id = null where production_id = @newprodid;
		-- project_member.contact_id
		call copy_mapped_many2one_records( 'Contact', 'project_member', 'contact_id', indb, outdb);
		-- Approver.contact_id
		call copy_mapped_many2one_records( 'Contact', 'Approver', 'contact_id', indb, outdb);
		-- Images ref'd via image.contact_id
		call copy_mapped_many2one_records( 'Contact', 'Image', 'contact_id', indb, outdb);
		-- Start_Form's from Contacts
		call copy_mapped_many2one_records( 'Contact', 'Start_form', 'contact_id', indb, outdb);
			-- Address's of Start_Form's
			call copy_mapped_one2one_records( 'Start_form', 'loan_out_address_id', 'address', indb, outdb);
			call copy_mapped_one2one_records( 'Start_form', 'agency_address_id', 'address', indb, outdb);
			-- StartRateSet's of Start_Form's
			call copy_mapped_one2one_records( 'Start_form', 'prod_rate_id', 'Start_Rate_Set', indb, outdb);
			call copy_mapped_one2one_records( 'Start_form', 'prep_rate_id', 'Start_Rate_Set', indb, outdb);

IF oldProdId = 1 THEN
	-- if copying SYSTEM production, stop here!
	leave Proc; 
END IF;

	-- Approval_Anchor
	call copy_many2one_records( 'Approval_Anchor', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Audit_Event
	call copy_many2one_records( 'Audit_Event', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Department
	call copy_many2one_records( 'Department', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Material
	call copy_many2one_records( 'material', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Point_of_Interest
	call copy_many2one_records( 'Point_of_Interest', 'production_id', oldProdId, @newprodid, indb, outdb);
		-- Address_id
		call copy_one2one_records('Point_of_Interest', 'production_id', @newprodid, 'address_id', 'address', indb, outdb);
		-- Images ref'd via image.Point_of_Interest_id
		call copy_mapped_many2one_records( 'Point_of_Interest', 'Image', 'Point_of_Interest_id', indb, outdb);

	-- production_batch
	call copy_many2one_records( 'production_batch', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- production_contract
	call copy_noId_records( 'production_contract', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Real_World_Element
	call copy_many2one_records( 'Real_World_Element', 'production_id', oldProdId, @newprodid, indb, outdb);
		-- Address_id
		call copy_one2one_records('Real_World_Element', 'production_id', @newprodid, 'address_id', 'address', indb, outdb);
		-- map_id
		call copy_one2one_records('Real_World_Element', 'production_id', @newprodid, 'map_id', 'image', indb, outdb);
		-- Images ref'd via image.real_world_element_id
		call copy_mapped_many2one_records( 'Real_World_Element', 'Image', 'Real_World_Element_id', indb, outdb);
		-- Real_Links mapped by Real_Link.Real_Element_id
		call copy_mapped_many2one_records( 'Real_World_Element', 'Real_Link', 'Real_Element_id', indb, outdb);

	-- Role
	call copy_many2one_records( 'Role', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Scripts mapped by Script.project_id
	call copy_mapped_many2one_records( 'project', 'Script', 'project_id', indb, outdb);
		-- Scenes ref'd via scene.script_id
		call copy_mapped_many2one_records( 'Script', 'scene', 'script_id', indb, outdb);
		-- Pages ref'd via page.script_id
		call copy_mapped_many2one_records( 'Script', 'page', 'script_id', indb, outdb);
			-- Text_element ref'd via Text_element.page_id
			call copy_mapped_many2one_records( 'page', 'Text_element', 'page_id', indb, outdb);

	-- Script_Elements mapped by ScriptElement.project_id
	call copy_mapped_many2one_records( 'project', 'Script_Element', 'project_id', indb, outdb);

	-- Stripboards mapped by Stripboard.project_id
	call copy_mapped_many2one_records( 'project', 'Stripboard', 'project_id', indb, outdb);
		-- Strips ref'd via Strip.Stripboard_id
		call copy_mapped_many2one_records( 'Stripboard', 'Strip', 'Stripboard_id', indb, outdb);
		-- Unit_Stripboards ref'd via Unit_Stripboard.Stripboard_id
		call copy_mapped_many2one_records( 'Stripboard', 'Unit_Stripboard', 'Stripboard_id', indb, outdb);

	-- location_interest - special process
	call copy_location_interest( indb, outdb );

	-- scene_script_element - special process
	call copy_scene_script_elements( indb, outdb );

	-- weekly_batch
	call copy_many2one_records( 'weekly_batch', 'production_id', oldProdId, @newprodid, indb, outdb);

	-- Weekly_Time_Card
	call copy_many2one_records( 'Weekly_Time_Card', 'prod_id', oldprodAid, newprodAid_quoted, indb, outdb);
		-- DailyTimes.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Daily_time', 'weekly_id', indb, outdb);
		-- Box_rental.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Box_rental', 'weekly_id', indb, outdb);
		-- Mileage.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Mileage', 'weekly_id', indb, outdb);
			-- mileage_line.mileage_id
			call copy_mapped_many2one_records( 'Mileage', 'mileage_line', 'mileage_id', indb, outdb);
		-- time_card_event.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'time_card_event', 'weekly_id', indb, outdb);
		-- Pay_expense.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Pay_expense', 'weekly_id', indb, outdb);
		-- Pay_breakdown.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Pay_breakdown', 'weekly_id', indb, outdb);
		-- Pay_job.weekly_id
		call copy_mapped_many2one_records( 'Weekly_Time_Card', 'Pay_job', 'weekly_id', indb, outdb);
			-- Pay_job_daily.job_id
			call copy_mapped_many2one_records( 'Pay_job', 'Pay_job_daily', 'job_id', indb, outdb);

	-- Update user account numbers
	call update_all_accts( outdb );

	-- Update various key fields based on the old/new key mappings generated during the copy operation
	call update_all_keys( outdb );
	
	update weekly_time_card set locked_by = null where id in (select newKey from temp_map where tableName = 'weekly_time_card');

	insert into temp_msg values(null, concat(curtime(), ' Production copy completed.'));
	insert into temp_msg values(null, concat(curtime(), ' Run DooD report to update script element info.'));

END $$
DELIMITER ;
