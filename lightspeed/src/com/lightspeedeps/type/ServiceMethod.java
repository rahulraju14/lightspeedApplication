//	File Name:	ServiceMethod.java
package com.lightspeedeps.type;

/**
 * Enumeration used in PayrollService class to describe the method
 * of transferring information to and from the service.
 */
public enum ServiceMethod {
	/** Create and optionally Email Showbiz Crew Cards file and PDF */
	CREW_CARDS	("Create and optionally Email Showbiz Crew Cards file and PDF"),

	/** Create and optionally Email ABS Payroll File and PDF */
	ABS_FILE	("Create and optionally Email ABS Payroll File and PDF"),

	/** Create and optionally Email TEAM Export File and PDF */
	TEAM_FILE	("Create and optionally Email TEAM Export File and PDF"),

	/** Create and transfer (FTP) TEAM Export File and PDF */
	TEAM_FTP	("Create and transfer (FTP) TEAM Export File and PDF"),

	/** Create and Email TEAM PDF file (no data file) */
	TEAM_PDF	("Create and Email TEAM PDF file (no data file)"),

	/** Authorize & Post (Direct transfer) */
	AUTH_POST	("Authorize & Post (Direct transfer)"), // created for IndiePay

	/** Create and optionally Email Generic CSV File and PDF */
	CSV_FILE	("Create and optionally Email Generic CSV File and PDF"),

	/** Create and Email PDF File only; no data file available */
	PDF_ONLY	("Create and Email PDF File only; no data file available"),
	;

	private final String heading;

	ServiceMethod(String head) {
		heading = head;
	}

	/**
	 * @return True if the report generated for this Service should display
	 * the full 9-digit SSN.  This is currently used only for files transmitted
	 * to TEAM.
	 */
	public boolean includeSSN() {
		return (this == TEAM_FILE || this == TEAM_FTP || this == TEAM_PDF);
	}

	/**
	 * @return True if this is a TEAM-only transfer protocol.
	 */
	public boolean isTeam() {
		return (this == TEAM_FILE || this == TEAM_FTP || this == TEAM_PDF);
	}

	/**
	 * @return True if this is a TEAM transfer protocol that includes a data file.
	 */
	public boolean hasTeamData() {
		return (this == TEAM_FILE || this == TEAM_FTP);
	}

	/**
	 * @return True iff this method creates a PDF but no data file.
	 */
	public boolean hasNoDataFile() {
		return (this == PDF_ONLY || this == TEAM_PDF);
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

}
