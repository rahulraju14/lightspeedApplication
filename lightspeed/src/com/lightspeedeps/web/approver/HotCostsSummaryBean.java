package com.lightspeedeps.web.approver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.view.View;


/**
 * Contains functionality for the Hot Costs data entry page
 */
@ManagedBean
@ViewScoped
public class HotCostsSummaryBean extends View {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsSummaryBean.class);
	private static final long serialVersionUID = 1L;

	/**
	 * Hot Costs Bean for common functionality used by both HotCostsDataEntryBean and
	 * HotCostsSummaryBean
	 */
	private HotCostsBean hotCostsBean;
	/** List of an individuals daily hot costs line items for a particular week. */
	private List<WeeklyHotCosts> weeklyHotCosts;
	/** List that contains the Hot Costs data line entries for a department */
	private List<WeeklyHotCostsEntry> deptHotCostsList;
	private List<DepartmentHotCostsEntry> deptWeeklyHotCostsList;

	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap;
	/** Whether or expand or collapse individual department tables */
	boolean expandTable;
	/** Department selected by the user */
	private Integer selectedDept;
	/** Whether or not to show departments without employees */
	private Boolean includeDeptsWithoutEmployees;
	/** Week End Date we are showing the summary for. */
	private Date weekEndDate;
	/** The totals amount for Monday for all the WeeklyHotCosts */
	private BigDecimal grandTotalSundayCost;
	/** The totals amount for Monday for all the WeeklyHotCosts */
	private BigDecimal grandTotalMondayCost;
	/** The totals amount for Tuesday for all the WeeklyHotCosts */
	private BigDecimal grandTotalTuesdayCost;
	/** The totals amount for Wednesdqy for all the WeeklyHotCosts */
	private BigDecimal grandTotalWednesdayCost;
	/** The totals amount for Thursday for all the WeeklyHotCosts */
	private BigDecimal grandTotalThursdayCost;
	/** The totals amount for Friday for all the WeeklyHotCosts */
	private BigDecimal grandTotalFridayCost;
	/** The totals amount for Saturday for all the WeeklyHotCosts */
	private BigDecimal grandTotalSaturdayCost;
	/** Total Actual Costs for all the WeeklyHotCosts. */
	private BigDecimal grandTotalActualCost;
	/** Total Budgeted Costs for all the WeeklyHotCosts. */
	private BigDecimal grandTotalBudgetedCost;
	/** Total Actual Hours for all the WeeklyHotCosts. */
	private BigDecimal grandTotalActualHours;
	/** Total Budgeted Hours for all the WeeklyHotCosts. */
	private BigDecimal grandTotalBudgetedHours;
	/** Total pay hours for all the WeeklyHotCosts */
	private BigDecimal grandTotalPayHours;

	/**
	 * Returns the current instance of our bean. Note that this may not be available in a batch
	 * environment, in which case null is returned.
	 */
	public static HotCostsSummaryBean getInstance() {
		HotCostsSummaryBean bean = null;

		bean = (HotCostsSummaryBean) ServiceFinder.findBean("hotCostsSummaryBean");

		return bean;
	}

	// Constructors
	/** Default Constructor */
	public HotCostsSummaryBean() {
		super("");
//		super.setScrollPos("0");
		selectedDept = 0;
		hotCostsBean = HotCostsBean.getInstance();
		hotCostsBean.setScrollPos("0");
		weekEndDate = hotCostsBean.getWeekEndDate();
		includeDeptsWithoutEmployees = false;
//		createDepartmentHotCostsList(); // TODO may need to restore this?
					//  ... trying to avoid overhead since Hot Costs is not in production
		//accumulateGrandTotals();
	}

	public HotCostsSummaryBean(String prefix) {
		super(prefix);

		hotCostsBean = HotCostsBean.getInstance();
		includeDeptsWithoutEmployees = false;

	}

	/** See {@link #deptDailyHotCostsList}. */
	public List<DepartmentHotCostsEntry> getDeptWeeklyHotCostsList() {
		if(deptWeeklyHotCostsList == null) {
			createDepartmentHotCostsList();
		}
		return deptWeeklyHotCostsList;
	}

	/** See {@link #deptWeeklyHotCostsList}. */
	public void setDeptWeeklyHotCostsList(List<DepartmentHotCostsEntry> deptWeeklyHotCostsList) {
		this.deptWeeklyHotCostsList = deptWeeklyHotCostsList;
	}

	/** See {@link #deptHotCostsList}. */
	public void setDeptWeeklyHotCosts(Map<? extends Integer, ? extends DepartmentHotCostsEntry> deptWeeklyHotCostsMap) {
		this.deptWeeklyHotCostsMap.putAll(deptWeeklyHotCostsMap);
	}

	/** See {@link #selectedDept}. */
	public Integer getSelectedDept() {
		return selectedDept;
	}

	/** See {@link #selectedDept}. */
	public void setSelectedDept(Integer selectedDept) {
		this.selectedDept = selectedDept;
	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** Create department list for select box */
	public List<SelectItem> getDepartmentsDL() {
		List<SelectItem> deptList = DepartmentUtils.getDepartmentCastCrewDL();
		deptList.add(0, Constants.SELECT_ALL_IDS);

		return deptList;
	}

	/**
	 * Return list of departments for this production. We will return all of the departments or just
	 * the departments that have members depending the user's choice.
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

	/**
	 * Calculate Total Sunday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalSundayCost() {
		return grandTotalSundayCost;
	}

	/**
	 * Calculate Total Monday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalMondayCost() {
		return grandTotalMondayCost;
	}

	/**
	 * Calculate Total Tuesday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalTuesdayCost() {
		return grandTotalTuesdayCost;
	}

	/**
	 * Calculate Total Wednesday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalWednesdayCost() {
		return grandTotalWednesdayCost;
	}

	/**
	 * Calculate Total Thursday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalThursdayCost() {
		return grandTotalThursdayCost;
	}

	/**
	 * Calculate Total Friday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalFridayCost() {
		return grandTotalFridayCost;
	}

	/**
	 * Calculate Total Saturday Cost for the Summary row
	 */
	public BigDecimal getGrandTotalSaturdayCost() {
		return grandTotalSaturdayCost;
	}

	/**
	 * Calculate Total Hours for the Summary row
	 */
	public BigDecimal getGrandTotalActualHours() {
		return grandTotalActualHours;
	}

	/**
	 * Calculate Total Budgeted Hours for the Summary row
	 */
	public BigDecimal getGrandTotalBudgetedHours() {
		return grandTotalBudgetedHours;
	}

	/** See {@link #grandTotalPayHours}. */
	public BigDecimal getGrandTotalPayHours() {
		return grandTotalPayHours;
	}

	/**
	 * Calculate Total Hours Variance for the Summary row
	 */
	public BigDecimal getGrandTotalHoursVariance() {
		return grandTotalPayHours.subtract(grandTotalBudgetedHours);
	}

	/**
	 * Calculate Total Costs for the Summary row
	 */
	public BigDecimal getGrandTotalActualCost() {
		return grandTotalActualCost;
	}

	/**
	 * Calculate Total Budgeted Costs Variance for the Summary row
	 */
	public BigDecimal getGrandTotalBudgetedCost() {
		return grandTotalBudgetedCost;
	}

	/**
	 * Calculate Total Costs Variance for the Summary row
	 */
	public BigDecimal getGrandTotalCostVariance() {
		return grandTotalActualCost.subtract(grandTotalBudgetedCost);
	}

	/** See {@link #deptHotCostsList}. */
	public List<WeeklyHotCostsEntry> getDeptHotCostsList() {
		createDepartmentHotCostsList();
		List<DepartmentHotCostsEntry>dhcList;
		if (selectedDept == (Integer)Constants.SELECT_ALL_IDS.getValue()) {
			dhcList = new ArrayList<>(deptWeeklyHotCostsMap.values());
		}
		else {
			DepartmentHotCostsEntry dhcEntry = deptWeeklyHotCostsMap.get(selectedDept);
			dhcList = new ArrayList<>();
			dhcList.add(dhcEntry);
		}
		return deptHotCostsList;
	}

	/** See {@link #deptHotCostsList}. */
	public void setDeptHotCosts(List<WeeklyHotCostsEntry> deptHotCostsList) {
		this.deptHotCostsList = deptHotCostsList;
	}

	/** See {@link #weeklyHotCosts}. */
	public List<WeeklyHotCosts> getWeeklyHotCosts() {
		return weeklyHotCosts;
	}

	/** Create the list of Hot Costs by department */
	public void createDepartmentHotCostsList() {
//		List<Department> depts;
		deptWeeklyHotCostsMap = hotCostsBean.getDeptWeeklyHotCostsMap();


		if (selectedDept == (Integer) Constants.SELECT_ALL_IDS.getValue()) {
			deptWeeklyHotCostsList =  new ArrayList<>(deptWeeklyHotCostsMap.values());
		}
		else {
			deptWeeklyHotCostsList = new ArrayList<>();
			deptWeeklyHotCostsList.add(deptWeeklyHotCostsMap.get(selectedDept));
		}


		// Reset the Grand Totals
		grandTotalSundayCost = Constants.DECIMAL_ZERO;
		grandTotalMondayCost = Constants.DECIMAL_ZERO;
		grandTotalTuesdayCost = Constants.DECIMAL_ZERO;
		grandTotalWednesdayCost = Constants.DECIMAL_ZERO;
		grandTotalThursdayCost = Constants.DECIMAL_ZERO;
		grandTotalFridayCost = Constants.DECIMAL_ZERO;
		grandTotalSaturdayCost = Constants.DECIMAL_ZERO;
		grandTotalActualCost = Constants.DECIMAL_ZERO;
		grandTotalBudgetedCost = Constants.DECIMAL_ZERO;
		grandTotalActualHours = Constants.DECIMAL_ZERO;
		grandTotalPayHours = Constants.DECIMAL_ZERO;
		grandTotalBudgetedHours = Constants.DECIMAL_ZERO;

//		for (Department dept : depts) {
//			DepartmentHotCostsEntry deptHcEntry = deptWeeklyHotCostsMap.get(dept.getId());
//			if(deptHcEntry != null) {
//			List<WeeklyHotCostsEntry>whcWrappers = deptHcEntry.getWeeklyHotCostsWrappers();
//			if(whcWrappers != null && ! whcWrappers.isEmpty()) {
//				for(WeeklyHotCostsEntry whcWrapper : whcWrappers) {
//					whcWrapper.getWeeklyHotCosts().getEmployment().getContact();
//				}
//			}
//			}

//			deptWeeklyHotCostsMap.put(dept.getId(), deptHcEntry);
//		}
		accumulateGrandTotals();
	}

	/**
	 * Add up allof the totals to display at the bottom of the page.
	 */
	public void accumulateGrandTotals() {
		for(DepartmentHotCostsEntry dhcEntry : deptWeeklyHotCostsList) {
			for(WeeklyHotCostsEntry whcWrapper : dhcEntry.getWeeklyHotCostsWrappers()) {
				List<DailyHotCost> dailyHotCosts = whcWrapper.getWeeklyHotCosts().getDailyHotCosts();
				for(DailyHotCost dhc : dailyHotCosts) {
					BigDecimal actualCost = dhc.getActualCost();
					BigDecimal budgetedCost = dhc.getBudgetedCost();
					BigDecimal actualHours = dhc.getHours();
					BigDecimal payHours = dhc.getPayHours();
					BigDecimal budgetedHours = dhc.getBudgetedHours();
					BigDecimal budgetedMpvCost = dhc.getBudgetedMpvCost();

					grandTotalActualCost = NumberUtils.safeAdd(grandTotalActualCost, actualCost);
					grandTotalActualHours = NumberUtils.safeAdd(grandTotalActualHours, actualHours);
					grandTotalPayHours = NumberUtils.safeAdd(grandTotalPayHours, payHours);
					grandTotalBudgetedCost = NumberUtils.safeAdd(grandTotalBudgetedCost, budgetedCost);
					grandTotalBudgetedCost = NumberUtils.safeAdd(grandTotalBudgetedCost, budgetedMpvCost);
					grandTotalBudgetedHours = NumberUtils.safeAdd(grandTotalBudgetedHours, budgetedHours);

					switch (dhc.getDayNum()) {
						case HotCostsUtils.SUNDAY_NUM:
							grandTotalSundayCost = NumberUtils.safeAdd(grandTotalSundayCost, actualCost);
							break;
						case HotCostsUtils.MONDAY_NUM:
							grandTotalMondayCost = NumberUtils.safeAdd(grandTotalMondayCost, actualCost);
							break;
						case HotCostsUtils.TUESDAY_NUM:
							grandTotalTuesdayCost = NumberUtils.safeAdd(grandTotalTuesdayCost, actualCost);
							break;
						case HotCostsUtils.WEDNESDAY_NUM:
							grandTotalWednesdayCost = NumberUtils.safeAdd(grandTotalWednesdayCost, actualCost);
							break;
						case HotCostsUtils.THURSDAY_NUM:
							grandTotalThursdayCost = NumberUtils.safeAdd(grandTotalThursdayCost, actualCost);
							break;
						case HotCostsUtils.FRIDAY_NUM:
							grandTotalFridayCost = NumberUtils.safeAdd(grandTotalFridayCost, actualCost);
							break;
						case HotCostsUtils.SATURDAY_NUM:
							grandTotalSaturdayCost = NumberUtils.safeAdd(grandTotalSaturdayCost, actualCost);
							break;
						default:
							break;
					}
				}
			}
		}
	}


	public String actionDataView() {
		hotCostsBean.setShowSummaryView(false);
		expandTable = true;

		View.addButtonClicked();
		return null;
	}

	public String actionPrint() {
		return null;
	}

	// Export Hot Costs to Excel
	public String actionExport() {
		// Build the query to be passed to the jasper report.
		// Department id string
		Production production = hotCostsBean.getProduction();

		String sqlQuery = "select id as deptId, name as deptName from Department where production_id=1 or production_id="
			+ production.getId() + " order by deptName";

		// log.debug("HotCosts export sql = " + sqlQuery);
		Project project = SessionUtils.getCurrentProject();
		ReportUtils.generateHotCostsSummaryView(ReportBean.JASPER_HOTCOSTS_WEEKLY_SUM, sqlQuery, weekEndDate,
				production, project, true, true);

		return null;
	}

	public String actionRefresh() {
		createDepartmentHotCostsList();
		View.addButtonClicked();

		return null;
	}

	public String actionShowAllDepartments() {

		return null;
	}

	public void listenDeptChange(ValueChangeEvent event) {
		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		// Update selectedDept
		selectedDept = (Integer) event.getNewValue();
		createDepartmentHotCostsList();
	}

	/**
	 * Listen for changes in the week end select box and regenerate the hot costs list.
	 *
	 * @param event
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		if (event.getNewValue() != null) {
			weekEndDate = (Date) event.getNewValue();
			hotCostsBean.setWeekEndDate(weekEndDate);
			createDepartmentHotCostsList();
			accumulateGrandTotals();
		}
	}

	public void listenIncludeDeptsWithoutEmployeesChange(ValueChangeEvent event) {
		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		createDepartmentHotCostsList();
	}
}
