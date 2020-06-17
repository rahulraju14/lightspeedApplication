//	File Name:	ScriptElementType.java
package com.lightspeedeps.type;

/**
 * Enumeration of element types used in {@link com.lightspeedeps.model.ScriptElement}
 * and {@link com.lightspeedeps.model.RealWorldElement}.
 */
public enum ScriptElementType {
	// The order here determines the order displayed in Scene List expansion boxes
	// and in drop-down lists.
	CHARACTER 			("Characters", "Cast"),
	EXTRA 				("Extras"),
	PROP 				("Props"),
	MAKEUP_HAIR 		("Makeup/Hair"),
	WARDROBE 			("Costumes"),
	SET_DECORATION 		("Set Dressing"),
	STUNT 				("Stunts"),
	SPECIAL_EFFECT 		("Special Effects"),
	VEHICLE 			("Vehicles"),
	LIVESTOCK			("Livestock"),
	ANIMAL 				("Animal Handler"),
	GREENERY			("Greenery"),
	MUSIC				("Music"),
	SOUND				("Sound"),
	EQUIPMENT 			("Special Equipment"),
	ADDITIONAL_LABOR	("Additional Labor"),
	OPTICAL_FX			("Optical FX"),
	MECHANICAL_FX		("Mechanical FX"),
	SECURITY			("Security"),
	MISC				("Misc"),
	N_A 				("N/A"),		// unknown / uninitialized
	LOCATION 			("Sets", "Locations");
	// Location is after N/A, so it is NOT included in the Scene List expansion

	public static final int ELEMENT_TABLE_SIZE = MISC.ordinal() + 1;
	// If # of element types changes, Breakdown page JSP and Script Comparison (expansion) JSP
	// must be changed, too.

	private final static String[] headings = new String[ScriptElementType.values().length];
	private final static String[] rwHeadings = new String[ScriptElementType.values().length];

	static {
		for (ScriptElementType se : ScriptElementType.values()) {
			headings[se.ordinal()] = se.heading;
			rwHeadings[se.ordinal()] = (se.rwHeading == null ? se.heading : se.rwHeading);
		}
	}

	private final String heading;
	private final String rwHeading;

	ScriptElementType(String head) {
		heading = head;
		rwHeading = null;
	}

	ScriptElementType(String head, String rwHead) {
		heading = head;
		rwHeading = rwHead;
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
	 * Returns a "pretty" mixed-case version of the enumerated value for
	 * RealWorldElement situations.
	 */
	public String toRwString() {
		if (rwHeading != null) {
			return rwHeading;
		}
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

	/**
	 * Returns the "pretty" printable version of this enumerated type, for RealWorldElement
	 * situations. It is the same as toString, but can be used from jsp pages since it follows the
	 * bean accessor (getter) naming convention.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getRwLabel() {
		return this.toRwString();
	}

	/**
	 * Get the special labels used for the DooD and Element report page
	 * check-boxes.
	 *
	 * @return For items with separate Real-World and Script Element headings,
	 *         it generates a label containing both headings, separated by a
	 *         slash, e.g., "Characters/Cast". For all other items it simply
	 *         returns the normal label.
	 */
	public String getDblLabel() {
		if (rwHeading != null) {
			return heading + "/" + rwHeading;
		}
		return heading;
	}

}
