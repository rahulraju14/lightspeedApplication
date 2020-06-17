-- Create new Address instances from the address data in existing Form_W4 records.

-- **** BACKUP DATABASE BEFORE RUNNING THIS ************

/* *
-- Clean up existing address fields so we don't create unnecessary Address instances...
-- first replace empty strings with nulls:
update form_W4 set address = null where address = '';
update form_W4 set city = null where city = '';
update form_W4 set state = null where state = '';
update form_W4 set zip = null where zip = '';

-- then set state = null if all other fields are null:
update form_W4 set state = null
where address is  null 
 and city is  null
 and zip is  null;

-- add temporary column to Address to hold matching W4 id
Alter table address add w4_id integer;

insert into address(addr_line1,city,state,zip,w4_id) 
select address,city,state,zip,id from form_W4
where  (address is not null 
 or city is not null
 or state is not null
 or zip is not null) ;

update form_W4 w4, address a 
set w4.address_id= a.id
where a.w4_id = w4.id;

/* */
/* Run this to see results:
select w.id, address_id, w.address,w.city,w.state,w.zip, a.* from form_W4 w, address a
where address_id is not null and a.id = address_id order by address_id;
/* */
/* Run this to drop old data:
alter table Address drop w4_id;
alter table form_W4 drop address, drop city, drop state, drop zip;
/* */
