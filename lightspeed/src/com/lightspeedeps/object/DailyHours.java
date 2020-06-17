/**
 * File: DailyHours.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Dpr;
import com.lightspeedeps.model.ReportTime;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * This is used to back each line-item in the DPR section of the Timecard Review
 * and Approval page. It holds the call, wrap, and meal times for one day.
 */
public class DailyHours implements Serializable {
	/** */
	private static final long serialVersionUID = - 8895337281700914570L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DailyHours.class);

	/** The date this data corresponds to. */
	private Date date;

	/** True iff a DPR exists that was used to populate this entity. */
	private boolean dprExists;

	/** True iff a matching contact was found. */
	private boolean contactMatch;

	/** True iff this data corresponds to a published DPR. */
	private boolean published;

//	/** True iff this data corresponds to a person whose entry was not
//	 * found in the matching DPR. */
//	private boolean missingPerson = true;

	/** The call time. */
	private BigDecimal callTime;

	/** The time out (start) for meal 1. */
	private BigDecimal m1Out;

	/** The time in (end) for meal 1. */
	private BigDecimal m1In;

	/** The time out (start) for meal 2. */
	private BigDecimal m2Out;

	/** The time in (end) for meal 2. */
	private BigDecimal m2In;

	/** The wrap (end of day) time. */
	private BigDecimal wrap;

	/** The calculated number of working hours for this day. */
	private BigDecimal hours;

	/** Number of meal-period violations (MPVs) in the a.m. */
	private Byte mpv1;
	/** Number of meal-period violations (MPVs) in the p.m. */
	private Byte mpv2;

	public DailyHours() {
		// default constructor
	}

	/**
	 * Construct a DailyHours with the information extracted from the given DPR.
	 * @param dpr
	 */
	public DailyHours(Dpr dpr) {
		published = (dpr.getStatus() == ReportStatus.PUBLISHED);
		dprExists = true;
		date = dpr.getDate();
		m1Out = CalendarUtils.convertTimeToDecimal(dpr.getFirstMealBegin(), 2);
		m1In = CalendarUtils.convertTimeToDecimal(dpr.getFirstMealEnd(), 2);
		m2Out = CalendarUtils.convertTimeToDecimal(dpr.getSecondMealBegin(), 2);
		m2In = CalendarUtils.convertTimeToDecimal(dpr.getSecondMealEnd(), 2);

		BigDecimal call = CalendarUtils.convertTimeToDecimal(dpr.getCrewCall(), 2);
		if (call != null) {
			if (m1Out != null && m1Out.compareTo(call) < 0) {
				m1Out = m1Out.add(Constants.HOURS_IN_A_DAY);
			}
			if (m1In != null && m1In.compareTo(call) < 0) {
				m1In = m1In.add(Constants.HOURS_IN_A_DAY);
			}
			if (m2Out != null && m2Out.compareTo(call) < 0) {
				m2Out = m2Out.add(Constants.HOURS_IN_A_DAY);
			}
			if (m2In != null && m2In.compareTo(call) < 0) {
				m2In = m2In.add(Constants.HOURS_IN_A_DAY);
			}
		}
	}

	/**
	 * Calculate the number of working hours described by this DailyHour object.
	 * 'hours' will be set to the appropriate value, or to null if the value
	 * cannot be calculated due to either call or wrap times being null.
	 */
	public void calcHours() {
		setHours(null);
		if (getCallTime() != null && getWrap() != null) {
			BigDecimal hoursVal = getWrap().subtract(getCallTime());
			if (getM1In() != null && getM1Out() != null &&
					getM1Out().compareTo(getCallTime()) >= 0 && getM1In().compareTo(getWrap()) <= 0) {
				hoursVal = hoursVal.subtract(getM1In().subtract(getM1Out()));
			}
			if (getM2In() != null && getM2Out() != null &&
					getM2Out().compareTo(getCallTime()) >= 0 && getM2In().compareTo(getWrap()) <= 0) {
				hoursVal = hoursVal.subtract(getM2In().subtract(getM2Out()));
			}
			setHours(hoursVal);
		}

	}

	public boolean getOnCall() {
		return (getCallTime() != null) && (ReportTime.OC_TIME.compareTo(callTime) == 0);
	}

	/** See {@link #date}. */
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/**See {@link #dprExists}. */
	public boolean getDprExists() {
		return dprExists;
	}
	/**See {@link #dprExists}. */
	public void setDprExists(boolean dprExists) {
		this.dprExists = dprExists;
	}

	/**See {@link #contactMatch}. */
	public boolean getContactMatch() {
		return contactMatch;
	}
	/**See {@link #contactMatch}. */
	public void setContactMatch(boolean contactMatch) {
		this.contactMatch = contactMatch;
	}

	/** See {@link #published}. */
	public boolean getPublished() {
		return published;
	}
	/** See {@link #published}. */
	public void setPublished(boolean published) {
		this.published = published;
	}

//	/** See {@link #missingPerson}. */
//	public boolean getMissingPerson() {
//		return missingPerson;
//	}
//	/** See {@link #missingPerson}. */
//	public void setMissingPerson(boolean missingPerson) {
//		this.missingPerson = missingPerson;
//	}

	/** See {@link #callTime}. */
	public BigDecimal getCallTime() {
		return callTime;
	}
	/** See {@link #callTime}. */
	public void setCallTime(BigDecimal callTime) {
		this.callTime = callTime;
	}

	/** See {@link #m1Out}. */
	public BigDecimal getM1Out() {
		return m1Out;
	}
	/** See {@link #m1Out}. */
	public void setM1Out(BigDecimal m1Out) {
		this.m1Out = m1Out;
	}

	/** See {@link #m1In}. */
	public BigDecimal getM1In() {
		return m1In;
	}
	/** See {@link #m1In}. */
	public void setM1In(BigDecimal m1In) {
		this.m1In = m1In;
	}

	/** See {@link #m2Out}. */
	public BigDecimal getM2Out() {
		return m2Out;
	}
	/** See {@link #m2Out}. */
	public void setM2Out(BigDecimal m2Out) {
		this.m2Out = m2Out;
	}

	/** See {@link #m2In}. */
	public BigDecimal getM2In() {
		return m2In;
	}
	/** See {@link #m2In}. */
	public void setM2In(BigDecimal m2In) {
		this.m2In = m2In;
	}

	/** See {@link #wrap}. */
	public BigDecimal getWrap() {
		return wrap;
	}
	/** See {@link #wrap}. */
	public void setWrap(BigDecimal wrap) {
		this.wrap = wrap;
	}

	/** See {@link #hours}. */
	public BigDecimal getHours() {
		return hours;
	}
	/** See {@link #hours}. */
	public void setHours(BigDecimal hours) {
		this.hours = hours;
	}

	/** See {@link #mpv1}. */
	public Byte getMpv1() {
		return mpv1;
	}
	/** See {@link #mpv1}. */
	public void setMpv1(Byte mpv) {
		mpv1 = mpv;
	}

	/** See {@link #mpv2}. */
	public Byte getMpv2() {
		return mpv2;
	}
	/** See {@link #mpv2}. */
	public void setMpv2(Byte mpv2) {
		this.mpv2 = mpv2;
	}

}
