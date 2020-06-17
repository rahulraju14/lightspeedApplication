SET foreign_key_checks = on;
/* */
 set @mail = ''; --  -- '@lightspeedeps.com' 
;
-- set @mail = (select email_address from user where first_name= '' and last_name = '' order by id desc limit 1);

/* RESULTS: 1:User, 2:Contacts, 3:Prods, 4:PM/Roles, 5:ERs, 6:SFs, 7:CDs, 8:CD events, 9:Doc Chg events, 10:TCs, 11:Events/errors, 
		12:Changes, 13:TC events for user, 14:W4, 15:I9, 16:WTPA, 17: TC Events; 18: Contact DocEvents; 19:doc_change_event for last TC */

-- ls eps admin mask for onboarding = 6917529027641081855

 -- set @uid = 2; -- use this or @mail
 set @uid = ( select id from user where email_address like @mail limit 1) ;
 set @mail = ( select email_address from user where id = @uid) ;
 set @acct = ( select account_number from user where id = @uid) ;
 set @prodid = 0;
 set @sfid = 0;  set @contactid = 0; set @empid = 0; set @pmid = 0;
/* *
 set @prodid = '0'; -- #4759=bauer; #4954=Schrom; #3938=Chelsea; ET=2308;
 	-- @prodid -- use to restrict to a particular production
 set @contactid = (select id from contact where user_id = @uid and production_id = @prodid);
 set @empid = (select id from employment where contact_id = @contactid order by id desc limit 1);
 set @sfid = (select id from start_form sf where employment_id = @empid order by sf.id desc limit 1);
 set @pmid = (select id from project_member where employment_id = @empid order by id desc limit 1);

/* 1: User record */
select * from user where email_address like @mail ;

/* 2: CONTACTS for this user */
select * from contact where user_id = @uid and (CASE WHEN @prodid <= 0 THEN true ELSE production_id = @prodid END)
	order by production_id ;

-- MEMBERSHIP 
/* 3: PRODUCTIONS this user belongs to */
select pd.* from production pd, contact c where c.user_id = @uid 
		and c.production_id = pd.id
		and (CASE WHEN @prodid <= 0 THEN true ELSE pd.id = @prodid END) 
		order by pd.id;

/* 4: ROLES with Unit membership this user has in all productions. */
select pm.*, unit.number UnitNum, role.name, role.department_id, pj.id pj_id, pj.title, pj.code, pj.production_id, e.contact_id, e.def_role 
	from project_member pm, contact c, role, unit, project pj, employment e
	where c.user_id = @uid 
		and c.id = e.contact_id and pm.employment_id = e.id and e.role_id = role.id 
		and ((unit.id = pm.unit_id and pj.id = unit.project_id) 
				or (pm.unit_id is null and c.production_id = pj.production_id and pj.sequence = 1 and pj.id = unit.project_id))
		and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END) 
		group by pm.id order by production_id, e.id, e.role_id, pj.title;
		
/* 5: 3.1: Employment records */
select emp.*, hex(emp.permission_mask) from employment emp, contact c where c.user_id = @uid and emp.contact_id = c.id 
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	order by c.id, project_id;

/* 6: START FORMS */
-- All StartForms (with Shoot rate set) from the production whose id is set at the top
select sf.*, rs.* from start_form sf, contact c, start_rate_set rs where c.user_id = @uid and sf.contact_id = c.id
	and rs.id = sf.prod_rate_id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END);

/* 7: ContactDocuments */
select cd.* from Contact_document cd, contact c where c.user_id = @uid and cd.contact_id = c.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	order by contact_id, employment_id, cd.id desc;

/* 8: Contact Document Events */
select cde.*, cd.form_type, cd.status, cd.employment_id from contact_doc_event cde, Contact_document cd, contact c 
	where c.user_id = @uid and cd.contact_id = c.id
	and cde.contact_document_id = cd.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	order by cd.id, cde.date desc;

/* 9: Doc Change Events */
select dc.*, cd.form_type, cd.status, cd.employment_id from doc_change_event dc, Contact_document cd, contact c 
	where c.user_id = @uid and cd.contact_id = c.id
	and dc.contact_document_id = cd.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	order by cd.id, dc.date desc;

/* 10: TIME CARDS */
select * from weekly_time_card wtc where user_account = @acct
		and (CASE WHEN @sfid <= 0 THEN true ELSE wtc.start_form_id = @sfid END);

/* 11: Event Log */
select * from event where username like @mail order by id desc;

/* 12: Changes log - will show User creation + Contact creation(s) */
select * from changes c where user_name like concat('%',@mail,'%')
	and (CASE WHEN @prodid <= 0 THEN true ELSE (c.production_id = @prodid or c.production_id is null) END) 
	order by id desc;

/* 13: time_card_event - will show User's approve/reject/submit/pull actions */
select * from time_card_event where user_account = @acct order by id desc;

-- SET 'current TC' from last submitted one:
-- set @tcid = (select weekly_id from time_card_event where user_account = @acct and type='SUBMIT' order by id desc limit 1);
-- OR SET 'current TC' from last modified one, based on timecard audit events:
set @tcid = (select timecard_id from timecard_change_event where user_account = @acct order by id desc limit 1);

/* 14: form W4's */
select * from form_w4 where id in (
	select related_form_id from Contact_document cd, contact c where c.user_id = @uid and cd.contact_id = c.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	and form_type = 'w4' );

/* 15: form I9's */
select * from form_i9 where id in (
	select related_form_id from Contact_document cd, contact c where c.user_id = @uid and cd.contact_id = c.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	and form_type = 'I9' );

/* 16: form WTPA's */
select * from form_wtpa where id in (
	select related_form_id from Contact_document cd, contact c where c.user_id = @uid and cd.contact_id = c.id
	and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	and (form_type = 'ca_wtpa' or form_type = 'ny_wtpa') );

/* 17: time_card_event for the most recent timecard; i.e., signature events *
select tce.*, u.display_name, u.email_address from time_card_event tce, user u 
	where weekly_id = @tcid and new_id = u.id order by tce.id desc;
	
/* 17: timecard_change_event for the most recent timecard; i.e., any edits to timecard */
select tce.* from timecard_change_event tce
	where timecard_id = @tcid order by tce.id desc;


/* 18: Contact Document Events performed by this user */
select cde.*, cd.form_type, cd.status, cd.employment_id from contact_doc_event cde, Contact_document cd
	where cde.user_account = @acct 
	and cde.contact_document_id = cd.id
	-- and (CASE WHEN @prodid <= 0 THEN true ELSE c.production_id = @prodid END)
	order by cd.id, cde.date desc;

/* 19: doc_change_event - will show User's change actions on any doc (their own or others) */
select * from doc_change_event where user_account = @acct order by id desc;

/* 20: debug output: */
select @uid, @acct, @prodid, @contactid, @empid, @sfid, @pmid, @tcid;

/* Sample telephone number search: *
set @phone='310%666%9818';
select * from start_form where phone like @phone;
select * from user where cell_phone like @phone or home_phone like @phone or business_phone like @phone;
select * from contact where cell_phone like @phone or home_phone like @phone or business_phone like @phone;
/* */

-- select *, oct(permission_mask),  hex(permission_mask), conv(permission_mask,10,2) from employment e where id = @empid limit 1;

/* *** MISCELLANEOUS UPDATES FOLLOW - These have been useful in various customer support issues ... ***/
-- update user set password = null, locked_out = 0 where id = @uid and email_address like @mail ;
-- update employment e set permission_mask = 6917529027641081855 where id = 3844;

/* Switch an SF from one PM/ER pair to another one *
update Start_form set project_member_id = @pmid, employment_id = @empid where
	contact_id = @contactid and id = @sfid limit 1;

/* Switch an SF to a whole nother person! *
update Start_form set employment_id = @empid, project_member_id = @pmid, contact_id = @contactid, form_number='-1' where
	 id = x limit 1;

/* Update a timecard for Dept/Role change *
update weekly_time_card set department_id=127, dept_name='Post Production' where end_date >= '2016-10-08' 
	and user_account = @acct and prod_id = 'P26' and department_id = 101 limit 3;

/*
chelsea:
2585=Field Production
2582=Stage
ET-TI custom depts:
1648=Tape Vault
1741=Stage-Booth
1921=Newsroom
1956=Clearance
2004=Clearance-Insider
2001=Decades
regular:
101-Production
119-Production Office
127-Post Production
142-Assistants
*/
	
/* */
/* : SCRIPTs in this user's productions. *
select s.* from script s, project p, production pd, contact c where s.project_id = p.id and 
		p.production_id = pd.id and c.user_id = @uid and c.production_id = pd.id
		order by pd.id, p.id, s.date;
/* */
/* : UNITs in this user's productions.
select u.* from unit u, project p, production pd, contact c where u.project_id = p.id and 
		p.production_id = pd.id and c.user_id = @uid and c.production_id = pd.id
		order by pd.id, p.id, u.number; /* */
/* : UNIT_STRIPBOARDs in this user's productions.
select usb.* from UNIT_STRIPBOARD usb, unit u, project p, production pd, contact c where usb.unit_id = u.id and
		u.project_id = p.id and p.production_id = pd.id and c.user_id = @uid and c.production_id = pd.id
		order by usb.unit_id, usb.id; /* */
/* : STRIPBOARDs in this user's productions. *
select sb.* from stripboard sb, project p, production pd, contact c 
		where sb.project_id = p.id and
		p.production_id = pd.id and c.user_id = @uid and c.production_id = pd.id
		order by sb.project_id, sb.revision;
/* */
/* */
