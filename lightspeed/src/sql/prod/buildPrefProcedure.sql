SET foreign_key_checks = on;

-- Create the Build_pref procedure; 

DROP PROCEDURE IF EXISTS `Build_Pref`;

DELIMITER $$  
CREATE PROCEDURE `Build_Pref`(OUT count VARCHAR(10)) 
-- This is used to create payroll_preference records for each of the
-- projects within all Commercial productions,
-- as part of upgrading a 2.2 database to 2.9/3.0.
BEGIN 
set count = 0;
x: LOOP
	-- each iteration through loop creates a payroll_preference record, 
	-- for a project that doesn't have one yet.

	-- get production.id value for some production that needs a record.
	set @prodid = (select pd.id from project pj, production pd where
	 pj.production_id = pd.id and
	 pd.type='tv_commercials' and pj.payroll_preference_id is null
	  order by pd.id, pj.id limit 1);
	
	-- get project.id value for some project that needs a record.
	set @projectid = null;
	IF @prodid is not null THEN
		set @projectid = (select pj.id from project pj, production pd where
		 pj.production_id = @prodid and
		 pd.type='tv_commercials' and pj.payroll_preference_id is null
		  order by pd.id, pj.id limit 1);
	END IF;

	IF @projectid is null THEN
		-- we're done. All commercial projects have a preference record.
		LEAVE x;
	END IF;
	drop temporary table if exists pay_pref_tmp ;
	-- copy the production's preference record
	create temporary table pay_pref_tmp select * from payroll_preference where id = @prodid;
	alter table pay_pref_tmp drop id;
	-- then insert it back into table as a new entity
	insert into payroll_preference select 0, pay_pref_tmp.* from pay_pref_tmp;
	-- get id of newly-inserted record
	set @newid = (select max(id) from payroll_preference);
	-- and update the foreign key in the project to point to it.
	update project set payroll_preference_id = @newid where id = @projectid;
	set count = count + 1;
END LOOP;

END $$
DELIMITER ;

/* */
	