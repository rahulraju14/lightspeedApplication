// File Name: HeaderViewBean.java
package com.lightspeedeps.web.util;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.*;
import javax.faces.event.*;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.home.HomePageBean;
import com.lightspeedeps.web.login.*;
import com.lightspeedeps.web.view.*;

/**
 * Provides methods to support displaying the header portion of all LS pages,
 * e.g., the current production title and list of Projects.
 */
@SuppressWarnings("unused")
@ManagedBean
@ViewScoped
public class HeaderViewBean implements Serializable {
	/** */
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(HeaderViewBean.class);

	private static final String ATTR_CURRENT_TAB_IX = Constants.ATTR_PREFIX + "currentTabIx";
	private static final String ATTR_MENU_TRACKING_IX = Constants.ATTR_PREFIX + "mainMenuTrackingIx";

	/** URL used to logout of ESS when user has jumped to or from ESS during this session. LS-3758 */
	private static final String ESS_LOGOUT_URL =  ApplicationUtils.getInitParameterString("ESS_URL", false) + "logout";

	/** The text that appears in the title of the tab based on the TTC Online domain  LS-1691 */
	private static final String TEAM_TAB_TITLE = "TTC Online";
	/** The text that appears in the title of the tab based on the LS domain LS-1691 */
	private static final String LS_TAB_TITLE = "LightSPEED eps";

	/** Flag that determines whether this is in the TTC Online domain LS-1691 */
	private String tabDomainTitle;

	/** Company Name based on the domain that generated the request LS-1691 */
	private String companyName;

	/** The current Production */
	private transient Production production;

	/** The Project that is currently active. Note that while the User is
	 * viewing the Project page, a different project may be selected in the
	 * project list, but that does not change THIS value. */
	private transient Project project;

	/** The currently logged-in User. */
	private transient User user;

	/** The current Contact (representing the current User's relationship
	 * with the current Production). */
	private transient Contact contact;

	/** The database id of the Project selected by the user in the
	 * drop-down list in the header area. */
	private Integer selectedProjectId;

	/** List of SelectItem`s displayed in the drop-down list at the top of
	 * the screen for episodic Production`s.  The values are Project.id and
	 * the labels are Project.title. */
	private List<SelectItem> projectTitles;

	/** True when this instance is created; set to false by the first call to
	 * {@link #getTabValue()}, which is invoked from our JSP html. */
	private boolean newInstance = true;

	/** True iff the current production, or login session, is "branded",
	 * meaning that another company's logo(s) will be used. */
	private boolean branded;

	/** The logo to display on desktop pages for a branded production. */
	private Image brandDesktopLogo;

	/** The logo to display on mobile pages for a branded production. */
	private Image brandMobileLogo;

	/** This is bound to a hidden outputText field on every page; the outputText should have
	 * an f:attribute child tag that specifies the tabId to be "selected" when the page
	 * is rendered.  This is part of the system that causes the correct tab to show as
	 * selected when we navigate to some arbitrary page from within the Java code. */
//	private transient HtmlOutputText tabName;

	/** Indicates if messages have been issued. */
	private boolean msgExists = false;

	/** True if user is on smart-phone (not tablet) device. */
	private Boolean mobile;

	/** True if user is on tablet device. */
	private Boolean tablet;

	/** Hidden input control, used for mobile pages "back" button. This is
	 * bound to an ice:inputHidden field in each page.  The value of the field
	 * is used by the {@link #actionBack()} method. */
	private transient HtmlInputHidden backAction;

	/** Field used in header.xhtml to determine which main menu item is shown as active. */
	private int mainMenuTrackingIx = 0;

	/** Tracks the current tab for this user. */
	private Tab currentTab;

	/** The number (origin 0) of the currently selected mini-tab.  This is used
	 * primarily where we have different beans supporting mini-tabs that are on the
	 * same page, e.g., on the Approver Dashboard and (Payroll) Preferences.
	 * "Normal" pages, where a single bean supports all the mini-tabs, use the
	 * View.selectedTab field for this same purpose. */
	private int miniTab;

	/** A list of all the defined tabs; the order is not important.  The index of each tab
	 *  is assigned to a static final that may be used throughout the program to cause
	 *  the displayed tab to switch to the one specified.
	 */
	private static final List<Tab> tabList = new ArrayList<>();

	/**
	 * A List of Lists of Tabs.  Each inner List<Tab> corresponds to all the sub-tabs
	 * that belong to a particular main tab, and the entries within that list represent
	 * the order in which they are displayed.
	 * The outer List then holds the List<Tab> objects for all the main tabs.  That list
	 * is ordered by the order of the main menu tabs.
	 * This structure is used by header.xhtml -- it iterates over both outer and
	 * inner lists to generate the menus.
	 */
	private static final List<List<Tab>> subMenuLists = new ArrayList<>();
	private static final List<List<Tab>> systemMenuLists = new ArrayList<>();

	/**
	 *  The values of the following statics defines the order of the main menu items.
	 *  They are used in creating the "Tab" objects which, in turn, control the sub-menu creation;
	 *  the creation process associates each group of sub-tabs with a particular main tab.
	 */

	/** ********* NOTE: if the number of main tab changes, you MUST update the value in the
	 *  ********* script_desktop.uc.js file, function findActTpNav() !!                    **********  */

	private static final int MENU_HOME_IX = 0;
	private static final int MENU_PROJECT_IX = 1;
	private static final int MENU_TALENT_PROJECT_IX = 2;
	private static final int MENU_PEOPLE_IX = 3;
	private static final int MENU_TALENT_PEOPLE_IX = 4; // Replaces Cast & Crew menu for Talent/Canada
	private static final int MENU_PAYROLL_IX = 5;
	private static final int MENU_TALENT_ONBOARDING_IX = 6; // Replaces Payroll menu for Talent/Canada
	private static final int MENU_SCRIPT_IX = 7;
	private static final int MENU_SCHEDULE_IX = 8;
	private static final int MENU_REPORTS_IX = 9;
	private static final int MENU_FILES_IX = 10;
	private static final int MENU_ADMIN_IX = 11;
	// SEE ABOVE NOTE IF NEW TOP MENU ITEMS ARE ADDED!!

	private static final int MENU_MY_PROD_IX = 0;
	private static final int MENU_MY_TIMECARDS_IX = 1;
	private static final int MENU_MY_FORMS_IX = 2;
	private static final int MENU_MY_ACCOUNT_IX = 3;
	private static final int MENU_ESS_IX = 4;	// LS-3758
	private static final int MENU_PROD_ADMIN_IX = 5;
	// SEE ABOVE NOTE IF NEW TOP MENU ITEMS ARE ADDED!!

	/** A list of the main (top-level) tabs when the user is NOT "in a
	 * production", i.e., when they first login. The index values (the first
	 * parameter to each createMainMenu call) determine their order in the
	 * header navigation bar. This list is used by header2.xhtml. */
	private static final List<Tab> systemMenus = new ArrayList<>();

	/** A list of the main (top-level) tabs when the user is in a production. The
	 * index values (the first parameter to each createMainMenu call) determine
	 * their order in the header navigation bar. This list is used by
	 * header2.xhtml. */
	private static final List<Tab> mainMenus = new ArrayList<>();
	static {
		// Main menu items displayed while in a production
		createMainMenu(mainMenus, MENU_HOME_IX, 			"Home", null, Constants.PGKEY_PRODUCTION, "0.2,non_tour_home", "0.2,non_talent_home", "HomeTab");
		createMainMenu(mainMenus, MENU_PROJECT_IX, 			"Project", "Production", "0.2,project_tab", null,"0.2,project_tab", "ProjectTab");
		createMainMenu(mainMenus, MENU_TALENT_PROJECT_IX, 	"Production", null, "0.2,non-talent_project_tab", null, "0.2,non-talent_project_tab", "ProjectTab");
		createMainMenu(mainMenus, MENU_PEOPLE_IX, 			"Cast & Crew", null, "0.2,contact_tab", null, "0.2,non_talent_contact_tab", "CastAmpCrewTab");
		createMainMenu(mainMenus, MENU_TALENT_PEOPLE_IX, 	"Staff & Talent", null, null, null, "0.2,contact_tab", "CastAmpCrewTab");
		createMainMenu(mainMenus, MENU_PAYROLL_IX, 			"Payroll", null, Constants.PGKEY_ONLINE, Constants.PGKEY_PAYROLL_TAB, "0.2,talent_payroll", "PayrollTab");
		createMainMenu(mainMenus, MENU_TALENT_ONBOARDING_IX,"Onboarding", null, null, null, Constants.PGKEY_ONLINE, "OnboardingTab");
		createMainMenu(mainMenus, MENU_SCRIPT_IX, 			"Script/Breakdown", null, "0.2,script_tab", "0.2,tour_script_tab",  "0.2,talent_script", "ScriptBreakdownTab");
		createMainMenu(mainMenus, MENU_SCHEDULE_IX, 		"Schedule", null, "0.2,schedule_tab", "0.2,tour_schedule_tab", "0.2,talent_schedule", "ScheduleTab");
		createMainMenu(mainMenus, MENU_REPORTS_IX, 			"Reports", null, "0.2,report_tab", "0.2,tour_report_tab", "0.2,talent_reports_tab", "ReportsTab");
		createMainMenu(mainMenus, MENU_FILES_IX, 			"Files", null, "0.2,file_tab", null, "0.2,talent_file", "FilesTab");
		createMainMenu(mainMenus, MENU_ADMIN_IX, 			"Admin", null, "0.2,admin_tab", null, "0.2,admin_tab", "HomeTab");

		// Main menu items for the "My Home" page (i.e., when NOT in a production)
		createMainMenu(systemMenus, MENU_MY_PROD_IX, 		Constants.HEADER_TEXT_FOR_US, Constants.HEADER_TEXT_FOR_CANADA, Constants.PGKEY_ONLINE, null, null, "MyProductions");
		createMainMenu(systemMenus, MENU_MY_TIMECARDS_IX, 	"My Timecards", null, Constants.PGKEY_ONLINE_US, null, null, "MyTimecards"); // LS-1658 Hide for non-US
		createMainMenu(systemMenus, MENU_MY_FORMS_IX, 		"My Starts", "My Contracts", Constants.PGKEY_ONLINE, null, null, "StartForms2"); // need "MyStarts" help file
		createMainMenu(systemMenus, MENU_MY_ACCOUNT_IX, 	"My Account", null, Constants.PGKEY_ONLINE, null, null, "MyAccount");
		createMainMenu(systemMenus, MENU_ESS_IX, 			"Self-Service", null, Constants.PGKEY_ESS, null, null, "ESS"); // LS-3758. need "ESS" help file
		createMainMenu(systemMenus, MENU_PROD_ADMIN_IX, 	"Prod Admin", null, "0.2,admin_tab", null, null, "MyProductions");
	}

	/*
	 * These define the sub-tabs, and the Faces action strings for each sub-tab.
	 * They are used within the faces-config.xml file to control navigation
	 * between pages. The faces-config entries define the correlation between
	 * these action strings and the actual filename of the JSP page. These
	 * action strings are stored in the Tab objects (created below). Some are
	 * 'public' because other Java code uses the named statics instead of
	 * hard-coding the literal values.
	 */

	/** Login page navigation string */
	public static final String VIEW_LOGIN = "login";

	// Home menu
	public static final String HOME_MENU_MYHOME = "myhome";

	// Project menu
	private static final String PROJECT_MENU_VIEW = "project";
//	public static final String PROJECT_MENU_MATERIALS = "materials";
	public static final String PROJECT_MENU_PERMISSIONS = "permissions";

	// Cast & Crew menu
	public static final String PEOPLE_MENU_CONTACTS = "contactlist";
	private static final String PEOPLE_MENU_DEPARTMENTS = "departments";
	public static final String PEOPLE_MENU_ONBOARDING = "onboarding";

	public static final String PEOPLE_MENU_STARTS = "tcstarts";

	private static final String TOURS_MENU_HOME = PEOPLE_MENU_CONTACTS;

	// Onboarding for Talent. In talent Onboarding is a main menu.
	public static final String TALENT_PEOPLE_MENU_ONBOARDING = "talentonboarding";
	public static final String TALENT_PAYROLL_START_FORMS = "talentprstartform";

	// Cast and Crew for Talent
	public static final String TALENT_PROJECT_MENU_VIEW = "talentproject";
	private static final String TALENT_PROJECT_MENU_PERMISSIONS = "talentpermissions";

	// Project tab for Talent
	public static final String TALENT_PEOPLE_MENU_CONTACTS = "talentcontactlist";
	public static final String TALENT_PEOPLE_MENU_DEPARTMENTS = "talentdepartments";

	// Payroll menu
	public static final String PAYROLL_TIMECARD = "timecard";
	public static final String PAYROLL_FULL_TC = "fulltimecard";
	public static final String PAYROLL_TIMESHEET = "timesheet";
	public static final String PAYROLL_APPROVER = "tcapprover";
	public static final String PAYROLL_PREFERENCES = "tcpreferences";
	private static final String PAYROLL_CONTRACTS = "prcontracts";
	private static final String PAYROLL_SIDE_LETTERS = "sideletters";
	public static final String PAYROLL_START_FORMS = "prstartform";
	public static final String PAYROLL_HOT_COSTS_DATA_ENTRY = "hotcostsdataentry";
	// Tours Wage/Tax Allocation page
	public static final String PAYROLL_WAGE_TAX_ALLOCATION = "toursallocation";

	// Script/breakdown menu
	public static final String SCRIPT_MENU_BREAKDOWN = "breakdown";
	public static final String SCRIPT_MENU_IMPORT = "newrevision";
	public static final String SCRIPT_MENU_DRAFTS = "drafts";
	public static final String ELEMENTS_MENU_SCRIPT_ELEMENTS = "scriptelements";
	public static final String ELEMENTS_MENU_REAL_ELEMENTS = "realelements";
	public static final String ELEMENTS_MENU_POI = "pointofinterest";

	// Schedule menu
	public static final String SCHEDULE_MENU_CALENDAR = "calendar";
	public static final String SCHEDULE_MENU_STRIPBOARD_VIEW = "stripboardview";
	public static final String SCHEDULE_MENU_STRIPBOARD_EDIT = "stripboardeditm";
	public static final String SCHEDULE_MENU_STRIPBOARDS = "stripboardlist";
	public static final String SCHEDULE_MENU_CALLSHEET_VIEW = "callsheetview";

	// Report menu
	public static final String REPORTS_MENU_REPORTS = "reports";
	public static final String REPORTS_MENU_DPR_VIEW = "dprview";
	public static final String REPORTS_MENU_DPR_EDIT = "dpredit";
	public static final String REPORTS_MENU_CALLSHEET_VIEW = "callsheetview";
	public static final String REPORTS_MENU_CALLSHEET_EDIT = "callsheetedit";
	public static final String REPORTS_MENU_EXHIBITG_VIEW = "exhibitGview";

	// Files menu
	private static final String FILES_MENU_REPOSITORY = "filerepository";

	// Admin menu within a Production
	private static final String ADMIN_MENU_EVENTLOG = "eventlist";
	private static final String ADMIN_MENU_MISC = "miscadmin";

	// System (pre-production login) menus
	public  static final String MYPROD_MENU_PROD = "myproductions";
	public  static final String MYTIMECARDS_MENU_DETAILS = "mytimecards";
	public  static final String MYTIMECARDS_FULL_TC = "myfulltimecard";
	public  static final String MYFORMS_MENU_DETAILS = "myforms";
	public  static final String ESS_MENU_PAYSTUBS = "paystub"; 	// will be re-routed to ESS URL. LS-3758
	public  static final String ESS_MENU_W2FORMS = "w2form"; 	// will be re-routed to ESS URL. LS-3758
	public static  final String MYACCT_MENU_DETAILS = "myaccount";
	public static  final String ESS_MY_PROFILE = "myprofile"; // used for routing to ESS My Profile page. LS-4850
	private static final String PRODADMIN_MENU_PROD = "adminproductions";
	private static final String PRODADMIN_MENU_USERS = "adminusers";
	private static final String PRODADMIN_MENU_PAYROLL = "adminpayroll";
	private static final String PRODADMIN_MENU_EVENTS = "adminevents";
	private static final String PRODADMIN_MENU_MISC = "prodadminmisc";

	/** Navigation string for mobile My Projects page. */
	public  static final String MYPROJECTS_PAGE = "myprojectsm";
	/** Navigation string for mobile My Timecards page. */
	public  static final String MYTIMECARDS_PAGE = "mytimecardsm";
	/** Navigation string for mobile My Account page. */
	public static  final String MYACCT_MOBILE = "myaccountm";

	/**
	 * A List of all the available (sub) menu tabs, ordered by the main tab
	 * within which they appear. These int's may be used within the application
	 * as the argument to setMenu(), to switch the user to a designated tab. The
	 * ones that are private are (obviously?) not used currently.
	 */

	/** setMenu index for in-Production "Home" page */
	public  static final int HOME_MENU_MYHOME_IX                = createTab(mainMenus, subMenuLists, MENU_HOME_IX, 0, "Production Home", null, HOME_MENU_MYHOME, Constants.PGKEY_PRODUCTION, "0.2,non_tour_home", "0.2,non_talent_home", "HomeTab");

	private static final int SUB_MENU_PROJECT_VIEW_IX           = createTab(mainMenus, subMenuLists, MENU_PROJECT_IX, 0, "Projects", "Productions", PROJECT_MENU_VIEW, "0.2,project_subhead", null, "0.2,project_subhead", "Projects");
//	private static final int SUB_MENU_MATERIALS_IX              = createTab(mainMenus, subMenuLists, MENU_PROJECT_IX, 1, "Materials", PROJECT_MENU_MATERIALS, "0.2,materials", "0.2,tour_materials",null,  "Materials");
	private static final int SUB_MENU_PERMISSIONS_IX            = createTab(mainMenus, subMenuLists, MENU_PROJECT_IX, 1, "Permissions", null, PROJECT_MENU_PERMISSIONS, "0.2,permissions", null, "0.2,permissions", "Permissions");

	private static final int SUB_MENU_CAST_CREW_IX  			= createTab(mainMenus, subMenuLists, MENU_PEOPLE_IX, 0, "Cast & Crew List", null, PEOPLE_MENU_CONTACTS, "0.2,contact_view", null, null, "CastAmpCrewList");
	private static final int SUB_MENU_ONBOARDING_IX 			= createTab(mainMenus, subMenuLists, MENU_PEOPLE_IX, 1, "Onboarding", null, PEOPLE_MENU_ONBOARDING, "0.2,onboarding_tab", null, null, "StartForms2"); // need "Onboarding" help file
	private static final int SUB_MENU_DEPARTMENTS_IX 			= createTab(mainMenus, subMenuLists, MENU_PEOPLE_IX, 2, "Departments", null, PEOPLE_MENU_DEPARTMENTS, "0.2,department_tab", null, null, "Departments");

	// Talent Cast & Crew page
	public  static final int SUB_STAFF_TALENT_MENU_IX           = createTab(mainMenus, subMenuLists, MENU_TALENT_PEOPLE_IX, 0, "Staff & Talent List", null, TALENT_PEOPLE_MENU_CONTACTS, "0.2,contact_view", null, "0.2,contact_view", "CastAmpCrewList");
	// Do not show departments sub menu for talent.
	//	private static final int SUB_MENU_TALENT_DEPARTMENTS_IX 	= createTab(mainMenus, subMenuLists, MENU_TALENT_PEOPLE_IX, 1, "Departments", TALENT_PEOPLE_MENU_DEPARTMENTS, "0.2,department_tab", null, "0.2,department_no_tab", "Departments");
	// Talent Project Page
	private static final int SUB_TALENT_MENU_PROJECT_VIEW_IX    = createTab(mainMenus, subMenuLists, MENU_TALENT_PROJECT_IX, 0, "Projects", "Productions", TALENT_PROJECT_MENU_VIEW, null, null, null, "Projects");
	private static final int SUB_TALENT_MENU_PERMISSIONS_IX     = createTab(mainMenus, subMenuLists, MENU_TALENT_PROJECT_IX, 1, "Permissions", null, TALENT_PROJECT_MENU_PERMISSIONS, null, null,null, "Permissions");

	private static final int SUB_MENU_TIMECARD_IX               = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 0, "Timecard", null, PAYROLL_TIMECARD, Constants.PGKEY_ONLINE, "0.2,tour_timecard", null, "Timecard3");
	private static final int SUB_MENU_TIMESHEET_IX              = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 1, "Employee Timesheet", null, PAYROLL_TIMESHEET, "0.2,non_tour_timesheet", "0.2,tc_preferences", null, "Timesheet");
	private static final int SUB_MENU_TC_APPROVER_IX            = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 2, "Approver Dashboard", null, PAYROLL_APPROVER, Constants.PGKEY_TC_APPROVAL, null, null, "ApproverDashboards");
	private static final int SUB_MENU_TC_HIERARCHY_IX           = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 3, "Preferences", null, PAYROLL_PREFERENCES, "0.2,tc_preferences", null, null, "PayrollPreferences");
	private static final int SUB_MENU_PR_CONTRACTS_IX           = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 4, "Contracts", null, PAYROLL_CONTRACTS, "0.2,tc_contracts", null, null, "ApproverHierarchy2");
	private static final int SUB_MENU_PR_SIDE_LTR_IX            = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 5, "Side Ltrs", null, PAYROLL_SIDE_LETTERS, "0.2,tc_contracts", null, null, "ApproverHierarchy2");
	private static final int SUB_MENU_PR_START_FORMS_IX         = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 6, "Start Forms", null, PAYROLL_START_FORMS, "0.2,pr_starts_tab", null, null, "StartForms2");
	private static final int SUB_MENU_TOURS_ALLOCATION_IX       = createTab(mainMenus, subMenuLists, MENU_PAYROLL_IX, 7, "Wage Allocations", null, PAYROLL_WAGE_TAX_ALLOCATION, null, "0.2,tours_allocations", null, "TaxWageAllocations");

	// Talent Cast & Crew page
	private static final int SUB_TALENT_MANAGE_FORMS_MENU_IX	= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 0, "Manage Forms", null, TALENT_PEOPLE_MENU_ONBOARDING, "0.2,onboarding_tab", null, "0.2,onboarding_tab", "OnboardingTab");
	private static final int SUB_TALENT_START_FORMS_MENU_IX		= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 1, "Start Forms", null, TALENT_PAYROLL_START_FORMS, "0.2,pr_starts_tab", null, "0.2,pr_starts_tab", "StartForms2");

	public  static final int SUB_MENU_BREAKDOWN_IX 				= createTab(mainMenus, subMenuLists, MENU_SCRIPT_IX, 0, "Breakdown", null, SCRIPT_MENU_BREAKDOWN, "0.2,breakdowns", null, null, "Breakdown2");
	public  static final int SUB_MENU_REVISIONS_IX              = createTab(mainMenus, subMenuLists, MENU_SCRIPT_IX, 1, "Scripts", null, SCRIPT_MENU_DRAFTS, "0.2,drafts", null, null, "Scripts");
	private static final int SUB_MENU_SCRIPT_ELEM_IX     		= createTab(mainMenus, subMenuLists, MENU_SCRIPT_IX, 2, "Script Elements", null, ELEMENTS_MENU_SCRIPT_ELEMENTS, "0.2,script_element_view", null, null, "ScriptElements");
	private static final int SUB_MENU_PROD_ELEM_IX     		 	= createTab(mainMenus, subMenuLists, MENU_SCRIPT_IX, 3, "Production Elements", null, ELEMENTS_MENU_REAL_ELEMENTS, "0.2,real_element_view", null, null, "ProductionElements");
	private static final int SUB_MENU_POI_IX 					= createTab(mainMenus, subMenuLists, MENU_SCRIPT_IX, 4, "P.O.I.", null, ELEMENTS_MENU_POI, "0.2,poi_view", null, null, "PointsOfInterest");

	private static final int SUB_MENU_CALENDAR_IX 				= createTab(mainMenus, subMenuLists, MENU_SCHEDULE_IX, 0, "Calendar", null, SCHEDULE_MENU_CALENDAR, "0.2,calendar", null, null, "Calendar");
	public  static final int SUB_MENU_STRIPBOARD_IX				= createTab(mainMenus, subMenuLists, MENU_SCHEDULE_IX, 1, "Strip Board", null, SCHEDULE_MENU_STRIPBOARD_VIEW, "0.2,stripboard", null, null, "StripBoard2");
	private static final int SUB_MENU_STRIPBOARD_LIST_IX 		= createTab(mainMenus, subMenuLists, MENU_SCHEDULE_IX, 2, "Strip Board Sets", null, SCHEDULE_MENU_STRIPBOARDS, "0.2,stripboard_list", null, null, "StripBoardSets");
	private static final int SUB_MENU_CALLSHEET_IX 				= createTab(mainMenus, subMenuLists, MENU_SCHEDULE_IX, 3, "Call Sheet", null, SCHEDULE_MENU_CALLSHEET_VIEW, "0.2,callsheet", null, null, "CallSheet2");

	// Talent Onboarding
//	private static final int SUB_MENU_OB_PACKETS				= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 0, "Documents & Packets", ONBOARDING_PACKETS, null, null, "0.2,onboarding_subhead", "Packets");
//	private static final int SUB_MENU_OB_DIST_REVIEW			= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 1, "Distribution & Review", ONBOARDING_DIST_REVIEW, null, null, "0.2,onboarding_subhead", "DistReview");
//	private static final int SUB_MENU_OB_DOC_TRANSFER			= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 2, "Transfer", ONBOARDING_DOC_TRANSFER, null, null, "0.2,onboarding_subhead", "ApprovalPath");
//	private static final int SUB_MENU_OB_APPROVE_PATH			= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 3, "Approval Paths", ONBOARDING_APPROVAL_PATHS, null, null, "0.2,onboarding_subhead", "DocTransfer");
//	private static final int SUB_MENU_OB_DOC_PREFS				= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 4, "Preferences", ONBOARDING_DOC_PREFS, null, null, "0.2,onboarding_subhead", "DocPrefs");
//	private static final int SUB_MENU_OB_COPY_DOCS 				= createTab(mainMenus, subMenuLists, MENU_TALENT_ONBOARDING_IX, 5, "Copy Documents", ONBOARDING_COPY_DOCS, null, null, "0.2,onboarding_subhead", "CopyDocs");

	public  static final int REPORTS_MENU_REPORTS_IX            = createTab(mainMenus, subMenuLists, MENU_REPORTS_IX, 0, "Reports", null, REPORTS_MENU_REPORTS, "0.2,report_tab", null, null, "ReportsTab");

	private static final int SUB_MENU_REPOSITORY_IX  			= createTab(mainMenus, subMenuLists, MENU_FILES_IX, 0, "File Repository", null, FILES_MENU_REPOSITORY, "0.2,files", null, null, "FileRepository");

	private static final int SUB_MENU_EVENTLOG_IX				= createTab(mainMenus, subMenuLists, MENU_ADMIN_IX, 0, "Events", null, ADMIN_MENU_EVENTLOG, "0.2,admin_tab", null, null, "HomeTab");
//	private static final int SUB_MENU_MERGE_IX 					= createTab(mainMenus, subMenuLists, MENU_ADMIN_IX, 1, "Merge", ADMIN_MENU_MERGE, "0.2,admin_tab", "0.2,tours_merge", "HomeTab");
	private static final int SUB_MENU_MISC_IX 					= createTab(mainMenus, subMenuLists, MENU_ADMIN_IX, 1, "Misc", null, ADMIN_MENU_MISC, "0.2,admin_tab", null, null, "HomeTab");

	private static final int SUB_MENU_MY_PROD_IX                = createTab(systemMenus, systemMenuLists, MENU_MY_PROD_IX, 0, "My Productions", null, MYPROD_MENU_PROD, Constants.PGKEY_ONLINE, null, null, "MyProductions");
//	private static final int ADMIN_MENU_PURCHASE_IX            	= createTab(MENU_ADMIN_IX, 1, "Purchase", MYPROD_MENU_PURCHASE, "0.2,admin_tab", "HomeTab");

	private static final int SUB_MENU_MY_TC_IX 					= createTab(systemMenus, systemMenuLists, MENU_MY_TIMECARDS_IX, 0, "My Timecards", null, MYTIMECARDS_MENU_DETAILS, Constants.PGKEY_ONLINE, null,null,  "MyTimecards");

	private static final int SUB_MENU_MY_FORMS_IX 				= createTab(systemMenus, systemMenuLists, MENU_MY_FORMS_IX, 0, "My Starts", null, MYFORMS_MENU_DETAILS, Constants.PGKEY_ONLINE, null, null, "StartForms2"); // need "MyStarts" help file

	private static final int SUB_MENU_ESS_PAYSTUBS_IX 			= createTab(systemMenus, systemMenuLists, MENU_ESS_IX, 0, "Pay Stubs", null, ESS_MENU_PAYSTUBS, Constants.PGKEY_ONLINE, null, null, "ESS"); // LS-3758. need "ESS" help file
	private static final int SUB_MENU_ESS_W2_IX 				= createTab(systemMenus, systemMenuLists, MENU_ESS_IX, 1, "W2 Forms", null, ESS_MENU_W2FORMS, Constants.PGKEY_ONLINE, null, null, "ESS"); 	// LS-3758. need "ESS" help file

	private static final int SUB_MENU_MY_ACCT_IX 				= createTab(systemMenus, systemMenuLists, MENU_MY_ACCOUNT_IX, 0, "My Account", null, MYACCT_MENU_DETAILS, Constants.PGKEY_ONLINE, null, null, "MyAccount");

	public  static final int SUB_MENU_ADMIN_PROD_IX             = createTab(systemMenus, systemMenuLists, MENU_PROD_ADMIN_IX, 0, "Productions", null, PRODADMIN_MENU_PROD, "0.2,admin_tab", null, null, "MyProductions");
	private static final int SUB_MENU_ADMIN_USERS_IX            = createTab(systemMenus, systemMenuLists, MENU_PROD_ADMIN_IX, 1, "Users", null, PRODADMIN_MENU_USERS, "0.2,admin_tab", null, null, "MyProductions");
	private static final int SUB_MENU_ADMIN_PAYROLL_IX          = createTab(systemMenus, systemMenuLists, MENU_PROD_ADMIN_IX, 2, "Payroll Services", null, PRODADMIN_MENU_PAYROLL, "0.2,admin_tab", null, null, "MyProductions");
	private static final int SUB_MENU_ADMIN_EVENTS_IX           = createTab(systemMenus, systemMenuLists, MENU_PROD_ADMIN_IX, 3, "Events", null, PRODADMIN_MENU_EVENTS, "0.2,admin_tab", null, null, "MyProductions");
	private static final int SUB_MENU_ADMIN_MISC_IX             = createTab(systemMenus, systemMenuLists, MENU_PROD_ADMIN_IX, 4, "Other", null, PRODADMIN_MENU_MISC, "0.2,admin_tab", null, null, "MyProductions");


	static {
		for (Tab m : mainMenus) {
			if (m.subTabs.size() == 1) {
				m.showSubTabs = false;
			}
		}
		for (Tab m : systemMenus) {
			if (m.subTabs.size() == 1) {
				m.showSubTabs = false;
			}
		}
//		if (log.isDebugEnabled()) {
//			for (Tab m : mainMenus) {
//				log.debug("Main tab: " + m);
//				log.debug("...sub tabs:");
//				for (Tab sub : m.subTabs) {
//					log.debug(sub);
//				}
//			}
//		}
	}

	private transient UserDAO userDAO;


	public HeaderViewBean() {
		try {
			if (SessionUtils.getHttpSession() != null) {
				Integer ix = (Integer)SessionUtils.getHttpSession().getAttribute(ATTR_CURRENT_TAB_IX);
				if (ix == null) {
					ix = getHomeMenuIx(SessionUtils.getProduction());
				}
				setThisMenu(ix);
				ix = (Integer)SessionUtils.getHttpSession().getAttribute(ATTR_MENU_TRACKING_IX);
				if (ix != null) {
					mainMenuTrackingIx = ix;
				}
			}
			else {
				log.warn("null httpSession");
				setThisMenu(getHomeMenuIx(SessionUtils.getProduction()));
			}

			if (SessionUtils.isTTCProd() || SessionUtils.isTTCOnline()) {
				tabDomainTitle = TEAM_TAB_TITLE;
				companyName = Constants.TTC_ONLINE_COMPANY_NAME;
			}
			else {
				tabDomainTitle = LS_TAB_TITLE;
				companyName = Constants.LS_COMPANY_NAME;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static HeaderViewBean getInstance() {
		return (HeaderViewBean)ServiceFinder.findBean("headerViewBean");
	}

	/**
	 * The actionListener method for the sub-tabs.  It changes
	 * the menuTrackingIx based on the attribute passed with the event, and
	 * also saves the menuTracking value, which is the faces action string.
	 * @param evt
	 */
	public void valueTracking(ActionEvent evt) {
		Integer tabix = (Integer)evt.getComponent().getAttributes().get("tabix");
		setThisMenu(tabix);
	}

	/**
	 * The actionListener method for the main menu tabs.  It changes
	 * the mainMenuTrackingIx based on the attribute passed with the event.
	 * @param evt
	 */
	public void mainvalueTracking(ActionEvent evt) {
		setMainMenuTrackingIx( (Integer)evt.getComponent().getAttributes().get("index") );
		//log.debug("mainMenuTrackingIx=" + mainMenuTrackingIx);
	}

	/**
	 * The ActionListener method for all the main tabs with the hover styling.
	 * It jumps to the "landing page" for the main tab that was clicked, which
	 * is the first (left-most) tab that the user has permission to view.
	 */
	public String actionHeaderClicked() {
		log.debug("header clicked, ix=" + mainMenuTrackingIx);
		AuthorizationBean auth = AuthorizationBean.getInstance();
		List<Tab> tabs;
		if (getProduction() == null || getProduction().isSystemProduction()) {
			tabs = systemMenus.get(mainMenuTrackingIx).subTabs;
		}
		else {
			tabs = mainMenus.get(mainMenuTrackingIx).subTabs;
		}
		Tab newTab = null;
		for (Tab tab : tabs) {
			if (getProduction() != null && getProduction().getType().isTours()) {
				if (auth.getPgFields().containsKey(tab.toursPermission)) {
					newTab = tab;
					break;
				}
			}
			else if (getProduction() != null && getProduction().getType().isTalent()) {
				if (auth.getPgFields().containsKey(tab.talentPermission)) {
					newTab = tab;
					break;
				}
			}
			else {
				if (auth.getPgFields().containsKey(tab.permission)) {
					newTab = tab;
					break;
				}
			}
		}
		if (newTab == null) {
			// this shouldn't happen!
			newTab = tabs.get(0); // try the first one anyway; will probably get rejected.
		}
		setThisMenu(newTab);

		String action = currentTab.subTabAction;
		action = redirect(action); // check for possible redirection. LS-4850

		Scroller.clearScroll(); // Any saved scroll value no longer applies.
		return action;
	}

	/**
	 * The action method for all the sub-tabs.  It returns the faces action
	 * string that was set by the listener method, valueTracking().
	 */
	public String actionMenuClicked() {
		String action = currentTab.subTabAction;
		log.debug("menuTracking(action)=" + action);
		Scroller.clearScroll(); // Any saved scroll value no longer applies.
		if (action.equals(SCHEDULE_MENU_CALLSHEET_VIEW)) {
			// Schedule | Callsheet -- force lookup of user's "current" callsheet
			SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, null);
		}
		else { // check for possible redirection. LS-4850
			action = redirect(action);
		}
		return action;
	}

	/**
	 * Determine an alternate (JSF navigation) action based on user or system
	 * settings. Currently used within mobile JSF pages.
	 *
	 * @param action The 'normal' action.
	 * @return Either the same action that was passed, or an alternate action if
	 *         that is appropriate (e.g., for redirecting to an ESS page).
	 */
	public String getRedirect(String action) {
		return redirect(action);
	}

	/**
	 * Check if the given JSF action string should be redirected; e.g., to an
	 * ESS link, based on user or system settings. LS-4850
	 *
	 * @param action The currently planned JSF navigation string
	 * @return The possibly changed JSF navigation string.
	 */
	private String redirect(String action) {
		if (action.equals(MYACCT_MENU_DETAILS) || action.equals(MYACCT_MOBILE)) {
			if (FF4JUtils.useFeature(FeatureFlagType.ESS_MY_PROFILE)) {
				User usr = SessionUtils.getCurrentUser();
				if (usr.getShowUS()) {
					action = ESS_MY_PROFILE;
				}
			}
		}
		return action;
	}

	/**
	 * Changes the project selected and sets it in the session; and also changes the user's default
	 * project for when they next log on. The authorization Map is also reloaded, since the user's
	 * role may have changed. They are then moved to the Home page.
	 */
	public void actionChangeProject(ValueChangeEvent event) {
		if (null != event.getNewValue()) {
			try {
				int newId = (Integer)event.getNewValue();
				setupNewProject(newId);
			}
			catch (Exception e) {
				reset();
				EventUtils.logError(e);
				MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			}
		}
	}

	/**
	 * Setup the currently selected project. Used by the project dropdown in the header and used
	 * by talent productions when selecting a project from the project list on the Projects page.
	 * LS-1853
	 *
	 * @param projectId
	 */
	public void setupNewProject(Integer projectId) {
		setSelectedProjectId(projectId);
		project = ProjectDAO.getInstance().findById(projectId);

		Contact cont = getContact();
		cont.setProject(getProject()); // update user's default project

		log.debug("User project changing: user=" + cont.getId() + ", proj=" + project.getId());
		UserDAO.getInstance().attachDirty(cont);

		// purge session variables that are project-dependent
		SessionUtils.put(Constants.ATTR_STRIPBOARD_ID, null);
		SessionUtils.put(Constants.ATTR_STRIPBOARD_UNIT_ID, null);

		SessionUtils.setCurrentProject(getProject());

		AuthorizationBean.getInstance().auth(cont); // update authorization map
//		ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, getProject(), contact.getUser(), "switched current project");
		HomePageBean home = HomePageBean.getInstance();
		home.projectChanged();
		setThisMenu(getHomeMenuIx(getProduction()));
		navigate(getHomeNavigation(production)); // switch the page
	}

	/**
	 * Action method for some "Done"/"Return" buttons; we pull the previous
	 * tab index from our session data, and jump back to that tab.
	 * @return The faces navigation string appropriate to the prior tab.
	 */
	public String actionReturn() {
		int priorTabIx = SessionUtils.getInteger(Constants.ATTR_PRIOR_TAB_IX, -1);
		log.debug(priorTabIx);
		if (priorTabIx >= 0) {
			setThisMenu(priorTabIx);
			Scroller.clearScroll(); // Any saved scroll position no longer applies.
		}
		return currentTab.subTabAction;
	}

	/**
	 * Action method for the generic "Back" button on mobile pages, and for some
	 * "Return" buttons on desktop pages.
	 * <p>
	 * The value of the "backAction" hidden input tag is used to determine the
	 * JSF navigation string to return. If the value does not begin with a
	 * question mark, it is returned as the JSP navigation string. If it begins
	 * with a question mark, the question mark is removed and the remainder of
	 * the string is used as a key to look up a session parameter. The parameter
	 * value is then returned as the navigation string.
	 *
	 * @return The navigation string to return to the "prior" page, as
	 *         determined by the value set in the current page in the
	 *         ice:inputHidden tag bound to {@link #backAction}.
	 */
	public String actionBack() {
		String ret = null; // default to null, so we won't go anywhere
		if (backAction != null) {
			Object back = backAction.getValue();
			if (back instanceof String) {
				ret = (String)back;
				ret = ret.trim();
				if (ret.startsWith("?")) {
					String key = ret.substring(1); // remove leading "?"
					ret = SessionUtils.getString(key); // use it as a session attribute key
					if (ret == null) {
						log.warn("WARNING - missing 'back' navigation string for session key '" + key + "'");
						ret = SessionUtils.mobilize(getHomeNavigation(production));
					}
				}
				Scroller.clearScroll(); // Any saved scroll position no longer applies.
			}
		}
		return ret;
	}

	/**
	 * Called from the recover.xhtml page to forward the user
	 * to either the "My Productions" page or the "My Home" page.
	 *
	 * @return empty navigation string
	 */
	public String getRecover() {
		log.debug("");
		Production prod = SessionUtils.getProduction();
		//dumpAttributes();
		if (prod == null) { // may have session already ...
			User usr = SessionUtils.getCurrentUser();
			if (usr == null) {
				navigateToUrl("../logout.jsp"); // invalidate session & redirects to login
				// Note that URL is relative to '/jsp/recover.xhtml', so only one '..' is necessary
			}
			else { // logged in, but no Production, send to My Productions page
				navigate(SessionUtils.mobilize(MYPROD_MENU_PROD));
			}
		}
		else if (prod.isSystemProduction()) {
			navigate(SessionUtils.mobilize(MYPROD_MENU_PROD)); // switch to My Productions
		}
		else {
			setThisMenu(getHomeMenuIx(prod));
			if (getTablet()) {	// skip 'mobilizing' for tablet - use desktop
				navigate(HOME_MENU_MYHOME);	// switch to My Home
			}
			else {
				navigate(SessionUtils.mobilize(getHomeNavigation(prod)));	// switch to My Home
			}
		}
		return "";
	}

	/**
	 * Forward the user to a new page within our application using action strings.
	 * Do not use this for external URLs.
	 * @param viewId The action string, or "outcome", that will be used to
	 * determine the new page, using the faces-config.xml navigation rules.
	 */
	public static void navigate(String viewId) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		facesContext.getApplication().getNavigationHandler().handleNavigation(facesContext, "", viewId );
	}

	/**
	 * Redirect the user to the specified URL; currently this works for
	 * "internal" URLs. To make this work, ".xhtml" suffixes are converted to
	 * ".jsf". This method is used by the login code to redirect a user to a
	 * page trying to be reached while not logged in, such as an email link to
	 * the callsheet.
	 *
	 * @param url
	 * @return True if no IOException (or other Exception) occurs during the
	 *         redirect.
	 */
	public static boolean navigateToUrl(String url) {
		log.debug(url);
		boolean bRet = false;
		try {
			FacesContext fc = FacesContext.getCurrentInstance();
			if (fc != null) {
				ExternalContext ec = fc.getExternalContext();
				if (ec != null) {
					if (url.charAt(0) == '/') {
						url = url.substring(1);
					}
					if (url.indexOf(".xhtml") > 0) {
						url = url.replaceAll("\\.xhtml", ".jsf");
					}
					UIViewRoot root = fc.getViewRoot();
					if (root != null) {
						String uri = root.getViewId();
						if (url.startsWith("jsp/") && uri.startsWith("/jsp/")) {
							url = "../../" + url;
						}
						else if (url.startsWith("m/") && uri.startsWith("/m/")) {
							url = "../../" + url;
						}
					}
					else { // our best guess...
						url = "../../" + url;
					}
					//log.debug(url);
					ec.redirect(url);
					log.debug("redirect completed to " + url);
					bRet = true;
				}
			}
		}
		catch (Exception e) {
			log.error(e);
		}
		return bRet;
	}

	/**
	 * Create a token with the given User, then navigate to the specified
	 * URL, passing the token along.
	 *
	 * @param url
	 * @param user
	 * @return
	 */
	public static boolean navigateWithToken(String url) {
		boolean bRet = false;
		User user = SessionUtils.getCurrentUser();
		if (user != null) {
			String token = ApiUtils.getToken(user);
			url += "?token=" + token;
			bRet = navigateToUrl(url);
		}
		return bRet;
	}

	/**
	 * Create a new Tab with the specified parameters, add it to our list of Tabs,
	 * and to the subMenuLists, which is our List of Lists of Tabs.
	 * This also returns the index of the Tab within the tabList.  The index can be
	 * used later when calling setMenu() to switch the display to this Tab.
	 *
	 * @param main  The index of the main tab that owns this sub-tab.
	 * @param subIx The index of this sub-tab within the list for its main tab.
	 * @param pSubText The text to be displayed on the tab.
	 * @param action The action string to be passed to Faces when this sub-tab is clicked.
	 *
	 * @return The index of the newly created Tab within our tabList.
	 */
	private static int createTab( List<Tab> menuList, List<List<Tab>> subLists, int main, int subIx,
			String pSubText, String pSubTextCa, String action,
			String permission, String toursPerm, String talentPerm, String helpFile) {
		Tab tab = new Tab(main, subIx, pSubText, action, permission, toursPerm, talentPerm, helpFile);

		if (pSubTextCa != null) {
			tab.setSubTextCa(pSubTextCa);
		}

		tab.tabIx = tabList.size();
		tabList.add(tab);	// Add it to the complete (single) list of Tabs.

		// Now add the Tab to the appropriate list, corresponding to its main tab.
		// First make sure that subMenuLists is big enough.
		while (tab.mainTabIx >= subLists.size()) {
			List<Tab> sublist = new ArrayList<>();
			subLists.add( sublist );
			menuList.get(subLists.size()-1).subTabs = sublist;
		}
		// Now make sure that the sub-list for this main tab is big enough.
		List<Tab> list = subLists.get(tab.mainTabIx);
		while (tab.subTabIx >= list.size()) {
			list.add(null);
		}
		list.set(tab.subTabIx, tab);
		/* Note that we can't just do list.add(ix,tab), because we do NOT require
		 * that the tabs be created in the same order as their index values; and if
		 * they are created out of order, doing the "add(ix...)" will shift previously
		 * entered Tabs in the list to different locations.
		*/
		return tab.tabIx;
	}

	/**
	 * Create an entry in either {@link #mainMenus} or {@link #systemMenus} for
	 * a main menu tab.
	 *
	 * @param menuList The List of Tabs to which this tab will be added.
	 * @param main The index (origin 0) of the tab
	 * @param pSubText The text displayed for the tab
	 * @param pSubTextCa The text displayed for the tab for Canadian users; if
	 *            null, pSubText will be displayed.
	 * @param permission The page-field-access string that the user must have
	 *            for this tab to be displayed.
	 * @param toursPerm The page-field-access string that the user must have for
	 *            this tab to be displayed if this is a Tours production type.
	 * @param talentPerm The page-field-access string that the user must have
	 *            for this tab to be displayed if this is a Talent/Canada
	 *            production type.
	 * @param pHelp The help context string.
	 */
	private static void createMainMenu( List<Tab> menuList, int main, String pSubText, String pSubTextCa,
			String permission, String toursPerm, String talentPerm, String pHelp) {

		Tab tab = new Tab(main, -1, pSubText, null, permission, toursPerm, talentPerm, pHelp);
		if (pSubTextCa != null) {
			tab.setSubTextCa(pSubTextCa);
		}

		// Make sure that the menu list is big enough.
		while (tab.mainTabIx >= menuList.size()) {
			menuList.add(null);
		}
		menuList.set(tab.mainTabIx, tab);
		/* Note that we can't just do list.add(ix,tab), because we do NOT require
		 * that the tabs be created in the same order as their index values.
		*/
	}

	/**
	 * Sets the current main & sub-menu tab.
	 * The index parameter is usually specified by one of the static int's defined
	 * at the beginning of this file.
	 * @param ix
	 */
	public static void setMenu(int ix) {
		if ( ! SessionUtils.isMobileApp()) { // This method is a no-op for LSMOBILE
			HeaderViewBean header = (HeaderViewBean)ServiceFinder.findBean("headerViewBean");
			if (ix < tabList.size()) {
				Tab tab = tabList.get(ix);
				header.setThisMenu(tab);
			}
		}
	}

	public void setMenu(String tabAction) {
		for (Tab tab : tabList) {
			if (tab.subTabAction.equalsIgnoreCase(tabAction)) {
				setThisMenu(tab);
				//log.debug(tab.getTabIx() + ", mainTabIx=" + tab.getMainTabIx());
				break;
			}
		}
	}

	/**
	 * Return the correct prefix to use in the tab title based on the domain
	 * that originated the request. LS-1691
	 *
	 * @return
	 */
	public String getDomainTabTitle() {
		return tabDomainTitle;
	}

	public String getContextPath() {
		return ApplicationUtils.getRequestContext().getContextPath();
	}

	/**
	 * The domain that originated the request. LS-1691
	 *
	 * @return
	 */
	public String getDomain() {
		// HTTP request domain
		return SessionUtils.getString(Constants.ATTR_HTTP_REQUEST_DOMAIN, "");
	}

	/**
	 * Set the 'currentTab' value to the given Tab, and update session tracking
	 * information (prior and current tab indices).
	 *
	 * @param ix This index of the Tab to be set within the {@link #tabList}.
	 */
	private void setThisMenu(int ix) {
		setThisMenu(tabList.get(ix));
	}

	/**
	 * Set the 'currentTab' value to the given Tab, and update session tracking
	 * information (prior and current tab indices).
	 *
	 * @param tab The Tab being made current.
	 */
	private void setThisMenu(Tab tab) {
		SessionUtils.put(ATTR_CURRENT_TAB_IX, tab.tabIx);
		if (tab != currentTab && currentTab != null) {
			SessionUtils.put(Constants.ATTR_PRIOR_TAB_IX, currentTab.tabIx);
		}
		currentTab = tab;
		setMainMenuTrackingIx(currentTab.mainTabIx);
	}

	/**
	 * Open the Help window on the appropriate subject page, based on the
	 * currently selected tab. The Tab instances include the name of the help
	 * entry to be displayed.
	 *
	 * @param ae The event generated by the front end as a result of the user
	 *            clicking a help button or link.
	 */
	public void openHelpWindow(ActionEvent ae)
	{
		String params;
		if (currentTab == null) {
			params = "'Home',0,0,'index'"; // "index" is only used for word2web
		}
		else {
			params = "'" + currentTab.subText + "',"  // first 3 params are only for chm2web
					+ currentTab.mainTabIx + ","
					+ currentTab.tabIx + ","
					+ "'" + currentTab.helpFile + "'"; // this param is only for word2web
		}

		log.debug("help params=" + params);
		View.addJavascript("openHelp(" + params + ");" ); // Calls 'openHelp' function in LS script.js;
	}

	/**
	 * Action method for the "My Home" link from the header area. Exit the
	 * current User from the current Production, and display the My Productions
	 * page.  The User will be signed into the SYSTEM production if they
	 * are a member of it.
	 *
	 * @return Navigation string for My Productions page
	 */
	public String actionChangeProduction() {
		user = SessionUtils.getCurrentUser();
		exitProduction(getUser());
		// clear saved page, so we don't go back to that page if we re-enter the production.
		SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, null);
		setThisMenu(HOME_MENU_MYHOME_IX);
		if (SessionUtils.isMobileUser()) { // mobile goes to top "My Home" page
			return HOME_MENU_MYHOME + "m";
		}
		else { // desktop goes to "My Productions" page
			return MYPROD_MENU_PROD;
		}
	}

	/**
	 * Exit the given User from the current Production, and put them in the
	 * "outside production" state. They will be put "in" the SYSTEM production
	 * if they have those credentials.
	 *
	 * @param usr The User to be switched.
	 */
	public static void exitProduction(User usr) {
		HttpSession session = SessionUtils.getHttpSession();
		clearAttributes(session, false);
		SessionSetUtils.setCurrentUser(usr);
		AuthorizationBean.getInstance().loggedIn(usr, null);
		SessionUtils.put(Constants.ATTR_LOGGED_IN, Boolean.TRUE);
		loginToSystem(usr);	// sign in to SYSTEM production if applicable
	}

	/**
	 * Mark the User as logged into the System production, if they have that
	 * privilege, else mark them as not in any production.
	 *
	 * @param usr The User to be switched.
	 */
	private static void loginToSystem(User usr) {
		Contact c = null;
		Production prod = ProductionDAO.getSystemProduction();
		if (prod != null) {
			c = ContactDAO.getInstance().findByUserProduction(usr, prod);
			if (c != null && c.getStatus().isActive() && c.getLoginAllowed()) {
				SessionSetUtils.setProduction(prod);
				if (LoginBean.checkProjectAccess(c)) {
					SessionUtils.setCurrentContact(c);
				}
			}
			else { // Not a member of "System" production,
				SessionSetUtils.setProduction(null); // so clear 'current production' value.
			}
		}
	}

	/**
	 * Action method from the "logout" button in the (common) page header.
	 * Invalidate (expire) the current session, remove the "currentUser" setting,
	 * and send the user to the login page.  Note that 'event' may be null.
	 */
	public void actionLogout(ActionEvent event) {
		HttpSession session = SessionUtils.getHttpSession();
		log.debug("logging off session=" + session.getId());

		boolean crossApp = SessionUtils.getBoolean(Constants.ATTR_CROSS_APP, false); // LS-3758

		try {
			EventUtils.logEvent(EventType.LOGOUT,
					"session id=" + session.getId() +
					", IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR));
			setThisMenu(SUB_MENU_ADMIN_PROD_IX); // was HOME_MENU_MYHOME_IX; rev 7386

			clearAttributes(session, true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		// we can't invalidate the session directly here, or an error occurs during
		// rendering.  However, we can redirect to a plain jsp page that uses
		// javascript to invalidate the session, and then redirects back to the login page.

		logout(crossApp); // logout.jsp invalidates, & redirects to login

		log.debug("done");
	}

	/**
	 * Handle UI part of logout: Send the session directly to the logout page,
	 * which will invalidate their session and redirect them to the login page.
	 */
	public static void logout() {
		logout(false);
	}

	/**
	 * Handle UI part of logout: Send the session directly to the logout page,
	 * which will invalidate their session and redirect them to either the TTCO
	 * login page or the ESS login page.
	 */
	private static void logout(boolean crossApp) {
		String page = "../../logout.jsp";
		if (crossApp) { // LS-3758
			// after invalidating session, jump to ESS Logout page;
			// that will end the ESS session and redirect to the ESS login page.
			page += "?jump=" + ESS_LOGOUT_URL + "?source=TTCO";
		}
		else {
			Integer id = SessionUtils.getInteger(Constants.ATTR_BRAND_SERVICE_ID);
			if (id != null && id > 0) {
				PayrollService ps = PayrollServiceDAO.getInstance().findById(id);
				if (ps != null && ps.getReferParam() != null) {
					page += "?ref=" + ps.getReferParam();
				}
			}
		}
		navigateToUrl(page);		// new way -- logout.jsp invalidates, & redirects to login
	}

	public static void clearAttributes(HttpSession session) {
		clearAttributes(session, false);
	}

	private static void clearAttributes(HttpSession session, boolean logout) {
		Enumeration<String> attrs = session.getAttributeNames();
		// Clear ALMOST all the session attributes, just in case the session does not
		// get invalidated.
		while (attrs.hasMoreElements()) {
			String attr = attrs.nextElement();
			if (attr != null) {
				if (attr.contains("userPrefBean") || attr.equals("disposer")
						// leave userPrefBean & Disposer, so their dispose() method will be called.
						|| attr.equals(Constants.ATTR_BRAND_SERVICE_ID) // retain branding information
						|| attr.equals(Constants.ATTR_BRAND_SERVICE_REFER) // retain branding information
						|| attr.equals(Constants.ATTR_IS_TTCONLINE_DOMAIN) // retain domain information
						|| (! logout &&
							(attr.equals(Constants.ATTR_IP_ADDR)  // save IP address
							|| attr.contains("icefaces")	// retain any ICEfaces components
							|| attr.contains("icesoft")		// retain any ICEfaces components
							|| attr.contains("authBean") 	// retain AuthorizationBean (session-scoped)
							|| attr.contains("timeout")		// session time-out information
							|| attr.equals(Constants.ATTR_USER_PHONE)
							|| attr.equals(Constants.ATTR_USER_TABLET)
							|| attr.equals(Constants.TOMCAT_USER_KEY)
							|| attr.equals(Constants.ATTR_HTTP_REQUEST_DOMAIN)
							|| attr.equals(Constants.ATTR_SELECT_CONTACT_ID)) )
						) {
					// don't do it!
				}
				else {
					log.debug("removed attribute=" + attr);
					session.removeAttribute(attr);
				}
			}
		}
	}

	private static void dumpAttributes() {
		if (log.isDebugEnabled()) {
			Enumeration<String> attrs = SessionUtils.getHttpSession().getAttributeNames();
			while (attrs.hasMoreElements()) {
				String attr = attrs.nextElement();
				log.debug("attribute=" + attr);
			}
		}
	}

	/**
	 * This method should be called when the current Production, Project
	 * or User changes in some way that should be reflected in the header
	 * information, e.g., changing the user's current project.
	 */
	public static void reset() {
		try {
			HeaderViewBean me = getInstance(); // not available in batch mode
			if (me != null) {
				me.setProduction(null);
				me.setProject(null);
				me.setUser(null);
				me.setProjectTitles(null);
			}
		}
		catch (Exception e) {
		}
	}

	/** See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getProduction();
			if (production != null || !branded) {
				// in a production, or branded not set yet, possibly (re)set branding state.
				SessionSetUtils.setupBranding(production, branded, this);
			}
		}
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	public boolean isTours() {
		if (getProduction() != null) {
			return getProduction().getType().isTours();
		}
		return false;
	}

	public boolean isTalent() {
		if (getProduction() != null) {
			return getProduction().getType().isTalent();
		}
		return false;
	}

	// LS-3095 Determine whether this is a Team Client Production.
	public boolean isTTCProd() {
		return SessionUtils.isTTCProd();
	}

	// LS-3095 Determine if the request is coming from ttconline.com domain.
	public boolean isTTCOnline() {
		return SessionUtils.isTTCOnline();
	}

	public static String getHomeNavigation() {
		return getHomeNavigation(SessionUtils.getProduction());
	}

	public static String getHomeNavigation(Production prod) {
		String nav = HOME_MENU_MYHOME;
		if (prod != null) {
			if(prod.getType().isTours()) {
				nav = TOURS_MENU_HOME;
			}
			else if(prod.getType().isTalent()) {
				nav = PROJECT_MENU_VIEW;
			}
		}

		return nav;
	}

	public static int getHomeMenuIx(Production prod) {
		int nav = HOME_MENU_MYHOME_IX;
		if (prod != null) {
			if(prod.getType().isTours()) {
				nav = SUB_MENU_CAST_CREW_IX;
			}
			else if(prod.getType().isTalent()) {
				nav = SUB_MENU_PROJECT_VIEW_IX;
			}
		}
		return nav;
	}

	public Project getProject() {
		if (project == null) {
			project = getContact().getProject();
		}
		project = ProjectDAO.getInstance().refresh(project);
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getUser() {
		if (user == null) {
			user = SessionUtils.getCurrentUser();
			log.debug("session user #" + (user==null ? "null" : user.getId()));
		}
		else {
			user = getUserDAO().refresh(user);
			log.debug("refresh user #" + user.getId());
		}
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * @return True iff the current user is a Team payroll biller.
	 */
	public boolean isTeamBiller() {
		User u = getUser();
		if (u.getEmailAddress().endsWith("@theteamcompanies.com")) {
			return true;
		}
		return false;
	}

	public Contact getContact() {
		if (contact == null) {
			contact = SessionUtils.getCurrentContact();
			log.debug("session contact #" + (contact==null ? "null" : contact.getId()));
		}
		else {
			contact = ContactDAO.getInstance().refresh(contact);
			log.debug("refresh contact #" + contact.getId());
		}
		return contact;
	}

	private UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

	/** See {@link #mainMenus}. */
	public List<Tab> getMainMenus() {
		if (getProduction() == null || getProduction().isSystemProduction()) {
			return systemMenus;
		}
		return mainMenus;
	}

	/** See {@link #mainMenuTrackingIx}. */
	public int getMainMenuTrackingIx() {
		return mainMenuTrackingIx;
	}
	public void setMainMenuTrackingIx(int ix) {
		mainMenuTrackingIx = ix;
		SessionUtils.getHttpSession().setAttribute(ATTR_MENU_TRACKING_IX, mainMenuTrackingIx);
	}

	public int getMenuTrackingIx() {
		return currentTab.subTabIx;
	}

	public int getCurrentTabIx() {
		return currentTab.tabIx;
	}

	public List<List<Tab>> getSubMenuLists() {
		if (getProduction() == null || getProduction().isSystemProduction()) {
			return systemMenuLists;
		}
		return subMenuLists;
	}

	/**
	 * Get the title of the currently selected project.
	 * @return
	 */
	public String getProjectTitle() {
		return SessionUtils.getCurrentProject().getTitle();
	}

	public Integer getSelectedProjectId() {
		if (selectedProjectId == null) {
			selectedProjectId = SessionUtils.getCurrentProject().getId();
		}
		return selectedProjectId;
	}
	public void setSelectedProjectId(Integer selectedProjectId) {
		this.selectedProjectId = selectedProjectId;
	}

	public List<SelectItem> getProjectTitles() {
		if (projectTitles == null) {
			projectTitles = SessionUtils.createProjectList(getContact(), false);
		}
		return projectTitles;
	}
	public void setProjectTitles(List<SelectItem> titles) {
		projectTitles = titles;
	}

//	/**See {@link #params}. */
//	public Map<String, Integer> getParams() {
//		return params;
//	}
//	/**See {@link #params}. */
//	public void setParams(Map<String, Integer> params) {
//		this.params = params;
//	}

	/**
	 * Return current Date (and time); used to display current
	 * date on various pages.
	 */
	public Date getToday() {
		return new Date();
	}

	// Both getTabName() and setTabName() were moved to HeaderRequestBean,
	// to avoid the extra instantiation of this (HeaderViewBean) on
	// every request.  The bindings in all of our xhtml page was changed
	// from:
	//  	binding="#{headerViewBean.tabName}"
	// to:
	//  	binding="#{headerRequestBean.tabName}"
//
//	/**
//	 * Support binding our tabname field to the ice:outputText field in each
//	 * webpage, whose 'tabid' attribute will identify the matching tab for the page.
//	 * @return The outputText object
//	 */
//	public HtmlOutputText getTabName() {
//		HeaderRequestBean bean = HeaderRequestBean.getInstance();
//		return bean.getTabName();
//		return tabName;
//	}
//	public void setTabName(HtmlOutputText name) {
//		HeaderRequestBean bean = HeaderRequestBean.getInstance();
//		bean.setTabName(name);
//		tabName = name;
//	}

	/** See {@link #miniTab}. */
	public int getMiniTab() {
		return miniTab;
	}
	/** This sets the {@link #miniTab} value, and also stores the value
	 * in the user's session under an attribute name which is unique to the page currently
	 * displayed. (The Faces navigation string for the page is used to make
	 * it unique.) */
	public void setMiniTab(int n) {
		miniTab = n;
		if (currentTab != null) { // should always be true
			String attr = Constants.ATTR_CURRENT_MINI_TAB + "." + currentTab.subTabAction;
			SessionUtils.put(attr, miniTab);
		}
	}

	/**
	 * This is meant to be called from JSP to cause our mini-tab setting to be set to the
	 * last saved value for the current page.
	 * @return the new mini-tab index setting (origin zero).
	 */
	public int getCheckMiniTab() {
		miniTab = getSavedMiniTab();
		return miniTab;
	}

	/**
	 * @return The saved value of the selected mini-tab (origin 0) for the
	 * current page.  Each sub-menu has its own attribute name under which the
	 * mini-tab setting can be stored in the user's session.
	 */
	public int getSavedMiniTab() {
		int tabNum = 0;
		if (currentTab != null) { // should always be true
			String attr = Constants.ATTR_CURRENT_MINI_TAB + "." + currentTab.subTabAction;
			tabNum = SessionUtils.getInteger(attr, 0);
		}
		return tabNum;
	}

	/** See {@link #msgExists}. */
	public boolean getMsgExists() {
		return msgExists;
	}
	/** See {@link #msgExists}. */
	public void setMsgExists(boolean msgExists) {
		this.msgExists = msgExists;
	}

	/**See {@link #branded}. */
	public boolean getBranded() {
		if (production == null) {
			getProduction(); // init our production field & branding
		}
		return branded;
	}
	/**See {@link #branded}. */
	public void setBranded(boolean branded) {
		this.branded = branded;
	}

	/**See {@link #brandDesktopLogo}. */
	public Image getBrandDesktopLogo() {
		return brandDesktopLogo;
	}
	/**See {@link #brandDesktopLogo}. */
	public void setBrandDesktopLogo(Image brandImage) {
		brandDesktopLogo = brandImage;
	}

	/**See {@link #brandMobileLogo}. */
	public Image getBrandMobileLogo() {
		return brandMobileLogo;
	}
	/**See {@link #brandMobileLogo}. */
	public void setBrandMobileLogo(Image brandMobileLogo) {
		this.brandMobileLogo = brandMobileLogo;
	}

	/** See {@link #mobile}. */
	public boolean getMobile() {
		if (mobile == null) {
			mobile = SessionUtils.isPhoneUser();
		}
		return mobile;
	}
	/** See {@link #mobile}. */
	public void setMobile(boolean mobile) {
		this.mobile = mobile;
	}

	/**See {@link #tablet}. */
	public Boolean getTablet() {
		if (tablet == null) {
			tablet = SessionUtils.isTabletUser();
		}
		return tablet;
	}
	/**See {@link #tablet}. */
	public void setTablet(Boolean tablet) {
		this.tablet = tablet;
	}

	/** See {@link #backAction}. */
	public HtmlInputHidden getBackAction() {
		return backAction;
	}
	/** See {@link #backAction}. */
	public void setBackAction(HtmlInputHidden backAction) {
		this.backAction = backAction;
	}

	public boolean getOffline() {
		return ApplicationUtils.isOffline();
	}

	/**
	 * Invoked during rendering of the {@link #tabName} field, which is defined
	 * on every page. If {@link #newInstance} is true (indicating a new
	 * instantiation of HeaderViewBean), this should be a display of the
	 * underlying page, and not just a click on a different main tab. This
	 * includes the result of a browser "Back" or "Refresh" request. In these
	 * cases, we need to force the tab/sub-tab display to be correct by calling
	 * setMenu.
	 * <p>
	 * Note that the 'tabid' attribute value (set via an f:attribute tag) should
	 * be one of the strings that identifies a particular tab, e.g., "timecard".
	 * This is typically the Faces navigation string for the page. The lookup is
	 * NOT case sensitive.
	 *
	 * @return null, since this field should always have "visible=false" set.
	 */
	public String getTabValue() {
		if (newInstance) {
			HeaderRequestBean bean = HeaderRequestBean.getInstance();
			String tabAction = (String) bean.getTabName().getAttributes().get("tabid");
			newInstance = false;
			setMenu(tabAction);
		}
		return null;
	}

	/**
	 * Maintains the description of a main menu or sub-menu tab.
	 */
	public static class Tab implements Serializable {
		/** */
		private static final long serialVersionUID = - 8216244647662487975L;

		/** The index of the main tab to which this sub-tab belongs. */
		private final int mainTabIx;

		/** The index of this sub-tab within its main tab. */
		private final int subTabIx;

		/** The index of this sub-tab within the overall tabList list. */
		private int tabIx;

		/** The Faces action string associated with clicking this tab. */
		private final String subTabAction;

		/** The text to be displayed on this sub-tab. */
		private final String subText;

		/** The text to be displayed on this sub-tab for Canadian users; if null,
		 * the {@link #subText} field will be used. */
		private String subTextCa;

		/** The permission string (page-field access key) required to see this tab. This
		 * value is used in the xhtml that renders the main tab and sub-tab displays. */
		private final String permission;

		/** The permission string (page-field access key) required to see this
		 * tab when the current Production is a Tours type. This value is used
		 * in the xhtml that renders the main tab and sub-tab displays. This
		 * allows a different set of tabs to be rendered in a Tours production
		 * as opposed to other types of Production`s. */
		private final String toursPermission;

		/** The permission string (page-field access key) required to see this
		 * tab when the current Production is an US or Canada Talent type. This value is used
		 * in the xhtml that renders the main tab and sub-tab displays. This
		 * allows a different set of tabs to be rendered in these productions
		 * as opposed to other types of Production`s. */
		private final String talentPermission;

		/** The name of the help topic or file to present when this tab is open. */
		private final String helpFile;
		/** A List of the subtabs belonging to this Main tab; null for subtabs. */

		private List<Tab> subTabs;

		/** True if subtabs of the main tab should be displayed, false otherwise.
		 * Typically set to false for main tabs that have only a single subtab. */
		private boolean showSubTabs;

		public Tab( int main, int subIx, String pSubText, String action, String pSubPerm, String toursPerm, String talentPerms, String pHelp) {
			mainTabIx = main;
			subTabAction = action;
			subTabIx = subIx;
			subText = pSubText;
			subTextCa = pSubText;
			permission = pSubPerm;
			helpFile = pHelp;
			showSubTabs = true;
			if (toursPerm == null) { // use the same as non-Tours productions
				toursPermission = permission;
			}
			else {
				toursPermission = toursPerm;
			}
			if (talentPerms == null) { // use the same as non-Tours productions
				talentPermission = permission;
			}
			else {
				talentPermission = talentPerms;
			}
			//log.debug(this);
		}

		/** Return the text to display on the tab.  Used by header.xhtml. */
		public String getSubText() {
			return subText;
		}

		/** See {@link #subTextCa}. */
		public String getSubTextCa() {
			return subTextCa;
		}
		/** See {@link #subTextCa}. */
		public void setSubTextCa(String subTextCa) {
			this.subTextCa = subTextCa;
		}

		public int getMainTabIx() {
			return mainTabIx;
		}
		public int getTabIx() {
			return tabIx;
		}
		/** See {@link #subTabs}. */
		public List<Tab> getSubTabs() {
			return subTabs;
		}

		/** See {@link #showSubTabs}. */
		public boolean getShowSubTabs() {
			return showSubTabs;
		}
		/** See {@link #permission}. */
		public String getPermission() {
			return permission;
		}
		/** See {@link #toursPermission}. */
		public String getToursPermission() {
			return toursPermission;
		}
		/** See {@link #agencyPermission}. */
		public String getTalentPermission() {
			return talentPermission;
		}

		/** See {@link #helpFile}. */
		public String getHelpFile() {
			return helpFile;
		}

		@Override
		public String toString() {
			String s = super.toString();
			s += "[" + mainTabIx + ", " + subTabIx + ", " + subText + "]";
			return s;
		}
	}

}