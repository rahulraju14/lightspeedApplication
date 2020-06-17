/**
 * File: TimesheetEntry.java
 */
package com.lightspeedeps.object;

import java.math.BigDecimal;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.WeeklyTimecard;

/**
 * This object represents a line-item on the (Tours) Timesheet page. It contains
 * information related to a specific {@link WeeklyTimecard} object, and includes
 * a reference to that object. It is not related to any database table (it is
 * not persisted).
 */
public class TimesheetEntry extends TimecardRow implements Comparable<TimesheetEntry> {
	/** */
	private static final long serialVersionUID = 4449573521305342010L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimesheetEntry.class);

	/** Reimbursement - from expense record (which case from StartForm) */
	private BigDecimal payCategory1Amount;

	/** Cash Advance - from expense record (which case from StartForm) */
	private BigDecimal payCategory2Amount;

	/** Per Diem non-taxable - from expense record (which case from StartForm) */
	private BigDecimal payCategory3Amount;

	/** PerDiem taxable - from expense record (which case from StartForm) */
	private BigDecimal payCategory4Amount;

	private static Comparator<TimesheetEntry> nameComparator = null;

	public TimesheetEntry() {
		// default constructor
	}

	public TimesheetEntry(WeeklyTimecard wtc) {
		super(wtc);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getWeeklyTc() == null) ? 0 : getWeeklyTc().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		boolean b = true;
		if (this != o) {
			b = false;
			try {
				TimesheetEntry c = (TimesheetEntry)o;
				if (c != null) {
					if (getWeeklyTc() != null && c.getWeeklyTc() != null) {
						b = getWeeklyTc().equals(c.getWeeklyTc());
					}
				}
			}
			catch(Exception e) {
			}
		}
		return b;
	}

	@Override
	public int compareTo(TimesheetEntry o) {
		return compareTimecard(o);
	}

	/**
	 * Compare using the specified field, with the specified ordering.
	 *
	 * @param other The TimecardEntry to compare to this one.
	 * @param sortField One of the statically defined sort-key values.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(TimesheetEntry other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 * @param other The TimecardEntry to compare to this one.
	 * @param sortField One of the statically defined sort-key values.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(TimesheetEntry other, String sortField) {
		int ret = 0;
		ret = getWeeklyTc().compareTo(other.getWeeklyTc(), sortField);
		return ret;
	}

	/**
	 * @return A Comparator which will sort TimesheetEntry`s based on the
	 *         employee name within the associated WeeklyTimecard.
	 */
	public static Comparator<TimesheetEntry> getNameComparator() {
		if (nameComparator == null) {
			nameComparator = new Comparator<TimesheetEntry>() {
				@Override
				public int compare(TimesheetEntry c1, TimesheetEntry c2) {
					int ret;
					ret = c1.compareTo(c2, WeeklyTimecard.SORTKEY_DEPT, true);
					 if (ret == 0) {
						 ret = c1.compareTo(c2, WeeklyTimecard.SORTKEY_NAME, true);
					 }
					 return ret;
				}
			};
		}
		return nameComparator;
	}

	/** See {@link #payCategory1Amount}. */
	public BigDecimal getPayCategory1Amount() {
		return payCategory1Amount;
	}

	/** See {@link #payCategory1Amount}. */
	public void setPayCategory1Amount(BigDecimal payCategory1Amount) {
		this.payCategory1Amount = payCategory1Amount;
	}

	/** See {@link #payCategory2Amount}. */
	public BigDecimal getPayCategory2Amount() {
		return payCategory2Amount;
	}

	/** See {@link #payCategory2Amount}. */
	public void setPayCategory2Amount(BigDecimal payCategory2Amount) {
		this.payCategory2Amount = payCategory2Amount;
	}

	/** See {@link #payCategory3Amount}. */
	public BigDecimal getPayCategory3Amount() {
		return payCategory3Amount;
	}

	/** See {@link #payCategory3Amount}. */
	public void setPayCategory3Amount(BigDecimal payCategory3Amount) {
		this.payCategory3Amount = payCategory3Amount;
	}

	/** See {@link #payCategory4Amount}. */
	public BigDecimal getPayCategory4Amount() {
		return payCategory4Amount;
	}

	/** See {@link #payCategory4Amount}. */
	public void setPayCategory4Amount(BigDecimal payCategory4Amount) {
		this.payCategory4Amount = payCategory4Amount;
	}

}
