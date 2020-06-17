package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.port.Exporter;

/**
 * PayBreakdownDaily entity. Each instance is a detail line in the PayBreakdownDaily
 * table within a WeeklyTimecard. This entity extends the PayBreakdown class by
 * adding a Date to each line item.  These entries are generated for those Productions
 * (timecards) whose breakdown will be exported to a payroll service as a data file.
 */
@Entity
@Table(name = "pay_breakdown_daily")
public class PayBreakdownDaily extends PayBreakdownMapped {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayBreakdownDaily.class);
	private static final long serialVersionUID = -3065030383864320410L;

	// Fields

	/** The date this entry applies to. The timecard's week-ending date may be used for
	 * entries that are not specific to a single date, e.g., allowances. */
	private Date date;

	// Constructors

	/** default constructor */
	public PayBreakdownDaily() {
	}

//	/** minimal constructor */
//	public PayBreakdownDaily(WeeklyTimecard wtc, byte lineNumber) {
//		super(wtc, lineNumber);
//	}

	/** typical constructor */
	public PayBreakdownDaily(WeeklyTimecard wtc, byte lineNumber, Date pDate) {
		super(wtc, lineNumber);
		date = pDate;
	}

	/** See {@link #date}. */
	@Override
	@Temporal(TemporalType.DATE)
	@Column(name = "Date", nullable = true, length = 10)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be
	 * transmitted to other services capable of importing it.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param pCast True if this is for a Cast-department timecard; this
	 *            affects the mapping (translation) of some PayCategory values. (LS-1140)
	 * @param pModelRelease True if this is for a model-release-based timecard. (LS-4664)
	 */
	@Override
	public void exportTabbed(Exporter ex, boolean pCast, boolean pModelRelease) {
//		ex.append(getId());
//		ex.append(getWeeklyId());
//		ex.append(getLineNumber());
		ex.append(getDate());
		super.exportTabbed(ex, pCast, pModelRelease);
	}

}