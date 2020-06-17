SET foreign_key_checks = on;

DROP PROCEDURE IF EXISTS `copy_record`;

DELIMITER $$  
CREATE PROCEDURE `copy_record`(IN intable varchar(100), IN outtable varchar(100), IN oldid integer, OUT newid integer) 
BEGIN 
	drop table if exists add_temp ;
	set @newid= -1;
	-- copy the record into a temporary table
	set @stmt = concat('create table add_temp select * from ', intable, ' where id =', oldid);
	prepare stmt1 from @stmt;
	execute stmt1;
	deallocate prepare stmt1;
	-- drop the id column, then add the record to the output table with a new key
	alter table add_temp drop id;
	set @stmt = concat('insert into ', outtable, ' select 0, add_temp.* from add_temp');
	prepare stmt1 from @stmt;
	execute stmt1;
	deallocate prepare stmt1;
	-- extract id of newly-inserted record
	set @stmt = concat('select max(id) from ', outtable, ' into @newid');
	prepare stmt1 from @stmt;
	execute stmt1;
	deallocate prepare stmt1;
	-- set output param to id of newly-inserted record
	set newid = @newid;
END $$
DELIMITER ;
