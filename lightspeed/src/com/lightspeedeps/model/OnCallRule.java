package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.PayFraction;
import com.lightspeedeps.type.RuleType;

/**
 * OnCallRule entity.
 */
@Entity
@Table(name = "on_call_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class OnCallRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = - 4874259058386406502L;

	// Fields

	private BigDecimal days1to5Mult;
	private PayFraction days1to5Base;
	private BigDecimal day6Mult;
	private PayFraction day6Base;
	private BigDecimal day7Mult;
	private PayFraction day7Base;

	// Constructors

	/** default constructor */
	public OnCallRule() {
		setType(RuleType.OC);
	}

//	/** full constructor */
//	public OnCallRule(Integer productionId, String ruleKey, BigDecimal days1to5Mult,
//			String days1to5Base, BigDecimal day6Mult, String day6Base, BigDecimal day7Mult,
//			String day7Base, String description, String notes) {
//		super(productionId, ruleKey, description, notes);
//		this.days1to5Mult = days1to5Mult;
//		this.days1to5Base = days1to5Base;
//		this.day6Mult = day6Mult;
//		this.day6Base = day6Base;
//		this.day7Mult = day7Mult;
//		this.day7Base = day7Base;
//	}

	// Property accessors

	@Column(name = "Days1to5_Mult", precision = 4, scale = 2, nullable = false)
	public BigDecimal getDays1to5Mult() {
		return days1to5Mult;
	}
	public void setDays1to5Mult(BigDecimal days1to5Mult) {
		this.days1to5Mult = days1to5Mult;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Days1to5_Base", length = 10, nullable = false)
	public PayFraction getDays1to5Base() {
		return days1to5Base;
	}
	public void setDays1to5Base(PayFraction days1to5Base) {
		this.days1to5Base = days1to5Base;
	}

	@Column(name = "Day6_Mult", precision = 4, scale = 2, nullable = false)
	public BigDecimal getDay6Mult() {
		return day6Mult;
	}
	public void setDay6Mult(BigDecimal day6Mult) {
		this.day6Mult = day6Mult;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Day6_Base", length = 10, nullable = false)
	public PayFraction getDay6Base() {
		return day6Base;
	}
	public void setDay6Base(PayFraction day6Base) {
		this.day6Base = day6Base;
	}

	@Column(name = "Day7_Mult", precision = 4, scale = 2, nullable = false)
	public BigDecimal getDay7Mult() {
		return day7Mult;
	}
	public void setDay7Mult(BigDecimal day7Mult) {
		this.day7Mult = day7Mult;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Day7_Base", length = 10, nullable = false)
	public PayFraction getDay7Base() {
		return day7Base;
	}
	public void setDay7Base(PayFraction day7Base) {
		this.day7Base = day7Base;
	}

}
