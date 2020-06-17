//	File Name:	WorkdayType.java
package com.lightspeedeps.type;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration used for Days-out-of-Days (DooD) tracking, used in the calendar,
 * scheduling, and other areas.
 */
public enum WorkdayType {
	/** A "normal" working day.  Note that there are a number of more specific values that
	 * should be used when appropriate, such as START, FINISH, etc. */
	WORK		("W", "Work", true),
	OTHER_TRAVEL("T", "Travel", false),
	COMPANY_TRAVEL("T", "Travel", false),
	/** This is a non-working day. */
	HOLIDAY		("L", "Holiday", false),
	/** This is typically used for Crew members on a production shooting day when
	 * the particular member is not required on set, but is available if necessary. */
	ON_CALL		("C", "On Call", false),
	/** Any day preceding the project start day (first day of shooting). */
	PREP		("P", "Prep", false),
	WRAP		("R", "Wrap", false),
	/** A non-working day.  This may be before the START date, after the FINISH date, or
	 * in between WORK dates (if HOLD does not apply).  Any date which is on one of a
	 * project's normal "days off" should have this status. */
	OFF			(" ", "Off", false),
	/** This is applied to individual elements during DooD information creation and
	 * indicates that it is a normal work day, but this element is not scheduled to
	 * be in use (working) on that day.
	 */
	NOT_NEEDED	(" ", "Not needed", false),
	/** The first date an element is used in a project */
	START		("SW", "Start", true),
	/** For a single work day, that date is both START and FINISH (WORK is implied) */
	START_FINISH("SWF", "Start/Finish", true),
	/** This status is for the last day of work preceding a DROP period, when this is
	 * also the first work day for the element. */
	START_DROP	("SWD", "Start/Drop", true),
	START_TRAVEL("ST", "Start/Travel", false),
	/** This status is for the last day of work preceding a DROP period. */
	DROP		("WD", "Work/Drop", true),
	/** This status is for the first day of work following a DROP period.  Note that PICKUP_DROP
	 * or PICKUP_FINISH should be used if they are more accurate. */
	PICKUP		("PW", "Work/Pickup", true),
	/** This status indicates an element is used for a single day in between two DROP periods. */
	PICKUP_DROP	("PWD", "Pickup/Drop", true),
	/** The last date an element is used in a project, and it was previously DROP'd. */
	PICKUP_FINISH("PWF", "Pickup/Finish", true),
	/** The last date an element is used in a project.  Use START_FINISH or
	 * PICKUP_FINISH if one of those other status values is more accurate. */
	FINISH		("WF", "Work/Finish", true),
	/** A date in between two WORK days (or periods) when an actor is in hold status,
	 * that is, they have not been DROP'd. */
	HOLD		("H", "Hold", false),
	/** A day that is off because it is (or was) a "normal weekly day off".  This value
	 * is only used in saved DateEvent's.  ScheduleUtils methods change it to "OFF" before
	 * returning it. */
	STANDARD_OFF ("", "Off", false);

	/** The string used on reports (Call Sheet, Time Sheet, DooD) to represent the
	 * work-day status. */
	String status;
	/** The string used in notification messages or other more user-friendly areas. */
	String label;
	/** True iff this is a work day. */
	boolean work;

	// These used to be used to compute the 'status' and 'work' values at run-time.
//	private static EnumSet<WorkdayType> workSet = EnumSet.of(WORK, START, START_FINISH, START_DROP, DROP,
//			PICKUP, PICKUP_DROP, PICKUP_FINISH, FINISH);
//	private static EnumSet<WorkdayType> startSet = EnumSet.of(START, START_FINISH, START_DROP);
//	private static EnumSet<WorkdayType> dropSet = EnumSet.of(START_DROP, DROP, PICKUP_DROP);
//	private static EnumSet<WorkdayType> finishSet = EnumSet.of(START_FINISH, PICKUP_FINISH, FINISH);
//	private static EnumSet<WorkdayType> pickupSet = EnumSet.of(PICKUP, PICKUP_DROP, PICKUP_FINISH);

	private final static Map<String,WorkdayType> statusMap = new HashMap<String,WorkdayType>();

	static {
		for (WorkdayType wt : WorkdayType.values()) {
			statusMap.put(wt.status, wt);
		}
	}

	WorkdayType(String stat, String name, boolean wk) {
		status = stat;
		work = wk;
		label = name;
	}

	public boolean isWork() {
		return work;
	}

	/**
	 * Returns the string used to represent this workdayType in a Day-out-of-Days
	 * report, in a Call Sheet cast status, or in the Time Sheet day-type field.
	 */
	public String asWorkStatus() {
		return status;
	}

	/**
	 * Same as "asWorkStatus", renamed to match bean style getProperty.
	 */
	public String getWorkStatus() {
		return asWorkStatus();
	}

	/** See {@link #label}. */
	public String getLabel() {
		return label;
	}

	/**
	 * Convert a "work status" string back to an enumerated value.  This
	 * is the inverse of the "asWorkStatus" method.
	 * @param s The string to convert.
	 * @return The equivalent WorkdayType.
	 */
	public static WorkdayType toValue(String s) {
		WorkdayType type = statusMap.get(s);
		if (type == null) {
			type = OFF;
		}
		return type;
	}

	/* SQL version of "getWorkStatus()":
	case time_card.day_type
		when 'WORK' then 'W'
		when 'OTHER_TRAVEL' then 'T'
		when 'COMPANY_TRAVEL' then 'T'
		when 'HOLIDAY' then 'Y' // or 'L'?
		when 'OFF' then ' '
		when 'START' then 'SW'
		when 'START_FINISH' then 'SWF'
		when 'START_DROP' then 'SWD'
		when 'START_TRAVEL' then 'ST'
		when 'DROP' then 'WD'
		when 'PICKUP' then 'PW'
		when 'PICKUP_DROP' then 'PWD'
		when 'PICKUP_FINISH' then 'PWF'
		when 'FINISH' then 'WF'
		when 'HOLD' then 'H'
	 	else '?'
	 end
	 */
}
