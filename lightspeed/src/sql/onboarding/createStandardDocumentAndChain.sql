-- --------------------------------------------------------------------------------
-- Routine DDL
-- Note: comments before and after the routine body will not be stored by the server
-- --------------------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE if exists createStandardDocumentAndChain $$

CREATE PROCEDURE `createStandardDocumentAndChain`()
BEGIN

DECLARE v_finished INT default 0;
DECLARE folder_id_var INT;
DECLARE central_repo_id cursor for 
select id from folder where name = "Onboarding" and id not in ( select distinct folder_id from document_chain 
where type='LS_fORM');

DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_finished = 1; 
OPEN central_repo_id;

read_central_repo_loop: LOOP
	FETCH central_repo_id INTO folder_id_var;
	IF v_finished = 1 THEN
	LEAVE read_central_repo_loop;
	END IF;
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('I9','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Payroll Start','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('W4','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('CA WTPA','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('NY WTPA','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Direct Deposit','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('W9','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Minor Trust Account','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('Indemnification','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('G4','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('A4','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('L4','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('IL-W4','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('ACTRA Contract','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('ACTRA Work Permit','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 insert into document_chain (Name, Type, Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id, deleted) 
	 values ('ACTRA Intent','LS_FORM', now(), now(), 1, 'A010001', 'RD_SN_SUB', folder_id_var, 0);
	 
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('I9', 'Federal Form I9', 'System', 1, now(), now(), now(), 0, 'I9', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='I9' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Payroll Start', 'Payroll Start Form', 'System', 1, now(), now(), now(), 0, 'Payroll Start', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Payroll Start' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('W4', 'Form W-4', 'System', 1, now(), now(), now(), 0, 'W4', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='W4' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('CA WTPA', 'CA WTPA Form', 'System', 1, now(), now(), now(), 0, 'CA WTPA', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='CA WTPA' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('NY WTPA', 'NY WTPA Form', 'System', 1, now(), now(), now(), 0, 'NY WTPA', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='NY WTPA' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Direct Deposit', 'Direct Deposit Form', 'System', 1, now(), now(), now(), 0, 'Direct Deposit', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Direct Deposit' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('W9', 'Form W-9', 'System', 1, now(), now(), now(), 0, 'W9', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='W9' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Minor Trust Account', 'Minor Trust Account Form', 'System', 1, now(), now(), now(), 0, 'Minor Trust Account', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Minor Trust Account' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('Indemnification', 'Indemnification Form', 'System', 1, now(), now(), now(), 0, 'Indemnification', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='Indemnification' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('G4', 'Form G-4', 'System', 1, now(), now(), now(), 0, 'G4', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='G4' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('A4', 'Form A-4', 'System', 1, now(), now(), now(), 0, 'A4', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='A4' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('L4', 'Form L-4', 'System', 1, now(), now(), now(), 0, 'L4', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='L4' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('IL-W4', 'Form IL-W4', 'System', 1, now(), now(), now(), 0, 'IL-W4', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='IL-W4' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('ACTRA Contract', 'ACTRA Commercial Engagement Contract Form', 'System', 1, now(), now(), now(), 0, 'ACTRA Contract', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='ACTRA Contract' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('ACTRA Work Permit', 'ACTRA Commercial Work Permit Application Form', 'System', 1, now(), now(), now(), 0, 'ACTRA Work Permit', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='ACTRA Work Permit' and folder_id = folder_id_var), 1, 1, 0, 1);
	 insert into document (Name, Description, Author, Owner_Id, Created, Loaded, Updated, Private, Type, Content, Folder_Id, MimeType,
	 DocChainId, Revision, oldest, Deleted, standard) 
	 values ('ACTRA Intent', 'ACTRA Intent to Produce Form', 'System', 1, now(), now(), now(), 0, 'ACTRA Intent', null, folder_id_var,'LS_FORM', 
	 (select id from document_chain where name='ACTRA Intent' and folder_id = folder_id_var), 1, 1, 0, 1);
	 
	END LOOP;
	CLOSE central_repo_id;

END $$

DELIMITER ;


