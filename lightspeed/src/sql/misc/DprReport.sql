/* SQL queries to generate data for DPR Report */
/* Sample date & project id are included -- must be replaced at runtime. */

/* Top heading area info; DPR record also includes
	the 4 rows of data for "scenes completed" (below the "Days" table,
	the "UPM", "1st A.D.", and "2nd A.D." names at the bottom
	of the front page, and the Notes section at the bottom of the
	back (second) page. */
select dpr.*, production.*, address.*
from dpr, production, address
where dpr.date = '2008-12-21'
	and dpr.project_id = 101
	and address.id = production.address_id;/**/

/* List of Scenes/title/location/miles *
select dpr_scene.* 
from dpr, dpr_scene
where dpr.id = dpr_scene.Dpr_Id
	and dpr.date = '2008-12-21'; /**/

/* "Days" table - left column *
select dpr_days.*
from dpr, dpr_days
where dpr.days_scheduled_id = dpr_days.Id
	and dpr.date = '2008-12-21'; /**/

/* "Days" table - middle column.  Right column is left minus middle.
	Last row of table is sum of all other rows. *
select dpr_days.*
from dpr, dpr_days
where dpr.days_to_date_id = dpr_days.Id
	and dpr.date = '2008-12-21'; /**/

/* script/Scenes shot table - 9 rows, 9 queries *
select script_measure.* from dpr, script_measure
where dpr.Script_Prior_Total_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Added_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Deleted_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Total_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Shot_Prior_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Planned_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Shot_Id = script_measure.Id
	and dpr.date = '2008-12-21'; /* ("Actual today") *

select script_measure.* from dpr, script_measure
where dpr.Script_Shot_To_Date_Id = script_measure.Id
	and dpr.date = '2008-12-21';

select script_measure.* from dpr, script_measure
where dpr.Script_Remaining_Id = script_measure.Id
	and dpr.date = '2008-12-21';

/* Film used table: query returns 1 record per type -- print
   up to 3 types.  The first query returns data for first row ("Prev") *
select material.type, film_measure.* 
	from material, film_stock, film_measure
where film_stock.date = '2008-12-21'
	and film_stock.project_id = 101
	and film_stock.material_id = material.id
	and film_stock.used_prior_id = film_measure.id
order by material.type
limit 3;

/* This query returns data for next row ("Today") *
select film_measure.* from material, film_stock, film_measure
where film_stock.date = '2008-12-21'
	and film_stock.project_id = 101
	and film_stock.material_id = material.id
	and film_stock.used_today_id = film_measure.id
order by material.type
limit 3;

/* This query returns data for last row ("To Date") *
select film_measure.* from material, film_stock, film_measure
where film_stock.date = '2008-12-21'
	and film_stock.project_id = 101
	and film_stock.material_id = material.id
	and film_stock.used_total_id = film_measure.id
order by material.type
limit 3;

/* Film Inventory table: query returns one record for each column, 
   print a max of 3 columns *
select material.type, film_stock.inventory_prior, film_stock.inventory_received,
	film_stock.inventory_used_today, film_stock.inventory_total
from material, film_stock
where film_stock.date = '2008-12-21'
	and film_stock.project_id = 101
	and film_stock.material_id = material.id
order by material.type
limit 3;

/* Sound Rolls *
select sound_stock.* from dpr, sound_stock
where dpr.sound_stock_Id = sound_stock.Id
	and dpr.date = '2008-12-21';

/* Timesheet info for "weekly & Day players" *
select contact.display_name, tc.castid, script_element.name,
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
	where ts.date =  '2008-12-21'
	and ts.project_id = 101
	and tc.time_sheet_id = ts.id
	and tc.dtype = 'CT'
	and contact.id = tc.contact_id
	and contact.id = real_world_element.contact_id
	and real_world_element.id = real_link.real_element_id
	and real_link.script_element_id = script_element.id
	and real_link.status = 'SELECTED'
	and script_element.type = 'CHARACTER'; /**/
  
/* "Extras, Standins" section *
Select extra_time.* 
	from dpr, extra_time 
where Extra_time.dpr_id = dpr.id
	and dpr.date = '2008-12-21'; /**/

/* Crew data for back page. Jasper report needs to break
	on change in department.name value.  If report_set ("IN") is
	null, print "O/C"; same for dismiss_set ("OUT"). *
select contact.display_name, tc.report_set, tc.dismiss_set, department.name 
	from time_sheet ts, time_card tc, contact, department, role
where ts.date = '2008-12-21'
	and ts.project_id = 101
	and tc.time_sheet_id = ts.id
	and tc.dtype != 'CT'
	and tc.contact_id = contact.id
	and contact.role_id = role.id
	and tc.department_id = department.id
	order by department.list_priority, role.list_priority; /**/

/* Meal info (bottom right of back page *
select catering_log.*
	from catering_log, dpr
where catering_log.dpr_id = dpr.id
	and dpr.date = '2008-12-20'; /**/

