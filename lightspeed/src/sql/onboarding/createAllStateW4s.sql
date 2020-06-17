-- 
-- Run the createStateW4 procedure for each of the state W4 forms:

-- it is defined as:
--  PROCEDURE `createAnyStandardDocument`(IN form_desc VARCHAR(30), IN form_name VARCHAR(30))


call createAnyStandardDocument('NY W4', 'NY W4 (IT-2104)');
call createAnyStandardDocument('CA W4', 'CA W4 (DE-4)');

-- add to all productions, existing state W4s for AZ, GA, IL, LA :

call createAnyStandardDocument('AZ W4', 'AZ W4 (A-4)'); -- AZ
call createAnyStandardDocument('GA W4', 'GA W4 (G-4)'); -- GA
call createAnyStandardDocument('IL W4', 'IL W4'); -- IL
call createAnyStandardDocument('LA W4', 'LA W4 (L-4)'); -- LA 

/*
-- These will correct entries for the "old" state W4s that may have been distributed already.

update document set name = 'AZ W4 (A-4)', type = 'AZ W4 (A-4)' where name = 'A4';
update document_chain set name = 'AZ W4 (A-4)' where name = 'A4';

update document set name = 'GA W4 (G-4)', type = 'GA W4 (G-4)' where name = 'G4';
update document_chain set name = 'GA W4 (G-4)' where name = 'G4';

update document set name = 'IL W4', type = 'IL W4' where name = 'IL-W4';
update document_chain set name = 'IL W4' where name = 'IL-W4';

update document set name = 'LA W4 (L-4)', type = 'LA W4 (L-4)' where name = 'L4';
update document_chain set name = 'LA W4 (L-4)' where name = 'L4';

*/
