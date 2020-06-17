/*	File Name:	StudioType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the Production object, indicating if the studio shooting
 * this production is a major or an indie. This is used to filter the
 * sub-categories of production type and, possibly, available contracts or
 * occupations.
 */
public enum StudioType {
	IN		("Independent"),
	MJ		("Major"),
	;

	private final String heading;

	StudioType(String head) {
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
