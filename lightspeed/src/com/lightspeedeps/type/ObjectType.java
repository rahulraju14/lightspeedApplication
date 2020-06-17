/*	File Name:	ObjectType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the AuditEvent object to indicate the type
 * of "related object".  This essentially represents one of our
 * model classes.
 */
public enum ObjectType {
	/** WeeklyTimecard */
	WT			("WeeklyTimecard"),
	/** WeeklyBatch */
	WB			("WeeklyBatch"),
	;

	private final String className;

	ObjectType(String clas) {
		className = clas;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return className;
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

}
