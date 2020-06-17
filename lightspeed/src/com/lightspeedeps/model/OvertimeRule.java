package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.OvertimeType;
import com.lightspeedeps.type.RuleType;

/**
 * OvertimeRule entity. Documents the conditions under which an overtime rate is
 * applied, and what that rate is. Also indicates if such overtime is subject to
 * the FLSA calculation.
 */
@Entity
@Table(name = "overtime_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class OvertimeRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 8114737490663204761L;

	// Fields

//	/** Guaranteed hours of pay. (not used: same as minimum call) */
//	private BigDecimal guarHours;

	/** After this many hours worked in a day, the rate multiplier changes to the
	 * value in the "OT Rate" column. Set to 99 when not applicable. */
	private BigDecimal overtimeDailyBreak;

	/** If the employee works after this time of day, the rate multiplier changes
	 * to the value in the "OT Rate" column. Ignore if zero. This is rarely used. */
	private BigDecimal overtimeBreakTime;

	/** The rate multiplier to use for the overtime involved, e.g., 1.5 */
	private BigDecimal overtimeRate;

	/** After this many hours worked in a day, the rate multiplier changes to the
	 * 'overtimeRate2' value. Set to 99 when not applicable.  This is rarely used. */
	private BigDecimal overtimeDailyBreak2;

	/** The rate multiplier to use for the overtime involved, after reaching the
	 * 'overtimeDailyBreak2' number of work hours in a day. */
	private BigDecimal overtimeRate2;

	/** True iff FLSA calculation should be applied to these overtime hours. */
	private Boolean applyFlsa;

	/** Indicates how the overtime rate is applied; allows use of hourly, daily, or
	 * weekly rates in overtime calculations. */
	private OvertimeType overtimeType;

	// Constructors

	/** default constructor */
	public OvertimeRule() {
		setType(RuleType.OT);
	}

//	/** full constructor */
//	public OvertimeRule(Integer productionId, String ruleKey, BigDecimal guarHours, BigDecimal minimumCall,
//			BigDecimal straightMult, BigDecimal overtimeDailyBreak, BigDecimal overtimeBreakTime,
//			BigDecimal overtimeRate, Boolean applyFlsa, BigDecimal maxHours, String description,
//			String notes) {
//		super(productionId, ruleKey, description, notes);
//		this.guarHours = guarHours;
//		this.minimumCall = minimumCall;
//		this.straightMult = straightMult;
//		this.overtimeDailyBreak = overtimeDailyBreak;
//		this.overtimeBreakTime = overtimeBreakTime;
//		this.overtimeRate = overtimeRate;
//		this.applyFlsa = applyFlsa;
//		this.maxHours = maxHours;
//	}

	// Property accessors

//	@Column(name = "Guar_Hours", precision = 4, scale = 2)
//	public BigDecimal getGuarHours() {
//		return guarHours;
//	}
//	public void setGuarHours(BigDecimal guarHours) {
//		this.guarHours = guarHours;
//	}

	@Column(name = "Overtime_Daily_Break", precision = 4, scale = 2, nullable = false)
	public BigDecimal getOvertimeDailyBreak() {
		return overtimeDailyBreak;
	}
	public void setOvertimeDailyBreak(BigDecimal overtimeDailyBreak) {
		this.overtimeDailyBreak = overtimeDailyBreak;
	}

	@Column(name = "Overtime_Break_Time", precision = 4, scale = 2, nullable = false)
	public BigDecimal getOvertimeBreakTime() {
		return overtimeBreakTime;
	}
	public void setOvertimeBreakTime(BigDecimal overtimeBreakTime) {
		this.overtimeBreakTime = overtimeBreakTime;
	}

	@Column(name = "Overtime_Rate", precision = 8, scale = 6, nullable = false)
	public BigDecimal getOvertimeRate() {
		return overtimeRate;
	}
	public void setOvertimeRate(BigDecimal overtimeRate) {
		this.overtimeRate = overtimeRate;
	}

	/** See {@link #overtimeDailyBreak2}. */
	@Column(name = "Overtime_Daily_Break2", precision = 4, scale = 2, nullable = false)
	public BigDecimal getOvertimeDailyBreak2() {
		return overtimeDailyBreak2;
	}
	/** See {@link #overtimeDailyBreak2}. */
	public void setOvertimeDailyBreak2(BigDecimal overtimeDailyBreak2) {
		this.overtimeDailyBreak2 = overtimeDailyBreak2;
	}

	/** See {@link #overtimeRate2}. */
	@Column(name = "Overtime_Rate2", precision = 8, scale = 6, nullable = false)
	public BigDecimal getOvertimeRate2() {
		return overtimeRate2;
	}
	/** See {@link #overtimeRate2}. */
	public void setOvertimeRate2(BigDecimal overtimeRate2) {
		this.overtimeRate2 = overtimeRate2;
	}

	@Column(name = "Apply_Flsa", nullable = false)
	public Boolean getApplyFlsa() {
		return applyFlsa;
	}
	public void setApplyFlsa(Boolean applyFlsa) {
		this.applyFlsa = applyFlsa;
	}

	/** See {@link #type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Overtime_Type", length = 10, nullable = false)
	public OvertimeType getOvertimeType() {
		return overtimeType;
	}
	/** See {@link #type}. */
	public void setOvertimeType(OvertimeType type) {
		overtimeType = type;
	}

}
