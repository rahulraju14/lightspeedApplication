//	File Name:	OrderStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Production class to indicate the status
 * of the order for this production, i.e., has it been paid for or not.
 */
public enum OrderStatus {
	FREE		("Free"), 		// Free production (trial)
	PENDING		("Pending"), 	// selected from purchase screen, but no order received
	ORDERED		("Ordered"),	// Order received, not yet charged
	CHARGED		("Charged"),	// Charge submitted
	PAID		("Paid");		// Charge completed - item is paid for, or it was free.

	private String heading;

	private OrderStatus(String head) {
		heading = head;
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
