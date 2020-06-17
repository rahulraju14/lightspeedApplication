/**
 * File: ExistencePolicy.java
 */
package com.lightspeedeps.type;

/**
 * An enumeration which can specify the policy regarding the existence of some
 * object or attribute. The policy has three settings: Allow, Require, or
 * Forbid. This is currently used to determine the policy for attachments to a
 * Form I-9 for a given Production. LS-1548
 */
public enum ExistencePolicy {
	ALLOW		("Allowed"),
	REQUIRE		("Required"),
	FORBID		("Forbidden"),
	;

	private final String heading;

	private ExistencePolicy(String head) {
		heading = head;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
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
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/**
	 * @return True if setting is "Forbidden"; this is a bit cleaner in JSP than doing compare.
	 */
	public boolean isForbidden() {
		return this == FORBID;
	}

}
