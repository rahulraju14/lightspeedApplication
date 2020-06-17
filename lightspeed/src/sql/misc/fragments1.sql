SET foreign_key_checks = off;

-- -----------------------------------------------------------------------------------------------
/* 1/8/15 turn off email stuff when i'm running a copy of beta or app locally!

update production set prod_id = 'TPRD' where id = 1;

update payroll_preference set batch_email_address = 'dharm@theteamcompanies.com';
update payroll_preference set onboard_email_address = 'dharm@theteamcompanies.com';

-- change "TEAM" payroll service to "TEAM Test"
update payroll_preference set payroll_service_id = 11 where  payroll_service_id = 4;

update contact set notify_by_email = 0, send_callsheet=0, send_dpr=0, send_directions=0, send_advance_script=0 
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';

update contact set email_address = concat(email_address,".z")
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';

update user set email_address = concat(email_address,".z")
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';

update user set pin = null
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';

/* Set all passwords to a common value... *
update user set password = null where email_address = 'af@ls.test.ttc';

set @pw = (select password from user where email_address = 'af@ls.test.ttc');

update user set password = @pw
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';
/*
update user set password = null; -- enable login to any email address
/*
update user set status = 'DELETED' 
	where email_address not like '%@harmf%' and email_address not like '%@ls.test.ttc' and email_address not like '%@lightspeedeps%'
							 and email_address not like '%@theteamcompanies%';
-- -----------------------------------------------------------------------------------------------

/* 10/11/19 mismatched mailing addresses *

-- select count(*) from (
Select p.Title, af.Addr_Line1 w4addr, af.Addr_Line2, af.city, af.state, aum.addr_line1 userAddr, aum.addr_line2, aum.city, aum.state, cd.last_sent, u.last_name, u.first_name, u.email_address, u.account_number
	from user u, contact c, contact_document cd, form_w4 f, address af, address aum, 
		weekly_time_card w, start_form sf,
		production p, payroll_preference pp, payroll_service ps
	where 
    cd.Contact_Id = c.id and
    w.Start_Form_Id = sf.id and
    sf.Contact_Id = c.id and
    w.end_date > '2019-06-01' and
    c.Production_Id = p.id and
    p.Payroll_Preference_Id = pp.id and
    pp.Payroll_Service_Id = ps.id and
    ps.Team_Payroll and
    c.User_Id = u.id and
    u.Status <> 'deleted' and
    u.Email_Address not like '%@theteamcompanies.com' and
    cd.Last_Sent <= '2019-01-01' AND cd.Form_Type = 'W4' AND
    cd.Related_Form_Id = f.id and
    u.Mailing_Address_Id = aum.id and
    f.Address_Id = af.id and
    (replace(replace(trim(af.Addr_Line1),'  ',' '),'.','') <> replace(replace(trim(aum.Addr_Line1),'  ',' '),'.',''))
    
    group by u.id
    order by cd.last_sent desc, u.Last_Name, u.First_Name

 --   ) tab;

/* 10/14/19 -- mismatched addresses w/ SSN, etc.

set @akey = '<encrypt key>';

Select p.Title, af.Addr_Line1 w4addr, af.Addr_Line2, af.city, af.state, aum.addr_line1 userAddr, aum.addr_line2, aum.city, aum.state, 
		concat('''',cd.last_sent,''''), 
		u.last_name, u.first_name, u.email_address, u.account_number,
		concat('''',cast(AES_DECRYPT(u.Social_Security_aes, @akey) AS char)) as ssn,
		u.business_phone, u.cell_phone, u.home_phone
	from user u, contact c, contact_document cd, form_w4 f, address af, address aum, 
		weekly_time_card w, start_form sf,
		production p, payroll_preference pp, payroll_service ps
	where
	    cd.Contact_Id = c.id and
	    w.Start_Form_Id = sf.id and
	    sf.Contact_Id = c.id and
	    w.end_date > '2019-06-01' and
	    c.Production_Id = p.id and
	    p.Payroll_Preference_Id = pp.id and
	    pp.Payroll_Service_Id = ps.id and
	    ps.Team_Payroll and
	    c.User_Id = u.id and
	    u.Status <> 'deleted' and
	    u.Email_Address not like '%@theteamcompanies.com' and
	    cd.Last_Sent <= '2019-01-01' AND cd.Form_Type = 'W4' AND
	    cd.Related_Form_Id = f.id and
	    u.Mailing_Address_Id = aum.id and
	    f.Address_Id = af.id and
	    (replace(replace(trim(af.Addr_Line1),'  ',' '),'.','') <> replace(replace(trim(aum.Addr_Line1),'  ',' '),'.',''))
    group by u.id
    order by cd.last_sent desc, u.Last_Name, u.First_Name

/* 12/5/18 CREW WHO WORKED - based on TIMECARDS - for a studio (or production) - for Evolution *

select w.last_name, w.first_name, u.email_address, p.Title, min(w.end_date) as 'First WE', max(w.end_date) as 'Last WE' 
	from user u, weekly_time_card w , production p
where
	w.updated is not null and w.prod_id = p.prod_id and
	w.Grand_Total is not null and w.Grand_Total <> 0 and -- filter timecards not calculated
	(p.Studio like 'EFT%' or p.Studio like 'evol%') and -- production/studio filter
	w.User_Account = u.Account_Number and 
	w.end_date between '2017-01-01' and '2018-12-31'	-- Date filter
group by w.User_Account, w.Prod_Id						-- grouping gives one row per production per employee
order by w.end_date, last_name, first_name;

/* 1/19/18  UPDATE START_FORM - for NFL films - changed effective_start_date

select * from start_form sf 
	where prod_company = 'nfl films' and sf.hire_date ='2018-01-19' and effective_start_date = '2018-01-21'
	and job_name = 'Games 2017 - Wk 20 /I61025';

-- update start_form sf set sf.effective_start_date = '2018-01-20'
	where prod_company = 'nfl films' and sf.hire_date ='2018-01-19' and effective_start_date = '2018-01-21'
	and job_name = 'Games 2017 - Wk 20 /I61025' ;

/* 1/9/2019 Review of mileage rates paid *
select p.rate, end_date, w.prod_co, w.job_name from weekly_time_card w, pay_expense p where p.category = 'Mileage - NonTax' and
 p.weekly_id = w.id and end_date > '2018-11-01'
 order by rate, prod_co;

/* */
/* 10/9/19 Find all productions that contain a particular document name:

select * from folder f1, folder f2, production p, document d where d.name = 'Screening Consent.pdf' and
	d.folder_id = f1.id and
	f1.parent_id = f2.id and
	p.repository_id = f2.id;

/* */
/* 3/27/18  *******  EMERGENCY CONTACT INFO from Start Forms for a client (production studio name) **********

select sf.last_name, sf.first_name, 
sf.emergency_name, sf.emergency_phone, sf.emergency_relation, p.title
-- cd.*, c.id,  u.* 
from production p, user u, contact c 
join contact_document cd on cd.contact_id = c.id
join start_form sf on cd.related_form_id = sf.id
where 
studio like 'lion tv%'
and c.production_id = p.id
and c.user_id = u.id
and u.email_address not like '%@lightspeedeps.com'
and cd.form_type = 'start'
order by u.last_name, u.first_name, p.title;

/* 3/27/18  *******  birthdate from I-9 (I9) for a client (production studio name) **********

select u.last_name, u.first_name, 
date_format(f.date_of_birth, '%M %d'),
date_format(f.date_of_birth, '%m-%d'),
p.title
-- cd.*, c.id,  u.* 
from production p, user u, contact c 
join contact_document cd on cd.contact_id = c.id
join form_i9 f on cd.related_form_id = f.id
where 
studio like 'lion tv%'
and c.production_id = p.id
and c.user_id = u.id
and u.email_address not like '%@lightspeedeps.com'
and cd.form_type = 'i9'
order by u.last_name, u.first_name, p.title;

/* 2/3/17 ************ Code to show all the DEPT APPROVERs on all APPROVAL PATHS ************

select p.title as Production, ap.name as PathName, d.name as Dept, c.display_name as Approver, aa.* 
from approval_path_anchor aa, approval_path ap, production p, department d, approver a, contact c
where aa.production_id = p.id 
and aa.department_id = d.id
and aa.approval_path_id = ap.id
and aa.first_approver_id = a.id
and a.contact_id = c.id
order by p.title, ap.name, d.name


/*  CODE to get all AGENCY / AGENT names, addresses from Start Forms ********* *
-- 3/13/17
select first_name, last_name, agency_name, ad.* from address ad, start_form sf
	where sf.agency_address_id = ad.id
	and sf.agent_rep = 1
	and sf.job_name = '17fb/scr'
	order by last_name, first_name;

/* */
/* 3/8/16 ************ Code to DELETE a whole bunch of TIMECARDS  **************

-- CREATE table with IDs of timecards to be deleted --
set @pid = 'PB1144';
create table temp_del_tc
	select id from weekly_time_card  where end_date = '2017-03-04' and prod_id = @pid;

-- delete Pay breakdown & expense entries
delete from pay_breakdown where weekly_id in (select id from temp_del_tc) ;
delete from pay_expense where weekly_id in (select id from temp_del_tc) ;
delete from box_rental where weekly_id in (select id from temp_del_tc) ;

-- create list of PayJob ids
create table temp_del_pj
	select id from pay_job where weekly_id in (select id from temp_del_tc);

delete from pay_job_daily where job_id in (select id from temp_del_pj) ;
delete from pay_job where weekly_id in (select id from temp_del_tc) ;
-- Delete audit (trace) in 2 steps
delete from audit_event where related_object_id in (select id from temp_del_tc) 
	and parent_id is not null;
delete from audit_event where related_object_id in (select id from temp_del_tc) ;

-- daily times and main records are last...
delete from daily_time where weekly_id in (select id from temp_del_tc);

delete from weekly_time_card  where id in (select id from temp_del_tc);

Drop table if exists temp_del_tc;
drop table if exists temp_del_pj;
/* ---- End of delete timecards ---- */

/* 4/12/17 Code to INSERT PROJECT_MEMBER entries into a production *
	insert into project_member(
		`Unit_Id`,
		`employment_id` ) 
	select u.id, er.id from employment er, contact c, unit u, production p, project pj
	where er.contact_id = c.id and
		c.production_id = p.id and
		u.project_id = pj.id and
		pj.production_id = p.id and
		p.id = 1193 ;
/* */
/* 2/10/17 find all ASSIGNED CONTRACTS vs PRODUCTIONS

select c.name, p.title  from production_contract pc, contract c, production p
where pc.production_id = p.id
and pc.contract_key = c.contract_key
order by c.name, p.title;
*/
/* 8/2016 Find all productions where a user is an APPROVER: *

select distinct a.*, p.title from approver a,  user u, contact c, production p 
where a.contact_id = c.id
and u.account_number = 'A0110518' -- need to lookup account number first
and c.user_id = u.id
and c.production_id = p.id
order by p.title;


/*
-- Get all users who are Prod Data Admin or Financial Data Admin on Productions with at least one "production approver" for timecards.

/* 12/27/17 -- added nesting to get most recent production title for each email address; omit inactive contacts *
select  display_name, last_name, first_name, email_address, title
from (
	select u.display_name, u.last_name, u.first_name, u.email_address, p.title from user u,  contact c, employment e, production p
	where
	 c.user_id = u.id and
	 c.production_id = p.id and
	 e.contact_id = c.id and
	 e.role_id in (422,423) and -- specific role ids for Financial and Prod data admins
	 p.status = 'active' and
	 c.status = 'ACCEPTED' and -- omit Deleted and Pending
	 u.status = 'REGISTERED' and -- possibly superfluous, can't hurt
	 u.email_address not like '%lightspeedeps.com' and
	 u.email_address not like '%@ls.test.ttc' and
	 u.email_address not like '%@theteamcompanies.com' and
	 u.email_address not like '%@media-services.com' and
	 p.approver_id is not null
	 group by u.id, p.id -- drop "p.id" to get unique emails
	order by p.id desc
	 ) tab1
 group by email_address
 order by last_name, first_name;

/* 9/2016 
--  3.1 version:
select u.display_name, u.last_name, u.first_name, u.email_address, p.title from user u,  contact c, employment e, production p
where
 c.user_id = u.id and
 c.production_id = p.id and
 e.contact_id = c.id and
 e.role_id in (422,423) and
 u.email_address not like '%lightspeedeps.com' and
 u.email_address not like '%@ls.test.ttc' and
 p.approver_id is not null
 group by u.id, p.id -- drop "p.id" to get unique emails
 order by u.last_name, u.first_name
 ;

/* 2/6/18  Crew report - names, occupation, address, phone - for a Production *

SELECT   cont.id , cont.assistant_Id as assistant, dept.name as department_name, 
	usr.first_name, usr.last_name, role.name as Role, cont.email_address, 
	addr.addr_line1, addr.addr_line2, addr.city, addr.state, addr.zip, 
	addr2.addr_line1, addr2.addr_line2, addr2.city, addr2.state, addr2.zip, 
	cont.home_phone, cont.business_phone, cont.cell_phone,  
	case cont.minor when 0 then ' ' else 'Minor' end as Minor 
FROM unit u, employment emp, contact as cont ,
	  project as proj, department as dept, role as role, project_member as prom, 
	  user as usr left join address as addr on usr.Home_Address_Id = addr.id 
	  left join address as addr2 on usr.mailing_Address_Id = addr.id 
WHERE  cont.user_id = usr.id and emp.role_id=role.id
	and prom.employment_id = emp.id and emp.contact_id = cont.id and emp.project_id = proj.id
	and proj.id = u.project_id and prom.unit_id = u.id and role.department_id=dept.id 
	 and dept.name <> 'LS Admin'
	 and cont.production_id = 5913
 
GROUP BY cont.id
order by last_name, first_name;
/* */

/* GENERIC "payroll report" of ALL timecards for one production, with most commonly used fields *

SELECT w.end_date, last_name, first_name, prod_name, dept_name, occupation, employee_rate_type, rate, hourly_rate, daily_rate, weekly_rate, guar_hours, state_worked, city_worked, grand_total
FROM weekly_time_card w 
WHERE w.updated is not null and  w.prod_id = 'P6391' 
-- and w.end_date between '2017-10-21' and '2017-10-21'  
order by  w.last_name, w.first_name, w.Account_Major, w.Account_Dtl, Account_Set, w.Occupation, w.end_date desc ;

/* "payroll report" of timecards not in final approval state - possibly not paid? (for NFL 9/18/18) *
SELECT w.end_date, w.status, last_name, first_name, 
	occupation, employee_rate_type,  hourly_rate, daily_rate, guar_hours, city_worked, state_worked, grand_total, w.Time_Sent, b.name as 'Batch Name', b.Sent
-- prod_name, dept_name, weekly_rate,
FROM weekly_time_card w left join weekly_batch b on weekly_batch_id = b.id
WHERE w.updated is not null and  w.prod_id = 'P5103' 
and w.status not in ('open', 'approved', 'rejected')
and Approver_Id is not null
and w.end_date between '2015-08-01' and '2018-08-01'  
order by  w.end_date, w.last_name, w.first_name, w.Account_Major, w.Account_Dtl, Account_Set, w.Occupation desc ;

/* *
-- 9/11/18 -- Payroll report of days NOT WORKED (non-work day types) -- for Sunset productions
SELECT w.end_date, d.date, d.day_type, last_name, first_name, prod_name, dept_name, occupation, employee_rate_type, rate, hourly_rate, daily_rate, weekly_rate, guar_hours, state_worked, city_worked, grand_total
FROM weekly_time_card w, daily_time d
WHERE d.weekly_id = w.id and d.day_type <> 'wk' and d.day_type <> 'hp' and d.day_type <> 'hw' and d.day_type <> 'g1' and d.day_type is not null and
w.grand_total is not null and w.grand_total > 0 and
w.updated is not null and  w.prod_id = 'P5388'  and w.end_date between '2018-01-01' and '2018-10-21'  
order by w.last_name, w.first_name, d.date ;
/* *
P5388 = sunset productions
/* */
-- 9/11/18 -- Payroll report of days NOT WORKED (non-work day types) -- for Sunset productions
SELECT w.end_date, d.date, d.day_type, last_name, first_name, prod_name, dept_name, occupation, employee_rate_type, rate, hourly_rate, daily_rate, weekly_rate, guar_hours, state_worked, city_worked, grand_total
FROM weekly_time_card w, daily_time d
WHERE d.weekly_id = w.id and d.day_type <> 'wk' and d.day_type <> 'hp' and d.day_type <> 'hw' and d.day_type <> 'g1' and d.day_type is not null and
w.grand_total is not null and w.grand_total > 0 and
w.updated is not null and  w.prod_id = 'P5388'  and w.end_date between '2018-01-01' and '2018-10-21'  
order by w.last_name, w.first_name, d.date ;
/* *
P5388 = sunset productions
/* */
/* */
/* 7/2016  PRODUCTIONS WITH TIMECARDS (by date) (by status) report *

select distinct studio, title, concat(u.first_name, ' ', u.last_name), u.email_address from production p, user u 
where u.account_number = p.owning_account
and prod_id in (
select distinct prod_id from weekly_time_card w 
	where w.end_date > '2016-06-01' -- include this to limit by timecard date
	and w.status <> 'OPEN'
) order by studio, title, first_name, last_name, title;

/*
-- UPDATE all ... for a single production (all projects - commercial)
update payroll_preference pp, project pj, production p 
		set xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
	where pj.payroll_preference_id = pp.id and pj.production_id = p.id
	and pp.use_onboard_email = true
	and p.id = 5990;

/* UPDATE TEAM EOR for any production - we use production's payroll pref for EOR setting: *
select * from production where  title = 'Latitude 45 Catering';

update payroll_preference pp, production p set pp.team_eor = 'TEAM_D' 
	where p.payroll_preference_id = pp.id
	and pp.team_eor is null
	and p.id = 7118;

/*

select pp.team_eor, p.title, p.studio, pj.title, p.*, pp.*, pj.* from payroll_preference pp, project pj, production p 
	where pj.production_id = p.id and
	((pj.payroll_preference_id = pp.id)
	or p.payroll_preference_id = pp.id and p.type = 'FEATURE_FILM')
	-- and pp.use_onboard_email = true
	-- and pp.team_eor is not null
	and p.id = 5990
	order by p.id, pj.id;
;

/*
-- UPDATE all ... for a single production (all projects - commercial)
update payroll_preference pp, project pj, production p 
		set pp.team_eor = 'TEAM_D' 
	where pj.production_id = p.id and
	((pj.payroll_preference_id = pp.id)
	or p.payroll_preference_id = pp.id and p.type = 'FEATURE_FILM')
	-- and pp.use_onboard_email = true
	and pp.team_eor is null
	and p.id = 5990 ;


/*
Superfly productions:
6830
6168
7099
festivol: 5990

/* SELECT all PAYROLL PREFERENCES for a commercial production: *
select * from payroll_preference pp, project pj, production p 
	where pj.payroll_preference_id = pp.id and pj.production_id = p.id
	-- and pp.use_onboard_email = true
	and p.id = 5990;

/* SELECT the one PAYROLL PREFERENCE for a feature production: *
select p.title, * from payroll_preference pp, production p 
	where p.payroll_preference_id = pp.id
	and p.id = 6830;

/*
select * from payroll_preference pp, project pj, production p 
	where pj.payroll_preference_id = pp.id and pj.production_id = p.id
	and pp.use_onboard_email = true
	and p.id = 5990;

/* check for MILEAGE RATE
select * from production p, payroll_Preference pp where mileage_rate = 0.54
and type='TV_COMMERCIALS' and status = 'active' and p.payroll_preference_id = pp.id;

/* UPDATE MILEAGE rate for all Production-related Payroll Preference records (source for new projects)

update production p, payroll_Preference pp set mileage_rate = 0.545 where mileage_rate = 0.535
and type='TV_COMMERCIALS' and status = 'active' and p.payroll_preference_id = pp.id;
/* *
select * from production p, project j, payroll_Preference pp where mileage_rate = 0.535
and type='TV_COMMERCIALS' 
-- and j.updated > '2017-12-30'
and p.status = 'active' and j.status = 'active'
and j.production_id = p.id 
and j.payroll_preference_id = pp.id;
/* *
update production p, project j, payroll_Preference pp set mileage_rate = 0.545 where mileage_rate = 0.535
and type='TV_COMMERCIALS' 
and j.updated > '2017-12-30'
and p.status = 'active' and j.status = 'active'
and j.production_id = p.id 
and j.payroll_preference_id = pp.id;
/* *
update production p, project j, payroll_Preference pp set mileage_rate = 0.545 where mileage_rate = 0.535
and type='TV_COMMERCIALS' 
and first_payroll_date > '2018-01-01'
and p.status = 'active' and j.status = 'active'
and j.production_id = p.id 
and j.payroll_preference_id = pp.id;
/* *
update production p, payroll_Preference pp set mileage_rate = 0.545 where mileage_rate = 0.535
and type='TV_COMMERCIALS' and status = 'active' and p.payroll_preference_id = pp.id;


/* UPDATE ALL BATCH EMAIL addresses for LION TV
update payroll_preference pp, project pj, production p 
		set pp.batch_email_address = 'TianaS@media-services.com' ,
		 pp.onboard_email_address = 'TianaS@media-services.com' 
where batch_email_address like '%@media-services.com%'
	and p.status = 'active' 
	and pp.payroll_service_id = 12
	and 
		( (pj.payroll_preference_id = pp.id and pj.production_id = p.id)
		or (p.payroll_preference_id = pp.id and pj.id = p.default_project_id) )
	and pp.batch_email_address is not null
	and p.studio like '%lion%' ;
/* */
/* *  SELECT ALL payroll preferences based on studio and/or existing email values
select * from payroll_preference pp, production p, project pj  
where batch_email_address is not null 
	-- and like '%@media-services.com%'
	and p.status = 'active' 
	-- and pp.payroll_service_id = 12
	and 
		( (pj.payroll_preference_id = pp.id and pj.production_id = p.id)
		or (p.payroll_preference_id = pp.id and pj.id = p.default_project_id) )
	and pp.batch_email_address is not null
	and p.studio like '%lion%'
order by p.studio, batch_email_address;

/* *
-- UPDATE all BATCH EMAIL ADDRESSES for a single production (all projects - commercial)
update payroll_preference pp, project pj, production p 
		set pp.batch_email_address = 'LMezzacappa@theTeamCompanies.com' 
	where pj.payroll_preference_id = pp.id and pj.production_id = p.id
	and pp.batch_email_address is not null
	and pp.batch_email_address <> 'lmezzacappa@theteamcompanies.com'
	and p.id = 5557;
/* */

/* 2/25/16 Find productions using BATCH TRANSFER EMAIL ADDRESS ... *

select p.title, j.title, use_email, batch_email_address from production p, project j, payroll_preference pp where batch_email_address is not null
 and j.production_id = p.id 
 and (j.payroll_preference_id = pp.id
 or p.payroll_preference_id = pp.id)
/* */

/* Select info from PRODUCTION, PAYROLL_PREFERENCE based on owner - ALL PILGRIM SHOWS

select p.prod_id, title, studio, status, payroll_service_id, ps.name, p.timecard_fee_percent, p.document_fee_amount
from production p, payroll_preference pp, payroll_service ps where
p.payroll_preference_id = pp.id and ps.id = payroll_service_id and
owning_account = 'A014339';
/* */

/* Set Start Form work city and state...
update start_form set work_location='New York', work_state='NY'
		 where prod_title = 'Indie Commercial 1' and work_location is null limit 100;
/* */

/* 2/20/16 find START FORMS that are in a COMMERCIAL production, but have a null project_id (invalid!)

select s.id, s.prod_title, p.id, last_name, first_name from start_form s,  production p, contact c
 where s.project_id is  null 
 and p.type = 'TV_COMMERCIALS'
 and s.contact_id = c.id
 and c.production_id = p.id
/* *
update start_form set project_id =1950 where project_id is null and prod_title = "Dwight's episodic P1829" limit 80;
/* */

/* Handling issue with ZEROS in CUSTOM MULTPLIER fields *
select * from pay_job where custom_mult4 is null;
update pay_job set custom_mult4=3.5 where custom_mult4 is null;
update pay_job set custom_mult5=0 where custom_mult5 is null;
update pay_job set custom_mult6=0 where custom_mult6 is null;
/* */

/* 2/10/16 Update TIMECARD RATE, LS_OCC_CODE others -- to fix up after StartForms were not completed properly (missing PayRates, etc.)

update weekly_time_card w, start_form sf, start_rate_set r 
set w.ls_occ_code = sf.ls_occ_code, w.occ_code = sf.occupation_code, 
		w.hourly_rate = r.hourly_rate_studio, w.daily_rate = r.daily_rate_studio, 
		w.rate = r.hourly_rate_studio,
		w.union_number = sf.union_local_num,
		w.occupation = sf.job_class
-- 		w.rate = r.daily_rate_studio
where prod_co like 'videotape t%'
 and sf.prod_rate_id = r.id
 and w.start_form_id = sf.id
 and w.last_name = 'videotape'
 -- and w.first_name='locationscout'
;

/* 2/10/16 misc timecard, startform selection stuff from Indie commercial QA beta productions ...   
select * from weekly_time_card where prod_co like 'indie com%'
 and first_name='bb electric';
/*
select r.*, sf.* from start_form sf, start_rate_set r where prod_company like 'indie com%'
and sf.prod_rate_id = r.id;
/* */

/* Update rates in timecards ...

update weekly_time_card set rate= 42.6829 , hourly_rate=42.6829   where prod_name = 'aicp 2' and last_name='817'
and ls_occ_code = 'cm674'
limit 61;

update weekly_time_card set rate=49.2830 , hourly_rate=49.2830  where prod_name = 'aicp 1' and last_name='817'
and ls_occ_code = 'cm670'
limit 61;

/* Find a bunch of timecards by OCC CODE and/or union  *
select * from weekly_time_card where prod_name = 'aicp 1' and union_number ='817'
and ls_occ_code = 'cm674'
order by end_date;

/* *
select * from weekly_time_card where prod_name = 'aicp 1' and union_number='817'
and ls_occ_code = 'cm670'
order by end_date;

/* */

/* 1/27/16 Fix contract keys in Start Forms
-- update start_form set occ_rule_key = 'C-38' where occ_rule_key = 'CI-38';
-- update start_form set union_key = 'CPA-38' where union_key = 'CPA-38-I'; 
/* */
/* Copy a record back into the same table with a new index...

DROP TABLE if exists temp_table1;
CREATE TABLE temp_table1 ENGINE=MEMORY
SELECT * FROM payroll_preference WHERE id=199;
alter table temp_table1 drop id;
INSERT INTO payroll_preference SELECT 0, temp_table1.* FROM temp_table1;

/* */
/*   Change PRODUCTION NAME and production company in all timecards and start forms *

update Weekly_time_card set prod_name = 'AICP 1-dh' where  prod_name = 'AICP 1';
update Weekly_time_card set prod_co = 'AICP 1-dh' where  prod_co = 'AICP 1';

update Weekly_time_card set prod_name = 'AICP 2-dh' where  prod_name = 'AICP 2';
update Weekly_time_card set prod_co = 'AICP 2-dh' where  prod_co = 'AICP 2';

update start_form set prod_title = 'AICP 1-dh' where  prod_title = 'AICP 1';
update start_form set prod_company = 'AICP 1-dh' where  prod_company = 'AICP 1';

update start_form set prod_title = 'AICP 2-dh' where  prod_title = 'AICP 2';
update start_form set prod_company = 'AICP 2-dh' where  prod_company = 'AICP 2';
/* */

/* BATCH TRANSMIT
-- Find productions using Batch Transmit --
select p.studio, p.title,pp.batch_email_address from payroll_preference pp, production p where pp.batch_email_address is not null
and p.payroll_preference_id = pp.id;

select p.studio, p.title,pp.batch_email_address from payroll_preference pp, project pj, production p where pp.batch_email_address is not null
and pj.payroll_preference_id = pp.id
and pj.production_id = p.id;
/*
group by p.title
order by studio, production_id, date;

/* */
/*
-- List of names and email address of production owners
select u.id, u.account_number, studio, title, concat(u.first_name, ' ', u.last_name) name, u.email_address email,
u.first_name, u.last_name,
substr(u.email_address,instr(u.email_address,'@')+1) domain,
date_format(start_date,'%Y-%m-%d')
from payroll_preference pp, production p, user u where
u.account_number = p.owning_account
-- and start_date > '2015-01-01'
and p.payroll_preference_id = pp.id
-- eliminate by payroll service, e.g., MS = 3
and (pp.payroll_service_id is null or pp.payroll_service_id <> 3)
group by email
order by name;

/* */
-- ---------------
/* LOOKING FOR ADMIN LIGHTSPEEDEPS LOGINS not from known IP addresses.
select * from event where 
username like '%lightspeedeps%' and 
type='login_ok'
and
( 
description  like '%108.251.56.140%')
 order by id desc limit 100;
/*
and description not like '%107.142.214.50%'
and description not like '%107.142.214.50%'
and description not like '%104.174.143.110%'
and description not like '%166.170.44.37%'
*/
/* LOOKING / FIXING ADMIN entries with WRONG PERMISSIONS mask

update project_member set permission_mask = 2283325011076841471 where id = 17102
and contact_id = 13116 and role_id=2;

/*
-- find all PAID productions owned by a user with a particular refer-by code:
select created, start_date, u.*, p.* from user u, production p where Referred_By ='fs' and owning_account = account_number
and order_status <> 'free' and sku <> 'f-fr-01';

/* stuff about coupons and payments - authorize.net
select * from event where type='info' and description like '%coupon%' order by production_id;
select * from event where type='info' and description like '%authorize%' order by production_id;
/* *
select * from user where Referred_By is not null and Referred_By <> 'fs';
select count(*) from user where Referred_By ='fs';

/*******	Find Start forms for Union roles, where matching TCs were generated after some date (like 7/1/15)

select * from (
		select max(w.end_date) last, s.* from start_form s, weekly_time_card w where
		w.start_form_id = s.id and 
		 s.occupation_code not like 'vt%'
		and s.occ_rule_key like 'la-%'
		group by s.id
		order by prod_title, s.union_local_num ) table1
where last > '2015-07-01' ;

/* ************ Find all RULEs that are NOT being used! (HTG) ****************
select rule_key from (
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
		union select rule_key from special_rule ) tableA
where rule_key not in (
	select distinct use_rule_key from contract_rule
) order by rule_key;

/* */
/********* statements from Timecard cleanup after Auto-Create was accidentally run on all productions
	with "ignore last week" option turned on. ************************
	/*
select * from temp_bad_wtcs3 where prod_name = 'et-ti';
select * from weekly_time_card  where
 updated > '2015-03-06 11:25' and updated < '2015-03-07' and status = 'OPEN' and end_date >= '2015-03-07';
/*
delete from daily_time where date >= '2015-03-01' and date <= '2015-03-14'
and weekly_id >= 37957 and weekly_id <= 38520
and weekly_id in ( select id from temp_bad_wtcs3);
/*
delete from weekly_time_card  where
 updated > '2015-03-03 23:40' and updated < '2015-03-03 23:58' and status = 'OPEN' and end_date >= '2015-03-07'
 and locked_by is null ;
/*
select * from weekly_time_card  where
 updated > '2015-03-03 23:45' and updated < '2015-03-03 23:58' and status = 'OPEN' and end_date = '2015-03-07';

delete from weekly_time_card  where
 updated > '2015-03-03 23:45' and updated < '2015-03-03 23:58' and status = 'OPEN' and end_date >= '2015-03-07' limit 600;
/*
delete from daily_time where date >= '2015-03-01' and date <= '2015-03-07'
and weekly_id >= 38521 and weekly_id <= 39786
and weekly_id in (select id from temp_bad_wtcs2);
/*
select count(*) from temp_bad_dt;
select count(*) from daily_time where date >= '2015-03-01' and date <= '2015-03-14'
and weekly_id >= 38521 and weekly_id <= 39786
and weekly_id in
( select id from temp_bad_wtcs2);
/*
select id from temp_bad_wtcs3 order by id desc ;
/*
select count(*) from daily_time where date >= '2015-03-01' and date <= '2015-03-07'
/* 
and weekly_id in
(select id from temp_bad_wtcs);
select count(*) from temp_bad_wtcs3;
/*
create table temp_bad_wtcs3
select * from weekly_time_card  where
 updated > '2015-03-03 23:40' and updated < '2015-03-03 23:58' and status = 'OPEN' and end_date >= '2015-03-07';
/*
select * from weekly_time_card where end_date = '2015-03-14' and not
(updated > '2015-03-03 23:50' and updated < '2015-03-03 23:58');
/*
update weekly_time_card set end_date = '2015-03-14', status='OPEN' where
 updated > '2015-03-03 23:50' and updated < '2015-03-03 23:58' and status = 'REJECTED' and end_date = '2015-03-07' limit 1250;
 ************* END TIMECARD CLEANUP CODE ************************/
	
/* delete "orphan" audit-trail records -- those whose timecards have been deleted
delete from audit_event where related_object_type = 0 and related_object_id not in
	(select id from weekly_time_card) ;

/* Check for Pilgrim Start forms not set to "CA" *
-- update start_form set overtime_rule = 'CA' where prod_company like 'pilgrim%' and overtime_rule = 'x1' limit 20;
select prod_title, first_name, last_name from start_form where prod_company like 'pilgrim%' and overtime_rule <> 'ca'
order by prod_title, last_name;

/*  Check for Start's with OT Rate table values > 1.65 *
select r.ot2_multiplier, first_name, last_name, prod_company, sf.* from start_form sf, start_rate_set r where prod_rate_id in (
select id from start_rate_set where ot2_multiplier > 1.65000 )
and r.id = sf.prod_rate_id
order by prod_rate_id;
/* *
select * from start_rate_set where ot2_multiplier > 1.65;

/* 3/13/16 CLEANUP REPORT TABLES *

delete from dood_report where report_id like '%-15%';
delete from dood_report where report_id like '%-160%' and report_id not like '%-1603%';

delete from script_report where report_id like '%-15%';
delete from script_report where report_id like '%-160%' and report_id not like '%-1603%';

delete from stripboard_report where report_id like '%-15%';
delete from stripboard_report where report_id like '%-160%' and report_id not like '%-1603%';
/* */
/* 
-- Find USER entries that have matching user FIRST and LAST NAMES

select u1.id, u2.id, u1.email_address, u2.email_address, u1.status, u2.status, u1.first_name, u1.last_name from user u1, user u2 
where u1.id < u2.id 
and u1.last_name = u2.last_name
and u1.first_name = u2.first_name
order by u1.last_name, u1.first_name, u1.id, u2.id

/*
-- LOOKING FOR all USER who might have created /beta accounts

select * from user where status <> 'deleted'
and email_address not like '%@ls.test.ttc' 
and email_address not like '%@lightspeed%' 
and email_address not like '%@harmfam%' 
and email_address not like '%@media-s%'
and email_address not like '%@indiepay%'
and email_address not like '%@innoeye%'
and email_address not like '%@abspayrol%'
and email_address not like '%@theteamcomp%'
-- and created_by = account_number

/* recent time-card Events *
select tce.* from  time_card_event tce where date > '2015-01-24' 
 order by date desc ;

/* statistics on recent time-card Events *
select count(id),type from  time_card_event tce where date > '2015-01-24' group by type;
/*
select * from user where id in (8444,7551);

/* event selection *

select id, start_time from event where type='app_error' 
and start_time > '2015-01-24'
and description not like '%calculateRoundingtype%actioneditok%'
and description not like '%expenselines%listendailychange%'
and description not like '%expenselines%listenmileagechange%'
and description not like '%expenselines%actiondeletemileage%'
and description not like '%expenselines%actioncreateboxrental%'
and description not like '%paylines%actionsave%'

order by id desc;

/* 4/3/15 *
select count(*) c,id, date(start_time), left(description,200) from event where type='app_error' 
and start_time > '2015-02-01'
group by description


/* 5/10/14 *
-- Find particular event entries...
Select * from event where description like '%precedesCurrentApprover%' and start_time > '2014-05-01';

/* 5/5/14
-- Check for start forms assigned to batches in the wrong production - happened due to bug in "import contacts" code.
select * from Start_form sf, contact c, production_Batch b, Production p where
	sf.production_batch_id = b.id and 
	b.production_id = p.id and
	sf.contact_id = c.id and
	c.production_id <> p.id;

-- Use the following to fix the problem:
	-- update start_form set production_batch_id = null where prod_title = 'Crew Transfer Test';
/* */
/*  Un-Do changes to DayType done by AlterV22toV30 script **
update daily_time set work_zone = null ,  day_type = null where work_zone = 'ZN' and  day_type = 'WK' and hours is null
 and call_time is null and wrap is null and worked = 0;
/* *
select * from daily_time where work_zone = 'DL' and  day_type = 'WK' and hours is null
 and call_time is null and wrap is null and worked = 0;

/* */

/* Episodic productions with more than one unit: 
    select unit.*, production.Type, production.id, production.Title
    from unit, project, production 
        where unit.number <> 1
        and project.id = unit.project_id
        and project.Production_Id = production.id
				and production.type <> 'feature_film'
/* */
/* orphan productions 
    select c.*,
         Prod_Id ,
         Title ,
         Type ,
        p.Status,
         Sku ,
         Start_Date ,
         End_Date ,
         Next_Bill_Date ,
         Billing_Amount ,
         Contact_Name ,
         Owning_Account 
    from
        production p, user u, contact c
    where u.email_address like '@%' and p.owning_account = u.account_number and c.production_id = p.id
    and c.user_id <> u.id and c.user_id = 1
    order by p.id;
/* */
/* coupons

-- update coupon set redeemed = '2012-07-20 13:15' where id=2;
insert coupon value(1, 'ABCD-01-FE-123456', 'Discount: $25 off. Your monthly subscription is now $50 per month.',
	'AMOUNT_OFF', 25, '.*', 0, 1, '2012-06-01', null, null, null, null);
insert coupon value(2, 'ABCD-01-FE-765432', 'Discount: $50 off. Your monthly subscription is now $25 per month.',
	'AMOUNT_OFF', 50, '.*', 0, 1, '2012-06-01', null, null, null, null);
insert coupon value(3, 'ABCD-02-FE-123456', 'Discount: $70 off. Your monthly subscription is now $5 per month.',
	'AMOUNT_OFF', 70, '.*', 0, 1, '2012-06-01', null, null, null, null);
insert coupon value(4, 'EX', 'Discount for 7/1 only!', 
	'AMOUNT_OFF', 25, 'F-IN-01', 0, 1, '2012-07-01', '2012-07-02', '2012-07-03 11:34', 'A0001', 'P2');

/*
-- update weekly_time_card set approver_id = 41 where approver_id = 32 ;
-- select * from weekly_time_card w where w.approver_id is not null;
/*
		select approver_id id, 'w' from weekly_time_card where approver_id is not null
		union
		select next_approver_id id, 'n' from approver where next_approver_id is not null
		union
		select approver_id id, 'p' from production where approver_id is not null
		union
		select first_approver_id id, 'd' from approval_anchor where first_approver_id is not null
order by id;
/*
-- delete from approver where id in (x);
select * from approver where id not in (
	select id from 
		(
		select approver_id id from weekly_time_card where approver_id is not null
		union
		select next_approver_id id from approver where next_approver_id is not null
		union
		select approver_id id from production where approver_id is not null
		union
		select first_approver_id id from approval_anchor where first_approver_id is not null
		) as t1
);


/* get rid of old un-shared approvers:
delete from approver where shared = 0 and id not in (
	select distinct approver_id from weekly_time_card w where w.approver_id is not null);
/*
-- Statements to review approver information ..

set @mail = 'more@%';
set @prod = 'best tv%';

set @uid = ( select id from user where email_address like @mail limit 1) ;
select * from user where id = @uid;
select p.title, c.* from contact c, production p where c.user_id = @uid and p.id = c.production_id;

set @cid = ( select c.id from contact c, production p where c.user_id = @uid and p.id = c.production_id
	and p.title like @prod );

select * from approver a where a.contact_id = @cid;

select * from weekly_time_card w where w.approver_id in (select a.id from approver a where a.contact_id = @cid);
/*
-- See how many registered users we have (use on app01)

select count(*) from user where email_address not like '%.uncsa.edu' and email_address not like '%@ls.test.ttc'
and status ='registered';

select count(*) from user where email_address like '@%';

select count(*) from user where email_address like '%.uncsa.edu' and status ='registered';

select count(*) from user where email_address like '%@ls.test.ttc' and status ='registered';

/*
-- find timecards that don't match startForm's occupation.
select start_form.last_name, start_form.First_Name, start_form.Job_Class, weekly_time_card.Occupation,
weekly_time_card.End_Date
 from weekly_time_card, start_form, user, contact
where weekly_time_card.User_Account = user.Account_Number
and contact.User_Id = user.Id
and contact.Production_Id = 25
and start_form.Contact_Id = contact.id
and start_form.Job_Class <> weekly_time_card.Occupation
order by last_name

/*
-- Find all project members with a particular permission on in their mask
set @permId = 61;
set @mask = 1 << (@permId-1); -- generate the mask value with a 1-bit in the proper position
select * from Project_Member where (mod(permission_Mask,( @mask * 2 )) / @mask ) >= 1;

-- Update all LS Admin roles to include 2 new permssions:
set @permId1 = 11;
set @mask1 = 1 << (@permId1-1); -- generate the mask value with a 1-bit in the proper position

set @permId2 = 33;
set @mask2 = 1 << (@permId2-1); -- generate the mask value with a 1-bit in the proper position

update project_member set permission_mask = (permission_Mask | @mask1 | @mask2) where role_id = 2;

/* */
-- alter table callsheet alter date drop default; 
-- update callsheet c set date = date_add(c.date, interval 2 hour) where c.id =56;
-- update dpr d set date = d.date, revised_end_date = date_add(d.revised_end_date, interval 2 hour);

/*
update changes set production_id = 5;
update event set production_id = 5;
update project set production_id = 5;
update production set id = 5;

/* 
truncate dood_report;
truncate script_report;
truncate stripboard_report;
/* 
select count(*) from dood_report;
select count(*) from script_report;
select count(*) from stripboard_report;

/*
-- From elementListSubreport.jrxml:

(select sc.number, sc.sequence from scene_script_element sse, scene sc where 
 sse.script_element_id =3724 and sse.scene_id = sc.id
  and sc.script_id = 57 )
  union(
select sc.number, sc.sequence from scene sc where sc.set_id = 3724 and
		sc.script_id = 57 )
		order by sequence ASC
;

/*
-- fix textElements accidentally modified by old scriptPageBean code:

update text_element set text = replace(text,'<br/>', '\n') where text like '%<br%';

/*
SELECT table_schema, sum( data_length + index_length ) / 1024 / 1024 "Data Base Size in MB" 
FROM information_schema.TABLES GROUP BY table_schema ;

SELECT table_schema, table_name, ( data_length + index_length ) / 1024 / 1024 as "SizeMB"
FROM information_schema.TABLES where table_schema = 'lsdb01'
order by sizemb desc;

/**
delete from event where type='APP_ERROR' and start_time < '2010-12-01';
/**
select sn.* from scene sn where script_id in (328) order by script_id, sequence;
/**
SELECT s.id, sn.number, te.* from text_element te, script s, scene sn where 
	sn.script_id = s.id and
	te.scene_id = sn.id and
	sn.number in ('1','2','3','4','5','6','20') and
	s.id in (328)
	order by  s.id, sn.sequence, te.sequence;
/**
delete from script_element where id = 4328;
/*
select distinct se.id , se.name, 'selcted'
from script_element se, real_link rl 
where se.id = rl.script_element_id 
and se.project_id = 101
and se.type in ( 'CHARACTER','prop'  )
and rl.status = 'selected'
union
select distinct se.id , se.name, 'UNselcted'
from script_element se 
left join real_link rl on (se.id = rl.script_element_id)
where se.project_id = 101
and se.type in ( 'CHARACTER','prop'  )
and se.id not in (
select distinct se.id
from script_element se, real_link rl 
where se.id = rl.script_element_id 
and se.project_id = 101
and rl.status = 'selected')
/*
/**
select * from text_element where scene_id between 3173 and 3342; /* moon*/
/*
select * from text_element where scene_id > 3520; /* precious */ 
/**select * from text_element where scene_id between 3498 and 3520; /* 8-scene?
/*
select * from image where location_element_id is not null;

/*
		SELECT  
		 sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, 
		 sp.scene_Numbers scene_number, sp.Synopsis, sp.Elapsed_Time, sp.Sheet_Number, 
		 sce.IE_Type scne_IET, sce.DN_Type scene_DNT, 
		 IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length, 
		 sce.page_number scene_page_number 
		FROM project proj, scene sce, strip sp 
		WHERE  proj.current_stripboard_id=sp.Stripboard_Id and sp.Type='BREAKDOWN' and 
		 concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) 
		 and sce.Script_Id=proj.Current_Script_Id
		 and proj.id = 101
		  order by sce.sequence;
/*
		SELECT distinct 
		 sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, 
		 sp.scene_Numbers scene_number, sp.Synopsis, sp.Elapsed_Time, sp.Sheet_Number, 
		 sce.IE_Type scne_IET, sce.DN_Type scene_DNT, 
		 IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length, 
		 sce.page_number scene_page_number 
		FROM project proj, scene sce, script_element sc, strip sp 
		WHERE  proj.current_stripboard_id=sp.Stripboard_Id and sp.Type='BREAKDOWN' and 
		 concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) 
		 and sce.Script_Id=proj.Current_Script_Id
		 and proj.id = 101
		  order by sce.sequence;

/*
SELECT sp.scene_Numbers title, sp.scene_Numbers project_title, sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, 
  sp.scene_Numbers scene_number, sp.Synopsis, sp.Elapsed_Time, 
  sp.Sheet_Number,  sce.IE_Type scne_IET, sce.DN_Type scene_DNT,
IF(sp.length>=8, concat(cast(truncate(sp.length/8,0) as char), if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))), concat(cast(sp.length as char),'/8')) page_length,
sce.page_number scene_page_number, sp.type, sp.status, sp.orderNumber 
FROM project proj, scene sce, strip sp
WHERE  proj.current_stripboard_id=sp.Stripboard_Id and 
(sp.Type='BREAKDOWN' or sp.type='end_of_day') and  
(concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) or sp.scene_numbers is null) and
sce.Script_Id=proj.Current_Script_Id  and 
proj.id = 101 and 
sp.Status <> 'OMITTED'
group by ordernumber
order by Status, OrderNumber;

/* DPR-crew subreport:
select contact.first_name, contact.last_name, tc.report_set, tc.dismiss_set, department.name, tc.role
from time_sheet ts, time_card tc, contact, department, role
where ts.date = '2010-09-03'
and ts.project_id = 10
and tc.time_sheet_id = ts.id
and tc.dtype != 'CT'
and tc.contact_id = contact.id
and tc.role = role.Name
and tc.department_id = department.id
and department.id in (101,108,115)
and department.list_priority > 0
order by department.list_priority, role.List_Priority;
/*
delete from date_event where type='standard_off';
update project_schedule set last_days_off_change = null;
/* Shooting schedule selection
SELECT  sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, 
  sp.scene_Numbers scene_number, sp.Synopsis, sp.Elapsed_Time, 
sp.Sheet_Number,  sce.IE_Type scne_IET, sce.DN_Type scene_DNT,
IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length,
sce.page_number scene_page_number, sp.type, sp.status, sp.orderNumber 
FROM project proj, scene sce, strip sp
WHERE  proj.current_stripboard_id=sp.Stripboard_Id and 
(sp.Type='BREAKDOWN' or sp.type='end_of_day') and  
(concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) or sp.scene_numbers is null) and
sce.Script_Id=proj.Current_Script_Id  and 
proj.id = 10 and 
sp.Status <> 'OMITTED'
group by ordernumber
order by Status, OrderNumber;
/*
update contact set notify_for_alerts = false;
update contact set notify_for_script_changes = false;
/*
select * from text_element
where scene_id = 1070
/*
select dept.name as deptname, cc.count, cc.role_name, cc.name as crewcallName, cc.time, cc.call_type
from dept_call dc, crew_call cc,callsheet cs, department dept
where dc.id in (235,237)
and cc.dept_call_id = dc.id
and dc.department_id = dept.id
order by dept.list_priority, line_number;
/*
SELECT contact.First_name,contact.last_name, tc.castid,
case tc.day_type
when 'WORK' then 'W' 
when 'OTHER_TRAVEL' then 'T'
when 'COMPANY_TRAVEL' then 'T'
when 'HOLIDAY' then 'Y'
when 'OFF' then ' '
when 'START' then 'SW'
when 'START_FINISH' then 'SWF'
when 'START_DROP' then 'SWD'
when 'START_TRAVEL' then 'ST'
when 'DROP' then 'WD'
when 'PICKUP' then 'PW'
when 'PICKUP_DROP' then 'PWD'
when 'PICKUP_FINISH' then 'PWF'
when 'FINISH' then 'WF'
when 'HOLD' then 'H'
else '?'
end as status,
tc.* 
   FROM time_card tc,  contact
WHERE 
tc.time_sheet_id = 45
and contact.id = tc.contact_id
and tc.dtype = 'CT'
order by tc.castId;

/*
select *
from dept_call dc, crew_call cc, department dept
where dc.id in (238,239,240)
	and cc.dept_call_id = dc.id
	and dc.department_id = dept.id
order by dept.list_priority, line_number;
/*
update user set encrypted_password = null where id = 6;
/*
		SELECT img.content as content, rw.Name, rw.id, rw.phone, a.Addr_Line1, a.Addr_Line2, a.City, a.State, 
		 a.Zip, rw.Phone, rw.Special_Instructions as Parking 
		FROM  real_world_element rw 
		 left outer join address a on (rw.Address_Id=a.id) 
		 left outer join image img on( rw.map_id=img.id) 
		WHERE rw.Type='LOCATION' 

/*
select rw.id, rw.name, rw.special_instructions as parking, rw.phone, rw.directions,
 ad.addr_line1, ad.addr_line2, ad.city, ad.state, ad.zip, image.content
 from real_world_element rw left join image on rw.map_id = image.id, address ad
 where
 rw.address_id = ad.id and
 rw.id = 119;
 
 select *
  from point_of_interest poi, location_interest li, address ad
  where poi.id = li.interest_id and
  poi.address_id = ad.id and 
  li.location_id = 119
  ;
/*
SELECT table_schema, sum( data_length + index_length ) / 1024 / 1024 "Data Base Size in MB" 
FROM information_schema.TABLES GROUP BY table_schema ;
*/
/**
SELECT table_name, ( data_length + index_length ) / 1024 / 1024 as "SizeMB"
FROM information_schema.TABLES where table_schema = 'lsdb01'
order by sizemb desc;
 
/*
replace Call_Note values 	
(1101,0,'leader',1,'Come & Get It - Hot Breakfast!', 92 )	,
(1102,1,'Misc',1,'No forced call without prior approval of UPM! (I mean it!)', 92 )	,
(1103,2,'Misc',1,'Production needs donuts.', 92 )	,
(1104,3,'Misc',1,'All the props went on strike', 92 )	,
(1105,4,'Misc',1,'Dressing? Italian please.', 92 )	,
(1106,5,'Misc',1,'blow up lots of buildings', 92 )	,
(1107,6,'Misc',1,'Is it Halloween yet?', 92 )	,
(1108,7,'Misc',1,'Gave everyone a shave', 92 )	,
(1109,8,'Misc',1,'lighten up, please!', 92 )	,
(1110,9,'Misc',1,'zoom, zoooom', 92 )	,
(1111,10,'Misc',1,'we are all special', 92 )	,
(1112,21,'Misc',1,'UPM: John Johnston         1st AD:  Jane Jamestown        2ndAD:  Sarah Soronston      2nd2ndAD: Jim James', 92 )	,
(1113,21,'Misc',2,'                                                                  310-555-1212                   310-444-1212', 92 )	,
(1114,22,'Misc',1,'This is the first line of some notes for the bottom of the back page.', 92 )	,
(1115,22,'Misc',2,'And here is another line.', 92 )	,
(1116,0,'leader',1,'Come & Get It - Hot Breakfast!', 45  )	,
(1117,1,'Misc',1,'No forced call without prior approval of UPM! (I mean it!)', 45  )	,
(1118,2,'Misc',1,'Production needs donuts.', 45  )	,
(1121,5,'Misc',1,'blow up lots of cars', 45  )	,
(1122,6,'Misc',1,'Is it Halloween yet?', 45  )	,
(1123,7,'Misc',1,'Shaved yesterday', 45  )	,
(1124,8,'Misc',1,'lighten up, please!', 45  )	,
(1125,9,'Misc',1,'vroooom', 45  )	,
(1126,10,'Misc',1,'we are all a little special', 45  )	,
(1127,21,'Misc',1,'UPM: John Johnston         1st AD:  Jane Jamestown        2ndAD:  Sarah Soronston      2nd2ndAD: Jim James', 45  )	,
(1128,21,'Misc',2,'                                                                  310-555-1212                   310-444-1212', 45  )	,
(1129,22,'Misc',1,'Its great having notes for the bottom of the back page.', 45  )	,
(1130,22,'Misc',2,'And here is another line.', 45  )	
;	


/*
select last_name, first_name, primary_phone_index, hidden from contact order by last_name, first_name;
/**
update contact set hidden = true where id in (117,109, 120, 111);

/*
SELECT  distinct cont.id , cont.assistant_Id as assistant, dept.name as department_name, 
cont.first_name, cont.last_name, role.name as Role, cont.email_address, 
addr.addr_line1, addr.addr_line2, cont.home_phone, cont.business_phone, cont.cellphone, addr.city,  
MIN(role.List_Priority),
addr.state, addr.zip, cont.im_service, cont.im_address,cont.home_phone as assistant_phone,
case cont.minor when 0 then ' ' else 'Minor' end as Minor 

  FROM contact as cont left join address as addr on cont.Home_Address_Id = addr.id,
	  project as proj, department as dept, role as role, project_member as prom, user as usr 

WHERE  usr.contact_id=cont.id and prom.role_id=role.id 
and proj.id=prom.project_id and prom.user_id=usr.id and prom.assigned=1 and role.department_id=dept.id 
 and dept.name <> 'LS Admin'
 
GROUP BY cont.id  /*, dept.name

order by last_name, first_name;

/* *
-- find mismatched default role/default departent
select c.id, c.last_name, c.first_name, c.role_id, c.department_id, r.name, d.name, dc.name from contact c, role r, department d, department dc
 where r.department_id = d.id and c.role_id = r.id and dc.id = c.department_id and
  r.department_id <> c.department_id;

/**
select c1.* from Contact c1 where c1.id not in 
					(select c.id from Contact c, User u, Project_Member pm, Role r 
					where u.contact_id = c.id and pm.user_id = u.id and pm.role_id = r.id 
					 and r.department_id in (124,128) );
/**
select distinct c.* from Contact c, User u, Project_Member pm, Role r
					where u.contact_id = c.id and pm.user_id = u.id 
					and pm.role_id = r.id and r.department_id not in (124,128);
/**
select  c1.id from Contact c1 where c1.id not in 
					(select distinct c.id from Contact c, User u, Project_Member pm, Role r 
					where u.contact_id = c.id and pm.user_id = u.id and pm.role_id = r.id and 
					((r.name like '%talent%') or (r.name like 'LS %') or (r.name like 'VIP')) );
/**
(select sc.number from scene_script_element sse, scene sc where 
 sse.script_element_id = 101 and sse.scene_id = sc.id  order by sc.sequence) 
 union (select sc.number from scene sc where sc.set_id = 101 order by sc.sequence);
/*
select distinct sc.number from scene_script_element sse, scene sc where 
 (sse.script_element_id = 101 and sse.scene_id = sc.id) or 
  sc.set_id = 101 order by sc.sequence;
/**
select sc.number from scene sc 
where sc.set_id = 101;
/* *
select distinct sc.number from scene_script_element sse, scene sc where 
 (sse.script_element_id = 102 and sse.scene_id = sc.id) or sc.set_id = 102 order by sc.sequence;
/*
select distinct usr.user_name, usr.login_allowed, cont.assistant_id as assistant, dept.name as department_name,  
cont.first_name, cont.last_name, role.name as role,  case cont.minor when 0 then ' 'else 'minor' end as Minor, 
addr.addr_line1, addr.addr_line2, addr.city, addr.state, addr.zip, cont.pseudonym, cont.home_phone, 
cont.business_phone,  cont.cellphone,cont.email_address, cont.imdb_link, cont.im_service, cont.im_address, 
cont.sag_member, cont.dga_member,  cont.aftra_member, cont.iatse_member, cont.teamsters_member, 
cont.notify_by_email, cont.notify_by_text_msg 
 
from contact as cont left join address as addr on cont.Home_Address_Id = addr.id,
project as proj, department as dept, 
user as usr left join project_member as prom on prom.user_id=usr.id left join role on prom.role_id=role.id
 
where usr.contact_id=cont.id and 
role.department_id=dept.id  and 

prom.assigned=1 and 
prom.project_id=101 and

cont.id = 158;

/**
select distinct usr.user_name, usr.login_allowed, cont.assistant_id as assistant, dept.name as department_name,  
cont.first_name, cont.last_name, role.name as role,  case cont.minor when 0 then ' 'else 'minor' end as Minor, 
addr.addr_line1, addr.addr_line2, addr.city, addr.state, addr.zip, cont.pseudonym, cont.home_phone, 
cont.business_phone,  cont.cellphone,cont.email_address, cont.imdb_link, cont.im_service, cont.im_address, 
cont.sag_member, cont.dga_member,  cont.aftra_member, cont.iatse_member, cont.teamsters_member, 
cont.notify_by_email, cont.notify_by_text_msg 
 
from contact as cont left join address as addr on cont.Home_Address_Id = addr.id,
project as proj, department as dept, role as role, project_member as prom, user as usr
 
where usr.contact_id=cont.id and prom.role_id=role.id  and 
 prom.user_id=usr.id and prom.assigned=1 and 
role.department_id=dept.id  and 

prom.project_id = 101 and
cont.id = 116;
/**/

/*
select distinct dept.name as department_name,
	 cont.first_name, cont.last_name, cont.email_address, role.name as role, 
	 case when cont.primary_phone_index = 1 then cont.cellphone 
	 	when cont.primary_phone_index = 2 then cont.home_phone 
	 	else cont.business_phone end as phoneno
	 from production as prod, project as proj, department as dept, contact as cont,
	 	role as role, project_member as prom, user as usr 
	 where prod.id=proj.production_id and usr.contact_id=cont.id and prom.role_id=role.id
	 and proj.id=prom.project_id and prom.user_id=usr.id and prom.assigned=1 and role.department_id=dept.id
	 and proj.id=101
	 order by dept.name, last_name;
	

/* orphan test*
select se.id from script_element se where project_id=101 and se.type = 'LOCATION' 
and id not in (
	select se.id from script_Element se, scene_script_element sse
		where se.project_id = 101
		and sse.script_element_id = se.id
		group by se.id )
and id not in
	(select distinct sn.set_id from scene sn, script st
		where st.project_id = 101 
		and sn.script_id = st.id and sn.set_id is not null);

/*
select se.id from script_element se where id not in (
 group by se.id order by count(se.id) desc;
/*
select rwe.* from Real_World_Element rwe, Project_Member pm,
Contact c, User u
where rwe.type='CHARACTER'
 and pm.project_id = 101 
 and pm.user_id = u.id and u.contact_id = c.id and rwe.contact_id = c.id;
 
/**
delete from dpr_days where id > 104;
delete from script_measure where id > 118;
delete from dpr_scene where dpr_id > 102;
delete from extra_time where dpr_id > 102;
delete from dpr where id > 102;
delete from catering_log where dpr_id > 102;
delete from sound_stock where id > 102;
/**
delete from time_card where time_sheet_id > 102;
delete from time_sheet where id > 102;
/**
select version();

/** select * from dood_report 
  where report_id like 'break%' 
  and type_name in ( 'cast', 'props','sets' )
  order by report_id, segment,  sequence, work desc, element_name;

/** select * from dood_report 
  where report_id like 'nobreak09022%' 

order by report_id, segment, type, sequence, cast_id, element_name;
/*and type_name in ( 'HEADING', 'cast', 'PROPs', 'WARDROBE', 'Set' )*/

/*
select se.name, se.id, count(se.id) from script_Element se, scene_script_element sse, scene s
 where se.type = 'CHARACTER' 
 and se.project_id = 101 
 and sse.script_element_id = se.id
 and sse.scene_id = s.id
 and s.script_id = 139

 group by se.id order by count(se.id) desc;

/*and se.element_Id is null*/

/*
update user set locked_out=0;

/*
select * from contact, role, project_member pm, user 
	where pm.project_id = 505 and pm.assigned = true 
	and pm.user_id = user.id and pm.role_id = role.id 
	and user.contact_id = contact.id 
	and contact.department_id = 103 
	order by role.list_priority ;

/*
select * from department where list_priority > 0 order by list_priority;

/*
select contact.* from contact, role where contact.role_id = role.id and role.name = 'Unit production manager'

/*
select se.*
from script_element se, scene_script_element sse
where sse.scene_id = 707
and sse.script_element_id = se.id
and se.type = 'character';

/*
select poi.* 
from point_of_interest poi, location_interest li
where li.location_id = 1017
and li.interest_id = poi.id
and poi.type='hospital';

/*
select  rwe.*
	from real_link as rl, real_world_element as rwe
	where rl.script_element_id = 808
	and rwe.id = rl.real_element_id
	and rl.status = 'selected';

/*
select * from Strip, scene ,project where strip.stripboard_id = project.Current_Stripboard_Id 
	and project.id = 901 
	and strip.status = "Scheduled"
	and strip.type = "Breakdown"
	and scene.script_id = project.Current_Script_Id
	and strip.Scene_Numbers = scene.Number
	order by strip.orderNumber;
	/*
	and concat( strip.scene_numbers, ',') like concat( scene.number, ',%')

select * from Strip, project where stripboard_id = project.Current_Stripboard_Id
	and project.id = 901 
	and strip.status = "SCHEDULED"
	and strip.type = "BREAKDOWN"
	order by strip.orderNumber;
/*
select distinct se.id, se.name, se.element_id, se.type, rl.status  
from script_element as se 
left join real_link as rl on (se.id = rl.script_element_id and(rl.status='selected' or rl.status='under_review'))
order by se.id;
/*
select se.id, se.name, rl.* 
from script_element as se left join real_link as rl on (se.id = rl.script_element_id)
order by se.id;
/*
select * from 
(select id as id2, status as st2, script_element_id as sid2  from real_link) as t2 left join
(select id as id1, status as st1, script_element_id as sid1  from real_link where status='SELECTED') as t1
 on (t1.sid1 = t2.sid2)
 order by sid2;

/*

select * from real_link as r1 left join real_link as r2 on (r1.script_element_id = r2.script_element_id and r2.status='SELECTED');
/*
select script_element.id, script_element.name, r1.status, r2.status
from script_element left join real_link r1 left join real_link r2,
where 
 r1.script_element_id = script_element.id
  and r1.status = 'SELECTED'
  and r2.script_element_id = script_element.id
  and r2.status != 'SELECTED'
   and project_id=101;
  
/**  update user set locked_out=0;  /**/
/** select se.id, count(se.id) from Scene s, script_Element se, scene_script_element sse 
				where se.type='CHARACTER' 
				and se.project_id = 220
				and se.element_Id is null 
				and s.script_id = 225
				and sse.scene_id = s.id and sse.script_element_id = se.id 
				group by se.id order by count(se.id) desc; /**/

/** update user set encrypted_password='tWF4i5QzmM9teAYYTZ6hoy6DeWnKHkiaXjbmbiFt3Y2Mzyr1OEKeYg==' where id=101; /**/
/** select * from callsheet; /**/
/**  select * from page_field_access where page like '6.1%'; /**/

/** select distinct concat(page, ',', field) 
	from Page_field_access pfa, Role_Group_Permission rgp, role r where
	r.id = 7
	AND rgp.role_group_id = r.role_group_id
	AND rgp.permission_id = pfa.permission_id order by page, field;
/**/

/**  select * from page_field_access where permission_id=16; /**/
/**  select * from role_group_permission where permission_id=16; /**/

/** select * from stripboard_report
where report_id like 'all-sh%'
order by sheet;
/**/

/** select concat('Day ',cast(sc.day_number as char)) date, sc.heading, sc.number, sc.cast_ids, sc.day_night, sc.pages, sc.location
from scene_call sc, callsheet cs
where sc.advance_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
order by day_number, line_number;

/* "Days" table - middle column.  Right column is left minus middle.
        Last row of table is sum of all other rows. *
select dpr_days.*
from dpr, dpr_days
where dpr.days_to_date_id = dpr_days.Id
        and dpr.date = '2008-12-21'; /**

select 
(x.first_unit-y.first_unit) First_Unit,
(x.second_unit-y.second_unit) Second_Unit,
(x.Rehearse-y.Rehearse) Rehearse,
(x.Test-y.Test) Test,
(x.WAS-y.WAS) WAS,
(x.Reshoot-y.Reshoot) Reshoot,
((x.first_unit+x.second_unit+x.Rehearse+x.Test+x.WAS+x.Reshoot)-
	(y.first_unit+y.second_unit+y.Rehearse+y.Test+y.WAS+y.Reshoot)) as total
from dpr b, dpr_days x, dpr_days y
where y.id=b.Days_To_Date_Id
and b.date ='2008-12-21'
and project_id=101
and x.id= b.Days_Scheduled_Id; 

/**
select * from user where encrypted_password is null;

/**
update user set locked_out = 0 where id < 112;
  
/*
replace Project_Member values 	
(107,111,5,TRUE,101);

/**
select contact.display_name, tc.castid,   script_element.name,
	case tc.day_type 
		when 'WORK' then 'W'
		when 'OTHER_TRAVEL' then 'T'
		when 'COMPANY_TRAVEL' then 'T'
		when 'HOLIDAY' then 'Y'
		when 'OFF' then ' '
		when 'START' then 'SW'
		when 'START_FINISH' then 'SWF'
		when 'START_DROP' then 'SWD'
		when 'START_TRAVEL' then 'ST'
		when 'DROP' then 'WD'
		when 'PICKUP' then 'PW'
		when 'PICKUP_DROP' then 'PWD'
		when 'PICKUP_FINISH' then 'PWF'
		when 'FINISH' then 'WF'
		when 'HOLD' then 'H'
	 	else '?' 
	 end as status,
   tc.*
 from time_sheet ts, time_card tc,  contact, real_world_element, real_link, script_element
	where ts.date =  '2008-12-20'
	and ts.project_id = 101
	and tc.time_sheet_id = ts.id
	and tc.dtype = 'CT'
	and contact.id = tc.contact_id
  and contact.id = real_world_element.contact_id
  and real_world_element.id = real_link.real_element_id
  and real_link.script_element_id = script_element.id
  and real_link.status = 'SELECTED'
  and script_element.type = 'CHARACTER'  ; /**/

/**	select * from Strip, scene where stripboard_id = 101  
	and strip.status = "Scheduled"
	and strip.type = "Breakdown"
	and scene.script_id = 101
	and concat( strip.scene_numbers, ',') like concat( scene.number, ',%')
	order by strip.orderNumber; /**/

/**	select * from Strip where stripboard_id = 101  
	and strip.status = "Scheduled"
	and strip.type = "Breakdown"
	order by strip.orderNumber; /**/

/** update script_element 
  set requirement_satisfied=true 
  where id in (103,101,112);/**/

/** select count(*), script_element.type from script_element 
 where  real_element_required = true 
 and script_element.id not in 
 (select script_element_id from real_link where real_link.status = 'SELECTED') 
 and project_id=101
 group by script_element.type; /**/
 
 
/** select count(*), script_element.type from script_element 
 where  real_element_required = true 
 and requirement_satisfied = false
  and project_id=101
  and script_element.responsible_Party_id >0
 group by script_element.type;
/**/
/* select  distinct script_element.type, script_element.id  from script_element, script, scene, scene_script_element, project 
where project.id=505
  and script.id = project.current_script_id
  and scene.Script_Id = script.id
  and scene.Id = scene_script_element.Scene_Id
  and script_element.id = scene_script_element.Script_Element_Id
  and script_element.responsible_Party_id =102
  and  real_element_required = true 
  and requirement_satisfied = false ; /**/
 
/** select * from script_element where script_element.type = 'PROP'  
 and project_id=101;  /**/

/** select script_element.name, real_link.status, real_world_element.* 
from real_world_element, real_link, script_element
where  real_world_element.id = real_link.real_element_id
  and real_link.script_element_id = script_element.id
  and real_link.status = 'SELECTED'
  and script_element.type = 'PROP' 
  and project_id=101; /**/

/** select contact.*,  script_element.name as jobTitle
from contact, real_world_element, real_link, script_element
where contact.id = real_world_element.contact_id
  and real_world_element.id = real_link.real_element_id
  and real_link.script_element_id = script_element.id
  and real_link.status = 'SELECTED'
  and script_element.type = 'CHARACTER'  ;

/**
select '1' as type, contact.*,  script_element.element_id as orderNumber, script_element.name
from contact, real_world_element, real_link, script_element, role
where contact.id = real_world_element.contact_id
  and real_world_element.id = real_link.real_element_id
  and real_link.status = 'SELECTED'
  and real_link.script_element_id = script_element.id
union 
select '2', contact.*, role.list_priority, role.name 
from contact, role
where contact.role_id = role.id 
  and role.name not like '%Talent%'
 order by type, ordernumber
; /**/
/**
select '1' as type, contact.*,  script_element.name as jobTitle
from contact, real_world_element, real_link, script_element, role
where contact.id = real_world_element.contact_id
  and real_world_element.id = real_link.real_element_id
  and real_link.status = 'SELECTED'
  and real_link.script_element_id = script_element.id
union 
select '2', contact.*, role.name 
from contact, role
where contact.role_id = role.id 
  and role.name not like '%Talent%'
 order by type, jobTitle
;/**/
/**
select display_name  from contact where first_name like "b%" 
union select first_name  from contact where last_name like "s%";
/**/

/** select * from time_sheet ts, time_card tc
	where ts.date > '2008-12-01'
	and ts.project_id = 101
	and tc.time_sheet_id = ts.id
	and tc.dtype = 'CT'; /**/

/**  select * from scene_script_element
   order by script_element_id; /*
	order by scene_id; /**/
/** select * 
	from Scene scene inner join scene_script_Element se on scene.Id = se.scene_id
		where se.script_element_Id = 113
		and scene.Script_Id = 101 
		
		; /**/


/** select count(*)
	from Scene scene inner join scene_script_Element se on scene.Id = se.scene_id 
		where se.script_element_Id = 113 and scene.Script_Id = 101
		; /**/

/** select * from scene where script_id = 159 order by sequence;

/** Select se.* from Script_Element se, Real_Link rl, real_world_element re
					where re.contact_id = 105
					and  rl.real_element_id=re.id
					and  rl.status = 'SELECTED'
					and  rl.script_element_id= se.id
					and se.project_id = 101; /**/

/** Select c.* from Contact c, Real_Link rl, real_world_element re
					where rl.script_element_id=113
					and rl.real_element_id=re.id
					and  rl.status = 'SELECTED'
					and re.contact_id = c.id; /**/


/** delete from date_event where schedule_id=101; /**/

/** select * from date_event where schedule_id = 101 order by start; /**/

/**  select * from strip where stripboard_id = 101 order by ordernumber ; /**/
/**  drop user lightspeed; /**/

/** select * from scene where sequence > 2 limit 1; /**/

/** select  * from script_element where project_id=1 order by id; /**/

/** update script_element set element_id = (id - 612) where project_id = 1; /**/
	/**
/**/
/**/
	