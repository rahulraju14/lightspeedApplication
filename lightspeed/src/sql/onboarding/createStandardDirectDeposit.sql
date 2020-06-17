-- --------------------------------------------------------------------------------
-- Routine DDL
-- Note: comments before and after the routine body will not be stored by the server
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists createStandardDirectDeposit $$

CREATE PROCEDURE `createStandardDirectDeposit`()
BEGIN

DECLARE v_finished INT default 0;
DECLARE folder_id_var INT;
DECLARE central_repo_id cursor for 
select id from folder where name = "Onboarding" and id not in ( select distinct folder_id from document_chain 
where name='Direct Deposit');

DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1; 
OPEN central_repo_id;

read_central_repo_loop: LOOP
	FETCH central_repo_id INTO folder_id_var;
	IF v_finished = 1 THEN
	LEAVE read_central_repo_loop;
	END IF;
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Direct Deposit','LS_FORM', now(), now(), 1, 'L220001', 'RD_SN_SUB', folder_id_var, 0);
	 
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Direct Deposit', 'Direct Deposit Form', 'System', 1, now(), now(), now(), 0, 'Direct Deposit', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Direct Deposit' and folder_id = folder_id_var), 1, 1, 0, 1);
	END LOOP;
	CLOSE central_repo_id;

END $$

DELIMITER ;


