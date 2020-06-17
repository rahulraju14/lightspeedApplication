/* Database changes in v4.x versions */
SET foreign_key_checks = off;

-- --------------------- Start of release 4.0.0 changes ------------------------------------------
/*

-- Add new table "form_actra_contract" for the new form ACTRA Contract form, for LS-1400
drop table  if exists form_actra_contract;
CREATE TABLE `form_actra_contract` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '1',
  `Contract_Num` varchar(25) DEFAULT NULL,
  `Branch_Code` varchar(15) DEFAULT NULL,
  `Agency_Name` varchar(150) DEFAULT NULL,
  `Agency_Producer` varchar(150) DEFAULT NULL,
  `Agency_Address_Id` int(11) DEFAULT NULL,
  `Advertiser_Name` varchar(150) DEFAULT NULL,
  `Product_Name` varchar(150) DEFAULT NULL,
  `Prod_House_Name` varchar(150) DEFAULT NULL,
  `Prod_House_Address_Id` int(11) DEFAULT NULL,
  `Pay_Session_Fee` tinyint(1) DEFAULT '0',
  `Director_Name` varchar(150) DEFAULT NULL,
  `Loan_Out_Name` varchar(100) DEFAULT NULL,
  `Talent_Name` varchar(100) DEFAULT NULL,
  `Talent_Address_Id` int(11) DEFAULT NULL,
  `Talent_Email_Address` varchar(100) DEFAULT NULL,
  `Talent_Phone_Num` varchar(15) DEFAULT NULL,
  `Social_Insurance_Num` varchar(1000) DEFAULT NULL,
  `Gst_Hst` varchar(20) DEFAULT 'N/A',
  `Qst` varchar(20) DEFAULT 'N/A',
  `DOB` date DEFAULT NULL,
  `Talent_Agency_Name` varchar(150) DEFAULT NULL,
  `Agency_Contact` varchar(100) DEFAULT NULL,
  `Performance_Category` varchar(30) DEFAULT NULL,
  `Full_Member_Num` varchar(15) DEFAULT NULL,
  `Apprentice_Num` varchar(45) DEFAULT NULL,
  `Work_Permit_Num` varchar(25) DEFAULT NULL,
  `National_Tv` varchar(15) DEFAULT NULL,
  `National_Radio` varchar(15) DEFAULT NULL,
  `National_Digital_Media_Video` varchar(15) DEFAULT NULL,
  `National_Digital_Media_Audio` varchar(15) DEFAULT NULL,
  `Tags_Tv` int(11) DEFAULT NULL,
  `Tags_Radio` int(11) DEFAULT NULL,
  `Tags_Digital_Media` int(11) DEFAULT NULL,
  `Regional_Changes_Tv` int(11) DEFAULT NULL,
  `Regional_Changes_Radio` int(11) DEFAULT NULL,
  `Regional_Changes_Digital_Media` int(11) DEFAULT NULL,
  `Psa_Tv` varchar(15) DEFAULT NULL,
  `Psa_Radio` varchar(15) DEFAULT NULL,
  `Psa_Digital_Media` varchar(15) DEFAULT NULL,
  `Demo_Tv` varchar(15) DEFAULT NULL,
  `Demo_Radio` varchar(15) DEFAULT NULL,
  `Demo_Digital` varchar(15) DEFAULT NULL,
  `Demo_Presentation` varchar(15) DEFAULT NULL,
  `Demo_Infomercial` varchar(15) DEFAULT NULL,
  `Seasonal_Tv` varchar(15) DEFAULT NULL,
  `Seasonal_Radio` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer_Tv` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer_Radio` varchar(15) DEFAULT NULL,
  `Seasonal_Double_Shoot` varchar(15) DEFAULT NULL,
  `Seasonal_Joint_Promo` varchar(15) DEFAULT NULL,
  `Local_Regional_Category_Num` varchar(15) DEFAULT NULL,
  `Local_Regional_Tv` varchar(15) DEFAULT NULL,
  `Local_Regional_Radio` varchar(15) DEFAULT NULL,
  `Local_Regional_Digital_Media` varchar(15) DEFAULT NULL,
  `Local_Regional_Demo` varchar(15) DEFAULT NULL,
  `Local_Regional_Digital_Media_Broadcast` varchar(15) DEFAULT NULL,
  `Local_Regional_Broadcast_Digital_Media` varchar(15) DEFAULT NULL,
  `Local_Regional_Other` varchar(15) DEFAULT NULL,
  `Local_Regional_Pilot_Project` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_7_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_14_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_31_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_45_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_7_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_14_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_31_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_45_Days` varchar(15) DEFAULT NULL,
  `Tv_Broadcast_Digital_Media` varchar(15) DEFAULT NULL,
  `Digital_Media_Broadcast_Tv` varchar(15) DEFAULT NULL,
  `Digital_Media_Other` varchar(15) DEFAULT NULL,
  `Radio_Digital_Media` varchar(15) DEFAULT NULL,
  `Actra_Online` varchar(15) DEFAULT NULL,
  `Pilot_Project` varchar(15) DEFAULT NULL,
  `Article_2403` varchar(15) DEFAULT NULL,
  `Article_2404` varchar(15) DEFAULT NULL,
  `Article_2405` varchar(15) DEFAULT NULL,
  `Article_2406` varchar(15) DEFAULT NULL,
  `Commercial_Name` varchar(250) DEFAULT NULL,
  `Docket` varchar(50) DEFAULT NULL,
  `Additional_Titles` varchar(250) DEFAULT NULL,
  `Session_Fees` varchar(100) DEFAULT NULL,
  `Residual_Fees` varchar(100) DEFAULT NULL,
  `Other_Fees` varchar(100) DEFAULT NULL,
  `Digital_Media_Fees` varchar(100) DEFAULT NULL,
  `Special_Provisions` varchar(1000) DEFAULT NULL,
  `NDM` tinyint(1) DEFAULT '0',
  `Weekly_Timecard_Id` int(11) NOT NULL,
  `Created_By_id` int(11) NOT NULL,
  `Created_Date` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `Updated_By_Id` int(11) NOT NULL,
  `Updated_Date` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  KEY `Agency_Address_Id_idx` (`Agency_Address_Id`),
  KEY `Prod_House_Address_Id_idx` (`Prod_House_Address_Id`),
  KEY `Talent_Address_Id_idx` (`Talent_Address_Id`),
  KEY `Updated_By_Id_idx` (`Updated_By_Id`),
  KEY `Created_By_Id_idx` (`Created_By_id`),
  CONSTRAINT `Agency_Address_Id` FOREIGN KEY (`Agency_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `Created_By_Id` FOREIGN KEY (`Created_By_id`) REFERENCES `user` (`Id`),
  CONSTRAINT `Prod_House_Address_Id` FOREIGN KEY (`Prod_House_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `Talent_Address_Id` FOREIGN KEY (`Talent_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `Updated_By_Id` FOREIGN KEY (`Updated_By_Id`) REFERENCES `user` (`Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

drop table  if exists talent_category;
CREATE TABLE `talent_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Category_Code` varchar(25) DEFAULT NULL,
  `Ls_Occ_Code` varchar(10) DEFAULT NULL,
  `Gs_Occ_Code` varchar(12) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

drop table  if exists commercial;
CREATE TABLE `commercial` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Name` varchar(100) DEFAULT NULL,
  `Shoot_Date` date DEFAULT NULL,
  `Location` varchar(150) DEFAULT NULL,
  `Length` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Table for canada specific project details to used as defaults on forms.
drop table  if exists canada_project_detail;
CREATE TABLE `canada_project_detail` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Agency_Name` varchar(150) DEFAULT NULL,
  `Agency_Producer` varchar(100) DEFAULT NULL,
  `Agency_Address_Id` int(11) DEFAULT NULL,
  `Advertiser_Name` varchar(100) DEFAULT NULL,
  `Product_Name` varchar(150) DEFAULT NULL,
  `Prod_House_Name` varchar(150) DEFAULT NULL,
  `Prod_House_Address_Id` int(11) DEFAULT NULL,
  `Director_Name` varchar(100) DEFAULT NULL,
  `Commercial_Name` varchar(250) DEFAULT NULL,
  `Additional_Titles` varchar(250) DEFAULT NULL,
  `Brand_Name` varchar(150) DEFAULT NULL,
  `Branch_Code` varchar(20) DEFAULT NULL,
  `Docket` varchar(50) DEFAULT NULL,
  `National_Tv` varchar(15) DEFAULT NULL,
  `National_Radio` varchar(15) DEFAULT NULL,
  `National_Digital_Media_Video` varchar(15) DEFAULT NULL,
  `National_Digital_Media_Audio` varchar(15) DEFAULT NULL,
  `Tags_Tv` int(11) DEFAULT NULL,
  `Tags_Radio` int(11) DEFAULT NULL,
  `Tags_Digital_Media` int(11) DEFAULT NULL,
  `Regional_Changes_Tv` int(11) DEFAULT NULL,
  `Regional_Changes_Radio` int(11) DEFAULT NULL,
  `Regional_Changes_Digital_Media` int(11) DEFAULT NULL,
  `Psa_Tv` varchar(15) DEFAULT NULL,
  `Psa_Radio` varchar(15) DEFAULT NULL,
  `Psa_Digital_Media` varchar(15) DEFAULT NULL,
  `Demo_Tv` varchar(15) DEFAULT NULL,
  `Demo_Radio` varchar(15) DEFAULT NULL,
  `Demo_Digital` varchar(15) DEFAULT NULL,
  `Demo_Presentation` varchar(15) DEFAULT NULL,
  `Demo_Infomercial` varchar(15) DEFAULT NULL,
  `Seasonal_Tv` varchar(15) DEFAULT NULL,
  `Seasonal_Radio` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer_Tv` varchar(15) DEFAULT NULL,
  `Seasonal_Dealer_Radio` varchar(15) DEFAULT NULL,
  `Seasonal_Double_Shoot` varchar(15) DEFAULT NULL,
  `Seasonal_Joint_Promo` varchar(15) DEFAULT NULL,
  `Local_Regional_Category_Num` varchar(15) DEFAULT NULL,
  `Local_Regional_Tv` varchar(15) DEFAULT NULL,
  `Local_Regional_Radio` varchar(15) DEFAULT NULL,
  `Local_Regional_Digital_Media` varchar(15) DEFAULT NULL,
  `Local_Regional_Demo` varchar(15) DEFAULT NULL,
  `Local_Regional_Digital_Media_Broadcast` varchar(15) DEFAULT NULL,
  `Local_Regional_Broadcast_Digital_Media` varchar(15) DEFAULT NULL,
  `Local_Regional_Other` varchar(15) DEFAULT NULL,
  `Local_Regional_Pilot_Project` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_7_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_14_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_31_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Tv_45_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_7_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_14_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_31_Days` varchar(15) DEFAULT NULL,
  `Short_Life_Radio_45_Days` varchar(15) DEFAULT NULL,
  `Tv_Broadcast_Digital_Media` varchar(15) DEFAULT NULL,
  `Digital_Media_Broadcast_Tv` varchar(15) DEFAULT NULL,
  `Digital_Media_Other` varchar(15) DEFAULT NULL,
  `Radio_Digital_Media` varchar(15) DEFAULT NULL,
  `Actra_Online` varchar(15) DEFAULT NULL,
  `Pilot_Project` varchar(15) DEFAULT NULL,
  `Article_2403` varchar(15) DEFAULT NULL,
  `Article_2404` varchar(15) DEFAULT NULL,
  `Article_2405` varchar(15) DEFAULT NULL,
  `Article_2406` varchar(15) DEFAULT NULL,
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
  
drop table  if exists form_actra_intent;
CREATE TABLE `form_actra_intent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '1',
  `Project_Id` integer,
  `Advertiser` varchar(150) DEFAULT NULL,
  `Product` varchar(150) DEFAULT NULL,
  `Agency_Name` varchar(150) DEFAULT NULL,
  `Producer_Name` varchar(150) DEFAULT NULL,
  `Producer_Email` varchar(150) DEFAULT NULL,
  `Signatory_Engager` varchar(150) DEFAULT NULL,
  `Director_Name` varchar(150) DEFAULT NULL,
  `Production_House_Name` varchar(150) DEFAULT NULL,
  `Line_Producer_Name` varchar(150) DEFAULT NULL,
  `Casting_Director_Name` varchar(150) DEFAULT NULL,
  `Multi_Branch` tinyint(1) DEFAULT '0',
  `Multi_Branch_Locations` varchar(250) DEFAULT NULL,
  `Use_Canada` tinyint(1) DEFAULT '0',
  `Use_Us` tinyint(1) DEFAULT '0',
  `Digital_Media_1` tinyint(1) DEFAULT '0',
  `Digital_Media_2` tinyint(1) DEFAULT '0',
  `Other_1` tinyint(1) DEFAULT '0',
  `Other_2` tinyint(1) DEFAULT '0',
  `National_Com` tinyint(1) DEFAULT '0',
  `Tv` tinyint(1) DEFAULT '0',
  `Radio` tinyint(1) DEFAULT '0',
  `Psa` tinyint(1) DEFAULT '0',
  `Loc_Region_Com` tinyint(1) DEFAULT '0',
  `Double_Shoot` tinyint(1) DEFAULT '0',
  `Demo` tinyint(1) DEFAULT '0',
  `A706_Exclusions` tinyint(1) DEFAULT '0',
  `A707_Waivers` tinyint(1) DEFAULT '0',
  `Seasonal_Com` tinyint(1) DEFAULT '0',
  `Tags` tinyint(1) DEFAULT '0',
  `Dealer` tinyint(1) DEFAULT '0',
  `Informercial` tinyint(1) DEFAULT '0',
  `Short_Life_7_Days` tinyint(1) DEFAULT '0',
  `Short_Life_14_Days` tinyint(1) DEFAULT '0',
  `Short_Life_31_Days` tinyint(1) DEFAULT '0',
  `Short_Life_45_Days` tinyint(1) DEFAULT '0',
  `Commercial_Id1` int(11) DEFAULT NULL,
  `Commercial_Id2` int(11) DEFAULT NULL,
  `Commercial_Id3` int(11) DEFAULT NULL,
  `Minor` tinyint(1) DEFAULT '0',
  `Num_Minors` int(11) DEFAULT NULL,
  `Minor_Ages` varchar(75) DEFAULT NULL,
  `Num_Extras_General` int(11) DEFAULT NULL,
  `Num_Extras_Group` int(11) DEFAULT NULL,
  `Num_Extras_Group_31` int(11) DEFAULT NULL,
  `Stunts` tinyint(1) DEFAULT '0',
  `Stunt_Coordinator` varchar(25) DEFAULT NULL,
  `Stunt_Type` varchar(100) DEFAULT NULL,
  `Ext_Scenes` tinyint(1) DEFAULT '0' COMMENT 'Exterior Scenes',
  `Ext_Scenes_Type` varchar(100) DEFAULT NULL,
  `Location_Shoot_40_Radius` tinyint(1) DEFAULT '0',
  `Weather_Permitting` tinyint(1) DEFAULT '0',
  `Weekend_Night` tinyint(1) DEFAULT '0',
  `Nude_Scenes` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `Commercial_Id1` (`Commercial_Id1`),
  KEY `Commercial_Id2` (`Commercial_Id2`),
  KEY `Commercial_Id3` (`Commercial_Id3`),
  CONSTRAINT `form_actra_intent_ibfk_1` FOREIGN KEY (`Commercial_Id1`) REFERENCES `commercial` (`id`),
  CONSTRAINT `form_actra_intent_ibfk_2` FOREIGN KEY (`Commercial_Id2`) REFERENCES `commercial` (`id`),
  CONSTRAINT `form_actra_intent_ibfk_3` FOREIGN KEY (`Commercial_Id3`) REFERENCES `commercial` (`id`),
  CONSTRAINT `form_actra_intent_ibfk_4` foreign key(Project_Id) references project(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*
drop table  if exists form_actra_work_permit;
CREATE TABLE `form_actra_work_permit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '1',
  `Professional_Name` varchar(150) DEFAULT NULL,
  `Citizenship` varchar(50) DEFAULT NULL,
  `Legal_Name` varchar(150) DEFAULT NULL,
  `Home_Phone` varchar(25) DEFAULT NULL,
  `Cell_Phone` varchar(25) DEFAULT NULL,
  `Talent_Email_Address` varchar(100) DEFAULT NULL,
  `Talent_Address_Id` int(11) DEFAULT NULL,
  `Talent_Agency_Name` varchar(150) DEFAULT NULL,
  `Talent_Dob` date DEFAULT NULL, 
  `Guardian_Name` varchar(150) DEFAULT NULL,
  `Talent_Gender` tinyint(1) DEFAULT '0',
  `Sin_Num` varchar(1000) DEFAULT NULL,
  `Sag_Aftra_Member` tinyint(1) DEFAULT '0',
  `Equity_Member` tinyint(1) DEFAULT '0',
  `Apprentice_Member` tinyint(1) DEFAULT '0',
  `Apprentice_Num` varchar(45) DEFAULT NULL,
  `Adhered_Engager` varchar(150) DEFAULT NULL,
  `Advertiser` varchar(150) DEFAULT NULL,
  `Production_House` varchar(150) DEFAULT NULL,
  `Members_Auditioned_Num` int(11) DEFAULT NULL,
  `Members_Auditioned_Names` varchar(1000) DEFAULT NULL,
  `Members_Auditioned_Names_Line2` varchar(1000) DEFAULT NULL,
  `Commercial_Name` varchar(100) DEFAULT NULL,
  `Com_Date` date DEFAULT NULL,
  `Char_Name_Desc` varchar(500) DEFAULT NULL,
  `Num_Com` int(11) DEFAULT NULL,
  `Com_Id` varchar(30) DEFAULT NULL,
  `Com_Type_Tv` tinyint(1) DEFAULT '0',
  `Com_Type_Radio` tinyint(1) DEFAULT '0',
  `Com_Type_Digital_Media` tinyint(1) DEFAULT '0',
  `Performance_Category` varchar(11) DEFAULT NULL,
  `Com_Location` varchar(100) DEFAULT NULL,
  `Applicant_Sign_Id` int(11) DEFAULT NULL,
  `Work_Permit_Fee` decimal(6,2) DEFAULT NULL,
  `Work_Permit_Num` varchar(45) DEFAULT NULL,
  `Paid_By` varchar(20) DEFAULT NULL,
  `Payment_Method` varchar(20) DEFAULT NULL,
  `Receipt_By_Email` tinyint(1) DEFAULT '0',
  `Receipt_To` varchar(500) DEFAULT NULL,
  `CC_Holder_Name` varchar(100) DEFAULT NULL COMMENT 'Name of Credit Card Holder',
  `CC_Holder_Sign_Id` int(11) DEFAULT NULL,
  `CC_Num` varchar(45) DEFAULT NULL,
  `CC_Expiration_Date` date DEFAULT NULL,
  `Engager_Id` varchar(30) DEFAULT NULL,
  `Date_Approved` date DEFAULT NULL,
  `Date_Processed` date DEFAULT NULL,
  `Approved_Denied_By` varchar(100) DEFAULT NULL,
  `Qualifying` tinyint(1) DEFAULT '0',
  `Denial_Reason` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `Applicant_Sign_Id_idx` (`Applicant_Sign_Id`),
  KEY `CC_Holder_Sign_Id_idx` (`CC_Holder_Sign_Id`),
  CONSTRAINT `Applicant_Sign_Id` FOREIGN KEY (`Applicant_Sign_Id`) REFERENCES `contact_doc_event` (`Id`),
  CONSTRAINT `CC_Holder_Sign_Id` FOREIGN KEY (`CC_Holder_Sign_Id`) REFERENCES `contact_doc_event` (`Id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='Work permit that allows perfomer to work on the production/project.';


-- Add new columns in the table form_actra_work_permit.
alter table form_actra_work_permit 
add column Talent_Agency_Phone varchar(25) DEFAULT NULL after Talent_Agency_Name,
add column Product_Name varchar(150) DEFAULT NULL after Advertiser;

-- Add foreign key to CanadaProjectDetails to Project
-- alter table project drop column Canada_Project_Details_Id;
-- alter table project drop FOREIGN KEY  `Canada_Project_Details_Id`;

alter table project add column Canada_Project_Details_Id INT(11) DEFAULT NULL; 
ALTER TABLE `project` ADD CONSTRAINT `Canada_Project_Details_Id`  FOREIGN KEY (`Canada_Project_Details_Id`)  REFERENCES `canada_project_detail` (`id`);

-- Table for Talent assigned to the Actra Intent form
drop table if exists talent;
CREATE TABLE `talent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Name` varchar(100) DEFAULT NULL,
  `Minor` tinyint(1) DEFAULT '0',
  `Category` varchar(50) DEFAULT NULL,
  `Shoot_Date` date DEFAULT NULL,
  `Location` varchar(150) DEFAULT NULL,
  `Intent_Id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Intent_Id_idx` (`Intent_Id`),
  CONSTRAINT `Intent_Id` FOREIGN KEY (`Intent_Id`) REFERENCES `form_actra_intent` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*
-- Add new fields in daily_time table
alter table daily_time 
	add column Trvl_To_Loc_From decimal(4,2),
	add column Trvl_To_Loc_To decimal(4,2),
	add column Trvl_From_Loc_From decimal(4,2),
	add column Trvl_From_Loc_To decimal(4,2),
	add column date2 date null,
	add column call_time2 decimal(4,2),
	add column wrap2 decimal(4,2),
	add column call_type varchar(20) null,
	modify column date date null;

-- Alter new fields in User table
ALTER TABLE user ADD COLUMN GST_Number VARCHAR(20) NULL DEFAULT NULL,
	add column QST_Number varchar(20) NULL DEFAULT NULL,
	add column Full_Member_Num varchar(20) NULL DEFAULT NULL,
-- Flags to determine if functionality is US or Canadian specific	
	ADD COLUMN Show_US Boolean DEFAULT true,
	add column Show_Canada Boolean DEFAULT false,
	ADD COLUMN Agent_Email VARCHAR(100) NULL DEFAULT NULL,
	ADD COLUMN SendDocumentsTo_Email VARCHAR(100) NULL DEFAULT NULL,
	ADD COLUMN Guardian_Name VARCHAR(75) NULL DEFAULT NULL after Minor;

-- Office table to track which office a document has been sent to.
drop table if exists office;
CREATE TABLE `office` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Office_Name` varchar(75) NOT NULL,
  `Branch_Code` varchar(10) DEFAULT NULL,
  `Email_Address` varchar(200) DEFAULT NULL,
  `Office_Type` varchar(45) DEFAULT NULL,
  `Country_Code` char(2) DEFAULT NULL,
  `Sort_Order` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8 COMMENT='Used for Canada ACTRA offices.';

-- Cross reference table between Actra contract and office
drop table if exists actra_contract_office;
CREATE TABLE `actra_contract_office` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Office_Id` int(11) NOT NULL,
  `Form_Id` int(11) NOT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

/* Cross reference table between Actra work permit and office *
drop table if exists actra_work_permit_office;
CREATE TABLE `actra_work_permit_office` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Office_Id` int(11) NOT NULL,
  `Form_Id` int(11) NOT NULL,
  PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

-- Insert for Office table using production values
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (1, 'Calgary', 'CAL', 'alberta@actra.ca', 'ACTRA', 'CA', 1);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (2, 'Halifax', 'HAL', 'maritimes@actra.ca', 'ACTRA', 'CA', 2);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (3, 'Montreal', 'MTL', 'sjoutel@actra.ca', 'ACTRA', 'CA', 3);  
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (4, 'Ottawa', 'OTW', 'ottawa@actra.ca', 'ACTRA', 'CA', 4);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (5, 'Saskatchewan', 'SAS', 'saskatchewan@actra.ca', 'ACTRA', 'CA', 5);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (6, 'St. John''s', 'STJ', 'newfoundland@actra.ca', 'ACTRA', 'CA', 6);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (7, 'Toronto', 'TOR', 'nca@actratoronto.com', 'ACTRA', 'CA', 7);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (8, 'Vancouver', 'VAN', 'Sheryl.Scott@ubcp.com;Ashton.Ehnes@ubcp.com', 'ACTRA', 'CA', 8);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (9, 'Winnipeg', 'WIN', 'manitoba@actra.ca', 'ACTRA', 'CA', 9);

-- For testing use the following inserts substituting the email address you want the pdf sent to.
set @email = 'tanugu@theteamcompanies.com';
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (1, 'Calgary', 'CAL', @email, 'ACTRA', 'CA', 1);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (2, 'Halifax', 'HAL', @email, 'ACTRA', 'CA', 2);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (3, 'Montreal', 'MTL', @email, 'ACTRA', 'CA', 3);  
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (4, 'Ottawa', 'OTW', @email, 'ACTRA', 'CA', 4);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (5, 'Saskatchewan', 'SAS', @email, 'ACTRA', 'CA', 5);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (6, 'St. John''s', 'STJ', @email, 'ACTRA', 'CA', 6);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (7, 'Toronto', 'TOR', @email, 'ACTRA', 'CA', 7);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (8, 'Vancouver', 'VAN', @email, 'ACTRA', 'CA', 8);
INSERT INTO office (id, Office_Name, Branch_Code, Email_Address, Office_Type, Country_Code, Sort_Order) VALUES (9, 'Winnipeg', 'WIN', @email, 'ACTRA', 'CA', 9);

/* */
-- --------------------- End of release 4.0.0 changes ------------------------------------------
/* *

-- LS-1660 Add new Agent fields in the User
alter table user add column Agent_Name varchar(60) DEFAULT NULL after Agent_Email,
	add column Agent_Phone varchar(25) DEFAULT NULL after Agent_Name;

-- --------------------- End of release 4.0.1 changes ------------------------------------------
/* *
-- added for Omni-Gen processing
alter table project add column Skip Boolean DEFAULT false after notifying;

/* */
-- --------------------- End of release 4.1.0 changes ------------------------------------------
/* *
-- LS-1680 Update the version field with the last 2 digits of year for the old records.
update form_w9 set version=14 where version=1;

-- TTCV-72 new Employment fields for OmniGen
alter table employment
	add Interfaced timestamp null,
	add Processed timestamp null,
	add Payroll_Project_Cast_Id varchar(50) null, 
	add Skip boolean default false;

-- LS-1616 Support custom Timesheet expense/reimbursement categories
alter table timesheet add column Pay_Category1 varchar(100) DEFAULT null after Time_Sent,
	add column Pay_Category2 varchar(100) DEFAULT null after Pay_Category1,
	add column Pay_Category3 varchar(100) DEFAULT null after Pay_Category2,
	add column Pay_Category4 varchar(100) DEFAULT null after Pay_Category3;

/* */
-- --------------------- End of release 4.2.0 changes ------------------------------------------
/* *

--LS-1706 the Contract field should get populated with "DGA" for DG projects
UPDATE unions SET GS_Local_Num = 'DG' WHERE (Id = '116');
UPDATE unions SET GS_Local_Num = 'NonU' WHERE (Id = '6');

-- improve performance; melody showed high use and mean time for query on this field
alter table contact_document add index `Related_Form_Id` (`Related_Form_Id`);

alter table contact_document
	add Interfaced timestamp null,
	add Processed timestamp null,
	add Skip boolean default false;
/* */
-- --------------------- End of release 4.3.0 changes ------------------------------------------
/* *
-- Add 4.4.0 changes here:
--LS-1773 Add Apprentice # Field in the User

ALTER TABLE user ADD Apprentice_Num varchar(20) AFTER Full_Member_Num;

-- Add new columns in the table form_actra_intent.
alter table form_actra_intent
add column Producer_Email varchar(150) DEFAULT NULL after Producer_Name,
add column A707_Waivers tinyint(1) DEFAULT '0' after A706_Exclusions,
add column Stunt_Coordinator varchar(25) DEFAULT NULL after Stunts;

-- Drop column Commercial_Id from the table form_actra_intent.
ALTER table form_actra_intent DROP foreign key Commercial_Id;
ALTER table form_actra_intent DROP key Commercial_Id_idx;
ALTER table form_actra_intent DROP Column Commercial_Id;

-- Add new columns Commercial_Id1, Commercial_Id2 & Commercial_Id3 from in table form_actra_intent.
ALTER table form_actra_intent ADD column Commercial_Id1 integer DEFAULT NULL after Short_Life_45_Days,
ADD column Commercial_Id2 integer DEFAULT NULL after Commercial_Id1, 
ADD column Commercial_Id3 integer DEFAULT NULL after Commercial_Id2;
ALTER table form_actra_intent ADD FOREIGN KEY(Commercial_Id1) references commercial(Id),
ADD FOREIGN KEY(Commercial_Id2) references commercial(Id),
ADD FOREIGN KEY(Commercial_Id3) references commercial(Id);

-- Add new column Project_Id in the table form_actra_intent.
alter table form_actra_intent add column Project_Id integer after version;
alter table form_actra_intent add foreign key(Project_Id) references project(Id);
-- update values in the Project_Id column of the form_actra_intent table.
UPDATE form_actra_intent f set f.Project_Id = (select cd.Project_Id from contact_document cd where cd.Related_Form_Id = f.Id and 
	cd.form_type='ACTRA_INTENT');
-- Query to verify the updated values in the Project_Id column in the table form_actra_intent.
select cd.id CD_Id, cd.related_form_id CD_Related_Form_Id, f.id Form_Id, cd.project_id CD_Project, f.project_id Form_Project from contact_document cd,form_actra_intent f 
	where form_type='ACTRA_INTENT' and cd.related_form_id = f.id order by related_form_id;

-- Change Category_Id to Category.
SET foreign_key_checks = off;
ALTER table talent DROP foreign key Category_Id;
ALTER table talent DROP key Category_Id_idx;
ALTER table talent CHANGE Category_Id Category varchar(50);

/* */
-- --------------------- End of release 4.4.0 changes ------------------------------------------
/* *
-- 4.5.0 changes

-- TTCV-176 add to contact_document
alter table contact_document add payroll_address_number int;

/* */
-- --------------------- End of release 4.5.0 changes ------------------------------------------
/* *

-- LS-1853: Remove the project reference in FormActraIntent. We'll get the project from the associated Contact Document.
SET foreign_key_checks = off;
ALTER table form_actra_intent DROP foreign key form_actra_intent_ibfk_4;
ALTER table form_actra_intent DROP column Project_Id;

-- Add agent table for Canada. Will probably be used for US talent as well.
drop table if exists agent ;
CREATE TABLE `agent` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `First_Name` varchar(30) DEFAULT NULL,
  `Last_Name` varchar(30) DEFAULT NULL,
  `Display_Name` varchar(62) DEFAULT NULL,
  `Email_Address` varchar(100) DEFAULT NULL,
  `Office_Phone` varchar(25) DEFAULT NULL,
  `Agency_Name` varchar(50) DEFAULT NULL,
  `Agency_Address_id` int(11) DEFAULT NULL,
  `Selected` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`Id`),
  KEY `Agency_Address_Id_idx` (`Agency_Address_id`),
  CONSTRAINT `fk_agency_address_id` FOREIGN KEY (`Agency_Address_id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

-- agent_user reference table for many to many mapping from agent to user tables
drop table if exists agent_user;
CREATE TABLE `agent_user` (
  `Agent_Id` int(11) NOT NULL,
  `User_Id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/* */
-- --------------------- End of release 4.6.0 changes ------------------------------------------
/* *
 
-- LS-1958 Add Other_Gender_Desc column in the table User
alter table user
	add column Other_Gender_Desc varchar(50) after Gender;

alter table role add column Ls_Occ_Code varchar(10) after department_id,
	add column Role_Select_Type varchar(30) after Ls_Occ_Code;

-- 4/10/19: LS-1892 update mask bits to add new departments for Hybrid and Tours productions
-- note, 18446744073709551615 is value of unsigned 64-bit (BIGINT) when all bits are 1
/* *
set @maskT1 = 1 << (53-1); -- generate the mask value for Tours: Band
set @maskT2 = 1 << (54-1); -- generate the mask value for Tours: Crew
set @maskT3 = 1 << (55-1); -- generate the mask value for Tours: Dancers
set @maskT4 = 1 << (56-1); -- generate the mask value for Tours: Drivers
set @maskT5 = 1 << (57-1); -- generate the mask value for Tours: Officer
set @maskH1 = 1 << (58-1); -- generate the mask value for Hybrid: Domestic
set @maskH2 = 1 << (59-1); -- generate the mask value for Hybrid: Catering
set @maskH3 = 1 << (45-1); -- generate the mask value for Hybrid: Stationary

set @maskT = @maskT1 |  @maskT2 |  @maskT3 |  @maskT4 |  @maskT5;
set @maskH = @maskH1 |  @maskH2 |  @maskH3 |  @maskT4; -- Hybrid also gets Drivers

-- select @maskT, hex(@maskT), @maskH, hex(@maskH); -- for testing

/** *
-- tours productions & projects
select id, dept_mask, hex(dept_mask), title from production p where type = 'TOURS' and status = 'ACTIVE'
	order by p.title;
select j.id, j.dept_mask, hex(j.dept_mask), j.title from production p, project j where p.type = 'TOURS' and p.status = 'ACTIVE'
	and j.production_id = p.id
	order by p.title, j.title;

-- hybrid productions & projects
select p.id, dept_mask, hex(dept_mask), title from production p, payroll_preference pp where type = 'TV_COMMERCIALS' and status = 'ACTIVE'
	and p.payroll_preference_id = pp.id and pp.include_touring
	order by p.title;
select j.id, j.dept_mask, hex(j.dept_mask), p.title, j.title from production p, payroll_preference pp, project j where p.type = 'TV_COMMERCIALS' and p.status = 'ACTIVE'
	and p.payroll_preference_id = pp.id and pp.include_touring
	and j.production_id = p.id
	order by p.title, j.title;
/* */
/* *
-- Tours productions
update production p set dept_mask = (dept_mask | @maskT) where p.id in 
	(select id from (select id from production p where type = 'TOURS' and p.status = 'ACTIVE') as temp_tbl);
-- Tours projects
update project j set dept_mask = (dept_mask | @maskT) where j.id in 
	(select id from (select j.id from production p, project j where p.type = 'TOURS' and p.status = 'ACTIVE'
		and j.production_id = p.id) as temp_tbl);
/* */
-- hybrid productions
/* *
update production p set dept_mask = (dept_mask | @maskH) where p.id in 
	(select id from (select p.id, dept_mask, hex(dept_mask), title from production p, payroll_preference pp where type = 'TV_COMMERCIALS' and p.status = 'ACTIVE'
	and p.payroll_preference_id = pp.id and pp.include_touring) as temp_tbl);
/* *
-- hybrid projects
update project j set dept_mask = (dept_mask | @maskH) where j.id in 
	(select id from (select j.id from production p, payroll_preference pp, project j where p.type = 'TV_COMMERCIALS' and p.status = 'ACTIVE'
		and p.payroll_preference_id = pp.id and pp.include_touring
		and j.production_id = p.id) as temp_tbl);

/* */
-- --------------------- End of release 4.7.0 changes ------------------------------------------
/* *
-- Add US talent to the product table
INSERT INTO product
(`Id`,
`SKU`,
`Button`,
`Title`,
`Description`,
`Type`,
`Max_Projects`,
`Max_Users`,
`Duration`,
`Sms_Enabled`,
`Price`)
VALUES
(13,
'T-CM-US1-9',
'U.S. Talent',
'TTC Online - U.S. Talent',
'U.S. Talent production',
'US_TALENT',
999,
1000,
0,
0,
0.00);

-- A1 Talent Contract
drop table if exists form_a1_contract;
CREATE TABLE `form_a1_contract` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '0',
  `Wardrobe_Provided_By` varchar(30) DEFAULT NULL,
  `Special_Provisions` varchar(1000) DEFAULT NULL,
  `Special_Provisions_Sign_Id` int(11) DEFAULT NULL,
  `Minor_Consent_Signer` varchar(30) DEFAULT NULL,
  `Minor_Consent_Sign_Id` int(11) DEFAULT NULL,
  `Compensation` varchar(25) DEFAULT NULL,
  `Talent_Name` varchar(100) DEFAULT NULL,
  `Talent_Phone` varchar(12) DEFAULT NULL,
  `Talent_Email_Address` varchar(100) DEFAULT NULL,
  `Talent_Primary_Address_Id` int(11) DEFAULT NULL,
  `Talent_C_O_Name` varchar(30) DEFAULT NULL,
  `Talent_C_O_Address_Id` int(11) DEFAULT NULL,
  `Talent_Pay_To` varchar(30) DEFAULT NULL,
  `Talent_Sign_Id` int(11) DEFAULT NULL,
  `Contract_Creation_Date` date DEFAULT NULL,
  `Estimate_Num` varchar(30) DEFAULT NULL,
  `Job_Num` varchar(30) DEFAULT NULL,
  `Agency_Name` varchar(30) DEFAULT NULL,
  `Agency_Address_Id` int(11) DEFAULT NULL,
  `Agency_Email_Address` varchar(100) DEFAULT NULL,
  `Producer_Name` varchar(30) DEFAULT NULL,
  `Producer_Sign_Name` varchar(30) DEFAULT NULL,
  `Producer_Sign_Id` int(11) DEFAULT NULL,
  `Prod_Company_Name` varchar(50) DEFAULT NULL,
  `Advertiser_Name` varchar(30) DEFAULT NULL,
  `Advertiser_Products` varchar(250) DEFAULT NULL,
  `Non_Evening_Wear_Cnt` int(11) DEFAULT NULL,
  `Evening_Wear_Cnt` int(11) DEFAULT NULL,
  `Total_Wardrobe_Fee` decimal(6,2) DEFAULT NULL,
  `Flight_Insurance_Days` int(11) DEFAULT NULL,
  `Place_Engagement` varchar(100) DEFAULT NULL,
  `Date_Hour_Engagement` varchar(50) DEFAULT NULL,
  `Services_Rendered_Address_Id` int(11) DEFAULT NULL,
  `Part_Played` varchar(100) DEFAULT NULL,
  `Multi_Track_Sweetening` tinyint(1) DEFAULT '0',
  `Dancer_Footwear_Cnt` int(11) DEFAULT NULL,
  `Total_Dancer_Footwear_Fee` decimal(6,2) DEFAULT NULL,
  `Dealer_Commercial` tinyint(1) DEFAULT '0',
  `Dealer_Com_TypeA` tinyint(1) DEFAULT '0',
  `Dealer_Com_TypeB` tinyint(1) DEFAULT '0',
  `Seasonal_Com` tinyint(1) DEFAULT '0',
  `Test_Market_Com` tinyint(1) DEFAULT '0',
  `Non_Air_Com` tinyint(1) DEFAULT '0',
  `Cable_Only_Com` tinyint(1) DEFAULT '0',
  `Internet_Only_Com` tinyint(1) DEFAULT '0',
  `New_Media_Com` tinyint(1) DEFAULT '0',
  `Social_Media_Com` tinyint(1) DEFAULT '0',
  `Work_In_Smoke` tinyint(1) DEFAULT '0',
  `Lang_Trans_Service` tinyint(1) DEFAULT '0',
  `Other_Residual` tinyint(1) DEFAULT '0',
  `Other_Residual_Text` varchar(75) DEFAULT NULL,
  `Consent_Internet` tinyint(1) DEFAULT '0',
  `Consent_New_Media` tinyint(1) DEFAULT '0',
  `Consent_Social_Media` tinyint(1) DEFAULT '0',
  `Consent_Dealer_Rates` tinyint(1) DEFAULT '0',
  `Strike_Foreign_Use` tinyint(1) DEFAULT '0',
  `Strike_Theatrical_Industry_Use` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`Id`),
  KEY `form_a1_contract_ibfk_1_idx` (`Talent_Primary_Address_Id`),
  KEY `form_a1_contractt_ibfk_2_idx` (`Talent_C_O_Address_Id`),
  KEY `form_a1_contract` (`Agency_Address_Id`),
  KEY `form_a1_contract_ibfk_4_idx` (`Services_Rendered_Address_Id`),
  CONSTRAINT `form_a1_contract_ibfk_1` FOREIGN KEY (`Talent_Primary_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a1_contract_ibfk_2` FOREIGN KEY (`Talent_C_O_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a1_contract_ibfk_3` FOREIGN KEY (`Agency_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a1_contract_ibfk_4` FOREIGN KEY (`Services_Rendered_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

-- A2 Talent Contract
drop table if exists form_a2_contract;
CREATE TABLE `form_a2_contract` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '0',
  `Wardrobe_Provided_By` varchar(30) DEFAULT NULL,
  `Special_Provisions` varchar(1000) DEFAULT NULL,
  `Special_Provisions_Sign_Id` int(11) DEFAULT NULL,
  `Minor_Consent_Signer` varchar(30) DEFAULT NULL,
  `Minor_Consent_Sign_Id` int(11) DEFAULT NULL,
  `Compensation` varchar(25) DEFAULT NULL,
  `Talent_Name` varchar(100) DEFAULT NULL,
  `Talent_Phone` varchar(12) DEFAULT NULL,
  `Talent_Email_Address` varchar(100) DEFAULT NULL,
  `Talent_Primary_Address_Id` int(11) DEFAULT NULL,
  `Talent_C_O_Name` varchar(30) DEFAULT NULL,
  `Talent_C_O_Address_Id` int(11) DEFAULT NULL,
  `Talent_Pay_To` varchar(30) DEFAULT NULL,
  `Talent_Sign_Id` int(11) DEFAULT NULL,
  `Contract_Creation_Date` date DEFAULT NULL,
  `Estimate_Num` varchar(30) DEFAULT NULL,
  `Job_Num` varchar(30) DEFAULT NULL,
  `Agency_Name` varchar(30) DEFAULT NULL,
  `Agency_Address_Id` int(11) DEFAULT NULL,
  `Agency_Email_Address` varchar(100) DEFAULT NULL,
  `Producer_Name` varchar(30) DEFAULT NULL,
  `Producer_Sign_Name` varchar(30) DEFAULT NULL,
  `Producer_Sign_Id` int(11) DEFAULT NULL,
  `Prod_Company_Name` varchar(50) DEFAULT NULL,
  `Advertiser_Name` varchar(30) DEFAULT NULL,
  `Advertiser_Products` varchar(250) DEFAULT NULL,
  `Non_Evening_Wear_Cnt` int(11) DEFAULT NULL,
  `Evening_Wear_Cnt` int(11) DEFAULT NULL,
  `Total_Wardrobe_Fee` decimal(6,2) DEFAULT NULL,
  `Flight_Insurance_Days` int(11) DEFAULT NULL,
  `Place_Engagement` varchar(100) DEFAULT NULL,
  `Date_Hour_Engagement` varchar(50) DEFAULT NULL,
  `Services_Rendered_Address_Id` int(11) DEFAULT NULL,
  `Part_Played` varchar(100) DEFAULT NULL,
  `Use_Unlimited` tinyint(1) DEFAULT '0',
  `Use_13_Weeks` tinyint(1) DEFAULT '0',
  `Use_Cable_Only` tinyint(1) DEFAULT '0',
  `Use_Internet_Only` tinyint(1) DEFAULT '0',
  `Use_New_Media_Only` tinyint(1) DEFAULT '0',
  `Use_Social_Media_Only` tinyint(1) DEFAULT '0',
  `Talent_Agency_Name` varchar(50) DEFAULT NULL,
  `Agent_Commission` decimal(6,2) DEFAULT NULL,
  `Travel_Within_Zone_Days` int(11) DEFAULT NULL,
  `Using_Moped` tinyint(1) DEFAULT '0',
  `Moped_Type` varchar(30) DEFAULT NULL,
  `Moped_Tolls_Amt` decimal(6,2) DEFAULT NULL,
  `Moped_Mileage_Amt` decimal(6,2) DEFAULT NULL,
  `Moped_Parking_Amt` decimal(6,2) DEFAULT NULL,
  `Using_Auto` tinyint(1) DEFAULT '0',
  `Auto_Type` varchar(30) DEFAULT NULL,
  `Auto_Tolls_Amt` decimal(6,2) DEFAULT NULL,
  `Auto_Mileage_Amt` decimal(6,2) DEFAULT NULL,
  `Auto_Parking_Amt` decimal(6,2) DEFAULT NULL,
  PRIMARY KEY (`Id`),
  KEY `form_a2_contract_ibfk_1_idx` (`Talent_Primary_Address_Id`),
  KEY `form_a2_contractt_ibfk_2_idx` (`Talent_C_O_Address_Id`),
  KEY `form_a2_contract` (`Agency_Address_Id`),
  KEY `form_a2_contract_ibfk_4_idx` (`Services_Rendered_Address_Id`),
  CONSTRAINT `form_a2_contract_ibfk_1` FOREIGN KEY (`Talent_Primary_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a2_contract_ibfk_2` FOREIGN KEY (`Talent_C_O_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a2_contract_ibfk_3` FOREIGN KEY (`Agency_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `form_a2_contract_ibfk_4` FOREIGN KEY (`Services_Rendered_Address_Id`) REFERENCES `address` (`Id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

-- LS-1987 update category field lengths
ALTER TABLE form_actra_contract 
	CHANGE COLUMN Performance_Category Performance_Category VARCHAR(50) NULL DEFAULT NULL ;

-- LS-2013  Fields for TOCS user migration
ALTER Table User
	add column User_Name varchar(20) after account_number,
	add column Team_Employee bit default false after student,
	add column Source_system varchar(20) after member_type,
	add column Reset_click_count int default 0 after preferences,
	add column Password_expiration_days int default 0 after lock_out_time,
	add column Password_last_changed timestamp null after Password_expiration_days;
	-- add column Last_login timestamp default null after , -- field already in table, but not in java!

DROP Table if exists	User_Client	;	
CREATE Table	User_Client	(	
User_Id	Integer	NOT NULL,
Client_Id	Integer	NOT NULL,
Foreign key(User_Id) references User ( Id ),
		primary key (User_Id, Client_Id)
	)		engine "innodb" charset utf8;

-- Correct timestamp fields that had "on update" implied setting:
alter table contact_document 	change delivered delivered 		timestamp default 0;
alter table document 			change Loaded Loaded 			timestamp default 0;
alter table Changes 			change Start_Time Start_Time 	timestamp default 0;
alter table Changes 			change End_Time End_Time 		timestamp default 0;
alter table Date_Event 			change Start Start 				timestamp default 0;
alter table Exhibit_G 			change Date Date 				timestamp default 0;
alter table Date_Range 			change Start_Date Start_Date 	timestamp default 0;
alter table Document_Chain 		change Revised Revised 			timestamp default 0;
alter table Packet 				change LastModified LastModified timestamp default 0;

alter table form_actra_work_permit modify commercial_name varchar(250);

/* */
-- --------------------- End of release 4.8.0 changes ------------------------------------------
/* *
-- --------------------- Start of release 4.10.0 changes:

-- LS-2028, LS-2099 FEIN encryption process
-- Tables & Fields being updated/encrypted
-- 		Form_G4.employer_FEIN
-- 		Form_WTPA.fedid_Number
-- 		Payroll_Preference.fedid_Number
-- 		Start_Form.federal_Tax_Id
-- 		User.federal_Tax_Id
-- 		Weekly_Time_card.fed_Corp_Id

-- Create new columns used by the encryption process:
alter table Form_G4 add column employer_FEIN_check 	varchar(11)   after employer_FEIN,
				    add column employer_FEIN_enc 	varchar(1000) after employer_FEIN;

alter table Form_WTPA add column fedid_Number_check varchar(11)   after fedid_Number,
					  add column fedid_Number_enc 	varchar(1000) after fedid_Number;

alter table Payroll_Preference 	add column fedid_Number_check   varchar(11)   after fedid_Number,
								add column fedid_Number_enc 	varchar(1000) after fedid_Number;

alter table Start_Form add column federal_Tax_Id_check  varchar(11)   after federal_Tax_Id,
					   add column federal_Tax_Id_enc 	varchar(1000) after federal_Tax_Id;

alter table User add column federal_Tax_Id_check varchar(11)   after federal_Tax_Id,
				 add column federal_Tax_Id_enc 	 varchar(1000) after federal_Tax_Id;

alter table Weekly_Time_card add column fed_Corp_Id_check   varchar(11)   after fed_Corp_Id,
							 add column fed_Corp_Id_enc 	varchar(1000) after fed_Corp_Id;

/* RENAME columns just before installing new TTCO version *

alter table Form_G4 change employer_FEIN 		employer_FEIN_clear varchar(11),
					change employer_FEIN_enc 	employer_FEIN 		varchar(1000);

alter table Form_WTPA change fedid_Number 		fedid_Number_clear  varchar(11),
					  change fedid_Number_enc 	fedid_Number 		varchar(1000);

alter table Payroll_Preference  change fedid_Number 		fedid_Number_clear  varchar(11),
								change fedid_Number_enc 	fedid_Number 		varchar(1000);

alter table Start_Form  change federal_Tax_Id 		federal_Tax_Id_clear varchar(11),
						change federal_Tax_Id_enc 	federal_Tax_Id 		 varchar(1000);

alter table User  change federal_Tax_Id 		federal_Tax_Id_clear varchar(11),
				  change federal_Tax_Id_enc 	federal_Tax_Id 		 varchar(1000);

alter table Weekly_Time_card 	change fed_Corp_Id 		fed_Corp_Id_clear   varchar(11),
								change fed_Corp_Id_enc 	fed_Corp_Id 		varchar(1000);
/* *

-- LS-2139 Adding Fields to Payroll Start for NU Time Card for Talent
 ALTER TABLE start_form 
	ADD COLUMN Emp_Reuse decimal(8,2) default 0.00,
	ADD COLUMN Emp_Agent_Commission decimal(8,4) default 0.00;

/* */
-- --------------------- End of release 4.10.0 changes ------------------------------------------
/* */
-- --------------------- START of release 4.11.0 changes:
/* *

--LS-2156 Add Work_Country Field in the weekly timecard 
alter table weekly_time_card add column Work_Country varchar(100) DEFAULT NULL after Work_Zip;

-- LS-2158 Add Work_Country Field in the payroll_preference 
alter table payroll_preference add column Work_Country varchar(50) DEFAULT 'US' after Work_State;

update payroll_preference pp, production p set pp.Work_Country = 'CA' 
	where p.payroll_preference_id = pp.id and p.type = 'CANADA_TALENT';

update start_form set work_county = 'US' where work_county is null;

alter table daily_time 
	add column disable_city Boolean DEFAULT false after country,
	add column disable_state Boolean DEFAULT false after disable_city,
	add column disable_country Boolean DEFAULT false after disable_state;

--LS-2280 Format Agent Commision field to two decimal places on Payroll Start
ALTER TABLE start_form
	MODIFY COLUMN Emp_Agent_Commission decimal(5,2) default null,
	MODIFY COLUMN Emp_Reuse decimal(8,2) default null;

/* */
-- --------------------- End of release 4.11.0 changes ------------------------------------------
/* */
-- --------------------- START of release 4.12.0 changes:
/* *

-- LS-2332 Updated My Account details page for Canada
ALTER TABLE user ADD Uda_Member varchar(20) after Guardian_Name ;

-- LS-2282 Day Rate Calculator - Premium OT Preference
ALTER TABLE payroll_preference 
	ADD COLUMN use_premium_rate Boolean DEFAULT true after use_prior_year_rates;

/* */
-- --------------------- End of release 4.12.0 changes ------------------------------------------
/* */
-- --------------------- START of release 4.13.0 changes:
/* *
-- SD-2341 error saving Indemnification form with long company name
alter table form_indem modify Company_Name varchar(35) null; -- applied to prod 6/20/2019

-- LS-2241 Add contract rate table
Drop Table if exists	Contract_Rate	;		
CREATE Table	Contract_Rate	(		
Id	Integer	NOT NULL PRIMARY KEY	AUTO_INCREMENT,	
Contract_Code	varchar(20)	NOT NULL,		-- Contract code, matches Contract table
Start_Date	date	NOT NULL default 0,		-- first effective date of this rate
End_Date	date	NOT NULL default 0,		-- last effective date of this rate
Reference	varchar(30)	default NULL,		-- paragraph, article, etc. where rate is defined
Rate_Key	varchar(30)	NOT NULL,		-- key/code unique per contract
Description	varchar(100)	default NULL,		-- textual description of rate
Rate	decimal(8,2)	default NULL,		-- default rate for all occupations
Rate_P	decimal(8,2)	default NULL,		-- rate for Category P (UDA)
Rate_SP	decimal(8,2)	default NULL,		-- rate for Category SP (UDA)
Rate_VO	decimal(8,2)	default NULL,		-- rate for Category VO (UDA)
Rate_PE	decimal(8,2)	default NULL,		-- rate for Category PE (UDA)
Rate_Group	decimal(8,2)	default NULL,		-- rate for Category Group (UDA)
Rate_Dem	decimal(8,2)	default NULL,		-- rate for Category DEM (UDA)
Rate_Ext	decimal(8,2)	default NULL		-- rate for Category EXT (UDA)
	)		engine 'innodb' charset utf8;	

insert into Contract_Rate values (1,'C-399L','2019-02-03','2099-12-31','','REST','Rest invasion - Location Mgrs & Scouts',50,0,0,0,0,0,0,0);

-- LS-2241 Add custom column types to PayJob table
alter table Pay_Job
	add column Custom1_Type varchar(10) not null default 'H' after custom1_premium,
	add column Custom2_Type varchar(10) not null default 'H' after custom2_premium,
	add column Custom3_Type varchar(10) not null default 'H' after custom3_premium,
	add column Custom4_Type varchar(10) not null default 'H' after custom4_premium,
	add column Custom5_Type varchar(10) not null default 'H' after custom5_premium,
	add column Custom6_Type varchar(10) not null default 'H' after custom6_premium;

-- LS-2241 Set custom column types based on premium flag and sign of multiplier
update pay_job set Custom1_Type = 
	case when custom_mult1 >= 0
		then case when custom1_premium then 'P' else 'H' end
		else case when custom1_premium then 'W' else 'D' end
		end
	where custom_mult1 is not null and (custom_mult1 < 0 or custom1_premium);

update pay_job set Custom2_Type = 
	case when custom_mult2 >= 0
		then case when custom2_premium then 'P' else 'H' end
		else case when custom2_premium then 'W' else 'D' end
		end
	where custom_mult2 is not null and (custom_mult2 < 0 or custom2_premium);

update pay_job set Custom3_Type = 
	case when custom_mult3 >= 0
		then case when custom3_premium then 'P' else 'H' end
		else case when custom3_premium then 'W' else 'D' end
		end
	where custom_mult3 is not null and (custom_mult3 < 0 or custom3_premium);

update pay_job set Custom4_Type = 
	case when custom_mult4 >= 0
		then case when custom4_premium then 'P' else 'H' end
		else case when custom4_premium then 'W' else 'D' end
		end
	where custom_mult4 is not null and (custom_mult4 < 0 or custom4_premium);

update pay_job set Custom5_Type = 
	case when custom_mult5 >= 0
		then case when custom5_premium then 'P' else 'H' end
		else case when custom5_premium then 'W' else 'D' end
		end
	where custom_mult5 is not null and (custom_mult5 < 0 or custom5_premium);

update pay_job set Custom6_Type = 
	case when custom_mult6 >= 0
		then case when custom6_premium then 'P' else 'H' end
		else case when custom6_premium then 'W' else 'D' end
		end
	where custom_mult6 is not null and (custom_mult6 < 0 or custom6_premium);
-- end of LS-2241 updates

/* */
-- --------------------- End of release 4.13.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.14.0 changes:
/* *
 -- LS-2196 Send to TPS button functionality
ALTER TABLE selection_item
	CHANGE COLUMN Name Name VARCHAR(100) NOT NULL ;

-- Inserts are only to be run in the Production db - LS-2196
INSERT into selection_item (id, Type, Name, Label) values (13001, 'BILLER-CA', 'Ann-Marie Boudreau', 'aboudreau@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13002, 'BILLER-CA', 'Eileen Limerick', 'elimerick@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13003, 'BILLER-CA', 'Gladys Laughlin', 'glaughlin@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13004, 'BILLER-CA', 'Inga Rotter', 'irotter@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13005, 'BILLER-CA', 'Janelle Bayerle', 'jbayerle@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13006, 'BILLER-CA', 'Mary Corigliano', 'mcorigliano@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13007, 'BILLER-CA', 'Susanne Hardaker', 'shardaker@theteamcompanies.com');
INSERT into selection_item (id, Type, Name, Label) values (13008, 'BILLER-CA', 'Susie Schwartz', 'sschwartz@theteamcompanies.com'); 
-- for testing:
-- delete from selection_item where type = 'BILLER-CA';
-- INSERT into selection_item (id, Type, Name, Label) values (13001, 'BILLER-CA', 'Teja', 'tanugu@theteamcompanies.com');
-- INSERT into selection_item (id, Type, Name, Label) values (13002, 'BILLER-CA', 'Avanthi', 'agopal@theteamcompanies.com');

--LS-2194 Branch code droptown for project details and Actra contract
alter table form_actra_contract add Office_Id int(11),
	add CONSTRAINT `form_actra_contract_Office_Id` FOREIGN KEY (`Office_Id`) REFERENCES `office` (`Id`);
alter table canada_project_detail add `Office_Id` int(11),
	add CONSTRAINT `canada_project_detail_Office_Id` FOREIGN KEY (`Office_Id`) REFERENCES `office` (`Id`);
ALTER TABLE form_actra_contract DROP Branch_Code;
ALTER TABLE canada_project_detail DROP Branch_Code;

-- LS-2363 Add the Branch Code Office to the Work Permit
alter table form_actra_work_permit add `Office_Id` int(11),
	add CONSTRAINT `form_actra_work_permit_Office_Idx` FOREIGN KEY (`Office_Id`) REFERENCES `office` (`Id`);

-- LS-2465: provide (re)invite URL link to team employees: ensure @teamcompanies accounts are marked
update user set team_employee = true where email_address like '%@theteamcompanies.com' and team_employee = false and status <> 'deleted';
update user set team_employee = true where email_address = 'admin@lightspeedeps.com' and team_employee = false;

-- for developer's databases (run in beta & prod by TTCV group):
-- ALTER TABLE user add COLUMN Agree_To_Terms bit not null default false after team_employee;

-- LS-2356 Uda Detail Page
CREATE TABLE `uda_project_detail` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Producer_Name` varchar(150) DEFAULT NULL,
  `Producer_Address_Id` int(11) DEFAULT NULL,
  `Producer_Phone` varchar(20) DEFAULT NULL,
  `Producer_Email` varchar(30) DEFAULT NULL,
  `Responsible_Name` varchar(150) DEFAULT NULL,
  `No_Uda_Member` varchar(150) DEFAULT NULL,
  `Advertiser_Name` varchar(150) DEFAULT NULL,
  `Commercial_Title` varchar(150) DEFAULT NULL,
  `Commercial_Description` varchar(150) DEFAULT NULL,
  `Commercial_Version` varchar(150) DEFAULT NULL,
  `Product_Name` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `Producer_Address_Id` (`Producer_Address_Id`),
  CONSTRAINT `uda_project_detail_idfk_1` FOREIGN KEY (`Producer_Address_Id`) REFERENCES `address` (`Id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

ALTER TABLE project ADD COLUMN  `Uda_Project_Details_Id` int(11) DEFAULT NULL;

ALTER TABLE project ADD CONSTRAINT `Uda_Project_Details_Id` FOREIGN KEY (`Uda_Project_Details_Id`) REFERENCES `uda_project_detail` (`id`);

-- Add new table "form_uda_contract" for the new form UDA Contract form, for LS-2273

CREATE TABLE `form_uda_contract` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '1',
  `Inm_Number` varchar(155) DEFAULT NULL,
  `Prod_Number` varchar(155) DEFAULT NULL,
  `Producer_Name` varchar(155) DEFAULT NULL,
  `Producer_Address_Id` int(11) DEFAULT NULL,
  `Producer_Phone` varchar(25) DEFAULT NULL,
  `Producer_Email` varchar(155) DEFAULT NULL,
  `Responsible_Name` varchar(155) DEFAULT NULL,
  `Producer_UDA` varchar(155) DEFAULT NULL,
   `Advertiser_Name_1` varchar(155) DEFAULT NULL,
  `Advertiser_Name_2` varchar(155) DEFAULT NULL,
  `Advertiser_Name_3` varchar(155) DEFAULT NULL,
  `Product_Name_1` varchar(155) DEFAULT NULL,
  `Product_Name_2` varchar(155) DEFAULT NULL,
  `Product_Name_3` varchar(155) DEFAULT NULL,
  `Active_Member` tinyint(1) DEFAULT '0',
  `Apprentice_Member` tinyint(1) DEFAULT '0',
  `Artist_Name` varchar(155) DEFAULT NULL,
  `Company_Name` varchar(155) DEFAULT NULL,
  `Artist_Address_Id` int(11) DEFAULT NULL,
  `Artist_Phone` varchar(25) DEFAULT NULL,
  `Artist_Email` varchar(155) DEFAULT NULL,
  `Artist_Gst` varchar(155) DEFAULT NULL,
  `Artist_Qst` varchar(155) DEFAULT NULL,
  `Social_Insurance_Number` varchar(1000) DEFAULT NULL,
  `No_UDA_Member` varchar(155) DEFAULT NULL,
  `Date_Of_Birth` date DEFAULT NULL,
  `UDA_Status` varchar(20) DEFAULT '0',
  `Permit_Holder` tinyint(1) DEFAULT '0',
  `Less_Than_18_Years` tinyint(1) DEFAULT '0',
  `Commercial_Title` varchar(155) DEFAULT NULL,
  `Commercial_Version` varchar(155) DEFAULT NULL,
  `Commercial_Description` varchar(155) DEFAULT NULL,
  `Recording_id_1` int(11) DEFAULT NULL,
  `Recording_id_2` int(11) DEFAULT NULL,
  `Recording_id_3` int(11) DEFAULT NULL,
  `Recording_Other` varchar(155) DEFAULT NULL,
  `Use_Table_A` tinyint(1) DEFAULT '0',
  `Use_Table_B` tinyint(1) DEFAULT '0',
  `Use_Table_C` tinyint(1) DEFAULT '0',
  `Use_Table_D` tinyint(1) DEFAULT '0',
  `Use_Table_E` tinyint(1) DEFAULT '0',
  `Use_Table_F` tinyint(1) DEFAULT '0',
  `Period` varchar(155) DEFAULT NULL,
  `First_Use_Date` date DEFAULT NULL,
  `Portfolio_Use_Authorization` tinyint(1) DEFAULT '0',
  `Exclusivity_Duration` tinyint(1) DEFAULT '0',
  `Exclusivity_Duration_Text` varchar(155) DEFAULT NULL,
  `Exclusivity_Service` varchar(155) DEFAULT NULL,
  `Function_Principal_Performer` tinyint(1) DEFAULT '0',
  `Function_SOC_Performer` tinyint(1) DEFAULT '0',
  `Function_Voice_Over` tinyint(1) DEFAULT '0',
  `Function_Principal_Extra` tinyint(1) DEFAULT '0',
  `Function_Extra` tinyint(1) DEFAULT '0',
  `Function_Chorist` tinyint(1) DEFAULT '0',
  `Function_Demonstrator` tinyint(1) DEFAULT '0',
  `Function_Other` tinyint(1) DEFAULT '0',
  `Function_Other_Text` varchar(155) DEFAULT NULL,
  `Fee_Rate` varchar(155) DEFAULT NULL,
  `Field_Name` varchar(155) DEFAULT NULL,
  `Negotiable_Recording_Percent` decimal(9,4) DEFAULT NULL,
  `Negotiable_Recording_Amount` decimal(9,4) DEFAULT NULL,
  `Negotiable_Inm_Use_Percent` decimal(9,4) DEFAULT NULL,
  `Negotiable_Inm_Use_Amount` decimal(9,4) DEFAULT NULL,
  `Negotiable_Other_Percent` decimal(9,4) DEFAULT NULL,
  `Negotiable_Other_Amount` decimal(9,4) DEFAULT NULL,
  `Special_Condition_1` varchar(155) DEFAULT NULL,
  `Special_Condition_2` varchar(155) DEFAULT NULL,
  `Special_Condition_3` varchar(155) DEFAULT NULL,
  `Negotiable_Date_A` date DEFAULT NULL,
  `Negotiable_Date_b` date DEFAULT NULL,
  `Weekly_Timecard_Id` int(11) DEFAULT NULL,
  `Function_Name` decimal(9,4) DEFAULT NULL,
  `Function_Rate` decimal(9,4) DEFAULT NULL,
  `Additional_Amount` decimal(9,4) DEFAULT NULL,
  `OT_Hour` decimal(9,4) DEFAULT NULL,
  `OT_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `OT_Hour_Total` decimal(9,4) DEFAULT NULL,
  `Add_OT_Hour` decimal(9,4) DEFAULT NULL,
  `Add_OT_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `Add_OT_Hour_Total` decimal(9,4) DEFAULT NULL,
  `Night_Hour` decimal(9,4) DEFAULT NULL,
  `Night_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `Night_Hour_Total` decimal(9,4) DEFAULT NULL,
  `OT_Night_Hour` decimal(9,4) DEFAULT NULL,
  `OT_Night_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `OT_Night_Hour_Total` decimal(9,4) DEFAULT NULL,
  `Holiday_Hour` decimal(9,4) DEFAULT NULL,
  `Holiday_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `Holiday_Hour_Total` decimal(9,4) DEFAULT NULL,
  `Travel_Hour` decimal(9,4) DEFAULT NULL,
  `Travel_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `Travel_Hour_Total` decimal(9,4) DEFAULT NULL,
  `Waiting_Hour` decimal(9,4) DEFAULT NULL,
  `Waiting_Hour_Amount` decimal(9,4) DEFAULT NULL,
  `Waiting_Hour_Total` decimal(9,4) DEFAULT NULL,
  `StandBy_Day` decimal(9,4) DEFAULT NULL,
  `StandBy_Day_Amount` decimal(9,4) DEFAULT NULL,
  `StandBy_Day_Total` decimal(9,4) DEFAULT NULL,
  `Lodging_Meal` tinyint(1) DEFAULT '0',
  `Lodging_Meals_Number` decimal(9,4) DEFAULT NULL,
  `Lodging_Meals_Amount` decimal(9,4) DEFAULT NULL,
  `Lodging_Meals_Total` decimal(9,4) DEFAULT NULL,
  `Meals_Only` tinyint(1) DEFAULT '0',
  `Meal_Only_Number` decimal(9,4) DEFAULT NULL,
  `Meal_Only_Amount` decimal(9,4) DEFAULT NULL,
  `Meal_Only_Total` decimal(9,4) DEFAULT NULL,
  `Fitting_Rehearsal` decimal(9,4) DEFAULT NULL,
  `Fitting_Rehearsal_Amount` decimal(9,4) DEFAULT NULL,
  `Fitting_Rehearsal_Total` decimal(9,4) DEFAULT NULL,
  `Mileage_Rate` decimal(9,4) DEFAULT NULL,
  `Mileage_Rate_Amount` decimal(9,4) DEFAULT NULL,
  `Mileage_Rate_Total` decimal(9,4) DEFAULT NULL,
  `Call_Back_On_Camera` decimal(9,4) DEFAULT NULL,
  `Call_Back_On_Camera_Total` decimal(9,4) DEFAULT NULL,
  `Call_Back_Off_Camera` decimal(9,4) DEFAULT NULL,
  `Call_Back_OFF_Camera_Total` decimal(9,4) DEFAULT NULL,
  `Tag_Number` decimal(9,4) DEFAULT NULL,
  `Tag_Value` decimal(9,4) DEFAULT NULL,
  `Tag_Total` decimal(9,4) DEFAULT NULL,
  `Others` decimal(9,4) DEFAULT NULL,
  `Others_Total` decimal(9,4) DEFAULT NULL,
  `Gross_Fee` decimal(9,4) DEFAULT NULL,
  `Apprentice` decimal(9,4) DEFAULT NULL,
  `Permit_Holder_Number` decimal(9,4) DEFAULT NULL,
  `Permit_Number` decimal(9,4) DEFAULT NULL,
  `Permit_Amount` decimal(9,4) DEFAULT NULL,
  `Permit_Total` decimal(9,4) DEFAULT NULL,
  `Minority_Aged_Artists_Fund` decimal(9,4) DEFAULT NULL,
  `Fee_Before_Taxes` decimal(9,4) DEFAULT NULL,
  `Gst` decimal(9,4) DEFAULT NULL,
  `Qst` decimal(9,4) DEFAULT NULL,
  `Net_Fee` decimal(9,4) DEFAULT NULL,
  `Producer_Share` decimal(9,4) DEFAULT NULL,
  `Total_Amount_To_CSA` decimal(9,4) DEFAULT NULL,
  PRIMARY KEY (`Id`),
  KEY `Producer_Address_Id` (`Producer_Address_Id`),
  KEY `Artist_Address_Id` (`Artist_Address_Id`),
  KEY `Recording_id_1` (`Recording_id_1`),
  KEY `Recording_id_2` (`Recording_id_2`),
  KEY `Recording_id_3` (`Recording_id_3`),
  KEY `Weekly_Timecard_Id` (`Weekly_Timecard_Id`),
  CONSTRAINT `form_uda_contract_idfk_1` FOREIGN KEY (`Producer_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `form_uda_contract_idfk_2` FOREIGN KEY (`Artist_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `form_uda_contract_idfk_3` FOREIGN KEY (`Recording_id_1`) REFERENCES `recording` (`id`),
  CONSTRAINT `form_uda_contract_idfk_4` FOREIGN KEY (`Recording_id_2`) REFERENCES `recording` (`id`),
  CONSTRAINT `form_uda_contract_idfk_5` FOREIGN KEY (`Recording_id_3`) REFERENCES `recording` (`id`),
  CONSTRAINT `form_uda_contract_idfk_6` FOREIGN KEY (`Weekly_Timecard_Id`) REFERENCES `weekly_time_card` (`Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `recording` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `Recording_Date` date DEFAULT NULL,
  `Recording_Time` varchar(150) DEFAULT NULL,
  `Recording_Location` varchar(150) DEFAULT NULL,
  PRIMARY KEY (`id`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

/* */
-- --------------------- End of release 4.14.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.15.0 changes:
/* *

--LS-2502 Gender Field changed to enum (ACTRA Work Permit and User)
alter table form_actra_work_permit
	change Talent_Gender Talent_Gender_Bit bit,
	add column Talent_Gender char(1) DEFAULT NULL after Guardian_Name,
	add column Talent_Gender_Other varchar(50) DEFAULT NULL after talent_gender;
update form_actra_work_permit set talent_gender = 'M' where Talent_Gender_Bit is not null and Talent_Gender_Bit = true;
update form_actra_work_permit set talent_gender = 'F' where Talent_Gender_Bit is not null and Talent_Gender_Bit = false;
-- alter table form_actra_work_permit drop Talent_Gender_Bit; -- run after above updates are confirmed successful.
update user set gender = null where gender = '';
update start_form set gender = null where gender = '';

--LS-2386 Change in UDA INM column
ALTER TABLE form_uda_contract MODIFY COLUMN Function_Name varchar(255);

ALTER TABLE form_uda_contract DROP Period;
ALTER TABLE form_uda_contract ADD Period_3month  tinyint(1) DEFAULT '0' after Use_Table_F;
ALTER TABLE form_uda_contract ADD Period_6month  tinyint(1) DEFAULT '0' after Period_3month;
ALTER TABLE form_uda_contract ADD Period_9month  tinyint(1) DEFAULT '0' after Period_6month;

ALTER TABLE form_uda_contract ADD `Signature_Date` date DEFAULT NULL after Weekly_Timecard_Id;

-- 7/19/19: update mask bits to add Cast-UDA department to Canadian productions
set @maskC = 1 << (61-1); -- generate the mask value for Cast - UDA
-- select @maskC, hex(@maskC); -- for testing

-- update Canadian Production masks:
-- inner 'select's have extra columns so they can be run separately for testing/review
update production p set dept_mask = (dept_mask | @maskC) where p.id in 
	(select id from (select p.id, dept_mask, hex(dept_mask), title from production p 
		where type = 'CANADA_TALENT' and p.status = 'ACTIVE') as temp_tbl);

-- update Canadian Project masks:
update project j set dept_mask = (dept_mask | @maskC) where j.id in 
	(select id from (select j.id, j.dept_mask, hex(j.dept_mask), j.title pjtitle, p.title from production p, project j where p.type = 'CANADA_TALENT' and p.status = 'ACTIVE' 
		and j.production_id = p.id) as temp_tbl);

-- LS-1831 add extended rate to pay breakdown
alter table pay_breakdown       add column Ext_Rate decimal(12,6) default null after rate;
alter table pay_breakdown_daily add column Ext_Rate decimal(12,6) default null after rate;
-- calculate extended rate values for existing records
update pay_breakdown set Ext_Rate = rate where rate is not null and multiplier is not null;
update pay_breakdown set Ext_Rate = rate * multiplier where rate is not null and multiplier is not null
	and category <> 'Meet Guarantee' and category <> 'Flat Rate';
update pay_breakdown_daily set Ext_Rate = rate where rate is not null and multiplier is not null;
update pay_breakdown_daily set Ext_Rate = rate * multiplier where rate is not null and multiplier is not null
	and category <> 'Meet Guarantee' and category <> 'Flat Rate';

-- Add additional fields to Tours Timesheet for 16-Days - LS-2440
alter table timesheet add column Start_Date DATE after Prod_Id,
	add column Num_Days INT(11),
	add column Pay_Period_Type VARCHAR(25) DEFAULT 'W';

update timesheet set start_date = date_add(end_date, INTERVAL -6 DAY) where start_date is null and end_date is not null;
update timesheet set Num_Days = 7 where Num_Days is null;

-- LS-2440 Add PayPeriodType field
ALTER TABLE payroll_preference
	ADD COLUMN Pay_Period_Type varchar(10) DEFAULT 'W' after include_touring;
	
--LS-2357 Add Column in UDA INM column
 ALTER TABLE form_uda_contract ADD COLUMN `Owner_Id` int(11) DEFAULT NULL;
 ALTER TABLE form_uda_contract ADD CONSTRAINT `form_uda_contract_idfk_7` FOREIGN KEY (`Owner_Id`) REFERENCES `office` (`id`)

-- Increase the size of the Producer Phone # and drop advertiser and product name columns. LS-2583
alter table form_uda_contract modify column Producer_Phone varchar(25),
	modify column Artist_Phone varchar(25);
alter table form_uda_contract drop column Advertiser_Name,
	drop Product_Name;

/* */
-- --------------------- End of release 4.15.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.16.0 changes:
/* *
-- Added new fields to StartForm for Loan-Out section. LS-2562
alter table start_form 
	add column Tax_Classification char(1) default NULL after State_Tax_ID,
	add column Paid_As char(2) default NULL after Company_Name,
	add column LLC_Type char(1) default NULL after Tax_Classification;

-- Added new fields to User for Loan-Out section. LS-2562
alter table user 
	add column Tax_Classification char(1) default NULL after State_Tax_ID,
	add column LLC_Type char(1) default NULL after Tax_Classification;

-- LS-2568 Use AES for SSN encryption.
ALTER TABLE user
	ADD COLUMN social_security_aes longblob DEFAULT null after social_security;

 -- LS-2435 TimeSheet UI - Calculation of total wagegs for monthly timesheet
alter table weekly_time_card add COLUMN Total_Wages1 decimal(10,2) after Adj_Gtotal,
		add COLUMN Total_Wages2 decimal(10,2) after Total_Wages1;

-- LS-2579 & LS-2580 - Change in ROTH category name
update pay_breakdown set category = 'ROTH' where category = 'ROTH4K';
update pay_breakdown_daily set category = 'ROTH' where category = 'ROTH4K';
update pay_expense set category = 'ROTH' where category = 'ROTH4K';
update timesheet set pay_category1 = 'ROTH' where pay_category1 = 'ROTH4K';
update timesheet set pay_category2 = 'ROTH' where pay_category2 = 'ROTH4K';
update timesheet set pay_category3 = 'ROTH' where pay_category3 = 'ROTH4K';
update timesheet set pay_category4 = 'ROTH' where pay_category4 = 'ROTH4K';

-- LS-2615
alter table uda_project_detail change  No_Uda_Member Prod_Number varchar(150);
alter table uda_project_detail add column Producer_Uda varchar(155) default null after Prod_Number;

--LS-2357 Alter field
ALTER TABLE form_uda_contract CHANGE COLUMN Owner_Id Office_Id int(11) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Advertiser_Name_1 varchar(155) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Advertiser_Name_2 varchar(155) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Advertiser_Name_3 varchar(155) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Product_Name_1 varchar(155) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Product_Name_2 varchar(155) DEFAULT NULL;
ALTER TABLE form_uda_contract MODIFY COLUMN Product_Name_3 varchar(155) DEFAULT NULL;

ALTER TABLE form_uda_contract CHANGE COLUMN Owner_Id Office_Id int(11) DEFAULT NULL;

-- Add UDA office for UDA Contract transfer LS-2537
insert into office values(10,'Montreal', 'MTL', 'amiric@theteamcompanies.com', 'UDA', 'CA', 1);

-- ESS-466. This is to add more UDA HTG CALC related columns into daily_time 

ALTER TABLE daily_time 
	ADD COLUMN holiday_hours decimal(4,2),
	ADD COLUMN night_hours decimal(4,2);

-- ESS-466. This is to add more UDA HTG CALC related columns into pay_expense 

ALTER TABLE pay_expense 
	ADD COLUMN start_time decimal(4,2),
	ADD COLUMN end_time decimal(4,2),
	ADD COLUMN holiday_hours decimal(4,2),
	ADD COLUMN night_hours decimal(4,2),
	ADD COLUMN work_date date,
	ADD COLUMN unit varchar(10);

-- ESS-466: add dummy records to pay_expense to support jrxml changes:

insert into pay_expense (weekly_id, line_number) values (-1, 101);
insert into pay_expense (weekly_id, line_number) values (-1, 102);
insert into pay_expense (weekly_id, line_number) values (-1, 103);
insert into pay_expense (weekly_id, line_number) values (-1, 104);
insert into pay_expense (weekly_id, line_number) values (-1, 105);
insert into pay_expense (weekly_id, line_number) values (-1, 106);
insert into pay_expense (weekly_id, line_number) values (-1, 107);
insert into pay_expense (weekly_id, line_number) values (-1, 108);
insert into pay_expense (weekly_id, line_number) values (-1, 109);
insert into pay_expense (weekly_id, line_number) values (-1, 110);

/* */
-- --------------------- End of release 4.16.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.17.0 changes:
/* *

--LS-2785 Alter column name in Recording in UDA-INM
UPDATE recording set recording_time = null where recording_time = '';
ALTER TABLE recording MODIFY COLUMN Recording_Time decimal(9,4) DEFAULT NULL;

-- LS-2610 Add Column in form_deposit
alter table form_deposit 
	add column Loan_Out_Name varchar(100) DEFAULT NULL after Employee_Phone,
	add column FEIN varchar(1000) DEFAULT NULL after Employee_Phone;

--LS-2795 change column type Tag_Number 
ALTER TABLE form_uda_contract MODIFY COLUMN Tag_Number int(11) DEFAULT NULL;
--LS-2796 change column type Others
ALTER TABLE form_uda_contract MODIFY COLUMN Others varchar(155) DEFAULT NULL;
-- LS-2562 Add Paid As type to weekly timecard for testing if all fields are filled in for submit. 
alter table weekly_time_card add column Paid_As char(2) default NULL after Work_Country;

-- LS-2928 add effective date range to contract rules
Alter table contract_rule add column First_Effective_Date date default null after priority,
	add column Last_Effective_Date date default null after First_Effective_Date;

/* */
-- --------------------- End of release 4.17.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.18.0 changes:
/* *
-- LS-2935 New overnight rate for Hybrids
Alter table start_form add column Overnight decimal(9,4) default null after prep_rate_id;

-- LS-2946 New fields for W4 form for 2020.
alter table form_w4 add column Multiple_Jobs tinyint(1) DEFAULT 0,
		add column Child_Dependency_Amt int(11) DEFAULT NULL,
		add column Other_Dependency_Amt int(11) DEFAULT NULL,
		add column Total_Dependency_Amt int(11) DEFAULT NULL,
		add column Other_Income_Amt int(11) DEFAULT NULL,
		add column Deductions_Amt int(11) DEFAULT NULL,
		add column Extra_Withholding_Amt int(11) DEFAULT NULL,
		add column Num_Pay_Periods int(11) DEFAULT NULL,
		add column Two_Jobs_Taxable_Wages int(11) DEFAULT NULL,
		add column Three_Jobs_High_Taxable_Wages int(11) DEFAULT NULL,
		add column Three_Jobs_High_Low_Taxable_Wages int(11) DEFAULT NULL,
		add column Three_Jobs_Total_Taxable_Wages int(11) DEFAULT NULL,
		add column Est_Itemized_Deductions int(11) DEFAULT NULL,
		add column Filing_Status_Amt int(11) DEFAULT NULL,
		add column Deductions_Sub_Total int(11) DEFAULT NULL,
		add column Other_Deductible_Amts int(11) DEFAULT NULL,
		add column Deductions_Total int(11) DEFAULT NULL;
		
-- LS-3003 Wire up Worksheet to FormW4 model class
alter table form_w4 add column Est_Qualified_Deductions int(11) DEFAULT NULL after Est_Itemized_Deductions;

/* */
-- --------------------- End of release 4.18.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.19.0 changes:
/* *

-- Define new form_model_release_print table
Drop table if exists form_model_release_print;
CREATE TABLE form_model_release_print (
 Id int(11) NOT NULL AUTO_INCREMENT,
 version tinyint(4) NOT NULL DEFAULT '19',
 Model_Name varchar(30) DEFAULT NULL,
 Model_SSN varchar(1000) DEFAULT NULL,
 Model_Agency_Yes tinyint(1) DEFAULT '0',
 Model_Agency_Text varchar(125) DEFAULT NULL,
 Model_Agency_No tinyint(1) DEFAULT '0',
 Model_Corporation_Yes tinyint(1) DEFAULT '0',
 Model_Corporation_Text varchar(30) DEFAULT NULL,
 Model_Corporation_No tinyint(1) DEFAULT '0',
 Corporation_Federal_Yes tinyint(1) DEFAULT '0',
 Corporation_Federal_Id varchar(1000) DEFAULT NULL,
 Corporation_Federal_No tinyint(1) DEFAULT '0',
 Project_Name varchar(20) DEFAULT NULL,
 Job varchar(15) DEFAULT NULL,
 JobPO varchar(60) DEFAULT NULL,
 Shoot_Date varchar(60) DEFAULT NULL,
 Product varchar(8) DEFAULT NULL,
 Advertiser varchar(30) DEFAULT NULL,
 Ad_Agency varchar(30) DEFAULT NULL,
 Producer_Name varchar(80) DEFAULT NULL,
 Photographer varchar(16) DEFAULT NULL,
 Job_Location varchar(16) DEFAULT NULL,
 Usage_Yes tinyint(1) DEFAULT '0',
 Usage_No tinyint(1) DEFAULT '0',
 Unlimited tinyint(1) DEFAULT '0',
 Unlimited_Except_Outdoor tinyint(1) DEFAULT '0',
 Electric_Media tinyint(1) DEFAULT '0',
 B_Roll tinyint(1) DEFAULT '0',
 Direct_Mail tinyint(1) DEFAULT '0',
 Collateral tinyint(1) DEFAULT '0',
 ISM tinyint(1) DEFAULT '0',
 PR tinyint(1) DEFAULT '0',
 Print tinyint(1) DEFAULT '0',
 Circular tinyint(1) DEFAULT '0',
 Industrial tinyint(1) DEFAULT '0',
 Media_Other tinyint(1) DEFAULT '0',
 Media_Other_Text varchar(125) DEFAULT NULL,
 Term tinyint(1) DEFAULT '0',
 Term_Month varchar(125) DEFAULT NULL,
 Term_Date date DEFAULT NULL,
 Term_Year date DEFAULT NULL,
 Unlimited_Time tinyint(1) DEFAULT '0',
 Term_Other tinyint(1) DEFAULT '0',
 North_America tinyint(1) DEFAULT '0',
 European_Union tinyint(1) DEFAULT '0',
 South_America tinyint(1) DEFAULT '0',
 Asia tinyint(1) DEFAULT '0',
 World_Wide tinyint(1) DEFAULT '0',
 World_Wide_Electronic_Media tinyint(1) DEFAULT '0',
 Territory_Other tinyint(1) DEFAULT '0',
 Territory_Other_Text varchar(125) DEFAULT NULL,
 Optional_Reuse_Yes tinyint(1) DEFAULT '0',
 Optional_Reuse_No tinyint(1) DEFAULT '0',
 Reuse_Media varchar(125) DEFAULT NULL,
 Reuse_Term varchar(125) DEFAULT NULL,
 Rate_Use_Territory varchar(125) DEFAULT NULL,
 Rate_Use_Fee varchar(125) DEFAULT NULL,
 Compensation_Yes tinyint(1) DEFAULT '0',
 Compensation_No tinyint(1) DEFAULT '0',
 Rate_For_Service varchar(125) DEFAULT NULL,
 Per_Day tinyint(1) DEFAULT '0',
 Service_Hour varchar(125) DEFAULT NULL,
 Right_Term tinyint(1) DEFAULT '0',
 Not_Right_Term tinyint(1) DEFAULT '0',
 Usage_30days varchar(125) DEFAULT NULL,
 Addl_Yes tinyint(1) DEFAULT '0',
 Addl_No tinyint(1) DEFAULT '0',
 Addl_Over_Time tinyint(1) DEFAULT '0',
 Addl_Per_Hour varchar(125) DEFAULT NULL,
 Addl_Hour varchar(125) DEFAULT NULL,
 Shoot_Day tinyint(1) DEFAULT '0',
 Shoot_Per_Day varchar(125) DEFAULT NULL,
 Shoot_Per_Hour varchar(125) DEFAULT NULL,
 Prep_Day tinyint(1) DEFAULT '0',
 Prep_Per_Day varchar(125) DEFAULT NULL,
 Prep_Per_Hour varchar(125) DEFAULT NULL,
 Weather_Day tinyint(1) DEFAULT '0',
 Weather_Per_Day varchar(125) DEFAULT NULL,
 Weather_Hour varchar(125) DEFAULT NULL,
 Intimates_Day tinyint(1) DEFAULT '0',
 Intimates_Per_Day varchar(155) DEFAULT NULL,
 Intimates_Per_Hour varchar(155) DEFAULT NULL,
 Travel_Day_Rate tinyint(1) DEFAULT '0',
 Travel_Day_Per_Day varchar(125) DEFAULT NULL,
 Travel_Day_Per_Hour varchar(125) DEFAULT NULL,
 Non_Taxable tinyint(1) DEFAULT '0',
 Taxable tinyint(1) DEFAULT '0',
 Additional_Provision varchar(125) DEFAULT NULL,
 Project varchar(125) DEFAULT NULL,
 Job_Model varchar(125) DEFAULT NULL,
 Age varchar(3) DEFAULT NULL,
 Guidance varchar(125) DEFAULT NULL,
 Model tinyint(1) DEFAULT '0',
 Model_Agency tinyint(1) DEFAULT '0',
 Producer tinyint(1) DEFAULT '0',
 Guardian tinyint(1) DEFAULT '0',
 Model_Date date DEFAULT NULL,
 Producer_Date date DEFAULT NULL,
 Model_Email varchar(55) DEFAULT NULL,
 Producer_Email varchar(55) DEFAULT NULL,
 Model_Phone varchar(25) DEFAULT NULL,
 Producer_Phone varchar(25) DEFAULT NULL,
 Company varchar(125) DEFAULT NULL,
 Model_Agency_Name varchar(4) DEFAULT NULL,
 Model_Agency_Street varchar(125) DEFAULT NULL,
 Model_Agency_Phone varchar(25) DEFAULT NULL,
 Model_Agency_Company varchar(125) DEFAULT NULL,
 Model_Print_Name varchar(125) DEFAULT NULL,
 Producer_Print_Name varchar(125) DEFAULT NULL,
 Producer_Address_Id int(11) DEFAULT NULL,
 Model_Address_Id int(11) DEFAULT NULL,
 Prep_Hour tinyint(1) DEFAULT '0',
 Per_Hour tinyint(1) DEFAULT '0',
 Under_Age varchar(255) DEFAULT NULL,
 TTC_Client varchar(60) DEFAULT NULL,
 PRIMARY KEY (Id),
 KEY form_model_release_ibfk_2 (Produer_Address_Id),
 KEY form_model_release_ibfk_3 (Model_Address_Id),
 CONSTRAINT form_model_release_ibfk_2 FOREIGN KEY (Produer_Address_Id) REFERENCES address (Id),
 CONSTRAINT form_model_release_ibfk_3 FOREIGN KEY (Model_Address_Id) REFERENCES address (Id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- LS-3052 Branch Code Doesn't Save on Intent to Produce
alter table form_actra_intent add column office_id int(11) DEFAULT NULL;

-- LS-3048 adjust field lengths of phone numbers; fix spelling of field.
-- Alter TABLE form_model_release_print 
-- 	modify column Model_Phone varchar(25) DEFAULT NULL,
-- 	modify column Producer_Phone varchar(25) DEFAULT NULL,
-- 	modify column Model_Agency_Phone varchar(25) DEFAULT NULL,
-- 	change column Produer_Address_Id Producer_Address_Id int(11) DEFAULT NULL;

Alter TABLE form_model_release_print Drop FOREIGN KEY form_model_release_ibfk_2;
Alter table form_model_release_print Add CONSTRAINT form_model_release_ibfk_2 FOREIGN KEY (Producer_Address_Id) REFERENCES address (Id);

-- LS-3043 Update Import Process to include Individual/Corporate fields
alter TABLE contact_import 
    add column Pay_Indicator varchar(100) DEFAULT NULL,
    add column Loan_Out_Name varchar(100) DEFAULT NULL,
    add column FEIN varchar(1000) DEFAULT NULL,
    add column Tax_Classification varchar(30) DEFAULT NULL;
    
    

/* */
-- --------------------- End of release 4.19.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 4.20.0 changes:
/* *
--LS-3080 Changes in model release fields
CREATE TABLE `form_model_release_print` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '19',
  `Model_Name` varchar(30) DEFAULT NULL,
  `Model_SSN` varchar(1000) DEFAULT NULL,
  `Model_Agency_Yes` tinyint(1) DEFAULT '0',
  `Model_Agency_Text` varchar(4) DEFAULT NULL,
  `Model_Agency_No` tinyint(1) DEFAULT '0',
  `Model_Corporation_Yes` tinyint(1) DEFAULT '0',
  `Model_Corporation_Text` varchar(30) DEFAULT NULL,
  `Model_Corporation_No` tinyint(1) DEFAULT '0',
  `Corporation_Federal_Yes` tinyint(1) DEFAULT '0',
  `Corporation_Federal_Id` varchar(1000) DEFAULT NULL,
  `Corporation_Federal_No` tinyint(1) DEFAULT '0',
  `Project_Name` varchar(20) DEFAULT NULL,
  `Job` varchar(15) DEFAULT NULL,
  `JobPO` varchar(60) DEFAULT NULL,
  `Shoot_Date` varchar(125) DEFAULT NULL,
  `Product` varchar(8) DEFAULT NULL,
  `Advertiser` varchar(30) DEFAULT NULL,
  `Ad_Agency` varchar(30) DEFAULT NULL,
  `Producer_Name` varchar(8) DEFAULT NULL,
  `Photographer` varchar(16) DEFAULT NULL,
  `Job_Location` varchar(24) DEFAULT NULL,
  `Usage_Yes` tinyint(1) DEFAULT '0',
  `Usage_No` tinyint(1) DEFAULT '0',
  `Unlimited` tinyint(1) DEFAULT '0',
  `Unlimited_Except_Outdoor` tinyint(1) DEFAULT '0',
  `Electric_Media` tinyint(1) DEFAULT '0',
  `B_Roll` tinyint(1) DEFAULT '0',
  `Direct_Mail` tinyint(1) DEFAULT '0',
  `Collateral` tinyint(1) DEFAULT '0',
  `ISM` tinyint(1) DEFAULT '0',
  `PR` tinyint(1) DEFAULT '0',
  `Print` tinyint(1) DEFAULT '0',
  `Circular` tinyint(1) DEFAULT '0',
  `Industrial` tinyint(1) DEFAULT '0',
  `Media_Other` tinyint(1) DEFAULT '0',
  `Media_Other_Text` varchar(20) DEFAULT NULL,
  `Term` tinyint(1) DEFAULT '0',
  `Term_Month` varchar(16) DEFAULT NULL,
  `Term_Date` date DEFAULT NULL,
  `Term_Year` date DEFAULT NULL,
  `Unlimited_Time` tinyint(1) DEFAULT '0',
  `Term_Other` tinyint(1) DEFAULT '0',
  `North_America` tinyint(1) DEFAULT '0',
  `European_Union` tinyint(1) DEFAULT '0',
  `South_America` tinyint(1) DEFAULT '0',
  `Asia` tinyint(1) DEFAULT '0',
  `World_Wide` tinyint(1) DEFAULT '0',
  `World_Wide_Electronic_Media` tinyint(1) DEFAULT '0',
  `Territory_Other` tinyint(1) DEFAULT '0',
  `Territory_Other_Text` varchar(16) DEFAULT NULL,
  `Optional_Reuse_Yes` tinyint(1) DEFAULT '0',
  `Optional_Reuse_No` tinyint(1) DEFAULT '0',
  `Reuse_Media` varchar(20) DEFAULT NULL,
  `Reuse_Term` varchar(16) DEFAULT NULL,
  `Rate_Use_Territory` varchar(16) DEFAULT NULL,
  `Rate_Use_Fee` decimal(11,3) DEFAULT NULL,
  `Compensation_Yes` tinyint(1) DEFAULT '0',
  `Compensation_No` tinyint(1) DEFAULT '0',
  `Rate_For_Service` decimal(13,4) DEFAULT NULL,
  `Per_Day` tinyint(1) DEFAULT '0',
  `Service_Hour` decimal(4,2) DEFAULT NULL,
  `Right_Term` tinyint(1) DEFAULT '0',
  `Not_Right_Term` tinyint(1) DEFAULT '0',
  `Usage_30days` decimal(11,3) DEFAULT NULL,
  `Addl_Yes` tinyint(1) DEFAULT '0',
  `Addl_No` tinyint(1) DEFAULT '0',
  `Addl_Over_Time` tinyint(1) DEFAULT '0',
  `Addl_Per_Hour` decimal(13,5) DEFAULT NULL,
  `Addl_Hour` int(2) DEFAULT NULL,
  `Shoot_Day` tinyint(1) DEFAULT '0',
  `Shoot_Per_Day` decimal(13,5) DEFAULT NULL,
  `Shoot_Per_Hour` int(2) DEFAULT NULL,
  `Prep_Day` tinyint(1) DEFAULT '0',
  `Prep_Per_Day` decimal(13,5) DEFAULT NULL,
  `Prep_Per_Hour` int(2) DEFAULT NULL,
  `Weather_Day` tinyint(1) DEFAULT '0',
  `Weather_Per_Day` decimal(13,5) DEFAULT NULL,
  `Weather_Hour` int(2) DEFAULT NULL,
  `Intimates_Day` tinyint(1) DEFAULT '0',
  `Intimates_Per_Day` decimal(13,5) DEFAULT NULL,
  `Intimates_Per_Hour` int(2) DEFAULT NULL,
  `Travel_Day_Rate` tinyint(1) DEFAULT '0',
  `Travel_Day_Per_Day` decimal(13,5) DEFAULT NULL,
  `Travel_Day_Per_Hour` int(2) DEFAULT NULL,
  `Non_Taxable` tinyint(1) DEFAULT '0',
  `Taxable` tinyint(1) DEFAULT '0',
  `Additional_Provision` varchar(300) DEFAULT NULL,
  `Project` varchar(30) DEFAULT NULL,
  `Job_Model` varchar(125) DEFAULT NULL,
  `Age` varchar(3) DEFAULT NULL,
  `Guidance` varchar(125) DEFAULT NULL,
  `Model` tinyint(1) DEFAULT '0',
  `Model_Agency` tinyint(1) DEFAULT '0',
  `Producer` tinyint(1) DEFAULT '0',
  `Guardian` tinyint(1) DEFAULT '0',
  `Model_Date` date DEFAULT NULL,
  `Producer_Date` date DEFAULT NULL,
  `Model_Email` varchar(60) DEFAULT NULL,
  `Producer_Email` varchar(60) DEFAULT NULL,
  `Model_Phone` varchar(13) DEFAULT NULL,
  `Producer_Phone` varchar(13) DEFAULT NULL,
  `Company` varchar(30) DEFAULT NULL,
  `Model_Agency_Name` varchar(30) DEFAULT NULL,
  `Model_Agency_Street` varchar(60) DEFAULT NULL,
  `Model_Agency_Phone` varchar(13) DEFAULT NULL,
  `Model_Agency_Company` varchar(30) DEFAULT NULL,
  `Model_Print_Name` varchar(30) DEFAULT NULL,
  `Producer_Print_Name` varchar(30) DEFAULT NULL,
  `Producer_Address_Id` int(11) DEFAULT NULL,
  `Model_Address_Id` int(11) DEFAULT NULL,
  `Prep_Hour` tinyint(1) DEFAULT '0',
  `Pre_Hour` tinyint(1) DEFAULT '0',
  `Per_Hour` tinyint(1) DEFAULT '0',
  `Under_Age` varchar(255) DEFAULT NULL,
  `TTC_Client` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`Id`),
  KEY `form_model_release_ibfk_2` (`Producer_Address_Id`),
  KEY `form_model_release_ibfk_3` (`Model_Address_Id`),
  CONSTRAINT `form_model_release_ibfk_2` FOREIGN KEY (`Producer_Address_Id`) REFERENCES `address` (`Id`),
  CONSTRAINT `form_model_release_ibfk_3` FOREIGN KEY (`Model_Address_Id`) REFERENCES `address` (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8;
    -- LS-3064 adjust field lengths of phone numbers; fix spelling of field.
Alter TABLE form_model_release_print 
	modify column Model_Phone varchar(13) DEFAULT NULL,
	modify column Producer_Phone varchar(13) DEFAULT NULL,
	modify column Model_Agency_Phone varchar(13) DEFAULT NULL;
	
--LS-3080 Change Model Agency Text
ALTER TABLE form_model_release_print MODIFY COLUMN Model_Agency_Text varchar(30) DEFAULT NULL;
ALTER TABLE form_model_release_print MODIFY COLUMN Producer_Name varchar(30) DEFAULT NULL;

-- LS-3093 Rows on Intent to Produce (Location Shoot, Weather, Weekend/Night Production) Do NOT have Editable text fields
alter table form_actra_intent 
add column Location_Shoot_Details varchar(100),
add column Weather_Permitting_Details varchar(100),
add column Weekend_Night_Details varchar(100),
add column Nude_Scenes_Details varchar(100);

--LS-3135 Add new field for model release.
alter TABLE form_model_release_print 
add column Term_Other_Text varchar(16) DEFAULT NULL;

--LS-3134 Change colimn size Job
ALTER TABLE form_model_release_print MODIFY COLUMN  Job varchar(60) DEFAULT NULL;

--LS-3134 Change colimn size Job
ALTER TABLE form_model_release_print MODIFY COLUMN Term_Month int(2) DEFAULT NULL;


/* */
-- --------------------- End of release 4.20.0 changes ------------------------------------------
/* */


/* */
-- --------------------- START of release 4.21.0 changes:
/* *

-- LS-3126 Add column for per denim
alter TABLE form_model_release_print 
    add column Per_Deim_Amount decimal(13,5) DEFAULT NULL,
    add column Per_Diem_Days int(11) DEFAULT NULL,
    add column Lodging tinyint(1) DEFAULT '0',
    add column Meals_Incidentals tinyint(1) DEFAULT '0';

-- LS-3183 Add Taxable_Total and Non_Taxable_Total columns
alter TABLE form_model_release_print 
    add column Taxable_Total decimal(11,3) DEFAULT NULL,
    add column Non_Taxable_Total decimal(11,3) DEFAULT NULL;

-- ESS-739 contract rate table change- see db subtask ESS-767

-- Release Notes related

CREATE TABLE dba1.app_release (
id INT AUTO_INCREMENT,
app_name VARCHAR(20) NOT NULL,
title VARCHAR(100) NOT NULL,
version VARCHAR(60) NOT NULL,
release_date TIMESTAMP NOT NULL,
PRIMARY KEY (id)
);

CREATE TABLE dba1.app_release_detail (
id INT AUTO_INCREMENT,
release_id INT NOT NULL,
note LONGTEXT NOT NULL,
PRIMARY KEY (id),
FOREIGN KEY (release_id) REFERENCES app_release(id) ON DELETE CASCADE
);

/* */
-- --------------------- End of release 4.21.0 changes ------------------------------------------
/* */

/* */
-- --------------------- START of release 4.22.0 changes:
/* *
-- Remove unused producer_date and model_date from form_model_release_print
alter table form_model_release_print drop column Producer_Date;
alter table form_model_release_print drop column Model_Date;

-- LS-3283 Remove unused UDA fields
ALTER TABLE daily_time 
	DROP COLUMN holiday_hours,
	DROP COLUMN night_hours;

ALTER TABLE pay_expense
	DROP COLUMN start_time,
	DROP COLUMN end_time,
	DROP COLUMN holiday_hours,
	DROP COLUMN night_hours,
	DROP COLUMN work_date,
	DROP COLUMN unit;

--LS-2879 change Function_Name type
ALTER TABLE form_uda_contract DROP Function_Name;

-- LS-3118 DC tax for DC residents validations to be removed
-- updated file ls\data\Payroll\TaxWageRowTemplate.sql should be run on the database	

/* */
-- --------------------- End of release 4.22.0 changes ------------------------------------------
/* */
-- alter table holiday_rule change hourly_rate weekly_rate varchar(20);
/* */
-- --------------------- START of release 4.22.1 changes:
/* *
-- LS-3247
alter table form_w4 add column Multiple_Jobs_Total int(11) after Three_Jobs_Total_Taxable_Wages;
-- LS-3244
alter table form_w4 add column First_Date_Employment Date;
/* */
-- --------------------- End of release 4.22.1 changes ------------------------------------------
/* */

/* */
-- --------------------- START of release 4.23.0 changes:
/* *

--LS-2879 remove Function_Name field
ALTER TABLE form_uda_contract DROP Function_Name;

/* */
-- --------------------- End of release 4.23.0 changes ------------------------------------------
/* */

/* */
-- --------------------- START of release 20.2.0 changes:
/* *

-- LS-3420
alter table user add column Same_As_Home_Addr boolean default false after Home_Address_Id;
-- LS-3412
alter table user add column Alien_Auth_Country_Code varchar(50) after Citizen_Status;

-- LS-3477 missing field on Direct Deposit
ALTER TABLE form_deposit add column Bank_Amount2 decimal(8,2) null,
	add column Bank_Percent2 Boolean not null default false;

-- LS-3451, LS-3452, LS-3453, LS-3454 and LS-3292 - My account W4 information changes
alter table user add column Multiple_Jobs Boolean null default false,
add column Child_Dependency_Amt int(11) DEFAULT NULL,
add column Other_Dependency_Amt int(11) DEFAULT NULL,
add column Other_Income_Amt int(11) DEFAULT NULL,
add column Deductions_Amt int(11) DEFAULT NULL,
add column Extra_Withholding_Amt int(11) DEFAULT NULL;

/* */
-- --------------------- End of release 20.2.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.3.0 changes:
/* *
-- LS-3631
alter table user add column Loan_Out_Mailing_Address_Id INT(11) DEFAULT NULL after Loan_Out_Address_Id;
alter table user Add CONSTRAINT user_ibfk_7 FOREIGN KEY (Loan_Out_Mailing_Address_Id) REFERENCES address (Id);
-- LS-3632
alter table user add column Same_As_Corp_Addr boolean default false after Same_As_Home_Addr;

-- LS-3576 Add new multi-state W4 table
drop table  if exists form_state_w4;
CREATE TABLE `form_state_w4` (
	`id` int(11) NOT NULL AUTO_INCREMENT,
	`version` tinyint(4) NOT NULL DEFAULT '0',
	`form_type` varchar(20) NOT NULL DEFAULT '',
	`Full_Name` varchar(62) DEFAULT NULL,
	`First_Name` varchar(30) DEFAULT NULL,
	`First_and_Initial` varchar(32) DEFAULT NULL,
	`Middle_initial` varchar(1) DEFAULT NULL,
	`Last_Name` varchar(30) DEFAULT NULL,
	`Social_Security` varchar(1000) DEFAULT NULL,
	`Address_Id` int(11) DEFAULT NULL,
	`Marital` varchar(2) DEFAULT NULL,
	`Exempt` tinyint(1) DEFAULT NULL,
	`Allowances` int(11) DEFAULT NULL,
	`Additional_Amount` int(11) DEFAULT NULL,
	PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

-- LS-3635 Adding Loan Out Mailing Address to StartForm
alter table start_form add column Loan_Out_Mailing_Address_Id INT(11) DEFAULT NULL after Loan_Out_Address_Id;
alter table start_form add CONSTRAINT start_form_ibfk_10 FOREIGN KEY (Loan_Out_Mailing_Address_Id) REFERENCES address (Id);

-- LS-3579 added additional fields for NY-W4 form
alter table form_state_w4 
add column NYC_Resident tinyint(1) DEFAULT NULL,
add column Yonkers_Resident tinyint(1) DEFAULT NULL,
add column NYC_Allowances int(11) DEFAULT NULL,
add column NYS_Amount int(11) DEFAULT NULL,
add column NYC_Amount int(11) DEFAULT NULL,
add column Yonkers_Amount int(11) DEFAULT NULL,
add column NYS_EMP_Claim tinyint(1) DEFAULT 0,
add column Emp_NewHire tinyint(1) DEFAULT 0,
add column First_Date_Employment Date DEFAULT NULL,
add column Dependent_Health_Ins tinyint(1) DEFAULT 0,
add column Emp_Qualify_Date Date DEFAULT NULL,
add column Emp_Name_Address varchar(200) DEFAULT NULL,
add column Emp_Id_Number varchar(10) DEFAULT NULL;

-- LS-3520 change column type
ALTER TABLE form_model_release_print MODIFY COLUMN `Rate_Use_Fee` varchar(11) DEFAULT NULL;

-- LS-3595
/*
alter table form_state_w4  
add column Certified_For_Penalty tinyint(1) DEFAULT FALSE,
add column Emp_Tax_Account_Number int(11) DEFAULT NULL,
add column Claim_Exemption int(11) DEFAULT NULL;
*
-- Run this alter statement if the above alter of additional columns to form_state_w4 was run.
-- removed column Claim_Exemption as we are using 'Exempt'
-- alter table form_state_w4 drop Claim_Exemption;

alter table form_state_w4  
add column Certified_For_Penalty tinyint(1) DEFAULT FALSE,
add column Emp_Tax_Account_Number int(11) DEFAULT NULL;


/* */
-- --------------------- End of release 20.3.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.4.0 changes:
/* *

-- LS-3776 update "old" state form names
update document set name = 'AZ W4 (A-4)', type = 'AZ W4 (A-4)' where name = 'A4';
update document_chain set name = 'AZ W4 (A-4)' where name = 'A4';

update document set name = 'GA W4 (G-4)', type = 'GA W4 (G-4)' where name = 'G4';
update document_chain set name = 'GA W4 (G-4)' where name = 'G4';

update document set name = 'IL W4', type = 'IL W4' where name = 'IL-W4';
update document_chain set name = 'IL W4' where name = 'IL-W4';

update document set name = 'LA W4 (L-4)', type = 'LA W4 (L-4)' where name = 'L4';
update document_chain set name = 'LA W4 (L-4)' where name = 'L4';

-- Also update form types (enum values):
update contact_document cd set form_type = 'AZ_W4' where cd.Form_Type = 'a4';
update contact_document cd set form_type = 'GA_W4' where cd.Form_Type = 'g4';
update contact_document cd set form_type = 'IL_W4' where cd.Form_Type = 'ilw4';
update contact_document cd set form_type = 'LA_W4' where cd.Form_Type = 'l4';

-- Distribute new state W4 documents and document_chains:
call createAnyStandardDocument('MA W4', 'MA W4 (M-4)');
-- call createAnyStandardDocument('MD W4', 'MD W4 (MW507)');
call createAnyStandardDocument('MI W4', 'MI W4');
call createAnyStandardDocument('OR W4', 'OR W4');
-- if you previously updated your local db with MD form name as 'MW5074', run these:

-- update document set name = 'MD W4 (MW507)', type = 'MD W4 (MW507)' where name = 'MD W4 (MW5074)';
-- update document_chain set name = 'MD W4 (MW507)' where name = 'MD W4 (MW5074)';

-- LS-3538 Add MA State W-4
alter table form_state_w4
add column Claimed_Exemptions int(11) DEFAULT NULL,
add column Qualified_Dependents int(2) DEFAULT NULL,
add column Spouse_Exemptions int(2) DEFAULT NULL,
add column Personal_Exemptions int(2) DEFAULT NULL,
add column Fulltime_Student tinyint(1) DEFAULT FALSE,
add column Spouse_Blind tinyint(1) DEFAULT FALSE,
add column Blind tinyint(1) DEFAULT FALSE,
add column Head_Of_Household tinyint(1) DEFAULT FALSE;

-- LS-3539 Add column for MI-W4
alter table form_state_w4 add column Date_of_Birth date DEFAULT NULL, 
add column License_No varchar(20) DEFAULT NULL, 
add column Employer tinyint(1) DEFAULT FALSE,
add column Employer_No tinyint(1) DEFAULT FALSE,
add column Employer_Yes tinyint(1) DEFAULT FALSE,
add column Date_of_Hire date DEFAULT NULL,
add column Claiming int(20) DEFAULT NULL,
add column Deduction_Amount int(20) DEFAULT NULL,
add column 8a tinyint(1) DEFAULT FALSE,
add column 8b tinyint(1) DEFAULT FALSE,
add column Explaination varchar(50) DEFAULT NULL,
add column 8c tinyint(1) DEFAULT FALSE,
add column Renaissance_Zone varchar(30) DEFAULT NULL,
add column Federal_Tax_ID int(11) DEFAULT NULL;

-- LS-3640 Add column for NJ-W4
alter table form_state_w4
add column Single tinyint(1) DEFAULT FALSE,
add column Union_Couple_Joint tinyint(1) DEFAULT FALSE,
add column Union_Couple_Separate tinyint(1) DEFAULT FALSE,
add column Head_of_House tinyint(1) DEFAULT FALSE,
add column Widower tinyint(1) DEFAULT FALSE,
add column Instruction_Letter varchar(1) DEFAULT NULL;

-- LS-3540 Add OR State W-4
alter table form_state_w4
add column Re_Determination tinyint(1) DEFAULT FALSE,
add column Exempt_Code	varchar(2) DEFAULT NULL;

-- LS-3816 Add flag to decide whether to use xfdf or jasper report for printing start form
alter table start_form add column Use_Xfdf tinyint(1) default 0;

-- for ESS-1179
ALTER TABLE USER
	ADD COLUMN W2_Flag tinyint(1) default 0;

/* */
-- --------------------- End of release 20.4.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.5.0 changes:
/* *

-- Add document/document chains for completed state W4 forms:
call createStandardUSdocument('MD W4', 'MD W4 (MW507)');
call createStandardUSdocument('VA-W4', 'VA W4 (VA-4)'); -- LS-3646 VA-W4
call createStandardUSdocument('IN-W4', 'IN W4 (WH-4)'); -- LS-3645 IN W4
call createStandardUSdocument('OH-W4', 'OH W4 (IT 4)'); -- LS-3641 OH W4
call createStandardUSdocument('WI-W4', 'WI W4 (WT-4)'); -- LS-3643 WI W4
call createStandardUsdocument('NC-W4', 'NC W4 (NC-4)'); -- LS-3642 NC W4
call createStandardUSdocument('MO-W4', 'MO W4'); -- LS-3644 MO W4
call createStandardUSdocument('MN-W4', 'MN W4 (W-4MN)'); -- LS-3949 MN W-4
call createStandardUSdocument('KY-W4', 'KY W4 (K-4)'); -- KY W4 LS-3850
call createStandardUSdocument('AL W4', 'AL W4'); -- LS-3692 AL W4 
call createStandardUSdocument('NJ W4', 'NJ W4'); -- LS-xxxx NJ W4 


-- LS-3646 Add column for VA-W4

alter table form_state_w4
	add column Blindness_Exemption int(4) DEFAULT NULL;

-- New fields for state W4s:

-- LS-3645 IN W4

alter table form_state_w4
	add column County_Of_Residence varchar(30) DEFAULT NULL,
	add column County_Of_Principal varchar(30) DEFAULT NULL,
	add column Older tinyint(1) DEFAULT FALSE,
	add column Spouse_Older  tinyint(1) DEFAULT FALSE,
	add column Total_Boxes int(1) DEFAULT NULL,
	add column Total_Exemptions int(30) DEFAULT NULL,
	add column Add_State_Withhold int(9) DEFAULT NULL,
	add column Add_County_Withhold int(9) DEFAULT NULL;

-- LS-3641 OH W4

alter table form_state_w4 
	add column School_Dis_Name varchar(50) DEFAULT NULL,
	add column School_Dis_No varchar(30) DEFAULT NULL;

-- LS-3642 NC W4

alter table form_state_w4
	add column Country_Name varchar(30) DEFAULT NULL;

-- LS-3644 MO W4

alter table form_state_w4 
	add column Reduced_Withholding int(9) DEFAULT NULL,
	add column Exempt_Reason varchar(1) DEFAULT NULL;

-- alter table form_state_w4 change checkBox1 Check_Box_1 tinyint(1) DEFAULT false,
-- 	change checkBox2 Check_Box_2 tinyint(1) DEFAULT false,
--     change checkBox3 Check_Box_3 tinyint(1) DEFAULT false,
--     change checkBox3 Check_Box_3 tinyint(1) DEFAULT false,
-- 	change ExemptReason Exempt_Reason varchar(1) DEFAULT NULL;

-- 3845 Add fields for MD and AL State W-4s
-- MD W-4
alter table form_state_w4 add column Complete_Address varchar(200) DEFAULT NULL after Address_id,
    add column Check_Box_1 tinyint(1) DEFAULT false,
    add column Check_Box_2 tinyint(1) DEFAULT false after Check_Box_1,
    add column Check_Box_3 tinyint(1) DEFAULT false after Check_Box_2,
	add column Check_Box_4 tinyint(1) DEFAULT false after Check_Box_3,
    add column Check_Box_5 tinyint(1) DEFAULT false after Check_Box_4,
    add column Resident_State char(2) DEFAULT NULL,
    add column Domicile_State char(2) DEFAULT NULL after Resident_State,
    add column Applicable_Year int(11) DEFAULT NULL,
    add column Exempt_Status_1 tinyint(1) DEFAULT false after Exempt,
	add column Exempt_Status_2 tinyint(1) DEFAULT false after Exempt_Status_1,
	add column Exempt_Status_3 tinyint(1) DEFAULT false after Exempt_Status_2,
    add column Exempt_Status_4 tinyint(1) DEFAULT false after Exempt_Status_3,
    add column Exempt_Status_5 tinyint(1) DEFAULT false after Exempt_Status_4,
    add column Exempt_Status_6 tinyint(1) DEFAULT false after Exempt_Status_5;

-- Added for MD W-4	
alter table address add column County varchar(100) DEFAULT NULL;

-- AL W-4
alter table form_state_w4 
	add column Marital_Status_Code_1 varchar(10) DEFAULT NULL,
	add column Marital_Status_Code_2 varchar(10) DEFAULT NULL;
	
-- Added for MN W-4	LS-3949

alter table form_state_w4 
	add column Married tinyint(1) DEFAULT FALSE,
	add column Married_Withhold tinyint(1) DEFAULT FALSE,
	add column MN_Allownaces tinyint(1) DEFAULT FALSE,
	add column MN_Allownaces_Section_A int(1) DEFAULT NULL,
	add column MN_Allownaces_Section_B int(1) DEFAULT NULL,
	add column MN_Allownaces_Section_C int(1) DEFAULT NULL,
	add column MN_Allownaces_Section_D int(3) DEFAULT NULL,
	add column MN_Allownaces_Section_E int(1) DEFAULT NULL,
	add column MN_Allownaces_Section_F int(4) DEFAULT NULL,
	add column MN_Withholding tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_A tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_B tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_C tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_D tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_E tinyint(1) DEFAULT FALSE,
	add column MN_Withholding_Section_F tinyint(1) DEFAULT FALSE,
	add column MN_Allowances_Section_Total int(4) DEFAULT NULL,
	add column MN_Withholding_Section_Total int(9) DEFAULT NULL,
	add column Domicile_Line varchar(30) DEFAULT NULL,
	add column Daytime_PhoneNumber varchar(11) DEFAULT NULL;

alter table form_state_w4 modify column Daytime_PhoneNumber varchar(25);

/* */
-- --------------------- End of release 20.5.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.6.0 changes:
/* *

call createStandardUSdocument('AR W4', 'AR W4 (AR4EC)'); -- LS-3984
call createStandardUSdocument('CT W4', 'CT W4'); -- LS-3647
call createStandardUSdocument('DC W4', 'DC W4 (D4)'); -- LS-3648
call createStandardUSdocument('HI W4', 'HI W4 (HW-4)'); -- LS-3986
call createStandardUSdocument('IA W4', 'IA W4'); -- LS-3696
call createStandardUSdocument('KS W4', 'KS W4 (K-4)'); -- LS-3983
call createStandardUSdocument('ME W4', 'ME W4 (W-4ME)'); -- LS-3698
call createStandardUSdocument('RI W4', 'RI W4'); -- LS-3985
call createStandardUSdocument('VT W4', 'VT W4 (W-4VT)'); -- LS-3987

-- LS-3647 CT state new fields
Alter table form_state_w4
add column Withholding_Code varchar(1) DEFAULT NULL,
add column MSRRA_EXEMPT tinyint(1) DEFAULT FALSE,
add column Legal_State varchar(30) DEFAULT NULL;

-- LS-3894 AR State new fields
Alter table form_state_w4
add column Low_Income_Tax tinyint(1) DEFAULT FALSE;

-- LS-4096 VT state new field.
Alter table form_state_w4
add column Head_Household_Exemptions int(2) DEFAULT NULL after Personal_Exemptions;

-- LS-3698 ME add fields 
alter table form_state_w4 
	add column Federal_Form tinyint(1) DEFAULT FALSE,
	add column Tax_Liability tinyint(1) DEFAULT FALSE,
	add column Perodic_Retirement tinyint(1) DEFAULT FALSE,
	add column Military_Spouse_Residency tinyint(1) DEFAULT FALSE;

-- LS-3696 IA form fields		
Alter table form_state_w4
	add column Effective_Year int(4) DEFAULT NULL,
	add column Personal_Allownaces int(1) DEFAULT NULL,
	add column Dependent_Allownaces int(3) DEFAULT NULL,
	add column Deduction_Allownaces int(3) DEFAULT NULL,
	add column Adjustment_Allownaces int(3) DEFAULT NULL,
	add column Child_Dependent_Allownaces int(1) DEFAULT NULL;
	
-- LS-3648 DC form fields
alter table form_state_w4
    add column Allownaces_Section_A int(3)  DEFAULT NULL,
	add column Allownaces_Section_B int(3)  DEFAULT NULL,
	add column Allownaces_Section_Total  int(10)  DEFAULT NULL,
	add column Radio_Button1 tinyint(1) DEFAULT NULL,
	add column Radio_Button2 tinyint(1) DEFAULT NULL;

/* */
-- --------------------- End of release 20.6.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.6.1 changes:
/* *

alter table production modify custom_text2 varchar(2000); -- LS-4219

/* */
-- --------------------- End of release 20.6.1 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.7.0 changes:
/* *

call createStandardUSdocument('ID W4', 'ID W4'); -- LS-4177
call createStandardUSdocument('NE W4', 'NE W4 (W-4N)'); -- LS-4175
call createStandardUSdocument('MS W4', 'MS W4 (89-350)'); -- LS-3982
call createStandardUSdocument('SC W4', 'SC W4'); -- LS-3695
call createStandardUSdocument('OK W4', 'OK W4'); -- LS-3849
call createStandardUSdocument('DE W4', 'DE W4'); -- LS-4176
call createStandardUSdocument('MT W4', 'MT W4 (MW-4)'); -- LS-4178
call createStandardUSdocument('PR W4', 'PR W4 (499 R-4.1)'); -- LS-4228
call createStandardUSdocument('WV W4', 'WV W4 (WV/IT-104)'); -- LS-4179

-- LS-3849 OK form fields
alter table form_state_w4     
    add column Allowances_Section_C int(3)  DEFAULT NULL, 
    add column Allowances_Section_D int(3)  DEFAULT NULL,
    add column Allowances_Section_E int(5)  DEFAULT NULL;
-- LS-4278
alter table form_i9 add column Alien_Work_Expiration_Date varchar(10) DEFAULT NULL after Work_Auth_Expiration_Date;

-- LS-4278
alter table form_i9 add column Alien_Work_Expiration_Date varchar(10) DEFAULT NULL after Work_Auth_Expiration_Date;

-- LS-3982 MS form fields
alter table form_state_w4 
	add column Check_Box_6 tinyint(1) DEFAULT false after Check_Box_5;
	
-- LS-4178 MT form fields
alter table form_state_w4     
    add column Mt_Allowances_Total int(5)  DEFAULT NULL;

-- LS-4228
alter table form_state_w4
	add column Mailing_Addr_Id int(11) DEFAULT NULL,
	add column Spouse_Full_Name varchar(70) DEFAULT NULL,
	add column Spouse_Social_Security varchar(1000) DEFAULT NULL,
	add column Optional_Tax_Computation tinyint(1) DEFAULT '0',
	add column Military_Spouse_Residency_Relief tinyint(1) DEFAULT '0',
	add column Employer_Not_Consider_Exemption tinyint(1) DEFAULT '0',
	add column Personal_Exemption_Status varchar(2) DEFAULT NULL,
	add column Vet_Exemption_Status varchar(2) DEFAULT NULL,
	add column Home_Mortgage_Interest_Allowance int(11) DEFAULT NULL,
	add column Charitable_Contributation_Allowance int(11) DEFAULT NULL,
	add column Medical_Expenses_Allowance int(11) DEFAULT NULL,
	add column Student_Loan_Interest_Allowance int(11) DEFAULT NULL,
	add column Gov_Pension_Contributions_Allowance int(11) DEFAULT NULL,
	add column Individ_Retirement_Acct_Allowances int(11) DEFAULT NULL,
	add column Education_Contribution_Allowance int(11) DEFAULT NULL,
	add column Health_Saving_Acct_Allowance int(11) DEFAULT NULL,
	add column Casualty_Residence_Loss_Allowance int(11) DEFAULT NULL,
	add column Personal_Property_Loss_Allowance int(11) DEFAULT NULL,
	add column Total_Allowances_Amt int(11) DEFAULT NULL,
	add column Auth_Employer_Pay_Period_Withholding tinyint(1) DEFAULT '0',
	add column Employer_Withhold_Pay_Period_Amt int(11) DEFAULT NULL,
	add column Employer_Withhold_Pay_Period_Percent int(11) DEFAULT NULL,
	add column Joint_Custody_Dependents int(11) DEFAULT NULL,
	add column Employee_Participates_Gov_Pension tinyint(1) DEFAULT '0',
	add column Complete_Mailing_Address varchar(200) DEFAULT NULL after Complete_Address,
	add column Date_Of_Birth_Month int(2) DEFAULT NULL after Date_of_Birth,
	add column Date_Of_Birth_Day int(2) DEFAULT NULL after  Date_Of_Birth_Month,
	add column Date_Of_Birth_year int(4) DEFAULT NULL after Date_Of_Birth_Day;

alter table form_state_w4 add CONSTRAINT form_state_w4_ibfk_2 FOREIGN KEY (Mailing_Addr_Id) REFERENCES address (Id);

-- LS-4239 WV W4
alter table form_state_w4 add column Certified_Legal_State_Or_Res char(2) Default NULL after Legal_State,
	add column Non_Resident_Addr_Id int(11) Default NULL after Address_Id,
	add column Non_Resident_Social_Security varchar(1000) default null after Social_Security,
	add column Non_Resident_Full_Name varchar(62) default null after Full_Name,
	add column Married_Exemptions int(11) Default null after Personal_Exemptions;
    
alter table form_state_w4 add CONSTRAINT form_state_w4_ibfk_3 FOREIGN KEY (Non_Resident_Addr_Id) REFERENCES address (Id);

-- LS-4240 Add flag to determnine if it is the first or second signature on WV W4 form.
alter table form_state_w4 add column Show_Second_Signature tinyint(1) DEFAULT 0;

	
/* */
-- --------------------- End of release 20.7.0 changes ------------------------------------------
/* */
/* */
-- --------------------- START of release 20.8.0 changes:
/* *

-- LS-4223 KY-W4 
alter table form_state_w4
	add column Check_Box_7 tinyint(1) DEFAULT '0',
	add column Check_Box_8 tinyint(1) DEFAULT '0',
	add column Check_Box_9 tinyint(1) DEFAULT '0',
	add column Check_Box_10 tinyint(1) DEFAULT '0',
	add column Check_Box_11 tinyint(1) DEFAULT '0',
	add column Check_Box_12 tinyint(1) DEFAULT '0',
	add column Illinois tinyint(1) DEFAULT '0',
	add column Indiana tinyint(1) DEFAULT '0',
	add column Michingan tinyint(1) DEFAULT '0',
	add column West_Virgina tinyint(1) DEFAULT '0',
	add column Wisconsin tinyint(1) DEFAULT '0',
	add column Virginia tinyint(1) DEFAULT '0',
	add column Ohio tinyint(1) DEFAULT '0';
	
-- LS-3709 Add 	Acknowledged in model release
alter table form_model_release_print
	add column Acknowledged tinyint(1) DEFAULT '0';

-- Add new table "activity_log" for address-unification project. LS-4436
drop table  if exists activity_log;
CREATE TABLE activity_log (
  id int(11) NOT NULL AUTO_INCREMENT,
  version tinyint(4) NOT NULL DEFAULT '1',
  type varchar(30) not null,
  created timestamp not null default 0,
  processed timestamp null,
  next_run timestamp null,
  skip boolean default false,
  user_acct varchar(20) DEFAULT NULL,
  mailing_addr_changed boolean default false,
  permanent_addr_changed boolean default false,
  PRIMARY KEY (id)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- fix unreported issue with Model name field too short.
alter table form_model_release_print modify model_name varchar(62);
alter table form_model_release_print modify Model_Print_Name varchar(125) DEFAULT NULL;

/* */
-- --------------------- End of release 20.8.0 changes ------------------------------------------	
/* */  
/* */
-- --------------------- START of release 20.9.0 changes:
/* *

-- LS-4467 MOdel release form title section added new fields
alter table form_model_release_print
add column shootUse tinyint(1) DEFAULT '1';

-- LS-4460 Model release form title section added new field
alter table form_model_release_print
add column Agent_Percentage int(2) DEFAULT NULL;

-- alter table activity_log add Skip boolean default false after next_run;

-- 4/17/20 Update registration table for ESS; ESS-1346
alter table user_registration
	modify COLUMN date_of_birth date  NULL,
	ADD COLUMN account_type varchar(10) NULL;

-- TOCS user migration to TTCO
alter table user 
	ADD COLUMN last_client_id varchar(20) NULL after preferences;

-- change User FEIN field to use AES encryption instead of jasypt. LS-4572
ALTER TABLE user
	ADD COLUMN Federal_Tax_Id_Aes longblob DEFAULT null after Federal_Tax_Id,
    drop column social_security; -- old column, no longer used

-- LS-4539 Model release form added new fields
alter table form_model_release_print
add column Employer_Attach_Doc tinyint(1) DEFAULT '0';

-- LS-4506 Add Model Release checkbox to My Prod / Admin
alter table payroll_preference add column Use_Model_Release boolean default false;

-- LS-4574 Activity log changes
alter table activity_log add mailing_addr_changed boolean default false,
	add permanent_addr_changed boolean default false,
    modify type varchar(30) not null;
    
-- LS-4469 Transfer Page status icons
alter table contact_document
add column Sent_To_Performer tinyint(1) DEFAULT '0',
add column Sent_To_Union tinyint(1) DEFAULT '0',
add column Sent_To_TPS tinyint(1) DEFAULT '0';

-- LS-4461 Model release form Job location added new field
alter table form_model_release_print
add column Job_State varchar(2) DEFAULT NULL,
add column Job_City varchar(30) DEFAULT NULL;

alter table form_model_release_print drop column Job_Location;

-- mark previously transferred Canadian documents as if they had been transferred only to the performer:
update contact_document cd set cd.Sent_To_Performer = 1 where cd.Time_Sent is not null and cd.Form_Type in ('ACTRA_CONTRACT', 'ACTRA_PERMIT', 'UDA_INM')
	and cd.Sent_To_Performer = 0 and cd.Sent_To_TPS = 0 and cd.Sent_To_Union = 0;

/* */
-- --------------------- End of release 20.9.0 changes ------------------------------------------	
/* *

-- Release 20.9.1:  LS-4600 allow document ordering in packets
ALTER table document ADD column list_order integer DEFAULT 0 after Short_Name;

/* */
-- --------------------- START of release 20.10.0 changes:
/* *
-- LS-4504 Save of model release updates StartForm
alter table Start_Form
	add column Model_Release_Id int(11) NULL,
	add CONSTRAINT Model_Release_fk FOREIGN KEY (Model_Release_Id) REFERENCES Form_Model_Release_Print (`Id`);

-- LS-4603 Model release form Occupation added new field
alter table form_model_release_print
add column Occupation varchar(30) DEFAULT NULL;

-- LS-4602 Model release form Occupation added new field
alter table form_model_release_print
add column Other_City_Name varchar(30) DEFAULT NULL;

-- LS-4356 Added new field for SSN encryption changed non_resident_social_security and spouse_social_security fields to longblob
alter table form_state_w4 add column Social_Security_Enc LONGBLOB DEFAULT NULL after Social_Security,
	modify column Non_resident_Social_Security LONGBLOB DEFAULT NULL,
 	modify column Spouse_Social_Security LONGBLOB DEFAULT NULL;

-- LS-4504 Save of model release updates StartForm
alter table Start_Form modify column emp_reuse decimal(11,2) null;

/* */
-- --------------------- End of release 20.10.0 changes ------------------------------------------	
/* */ 
/* */
-- --------------------- START of release 20.11.0 changes:
/* *

-- LS-4470 Locking SSN in TTCO
alter table User add column ssn_locked boolean default false after social_security_aes;

-- LS-4589 model timecard 
alter table daily_time
	add column Weather_Day tinyint(1) DEFAULT '0',
	add column Intimates_Day tinyint(1) DEFAULT '0',
	add column comments varchar(90) DEFAULT null;
	
-- LS-4476 CA State W-4 form changes 
alter table form_state_w4 
	add column Additional_Radio tinyint(1) DEFAULT true;

/* */
-- --------------------- End of release 20.11.0 changes ------------------------------------------	
/* */ 
/* */
-- --------------------- START of release 20.12.0 changes:
/* */

/* */
-- --------------------- End of release 20.12.0 changes ------------------------------------------	
/* */
