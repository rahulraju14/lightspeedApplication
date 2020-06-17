/**
 * File: TimecardRow.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.WeeklyTimecard;

/**
 * This is a superclass of the wrapper classes {@link TimecardEntry} and
 * {@link TimesheetEntry}. It contains information related to a specific
 * {@link WeeklyTimecard} object, and includes a reference to that object. It is
 * not related to any database table (it is not persisted).
 */
public class TimecardRow implements Serializable {
	/** */
	private static final long serialVersionUID = - 8955281305964080097L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimecardRow.class);

	/** The week-ending date, copied from the WeeklyTimecard. */
	private Date date;

	/** The crew-person's name, in "last, first" style, created from
	 * the name fields in the WeeklyTimecard. */
	private String name;

	/** The crew-person's role, copied from the WeeklyTimecard. */
	private String occupation;

	/** The related time card, if any. */
	private WeeklyTimecard weeklyTc;

	/** Used to track the check mark setting in the displayed list. */
	private boolean checked;

	/** Used to keep track of the currently selected row */
	private boolean selected;

	public TimecardRow() {
		// default constructor
	}

	public TimecardRow(WeeklyTimecard wtc) {
		weeklyTc = wtc;
		name = wtc.getLastName() + ", " + wtc.getFirstName();
		date = wtc.getEndDate();
		occupation = wtc.getOccupation();
	}

	/** See {@link #date}. */
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #occupation}. */
	public String getOccupation() {
		return occupation;
	}
	/** See {@link #occupation}. */
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #weeklyTc}. */
	public WeeklyTimecard getWeeklyTc() {
		return weeklyTc;
	}
	/** See {@link #weeklyTc}. */
	public void setWeeklyTc(WeeklyTimecard weeklyTc) {
		this.weeklyTc = weeklyTc;
	}

	/** See {@link #checked}. */
	public boolean getChecked() {
		return checked;
	}
	/** See {@link #checked}. */
	public void setChecked(boolean selected) {
		checked = selected;
	}

	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}

	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getWeeklyTc() == null) ? 0 : getWeeklyTc().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		boolean b = true;
		if (this != o) {
			b = false;
			try {
				TimecardRow c = (TimecardRow)o;
				if (c != null) {
					if (getWeeklyTc() != null && c.getWeeklyTc() != null) {
						b = getWeeklyTc().equals(c.getWeeklyTc());
					}
				}
			}
			catch(Exception e) {
			}
		}
		return b;
	}

	/**
	 * Compare the timecard within the given TimecardRow.
	 * @param o Object to be compared to.
	 * @return Standard -1/0/1 compareTo values.
	 */
	public int compareTimecard(TimecardRow o) {
		if (getWeeklyTc() != null) {
			if (o.getWeeklyTc() != null) {
				return getWeeklyTc().compareTo(o.getWeeklyTc());
			}
			else {
				return 1;
			}
		}
		else if (o.getWeeklyTc() != null) {
			return -1;
		}
		return 0; // both wtc's are null
	}

}
