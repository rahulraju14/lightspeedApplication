drop table  if exists form_actra_intent;
CREATE TABLE `form_actra_intent` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` tinyint(4) NOT NULL DEFAULT '1',
  `Advertiser` varchar(150) DEFAULT NULL,
  `Product` varchar(150) DEFAULT NULL,
  `Agency_Name` varchar(150) DEFAULT NULL,
  `Producer_Name` varchar(150) DEFAULT NULL,
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
  `Seasonal_Com` tinyint(1) DEFAULT '0',
  `Tags` tinyint(1) DEFAULT '0',
  `Dealer` tinyint(1) DEFAULT '0',
  `Informercial` tinyint(1) DEFAULT '0',
  `Short_Life_7_Days` tinyint(1) DEFAULT '0',
  `Short_Life_14_Days` tinyint(1) DEFAULT '0',
  `Short_Life_31_Days` tinyint(1) DEFAULT '0',
  `Short_Life_45_Days` tinyint(1) DEFAULT '0',
  `Commercial_Id` int(11) DEFAULT NULL,
  `Minor` tinyint(1) DEFAULT '0',
  `Num_Minors` int(11) DEFAULT NULL,
  `Minor_Ages` varchar(75) DEFAULT NULL,
  `Num_Extras_General` int(11) DEFAULT NULL,
  `Num_Extras_Group` int(11) DEFAULT NULL,
  `Num_Extras_Group_31` int(11) DEFAULT NULL,
  `Stunts` tinyint(1) DEFAULT '0',
  `Stunt_Type` varchar(100) DEFAULT NULL,
  `Ext_Scenes` tinyint(1) DEFAULT '0' COMMENT 'Exterior Scenes',
  `Ext_Scenes_Type` varchar(100) DEFAULT NULL,
  `Location_Shoot_40_Radius` tinyint(1) DEFAULT '0',
  `Weather_Permitting` tinyint(1) DEFAULT '0',
  `Weekend_Night` tinyint(1) DEFAULT '0',
  `Nude_Scenes` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `Commercial_Id_idx` (`Commercial_Id`)
--  KEY `Talent_Id_idx` (`Talent_Id`),
  CONSTRAINT `Commercial_Id` FOREIGN KEY (`Commercial_Id`) REFERENCES `commercial` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;