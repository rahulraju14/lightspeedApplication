/*	File Name:	RuleField.java */
package com.lightspeedeps.type;

import java.math.BigDecimal;

/**
 * An enumeration used in the {@link com.lightspeedeps.object.RuleFilter
 * RuleFilter} object to define the field whose value will be tested. Note that
 * the enumeration also defines, via the {@link #getClazz()} method, the
 * expected class (e.g., BigDecimal, Boolean) of the value to be compared.
 * <p>
 * Methods in RuleFilter, such as
 * {@link com.lightspeedeps.object.RuleFilter#getBooleanValue(RuleField, production, dailyTime, weeklyTimecard)
 * getBooleanValue()}, are responsible for finding the value that corresponds to
 * a particular RuleField enum in relation to the timecard that is being
 * processed.
 */
public enum RuleField {

	/** Use the value of the Location field (currently 'S'-Studio or 'D'-Distant.
	Used in 2.3, probably won't be used in 3.0 */
	LO ("Location", String.class, true, "Location type, either Studio or Distant"),

	/** Use the DayType field.
	Used in 2.3, probably won't be used in 3.0 */
	DT	("DayType", String.class, true, "Day Type"),

	// FIELDS BASED ON Production VALUES....

	/** Use the appropriate production attribute to select between "feature" vs "TV".
	Used in 2.3, probably won't be used in 3.0 */
	FT	("FeatureTV", String.class, false, "Production: Feature or TV"),

	/** The sequential day number, 1-7, where 1=1st day of producer's work week; this is
	 * different than the DayNum, which is the 'worked day number' (where Friday
	 * might be 5 and Sunday 6). */
	PDN	("ProdDayNum", Integer.class, true, "The production day number, 1-7, where 1=1st day of producer's work week; this is diffferent than the WorkedDayNum, that counts the days worked this week, where Friday might be 5 and Sunday 6."),

	/** Use the appropriate production attribute for a TV production to select
	 * between a "Pilot", "movie of the week" (or other "long form"), or a
	 * Series. Values allowed include combinations of 1 or 2 letter codes,
	 * LF=LongForm, P=Pilot, 1=1st season.  Examples: "P-1", "LF-P-1". */
	TP	("TvType", String.class, false, "TV Production: Pilot, Movie, or Series"),

//	/** Use the appropriate production attribute for a TV production to select
//	 * base on the length of a TV production. */
//	TL	("TvLength", "TV show length (min)"),

	/** Based on Production.studioType, true if  "Major", false if "Indie". */
	MJ	("MajorIndie", Boolean.class, false, "Production: Major or Indie"),

	/** True if the shooting schedule was shifted this week. */
	SH	("Shifted", Boolean.class, false, "True if the shooting schedule was shifted this week"),

	// FIELDS BASED ON VALUES IN THE CURRENT DailyTime...

	/** Compares the State Worked to a list of states, and returns true if the
	 * state worked is in the list. */
	AW	("AreaWorked", String.class, true, "Compare the State Worked to a list of states."),

	/** The employee's call time for the current day. */
	CL	("Call", BigDecimal.class, true, "The employee's call time for the current day."),

	/** The number of elapsed hours in the employee's work time for the current day. */
	EH	("HoursElapsed", BigDecimal.class, true, "The number of elapsed hours in the employee's work time"),

	/** Test if 'french hours' checked for the day (Y/N) */
	FR	("French", Boolean.class, true, "Test if 'french hours' checked for the day (Y/N)"),

	/** The number of hours worked (not elapsed) in the current day. */
	HW	("HoursWorked", BigDecimal.class, true, "The number of hours worked in the current day"),

	/** The day after the current work day is a holiday  (Y/N). */
	NXH	("NextDayHoliday", Boolean.class, true, "The day after the current work day is a holiday"),

	/** Phase of production selected for this day - Prep, Shoot, or Wrap. (Single character,
	 * 'P', 'S', or 'W'.) */
	PH	("Phase", String.class, true, "Phase of production selected for this day - Prep, Shoot, or Wrap."),

	/** Total number of hours off between last wrap and this day's call time; useful when
	 * a schedule has shifted to determine if minimum break was invaded. (Decimal value.) */
	TO	("TimeOff", BigDecimal.class, true, "Total number of hours off between last wrap and this day's call time; useful when a schedule has shifted to determine if minimum break was invaded."),

	/** Which day of the calendar week it is, 3 characters: Mon, Tue, Wed, Thu, Fri, Sat, Sun. */
	WD	("WeekDay", String.class, true, "Which day of the calendar week it is (Mon,Tue,Wed,Thu,Fri,Sat,Sun)"),

	/** The sequential day number, 1-7, where 1=1st day of producer's work week; this is
	 * different than the DayNum, which is the 'worked day number' (where Friday
	 * might be 5 and Sunday 6). */
	WDN	("WorkedDayNum", Integer.class, true, "The 'worked day' number, which is 1 for the first day worked by this employee in the current week, within the production week. (That is, Sunday could be 6, Monday is 0, and Tuesday is 1).  It is usually 0 if the day was not worked by the employee."),

	/** The employee's wrap time for the current day. */
	WR	("Wrap", BigDecimal.class, true, "The employee's wrap time for the current day."),


	// FIELDS BASED ON START FORM OR OTHER DOCUMENTS...

	/** From Start Form, indicator if employee is "additional staff" (true) or "regular staff" (false). */
	AD	("Additional", Boolean.class, false, "'Additional staff' (from Start Form) for Videotape agreement"),

	/** Employee type from Start Form, single character: 'H'(hourly), 'D'(daily), or 'W'(weekly). */
	ET	("EmpType", String.class, false, "Employee type from Start Form: 'H'(hourly), 'D'(daily), or 'W'(weekly)."),

	/** The employee's Start Form has the "exempt" box checked  (Y/N). */
	EX	("Exempt", Boolean.class, false, "The related Start Form has 'exempt' checked"),

	/** Which holiday it is; value will be a HolidayType enum name or null. */
	HO	("Holiday", String.class, true, "Which holiday it is; value will be a HolidayType enum name or null."),

	/** From Start Form, indicator if employee is "additional staff" (true) or "regular staff" (false). */
	NH	("NearbyHire", Boolean.class, false, "'Nearby Hire' (from Start Form) for ASA contract"),


	// FIELDS BASED ON OTHER TIMECARD VALUES...

	/** "Excess hours" worked - applicable to Local 817. Hours worked after the end of the
	 * "span" period. The span ends 5 days (120 hours) after the first call time in the work week. (Decimal value.) */
	EXHR	("ExcessHours", BigDecimal.class, false, "Number of hours worked beyond the end of the 'span'; span ends 5 days after 1st call time."),

	/** The current week has both Studio and Distant workdays  (Y/N). */
	MXW		("MixedWeek", Boolean.class, false, "The current week has both Studio and Distant workdays"),

	/** The current timecard's work week starts after the 1st production day, or ends early. (Y/N). */
	PW		("PartialWeek", Boolean.class, false, "The timecard's work week starts after the 1st production day, or ends early."),

	/** The number of elapsed hours in the employee's work time for the previous day. */
	PHE		("PriorHoursElapsed", BigDecimal.class, true, "The number of elapsed hours in the employee's work time on the previous day"),

	/** The number of hours the employee worked on the previous day. */
	PHW		("PriorHoursWorked", BigDecimal.class, true, "The number of hours the employee worked on the previous day"),

	/** The number of days the employee was paid for in the prior week (integer). */
	PWDP	("PriorWeekDaysPaid", Integer.class, false, "The number of days the employee was paid for in the prior week"),

	/** The zone worked the previous day, abbreviated, e.g., PZ=ST (for Stage). The value should match one of the
	 * WorkZone enum values. */
	PZ	("PriorZone", String.class, true, "The zone worked the previous day, abbreviated, e.g., PriorZone=ST (for Stage)"),

	/** The number of days the employee worked in the current week (integer). */
	WRKD	("DaysWorked", Integer.class, false, "The number of days the employee worked in the current week"),

	/** The number of consecutive weeks the employee worked prior to this week (integer). */
	WW	("WeeksWorked", Integer.class, false, "The number of consecutive weeks the employee worked prior to this week"),

	;

	/** A description of the field; this may be used in the detailed Audit trail when displaying
	 * a  {@link com.lightspeedeps.object.RuleFilter RuleFilter} applied as part of a
	 * {@link com.lightspeedeps.model.ContractRule ContractRule}. */
	private final String description;

	/** A 'name' of the field, longer than the enumeration name itself, but much shorter than
	 * the {@link #description}.  This is meant to be the text used in a "contract rules spreadsheet"
	 * when defining a restriction (filter). For example, a restriction might be "WeekDay=Fri",
	 * where 'WeekDay' is the WD.expression value.  Such a spreadsheet will normally have a lookup
	 * table for converting the expression text ('WeekDay') into the enumeration name ('WD'),
	 * so that the generated SQL contains the enumeration name as part of the
	 * {@link com.lightspeedeps.model.ContractRule#extraFilters} field. */
	private final String expression;

	/** The expected class of a value to which this field will be compared. The currently used
	 * values are Integer, BigDecimal, and String. */
	@SuppressWarnings("rawtypes")
	private final Class clazz;

	/** True iff this RuleField can only be evaluated for a specific day (DailyTime). For
	 * example, French or Call would be true, since they apply to a specific day's values,
	 * but Exempt or MajorIndie would be false, since their value does not depend on
	 * a specific day of the week. */
	private final boolean dailyOnly;

	/**
	 * The only constructor.
	 *
	 * @param expr See {@link #expression}
	 * @param clas See {@link #clazz}
	 * @param daily See {@link #dailyOnly}
	 * @param desc See {@link #description}
	 */
	@SuppressWarnings("rawtypes")
	RuleField(String expr, Class clas, boolean daily, String desc) {
		expression = expr;
		description = desc;
		clazz = clas;
		dailyOnly = daily;
	}

	/**See {@link #clazz}. */
	@SuppressWarnings("rawtypes")
	public Class getClazz() {
		return clazz;
	}

	/** See {@link #dailyOnly}. */
	public boolean getDailyOnly() {
		return dailyOnly;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return description;
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

	/**See {@link #expression}. */
	public String getExpression() {
		return expression;
	}

}
