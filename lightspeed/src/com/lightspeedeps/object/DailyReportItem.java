/**
 * File: DailyReportItem.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;

import com.lightspeedeps.type.ReportStatus;

/**
 * A class for references to daily report objects such as Callsheet,
 * Dpr, and ExhibitG, for purposes of displaying in a list without
 * the overhead of holding numerous entire reports in memory.
 */
public class DailyReportItem extends Item implements Serializable {
	/** */
	private static final long serialVersionUID = 3658372326249666032L;
	/** The date this report applies to; the time of day should be 12:00am */
	private Date date;
	/** The date & time this report was last saved. */
	private Date lastModified;
	/** The current status of the report. */
	private ReportStatus status;
	/** The shooting day*/
	private int shootDay;
	/** For Call Sheet, the shooting call time. */
	private Date shootTime;
	/** For Call Sheet, the crew call time. */
	private Date crewTime;

	public DailyReportItem() {
	}

	/**
	 * A useful constructor.
	 */
	public DailyReportItem(Integer pid, Date pDate, ReportStatus pStatus, Date modified, int shootingDay) {
		super(pid, (pDate == null ? "" : pDate.toString())); // just in case date is null; rev 2.1.4122
		date = pDate;
		status = pStatus;
		lastModified = modified;
		shootDay = shootingDay;
	}

	/**
	 * Constructor for Call Sheet references, which includes crew-call and shooting-call times.
	 */
	public DailyReportItem(Integer pid, Date pDate, ReportStatus pStatus, Date modified, int shootingDay,
			Date crew, Date calltime) {
		this(pid, pDate, pStatus, modified, shootingDay);
		crewTime = crew;
		shootTime = calltime;
	}

	/** See {@link #date}. */
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #lastModified}. */
	public Date getLastModified() {
		return lastModified;
	}
	/** See {@link #lastModified}. */
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	/** See {@link #status}. */
	public ReportStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(ReportStatus status) {
		this.status = status;
	}

	/** See {@link #shootDay}. */
	public int getShootDay() {
		return shootDay;
	}
	/** See {@link #shootDay}. */
	public void setShootDay(int shootDay) {
		this.shootDay = shootDay;
	}

	/** See {@link #shootTime}. */
	public Date getShootTime() {
		return shootTime;
	}
	/** See {@link #shootTime}. */
	public void setShootTime(Date shootTime) {
		this.shootTime = shootTime;
	}

	/** See {@link #crewTime}. */
	public Date getCrewTime() {
		return crewTime;
	}
	/** See {@link #crewTime}. */
	public void setCrewTime(Date crewTime) {
		this.crewTime = crewTime;
	}

}
