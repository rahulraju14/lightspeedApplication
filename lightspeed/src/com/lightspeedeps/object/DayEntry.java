package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.model.DailyTime;

/**
 * A class to hold matching DailyTime (from WeeklyTimecard) and DailyHour (from
 * DPR data) entries, for display in the weekly detail table on the timecard
 * Approval Dashboard page.
 */
public class DayEntry implements Serializable {

	/** */
	private static final long serialVersionUID = - 4680948980071616516L;

	private DailyTime dailyTime;

	private DailyHours dailyHours;

	public DayEntry(DailyTime dt, DailyHours hours) {
		dailyTime = dt;
		dailyHours = hours;
	}


	/** See {@link #dailyTime}. */
	public DailyTime getDailyTime() {
		return dailyTime;
	}
	/** See {@link #dailyTime}. */
	public void setDailyTime(DailyTime dailyTime) {
		this.dailyTime = dailyTime;
	}

	/** See {@link #dailyHours}. */
	public DailyHours getDailyHours() {
		return dailyHours;
	}
	/** See {@link #dailyHours}. */
	public void setDailyHours(DailyHours dailyHours) {
		this.dailyHours = dailyHours;
	}

}