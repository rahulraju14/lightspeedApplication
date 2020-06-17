/**
 * File: PrintTimecardBean.java
 */
package com.lightspeedeps.web.timecard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.WeeklyBatchDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.EnumList;

/**
 * This class extends TimecardSelectBase with facilities to select the
 * various timecard printing options. It also handles options for the
 * Timecard-PR Discrepancy report.
 */
@ManagedBean
@ViewScoped
public class PrintTimecardBean extends TimecardSelectBase implements Serializable {
	/** */
	private static final long serialVersionUID = 4507600958965707930L;

	private static final Log log = LogFactory.getLog(PrintTimecardBean.class);

	/** Indicates the report style selected */
	private String reportStyle = ReportStyle.TC_FULL.name();

	/** Drop-down list of styles of timecard reports available. */
	private List<SelectItem> reportStyleDL;

	/** The value of the Report Type drop-down entry selected by the user. */
	private String reportType = TYPE_TIMECARD;
	public static final String TYPE_TIMECARD = "t";
	public static final String TYPE_PAYROLL = "p";
	public static final String TYPE_DISCREPANCY = "d";
	public static final String TYPE_BATCH_REPORT= "b";
	public static final String TYPE_DAILY_TIMESHEET = "dt";

	/** Default drop-down list of types of reports available. */
	private static final List<SelectItem> REPORT_TYPE_DL = Arrays.asList(
			new SelectItem(TYPE_TIMECARD, "Timecards"),
			new SelectItem(TYPE_DISCREPANCY, "Timecard - PR Discrepancy Report"),
			new SelectItem(TYPE_BATCH_REPORT, "Batch Report"),
			new SelectItem(TYPE_PAYROLL, "Payroll Report"),
			new SelectItem(TYPE_DAILY_TIMESHEET, "Daily Timesheet")
			);

	/** Drop-down list of types of reports available for approver who is also
	 * looking at their own timecard. */
	private static final List<SelectItem> REPORT_TYPE_APPROVER_DL = Arrays.asList(
			new SelectItem(TYPE_TIMECARD, "Timecards"),
			new SelectItem(TYPE_DAILY_TIMESHEET, "Daily Timesheet")
			);

	/** Drop-down list of types of reports available for approver who is NOT
	 * looking at their own timecard. */
	private static final List<SelectItem> REPORT_TYPE_APPROVER_ONLY_DL = Arrays.asList(
			new SelectItem(TYPE_DAILY_TIMESHEET, "Daily Timesheet")
			);

	/** The list of ReportTypes to be displayed to the user.  It will be one of the
	 * static lists above; which one depends on the various parameters passed to
	 * the 'show' method. */
	private List<SelectItem> reportTypeDL = REPORT_TYPE_DL;

	/** If true, the Discrepancy report options will be available. */
	private boolean showDisc = true;

	/** If true, include the Timecard in the printed output. */
	private boolean includeTimecard = true;

	/** If true, include the Box Rental form in the printed output. */
	private boolean includeBox = false;

	/** If true, include the Mileage form in the printed output. */
	private boolean includeMileage = false;

	/** If true, include pay breakdown information in the printout. */
	private boolean includeBreakdown = true;

	/** If true, include attchments in the printout. */
	private boolean includeAttachments= false;

	/** Indicates the type of Discrepancy report selected */
	protected String discReportType = DISC_TYPE_DETAIL;
	public static final String DISC_TYPE_DETAIL = "d";
	public static final String DISC_TYPE_SUMMARY = "s";

	/** Discrepancy report: exclude Off Production employees */
	private boolean discExcludeOffProd;

	/** Discrepancy report: exclude MPV differences */
	private boolean discExcludeMpv;

	/** Discrepancy report: exclude days with No PR. */
	private boolean discExcludeNoPr = true;

	/** Discrepancy report: exclude Sick and other non-Work days */
	private boolean discExcludeNonWork = true;

	/** The currently selected WeeklyBatch entry in our list. */
	private WeeklyBatch weeklyBatch;

	/** Backing field for the user's input. */
	private Object selectedObject;

	/**The drop-down list of available batches for the currently selected Week ending date*/
	private List<SelectItem> selectBatchList;

	/** Backing field for radio buttons -- indicates the type of Batch report
	 * selected by the user. */
	private String batchReportType = ALL_BATCH_TYPE;
	/** Indicates printing all batches; may or may not include unbatched timecards. */
	public static final String ALL_BATCH_TYPE= "a";
	/** Indicates printing only unbatched timecards. */
	public static final String ONLY_UNBATCHED_TYPE= "u";
	/** Indicates printing only a specific batch of timecards. */
	public static final String ONLY_BATCH_TYPE= "b";

	/** Backing field for checkbox -- if true, include unbatched timecards in selection */
	private boolean allUnbatchedTimecard=false;

	/** If true , all the radio buttons for Timecard report styles will be displayed
	 *  on the Timecard pop up; if false, the options specific to a timecard will not
	 *  be displayed. */
	private boolean timecardSelected = false;

	/** If true, the "Selected timecards" option should be displayed and enabled in the
	 * dialog box. */
	boolean allowSelected = false;

	/** If true, generates the report in xls format for Payroll Report. */
	private boolean exportToXls = false;

	/** The week day number selected on pop up for daily timesheet. */
	private Integer weekDay;

	/** List of SelectItem`s of Department`s for the drop-down list used for
	 * the Daily Timesheet report options. */
	private List<SelectItem> departmentList;

	/** The value field from the Department entry selected on pop up for daily timesheet. */
	private String timesheetDeptText;

	/** The Department for daily timesheet, determined from 'text' value above. */
	private Department timesheetDept;

	/** Drop-down list of styles of Payroll reports available. */
	private static final List<SelectItem> PAYROLL_REPORT_STYLE = Arrays.asList(
			new SelectItem(ReportStyle.TC_PAY_HOURS.name(), ReportStyle.TC_PAY_HOURS.toString()),
			new SelectItem(ReportStyle.TC_PAY_BREAKDOWN.name(), ReportStyle.TC_PAY_BREAKDOWN.toString())
			);

	public PrintTimecardBean() {
	}

	public static PrintTimecardBean getInstance() {
		return (PrintTimecardBean)ServiceFinder.findBean("printTimecardBean");
	}

	/**
	 * Display the print options dialog with the specified values. Note that the
	 * Strings are all message-ids, which will be looked up in the message
	 * resource file.
	 *
	 * @param full True iff all options should be presented to the user. When
	 *            false, the options for "whole department" and "all crew" are
	 *            not presented in the dialog box.
	 * @param cloneAuth True iff the current user has Clone authority, which will
	 *            also give them access to some multi-person reports, such as
	 *            the Daily Timesheet, but NOT to all reports.
	 * @param disc If true, the Discrepancy report options will be available in
	 *            the dialog box.
	 * @param showAllProj If true, the action is for all projects within a
	 *            Commercial production. Additional message text will be
	 *            displayed about "print selections include all projects", and
	 *            the date selection drop-down list will be calculated
	 *            accordingly.
	 * @param prod The Production containing the timecards to be printed.
	 * @param project The Project associated with the timecard(s) being printed,
	 *            if any. This will be non-null only for Commercial productions.
	 * @param dept The name of the department, which will be included in the
	 *            dialog box text.
	 * @param batch The name of the batch to include in the print options.
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box.
	 * @param msgId The message-id of the message to be shown in the dialog.
	 * @param buttonOkId The message-id of the text for the "OK" button.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button.
	 * @param isTimeCardSelected boolean field to check time card is selected or not.
	 * @param ownTimecard True iff a timecard was selected and it belongs to the
	 *            currently logged-in user.
	 * @param allowSelected True if the "Selected timecards" option should be enabled.
	 */
	public void show(boolean full, boolean cloneAuth, boolean disc, boolean showAllProj, Production prod,
			Project proj, String dept, String batch, PopupHolder holder, int act,
			String titleId, String msgId, String buttonOkId, String buttonCancelId,
			boolean isTimeCardSelected, boolean ownTimecard, boolean pAllowSelected) {
		log.debug("action=" + act);
		showDisc = disc;
		allowSelected = pAllowSelected;

		reportTypeDL = REPORT_TYPE_DL; // default drop-down list
		if (cloneAuth && (! full)) { // dept approver or timekeeper
			if (ownTimecard) {
				reportTypeDL = REPORT_TYPE_APPROVER_DL; // drop-down list if approver & selected own timecard
				setReportType(TYPE_TIMECARD);
			}
			else {
				reportTypeDL = REPORT_TYPE_APPROVER_ONLY_DL; // drop-down if approver and not own timecard selected
				setReportType(TYPE_DAILY_TIMESHEET);
			}
			showDisc = true; // this enables report-choice drop-down
		}

		// A number of fields are left unchanged by show() --
		// they will keep the value last selected by the user if they haven't left the current page.
//		includeTimecard = true;
//		includeBreakdown = true;
//		includeBox = false;
//		includeMileage = false;
//		reportStyle = STYLE_FULL;
		if(reportType.equals(TYPE_TIMECARD)) {
			exportToXls = false;
			if (prod.getPayrollPref().getUseModelRelease()) {
				reportStyle = ReportStyle.TC_MODEL.name();
				reportStyleDL = null;
			}
			else if (prod.getType().isAicp()) {
				reportStyle = ReportStyle.TC_AICP.name();
				reportStyleDL = null;
			}
			else {
				reportStyleDL = EnumList.createEnumSelectListStopNA(ReportStyle.class);
			}
		}
		else {
			reportStyleDL = PAYROLL_REPORT_STYLE;
		}
		// discrepancy defaults:
//		discReportType = DISC_TYPE_DETAIL;
//		discExcludeMpv = false;
//		discExcludeNoPr = true;
//		discExcludeNonWork = true;
//		sortOrder = ORDER_DATE;
		batchReportType = ALL_BATCH_TYPE;
		timecardSelected = isTimeCardSelected;
		selectBatchList = null; // force refresh of SelectItem list of WeeklyBatch
		departmentList = null; // force refresh of department list

		super.show(prod, proj, dept, batch, full, showAllProj, holder, act, titleId, msgId,
				buttonOkId, buttonCancelId);

		if (! timecardSelected) { // the usual default radio button will not be available,
			setRangeSelection(RANGE_ALL); // so select the "entire crew" button instead.
		}
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		if (! (includeTimecard || includeBox || includeMileage)) {
			MsgUtils.addFacesMessage("Timecard.Print.MissingInclude", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		return super.actionOk();
	}

	/**
	 * Method used to handle an ESCape key function.  If the confirmation dialog
	 * is visible, call our normal Cancel function and return true; otherwise
	 * return false.
	 */
	public static boolean actionEscape() {
		log.debug("");
		boolean ret = false;
		PrintTimecardBean bean = getInstance();
		if (bean.isVisible()) {
			bean.actionCancel();
			ret = true;
		}
		return ret;
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 * @param event contains old and new values
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
			if (event.getNewValue() != null) {
				setWeekEndDate((Date)event.getNewValue());
				setSecondWeekEndDate((Date)event.getNewValue());
				setSelectBatchList(createBatchSelectList());
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Report/Document Type drop-down list.
	 * @param event contains old and new values
	 */
	public void listenReportTypeChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
			if (event.getNewValue() != null && event.getNewValue().toString().equals(TYPE_PAYROLL)) {
				reportStyleDL = PAYROLL_REPORT_STYLE;
			}
			else {
				exportToXls = false;
				if (SessionUtils.getProduction().getPayrollPref().getUseModelRelease()) {
					reportStyle = ReportStyle.TC_MODEL.name();
					reportStyleDL = null;
				}
				else if (SessionUtils.getProduction().getType().isAicp()) {
					reportStyle = ReportStyle.TC_AICP.name();
					reportStyleDL = null;
				}
				else {
					reportStyleDL = EnumList.createEnumSelectListStopNA(ReportStyle.class);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Create the drop-down list of WeeklyBatch`s that exist for the currently
	 * selected week-ending date
	 */
	private List<SelectItem> createBatchSelectList() {
		Project showProject = project;
		if (getShowAllProjects()) {
			showProject = null;
		}
		Date date = getWeekEndDate();
		if (date.equals(Constants.SELECT_ALL_DATE)) {
			date = null;
		}
		List<WeeklyBatch> batchList = WeeklyBatchDAO.getInstance().findByProductionProjectDate(
				production, showProject, date, (showProject == null));
		List<SelectItem> list = new ArrayList<>();
		for (WeeklyBatch wb : batchList) {
			list.add(new SelectItem(wb.getId(), wb.getName()));
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
	public List<SelectItem> createDepartmentList() {
		ArrayList<SelectItem> deptList;
		boolean userHasViewHtg = AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_VIEW_HTG);
		Contact currContact = SessionUtils.getCurrentContact();
		if (userHasViewHtg || ApproverUtils.isProdOrProjectApprover(currContact, project)) {
			deptList = new ArrayList<>(DepartmentUtils.getDepartmentDataAdminDL(
					getShowAllProjects() ? null : project, true));
		}
		else {
			// Find depts with this user as approver
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(currContact, project);
			deptList = new ArrayList<>();
			for (Department dept : depts) {
				deptList.add(new SelectItem(dept.getId(), dept.getName()));
			}
			// find any additional departments for which the current contact is a "time keeper"
			Contact vContact = SessionUtils.getCurrentContact();
			List<Department> tkDeps = DepartmentDAO.getInstance().findStdDeptsByContact(vContact, project);
			for (Department dept : tkDeps) {
				if (! depts.contains(dept)) { // avoid adding duplicates
					deptList.add(new SelectItem(dept.getId(), dept.getName()));
				}
			}
		}
		if (deptList.size() > 0) {
			setTimesheetDeptText(deptList.get(0).getValue().toString());
		}
		return deptList;
	}

	/**
	 * @return Drop-down selection list of week-day names.
	 */
	public List<SelectItem> getWeekDayNameList() {
		return Constants.WEEKDAY_NAMES_DL;
	}

	/**See {@link #reportType}. */
	public String getReportType() {
		return reportType;
	}
	/**See {@link #reportType}. */
	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	/**See {@link #REPORT_TYPE_DL}. */
	public List<SelectItem> getReportTypeDL() {
		return reportTypeDL;
	}

	/**See {@link #showDisc}. */
	public boolean getShowDisc() {
		return showDisc;
	}
	/**See {@link #showDisc}. */
	public void setShowDisc(boolean showDisc) {
		this.showDisc = showDisc;
	}

	/** See {@link #includeTimecard}. */
	public boolean getIncludeTimecard() {
		return includeTimecard;
	}
	/** See {@link #includeTimecard}. */
	public void setIncludeTimecard(boolean includeTimecard) {
		this.includeTimecard = includeTimecard;
	}

	/**See {@link #includeBreakdown}. */
	public boolean getIncludeBreakdown() {
		return includeBreakdown;
	}
	/**See {@link #includeBreakdown}. */
	public void setIncludeBreakdown(boolean includeBreakdown) {
		this.includeBreakdown = includeBreakdown;
	}

	/** See {@link #includeBox}. */
	public boolean getIncludeBox() {
		return includeBox;
	}
	/** See {@link #includeBox}. */
	public void setIncludeBox(boolean includeBox) {
		this.includeBox = includeBox;
	}

	/** See {@link #includeMileage}. */
	public boolean getIncludeMileage() {
		return includeMileage;
	}
	/** See {@link #includeMileage}. */
	public void setIncludeMileage(boolean includeMileage) {
		this.includeMileage = includeMileage;
	}

	/** See {@link #includeAttachments}. */
	public boolean getIncludeAttachments() {
		return includeAttachments;
	}
	/** See {@link #includeAttachments}. */
	public void setIncludeAttachments(boolean includeAttachments) {
		this.includeAttachments = includeAttachments;
	}

	/** See {@link #reportStyle}. */
	public String getReportStyle() {
		return reportStyle;
	}
	/** See {@link #reportStyle}. */
	public void setReportStyle(String reportStyle) {
		this.reportStyle = reportStyle;
	}

	/**
	 * @return The user-selected report style as an enum value.
	 */
	public ReportStyle getReportStyleEnum() {
		return ReportStyle.valueOf(reportStyle);
	}

	/** See {@link #reportStyleDL}. */
	public List<SelectItem> getReportStyleDL() {
		return reportStyleDL;
	}

	// * * * Discrepancy report settings

	/**See {@link #discReportType}. */
	public String getDiscReportType() {
		return discReportType;
	}
	/**See {@link #discReportType}. */
	public void setDiscReportType(String discReportType) {
		this.discReportType = discReportType;
	}

	/**See {@link #discExcludeOffProd}. */
	public boolean getDiscExcludeOffProd() {
		return discExcludeOffProd;
	}
	/**See {@link #discExcludeOffProd}. */
	public void setDiscExcludeOffProd(boolean discExcludeOffProd) {
		this.discExcludeOffProd = discExcludeOffProd;
	}

	/**See {@link #discExcludeMpv}. */
	public boolean getDiscExcludeMpv() {
		return discExcludeMpv;
	}
	/**See {@link #discExcludeMpv}. */
	public void setDiscExcludeMpv(boolean discExcludeMpv) {
		this.discExcludeMpv = discExcludeMpv;
	}

	/**See {@link #discExcludeNoPr}. */
	public boolean getDiscExcludeNoPr() {
		return discExcludeNoPr;
	}
	/**See {@link #discExcludeNoPr}. */
	public void setDiscExcludeNoPr(boolean discExcludeNoPr) {
		this.discExcludeNoPr = discExcludeNoPr;
	}

	/**See {@link #discExcludeNonWork}. */
	public boolean getDiscExcludeNonWork() {
		return discExcludeNonWork;
	}
	/**See {@link #discExcludeNonWork}. */
	public void setDiscExcludeNonWork(boolean discExcludeNonWork) {
		this.discExcludeNonWork = discExcludeNonWork;
	}

	// * * * Batch report settings * * *

	/**See {@link #batchReportType}. */
	public String getBatchReportType() {
		return batchReportType;
	}
	/**See {@link #batchReportType}. */
	public void setBatchReportType(String batchReportType) {
		this.batchReportType = batchReportType;
	}

	/**See {@link #allUnbatchedTimecard}. */
	public boolean getAllUnbatchedTimecard() {
		return allUnbatchedTimecard;
	}
	/**See {@link #allUnbatchedTimecard}. */
	public void setAllUnbatchedTimecard(boolean allUnbatchedTimecard) {
		this.allUnbatchedTimecard = allUnbatchedTimecard;
	}

	/**See {@link #timecardSelected}. */
	public boolean getTimecardSelected() {
		return timecardSelected;
	}
	/**See {@link #timecardSelected}. */
	public void setTimecardSelected(boolean timecardSelected) {
		this.timecardSelected = timecardSelected;
	}

	/** See {@link #allowSelected}. */
	public boolean getAllowSelected() {
		return allowSelected;
	}
	/** See {@link #allowSelected}. */
	public void setAllowSelected(boolean allowSelected) {
		this.allowSelected = allowSelected;
	}

	/**See {@link #weeklyBatch}. */
	public WeeklyBatch getWeeklyBatch() {
		return weeklyBatch;
	}
	/**See {@link #weeklyBatch}. */
	public void setWeeklyBatch(WeeklyBatch weeklyBatch) {
		this.weeklyBatch = weeklyBatch;
	}

	/**See {@link #selectedObject}. */
	public Object getSelectedObject() {
		return selectedObject;
	}
	/**See {@link #selectedObject}. */
	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

	/**See {@link #selectBatchList}. */
	public List<SelectItem> getSelectBatchList() {
		if (selectBatchList == null) {
			selectBatchList = createBatchSelectList();
		}
		return selectBatchList;
	}
	/**See {@link #selectBatchList}. */
	public void setSelectBatchList(List<SelectItem> selectList) {
		selectBatchList = selectList;
	}

	/**See {@link #exportToXls}. */
	public boolean getExportToXls() {
		return exportToXls;
	}
	/**See {@link #exportToXls}. */
	public void setExportToXls(boolean exportToXls) {
		this.exportToXls = exportToXls;
	}

	/**See {@link #weekDay}. */
	public Integer getWeekDay() {
		return weekDay;
	}
	/**See {@link #weekDay}. */
	public void setWeekDay(Integer weekDay) {
		this.weekDay = weekDay;
	}

	/** See {@link #departmentList}. */
	public List<SelectItem> getDepartmentList() {
		if (departmentList == null) {
			departmentList = createDepartmentList();
		}
		return departmentList;
	}

	/** See {@link #timesheetDeptText}. */
	public String getTimesheetDeptText() {
		return timesheetDeptText;
	}
	/** See {@link #timesheetDeptText}. */
	public void setTimesheetDeptText(String timesheetDeptText) {
		this.timesheetDeptText = timesheetDeptText;
		setTimesheetDept(timesheetDeptText);
	}

	/**See {@link #timesheetDept}. */
	public Department getTimesheetDept() {
		return timesheetDept;
	}
	/**See {@link #timesheetDept}. */
	public void setTimesheetDept(String timesheetDeptText) {
		if (timesheetDeptText == null) {
			timesheetDept = null;
		}
		else {
			timesheetDept = DepartmentDAO.getInstance().findById(Integer.valueOf(timesheetDeptText));
		}
	}

}
