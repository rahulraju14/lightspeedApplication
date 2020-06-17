SET foreign_key_checks = off;
/* 
	Code to copy the Adress and Image records related to the payroll_service table entries,
	from one database to another.
	** This code assumes that the entries in the payrollService table were "moved" from the 
	old database to the new one by using mysqldump. **

*/

-- truncate image;
-- delete from Address where id > 1;
-- ALTER TABLE Address AUTO_INCREMENT = 2;

	call copy_one2one_records('payroll_service', 'id', 1, 'address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 1, 'mailing_address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 1, 'desktop_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 1, 'mobile_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 1, 'report_logo_id', 'image', 'dbt30','lsdbtest');
  
	call copy_one2one_records('payroll_service', 'id', 2, 'address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 2, 'mailing_address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 2, 'desktop_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 2, 'mobile_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 2, 'report_logo_id', 'image', 'dbt30','lsdbtest');

	call copy_one2one_records('payroll_service', 'id', 3, 'address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 3, 'mailing_address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 3, 'desktop_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 3, 'mobile_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 3, 'report_logo_id', 'image', 'dbt30','lsdbtest');

	call copy_one2one_records('payroll_service', 'id', 4, 'address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 4, 'mailing_address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 4, 'desktop_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 4, 'mobile_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 4, 'report_logo_id', 'image', 'dbt30','lsdbtest');

	call copy_one2one_records('payroll_service', 'id', 5, 'address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 5, 'mailing_address_id', 'address', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 5, 'desktop_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 5, 'mobile_logo_id', 'image', 'dbt30','lsdbtest');
	call copy_one2one_records('payroll_service', 'id', 5, 'report_logo_id', 'image', 'dbt30','lsdbtest');

/* */
	