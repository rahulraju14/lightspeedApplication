SET foreign_key_checks = on;

-- ***  ACCOUNT MERGINE  ***

-- This script merges two email accounts, where the person is employed by a SINGLE PRODUCTION,
-- but has contacts on that production under two different email addresses (User accounts).
-- Essentially, everything associated with the 'unwanted' contact & User needs to be moved
-- to the preferred account.

--   This marks the unwanted contact as "DELETED". It does NOT change the status of the
-- unwanted User account.  When this is finished and checked, then from the UI the unwanted
-- User can be "Deleted".

set @fromMail = 'blah.blah@yahoo.blah';  -- unwanted email addr - moving data from this one to the "To" account
set @toMail   = 'blah.blah@google.blah';  -- desired email addr - moving data to this one.

-- use this to get production ID. Can't rely on using this without human verification,
--  since multiple productions may have the same name.
select * from production where title = 'some big production'; 

set @prodid = 0; -- set the Production.id value here.

-- determine the source user.id, user.account_number, and contact.id values:
 
set @fromUid =  ( select id from user where email_address = @fromMail limit 1) ;
set @fromAcct = ( select account_number from user where id = @fromUid) ;
set @fromCt  =  ( select id from contact where user_id = @fromUid and production_id = @prodid);
 
-- determine the target user.id, user.account_number, and contact.id values:

set @toUid =  ( select id from user where email_address = @toMail limit 1) ;
set @toAcct = ( select account_number from user where id = @toUid) ;
set @toCt =   ( select id from contact where user_id = @toUid and production_id = @prodid);

-- may be used to check values:
select @fromMail, @fromUid, @fromAcct, @fromCt, @toMail, @toUid, @toAcct, @toCt;

-- mark old Contact as deleted
/* */ update contact set status = 'DELETED' where id = @fromCt;
/**/

-- Update timecards - account number
/* */ update weekly_time_card set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update Start_form.contact_id
/* */ update Start_form set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update Contact_document.contact_id
/* */ update Contact_document set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update employment.contact_id
/* */ update employment set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update image.contact_id (image from Cast & Crew)
/* */ update image set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update image.user_Id (image from My Account)
/* */ update image set user_Id = @toUid where user_Id = @fromUid;
/**/

-- Update contact_doc_event.user_account (document e-signings by this user)
/* */ update contact_doc_event set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update doc_change_event.user_account (I9 audit trail)
/* */ update doc_change_event set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update time_card_event.user_account (TC e-signings)
/* */ update time_card_event set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update Document.Owner_Id (person who uploaded a doc)
/* */ update Document set Owner_Id = @toUid where Owner_Id = @fromUid;
/**/

-- Update Form_I9.contact_id
/* */ update Form_I9 set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update Form_I9.user_account
/* */ update Form_I9 set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update timecard_change_event.user_account (TC audit trail)
/* */ update timecard_change_event set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update approval_path_contact_pool.contact_id
/* */ update approval_path_contact_pool set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update approval_path_editor.contact_id
/* */ update approval_path_editor set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update approval_group_contact_pool.contact_id
/* */ update approver_group_contact_pool set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update approver.contact_id
/* */ update approver set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update audit_event.user_account (TC HTG audit trail)
/* */ update audit_event set user_account = @toAcct where user_account = @fromAcct;
/**/

-- Update tax_wage_allocation_form.contact_id
/* */ update tax_wage_allocation_form set contact_id = @toCt where contact_id = @fromCt;
/**/

-- Update user_client.user_Id (client ids from TOCS Account)
/* * update user_client set user_Id = @toUid where user_Id = @fromUid;
/**/





/* */
/* */
