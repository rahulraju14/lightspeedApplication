//	File Name:	AccessStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Production and Project classes to indicate the status
 * of the object, in particular whether it is "Active" or not.
 */
public enum AccessStatus {
	/** No longer used; originally for Production, for period
	 * between user selection & payment receipt. */
//	PENDING		("Pending"),

	/** Normal status. */
	ACTIVE		("Active"),

	/** No changes allowed to data; used for Projects. */
	READ_ONLY	("Read-Only"),

	/** Deactivated ('archived'); prevents data changes, same as read-only. The production
	 * owner may Delete a Production in this state. */
	ARCHIVED	("Archived"),

	/** Expired (from Active state); prevents data changes, same as read-only. The production
	 * owner may Delete a Production in this state. */
	EXPIRED_ACTIVE		("Expired"),

	/** Expired (from Archived state); prevents data changes, same as read-only. The production
	 * owner cannot Delete a Production in this state. */
	EXPIRED_ARCHIVED	("Expired(r)"),

	/** "Deleted" but in database; no access allowed. */
	DELETED		("Deleted"),

	/** No UI to set at this time; some Project-related code respects this setting. */
	OFFLINE		("Archived");

	private String heading;

	private AccessStatus(String head) {
		heading = head;
	}

	/**
	 * @return True iff this status indicates the production is billable.
	 * Currently this means it is Active, Read-only, or Archived.
	 */
	public boolean isBillable() {
		return this == ACTIVE || this == READ_ONLY || this == ARCHIVED;
	}

	/**
	 * @return True iff the status indicates the production may be updated.
	 */
	public boolean isWritable() {
		return this == ACTIVE;
	}

	/**
	 * @return True iff a 'pending' Contact may accept or decline an invitation
	 * to a Production with this status.
	 */
	public boolean getAllowsAccept() {
		return this == ACTIVE || this == READ_ONLY || this == EXPIRED_ACTIVE;
	}

	/**
	 * @return True iff a production with this status may be written to (updated).
	 */
	public boolean getAllowsWrite() {
		return this == ACTIVE;
	}

	/**
	 * @return A string, meant to be used in SQL queries, containing the names
	 *         of the enumerated values that are considered "active" members.
	 */
	public static String sqlActiveList() {
		return "('" /*+ PENDING.name() + "','" */ + ACTIVE.name() + "')" ;
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
