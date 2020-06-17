/**
 * File: ReportUtils.java
 */
package com.lightspeedeps.util.report;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.report.ReportQueries;
import com.lightspeedeps.web.script.ScriptPageBean;

/**
 * Contains static methods related to report generation.  These are used
 * by ReportBean as well as other classes that need to create reports.
 */
public class ReportUtils {
	private static final Log log = LogFactory.getLog(ReportUtils.class);

	private ReportUtils() {
		// prevent instantiation
	}

	/**
	 * Create our standard watermark string, based on the User's
	 * name or Contact name (if available) and today's date.
	 */
	public static WaterMark createWaterMark(User pUser, boolean includeDate) {
		WaterMark waterMark = new WaterMark(createWaterMarkText(pUser, includeDate));
		return waterMark;
	}

	public static WaterMark updateWaterMark(WaterMark waterMark, User pUser) {
		waterMark.setText(createWaterMarkText(pUser, waterMark.isIncludeDate()));
		return waterMark;
	}

	private static String createWaterMarkText(User pUser, boolean includeDate) {
		String name = pUser.getFirstNameLastName();
		if (name == null || name.length() == 0) {
			name = pUser.getEmailAddress();
		}
		String text = name;
		if (includeDate) {
			DateFormat df = new SimpleDateFormat(Constants.WATERMARK_DATE);
			text += " " + df.format(new Date());
		}
		return text;
	}


	/**
	 * Generate a PDF of part of a Script for "advance script" notification
	 * purposes. This code duplicates some portions of other script-related
	 * methods (e.g., setupScript()), as we wanted it to be a static method,
	 * which makes it much easier to call from a batch (Quartz-scheduled) task.
	 *
	 * @param reporter The ScriptReporter object instantiated by the caller,
	 *            which can be called to fill in the script_report table with
	 *            the data to be printed.
	 * @param proj The project related to this request.
	 * @param u The Unit related to this request.
	 * @return The fully-qualified file name of the generated PDF.
	 */
	public static String generateAdvanceScript(ScriptReporter reporter,
			Project proj, Unit u) {
		DateFormat df = new SimpleDateFormat(ReportBean.REPORT_ID_DATE_STYLE);
		String report_id = "U" + u.getId() + "-" + df.format(new Date());
		log.debug("report_id=" + report_id);

		reporter.createReport(report_id); // generate the data to be printed

		String sqlQuery = ReportQueries.scriptPages;
		sqlQuery += " where report_id = '" + report_id + "' ";
		sqlQuery += " order by page_number, line_number; ";

		String rptName;
		if (reporter.getSidesStyle()) {
			rptName = "scriptsides1";
		}
		else {
			rptName = "script";
		}
		Map<String, Object> parms = new HashMap<>();
		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);
		parms.put("reportid", report_id);
		parms.put("colorStyle", ScriptPageBean.PRINT_COLOR_ALL); // page coloring
		String filename = ReportGenerator.generateReport(proj, u, parms, rptName, null,
				null, false, false, null); // generates the PDF
		return filename; // the fully-qualified PDF file name
	}


	/**
	 * Generate a single WeeklyTimecard report.
	 *
	 * @param report the name of the Jasper report file to use.
	 * @param filePrefix An optional string to be used as the first part of the
	 *            generated file name. If null, then the 'reportName' parameter
	 *            is used as the file name prefix instead.
	 * @param sqlQuery the SQL query to pass to the Jasper report engine.
	 * @param prodId The Production.prodId of the timecard being printed.
	 * @param prod The Production of the timecard being printed.
	 * @param project The Project of the timecard being printed, for Commercial
	 *            productions.
	 * @param includeTc True iff the Timecard should be included in the report.
	 * @param includeBreakdown True iff the Pay Breakdown lines, and other
	 *            calculated values should be included in the printed output.
	 * @param includeBox True iff the Box Rental forms should be included in the
	 *            report.
	 * @param includeMiles True iff the Mileage forms should be included in the
	 *            report.
	 * @param includeAttachments True iff any attachments to the timecard should
	 *            be included in the report.
	 * @param forTransfer True iff the report is being generated as part of a
	 *            data transfer, e.g., to a payroll service. This may affect the
	 *            data included on the report.
	 * @param openWindow True iff the caller wants the report opened in a
	 *            browser window. False should be passed if the caller just
	 *            needs the report file created, e.g., so that it can be
	 *            emailed.
	 * @param pExport True iff the caller wants to create a cvs file.
	 * @param weekEndDate - the week ending for this timecard
	 * @return The fully-qualified name of the report file created.
	 */
	public static String generateTimecard(String report, String filePrefix, String sqlQuery,
			String prodId, Production prod, Project project, boolean includeTc, boolean includeBreakdown,
			boolean includeBox, boolean includeMiles, boolean includeAttachments, boolean forTransfer,
			boolean openWindow, boolean pExport, Date weekEndDate) {
		Map<String, Object> parms = new HashMap<>();

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);

		parms.put("sfDAO", StartFormDAO.getInstance());

		parms.put("weekEndDate", weekEndDate);

		parms.put("wbDAO", WeeklyBatchDAO.getInstance());

		if (prod == null) {
			prod = ProductionDAO.getInstance().findByProdId(prodId);
			if (prod == null) { // shouldn't happen!
				prod = ProductionDAO.getSystemProduction();
			}
		}

		if (project == null) {
			project = SessionUtils.getCurrentProject();
			if (project == null || project.getId() == 1 /*SYSTEM project*/) {
				project = prod.getDefaultProject();
			}
		}

		if (prod.getType().isAicp()) {
			parms.put("aicp", true);
			parms.put("hideTcGuarantee", project.getPayrollPref().getHideTcGuarantee());
			parms.put("hideTcGuarantee", project.getPayrollPref().getHideTcGuarantee());
			parms.put("mayUseOnCall", Boolean.FALSE);
			String weekDay = Constants.WEEKDAY_LONG_NAMES.get(project.getPayrollPref().getFirstWorkWeekDay()-1);
			parms.put("weekStartDay", weekDay);
		}
		else {
			parms.put("aicp", false);
			parms.put("hideTcGuarantee", prod.getPayrollPref().getHideTcGuarantee());
			parms.put("mayUseOnCall", prod.getPayrollPref().getShowOnCallFields());
		}

		// Flag for TEAM only processing
		boolean teamPayroll = false;
		PayrollService service = prod.getPayrollPref().getPayrollService();
		if(service  != null) {
			teamPayroll = prod.getPayrollPref().getPayrollService().getTeamPayroll();
			// 	LS-3025 Only show full ssn if timecard is being send to biller via transfer.
			if ((! openWindow) && (! pExport) && service.getSendBatchMethod().includeSSN() && forTransfer) {
				parms.put("fullSsn", true);
			}
		}
		parms.put("teamPayroll", teamPayroll);

		String customBoxRental = "";
		String customMileage = "";
		if( service != null) {
			customBoxRental = service.getBoxRentalText();
			customMileage = service.getOtherText();
		}
		parms.put("customBoxRental", customBoxRental);
		parms.put("customMileage", customMileage);
		
		if (forTransfer) {
			parms.put("showDob", true); // include date-of-birth on report
		}
		else {
			parms.put("showDob", false); // do NOT include date-of-birth on report
		}

		parms.put("includeBreakdown", includeBreakdown);
		parms.put("includeMiles", includeMiles);
		parms.put("includeBoxRental", includeBox);
		parms.put("includeAttachments", includeAttachments);

		parms.put("dayTypeSqlCase", DayType.getSqlCase());
		parms.put("workZoneSqlCase", WorkZone.getSqlCase());
		parms.put("isHybrid", prod.getPayrollPref().getIncludeTouring());

		String genCompleteFilePath = ReportGenerator.generateReport(project, null, parms, report,
				filePrefix, null, openWindow, pExport, null);

		return genCompleteFilePath;
	}

	/**
	 * Generate the DPR-Time card discrepancy report.
	 *
	 * @param report
	 * @param sqlQuery
	 * @param prodId
	 * @param filterDate
	 * @param excludeMpv
	 * @param excludeNoPr
	 * @param excludeNonWork
	 * @param totalTcs
	 * @param openWindow
	 * @return The fully-qualified file name of the generated PDF.
	 */
	public static String generateDiscrepancy(String report, String sqlQuery,
			String prodId, Date filterDate,
			boolean excludeMpv, boolean excludeNoPr, boolean excludeNonWork,
			int totalTcs, boolean openWindow) {
		Map<String, Object> parms = new HashMap<>();

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);

		parms.put("sfDAO", StartFormDAO.getInstance());

		Production prod = SessionUtils.getNonSystemProduction();
		if (prod == null) {
			prod = ProductionDAO.getInstance().findByProdId(prodId);
			if (prod == null) { // shouldn't happen!
				prod = ProductionDAO.getSystemProduction();
			}
		}

		Project proj = SessionUtils.getCurrentProject();
		if (proj == null) {
			proj = prod.getDefaultProject();
		}

		parms.put("aicp", prod.getType().isAicp());

		parms.put("excludeMpv", excludeMpv);
		parms.put("excludeNoPr", excludeNoPr);
		parms.put("excludeNonWork", excludeNonWork);
		parms.put("weekEndDate", filterDate);
		parms.put("totalTcs", totalTcs);

		String genCompleteFilePath = ReportGenerator.generateReport(proj, null, parms, report,
				null, null, openWindow, false, null);

		return genCompleteFilePath;
	}

	/**
	 * Generate a report listing all the timecards arranged within
	 * their weekly batches.
	 *
	 * @param report
	 * @param sqlQuery
	 * @param prodId
	 * @param weekEndDate
	 * @param openWindow
	 * @return The fully-qualified file name of the generated PDF.
	 */
	public static String generateWeeklyBatchList(String report, String sqlQuery,
			String prodId, Date weekEndDate, boolean openWindow) {
		Map<String, Object> parms = new HashMap<>();

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);

		Project proj = SessionUtils.getCurrentProject();
		if (proj == null) {
			Production prod = SessionUtils.getNonSystemProduction();
			if (prod == null) {
				prod = ProductionDAO.getInstance().findByProdId(prodId);
				if (prod == null) { // shouldn't happen!
					prod = ProductionDAO.getSystemProduction();
				}
			}
			proj = prod.getDefaultProject();
		}

		parms.put("weekEndDate", weekEndDate);

		String genCompleteFilePath = ReportGenerator.generateReport(proj, null, parms, report,
				null, null, openWindow, false, null);

		return genCompleteFilePath;
	}

	/**
	 * Generate either an xls or pdf report of the Hot Costs Daily Entry
	 *
	 * @param report
	 * @param sqlQuery
	 * @param dayDate
	 * @param weekEndDate
	 * @param prod
	 * @param project
	 * @param openWindow
	 * @param export
	 * @return
	 */
	public static String generateHotCostsDaily(String report, String sqlQuery, Date dayDate,
			Date weekEndDate, Production prod, Project project, boolean openWindow,
			boolean export) {
		String genCompleteFilePath;
		Map<String, Object> parms = new HashMap<>();

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);
		parms.put("dayDate", dayDate);
		parms.put("weekEndDate", weekEndDate);
		parms.put("prodId", prod.getProdId());
		parms.put("productionName", prod.getTitle());
		parms.put("projectName", project.getTitle());
		parms.put("prodInternalId", prod.getId());

		genCompleteFilePath = ReportGenerator.generateReport(project, null, parms, report, null,
				null, openWindow, export, null);

		return genCompleteFilePath;
	}

	/**
	 * Generate
	 * @param report
	 * @param sqlQuery
	 * @param weekEndDate
	 * @param prod
	 * @param project
	 * @param openWindow
	 * @param export
	 * @return
	 */
	public static String generateHotCostsSummaryView(String report, String sqlQuery,
			Date weekEndDate, Production prod, Project project, boolean openWindow,
			boolean export) {

		String genCompleteFilePath;
		Map<String, Object> parms = new HashMap<>();

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);
		parms.put("weekEndDate", weekEndDate);
		parms.put("prodId", prod.getProdId());
		parms.put("productionName", prod.getTitle());
		parms.put("projectName", project.getTitle());
		parms.put("prodInternalId", prod.getId());

		genCompleteFilePath = ReportGenerator.generateReport(project, null, parms, report, null,
				null, openWindow, export, null);

		return genCompleteFilePath;
	}

	public static String generateTaxWageAllocationReport(Project project, String reportName, String sqlQuery,
			 String filePrefix, boolean openWindow, boolean export, Integer formId, BigDecimal runningTotal) {

		String genCompleteFilePath;
		Map<String, Object> parms = new HashMap<>();
		sqlQuery = sqlQuery.replace("?", formId.toString());

		ResourceBundle bundle = ResourceBundle.getBundle("com.lightspeedeps.util.app.messageResources");

		parms.put(ReportBean.PARAMETER_QUERY, sqlQuery);
		parms.put("twafDAO", TaxWageAllocationFormDAO.getInstance());
		parms.put("formId", formId);
		parms.put("runningTotal", runningTotal);
		parms.put("imagePath", "../i");
		parms.put("REPORT_RESOURCE_BUNDLE", bundle);

		genCompleteFilePath = ReportGenerator.generateReport(project, null, parms, reportName, filePrefix,
				null, openWindow, export, null);

		return genCompleteFilePath;
	}

	/** This string is used in a replaceAll() call to replace all
	 * occurrences of these characters:<br>
	 *  |?"<>/:*\
	 * <br> with underscores.*/
	private static final String INVALID_FILENAME_CHARS_REGEXP = "\\||\\?|\"|<|>|/|:|\\*|\\\\";

	/**
	 * Given a proposed file name, replace any characters that are not valid for
	 * a (Windows or Linux) file name with underscores, and then add a PDF suffix if it
	 * does not already have one.
	 *
	 * @param fileName The file name to be validated and possibly modified.
	 * @return The resulting file name which is a legal Windows file name with a
	 *         PDF extension.  The result should also be a legal file name for Linux
	 *         systems, which are less restrictive.
	 */
	public static String makePdfName(String fileName) {
		String ext = "." + MimeType.PDF.getExtension().toLowerCase();
		fileName = fileName.trim().replaceAll(INVALID_FILENAME_CHARS_REGEXP, "_");
		if (! fileName.toLowerCase().endsWith(ext)) {
			fileName += ext;
		}
		return fileName;
	}

}
