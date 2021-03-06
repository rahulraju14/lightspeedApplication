package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.lightspeedeps.type.DayType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * DailyHotCosts entity. Values for the individual Daily Hot Cost
 */
@Entity
@Table(name = "daily_hot_cost")
public class DailyHotCost extends  DailyTimeMapped implements Cloneable {
	private static final long serialVersionUID = 1L;

	// Fields
	/** The parent of this Daily Hot Cost */
	private WeeklyHotCosts weeklyHotCosts;
	/** Budgeted Hours for this Daily Hot Costs */
	private BigDecimal budgetedHours;
	/** Budgeted Cost for this Daily Hot Costs */
	private BigDecimal budgetedCost;
	/** Budgeted MPVs for this Daily Hot Costs */
	private Byte budgetedMpv;
	/** Budgeted MPV Cost for this Daily Hot Costs */
	private BigDecimal budgetedMpvCost;
	/** Holds the Employee"s current rate or their Re-rate value */
	private BigDecimal rate;
	/** Cost generated by calculating HTG */
	private BigDecimal actualCost;
	/** Account Detail code for this Daily Hot Costs */
	private String accountDetail;
	/** Date daily hot costs was created */
	private Date created;
	/** Person creating the daily hot costs */
	private String createdBy;
	/** Date daily hot costs was created */
	private Date updated;
	/** Person creating the daily hot costs */
	private String updatedBy;
	/** Pay Hours - derived from the actual hours calculated on
	 * Hot Costs Entry page
	 */
	private BigDecimal payHours;

	@Transient
	/** True if the ndb end date has been set. */
	private Boolean ndb;
	@Transient
	/** True if the ndm start and end dates have been set. */
	private Boolean ndm;

	// Constructors

	/** default constructor */
	public DailyHotCost() {
		init();
		setDate(CalendarUtils.todaysDate());
	}

	public DailyHotCost(Date date) {
		init();
		setDate(date);
		setDayNum((byte) CalendarUtils.getDayNumber(date));
		setWorkDayNum(getDayNum());
	}

	/** Initialize this Daily Hot Costs object */
	private void init() {
		setWorked(false);
		setNoStartForm(false);
		setCameraWrap(false);
		setFrenchHours(false);
		setOpposite(false);
		setOffProduction(false);
		setForcedCall(false);
		setNonDeductMeal(false);
		setNonDeductMeal2(false);
		setNonDeductMealPayroll(false);
		setNonDeductMeal2Payroll(false);
		setReRate(false);
		setSplitByPercent(false);
		setJobNum1((byte)1);
		setJobNum2((byte)1);
		setJobNum3((byte)1);
		setDayNum((byte)0);
		setWorkDayNum((byte)0);
		setActualCost(BigDecimal.ZERO);
		setBudgetedCost(null);
		setHours(BigDecimal.ZERO);
		setBudgetedHours(null);
		setBudgetedMpv(null);
		setBudgetedMpvCost(null);
		setMpv1Payroll(null);
		setMpv2Payroll(null);
		setMpv3Payroll(null);
		setDayType(DayType.WK);
	}

	// Property accessors
	/** See {@link #weeklyHotCosts}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyHotCosts getWeeklyHotCosts() {
		return this.weeklyHotCosts;
	}

	/** See {@link #weeklyHotCosts}. */
	public void setWeeklyHotCosts(WeeklyHotCosts weeklyHotCosts) {
		this.weeklyHotCosts = weeklyHotCosts;
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

	/** See {@link #rate}. */
	@Column(name = "Rate", precision = 10)
	public BigDecimal getRate() {
		return this.rate;
	}

	/** See {@link #rate}. */
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	/** See {@link #actualCost}. */
	@Column(name = "Actual_Cost", precision = 10)
	public BigDecimal getActualCost() {
		return this.actualCost;
	}

	/** See {@link #actualCost}. */
	public void setActualCost(BigDecimal actualCost) {
		this.actualCost = actualCost;
	}

	/** See {@link #accountDetail}. */
	@Column(name = "Acct_Detail")
	public String getAccountDetail() {
		return this.accountDetail;
	}

	/** See {@link #accountDetail}. */
	public void setAccountDetail(String accountDetail) {
		this.accountDetail = accountDetail;
	}

	/** See {@link #payHours}. */
	@Column(name= "Pay_Hours")
	public BigDecimal getPayHours() {
		return payHours;
	}

	/** See {@link #payHours}. */
	public void setPayHours(BigDecimal payHours) {
		this.payHours = payHours;
	}

	/** See {@link #created}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created")
	public Date getCreated() {
		return created;
	}

	/** See {@link #created}. */
	public void setCreated(Date created) {
		this.created = created;
	}

	/** See {@link #createdBy}. */
	@Column(name = "Created_By")
	public String getCreatedBy() {
		return createdBy;
	}

	/** See {@link #createdBy}. */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/** See {@link #updated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated")
	public Date getUpdated() {
		return updated;
	}

	/** See {@link #updated}. */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/** See {@link #updatedBy}. */
	@Column(name = "Updated_By")
	public String getUpdatedBy() {
		return updatedBy;
	}

	/** See {@link #updatedBy}. */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	@Transient
	public Boolean getNdb() {
		return getNdbEnd() != null;
	}

	@Transient
	// Should not do anything
	public void setNdb(Boolean ndb) {}

	@Transient
	public Boolean getNdm() {
		return (getNdmStart() != null && getNdmEnd() != null);
	}

	@Transient
	// Should not do anything
	public void setNdm(Boolean ndm) {}

	@Transient
	public void reset() {
		setDayType(null);
		setWorkZone(null);
		setCallTime(null);
		setM1Out(null);
		setM1In(null);
		setM2Out(null);
		setM2In(null);
		setWrap(null);
		setNdbEnd(null);
		setNdmStart(null);
		setNdmEnd(null);
		setOffProduction(false);
		setCameraWrap(false);
		setForcedCall(false);
		setGrace1(null);
		setGrace2(null);
		setMpv1Payroll(null);
		setMpv2Payroll(null);
		setHours(Constants.DECIMAL_ZERO);

		actualCost = Constants.DECIMAL_ZERO;
		budgetedHours = null;
		budgetedCost = null;
		budgetedMpv = null;
		budgetedMpvCost = null;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public DailyHotCost clone() {
		DailyHotCost dhc = null;
		try {
			dhc = (DailyHotCost)super.clone();
		}
		catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return dhc;
	}
}