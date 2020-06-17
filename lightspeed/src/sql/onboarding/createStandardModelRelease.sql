-- --------------------------------------------------------------------------------
-- Routine DDL
-- Note: comments before and after the routine body will not be stored by the server
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists createStandardModelRelease $$

CREATE PROCEDURE `createStandardModelRelease`()
BEGIN

DECLARE v_finished INT default 0;
DECLARE folder_id_var INT;
DECLARE central_repo_id cursor for 
select f.id from folder f, production p, payroll_preference pref, payroll_service ps where f.name = "Onboarding"
    and f.id not in ( select distinct folder_id from document_chain
    where name='Model Release Print')
    and f.parent_id = p.repository_id -- f.parent is Central Repository folder for production
    and p.type = 'TV_COMMERCIALS'
    and p.payroll_preference_id is not null
    and pref.id = p.payroll_preference_id and ps.id = pref.payroll_service_id
    and ps.name like '%TEAM%';
	
DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1; 
OPEN central_repo_id;

read_central_repo_loop: LOOP
	FETCH central_repo_id INTO folder_id_var;
	IF v_finished = 1 THEN
	LEAVE read_central_repo_loop;
	END IF;
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Model Release Print','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Model Release Print', 'Model Release Print form', 'System', 1, now(), now(), now(), 0, 'Model Release Print', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Model Release Print' and folder_id = folder_id_var), 1, 1, 0, 1);
	END LOOP;
	CLOSE central_repo_id;

END $$

DELIMITER ;


