package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;

/**
 * GuaranteeRule entity.
 */
@Entity
@Table(name = "guarantee_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class GuaranteeRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 8114737490663204761L;

	// Fields

//	/** Guaranteed hours of pay. (not used: same as minimum call) */
//	private BigDecimal guarHours;

	/** Minimum call time. */
	private BigDecimal minimumCall;

	/** If true, hours guaranteed by this rule will not count towards weekly
	 * straight-time hours (in terms of causing a weekly OT break to occur). */
	private boolean notWeekly;

	/** If true, hours guaranteed by this rule will only be paid at straight
	time, regardless of other OT or Gold rules in effect. */
	private boolean onlyStraight;

	/** Maximum hours payable. Typically used where a fixed number of hours are
	 * being paid, such as "Travel-4".  Set to 99 when not applicable. */
	private BigDecimal maxHours;

	// Constructors

	/** default constructor */
	public GuaranteeRule() {
		setType(RuleType.GT);
	}

	// Property accessors

	@Column(name = "Minimum_Call", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumCall() {
		return minimumCall;
	}
	public void setMinimumCall(BigDecimal minimumCall) {
		this.minimumCall = minimumCall;
	}

	/** See {@link #notWeekly}. */
	@Column(name = "Not_Weekly", nullable = false)
	public boolean getNotWeekly() {
		return notWeekly;
	}
	/** See {@link #notWeekly}. */
	public void setNotWeekly(boolean notWeekly) {
		this.notWeekly = notWeekly;
	}

	@Column(name = "Only_Straight", nullable = false)
	public boolean getOnlyStraight() {
		return onlyStraight;
	}
	public void setOnlyStraight(boolean straightMult) {
		onlyStraight = straightMult;
	}

	@Column(name = "Max_Hours", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxHours() {
		return maxHours;
	}
	public void setMaxHours(BigDecimal maxHours) {
		this.maxHours = maxHours;
	}

}
