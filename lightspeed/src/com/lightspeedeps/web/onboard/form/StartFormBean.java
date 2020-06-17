package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlInputText;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.ContractUtils;
import com.lightspeedeps.util.payroll.StartFormUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.payroll.BatchSetupBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.util.*;
import com.lightspeedeps.web.view.ListView;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the (v3.0) Start Form page, and for the "Payroll Start" (StartForm) on
 * the onboarding Payroll / Start Forms page.  It also supports the "New Start Form" dialog
 * box used in non-Onboarding productions.
 */
@ManagedBean
@ViewScoped
public class StartFormBean extends StandardFormBean<StartForm> implements PopupHolder, Serializable, Disposable {
	/** */
	private static final long serialVersionUID = - 8424896004071532440L;

	private static final Log log = LogFactory.getLog(StartFormBean.class);

	private static final int TAB_START = 0;

	private static final int ACT_DELETE_START_FORM = 11;

	/** Id value used in SelectItem`s indicating a real value has not been selected yet. */
	private static final int SELECT_ID = -1;

	private static final int SELECT_JOB_ID = SELECT_ID;
	private static final String SELECT_JOB_TEXT = "(select occupation)";

	/** SelectItem id value for job class entry which re-fills the Job Classification list. */
	private static final int GET_FULL_JOB_LIST_ID = -2;
	private static final String GET_FULL_JOB_LIST_TEXT = "(get occupation list)";

	/** SelectItem id value for job class entry to allow user to enter a new name. */
	private static final int CUSTOM_JOB_NAME_ID = -3;
	private static final String CUSTOM_JOB_NAME_TEXT = "(Custom Job Class)";

	private static final SelectItem SELECT_HEADER = new SelectItem(SELECT_ID, "(select)");

	/* LS-2242 We should use the super class's production value (StandardFormBean), not have our own! */
//	private Production production = SessionUtils.getNonSystemProduction();

	/** For Commercial productions, the current Project; otherwise null. */
	private Project project = null;

	/** The Contact whose StartForms are currently being presented. */
	private Contact contact;

	/** The currently logged-in user. */
	User user;

	/** Drop-down to select a particular StartForm to view or edit, on the Start mini-tab/ */
	private List<SelectItem> startFormDL;

	/** The database id of the currently viewed StartForm. */
	private Integer startFormId;

	/** The currently viewed StartForm. */
//	private StartForm form;

	/** True iff the "Loan Out" section of the form should be displayed. */
	private boolean showLoanOut;

	/** True iff the Day Rate Calculator popup is being displayed. */
	private boolean showDayRate;

	/** List of StartForm "types" (New, Change, Re-Hire) for drop-down selection. */
	private static final List<SelectItem> START_FORM_TYPE_LIST = Arrays.asList(
		new SelectItem("N", "New"), 		// New
		new SelectItem("C", "Change"),		// Change
		new SelectItem("R", "Re-Hire") ); 	// Re-hire

	/** Employee type (radio button) - Hourly, Daily, Weekly*/
	private String employeeType = EMPLOYEE_TYPE_HOURLY;
	private static final String EMPLOYEE_TYPE_HOURLY = "H";
	private static final String EMPLOYEE_TYPE_DAILY = "D";
	private static final String EMPLOYEE_TYPE_WEEKLY = "W";

	/** The list of Unions to choose from. */
	private List<SelectItem> unionNameDL;

	/** The Unions object matching the unionKey of the current StartForm. */
	private Unions union;

	/** True iff current StartForm has a Union selected (and not the "non-Union"
	 * entry). */
	private boolean isUnion = false;

	/** The database id of the selected job class (occupation) entry. */
	private Integer jobClassId;

	/** The list of job classifications (occupations) to choose from. */
	private List<SelectItem> jobClassDL;

	/** The database id of the Occupation entry selected via the Occ Code drop-down. */
	private Integer occCodeId;

	/** The list of occupation codes to choose from, based on the
	 * currently selected union and job classification (occupation name). */
	private List<SelectItem> occCodeDL;

	/** The database id of the PayRate entry selected via the Schedule drop-down. */
	private Integer scheduleId;

	/** The current PayRate object, based on the current StartForm's union, occupation
	 * code, and schedule. */
	private PayRate[] payRates;

	/** The list of contract schedules codes to choose from, based on the
	 * currently selected occupation code. */
	private List<SelectItem> scheduleDL;

	/** True iff the user selected an ASA union. */
	private boolean asa;

	/** The Occupation table entry matching the currently selected occupation code. */
	private Occupation occupation;

	/** True iff the "New Start Form" dialog should be displayed. */
	private boolean showAddStartForm;

	/** The Creation date field for the New Start Form popup. */
	private Date addSdCreation;

	/** The Effective Start date field for the New Start Form popup. */
	private Date addSdEffectiveStart;

	/** The Effective End date field for the New Start Form popup. */
	private Date addSdEffectiveEnd;

	/** The form type (new/change/rehire) field for the New Start Form popup. */
	private String addSdFormType;

	/** The selected StartForm id field, from the form drop-down list,
	 *  for the New Start Form popup.  This applies to both the "Change"
	 *  and "Re-Hire" types of new Start forms.  For a "Re-hire" it may
	 *  be null, but for a "Change" it will be non-null. */
	private Integer addSdReplaceStartFormId;

	/** The selected occupation value, from the occupation drop-down list,
	 *  for the New Start Form popup. */
	private Integer addSdOccupationId;

	/** The StartForm drop-down list for the New Start Form popup. */
	private List<SelectItem> addSdFormDL;

	/** The Occupation selection drop-down list for the New Start Form popup. */
	private List<SelectItem> addSdOccupationDL;

	/** Drop-down for choosing which state wage laws to apply. */
	private List<SelectItem> wageRuleStateDL;

	/** List of Departments; only used in non-onboarding productions. */
	private List<SelectItem> departmentDL;

	/** List of Production batch items that may be selected from for this
	 * StartForm.  The value field is the ProductionBatch.id. */
	private List<SelectItem> prodBatchList;

	/** AICP Day Rate Calculator, user entry: dollar amount. */
	private String dayRateAmount;

	/** AICP Day Rate Calculator, user entry: number of hours. */
	private String dayRateHours;

	/** AICP Day Rate Calculator: checkbox for "use california laws". */
	private boolean dayRateCalifornia;

	/** AICP Day Rate Calculator: checkbox for "rates for prep only". */
	private boolean dayRatePrep;

	private String employeeAccount;

	/** backing field for Wage State rules field on Payroll Start form in NON-onboarding
	 * environment; mirrors value in the current StartForm's Employment record. LS-2220. */
	private String wageState;

	/** The database id of the currently selected ProductionBatch object. */
	private Integer batchId;

	/** Whether or not the production is using the Video Tape Agreement */
	private Boolean usesVideoTape;

	/** True iff the Production in use is an OnBoarding production. */
	private boolean isOnboarding;

	private boolean userHasViewAllProjects;

	/** List of Tours work zones */
	private List<SelectItem>toursWorkZoneDL;

	private transient StartFormDAO startFormDAO;
	/** List of Role's available in the currently selected Department. */
	protected List<SelectItem> roleDL;
	protected static final int ROLE_ID_NEW = -2; // don't use -1 as that returns as 'null' Role

	private final Disposer disposer = Disposer.getInstance();

	/** Employee has (radio button) - Standard / Prep / Touring Rates. */
	private String empRates = RATES_STD;
	private static final String RATES_STD = "S";
	private static final String RATES_PREP = "P";
	private static final String RATES_TOUR = "T";
	/** Employee department check*/
	public boolean isEmpDeptCast;

	/** Agent Commission field shall be inactive if no agent information is available*/
	boolean isAgentComm;
	/** set Employee based department name*/
	private String empDeptName = null;

	/** True iff a non-Union Start form is not allowed to have a Custom Occupation, and will
	 * instead be restricted to a supplied occupation drop-down list. */
	private boolean useNonUnionOccList;

	/** used to compare a job class as director */
	private static final String DIRECTOR = "director";

	/** Default Constructor */
	public StartFormBean() {
		super("StartForm.");
		log.debug("");
		disposer.register(this);
		setScrollable(true);
//		if (production == null) { // "My Starts" page
//			Integer id = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
//			if (id != null) {
//				production = ProductionDAO.getInstance().findById(id);
//			}
//		}
		if (getProduction() != null && getProduction().getType().hasPayrollByProject()) {
			// Commercial production, Start Forms are associated with Projects
			project = SessionUtils.getCurrentOrViewedProject(); // LS-1994
			AuthorizationBean authBean = AuthorizationBean.getInstance();
			userHasViewAllProjects = authBean.hasPageField(Constants.PGKEY_ALL_PROJECTS);
		}
//		if (getProduction() != null) {
//			isOnboarding = getProduction().getAllowOnboarding();
//		}

		Integer sfId = SessionUtils.getInteger(Constants.ATTR_START_FORM_ID);
		if (sfId != null) {
			if (sfId > 0) {
				StartForm sf = getStartFormDAO().findById(sfId);
				if (sf != null) {
					contact = sf.getContact();
				}
				else { // had an id, but no longer in db!
					sfId = null;
				}
			}
			else { // zero same as null - unknown id
				sfId = null;
			}
		}

		if (sfId == null) { // Maybe we were passed an Employment id...
			Integer empId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID);
			if (empId != null && empId > 0) {
				Employment emp = EmploymentDAO.getInstance().findById(empId);
				if (emp != null && emp.getStartForm() != null) {
					sfId = emp.getStartForm().getId();
				}
			}
		}

		Integer id = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
		if (contact == null || (id != null && ! id.equals(contact.getId()))) {
			if (id == null && getvContact() != null) {
				id = getvContact().getId();
			}
			if (id != null) {
				contact = ContactDAO.getInstance().findById(id);
			}
		}

		setup();
		startFormId = sfId;
		restoreScrollFromSession();
	}

	/** Used to return the instance of the ContactFormBean */
	public static StartFormBean getInstance() {
		return (StartFormBean)ServiceFinder.findBean("startFormBean");
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public StartForm getFormById(Integer id) {
		return StartFormDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = StartFormDAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public StartForm getBlankForm() {
		StartForm startForm = new StartForm();
		startForm.setMailingAddress(new Address());
		startForm.setLoanOutAddress(new Address());
		startForm.setPermAddress(new Address());
		startForm.setAgencyAddress(new Address());
		return startForm;
	}

	/**
	 * We probably have a StartForm object; initialize any fields that are
	 * necessary for display.
	 */
	public void setup() {
		form = null; // refresh when needed
		startFormId = null; // refresh when needed
		startFormDL = null; // refresh when needed
		if (contact != null) {
			employeeAccount = contact.getUser().getAccountNumber();
		}
	}

	/**
	 * Called when a StartForm is ready to be displayed, to set any related fields.
	 */
	public void setupStartForm() {
		if (form != null) {
			log.debug("");
			startFormId = form.getId();
			SessionUtils.put(Constants.ATTR_START_FORM_ID, startFormId);
			if (getProduction().getType().hasPayrollByProject()) { // LS-2242
				project = SessionUtils.getCurrentOrViewedProject(); // may have changed since bean was instantiated. LS-1994
			}
			isOnboarding = getProduction().getAllowOnboarding();
			if (form.getRateType() == EmployeeRateType.WEEKLY) {
				employeeType = EMPLOYEE_TYPE_WEEKLY;
			}
			else if (form.getRateType() == EmployeeRateType.DAILY) {
				employeeType = EMPLOYEE_TYPE_DAILY;
			}
			else {
				employeeType = EMPLOYEE_TYPE_HOURLY;
			}
			getEmpRates(); // initialize empRates

			Employment emp = form.getEmployment();
			if (emp != null && emp.getProductionBatch() != null) {
				batchId = emp.getProductionBatch().getId();
			}
			else {
				batchId = SELECT_ID;
			}
			if (emp != null) {
				if (SessionUtils.getNonSystemProduction() != null) {
					// (if in "my starts", don't instantiate EmploymentBean)
					// Update the Employment record used on the Payroll Start page when selecting the
					// Start Form from the dropdown.
					EmploymentBean.getInstance().setEmployment(emp);
				}
				wageState = emp.getWageState(); // LS-2220
			}

			union = null; // default to "select union" list entry
			isUnion = false;
			asa = false;
			if (form.getUnionKey() != null) {
				union = UnionsDAO.getInstance().findOneByProperty(UnionsDAO.UNION_KEY, form.getUnionKey());
				if (union != null) {
					isUnion = ! union.getUnionKey().equals(Unions.NON_UNION);
					asa = union.getOccupationUnion().startsWith("ASA");
				}
			}
			if (union == null) {
				union = new Unions();
				union.setId(SELECT_ID);
			}
			isEmpUnderCastDept();
			jobClassDL = null; // will get re-populated if needed
			occCodeDL = null; // will get re-populated if needed
			scheduleDL = null; // will get re-populated if needed

			calcUseOccListFlag(); // determine if restricting non-union occupations

			forceLazyInit();

			if (form.getLoanOutAddress() == null) {
				form.setLoanOutAddress(new Address());
			}
			//LS-3635
			if (form.getLoanOutMailingAddress() == null) {
				form.setLoanOutMailingAddress(new Address());
			}
			if (form.getAgencyAddress() == null) {
				form.setAgencyAddress(new Address());
			}
			if (form.getMailingAddress() == null) {
				form.setMailingAddress(new Address());
			}
			if (form.getPermAddress() == null) {
				form.setPermAddress(new Address());
			}
		}
		else {
			startFormId = null;
			log.debug("");
		}
		if (contact == null) {
			contact = getvContact();
		}
		SessionUtils.put(Constants.ATTR_CONTACT_ID, contact.getId());
		SessionUtils.put(Constants.ATTR_START_FORM_ID, startFormId);
		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, contact.getUser().getId()); // for Timecard pages
	}

	/**
	 * LS-2139 check for a non union client adds an employee under
	 * department "CAST"
	 */
	private void isEmpUnderCastDept() {
		setIsEmpDeptCast(false);
		setIsAgentComm(false);
		if (Unions.NON_UNION.equals(union.getUnionKey())) {
			if (empDeptName == null) {
				if (form.getEmployment().getDepartment() != null) {
					empDeptName = form.getEmployment().getDepartment().getName();
				}
			}
			if (empDeptName != null && empDeptName.equals("Cast")) {
				setIsEmpDeptCast(true);
			}
			if (form.getAgentRep()) {
				setIsAgentComm(true);
			}
		}
	}

	/**
	 * reference fields that might be needed in rendering in subsequent page
	 * updates.
	 */
	private void forceLazyInit() {
		if (form.getContact() != null) {
			form.getContact().getUser(); // force Hibernate initialization
		}
		form.getProd().getDaily();
		if (form.getPrep() != null) {
			form.getPrep().getDaily().getDtl(); // force reference
		}
		if (form.getLoanOutAddress() != null) {
			form.getLoanOutAddress().getAddrLine1();
		}
		// LS-3592
		if (form.getLoanOutMailingAddress() != null) {
			form.getLoanOutMailingAddress().getAddrLine1();
		}
		if(form.getAgencyAddress() != null) {
			form.getAgencyAddress().getAddrLine1();
		}
		if(form.getMailingAddress() != null) {
			form.getMailingAddress().getAddrLine1();
		}
		if(form.getPermAddress() != null) {
			form.getPermAddress().getAddrLine1();
		}
		if (getProduction() != null) {
			// prevent LIE when selecting a union Job Class. LS-2860
			getProduction().getContractCodes();
		}
	}

	/**
	 * Action method for the "New" legend button on the StartForm mini-tab.
	 * Set some initial values and open the "New Start Form" dialog box.
	 * @return null navigation string
	 */
	public String actionNewStartForm() {
		try {
			addSdOccupationDL = null; // force refresh of occupation list
			if (getAddSdOccupationDL().size() == 0) {
				if (project == null) { // TV or Feature production
					MsgUtils.addFacesMessage("StartForm.NoCrewPosition", FacesMessage.SEVERITY_ERROR);
				}
				else { // Commercial production
					MsgUtils.addFacesMessage("StartForm.NoCrewPositionInProject", FacesMessage.SEVERITY_ERROR);
				}
				return null;
			}
			addSdFormType = "N";
			Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			CalendarUtils.setStartOfDay(cal);
			Date date = cal.getTime();
			addSdCreation = date;
			addSdEffectiveStart = date;
			// Set "prior SD effective end date" to yesterday:
			cal.add(Calendar.DAY_OF_MONTH, -1); // backup one day
			addSdEffectiveEnd = cal.getTime();
			addSdFormDL = null;
			addSdReplaceStartFormId = null;
			showAddStartForm = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the Cancel button on the "New Start Form" dialog box.
	 * @return null navigation string
	 */
	public String actionCloseNewStartForm() {
		showAddStartForm = false;
		//ListView.clearEditFields(addSdGroup);
		return null;
	}

	/**
	 * Action method for the OK button on the "New Start Form" dialog box.
	 * Close the dialog box, create the new form, and display it.
	 * @return null navigation string
	 */
	public String actionNewStartFormOk() {
		if (addSdFormType.equals(StartForm.FORM_TYPE_CHANGE) ) {
			if (addSdReplaceStartFormId == null || addSdReplaceStartFormId < 0) {
				MsgUtils.addFacesMessage("StartForm.ChoosePriorDoc", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		showAddStartForm = false;
		try {
			createStartForm();
			startFormId = form.getId();
			initStartFormDisplay();
			if (! showLoanOut) {
				if ((! StringUtils.isEmpty(form.getLoanOutCorpName())) ||
						(! StringUtils.isEmpty(form.getFederalTaxId())) ||
						(form.getLoanOutAddress() != null && ! form.getLoanOutAddress().isEmpty())) {
					showLoanOut = true;
				}
			}
			actionEdit(); // leave user in Edit mode on new StartForm
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the "Edit" legend button on the StartForm mini-tab.
	 * Sets up data that's only needed in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		try {
			log.debug("");
			if (startFormId != null && startFormId >= 0) {
				if (project != null) {
					project = ProjectDAO.getInstance().refresh(project);
				}
				form = getStartFormDAO().refresh(form);
				if (! lockAndPrompt("")) {
					return null;
				}
				editMode = true;
				calculateEditFlags(true); // approver can edit Submittable doc
				if (form.getLoanOutAddress() == null) {
					form.setLoanOutAddress(new Address());
				}
				form.getLoanOutAddress().getAddrLine1();
				// LS-3592
				if (form.getLoanOutMailingAddress() == null) {
					form.setLoanOutMailingAddress(new Address());
				}
				form.getLoanOutMailingAddress().getAddrLine1();
				
				if (form.getAgencyAddress() == null) {
					form.setAgencyAddress(new Address());
				}
				if (form.getMailingAddress() == null) {
					form.setMailingAddress(new Address());
				}
				if (form.getPermAddress() == null) {
					form.setPermAddress(new Address());
				}
				form.getAgencyAddress().getAddrLine1();
				// set the jobClassId so the proper entry in the drop-down will be selected
				if (isUnion && union != null && form.getJobClass() != null) {
					jobClassId = SELECT_JOB_ID; // "select occupation" entry
					for (SelectItem si : getJobClassDL()) {
						if (form.getJobClass().equals(si.getLabel())) {
							jobClassId = (Integer)si.getValue();
							break;
						}
					}
					if (jobClassId == SELECT_JOB_ID) { // not in list, must be "custom" job name.
						jobClassId = CUSTOM_JOB_NAME_ID;
						if (form.getLsOccCode() == null) {
							occupation = null;
						}
						else {
							occupation = OccupationDAO.getInstance().findByUnionAndOccCode(
									getProduction(), project, union.getOccupationUnion(), form.getLsOccCode());
						}
					}
					else {
						occupation = OccupationDAO.getInstance().findByUnionJobAndOccCode(
								getProduction(), project, union.getOccupationUnion(),
								form.getJobClass(), form.getLsOccCode());
					}
					if (occupation != null) {
						if (form.getContractSchedule() != null) {
							occCodeId = occupation.getId();
							payRates = PayRateDAO.getInstance().findByContractOccCodeAndSchedule(
									form.getWorkStartOrHireDate(),
									occupation.getContractRateKey(),
									occupation.getLsOccCode(), form.getContractSchedule());
							if (payRates != null) {
								if (payRates[0] != null) {
									scheduleId = payRates[0].getId();
								}
								else {
									scheduleId = payRates[1].getId();
								}
							}
						}
					}
				}
				else if (useNonUnionOccList && form.getJobClass() != null) {
					jobClassId = SELECT_JOB_ID; // "select occupation" entry
					for (SelectItem si : getJobClassDL()) {
						if (form.getJobClass().equals(si.getLabel())) {
							jobClassId = (Integer)si.getValue();
							break;
						}
					}
				}
				forceLazyInit();
			}
			super.actionEdit(); // retain scroll position
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Cancel" legend button on the StartForm mini-tab.
	 * Exit edit mode and refresh the current StartForm.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		super.actionCancel(); // exit edit mode & retain scroll position
		if (startFormId != null) {
			form = getStartFormDAO().findById(startFormId);
			getStartFormDAO().unlock(form, SessionUtils.getCurrentUser().getId());
		}
		else {
			form = null;
		}
		setupStartForm();
		return null;
	}

	/**
	 * Action method for the "Save" button on the non-Onboarding StartForm
	 * mini-tab, or called from ContactFormBean for save in Onboarding
	 * production.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		try {
			if (form != null) {
				StartFormUtils.trimFields(form);
				if (project != null) { // Commercial production
					copyStudioRatesToDistant();
				}
				if (StartFormUtils.validateStartForm(form, isUnion)) {
					// LS-3915 Set the version number so we know between perp and non-prep versions
					if(!form.getHasPrepRates()) {
						form.setVersion(StartForm.PAROLL_START_VERSION_NON_PREP);
					}
					else {
						form.setVersion(StartForm.PAROLL_START_VERSION_PREP);
					}

					saveAddresses();
					if (form.getOccupationCode() != null && form.getOccupationCode().equals("-1")) { // none selected
						form.setOccupationCode(null);
						form.setLsOccCode(null);
					}
					if (! isUnion && ! useNonUnionOccList && ! StringUtils.isEmpty(form.getJobClass())) {
						// LS-3161: copy LS Occ Code for non-union roles
						Role role = RoleDAO.getInstance().findByName(form.getJobClass());
						if (role != null) { // Found match in non-union role table
							form.setLsOccCode(role.getLsOccCode());
						}
						else { // Role not found, clear occ code (in case it was set previously)
							form.setLsOccCode(null);
						}
					}
					// LS-2159, fetch default work country from payroll preference
					if (form.getWorkCountry() == null || form.getWorkCountry().trim().isEmpty()) {
						PayrollPreference pref = StartFormService.getSFPayrollPreference(form);
						if (pref != null) {
							form.setWorkCountry(pref.getWorkCountry());
							form.setWorkState(pref.getWorkState());
						}
					}
					StartRateSet oldPrepRate = null;
					if ((! form.getHasPrepRates()) && form.getPrep() != null) {
						oldPrepRate = form.getPrep();
						form.setPrep(null);
					}
					// LS-1745, changes for the last name
					if (form.getLastName() != null) {
						String name = DocumentService.checkSuffix(form.getLastName());
						form.setLastName(name);
					}
					boolean areAcctCodesEntered = false;
					AccountCodes ac = form.getAccount();
					ProductionType prodType = getProduction().getType();
					// Check to see if there is any account code data for this form.
					if (prodType.isAicp()) {
						areAcctCodesEntered = ((ac.getMajor() != null && !ac.getMajor().isEmpty())
								|| (ac.getDtl() != null && ! ac.getDtl().isEmpty()));
					}
					else {
						areAcctCodesEntered = ((ac.getDtl() != null && !ac.getDtl().isEmpty())
								|| (ac.getFree() != null && !ac.getFree().isEmpty())
								|| (ac.getFree2() != null && !ac.getFree2().isEmpty())
								|| (ac.getMajor() != null && !ac.getMajor().isEmpty())
								|| (ac.getSet() != null && !ac.getSet().isEmpty())
								|| (ac.getSub() != null && !ac.getSub().isEmpty())
								|| (ac.getAloc() != null && !ac.getAloc().isEmpty())
							);
					}

					if (! isOnboarding || areAcctCodesEntered) {
						// Copy 'additional staff' and account codes to Employment record
						// Need refresh to avoid LIE SD-1502
						Employment modifiedEmp = EmploymentDAO.getInstance().refresh(form.getEmployment());

						modifiedEmp.setAdditionalStaff(form.getAdditionalStaff());
						modifiedEmp.getAccount().copyFrom(form.getAccount()); // copy all account codes
						modifiedEmp.setWageState(wageState); // persist (possibly changed) value. LS-2220
						getStartFormDAO().merge(modifiedEmp); // attachDirty got DuplicateKeyException on model.Role
						form.setEmployment(modifiedEmp);	// so UI reflects updates
					}
					// LS-1682: removed code that clears particular touring rates

					form.setLockedBy(null); // unlock it
					getStartFormDAO().attachDirty(form); // don't merge: that breaks prep/std change; LS-1457
					Date date = form.getWorkStartOrHireDate();
					date = TimecardUtils.calculateWeekEndDate(date);
					// Fixed NPE
					//Production prod = SessionUtils.getNonSystemProduction();
					log.debug("production:  "+ getProduction());
					setProduction(ProductionDAO.getInstance().refresh(getProduction()));
					if (getProduction() != null && getProduction().getPayrollPref().getFirstPayrollDate().after(date)) {
						// The "first payroll date" is later than this start-doc's start-date, so update the production.
						getProduction().getPayrollPref().setFirstPayrollDate(date);
						startFormDAO.attachDirty(getProduction().getPayrollPref());
					}
					if (startFormId == null) {
						startFormId = form.getId();
					}
					if (oldPrepRate != null && oldPrepRate.getId() != null) {
						startFormDAO.delete(oldPrepRate);
					}
					
					initStartFormDisplay();
					super.actionSave(); // retain scroll position
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		View.addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "Delete" legend button on the StartForm mini-tab.
	 * This just puts up a confirmation dialog.
	 *
	 * @return null navigation string
	 */
	public String actionDelete() {
		if (startFormId != null && startFormId >= 0) {
			if (WeeklyTimecardDAO.getInstance().existsTimecardsForStartForm(startFormId)) {
				// Don't allow delete if the StartForm has associated timecards
				MsgUtils.addFacesMessage("StartForm.HasTimecards", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			form = getStartFormDAO().refresh(form);
			if (! lockAndPrompt("Delete.")) {
				return null;
			}
			PopupBean.getInstance().show(
					this, ACT_DELETE_START_FORM,
					"StartForm.Delete.");
		}
		return null;
	}

	/**
	 * Action method for the "Print" button on the Start Forms mini-tab. In
	 * Non-On-boarding productions, this is called directly from the button (via
	 * framework). In On-boarding productions, it is called from
	 * ContactFormBean.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionPrint() {
		try {
			StartForm startForm = null;
			String docStatus = ApprovalStatus.OPEN.name();
			if (contactDoc == null) { // Non-On-Boarding
				if (startFormId != null) {
					startForm = StartFormDAO.getInstance().findById(startFormId);
				}
			}
			else {
				docStatus = contactDoc.getStatus().name();
				if (contactDoc.getEmployment() != null) {
					Integer id = contactDoc.getRelatedFormId();
					if (id != null) {
						startForm = StartFormDAO.getInstance().findById(id);
					}
					else {
						startForm = StartFormDAO.getInstance().findOneByProperty("employment", contactDoc.getEmployment());
					}
				}
			}
			if (startForm != null) {
				boolean useXfdf = false;
				
				if(FF4JUtils.useFeature(FeatureFlagType.TTCO_PAYROLL_START_PDF_PRINTING)) {
					useXfdf  = startForm.getUseXfdf();
				}
				if(useXfdf) {
					// LS-3824 Print using pdf and xfdf instead of jasper reports
					DateFormat df = new SimpleDateFormat("mmssSSS");
					String timestamp = df.format(new Date());
					String fileNamePdf = contactDoc.getFormType().getReportPrefix() + SessionUtils.getCurrentOrViewedProject().getId() + "_" 
							+ contactDoc.getContact().getUser().getId() + "_" + timestamp + ".pdf";
					fileNamePdf = DocumentService.printDocument(contactDoc.getFormType(), contactDoc, startForm, fileNamePdf, true);
					String fileWithAttachmentsIfExist = fileNamePdf;
					if (fileNamePdf != null) {
						fileWithAttachmentsIfExist = DocumentService.mergeAttachments(null, contactDoc, fileNamePdf, user);
						openDocumentInNewTab(fileWithAttachmentsIfExist);
					}
				}
				else {
					ReportBean report = ReportBean.getInstance();
					report.setOpenReportPopupWindow(true);
					startForm = StartFormDAO.getInstance().refresh(startForm);
					report.generateStartForm(startForm, null, docStatus);
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
	 * Action method for the "Hide" button on the "Loan-Out" section of
	 * the Start Form page.
	 * @return null navigation String
	 */
	public String actionHideLoanOut() {
		showLoanOut = false;
		View.addButtonClicked();
		return null;
	}

	/**
	 * Action method for the "Show" button on the "Loan-Out" section of
	 * the Start Form page.
	 * @return null navigation String
	 */
	public String actionShowLoanOut() {
		showLoanOut = true;
		View.addButtonClicked();
		return null;
	}

	/**
	 * Action method to display Day Rate Calculator pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionDayRateOpen() {
		showDayRate = true;
		return null;
	}

	/**
	 * Action method called when Cancel is clicked in Day Rate Calculator
	 * pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionDayRateCancel() {
		showDayRate = false;
		return null;
	}

	/**
	 * Action method called when Calculate (OK) is clicked in Day Rate
	 * Calculator pop-up.
	 *
	 * @return null navigation string
	 */
	public String actionDayRateCalculate() {
		boolean valid = true;
		BigDecimal amount = null;
		BigDecimal hours = null;
		do { // so we can use "continue"
			if (dayRateAmount == null || dayRateAmount.trim().length() == 0) {
				MsgUtils.addFacesMessage("StartForm.DayRate.AmountInvalid", FacesMessage.SEVERITY_ERROR, "1");
				valid = false;
				continue;
			}
			if (dayRateHours == null || dayRateHours.trim().length() == 0) {
				MsgUtils.addFacesMessage("StartForm.DayRate.HoursInvalid", FacesMessage.SEVERITY_ERROR, "2");
				valid = false;
				continue;
			}

			try {
				dayRateAmount = dayRateAmount.trim();
				if (dayRateAmount.charAt(0) == '$') {
					dayRateAmount = dayRateAmount.substring(1);
					dayRateAmount = dayRateAmount.trim();
					if (dayRateAmount.length() == 0) {
						MsgUtils.addFacesMessage("StartForm.DayRate.AmountInvalid", FacesMessage.SEVERITY_ERROR, "3");
						valid = false;
						continue;
					}
				}
				amount = new BigDecimal(dayRateAmount);
			}
			catch (NumberFormatException e) {
				MsgUtils.addFacesMessage("StartForm.DayRate.AmountInvalid", FacesMessage.SEVERITY_ERROR, "4");
				valid = false;
				continue;
			}

			try {
				hours = new BigDecimal(dayRateHours);
			}
			catch (NumberFormatException e) {
				MsgUtils.addFacesMessage("StartForm.DayRate.HoursInvalid", FacesMessage.SEVERITY_ERROR, "5");
				valid = false;
				continue;
			}

			if (amount == null || amount.signum() <= 0) {
				MsgUtils.addFacesMessage("StartForm.DayRate.AmountInvalid", FacesMessage.SEVERITY_ERROR, "6");
				valid = false;
				continue;
			}
			if (hours == null || hours.compareTo(StartFormService.REGULAR_RATE_HOURS) < 0 ||
					hours.compareTo(StartFormService.MAX_DAY_RATE_HOURS) > 0) {
				MsgUtils.addFacesMessage("StartForm.DayRate.HoursInvalid", FacesMessage.SEVERITY_ERROR, "7");
				valid = false;
				continue;
			}
		} while(false);

		if (valid) {
			if (dayRatePrep && (form.getPrep() == null)) {
				/*form.setHasPrepRates(true);*/
				form.setStartRates(StartRatesType.RATES_PREP);
				form.setPrep(new StartRateSet());
				StartFormUtils.fillRateAccounts(form, form.getPrep());
			}
			showDayRate = false;
			StartFormService.calculateDayRate(form, amount, hours, dayRatePrep, dayRateCalifornia);
		}

		return null;
	}

	/**
	 * Action method when the user clicks Cancel on the Delete prompt. We need
	 * to unlock the start form, since we lock it when the user first clicks the
	 * Delete button.
	 *
	 * @return null navigation string
	 */
	private String actionDeleteCancel() {
		return actionCancel(); // this will refresh & unlock
	}

	/**
	 * Delete the current StartForm. Control comes here via the confirmOk()
	 * method, after the user clicks OK on the confirmation dialog.
	 *
	 * @return null navigation string
	 */
	private String actionDeleteOk() {
		try {
			if (form != null) {
				// Get rid of "dummy" address objects, if any.
				if (form.getAgencyAddress() != null && form.getAgencyAddress().getId() == null) {
					form.setAgencyAddress(null);
				}
				if (form.getMailingAddress() != null && form.getMailingAddress().getId() == null) {
					form.setMailingAddress(null);
				}
				if (form.getPermAddress() != null && form.getPermAddress().getId() == null) {
					form.setPermAddress(null);
				}
				if (form.getLoanOutAddress() != null && form.getLoanOutAddress().getId() == null) {
					form.setLoanOutAddress(null);
				}
				// LS-3592
				if (form.getLoanOutMailingAddress() != null && form.getLoanOutMailingAddress().getId() == null) {
					form.setLoanOutMailingAddress(null);
				}
				ContactDAO.getInstance().delete(form);
			}
			form = null;
			setupStartTab(TAB_START);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return HeaderViewBean.PEOPLE_MENU_STARTS;
	}

	/**
	 * ValueChangeListener for the StartForm drop-down list, which allows the
	 * user to select which StartForm values are being viewed or edited.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenStartForm(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		if (id != null) {
			form = getStartFormDAO().findById(id);
//			if (getProduction() != null && getProduction().getType().hasPayrollByProject()) {
				//prodBatchList = null; // force refresh, in case project changed
//			}
			setupStartForm();
		}
	}

	/**
	 * ValueChangeListener for Employee drop-down list
	 * @param event contains old and new values
	 */
	public void listenEmployeeChange(ValueChangeEvent event) {
		try {
			String acct = (String)event.getNewValue();
			if (acct != null) {
				employeeAccount = acct;
				contact = ContactDAO.getInstance().findByAccountNumAndProduction(acct, getProduction());
				SessionUtils.put(Constants.ATTR_CONTACT_ID, contact.getId());
				SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force timecard pages to use attr_contact_id
				setup();
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for the Employee type radio buttons which
	 * select between weekly, daily, and hourly employee.  For weekly employees,
	 * we need to display an extra line in the pay table.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenEmployeeType(ValueChangeEvent evt) {
		String type = (String)evt.getNewValue();
		if (type != null && form != null) {
			if (type.equals(EMPLOYEE_TYPE_HOURLY)) {
				form.setRateType(EmployeeRateType.HOURLY);
				form.setAllowWorked(false);
				form.setPay6th7thDay(false);
			}
			else if (type.equals(EMPLOYEE_TYPE_DAILY)) {
				form.setRateType(EmployeeRateType.DAILY);
				form.setAllowWorked(true);
				form.setPay6th7thDay(false);
				populateRateGuarHours();
				if (! isUnion) {
					form.getProd().getDaily().setStudioHrs(null); // clear guaranteed hours. LS-2567
					form.getProd().getDaily().setLocHrs(null);
					if (form.getPrep() != null) {
						form.getPrep().getDaily().setStudioHrs(null);
						form.getPrep().getDaily().setLocHrs(null);
					}
				}
			}
			else {
				form.setRateType(EmployeeRateType.WEEKLY);
				form.setAllowWorked(true);
				populateRateGuarHours();
				if (! isUnion) {
					form.getProd().getWeekly().setStudioHrs(null); // clear guaranteed hours. LS-2567
					form.getProd().getWeekly().setLocHrs(null);
					if (form.getPrep() != null) {
						form.getPrep().getWeekly().setStudioHrs(null);
						form.getPrep().getWeekly().setLocHrs(null);
					}
				}
			}
		}
	}

	/**
	 * method to auto populate Rate/Guarantee hours for DGA Union having job
	 * class other than 'director'.  Note that "guaranteed hours" is usually hours
	 * per day, but code should handle case where it is hours/week for a weekly
	 * person (e.g., 60).
	 */
	public void populateRateGuarHours() {
		if (((union != null && union.getNumber().equals(Unions.DGA))
				&& (form.getJobClass() != null && ! form.getJobClass().equalsIgnoreCase(DIRECTOR)))) {
			BigDecimal studioAmt = null;
			BigDecimal guarHrs = form.getGuarHours();
			if (form.getRateType().equals(EmployeeRateType.DAILY)) {
				studioAmt = form.getProd().getDaily().getStudio();
			}
			else if (form.getRateType().equals(EmployeeRateType.WEEKLY)) {
				studioAmt = form.getProd().getWeekly().getStudio();
				if (studioAmt != null) { // get daily pay
					studioAmt = studioAmt.divide(Constants.DECIMAL_FIVE, 2, RoundingMode.HALF_UP);
				}
				if (guarHrs != null && guarHrs.compareTo(Constants.HOURS_IN_A_DAY) >= 0) {
					// unusual: guar hours is per week, not per day
					guarHrs = guarHrs.divide(Constants.DECIMAL_FIVE, 2, RoundingMode.HALF_UP);
				}
			}
			if (guarHrs == null) { // not normal; assume straight time
				guarHrs = Constants.DAILY_STRAIGHT_HOURS;
			}
			if (studioAmt != null && guarHrs != null) {
				if (guarHrs.compareTo(Constants.DAILY_STRAIGHT_HOURS) > 0) {
					// We have to convert guaranteed hours into "pay hours" considering part is OT
					BigDecimal ot = guarHrs.subtract(Constants.DAILY_STRAIGHT_HOURS);
					ot = ot.multiply(Constants.DECIMAL_ONE_FIVE); // OT hours paid at 1.5x
					guarHrs = Constants.DAILY_STRAIGHT_HOURS.add(ot);
				}
				form.getProd().getHourly()
						.setStudio(studioAmt.divide(guarHrs, 4, RoundingMode.HALF_UP));
			}
		}
		else if (((union != null && union.getNumber().equals(Unions.DGA)) &&
				(form.getJobClass() != null && form.getJobClass().equalsIgnoreCase(DIRECTOR)))) {
			if (form.getGuarHours() == null) {
				form.setGuarHours(form.getProd(), Constants.WORKED_HOURS_UNION);
			}
		}
	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * @param event contains old and new values
	 *//*
	public void listenBatchChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Integer id = (Integer)event.getNewValue();
			if (id != null) {
				batchId = id;
				ProductionBatch batch = ProductionBatchDAO.getInstance().findById(batchId);
				startForm.getEmployment().setProductionBatch(batch);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}*/

	/**
	 * The ValueChangeListener for the major/detail/set account fields.
	 * If the user sets one of the "main" account number parts (Major, Detail, or Set), then
	 * set the corresponding values in all the detail item account fields.
	 *
	 * @param evt The change event created by the framework.
	 */
	public void listenAccount(ValueChangeEvent evt) {
		if (! evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();
			return;
		}
		if (form == null) {
			// Unusual case - user probable switched tabs or logged off while in Edit mode. LS-1624
			return;
		}
		if (getProduction().getType().isAicp()) {
			if (form.getAccountMajor() != null && StringUtils.isEmpty(form.getAccountDtl())) {
				form.setAccountMajor(form.getAccountMajor().trim());
				try {
					int acctNum = Integer.valueOf(form.getAccountMajor());
					if (acctNum > 0 && acctNum < 950) {
						acctNum += 50;
						form.setAccountDtl(""+acctNum);
					}
				}
				catch (NumberFormatException e) {
				}
			}
		}
		else {
			// Propagate the header account codes into any blank account fields in
			// the Rates/Accounts table.
			forceLazyInit();
			StartFormUtils.fillRateAccounts(form, form.getProd());	// production rate rows
			if (form.getPrep() != null) {		// prep rate rows (if they exist)
				StartFormUtils.fillRateAccounts(form, form.getPrep());
			}
			StartFormUtils.fillAllowanceAccounts(form);	// Allowance rows
		}
	}

	/**
	 * ValueChangeListener for SSN. This is used just to help keep the
	 * focus in the correct field when a user tabs out of the SSN after
	 * changing it.  A loss of focus seemed to occur when completing the
	 * SSN changed the form status from "Incomplete" to "complete".
	 *
	 * @param evt The event object supplied by the framework.
	 */
	public void listenSSN(ValueChangeEvent evt) {
		View.addFocus("offProd");
	}

	/**
	 * ValueChangeListener for Rates in the Overtime edit area.
	 * <p>
	 * Note that an ice:setEventPhase component is used to delay this
	 * ValueChangeEvent so that it arrives during the InvokeApplication phase.
	 *
	 * @param evt The event object supplied by the framework.
	 */
	public void listenRateOvertime(ValueChangeEvent evt) {
		try {
			if (form != null) {
				StartFormUtils.calculateOvertimeMultipliers(form);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Hourly Location Rate in the Start Form edit area.
	 * <p>
	 * Note that an ice:setEventPhase component is used to delay this
	 * ValueChangeEvent so that it arrives during the InvokeApplication phase.
	 *
	 * @param evt The event object supplied by the framework.
	 */
	public void listenRateLoc(ValueChangeEvent evt) {
//		try {
//			if (startForm.getProd().getHourly().getLoc() != null) {
//				startForm.getProd().getHourly().setLoc(
//						NumberUtils.scaleHourlyRate(startForm.getProd().getHourly().getLoc()));
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
	}

	/**
	 * ValueChangeListener for Hourly Studio Rate in the Start Form edit area.
	 * <p>
	 * Note that an ice:setEventPhase component is used to delay this
	 * ValueChangeEvent so that it arrives during the InvokeApplication phase.

	 * @param evt The event object supplied by the framework.
	 */
	public void listenRateStudio(ValueChangeEvent evt) {
		if (evt.getNewValue() != null) {
			BigDecimal studioRate = new BigDecimal(evt.getNewValue().toString());
			RateHoursGroup srs =
					(RateHoursGroup)evt.getComponent().getAttributes().get("selectedRow");
			srs.setStudio(studioRate);
			populateRateGuarHours();
		}
//		try {
//			if (startForm.getProd().getHourly().getStudio() != null) {
//				startForm.getProd().getHourly().setStudio(
//						NumberUtils.scaleHourlyRate(startForm.getProd().getHourly().getStudio()));
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
	}

	/**
	 * ValueChangeListener for the Employee Rates radio buttons which
	 * select between Standard, Prep, and Touring rates.
	 * For Prep & touring rates, we need to display an extra table.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenEmployeeRates(ValueChangeEvent evt) {
		String type = (String)evt.getNewValue();
		if (type != null) {
			setEmpRates(type);
			log.debug("");
			if (type.equals(RATES_STD)) {
				form.setStartRates(StartRatesType.RATES_STD);
			}
			else if (type.equals(RATES_PREP)) {
				if (form.getPrep() == null) {
					form.setPrep(new StartRateSet());
					StartFormUtils.fillRateAccounts(form, form.getPrep());
				}
				form.setStartRates(StartRatesType.RATES_PREP);
			}
			else {
				form.setStartRates(StartRatesType.RATES_TOURS);
			}
		}
	}


	/**
	 * ValueChangeListener for the StartForm "employee has Prep Rates" check-box.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenHasPrep(ValueChangeEvent evt) {
		Boolean hasPrep = (Boolean)evt.getNewValue();
		if (hasPrep != null && form != null) {
			if (hasPrep && form.getPrep() == null) {
				form.setPrep(new StartRateSet());
				StartFormUtils.fillRateAccounts(form, form.getPrep());
			}
		}
	}

	/**
	 * ValueChangeListener for the StartForm "type of form" radio buttons, which allows the
	 * user to select whether the StartForm is New, Change, or Re-Hire.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenAddSdFormType(ValueChangeEvent evt) {
		if (! evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();
			return;
		}
		String type = (String)evt.getNewValue();
		if (type != null) {
			if (type.equals("N")) {
			}
			else if (type.equals("C")) {
				// set the default "last effective date" now
				Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
				if (addSdEffectiveStart == null) {
					addSdEffectiveStart = new Date();
				}
				cal.setTime(addSdEffectiveStart);
				cal.add(Calendar.DAY_OF_MONTH, -1);
				addSdEffectiveEnd = cal.getTime();
			}
			else { // R = re-hire
			}
		}
	}

	/**
	 * ValueChangeListener for the Union drop-down list, which allows the
	 * user to select which union the employee belongs to. When the selected
	 * union changes, we need to re-populate the list of possible job classifications,
	 * and to set the {@link #isUnion} field appropriately.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenUnion(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		if (id != null && form != null) {
			String holdJobClass = form.getJobClass();
			form.setUnionLocalNum(null);
			form.setUnionKey(null);
			form.setContractCode(null);
			form.setJobClass(null);
			form.setOccupationCode(null);
			form.setLsOccCode(null);
			jobClassDL = null;	// force refresh of job classification list
			jobClassId = SELECT_JOB_ID;
			occCodeDL = null;	// force refresh of occupation code list
			occCodeId = null;
			occupation = null;
			scheduleDL = null;	// force refresh of schedule list
			scheduleId = null;
			isUnion = false;
			useNonUnionOccList = false;
			union = null;
			if (id >= 0) {
				union = UnionsDAO.getInstance().findById(id);
				if (union != null) {
					form.setUnionKey(union.getUnionKey());
					if (union.getNumber().equals(Unions.TEAMSTERS_NUMBER)) {
						form.setUnionLocalNum("");
					}
					else {
						if (getIsTeamPayroll()) {
							form.setUnionLocalNum(union.getGsLocalNum());
						}
						else {
							form.setUnionLocalNum(union.getNumber());
						}
					}
					asa = union.getOccupationUnion().startsWith("ASA");
					if (union.getOccupationUnion().equals(Unions.NON_UNION)) {
						form.setJobClass(holdJobClass);
						form.setScheduleCode(null);
						form.setContractSchedule(null);
						calcUseOccListFlag(); // determine if using occupation list
					}
					else {
						isUnion = true;
					}
					getJobClassDL(); // ensure this re-initializes before Occ code list.
				}
			}
			if (union == null) {
				union = new Unions();
				union.setId(SELECT_ID);
			}
			View.addFocus("union"); // help Icefaces keep the focus in the right place
			if (isUnion && form.getHasTourRates()) { // cannot use Touring rates if Union
				form.setStartRates(StartRatesType.RATES_STD);
			}
		}
	}

	/**
	 * ValueChangeListener for tax classification radio bttn change in
	 * loan-out section. LS-2562
	 *
	 * @param event
	 */
	public void listenTaxClassChange(ValueChangeEvent event) {
		TaxClassificationType ctt = (TaxClassificationType)event.getNewValue();

		if(ctt != null) {
			HtmlInputText llcType = (HtmlInputText)event.getComponent().findComponent("llcType");
			if(llcType != null) {
				if(ctt.isLLC()) {
					llcType.setDisabled(false);
				}
				else {
					llcType.setDisabled(true);
					llcType.setValue(null);
				}
			}
		}
	}

	/**
	 * ValueChangeListener for the Job Classification drop-down list, which allows the
	 * user to select the employee's occupation.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenJobClass(ValueChangeEvent evt) {
		//log.debug("new=" + evt.getNewValue() + ", old=" + evt.getOldValue());
		Integer id = (Integer)evt.getNewValue();
		if (id != null && form != null) {
			form.setJobClass(null);
			if (id == CUSTOM_JOB_NAME_ID) {
				return;
			}
			form.setOccupationCode(null);
			form.setLsOccCode(null);
			form.setScheduleCode(null);
			form.setContractSchedule(null);
			scheduleDL = null;
			scheduleId = null;
			if (id >= 0) {
				OccupationDAO occupationDAO = OccupationDAO.getInstance();
				jobClassId = id;
				Occupation tempOccupation = occupationDAO.findById(id);
				if (tempOccupation != null) {
					form.setJobClass(tempOccupation.getName());
					boolean match = false;
					if (occCodeId != null && occupation != null && occCodeId > 0) {
						// user changed the occupation after selecting the occ code; see if the combination matches.
						Occupation occ = occupationDAO.findByUnionJobAndOccCode(getProduction(), project, union.getOccupationUnion(),
								form.getJobClass(), occupation.getLsOccCode());
						if (occ != null) { // ok, the new job class matches the previously-selected occ code.
							occupation = occ;
							log.debug(occCodeId);
							updateOccCodeDL(occupation); // make sure right occupation id is in occCodeDL
							changeOccCode();
							match = true;
						}
					}
					if (! match) {
						// no match of job class with current occ code; get new occ code list.
						occCodeId = null;
						occupation = null;
						occCodeDL = occupationDAO.createOccCodeDL(getProduction(), project, union.getOccupationUnion(), tempOccupation.getName());
						if (occCodeDL.size() == 1) {
							// note that if multiple occ-list entries have the same name, the one selected from
							// the drop-down list may be different than this occCode entry just located.
							id = (Integer)occCodeDL.get(0).getValue(); // get this one's id
							tempOccupation = occupationDAO.findById(id); // and the matching Occupation entry
							occupation = tempOccupation;
							changeOccCode(); // set startForm fields; maybe rates, too.
							occCodeDL = null;
						}
						else if (occCodeDL.size() > 1) {
							occCodeDL.add(0, SELECT_HEADER);
							occCodeId = SELECT_ID;
						}
					}
				}
			}
			else { // not a job name, but one of our special choices
				occCodeDL = null;	// force refresh of occupation code list
				occCodeId = null;
				occupation = null;
				if (id == GET_FULL_JOB_LIST_ID) {
					jobClassDL = null;	// force refresh of job classification list
					jobClassId = SELECT_JOB_ID;
				}
			}
			View.addFocus("jobClass"); // help Icefaces keep the focus in the right place
		}
	}

	/**
	 * ValueChangeListener for the StartForm Agent Checkbox, which allows the
	 * Agent Commission StartForm value are being viewed or edited.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenAgentRep(ValueChangeEvent evt) {
		boolean agentRep = (boolean)evt.getNewValue();
		if (agentRep) {
			setIsAgentComm(true);
		}
		else {
			setIsAgentComm(false);
			// LS-2406 We need to clear out the Agent Commission amount.
			form.setEmpAgentCommisssion(null);
		}
	}

	/**
	 * ValueChangeListener for the Job Classification drop-down list, which allows the
	 * user to select the employee's occupation.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenNonUnionJobClass(ValueChangeEvent evt) {
		//log.debug("new=" + evt.getNewValue() + ", old=" + evt.getOldValue());
		Integer id = (Integer)evt.getNewValue();
		if (id != null && form != null) {
			form.setJobClass(null);
			if (id == CUSTOM_JOB_NAME_ID) {
				return;
			}
			form.setOccupationCode(null);
			form.setLsOccCode(null);
			form.setScheduleCode(null);
			form.setContractSchedule(null);
			scheduleDL = null;
			scheduleId = null;
			if (id >= 0) {
				jobClassId = id;
				RoleDAO roleDAO = RoleDAO.getInstance();
				Role role = roleDAO.findById(id);
				if (role != null) {
					form.setJobClass(role.getName());
					form.setLsOccCode(role.getLsOccCode()); // LS-2477 get non-union Occ Codes
				}
			}
			else { // not a job name, but one of our special choices
				occCodeDL = null;	// force refresh of occupation code list
				occCodeId = null;
				occupation = null;
				if (id == GET_FULL_JOB_LIST_ID) {
					jobClassDL = null;	// force refresh of job classification list
					jobClassId = SELECT_JOB_ID;
				}
			}
			View.addFocus("jobClass"); // help Icefaces keep the focus in the right place
		}
	}

	/**
	 * LS-2562
	 * Determines whether the loan-out section should be shown.
	 *
	 * @param event
	 */
	public void listenPaidAsChange(ValueChangeEvent event) {
		PaidAsType paidAs = (PaidAsType)event.getNewValue();
		form.setPaidAs(paidAs);
	}

	/**
	 * Ensure that the given Occupation is listed in the current occCodeDL. This
	 * takes care of the case when a user (a) selects an occupation; (b) selects
	 * an occ-code; then (c) selects an occupation. When an occupation has
	 * multiple occ-codes, and one of those occ-codes has multiple occupations,
	 * then the occupation selected in step (c) may match a different PayRate
	 * entry then the one used to generate the occCodeDL in step (a). E.g.,
	 * given these db entries:
	 * <ul>
	 * <li>id:539/4541/Grip</li>
	 * <li>id:1637/VT4541/Grip</li>
	 * <li>id:1638/VT4541/Grip-40 hrs</li>
	 * </ul>
	 * <p>
	 * (a) User selects "Grip". OccCodeDL=>(539,1637). (b) User selects VT4541.
	 * (c) User selects Grip-40 hrs. The occCode (VT4541) is in the occCodeDL,
	 * but it is the entry for 1637. We need to update it to 1638 so that it
	 * matches the selected occupation entry.
	 *
	 * @param occ The occupation whose id must be in the occCodeDL; if it is not
	 *            there, it's id will replace the entry with the matching
	 *            Occ-Code.
	 */
	private void updateOccCodeDL(Occupation occ) {
		Integer id = occ.getId();
		for (SelectItem si : occCodeDL) {
			if (id.equals(si.getValue())) {
				return; // id matched - all done
			}
		}
		// Missing id; replace the id in the matching Occ-Code entry.
		String occCode = occ.getOccCode();
		for (SelectItem si : occCodeDL) {
			if (occCode.equals(si.getLabel())) {
				si.setValue(id);
				return;
			}
		}
		// This shouldn't happen.
		EventUtils.logError("Unable to update Occ Code list after job-class selection, Occupation id=" + occ.getId());
	}

	/**
	 * ValueChangeListener for the Occupation Code drop-down list, which allows the
	 * user to select which occupation code applies to the employee.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenOccCode(ValueChangeEvent evt) {
		//log.debug("new=" + evt.getNewValue() + ", old=" + evt.getOldValue());
		Integer id = (Integer)evt.getNewValue();
		/*
		 * Note that when the user changes Union selection and are listenUnion
		 * method runs, we may be called by the framework for an OccCode change
		 * (in the same processing cycle as the union change), even though the
		 * user has not touched the Occ Code drop-down. We detect this by the
		 * fact that the occCodeDL is still null, having been set to null by
		 * listenUnion.
		 */
		if (id != null && form != null && occCodeDL != null) {
			form.setScheduleCode(null);
			form.setContractSchedule(null);
			form.setOccupationCode(null);
			form.setLsOccCode(null);
			scheduleDL = null;
			scheduleId = null;
			occupation = null;
			occCodeId = null;
			if (id >= 0) {
				occupation = OccupationDAO.getInstance().findById(id);
				changeOccCode();
			}
		}
	}

	/*public String showInformationMessage() {
		//Add the Message normally using FacesContext::addMessage method
		FacesContext ctx = FacesContext.getCurrentInstance();
		FacesMessage fMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, null, "StartForm.PopulateScaleRates");
		ctx.addMessage(null, fMessage);

        ExtendedRenderKitService erks = Service.getRenderKitService(ctx, ExtendedRenderKitService.class);
        StringBuilder builder= new StringBuilder();
        builder.append("jQuery('.auto-hide').delay(3000).hide('fast');");
        erks.addScript(ctx, builder.toString());

		//Select the message using .auto-hide selector, wait for 3 seconds and hide it fast
		addJavascript("jQuery(‘.auto-hide’).delay(3000).hide(‘fast’);");
		//JavaScriptRunner.runScript(ctx, "jQuery(‘.auto-hide’).delay(3000).hide(‘fast’);");
		return null;
	}*/

	/**
	 * ValueChangeListener for the Schedule drop-down list, which allows the
	 * user to select which contract Schedule applies to the employee.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenSchedule(ValueChangeEvent evt) {
		//log.debug("new=" + evt.getNewValue() + ", old=" + evt.getOldValue());
		Integer id = (Integer)evt.getNewValue();
		changeScheduleId(id, true);
	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * @param event contains old and new values
	 */
	public void listenBatchChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Integer id = (Integer)event.getNewValue();
			if (id != null && form != null) { // LS-1624 check form, in case user left page.
				batchId = id;
				ProductionBatch batch = ProductionBatchDAO.getInstance().findById(batchId);
				form.getEmployment().setProductionBatch(batch);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	private void changeOccCode() {
		occCodeId = occupation.getId();
		form.setOccupationCode(occupation.getOccCode());
		form.setLsOccCode(occupation.getLsOccCode());
		form.setContractCode(occupation.getContractCode()); // LS-1333
		if (union != null) {
			boolean match = false;
			if (jobClassId == CUSTOM_JOB_NAME_ID) {
				match = true;
			}
			else if (jobClassDL != null) {
				// user selected the Occ Code BEFORE selecting the Job Class.
				// check existing JobClass list for matching entry (frequently there)
				for (SelectItem si : jobClassDL) {
					if (occCodeId.equals(si.getValue())) {
						jobClassId = occCodeId;
						form.setJobClass(si.getLabel());
						match = true;
						break;
					}
				}
			}
			if (! match) { // may happen when job classification (Occupation) has multiple occ codes
				// create new job list from Occ Code
				jobClassDL = OccupationDAO.getInstance()
						.createOccupationDLbyUnionOccCode(union.getOccupationUnion(), occupation.getOccCode());
				if (jobClassDL.size() == 1) {
					jobClassId = (Integer)jobClassDL.get(0).getValue();
					//Occupation tempOccupation = OccupationDAO.getInstance().findById(jobClassId);
					form.setJobClass(jobClassDL.get(0).getLabel());
				}
				else if (jobClassDL.size() > 1) { // multiple occupations for one occ-code,
					// Try & find matching entry (should be here)
					for (SelectItem si : jobClassDL) {
						if (occCodeId.equals(si.getValue())) {
							jobClassId = occCodeId;
							form.setJobClass(si.getLabel());
							break;
						}
					}
				}
				if (jobClassDL.size() > 0) { // should be!
					// Give user a way to get back full list of occupations for current union.
					// Note special id value! Checked for in listenJobClass().
					if (! getIsTeamPayroll()) {
						jobClassDL.add(0, new SelectItem(CUSTOM_JOB_NAME_ID, CUSTOM_JOB_NAME_TEXT));
					}
					jobClassDL.add(0, new SelectItem(GET_FULL_JOB_LIST_ID, GET_FULL_JOB_LIST_TEXT));
				}
			}
		}
		if (asa) {
			changeAsaRate();
		}
		else {
			createScheduleDL(true); // refresh the schedule list and update rates if appropriate
		}
	}

	private void changeAsaRate() {
		Production prod = getProduction();
		if (prod.getPayrollPref().getAsaContract() != null) {
			String contract = union.getOccupationUnion() + "-" + prod.getPayrollPref().getAsaContract().getSuffix();
			payRates = PayRateDAO.getInstance()
					.findByContractAndOccCode(form.getWorkStartOrHireDate(), contract, form.getLsOccCode());
			if (payRates != null) {
				fillRates();
			}
		}
	}

	/**
	 * Set the current Schedule code value based on the given PayRate.id
	 * database value. Optionally update the rates in the StartForm from the
	 * PayRate object.
	 *
	 * @param id The database id of the PayRate entry to make current.
	 * @param setRates If true, the rates from the PayRate entry are copied into
	 *            the corresponding StartForm fields.
	 */
	private void changeScheduleId(Integer id, boolean setRates) {
		if (id != null && form != null) {
			form.setScheduleCode(null);
			form.setContractSchedule(null);
			scheduleId = null;
			if (id >= 0) {
				PayRate payRate = PayRateDAO.getInstance().findById(id);
				if (payRate != null) {
					scheduleId = id;
					String scheduleCode = payRate.getContractSchedule();
					form.setContractSchedule(scheduleCode);
					form.setScheduleCode(payRate.getSchedule());
					String contractRateKey = null;
					if (occupation != null) {
						contractRateKey = occupation.getContractRateKey();
					}
					Date rateDate = form.getWorkStartOrHireDate();
					if (getProduction().getPayrollPref().getUsePriorYearRates()) {
						// backup one year from start date...
						Calendar cal = CalendarUtils.getInstance(rateDate);
						cal.add(Calendar.YEAR, -1);
						rateDate = cal.getTime();
					}
					payRates = PayRateDAO.getInstance().findByContractOccCodeAndSchedule(
							rateDate, contractRateKey, form.getLsOccCode(), scheduleCode);
					if (setRates && payRates != null) {
						fillRates();
					}
				}
			}
		}
	}

	/**
	 * Fill in all the rates in the current StartForm (hourly, daily, weekly,
	 * 6th, ...) from the given PayRate instance. We do not fill in any
	 * information for Idle 6th or 7th. The contractCode (e.g., "LA-80")
	 * is also set.
	 *
	 * @param payRates The pair (Studio,Distant) of PayRate objects with the
	 *            applicable rates to be applied to the StartForm.
	 */
	private void fillRates() {
		form.setAllowWorked(false);
		form.setRateType(EmployeeRateType.HOURLY);
		setEmployeeType(EMPLOYEE_TYPE_HOURLY);
		if (payRates[0] != null) {
			form.setContractCode(payRates[0].getContractCode());
		}
		else if (payRates[1] != null) {
			form.setContractCode(payRates[1].getContractCode());
		}
		if (form.getRateType() == EmployeeRateType.HOURLY &&
				(payRates[0] == null || payRates[0].getHourlyRate() == null) &&
				(payRates[1] == null || payRates[1].getHourlyRate() == null)) {
			if ((payRates[0] == null || payRates[0].getWeekly()) &&
					(payRates[1] == null || payRates[1].getWeekly())) {
				form.setRateType(EmployeeRateType.WEEKLY);
				setEmployeeType(EMPLOYEE_TYPE_WEEKLY);
				form.setAllowWorked(true);
			}
			else if ((payRates[0] == null || payRates[0].getDailyRate() != null) &&
					(payRates[1] == null || payRates[1].getDailyRate() != null)) {
				// Has Daily rate, but no Hourly rate -- mark as Daily, On-call (exempt)
				form.setRateType(EmployeeRateType.DAILY);
				setEmployeeType(EMPLOYEE_TYPE_DAILY);
				form.setAllowWorked(true);
			}
		}
		else if ((payRates[0] == null || payRates[0].getEmployeeRateType() == EmployeeRateType.DAILY) &&
				(payRates[1] == null || payRates[1].getEmployeeRateType() == EmployeeRateType.DAILY)) {
			form.setRateType(EmployeeRateType.DAILY);
			setEmployeeType(EMPLOYEE_TYPE_DAILY);
		}
		boolean ratesFilled = StartFormUtils.fillRateSet(form, form.getProd(), payRates);
		if (form.getPrep() != null) {
			ratesFilled |= StartFormUtils.fillRateSet(form, form.getPrep(), payRates);
		}
		populateRateGuarHours();
		if (ratesFilled) {
			MsgUtils.addFacesMessage("StartForm.PopulateScaleRates", FacesMessage.SEVERITY_INFO);
		}
		//showInformationMessage();
	}

	/**
	 * Create and save a new StartForm instance, associated with {@link #contact}, and
	 * with the start and end dates set in the dialog box controls.  If the user
	 * selected a 'related' StartForm from the drop-down list, the new StartForm will
	 * be linked to it.
	 */
	private void createStartForm() {
		StartForm related = null;
		if (addSdReplaceStartFormId != null) {
			related = getStartFormDAO().findById(addSdReplaceStartFormId);
		}

		StartForm latest = null;
		if (addSdFormType.equals(StartForm.FORM_TYPE_NEW) ||
				(addSdFormType.equals(StartForm.FORM_TYPE_REHIRE) && related == null)) {
			if (startFormDL.size() > 0) {
				latest = getStartFormDAO().findLatestForContact(contact);
			}
		}

		contact = ContactDAO.getInstance().refresh(contact);
		Role role = RoleDAO.getInstance().findById(addSdOccupationId);
		Employment emp = EmploymentDAO.getInstance().findByContactProjectRole(contact, project, role);

//		ProjectMember pm = ProjectMemberDAO.getInstance().findByContactProjectRoleId(contact, project, addSdOccupationId);
//		if (pm == null) {
//			throw(new IllegalArgumentException("No ProjectMember found matching selected position."));
//		}
		form = StartFormService.createStartForm(addSdFormType, related, latest,
				contact, emp, SessionUtils.getCurrentOrViewedProject(),
				addSdCreation, addSdEffectiveStart, addSdEffectiveEnd, false, true);

		if (related != null) { // maintain a "chain" of forms for "Change" instances
			form.setPriorFormId(addSdReplaceStartFormId);
		}

		form.setSequence(getStartFormDAO().findMaxSequence(contact)+1);
		form.setFormNumber(contact.getId() + "-" + form.getSequence());

		getStartFormDAO().attachDirty(form);
	}

	/**
	 * For Commercial production Starts, we set the distant rates the same
	 * as the studio rates.
	 */
	private void copyStudioRatesToDistant() {
		copyRates(form.getPrep());
		copyRates(form.getProd());
	}

	/**
	 * Copy the studio rates and hours into the matching Distant fields in the
	 * given StartRateSet.
	 *
	 * @param rates The StartRateSet to be updated.
	 */
	private void copyRates(StartRateSet rates) {
		if (rates != null) {
			copyRates(rates.getHourly());
			copyRates(rates.getDaily());
			copyRates(rates.getWeekly());
			copyRates(rates.getDay6());
			copyRates(rates.getDay7());
			rates.getX15().setLocHrs(null);
			rates.getX20().setLocHrs(null);
			rates.getIdleDay6().setLoc(rates.getHourly().getStudio());
			rates.getIdleDay6().setLocHrs(null);
			rates.getIdleDay7().setLoc(rates.getHourly().getStudio());
			rates.getIdleDay7().setLocHrs(null);
		}
	}

	/**
	 * Copy the studio rate and hours into the Distant (location) rate and hours
	 * fields in the given RateHoursGroup.
	 *
	 * @param rateGroup the RateHoursGroup to be updated.
	 */
	private void copyRates(RateHoursGroup rateGroup) {
		rateGroup.setLoc(rateGroup.getStudio());
		rateGroup.setLocHrs(rateGroup.getStudioHrs());
	}

	/**
	 * Check attached address objects - if empty, reset our references,
	 * else save the data.
	 */
	private void saveAddresses() {
		form.setLoanOutAddress(trimAndMergeAddress(form.getLoanOutAddress()));
		// LS-3592
		form.setLoanOutMailingAddress(trimAndMergeAddress(form.getLoanOutMailingAddress()));
		form.setAgencyAddress(trimAndMergeAddress(form.getAgencyAddress()));
		form.setMailingAddress(trimAndMergeAddress(form.getMailingAddress()));
		form.setPermAddress(trimAndMergeAddress(form.getPermAddress()));
	}

	/**
	 * Trims the given Address object (if not null), and, if the result is not
	 * empty, merges it into the database.
	 *
	 * @param addr The Address object to be modified and saved.
	 * @return The updated Address object, or null if either (a) the original
	 *         parameter was null, or (b) the trimmed Address object was empty.
	 */
	private Address trimAndMergeAddress(Address addr) {
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
			}
			else {
				addr = null;
			}
		}
		return addr;
	}

	/**
	 * Create the list of role names that populates the drop-down list in the
	 * "New Start Form" dialog box.
	 *
	 * @return List of existing roles that have been assigned to the user whose
	 *         StartForm is being displayed.
	 */
	private List<SelectItem> createOccupationDL() {
		List<SelectItem> list = new ArrayList<>();
		Set<Integer> ids = new HashSet<>(); // to avoid duplicate roles
		Production prod = getProduction();
		contact = ContactDAO.getInstance().refresh(contact);
		if (getContact() != null && getContact().getEmployments() != null) {
			for (Employment emp : getContact().getEmployments()) {
				Role role = emp.getRole();
				if (role.isCrewTc() || (role.isCastOrStunt() && prod.getCastUseCrewTc())) {
					if (! ids.contains(role.getId())) { // don't add duplicate roles to list
						if (project == null || // TV/Feature - add all roles; or
								project.equals(emp.getProject())) { // Commercial & this PM is in current project
							ids.add(role.getId());
							list.add(new SelectItem(role.getId(), role.getName()));
						}
					}
				}
			}
		}
		Collections.sort(list, ListView.getSelectItemComparator());
		return list;
	}

	/**
	 * Create the list of occupation codes for the drop-down.
	 *
	 * @return A List of SelectItem`s where the value is the Occupation.id and
	 *         the label is the Occupation.occCode.
	 */
	private List<SelectItem> createOccCodeDL() {
		List<SelectItem> occList = null;
		if (form != null && isUnion && union != null) {
			if (form.getJobClass() != null) {
				occList = OccupationDAO.getInstance().createOccCodeDL(getProduction(), project,
						union.getOccupationUnion(), form.getJobClass());
				if (occList.size() > 1) {
					occList.add(0, SELECT_HEADER);
				}
				else if (occList.size() == 0) {
					// This can happen with "custom" (user entered) job name.
					occList = OccupationDAO.getInstance().createOccCodeDL(getProduction(), project, union.getOccupationUnion());
					if (occList.size() > 1) {
						occList.add(0, SELECT_HEADER);
					}
				}
			}
			else {
				occList = OccupationDAO.getInstance().createOccCodeDL(getProduction(), project, union.getOccupationUnion());
				if (occList.size() > 1) {
					occList.add(0, SELECT_HEADER);
				}
				else if (occList.size() == 0) {
					occList = null;
					occCodeId = null;
					occupation = null;
				}
			}
		}
		return occList;
	}

	/**
	 * Create a List for the Schedule drop-down; if only a single entry exists,
	 * set that value as the selected schedule code.
	 *
	 * @param setRates This parameter is passed through to changeScheduleId --
	 *            if true, that method will update the Start Form rate fields.
	 * @return The List for the schedule code drop-down. The value fields are
	 *         PayRate.id values, and the labels are the numeric schedule codes.
	 */
	private List<SelectItem> createScheduleDL(boolean setRates) {
		if (form != null && ! asa) {
			if (occupation != null && isUnion) {
				scheduleDL = PayRateDAO.getInstance().createScheduleDL(form.getWorkStartOrHireDate(),
								occupation.getContractRateKey(), occupation.getLsOccCode());
			}
			else if (! isUnion) {
				scheduleDL = PayRateDAO.getInstance().createNonUnionScheduleDL(Unions.NON_UNION);
			}
			if (scheduleDL != null) {
				if (scheduleDL.size() > 1) {
					scheduleDL.add(0, SELECT_HEADER);
				}
				if (scheduleId == null) {
					if (scheduleDL.size() == 1) {
						scheduleId = (Integer)scheduleDL.get(0).getValue();
						changeScheduleId(scheduleId, setRates);
					}
					else if (scheduleDL.size() > 1) {
						scheduleId = SELECT_ID;
					}
				}
			}
		}
		return scheduleDL;
	}

	/**
	 * For StartForm tab, make sure we have a StartForm to
	 * display.
	 */
	public void setupStartTab(int tab) {
		if (tab == TAB_START) {
			if (form == null) {
				startFormId = null;
				initStartFormDisplay();
				if (form == null) { // none in list
					form = new StartForm();
				}
			}
		}
	}

	/**
	 * Creates the list of start form "names" for the drop-down selection of
	 * existing StartForm`s for the currently displayed user. Then, if
	 * {@link #startFormId} is null, it is set to be the most current one in the
	 * list -- the one with the most recent effectiveStartDate.
	 * <p>
	 * It finally calls {@link #setupStartForm()} to properly display whichever
	 * start form is selected.
	 */
	private void initStartFormDisplay() {
		List<StartForm> list = createStartFormDL();
		if (list.size() != 0) {
			Date latest = Constants.JAN_1_2000;
			StartForm latestItem = null;
			form = null;
			for (StartForm sd : list) {
				if (startFormId != null && sd.getId().equals(startFormId)) {
					form = sd;
					break; // exact match, all done.
				}
				if (sd.getAnyStartDate().after(latest)) {
					latest = sd.getAnyStartDate();
					latestItem = sd;
				}
			}
			if (startFormId == null || form == null) {
				// either no startFormId, or had it but it didn't match anything in list
				form = latestItem;
				startFormId = form.getId();
			}
		}
		else {
			form = null; // new StartForm();
		}
		setupStartForm();
	}

	/**
	 * Create the startFormDL -- the selection list of the displayed user's StartForms
	 * within the current production or project.
	 */
	private List<StartForm> createStartFormDL() {
		startFormDL = new ArrayList<>();
		Project viewProject = project;
		if (viewProject != null &&
				(userHasViewAllProjects || SessionUtils.getCurrentUser().equals(getvContact().getUser()))) {
			viewProject = null;
		}
		List<StartForm> list = getStartFormDAO().findByContactProjectPermitted(getContact(), viewProject);
		if (list.size() != 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
			for (StartForm sd : list) {
				String label = StartFormUtils.createStartFormLabel(sd, sdf);
				startFormDL.add(new SelectItem(sd.getId(), label));
			}
		}
		return list;
	}

	/**
	 * Attempt to lock the current StartForm. If the lock fails, put up the
	 * prompt dialog.
	 *
	 * @param msgType The 'additional' part of the message id for the prompt.
	 *            This should be an empty string ("") if the default 'lock
	 *            failed' message is desired.  The message id used to look
	 *            up the prompt is <"StartForm.Locked." + msgType + "Text">.
	 * @return True iff the current StartForm has been locked.
	 */
	private boolean lockAndPrompt(String msgType) {
		if (isOnboarding) {
			// In onboarding environment, ContactFormBean will have locked
			// the ContactDocument already; don't bother locking the StartForm.
			return true;
		}
		user = SessionUtils.getCurrentUser();
		if (! getStartFormDAO().lock(form, user)) {
			PopupBean.getInstance().show(null, 0,
					"StartForm.Locked.Title",
					"StartForm.Locked." + msgType + "Text",
					"Confirm.OK", null); // no cancel button
			if (form != null) {
				log.debug("edit/etc prevented: locked by user #" + form.getLockedBy());
			}
			editMode = false;
			return false;
		}
		return true;
	}

	/**
	 * This method is called by the JSF framework when this bean is about to go
	 * 'out of scope', e.g., when the user is leaving the page. Note that in JSF
	 * 2.1, this method is not called for session expiration, so we handle that
	 * case via the Disposable interface.
	 */
	@PreDestroy
	public void preDestroy() {
		log.debug("");
		if (disposer != null) {
			disposer.unregister(this);
		}
		dispose();
	}

	/**
	 * This method is called when this bean is about to go 'out of scope', e.g.,
	 * when the user is leaving the page or their session expires. We use it to
	 * unlock the StartForm to make it available again for editing.
	 */
	@Override
	public void dispose() {
		log.debug("");
		try {
			if (form != null && user != null) {
				form = getStartFormDAO().refresh(form); // prevent "non-unique object" failure in logout case
				if (form != null && form.getLockedBy() != null
						 && user.getId().equals(form.getLockedBy())) {
					log.debug("dispose calling unlock");
					getStartFormDAO().unlock(form, user.getId());
				}
			}
		}
		catch (Exception e) {
			log.error("Exception: ", e);
		}
	}

	/** Called by the PopupBean when the user clicks OK
	 * on a confirmation dialog. */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_DELETE_START_FORM:
					res = actionDeleteOk();
					ListView.addClientResizeScroll();
					break;
				default:
					res = super.confirmOk(action);
					break;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
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
		String res = null;
		switch(action) {
			case ACT_DELETE_START_FORM:
				res = actionDeleteCancel();
				break;
			default:
				res = super.confirmCancel(action);
				break;
		}
		return res;
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		StartForm startForm = null;
		contact = contactDocument.getContact();
//		setProduction(contact.getProduction()); // SessionUtils.getNonSystemProduction();
//		if (production == null) { // "My Starts" page
//			Integer id = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
//			if (id != null) {
//				production = ProductionDAO.getInstance().findById(id);
//				if (getProduction() != null) {
//					isOnboarding = getProduction().getAllowOnboarding();
//				}
//			}
//		}
		if (contactDocument.getRelatedFormId() != null) {
			startForm = StartFormDAO.getInstance().findById(contactDocument.getRelatedFormId());
		}
		else { // Should not happen, as we create SF's when they are distributed.
			String msg = "Missing StartForm for ContactDocument, CD id=" + contactDocument.getId() + ", SF id=" + startFormId;
			startForm = null; //new StartForm();
			startFormId = null;
			log.error(msg);
			EventUtils.logError(msg);
		}
		setContactDoc(contactDocument);
		setForm(startForm);
		setupStartForm();

		if (startForm != null) {
			log.debug("StartForm id: " + startForm.getId());
			SessionUtils.put(Constants.ATTR_START_FORM_ID, startForm.getId());
		}
		//initScrollPos(0);
	}

//	@Override - Not used.
//	public void setRelatedFormId(ContactDocument cd) {
//		if (cd.getId() != null) {
//			cd.setRelatedFormId(SessionUtils.getInteger(Constants.ATTR_START_FORM_ID));
//			// To avoid DataIntegrityViolationException on form type of previously stored contact documents.
//			if (cd.getFormType() == null) {
//				cd.setFormType(PayrollFormType.START);
//			}
//			ContactDocumentDAO.getInstance().attachDirty(cd);
//		}
//	}

	@Override
	public String autoFillForm(boolean prompted) {
		try {
			log.debug("Form id: " + getForm().getId());
			User cdUser = getContactDoc().getContact().getUser();
			cdUser = UserDAO.getInstance().refresh(cdUser);
			form.setFirstName(cdUser.getFirstName());
			form.setMiddleName(cdUser.getMiddleName());
			form.setLastName(cdUser.getLastName());
			form.setEthnicCode(cdUser.getEthnicCode());
			form.setGender(cdUser.getGender());
			form.setCitizenStatus(cdUser.getCitizenStatus());
			form.setStateOfResidence(cdUser.getStateOfResidence());
			form.setDateOfBirth(cdUser.getBirthdate());
			form.setMinor(cdUser.getMinor());
			form.setPhone(cdUser.getPrimaryPhone());
			form.setAgentRep(cdUser.getAgentRep());
			form.setAgencyName(cdUser.getAgencyName());
			if (cdUser.getAgencyAddress() != null) {
				form.getAgencyAddress().copyFrom(cdUser.getAgencyAddress());
			}
			form.setEmergencyName(cdUser.getEmergencyName());
			form.setEmergencyPhone(cdUser.getEmergencyPhone());
			form.setEmergencyRelation(cdUser.getEmergencyRelation());
			form.setSocialSecurity(cdUser.getSocialSecurity());
			if (cdUser.getMailingAddress() != null) {
				form.getMailingAddress().copyFrom(cdUser.getMailingAddress());
			}
			if (cdUser.getHomeAddress() != null) {
				form.getPermAddress().copyFrom(cdUser.getHomeAddress());
			}

			// LOAN-OUT fields ...
			form.setLoanOutCorpName(cdUser.getLoanOutCorpName());
			form.setFederalTaxId(cdUser.getFederalTaxId());  // TTCV-809
			form.setStateTaxId(cdUser.getStateTaxId());
			form.setIncorporationState(cdUser.getIncorporationState());
			form.setIncorporationDate(cdUser.getIncorporationDate());
			form.setTaxClassification(cdUser.getTaxClassification());
			form.setLlcType(cdUser.getLlcType());
			if (cdUser.getLoanOutAddress() != null) {
				form.getLoanOutAddress().copyFrom(cdUser.getLoanOutAddress());
			}
			// LS-3592
			if (cdUser.getLoanOutMailingAddress() != null) {
				form.getLoanOutMailingAddress().copyFrom(cdUser.getLoanOutMailingAddress());
			}
			form.setLoanOutPhone(cdUser.getLoanOutPhone());

			form.setLoanOutQualifiedCa(cdUser.getLoanOutQualifiedCa());
			form.setLoanOutQualifiedNy(cdUser.getLoanOutQualifiedNy());
			form.setLoanOutQualifiedStates(cdUser.getLoanOutQualifiedStates());

			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#compareFormAccountDetails(com.lightspeedeps.model.User)
	 */
	@Override
	public User compareFormAccountDetails(User cdUser) {
		boolean valid = true;
		log.debug("Form id: " + getForm().getId());

		if ((! StringUtils.isEmpty(form.getFirstName())) && ! form.getFirstName().equals(cdUser.getFirstName())) {
			cdUser.setFirstName(form.getFirstName());
			valid = false;
		}
		if ((! Form.isEmptyNA(form.getMiddleName())) && ! form.getMiddleName().equals(cdUser.getMiddleName())) {
			cdUser.setMiddleName(form.getMiddleName());
			valid = false;
		}
		if ((! StringUtils.isEmpty(form.getLastName())) && ! form.getLastName().equals(cdUser.getLastName())) {
			cdUser.setLastName(form.getLastName());
			valid = false;
		}
		if (form.getEthnicCode() != null && ! form.getEthnicCode().equals(cdUser.getEthnicCode())) {
			cdUser.setEthnicCode(form.getEthnicCode());
			valid = false;
		}
		if (form.getGender() != null && ! form.getGender().equals(cdUser.getGender())) {
			cdUser.setGender(form.getGender());
			valid = false;
		}
		if (form.getCitizenStatus() != null && ! form.getCitizenStatus().equals(cdUser.getCitizenStatus())) {
			cdUser.setCitizenStatus(form.getCitizenStatus());
			valid = false;
		}
		if (form.getStateOfResidence() != null && ! form.getStateOfResidence().equals(cdUser.getStateOfResidence())) {
			cdUser.setStateOfResidence(form.getStateOfResidence());
			valid = false;
		}
		if (form.getDateOfBirth() != null && ! form.getDateOfBirth().equals(cdUser.getBirthdate())) {
			cdUser.setBirthdate(form.getDateOfBirth());
			valid = false;
		}
		if (form.getMinor() != cdUser.getMinor()) {
			cdUser.setMinor(form.getMinor());
			valid = false;
		}
		if (form.getPhone() != null && ! form.getPhone().equals(cdUser.getPrimaryPhone())) {
			switch(cdUser.getPrimaryPhoneIndex()) {
				case 0:
					cdUser.setBusinessPhone(form.getPhone());
					break;
				case 1:
					cdUser.setCellPhone(form.getPhone());
					break;
				case 2:
					cdUser.setHomePhone(form.getPhone());
					break;
			}
			valid = false;
		}
		if (form.getAgentRep() != cdUser.getAgentRep()) {
			cdUser.setAgentRep(form.getAgentRep());
			valid = false;
		}
		if (form.getAgencyName() != null && ! form.getAgencyName().equals(cdUser.getAgencyName())) {
			cdUser.setAgencyName(form.getAgencyName());
			valid = false;
		}

		if (cdUser.getAgencyAddress() == null) {
			cdUser.setAgencyAddress(new Address());
		}
		cdUser.setAgencyAddress(AddressDAO.getInstance().refresh(cdUser.getAgencyAddress()));

		if (form.getAgencyAddress() != null && (! form.getAgencyAddress().equalsAddress(cdUser.getAgencyAddress()))) {
			cdUser.getAgencyAddress().copyFrom(form.getAgencyAddress());
		}

		if (cdUser.getHomeAddress() == null) {
			cdUser.setHomeAddress(new Address());
		}
		cdUser.setHomeAddress(AddressDAO.getInstance().refresh(cdUser.getHomeAddress()));

		if (form.getPermAddress() != null && (! form.getPermAddress().equalsAddress(cdUser.getHomeAddress()))) {
			cdUser.getHomeAddress().copyFrom(form.getPermAddress());
		}

		if (form.getEmergencyName() != null && ! form.getEmergencyName().equals(cdUser.getEmergencyName())) {
			cdUser.setEmergencyName(form.getEmergencyName());
			valid = false;
		}
		if (form.getEmergencyPhone() != null && ! form.getEmergencyPhone().equals(cdUser.getEmergencyPhone())) {
			cdUser.setEmergencyPhone(form.getEmergencyPhone());
			valid = false;
		}
		if (form.getEmergencyRelation() != null && ! form.getEmergencyRelation().equals(cdUser.getEmergencyRelation())) {
			cdUser.setEmergencyRelation(form.getEmergencyRelation());
			valid = false;
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && ! form.getSocialSecurity().equals(cdUser.getSocialSecurity())) {
			cdUser.setSocialSecurity(form.getSocialSecurity());
			valid = false;
		}

		if (cdUser.getMailingAddress() == null) {
			cdUser.setMailingAddress(new Address());
		}
		cdUser.setMailingAddress(AddressDAO.getInstance().refresh(cdUser.getMailingAddress()));

		if (form.getMailingAddress() != null && (! form.getMailingAddress().equalsAddress(cdUser.getMailingAddress()))) {
			cdUser.getMailingAddress().copyFrom(form.getMailingAddress());
		}

		// LOAN-OUT fields ...
		if (form.getLoanOutCorpName() != null && ! form.getLoanOutCorpName().equals(cdUser.getLoanOutCorpName())) {
			cdUser.setLoanOutCorpName(form.getLoanOutCorpName());
			valid = false;
		}
		if (form.getFederalTaxId() != null && ! form.getFederalTaxId().equals(cdUser.getFederalTaxId())) {
			cdUser.setFederalTaxId(form.getFederalTaxId());
			valid = false;
		}
		if (form.getStateTaxId() != null && ! form.getStateTaxId().equals(cdUser.getStateTaxId())) {
			cdUser.setStateTaxId(form.getStateTaxId());
			valid = false;
		}
		if (form.getIncorporationState() != null && ! form.getIncorporationState().equals(cdUser.getIncorporationState())) {
			cdUser.setIncorporationState(form.getIncorporationState());
			valid = false;
		}
		if (form.getIncorporationDate() != null && ! form.getIncorporationDate().equals(cdUser.getIncorporationDate())) {
			cdUser.setIncorporationDate(form.getIncorporationDate());
			valid = false;
		}

		if (cdUser.getLoanOutAddress() == null) {
			cdUser.setLoanOutAddress(new Address());
		}
		cdUser.setLoanOutAddress(AddressDAO.getInstance().refresh(cdUser.getLoanOutAddress()));

		if (form.getLoanOutAddress() != null && (! form.getLoanOutAddress().equalsAddress(cdUser.getLoanOutAddress()))) {
			cdUser.getLoanOutAddress().copyFrom(form.getLoanOutAddress());
		}
		// LS-3592
		if (cdUser.getLoanOutMailingAddress() == null) {
			cdUser.setLoanOutMailingAddress(new Address());
		}
		cdUser.setLoanOutMailingAddress(AddressDAO.getInstance().refresh(cdUser.getLoanOutMailingAddress()));

		if (form.getLoanOutMailingAddress() != null && (! form.getLoanOutMailingAddress().equalsAddress(cdUser.getLoanOutMailingAddress()))) {
			cdUser.getLoanOutMailingAddress().copyFrom(form.getLoanOutMailingAddress());
		}
		if (form.getLoanOutPhone() != null && ! form.getLoanOutPhone().equals(cdUser.getLoanOutPhone())) {
			cdUser.setLoanOutPhone(form.getLoanOutPhone());
			valid = false;
		}
		if (form.getLoanOutQualifiedCa() != cdUser.getLoanOutQualifiedCa()) {
			cdUser.setLoanOutQualifiedCa(form.getLoanOutQualifiedCa());
			valid = false;
		}
		if (form.getLoanOutQualifiedNy() != cdUser.getLoanOutQualifiedNy()) {
			cdUser.setLoanOutQualifiedNy(form.getLoanOutQualifiedNy());
			valid = false;
		}
		if (form.getLoanOutQualifiedStates() != null && ! form.getLoanOutQualifiedStates().equals(cdUser.getLoanOutQualifiedStates())) {
			cdUser.setLoanOutQualifiedStates(form.getLoanOutQualifiedStates());
			valid = false;
		}
		setValidData(valid);
		log.debug("valid Data :" + valid);
		return super.compareFormAccountDetails(cdUser);
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		String blankField = null;
		if (form.getFirstName() == null || form.getFirstName().isEmpty()) {
			isValid = false;
			blankField = "First Name";
		}
		else if (form.getLastName() == null || form.getLastName().isEmpty()){
			isValid = false;
			blankField = "Last Name";
		}
		else if (form.getSocialSecurity() == null || form.getSocialSecurity().isEmpty()){
			isValid = false;
			blankField = "Social Security Number";
		}
		else if (getUnion() == null) {
			isValid = false;
			blankField = "Union";
		}
		else if (form.getJobClass() == null || form.getJobClass().isEmpty()) {
			isValid = false;
			blankField = "Job Class";
		}
		else if (form.getCitizenStatus() == null || form.getCitizenStatus().isEmpty()) {
			// LS-3581 Only do Citizenship validation for Non-Team clients.
			if(! FF4JUtils.useFeature(FeatureFlagType.TTCO_ADDR_UNIF_PAYROLL_START) || ! getIsTeamPayroll()) {
				isValid = false;
				blankField = "Citizenship Status";
			}
		}
		else if (form.getDateOfBirth() == null) {
			isValid = false;
			blankField = "Date Of Birth";
		}
		else if ((blankField = checkAddressValid(form.getMailingAddress())) != null) {
			isValid = false;
			blankField = "Mailing " + blankField;
		}
		else if(getIsTeamPayroll() && form.getPaidAs() == null && !getProduction().getType().isTours()) {// LS-3399 Do not validate for Tours productions
			// LS-2657 Validate for required fields
			if( FF4JUtils.useFeature(FeatureFlagType.TTCO_ENHANCED_LOAN_OUT)) {
				isValid = false;
				blankField = "Employee is requesting to get paid as";
			}
		}
		else if(getIsTeamPayroll() && (form.getPaidAs() != null && form.getPaidAs().isLoanOut())
				&& (form.getFederalTaxId() == null || form.getFederalTaxId().isEmpty())) {
			// LS-2657 Validate for required fields
			if( FF4JUtils.useFeature(FeatureFlagType.TTCO_ENHANCED_LOAN_OUT)) {
				isValid = false;
				blankField = "Federal Tax ID";
			}
		}
		/*else if (form.getMailingAddress() == null) {
			isValid = false;
			blankField = "Mailing Address Fields";
		}
		else if (form.getMailingAddress().getAddrLine1() == null || form.getMailingAddress().getAddrLine1().isEmpty()) {
			isValid = false;
			blankField = "Mailing Address";
		}
		else if (form.getMailingAddress().getCity() == null || form.getMailingAddress().getCity().isEmpty()) {
			isValid = false;
			blankField = "Mailing City";
		}
		else if (form.getMailingAddress().getState() == null || form.getMailingAddress().getState().isEmpty()) {
			isValid = false;
			blankField = "Mailing State";
		}
		else if (form.getMailingAddress().getZip() == null || form.getMailingAddress().getZip().isEmpty()) {
			isValid = false;
			blankField = "Mailing Zip";
		}*/
		else if (form.getStateOfResidence() == null || form.getStateOfResidence().isEmpty()) {
			// LS-3581 Only do State of Residence validation for Non-Team clients.
			if(! FF4JUtils.useFeature(FeatureFlagType.TTCO_ADDR_UNIF_PAYROLL_START) || ! getIsTeamPayroll()) {
				isValid = false;
				blankField = "State Of Residence";
			}
		}
		else {
			isValid = true;
		}
		if (! isValid) {
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		User currentUser = SessionUtils.getCurrentUser();
		// Check for discrepancy for SSN between Start Form and user record.
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}

		isValid &= isValidSsn(form.getSocialSecurity());

		log.debug("valid : " + isValid);
		setSubmitValid(isValid);

		return super.checkSubmitValid();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#checkSaveValid()
	 */
	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		isValid = StartFormUtils.validateStartForm(form, isUnion);
		isValid &= checkAddressValid(form.getMailingAddress(), "MailingZipCode");
		isValid &= checkAddressValid(form.getPermAddress(), "PermanentZipCode");
		isValid &= checkAddressValid(form.getAgencyAddress(), "AgencyZipCode");
		log.debug(" isValid = "  + isValid);
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/**
	 * Determine the setting of the {@link #useNonUnionOccList} flag. LS-1994
	 */
	private void calcUseOccListFlag() {
		useNonUnionOccList = false;
		if (getIsTeamPayroll() && ! isUnion) {
			/* Prevent custom occupations for Tours/hybrids */
			if (getProduction().getType().isTours()) {
				useNonUnionOccList = true; // not for Tours
			}
			else if (getProduction().getType().isAicp() &&
					getProduction().getPayrollPref().getIncludeTouring()) {
				useNonUnionOccList = true; // not for "hybrids"
			}
		}
	}

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}

	/** See {@link #employeeAccount}. */
	public String getEmployeeAccount() {
		return employeeAccount;
	}
	/** See {@link #employeeAccount}. */
	public void setEmployeeAccount(String employeeAccount) {
		this.employeeAccount = employeeAccount;
	}

	/** See {@link #showAddStartForm}. */
	public boolean getShowAddStartForm() {
		return showAddStartForm;
	}
	/** See {@link #showAddStartForm}. */
	public void setShowAddStartForm(boolean b) {
		showAddStartForm = b;
	}

	/** See {@link #startFormDL}. */
	public List<SelectItem> getStartFormDL() {
		if (startFormDL == null) {
			initStartFormDisplay();
		}
		return startFormDL;
	}
	/** See {@link #startFormDL}. */
	public void setStartFormDL(List<SelectItem> startFormDL) {
		this.startFormDL = startFormDL;
	}

	/** See {@link #startFormId}. */
	public Integer getStartFormId() {
		if (startFormId == null) {
			initStartFormDisplay();
		}
		return startFormId;
	}
	/** See {@link #startFormId}. */
	public void setStartFormId(Integer id) {
		startFormId = id;
	}

	/** See {@link #startForm}. */
	@Override
	public StartForm getForm() {
		if (form == null) {
			initStartFormDisplay();
		}
		return form;
	}
//	/** See {@link #startForm}. */
//	public void setForm(StartForm startForm) {
//		this.form = startForm;
//	}

//	/**See {@link #studioLocDefault}. */
//	public String getStudioLocDefault() {
//		return studioLocDefault;
//	}
//	/**See {@link #studioLocDefault}. */
//	public void setStudioLocDefault(String studioLocDefault) {
//		this.studioLocDefault = studioLocDefault;
//	}

	/**See {@link #showLoanOut}. */
	public boolean getShowLoanOut() {
		return showLoanOut;
	}
	/**See {@link #showLoanOut}. */
	public void setShowLoanOut(boolean showLoanOut) {
		this.showLoanOut = showLoanOut;
	}

	/** Selection list contents for Studio/Distant(Location) rate type choice. */
	public SelectItem[] getRateTypeDL() {
		return WeeklyTimecard.RATE_TYPE_SELECTION;
	}

	/** Selection list for ACA Employment Basis drop-down. */
	public List<SelectItem> getEmploymentBasisDL() {
		return EnumList.getEmploymentBasisTypeList();
	}

	/** Selection list for ACA Employment Basis drop-down. */
	public List<SelectItem> getDepartmentDL() {
		if (departmentDL == null) {
			// get the full department list - without filtering by "active" departments
			departmentDL = DepartmentUtils.getDepartmentCrewDL();
		}
		return departmentDL;
	}

	/**
	 * @return {@link #prodBatchList}, creating it first
	 * if necessary.*/
	public List<SelectItem> getProdBatchList() {
		if (prodBatchList == null) {
//			Project project = null;
//			if (production != null && production.getType().hasPayrollByProject()) {
//				project = SessionUtils.getCurrentProject();
//			}
			prodBatchList = BatchSetupBean.createProdBatchList(getProduction(), project);
			prodBatchList.add(0, new SelectItem(SELECT_ID, "(no batch selected)"));
		}
		return prodBatchList;
	}

	/**
	 * ValueChangeListener for Effective dates
	 * @param event contains old and new values
	 */
	public void listenChangeEffectiveDate(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Date date = (Date)event.getNewValue();
			String id = event.getComponent().getId();
			log.debug("ID = " + id);
			if (date != null && addSdFormType.equals(StartForm.FORM_TYPE_CHANGE)) {
				Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
				cal.setTime(date);
				//CalendarUtils.setStartOfDay(cal);
				if (id.equals("effectiveStart")) {
					log.debug(" ");
					cal.add(Calendar.DAY_OF_MONTH, -1); // backup one day
					addSdEffectiveEnd = cal.getTime();
					log.debug("AddSdEffectiveEnd : " + addSdEffectiveEnd);
				}
				else {
					log.debug(" ");
					cal.add(Calendar.DAY_OF_MONTH, 1);
					addSdEffectiveStart = cal.getTime();
					log.debug("AddSdEffectiveStart : " + addSdEffectiveStart);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/** Return the list of Retirement Plan choices, generated
	 * by StartFormService.getRetirementPlanDL() */
	public List<SelectItem> getRetirementPlanDL() {
		return StartFormService.getRetirementPlanDL();
	}

	/** See {@link com.lightspeedeps.util.app.Constants#CITIZEN_STATUS_ITEMS}. */
	public SelectItem[] getCitizenStatusItems() {
		return Constants.CITIZEN_STATUS_ITEMS;
	}

	/**
	 * List of PaidAsTypes that determine whether the employee will
	 * be paid as an individual or loan-out
	 * LS-2562
	 *
	 * @return list of PaidAsTypes
	 */
	public List<SelectItem> getPaidAsDL() {
		return EnumList.getPaidAsList();
	}

	/** See {@link #employeeType}. */
	public String getEmployeeType() {
		return employeeType;
	}
	/** See {@link #employeeType}. */
	public void setEmployeeType(String employeeType) {
		this.employeeType = employeeType;
	}

	/** See {@link #addSdCreation}. */
	public Date getAddSdCreation() {
		return addSdCreation;
	}
	/** See {@link #addSdCreation}. */
	public void setAddSdCreation(Date addSdCreation) {
		this.addSdCreation = addSdCreation;
	}

	/** See {@link #addSdEffectiveStart}. */
	public Date getAddSdEffectiveStart() {
		return addSdEffectiveStart;
	}
	/** See {@link #addSdEffectiveStart}. */
	public void setAddSdEffectiveStart(Date addSdEffectiveStart) {
		this.addSdEffectiveStart = addSdEffectiveStart;
	}

	/** See {@link #addSdEffectiveEnd}. */
	public Date getAddSdEffectiveEnd() {
		return addSdEffectiveEnd;
	}
	/** See {@link #addSdEffectiveEnd}. */
	public void setAddSdEffectiveEnd(Date addSdEffectiveEnd) {
		this.addSdEffectiveEnd = addSdEffectiveEnd;
	}

	/** See {@link #addSdFormType}. */
	public String getAddSdFormType() {
		return addSdFormType;
	}
	/** See {@link #addSdFormType}. */
	public void setAddSdFormType(String type) {
		addSdFormType = type;
	}

	/** See {@link #addSdReplaceStartFormId}. */
	public Integer getAddSdReplaceStartFormId() {
		return addSdReplaceStartFormId;
	}
	/** See {@link #addSdReplaceStartFormId}. */
	public void setAddSdReplaceStartFormId(Integer id) {
		addSdReplaceStartFormId = id;
	}

	/** See {@link #addSdOccupationId}. */
	public Integer getAddSdOccupation() {
		return addSdOccupationId;
	}
	/** See {@link #addSdOccupationId}. */
	public void setAddSdOccupation(Integer id) {
		addSdOccupationId = id;
	}

	/** See {@link #addSdFormDL}. */
	public List<SelectItem> getAddSdFormDL() {
		if (addSdFormDL == null) {
			if (startFormDL == null) {
				createStartFormDL();
			}
			if (startFormDL.size() > 0) {
				addSdFormDL = new ArrayList<>(startFormDL);
				addSdFormDL.add(0, new SelectItem(SELECT_ID, "Select a Start Form"));
			}
		}
		return addSdFormDL;
	}
	/** See {@link #addSdFormDL}. */
	public void setAddSdFormDL(List<SelectItem> list) {
		addSdFormDL = list;
	}

	/** See {@link #addSdOccupationDL}. */
	public List<SelectItem> getAddSdOccupationDL() {
		if (addSdOccupationDL == null) {
			addSdOccupationDL = createOccupationDL();
		}
		return addSdOccupationDL;
	}
	/** See {@link #addSdOccupationDL}. */
	public void setAddSdOccupationDL(List<SelectItem> addSdOccupationDL) {
		this.addSdOccupationDL = addSdOccupationDL;
	}

	/** See {@link #toursWorkZoneDL}. */
	public List<SelectItem> getToursWorkZoneDL() {
		if(toursWorkZoneDL == null) {
			toursWorkZoneDL = new ArrayList<>();

			toursWorkZoneDL.add(new SelectItem(WorkZone.DL, MsgUtils.getMessage("Timesheet.WorkZone.StudioDistance")));
			toursWorkZoneDL.add(new SelectItem(WorkZone.ST, MsgUtils.getMessage("Timesheet.WorkZone.Studio")));
		}
		return toursWorkZoneDL;
	}

	/** See {@link #toursWorkZoneDL}. */
	public void setToursWorkZoneDL(List<SelectItem> toursWorkZoneDL) {
		this.toursWorkZoneDL = toursWorkZoneDL;
	}

	/**See {@link #isUnion}. */
	public boolean getIsUnion() {
		return isUnion;
	}
	/**See {@link #isUnion}. */
	public void setIsUnion(boolean isUnion) {
		this.isUnion = isUnion;
	}

	/**See {@link #union}. */
	public Unions getUnion() {
		return union;
	}
	/**See {@link #union}. */
	public void setUnion(Unions union) {
		this.union = union;
	}

	public Boolean getUsesVideoTape() {
		if(usesVideoTape == null) {
			usesVideoTape = ContractUtils.calculateUsesVideoTape(getProduction());
		}
		return usesVideoTape;
	}

	/**
	 * @return {@link #unionNameDL}, creating it first
	 * if necessary.*/
	public List<SelectItem> getUnionNameDL() {
		Production prod = getProduction();
		if (unionNameDL == null && prod != null) {
			boolean allUnions = true;
			prod = ProductionDAO.getInstance().refresh(prod);
			if (prod.getPayrollPref().getUse30Htg()) {
				allUnions = false;
				//production = ProductionDAO.getInstance().refresh(production);
				for (Contract c : prod.getContracts()) {
					// Temporary? Show all unions if VideoTape agreement assigned.
					if (c.getUnionKey().equals("VT")) {
						allUnions = true;
						break;
					}
				}
			}
			if (allUnions) {
				if (prod.getType().hasPayrollByProject()) {
					unionNameDL = UnionsDAO.getInstance().createCommercialUnionDL();
				}
				else {
					unionNameDL = UnionsDAO.getInstance().createNonCommercialUnionDL();
				}
			}
			else {
				unionNameDL = UnionsDAO.getInstance().createContractUnionDL(prod);
			}
			unionNameDL.add(0, new SelectItem(SELECT_ID, "(select union)"));
		}
		return unionNameDL;
	}
	/**See {@link #unionNameDL}. */
	public void setUnionNameDL(List<SelectItem> unionList) {
		unionNameDL = unionList;
	}

	/**See {@link #jobClassId}. */
	public Integer getJobClassId() {
		return jobClassId;
	}
	/**See {@link #jobClassId}. */
	public void setJobClassId(Integer jobClassId) {
		this.jobClassId = jobClassId;
	}

	/**See {@link #jobClassDL}. */
	public List<SelectItem> getJobClassDL() {
		if (jobClassDL == null) {
			if (useNonUnionOccList) {
				 /* LS-1994 use fixed occupation list for Tours & hybrids */
				jobClassDL = createAllRoles();
			}
			else if (union != null) {
				Production prod = ProductionDAO.getInstance().refresh(getProduction());
				jobClassDL = OccupationDAO.getInstance().createOccupationDLbyUnion(prod, project, union);
				if (! getIsTeamPayroll()) {
					jobClassDL.add(0, new SelectItem(CUSTOM_JOB_NAME_ID, CUSTOM_JOB_NAME_TEXT));
				}
				jobClassDL.add(0, new SelectItem(SELECT_JOB_ID, SELECT_JOB_TEXT));
			}
		}
		return jobClassDL;
	}
	/**See {@link #jobClassDL}. */
	public void setJobClassDL(List<SelectItem> jobClassDL) {
		this.jobClassDL = jobClassDL;
	}

	/**
	 * Create list of non-union roles across departments. Used for Tours and
	 * Hybrid productions if Team client. LS-1994
	 *
	 * @return List of SelectItem's of all occupations for the available
	 *         departments.
	 */
	private List<SelectItem> createAllRoles() {
		List<SelectItem> occupationDL = new ArrayList<>();
		Contact ct = getvContact(); // Contact of viewer; more accurate in My Starts environment
		if (ct == null) {
			ct = SessionUtils.getCurrentContact();
		}
		List<SelectItem> allowedDepts = DepartmentUtils.getDepartmentDL(ct, project, true, true);
		for (SelectItem si : allowedDepts) {
			Integer deptId = (Integer)si.getValue();
			RoleSelectType selectType = null;
			if (Constants.RESTRICTED_ROLES_DEPARTMENT_IDS.contains(deptId)) {
				selectType = RoleSelectType.EOR_D;
				if (getProduction().getType().isTours()) {
					selectType = RoleSelectType.EOR_D;
					if (getProduction().getPayrollPref().getTeamEor() == EmployerOfRecord.TEAM_TOURS) {
						selectType = RoleSelectType.EOR_S;
					}
				}
				else if (getProduction().getType().isAicp()) {
					PayrollPreference pref = PayrollPreferenceDAO.getInstance().refresh(project.getPayrollPref());
					boolean comp = pref.getWorkersComp();
					selectType = (comp ? RoleSelectType.WC_Y : RoleSelectType.WC_N);
				}
			}
			occupationDL.addAll(RoleDAO.getInstance().createRoleIdSelectList(getProduction(), deptId, false, selectType, null));
		}
		occupationDL.add(0, new SelectItem(SELECT_JOB_ID, SELECT_JOB_TEXT));
		log.debug("role size=" + occupationDL.size());
		return occupationDL;
	}

	/**See {@link #occCodeId}. */
	public Integer getOccCodeId() {
		return occCodeId;
	}
	/**See {@link #occCodeId}. */
	public void setOccCodeId(Integer occCodeId) {
		this.occCodeId = occCodeId;
	}

	/**See {@link #occCodeDL}. */
	public List<SelectItem> getOccCodeDL() {
		if (occCodeDL == null) {
			occCodeDL = createOccCodeDL();
		}
		return occCodeDL;
	}
	/**See {@link #occCodeDL}. */
	public void setOccCodeDL(List<SelectItem> occCodeDL) {
		this.occCodeDL = occCodeDL;
	}

	public boolean getOccCodeDlEnable() {
		if (occCodeDL == null) {
			return false;
		}
		return occCodeDL.size() > 1;
	}

	/**See {@link #scheduleId}. */
	public Integer getScheduleId() {
		return scheduleId;
	}
	/**See {@link #scheduleId}. */
	public void setScheduleId(Integer scheduleId) {
		this.scheduleId = scheduleId;
	}

	/**See {@link #scheduleDL}. */
	public List<SelectItem> getScheduleDL() {
		if (scheduleDL == null) {
			// Build the schedule drop-down, but do NOT update the StartForm rates!
			scheduleDL = createScheduleDL(false);
		}
		return scheduleDL;
	}
	/**See {@link #scheduleDL}. */
	public void setScheduleDL(List<SelectItem> scheduleDL) {
		this.scheduleDL = scheduleDL;
	}

	/**See {@link #asa}. */
	public boolean getAsa() {
		return asa;
	}
	/**See {@link #asa}. */
	public void setAsa(boolean asa) {
		this.asa = asa;
	}

	/**See {@link #batchId}. */
	public Integer getBatchId() {
		return batchId;
	}
	/**See {@link #batchId}. */
	public void setBatchId(Integer batchId) {
		this.batchId = batchId;
	}

	/** See {@link #wageState}. */
	public String getWageState() {
		return wageState;
	}
	/** See {@link #wageState}. */
	public void setWageState(String wageState) {
		this.wageState = wageState;
	}

	/** See {@link #wageRuleStateDL}. */
	public List<SelectItem> getWageRuleStateDL() {
		if (wageRuleStateDL == null) {
			wageRuleStateDL = ApplicationScopeBean.getInstance().getStateCodeWorkedDL(getProduction());
		}
		return wageRuleStateDL;
	}

	/** Drop-down selection for "Weekly"/"Daily" allowance settings. */
	public SelectItem[] getWeeklyChoices() {
		return Allowance.WEEKLY_CHOICE;
	};

	/** See {@link #showDayRate}. */
	public boolean getShowDayRate() {
		return showDayRate;
	}
	/** See {@link #showDayRate}. */
	public void setShowDayRate(boolean showDayRate) {
		this.showDayRate = showDayRate;
	}

	/** See {@link #dayRateAmount}. */
	public String getDayRateAmount() {
		return dayRateAmount;
	}
	/** See {@link #dayRateAmount}. */
	public void setDayRateAmount(String dayRateAmount) {
		this.dayRateAmount = dayRateAmount;
	}

	/** See {@link #dayRateHours}. */
	public String getDayRateHours() {
		return dayRateHours;
	}
	/** See {@link #dayRateHours}. */
	public void setDayRateHours(String dayRateHours) {
		this.dayRateHours = dayRateHours;
	}

	/** See {@link #dayRateCalifornia}. */
	public boolean getDayRateCalifornia() {
		return dayRateCalifornia;
	}
	/** See {@link #dayRateCalifornia}. */
	public void setDayRateCalifornia(boolean dayRateCalifornia) {
		this.dayRateCalifornia = dayRateCalifornia;
	}

	/** See {@link #dayRatePrep}. */
	public boolean getDayRatePrep() {
		return dayRatePrep;
	}
	/** See {@link #dayRatePrep}. */
	public void setDayRatePrep(boolean dayRatePrep) {
		this.dayRatePrep = dayRatePrep;
	}

	/**See {@link #startFormDAO}. */
	protected StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}

	/** See {@link #START_FORM_TYPE_LIST}. */
	public List<SelectItem> getFormTypeDL() {
		return START_FORM_TYPE_LIST;
	}

	/** See {@link #empRates}. */
	public String getEmpRates() {
		if (form.getStartRates() == StartRatesType.RATES_STD) {
			empRates = RATES_STD;
		}
		else if (form.getStartRates() == StartRatesType.RATES_PREP) {
			empRates = RATES_PREP;
		}
		else {
			empRates = RATES_TOUR;
		}
		return empRates;
	}
	/** See {@link #empRates}. */
	public void setEmpRates(String empRates) {
		this.empRates = empRates;
	}

	/** See {@link #useNonUnionOccList}. */
	public boolean getUseNonUnionOccList() {
		return useNonUnionOccList;
	}

	/**
	 * Get a list of states for a country code.
	 * @return A non-empty SelectItem list of states for the specified country.
	 *         If no matching list is found, a minimal list of a blank item and
	 *         the "OT" (other/foreign) entry is returned.
	 */
	public List<SelectItem> getCountryStateCodeSF() {
		List<SelectItem> list;
		String workCountry = null;
		if (form != null && ! StringUtils.isEmpty(form.getWorkCountry())) {
			workCountry = form.getWorkCountry();
		}
		else if (getProduction().getType().isCanadaTalent()) {
			workCountry = Constants.COUNTRY_CODE_CANADA;
		}
		else {
			workCountry = Constants.DEFAULT_COUNTRY_CODE;
		}

		if (workCountry.equals(Constants.DEFAULT_COUNTRY_CODE) && getProduction().getPayrollPref().getIncludeTouring()) {
			list = ApplicationScopeBean.getInstance().getStateCodeHybridDL();
		}
		else {
			list = ApplicationScopeBean.getInstance().getStateCodeDL(workCountry);
		}
		return list;
	}

	/** See {@link #isEmpDeptCast}. */
	public boolean getIsEmpDeptCast() {
		return isEmpDeptCast;
	}
	/** See {@link #isEmpDeptCast}. */
	public void setIsEmpDeptCast(boolean isEmpDeptCast) {
		this.isEmpDeptCast = isEmpDeptCast;
	}

	/** See {@link #isAgentComm}. */
	public boolean getIsAgentComm() {
		return isAgentComm;
	}
	/** See {@link #isAgentComm}. */
	public void setIsAgentComm(boolean isAgentComm) {
		this.isAgentComm = isAgentComm;
	}

	/** See {@link #empDeptName}. */
	public String getEmpDeptName() {
		return empDeptName;
	}
	/** See {@link #empDeptName}. */
	public void setEmpDeptName(String empDeptName) {
		this.empDeptName = empDeptName;
	}

}
