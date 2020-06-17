package com.lightspeedeps.web.approver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.ControlHolder;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.TriStateCheckboxExt;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.util.EnumList;

/**
 * Contains common functionality between the HotCosts Summary page and the Hot
 * Costs Data Entry page
 */

@ManagedBean
@ViewScoped
public class HotCostsBean extends ApproverFilterBase implements ControlHolder {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsBean.class);
	private static final long serialVersionUID = -1808249900735392255L;

	/** Current Production */
	private Production prod;
	/** Current Project */
	private Project proj;
	/** Whether we are viewing the Summary dashboard or the Data Entry page. */
	private boolean showSummaryView;

	/** List of Departments available to the current user. */
	private List<SelectItem> departmentsDL;
	/**
	 * Week End Date for the data being pulled from the timecards and the week
	 * end date for the hot costs being processed
	 */
	private Date weekEndDate;
	/** List that contains the Hot Costs data line entries for a department for the Summary view*/
	private List<DepartmentHotCostsEntry> deptSummaryWeeklyHotCostsList;
	/** Map of Employments With Weekly Hot Costs for the selected week end date */
	private Map<Integer, WeeklyHotCosts>employmentsWithWeeklyHotCosts;
	/** List of Employments With Weekly Timecards for the selected week end date */
	private Map<Integer, WeeklyTimecard> employmentsWithWeeklyTimecards;
	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap;
	/**
	 * Returns the current instance of our bean. Note that this may not be available in a batch
	 * environment, in which case null is returned.
	 */
	public static HotCostsBean getInstance() {
		HotCostsBean bean = null;

		bean = (HotCostsBean) ServiceFinder.findBean("hotCostsBean");

		return bean;
	}

	public HotCostsBean() {
		super("");

		getFilterBean().register(this, FilterBean.TAB_HOT_COSTS);
		prod = SessionUtils.getProduction();
		proj = SessionUtils.getCurrentProject();
		showSummaryView = true;

		Calendar cal = Calendar.getInstance();
		CalendarUtils.setEndOfWeek(cal);
		String compareDate = CalendarUtils.formatDate(cal.getTime(), "yyyy-MM-dd");
		SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
		try {
			sdf.applyPattern("yyyy-MM-dd");
			cal.setTime(sdf.parse(compareDate));
		}
		catch (ParseException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		weekEndDate = cal.getTime();
//		createDepartmentHotCostsList();
		super.setScrollPos("0");
		super.setScrollable(true);
	}

	/** See {@link #prod}. */
	@Override
	public Production getProduction() {
		return prod;
	}

	/** See {@link #proj}. */
	@Override
	public Project getProject() {
		return proj;
	}

	/** See {@link #deptSummaryWeeklyHotCostsList}. */
	public List<DepartmentHotCostsEntry> getDeptSummaryWeeklyHotCostsList() {
		if(deptSummaryWeeklyHotCostsList == null) {
			deptSummaryWeeklyHotCostsList = new ArrayList<>();
		}
		return deptSummaryWeeklyHotCostsList;
	}

	/** See {@link #deptSummaryWeeklyHotCostsList}. */
	public void setDeptSummaryWeeklyHotCostsList(List<DepartmentHotCostsEntry> deptSummaryWeeklyHotCostsList) {
		this.deptSummaryWeeklyHotCostsList = deptSummaryWeeklyHotCostsList;
	}

	/** Get list of day types for the daily entry select boxes. */
	public List<SelectItem> getDayTypeDL() {
		return EnumList.getDayTypeList();
	}

	/** Get list of work zones for the daily entry select boxes. */
	public List<SelectItem> getWorkZoneDL() {
		return EnumList.getWorkZoneList();
	}

	/** List of Days of the Week */
	public List<SelectItem> getDaysOfWeek() {
		List<SelectItem> daysOfWeek = new ArrayList<>();

		daysOfWeek.add(new SelectItem(1, "Sunday"));
		daysOfWeek.add(new SelectItem(2, "Monday"));
		daysOfWeek.add(new SelectItem(3, "Tuesday"));
		daysOfWeek.add(new SelectItem(4, "Wednesday"));
		daysOfWeek.add(new SelectItem(5, "Thursday"));
		daysOfWeek.add(new SelectItem(6, "Friday"));
		daysOfWeek.add(new SelectItem(7, "Saturday"));

		return daysOfWeek;
	}

	/** See {@link #departmentsDL}. */
	public List<SelectItem> getDepartmentsDL() {
		if (departmentsDL == null) {
			departmentsDL = DepartmentUtils.getDepartmentCastCrewDL();
			departmentsDL.add(0, Constants.SELECT_ALL_IDS);
		}
		return departmentsDL;
	}

	/** See {@link #showSummaryView}. */
	public boolean getShowSummaryView() {
		return showSummaryView;
	}
	/** See {@link #showSummaryView}. */
	public void setShowSummaryView(boolean showSummaryView) {
		this.showSummaryView = showSummaryView;
	}

	/** See {@link #weekEndDate}. */
	@Override
	public Date getWeekEndDate() {
		return weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	@Override
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #deptWeeklyHotCostsMap}. */
	public Map<Integer, DepartmentHotCostsEntry> getDeptWeeklyHotCostsMap() {
		if (deptWeeklyHotCostsMap == null) {
			createDepartmentHotCostsList();
		}
		return deptWeeklyHotCostsMap;
	}

   /** See {@link #employmentsWithWeeklyHotCosts}. */
	public Map<Integer, WeeklyHotCosts> getEmploymentsWithWeeklyHotCosts() {
		return employmentsWithWeeklyHotCosts;
	}


	/** See {@link #employmentsWithWeeklyTimecards}. */
	public Map<Integer, WeeklyTimecard> getEmploymentsWithWeeklyTimecards() {
		return employmentsWithWeeklyTimecards;
	}

	/**
	 * Listener for Week End change
	 * @param event
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		weekEndDate = (Date) event.getNewValue();
		if(weekEndDate != null) {
			createDepartmentHotCostsList();
			// Rebuild the Summary and Data Entry lists.
			HotCostsSummaryBean.getInstance().setDeptWeeklyHotCostsList(null);
			HotCostsSummaryBean.getInstance().createDepartmentHotCostsList();
			HotCostsDataEntryBean.getInstance().createDepartmentHotCostsList();
			HotCostsDataEntryBean.getInstance().setDefaultDayOfWeekNum();
			//HotCostsSummaryBean.getInstance().accumulateGrandTotals();
		}

	}

	/**
     * Create Department list to display
     */
    public void createDepartmentHotCostsList() {
    	deptWeeklyHotCostsMap = HotCostsUtils.createDepartmentHotCostsMap(production, project, weekEndDate, false);
//    	List<Department> depts;

//		depts = DepartmentUtils.getDepartmentList();

//		deptWeeklyHotCostsMap = new LinkedHashMap<Integer, DepartmentHotCostsEntry>();
//		boolean includeDeptsWithoutEmployees = false;
//
//		// Get all the Weekly HotCosts instances keyed by the employment record id.
//		employmentsWithWeeklyHotCosts = createEmploymentsWithWeeklyHotCosts();
//		// Get all employments with weekly time cards for the selected week end date.
//		employmentsWithWeeklyTimecards = createEmploymentsWithWeeklyTimecards();
//		// Get all the employment records for each department
//		Map<Integer, List<Employment>> deptEmps = createEmploymentsByDepartment();
//
//		for (Department dept : depts) {
//			int deptId = dept.getId();
//			if (deptId == Constants.DEPARTMENT_ID_DATA_ADMIN || deptId == Constants.DEPARTMENT_ID_LS_ADMIN) {
//				// Skip the LS Admin departments
//				continue;
//			}
//
//			DepartmentHotCostsEntry deptHcEntry = new DepartmentHotCostsEntry();
//
//			// Get the WeeklyHotCosts for this department. If none then continue
//			// to the next department.
//			deptHcEntry.setDept(dept);
//			deptHcEntry.setExpand(true);
//			deptHcEntry.setSummaryExpand(true);
//
//			List<Employment>emps = deptEmps.get(deptId);
//			if (emps != null && emps.size() > 0) {
//				// See if they already have a WeeklyHotCost object attached to
//				// this employment record and week end date.
//				for (Employment emp : emps) {
//					if (emp.getRole().isAdmin() || emp.getRole().isDataAdmin() || emp.getRole().isFinancialAdmin()) {
//						continue;
//					}
//
//					StartForm sf = new StartForm();
//					StartForm existingSf = emp.getStartForm();
//
//					if (existingSf == null) {
//						sf.setEmployment(emp);
//						sf.setContact(emp.getContact());
//						sf.setProd(null);
//					}
//					else {
//						sf = existingSf.deepCopy();
//					}
//
//					// Get the Weekly HotCosts for this employment record.
//					WeeklyHotCosts whc = employmentsWithWeeklyHotCosts.get(emp.getId());
//
//					if(whc == null && ! employmentsWithWeeklyTimecards.containsKey(emp.getId())){
//						// This employment record does not have a weekly timecard or
//						// and existing weekly hot costs object so skip it.
//						continue;
//					}
//
//					if (whc == null) {
//						whc = HotCostsUtils.createNewWeeklyHotCosts(emp, weekEndDate, sf);
//					}
//					else {
//						whc.getDailyHotCosts().size();
//						whc.setEmployment(emp);
//						whc.setStartFormId(sf.getId());
//						whc.setStartForm(sf);
//					}
//					if (whc != null) {
//						whc.getEmployment().getContact();
//						deptHcEntry.getWeeklyHotCosts().add(whc);
//					}
////					// Add the display collection
////					WeeklyHotCostsEntry weeklyHotCostsEntry = new WeeklyHotCostsEntry();
////					weeklyHotCostsEntry.setEmployment(emp);
////					weeklyHotCostsEntry.setWeeklyHotCosts(whc);
////
////					Map<Byte, DailyHotCost> dhcs = new HashMap<>();
////					for(DailyHotCost dhc : whc.getDailyHotCosts()) {
////						if(dhc.getDisplay()) {
////							dhcs.put(dhc.getDayNum(), dhc);
////						}
////					}
////					if(! dhcs.isEmpty()) {
////						// We have Daily Hot Cost items to display so add to
////						// the department list.
////						weeklyHotCostsEntry.setDailyHotCosts(dhcs);
////						deptHcEntry.getWeeklyHotCostsWrappers().add(weeklyHotCostsEntry);
//////						deptWeeklyHotCostsMap.put(dept.getId(), deptHcEntry);
////					}
//				}
//			}
//			else {
//				if (! includeDeptsWithoutEmployees) {
//					// Do not show departments that do not have employees
//					continue;
//				}
//			}
//
//			deptWeeklyHotCostsMap.put(dept.getId(), deptHcEntry);
//		}
	}

//	/**
//	 * Create map up employees for each department.
//	 *
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	private Map<Integer, List<Employment>> createEmploymentsByDepartment() {
//		Map<Integer, List<Employment>> deptEmps = new HashMap<>();
//		List<Employment> emps = new ArrayList<>();
//		List<Employment> empsByDept = new ArrayList<>();
//		List<Object> sqlParams = new ArrayList<>();
//
//		String sqlQuery = "select emp from Employment emp, Department dept, Contact ct, Role rl where rl.id = emp.role.id and dept.id = rl.department.id and ";
//		sqlQuery += "ct.id = emp.contact.id and ct.production=? and (emp.project = ? or emp.project is null) order by dept.name";
//		sqlParams.add(prod);
//		sqlParams.add(proj);
//
//		emps = EmploymentDAO.getInstance().find(sqlQuery, sqlParams.toArray());
//
//		Integer currDeptId, newDeptId;
//		Iterator<Employment> empItr = emps.iterator();
//		currDeptId = emps.get(0).getDepartmentId();
//		newDeptId = currDeptId;
//
//		while (empItr.hasNext()) {
//			Employment emp = empItr.next();
//
//			newDeptId = emp.getDepartmentId();
//			if (currDeptId != newDeptId) {
//				deptEmps.put(currDeptId, empsByDept);
//				empsByDept = new ArrayList<>();
//				currDeptId = newDeptId;
//			}
//			empsByDept.add(emp);
//		}
//		// Add the last entry in.
//		if(!empsByDept.isEmpty()) {
//			deptEmps.put(currDeptId, empsByDept);
//		}
//
//		return deptEmps;
//	}
//
//	private Map<Integer, WeeklyTimecard> createEmploymentsWithWeeklyTimecards() {
//		Map<Integer, WeeklyTimecard> employmentsWithTimecards = new LinkedHashMap<>(0);
//		List<WeeklyTimecard> wtcs = WeeklyTimecardDAO.getInstance().findByWeekEndDate(production, null, weekEndDate, null);
//
//		for(WeeklyTimecard wtc : wtcs) {
//			employmentsWithTimecards.put(wtc.getStartForm().getEmployment().getId(), wtc);
//		}
//		return employmentsWithTimecards;
//	}
//
//	private Map<Integer, WeeklyHotCosts> createEmploymentsWithWeeklyHotCosts() {
//		Map<Integer, WeeklyHotCosts> empWhcs = new HashMap<>();
//		List<WeeklyHotCosts> whcs;
//		whcs = WeeklyHotCostsDAO.getInstance().findByProdProjWeekEndDate(prod, SessionUtils.getCurrentProject(), weekEndDate,
//				"department_id");
//
//		if (!whcs.isEmpty()) {
//			Iterator<WeeklyHotCosts> whcsItr = whcs.iterator();
//
//			while (whcsItr.hasNext()) {
//				WeeklyHotCosts whc = whcsItr.next();
//
//				// Get the Employment Record associated with this weekly hot
//				// cost.
//				Employment emp = EmploymentDAO.getInstance().findById(whc.getEmploymentId());
//				if(emp != null) {
//					// This could happen if a person has hot costs record and the
//					// Employment Record associated with the hot costs has been deleted.
//					whc.setEmployment(emp);
//
//					empWhcs.put(emp.getId(), whc);
//				}
//			}
//		}
//		return empWhcs;
//	}

    /**
	 * @see com.lightspeedeps.web.approver.FilterHolder#changeTab(int, int)
	 */
	@Override
	public void changeTab(int priorTab, int currentTab) {
		// TODO Auto-generated method stub
	}

	/**
	 * @see com.lightspeedeps.object.ControlHolder#clicked(javax.faces.event.ActionEvent, java.lang.Object)
	 */
	@Override
	public void clicked(TriStateCheckboxExt checkBox, Object id) {
		// TODO Auto-generated method stub
	}

}
