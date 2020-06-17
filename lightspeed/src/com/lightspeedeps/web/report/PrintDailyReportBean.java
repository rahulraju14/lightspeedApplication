/*
 * File: PrintDailyReportBean.java
 */
package com.lightspeedeps.web.report;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.batch.ExecuteReport;
import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.message.MessageHandler;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DeptTime;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.ArchiveUtils;
import com.lightspeedeps.util.report.CallSheetUtils;
import com.lightspeedeps.util.report.DprUtils;
import com.lightspeedeps.util.report.ReportGenerator;
import com.lightspeedeps.web.popup.SelectContactsBean;
import com.lightspeedeps.web.popup.SelectContactsHolder;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * This class handles print requests generated by the Print, Send,
 * Export and similar buttons on the Callsheet, DPR, and ExhibitG
 * View pages.
 */
@ManagedBean
@ViewScoped
public class PrintDailyReportBean implements SelectContactsHolder, Serializable {
	/** */
	private static final long serialVersionUID = 8837919405007918817L;

	private static final Log log = LogFactory.getLog(PrintDailyReportBean.class);

	/* These strings must match the strings defined for the report-type radio buttons in the JSP. */

	public static final String TYPE_DAILY_DPR = "dpr";
	public static final String TYPE_DAILY_EXHIBITG = "exhibitg";
	public static final String TYPE_DAILY_CALLSHEET = "callsheet";

	// PARAMETERS
	private static final String PARAMETER_QUERY = "sqlQry";
	private static final String PARAMETER_CS_COL1 = "column1dcIds";
	private static final String PARAMETER_CS_COL2 = "column2dcIds";
	private static final String PARAMETER_CS_COL3 = "column3dcIds";
	private static final String PARAMETER_EXG_SUBQRY = "sqlQryTimesheet";
	private static final String PARAMETER_EPISODIC = "episodic";

	private static final String PARAMETER_DPR_CAST_SUBQRY = "sqlQryCast";

	// Actions for Send - used with SelectContactsBean
	private static final int ACT_SEND_CALLSHEET = 10;
	private static final int ACT_SEND_DPR = 11;
	private static final int ACT_SEND_EXHIBITG = 12;

	private String selectedType = TYPE_DAILY_DPR;

	/** The database id of the (daily) report being printed, distributed, etc. */
	private Integer reportId;

	/** The date of the ExhibitG being processed. */
	private Date reportDate; // TODO should be able to eliminate reportDate field.

	/** The Dpr to be printed. */
	private Dpr dpr; // TODO eliminate this Dpr field!

	/** The current project. */
	private Project project;

	/** The currently selected Unit. */
	private Unit unit;

	/** True if the report generation should also add a JavaScript call to the
	 * output stream to open the created report in a new browser window. */
	private boolean openReportPopupWindow = true;

	/** The name of the jasper report file to use. */
	private String reportName = "";

	/** The fully qualified (OS) path and filename of the generated report. */
	private String genCompleteFilePath = "";


	public PrintDailyReportBean() {
		log.debug("");
	}

	@PostConstruct
	public void postContruct() {
		log.debug("");
		project = SessionUtils.getCurrentProject(); // null if in batch mode
		if (project != null) {
			unit = project.getMainUnit(); // callsheet report may change this
			log.debug("Using (current) project=" + project.getId() + ", unit=" + unit.getName());
		}
	}

	/**
	 * @return The bean instance defined in faces-config.xml. Note that there is
	 *         also a bean defined with a capital P, PrintDailyReportBean, which
	 *         is defined in one of the applicationContext XML files and is used
	 *         for "batch" processing.
	 */
	public static PrintDailyReportBean getInstance() {
		return (PrintDailyReportBean)ServiceFinder.findBean("printDailyReportBean");
	}

	/**
	 * Action method for "Print" button on Call sheet View page.
	 * Also call internally by 'send' methods.
	 * @return null navigation string
	 */
	public String actionPrintCallsheet() {
		log.debug("");
		setSelectedType(TYPE_DAILY_CALLSHEET);
		generateReport();
		return null;
	}

	/**
	 * Action method for "Export" button on Call sheet View page.
	 * Also call internally by 'send' methods.
	 * @return null navigation string
	 */
	public String actionExportCallsheet() {
		log.debug("");
		setSelectedType(TYPE_DAILY_CALLSHEET);
		generateReport(true);
		return null;
	}

	/**
	 * Generate the call sheet report as a PDF, and mail it to the appropriate
	 * contacts, and optionally generate and send the location report. Invoked
	 * from CallSheetViewBean, typically as part of the "make final" process.
	 * Note that call-time notifications are done separately, in
	 * CallSheetViewBean.
	 *
	 * @param includeLocReports True iff Location Reports should also be
	 *            generated and sent.
	 * @return Action string for faces navigation
	 */
	/*package*/ String actionPrintAndSendCallsheet(boolean includeLocReports) {
		log.debug("");
		Callsheet cs = CallsheetDAO.getInstance().findById(reportId);
		if (cs != null) {
			openReportPopupWindow = false;
			actionPrintCallsheet();
			openReportPopupWindow = true;
			Unit unit = UnitDAO.getInstance().findByProjectAndNumber(project, cs.getUnitNumber());

			List<Contact> reportContacts = new ArrayList<Contact>();
			List<Contact> directionContacts = new ArrayList<Contact>();
			// get project members who want callsheet or directions e-mailed
			List<Contact> members = ProjectMemberDAO.getInstance().findByUnitDistinctContact(unit);
			for (Contact contact : members) {
				if (EmailValidator.isValidEmail(contact.getEmailAddress())) {
					if (contact.getSendCallsheet()) {
						reportContacts.add(contact);
					}
					if (includeLocReports && contact.getSendDirections()) {
						directionContacts.add(contact);
					}
				}
			}
			// send Callsheet report e-mails
			log.debug("rpt contacts=" + reportContacts.size() + ", loc contacts=" + directionContacts.size());
			if (reportContacts.size() > 0) {
				DoNotification no = DoNotification.getInstance();
				no.callsheetPublished(reportContacts, unit, cs.getDate(), cs.getUpdated(), genCompleteFilePath);
			}
			// send location directions e-mails
			if (directionContacts.size() > 0) {
				ReportBean rb = ReportBean.getInstance();
				rb.sendLocDirReport(cs, directionContacts);
			}
		}
		return "reports";
	}

	/**
	 * Archive the given Callsheet asynchronously, by scheduling another task to
	 * perform the archive independently.
	 *
	 * @param cs The Callsheet to be archived.
	 */
	public void archiveCallsheetAsync(Callsheet cs) {
		log.debug("");
		ExecuteReport.scheduleReport(ReportType.CALL_SHEET, cs, SessionUtils.getCurrentUser());
	}

	/**
	 * Generate a call sheet report and archive it. This generates a PDF print
	 * of the Callsheet, then stores the PDF as an object in our File Repository
	 * in the appropriate directory, based on the Project and Unit. Note that
	 * this method may be called from a "batch" process, which will not have a
	 * FacesContext.
	 *
	 * @param cs The Callsheet to be archived.
	 * @param cUser The User who will be made the "owner" of the archived
	 *            callsheet document in the File Repository.
	 */
	public void archiveCallsheet(Callsheet cs, User cUser) {
		try {
			if (cs != null) {
				cs = CallsheetDAO.getInstance().refresh(cs);
				project = cs.getProject();
				unit = cs.getProject().getUnit(cs.getUnitNumber());
				if (unit == null) {
					EventUtils.logError("Missing Unit for Callsheet, id=" + cs.getId());
				}
				else {
					log.debug("proj=" + project.getId() + ", unit=" + unit.getId());
					setReportId(cs.getId());
					openReportPopupWindow = false;
					actionPrintCallsheet();
					openReportPopupWindow = true;
					if (genCompleteFilePath != null) {
						File file = new File(genCompleteFilePath);
						boolean ret = ArchiveUtils.storeCallsheet(cs.getDate(), unit, file, cUser);
						file.delete();
						if (! ret) {
							EventUtils.logError("Save of archival Call sheet to File Repository failed.");
						}
					}
					else {
						EventUtils.logError("Generation of archival Call sheet failed.");
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Generate the call sheet report as a PDF, and mail it to the addresses supplied by the user.
	 * Invoked via Action method of Send button on Call Sheet View or Edit page.
	 * Note that the requested call sheet date has already been set via a f:setPropertyActionListener
	 * tag in the jsp page.
	 *
	 * @return Action string for faces navigation
	 */
	public String actionSendCallsheet() {
		log.debug("");
		try {
			Callsheet cs = CallsheetDAO.getInstance().findById(reportId);
			if (cs != null) {
				unit = project.getUnit(cs.getUnitNumber());
				if (unit == null) { // VERY unusual! (deleted unit after creating callsheet?)
					unit = project.getMainUnit();
				}
				SelectContactsBean.getInstance().show(
						ACT_SEND_CALLSHEET, this, unit, "Report.SendCallsheet.Title" );
				// control returns via the contactsSelected() method
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for "Print" button on DPR View page.  The reportId
	 * field has already been set via an f:setPropertyActionListener tag.
	 * @return Empty string, so the user stays on the View page.
	 */
	public String actionPrintDpr() {
		Dpr dpr = DprDAO.getInstance().findById(reportId);
		generateDpr(dpr, false);
		return null;
	}

	/**
	 * Action method for the "Export" button on DPR View page.
	 * The 'reportId' field has already been set via an f:setPropertyActionListener tag.
	 * @return null navigation string
	 */
	public String actionExportDpr() {
		Dpr dpr = DprDAO.getInstance().findById(reportId);
		generateDpr(dpr, true);
		return null;
	}

	public String actionSendDpr() {
		log.debug("");
		try {
			Dpr dpr = DprDAO.getInstance().findById(reportId);
			if (dpr != null) {
				reportDate = dpr.getDate();
				SelectContactsBean.getInstance().show(
						ACT_SEND_DPR, this, null,
						"Report.SendDpr.Title");
				// control returns via the contactsSelected() method
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Currently unused (8/14/2012, rev 3312)
	 * Generate the DPR report as a PDF, and mail it to the appropriate contacts.
	 * Typically called from DprViewBean as a result of the Distribute button
	 * being clicked.
	 */
	@SuppressWarnings("unused")
	private void printAndSendDpr(Dpr pDpr) {
/*		try {
			reportDate = pDpr.getDate();
			log.debug("dprDate=" + reportDate);
			openReportPopupWindow = false;
			generateDpr(pDpr, false); // Create the PDF
			openReportPopupWindow = true;

			AuthorizationBean authBean = AuthorizationBean.getInstance();
			List<Contact> reportContacts = new ArrayList<Contact>();
			// get project members who want callsheet or directions e-mailed
			List<Contact> contacts = projectMemberDAO.findByProjectDistinctContact(project);
			for (Contact tContact : contacts) {
				if (tContact.getSendDpr()) { // person wants DPR sent
					if (EmailValidator.isValidEmail(tContact.getEmailAddress())) { // has good email addr
						if (authBean.hasPermission(tContact, project, Permission.VIEW_PRODUCTION_REPORT)) {
							// ..and has appropriate permission, so add to list
							reportContacts.add(tContact);
						}
					}
				}
			}
			// send report e-mails
			log.debug("rpt contacts=" + reportContacts.size());
			if (reportContacts.size() > 0) {
				DoNotification no = DoNotification.getInstance();
				no.dprPublished(reportContacts, reportDate, pDpr.getUpdated(), genCompleteFilePath);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
*/	}

	/**
	 * Action method for Print button on Exhibit G View page.
	 * The reportId field should be set already via an f:setPropertyActionListener.
	 *
	 */
	public String actionPrintExhibitG() {
		log.debug("");
		ExhibitG exg = ExhibitGDAO.getInstance().findById(reportId);
		if (exg != null) {
			setReportDate(exg.getDate());
			openReportPopupWindow = true;
			generateExhibitG();
		}
		else {
			MsgUtils.addFacesMessage("ExhibitG.Missing", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	public String actionSendExhibitG() {
		log.debug("");
		try {
			ExhibitG exg = ExhibitGDAO.getInstance().findById(reportId);
			if (exg != null) {
				setReportDate(exg.getDate());
				SelectContactsBean.getInstance().show(
						ACT_SEND_EXHIBITG, this, null, "Report.SendExhibitG.Title");
				// control returns via the contactsSelected() method
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Called when user closes the Contact selection pop-up, used for
	 * selecting recipients when sending reports.
	 *
	 * @see com.lightspeedeps.web.popup.SelectContactsHolder#contactsSelected(int, Collection)
	 */
	@Override
	public void contactsSelected(int action, Collection<Contact> list) {
		if (list != null) {
			switch(action) {
			case ACT_SEND_CALLSHEET:
				sendCallsheetContacts(list);
				break;
			case ACT_SEND_DPR:
				sendDprContacts(list);
				break;
			case ACT_SEND_EXHIBITG:
				sendExhibitGContacts(list);
				break;
			}
		}
	}

	/**
	 * Generate the report type specified in selectedType as a PDF.
	 */
	private void generateReport() {
		generateReport(false);
	}

	/**
	 * Generate the report type specified in selectedType.
	 *
	 * @param pExport True if the report should be generated as an Excel file,
	 *            instead of as a PDF.
	 */
	private void generateReport(boolean pExport) {
		try {
			boolean bRet = true;
			/** A Map of report parameters, created here, and passed to the Jasper Reports
			 * code to control report generation. */
			Map<String, Object> parameters = new HashMap<String, Object>();

			project = ProjectDAO.getInstance().refresh(project);
			unit = UnitDAO.getInstance().refresh(unit);

			if (selectedType.equalsIgnoreCase(TYPE_DAILY_EXHIBITG)) {
				bRet = setupExhibitG(parameters);
			}
			else if (selectedType.equalsIgnoreCase(TYPE_DAILY_DPR)) {
				setupDprReport(parameters);
			}
			else if (selectedType.equalsIgnoreCase(TYPE_DAILY_CALLSHEET)) {
				bRet = setupCallsheetReport(parameters);
			}

			if (! bRet) {
				return;
			}

			genCompleteFilePath = ReportGenerator.generateReport(project, unit, parameters, reportName,
					null, null, openReportPopupWindow, pExport, null);
			if (genCompleteFilePath == null) {
				MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return;
	}

	/**
	 * Generate a DPR (Production Report) as either a PDF or XLS file. This can
	 * be used for either displaying the result in a pop-up window, or for
	 * sending the resulting file.
	 *
	 * @param pDpr The DPR whose report will be generated.
	 * @param pExport True if the output should be an XLS file, false for a PDF.
	 */
	private void generateDpr(Dpr pDpr, boolean pExport) {
		log.debug("");
		dpr = pDpr;
		reportDate = dpr.getDate();
		setSelectedType(TYPE_DAILY_DPR);

		generateReport(pExport);
		dpr = null;
	}

	/**
	 * Generate the Exhibit G report, either for display in a pop-up window, or for
	 * sending the PDF.
	 */
	private void generateExhibitG() {
		setSelectedType(TYPE_DAILY_EXHIBITG);
		generateReport();
	}

	/**
	 * Create a PDF of the Call Sheet and send it to the list of Contacts
	 * supplied. Called from the contactsSelected method, when the user clicks
	 * Ok (or Close?) on the "send contacts" pop-up dialog.  The 'reportId' field
	 * should already be set to the database id of the Callsheet to be printed.
	 *
	 * @param list The List of Contact`s who should receive the report.
	 */
	private void sendCallsheetContacts(Collection<Contact> list) {
		log.debug("");
		try {
			project = SessionUtils.getCurrentProject();
			Callsheet cs = CallsheetDAO.getInstance().findById(reportId);
			if (cs != null && list != null && list.size() > 0) {
				openReportPopupWindow = false;
				actionPrintCallsheet();
				openReportPopupWindow = true;
				String sender = SessionUtils.getCurrentUser().getAnyName();
				MessageHandler handler = MessageHandler.getInstance();
				handler.sendReport(list, "MessageHandler.report", "Call Sheet",
						cs.getDate(), cs.getUpdated(),
						sender, genCompleteFilePath, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Generate the DPR as a PDF, and mail it to the addresses supplied by the
	 * user. Called from contactsSelected after user hit Send button on DPR View
	 * page. Note that the requested DPR reportId was set via a
	 * f:setPropertyActionListener tag in the jsp page.
	 *
	 * @param list The List of Contact`s who should receive the report.
	 */
	private void sendDprContacts(Collection<Contact> list) {
		log.debug("");
		try {
			Dpr dpr = DprDAO.getInstance().findById(reportId);
			if (dpr != null && list != null && list.size() > 0) {
				openReportPopupWindow = false;
				generateDpr(dpr, false);
				openReportPopupWindow = true;
				String sender = SessionUtils.getCurrentUser().getAnyName();
				MessageHandler handler = MessageHandler.getInstance();
				handler.sendReport(list, "MessageHandler.report", "Production Report",
						dpr.getDate(), dpr.getUpdated(),
						sender, genCompleteFilePath, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Generate the ExhibitG report as a PDF, and mail it to the addresses
	 * supplied by the user. Invoked from contactsSelected, after user hits Send
	 * button on ExhibitG View page. Note that the reportId has already been
	 * set via a f:setPropertyActionListener tag in the jsp page.
	 *
	 * @param list The List of Contact`s who should receive the report.
	 */
	private void sendExhibitGContacts(Collection<Contact> list) {
		log.debug("");
		try {
			ExhibitG exg = ExhibitGDAO.getInstance().findById(reportId);
			if (exg != null && list != null && list.size() > 0) {
				openReportPopupWindow = false;
				generateExhibitG();
				openReportPopupWindow = true;
				String sender = SessionUtils.getCurrentUser().getAnyName();
				MessageHandler handler = MessageHandler.getInstance();
				handler.sendReport(list, "MessageHandler.report.Exg", "Exhibit G",
						exg.getDate(), exg.getUpdated(),
						sender, genCompleteFilePath, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Does the necessary preparation for creating a Callsheet report, such as
	 * creating the SQL query and building the parameters used for the layout of
	 * the crew-call section. The 'reportId' field should already be set to the
	 * database id of the Callsheet to be printed.
	 */
	private boolean setupCallsheetReport(Map<String, Object> parameters) {
		boolean ret = false;
		String callsheetQuery = ReportQueries.callsheetQuery;
		callsheetQuery += " and cs.id =" + reportId.toString() + ";";

		Callsheet callsheet = CallsheetDAO.getInstance().findById(reportId);
		parameters.put("reportTimeZone", callsheet.getTimeZone());
		parameters.put(PARAMETER_EPISODIC, project.getProduction().getType().getCrossProject());

		unit = project.getUnit(callsheet.getUnitNumber());
		if (unit == null) {
			EventUtils.logError("Missing Unit for Callsheet, id=" + callsheet.getId());
			MsgUtils.addFacesMessage("Report.Callsheet.NoUnit", FacesMessage.SEVERITY_ERROR);
		}
		else {
			log.debug("unit #" + unit.getNumber() + "=" + unit.getName());
			@SuppressWarnings("unchecked")
			List<DeptCall> deptCalls[] = new List[3];
			CallSheetUtils.createDeptColumns(callsheet, deptCalls);
			String[] deptList = new String[3];
			for (int i=0; i < 3; i++) {
				List<DeptCall> dcList = deptCalls[i];
				String dstr = "";
				for (DeptCall dc : dcList) {
					dstr += "," + dc.getId();
				}
				if (dstr.length() > 0) {
					dstr = dstr.substring(1); // remove leading comma
				}
				else {
					dstr = "0"; // id that won't match any records, but keeps SQL legal
				}
				deptList[i] = dstr;
				log.debug("dept list " + i + "=" + deptList[i] );
			}
			parameters.put(PARAMETER_CS_COL1, deptList[0]);
			parameters.put(PARAMETER_CS_COL2, deptList[1]);
			parameters.put(PARAMETER_CS_COL3, deptList[2]);

			setReportName(ReportBean.JASPER_CALL_SHEET);
			parameters.put(PARAMETER_QUERY, callsheetQuery);
			ret = true;
		}
		return ret;
	}

	/**
	 * Set up parameters for DPR report, such as creating the SQL query and
	 * building the parameters used for the layout of the crew-call section. The
	 * 'dpr' field should be the DPR object to be printed.
	 */
	private void setupDprReport(Map<String, Object> parameters) {
		String sqlQry = ReportQueries.dprQuery + " and dpr.id = " + dpr.getId() + ";";
		parameters.put(PARAMETER_QUERY, sqlQry);
		parameters.put(PARAMETER_EPISODIC, project.getProduction().getType().getCrossProject());

		String sqlSubQry = ReportQueries.dprCastQuery +
			" and tc.dpr_cast_id = " + dpr.getId() +
			" order by tc.castid; ";
		parameters.put(PARAMETER_DPR_CAST_SUBQRY, sqlSubQry);

		setReportName(ReportBean.JASPER_DPR);

		// Set flag parameters to control printing of "Media usage" section; rev 4074.
		List<FilmStock> filmStockList = FilmStockDAO.getInstance().findLatestThroughDate(dpr.getDate());
		Boolean printMedia = filmStockList.size() > 0;
		parameters.put("printMedia", printMedia);
		printMedia = filmStockList.size() > 3; // second row of "film use" required?
		parameters.put("printMedia2", printMedia);

		@SuppressWarnings("unchecked")
		List<DeptTime> deptTimes[] = new List[3];
		DprUtils.createDeptColumns(dpr, deptTimes);
		String[] deptList = new String[3];
		for (int i=0; i < 3; i++) {
			List<DeptTime> dcList = deptTimes[i];
			String dstr = "";
			for (DeptTime dc : dcList) {
				dstr += "," + dc.getDepartment().getId();
			}
			if (dstr.length() > 0) {
				dstr = dstr.substring(1); // remove leading comma
			}
			else {
				dstr = "0"; // id that won't match any records, but keeps SQL legal
			}
			deptList[i] = dstr;
			log.debug("dept list " + i + "=" + deptList[i] );
		}
		parameters.put(PARAMETER_CS_COL1, deptList[0]);
		parameters.put(PARAMETER_CS_COL2, deptList[1]);
		parameters.put(PARAMETER_CS_COL3, deptList[2]);
	}

	/**
	 * Set up parameters for ExhibitG report, such as creating the SQL query
	 * and retrieving relevant information from the associated Timesheet.
	 * The 'reportId' field should already be set to the database id of the
	 * ExhibitG to be printed.
	 */
	private boolean setupExhibitG(Map<String, Object> parameters) {
		boolean bRet = false;
		try {
			String sqlQry = ReportQueries.exhibitGQuery + " exhibit_g.id =" + reportId + ";";
			String exhibitGQuerySubRept = ReportQueries.exhibitGQuerySubRept;
			exhibitGQuerySubRept += " and tc.exhibitG_id = " + reportId;
			exhibitGQuerySubRept += " order by tc.castId, last_name, first_name; ";
			setReportName(ReportBean.JASPER_EXHIBIT_G);
			parameters.put(PARAMETER_QUERY, sqlQry);
			parameters.put(PARAMETER_EXG_SUBQRY, exhibitGQuerySubRept);
			bRet = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	public void setSelectedType(String s) {
		selectedType = s;
	}

	public String getReportName() {
		return reportName;
	}
	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #reportId}. */
	public Integer getReportId() {
		return reportId;
	}
	/** See {@link #reportId}. */
	public void setReportId(Integer reportId) {
		this.reportId = reportId;
	}

	/** See {@link #reportDate}. */
	public Date getReportDate() {
		return reportDate;
	}
	/** See {@link #reportDate}. */
	public void setReportDate(Date reportDate) {
		this.reportDate = reportDate;
	}

}