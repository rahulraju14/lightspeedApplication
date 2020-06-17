package com.lightspeedeps.type;

public enum EmpStatus {

	NOT_STARTED	("Not Started"),

	WRAPPED		("Wrapped"),

	STARTED		("Started"),

	N_A			("N/A");

	private String heading;

	private EmpStatus(String head) {
		heading = head;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. This
	 * could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
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
