/*	File Name:	DiscountType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in Coupon object.
 */
public enum DiscountType {
	PERCENT_OFF	("Percent Off"),
	AMOUNT_OFF	("Amount Off"),
	PRICE		("Specified Price"),
	FREE_MONTHS ("Free months"),
	PRE_PURCHASE("Pre-purchase"),
	;

	private final String heading;

	DiscountType(String head) {
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
		return this.toString();
	}

}
