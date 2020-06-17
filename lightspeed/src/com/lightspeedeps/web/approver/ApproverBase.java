/**
 * File: ApproverBase.java
 */
package com.lightspeedeps.web.approver;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.icu.util.Calendar;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.timecard.TimecardBase;
import com.lightspeedeps.web.view.ListView;

/**
 * Base class for the Approver Dashboard in both desktop and mobile versions.
 */
public abstract class ApproverBase extends ListView {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ApproverBase.class);

	private static final long serialVersionUID = 1L;

	private static final String APPROVER_ATTRIBUTE_PREFIX = "Approver";

	/** The currently selected weekend date. Note that subclasses of ApproverFilterBase
	 * don't reference this field -- they are redirected to FilterBean.weekEndDate. */
	private Date weekEndDate;

	/** The last day of the week on a weekly timecard for the current production
	 * or project. Defaults to Saturday (=7); Sunday = 1. */
	private Integer weekEndDay;

	/** The production for which we are displaying time cards, StartForm`s or batches. */
	protected Production production;

	/** The current Project, which may restrict the StartForm`s, WeeklyTimecard`s
	 * or batches displayed. For TV & Feature productions, this will be null.
	 * For Commercial productions, it will be the currently selected project. */
	protected Project project;

	/** The timecard of the currently selected row. */
	protected WeeklyTimecard weeklyTimecard;

	/** The currently selected entry in the list. The TimecardEntry holds information
	 * beyond what's in the WeeklyTimecard, for ease of display and (in some cases)
	 * improved performance so we aren't accessing the database so much each time the user
	 * picks a different row to view. */
	protected TimecardEntry timecardEntry;

	/** The list of time cards currently displayed. */
	protected List<TimecardEntry> timecardEntryList;

	/** True iff the current user has the Edit-HTG permission in the current project. */
	private boolean userHasEditHtg;

	/** True iff the current user has the View-HTG permission in the current project. */
	private boolean userHasViewHtg;

	/** DAO for WeeklyTimecard */
	private transient WeeklyTimecardDAO weeklyTimecardDAO;

	protected abstract void createTimecardList();

	public ApproverBase(String defaultSortColumn) {
		super(defaultSortColumn, TimecardBase.TIMECARD_MESSAGE_PREFIX);
		setAttributePrefix(APPROVER_ATTRIBUTE_PREFIX);

		production = SessionUtils.getNonSystemProduction();

		if (production != null && production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}

		weekEndDate = SessionUtils.getDate(Constants.ATTR_APPROVER_DATE);
		if (weekEndDate == null) {
			weekEndDate = TimecardUtils.calculateDefaultWeekEndDate(Calendar.SATURDAY);
		}

		AuthorizationBean authBean = AuthorizationBean.getInstance();
		userHasEditHtg = authBean.hasPageField(Constants.PGKEY_EDIT_HTG);
		userHasViewHtg = authBean.hasPageField(Constants.PGKEY_VIEW_HTG);

	}

	/**
	 * Verify that all the selected entries on the given timecardEntryList are
	 * ready for approval by the current user.
	 *
	 * @param tcEntryList List of TimecardEntry to be scanned for selected items.
	 * @param contactIdList If non-null, then an empty list of Integers. On
	 *            return, the list will contain the contact.id values of all the
	 *            next approvers of the selected timecards.
	 * @return - null if no valid (ready for approval) timecards were found; or <br/>
	 *         - a zero-length String if everything is ok; or <br/>
	 *         - non-blank String if some final-approved timecards were found,
	 *         in which case the String contains the message to be added to the
	 *         approval prompt dialog box.
	 */
	protected String validateApproveList(List<TimecardEntry> tcEntryList, List<Integer> contactIdList) {
		WeeklyTimecard wtc = null;
		String msg = ""; // normal response
		int readyTc = 0;
		int finalTc = 0;
		Integer currentId = getvContact().getId();
		for (TimecardEntry tce : tcEntryList) {
			if (tce.getChecked() || tce.getWeeklyTc().getMarkedForApproval()) {
				wtc = tce.getWeeklyTc();
				wtc = getWeeklyTimecardDAO().refresh(wtc);
				tce.setWeeklyTc(wtc);
				if (wtc.getApproverId() == null) {
					if (tce.getChecked()) {
						finalTc++;
					}
					continue;	// ignore it - marked by another user, or in Final approval state
				}
				else if (! ApproverUtils.isNextApprover(wtc, currentId)) {
					// oops! Current user is not the next approver!
					ApproverUtils.calculateApprovalStatus(tce, SessionUtils.getCurrentContact());
					/*
					 * At this point, we can't tell the difference between (a) a timecard that was
					 * in the current user's queue and which he marked for approval and then was
					 * taken from him before he clicked Approve, and (b) a timecard that has been
					 * marked for approval by some other user and just happens to be in the current
					 * user's displayed list.  So we assume (for now) that it's case (b), and just
					 * ignore it.
					 */
					continue;
				}
				readyTc++;
				if (contactIdList != null) {
					Integer id = TimecardUtils.findNextApproverContactId(wtc);
					contactIdList.add(id);
				}
			}
		}
		if (readyTc == 0) {
			MsgUtils.addFacesMessage("Approval.NoneReady", FacesMessage.SEVERITY_ERROR);
			msg = null; // error response
		}
		else if (finalTc > 0) {
			// ok to proceed, but warn user some timecards will be ignored
			msg = MsgUtils.formatMessage("Approval.Approve.SomeFinal", finalTc);
		}
		return msg;
	}

	/**
	 * Approve all the "checked" entries in the current timecardEntryList.
	 */
	protected void approveList() {
		Contact apprContact = SessionUtils.getCurrentContact();
		ApprovePromptBean bean = ApprovePromptBean.getInstance();
		boolean multiple = bean.getMultipleApprovers();
		if (! multiple) {
			Integer nextAppContactId = bean.getApproverContactId();
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getStatus().isReady() && tce.getWeeklyTc().getMarkedForApproval()) {
					// Skip timecards not in this user's approval queue
					if (! approveEntry(tce, apprContact, nextAppContactId)) {
						MsgUtils.addFacesMessage("Approval.OneNotReady", FacesMessage.SEVERITY_ERROR);
						break;
					}
				}
			}
		}
		else {
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getStatus().isReady() && tce.getWeeklyTc().getMarkedForApproval()) {
					// Skip timecards not in this user's approval queue
					if (! approveEntry(tce, apprContact, TimecardUtils.findNextApproverContactId(tce.getWeeklyTc()))) {
						MsgUtils.addFacesMessage("Approval.OneNotReady", FacesMessage.SEVERITY_ERROR);
						break;
					}
				}
			}
		}
	}

	/**
	 * Do the approval process for one timecard.
	 *
	 * @param tce The TimecardEntry containing the timecard to be approved.
	 * @param apprContact The Contact of the current approver.
	 * @param nextAppContactId The Contact.id of the next approver; this may be
	 *            an out-of-line approver if the user has selected a next
	 *            approver other than the normal one in the chain.
	 * @return True iff the approval was successful. The approval can fail if
	 *         another user has changed the status of the timecard in the
	 *         meantime, e.g., by a "pull" or "recall".
	 */
	private boolean approveEntry(TimecardEntry tce, Contact apprContact, Integer nextAppContactId) {
		WeeklyTimecard wtc = tce.getWeeklyTc();
		wtc = getWeeklyTimecardDAO().approve(wtc, nextAppContactId);
		if (wtc == null) { // approval did not complete!
			return false;
		}
		tce.setWeeklyTc(wtc);
		ApproverUtils.calculateApprovalStatus(tce, apprContact);
		TimecardCalc.calculateWeeklyTotals(wtc);
		return true;
	}

	/**
	 * Mark the WeeklyTimecard associated with the give TimecardEntry as
	 * "Locked" by payroll. (This is not the "edit lock" setting.)
	 *
	 * @param tce The TimecardEntry that references the WeeklyTimecard to be
	 *            locked.
	 * @param locker The Contact representing the person locking the timecard.
	 */
	/* package */ boolean lockEntry(TimecardEntry tce, Contact locker) {
		WeeklyTimecard wtc = tce.getWeeklyTc();
		wtc = getWeeklyTimecardDAO().lockStatus(wtc);
		if (wtc == null) {
			return false;
		}
		tce.setWeeklyTc(wtc);
		tce.setChecked(false);
		ApproverUtils.calculateApprovalStatus(tce, locker);
		TimecardCalc.calculateWeeklyTotals(wtc);
		return true;
	}

	/**
	 * Create the list of time cards to display for the mobile approver
	 * dashboard.
	 *
	 * @param weDate The week-ending date to select on, or null if no date
	 *            selection should be done.
	 * @param anyBatch If true, then a timecard with any batch (or no batch) is
	 *            allowed, and the 'batch' parameter is ignored. If false, then
	 *            the 'batch' parameter is respected.
	 * @param batch The WeeklyBatch that the timecard should belong to, or null
	 *            if only un-batched timecards are to be included.
	 * @param acct The accountNumber to select on, or null if no selection
	 *            should be done by account (employee).
	 */
	protected void createTimecardListAllDepts(Date weDate, boolean anyBatch, WeeklyBatch batch, String acct) {
		timecardEntryList = TimecardUtils.createTimecardListAllDepts(production, project, weDate, true,
				anyBatch, batch, null, acct, getvContact(), userHasViewHtg, false);
	}

	/**
	 * Count how many timecards have their check-box selected. Be careful not to
	 * count ones that have been marked by a different user!
	 *
	 * @return Non-negative count.
	 */
	public int getSelectedItemCount() {
		int items = 0;
		for (TimecardEntry tce : getTimecardEntryList()) {
			if (tce.getStatus().isReady() &&
					tce.getWeeklyTc().getMarkedForApproval()) {
				items++;
			}
			else if ((tce.getStatus() == ApprovalStatus.APPROVED) && tce.getChecked()) {
				items++;
			}
		}
		return items;
	}

	/**
	 * Create the list of "week-end dates" that the user will be able to choose
	 * from in the drop-down list on the desktop Approval Dashboard or via the
	 * left/right arrows on the mobile time card pages, or in the Print
	 * Timecards pop-up's date selection.
	 *
	 * @param listDate The date that the current timecard-list is generated
	 *            from; we need to be sure this date is in the list of week-end
	 *            dates. It might not be if the "first payroll date" in the
	 *            Production is set too late.
	 * @param allProjects If true then include dates that cover timecards for
	 *            all projects instead of just the current project. This
	 *            parameter is only applicable to Commercial productions; it is
	 *            ignored for TV/Feature productions.
	 * @return A non-null List of SelectItem`s; in each SelectItem, the value is
	 *         a Date object, and the label is the corresponding mm/dd/yyyy
	 *         representation.
	 */
	protected List<SelectItem> createEndDateList(Date listDate, boolean allProjects) {

		// LS-2173 first determine if drop-down will use Saturday dates or not
		boolean useWeekdayPref = true; // assume using project's WeekFirstDay preference
		if (allProjects && getProduction().getType().isAicp() && getProject() != null) {
			// only need to check if showing all projects for Commercial-style (where projects may have different WeekFirstDay prefs)
			int currWeekFirstDay = getProject().getPayrollPref().getWeekFirstDay();
			if (currWeekFirstDay != Calendar.SUNDAY) {
				// don't need to check if the WeekFirstDay is Sunday, as that means W/E day is Saturday, the default
				if (production.getProjects().size() > 1) {
					// don't need to check if only one project -- it's ok to use WeekFirstDay
					// Ok, need to check if all projects have the same WeekFirstDay or not.
					int pp = PayrollPreferenceDAO.getInstance().findCountWeekFirstDay(production);
					if (pp > 1) {
						// not all projects have the same WeekFirstDay, so force Saturday dates for drop-down
						useWeekdayPref = false;
					}
				}
			}
		}

		return TimecardUtils.createEndDateList(production, project, listDate, allProjects, useWeekdayPref);
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(Object,boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((TimecardEntry)item).getWeeklyTc().setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The comparator for sorting the list of TimecardEntry's. Note that it
	 * actually invokes the compareTo function on the respective WeeklyTimecards
	 * represented by each TimecardEntry.
	 *
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<TimecardEntry> getComparator() {
		Comparator<TimecardEntry> comparator = new Comparator<TimecardEntry>() {
			@Override
			public int compare(TimecardEntry c1, TimecardEntry c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#isDefaultAscending(String)
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (sortColumn.equals(WeeklyTimecard.SORTKEY_DATE) || sortColumn.equals(WeeklyTimecard.SORTKEY_HOURS)) {
			return false; // Date & hours default to descending
		}
		return true; // make columns default to ascending sort
	}

	/**See {@link #production}. */
	public Production getProduction() {
		return production;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}

	/** See {@link #userHasEditHtg}. */
	public boolean getUserHasEditHtg() {
		return userHasEditHtg;
	}
	/** See {@link #userHasEditHtg}. */
	public void setUserHasEditHtg(boolean userHasEditHtg) {
		this.userHasEditHtg = userHasEditHtg;
	}

	/**See {@link #userHasViewHtg}. */
	public boolean getUserHasViewHtg() {
		return userHasViewHtg;
	}

	/**See {@link #userHasViewHtg}. */
	public void setUserHasViewHtg(boolean userHasViewHtg) {
		this.userHasViewHtg = userHasViewHtg;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	@Override
	public List getItemList() {
		return getTimecardEntryList();
	}

	public List<TimecardEntry> getTimecardEntryList() {
		if (timecardEntryList == null) {
			createTimecardList();
		}
		sortIfNeeded(); // only sort if the sort column or ordering has changed.
		return timecardEntryList;
	}
	public void setTimecardEntryList(List<TimecardEntry> timecardEntryList) {
		this.timecardEntryList = timecardEntryList;
	}

	/** See {@link #weekEndDay}. */
	public Integer getWeekEndDay() {
		if (weekEndDay == null) {
			weekEndDay = TimecardUtils.findWeekEndDay();
		}
		return weekEndDay;
	}
	/** See {@link #weekEndDay}. */
	public void setWeekEndDay(Integer weekEndDay) {
		this.weekEndDay = weekEndDay;
	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}
	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date archiveDate) {
		weekEndDate = archiveDate;
	}

	/** See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #timecardEntry}. */
	public TimecardEntry getTimecardEntry() {
		return timecardEntry;
	}
	/**See {@link #timecardEntry}. */
	public void setTimecardEntry(TimecardEntry timecardEntry) {
		this.timecardEntry = timecardEntry;
	}

	/**See {@link #weeklyTimecardDAO}. */
	protected WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}

}
