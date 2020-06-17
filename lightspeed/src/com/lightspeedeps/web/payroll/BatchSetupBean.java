package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.ProductionBatchDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ControlHolder;
import com.lightspeedeps.object.TriStateCheckboxExt;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupInputBean;
import com.lightspeedeps.web.view.*;

/**
 * Backing bean for the Batch Setup page.
 */
@ManagedBean
@ViewScoped
public class BatchSetupBean extends ListView implements ControlHolder, Serializable, SortHolder {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(BatchSetupBean.class);

	private static final int ACT_NEW_BATCH = 15;
//	private static final int ACT_PRINT = 16;

	/** The Production containing the batches being viewed and managed. */
	private Production production;

	/** The current Project, which restricts the batches displayed. Only
	 * used for Commercial productions. For TV & Feature
	 * productions, this will be null. */
	private Project project;

	/** The WeeklyTimecard.id value of the icon clicked on by the user. */
	private Integer selectedTimecardId;

	private Employment employment;

	/** The database id of the currently selected ProductionBatch object. */
	private Integer batchId;

	/** The name of the currently selected ProductionBatch. */
	private String batchName;

	/** The List of ProductionBatch objects to choose from; the StartForm`s from the
	 * selected one will be displayed.  The value field of the SelectItem`s are the
	 * database ids of each ProductionBatch object. */
	private List<SelectItem> prodBatchList;

	/** The List of Employment`s displayed on the left, not yet in a batch. */
	private List<Employment> unbatchedSfList;

	/** The List of Employment`s displayed on the right, contained within the
	 * currently selected batch. */
	private final List<Employment> batchedSfList = new ArrayList<>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final SortableListImpl sortedBatchedSfList = new SortableListImpl(this, (List)batchedSfList, StartForm.SORTKEY_NAME, true);

	/** The backing field for the checkbox in the batch header area, which acts
	 * as a "master" select/unselect for all enabled checkboxes in the
	 * list of weekly batches. */
	private TriStateCheckboxExt batchMasterCheck = new TriStateCheckboxExt();

	/** The backing field for the checkbox in the timecard header area, which
	 * acts as a "master" select/unselect for all enabled checkboxes in the
	 * current timecard list. */
	private TriStateCheckboxExt sfMasterCheck = new TriStateCheckboxExt();

	/** The department selection list; this normally includes only those departments
	 * for which the current user would have at least one timecard listed on the
	 * currently selected date. */
	private List<SelectItem> departmentList;

	/** Selected department ID.  A value of 0 is used for the "All" entry,
	 * when timecards from all departments are displayed. */
	private Integer departmentId;

	/** Name of currently selected department*/
	private String departmentName;

	private String batchedCompareColumn;

	private transient EmploymentDAO employmentDAO;

	private transient ProductionBatchDAO productionBatchDAO;

	/**
	 * default constructor
	 */
	public BatchSetupBean() {
		super(StartForm.SORTKEY_NAME, "BatchSetup.");
		log.debug("Init");
		//rowHeight = 24; // set our table row display height - not the usual 26px.

		production = SessionUtils.getProduction();
		if (production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}

		batchMasterCheck.setHolder(this);
		batchMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		batchMasterCheck.setId(batchMasterCheck);

		sfMasterCheck.setHolder(this);
		sfMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		sfMasterCheck.setId(sfMasterCheck);

		prodBatchList = createProdBatchList();

		batchId = SessionUtils.getInteger(Constants.ATTR_SETUP_BATCH_ID);
		if (batchId != null) {
			ProductionBatch batch = getProductionBatchDAO().findById(batchId);
			if (project != null && batch != null &&
					batch.getProject() != null && (! batch.getProject().equals(project))) {
				// user changed projects since last page view
				batchId = null;
				SessionUtils.put(Constants.ATTR_SETUP_BATCH_ID, null);
			}
		}
		refresh();

		// Get saved values from last time page was displayed
		departmentId = SessionUtils.getInteger(Constants.ATTR_APPROVER_DEPT);

		unbatchedSfList = createUnbatchedList();
		restoreSortOrder();
		doSort();
	}

	public String actionNewBatch() {
		PopupInputBean inputBean = PopupInputBean.getInstance();
		inputBean.show( this, ACT_NEW_BATCH, getMessagePrefix()+"New.");
		inputBean.setInput("");
		inputBean.setMaxLength(ProductionBatch.MAX_NAME_LENGTH);
		return null;
	}

	private String actionNewBatchOk() {
		PopupInputBean inputBean = PopupInputBean.getInstance();
		String name = inputBean.getInput();
		if (name == null || name.trim().length() == 0) {
			MsgUtils.addFacesMessage("BatchSetup.NoneSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		ProductionBatch batch = new ProductionBatch(production, project, name);
		getProductionBatchDAO().save(batch);
		prodBatchList = createProdBatchList();
		batchId = batch.getId();
		createBatchedSfList(); // refresh list of SF's, and updates their status
		return null;
	}

	public String actionAddToBatch() {
		int n = countSelectedItems(unbatchedSfList);
		if (n == 0 ) {
			MsgUtils.addFacesMessage("BatchSetup.NoneSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (batchId != null) {
			ProductionBatch batch = getProductionBatchDAO().findById(batchId); // refresh
			if (batch != null) {
				for (Employment emp : unbatchedSfList) {
					if (emp.getChecked()) {
						emp.setChecked(false);
						batch.getEmployments().add(emp);
						emp.setProductionBatch(batch);
						getEmploymentDAO().attachDirty(emp);
					}
				}
				getProductionBatchDAO().attachDirty(batch);
			}
		}
		refresh();
		return null;
	}

	public String actionRemoveFromBatch() {
		int n = countSelectedItems(batchedSfList);
		if (n == 0 ) {
			MsgUtils.addFacesMessage("BatchSetup.NoneSelected", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (batchId != null) {
			ProductionBatch batch = getProductionBatchDAO().findById(batchId); // refresh
			if (batch != null) {
				boolean b;
				for (Employment emp : batchedSfList) {
					if (emp.getChecked()) {
						emp.setChecked(false);
						b = removeEmployment(batch, emp);
						if (!b) {
							log.error("Employment not found in batch");
						}
					}
				}
				getProductionBatchDAO().attachDirty(batch);
			}
		}
		refresh();
		return null;
	}

	public String actionDeleteBatch() {
		PopupBean.getInstance().show(
				this, ACT_DELETE_ITEM,
				getMessagePrefix()+"Delete.");
		return null;
	}

	private String actionDeleteBatchOk() {
		try {
			ProductionBatch batch = getProductionBatchDAO().findById(batchId); // refresh
			for (Employment emp : batch.getEmployments()) {
				emp.setChecked(false);
				emp.setProductionBatch(null);
				getEmploymentDAO().attachDirty(emp);
			}
			batch.getEmployments().clear();
			getProductionBatchDAO().delete(batch);
			batchId = null;
			prodBatchList = createProdBatchList();
			refresh();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Print button.  Display the print options
	 * pop-up dialog.
	 * @return null navigation string
	 */
//	public String actionPrint() {
//		setWeeklyTimecard(getStartFormDAO().refresh(startForm));
//		if (startForm == null) { // someone else deleted the current timecard
//			MsgUtils.addFacesMessage("Timecard.View.Deleted", FacesMessage.SEVERITY_ERROR);
//			actionRefreshOk();
//			return null;
//		}
//		TimecardUtils.showPrintOptions(startForm, getUserHasEditHtg(), this, ACT_PRINT);
//		MsgUtils.addFacesMessage("Generic.NotAvailable", FacesMessage.SEVERITY_ERROR);
//		return null;
//	}

	/**
	 * Print the timecards as requested by the user.
	 * @return null navigation string
	 */
//	private String actionPrintOk() {
//		return null;
//	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * @param event contains old and new values
	 */
	public void listenBatchChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Integer id = (Integer)event.getNewValue();
			if (id != null) {
				batchId = id;
				createBatchedSfList(); // refresh list of SF's, and updates their status
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for Department drop-down list
	 * @param event contains old and new values
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue().getClass() + ":" + event.getNewValue());
			Integer id = (Integer)event.getNewValue();
			if (id != null) {
				Integer tcId = null;
				if (employment != null) {
					tcId = employment.getId();
					employment.setSelected(false);
				}
				departmentId = id;
				unbatchedSfList = createUnbatchedList(); // refresh list of SF's, and updates their status
				doSort();
				SessionUtils.put(Constants.ATTR_APPROVER_DEPT, departmentId);
				boolean found = false;
				if (tcId != null) {
					for (Employment tce : unbatchedSfList) {
						if (tcId.equals(tce.getId())) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					setupSelectedItem(tcId);
				}
				else {
					setupFirst(); // display first user in list
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
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
				Integer tcId = null;
				if (employment != null) {
					tcId = employment.getId();
					employment.setSelected(false);
				}
				createUnbatchedList(); // refresh list of TC's, and updates their status
				boolean found = false;
				if (tcId != null) {
					for (Employment tce : unbatchedSfList) {
						if (tcId.equals(tce.getId())) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					setupSelectedItem(tcId);
				}
				else {
					setupFirst(); // display first user in list
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for the checkboxes on each Start Form entry in the
	 * unbatched (left-hand) list.
	 *
	 * @param event The event created by the framework.
	 */
	public void listenUnbatchedCheckEntry(ValueChangeEvent event) {
		log.debug("unbatched checkbox=" + event.getNewValue());
		if (event.getNewValue() != null) {
			int n = countSelectedItems(unbatchedSfList);
			if (n == unbatchedSfList.size()) {
				sfMasterCheck.setChecked();
			}
			else if (n == 0) {
				sfMasterCheck.setUnchecked();
			}
			else {
				sfMasterCheck.setPartiallyChecked();
			}
		}
	}

	/**
	 * ValueChangeListener for the checkboxes on each Start Form entry in the
	 * current batch (right-hand) list.
	 *
	 * @param event The event created by the framework.
	 */
	public void listenBatchedCheckEntry(ValueChangeEvent event) {
		log.debug("checkbox=" + event.getNewValue());
		if (event.getNewValue() != null) {
			int n = countSelectedItems(batchedSfList);
			if (n == batchedSfList.size()) {
				batchMasterCheck.setChecked();
			}
			else if (n == 0) {
				batchMasterCheck.setUnchecked();
			}
			else {
				batchMasterCheck.setPartiallyChecked();
			}
		}
	}

	public void listenBatchMasterCheck(ValueChangeEvent event) {
		log.debug(SessionUtils.getPhaseIdFmtd() + "");
		batchMasterCheck.listenChecked(event);
	}

	public void listenSfMasterCheck(ValueChangeEvent event) {
		log.debug(SessionUtils.getPhaseIdFmtd() + "");
		sfMasterCheck.listenChecked(event);
	}

	/**
	 * Called when the user clicks on the "master" selection checkbox in the header
	 * area of the timecard list.  This will set all the available (enabled) checkboxes
	 * in the list to either checked or unchecked.
	 *
	 * @see com.lightspeedeps.object.ControlHolder#clicked(javax.faces.event.ActionEvent, java.lang.Object)
	 */
	@Override
	public void clicked(TriStateCheckboxExt ckBox, Object id) {
		List<Employment> list = null;
		if (id == batchMasterCheck) {
			list = getBatchedSfList();
		}
		else if (id == sfMasterCheck) {
			list = getUnbatchedSfList();
		}
		else {
			log.error("unknown tristate check id!");
		}
		if (list != null && ckBox != null) {
			if (ckBox.isPartiallyChecked()) { // skip partial-check state
				ckBox.setCheckValue(TriStateCheckboxExt.CHECK_ON);
			}
			for (Employment emp : list) {
				if (ckBox.isUnchecked()) {
					emp.setChecked(false);
				}
				else {
					emp.setChecked(true);
				}
			}
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_NEW_BATCH:
				res = actionNewBatchOk();
				break;
			case ACT_DELETE_ITEM:
				res = actionDeleteBatchOk();
				break;
//			case ACT_PRINT:
//				res = actionPrintOk();
//				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/**
	 * Called when the user Cancels one of our pop-up dialogs.
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			default:
				res = super.confirmCancel(action);
				break;
		}
		return res;
	}

	/**
	 * This method is fired when row is selected and set the bean value depend on the ID selected
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (employment != null) {
			employment.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		employment = null;
		if (id == null) {
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			employment = findEntry(id);
			if (employment == null) {
				id = findDefaultId();
				if (id != null) {
					employment = getEmploymentDAO().findById(id);
				}
			}
		}
		SessionUtils.put(Constants.ATTR_START_FORM_ID, id);
		setupSelectedItem();
	}

	/**
	 * Get the first default id
	 * @return id
	 */
	private Integer findDefaultId() {
		Integer id = null;
		if (getItemList().size() > 0) {
			Employment emp = (Employment)getItemList().get(0);
			id = emp.getId();
		}
		return id;
	}

	/**
	 * Find the Employment corresponding to a particular WeeklyTimecard
	 * database id value.
	 * @param id
	 * @return The matching Employment, or null if not found.
	 */
	private Employment findEntry(Integer id) {
		Employment tc = null;
		for (Employment tce : unbatchedSfList) {
			if (id.equals(tce.getId())) {
				tc = tce;
				break;
			}
		}
		return tc;
	}

	/**
	 * Set up to display the first entry in unbatchedList.
	 */
	private void setupFirst() {
		if (unbatchedSfList.size() > 0) {
			employment = unbatchedSfList.get(0);
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, employment.getId());
		}
		else { // Create empty timecard
			employment = new Employment();
			employment.setProject(project);
		}
		setupSelectedItem();
	}

	private void setupSelectedItem() {
		if (employment != null) {
			employment.setSelected(true);
		}
	}

	/**
	 * @param empList List of Employment`s to check.
	 * @return The number of Employment`s which have check marks.
	 */
	private int countSelectedItems(List<Employment> empList) {
		int items = 0;
		for (Employment emp : empList) {
			if (emp.getChecked()) {
				items++;
			}
		}
		return items;
	}

	/**
	 * Refresh our display:
	 * - if no batch is selected, select the first one in the list.
	 * - rebuild the list of Start Forms in the selected batch.
	 * - rebuild the list of unbatched Start Forms.
	 * - clear the master check-boxes.
	 */
	private void refresh() {
		if (batchId == null && prodBatchList.size() > 0) {
			batchId = (Integer)prodBatchList.get(0).getValue();
		}
		createBatchedSfList();
		unbatchedSfList = null;
		sfMasterCheck.setUnchecked();
	}

	private List<Employment> createBatchedSfList() {
		batchedSfList.clear();
		if (batchId != null) {
			ProductionBatch batch = getProductionBatchDAO().findById(batchId);
			batchName = batch.getName();
			Map<String, Object> values = new HashMap<>();
			values.put("production", production);
			values.put("project", project);
			values.put("batch", batch);
			batchedSfList.addAll(EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_BATCH, values));
			sortedBatchedSfList.doSort();
			for (Employment emp : batchedSfList) { // fix LIE on employment.getStartForm()
				emp.getStartForm();
			}
		}
		SessionUtils.put(Constants.ATTR_SETUP_BATCH_ID, batchId);
		batchMasterCheck.setUnchecked();
		return batchedSfList;
	}

	private List<SelectItem> createProdBatchList() {
		return createProdBatchList(production, project);
	}

	public static List<SelectItem> createProdBatchList(Production prod, Project project) {
		List<ProductionBatch> list = ProductionBatchDAO.getInstance().findByProductionProject(prod, project);
		List<SelectItem> batchList = new ArrayList<>();
		for (ProductionBatch pb : list) {
			batchList.add(new SelectItem(pb.getId(), pb.getName()));
		}
		return batchList;
	}

	private List<Employment> createUnbatchedList() {
		List<Employment> empList;

//		batchMasterCheck.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		if (departmentId == null) {
			departmentId = 0;
			SessionUtils.put(Constants.ATTR_APPROVER_DEPT, departmentId);
		}

		Map<String, Object> values = new HashMap<>();
		values.put("production", production);
		values.put("project", project);

		if (departmentId != 0) {
			Department currDept = DepartmentDAO.getInstance().findById(departmentId);
			departmentName = currDept.getName();
			values.put("department", currDept);
			empList = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_DEPARTMENT, values);
		}
		else {
			// "All departments" is selected
			departmentName = "All";
			empList = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT, values);
		}
		// Remove Employment from Deleted Contacts
		for (Iterator<Employment> iter = empList.iterator(); iter.hasNext(); ) {
			Employment emp = iter.next();
			if (! emp.getContact().getActive()) {
				iter.remove();
			}

			// fix LIEs
			emp.getStartForm();
			emp.getDepartment().getName();

		}
		sfMasterCheck.setUnchecked();

		return empList;
	}

	/**
	 * Create the list of Department`s for the drop-down list.
	 * <p>
	 * An "All" entry is added at the top of the list if there is more than one
	 * department.
	 *
	 * @return the list of Departments as SelectItem`s, where the value field is
	 *         the Department.id field.
	 */
	private List<SelectItem> createDepartmentList() {
		ArrayList<SelectItem> deptList;
		deptList = new ArrayList<>(DepartmentUtils.getDepartmentCastCrewDL());
		deptList.add(0, new SelectItem(0, "All"));

		/**
		 * Create the list of Department`s for the drop-down list. The list
		 * includes exactly those departments represented by the timecards in
		 * the current unbatchedList. An "All" entry is added at the top
		 * of the list if there is more than one department.
		 */
//		if (unbatchedList.size() > 0) {
//			DepartmentDAO departmentDAO = DepartmentDAO.getInstance();
//			Set<Integer> deptIds = new HashSet<Integer>();
//			for (StartForm tce : unbatchedList) {
//				Integer deptId = tce.getDepartment().getId();
//				deptIds.add(deptId);
//			}
//			for (Integer id : deptIds) {
//				Department dept = departmentDAO.findById(id);
//				deptList.add(new SelectItem(dept.getId(), dept.getName()));
//			}
//			Collections.sort(departmentList, getSelectItemComparator());
//			if (deptList.size() > 1) {
//				deptList.add(0, new SelectItem(0, "All"));
//			}
//			else if (departmentId == 0) {
//				departmentId = (Integer)departmentList.get(0).getValue();
//			}
//		}
//		else {
//			departmentId = null;
//		}

		return deptList;
	}

//	/**
//	 * Generate a sorted drop-down selection list of departments, based on the
//	 * supplied list of Department`s. If more than one Department is in the
//	 * List, an "All" entry is added to the beginning of the list.
//	 *
//	 * @param depts The List of Department`s whose names should be presented in
//	 *            the drop-down list. The order is irrelevant.
//	 */
//	private void createDepartmentList(List<Department> depts) {
//		departmentList.clear();
//		if (depts.size() > 0) {
//			for (Department dept : depts) {
//				departmentList.add(new SelectItem(dept.getId(), dept.getName()));
//			}
//			Collections.sort(departmentList, getSelectItemComparator());
//			if (departmentList.size() > 1) {
//				departmentList.add(0, new SelectItem(0, "All"));
//			}
//		}
//	}

	/**
	 * Remove a Employment from the given ProductionBatch.
	 *
	 * @param batch The ProductionBatch of interest.
	 * @param form The Employment to be removed.
	 * @return True iff the removal was successful.
	 */
	private boolean removeEmployment(ProductionBatch batch, Employment form) {
		boolean removed = false;
		Set<Employment> list = batch.getEmployments();
		Iterator<Employment> it = list.iterator();
		while(it.hasNext()) {
			Employment emp = it.next();
			if (emp.getId().equals(form.getId())) {
				it.remove();
				removed = true;
				emp.setProductionBatch(null);
				getEmploymentDAO().attachDirty(emp);
				break;
			}
		}
		return removed;
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List, com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void sort(@SuppressWarnings("rawtypes") List list, SortableList sortableList) {
		if (list != null) {
			batchedCompareColumn = sortableList.getSortColumnName();
			if (sortableList.isAscending()) {
				Collections.sort(list, batchedComparator);
			}
			else {
				Collections.sort(list, Collections.reverseOrder(batchedComparator));
			}
		}
	}

	public Comparator<Employment> batchedComparator = new Comparator<Employment>() {
		@Override
		public int compare(Employment one, Employment two) {
			int ret = one.compareTo(two, batchedCompareColumn);
			return ret;
		}
	};

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		return true; // all columns default to ascending sort
	}

	// accessors and mutators

	/**See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getProduction();
		}
		return production;
	}

	/**See {@link #batchId}. */
	public Integer getBatchId() {
		return batchId;
	}
	/**See {@link #batchId}. */
	public void setBatchId(Integer batchId) {
		this.batchId = batchId;
	}

	/**See {@link #batchName}. */
	public String getBatchName() {
		return batchName;
	}
	/**See {@link #batchName}. */
	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}

	/** See {@link #batchMasterCheck}. */
	public TriStateCheckboxExt getBatchMasterCheck() {
		return batchMasterCheck;
	}
	/** See {@link #batchMasterCheck}. */
	public void setBatchMasterCheck(TriStateCheckboxExt masterTriState) {
		batchMasterCheck = masterTriState;
	}

	/**See {@link #sfMasterCheck}. */
	public TriStateCheckboxExt getSfMasterCheck() {
		//log.info(SessionUtils.getPhaseIdFmtd() + " value=" + sfMasterCheck.toString());
		return sfMasterCheck;
	}
	/**See {@link #sfMasterCheck}. */
	public void setSfMasterCheck(TriStateCheckboxExt tcMasterCheck) {
		sfMasterCheck = tcMasterCheck;
		//log.info(SessionUtils.getPhaseIdFmtd() + " value=" + sfMasterCheck.toString());
	}

	/**See {@link #selectedTimecardId}. */
	public Integer getSelectedTimecardId() {
		return selectedTimecardId;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return getUnbatchedSfList();
	}


	/**See {@link #batchedSfList}. */
	private List<Employment> getBatchedSfList() {
		return batchedSfList;
	}

	/**See {@link #unbatchedSfList}. */
	public List<Employment> getUnbatchedSfList() {
		if (unbatchedSfList == null) {
			unbatchedSfList = createUnbatchedList();
			doSort();
		}
		return unbatchedSfList;
	}
	/**See {@link #unbatchedSfList}. */
	public void setUnbatchedSfList(List<Employment> unbatchedList) {
		unbatchedSfList = unbatchedList;
	}

	/**See {@link #sortedBatchedSfList}. */
	public SortableListImpl getSortedBatchedSfList() {
		return sortedBatchedSfList;
	}

	/** See {@link #departmentList}. */
	public List<SelectItem> getDepartmentList() {
		if (departmentList == null) {
			departmentList = createDepartmentList();
		}
		return departmentList;
	}
	/** See {@link #departmentList}. */
	public void setDepartmentList(List<SelectItem> departmentList) {
		this.departmentList = departmentList;
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/**See {@link #departmentName}. */
	public String getDepartmentName() {
		return departmentName;
	}
	/**See {@link #departmentName}. */
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object, boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		((Employment)item).setSelected(b);
	}

	/**See {@link #prodBatchList}. */
	public List<SelectItem> getProdBatchList() {
		if (prodBatchList == null) {
			prodBatchList = createProdBatchList();
		}
		return prodBatchList;
	}
	/**See {@link #prodBatchList}. */
	public void setProdBatchList(List<SelectItem> prodBatchList) {
		this.prodBatchList = prodBatchList;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<Employment> getComparator() {
		Comparator<Employment> comparator = new Comparator<Employment>() {
			@Override
			public int compare(Employment c1, Employment c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	private EmploymentDAO getEmploymentDAO() {
		if (employmentDAO == null) {
			employmentDAO = EmploymentDAO.getInstance();
		}
		return employmentDAO;
	}

	private ProductionBatchDAO getProductionBatchDAO() {
		if (productionBatchDAO == null) {
			productionBatchDAO = ProductionBatchDAO.getInstance();
		}
		return productionBatchDAO;
	}

}
