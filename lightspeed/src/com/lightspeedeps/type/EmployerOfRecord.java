/*	File Name:	EmployerOfRecord.java */
package com.lightspeedeps.type;

/**
 * Enumeration used when export (transferring) timecards
 * or documents, to separate data into groups based on how
 * the processing service will handle them.
 * <p>
 * For now, this is only used for the TEAM payroll service.
 */
public enum EmployerOfRecord {
	/** For TEAM, the code for Union crew */
	TEAM_ALT	("A", "A", "A: Alternative"),

	/** For TEAM, the code for DGA Union crew; it uses
	 * a different enum name, as it is grouped separately
	 * from other union crew. */
	TEAM_DGA	("A", "G", "A: DGA"),

	/** For TEAM, the code for Tours crew ("S") who are not under Worker's Comp. */
	TEAM_TOURS	("S", "S", "S: Tours"),

	/** For TEAM, the code for Non-Union crew and Talent (SAG) */
	TEAM_TALENT	("T", "T", "T: Talent"),

	/** Worker's Comp companies/clients use "D", including some Tours. */
	TEAM_D	("D", "D", "D: Worker's Comp"),

	NONE		("N", "N", "None"),
	;


	/** Text used when enum name is not appropriate */
	private final String heading;

	/** Prompt string for drop-down list */
	private final String prompt;

	/** Value used when grouping files during transfer/export. */
	private final String filenameKey;

	EmployerOfRecord(String head, String filekey, String pPrompt) {
		heading = head;
		prompt = pPrompt;
		filenameKey = filekey;
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

	/**
	 * @return The string passed in the export file for this enum value.
	 * Currently this is defined in the "heading" field.
	 */
	public String getExport() {
		return heading;
	}

	/** See {@link #prompt}. */
	public String getPrompt() {
		return prompt;
	}

	public String getFilenameKey() {
		return filenameKey;
	}

}
