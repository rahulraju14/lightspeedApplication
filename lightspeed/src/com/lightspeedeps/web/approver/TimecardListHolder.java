/**
 * TimecardListHolder.java
 */
package com.lightspeedeps.web.approver;

import java.util.Date;
import java.util.List;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.type.ApprovableStatusFilter;

/**
 * This defines those methods required to support the creation of timecard lists
 * within either the Full Timecard page or Approver Dashboard page.
 */
public interface TimecardListHolder {

	/** Get the Production containing the currently viewed timecards. */
	public Production getProduction();

	/** Get the Project associated with the currently viewed timecards; if
	 * null, then all timecards associated with the Production will be
	 * visible.  Only used for Commercial productions. */
	public Project getProject();

	/** Get the Contact of the currently logged-in user. */
	public Contact getvContact();

	/** True iff the current user has the View_HTG permission. */
	public boolean getUserHasViewHtg();

	/** The method to be called to sort the List of TimecardEntry after
	 * it has been created. */
	public void sortTimecards(List<TimecardEntry> timecardEntryList);

	// * * * FILTER VALUE ACCESS METHODS

	/** Get the currently selected weeklyBatch id (for filtering).  If null
	 *  or zero, then no filtering based on batch is done. */
	public Integer getBatchId();
	/** Set the currently selected weeklyBatch id (for filtering).  */
	public void setBatchId(Integer batchId);

	/** Get the currently selected department id (for filtering). If null or
	 * zero, then no filtering based on department is done. */
	public Integer getDepartmentId();
	/** Set the currently selected department id (for filtering). */
	public void setDepartmentId(Integer deptId);

	/** Get the LightSPEED account number to be used for filtering the list
	 * of timecards displayed.  If null, then no filtering based on employee
	 * is done. */
	public String getEmployeeAccount();

	/** Get the WeeklyStatusFilter value currently being used to filter
	 * the list of timecards displayed. */
	public ApprovableStatusFilter getStatusFilter();
	/** Set the WeeklyStatusFilter value currently being used to filter
	 * the list of timecards displayed. */
	public void setStatusFilter(ApprovableStatusFilter status);

	/** Get the current week-ending date (for filtering).  This value may be
	 * null or Constants.SELECT_ALL_DATE to indicate no filtering by date. */
	public Date getWeekEndDate();

	/** True iff the user has elected to show timecards or StartForm`s for all
	 * projects in the case of a Commercial (AICP) production.  This option is
	 * only available to users with Financial Data Admin permission.*/
	public boolean getShowAllProjects();

}
