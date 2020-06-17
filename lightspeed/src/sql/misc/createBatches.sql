SET foreign_key_checks = on;
/* Create weekly batch records and put all the timecards for a week into that batch. */

/*
	First step is to create a "template" weeklyBatch record that can be used to generate
	each additional weeklyBatch record.

	After that, each time this script is run, it will find the earliest date of a timecard that 
	is not yet assigned to a batch, create a batch for that W/E date, and put all of the
	timecards from that week into the batch.
*/

set @prod = 'ftv 44 property feature';
-- select * from production where title = @prod;
set @prodid = 1054;

-- select * from project where production_id = @prodid;
-- set @projid = 201; -- ONLY NEEDED for Commercial productions!

set @lastName = '44 OC'; -- pick any user within the production
set @firstName = 'Propmaker Foreman';

/* *
-- Do this next step (sample record table) ONCE --
-- Via UI, manually create a Weekly Batch, with the w/e date entered in "tempDate".
-- Then, from that record, create a "Sample" record which will be inserted and updated each time: 
-- DROP TABLE if exists temp_weekly_batch;
set @tempDate = '2017-01-07'; -- date value in temp_weekly_batch sample record
 CREATE TABLE temp_weekly_batch
	SELECT * FROM weekly_batch WHERE date = @tempDate and production_id = @prodid 
	-- and project_id = @projid
	;
alter table temp_weekly_batch drop id;
/* */
	
-- First find the earliest date of a timecard that is not yet assigned to a batch
set @wedate = (select date_format(end_date,'%Y-%m-%d') from weekly_time_card w where
  prod_name = @prod
  and last_name = @lastName and first_name = @firstName
  and weekly_batch_id is null order by end_date limit 1);

-- Same as above, but in a different format -- for batch name use
set @batchDate = (select date_format(end_date,'WE%Y%m%d') from weekly_time_card w where
  prod_name = @prod
  and last_name = @lastName and first_name = @firstName
  and weekly_batch_id is null order by end_date limit 1);

-- select @wedate, @batchDate;
/* */
-- Create the new weeklyBatch record
INSERT INTO weekly_batch SELECT 0, temp_weekly_batch.* FROM temp_weekly_batch;

-- Then update it's date and name to match the week-ending date determined above (wedate).
-- Use project_id test only for COMMERCIAL productions.
update weekly_batch set date= str_to_date(@wedate,'%Y-%m-%d'), name= concat( 'Weekly-', @batchDate)
	where date = @tempDate and production_id = @prodid 
	-- and project_id = @projid
	;

/* */
-- Determinte the ID value of the weeklyBatch record just inserted.
-- Use project_id test only for COMMERCIAL productions.
set @wbid= (select id from weekly_batch where date = @wedate and production_id = @prodid 
	-- and project_id = @projid 
	limit 1 );

-- select @wedate, @batchDate, @wbid, str_to_date(@wedate,'%Y-%m-%d'), @prod, @prodid, @projid;

/* */
-- Then set the weeklyBatchId of the appropriate timecards, so they are in the batch.
update weekly_time_card w set weekly_batch_id = @wbid where
	prod_name = @prod
	and end_date = str_to_date(@wedate,'%Y-%m-%d')
	and weekly_batch_id is null;

/* */
-- Display the W/E date processed, the batch id, and the count of timecards assigned to the new batch.
select @wedate, @batchDate, @wbid, str_to_date(@wedate,'%Y-%m-%d'),
	count(*) from weekly_time_card where weekly_batch_id = @wbid and end_date = str_to_date(@wedate,'%Y-%m-%d');

/* *
select * from weekly_time_card w where
	prod_name = @prod
	and end_date = str_to_date(@wedate,'%Y-%m-%d')
	and weekly_batch_id is null;
/* */

