-- --------------------------------------------------------------------------------
-- Routine DDL
-- Note: comments before and after the routine body will not be stored by the server
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists createStandardUDAContract $$

CREATE PROCEDURE `createStandardUDAContract`()
BEGIN

DECLARE v_finished INT default 0;
DECLARE folder_id_var INT;
DECLARE central_repo_id cursor for 
select f.id from folder f,production p where f.name = "Onboarding" and f.parent_id = p.repository_id and  p.type = 'CANADA_TALENT' and f.id not in ( select distinct folder_id from document_chain  where name='UDA INM Contract');


DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1; 
OPEN central_repo_id;

read_central_repo_loop: LOOP
	FETCH central_repo_id INTO folder_id_var;
	IF v_finished = 1 THEN
	LEAVE read_central_repo_loop;
	END IF;
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('UDA INM Contract','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('UDA INM Contract', 'Canada UDA INM Bilingual Contract form', 'System', 1, now(), now(), now(), 0, 'UDA INM Contract', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='UDA INM Contract' and folder_id = folder_id_var), 1, 1, 0, 1);
	END LOOP;
	CLOSE central_repo_id;

END $$

DELIMITER ;


