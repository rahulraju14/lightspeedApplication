package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A class used to record timed, and often signed, events, such as Submit and
 * Approve, that happen to a Timesheet object. Part of the Tours system.
 *
 * @see SignedEvent
 */
@Entity
@Table (name = "timesheet_event")
public class TimesheetEvent extends SignedEvent<TimesheetEvent> {

	private static final long serialVersionUID = -7237417487296983010L;

	private Timesheet timesheet;

	private Integer timecardCount;

	public TimesheetEvent() {
		super();
	}
	/** See {@link #timesheet}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Timesheet_Id")
	public Timesheet getTimesheet() {
		return timesheet;
	}
	/** See {@link #timesheet}. */
	public void setTimesheet(Timesheet timesheet) {
		this.timesheet = timesheet;
	}

	/** See {@link #timecardCount}. */
	@Column(name = "Timecard_Count", nullable = true)
	public Integer getTimecardCount() {
		return timecardCount;
	}
	/** See {@link #timecardCount}. */
	public void setTimecardCount(Integer timecardCount) {
		this.timecardCount = timecardCount;
	}

}
