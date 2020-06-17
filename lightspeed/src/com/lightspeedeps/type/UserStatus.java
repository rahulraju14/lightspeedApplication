//	File Name:	UserStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Contact class to indicate the status
 * of a Contact's membership within a Production.
 */
public enum UserStatus {
	PENDING		("Pending",    "Pnd", true),
	REGISTERED	("Registered", "Reg", true),
	DELETED		("Deleted",    "Del", false);

	private String heading;
	private String shortLabel;
	private boolean active;

	private UserStatus(String head, String shortLab, boolean pActive) {
		heading = head;
		shortLabel = shortLab;
		active = pActive;
	}

	/** @return True iff this status is considered "active", typically
	 * meaning the Contact can access production information. */
	public boolean isActive() {
		return active;
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

	/**
	 * Returns the short printable version of this enumerated type.
	 *
	 * @return The value of the enumerated type as short mixed-case text.
	 */
	public String getShortLabel() {
		return shortLabel;
	}

}
