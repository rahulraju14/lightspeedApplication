/**
 * File: EmailType.java
 */
package com.lightspeedeps.type;

/**
 * Enum that defines the various types of emails that are sent
 * via the email micro-service API.  ESS-1471
 */
public enum EmailType {

	INVITE_CONTACT ("InviteContact"),
	INVITE_NEW_ACCOUNT ("InviteNewAccount"),
	;

	private String template;

	private EmailType (String type) {
		template = type;
	}

	/** See {@link #template}. */
	public String getTemplate() {
		return template;
	}
}
