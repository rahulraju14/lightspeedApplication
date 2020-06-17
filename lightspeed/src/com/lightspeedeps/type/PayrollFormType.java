//	File Name:	PayrollFormType.java
package com.lightspeedeps.type;

import com.lightspeedeps.model.FormStateW4;
import com.lightspeedeps.util.app.Constants;

/**
 * An enumeration used in the {@link com.lightspeedeps.model.ContactDocument} class
 * to identify the specific type of payroll document referenced by the
 * ContactDocument (e.g., Payroll Start, W-4). The various attributes of the
 * enumeration have significant effects on how a given form is PROCESSED(!), in
 * addition to the usual specification of labels and text related to an
 * enumeration.
 * <p>
 * NOTE: the order of the enumeration determines the order of the records in the
 * data file transferred to a payroll service by a document Transfer request.
 */
public enum PayrollFormType {
	//      		  Heading				Def. Approval Path		Model Class				Document name			Document description						xfer Col Hdr	ExportType	Rpt pref			Master Report PDF
	CA_WTPA 		("CA WTPA",				"WTPA", 				"FormWTPA",				"CA WTPA",				"CA WTPA Form",								null,			"WT",		"CA_WTPA_",			"Form-CA-WTPA.pdf"),
	NY_WTPA 		("NY WTPA",				"WTPA", 				"FormWTPA",				"NY WTPA",				"NY WTPA Form",								null,			"WT",		"NY_WTPA_",			"Form-NY-WTPA.pdf"),
	DEPOSIT 		("Direct Deposit",		"Direct Deposit",		"FormDeposit",			"Direct Deposit",		"Direct Deposit",							null,			"DD",		"Dir_Dep_",			"Form-Direct-Deposit.pdf"),
	I9				("I-9",					"I-9", 					"FormI9",				"I9",					"Federal Form I9",							"Federal I9",	"I9",		"I9_",				"Form-I9.pdf"),
	INDEM  			("Indemnification",		"Indemnification", 		"FormIndem",			"Indemnification",		"Indemnification Form",						"Corp Indemn.",	"IN",		"INDEM_",	 		"Form-Indemnification.pdf"),
	MODEL_RELEASE 	("Model Release Print",	"Model Release Print", 	"FormModelRelease",		"Model Release Print",	"Model Release Print form",          		null,			"MODEL_RELEASE","MODEL_RELEASE_PRINT_",	"Form-Model-Release-Print.pdf"),
	MTA 			("Minor Trust Account",	"Minor Trust Account", 	"FormMTA",				"Minor Trust Account",	"Minor Trust Account Form",					"Minor Trust",	"MT",		"MTA_",				"Form-Minor-Trust-Account.pdf"),
	PROJECT			("Project",				null, 					"Project", 				null,					"Project/Job Description",					null,			"PR",		null,				null),
	START			("Payroll Start",		"Payroll Start",		"StartForm",			"Payroll Start", 		"Payroll Start Form",						null,			"PS",		"PS_",		 		"Form-Payroll-Start.pdf"),
	SUMMARY			("SUMMARY SHEET",		null, 					"Employment",			"SUMMARY SHEET", 		null, 										null,			"SU",		null,				null),
	T_W_ALLOC		("",					null, 					"TaxWageAllocationForm",null,					"Allocation Form",							null,			"TW",		null,				null),
	W4  			("W-4",					"W-4", 					"FormW4",				"W4",					"Federal Form W4",							"Federal W4",	"W4",		"W4_",		 		"Form-W4.pdf"),
	W4_WORKSHEET  	("W-4 Worksheet",		null, 					"FormW4",				"W4 Worksheet",			"Federal Form W4 Worksheet",				null,			"W4",		"W4_WORKSHEET",		"Form-W4-Worksheet.pdf"),
	W4_INSTR  		("W-4 Instructions",	null, 					"FormW4",				"W4 Instructions",		"Federal Form W4 Instructions",				null,			"W4",		"W4_INSTRUCTIONS",	"Form-W4-Instructions.pdf"),
	W9  			("W-9",					"W-9", 					"FormW9",				"W9",					"Federal Form W9",							"Federal W9",	"W9",		"W9_",		 		"Form-W9.pdf"),
	// State W4s:
	AL_W4  			("AL W4",				"W-4", 					"FormStateW4",			"AL W4",				"AL Form W4",								null,		    "ALW4", 	"AL_W4_",	        "Form-AL-W4.pdf"),
	AR_W4  			("AR W4",				"W-4", 					"FormStateW4",			"AR W4 (AR4EC)",		"AR W4 (AR4EC)",							null,		    "ARW4", 	"AR_W4_",	        "Form-AR-W4.pdf"),
	AZ_W4  			("AZ W4",				"W-4", 					"FormA4",				"AZ W4 (A-4)",			"AZ Form W4 (A-4)",							null,			"AZW4",		"AZ_W4_",	 		"Form-A4.pdf"),
	CA_W4			("CA W4",				"W-4", 					"FormStateW4",			"CA W4 (DE-4)",			"CA Form W4 (DE-4)",						null,			"CAW4",		"CA_W4_",	 		"Form-CA-W4.pdf"),
	CT_W4			("CT W4",				"W-4", 					"FormStateW4",			"CT W4",			    "CT Form W4",						        null,			"CTW4",		"CT_W4_",	 		"Form-CT-W4.pdf"),
	DC_W4			("DC W4",				"W-4", 					"FormStateW4",			"DC W4 (D4)",			"DC Form W4 (D4)",							null,			"DCW4",		"DC_W4_",	 		"Form-DC-W4.pdf"),
	DE_W4			("DE W4",				"W-4", 					"FormStateW4",			"DE W4",		        "DE Form W4",				        		null,			"DEW4",		"DE_W4_",	 		"Form-DE-W4.pdf"),
	GA_W4  			("GA W4",				"W-4", 					"FormG4",				"GA W4 (G-4)",			"GA Form W4 (G-4)",							null,			"GAW4",		"GA_W4_",	 		"Form-G4.pdf"),
	HI_W4  			("HI W4",				"W-4", 					"FormStateW4",			"HI W4 (HW-4)",			"HI Form W4 (HW-4)",						null,			"HIW4",		"HI_W4",	 		"Form-HI-W4.pdf"),
	IA_W4  			("IA W4",				"W-4", 					"FormStateW4",			"IA W4",				"IA Form W4 ",								null,			"IAW4",		"IA_W4_",	 		"Form-IA-W4.pdf"),
	ID_W4  			("ID W4",				"W-4", 					"FormStateW4",			"ID W4",				"ID Form W4 ",								null,			"IDW4",		"ID_W4_",	 		"Form-ID-W4.pdf"),
	IL_W4  			("IL W4",				"W-4", 					"FormILW4",				"IL W4",				"IL Form W4",								null,			"ILW4",		"IL_W4_",	 		"Form-IL-W4.pdf"),
	IN_W4  			("IN W4",				"W-4", 					"FormStateW4",			"IN W4 (WH-4)",		    "IN Form W4 (WH-4)",					    null,			"INW4",		"IN_W4_",	 		"Form-IN-W4.pdf"),
	KS_W4  			("KS W4",				"W-4", 					"FormStateW4",			"KS W4 (K-4)",		    "KS Form W4 (K-4)",					    	null,			"KSW4",		"KS_W4_",	 		"Form-KS-W4.pdf"),
	KY_W4  			("KY W4",				"W-4", 					"FormStateW4",			"KY W4 (K-4)",			"KY Form W4 (K-4)",							null,		    "KYW4", 	"KY_W4_",	        "Form-KY-W4.pdf"),
	LA_W4  			("LA W4",				"W-4", 					"FormL4",				"LA W4 (L-4)",			"LA Form W4 (L-4)",							null,			"LAW4",		"LA_W4_",	 		"Form-L4.pdf"),
	MA_W4			("MA W4",				"W-4",					"FormStateW4",			"MA W4 (M-4)",			"MA Form W4 (M-4)",							null,			"MAW4",		"MA_W4_",			"Form-MA-W4.pdf"),
	MD_W4			("MD W4",				"W-4", 					"FormStateW4",			"MD W4 (MW507)",		"MD Form W4 (MW507)",						null,			"MDW4",		"MD_W4_",	 		"Form-MD-W4.pdf"),
	ME_W4  			("ME W4",				"W-4", 					"FormStateW4",			"ME W4 (W-4ME)",		"ME Form W4 (W-4ME)",					    null,			"MEW4",		"ME_W4_",	 		"Form-ME-W4.pdf"),
	MI_W4  			("MI W4",				"W-4", 					"FormStateW4",			"MI W4",			    "MI Form W4",							    null,		    "MIW4",	    "MI_W4_", 		    "Form-MI-W4.pdf"),
	MN_W4  			("MN W4",				"W-4", 					"FormStateW4",			"MN W4 (W-4MN)",		"MN Form W4 (W-4MN)",						null,		    "MNW4", 	"MN_W4_",	        "Form-MN-W4.pdf"),
    MO_W4  			("MO W4",				"W-4", 					"FormStateW4",			"MO W4",				"MO Form W4",							    null,			"MOW4", 	"MO_W4_",	        "Form-MO-W4.pdf"),
    MS_W4  			("MS W4",				"W-4", 					"FormStateW4",			"MS W4 (89-350)",		"MS Form W4 (89-350)",						null,			"MSW4", 	"MS_W4_",	        "Form-MS-W4.pdf"),
    MT_W4  			("MT W4",				"W-4", 					"FormStateW4",			"MT W4 (MW-4)",			"MT Form W4",							    null,			"MTW4", 	"MT_W4_",	        "Form-MT-W4.pdf"),
    NC_W4			("NC W4",				"W-4", 					"FormStateW4",			"NC W4 (NC-4)",		    "NC Form W4 (NC-4)",						null,			"NCW4",		"NC_W4_",	 		"Form-NC-W4.pdf"),
	NE_W4			("NE W4",				"W-4", 					"FormStateW4",			"NE W4 (W-4N)",		    "NE Form W4 (W-4N)",						null,			"NEW4",		"NE_W4_",	 		"Form-NE-W4.pdf"),
	NJ_W4  			("NJ_W4",				"W-4", 					"FormStateW4",			"NJ W4",				"NJ Form W4",								null,		    "NJW4",     "NJ_W4_", 		    "Form-NJ-W4.pdf"),
    NY_W4			("NY W4",				"W-4", 					"FormStateW4",			"NY W4 (IT-2104)",		"NY Form W4 (IT-2104)",						null,			"NYW4",		"NY_W4_",	 		"Form-NY-W4.pdf"),
    OH_W4			("OH W4",				"W-4", 					"FormStateW4",			"OH W4 (IT 4)",		    "OH Form W4 (IT 4)",					    null,           "OHW4",	    "OH_W4_",	 		"Form-OH-W4.pdf"),
	OK_W4  			("OK W4",				"W-4", 					"FormStateW4",			"OK W4",				"OK Form W4",								null,		    "OKW4", 	"OK_W4_",	        "Form-OK-W4.pdf"),
	OR_W4			("OR W4",				"W-4", 					"FormStateW4",			"OR W4",		        "OR Form W4",							    null,           "ORW4",	    "OR_W4_",	 		"Form-OR-W4.pdf"),
	PR_W4			("PR W4",				"W-4", 					"FormStateW4",			"PR W4 (499 R-4.1)",	"PR Form W4 (499 R-4.1)",				    null,           "PRW4",	    "PR_W4_",	 		"Form-PR-W4.pdf"),
	RI_W4  			("RI W4",				"W-4", 					"FormStateW4",			"RI W4",		    	"RI Form W4",					    		null,			"RIW4",		"RI_W4_",	 		"Form-RI-W4.pdf"),
	SC_W4  			("SC W4",				"W-4", 					"FormStateW4",			"SC W4",		    	"SC Form W4",					    		null,			"SCW4",		"SC_W4_",	 		"Form-SC-W4.pdf"),
	VA_W4  			("VA W4",				"W-4", 					"FormStateW4",			"VA W4 (VA-4)",			"VA Form W4 (VA-4)",						null,		    "VAW4", 	"VA_W4_",	        "Form-VA-W4.pdf"),
	VT_W4  			("VT W4",				"W-4", 					"FormStateW4",			"VT W4 (W-4VT)",		"VT Form W4 (W-4VT)",						null,		    "VTW4", 	"VT_W4_",	        "Form-VT-W4.pdf"),
	WI_W4  			("WI W4",				"W-4", 					"FormStateW4",			"WI W4 (WT-4)",			"WI Form W4",								null,		    "WIW4", 	"WI_W4_",	        "Form-WI-W4.pdf"),
	WV_W4  			("WV W4",				"W-4", 					"FormStateW4",			"WV W4 (WV/IT-104)",	"WV W4 (WV/IT-104)",						null,		    "WVW4", 	"WV_W4_",	        "Form-WV-W4.pdf"),

	// Canadian forms:
	ACTRA_CONTRACT  ("ACTRA Contract",		"ACTRA Contract",		"FormActraContract",	"ACTRA Contract",		"ACTRA Contract",							null,			"AC",		"ACTRA_CT_",		"Form-ACTRA-Contract.pdf"),
	ACTRA_PERMIT  	("ACTRA Work Permit",	"Actra Work Permit",	"FormActraWorkPermit",	"ACTRA Work Permit",	"ACTRA Commercial Work Permit Application",	"ACTRA Permit",	"AWP",		"AWP_",		 		"Form-ACTRA-Work-Permit.pdf"),
	ACTRA_INTENT	("ACTRA Intent",		"ACTRA Intent",			"FormActraIntent",		"ACTRA Intent",			"ACTRA Intent to Produce Form",				null,			"AI",		"AI_",				"Form-ACTRA-Intent.pdf"),
	UDA_INM 		("UDA-INM Contract",	"UDA INM Contract", 	"FormUDAContract",		"UDA INM Contract",		"Canada UDA INM Bilingual Contract form",	null,			"UDA_INM",	"UDA_INM_",			"Form-UDA-INM-Contract.pdf"),
	// "Other" is used for custom (non-HTML) forms:
	OTHER			("Other",				null, 					null, 					null,					"non-standard form",						null,			"OT",		"OT_",				null),
	;

	/** Used for the 'toString()' result, and also as the column heading on the Document Transfer
	 * page (if the document appears there). */
	private final String heading;

	/** The name of the "standard" document in the repository. This document
	 * entry will not have any "content", but serves as a place-holder for
	 * doing document distribution. */
	private final String docName;

	/** The name of the default ApprovalPath created for this document type. */
	private final String defaultPath;

	/** The name of the Model class (without the package name) used to
	 * persist data for this form type.  For example, the FormW4 class persists
	 * data for the W4 PayrollFormType. */
	private final String className;

	/** A descriptive string used in some displays. */
	private final String description;

	/** The text used as the column heading on the Document Transfer page. */
	private final String transferColHeading;

	/** The value for the Record Type field in the export record for this form type.
	 * Used in the document Transfer process. */
	private final String exportType;

	/** The string used as the prefix of the printed report's file name.  This value
	 * should always end with an underscore ("_"). */
	private final String reportPrefix;

	/** The standard filename under which the document's original PDF should be
	 * stored in the LS document repository.  The 'master' XFDF is retrieved from this
	 * document for the purpose of printing instances of this form. */
	private final String fileName;

	PayrollFormType(String head, String defPath, String classNm, String pname, String desc, String xferColHead, String exp, String pref, String file) {
		heading = head;
		docName = pname;
		defaultPath = defPath;
		className = classNm;
		description = desc;
		if (xferColHead == null) { // not specified, use "Heading" value
			transferColHeading = head;
		}
		else {
			transferColHeading = xferColHead;
		}
		exportType = exp;
		reportPrefix = pref;
		fileName = file;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/** See {@link #docName}. */
	public String getName() {
		return docName;
	}

	/** See {@link #defaultPath}. */
	public String getDefaultPath() {
		return defaultPath;
	}

	/** See {@link #className}. */
	public String getClassName() {
		return className;
	}

	/** See {@link #description}. */
	public String getDescription() {
		return description;
	}

	/** See {@link #transferColHeading}. */
	public String getTransferColHeading() {
		return transferColHeading;
	}

	/** See {@link #exportType}. */
	public String getExportType() {
		return exportType;
	}

	/** See {@link #reportPrefix}. */
	public String getReportPrefix() {
		return reportPrefix;
	}

	/** True iff a Submit button should be displayed with this form.  LS-3576 */
	public boolean getShowSubmit() {
		if (this == START) {
			return true;
		}
		return false;
	}

	/** See {@link #fileName}. */
	public String getFileName() {
		return fileName;
	}

	/** Method used to append the version in the {@link #fileName} parameter to
	 * match the {@link #fileName} with the document's original PDF name
	 * stored in the LS document repository for specific version.
	 * @param version Version of the form
	 * @return fileName with version.
	 */
	public String getFileNameWithVersion(Byte version) {
		if (fileName != null) {
			return fileName.concat(".v" + version);
		}
		return fileName;
	}

	/**
	 * Finds the Enum value that corresponds to the given text. That is, it
	 * checks the "getName()" value of each Enum and compares it to the supplied
	 * text, looking for a match. This is different than the builtin valueOf()
	 * method, which compares against each Enum's name.
	 *
	 * @param str The text to match against the Enum label, or null.
	 * @return The matching entry, or OTHER if no match is found or if 'str' is
	 *         null.
	 */
	public static PayrollFormType toValue(String str) {
		PayrollFormType type = OTHER;
		if (str != null) {
			for (PayrollFormType formType : values()) {
				if (formType.getName() != null && formType.getName().equalsIgnoreCase(str)) {
					type = formType;
					break;
				}
			}
		}
		return type;
	}

	/**
	 * @return True iff this is a "WTPA" type of form. This setting has a
	 *         significant effect on the submit and approval process, and is
	 *         also assumed to imply some other aspects, such as how it is
	 *         printed. Note usage of this method within other PayrollFormType
	 *         methods.
	 */
	public boolean isWtpa() {
		return this == CA_WTPA || this == NY_WTPA;
	}

	/**
	 * @return True iff this is a "W4/G4/A4/L4/IL-W4" type of form. This setting has a
	 *         significant effect on the submit and approval process, and is
	 *         also assumed to imply some other aspects, such as how it is
	 *         printed. Note usage of this method within other PayrollFormType
	 *         methods.
	 */
	public boolean isW4Type() {
		return this == W4 || this == GA_W4 || this == LA_W4 || this == AZ_W4 || this == IL_W4
				|| isFormStateW4Type(); // includes all 'new' state W4s. LS-3576
	}

	/**
	 * @return True iff this is a form which uses the FormStateW4DTO for
	 *         persistence. Note usage of this method within other
	 *         PayrollFormType methods. LS-3576
	 */
	public boolean isFormStateW4Type() {
		// includes all state W4s that use the FormStateW4DTO class
		return (className != null) && className.equals(FormStateW4.class.getSimpleName());
	}

	/**
	 * @return True iff this is a form which uses the TTCO Onboarding API for
	 *         persistence and printing. LS-3576
	 */
	public boolean getUseOnboardingApi() {
		// Include Model release form
		// Includes all state W4s that use the FormStateW4DTO class
		return this == MODEL_RELEASE || isFormStateW4Type();
	}

	/**
	 * @return the prefix of the URL/URI string for calling the onboarding API
	 *         for the given form type.
	 */
	private String getApiUrlPrefix() {
		String prefix = Constants.ONBOARDING_URL_PREFIX;
		if (isW4Type()) {
			prefix += "formStateW4";
		}
		else {
			switch(this) {
				case MODEL_RELEASE:
					prefix += "formModelRelease";
					break;
				default:
					break;
			}
		}
		return prefix;
	}

	/**
	 * @return the the URL/URI string for calling the Save method of the
	 *         onboarding API for the given form type.
	 */
	public String getApiSaveUrl() {
		return getApiUrlPrefix();
	}

	/**
	 * @return the the URL/URI string for calling the Update method of the
	 *         onboarding API for the given form type.
	 */
	public String getApiUpdateUrl() {
		return getApiUrlPrefix();
	}

	/**
	 * @return the the URL/URI string for calling the find-by-id method of the
	 *         onboarding API for the given form type.
	 */
	public String getApiFindUrl() {
		return getApiUrlPrefix() + "/id/";
	}

	/**
	 * @return the the URL/URI string for calling the Delete method of the
	 *         onboarding API for the given form type.
	 */
	public String getApiDeleteUrl() {
		return getApiUrlPrefix() + "/id/";
	}

	/**
	 * @return the the URL/URI string for calling the Print method of the
	 *         onboarding API for the given form type.  Note that the print
	 *         method is currently the same across all form types.
	 */
	public String getApiPrintUrl() {
		return Constants.ONBOARDING_URL_PREFIX + "download/id/";
	}

	/**
	 * @return True iff this form supports the Auto-Fill function.
	 */
	public boolean isAutoFilled() {
		return this == START || isW4Type() || this == DEPOSIT || isWtpa() || this == MTA || this == W9 || this == INDEM
				|| this == ACTRA_CONTRACT || this == ACTRA_PERMIT  || this == UDA_INM || this == MODEL_RELEASE;
	}

	/**
	 * @return True iff this document type may be transferred to a payroll
	 *         service. Currently this is tailored to the TEAM service.
	 */
	public boolean isTransferable() {
		return this == I9 || this == START || isW4Type() || this == W9 || isWtpa() || this == PROJECT || this == T_W_ALLOC;
	}

	/**
	 * @return True if this form type needs the off-document "Approve" and "Reject" buttons.  Some forms, such
	 * as the I-9 and WTPA, only use on-document buttons.  This is used from JSP.
	 */
	public boolean getUsesApprove() {
		// shorter to list the ones that DON'T use Approve button:
		boolean notUsed = this == I9 || isWtpa()
				|| this == ACTRA_CONTRACT || this == ACTRA_PERMIT || this == UDA_INM
				|| this == MODEL_RELEASE;
		return ! notUsed;
	}

	/**
	 * @return True if this form may have "SIGN" event types from an employee.
	 *         Note that for most forms, an employee will create a SUBMIT event,
	 *         not a SIGN event.
	 */
	public boolean getEmployeeMaySign() {
		return isWtpa() ||			// WTPAs allow employee to SIGN, not SUBMIT
				isModelRelease() || // ModelReleasePrint allows employee to SIGN, not SUBMIT
				this == ACTRA_CONTRACT || this == UDA_INM; // ACTRA and UDA allow employee to SIGN, not SUBMIT
	}

	/**
	 * @return True if this form type allows a "Recall" operation.  Some forms, such
	 * as the I-9 and WTPA, are not allowed to be Recalled.
	 */
	public boolean getAllowsRecall() {
		// shorter to list the ones that DON'T use Recall:
		boolean notAllowed = this == I9 || isWtpa()
				|| this == ACTRA_CONTRACT
				|| this == ACTRA_PERMIT
				|| this == UDA_INM ;// UDA does not allow recall. LS-2390
		return ! notAllowed;
	}

	/**
	 * @return True iff this form should be automatically added to a production
	 *         when it has onboarding enabled. This covers all forms that are
	 *         "production-ready", and are in common use by all production
	 *         companies. It EXCLUDES forms that are specific to Team (or
	 *         another payroll service), or are only for Canadian productions.
	 */
	public boolean isAutoAdded() {
		return this == I9 || this == START || this == W4 || isWtpa() || this == DEPOSIT || this == W9
				|| isW4Type(); // LS-3576
	}

	/**
	 * @return True iff this form should be automatically added to Canadian
	 *         productions when onboarding is enabled.
	 */
	public boolean isAutoAddedCanada() {
		return this == ACTRA_CONTRACT || this == ACTRA_INTENT || this == ACTRA_PERMIT || this == UDA_INM;
	}

	/**
	 * @return True iff this form should be automatically added to a Team client's
	 *         production when it has onboarding enabled.  These forms are NOT added
	 *         to productions assigned a different (or no) payroll service.
	 */
	public boolean isAutoAddedTeam() {
		return (this == INDEM || this == MODEL_RELEASE);
	}

	/**
	 * @return True iff this form should be included in the "Add this document"
	 *         list on Prod Admin / Productions / Payroll. These forms are
	 *         typically only used by a small subset of productions.
	 */
	public boolean isManuallyAdded() {
		boolean manuallyAdded = this == INDEM || this == MTA;
		return manuallyAdded;
	}

	/**
	 * @return True if a user is allowed to upload a "replacement document" for
	 *         this document type.
	 */
	public boolean isReplaceAllowed() {
		if (this == SUMMARY || this == START || this == PROJECT || this == T_W_ALLOC) {
			return false;
		}
		return true;
	}

	/** @return true if this is a Canada ACTRA contract. Used in JSP. */
	public boolean isCanadaActra() {
		return this == ACTRA_CONTRACT;
	}

	/** @return true if this is a Canada ACTRA Work Permit. Used in JSP. */
	public boolean isActraPermit() {
		return this == ACTRA_PERMIT;
	}

	/** @return true if this is a Canada UDA-INM contract. Used in JSP. */
	public boolean isCanadaUda() {
		return this == UDA_INM;
	}

	/** @return true if this is a Canada-only form. Used in JSP. */
	public boolean isCanada() {
		return this == UDA_INM || this == ACTRA_PERMIT || this == ACTRA_CONTRACT;
	}

	/**
	 * @return True if this form type should be hidden (not displayed) in
	 *         Canadian productions.
	 */
	public boolean hideForCanada() {
		return (this == START || this == ACTRA_INTENT);
	}

	/** @return true if this is a Model Release Print contract. Used in JSP. LS-3163 */
	public boolean isModelRelease() {
		return this == MODEL_RELEASE;
	}

	/**
	 * @return True iff this document should be printed by merging a master PDF
	 *         document with an XFDF data stream. When false, the document is
	 *         printed using a Jasper report which retrieves the report data via
	 *         SQL.
	 */
	public boolean printUsingXFDF() {
		return isW4Type() || isWtpa() || this == DEPOSIT || this == OTHER || this == W9 || this == ACTRA_CONTRACT
				|| this == ACTRA_PERMIT || this == UDA_INM
				|| this == MODEL_RELEASE;
	}

	/**
	 * Finds the Enum value that corresponds to the given text. That is, it
	 * checks the "name()" value of each Enum and compares it to the supplied
	 * text, looking for a match.
	 *
	 * @param str The text to match against the Enum label, or null.
	 * @return The matching entry, or null if no match is found or if 'str' is
	 *         null.
	 */
	public static PayrollFormType stringToValue(String str) {
		PayrollFormType type = OTHER;
		if (str != null) {
			for (PayrollFormType formType : values()) {
				if (formType.name() != null && formType.name().equalsIgnoreCase(str)) {
					type = formType;
					break;
				}
			}
		}
		return type;
	}

	/**
	 * @return True iff this is a "G4/NY-W4" type of form, allows "Edit" action
	 *         on fields
	 *  	   Will return false for now until it is decided whether the
	 *  	   employer can edit the the employer specific fields
	 *
	 */
	public boolean getAllowW4Edit() {
//		return this == G4 || this.isFormStateW4Type();
		return false;
	}

	/**
	 * @return True iff this is a state W4 form that allows the number of
	 *         allowances to be blank even if 'Exempt' is false.
	 */
	public boolean getAllowW4Exempt() {
		// NOTE: Please add in alphabetical order.
		return this == AL_W4 || this == AR_W4 || this == CA_W4 || this == CT_W4 || this == DC_W4 || this == DE_W4 || this == HI_W4 ||
				this == IA_W4 || this == ID_W4 || this == IN_W4 || this == KS_W4 || this == KY_W4 || this == MA_W4 || this == MD_W4 ||
				this == ME_W4 || this == MI_W4 || this == MN_W4 || this == MO_W4 || this == MS_W4 || this == MT_W4 || this == NC_W4 ||
				this == NE_W4 || this == NJ_W4 || this == NY_W4 || this == OH_W4 || this == OK_W4 || this == OR_W4 || this == PR_W4 ||
				this == RI_W4 || this == SC_W4 || this == VA_W4 || this == VT_W4 || this == WI_W4 || this == WV_W4;
	}

}
