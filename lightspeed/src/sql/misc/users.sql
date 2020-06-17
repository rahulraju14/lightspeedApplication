
-- Standard commands to create the user account 
-- used by the application to access its database.

/**  drop user lsuser01@localhost; /**/

create user lsuser22@localhost identified by 'lightspeed';
create user lightspeed@localhost identified by 'lightspeed';

-- this user only needs CRUD access, & only on its specific database:
grant delete, insert, select, update on lsdb22.* to lsuser22@localhost;
grant delete, insert, select, update on lsdb21.* to lightspeed@localhost;

