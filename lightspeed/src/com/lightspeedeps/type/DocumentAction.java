/*	File Name:	DocumentAction.java */
package com.lightspeedeps.type;

/**
 * An enumeration used in the Approval Path tab in the Onboarding tab to indicate what type approval process the corresponding
 * document has to undergo.
 */
public enum DocumentAction {

	/** Just receive the document; no action required. */
	RCV ("No Action Required"),

	/** Read the document - open/view it. */
	RD ("Read"),

	/** Acknowledge the document, probably via button & prompt. No
	 * password or PIN required (not an e-signature). */
	ACK ("Read and Acknowledge"),

	/** Submit the document, probably via button & prompt. No password or PIN
	 * required (not an e-signature). Assumes that employee will be entering some
	 * data. For example, a change-of-address form. */
	SUBMIT ("Edit and Submit"),

	/** The employee must provide an e-signature of the document. */
	SIGN ("Sign and/or Initial on the Signature Line(s)"),
	;

	/** A description of the field; this may be used in the detailed Audit trail when displaying
	 * a  {@link com.lightspeedeps.model.ApprovalPath ApprovalPath} applied as part of a
	 * {@link com.lightspeedeps.model.ContractRule ContractRule}. */
	private final String description;

	/**
	 * The only constructor.
	 *
	 * @param desc See {@link #description}
	 */
	DocumentAction(String desc) {
		description = desc;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return description;
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

	/** True if Document can be submitted. */
	public boolean isDocSubmitable() {
		return (this == SUBMIT || this == SIGN);
	}

	/** True if Document is read only. */
	public boolean isReadOnly() {
		return (this == RCV || this == RD);
	}

}
