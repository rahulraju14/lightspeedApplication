/*	File Name:	MediumType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the PayrollPreference object, indicating if the target
 * medium is theater or television. This is used to filter the sub-categories of
 * production type and payroll preferences available, and affects which
 * ContractRules are applied during HTG (hours-to-gross) processing, and which
 * Occupations are available when editing StartForms.
 * 
 * @see com.lightspeedeps.model.ContractRule
 * @see com.lightspeedeps.model.Occupation
 */
public enum MediumType {
	/** Feature production */
	FT		("F", "Theatrical (Feature)"),
	/** TV production */
	TV		("T", "Television"),
	/** N/A - used in Occupation/Rates when an entry applies to both Feature and TV. */
	N_A		("A", "N/A"),	// not used in UI, only in Occupations
	/** Commercial production company; this choice is not included in drop-down selections. */
	CM		("C", "Commercial"),
	;

	/** The corresponding code used in the Occupation table for this medium type. */
	private final String occupationCode;

	/** The mixed-case name that may be used when displaying values of this type. */
	private final String heading;

	MediumType(String occCode, String head) {
		occupationCode = occCode;
		heading = head;
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

	/** See {@link #occupationCode}. */
	public String getOccupationCode() {
		return occupationCode;
	}

}
