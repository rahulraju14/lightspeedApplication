/*	File Name:	HolidayType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used for managing Holidays. There is a enum value for each
 * "known" holiday -- at least all the ones that are either observed by a
 * union, or defined by a federal law.
 * The actual dates these occur are stored in the DateEvent table, and are
 * calculated using a spreadsheet (most recently, "LS eps Holiday Data v4.21.xlsx").
 * See DateEventDAO.findSystemHolidayType().
 */
public enum HolidayType {
	NWYR ("New Year's Day"), 			// Jan 1st
	MLK  ("Martin Luther King, Jr. Day"), // 3rd Monday in January
	GDHG ("Groundhog Day"), 			// Feb 2nd
	LINC ("Lincoln's birthday"), 		// Feb 12th
	VAL  ("Valentine's Day"), 			// Feb 14th
	PRES ("Presidents Day"), 			// 3rd Monday in February
	SPAT ("St Patrick's Day"), 			// Mar 17th
	MARD ("Mardi Gras"), 				// 47 days before Easter
	PASS ("Passover"), 					// 15 Nissan, calc'd from Rosh Hashanah
	PALM ("Palm Sunday"), 				// Sunday before (7 days before) Easter
	GDFR ("Good Friday"), 				// 2 days before Easter
	EAST ("Easter Sunday"), 			// MANUAL LOOKUP REQUIRED
	ESMN ("Easter Monday"), 			// Day after Easter Sunday
	MOTH ("Mother's Day"), 				// 2nd Sunday in May
	MEM  ("Memorial Day"), 				// Last Monday in May
	JNP  ("Journee nationale des Patriotes"), // Monday before May 25th (Canada)
	FLAG ("Flag Day"), 					// June 14th
	FNQ  ("Fete Nationale de Quebecois"), // June 24th (Canada)
	FATH ("Father's Day"), 				// 3rd Sunday in June
	CADY ("Canada Day"), 				// Canada - July 1; or July 2 if July 1 is Sunday
	INDP ("Independence Day"), 			// July 4
	LABR ("Labor Day"), 				// first monday in September, both in US and Canada
	ROSH ("Rosh Hashanah"), 			// MANUAL LOOKUP REQUIRED
	YOMK ("Yom Kippur"), 				// Rosh Hahanah + 9
	COLM ("Columbus Day"), 				// 2nd Monday in October
	HALW ("Halloween"), 				// Oct 31st
	ELEC ("Election Day"), 				// Tues after 1st Mon in Nov
	VET  ("Veteran's Day"), 			// Nov 11th
	THKS ("Thanksgiving"), 				// 4th Thursday in November
	THKF ("Fri after Thanksgiving"), 	// 1 day after Thanksgiving
	HANU ("Hanukkah"), 					// 25 Kislev, calc'd from Rosh Hashanah
	XMEV ("Christmas Eve"), 			// Dec 24th
	XMAS ("Christmas"), 				// Dec 25th
	KWAN ("Kwanzaa"), 					// Dec 26 - Jan 1
	;

	private final String heading;

	HolidayType(String head) {
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
