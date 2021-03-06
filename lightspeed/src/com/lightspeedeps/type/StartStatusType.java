package com.lightspeedeps.type;

/**
 * Enumeration used in {@link com.lightspeedeps.web.onboard.StatusListBean} class
 */
public enum StartStatusType {
	EMPLOYEE_STATUS ("Employee Status"),
	DOCUMENT_STATUS ("Document Status"),
	EMPLOYEE_DETAIL ("Employee Detail");

	private final String heading;

	StartStatusType(String head) {
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
		return toString();
	}
}
