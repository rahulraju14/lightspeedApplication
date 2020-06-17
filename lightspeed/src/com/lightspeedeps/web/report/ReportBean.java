/*
 * File: ReportBean.java
 */
package com.lightspeedeps.web.report;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.message.MessageHandler;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DailyReportItem;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.file.ArchiveUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.*;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupCheckboxBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Report selection page.
 */
@ManagedBean
@ViewScoped
public class ReportBean extends View implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 3093133818634528859L;

	private static final Log log = LogFactory.getLog(ReportBean.class);

	/** The attribute used for saving the Report Type in the session. */
	public static final String ATTR_RPT_TYPE = Constants.ATTR_PREFIX + "reportType";

	/* These strings must match the strings defined for the report-type radio buttons in the JSP. */
	private static final String TYPE_BREAKDOWN = "breakdown";
	private static final String TYPE_STRIPBOARD = "stripboard";
	private static final String TYPE_ELEMENTS = "elements";
//	private static final String TYPE_CONTRACTS = "contracts";
	private static final String TYPE_PEOPLE = "people";

	private static final String TYPE_LOCATION = "locIndReports";

	public static final String TYPE_DAILY_DPR = "dpr";
	public static final String TYPE_DAILY_EXHIBITG = "exhibitg";
	public static final String TYPE_DAILY_CALLSHEET = "callsheet";

	public static final String TYPE_SCRIPT = "script";

	private static final String TYPE_START_FORM = "startForm";
	private static final String TYPE_START_FORM_LIST = "startFormList";
	private static final String TYPE_FORM_I9 = "formI9";
	private static final String TYPE_FORM_INDEM = "formIndem";
	private static final String TYPE_FORM_MTA = "formMta";
	private static final String TYPE_FORM_ACTRA_CONTRACT = "formActraContract";
	private static final String TYPE_FORM_ACTRA_INTENT = "formActraIntent";
	private static final String TYPE_TOURS_WEEKLY_TIMESHEET = "toursWeeklyTimesheet";
	private static final String TYPE_TOURS_BW_TIMESHEET = "toursBWTimesheet";
	private static final String TYPE_TOURS_SM_TIMESHEET = "toursSMTimesheet";
	private static final String TYPE_TOURS_16DAY_TIMESHEET = "tours16DayTimesheet";
	private static final String TYPE_TOURS_MONTHLY_TIMESHEET = "toursMonthlyTimesheet";
	private static final String TYPE_IMG_ATTACHMENT = "imageAttachment";

	private static final String INCLUDE_ALL = "all";
	private static final String RANGE_DATE = "dateRange";
	private static final String RANGE_SCENE = "scene";
	private static final String ORDER_SCENE = "scene";
	private static final String ORDER_SCHEDULE = "schedule";

	// PARAMETERS
	public static final String PARAMETER_QUERY = "sqlQry";

	// TAB DEFINITIONS
	//private static final int TAB_CONTACTS = 0;
	//private static final int TAB_CONTACT_SHEET = 1; // removed from use 9/9/2011 rev 2185

	private static final int TAB_BREAKDOWN = 0;
	//private static final int TAB_SHOOTING_SCHEDULE = 1;

	private static final int TAB_ELEMENTS = 1;
	private static final int TAB_LOCATIONS = 2;
	//private static final int TAB_DOOD = 0;


	//  * * * * * * * * * * * * * * * * * * * * * * * * * * * *
	//  ** JASPER REPORT NAMES (names of jrxml/jasper files) **
	//  * * * * * * * * * * * * * * * * * * * * * * * * * * * *

	public static final String JASPER_CALL_SHEET = "callsheet";
	public static final String JASPER_DPR = "dpr";
	public static final String JASPER_EXHIBIT_G = "exhibitgMain";

	private static final String JASPER_DOOD = "dood";

	/** Suffix added to Contact report name when "group by department" is NOT
	 * selected.  Note that the beginning part of the report name is taken from
	 * the constants defined for the drop-down, below. See {@link #peopleReportStyle}. */
	private static final String JASPER_CONTACTS_UNGRP_SUFFIX = "Ungrp";

	private static final String JASPER_LOCATION_REPORT = "locRept";

	private static final String JASPER_ELEMENTS = "elementList";
	private static final String JASPER_ELEM_PAGED = "elementListNewPage";
	private static final String JASPER_ELEM_SCENE = "elementListIncludingScene";
	private static final String JASPER_ELEM_SCENE_PAGED = "elementListIncludingSceneNewPage";

	private static final String JASPER_BREAKDOWN = "breakdownSheetScene";

	private static final String JASPER_SCHEDULE = "shootingSchMainRept";
	private static final String JASPER_SCHEDULE_1_LINE = "shootingOneSchMainRept";
	private static final String JASPER_SCHEDULE_DATE = "shootingSchMainReptDate";
	private static final String JASPER_SCHEDULE_1_LINE_DATE = "shootingOneSchMainReptDate";

	/** Stripboard partial report name - will get suffix of either "Date" or "Scene" */
	private static final String JASPER_STRIPBOARD_THIN = "stripboardThin";
	/** Stripboard partial report name - will get suffix of either "Date" or "Scene" */
	private static final String JASPER_STRIPBOARD_THICK = "stripboardThick";
	private static final String JASPER_STRIPBOARD_SCENE_SUFFIX = "Scene";
	private static final String JASPER_STRIPBOARD_DATE_SUFFIX = "Date";

	private static final String JASPER_SCRIPT = "script";
	private static final String JASPER_SCRIPT_SIDES_1 = "scriptsides1";
	private static final String JASPER_SCRIPT_SDES_2 = "scriptsides2";

	// JASPER REPORTS for PAYROLL

	private static final String JASPER_START_FORM = "startForm";
	private static final String JASPER_START_FORM_AICP = "startFormAicp";
	private static final String JASPER_START_FORM_TOURS = "startFormTours";
	private static final String JASPER_START_FORM_LIST = "startFormList";
	private static final String JASPER_START_FORM_LIST_AICP = "startFormListAicp";
	public static final String JASPER_TIMECARD_1 = "timecard1";
	public static final String JASPER_TIMECARD_2 = "timecard2";
	public static final String JASPER_TIMECARD_3 = "timecard3";
	public static final String JAPSER_MODEL_TIMECARD_AICP = "modelTcAicp";
	public static final String JASPER_TIMECARD_AICP = "timecardAicp";
	public static final String JASPER_TIMECARD_AICP2 = "timecardAicp2"; // For QA testing 1/29/16
	public static final String JASPER_BOX_RENTAL = "boxRentalOnly";
	public static final String JASPER_MILEAGE = "mileageOnly";
	public static final String JASPER_BATCH_LIST = "weeklyBatchList";
	public static final String JASPER_BATCH_LIST_AICP = "weeklyBatchListAicp";
	public static final String JASPER_DISCREPANCY = "discrepancy";
	public static final String JASPER_DISCREPANCY_SUM = "discrepancySummary";
	public static final String JASPER_PAYROLL_1 = "payroll1";
	public static final String JASPER_PAYROLL_2 = "prWeek1";
	public static final String JASPER_HOTCOSTS_DAILY = "hotCostsDaily";
	public static final String JASPER_HOTCOSTS_WEEKLY_SUM = "hotCostsWeeklySummary";
	private static final String JASPER_FORM_I9 = "formI9";
	private static final String JASPER_FORM_I9_17 = "formI917";
	private static final String JASPER_FORM_I9_20 = "formI920";
	private static final String JASPER_FORM_INDEM = "formIndem";
	private static final String JASPER_FORM_MTA = "formMta";
	private static final String JASPER_FORM_ACTRA_CONTRACT = "formActraContract";
	private static final String JASPER_FORM_ACTRA_INTENT = "formActraIntent";
	public static final String JASPER_DAILY_TIMESHEET = "dailyTimeSheet";
	private static final String JASPER_TOURS_WEEKLY_TIMESHEET = "toursWeeklyTimesheet";
	private static final String JASPER_TOURS_BW_TIMESHEET = "toursBWTimesheet";
	private static final String JASPER_TOURS_SM_TIMESHEET = "toursSMTimesheet";
	private static final String JASPER_TOURS_16DAY_TIMESHEET = "tours16DayTimesheet";
	private static final String JASPER_TOURS_MONTHLY_TIMESHEET = "toursMonthlyTimesheet";
	public static final String JASPER_TOURS_TAX_WAGE_ALLOCATION = "toursTaxWageAllocationForm";
	private static final String JASPER_IMG_ATTACHMENT = "imageAttachment";

	private List<Item> reportStyleList = new ArrayList<>();
	private final List<String> reportTypeList = new ArrayList<>();
	private List<TaskStatus> taskStatusList;

//	private List<SelectItem> contactDL = null;

	private Item selectedItem;
	private String selectedType = TYPE_BREAKDOWN;

	/** The database id of the currently selected Unit.  Set via the drop-down
	 * list on the Calendar View page. */
	private Integer selectedUnitId;

	/** A selection list for choosing which Project to use for Contact report. */
	private List<SelectItem> projectDL;

	/** A selection list for choosing which Unit to use for various reports. */
	private List<SelectItem> unitDL;

	/** A selection list for choosing which Unit to use for Contact/People reports.
	 * This includes the choice "All" in addition to the individual Unit's. */
	private List<SelectItem> unitAllDL;

	// LOCATIONS

	/** The database id of the Location (RW Element) that the
	 * location report is for. */
	private Integer locationId;
	private String locationStatus = "locationissuelist";

	// Breakdown / shooting schedule

	/** Style for shooting schedule: normal or 1-line */
	private String shootingReportStyle = SHOOTING_STYLE_STANDARD;
	private static final String SHOOTING_STYLE_STANDARD = "standard";
	@SuppressWarnings("unused")
	private static final String SHOOTING_STYLE_ONELINE = "oneline";

	private transient ScheduleUtils sDays;

	/** Report order selection for Breakdown report. */
	private String orderByInBreakDown = ORDER_SCENE;
	/** Report order selection for Shooting schedule report. */
	private String orderByInShootingSch = ORDER_SCHEDULE;

	/** Starting scene number in scene range selection for Breakdown. */
	private String fromSheetTypeInBreakdown;
	/** Ending scene number in scene range selection for Breakdown. */
	private String toSheetTypeInBreakdown;

	/** Starting scene number in scene range selection for Shooting schedule. */
	private String fromSheetTypeInShootingSch;
	/** Ending scene number in scene range selection for Shooting schedule. */
	private String toSheetTypeInShootingSch;

	private String includeInBreakdown = INCLUDE_ALL;
	private String includeInScene = INCLUDE_ALL;

	/** Starting date in date range selection for both Breakdown and Shooting schedule. */
	private Date fromDateInBrkDown;
	/** Ending date in date range selection for both Breakdown and Shooting schedule. */
	private Date toDateInBrkDown;

	private int firstShootDayNumber;
	private List<SelectItem> sceneNumbers;

	/** A boolean used to control date fields.  When a Unit selection is made by
	 * the user, we want to update the default date ranges for breakdown, shooting
	 * schedule, and stripboard reports.  However, the valueChangeListener which
	 * gets the Unit change is called during a phase prior to the UpdateModel phase,
	 * at which point our "setXxxx" methods are called.  We set the override flag
	 * in actionChangeUnit() so the setX methods for the from- and to-dates will ignore
	 * the values during the next updateModel phase. */
	private boolean override;

	// STRIPBOARD

	/** radio button selection for style of report, either Thick or Thin. */
	private String styleByInStripboard = STRIPBOARD_STYLE_THIN;
	private static final String STRIPBOARD_STYLE_THIN = "thin";
	@SuppressWarnings("unused")
	private static final String STRIPBOARD_STYLE_THICK = "thick";

	private String orderByInStripboard = STRIPBOARD_ORDER_SCENE;
	private static final String STRIPBOARD_ORDER_SCENE = "scene";
	@SuppressWarnings("unused")
	private static final String STRIPBOARD_ORDER_SCHEDULE = "schedule";

	/** Whether or not to limit the report by a date range */
//	private boolean dateRangeInStripboard = true;
	/** Starting date in date range for Stripboard report. */
	private Date fromDateInStripboard;
	/** Ending date in date range for Stripboard report. */
	private Date toDateInStripboard;

	/** Radio button selection for which strips to include in the Stripboard report. */
	private String includeStrips = INCLUDE_STRIPS_SCHEDULED;
	public static final String INCLUDE_STRIPS_ALL = "all";
	public static final String INCLUDE_STRIPS_UNSCHEDULED = "unscheduled";
	public static final String INCLUDE_STRIPS_SCHEDULED = "scheduled";

	//  CONTACT / PEOPLE

	/** Allowed to display cast in Contact report. */
	private boolean showCast;
	/** Allowed to display crew in Contact report. */
	private boolean showCrew;
//	/** Allowed to display LS Admin role users in report. */
//	private boolean showAdmin;
	/** Allowed to display Contacts marked as 'hidden' in report. */
	private boolean showHidden;
	/** The database id for the selected contact for a Contact Sheet report. */
	private Integer contactId;

	/** Internal report style for contact report - based on user's radio button
	 * selections, and used to generate jasper report name that will be used. */
	private String peopleReportStyle = CONTACT_LIST_COMPACT;
	private static final String CONTACT_LIST_COMPACT = "contLstComp";
	private static final String CONTACT_LIST_MEDIUM = "contLstMed";

	/** Radio button - Order selection for Contact (Cast&Crew) report. */
	private String orderByInPeople = CONTACT_REPORT_ORDER_NAME;
	private static final String CONTACT_REPORT_ORDER_NAME = "Name";
	@SuppressWarnings("unused")
	private static final String CONTACT_REPORT_ORDER_JOB = "JobTitle";

	/** Include cast in Contact report. */
	private boolean includeCastInPeople;
	/** Include crew in Contact report. */
	private boolean includeCrewInPeople = true;
	/** Include vendors (not cast or crew) in report. */
	private boolean includeVendors;
	/** If true, user has selected to group Contact report by Department.  This
	 * will affect which jasper report name is selected. */
	private boolean groupByDepartmentInPeople;

	/** The id of the Project whose members will be included in the
	 * Contact report. */
	private Integer peopleProjectId = 0;
	/** The id of the Unit whose members will be included in the
	 * Contact report. */
	private Integer peopleUnitId = 0;

	// CONTRACTS

	private String orderByInContracts = "Name";
	private boolean includeCastInContract = true;
	private boolean includeCrewInContracts;
	private boolean groupByDepartmentInContracts = true;
	private boolean fullyCompleted = true;
	private boolean itemsPending;

	// ELEMENTS

	private static final int TYPE_COUNT = ScriptElementType.values().length;

	/** Array to track which ScriptElementType`s have been selected by the
	 * user for inclusion in the Element report. */
	private Boolean[] includeInElement = new Boolean[TYPE_COUNT];

	/** Element list filter selected by the user related to whether or
	 * not the "real element required" status has been satisfied or not. */
	private String requirementStatInElement = REQUIRED_BOTH;
	private static final String REQUIRED_MISSING = "missing";
	private static final String REQUIRED_SATISFIED = "satisfied";
	private static final String REQUIRED_BOTH = "both";

	private String orderByInElement = ORDER_ID;
	private static final String ORDER_ID = "id";
	private static final String ORDER_OCCURS = "occurs";

	/** If true, start a new page for each ScriptElementType in
	 * the Element report.  This affects which jasper report is selected. */
	private boolean includeNewPageInElement = true;
	private boolean includeSceneInElement;

	/** A selection list for choosing which Unit to use for DooD/Element reports.
	 * This includes the choice "All" in addition to the individual Unit's
	 * in the current Projecct. */
	private List<SelectItem> unitElemAllDL;
	/** The id of the Unit whose members will be included in the
	 * Contact report. This is the selected value from the unitElemeAllDL
	 * drop-down list. */
	private Integer elemUnitId = 0;

	//  DOOD report

	/** Array to track which ScriptElementType`s have been selected by the
	 * user for inclusion in the DooD report. */
	private Boolean[] includeInDood = new Boolean[TYPE_COUNT];

	@SuppressWarnings("unused")
	private static final String DOOD_ORDER_WORKDAYS = "numberofWorkdays";
	private static final String DOOD_ORDER_ID = "id";
	private static final String DOOD_ORDER_DATE = "startdate";
	private String orderByInDayOut = DOOD_ORDER_ID;

	/** If true, start a new page for each ScriptElementType in
	 * the DooD report.  This affects which jasper report is selected. */
	private boolean includeNewPageInDayOut = true;

	// Daily Reports

	private static final int ACT_DELETE_REPORT = 10;

	/** Name of the archived file to be opened. Usually set via
	 * f:setPropertyActionListener. */
	private String archivedName;

	/** List of existing Dpr`s for the current Project. */
	private List<DailyReportItem> dprList;
	/** List of existing Callsheet`s for the currently selected Unit. */
	private List<DailyReportItem> callSheetList;
	/** List of existing ExhibitG`s for the current Project. */
	private List<DailyReportItem> exhibitGList;

	private Date archiveDate = new Date();

	/** List of Date`s of available archived Callsheet`s within the currently
	 * selected Unit. */
	private List<SelectItem> archivedCallsheetDateDL;

	/** List of archived Callsheets (as Documents) for the currently selected Unit
	 * on the selected Date. */
	private List<Document> archivedCallSheets;

	/** Show the "create report" pop-up when true. */
	private boolean showCreateReport;

	/** The database id of the report to be deleted, set via the
	 * f:setPropertyActionListener tag on the red "X" (delete) control. */
	private Integer removeReportId;

	/** The date to be assigned to the new daily report being created,
	 * set via the calendar pop-up in the createnew.xhtml fragment. */
	private Date newDate;

	// Script

	/** The text to apply as a watermark to the report.  If null, no watermarking
	 * is done. */
	private WaterMark waterMark;

	// For all or multiple reports


	/** The user's current Project. */
	private Project project;

	/** The currently selected Unit. */
	private Unit unit;

	private Unit sbUnit;	// unit for stripboard report

	/** The database id of the user's current Project. */
	private final int projId;
	private final String addProjectId;

	private boolean openReportPopupWindow = true;
	private String sqlQry = "";
	private String reportName = "";
	private String genCompleteFilePath = "";
	private final Map<String, Object> parameters = new HashMap<>();

	/** Track the state of the selected item in the reportsStyleList */
	private RowStateMap stateMap = new RowStateMap();

	public ReportBean() {
		this(SessionUtils.getCurrentProject(), SessionUtils.getString(ATTR_RPT_TYPE));
	}

	public ReportBean(Project proj, String rptType) {
		super("");
		log.debug("");
		final AuthorizationBean authBean = AuthorizationBean.getInstance();

		project = proj;
		if (project == null) { // Outside-production environment, e.g., "My Starts"
			// Probably instantiated to print a document
			project = SessionUtils.getCurrentOrViewedProject();
		}

		projId = project.getId();
		addProjectId = " and proj.id = " + projId + " ";
		peopleProjectId = project.getId();
		unit = project.getMainUnit();
		selectedUnitId = unit.getId();

		sDays = new ScheduleUtils(unit);
		for (int i=0; i < includeInDood.length; i++) {
			includeInDood[i] = false;
		}
		for (int i=0; i < includeInElement.length; i++) {
			includeInElement[i] = false;
		}
		includeInElement[0] = true;
		includeInDood[0] = true;

		if (rptType != null) {
			setSelectedType(rptType);
			log.debug("report type (selected group): " + rptType);
		}
		else if (authBean.hasPermission(SessionUtils.getCurrentContact(), project, Permission.VIEW_SCENE)) {
			setSelectedType(TYPE_BREAKDOWN);	// normal default (first radio button)
		}
		else if (ApplicationUtils.isOffline()) {
			// no breakdown access; select the one that ALMOST everyone can get to Offline
			setSelectedType(TYPE_STRIPBOARD);
		}
		else {
			// no breakdown access; select the one that everyone can get to Online
			setSelectedType(TYPE_PEOPLE); // People/Contacts
		}

		createReportStyleList(authBean);
		checkTab();
//		createTaskStatusList();
	}

	public static ReportBean getInstance() {
		return (ReportBean)ServiceFinder.findBean("reportBean");
	}

	/**
	 * The Action method on the Generate Report button.
	 * @return null navigation string
	 */
	public String actionCreate() {
		generateReport();
		return null;
	}

	public void actionChangeArchiveDate(ValueChangeEvent event) {
		archiveDate = (Date)event.getNewValue();
		archivedCallSheets = null; // force refresh
	}

	/**
	 * The ValueChangeListener method for the "Project" drop-down list.
	 */
	public void actionChangeProject(ValueChangeEvent event) {
		if (null != event.getNewValue()) {
			try {
				unitAllDL = null; // force refresh of the Unit drop-down
				peopleUnitId = 0;
			}
			catch (Exception e) {
				MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
				EventUtils.logError(e);
			}
		}
	}

	/**
	 * The ValueChangeListener method for the "Unit" drop-down list. Changes the
	 * unit selected.
	 */
	public void actionChangeUnit(ValueChangeEvent event) {
		if (null != event.getNewValue()) {
			try {
				int newId = (Integer)event.getNewValue();
				setSelectedUnitId(newId);
				updateUnit(selectedUnitId);
			}
			catch (Exception e) {
				MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
				EventUtils.logError(e);
			}
		}
	}

	/**
	 * Action method for the "Create" button on any of the daily report tabs.
	 * @return null navigation string
	 */
	public String actionOpenCreateReport() {
		project = ProjectDAO.getInstance().refresh(project);
		unit = UnitDAO.getInstance().refresh(unit);
		try {
			if (selectedType.equals(TYPE_DAILY_CALLSHEET)) {
				newDate = findNextReportDate(callSheetList, unit);
			}
			else if (selectedType.equals(TYPE_DAILY_DPR)) {
				newDate = findNextReportDate(dprList, project.getMainUnit());
			}
			else { // exhibit G
				newDate = findNextReportDate(exhibitGList, project.getMainUnit());
			}
			showCreateReport = true;
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Create button on the Create New Report pop-up,
	 * used for creating DPR, ExhibitG, and Call Sheet objects.  The 'newDate'
	 * field should already be set from a control in the pop-up.
	 * @return navigation string to jump to proper page
	 */
	public String actionCreateReport() {
		String nav = createNewReport(newDate);
		if (nav != null) {
			showCreateReport = false;
			// Set prior-tab, so "Return" button(s) come back to Report page
			SessionUtils.put(Constants.ATTR_PRIOR_TAB_IX, HeaderViewBean.REPORTS_MENU_REPORTS_IX);
		}
		return nav;
	}

	public String actionCancelCreate() {
		showCreateReport = false;
		return null;
	}

	public String actionDeleteReport() {
		log.debug("id=" + removeReportId);
		boolean checkbox = false;
		if (removeReportId != null && selectedType.equals(TYPE_DAILY_CALLSHEET)) {
			Callsheet cs = CallsheetDAO.getInstance().findById(removeReportId);
			if (cs != null && cs.getStatus() == ReportStatus.PUBLISHED) {
				Date now = new Date();
				if (cs.getDate().after(now) || cs.getCallTime().after(now)) {
					checkbox = true; // ask to confirm sending notifications
				}
			}
		}
		if (checkbox) {
			PopupCheckboxBean.getInstance().prompt(this, ACT_DELETE_REPORT,
					"Report." + selectedType + ".DeleteCheck.", true, null);
		}
		else {
			PopupBean.getInstance().show(
					this, ACT_DELETE_REPORT,
					"Report." + selectedType+".Delete.Title",
					"Report." + selectedType+".Delete.Text",
					"Report.Delete.Ok",
					"Confirm.Cancel");
		}
//		addClientResize();
		return null;
	}

	public String actionDeleteReportOk() {
		log.debug("id=" + removeReportId);
		try {
			if (removeReportId != null) {
				if (selectedType.equals(TYPE_DAILY_CALLSHEET)) {
					CallsheetDAO callsheetDAO = CallsheetDAO.getInstance();
					Callsheet cs = callsheetDAO.findById(removeReportId);
					if (cs != null) {
						callsheetDAO.remove(cs);
						if (cs.getStatus() == ReportStatus.PUBLISHED) {
							if (PopupCheckboxBean.getInstance().getCheck()) {
								Date now = new Date();
								if (cs.getDate().after(now) || cs.getCallTime().after(now)) {
									DoNotification no = DoNotification.getInstance();
									no.callsheetDeleted(CallSheetViewBean.getCallsheetContacts(cs),
											unit, cs.getDate());
								}
							}
						}
					}
					callSheetList = null; // force refresh
				}
				else if (selectedType.equals(TYPE_DAILY_DPR)) {
					Dpr dpr = DprDAO.getInstance().findById(removeReportId);
					if (dpr != null) {
						DprDAO.getInstance().delete(dpr);
					}
					dprList = null; // force refresh
				}
				else if (selectedType.equals(TYPE_DAILY_EXHIBITG)) {
					ExhibitGDAO exhibitGDAO = ExhibitGDAO.getInstance();
					ExhibitG exg = exhibitGDAO.findById(removeReportId);
					if (exg != null) {
						exhibitGDAO.delete(exg);
					}
					exhibitGList = null; // force refresh
				}
			}
//			addClientResize();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	public String actionOpenArchivedCallsheet() {
		String name = Constants.ARCHIVE_CALLSHEET + '/' + getArchivedName();
		openArchive(name);
		return null;
	}

	/**
	 * Action method on the "location report" button, on the Real World Location
	 * view page.  The 'locationId' should have been set already via
	 * a f:setPropertyActionListener tag.
	 */
	public String actionPrintLocationReport() {
		openReportPopupWindow = true;
		try {
			if (locationId != null) {
				setSelectedType(TYPE_LOCATION);
				if (! generateReport()) {
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

	public String actionClearAllDood() {
		for (int i=0; i < TYPE_COUNT; i++) {
			includeInDood[i] = false;
		}
		return null;
	}

	public String actionClearAllElements() {
		for (int i=0; i < TYPE_COUNT; i++) {
			includeInElement[i] = false;
		}
		return null;
	}

	public String actionSelectAllDood() {
		for (int i=0; i < TYPE_COUNT; i++) {
			includeInDood[i] = true;
		}
		return null;
	}

	public String actionSelectAllElements() {
		for (int i=0; i < TYPE_COUNT; i++) {
			includeInElement[i] = true;
		}
		return null;
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE_REPORT:
				res = actionDeleteReportOk();
				break;
		}
		return res;
	}

	/**
	 * Called when user clicks "Cancel" on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
//		addClientResize();
		return null;
	}

	/**
	 * Create our standard watermark string, based on the User's
	 * name or Contact name (if available) and today's date.
	 */
	private void createWaterMark(boolean includeDate) {
		waterMark = ReportUtils.createWaterMark(getvUser(), includeDate);
	}

	/**
	 * Generate a PDF of some part of the script, either to be displayed to the
	 * current user, or email'd to a group of Contact's. Called from the
	 * ScriptPageBean.
	 *
	 * @param reporter The ScriptReporter object which has already been
	 *            initialized with the necessary information to create the
	 *            ScriptReport entries which will be processed by the jasper
	 *            generation call.
	 * @param contacts If this is null or an empty Collection, then the
	 *            generated PDF will be displayed to the current user (opened in
	 *            the browser). If this is a non-null Collection with at least
	 *            one entry, then the PDF will not be displayed to the user, but
	 *            will instead be email'd to each of the Contact's included in
	 *            the Collection.
	 * @param date The date to be displayed as part of the subject line and/or
	 *            message text if this Script PDF will be mailed to a list of
	 *            Contact's.
	 * @param watermark True iff a watermark should be applied to the resulting
	 *            PDF.
	 * @param watermarkDate True iff today's date should be appended to the
	 *            watermark text (following the user's name).
	 * @param emailSubject The subject line of the email message to be sent.
	 * @param emailBody The body of the email message to be sent.
	 * @return The real (OS) fully-qualified name of the generated PDF or XLS
	 *         file. null is returned if the report generation failed.
	 */
	public String generateScript(ScriptReporter reporter, String sender, Collection<Contact> contacts, Date date,
			boolean watermark, boolean watermarkDate, Integer colorStyle, String emailSubject, String emailBody) {
		setSelectedType(TYPE_SCRIPT);
		waterMark = null;
		if (watermark) {
			createWaterMark(watermarkDate); // sets 'waterMark' field
			if (reporter.getSidesStyle()) {
				waterMark.setTwoUp(true);
			}
		}
		setupScript(reporter); // Creates ScriptReport data -- ready for Jasper to print.
		parameters.put("colorStyle", colorStyle); // page coloring - full/stripe/etc

		if (contacts != null && contacts.size() > 0) {
			WaterMark wm = waterMark; // save it
			waterMark = null;	// don't watermark original file
			openReportPopupWindow = false; // don't show PDF
			generateReport(); // Create the PDF (to be emailed)
			openReportPopupWindow = true;
			if (watermark) {
				wm.setIncludeDate(watermarkDate); // pass the date setting
				waterMark = wm;	// restore the WaterMark
			}
			String originalName = reporter.getScript().getDescription();
			originalName = ReportUtils.makePdfName(originalName);
			MessageHandler handler = MessageHandler.getInstance();
			handler.setProject(project);
			handler.setProductionName(project.getProduction().getTitle());
			// Send the generated Script PDF to the supplied list of Contacts
			handler.sendFileToAll(contacts, emailSubject, emailBody, genCompleteFilePath, originalName, wm);
		}
		else {
			generateReport();
		}
		return genCompleteFilePath;
	}

	/**
	 * Generate a single StartForm report.
	 * @param startForm the StartForm to print.
	 * @param fileNamePdf Name for the generated pdf.
	 * @param docStatus Document status; currently used to control "VOID" watermark in jasper reports.
	 */
	public void generateStartForm(StartForm startForm, String fileNamePdf, String docStatus) {
		setSelectedType(TYPE_START_FORM);
		String sqlQuery = ReportQueries.startForm;
		Production prod = SessionUtils.getCurrentOrViewedProduction();

		if (startForm.getProject() != null) {
			reportName = JASPER_START_FORM_AICP;
		}
		else {
			reportName = JASPER_START_FORM;
			if (prod != null && prod.getType().isTours()) {
				sqlQuery = ReportQueries.startFormTours;
				reportName = JASPER_START_FORM_TOURS;
			}
		}
		sqlQuery += " sf.id = " + startForm.getId();
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("formId", startForm.getId());
		parameters.put("sfDAO", StartFormDAO.getInstance());
		parameters.put("docStatus", docStatus);
		if (prod != null && prod.getPayrollPref().getPayrollService() != null) {
			parameters.put("isTeamPayroll", prod.getPayrollPref().getPayrollService().getTeamPayroll());
		}
		else {
			parameters.put("isTeamPayroll", false);
		}

		generateReport(false, fileNamePdf);
	}

	/**
	 * Generate a single Form I9 report.
	 * @param form the FormI9 to print.
	 * @param fileNamePdf Name for the generated pdf.
	 */
	public void generateFormI9(FormI9 form, String fileNamePdf, Integer contactDocumentId, String docStatus) {
		setSelectedType(TYPE_FORM_I9);
		log.debug("FormI9 = " + form.getId());
		log.debug("ContactDocumentId = " + contactDocumentId);
		if (form.getVersion() == FormI9.I9_VERSION_2013) {
			reportName = JASPER_FORM_I9;
		}
		else if( form.getVersion() == FormI9.I9_VERSION_2017 
				|| form.getVersion() == FormI9.I9_VERSION_2017_B){
			reportName = JASPER_FORM_I9_17;
		}
		else if( form.getVersion() == FormI9.I9_VERSION_2020){
			reportName = JASPER_FORM_I9_20;
		}
		
		String sqlQuery = ReportQueries.formI9Query;
		sqlQuery += form.getId();
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("contactDocId", contactDocumentId);
		parameters.put("ssnDAO", FormI9DAO.getInstance());
		parameters.put("docStatus", docStatus);
		generateReport(false, fileNamePdf);
	}

	/**
	 * Prints an image attachment on report.
	 * @param atc the Attachment to print.
	 * @param fileNamePdf Name for the generated pdf.
	 * @param atcImagePath the fully-qualified path to the image file.
	 */
	public void printImageAttachment(Attachment atc, String fileNamePdf, String atcImagePath) {
		setSelectedType(TYPE_IMG_ATTACHMENT);
		log.debug("Attachment Id = " + atc) ;
		log.debug("Attachment = " + atcImagePath) ;
		reportName = JASPER_IMG_ATTACHMENT;
		String sqlQuery = ReportQueries.imgAttachmentQuery;
		sqlQuery += atc.getId();
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("attachmentImagePath", atcImagePath);
		generateReport(false, fileNamePdf);
	}

	/**
	 * Generate a single Form Indemnification report.
	 * @param form the FormIndem to print.
	 * @param fileNamePdf Name for the generated pdf.
	 */
	public void generateFormIndem(FormIndem form, String fileNamePdf, Integer contactDocumentId, String docStatus) {
		setSelectedType(TYPE_FORM_INDEM);
		log.debug("FormIndem = " + form.getId());
		log.debug("ContactDocumentId = " + contactDocumentId);
		reportName = JASPER_FORM_INDEM;
		String sqlQuery = ReportQueries.formIndemQuery;
		sqlQuery += form.getId();
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("contactDocId", contactDocumentId);
		parameters.put("formIndemDAO", FormIndemDAO.getInstance());
		parameters.put("docStatus", docStatus);
		generateReport(false, fileNamePdf);
	}

	/**
	 * Generate a single Form Mta report.
	 * @param form the FormMta to print.
	 * @param fileNamePdf Name for the generated pdf.
	 */
	public void generateFormMta(FormMTA form, String fileNamePdf, Integer contactDocumentId, String docStatus) {
		setSelectedType(TYPE_FORM_MTA);
		log.debug("FormMTA = " + form.getId());
		log.debug("ContactDocumentId = " + contactDocumentId);
		reportName = JASPER_FORM_MTA;
		String sqlQuery = ReportQueries.formMtaQuery;
		sqlQuery += form.getId();
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("contactDocId", contactDocumentId);
		parameters.put("formMtaDAO", FormMtaDAO.getInstance());
		parameters.put("docStatus", docStatus);
		generateReport(false, fileNamePdf);
	}

	/**
	 * Generate a single Form Actra Contract report.
	 * @param form the Form Actra Contract to print.
	 * @param fileNamePdf Name for the generated pdf.
	 */
	public void generateFormActraContract(FormActraContract form, String fileNamePdf, Integer contactDocumentId, String docStatus) {
		setSelectedType(TYPE_FORM_ACTRA_CONTRACT);
		if (form != null) {
			log.debug("FormActraContract = " + form.getId());
			log.debug("ContactDocumentId = " + contactDocumentId);
			reportName = JASPER_FORM_ACTRA_CONTRACT;
			String sqlQuery = ReportQueries.formActraContractQuery;
			sqlQuery += form.getId();
			parameters.put(PARAMETER_QUERY, sqlQuery);
			parameters.put("contactDocId", contactDocumentId);
			parameters.put("ssnDAO", FormActraContractDAO.getInstance());
			parameters.put("docStatus", docStatus);
			generateReport(false, fileNamePdf);
		}
	}

	/**
	 * Generate a Form Actra Intent report.
	 * @param form the Form Intent to print.
	 */
	public void generateFormActraIntent(FormActraIntent form, String fileNamePdf) {
		setSelectedType(TYPE_FORM_ACTRA_INTENT);
		if (form != null) {
			log.debug("FormActraIntent = " + form.getId());
			reportName = JASPER_FORM_ACTRA_INTENT;
			String sqlQuery = ReportQueries.formActraIntentQuery;
			sqlQuery += form.getId();
			boolean isEmptyPerformers = false;
			isEmptyPerformers = (form.getTalent() == null || form.getTalent().isEmpty());
			parameters.put(PARAMETER_QUERY, sqlQuery);
			parameters.put("ssnDAO", FormActraIntentDAO.getInstance());
			parameters.put("isEmptyPerformers", isEmptyPerformers);
			generateReport(false, fileNamePdf);
		}
	}

	/**
	 * Generate a Timesheet report for a week ending date.
	 * @param timesheetId of the timesheet to print.
	 * @param prodId of the timesheet to print.
	 * @param fileNamePdf Name for the generated pdf.
	 * @param workplace Home or Touring
	 * @param batched - determines which query to use to generate the report
	 * @param show If true, open a browser window on the file to show it to current user.
	 *
	 * @return The fully-qualified name of the report file created.
	 */
	public String generateToursTimesheet(Integer timesheetId, String prodId, String fileNamePdf, String workplace, boolean batched, boolean show, int numDays) {
		String sqlQuery = ReportQueries.toursTimesheetBatchIdQuery;
		String limitQuery = "";

		if(!batched) {
			sqlQuery = ReportQueries.toursTimesheetNoBatchIdQuery;
		}
		sqlQuery = sqlQuery + timesheetId;

		// Setting the report name to be called for printing as per num days
		if (numDays == 7) {
			setSelectedType(TYPE_TOURS_WEEKLY_TIMESHEET);
			reportName = JASPER_TOURS_WEEKLY_TIMESHEET;
		}
		else if (numDays == 14) {
			setSelectedType(TYPE_TOURS_BW_TIMESHEET);
			reportName = JASPER_TOURS_BW_TIMESHEET;
		}
		else if (numDays == 15) {
			setSelectedType(TYPE_TOURS_SM_TIMESHEET);
			reportName = JASPER_TOURS_SM_TIMESHEET;
		}
		else if (numDays == 16) {
			setSelectedType(TYPE_TOURS_16DAY_TIMESHEET);
			reportName = JASPER_TOURS_16DAY_TIMESHEET;
		}
		else {
			setSelectedType(TYPE_TOURS_MONTHLY_TIMESHEET);
			reportName = JASPER_TOURS_MONTHLY_TIMESHEET;
			limitQuery =  " limit 1";
		}

		sqlQuery = sqlQuery + limitQuery;

		openReportPopupWindow = show;
		log.debug("Timesheet=" + timesheetId + ", prod id = " + prodId);
		List<Integer> timesheetDayIds =  TimesheetDayDAO.getInstance().
				findIntegerListByNamedQuery(TimesheetDay.GET_TIMESHEET_DAY_ID_LIST_BY_TIMESHEET_ID, map("timesheetId", timesheetId));
		if (timesheetDayIds == null || timesheetDayIds.isEmpty()) {
			log.debug("Blank Timesheet report");
			timesheetDayIds = new ArrayList<>(numDays);
			for (int i = - numDays; i < 0; i++) {
				timesheetDayIds.add(i);
			}
			log.debug("timesheetDayIds list size = " + timesheetDayIds.size());
		}
		parameters.put(PARAMETER_QUERY, sqlQuery);
		parameters.put("timesheetDAO", TimesheetDAO.getInstance());
		parameters.put("timesheetDayIdList", timesheetDayIds);
		parameters.put("workplace", workplace);
		parameters.put("timesheetId", timesheetId);
		generateReport(false, fileNamePdf);
		return genCompleteFilePath;
	}

	/**
	 * Generate a Start Form Info (Rate Sheet) report, either printed
	 * or exported as an XLS file.
	 *
	 * @param ids List of StartForm.id values of the StartForm`s to be included
	 *            in the report.
	 * @param export True iff the data should be exported rather than printed.
	 */
	public boolean generateStartFormList(List<Integer> ids, boolean export, String orderBy) {
		setSelectedType(TYPE_START_FORM_LIST);
		reportName = JASPER_START_FORM_LIST;
		if (getProject().getProduction().getType().isAicp()) {
			reportName = JASPER_START_FORM_LIST_AICP;
		}

		String sqlQuery = ReportQueries.startFormList;
		sqlQuery += " ( 0," ; // won't match; prevents SQL error if "ids" is empty
		for (Integer id : ids) {
			sqlQuery += id + ",";
		}
		sqlQuery = sqlQuery.substring(0, sqlQuery.length()-1) + ")";
		if (orderBy == null) {
			orderBy = " last_name, first_name ";
		}
		sqlQuery += " order by " + orderBy;
		parameters.put(PARAMETER_QUERY, sqlQuery);

		parameters.put("sfDAO", StartFormDAO.getInstance());

		return generateReport(export, null);
	}

	/**
	 * SelectionListener for the list of report types.
	 */
	public void listenReportSelection(SelectEvent evt) {
		Item rowItem = (Item)evt.getObject();
		int row = rowItem.getId();
		setSelectedTab(0);
		if (selectedItem != null) {
			selectedItem.setSelected(false);
		}
		selectedItem = reportStyleList.get(row);
		selectedItem.setSelected(true);
		// Hightlight the selected row.
		selectRowState(selectedItem);
		setSelectedType(reportTypeList.get(row));
		project = ProjectDAO.getInstance().refresh(project);
		setvUser(UserDAO.getInstance().refresh(getvUser()));
//		addClientResize();
		log.debug("row=" + row);
	}

	/**
	 * Called when user clicks on a row in the Task Status table.
	 * Currently unused.
	 * @param evt RowSelectorEvent created by the Icefaces framework.
	 */
	public void listenStatusSelection(SelectEvent evt) {
		//int row = evt.getRow();
	}

	/**
	 * Generate the report type specified in selectedType.
	 *
	 * @return True if a report was generated.
	 */
	private boolean generateReport() {
		return generateReport(false, null);
	}

	/**
	 * Generate the report type specified in selectedType.
	 *
	 * @param pExport True if an XLS file should be created instead of a PDF.
	 * @param fileNamePdf Name of the pdf to be generated.
	 * @return True if a report was generated.
	 */
	private boolean generateReport(boolean pExport, String fileNamePdf) {
		boolean bRet = true;
		refresh();
		try {
			project = ProjectDAO.getInstance().refresh(project);
			Project reportProject = project;
			Unit reportUnit = unit;	// default unit for report generation
			if (selectedType.equalsIgnoreCase(TYPE_LOCATION)) {
				setupLocationReport();
			}
			else if (selectedType.equalsIgnoreCase(TYPE_PEOPLE)) {
				bRet = setupContactReport();
				if (! peopleProjectId.equals(0)) {
					reportProject = ProjectDAO.getInstance().findById(peopleProjectId);
				}
			}
			else if (selectedType.equalsIgnoreCase(TYPE_ELEMENTS)) {
				if (getSelectedTab() == TAB_LOCATIONS) {
					// as of 11/12/2010 we're dropping this report for now, but
					// I'll leave the code intact as the report will probably be
					// needed at some point.
//					setupLocationIssueReport();
				}
				else if (getSelectedTab() == TAB_ELEMENTS) {
					bRet = setupElements();
				}
				else { // DOOD
					bRet = setupDood();
				}
				reportUnit = unit; // DooD & element update Unit
			}
			else if (selectedType.equalsIgnoreCase(TYPE_BREAKDOWN)) {
				sbUnit = unit;
				bRet = setupBreakdownReport();
				reportUnit = sbUnit; // may have been changed.
			}
			else if (selectedType.equalsIgnoreCase(TYPE_STRIPBOARD)) {
				sbUnit = unit;
				bRet = setupStripboard();
				reportUnit = sbUnit; // may have been changed.
			}

			if (unit == null) { // Element report (& others?) may null this out temporarily
				unit = project.getMainUnit(); // restore before another caller attempts to use it
			}

			if (! bRet) {
				return bRet;
			}
			genCompleteFilePath = ReportGenerator.generateReport(reportProject, reportUnit, parameters, reportName,
					null, waterMark, openReportPopupWindow, pExport, fileNamePdf);
			if (genCompleteFilePath == null) {
				MsgUtils.addFacesMessage("Report.Failed", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}

		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Report.Failed", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		return bRet;
	}

	private void createReportStyleList(AuthorizationBean authBean) {
		showCast = authBean.hasPageField(Constants.PGKEY_VIEW_CAST);
		showCrew = authBean.hasPageField(Constants.PGKEY_VIEW_CREW);
		showHidden = authBean.hasPermission(getvContact(), project, Permission.VIEW_CONTACTS_HIDDEN);
//		showAdmin = authBean.isAdmin();

		if (showCast || showCrew) {
			reportStyleList.add(new Item(0, Constants.REPORT_TYPE_PEOPLE ));
			reportTypeList.add(TYPE_PEOPLE);
			if ( ! showCrew) {
				includeCrewInPeople = false;
				includeCastInPeople = true;
			}
		}
		if (authBean.hasPermission(getvContact(), project, Permission.VIEW_SCENE)) {
			reportStyleList.add(new Item(1, Constants.REPORT_TYPE_BREAKDOWN ));
			reportTypeList.add(TYPE_BREAKDOWN);
			reportStyleList.add(new Item(2, Constants.REPORT_TYPE_DOOD ));
			reportTypeList.add(TYPE_ELEMENTS);
		}
		if (authBean.hasPermission(getvContact(), project, Permission.VIEW_STRIPBOARD)) {
			reportStyleList.add(new Item(3, Constants.REPORT_TYPE_STRIPBOARD ));
			reportTypeList.add(TYPE_STRIPBOARD);
		}
		if (authBean.hasPageField(Constants.PGKEY_VIEW_CS_LIST)) {
			reportStyleList.add(new Item(4, Constants.REPORT_TYPE_CALLSHEET ));
			reportTypeList.add(TYPE_DAILY_CALLSHEET);
		}
		if (authBean.hasPageField(Constants.PGKEY_VIEW_EX_G)) {
			reportStyleList.add(new Item(5, Constants.REPORT_TYPE_EXHIBITG ));
			reportTypeList.add(TYPE_DAILY_EXHIBITG);
		}
		if (authBean.hasPageField(Constants.PGKEY_VIEW_DPR)) {
			reportStyleList.add(new Item(6, Constants.REPORT_TYPE_DPR ));
			reportTypeList.add(TYPE_DAILY_DPR);
		}

		int ix = 0;
		for (String s : reportTypeList) {
			if (s.equalsIgnoreCase(selectedType)) {
				selectedItem = reportStyleList.get(ix);
				selectedItem.setSelected(true);
				// Hightlight the selected row.
				selectRowState(selectedItem);
				//setTabHeading(selectedItem.getName());
				//log.debug("select match, ix=" + ix);
				break;
			}
			ix++;
		}
	}

	/**
	 * Create the Task Status table, which displays the status of the call
	 * sheet, Exhibit G, and DPR reports for the shooting dates of the current
	 * project from the first day of shooting through today.
	 */
	private void createTaskStatusList() {
		taskStatusList = new ArrayList<>();
		Calendar cal = CalendarUtils.getInstance(new Date());
		cal = getScheduleUtils().findNextWorkDate(cal);
		if (cal == null || cal.getTime().after(getScheduleUtils().getEndDate()) ) {
			cal = CalendarUtils.getInstance(getScheduleUtils().getEndDate());
		}
		List<Date> dates = getScheduleUtils().getShootingDatesList();
		int dayn = getScheduleUtils().findShootingDayNumber(cal.getTime());
		while(dayn > 0) {
			Date date = dates.get(dayn-1);
			taskStatusList.add(createTaskStatus(date));
			dayn--;
		}
	}

	/**
	 * Create one line item (TaskStatus) for the Task Status table on the
	 * Reports page.
	 *
	 * @param date The date that this line documents.
	 * @return A new TaskStatus giving the status of the relevant reports for
	 *         the specified date.
	 */
	private TaskStatus createTaskStatus(Date date) {
		TaskStatus ts = new TaskStatus(date); // status entries initialized as "missing"
		try {
			Callsheet cs = CallsheetDAO.getInstance().findByDateAndProjectMain(date, project);
			if (cs != null) {
				if (cs.getStatus() == ReportStatus.APPROVED || cs.getStatus() == ReportStatus.PUBLISHED) {
					ts.callSheetStatus = TaskStatus.STATUS_DONE;
				}
				else {
					ts.callSheetStatus = TaskStatus.STATUS_IN_PROGRESS;
				}
			}
			Dpr dpr = DprDAO.getInstance().findByDateAndProject(date, project);
			if (dpr != null) {
				if (dpr.getStatus() == ReportStatus.APPROVED || dpr.getStatus() == ReportStatus.PUBLISHED) {
					ts.dprStatus = TaskStatus.STATUS_DONE;
				}
				else {
					ts.dprStatus = TaskStatus.STATUS_IN_PROGRESS;
				}
			}
			ExhibitG exg = ExhibitGDAO.getInstance().findByDateAndProject(date, project);
			if (exg != null) {
				if (exg.getStatus() == ReportStatus.APPROVED || exg.getStatus() == ReportStatus.PUBLISHED) {
					ts.exhibitGStatus = TaskStatus.STATUS_DONE;
				}
				else {
					ts.exhibitGStatus = TaskStatus.STATUS_IN_PROGRESS;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return ts;
	}

	/**
	 * Creates the list of Call sheets displayed on the right-hand part of the Report
	 * page when the "Call sheet" report category is selected.  Each line item in the
	 * list is a {@link DailyReportItem}.
	 */
	private void createCallSheetList() {
		callSheetList = new ArrayList<>();
		try {
			List<Callsheet> list = CallsheetDAO.getInstance().findByProjectAndUnit(project, unit);
			for (Callsheet cs : list) {
				callSheetList.add(makeItem(cs.getId(), cs.getDate(), cs.getStatus(),
						cs.getUpdated(), cs.getShootDay()) );
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}


	/**
	 * Creates the list of DPRs displayed on the right-hand part of the Report
	 * page when the "Production Report" report category is selected.  Each line item in the
	 * list is a {@link DailyReportItem}.
	 */
	private void createDprList() {
		dprList = new ArrayList<>();
		try {
			List<Dpr> list = DprDAO.getInstance().findByProject(project);
			for (Dpr dpr : list) {
				dprList.add(makeItem(dpr.getId(), dpr.getDate(), dpr.getStatus(),
						dpr.getUpdated(), dpr.getShootDay() ) );
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}


	/**
	 * Creates the list of Exhibit Gs displayed on the right-hand part of the Report
	 * page when the "SAG Exhibit G" report category is selected.  Each line item in the
	 * list is a {@link DailyReportItem}.
	 */
	private void createExhibitGList() {
		exhibitGList = new ArrayList<>();
		try {
			getScheduleUtils().refresh(); // fix lazy init errors
			List<ExhibitG> list = ExhibitGDAO.getInstance().findByProject(project);
			for (ExhibitG ex : list) {
				int dayn = getScheduleUtils().findShootingDayNumber(ex.getDate());
				exhibitGList.add(makeItem(ex.getId(), ex.getDate(), ex.getStatus(), ex.getUpdated(), dayn ) );
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Create a line item for one of the lists of daily reports (Call sheets,
	 * DPRs, Exhibit Gs) that appear on the right-hand side of the Report page.
	 *
	 * @param id The database id of the report object.
	 * @param date The date that the report relates to (NOT the creation date).
	 * @param status The report status, e.g., In Progress.
	 * @param modified The timestamp of the last time the report was saved.
	 * @param shootDay The shooting day of the production (or project) that this
	 *            report is for.
	 * @return A new {@link DailyReportItem} reflecting the provided values.
	 */
	private DailyReportItem makeItem(Integer id, Date date, ReportStatus status,
			Date modified, Integer shootDay) {
		int shDay = (shootDay == null ? 0 : shootDay);
		DailyReportItem item = new DailyReportItem(id, date, status, modified, shDay);
		return item;
	}

	private List<SelectItem> createArchivedCallsheetDateDL() {
		List<SelectItem> list = new ArrayList<>();
		List<Date> dates = ArchiveUtils.findCallsheetDates(unit);
		if (dates.size() > 0) {
			archiveDate = dates.get(0);
			DateFormat df = new SimpleDateFormat("M/dd/yyyy");
			for (Date d : dates) {
				list.add(new SelectItem(d, df.format(d)));
			}
		}
		else {
			archiveDate = new Date(); // just don't want it null
		}
		log.debug("count=" + list.size());
		archivedCallSheets = createArchivedCallSheets();
		return list;
	}

	private List<Document> createArchivedCallSheets() {
		List<Document> list = null;
		list = ArchiveUtils.findCallsheets(unit, archiveDate);
		return list;
	}

	private void createSceneNumberList() {
		// For populating Scene number drop down in Shooting Schedule Report
		sceneNumbers = new ArrayList<>();
		if (project.getScript() != null) {
			List<Scene> list = project.getScript().getScenes();
			for (Scene s : list) {
				toSheetTypeInBreakdown = s.getSequence().toString(); // will end up with the last value
				sceneNumbers.add(new SelectItem(toSheetTypeInBreakdown, s.getNumber()));
			}
		}
		if (sceneNumbers.size() == 0) { // avoid other problems by making dummy entry
			sceneNumbers.add(new SelectItem( "0", "0"));
			toSheetTypeInBreakdown = "0";
		}
		toSheetTypeInShootingSch = toSheetTypeInBreakdown;
		fromSheetTypeInBreakdown = (String)sceneNumbers.get(0).getValue();
		fromSheetTypeInShootingSch = fromSheetTypeInBreakdown;
	}

	public static final String REPORT_ID_DATE_STYLE = "yyMMddHHmmss";
	private String createReportId() {
		String id;
		if (getvUser() != null) {
			id = "" + getvUser().getId();
		}
		else {
			id = "" + (int)(Math.random()*10000.0);
		}
		DateFormat df = new SimpleDateFormat(REPORT_ID_DATE_STYLE);
		id += "-" + df.format(new Date());
		return id;
	}

	/**
	 * Called to create a new daily report, after the user has selected the date
	 * in the New Report pop-up.
	 * @return The navigation string to go to the appropriate page, probably
	 * the edit page for the newly created report.
	 */
	private String createNewReport(Date reportDate) {
		reportDate = CalendarUtils.setTime(reportDate, 0, 0); // set to beginning of day
		project = ProjectDAO.getInstance().refresh(project);
		log.debug("report Type: " + selectedType + ", date: " + reportDate +
				", proj=" + project.getId() + ", unit#=" + unit.getNumber());
		try {
			if (selectedType.equalsIgnoreCase(TYPE_DAILY_EXHIBITG)) {
				ExhibitG exhibitG = ExhibitGDAO.getInstance().findByDateAndProject(reportDate, project);
				if (exhibitG != null) {
					log.debug("exhibitg already exist ");
					MsgUtils.addFacesMessage("ExhibitG.Duplicate", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				SessionUtils.put(ExhibitGViewBean.ATTR_EXHIBITG_NEW_DATE, reportDate);
				SessionUtils.put(ExhibitGViewBean.ATTR_EXHIBITG_EDIT, true);
				return "exhibitGedit";
			}
			else if (selectedType.equalsIgnoreCase(TYPE_DAILY_DPR)) {
				Dpr dpr = DprDAO.getInstance().findByDateAndProject(reportDate, project);
				if (dpr != null) {
					log.debug("dpr already exists");
					MsgUtils.addFacesMessage("Dpr.Duplicate", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				else {
					SessionUtils.put(DprViewBean.ATTR_NEW_DATE, reportDate);
				}
				return HeaderViewBean.REPORTS_MENU_DPR_EDIT;
			}
			else if (selectedType.equalsIgnoreCase(TYPE_DAILY_CALLSHEET)) {
				return createNewCallsheet(reportDate, unit);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Create a new call sheet and return the navigation String to jump to the
	 * Call Sheet Edit page.
	 *
	 * @param date
	 * @return The string to navigate to the Edit page, if successful, and null
	 *         if the create failed.
	 */
	private String createNewCallsheet(Date date, Unit rptUnit) {
		if (date == null) {
			MsgUtils.addFacesMessage("Callsheet.DateMissing", FacesMessage.SEVERITY_ERROR);
		}
		else {
			if (CallSheetUtils.validateDate(date, rptUnit)) {
				Callsheet cs = CallSheetUtils.create(date, rptUnit);
				if (cs != null) {
					CallsheetDAO.getInstance().store(cs);
					SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, cs.getId());
//					SessionUtils.put(CallSheetViewBean.ATTR_CALLSHEET_EDIT, true);
					SessionUtils.put(Constants.ATTR_HEADER_TAB_ID, HeaderViewBean.REPORTS_MENU_REPORTS);
					return HeaderViewBean.REPORTS_MENU_CALLSHEET_EDIT;
				}
				MsgUtils.addFacesMessage("Callsheet.NoScenesToShoot", FacesMessage.SEVERITY_ERROR);
			}
		}
		return null;
	}

	private void openArchive(String name) {
		String fileName = ArchiveUtils.retrieveItem(name, unit);
		if (fileName != null) {
			String javascriptCode =
				"window.open('../../" + fileName + "','archiveWindow');";
			log.debug("js: "+javascriptCode);
			addJavascript(javascriptCode);
		}
	}

	/**
	 * Generate location reports for each of the locations in the callsheet, and
	 * send the reports to the list of Contacts given. Normally called as a
	 * result of the "make final" action on the Callsheet.
	 *
	 * @param cs The callsheet from which to get the location information.
	 * @param directionContacts The list of Contacts to receive the report.
	 */
	/*package*/ void sendLocDirReport(Callsheet cs, List<Contact> directionContacts) {
		List<String> reports = new ArrayList<>();
		List<Integer> sentIds = new ArrayList<>();
		openReportPopupWindow = false;
		try {
			for (SceneCall sc : cs.getSceneCalls()) {
				locationId = RealWorldElementDAO.getLocationId(sc);
				if (locationId != null && ! sentIds.contains(locationId)) {
					// don't send duplicate reports
					setSelectedType(TYPE_LOCATION);
					generateReport();
					reports.add(genCompleteFilePath);
					sentIds.add(locationId);
				}
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		openReportPopupWindow = true;
		if (reports.size() > 0) {
			DoNotification no = DoNotification.getInstance();
			no.callsheetLocationPublished(directionContacts, unit, cs.getDate(), reports, sentIds);
		}
	}

	/**
	 * Setup parameters for a Location report.
	 */
	private void setupLocationReport() {
		setReportName(JASPER_LOCATION_REPORT);
		sqlQry = ReportQueries.locReptQry + locationId;

		parameters.put(PARAMETER_QUERY, sqlQry);
	}

	/**
	 *  sets .jrxml, images file path for the various Contact reports -- Crew and/or Cast,
	 *  and Compact, Medium, or Detailed.
	 *  NOTE that the SQL query for the assistant information is embedded in the
	 *  jrxml file for the assistant sub-report, contLstDetsubrpt.jrxml.
	 */
	private boolean setupContactReport() {
//		if (getSelectedTab() == TAB_CONTACT_SHEET) {
//			return setupContactSheet();
//		}

		if ( ! isIncludeCastInPeople() && ! isIncludeCrewInPeople() && ! isIncludeVendors()) {
			MsgUtils.addFacesMessage("Report.People.SelectGroup", FacesMessage.SEVERITY_ERROR);
			enableGenerateButton();
			return false;
		}

		String contactQry;

		if (peopleReportStyle.equalsIgnoreCase(CONTACT_LIST_COMPACT)) {
			if (showHidden) {
				contactQry = ReportQueries.contLstCompQry;
			}
			else { // use query that does not extract hidden fields
				contactQry = ReportQueries.contLstCompQryHidden;
			}
		}
		else { // "contLstMed" - detailed (5-line)
			peopleReportStyle = CONTACT_LIST_MEDIUM; // in case framework set it to empty! (happened once)
			if (showHidden) {
				contactQry = ReportQueries.contLstMedQrySelect;
			}
			else { // use SELECT that does not extract hidden fields
				contactQry = ReportQueries.contLstMedQrySelectHidden;
			}
//			if ( ! groupByDepartmentInPeople) { // for un-grouped, one more select phrase...
				// rev 2783: remove this - we are now listing all roles; fixes bug in rev 2523.
//				contactQry += ReportQueries.contLstMedQryUngroupedClause;
//			}
			// then append FROM and WHERE clauses:
			contactQry += ReportQueries.contLstMedQryFromWhere;
		}

		if (includeCrewInPeople) {
			if ( ! includeCastInPeople) {
				contactQry += ReportQueries.contNotTalent;
			}
			if ( ! includeVendors) {
				contactQry += ReportQueries.excludeVendors;
			}
		}
		else { // crew is false; cast or vendors must be true
			if (includeCastInPeople) {
				contactQry += ReportQueries.contTalentB;
				if (includeVendors) {
					contactQry += " or " + ReportQueries.vendorsOnly;
				}
				contactQry += " ) ";
			}
			else {
				contactQry += " and " + ReportQueries.vendorsOnly;
			}
		}

		if (! peopleProjectId.equals(0)) {
			contactQry += " and proj.id = " + peopleProjectId + " ";
			if (! peopleUnitId.equals(0)) {
				contactQry += " and unit.id = " + peopleUnitId + " ";
			}
		}

		setReportName(getPeopleReportStyle());
		if ( ! groupByDepartmentInPeople) {
			setReportName(getPeopleReportStyle() + JASPER_CONTACTS_UNGRP_SUFFIX);
			// 12/27/11 - keep multiple roles in ungrouped report
			// contactQry += ReportQueries.contLstQryUngroupedSuffix;
		}
		String orderBy = " unitNumber, ";
		if (groupByDepartmentInPeople) {
			orderBy += " dept.name, ";
		}
		if (orderByInPeople != null && orderByInPeople.equalsIgnoreCase(CONTACT_REPORT_ORDER_NAME)) {
			orderBy += " user.last_name, user.first_name, user.id, role.list_priority;";
		}
		else { // order by job title
			orderBy += " role.name, user.last_name, user.first_name ;";
		}
		sqlQry = contactQry + " order by " + orderBy;
		parameters.put(PARAMETER_QUERY, sqlQry);
		log.debug("qry=" + sqlQry);
		return true;
	}

	/**
	 * Initial parameter checks for the Day out of Days report.
	 *
	 * @return True if report generation should proceed; false if an error
	 *         message was issued and no further processing should take place.
	 */
	private boolean setupDood() {
		updateUnit(elemUnitId);
		if (project.getStripboard() == null || project.getStripboard().getShootingDays(unit) < 1) {
			if (project.getHasUnits()) {
				MsgUtils.addFacesMessage("Report.DooD.NoDays.Unit", FacesMessage.SEVERITY_ERROR);
			}
			else {
				MsgUtils.addFacesMessage("Report.DooD.NoDays", FacesMessage.SEVERITY_ERROR);
			}
			enableGenerateButton();
			return false;
		}
		boolean bRet = false;
		for (int i=0; i < TYPE_COUNT; i++) {
			if (includeInDood[i]) { // require at least one item selected
				bRet = true;
				break;
			}
		}
		if (bRet) {
			setupDayOutOfDaysReport();
		}
		else {
			MsgUtils.addFacesMessage("Report.DooD.Category", FacesMessage.SEVERITY_ERROR);
			enableGenerateButton();
		}
		return bRet;
	}

	/**
	 * Set up parameters for Day out of days report, and call the DooD report
	 * generation code to build the table records required.
	 */
	private void setupDayOutOfDaysReport() {
		String report_id = createReportId();
		log.debug("report_id=" + report_id);


		DoodReportGen gen = new DoodReportGen();
		gen.generate(report_id, unit, includeNewPageInDayOut);

		String doodRepQuery = ReportQueries.doodRep;
		doodRepQuery += " where report_id = '" + report_id + "' and (type_name ='heading' ";

		for (int i = 0; i < TYPE_COUNT; i++) {
			doodRepQuery = addElementTypeDooD(doodRepQuery, includeInDood[i], ScriptElementType.values()[i]);
		}

		log.debug(orderByInDayOut);
		if (orderByInDayOut.equalsIgnoreCase(DOOD_ORDER_DATE)) {
			doodRepQuery += ") order by segment, type, sequence, start, element_name; ";
		}
		else if (orderByInDayOut.equalsIgnoreCase(DOOD_ORDER_ID)) {
			doodRepQuery += ") order by segment, type, sequence, cast_id_num, element_name;";
		}
		else { // order by work-days
			doodRepQuery += ") order by segment, type, sequence, work desc, hold desc, element_name;";
		}

		sqlQry = doodRepQuery;
		log.debug("doodRepQuery: " + doodRepQuery);
		setReportName(JASPER_DOOD);
		parameters.put(PARAMETER_QUERY, sqlQry);
	}

	/**
	 * Add a little piece to the Element report query string, based on the parameters...
	 * @param query The existing query string, to be appended to
	 * @param include True if the specified element type is included in the report.
	 * @param type  The element type in question.
	 * @return Query string, either the same as was passed if "include" is false, or
	 * with the addition of the type check for the specified element type.
	 */
	private String addElementTypeDooD(String query, boolean include, ScriptElementType type) {
		if (include && type != ScriptElementType.N_A) {
			query += "or type_name = '" + type.toRwString() + "' ";
		}
		return query;
	}

	/**
	 * Initial parameter checks for the Elements report.
	 *
	 * @return True if report generation should proceed; false if an error
	 *         message was issued and no further processing should take place.
	 */
	private boolean setupElements() {
		boolean bRet = false;
		updateUnit(elemUnitId);
		if (project.getStripboard() == null ||
				(elemUnitId != 0 && project.getStripboard().getShootingDays(unit) < 1)) {
			if (project.getHasUnits()) {
				MsgUtils.addFacesMessage("Report.DooD.NoDays.Unit", FacesMessage.SEVERITY_ERROR);
			}
			else {
				MsgUtils.addFacesMessage("Report.DooD.NoDays", FacesMessage.SEVERITY_ERROR);
			}
			enableGenerateButton();
			return false;
		}

		for (int i=0; i < TYPE_COUNT; i++) {
			if (includeInElement[i]) { // require at least one item selected
				bRet = true;
				break;
			}
		}

		if (bRet) {
			setupElementReport();
		}
		else {
			MsgUtils.addFacesMessage("Report.Elements.Category", FacesMessage.SEVERITY_ERROR);
			enableGenerateButton();
		}
		return bRet;
	}

	/**
	 * Set up parameters for an element report.  The report will pull Start and Finish dates
	 * directly from the Dood_report table, so we need to generate a current one.
	 */
	private void setupElementReport() {
		String report_id = createReportId();
		DoodReportGen gen = new DoodReportGen();
		if (elemUnitId == 0) { // unit=All selected
			gen.generateElementDateReport(report_id, project);
			unit = null;
		}
		else {
			gen.generate(report_id, unit, false);
		}
		String elementQuery;
		String selectedTypesList = "";
		for (int i = 0; i < TYPE_COUNT; i++) {
			selectedTypesList = addElementType(selectedTypesList, includeInElement[i], ScriptElementType.values()[i]);
		}
		if (selectedTypesList.startsWith(",")) {
			selectedTypesList = selectedTypesList.substring(1);
		}
		if (getRequirementStatInElement().equals(REQUIRED_MISSING)) {
			elementQuery = ReportQueries.elementListUnselected;
			if (elemUnitId != 0) {
				elementQuery = ReportQueries.elementListUnselectedUnit;
			}
		}
		else if (getRequirementStatInElement().equals(REQUIRED_SATISFIED)) {
			elementQuery = ReportQueries.elementListSelected;
			if (elemUnitId != 0) {
				elementQuery = ReportQueries.elementListSelectedUnit;
			}
		}
		else {
			elementQuery = ReportQueries.elementListAll;
			if (elemUnitId != 0) {
				elementQuery = ReportQueries.elementListAllUnit;
			}
		}

		// don't let formatText format id's - it adds commas!
		elementQuery = MsgUtils.formatText(elementQuery, ""+projId, selectedTypesList,
				ReportQueries.elementTypes, ""+elemUnitId );

		String orderBy = " order by element_type, element_id, name;";
		if (orderByInElement.equals(ORDER_OCCURS)) {
			// Order by occurrences is not currently supported!
		}

		if (includeNewPageInElement) {
			if (includeSceneInElement) {
				setReportName(JASPER_ELEM_SCENE_PAGED);
			}
			else {
				setReportName(JASPER_ELEM_PAGED);
			}
		}
		else {
			if (includeSceneInElement) {
				setReportName(JASPER_ELEM_SCENE);
			}
			else {
				setReportName(JASPER_ELEMENTS);
			}
		}

		parameters.put(PARAMETER_QUERY, elementQuery + orderBy);
		parameters.put("reportId", report_id);
		Integer scriptId = 0;
		if (project.getScript() != null) {
			scriptId = project.getScript().getId();
		}
		parameters.put("scriptId", scriptId);

	}

	/**
	 * Add a little piece to the Element report query string, based on the parameters...
	 * @param query The existing query string, to be appended to
	 * @param include True if the specified element type is included in the report.
	 * @param type  The element type in question.
	 * @return Query string, either the same as was passed if "include" is false, or
	 * with the addition of the type check for the specified element type.
	 */
	private String addElementType(String query, boolean include, ScriptElementType type) {
		if (include && type != ScriptElementType.N_A) {
			query += ", '" + type.name() + "'";
		}
		return query;
	}

	/**
	 * Create the query and do other initialization for the Breakdown Sheet and
	 * Shooting Schedule reports. This method does parameter validation, then
	 * calls setupBreakdownReport(boolean) to do the "real work".
	 *
	 * @return True if the setup was successful -- no errors encountered --
	 *         meaning that the Jasper report call should be made.
	 */
	private boolean setupBreakdownReport() {
		boolean breakdown = (getSelectedTab() == TAB_BREAKDOWN);
		if (project.getScript() == null || project.getStripboard() == null) {
			MsgUtils.addFacesMessage("Report.Breakdown.NoScriptStripboard", FacesMessage.SEVERITY_ERROR);
			enableGenerateButton();
			return false;
		}
		if ((breakdown && includeInBreakdown.equalsIgnoreCase(RANGE_DATE)) ||
				(! breakdown && includeInScene.equalsIgnoreCase(RANGE_DATE)) ) {
			if (fromDateInBrkDown == null || toDateInBrkDown == null) {
				MsgUtils.addFacesMessage("Report.Breakdown.MissingDate", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
			if (fromDateInBrkDown.after(toDateInBrkDown)) {
				MsgUtils.addFacesMessage("Report.Breakdown.DateRange", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
			if (fromDateInBrkDown.after(getScheduleUtils().getEndDate())) {
				MsgUtils.addFacesMessage("Report.Breakdown.DateAfter", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
			if (toDateInBrkDown.before(getScheduleUtils().getStartDate())) {
				MsgUtils.addFacesMessage("Report.Breakdown.DateBefore", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
		}

		if (breakdown) {
			if (includeInBreakdown.equalsIgnoreCase(RANGE_SCENE)
					&& (Integer.parseInt(fromSheetTypeInBreakdown) > Integer.parseInt(toSheetTypeInBreakdown))) {
				MsgUtils.addFacesMessage("Report.Breakdown.SceneRange", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
		}
		else {
			if (includeInScene.equalsIgnoreCase(RANGE_SCENE)
					&& (Integer.parseInt(fromSheetTypeInShootingSch) > Integer
							.parseInt(toSheetTypeInShootingSch))) {
				MsgUtils.addFacesMessage("Report.Breakdown.SceneRange", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
		}

		boolean bRet;
		if (breakdown || shootingReportStyle.equals(SHOOTING_STYLE_STANDARD)) {
			bRet = setupBreakdownReport(breakdown);
		}
		else {
			bRet = setupOneLineShootingScheduleReport();
		}
		return bRet;
	}

	/**
	 * Create the query and do other initialization for both the Breakdown Sheet
	 * and Shooting Schedule reports. Some parameter validation was already
	 * done, but we still might determine that the report should not be run --
	 * for example, there may be no scheduled Strip`s within the date range
	 * selected.
	 *
	 * @param breakdown True if the request is for a Breakdown report, false for
	 *            a Shooting Schedule report.
	 * @return True if the setup was successful -- no errors encountered --
	 *         meaning that the Jasper report call should be made.
	 */
	private boolean setupBreakdownReport(boolean breakdown) {
		boolean bRet = true;
		String query;
		boolean scheduleOrder = true;
		if (breakdown) { // breakdown (not shooting schedule) report
			setReportName(JASPER_BREAKDOWN);
			query = ReportQueries.breakdownSceneQry + addProjectId;
			log.debug(includeInBreakdown);
			if (includeInBreakdown.equalsIgnoreCase(INCLUDE_ALL)) {
				if (orderByInBreakDown.equalsIgnoreCase(ORDER_SCENE)) {
					scheduleOrder = false;
					sbUnit = null; // all units included
				}
				else { // schedule order
					query += " and sp.Status <> '" + StripStatus.OMITTED.name() + "' ";
				}
			}
			else if (includeInBreakdown.equalsIgnoreCase(RANGE_SCENE)) {
				log.debug("scene range: " + fromSheetTypeInBreakdown + " to " + toSheetTypeInBreakdown);
				query += " and sce.sequence between " + fromSheetTypeInBreakdown +
						" and " + toSheetTypeInBreakdown;
				scheduleOrder = false;
				sbUnit = null; // all units included
			}
			else { // Date range
				query += " and sp.Unit_Id = " + selectedUnitId + " ";
				String range = setupSceneRange(true);
				if (range != null) {
					query += range;
				}
				else {
					MsgUtils.addFacesMessage("Report.Breakdown.RangeEmpty", FacesMessage.SEVERITY_ERROR);
					enableGenerateButton();
					return false;
				}
			}
		}
		else { // Shooting Schedule report
			firstShootDayNumber = 1;
			if (orderByInShootingSch.equalsIgnoreCase(ORDER_SCENE) ||
					includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
				scheduleOrder = false;
				query = ReportQueries.shootingScheduleSceneQry;
				setReportName(JASPER_SCHEDULE);
			}
			else {
				query = ReportQueries.shootingScheduleDateQry;
				setReportName(JASPER_SCHEDULE_DATE);
			}
			query += addProjectId;
			if (includeInScene.equalsIgnoreCase(INCLUDE_ALL)) {
				if (scheduleOrder) {
					query += " and sp.Status <> '" + StripStatus.OMITTED.name() + "' ";
				}
			}
			else if (includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
				query += " and sce.sequence between " + fromSheetTypeInShootingSch
						+ " and " + toSheetTypeInShootingSch;
			}
			else { // date range
				query += " and sp.Unit_Id = " + selectedUnitId + " ";
				String range = setupSceneRange(false);
				if (range != null) {
					query += range;
				}
				else {
					MsgUtils.addFacesMessage("Report.Breakdown.RangeEmpty", FacesMessage.SEVERITY_ERROR);
					enableGenerateButton();
					return false;
				}
			}
			if (scheduleOrder) {
				query += " group by ordernumber, unit_number ";
				List<Date> dates;
				if (includeInScene.equalsIgnoreCase(INCLUDE_ALL)) {
					dates = new ArrayList<>();
					for (Unit u : project.getUnits()) {
						ScheduleUtils su = new ScheduleUtils(u);
						dates.addAll(su.getShootingDatesList());
					}
				}
				else {
					dates = getScheduleUtils().getShootingDatesList();
				}
				List<String> shootDates = new ArrayList<>(dates.size() + 2);
				DateFormat df = new SimpleDateFormat("MMM dd, yyyy");
				String s = "";
				for (int i = firstShootDayNumber-1; i < dates.size(); i++) {
					s = df.format(dates.get(i));
					shootDates.add(s); // Add 2 of each date to ...
					shootDates.add(s); // ... allow Jasper code to work.
					log.debug(s);
				}
				shootDates.add(s); // Add 2 extra dates to ...
				shootDates.add(s); // ... allow Jasper code to handle un-scheduled scenes.
				parameters.put("shootDates", shootDates);
			}
		}

		String orderBy;
		if (scheduleOrder) {
			parameters.put("breakdownOrder", ORDER_SCHEDULE);
			orderBy = " order by sp.Status, sp.Unit_id, sp.OrderNumber;";
		}
		else {
			parameters.put("breakdownOrder", ORDER_SCENE);
			orderBy = " order by sce.sequence;";
		}
		sqlQry = query + orderBy;
		parameters.put(PARAMETER_QUERY, sqlQry);
		return bRet;
	}

	/**
	 * Find the range of breakdown sheets (pages), using their orderNumber values, that correspond
	 * to the user's requested date range, based on the current shooting schedule.
	 * Returns an SQL fragment to be added to the query to restrict the range of data selected.
	 */
	private String setupSceneRange(boolean breakdown) {
		String range = null;
		int fromStripOrder = findFirstStripOrderNumber(fromDateInBrkDown);
		int toStripOrder = findLastStripOrderNumber(toDateInBrkDown, breakdown);
		if (fromStripOrder <= toStripOrder) {
			range = " and sp.OrderNumber between " + fromStripOrder + " and " + toStripOrder +
			" and sp.Status = '" + StripStatus.SCHEDULED.name() + "' ";
		}
		return range;
	}

	/**
	 * Set up query for the one-line shooting schedule report, and call the
	 * stripboard report-generator to create the temporary table with the
	 * appropriate data.
	 *
	 * @return True if report generation should proceed.
	 */
	private boolean setupOneLineShootingScheduleReport() {
		log.debug("");
		Date startDate = null;
		Date endDate = null;
		boolean scheduleOrder = true;
		String included = INCLUDE_STRIPS_ALL;
		String reportId = createReportId();

		String query = ReportQueries.stripboardQuery;
		query += " where report_id ='" + reportId + "' ";
		query += " and type <> '" + StripType.BANNER.name() + "' ";

		firstShootDayNumber = 1;

		if (orderByInShootingSch.equalsIgnoreCase(ORDER_SCENE) ||
				includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
			scheduleOrder = false;
			reportName = JASPER_SCHEDULE_1_LINE;
		}
		else {
			reportName = JASPER_SCHEDULE_1_LINE_DATE;
		}

		if (includeInScene.equalsIgnoreCase(INCLUDE_ALL)) {
			sbUnit = null;
			if (scheduleOrder) {
				query += " and status <> '" + StripStatus.OMITTED.name() + "' ";
			}
		}
		else if (includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
			sbUnit = null;
			query += " and script_order between " + fromSheetTypeInShootingSch
					+ " and " + toSheetTypeInShootingSch;
		}
		else { // date range
			included = INCLUDE_STRIPS_SCHEDULED;
			startDate = fromDateInBrkDown;
			endDate = toDateInBrkDown;
			String range = setupSceneRange(false);
			if (range == null) {
				MsgUtils.addFacesMessage("Report.Breakdown.RangeEmpty", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				return false;
			}
			if (firstShootDayNumber != 1) { // changed; adjust startDate to match
				startDate = getScheduleUtils().findShootingDay(firstShootDayNumber);
				// this ensures stripboard report and 'shootDates' array start on same date
			}
		}

		if (scheduleOrder) {
			List<Date> dates;
			if (includeInScene.equalsIgnoreCase(INCLUDE_ALL)) {
				dates = new ArrayList<>();
				for (Unit u : project.getUnits()) {
					ScheduleUtils su = new ScheduleUtils(u);
					dates.addAll(su.getShootingDatesList());
				}
			}
			else {
				dates = getScheduleUtils().getShootingDatesList();
			}
			List<String> shootDates = new ArrayList<>(dates.size() + 2);
			DateFormat df = new SimpleDateFormat("MMM dd, yyyy");
			String s = "";
			for (int i = firstShootDayNumber-1; i < dates.size(); i++) {
				s = df.format(dates.get(i));
				shootDates.add(s); // Add 2 of each date to ...
				shootDates.add(s); // ... allow Jasper code to work.
				log.debug(s);
			}
			shootDates.add(s); // Add 2 extra dates to ...
			shootDates.add(s); // ... allow Jasper code to handle un-scheduled scenes.
			parameters.put("shootDates", shootDates);
		}

		String orderBy;
		if (scheduleOrder) {
			parameters.put("breakdownOrder", ORDER_SCHEDULE);
			orderBy = " order by istatus, sequence";
		}
		else {
			parameters.put("breakdownOrder", ORDER_SCENE);
			orderBy = " order by script_order;";
		}
		parameters.put(PARAMETER_QUERY, query + orderBy);

		StripboardReportGen stripboardReportGen = new StripboardReportGen();
		boolean bRet = stripboardReportGen.generate(reportId, included,
				sbUnit, startDate, endDate, !scheduleOrder, project);

		return bRet;
	}

	private int findFirstStripOrderNumber(Date date) {
		int fromStripOrder = Integer.MAX_VALUE;
		int shootDay = getScheduleUtils().findShootingDayNumber(date);
		if (shootDay == 0) {
			Calendar cal = getScheduleUtils().findNextWorkDate(date);
			if (cal != null) {
				date = cal.getTime();
				shootDay = getScheduleUtils().findShootingDayNumber(date);
			}
		}
		if (shootDay != 0) {
			int shootingDays = project.getStripboard().getShootingDays(getUnit());
			List<Strip> strips = null;
			StripDAO stripDAO = StripDAO.getInstance();
			for ( ; shootDay <= shootingDays; shootDay++) {
				strips = stripDAO.findByShootDay(project.getStripboard(), getUnit(), shootDay);
				if (strips.size() > 0) {
					Strip strip = strips.get(0); // get first strip of starting date
					fromStripOrder = strip.getOrderNumber();
					firstShootDayNumber = shootDay;
					break;
				}
			}
		}
		return fromStripOrder;
	}

	private int findLastStripOrderNumber(Date date, boolean breakdown) {
		int toStripOrder = 0;
		int shootDay = getScheduleUtils().findShootingDayNumber(date);
		if (shootDay == 0) {
			Calendar cal = getScheduleUtils().findPreviousWorkDate(CalendarUtils.getInstance(date));
			if (cal != null) {
				date = cal.getTime();
				shootDay = getScheduleUtils().findShootingDayNumber(date);
			}
		}
		StripDAO stripDAO = StripDAO.getInstance();
		for ( ; shootDay > 0; shootDay--) {
			// for Shooting schedule (not Breakdown) report, include End-of-day strips.
			List<Strip> strips = stripDAO.findByShootDay(project.getStripboard(), getUnit(), shootDay, ! breakdown);
			if (strips.size() > 0) {
				Strip strip = strips.get(strips.size()-1); // get last Strip of ending date
				toStripOrder = strip.getOrderNumber();
				break;
			}
		}
		return toStripOrder;
	}

	/**
	 * Find the next work (shooting) date following the date of the first item
	 * in the list. We use the first item, since our report lists are all sorted
	 * in descending date order, so the first item should be the most current.
	 * If there are no items, the first shooting date will be returned. If there
	 * is no shooting date found after the date of the first report item,
	 * today's date is returned.
	 *
	 * @param list
	 *            The DailyReportItem list to use as the source of the most
	 *            recent existing report.
	 * @return The next appropriate date for a working-day report.
	 */
	private Date findNextReportDate(List<DailyReportItem> list, Unit rptUnit) {
		Date lastDate = null;
		if (list.size() > 0) {
			lastDate = list.get(0).getDate();
		}
		project = ProjectDAO.getInstance().refresh(project);
		sDays = new ScheduleUtils(rptUnit); // refresh scheduler
		Calendar cal = getScheduleUtils().findNextWorkDate(lastDate);
		if (cal != null) {
			lastDate = cal.getTime();
		}
		else { // no 'next' shooting date found -- end of production?
			lastDate = new Date();
		}
		return lastDate;
	}

	/**
	 * Internal setup for the Script PDF.  Generates a report id, and calls
	 * ScriptUtils to build all the data as temporary records in the ScriptReport table.
	 *
	 * @param reporter The ScriptReporter object to be called to create the
	 * script data that will be printed.
	 */
	private void setupScript(ScriptReporter reporter) {
		String report_id = createReportId();
		log.debug("report_id=" + report_id);

		reporter.createReport(report_id);

		sqlQry = ReportQueries.scriptPages;
		sqlQry += " where report_id = '" + report_id + "' ";
		sqlQry += " order by id; ";  // was: page_number, line_number

		if (reporter.getSidesStyle()) {
			if (reporter.getSidesType() == ScriptReporter.SIDES_TYPE_SEQUENTIAL) {
				setReportName(JASPER_SCRIPT_SIDES_1);
			}
			else {
				setReportName(JASPER_SCRIPT_SDES_2);
			}
		}
		else {
			setReportName(JASPER_SCRIPT);
		}
		parameters.put(PARAMETER_QUERY, sqlQry);
	}

	private boolean setupStripboard() {
//		if (dateRangeInStripboard) {
			if (fromDateInStripboard == null || toDateInStripboard == null) {
				MsgUtils.addFacesMessage("Report.Stripboard.MissingDates", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				log.debug("missing stripboard date range");
				return false;
			}
			if (fromDateInStripboard.after(toDateInStripboard)) {
				MsgUtils.addFacesMessage("Report.Stripboard.StartDate", FacesMessage.SEVERITY_ERROR);
				enableGenerateButton();
				log.debug("reversed stripboard date range");
				return false;
			}
//		}
		boolean haveData = setupStripboardReport();
		if ( ! haveData) {
			MsgUtils.addFacesMessage("Report.Stripboard.NoMatch", FacesMessage.SEVERITY_ERROR);
			enableGenerateButton();
			log.debug("no strips found in range");
			return false;
		}
		return true;
	}

	/**
	 * Generate data in the stripboard_report table for the jasper report to
	 * use. Validates some parameters and builds parameters for the
	 * StripboardReportGen class to use.
	 * <p>
	 * On entry, 'sbUnit' is set to the Unit value selected in the drop-down;
	 * but this is only used for the "scheduled only" case. Otherwise, we set it
	 * to null here, so that a unit name will not be passed in the jasper
	 * parameters.
	 *
	 * @return True if data was generated; false if none was generated, either
	 *         due to invalid parameters, or no stripboard data falling within
	 *         the requested ranges.
	 */
	private boolean setupStripboardReport() {
		log.debug("Inside stripboardReports");
		Date startDate = null;
		Date endDate = null;
		boolean orderByScript = false; // Schedule or Script/scene order?

		String reportId = createReportId();

		String stripboardQuery = ReportQueries.stripboardQuery;
		stripboardQuery += " where report_id ='" + reportId + "' ";

		if (styleByInStripboard.equalsIgnoreCase(STRIPBOARD_STYLE_THIN)) {
			setReportName(JASPER_STRIPBOARD_THIN);
		}
		else {
			setReportName(JASPER_STRIPBOARD_THICK);
		}
		if (includeStrips.equalsIgnoreCase(INCLUDE_STRIPS_ALL)) {
			sbUnit = null;
			if (orderByInStripboard.equalsIgnoreCase(STRIPBOARD_ORDER_SCENE)) {
				orderByScript = true;
				reportName += JASPER_STRIPBOARD_SCENE_SUFFIX;
				stripboardQuery += " order by script_order;";
			}
			else {
				orderByScript = false;
				reportName += JASPER_STRIPBOARD_DATE_SUFFIX;
				stripboardQuery += " order by istatus, sequence;";
			}
		}
		else if (includeStrips.equalsIgnoreCase(INCLUDE_STRIPS_SCHEDULED)) {
			orderByScript = false;
			reportName += JASPER_STRIPBOARD_DATE_SUFFIX;
			stripboardQuery += " order by istatus, sequence;";
//			if (dateRangeInStripboard) {
				startDate = fromDateInStripboard;
				endDate = toDateInStripboard;
//			}
		}
		else { // "include" must be "unscheduled only"
			sbUnit = null;
			orderByScript = true;
			reportName += JASPER_STRIPBOARD_DATE_SUFFIX;
			stripboardQuery += " order by istatus, script_order;";
		}
		// reportName += "V2"; // use for testing "V2" stripboard reports

		log.debug("stripboard sql query: " + stripboardQuery);
		parameters.put(PARAMETER_QUERY, stripboardQuery);

		StripboardReportGen stripboardReportGen = new StripboardReportGen();
		boolean bRet = stripboardReportGen.generate(reportId, includeStrips,
				sbUnit, startDate, endDate, orderByScript, project);

		return bRet;
	}

	/**
	 * Set up unit-related data fields based on the given unit database id
	 * value.
	 *
	 * @param unitId The database id of the desired Unit, or zero if "All" was
	 *            selected in a Unit drop-down list.
	 */
	private void updateUnit(Integer unitId) {
		project = ProjectDAO.getInstance().refresh(project);
		if (unitId == null || unitId.intValue() == 0) {
			unit = project.getMainUnit();
		}
		else {
			unit = UnitDAO.getInstance().findById(unitId);
		}
		sDays = new ScheduleUtils(unit); // refresh it
		callSheetList = null;
		archivedCallSheets = null;
		archivedCallsheetDateDL = null;
		fromDateInStripboard = getScheduleUtils().getStartDate();
		toDateInStripboard = getScheduleUtils().getEndDate();
		fromDateInBrkDown = getScheduleUtils().getStartDate();
		toDateInBrkDown = getScheduleUtils().getEndDate();
		override = true;
		log.debug("unit: " + unitId);
	}

	private void refresh() {
		project = ProjectDAO.getInstance().refresh(project);
		if (unit == null) {
			sDays = new ScheduleUtils(project.getMainUnit()); // refresh it
		}
		else {
			unit = UnitDAO.getInstance().refresh(unit);
			sDays = new ScheduleUtils(unit); // refresh it
		}
	}

	/**
	 * Enable the "generate report" button.
	 */
	private void enableGenerateButton() {
		addJavascript("enableGenRptBtn();");
	}

	/** See {@link #reportStyleList}. */
	public List<Item> getReportStyleList() {
		return reportStyleList;
	}
	/** See {@link #reportStyleList}. */
	public void setReportStyleList(List<Item> reportStyleList) {
		this.reportStyleList = reportStyleList;
	}

	/** See {@link #taskStatusList}. */
	public List<TaskStatus> getTaskStatusList() {
		if (taskStatusList == null) {
			createTaskStatusList();
		}
		return taskStatusList;
	}
	/** See {@link #taskStatusList}. */
	public void setTaskStatusList(List<TaskStatus> taskStatusList) {
		this.taskStatusList = taskStatusList;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public String getSelectedType() {
		return selectedType;
	}

	public void setSelectedType(String s) {
		selectedType = s;
		if (! (selectedType.equals(TYPE_LOCATION) || selectedType.equals(TYPE_SCRIPT))) {
			// never make "location" or "script" the default report type (no equivalent tabs)
			SessionUtils.put(ATTR_RPT_TYPE, selectedType);
		}

		if (selectedType.equals(TYPE_ELEMENTS)) {
			if (! AuthorizationBean.getInstance().hasPermission(getvContact(), project, Permission.VIEW_REPORTS_DOOD)) {
				setSelectedTab(TAB_ELEMENTS); // no DooD tab, select Elements tab
			}
		}
		else if (selectedType.equals(TYPE_STRIPBOARD)) {
			refresh();
			if (fromDateInStripboard == null) {
				fromDateInStripboard = getScheduleUtils().getStartDate();
			}
			if (toDateInStripboard == null) {
				toDateInStripboard = getScheduleUtils().getEndDate();
			}
		}
		else if (selectedType.equals(TYPE_BREAKDOWN)) {
			refresh();
			if (fromDateInBrkDown == null) {
				fromDateInBrkDown = getScheduleUtils().getStartDate();
			}
			if (toDateInBrkDown == null) {
				toDateInBrkDown = getScheduleUtils().getEndDate();
			}
		}
	}

	/** See {@link #selectedItem}. */
	public Item getSelectedItem() {
		return selectedItem;
	}
	/** See {@link #selectedItem}. */
	public void setSelectedItem(Item selectedItem) {
		this.selectedItem = selectedItem;
	}

	/** See {@link #projectDL}. */
	public List<SelectItem> getProjectDL() {
		if (projectDL == null) {
			projectDL = SessionUtils.createProjectList(SessionUtils.getCurrentContact(), false);
		}
		return projectDL;
	}
	/** See {@link #projectDL}. */
	public void setProjectDL(List<SelectItem> projectDL) {
		this.projectDL = projectDL;
	}

	/** See {@link #peopleProjectId}. */
	public Integer getPeopleProjectId() {
		return peopleProjectId;
	}
	/** See {@link #peopleProjectId}. */
	public void setPeopleProjectId(Integer projectPeopleId) {
		peopleProjectId = projectPeopleId;
	}

	/** See {@link #selectedUnitId}. */
	public Integer getSelectedUnitId() {
		return selectedUnitId;
	}
	/** See {@link #selectedUnitId}. */
	public void setSelectedUnitId(Integer id) {
		selectedUnitId = id;
	}

	/** See {@link #unitDL}. */
	public List<SelectItem> getUnitDL() {
		if (unitDL == null) {
			unitDL = ScheduleUtils.createUnitList(project);
		}
		return unitDL;
	}
	/** See {@link #unitDL}. */
	public void setUnitDL(List<SelectItem> unitDL) {
		this.unitDL = unitDL;
	}

	/** See {@link #unitAllDL}. */
	public List<SelectItem> getUnitAllDL() {
		if (unitAllDL == null) {
			peopleUnitId = 0; // select "All" entry by default
			Project p = ProjectDAO.getInstance().findById(peopleProjectId);
//			if (p != null) {
//				unit = p.getMainUnit();
//				this.updateUnit();
//			}
			if (p != null && p.getHasUnits()) {
				unitAllDL = ScheduleUtils.createUnitList(p);
				unitAllDL.add(0, new SelectItem(new Integer(0),"All"));
			}
			else {
				unitAllDL = new ArrayList<>();
			}
		}
		return unitAllDL;
	}
	/** See {@link #unitAllDL}. */
	public void setUnitAllDL(List<SelectItem> unitAllDL) {
		this.unitAllDL = unitAllDL;
	}

	/** See {@link #peopleUnitId}. */
	public Integer getPeopleUnitId() {
		return peopleUnitId;
	}
	/** See {@link #peopleUnitId}. */
	public void setPeopleUnitId(Integer peopleUnitId) {
		this.peopleUnitId = peopleUnitId;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	public boolean isGroupByDepartmentInContracts() {
		return groupByDepartmentInContracts;
	}
	public void setGroupByDepartmentInContracts(boolean groupByDepartmentInContracts) {
		this.groupByDepartmentInContracts = groupByDepartmentInContracts;
	}

	public boolean isFullyCompleted() {
		return fullyCompleted;
	}
	public void setFullyCompleted(boolean fullyCompleted) {
		this.fullyCompleted = fullyCompleted;
	}

	public boolean isItemsPending() {
		return itemsPending;
	}
	public void setItemsPending(boolean itemsPending) {
		this.itemsPending = itemsPending;
	}

	public boolean isIncludeCastInContract() {
		return includeCastInContract;
	}
	public void setIncludeCastInContract(boolean includeCastInContract) {
		this.includeCastInContract = includeCastInContract;
	}

	public boolean isIncludeCrewInContracts() {
		return includeCrewInContracts;
	}
	public void setIncludeCrewInContracts(boolean includeCrewInContracts) {
		this.includeCrewInContracts = includeCrewInContracts;
	}

	public String getOrderByInContracts() {
		return orderByInContracts;
	}
	public void setOrderByInContracts(String orderByInContracts) {
		this.orderByInContracts = orderByInContracts;
	}

	// CONTACT / PEOPLE fields

	public String getPeopleReportStyle() {
		return peopleReportStyle;
	}
	public void setPeopleReportStyle(String peopleReportStyle) {
		this.peopleReportStyle = peopleReportStyle;
	}

	public boolean isIncludeCastInPeople() {
		return includeCastInPeople;
	}
	public void setIncludeCastInPeople(boolean includeCastInPeople) {
		this.includeCastInPeople = includeCastInPeople;
	}

	public boolean isIncludeCrewInPeople() {
		return includeCrewInPeople;
	}
	public void setIncludeCrewInPeople(boolean includeCrewInPeople) {
		this.includeCrewInPeople = includeCrewInPeople;
	}

	public boolean isIncludeVendors() {
		return includeVendors;
	}
	public void setIncludeVendors(boolean b) {
		includeVendors = b;
	}

	public String getOrderByInPeople() {
		return orderByInPeople;
	}
	public void setOrderByInPeople(String orderByInPeople) {
		this.orderByInPeople = orderByInPeople;
	}

	public boolean isGroupByDepartmentInPeople() {
		return groupByDepartmentInPeople;
	}
	public void setGroupByDepartmentInPeople(boolean groupByDepartmentInPeople) {
		this.groupByDepartmentInPeople = groupByDepartmentInPeople;
	}

	public String getLocationStatus() {
		return locationStatus;
	}
	public void setLocationStatus(String status) {
		locationStatus = status;
	}

	/** See {@link #locationId}. */
	public Integer getLocationId() {
		return locationId;
	}
	/** See {@link #locationId}. */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	public String getReportName() {
		return reportName;
	}
	public void setReportName(String reportName) {
		this.reportName = reportName;
	}

	// STRIPBOARD

	public String getStyleByInStripboard() {
		return styleByInStripboard;
	}
	public void setStyleByInStripboard(String styleByInStripboard) {
		// had an event log where field was null, can't recreate! So check input value.
		if (styleByInStripboard != null) {
			this.styleByInStripboard = styleByInStripboard;
		}
	}

	/** See {@link #includeStrips}. */
	public String getIncludeStrips() {
		return includeStrips;
	}
	/** See {@link #includeStrips}. */
	public void setIncludeStrips(String includeStrips) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (includeStrips != null) {
			this.includeStrips = includeStrips;
		}
	}

	public String getOrderByInStripboard() {
		return orderByInStripboard;
	}
	public void setOrderByInStripboard(String orderByInStripboard) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (orderByInStripboard != null) {
			this.orderByInStripboard = orderByInStripboard;
		}
	}

	/** Returns the list of ScriptElementType enumerations for use in JSP. */
	public ScriptElementType[] getElemValues() {
		return ScriptElementType.values();
	}

	// DooD and Element report fields

	/** See {@link #unitElemAllDL}. */
	public List<SelectItem> getUnitElemAllDL() {
		if (unitElemAllDL == null) {
			elemUnitId = 0; // select "All" entry by default
			Project p = SessionUtils.getCurrentProject();
			if (p != null && p.getHasUnits()) {
				unitElemAllDL = ScheduleUtils.createUnitList(p);
			}
			else {
				unitElemAllDL = new ArrayList<>();
			}
			unitElemAllDL.add(0, new SelectItem(new Integer(0),"All"));
		}
		return unitElemAllDL;
	}
	/** See {@link #unitElemAllDL}. */
	public void setUnitElemAllDL(List<SelectItem> unitElemAllDL) {
		this.unitElemAllDL = unitElemAllDL;
	}

	/** See {@link #elemUnitId}. */
	public Integer getElemUnitId() {
		return elemUnitId;
	}
	/** See {@link #elemUnitId}. */
	public void setElemUnitId(Integer elemUnitId) {
		this.elemUnitId = elemUnitId;
	}

	/** See {@link #includeInElement}. */
	public Boolean[] getIncludeInElement() {
		return includeInElement;
	}
	/** See {@link #includeInElement}. */
	public void setIncludeInElement(Boolean[] includeInElement) {
		this.includeInElement = includeInElement;
	}

	public boolean isIncludeNewPageInElement() {
		return includeNewPageInElement;
	}
	public void setIncludeNewPageInElement(boolean includeNewPageInElement) {
		this.includeNewPageInElement = includeNewPageInElement;
	}

	public boolean isIncludeSceneInElement() {
		return includeSceneInElement;
	}
	public void setIncludeSceneInElement(boolean includeSceneInElement) {
		this.includeSceneInElement = includeSceneInElement;
	}

	public String getRequirementStatInElement() {
		return requirementStatInElement;
	}
	public void setRequirementStatInElement(String requirementStatInElement) {
		// had an event log where field was null, can't recreate! So check input value.
		if (requirementStatInElement != null) {
			this.requirementStatInElement = requirementStatInElement;
		}
	}

	/** See {@link #orderByInElement}. */
	public String getOrderByInElement() {
		return orderByInElement;
	}
	/** See {@link #orderByInElement}. */
	public void setOrderByInElement(String orderByInElement) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (orderByInElement != null) {
			this.orderByInElement = orderByInElement;
		}
	}

	/** See {@link #includeInDood}. */
	public Boolean[] getIncludeInDood() {
		return includeInDood;
	}
	/** See {@link #includeInDood}. */
	public void setIncludeInDood(Boolean[] includeInDood) {
		this.includeInDood = includeInDood;
	}

	public boolean isIncludeNewPageInDayOut() {
		return includeNewPageInDayOut;
	}
	public void setIncludeNewPageInDayOut(boolean includeNewPageInDayOut) {
		this.includeNewPageInDayOut = includeNewPageInDayOut;
	}

	public String getOrderByInDayOut() {
		return orderByInDayOut;
	}
	public void setOrderByInDayOut(String orderByInDayOut) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (orderByInDayOut != null) {
			this.orderByInDayOut = orderByInDayOut;
		}
	}

	// Breakdown / Shooting Schedule

	public String getIncludeInScene() {
		return includeInScene;
	}
	public void setIncludeInScene(String includeInScene) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (includeInScene != null) {
			if (includeInScene.equalsIgnoreCase(RANGE_DATE)) {
				setOrderByInShootingSch(ORDER_SCHEDULE);
			}
			else if (includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
				setOrderByInShootingSch(ORDER_SCENE);
			}
			this.includeInScene = includeInScene;
		}
	}

	public String getFromSheetTypeInBreakdown() {
		if (sceneNumbers == null) {
			createSceneNumberList();
		}
		return fromSheetTypeInBreakdown;
	}
	public void setFromSheetTypeInBreakdown(String fromSheetTypeInBreakdown) {
		this.fromSheetTypeInBreakdown = fromSheetTypeInBreakdown;
	}

	public String getToSheetTypeInBreakdown() {
		if (sceneNumbers == null) {
			createSceneNumberList();
		}
		return toSheetTypeInBreakdown;
	}
	public void setToSheetTypeInBreakdown(String toSheetTypeInBreakdown) {
		this.toSheetTypeInBreakdown = toSheetTypeInBreakdown;
	}

	public String getIncludeInBreakdown() {
		return includeInBreakdown;
	}
	public void setIncludeInBreakdown(String includeInBreakdown) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (includeInBreakdown != null) {
			if (includeInBreakdown.equalsIgnoreCase(RANGE_DATE)) {
				setOrderByInBreakDown(ORDER_SCHEDULE);
			}
			else if (includeInBreakdown.equalsIgnoreCase(RANGE_SCENE)) {
				setOrderByInBreakDown(ORDER_SCENE);
			}
			this.includeInBreakdown = includeInBreakdown;
		}
	}

	public String getOrderByInBreakDown() {
		return orderByInBreakDown;
	}
	public void setOrderByInBreakDown(String order) {
		if (includeInBreakdown.equalsIgnoreCase(RANGE_DATE)) {
			order = ORDER_SCHEDULE;
		}
		else if (includeInBreakdown.equalsIgnoreCase(RANGE_SCENE)) {
			order = ORDER_SCENE;
		}
		if (order != null) {
			orderByInBreakDown = order;
		}
	}

	/** See {@link #shootingReportStyle}. */
	public String getShootingReportStyle() {
		return shootingReportStyle;
	}
	/** See {@link #shootingReportStyle}. */
	public void setShootingReportStyle(String shootingReportStyle) {
		// have seen cases where radio button values got set to null, shouldn't happen!
		if (shootingReportStyle != null) {
			this.shootingReportStyle = shootingReportStyle;
		}
	}

	public String getOrderByInShootingSch() {
		return orderByInShootingSch;
	}
	public void setOrderByInShootingSch(String order) {
		if (includeInScene.equalsIgnoreCase(RANGE_DATE)) {
			order = ORDER_SCHEDULE;
		}
		else if (includeInScene.equalsIgnoreCase(RANGE_SCENE)) {
			order = ORDER_SCENE;
		}
		if (order != null) {
			orderByInShootingSch = order;
		}
	}

	public String getFromSheetTypeInShootingSch() {
		if (sceneNumbers == null) {
			createSceneNumberList();
		}
		return fromSheetTypeInShootingSch;
	}
	public void setFromSheetTypeInShootingSch(String fromSheetTypeInShootingSch) {
		this.fromSheetTypeInShootingSch = fromSheetTypeInShootingSch;
	}

	public String getToSheetTypeInShootingSch() {
		if (sceneNumbers == null) {
			createSceneNumberList();
		}
		return toSheetTypeInShootingSch;
	}
	public void setToSheetTypeInShootingSch(String toSheetTypeInShootingSch) {
		this.toSheetTypeInShootingSch = toSheetTypeInShootingSch;
	}

	public List<SelectItem> getSceneNumbers() {
		if (sceneNumbers == null) {
			createSceneNumberList();
		}
		return sceneNumbers;
	}
	public void setSceneNumbers(List<SelectItem> sceneNumbers) {
		this.sceneNumbers = sceneNumbers;
	}

	public Date getFromDateInStripboard() {
		return fromDateInStripboard;
	}
	public void setFromDateInStripboard(Date fromDateInStripboard) {
		if (!override) {
			this.fromDateInStripboard = fromDateInStripboard;
		}
	}

	public Date getToDateInStripboard() {
		return toDateInStripboard;
	}
	public void setToDateInStripboard(Date toDateInStripboard) {
		if (!override) {
			this.toDateInStripboard = toDateInStripboard;
		}
		override = false;
	}

	public Date getFromDateInBrkDown() {
		return fromDateInBrkDown;
	}
	public void setFromDateInBrkDown(Date fromDateInBrkDown) {
		if (!override) {
			this.fromDateInBrkDown = fromDateInBrkDown;
		}
	}

	public Date getToDateInBrkDown() {
		return toDateInBrkDown;
	}
	public void setToDateInBrkDown(Date toDateInBrkDown) {
		if (!override) {
			this.toDateInBrkDown = toDateInBrkDown;
		}
		override = false;
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	// * * * DAILY REPORTS * * *

	/** See {@link #showCreateReport}. */
	public boolean isShowCreateReport() {
		return showCreateReport;
	}
	/** See {@link #showCreateReport}. */
	public void setShowCreateReport(boolean showCreateReport) {
		this.showCreateReport = showCreateReport;
	}

	/** See {@link #removeReportId}. */
	public Integer getRemoveReportId() {
		return removeReportId;
	}
	/** See {@link #removeReportId}. */
	public void setRemoveReportId(Integer removeReportId) {
		this.removeReportId = removeReportId;
	}

	/** See {@link #newDate}. */
	public Date getNewDate() {
		return newDate;
	}
	/** See {@link #newDate}. */
	public void setNewDate(Date newDate) {
		this.newDate = newDate;
	}

	/** See {@link #dprList}. */
	public List<DailyReportItem> getDprList() {
		if (dprList == null) {
			createDprList();
		}
		return dprList;
	}
	/** See {@link #dprList}. */
	public void setDprList(List<DailyReportItem> dprList) {
		this.dprList = dprList;
	}

	/** See {@link #callSheetList}. */
	public List<DailyReportItem> getCallSheetList() {
		if (callSheetList == null) {
			createCallSheetList();
		}
		return callSheetList;
	}
	/** See {@link #callSheetList}. */
	public void setCallSheetList(List<DailyReportItem> callSheetList) {
		this.callSheetList = callSheetList;
	}

	/** See {@link #archiveDate}. */
	public Date getArchiveDate() {
		return archiveDate;
	}
	/** See {@link #archiveDate}. */
	public void setArchiveDate(Date archiveDate) {
		this.archiveDate = archiveDate;
	}

	/** See {@link #archivedCallSheets}. */
	public List<Document> getArchivedCallSheets() {
		if (archivedCallsheetDateDL == null) { // setting this affects archiveDate, which affects callsheet list!
			archivedCallsheetDateDL = createArchivedCallsheetDateDL();
		}
		if (archivedCallSheets == null) {
			archivedCallSheets = createArchivedCallSheets();
		}
		return archivedCallSheets;
	}
	/** See {@link #archivedCallSheets}. */
	public void setArchivedCallSheets(List<Document> archivedCallSheets) {
		this.archivedCallSheets = archivedCallSheets;
	}

	/** See {@link #archivedCallsheetDateDL}. */
	public List<SelectItem> getArchivedCallsheetDateDL() {
		if (archivedCallsheetDateDL == null) {
			archivedCallsheetDateDL = createArchivedCallsheetDateDL();
		}
		return archivedCallsheetDateDL;
	}
	/** See {@link #archivedCallsheetDateDL}. */
	public void setArchivedCallsheetDateDL(List<SelectItem> archivedCallsheetDateDL) {
		this.archivedCallsheetDateDL = archivedCallsheetDateDL;
	}

	/** See {@link #archivedName}. */
	public String getArchivedName() {
		return archivedName;
	}
	/** See {@link #archivedName}. */
	public void setArchivedName(String archivedName) {
		this.archivedName = archivedName;
	}

	/** See {@link #exhibitGList}. */
	public List<DailyReportItem> getExhibitGList() {
		if (exhibitGList == null) {
			createExhibitGList();
		}
		return exhibitGList;
	}
	/** See {@link #exhibitGList}. */
	public void setExhibitGList(List<DailyReportItem> exhibitGList) {
		this.exhibitGList = exhibitGList;
	}

	/** See {@link #openReportPopupWindow}. */
	public boolean getOpenReportPopupWindow() {
		return openReportPopupWindow;
	}
	/** See {@link #openReportPopupWindow}. */
	public void setOpenReportPopupWindow(boolean openReportPopupWindow) {
		this.openReportPopupWindow = openReportPopupWindow;
	}

	private ScheduleUtils getScheduleUtils() {
		if (sDays == null) {
			sDays = new ScheduleUtils(unit);
		}
		return sDays;
	}

	/** See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}

	/** See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/**
	 * Select a row in the current list by creating an ace RowState object and
	 * adding to the {@link #stateMap}.
	 *
	 * @param entry The object (list item) to be selected.
	 */
	private void selectRowState(Object entry) {
		RowState state = new RowState();
		state.setSelected(true);
		stateMap.clear();
		stateMap.put(entry, state);
		//setSelectedRow(entry);
	}

	/**
	 * A simple data-holding class for backing the "Task Status" list presented on
	 * the Report page.
	 */
	public static class TaskStatus implements Serializable {
		/** */
		private static final long serialVersionUID = - 4977991920331230019L;

		public static final int STATUS_MISSING = 0;
		public static final int STATUS_IN_PROGRESS = 1;
		public static final int STATUS_DONE = 2;

		Date date;
		int callSheetStatus;
		int exhibitGStatus;
		int dprStatus;
		boolean selected = false;

		public TaskStatus() {
			//rand(); // may be used during testing to generate random status values
		}
		public TaskStatus(Date dd) {
			date = dd;
			callSheetStatus = STATUS_MISSING;
			exhibitGStatus = STATUS_MISSING;
			dprStatus = STATUS_MISSING;
		}

//		private void rand() { // Used during testing/development
//			callSheetStatus = (int)(Math.random() * 3);
//			exhibitGStatus = (int)(Math.random() * 3);
//			dprStatus = (int)(Math.random() * 3);
//		}

		/** See {@link #date}. */
		public Date getDate() {
			return date;
		}
		/** See {@link #date}. */
		public void setDate(Date date) {
			this.date = date;
		}

		/** See {@link #callSheetStatus}. */
		public int getCallSheetStatus() {
			return callSheetStatus;
		}
		/** See {@link #callSheetStatus}. */
		public void setCallSheetStatus(int callSheetStatus) {
			this.callSheetStatus = callSheetStatus;
		}

		/** See {@link #exhibitGStatus}. */
		public int getExhibitGStatus() {
			return exhibitGStatus;
		}
		/** See {@link #exhibitGStatus}. */
		public void setExhibitGStatus(int exhibitGStatus) {
			this.exhibitGStatus = exhibitGStatus;
		}

		/** See {@link #dprStatus}. */
		public int getDprStatus() {
			return dprStatus;
		}
		/** See {@link #dprStatus}. */
		public void setDprStatus(int status) {
			dprStatus = status;
		}

		/** See {@link #selected}. */
		public boolean getSelected() {
			return selected;
		}
		/** See {@link #selected}. */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}

}
