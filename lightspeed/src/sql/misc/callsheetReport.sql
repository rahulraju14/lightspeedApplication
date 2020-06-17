/* Data for Callsheet report */
/* Top heading area info (header fields on second page
   of report are from this data also. */
select cs.*, production.*,  if (cs.published,'Published','Not Published') as pubStatus
from callsheet cs, production, address
where cs.date = '2008-12-21'
	and cs.project_id = 101
	and address.id = production.address_id;

/* Director's name */
select contact.display_name
from contact, project_member pm, user, role
where user.contact_id = contact.id
		and user.id = pm.user_id
		and pm.role_id = role.id
		and pm.project_id = 101
		and role.name = 'Director';

/* Producer's name (use the first in retrieved set) */
select contact.display_name
from contact, project_member pm, user, role
where user.contact_id = contact.id
		and user.id = pm.user_id
		and pm.role_id = role.id
		and pm.project_id = 101
		and role.name like '%Producer%'
order by role.list_priority;

/* Line Producer's name */
select contact.display_name
from contact, project_member pm, user, role
where user.contact_id = contact.id
		and user.id = pm.user_id
		and pm.role_id = role.id
		and pm.project_id = 101
		and role.name = 'Line Producer';

/* scenes */
select sc.*
from scene_call sc, callsheet cs
where sc.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
order by line_number;

/* Cast */
select cc.*
from cast_call cc, callsheet cs
where cc.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
order by actor_id;

/* standins */
select oc.*
from other_call oc, callsheet cs
where oc.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and oc.type = "STANDIN"
order by line_number;

/* atmosphere */
select oc.*
from other_call oc, callsheet cs
where oc.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and oc.type = "ATMOSPHERE"
order by line_number;

/* special requirements -- split by section number */
select cn.*
from call_note cn, callsheet cs
where cn.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and cn.section >= 2 and cn.section <= 10
order by section, line_number;

/* advance scenes */
select sc.*
from scene_call sc, callsheet cs
where sc.advance_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
order by line_number;

/* bottom notes on front page - usually names & phone numbers */
select cn.*
from call_note cn, callsheet cs
where cn.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and cn.section = 21
order by line_number;

/* Crew data for back page. Jasper report needs to break
	on change in department.name value.  If 'time' is null,
	and 'call_type' is not null, output call_type in time column. */
select dept.name, cc.count, cc.role_name, cc.name, cc.time, cc.call_type 
from dept_call dc, crew_call cc, callsheet cs, department dept
where dc.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and cc.dept_call_id = dc.id
	and dc.department_id = dept.id
order by dept.list_priority, line_number; /**/

/* bottom notes on second page */
select cn.*
from call_note cn, callsheet cs
where cn.callsheet_id = cs.id
	and cs.date = '2008-12-21'
	and cs.project_id = 101
	and cn.section = 22
order by line_number;

/* end callsheet queries */
