/**
 * File: TimecardEntry.java
 */
package com.lightspeedeps.object;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * This object represents a line-item on the Timecard Review and Approval page
 * ("Approval Dashboard"). It contains information related to a specific
 * {@link WeeklyTimecard} object, and includes a reference to that object. It is
 * not related to any database table (it is not persisted).
 */
public class TimecardEntry extends TimecardRow implements Comparable<TimecardEntry> {
	/** */
	private static final long serialVersionUID = 7636847583515708162L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimecardEntry.class);

	private static Comparator<TimecardEntry> nameComparator = null;

	/** The Contact object that matches the employee on the timecard. */
	private Contact contact;

	/** The name of the related Department (not currently set or used). */
	private String departmentName;

	/** The status of the entry, which may be affected by the user viewing it, e.g., if
	 * the timecard is ready to be approved/rejected by the current user or not. */
	private ApprovalStatus status;

	/** The name of the next approver for the related timecard. */
	private String approverName = ""; // avoid null comparisons during sort

	/** True iff the daily hours should be displayed on the Approver / Timecard review page. Used to
	 * hide hours when the week-ending day floats and does not match the current project's week-ending day. */
	private boolean showDailyHours = true;


	public TimecardEntry() {
		// default constructor
	}

	public TimecardEntry(WeeklyTimecard wtc) {
		super(wtc);
		departmentName = "TBD";
		status = wtc.getStatus();
	}

	/**See {@link #contact}. This method does a database lookup if the contact
	 * value has not yet been set. */
	public Contact getContact() {
		if (contact == null && getWeeklyTc() != null) {
			contact = ContactDAO.getInstance().findByAccountNumAndProduction(getWeeklyTc().getUserAccount(),
					SessionUtils.getNonSystemProduction());
		}
		return contact;
	}
	/**See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #departmentName}. */
	public String getDepartmentName() {
		return departmentName;
	}
	/** See {@link #departmentName}. */
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	/** See {@link #status}. */
	public ApprovalStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}

	/** See {@link #approverName}. */
	public String getApproverName() {
		return approverName;
	}
	/** See {@link #approverName}. */
	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	/** See {@link #showDailyHours}. */
	public boolean getShowDailyHours() {
		return showDailyHours;
	}
	/** See {@link #showDailyHours}. */
	public void setShowDailyHours(boolean showDailyHours) {
		this.showDailyHours = showDailyHours;
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
				TimecardEntry c = (TimecardEntry)o;
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
	public int compareTo(TimecardEntry o) {
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
	public int compareTo(TimecardEntry other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 * @param other The TimecardEntry to compare to this one.
	 * @param sortField One of the statically defined sort-key values.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(TimecardEntry other, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by default later
		}
		else if (sortField.equals(WeeklyTimecard.SORTKEY_STATUS) ) {
			ret = getStatus().compareTo(other.getStatus());
		}
		else if (sortField.equals(WeeklyTimecard.SORTKEY_WAITING_FOR) ) {
			ret = StringUtils.compare(getApproverName(), other.getApproverName());
			if (ret != 0 &&
					((getApproverName() != null && getApproverName().charAt(0) < 'A') ||
					(other.getApproverName() != null && other.getApproverName().charAt(0) < 'A'))) {
				// one (or both) fields probably start with HTML like <i>, we want them to sort later...
				ret = - ret; // so just reverse sort comparison result.
			}
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			ret = getWeeklyTc().compareTo(other.getWeeklyTc(), sortField);
		}
		return ret;
	}

	/**
	 * @return A Comparator which will sort TimecardEntry`s based on the
	 *         employee name within the associated WeeklyTimecard.
	 */
	public static Comparator<TimecardEntry> getNameComparator() {
		if (nameComparator == null) {
			nameComparator = new Comparator<TimecardEntry>() {
				@Override
				public int compare(TimecardEntry c1, TimecardEntry c2) {
					return c1.compareTo(c2, WeeklyTimecard.SORTKEY_NAME, true);
				}
			};
		}
		return nameComparator;
	}

}
