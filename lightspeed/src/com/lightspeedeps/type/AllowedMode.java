package com.lightspeedeps.type;

/**
 * The enumeration of settings for page-field-access keys which control whether
 * the access is granted, based on attributes of the current production. This
 * may be the production type, or a setting such as "Onboarding enabled".
 *
 * @see com.lightspeedeps.model.PageFieldAccess
 * @see com.lightspeedeps.web.login.AuthorizationBean
 */
public enum AllowedMode {
	/** Applies to all/any environments */
	ANY 		("Any"),

	/** Applies to Beta environment (set in web.xml) */
	BETA 		("Beta environment"),

	/** Onboarding enabled, any production type */
	ONBOARD		("Onboarding enabled"),

	/** Scripting/scheduling enabled, any production type; LS-1327 */
	SCRIPTS		("Scripting enabled"),

	/** Applies to Agency/Talent production type only */
	AGENCY 		("Agency/Talent"),

	/** Applies to Tours production type only */
	TOURS 		("Tours"),

	/** Applies to all production types EXCEPT Tours */
	NONTOURS	("Non-Tours"),
	;

	private final String label;

	AllowedMode(String head) {
		label = head;
	}

	/**
	 * Returns a "pretty" version of the enumerated value, for use in screen
	 * displays or reports.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return label;
	}

	/**
	 * Returns the "pretty" printable/display version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as it should be displayed on-screen or in reports.
	 */
	public String getLabel() {
		return this.toString();
	}

}
