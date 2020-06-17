//	File Name:	DayNightType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in Scene, specifying the part of the day the Scene
 * is expected to be shot, or to represent.
 */
public enum DayNightType {
	// The order of the values determines the order in the drop-down lists,
	// for example, on the Breakdown Edit page.
	DAY 		("D",  "Day",  		100,	null),
	NIGHT		("N",  "Night",  	200,	null),
	GOLDEN		("D-", "Golden",  	30,		DAY),
	OVERNIGHT	("N+", "Overnight", 250, 	NIGHT),
	N_A			("?",  "N/A",    	0,		DAY),
	/* others seen: */
	SUNRISE		("D-", "Sunrise",  	10,		DAY),
	DAWN		("D-", "Dawn",  	20,		DAY),
	MORNING		("D",  "Morning",   40,		DAY),
	AFTERNOON	("D",  "Afternoon", 110,	DAY),
	DUSK		("N-", "Dusk", 		150, 	NIGHT),
	SUNSET		("N-", "Sunset", 	160, 	NIGHT),
	TWILIGHT	("N-", "Twilight", 	170, 	NIGHT),
	EVENING		("N-", "Evening", 	190, 	NIGHT);
	/* Additional possibilities
	PRE-DAWN,
	EARLY MORNING,
	MID-DAY,
	LATE AFTERNOON,
	SAME TIME,
	CONTINUED,
	CONTINUOUS,
	 * */

	static { // set the filters we can't set via constructor
		DAY.filterType = DAY;
		NIGHT.filterType = NIGHT;
	}

	/** The 'pretty' text to use when displaying this value. */
	private String label;

	/** A very short string (typically one character, sometimes 2) to represent
	 * the value.  Used for the Stripboard display. */
	private String tinyLabel;

	/** A value that determines the sort order of each type in
	 * comparison to the others, when the user selects the
	 * Day/Night column as the sort key in the Stripboard Editor
	 * or the Stripboard auto-scheduler. */
	private int sortOrder;

	/** A value of either DAY or NIGHT, indicating whether this instance
	 * is considered Day or Night for purposes of the stripboard editor
	 * filter.*/
	private DayNightType filterType;

	private DayNightType(String tiny, String lab, int sort, DayNightType filter) {
		label = lab;
		tinyLabel = tiny;
		sortOrder = sort;
		filterType = filter;
	}

	/**
	 * Returns the enum type that matches the given string, or N_A if
	 * the string does not match any DayNightType values.
	 */
	public static DayNightType toValue(String str) {
		DayNightType dntype = N_A;
		try {
			dntype = DayNightType.valueOf(str);
		}
		catch (RuntimeException ex) {
			// 'str' doesn't match - return N_A value.
		}
		return dntype;
	}

	/** See {@link #label}. */
	public String getLabel() {
		return label;
	}

	/** See {@link #tinyLabel}. */
	public String getTinyLabel() {
		return tinyLabel;
	}

	/** See {@link #filterType}. */
	public DayNightType getFilterType() {
		return filterType;
	}

	/** See {@link #sortOrder}. */
	public int getSortOrder() {
		return sortOrder;
	}

}
