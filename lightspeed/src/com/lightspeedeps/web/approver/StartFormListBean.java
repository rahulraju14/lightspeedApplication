/**
 * File: StartFormListBean.java
 */
package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.ProductionBatchDAO;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.ProductionBatch;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.type.ApprovableStatusFilter;
import com.lightspeedeps.type.FilterType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.report.ReportBean;

/**
 * Backing bean for the StartForm list mini-tab within the
 * Approver Dashboard page.
 */
@ManagedBean
@ViewScoped
public class StartFormListBean extends ApproverFilterBase {
	/** */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StartFormListBean.class);

	/** Label to display in drop-down for "All" unions. */
	private static final String SELECT_ALL_LABEL = "All";

	/** Value string in drop-down entry for "All" unions; this is the default "All" value
	 * for any filter to show all entries. */
	private static final String SELECT_ALL_VALUE = "0";

	/** The SelectItem to add at the top of the Unions drop-down list. */
	private static final SelectItem SELECT_ALL = new SelectItem(SELECT_ALL_VALUE, SELECT_ALL_LABEL);

	/** True iff the Start Forms List page is being rendered and is visible. When
	 * this is false, we skip creating the list of StartForms. */
	private boolean rendered = false;

	/** The Employment of the currently selected row. */
	private Employment employment;

	/** The list of Employments currently displayed. */
	private List<Employment> employmentList;

	/** The database id of the ProductionBatch currently selected for viewing. */
	private Integer prodBatchId;

	/** True iff the current user has the View-HTG permission in the current project. */
	private final boolean userHasViewHtg;

	private transient EmploymentDAO employmentDAO;

//	private String unionLocalNum;

	public StartFormListBean() {
		super(StartForm.SORTKEY_NAME);
		setSortAttributePrefix("startFormList.");

		//SessionUtils.put(Constants.ATTR_START_FORM_BACK_PAGE, HeaderViewBean.PAYROLL_APPROVER);

		if (this.getClass().equals(StartFormListBean.class)) {
			getFilterBean().register(this, FilterBean.TAB_STARTS);
		}

		AuthorizationBean authBean = AuthorizationBean.getInstance();
		userHasViewHtg = authBean.hasPageField(Constants.PGKEY_VIEW_HTG);

		// Get saved values from last time page was displayed
		setDepartmentId(SessionUtils.getInteger(Constants.ATTR_APPROVER_DEPT));

		if (getFilterBean().getFilterType() == FilterType.BATCH) {
			setProdBatchId(SessionUtils.getInteger(Constants.ATTR_SF_BATCH_ID));
		}
		else {
			setProdBatchId(0); // select all batches if filtering by anything else
			getFilterBean().setSelectFilterList(null); // force refresh of selection list - may be invalid
			setStatusFilter(ApprovableStatusFilter.ALL); // set "status" filter to "All" - existing setting may be incompatible
		}

		setEmployeeAccount(SessionUtils.getString(Constants.ATTR_APPROVER_EMPLOYEE));
		if (getEmployeeAccount() == null) {
			setEmployeeAccount(Constants.CATEGORY_ALL);
		}

		restoreSortOrder();
		// remaining initialization will be done, ONLY IF we're being rendered,
		// by the setup() method. This avoids creating the employment list when we're not
		// being rendered.
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(java.lang.Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		if (id != null) {
			for (Employment emp : employmentList) {
				if (emp.getId().equals(id)) {
					emp.setSelected(true);
					employment = emp;
				}
				else {
					emp.setSelected(false);
				}
			}
		}
		super.setupSelectedItem(id);
		SessionUtils.put(Constants.ATTR_START_FORM_ID, id);
	}

	public String actionRefresh() {
		createStartFormList();
		return null;
	}

	/**
	 * Action method for the "Print" button on the Start Form List mini-tab.
	 * @return null navigation string
	 */
	public String actionPrint() {
		print(false);
		return null;
	}

	/**
	 * Action method for the "Export" button on the Start Form List mini-tab.
	 * @return null navigation string
	 */
	public String actionExport() {
		print(true);
		return null;
	}

	/**
	 * Handles the "Print" and "Export" functions. It gathers the database ids
	 * of the StartForm`s to be printed and passes the list to the appropriate
	 * print method.
	 */
	private void print(boolean export) {
		if (getEmploymentList().size() > 0) {
			ReportBean report = ReportBean.getInstance();
			List<Integer> ids = new ArrayList<>();
			for (Employment emp : getEmploymentList()) {
				ids.add(emp.getId());
			}
			String orderBy = getSortColumnName();
			orderBy = StartForm.getSqlSortKey(orderBy, isAscending());
			if (! report.generateStartFormList(ids, export, orderBy)) {
				MsgUtils.addGenericErrorMessage();
			}
		}
	}

	/**
	 * @see com.lightspeedeps.web.approver.FilterHolder#changeTab(int,int)
	 */
	@Override
	public void changeTab(int oldTab, int newTab) {
		if (newTab == FilterBean.TAB_STARTS) {
			employmentList = null;	// so it will get rebuilt
			if (getFilterBean().getFilterType() == FilterType.BATCH) {
				getFilterBean().setSelectFilterList(null); // force refresh of batch selection list
				setProdBatchId(prodBatchId);
			}
			else if (getFilterBean().getFilterType() == FilterType.STATUS) {
				getFilterBean().setSelectFilterList(null); // force refresh of status selection list
				setStatusFilter(ApprovableStatusFilter.ALL); // set to "All" - existing setting may be incompatible
			}
		}
	}

	/**
	 * Listener for Batch selection drop-down list change
	 */
	@Override
	protected void listenBatchChange() {
		employmentList = null;
	}

	/**
	 * Listener for Department selection drop-down list change
	 */
	@Override
	protected void listenDeptChange() {
		employmentList = null;
	}

	/**
	 * Listener for Status selection drop-down list change.
	 * @see com.lightspeedeps.web.approver.ApproverFilterBase#listenStatusChange()
	 */
	@Override
	protected void listenStatusChange() {
		employmentList = null; // force refresh
	}

	/**
	 * Listener for Employee drop-down list change
	 */
	@Override
	protected void listenEmployeeChange() {
		employmentList = null;
	}

	/**
	 * Listener for Union drop-down list change
	 */
	@Override
	protected void listenUnionChange() {
		employmentList = null;
	}

	/**
	 * Create a list of SelectItem`s representing the ProductionBatch objects
	 * within the current Production.
	 *
	 * @param weekEndDate - ignored
	 * @return A non-null, but possibly empty, List as described above.
	 */
	@Override
	protected List<SelectItem> createBatchList(Date weekEndDate) {
		List<SelectItem> list = new ArrayList<>();
		list.add(Constants.SELECT_NOT_BATCHED);	// "un-batched" - will be top item if no others exist

		List<ProductionBatch> batches =
				ProductionBatchDAO.getInstance().findByProductionProject(production, project);
		if (batches.size() > 0) {
			for (ProductionBatch pb : batches) {
				list.add(new SelectItem(pb.getId().toString(), pb.getName()));
			}
			list.add(0,Constants.SELECT_ALL_IDS); // top item is "All", followed by unbatched
		}
		return list;
	}

	/**
	 * Create list of choices for Status drop-down; we want just the ones applicable
	 * to StartForms, not to timecards.
	 *
	 * @see com.lightspeedeps.web.approver.ApproverFilterBase#createStatusList()
	 */
	@Override
	protected List<SelectItem> createStatusList() {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		boolean include = false;
		for (ApprovableStatusFilter wsf : ApprovableStatusFilter.values()) {
			if (include || wsf == ApprovableStatusFilter.ALL) {
				selectitem = new SelectItem(wsf.name(), wsf.toString()); // get the Real World version
				list.add(selectitem);
			}
			else if (wsf == ApprovableStatusFilter.N_A) {
				// The enum entries AFTER "N_A" are for Starts!
				include = true;
			}
		}
		return list;
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
	@Override
	protected List<SelectItem> createDepartmentList() {
		ArrayList<SelectItem> deptList;
		if (userHasViewHtg || ApproverUtils.isProdOrProjectApprover(getvContact(), project)) {
			deptList = new ArrayList<>(DepartmentUtils.getDepartmentDataAdminDL(
					getShowAllProjects() ? null : project, true));
		}
		else {
			// Find depts with this user as approver
			deptList = new ArrayList<>();
			if (getvContact() == null) {
				// very unusual - user logged out or did "My Home" simultaneous with filter change
				return deptList;
			}
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(getvContact(), project);
			for (Department dept : depts) {
				deptList.add(new SelectItem(dept.getId(), dept.getName()));
			}
		}
		deptList.add(0, new SelectItem(0, "All"));
		return deptList;
	}

	/**
	 * Create the List of StartForm`s to be displayed.
	 */
	private void createStartFormList() {
		ProductionBatch batch = null;
		boolean anyBatch = false;
		if (getFilterBean().getSelectFilterValue() != null) {
			FilterType f = getFilterBean().getFilterType();
			if (f == FilterType.DEPT) {
				setDepartmentId(Integer.valueOf(getFilterBean().getSelectFilterValue()));
				setProdBatchId(0);	// clear batch filter
				setStatusFilter(ApprovableStatusFilter.ALL); // clear status filter
				setUnionLocalNum(null); // clear union filter
			}
			else if (f == FilterType.BATCH) {
				setDepartmentId(0);	// clear department filter
				setProdBatchId(Integer.valueOf(getFilterBean().getSelectFilterValue()));
				setStatusFilter(ApprovableStatusFilter.ALL);	// clear status filter
				setUnionLocalNum(null); // clear union filter
			}
			else if (f == FilterType.STATUS) {
				setDepartmentId(0);	// clear department filter
				setProdBatchId(0);	// clear batch filter
				// Note that the filterBean's selectFilterValue is the Enum's name value.
				//LS-2760  On ApproverDashboard selecting By Status in Filter dropdown causes 500 error
				if (null != getFilterBean().getSelectFilterValue()
						&& getFilterBean().getSelectFilterValue().equals("0")) {
					setStatusFilter(ApprovableStatusFilter.ALL);
				} 
				else {
					setStatusFilter(ApprovableStatusFilter.valueOf(getFilterBean().getSelectFilterValue()));
				}
				setUnionLocalNum(null); // clear union filter				
			}
			else if (f == FilterType.UNION) {
				setDepartmentId(0);	// clear department filter
				setProdBatchId(0);	// clear batch filter
				setStatusFilter(ApprovableStatusFilter.ALL);	// clear status filter
				setUnionLocalNum(getFilterBean().getSelectFilterValue());
				if (SELECT_ALL_VALUE.equals(getUnionLocalNum())) {
					setUnionLocalNum(null);
				}
			}
		}
		if (getProdBatchId() == null || getProdBatchId() == 0) {
			setProdBatchId(0);
			anyBatch = true;
		}
		else if (getProdBatchId() != Constants.SELECT_BATCH_NONE_ID) {
			batch = ProductionBatchDAO.getInstance().findById(getProdBatchId());
		}

		String deptName = null;
		if (getDepartmentId() != null && getDepartmentId() != 0) {
			Department dept = DepartmentDAO.getInstance().findById(getDepartmentId());
			deptName = dept.getName();
		}

		Project filterProject = project;
		if (getShowAllProjects()) {
			filterProject = null;
		}

		if (this instanceof UpdateRatesBean) {
			employmentList = getEmploymentDAO().findByBatchDepartmentUserAcct(production, filterProject,
					anyBatch, batch, deptName, getEmployeeAccount(), getStatusFilter(), getUnionLocalNum());
		}
		else {
			employmentList = getEmploymentDAO().findByBatchDepartmentUserAcct(production, filterProject,
					anyBatch, batch, deptName, getEmployeeAccount(), getStatusFilter(), getUnionLocalNum());
		}
		setSelectedRow(-1);
		if (employmentList.size() > 0) {
			// set transient employment.startForm for each Employment in list.
			Iterator<Employment> itr = employmentList.iterator();
			while (itr.hasNext()) {
				Employment emp = itr.next();
				if (emp.getStartForms() == null || emp.getStartForms().isEmpty()) {
					//Remove all Employments from the list that do not have an associated StartForm.
					itr.remove();
				}
			}
			if (employmentList.size() > 0) {
				doSort();
				employment = employmentList.get(0);
				setupDefaultItem();
			}
		}
		SessionUtils.put(Constants.ATTR_SF_BATCH_ID, getProdBatchId());
		SessionUtils.put(Constants.ATTR_APPROVER_DEPT, getDepartmentId());
		SessionUtils.put(Constants.ATTR_APPROVER_EMPLOYEE, getEmployeeAccount());
	}

	/**
	 * Rebuild our StartForm list to match the current filter settings.
	 */
	@Override
	protected void refreshTimecardList() {
		createStartFormList();
	}

	/**
	 * Determine which StartForm should be selected, and set it up so
	 * it's selected and visible.
	 */
	private void setupDefaultItem() {
		Integer id = SessionUtils.getInteger(Constants.ATTR_START_FORM_ID);
		if (id == null && employment != null) {
			id = employment.getId();
		}
		setupSelectedItem(id);
		if (employment != null) {
			scrollToRow(employment);
		}
	}

	/**
	 * @see com.lightspeedeps.web.approver.ApproverBase#createEndDateList(java.util.Date,
	 *      boolean)
	 * @return null, since the Start Form List page does not use the week-ending
	 *         date drop-down.
	 */
	@Override
	protected List<SelectItem> createEndDateList(Date listDate, boolean allProjects) {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected List<SelectItem> createUnionList() {
		List<SelectItem> list = new ArrayList<>();
		List<Object> startForms = (List) getEmploymentDAO().findByNamedQuery(StartForm.GET_DISTINCT_UNION_LOCAL_FOR_CURRENT_PRODUCTION, map("production", SessionUtils.getProduction()));
		for (int i=0; i<startForms.size(); i++) {
			String union = startForms.get(i).toString();
			list.add(new SelectItem(union));
		}
		list.add(0, SELECT_ALL);
		return list;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(Object,boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			StartForm sf = (StartForm)item;
			if (sf != null) {
				sf.setSelected(b);
				SessionUtils.put(Constants.ATTR_START_FORM_ID, sf.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The comparator for sorting the list of StartForm's.
	 *
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Comparator getComparator() {
		Comparator<Employment> comparator = new Comparator<Employment>() {
			@Override
			public int compare(Employment c1, Employment c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#isDefaultAscending(String)
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (sortColumn.equals(StartForm.SORTKEY_DATE)) {
			return false;
		}
		return true; // make columns default to ascending sort
	}

	/**
	 * Called due to hidden field value in our JSF page. Note that this must be public as
	 * it is called from JSF.  Used by DB or CPU-intensive setup processes to avoid
	 * initializing lists, etc., if page is not actually being rendered.
	 */
	public boolean setUp(Boolean render) {
		if (render != null && render == true && ! rendered) {
			rendered = true;
			getEmploymentList();
			setupDefaultItem();
		}
		return false;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({"rawtypes"})
	@Override
	public List getItemList() {
		return getEmploymentList();
	}

	/**
	 * @return the list of StartForm`s to be rendered on the page.  If the
	 * {@link #rendered} flag is false, we are not actually being rendered.
	 * It's just the ICEfaces framework calling the value= method on the
	 * ace:dataTable (for some unknown reason).
	 */
	public List<Employment> getEmploymentList() {
		if (! rendered) {
			return new ArrayList<>();
		}
		if (employmentList == null) {
			createStartFormList();
		}
		sortIfNeeded(); // only sort if the sort column or ordering has changed.
		return employmentList;
	}

	public void setEmploymentList(List<Employment> employmentList) {
		this.employmentList = employmentList;
	}

	/** See {@link #employment}. */
	public Employment getEmployment() {
		if (employment == null) {
			getEmploymentList();
		}
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/**See {@link #prodBatchId}. */
	public Integer getProdBatchId() {
		return prodBatchId;
	}
	/**See {@link #prodBatchId}. */
	public void setProdBatchId(Integer pbId) {
		prodBatchId = pbId;
		if (prodBatchId != null && getFilterBean().getFilterType() == FilterType.BATCH) {
			getFilterBean().setSelectFilterValue(prodBatchId.toString());
		}
	}

	/**See {@link #employmentDAO}. */
	/*package*/ EmploymentDAO getEmploymentDAO() {
		if (employmentDAO == null) {
			employmentDAO = EmploymentDAO.getInstance();
		}
		return employmentDAO;
	}

}
