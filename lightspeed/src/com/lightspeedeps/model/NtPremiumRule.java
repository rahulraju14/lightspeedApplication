package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;
import com.lightspeedeps.util.app.Constants;

/**
 * NtPremiumRule entity.
 */
@Entity
@Table(name = "nt_premium_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class NtPremiumRule extends Rule implements java.io.Serializable {
	/** */
	private static final long serialVersionUID = 1L;

	public static final BigDecimal PREMIUM_LOW_RATE = new BigDecimal("1.1");
	public static final BigDecimal PREMIUM_HIGH_RATE = new BigDecimal("1.2");
	public static final BigDecimal PREMIUM_OT_RATE = Constants.DECIMAL_ONE_FIVE;

	// Fields

	/** If the current rate multiplier in effect (e.g., for OT or gold) is greater
	 * than this value, do NOT apply night premium rules.  This is typically 2.0,
	 * so that night premium does not apply to Gold (2x) or higher rates. */
	private BigDecimal noNpLimit;

	/** If call time is this time or later, night premium time starts at
	 * the {@link #npRateStart} time, using the {@link #npRate} multiplier.*/
	private BigDecimal callStart;

	/** Night premium time starts at this time if the call time is
	 * {@link #callStart} or later, using the {@link #npRate} multiplier. */
	private BigDecimal npRateStart;

	/** Use this multiplier for night premium starting at {@link #npRateStart}
	 * time. */
	private BigDecimal npRate;

	/** The time that night premium ends, if it started due to exceeding the
	 * {@link #npRateStart} time. */
	private BigDecimal npRateEnd;

	/** When call time is after this time, night premium begins immediately,
	 * at a rate specified in {@link #lateRate}, and extends until {@link #lateRateEnd}. */
	private BigDecimal lateCallStart;

	/** Night premium ends at this time, when it was caused by a call time later
	 * than {@link #lateCallStart}. */
	private BigDecimal lateCallEnd;

	/** The rate multiplier applied for night premium caused by a call time later
	 * than {@link #lateCallStart}. */
	private BigDecimal lateRate;

	/** Night premium pay ends at this time when it was a result of a call time
	 * later than {@link #lateCallStart}. */
	private BigDecimal lateRateEnd;

	/** Night premium applies to call between this time and {@link #earlyCallEnd},
	 * with a rate of {@link #earlyRate}. */
	private BigDecimal earlyCallStart;

	/** Night premium applies to a call time between {@link #earlyCallStart} and this time,
	 * with a rate of {@link #earlyRate}. */
	private BigDecimal earlyCallEnd;

	/** The rate multiplier (e.g., 1.1) to be used for night premium that occurs as
	 * a result of a call time between {@link #earlyCallStart} and {@link #earlyCallEnd}. */
	private BigDecimal earlyRate;

	/** The time at which night premium payment ends, when that premium began as a
	 * result of an "early call" -- between {@link #earlyCallStart} and {@link #earlyCallEnd}. */
	private BigDecimal earlyRateEnd;

	// Constructors

	/** default constructor */
	public NtPremiumRule() {
		setType(RuleType.NP);
	}

//	/** minimal constructor */
//	public NtPremiumRule(String ruleKey) {
//		super(ruleKey);
//	}

	// Property accessors

	/** See {@link #noNpLimit}. */
	@Column(name = "No_Np_Limit", precision = 4, scale = 2, nullable = false)
	public BigDecimal getNoNpLimit() {
		return noNpLimit;
	}
	/** See {@link #noNpLimit}. */
	public void setNoNpLimit(BigDecimal noNpLimit) {
		this.noNpLimit = noNpLimit;
	}

	/** See {@link #callStart}. */
	@Column(name = "Call_Start", precision = 4, scale = 2, nullable = true)
	public BigDecimal getCallStart() {
		return callStart;
	}
	/** See {@link #callStart}. */
	public void setCallStart(BigDecimal callStart) {
		this.callStart = callStart;
	}

	/** See {@link #npRateStart}. */
	@Column(name = "Np_Rate_Start", precision = 4, scale = 2, nullable = true)
	public BigDecimal getNpRateStart() {
		return npRateStart;
	}
	/** See {@link #npRateStart}. */
	public void setNpRateStart(BigDecimal npRateStart) {
		this.npRateStart = npRateStart;
	}

	/** See {@link #npRate}. */
	@Column(name = "Np_Rate", precision = 4, scale = 2, nullable = false)
	public BigDecimal getNpRate() {
		return npRate;
	}
	/** See {@link #npRate}. */
	public void setNpRate(BigDecimal npRate) {
		this.npRate = npRate;
	}

	/** See {@link #npRateEnd}. */
	@Column(name = "Np_Rate_End", precision = 4, scale = 2, nullable = true)
	public BigDecimal getNpRateEnd() {
		return npRateEnd;
	}
	/** See {@link #npRateEnd}. */
	public void setNpRateEnd(BigDecimal npRateEnd) {
		this.npRateEnd = npRateEnd;
	}

	/** See {@link #lateCallStart}. */
	@Column(name = "Late_Call_Start", precision = 4, scale = 2, nullable = true)
	public BigDecimal getLateCallStart() {
		return lateCallStart;
	}
	/** See {@link #lateCallStart}. */
	public void setLateCallStart(BigDecimal lateCallStart) {
		this.lateCallStart = lateCallStart;
	}

	/** See {@link #lateCallEnd}. */
	@Column(name = "Late_Call_End", precision = 4, scale = 2, nullable = true)
	public BigDecimal getLateCallEnd() {
		return lateCallEnd;
	}
	/** See {@link #lateCallEnd}. */
	public void setLateCallEnd(BigDecimal lateCallEnd) {
		this.lateCallEnd = lateCallEnd;
	}

	/** See {@link #lateRate}. */
	@Column(name = "Late_Rate", precision = 4, scale = 2, nullable = false)
	public BigDecimal getLateRate() {
		return lateRate;
	}
	/** See {@link #lateRate}. */
	public void setLateRate(BigDecimal lateRate) {
		this.lateRate = lateRate;
	}

	/** See {@link #lateRateEnd}. */
	@Column(name = "Late_Rate_End", precision = 4, scale = 2, nullable = true)
	public BigDecimal getLateRateEnd() {
		return lateRateEnd;
	}
	/** See {@link #lateRateEnd}. */
	public void setLateRateEnd(BigDecimal lateRateEnd) {
		this.lateRateEnd = lateRateEnd;
	}

	/** See {@link #earlyCallStart}. */
	@Column(name = "Early_Call_Start", precision = 4, scale = 2, nullable = true)
	public BigDecimal getEarlyCallStart() {
		return earlyCallStart;
	}
	/** See {@link #earlyCallStart}. */
	public void setEarlyCallStart(BigDecimal earlyCallStart) {
		this.earlyCallStart = earlyCallStart;
	}

	/** See {@link #earlyCallEnd}. */
	@Column(name = "Early_Call_End", precision = 4, scale = 2, nullable = true)
	public BigDecimal getEarlyCallEnd() {
		return earlyCallEnd;
	}
	/** See {@link #earlyCallEnd}. */
	public void setEarlyCallEnd(BigDecimal earlyCallEnd) {
		this.earlyCallEnd = earlyCallEnd;
	}

	/** See {@link #earlyRate}. */
	@Column(name = "Early_Rate", precision = 4, scale = 2, nullable = false)
	public BigDecimal getEarlyRate() {
		return earlyRate;
	}
	/** See {@link #earlyRate}. */
	public void setEarlyRate(BigDecimal earlyRate) {
		this.earlyRate = earlyRate;
	}

	/** See {@link #earlyRateEnd}. */
	@Column(name = "Early_Rate_End", precision = 4, scale = 2, nullable = true)
	public BigDecimal getEarlyRateEnd() {
		return earlyRateEnd;
	}
	/** See {@link #earlyRateEnd}. */
	public void setEarlyRateEnd(BigDecimal earlyRateEnd) {
		this.earlyRateEnd = earlyRateEnd;
	}

}
