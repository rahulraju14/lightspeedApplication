
/* header area data *
select exhibit_g.*,
	if(production.type='FEATURE_FILM',1,0) as MP,
	if(production.type='TELEVISION_SERIES',1,0) as TV,
	if(production.type='TELEVISION_MOVIE',1,0) as MOW,
	if(production.type='INDUSTRIAL',1,0) as INDUSTRIAL,
	if(production.type='INDY_FEATURE' OR production.type='DOCUMENTARY' OR production.type='OTHER' ,1,0) as OTHER
from exhibit_g, production
	where
	exhibit_g.date =  '2008-12-22'
	and exhibit_g.project_id = 101;
/**/
/* detail lines *
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
	and script_element.type = 'CHARACTER'  ; 
  /**/
