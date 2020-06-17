SET foreign_key_checks = on;
/*

/* FIXING FONTS or other issues in REALWORLDELEMENT - MAP / DIRECTIONS data

 select * from   real_world_element rw  where rw.id = 3599;
 set @dir = (select directions from   real_world_element rw  where rw.id = 3599);
select cast(@dir as char(1000));
/*
update real_world_element set directions = replace(directions,
'li style="font-size: 11.000000pt; font-family: \'Roboto\'"',
'li')
where id = 3599;

update real_world_element set directions = replace(directions,
'li style="font-size: 11pt; font-family: Roboto;"',
'li')
where id = 3599;

update real_world_element set directions = replace(directions,
'failFont',
'Times New Roman')
where id = 3599;

update real_world_element set directions = replace(directions,
'\'Roboto\'',
'\'Times New Roman\'')
where id = 3599;

update real_world_element set directions = replace(directions,
'Times New Roman', 'failFont;')
where id = 3599;

/* *
-- find user's with same first & last names but different account #'s, with timecards.
select * from user u, 
(
	select * from
	(
		select count(*) cnt, tab.* from
		(
			select user_account, last_name, first_name from weekly_time_card 
			where user_account not like 'g_%'
			group by user_account, last_name, first_name
			) tab
		group by last_name, first_name
		) tab2
	where cnt > 1
	order by last_name, first_name
	) t
where u.last_name = t.last_name and u.first_name = t.first_name
order by u.last_name, u.first_name, account_number

/* */
/* *
-- checking for possible duplicate pay-rate entries
select p1.id, p2.id, o1.union_code, o1.occ_code, o1.name, p1.contract_code, p2.*, p1.* 
from Pay_Rate p1, Pay_Rate p2, occupation o1
where
(o1.id = p1.id or o1.id = p2.id) and
p1.occ_code = p2.occ_code and
p1.schedule = p2.schedule and
p1.locality = p2.locality and
p1.id < 10000 and p2.id < 10000 and
p1.id < p2.id and
p1.contract_code < p2.contract_code
group by p1.occ_code
order by p1.id
/* *
select p1.id, p2.id, p1.contract_code, p2.*, p1.* from Pay_Rate p1, Pay_Rate p2
where
p1.occ_code = p2.occ_code and
p1.schedule = p2.schedule and
p1.locality = p2.locality and
p1.id < 10000 and p2.id < 10000 and
p1.id < p2.id and
p1.contract_code < p2.contract_code
group by p1.occ_code
order by p1.id
/* */

/* 1/20/2014 Find all roles in use by all productions, grouped by role: *
select r.id, r.name, count(r.id) 
-- , pr.id, pr.status, pr.title 
 from project_member pm, role r, unit u, project pj, production pr 
 where
pm.role_id = r.id and 
r.id > 9999 and
unit_id is not null and
u.id = unit_id and
pj.id = u.Project_id and
pr.id = pj.Production_Id and
-- eliminate some for various reasons...
pr.id > 11 and -- test productions 
pr.sku not like '%-ED-%' and -- Student productions (EDucational)
pr.owning_account <> 'a010003' and -- owned by JF 
pr.studio <> 'indiepay' and
pr.id not in (92,947)  -- media services
group by r.id
order by count(r.id) desc, r.name, r.id -- for a couple of duplicated names (in different departments)
/* */
/* See preferred styles of phone numbers (most just used '-'s) *
select phone from production where phone is not null and phone <> '' and phone like '(%';
select phone from production where phone is not null and phone <> '' and phone like '%-%' and phone not like '(%';
select phone from production where phone is not null and phone <> '' and phone like '%.%' and phone not like '(%';
select phone from production where phone is not null and phone <> '' and 
	phone not like '+%' and phone not like '0%' and phone not like '%.%' and phone not like '%-%';
/* */
/*
-- 1/8/2014 related to custom departments and custom roles ...
-- find custom departments and owning production names
select d.name, p.title from department d, production p
where d.id > 999 and p.id = d.production_id 
and d.std_dept_id is null
order by d.name;

/*
-- find all custom roles that are not assigned to anyone
select  r.id, r.name from  role r  where r.id > 9999 
and id not in (select role_id from project_member pm);

/* custom roles in more than 15 productions(?) and more than 30 start forms  *
select * from
(
select count( s.id) c2, dept_name, rname from start_form s,
(
select count(r.id) cnt,  r.name rname, d.id did, d.name, pd.title, pd.id 
from project_member pm, role r, unit u, project p, production pd,department d  
where pm.role_id > 9999
-- and d.id > 999
and pm.role_id = r.id
and u.id = pm.unit_id and p.id = u.project_id and pd.id = p.production_id
and r.department_id = d.id
group by r.name, d.id 
) tab1

where s.job_class = rname
and cnt > 15
group by rname, dept_name
) tab2
where c2 >= 30
order by dept_name, rname;

/* *
-- Find all custom roles currently in use, with their dept & production: 
select count(r.name),  r.name, d.id, d.name, pd.title, pd.id from project_member pm, role r, unit u, project p, production pd,department d  
where pm.role_id > 9999
-- and d.id > 999
and pm.role_id = r.id
and u.id = pm.unit_id and p.id = u.project_id and pd.id = p.production_id
and r.department_id = d.id
group by r.name
order by count(r.name) desc, r.name, d.name;
/*
select * from role r, department d where r.id > 9999
 and r.department_id = d.id
 order by r.name, department_id;

select count(*) from project_member;
select * from project_member where role_id >= 390 and role_id <= 396;
select * from project_member where role_id in (41,42,46,47);

/*  11/12/2013 - create list of unique occupation names, with or without unions considered.

select o.* from occupationz o order by o.name, o.id;

select o.*, u.name from  occupationz o, unions u 
where  o.union_code = u.occupation_union and number not in ('38','477','481') order by  o.name, o.id;
/*
select * from occupationz where id not in 
(select o.id from  occupationz o, unions u 
where  o.union_code = u.union_key ) order by id

/*
truncate occupationz;
insert ignore into occupationz select id, union_code,name from occupation 
	where union_code not in ('ialb2', 'ialb3', 'asa-nm', 'hw-lv', 'asa-dc');

/*
DROP Table if exists	Occupationz	;		
CREATE Table	Occupationz	(		
Id	Integer	NOT NULL PRIMARY KEY	AUTO_INCREMENT,	-- unique database key
Union_Code	varchar(10)	NOT NULL,		-- Union identifier [OMIT this column to generate PURELY unique names]
Name	varchar(200)	NOT NULL,		-- Full occupation name (job classification)
unique (union_code,name)
	)		engine "innodb";	
/*
select o.*, u.name from  occupation o, unions u 
where  o.union_code = u.union_key group by o.name order by o.id
/*
select x.*, o.union_code, u.name from occupationx x, occupation o, unions u 
where o.id = x.id and o.union_code = u.union_key order by x.id

/*
-- code used by discrepancy report to gather DPR, timecard, etc.
-- weekly_time_card.id value
set @wid = 732; -- 732,728,741,743,734
-- production.id value
set @pid = 25;

select tc.id, tc.role, wtc.occupation, tc.wrap, dt.*, tc.*, dpr.*
from daily_time dt
 right join weekly_time_card wtc on  dt.weekly_id = wtc.id
 right join user on user.account_number = wtc.user_account
 right join contact on contact.user_id = user.id and contact.production_id = @pid
 left join 
 (time_card tc join dpr on tc.dpr_crew_id = dpr.id) 
 	on tc.contact_id = contact.id and dpr.date = dt.date and (tc.role = wtc.occupation or tc.role is null)
where wtc.id = @wid
order by day_num;

/*
-- code to update folder info from production & parents when not originally set.
/*
update folder set created = (select p.start_date from production p
 where p.repository_id = folder.id
) where created is null;
/* */
-- copy / duplicate an entire table:
/*
CREATE TABLE tempFolder LIKE folder;
INSERT tempFolder SELECT * FROM folder;

update folder set created = (select tempFolder.created from tempFolder
 where folder.parent_id = tempFolder.id
 ) where created is null;

drop table tempFolder;

/* */
/*
select dept.name as deptname, cc.count, cc.role_name, cc.name as crewcallName, cc.time, cc.call_type
from dept_call dc, crew_call cc, department dept
where dc.id in (734,735,736)
and cc.dept_call_id = dc.id
and dc.department_id = dept.id
order by dept.list_priority, line_number;
/*
SELECT table_schema, table_name, ( data_length + index_length ) / 
1024 / 1024 as "SizeMB" 
FROM information_schema.TABLES where table_schema = 'lightspeed' 
order by sizemb desc; 
/**
SELECT table_schema, sum( data_length + index_length ) / 1024 / 1024 
"Data Base Size in MB" 
FROM information_schema.TABLES GROUP BY table_schema ; 

/*
select distinct sce.hint scene_hint, sce.Script_Day,sce.id scene_id, sp.scene_Numbers scene_number, 
sp.Synopsis, sp.Elapsed_Time, sp.Sheet_Number, sce.IE_Type scne_IET, sce.DN_Type scene_DNT, 
IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length, 
sce.page_number scene_page_number from project proj, scene sce, strip sp 
where proj.current_stripboard_id=sp.Stripboard_Id and sp.Type='BREAKDOWN' and 
concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) and 
sce.Script_Id=proj.Current_Script_Id 
and proj.id = 1009 
and sp.Status <> 'OMITTED'  
order by sp.Status, sp.OrderNumber;

/*
select count(*) from document where content like '%/TaggerDoc%>%';
/*
delete from date_event where id in (101, 102, 107, 108);
/*
select * from strip where stripboard_id in (101,227)
order by stripboard_id, ordernumber;
/* *
update user set locked_out = false;

/*
update user set encrypted_password = null where user_name like 'RG%';

/*
delete from Message_Instance;	
replace Message_Instance values 	
(101,101,'2010-02-26 09:22','EMAIL',TRUE,'2010-02-26 10:31',101)	,
(102,101,'2010-02-26 15:22','TEXT_MESSAGE',FALSE,null,102)	,
(103,101,'2010-02-27 13:52','TEXT_MESSAGE',FALSE,null,103)	,
(104,120,'2010-02-26 03:22','EMAIL',FALSE,null,101)	,
(105,120,'2010-02-26 16:24','TEXT_MESSAGE',TRUE,'2010-02-27 17:11',102)	,
(106,120,'2010-02-27 11:42','TEXT_MESSAGE',FALSE,null,103)	,
(107,120,'2010-02-27 09:45','TEXT_MESSAGE',FALSE,null,104)	,
(108,120,'2010-02-25 08:06','TEXT_MESSAGE',FALSE,null,105)	,
(109,120,'2010-02-26 18:38','TEXT_MESSAGE',FALSE,null,106)	,
(110,120,'2010-02-27 17:41','TEXT_MESSAGE',FALSE,null,107)	,
(111,120,'2010-02-26 05:52','TEXT_MESSAGE',FALSE,null,108)	,
(112,120,'2010-02-26 06:49','TEXT_MESSAGE',FALSE,null,109)	,
(113,120,'2010-02-27 13:12','TEXT_MESSAGE',FALSE,null,110)	,
(114,120,'2010-02-27 09:46','TEXT_MESSAGE',FALSE,null,111)	,
(115,120,'2010-02-24 11:52','TEXT_MESSAGE',FALSE,null,112)	,
(116,120,'2010-02-27 09:15','TEXT_MESSAGE',FALSE,null,113)	
;	

/*
delete from scene where id > 4;	
/*
select p.* from Strip p, stripboard s 
where s.id = 2
and p.stripboard_id = s.id
and p.scene_Numbers is not null
order by p.sheet_Number
/*
update strip set status = "SCHEDULED";
/*
SELECT * from text_element s where s.type = "other"; 

/* *
update project set current_script_id = null;
update project set current_stripboard_id = null;
delete from image where id = 11;
delete from script_writer;
delete from script;
delete from script_element;
delete from real_link;
delete from scene;
delete from scene_script_element;
delete from text_element;
delete from strip;
delete from stripboard;
delete from note;
delete from time_sheet;
delete from time_card;
delete from exhibit_g_detail;
delete from exhibit_g;
delete from cast_call;
delete from scene_call;
delete from crew_call;
delete from dept_call;
delete from call_note;
delete from other_call;
delete from callsheet;
delete from dpr_days;
delete from dpr_scene;
delete from catering_log;
delete from vehicle_log;
delete from extra_time;
delete from dpr;
delete from sound_stock;
delete from script_measure;

/*
replace Project values 	
(101,'Episode for Tom','ACTIVE',FALSE,null,TRUE,TRUE,TRUE,TRUE,'2009-11-10',null,null,101,101,'US/Pacific')	
;	
	
replace Contact values 	
(202,null,'Tom',null,'Keith','TK',202,203,19,FALSE,TRUE,FALSE,FALSE,FALSE,'310-555-1212','888-555-1212',null,0,null,FALSE,TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,TRUE,FALSE,FALSE,'FULL',FALSE,FALSE,FALSE,2,TRUE,FALSE,101,null,'YAHOO',null,null,null,null,null,FALSE,FALSE,FALSE,0,0,null)	
;	
	
replace Project_Member values 	
(202,202,19,TRUE,101)	
;	
	
replace User values 	
(202,'TomKeith',null,202,101,TRUE,FALSE,null,FALSE,0,'FILE_WRITE_DELETE',null)	
;	
	
replace Project_Schedule values 	
(101,'2009-10-17','1000001')	
;	
	
replace Address values 	
(202,'123 Wilshire Blvd',null,'Los Angeles','CA','90001',null,'PST',TRUE,null)	,
(203,'456 Side St',null,'Santa Monica','CA','90405',null,'PST',TRUE,null)	
;	
	
replace Folder values 	
(202,"Tom's files",202,FALSE,101)	
;	
	
replace Contract_State values 	
(1201,'NOT_DISTRIBUTED',102,202)	,
(1202,'NOT_DISTRIBUTED',103,202)	,
(1203,'NOT_DISTRIBUTED',104,202)	,
(1204,'NOT_DISTRIBUTED',105,202)	,
(1205,'NOT_DISTRIBUTED',106,202)	,
(1206,'NOT_DISTRIBUTED',107,202)	,
(1207,'NOT_DISTRIBUTED',108,202)	,
(1208,'NOT_DISTRIBUTED',109,202)	,
(1209,'NOT_DISTRIBUTED',110,202)	,
(1210,'NOT_DISTRIBUTED',111,202)	
;	
/* */