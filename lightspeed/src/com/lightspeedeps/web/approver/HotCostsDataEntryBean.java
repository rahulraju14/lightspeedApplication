package com.lightspeedeps.web.approver;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DailyHotCostDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.HotCostsInputDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyHotCostsDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.HotCostsInput;
import com.lightspeedeps.model.PayBreakdown;
import com.lightspeedeps.model.PayBreakdownDaily;
import com.lightspeedeps.model.PayJob;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.service.PayJobService;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.view.View;

/**
 * Contains functionality for the Hot Costs data entry page
 */
@ManagedBean
@ViewScoped
public class HotCostsDataEntryBean extends View {
	// @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsDataEntryBean.class);
	private static final long serialVersionUID = 1L;

	private static final String NON_UNION = "NonU";
	/**
	 * Hot Costs Bean for common functionality used by both
	 * HotCostsDataEntryBean and HotCostsSummaryBean
	 */
	HotCostsBean hotCostsBean;
	/** Current Production */
	private Production prod;
	/** Current Project */
	private Project proj;
	/** Flag to determine whether the user has expanded the department tables */
	private boolean expandTable;
	/** List that contains the Weekly Hot Costs data for a department */
	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap;
	/** List of Department Hot Costs Entry items. */
	private List<DepartmentHotCostsEntry>deptHCEntries;
	/**
	 * List of an individuals daily hot costs line items for a particular week.
	 */
	private List<WeeklyHotCosts> weeklyHotCosts;
	/** List of eligible Employments */
	private Map<Employment, WeeklyHotCosts> eligibleEmployments;
	/** First day that that budgeted values can be copied from. We don't want to let the user
	 * select a date before this date from which to copy budgeted
	 * values from.
	 */
	private Date firstBudgetedValuesDate;
	/** Date from which to copy budgeted values from.*/
	private Date copyBudgetFromDate;
	/** Day that Hot Costs is being processed for. */
	private Date dayDate;
	/** Day of Week num */
	private Byte dayOfWeekNum;
	/**
	 * Week End Date for the data being pulled from the timecards and the week
	 * end date for the hot costs being processed
	 */
	private Date weekEndDate;
	/**
	 * Collection of data from the top of the Data Entry page that can be pushed
	 * down to the individual
	 */
	private HotCostsInput hotCostsInput;
	/**
	 * Whether importing from timecard or production report. Currently only
	 * supporting timecard imports.
	 */
	private String importType;
	/**
	 * Department selected from department select box to show the hot costs for.
	 */
	private Integer selectedDept;
	/** Whether or not to overwrite existing data when doing the import. */
	private boolean overwriteData;
	/** Whether or not to show departments without employees */
	private Boolean includeDeptsWithoutEmployees;
	/**
	 * List of Component values by department. Used to update values for just a
	 * particular department
	 */
	/** Department Off Production value push */
	private Map<Integer, Boolean> deptOffProdCheckBoxValues;
	/** Department Off Production value push */
	private Map<Integer, Boolean> deptNdbCheckBoxValues;
	/** Department NDM value push */
	private Map<Integer, Boolean> deptNdmCheckBoxValues;
	/** Department Camera Wrap value push */
	private Map<Integer, Boolean> deptCamWrapCheckBoxValues;
	/** Department French Hours value push */
	private Map<Integer, Boolean> deptFrenchHoursCheckBoxValues;
	/** Department Forced Call value push */
	private Map<Integer, Boolean> deptForcedCallCheckBoxValues;
	/** Department Grace 1 value push */
	private Map<Integer, BigDecimal> deptGrace1Values;
	/** Department Grace 2 value push */
	private Map<Integer, BigDecimal> deptGrace2Values;
	/** Department MPV 1 value push */
	private Map<Integer, Byte> deptMpv1Values;
	/** Department MPV 2 value push */
	private Map<Integer, Byte> deptMpv2Values;
	/** Department Last Man value push */
	private Map<Integer, BigDecimal> deptLastManInValues;
	/** Department Day Type value push */
	private Map<Integer, DayType> deptDayTypeValues;
	/** Department Work Zone value push */
	private Map<Integer, WorkZone> deptWorkZoneValues;
	/** Department Prod/Epi value push */
	private Map<Integer, String> deptProdEpiValues;
	/** Department Acct Set value push */
	private Map<Integer, String> deptAcctSetValues;
	/** Department Acct Detail value push */
	private Map<Integer, String> deptAcctDetailValues;
	/** Department Call value push */
	private Map<Integer, BigDecimal> deptCallValues;
	/** Department M1 Out value push */
	private Map<Integer, BigDecimal> deptM1OutValues;
	/** Department M1 In value push */
	private Map<Integer, BigDecimal> deptM1InValues;
	/** Department M2 Out value push */
	private Map<Integer, BigDecimal> deptM2OutValues;
	/** Department M2 In value push */
	private Map<Integer, BigDecimal> deptM2InValues;
	/** Department Wrap value push */
	private Map<Integer, BigDecimal> deptWrapValues;
	/** Department Budgeted Hours value push */
	private Map<Integer, BigDecimal> deptBgtdHoursValues;
	/** Department Budgeted Cost value push */
	private Map<Integer, BigDecimal> deptBgtdCostValues;
	/** Department Budgeted MPVs value push */
	private Map<Integer, Byte> deptBgtdMpvValues;
	/** Department Budgeted MPV Cost value push */
	private Map<Integer, BigDecimal> deptBgtdMpvCostValues;

	/** WeeklyHotCostDAO */
	private transient WeeklyHotCostsDAO weeklyHotCostsDAO;

	/** DailyHotCostDAO instance */
	private transient DailyHotCostDAO dailyHotCostsDAO;

	/** Import Action */
	private static final int ACT_IMPORT = 18;
	/** Re-Rate Action */
	private static final int ACT_RERATE = 19;

	// Sections that can be pushed to individual line items.
	/** Daily time entry fields section */
	public static final int ACT_DAILY_TIME_ENTRY_SECTION = 20;
	/** MPV entry fields section. */
	public static final int ACT_MPV_ENTRY_SECTION = 21;
	/** Budgeted Amount fields section. */
	public static final int ACT_BUDGETED_AMOUNTS_ENTRY_SECTION = 22;
	/** Delete Action */
	public static final int ACT_DELETE_HOTCOSTS = 23;
	/** Add Daily Hot Cost action */
	public static final int ACT_ADD_HOTCOSTS = 24;
	/** Action to import budgeted values. */
	public static final int ACT_IMPORT_BUDGETED_VALUES = 30;
	/**/
	public static final int ACT_CLONE_BUDGETED_VALUES = 31;

	/**
	 * Returns the current instance of our bean. Note that this may not be
	 * available in a batch environment, in which case null is returned.
	 */
	public static HotCostsDataEntryBean getInstance() {
		HotCostsDataEntryBean bean = null;

		bean = (HotCostsDataEntryBean)ServiceFinder.findBean("hotCostsDataEntryBean");

		return bean;
	}

	// Constructors
	/** Default Constructor */
	public HotCostsDataEntryBean() {
		super("");
		hotCostsBean = HotCostsBean.getInstance();

		init();
		createDepartmentHotCostsList();
	}

	public HotCostsDataEntryBean(String prefix) {
		super(prefix);
		hotCostsBean = HotCostsBean.getInstance();

		init();
		createDepartmentHotCostsList();
	}

	/**
	 * Initialize the bean
	 */
	private void init() {
		prod = hotCostsBean.getProduction();
		proj = hotCostsBean.getProject();
		expandTable = true;
		dayOfWeekNum = 1;
		includeDeptsWithoutEmployees = false;
		weekEndDate = hotCostsBean.getWeekEndDate();
		selectedDept = (Integer)Constants.SELECT_ALL_IDS.getValue();
		Calendar cal = Calendar.getInstance();
		cal.setTime(weekEndDate);
		cal.set(Calendar.DAY_OF_WEEK, dayOfWeekNum + 1);
		dayDate = cal.getTime();
		// Get the date for the first day of the first pay week.
		// The first day will be the first pay week - producer's day of the week num.
		PayrollPreference payPref;
		if(hotCostsBean.getProduction().getType().isAicp()) {
			payPref = SessionUtils.getCurrentProject().getPayrollPref();
		}
		else {
			payPref = hotCostsBean.getProduction().getPayrollPref();
		}
		cal.setTime(payPref.getFirstPayrollDate());
		cal.set(Calendar.DAY_OF_WEEK, payPref.getFirstWorkDayNum());
		firstBudgetedValuesDate = cal.getTime();
		setDefaultDayOfWeekNum();
		hotCostsBean.setScrollPos("0");
		setScrollable(true);
		restoreScrollFromSession(); // try to maintain scrolled position
	}

	/** See {@link #dayOfWeekNum}. */
	public Byte getDayOfWeekNum() {
		return dayOfWeekNum;
	}

	/** See {@link #dayOfWeekNum}. */
	public void setDayOfWeekNum(Byte dayOfWeekNum) {
		this.dayOfWeekNum = dayOfWeekNum;
	}

	/** See {@link #selectedDept}. */
	public Integer getSelectedDept() {
		return selectedDept;
	}

	/** See {@link #selectedDept}. */
	public void setSelectedDept(Integer selectedDept) {
		this.selectedDept = selectedDept;
	}

	/** See {@link #overwriteData}. */
	public boolean getOverwriteData() {
		return overwriteData;
	}

	/** See {@link #overwriteData}. */
	public void setOverwriteData(boolean overwriteData) {
		this.overwriteData = overwriteData;
	}

	/**
	 * Return list of departments for this production. We will return all of the
	 * departments or just the departments that have members depending the
	 * user's choice.
	 *
	 * @return
	 */
	public List<Department> getDepartments() {
		createDepartmentHotCostsList();
		return DepartmentUtils.getDepartmentList();
	}

	/** See {@link #includeDeptsWithoutEmployees}. */
	public Boolean getIncludeDeptsWithoutEmployees() {
		return includeDeptsWithoutEmployees;
	}

	/** See {@link #includeDeptsWithoutEmployees}. */
	public void setIncludeDeptsWithoutEmployees(Boolean includeDeptsWithoutEmployees) {
		this.includeDeptsWithoutEmployees = includeDeptsWithoutEmployees;
	}

	// Getters and Setters for the Dept level values.
	/** See {@link #deptDayTypeValues}. */
	public Map<Integer, DayType> getDeptDayTypeValues() {
		return deptDayTypeValues;
	}

	/** Does nothing. Allows for listener to use the DayType enum */
	/** See {@link #deptDayTypeValues}. */
	public void setDeptDayTypeValues(Map<Integer, DayType> dayType) {

	}

	/** See {@link #deptCallValues}. */
	public Map<Integer, BigDecimal> getDeptCallValues() {
		return deptCallValues;
	}

	/** See {@link #deptM1OutValues}. */
	public Map<Integer, BigDecimal> getDeptM1OutValues() {
		return deptM1OutValues;
	}

	/** See {@link #deptM1InValues}. */
	public Map<Integer, BigDecimal> getDeptM1InValues() {
		return deptM1InValues;
	}

	/** See {@link #deptM2OutValues}. */
	public Map<Integer, BigDecimal> getDeptM2OutValues() {
		return deptM2OutValues;
	}

	/** See {@link #deptM2InValues}. */
	public Map<Integer, BigDecimal> getDeptM2InValues() {
		return deptM2InValues;
	}

	/** See {@link #deptWrapValues}. */
	public Map<Integer, BigDecimal> getDeptWrapValues() {
		return deptWrapValues;
	}

	/** See {@link #deptWorkZoneValues}. */
	public Map<Integer, WorkZone> getDeptWorkZoneValues() {
		return deptWorkZoneValues;
	}

	/** Does nothing. Allows for listener to use the DayType enum */
	/** See {@link #deptWorkZoneValues}. */
	public void setDeptWorkZoneValues(Map<Integer, WorkZone> workZone) {

	}

	/** See {@link #deptOffProdCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptOffProdCheckBoxValues() {
		return deptOffProdCheckBoxValues;
	}

	/** See {@link #deptNdbCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptNdbCheckBoxValues() {
		return deptNdbCheckBoxValues;
	}

	/** See {@link #deptNdmCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptNdmCheckBoxValues() {
		return deptNdmCheckBoxValues;
	}

	/** See {@link #deptCamWrapCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptCamWrapCheckBoxValues() {
		return deptCamWrapCheckBoxValues;
	}

	/** See {@link #deptFrenchHoursCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptFrenchHoursCheckBoxValues() {
		return deptFrenchHoursCheckBoxValues;
	}

	/** See {@link #deptForcedCallCheckBoxValues}. */
	public Map<Integer, Boolean> getDeptForcedCallCheckBoxValues() {
		return deptForcedCallCheckBoxValues;
	}

	/** See {@link #deptProdEpiValues}. */
	public Map<Integer, String> getDeptProdEpiValues() {
		return deptProdEpiValues;
	}

	/** See {@link #deptAcctSetValues}. */
	public Map<Integer, String> getDeptAcctSetValues() {
		return deptAcctSetValues;
	}

	/** See {@link #deptAcctDetailValues}. */
	public Map<Integer, String> getDeptAcctDetailValues() {
		return deptAcctDetailValues;
	}

	/** See {@link #deptGrace1Values}. */
	public Map<Integer, BigDecimal> getDeptGrace1Values() {
		return deptGrace1Values;
	}

	/** See {@link #deptGrace2Values}. */
	public Map<Integer, BigDecimal> getDeptGrace2Values() {
		return deptGrace2Values;
	}

	/** See {@link #deptMpv1Values}. */
	public Map<Integer, Byte> getDeptMpv1Values() {
		return deptMpv1Values;
	}

	/** See {@link #deptMpv2Values}. */
	public Map<Integer, Byte> getDeptMpv2Values() {
		return deptMpv2Values;
	}

	/** See {@link #deptLastManInValues}. */
	public Map<Integer, BigDecimal> getDeptLastManInValues() {
		return deptLastManInValues;
	}

	/** See {@link #deptBgtdHoursValues}. */
	public Map<Integer, BigDecimal> getDeptBgtdHoursValues() {
		return deptBgtdHoursValues;
	}

	/** See {@link #deptBgtdCostValues}. */
	public Map<Integer, BigDecimal> getDeptBgtdCostValues() {
		return deptBgtdCostValues;
	}

	/** See {@link #deptBgtdMpvValues}. */
	public Map<Integer, Byte> getDeptBgtdMpvValues() {
		return deptBgtdMpvValues;
	}

	/** See {@link #deptBgtdMpvCostValues}. */
	public Map<Integer, BigDecimal> getDeptBgtdMpvCostValues() {
		return deptBgtdMpvCostValues;
	}
	// End of getter and setters for Dept level values

	/** See {@link #expandTable}. */
	public boolean getExpandTable() {
		return expandTable;
	}

	/** See {@link #deptDailyHotCostsList}. */
	public List<DepartmentHotCostsEntry> getDeptWeeklyHotCostsList() {
		if (selectedDept == (Integer)Constants.SELECT_ALL_IDS.getValue()) {
			deptHCEntries = new ArrayList<>(deptWeeklyHotCostsMap.values());
		}
		else {
			DepartmentHotCostsEntry dhcEntry = deptWeeklyHotCostsMap.get(selectedDept);
			deptHCEntries = new ArrayList<>();
			deptHCEntries.add(dhcEntry);
		}
		return deptHCEntries;
	}

	/** See {@link #deptHotCostsList}. */
	public void setDeptWeeklyHotCosts(
			Map<? extends Integer, ? extends DepartmentHotCostsEntry> deptWeeklyHotCostsMap) {
		this.deptWeeklyHotCostsMap.putAll(deptWeeklyHotCostsMap);
	}

	// /** See {@link #deptDailyHotCostsList}. */
	// public List<DepartmentHotCostsEntry> getDeptHotCostsList() {
	// if (deptHotCostsList == null) {
	// createDepartmentHotCostsList();
	// }
	// return deptHotCostsList;
	// }
	//
	// /** See {@link #deptHotCostsList}. */
	// public void setDeptHotCosts(List<DepartmentHotCostsEntry>
	// deptHotCostsList) {
	// this.deptHotCostsList = deptHotCostsList;
	// }

	/** See {@link #dayDate}. */
	public Date getDayDate() {
		return dayDate;
	}

	/** See {@link #dayDate}. */
	public void setDayDate(Date dayDate) {
		this.dayDate = dayDate;
	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #copyBudgetFromDate}. */
	public Date getCopyBudgetFromDate() {
		return copyBudgetFromDate;
	}

	/** See {@link #copyBudgetFromDate}. */
	public void setCopyBudgetFromDate(Date copyBudgetFromDate) {
		this.copyBudgetFromDate = copyBudgetFromDate;
	}

	/** See {@link #hotCostsInput}. */
	public HotCostsInput getHotCostsInput() {
		return hotCostsInput;
	}

	/** See {@link #hotCostsInput}. */
	public void setHotCostsInput(HotCostsInput hotCostsInput) {
		this.hotCostsInput = hotCostsInput;
	}

	/** See {@link #importType}. */
	public void setImportType(String importType) {
		this.importType = importType;
	}

	/** See {@link #weeklyHotCosts}. */
	public List<WeeklyHotCosts> getWeeklyHotCosts() {
		return weeklyHotCosts;
	}

	/**
	 * Set the default day of the week for the hot costs entry page based on the
	 * weekFirstDay from the payroll preferences.
	 */
	public void setDefaultDayOfWeekNum() {
		// Set day of the week to payroll pref first day of the week as the default.
		PayrollPreference payPref;
		if(hotCostsBean.getProduction().getType().isAicp()) {
			payPref = SessionUtils.getCurrentProject().getPayrollPref();
		}
		else {
			payPref = hotCostsBean.getProduction().getPayrollPref();
		}
		dayOfWeekNum = payPref.getFirstWorkDayNum().byteValue();
	}

	private WeeklyHotCostsDAO getWeeklyHotCostsDAO() {
		if(weeklyHotCostsDAO ==  null) {
			weeklyHotCostsDAO = WeeklyHotCostsDAO.getInstance();
		}

		return weeklyHotCostsDAO;
	}


	private DailyHotCostDAO getDailyHotCostDAO() {
		if(dailyHotCostsDAO == null) {
			dailyHotCostsDAO = DailyHotCostDAO.getInstance();
		}

		return dailyHotCostsDAO;
	}
	/**
	 * Create Department list to display
	 */
	public void createDepartmentHotCostsList() {
		deptWeeklyHotCostsMap = HotCostsBean.getInstance().getDeptWeeklyHotCostsMap();

		weekEndDate = hotCostsBean.getWeekEndDate();
		// Initialize the dept checkbox values
		deptOffProdCheckBoxValues = new HashMap<>();
		deptNdbCheckBoxValues = new HashMap<>();
		deptNdmCheckBoxValues = new HashMap<>();
		deptCamWrapCheckBoxValues = new HashMap<>();
		deptFrenchHoursCheckBoxValues = new HashMap<>();
		deptForcedCallCheckBoxValues = new HashMap<>();
		deptGrace1Values = new HashMap<>();
		deptGrace2Values = new HashMap<>();
		deptMpv1Values = new HashMap<>();
		deptMpv2Values = new HashMap<>();
		deptLastManInValues = new HashMap<>();
		deptDayTypeValues = new HashMap<>();
		deptWorkZoneValues = new HashMap<>();
		deptProdEpiValues = new HashMap<>();
		deptAcctSetValues = new HashMap<>();
		deptAcctDetailValues = new HashMap<>();
		deptCallValues = new HashMap<>();
		deptM1OutValues = new HashMap<>();
		deptM1InValues = new HashMap<>();
		deptM2OutValues = new HashMap<>();
		deptM2InValues = new HashMap<>();
		deptWrapValues = new HashMap<>();

		// Budgeted Amounts
		deptBgtdHoursValues = new HashMap<>();
		deptBgtdCostValues = new HashMap<>();
		deptBgtdMpvValues = new HashMap<>();
		deptBgtdMpvCostValues = new HashMap<>();
/*
		if (selectedDept == (Integer)Constants.SELECT_ALL_IDS.getValue()) {
			depts = DepartmentUtils.getDepartmentList();
		}
		else {

			depts = new ArrayList<>();
			depts.add(DepartmentDAO.getInstance().findById(selectedDept));
		}

//		deptWeeklyHotCostsMap = new LinkedHashMap<>();
//
*///		// Retrieve the hot costs input from this date for this production.
		hotCostsInput = HotCostsInputDAO.getInstance().findByProdIdWeekEndDateDayNum(prod,
				weekEndDate, dayOfWeekNum);

		if (hotCostsInput == null) {
			// Object does not exists so create a new one
			hotCostsInput = new HotCostsInput(weekEndDate, dayOfWeekNum, prod.getProdId());
		}
/*
		for (Department dept : depts) {
			int deptId = dept.getId();

			DepartmentHotCostsEntry deptHcEntry = deptWeeklyHotCostsMap.get(deptId);
			if (deptHcEntry != null) {
				DepartmentHotCostsEntry copyDeptHcEntry = new DepartmentHotCostsEntry();

				copyDeptHcEntry.setDept(dept);

				// Only add the WeeklyHotCostsEntry object if its daily hot cost for this
				// day is not mull
//				for (WeeklyHotCosts whc : deptHcEntry.getWeeklyHotCosts()) {
//					DailyHotCost dailyHotCost = whc.getDailyHotCosts().get(dayOfWeekNum - (byte)1);
//					if (dailyHotCost.getDisplay()) {
//						copyDeptHcEntry.getWeeklyHotCosts().add(whc);
//						WeeklyHotCostsEntry whce = new WeeklyHotCostsEntry();
//						whce.setWeeklyHotCosts(whc);
//						whce.getDailyHotCosts().put(dayOfWeekNum, dailyHotCost);
//						whce.setEmployment(whc.getEmployment());
//						copyDeptHcEntry.getWeeklyHotCostsWrappers().add(whce);
//					}
//				}

//				if (! copyDeptHcEntry.getWeeklyHotCostsWrappers().isEmpty()) {
					// Set the default checkbox values for the Department Headers.
					deptOffProdCheckBoxValues.put(dept.getId(), false);
					deptNdbCheckBoxValues.put(dept.getId(), false);
					deptNdmCheckBoxValues.put(dept.getId(), false);
					deptCamWrapCheckBoxValues.put(dept.getId(), false);
					deptFrenchHoursCheckBoxValues.put(dept.getId(), false);
					deptForcedCallCheckBoxValues.put(dept.getId(), false);
					deptGrace1Values.put(dept.getId(), null);
					deptGrace2Values.put(dept.getId(), null);
					deptMpv1Values.put(dept.getId(), null);
					deptMpv2Values.put(dept.getId(), null);
					deptLastManInValues.put(dept.getId(), null);
					deptDayTypeValues.put(dept.getId(), null);
					deptWorkZoneValues.put(dept.getId(), null);
					deptProdEpiValues.put(dept.getId(), null);
					deptAcctSetValues.put(dept.getId(), null);
					deptAcctDetailValues.put(dept.getId(), null);
					deptCallValues.put(dept.getId(), null);
					deptM1OutValues.put(dept.getId(), null);
					deptM1InValues.put(dept.getId(), null);
					deptM2OutValues.put(dept.getId(), null);
					deptM2InValues.put(dept.getId(), null);
					deptWrapValues.put(dept.getId(), null);

					// Budgeted Amounts
					deptBgtdHoursValues.put(dept.getId(), null);
					deptBgtdCostValues.put(dept.getId(), null);
					deptBgtdMpvValues.put(dept.getId(), null);
					deptBgtdMpvCostValues.put(dept.getId(), null);

			}
		}
*/
		// Find the date with the latest budgeted values and apply
		// them to the daily hot costs for this day.
//		loadBudgetedValues(null);

	}


	private void createListOfEmploymentsToAdd() {
		Map<String, Object> sqlParms = new HashMap<>();
		List<Employment>emps;
		eligibleEmployments = new LinkedHashMap<>(0);

		sqlParms.put("project", proj);
		sqlParms.put("production", prod);


		emps = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_OR_NULL_ACTIVE, sqlParms);

		for(Employment emp : emps) {
			// See if this employment record already has hot costs for this date
			// or if they have a timecard for this week ending. If they do not
			// include the employment record in the list of eligible employment records
			// to add.
//			WeeklyTimecard wtc = hotCostsBean.getEmploymentsWithWeeklyTimecards().get(emp.getId());
			WeeklyHotCosts whc = hotCostsBean.getEmploymentsWithWeeklyHotCosts().get(emp.getId());
			if(whc == null ||  whc.getDailyHotCosts().get(dayOfWeekNum - 1).getCreated() == null) {
				if(! emp.getRole().isAdmin() && ! emp.getRole().isDataAdmin() && ! emp.getRole().isFinancialAdmin()) {
					emp = EmploymentDAO.getInstance().refresh(emp);
					eligibleEmployments.put(emp, whc);
				}
			}
		}
	}

	/** Go to the Summary View */
	public String actionSummaryView() {
		hotCostsBean.setShowSummaryView(true);
		expandTable = true;

		View.addButtonClicked();
		return null;
	}

	/** Calculate HTG for the individual line items for just that day */
	public String actionCalcHtg() {

		for(DepartmentHotCostsEntry dhcEntry : deptHCEntries) {
			List<WeeklyHotCostsEntry> whcEntries = dhcEntry.getWeeklyHotCostsWrappers();
			for(WeeklyHotCostsEntry whcEntry : whcEntries) {
				WeeklyHotCosts whc = whcEntry.getWeeklyHotCosts();
				DailyHotCost dailyHc = whc.getDailyHotCosts().get(dayOfWeekNum - 1);

	//				// If there are budgeted hours calculate the budgeted cost
//				if(dailyHc.getBudgetedHours() != null && dailyHc.getBudgetedHours().doubleValue() > 0.0) {
//
//				}
				// Skip if the daily hot costs is missing either the call or
				// wrap times unless the crew member is daily exempt.
				if (dailyHc == null ||
						((dailyHc.getCallTime() == null || dailyHc.getWrap() == null) &&
								! dailyHc.getWorked())) {
					continue;
				}

				// Calculate the actual cost
				calcHtg(whc, dailyHc, dhcEntry.getDept().getId(), false);
			}
		}
		// Save the daily hot cost values including the actual cost values
		saveDailyHotCosts();

		return null;
	}

	/**
	 * Display the Import popup
	 *
	 * @return
	 */
	public String actionImport() {
		overwriteData = false;
		View.addButtonClicked();
		HotCostsUtils.showHotCostsImport(this, ACT_IMPORT, "HotCosts.Import.Title");

		return null;
	}

	/**
	 * Import the timecards and display the HotCostsImportMessage popup
	 *
	 * @return
	 */
	public String actionImportOk() {
		List<Object> importResults;
		String popupTitleId = "HotCosts.Import.Results.Timecard.Title";

		if (importType.equals("PR")) {
			popupTitleId = "HotCosts.Import.Results.PR.Title";
		}
		// Import any weekly timecards and output any messages.
		importResults = HotCostsUtils.importFromTimecards(overwriteData, deptWeeklyHotCostsMap,
				prod, dayOfWeekNum);
		HotCostsUtils.showHotCostsImportMsgs(importResults, popupTitleId);

		// If not in edit mode persist directly to database
		if(!editMode) {
			saveDailyHotCosts();
		}

		return null;
	}

	/**
	 * Currently not used. We only are exporting to excel
	 *
	 * @return
	 */
	public String actionPrint() {
		return null;
	}

	// Export Hot Costs to Excel
	public String actionExport() {
		// Build the query to be passed to the jasper report.
		// Department id string
		String sqlQuery =
				"select id as deptId, name as deptName from Department where production_id=1 or production_id=" +
						prod.getId() + " order by deptName";

		// log.debug("HotCosts export sql = " + sqlQuery);
		Project project = SessionUtils.getCurrentProject();

		ReportUtils.generateHotCostsDaily(ReportBean.JASPER_HOTCOSTS_DAILY, sqlQuery, dayDate,
				weekEndDate, prod, project, true, true);

		return null;
	}

	/**
	 * Setup page for editing
	 *
	 * @return
	 */
	public String actionEditData() {
//		editMode = true;

		View.addButtonClicked();
		return super.actionEdit();
	}

	/**
	 * Show the Re-rate popup
	 *
	 * @param event
	 */
	public void actionReRate(ActionEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");
		// id of daily hot costs data table
		if (dailyHotCosts != null) {
			WeeklyHotCostsEntry wrapper = (WeeklyHotCostsEntry)dailyHotCosts.getRowData();
			WeeklyHotCosts whc = wrapper.getWeeklyHotCosts();
			DailyHotCost dhc = whc.getDailyHotCosts().get(dayOfWeekNum - 1);
			HotCostsReRateBean bean = HotCostsReRateBean.getInstance();
			if (bean.isVisible()) { // probably double-clicked
				log.debug("ignoring double-click");
				return;
			}
			Employment emp = EmploymentDAO.getInstance().refresh(whc.getEmployment());
			StartForm sf = emp.getStartForm();
			whc.setEmployment(emp);
			if(sf == null) {
				return;
			}
			bean.setRateType(sf.getRateType());
			if(dhc.getRate() == null) {
				bean.setReRateAmount(sf.getRate());
			}
			else {
				bean.setReRateAmount(dhc.getRate());
			}

			// See if this is a union employee based on the union key.
			String unionKey = sf.getUnionKey();

			if (unionKey == null || unionKey.equals(NON_UNION)) {
				bean.setIsUnion(false);
			}
			else {
				// Union employee
				bean.setIsUnion(true);
			}

			bean.setUseStandardRate(!dhc.getReRate());
			bean.setUseRerate(dhc.getReRate());
			bean.setEditMode(editMode);
			bean.setDailyHotCost(whc.getDailyHotCosts().get(dayOfWeekNum - 1));
			bean.setWeeklyHotCosts(whc);
			bean.show(this, sf, ACT_RERATE);
		}

		return;
	}

	@Override
	public String actionSave() {
		editMode = false;

		// Save new Hot Costs Input or update existing one.
		if (hotCostsInput.getId() == null) {
			HotCostsInputDAO.getInstance().save(hotCostsInput);
		}
		else {
			HotCostsInputDAO.getInstance().attachDirty(hotCostsInput);
		}

		// Save Hot Costs daily values
		saveDailyHotCosts();

		return null;
	}

	@Override
	public String actionCancel() {
		for(DepartmentHotCostsEntry dhcWrapper : deptHCEntries) {
			List<WeeklyHotCostsEntry>whcWrappers = dhcWrapper.getWeeklyHotCostsWrappers();
			for(WeeklyHotCostsEntry whcWrapper : whcWrappers) {
				WeeklyHotCosts whc = whcWrapper.getWeeklyHotCosts();
				DailyHotCost dhc = whc.getDailyHotCosts().get(dayOfWeekNum -1);
				dhc = getDailyHotCostDAO().refresh(dhc);
				whc.getDailyHotCosts().set(dayOfWeekNum - 1, dhc);
			}
		}
		editMode = false;
		hotCostsInput = HotCostsInputDAO.getInstance().refresh(hotCostsInput);
		// super.actionCancel();

		View.addButtonClicked();
		return null;
	}

	/**
	 * Push the MPV section values to the line items.
	 *
	 * @return
	 */
	public String actionUpdateMPV() {
		if (deptWeeklyHotCostsMap != null && deptWeeklyHotCostsMap.size() > 0) {
			UpdateHotCostsBean bean = UpdateHotCostsBean.getInstance();

			bean.setHotCostsInput(hotCostsInput);
			bean.setDeptWeeklyHotCostsMap(deptWeeklyHotCostsMap);
			bean.setDayOfWeekNum(dayOfWeekNum);
			bean.setSelectAllRecipients(false);
			bean.setRecipients(null);
			bean.setOverwriteData(false);
			bean.setTitle(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Title"));
			bean.setText(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Text"));
			bean.setOverwriteText(
					MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.OverwriteText"));

			HotCostsUtils.showUpdateHotCostsPopup(this, bean, ACT_MPV_ENTRY_SECTION, hotCostsInput,
					deptWeeklyHotCostsMap, dayOfWeekNum);
		}

		View.addButtonClicked();
		return null;
	}

	/**
	 * Update the hour entries from the global push entries to the Daily Hot
	 * Costs entries below. Skip if the fields are empty.
	 *
	 * @return
	 */
	public String actionUpdateDailyTimes() {
		if (deptWeeklyHotCostsMap != null && deptWeeklyHotCostsMap.size() > 0) {
			UpdateHotCostsBean bean = UpdateHotCostsBean.getInstance();

			bean.setHotCostsInput(hotCostsInput);
			bean.setDeptWeeklyHotCostsMap(deptWeeklyHotCostsMap);
			bean.setDayOfWeekNum(dayOfWeekNum);
			bean.setSelectAllRecipients(false);
			bean.setRecipients(null);
			bean.setOverwriteData(false);
			bean.setTitle(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Title"));
			bean.setText(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Text"));
			bean.setOverwriteText(
					MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.OverwriteText"));

			HotCostsUtils.showUpdateHotCostsPopup(this, bean, ACT_DAILY_TIME_ENTRY_SECTION,
					hotCostsInput, deptWeeklyHotCostsMap, dayOfWeekNum);
		}

		View.addButtonClicked();
		return null;
	}

	/**
	 * Update the budgeted values from the global push entries to the Daily Hot
	 * Costs entries below.
	 *
	 * @return
	 */
	public String actionUpdateBudgetedValues() {
		if (deptWeeklyHotCostsMap != null && deptWeeklyHotCostsMap.size() > 0) {
			UpdateHotCostsBean bean = UpdateHotCostsBean.getInstance();

			bean.setHotCostsInput(hotCostsInput);
			bean.setDeptWeeklyHotCostsMap(deptWeeklyHotCostsMap);
			bean.setDayOfWeekNum(dayOfWeekNum);
			bean.setSelectAllRecipients(false);
			bean.setRecipients(null);
			bean.setOverwriteData(false);
			bean.setTitle(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Title"));
			bean.setText(MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.Text"));
			bean.setOverwriteText(
					MsgUtils.getMessage("HotCosts.UpdateBudgetedValues.OverwriteText"));

			HotCostsUtils.showUpdateHotCostsPopup(this, bean, ACT_BUDGETED_AMOUNTS_ENTRY_SECTION,
					hotCostsInput, deptWeeklyHotCostsMap, dayOfWeekNum);
		}

		View.addButtonClicked();
		return null;
	}

	/**
	 * Currently not being used. We are only showing departments with members.
	 * @return
	 */
	public String actionShowAllDepartments() {

		return null;
	}

	/**
	 * Show popup for deleted selected hot costs.
	 *
	 * @return
	 */
	public String actionDeleteDailyHotCosts() {
		if (deptWeeklyHotCostsMap != null && deptWeeklyHotCostsMap.size() > 0) {
			HotCostsUtils.showDeleteHotCostsPopup(this, ACT_DELETE_HOTCOSTS, deptWeeklyHotCostsMap, dayOfWeekNum);
		}

		View.addButtonClicked();

		return null;
	}

	/**
	 * Rebuild the list of Daily Hot Costs to display.
	 * @return
	 */
	public String actionDeleteDailyHotCostsOk() {
		deptWeeklyHotCostsMap = null;
		deptWeeklyHotCostsMap = hotCostsBean.getDeptWeeklyHotCostsMap();
		return null;
	}

	/**
	 *  Show the Add Daily Hot Costs pop up.
	 * @return
	 */
	public String actionAddDailyHotCost() {
		// Create the list of possible people to add.
		createListOfEmploymentsToAdd();
		// Calculate the day that we are adding hot costs for based on the week end date.
		Calendar calendar =	Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		Date endDate = weekEndDate;
		calendar.setTime(endDate);
		calendar.add(Calendar.DAY_OF_YEAR, - (7 - dayOfWeekNum));
		dayDate = calendar.getTime();
		HotCostsUtils.showAddHotCostsPopup(this, ACT_ADD_HOTCOSTS, eligibleEmployments, deptWeeklyHotCostsMap, dayDate, weekEndDate);
		View.addButtonClicked();

		return null;
	}

	/**
	 * User as selected the people to add, now add Daily Hot Costs for those people.
	 * @return
	 */
	public String actionAddDailyHotCostOk() {
		// Create the list of possible people to add.
		hotCostsBean.createDepartmentHotCostsList();
		deptWeeklyHotCostsMap = null;

		return null;
	}

	/**
	 * Show import budgeted values popup
	 * @return
	 */
	public String actionImportBudgetedValues() {
		HotCostsImportBean bean = HotCostsImportBean.getInstance();

		bean.setSelectedImportType(HotCostsImportBean.IMPORT_TYPE_BUDGETED_VALUES);

		HotCostsUtils.showHotCostsImportBudgetedValues(this, ACT_IMPORT_BUDGETED_VALUES, "HotCosts_Import_BudgetedValuesTitle", prod.getProdId());

		View.addButtonClicked();

		return null;
	}

	/**
	 * Process import of budgeted values
	 * @return
	 */
	public String actionImportBudgetedValuesOk() {
		// Fetch the budgeted values for the day selected
		// on the import budgeted values popup.
		loadBudgetedValues(dayDate);
		return null;
	}


	public void actionCloneBudgetedValues() {
		// Set the default date to set the calendar widget for copying the budget from.
		// We will use today's date - 1.
		Calendar cal = Calendar.getInstance();
		cal.setTime(dayDate);
		cal.set(Calendar.DAY_OF_WEEK, cal.get(Calendar.DAY_OF_WEEK) - 1);
		copyBudgetFromDate = cal.getTime();
		HotCostsUtils.showCloneHotCostsBudgetedValuesPopup(this, firstBudgetedValuesDate,  copyBudgetFromDate, ACT_CLONE_BUDGETED_VALUES);
	}


	public void actionCloneBudgetedValuesOk() {
		loadBudgetedValues(copyBudgetFromDate);
	}

	// Listeners

	/**
	 * Listen for changes in the day of the week select box and regenerate the
	 * daily hot costs list.
	 *
	 * @param event
	 */
	public void listenDayOfWeekChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		if (event.getNewValue() != null) {
			dayOfWeekNum = (Byte)event.getNewValue();
			// Get the Hot Cost Global input object for this date from the
			// database.
			// If there isn't one, then create new one.
			hotCostsInput = HotCostsInputDAO.getInstance().findByProdIdWeekEndDateDayNum(prod,
					weekEndDate, dayOfWeekNum);

			if (hotCostsInput == null) {
				// Object does not exists so create a new one
				hotCostsInput = new HotCostsInput(weekEndDate, dayOfWeekNum, prod.getProdId());
			}
			// Calculate the new date for the selected day.
			Calendar cal = Calendar.getInstance();
			cal.setTime(weekEndDate);
			cal.set(Calendar.DAY_OF_WEEK, dayOfWeekNum);
			dayDate = cal.getTime();

			// If there are not daily hot costs associated with this day, pull them from the
			// timecard
//			for(DepartmentHotCostsEntry dhcEntry : deptHCEntries) {
////				Go down to whc in wrapper and check if there are dailyhotcosts for this dayof Week. If not use timecard.
//				for(WeeklyHotCostsEntry wrapper : dhcEntry.getWeeklyHotCostsWrappers()) {
//					// Get the daily hot cost entry for this day and see if it has persisted values.
////					if(wrapper.getWeeklyHotCosts() == null) {
//						WeeklyTimecard wtc = wrapper.getWeeklyTimecard();
//						if(wtc != null) {
//
//						}
////					}
//				}
//			}

//			deptWeeklyHotCostsMap = null;
		}
	}

	/**
	 * Listen for changes in the week end select box and regenerate the hot
	 * costs list.
	 *
	 * @param event
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		if (event.getNewValue() != null) {
			PayrollPreference payPref;
			weekEndDate = (Date)event.getNewValue();
			hotCostsBean.setWeekEndDate(weekEndDate);
			createDepartmentHotCostsList();
			// Set day of the week to payroll pref first day of the week as the default.
			if(hotCostsBean.getProduction().getType().isAicp()) {
				payPref = SessionUtils.getCurrentProject().getPayrollPref();
			}
			else {
				payPref = hotCostsBean.getProduction().getPayrollPref();
			}
			dayOfWeekNum = payPref.getFirstWorkDayNum().byteValue();
		}
	}

	/**
	 * Hot Costs Day date field changes
	 *
	 * @param event
	 */
	public void listenDayChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		if (event.getNewValue() != null) {
			dayDate = (Date)event.getNewValue();
			weekEndDate = TimecardUtils.calculateWeekEndDate(dayDate);

			createDepartmentHotCostsList();
		}
	}

	/**
	 * ValueChangeListener method for all daily hot costs numeric fields.
	 *
	 * @param event contains old and new values
	 */
	public void listenDailyHotCostChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");
		if (dailyHotCosts != null) {
			boolean recalc = true;
			try {
				if (event.getOldValue() != null && event.getNewValue() != null) {
					try {
						double d1 = Double.valueOf(event.getOldValue().toString());
						double d2 = Double.valueOf(event.getNewValue().toString());
						if (d1 == d2) {
							recalc = false;
						}
					}
					catch (Exception e) { // ignore any conversion errors
						 log.error("",e);
					}
				}
				if (recalc) {
					WeeklyHotCostsEntry whce = (WeeklyHotCostsEntry)dailyHotCosts.getRowData();
					DailyHotCost dhc = whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
					HotCostsUtils.updateHours(dhc, true);
				}
			}
			catch (Exception e) {
				log.error("id: " + event.getComponent().getId() + ", old: `" + event.getOldValue() +
						"`, new: `" + event.getNewValue() + "`; recalc: " + recalc);
				EventUtils.logError(e);
				MsgUtils.addGenericErrorMessage();
			}
		}
	}


	public void listenBudgetedHoursChange(ValueChangeEvent event) {
		UIData budgetedValues = (UIData)event.getComponent().findComponent("bgtdValues");
		if (budgetedValues != null) {
			boolean recalc = true;
			try {
				if (event.getOldValue() != null && event.getNewValue() != null) {
					try {
						double d1 = Double.valueOf(event.getOldValue().toString());
						double d2 = Double.valueOf(event.getNewValue().toString());
						if (d1 == d2) {
							recalc = false;
						}
					}
					catch (Exception e) { // ignore any conversion errors
						 log.error("",e);
					}
				}
				if (recalc) {
					WeeklyHotCostsEntry whce = (WeeklyHotCostsEntry)budgetedValues.getRowData();
					DailyHotCost dhc = whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
					HotCostsUtils.updateBudgetedAmount(whce, dhc, (BigDecimal)event.getNewValue());
				}
			}
			catch (Exception e) {
				log.error("id: " + event.getComponent().getId() + ", old: `" + event.getOldValue() +
						"`, new: `" + event.getNewValue() + "`; recalc: " + recalc);
				EventUtils.logError(e);
				MsgUtils.addGenericErrorMessage();
			}
		}
	}

	/**
	 * Listen for Department Day Type changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptDayTypeChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			DayType dayType = (DayType)event.getNewValue();

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptDayTypeValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), dayType);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setDayType(dayType);
			}
		}
	}

	/**
	 * Listen for Department Call Time changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptCallChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptCall");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptCallValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setCallTime(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for Department Meal 1 Out changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptM1OutChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptM1Out");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptM1OutValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setM1Out(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for Department Meal 1 In changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptM1InChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptM1In");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptM1InValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setM1In(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for DepartmentMeal 2 Out changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptM2OutChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptM2Out");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptM2OutValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setM2Out(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for Department Meal 2 In changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptM2InChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptM2In");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptM2InValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setM2In(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for Department Wrap changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptWrapChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptM1In");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptWrapValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				DailyHotCost dhc = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setWrap(value);
				HotCostsUtils.updateHours(dhc, true);
			}
		}
	}

	/**
	 * Listen for Department Work Zone changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptWorkZoneChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			WorkZone value = (WorkZone)event.getNewValue();

			List<WeeklyHotCostsEntry> weeklyHotCostEntries =	(List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptWorkZoneValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setWorkZone(value);
			}
		}
	}

	/**
	 * Listen for Department Off Production changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptOffProdChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean offProd = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptOffProd");

			cb.setValue(offProd);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptOffProdCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), offProd);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setOffProduction(offProd);
			}
		}
	}

	/**
	 * Listen for Department NDB changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptNdbChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean value = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb =
					(HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptNdb");

			cb.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptNdbCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setNonDeductMeal(value);
			}
		}
	}

	/**
	 * Listen for Department NDM changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptNdmChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean value = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptNdm");

			cb.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries =	(List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptNdmCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setNonDeductMeal2(value);
			}
		}
	}

	/**
	 * Listen for Department Camera Wrap changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptCamWrapChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean value = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptCamWrap");

			cb.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptCamWrapCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setCameraWrap(value);
			}
		}
	}

	/**
	 * Listen for Department French Hours changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptFrenchHoursChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean value = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptFrenchHours");

			cb.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptFrenchHoursCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setFrenchHours(value);
			}
		}
	}

	/**
	 * Listen for Department Forced Call changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptForcedCallChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Boolean value = (Boolean)event.getNewValue();
			HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent().findComponent("deptForcedCall");

			cb.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptForcedCallCheckBoxValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setForcedCall(value);
			}
		}
	}

	/**
	 * Listen for Department Grace 1 changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptGrace1Change(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptGrace1");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptGrace1Values.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setGrace1(value);
			}
		}
	}

	/**
	 * Listen for Department Grace 2 changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptGrace2Change(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptGrace2");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptGrace2Values.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setGrace2(value);
			}
		}
	}

	/**
	 * Listen for Department MPV 1 changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptMpv1Change(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Integer value = (Integer)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptMpv1");
			Byte byteValue = null;
			if (value != null) {
				byteValue = value.byteValue();
			}
			comp.setValue(value);
			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptMpv1Values.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), byteValue);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setMpv1Payroll(byteValue);
			}
		}
	}

	/**
	 * Listen for Department Last Man In changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptLastManInChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptLastManIn");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptLastManInValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setLastManIn(value);
			}
		}
	}

	/**
	 * Listen for Department MPV 2 changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptMpv2Change(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			Integer value = (Integer)event.getNewValue();
			// HtmlInputText comp =
			// (HtmlInputText)event.getComponent().findComponent("deptMpv2");
			Byte byteValue = null;

			if (value != null) {
				byteValue = value.byteValue();
			}

			// comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptMpv2Values.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), byteValue);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setMpv2Payroll(byteValue);
			}
		}
	}

	/**
	 * Listen for Department Prod/Epi changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptProdEpiChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			String value = (String)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptProdEpi");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptProdEpiValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setAccountMajor(value);
			}
		}
	}

	/**
	 * Listen for Department Acct Set changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptAcctSetChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			String value = (String)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptAcctSet");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptAcctSetValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setAccountSet(value);
			}
		}
	}

	/**
	 * Listen for Department Acct Detail changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptAcctDetailChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		UIData dailyHotCosts = (UIData)event.getComponent().findComponent("hcDataTable");

		if (dailyHotCosts != null) {
			String value = (String)event.getNewValue();
			HtmlInputText comp =
					(HtmlInputText)event.getComponent().findComponent("deptAcctDetail");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)dailyHotCosts.getValue();
			deptAcctDetailValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whc : weeklyHotCostEntries) {
				whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setAccountDetail(value);
			}
		}
	}

	/**
	 * Listen for Department Budgeted Hours Cost changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptBgtdHoursChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData budgetedHotCosts = (UIData)event.getComponent().findComponent("bgtdValues");

		if (budgetedHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptBgtdHours");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)budgetedHotCosts.getValue();
			deptBgtdHoursValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whce : weeklyHotCostEntries) {
				DailyHotCost dhc = whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);
				dhc.setBudgetedHours(value);
				HotCostsUtils.updateBudgetedAmount(whce, dhc, value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Listen for Department Budgeted Cost changes.
	 *
	 * @param event
	 */
	public void listenDeptBgtdCostChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData budgetedHotCosts = (UIData)event.getComponent().findComponent("bgtdValues");

		if (budgetedHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptBgtdCost");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)budgetedHotCosts.getValue();
			deptBgtdCostValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whce : weeklyHotCostEntries) {
				whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setBudgetedCost(value);
			}
		}
	}

	/**
	 * Listen for Department Budgeted MPV changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptBgtdMpvChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData budgetedHotCosts = (UIData)event.getComponent().findComponent("bgtdValues");

		if (budgetedHotCosts != null) {
			Long val = (Long)event.getNewValue();
			Byte value = new Byte(val.byteValue());
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptBgtdMpv");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)budgetedHotCosts.getValue();
			deptBgtdMpvValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whce : weeklyHotCostEntries) {
				whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setBudgetedMpv(value);
			}
		}
	}

	/**
	 * Listen for Department Budgeted MPV Cost changes.
	 *
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	public void listenDeptBgtdMpvCostChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData budgetedHotCosts = (UIData)event.getComponent().findComponent("bgtdValues");

		if (budgetedHotCosts != null) {
			BigDecimal value = (BigDecimal)event.getNewValue();
			HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("deptBgtdMpvCost");

			comp.setValue(value);

			List<WeeklyHotCostsEntry> weeklyHotCostEntries = (List<WeeklyHotCostsEntry>)budgetedHotCosts.getValue();
			deptBgtdMpvCostValues.put(weeklyHotCostEntries.get(0).getWeeklyHotCosts().getDepartmentId(), value);

			for (WeeklyHotCostsEntry whce : weeklyHotCostEntries) {
				whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1).setBudgetedMpvCost(value);
			}
		}
	}

	/**
	 * Listen for Department select box changes
	 *
	 * @param event
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		// Update selectedDept
		selectedDept = (Integer)event.getNewValue();
		createDepartmentHotCostsList();
	}

	/**
	 * Listens for change of the display departments without members checkbox.
	 *
	 * @param event
	 */
	public void listenIncludeDeptsWithoutEmployeesChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are
			// called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		Boolean showDepts = (Boolean)event.getNewValue();
		if (showDepts) {
			createDepartmentHotCostsList();
		}
		else {
			removeDeptsWithoutEmployees();
		}
	}

	@Override
	/**
	 * Set by the popup holder to perform the Ok tasks
	 */
	public String confirmOk(int action) {
		switch (action) {
			case ACT_IMPORT:
				actionImportOk();
				break;
			case ACT_DELETE_HOTCOSTS:
				actionDeleteDailyHotCostsOk();
				break;
			case ACT_ADD_HOTCOSTS:
				actionAddDailyHotCostOk();
				break;
			case ACT_IMPORT_BUDGETED_VALUES:
				actionImportBudgetedValuesOk();
				break;
			case  ACT_CLONE_BUDGETED_VALUES:
				actionCloneBudgetedValuesOk();
				break;
		}
		return null;
	}

	/**
	 * If the user unchecks the "Show Departments without Employees, remove the
	 * departments from our collection.
	 */
	private void removeDeptsWithoutEmployees() {
		for (int index = deptWeeklyHotCostsMap.size(); index > 0; index--) {
			DepartmentHotCostsEntry entry = deptWeeklyHotCostsMap.get(index - 1);

			if (entry.getWeeklyHotCostsWrappers().isEmpty()) {
				deptWeeklyHotCostsMap.remove(entry);
			}
		}
	}

	/**
	 * Save the daily hot cost values. This is also called after calculating HTG
	 */
	private void saveDailyHotCosts() {
		if(deptHCEntries != null && !deptHCEntries.isEmpty()) {
			for(DepartmentHotCostsEntry deptEntry : deptHCEntries) {
				List<WeeklyHotCostsEntry> whcEntries = deptEntry.getWeeklyHotCostsWrappers();
				for (WeeklyHotCostsEntry whcWrapper : whcEntries) {
					WeeklyHotCosts whc = whcWrapper.getWeeklyHotCosts();
					DailyHotCost dhc = whc.getDailyHotCosts().get(dayOfWeekNum - 1);
					// If whc does not have an id save it otherwise update
					// the existing whc.
					if (whc.getId() == null) {
						// Get the selected daily hot cost and set the created and created by fields

						dhc.setCreated(new Date());
						dhc.setCreatedBy(getvUser().getAccountNumber());
						getWeeklyHotCostsDAO().save(whc);
					}
					else {
						// Get the selected daily hot cost and set the updated and updated by fields
						dhc.setUpdated(new Date());
						dhc.setUpdatedBy(getvUser().getAccountNumber());
						getDailyHotCostDAO().attachDirty(dhc);
						whc = WeeklyHotCostsDAO.getInstance().refresh(whc);
					}
				}
			}
		}
	}

	/**
	 * Load the budgeted values from the date selected by the user. If
	 * dateToLoadFrom is null, then find the last day that has budgeted
	 * values and use those.
	 * @param dateToLoadFrom
	 */
	private void loadBudgetedValues(Date dateToLoadFrom) {
		// Load from the date passed in.
		String query;
		Date loadFromWeekEnd;

		if(dateToLoadFrom != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dateToLoadFrom);
			CalendarUtils.setEndOfWeek(cal);

			Calendar budgetDate = Calendar.getInstance();
			budgetDate.setTime(dateToLoadFrom);
			Integer budgetDayNum = budgetDate.get(Calendar.DAY_OF_WEEK);
			// Get the weekly hot costs for the current week ending.
			loadFromWeekEnd = TimecardUtils.calculateWeekEndDate(dateToLoadFrom);
			loadFromWeekEnd = CalendarUtils.parseDate(CalendarUtils.dateFormatYYYYMMDD(loadFromWeekEnd), "yyyy-MM-dd");
			for(DepartmentHotCostsEntry dhc: deptHCEntries) {
				for(WeeklyHotCostsEntry whcEntry: dhc.getWeeklyHotCostsWrappers()) {
					query = " from WeeklyHotCosts whc where whc.endDate=? and whc.userAccount = ?";
					WeeklyHotCosts whc = whcEntry.getWeeklyHotCosts();
					Object [] values = { loadFromWeekEnd, whc.getUserAccount() };
					WeeklyHotCosts budgetedWhc = WeeklyHotCostsDAO.getInstance().findOne(query, values);


					if(budgetedWhc != null) {
						// Initialize the list.
						budgetedWhc.getDailyHotCosts().size();

						DailyHotCost budgetedDailyHotCost = budgetedWhc.getDailyHotCosts().get(budgetDayNum - 1);
						DailyHotCost dailyHotCost = whc.getDailyHotCosts().get(dayOfWeekNum - 1);

						dailyHotCost.setBudgetedHours(budgetedDailyHotCost.getBudgetedHours());
						dailyHotCost.setBudgetedCost(budgetedDailyHotCost.getBudgetedCost());
						dailyHotCost.setBudgetedMpv(budgetedDailyHotCost.getBudgetedMpv());
						dailyHotCost.setBudgetedMpvCost(budgetedDailyHotCost.getBudgetedMpvCost());

						// If this is done outside of edit mode, save it directly
						if(!editMode) {
							saveDailyHotCosts();
						}
					}
				}
			}
		}
	}

	/**
	 * Calculate HTG for actual or budgeted amounts.
	 * @param whc
	 * @param dhc
	 * @param deptId
	 * @param calcBudgetedAmount - Flag to determine whether we are calculating for budgeted or actual values.
	 */
	private void calcHtg(WeeklyHotCosts whc, DailyHotCost dailyHc, Integer deptId, boolean calcBudgetedAmount) {
		// See if there is a weekly timecard for this person. If
		// yes, create a
		// deep copy. If not, create a blank timecard.
		Employment emp = whc.getEmployment();
		emp = EmploymentDAO.getInstance().refresh(emp);
		User user = emp.getContact().getUser();
		Integer departmentId = deptId;
		WeeklyTimecard wtc = null;
		StartForm sf = emp.getStartForm();
		String userAcct = user.getAccountNumber();

		if (sf != null) {
			// User has start form so may have a weekly timecard
			WeeklyTimecard origWtc;

			sf = StartFormDAO.getInstance().refresh(sf);
			origWtc = WeeklyTimecardDAO.getInstance()
					.findByWeekEndDateAccountDeptStartForm(prod, null, weekEndDate,
							false, userAcct, departmentId, sf, null);
			if (origWtc != null) {
				wtc = origWtc.deepCopy();
				wtc.setStartForm(sf);
				wtc.setPayJobs(new ArrayList<PayJob>());
				if (wtc.getPayJobs().isEmpty()) {
					PayJob pj = TimecardUtils.createPayJob(wtc, (byte)0);
					wtc.getPayJobs().add(pj);
				}

				wtc.setPayDailyLines(new ArrayList<PayBreakdownDaily>());
				wtc.setPayLines(new ArrayList<PayBreakdown>());

				// Don't use the daily times for this timecard. Set
				// the collection
				// to empty Daily Times
				Calendar calendar =
						Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
				Date endDate = wtc.getEndDate();
				calendar.setTime(endDate);
				calendar.add(Calendar.DAY_OF_YEAR, - 6);

				List<DailyTime> dailyTimes = new ArrayList<>();
				for (byte i = 1; i < 8; i++) {
					DailyTime dt = new DailyTime(wtc, i);
					dt.setDate(calendar.getTime()); // Set a day for
													// a week
					dt.setState(wtc.getWageState());
					dailyTimes.add(dt);
					calendar.add(Calendar.DAY_OF_YEAR, 1);
				}
				wtc.setDailyTimes(dailyTimes); // Add
												// List<DailyTime>
												// to weeklyTimecard
			}
		}
		else {
			sf = new StartForm();
		}

		if (wtc == null) {
			wtc = TimecardUtils.createBlankTimecard(weekEndDate);
			wtc.setUserAccount(userAcct);
			wtc.setDepartmentId(departmentId);
			wtc.setProdId(prod.getProdId());
			wtc.setStartForm(sf);

			wtc.setAllowWorked(whc.getExemptEmployee());

			PayJob pj = TimecardUtils.createPayJob(wtc, (byte)0);
			wtc.getPayJobs().add(pj);

			TimecardUtils.updateTimecardFromStart(wtc, prod);
		}

		// If the Weekly Hot Costs object has never been persisted,
		// there will be no Daily Hot Costs
		// entries
		List<DailyHotCost> dailyHotCostsList = whc.getDailyHotCosts();

		// See if this Daily Hot Cost is in the dailyHotCosts List.
		// If so, replace the entry
		// that is there. If not, then add the Daily HotCosts to the
		// List.
		boolean addToList = true;

		int index = 0;
		for (DailyHotCost dailyHotCost : dailyHotCostsList) {
			if (dailyHotCost.getDayNum() == dayOfWeekNum) {
				dailyHotCostsList.set(index, dailyHc);
				addToList = false;
				break;
			}

			index++;
		}

		if (addToList) {
			dailyHotCostsList.add(dailyHc);
		}

		if (dailyHotCostsList.size() > 0) {
			List<DailyTime> dailyTimes = wtc.getDailyTimes();
			for (DailyHotCost dailyHotCost : dailyHotCostsList) {
				byte dayNum = dailyHotCost.getDayNum();
				DailyTime dt = dailyTimes.get(dayNum - 1);

				HotCostsUtils.copyFromDailyHotCostsToDailyTime(dailyHotCost, dt, true, calcBudgetedAmount);
				BigDecimal hours;

				hours = dt.getHours();

				if (hours == null || hours.doubleValue() == 0) {
					TimecardCalc.calculateDailyHours(dt);
				}
			}
		}

		boolean reRate = false;

		if (! calcBudgetedAmount) {
			reRate = dailyHc.getReRate();
		}

		// Do not use re-rate value when calculating budgeted amount
		if (reRate) {
			// This is a re-rate so set the rate in the payjob
			wtc.getPayJobs().get(0).setRate(dailyHc.getRate());
		}

		TimecardService.calculateAllHtg(wtc, true, reRate);

		// Get the actual cost for this day from the pay daily
		// breakdown
		BigDecimal calculatedCost = new BigDecimal(0.0);


			// Get the amount for this particular daily hot cost date.
			List<PayBreakdownDaily>payDailyLines = wtc.getPayDailyLines();

			Calendar cal = Calendar.getInstance();
			cal.setTime(whc.getDailyHotCosts().get(dayOfWeekNum - 1).getDate());
			for (PayBreakdownDaily pd : payDailyLines) {
				Calendar cal2 = Calendar.getInstance();

				String compareDate = CalendarUtils.formatDate(pd.getDate(), "yyyy-MM-dd");
				SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateInstance();
				try {
					sdf.applyPattern("yyyy-MM-dd");
					cal2.setTime(sdf.parse(compareDate));
				}
				catch (ParseException e) {
					EventUtils.logError(e);
					MsgUtils.addGenericErrorMessage();
				}

				if (cal.compareTo(cal2) == 0) {
					if (pd.getCategoryType() != PayCategory.MEET_GUARANTEE) {
						calculatedCost = NumberUtils.safeAdd(calculatedCost, pd.getTotal());
					}
				}
			}

		if (sf.getRateType() == EmployeeRateType.DAILY) {
			if(wtc.getDailyRate() != null) {
				calculatedCost = NumberUtils.safeAdd(calculatedCost, wtc.getDailyRate());
			}
		}
		else if (sf.getRateType() == EmployeeRateType.WEEKLY) {
			if(wtc.getWeeklyRate() != null) {
				BigDecimal dailyPercentage = Constants.DECIMAL_POINT_TWO;

				// If the weekly timecard is not null, get the studioOrLoc value.
				// If not, use the Start Form.
				if((wtc != null && wtc.isDistantRate()) || sf.isDistantRate()) {
						dailyPercentage = new BigDecimal(.1667);
				}

				MathContext mt = new MathContext(6, RoundingMode.HALF_UP);
				calculatedCost = calculatedCost.add(wtc.getWeeklyRate().multiply(dailyPercentage, mt));
			}
		}

		if(calcBudgetedAmount) {
			dailyHc.setBudgetedCost(NumberUtils.scaleTo2Places(calculatedCost));
		}
		else {
			dailyHc.setActualCost(NumberUtils.scaleTo2Places(calculatedCost));
		}

		for (DailyHotCost dhc : whc.getDailyHotCosts()) {
			DailyTime dt = wtc.getDailyTimes().get(dhc.getDayNum() - 1);
			HotCostsUtils.copyFromDailyTimeToDailyHotCosts(dt, dhc, true, calcBudgetedAmount);
		}

		// Calculate the pay hours.
		if(wtc != null) {
			dailyHc.setPayHours(PayJobService.calculatePayHours(wtc.getPayJobs(), dayOfWeekNum - 1));
		}
	}

}
