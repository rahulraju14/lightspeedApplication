SET foreign_key_checks = on;
-- Misc fragments of SQL related to onboarding 

/* DISTRIBUTE Direct Deposit to ONE production only:
	-- see createOneDirectDeposit.sql */

/* FESTIVOL query for submitted document status:
	-- see festivolQuery.sql */

/* Find all CD's in specific Production:

select cd.* from contact c, contact_document cd where 
	c.production_id in (25)
	and cd.contact_id = c.id

--  DELETE all CD's in specific Production

delete cd from contact c, contact_document cd where 
	c.production_id in (25)
	and cd.contact_id = c.id

/* 5/2/17  RECENT SIGNATURES
-- Select most recent document signature events and display all the related info 

select u.first_name, u.last_name, u.email_address, e.occupation, d.name,
	ce.*, c.*, cd.*
		from contact_doc_event ce, contact_document cd, 
		contact c, user u, document d, employment e 
	where cd.id = ce.contact_document_id
	and cd.document_id = d.id
	and cd.employment_id=e.id
	and cd.contact_id = c.id
	and c.user_id = u.id
--	and form_type='other'					-- to restrict to custom docs
--	and ce.last_name='snoonian' 			-- approver's last name
order by ce.id desc limit 50

/* */
/* Update SOCIAL SECURITY on matching documents from USER.SOCIAL_SECURITY

set @mail= 'jj@taton.com';
set @ss = (select social_security from user where email_address = @mail );

 update form_i9 set social_security = @ss where  first_name='Joseph' and last_name='Jenkins';
  update form_w4 set social_security = @ss where first_name='Joseph' and last_name='Jenkins';
 update start_form set social_security = @ss where first_name='Joseph' and last_name='Jenkins';
/* */

/* 		FIND PRODUCTION FROM DOCUMENT_CHAIN

-- Find the production given a document_Chain id, e.g., from a ContactDocument or Document.
-- Note: if you parameterize the document_chain id, this runs MUCH slower (tens of seconds)
select * from production where repository_id = 
	(select id from folder where id =
		(select parent_id from folder where id =  
			(select folder_id from document_chain where id = 217 ) -- the document_Chain id
		)
	);
/* */

/* *
-- Find CD's that are wrong - CD-> employment E, and E.project_id != CD.project_id

select pj.id, c.production_id, pj.production_id, pj.title, e.id, e.project_id, pj2.production_id, pj2.title, e.occupation, cd.* 
	from contact_document cd, employment e, project pj, project pj2, contact c where 
	cd.employment_id = e.id 
	and cd.project_id = pj.id
	and e.project_id <> pj.id
	and e.project_id = pj2.id
	and cd.contact_id = c.id
	order by pj.id, contact_id, e.id
	 
/* *
select cd.id, u.account_number, cd.contact_id, cd.document_id, cd.form_type, cd.status, cd.employment_id
, c.email_address
, p.title
-- , e.id, e.contact_id, e.occupation, e.project_id, 
-- , f.id, f.name
, c.display_name
from contact_document cd, user u, contact c, production p
, start_form f
-- , employment e
-- , form_wtpa f
-- , form_w4 f
where 
 cd.form_type = 'start' and
cd.related_form_id is null and cd.form_type <> 'other'
and cd.contact_id = c.id
and c.production_id = p.id
and c.user_id = u.id
-- and e.id = cd.employment_id
-- and cd.contact_id = e.contact_id
 and f.first_name = u.first_name
-- and f.email = u.email_address
-- and f.user_account = u.account_number
 order by u.account_number, cd.id, cd.form_type, cd.contact_id ;
/* *
select cd.id, cd.contact_id, e.contact_id, document_id, form_type, employment_id, e.id, e.occupation, e.project_id from contact_document cd
, employment e
-- , start_form s
where 
cd.related_form_id is null and form_type <> 'other'
and e.id = cd.employment_id
and cd.contact_id = e.contact_id
order by form_type, cd.contact_id;
/* *
select cd.id, cd.contact_id, document_id, form_type, employment_id, e.id, e.contact_id, e.occupation, e.project_id 
from contact_document cd
, employment e
where  e.id = cd.employment_id and cd.contact_id <> e.contact_id
/* */
-- select * from contact_document where related_form_id is null;

/* *
-- delete 
select distinct id
from start_form  
where project_id in (6945, 6946, 6947, 6907, 6928) and prod_company = 'festiVOL' limit 600;

/* */
/* 
	-- delete
	select distinct id
	from contact_document  where
	status = 'pending' and form_type='start' and 
	project_id in (6945, 6946, 6947, 6907, 6928) ;

/*
select * from form_i9 where citizenship_status <> 'citizen' order by id desc limit 30;
select * from form_i9  order by id desc limit 30;
/* 
-- update form_i9 set sec2_middle_initial = middle_name where  version = 2 and  
	((middle_name is not null and sec2_middle_initial is null)) limit 200;

-- update form_i9 set sec2_last_name = last_name, sec2_first_name=first_name where  version = 2 and  
	((last_name is not null and sec2_last_name is null)) limit 200;

-- update form_i9 set sec2_citizenship_status = citizenship_status where  version = 2 and  
	(citizenship_status is not null and sec2_citizenship_status is null) limit 200;

/* *
select * from form_i9 where version = 2 and ( (last_name <> sec2_last_name)
 or (middle_name is not null and sec2_middle_initial is null) ) order by id desc limit 10;


/* *
select count(*)
	from contact_document cd, form_i9 i  where
	status = 'submitted' and form_type='i9' and 
	project_id in (6945, 6946, 6947, 6907, 6928) 
	and related_form_id = i.id
	and version = 2
	and  (last_name is not null and sec2_last_name is null);

/* *
select c.email_address, p.title, cd.*, cde.* 
	from contact_document cd, contact_doc_event cde, contact c, production p
	where
	cde.contact_document_id = cd.id
	and cd.contact_id = c.id
	and c.production_id = p.id
	-- and cde.type='submit'
	and cd.status = 'submitted'
	and form_type <> 'other'
	and cd.related_form_id is null

/* *
select c.email_address, p.title, cd.*, cde.* 
	from contact_document cd, contact_doc_event cde, contact c, production p
	where
	cde.contact_document_id = cd.id
	and cd.contact_id = c.id
	and c.production_id = p.id
	and cde.type='Approve'
	and cd.status = 'submitted'
	and cd.approver_id is null
/*
and form_type='i9'
order by cde.id desc
limit 30;

/*
select * from contact_doc_event cde
order by id desc
limit 30;

/*
select * from production pd, payroll_preference pp where
	pd.payroll_preference_id = pp.id and
	team_eor = 'TEAM_D';

select * from project p, payroll_preference pp where
	p.payroll_preference_id = pp.id and
	team_eor = 'TEAM_D';

/*
select distinct contact0_.Id as Id19_ from contact contact0_ 
cross join project_member projectmem1_ 
cross join unit unit2_ 
cross join employment employment3_ 
where projectmem1_.Employment_Id=employment3_.Id 
and employment3_.Contact_Id=contact0_.Id 
and contact0_.Production_Id=5990
and (projectmem1_.Unit_Id is null or (projectmem1_.Unit_Id=unit2_.Id and unit2_.Project_id=6907))

/*
select * from contact_document where status = 'OPen' and approver_id is not null;

--  update project_member set unit_id = null where employment_id = 87784 and id = 68163;
-- update employment set project_id = null where role_id = 422 and id = 87784;
-- update contact_document set status = 'SUBMITTED' where id = 2786 and approver_id < 0 and contact_id = 39643;

-- update contact_document set employment_id = 87918 where id in (12519,12520) and project_id = 6944 and contact_id = 46946;
/*
delete from contract_rule where id = 900223;
insert into contract_rule values(900223,1,'223',"LA-80",null,"80","N_A","N_A","N_A","ON","N_A","N_A","N_A",null,"HO",'HO-SS-PH',230,'','');


/*
select distinct production_id from audit_event where summary like '%Cume%';
/* 3938 (CH), 5386 (ranch)
ranch: cruz, asst editor 700, 4173 / C-2 
WK-CUME-45-A; CB-NONE; GL-EL-12-2X; GT-7-1X; HO-STD-P; MP-LA; NP-NONE; On-call: N/A; OT-NONE; RST-9-PR; SP_GOLD_NO_DED_MEAL


select * from audit_event where production_id=3938 and summary like '%Cume%' order by id desc limit 20;
/*
select * from start_form where contract_schedule = 'B';

/* *
-- update contact_document  set status = 'SUBMITTED' where status =  'APPROVED_PAST' ;

select * from contact_document  where status not in  ('SUBMITTED_NO_PATH', 'APPROVED', 'OPEN', 'LOCKED', 'SUBMITTED', 'VOID', 'PENDING', 'RECALLED', 'REJECTED') ;

/*
select * from address where zip is not null and zip <> "" and length(zip) < 5 and (country = 'us' or country is null);

/*
/* *
-- ls eps admin mask for onboarding = 6917529027641081855
-- was 2305843009213693951 admin.view?

/* */
/* */
