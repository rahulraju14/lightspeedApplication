//	File Name:	ProductionPhase.java
package com.lightspeedeps.type;

/**
 * An enumeration used on Commercial Productions indicate the status of the
 * production related to shooting - prep, shoot, or wrap.
 * <p>
 * The enum values are kept short so they take a minimum of database space,
 * while still being stored as a name value, rather than ordinal (which is very
 * brittle).
 */
public enum ProductionPhase {
	/** Prep */
	P		("Prep"),
	/** Shoot */
	S		("Shoot"),
	/** Wrap */
	W		("Wrap"),
	/** N/A; used for "No Change" on Global Updates page. */
	N_A		("No Chg");

	private final String heading;

	private ProductionPhase(String head) {
		heading = head;
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

}
