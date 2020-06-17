/**
 * File: TimecardSelectBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.type.TimecardSelectionType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class extends our basic PopupBean by adding the facility to select
 * various groups of timecards. It is a superclass of the beans that back the
 * print, export, and "run HTG" dialogs.
 */
public class TimecardSelectBase extends PopupBean {
	/** */
	private static final long serialVersionUID = - 3777641684679821504L;

	private static final Log log = LogFactory.getLog(TimecardSelectBase.class);

	/** Indicates the print range selected */
	protected String rangeSelection = RANGE_CURRENT;
	public static final String RANGE_CURRENT = "c";
	public static final String RANGE_PERSON = "p";
	public static final String RANGE_SELECT = "s";
	public static final String RANGE_DEPT = "d";
	public static final String RANGE_BATCH = "b";
	public static final String RANGE_UNBATCHED = "u";
	public static final String RANGE_ALL = "a";

	/** Indicates the sort order selected */
	private String sortOrder = ORDER_DATE;
	public static final String ORDER_ACCT = "a";
	public static final String ORDER_DEPT = "d";
	public static final String ORDER_NAME = "n";
	public static final String ORDER_DATE = "w";

	private static final List<SelectItem> SORT_ORDER_DL = Arrays.asList(
			new SelectItem(ORDER_ACCT, "By Account Number. Then by Last Name."),
			new SelectItem(ORDER_DEPT, "By Department. Then by Account Number."),
			new SelectItem(ORDER_NAME, "By Last Name."),
			new SelectItem(ORDER_DATE, "By Week Ending. Then by Account Number.") );

	/** Indicates the sort order selected */
	private String statusSelection = STATUS_ALL;
	public static final String STATUS_ALL = "a";
	public static final String STATUS_SUBMIT = "s";
	public static final String STATUS_APPROVE = "p";
	public static final String STATUS_VOID = "v";

	/**
	 * The drop-down list of selections for Status values available as filters
	 * for timecards.
	 */
	private static final List<SelectItem> STATUS_SELECT_DL = Arrays.asList(
			new SelectItem(STATUS_ALL, "All"),
			new SelectItem(STATUS_SUBMIT, "Submitted"),
			new SelectItem(STATUS_APPROVE, "Approved (Final)"),
			new SelectItem(STATUS_VOID, "Void")
			);

	/** The Production associated with the timecard(s) being printed. */
	protected Production production;

	/** The Project associated with the timecard(s) being printed, if any. This
	 * will be non-null only for Commercial productions. */
	protected Project project;

	/** The Department name which may be included in text within the dialog box. */
	private String deptName;

	/** The WeeklyBatch name which may be included in text within the dialog box. */
	private String batchName;

	/** If true, the full set of options will be available in the pop-up. If
	 * false, only the "employee" options will be available.  This value is
	 * set by the caller (holding class). */
	private boolean showFull;

	/** If true, the action is for all projects within a Commercial production.
	 * Additional message text will be displayed about
	 * "print selections include all projects", and the date selection drop-down
	 * list will be calculated accordingly. */
	private boolean showAllProjects = false;

	/** If true, include message text about "print selections does NOT include all projects". */
	private boolean showNotAllProjects = false;

	/** If true, display the "print unbatched" option. */
	private boolean unbatched;

	/** The week-ending date selection list for the Create Timecard pop-up. */
	private List<SelectItem> weekEndDateDL;

	/** The week-ending data selected on the Create Timecard pop-up. */
	private Date weekEndDate;

	/** The to-week-ending data selected on the print Timecard pop-up. */
	private Date secondWeekEndDate;

	/** The current date selected in the Week-ending filter. Null indicates the
	 * "All" selection. */
	private Date filterDate;

	/** True iff the 'week-ending dates' selection list should include a choice
	 * for "All". */
	protected boolean allowAllWeeks;


	public TimecardSelectBase() {
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * Strings are generally message-ids, which will be looked up in the message
	 * resource file.
	 *
	 * @param prod
	 * @param proj The Project associated with the timecard(s) being printed, if
	 *            any. This will be non-null only for Commercial productions.
	 * @param dept The department name, which will be available to the JSP for
	 *            use in messages -- in particular, for the choice of selecting
	 *            all timecards related to one Department.
	 * @param batch The WeeklyBatch name, which will be available to the JSP for
	 *            use in messages -- in particular, for the choice of selecting
	 *            all timecards related to one batch.
	 * @param full If true, show all the radio-button options; if false, only
	 *            the basic employee choices are displayed.
	 * @param showAllProj If true, the action is for all projects within a
	 *            Commercial production. Additional message text will be
	 *            displayed about "print selections include all projects", and
	 *            the date selection drop-down list will be calculated
	 *            accordingly.
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box.
	 * @param msgId The message-id of the message to be shown in the dialog.
	 * @param buttonOkId The message-id of the text for the "OK" button.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button.
	 */
	public void show(Production prod, Project proj, String dept, String batch, boolean full,
			boolean showAllProj, PopupHolder holder, int act, String titleId, String msgId,
			String buttonOkId, String buttonCancelId) {
		log.debug("action=" + act);
		production = prod;
		production.getPayrollPref().getFirstPayrollDate(); // initialize it
		project = proj;
		deptName = dept;
		batchName = batch;
		showFull = full;

		showAllProjects = showAllProj; // Show "includes all projects" message
		if (! showAllProjects) { // if not showing this,
			// and user has the privilege, then show "does NOT include" message
			showNotAllProjects = AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_ALL_PROJECTS);
		}
		else {
			showNotAllProjects = false;
		}

		unbatched = (batch == null || batch.length() == 0);
		rangeSelection = RANGE_CURRENT;
		sortOrder = ORDER_DATE;
		statusSelection = STATUS_ALL;
		allowAllWeeks = true;
		weekEndDate = Constants.SELECT_ALL_DATE; // our holder will likely change this
		secondWeekEndDate = Constants.SELECT_ALL_DATE; // our holder will likely change this
		weekEndDateDL = null;	// make sure we generate new list
		super.show(holder, act, titleId, msgId, buttonOkId, buttonCancelId);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	@Override
	public String actionCancel() {
		return super.actionCancel();
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
//		log.debug("OK, action=" + action);
		return super.actionOk();
	}

	public void listenRangeSelection(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (rangeSelection != null) {
				boolean allWeeks = false;
				if (rangeSelection.equals(RANGE_CURRENT) || rangeSelection.equals(RANGE_PERSON)) {
					allWeeks = true;
					sortOrder = ORDER_DATE;
				}
				else if (rangeSelection.equals(RANGE_DEPT) || rangeSelection.equals(RANGE_BATCH) ||
						rangeSelection.equals(RANGE_UNBATCHED)) {
					sortOrder = ORDER_ACCT;
				}
				else {
					sortOrder = ORDER_DEPT;
				}
				if (AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_ALL_TIMECARDS)) {
					allWeeks = true;
				}
				if (allWeeks != allowAllWeeks) {
					allowAllWeeks = allWeeks;
					if (allowAllWeeks) {
						weekEndDateDL.add(0, Constants.SELECT_ALL_DATES);
					}
					else {
						weekEndDateDL.remove(0);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Create the list of "week-end dates" that the user will be able to choose
	 * from in the drop-down list in the Print (or export) Timecards pop-up's
	 * date selection. The earliest date included is based on the earliest
	 * timecard in the Production under consideration. For a Commercial
	 * production, this is also the case if {@link #showAllProjects} is true;
	 * otherwise it will be the earliest timecard in the relevant
	 * {@link #project}.
	 *
	 * @return A non-null List of SelectItem`s; in each SelectItem, the value is
	 *         a Date object, and the label is the corresponding mm/dd/yyyy
	 *         representation. If {@link #allowAllWeeks} is true, then the first
	 *         choice in the list will be "All".
	 */
	protected List<SelectItem> createWeekEndDateDL() {
		Project dateProj = getShowAllProjects() ? null : project;
		List<SelectItem> list = TimecardUtils.createEndDateList(production, dateProj, weekEndDate, false, true);
		if (allowAllWeeks) {
			list.add(0, Constants.SELECT_ALL_DATES);
		}
		return list;
	}

//	/** See {@link #showFull}. */
//	public boolean getShowFull() {
//		return showFull;
//	}
//	/** See {@link #showFull}. */
//	public void setShowFull(boolean showFull) {
//		this.showFull = showFull;
//	}

	/** See {@link #deptName}. */
	public String getDeptName() {
		return deptName;
	}

	/**See {@link #batchName}. */
	public String getBatchName() {
		return batchName;
	}

	/** See {@link #showFull}. */
	public boolean getShowFull() {
		return showFull;
	}
	/** See {@link #showFull}. */
	public void setShowFull(boolean showFull) {
		this.showFull = showFull;
	}

	/** See {@link #showAllProjects}. */
	public boolean getShowAllProjects() {
		return showAllProjects;
	}

	/** See {@link #showNotAllProjects}. */
	public boolean getShowNotAllProjects() {
		return showNotAllProjects;
	}

	/** See {@link #unbatched}. */
	public boolean getUnbatched() {
		return unbatched;
	}
	/** See {@link #unbatched}. */
	public void setUnbatched(boolean unbatched) {
		this.unbatched = unbatched;
	}

	/** See {@link #rangeSelection}. */
	public String getRangeSelection() {
		return rangeSelection;
	}
	/** See {@link #rangeSelection}. */
	public void setRangeSelection(String rangeSelection) {
		this.rangeSelection = rangeSelection;
	}

	/**
	 * @return the user's range selection as an TimecardSelectionType enum
	 *         value.
	 */
	public TimecardSelectionType getRangeSelectionType() {
		TimecardSelectionType select = TimecardSelectionType.ALL;
		if (getRangeSelection().equals(RANGE_CURRENT)) {
			// current person and occupation
			select = TimecardSelectionType.CURRENT;
		}
		else if (getRangeSelection().equals(RANGE_PERSON)) {
			// all timecards for wtc.getUserAccount()
			select = TimecardSelectionType.PERSON;
		}
		else if (getRangeSelection().equals(RANGE_DEPT)) {
			// all timecards for wtc.getDepartmentId();
			select = TimecardSelectionType.DEPT;
		}
		else if (getRangeSelection().equals(RANGE_BATCH)) {
			// all timecards matching wtc.getWeeklyBatch()
			select = TimecardSelectionType.BATCH;
		}
		else if (getRangeSelection().equals(RANGE_UNBATCHED)) {
			// all timecards matching wtc.getWeeklyBatch()
			select = TimecardSelectionType.UNBATCHED;
		}
		else if (getRangeSelection().equals(RANGE_ALL)) {
			// all crew timecards for current date
			select = TimecardSelectionType.ALL;
		}
		else if (getRangeSelection().equals(RANGE_SELECT)) {
			// select timecards
			select = TimecardSelectionType.SELECT;
		}
		return select;
	}

	/** See {@link #sortOrder}. */
	public String getSortOrder() {
		return sortOrder;
	}
	/** See {@link #sortOrder}. */
	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	/** See {@link #SORT_ORDER_DL}. */
	public List<SelectItem> getSortOrderDL() {
		return SORT_ORDER_DL;
	}

	/**See {@link #statusSelection}. */
	public String getStatusSelection() {
		return statusSelection;
	}
	/**See {@link #statusSelection}. */
	public void setStatusSelection(String statusSelection) {
		this.statusSelection = statusSelection;
	}

	/**See {@link #STATUS_SELECT_DL}. */
	public List<SelectItem> getStatusSelectDL() {
		return STATUS_SELECT_DL;
	}

	/**See {@link #filterDate}. */
	public Date getFilterDate() {
		return filterDate;
	}
	/**See {@link #filterDate}. */
	public void setFilterDate(Date filterDate) {
		this.filterDate = filterDate;
	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}
	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #weekEndDateDL}. */
	public List<SelectItem> getWeekEndDateDL() {
		if (weekEndDateDL == null) {
			weekEndDateDL = createWeekEndDateDL();
		}
		return weekEndDateDL;
	}
	/** See {@link #weekEndDateDL}. */
	public void setWeekEndDateDL(List<SelectItem> createDateDL) {
		weekEndDateDL = createDateDL;
	}

	/** See {@link #secondWeekEndDate}. */
	public Date getSecondWeekEndDate() {
		return secondWeekEndDate;
	}
	/** See {@link #secondWeekEndDate}. */
	public void setSecondWeekEndDate(Date secondWeekEndDate) {
		this.secondWeekEndDate = secondWeekEndDate;
	}

}
