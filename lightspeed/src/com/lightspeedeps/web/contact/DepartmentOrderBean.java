/**
 * DepartmentOrderBean.java
 */
package com.lightspeedeps.web.contact;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.contact.DepartmentBean.DeptMember;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.SelectableList;
import com.lightspeedeps.web.view.SelectableListHolder;
import com.lightspeedeps.web.view.SortableList;

/**
 * This is the backing bean for the "Order Departments" dialog box. This dialog
 * also allows for marking departments as 'active' or not within the current
 * Production (or Project, for Commercial productions).
 */
@ManagedBean
@ViewScoped
public class DepartmentOrderBean extends PopupBean implements SelectableListHolder {
	private static final Log log = LogFactory.getLog(DepartmentOrderBean.class);

	/** */
	private static final long serialVersionUID = 1L;

	/** The list of departments used in the "change order" popup. */
	private final List<Department> departmentOrderList = new ArrayList<>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	private final SelectableList selectDeptList =
			new SelectableList(this, departmentOrderList, DeptMember.SORT_KEY_NAME, true);

	private transient DepartmentDAO departmentDAO;

	/** The Department mask currently in effect, either for the current Production,
	 * or the current Project. */
	private BitMask activeMask;

	/**
	 * Default constructor.
	 */
	public DepartmentOrderBean() {
		log.debug("");
	}

	/**
	 * @return the current instance of this bean.
	 */
	public static DepartmentOrderBean getInstance() {
		return (DepartmentOrderBean)ServiceFinder.findBean("departmentOrderBean");
	}

	/**
	 * Display our dialog box.
	 *
	 * @param holder Typically our caller, used for call-backs.
	 * @param act An integer passed in call-backs to discriminate among various
	 *            actions the caller might manage.
	 * @param titleId The message-id to use to look up the dialog box title.
	 * @param buttonOkId The message-id to use to look up the "Ok" button text.
	 * @param buttonCancelId The message-id to use to look up the "Cancel"
	 *            button text.
	 * @param deptOrderList The list of departments to display.
	 * @param activeMsk The mask of active departments, used to set the
	 *            check-boxes in the displayed list of departments.
	 */
	public void show(PopupHolder holder, int act, String titleId, String buttonOkId, String buttonCancelId,
			List<Department> deptOrderList, BitMask activeMsk) {
		init();
		departmentOrderList.clear();
		departmentOrderList.addAll(deptOrderList);
		activeMask = activeMsk;
		super.show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/**
	 * Initialize all our fields. It's necessary to do this each time the dialog
	 * box is displayed on the desktop since the bean is not re-instantiated for
	 * each display.
	 */
	private void init() {
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionOk()
	 */
	@Override
	public String actionOk() {
		unselectCurrent();
		if (actionSaveChangeOrder()) {
			return super.actionOk();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		unselectCurrent();
		return super.actionCancel();
	}

	/**
	 * Action method for the "Check All" button -- sets all departments'
	 * "active" check-box on.
	 * @return null navigation string
	 */
	public String actionCheckAll() {
		for (Department dept : departmentOrderList) {
			dept.setActive(true);
		}
		return null;
	}

	/**
	 * Action method for the "Un-check All" button -- sets all departments'
	 * "active" check-box off, except for 'unique' departments -- custom
	 * departments that don't correspond to a system department.
	 *
	 * @return null navigation string
	 */
	public String actionUncheckAll() {
		for (Department dept : departmentOrderList) {
			if (! dept.isUnique()) {
				dept.setActive(false);
			}
		}
		return null;
	}

	/**
	 * Action method for the "up arrow" control. This moves the currently
	 * selected Department name up one position in the list. If the Department
	 * is already first in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveDeptUp() {
		int ix = findOrderIndex();
		if (ix > 0) {
			Department dept = departmentOrderList.remove(ix);
			departmentOrderList.add(ix-1, dept);
			getSelectDeptList().setSelectedRow(ix-1);
			showSelected(dept);
		}
		return null;
	}

	/**
	 * Action method for the "down arrow" control. This moves the currently
	 * selected Department name down one position in the list. If the Department
	 * is already last in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveDeptDown() {
		int ix = findOrderIndex();
		if (ix >= 0 && ix < departmentOrderList.size()-1) {
			Department dept = departmentOrderList.remove(ix);
			departmentOrderList.add(ix+1, dept);
			getSelectDeptList().setSelectedRow(ix+1);
			showSelected(dept);
		}
		return null;
	}

	/**
	 * Find the index of the currently selected Department (on the
	 * "change order" pop-up dialog) within the selection list.
	 *
	 * @return The zero-origin index of the selected Department, or -1 if it is
	 *         not found (which should not happen!).
	 */
	private int findOrderIndex() {
		int ix = getSelectDeptList().getSelectedRow();
		if (ix > departmentOrderList.size()) {
			ix = -1;
		}
		return ix;
	}

	/**
	 * Method to save changes from on the "change order" pop-up dialog.
	 * Re-assign the department list priorities to match the user's selected
	 * order. Note that this requires creating custom Department objects if the
	 * user's ordering is such that the "list priority" of the system Department
	 * does not match the priority specified by the user.
	 *
	 * @return True iff no exception occurred during save processing.
	 */
	public boolean actionSaveChangeOrder() {
		boolean ret = false;
		try {
			Production prod = SessionUtils.getNonSystemProduction();
			int order = 1;
			boolean active;
			boolean maskUpdated = false;
			for (Department dept : departmentOrderList) {
				active = dept.getActive();
				dept = getDepartmentDAO().refresh(dept);
				if (! dept.getListPriority().equals(order)) {
					// Order changed
					if (dept.getProduction().isSystemProduction()) {
						// create custom one if currently a (shared) system department
						Department customDept = dept.clone();
						customDept.setListPriority(order);
						customDept.setProduction(prod);
						if (prod.getType().hasPayrollByProject()) {
							customDept.setProject(SessionUtils.getCurrentProject());
						}
						customDept.setStandardDeptId(dept.getId());
						getDepartmentDAO().save(customDept);
					}
					else { // just update the existing custom entry
						dept.setListPriority(order);
						getDepartmentDAO().attachDirty(dept);
					}
				}
				if ((!dept.isUnique()) && active != (dept.getMask().intersects(activeMask))) {
					maskUpdated = true;
					if (active) {
						activeMask.or(dept.getMask());
					}
					else {
						activeMask.andNot(dept.getMask());
					}
				}
				order++;
			}
			if (maskUpdated && prod != null) {
				if (prod.getType().isAicp()) {
					Project proj = SessionUtils.getCurrentProject();
					if (proj != null) {
						log.debug("old mask=" + proj.getDeptMask() + ", new=" + activeMask);
						proj.setDeptMask(activeMask);
						ProjectDAO.getInstance().attachDirty(proj);
					}
				}
				else {
					log.debug("old mask=" + prod.getDeptMaskB() + ", new=" + activeMask);
					prod.setDeptMaskB(activeMask);
					ProductionDAO.getInstance().attachDirty(prod);
				}
			}
			ret = true;
			// TODO UN-lock the department list for this production!
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return ret;
	}

	/**
	 * Value Change Listener for check box selecting whether this department is
	 * used in this Production or Project ("active").
	 */
	public void listenCheckActive(ValueChangeEvent event) {
		try {
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List, com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void sort(List list, SortableList sortableList) {
		// we don't support sorting in this popup
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		return true;
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#selectableRowChanged()
	 */
	@Override
	public void selectableRowChanged() {
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setSelectableSelected(java.lang.Object, boolean)
	 */
	@Override
	public void setSelectableSelected(Object item, boolean b) {
		try {
			Department dept = (Department)item;
			dept.setSelected(b);
		}
		catch (Exception e) {
		}
	}

	/**
	 * Turn off the "select" flag in the currently selected department. We do
	 * this before we exit, since the "select" field in the Department is also
	 * used by the main page in the left-hand list.
	 */
	private void unselectCurrent() {
		int n = getSelectDeptList().getSelectedRow();
		if (n > 0) {
			Department dept = getDepartmentOrderList().get(n);
			setSelectableSelected(dept, false);
		}
	}

	/**
	 * Make the department show up as selected using ace rowStateMap.
	 * @param dept
	 */
	private void showSelected(Department dept) {
		getSelectDeptList().getStateMap().clear();
		getSelectDeptList().selectRowState(dept);
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setupSelectableItem(java.lang.Integer)
	 */
	@Override
	public void setupSelectableItem(Integer id) {
		// Not used at this time.
	}

	/** See {@link #selectDeptList}. */
	public SelectableList getSelectDeptList() {
		return selectDeptList;
	}

	/** See {@link #departmentOrderList}. */
	public List<Department> getDepartmentOrderList() {
		return departmentOrderList;
	}

	/**
	 * @return An instance of DepartmentDAO.
	 */
	private DepartmentDAO getDepartmentDAO() {
		if (departmentDAO == null) {
			departmentDAO = DepartmentDAO.getInstance();
		}
		return departmentDAO;
	}

}
