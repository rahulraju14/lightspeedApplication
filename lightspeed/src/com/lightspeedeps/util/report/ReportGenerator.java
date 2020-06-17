/*
 * File: ReportGenerator.java
 */
package com.lightspeedeps.util.report;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.engine.SessionFactoryImplementor;

import com.lightspeedeps.model.*;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.PdfUtils;
import com.lightspeedeps.web.view.View;

import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.*;

/**
 * This class interfaces to the Jasper Report package. It is called by the other
 * report beans with a partially-initialized set of parameters. This code fills
 * in all the shared parameters, invokes the Jasper methods to 'fill' and
 * 'export' the report, thereby generating an external file. It will also,
 * optionally, queue the JavaScript call to open the file in a new browser
 * window.
 */
public class ReportGenerator  {
	private static final Log log = LogFactory.getLog(ReportGenerator.class);


	private ReportGenerator() {
	}

//	public static ReportGenerator getInstance() {
//		return new ReportGenerator();
//	}

	/**
	 * Generate a report as a PDF or XLS file, and optional add a JavaScript
	 * command to the output stream to open the file in a new browser window.
	 * The report file is created using JasperSoft, and is stored in the
	 * REPORT_FOLDER folder of the web application. A unique filename is
	 * generated for the report, based on the reportName and the current time of
	 * day.
	 *
	 * @param project The project whose name will be added to the parameters
	 *            passed to Jasper. This may NOT be null.
	 * @param unit The Unit whose name and number will be added to the
	 *            parameters passed to Jasper. May be null.
	 * @param parameters A Map (of String to Object) for all the parameters
	 *            passed to the Jasper software. This typically contains several
	 *            value pairs when it is passed, and this code adds additional
	 *            parameters that are common to most of the reports, such as the
	 *            current Project's title. Must not be null, but may be empty.
	 *            It will be cleared before we return.
	 * @param reportName The Jasper report name -- the name of the ".jasper"
	 *            file in the reporttemplates directory.
	 * @param filePrefix An optional string to be used as the first part of the
	 *            generated file name. If null, then the 'reportName' parameter
	 *            is used as the file name prefix instead.
	 * @param waterMark A WaterMark to be applied to a completed PDF report; if
	 *            null, no watermark is added to the file.
	 * @param openReportPopupWindow True if the file should be opened in a
	 *            browser window, in which case this method will add the
	 *            appropriate JavaScript call to the outbound stream.
	 * @param pExport True if an XLS file should be created instead of a PDF.
	 * @param fileNamePdf
	 *
	 * @return The real (OS) fully-qualified name of the generated PDF or XLS
	 *         file. null is returned if the report generation failed.
	 */
	public static String generateReport(Project project, Unit unit, Map<String, Object> parameters, String reportName,
			String filePrefix, WaterMark waterMark, boolean openReportPopupWindow, boolean pExport, String fileNamePdf) {

		String genCompleteFilePath = null; // returned file path
		Connection conn = null;
		try {
			InputStream reportLogo;
			InputStream prodLogo;
			// Set parameters common to multiple reports
			Production prod = project.getProduction();
			parameters.put("productionName", prod.getTitle());
			parameters.put("productionId", prod.getId());
			parameters.put("productionTimeZone", prod.getTimeZone());
			parameters.put("todaysDate", new Date());
			parameters.put("isHybrid", prod.getPayrollPref().getIncludeTouring());

			if (prod.getPayrollPref().getPayrollService() != null) {
				PayrollService service = prod.getPayrollPref().getPayrollService();
				parameters.put("teamPayroll", service.getTeamPayroll());
				if (service.getReportLogo() != null) {
					Image image = service.getReportLogo();
					reportLogo = new ByteArrayInputStream(image.getContent());
					parameters.put("reportLogo", reportLogo);
				}
			}

			if (prod.getLogo() != null) {
				prodLogo = new ByteArrayInputStream(prod.getLogo().getContent());
				parameters.put("productionLogo", prodLogo);
			}

			if (prod.getType().getEpisodic()) { // multiple projects/episodes
				parameters.put("projectName", project.getTitle());
			}
			else { // feature film, etc.
				parameters.put("projectName", null);
			}
			parameters.put("projectId", project.getId());

			User user = SessionUtils.getCurrentUser();
			if (user != null) {
				parameters.put("userName",  SessionUtils.getCurrentUser().getDisplayName());
			}
			else {
				parameters.put("userName",  "");
			}

			if (project.getHasUnits()) {
				parameters.put("showUnit", true);
				if (unit == null ) {
					parameters.put("unitNumber", 0);
					parameters.put("unitName", "All Units");
					parameters.put("unitId", 0);
				}
				else {
					parameters.put("unitNumber", unit.getNumber());
					parameters.put("unitName", unit.getName());
					parameters.put("unitId", unit.getId());
				}
			}
			else {
				parameters.put("showUnit", false);
				parameters.put("unitNumber", null);
				parameters.put("unitName", null);
				parameters.put("unitId", null);
			}

			String reportsTemplate = SessionUtils.getRealPath("reportstemplate");
			String gnrtdReportsPath = SessionUtils.getRealReportPath();

			parameters.put("imagesPath", SessionUtils.getRealPath("i"));
			parameters.put("reportsTemplate", reportsTemplate);

			String reportFileName = createFilename(user, project, null);
			if (filePrefix != null) {
				reportFileName = StringUtils.cleanFilename(filePrefix) + reportFileName;
			}
			else {
				reportFileName = reportName + reportFileName;
			}
			String jasperPath = reportsTemplate + File.separator + reportName;
			if (pExport) {
				jasperPath += "Xls";
			}
			jasperPath += ".jasper";
			log.debug("jasper path=" + jasperPath);
			//log.debug("report path=" + gnrtdReportsPath);
			log.debug("parameters=" + parameters);

			conn = getConnection();
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperPath, parameters, conn);
			log.debug("report filled");

			if (pExport) { // Export data to Excel format
				reportFileName += ".xls";
				// Some date/time formats are not handled properly by JRXlsExporter -- override them:
				Map<String,String> formatMap = new HashMap<>();
				formatMap.put("h:mm a", "h:mm AM/PM");
				formatMap.put("EEE, MMMM dd, yyyy", "ddd, mmmm dd, yyyy");

				OutputStream outputStream = new FileOutputStream(new File(gnrtdReportsPath + reportFileName));
				JRXlsExporter exporterXLS = new JRXlsExporter();
				exporterXLS.setParameter(JRXlsExporterParameter.JASPER_PRINT, jasperPrint);
				exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, outputStream);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_COLUMNS, Boolean.FALSE);
				exporterXLS.setParameter(JRXlsExporterParameter.FORMAT_PATTERNS_MAP, formatMap);

/*		        JExcelApiExporter exporterXLS = new JExcelApiExporter();
				exporterXLS.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporterXLS.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
				exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
*/
				exporterXLS.exportReport();
			}
			else { // output data in PDF format
				String fileName = "";
				if (fileNamePdf != null) {
					fileName = fileNamePdf;
					reportFileName = fileNamePdf;
					gnrtdReportsPath = SessionUtils.getRealReportPath();
				}
				else {
					fileName = reportFileName; // without extension
					reportFileName += ".pdf";
				}

			//	JasperExportManager.exportReportToPdfFile(jasperPrint, gnrtdReportsPath + reportFileName);
				// Set up exporter ourselves so we can override compression setting.
				JRPdfExporter exporter = new JRPdfExporter();
				exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
				exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, gnrtdReportsPath + reportFileName);
				// With the Deflater memory leak fixed, we can turn on compression:
				exporter.setParameter(JRPdfExporterParameter.IS_COMPRESSED, Boolean.TRUE);
				exporter.exportReport();

				if (waterMark != null) {
					PdfUtils.addWatermark(gnrtdReportsPath + reportFileName,
							gnrtdReportsPath + fileName + "W.pdf", waterMark, false);
					File f = new File(gnrtdReportsPath + reportFileName);
					if (f.exists()) {
						if ( ! f.delete()) {
							log.error("Report file delete failed for '" + f.getAbsolutePath() + "'");
							EventUtils.logError("Report file delete failed for '" + f.getAbsolutePath() + "'");
						}
						else {
							log.debug("Report file deleted OK: '" + f.getAbsolutePath() + "'");
						}
					}
					reportFileName = fileName + "W.pdf";
				}
			}
			genCompleteFilePath = gnrtdReportsPath + reportFileName; // used by report distribution code
			log.debug("exported to: " + reportFileName);

			if (openReportPopupWindow) {
				openReportInBrowser(reportFileName);
			}

			parameters.clear();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (SQLException e) {
					EventUtils.logError(e);
				}
			}
		}
		return genCompleteFilePath;
	}

	/**
	 * Open a browser window containing the given text.
	 *
	 * @param output The String of text to be displayed in the browser window.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void showOutputTab(String output) throws FileNotFoundException, IOException {
		String fileName = createFilename(SessionUtils.getCurrentUser(),
				SessionUtils.getCurrentProject(), null);
		fileName = "HTG_" + fileName + ".txt";

		// Create a file in our report directory containing the text passed.
		String output_path = SessionUtils.getRealReportPath();
		String pathName = output_path + fileName;
		File file = new File(pathName);
		FileOutputStream fop = new FileOutputStream(file);
		fop.write(output.getBytes());
		fop.flush();
		fop.close();

		openReportInBrowser(fileName);
	}

	/**
	 * Create that portion of a report's filename which follows a standard
	 * pattern that is expected, and used, by LsFacesServlet, to validate a
	 * user's right to access the report file.
	 *
	 * @param user The User creating the report, or null.
	 * @param project The Project associated with this report.
	 * @param timestamp A timestamp whose primary purpose is to guarantee unique
	 *            filenames.
	 * @return A string which will ultimately be part of the filename,
	 *         containing the user's id and the project id.
	 */
	private static String createFilename(User user, Project project, String timestamp) {
		int userId = 0;
		if (user != null) {
			userId = user.getId();
		}
		if (timestamp == null) {
			DateFormat df = new SimpleDateFormat("dd");
			timestamp = df.format(new Date());
			timestamp += ApplicationUtils.findNextSequenceNumber();
		}
		String reportFileName = "_" + project.getId() + "_" + userId + "_" + timestamp;
		return reportFileName;
	}

	/**
	 * Send a JavaScript command to open the given file in a new browser window.
	 * Note that the browser window is "named" uniquely (using the file name),
	 * so that the user can have multiple reports open in the browser at one
	 * time.
	 *
	 * @param fileName The name of the file to open; it must exist in our
	 *            standard "report" directory within our webapp.
	 */
	public static void openFile(String fileName) {
		openReportInBrowser(fileName);
	}

	/**
	 * Send a JavaScript command to open the given file in a new browser window.
	 * Note that the browser window is "named" uniquely (using the file name),
	 * so that the user can have multiple reports open in the browser at one
	 * time.
	 *
	 * @param reportFileName The name of the file to open; it must exist in our
	 *            standard "report" directory within our webapp.
	 */
	private static void openReportInBrowser(String reportFileName) {
		String javascriptCode = "reportOpen('../../" + Constants.REPORT_FOLDER + "/" +
				reportFileName + "','LSrep" + reportFileName + "');";
		View.addJavascript(javascriptCode);
	}

	/**
	 * Get a database connection for use by Jasper Reports.  This works in both online
	 * and batch contexts.
	 * @throws SQLException
	 */
	private static Connection getConnection() throws SQLException {
		SessionFactoryImplementor sfe = (SessionFactoryImplementor)ServiceFinder.findBean("sessionFactory");
		ConnectionProvider cp = sfe.getConnectionProvider();
		Connection conn = cp.getConnection();
		//log.debug("conn=" + conn);
		return conn;
	}

}
