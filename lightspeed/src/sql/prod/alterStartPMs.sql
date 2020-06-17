SET foreign_key_checks = on;
-- Repair the Start_Form.project_member_id fields, so that the PM's they identify
-- belong to the same project as Start_Form.project_id.

/* -- insert temporary column */
Alter table start_form add pm_pj_Id integer null after project_member_id;

/* -- If there's NO SF.project_member_id, pick any one for this contact... */

update start_form sf, project_member pm
	set sf.project_member_id = pm.id
	where sf.project_member_id is null
	and sf.contact_id = pm.contact_id;

/* -- compute project_id based on current project_member_id: */

update start_form sf, project_member pm, unit u
	set sf.pm_pj_id = u.project_id
	where sf.project_id is not null 
	and sf.project_member_id = pm.id
	and pm.unit_id = u.id;

/* -- how many are wrong? */
select count(*), 'Invalid proj-id values(1)' from start_form where project_id is not null 
	and (project_id <> pm_pj_id or pm_pj_id is null);

/* -- Fix the wrong ones if possible: */
update start_form sf, project_member pm, project pj, unit u
	set sf.project_member_id = pm.id
	where sf.project_id is not null
	and (sf.project_id <> sf.pm_pj_id or sf.pm_pj_id is null)
	and sf.project_id = pj.id
	and sf.contact_id = pm.contact_id
	and u.project_id = pj.id
	and pm.unit_id = u.id;

/* -- RE-compute project_id based on (possibly updated) project_member_id: */

update start_form sf, project_member pm, unit u
	set sf.pm_pj_id = u.project_id
	where sf.project_id is not null 
	and sf.project_member_id = pm.id
	and pm.unit_id = u.id;

/* */
-- see what's left
select count(*), 'Invalid proj-id values(2)' from start_form where project_id is not null 
	and (project_id <> pm_pj_id or pm_pj_id is null);

/* */
Alter table start_form drop pm_pj_Id;
/* */
