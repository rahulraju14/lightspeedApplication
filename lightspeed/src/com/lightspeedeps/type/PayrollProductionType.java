//	File Name:	PayrollProductionType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in PayrollPreference and other payroll processing, to
 * indicate the type of production as it relates to occupations, wage rates,
 * unions, contracts, etc. Currently only used to distinguish between varieties
 * of ASA production contract types.
 */
public enum PayrollProductionType {
	/** Theatrical production for ASA contract purposes. */
	ASA_THEATRICAL	("Theatrical", true, "F"),

	/** TV Pilot production for ASA contract purposes. */
	ASA_PILOT		("Pilot, Long Form, or 1st Yr of 1-Hr series", true, "P"),
	
	/** "Other" TV production (e.g., non-Pilot), for ASA contract purposes. */
	ASA_OTHER		("All Other TV", true, "O"),
	;

	/** */
	private final String heading;

	/** Suffix used to generate the Contract Code applicable for this production type. */
	private final String suffix;

	/** True iff this production type applies only to ASA contracts. */
	private final boolean asa;

	private PayrollProductionType(String head, boolean pAsa, String pSuffix) {
		heading = head;
		asa = pAsa;
		suffix = pSuffix;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
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
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/**See {@link #suffix}. */
	public String getSuffix() {
		return suffix;
	}

	/**See {@link #asa}. */
	public boolean getAsa() {
		return asa;
	}

}
