package com.lightspeedeps.web.contact;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ApprovalAnchorDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.DeptCallDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.ReportTimeDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.ListView;
import com.lightspeedeps.web.view.SortHolder;
import com.lightspeedeps.web.view.SortableList;
import com.lightspeedeps.web.view.SortableListImpl;

/**
 * The backing bean for the Departments List/View/Edit page.
 */
@ManagedBean
@ViewScoped
public class DepartmentBean extends ListView implements SortHolder, PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 193685271733021750L;

	private static final Log log = LogFactory.getLog(DepartmentBean.class);
	//private static final int TAB_DETAIL = 0;

	private static final int ACT_DEPT_ORDER = 11;

	/* Fields */

	/** True iff the "change order" popup should be displayed. */
	private boolean showChangeOrder;

	/** The list of elements currently displayed. */
	private List<Department> departmentList;

	/** The currently viewed item. */
	private Department department;

	/** The name of the department prior to the user possibly editing it. */
	private String originalName;

	/** If true, the members list only shows Contacts with a role in the
	 * current project. */
	private boolean showProject;

	/** The Department mask currently in effect, either for the current Production,
	 * or the current Project. */
	private BitMask activeMask;

	/** The List of department members being displayed. */
	private final List<DeptMember> memberList = new ArrayList<DeptMember>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private SortableListImpl sortedMemberList = new SortableListImpl(this, (List)memberList, DeptMember.SORT_KEY_NAME, true);

	private transient DepartmentDAO departmentDAO;
	private int selectedTab=0;

	/** Constructor */
	public DepartmentBean() {
		super(Department.SORT_KEY_NAME, "Department.");
		log.debug("Init");
		try {
			Production prod = SessionUtils.getProduction();
			showProject = prod.getType().getEpisodic();
			Integer id = SessionUtils.getInteger(Constants.ATTR_DEPARTMENT_ID);
			activeMask = SessionUtils.getDeptMask(prod, false);
			refreshList();
			setupSelectedItem(id);
			scrollToRow();
			restoreSortOrder();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(java.lang.Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		memberList.clear();
		if (department != null) {
			department.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_DEPARTMENT_ID, null);
			department = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			department = getDepartmentDAO().findById(id);
			if (department != null) {
				if (getItemList().indexOf(department) < 0) {
					// Can happen if switched projects in Commercial production.
					department = null; // Force use of default entry.
				}
			}
			if (department == null) {
				id = findDefaultId();
				if (id != null) {
					department = getDepartmentDAO().findById(id);
				}
			}
			SessionUtils.put(Constants.ATTR_DEPARTMENT_ID, id);
			if (department != null) {
				createMembersList();
			}
		}
		else {
			log.debug("new dept");
			SessionUtils.put(Constants.ATTR_DEPARTMENT_ID, null); // erase "new" flag
			department = new Department();
			department.setListPriority(findMaxListPriority()+1);
			department.setProduction(SessionUtils.getNonSystemProduction());
			department.setMask(Department.UNIQUE_DEPT_MASK);
		}
		if (department != null) {
			department.setSelected(true);
			department.setActive(activeMask.intersects(department.getMask()) || department.isUnique());
			forceLazyInit();
		}
	}

	/**
	 * Determine the Department.id value of the default Department to be
	 * selected.
	 *
	 * @return The Department.id value of the first Department in our current
	 *         list.
	 */
	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<Department> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	/**
	 * Reference fields that may be required during page rendering, to avoid
	 * LazyInitializationException`s.
	 */
	private void forceLazyInit() {
		if (department.getProduction() != null) {
			department.getProduction().isSystemProduction();
		}
	}

	/**
	 * Set the 'selected' flag on the given item in our list, which should be a
	 * Department.
	 *
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object,
	 *      boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Department)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Action method for the Edit button. Refresh the instance (in case someone
	 * has changed it recently) and switch to edit mode.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionEdit()
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		department = getDepartmentDAO().refresh(department);
		department.setActive(activeMask.intersects(department.getMask()) || department.isUnique());
		originalName = department.getName();
		getElement().setDescription(StringUtils.editHtml(getElement().getDescription()));
		forceLazyInit();
		return super.actionEdit();
	}

	/**
	 * Action method for the "Save" button. Validate the data then save the
	 * changes.
	 *
	 * @return null navigation string
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String actionSave() {
		if (getElement().getName() == null || getElement().getName().trim().length() == 0) {
			MsgUtils.addFacesMessage("Department.BlankName", FacesMessage.SEVERITY_ERROR);
			//setSelectedTab(TAB_DETAIL);
			return null;
		}
		try {
			getElement().setName(getElement().getName().trim());
			if (newEntry || ! department.getName().equals(originalName)) {
				// Note that if the name changed, it must be a custom dept, since we don't allow editing
				// the names of system (or system-cloned) departments.
				for (Department dept : (List<Department>)getItemList()) {
					if (dept.getName().equalsIgnoreCase(department.getName())) {
						MsgUtils.addFacesMessage("Department.DuplicateName", FacesMessage.SEVERITY_ERROR);
						//setSelectedTab(TAB_DETAIL);
						return null;
					}
				}
			}
			getElement().setDescription(StringUtils.saveHtml(getElement().getDescription()));
			Production prod = SessionUtils.getNonSystemProduction();

			if (prod != null && department.getActive() != (department.getMask().intersects(activeMask))) {
				if (department.getActive()) {
					activeMask.or(department.getMask());
				}
				else {
					activeMask.andNot(department.getMask());
				}
				DepartmentUtils.updateMask(activeMask);
			}

			if (! newEntry) {
				if (department.getProduction().isSystemProduction()) {
					// Create a custom copy (proxy) of the System department, for this production
					department.setStandardDeptId(department.getId());
					department.setId(null);
					department.setProduction(prod);
					if (prod != null && prod.getType().hasPayrollByProject()) {
						department.setProject(SessionUtils.getCurrentProject());
					}
					getDepartmentDAO().save(department);
					refreshList();
				}
				else {
					// saving an existing custom department
					getDepartmentDAO().attachDirty(department);
					updateItemInList(getElement());
					getElement().setSelected(true);
				}
				forceLazyInit();
			}
			else {
				department.setProduction(prod);
				if (prod != null && prod.getType().hasPayrollByProject()) {
					department.setProject(SessionUtils.getCurrentProject());
				}
				getDepartmentDAO().attachDirty(department);
				refreshList();
				scrollToRow();
			}
			setupSelectedItem(department.getId());
			SessionUtils.put(Constants.ATTR_DEPARTMENT_ID, department.getId());
			return super.actionSave();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		if (getElement() == null || isNewEntry()) {
			department = null;
			refreshList();
			setSelectedRow(0);
			setupSelectedItem(null); // pick a default item to select
		}
		else {
			department = getDepartmentDAO().refresh(department);
			setupSelectedItem(getElement().getId());
		}
		return null;
	}

	/**
	 * The Action method of the "Delete" button. Verify that the Department may
	 * be deleted -- we prevent deletion if the Department is "in use" by other
	 * objects, such as Callsheet`s, WeeklyTimecard`s, etc. If the Department
	 * may be deleted, then the standard delete prompt is displayed (handled by
	 * invoking our superclass).
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionDelete()
	 * @return null navigation string.
	 */
	@Override
	public String actionDelete() {
		try {
			if (! calculateDeletable(department)) {
				return null;
			}
			return super.actionDelete();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method of the "OK" button on the Delete prompt pop-up. Delete the
	 * Department, as long as it isn't a standard Department. (We shouldn't be
	 * able to get to this point for a standard Department, but we check, just
	 * in case!) Note that deleting a Department also deletes any custom Role`s
	 * that were defined for that Department.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionDeleteOk()
	 * @return null navigation string
	 */
	@Override
	public String actionDeleteOk() {
		try {
			department = getDepartmentDAO().findById(department.getId()); // refresh
			if (! department.getProduction().isSystemProduction()) {
				// delete the department and any custom Roles
				getDepartmentDAO().remove(department);
			}
			SessionUtils.put(Constants.ATTR_DEPARTMENT_ID, null);
			department = null;
			int n = getSelectedRow();
			refreshList();
			setupSelectedItem(getRowId(n));
			addClientResizeScroll();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Value Change Listener for check box selecting whether to
	 * show "all members" or "project-only members".
	 */
	public void listenShowProject(ValueChangeEvent event) {
		try {
			Boolean b = (Boolean)event.getNewValue();
			if (b != null) {
				showProject = b;
				createMembersList();
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
	}

	public void listenActiveCheckbox(ValueChangeEvent event) {
		Boolean b = (Boolean)event.getNewValue();
		if (b != null && department != null) {
			department.setShowOnCallsheet(b);
			department.setShowOnDpr(b);
			department.setActive(b);
		}
	}

	/**
	 * Action method for the "Change Order" button -- just opens the
	 * "change order" pop-up dialog.
	 *
	 * @return null navigation string
	 */
	public String actionOpenChangeOrder() {
		if (! editMode) {
			DepartmentOrderBean bean = DepartmentOrderBean.getInstance();
			bean.show(this, ACT_DEPT_ORDER, null, null, null,
					createDeptOrderList(), activeMask);
			showChangeOrder = true;
			addClientResizeScroll();
		}
		return null;
	}

	@Override
	protected void refreshList() {
		createDepartmentList();
		setSelectedRow(-1);
		sort();
	}

	/**
	 * Create the list of Department`s to display. This includes both
	 * system-defined and custom departments. Note that the "LS Admin"
	 * department is omitted from the list unless the current user is an Admin.
	 */
	private void createDepartmentList() {
		Production prod = SessionUtils.getProduction();
		Project project = null;
		if (prod.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
		// Note that our list needs the custom department ids
		departmentList = getDepartmentDAO().findByProductionProjectComplete(prod, project);
		boolean isAdmin = AuthorizationBean.getInstance().isAdmin();
		for (Iterator<Department> iter = departmentList.iterator(); iter.hasNext(); ) {
			Department dept = iter.next();
			dept.setActive(dept.getMask().intersects(activeMask) || dept.isUnique());
			if (! isAdmin) {
				if (dept.getId() == Constants.DEPARTMENT_ID_LS_ADMIN ||
						(dept.getStandardDeptId() != null &&
						dept.getStandardDeptId() == Constants.DEPARTMENT_ID_LS_ADMIN)) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Create the list of department members to be displayed.
	 */
	private void createMembersList() {
		Set<DeptMember> deptMbrs = new HashSet<DeptMember>();
		List<ProjectMember> mbrs;
		Department dept = department;
		if (department.getStandardDeptId() != null) {
			dept = getDepartmentDAO().findById(department.getStandardDeptId());
		}
		if (showProject) {
			mbrs = ProjectMemberDAO.getInstance()
					.findByProjectAndDepartment(SessionUtils.getCurrentProject(), dept);
		}
		else {
			mbrs = ProjectMemberDAO.getInstance().findByProductionAndDepartment(dept);
		}
		for (ProjectMember mbr : mbrs) { // add to Set to eliminate duplicates
			deptMbrs.add(new DeptMember(mbr));
		}
		memberList.clear();
		memberList.addAll(deptMbrs); // move Set into List
		sortedMemberList.forceSort(); // make sure list get sorted next time it is needed
	}

	/**
	 * Create the list of SelectItem entries used in the "change order" pop-up
	 * dialog box. There will be one entry for each Department in the current
	 * Department list; the value is the Department.id, and the label is the
	 * Department.name. We need to get a fresh copy of the department list to be
	 * sure it hasn't been changed by someone else since we last loaded it.
	 * <p>
	 * To prevent two people from trying to change the order simultaneously, we
	 * need to add code to lock (and later, unlock) the list while this is in
	 * progress.
	 */
	private List<Department> createDeptOrderList() {
		// TODO lock the department list for this production!
		refreshList();		// get a fresh copy of the department list from the database.
		List<Department> deptList = new ArrayList<Department>(departmentList);
		Collections.sort(deptList, piorityComparator);
		for (Department dept : deptList) {
			dept.setActive(dept.getMask().intersects(activeMask));
			dept.getProduction().getId();
		}
		return deptList;
	}

	/**
	 * Determine if the given Department may be deleted; if not, issue one or
	 * more messages indicating why not.
	 *
	 * @param dept The Department to be tested.
	 * @return True iff it is OK to delete the given Department.
	 */
	private boolean calculateDeletable(Department dept) {
		boolean bRet = true;
		if (dept.getProduction().isSystemProduction()) {
			MsgUtils.addFacesMessage("Department.NotDeletable.System", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (EmploymentDAO.getInstance().existsInProductionAndDepartment(department)) {
			// department has members
			MsgUtils.addFacesMessage("Department.NotDeletable.Member", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (DeptCallDAO.getInstance().referencesDept(dept)) {
			// department is used in call sheets (DeptCall objects)
			MsgUtils.addFacesMessage("Department.NotDeletable.Callsheet", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (ReportTimeDAO.getInstance().referencesDept(dept)) {
			// department is used in DPRs (old-style TimeCard objects)
			MsgUtils.addFacesMessage("Department.NotDeletable.Dpr", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
//		if (StartFormDAO.getInstance().referencesDept(dept)) {
//			// department is referenced on StartForm`s
//			MsgUtils.addFacesMessage("Department.NotDeletable.StartForm", FacesMessage.SEVERITY_ERROR);
//			bRet = false;
//		}
		if (WeeklyTimecardDAO.getInstance().referencesDept(dept)) {
			// department is referenced on WeeklyTimecard`s
			MsgUtils.addFacesMessage("Department.NotDeletable.Timecard", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (ApprovalAnchorDAO.getInstance().referencesDept(dept)) {
			// department is referenced by ApprovalAnchor`s (has departmental approvers assigned)
			MsgUtils.addFacesMessage("Department.NotDeletable.Approver", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		return bRet;
	}

	/**
	 * @return The highest "list priority" value in the existing list of
	 *         departments. This is used to determine the priority to assign a
	 *         newly-created department.
	 */
	private int findMaxListPriority() {
		int pri = 0;
		for ( Department dept : departmentList) {
			if (dept.getListPriority() > pri) {
				pri = dept.getListPriority();
			}
		}
		return pri;
	}

	/**
	 * For the current element in the main (left-hand) list, send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	private void scrollToRow() {
		scrollToRow(getElement());
	}

	/**
	 * Return the id of the item that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {
		Object item;
		return ((item=getRowItem(row)) == null ? null : ((Department)item).getId());
	}

	/**
	 * Comparator needed to handle sorting of the left-hand list (of
	 * Department`s) in our page. This passes the current setting of the sort
	 * column name and ascending/descending flag to
	 * {@link Department#compareTo(Department,String,boolean)}.
	 *
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<Department> getComparator() {
		Comparator<Department> comparator = new Comparator<Department>() {
			@Override
			public int compare(Department d1, Department d2) {
				return d1.compareTo(d2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * A Department comparator used to sort Department`s by priority. Used by
	 * the "Change Order" pop-up dialog.
	 */
	private static Comparator<Department> piorityComparator = new Comparator<Department>() {
		@Override
		public int compare(Department d1, Department d2) {
			return d1.compareTo(d2, Department.SORT_KEY_PRIORITY, true);
		}
	};

	/**
	 * Sorts the list of Department members displayed in the Detail (right-hand
	 * side) mini-tab.
	 *
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List,
	 *      com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void sort(@SuppressWarnings("rawtypes") List list, SortableList sortableList) {
		if (sortableList.getSortColumnName().equals(DeptMember.SORT_KEY_NAME)) {
			if (sortableList.isAscending()) {
				Collections.sort(list, DeptMember.nameComparator);
			}
			else {
				Collections.sort(list, Collections.reverseOrder(DeptMember.nameComparator));
			}
		}
		else {
			if (sortableList.isAscending()) {
				Collections.sort(list, DeptMember.roleComparator);
			}
			else {
				Collections.sort(list, Collections.reverseOrder(DeptMember.roleComparator));
			}
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		return true; // make columns default to ascending sort
	}

	/**
	 * Intercept popup confirmations so we can reset our 'showChangeOrder' flag.
	 *
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String ret = super.confirmOk(action); // superclass distributes common actions
		if (showChangeOrder) {
			DepartmentOrderBean bean = DepartmentOrderBean.getInstance();
			showChangeOrder = bean.isVisible();
			if (! showChangeOrder) { // dialog box finished
				refreshList();
				scrollToRow();
			}
		}
		return ret;
	}

	/**
	 * Intercept popup cancellations so we can reset our 'showChangeOrder' flag.
	 *
	 * @see com.lightspeedeps.web.view.ListView#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		showChangeOrder = false;
		refreshList();
		setSelectedRow(getElement());
		return super.confirmCancel(action);
	}

	/** See {@link #showChangeOrder}. */
	public boolean getShowChangeOrder() {
		return showChangeOrder;
	}
	/** See {@link #showChangeOrder}. */
	public void setShowChangeOrder(boolean showChangeOrder) {
		this.showChangeOrder = showChangeOrder;
	}

	/** See {@link #department}. */
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department dept) {
		department = dept;
	}

	/** See {@link #department}.  This method is typically used in JSP
	 * pages, providing a common syntax (bean.element) across many of our
	 * pages that deal with lists of items. */
	public Department getElement() {
		return department;
	}

	/** See {@link #departmentList}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return departmentList;
	}

	/** See {@link #showProject}. */
	public boolean getShowProject() {
		return showProject;
	}
	/** See {@link #showProject}. */
	public void setShowProject(boolean showProject) {
		this.showProject = showProject;
	}

	/** See {@link #sortedMemberList}. */
	public SortableListImpl getSortedMemberList() {
		return sortedMemberList;
	}
	/** See {@link #sortedMemberList}. */
	public void setSortedMemberList(SortableListImpl sortedMemberList) {
		this.sortedMemberList = sortedMemberList;
	}

	/**
	 * A class to hold the data presented in the list of Department members.
	 */
	public static class DeptMember implements Serializable, Comparable<DeptMember> {
		/** */
		private static final long serialVersionUID = 9211197355034218952L;
		static final String SORT_KEY_NAME = "name";
		static final String SORT_KEY_ROLE = "role";

		String name;
		String roleName;
		boolean selected;

		public DeptMember(ProjectMember mbr) {
			name = mbr.getEmployment().getContact().getUser().getLastNameFirstName();
			roleName = mbr.getEmployment().getRole().getName();
		}

		/** See {@link #name}. */
		public String getName() {
			return name;
		}
		/** See {@link #name}. */
		public void setName(String name) {
			this.name = name;
		}

		/** See {@link #roleName}. */
		public String getRoleName() {
			return roleName;
		}
		/** See {@link #roleName}. */
		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}

		/** See {@link #selected}. */
		public boolean isSelected() {
			return selected;
		}
		/** See {@link #selected}. */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public static Comparator<DeptMember> nameComparator = new Comparator<DeptMember>() {
			@Override
			public int compare(DeptMember one, DeptMember two) {
				int ret = one.name.compareTo(two.name);
				if (ret == 0) {
					ret = one.roleName.compareTo(two.roleName);
				}
				return ret;
			}
		};

		public static Comparator<DeptMember> roleComparator = new Comparator<DeptMember>() {
			@Override
			public int compare(DeptMember one, DeptMember two) {
				int ret = one.roleName.compareTo(two.roleName);
				if (ret == 0) {
					ret = one.name.compareTo(two.name);
				}
				return ret;
			}
		};

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((roleName == null) ? 0 : roleName.hashCode());
			return result;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DeptMember other = (DeptMember)obj;
			if (name == null) {
				if (other.name != null)
					return false;
			}
			else if (!name.equals(other.name))
				return false;
			if (roleName == null) {
				if (other.roleName != null)
					return false;
			}
			else if (!roleName.equals(other.roleName))
				return false;
			return true;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(DeptMember other) {
			if (other == null) {
				return 1;
			}
			int ret = getName().compareTo(other.getName());
			if (ret == 0) {
				ret = getRoleName().compareTo(other.getRoleName());
			}
			return ret;
		}

	}

	private DepartmentDAO getDepartmentDAO() {
		if (departmentDAO == null) {
			departmentDAO = DepartmentDAO.getInstance();
		}
		return departmentDAO;
	}

	public int getSelectedTab() {
		return selectedTab;
	}

	public void setSelectedTab(int selectedTab) {
		this.selectedTab = selectedTab;
	}
	
}
