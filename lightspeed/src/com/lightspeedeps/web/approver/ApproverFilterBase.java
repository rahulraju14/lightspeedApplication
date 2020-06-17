package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.ApprovableStatusFilter;
import com.lightspeedeps.type.FilterType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.EnumList;

/**
 * This is a superclass for backing beans for the Approver DashBoard pages. This
 * class extends the ApproverBase by adding support for the FilterBean (which
 * manages the drop-down controls). It has delegate methods for the various
 * lists and currently selected values for each list, plus it gets the callback
 * from FilterBean and "distributes" that callback to various methods based on
 * the FilterType of the callback. This reduces the amount of code required in
 * each of the beans that support the individual mini-tabs.
 */
public abstract class ApproverFilterBase extends ApproverBase implements FilterHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(ApproverFilterBase.class);

	/**
	 * The FilterBean instance that manages filter settings and changes.
	 */
	private final FilterBean filterBean;

	/**
	 * The current Department filter setting -- the database id of the
	 * Department that the displayed timecards should belong to. If 0, then the
	 * Department filter is set to "All" (no filtering).
	 */
	private Integer departmentId;

	/**
	 * The current WeeklyBatch filter setting -- the database id of the
	 * WeeklyBatch that the displayed timecards should belong to. If 0, then the
	 * WeeklyBatch filter is set to "All" (no filtering).
	 */
	private Integer batchId;

	/**
	 * The current WeeklyStatusFilter filter setting, which (indirectly) defines
	 * the possible WeeklyStatus values that the displayed timecards should
	 * have. If set to WeeklyStatusFilter.ALL, then the WeeklyStatusFilter
	 * selection is set to "All" -- no filtering.
	 */
	private ApprovableStatusFilter statusFilter;

	/** True iff the user has elected to show timecards or StartForm`s for all
	 * projects in the case of a Commercial (AICP) production.  This option is
	 * only available to users with Financial Data Admin permission.  This is
	 * also known as "aggregate view" or "cross-project view". */
	private boolean showAllProjects;

	private String unionLocalNum;

	/**
	 * default constructor
	 */
	public ApproverFilterBase(String sortkey) {
		super(sortkey);
		log.debug("Init");

		filterBean = FilterBean.getInstance();

		showAllProjects = SessionUtils.getBoolean(Constants.ATTR_APPROVER_SHOW_ALL, false);

		if (getWeekEndDate() == null) {
			setWeekEndDate(super.getWeekEndDate());
		}

		// not currently using the View class' selectedTab support
//		setSelectedTab(filterBean.getCurrentTab());

	}

	/**
	 * A callback method, called by FilterBean when a user has eliminated the
	 * user of a particular filter type.
	 *
	 * @param type The type of filter being removed.
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#dropFilter(com.lightspeedeps.type.FilterType)
	 */
	@Override
	public void dropFilter(FilterType type) {
		switch(type) {
			case DEPT:
				departmentId = 0;
				listenDeptChange();
				break;
			case NAME:
				setEmployeeAccount(Constants.CATEGORY_ALL);
				listenEmployeeChange();
				break;
			case DATE:
				setWeekEndDate(Constants.SELECT_ALL_DATE);
				listenWeekEndChange();
				break;
			case BATCH:
				batchId = 0;
				listenBatchChange();
				break;
			case STATUS:
				statusFilter = ApprovableStatusFilter.ALL;
				listenStatusChange();
				break;
			case UNION:
				unionLocalNum = null;
				listenUnionChange();
				break;
			case N_A:
				break;
		}
	}

	/**
	 * A change-event listener method, called by FilterBean, to handle any one of
	 * several types of changes.
	 * @param type
	 * @param value The newly-selected value from the drop-down list. This is never null.
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#listenChange(com.lightspeedeps.type.FilterType, java.lang.Object)
	 */
	@Override
	public void listenChange(FilterType type, Object value) {
		switch(type) {
			case DEPT:
				departmentId = Integer.valueOf((String)value);
				listenDeptChange();
				break;
			case NAME:
				setEmployeeAccount((String)value);
				listenEmployeeChange();
				break;
			case DATE:
				setWeekEndDate((Date)value);
				listenWeekEndChange();
				break;
			case BATCH:
				batchId = Integer.valueOf((String)value);
				listenBatchChange();
				break;
			case STATUS:
				try {
					statusFilter = ApprovableStatusFilter.valueOf((String)value);
				}
				catch (Exception e) { // can happen when switching "filter by"
					statusFilter = ApprovableStatusFilter.ALL;
				}
				listenStatusChange();
				break;
			case UNION:
				unionLocalNum = (String) value;
				listenUnionChange();
				break;
			case N_A:
				break;
		}
	}

	/**
	 * ValueChangeListener for the "show all projects" check-box. This option is
	 * only available to select users, currently those with Financial Data Admin
	 * permission. Also known as "aggregate" or "cross-project" view.
	 *
	 * @param event The event created by the framework.
	 */
	public void listenAllProjects(ValueChangeEvent event) {
		try {
			log.debug("checkbox=" + event.getNewValue());
			if (event.getNewValue() != null) {
				if (getFilterBean().getFilterType() == FilterType.BATCH ||
						getFilterBean().getFilterType() == FilterType.DEPT) {
					getFilterBean().setSelectFilterList(null); // force refresh of batch or dept selection list
					getFilterBean().getSelectFilterList();	// must refresh filter list before we refresh TC list
				}
				getFilterBean().setEndDateList(null); // force refresh (oldest entry may change)
				refreshTimecardList();
				SessionUtils.put(Constants.ATTR_APPROVER_SHOW_ALL, showAllProjects);
				// set aggregate view default on Full Timecard page to be the same:
				SessionUtils.put(Constants.ATTR_FULL_TC_SHOW_ALL, showAllProjects);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * Upon entry, {@link #getBatchId()} has been set to the new value.
	 */
	protected void listenBatchChange() {
	}

	/**
	 * ValueChangeListener for Department drop-down list
	 * Upon entry, {@link #getDepartmentId()} has been set to the new value.
	 */
	protected void listenDeptChange() {
	}

	/**
	 * ValueChangeListener for Employee drop-down list
	 * Upon entry, {@link #getEmployeeAccount()} has been set to the new value.
	 */
	protected void listenEmployeeChange() {
	}

	/**
	 * ValueChangeListener for Status drop-down filter list
	 * Upon entry, {@link #getStatusFilter()} has been set to the new value.
	 */
	protected void listenStatusChange() {
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 * Upon entry, {@link #getWeekEndDate()} has been set to the new value.
	 */
	protected void listenWeekEndChange() {
	}

	protected void refreshTimecardList() {
	}

	protected void listenUnionChange() {
	}

	/**
	 *
	 * @see com.lightspeedeps.web.approver.FilterHolder#createList(com.lightspeedeps.type.FilterType)
	 */
	@Override
	public List<SelectItem> createList(FilterType type) {
		List<SelectItem> list = null;
		switch(type) {
			case DEPT:
				list = createDepartmentList();
				break;
			case NAME:
				list = ApproverUtils.createEmployeeList(getvContact(), project, true, getUserHasViewHtg());
				break;
			case DATE:
				list = createEndDateList(getWeekEndDate(), getShowAllProjects());
				break;
			case BATCH:
				list = createBatchList(getWeekEndDate());
				break;
			case STATUS:
				list = createStatusList();
				break;
			case UNION:
				list = createUnionList();
				break;
			case N_A:
				break;
		}
		return list;
	}

	/**
	 * Create a list of WeeklyBatch items for a drop-down.
	 *
	 * @param weekEndDate The week-ending date of interest.
	 * @return null; typically subclasses will override this method to provide
	 *         the appropriate List of items.
	 */
	protected List<SelectItem> createBatchList(Date weekEndDate) {
		return null;
	}

	/**
	 * Create a list of WeeklyStatusFilter items for a drop-down.
	 *
	 * @return default list generated from all WeeklyStatusFilter values;
	 *         subclasses may override this method to provide a different list,
	 *         e.g., for the status of Start Forms instead of timecards.
	 */
	protected List<SelectItem> createStatusList() {
		return EnumList.getWeeklyStatusFilterList();
	}

	/**
	 * @see com.lightspeedeps.web.approver.ApproverBase#createTimecardList()
	 */
	@Override
	protected void createTimecardList() {
	}

	/**
	 * Create the list of Department`s for the drop-down list. For a Production
	 * Approver the list includes all Departments; for a departmental approver,
	 * it includes just those Departments for which the Contact is an approver.
	 * <p>
	 * An "All" entry is added at the top of the list if there is more than one
	 * department.
	 *
	 * @return the list of Departments as SelectItem`s, where the value field is
	 *         the Department.id field.
	 */
	protected List<SelectItem> createDepartmentList() {
		return null;
	}

	protected List<SelectItem> createUnionList() {
		return null;
	}

	/**See {@link #filterBean}. */
	public FilterBean getFilterBean() {
		return filterBean;
	}

	/** See {@link FilterBean#getWeekEndDate()}. */
	@Override
	public Date getWeekEndDate() {
		return filterBean.getWeekEndDate();
	}

	/** See {@link FilterBean#setWeekEndDate(Date)}. */
	@Override
	public void setWeekEndDate(Date date) {
		filterBean.setWeekEndDate(date);
	}

	/** See {@link FilterBean#getEndDateList()}. */
	public List<SelectItem> getEndDateList() {
		return filterBean.getEndDateList();
	}
	/** See {@link FilterBean#setEndDateList(List)}. */
	public void setEndDateList(List<SelectItem> endDateList) {
		filterBean.setEndDateList(endDateList);
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer deptId) {
		departmentId = deptId;
		if (deptId != null && filterBean.getFilterType() == FilterType.DEPT) {
			filterBean.setSelectFilterValue(deptId.toString());
		}
	}

	/**See {@link #batchId}. */
	public Integer getBatchId() {
		return batchId;
	}
	/**See {@link #batchId}. */
	public void setBatchId(Integer batchId) {
		this.batchId = batchId;
		if (batchId != null && filterBean.getFilterType() == FilterType.BATCH) {
			filterBean.setSelectFilterValue(batchId.toString());
		}
	}

	/** See {@link #statusFilter}. */
	public ApprovableStatusFilter getStatusFilter() {
		return statusFilter;
	}
	/** See {@link #statusFilter}. */
	public void setStatusFilter(ApprovableStatusFilter status) {
		statusFilter = status;
		if (statusFilter != null && filterBean.getFilterType() == FilterType.STATUS) {
			filterBean.setSelectFilterValue(statusFilter.name());
		}
	}

	/** See {@link FilterBean#getEmployeeAccount()}. */
	public String getEmployeeAccount() {
		String acct = filterBean.getEmployeeAccount();
		if (acct != null && acct.equals(Constants.CATEGORY_ALL)) {
			acct = null;
		}
		return acct;
	}
	/** See {@link FilterBean#setEmployeeAccount(String)}. */
	public void setEmployeeAccount(String employeeAccount) {
		filterBean.setEmployeeAccount(employeeAccount);
		if (filterBean.getFilterType() == FilterType.NAME) {
			filterBean.setSelectFilterValue(employeeAccount);
		}
	}

	/** See {@link FilterBean#getEmployeeList()}. */
	public List<SelectItem> getEmployeeList() {
		return filterBean.getEmployeeList();
	}
	/** See {@link FilterBean#setEmployeeList(List)}. */
	public void setEmployeeList(List<SelectItem> employeeList) {
		filterBean.setEmployeeList(employeeList);
	}

	/** See {@link #showAllProjects}. */
	public boolean getShowAllProjects() {
		return showAllProjects;
	}
	/** See {@link #showAllProjects}. */
	public void setShowAllProjects(boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}

	/** See {@link #unionLocalNum}. */
	public String getUnionLocalNum() {
		return unionLocalNum;
	}
	/** See {@link #unionLocalNum}. */
	public void setUnionLocalNum(String union) {
		unionLocalNum = union;
		if (unionLocalNum != null && filterBean.getFilterType() == FilterType.UNION) {
			filterBean.setSelectFilterValue(unionLocalNum);
		}
	}

}
