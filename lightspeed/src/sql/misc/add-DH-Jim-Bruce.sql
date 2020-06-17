-- Add the SYSTEM production with a single Project and the minimum objects to support it.		
SET foreign_key_checks = off;	

/* */
-- delete from user where id = 2;

insert into User values 	(2,'XX10004','dxwight@lightspeedeps.com','REGISTERED',null,'Dwight',null,'Harm', 
null,     null,null,null,null,null,2,0,0,0,0,0,/*imdb*/null,'NONE',null,null,null,null,0,0,3,
-- created:
null,null,null,null,null,0, 0,null,0,null,/*pin*/null,
'M', null,null,null,null,null, null,null,0,0,null, 'X','U',null,0, null,null,null,null,null)	;
/*
insert into User values 	(4,'L210004','dwight@lightspeedeps.com','REGISTERED',null,'Dwight',null,'Harm', null,     null,null,null,null,null,2,0,0,0,0,0,null,'NONE',null,null,null,null,0,0,3,null,null,null,null,null,0,0,null,0,null,null)	;
insert into Contact values 	(4,4,1,'ACCEPTED',1,0,2,null,0,0,'dwight@lightspeedeps.com',null,null,null,null,2,0,0,0,0,0,0,0,0,0,0,'BRIEF',0,0,0,0,1,0,null,null,1,'DEFAULT')	;
insert into Project_Member values 	(4,4,2,1995094630630161407,null)	;

insert into User values 	(5,'L210005','jim.fitzgerald@lightspeedeps.com','REGISTERED',null,'Jim',null,'Fitzgerald',null,null,null,null,null,null,2,0,0,0,0,0,null,'NONE',null,null,null,null,0,0,3,null,null,null,null,null,0,0,null,0,null,null)	;
insert into Contact values 	(5,5,1,'ACCEPTED',1,0,2,null,0,0,'jim.fitzgerald@lightspeedeps.com',null,null,null,null,2,0,0,0,0,0,0,0,0,0,0,'BRIEF',0,0,0,0,1,0,null,null,1,'DEFAULT')	;
insert into Project_Member values 	(5,5,2,1995094630630161407,null)	;

insert into User values 	(3,'L210003','bruce.sands@lightspeedeps.com','REGISTERED',null,'Bruce',null,'Sands',null,null,null,null,null,null,2,0,0,0,0,0,null,'NONE',null,null,null,null,0,0,3,null,null,null,null,null,0,0,null,0,null,null)	;
insert into Contact values 	(3,3,1,'ACCEPTED',1,0,2,null,0,0,'bruce.sands@lightspeedeps.com',null,null,null,null,2,0,0,0,0,0,0,0,0,0,0,'BRIEF',0,0,0,0,1,0,null,null,1,'DEFAULT')	;
insert into Project_Member values 	(3,3,2,1995094630630161407,null)	;

/* for Kenneth in the lsdb30ken database:
insert into Contact values 	(5,457,1,'ACCEPTED',1,0,2,null,0,0,'Kenneth McLeod','edmcleod@aol.com',null,null,null,null,2,0,0,0,0,0,0,0,0,0,0,'BRIEF',0,0,0,0,1,0,null,null,1,'DEFAULT')	;
insert into Project_Member values 	(5,5,2,1995094630630161407,null)	;
		
	/* ********** END OF DATA  ************ */	
