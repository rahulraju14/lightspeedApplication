//	File Name:	PointOfInterestType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in {@link com.lightspeedeps.model.PointOfInterest}.
 */
public enum PointOfInterestType {
	COPY_SERVICE,
	GAS_STATION,
	HOSPITAL,
	OTHER,
	PARKING,
	RESTAURANT;

	public final static String[] headings = {
		"Copying Service",
		"Gas Station",
		"Hospital",
		"Other",
		"Parking",
		"Restaurant"
		};

	@Override
	public String toString() {
		return headings[this.ordinal()];
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
