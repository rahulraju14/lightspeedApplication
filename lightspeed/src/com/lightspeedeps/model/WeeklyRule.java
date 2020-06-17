package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;

/**
 * WeeklyRule entity.
 */
@Entity
@Table(name = "weekly_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class WeeklyRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 880525215281893190L;

	// Fields

	/** The fraction of an hour in which overtime increments are measured. If negative, use
	 * the PayrollPreference setting for rounding -- this is meant for non-union timecards. */
	private BigDecimal overtimeIncrement;

	/** After this many hours paid at straight time in a week, remaining hours
	 * are paid at OT rate. Depending on the Overtime rule in effect, OT may be
	 * paid on a daily basis as well. */
	private BigDecimal overtimeWeeklyBreak;

	/** Guaranteed hours of employment for the week.  If 'null', then there
	 * is no weekly guarantee. */
	private BigDecimal cumeHours;

	/** Number of days in week to reach guaranteed hours. If employee is HOA,
	 * guarantee is reduced by the appropriate fraction, e.g., 1/5th for 5-day
	 * guarantee. */
	private Integer cumeDays;

	/** Multiplier used to calculate overtime pay, e.g., 1.5 for time-and-a-half. */
	private BigDecimal overtimeRate;

	// Constructors

	/** default constructor */
	public WeeklyRule() {
		setType(RuleType.BA);
	}

//	/** full constructor */
//	public WeeklyRule(Integer productionId, String ruleKey, BigDecimal overtimeIncrement,
//			BigDecimal overtimeWeeklyBreak, BigDecimal cumeHours, Integer cumeDays, BigDecimal overtimeRate,
//			String description, String notes) {
//		super(productionId, ruleKey, description, notes);
//		this.overtimeIncrement = overtimeIncrement;
//		this.overtimeWeeklyBreak = overtimeWeeklyBreak;
//		this.cumeHours = cumeHours;
//		this.cumeDays = cumeDays;
//		this.overtimeRate = overtimeRate;
//	}

	// Property accessors

	/** See {@link #overtimeIncrement}. */
	@Column(name = "Overtime_Increment", precision = 4, scale = 2, nullable = false)
	public BigDecimal getOvertimeIncrement() {
		return overtimeIncrement;
	}
	/** See {@link #overtimeIncrement}. */
	public void setOvertimeIncrement(BigDecimal overtimeIncrement) {
		this.overtimeIncrement = overtimeIncrement;
	}

	/** See {@link #overtimeWeeklyBreak}. */
	@Column(name = "Overtime_Weekly_Break", precision = 5, scale = 2, nullable = false)
	public BigDecimal getOvertimeWeeklyBreak() {
		return overtimeWeeklyBreak;
	}
	/** See {@link #overtimeWeeklyBreak}. */
	public void setOvertimeWeeklyBreak(BigDecimal overtimeWeeklyBreak) {
		this.overtimeWeeklyBreak = overtimeWeeklyBreak;
	}

	/** See {@link #cumeHours}. */
	@Column(name = "Cume_Hours", precision = 4, scale = 2, nullable = true)
	public BigDecimal getCumeHours() {
		return cumeHours;
	}
	/** See {@link #cumeHours}. */
	public void setCumeHours(BigDecimal cumeHours) {
		this.cumeHours = cumeHours;
	}

	/** See {@link #cumeDays}. */
	@Column(name = "Cume_Days", nullable = true)
	public Integer getCumeDays() {
		return cumeDays;
	}
	/** See {@link #cumeDays}. */
	public void setCumeDays(Integer cumeDays) {
		this.cumeDays = cumeDays;
	}

	/** See {@link #overtimeRate}. */
	@Column(name = "Overtime_Rate", precision = 4, scale = 2, nullable = false)
	public BigDecimal getOvertimeRate() {
		return overtimeRate;
	}
	/** See {@link #overtimeRate}. */
	public void setOvertimeRate(BigDecimal overtimeRate) {
		this.overtimeRate = overtimeRate;
	}

}
