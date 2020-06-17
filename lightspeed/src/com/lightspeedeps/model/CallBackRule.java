package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;

/**
 * CallBackRule entity.
 */
@Entity
@Table(name = "call_back_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class CallBackRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = - 4874542776926127143L;

	// Fields

	/** The minimum number of hours the employee must be paid. */
	private BigDecimal minimumHours;

	/** The minimum the employee must be paid as a percentage of their
	 * minimum call time.  Commonly 50%, represented here as 0.5. */
	private BigDecimal minimumPercent;

	/** The rate multiplier to use when the employee is called back, e.g., 1.5.
	 * Note that this is the minimum multiplier; an applicable RestRule (for
	 * rest invasion) may specify a higher multiplier. */
	private BigDecimal rate;

	// Constructors

	/** default constructor */
	public CallBackRule() {
		setType(RuleType.CB);
	}

	// Property accessors

	@Column(name = "Minimum_Hours", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumHours() {
		return minimumHours;
	}
	public void setMinimumHours(BigDecimal minimumHours) {
		this.minimumHours = minimumHours;
	}

	@Column(name = "Minimum_Percent", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumPercent() {
		return minimumPercent;
	}
	public void setMinimumPercent(BigDecimal minimumPercent) {
		this.minimumPercent = minimumPercent;
	}

	@Column(name = "Rate", precision = 8, scale = 6, nullable = false)
	public BigDecimal getRate() {
		return rate;
	}
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

}
