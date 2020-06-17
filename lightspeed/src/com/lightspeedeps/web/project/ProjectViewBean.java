package com.lightspeedeps.web.project;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.OnboardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.common.TimeZoneUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.onboard.form.FormActraIntentBean;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.production.ProductionListBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the Project edit/view page.
 */
@ManagedBean
@ViewScoped
public class ProjectViewBean extends ListView implements Serializable {
	/** */
	private static final long serialVersionUID = 4634957178851974000L;

	private static final Log log = LogFactory.getLog(ProjectViewBean.class);

	private static final int ACT_CHANGE_DEFAULT = 11;
	private static final int ACT_DELETE_UNIT = 12;
	private static final int ACT_IMPORT_PROJECT = 13;
	private static final int ACT_TRANSFER_PROJECT = 21;

	private static final int TAB_DETAILS = 0;
	private static final int TAB_PREFERENCES = 1;
//	private static final int TAB_OFFLINE = 3;

	// Fields
	private Production production;
	/** The currently selected project (in the list), whose data will
	 * be displayed in the right-hand panels. */
	private Project project;

	/** The "list" of productions for display -- this will always have only
	 * one production, the current Production.  This is done as a List so
	 * the JSP can use our usual table tags and styles. */
	private List<Production> productionList;

	/** The List of Project`s to display -- all the projects in the current
	 * Production. */
	private List<Project> projectList;

	private RowStateMap stateMap = new RowStateMap();

	/** The list of contacts on the Project Preferences tab, for assigning tasks.
	 * This only includes contacts who are members of the selected project. */
	private List<SelectItem> contactDL;

	private int selectedProdRow = 0;

	/** Mapping from Unit number to number of Cast department members in that Unit. */
	private Map<Integer,Integer> castCount = new HashMap<>();

	/** Mapping from Unit number to number of crew members (excluding Admin roles) in that Unit. */
	private Map<Integer,Integer> crewCount = new HashMap<>();

	private String curShootDate;

	private Date[] lastShootDate;

	/** Save the project title for comparison upon saving. */
	private String originalTitle;

	/** Save 'daysHeldBeforeDrop' for comparison upon saving. */
	private int originalDaysHeld;

	/** For productions that may have more than one project, this flag indicates if the
	 * selection is currently on a Project line rather than the Production display line. */
	private boolean projectSelected = true;

	/** True if certain attributes/settings of the project are now locked. This is
	 * done when at least one timecard exists for the project. */
	private boolean locked = false;

	/** True indicates that this is a production which can only have a single project,
	 * e.g., a Feature Film.  It is set during initialization based on the Production type.
	 * For BETA testing, a checkbox is provided on the page, and the setStyleFilm()
	 * method toggles the setting and fakes a single-project production if necessary. */
	private boolean styleFilm;

	/** True if this is an episodic (e.g., TV series) production, and the Production
	 * line is selected on the page (which also means that no Project line is selected). */
	private boolean styleTVProduction;

	/** True if this is an episodic (e.g., TV series) production, and any Project line
	 * is selected on the page (which also means the Production line is NOT selected). */
	private boolean styleTVProject;

	// Add Project (Episode/Job) pop-up fields

	/** True iff "Add Project" pop-up should be displayed */
	private boolean showAddProject = false;
	/** Project name for Add Project pop-up. */
	private String apName;
	/** Project number for Add Project pop-up. */
	private String apNumber;
	/** Start date for Add Project pop-up. */
	private Date apStartDate;
	/** End date for Add Project pop-up. */
	private Date apEndDate;

	/** Field for the "copy members" checkbox on the Add Project pop-up. */
	private boolean copyMembers;
	/** Field for the "copy all preferences" checkbox on the Add Project pop-up. */
	private boolean copyPreferences;
	/** Field for the "copy all timecard approvers" checkbox on the Add Project pop-up. */
	private boolean copyTcApprovers;
	/** Field for the "copy all onboarding approvers and approval paths" checkbox on the Add Project pop-up. */
	private boolean copyOnbApproversPaths;
	/** Field for the "copy all onboarding packets" checkbox on the Add Project pop-up. */
	private boolean copyOnbPackets;

	/** backing field for Worker's Comp checkbox, which will be copied to the PayrollPreference. */
	private boolean workersComp;

	/** backing field on Add Project popup for Worker's Comp checkbox, which will be set in the PayrollPreference. */
	private boolean addWorkersComp;

	/** The database id of the Unit on which the user has clicked the "delete" icon. */
	private Integer removeUnitId;

	/** A collection of the database ids of any Unit`s added during the current
	 * Edit session. */
	private final Set<Integer> addedUnits = new HashSet<>();

	/** TRUE if the current user has Edit_Project permission for the selected
	 * project (not their currently-signed-in project). Used in JSP code. */
	private boolean permEdit;

	/** TRUE if the current user has View_Project_Preferences permission for the selected
	 * project (not their currently-signed-in project). Used in JSP code. */
	private boolean permViewPref;

	/** TRUE if the current user has Edit_Project_Preferences permission for the selected
	 * project (not their currently-signed-in project). Used in JSP code. */
	private boolean permEditPref;

	/** TRUE if the current user has Manage_Projects permission for the selected
	 * project (not their currently-signed-in project). Used in JSP code. */
	private boolean permManage;

	/** TRUE if the current user has View_Projects_Details permission for the selected
	 * project (not their currently-signed-in project). Used in JSP code. */
	private boolean permDetails;

	// * * * Offline Tab fields * * *

	private File importFile;

	private String importFileName;

	// Offline options set via check-boxes

	/** Load any new Scripts into online system. */
	private boolean loadNewScripts = true;
	/** Load ScriptElements & attributes into online system. */
	private boolean loadElements = true;
	/** Load breakdown information - scene/script element relations - into online system. */
	private boolean loadBreakdown = true;
	/** Load Calendar information into online system. */
	private boolean loadCalendar = true;
	/** Load all Stripboard information into online system. */
	private boolean loadStripboard = true;
	/** State/Province list for Agency Address */
	private List<SelectItem> agencyStateProvinceList;
	/** State/Province list for Production Address */
	private List<SelectItem> prodStateProvinceList;
	private transient ProjectDAO projectDAO;
	private transient ProductionDAO productionDAO;
	/**  set the project or production LS-2056 change production to project terms*/
	private String projectTitle;
	private String projectJobTitle;
	/** Label for the Production section */
	private String productionSectionLbl;
	/** Production Label LS-2056 */
	private String productionLbl;

	/** Select item list of office to send documents to. Current just used for Canada */
	private List<SelectItem>officeListDL;

	private transient OfficeDAO officeDAO;

	/** The FormActraIntentBean instance . */
	private transient FormActraIntentBean formActraIntentBean;

	private transient FormActraIntentDAO formActraIntentDAO;

	@Transient
	private Boolean isUdaForm=false;

	@Transient
	private Boolean isActraForm=true;

	/* Constructor */
	public ProjectViewBean() {
		super(Project.SORTKEY_CODE, "Project.");
		log.debug("Init");
		try {
			projectList = createProjectList();
			production = SessionUtils.getProduction();
			productionList = new ArrayList<>(1);
			productionList.add(production);

			if (production.getType().getEpisodic()) {
				projectSelected = true;
				styleTVProject = true;
			}
			else {
				styleFilm = true;
			}

			Integer id = SessionUtils.getInteger(Constants.ATTR_PROJECT_VIEW_ID);
			setupSelectedItem(id);

			if(production.getType() == ProductionType.CANADA_TALENT) {
				initCanadaProject();
				setProjectTitle(Constants.PROJECT_TEXT_CANADA);
				setProjectJobTitle(Constants.PROJECT_TEXT_CANADA);
				setProductionSectionLbl(Constants.CANADA_PRODUCTION_COMPANY_TEXT);
				setProductionLbl(Constants.CANADA_PRODUCTION_TEXT);
			}
			else {
				setProjectTitle(Constants.PROJECT_TEXT_US);
				setProjectJobTitle(Constants.PROJECTJOB_TEXT);
				setProductionSectionLbl(Constants.STANDARD_PRODUCTION_COMPANY_TEXT);
				setProductionLbl(Constants.STANDARD_PRODUCTION_TEXT);
			}

			scrollToRow(project); // ensure selected item is visible
			checkTab(); // restore last selected mini-tab
			restoreSortOrder();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public static ProjectViewBean getInstance() {
		return (ProjectViewBean)ServiceFinder.findBean("projectViewBean");
	}

	/**
	 * Determine which element we are supposed to view. If the id given is null or invalid, we try
	 * to display the "default" element.
	 *
	 * @param id
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id="+id);
		if (project != null) {
			project.setSelected(false);
		}
		contactDL = null;	// force refresh
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_PROJECT_VIEW_ID, null);
			project = null;
			editMode = false;
			newEntry = false;
		}
		else {
			project = getProjectDAO().findById(id);
			if (project == null) {
				id = findDefaultId();
				if (id != null) {
					project = getProjectDAO().findById(id);
				}
			}
		}
		if (project != null) {
			project.setSelected(true);
			if (production.getType().hasPayrollByProject()) {
				setWorkersComp(project.getPayrollPref().getWorkersComp());
			}
			// Clear the row state map from previous selected state.
			stateMap.clear();
			RowState state = new RowState();
			state.setSelected(true);
			stateMap.put(project, state);

			SessionUtils.put(Constants.ATTR_PROJECT_VIEW_ID, id);
			lastShootDate = new Date[project.getUnits().size()];
			int ix = 0;
			for (Unit unit : project.getUnits()) {
				ScheduleUtils scheduleUtils = new ScheduleUtils(unit);
				lastShootDate[ix++] = scheduleUtils.getEndDate();
			}
			originalDaysHeld = project.getDaysHeldBeforeDrop();
			castCount.clear();
			crewCount.clear();
			for (Unit unit : project.getUnits()) {
				//log.debug(ProjectMemberDAO.getInstance().findMemberCount(unit));
				int cast = 0, crew = 0;
				for (ProjectMember projectMember : unit.getProjectMembers()) {
					if (projectMember.getEmployment() != null) {
						// note that getProjectMembers() will not include contacts that
						// have production-wide roles, but those are only admin roles,
						// which wouldn't be counted anyway.
						if (projectMember.getEmployment().getRole().isCastOrStunt()) {
							cast++;
						}
						else if (projectMember.getEmployment().getRole().isCrewNotStunt()) {
							crew++;
						}
					}
				}
				castCount.put(unit.getNumber(), cast);
				crewCount.put(unit.getNumber(), crew);
			}
			locked = false;
			if (production.getType().hasPayrollByProject()) {
				locked = WeeklyTimecardDAO.getInstance().existsTimecardsForProject(project);
			}
			updatePermissions();
			forceLazyInit();
			log.debug("Project = " + project);
			if (getProduction().getType().isCanadaTalent()) {
				findRelatedIntentForm(project);
			}
		}
	}

	/**
	 * Initialize a project that is within a Canadian production.
	 */
	private void initCanadaProject() {
		agencyStateProvinceList = new ArrayList<>();
		prodStateProvinceList = new ArrayList<>();

		// Canadian Talent production so populate the agency and production house
		// state/province lists based on the values of the Canada Project Details.
		// If those values have not been set then default to  Canada(CA)
		CanadaProjectDetail cpd = project.getCanadaProjectDetail();
		Address address = cpd.getAgencyAddress();
		String countryCode = address.getCountry();
		if (countryCode == null || countryCode.isEmpty()) {
			countryCode = Constants.COUNTRY_CODE_CANADA;
		}
		agencyStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL(countryCode);

		address = cpd.getProdHouseAddress();
		countryCode = address.getCountry();
		if (countryCode == null || countryCode.isEmpty()) {
			countryCode = Constants.COUNTRY_CODE_CANADA;
		}
		prodStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL(countryCode);

		if (project.getUdaProjectDetail() == null) {
			UdaProjectDetail udaProjectDetail = new UdaProjectDetail();
			project.setUdaProjectDetail(udaProjectDetail);
			getProjectDAO().save(udaProjectDetail);
			getProjectDAO().attachDirty(project);
		}
	}

	/** Finds the ACTRA Intent to produce Form related to the given Project.
	 * @param proj
	 */
	private void findRelatedIntentForm(Project proj) {
		ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
		ContactDocument cd = contactDocumentDAO.findIntentContactDocumentForProject(proj);
		log.debug("");
		if (cd == null) {
			log.debug("");
			OnboardService onboardService = new OnboardService();
			Document intentDoc = DocumentDAO.getInstance().findIntentDocument(getProduction());
			if (intentDoc != null) {
				log.debug("INTENT DOC = " + intentDoc.getId());
				Integer id = onboardService.saveContactDocument(null, SessionUtils.getCurrentContact(), intentDoc, null, ApprovalStatus.OPEN, proj);
				if (id != null) {
					cd = contactDocumentDAO.findById(id);
				}
			}
		}
		if (cd != null) {
			log.debug("ContactDocument = " + cd.getId());
			getFormActraIntentBean().setContactDoc(cd);
			getFormActraIntentBean().setSelectedProject(proj);
			getFormActraIntentBean().setUpForm();
		}
	}

	/**
	 *
	 */
	public void updatePermissions() {
		AuthorizationBean bean = AuthorizationBean.getInstance();
		permEdit = bean.hasPermission(SessionUtils.getCurrentContact(),
				project, Permission.EDIT_PROJECT);
		permViewPref = bean.hasPermission(SessionUtils.getCurrentContact(),
				project, Permission.VIEW_PROJECT_PREFERENCES);
		permEditPref = bean.hasPermission(SessionUtils.getCurrentContact(),
				project, Permission.EDIT_PROJECT_PREFERENCES);
		permManage = bean.hasPermission(SessionUtils.getCurrentContact(),
				project, Permission.MANAGE_PROJECTS);
		permDetails = bean.hasPermission(SessionUtils.getCurrentContact(),
				project, Permission.VIEW_PROJECT_DETAILS);
	}

	private Integer findDefaultId() {
		Integer id = SessionUtils.getCurrentProject().getId();
		return id;
	}

	private void forceLazyInit() {
		try {
			production = getProductionDAO().refresh(production);
			for (ReportRequirement rr : project.getReportRequirements()) {
				if (rr.getContact() != null) {
					rr.setContact(ContactDAO.getInstance().refresh(rr.getContact()));
					rr.getContact().getUser().getLastNameFirstName();
				}
			}
			production.getAddress().getAddrLine1();
			for (Unit unit : project.getUnits()) {
				unit.getProjectSchedule().getStartDate();
			}
			project.getMainUnit().getProjectSchedule().getStartDate();
			project.toString();
			if (production.getType().hasPayrollByProject()) {
				PayrollPreference pref = project.getPayrollPref();
				pref.getWorkersComp();
			}
			if (production.getType().isCanadaTalent()) {
				project.getCanadaProjectDetail().getAgencyName();
				project.getCanadaProjectDetail().getAgencyAddress().getAddrLine1();
				project.getCanadaProjectDetail().getProdHouseAddress().getAddrLine1();
				if (project.getCanadaProjectDetail().getOffice() != null) {
					project.getCanadaProjectDetail().getOffice().getEmailAddress();
				}
				if (project.getUdaProjectDetail() != null) { // Avoid LIE on UDA fields
					project.getUdaProjectDetail().getProducerName();
					if (project.getUdaProjectDetail().getProducerAddress() != null) {
						project.getUdaProjectDetail().getProducerAddress().getAddrLine1();
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Project)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	public String actionEdit() {
		production = SessionUtils.getProduction();
		production = getProductionDAO().refresh(production);
		project = getProjectDAO().refresh(project);
		originalTitle = project.getTitle();
		forceLazyInit();
		if (production.getType().isCanadaTalent()) {
			getFormActraIntentBean().setEditMode(true);
		}
		return super.actionEdit();
	}

	/**
	 * Action method for the "Save" button that saves preferences, etc.
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		try {
			log.debug("");
			if (project.getTitle() == null || project.getTitle().trim().length() == 0) {
				MsgUtils.addFacesMessage("Project.BlankName", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAILS);
				return null;
			}
			if (production.getTitle() == null || production.getTitle().trim().length() == 0) {
				MsgUtils.addFacesMessage("Project.BlankProductionTitle", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAILS);
				return null;
			}
			if (! ProductionListBean.validateEmailSender(production)) {
				MsgUtils.addFacesMessage("Email.InvalidPrefix", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAILS);
				return null;
			}
			if ( ! styleTVProduction && project.getOriginalEndDate() != null
					&& project.getOriginalEndDate().before(project.getMainUnit().getProjectSchedule().getStartDate())) {
				MsgUtils.addFacesMessage("apEndDate", "Project.EndDateEarly", FacesMessage.SEVERITY_ERROR);
				setSelectedTab(TAB_DETAILS);
				return null;
			}
			if (styleFilm || styleTVProduction) {
				TimeZone tz = TimeZoneUtils.getTimeZone(production.getTimeZoneStr());
				if (tz == null) { // no match
					MsgUtils.addFacesMessage("Production.UnknownTimeZone", FacesMessage.SEVERITY_ERROR,
							production.getTimeZoneStr());
					setSelectedTab(TAB_DETAILS);
					return null;
				}
				production.setTimeZone(tz);
				if (styleFilm) {
					project.setTimeZone(tz);
				}
				else {
					production.setTitle(production.getTitle().trim());
					if (production.getType().hasPayrollByProject()) {
						// for Commercial productions, we sync studio (prod company) to title
						production.setStudio(production.getTitle());
					}
				}
			}
			if (styleTVProject) { // Episodic, and a project row is selected
				if (production.getType().hasPayrollByProject()) {
					PayrollPreference pref = project.getPayrollPref();
					pref.setWorkersComp(workersComp);
					getProjectDAO().attachDirty(pref);
				}
			}
			if (project.getHasUnits()) {
				String name;
				Collection<String> names = new HashSet<>();
				for (Unit unit : project.getUnits()) {
					name = unit.getName();
					if (name == null || name.trim().length() == 0) {
						MsgUtils.addFacesMessage("Unit.BlankName", FacesMessage.SEVERITY_ERROR);
						setSelectedTab(TAB_PREFERENCES);
						return null;
					}
					name = name.trim();
					if (! names.add(name.toLowerCase())) {
						MsgUtils.addFacesMessage("Unit.DuplicateName", FacesMessage.SEVERITY_ERROR, name);
						setSelectedTab(TAB_PREFERENCES);
						return null;
					}
					unit.setName(name); // in case trim() changed it
				}
				addedUnits.clear();
			}
			boolean titleChanged = false;
			if (styleFilm) {
				project.setTitle(production.getTitle());	// copy title
				project.setNotifying(production.getNotify()); // copy notification setting
			}
			else if (! styleTVProduction) {
				project.setTitle(project.getTitle().trim());
				if (! project.getTitle().equalsIgnoreCase(originalTitle.trim())) {
					titleChanged = true;
					List<Project> list = getProjectDAO().findByProductionAndTitle(production, project.getTitle());
					if (list != null && list.size() > 0) {
						MsgUtils.addFacesMessage("Project.DuplicateName", FacesMessage.SEVERITY_ERROR, project.getTitle());
						setSelectedTab(TAB_DETAILS);
						return null;
					}
				}
			}
			if (!newEntry) {
				for (ReportRequirement rr : project.getReportRequirements()) {
					if (rr.getContact() != null) {
						Contact ct = ContactDAO.getInstance().refresh(rr.getContact());
						// These seem to fix duplicate instance of Role and (maybe) Production.
						getProjectDAO().evict(ct.getRole());
						getProjectDAO().evict(ct.getProduction());
					}
				}
				contactDL = null; // also release any duplicate Contact instances
				if (styleTVProduction || styleFilm) {
					AddressDAO.getInstance().attachDirty(production.getAddress());
				}

				project.setUpdated(new Date());
				if (project.getDaysHeldBeforeDrop() == null) {
					project.setDaysHeldBeforeDrop(0);
				}
				getProjectDAO().attachDirty(project); // LS-2059 Save project before production

				production = getProductionDAO().merge(production); // 4.5.0=attach;4.6.0=merge
				for (Project proj : production.getProjects()) { // LS-2048
					if (proj.equals(project)) {
						project = proj; // get updated object instance
						break;
					}
				}
				if (styleTVProject) {
					project.setSelected(true);
					production.setSelected(false);
				}
				if (originalDaysHeld != project.getDaysHeldBeforeDrop()) {
					ScriptElementDAO.getInstance().updateDaysHeld(project);
				}
			}
			else {
				getProjectDAO().attachDirty(getElement());
				refreshList(true);
			}
			if (production.getType().getCrossProject()) {
				// Check for unit name changes and apply to all projects if found.
				boolean changed = false;
				for (Project proj : production.getProjects()) {
					if (! proj.equals(project)) {
						for (Unit unit : proj.getUnits()) {
							Unit current = project.getUnit(unit.getNumber());
							if (current != null && ! current.getName().equals(unit.getName())) {
								changed = true;
								unit.setName(current.getName());
								getProjectDAO().attachDirty(unit);
							}
						}
						if (! changed) { // got through one project & all matched,
							// therefore no unit was renamed, so no need to check all the other projects.
							break;
						}
					}
				}
			}
			if (titleChanged) {
				HeaderViewBean.reset();
			}
			if (project.getNotifying() && project.getNotificationChanged() == null) {
				project.setNotificationChanged(new Date());
				getProjectDAO().attachDirty(project);
				// DoNotification.getInstance().welcomeMessageProject(project);
			}
			SessionUtils.put(Constants.ATTR_PROJECT_VIEW_ID, getElement().getId());
			if (production.getType().isCanadaTalent()) {
				FormActraIntent form = getFormActraIntentBean().getForm();
				getFormActraIntentBean().refreshForm();
				//Populate certain fields from Project Details to Intent to Produce form
				// LS-2894 Cannot Overwrite Auto populated Fields on ACTRA Intent to Produce
				if (form.getAdvertiser() == null || form.getAdvertiser().isEmpty()) {
					form.setAdvertiser(project.getCanadaProjectDetail().getAdvertiserName());
				}
				if (form.getAgencyName() == null || form.getAgencyName().isEmpty()) {
					form.setAgencyName(project.getCanadaProjectDetail().getAgencyName());
				}
				if (form.getProducerName() == null || form.getProducerName().isEmpty()) {
					form.setProducerName(project.getCanadaProjectDetail().getAgencyProducer());
				}
				if (form.getProductionHouseName() == null ||
						form.getProductionHouseName().isEmpty()) {
					form.setProductionHouseName(project.getCanadaProjectDetail().getProdHouseName());
				}
				if (form.getDirectorName() == null || form.getDirectorName().isEmpty()) {
					form.setDirectorName(project.getCanadaProjectDetail().getDirectorName());
				}
				if (form.getProduct() == null || form.getProduct().isEmpty()) {
					form.setProduct(project.getCanadaProjectDetail().getBrandName());
				}
				// LS-3087
				if (StringUtils.isEmpty(form.getCommercial1().getName())) {
					if (project.getCanadaProjectDetail().getCommercialName() != null) {
						form.getCommercial1()
								.setName(project.getCanadaProjectDetail().getCommercialName());
					}
				}
				getFormActraIntentDAO().merge(form);
				getFormActraIntentBean().actionSave();
			}
			refreshProject();
			forceLazyInit();
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;

	}

	/** Action method for the main (project details) Cancel button */
	@Override
	public String actionCancel() {
		try {
			refreshProject();
			production = getProductionDAO().refresh(production);
			if (addedUnits.size() > 0) {
				deleteAddedUnits();
			}
			refreshList(true);	// in case name or other list info changed
			HeaderViewBean.reset();
			if (production.getType().isCanadaTalent()) {
				getFormActraIntentBean().actionCancel();
			}
			super.actionCancel();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user clicks the "Add Project" button on the project
	 * details page. This sets up the default values and causes the pop-up
	 * dialog to be displayed.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionNew() {
		log.debug("");
		if (editMode) {
			PopupBean.getInstance().show(null, 0,
					getMessagePrefix()+"AddSaveFirst.Title",
					getMessagePrefix()+"AddSaveFirst.Text",
					"Confirm.OK",
					null); // no cancel button
//			addClientResize();
			return null;
		}
		Production prod = SessionUtils.getProduction();
		if (prod.getMaxProjects() <= prod.getProjects().size()) {
			PopupBean conf = PopupBean.getInstance();
			conf.show(null, 0,
					"Project.MaxProjectsInProduction.Title",
					"Confirm.Close",
					null);
			String msg = MsgUtils.formatMessage("Project.MaxProjectsInProduction.Text",
					prod.getMaxProjects());
			conf.setMessage(msg);
			return null;
		}
		newEntry = true;
		editMode = true;
		//setupSelectedItem(NEW_ID);
		apName = "";
		int seq = getProjectDAO().findMaxSequence(prod) + 1;
		apNumber = "" + seq;
		Date date = CalendarUtils.todaysDate();
		apStartDate = date;
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.add(Calendar.MONTH, 1);
		apEndDate = cal.getTime();
		copyMembers = true;
		copyPreferences = true;

		if (production.getType() == ProductionType.CANADA_TALENT) {
			copyMembers = false;
		}
		showAddProject = true;
		copyTcApprovers = false;
		copyOnbApproversPaths = false;
		copyOnbPackets = false;
//		addClientResize();
		addFocus("addProject");
		return null;
	}

	/**
	 * Force the project list to be refreshed.
	 */
	@Override
	public void refreshList() {
		refreshList(false);
	}

	private void refreshList(boolean pickElement) {
		projectList = createProjectList();
		setSelectedRow(-1);
		if (styleFilm) {
			project = projectList.get(0);
			projectSelected = false;
			projectList.clear();
			projectList.add(project);
		}
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<Project> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(project);
				if (ix < 0) {
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					project = list.get(ix);
					if (styleTVProject) {
						getElement().setSelected(true);
					}
					forceLazyInit();
				}
			}
			else {
				// no current element & not doing "Add"; if list has entries, view the first
				if (list.size() > 0) {
					setupSelectedItem(list.get(0).getId());
				}
			}
		}
//		addClientResize();
	}

	private List<Project> createProjectList() {
		List<Project> list = getProjectDAO().findByProduction();
		for (Project p : list) {
			p.setSelected(false);
		}
//		if (projectStatus.equalsIgnoreCase("ALL")) {
//			projectList = getProjectDAO().findByProduction();
//		}
//		else if (projectStatus.equalsIgnoreCase("ARCHIVED")) {
//			projectList = getProjectDAO().findByStatus("OFFLINE");
//		}
//		else if (projectStatus.equalsIgnoreCase("INUSE")) {
//			projectList = getProjectDAO().findByProjectStatus();
//		}
		return list;
	}

	/** Create  drop-down List for the contact list for task assignment table. */
	private List<SelectItem> createContactList() {
		List<SelectItem> contacts = new ArrayList<>();
		List<Contact> contactList = ContactDAO.getInstance().findCrew(false);
		for (ReportRequirement rr : project.getReportRequirements()) {
			if (rr.getContact() != null && ! contactList.contains(rr.getContact())) {
				contactList.add(rr.getContact());
			}
		}

		if (contactList.size() > 0) {
			Collections.sort(contactList);
			contacts.add(new SelectItem(null, Constants.SELECT_HEAD_NO_TASK_OWNER));
			for (Contact contact : contactList) {
				contacts.add( new SelectItem(contact, contact.getUser().getLastNameFirstName()) );
			}
		}
		return contacts;
	}

	/**
	 * The ValueChangeListener method called by the "Make default" checkbox.
	 * Here we just put up the confirmation dialog.
	 */
	public void actionChangeDefault(ValueChangeEvent evt) {
		PopupBean conf = PopupBean.getInstance();
		conf.show( this, ACT_CHANGE_DEFAULT,
				"Project.Default.Title",
				"Project.Default.Ok",
				"Confirm.Cancel");
		String msg = MsgUtils.formatMessage("Project.Default.Text", project.getTitle());
		conf.setMessage(msg);
//		addClientResize();
	}

	/**
	 * Set the Default project for all users. Called via confirmOk, after the
	 * user confirms the request.
	 */
	public String actionChangeDefaultOk() {
		log.debug("");
		try {
			if (project != null) {
				project = getProjectDAO().findById(project.getId()); // fix lazy init errors
				Contact user = SessionUtils.getCurrentContact();
//				log.debug("changing default project: user=" + user.getId() + ", proj=" + project.getId());
				getProjectDAO().changeDefaultProject(project, user);
				production = getProductionDAO().refresh(production);
				refreshList(true);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for "Cancel" button on "Add project" pop-up
	 *
	 * @return null navigation string
	 */
	public String cancelAddProject() {
		showAddProject = false;
		newEntry = false;
		editMode = false;
		return null;
	}

	/**
	 * Action method for the "Add" button on the "Add project" pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionAddProject() {
		log.debug("");
		try {
			boolean done = false;
			if (apName == null || apName.trim().length() == 0 ) {
				MsgUtils.addFacesMessage("apName", "Project.BlankName", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			apName = apName.trim();
			List<Project> list = getProjectDAO().findByProductionAndTitle(apName);
			if (list != null && list.size() > 0) {
				MsgUtils.addFacesMessage("Project.DuplicateName", FacesMessage.SEVERITY_ERROR, apName);
			}
			else {
				Project newProject = null;
				try {
					showAddProject = false;
					newProject = new Project();
					newProject.setTitle(apName);
					newProject.setEpisode(apNumber);
					newProject.setOriginalEndDate(apEndDate);
					Production prod = SessionUtils.getProduction();
					newProject.setProduction(prod);
					newProject.setTimeZoneStr(prod.getTimeZoneStr());
					Project sourceProject = SessionUtils.getCurrentProject();
					done = getProjectDAO().createNewProject(newProject, sourceProject, apStartDate,
							copyPreferences, copyMembers, false, copyTcApprovers, copyOnbApproversPaths, copyOnbPackets);
				}

				catch (Exception e) {
					EventUtils.logError(e);
					MsgUtils.addGenericErrorMessage();
					return null;
				}
				originalTitle = newProject.getTitle();
				if (done) {
					if (production.getType().isAicp()) {
						try {
							PayrollPreference pref = newProject.getPayrollPref();
							pref.setWorkersComp(addWorkersComp);
							getProjectDAO().attachDirty(pref);
						}
						catch (Exception e) {
							EventUtils.logError(e);
							MsgUtils.addGenericErrorMessage();
						}
					}
					if (production.getType() == ProductionType.CANADA_TALENT) {
						HeaderViewBean.getInstance().setupNewProject(newProject.getId());
						initCanadaProject();
						// Update approval path if user is in Production. LS-3661
						Contact contact = SessionUtils.getCurrentContact();
						if (contact.getRole().getDepartment().getId() == Constants.DEPARTMENT_ID_ACTRA_PRODUCTION) {
							ApproverUtils.addContactToApprovalPool(contact, newProject);
						}
					}

					refreshList(false);
					setupSelectedItem(newProject.getId());
					newEntry = false;
					HeaderViewBean.reset();	// refresh project list in page header
					MsgUtils.addFacesMessage("Project.ProjectAdded", FacesMessage.SEVERITY_INFO, apName);
//					if (newProject.getNotifying()) {
//						//DoNotification.getInstance().welcomeMessageProject(newProject);
//					}
				}
				else {
					MsgUtils.addFacesMessage("Project.AddProjectFailed", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionAddUnit() {
		log.debug("");
		apStartDate = new Date();
		try {
			//project = getProjectDAO().refresh(project);
			if (project.getUnits().size() >= Constants.MAX_UNITS) {
				MsgUtils.addFacesMessage("Unit.TooMany", FacesMessage.SEVERITY_ERROR, Constants.MAX_UNITS);
				return null;
			}
			Unit unit = null;
			try {
				unit = getProjectDAO().createNewUnit(project, apStartDate, null);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			if (unit != null) {
				project = unit.getProject(); 		// may be refreshed...
				production = project.getProduction(); // ...which might change this one
				// Added above 2 lines to fix NonUniqueObjectException during rev 3107 testing
				addedUnits.add(unit.getId()); // track in case user Cancels
				refreshList(false);
				setupSelectedItem(project.getId());
				newEntry = false;
				Contact user = SessionUtils.getCurrentContact();
				Contact contact = SessionUtils.getCurrentContact();
				if (project.equals(contact.getProject())) {
					// User's current project is the one being updated...
					AuthorizationBean.getInstance().auth(user, true); // refresh authorization map
				}
				MsgUtils.addFacesMessage("Unit.Added", FacesMessage.SEVERITY_INFO, unit.getName());
//				if (project.getNotifying()) {
//					DoNotification.getInstance().welcomeMessageProject(newProject);
//				}
			}
			else {
				MsgUtils.addFacesMessage("Unit.AddFailed", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Delete any Units and related objects that were added in the last edit session.
	 */
	private void deleteAddedUnits() {
		try {
			String msg = "add units cancelled, ids=";
			for (Integer unitId : addedUnits) {
				Unit unit = UnitDAO.getInstance().findById(unitId);
				if (unit != null) {
					msg += unitId + " ";
					getProjectDAO().removeUnit(project, unit);
				}
			}
			addedUnits.clear();
			refreshProject();
			ChangeUtils.logChange(ChangeType.UNIT, ActionType.DELETE, project, msg);
			Contact contact = SessionUtils.getCurrentContact();
			if (project.equals(contact.getProject())) {
				// User's current project is the one being updated...
				AuthorizationBean.getInstance().auth(contact, true); // refresh authorization map
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to clear all the fields from project details page for canada
	 * production LS-2070
	 *
	 * @return
	 */
	public String actionClear() {
		if (isActraForm) {
			CanadaProjectDetail cpd = getProject().getCanadaProjectDetail();
			if (cpd != null) {
				cpd.clearFields();
			}
		}
		if (isUdaForm) {
			UdaProjectDetail upd = getProject().getUdaProjectDetail();
			if (upd != null) {
				upd.clearFields();
			}
		}
		return null;
	}
	/**
	 * Delete the currently selected Project. This is an Action method called by
	 * the Delete button. Here we just put up the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionDelete() {
		try {
			production = getProductionDAO().refresh(production);
			if (WeeklyTimecardDAO.getInstance().existsTimecardsForProject(project)) {
				// Don't allow delete if the Project has associated timecards
				MsgUtils.addFacesMessage("Project.HasTimecards", FacesMessage.SEVERITY_ERROR);
			}
			else if ((! production.getType().isAicp()) && production.getDefaultProject().equals(project)) {
				MsgUtils.addFacesMessage("Project.CantDeleteDefault", FacesMessage.SEVERITY_ERROR);
			}
			else if (SessionUtils.getCurrentProject().equals(project) && !production.getType().isCanadaTalent()) {
				// Allow Canadian Talent productions to delete the current project LS-1853
				MsgUtils.addFacesMessage("Project.CantDeleteCurrent", FacesMessage.SEVERITY_ERROR);
			}
			else if (production.getProjects().size() == 1) {
				MsgUtils.addFacesMessage("Project.CantDeleteLast", FacesMessage.SEVERITY_ERROR);
			}
			else {
				PopupBean conf = PopupBean.getInstance();
				conf.show(this, ACT_DELETE_ITEM,
						"Project.Delete.Title",
						"Project.Delete.Ok",
						"Confirm.Cancel");
				String msg = MsgUtils.formatMessage("Project.Delete.Text", project.getTitle());
				conf.setMessage(msg);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#actionDeleteOk()
	 */
	@Override
	protected String actionDeleteOk() {
		try {
			Project delProject = getProject();
			Integer id = delProject.getId();
			Contact contact = SessionUtils.getCurrentContact();
			Integer userProjectId = contact.getProject().getId();
			delProject = getProjectDAO().findById(id); // refresh
			String msg = "id=" + delProject.getId() + ", title=" + delProject.getTitle();
			production = getProductionDAO().refresh(production);
			if ((production.getType().isAicp()) && production.getDefaultProject().getId().equals(id)) {
				// Deleting the "default project" in a commercial production.
				// Find any other project to make the default.
				Project defP = null;
				boolean found = false;
				production.getProjects().iterator();
				for (Iterator<Project> iter = production.getProjects().iterator(); iter.hasNext();  ) {
					defP = iter.next();
					if (! defP.equals(delProject)) {
						found = true; // found one other than the one being deleted.
						break;
					}
				}
				if (found) { // should always be true!
					production.setDefaultProject(defP);
					getProjectDAO().attachDirty(production);
				}
			}
			production.getProjects().remove(delProject);
			getProjectDAO().remove(delProject, true); // this may change user's current project
			ChangeUtils.logChange(ChangeType.PROJECT, ActionType.DELETE, msg);
			if (id.equals(userProjectId)) {
				// Switch current project to the current user's (maybe new) default project...
				SessionUtils.setCurrentProject(contact.getProject());
				AuthorizationBean.getInstance().auth(contact, true); // update authorization map
			}
			project = null;
			int ix = getSelectedRow();
			refreshList(false);
			setupSelectedItem(getRowId(ix));
			//addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO,
					"Delete failed; please try again.");
		}
		return null;
	}

	/**
	 * Delete the specified Unit. This is an Action method called by the Delete
	 * icon on one of the detail lines in the Unit table. Here we just put up
	 * the confirmation dialog.
	 */
	public String actionDeleteUnit() {
		try {
			Unit unit = UnitDAO.getInstance().findById(removeUnitId);
			if (unit.getNumber() == 1) {
				MsgUtils.addFacesMessage("Unit.CantDeleteMain", FacesMessage.SEVERITY_ERROR);
			}
			else {
				PopupBean conf = PopupBean.getInstance();
				conf.show(this, ACT_DELETE_UNIT,
						"Unit.Delete.Title",
						"Unit.Delete.Ok",
						"Confirm.Cancel");
				String msg = MsgUtils.formatMessage("Unit.Delete.Text", unit.getName());
				conf.setMessage(msg);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Delete the unit specified by its database id.
	 */
	protected String actionDeleteUnitOk() {
		try {
			Unit unit = UnitDAO.getInstance().findById(removeUnitId);
			String msg = "id=" + removeUnitId + ", proj=" + project.getTitle() + ", name=" + unit.getName();

			// We'll need to update any default role & department that are no longer valid because
			// that role will be deleted along with the unit.
			List<Contact> contacts = ProjectMemberDAO.getInstance().findByUnitExactDistinctContact(unit);

			getProjectDAO().removeUnit(project, unit);
			refreshProject();
			production = project.getProduction();

			ContactDAO.getInstance().updateRoles(contacts);

			ChangeUtils.logChange(ChangeType.UNIT, ActionType.DELETE, project, msg);
			Contact contact = SessionUtils.getCurrentContact();
			if (project.equals(contact.getProject())) {
				// User's current project is the one being updated...
				AuthorizationBean.getInstance().auth(contact, true); // refresh authorization map
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO,
					"Unit delete failed; please try again.");
		}
		return null;
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation
	 * dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 *
	 * @return null navigation string (since none of these actions navigate to a
	 *         new page).
	 */
	@Override
	public String confirmOk(int action) {
		log.debug(action);
		String res = null;
		switch(action) {
			case ACT_CHANGE_DEFAULT:
				res = actionChangeDefaultOk();
				break;
			case ACT_DELETE_UNIT:
				res = actionDeleteUnitOk();
				break;
			case ACT_IMPORT_PROJECT:
				res = actionLoadOk();
				break;
			case ACT_TRANSFER_PROJECT:
				res = actionTransferOk();
				break;
			default:
				res = super.confirmOk(action);
		}
		return res;
	}

	public String actionTransfer() {
		// Project transfer is no longer needed. 12/11/18
//		production = SessionUtils.getProduction();
//		refreshProject();
//		try {
//			PayrollPreference payrollPref = null;
//			if (production.getType().hasPayrollByProject()) {
//				payrollPref = project.getPayrollPref();
//			}
//			else {
//				payrollPref = production.getPayrollPref();
//			}
//			if (payrollPref == null) {
//				return null;// should never happen
//			}
//
//			PayrollService service = production.getPayrollPref().getPayrollService();
//			if (service == null) {
//				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoService", FacesMessage.SEVERITY_ERROR);
//				return null;
//			}
//
////			if (! service.getSendBatchMethod().isTeam()) {
////			}
//			if (! payrollPref.getUseEmail()) { // E-mail preference is not set...
//				MsgUtils.addFacesMessage("WeeklyBatch.SetEmailPreference", FacesMessage.SEVERITY_ERROR);
//				return null;
//			}
//			if (StringUtils.isEmpty(payrollPref.getBatchEmailAddress())) {
//				MsgUtils.addFacesMessage("TransferBean.SetEmailPreference", FacesMessage.SEVERITY_ERROR);
//				return null;
//			}
//
//			PopupBean bean = PopupBean.getInstance();
//			bean.show(this, ACT_TRANSFER_PROJECT, "Project.Transfer.");
//			bean.setMessage(MsgUtils.formatMessage("Project.Transfer.Text", project.getTitle()));
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//			MsgUtils.addGenericErrorMessage();
//		}
		return null;
	}

	/**
	 * Action method called when user hits OK on Transfer Project prompt. Called
	 * via confirmOk.
	 *
	 * @return empty navigation string
	 */
	private String actionTransferOk() {
		// Project transfer is no longer needed. 12/11/18
//		try {
//			refreshProject();
//			boolean ret = DocumentTransferService.getInstance().transmitProject(project);
//			if (ret) {
//				MsgUtils.addFacesMessage("Project.TransferSuccessful", FacesMessage.SEVERITY_INFO, 0);
//			}
//			else {
//				PopupBean bean = PopupBean.getInstance();
//				bean.show(null, 0, null, null, "Confirm.Close", null);
//				bean.setMessage(MsgUtils.formatMessage("Project.TransferError.Text", project.getTitle()));
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//			MsgUtils.addGenericErrorMessage();
//		}
		return null;
	}

	public String actionFakeOffline() {
		if (ApplicationUtils.isOffline()) {
			ApplicationUtils.setOffline(false);
		}
		else {
			ApplicationUtils.setOffline(true);
		}
		return null;
	}

	/**
	 * Action method for the "install software" button, used to download and
	 * install the Offline version of LightSPEED.
	 * @return null navigation string
	 */
	public String actionInstallOffline() {
		String javascriptCode = "window.open('../../files/LightSPEED.install.exe','LSexe');";
		addJavascript(javascriptCode);
		return null;
	}

	/**
	 * Action method for the "update software" button, used to download and install any
	 * updates to the Offline version of LightSPEED previously installed on the user's PC.
	 * @return null navigation string
	 */
	public String actionUpdateOffline() {
		String javascriptCode = "window.open('../../files/LightSPEED.update.exe','LSexe');";
		addJavascript(javascriptCode);
		return null;
	}

	/**
	 * Prompt user to continue Import project operation.  Called when
	 * the file upload has completed from the user's machine to the server.
	 */
	private void actionLoad() {
		production = getProductionDAO().refresh(production);
		PopupBean conf = PopupBean.getInstance();
		conf.show(this, ACT_IMPORT_PROJECT,
				"Project.Import.Title",
				"Project.Import.Ok",
				"Confirm.Cancel");
		String msg = MsgUtils.formatMessage("Project.Import.Text", project.getTitle());
		conf.setMessage(msg);
//		addClientResize();
	}

	/**
	 * Load the specified file, importFileName, (an absolute path) into the
	 * selected Project (not necessarily the user's "current" (active) Project).
	 * Called from the confirmation processing method.
	 */
	private String actionLoadOk() {
		refreshProject();
		if (importFileName != null) {
			//project = doLoad(project, importFileName);
			importFileName = null; // prevents accidental re-load if user double-clicks
			importFile = null; // ditto
		}
		refreshList(false);
		setupSelectedItem(project.getId());
		return null;
	}

	/**
	 * Export the given project -- create a file containing all the data necessary to
	 * import the project into a different (online or offline) system.
	 * @param project The Project to be exported.
	 */
	/*private void export(Project project) {
		log.debug("exporting: " + project.getTitle());
		DateFormat df = new SimpleDateFormat("MM-dd_HH-mm-ss");
		String stamp = df.format(new Date());

		String title = project.getTitle();
		title = title.replace('/', '-'); // ensure filename is legal
		String filename;
		if (ApplicationUtils.isOffline()) {
			filename = Constants.UPLOAD_PREFIX;
		}
		else {
			filename = Constants.DOWNLOAD_PREFIX;
		}
		filename += title + "." + stamp + Constants.EXPORT_TYPE;
		String path = Export.export(project, filename);
		if (path != null) {
			if (ApplicationUtils.isOffline()) {// offline mode - display filename created to user
				MsgUtils.addFacesMessage("Offline.exportOkToFile", FacesMessage.SEVERITY_INFO,
						project.getTitle(), path);
			}
			else { // we're online - push via browser
				String javascriptCode = "window.open('../../servlet/FileDownloadServlet?fileName="
					+ filename + "','Version');";
				addJavascript(javascriptCode);
				MsgUtils.addFacesMessage("Offline.exportOk", FacesMessage.SEVERITY_INFO,
						project.getTitle());
			}
		}
		else {
			MsgUtils.addFacesMessage("Offline.exportFailed", FacesMessage.SEVERITY_ERROR,
					project.getTitle());
		}
		log.debug("export done");
	}*/

	/** Load a file.
	 * In the offline version, it replaces the existing project completely.
	 * In the online version, some elements are merged.
	 */
	/*private Project doLoad(Project project, String filename) {
		log.debug("");
		boolean ret = Import.load(project, filename, loadNewScripts, loadBreakdown,
				loadCalendar, loadElements, loadStripboard);
		refresh();
		if (ret) {
			MsgUtils.addFacesMessage("Offline.importOk", FacesMessage.SEVERITY_INFO,
					project.getTitle());
		}
		else {
			MsgUtils.addFacesMessage("Offline.importFailed", FacesMessage.SEVERITY_ERROR,
					project.getTitle());
		}
		return project;
	}*/

	/**
	 * The fileEntryListener method for the ace:fileEntry control. This method is called for both
	 * successful and unsuccessful file loads.
	 *
	 * @param evt The FileEntryEvent created by the framework, which contains the FileEntry component.
	 */
	public void listenUpload(FileEntryEvent evt) {
		log.debug("");
		FileEntry inputFile = (FileEntry)evt.getSource();
	    FileEntryResults results = inputFile.getResults();
		FileInfo fileInfo = results.getFiles().get(0);
		log.debug("file=" + fileInfo.getFileName() + ", status=" + fileInfo.getStatus());
		String messageId = null;
		if (! fileInfo.isSaved()) {
			// upload failed, generate custom messages
			messageId = FileLoadBean.findErrorId(fileInfo);
			MsgUtils.addFacesMessage( messageId, FacesMessage.SEVERITY_ERROR, importFile.getName());
			//MsgUtils.addFacesMessage( "Offline.UploadFailed", FacesMessage.SEVERITY_ERROR, importFile.getName());
		}
		else {
			importFile = fileInfo.getFile();
//			importFileName = importFile.getName();
			importFileName = importFile.getAbsolutePath().trim();
			fileInfo.updateStatus(new UploadSuccessStatus("Offline.FileUploaded"), false);
//			MsgUtils.addFacesMessage( "Offline.FileUploaded", FacesMessage.SEVERITY_INFO, importFile.getName());
			actionLoad();
		}
	}

	/**
	 * Change the states/provinces list for Canada Talent
	 * @param event
	 */
	public void listenCountryChange(ValueChangeEvent event) {
		String id = event.getComponent().getId();
		String countryCode = (String)event.getNewValue();

		if(id.contentEquals("cpdAgencyAddressCountry")) {
			agencyStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL().get(countryCode);
		}
		else if(id.contentEquals("cpdProdHouseAddressCountry")) {
			prodStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL().get(countryCode);
		}
		else if(id.contentEquals("updAgencyAddressCountry")) {
			agencyStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL().get(countryCode);
		}
	}

	/**
	 * Do a database refresh of our {@link #project} field, and
	 * update the matching value in the project list (to avoid 'duplicate object' exceptions).
	 */
	private void refreshProject() {
		project = getProjectDAO().refresh(project);
		updateItemInList(project);
	}

	/**
	 * Return the id of the item that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {
		Object item;
		return ((item=getRowItem(row)) == null ? null : ((Project)item).getId());
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<Project> getComparator() {
		log.debug("col=" + getSortColumnName());
		Comparator<Project> comparator = new Comparator<Project>() {
			@Override
			public int compare(Project one, Project two) {
				return one.compareTo(two, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * Specifies, for a given sort column name, whether it defaults to
	 * ascending (true) or descending (false) order.
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (sortColumn.equalsIgnoreCase(Project.SORTKEY_ID) ||
				sortColumn.equals(Project.SORTKEY_CODE)) {
			// sort by project sequence or episode defaults to descending - most recent on top
			return false;
		}
		return true;	// all other columns default to ascending
	}


	/**
	 * RowSelector selectionListener method: invoked when user selects a row in the
	 * Project list.
	 * It is IMPERATIVE that the RowSelector tag include the 'immediate="false"'
	 * attribute for proper operation.  Otherwise, in Edit mode the input fields do not get
	 * refreshed with data from the newly selected object.
	 *
	 * @param evt The selection event, which includes the row number. We don't use this information.
	 */
	@Override
	public void rowSelected(SelectEvent evt) {
		log.debug("");
		if (styleTVProduction) { // switch from Production style to Project style
			styleTVProject = true;
			styleTVProduction = false;
		}
		// For talent production types, selecting the project from list
		// will make it the current project. LS-1853
		if(production.getType().isTalent()) {
			Project proj = (Project)evt.getObject();
			if(proj != null) {
				// Set the current project
				HeaderViewBean.getInstance().setupNewProject(proj.getId());
				project = proj;
			}
		}

		super.rowSelected(evt);
		setSelectedProdRow(-1); // suppress selection on Production row
		production.setSelected(false);
		if (editMode && project.getStatus() != AccessStatus.ACTIVE) {
			editMode = false; // can't edit a R/O or OFFLINE project
		}
	}

	/**
	 * RowSelector selectionListener method: invoked when user selects a row in the
	 * Production list, which should only have a single entry.
	 * It is IMPERATIVE that the RowSelector tag include the 'immediate="false"'
	 * attribute for proper operation.  Otherwise, in Edit mode the input fields do not get
	 * refreshed with data from the newly selected object.
	 *
	 * @param evt The selection event, which includes the row number. We don't use this information.
	 */
	public void prodRowSelected(SelectEvent evt) {
		UIData ud = (UIData)evt.getComponent();
		//Integer id = (Integer)evt.getComponent().getAttributes().get("currentId");
		//log.debug("row=" + evt.getRow() + ", id=" + id);
		setSelectedProdRow(ud.getRowIndex());
		if (! styleFilm) {
			styleTVProject = false;
			styleTVProduction = true;
			setSelectedRow(-1);	// suppress selection on project list
			if (project != null) {
				refreshProject();
				project.setSelected(false);
				forceLazyInit();
			}
		}
//		addClientResize();
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	public Project getElement() {
		return getProject();
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return projectList;
	}

	/** See {@link #stateMap}. */
	@Override
	public RowStateMap getStateMap() {
		return stateMap;
	}

	/** See {@link #stateMap}. */
	@Override
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	public boolean getIsDefaultProject() {
		return (styleTVProject && project != null &&
				project.getId().equals(production.getDefaultProject().getId()));
	}
	/** Dummy method for JSP validation. */
	public void setIsDefaultProject(boolean b) {
	}

	/** See {@link #projectSelected}. */
	public boolean getProjectSelected() {
		return projectSelected;
	}
	/** See {@link #projectSelected}. */
	public void setProjectSelected(boolean projectSelected) {
		this.projectSelected = projectSelected;
	}

	/** See {@link #locked}. */
	public boolean getLocked() {
		return locked;
	}
	/** See {@link #locked}. */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/** See {@link #styleFilm}. */
	public boolean getStyleFilm() {
		return styleFilm;
	}
	/** See {@link #styleFilm}. */
	public void setStyleFilm(boolean b) {
		styleFilm = b;
	}

	/** See {@link #styleTVProduction}. */
	public boolean getStyleTVProduction() {
		return styleTVProduction;
	}
	/** See {@link #styleTVProduction}. */
	public void setStyleTVProduction(boolean styleTVProduction) {
		this.styleTVProduction = styleTVProduction;
	}

	/** See {@link #styleTVProject}. */
	public boolean getStyleTVProject() {
		return styleTVProject;
	}
	/** See {@link #styleTVProject}. */
	public void setStyleTVProject(boolean styleTVProject) {
		this.styleTVProject = styleTVProject;
	}

	/** See {@link #selectedRow}. */
	public int getSelectedProdRow() {
		return selectedProdRow;
	}
	/** See {@link #selectedRow}. */
	public void setSelectedProdRow(int selectedRow) {
		//log.debug("row=" + selectedRow);
		selectedProdRow = selectedRow;
	}

	/** See {@link #permEdit}. */
	public boolean getPermEdit() {
		return permEdit;
	}

	/** See {@link #permViewPref}. */
	public boolean getPermViewPref() {
		return permViewPref;
	}

	/** See {@link #permEditPref}. */
	public boolean getPermEditPref() {
		return permEditPref;
	}

	/** See {@link #permManage}. */
	public boolean getPermManage() {
		return permManage;
	}

	/** See {@link #permDetails}. */
	public boolean getPermDetails() {
		return permDetails;
	}

	public Map<Integer,Integer> getCastCount() {
		return castCount;
	}
	public void setCastCount(Map<Integer,Integer> cast) {
		castCount = cast;
	}

	public Map<Integer,Integer> getCrewCount() {
		return crewCount;
	}
	public void setCrewCount(Map<Integer,Integer> crew) {
		crewCount = crew;
	}

	public List<SelectItem> getAccessStatusListDL() {
		return EnumList.getAccessStatusList();
	}

	/** See {@link #productionList}. */
	public List<Production> getProductionList() {
		productionList.set(0,production);
		if (!styleTVProject) {
			productionList.get(0).setSelected(true);
		}
		return productionList;
	}

	/** See {@link #contactDL}. */
	public List<SelectItem> getContactDL() {
		if (contactDL == null) {
			contactDL = createContactList();
		}
		return contactDL;
	}
	/** See {@link #contactDL}. */
	public void setContactDL(List<SelectItem> contactDL) {
		this.contactDL = contactDL;
	}

//	public String getShootDayMsg() {
//		return shootDayMsg;
//	}
//	public void setShootDayMsg(String shootDayMsg) {
//		this.shootDayMsg = shootDayMsg;
//	}

	public String getCurShootDate() {
		return curShootDate;
	}
	public void setCurShootDate(String curShootDate) {
		this.curShootDate = curShootDate;
	}

	/** See {@link #lastShootDate}. */
	public Date[] getLastShootDate() {
		return lastShootDate;
	}

	public boolean getShowAddProject() {
		return showAddProject;
	}
	public void setShowAddProject(boolean addProjectVisible) {
		showAddProject = addProjectVisible;
	}

	public String getApName() {
		return apName;
	}
	public void setApName(String apName) {
		log.debug("name="+apName);
		this.apName = apName;
	}

	/** See {@link #apNumber}. */
	public String getApNumber() {
		return apNumber;
	}
	/** See {@link #apNumber}. */
	public void setApNumber(String apNumber) {
		this.apNumber = apNumber;
	}

	/** See {@link #apStartDate}. */
	public Date getApStartDate() {
		return apStartDate;
	}
	/** See {@link #apStartDate}. */
	public void setApStartDate(Date apStartDate) {
		this.apStartDate = apStartDate;
	}

	/** See {@link #copyMembers}. */
	public boolean getCopyMembers() {
		return copyMembers;
	}
	/** See {@link #copyMembers}. */
	public void setCopyMembers(boolean copyMembers) {
		this.copyMembers = copyMembers;
	}

	/** See {@link #copyPreferences}. */
	public boolean getCopyPreferences() {
		return copyPreferences;
	}
	/** See {@link #copyPreferences}. */
	public void setCopyPreferences(boolean copyReports) {
		this.copyPreferences = copyReports;
	}

	/** See {@link #copyTcApprovers}. */
	public boolean getCopyTcApprovers() {
		return copyTcApprovers;
	}
	/** See {@link #copyTcApprovers}. */
	public void setCopyTcApprovers(boolean copyTcApprovers) {
		this.copyTcApprovers = copyTcApprovers;
	}

	/** See {@link #copyOnbApproversPaths}. */
	public boolean getCopyOnbApproversPaths() {
		return copyOnbApproversPaths;
	}
	/** See {@link #copyOnbApproversPaths}. */
	public void setCopyOnbApproversPaths(boolean copyOnbApproversPaths) {
		this.copyOnbApproversPaths = copyOnbApproversPaths;
	}

	/** See {@link #copyOnbPackets}. */
	public boolean getCopyOnbPackets() {
		return copyOnbPackets;
	}
	/** See {@link #copyOnbPackets}. */
	public void setCopyOnbPackets(boolean copyOnbPackets) {
		this.copyOnbPackets = copyOnbPackets;
	}

	/** See {@link #workersComp}. */
	public boolean getWorkersComp() {
		return workersComp;
	}
	/** See {@link #workersComp}. */
	public void setWorkersComp(boolean workersComp) {
		this.workersComp = workersComp;
	}

	/** See {@link #addWorkersComp}. */
	public boolean getAddWorkersComp() {
		return addWorkersComp;
	}
	/** See {@link #addWorkersComp}. */
	public void setAddWorkersComp(boolean addWorkersComp) {
		this.addWorkersComp = addWorkersComp;
	}

	/** See {@link #removeUnitId}. */
	public Integer getRemoveUnitId() {
		return removeUnitId;
	}
	/** See {@link #removeUnitId}. */
	public void setRemoveUnitId(Integer removeUnitId) {
		this.removeUnitId = removeUnitId;
	}

	/** See {@link #loadNewScripts}. */
	public boolean getLoadNewScripts() {
		return loadNewScripts;
	}
	/** See {@link #loadNewScripts}. */
	public void setLoadNewScripts(boolean loadNewScripts) {
		this.loadNewScripts = loadNewScripts;
	}

	/** See {@link #loadElements}. */
	public boolean getLoadElements() {
		return loadElements;
	}
	/** See {@link #loadElements}. */
	public void setLoadElements(boolean loadElements) {
		this.loadElements = loadElements;
	}

	/** See {@link #loadBreakdown}. */
	public boolean getLoadBreakdown() {
		return loadBreakdown;
	}
	/** See {@link #loadBreakdown}. */
	public void setLoadBreakdown(boolean loadBreakdown) {
		this.loadBreakdown = loadBreakdown;
	}

	/** See {@link #loadCalendar}. */
	public boolean getLoadCalendar() {
		return loadCalendar;
	}
	/** See {@link #loadCalendar}. */
	public void setLoadCalendar(boolean loadCalendar) {
		this.loadCalendar = loadCalendar;
	}

	/** See {@link #loadStripboard}. */
	public boolean getLoadStripboard() {
		return loadStripboard;
	}
	/** See {@link #loadStripboard}. */
	public void setLoadStripboard(boolean loadStripboard) {
		this.loadStripboard = loadStripboard;
	}

	/** See {@link #agencyStateProvinceList}. */
	public List<SelectItem> getAgencyStateProvinceList() {
		return agencyStateProvinceList;
	}

	/** See {@link #agencyStateProvinceList}. */
	public void setAgencyStateProvinceList(List<SelectItem> agencyStateProvinceList) {
		this.agencyStateProvinceList = agencyStateProvinceList;
	}

	/** See {@link #prodStateProvinceList}. */
	public List<SelectItem> getProdStateProvinceList() {
		return prodStateProvinceList;
	}

	/** See {@link #prodStateProvinceList}. */
	public void setProdStateProvinceList(List<SelectItem> prodStateProvinceList) {
		this.prodStateProvinceList = prodStateProvinceList;
	}

	private ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}

	private ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}

	/** Utility method
	 * @return FormActraIntentBean instance
	 */
	public FormActraIntentBean getFormActraIntentBean() {
		if (formActraIntentBean == null) {
			formActraIntentBean = FormActraIntentBean.getInstance();
		}
		return formActraIntentBean;
	}

	/** See {@link #projectTitle}. */
	public String getProjectTitle() {
		return projectTitle;
	}

	/** See {@link #projectTitle}. */
	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	/** See {@link #projectJobTitle}. */
	public String getProjectJobTitle() {
		return projectJobTitle;
	}

	/** See {@link #projectJobTitle}. */
	public void setProjectJobTitle(String projectJobTitle) {
		this.projectJobTitle = projectJobTitle;
	}

	/** See {@link #productionSectionLbl}. */
	public String getProductionSectionLbl() {
		return productionSectionLbl;
	}

	/** See {@link #productionSectionLbl}. */
	public void setProductionSectionLbl(String productionSectionLbl) {
		this.productionSectionLbl = productionSectionLbl;
	}

	/** See {@link #productionLbl}. */
	public String getProductionLbl() {
		return productionLbl;
	}

	/** See {@link #productionLbl}. */
	public void setProductionLbl(String productionLbl) {
		this.productionLbl = productionLbl;
	}

	@Transient
	/** See {@link #isActraForm}. */
	public Boolean getIsUdaForm() {
		return isUdaForm;
	}
	/** See {@link #isUdaForm}. */
	public void setIsUdaForm(Boolean isUdaForm) {
		this.isUdaForm = isUdaForm;
	}

	@Transient
	/** See {@link #isActraForm}. */
	public Boolean getIsActraForm() {
		return isActraForm;
	}
	/** See {@link #isActraForm}. */
	public void setIsActraForm(Boolean isActraForm) {
		this.isActraForm = isActraForm;
	}

	//LS-2194 Branch code droptown for project details and Actra contract
	/** See {@link #officeListDL}. */
	public List<SelectItem> getOfficeListDL() {
		if (officeListDL == null) {
			createOfficeDL();
		}
		return officeListDL;
	}

	/** See {@link #officeListDL}. */
	public void setOfficeListDL(List<SelectItem> officeListDL) {
		this.officeListDL = officeListDL;
	}

	/**
	 * Create a list of Offices. will use for branch code
	 * selected office. Currently only being used for Canada
	 */
	private void createOfficeDL() {
		officeListDL = new ArrayList<>();
		List<Office>officeList = getOfficeDAO().findOffices(OfficeType.ACTRA, "sortOrder");
		officeListDL.add(Constants.EMPTY_SELECT_ITEM);
		for (Office office : officeList) {
			officeListDL.add(new SelectItem(office, office.getBranchCode()));
		}
	}

	/**
	 * Set the select office to new BranchCode.
	 * @param event
	 */
	public void listenOfficeListChange(ValueChangeEvent event) {
		Office office = (Office)event.getNewValue();
		if (office != null) {
			CanadaProjectDetail cpd = project.getCanadaProjectDetail();
			cpd.setOffice(office);
		}
		else {
			office = new Office();
		}
	}

	private FormActraIntentDAO getFormActraIntentDAO() {
		if (formActraIntentDAO == null) {
			formActraIntentDAO = FormActraIntentDAO.getInstance();
		}
		return formActraIntentDAO;
	}

	private OfficeDAO getOfficeDAO() {
		if (officeDAO == null) {
			officeDAO = OfficeDAO.getInstance();
		}
		return officeDAO;
	}

}
