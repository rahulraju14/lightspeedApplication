package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.ForcedPayMethod;
import com.lightspeedeps.type.RuleType;

/**
 * RestRule entity.
 */
@Entity
@Table(name = "rest_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class RestRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 8901231972685155325L;

	// Fields

	/** Minimum rest period required for no penalty to take effect. */
	private BigDecimal minimum;

	/** If the employee gets less than this many hours of rest, the
	 * intervening time counts as worked time, not rest. */
	private BigDecimal lessIsWork;

	/** Indicates the method of calculating the rate of pay in effect
	 * when the employee returns to work. */
	private ForcedPayMethod forcedPayMethod;

	/** The minimum rest period required for a 5-day work week, between the
	 * last work-day's wrap time and the call time of the first day of the
	 * next week. */
	private BigDecimal minimumAfter5Days;

	/** The minimum rest period required for a 6-day work week, between the
	 * last work-day's wrap time and the call time of the first day of the
	 * next week. */
	private BigDecimal minimumAfter6Days;

	/** The minimum number of hours that an employee must be dismissed, after
	 * working past midnight on the 7th day, for the gold rate to reset to the
	 * "daily gold" value specified in the {@link #gold7MidnightRate} field. */
	private BigDecimal midnightRateReset;

	/** This is for Golden hour provisions as in Local 80, section 11(b), when
	 * Golden rate is reached on 7th day worked and continues past midnight,
	 * then dismissed for more than 'x' (usually 4) but less than 8 hours, then
	 * the rate reverts to this gold rate (typically 2X). The number of hours
	 * off required for this to happen ('x') is specified in the
	 * {@link #midnightRateReset} field. */
	private BigDecimal gold7MidnightRate;

	// Constructors

	/** default constructor */
	public RestRule() {
		setType(RuleType.RS);
	}

//	/** full constructor */
//	public RestRule(Integer productionId, String ruleKey, BigDecimal minimum, BigDecimal lessIsWork,
//			String forcedPayMethod, BigDecimal gold7MidnightRate, String description, String notes) {
//		super(productionId, ruleKey, description, notes);
//		this.minimum = minimum;
//		this.lessIsWork = lessIsWork;
//		this.forcedPayMethod = forcedPayMethod;
//		this.gold7MidnightRate = gold7MidnightRate;
//	}

	/** See {@link #minimum}. */
	@Column(name = "Minimum", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimum() {
		return minimum;
	}
	public void setMinimum(BigDecimal minimum) {
		this.minimum = minimum;
	}

	/** See {@link #lessIsWork}. */
	@Column(name = "Less_Is_Work", precision = 4, scale = 2, nullable = false)
	public BigDecimal getLessIsWork() {
		return lessIsWork;
	}
	public void setLessIsWork(BigDecimal lessIsWork) {
		this.lessIsWork = lessIsWork;
	}

	/** See {@link #forcedPayMethod}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Forced_Pay_Method", length = 10, nullable = false)
	public ForcedPayMethod getForcedPayMethod() {
		return forcedPayMethod;
	}
	public void setForcedPayMethod(ForcedPayMethod forcedPayMethod) {
		this.forcedPayMethod = forcedPayMethod;
	}

	/** See {@link #minimumAfter5Days}. */
	@Column(name = "Minimum_after_5_days", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumAfter5Days() {
		return minimumAfter5Days;
	}
	/** See {@link #minimumAfter5Days}. */
	public void setMinimumAfter5Days(BigDecimal minimumAfter5Days) {
		this.minimumAfter5Days = minimumAfter5Days;
	}

	/** See {@link #minimumAfter6Days}. */
	@Column(name = "Minimum_after_6_days", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumAfter6Days() {
		return minimumAfter6Days;
	}
	/** See {@link #minimumAfter6Days}. */
	public void setMinimumAfter6Days(BigDecimal minimumAfter6Days) {
		this.minimumAfter6Days = minimumAfter6Days;
	}

	/** See {@link #midnightRateReset}. */
	@Column(name = "Midnight_Rate_Reset", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMidnightRateReset() {
		return midnightRateReset;
	}
	/** See {@link #midnightRateReset}. */
	public void setMidnightRateReset(BigDecimal midnightRateReset) {
		this.midnightRateReset = midnightRateReset;
	}

	/** See {@link #gold7MidnightRate}. */
	@Column(name = "Gold7_Midnight_Rate", precision = 4, scale = 2, nullable = false)
	public BigDecimal getGold7MidnightRate() {
		return gold7MidnightRate;
	}
	public void setGold7MidnightRate(BigDecimal gold7MidnightRate) {
		this.gold7MidnightRate = gold7MidnightRate;
	}

}
