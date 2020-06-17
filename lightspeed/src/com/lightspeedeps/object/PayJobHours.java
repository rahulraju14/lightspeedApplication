/**
 * PayJobHours.java
 */
package com.lightspeedeps.object;

import java.math.BigDecimal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayJobDaily;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * A simple object to hold a set of hours corresponding to the columns in a
 * PayJob (job table) instance. Used by the PayBreakdownService to track holiday
 * and other special hour totals.
 */
public class PayJobHours {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayJobHours.class);


	/** The number of hours charged at 1.0 x the hourly rate. */
	private BigDecimal hours10 = BigDecimal.ZERO;

	/** The number of hours charged at 1.5 x the hourly rate. */
	private BigDecimal hours15 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 1 times the hourly rate. */
	private BigDecimal hoursCust1 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 2 times the hourly rate. */
	private BigDecimal hoursCust2 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 3 times the hourly rate. */
	private BigDecimal hoursCust3 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 4 times the hourly rate. */
	private BigDecimal hoursCust4 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 5 times the hourly rate. */
	private BigDecimal hoursCust5 = BigDecimal.ZERO;

	/** The number of hours charged at custom rate 6 times the hourly rate. */
	private BigDecimal hoursCust6 = BigDecimal.ZERO;

	/**
	 *
	 */
	public PayJobHours() {
	}

	/**
	 * Copy the hours values stored in a PayJobDaily into this instance.
	 *
	 * @param pjd The PayJobDaily source.
	 */
	public void copyFrom(PayJobDaily pjd) {
		hours10 = pjd.getHours10();
		hours15 = pjd.getHours15();
		hoursCust1 = pjd.getHoursCust1();
		hoursCust2 = pjd.getHoursCust2();
		hoursCust3 = pjd.getHoursCust3();
		hoursCust4 = pjd.getHoursCust4();
		hoursCust5 = pjd.getHoursCust5();
		hoursCust6 = pjd.getHoursCust6();
	}

	/**
	 * Add the hours values stored in a PayJobDaily to the hours stored in this
	 * instance. A "safe add" is done, so source and/or this may contain null
	 * hour values.
	 *
	 * @param pjd The PayJobDaily source.
	 */
	public void add(PayJobDaily pjd) {
		hours10 = NumberUtils.safeAdd(pjd.getHours10(), hours10);
		hours15 = NumberUtils.safeAdd(pjd.getHours15(), hours15);
		hoursCust1 = NumberUtils.safeAdd(pjd.getHoursCust1(), hoursCust1);
		hoursCust2 = NumberUtils.safeAdd(pjd.getHoursCust2(), hoursCust2);
		hoursCust3 = NumberUtils.safeAdd(pjd.getHoursCust3(), hoursCust3);
		hoursCust4 = NumberUtils.safeAdd(pjd.getHoursCust4(), hoursCust4);
		hoursCust5 = NumberUtils.safeAdd(pjd.getHoursCust5(), hoursCust5);
		hoursCust6 = NumberUtils.safeAdd(pjd.getHoursCust6(), hoursCust6);
	}

	/** See {@link #hours10}. */
	public BigDecimal getHours10() {
		return hours10;
	}
	/** See {@link #hours10}. */
	public void setHours10(BigDecimal hours10) {
		this.hours10 = hours10;
	}

	/** See {@link #hours15}. */
	public BigDecimal getHours15() {
		return hours15;
	}
	/** See {@link #hours15}. */
	public void setHours15(BigDecimal hours15) {
		this.hours15 = hours15;
	}

	/** See {@link #hoursCust1}. */
	public BigDecimal getHoursCust1() {
		return hoursCust1;
	}
	/** See {@link #hoursCust1}. */
	public void setHoursCust1(BigDecimal hoursCust1) {
		this.hoursCust1 = hoursCust1;
	}

	/** See {@link #hoursCust2}. */
	public BigDecimal getHoursCust2() {
		return hoursCust2;
	}
	/** See {@link #hoursCust2}. */
	public void setHoursCust2(BigDecimal hoursCust2) {
		this.hoursCust2 = hoursCust2;
	}

	/** See {@link #hoursCust3}. */
	public BigDecimal getHoursCust3() {
		return hoursCust3;
	}
	/** See {@link #hoursCust3}. */
	public void setHoursCust3(BigDecimal hoursCust3) {
		this.hoursCust3 = hoursCust3;
	}

	/** See {@link #hoursCust4}. */
	public BigDecimal getHoursCust4() {
		return hoursCust4;
	}
	/** See {@link #hoursCust4}. */
	public void setHoursCust4(BigDecimal hoursCust4) {
		this.hoursCust4 = hoursCust4;
	}

	/** See {@link #hoursCust5}. */
	public BigDecimal getHoursCust5() {
		return hoursCust5;
	}
	/** See {@link #hoursCust5}. */
	public void setHoursCust5(BigDecimal hoursCust5) {
		this.hoursCust5 = hoursCust5;
	}

	/** See {@link #hoursCust6}. */
	public BigDecimal getHoursCust6() {
		return hoursCust6;
	}
	/** See {@link #hoursCust6}. */
	public void setHoursCust6(BigDecimal hoursCust6) {
		this.hoursCust6 = hoursCust6;
	}

}
