SET foreign_key_checks = on;

-- SQL for validating issues related to onboarding tables

/* *
-- Check for case of two (or more) ContactDocument records referencing the same Related_form_id.

drop table if exists temp_cd_dup_forms;
create table temp_cd_dup_forms 
		select count(*) cnt, related_form_id, form_type from contact_document cd where related_form_id is not null
		group by related_form_id, form_type
		order by count(*) desc;

delete from temp_cd_dup_forms where cnt < 2;

select cd.* from contact_document cd, temp_cd_dup_forms t where 
cd.related_form_id = t.related_form_id
and cd.form_type = t.form_type
order by t.related_form_id, t.form_type, delivered;

/* */
/* *
-- Find CD's that are wrong - CD-> employment E, and E.project_id != CD.project_id

select pj.id, c.production_id, pj.title, e.id E_Id, e.project_id E_ProjId, c.display_name, pj2.title, e.occupation, cd.* 
	from contact_document cd, employment e, project pj, project pj2, contact c where 
	cd.employment_id = e.id 
	and cd.project_id = pj.id
	and e.project_id <> pj.id
	and e.project_id = pj2.id
	and cd.contact_id = c.id
	order by pj.id, contact_id, E_ProjId, e.id

/* */
/* */
