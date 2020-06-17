// file name - CalifSickLeaveType.java
package com.lightspeedeps.type;

/**
 * Enumeration used in {@link com.lightspeedeps.model.FormWTPA} class,
 * related to the sick leave type on CA WTPA form
 */
public enum CalifSickLeaveType {

	CA		("Accrues paid sick leave", 0),
	POLICY	("Accrues paid sick leave persuant to the employer's policy", 1),
	PLUS_24	("Employer provides no less than 24 hours", 2),
	EXEMPT			("Is exempt from paid sick leave under CA Labor Code", 3)
	;
	
	private final String heading;
	
	/** Index of the enum used by the radio buttons on the form. */
	private final int index;

	CalifSickLeaveType(String head, int index) {
		heading = head;
		this.index = index;
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
	 * Returns the name of this enum constant, exactly as declared in its enum declaration.
	 */
	public String getName() {
		return name();
	}
	
	/**See {@link #index}. */
	public int getIndex() {
		return index;
	}
}
