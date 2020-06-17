//	File Name:	WorkZone.java
package com.lightspeedeps.type;

/**
 * An enumeration used in DailyTime (time card system). The value indicates where
 * the employee worked, e.g., Studio versus Distant.
 * <p>
 * The order of the values determines the order in the drop-down lists, for
 * example, on the Full Timecard page.
 * <p>
 * The enumeration value itself (a 2-letter code) is stored in the database, but
 * never displayed to the user. The short label (up to 7 letters) is used in the
 * drop-down list in the Full Timecard page. The long label is sometimes used as
 * roll-over (title) text.
 */
public enum WorkZone {
	// The order of the values determines the order in the drop-down lists,
	// for example, on the Breakdown Edit page.

	// Parameters are: Label, Short label, "default hours", "is work day"

	/** Distant Location; for touring environment, used for "Touring staff" */
	DL	("Distant Location", 	"Distant",		"Dist"),
	/** In Studio Zone */
	ZN	("In Studio Zone",		"InZone",		"InZn"),
	/** Nearby/Bus to Loc */
	BL	("Nearby/Bus to Loc", 	"Nrby/Bus2",	"Bus2"),
	/** On-Stage; for touring environment, used for "STationary staff" */
	ST	("On-Stage", 			"Stage",		"Stg"),

	// DayType "NA" and all following entries are omitted from the normal drop-down list

	/** N/A - not for drop-down; used only in Contract Rules, meaning rule applies to all cases. */
	N_A  ("N/A",				"N/A",			"N/A"),
	/** Not BusTo - not for drop-down; used only in Contract Rules */
	NB	("NOT Bus to Loc", 		"Not Bus2",		""),
	/** Stage/Bus to - not for drop-down; used only in Contract Rules */
	SB	("Stage/Bus to", 		"Stg/Bus2",		""),
	/** Zone/Bus to - not for drop-down; used only in Contract Rules */
	ZB	("Zone/Bus to", 		"Zone/Bus2",	""),
	/** "generic" Studio Location -- not for drop-down; used only in Contract Rules */
	SL	("Studio Location", 	"Studio",		""),
	;

	/** The "pretty" mixed-case version of this enumerated type. This version is
	 * longer than {@link #shortLabel}, and is not currently used. */
	private String label;

	/** The mixed-case version of the enum value used in drop-down lists. */
	private String shortLabel;

	/** The mixed-case version of the enum value used in reports. */
	private String reportLabel;

	/** The array of characters to display in place of the call/meal/wrap times. */
//	private char[] dayText;

	WorkZone(String lab, String shortLab, String rptLabel) {
		label = lab;
		shortLabel = shortLab;
		reportLabel = rptLabel;
	}

	/**
	 * Returns the enum type that matches the given string, or ZN if
	 * the string does not match any DayType values.
	 */
	public static WorkZone toValue(String str) {
		WorkZone type = ZN;
		try {
			type = WorkZone.valueOf(str);
		}
		catch (RuntimeException ex) {
			// 'str' doesn't match
		}
		return type;
	}

	/**
	 * @return the value of {@link #shortLabel}. This is the "pretty" mixed-case
	 *         version of the enumerated value. This could be enhanced to use
	 *         the current locale setting for i18n purposes. This is the value
	 *         generally used in drop-down lists.
	 *         <p>
	 *         NOTE: This overrides the default toString(), which returns the
	 *         same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return shortLabel;
	}

	/**
	 * A method that allows JSP to get the 'name' of the enum
	 * as a String.
	 * @return The same as this.name().
	 */
	public String getName() {
		return name();
	}

	/** See {@link #label}. */
	public String getLabel() {
		return label;
	}

	/** See {@link #shortLabel}. */
	public String getShortLabel() {
		return shortLabel;
	}

	/**
	 * @return A String of SQL which may be used in a report query to change the
	 *         stored database values into a textual representation for reports.
	 */
	public static String getSqlCase() {
		String sql = "case Work_Zone ";
		for (WorkZone zone : values()) {
			if (zone == N_A) {
				break;
			}
			sql += " when '" + zone.name() + "' then '" + zone.reportLabel + "' ";
		}
		sql += " else ' ' end as workZoneName ";
		return sql;
	}

	/**
	 * @return True iff this DayType represents a local/in-town work zone, i.e.,
	 * Studio (but not just on-stage).
	 */
	public boolean isStudio() {
		return (this != DL);
	}

	/**
	 * @return True iff this DayType represents a distant work zone, i.e.,
	 * not Studio.
	 */
	public boolean isDistant() {
		return (this == DL);
	}

	/**
	 * @return True iff this DayType, within a Tours production, represents a
	 *         'home' (stationary) work zone (as opposed to a touring work zone).
	 */
	public boolean isHome() {
		return (this != DL);
	}

	/**
	 * @return True iff this DayType, within a Tours production, represents a
	 *         touring work zone (as opposed to Home work zone).
	 */
	public boolean isTouring() {
		return (this == DL);
	}

	/**
	 * @return the equivalent "area type", either DL or ZN, corresponding
	 * to this DayType.
	 */
	public WorkZone getAreaType() {
		if (this == DL) {
			return DL;
		}
		return ZN;
	}

}
