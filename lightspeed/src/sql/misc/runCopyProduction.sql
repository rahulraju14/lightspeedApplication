SET foreign_key_checks = off;
/*
	An example of running the copyProduction procedure.
  See copyProduction.sql for all the procedures that implement this process.

	`copy_production`(IN indb varchar(50), IN outdb varchar(50),
			IN oldProdId integer, IN newprodAid varchar(10), IN userAcctPrefix varchar(10))

	If running from within Eclipse, set database in use to "target" database,
	and run copyProduction.sql against target db to store the procedures there.

	** It is STRONGLY recommended that you backup the target database first!! **

*/

/* Be sure databases are set to utf8_general_ci collation first. */
-- alter database dbap29 charset utf8 collate utf8_general_ci;
-- alter database lsempty charset utf8 collate utf8_general_ci;

/* SAMPLE CALLS */
-- 116
-- call copy_production('source', 'target', src#, 'new pId', 'usr-pref');

-- call copy_production('dbap29', 'lsdb30', 1042, 'P116', 'B30'); -- AICP 2; 46 minutes to copy local->local (truncated audit_event prior to this!)
-- call copy_production('dbap29', 'lsdb30', 1041, 'P115', 'B30'); -- AICP 1; 68 minutes to copy local->local (lots of audit_events)

-- call copy_production('dbap29', 'lsempty', 1029, 'P3240', 'B30'); -- 1029=Thicker than blood; 3 minutes to copy local->local
-- call copy_production('dbt30', 'dbap29', 24, 'PT24', 'DH30'); -- 24=sparrow 2; 10 seconds to copy
-- call copy_production('dbt30', 'dbap29', 38, 'PB1025', 'B30'); -- 38=QA FMB; about 40 minutes to copy (30m for text element keys!)
-- call copy_production('dbt30', 'dbap29', 1, 'SYS'); -- 1 = SYSTEM production - special case code
-- call copy_production('dbap29', 'lsdb30', 1040, 'P114', 'B30'); -- 1040=Videotape test; (QA). 3 minutes to copy local->local

-- BETA (dbap29):
-- PB1041 = AICP 1
-- PB1042 = AICP 2

-- When done, show the end of the message table:
select * from temp_msg order by id desc; /* rows limited by eclipse db browser */
-- ... and the very beginning of the message table:
select * from temp_msg order by id limit 5;

/* may be run to drop temp tables created by copy procedures: */
-- call clean_up_temp();

/* to get info on charset and collation currently set:
SELECT CCSA.character_set_name FROM information_schema.`TABLES` T,
       information_schema.`COLLATION_CHARACTER_SET_APPLICABILITY` CCSA
	WHERE CCSA.collation_name = T.table_collation
	  AND T.table_schema = "dbap29"
  	AND T.table_name = "tablename";
/* *
SELECT * FROM information_schema.SCHEMATA 
	WHERE schema_name = "dbap29";
/* */
	