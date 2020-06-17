package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.chart.Axis;
import org.icefaces.ace.component.chart.AxisType;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.chart.CartesianSeries;
import org.icefaces.ace.model.chart.CartesianSeries.CartesianType;
import org.icefaces.ace.model.table.RowState;

//import com.icesoft.faces.component.outputchart.OutputChart;
import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.DprDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.service.TimecardExport;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardPrintUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.payroll.WeeklyBatchUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.payroll.HtgMessageBean;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupTwoInputBean;
import com.lightspeedeps.web.popup.RejectPromptBean;
import com.lightspeedeps.web.timecard.ExportTimecardBean;
import com.lightspeedeps.web.timecard.PrintTimecardBean;
import com.lightspeedeps.web.timecard.TimecardBase;
import com.lightspeedeps.web.user.ChangePinBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the Approver DashBoard page, Timecard Review
 * and Gross Payroll mini-tabs.
 * <p>
 * The Start Forms and Batch Transfer mini-tabs have their
 * own backing-bean classes.
 */
@ManagedBean
@ViewScoped
public class ApproverDashboardBean extends ApproverFilterBase
		implements ControlHolder, FilterHolder, TimecardListHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(ApproverDashboardBean.class);

	public static final String ATTR_SORT_PREFIX_DASH = "approverDash.";

	private static final int ACT_APPROVE = 12;
	private static final int ACT_REJECT = 13;
	private static final int ACT_LOCK = 14;
	private static final int ACT_REFRESH = 15;
	private static final int ACT_PRINT = 16;
	private static final int ACT_EXPORT = 17;
	private static final int ACT_IMPORT = 18;
	private static final int ACT_HTG = 19;
	private static final int ACT_ONE_HTG = 20;
	private static final int ACT_FRINGE = 21;

	/** Number of entries in the production list above which the JSP should add
	 * pagination controls.  This value should match the "rows=" attribute on the
	 * dataTable tag for the production list. */
	private static int PAGINATE_SIZE = 200;
	{
		PAGINATE_SIZE = ApplicationScopeBean.getInstance().getIsBeta() ? 50 : 200;
	}

	//////////////////////////   CHART   //////////////

	/** list of the Labels for the x axis of the chart */
	private static final List<String> WEEKDAY_LABELS = Constants.WEEKDAY_NAMES;

	/** x-axis chart labels - days of week, based on selected timecard's starting week-day. */
	private List<String> chartLabels;

	/** The list of the legend label for the chart */
   private final List<String> legendLabels = new ArrayList<>(Arrays.asList(
			new String[]{"Crew Avg.", "Dept Avg.", "Current Employee"}));

   /** The list of the data used by the chart */
   private List<CartesianSeries> chartData;

   /** The list of the colors used by the chart */
   private final String[] paints =
		   new String[] {"#800000", //maroon - Crew Avg.
											"#64afff", //sky - Dept Avg.
											"#0000ff"}; //blue - Current Employee

   /** The list of the colors used by the chart */
//   private final List<Color> paints =
//		   new ArrayList<>(Arrays.asList(new Color[]{new Color(128, 0, 0), //maroon - Crew Avg.
//											new Color(100, 175, 255), //sky - Dept Avg.
//												new Color(0, 0, 255)})); //blue - Current Employee
   /** sets the chart type to bar for default */
	private String type = "";//.BAR_CLUSTERED_CHART_TYPE;

	/** Title for the x-axis */
	private String titleX = "Day";

	/** Title for the y-axis */
	private String titleY = "Hours";

	/** Used for bar and bar-clustered horizontal attribute */
	private boolean horizontal = false;

	/** Values to display for the y axis */
	private List<Axis> yAxis;

	/** Values to display for the x axis */
	private Axis xAxis;

	/** Default series used for configuration of the chart. */
	private CartesianSeries configSeries;
	/* *** END of Chart fields */


	/** The WeeklyTimecard.id value of the icon clicked on by the user. */
	private Integer selectedTimecardId;

	/** The backing field for the checkbox in the header area, which acts
	 * as a "master" select/unselect for all enabled checkboxes in the current
	 * timecard list. */
	private TriStateCheckboxExt masterTriState = new TriStateCheckboxExt();

	/** True iff the bottom ("timecard detail") portion of the Timecard Review page
	 * should be displayed. */
	private boolean showDetail = true;

	/** True iff the "View PR" button should be displayed.  It is NOT displayed when,
	 * in a Commercial production, an "aggregate viewer" is looking at a timecard that
	 * is linked to a project other than the current project. */
	private boolean enableViewDpr = true;

	/** True if the "change PIN" dialog should be displayed. */
	private boolean showChangePin;

	/** True if the "Reject" dialog should be displayed. */
	private boolean showReject;

	private Integer priorUnapprovedTcs = null;

	/** The current Project's preference setting for the week-ending day
	 * (1=Sunday, 7=Saturday). */
	int weekEndDay = TimecardUtils.findWeekEndDay();

	/** Array of day names used for column headers on Timecard Review page. This is
	 * typically 'Sun' through 'Sat', but will change if the Project has set the
	 * 'Start of week' preference to something other than Sunday. */
	private String[] timecardDays;

	/** True iff the Department of the currently selected timecard is normally
	 * displayed on the DPR.  Used in the JSP to skip "difference" highlighting
	 * when the employee's timecard is not on the DPR. */
	private boolean deptOnDpr;

	/** True iff the DPR selection popup should be displayed. */
	private boolean dprPopup = false;

	/** Date of DPR requested to view in popup. */
	private Date dprDate;

	private final Dpr[] weekOfDprs = new Dpr[7];

	/** The list of objects containing daily information from the currently
	 * selected WeeklyTimecard, for use in the JSP detail area comparing the
	 * time card to DPR information. */
	private List<DayEntry> dayEntry = null;

	/** Popup DPR days selection list; the values are Date, and the labels are mm/dd/yyyy */
	private List<SelectItem> popupDprDayList = new ArrayList<>();

	/** Map of department names to Boolean indicating if the department is included
	 * on the PR; keep these in a map so we only have to look it up once. */
	private final Map<String, Boolean> deptPrMap = new HashMap<>(30);

	/**
	 * default constructor
	 */
	public ApproverDashboardBean() {
		super(WeeklyTimecard.SORTKEY_STATUS);
		log.debug("Init");

		setSortAttributePrefix(ATTR_SORT_PREFIX_DASH);
		rowHeight = 24; // set our table row display height - not the usual 26px.

		getFilterBean().register(this, FilterBean.TAB_REVIEW);
		getFilterBean().register(this, FilterBean.TAB_GROSS);

		masterTriState.setHolder(this);
		masterTriState.setCheckValue(TriStateCheckboxExt.CHECK_OFF);

		// Get saved values from last time page was displayed

		showDetail = SessionUtils.getBoolean(Constants.ATTR_APPROVER_SHOW_DETAIL, true);

		setDepartmentId(SessionUtils.getInteger(Constants.ATTR_APPROVER_DEPT));

		String status = SessionUtils.getString(Constants.ATTR_APPROVER_STATUS);
		if (status != null) {
			setStatusFilter(ApprovableStatusFilter.valueOf(status));
		}
		else {
			setStatusFilter(ApprovableStatusFilter.ALL);
		}

		setBatchId(SessionUtils.getInteger(Constants.ATTR_FILTER_BATCH_ID));

		setEmployeeAccount(SessionUtils.getString(Constants.ATTR_APPROVER_EMPLOYEE));
		if (getEmployeeAccount() == null) {
			setEmployeeAccount(Constants.CATEGORY_ALL);
		}

		if (getWeekEndDate() != null && getDepartmentId() != null) { // restore list from these values
			createTimecardList();
			if (timecardEntryList.size() > 0) {
				Integer tcId = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
				if (tcId != null) {
					setupSelectedItem(tcId);
					scrollToRow();
				}
				else {
					setupFirst();
				}
			}
		}

		if (weeklyTimecard == null) { // nothing selected yet
			if (getWeekEndDate() == null) { // get the default W/E date
				setWeekEndDate(TimecardUtils.calculateDefaultWeekEndDate()); // Set end date as Saturday date, with time=12:00am
			}
			setDepartmentId(0); // show all departments
			SessionUtils.put(Constants.ATTR_APPROVER_DEPT, getDepartmentId());
			createTimecardList();
//			if (timecardEntryList.size() == 0 && ! getWeekEndDate().equals(Constants.SELECT_ALL_DATE)) {
//				// no entries for current week - try prior week
//				setWeekEndDate(TimecardUtils.calculatePriorWeekEndDate(getWeekEndDate()));
//
//				createTimecardList();	// try again!
//			}
			setupFirst(); // just select first in list
		}

		restoreSortOrder();
	}

	/**
	 * Action method called when the user clicks the "Approve" button. Process
	 * all time card entries that have been "checked".
	 *
	 * @return null navigation string
	 */
	public String actionApprove() {
		try {
			int n = getSelectedItemCount();
			if (n == 0 ) {
				MsgUtils.addFacesMessage("Timecard.NoneSelected", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			User user = SessionUtils.getCurrentUser();
			if (user.getPin() == null) {
				showChangePin = true;
				ChangePinBean.getInstance().show(this);
				addFocus("pin");
				return null;
			}

			// make sure all the selected timecards are still in "ready for approval" state
			List<Integer> contactIdList = new ArrayList<>();
			String msg = validateApproveList(timecardEntryList, contactIdList);
			if (msg == null) { // error - no selected timecards ready for approval
				return null;	// facesMessage already queued; just return.
			}
			else if (msg.length() == 0) { // normal case
				msg = null;	// don't give any additional message in prompt
			}

			if (production.getPayrollPref().getPayrollService().getTeamPayroll()) {
				// Check for un-calculated time cards
				boolean missingPaymentInfo = false;

				for (TimecardEntry tcEntry : timecardEntryList) {
					if (tcEntry.getChecked() || tcEntry.getWeeklyTc().getMarkedForApproval()) {
						// See if selected timecards are missing information to perform a calculation
						if (TimecardUtils.checkForMissingPaymentInfo(tcEntry.getWeeklyTc())) {
							missingPaymentInfo = true;
							break;
						}
					}
				}
				if (missingPaymentInfo) {
					if (msg == null) {
						msg = "";
					}
					msg += MsgUtils.getMessage("Approval.MissingPaymentInfo.MultiTC");
				}
			}

			ApproverUtils.approvePrompt(contactIdList, msg, this, ACT_APPROVE, getMessagePrefix());
			addFocus("approve");
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user has "OK"d the approval pop-up.
	 * @return null navigation string
	 */
	private String actionApproveOk() {
		try {
			approveList();
			setPriorUnapprovedTcs(null); // force refresh of unapproved timecard count
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method called when the user clicks the "Approve" button. Process
	 * all time card entries that have been "checked".
	 *
	 * @return null navigation string
	 */
	public String actionLock() {
		try {
			int n = getSelectedItemCount();
			if (n == 0 ) {
				MsgUtils.addFacesMessage("Timecard.NoneSelected", FacesMessage.SEVERITY_ERROR);
				return null;
			}

			WeeklyTimecard wtc = null;
			int ready = 0;
			int notReady = 0;
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked() || tce.getWeeklyTc().getMarkedForApproval()) {
					wtc = tce.getWeeklyTc();
					wtc = getWeeklyTimecardDAO().refresh(wtc);
					if (wtc.getStatus() == ApprovalStatus.APPROVED) {
						ready++;
					}
					else {
						notReady++;
					}
				}
			}
			if (ready == 0) {
				MsgUtils.addFacesMessage("Approval.Lock.NoFinal", FacesMessage.SEVERITY_ERROR);
				return null;
			}

			PopupBean bean = PopupBean.getInstance();
			bean.show(this, ACT_LOCK, "Approval.LockPrompt.");
			String msg;
			if (notReady > 0) {
				msg = MsgUtils.formatMessage("Approval.Lock.SomeNotFinal", ready);
			}
			else {
				msg = MsgUtils.formatMessage("Approval.LockPrompt.Text", ready);
			}
			bean.setMessage(msg);
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionLockOk() {
		boolean bRet = true;
		try {
			Contact contact = SessionUtils.getCurrentContact();
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked() && tce.getStatus() == ApprovalStatus.APPROVED) {
					bRet &= lockEntry(tce, contact);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		if (! bRet) {
			MsgUtils.addFacesMessage("Approval.Lock.Failed", FacesMessage.SEVERITY_ERROR);
		}

		return null;
	}

	/**
	 * Action method for the "refresh" button on the dashboard. This puts up a
	 * warning pop-up if any check-marks would be lost -- this is for
	 * check-marks on Approved timecards, since those check-marks are in the
	 * TimecardEntry, not in the WeeklyTimecard.
	 *
	 * @return null navigation string
	 */
	public String actionRefresh() {
		try {
			int n = 0;
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked()) {
					n++;
				}
			}
			if (n > 0) {
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_REFRESH, getMessagePrefix()+"RefreshApprove.");
				bean.setMessage(MsgUtils.formatMessage(getMessagePrefix()+"RefreshApprove.Text", n));
				return null;
			}
			actionRefreshOk();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "ok" button on the prompt given for a "refresh" if
	 * some checkboxes are selected; or called immediately from the Refresh
	 * button if no items are checked.
	 *
	 * @return null navigation string
	 */
	private String actionRefreshOk() {
		try {
			clearDprs();	// Force refresh of DPRs next time their needed.
			setPriorUnapprovedTcs(null); // force refresh of unapproved timecard count
			Object selectedItem = null;
			if (getSelectedRow() >= 0) {
				selectedItem = getRowItem(getSelectedRow());
				setSelectedRow(-1);
			}
			createTimecardList(); // refresh the list
			if (selectedItem != null) {
				int ix = getItemList().indexOf(selectedItem);
				if (ix >= 0) {
					timecardEntry = (TimecardEntry)getItemList().get(ix);
					setSelected(timecardEntry, true);
					setSelectedRow(ix);
					setupSelectedItem(); // refreshes PR area, etc.
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user clicks the "Reject" button. Does
	 * validation checks and then displays the "Reject" pop-up dialog.
	 *
	 * @return null navigation string
	 */
	public String actionReject() {
		try {
			int n = getSelectedItemCount();
			if (n == 0 ) {
				MsgUtils.addFacesMessage("Timecard.RejectNoneSelected", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (n > 1) {
				PopupBean.getInstance().show(null, 0,
						"Timecard.RejectNotOne.Title", "Timecard.RejectNotOne.Text",
						"Confirm.Close", null);
				return null;
			}
			WeeklyTimecard wtc = null;
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked() || tce.getWeeklyTc().getMarkedForApproval()) {
					wtc = tce.getWeeklyTc();
					wtc = getWeeklyTimecardDAO().refresh(wtc);
					if (wtc.getApproverId() == null) {
						// Can't reject a final-approved timecard
						tce.setWeeklyTc(wtc);
						ApproverUtils.calculateApprovalStatus(tce, SessionUtils.getCurrentContact());
						MsgUtils.addFacesMessage("Approval.CantRejectApproved", FacesMessage.SEVERITY_ERROR);
						return null;
					}
					else if (! ApproverUtils.isNextApprover(wtc, getvContact().getId())) {
						// oops! Current user is no longer the next approver!
						tce.setWeeklyTc(wtc);
						ApproverUtils.calculateApprovalStatus(tce, SessionUtils.getCurrentContact());
						MsgUtils.addFacesMessage("Approval.RemovedFromQueue", FacesMessage.SEVERITY_ERROR);
						return null;
					}
					break;
				}
			}
			if (! lockAndPrompt(wtc, "Reject.")) {
				return null;
			}
			showReject = true;
			TimecardBase.promptReject(wtc, this, getMessagePrefix());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Attempt to lock the current timecard. If the lock fails, put up the
	 * prompt dialog to inform the user of the failure.
	 *
	 * @param wtc The timecard to be locked.
	 * @param msgType The 'additional' part of the message id for the prompt.
	 *            This should be an empty string ("") if the default 'lock
	 *            failed' message is desired. The message id used to look up the
	 *            prompt is <"Timecard.CardLocked." + msgType + "Text">.
	 * @return True iff the current WeeklyTimecard has been locked.
	 */
	private boolean lockAndPrompt(WeeklyTimecard wtc, String msgType) {
		if (! getWeeklyTimecardDAO().lock(wtc, getvUser())) {
			PopupBean.getInstance().show(null, 0,
					"Timecard.CardLocked.Title",
					"Timecard.CardLocked." + msgType + "Text",
					"Confirm.OK", null); // no cancel button
			log.debug("reject/etc prevented: locked by user #" + wtc.getLockedBy());
			return false;
		}
		return true;
	}

	/**
	 * Execute the reject function - called when user OK's the Reject pop-up.
	 * Although this code was written to support multiple checked items for
	 * rejection, we currently only allow a single item to be checked (enforced
	 * in actionReject()).
	 *
	 * @return null navigation string
	 */
	private String actionRejectOk() {
		try {
			showReject = false;
			Contact apprContact = SessionUtils.getCurrentContact();
			RejectPromptBean bean = RejectPromptBean.getInstance();
			Integer approverId = bean.getSelectedApproverId();
			Integer sendToId = bean.getSelectedContactId();
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getWeeklyTc().getMarkedForApproval()) {
					WeeklyTimecard wtc = tce.getWeeklyTc();
					wtc = getWeeklyTimecardDAO().reject(wtc, getvContact(), sendToId, approverId, bean.getComment());
					tce.setWeeklyTc(wtc);
					ApproverUtils.calculateApprovalStatus(tce, apprContact);
					TimecardCalc.calculateWeeklyTotals(wtc);
					updateItemInList(tce);
					break;	// only one reject at a time currently allowed
				}
			}
			setPriorUnapprovedTcs(null); // force refresh of unapproved timecard count
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user cancels the Reject prompt dialog. Unlock the
	 * timecard.
	 *
	 * @return null navigation string
	 */
	private String actionRejectCancel() {
		showReject = false;
		for (TimecardEntry tce : timecardEntryList) {
			if (tce.getWeeklyTc().getMarkedForApproval()) {
				WeeklyTimecard wtc = tce.getWeeklyTc();
				wtc = getWeeklyTimecardDAO().refresh(wtc);
				getWeeklyTimecardDAO().unlock(wtc, getvUser().getId());
				break;	// only one reject at a time currently allowed
			}
		}
		return null;
	}

	/**
	 * Action method for the "Calc HTG" button (for a single timecard). This
	 * will issue a prompt if the timecard already has Pay Breakdown entries.
	 * Otherwise it will proceed to do the calculation.
	 *
	 * @return null navigation String
	 */
	public String actionCalculateOneHtg() {
		try {
			if (weeklyTimecard.getPayLines().size() > 0) {
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_ONE_HTG, getMessagePrefix()+"CalcOneHtg.");
			}
			else {
				actionCalculateOneHtgOk();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Calc HTG" button (on a single timecard). Called
	 * immediately if the timecard does not have any Pay Breakdown entries, or
	 * called after clicking OK/Calculate in the pop-up prompt.
	 *
	 * @return null navigation String
	 */
	private String actionCalculateOneHtgOk() {
		try {
			boolean selected = weeklyTimecard.getSelected(); // typically true!
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			TimecardService.calculateAllHtgAndUpdate(weeklyTimecard, null);
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			weeklyTimecard.setSelected(selected);
			TimecardEntry tce = findEntry(weeklyTimecard.getId());
			if (tce != null) { // it should be found!
				tce.setWeeklyTc(weeklyTimecard);
				updateItemInList(tce);
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
	 * Action method for both the "View timecard" button (in the PR compare
	 * area) or for clicking on the timecard's status icon.
	 * <p>
	 * Put the timecard's id and the current Department filter id into the
	 * appropriate Session variable, and jump to the full-timecard page. The
	 * field {@link #selectedTimecardId} has been set by a JSF tag. (This is not
	 * necessarily the currently selected timecard if the user clicked on the
	 * status icon.)
	 *
	 * @return the Faces navigation string for the Full Timecard page.
	 */
	public String actionViewTimecard() {
		if (selectedTimecardId != null) {
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, selectedTimecardId);
//			SessionUtils.put(Constants.ATTR_FULL_TC_DEPT, getDepartmentId());
			return HeaderViewBean.PAYROLL_FULL_TC;
		}
		return null;
	}

	/**
	 * Action method for the main page's 'View PR' button; initialize the list
	 * of DPRs to be displayed in the selection list and set the flag to display
	 * the "View DPR" popup.
	 *
	 * @return null navigation String
	 */
	public String actionOpenViewDpr() {
		try {
			dprDate = null;
			popupDprDayList.clear();
			//Set days in the week for the pop up drop-down list
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy - E");
			if (getDayEntry() != null) {
				dprPopup = true; // causes pop-up to be rendered
				for (DayEntry de : getDayEntry()) {
					DailyHours dprDay = de.getDailyHours();
					popupDprDayList.add(new SelectItem(dprDay.getDate(), df.format(dprDay.getDate())));
					if (dprDate == null && dprDay.getHours() != null) {
						dprDate = dprDay.getDate(); // set default entry for selection
					}
				}
			}
			else {
				// This only happens if there's no timecard in the list.
				// In that case, we ignore the click - no popup is displayed.
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "View PR" button on the View DPR pop-up.
	 * @return Navigation string for the Reports page
	 */
	public String actionViewPR() {
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		return HeaderViewBean.REPORTS_MENU_REPORTS;
	}

	/**
	 * Action listener for the close buttons on the "View DPR" popup.
	 */
	public void actionCloseViewDpr(ActionEvent event) {
		dprPopup = false;
	}

	/**
	 * Action method to toggle the display of the "timecard details" section of
	 * the Timecard Review page.
	 *
	 * @return null navigation string
	 */
	public String actionToggleDetail() {
		showDetail = ! showDetail;
		SessionUtils.put(Constants.ATTR_APPROVER_SHOW_DETAIL, showDetail);
		return null;
	}

	/**
	 * Action method for the "view prior unapproved" link. Update the filters
	 * and rebuild the timecard list.
	 *
	 * @return null navigation string
	 */
	public String actionFilterUnapproved() {
		getFilterBean().changeFilterBy(getFilterBean().getFilterType(), FilterType.STATUS);

		setStatusFilter(ApprovableStatusFilter.SUBMITTED);
		SessionUtils.put(Constants.ATTR_APPROVER_STATUS, ApprovableStatusFilter.SUBMITTED.name());

		setWeekEndDate(Constants.SELECT_PRIOR_DATE);
		SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());

		refreshTimecardList(); // update list, probably set up new timecard
		return null;
	}

	/**
	 * Action method for the Print button.  Display the print options
	 * pop-up dialog.
	 * @return null navigation string
	 */
	public String actionPrint() {
		try {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			if (weeklyTimecard == null) { // someone else deleted the current timecard
				MsgUtils.addFacesMessage("Timecard.View.Deleted", FacesMessage.SEVERITY_ERROR);
				actionRefreshOk();
				return null;
			}
			calculateDprTotals(); // got cleared by refresh above
			TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
			TimecardPrintUtils.showPrintOptions(weeklyTimecard, null, getFilterBean().getWeekEndDate(),
					getUserHasViewHtg(), true, this, ACT_PRINT, getUserHasViewHtg(), getShowAllProjects(), true);
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Print the timecards as requested by the user. Called when the user clicks
	 * OK in the print dialog box, via our confirmOk method.
	 *
	 * @return null navigation string
	 */
	private String actionPrintOk() {
		try {
			Collection<Integer> ids = new ArrayList<>();
			PrintTimecardBean bean = PrintTimecardBean.getInstance();
			for (TimecardEntry entry : timecardEntryList) {
				if ((entry.getStatus().isReady() && entry.getWeeklyTc().getMarkedForApproval()) ||
						((entry.getStatus() == ApprovalStatus.APPROVED) && entry.getChecked())) {
					ids.add(entry.getWeeklyTc().getId());
				}
			}
			if (ids.isEmpty() && (bean.getRangeSelectionType() == TimecardSelectionType.SELECT)) {
				MsgUtils.addFacesMessage("Timecard.Print.NoneSelected", FacesMessage.SEVERITY_INFO);
				return null;
			}
			boolean skipAttachments = ! bean.getIncludeAttachments();
			String file = TimecardPrintUtils.printSelectedReport(weeklyTimecard, getProject(), getShowAllProjects(),
					skipAttachments, // 'openWindow' param: if skipping attachments, then have printSelectReport() open browser window
					ids);
			log.debug("file = " + file);
			if (file == null) {
				MsgUtils.addGenericErrorMessage();
				actionRefreshOk();
			}
			return null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the Export/Download button. Just displays
	 * the dialog box with the export options.
	 *
	 * @return null navigation string
	 */
	public String actionExport() {
		if (weeklyTimecard == null) { // none selected, probably none displayed
			return null;
		}
		try {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			if (weeklyTimecard == null) { // someone else deleted the current timecard
				MsgUtils.addFacesMessage("Timecard.View.Deleted", FacesMessage.SEVERITY_ERROR);
				actionRefreshOk();
				return null;
			}
			ExportTimecardBean bean = ExportTimecardBean.getInstance();
			bean.show(getUserHasViewHtg(), this, ACT_EXPORT, weeklyTimecard, getShowAllProjects());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the Export/Download facility, called via our confirmOk
	 * method, when the user clicks Ok/export in the Export dialog box. It
	 * creates a flat file containing selected WeeklyTimecard`s, for importing
	 * into Crew Cards or other payroll-related applications.
	 *
	 * @return null navigation string
	 */
	private String actionExportOk() {
		try {
			ExportTimecardBean bean = ExportTimecardBean.getInstance();
			ExportType exportType = bean.getType();
			if (exportType == ExportType.SHOWBIZ_BUDGET) {
				// requires a second prompt, for the Fringe amount.
				showFringePrompt();
				return null;
			}
			doExport(bean, null, null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Export the set of timecards selected by the user.
	 *
	 * @param bean The export bean, which may hold parameters used in the export
	 *            process.
	 * @param fringe The "fringe" percentage value to apply. Only used for
	 *            Showbiz Budgeting export.
	 * @param poNumber The PO # (Purchase Order number) to insert into the
	 *            export file. Only used for Showbiz Budgeting export.
	 */
	private void doExport(ExportTimecardBean bean, BigDecimal fringe, String poNumber) {
		List<WeeklyTimecard> cards = TimecardExport.createExportList(bean, weeklyTimecard,
				production, getShowAllProjects() ? null : project);
		if (cards.size() > 0) {
			// eliminate any "Void" timecards from the collection. rev 3.1.7737
			Iterator<WeeklyTimecard> i = cards.iterator();
			while (i.hasNext()) {
				WeeklyTimecard tc = i.next();
				if (tc.getStatus() == ApprovalStatus.VOID) {
					i.remove();
				}
			}
		}
		if (cards.size() > 0) {
			String filename = TimecardExport.export(cards, bean.getType(), fringe, poNumber);
			if (filename != null) {
				// open in "same window" ('_self'), since user should get prompt to save as file
				String javascriptCode = "window.open('../../" + filename
						+ "','_self');";
				addJavascript(javascriptCode);
				/** Note: we depend on {@link LsFacesServlet} to encourage the browser to
				 prompt for saving the file (instead of opening it in a browser window)
				 by setting the content-disposition in the response header appropriately.
				 */
				MsgUtils.addFacesMessage("Timecard.Download.Ok", FacesMessage.SEVERITY_INFO, cards.size());
			}
			else {
				MsgUtils.addFacesMessage("Timecard.Download.Failed", FacesMessage.SEVERITY_ERROR);
			}
		}
		else {
			MsgUtils.addFacesMessage("Timecard.Download.NoTimecards", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Called to issue the prompt for a Fringe percentage when the user is doing
	 * an export to Showbiz Budgeting. Puts up a prompt dialog.
	 */
	private void showFringePrompt() {
		PopupTwoInputBean inputBean = PopupTwoInputBean.getInstance();
		inputBean.show( this, ACT_FRINGE, "Timecard.FringePO.");
		inputBean.setNumeric(true);
		inputBean.setMaxLength(4); // nn.n (percent)
		inputBean.setMessage2(MsgUtils.getMessage("Timecard.FringePO.Message2"));
		inputBean.setLeftLabel(MsgUtils.getMessage("Timecard.FringePO.LeftLabel"));
		inputBean.setRightLabel(MsgUtils.getMessage("Timecard.FringePO.RightLabel"));
		inputBean.setMessageB(MsgUtils.getMessage("Timecard.FringePO.MessageB"));
		inputBean.setLeftLabelB(MsgUtils.getMessage("Timecard.FringePO.LeftLabelB"));
		inputBean.setRightLabelB(MsgUtils.getMessage("Timecard.FringePO.RightLabelB"));
		return;
	}

	/**
	 * Action method called when the user "Ok"s the prompt for the fringe
	 * percentage. Called via confirmOk().
	 *
	 * @return null navigation string
	 */
	private String actionPromptFringeOk() {
		PopupTwoInputBean inputBean = PopupTwoInputBean.getInstance();
		BigDecimal decFringe = inputBean.getDecimalInput();
		if (decFringe == null) {
			decFringe = BigDecimal.ZERO;
		}
		String poNumber = inputBean.getInputB();
		doExport(ExportTimecardBean.getInstance(), decFringe, poNumber);
		return null;
	}

	/**
	 * Action method for the "import" button.
	 * @return null navigation string
	 */
	public String actionImport() {
		FileLoadBean bean = FileLoadBean.getInstance();
		bean.setAddedMessageId("Timecard.Import.Loaded");
		bean.show(this, ACT_IMPORT, "Timecard.Import.");
		return null;
	}

	/**
	 * Action method for the "import" button.
	 * @return null navigation string
	 */
	private String actionImportOk() {
		try {
			String filename = "D:\\Dev\\Studio\\samples\\timecards_P25_17155340.tab";
			FileLoadBean bean = FileLoadBean.getInstance();
			filename = bean.getServerFilename();
			log.debug("file=" + filename);
			Collection<WeeklyTimecard> list = getWeeklyTimecardDAO().imports(filename);
			if (list.size() > 0) {
				MsgUtils.addFacesMessage("Timecard.Import.Done", FacesMessage.SEVERITY_INFO, list.size());
			}
			else {
				MsgUtils.addFacesMessage("Timecard.Import.Failed", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionHtg() {
		try {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			if (weeklyTimecard == null) { // someone else deleted the current timecard
				MsgUtils.addFacesMessage("Timecard.View.Deleted", FacesMessage.SEVERITY_ERROR);
				actionRefreshOk();
				return null;
			}
			calculateDprTotals(); // got cleared by refresh above
			TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
			ApproverHtg.showHtgOptions(weeklyTimecard, null, getFilterBean().getWeekEndDate(), getUserHasViewHtg(),
					this, ACT_HTG);
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when user clicks "Ok" (or equivalent) on the
	 * "Run HTG" pop-up dialog.
	 *
	 * @return null navigation string
	 */
	private String actionHtgOk() {
		try {
			List<TimecardMessage> msgs = new ArrayList<>();
			List<Integer> results = ApproverHtg.runHtg(weeklyTimecard, msgs);

			HtgMessageBean.showHtgMessages(results, msgs);

			actionRefreshOk();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.approver.FilterHolder#changeTab(int,int)
	 */
	@Override
	public void changeTab(int oldTab, int newTab) {
		if (newTab != FilterBean.TAB_STARTS) {
			if (getFilterBean().getFilterType() == FilterType.BATCH) {
				setBatchId(getBatchId());
				getFilterBean().setSelectFilterList(null); // force refresh of batch selection list
			}
			else if (getFilterBean().getFilterType() == FilterType.STATUS) {
				getFilterBean().setSelectFilterList(null); // force refresh of status selection list
				if (newTab == FilterBean.TAB_TRANSFER) {
					setStatusFilter(ApprovableStatusFilter.ALL); // set to "All", old setting is irrelevant
				}
			}
		}
		if (oldTab == FilterBean.TAB_TRANSFER || oldTab == FilterBean.TAB_STARTS) {
			timecardEntryList = null;	// so it will get rebuilt; filters may have changed.
//			setEmployeeList(null);		// emp list is usually unchanged 2.9.5505
		}
	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * Upon entry, {@link #getBatchId()} has been set to the new value.
	 */
	@Override
	protected void listenBatchChange() {
		try {
			Integer tcId = saveSelection(); // Save id of existing selected Timecard, if any.
			createTimecardList(); // refresh list of TC's, and updates their status
			SessionUtils.put(Constants.ATTR_FILTER_BATCH_ID, getBatchId());
			reselect(tcId); // Restore the selection, if possible, or select 1st TC if not.
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Department drop-down list
	 * Upon entry, {@link #getDepartmentId()} has been set to the new value.
	 */
	@Override
	protected void listenDeptChange() {
		try {
			SessionUtils.put(Constants.ATTR_APPROVER_DEPT, getDepartmentId());
			refreshTimecardList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Employee drop-down list
	 * Upon entry, {@link #getEmployeeAccount()} has been set to the new value.
	 */
	@Override
	protected void listenEmployeeChange() {
		try {
			SessionUtils.put(Constants.ATTR_APPROVER_EMPLOYEE, getEmployeeAccount());
			refreshTimecardList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Status drop-down filter list
	 */
	@Override
	protected void listenStatusChange() {
		try {
			SessionUtils.put(Constants.ATTR_APPROVER_STATUS, getStatusFilter().name());
			refreshTimecardList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 * Upon entry, {@link #getWeekEndDate()} has been set to the new value.
	 */
	@Override
	protected void listenWeekEndChange() {
		try {
			SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());
			Integer tcId = saveSelection();
			if (getFilterBean().getFilterType() == FilterType.BATCH) {
				getFilterBean().setSelectFilterList(null); // force refresh of batch selection list
				getFilterBean().getSelectFilterList();	// must refresh filter list before we refresh TC list
			}
			clearDprs();
			createTimecardList();
			reselect(tcId);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for the checkboxes on each timecard.
	 * @param event  The event created by the framework.
	 */
	public void listenCheckEntry(ValueChangeEvent event) {
		try {
			log.debug("checkbox=" + event.getNewValue());
			if (event.getNewValue() != null) {
				boolean checked = (Boolean)event.getNewValue();
				if (! checked) {
					masterTriState.setUnchecked();
				}
				TimecardEntry tce = (TimecardEntry)ServiceFinder.getManagedBean("tcEntry");
				if (tce != null) {
					timecardEntry = tce;
					WeeklyTimecard wtc = timecardEntry.getWeeklyTc();
					if (wtc != null) {
						wtc = getWeeklyTimecardDAO().refresh(wtc);
						wtc.setMarkedForApproval(checked);
						getWeeklyTimecardDAO().attachDirty(wtc);
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
	 * Listener for the header-area "master" tri-state checkbox.
	 * This just passes the event to the checkbox object to handle.
	 * @param event The change event created by the framework.
	 */
	public void listenCheckMasterEntry(ValueChangeEvent event) {
		masterTriState.listenChecked(event);
	}

	@Override
	public void listenRowClicked(SelectEvent evt) {
		UIData ud = (UIData)evt.getComponent();
		try {

			FacesContext facesContext = FacesContext.getCurrentInstance();
			Map<String, String> requestMap =
					facesContext.getExternalContext().getRequestParameterMap();

			if (requestMap != null) {
				String baseId = ud.getClientId();
				int id = -1;
				TimecardEntry tce = null;
				baseId += "_instantSelectedRowIndexes";

				String selectedIndexStr = requestMap.get(baseId);
				int selectedIndex = Integer.parseInt(selectedIndexStr);

				if(selectedIndex >= timecardEntryList.size()) {
					// Make sure we don't past the last object.
					selectedIndex = 0;
				}
				else {
					 tce = timecardEntryList.get(selectedIndex);
					id = tce.getWeeklyTc().getId();
				}

				setSelectedRow(tce);
				setSelectedRow(selectedIndex);
				setupSelectedItem(id);
			}
		}
		catch (Exception e) {
			log.error("error: ", e);
		}
		ApplicationUtils.dumpParams(null);
	}

	/**
	 * Rebuild our timecard list to match the current filter settings.
	 */
	@Override
	protected void refreshTimecardList() {
		Integer tcId = saveSelection();
		createTimecardList();
		reselect(tcId);
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
					timecardEntry = tce;
					found = true;
					break;
				}
			}
		}
		if (found) {
			setupSelectedItem(tcId);
		}
		else {
			setupFirst(); // display first item in list
		}
	}

	/**
	 * Called when the user clicks on the "master" selection checkbox in the header
	 * area of the timecard list.  This will set all the available (enabled) checkboxes
	 * in the list to either checked or unchecked.
	 *
	 * @see com.lightspeedeps.object.ControlHolder#clicked(javax.faces.event.ActionEvent, java.lang.Object)
	 */
	@Override
	public void clicked(TriStateCheckboxExt chkBox, Object id) {
		try {
			if (masterTriState.isPartiallyChecked()) { // skip partial-check state
				masterTriState.setChecked();
			}
			Contact currentApprContact = SessionUtils.getCurrentContact();
			boolean updated;
			for (TimecardEntry tce : getTimecardEntryList()) {
				if (tce.getWeeklyTc() != null) {
					updated = false;
					if (masterTriState.isUnchecked()) {
						if (tce.getStatus().isReady() && tce.getWeeklyTc().getMarkedForApproval()) {
							// ensure "isReady" & "markedForApproval" values are current
							refreshStatus(tce, currentApprContact); // update, then re-check
							if (tce.getStatus().isReady() && tce.getWeeklyTc().getMarkedForApproval()) {
								tce.getWeeklyTc().setMarkedForApproval(false);
								updated = true;
							}
						}
						if (tce.getChecked()) {
							tce.setChecked(false);
						}
					}
					else {
						if (tce.getStatus().isReady()) {
							if (! tce.getWeeklyTc().getMarkedForApproval()) {
								// ensure "isReady" & "markedForApproval" values are current
								refreshStatus(tce, currentApprContact); // update, then re-check
								if (tce.getStatus().isReady() && ! tce.getWeeklyTc().getMarkedForApproval()) {
									tce.getWeeklyTc().setMarkedForApproval(true);
									updated = true;
								}
							}
						}
						else if (tce.getStatus() == ApprovalStatus.APPROVED && getUserHasEditHtg()) {
							if (! tce.getChecked()) {
								tce.setChecked(true);
							}
						}
					}
					if (updated) {
						getWeeklyTimecardDAO().attachDirty(tce.getWeeklyTc());
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
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_APPROVE:
				res = actionApproveOk();
				break;
			case ACT_EXPORT:
				res = actionExportOk();
				break;
			case ACT_FRINGE:
				res = actionPromptFringeOk();
				break;
			case ACT_IMPORT:
				res = actionImportOk();
				break;
			case ACT_LOCK:
				res = actionLockOk();
				break;
			case ACT_PRINT:
				res = actionPrintOk();
				break;
			case ACT_REFRESH:
				res = actionRefreshOk();
				break;
			case ACT_REJECT:
				res = actionRejectOk();
				break;
			case ACT_HTG:
				res = actionHtgOk();
				break;
			case ACT_ONE_HTG:
				res = actionCalculateOneHtgOk();
				break;
			case ChangePinBean.ACT_PROMPT_PIN:
				showChangePin = false;
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/**
	 * Called when the user Cancels one of our pop-up dialogs.
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ChangePinBean.ACT_PROMPT_PIN:
				showChangePin = false;
				break;
			case ACT_REJECT:
				actionRejectCancel();
				break;
			case ACT_LOCK:
			case ACT_PRINT:
			case ACT_HTG:
			case ACT_ONE_HTG:
				addClientResizeScroll();
				break;
		}
		return res;
	}

	/**
	 * This method is fired when row is selected and set the bean value depend on the ID selected
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (timecardEntry != null) {
			if(weeklyTimecard != null) {
				weeklyTimecard.setSelected(false);
			}
			if(timecardEntry != null) { // TODO
				timecardEntry.setSelected(false);
			}
		}

		if (id == null) {
			id = findDefaultId();
		}
		weeklyTimecard = null;
		if (id == null) {
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			TimecardEntry tce = findEntry(id);
			if (tce != null) {
				timecardEntry = tce;
				weeklyTimecard = timecardEntry.getWeeklyTc();
			}
			if (weeklyTimecard == null) {
				id = findDefaultId();
				if (id != null) {
					weeklyTimecard = getWeeklyTimecardDAO().findById(id);
				}
			}
		}
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, id);
		setupSelectedItem();
	}

	/**
	 * Get the first default id
	 * @return id
	 */
	@SuppressWarnings("unchecked")
	private Integer findDefaultId() {
		Integer id = null;
		List<TimecardEntry> list = getItemList();
		if (list.size() > 0) {
			timecardEntry = list.get(0);
			id = timecardEntry.getWeeklyTc().getId();
		}
		return id;
	}

	/**
	 * Find the TimecardEntry corresponding to a particular WeeklyTimecard
	 * database id value.
	 * @param id
	 * @return The matching TimecardEntry, or null if not found.
	 */
	private TimecardEntry findEntry(Integer id) {
		TimecardEntry tc = null;
		for (TimecardEntry tce : timecardEntryList) {
			if (id.equals(tce.getWeeklyTc().getId())) {
				tc = tce;
				break;
			}
		}
		return tc;
	}

	/**
	 * Set up to display the first entry in timecardEntryList.
	 */
	private void setupFirst() {
		if (timecardEntryList.size() > 0) {
			timecardEntry = timecardEntryList.get(0);
			weeklyTimecard = timecardEntry.getWeeklyTc();
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		}
		else { // Create empty timecard
			weeklyTimecard = new WeeklyTimecard();
			timecardEntry = new TimecardEntry(weeklyTimecard);
			List<DailyTime> dailyTimes = new ArrayList<>();
			for (byte i=1; i < 8; i++) {
				dailyTimes.add(new DailyTime(weeklyTimecard, i));
			}
			weeklyTimecard.setDailyTimes(dailyTimes); // Add List<DailyTime> to weeklyTimecard
			weeklyTimecard = null;
		}

		setupSelectedItem();
	}

	private void setupSelectedItem() {
		if (weeklyTimecard != null && weeklyTimecard.getId() != null) {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
			weeklyTimecard.setSelected(true);

			if (timecardEntry != null && timecardEntry.getWeeklyTc().getId() == weeklyTimecard.getId()) {
				timecardEntry.setWeeklyTc(weeklyTimecard);
				timecardEntry.setSelected(true);
				RowState state = new RowState();
				state.setSelected(true);
				getStateMap().put(timecardEntry, state);
				setSelectedRow(timecardEntry);;
//				setSelectedRow(timecardEntryList.indexOf(timecardEntry));
			}
			weeklyTimecard.getPayLines().size(); // force lazy init
		}
		chartLabels = null;
		if (showDetail) {
			createDetailData();
		}
		else {
			chartData = null; // force refresh if detail area is displayed
		}
	}

	/**
	 * send a JavaScript command to scroll the list so that the currently
	 * selected timecard is visible.
	 */
	private void scrollToRow() {
		if (getFilterBean().getMiniTab() != FilterBean.TAB_STARTS) {
			TimecardEntry tce = null;
			if (weeklyTimecard != null && weeklyTimecard.getId() != null) {
				tce = findEntry(weeklyTimecard.getId());
			}
			scrollToRow(tce);
		}
	}

	/**
	 * Force particular fields to be loaded so they don't get a
	 * LazyInitializationException, typically when a page gets
	 * rendered.
	 */
	private void forceLazyInit() {
		if (timecardEntryList != null) {
			for (TimecardEntry tce : timecardEntryList) {
				WeeklyTimecard wtc = tce.getWeeklyTc();
				for (PayBreakdown pb : wtc.getPayLines()) {
					// Gross pay page was getting exceptions on WeeklyTimecard.payLines
					pb.getCategory();	// force a reference
				}
			}
			if (timecardEntry != null) {
				weeklyTimecard = timecardEntry.getWeeklyTc();
				weeklyTimecard.setSelected(true);
			}
		}
		if (project != null) {
			project = ProjectDAO.getInstance().refresh(project);
			project.getPayrollPref().getHidePrDiscrepancy();
		}
	}

	/**
	 * Refresh the timecard represented by the given entry, and re-calculate its
	 * status value.
	 *
	 * @param tce The TimecardEntry whose WeeklyTimecard should be refreshed.
	 * @param currentApprContact The current (logged-in) Contact, which is used
	 *            to determine the timecards "relative status" (e.g., waiting
	 *            for this person's approval).
	 */
	private void refreshStatus(TimecardEntry tce, Contact currentApprContact) {
		WeeklyTimecard wtc = tce.getWeeklyTc();
		wtc = getWeeklyTimecardDAO().refresh(wtc);
		tce.setWeeklyTc(wtc);
		TimecardCalc.calculateWeeklyTotals(wtc);
		ApproverUtils.calculateApprovalStatus(tce, currentApprContact);
	}

	/**
	 * Create the fields used by the "timecard comparison" (detail) area
	 * at the bottom of the Timecard Review and Gross Payroll pages.
	 */
	private void createDetailData() {
		if(weeklyTimecard != null) {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			createDayEntry();
			createChartValues();
		}
	}

	/**
	 * Create the dayEntry List, which is used to populate the display of
	 * hours for the current timecard versus the corresponding DPR's hours.
	 */
	private void createDayEntry() {
		deptOnDpr = true;
		enableViewDpr = true;
		if (weeklyTimecard != null && weeklyTimecard.getEndDate() != null) {
			Contact contact = timecardEntry.getContact();
			if (contact != null) { // only null if logout occurred on same JSF cycle. rev 2.2.4904
				contact = ContactDAO.getInstance().refresh(contact);
				timecardEntry.setContact(contact);
			}
			Project tcProject = project;
			if (SessionUtils.getProduction().getType().isAicp()) {
				tcProject = weeklyTimecard.getStartForm().getProject();
				if (getShowAllProjects() && ! tcProject.equals(SessionUtils.getCurrentProject())) {
					enableViewDpr = false;
				}
			}
			List<DailyHours> dprHours = DprDAO.getInstance().findDailyHours(weeklyTimecard.getEndDate(),
					contact, weeklyTimecard.getOccupation(),
					tcProject, weekOfDprs);
			dayEntry = new ArrayList<>();
			int index = 0;
			for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
				DailyHours dh = dprHours.get(index++);
				DayEntry entry = new DayEntry(dt, dh);
				dayEntry.add(entry);
			}
			calculateDprTotals();
			// determine if Department appears on DPR
			deptOnDpr = DepartmentDAO.getInstance().
					findOnDprByProductionStandardId(production, tcProject, weeklyTimecard.getDepartmentId());
		}
		else { // generate enough dummy data to satisfy JSP requirements.
			Date date = getWeekEndDate();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, -6);
			dayEntry = new ArrayList<>();
			DailyTime dt = new DailyTime(null, (byte)0);
			for (int i = 0; i < 7; i++) {
				DailyHours dh = new DailyHours();
				dh.setDate(cal.getTime());
				DayEntry entry = new DayEntry(dt, dh);
				dayEntry.add(entry);
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}
		}
	}

	/** Create values to be displayed on the y axis of the hours chart */
	private List<Axis> createYAxis() {
		List<Axis> axes = new ArrayList<>();

		Axis axis = new Axis();
		axis.setAutoscale(true);
		axis.setDrawMajorGridlines(false);
		axis.setMin(0);
		axis.setMax(20);
		axis.setTickInterval("5");
		axis.setFormatString("%d");
		axis.setType(AxisType.LINEAR);

		axes.add(axis);

		return axes;
	}

	/** Create values to be displayed on the x axis of the hours chart */
	private Axis createXAxis() {
		Axis axis = new Axis();
		axis.setAutoscale(true);
		axis.setType(AxisType.CATEGORY);
		axis.setDrawMajorGridlines(false);

		return axis;
	}

	private void calculateDprTotals() {
		BigDecimal totalHours = BigDecimal.ZERO;
		short totalMpv1 = 0;
		short totalMpv2 = 0;
		if (dayEntry != null) {
			for (DayEntry entry : dayEntry) {
				DailyHours dh = entry.getDailyHours();
				totalHours = NumberUtils.safeAdd(totalHours, dh.getHours());
				if (dh.getMpv1() != null) {
					totalMpv1 += dh.getMpv1();
				}
				if (dh.getMpv2() != null) {
					totalMpv2 += dh.getMpv2();
				}
			}
		}
		weeklyTimecard.setTotalDprHours(totalHours);
		weeklyTimecard.setTotalDprMpv1(totalMpv1);
		weeklyTimecard.setTotalDprMpv2(totalMpv2);
	}

	/**
	 * Set bar-chart values when a timecard is selected
	 */
	private void createChartValues() {
		createChartLabels();

		chartData = new ArrayList<>();
		// List of Employee hours for each day of the week.
		List<Double> empHours = new ArrayList<>(0);
		List<List<Double>> newData = new ArrayList<>(3);
		// List of average hours for entire crew for each day of the week.
		List<Double> listCrew = new ArrayList<>(0);
		// List of average hours for department members for each day of the week.
		List<Double> listDept = new ArrayList<>(0);

		if (weeklyTimecard != null) {
			Date date = weeklyTimecard.getEndDate();
			// Get Crew average times
			listCrew = getWeeklyTimecardDAO().findAverageHours(production, project, date);
			newData.add(listCrew);
			// Get Dept average times
			listDept = getWeeklyTimecardDAO().findAverageHoursDept(production, project, date,
					weeklyTimecard.getDeptName());
			newData.add(listDept);

			// Populate the Daily Time hours
			for (int i = 0; i < 7; i++) {
				BigDecimal hrs = weeklyTimecard.getDailyTimes().get(i).getHours();
				double dHrs = 0;
				if (hrs != null) {
					dHrs = hrs.doubleValue();
				}
				empHours.add(dHrs);
			}
			newData.add(empHours);
		}
		else {
			double dHrs = 0;
			for (int i = 0; i < 7; i++) {
				empHours.add(dHrs);
				listCrew.add(dHrs);
				listDept.add(dHrs);
			}
			newData.add(listCrew);
			newData.add(listDept);
			newData.add(empHours);
		}

		// Create chart services for each of the hour collections.
		// List Crew, ListDept, and EmpHours
		for(int i = 0; i < 3; i++) {
			List<Double>hours = newData.get(i);
			CartesianSeries data = new CartesianSeries();
			for(int j = 0; j < 7; j++) {
				data.add(chartLabels.get(j), hours.get(j));
			}

			chartData.add(data);
		}
	}

	/**
	 * @return The number of unapproved timecards with a week-ending date
	 *         earlier than the default week-ending date.
	 */
	private Integer calculateUnapprovedTcs() {
		Integer tcCount = 0;
		Date weDate = TimecardUtils.calculateDefaultWeekEndDate();
		String filter = ApprovableStatusFilter.SUBMITTED.sqlFilter();
		setvContact(ContactDAO.getInstance().refresh(getvContact()));
		if (getUserHasViewHtg() || ApproverUtils.isProdOrProjectApprover(getvContact(), project)) {
			tcCount = getWeeklyTimecardDAO().findCountByWeekEndDateDeptSubmitted(production, project, weDate, null, filter);
		}
		else { // Department approver
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(getvContact(), project);
			// Then count all unapproved timecards for those departments
			for (Department dept : depts) {
				tcCount += getWeeklyTimecardDAO().findCountByWeekEndDateDeptSubmitted(production, project, weDate, dept.getName(), filter);
			}
		}
		return tcCount;
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
	@Override
	protected List<SelectItem> createBatchList(Date weekEndDate) {
		if (getShowAllProjects()) {
			return WeeklyBatchUtils.createBatchList(production, null, weekEndDate);
		}
		else {
			return WeeklyBatchUtils.createBatchList(production, project, weekEndDate);
		}
	}

	/**
	 * Create list of time cards available to this person, based on the current values
	 * of weekEndDate, departmentId, and batchId.
	 */
	@Override
	protected void createTimecardList() {
		masterTriState.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		setSelectedRow(-1); // prevent sort from trying to maintain this
		timecardEntryList = TimecardUtils.createTimecardList(this, getFilterBean(), true);
		if (getShowAllProjects()) {
			hideDailyHours();
		}
		scrollToRow(); // reset selected row
		if (timecardEntry != null) { // need to update it from the new list
			if (getSelectedRow() >= 0) {
				// get the new timecardEntry (since the list was rebuilt)
				timecardEntry = timecardEntryList.get(getSelectedRow());
			}
			else if (timecardEntryList.size() > 0) {
				timecardEntry = timecardEntryList.get(0);
			}
			// and update timecard from the new timecardEntry
			weeklyTimecard = timecardEntry.getWeeklyTc();
		}
//		if (getFilterBean().getMiniTab() == FilterBean.TAB_GROSS ||
//				getFilterBean().getMiniTab() == FilterBean.TAB_REVIEW) {
			forceLazyInit();
//		}
	}

	/**
	 * Set the TimecardEntry flag to hide the daily hours columns for those
	 * timecards whose week-ending day is different than the current Project's
	 * value. This process is only necessary if the user has selected the "show
	 * all projects" option.
	 */
	private void hideDailyHours() {
		for (TimecardEntry tce : timecardEntryList) {
			WeeklyTimecard wtc = tce.getWeeklyTc();
			tce.setShowDailyHours(wtc.getWeekEndDay() == weekEndDay);
		}
	}

	@Override
	public void sortTimecards(List<TimecardEntry> tceList) {
		timecardEntryList = tceList;
		sort();
	}

	/**
	 * Create the drop-down list of W/E dates, including "All" and "Prior"
	 * choices.
	 * @see com.lightspeedeps.web.approver.ApproverBase#createEndDateList(java.util.Date, boolean)
	 */
	@Override
	protected List<SelectItem> createEndDateList(Date listDate, boolean allProjects) {
		List<SelectItem> dateList = super.createEndDateList(listDate, allProjects);
		dateList.add(0, new SelectItem(Constants.SELECT_ALL_DATE, "All"));
		dateList.add(1, new SelectItem(Constants.SELECT_PRIOR_DATE, "Prior"));
		return dateList;
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
	@Override
	protected List<SelectItem> createDepartmentList() {

		ArrayList<SelectItem> deptList;
		if (getUserHasViewHtg() || ApproverUtils.isProdOrProjectApprover(getvContact(), project)) {
			deptList = new ArrayList<>(DepartmentUtils.getDepartmentDataAdminDL(
					getShowAllProjects() ? null : project, true));
		}
		else {
			// Find depts with this user as approver
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(getvContact(), project);
			deptList = new ArrayList<>();
			for (Department dept : depts) {
				deptList.add(new SelectItem(dept.getId(), dept.getName()));
			}
		}
		//if (departmentList.size() > 1) { // may have out-of-line approvals, allow other depts this way.
			deptList.add(0, new SelectItem(0, "All"));
		//}
		return deptList;
	}

	/**
	 * Fill the {@link #timecardDays} array with the appropriate day names, based on the
	 * current Production's & Project's week-ending day preference.
	 */
	private void createChartLabels() {
		int day;
		if (weeklyTimecard != null) {
			day = weeklyTimecard.getWeekEndDay();
		}
		else {
			day = TimecardUtils.findWeekEndDay();
		}
		chartLabels = Arrays.asList(TimecardUtils.createTimecardDayLabels(day));
	}

	/**
	 * Clear our stash of the current week's DPRs, so the next attempt to use
	 * them (for discrepancy data) will cause them to be refreshed from the
	 * database.
	 */
	private void clearDprs() {
		for (int i=0; i < weekOfDprs.length; i++) {
			weekOfDprs[i] = null;
		}
	}

	/** See {@link #dprDate}. */
	public Date getDprDate() {
		return dprDate;
	}
	/** See {@link #dprDate}. */
	public void setDprDate(Date dprDate) {
		this.dprDate = dprDate;
	}

	public List<String> getLabels() {
		return WEEKDAY_LABELS;
	}

	/** See {@link #chartLabels}. */
	public List<String> getChartLabels() {
		if (chartLabels == null) {
			createChartLabels();
		}
		return chartLabels;
	}

	public List<String> getLegendLabels() {
		return legendLabels;
	}

	public List<CartesianSeries> getChartData() {
		if (chartData == null) {
			createDetailData();
		}
		return chartData;
	}
	public void setChartData(List<CartesianSeries> chartData) {
		this.chartData = chartData;
	}

	public String [] getPaints() {
		return paints;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public String getTitleX() {
		return titleX;
	}
	public void setTitleX(String titleX) {
		this.titleX = titleX;
	}

	public String getTitleY() {
		return titleY;
	}
	public void setTitleY(String titleY) {
		this.titleY = titleY;
	}

	public boolean getHorizontal() {
		return horizontal;
	}
	/**
	 * Sets the horizontal, true the chart will be displayed in a
	 * horizontal orientation, false the chart will be in a vertical
	 * orientation.
	 *
	 * @param horizontal true for horizontal, false for vertical.
	 */
	public void setHorizontal(boolean horizontal) {
		this.horizontal = horizontal;
	}

	public DayType getDayTypeWorked() {
		return DayType.WK;
	}

	/** See {@link #masterTriState}. */
	public TriStateCheckboxExt getMasterTriState() {
		return masterTriState;
	}
	/** See {@link #masterTriState}. */
	public void setMasterTriState(TriStateCheckboxExt masterTriState) {
		this.masterTriState = masterTriState;
	}

	/**See {@link #selectedTimecardId}. */
	public Integer getSelectedTimecardId() {
		return selectedTimecardId;
	}
	/**See {@link #selectedTimecardId}. */
	public void setSelectedTimecardId(Integer selectedTimecardId) {
		this.selectedTimecardId = selectedTimecardId;
	}

	/** See {@link #priorUnapprovedTcs}. */
	public Integer getPriorUnapprovedTcs() {
		if (priorUnapprovedTcs == null) {
			priorUnapprovedTcs = calculateUnapprovedTcs();
		}
		return priorUnapprovedTcs;
	}
	/** See {@link #priorUnapprovedTcs}. */
	public void setPriorUnapprovedTcs(Integer priorUnapprovedTcs) {
		this.priorUnapprovedTcs = priorUnapprovedTcs;
	}

	/** See {@link #dayEntry}. */
	public List<DayEntry> getDayEntry() {
		return dayEntry;
	}

	/** See {@link #timecardDays}. */
	public String[] getTimecardDays() {
		if (timecardDays == null) {
			timecardDays = TimecardUtils.createTimecardDayLabels();
		}
		return timecardDays;
	}

	/** See {@link #timecardDays}. */
	public void setTimecardDays(String[] timecardDays) {
		this.timecardDays = timecardDays;
	}

	/**See {@link #deptOnDpr}. */
	public boolean getDeptOnDpr() {
		return deptOnDpr;
	}
	/**See {@link #deptOnDpr}. */
	public void setDeptOnDpr(boolean deptOnDpr) {
		this.deptOnDpr = deptOnDpr;
	}

	/**See {@link #deptPrMap}. */
	public Map<String, Boolean> getDeptPrMap() {
		return deptPrMap;
	}

	/**See {@link #weekOfDprs}. */
	public Dpr[] getWeekOfDprs() {
		return weekOfDprs;
	}

	/** See {@link #enableViewDpr}. */
	public boolean getEnableViewDpr() {
		return enableViewDpr;
	}
	/** See {@link #enableViewDpr}. */
	public void setEnableViewDpr(boolean enableViewDpr) {
		this.enableViewDpr = enableViewDpr;
	}

	/**See {@link #showDetail}. */
	public boolean getShowDetail() {
		return showDetail;
	}
	/**See {@link #showDetail}. */
	public void setShowDetail(boolean showDetail) {
		this.showDetail = showDetail;
	}

	/** See {@link #dayEntry}. */
	public void setDayEntry(List<DayEntry> dayEntry) {
		this.dayEntry = dayEntry;
	}

	/** See {@link #showChangePin}. */
	public boolean getShowChangePin() {
		return showChangePin;
	}
	/** See {@link #showChangePin}. */
	public void setShowChangePin(boolean showChangePin) {
		this.showChangePin = showChangePin;
	}

	/** See {@link #showReject}. */
	public boolean getShowReject() {
		return showReject;
	}
	/** See {@link #showReject}. */
	public void setShowReject(boolean showReject) {
		this.showReject = showReject;
	}

	/** See {@link #dprPopup}. */
	public boolean getDprPopup() {
		return dprPopup;
	}
	/** See {@link #dprPopup}. */
	public void setDprPopup(boolean dprPopup) {
		this.dprPopup = dprPopup;
	}

	/** See {@link #popupDprDayList}. */
	public List<SelectItem> getPopupDprDayList() {
		return popupDprDayList;
	}
	/** See {@link #popupDprDayList}. */
	public void setPopupDprDayList(List<SelectItem> popupDprDayList) {
		this.popupDprDayList = popupDprDayList;
	}

	/** See {@link #pAGINATE_SIZE}. */
	public int getPaginateSize() {
		return PAGINATE_SIZE;
	}
	/** See {@link #pAGINATE_SIZE}. */
	public void setPaginateSize(int pAGINATE_SIZE) {
		// setter is just here to satisfy JSP; not used.
	}

	/** See {@link #yAxis}. */
	public Axis[] getyAxis() {
		if(yAxis == null) {
			yAxis = createYAxis();
		}
		return (Axis[])yAxis.toArray();
	}

	/** See {@link #xAxis}. */
	public Axis getxAxis() {
		if(xAxis == null) {
			xAxis = createXAxis();
		}
		return xAxis;
	}

	/** See {@link #configSeries}. */
	public CartesianSeries getConfigSeries() {
		if(configSeries == null) {
			configSeries = new CartesianSeries();
			configSeries.setType(CartesianType.BAR);
			configSeries.setBarWidth(10);
			configSeries.setBarPadding(1);
			configSeries.setShadow(false);
			configSeries.setSmooth(true);
		}
		return configSeries;
	}

}
