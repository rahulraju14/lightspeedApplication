/**
 * File: ToursPayPeriod.java
 */
package com.lightspeedeps.type;

/**
 * Enumeration used in Payroll Preferences for Pay Period preferences
 *
 */
public enum PayPeriodType {
	W("Weekly", 7),
	BW("Bi-weekly", 14),
	SM("Semi-Monthly", 15),
	M("Monthly", 15)
	;

	private final String heading;

	/** Index of the enum used by the radio buttons on the form. */
	private final int days;

	PayPeriodType(String head, int days) {
		heading = head;
		this.days = days;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns the same value
	 * as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the
	 *         same as toString, but can be used from jsp pages since it follows
	 *         the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

	/**
	 * Returns the name of this enum constant, exactly as declared in its enum
	 * declaration.
	 */
	public String getName() {
		return name();
	}

	/** See {@link #index}. */
	public int getDays() {
		return days;
	}

	public boolean isWeekly() {
		return this == PayPeriodType.W;
	}

	public boolean isBiWeekly() {
		return this == PayPeriodType.BW;
	}

	public boolean isSemiMonthly() {
		return this == PayPeriodType.SM;
	}

	public boolean isMonthly() {
		return this == PayPeriodType.M;
	}
}
