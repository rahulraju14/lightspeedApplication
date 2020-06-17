/*	File Name:	RuleType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the ContractRule object to indicate the type of rule
 * being selected. This will probably control which table the associated rule is
 * retrieved from.
 */
public enum RuleType {
	/** Basic-Weekly */
	BA		("Weekly", "Basic-Weekly"),
	/** Guaranteed Hours */
	GT		("Guarantee", "Guaranteed Hours"),
	/** "Overtime Breakage */
	OT		("Overtime", "Overtime Breakage"),
	/** On-Call Factors */
	OC		("OnCall", "On-Call Factors"),
	/** Golden Time Breakage */
	GL		("Golden", "Golden Time Breakage"),
	/** Rest Time - Forced Call */
	RS		("Rest", "Rest Time - Forced Call"),
	/** Call-Back Factors */
	CB		("CallBack", "Call-Back Factors"),
	/** Meal Period Violation factors */
	MP 		("MPV", "Meal Period Violations"),
	/** Night Premium */
	NP 		("NtPrem", "Night Premium"),
	/** Forced Call */
	FC 		("Forced", "Forced Call"),
	/** Holiday Factors */
	HO 		("Holiday", "Holiday Factors"),
	/** Holiday Lists */
	HL 		("Hol.List", "Holiday Lists"),
	/** Special Calculations */
	SP 		("Special", "Special Calculations"),
	;

	private final String heading;
	private final String brief;

	RuleType(String pBrief, String head) {
		brief = pBrief;
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

	/**See {@link #brief}. */
	public String getBrief() {
		return brief;
	}

}
