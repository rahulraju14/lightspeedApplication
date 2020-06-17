/**
 * File: DatePair.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Transient;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * A class to hold a pair of dates denoting a range. Used for Day-out-of-Day (DooD)
 * calculations.
 * ("DateRange" was already in use as a database table.)
 */
public class DatePair implements Serializable {
	/** */
	private static final long serialVersionUID = - 92156256893723250L;

	private static final String DATE_FORMAT = "MM/dd/yy";

	/** The starting date (inclusive) of the range. */
	private Date startDate;
	/** The ending date (inclusive) of the range. */
	private Date endDate;

	/** The number of days included in the range. */
	@Transient
	private Integer span;

	@Transient
	private String selectLabel;


	/**
	 * Default constructor
	 */
	public DatePair() {
		span = 0;
	}

	/**
	 * A useful constructor.
	 */
	public DatePair(Date from, Date to) {
		startDate = from;
		endDate = to;
	}


	private int calculateSpan() {
		if (startDate == null || endDate == null) {
			return 0;
		}
		return CalendarUtils.durationInDays(startDate, endDate) + 1;
	}

	private String createSelectLabel() {
		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

		String s1 = sdf.format(startDate);
		String s2 = sdf.format(endDate);
		return MsgUtils.formatMessage("DatePair.ListFormat", s1, s2, getSpan());
	}


	/** See {@link #startDate}. */
	public Date getStartDate() {
		return startDate;
	}
	/** See {@link #startDate}. */
	public void setStartDate(Date date) {
		span = null;
		selectLabel = null;
		startDate = date;
	}

	/** See {@link #endDate}. */
	public Date getEndDate() {
		return endDate;
	}
	/** See {@link #endDate}. */
	public void setEndDate(Date shootTime) {
		span = null;
		selectLabel = null;
		endDate = shootTime;
	}

	/** See {@link #span}. */
	public int getSpan() {
		if (span == null) {
			span = calculateSpan();
		}
		return span;
	}

	/** See {@link #selectLabel}. */
	public String getSelectLabel() {
		if (selectLabel == null) {
			selectLabel = createSelectLabel();
		}
		return selectLabel;
	}

	@Override
	public String toString() {
		return getSelectLabel();
	}

}
