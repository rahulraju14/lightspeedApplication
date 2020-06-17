/*
	File Name:	ImportStatus.java
 */
package com.lightspeedeps.type;

/**
 * An enumeration used in WeeklyTimecard class to indicate the
 * status of a recently-completed import operation.
 */
public enum ImportStatus {
	UPDATED		("Updated"),
	REPLACED	("Replaced"),
	ADDED		("Added"),
	DELETED		("Deleted"),
	;

	/** Mixed-case label for UI or reporting purposes. */
	private String heading;

	private ImportStatus(String head) {
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
