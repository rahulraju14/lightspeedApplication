SET foreign_key_checks = on;

/* *
-- get number of unique users logging in, by month, where user is a member
-- of a Team client production. 
select count(*) num, date2
from (
	select date_format(date(start_time),'%Y-%m') date2, 
		concat(date_format(date(start_time),'%Y-%m'),u.id) dateUser, u.id userId, count(*) uc
	from event e, production p, user u, contact c, payroll_service ps, payroll_preference pp
	where e.type='LOGIN_OK' 
		and e.id > 1030000 -- optimization to skip old data
		and start_time > '2019-01-01' -- Limit date range here 
		and start_time < '2019-12-31'  -- Limit date range here
		and username not like '%lightspeedeps%'
		and username not like '%theteamcompanies%'
		and u.email_address = e.username
		and c.user_id = u.id
		and c.production_id = p.id
		and p.payroll_preference_id = pp.id
		and pp.payroll_service_id = ps.id
		and ps.team_payroll = 1 -- 1: Team payroll clients; 0: non-Team payroll
		group by dateUser 
		order by date2 desc, userId
	) as userlogin
group by date2
order by  date2;

/* *
select count(distinct u.id)
from event e, production p, user u, contact c, payroll_service ps, payroll_preference pp
where e.type='LOGIN_OK'
	and username not like '%lightspeedeps%'
	and username not like '%theteamcompanies%'
	and e.id > 700000
	and e.start_time > '2018-01-01'  -- Limit date range here
	and e.start_time < '2018-02-01'  -- Limit date range here
	and u.email_address = username
	and c.user_id = u.id
	and c.production_id = p.id
	and p.payroll_preference_id = pp.id
	and pp.payroll_service_id = ps.id
	and ps.team_payroll = 1 -- 1: Team payroll clients; 0: non-Team payroll
;

/* Count of documents processed (signed) *
select count(*) 
from contact_document cd, contact c, production p, payroll_service ps, payroll_preference pp
where
	c.production_id = p.id
	and cd.contact_id = c.id
	and p.payroll_preference_id = pp.id
	and pp.payroll_service_id = ps.id
	and ps.team_payroll = 1 -- 1: Team payroll clients; 0: non-Team payroll
	and cd.delivered > '2018-01-01' -- use for time-related grouping/limiting
	and cd.status <> 'pending' and cd.status <> 'open' and cd.status <> 'void';

/* */
/* Count of timecards processed, and total $'s calculated. *
select count(*), sum(w.grand_total)
from weekly_time_card w, production p, payroll_service ps, payroll_preference pp
where w.prod_id = p.prod_id
	and p.payroll_preference_id = pp.id
	and pp.payroll_service_id = ps.id
	and ps.team_payroll = 1 -- 1: Team payroll clients; 0: non-Team payroll
	and w.end_date > '2018-01-01' -- use w.end_date to split into monthly chunks
	and (w.status = 'submitted' or w.status = 'approved' or w.status = 'locked')
	and w.grand_total is not null; -- indicates calculated

/* */
/* Get number of UNIQUE users logging in per month *
select count(*) num, date2
from (
	select date_format(date(start_time),'%Y-%m') date2, 
		concat(date_format(date(start_time),'%Y-%m'),username) dateUser, username, count(*) uc
	from event  where type='LOGIN_OK' 
		and username not like '%lightspeedeps%'
		and username not like '%theteamcompanies%'
		and start_time > '2018-01-01' -- Limit date range here 
		group by dateUser 
		order by date2 desc, username
	) as userlogin
	group by date2
order by  date2;

/* count unique users per month, restricted to Team clients! (Much slower than above, like 2 minutes)
select count(*) num, date2
from (
	select date_format(date(start_time),'%Y-%m') date2, 
		concat(date_format(date(start_time),'%Y-%m'),u.id) dateUser, u.id userId, count(*) uc
	from event e, production p, user u, contact c, payroll_service ps, payroll_preference pp
	where e.type='LOGIN_OK' 
		and e.id > 700000 -- optimization to skip old data; 1/1/2018=726000
		and username not like '%theteamcompanies%'
		and start_time > '2018-01-01' -- Limit date range here 
		and u.email_address = e.username
		and c.user_id = u.id
		and c.production_id = p.id
		and p.payroll_preference_id = pp.id
		and pp.payroll_service_id = ps.id
		and ps.team_payroll = 1 -- 1: Team payroll clients; 0: non-Team payroll
		group by dateUser 
		order by date2 desc, userId
	) as userlogin
group by date2
order by  date2;

/* */
-- find users who logged in within any particular time-frame
/* *
select sum(cnt), sum(1) from (
select count(*) cnt, username from event where type='LOGIN_OK' 
	and start_time > '2015-02-01' and start_time < '2015-03-01'
	and username not like '%lightspeedeps.com' and username not like '%taton.com' and username not like '%harmfamily.org'
	group by username
	order by count(*) desc
	) T1 ;

/* *
-- Get email addresses for mailing, from User records ...
select email_address, first_name, last_name,
	substr(email_address, instr(email_address,'@')+1) domain,
	right(email_address, instr(reverse(email_address),'.')-1) tld
from user where status = 'REGISTERED'
	and email_address not like '%lightspeedeps.com' 
	and email_address not like '%taton.com' 
	and email_address not like '%harmfamily.org'
order by tld, domain, email_address;

/* */
/* Get number of unique users logging in per hour (top 50) *
select * from (
    select count(*) num, hr, date_format(date1,'%Y-%m-%d') date2
    from (
		    select date(start_time) date1, hour(start_time) hr, id
		    from event  where type='LOGIN_OK' and username not like '%lightspeedeps%' group by username 
		    ) as userlogin
		group by hr, date(date1)
    order by num desc, date1 limit 20 ) as top
 order by date2 desc;

/* Get number of unique users logging in per day (top 20) * (CORRECTED 4/8/15) *
    select count(*) num, date_format(date1,'%Y-%m-%d') date2
    from (
		    select date(start_time) date1, concat(date_format(date(start_time),'%Y-%m-%d'),username) dateUser, username, count(*) uc
		    from event  where type='LOGIN_OK' and username not like '%lightspeedeps%' group by dateUser
				order by date1 desc, username
		    ) as userlogin
		group by date1
    order by num desc, date1 limit 20;

/* Get number of unique users logging in per month (top 20) * (CORRECTED 4/8/15)
    select count(*) num, date2
    from (
		    select date_format(date(start_time),'%Y-%m') date2, 
								concat(date_format(date(start_time),'%Y-%m'),username) dateUser, username, count(*) uc
		    from event  where type='LOGIN_OK' and username not like '%lightspeedeps%'
				and start_time > '2017-01-01' -- MAY BE USED TO LIMIT DATA 
				group by dateUser 
				order by date2 desc, username
		    ) as userlogin
		group by date2
    order by num desc, date2 limit 20;
/* */

/*
-- unique users, login counts, & their productions logged into, by month...

		    select count(*) cnt, username, date_format(date(start_time),'%b %Y') date1, pd.title
		    from event, project pj, production pd
		    where event.type='LOGIN_OK' and username not like '%lightspeedeps.com'
		    and username not like '%taton.com'
		    and start_time > '2013-01-01' and start_time < '2014-01-01'
		    and project_id is not null
		    and pj.id = project_id
		    and pd.id = pj.production_id
		    group by username, pd.id, date1
		    order by username, date1 desc, project_id -- cnt desc 
/* */

/*
-- summary, unique user count & login count, by production, by month:

select count(*) num, sum(cnt), date2, ptitle from

	(select count(*) cnt, username, date_format(date(start_time),'%Y-%m') date2, pd.title ptitle
	from event, project pj, production pd
	where event.type='LOGIN_OK' and username not like '%lightspeedeps.com'
	and username not like '%taton.com'
	and start_time > '2013-01-01' and start_time < '2014-01-01'
	and project_id is not null
	and pj.id = project_id
	and pd.id = pj.production_id
	group by username, pd.id, date2) as userlogin
	
group by date2, ptitle
order by date2, num desc
/* */

/*
-- unique users & total logins per month

select count(*) num, sum(cnt), date2 from

	(select count(*) cnt, username, date_format(date(start_time),'%Y-%m') date2, pd.title ptitle
	from event, project pj, production pd
	where event.type='LOGIN_OK' and username not like '%lightspeedeps.com'
	and username not like '%taton.com'
	and start_time > '2013-01-01' and start_time < '2015-01-01'
	and project_id is not null
	and pj.id = project_id
	and pd.id = pj.production_id
	group by username, pd.id, date2) as userlogin
	
group by date2
order by date2, num desc
/* */

/* Timecard submit/approve activity, by Day of week (%a=weekday name); change type to 'submit' for those! *
select count(*), date_format(date,'%a') date2, date_format(date,'%w') dayNum from time_card_event 
	where (type='submit' or type='approve') and 
	(date >= '2015-02-13') 
	group by date2 order by dayNum; -- count(*) desc;
/* *
	((date >= '2014-08-31' and date <= '2014-11-22') or (date >= '2014-12-07' and date <= '2014-12-20')) 
/* */

/*  Timecard approve activity, by date.
select count(*), type, date_format(date,'%Y-%m-%d') date2 from time_card_event where type in ('approve','submit') and 
date < '2015-12-31' and date > '2015-02-13'
 group by type, date2; 
/* */

/****************************** SQL for Timecard usage by production / project (job) / unique users ************/
/*
-- Count unique user accounts using timecards:

select count(*) from 
(
select count(*), user_account, last_name, first_name from weekly_time_card 
where end_date > '2017-01-01'
and status <> 'open'  -- removing this filter added about 500 total unique users
and job_name is  null  -- use for commercial; or job_number is not null -- same result
group by user_account
) tcusers ;


-- unique Productions using timecards:
select count(*), prod_co, prod_name, p.type 
from weekly_time_card w, production p
where w.end_date > '2017-01-01'
and w.status <> 'open'
and w.prod_id = p.prod_id
-- and p.type = 'FEATURE_FILM' -- TV_COMMERCIALS, TELEVISION_SERIES, FEATURE_FILM
group by prod_name
order by prod_co, prod_name;

-- unique commercial jobs using timecards:

select count(*), p.prod_id, prod_name, job_name 
from weekly_time_card w, production p
where w.end_date > '2017-01-01'
and w.status <> 'open'
and w.prod_id = p.prod_id
-- Eliminate test productions:
and prod_name not like 'demo -%'
and prod_name not like 'Team QA%'
and prod_co not like '% - TEAM'
and prod_co <> 'Hot Dog Enterprises'
and prod_co <> 'm squared productions'
and p.type = 'TV_COMMERCIALS'
group by w.prod_id, job_name
order by prod_name, job_name;

/*
unique users logged in:
02/2015 - 1194 (7509 logins)
01/2015 - 1077 (7013 logins)
11/2014 - 387
08/2014 - 434
11/2012 - 159
10/2012 - 197
since beginning (1/10/12) - >2000 users (as of 3/1/15)
*/

/* from 3/xx/2019? *
48K users
8K self-created
40k invited

6600 users w/approved CDs
13k w/ either approved CD or payroll start
28k w/ either  approved CD or any payroll start or non-open timecard 
24k w/ either  approved CD or non-open timecard 
9600 w/ signed W4
26450 w/ signed w4 or user.ssn or non-open timecard
9k invited w/SSN
(350 self-created w/SSN)

/* */
/* */
