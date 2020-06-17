//	File Name:	DayType.java
package com.lightspeedeps.type;

import java.math.BigDecimal;

import com.lightspeedeps.util.app.Constants;

/**
 * An enumeration used in DailyTime (timecard system). The order of the values
 * determines the order in the drop-down lists, for example, on the Basic Timecard page.
 * <p>
 * The enumeration value itself (a 2-letter code) is stored in the database, but
 * never displayed to the user. The short label (up to 7 letters) is used in the
 * drop-down list in the Full Timecard page. The long label is currently not
 * used, except for documentation purposes in this file.
 * <p>
 * Each entry has a boolean 'workDay' value. This is currently used for the
 * purpose of the DPR comparison report. If the user chooses to
 * "exclude non-working days", then days with DayType where workDay is false
 * will be ignored when determining if there was a discrepancy or not.
 * <p>
 * Each entry can have an optional array of characters; when this
 * is specified, the individual characters may be displayed instead of the
 * input/output fields for Call time, Wrap, etc. This is currently used
 * in the DPR Comparison pane when no hours are reported for the day.
 */
public enum DayType {
	// The order of the values determines the order in the drop-down lists,
	// for example, on the Breakdown Edit page.

	// Parameters are:
	//		Label, 				Short label, PayCategory,  Default hours, Is work day, Banner[...............], Is Tours, Is Non-union, Is Hybrid
	/** Bereavement */
	BR	("Bereavement", 		"Bereave",	null,					null, false, 	new char[]{'B','E','R','E','A','V'}, false, false, false),
	/** Budgeted/Not Worked */
	BN	("Budgeted/Not Worked",	"BudNoWrk",	null,					null, false, 	new char[]{'B','U','D','G','E','T'}, false, false, false),
	/** Completion of Assignment */
	CO	("Completion of Assignment", "COA",	PayCategory.COMP_OF_ASGN,null, true, 	new char[]{'C','.','O','.','A','.'}, false, false, false),
	/** Dark Day */
	DD	("Dark Day", 			"DarkDay",	null,					null, false, 	new char[]{'D','A','R','K'}, false, false, false),
	/** Drug Test */
	DT	("Drug Test", 			"DrugTest",	PayCategory.DRUG_TEST,	null, false, 	new char[]{'D','R','G','T','S','T'}, false, false, false),	// was "ZERO"
	/** Guaranteed payment */
	GR	("Guarantee", 			"Guar",		null,					null,  true, 	new char[]{'G','U','A','R'}, false, false, false),
	/** Guaranteed 8 hours pay */
	G8	("Guar (8)", 			"Guar(8)",	null,	Constants.DECIMAL_EIGHT,true,	new char[]{'G','U','A','R'}, false, false, false),
	/** Guaranteed 10 hours pay */
	G1	("Guar (10)", 			"Guar(10)",	null,	Constants.DECIMAL_10,  true, 	new char[]{'G','U','A','R'}, false, false, false),
	/** Guaranteed 12 hours pay */
	G2	("Guar (12)", 			"Guar(12)",	null,	Constants.DECIMAL_12,  true, 	new char[]{'G','U','A','R'}, false, false, false),
	/** Hiatus Paid */
	AP	("Hiatus Paid", 		"HiatusPd",	PayCategory.HIATUS_PAID,BigDecimal.ZERO, false, new char[]{'H','I','A','T','U','S'}, false, false, false),
	/** Hiatus Unpaid */
	AU	("Hiatus Unpaid", 		"HiatUnPd",	null,					null, false, 	new char[]{'H','I','A','T','U','S'}, false, false, false),
	/** Hold */
	HO	("Hold", 				"Hold",		null,					null, false, 	new char[]{'H','O','L','D'}, false, false, false),
	/** Home on Own Accord */
	HA	("Home on Own Accord", 	"HOA",		null,					null, false, 	new char[]{'H','.','O','.','A','.'}, false, false, false),
	/** Holiday Paid */
	HP	("Holiday Paid", 		"HoliPaid",	PayCategory.HOLIDAY_PAID,null,false, 	new char[]{'H','O','L','D','A','Y'}, false, false, true),
	/** Holiday Unpaid */
	HU	("Holiday Unpaid", 		"HoliUnPd",	PayCategory.HOLIDAY_WKD,null, false, 	new char[]{'H','O','L','D','A','Y'}, false, false, false),
	/** Holiday Worked */
	HW	("Holiday Worked", 		"HoliWork",	PayCategory.HOLIDAY_WKD,null,  true, 	new char[]{'H','O','L','D','A','Y'}, false, false, false),
	/** Idle */
	ID	("Idle", 				"Idle",	PayCategory.IDLE_PAY,		null,  true, 	new char[]{'I','D','L','E'}, false, false, false),
	/** Idle (4) */
	I4	("Idle (4)", 			"Idle(4)",	PayCategory.IDLE_PAY,Constants.DECIMAL_FOUR, true, new char[]{'I','D','L','E'}, false, false, false),
	/** Idle (5.5) */
	I5	("Idle (5.5)", 			"Idle(5.5)",PayCategory.IDLE_PAY, Constants.DECIMAL_FIVE_FIVE, true, new char[]{'I','D','L','E'}, false, false, false),
	/** Idle (7) */
	I7	("Idle (7)", 			"Idle(7)",	PayCategory.IDLE_PAY,Constants.DECIMAL_SEVEN, true, new char[]{'I','D','L','E'}, false, false, false),
	/** "Idle (8) */
	I8	("Idle (8)", 			"Idle(8)",	PayCategory.IDLE_PAY,Constants.DECIMAL_EIGHT, true, new char[]{'I','D','L','E'}, false, false, false),
	/** Idle Paid */
	IP	("Idle Paid", 			"IdlePaid",	PayCategory.IDLE_PAY,	null,  true, 	new char[]{'I','D','L','E'}, false, false, false),
	/** Jury Paid */
	JP	("Jury Paid", 			"JuryPaid",	null,		 BigDecimal.ZERO, false, 	new char[]{'J','U','R','Y'}, false, true, false),
	/** Jury Unpaid */
	JU	("Jury Unpaid", 		"JuryUnPd",	null,					null, false, 	new char[]{'J','U','R','Y'}, false, false, false),
	/** Off */
	OF	("Off", 				"Off",		null,					null, false, 	new char[]{'O','F','F'}, false, false, false),
	/** Per Diem Only */
	PD	("Per Diem Only", 		"PerDiem",	null,					null,  true, 	null, false, false, false),
	/** Prep */
	PR	("Prep", 				"Prep",		null,					null,  true, 	new char[]{'P','R','E','P'}, false, true, false),
	/** PTO Paid */
	PO	("PTO Paid", 			"PTO Paid",	PayCategory.PERSNL,BigDecimal.ZERO,false,new char[]{'P','T','O','-','P','D'}, false, true, true),
	/** PTO Unpaid */
	PU	("PTO Unpaid", 			"PTO UnPd",	null,					null, false, 	new char[]{'P','T','O','-','N','P'}, false, false, false),
	/** Safety */
	SF	("Safety", 				"Safety",	PayCategory.SAFETY,		null, false, 	new char[]{'S','A','F','E','T','Y'}, false, false, false),	// was "ZERO"
	/** Sick Paid */
	SP	("Sick Paid", 			"SickPaid",	PayCategory.SICK_PAY,BigDecimal.ZERO, false, 	new char[]{'S','I','C','K','-','P'}, false, true, true),
	/** Sick Unpaid */
	SU	("Sick Unpaid", 		"SickUnPd",	null,					null, false, 	new char[]{'S','I','C','K','-','N'}, false, false, false),
	/** Pre-Timing */
	PT	("Pre-Timing", 			"Timing",	null,					null,  true, 	new char[]{'T','I','M','I','N','G'}, false, false, false),
	/** Travel, pay for 4 hours */
	T4	("Travel (4)", 			"Trav(4)",	PayCategory.TRAVEL,	Constants.DECIMAL_FOUR, false, new char[]{'T','R','A','V','E','L'}, false, false, false),
	/** Travel, pay for 7 hours */
	T7	("Travel (7)", 			"Trav(7)",	PayCategory.TRAVEL,	Constants.DECIMAL_SEVEN, false, new char[]{'T','R','A','V','E','L'}, false, false, false),
	/** Travel, pay for 8 hours */
	T8	("Travel (8)", 			"Trav(8)",	PayCategory.TRAVEL,	Constants.DECIMAL_EIGHT, false, new char[]{'T','R','A','V','E','L'}, false, false, false),
	/** Travel day */
	TR	("Travel", 				"Travel",	PayCategory.TRAVEL,		null, false, 	new char[]{'T','R','A','V','E','L'}, false, true, true), // was ZERO
	/** Travel Paid */
	TP	("Travel Paid", 		"TravPaid",	PayCategory.TRAVEL,		null, false, 	new char[]{'T','R','A','V','E','L'}, false, false, false),	// was "ZERO"
	/** Travel Unpaid */
	TU	("Travel Unpaid", 		"TravUnPd",	PayCategory.TRAVEL,		null, false, 	new char[]{'T','R','A','V','E','L'}, false, false, false),
	/** Travel then Work on same day */
	TW	("Travel/Work", 		"TravWork",	PayCategory.TRAVEL,		null,  true, 	new char[]{'T','R','V','W','R','K'}, false, false, false),	// was "ZERO"
	/** Vacation */
	VA	("Vacation", 			"Vacation",	PayCategory.VACATION,	null, false, 	new char[]{'V','A','C','A','T','N'}, false, true, true),
	/** Work */
	WK	("Work", 				"Work",		null,					null,  true, 	new char[]{'W','O','R','K','E','D'}, false, true, true),
	/** Work/Layoff - last day of work */
	WL	("Work/Layoff",			"WorkLayOf",null,					null,  true, 	new char[]{'W','O','R','K','E','D'}, false, false, false),
	/** Work then Travel on same day */
	WT	("Work/Travel",			"WorkTrav",	PayCategory.TRAVEL,		null,  true, 	new char[]{'W','R','K','T','R','V'}, false, false, false),	// was "ZERO"
	/** Worked Ex */
	WE	("Worked Ex", 			"WorkedEx",	null,					null,  true, 	new char[]{'W','O','R','K','E','D'}, false, false, false),
	/** "Wrap Paid */
	WP	("Wrap Paid", 			"WrapPaid",	PayCategory.WRAP_PAID,	null,  true, 	new char[]{'W','R','A','P'}, false, false, false),	// was "ZERO"

	// DayType "NA" and all following entries are omitted from the normal drop-down list;
	// they may be used in ContractRule fields.
	/** N/A */
	N_A  ("N/A",				"N/A",		null,					null, false, 	null, false, false, false),
	/** On Production - not for drop-down; used in Contract Rules */
	ON  ("On Production",		"OnProd",	null,					null,  true, 	null, false, false, false),
	/** Off Production - not for drop-down; used in Contract Rules */
	OP	("Off Production", 		"OffProd",	null,					null,  true, 	null, false, false, false),

	//		*** Hybrid or tours-only Day Types, not to be included in the "standard" list ***

	/** Overnight - Hybrid only - paid Hourly rate for reported hours, plus overnight flat rate. LS-2942 */
	OV	("Overnight", 			"Overnight",null,					null,  true, 	new char[]{'O','V','E','R','N','T'}, false, false, true),  // include OV in Hybrid drop-down

	//		*** Model Release (Print) Day Types, not to be included in the "standard" list ***

	/** Print session (shoot) = work */
	PS	("Print Session", 		"Session",	null,					null,  true, 	new char[]{'S','E','S','I','O','N'}, false, false, false),
	/** Wardrobe fitting (pays as prep) */
	WD	("Wardrobe Fitting", 	"Wardrobe",	null,					null,  true, 	new char[]{'W','R','D','R','O','B'}, false, false, false),

	// 		*** TOURS DAY TYPES *** updated for LS-1347

	/** Tours: None */
	NONE("--", 					"--",		null,					null,  false, 	null, true, false, false),
	/** Tours: Show */
	TSH	("Show(T)", 			"Show(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'S','H','O','W'}, true, false, false),
	/** Tours: Prep */
	TPR	("Prep(T)", 			"Prep(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'P','R','E','P'}, true, false, false),
	/** Tours:  Post */
	TPO	("Post(T)", 			"Post(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'P','O','S','T'}, true, false, false),
	/** Tours: Travel*/
	TTR	("Travel(T)", 			"Trav(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'T','R','A','V','E','L'}, true, false, false),
	/** Tours: Down (off) */
	TDO	("Down(T)", 			"Down(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'D','O','W','N'}, true, false, false),
	/** Home: Work */
	HOW	("Work(T)", 			"Work(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'W','O','R','K'}, true, false, false),
	/** Home Office Off */
	HOO	("Off(T)", 				"Off(T)",	null,Constants.DECIMAL_EIGHT,  true, 	new char[]{'O','F','F'}, true, false, false)
	;

	/** The 'pretty' text to use when displaying this value. */
	private final String label;

	/** This label is used in the drop-down boxes of Day Types, e.g., on timecards. */
	private final String shortLabel;

	/** If not null, this represents the PayCategory which should be used in the Pay Breakdown
	 * table for reporting hours on a day with this DayType. If null, the hours from a day
	 * with this DayType will be listed in the usual "n.nX Hours" category. */
	private final PayCategory category;

	/** True iff this is a "working day" (for purposes of DPR comparison, at least) */
	private final boolean workDay;

	/** The number of guaranteed work hours defined by this DayType. */
	private final BigDecimal hours;

	/** Text to be displayed in place of hours, as one character per cell in a timecard. */
	private final char[] banner;

	/** The array of characters to display in place of the call/meal/wrap times. */
//	private char[] dayText;

	/** True iff the production is Tours Production. */
	private final boolean tours;

	/** True for non-union timecards LS-2010 */
	private final boolean nonUnion;

	/** True if Hybrid timecard LS-2160 */
	private final boolean hybrid;

	DayType(String lab, String shortLab, PayCategory pc, BigDecimal hrs, boolean work, char[] text,
			boolean isTours, boolean isNonUnion, boolean isHybrid) {
		label = lab;
		shortLabel = shortLab;
		category = pc;
		workDay = work;
		hours = hrs;
		banner = text;
		tours = isTours;
		nonUnion = isNonUnion;
		hybrid = isHybrid;
	}

	/**
	 * Returns the enum type that matches the given string, or N_A if
	 * the string does not match any DayType values.
	 */
	public static DayType toValue(String str) {
		DayType type = N_A;
		try {
			type = DayType.valueOf(str);
		}
		catch (RuntimeException ex) {
			// 'str' doesn't match
		}
		return type;
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

	/**
	 * @return the value of {@link #shortLabel}. This is the "pretty" mixed-case
	 *         version of the enumerated value. This is the value
	 *         generally used in drop-down lists.
	 *         <p>
	 *         NOTE: This overrides the default toString(), which returns the
	 *         same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return shortLabel;
	}

	/** See {@link #category}. */
	public PayCategory getCategory() {
		return category;
	}

	/** See {@link #shortLabel}. */
	public String getShortLabel() {
		return shortLabel;
	}

	/**See {@link #hours}. */
	public BigDecimal getHours() {
		return hours;
	}

	/** See {@link #banner}. */
	public char[] getBanner() {
		return banner;
	}

	/**See {@link #workDay}. */
	public boolean getWorkDay() {
		return workDay;
	}

	public static String getSqlCase() {
		String sql = "case Day_Type ";
		for (DayType dt : values()) {
			sql += " when '" + dt.name() + "' then '" +
					(dt == OV ? "Ovrnite" : dt.shortLabel) + "' ";
					// special case for Overnight to fit in report column. LS-3011
		}
		sql += " else ' ' end as dayTypeName ";
		return sql;
	}

	/**
	 * @return True iff this DayType represents some sort of "Idle" day.
	 */
	public boolean isIdle() {
		boolean ret = false;
		switch (this) {
			case ID:
			case I4:
			case I5:
			case I7:
			case I8:
			case IP:
				ret = true;
				break;
			default:
				break;
		}
		return ret;
	}

	/**
	 * @return True iff this DayType represents a day on which the employee will
	 *         receive some payment even if they did not report any hours or
	 *         checked "worked".  This is currently used only for OnCall positions.
	 */
	public boolean isAlwaysPaid() {
		boolean ret = false;
		switch (this) {
			// 2.9.5086: only count types with explicit hours as 'always paid'.
			case AP:
//			case DT:
//			case ID:
			case I4:
			case I5:
			case I7:
			case I8:
//			case IP:
			case G1:
			case G2:
			case G8:
//			case GR:
//			case HP:
			case JP:
//			case PO:
//			case SF:
			case SP:
			case T4:
			case T7:
			case T8:
//			case TP:
			case TR: // always pay for on-call
//			case TW:
//			case WT:
				ret = true;
				break;
			default:
				break;
		}
		return ret;
	}

	/**
	 * @return True if this is one of the Holiday day types (paid, unpaid, or
	 *         worked).
	 */
	public boolean isHoliday() {
		return (this == HP || this == HW || this == HU);
	}

	/**
	 * @return True if this DayType specifies that it was a pure Travel day.
	 *         Excludes Work/Travel and Travel/Work.
	 */
	public boolean isTravel() {
		boolean ret = false;
		switch (this) {
			case T4:
			case T7:
			case T8:
			case TP:
			case TR:
			case TU:
				ret = true;
				break;
			default:
				break;
		}
		return ret;
	}

	/**
	 * @return True if this DayType specifies that Travel occurred during the
	 *         day (including Work/Travel and Travel/Work).
	 */
	public boolean includesTravel() {
		boolean ret = false;
		if (isTravel()) {
			ret = true;
		}
		else {
			switch (this) {
				case TW:
				case WT:
					ret = true;
					break;
				default:
					break;
			}
		}
		return ret;
	}

	/**
	 * @return True iff this DayType represents one of the "Work" day types --
	 *         used for controlling the display of "banner" text on the DPR
	 *         compare panel, and for some HTG decisions.
	 */
	public boolean isWork() {
		boolean ret = isWorkTime() || this == OV;
		return ret;
	}

	/**
	 * @return True iff this DayType represents one of the "Work" day types --
	 *         used for controlling the display of "banner" text on the DPR
	 *         compare panel, and for some HTG decisions, and requirements for
	 *         recording hours. This includes all working DayType's EXCEPT
	 *         "Overnight" {@link #OV}, because Overnight does not require hours
	 *         to be entered or the "Worked" box to be checked.
	 */
	public boolean isWorkTime() {
		boolean ret = this == WK || this == WL || this == WE;
		return ret;
	}

	/** True iff this is one of the "touring" Day Types such as Show or Down. */
	public boolean isTours() {
		return tours;
	}

	/** True iff this is one of the non union timecard. LS-2010 */
	public boolean isNonUnion() {
		return nonUnion;
	}

	/** True iff this is one of the Hybrid day types LS-2160 */
	public boolean isHybrid() {
		return hybrid;
	}

	/**
	 * @return True if this DayType is a touring type where the state should default
	 * to "Home state" (HM). LS-1492
	 */
	public boolean useHomeState() {
		return this == TPR || this == TPO || this == TTR || this == TDO || this == HOO;
	}

	/**
	 * @return True if, for Hybrid productions, this DayType requires the state
	 *         value to be filled in, and, if the country is US, also requires
	 *         the city value to be filled in, for the corresponding DailyTime
	 *         item. LS-2942
	 */
	public boolean isUsCityStateRequired() {
		return this == TSH || this == HOW || this == WK || this == OV;
	}

	/**
	 * @return True if this DayType requires the city value to be filled in
	 * for the corresponding DailyTime item in a Hybrid production, and the state
	 * value forced to "HM" (Home). LS-2942
	 */
	public boolean isCityRequiredStateHM() {
		return this == SP || this == HP || this == VA || this == PO;
	}

	/**
	 * @return True if, for this DayType, the 'state' value is always locked to
	 *         a particular value (typically "HM" or "OT"). Note that there are
	 *         other DayTypes for which this method returns false, but may still
	 *         have the state locked due to other values in the DailyTime
	 *         record, such as the country. LS-2331
	 */
	public boolean isStateDisabled() {
		return this == TPR || this == TPO || this == TTR || this == TDO || this == HOO
				|| isTravel() || this == SP || this == HP || this == VA || this == PO;
	}

	/** True iff this is one of the two Tours "Home" (Stationary) Day Types: Work or Off */
	public boolean isToursHomeZone() {
		return this == HOW || this == HOO;
	}

	/** True iff this is one of the Tours "touring" (not Stationary) Day Types */
	public boolean isTouringZone() {
		return (this != HOW && this != HOO);
	}

	/** True iff this is one of the Model release Form Day Types LS-4589 */
	public boolean isModelRelease() {
		return (this == PS || this == WD || this == ID || this == TR);
	}

}
