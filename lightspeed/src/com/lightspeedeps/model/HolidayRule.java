package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.*;

import com.lightspeedeps.type.PayFraction;
import com.lightspeedeps.type.RuleType;

/**
 * HolidayRule entity.
 */
@Entity
@Table(name = "holiday_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class HolidayRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 2553389417565132403L;

	// Fields

	/** Treat a holiday falling on Saturday as if it fell on Friday. */
	private boolean saturdayAsFriday;

	/** treat a holiday falling on Sunday as if it fell on Monday. */
	private boolean sundayAsMonday;

//	/** Multiplier for hourly rate for holiday time. */
//	private BigDecimal dailyRate;
//
//	/** Multiplier for hourly rate for holiday time that extends into gold. */
//	private BigDecimal goldRate;
//
//	/** The breakpoint after which time worked (or elapsed) is paid at gold rate. */
//	private BigDecimal goldHours;

	/** The holiday pay for an On-Call schedule, as a PayFraction. */
	private PayFraction onCallRate;

	/** The holiday pay for a hourly (not on-call) schedule, as a PayFraction. */
	private PayFraction hourlyRate;

	/** When a non-holiday work-day extends past midnight into a holiday,
	 * pay the hours on the holiday at holiday rate iff this is true.  */
	private Boolean overlapApplies;

	/** When a non-holiday work-day extends past midnight into a holiday,
	 * pay the hours on the holiday at this rate, if overlapApplies is true. */
	private BigDecimal overlapDailyRate;

	/** When a non-holiday work-day extends past midnight into a holiday,
	 * pay gold hours on the holiday at this rate, if overlapApplies is true. */
	private BigDecimal overlapGoldRate;

//	/** */
//	private BigDecimal overlapGoldHours;

	/** True iff non-worked holidays should be paid as they occur. */
	private Boolean paid;

	/** The number of hours to pay for a non-worked holiday. */
	private BigDecimal notWorkedHours;

	/** The time to pay for a non-worked holiday, for On-Call schedules,
	 * expressed as a PayFraction. */
	private PayFraction notWorkedOnCallRate;

	/** The time to pay for a non-worked holiday, for weekly (not On-Call) schedules,
	 *  expressed as a PayFraction. */
	private PayFraction notWorkedWeeklyRate;

	// Constructors

	/** default constructor */
	public HolidayRule() {
		setType(RuleType.HO);
	}

//	/** full constructor */
//	public HolidayRule(Integer productionId, String ruleKey, BigDecimal dailyRate, BigDecimal goldRate,
//			BigDecimal goldHours, PayFraction onCallRate, PayFraction weeklyRate, Boolean overlapApplies,
//			BigDecimal overlapDailyRate, BigDecimal overlapGoldRate, BigDecimal overlapGoldHours, Boolean paid,
//			BigDecimal notWorkedHours, PayFraction notWorkedOnCallRate, PayFraction notWorkedWeeklyRate,
//			String description, String notes) {
//		super(productionId, ruleKey, description, notes);
//		this.dailyRate = dailyRate;
//		this.goldRate = goldRate;
//		this.goldHours = goldHours;
//		this.onCallRate = onCallRate;
//		this.weeklyRate = weeklyRate;
//		this.overlapApplies = overlapApplies;
//		this.overlapDailyRate = overlapDailyRate;
//		this.overlapGoldRate = overlapGoldRate;
//		this.overlapGoldHours = overlapGoldHours;
//		this.paid = paid;
//		this.notWorkedHours = notWorkedHours;
//		this.notWorkedOnCallRate = notWorkedOnCallRate;
//		this.notWorkedWeeklyRate = notWorkedWeeklyRate;
//	}

	// Property accessors

	/**See {@link #saturdayAsFriday}. */
	@Column(name = "Saturday_As_Friday", nullable = false)
	public boolean getSaturdayAsFriday() {
		return saturdayAsFriday;
	}
	/**See {@link #saturdayAsFriday}. */
	public void setSaturdayAsFriday(boolean saturdayAsFriday) {
		this.saturdayAsFriday = saturdayAsFriday;
	}

	/**See {@link #sundayAsMonday}. */
	@Column(name = "Sunday_As_Monday", nullable = false)
	public boolean getSundayAsMonday() {
		return sundayAsMonday;
	}
	/**See {@link #sundayAsMonday}. */
	public void setSundayAsMonday(boolean sundayAsMonday) {
		this.sundayAsMonday = sundayAsMonday;
	}

//	/** See {@link #dailyRate}. */
//	@Column(name = "Daily_Rate", precision = 4, scale = 2, nullable = false)
//	public BigDecimal getDailyRate() {
//		return dailyRate;
//	}
//	/** See {@link #dailyRate}. */
//	public void setDailyRate(BigDecimal dailyRate) {
//		this.dailyRate = dailyRate;
//	}
//
//	/** See {@link #goldRate}. */
//	@Column(name = "Gold_Rate", precision = 4, scale = 2, nullable = false)
//	public BigDecimal getGoldRate() {
//		return goldRate;
//	}
//	/** See {@link #goldRate}. */
//	public void setGoldRate(BigDecimal goldRate) {
//		this.goldRate = goldRate;
//	}
//
//	/** See {@link #goldHours}. */
//	@Column(name = "Gold_Hours", precision = 4, scale = 2, nullable = false)
//	public BigDecimal getGoldHours() {
//		return goldHours;
//	}
//	/** See {@link #goldHours}. */
//	public void setGoldHours(BigDecimal goldHours) {
//		this.goldHours = goldHours;
//	}

	/** See {@link #onCallRate}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "On_Call_Rate", length = 10, nullable = false)
	public PayFraction getOnCallRate() {
		return onCallRate;
	}
	/** See {@link #onCallRate}. */
	public void setOnCallRate(PayFraction onCallRate) {
		this.onCallRate = onCallRate;
	}

	/** See {@link #hourlyRate}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Weekly_Rate", length = 10, nullable = false)
	public PayFraction getHourlyRate() {
		return hourlyRate;
	}
	/** See {@link #hourlyRate}. */
	public void setHourlyRate(PayFraction rate) {
		this.hourlyRate = rate;
	}

	/** See {@link #overlapApplies}. */
	@Column(name = "Overlap_Applies", nullable = false)
	public Boolean getOverlapApplies() {
		return overlapApplies;
	}
	/** See {@link #overlapApplies}. */
	public void setOverlapApplies(Boolean overlapApplies) {
		this.overlapApplies = overlapApplies;
	}

	/** See {@link #overlapDailyRate}. */
	@Column(name = "Overlap_Daily_Rate", precision = 8, scale = 6, nullable = false)
	public BigDecimal getOverlapDailyRate() {
		return overlapDailyRate;
	}
	/** See {@link #overlapDailyRate}. */
	public void setOverlapDailyRate(BigDecimal overlapDailyRate) {
		this.overlapDailyRate = overlapDailyRate;
	}

	/** See {@link #overlapGoldRate}. */
	@Column(name = "Overlap_Gold_Rate", precision = 8, scale = 6, nullable = false)
	public BigDecimal getOverlapGoldRate() {
		return overlapGoldRate;
	}
	/** See {@link #overlapGoldRate}. */
	public void setOverlapGoldRate(BigDecimal overlapGoldRate) {
		this.overlapGoldRate = overlapGoldRate;
	}

//	/** See {@link #overlapGoldHours}. */
//	@Column(name = "Overlap_Gold_Hours", precision = 4, scale = 2)
//	public BigDecimal getOverlapGoldHours() {
//		return overlapGoldHours;
//	}
//	/** See {@link #overlapGoldHours}. */
//	public void setOverlapGoldHours(BigDecimal overlapGoldHours) {
//		this.overlapGoldHours = overlapGoldHours;
//	}

	/** See {@link #paid}. */
	@Column(name = "Paid", nullable = false)
	public Boolean getPaid() {
		return paid;
	}
	/** See {@link #paid}. */
	public void setPaid(Boolean paid) {
		this.paid = paid;
	}

	/** See {@link #notWorkedHours}. */
	@Column(name = "Not_Worked_Hours", precision = 4, scale = 2, nullable = false)
	public BigDecimal getNotWorkedHours() {
		return notWorkedHours;
	}
	/** See {@link #notWorkedHours}. */
	public void setNotWorkedHours(BigDecimal notWorkedHours) {
		this.notWorkedHours = notWorkedHours;
	}

	/** See {@link #notWorkedOnCallRate}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Not_Worked_On_Call_Rate", length = 10, nullable = false)
	public PayFraction getNotWorkedOnCallRate() {
		return notWorkedOnCallRate;
	}
	/** See {@link #notWorkedOnCallRate}. */
	public void setNotWorkedOnCallRate(PayFraction notWorkedOnCallRate) {
		this.notWorkedOnCallRate = notWorkedOnCallRate;
	}

	/** See {@link #notWorkedWeeklyRate}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Not_Worked_Weekly_Rate", length = 10, nullable = false)
	public PayFraction getNotWorkedWeeklyRate() {
		return notWorkedWeeklyRate;
	}
	/** See {@link #notWorkedWeeklyRate}. */
	public void setNotWorkedWeeklyRate(PayFraction notWorkedWeeklyRate) {
		this.notWorkedWeeklyRate = notWorkedWeeklyRate;
	}

}
