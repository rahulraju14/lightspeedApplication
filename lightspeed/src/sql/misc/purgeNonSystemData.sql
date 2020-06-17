SET foreign_key_checks = off;

-- Purges all non-standard tables and data; preserves the SYSTEM production!

-- Preserves all standard data including departments, roles,
-- permissions, HTG rules, etc.

-- extra addresses for 5 payroll services
delete from Address where id > 11;
ALTER TABLE Address AUTO_INCREMENT = 12;

truncate Approver;
truncate Approval_Anchor;
truncate Audit_Event;
truncate Box_Rental;
truncate Call_Note;
truncate Callsheet;
truncate Cast_Call;
truncate Catering_Log;
truncate Changes;

delete from contact where id > 5;
ALTER TABLE contact AUTO_INCREMENT = 6;

truncate Crew_Call;
truncate Daily_Time;

delete from Department where id > 999;
ALTER TABLE Department AUTO_INCREMENT = 1000;

truncate Dept_Call;
truncate Document;
truncate Dood_report;
truncate DPR;
truncate DPR_Days;
truncate Dpr_Episode;
truncate DPR_Scene;
truncate Event;
truncate Exhibit_G;
truncate Extra_Time;
truncate Film_Measure;
truncate Film_Stock;

delete from Folder where id > 1;
ALTER TABLE Folder AUTO_INCREMENT = 2;

-- extra images for 5 payroll_services
delete from Image where id > 13;
ALTER TABLE Image AUTO_INCREMENT = 14;

truncate location_interest;
truncate Material;
truncate Message;
truncate Message_Instance;
truncate Mileage;
truncate Mileage_Line;
truncate Note;
truncate Notification;
truncate Other_Call;
truncate Page;
truncate Pay_Breakdown;
truncate Pay_Expense;
truncate Pay_Job;
truncate Pay_Job_Daily;

delete from Payroll_Preference where id > 1;
ALTER TABLE Payroll_Preference AUTO_INCREMENT = 2;

truncate Point_Of_Interest;

delete from Production where id > 1;
ALTER TABLE Production AUTO_INCREMENT = 2;

truncate Production_Batch;
truncate Production_Contract;

delete from Project where id > 1;
ALTER TABLE Project AUTO_INCREMENT = 2;

truncate Project_Callsheet;

delete from Project_Member where id > 5;
ALTER TABLE Project_Member AUTO_INCREMENT = 6;

delete from Project_Schedule where id > 1;
ALTER TABLE Project_Schedule AUTO_INCREMENT = 2;

truncate Real_Link;
truncate Real_World_Element;

delete from Role where id > 9999;
ALTER TABLE Role AUTO_INCREMENT = 10000;

truncate Scene;
truncate Scene_Call;
truncate scene_script_element;
truncate Script;
truncate Script_Element;
truncate Script_Measure;
truncate Script_Report;
truncate Start_Form;
truncate Start_Form_Event;
truncate Start_Rate_Set;
truncate Strip;
truncate Stripboard;
truncate Stripboard_report;
truncate Text_Element;
truncate time_card;
truncate Time_Card_Event;

delete from Unit where id > 1;
ALTER TABLE Unit AUTO_INCREMENT = 2;

truncate Unit_Stripboard;

delete from User where id > 5;
ALTER TABLE User AUTO_INCREMENT = 6;

truncate Vehicle_Log;
truncate Weekly_Batch;
truncate Weekly_Batch_Event;
truncate Weekly_Time_Card;

/**/
