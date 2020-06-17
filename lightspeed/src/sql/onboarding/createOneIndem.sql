SET foreign_key_checks = on;

/* DISTRIBUTE Indemnification to ONE production only: */

set @repoId = (select repository_id from production where title like '%PutTitleHere%' and status='active');

set @folder_id_var = (select id from folder where name = "Onboarding" and parent_id = @repoId and
	id not in ( select distinct folder_id from document_chain where name='Indemnification') );

select @repoId, @folder_id_var;

insert into document_chain 
	        (Name,            Type,      Created, Revised, Revisions, Creator_Acct, Document_Flow_Type, Folder_Id,      deleted) 
	 values ('Indemnification','LS_FORM', now(),   now(),   1,         'A010001',    'RD_SN_SUB',        @folder_id_var, 0  );
	 
insert into document 
			(Name,             Description,           Author,  Owner_Id, Created, Loaded, Updated, Private, 
			Type,              Content, Folder_Id,     MimeType, 
			DocChainId, 
			Revision, oldest, Deleted, standard) 

	values ('Indemnification', 'Indemnification Form', 'System', 1,        now(),   now(),  now(),   0, 
			'Indemnification', null,    @folder_id_var,'LS_FORM', 
			(select id from document_chain where name='Indemnification' and folder_id = @folder_id_var), 
			1,        1,      0,       1);

/* */
/* */
