SET foreign_key_checks = off;
/* */
/* A place to consolidate SQL statements that can check validity of table data, beyond the rules
  enforced by MySql due to foreign keys, not-null specifications, etc.  The SQL statements here
	can check for business logic requirements, such as a start-date must precede an end-date. */
/* */
set @bad = 0;  -- if this is non-zero at the end, something failed!

/* PAY_RATE: salaries */
-- find (probably) invalid pay_rate entries; all selects should result in count of 0
	-- Some of these values may need to be adjusted as salaries increase! :)

set @bad = (select count(id) from pay_rate where start_date > end_date);
set @bad = @bad + (select count(id) from pay_rate where hourly_rate < 0 or daily_rate < 0 or weekly_rate < 0 or guar_hours < 0);
set @bad = @bad + (select count(id) from pay_rate where locality not in ('a','s','d'));

set @bad = @bad + (select count(id) from pay_rate where hourly_rate < 10 or hourly_rate > 200);
set @bad = @bad + (select count(id) from pay_rate where (daily_rate < 80 or daily_rate > 1500) and contract_code not like '%DGA%');
set @bad = @bad + (select count(id) from pay_rate where (weekly_rate < 400 or weekly_rate > 6000) and contract_code not like '%DGA%');

set @bad = @bad + (select count(id) from pay_rate 
	where hourly_rate > daily_rate or daily_rate > weekly_rate or hourly_rate > weekly_rate);

/* PAY_RATE: SCHEDULE CODES */
-- Check for unknown/unexpected schedule codes
set @bad = @bad + (select count(id) from pay_rate 
	where schedule not in 
	('00', '01', '10', '40', '43','44', '45', '48', '49', '54', '55',  '56', '57',  '60', '64', '70', ' ', 'X1', 'XC')
		and contract_code <> 'N');
/* */

/* TIMECARDS */
set @bad = @bad + (select count(id) from weekly_time_card w where status = 'open' and id  in
	(select weekly_id from time_card_event where type='submit'));

set @bad = @bad + (select count(id) from weekly_time_card w where status <> 'open' and id not in
	(select weekly_id from time_card_event where type='submit'));

-- verify all PayJob's have 7 PayJobDaily records
set @bad = @bad + (select count(job_id) from (select job_id, count(*) cnt from pay_job_daily group by job_id) pjds where cnt <> 7);

-- verify all WeeklyTimecard's have 7 DailyTime records
set @bad = @bad + (select count(weekly_id) from (select weekly_id, count(*) cnt from daily_time group by weekly_id) dts where cnt <> 7);

/* CONTRACT RULES */

set @bad = @bad + (select count(*) from contract_rule where use_rule_key not in (
	select rule_key from weekly_rule 
	union select rule_key from call_back_rule 
	union select rule_key from golden_rule 
	union select rule_key from guarantee_rule 
	union select rule_key from holiday_rule 
	union select rule_key from holiday_list_rule 
	union select rule_key from mpv_rule 
	union select rule_key from nt_premium_rule  
	union select rule_key from on_call_rule 
	union select rule_key from overtime_rule 
	union select rule_key from rest_rule 
	union select rule_key from special_rule
	)
	and use_rule_key not in (select concat('not ',rule_key) from special_rule) );

set @bad = @bad + (select count(*) from contract_rule cr where cr.union_key in (
select distinct cr.union_key from contract_rule cr where union_key not in
	(select u.union_key from unions u)
	and cr.union_key <> 'N_A'));

-- Check schedule values in Contract Rules to make sure they match PayRate entries.

set @bad = @bad + (select count(*) from contract_rule cr 
	where cr.union_key <> "nonu" and schedule <> "N_A" and occ_code <> "Ver" and
		schedule not in (select distinct contract_schedule from pay_rate));

set @bad = @bad + (select count(*) from Contract_Rule where 
	day_number not in ("N_A","15","16","56","67","5","6","7"));


-- Show final count of bad records:
select @bad;

-- since we don't have all contracts coded yet, these don't come out zero; but should in the end!
/* *
select Contract_Code, c.* from contract c where contract_code not in 
	(select distinct contract_code from occupation);
select  distinct Contract_Code from pay_rate p where contract_code not in 
	(select distinct contract_code from contract);

/* */

/* *
-- The equivalent SELECT statements follow; run them if the @bad count is non-zero

-- PAY RATES

(select * from pay_rate where start_date > end_date);
(select * from pay_rate where hourly_rate < 0 or daily_rate < 0 or weekly_rate < 0 or guar_hours < 0);
(select * from pay_rate where locality not in ('a','s','d'));

(select * from pay_rate where hourly_rate < 10 or hourly_rate > 200);
(select * from pay_rate where (daily_rate < 80 or daily_rate > 1500) and contract_code not like '%DGA%');
(select * from pay_rate where (weekly_rate < 400 or weekly_rate > 6000) and contract_code not like '%DGA%');

(select * from pay_rate 
	where hourly_rate > daily_rate or daily_rate > weekly_rate or hourly_rate > weekly_rate);

(select *, (end_date - start_date) from pay_rate 
	where schedule not in 
		('00', '01', '10', '12', '40', '43','44', '45', '48', '49', '54', '55',  '56', '57',  '60', '64', '70', ' ', 'x1', 'XC')
		and contract_code <> 'N');

-- TIMECARDS
/* *
select * from weekly_time_card w where status = 'open' and id in
	(select weekly_id from time_card_event where type='submit');

select * from weekly_time_card w where status <> 'open' and id not in
	(select weekly_id from time_card_event where type='submit');

select * from (select job_id, count(*) cnt from pay_job_daily group by job_id) pjds where cnt <> 7;
/* *

-- Contract Rules

select use_rule_key, cr.* from contract_rule cr where use_rule_key not in (
select rule_key from weekly_rule 
union select rule_key from call_back_rule 
union select rule_key from golden_rule 
union select rule_key from guarantee_rule 
union select rule_key from holiday_rule 
union select rule_key from holiday_list_rule 
union select rule_key from mpv_rule 
union select rule_key from nt_premium_rule  
union select rule_key from on_call_rule 
union select rule_key from overtime_rule 
union select rule_key from rest_rule 
union select rule_key from special_rule
)
and use_rule_key not in (select concat('not ',rule_key) from special_rule);

select cr.union_key, cr.* from contract_rule cr where cr.union_key in (
select distinct cr.union_key from contract_rule cr where union_key not in
	(select u.union_key from unions u)
	and cr.union_key <> 'N_A');

-- Check schedule values in Contract Rules to make sure they match PayRate entries.

select schedule, cr.* from contract_rule cr
	where cr.union_key <> "nonu" and schedule <> "N_A" and occ_code <> "Ver" and
		schedule not in (select distinct contract_schedule from pay_rate);

select day_number, cr.* from Contract_Rule cr where 
	day_number not in ("N_A","15","16","56","67","5","6","7");

/**/
