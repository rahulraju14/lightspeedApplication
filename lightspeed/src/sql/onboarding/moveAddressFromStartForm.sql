-- Create new Address instances from the address data in existing Start_Form records.

-- **** BACKUP DATABASE BEFORE RUNNING THIS ************

-- Clean up existing address fields so we don't create unnecessary Address instances...
-- first replace empty strings with nulls:
update start_form set addr_line1 = null where addr_line1 = '';
update start_form set addr_line2 = null where addr_line2 = '';
update start_form set city = null where city = '';
update start_form set state = null where state = '';
update start_form set zip = null where zip = '';
update start_form set country = null where country = '';

-- then set country = null if all other fields are null:
update start_form set country = null
where addr_line1 is  null 
 and addr_line2 is  null 
 and city is  null
 and state is  null
 and zip is  null
 and country ='us' ;

-- add temporary column to Address to hold matching StartForm's id
Alter table address add sf_id integer;

insert into address(addr_line1,addr_line2,city,state,zip,country,sf_id) 
select addr_line1,addr_line2,city,state,zip,country,id from start_form
where  (addr_line1 is not null 
 or addr_line2 is not null 
 or city is not null
 or state is not null
 or zip is not null
 or country is not null) ;

update start_form s, address a 
set s.permanent_address_id= a.id
where a.sf_id = s.id;
/* */
/* Run this to see results:
select id, permanent_address_id, addr_line1,addr_line2,city,state,zip,country from start_form
where  permanent_address_id is not null order by permanent_address_id;
/* */
/* Run this to drop old data: */
alter table Address drop sf_id;
alter table Start_form drop addr_line1, drop addr_line2, drop city, drop state, drop zip, drop country;
/* */
