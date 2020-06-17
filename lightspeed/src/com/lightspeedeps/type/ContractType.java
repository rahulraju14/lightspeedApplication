/*	File Name:	ContractType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the Contract object to distinguish between Commercial
 * and non-Commercial contracts, as well as segregate the various types of
 * Commercial contracts, e.g., independent vs AICP.
 */
public enum ContractType {
	AGTF	("Agreement-TV/Feature"),
	AGC		("Agreement-Commercial (All)"),
	AGCD	("Agreement-Commercial-DGA"),
	AGCI	("Agreement-Commercial-Indie"),
	AGCA	("Agreement-Commercial-AICP"),
	AGUD	("Agreement-UDA (Canada)"),
	;

	private final String heading;

	ContractType(String head) {
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

	public boolean isTvFeature() {
		return this == AGTF;
	}

	public boolean isCommercial() {
		return ! isTvFeature();
	}

}
