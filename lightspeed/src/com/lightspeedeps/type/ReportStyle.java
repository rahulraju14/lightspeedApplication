//	File Name:	ReportStyle.java
package com.lightspeedeps.type;

import javax.faces.model.SelectItem;

/**
 * An enumeration used in various print beans and utilities to indicate a style
 * of report to be generated. Currently it only reflects styles of timecard
 * reports, although that could change in the future.
 */
public enum ReportStyle {
	/** Full Timecard report */
	TC_FULL 	("1. Full Timecard"),
	/** Timecard report with Job Table(s) */
	TC_JOBS 	("2. Job Breakdown"),
	/** Simple Timecard report */
	TC_SIMPLE 	("3. Simple Timecard"),
	N_A 		("None"), // this marks the end of those entries included in "normal" drop-down lists
	/** Commercial Timecard report */
	TC_AICP 	("Commercial Timecard"),
	/** Commercial Timecard report for TEAM internal use only */
	TC_AICP_TEAM ("Commercial Timecard--TEAM internal"),
	/** ModelTimecard report */
	TC_MODEL("Model Timecard"),
	/** Payroll report with paid hours and cost */
	TC_PAY_HOURS 	 ("1. Pay Hours/Cost"),
	/** Payroll report with Pay Breakdown line items */
	TC_PAY_BREAKDOWN ("2. Pay Breakdown Summary"),
	/** Timesheet report, used for Tours productions */
	TIMESHEET		("Timesheet (Tours)"),
	/** Used for Tours Tax Wage Allocation forms */
	ALLOCATION		("Wage and Tax Allocation"),
	;

	/** The descriptive name to display (e.g., in a drop-down list). */
	private final String heading;

	ReportStyle(String head) {
		heading = head;
	}

	/** Returns the "pretty" printable version of this enumerated type.
	 * <p>
	 * See {@link #heading}. */
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

	/**
	 * @return a SelectItem instance containing this enum value and its standard
	 *         label.
	 */
	public SelectItem getSelectItem() {
		return new SelectItem(name(), getLabel());
	}

}
