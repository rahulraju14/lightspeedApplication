package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.HoursType;
import com.lightspeedeps.type.RuleType;

/**
 * GoldenRule entity.
 */
@Entity
@Table(name = "golden_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class GoldenRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = - 5584464160205043964L;

	// Fields

	/** Indicates if gold breaks are based on Worked hours or
	 * Elapsed hours.  Also allows for mixtures where some breaks
	 * are worked and others are elapsed. */
	private HoursType workOrElapsed;

	/** Number of hours after which first gold rate is paid. This may be
	 * zero, indicating that gold time begins immediately. May not be null. */
	private BigDecimal break1;

	/** First gold rate; should never be null or zero. This is normally a
	 * multiplier of the hourly rate; however, a negative value indicates
	 * a multiplier of the Daily rate. */
	private BigDecimal multiplier;

	/** Number of hours after which second gold rate is paid.  Should be
	 * null if there is no second gold rate applicable. */
	private BigDecimal break2;

	/** Second gold rate; may be null or zero if 'break2' is null. */
	private BigDecimal multiplier2;

	/** Number of hours after which third gold rate is paid.  Should be
	 * null if there is no third gold rate applicable.  */
	private BigDecimal break3;

	/** Third gold rate; may be null or zero if 'break3' is null. */
	private BigDecimal multiplier3;

	/** The minimum number of hours of Gold time to be paid, if any gold
	 * time occurs. Usually zero; some or all of the Local 600 (camera) rules
	 * have a value of one. */
	private BigDecimal minHours;

	// Constructors

	/** default constructor */
	public GoldenRule() {
		setType(RuleType.GL);
	}

	// Property accessors

	/** See {@link #workOrElapsed}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Work_Or_Elapsed", length = 10, nullable = false)
	public HoursType getWorkOrElapsed() {
		return workOrElapsed;
	}
	/** See {@link #workOrElapsed}. */
	public void setWorkOrElapsed(HoursType workOrElapsed) {
		this.workOrElapsed = workOrElapsed;
	}

	/** See {@link #break1}. */
	@Column(name = "Break", precision = 4, scale = 2, nullable = false)
	public BigDecimal getBreak1() {
		return break1;
	}
	/** See {@link #break1}. */
	public void setBreak1(BigDecimal break1) {
		this.break1 = break1;
	}

	/** See {@link #multiplier}. */
	@Column(name = "Multiplier", precision = 8, scale = 6, nullable = false)
	public BigDecimal getMultiplier() {
		return multiplier;
	}
	/** See {@link #multiplier}. */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/** See {@link #break2}. */
	@Column(name = "Break2", precision = 4, scale = 2, nullable = true)
	public BigDecimal getBreak2() {
		return break2;
	}
	/** See {@link #break2}. */
	public void setBreak2(BigDecimal break2) {
		this.break2 = break2;
	}

	/** See {@link #multiplier2}. */
	@Column(name = "Multiplier2", precision = 8, scale = 6, nullable = true)
	public BigDecimal getMultiplier2() {
		return multiplier2;
	}
	/** See {@link #multiplier2}. */
	public void setMultiplier2(BigDecimal multiplier2) {
		this.multiplier2 = multiplier2;
	}

	/** See {@link #break3}. */
	@Column(name = "Break3", precision = 4, scale = 2, nullable = true)
	public BigDecimal getBreak3() {
		return break3;
	}
	/** See {@link #break3}. */
	public void setBreak3(BigDecimal break3) {
		this.break3 = break3;
	}

	/** See {@link #multiplier3}. */
	@Column(name = "Multiplier3", precision = 8, scale = 6, nullable = true)
	public BigDecimal getMultiplier3() {
		return multiplier3;
	}
	/** See {@link #multiplier3}. */
	public void setMultiplier3(BigDecimal multiplier3) {
		this.multiplier3 = multiplier3;
	}

	/** See {@link #minHours}. */
	@Column(name = "Min_Hours", precision = 4, scale = 2, nullable = true)
	public BigDecimal getMinHours() {
		return minHours;
	}
	/** See {@link #minHours}. */
	public void setMinHours(BigDecimal minHours) {
		this.minHours = minHours;
	}

}
