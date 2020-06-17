SET foreign_key_checks = on;
/**/
replace Address values 	
(702,'1100 Wilshire Blvd',null,'Los Angeles','CA','90017',null,'PST',TRUE,null)	,
(703,'456 Arizona Ave',null,'Santa Monica','CA','90401',null,'PST',TRUE,null)	
;	
/**/
replace Contact values
(702,null,'Maggie',null,'Hendrie','Maggie Hendrie',702,703,2,FALSE,TRUE,FALSE,FALSE,FALSE,'310-555-1212','888-555-1212',null,0,null,FALSE,TRUE,FALSE,FALSE,FALSE,TRUE,TRUE,TRUE,FALSE,FALSE,'FULL',FALSE,FALSE,FALSE,2,TRUE,FALSE,101,null,'YAHOO',null,null,null,null,null,FALSE,FALSE,FALSE,0,0,null)	
;
/**/
replace User values 	
(702,'MHendrie',null,702,101,TRUE,FALSE,null,FALSE,0,'FILE_WRITE_DELETE',null)	
;	

/* Role 2=LS admin; 19=1st AD; */
replace Project_Member values 	
(1701,702,2,TRUE,101),
(1702,702,2,TRUE,1001),	
(1703,702,2,TRUE,1002)
;
	
replace Folder values 	
(702,"Maggie's files",702,FALSE,101)	
;	
	
replace Contract_State values 	
(1201,'NOT_DISTRIBUTED',102,702)	,
(1202,'NOT_DISTRIBUTED',103,702)	,
(1203,'DISTRIBUTED',104,702)	,
(1204,'SIGNED_AND_RETURNED',105,702)	,
(1205,'DISTRIBUTED',106,702)	,
(1206,'NOT_DISTRIBUTED',107,702)	,
(1207,'DISTRIBUTED',108,702)	,
(1208,'DISTRIBUTED',109,702)	,
(1209,'NOT_DISTRIBUTED',110,702)	,
(1210,'NOT_DISTRIBUTED',111,702)	
;	
/**/