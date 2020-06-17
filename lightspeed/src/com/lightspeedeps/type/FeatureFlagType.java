/**
 * File: FeatureFlagType.java
 */
package com.lightspeedeps.type;

/**
 * Enum of features that are being used for Feature Flag processing
 */
public enum FeatureFlagType {

	// * Insert new entries in alphabetical order *

	ESS_MY_PROFILE, // ESS My Profile page replaces TTCO My Account page. LS-4850
	TTCO_ADDR_UNIF_PAYROLL_START, // Address Unification changes for the Payroll Start form Product-1509
	TTCO_ADDR_UNIF_USER_PROFILE, // Address Unification changes for the My Account tab Product-1509
	TTCO_AS400_COUNTRY_LIST, // Use the AS400 list of countries for hybrid timecards and Tours timesheets
	TTCO_RESIDENT_COMMERCIAL_TIMECARDS, // Feature to display new resident changes for commercial timecards
	TTCO_ENHANCED_LOAN_OUT, // Feature adds LLC-type and tax classification to StartForm, etc.
	TTCO_ESS_RESET_PASSWORD, // Direct 'forgot password' link to ESS page
	TTCO_ESS_EMAIL_INVITATIONS, // Direct TTCO invitation emails to API for ESS styling. ESS-1471
	TTCO_ESS_SEAMLESS_INTEGRATION, // Support "cross-app links" to & from ESS page(s). LS-3758
	TTCO_ESS_REGISTER_INTEGRATION, // Direct TTCO registration to ESS pages
	TTCO_EXPENSE_NO_ATTACHMENT, // Feature to display warning message if Box Rental- NonTax, Gas, Lodging- NonTax, Mileage- NonTax, Parking- NonTax, Reimbursement are entered without an attachment.
	TTCO_I9_2020, // Use the new 2020 version of the I-9  LS-3979
	TTCO_IMPORT_LOANOUT, // Import process expects 4 additional fields for Loan-out info. LS-3043
	TTCO_LOCK_SSN, // Feature to lock SSN if verified ESS user or has a signed W4 LS-4571
	TTCO_MODEL_RELEASE_FORM, // Feature to include Model Release form to commercial productions
	TTCO_MRF_STARTS_AND_TIMECARDS, // Feature to have Start forms and timecards created from Model Release forms. LS-4757
	TTCO_MODEL_RELEASE_TIMECARD_PRINT, // Feature to include Model Release print changes
	TTCO_PAYROLL_START_PDF_PRINTING, // Use pdf and xfdf to print Payroll Start form LS-3636
	TTCO_RELEASE_NOTES, // Show releases notes for all of releases. Product-1411
	TTCO_SHOW_BANNER_MESSAGE, // Feature to display banner message on the Login page such as maintenance banner. LS-2965
	TTCO_SHOW_UDA,			// Feature to include the UDA-INM contract for Canadian productions
	TTCO_W4_2020, // Show 2020 version of W4. LS-2966
	TTCO_W4_EXEMPT_DISABLE, // Disable non-exempt fields if exempt is selected. If exempt then clears non-exempt fields on submit LS-3688
	TTCO_W4_W9_REMOVE_SUBMIT_VALIDATION, // Do not show update my account popup for W-4 and W-9 on submit LS-3712
	TTCO_W9_ADDR_UNIF_USER_PROFILE, // Address unification from W-9 to the My Account page for Product-1500
	;
}
