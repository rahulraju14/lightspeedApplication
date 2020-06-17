//	File Name:	MemberType.java
package com.lightspeedeps.type;

/**
 * Enumeration used in User object - user's type of membership/account.
 * <p>
 * ** DO NOT CHANGE THE ORDER OF THE ENUMERATED VALUES, as the values
 * are stored in the database as the 'ordinals' of the enumerated class,
 * i.e., the sequential number assigned based on their order of definition
 * in this class.
 */
public enum MemberType {

	/** ** DO NOT CHANGE THE ORDER OF THE ENUMERATED VALUES!! ** */
	
	/** 'Free' user is limited to owning a single Free production. */
	FREE		("Free"),		// 0
	/** 'Standard' has no limit to the number of Free productions they can own. */
	STANDARD	("Standard"),	// 1
	/** 'Student' (not used) */
	STUDENT		("Student"),	// 2
	/** 'Premium' (for LS staff) have access to non-standard "Create Production" list,
	 * and no limit to the number of Free productions they can own. */
	PREMIUM		("Premium");	// 3

	private final String heading;

	MemberType(String head) {
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
		return this.toString();
	}

}
