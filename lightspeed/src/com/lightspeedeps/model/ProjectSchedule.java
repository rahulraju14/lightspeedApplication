package com.lightspeedeps.model;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.lightspeedeps.util.common.CalendarUtils;

/**
 * ProjectSchedule entity.
 *
 * Much of the information implied by this object is available via methods
 * on the ScheduleUtils class.
 */
@Entity
@Table(name = "project_schedule")
public class ProjectSchedule extends PersistentObject<ProjectSchedule> implements Cloneable {

	// Fields
	private static final long serialVersionUID = -4351439140738713406L;

	/** The date (with no time) of the project's first shooting day.  setStartDate() clears
	 * the time-of-day portion of the timestamp (sets it to 12:00am). */
	private Date startDate;

	/** A string representing which days of the week are "normal" days off.  The string
	 * is always seven characters long, with the first character representing the status
	 * of Sunday, the next one Monday, and so on.  If the character is "1", the day is
	 * a day off (non-working); otherwise it will be "0", representing a working day.
	 * Note that the isDayOff(int) method provides a simple way to test a given day's
	 * status. */
	private String daysOff = "1000001";

	/** The date on which the "daysOff" setting was last changed.  This is used to
	 * determine how days off are calculated going forward and backward on the calendar. */
	private Date lastDaysOffChange;

	private Set<DateEvent> dateEvents = new HashSet<>(0);

	/** The Unit associated with this ProjectSchedule.  Each Unit has one and only one
	 * ProjectSchedule. */
	private Unit unit;

	// Constructors

	/** default constructor */
	public ProjectSchedule() {
		setStartDate(new Date());
	}

	/** full constructor */
/*	public ProjectSchedule(Date startDate, String daysOff,
			Set<DateEvent> dateEvents, Project project) {
		this.startDate = startDate;
		this.daysOff = daysOff;
		this.dateEvents = dateEvents;
		this.project = project;
	}
*/
	// Property accessors

	/** See {@link #startDate} */
	@Temporal(TemporalType.DATE) // just want date, not time of day
	@Column(name = "Start_Date", length = 0, nullable = false)
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date pDate) {
		Calendar cal = CalendarUtils.getInstance(pDate);
		CalendarUtils.setStartOfDay(cal);
		startDate = cal.getTime();
	}

	/** See {@link #daysOff} */
	@Column(name = "Days_Off", nullable = false, length = 7)
	public String getDaysOff() {
		return daysOff;
	}
	/** See {@link #daysOff} */
	public void setDaysOff(String daysOff) {
		this.daysOff = daysOff;
	}

	/**
	 * Determine if the specified weekdaynumber (1=Sunday, 2=Monday, etc.) is a day off on the
	 * current schedule. (The weekday numbers used match those given by the Calendar.get() function;
	 * note that the values given by the (deprecated) Date.getDay() function have origin zero.)
	 *
	 * @param weekdaynumber
	 * @return True iff the day is a day off.
	 */
	public boolean isDayOff(int weekdaynumber) {
		if (getDaysOff() != null && getDaysOff().length() >= weekdaynumber &&
				getDaysOff().charAt(weekdaynumber-1) == '1') {
			return true;
		}
		return false;
	}

	/** See {@link #lastDaysOffChange}. */
	@Temporal(TemporalType.DATE) // just want date, not time of day
	@Column(name = "Last_Days_Off_Change", length = 0)
	public Date getLastDaysOffChange() {
		return lastDaysOffChange;
	}
	/** See {@link #lastDaysOffChange}. */
	public void setLastDaysOffChange(Date lastDaysOffChange) {
		this.lastDaysOffChange = lastDaysOffChange;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "projectSchedule") // Changed to Lazy rev 3.0.4801
	public Set<DateEvent> getDateEvents() {
		return dateEvents;
	}
	public void setDateEvents(Set<DateEvent> dateEvents) {
		this.dateEvents = dateEvents;
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "projectSchedule")
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void merge(ProjectSchedule ps) {
		setDateEvents(ps.getDateEvents());
		for (DateEvent de : getDateEvents()) {
			de.setProjectSchedule(this);
		}
		setDaysOff(ps.getDaysOff());
		setLastDaysOffChange(ps.getLastDaysOffChange());
		setStartDate(ps.getStartDate());
	}

}