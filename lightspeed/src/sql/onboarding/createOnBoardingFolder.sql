-- --------------------------------------------------------------------------------
-- Routine DDL
-- Note: comments before and after the routine body will not be stored by the server
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists createOnBoardingFolder $$

CREATE PROCEDURE `createOnBoardingFolder`()
BEGIN

DECLARE v_finished INT default 0;
DECLARE parent_id_var INT;
DECLARE owner_id_var INT;
DECLARE central_repo_id cursor for 
select id,owner_id from folder where name ='Central Repository' and id not in (select parent_id from folder where name='Onboarding');

DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1; 
OPEN central_repo_id;

read_central_repo_loop: LOOP
	FETCH central_repo_id INTO parent_id_var,owner_id_var;
	IF v_finished = 1 THEN
	LEAVE read_central_repo_loop;
	END IF;
	 insert into folder (name,owner_id,private,created,parent_id) values ('Onboarding',owner_id_var,0,now(),parent_id_var);
	END LOOP;
	CLOSE central_repo_id;

END $$

DELIMITER ;


