/**
 * File: ReportStatusItem.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;

import com.lightspeedeps.type.ReportType;


/**
 * Used to hold the status of one type of Report for
 * presentation on the Home page.  (See HomePageBean.java)
 */
public class ReportStatusItem implements Comparable<ReportStatusItem>, Serializable {
	/** */
	private static final long serialVersionUID = - 1017751482914254540L;
	private ReportType type;
	private Date date;
	private boolean overdue = false;

	public ReportStatusItem(ReportType pType, Date pDate, boolean pOverdue) {
		type = pType;
		date = pDate;
		overdue = pOverdue;
	}

	public ReportStatusItem(ReportType pType) {
		type = pType;
	}

	/** See {@link #type}. */
	public ReportType getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(ReportType type) {
		this.type = type;
	}

	/** See {@link #date}. */
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #overdue}. */
	public boolean isOverdue() {
		return overdue;
	}
	/** See {@link #overdue}. */
	public void setOverdue(boolean overdue) {
		this.overdue = overdue;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportStatusItem other = (ReportStatusItem)obj;
		if (date == null && other.date != null)
			return false;
		if ( ! date.equals(other.date))
			return false;
		if (type == null && other.type != null)
			return false;
		return type.equals(other.type);
	}

	@Override
	public int compareTo(ReportStatusItem other) {
		if (other == null) {
			return 1;
		}
		if (equals(other)) {
			return 0;
		}
		int comp = date.compareTo(other.getDate());
		if (comp != 0) {
			return comp;
		}
		return getType().compareTo(other.getType());
	}

}
