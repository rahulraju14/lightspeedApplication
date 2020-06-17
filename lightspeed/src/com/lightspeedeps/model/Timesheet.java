package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.type.PayPeriodType;

@NamedQueries ({
	@NamedQuery(name=Timesheet.GET_TIMESHEET_BY_PROD_ID_END_DATE, query = "from Timesheet t where t.prodId =:prodId and t.endDate =:endDate")
})
@Entity
@Table (name = "timesheet")
public class Timesheet extends Approvable {

	private static final long serialVersionUID = 8486303192410020594L;

	public static final String GET_TIMESHEET_BY_PROD_ID_END_DATE = "getTimesheetByProdIdEndDate";

	private String prodId;

	/** Pay period type used to generate this timesheet.
	 * Possible values W (weekly), BW (bi-weekly), SM (semi-monthly), M (monthly)
	 */
	private PayPeriodType payPeriodType;

	/** The number of days this timesheet will cover. Based on the PayPeriodType selected
	 * Will be used to generate the correct number of days when creating the timesheet and
	 * the weekly timecard.
	 */
	private Integer numDays;

	/** The start date for the timesheet */
	private Date startDate;

	/** The end date for timesheet. */
	private Date endDate;

	/** The date when timesheet was sent. */
	private Date timeSent;

	/** The date when timesheet was last updated. */
	private Date lastUpdated;

	private List<TimesheetDay> timesheetDays = new ArrayList<>();

	/** List of Timesheet events for the corresponding Timesheet. */
	private List<TimesheetEvent> timesheetEvents = new ArrayList<>();

	/** Comments made by timesheet approver */
	private String comments;

	/** variable to save first category drop-down selected value */
	private PayCategory payCategory1;

	/** variable to save Second category drop-down selected value */
	private PayCategory payCategory2;

	/** variable to save third category drop-down selected value */
	private PayCategory payCategory3;

	/** variable to save fourth category drop-down selected value */
	private PayCategory payCategory4;

	/**
	 * Default constructor.
	 */
	public Timesheet() {
	}

	/** See {@link #prodId}. */
	@Column(name = "Prod_Id", nullable = true, length = 10)
	public String getProdId() {
		return prodId;
	}
	/** See {@link #prodId}. */
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	/** See {@link #endDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 10)
	public Date getEndDate() {
		return endDate;
	}
	/** See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #startDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", length = 30)
	public Date getStartDate() {
		return startDate;
	}

	/** See {@link #startDate}. */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/** See {@link #timeSent}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Sent", length = 0)
	public Date getTimeSent() {
		return timeSent;
	}
	/** See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	/** See {@link #lastUpdated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Updated", length = 0)
	public Date getLastUpdated() {
		return lastUpdated;
	}
	/** See {@link #lastUpdated}. */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/** See {@link #comments}. */
	@Column(name = "Comments", length = 2000)
	public String getComments() {
		return comments;
	}

	/** See {@link #comments}. */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** See {@link #timesheetDays}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "timesheet")
	@OrderBy("date")
	public List<TimesheetDay> getTimesheetDays() {
		return timesheetDays;
	}
	/** See {@link #timesheetDays}. */
	public void setTimesheetDays(List<TimesheetDay> timesheetDays) {
		this.timesheetDays = timesheetDays;
	}

	/** See {@link #timesheetEvents}. */
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "timesheet")
	@OrderBy("date")
	public List<TimesheetEvent> getTimesheetEvents() {
		return timesheetEvents;
	}
	/** See {@link #timesheetEvents}. */
	public void setTimesheetEvents(List<TimesheetEvent> timesheetEvents) {
		this.timesheetEvents = timesheetEvents;
	}

	@SuppressWarnings("rawtypes")
	@Override
	@Transient
	public List<? extends SignedEvent> getEvents() {
		return null;
	}

	@Override
	@Transient
	public Production getProduction() {
		return ProductionDAO.getInstance().findByProdId(getProdId());
	}

	@Override
	@Transient
	public Department getDepartment() {
		return null;
	}

	/** See {@link #payCategory1}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Category1", length = 100)
	public PayCategory getPayCategory1() {
		return payCategory1;
	}

	/** See {@link #payCategory1}. */
	public void setPayCategory1(PayCategory payCategory1) {
		this.payCategory1 = payCategory1;
	}

	/** See {@link #payCategory2}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Category2", length = 100)
	public PayCategory getPayCategory2() {
		return payCategory2;
	}

	/** See {@link #payCategory2}. */
	public void setPayCategory2(PayCategory payCategory2) {
		this.payCategory2 = payCategory2;
	}

	/** See {@link #payCategory3}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Category3", length = 100)
	public PayCategory getPayCategory3() {
		return payCategory3;
	}

	/** See {@link #payCategory3}. */
	public void setPayCategory3(PayCategory payCategory3) {
		this.payCategory3 = payCategory3;
	}

	/** See {@link #payCategory4}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Category4", length = 100)
	public PayCategory getPayCategory4() {
		return payCategory4;
	}

	/** See {@link #payCategory4}. */
	public void setPayCategory4(PayCategory payCategory4) {
		this.payCategory4 = payCategory4;
	}

	/** See {@link #payPeriodType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Period_Type")
	public PayPeriodType getPayPeriodType() {
		return payPeriodType;
	}

	/** See {@link #payPeriodType}. */
	public void setPayPeriodType(PayPeriodType payPeriodType) {
		this.payPeriodType = payPeriodType;
	}

	/** See {@link #numDays}. */
	@Column(name = "Num_Days")
	public Integer getNumDays() {
		return numDays;
	}

	/** See {@link #numDays}. */
	public void setNumDays(Integer numDays) {
		this.numDays = numDays;
	}

}
