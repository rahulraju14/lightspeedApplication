SET foreign_key_checks = on;

/* ****  FESTIVOL QUERY for Approved documents ****

set @pId = '5990'; -- (select id from production where title = 'Firefly Festival'); -- production previously called 'FestiVol'

select email, first_name, last_name, Project, max(numApp) Approved, 
		(case max(numApp) when 2 then 'Y' else 'N' end) DONE, (case fVemail when email then '' else fVemail end) fV_email
 from (
	(
	select count(*) numApp, c.id,  cd.status, p.title Project,  u.first_name, u.last_name, u.email_address email, c.email_address fVemail 
	from contact_document cd, project p, contact c, user u where
	cd.status = 'approved' and
	c.production_id = @pid and cd.project_id = p.id and cd.contact_id = c.id and c.user_id = u.id 
	group by cd.project_id, cd.contact_id
	)
) unionTable
where project = 'electric forest 2019' or project = 'electric forest post' -- project restriction added 6/23/19
group by project, id order by Done desc, email, last_name, first_name;


/* ****  FESTIVOL QUERY for Complete/Incomplete documents ****
-- note: user.created_by = 'A0123802' for 'noreply...'

set @pId = '5990'; -- (select id from production where title = 'Firefly Festival'); -- production previously called 'FestiVol'

select email, first_name, last_name, Project, max(numOpen) Incomplete, max(numSub) Complete, 
		(case max(numOpen) when 0 then 'Y' else 'N' end) DONE, (case fVemail when email then '' else fVemail end) fV_email
 from (
	(
	select count(*) numOpen, 0 numSub, c.id,  cd.status, p.title Project,  u.first_name, u.last_name, u.email_address email, c.email_address fVemail 
	from contact_document cd, project p, contact c, user u where
	cd.status = 'open' and
	c.production_id = @pid and cd.project_id = p.id and cd.contact_id = c.id and c.user_id = u.id 
	group by cd.project_id, cd.contact_id
	)
union (
	select 0 numOpen, count(*) numSub, c.id,  cd.status, p.title Project,  u.first_name, u.last_name, u.email_address email, c.email_address fVemail 
	from contact_document cd, project p, contact c, user u where
	(cd.status <> 'open' and cd.status <> 'pending' and cd.status <> 'void') and
	c.production_id = @pid and cd.project_id = p.id and cd.contact_id = c.id and c.user_id = u.id 
	group by cd.project_id, cd.contact_id
	)
) unionTable
where project = 'electric forest 2019' or project = 'electric forest post' -- project restriction added 6/23/19
group by project, id order by project, Done desc, email, last_name, first_name;
	 

/**  Query for Complete/Incomplete documents, includes Occupation -- not the usual report **
select email, first_name, last_name, Project, max(numOpen) Incomplete, max(numSub) Complete, 
		(case max(numOpen) when 0 then 'Y' else 'N' end) DONE, (case fVemail when email then '' else fVemail end) fV_email, occupation
 from (
	(
	select count(*) numOpen, 0 numSub, c.id,  cd.status, p.title Project,  u.first_name, u.last_name, u.email_address email,
	 c.email_address fVemail, e.occupation 
	from contact_document cd, project p, contact c, user u, employment e where
	cd.status = 'open' and
	cd.employment_id = e.id and
	c.production_id = @pid and cd.project_id = p.id and cd.contact_id = c.id and c.user_id = u.id 
	group by cd.project_id, cd.contact_id
	)
union (
	select 0 numOpen, count(*) numSub, c.id,  cd.status, p.title Project,  u.first_name, u.last_name, u.email_address email,
	 c.email_address fVemail, e.occupation 
	from contact_document cd, project p, contact c, user u, employment e where
	cd.employment_id = e.id and
	(cd.status <> 'open' and cd.status <> 'pending' and cd.status <> 'void') and
	c.production_id = @pid and cd.project_id = p.id and cd.contact_id = c.id and c.user_id = u.id 
	group by cd.project_id, cd.contact_id
	)
) unionTable
-- where occupation <> 'staff assistant'
group by project, id order by Done desc, email, last_name, first_name;

/* *
-- delete 
select distinct id
from start_form  
where project_id in (6945, 6946, 6947, 6907, 6928) and prod_company = 'festiVOL' limit 600;

/* */
/* */
