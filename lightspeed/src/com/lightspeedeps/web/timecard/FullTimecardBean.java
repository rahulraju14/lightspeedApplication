/**
 * File: FullTimecardBean.java
 */
package com.lightspeedeps.web.timecard;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.service.PayBreakdownService;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.payroll.WeeklyBatchUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.*;
import com.lightspeedeps.web.image.ImageManage;
import com.lightspeedeps.web.image.ImageManager;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the Full Timecard page. Some of the underlying
 * functionality is in the TimecardBase superclass.  This is shared
 * with the Individual Time Card page code in IndivTimecardBean.java.
 */
@ManagedBean
@ViewScoped
public class FullTimecardBean extends TimecardBase
		implements FilterHolder, TimecardListHolder, ImageManage {
	/** */
	private static final long serialVersionUID = 4917550680781132881L;

	private static final Log log = LogFactory.getLog(FullTimecardBean.class);

	private final FullTimecardFilterBean filterBean;

	/** The List of SelectItem`s for the "Filter by:" drop-down. */
	private List<SelectItem> filterByList;

	/** Selected department ID.  A value of 0 is used for the "All" entry,
	 * when timecards from all departments are displayed. */
	private Integer departmentId;

	/** The list of Department`s for the user to select from. */
	private List<SelectItem> departmentList;

	/** The database id of the currently selected WeeklyBatch. */
	private Integer batchId;

	/**
	 * The current WeeklyStatusFilter filter setting, which (indirectly) defines
	 * the possible WeeklyStatus values that the displayed timecards should
	 * have. If set to WeeklyStatusFilter.ALL, then the WeeklyStatusFilter
	 * selection is set to "All" -- no filtering.
	 */
	private ApprovableStatusFilter statusFilter;

	/** The list of WeeklyBatch`s for the user to select from. */
//	private List<SelectItem> batchList;

	/** True iff the "scrolling" arrow controls should be displayed, allowing the
	 * user to iterate through a list of timecards. The arrows are hidden if the
	 * displayed timecard does not match the current project and the user does
	 * not have 'aggregate' view (ability to see all Projects). */
	private Boolean showScrollArrows;

	/** Controls the style of the "History" display - true if all information
	 * should be displayed, false if only "signature" information should be
	 * displayed. */
	private boolean showAllHistory = true;

	/** True iff the "detail" information in the audit trail should be included
	 * in the display. */
	private boolean showAuditDetail = false;

	/** The (human-readable) audit trail of the currently displayed timecard. */
	private String auditTrail;

	/** True if the Pay Jobs section should be expanded; false means it
	 * is "collapsed" with only a small header and expand button showing. */
	private boolean expandJobs = true;

	/** True if the Job Splits section should be expanded; false means it
	 * is "collapsed" with only a small header and expand button showing. */
	private boolean expandSplit = true;

	/** True if a warning message should be issued about the total number
	 * of work hours not matching between the dailyTime totals and the
	 * PayJob totals. */
	private boolean jobTotalMismatch;

	/** Temporary string with occupation code and possibly (union) schedule */
	private String occCodeSchedule;

	/** Pay Job number to be deleted or cleared, origin 1; set by JSF. */
	private int deleteJobNumber;

	/** The index (origin 0) of the entry in the Pay Breakdown table
	 * to be deleted. Set by the JSP when the user clicks a red X icon
	 * in a pay breakdown detail line. */
	private int deletePayBreakdownIx;

	/** Which job is being modified -- set via JSP; used for setting
	 * custom pay job multipliers. This is Pay job number, origin 1. */
	private int modifyJobNumber;

	/** Which multiplier column was clicked, to be changed.  Value is
	 * set via JSP, and should be from 1 to 4, representing one of
	 * the 4 PayJob.customMultiplier<n> fields. */
	private long multiplierNumber;

	/** The boolean that backs the "premium" checkbox on the Custom Multiplier
	 * pop-up dialog box for the Pay Jobs. */
	private boolean premium;

	private BigDecimal multiplier;

	/** True iff the "set multiplier" dialog pop-up should be displayed. */
	private boolean showSetMultiplier;

	/** The pay category last selected from the drop-down list in the
	 * Pay Breakdown table.  Not used, except referenced by JSP. */
	private PayCategory pbCategory;

	/** True iff the "Insert Row" pop-up for Pay Breakdown should be displayed. */
	private boolean showAddPayLine;

	/** The user-selected line number from the "insert row" pop-up for the Pay
	 * Breakdown table. */
	private Integer payLineNumber;

	/** Contents of drop-down selection list for the "insert row" pop-up. */
	private List<SelectItem> payLineNumberDL;

	/** Contents of the drop-down selection list for "Category" in the
	 * Pay Breakdown table */
	private List<SelectItem> categoryDL;

	/** drop-down list for "job number" selectors. */
	private List<SelectItem> jobNumberDL;

	/** Whether to disable PayBreakdown lines */
	private Boolean disablePayBreakdownLines;

	/** List of timecards available to the current approver; lazy-initialized
	 * if/when the user clicks on the left or right scrolling arrows in the header. */
	private List<TimecardEntry> timecardEntryList;

	/** Employee department check*/
	public boolean isEmpDeptCast;

	/**
	 * Default constructor.
	 */
	public FullTimecardBean() {
		super(WeeklyTimecard.SORTKEY_NAME); // default sort (not user settable) is by name, then date
		log.debug("");
		// Initialize to null so that we can fetch the actual value only one.
		teamPayroll = null;
		disablePayBreakdownLines = null;

		setAttributePrefix("FullTimecard.");
		setSortAttributePrefix("FullTimecard.");
		setScrollable(true);

		filterBean = FullTimecardFilterBean.getInstance();

		// Register all of our mini-tabs with the filter bean
		getFilterBean().register(this, TAB_MAIN);
		getFilterBean().register(this, TAB_BOX);
		getFilterBean().register(this, TAB_MILES);
		getFilterBean().register(this, TAB_TC_ATTACHMENTS);
		getFilterBean().register(this, TAB_AUDIT);

		// Get saved values from last time, or from Approval page
		getFilterBean().setFilterType((FilterType)SessionUtils.get(Constants.ATTR_FILTER_BY, FilterType.DEPT));
		setDepartmentId(SessionUtils.getInteger(Constants.ATTR_APPROVER_DEPT));
		setBatchId(SessionUtils.getInteger(Constants.ATTR_FILTER_BATCH_ID));

		String status = SessionUtils.getString(Constants.ATTR_APPROVER_STATUS);
		if (status != null) {
			setStatusFilter(ApprovableStatusFilter.valueOf(status));
		}
		else {
			setStatusFilter(ApprovableStatusFilter.ALL);
		}

		if (departmentId == null) {
			departmentId = 0;
		}
		else if (departmentId != 0 && weeklyTimecard != null &&
				(! weeklyTimecard.getDepartmentId().equals(departmentId))) {
			// the requested timecard is not in the "filter" department...
			departmentId = 0;	// show all departments instead
		}
		boolean defExpand = true; // default setting for Expand Splits and Expand Pay Jobs
		setup();
		if (getNotInProduction()) {
			HeaderViewBean.getInstance().setMenu(HeaderViewBean.MYTIMECARDS_FULL_TC);
		}
		else {
			HeaderViewBean.getInstance().setMenu(HeaderViewBean.PAYROLL_TIMECARD);
			if (getProduction().getType().isTours()) {
				defExpand = false; // default for Tours hides Job Split and Job Tables
			}
		}
		checkTab();

		expandJobs = SessionUtils.getBoolean(Constants.ATTR_TC_EXPAND_JOBS, defExpand);
		expandSplit = SessionUtils.getBoolean(Constants.ATTR_TC_EXPAND_SPLIT, defExpand);

		// Restore any saved scroll position (used when canceling out of edit mode).
		restoreScrollFromSession(); // try to maintain scrolled position
		SessionUtils.put(Constants.ATTR_TIMECARD_VIEW, 1);
	}

	/**
	 * Initialize the values to display when the Full Timecard page loads. If
	 * the session contains an ATTR_TIMECARD_ID value, the WeeklyTimecard with
	 * the given database id should be displayed.
	 * <p>
	 * Note that there is no need to 'refresh' the weeklyTimecard before calling
	 * setup, as it does that itself.
	 */
	private void setup() {
		try {
			tcUser = null;
			setShowCopyPrior(null); // force recalculation of value.
			auditTrail = null; // force refresh
			occCodeSchedule = null; // force refresh

			// Clear out the timecardEventList so the list is recreated for the current timecard.
			// This prevents carrying over data from the previous timecard.
			setTimecardEventList(null);

			if (weeklyTimecard == null) {
				// can happen if browser link jumps to Full Timecard page
				if (getTimecardEntryList().size() > 0) {
					// this should work for an approver
					weeklyTimecard = getTimecardEntryList().get(0).getWeeklyTc();
					setWeekEndDate(weeklyTimecard.getEndDate());
				}
				else {
					Date date = getWeekEndDate();
					setWeekEndDate(TimecardUtils.calculateDefaultWeekEndDate());
					tcUser = SessionUtils.getCurrentUser();
					if (tcUser != null) { // LS-4422
						List<WeeklyTimecard> tcList = createWeeklyTimecardListUser();
						if (tcList.size() > 0) {
							weeklyTimecard = tcList.get(0);
						}
						else {
							setWeekEndDate(date);
						}
					}
				}
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			}
			else {
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			}
			if (weeklyTimecard != null) { // approver's full time card page
				SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId()); // make sure it matches
				String currentUserAcc = weeklyTimecard.getUserAccount();
				tcUser = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, currentUserAcc);
				//statusChanged(weeklyTimecard);
				calculateApproverFlags(weeklyTimecard);
				setApproverName(null); // force refresh
				forceLazyInit();
				TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
				TimecardCalc.calculateOtherTotals(weeklyTimecard);
				if (getModelRelease() != null) { //LS-4589
					TimecardCheck.allowModelReleaseFields(weeklyTimecard);
				}
				setWeekEndDate(weeklyTimecard.getEndDate());
				if (! getNotInProduction()) { // skip if "My Timecards"
					Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getProduction());
					SessionUtils.put(Constants.ATTR_CONTACT_ID, contact == null ? null : contact.getId());
					SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null); // so onboarding will use attr_contact_id
					SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID,null);
				}
			}
			else {
				// maybe browser link jumped to Full Timecard page and user has no TCs to view
				weeklyTimecard = new WeeklyTimecard();
				weeklyTimecard.setStatus(ApprovalStatus.VOID); // prevents most actions
				calculateApproverFlags(weeklyTimecard);
			}
			log.debug("");
//			getAttachmentBean().setDefaultAttachment(null, weeklyTimecard);
			timecardChanged();

			// Initialize the Phase radio buttons
			setupDailyTimePhaseRadioBttns();
			/* LS-2140 determine if the person is in cast department */
			setIsEmpDeptCast(false);
			if (weeklyTimecard.getDepartment() != null) { // v4.14.0 fix NPE
				String deptName = weeklyTimecard.getDepartment().getName();
				if (deptName != null && deptName.equals("Cast")) {
					setIsEmpDeptCast(true);
				}
			}
			setupUserChanged(tcUser);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/*
	 * Set the phase radio buttons for each DailyTime object in the
	 * WeeklyTimecard.
	 * Phases are used for Commercial crew productions.
	 */
	private void setupDailyTimePhaseRadioBttns() {
		List<DailyTime>dailyTimes = weeklyTimecard.getDailyTimes();

		if(dailyTimes != null && !dailyTimes.isEmpty()) {
			for(DailyTime dt : dailyTimes) {
				ProductionPhase phase = dt.getPhase();
				if(phase != null) {
					// Set the phases array on the DailyTime object.
					boolean [] phases = dt.getPhases();

					phases[0] = (phase == ProductionPhase.P);
					phases[1] = (phase == ProductionPhase.S);
					phases[2] = (phase == ProductionPhase.W);
				}
			}
		}
	}

	/**
	 * Invoked when we are about to switch into Edit mode -- the user clicked
	 * Edit, and any resulting prompts have been taken care of.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionEditOk()
	 */
	@Override
	protected String actionEditOk() {
		try {
			super.actionEditOk();
			TimecardCalc.calculateOtherTotals(weeklyTimecard);
			if (getEditHtg()) {
				checkTimecardRate(weeklyTimecard);
				checkWorkStartEndDates(weeklyTimecard);
			}
			insertExtraPbLine(weeklyTimecard);
			/*if (weeklyTimecard != null) {
				log.debug("......................>>> edit for weeklyTimecard");
				storeEventForTimecards(TimedEventType.EDIT, TimecardFieldType.N_A, -1);
			}*/
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Method that deletes the current weekly timecard. Called when the user
	 * clicked Delete and after any necessary prompts have been taken care of.
	 *
	 * @return Faces navigation string - either null to remain on Full Timecard
	 *         view, or switch to Basic view if no other available timecards to
	 *         view in Full mode.
	 */
	@Override
	public String actionDeleteOk() {
		String res = null;
		try {
			// figure out which timecard to display after deleting current one
			int ix = findCurrentTimecardIndex() + 1;
			// Delete the current timecard
			super.actionDeleteOk();
			weeklyTimecard = null;
			if (timecardEntryList.size() > 1) {
				// Set up to display the timecard after the one we deleted
				if (ix >= timecardEntryList.size()) { // wrap back to beginning
					ix = 0;
				}
				weeklyTimecard = timecardEntryList.get(ix).getWeeklyTc();
				setup();
				timecardEntryList = null; // force refresh, to remove deleted entry
			}
			else { // no other timecards in Full TC list - switch to Basic view.
				if (getNotInProduction()) {
					// List was empty while displaying My Timecards page
					res = HeaderViewBean.MYTIMECARDS_MENU_DETAILS; // go to My Timecards Basic view
				}
				else {
					// Normally switch to Timecards Basic view page
					res = HeaderViewBean.PAYROLL_TIMECARD;
				}
		}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return res;
	}

	/**
	 * Action method for the Save button. If save completes, force a page
	 * refresh, to clear out the ICEfaces DOM tree. This is necessary because
	 * some input fields get changed from rendered to non-rendered based on user
	 * input, but the tree does not get updated. (see rev 2.2.4341)
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionSave()
	 */
	@Override
	public String actionSave() {
		super.actionSave();
		if (! editMode) {
			/*if (weeklyTimecard != null) {
				log.debug("......................>>> Save for weeklyTimecard");
				storeEventForTimecards(TimedEventType.SAVE, TimecardFieldType.N_A, -1);
			}*/
			if (getNotInProduction()) {
				return HeaderViewBean.MYTIMECARDS_FULL_TC;
			}
			return HeaderViewBean.PAYROLL_FULL_TC;
		}
		return null;
	}

	@Override
	public String actionCancel() {
		super.actionCancel();
		/*if(weeklyTimecard != null) {
			log.debug("......................>>> cancel for weeklyTimecard");
			storeEventForTimecards(TimedEventType.CANCEL, TimecardFieldType.N_A, -1);
		}*/
		if (getNotInProduction()) {
			return HeaderViewBean.MYTIMECARDS_FULL_TC;
		}
		return HeaderViewBean.PAYROLL_FULL_TC;
	}

	public String actionViewBasic() {
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		SessionUtils.put(Constants.ATTR_WEEKEND_DATE, weeklyTimecard.getEndDate());
		SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, getViewProductionId());
		SessionUtils.put(Constants.ATTR_TIMECARD_VIEW, 0);
		clearScroll(); // don't use saved scroll value next time we display Full TC
		if (getNotInProduction()) {
			return HeaderViewBean.MYTIMECARDS_MENU_DETAILS;
		}
		else {
			return HeaderViewBean.PAYROLL_TIMECARD;
		}
	}

	/**
	 * Action method for the Add Comment button on the Worksheet mini-tab,
	 * @return null navigation string
	 */
	public String actionPreviousTimecard() {
		try {
			int ix = findCurrentTimecardIndex() - 1;
			if (timecardEntryList.size() < 2) {
				return null;
			}
			if (ix < 0) { // wrap to end of list
				ix = timecardEntryList.size() - 1;
			}
			weeklyTimecard = timecardEntryList.get(ix).getWeeklyTc();
			setup();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the Add Comment button on the Worksheet mini-tab,
	 * @return null navigation string
	 */
	public String actionNextTimecard() {
		try {
			int ix = findCurrentTimecardIndex() + 1;
			if (timecardEntryList.size() < 2) {
				return null;
			}
			if (ix >= timecardEntryList.size()) { // wrap back to beginning
				ix = 0;
			}
			weeklyTimecard = timecardEntryList.get(ix).getWeeklyTc();
			setup();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "Update" button; prompt the user.
	 *
	 * @return null navigation string
	 */
	public String actionUpdateFromStart() {
		addButtonClicked();
		StartForm sf = weeklyTimecard.getStartForm();

		if (! weeklyTimecard.getSubmitable()) {
			if (weeklyTimecard.getAllowWorked() && ! sf.getAllowWorked()) {
				PopupBean.getInstance().show(this, 0, getMessagePrefix() + "CantUpdate.Title",
						getMessagePrefix() + "CantUpdate.Text", "Confirm.Close", null);
				return null;
			}
		}
		// Check to see if the Start Form is complete. If not
		// Put up warning that the user cannot proceed.
		// 10/28/16 - allow update from incomplete Start, since they created the TC from this Start!
//		if(!sf.getHasRequiredFields()) {
//			PopupBean.getInstance().show(null, 0, getMessagePrefix() + "CantUpdate.Title",
//					getMessagePrefix() + "CantUpdate.IncompleteStart.Text", "Confirm.Close", null);
//			return null;
//		}

		PopupBean bean = PopupBean.getInstance();
		bean.show(this, ACT_UPDATE_TC, getMessagePrefix() + "Update.");
		return null;
	}

	/**
	 * Action method called when the user OK's the "Update" action. This updates
	 * the current timecard with information from the current StartForm. Note
	 * that this is only available in edit mode, therefore there's no need to
	 * lock or save the updated timecard.
	 *
	 * @return null navigation string
	 */
	private String actionUpdateFromStartOk() {
		if (weeklyTimecard != null) {
			TimecardUtils.updateTimecardFromStart(weeklyTimecard, getProduction());
			log.debug("......................>>> UPDATE_FROM_START event  for weeklyTimecard");
			storeEventForTimecards(TimedEventType.CHANGE, TimecardFieldType.UPDATE_FROM_START, -1, null);
		}
		return null;
	}

//	public String actionPrintOne() {
//		ReportBean report = (ReportBean)ServiceFinder.findBean("reportBean");
//		String sqlQuery = ReportQueries.timecard;
//		sqlQuery += " id = " + weeklyTimecard.getId();
//		String reportName = "timecard3";
//		if (this.getSelectedTab() == 1) {
//			reportName = "timecard1";
//		}
//		report.generateTimecard(reportName, sqlQuery);
//		return null;
//	}

	/**
	 * Action method for the Add Comment button on the Worksheet mini-tab,
	 * which adds a comment to the "private" comment field.
	 * @return null navigation string
	 */
	public String actionAddPrivateComment() {
		try {
			if (getNewPrivateComment() != null && getNewPrivateComment().trim().length() > 0) {
				Date date = new Date();
				DateFormat sdf = new SimpleDateFormat(", M/d/yy H:mm: ");
				String comment = weeklyTimecard.getPrivateComments();
				if (comment == null) {
					comment = "";
				}
				comment += "<b>" + getvUser().getFirstNameLastName()
						+ sdf.format(date) + "</b>" + getNewPrivateComment() + "<br/>";
				weeklyTimecard.setPrivateComments(comment);
				setNewPrivateComment("");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method to open the "Insert row" pop-up for the Pay Breakdown
	 * table. If there are less than two data rows now, just insert one instead
	 * of opening the pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionInsertPayLine() {
		try {
			if (weeklyTimecard.getPayLines().size() <= 2) {
				PayBreakdown pb = new PayBreakdown(weeklyTimecard, (byte)0);
				pb.setCategory("");
				weeklyTimecard.getPayLines().add(0, pb);
				PayBreakdownService.renumberPayLines(weeklyTimecard);
			}
			else {
				showAddPayLine = true;
				payLineNumber = 1;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "Insert" button on the "insert row" pop-up for the
	 * Pay Breakdown table. Inserts a new PayBreakdown item in the existing
	 * list, at the requested point.
	 *
	 * @return null navigation string
	 */
	public String actionInsertPayLineOk() {
		try {
			int n = weeklyTimecard.getPayLines().size();
			if (payLineNumber != null) {
				if (payLineNumber > 0 && payLineNumber < n) {
					PayBreakdown pb = new PayBreakdown(weeklyTimecard, (byte)payLineNumber.intValue());
					pb.setCategory("");
					weeklyTimecard.getPayLines().add(payLineNumber-1, pb);
					PayBreakdownService.renumberPayLines(weeklyTimecard);
				}
			}
			if (weeklyTimecard != null) {
				log.debug("......................>>> ADD_LINE event  for weeklyTimecard");
				storeEventForTimecards(TimedEventType.CHANGE, TimecardFieldType.ADD_LINE, payLineNumber, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		showAddPayLine = false;
		return null;
	}

	/**
	 * Action method for the "Cancel" button on the "insert row" pop-up for the
	 * Pay Breakdown table. Just closes the pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionCancelPayLine() {
		showAddPayLine = false;
		return null;
	}

	/**
	 * Action method for the red-X delete icons in the Pay Breakdown table.
	 * The 'deletePayBreakdownIx' field has already been set via the JSP.
	 * @return null navigation String
	 */
	public String actionDeletePayLine(int ix) {
		log.debug("ix=" + ix);
		try {
			deletePayBreakdownIx = ix;
			PayBreakdown payBreakdown = weeklyTimecard.getPayLines().get(deletePayBreakdownIx);
			String categoryDescription = payBreakdown.getCategory();
			PayBreakdown pb = weeklyTimecard.getPayLines().remove(deletePayBreakdownIx);
			if (pb.getId() != null) {
				weeklyTimecard.getDeletedPayLines().add(pb);
			}
			TimecardCalc.calculateOtherTotals(weeklyTimecard);	// recalculate grand total
			PayBreakdownService.renumberPayLines(weeklyTimecard);  // just to be safe!
			if (weeklyTimecard != null) {
				log.debug("......................>>> DELETE_LINE event for Pay Breakdown");
				storeEventForTimecards(TimedEventType.CHANGE, TimecardFieldType.DELETE_LINE, deletePayBreakdownIx+1, categoryDescription);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "Add Job" button.
	 * @return null navigation String
	 */
	public String actionAddJob() {
		try {
			addPayJob(); // Add the PayJob and update the job-number drop-down lists
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "delete" icon on the last PayJob table.
	 * deleteJobNumber should have been set by the framework (due to an
	 * f:setPropertyActionListener in the JSP) to the zero-origin PayJob entry
	 * to be deleted.
	 *
	 * @return null navigation string
	 */
	public String actionDeleteJob() {
		int jobIndex = deleteJobNumber - 1;
		try {
			if (jobIndex > 0 &&
					jobIndex == weeklyTimecard.getPayJobs().size()-1) {
				// get rid of the "insert" line in the pay breakdown table
				removeExtraPbLine(weeklyTimecard);
				// this should always be the case -- deleting the last one; there
				// should not be a delete control on any other PayJob.
				getWeeklyTimecardDAO().deletePayJob(weeklyTimecard, jobIndex);
				// then restore the "insert" line in the pay breakdown table
				insertExtraPbLine(weeklyTimecard);
				// recalculate PayJob table totals and compare to employee hours
				TimecardCalc.calculatePayJobHours(weeklyTimecard);
			}
			createJobNumberDL();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "clear/erase" icon on PayJob tables.
	 * deleteJobNumber should have been set by the framework (due to an
	 * f:setPropertyActionListener in the JSP) to the zero-origin PayJob entry
	 * to be cleared.
	 *
	 * @return null navigation string
	 */
	public String actionClearJob() {
		int jobIndex = deleteJobNumber - 1;
		try {
			if (jobIndex >= 0 &&
					jobIndex < weeklyTimecard.getPayJobs().size()) {
				// this should always be the case, due to values set by JSP

				// get rid of the "insert" line in the pay breakdown table
				removeExtraPbLine(weeklyTimecard);
				// then clear the job -- this will update the db
				getWeeklyTimecardDAO().clearPayJob(weeklyTimecard, jobIndex);
				// then restore the "insert" line in the pay breakdown table
				insertExtraPbLine(weeklyTimecard);
				// recalculate PayJob table totals and compare to employee hours
				TimecardCalc.calculatePayJobHours(weeklyTimecard);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "Fill Jobs" button. This uses the daily hours and split
	 * information to initialize the hourly data in the PayJob objects.
	 * <p>
	 * For 2.9, this also calculates MPVs, since we have hidden the "Calculate MPVs"
	 * button for now. This makes the Fill Jobs button action consistent with 2.2.
	 *
	 * @return null navigation String
	 */
	public String actionFillJobs() {
		try {
			if (! checkExemptMatches()) {
				// LS-1522 mismatch exempt status between TC and SF
				return null;
			}
			//LS-1558
			checkWorkStartEndDates(weeklyTimecard);
			TimecardService.fillPayJobs(weeklyTimecard); // Fill the PayJob objects from the daily info
			createJobNumberDL(); // number of payJobs may have changed
			auditTrail = null; // force refresh
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "Auto Pay" button. This generates Pay Breakdown
	 * line items from the expenses and Pay Job data
	 *
	 * @return null navigation String
	 */
	public String actionAutoPay() {
		try {
			if (! checkExemptMatches()) {
				// LS-1522 mismatch exempt status between TC and SF
				return null;
			}
			// Fill the Pay Breakdown line items from the expenses and Pay Job data
			boolean ret = TimecardService.autoPay(weeklyTimecard);
			insertExtraPbLine(weeklyTimecard); // add drop-down selection line item
			if (! ret) {
				MsgUtils.addFacesMessage("Timecard.HTG.AutoPay.NoData", FacesMessage.SEVERITY_INFO);
			}
			auditTrail = null; // force refresh
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "All/auto HTG" button.
	 *
	 * @return null navigation String
	 */
	public String actionCalculateAllHtg() {
		try {
			weeklyTimecard.setStartForm(StartFormDAO.getInstance().refresh(weeklyTimecard.getStartForm()));
			if (! checkExemptMatches()) {
				// LS-1522 mismatch exempt status between TC and SF
				return null;
			}
			//LS-1558
			checkWorkStartEndDates(weeklyTimecard);
			TimecardService.calculateAllHtg(weeklyTimecard, false, false);
			createJobNumberDL(); // number of payjobs may have changed
			insertExtraPbLine(weeklyTimecard); // add drop-down selection line item
			auditTrail = null; // force refresh
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "custom multiplier" buttons in the Pay Job tables.
	 * This just puts up the pop-up dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionSetMultiplier() {
		multiplier = null;
		premium = false;
		// note that modifyJobNumber is origin one
		int jobIndex = modifyJobNumber - 1;
		if (jobIndex >= 0 &&
				jobIndex < weeklyTimecard.getPayJobs().size()) {
			// this should always be the case
			PayJob pj = weeklyTimecard.getPayJobs().get(jobIndex);
			switch ((int)multiplierNumber) {
			case 1:
				multiplier = pj.getCustomMult1();
				premium = pj.getCustom1Type() == PayRateType.P;
				break;
			case 2:
				multiplier = pj.getCustomMult2();
				premium = pj.getCustom2Type() == PayRateType.P;
				break;
			case 3:
				multiplier = pj.getCustomMult3();
				premium = pj.getCustom3Type() == PayRateType.P;
				break;
			case 4:
				multiplier = pj.getCustomMult4();
				premium = pj.getCustom4Type() == PayRateType.P;
				break;
			case 5:
				multiplier = pj.getCustomMult5();
				premium = pj.getCustom5Type() == PayRateType.P;
				break;
			case 6:
				multiplier = pj.getCustomMult6();
				premium = pj.getCustom6Type() == PayRateType.P;
				break;
			default:
				break;
			}
		}
		showSetMultiplier = true;
		addFocus("multiplier");
		return null;
	}

	/**
	 * Action method for the Save button in the "custom multiplier" dialog
	 * pop-up for the Pay Job tables. Validates the multiplier value and stores
	 * it in the PayJob.
	 *
	 * @return null navigation string
	 */
	public String actionSetMultiplierOk() {
		if (multiplier == null) {
			MsgUtils.addFacesMessage("Timecard.PayJob.MultiplierRequired", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		try {
			multiplier = NumberUtils.scaleTo(multiplier, 1, 6); // display 1-2 decimal places
			if (multiplier.signum() != 1 ||
					multiplier.compareTo(Constants.DECIMAL_10) >= 0) {
				MsgUtils.addFacesMessage("Timecard.PayJob.MultiplierInvalid", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			// note that modifyJobNumber is origin one
			int jobIndex = modifyJobNumber - 1;
			if (jobIndex >= 0 &&
					jobIndex < weeklyTimecard.getPayJobs().size()) {
				// this should always be the case
				PayJob pj = weeklyTimecard.getPayJobs().get(jobIndex);
				switch ((int)multiplierNumber) {
				case 1:
					pj.setCustomMult1(multiplier);
					pj.setCustom1Type(premium ? PayRateType.P : PayRateType.H);
					break;
				case 2:
					pj.setCustomMult2(multiplier);
					pj.setCustom2Type(premium ? PayRateType.P : PayRateType.H);
					break;
				case 3:
					pj.setCustomMult3(multiplier);
					pj.setCustom3Type(premium ? PayRateType.P : PayRateType.H);
					break;
				case 4:
					pj.setCustomMult4(multiplier);
					pj.setCustom4Type(premium ? PayRateType.P : PayRateType.H);
					break;
				case 5:
					pj.setCustomMult5(multiplier);
					pj.setCustom5Type(premium ? PayRateType.P : PayRateType.H);
					break;
				case 6:
					pj.setCustomMult6(multiplier);
					pj.setCustom6Type(premium ? PayRateType.P : PayRateType.H);
					break;
				default:
					break;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		showSetMultiplier = false;
		return null;
	}

	/**
	 * Action method for the Cancel button in the "custom multiplier" pop-up for
	 * the Pay Job tables. This just closes the pop-up dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionCancelSetMultiplier() {
		showSetMultiplier = false;
		return null;
	}

	/**
	 * Action method
	 * @return null navigation String
	 */
	public String actionHistoryShowAll() {
		showAllHistory = true;
		addButtonClicked();
		return null;
	}

	/**
	 * Action method
	 * @return null navigation String
	 */
	public String actionHistoryShowSignatures() {
		showAllHistory = false;
		addButtonClicked();
		return null;
	}

	/**
	 * Action method
	 * @return null navigation String
	 */
	public String actionClearAuditTrail() {
		if (weeklyTimecard != null) {
			AuditEventDAO.getInstance().deleteTrail(ObjectType.WT, weeklyTimecard.getId());
			auditTrail = null;
		}
		addButtonClicked();
		return null;
	}

	/**
	 * Action method to hide detail information in the audit
	 * trail window.
	 *
	 * @return null navigation String
	 */
	public String actionAuditHideDetail() {
		showAuditDetail = false;
		auditTrail = null; // force refresh
		addButtonClicked();
		return null;
	}

	/**
	 * Action method to display detail information in the
	 * audit trail window.
	 *
	 * @return null navigation String
	 */
	public String actionAuditShowDetail() {
		showAuditDetail = true;
		auditTrail = null; // force refresh
		addButtonClicked();
		return null;
	}

	/**
	 * Action method to toggle the expand/collapse setting of
	 * the Pay Jobs display.
	 *
	 * @return null navigation String
	 */
	public String actionToggleJobs() {
		expandJobs = ! expandJobs;
		SessionUtils.put(Constants.ATTR_TC_EXPAND_JOBS, expandJobs);
		restoreScroll(); // try to maintain scrolled position
		addButtonClicked();
		return null;
	}

	/**
	 * Action method to toggle the expand/collapse setting of
	 * the Job Splits display.
	 *
	 * @return null navigation String
	 */
	public String actionToggleSplit() {
		expandSplit = ! expandSplit;
		SessionUtils.put(Constants.ATTR_TC_EXPAND_SPLIT, expandSplit);
		restoreScroll(); // try to maintain scrolled position
		addButtonClicked();
		return null;
	}

	/**
	 * Method for "View Start" button -- find the StartForm corresponding to the
	 * current timecard, and set up to display it.
	 *
	 * @return Navigation string for the Start Form page, or null if we can't
	 *         find the StartForm.
	 */
	public String actionViewStart() {
		String res = null;
		if (weeklyTimecard != null) {
			Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getViewProduction());
			if (contact != null) {
				// StartFormBean will use the contact id if no startForm id is supplied:
				SessionUtils.put(Constants.ATTR_CONTACT_ID, contact.getId());
//				SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // TODO force timecard pages to use attr_contact_id?
				StartForm sf = weeklyTimecard.getStartForm();
				if (sf != null) {
					// Set properties so StartFormBean will open this timecard's Start Form
					FormModelRelease mr = weeklyTimecard.getStartForm().getModelRelease();
					if (mr != null) {
						SessionUtils.put(Constants.ATTR_START_FORM_ID, mr.getId()); // request specific model release form id LS-4590
					}
					else {
						SessionUtils.put(Constants.ATTR_START_FORM_ID, sf.getId()); // request specific start form id
					}
					if (sf.getEmployment() != null) {
						SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, sf.getEmployment().getId()); // request specific employment record.
					}
					else {
						SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null);
					}
				}
				else {
					// Set properties so StartFormBean will open any one of this contact's Start Forms
					SessionUtils.put(Constants.ATTR_START_FORM_ID, null); // we don't know the start form id
					SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null);
				}
				// Set property for "Return" button on Start Form page
				//SessionUtils.put(Constants.ATTR_START_FORM_BACK_PAGE, HeaderViewBean.PAYROLL_FULL_TC);
				// Be sure our "view" properties are set in case we return to this page.
				SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
				SessionUtils.put(Constants.ATTR_WEEKEND_DATE, weeklyTimecard.getEndDate());
				SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, getViewProductionId());

				if (getViewProduction() != null && getViewProduction().getAllowOnboarding()) {
					res = HeaderViewBean.PAYROLL_START_FORMS;
				}
				else {
					res = HeaderViewBean.PEOPLE_MENU_STARTS;
				}
			}
		}

		return res;
	}

	/** The manager for uploading scanned timecard images. */
	private ImageManager imageManager;

	/**
	 * Action method for "add/replace attachment" -- use an ImageManager
	 * instance to prompt the user and load the file.
	 *
	 * @return null navigation string
	 */
	public String actionOpenNewImage() {
		if (imageManager == null) {
			imageManager = new ImageManager(this);
		}
		imageManager.setElementName(weeklyTimecard.getFirstName() + " " + weeklyTimecard.getLastName());
		imageManager.setImageAddedMessageId("Image.Uploaded.Timecard");
		addButtonClicked();
		return imageManager.actionOpenNewImage("Image.AddPrompt.Timecard", true);
	}

	/**
	 * Called by ImageManager when a new image has been successfully
	 * uploaded and saved in the database.  Link it to the current timecard.
	 *
	 * @see com.lightspeedeps.web.image.ImageManage#updateImage(com.lightspeedeps.model.Image, java.lang.String)
	 */
	@Override
	public void updateImage(Image image, String filename) {
		Image oldImage = weeklyTimecard.getPaperImage();
		weeklyTimecard.setPaperImage(image);
		// get rid of the "insert" line in the pay breakdown table
		removeExtraPbLine(weeklyTimecard);
		// Update the timecard in the database
		weeklyTimecard.setUpdated(new Date());
		getWeeklyTimecardDAO().attachDirty(weeklyTimecard);
		// then restore the "insert" line in the pay breakdown table
		insertExtraPbLine(weeklyTimecard);
		if (oldImage != null) {
			getWeeklyTimecardDAO().delete(oldImage);
		}
		imageManager.resetImages();
	}

	/**
	 * Action method for the "remove attachment" legend button.
	 * Just put up the confirmation dialog.
	 *
	 * @return null navigation String.
	 */
	public String actionDeleteImage() {
		PopupBean bean = PopupBean.getInstance();
		bean.show(this, ACT_DELETE_IMAGE, getMessagePrefix()+"DeleteImage.");
		addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "remove attachment" function, called when the user
	 * has clicked the Delete/Ok button in the confirmation dialog. Remove the
	 * attachment image from the database and from the current timecard.
	 *
	 * @return null navigation String.
	 */
	private String actionDeleteImageOk() {
		Image image = weeklyTimecard.getPaperImage();
		if (image != null) {
			weeklyTimecard.setPaperImage(null);
			// get rid of the "insert" line in the pay breakdown table
			removeExtraPbLine(weeklyTimecard);
			// Update the timecard in the database
			weeklyTimecard.setUpdated(new Date());
			getWeeklyTimecardDAO().attachDirty(weeklyTimecard);
			// then restore the "insert" line in the pay breakdown table
			insertExtraPbLine(weeklyTimecard);
			getWeeklyTimecardDAO().delete(image);
		}
		return null;
	}

	/**
	 * Action method called when the user clicks on the PDF image icon for
	 * a timecard's signature paper image.
	 * @return null navigation String
	 */
	public String actionOpenPdfImage() {
		Integer id = weeklyTimecard.getPaperImage().getId();
		Image image = weeklyTimecard.getPaperImage();
		String fileName = image.getTitle(); // ArchiveUtils.retrieveItem(name, unit);
		if (fileName != null) {
			// Create a specially-patterned filename.  When the browser requests this "file",
			// LsFacesServlet will recognize the format, load the Image from the database,
			// and return the Image.contents field to the browser.
			fileName = "../../" + Constants.IMAGE_PSEUDO_DIRECTORY + "/" + id + "/" + fileName;
			String javascriptCode = "window.open('" + fileName + "','signatureWindow');";
			addJavascript(javascriptCode);
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on one of our pop-up prompting
	 * dialogs.  Here we only process the "delete attachment" function.
	 * All other actions are passed to the superclass.
	 *
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE_IMAGE:
				res = actionDeleteImageOk();
				break;
			case ACT_UPDATE_TC:
				res = actionUpdateFromStartOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	@Override
	protected void forceLazyInit() {
		if (! editMode) {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			weeklyTimecard.setStartForm(StartFormDAO.getInstance().refresh(weeklyTimecard.getStartForm()));
		}
		if (weeklyTimecard.getPaperImage() != null) {
			weeklyTimecard.getPaperImage().getTitle();
			weeklyTimecard.getPaperImage().getArtist();
		}
		for (TimecardEvent tce: weeklyTimecard.getTimecardEvents()) {
			tce.getDate();
		}
		for (PayJob pj : weeklyTimecard.getPayJobs()) {
			pj.getAccountDtl();
			for (PayJobDaily pjd : pj.getPayJobDailies()) {
				pjd.getDate();
			}
		}
		for (PayBreakdown pb : weeklyTimecard.getPayLines()) {
			pb.getAccountDtl();
		}
		for (PayBreakdownDaily pb : weeklyTimecard.getPayDailyLines()) {
			pb.getAccountDtl();
		}
		if (editMode) {
			// fixed LazyInit error on Fill Jobs
			StartForm sf = weeklyTimecard.getStartForm();
			sf.getGuarHours();
			if (sf.getHasPrepRates()) {
				sf.getGuarHours(sf.getPrep(), sf.isStudioRate());
			}
			// Fix LIEs on payrollService and on boxRentatText
			PayrollPreference payrollPreference = PayrollPreferenceDAO.getInstance().refresh(getViewProduction().getPayrollPref());
			if (payrollPreference.getPayrollService() != null) {
				payrollPreference.getPayrollService().getBoxRentalText();
			}
		}
		super.forceLazyInit();
	}

	/**
	 * A callback method, called by FilterBean when a user has eliminated the
	 * user of a particular filter type.
	 *
	 * @param type The type of filter being removed.
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#dropFilter(com.lightspeedeps.type.FilterType)
	 */
	@Override
	public void dropFilter(FilterType type) {
		switch(type) {
			case DEPT:
				listenDeptChange(0);
				break;
			case BATCH:
				listenBatchChange(0);
				break;
			case STATUS:
				listenStatusChange(ApprovableStatusFilter.ALL);
				break;
			case DATE:
			case N_A:
			case NAME:
			case UNION:
				break;
		}
	}

	/**
	 * A change-event listener method, called by FilterBean, to handle any one
	 * of several types of changes.
	 *
	 * @param type The {@link FilterType} of the value that changed, e.g., DEPT.
	 * @param value The newly-selected value from the drop-down list. This is
	 *            never null.
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#listenChange(com.lightspeedeps.type.FilterType,
	 *      java.lang.Object)
	 */
	@Override
	public void listenChange(FilterType type, Object value) {
		switch(type) {
			case DEPT:
				listenDeptChange(Integer.valueOf((String)value));
				break;
			case BATCH:
				listenBatchChange(Integer.valueOf((String)value));
				break;
			case STATUS:
				ApprovableStatusFilter filter;
				try {
					filter = ApprovableStatusFilter.valueOf((String)value);
				}
				catch (Exception e) { // can happen when switching "filter by"
					filter = ApprovableStatusFilter.ALL;
				}
				listenStatusChange(filter);
				break;
			case DATE:
			case NAME:
			case UNION:
			case N_A:
				break;
		}
	}

	/**
	 * ValueChangeListener for WeeklyBatch drop-down list; invoked via the
	 * FilterBean listenChange call-back.
	 *
	 * @param id The new WeeklyBatch database id value.
	 */
	protected void listenBatchChange(Integer id) {
		if (id != null) {
			Integer tcId = saveSelection();
			batchId = id;
			createTimecardList(); // refresh list of TC's, and updates their status
			SessionUtils.put(Constants.ATTR_FILTER_BATCH_ID, getBatchId());
			reselect(tcId);
		}
	}

	/**
	 * ValueChangeListener for Department drop-down list; invoked via the
	 * FilterBean listenChange call-back.
	 *
	 * @param id The new Department database id value.
	 */
	public void listenDeptChange(Integer id) {
		try {
			if (id != null) {
				Integer tcId = saveSelection();
				departmentId = id;
				createTimecardList(); // refresh list of TC's, and updates their status
				SessionUtils.put(Constants.ATTR_APPROVER_DEPT, departmentId);
				reselect(tcId);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * ValueChangeListener for Status drop-down filter list; invoked via the
	 * FilterBean listenChange call-back.
	 */
	protected void listenStatusChange(ApprovableStatusFilter filter) {
		try {
			Integer tcId = saveSelection(); // Save id of existing selected Timecard, if any.
			statusFilter = filter;
			createTimecardList(); // refresh list of TC's, and updates their status
			SessionUtils.put(Constants.ATTR_APPROVER_STATUS, getStatusFilter().name());
			reselect(tcId); // Restore the selection, if possible, or select 1st TC if not.
		}
		catch(Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener for account fields; no action needed, but method is
	 * required by JSP (which is shared with Start Form page).
	 *
	 * @param evt
	 */
	public void listenAccount(ValueChangeEvent evt) {
		// nothing to do.
	}

	/**
	 * Listening for the Prep radio button change for Commercial prods.
	 * @param event ValueChangeEvent from the framework
	 */
	public void listenPhasePrepChange(ValueChangeEvent event) {
		updatePhase(event, 0);
	}

	/**
	 * Listening for the Shoot radio button change for Commercial prods.
	 * @param event ValueChangeEvent from the framework
	 */
	public void listenPhaseShootChange(ValueChangeEvent event) {
		updatePhase(event, 1);
	}

	/**
	 * Listening for the Wrap radio button change for Commercial prods.
	 * @param event ValueChangeEvent from the framework
	 */
	public void listenPhaseWrapChange(ValueChangeEvent event) {
		updatePhase(event, 2);
	}

	/**
	 * Update the given phase index based on the event data; update the account
	 * information for the daily entry based on the phase selected and the
	 * account codes in the timecard.
	 *
	 * @param event ValueChangeEvent from the framework
	 * @param ix index of the button selected; this MUST match the ordinal
	 *            values of the ProductionPhase enum.
	 */
	private void updatePhase(ValueChangeEvent event, int ix) {
		UIData ud = (UIData)event.getComponent().findComponent("cday");
		if (ud != null) {
			DailyTime dailyTime = (DailyTime)ud.getRowData();
			if (dailyTime != null) {
				boolean[] phases = dailyTime.getPhases();
				phases[0] = false;
				phases[1] = false;
				phases[2] = false;
				phases[ix] = true;
				dailyTime.setPhases(phases);
				switch (dailyTime.getPhase()) {
					case P:
					case W:
						dailyTime.setAccountMajor(weeklyTimecard.getAccountMajor());
						break;
					case S:
						dailyTime.setAccountMajor(weeklyTimecard.getAccountDtl());
						break;
					case N_A:
						dailyTime.setAccountMajor(null);
						break;
				}
			}
		}
	}

	/**
	 * @return If a timecard is currently selected, unselect it, and return the
	 *         timecard's id value. If none is selected, return null.
	 */
	private Integer saveSelection() {
				Integer tcId = null;
				if (weeklyTimecard != null) {
					tcId = weeklyTimecard.getId();
					weeklyTimecard.setSelected(false);
				}
		return tcId;
	}

	/**
	 * If the given timecard id exists in the current list of timecards, select
	 * it and run the usual setup on it.  If it does not exist in the current
	 * list, select and setup the first item in the list.
	 *
	 * @param tcId
	 */
	private void reselect(Integer tcId) {
				boolean found = false;
				if (tcId != null) {
					for (TimecardEntry tce : timecardEntryList) {
						if (tcId.equals(tce.getWeeklyTc().getId())) {
							found = true;
							break;
						}
					}
				}
				if (! found) {
					if (timecardEntryList.size() > 0) {
						weeklyTimecard = timecardEntryList.get(0).getWeeklyTc();
					}
					else {
						weeklyTimecard = null;
					}
				}
				setup(); // display first user in list
			}

	/**
	 * ValueChangeListener method for the rate-type drop-down in the header
	 * area. This is called in the INVOKE_APPLICATION phase.
	 *
	 * @param event contains old and new values
	 */
	public void listenRateType(ValueChangeEvent event) {
		try {
			String type = (String)event.getNewValue();
			if (type != null) {
				StartForm sd = weeklyTimecard.getStartForm();
				sd = StartFormDAO.getInstance().refresh(sd);

				// We no longer update expense table Box Rental amount at this point.
//2.9.5416		TimecardUtils.updateBoxRentalExpense(weeklyTimecard, false, false);

				WorkZone workZone = WorkZone.SL; // default to Studio Location
				if (type.equals(StartForm.USE_LOCATION_RATE)) {
					workZone = WorkZone.DL; // Distant Location
				}

				// Update TC's rate fields and car allowance expense item
				TimecardUtils.fillRates(weeklyTimecard, sd, getProduction(), workZone, null);

// 3.0.4952		if (dayType != null) {
//					// Update existing SL to DL and vice-versa; ignore all other Day Types
//					for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
//						if (dt.getDayType() == oppositeDayType) {
//							dt.setDayType(dayType);
//						}
//					}
//				}
				PayJob pj = null;
				if (weeklyTimecard.getPayJobs() != null && weeklyTimecard.getPayJobs().size() > 0) {
					pj = weeklyTimecard.getPayJobs().get(0);
					pj.setRate(weeklyTimecard.getHourlyRate());
					pj.setDailyRate(weeklyTimecard.getDailyRate());
					pj.setWeeklyRate(weeklyTimecard.getWeeklyRate());
// 2.2.4731			pj.setBoxAmt(sd.getBoxRental().getLoc());
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * ValueChangeListener method for the category drop-down in the Pay
	 * Breakdown table. This is called in the INVOKE_APPLICATION phase.
	 *
	 * @param event Event data (new and old) supplied by the framework.
	 */
	public void listenPayBreakdownCategory(ValueChangeEvent event) {
		try {
			PayCategory pc = (PayCategory)event.getNewValue();
			if (pc != null) {
				int n = weeklyTimecard.getPayLines().size() - 1;
				PayBreakdown pb = new PayBreakdown(weeklyTimecard, (byte)n);
				pb.setCategory(pc.getLabel());
				if (pc.getMultiplier() != null) {
					pb.setMultiplier(pc.getMultiplier());
				}
				weeklyTimecard.getPayLines().add(n, pb);
				PayBreakdownService.renumberPayLines(weeklyTimecard);  // just to be safe!

				StartForm sf = weeklyTimecard.getStartForm();
				if (getProduction().getType().isAicp()) {
					pb.setJobNumber(weeklyTimecard.getJobNumber());
				}
				AccountCodes ac = sf.getByPayCategory(pc);
				if (ac != null) {
					pb.setAccountCodes(ac);
				}
				else if (pc.getIsLabor()) {
					// for labor items, copy all account codes from the timecard. 2.9.5599
					pb.setAccountCodes(weeklyTimecard.getAccount());
				}
				else {
					// For non-labor items, only set Loc and Prd/Epi from the timecard. 2.9.5599
					pb.setAccountLoc(weeklyTimecard.getAccountLoc());
					pb.setAccountMajor(weeklyTimecard.getAccountMajor());
				}
				if (pc == PayCategory.MILEAGE_NONTAX || pc == PayCategory.MILEAGE_TAX) {
					PayrollPreference pref;
					if (sf.getProject() == null) { // TV/Feature
						Production prod = TimecardUtils.findProduction(weeklyTimecard);
						pref = prod.getPayrollPref();
					}
					else { // Commercial
						pref = sf.getProject().getPayrollPref();
					}
					pb.setRate(pref.getMileageRate());
				}
				if (weeklyTimecard != null) {
					log.debug("ADD_LINE event  for weeklyTimecard");
					storeEventForTimecards(TimedEventType.CHANGE, TimecardFieldType.ADD_LINE, n+1, pc.getLabel());
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	// Not currently used (no Phase displayed in Pay Breakdown table)
//	public void listenBreakdownPhaseChange(ValueChangeEvent event) {
//		if (event.getNewValue() != null) {
//			TimecardCalc.calculatePayBreakdown(weeklyTimecard);
//		}
//	}

	/**
	 * ValueChangeListener for the "Phase" column (radio buttons) in the daily
	 * section of the Full timecard.
	 *
	 * @param event Event data (new and old) supplied by the framework.
	 */
	public void listenDailyPhaseChange(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			if (getProduction().getType().isAicp()) {
				DailyTime dailyTime = (DailyTime)ServiceFinder.getManagedBean("dailyTime");
				if (dailyTime != null) {
					ProductionPhase phase = (ProductionPhase)event.getNewValue();
					switch (phase) {
						case P:
						case W:
							dailyTime.setAccountMajor(weeklyTimecard.getAccountMajor());
							break;
						case S:
							dailyTime.setAccountMajor(weeklyTimecard.getAccountDtl());
							break;
						case N_A:
							dailyTime.setAccountMajor(null);
							break;
					}
				}
			}
			//TimecardCalc.calculatePayBreakdown(weeklyTimecard);
		}
	}

	/**
	 * ValueChangeListener method for the job-number drop-downs in the Job
	 * Splits section of the full timecard.
	 *
	 * @param event Event data (new and old) supplied by the framework.
	 */
	public void listenJobNumberChange(ValueChangeEvent event) {
		try {
			Byte job = (Byte)event.getNewValue();
			if (job != null) {
				//log.debug(job);
				while (job > weeklyTimecard.getPayJobs().size()) {
					addPayJob(); // Add the PayJob and update the job-number drop-down lists
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * ValueChangeListener method for all PayJob (Job table) daily numeric
	 * fields.
	 *
	 * @param event Event data (new and old) supplied by the framework.
	 */
	public void listenJobDailyChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			// Calculate Job table Total Hours and MPVs for week
			if (weeklyTimecard != null) {
				TimecardCalc.calculatePayJobHours(weeklyTimecard);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Add a new Pay Job to the current timecard.
	 */
	private void addPayJob() {
		// get rid of the "insert" line in the pay breakdown table
		removeExtraPbLine(weeklyTimecard);
		// then add a new Job and save the timecard
		getWeeklyTimecardDAO().addAndSavePayJob(weeklyTimecard);
		// then restore the "insert" line in the pay breakdown table
		insertExtraPbLine(weeklyTimecard);
		createJobNumberDL();
	}

	/**
	 * Determine if the rate stored in the weeklyTimecard still matches the
	 * corresponding rate in the associated StartForm. If not, issue a message.
	 *
	 * @param wtc The timecard to check.
	 */
	private void checkTimecardRate(WeeklyTimecard wtc) {
		WorkZone workZone = wtc.getDefaultZone();
		WeeklyTimecard emptyTc = new WeeklyTimecard();
		emptyTc.setEndDate(wtc.getEndDate());
		TimecardUtils.fillRates(emptyTc, wtc.getStartForm(), getProduction(), workZone, null);

		if (wtc.getRate() != null) {
			if (emptyTc.getRate() == null ||
					wtc.getRate().compareTo(emptyTc.getRate()) != 0) {
				MsgUtils.addFacesMessage("Timecard.Rate.Mismatch", FacesMessage.SEVERITY_ERROR);
			}
		}
		else if (emptyTc.getRate() != null) {
			MsgUtils.addFacesMessage("Timecard.Rate.Mismatch", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * @return True if the "exempt" employee status is the same on the current timecard as on the
	 * associated StartForm; false otherwise.
	 */
	private boolean checkExemptMatches() {
		boolean bRet = true;
		StartForm sf;
		if (weeklyTimecard != null && (sf = weeklyTimecard.getStartForm()) != null) {
			if (weeklyTimecard.getAllowWorked() != sf.getAllowWorked()) {
				PopupBean.getInstance().show(null, 0,
						"Timecard.ExemptMismatch.Title",
						"Timecard.ExemptMismatch." + (weeklyTimecard.getAllowWorked() ? "TcExempt" : "TcNonExempt") + ".Text",
						"Confirm.Close", null); // no cancel button
				bRet = false;
			}
		}
		return bRet;
	}

	/**
	 * Determine if any reported hours on the given timecard are after the
	 * related StartForm's work-end date or effective end date, or before the
	 * start date(s). If so, issue an appropriate message.
	 *
	 * @param wtc The timecard to check.
	 */
	private void checkWorkStartEndDates(WeeklyTimecard wtc) {
		boolean usingEmpl = true;	// true if the Employment end date is the earliest
		StartForm sf = wtc.getStartForm();
		// Check both employment.endDate & StartForm dates; issue different messages for each
		if ((sf.getEmployment() != null && sf.getEmployment().getEndDate() != null) || sf.getEarliestEndDate() != null) {
			Date endDate = sf.getEmployment().getEndDate();
			if (endDate == null ||
					(sf.getEarliestEndDate() != null && sf.getEarliestEndDate().before(endDate))) {
				// StartForm date(s) is earlier than Employment
				endDate = sf.getEarliestEndDate();
				usingEmpl = false; // remember to use "work end date" message
			}
			if (! endDate.after(wtc.getEndDate())) {
				// See if any hours reported after end date
				for (DailyTime dt : wtc.getDailyTimes()) {
					if (dt.reportedWork()) {
						if (dt.getDate().after(endDate)) {
							if (usingEmpl) {
								MsgUtils.addFacesMessage("Timecard.StartForm.Employment.EndDate", FacesMessage.SEVERITY_ERROR);
							}
							else {
								MsgUtils.addFacesMessage("Timecard.StartForm.EarliestEndDate", FacesMessage.SEVERITY_ERROR);
							}
							break;
						}
					}
				}
			}
		}

		// Determine later of Work Start and Effective Start dates
		Date start = sf.getWorkStartOrHireDate();
		usingEmpl = false;
		if (sf.getEffectiveStartDate() != null && sf.getEffectiveStartDate().after(start)) {
			start = sf.getEffectiveStartDate();
			usingEmpl = true; // use appropriate error message
		}
		// Check for any time entered prior to start date
		for (DailyTime dt : wtc.getDailyTimes()) {
			if (dt.getDate().before(start)) {
				if (dt.reportedWork()) {
					if (usingEmpl) {
						MsgUtils.addFacesMessage("Timecard.StartForm.EffectiveStart", FacesMessage.SEVERITY_ERROR);
					}
					else {
						MsgUtils.addFacesMessage("Timecard.StartForm.WorkStart", FacesMessage.SEVERITY_ERROR);
					}
					break;
				}
			}
			else { // reached a timecard date that is equal to or passed the StartForm's date...
				break; // ...all done.
			}
		}
	}

	/**
	 * Find the index (origin zero) of the current WeeklyTimeCard within our
	 * current list of timecards.  This is used for navigating forward and
	 * backward through the list.
	 *
	 * @return The index of the current timecard, or -1 if it is not in the
	 *         list.
	 */
	private int findCurrentTimecardIndex() {
		getTimecardEntryList();
		int ix = 0;
		if (weeklyTimecard != null && weeklyTimecard.getId() != null) {
			int id = weeklyTimecard.getId();
			for (TimecardEntry tce : timecardEntryList) {
				if (id == tce.getWeeklyTc().getId()) {
					break;
				}
				ix++;
			}
		}
		if (ix >= timecardEntryList.size()) {
			ix = -1;
		}
		return ix;
	}

	/**
	 * Create list of time cards available to this person, based on the current values
	 * of archiveDate and departmentId.
	 */
	protected void createTimecardList() {
		if (getNotInProduction()) {
			timecardEntryList = new ArrayList<>();
		}
		else {
			timecardEntryList = TimecardUtils.createTimecardList(this, getFilterBean(), false);
		}
	}

	/**
	 * @see com.lightspeedeps.web.approver.TimecardListHolder#sortTimecards(java.util.List)
	 */
	@Override
	public void sortTimecards(List<TimecardEntry> tceList) {
		String col = SessionUtils.getString(ATTR_SORT_NAME_PREFIX + ApproverDashboardBean.ATTR_SORT_PREFIX_DASH);
		setSortColumnName(col);
		boolean asc = SessionUtils.getBoolean(ATTR_SORT_ORDER_PREFIX + ApproverDashboardBean.ATTR_SORT_PREFIX_DASH, true);
		setAscending(asc);
		Collections.sort(tceList, getComparator());
	}

	/**
	 * @see com.lightspeedeps.web.timecard.TimecardBase#getComparator()
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
	 * @see com.lightspeedeps.web.approver.FilterHolder#changeTab(int,int)
	 */
	@Override
	public void changeTab(int oldTab, int newTab) {
		// No changes necessary when user switches mini-tabs
	}

	/**
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#createList(com.lightspeedeps.type.FilterType)
	 */
	@Override
	public List<SelectItem> createList(FilterType type) {
		List<SelectItem> list = null;
		switch(type) {
			case DEPT:
				list = createDepartmentList();
				break;
			case BATCH:
				list = createBatchList(getWeekEndDate());
				break;
			case STATUS:
				list = EnumList.getWeeklyStatusFilterList();
				break;
			case NAME: // not used in FullTimecardBean
			case DATE: // not used in FullTimecardBean
			case UNION:
			case N_A:
				break;
			}
		return list;
	}

	/**
	 * Create the list of Department`s for the drop-down list. For a Production
	 * Approver the list includes all Departments; for a departmental approver,
	 * it includes just those Departments for which the Contact is an approver.
	 * <p>
	 * An "All" entry is added at the top of the list if there is more than one
	 * department.
	 *
	 * @return the list of Departments as SelectItem`s, where the value field is
	 *         the Department.id field.
	 */
	private List<SelectItem> createDepartmentList() {
		boolean resetDeptId = false;	// do we need to set departmentId to 0=All?
		if (getUserHasViewHtg() || ApproverUtils.isProdApprover(getvContact())) {
			departmentList = new ArrayList<>(DepartmentUtils.getDepartmentDataAdminDL());
		}
		else {
			resetDeptId = true; // reset id if not found in list
			// Find depts with this user as approver; if AICP, use current project
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(getvContact(), getCommProject());
			departmentList = new ArrayList<>();
			for (Department dept : depts) {
				departmentList.add(new SelectItem(dept.getId(), dept.getName()));
				if (dept.getId().equals(departmentId)) {
					resetDeptId = false; // found it, leave dept id unchanged
				}
			}
		}
		departmentList.add(0, new SelectItem(0, "All"));
		if (resetDeptId) {
			departmentId = 0;
		}
		return departmentList;
	}

	/**
	 * Create a List of SelectItem`s describing the WeeklyBatch objects in the
	 * current Production, for the given week-ending date. The SelectItem.value
	 * field will be the WeeklyBatch.id, and the SelectItem.label will be the
	 * name of the batch.
	 *
	 * @param weekEndDate The week-ending date of interest.
	 * @return A non-null, possibly empty, List of SelectItem`s as described.
	 */
	private List<SelectItem> createBatchList(Date weekEndDate) {
		return WeeklyBatchUtils.createBatchList(getViewProduction(), getViewProject(), weekEndDate);
	}

	private void createCategoryDL() {
		categoryDL = EnumList.createEnumValueSelectList(PayCategory.class);
		categoryDL.add(0, new SelectItem(null, "Add Line Item"));
	}

	/**
	 * Create the job-number selection list when needed.
	 */
	private void createJobNumberDL() {
		jobNumberDL = new ArrayList<>();
		byte max = 1;
		for (PayJob pj : weeklyTimecard.getPayJobs()) {
			if (pj.getJobNumber() > max) {
				max = pj.getJobNumber();
			}
		}
		max++; // we want one more entry in list than current Jobs
		for (byte i = 1; i <= max; i++) {
			jobNumberDL.add(new SelectItem(new Byte(i),""+i));
		}
	}

	/**
	 * Create the drop-down selection list for the "Insert row"
	 * pop-up for the Pay Breakdown table.
	 */
	private void createPayLineNumberDL() {
		payLineNumberDL = new ArrayList<>();
		if (weeklyTimecard != null) {
			for (int i = 1; i < weeklyTimecard.getPayLines().size(); i++) {
				payLineNumberDL.add(new SelectItem(i, ""+i));
			}
		}
	}

	/**
	 * @return the string to be displayed in the "Rate:" area of the timecard
	 * header.  This may include either the hourly rate, or the daily or weekly
	 * rate, or both.
	 */
	public String getRatePair() {
		String rate = "";
		DecimalFormat fmt = new DecimalFormat("#,##0.00##");
		if (weeklyTimecard != null) {
			if (weeklyTimecard.getHourlyRate() != null && weeklyTimecard.getHourlyRate().signum() > 0) {
				rate = fmt.format(weeklyTimecard.getHourlyRate());
			}
			if (weeklyTimecard.getEmployeeRateType() == EmployeeRateType.DAILY) {
				if (weeklyTimecard.getDailyRate() != null && weeklyTimecard.getDailyRate().signum() > 0) {
					if (rate.length() > 0) {
						if (weeklyTimecard.isNonUnion()) {
							rate = ""; // don't show hourly if exempt, non-union.
						}
						else {
							rate += " / ";
						}
					}
					rate += fmt.format(weeklyTimecard.getDailyRate()) + "/day";
				}
			}
			else if (weeklyTimecard.getEmployeeRateType() == EmployeeRateType.WEEKLY) {
				if (weeklyTimecard.getWeeklyRate() != null && weeklyTimecard.getWeeklyRate().signum() > 0) {
					if (rate.length() > 0) {
						if (weeklyTimecard.isNonUnion()) {
							rate = ""; // don't show hourly if exempt, non-union.
						}
						else {
							rate += " / ";
						}
					}
					rate += fmt.format(weeklyTimecard.getWeeklyRate()) + "/week";
				}
			}
		}
		return rate;
	}

	/**
	 * ValueChangeListener method for the category field in the Pay
	 * Breakdown table.
	 *
	 * @param event Event data (new and old) supplied by the framework.
	 */
	public void listenCategoryChange(ValueChangeEvent event) {
		try {
			if (weeklyTimecard != null) {
				log.debug("......................>>> Category field Change for weeklyTimecard");
				UIData pay = (UIData)event.getComponent().findComponent("cpaybrk"); // id of data table in fulltcPayBreakdown
				Integer row = null;
				if (pay != null) {
					row = pay.getRowIndex();
					log.debug("...................field Row no. =" + row);
					List<PayBreakdown> payLineList = new ArrayList<>(weeklyTimecard.getPayLines());
					PayBreakdown payBreakdown = payLineList.get(row);
					String description = payBreakdown.getCategory();
					checkEventValidity(event, description);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	public String getOccCodeSchedule() {
		if (occCodeSchedule == null) {
			occCodeSchedule = "";
			if (weeklyTimecard != null && weeklyTimecard.getOccCode() != null) {
				occCodeSchedule = weeklyTimecard.getOccCode();
				StartForm sf = weeklyTimecard.getStartForm();
				if (sf.getContractSchedule() != null && ! sf.getContractSchedule().startsWith("X")) {
					occCodeSchedule += " / " + sf.getContractSchedule();
				}
			}
		}
		return occCodeSchedule;
	}

	@Override
	public PayrollPreference getPayrollPref() {
		if (getCommProject() != null) {
			return getCommProject().getPayrollPref();
		}
		else {
			return getProduction().getPayrollPref();
		}
	}

	/**See {@link #filterBean}. */
	public FilterBean getFilterBean() {
		return filterBean;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getItemList() {
		return getTimecardEntryList();
	}

	/** See {@link #timecardEntryList}. */
	public List<TimecardEntry> getTimecardEntryList() {
		if (timecardEntryList == null) {
			createTimecardList();
		}
		return timecardEntryList;
	}

	/**See {@link #filterByList}. */
	public List<SelectItem> getFilterByList() {
		if (filterByList == null) {
			filterByList = FilterBean.createFilterByList();
		}
		return filterByList;
	}
	/**See {@link #filterByList}. */
	public void setFilterByList(List<SelectItem> filterList) {
		filterByList = filterList;
	}

	/**See {@link #batchId}. */
	@Override
	public Integer getBatchId() {
		return batchId;
	}
	/**See {@link #batchId}. */
	@Override
	public void setBatchId(Integer batchId) {
		this.batchId = batchId;
		if (batchId != null && filterBean.getFilterType() == FilterType.BATCH) {
			filterBean.setSelectFilterValue(batchId.toString());
		}
	}

	/** See {@link #departmentId}. */
	@Override
	public Integer getDepartmentId() {
		return departmentId;
	}
	/** See {@link #departmentId}. */
	@Override
	public void setDepartmentId(Integer deptId) {
		departmentId = deptId;
		if (deptId != null && filterBean.getFilterType() == FilterType.DEPT) {
			filterBean.setSelectFilterValue(deptId.toString());
		}
	}

	/** See {@link #statusFilter}. */
	@Override
	public ApprovableStatusFilter getStatusFilter() {
		return statusFilter;
	}
	/** See {@link #statusFilter}. */
	@Override
	public void setStatusFilter(ApprovableStatusFilter status) {
		statusFilter = status;
		if (statusFilter != null && filterBean.getFilterType() == FilterType.STATUS) {
			filterBean.setSelectFilterValue(statusFilter.name());
		}
	}

	/**
	 * @see com.lightspeedeps.web.approver.TimecardListHolder#getProduction()
	 */
	@Override
	public Production getProduction() {
		return getViewProduction();
	}

	@Override
	public Project getProject() {
		if (! SessionUtils.getBoolean(Constants.ATTR_FULL_TC_SHOW_ALL, false)) {
			return getCommProject();
		}
		return getViewProject();
	}

	/**
	 * The "show all projects" (or aggregate) option for Full Timecard page is
	 * based on the setting from either the Approver Dashboard, or the
	 * individual (Basic) timecard page. Those beans will set the appropriate
	 * option in the session.
	 *
	 * @see com.lightspeedeps.web.approver.TimecardListHolder#getShowAllProjects()
	 */
	@Override
	public boolean getShowAllProjects() {
		return SessionUtils.getBoolean(Constants.ATTR_FULL_TC_SHOW_ALL, false);
	}

	/**
	 * @see com.lightspeedeps.web.approver.TimecardListHolder#getEmployeeAccount()
	 * @return null, since the Full Timecard page does not support filtering by employee.
	 */
	@Override
	public String getEmployeeAccount() {
		return null;
	}

	/** See {@link #jobTotalMismatch}. */
	public boolean getJobTotalMismatch() {
		return jobTotalMismatch;
	}
	/** See {@link #jobTotalMismatch}. */
	public void setJobTotalMismatch(boolean jobTotalMismatch) {
		this.jobTotalMismatch = jobTotalMismatch;
	}

	/** See {@link #deleteJobNumber}. */
	public int getDeleteJobNumber() {
		return deleteJobNumber;
	}
	/** See {@link #deleteJobNumber}. */
	public void setDeleteJobNumber(int deleteJobNumber) {
		this.deleteJobNumber = deleteJobNumber;
	}

	/** See {@link #deletePayBreakdownIx}. */
	public int getDeletePayBreakdownIx() {
		return deletePayBreakdownIx;
	}
	/** See {@link #deletePayBreakdownIx}. */
	public void setDeletePayBreakdownIx(int deletePayBreakdownIx) {
		this.deletePayBreakdownIx = deletePayBreakdownIx;
	}

	/** See {@link #showScrollArrows}. */
	public boolean getShowScrollArrows() {
		if (showScrollArrows == null) {
			// check approver dashboard setting first...
			showScrollArrows = SessionUtils.getBoolean(Constants.ATTR_FULL_TC_SHOW_ALL, false);
			if (! showScrollArrows) {
				if (getNotInProduction()) {
					// "My Timecards" - leave it false
				}
				else if (! getProduction().getType().isAicp()) {
					showScrollArrows = true; // non-commercial, allow scroll arrows
				}
				else if (weeklyTimecard != null && weeklyTimecard.getStartForm() != null) {
					// allow scroll arrows if current project = timecard's project
					showScrollArrows = getCommProject().equals(weeklyTimecard.getStartForm().getProject());
				}
			}
		}
		return showScrollArrows.booleanValue();
	}

	/** See {@link #showAllHistory}. */
	public boolean getShowAllHistory() {
		return showAllHistory;
	}
	/** See {@link #showAllHistory}. */
	public void setShowAllHistory(boolean showAllHistory) {
		this.showAllHistory = showAllHistory;
	}

	/**See {@link #showAuditDetail}. */
	public boolean getShowAuditDetail() {
		return showAuditDetail;
	}
	/**See {@link #showAuditDetail}. */
	public void setShowAuditDetail(boolean b) {
		if (b != showAuditDetail) {
			auditTrail = null; // refresh it
		}
		showAuditDetail = b;
	}

	/**See {@link #auditTrail}. */
	public String getAuditTrail() {
		if (auditTrail == null && weeklyTimecard != null) {
			auditTrail = AuditUtils.getTrail(ObjectType.WT, weeklyTimecard.getId(), showAuditDetail);
			auditTrail = StringUtils.saveHtml(auditTrail);
		}
		return auditTrail;
	}
	/**See {@link #auditTrail}. */
	public void setAuditTrail(String auditTrail) {
		this.auditTrail = auditTrail;
	}

	/**See {@link #expandJobs}. */
	public boolean getExpandJobs() {
		return expandJobs;
	}
	/**See {@link #expandJobs}. */
	public void setExpandJobs(boolean expandJobs) {
		this.expandJobs = expandJobs;
	}

	/**See {@link #expandSplit}. */
	public boolean getExpandSplit() {
		return expandSplit;
	}
	/**See {@link #expandSplit}. */
	public void setExpandSplit(boolean expandSplit) {
		this.expandSplit = expandSplit;
	}

	/** Selection list contents for Studio/Distant(Location) rate type choice. */
	public SelectItem[] getRateTypeDL() {
		return WeeklyTimecard.RATE_TYPE_SELECTION;
	}

	/** @return the retirementPlanDL value from the StartFormService -- the
	 * drop-down list of Retirement Plan selections. */
	public List<SelectItem> getRetirementPlanDL() {
		return StartFormService.getRetirementPlanDL();
	}

	/** See {@link #jobNumberDL}. */
	public List<SelectItem> getJobNumberDL() {
		if (jobNumberDL == null) {
			createJobNumberDL();
		}
		return jobNumberDL;
	}
	/** See {@link #jobNumberDL}. */
	public void setJobNumberDL(List<SelectItem> jobNumberDL) {
		this.jobNumberDL = jobNumberDL;
	}

	/** See {@link #categoryDL}. */
	public List<SelectItem> getCategoryDL() {
		if (categoryDL == null) {
			createCategoryDL();
		}
		return categoryDL;
	}

	/** See {@link #pbCategory}. */
	public PayCategory getPbCategory() {
		return pbCategory;
	}
	/** See {@link #pbCategory}. */
	public void setPbCategory(PayCategory pbCategory) {
		this.pbCategory = pbCategory;
	}

	/** See {@link #categoryDL}. */
	public List<SelectItem> getPhaseDL() {
		return EnumList.getProductionPhaseList();
	}

	/** See {@link #showAddPayLine}. */
	public boolean getShowAddPayLine() {
		return showAddPayLine;
	}
	/** See {@link #showAddPayLine}. */
	public void setShowAddPayLine(boolean showAddPayLine) {
		this.showAddPayLine = showAddPayLine;
	}

	/** See {@link #payLineNumber}. */
	public Integer getPayLineNumber() {
		return payLineNumber;
	}
	/** See {@link #payLineNumber}. */
	public void setPayLineNumber(Integer payLineNumber) {
		this.payLineNumber = payLineNumber;
	}

	/** See {@link #payLineNumberDL}. */
	public List<SelectItem> getPayLineNumberDL() {
		if (payLineNumberDL == null ||
				(weeklyTimecard != null && payLineNumberDL.size() != weeklyTimecard.getPayLines().size()-1)) {
			createPayLineNumberDL();
		}
		return payLineNumberDL;
	}
	/** See {@link #payLineNumberDL}. */
	public void setPayLineNumberDL(List<SelectItem> pbLineNumberDL) {
		payLineNumberDL = pbLineNumberDL;
	}

	/** See {@link #modifyJobNumber}. */
	public int getModifyJobNumber() {
		return modifyJobNumber;
	}
	/** See {@link #modifyJobNumber}. */
	public void setModifyJobNumber(int modifyJobNumber) {
		this.modifyJobNumber = modifyJobNumber;
	}

	/** See {@link #multiplierNumber}. */
	public long getMultiplierNumber() {
		return multiplierNumber;
	}
	/** See {@link #multiplierNumber}. */
	public void setMultiplierNumber(long multiplierNumber) {
		this.multiplierNumber = multiplierNumber;
	}

	/** See {@link #premium}. */
	public boolean getPremium() {
		return premium;
	}
	/** See {@link #premium}. */
	public void setPremium(boolean premium) {
		this.premium = premium;
	}

	/** See {@link #multiplier}. */
	public BigDecimal getMultiplier() {
		return multiplier;
	}
	/** See {@link #multiplier}. */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/** See {@link #showSetMultiplier}. */
	public boolean getShowSetMultiplier() {
		return showSetMultiplier;
	}
	/** See {@link #showSetMultiplier}. */
	public void setShowSetMultiplier(boolean showSetMultiplier) {
		this.showSetMultiplier = showSetMultiplier;
	}

	/** See {@link #disablePayBreakdownLines}. */
	public Boolean getDisablePayBreakdownLines() {
		if(disablePayBreakdownLines == null) {
			disablePayBreakdownLines = false;

			PayrollPreference pf = getViewProduction().getPayrollPref();
			if(pf.getPayrollService() != null) {
				disablePayBreakdownLines =  pf.getPayrollService().getDisablePayBreakdownLines();
			}
		}
		return disablePayBreakdownLines;
	}

	/** See {@link #isEmpDeptCast}. */
	public boolean getIsEmpDeptCast() {
		return isEmpDeptCast;
	}

	/** See {@link #isEmpDeptCast}. */
	public void setIsEmpDeptCast(boolean isEmpDeptCast) {
		this.isEmpDeptCast = isEmpDeptCast;
	}

	/**
	 * Invoke the base class' preDestroy method.
	 * @see com.lightspeedeps.web.timecard.TimecardBase#preDestroy()
	 */
	@Override
	@PreDestroy
	public void preDestroy() {
		log.debug("");
		super.preDestroy();
	}

	/**
	 * This method is called when this bean is about to go 'out of scope', e.g.,
	 * when the user is leaving the page or their session expires. We use it to
	 * release any resources that might cause Hibernate issues.
	 */
	@Override
	public void dispose() {
		log.debug("");
		super.dispose();
		timecardEntryList = null;
	}

}
