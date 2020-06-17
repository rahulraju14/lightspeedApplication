/**
 * TimecardPrintUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFTextStripper;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DailyHours;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.FeatureFlagType;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.type.TimecardSelectionType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.file.PdfUtils;
import com.lightspeedeps.util.report.ReportGenerator;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.approver.ApproverDashboardBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.report.ReportQueries;
import com.lightspeedeps.web.timecard.PrintTimecardBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;

/**
 * Utility methods related to generating the various timecard reports.
 */
public class TimecardPrintUtils {
	private static final Log log = LogFactory.getLog(TimecardPrintUtils.class);

	/**
	 * private constructor -- should not be instantiated.
	 */
	private TimecardPrintUtils() {
	}

	/**
	 * Display the Print Timecards pop-up dialog, with appropriate messaging and
	 * options.
	 *
	 * @param wtc The current WeeklyTimecard, which may be selected by the user
	 *            for printing. Its department name will also be used for the
	 *            "print current department" message in the pop-up.
	 * @param weDate The week-ending date that the report may be related to.
	 *            Optional. This will be ignored if wtc is not null.
	 * @param filterDate The current date selected in the "week ending" filter;
	 *            null if "All" is selected.
	 * @param userHasViewHtg If this is true, the print options dialog box will
	 *            include options to print all timecards and all departments. If
	 *            false, the options for "whole department" and "all crew" are
	 *            not presented in the dialog box.
	 * @param cloneAuth True iff the current user has Clone authority, which will
	 *            also give them access to some multi-person reports, such as
	 *            the Daily Timesheet, but NOT to all reports.
	 * @param holder The PopupHolder to be used for the call-back when the user
	 *            clicks Print/Ok.
	 * @param action The action code to be returned to the holder as a call-back
	 *            parameter.
	 * @param showDisc True iff the "Discrepancy report" option should be
	 *            displayed in the Print dialog box.
	 * @param showAllProjects True iff the user is viewing all projects. If so,
	 *            the prompt dialog will contain additional text about this.
	 * @param allowSelected True if the "Selected timecards" option should be enabled.
	 */
	public static void showPrintOptions(WeeklyTimecard wtc, Date weDate, Date filterDate,
			boolean userHasViewHtg, boolean cloneAuth, PopupHolder holder,
			int action, boolean showDisc, boolean showAllProjects, boolean allowSelected) {
		PrintTimecardBean bean = PrintTimecardBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}
		String dept = "";
		String batch = "";
		String msg = "";
		boolean ownTimecard = false;
		if (wtc != null) {
			dept = wtc.getDeptName();
			bean.setTimecardSelected(true);
			if (wtc.getWeeklyBatch() != null) {
				batch = wtc.getWeeklyBatch().getName();
			}
			msg = "(" + wtc.getFirstName() + " " + wtc.getLastName() + " - " +
					wtc.getOccupation() + ")";
			User user = SessionUtils.getCurrentUser();
			if (user.getAccountNumber().equals(wtc.getUserAccount())) {
				ownTimecard = true;
			}
		}
		else {
			bean.setTimecardSelected(false);
		}
		Production prod = TimecardUtils.findProduction(wtc);
		Project project = TimecardUtils.findProject(prod, wtc);
		bean.show(userHasViewHtg, cloneAuth, showDisc, showAllProjects, prod, project, dept, batch,
				holder, action, "Timecard.Print.Title", null,
				"Timecard.Print.Ok", "Confirm.Cancel", bean.getTimecardSelected(), ownTimecard, allowSelected);
		if (wtc != null) {
			bean.setWeekEndDate(wtc.getEndDate());
			bean.setSecondWeekEndDate(wtc.getEndDate());
		}
		else if (weDate != null) {
			bean.setWeekEndDate(weDate);
			bean.setSecondWeekEndDate(weDate);
		}
		bean.setFilterDate(filterDate);
		bean.setMessage(msg);
	}

	/**
	 * Print a single timecard to a file using the specified timecard report
	 * style. The report will include the Box rental and Mileage forms (if they
	 * exist). The report will not be displayed to the user. This is typically
	 * used when someone submits a timecard, or when the batch transfer facility
	 * is generating individual timecard reports.
	 *
	 * @param wtc The timecard to be printed.
	 * @param reptStyle The style of timecard report to print; this should be
	 *            specified using one of the public static values defined in
	 *            this class.
	 * @param breakdown True iff the Pay breakdown numbers, and other calculated
	 *            values, should be included in the report.
	 * @param forTransfer True iff the report is being generated as part of a
	 *            data transfer, e.g., to a payroll service. This may affect the
	 *            data included on the report.
	 * @return The fully-qualified name of the report file created.
	 */
	public static String printTimecard(WeeklyTimecard wtc, ReportStyle reptStyle, boolean breakdown, boolean forTransfer) {
		String sqlQuery = ReportQueries.timecard;
		String reportName;

		switch(reptStyle) {
			case TC_FULL:
				reportName = ReportBean.JASPER_TIMECARD_1;
				break;
			case  TC_JOBS:
				reportName = ReportBean.JASPER_TIMECARD_2;
				break;
			case TC_AICP:
				sqlQuery = ReportQueries.timecardAicp;
				reportName = ReportBean.JASPER_TIMECARD_AICP;
				break;
			case TC_AICP_TEAM:
				sqlQuery = ReportQueries.timecardAicp;
				reportName = ReportBean.JASPER_TIMECARD_AICP2;
				break;
			case TC_MODEL:
				if (FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_TIMECARD_PRINT)) {
					sqlQuery = ReportQueries.modelTimecardAicp;
					reportName = ReportBean.JAPSER_MODEL_TIMECARD_AICP;
					break;
				}
			case TC_SIMPLE:
			default:
				reportName = ReportBean.JASPER_TIMECARD_3;
				break;
		}

		sqlQuery += " w.id = " + wtc.getId();

		Production prod = null;
		Project project = wtc.getStartForm().getProject();
		if (project != null) {
			prod = project.getProduction();
		}
		else {
			prod = TimecardUtils.findProduction(wtc);
		}

		String printedFile = ReportUtils.generateTimecard(reportName, null, sqlQuery, wtc.getProdId(),
								prod, project,
								true, /* include the timecard itself */
								breakdown, /* optional: include pay breakdown and other calculated values */
								true, /* include the box rental form */
								true, /* include the mileage form */
								true, /* include any attachments */
								forTransfer, /* if the report is part of data transfer or not */
								false,/* do NOT open a browser window on the report file */
								false,/* false for the payroll report export option*/
						null); /* null week ending date*/
		// LS-1382
		if (wtc.getAttachments() != null && (! wtc.getAttachments().isEmpty())) {
			printedFile = appendAttachmentInTcReport(printedFile, true);
		}
		return printedFile;
	}

	/**
	 * Print a list of timecards to a file using the specified timecard report
	 * style. The report will include the Box rental and Mileage forms (if they
	 * exist). The report will not be displayed to the user. This is typically
	 * used by the batch transfer/transmit facility.
	 *
	 * @param prod The Production containing the timecards to be printed.
	 * @param ids A collection of the database ids (WeeklyTimecard.id) of the
	 *            timecards to be printed.
	 * @param reportStyle The style of timecard report to print; this should be
	 *            specified using one of the public static values defined in
	 *            this class.
	 * @param filePrefix An optional string to be used as the first part of the
	 *            generated file name. If null, then the 'reportName' parameter
	 *            is used as the file name prefix instead.
	 * @param sortOrder a list of field names to be used in an "order by"
	 *            statement; this will determine the order of printing of the
	 *            timecards.
	 * @param breakdown True iff the Pay breakdown numbers, and other calculated
	 *            values, should be included in the report.
	 * @param forTransfer True iff the report is being generated as part of a
	 *            data transfer, e.g., to a payroll service. This may affect the
	 *            data included on the report.
	 * @return The fully-qualified name of the report file created.
	 */
	public static String printTimecardList(Production prod, Collection<Integer> ids, ReportStyle reportStyle,
			String filePrefix, String sortOrder, boolean breakdown, boolean forTransfer, Project project) {
		String sqlQuery = ReportQueries.timecard;
		String reportName;
		//Project project = prod.getDefaultProject();

		switch(reportStyle) {
			case TC_FULL:
				reportName = ReportBean.JASPER_TIMECARD_1;
				break;
			case  TC_JOBS:
				reportName = ReportBean.JASPER_TIMECARD_2;
				break;
			case TC_AICP:
				sqlQuery = ReportQueries.timecardAicp;
				reportName = ReportBean.JASPER_TIMECARD_AICP;
				break;
			case TC_AICP_TEAM:
				sqlQuery = ReportQueries.timecardAicp;
				reportName = ReportBean.JASPER_TIMECARD_AICP2;
				break;
			case TC_MODEL:
				if (FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_TIMECARD_PRINT)) {
					sqlQuery = ReportQueries.modelTimecardAicp;
					reportName = ReportBean.JAPSER_MODEL_TIMECARD_AICP;
					break;
				}
			case TC_SIMPLE:
			default:
				reportName = ReportBean.JASPER_TIMECARD_3;
				break;
		}

		sqlQuery += " w.id in ( 0";
		for (Integer id : ids) {
			sqlQuery += "," + id;
		}
		sqlQuery += " ) ";

		if (sortOrder != null) {
			sqlQuery += " order by " + sortOrder;
		}

		return ReportUtils.generateTimecard(reportName, filePrefix, sqlQuery, prod.getProdId(),
				prod, project,
				true, /* include the timecard itself */
				breakdown, /* optional: include pay breakdown and other calculated values */
				true, /* include the box rental form */
				true, /* include the mileage form */
				true, /* include any attachments */
				forTransfer, /* if the report is part of data transfer or not */
				false, /* do NOT open a browser window on the report file */
				false, /* false for the payroll report export option*/
				null); /* null week ending date*/
	}

	/**
	 * Print the timecards as requested by the user. Check the options set in
	 * the PrintTimecardBean, build the appropriate query, and call the
	 * ReportBean to generate the output.
	 *
	 * @param wtc The WeeklyTimecard of interest -- may be used as an exact
	 *            match, or for its account number, or occupation, or department
	 *            Id.
	 * @param project The Project related to the report being printed; currently
	 *            only used for the Batch List report.
	 * @param includeAllProjects True iff timecards from all projects in a
	 *            Commercial production should be included in the report. Should
	 *            also be true if this is a TV/Feature production.
	 * @param openWindow True iff the caller wants the report opened in a
	 *            browser window. False should be passed if the caller just
	 *            needs the report file created, e.g., so that it can be
	 *            emailed. (Currently this is always true.)
	 * @param ids
	 *
	 * @return The fully-qualified name of the report file created, or null if
	 *         the report could not be printed (e.g., if the target timecard was
	 *         deleted).
	 */
	public static String printSelectedReport(WeeklyTimecard wtc, Project project, boolean includeAllProjects, boolean openWindow, Collection<Integer> ids) {

		PrintTimecardBean bean = PrintTimecardBean.getInstance();
		String printedFile = null;
		if (bean.getReportType().equalsIgnoreCase(PrintTimecardBean.TYPE_TIMECARD)) {
			printedFile = printTimecardReport(bean, wtc, includeAllProjects, openWindow, ids, false, false);
		}
		else if (bean.getReportType().equalsIgnoreCase(PrintTimecardBean.TYPE_PAYROLL)) {
			printedFile = printTimecardReport(bean, wtc, includeAllProjects, openWindow, ids, true, false);
		}
		else if (bean.getReportType().equalsIgnoreCase(PrintTimecardBean.TYPE_DAILY_TIMESHEET)) {
			printedFile = printTimecardReport(bean, wtc, includeAllProjects, openWindow, ids, false, true);
		}
		else if (bean.getReportType().equalsIgnoreCase(PrintTimecardBean.TYPE_BATCH_REPORT)) {
			if (includeAllProjects) {
				project = null;
			}
			printedFile = printWeeklyBatchList(bean, project);
		}
		else {
			printedFile = printDiscrepancyReport(bean, openWindow);
		}
		// LS-1382
		if (printedFile != null && (! openWindow)) {
			// this will merge attachments into file and open it in browser window
			printedFile = TimecardPrintUtils.appendAttachmentInTcReport(printedFile, false);
		}
		return printedFile;
	}


	/**
	 * Print the timecards as requested by the user. Check the options set in
	 * the PrintTimecardBean, build the appropriate query, and call the
	 * ReportBean to generate the output.
	 *
	 * @param bean The PrintTimecardBean that has the user-selected print
	 *            options.
	 * @param wtc The WeeklyTimecard of interest -- may be used as an exact
	 *            match, or for its account number, or occupation, or department
	 *            Id.
	 * @param includeAllProjects True iff timecards from all projects in a
	 *            Commercial production should be included in the report. Should
	 *            also be true if this is a TV/Feature production.
	 * @param openWindow True iff the caller wants the report opened in a
	 *            browser window. False should be passed if the caller just
	 *            needs the report file created, e.g., so that it can be
	 *            emailed. (Currently this is always true.)
	 * @param ids
	 * @param isPayrollReport
	 * @param isdailyTimesheet
	 *
	 * @return The fully-qualified name of the report file created, or null if
	 *         the report could not be printed (e.g., if the target timecard was
	 *         deleted).
	 */
	private static String printTimecardReport(PrintTimecardBean bean, WeeklyTimecard wtc,
			boolean includeAllProjects, boolean openWindow, Collection<Integer> ids, boolean isPayrollReport, boolean isdailyTimesheet) {
		wtc = WeeklyTimecardDAO.getInstance().refresh(wtc);
		if (wtc == null) { // timecard was deleted
			return null;
		}
		Date printDate = bean.getWeekEndDate();
		Date secondPrintDate = bean.getSecondWeekEndDate();
		boolean allDates = printDate.equals(Constants.SELECT_ALL_DATE);
		boolean pExport = bean.getExportToXls();
		TimecardSelectionType select = bean.getRangeSelectionType();
		String sortOrder = bean.getSortOrder();

		String sqlQuery = ReportQueries.timecard;
		String reportName = ReportBean.JASPER_TIMECARD_3;

		if (bean.getIncludeTimecard()) {
			if (isPayrollReport) {
				reportName = ReportBean.JASPER_PAYROLL_1;
				if (bean.getReportStyleEnum() == ReportStyle.TC_PAY_BREAKDOWN) {
					reportName = ReportBean.JASPER_PAYROLL_1;
				}
				else {
					reportName = ReportBean.JASPER_PAYROLL_2;
				}
			}
			else if (isdailyTimesheet) {
				reportName = ReportBean.JASPER_DAILY_TIMESHEET;
				select = TimecardSelectionType.ALL;
				sortOrder = PrintTimecardBean.ORDER_NAME;
				sqlQuery = ReportQueries.dailyTimeSheet;
				secondPrintDate = printDate;
				if (printDate != null && bean.getWeekDay() != null) {
					Date dtDate = printDate;
					Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
					cal.setTime(dtDate);
					cal.add(Calendar.DAY_OF_MONTH, -7);
					cal.add(Calendar.DAY_OF_MONTH, bean.getWeekDay());
					String reptDate = "'" + CalendarUtils.formatDate(cal.getTime(), Constants.SQL_DATE_PATTERN) + "'";
					sqlQuery += " dt.date = " + reptDate + " and ";
					sqlQuery += " ((sf.effective_end_date is null) or (sf.effective_end_date >= " + reptDate + ")) and ";
					sqlQuery += " ((sf.work_end_date is null) or (sf.work_end_date >= " + reptDate + ")) and ";
					sqlQuery += " ((sf.work_start_date is null) or (sf.work_start_date <= " + reptDate + ")) and ";
				}
				if (bean.getTimesheetDept() != null) {
					sqlQuery += " w.Department_Id = " + bean.getTimesheetDept().getId() + " and ";
				}
			}
			else {
				if (bean.getReportStyleEnum() == ReportStyle.TC_AICP) {
					reportName = ReportBean.JASPER_TIMECARD_AICP;
					if (ApplicationScopeBean.getInstance().getIsBeta()) {
						reportName = ReportBean.JASPER_TIMECARD_AICP2;
					}
					sqlQuery = ReportQueries.timecardAicp;

				}
				else if ((bean.getReportStyleEnum() == ReportStyle.TC_MODEL) &&
						FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_TIMECARD_PRINT)) {
					sqlQuery = ReportQueries.modelTimecardAicp;
					reportName = ReportBean.JAPSER_MODEL_TIMECARD_AICP;
				}
				else if (bean.getReportStyleEnum() == ReportStyle.TC_FULL) {
					reportName = ReportBean.JASPER_TIMECARD_1;
				}
				else if (bean.getReportStyleEnum() == ReportStyle.TC_JOBS) {
					reportName = ReportBean.JASPER_TIMECARD_2;
				}
			}
		}
		else if (bean.getIncludeBox()) {
			reportName = ReportBean.JASPER_BOX_RENTAL;
			sqlQuery = ReportQueries.boxRental;
		}
		else if (bean.getIncludeMileage()) {
			reportName = ReportBean.JASPER_MILEAGE;
			sqlQuery = ReportQueries.mileage;
		}
		else { // no "include" selected -- shouldn't get this far!
			return null;
		}

		sqlQuery += " w.prod_id = '" + wtc.getProdId() + "' ";

		Project project = null;
		Production prod = null;
		if (! includeAllProjects) {
			project = wtc.getStartForm().getProject();
			if (project != null) {
				sqlQuery += " and sf.project_id = " + project.getId();
				prod = project.getProduction();
			}
			else {
				prod = TimecardUtils.findProduction(wtc);
			}
		}
		else {
			prod = TimecardUtils.findProduction(wtc);
		}

		// Update the query based on the range of timecards selected by the user
		switch(select) {
			case CURRENT:
				sqlQuery += " and w.user_Account = '" + wtc.getUserAccount() + "' " +
						" and w.occupation = '" + (wtc.getOccupation() != null ? wtc.getOccupation().replace("\'", "\\'") : null)+ "' ";
				break;
			case PERSON:
				sqlQuery += " and w.user_Account = '" + wtc.getUserAccount() + "' ";
				break;
			case DEPT:
				sqlQuery += " and w.Department_Id = " + wtc.getDepartmentId();
				// allDates = false;	// allow all dates with department selection (only on Admin UI)
				break;
			case UNBATCHED:
				sqlQuery += " and w.Weekly_Batch_Id is null ";
				break;
			case BATCH:
				if (wtc.getWeeklyBatch() == null) { // not currently used, but best to check
					sqlQuery += " and w.Weekly_Batch_Id is null ";
				}
				else {
					sqlQuery += " and w.Weekly_Batch_Id = " + wtc.getWeeklyBatch().getId();
					allDates = true;	// ensure we print all timecards in the requested batch
				}
				break;
			case SELECT:
				sqlQuery += " and w.id in ( 0";
				for (Integer id : ids) {
					sqlQuery += "," + id;
				}
				sqlQuery += " ) ";
				allDates = true;
				break;
			case ALL:
				// allDates = false;	// allow all dates with "all crew" (only on Admin UI)
				break;
			default:
		}

		if (! allDates) {
			sqlQuery += " and w.end_date between '" + CalendarUtils.formatDate(printDate, Constants.SQL_DATE_PATTERN) + "' and '" + CalendarUtils.formatDate(secondPrintDate, Constants.SQL_DATE_PATTERN) + "' ";
		}

		String status = bean.getStatusSelection();
		if (! status.equals(PrintTimecardBean.STATUS_ALL)) {
			if (status.equals(PrintTimecardBean.STATUS_APPROVE)) {
				sqlQuery += " and (w.status = '" + ApprovalStatus.APPROVED.name() + "'  or " +
						" w.status = '" + ApprovalStatus.LOCKED.name() + "') ";
			}
			else if (status.equals(PrintTimecardBean.STATUS_SUBMIT)) {
				sqlQuery += " and (w.status = '" + ApprovalStatus.SUBMITTED.name() + "'  or " +
						" w.status = '" + ApprovalStatus.RESUBMITTED.name() + "') ";
			}
			else if (status.equals(PrintTimecardBean.STATUS_VOID)) {
				sqlQuery += " and w.status = '" + ApprovalStatus.VOID.name() + "' ";
			}
		}

		String order;
//		String acctSetOrder = " Account_Set, "; // not used in mileage-only or box-only prints
//		if (! bean.getIncludeTimecard()) {
//			acctSetOrder = " ";
//		}
		if (sortOrder.equals(PrintTimecardBean.ORDER_ACCT)) {
			order = " w.Account_Major, w.Account_Dtl, Account_Set, w.last_name, w.first_name, w.Occupation, w.end_date desc ";
		}
		else if (sortOrder.equals(PrintTimecardBean.ORDER_DATE)) {
			order = " w.end_date desc, w.Account_Major, w.Account_Dtl, Account_Set, w.last_name, w.first_name, w.Occupation ";

		}
		else if (sortOrder.equals(PrintTimecardBean.ORDER_DEPT)) {
			order = " w.dept_name, w.Account_Major, w.Account_Dtl, Account_Set, w.last_name, w.first_name, w.Occupation, w.end_date desc ";
		}
		else { // ORDER_NAME
			order = " w.last_name, w.first_name, w.Account_Major, w.Account_Dtl, Account_Set, w.Occupation, w.end_date desc ";
		}

		sqlQuery += " order by " + order;
		log.debug("timecard query=" + sqlQuery);

		return ReportUtils.generateTimecard(reportName, null, sqlQuery, wtc.getProdId(),
				prod, project,
				bean.getIncludeTimecard(), bean.getIncludeBreakdown(),
				bean.getIncludeBox(), bean.getIncludeMileage(), bean.getIncludeAttachments(),
				false, /* the report is NOT part of a data transfer */
				openWindow, pExport, printDate);
	}

	/**
	 * Print the DPR-Timecard discrepancy report. The list of candidate
	 * timecards will be retrieved from the current ApproverDashboardBean. Only
	 * those timecards that have a discrepancy will be printed.
	 * <p>
	 * For Commercial productions, the list may include timecards from more than
	 * one project if the user is an "aggregate viewer".
	 *
	 * @param bean The PrintTimecardBean, which holds some print options used
	 *            during this process.
	 * @param openWindow True if a browser window should be opened on the
	 *            generated PDF.
	 * @return The fully-qualified file name of the generated PDF.
	 */
	private static String printDiscrepancyReport(PrintTimecardBean bean, boolean openWindow) {

		String reportName = ReportBean.JASPER_DISCREPANCY;

		if (bean.getDiscReportType().equals(PrintTimecardBean.DISC_TYPE_SUMMARY)) {
			reportName = ReportBean.JASPER_DISCREPANCY_SUM;
		}

		// Generate list of WeeklyTimecard.id values for timecards to be printed:
		// determine which timecards have discrepancies! get DPR data, compare, etc.

		// Get timecard list from the Approver Dashboard
		ApproverDashboardBean appBean = (ApproverDashboardBean)ServiceFinder.findBean("approverDashboardBean");
		List<TimecardEntry> tces = appBean.getTimecardEntryList();

		/** The list of WeeklyTimecard.id values for all those timecards that should appear
		 * on the discrepancy report. */
		List<Integer> ids = new ArrayList<>();

		/** List of all user.accountNumber values on the DPRs for the week being reported.
		 * Creating this list once is probably the least costly way to be able to determine
		 * if an employee appeared on any relevant DPR even though they were in a department
		 * that is specified as not appearing on the PR. */
		List<String> usersOnDprs = null;

		/** Map of department names to "included on PR" flags, so we only have to find these once. */
		Map<String, Boolean> deptMap = appBean.getDeptPrMap();

		Dpr[] weekOfDprs = appBean.getWeekOfDprs();

		boolean excludeOffProd = bean.getDiscExcludeOffProd();
		boolean excludeMpv = bean.getDiscExcludeMpv();
		boolean excludeNoPr = bean.getDiscExcludeNoPr();
		boolean excludeNonWork = bean.getDiscExcludeNonWork();

		Production prod = SessionUtils.getNonSystemProduction();
		Project currProject = SessionUtils.getCurrentProject();
		Project project = null;
		boolean aicp = prod.getType().hasPayrollByProject();

		/** The week-ending date for the timecard(s) being considered for the report.
		 * This value may change if the user has "All" as the week-ending date filter. */
		Date date = null;

		// For each timecard, see if it has any discrepancy against DPR data
		boolean include = false;
		for (TimecardEntry tce : tces) {
			WeeklyTimecard wtc = tce.getWeeklyTc();
			if (excludeOffProd && wtc.getOffProduction()) {
				continue; // skip off production employees if option selected.
			}
			project = null;
			// find the department -- need the custom one, if there is one
			String deptName = wtc.getDeptName();
			Boolean deptOnPr = deptMap.get(deptName);
			if (deptOnPr == null) {
				wtc = WeeklyTimecardDAO.getInstance().refresh(wtc);
				if (aicp) {
					project = wtc.getStartForm().getProject();
					// For commercial productions, only include TCs from current project
					if (! project.equals(currProject)) {
						continue;
					}
				}
				Department dept = DepartmentDAO.getInstance().findByProductionAndNameAny(prod, project, deptName);
				if (dept == null) { // This can happen if a custom department was renamed.
					dept = wtc.getDepartment();
					dept = DepartmentDAO.getInstance().refresh(dept);
				}
				if (dept != null) {
					deptOnPr = dept.getShowOnDpr();
					deptMap.put(deptName, deptOnPr);
				}
				else {
					deptOnPr = true;
				}
			}
			if (deptOnPr != null) {
				include = deptOnPr; // by default, only include if department is on PR
			}
			if (! include) { // department is off PR; check if person was added to PR
				if (date == null || ! date.equals(wtc.getEndDate())) {
					// new week-ending date
					usersOnDprs = null; // get new set of employees on DPRs
					date = wtc.getEndDate();
				}
				if (usersOnDprs == null) { // first time, or W/E date change, fill in List
					usersOnDprs = ReportTimeDAO.getInstance().findCrewOnDPRs(prod, date);
				}
				if (usersOnDprs.contains(wtc.getUserAccount())) {
					include = true;
				}
			}
			if (include) {
				if (aicp && project == null) {
					wtc = WeeklyTimecardDAO.getInstance().refresh(wtc);
					project = wtc.getStartForm().getProject();
					if (! project.equals(currProject)) {
						continue;
					}
				}
				TimecardCalc.calculateWeeklyTotals(wtc);
				if (hasDiscrepancy(tce, weekOfDprs, prod, project, excludeMpv, excludeNoPr, excludeNonWork)) {
					ids.add(wtc.getId()); // discrepancy: add to print list
				}
			}
		}

		// Create a query matching the list of timecards with discrepancies.
		String idList = "0," ; // won't match; prevents SQL error if "ids" is empty
		for (Integer id : ids) {
			idList += id + ",";
		}
		idList = idList.substring(0, idList.length()-1);

		String order = WeeklyTimecard.getSqlSortKey(appBean.getSortColumnName(), appBean.isAscending());

		String sqlQuery = ReportQueries.discrepancy; // contains {0}, etc., for substitution parameters
		// don't let formatText format id's - it adds commas!
		sqlQuery = MsgUtils.formatText(sqlQuery, ""+prod.getId(), idList, order);

		Date filter = bean.getFilterDate();
		if (filter.equals(Constants.SELECT_ALL_DATE)) {
			filter = null;
		}

		log.debug("timecard query=" + sqlQuery);

		return ReportUtils.generateDiscrepancy(reportName, sqlQuery, "", filter,
				excludeMpv, excludeNoPr, excludeNonWork, tces.size(),
				openWindow);
	}

	/**
	 * Determine if the given timecard has any relevant discrepancies with the
	 * DPRs for the matching time period.
	 *
	 * @param tce The timecardEntry holding the timecard being printed.
	 * @param weekOfDprs Array of 7 DPRs corresponding to the seven days of the
	 *            week. It may contain null entries, in which case the entries
	 *            will get filled with the appropriate DPR as they are found.
	 *            (This avoids the need to search for DPRs for every timecard
	 *            processed.)
	 * @param production The Production associated with the timecard.
	 * @param project The specific Project the DPR should match, or null if
	 *            irrelevant. Should be non-null only for Commercial
	 *            productions.
	 * @param excludeMpv Do not compare MPVs.
	 * @param excludeNoPr Exclude days from consideration when no PR exists.
	 * @param excludeNonWork Exclude non-working days from consideration.
	 * @return True iff there is a discrepancy, and it is not excluded by any of
	 *         the parameters.
	 */
	private static boolean hasDiscrepancy(TimecardEntry tce, Dpr[] weekOfDprs, Production production,
			Project project, boolean excludeMpv, boolean excludeNoPr, boolean excludeNonWork) {
		WeeklyTimecard weeklyTimecard = tce.getWeeklyTc();
		Contact contact = tce.getContact();
		List<DailyHours> dprHours = DprDAO.getInstance().findDailyHours(
				weeklyTimecard.getEndDate(), contact, tce.getOccupation(), project, weekOfDprs);
		int index = 0;

		/** total hours for week from the PR */
		BigDecimal totalDprHours = BigDecimal.ZERO;

		/** true if we should ignore the week's total hours, due to "Worked" checked at least once. */
		boolean ignoreTotal = false;

		/** Set to true if a discrepancy is detected. */
		boolean disc = false;

		for (DailyTime tcDay : weeklyTimecard.getDailyTimes()) {
			if (disc && (ignoreTotal || ! weeklyTimecard.getAllowWorked())) {
				// found a discrepancy, and don't need to keep checking to set "ignoreTotal"
				break;
			}
			DailyHours dprDay = dprHours.get(index++);
			if (excludeNonWork && tcDay.getDayType() != null && (! tcDay.getDayType().getWorkDay())) {
				continue;
			}
			if (dprDay.getDprExists()) {
				if (tcDay.getWorked()) { // just checked "Worked" - no hours recorded
					ignoreTotal = true;
					if ((! dprDay.getContactMatch()) || // Not on PR
							(dprDay.getCallTime() == null) || // missing "O/C"
							(dprDay.getCallTime().signum() >= 0) || // missing "O/C" (or time entered)
							(dprDay.getWrap() != null && dprDay.getWrap().signum() > 0)) { // wrap time entered
							// or PR has call or wrap time specified or does not have "O/C".
						disc = true;
						break;
					}
					continue;
				}
				if (weeklyTimecard.getAllowWorked() && tcDay.getHours() == null) {
					// On-Call worker who did NOT report any hours (and did not check "Worked")
					if ((dprDay.getHours() != null && dprDay.getHours().signum() > 0) || // hours on PR
							(dprDay.getCallTime() != null)) { // or maybe "O/C" on PR
						disc = true;
						// don't break -- need to set "ignoreTotal" if a Worked day occurs
					}
					continue;
				}
				totalDprHours = NumberUtils.safeAdd(totalDprHours, dprDay.getHours());
				if (dprDay.getContactMatch()) {
					if	((NumberUtils.compare(tcDay.getHours(), dprDay.getHours()) != 0) ||
							(NumberUtils.compare(tcDay.getCallTime(), dprDay.getCallTime()) != 0) ||
							(NumberUtils.compare(tcDay.getWrap(), dprDay.getWrap()) != 0)
							) {
						disc = true;
					}
					else if ((tcDay.getCallTime() != null  || tcDay.getWrap() != null ) && (
							// If either TC call or wrap times entered, also compare TC meal times to PR.
							// So we don't compare meal times if no hours were entered on the TC at all.
							(NumberUtils.compare(tcDay.getM1In(), dprDay.getM1In()) != 0) ||
							(NumberUtils.compare(tcDay.getM1Out(), dprDay.getM1Out()) != 0) ||
							(NumberUtils.compare(tcDay.getM2In(), dprDay.getM2In()) != 0) ||
							(NumberUtils.compare(tcDay.getM2Out(), dprDay.getM2Out()) != 0)
							)) {
						disc = true;
					}
					else if (! excludeMpv) {
						if ((NumberUtils.compare(tcDay.getMpv1Payroll(), dprDay.getMpv1()) != 0) ||
								(NumberUtils.compare(tcDay.getMpv2Payroll(), dprDay.getMpv2()) != 0)) {
							disc = true;
						}
					}
				}
				else { // employee not on DPR
					if (tcDay.getHours() != null && tcDay.getHours().signum() > 0) {
						disc = true;
					}
				}
			}
			else { // DPR does not exist
				if (tcDay.reportedWork()) {
					// employee reported hours or worked
					if (excludeNoPr) { // print option selected to exclude "No PR" days
						ignoreTotal = true; // ignore total, almost certainly wrong
					}
					else { // we're not excluding "No PR" days,
						disc = true; // so mark as discrepancy.
					}
				}
			}
		}
		if ((! disc) && (! ignoreTotal) && totalDprHours.compareTo(weeklyTimecard.getTotalHours()) != 0) {
			disc = true;
		}
		return disc;
	}

	/**
	 * Print a weekly batch list report.
	 *
	 * @param bean The PrintTimecardBean that holds the user's selections.
	 * @param project The relevant Project; should be null for non-Commercial
	 *            productions. If null for a Commercial production, then all
	 *            projects will be included.
	 * @return The fully-qualified file name of the generated PDF, or the text
	 *         "MSG" if a message has been issued. A null return indicates that
	 *         the print failed unexpectedly.
	 */
	private static String printWeeklyBatchList(PrintTimecardBean bean, Project project) {
		Date selectedDate = bean.getWeekEndDate();
		Date reportEndDate = selectedDate; // date to pass to report
		String sqlQuery = "";
		String returnMsg = "MSG"; // so caller knows a message was produced.

		// Make sure we have a Saturday W/E date
		Date weekEndDate = TimecardUtils.calculateWeekEndDate(selectedDate, Calendar.SATURDAY);
		// Then calculate the corresponding week-start date
		Calendar cal = CalendarUtils.getInstance(weekEndDate);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		Date weekStartDate = cal.getTime();

		String afterDateCompare;
		String beforeDateCompare;

		if (selectedDate.equals(Constants.SELECT_ALL_DATE)) {
			afterDateCompare = " is not null "; // dummy clause to keep query syntax valid
			beforeDateCompare = " is not null "; // dummy clause to keep query syntax valid
			weekEndDate = null;
			selectedDate = null; // to select all batches
		}
		else {
			// normal case is range of week-start to W/E date
			SimpleDateFormat sfDate = new SimpleDateFormat("''yyyy-MM-dd''");
			afterDateCompare = " >= " + sfDate.format(weekStartDate); // formatted date with primes: '2014-01-21'
			beforeDateCompare = " <= " + sfDate.format(weekEndDate); // formatted date with primes: '2014-01-21'
		}

		String reportName = ReportBean.JASPER_BATCH_LIST;
		String projectIdCompare = " "; // default is to ignore project associations

		Production prod = SessionUtils.getProduction();
		switch (bean.getBatchReportType()) {
			case PrintTimecardBean.ALL_BATCH_TYPE: // print all batches
				if (bean.getAllUnbatchedTimecard()) { // also include unbatched TCs
					// checking if any timecards exist -- if so, they must be either batched or unbatched.
					if (WeeklyTimecardDAO.getInstance().existsWeekEndDate(prod, project,
							selectedDate)) {
						sqlQuery = ReportQueries.AllBatchesList; // query for batched + unbatched
						if (project != null) {
							projectIdCompare = " and sf.project_id in (null, " + project.getId() + ") and " +
									" (aggregate is null or aggregate = false) ";
						}
						// don't let formatText format id's - it adds commas!
						sqlQuery = MsgUtils.formatText(sqlQuery, afterDateCompare, beforeDateCompare,
								"'" + prod.getProdId() + "'", projectIdCompare);
					}
					else {
						MsgUtils.addFacesMessage("WeeklyBatch.Print.NoAllBatches",
								FacesMessage.SEVERITY_ERROR);
						return returnMsg;

					}
				}
				else { // All batches, but NOT unbatched TCs
					if (WeeklyTimecardDAO.getInstance().existsWeekEndDateBatched(prod, project,
							selectedDate)) {
						sqlQuery = ReportQueries.weeklyBatchList; // query for batched timecards only
						if (project != null) {
							projectIdCompare = " and wb.Project_id = " + project.getId() + " and aggregate = false ";
						}
						Integer prodId = SessionUtils.getNonSystemProduction().getId();
						sqlQuery = MsgUtils.formatText(sqlQuery, afterDateCompare, beforeDateCompare,
								"" + prodId, projectIdCompare);
					}
					else {
						MsgUtils.addFacesMessage("WeeklyBatch.Print.NoBatches",
								FacesMessage.SEVERITY_ERROR);
						return returnMsg;
					}
				}
				break;
			case PrintTimecardBean.ONLY_UNBATCHED_TYPE:
				boolean unbatchedFlag = false;
				if (weekEndDate == null) {
					unbatchedFlag = WeeklyTimecardDAO.getInstance().existsUnbatched(prod, project);
				}
				else {
					unbatchedFlag = WeeklyTimecardDAO.getInstance().existsWeekEndDateUnbatched(
							prod, project, weekEndDate);
				}
				if (unbatchedFlag) {
					if (project != null) {
						projectIdCompare = " and sf.project_id = " + project.getId() + "";
					}
					sqlQuery = ReportQueries.onlyUnBatchedList;
					sqlQuery = MsgUtils.formatText(sqlQuery, afterDateCompare, beforeDateCompare,
							"'" + prod.getProdId() + "'",
							projectIdCompare);
				}
				else {
					MsgUtils.addFacesMessage("WeeklyBatch.Print.NoneUnBatchSelected",
							FacesMessage.SEVERITY_ERROR);
					return returnMsg;
				}
				break;
			case PrintTimecardBean.ONLY_BATCH_TYPE: // print a single batch
				WeeklyBatch wb = WeeklyBatchDAO.getInstance().findById(
						Integer.parseInt(bean.getSelectedObject().toString()));
				if (wb.getTimecards().size() > 0) {
					reportEndDate = wb.getEndDate(); // use the batch's date for the header
					sqlQuery = ReportQueries.onlySelectedBatchList;
					sqlQuery = MsgUtils.formatText(sqlQuery, "" + bean.getSelectedObject());
				}
				else {
					MsgUtils.addFacesMessage("WeeklyBatch.Print.NoTimeCardForTheBatch",
							FacesMessage.SEVERITY_ERROR);
					return returnMsg;
				}
				break;

			default:
		}
		log.debug("sqlQuery is : " + sqlQuery);
		if (prod.getType().isAicp()) {
			reportName = ReportBean.JASPER_BATCH_LIST_AICP;
		}
		return ReportUtils.generateWeeklyBatchList(reportName, sqlQuery, "", reportEndDate, true);
	}

	/**
	 * @param weeklyTimecard
	 * @param res
	 * @param project
	 * @param userId
	 *//*
	public static void mergeAttachments(WeeklyTimecard weeklyTimecard, String res, Project project, Integer userId) {
		log.debug("Attachments =" + weeklyTimecard.getAttachments().size());
		if (res != null) {
			String projectId = "";
			if (project != null) {
				projectId = "_" + project.getId();
			}
 			String resFile = printTimecardWithAttachments(weeklyTimecard, projectId, res, userId);
 			log.debug("resFile = " + resFile);
 			if (resFile != null) {
 				ReportGenerator.openFile(resFile);
 			}
		}
	}

	*//** Prints and merges all the Timecards and Attachments of a group.
	 * @param weeklyTimecard
	 * @param projectId
	 * @param fileNamePdf
	 * @param userId
	 * @return
	 *//*
	private static String printTimecardWithAttachments(WeeklyTimecard weeklyTimecard, String projectId,
				String fileNamePdf, Integer userId) {
		log.debug("");
		Integer badAttachmentCount = 0;
		List<String> fileList = new ArrayList<>();
		Production production = SessionUtils.getNonSystemProduction();
		String realReportPath = SessionUtils.getRealReportPath();
		DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
		String timestamp = df.format(new Date());
		timestamp += ApplicationUtils.findNextSequenceNumber();
		String attachmentOutputFileName = production.getProdId() +
				projectId + "_" + userId + "_" + "Attachments" + "_" + timestamp + ".pdf";
		log.debug("Print and attach Attachments");
		badAttachmentCount = TimecardPrintUtils.printAttachments(weeklyTimecard, userId, projectId, timestamp, attachmentOutputFileName);
		// TODO DH: What if some attachments are  not printed (badAttachmentCount > 0)

		String outputFileName = (fileNamePdf.substring(fileNamePdf.lastIndexOf("/") + 1).replace(".pdf", "_"))
				+ "with_attachments" + ".pdf";
		if (badAttachmentCount == 0 || (! badAttachmentCount.equals(weeklyTimecard.getAttachments().size()))) {
			fileList.add(fileNamePdf);
			fileList.add(realReportPath + attachmentOutputFileName);
			log.debug("outputFileName = " + outputFileName);
			boolean finalPdf = PdfUtils.combinePdfs(fileList, (realReportPath + outputFileName));
			log.debug("finalPdf = " + finalPdf);
		}
		return outputFileName;
	}*/

	/**
	 * Print all the attachments for a given WeeklyTimecard.
	 *
	 * @param wtc The WeeklyTimecard whose attachments should be printed.
	 * @param fileQualifier This is used to qualify the filenames of
	 *            intermediate files created during printing. It should be
	 *            unique to the timecard and user printing.
	 * @param attachmentOutputFileName Name of the final merged PDF (to be
	 *            created) containing all the attachments.
	 * @return A count of the number of attachments which failed during
	 *         printing, 0-n. If no attachments could be printed, returns -1.
	 */
	private static Integer printAttachments(WeeklyTimecard wtc, String fileQualifier, String attachmentOutputFileName) {
		int attNum = 0;
		Integer badAttachmentCount = 0;
		List<String> fileList = new ArrayList<>();
		String realReportPath = SessionUtils.getRealReportPath();
		for (Attachment attach : wtc.getAttachments()) {
			attach = AttachmentDAO.getInstance().refresh(attach);
			attNum++; // Each attachment needs a unique filename - use attachment number
			String atcFileNamePdf = fileQualifier + attNum + ".pdf";
			if (attach.getMimeType().isPdf()) {
				boolean print = DocumentService.printAttachment(attach, atcFileNamePdf);
				if (print) {
					fileList.add(realReportPath + atcFileNamePdf);
				}
				else {
					badAttachmentCount++;
				}
			}
			else if (attach.getMimeType().isImage()) {
				boolean print = false;
				try {
					print = DocumentService.printImageAttachment(attach, atcFileNamePdf, false);
				}
				catch (Exception e) {
					EventUtils.logError(e);
					MsgUtils.addGenericErrorMessage();
				}
				if (print) {
					fileList.add(realReportPath + atcFileNamePdf);
				}
				else {
					badAttachmentCount++;
				}
			}
		}
		int finalPdf = PdfUtils.combinePdfs(fileList, (realReportPath + attachmentOutputFileName));
		log.debug("finalPdf = "  + finalPdf);
		if (finalPdf != 0) {
			String insert = wtc.getFirstName() + " " + wtc.getLastName(); // LS-2889: add failing timecard info
			MsgUtils.addFacesMessage("Timecard.Print.AttachmentError", FacesMessage.SEVERITY_WARN, insert);
			badAttachmentCount = finalPdf; // will be negative if all attachments failed
		}
		return badAttachmentCount;
	}

	/**
	 * Method appends (inserts) attachments after their respective timecard
	 * within the given PDF.
	 *
	 * @param printedFileName Fully-qualified name of the file containing the
	 *            printed timecards. This file may contain pages that are "place
	 *            holders" for the attachments of a particular timecard. The
	 *            place-holder page follows the printed timecard, and contains
	 *            the text "ATTACHMENTS_<id>_...", where <id> is the
	 *            weeklyTimeCard.id value of the timecard whose attachments
	 *            should be inserted at that point. (The inserted attachment
	 *            replaces the place-holder page.)
	 * @param forTransfer If FALSE, the merged PDF will be opened in a new
	 *            browser window. If TRUE, it will not be opened (which is the
	 *            case if the file is being created as part of a transfer
	 *            operation -- so the file will be emailed or delivered
	 *            otherwise, and is therefore not opened in the browser).
	 * @return The name of the final output file containing the merged output.
	 */
	@SuppressWarnings("unchecked")
	public static String appendAttachmentInTcReport(String printedFileName, boolean forTransfer) {
		try {
			log.debug("Original FileName = " + printedFileName);
			String realReportPath = SessionUtils.getRealReportPath();
			Integer userId = SessionUtils.getCurrentUser().getId();
			//Integer badAttachmentCount = 0;
			DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
			String timestamp = df.format(new Date());
			timestamp += ApplicationUtils.findNextSequenceNumber();
			String attachmentOutputFileNameBase = "Att_U#" + userId + "_" + timestamp;
			PDDocument tcReport = PDDocument.load(printedFileName);
			List<PDPage> tcList = tcReport.getDocumentCatalog().getAllPages();
			PDDocument mergeDoc = new PDDocument();
			List<PDDocument> attachmentDocs = new ArrayList<>();
			for (int pageNumber = 1; pageNumber <= tcReport.getNumberOfPages(); pageNumber++) {
				PDFTextStripper s = new PDFTextStripper();
				s.setStartPage(pageNumber);
				s.setEndPage(pageNumber);
				String contents = s.getText(tcReport);
				int placeHolderIx = contents.indexOf(Constants.ATTACHMENT);
				if (placeHolderIx >= 0) {
					// Replace current page in report with the attachments.
					String placeHolder = contents.substring(placeHolderIx + Constants.ATTACHMENT.length()+1);
					Integer tcId = Integer.parseInt(placeHolder.substring(0, placeHolder.indexOf("_")));
					WeeklyTimecard wtc = WeeklyTimecardDAO.getInstance().findById(tcId);
					wtc = WeeklyTimecardDAO.getInstance().refresh(wtc);
					if (wtc != null && wtc.getAttachments() != null && (! wtc.getAttachments().isEmpty())) {
						String filenameQualifier = tcId + "_" +  attachmentOutputFileNameBase;
						String attachmentOutputFileName = filenameQualifier + ".pdf";
						int badAttachments = printAttachments(wtc, filenameQualifier + "_A", attachmentOutputFileName);
						if (badAttachments >= 0) {
							try {
								PDDocument attachment = PDDocument.load(realReportPath + attachmentOutputFileName);
								attachmentDocs.add(attachment);
								List<PDPage> attachList = attachment.getDocumentCatalog().getAllPages();
								for (PDPage atcPage : attachList) {
									//log.debug("ready to addPage");
									mergeDoc.addPage(atcPage);
								}
							}
							catch (Exception e) {
								EventUtils.logError(e);
								String insert = wtc.getFirstName() + " " + wtc.getLastName(); // LS-2889: add failing timecard info
								MsgUtils.addFacesMessage("Timecard.Print.AllAttachmentsError", FacesMessage.SEVERITY_WARN, insert);
							}
						}
						else { // print of all attachments failed. LS-2889
							mergeDoc.addPage(tcList.get(pageNumber-1));
						}
					}
 				}
				else {
					mergeDoc.addPage(tcList.get(pageNumber-1));
				}
			}
			String fileName = printedFileName.replace(realReportPath, "");
			fileName = fileName.replace(".pdf", "");
			fileName += "_Atts.pdf"; // this will be output filename
			String newFile = realReportPath + fileName; // Fully-qualified filename for output
			mergeDoc.save(newFile);
			mergeDoc.close();
			log.debug("FILE CREATED = " + newFile);
			for (PDDocument doc : attachmentDocs) {
				// NOTE! the attachmentDoc PDDocument(s) canNOT be closed until
				// the mergeDoc (final output PDDocument) has been closed.
				doc.close();
			}
			tcReport.close(); // close the source file document
			if (! forTransfer) {
				ReportGenerator.openFile(fileName);
			}
			return newFile;
		}
		catch (IOException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		catch (COSVisitorException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return printedFileName;
	}

	/** Utility method to clean the timecard report
	 * if user don't wants to print Attachments along with it.
	 * @param printedFileName
	 */
	@SuppressWarnings("unchecked")
	public static void cleanTcReport(String printedFileName) {
		try {
			log.debug("Original FileName = " + printedFileName);
			String realReportPath = SessionUtils.getRealReportPath();
			PDDocument tcReport = PDDocument.load(printedFileName);
			List<PDPage> tcList = tcReport.getDocumentCatalog().getAllPages();
			PDDocument mergeDoc = new PDDocument();
			String fileName = printedFileName.replace(realReportPath, "");
			fileName = "TC_" + fileName;
			for(int pageNumber = 1; pageNumber <= tcReport.getNumberOfPages(); pageNumber++) {
				PDFTextStripper s = new PDFTextStripper();
				s.setStartPage(pageNumber);
				s.setEndPage(pageNumber);
				String contents = s.getText(tcReport);
				if(! contents.contains(Constants.ATTACHMENT)) {
					mergeDoc.addPage(tcList.get(pageNumber-1));
 				}
			}
			mergeDoc.save(realReportPath + fileName);
			mergeDoc.close();
			log.debug("FILE CREATED = " + (realReportPath + fileName));
			ReportGenerator.openFile(fileName);
		}
		catch (IOException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		catch (COSVisitorException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

}
