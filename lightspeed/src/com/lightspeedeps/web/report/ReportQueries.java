package com.lightspeedeps.web.report;

import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.util.app.Constants;

public class ReportQueries {

	/**
	 *  People (Contact) - compact query, when user may view hidden fields
	 */
	protected static final String contLstCompQry =
		"SELECT distinct dept.name as department_name, " +
		" user.id, user.first_name, user.last_name, role.name as role, " +
		" unit.name as unitName, unit.number as unitNumber, " +
		" user.email_address, " +
		" user.minor as bMinor, " +
		" case cont.primary_phone_index " +
		"   when 1 then cont.cell_phone " +
		" 	when 2 then cont.home_phone " +
		" 	else cont.business_phone end as phoneno " +
		" FROM production as prod, project as proj, department as dept, contact as cont, " +
		" 	role as role, project_member as prom, employment emp, unit, user " +
		" WHERE prod.id=proj.production_id and user.id=cont.user_id and prom.employment_id=emp.id " +
		"  and emp.role_id=role.id and unit.id=prom.unit_id " +
		" and proj.id=unit.project_id and emp.contact_id=cont.id " +
		" and role.department_id=dept.id and dept.id not in " +
		" ( " + Constants.DEPARTMENT_ID_LS_ADMIN + "," + Constants.DEPARTMENT_ID_DATA_ADMIN + ") ";

	/**
	 *  People (Contact) - compact query, when user may NOT view hidden fields
	 */
	protected static final String contLstCompQryHidden =
		"SELECT distinct dept.name as department_name, " +
		" user.id, user.first_name, user.last_name, role.name as role, " +
		" unit.name as unitName, unit.number as unitNumber, " +
		" case when cont.hidden then ' ' else user.email_address end as email_address, " +
		" user.minor as bMinor, " +
		" case when (cont.hidden and cont.primary_phone_index > 0) then ' ' else " +
		"   case cont.primary_phone_index when 1 then cont.cell_phone " +
		" 	  when 2 then cont.home_phone " +
		" 	  else cont.business_phone end " +
		"   end as phoneno " +
		" FROM production as prod, project as proj, department as dept, contact as cont, " +
		" 	role as role, project_member as prom, employment emp, unit, user " +
		" WHERE prod.id=proj.production_id and user.id=cont.user_id and prom.employment_id=emp.id " +
		"   and emp.role_id=role.id and unit.id=prom.unit_id " +
		"   and proj.id=unit.project_id and emp.contact_id=cont.id " +
		"   and role.department_id=dept.id and dept.id not in " +
		" ( " + Constants.DEPARTMENT_ID_LS_ADMIN + "," + Constants.DEPARTMENT_ID_DATA_ADMIN + ") ";

	/**
	 * People (Contact) - "Detailed" contact list; define SELECT
	 * separate from FROM/WHERE so that we can append to the SELECT clause for the
	 * un-grouped situation.  There are two versions of the SELECT base, one that shows
	 * "hidden" fields, and one that hides them; this one shows them.
	 */
	protected static final String contLstMedQrySelect =
		"SELECT  distinct cont.id , cont.assistant_Id as assistant, dept.name as department_name, " +
		" user.id, user.first_name, user.last_name, role.name as Role, user.email_address, " +
		" unit.name as unitName, unit.number as unitNumber, " +
		" addr.addr_line1, addr.addr_line2, cont.home_phone, cont.business_phone, cont.cell_phone as cellphone, addr.city, " +
		" addr.state, addr.zip, user.im_service, user.im_address, " +
		" user.minor as bMinor ";

	/** The SELECT base for the Detailed Contact query where "hidden" fields are shown as blank
	 * if the Contact as the hidden attribute set to true. */
	protected static final String contLstMedQrySelectHidden =
		"SELECT  distinct cont.id , cont.assistant_Id as assistant, dept.name as department_name, " +
		" user.id, user.first_name, user.last_name, role.name as Role, " +
		" unit.name as unitName, unit.number as unitNumber, " +
		" case when cont.hidden then ' ' else user.email_address end as email_address, " +
		" case when cont.hidden then ' ' else addr.addr_line1 end as addr_line1, " +
		" case when cont.hidden then ' ' else addr.addr_line2 end as addr_line2, " +
		" case when cont.hidden then ' ' else cont.home_phone end as home_phone, " +
		" cont.business_phone, " +
		" case when cont.hidden then ' ' else cont.cell_phone end as cellphone, " +
		" case when cont.hidden then ' ' else addr.city end as city, " +
		" case when cont.hidden then ' ' else addr.state end as state, " +
		" case when cont.hidden then ' ' else addr.zip end as zip, " +
		" case when cont.hidden then ' ' else user.im_service end as im_service, " +
		" case when cont.hidden then ' ' else user.im_address end as im_address, " +
		" user.minor as bMinor ";

	/** The FROM/WHERE clauses for the various Detailed Contact queries. */
	protected static final String contLstMedQryFromWhere =
		" FROM contact as cont left join address as addr on cont.Home_Address_Id = addr.id, " +
		"   project as proj, department as dept, role as role, project_member as prom, employment as emp, unit, user " +
		" WHERE user.id=cont.user_id and prom.employment_id=emp.id " +
		"   and emp.role_id=role.id and unit.id=prom.unit_id " +
		"   and proj.id=unit.project_id and emp.contact_id=cont.id " +
		"   and role.department_id=dept.id and dept.id not in " +
		" ( " + Constants.DEPARTMENT_ID_LS_ADMIN + "," + Constants.DEPARTMENT_ID_DATA_ADMIN + ") ";

	/**
	 * People (Contact) - For "Detailed" contact list when NOT grouped by Dept,
	 * we want to remove duplicates, and only show one role per Contact. To do
	 * that we'll add this clause to the SELECT portion, and the suffix below
	 * after the WHERE clause.
	 * 2/21/12 Rev 2783 - drop this; fixes bug in rev 2523 which was supposed to show all roles.
	 */
//	protected static final  String contLstMedQryUngroupedClause =
//		" , MIN(role.List_Priority) ";

	/**
	 * This gets appended to both the compact & medium queries above, after any
	 * other "WHERE" qualifiers have been added by the code, e.g., for project
	 * or cast/crew selection, when the report is NOT grouped by department.
	 */
	protected static final  String contLstQryUngroupedSuffix =
		" GROUP by cont.id, unitNumber"; // Eliminates duplicate roles per contact in same unit

	/** People (Contact) - Contact Sheet */
//	protected static final  String contLstDetQry =
//		"SELECT distinct user.user_name ,user.login_allowed, cont.assistant_id as assistant, dept.name as department_name, " +
//		"   user.first_name, user.last_name, role.name as role, " +
//		"   unit.name as unitName, unit.number as unitNumber, " +
//		"   user.minor as bMinor, " +
//		"   addr.addr_line1, addr.addr_line2, addr.city, addr.state, addr.zip, user.pseudonym, cont.home_phone, cont.business_phone, " +
//		"   cont.cell_phone as cellphone,user.email_address, cont.imdb_link, user.im_service, user.im_address, cont.sag_member, cont.dga_member, " +
//		"   cont.aftra_member, cont.iatse_member, cont.teamsters_member, cont.notify_by_email, cont.notify_by_text_msg " +
//		" FROM contact as cont left join address as addr on cont.Home_Address_Id = addr.id, " +
//		"   project as proj, department as dept, role as role, project_member as prom, unit, user " +
//		" WHERE user.id=cont.user_id and prom.role_id=role.id " +
//		"   and unit.id=prom.unit_id " +
//		"   and proj.id=unit.project_id and prom.contact_id=cont.id " +
//		"   and role.department_id=dept.id ";

	/** People (Contact) - Contact Sheet - this one does not extract hidden fields */
//	protected static final  String contLstDetQryHidden =
//		"SELECT distinct user.user_name ,user.login_allowed, cont.assistant_id as assistant, " +
//		"   dept.name as department_name, " +
//		"   user.first_name, user.last_name, role.name as role, " +
//		"   unit.name as unitName, unit.number as unitNumber, " +
//		"   user.minor as bMinor, " +
//		"   case when cont.hidden then ' ' else addr.addr_line1 end as addr_line1, " +
//		"   case when cont.hidden then ' ' else addr.addr_line2 end as addr_line2, " +
//		"   case when cont.hidden then ' ' else addr.city end as city, " +
//		"   case when cont.hidden then ' ' else addr.state end as state, " +
//		"   case when cont.hidden then ' ' else addr.zip end as zip, " +
//		"   case when cont.hidden then ' ' else user.pseudonym end as pseudonym, " +
//		"   case when cont.hidden then ' ' else cont.home_phone end as home_phone, " +
//		"   cont.business_phone, " +
//		"   case when cont.hidden then ' ' else cont.cell_phone end as cellphone," +
//		"   case when cont.hidden then ' ' else user.email_address end as email_address, " +
//		"   cont.imdb_link, " +
//		"   case when cont.hidden then ' ' else user.im_service end as im_service, " +
//		"   case when cont.hidden then ' ' else user.im_address end as im_address, " +
//		"   cont.sag_member, cont.dga_member, " +
//		"   cont.aftra_member, cont.iatse_member, cont.teamsters_member, " +
//		"   cont.notify_by_email, cont.notify_by_text_msg " +
//		" FROM contact as cont left join address as addr on cont.Home_Address_Id = addr.id, " +
//		"   project as proj, department as dept, role as role, project_member as prom, unit, user " +
//		" WHERE user.id=cont.user_id and prom.role_id=role.id " +
//		"   and unit.id=prom.unit_id " +
//		"   and proj.id=unit.project_id and prom.contact_id=cont.id " +
//		"   and role.department_id=dept.id ";

	// Various phrases added to the Contact queries WHERE clause...

	/** Only allow roles that are Cast or Stunt occupations. */
	protected static final String contTalentB = " and (dept.id in (" +
			Constants.DEPARTMENT_ID_CAST + ", " + Constants.DEPARTMENT_ID_STUNTS + ")";
	/** Only allow roles that are NOT (strictly) Cast occupations; allows Stunt occupations. */
	protected static final String contNotTalent = " and (dept.id <> " + Constants.DEPARTMENT_ID_CAST + ")";
	/** Only allow roles that include the word "vendor". */
	protected static final String vendorsOnly = " role.name like '%vendor%' ";
	/** Do not allow roles that include the word "vendor". */
	protected static final String excludeVendors = " and role.name not like '%vendor%' ";

//	protected static final  String locIssueListQry=
//		"SELECT R.id, R.name, c.Business_Phone, rlink.Script_Element_Id, R.Management_Id, " +
//		" Ad.Addr_Line1, Ad.City, Ad.State, Ad.Zip " +
//		"FROM real_world_element R " +
//		" left outer join contact c on (R.Management_Id=c.id) " +
//		" left outer join real_link rlink on (R.id=rlink.Real_Element_Id and  rlink.status='SELECTED') " +
//		" left outer join address Ad on (R.Address_Id=Ad.id) " +
//		"WHERE R.Type='LOCATION';";

	protected static final String locReptQry =
		"SELECT img.content as content, rw.Name, rw.id, rw.phone, a.Addr_Line1, a.Addr_Line2, a.City, a.State, " +
		" a.Zip, rw.Phone, rw.Special_Instructions, rw.Parking " +
		"FROM  real_world_element rw " +
		" left outer join address a on (rw.Address_Id = a.id) " +
		" left outer join image img on( rw.map_id = img.id) " +
		"WHERE rw.id = "; // code adds selected RW Element id

	/**
	 * Element List report - for all elements in a Project, regardless of
	 * whether or not they have a link with a RealWorldElement.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListAll =
		"(SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Selected'' as status, " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM script_element se, real_link rl " +
		"WHERE se.id = rl.script_element_id " +
		"  and se.project_id = {0} " +
		"  and se.type in ( {1}  ) " +
		"  and rl.status = ''selected'' )" +
		"union " +
		"(SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Not Selected'' as status, " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM script_element se " +
		"  left join real_link rl on (se.id = rl.script_element_id) " +
		"WHERE se.project_id = {0} " +
		"  and se.type in ( {1} ) " +
		"  and se.id not in " +
		"  (SELECT distinct se.id " +
		"  FROM script_element se, real_link rl " +
		"  WHERE se.id = rl.script_element_id " +
		"    and se.project_id = {0} " +
		"    and rl.status = ''selected'' ) )";


	/**
	 * Element List report - for all elements in a project which have a
	 * link (to a RealWorldElement) with 'Selected' status.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListSelected =
		"SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Selected'' as status, " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM script_element se, real_link rl " +
		"WHERE se.id = rl.script_element_id " +
		"  and se.project_id = {0} " +
		"  and se.type in ( {1}  ) " +
		"  and rl.status = ''selected'' ";

	/**
	 * Element List report - for all elements in a project which do NOT have a
	 * link (to a RealWorldElement) with 'Selected' status.  Such elements might
	 * not have any links, or they might have links, none of which have a status
	 * of 'Selected'.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListUnselected =
		"SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Not Selected'' as status, " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM script_element se " +
		"  left join real_link rl on (se.id = rl.script_element_id) " +
		"WHERE se.project_id = {0} " +
		"  and se.type in ( {1} ) " +
		"  and se.id not in ( " +
		"  SELECT distinct se.id " +
		"  FROM script_element se, real_link rl " +
		"  WHERE se.id = rl.script_element_id " +
		"    and se.project_id = {0} " +
		"    and rl.status = ''selected'' ) ";

	/**
	 * Element List report - for all elements used within a given Unit,
	 * regardless of whether or not they have a link with a RealWorldElement. To
	 * be included, an element must be referenced by a scene that is listed in
	 * a scheduled strip for that Unit.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListAllUnit =
		"SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, " +
		"  unit.name unit_name,  " +
		"  case when rl.status = ''SELECTED'' " +
		"    then ''Selected'' " +
		"    else ''Not Selected'' " +
		"    end as status, " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM project proj, scene sce, scene_script_element sse, strip sp, unit, " +
		"   script_element se left outer join real_link rl on (se.id = rl.script_element_id) " +
		"WHERE proj.current_stripboard_id = sp.Stripboard_Id and  " +
		"  sp.Type = ''BREAKDOWN'' and  " +
		"  sp.unit_id = unit.id and " +
		"  sce.Script_Id = proj.Current_Script_Id and " +
		"  ((sse.scene_id = sce.id and sse.script_element_id = se.id ) or " +
		"  	 (sce.set_id = se.id)) and " +
		"  (concat('','', sp.scene_numbers, '','' ) like concat(''%,'', sce.number, '',%'' )) and  " +
		"  se.type in ( {1} ) and " +
		"  unit.id = {3} and " +
		"  se.project_id = {0} " +
		"GROUP by se.id ";


	/**
	 * Element List report - for all elements used within a given Unit which
	 * have a link (to a RealWorldElement) with 'Selected' status. To be
	 * included, an element must be referenced by a scene that is listed in a
	 * scheduled strip for that Unit.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListSelectedUnit =
		"SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Selected'' as status, " +
		"  unit.name unit_name,  " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM project proj, scene sce, script_element se, scene_script_element sse, real_link rl,  " +
		"	strip sp, unit " +
		"WHERE proj.current_stripboard_id = sp.Stripboard_Id and  " +
		"  sp.Type = ''BREAKDOWN'' and  " +
		"  sp.unit_id = unit.id and " +
		"  sce.Script_Id = proj.Current_Script_Id and " +
		"  ((sse.scene_id = sce.id and sse.script_element_id = se.id ) or " +
		"  	 (sce.set_id = se.id)) and " +
		"  se.id = rl.script_element_id and " +
		"  rl.status = ''selected'' and " +
		"  (concat('','', sp.scene_numbers, '','' ) like concat(''%,'', sce.number, '',%'' )) and  " +
		"  se.type in ( {1} ) and " +
		"  unit.id = {3} and " +
		"  se.project_id = {0} " +
		"GROUP by se.id ";

	/**
	 * Element List report - for all elements used within a given Unit which do
	 * NOT have a link (to a RealWorldElement) with 'Selected' status. Such
	 * elements might not have any links, or they might have links, none of
	 * which have a status of 'Selected'. To be included, an element must be
	 * referenced by a scene that is listed in a scheduled strip for that Unit.
	 * <p>
	 * Note that this query is processed using the Java MessageFormat class, so
	 * it has substitution parameters in the {0} style, and all primes (') must
	 * be doubled, as the formatter removes any single prime.
	 */
	protected static final String elementListUnselectedUnit =
		"SELECT distinct se.id, se.element_id, se.element_id_str castId, se.type, se.name, ''Not Selected'' as status, " +
		"  unit.name unit_name,  " +
		"  {2} " + 					// elementTypes 'case' goes here
		"FROM project proj, scene sce, script_element se, scene_script_element sse, " +
		"  strip sp, unit " +
		"WHERE proj.current_stripboard_id = sp.Stripboard_Id and  " +
		"  sp.Type = ''BREAKDOWN'' and  " +
		"  sp.unit_id = unit.id and " +
		"  sce.Script_Id = proj.Current_Script_Id and " +
		"  ((sse.scene_id = sce.id and sse.script_element_id = se.id ) or " +
		"  	 (sce.set_id = se.id)) and " +
		"  se.id not in ( " + // exclude list of selected ScriptElement id's
		"    SELECT distinct se.id " +
		"	 FROM script_element se, real_link rl " +
		"	 WHERE se.id = rl.script_element_id " +
		"	   and se.project_id = {0} " +
		"	   and rl.status = ''selected'' ) and " +
		"  (concat('','', sp.scene_numbers, '','' ) like concat(''%,'', sce.number, '',%'' )) and  " +
		"  se.type in ( {1} ) and " +
		"  unit.id = {3} and " +
		"  se.project_id = {0} " +
		"GROUP by se.id ";

	/**
	 * This SQL case statement is used to "translate" the ScriptElementType
	 * values that are in the database into the "pretty" labels used in the
	 * reports. This chunk of SQL gets inserted (using MessageFormat-ing) into
	 * all the various element queries (above). We do it this way to make the above
	 * queries a bit more readable, and so we only have to change this list in
	 * one place if types are added or deleted, or if the labels change.
	 */
	protected static final String elementTypes =
		" case se.type " +
		" when 'CHARACTER' then 'Characters' " +
		" when 'EXTRA' then 'Extras' " +
		" when 'PROP' then 'Props' " +
		" when 'MAKEUP_HAIR' then 'Makeup/Hair' " +
		" when 'WARDROBE' then 'Costumes' " +
		" when 'SET_DECORATION' then 'Set Dressing' " +
		" when 'STUNT' then 'Stunts' " +
		" when 'SPECIAL_EFFECT' then 'Special Effects' " +
		" when 'VEHICLE' then 'Vehicles' " +
		" when 'LIVESTOCK' then 'Livestock' " +
		" when 'ANIMAL' then 'Animal Handler' " +
		" when 'GREENERY' then 'Greenery' " +
		" when 'MUSIC' then 'Music' " +
		" when 'SOUND' then 'Sound' " +
		" when 'EQUIPMENT' then 'Special Equipment' " +
		" when 'ADDITIONAL_LABOR' then 'Additional Labor' " +
		" when 'OPTICAL_FX' then 'Optical FX' " +
		" when 'MECHANICAL_FX' then 'Mechanical FX' " +
		" when 'SECURITY' then 'Security' " +
		" when 'MISC' then 'Misc' " +
		" when 'LOCATION' then 'Sets' " +
		" else '?' " +
		" end as element_type ";

	/**
	 * Breakdown report query.
	 */
	protected static final String breakdownSceneQry =
		"SELECT distinct " +
		" sce.hint scene_hint, sce.Script_Day,sce.id scene_id, sp.scene_Numbers scene_number, " +
		" sce.Synopsis, sp.Elapsed_Time, sp.Sheet_Number, sce.IE_Type scne_IET, sce.DN_Type scene_DNT, " +
		" IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length, " +
		" sce.Page_Num_Str scene_page_number, " +
		" unit.number unit_number, unit.name unit_name " +
		"FROM project proj, scene sce, strip sp left join unit on sp.unit_id = unit.id " +
		"WHERE proj.current_stripboard_id=sp.Stripboard_Id and sp.Type='BREAKDOWN' and " +
		" concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) " +
		" and sce.Script_Id=proj.Current_Script_Id";

	/**
	 * Shooting Schedule report query, when sorted in Scene number order.
	 */
	protected static final String shootingScheduleSceneQry =
		"SELECT sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, " +
		" sp.scene_Numbers scene_number, sce.Synopsis, sp.Elapsed_Time, sp.Sheet_Number, " +
		" sce.IE_Type scne_IET, sce.DN_Type scene_DNT, " +
		" IF(sp.length>=8,concat(cast(truncate(sp.length/8,0) as char),if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))),concat(cast(sp.length as char),'/8')) page_length, " +
		" sce.page_num_str scene_page_number, 0 as unit_number, 'All Units' as unit_name " +
		"FROM project proj, scene sce, strip sp " +
		"WHERE proj.current_stripboard_id=sp.Stripboard_Id and sp.Type='BREAKDOWN' and " +
		" concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) " +
		" and sce.Script_Id=proj.Current_Script_Id ";

	/**
	 * Shooting Schedule report query, when sorted in shooting date order.
	 * This includes end-of-day records and unit information.
	 */
	protected static final String shootingScheduleDateQry =
		"SELECT if (type='BREAKDOWN','scene',cast(sp.id as char)) as grouping, " +
		"  sce.hint as scene_hint, sce.Script_Day, sce.id as scene_id, " +
		"  sp.scene_Numbers scene_number, sce.Synopsis, sp.Elapsed_Time, " +
		"  sp.Sheet_Number,  sce.IE_Type scne_IET, sce.DN_Type scene_DNT, " +
		"  IF(sp.length>=8, concat(cast(truncate(sp.length/8,0) as char), if(sp.length%8=0,'',concat(' ',cast(sp.length%8 as char),'/8'))), concat(cast(sp.length as char),'/8')) page_length, " +
		"  sce.page_num_str scene_page_number, sp.type, sp.status, sp.orderNumber, " +
		"  unit.number unit_number, unit.name unit_name " +
		"FROM project proj, scene sce, strip sp left join unit on sp.unit_id = unit.id " +
		"WHERE  proj.current_stripboard_id=sp.Stripboard_Id and " +
		"  (sp.Type='BREAKDOWN' or sp.type='end_of_day') and " +
		"  (concat(sp.scene_numbers, ',' ) like concat(sce.number, ',%' ) or sp.scene_numbers is null) and " +
		"  sce.Script_Id=proj.Current_Script_Id ";

	//Contract Report
//	protected static final String contractQuery =
//		"SELECT cont.display_name, dept.name as deptname, r.name as rolename, " +
//		" contract.name as contractname,conts.status " +
//		"FROM project proj, contact cont, project_member pm, unit, " +
//		" user u, role r, department dept, contract contract " +
//		" left outer join contract_state conts on (conts.contract_id=contract.id) " +
//		"WHERE proj.id = unit.project_id " +
//		" and unit.id = pm.unit_id " +
//		" and pm.contact_id = cont.id " +
//		" and cont.user_id = u.id " +
//		" and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id " +
//		" and cont.id=conts.contact_id and contract.in_use=1" ;

//	protected static final String contractQryfullyCompleted =
//		"SELECT cont.display_name, " +
//		" dept.name as deptname, r.name as rolename, contract.name as contractname, conts.status " +
//		" FROM production p, project proj, contact cont, project_member pm, unit, user u,role r, " +
//		" department dept, contract contract left outer join contract_state conts " +
//		" on (conts.contract_id=contract.id) " +
//		"WHERE p.id = proj.production_id " +
//		" and unit.id = pm.unit_id " +
//		" and proj.id = unit.project_id and pm.contact_id = cont.id " +
//		" and cont.user_id = u.id and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id and cont.id=conts.contact_id and contract.in_use=1 " +
//		" and cont.id not in ( " +
//		"SELECT cont.id " +
//		" FROM production p, project proj, contact cont, project_member pm, unit, user u, role r, " +
//		" department dept, contract contract left outer join contract_state conts " +
//		" on (conts.contract_id=contract.id) " +
//		"WHERE p.id = proj.production_id " +
//		" and unit.id = pm.unit_id " +
//		" and proj.id = unit.project_id and pm.contact_id = cont.id " +
//		" and u.id = cont.user_id and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id and cont.id=conts.contact_id and contract.in_use=1 " +
//		" and conts.status!='SIGNED_AND_RETURNED') and conts.status='SIGNED_AND_RETURNED' " ;


//	protected static final String contractQryItemsPending =
//		"SELECT cont.display_name, dept.name as deptname, " +
//		" r.name as rolename, contract.name as contractname, conts.status " +
//		"FROM production p, project proj, contact cont, project_member pm, unit, user u,role r, " +
//		" department dept, contract contract left outer join contract_state conts " +
//		" on (conts.contract_id=contract.id) " +
//		"WHERE p.id = proj.production_id " +
//		" and unit.id = pm.unit_id " +
//		" and proj.id = unit.project_id and pm.contact_id = cont.id " +
//		" and u.id = cont.user_id and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id and cont.id=conts.contact_id and contract.in_use=1 " +
//		" and cont.id not in ( " +
//		" SELECT cont.id " +
//		"FROM production p, project proj, contact cont, project_member pm, unit, user u,role r, " +
//		" department dept, contract contract left outer join contract_state conts " +
//		" on (conts.contract_id=contract.id) " +
//		"WHERE p.id = proj.production_id " +
//		" and unit.id = pm.unit_id " +
//		" and proj.id = unit.project_id and pm.contact_id = cont.id " +
//		" and u.id = cont.user_id and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id and cont.id=conts.contact_id and contract.in_use=1 " +
//		" and cont.id not in ( " +
//		"SELECT cont.id " +
//		"FROM production p, project proj, contact cont, project_member pm, unit, user u, role r, " +
//		" department dept, contract contract left outer join contract_state conts " +
//		" on (conts.contract_id=contract.id) " +
//		"WHERE p.id = proj.production_id " +
//		" and unit.id = pm.unit_id " +
//		" and proj.id = unit.project_id and pm.contact_id = cont.id " +
//		" and u.id = cont.user_id and cont.department_id = dept.id " +
//		" and cont.Role_id = r.id and cont.id=conts.contact_id and contract.in_use=1 " +
//		" and conts.status!='SIGNED_AND_RETURNED') and conts.status='SIGNED_AND_RETURNED') " ;

	//exhibitG Query
	protected static final String exhibitGQuery =
		"SELECT distinct exhibit_g.*, " +
		" if(exhibit_g.type='FEATURE_FILM',1,0) as MP, " +
		" if(exhibit_g.type='TELEVISION_SERIES',1,0) as TV, " +
		" if(exhibit_g.type='TELEVISION_MOVIE',1,0) as MOW, " +
		" if(exhibit_g.type='INDUSTRIAL',1,0) as INDUSTRIAL, " +
		" if(exhibit_g.type='INDY_FEATURE' OR exhibit_g.type='DOCUMENTARY' OR " +
		" exhibit_g.type='OTHER' ,1,0) as OTHER " +
		" FROM exhibit_g " +
		" WHERE ";

	//exhibitG Subreport Query (i.e Timesheet Query)
	protected static final String exhibitGQuerySubRept =
		"SELECT "
			+ " concat(user.first_name, ' ', user.last_name) as display_name, "
			+ " tc.role as name, "
			+ " tc.day_type as status,"
			+ " tc.* "
			+ "FROM time_card tc, contact, user "
			+ "WHERE "
			+ " contact.id = tc.contact_id and user.id = contact.user_id ";

	// Day Out of Days Query
	protected static final String doodRep = "SELECT * FROM dood_report " ;

	//DPR report
	protected static final String dprQuery =
		"SELECT distinct dpr.*,production.*, address.*, " +
		" case dpr.status " +
		"   when 'APPROVED' then 'APPROVED' " +
		"   when 'SUBMITTED' then 'SUBMITTED' " +
		"   else 'PRELIMINARY' " +
		"   end as pubStatus, " +
		" log.meal catering_1, cast(log.meal_count as char) catering_2, log.vendor catering_3, log.note catering_4 " +
		" FROM dpr, production, project, address, catering_log log " +
		" WHERE dpr.project_id = project.id and " +
		"   project.production_id = production.id and " +
		"   log.dpr_id = dpr.id and " +
		"   production.address_id = address.id ";

	/** DPR report - cast table sub-query */
	protected static final String dprCastQuery =
		"SELECT user.first_name, user.last_name, tc.castid, tc.role as name, " +
		" tc.day_type as status," +
		" tc.* " +
		"FROM time_card tc, contact, user " +
		"WHERE " +
		" contact.id = tc.contact_id " +
		" and user.id = contact.user_id ";
		// " order by tc.castid;" ;

	// DPR (and some other reports) have queries embedded in the jasper control files.
	// These queries may need to be updated if the tables change.  Some of the queries follow:
	/* DPR Film Inventory (in dprfilminvtrysubrpt.jrxml - 4/15/2011):
		select m.type as type,
			case when (fs.date=$P{date}) then fs.inventory_prior
				else (fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) end as inventory_prior,
			case when (fs.date=$P{date}) then fs.inventory_received   else 0 end as inventory_received,
			case when (fs.date=$P{date}) then fs.inventory_used_today else 0 end as inventory_used_today,
			(fs.inventory_prior + fs.inventory_received - fs.inventory_used_today) as inventory_total
		from film_stock fs, material m where
			fs.material_id = m.id and
			(fs.date, fs.material_id) IN
				(Select max(f2.date),f2.material_id from Film_Stock f2, Material
				where f2.date <= $P{date}
				and f2.material_id = material.id
				group by f2.material_id)
		order by type limit 6;
	 */
	/* DPR Film usage - "previous" line - dprfilmused1subrpt.jrxml - 4/15/2011
		select material.type, film_measure.id, film_measure.gross,
			SUM(film_measure.print) as Print,
			SUM(film_measure.no_good) as No_Good,
			SUM(film_measure.waste) as Waste,
			SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
		from material, film_stock, film_measure
		where film_stock.Date < $P{date}
			and film_stock.material_id = material.id
			and film_stock.used_today_id = film_measure.id
		group by material.type
		limit 3;
	*/
	/* DPR Film usage - "today" line - dprfilmused3subrpt.jrxml - 4/15/2011
		select m.type as type,
			case when (fs.date=$P{date}) then fm.print else 0 end as print,
			case when (fs.date=$P{date}) then fm.no_good else 0 end as no_good,
			case when (fs.date=$P{date}) then fm.waste else 0 end as waste,
			case when (fs.date=$P{date}) then (fm.print+fm.no_good+fm.waste) else 0 end as total
		from film_stock fs, material m, film_measure fm where
			fs.material_id = m.id and
			fs.used_today_id = fm.id and
			(fs.date, fs.material_id) IN
				(Select max(f2.date),f2.material_id from Film_Stock f2, Material
				where f2.date <= $P{date}
				and f2.material_id = material.id
				group by f2.material_id)
		group by type
		order by type
		limit 3;
	 */
	/* DPR Film usage - "total" line - dprfilmused3subrpt.jrxml - 4/15/2011
		select material.type, film_measure.id, film_measure.gross,
			SUM(film_measure.print) as Print,
			SUM(film_measure.no_good) as No_Good,
			SUM(film_measure.waste) as Waste,
			SUM(film_measure.print)+SUM(film_measure.no_good)+SUM(film_measure.waste) as Total
		from material, film_stock, film_measure
		where film_stock.Date <= $P{date}
			and film_stock.material_id = material.id
			and film_stock.used_today_id = film_measure.id
		group by material.type
		limit 3;
	 */

	// Call sheet report
	protected static final String callsheetQuery =
		"SELECT cs.*, production.*, address.*, " +
		"   if (cs.status='PUBLISHED','FINAL','PRELIM') as pubStatus, " +
		"   log.meal catering_1, log.meal_count catering_2, log.vendor catering_3, log.note catering_4  " +
		" FROM callsheet cs left join catering_log log on (log.id = catering_log_id)," +
		"   production, project, address " +
		" WHERE cs.project_id = project.id and " +
		"   project.production_id = production.id and " +
		"   production.address_id = address.id ";

	/** Script pages Query */
	public static final String scriptPages = "SELECT * FROM script_report " ;

	/** Stripboard report */
	protected static final String stripboardQuery = "SELECT *, LENGTH(cast_ids) length FROM stripboard_report " ;

	/** "Translate" ACA Employment Basis enum value for reports */
	private static String acaEmployment =
			" case employment_basis " +
			" when 'FT' then 'Full Time' " +
			" when 'PT' then 'Part Time' " +
			" when 'VAR' then 'Variable Hours' " +
			" when 'SNL' then 'Seasonal' " +
			" else 'N/A' " +
			" end as aca_empl ";

	/** "Translate" Retirement_plan enum value for reports */
	private static String retirement =
			" CASE w.retirement_plan " + // "w." for timecard reports
			" when '4' then '401K' " +
			" when 'P' then 'PHBP' " +
			" else '' " +
			" end as retirePlan ";

	private static String retirementSf =
			" CASE retirement_plan " + // no table qualifier for Start form report
			" when '4' then '401K' " +
			" when 'P' then 'PHBP' " +
			" else '' " +
			" end as retirePlan ";

	/** WeeklyTimecard report query */
	public static String timecard =
			"SELECT w.*, " +
			retirement + ", " +
			acaEmployment + ", "+
			" sf.contract_schedule as schedule, " +
			" (not w.allow_worked and employee_rate_type='Hourly' and (union_number is null or union_number='NonU')) as useOnCall " +
			" FROM weekly_time_card w, start_form sf " +
			" WHERE w.updated is not null and " +
			" w.start_form_id = sf.id and ";

	/** WeeklyTimecard report query for Commercial productions */
	public static String timecardAicp =
			"SELECT w.*, sf.*, sr.*, (employee_rate_type='hourly') isHourly, " +
			retirement + ", " +
			acaEmployment +
			" FROM weekly_time_card w, start_form sf, start_rate_set sr " +
			" WHERE w.updated is not null and " +
			" w.start_form_id = sf.id and " +
			" sf.prod_rate_id = sr.id and ";

	/** Weekly model Timecard report query for Commercial productions */
	public static String modelTimecardAicp =
			"SELECT w.*, sf.*, sr.*, mr.*, (employee_rate_type='hourly') isHourly, " +
			retirement + ", " +
			acaEmployment +
			" FROM weekly_time_card w, start_form sf, start_rate_set sr, form_model_release_print mr " +
			" WHERE w.updated is not null and " +
			" w.start_form_id = sf.id and " +
			" sf.Model_Release_Id = mr.id and " +
			" sf.prod_rate_id = sr.id and ";

	/** WeeklyTimecard report query - if only Mileage Forms */
	public static String mileage =
			"select w.*, m.comments m_comments, m.id mileage_id " +
					" FROM weekly_time_card w, start_form sf, mileage m " +
					" WHERE m.weekly_id = w.id and " +
					" w.start_form_id = sf.id and " +
					" w.updated is not null and " ;

	/** WeeklyTimecard report query - if only Box Rental Forms, or Box Rental and Mileage Forms */
	public static String boxRental =
			"select w.*, b.comments box_comments, b.amount box_amount, " +
					" b.inventory_on_file box_on_file, " +
					" b.inventory box_inventory " +
					" FROM weekly_time_card w, start_form sf, box_rental b " +
					" WHERE b.weekly_id = w.id and " +
					" w.start_form_id = sf.id and " +
					" w.updated is not null and " ;

	/**
	 * Timecard-PR Discrepancy report query. The complexity here is due to
	 * having to check the value of the appropriate "show on DPR" setting in the
	 * relevant department, where that department may be the standard one, or
	 * one custom to the production, or custom to the project (in a Commercial
	 * production).
	 */
	public static String discrepancy =
			"SELECT w.*, contact.id contactId, dept1.show_on_dpr onDpr1, sf.project_id projectId, " +
				" case when dept2.show_on_dpr is null then dept1.show_on_dpr else dept2.show_on_dpr end as onDpr2 " +
				" FROM user, contact, start_form sf, weekly_time_card w " +
				" left join department dept1 on w.department_id = dept1.id " +
				" left join department dept2 on w.department_id = dept2.std_dept_id and dept2.production_id = {0} " +
				" WHERE user.account_number = w.user_account and " +
				" w.start_form_id = sf.id and " +
				" ((dept2.id is null) or (sf.project_id is null) or (dept2.project_id = sf.project_id)) and " +
				" contact.user_id = user.id and contact.production_id = {0} " +
				" and w.id in ( {1} ) " +
				" order by {2} ";

	/** Timecards: Weekly Batch List report query, without unbatched timecards */
	public static String weeklyBatchList =
			"SELECT wtc.*, wb.name batchName, " +
				" case when Time_sent is null then 0 else 1 end as sent, " +
				" case when time_edit is null then 0 else 1 end as edit, " +
				" case when time_final is null then 0 else 1 end as final, " +
				" case when time_paid is null then 0 else 1 end as paid " +
				" FROM weekly_time_card wtc, weekly_batch wb " +
				" WHERE (wb.date {0} and wb.date {1} ) " +
				// note that {0} and {1} will contain operators
				" and wb.Production_Id = {2} " + // = Production.id
				" {3} "+
				" and weekly_batch_id is not null " +
				" and wb.id = wtc.weekly_batch_id " +
				" order by end_date desc, batchName, last_name, first_name;" ;

	/** Timecards: All Batched and Unbatched List report query */
	public static String AllBatchesList =
	"SELECT wtc.*, " +
		" case when wb.name is null then '' Unbatched'' else wb.name end as batchName, " +
		" case when wb.name is null then 0 else 1 end as batched, " +
		" case when Time_sent is null then 0 else 1 end as sent, " +
		" case when time_edit is null then 0 else 1 end as edit, " +
		" case when time_final is null then 0 else 1 end as final, " +
		" case when time_paid is null then 0 else 1 end as paid " +
		" FROM weekly_time_card wtc left join weekly_batch wb on wb.id = wtc.weekly_batch_id" +
		" left join start_form sf on sf.id=wtc.start_form_id " +
		" WHERE (( wb.date {0} and wb.date {1} ) " +
			" or (wtc.weekly_batch_id is null and (end_date {0} and end_date {1} ))) " +
		" and wtc.start_form_id is not null " +
		" and wtc.prod_id = {2} " + // = Production.getProdId()
		" {3} " +	// room for sf.project_id test, if any
		" order by batched, wtc.end_date desc, batchName, wtc.last_name, wtc.first_name;" ;

	/** Timecards: UnBatched List report query */
	public static String onlyUnBatchedList =
	"SELECT wtc.*, ''Unbatched'' batchName, " +
		" case when Time_sent is null then 0 else 1 end as sent, " +
		" case when time_edit is null then 0 else 1 end as edit, " +
		" case when time_final is null then 0 else 1 end as final, " +
		" case when time_paid is null then 0 else 1 end as paid " +
		" FROM weekly_time_card wtc left join weekly_batch wb on wb.id = wtc.weekly_batch_id" +
		" left join start_form sf on sf.id=wtc.start_form_id "+
		" WHERE (end_date {0} and end_date {1} ) " +
		" and wtc.start_form_id is not null " +
		" and wtc.prod_id = {2} " + // = Production.getProdId(), not .getId() !!
		" {3} " +	// room for sf.project_id test, if any
		" and weekly_batch_id is null " + // NOTE: 'is null' selects unbatched timecards
		" order by wtc.end_date desc, wtc.last_name, wtc.first_name;" ; // We don't need 'batchname' here

	/** Timecards: One Selected Batch report query */
	public static String onlySelectedBatchList =
	"SELECT wtc.*, wb.name batchName, " +
		" case when Time_sent is null then 0 else 1 end as sent, " +
		" case when time_edit is null then 0 else 1 end as edit, " +
		" case when time_final is null then 0 else 1 end as final, " +
		" case when time_paid is null then 0 else 1 end as paid " +
		" FROM weekly_time_card wtc, weekly_batch wb " +
		" WHERE " +
		" weekly_batch_id = {0} " +  // substitute batch id
		" and wb.id = wtc.weekly_batch_id " +
		" order by end_date desc, last_name, first_name;" ; // We don't need 'batchname' here

	/** StartForm report query */
	protected static String startForm = "SELECT *, d.name as Dept_Name, " +
			retirementSf + ", " +
			acaEmployment +
			" FROM start_form sf, employment e, role r, department d " +
			" WHERE sf.employment_id = e.id and  r.id= e.role_id and d.id = r.department_id  and ";

	/** StartForm report query for tours production. */
	protected static String startFormTours = "SELECT *," +
			retirementSf + ", " +
			acaEmployment + ", " +
			 " ROUND(CAST(((sf.Tours_Show_Rate * CAST(100 AS decimal)) / NULLIF(sf.Tours_Show_Rate, 0)) AS decimal(7,2)), 1) as 'Tours_Show_Percent', " +
			 " ROUND(CAST(((sf.Tours_Prep_Rate * CAST(100 AS decimal)) / NULLIF(sf.Tours_Show_Rate, 0)) AS decimal(7,2)), 1) as 'Tours_Prep_Percent', " +
			 " ROUND(CAST(((sf.Tours_Post_Rate * CAST(100 AS decimal)) / NULLIF(sf.Tours_Show_Rate, 0)) AS decimal(7,2)), 1) as 'Tours_Post_Percent', " +
			 " ROUND(CAST(((sf.Tours_Travel_Rate * CAST(100 AS decimal)) / NULLIF(sf.Tours_Show_Rate, 0)) AS decimal(7,2)), 1) as 'Tours_Travel_Percent', " +
			 " ROUND(CAST(((sf.Tours_Down_Rate * CAST(100 AS decimal)) / NULLIF(sf.Tours_Show_Rate, 0)) AS decimal(7,2)), 1) as 'Tours_Down_Percent' " +
			" FROM start_form sf, employment e " +
			" WHERE sf.employment_id = e.id and ";

	/** Start Form List (rate sheet) report query */
	protected static String startFormList =
			"SELECT sf.*, srs.*, d.name as dept_name, " +
			"   case when Use_Studio_or_Loc = 'S' then true else false end as studio, " +
			"   case when effective_start_date is null then work_start_date else effective_start_date end as start_date " +
			" FROM start_form sf, start_rate_set srs, employment e, role r, department d " +
			" WHERE sf.Prod_Rate_Id = srs.id and sf.employment_id = e.id and e.role_id = r.id and r.department_id = d.id " +
			"   and sf.employment_id in ";

	/** Form I-9 query */
	protected static String formI9Query =
			"SELECT form.*, siA1.label titleA1, siA2.label titleA2, siA3.label titleA3, siB.label titleB, siC.label titleC, siSec3.label titleSec3 " +
					" FROM form_i9 form left join selection_item siA1 on siA1.type ='I9-DOC-A' and siA1.name = form.A1_Doc_Title " +
					" left join selection_item siA2 on siA2.type ='I9-DOC-A' and siA2.name = form.A2_Doc_Title " +
					" left join selection_item siA3 on siA3.type ='I9-DOC-A' and siA3.name = form.A3_Doc_Title " +
					" left join selection_item siB on siB.type ='I9-DOC-B' and siB.name = form.B_Doc_Title " +
					" left join selection_item siC on siC.type ='I9-DOC-C' and siC.name = form.C_Doc_Title " +
					" left join selection_item siSec3 on (siSec3.type = 'I9-DOC-A' or siSec3.type='I9-DOC-C') and siSec3.name = form.Sec3_Doc_Title " +
					" WHERE " +
					" form.id = "; // report generator appends FormI9 id.

	/** Form Indemnification query */
	protected static String formIndemQuery = "SELECT form.* FROM form_indem form WHERE form.id = "; // report generator appends FormIndem id.

	/** Daily Timesheet query */
	public static  String dailyTimeSheet = "SELECT dt.*, w.First_Name, w.Last_Name," +
			" w.Occupation, w.Dept_Name " +
			" from daily_time dt, weekly_time_card w, start_form sf " +
			" where  w.updated is not null " +
			" and w.id = dt.weekly_id " +
			" and sf.id = w.start_form_id and " +
			"  ";

	/** Form MTA query */
	protected static String formMtaQuery = "SELECT form.* FROM form_mta form WHERE form.id = "; // report generator appends FormMta id.

	/** Form Actra Contract query */
	protected static String formActraContractQuery = "SELECT form.* FROM form_actra_contract form WHERE form.id = "; // report generator appends FormActraContract id.

	/** Form Actra Intent query */
	protected static String formActraIntentQuery = "SELECT form.*, " +
			" (case when form.Multi_Branch IS NULL then 'NULL' " +
			" when form.Multi_Branch <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Multi_Branch_Value, " +
			" (case when form.Minor IS NULL then 'NULL' " +
			" when form.Minor <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Minor_Value, " +
			" (case when form.Stunts IS NULL then 'NULL' " +
			" when form.Stunts <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Stunts_Value, " +
			" (case when form.Ext_Scenes IS NULL then 'NULL' " +
			" when form.Ext_Scenes <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Ext_Scenes_Value, " +
			" (case when form.Location_Shoot_40_Radius IS NULL then 'NULL' " +
			" when form.Location_Shoot_40_Radius <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Location_Shoot_40_Radius_Value, " +
			" (case when form.Weather_Permitting IS NULL then 'NULL' " +
			" when form.Weather_Permitting <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Weather_Permitting_Value, " +
			" (case when form.Weekend_Night IS NULL then 'NULL' " +
			" when form.Weekend_Night <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Weekend_Night_Value, " +
			" (case when form.Nude_Scenes IS NULL then 'NULL' " +
			" when form.Nude_Scenes <> 0 then 'YES' " +
			" else 'NO' end) " +
			" as Nude_Scenes_Value " +
			" FROM form_actra_intent form " +
			" WHERE form.id = ";
			//"SELECT form.* FROM form_actra_intent form WHERE form.id = "; // report generator appends FormActraIntent id.

	/** Tours Timesheet sub-query for pay categories */
	protected static String payCategoryCases = " when 'REIMB' then '" +  PayCategory.REIMB.getAbbreviation() + "' " +
			" when 'PER_DIEM_NONTAX' then '" +  PayCategory.PER_DIEM_NONTAX.getAbbreviation() + "' " +
			" when 'PER_DIEM_TAX' then '" +  PayCategory.PER_DIEM_TAX.getAbbreviation() + "' " +
			" when 'SAL_ADVANCE_NONTAX' then '" +  PayCategory.SAL_ADVANCE_NONTAX.getAbbreviation() + "' " +
			" when 'PRE_TAX_401K' then '" +  PayCategory.PRE_TAX_401K.getAbbreviation() + "' " +
			" when 'ROTH' then '" +  PayCategory.ROTH.getAbbreviation() + "' " +
			" when 'PRE_TAX_INS3' then '" +  PayCategory.PRE_TAX_INS3.getAbbreviation() + "' " +
			" when 'PRE_TAX_INS2' then '" +  PayCategory.PRE_TAX_INS2.getAbbreviation() + "' " +
			" when 'BONUS' then '" +  PayCategory.BONUS.getAbbreviation() + "' " +
			" when 'MILEAGE_NONTAX' then '" +  PayCategory.MILEAGE_NONTAX.getAbbreviation() + "' " +
			" when 'CAN_TAX_DED' then '" +  PayCategory.CAN_TAX_DED.getAbbreviation() + "' " +
			" when 'INSURANCE' then '" +  PayCategory.INSURANCE.getAbbreviation() + "' " +
			" when 'AUTO_EXP' then '" +  PayCategory.AUTO_EXP.getAbbreviation() + "' ";

	/** Tours Timesheet query with batch id */
	protected static String toursTimesheetBatchIdQuery = "SELECT t.*, wtc.id as weeklyId, wtc.Weekly_Batch_Id as WeeklyBatchId, wb.name as Weekly_Batch_Name, " +
			" case t.Pay_Category1 " + payCategoryCases + " else '---' end as PayCategory1, " +
			" case t.Pay_Category2 " + payCategoryCases + " else '---' end as PayCategory2, " +
			" case t.Pay_Category3 " + payCategoryCases + " else '---' end as PayCategory3, " +
			" case t.Pay_Category4 " + payCategoryCases + " else '---' end as PayCategory4 " +
			" FROM timesheet t, weekly_time_card wtc, weekly_batch wb where t.End_Date = wtc.End_Date and wtc.Status <> 'VOID' " +
			" and t.Prod_Id = wtc.Prod_Id and wtc.Weekly_Batch_Id = wb.Id and t.Id = "; // report generator appends timesheet id.

	/** Tours Timesheet query for timesheet that has not batch id because it has not been submitted */
	protected static String toursTimesheetNoBatchIdQuery = "select t.*, wtc.id as weeklyId, wtc.Weekly_Batch_Id as WeeklyBatchId, null as Weekly_Batch_Name, " +
			" case t.Pay_Category1 " + payCategoryCases + " else '---' end as PayCategory1, " +
			" case t.Pay_Category2 " + payCategoryCases + " else '---' end as PayCategory2, " +
			" case t.Pay_Category3 " + payCategoryCases + " else '---' end as PayCategory3, " +
			" case t.Pay_Category4 " + payCategoryCases + " else '---' end as PayCategory4 " +
			"FROM timesheet t, weekly_time_card wtc where t.End_Date = wtc.End_Date " +
			"and wtc.Status <> 'VOID' and t.Prod_Id = wtc.Prod_Id and t.Id =";

	/** Tours Tax Wage Allocation query */
	public static String taxWageAllocationQuery = "Select twaf.*, case twaf.Frequency when 'ANNUAL' then 'Annual' when 'QUARTERLY' then 'Quarterly' when 'MONTHLY' then 'Monthly' " +
			"when 'SEMI-MONTHLY' then 'Semi-Monthly' when 'BI-WEEKLY' then  'Bi-Weekly' when 'WEEKLY' then 'Weekly' else 'N/A' end as frequencyFormatted, " +
			"ct.display_name as Employee_Name, ct.user_id as User_Id, u.Social_Security  from Tax_Wage_Allocation_Form twaf, Contact ct, User u  where twaf.id = ?  and ct.id = twaf.contact_id and u.id = ct.user_id"; // report generator appends Form id.

	/** Attachment query to print image attachment. */
	public static String imgAttachmentQuery = "SELECT atc.* FROM attachment atc WHERE atc.id = ";

}
