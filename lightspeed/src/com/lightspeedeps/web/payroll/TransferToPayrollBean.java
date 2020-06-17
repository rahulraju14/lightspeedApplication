package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.WeeklyBatchDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.ControlHolder;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.object.TriStateCheckboxExt;
import com.lightspeedeps.service.WeeklyBatchService;
import com.lightspeedeps.type.ExportType;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.type.ServiceMethod;
import com.lightspeedeps.type.TimecardSelectionType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardPrintUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.payroll.WeeklyBatchUtils;
import com.lightspeedeps.web.approver.ApproverFilterBase;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.approver.FilterBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupInputBean;
import com.lightspeedeps.web.popup.PopupSelectBean;
import com.lightspeedeps.web.timecard.PrintTimecardBean;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.SelectableList;
import com.lightspeedeps.web.view.SelectableListHolder;
import com.lightspeedeps.web.view.SortableList;

/**
 * Backing bean for the Transfer to Payroll page.
 * <p>
 * Note that the list of timecards displayed on the page is owned and partially managed
 * by our superclass(es).
 */
@ManagedBean
@ViewScoped
public class TransferToPayrollBean extends ApproverFilterBase
		implements ControlHolder,  Serializable, SelectableListHolder {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(TransferToPayrollBean.class);

	private static final int ACT_TRANSMIT_BATCH = 11;
	private static final int ACT_TRANSMIT_TIMECARDS = 12;
	private static final int ACT_NOTIFY_INCOMPLETE = 13;
	private static final int ACT_PRINT = 16;
	private static final int ACT_NEW_BATCH = 17;
	private static final int ACT_RENAME_BATCH = 18;
	private static final int ACT_REASSIGN_TC = 19;
	private static final int ACT_REASSIGN_TC_ALL = 20;

	/** The WeeklyTimecard.id value of the icon clicked on by the user. */
	private Integer selectedTimecardId;

	/** The currently selected WeeklyBatch entry in our list. */
	private WeeklyBatch weeklyBatch;

	/** A transient (i.e., not persisted) WeeklyBatch that holds all the "unbatched" timecards. */
	private WeeklyBatch unBatched;

	/** The List of batches being displayed. */
	private final List<WeeklyBatch> batchList = new ArrayList<>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	private final SelectableList sortedBatchList = new SelectableList(this, batchList, WeeklyBatch.SORTKEY_NAME, true);

	/** The backing field for the checkbox in the batch header area, which acts
	 * as a "master" select/unselect for all enabled checkboxes in the
	 * list of weekly batches. */
	private TriStateCheckboxExt batchMasterCheck = new TriStateCheckboxExt();

	/** The backing field for the checkbox in the timecard header area, which
	 * acts as a "master" select/unselect for all enabled checkboxes in the
	 * current timecard list. */
	private TriStateCheckboxExt tcMasterCheck = new TriStateCheckboxExt();

	/** The PayrollService assigned to the current production, if any. */
	private PayrollService payrollService = null;

	/** True iff the current production is a Team payroll service client.  Based
	 * on flag in the PayrollService object. */
	private boolean isTeamPayroll;

	/** The current PayrollPreference. This is related either to the Production, for
	 * TV/Feature, or to the Project for Commercial productions. */
	private PayrollPreference payrollPref;

	/** The Worker's Comp setting of all the currently selected timecards.  This will be
	 * null if none are selected, or if we are not checking for "consistency" between
	 * timecards. (Consistency check is only done for Commercial productions that are
	 * Team payroll clients.) */
	private Boolean selectedWorkersComp = null;

	/** The bean used to prompt the user for transmit options (if any). */
	private EmailBatchPopupBean transmitBean;

	private String employeeAccount = Constants.CATEGORY_ALL;

	/** For managing sort of the WeeklyBatch list: current sort column */
	private String batchCompareColumn;
	/** For managing sort of the WeeklyBatch list: current sort direction */
	private boolean batchAscending;

	private transient WeeklyBatchDAO weeklyBatchDAO;

	/** The job name  in weekly batch. */
	private String jobName;

	/** The job code in weekly batch. */
	private String jobCode;

	/** The G/L Account Code in weekly batch. */
	private String glAccountCode;

	/** The Cost Center in weekly batch. */
	private String costCenter;

	/** The Comments in weekly batch. */
	private String comments;

	/**
	 * default constructor
	 */
	public TransferToPayrollBean() {
		super(WeeklyBatch.SORTKEY_NAME);

		log.debug("Init");
		setSortAttributePrefix("transferToPayroll.");

		rowHeight = 24; // set our table row display height - not the usual 26px.

		getFilterBean().register(this, FilterBean.TAB_TRANSFER);

		batchMasterCheck.setHolder(this);
		batchMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		batchMasterCheck.setId(batchMasterCheck);

		tcMasterCheck.setHolder(this);
		tcMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		tcMasterCheck.setId(tcMasterCheck);

		getPayrollService();
		if (payrollService != null) {
			isTeamPayroll = payrollService.getTeamPayroll();
		}

		if (getProduction().getPayrollPref().getIncludeTouring()) {
			// LS-1741 - 'hybrid' productions can't use "show all projects"
			setShowAllProjects(false);
		}

		createBatchList();

		// Get saved values from last time page was displayed
		setDepartmentId(SessionUtils.getInteger(Constants.ATTR_APPROVER_DEPT));

		employeeAccount = SessionUtils.getString(Constants.ATTR_APPROVER_EMPLOYEE);
		if (employeeAccount == null) {
			employeeAccount = Constants.CATEGORY_ALL;
		}

		createTimecardList();
		setupFirst();
		restoreSortOrder();
	}

	/**
	 * Action method for the "New" button in the Batch section of the
	 * Transfer to Payroll page.
	 *
	 * @return null navigation string
	 */
	public String actionAddBatch() {
		if (getWeekEndDate().equals(Constants.SELECT_ALL_DATE)) {
			MsgUtils.addFacesMessage("WeeklyBatch.AllSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		PopupInputBean inputBean = PopupInputBean.getInstance();
		inputBean.show( this, ACT_NEW_BATCH, "WeeklyBatch.New.");
		inputBean.setMaxLength(WeeklyBatch.MAX_NAME_LENGTH);
		Production prod = SessionUtils.getProduction();
		if (prod.getPayrollPref().getIncludeWeSuffix()) {
			String prompt;
			String strDate = "";
			Date date = getWeekEndDate();
			if (weeklyBatch.getEndDate() != null) {
				date = weeklyBatch.getEndDate();
			}
			SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
			strDate = sdf.format(date);
			if (prod.getPayrollPref().getUseWeAsPrefix()) {
				prompt = "WE" + strDate + "-";
			}
			else {
				prompt = "-WE" + strDate;
			}
			inputBean.setInput(prompt);
		}
		return null;
	}

	/**
	 * Action method for the "Rename" button in the Batch section of the
	 * Transfer to Payroll page.
	 *
	 * @return null navigation string
	 */
	public String actionRenameBatch() {
		WeeklyBatch wkbatch = null;
		int n = 0;
		for (WeeklyBatch wb : batchList) {
			if (wb.getChecked()) {
				n++;
				wkbatch = wb;
			}
		}
		if (n == 0) {
			MsgUtils.addFacesMessage("WeeklyBatch.NoneSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (n != 1 ) {
			MsgUtils.addFacesMessage("WeeklyBatch.Rename.TooManySelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (wkbatch != null) {
			if (wkbatch.getSent() != null) {
				MsgUtils.addFacesMessage("WeeklyBatch.Rename.BatchAlreadySent", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			PopupInputBean inputBean = PopupInputBean.getInstance();
			inputBean.show( this, ACT_RENAME_BATCH, "WeeklyBatch.Rename.");
			inputBean.setInput(wkbatch.getName());
			inputBean.setMaxLength(WeeklyBatch.MAX_NAME_LENGTH);
		}
		return null;
	}

	/**
	 * Action method for the "Delete" button in the Batch section of the
	 * Transfer to Payroll page.
	 *
	 * @return null navigation string
	 */
	public String actionDeleteBatch() {
		int n = getSelectedBatchCount();
		if (n == 0 ) {
			MsgUtils.addFacesMessage("Timecard.NoneSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		for (WeeklyBatch wb : batchList) {
			if (wb.getChecked() && wb.getSent() != null) {
				MsgUtils.addFacesMessage("WeeklyBatch.Delete.BatchAlreadySent", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		PopupBean.getInstance().show( this, ACT_DELETE_ITEM, "WeeklyBatch.Delete.");
		return null;
	}

	/**
	 * Action method for the "check payroll status" button on the Batch list in
	 * the dashboard.
	 *
	 * @return null navigation string
	 */
	public String actionCheckBatchStatus() {
		try {
			List<WeeklyBatch> batches = new ArrayList<>();
			getPayrollService(); // refresh it
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					batches.add(wb);
				}
			}
			if (batches.size() > 0) {
				if (WeeklyBatchService.updateStatusFromPayroll(batches)) {
					MsgUtils.addFacesMessage("WeeklyBatch.Status.Ok", FacesMessage.SEVERITY_INFO);
				}
				else {
					// a message was already issued.
				}
				createBothLists();
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.Status.NoBatches", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the "check payroll status" button on the Batch list in
	 * the dashboard.
	 *
	 * @return null navigation string
	 */
	public String actionCheckTimecardStatus() {
		try {
			List<WeeklyBatch> batches = new ArrayList<>();
			if (! isCurrentUnbatched()) {
				batches.add(weeklyBatch);
				if (WeeklyBatchService.updateStatusFromPayroll(batches)) {
					MsgUtils.addFacesMessage("WeeklyBatch.StatusOneBatch.Ok",
							FacesMessage.SEVERITY_INFO, weeklyBatch.getName());
				}
				else {
					// a message was already issued.
				}
				createBothLists();
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action Method for the Print button on Approver Dashboard for Batch
	 * Transfer Mini tab. This displays the Print Options pop up dialog box.
	 * Control will be returned via either confirmOk() or confirmCancel().
	 *
	 * @return null navigation string
	 */
	public String actionPrint() {
		try {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			if (weeklyTimecard != null) {
				TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
			}
			TimecardPrintUtils.showPrintOptions(weeklyTimecard, getWeekEndDate(), getFilterBean()
					.getWeekEndDate(), true, false, this, ACT_PRINT, true, getShowAllProjects(), false);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "print" button on the Print dialog, invoked
	 * via our confirmOk method.
	 *
	 * @return null navigation string
	 */
	private String actionPrintOk() {
		try {
			WeeklyTimecard wtc = weeklyTimecard;
			String result = null;
			PrintTimecardBean bean = PrintTimecardBean.getInstance();

			Project printProject = project;
			if (getShowAllProjects() || ! SessionUtils.getProduction().getType().isAicp()) {
				printProject = null;
			}

			if (wtc == null && bean.getReportType().equals(PrintTimecardBean.TYPE_TIMECARD)) {
				// Find a "default timecard" to pass to the print routine
				if (bean.getRangeSelectionType() == TimecardSelectionType.ALL) {
					List<WeeklyTimecard> list = getWeeklyTimecardDAO().findByWeekEndDate(production, printProject, bean.getWeekEndDate(), null);
					if (list.size() > 0) {
						wtc = list.get(0);
					}
				}
				if (wtc == null) {
					MsgUtils.addFacesMessage("WeeklyBatch.Print.NoTimeCards",
							FacesMessage.SEVERITY_ERROR);
				}
			}

			if (wtc != null || ! bean.getReportType().equals(PrintTimecardBean.TYPE_TIMECARD)) {
				boolean skipAttachments = ! bean.getIncludeAttachments();
				result = TimecardPrintUtils.printSelectedReport(wtc, printProject, (printProject==null), skipAttachments, null);
				if (result == null) { // Usually means the report generation threw an exception
					MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
				}
			}

		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Reassign (move) button. Presents a pop-up allowing
	 * the user to select which batch the checked timecards will be moved to.
	 *
	 * @return null navigation string
	 */
	public String actionReassign() {
		int items = 0;
		boolean sent = false;
		try {
			selectedWorkersComp = null;
			if (batchList.size() <= 1) {
				MsgUtils.addFacesMessage("WeeklyBatch.NoBatchesAvailable", FacesMessage.SEVERITY_ERROR);
				return null;
			}
//			if (getWeekEndDate().equals(Constants.SELECT_ALL_DATE)) {
//				PopupBean.getInstance().show(null, 0,
//						"WeeklyBatch.NoReassignAll.Title", "WeeklyBatch.NoReassignAll.Text",
//						"Confirm.Close", null);
//				return null;
//			}
			boolean wcError = false;
			boolean checkWc = false;
			if ((production.getType().isAicp() && isTeamPayroll) &&
					(getShowAllProjects() || weeklyBatch.getAggregate())) {
				// This is a Team commercial client, and
				// user could (possibly) select timecards from multiple projects,
				// or from a project other than the current batch's project.
				checkWc = true;  // so check Worker's Comp consistency below
			}

			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked()) {
					if (tce.getWeeklyTc().getTimeSent() != null && ! isCurrentUnbatched()) {
						// LS-1334 restore check for re-assigning already-transferred TC
						if (! sent) {
							MsgUtils.addFacesMessage("Timecard.SentSelected", FacesMessage.SEVERITY_ERROR);
						}
						sent = true;
					}
					else if (checkWc) { // do Worker's Comp consistency check
						Project tcProj = tce.getWeeklyTc().getStartForm().getProject();
						tcProj = ProjectDAO.getInstance().refresh(tcProj);
						boolean tcWc;
						if (tcProj.equals(getProject())) {
							tcWc = getPayrollPref().getWorkersComp();
						}
						else {
							tcWc = tcProj.getPayrollPref().getWorkersComp();
						}
						if (selectedWorkersComp == null) { // first selected TC
							selectedWorkersComp = tcWc;
						}
						else if (tcWc != selectedWorkersComp) {
							// Not allowed - mix of WC & non-WC timecards (projects) selected
							wcError = true;
						}
					}
					items++;
				}
			}
			if (items == 0 ) {
				MsgUtils.addFacesMessage("Timecard.NoneSelected", FacesMessage.SEVERITY_ERROR);
				return null;
			}

			if (sent) {
				// skip pop-up, error message was issued; no re-assign allowed. LS-1334
			}
			else if (wcError) {
				MsgUtils.addFacesMessage("Timecard.SelectedMixedWc", FacesMessage.SEVERITY_ERROR);
			}
			else if ((getWeekEndDate().equals(Constants.SELECT_ALL_DATE) ||
					getWeekEndDate().equals(Constants.SELECT_PRIOR_DATE))) {
				ReassignPopupBean selectBean = ReassignPopupBean.getInstance();
				selectBean.show(this, ACT_REASSIGN_TC_ALL, "WeeklyBatch.Reassign.");
				selectBean.setBatchList(batchList);
				selectBean.setFromBatch(weeklyBatch);
				List<SelectItem> weekEndingList = super.createEndDateList(null, false);
				selectBean.setWeekEndingList(weekEndingList);
				selectBean.show();
			}
			else {
				PopupSelectBean selectBean = PopupSelectBean.getInstance();
				selectBean.show( this, ACT_REASSIGN_TC, "WeeklyBatch.Reassign.");
				List<SelectItem> selectBatchList = createBatchSelectList(weeklyBatch, sent);
				selectBean.setSelectList(selectBatchList );
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method to transmit one or more selected Timecards to the payroll
	 * service.
	 *
	 * @return empty navigation string
	 */
	public String actionTransmitTimecards() {
		try {
			if (! isCurrentUnbatched()) {
				int numUncalcdTimecards = 0;
				int n = getSelectedTimecardCount();
				if (n == 0 ) {
					MsgUtils.addFacesMessage("Timecard.NoneSelected", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				getPayrollService(); // refresh value
				n = countReadyTimecards(payrollService.getSendOnlyChanges());
				if (n == 0 ) {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoChanges", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				String name;
				if ((name = checkTimecards()) != null) {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.BadTimecard", FacesMessage.SEVERITY_ERROR,
							weeklyBatch.getName(), name);
					return null;
				}

				if (isTeamPayroll) {
					// Count the number of uncalculated timecards.
					// Currently be done for TEAM clients
					numUncalcdTimecards = countUncalcdTimecards(payrollService.getSendOnlyChanges());
				}

				n -= numUncalcdTimecards;
				showTransmitPopup(1, n, numUncalcdTimecards,  "", ACT_TRANSMIT_TIMECARDS);
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.NoTransmitUnbatched", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method to transmit one or more selected batches to the payroll
	 * service.
	 *
	 * @return empty navigation string
	 */
	public String actionTransmitBatch() {
		try {
			getPayrollService(); // refresh value
			if (! getPayrollPref().getUseEmail()) { // E-mail preference is not set...
				if (getPayrollService().getSendBatchMethod().hasNoDataFile() || isTeamPayroll) {
					// if there's no data file,
					// or if it is a TEAM payroll service, we must have email set...
					MsgUtils.addFacesMessage("WeeklyBatch.SetEmailPreference", FacesMessage.SEVERITY_ERROR);
					return null;
				}
			}
			// First make sure we have something to send
			boolean checked = false;
			int batchCount = 0;
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					wb = getWeeklyBatchDAO().refresh(wb);
					wb.setChecked(true);
					checked = true; // at least one batch selected
					if (countReadyTimecards(wb, payrollService.getSendOnlyChanges()) > 0) {
						batchCount++;
					}
				}
			}
			if (! checked) {
				MsgUtils.addFacesMessage("WeeklyBatch.NoneSelected", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (batchCount == 0) {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoChanges", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			else if (isTeamPayroll && SessionUtils.getProduction().getBatchTransferExtraField() && batchCount > 1 ) {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.MultipleBatches", FacesMessage.SEVERITY_ERROR);
				return null;
			}

			// Check for non-transmittable timecards and notify user if found
			batchCount = 0;
			int totalIncomplete = 0;
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					int numIncomplete = countNotReadyTimecards(wb);
					if (numIncomplete > 0) {
						totalIncomplete += numIncomplete;
						batchCount++;
					}
				}
			}
			if (totalIncomplete > 0) {
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_NOTIFY_INCOMPLETE, "WeeklyBatch.Transmit.NotReady.");
				String msg = MsgUtils.formatMessage("WeeklyBatch.Transmit.NotReady.Text", totalIncomplete, batchCount);
				bean.setMessage(msg);
			}
			else {
				actionTransmitBatch2();
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}

		return null;
	}

	/**
	 * Transmit request by user passed initial requirement checks. Proceed with
	 * transmit preparation and put up confirmation dialog. Control will be
	 * returned via our {@link #confirmOk(int)} or {@link #confirmCancel(int)} methods.
	 *
	 * @return null navigation string
	 */
	private String actionTransmitBatch2() {
		try {
			getPayrollService(); // refresh value
			int batchCount = 0;
			int totalToSend = 0;
			int totalUncalcdTimecards = 0;

			String batchMessages = "";
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					wb = getWeeklyBatchDAO().refresh(wb);
					wb.setChecked(true);
					int numToSend = countReadyTimecards(wb, payrollService.getSendOnlyChanges());
					int numUncalcdTimecards = 0;
					payrollService.getBatchEmailAddress();
					if (isTeamPayroll) {
						numUncalcdTimecards = countUncalcdTimecards(wb,  payrollService.getSendOnlyChanges());
					}
					if (numToSend == 0) {
						batchMessages += ", " + wb.getName();
					}
					else {
						batchCount++;
						totalToSend += numToSend - numUncalcdTimecards;
						totalUncalcdTimecards += numUncalcdTimecards;
					}
				}
			}
			showTransmitPopup(batchCount, totalToSend, totalUncalcdTimecards, batchMessages, ACT_TRANSMIT_BATCH);
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * @param batchCount
	 * @param totalToSend
	 * @param totalUncalcdTimecards
	 * @param batchMessages
	 * @param action
	 */
	private void showTransmitPopup(int batchCount, int totalToSend, int totalUncalcdTimecards, String batchMessages, int action) {
		String msgPrefix = "Send.Dialog.";
		String warningMsgPrefix = "Send.Dialog.";
		String warningMessage = "";
		boolean showWarningMessage = false;
		boolean email = false;

		switch (getPayrollService().getSendBatchMethod()) {
			case AUTH_POST:
				break;
			case TEAM_FTP:
				email = true; // 10/6/16 force email'd PDF, plus FTP for now
				break;
			case CREW_CARDS:
			case ABS_FILE:
			case CSV_FILE:
			case PDF_ONLY:
			case TEAM_FILE:
			case TEAM_PDF:
				if (getPayrollPref().getUseEmail()) {
					email = true;
					msgPrefix = "Email.Dialog.";
					warningMsgPrefix = "Email.Dialog.";
				}
				else {
					msgPrefix = "File.Dialog.";
					warningMsgPrefix = "File.Dialog.";
				}
				break;
		}

		if (batchMessages.length() > 2) {
			batchMessages = "<br/>These batches have no timecards ready to send and will be skipped: " +
					batchMessages.substring(2);
		}

		String msg;
		if (action == ACT_TRANSMIT_BATCH) {
			msgPrefix = "WeeklyBatch.Transmit." + msgPrefix;
			msg = MsgUtils.formatMessage(msgPrefix + "Text", batchCount, totalToSend, batchMessages);
		}
		else {
			msgPrefix = "WeeklyBatch.TransmitTC." + msgPrefix;
			msg = MsgUtils.formatMessage(msgPrefix + "Text", totalToSend);
		}

		PopupBean bean;
		if (email) {
			transmitBean = EmailBatchPopupBean.getInstance();
			bean = transmitBean;
		}
		else {
			transmitBean = null;
			bean = PopupBean.getInstance();
		}

		// For TEAM clients blank and uncalculated timecards will not be transferred.
		// A warning message will be shown in the transfer popup.
		if (isTeamPayroll) {
			if (totalUncalcdTimecards > 0) {
				showWarningMessage = true;
				if (action == ACT_TRANSMIT_BATCH) {
					warningMsgPrefix = "WeeklyBatch.Transmit." + warningMsgPrefix;
				}
				else {
					warningMsgPrefix = "WeeklyBatch.TransmitTC." + warningMsgPrefix;
				}
				if (totalUncalcdTimecards > 0) {
					warningMessage += MsgUtils.getMessage(warningMsgPrefix + "UncalcdTimecards");
				}
				// Put up confirm popup if there are no items to send
				if (totalToSend < 1) {
					PopupBean warningPopup = PopupBean.getInstance();
					warningPopup.setShowWarningMessage(true);
					warningPopup.setWarningMessage(warningMessage);
					warningPopup.setMessage(MsgUtils.getMessage(msgPrefix + "CannotTransmit"));
					warningPopup.show(this, 0, msgPrefix + "Title", "Confirm.OK", null);
					return;
				}
			}
		}
		bean.setShowWarningMessage(showWarningMessage);
		bean.setWarningMessage(warningMessage);

		bean.show(this, action, msgPrefix);
		bean.setMessage(msg);
	}

	/**
	 * Action method to lock one or more selected Timecards.
	 * @return empty navigation string
	 */
	public String actionLock() {
		MsgUtils.addFacesMessage("Generic.NotAvailable", FacesMessage.SEVERITY_ERROR);
		return null;
	}

	/**
	 * Action method to lock one or more selected weekly batches.
	 * @return empty navigation string
	 */
	public String actionLockBatch() {
		MsgUtils.addFacesMessage("Generic.NotAvailable", FacesMessage.SEVERITY_ERROR);
		return null;
	}

	/**
	 * Action method for the "refresh" button on the dashboard.
	 * @return null navigation string
	 */
	public String actionRefresh() {
		weeklyBatch = getWeeklyBatchDAO().refresh(weeklyBatch);
		createTimecardList();
		setupFirst();
		return null;
	}

	/**
	 * Action method for the "View timecard" button. Put the timecard's id and
	 * the current Department filter id into the appropriate Session variable,
	 * and jump to the full-timecard page. The field {@link #selectedTimecardId}
	 * has been set by a JSF tag. (This is not necessarily the currently selected
	 * timecard, but the one where the user clicked on the status icon.)
	 *
	 * @return the Faces navigation string for the Full Timecard page.
	 */
	public String actionViewTimecard() {
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, selectedTimecardId);
		SessionUtils.put(Constants.ATTR_APPROVER_DEPT, getDepartmentId());
		return HeaderViewBean.PAYROLL_FULL_TC;
	}

	/**
	 * @see com.lightspeedeps.web.approver.FilterHolder#changeTab(int,int)
	 */
	@Override
	public void changeTab(int oldTab, int newTab) {
		if (newTab == FilterBean.TAB_TRANSFER) {
			timecardEntryList = null;	// so it will get rebuilt
			createBothLists();			// need to rebuild Weekly batch list, too.
//			setEndDateList(null);		// rebuild W/E date list; some tabs add "All" choice
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
			createBothLists();
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for the checkboxes on each timecard.
	 * @param event  The event created by the framework.
	 */
	public void listenCheckEntry(ValueChangeEvent event) {
		log.debug("checkbox=" + event.getNewValue());
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			if (! checked) {
				batchMasterCheck.setUnchecked();
			}
		}
	}

	/**
	 * Selection listener for our Batch list.  Passes the event
	 * through to the list manager.
	 * @param evt
	 */
	public void listenBatchRowSelected(SelectEvent evt) {
		sortedBatchList.rowSelected(evt);
	}

	public void listenBatchMasterCheck(ValueChangeEvent event) {
		batchMasterCheck.listenChecked(event);
	}

	public void listenTcMasterCheck(ValueChangeEvent event) {
		tcMasterCheck.listenChecked(event);
	}

	/**
	 * Called when the user clicks on the "master" selection checkbox in the header
	 * area of either the batch or timecard lists.  This will set all the available (enabled) checkboxes
	 * in the list to either checked or unchecked.
	 *
	 * @see com.lightspeedeps.object.ControlHolder#clicked(javax.faces.event.ActionEvent, java.lang.Object)
	 */
	@Override
	public void clicked(TriStateCheckboxExt ckBox, Object id) {
		if (ckBox.isPartiallyChecked()) { // skip partial-check state
			ckBox.setChecked();
		}
		if (ckBox == batchMasterCheck) {
			for (WeeklyBatch wb : batchList) {
				if (ckBox.isUnchecked()) {
					wb.setChecked(false);
				}
				else if (! isUnbatched(wb)) { // skip "unbatched" entry
					wb.setChecked(true);
				}
			}
		}
		else if (ckBox == tcMasterCheck) {
			for (TimecardEntry tce : getTimecardEntryList()) {
				if (ckBox.isUnchecked()) {
					tce.setChecked(false);
				}
				else {
					tce.setChecked(true);
				}
			}
		}
		else {
			log.error("unknown tristate check id!");
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_NEW_BATCH:
				res = actionAddBatchOk();
				break;
			case ACT_RENAME_BATCH:
				res = actionRenameBatchOk();
				break;
			case ACT_DELETE_ITEM:
				res = actionDeleteBatchOk();
				break;
			case ACT_REASSIGN_TC:
				res = actionReassignOk(false);
				break;
			case ACT_REASSIGN_TC_ALL:
				res = actionReassignOk(true);
				break;
			case ACT_PRINT:
				res = actionPrintOk();
				break;
			case ACT_NOTIFY_INCOMPLETE:
				actionTransmitBatch2();
				break;
			case ACT_TRANSMIT_BATCH:
				res = actionTransmitBatchOk();
				break;
			case ACT_TRANSMIT_TIMECARDS:
				res = actionTransmitTimecardsOk();
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
			default:
				res = super.confirmCancel(action);
				break;
		}
		return res;
	}

	private String actionAddBatchOk() {
		try {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			String name = inputBean.getInput();
			if (name == null || name.trim().length() == 0) {
				MsgUtils.addFacesMessage("WeeklyBatch.New.NoName", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			Date date = weeklyBatch.getEndDate(); // get the currently selected batch's date
			if (date == null) {	// "Unbatched" is selected
				date = getWeekEndDate(); // so use the filter date
			}
			WeeklyBatch batch = new WeeklyBatch(production, project, name, date);
			if ((production.getType().isAicp() && isTeamPayroll)) {
				batch.setWorkersComp(getPayrollPref().getWorkersComp());
			}
			getWeeklyBatchDAO().save(batch);
			createBothLists();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	private String actionDeleteBatchOk() {
		try {
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					for (WeeklyTimecard wtc : wb.getTimecards()) {
						wtc.setWeeklyBatch(null);
						getWeeklyBatchDAO().merge(wtc);
					}
					wb.setTimecards(null);
					getWeeklyBatchDAO().delete(wb);
				}
			}
			createBothLists();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Reassign one or more selected timecards to the batch chosen from the
	 * popup.  Called via the confirmOk() method, when the user clicks
	 * the "OK" (or equivalent) button in the popup.
	 *
	 * @return null navigation string
	 */
	private String actionReassignOk(boolean all) {
		try {
			WeeklyBatch toWb;
			PopupSelectBean selectBean;
			if (all) { // If W/E = "All", use pop-up with 2 selection lists
				selectBean = ReassignPopupBean.getInstance();
			}
			else { // use "standard" single selection-list pop-up
				selectBean = PopupSelectBean.getInstance();
			}
			Integer id = null;
			try {
				String str = (String)selectBean.getSelectedObject();
				id = Integer.parseInt(str);
			}
			catch (Exception e) {
			}
			if (id == null) {
				// ignore -- this shouldn't happen!
			}
			else if (id == 0) { // change to un-batched
				reassign(null);
			}
			else {
				toWb = getWeeklyBatchDAO().findById(id);
				if (toWb != null) {
					reassign(toWb);
				}
				else {
					MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
				}
			}
			createBothLists();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Reassign all "checked" WeeklyTimecard`s to the given WeeklyBatch.
	 *
	 * @param toWb The WeeklyBatch to which the timecards should be assigned. If
	 *            null, the timecards will become "un-batched".
	 */
	private void reassign(WeeklyBatch toWb) {
		WeeklyTimecard wtc;
		WeeklyBatch fromWb = weeklyBatch;
		if (isCurrentUnbatched()) {
			fromWb = null;
		}
		List<WeeklyTimecard> timecards = new ArrayList<>();
		for (TimecardEntry tce : timecardEntryList) {
			if (tce.getChecked()) {
				wtc = tce.getWeeklyTc();
				if (wtc != null) {
					timecards.add(wtc);
				}
			}
		}
		if (timecards.size() > 0) {
			if (toWb == null && timecards.get(0).getWeeklyBatch() == null) {
				// shouldn't happen - moving from unbatched to unbatched?
				return;
			}
			if (selectedWorkersComp != null && toWb != null) {
				// need to check workersComp setting of selected timecards vs target batch setting
				if (selectedWorkersComp != toWb.getWorkersComp()) {
					MsgUtils.addFacesMessage("Timecard.SelectedBatchWrongWc", FacesMessage.SEVERITY_ERROR);
					return;
				}
			}
			getWeeklyBatchDAO().moveTimecard(timecards, toWb);
			Set<WeeklyTimecard> sentTimecards = new HashSet<>();
			for (WeeklyTimecard tc : timecards) {
				if (tc.getTimeSent() != null) { // already transmitted to payroll service
					sentTimecards.add(tc);
				}
			}
			if ((sentTimecards.size() > 0) &&
					(getPayrollService().getSendBatchMethod() == ServiceMethod.AUTH_POST)) {
				boolean response = true;
				if (fromWb != null) {
					// timecards already sent have to be re-sent with "removed" status
					fromWb.setTimecards(sentTimecards);
					WeeklyBatchService service = new WeeklyBatchService();
					response = service.transmit(fromWb, sentTimecards, true,
							getProject(), ExportType.JSON, ReportStyle.TC_FULL, false);
				}
				if (! response) {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.Removal.Failed", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
	}

	/**
	 * Action method called (via confirmOk) when the user clicks Ok
	 * in the Rename Batch pop-up dialog.  The batch being renamed is
	 * the one batch whose check-box is checked in the batch list.
	 *
	 * @return null navigation string
	 */
	private String actionRenameBatchOk() {
		try {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			String name = inputBean.getInput();
			// input bean checks for non-blank input, we don't need to...
//			if (name == null || name.trim().length() == 0) {
//				MsgUtils.addFacesMessage("WeeklyBatch.Rename.NoName", FacesMessage.SEVERITY_ERROR);
//				return null;
//			}
			name = name.trim();
			for (WeeklyBatch wb : batchList) {
				if ((! wb.getChecked()) && wb.getName().equalsIgnoreCase(name)) {
					inputBean.displayError("Another batch already has that name");
					return null;
				}
			}
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					if (! name.equals(wb.getName())) {
						wb = getWeeklyBatchDAO().refresh(wb);
						wb.setName(name);
						getWeeklyBatchDAO().merge(wb);
					}
					break;
				}
			}
			createBothLists();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the transmit (batch) facility. It will send all the
	 * selected batches to the current Production's selected payroll service.
	 * Called from {@link #confirmOk(int)} method when the user clicks OK (or
	 * equivalent) on the Transfer batch confirmation pop-up.
	 *
	 * @return null navigation string
	 */
	private String actionTransmitBatchOk() {
		try {
			List<WeeklyBatch> batches = new ArrayList<>();
			int numToSend;
			getPayrollService(); // refresh value
			for (WeeklyBatch wb : batchList) {
				if (wb.getChecked()) {
					numToSend = countReadyTimecards(wb, payrollService.getSendOnlyChanges());
					int numUncalcdTimecards = 0;

					if (isTeamPayroll) {
						numUncalcdTimecards = countUncalcdTimecards(wb,  payrollService.getSendOnlyChanges());
						wb.setJobName(jobName);
						wb.setJobCode(jobCode);
						wb.setGlAccountCode(glAccountCode);
						wb.setCostCenter(costCenter);
						wb.setComments(comments);
						getWeeklyBatchDAO().merge(wb);
					}
					numToSend -= numUncalcdTimecards;
					if (numToSend != 0) {
						batches.add(wb);
					}

				}
			}
			if (batches.size() > 0) {
				actionTransmit(batches, null, null);
				createBothLists();
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoBatches", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Set up final parameters and call the service to transmit (or email) one
	 * or more batches and/or set of timecards to the payroll service.
	 *
	 * @param batches The collection of batches to be sent; should be null if
	 *            'batch' and 'cards' are not null.
	 * @param batch A single batch from which the timecards in 'cards' have been
	 *            selected. This will be ignored if 'batches' is not null.
	 * @param cards The timecards to be sent, when an entire batch was not
	 *            selected. This will be ignored if 'batches' is not null.
	 */
	private void actionTransmit(List<WeeklyBatch> batches, WeeklyBatch batch, Set<WeeklyTimecard> cards) {
		ServiceMethod method = getPayrollService().getSendBatchMethod();
		WeeklyBatchService service = new WeeklyBatchService();
		service.transmitBatch(method, getProject(), transmitBean, batches, batch, cards, null);
	}

	/**
	 * Action method for the transmit (timecard) facility. It will send a single batch
	 * containing the selected WeeklyTimecard`s to the current Production's
	 * selected payroll service.
	 *
	 * @return null navigation string
	 */
	private String actionTransmitTimecardsOk() {
		try {
			WeeklyBatch batch = null;
			for (WeeklyBatch wb : batchList) {
				if (wb.getSelected()) {
					batch = wb;
				}
			}
			if (batch != null) {
				Set<WeeklyTimecard> cards = new HashSet<>();
				// Check to see if this is a Team client.
				for (TimecardEntry tce : timecardEntryList) {
					if (tce.getChecked()) {
						// For Payroll Service that do not want to transmit
						// empty/uncalculatedTimecards
						boolean includeInTransmit = true;
						WeeklyTimecard wtc = tce.getWeeklyTc();

						if (isTeamPayroll) {
							if (wtc.getEmpty() ||  (wtc.getGrandTotal() == null || wtc.getGrandTotal().signum() == 0)) {
								includeInTransmit = false;
							}
						}
						if (includeInTransmit) {
							cards.add(wtc);
						}
					}
				}
				if (cards.size() > 0) {
					actionTransmit(null, batch, cards);
				}
				else {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoTimecards", FacesMessage.SEVERITY_INFO);
				}
				createBothLists();
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoBatches", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Count how many timecards have their check-box selected.
	 * @return Non-negative count.
	 */
	public int getSelectedTimecardCount() {
		int items = 0;
		if (timecardEntryList != null) {
			for (TimecardEntry tce : timecardEntryList) {
				if (tce.getChecked()) {
					items++;
				}
			}
		}
		return items;
	}

	/**
	 * Count how many timecards have their check-box selected and have either
	 * (a) never been sent or (b) been updated since they were last sent.
	 * @return Non-negative count.
	 */
	private int countReadyTimecards(boolean onlyChanges) {
		int items = 0;
		for (TimecardEntry tce : timecardEntryList) {
			if (tce.getChecked()) {
				if ((! onlyChanges) || tce.getWeeklyTc().getTimeSent() == null ||
						tce.getWeeklyTc().getUpdated().after(tce.getWeeklyTc().getTimeSent())) {
					items++;
				}
			}
		}
		return items;
	}

	/**
	 * Count how many timecards in a batch are ready to be sent. This means they
	 * are transmittable (have all required fields) and, if "onlyChanges" is
	 * true, they also must have either (a) never been sent or (b) been updated
	 * since they were last sent.
	 *
	 * @param onlyChanges Only count timecards that haven't been sent, or have
	 *            been changed since they were sent.
	 *
	 * @return Non-negative count.
	 */
	private int countReadyTimecards(WeeklyBatch batch, boolean onlyChanges) {
		int items = 0;
		batch = getWeeklyBatchDAO().refresh(batch);
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			if (wtc.getTransmittable()) {
				if ((! onlyChanges) ||
						wtc.getTimeSent() == null ||
						wtc.getUpdated().after(wtc.getTimeSent())) {
					items++;
				}
			}
		}
		return items;
	}

	/**
	 * Count the number of uncalculated timecards. If the timecard is blank,
	 * we do not want to add it to the uncalculated count since it will already be
	 * included in the blank timecard count. If "onlyChanges" is
	 * true, they also must have either (a) never been sent or (b) been updated
	 * since they were last sent.
	 *
	 * @param batch
	 * @param onlyChanges
	 * @return
	 */
	private int countUncalcdTimecards(WeeklyBatch batch, boolean onlyChanges) {
		int items = 0;
		batch = getWeeklyBatchDAO().refresh(batch);
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			if (wtc.getTransmittable()) {
				if(TimecardUtils.checkForMissingPaymentInfo(wtc)) {
					if ((! onlyChanges) ||
							wtc.getTimeSent() == null ||
							wtc.getUpdated().after(wtc.getTimeSent())) {
						items++;
					}
				}
			}
		}
		return items;
	}

	/**
	 * Count the number of blank uncalculated from the list of checked timecards. If the timecard is blank,
	 * we do not want to add it to the uncalculated count since it will already be
	 * included in the blank timecard count. If "onlyChanges" is
	 * true, they also must have either (a) never been sent or (b) been updated
	 * since they were last sent.
	 *
	 * @param batch
	 * @param onlyChanges
	 * @return
	 */
	private int countUncalcdTimecards(boolean onlyChanges) {
		int items = 0;
		for (TimecardEntry tce : timecardEntryList) {
			if (tce.getChecked()) {
				if(TimecardUtils.checkForMissingPaymentInfo(tce.getWeeklyTc())) {
					if ((! onlyChanges) || tce.getWeeklyTc().getTimeSent() == null ||
							tce.getWeeklyTc().getUpdated().after(tce.getWeeklyTc().getTimeSent())) {
						items++;
					}
				}
			}
		}
		return items;
	}

	/**
	 * Count the number of timecards in a batch that are incomplete, that is,
	 * are not transmittable.
	 *
	 * @param batch
	 * @return Non-negative count.
	 */
	private int countNotReadyTimecards(WeeklyBatch batch) {
		int items = 0;
		batch = getWeeklyBatchDAO().refresh(batch);
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			if (! wtc.getTransmittable()) {
				items++;
			}
		}
		return items;
	}

//	private static String checkBatch(WeeklyBatch batch) {
//		String name = null;
//		for (WeeklyTimecard wtc : batch.getTimecards()) {
//			if (! wtc.getTransmittable()) {
//				name = wtc.getLastNameFirstName();
//				break;
//			}
//		}
//		return name;
//	}

	/**
	 * Verify if all "checked" timecards in the current timecard list are
	 * "transmittable" -- that they have all fields required by the payroll
	 * service and they are not VOIDed.
	 *
	 * @return Null if all timecards are good, otherwise the name of the
	 *         employee on the first incomplete timecard, in "last name, first
	 *         name" style.
	 */
	private String checkTimecards() {
		String name = null;
		for (TimecardEntry tce : timecardEntryList) {
			if (tce.getChecked()) {
				if (! tce.getWeeklyTc().getTransmittable()) {
					name = tce.getWeeklyTc().getLastNameFirstName();
					break;
				}
			}
		}
		return name;
	}

	/**
	 * This method is fired when row is selected and set the bean value depend on the ID selected
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (weeklyTimecard != null) {
			weeklyTimecard.setSelected(false);
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
			if (tce != null) { // should always find this!
				weeklyTimecard = tce.getWeeklyTc();
			}
			if (weeklyTimecard == null) {
				id = findDefaultId();
				if (id != null) {
					weeklyTimecard = getWeeklyTimecardDAO().findById(id);
				}
			}
		}
//		SessionUtils.put(Constants.ATTR_TIMECARD_ID, id);
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
			id = list.get(0).getWeeklyTc().getId();
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
	 * Set up to display the first timecard in the bottom window, that is, the
	 * first entry in timecardEntryList.
	 */
	private void setupFirst() {
		if (timecardEntryList.size() > 0) {
			weeklyTimecard = timecardEntryList.get(0).getWeeklyTc();
//			SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		}
		else { // Create empty timecard
			weeklyTimecard = null;
		}
		setupSelectedItem();
	}

	/**
	 * Do any setup for displaying a particular WeeklyTimecard that
	 * has been selected.
	 */
	private void setupSelectedItem() {
		if (weeklyTimecard != null) {
			// 'selected' status is currently unused in Transfer
			// tab, and causes problems on other tabs! 3.2.8720
//			weeklyTimecard.setSelected(true);
		}
	}

	/**
	 * Called when the user clicks on an entry in the WeeklyBatch list.
	 *
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setupSelectableItem(java.lang.Integer)
	 */
	@Override
	public void setupSelectableItem(Integer id) {
		log.debug("id=" + id);
		if (weeklyBatch != null) {
			weeklyBatch.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		weeklyBatch = null;
		if (id == null) {
			//editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			weeklyBatch = getWeeklyBatchDAO().findById(id);
		}
		else {
			Project showProject = project;
			if (getShowAllProjects()) {
				showProject = null;
			}
			weeklyBatch = createUnbatched(showProject); // need to refresh all TCs in list
		}
		setupBatchItem();
	}

	/**
	 * Do any setup for displaying a particular WeeklyBatch entry.
	 */
	private void setupBatchItem() {
		createTimecardList(); // refresh list of TC's, and updates their status
		setupFirst();
		if (weeklyBatch != null) {
			if (weeklyBatch != unBatched) {
				WeeklyBatchUtils.checkAndUpdateStatus(weeklyBatch);
			}
			weeklyBatch.setSelected(true);
			sortedBatchList.updateItemInList(weeklyBatch);
			SessionUtils.put(Constants.ATTR_BATCH_ID, weeklyBatch.getId());
		}
		else {
			SessionUtils.put(Constants.ATTR_BATCH_ID, null);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#selectableRowChanged()
	 */
	@Override
	public void selectableRowChanged() {
		// nothing to do here.
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setSelectableSelected(java.lang.Object, boolean)
	 */
	@Override
	public void setSelectableSelected(Object item, boolean b) {
		((WeeklyBatch)item).setSelected(b);
	}

	/**
	 * @see com.lightspeedeps.web.approver.ApproverFilterBase#refreshTimecardList()
	 */
	@Override
	protected void refreshTimecardList() {
		createBothLists();
	}

	/**
	 * Create a new list of WeeklyBatch objects, and the list of WeeklyTimecard
	 * objects for the current batch.
	 */
	private void createBothLists() {
		createBatchList();
		createTimecardList();
		setupFirst();
	}

	/**
	 * Fill the batch list with batches from the current Production and the
	 * currently selected week-ending date. This also preserves the currently
	 * selected batch, if possible.  For a Commercial Production, the list
	 * is also limited by the current Project.
	 */
	private void createBatchList() {
		Integer batchId = null;
		if (weeklyBatch != null) {
			batchId = weeklyBatch.getId();
		}

		WeeklyBatchUtils.createBatchList(batchList, production, project, getWeekEndDate(), getShowAllProjects());
		WeeklyBatch batch = createUnbatched(getShowAllProjects() ? null : project);

		batchList.add(0,batch);
		weeklyBatch = batchList.get(0); // default to select unbatched list

		for (WeeklyBatch wb : batchList) { // update gross and "last updated" timestamps
			if (isUnbatched(wb)) {
				wb.updateGross();
			}
			else {
				WeeklyBatchUtils.checkAndUpdateStatus(wb);
			}
		}

		if (batchId != null && batchId != NEW_ID) {
			for (WeeklyBatch wb : batchList) { // try & restore prior selection
				if (wb.getId().equals(batchId)) {
					weeklyBatch = wb;
					break;
				}
			}
		}
		sort(batchList, sortedBatchList); // restore current sort of batch list
		setupBatchItem();
		batchMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		batchMasterCheck.setDisabled(batchList.size() <= 1);
	}

	/**
	 * Creates a local WeeklyBatch containing all the "unbatched" timecards for
	 * the currently displayed week-ending date. Sets {@link #unBatched} to the
	 * created instance.
	 *
	 * @param showProject The Project for which unbatched timecards should be
	 *            included. Only non-null for Commercial productions.
	 *
	 * @return {@link #unBatched}
	 */
	private WeeklyBatch createUnbatched(Project showProject) {
		WeeklyBatch wb = new WeeklyBatch();
		wb.setName("UNBATCHED");
		wb.setProduction(production);
		wb.setProject(showProject);
		wb.setId(NEW_ID);
		List<WeeklyTimecard> list;
		if (getWeekEndDate().equals(Constants.SELECT_ALL_DATE)) {
			wb.setEndDate(null);
			list = WeeklyTimecardDAO.getInstance().findByProductionUnbatched(production, showProject);
		}
		else if (getWeekEndDate().equals(Constants.SELECT_PRIOR_DATE)) {
			wb.setEndDate(null);
			Date weDate = TimecardUtils.calculateDefaultWeekEndDate();
			list = WeeklyTimecardDAO.getInstance()
					.findByWeekEndDateUnbatched(production, showProject, weDate, TimecardRange.PRIOR);
		}
		else {
			wb.setEndDate(getWeekEndDate());
			list = WeeklyTimecardDAO.getInstance()
					.findByWeekEndDateUnbatched(production, showProject, getWeekEndDate(), TimecardRange.WEEK);
		}
		wb.setTimecards(new HashSet<>(list));
		unBatched = wb;
		return unBatched;
	}

	/**
	 * Create a list of choices of WeeklyBatch names.
	 *
	 * @param fromBatch The source batch, which should not be included in the
	 *            returned selection list.
	 * @param unbatchedOnly If true, the returned list will only have the
	 *            "unbatched" choice.
	 *
	 * @return A newly-created, non-null, List of SelectItem objects describing
	 *         all of the WeeklyBatch instances EXCEPT for the currently
	 *         displayed one. This list is used in the "Reassign" pop-up. The
	 *         SelectItem.value field is the database id of the corresponding
	 *         WeeklyBatch. The first entry in the list will be labeled
	 *         "Unbatched", with an id (SelectItem.value) field of zero.
	 */
	private List<SelectItem> createBatchSelectList(WeeklyBatch fromBatch, boolean unbatchedOnly) {
		List<SelectItem> list = new ArrayList<>();
		if (! isUnbatched(fromBatch)) {
			list.add(new SelectItem(0, "(Unbatched)"));
		}
		if (! unbatchedOnly) {
			for (WeeklyBatch wb : batchList) {
				if ((! wb.equals(fromBatch)) && ! isUnbatched(wb)) {
					list.add(new SelectItem(wb.getId(), wb.getName()));
				}
			}
		}
		return list;
	}

	/**
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
	 * Create the list of time cards to be displayed, from the currently
	 * selected 'weeklyBatch'.
	 */
	@Override
	protected void createTimecardList() {
		List<TimecardEntry> list = new ArrayList<>();
		if (weeklyBatch != null) {
			for (WeeklyTimecard wtc : weeklyBatch.getTimecards()) {
				TimecardCalc.calculateWeeklyTotals(wtc);
				wtc.getStartForm().getContact().getUser().getLastNameFirstName(); // fixes LIE when sorting by name column
				list.add(new TimecardEntry(wtc));
			}
		}
		tcMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		tcMasterCheck.setDisabled(list.size() == 0);
		timecardEntryList = list;
		ApproverUtils.calculateListApprovalStatus(timecardEntryList);
		sort();
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List, com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void sort(@SuppressWarnings("rawtypes") List list, SortableList sortableList) {
		batchAscending = sortableList.isAscending();
		batchCompareColumn = sortableList.getSortColumnName();
		Collections.sort(list, batchComparator);
	}

	public Comparator<WeeklyBatch> batchComparator = new Comparator<WeeklyBatch>() {
		@Override
		public int compare(WeeklyBatch one, WeeklyBatch two) {
			int ret = one.compareTo(two, batchCompareColumn, batchAscending);
			return ret;
		}
	};

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		if (sortColumn.equals(WeeklyBatch.SORTKEY_UPDATED)
				|| sortColumn.equals(WeeklyBatch.SORTKEY_DATE)
				|| sortColumn.equals(WeeklyBatch.SORTKEY_SENT_DATE) ) {
			return false; // Date & hours default to descending
		}
		return true; // make columns default to ascending sort
	}

	/**
	 * Count how many batch entries have their check-box selected.
	 * @return Non-negative count.
	 */
	public int getSelectedBatchCount() {
		int items = 0;
		for (WeeklyBatch wb : batchList) {
			if (wb.getChecked()) {
				items++;
			}
		}
		return items;
	}

	/** See {@link #payrollPref}. */
	public PayrollPreference getPayrollPref() {
		if (payrollPref == null) {
			if (getProduction().getType().hasPayrollByProject()) {
				payrollPref = SessionUtils.getCurrentProject().getPayrollPref();
			}
			else {
				payrollPref = getProduction().getPayrollPref();
			}
		}
		return payrollPref;
	}

	// accessors and mutators

	/**See {@link #payrollService}. */
	public PayrollService getPayrollService() {
		if (payrollService == null) {
			payrollService = SessionUtils.getProduction().getPayrollPref().getPayrollService();
		}
		return payrollService;
	}

	private boolean isUnbatched(WeeklyBatch batch) {
		return (batch != null && batch.isUnBatched());
	}

	private boolean isCurrentUnbatched() {
		return isUnbatched(weeklyBatch);
	}

	/**See {@link #weeklyBatch}. */
	public WeeklyBatch getWeeklyBatch() {
		return weeklyBatch;
	}
	/**See {@link #weeklyBatch}. */
	public void setWeeklyBatch(WeeklyBatch weeklyBatch) {
		this.weeklyBatch = weeklyBatch;
	}

	/** See {@link #batchMasterCheck}. */
	public TriStateCheckboxExt getBatchMasterCheck() {
		return batchMasterCheck;
	}
	/** See {@link #batchMasterCheck}. */
	public void setBatchMasterCheck(TriStateCheckboxExt masterState) {
		batchMasterCheck = masterState;
	}

	/**See {@link #tcMasterCheck}. */
	public TriStateCheckboxExt getTcMasterCheck() {
		return tcMasterCheck;
	}
	/**See {@link #tcMasterCheck}. */
	public void setTcMasterCheck(TriStateCheckboxExt tcCheck) {
		tcMasterCheck = tcCheck;
	}

	/**See {@link #selectedTimecardId}. */
	public Integer getSelectedTimecardId() {
		return selectedTimecardId;
	}
	/**See {@link #selectedTimecardId}. */
	public void setSelectedTimecardId(Integer selectedTimecardId) {
		this.selectedTimecardId = selectedTimecardId;
	}

	/**See {@link #sortedBatchList}. */
	public SelectableList getSortedBatchList() {
		return sortedBatchList;
	}

	/**See {@link #weeklyBatchDAO}. */
	public WeeklyBatchDAO getWeeklyBatchDAO() {
		if (weeklyBatchDAO == null){
			weeklyBatchDAO = WeeklyBatchDAO.getInstance();
		}
		return weeklyBatchDAO;
	}

	/**See {@link #jobName}. */
	public String getJobName() {
		if (jobName == null) {
			return project.getTitle();
		}
		return jobName;
	}
	/**See {@link #jobName}. */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/**See {@link #jobCode}. */
	public String getJobCode() {
		if (jobCode == null) {
			return project.getEpisode();
		}
		return jobCode;
	}
	/**See {@link #jobCode}. */
	public void setJobCode(String jobCode) {
		this.jobCode = jobCode;
	}

	/**See {@link #glAccountCode}. */
	public String getGlAccountCode() {
		return glAccountCode;
	}
	/**See {@link #glAccountCode}. */
	public void setGlAccountCode(String glAccountCode) {
		this.glAccountCode = glAccountCode;
	}

	/**See {@link #costCenter}. */
	public String getCostCenter() {
		return costCenter;
	}
	/**See {@link #costCenter}. */
	public void setCostCenter(String costCenter) {
		this.costCenter = costCenter;
	}

	/**See {@link #costCenter}. */
	public String getComments() {
		return comments;
	}
	/**See {@link #costCenter}. */
	public void setComments(String comments) {
		this.comments = comments;
	}

}
