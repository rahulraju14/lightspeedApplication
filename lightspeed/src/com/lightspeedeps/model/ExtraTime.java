package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * ExtraTime entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "extra_time")
public class ExtraTime extends PersistentObject<ExtraTime> {

	// Fields
	private static final long serialVersionUID = -7535458510172883066L;

//	public static final char TYPE_STANDIN = 'S';
//	public static final char TYPE_BACKGROUND = 'B';

	private Dpr dpr;
	private Integer lineNumber = 0;
	private String description;
	private String nonUnion;
	private Integer count = 0;
	private String rate;
	private BigDecimal callTime;
	private BigDecimal m1Out;
	private BigDecimal m1In;
	private BigDecimal wrap;
	private BigDecimal adjustment;
	private BigDecimal ot;
	private Byte mpv;
	private BigDecimal miles;
	private String misc;

	// Constructors

	/** default constructor */
	public ExtraTime() {
	}

	public ExtraTime(Dpr pDpr, int lineNum) {
		dpr = pDpr;
		lineNumber = lineNum;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DPR_Id", nullable = false)
	public Dpr getDpr() {
		return dpr;
	}
	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
	}

	/** See {@link #lineNumber}. */
	@Column(name = "Line_Number")
	public Integer getLineNumber() {
		return lineNumber;
	}
	/** See {@link #lineNumber}. */
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/**See {@link #nonUnion}. */
	@Column(name = "Non_Union", length = 10)
	public String getNonUnion() {
		return nonUnion;
	}
	/**See {@link #nonUnion}. */
	public void setNonUnion(String nonUnion) {
		this.nonUnion = nonUnion;
	}

	@Column(name = "Count", nullable = false)
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}

	@Column(name = "Call_Time", precision = 4, scale = 2)
	public BigDecimal getCallTime() {
		return callTime;
	}
	public void setCallTime(BigDecimal timeIn) {
		callTime = timeIn;
	}

	/**See {@link #m1Out}. */
	@Column(name = "M1_Out", precision = 4, scale = 2)
	public BigDecimal getM1Out() {
		return m1Out;
	}
	/**See {@link #m1Out}. */
	public void setM1Out(BigDecimal m1Out) {
		this.m1Out = m1Out;
	}

	/**See {@link #m1In}. */
	@Column(name = "M1_In", precision = 4, scale = 2)
	public BigDecimal getM1In() {
		return m1In;
	}
	/**See {@link #m1In}. */
	public void setM1In(BigDecimal m1In) {
		this.m1In = m1In;
	}

	@Column(name = "Wrap", precision = 4, scale = 2)
	public BigDecimal getWrap() {
		return wrap;
	}
	public void setWrap(BigDecimal timeOut) {
		wrap = timeOut;
	}

	@Column(name = "Rate", length = 100)
	public String getRate() {
		return rate;
	}
	public void setRate(String rate) {
		this.rate = rate;
	}

	@Column(name = "Adjustment", precision = 4, scale = 2)
	public BigDecimal getAdjustment() {
		return adjustment;
	}
	public void setAdjustment(BigDecimal adjustment) {
		this.adjustment = adjustment;
	}

	@Column(name = "OT", precision = 4, scale = 2)
	public BigDecimal getOt() {
		return ot;
	}
	public void setOt(BigDecimal ot) {
		this.ot = ot;
	}

	@Column(name = "MPV")
	public Byte getMpv() {
		return mpv;
	}
	public void setMpv(Byte mpv) {
		this.mpv = mpv;
	}

	@Column(name = "Miles", precision = 8, scale = 2)
	public BigDecimal getMiles() {
		return miles;
	}
	public void setMiles(BigDecimal miles) {
		this.miles = miles;
	}

	@Column(name = "Misc", length = 1000)
	public String getMisc() {
		return misc;
	}
	public void setMisc(String misc) {
		this.misc = misc;
	}

}