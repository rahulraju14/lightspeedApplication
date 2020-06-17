/**
 * File: GenderType.java
 */
package com.lightspeedeps.type;

/**
 *
 */
public enum GenderType {
	M("Male"),
	F("Female"),
	O("Other");

	private final String label;

	GenderType(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the
	 *         same as toString, but can be used from jsp pages since it follows
	 *         the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return label;
	}

	public boolean isOther() {
		return this == GenderType.O;
	}


}
