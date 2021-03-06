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
  `Sin_Num` int(11) DEFAULT NULL,
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
  `Com_Type_Digital_Media` varchar(45) DEFAULT '0',
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
