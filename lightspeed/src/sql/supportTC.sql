SET foreign_key_checks = on;

/* Timecard support SQL queries; gives detailed records for one timecard - payJobs, payBreakdowns, etc. */

 set @mail =  ''; -- 
 set @date = '2018-10-15' ;
 set @pid = (select id from production where title like '%%' order by id desc limit 1);
 -- set @pid = 2093;
 
 set @uid = ( select id from user where email_address like @mail limit 1) ;
 set @acct = ( select account_number from user where email_address like @mail limit 1) ;

/* SET id for time-card with specified date and user */
set @wtc_id = (select id from weekly_time_card where user_account = @acct and end_date = @date order by id desc limit 1);

/* SET start-form id */
set @sf_id = (select start_form_id from weekly_time_card where id = @wtc_id) ;

/* Set contact id: */
set @contact_id = (select contact_id from start_form where id = @sf_id);

/* Set production.id: */
set @pid = (select production_id from contact where id = @contact_id);

	set @title = (select production.title from production where id = @pid) ;
	set @prodid = (select prod_id from production where id = @pid) ;

/* 1: User */
	select * from user where email_address like @mail ;

/* 2: dated timecards for this user */
	select * from weekly_time_card where user_account = @acct and end_date = @date order by occupation ;

/* 3: ALL START FORMS */
-- All StartForms from the production whose id is set at the top
select sf.* from start_form sf, contact c where  c.user_id = @uid and sf.contact_id = c.id;

/* 4: StartForm for the selected timecard */
	select * from start_form where id = @sf_id ;

/* 5: all timecards for this user */
	select * from weekly_time_card where user_account = @acct order by End_date desc, occupation ;

/* 6: Time_card_events */
	select * from time_card_event tce where  tce.weekly_id = @wtc_id order by date ;

/* 7: DailyTime instances for this timecard */
	select * from daily_time where weekly_id = @wtc_id;

/* 8: PayJob instances */
	select * from pay_job where weekly_id = @wtc_id;

/* 9: pay_job_daily's for the job: */
	set @pj_id = (select id from pay_job where weekly_id = @wtc_id limit 1);
	-- select * from pay_job_daily where job_id = @pj_id;
	select * from pay_job_daily where job_id in (select id from pay_job where weekly_id = @wtc_id);

/* 10: PayExpense instances */
	select * from pay_expense where weekly_id = @wtc_id;

/* 11: PayBreakdown instances */
	select * from pay_breakdown where weekly_id = @wtc_id;

/* 12: PayBreakdown instances */
	select * from pay_breakdown_daily where weekly_id = @wtc_id;

/* 13: AuditEvent instances */
	select * from Audit_Event where related_object_id = @wtc_id order by id desc;

/* 14: TimecardChangeEvent instances */
	select * from Timecard_Change_Event where timecard_id = @wtc_id order by id desc;

/* 15: All StartForms from the production whose id is set at the top */
	select * from start_form where prod_title = @title order by last_name, first_name;

/* 16:  All timecards for this week from given production. */
	select * from weekly_time_card where prod_id = @prodid and end_date = @date
		order by last_name, first_name;

/* 17: */
	select * from approval_anchor where production_id = @pid order by department_id;

/* 18: */
	select * from contact where production_id = @pid;

/* 19: selected ids */
	select @pid, @date, @uid, @contact_id, @sf_id, @wtc_id
 
/******
UPDATES

-- update weekly_time_card set start_form_id = XXXX where id = @wtc_id and user_account = @acct and end_date = @date;
-- update user set password = null where id=@uid limit 1;
-- update weekly_time_card set time_sent = null where weekly_batch_id = 77;
-- update weekly_batch set sent = null where id = 77;
-- update start_form set production_batch_id = null where prod_title = @title;
-- update start_form set prod_company = 'Dwight Studios, LLC' where prod_title = @title and (prod_company is null or prod_company = '');
-- update start_form set work_location = 'Anaheim' where prod_title = @title and  work_location is null;
-- update weekly_time_card set approver_id = null where id =  7643 and approver_id = 118;
-- update start_form set work_state = 'CA' where prod_title = @title and work_state is null;

-- update all major account codes (in StartForm and StartRateSet):

update start_form s, start_rate_set r set r.hourly_rate_acct_major = '038',
	 r.hourly_rate_acct_major = '038',
	 r.daily_rate_acct_major = '038',
	 r.weekly_rate_acct_major = '038',
	 r.x15_rate_over_acct_major = '038',
	 r.x20_rate_over_acct_major = '038',
	 r.day6_rate_acct_major = '038',
	 r.day7_rate_acct_major = '038',
	 r.idle_day6_rate_acct_major = '038',
	 r.idle_day7_rate_acct_major = '038',
	 s.box_rental_acct_major = '038',
	 s.car_allow_acct_major = '038',
	 s.meal_allow_acct_major = '038',
	 s.meal_penalty_acct_major = '038',
	 s.perdiem_tx_acct_major = '038',
	 s.perdiem_ntx_acct_major = '038',
	 s.perdiem_adv_acct_major = '038',
	 s.meal_money_acct_major = '038',
	 s.meal_money_adv_acct_major = '038',
	 s.fringe_acct_major = '038'
 where s.prod_title = @title and
 ( s.prod_rate_id = r.id or s.prep_rate_id = r.id)
  
  
  **********/
