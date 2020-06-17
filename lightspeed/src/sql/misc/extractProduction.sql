SET foreign_key_checks = off;

/*
 Last updated 1/16/2015, rev 2.9.5069

This SQL copies all the records related to a single production from a 'source' database into
a new (EMPTY), 'target', database.
The database id of the production to be copied must be set in the first statement of the 
following SQL.
If you need to run the Lightspeed app against the target database, it will also need to 
have added the System production, the standard tables data, permissions, roles, and HTG data. 
*/

use dbt30; -- Set this to the Source database name.

set @prod = 38; -- set the database id of production to be copied from (source)

set @prodid = ( select prod_id from production where id = @prod );

-- ** If the target database is not 'lsdbtest', change all references accordingly. **

-- copy the Production record
insert into lsdbtest.production         select * from production where id = @prod;

-- copy all records that are keyed to the production
insert into lsdbtest.Payroll_Preference select * from Payroll_Preference where id in (select Payroll_Preference_Id from lsdbtest.production);
insert into lsdbtest.Project            select * from Project            where production_id = @prod;
insert into lsdbtest.Contact            select * from Contact            where production_id = @prod;
insert into lsdbtest.Material           select * from Material           where production_id = @prod;
insert into lsdbtest.Point_Of_Interest  select * from Point_Of_Interest  where production_id = @prod;
insert into lsdbtest.Real_World_Element select * from Real_World_Element where production_id = @prod;
insert into lsdbtest.Department         select * from Department         where production_id = @prod;
insert into lsdbtest.Role               select * from Role               where production_id = @prod;
insert into lsdbtest.Approval_Anchor    select * from Approval_Anchor    where production_id = @prod;
insert into lsdbtest.production_batch   select * from production_batch   where production_id = @prod;
insert into lsdbtest.weekly_batch       select * from weekly_batch       where production_id = @prod;
insert into lsdbtest.Weekly_Time_Card   select * from Weekly_Time_Card   where prod_id = @prodid;

-- Copy some non-Production-specific data which may be referenced by a Production
-- insert into lsdbtest.Payroll_service       select * from Payroll_service;
insert into lsdbtest.Payroll_service    select * from Payroll_service    where id in (select payroll_service_id from lsdbtest.payroll_preference);

-- copy all records that are keyed to Weekly_Time_Card
insert into lsdbtest.Daily_Time         select * from Daily_Time         where Weekly_Id in (select id from lsdbtest.Weekly_Time_Card);
insert into lsdbtest.Pay_Job            select * from Pay_Job            where Weekly_Id in (select id from lsdbtest.Weekly_Time_Card);
insert into lsdbtest.Pay_Breakdown      select * from Pay_Breakdown      where Weekly_Id in (select id from lsdbtest.Weekly_Time_Card);
insert into lsdbtest.Pay_Expense        select * from Pay_Expense        where Weekly_Id in (select id from lsdbtest.Weekly_Time_Card);
insert into lsdbtest.Time_Card_Event    select * from Time_Card_Event    where Weekly_Id in (select id from lsdbtest.Weekly_Time_Card);

-- copy all records that are keyed to Pay_Job
insert into lsdbtest.Pay_Job_Daily      select * from Pay_Job_Daily      where job_Id in (select id from lsdbtest.Pay_Job);

-- copy all records that are keyed to Contact
insert ignore into lsdbtest.User      select * from User           where id in (select user_id from lsdbtest.contact);
insert into lsdbtest.Project_Member   select * from Project_Member where contact_id in (select id from lsdbtest.contact);
insert into lsdbtest.Start_Form       select * from Start_Form     where contact_id in (select id from lsdbtest.contact);
insert into lsdbtest.Approver         select * from Approver       where contact_id in (select id from lsdbtest.contact);

-- Copy additional Start_form related records
insert ignore into lsdbtest.start_rate_set    select * from start_rate_set  where id in (select prod_rate_id from lsdbtest.start_form) 
                                                                or id in (select prep_rate_id from lsdbtest.start_form); 

-- copy all records that are keyed to Project
insert into lsdbtest.unit             select * from unit           where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Script           select * from Script         where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Script_Element   select * from Script_Element where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Callsheet        select * from Callsheet      where project_id in (select id from lsdbtest.project);
insert into lsdbtest.DPR              select * from DPR            where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Exhibit_G        select * from Exhibit_G      where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Notification     select * from Notification   where project_id in (select id from lsdbtest.project);
insert into lsdbtest.Report_Requirement select * from Report_Requirement where project_id in (select id from lsdbtest.project);

-- copy optional "historical" records that are keyed to Project or the Production
insert into lsdbtest.Changes select * from Changes where project_id in (select id from lsdbtest.project)
																										or   production_id = @prod;
insert into lsdbtest.Event select * from Event     where project_id in (select id from lsdbtest.project)
																										or   production_id = @prod;

-- copy all records that are keyed to Notification
insert into lsdbtest.Message          select * from Message         where notification_id in (select id from lsdbtest.notification);

-- copy all records that are keyed to Message
insert into lsdbtest.Message_Instance select * from Message_Instance where message_id in (select id from lsdbtest.message);

-- copy all records that are keyed to Unit
insert into lsdbtest.unit_stripboard  select * from unit_stripboard  where unit_id in (select id from lsdbtest.unit);
insert into lsdbtest.Project_Schedule select * from Project_Schedule where id in (select schedule_id from lsdbtest.unit);

-- copy all records that are keyed to Project_schedule
insert into lsdbtest.Date_Event       select * from Date_Event      where schedule_id in (select id from lsdbtest.project_schedule); 

-- copy all records that are keyed to Real_World_Element
insert into lsdbtest.Date_Range        select * from Date_Range  where real_world_element_id in (select id from lsdbtest.real_world_element);
insert into lsdbtest.location_interest select * from location_interest where location_id in (select id from lsdbtest.real_world_element);
insert into lsdbtest.Real_Link         select * from Real_Link     where real_element_id in (select id from lsdbtest.real_world_element);

-- copy all records that are keyed to Script
insert into lsdbtest.Scene            select * from Scene           where script_id in (select id from lsdbtest.script);
insert into lsdbtest.Page             select * from Page            where script_id in (select id from lsdbtest.script);

-- copy all records that are keyed to Scene or Page
insert into lsdbtest.scene_script_element select * from scene_script_element where scene_id in (select id from lsdbtest.scene);
insert into lsdbtest.Text_Element         select distinct * from Text_Element where scene_id in (select id from lsdbtest.scene)
                                                                                 or page_id  in (select id from lsdbtest.page);
-- copy all records that are keyed to Script_element
insert into lsdbtest.Child_element    select * from Child_element   where parent_id in (select id from lsdbtest.script_element);

-- copy all records that are keyed to Unit_stripboard
insert into lsdbtest.Stripboard       select * from Stripboard      where id in (select stripboard_id from lsdbtest.unit_stripboard);

-- copy all records that are keyed to Stripboard
insert into lsdbtest.Strip            select * from Strip           where stripboard_id in (select id from lsdbtest.stripboard);

-- copy all records that are keyed to Strip
insert into lsdbtest.Note             select * from Note            where strip_id in (select id from lsdbtest.strip);

-- copy all records that are keyed to Callsheet
insert into lsdbtest.Call_Note        select * from Call_Note       where callsheet_id in (select id from lsdbtest.callsheet);
insert into lsdbtest.Other_Call       select * from Other_Call      where callsheet_id in (select id from lsdbtest.callsheet);
insert into lsdbtest.Scene_Call       select * from Scene_Call      where callsheet_id in (select id from lsdbtest.callsheet)
                                                                       or advance_id in (select id from lsdbtest.callsheet);
insert into lsdbtest.Cast_Call        select * from Cast_Call       where callsheet_id in (select id from lsdbtest.callsheet);
insert into lsdbtest.Dept_Call        select * from Dept_Call       where callsheet_id in (select id from lsdbtest.callsheet);
insert into lsdbtest.Crew_Call        select * from Crew_Call       where dept_call_id in (select id from lsdbtest.dept_call);
insert into lsdbtest.Catering_Log     select * from Catering_Log    where id in (select catering_log_id from lsdbtest.callsheet);
insert into lsdbtest.Catering_Log     select * from Catering_Log    where dpr_id in (select id from lsdbtest.dpr);

-- copy all records that are keyed to DPR
insert into lsdbtest.DPR_Days         select * from DPR_Days        where id in (select days_scheduled_id from lsdbtest.dpr)
                                                                       or id in (select days_to_date_id from lsdbtest.dpr);
insert into lsdbtest.dpr_episode      select * from dpr_episode     where dpr_id in (select id from lsdbtest.dpr);
insert into lsdbtest.DPR_Scene        select * from DPR_Scene       where dpr_id in (select id from lsdbtest.dpr);
insert into lsdbtest.Extra_Time       select * from Extra_Time      where dpr_id in (select id from lsdbtest.dpr);

-- copy all Script_Measure records that are keyed to DPR_episode(s)
insert into lsdbtest.Script_Measure   select * from Script_Measure  where id in (select script_prior_total_id from 
																															dpr_episode where dpr_id in (select id from lsdbtest.dpr));
insert into lsdbtest.Script_Measure   select * from Script_Measure  where id in (select script_total_id from 
																															dpr_episode where dpr_id in (select id from lsdbtest.dpr));
insert into lsdbtest.Script_Measure   select * from Script_Measure  where id in (select script_shot_prior_id from 
																															dpr_episode where dpr_id in (select id from lsdbtest.dpr));
insert into lsdbtest.Script_Measure   select * from Script_Measure  where id in (select script_shot_id from 
																															dpr_episode where dpr_id in (select id from lsdbtest.dpr));

-- copy all Time_card records - keyed to reports 
insert into lsdbtest.time_card        select * from time_card  where dpr_cast_id in (select id from lsdbtest.dpr)
                                                               or dpr_crew_id in (select id from lsdbtest.dpr)
                                                               or exhibitG_id in (select id from lsdbtest.exhibit_g);

-- copy all records that are keyed to Material
insert into lsdbtest.Film_Stock       select * from Film_Stock where material_id in (select id from lsdbtest.material);

-- copy all records that are keyed to Film_Stock
insert into lsdbtest.Film_Measure     select * from Film_Measure where id in (select used_prior_id from lsdbtest.film_stock)
                                                                    or id in (select used_today_id from lsdbtest.film_stock)
                                                                    or id in (select used_total_id from lsdbtest.film_stock);

-- copy all Address records - keyed to several other records
insert ignore into lsdbtest.Address          select * from Address  where id in (select address_id from lsdbtest.production)
                                                                or id in (select address_id from lsdbtest.point_of_interest)
                                                                or id in (select address_id from lsdbtest.real_world_element)
                                                                or id in (select home_address_id from lsdbtest.user)
                                                                or id in (select loan_out_address_id from lsdbtest.start_form) 
                                                                or id in (select agency_address_id from lsdbtest.start_form) 
                                                                or id in (select home_address_id from lsdbtest.contact)
                                                                or id in (select address_id from payroll_service)
                                                                or id in (select Mailing_address_id from payroll_service);

-- copy all Image records - keyed to several other records
insert into lsdbtest.Image select * from Image where contact_id in (select id from lsdbtest.contact)
                                                     or user_id in (select id from lsdbtest.user)
                                                     or script_element_id in (select id from lsdbtest.script_element)
                                                     or point_of_interest_id in (select id from lsdbtest.point_of_interest)
                                                     or real_world_element_id in (select id from lsdbtest.real_world_element)
                                                     or id in (select map_id from lsdbtest.real_world_element)
                                                     or id in (select logo_id from lsdbtest.production)
                                                     or id in (select Paper_Image_Id from lsdbtest.weekly_time_card)
                                                     or id in (select desktop_logo_id from payroll_service)
                                                     or id in (select mobile_logo_id from payroll_service)
                                                     or id in (select report_logo_id from payroll_service);

-- copy all Folder records that are keyed to production or parent folders (a tree).
insert into lsdbtest.Folder select * from Folder where id in (select repository_id from lsdbtest.production);
-- repeat the next insert as many times as the deepest nesting of folders we expect to find.
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);
insert ignore into lsdbtest.Folder select * from Folder where parent_id in (select id from lsdbtest.folder);

-- copy all records that are keyed to Folder (i.e., Documents)
insert into lsdbtest.Document select * from Document where folder_id in (select id from lsdbtest.folder);

/* */
	