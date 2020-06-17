package com.lightspeedeps.type;

/**
 * Enumeration used in {@link com.lightspeedeps.model.ApproverAudit} class.
 */
public enum ApproverAuditType {
	PROD			("Production Approver"),
	APP_GROUP		("Approver Group Approver"),
	PATH_ANCHOR		("Approval Path Anchor"),
	FINAL			("Final Approver");

	private final String heading;

	private ApproverAuditType(String head) {
		heading = head;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value.
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
	 * @return The "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}
}
