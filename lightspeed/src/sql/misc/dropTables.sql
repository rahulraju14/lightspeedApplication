SET foreign_key_checks = off; 
/* *
DROP Table if exists Address;
DROP Table if exists Agreement;
DROP Table if exists Approval_Anchor;
DROP Table if exists Approval_Path;
DROP Table if exists Approval_Path_Anchor;
DROP Table if exists Approval_Path_Contact_Pool;
DROP Table if exists Approval_Path_Document_Chain;
DROP Table if exists Approver;
DROP Table if exists Approver_Audit;
DROP Table if exists Audit_Event;
DROP Table if exists Box_Rental;
DROP Table if exists Call_Back_Rule;
DROP Table if exists Call_Note;
DROP Table if exists Callsheet;
DROP Table if exists Cast_Call;
DROP Table if exists Catering_Log;
DROP Table if exists Changes;
DROP Table if exists Checkout_Notification;
DROP Table if exists Checkout_Order;
DROP Table if exists Child_element;
DROP Table if exists Color_Name;
DROP Table if exists Contact;
DROP Table if exists Contact_Doc_Event;
DROP Table if exists Contact_Document;
DROP Table if exists Contact_Import;
DROP Table if exists Contract;
DROP Table if exists Contract_Rule;
DROP Table if exists Country;
DROP Table if exists Coupon;
DROP Table if exists Coupon_Type;
DROP Table if exists Crew_Call;
DROP Table if exists Daily_Time;
DROP Table if exists Date_Event;
DROP Table if exists Date_Range;
DROP Table if exists Department;
DROP Table if exists Dept_Call;
DROP Table if exists Doc_Change_Event;
DROP Table if exists Document;
DROP Table if exists Document_Chain;
DROP Table if exists Dood_report;
DROP Table if exists DPR;
DROP Table if exists DPR_Days;
DROP Table if exists Dpr_Episode;
DROP Table if exists DPR_Scene;
DROP Table if exists Employment;
DROP Table if exists Event;
DROP Table if exists Exhibit_G;
DROP Table if exists Extra_Time;
DROP Table if exists Film_Measure;
DROP Table if exists Film_Stock;
DROP Table if exists Folder;
DROP Table if exists Form_Field;
DROP Table if exists Form_I9;
DROP Table if exists Form_W4;
DROP Table if exists Form_Wtpa;
DROP Table if exists Golden_Rule;
DROP Table if exists Guarantee_Rule;
DROP Table if exists Holiday_List_Rule;
DROP Table if exists Holiday_Rule;
DROP Table if exists Image;
DROP Table if exists location_interest;
DROP Table if exists Material;
DROP Table if exists Message;
DROP Table if exists Message_Instance;
DROP Table if exists Mileage;
DROP Table if exists Mileage_Line;
DROP Table if exists Mpv_Rule;
DROP Table if exists Note;
DROP Table if exists Notification;
DROP Table if exists Nt_Premium_Rule;
DROP Table if exists Occupation;
DROP Table if exists On_Call_Rule;
DROP Table if exists Other_Call;
DROP Table if exists Overtime_Rule;
DROP Table if exists Packet;
DROP Table if exists Packet_Document;
DROP Table if exists Page;
DROP Table if exists Page_field_access;
DROP Table if exists Pay_Breakdown;
DROP Table if exists Pay_Breakdown_Daily;
DROP Table if exists Pay_Expense;
DROP Table if exists Pay_Job;
DROP Table if exists Pay_Job_Daily;
DROP Table if exists Pay_Rate;
DROP Table if exists Payroll_Preference;
DROP Table if exists Payroll_Service;
DROP Table if exists Point_Of_Interest;
DROP Table if exists Postal_Location;
DROP Table if exists Product;
DROP Table if exists Production;
DROP Table if exists Production_Batch;
DROP Table if exists Production_Contract;
DROP Table if exists Project;
DROP Table if exists Project_Callsheet;
DROP Table if exists Project_Member;
DROP Table if exists Project_Schedule;
DROP Table if exists Real_Link;
DROP Table if exists Real_World_Element;
DROP Table if exists Report_Requirement;
DROP Table if exists Rest_Rule;
DROP Table if exists Role;
DROP Table if exists Role_Group;
DROP Table if exists Rule_Term;
DROP Table if exists Scene;
DROP Table if exists Scene_Call;
DROP Table if exists scene_script_element;
DROP Table if exists Script;
DROP Table if exists Script_Element;
DROP Table if exists Script_Measure;
DROP Table if exists Script_Report;
DROP Table if exists Selection_Item;
DROP Table if exists Signature_Box;
DROP Table if exists Special_Rule;
DROP Table if exists Start_Form;
DROP Table if exists Start_Rate_Set;
DROP Table if exists State;
DROP Table if exists Strip;
DROP Table if exists Strip_color;
DROP Table if exists Stripboard;
DROP Table if exists Stripboard_report;
DROP Table if exists Text_Element;
DROP Table if exists time_card;
DROP Table if exists Time_Card_Event;
DROP Table if exists Timecard_Change_Event;
DROP Table if exists Unions;
DROP Table if exists Unit;
DROP Table if exists Unit_Stripboard;
DROP Table if exists User;
DROP Table if exists Vehicle_Log;
DROP Table if exists Weekly_Batch;
DROP Table if exists Weekly_Batch_Event;
DROP Table if exists Weekly_Rule;
DROP Table if exists Weekly_Time_Card;

/* */