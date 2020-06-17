package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.DayType;

@NamedQueries ({
	@NamedQuery(name=TimesheetDay.GET_TIMESHEET_DAY_ID_LIST_BY_TIMESHEET_ID, query = "select td.id from TimesheetDay td where td.timesheet.id=:timesheetId")
})

/**
 * Represents a day of the week for a tours employee
 */
@Entity
@Table (name = "timesheet_day")
public class TimesheetDay extends PersistentObject<TimesheetDay>  {

	private static final long serialVersionUID = 5744664286294191449L;

	public static final String GET_TIMESHEET_DAY_ID_LIST_BY_TIMESHEET_ID = "getTimesheetDayIdListByTimesheetId";

	/** Timesheet that is associated the this employee's day */
	private Timesheet timesheet;
	/** Date for this day */
	private Date date;
	/** Touring City where the employee is in on this day */
	private String touringCity;
	/** Touring State where the employee is in on this day if country is not international */
	private String touringState;
	/** Touring ISO code for the country that the employee is in on this day */
	private String touringCountryCode;
	/** Touring Day Type determines how much the employee will be paid on this day. */
	private DayType touringDayType;

	/** Home City where the employee is in on this day */
	private String homeCity;
	/** Home State where the employee is in on this day if country is not international */
	private String homeState;
	/** Home ISO code for the country that the employee is in on this day */
	private String homeCountryCode;
	/** Home Day Type determines how much the employee will be paid on this day. */
	private DayType homeDayType;
	
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

	/** See {@link #date}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date", length = 10)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #touringCity}. */
	@Column(name = "Touring_City", nullable = true, length = 50)
	public String getTouringCity() {
		return touringCity;
	}
	/** See {@link #touringCity}. */
	public void setTouringCity(String touringCity) {
		this.touringCity = touringCity;
	}

	/** See {@link #touringState}. */
	@Column(name = "Touring_State", nullable = true, length = 50)
	public String getTouringState() {
		return touringState;
	}
	/** See {@link #touringState}. */
	public void setTouringState(String touringState) {
		this.touringState = touringState;
	}

	/** See {@link #touringCountryCode}. */
	@Column(name = "Touring_Country_Code", nullable = true, length = 2)
	public String getTouringCountryCode() {
		return touringCountryCode;
	}
	/** See {@link #touringCountryCode}. */
	public void setTouringCountryCode(String touringCountryCode) {
		this.touringCountryCode = touringCountryCode;
	}
	
	/** See {@link #touringDayType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Touring_Day_Type" , nullable = true, length = 10)
	public DayType getTouringDayType() {
		return touringDayType;
	}
	/** See {@link #touringDayType}. */
	public void setTouringDayType(DayType touringDayType) {
		this.touringDayType = touringDayType;
	}
	
	/** See {@link #homeCity}. */
	@Column(name = "Home_City", nullable = true, length = 50)
	public String getHomeCity() {
		return homeCity;
	}
	/** See {@link #homeCity}. */
	public void setHomeCity(String homeCity) {
		this.homeCity = homeCity;
	}
	
	/** See {@link #homeState}. */
	@Column(name = "Home_State", nullable = true, length = 50)
	public String getHomeState() {
		return homeState;
	}
	/** See {@link #homeState}. */
	public void setHomeState(String homeState) {
		this.homeState = homeState;
	}
	
	/** See {@link #homeCountryCode}. */
	@Column(name = "Home_Country_Code", nullable = true, length = 2)
	public String getHomeCountryCode() {
		return homeCountryCode;
	}
	/** See {@link #homeCountryCode}. */
	public void setHomeCountryCode(String homeCountryCode) {
		this.homeCountryCode = homeCountryCode;
	}
	
	/** See {@link #homeDayType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Home_Day_Type" , nullable = true, length = 10)
	public DayType getHomeDayType() {
		return homeDayType;
	}
	/** See {@link #homeDayType}. */
	public void setHomeDayType(DayType homeDayType) {
		this.homeDayType = homeDayType;
	}
}
