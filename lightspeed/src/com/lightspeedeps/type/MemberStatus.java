//	File Name:	MemberStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Contact class to indicate the status
 * of a Contact's membership within a Production.
 */
public enum MemberStatus {
	PENDING		("Pending", true),
	ACCEPTED	("Accepted", true),
	DECLINED	("Declined", false),
	NO_ROLES	("No Roles", true),	// active member with no roles in production
	NO_ACCESS	("No Access", true), // active member without "Production access" privilege
	BLOCKED		("Blocked", false),
	DELETED		("Deleted", false);

	/** Mixed-case label for UI purposes. */
	private String heading;
	/** Contacts with active status are included in Cast&Crew displays. */
	private boolean active;

	private MemberStatus(String head, boolean pActive) {
		heading = head;
		active = pActive;
	}

	/** @return True iff this status is considered "active", typically
	 * meaning the Contact can access production information. */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return A string containing the names of the enumerated values that
	 * are considered "active" members, which can be used in SQL queries. This
	 * will exclude members that are marked Declined, Deleted, or Blocked.
	 */
	public static String sqlActiveList() {
		return "('" + PENDING.name() + "','" + ACCEPTED.name()
				+ "','" + NO_ACCESS.name() + "','" + NO_ROLES.name() + "')" ;
	}

	/**
	 * @return A string containing the names of the enumerated values that
	 * are considered "inactive" members, which can be used in SQL queries.
	 * This should includes those status values for which we want the entry
	 * to appear in the User's "inactive" list in My Productions.
	 * This includes Blocked entries, but not Deleted or Declined entries.
	 */
	public static String sqlInactiveList() {
		return "('" + BLOCKED.name() + "')" ;
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
