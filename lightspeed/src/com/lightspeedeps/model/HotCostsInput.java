package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * HotCostsInput entity. Model to persist the data values that can be
 * pushed to the individual hot cost line items.
 */
@Entity
@Table(name = "hot_costs_input")
public class HotCostsInput extends PersistentObject<HotCostsInput> {
	private static final long serialVersionUID = 1L;

	// Fields
	/** Production ID for the current production */
	private String prodId;
	/** Week Ending Date that Hot Costs is being generated for. Combine with day of the week number*/
	private Date weekEndDate;
	/** Day Number of the week */
	private Byte dayOfWeekNum;
	/** Work Zone */
	private WorkZone workZone;
	/** Day Type */
	private DayType dayType;
	/** Call time */
	private BigDecimal callTime;
	/** Meal 1 Out */
	private BigDecimal m1Out;
	/** Meal 1 In */
	private BigDecimal m1In;
	/** Meal 2 Out */
	private BigDecimal m2Out;
	/** Meal 2 In */
	private BigDecimal m2In;
	/** Wrap */
	private BigDecimal wrap;
	/** End of Nondeductible Breakfast */
	private BigDecimal ndbEnd;
	/** Start of Nondeductible Meal */
	private BigDecimal ndmStart;
	/** End of Nondeductible Meal */
	private BigDecimal ndmEnd;
	/** Time of first Grace Period */
	private BigDecimal grace1;
	/** Time of second Grace Period */
	private BigDecimal grace2;
	/** Time of last man in */
	private BigDecimal lastManIn;
	/** First Meal Penalty */
	private Byte mpv1Payroll;
	/** Second Meal Penalty */
	private Byte mpv2Payroll;
	/** Whether Crew Member is working with the shooting or crew or not */
	private Boolean offProduction;
	/** Whether the crew member has been called back to work before completion of their rest period */
	private Boolean forcedCall;
	/** Whether Camera Wrap has been called */
	private Boolean cameraWrap;
	/** Whether French Hours has been called */
	private Boolean frenchHours;
	/** Budgeted hours for the day */
	private BigDecimal budgetedHours;
	/** Budgeted cost for the day */
	private BigDecimal budgetedCost;
	/** Budgeted Meal Penalties for the day */
	private Byte budgetedMpv;
	/** Budgeted cost of Meal Penalties for the day */
	private BigDecimal budgetedMpvCost;

	// Constructors

	/** default constructor */
	public HotCostsInput() {
		prodId = "";
		weekEndDate = CalendarUtils.todaysDate();

		init();
	}

	/**
	 *
	 * @param weekEndDate
	 * @param dayOfWeekNum
	 * @param prodId
	 */
	public HotCostsInput(Date weekEndDate, Byte dayOfWeekNum, String prodId) {
		init();
		this.weekEndDate = weekEndDate;
		this.dayOfWeekNum = dayOfWeekNum;
		this.prodId = prodId;
	}

	/** See {@link #prodId}. */
	@Column(name = "Prod_Id", nullable = false, length=10)
	public String getProdId() {
		return prodId;
	}

	/** See {@link #prodId}. */
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	/** See {@link #weekEndDate}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Week_End_Date", nullable = false)
	public Date getWeekEndDate() {
		return this.weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #dayOfWeekNum}. */
	@Column(name = "Day_Of_Week_Num", nullable = false)
	public Byte getDayOfWeekNum() {
		return this.dayOfWeekNum;
	}

	/** See {@link #dayOfWeekNum}. */
	public void setDayOfWeekNum(Byte dayOfWeekNum) {
		this.dayOfWeekNum = dayOfWeekNum;
	}

	/** See {@link #workZone}. */
	@Column(name = "Work_Zone", length = 4)
	@Enumerated(EnumType.STRING)
	public WorkZone getWorkZone() {
		return this.workZone;
	}

	/** See {@link #workZone}. */
	public void setWorkZone(WorkZone workZone) {
		this.workZone = workZone;
	}

	/** See {@link #dayType}. */
	@Column(name = "Day_Type", length = 4)
	@Enumerated(EnumType.STRING)
	public DayType getDayType() {
		return this.dayType;
	}

	/** See {@link #dayType}. */
	public void setDayType(DayType dayType) {
		this.dayType = dayType;
	}

	/** See {@link #callTime}. */
	@Column(name = "Call_Time", precision = 4)
	public BigDecimal getCallTime() {
		return this.callTime;
	}

	/** See {@link #callTime}. */
	public void setCallTime(BigDecimal callTime) {
		this.callTime = callTime;
	}

	/** See {@link #m1Out}. */
	@Column(name = "M1_Out", precision = 4)
	public BigDecimal getM1Out() {
		return this.m1Out;
	}

	/** See {@link #m1Out}. */
	public void setM1Out(BigDecimal m1Out) {
		this.m1Out = m1Out;
	}

	/** See {@link #m1In}. */
	@Column(name = "M1_In", precision = 4)
	public BigDecimal getM1In() {
		return this.m1In;
	}

	/** See {@link #m1In}. */
	public void setM1In(BigDecimal m1In) {
		this.m1In = m1In;
	}

	/** See {@link #m2Out}. */
	@Column(name = "M2_Out", precision = 4)
	public BigDecimal getM2Out() {
		return this.m2Out;
	}

	/** See {@link #m2Out}. */
	public void setM2Out(BigDecimal m2Out) {
		this.m2Out = m2Out;
	}

	/** See {@link #m2In}. */
	@Column(name = "M2_In", precision = 4)
	public BigDecimal getM2In() {
		return this.m2In;
	}

	/** See {@link #m2In}. */
	public void setM2In(BigDecimal m2In) {
		this.m2In = m2In;
	}

	/** See {@link #wrap}. */
	@Column(name = "Wrap", precision = 4)
	public BigDecimal getWrap() {
		return this.wrap;
	}

	/** See {@link #wrap}. */
	public void setWrap(BigDecimal wrap) {
		this.wrap = wrap;
	}

	/** See {@link #ndbEnd}. */
	@Column(name = "Ndb_End", precision = 4)
	public BigDecimal getNdbEnd() {
		return this.ndbEnd;
	}

	/** See {@link #ndbEnd}. */
	public void setNdbEnd(BigDecimal ndbEnd) {
		this.ndbEnd = ndbEnd;
	}

	/** See {@link #ndmStart}. */
	@Column(name = "Ndm_Start", precision = 4)
	public BigDecimal getNdmStart() {
		return this.ndmStart;
	}

	/** See {@link #ndmStart}. */
	public void setNdmStart(BigDecimal ndmStart) {
		this.ndmStart = ndmStart;
	}

	/** See {@link #ndmEnd}. */
	@Column(name = "Ndm_End", precision = 4)
	public BigDecimal getNdmEnd() {
		return this.ndmEnd;
	}

	/** See {@link #ndmEnd}. */
	public void setNdmEnd(BigDecimal ndmEnd) {
		this.ndmEnd = ndmEnd;
	}

	/** See {@link #grace1}. */
	@Column(name = "Grace1", precision = 3)
	public BigDecimal getGrace1() {
		return this.grace1;
	}

	/** See {@link #grace1}. */
	public void setGrace1(BigDecimal grace1) {
		this.grace1 = grace1;
	}

	/** See {@link #grace2}. */
	@Column(name = "Grace2", precision = 3)
	public BigDecimal getGrace2() {
		return this.grace2;
	}

	/** See {@link #grace2}. */
	public void setGrace2(BigDecimal grace2) {
		this.grace2 = grace2;
	}

	/** See {@link #prodId}. */
	@Column(name = "Last_Man_In", precision = 4)
	public BigDecimal getLastManIn() {
		return this.lastManIn;
	}

	/** See {@link #lastManIn}. */
	public void setLastManIn(BigDecimal lastManIn) {
		this.lastManIn = lastManIn;
	}

	/** See {@link #mpv1Payroll}. */
	@Column(name = "Mpv1_Payroll")
	public Byte getMpv1Payroll() {
		return this.mpv1Payroll;
	}

	/** See {@link #mpv1Payroll}. */
	public void setMpv1Payroll(Byte mpv1Payroll) {
		this.mpv1Payroll = mpv1Payroll;
	}

	/** See {@link #mpv2Payroll}. */
	@Column(name = "Mpv2_Payroll")
	public Byte getMpv2Payroll() {
		return this.mpv2Payroll;
	}

	/** See {@link #mpv2Payroll}. */
	public void setMpv2Payroll(Byte mpv2Payroll) {
		this.mpv2Payroll = mpv2Payroll;
	}

	/** See {@link #offProduction}. */
	@Column(name = "Off_Production", nullable = false)
	public Boolean getOffProduction() {
		return this.offProduction;
	}

	/** See {@link #offProduction}. */
	public void setOffProduction(Boolean offProduction) {
		this.offProduction = offProduction;
	}

	/** See {@link #forcedCall}. */
	@Column(name = "Forced_Call", nullable = false)
	public Boolean getForcedCall() {
		return this.forcedCall;
	}

	/** See {@link #forcedCall}. */
	public void setForcedCall(Boolean forcedCall) {
		this.forcedCall = forcedCall;
	}

	/** See {@link #cameraWrap}. */
	@Column(name = "Camera_Wrap", nullable = false)
	public Boolean getCameraWrap() {
		return this.cameraWrap;
	}

	/** See {@link #cameraWrap}. */
	public void setCameraWrap(Boolean cameraWrap) {
		this.cameraWrap = cameraWrap;
	}

	/** See {@link #frenchHours}. */
	@Column(name = "French_Hours", nullable = false)
	public Boolean getFrenchHours() {
		return this.frenchHours;
	}

	/** See {@link #frenchHours}. */
	public void setFrenchHours(Boolean frenchHours) {
		this.frenchHours = frenchHours;
	}

	/** See {@link #budgetedHours}. */
	@Column(name = "Budgeted_Hours", precision = 6)
	public BigDecimal getBudgetedHours() {
		return this.budgetedHours;
	}

	/** See {@link #budgetedHours}. */
	public void setBudgetedHours(BigDecimal budgetedHours) {
		this.budgetedHours = budgetedHours;
	}

	/** See {@link #budgetedCost}. */
	@Column(name = "Budgeted_Cost", precision = 10)
	public BigDecimal getBudgetedCost() {
		return this.budgetedCost;
	}

	/** See {@link #budgetedCost}. */
	public void setBudgetedCost(BigDecimal budgetedCost) {
		this.budgetedCost = budgetedCost;
	}

	/** See {@link #budgetedMpv}. */
	@Column(name = "Budgeted_Mpv")
	public Byte getBudgetedMpv() {
		return this.budgetedMpv;
	}

	/** See {@link #budgetedMpv}. */
	public void setBudgetedMpv(Byte budgetedMpv) {
		this.budgetedMpv = budgetedMpv;
	}

	/** See {@link #budgetedMpvCost}. */
	@Column(name = "Budgeted_Mpv_Cost", precision = 10)
	public BigDecimal getBudgetedMpvCost() {
		return this.budgetedMpvCost;
	}

	/** See {@link #budgetedMpvCost}. */
	public void setBudgetedMpvCost(BigDecimal budgetedMpvCost) {
		this.budgetedMpvCost = budgetedMpvCost;
	}

	/**
	 * Initialize this object prior to its use;
	 */
	private void init() {
		offProduction = false;
		forcedCall = false;
		cameraWrap = false;
		frenchHours = false;
		forcedCall = false;
	}
}