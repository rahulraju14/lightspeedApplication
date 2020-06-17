SET foreign_key_checks = on;

/* ** Upgrade PROD DATA ADMIN to LS EPS ADMIN in one or more productions **/

set @prod = '%foobar%'; -- production title, may use "like" pattern; ok to match multiple productions
set @mail = ''; -- email of user to change, e.g., 'fred@lightspeedeps.com' ;

set @uid = ( select id from user where email_address like @mail limit 1) ;

/* */
-- This is query code if you want to see what will be updated.
select e.*, p.title, p.id from employment e, contact c, production p 
	where c.user_id = @uid and e.contact_id = c.id  and c.production_id = p.id
	and e.role_id  = 422 -- look for "prod data admin" entry
	and p.title like @prod;
/* */

/* *
-- This is the update code; note updates to both Contact and Employment,
-- so result count will be 2 times number of productions selected.

update employment e, contact c, production p 
	set e.role_id = 2, c.role_id=2, e.occupation = 'LS eps Administrator', e.permission_mask = 2305843009213693951
where c.user_id = @uid and e.contact_id = c.id and c.production_id = p.id
	and e.role_id = 422
	and p.title like @prod;

/* */
/* */
	