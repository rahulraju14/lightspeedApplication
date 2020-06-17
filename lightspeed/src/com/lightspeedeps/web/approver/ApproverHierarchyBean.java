/**
 * File: ApproverHierarchyBean.java
 */
package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ApprovalAnchorDAO;
import com.lightspeedeps.dao.ApproverDAO;
import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.ApprovalAnchor;
import com.lightspeedeps.model.Approver;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Role;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.ApproverChainRoot;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.ChangeUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.View;

/**
 * The backing bean for the Timecard Approver Hierarchy page.
 */
@ManagedBean
@ViewScoped
public class ApproverHierarchyBean extends View implements Serializable  {
	/** */
	private static final long serialVersionUID = - 8042146383090558112L;

	private static final Log log = LogFactory.getLog(ApproverHierarchyBean.class);

	private static final int ACT_REMOVE_DEPT = 10;
	private static final int ACT_REMOVE_PROD = 11;
	private static final int ACT_REMOVE_PROJECT = 12;

	private static final int PROD_APPROVER = 1;
	private static final int PROJECT_APPROVER = 2;
	private static final int DEPT_APPROVER = 3;

	/** The minimum time, in milliseconds, that must pass between us refreshing
	 * the page with a new department, and receiving a ValueChangeEvent for the
	 * TimeKeeper drop-down.  If the delay is less than this, the ValueChangeEvent
	 * is considered invalid and is ignored.  We need to do this because JSF or ICEfaces
	 * generates invalid ChangeValueEvents when the user switches too quickly between
	 * department entries.  (rev 2.0.3763) */
	private static final long MIN_CHANGE_DELAY = 500;

	/** true if the user is editing the Production Approval list. */
	private boolean productionSelected;

	/** true if the user is editing the Project Approval list. */
	private boolean projectSelected;

	/** true iff the current productino is AICP (Commercial). */
	private final boolean aicp;

	/** The current Production. */
	private Production production;

	/** The current Project; only used for Commercial productions. */
	private Project project;

	/** The database id of the currently selected department. */
	private Integer departmentId = -1; // start with "Production" entry selected.

	/** The currently selected Department within the Department list. */
	private Department department;

	/** The List of SelectItem's of Department's. The value field for each
	 * entry is the Department.id field, except for the first entry, which
	 * has a value of zero, and is used to select the Production Approval list. */
	private List<SelectItem> departmentItems;

	/** The database id of the ProjectMember linked to the currently
	 * selected Production Member list entry. */
	private Integer memberId = -1;

	/** The list of SelectItem's of production members. The label includes both
	 * the contact name and role. The value is the Contact.id field.*/
	private List<SelectItem> memberItems;

	/** The database id of the ProjectMember linked to the currently
	 * selected department-approver list entry. */
	private Integer deptApproverId;

	/** The List of SelectItem's of departmental approvers for the current department. The
	 * value field for each entry is the Approver.id field. */
	private List<SelectItem> deptApproverItems = new ArrayList<>();

	/** The Contact ids of the approvers present in either the Production,
	 * Project or Department approver list (whichever is currently selected).
	 * This is used to filter the "available members" list (right-hand list) so
	 * that existing approvers are not listed as available. */
	private final List<Integer> approverContactIds = new ArrayList<>();

	/** The database id of the Approver object linked to the currently
	 * selected production-approver list entry.	*/
	private Integer productionApproverId;

	/** The List of SelectItem's of Production approvers.  The value
	 * field for each entry is the Approver.id field. */
	private List<SelectItem> productionApproverItems;

	/** The List of SelectItem's of Project approvers.  The value
	 * field for each entry is the Approver.id field. */
	private List<SelectItem> projectApproverItems;

	/** The database id of the Approver linked to the currently
	 * selected project-approver list entry.	*/
	private Integer projectApproverId;

	/** The database id of the Approver object currently selected; this is
	 * used to hold the id while we prompt for confirmation of removing an
	 * Approver who has timecards in their approval queue. */
	private Integer selectedApproverId;

	/** To display 'All Production Members' or 'Department Members'*/
	private List<SelectItem> displayItems = new ArrayList<>();
	private Integer displayItemId = new Integer(0);
	private final static int DISPLAY_ALL_MEMBERS = 0;
	//private final static int DISPLAY_DEPT_MEMBERS = 1;

	/** To display 'Dept Time Entry'*/
	private List<SelectItem> deptTimeEntry = new ArrayList<>();
	private Integer deptTimeEntryId = new Integer(0);

	/** Date we last updated the page. */
	Date refreshTime = new Date();

	/** true if the current User is allowed to see "Admin" roles listed. */
	private final boolean showAdmin;

	/** Display Project Approvers list. Only true for Commercial productions. */
	private boolean showProjectApprovers;

	/** Display Production Approvers list; at some time we may support
	 * setting this to false for Commercial productions. */
	private boolean showProductionApprovers;

	private transient ContactDAO contactDAO;
	private transient ApproverDAO approverDAO;


	/**
	 * Default constructor.
	 */
	public ApproverHierarchyBean() {
		super("Hierarchy."); // set message prefix

		AuthorizationBean authBean = AuthorizationBean.getInstance();
		showAdmin = authBean.isAdmin();
		production = SessionUtils.getNonSystemProduction();

		aicp = production.getType().hasPayrollByProject();

		showProductionApprovers = true;
		if (aicp) {
			showProjectApprovers = true;
			project = SessionUtils.getCurrentProject();
		}
		else {
			showProjectApprovers = false;
		}

		boolean allowProdSelection = (!aicp) || authBean.hasPermission(Permission.VIEW_ALL_PROJECTS);

		createDepartmentList(allowProdSelection);
		createTopApproverLists(false);

		createDeptTimeEntry(department);
		createDisplayItems();

		if (allowProdSelection) {
			productionSelected = true; //Production (All Dept) is selected
		}
		else if (aicp) {
			projectSelected = true;
		}
		else {
			// first department will be selected
		}

		//Populate the approver contact list to filter in member list box
		createApproverContactIds();
		createMemberItems(null, productionSelected && aicp);
	}

	/**
	 * ValueChangeListener for "Display:" drop-down list.  Update the
	 * "production members" list based on the user's choice.
	 * @param event JSP event generated by framework
	 */
	public void listenDisplayChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try{
			log.debug("in displayValueChange() new val = " + event.getNewValue()
					+ "\n ID =" +  event.getComponent().getId());
			refreshMemberItems();
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * ValueChangeListener for the department list
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		try{
			log.debug("in processDeptValueChange() new val = " + event.getNewValue()
					+ ", ID =" +  event.getComponent().getId());

			Integer id = (Integer)event.getNewValue();
			deptApproverItems.clear();
			if (id != null) {
				departmentId = id;
				productionApproverId = null;
				projectApproverId = null;
				projectSelected = false;
				productionSelected = false;
				if (departmentId <= 0) {
					if (departmentId < 0) {
						productionSelected = true;
					}
					else {
						projectSelected = true; // Project Approvers list is selected
					}
					createDeptTimeEntry(null);
				}
				else {
					department = DepartmentDAO.getInstance().findById(departmentId);
					createDeptApproverList(department, false);
					deptApproverId = null;
					createDeptTimeEntry(department);
				}
				refreshMemberItems();
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * valueChangeListener for Dept Time Entry
	 */
	public void listenTimeEntryChange(ValueChangeEvent event) {
		log.debug("in timeEntryValueChange() new val = " + event.getNewValue()
				+ ", ID =" +  event.getComponent().getId());
		try {
			Integer id = (Integer)event.getNewValue();
			if (id != null) {
				long diff = (new Date()).getTime() - refreshTime.getTime();
				log.debug("old=" + event.getOldValue() + ", new=" + event.getNewValue() +
						", diff=" + diff);
				if (diff < MIN_CHANGE_DELAY) {
					log.info(">>>>>>>>>>>>>>>>>>>>>>>> CHANGE INVALID (too soon) >>>>>>>>>>>>>>>>>> diff=" + diff);
					return;
				}
				// newValue is Contact id - Integer; -1 = "none selected" (1st entry in drop-down list)
				deptTimeEntryId = id;

				DepartmentDAO departmentDAO = DepartmentDAO.getInstance();
				// Is there a customized Department object already?
				Department customDept = departmentDAO.findByProductionProjectDept(production, project, department.getName());
				if (customDept == null && deptTimeEntryId > 0) {
					// create a custom Department object to hold the time-keeper value
					customDept = department.clone();
					customDept.setProduction(production);
					customDept.setProject(project);
					customDept.setStandardDeptId(department.getId());
					departmentDAO.save(customDept);
				}
				if (customDept != null) {
					Contact keeper = null;
					if (id > 0) {
						keeper = getContactDAO().findById(id);
					}
					customDept.setTimeKeeper(keeper);
					departmentDAO.attachDirty(customDept);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Action method for the "add" button. Get the ProjectMember entry from the
	 * members list, identified by 'memberId', and add it to the bottom of the
	 * current approval list.
	 *
	 * @return null navigation string
	 */
	public String actionAddApprover() {
		try {
			log.debug("productionSelected = " + productionSelected);
			SelectItem item = extractItem(memberItems, memberId);
			log.debug(item);
			if (item != null) {
				if (productionSelected) {
					addProductionApprover(item);
					createTopApproverLists(true);
					productionApproverId = null;
				}
				else if (projectSelected) {
					addProjectApprover(item);
					createTopApproverLists(true);
					projectApproverId = null;
				}
				else {
					addDepartmentApprover(item);
					createDeptApproverList(department, true);
					deptApproverId = null;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "remove" button.
	 * Get the ProjectMember identified by 'memberId', remove it from
	 * the current approval list, and add it to the available members list.
	 * @return null navigation string
	 */
	public String actionRemoveApprover() {
		try {
			SelectItem item;
			boolean bRet = false;
			if (productionSelected) {
				item = findItem(productionApproverItems, productionApproverId);
				if (item != null) {
					bRet = removeProductionApprover(productionApproverId, false);
				}
			}
			else if (projectSelected) {
				item = findItem(projectApproverItems, projectApproverId);
				if (item != null) {
					bRet = removeProjectApprover(projectApproverId, false);
				}
			}
			else {
				item = findItem(deptApproverItems, deptApproverId);
				if (item != null) {
					bRet = removeDepartmentApprover(deptApproverId, false);
				}
			}
			if (! bRet) { // maybe another user updated approver chains
				refreshMemberItems();
			}
			if (memberItems.size() > 1) {
				memberId = null; // clear member list selection
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "down" button, which moves the approver into an
	 * earlier position in the sequence of approvers. Find the currently
	 * selected entry and move it one position earlier in the list, i.e.,
	 * towards the first-approver end of the list.
	 *
	 * @return null navigation string
	 */
	public String actionMoveApproverEarlier() {
		try {
			boolean changed = false;
			if (productionSelected) {
				if (productionApproverId != null) {
					changed = actionmoveEarlier(productionApproverItems, productionApproverId);
				}
				createTopApproverLists(changed);
			}
			else if (projectSelected) {
				if (projectApproverId != null) {
					changed = actionmoveEarlier(projectApproverItems, projectApproverId);
				}
				createTopApproverLists(changed);
			}
			else {
				if (deptApproverId != null) {
					changed = actionmoveEarlier(deptApproverItems, deptApproverId);
				}
				createDeptApproverList(department, changed);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "up" button, which moves the approver into a later
	 * position in the sequence of approvers. Find the currently selected entry
	 * and move it one position later in the list, i.e., toward the final-approver
	 * end of the list.
	 *
	 * @return null navigation string
	 */
	public String actionMoveApproverLater() {
		try {
			boolean changed = false;
			if (productionSelected) {
				if (productionApproverId != null) {
					changed = actionmoveLater(productionApproverItems, productionApproverId);
				}
				createTopApproverLists(changed);
			}
			else if (projectSelected) {
				if (projectApproverId != null) {
					changed = actionmoveLater(projectApproverItems, projectApproverId);
				}
				createTopApproverLists(changed);
			}
			else {
				if (deptApproverId != null) {
					changed = actionmoveLater(deptApproverItems, deptApproverId);
				}
				createDeptApproverList(department, changed);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the OK (or equivalent) button on a confirmation
	 * pop-up dialog.  We put up confirmations when the user is attempting
	 * to remove an approver from the chain, and that approver has timecards
	 * waiting for their approval.
	 * <p>
	 * @see com.lightspeedeps.web.view.View#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		try {
			switch(action) {
			case ACT_REMOVE_DEPT:
				removeDepartmentApprover(selectedApproverId, true);
				break;
			case ACT_REMOVE_PROD:
				removeProductionApprover(selectedApproverId, true);
				break;
			case ACT_REMOVE_PROJECT:
				removeProjectApprover(selectedApproverId, true);
				break;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			errorReset();
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Add a new Production Approver to the end of the existing production
	 * chain. The item's contact will be added as the first approver if no chain
	 * exists yet.
	 *
	 * @param item The SelectItem representing the Approver. The value field
	 *            holds the Contact.id of the person.
	 */
	private void addProductionApprover(SelectItem item) {
		addApprover(production, item);
	}

	/**
	 * Add a new Project Approver to the end of the existing Project approval
	 * chain. The item's contact will be added as the first approver if no chain
	 * exists yet.
	 *
	 * @param item The SelectItem representing the Approver. The value field
	 *            holds the Contact.id of the person.
	 */
	private void addProjectApprover(SelectItem item) {
		addApprover(project, item);
	}


	/**
	 * Add a new Approver to the end of the given approval chain. The item's
	 * contact will be added as the first approver if no chain exists yet.
	 *
	 * @param item The SelectItem representing the Approver. The value field
	 *            holds the Contact.id of the person.
	 */
	private void addApprover(ApproverChainRoot root, SelectItem item) {
		Integer id = (Integer)item.getValue();
		Contact contact = getContactDAO().findById(id);
		if (contact != null) {
			getContactDAO().attachClean(contact);
			contact.setPermissionMask(contact.getPermissionMask() | Permission.APPROVE_TIMECARD.getMask());
			Approver approver = root.getApprover();
			Approver priorApprover = null;
			while (approver != null) {
				priorApprover = approver;
				approver = approver.getNextApprover();
			}
			// create new Approver object
			approver = new Approver(contact, null, true);
			getApproverDAO().save(approver);

			// add to chain
			if (priorApprover == null) { // empty chain
				root.setApprover(approver);
				getContactDAO().attachDirty(root);
			}
			else {
				priorApprover.setNextApprover(approver);
				getApproverDAO().attachDirty(priorApprover);
			}
		}
	}

	/**
	 * Add a new Department Approver as the last approver for that department.
	 * If this Department has no approvers, it may not have an ApprovalAnchor
	 * yet; in that case we'll create one.
	 *
	 * @param item The SelectItem whose value is the contact.id of the person to
	 *            be added as an approver.
	 */
	private void addDepartmentApprover(SelectItem item) {
		Integer id = (Integer)item.getValue();
		Contact contact = getContactDAO().findById(id);
		if (contact != null) {
			getContactDAO().attachClean(contact);
			contact.setPermissionMask(contact.getPermissionMask() | Permission.APPROVE_TIMECARD.getMask());

			ApprovalAnchor approvalAnchor = ApprovalAnchorDAO.getInstance().
					findByProductionProjectDept(production, project, department);
			Approver approver = null;
			if (approvalAnchor != null) {
				approver = approvalAnchor.getFirstApprover();
			}

			Approver priorApprover = null;
			while (approver != null) {
				priorApprover = approver;
				approver = approver.getNextApprover();
			}

			// 'priorApprover' is last Approver in chain, or null if no chain exists
			approver = new Approver(contact, null, true); // create new one
			getApproverDAO().save(approver); // ...and add to database
			if (approvalAnchor == null) {
				// no anchor for this department yet -- create new ApprovalAnchor object
				approvalAnchor = new ApprovalAnchor(production, project, department, approver);
				getContactDAO().save(approvalAnchor); // add to database
			}
			else { // have an anchor ... add to end of chain
				if (priorApprover == null) { // empty chain
					approvalAnchor.setFirstApprover(approver);
					getContactDAO().attachDirty(approvalAnchor);
				}
				else {
					priorApprover.setNextApprover(approver);
					getApproverDAO().attachDirty(priorApprover);
				}
				getContactDAO().save(approvalAnchor); // Not necessary?
			}
			addDeptApproverCount(departmentItems);
		}
	}

	/**
	 * Remove the approver (represented by the contact id in the SelectItem's
	 * value) from the list of Production approvers; and change our internal
	 * state to match -- remove the approver from the list and refresh the list
	 * of available (unassigned) approvers.
	 *
	 * @param id The database id of the Approver from the SelectItem chosen.
	 * @param moveTcs True iff the user has already OK'd the prompt to move any
	 *            timecards in the approver's queue to the next approver in the
	 *            chain.
	 * @return True if the approver was successfully found and removed. False
	 *         may be returned in three cases: (1) if two (or more) users are making
	 *         concurrent changes to the approver chains; (2) if 'moveTcs' is
	 *         true, but the approver being removed is the last Production
	 *         approver; or (3) if 'moveTcs' is false and the approver has
	 *         timecards in their queue, in which case a prompt has been issued.
	 */
	private boolean removeProductionApprover(Integer id, boolean moveTcs) {
		Approver approver = removeApproverSub(production, id, moveTcs, this, PROD_APPROVER);
		if (approver != null) {
			extractItem(productionApproverItems, productionApproverId);
			productionApproverId = null;
			refreshMemberItems();
			writeChangeLog();
			return true;
		}
		return false;
	}

	/**
	 * Remove the approver (represented by the contact id in the SelectItem's
	 * value) from the list of Project approvers; and change our internal
	 * state to match -- remove the approver from the list and refresh the list
	 * of available (unassigned) approvers.
	 *
	 * @param id The database id of the Approver from the SelectItem chosen.
	 * @param moveTcs True iff the user has already OK'd the prompt to move any
	 *            timecards in the approver's queue to the next approver in the
	 *            chain.
	 * @return True if the approver was successfully found and removed. False
	 *         may be returned in three cases: (1) if two (or more) users are making
	 *         concurrent changes to the approver chains; (2) if 'moveTcs' is
	 *         true, but the approver being removed is the last Project
	 *         approver; or (3) if 'moveTcs' is false and the approver has
	 *         timecards in their queue, in which case a prompt has been issued.
	 */
	private boolean removeProjectApprover(Integer id, boolean moveTcs) {
		Approver approver = removeApproverSub(project, id, moveTcs, this, PROJECT_APPROVER);
		if (approver != null) {
			extractItem(projectApproverItems, projectApproverId);
			projectApproverId = null;
			refreshMemberItems();
			writeChangeLog();
			return true;
		}
		return false;
	}

	/**
	 * Remove the approver (represented by the Approver.id value) from the given
	 * list of approvers.
	 *
	 * @param root The root of the chain of approvers from which we are removing
	 *            the given item.
	 * @param id The database id of the Approver from the SelectItem chosen.
	 * @param moveTcs True iff the user has already OK'd the prompt to move any
	 *            timecards in the approver's queue to the next approver in the
	 *            chain.
	 * @param caller The calling bean; used for the pop-up prompt issued if
	 *            there are timecards that need to be moved, and 'moveTcs' is
	 *            false.
	 * @return The Approver if it was successfully found and removed. null may
	 *         be returned in three cases: (1) if two (or more) users are making
	 *         concurrent changes to the approver chains; (2) if 'moveTcs' is
	 *         true, but the approver being removed is the last Production
	 *         approver; or (3) if 'moveTcs' is false and the approver has
	 *         timecards in their queue. In case (3), if the 'caller' parameter
	 *         is not null, then a prompt will have been issued.
	 */
	private static Approver removeApproverSub(ApproverChainRoot root, Integer id, boolean moveTcs,
				ApproverHierarchyBean caller, int approverType) {
		ApproverDAO approverDAO = ApproverDAO.getInstance();
		Approver approver = root.getApprover();
		Approver priorApprover = null;
		while (approver != null && ! approver.getId().equals(id)) {
			priorApprover = approver;
			approver = approver.getNextApprover();
		}

		// 'approver' is the one to be deleted (or null if problems)
		// 'priorApprover' is the one pointing to it, or null if it is first in chain
		if (approver != null) { // only null if someone else changed chains concurrently
			if (moveTcs) { // user already OK'd to move timecards to prior approver
				if (! moveTimecards(approver, priorApprover)) {
					return null; // moving timecards failed
				}
				approver = approverDAO.refresh(approver);
			}
			Approver toApprover = approver.getNextApprover();
			if (toApprover == null) {
				toApprover = priorApprover;
			}
			boolean qEmpty;
			if (caller == null) {
				qEmpty = ! WeeklyTimecardDAO.getInstance().existsForApprover(approver);
			}
			else {
				caller.setSelectedApproverId(approver.getId());
				qEmpty = checkApproverQueueEmpty(approver, toApprover, approverType, caller);
			}
			if (qEmpty) {
				removeOutOfLineApprover(approver);
				if (priorApprover == null) { // first in chain - adjust Production reference
					root.setApprover(approver.getNextApprover());
					approverDAO.attachDirty(root);
				}
				else { // not first - adjust prior's reference to point to one after 'approver'
					priorApprover.setNextApprover(approver.getNextApprover());
					approverDAO.attachDirty(priorApprover);
				}
				approver.setNextApprover(null); // ensure Hibernate doesn't try to update
				approverDAO.delete(approver);
				removePermission(approver.getContact());
				return approver;
			}
		}
		return null;
	}

	/**
	 * Remove the approver (represented by the contact id in the SelectItem's
	 * value) from the list of Department approvers for the currently selected
	 * Department.
	 *
	 * @param moveTcs True if the user has already OK'd moving timecards in the
	 *            given approver's queue to the next approver in the chain.
	 * @param id The database id of the Approver from the SelectItem chosen.
	 * @return True if the approver was successfully found and removed. False
	 *         should only be returned if (a) two (or more) users are making
	 *         concurrent changes to the approver chains; or (b) there are
	 *         timecards in the queue for the specified approver.
	 */
	private boolean removeDepartmentApprover(Integer id, boolean moveTcs) {
		ApprovalAnchor approvalAnchor = ApprovalAnchorDAO.getInstance().
				findByProductionProjectDept(production, project, department);

		Approver approver = null;
		if (approvalAnchor != null) {
			approver = approvalAnchor.getFirstApprover();
		}
		Approver priorApprover = null;
		while (approver != null && ! approver.getId().equals(id)) {
			priorApprover = approver;
			approver = approver.getNextApprover();
		}

		// 'approver' is the one to be deleted (or null due to problems)
		// 'priorApprover' is the one pointing to it, or null if it is first in chain
		if (approver != null) { // only null if someone else changed chains concurrently
			if (moveTcs) { // user already OK'd to move timecards to prior approver
				if (! moveTimecards(approver, priorApprover)) {
					return false; // move of timecards failed!
				}
			}
			Approver toApprover = approver.getNextApprover();
			if (toApprover == null) {
				toApprover = priorApprover;
			}
			setSelectedApproverId( approver.getId());
			if (checkApproverQueueEmpty(approver, toApprover, DEPT_APPROVER, this)) {
				removeOutOfLineApprover(approver);
				if (priorApprover == null && approvalAnchor != null) { // first in chain - adjust anchor reference
					approvalAnchor.setFirstApprover(approver.getNextApprover());
					if (approvalAnchor.getFirstApprover() == null) {
						// no approvers for this department, don't need anchor
						getApproverDAO().delete(approvalAnchor);
					}
					else {
						getApproverDAO().attachDirty(approvalAnchor);
					}
				}
				else if (priorApprover != null) { // not first - adjust prior's reference to point to one after 'approver'
					priorApprover.setNextApprover(approver.getNextApprover());
					getApproverDAO().attachDirty(priorApprover);
				}
				approver.setNextApprover(null); // ensure Hibernate doesn't try to update
				getApproverDAO().delete(approver);
				removePermission(approver.getContact());
				extractItem(deptApproverItems, deptApproverId);
				deptApproverId = null;
				refreshMemberItems();
				writeDepartmentChangeLog();
				addDeptApproverCount(departmentItems);
				return true;
			}
		}
		return false;
	}

	/**
	 * Move all the timecards that are queued for a given Approver either to the
	 * following approving in the chain, or, if that is null, to the given
	 * preceding approver in the chain.
	 *
	 * @param approver The Approver whose WeeklyTimecard`s are to be moved.
	 * @param priorApprover The Approver to receive those timecards iff there is
	 *            no approver following the given approver in the approval
	 *            chain.
	 * @return True if the timecards were successful moved, or if no timecards
	 *         existed in the Approver's queue. Only returns false if the
	 *         timecards were not moved because there was neither a following
	 *         nor a prior Approver in the chain. In this case, a FacesMessage
	 *         has been issued.
	 */
	private static boolean moveTimecards(Approver approver, Approver priorApprover) {
		boolean bRet = true;
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		List<WeeklyTimecard> cards = weeklyTimecardDAO.findByApprover(approver);
		if (cards.size() > 0) {
			Approver toApprover = approver.getNextApprover();
			if (toApprover == null) {
				toApprover = priorApprover;
			}
			if (toApprover == null) {
				PopupBean.getInstance().show(null, 0,
						"Approval.CantRemoveLast.Title",
						"Approval.CantRemoveLast.Text",
						"Confirm.OK", null); // no cancel button
				bRet = false;
			}
			else {
				log.debug("to=" + toApprover.getId());
				for (WeeklyTimecard wtc : cards) {
					log.debug(wtc.getId());
					wtc.setApproverId(toApprover.getId());
					wtc.setMarkedForApproval(false);
					weeklyTimecardDAO.attachDirty(wtc);
				}
			}
		}
		if (bRet) { // ok so far; remove approver from any out-of-line links
			removeOutOfLineApprover(approver);
		}
		return bRet;
	}

	/**
	 * If the given approver is referred to by an out-of-line (OOL) approver,
	 * then change the OOL approver to point to the given approver's
	 * NextApprover. We do this when the given approver is about to be removed
	 * from the normal approval chain.
	 *
	 * @param approver The Approver who is about to be removed as either a
	 *            production or department approver, and therefore should be
	 *            removed from any pending out-of-line approval chain.
	 */
	private static void removeOutOfLineApprover(Approver approver) {
		ApproverDAO approverDAO = ApproverDAO.getInstance();
		List<Approver> approvers = approverDAO.findByNextUnshared(approver);
		if (approvers.size() > 0) {
			log.debug("out-of-line count=" + approvers.size());
			// update out-of-line links so they will skip over the one to be removed.
			Approver newNextApprover = approver.getNextApprover();
			for (Approver app : approvers) {
				app.setNextApprover(newNextApprover);
				approverDAO.attachDirty(app);
			}
		}
	}

	/**
	 * See if the given Approver has any timecards waiting on it. If so, put up
	 * a prompt dialog asking the user to OK the removal of this approver.
	 *
	 * @param approver The Approver about to be removed from the approval chain.
	 * @param toApprover The Approver to whom any pending timecards will be
	 *            moved.
	 * @param approverType indicates either project or production approver.
	 * @param caller The bean that invoked this instance.
	 * @return True iff the given Approver has no timecards queued, waiting for
	 *         approval.
	 */
	private static boolean checkApproverQueueEmpty(Approver approver, Approver toApprover, int approverType,
			PopupHolder caller) {
		boolean bRet = true;
		if (WeeklyTimecardDAO.getInstance().existsForApprover(approver)) {
			if (toApprover != null) {
				String name = toApprover.getContact().getUser().getFirstNameLastName();
				PopupBean bean = PopupBean.getInstance();
				if (approverType == PROD_APPROVER) {
					bean.show(caller, ACT_REMOVE_PROD, "Approval.RemoveQueued.");
				}
				else if (approverType == PROJECT_APPROVER) {
					bean.show(caller, ACT_REMOVE_PROJECT, "Approval.RemoveQueued.");
				}
				else {
					bean.show(caller, ACT_REMOVE_DEPT, "Approval.RemoveQueued.");
				}
				bean.setMessage(MsgUtils.formatMessage("Approval.RemoveQueued.Text", name));
				bRet = false;
			}
			else {
				PopupBean.getInstance().show(null, 0,
						"Approval.CantRemoveLast.Title",
						"Approval.CantRemoveLast.Text",
						"Confirm.OK", null); // no cancel button
				bRet = false;
			}
		}
		return bRet;
	}

	/**
	 * Optionally remove an Approver's APPROVE_TIMECARD Permission. If the
	 * Contact referenced by the given Approver object is no longer an
	 * "approver" (for the Production or any Department), then remove the
	 * Contact's APPROVE_TIMECARD Permission.
	 *
	 * @param contact The Contact to be optionally updated.
	 */
	private static void removePermission(Contact contact) {
		ContactDAO contactDAO = ContactDAO.getInstance();
		contact = contactDAO.refresh(contact);
		boolean isApprover = ApproverDAO.getInstance().existsProperty(ApproverDAO.CONTACT, contact);
		if (! isApprover) {
			//log.debug(contact.getPermissionMask());
			contact.setPermissionMask(contact.getPermissionMask() &
					(~ Permission.APPROVE_TIMECARD.getMask()));
			//log.debug(contact.getPermissionMask());
			contactDAO.attachDirty(contact);
		}

	}

	/**
	 * Find the SelectItem entry in the given list whose 'value' field matches
	 * the given integer. Remove it from the List, and return it.
	 *
	 * @param list The List of SelectItem's to be searched and updated.
	 * @param id The integer which should match one of the SelectItem.value
	 *            fields.
	 * @return The matching SelectItem entry, or null if not found.
	 */
	private SelectItem extractItem(List<SelectItem> list, Integer id) {
		for (SelectItem item : list) {
			if (((Integer)item.getValue()).equals(id)) {
				list.remove(item);
				return item;
			}
		}
		return null;
	}

	private SelectItem findItem(List<SelectItem> list, Integer id) {
		for (SelectItem item : list) {
			if (((Integer)item.getValue()).equals(id)) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Find the SelectItem whose value matches the given 'id', and move it one
	 * position earlier (towards the first approver) within the list of
	 * approvers described by 'approverItems'. Note that 'approverItems' itself
	 * is not modified.
	 *
	 * @param approverItems The existing list of SelectItem`s representing the
	 *            approver chain to be updated.
	 * @param id The Approver.id value (which should equal one of the
	 *            SelectItem.value fields) representing the approver to be move
	 *            earlier in the approval process.
	 * @return True iff the chain of approvers was changed.
	 */
	private boolean actionmoveEarlier(List<SelectItem> approverItems, Integer id) {
		if (approverItems != null && approverItems.size() > 1) {
			// more than one item in the list, we may have work to do!
			log.debug("approverItems size = " + approverItems.size());

			int currentNodePosition = 0;
			for (SelectItem approverItem : approverItems) {
				log.debug("currentNodePosition = " + currentNodePosition);
				if (((Integer)approverItem.getValue()).equals(id)) {
					break;
				}
				currentNodePosition++;
			}
			if (currentNodePosition == approverItems.size()-1) {
				// already the first approver, can't move any farther
				return false;
			}

			Approver approver = getApproverDAO().findById(id);
			Approver priorApprover;

			if (currentNodePosition == approverItems.size()-2) {
				// special case for prior reference if this one is the second approver.
				// Change root reference, in prod or anchor, to point to approver being moved to first approver slot.
				if (productionSelected) { // updating production approver chain
					priorApprover = production.getApprover();
					production.setApprover(approver); // make it first in chain
					getApproverDAO().attachDirty(production);
				}
				else if (projectSelected) { // updating project approver chain
					priorApprover = project.getApprover();
					project.setApprover(approver); // make it first in chain
					getApproverDAO().attachDirty(project);
				}
				else {// updating department approver chain
					ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().
							findByProductionProjectDept(production, project, department);
					if (anchor == null) {
						return false; // can only happen if 2 users are updating hierarchy concurrently
					}
					priorApprover = anchor.getFirstApprover();
					anchor.setFirstApprover(approver);
					getApproverDAO().attachDirty(anchor);
				}
				priorApprover.setNextApprover(approver.getNextApprover());
				getApproverDAO().attachDirty(priorApprover);
				// Set forward link from new 1st item to reference old 1st item.
				approver.setNextApprover(priorApprover);
				getApproverDAO().attachDirty(approver);
			}
			else {
				// any other case, get prior approvers from list
				id = (Integer)approverItems.get(currentNodePosition+2).getValue();
				Approver priorPriorApprover = getApproverDAO().findById(id);
				priorApprover = priorPriorApprover.getNextApprover();

				// priorPriorApprover = item "n+2"
				//      priorApprover = item "n+1"
				//           approver = item to move earlier ("n")

				// Current chain  : (n+2) => (n+1) => (n)   => (n-1)
				// change chain to: (n+2) => (n)   => (n+1) => (n-1)

				// Swap the items
				priorApprover.setNextApprover(approver.getNextApprover()); // (n+1) => (n-1)
				priorPriorApprover.setNextApprover(approver);	// (n+2) => (n)
				approver.setNextApprover(priorApprover);		// (n)   => (n+1)

				getApproverDAO().attachDirty(priorPriorApprover);
				getApproverDAO().attachDirty(priorApprover);
				getApproverDAO().attachDirty(approver);
				// Without the following refreshes, the createApproverList() method returned
				// the list as it existed before the above change!
				project = ProjectDAO.getInstance().refresh(project);
				production = ProductionDAO.getInstance().refresh(production);
			}
			return true;
		}
		return false; // empty list or only 1 approver in list - no change.
	}

	/**
	 * Find the SelectItem whose value matches the given 'id', and move it one
	 * position later (towards the final approver) within the list of approvers
	 * described by 'approverItems'. Note that 'approverItems' itself is not
	 * modified.
	 *
	 * @param approverItems The existing list of SelectItem`s representing the
	 *            approver chain to be updated.
	 * @param id The Approver.id value (which should equal one of the
	 *            SelectItem.value fields) representing the approver to be move
	 *            earlier in the approval process.
	 * @return True iff the chain of approvers was changed.
	 */
	private boolean actionmoveLater(List<SelectItem> approverItems, Integer id) {
		//If more than one list item presents
		if (approverItems != null && approverItems.size() > 1) {
			log.debug("approverItems size = " + approverItems.size());

			int currentNodePosition = 0;
			for (SelectItem approverItem : approverItems) {
				log.debug("currentNodePosition = " + currentNodePosition);
				if (((Integer)approverItem.getValue()).equals(id)) {
					break;
				}
				currentNodePosition++;
			}
			if (currentNodePosition == 0) {
				// already final approver position, can't move it any later
				return false;
			}

			Approver approver = getApproverDAO().findById(id);		// item n (currentNodePosition) in list
			Approver nextApprover = approver.getNextApprover(); // item n+1 in list

			if (nextApprover == null) { // can only happen if 2 users are updating hierarchy concurrently
				return false;
			}

			if (currentNodePosition == approverItems.size()-1) { // special case for first approver
				// Change root reference, in prod or anchor, to point to approver following 1st
				if (productionSelected) { // updating production approver chain
					// prod => app(n) => nextApp(n-1) => (n-2) ; change to: prod => nextApp => app => (n-2)
					production.setApprover(nextApprover); 						  // prod => nextApp
					getApproverDAO().attachDirty(production);
				}
				else if (projectSelected) { // updating project approver chain
					// prod => app(n) => nextApp(n-1) => (n-2) ; change to: project => nextApp => app => (n-2)
					project.setApprover(nextApprover); 						  // project => nextApp
					getApproverDAO().attachDirty(project);
				}
				else {// updating department approver chain
					ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().
							findByProductionProjectDept(production, project, department);
					if (anchor == null) {
						return false; // can only happen if 2 users are updating hierarchy concurrently
					}
					// anchor => app(n) => nextApp(n-1) => (n-2) ; change to: anchor => nextApp => app => (n-2)
					anchor.setFirstApprover(nextApprover); 					  // anchor => nextApp
					getApproverDAO().attachDirty(anchor);
				}
				approver.setNextApprover(nextApprover.getNextApprover()); // app => (n-2)
				nextApprover.setNextApprover(approver); 				  // nextApp => app
			}
			else { // any other case, get prior approver from list
				id = (Integer)approverItems.get(currentNodePosition+1).getValue();
				Approver priorApprover = getApproverDAO().findById(id);
				if (priorApprover == null) {
					return false; // can only happen if 2 users are updating hierarchy concurrently
				}
				// prior(n+1) => app(n) => nextApp(n-1) => (n-2) ; change to: prior(n+1) => nextApp => app => (n-2)
				priorApprover.setNextApprover(nextApprover);
				getApproverDAO().attachDirty(priorApprover);
				approver.setNextApprover(nextApprover.getNextApprover()); // app => (n-2)(may be null)
				nextApprover.setNextApprover(approver); 				  // nextApp => app
			}
			getApproverDAO().attachDirty(approver);
			getApproverDAO().attachDirty(nextApprover);
			// Without the following refreshes, the createApproverList() method returned
			// the list as it existed before the above change!
			production = ProductionDAO.getInstance().refresh(production);
			project = ProjectDAO.getInstance().refresh(project);
			return true;
		}
		return false; // empty list or only 1 approver in list - no change.
	}

	/**
	 * Creates entries for the Department selection list.
	 * @param allowProdSelection
	 */
	private void createDepartmentList(boolean allowProdSelection) {
		// Create list of (SelectItems) of departments for selection
		log.debug("");
		if (departmentItems == null) {
			log.debug("");
			departmentItems = new ArrayList<>();
			List<SelectItem> newDeptList = new ArrayList<>(DepartmentUtils.getDepartmentCastCrewDL().size());
			//departmentItems.addAll(DepartmentUtils.getDepartmentCastCrewDL());
			for (SelectItem item : DepartmentUtils.getDepartmentCastCrewDL()) {
				newDeptList.add(new SelectItem(item.getValue(), item.getLabel()));
			}
			log.debug("");
			addDeptApproverCount(newDeptList);
			departmentItems.addAll(newDeptList);
			if (showProjectApprovers) {
				departmentItems.add(0, new SelectItem(0,"* Project Approvers"));
			}
			if (allowProdSelection) {
				if (aicp) {
					departmentItems.add(0, new SelectItem(-1,"* Production Company Approvers"));
				}
				else {
					departmentItems.add(0, new SelectItem(-1,"* Production Approvers"));
				}
			}
		}
		departmentId = (Integer)departmentItems.get(0).getValue();
	}

	/** Method to modify the department label to show number of approvers wth the department name.
	 * @param newDeptList SelectItem list of department.
	 */
	private void addDeptApproverCount(List<SelectItem> newDeptList) {
		log.debug("");
		Iterator<SelectItem> itr = newDeptList.iterator();
		while (itr.hasNext()) {
			SelectItem item = itr.next();
			Department dept = DepartmentDAO.getInstance().findById((Integer)item.getValue());
			if (dept != null) {
				Integer count = calculateDeptApprovers(production, dept, project);
				dept.setApproverCount(count);
				if (count > 0) {
					String deptName = item.getLabel();
					if ((deptName.contains("(") && deptName.contains(")"))) {
						int index = deptName.indexOf("(");
						deptName = deptName.substring(0, index-1);
					}
					deptName = deptName + " (" + count + ")";
					item.setLabel(deptName);
				}

			}
		}
	}

	/**
	 * Create the SelectItem list representing the list of approvers for the
	 * given Department. Optional write a Change log entry with the new list.
	 *
	 * @param department The Department for which the approver list should be
	 *            created.
	 * @param writeLog If true, write a ChangeLog entry showing the current list
	 *            of department approvers.
	 */
	private void createDeptApproverList(Department department, boolean writeLog) {
		deptApproverItems = ApproverUtils.createDeptApproverList(production, project, department);
		if (writeLog) {
			writeDepartmentChangeLog();
		}
	}

	/**
	 * (Re)create the Production approver list and, if applicable, the Project
	 * approver list. Log the current list to the Change log if requested.
	 *
	 * @param writeLog If true, an entry will be written to the Change log
	 *            listing the current chain of approvers.
	 */
	private void createTopApproverLists(boolean writeLog) {
		if (showProductionApprovers) {
			productionApproverItems = createApproverList(production);
		}
		if (showProjectApprovers) {
			projectApproverItems = createApproverList(project);
		}
		if (writeLog) {
			writeChangeLog();
		}
	}

	/**
	 * Create a selection list of approvers for either a Production or a
	 * Project, given the chain origin.
	 *
	 * @param root The object holding the origin of the approval chain.
	 * @return The list of SelectItem`s representing the Approver`s in the
	 *         chain.
	 */
	private List<SelectItem> createApproverList(ApproverChainRoot root) {
		Approver approver = root.getApprover();
		List<SelectItem> listIn = new ArrayList<>();
		ApproverUtils.createApproverList(approver, listIn);
		List<SelectItem> listOut = new ArrayList<>();
		// reverse the order of the generated list:
		for (SelectItem se : listIn) {
			listOut.add(0,se);
		}
		return listOut;
	}

	/**
	 * Write a Change log entry showing the current department's approver list.
	 */
	private void writeDepartmentChangeLog() {
		String msg = "Dept #" + department.getId() + " approver list changed to: " + createListMsg(deptApproverItems);
		ChangeUtils.logChange(ChangeType.PAYROLL, ActionType.UPDATE, msg);
	}

	/**
	 * Write a Change log entry showing the current production's approver list.
	 */
	private void writeChangeLog() {
		String msg = " approver list changed to: ";
		if (productionSelected) {
			msg = "Prod" + msg + createListMsg(productionApproverItems);
		}
		else {
			msg = "Project" + msg + createListMsg(projectApproverItems);
		}
		ChangeUtils.logChange(ChangeType.PAYROLL, ActionType.UPDATE, msg);
	}

	/**
	 * Create a textual message consisting of the names of the approvers taken
	 * from the label fields of the given SelectItem`s.
	 *
	 * @param approverItems A list of SelectItem`s of approvers.
	 * @return A String with the list of approver's names taken from the labels
	 *         of the provided SelectItem`s. The names are separated by
	 *         semicolons.
	 */
	private String createListMsg(List<SelectItem> approverItems) {
		String msg = "";
		int ix;
		for (SelectItem se : approverItems) {
			String item = se.getLabel();
			if ((ix=item.indexOf(" - ")) > 0) {
				item = item.substring(0, ix);
			}
			msg += item + "; ";
		}
		return msg.trim();
	}

	/**
	 * Create the list of drop-down items for the "Dept Time Entry" (Time
	 * keeper) list.
	 *
	 * @param dept The department for which the list is being built. Only
	 *            members of the department are included in the list.
	 */
	private void createDeptTimeEntry(Department dept) {
		//deptTimeEntry = buildMemberItems(dept, false);
		deptTimeEntryId = -1;
		if (dept != null) {
			deptTimeEntry = createDeptTimeEntryItems(dept, false);
			// Is there a customized Department object?
			Department customDept = DepartmentDAO.getInstance()
					.findByProductionProjectDept(production, project, department.getName());
			if (customDept != null && customDept.getTimeKeeper() != null) {
				deptTimeEntryId = customDept.getTimeKeeper().getId();
			}
		}
		else {
			deptTimeEntry = new ArrayList<>();
		}
		deptTimeEntry.add(0, new SelectItem(-1, Constants.SELECT_HEAD_NONE));
	}

	/**
	 * Creates the list of drop-down items for the "Dept Time Entry" (Time
	 * keeper) list. The selected person will be allowed to input hours and
	 * clone timecards but cannot approve a time card.
	 *
	 * @param dept If null, all production members are listed; if not null, then
	 *            only those members whose Role is within the given Department
	 *            are listed.
	 * @param omitInUse If true, the returned list should NOT contain any
	 *            Contacts that are already included in either the Production
	 *            Approver or Department Approver list. NOT CURRENTLY SUPPORTED.
	 */
	private List<SelectItem> createDeptTimeEntryItems(Department dept, boolean omitInUse) {

		// TODO if 'omitInUse' is true, we need to exclude from the list we're building
		// those contacts that are already in the Production approver or Department approver list.
		// Maybe when buildApproverList() is called, it could create a Set of contact.id
		// values that are in those lists, and we could check against that Set in here.

		log.debug("");
		List<SelectItem> items = new ArrayList<>();
		Set<Integer> roleIds = new HashSet<>();
		Collection<Contact> contacts;
		if (showProjectApprovers) { // in Commercial production
			contacts = getContactDAO().findByProjectActive(project);
		}
		else {
			contacts = getContactDAO().findByProductionActive(production);
		}
		for (Contact contact : contacts) {
			User user = contact.getUser();
			roleIds.clear();

			for (Employment emp : contact.getEmployments()) {
				Role role = emp.getRole();
				if (dept == null || role.getDepartment().getId().equals(dept.getId())) {
					// don't list duplicate roles -- check id's versus collection
					if (! roleIds.contains(emp.getRole().getId())) {
						roleIds.add(emp.getRole().getId());
						// Omit Admin roles, unless user has privilege to see them.
						if (! emp.getRole().isAdmin() || showAdmin) {
							String label = user.getLastNameFirstName();
							label += " - " + emp.getRole().getName();
							items.add( new SelectItem(contact.getId(), label) );
						}
					}
				}
			}
			/*for (ProjectMember member : contact.getProjectMembers()) {
				Role role = member.getEmployment().getRole();
				if (dept == null || role.getDepartment().getId().equals(dept.getId())) {
					// don't list duplicate roles -- check id's versus collection
					if (! roleIds.contains(member.getEmployment().getRole().getId())) {
						roleIds.add(member.getEmployment().getRole().getId());
						// Omit Admin roles, unless user has privilege to see them.
						if (! member.getEmployment().getRole().isAdmin() || showAdmin) {
							String label = user.getLastNameFirstName();
							label += " - " + member.getEmployment().getRole().getName();
							items.add( new SelectItem(contact.getId(), label) );
						}
					}
				}
			}*/
		}
		Collections.sort(items, getSelectItemComparator());
		return items;
	}

	/**
	 * Refresh the Members list with either all Production members, or
	 * current department members, depending on the user's Display selection.
	 */
	private void refreshMemberItems() {
		// Populate the approver contact list to filter in member list box
		createApproverContactIds();
		if (productionSelected) {
			createMemberItems(null, aicp);
		}
		else if (projectSelected) {
			createMemberItems(null, false);
		}
		else {
			if (displayItemId == DISPLAY_ALL_MEMBERS) {
				createMemberItems(null, false);
			}
			else {
				createMemberItems(department, false);
			}
		}
		refreshTime = new Date();
	}

	/**
	 * Creates entries for the Production Members display/selection list. The
	 * display includes the member's name and role in this project. The list is
	 * filtered based on the user's viewing permissions for cast, crew, and
	 * admin roles.
	 *
	 * @param dept If null, all qualified production members are listed; if not
	 *            null, then only those members whose Role is within the given
	 *            Department are listed.
	 * @param aggregateOnly If true and 'dept' is null, only Contacts with
	 *            View__All_Projects permission are included. (If 'dept' is not
	 *            null, this parameter is ignored.)
	 */
	private void createMemberItems(Department dept, boolean aggregateOnly) {

		log.debug("");
		List<SelectItem> items = new ArrayList<>();
		Set<Integer> roleIds = new HashSet<>();
		Collection<Contact> contacts;

		// Get the list of eligible Contacts
		if (dept == null && aggregateOnly) {
			contacts = ProjectMemberDAO.getInstance()
					.findByProductionPermissionDistinctContact(production, Permission.VIEW_ALL_PROJECTS);
		}
		else if (showProjectApprovers) {
			contacts = getContactDAO().findByProjectActive(project);
		}
		else {
			contacts = getContactDAO().findByProductionActive(production);
		}

		// We need to exclude from the list we're building the contacts
		// that are already in the selected Production/Project/Department approver list.
		for (Contact contact : contacts) {
			if (! approverContactIds.contains(contact.getId())) {
				User user = contact.getUser();

				for (Employment emp : contact.getEmployments()) {
					Role role = emp.getRole();
					// Only allow if no department screening, or matches the department requested.
					if (dept == null || role.getDepartment().getId().equals(dept.getId())) {
						// don't list duplicate people -- check id's versus collection
						if (! roleIds.contains(contact.getId())) {
							// Omit Admin roles, unless user has privilege to see them.
							if (! emp.getRole().isAdmin() || showAdmin) {
								roleIds.add(contact.getId());
								String label = user.getLastNameFirstName();
								label += " - " + emp.getRole().getName();
								items.add( new SelectItem(contact.getId(), label) );
							}
						}
					}
				}

				/*for (ProjectMember member : contact.getProjectMembers()) {
					Role role = member.getEmployment().getRole();
					// Only allow if no department screening, or matches the department requested.
					if (dept == null || role.getDepartment().getId().equals(dept.getId())) {
						// don't list duplicate people -- check id's versus collection
						if (! roleIds.contains(contact.getId())) {
							// Omit Admin roles, unless user has privilege to see them.
							if (! member.getEmployment().getRole().isAdmin() || showAdmin) {
								roleIds.add(contact.getId());
								String label = user.getLastNameFirstName();
								label += " - " + member.getEmployment().getRole().getName();
								items.add( new SelectItem(contact.getId(), label) );
							}
						}
					}
				}*/
			}
		}
		Collections.sort(items, getSelectItemComparator());
		memberItems = items;
		if (memberItems.size() == 1) {
			// selectOneListBox doesn't work right with 1 entry; "pre-select" it.
			memberId = (Integer)memberItems.get(0).getValue();
		}
		else {
			memberId = null; // clear selection
		}
	}

	/**
	 * Populate {@link #approverContactIds} with the Contact ids of the approvers
	 * already present in either the Production, Project, or Department approver list.
	 */
	private void createApproverContactIds() {
		approverContactIds.clear();
		if (productionSelected) {
			fillApproverContactIds(productionApproverItems);
		}
		else if (projectSelected) {
			fillApproverContactIds(projectApproverItems);
		}
		else {
			fillApproverContactIds(deptApproverItems);
		}
	}

	/**
	 * Populate {@link #approverContactIds} with the Contact ids derived from the
	 * given list of SelectItem`s. The value field of each SelectItem should be
	 * an Approver.id value.
	 */
	private void fillApproverContactIds(List<SelectItem> items) {
		for (SelectItem filterItem : items) {
			Approver approver = getApproverDAO().findById((Integer)filterItem.getValue());
			if (approver != null) {
				approverContactIds.add(approver.getContact().getId());
			}
		}
	}

	/**
	 * Create drop-down list for selection of member list contents:
	 * 'All Production Members' or 'Department Members'
	 */
	private void createDisplayItems() {
		displayItems.add( new SelectItem(0, "All Production Members"));
		displayItems.add( new SelectItem(1, "Department Members"));
	}

	/**
	 * Called when we get an unexpected exception. Clear our lists in case
	 * there's a pending "click" message that could assume our lists are
	 * correct.
	 */
	private void errorReset() {
		try {
			production = SessionUtils.getNonSystemProduction();
			if (production.getType().hasPayrollByProject()) {
				project = SessionUtils.getCurrentProject();
			}
			departmentId = -1;
			if (showProductionApprovers) {
				productionSelected = true;
				projectSelected = false;
			}
			else {
				productionSelected = false;
				projectSelected = true; // Project Approvers list is selected
			}
			if (productionApproverItems != null) {
				productionApproverItems.clear();
			}
			if (projectApproverItems != null) {
				projectApproverItems.clear();
			}
			if (deptApproverItems != null) {
				deptApproverItems.clear();
			}
			createDeptTimeEntry(null);
		}
		catch (Exception e) {
			log.error("exception during error cleanup: ", e);
		}
	}

	/**
	 * Calculates number of approvers in a given department.
	 * 'All Production Members' or 'Department Members'
	 * @param prod Production
	 * @param dept Department whose approvers will be calculated.
	 * @param proj Project
	 *
	 * @return Integer number of Approvers in a given department.
	 */
	private Integer calculateDeptApprovers(Production prod, Department dept, Project proj) {
		Integer count = 0;
		if (dept != null) {
			ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, proj, dept);
			if (anchor != null) {
				Approver app = anchor.getFirstApprover();
				while (app != null) {
					count = count + 1;
					app = app.getNextApprover();
				}
				//dept.setApproverCount(count);
			}
		}
		return count;
	}


	// * * * Accessors and Mutators -- get/set methods

	/** See {@link #showProjectApprovers}. */
	public boolean getShowProjectApprovers() {
		return showProjectApprovers;
	}

	/** See {@link #showProjectApprovers}. */
	public void setShowProjectApprovers(boolean showProjectApprovers) {
		this.showProjectApprovers = showProjectApprovers;
	}

	/** See {@link #showProductionApprovers}. */
	public boolean getShowProductionApprovers() {
		return showProductionApprovers;
	}

	/** See {@link #showProductionApprovers}. */
	public void setShowProductionApprovers(boolean showProductionApprovers) {
		this.showProductionApprovers = showProductionApprovers;
	}

	/** See {@link #department}. */
	public Department getDepartment() {
		return department;
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/** See {@link #departmentItems}. */
	public List<SelectItem> getDepartmentItems() {
		return departmentItems;
	}
	/** See {@link #departmentItems}. */
	public void setDepartmentItems(List<SelectItem> departmentItems) {
		this.departmentItems = departmentItems;
	}

	/** See {@link #memberId}. */
	public Integer getMemberId() {
		return memberId;
	}
	/** See {@link #memberId}. */
	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	/** See {@link #memberItems}. */
	public List<SelectItem> getMemberItems() {
		return memberItems;
	}
	/** See {@link #memberItems}. */
	public void setMemberItems(List<SelectItem> memberItems) {
		this.memberItems = memberItems;
	}

	/** See {@link #deptApproverId}. */
	public Integer getDeptApproverId() {
		return deptApproverId;
	}
	/** See {@link #deptApproverId}. */
	public void setDeptApproverId(Integer deptApproverId) {
		this.deptApproverId = deptApproverId;
	}

	/** See {@link #deptApproverItems}. */
	public List<SelectItem> getDeptApproverItems() {
		return deptApproverItems;
	}
	/** See {@link #deptApproverItems}. */
	public void setDeptApproverItems(List<SelectItem> deptApproverItems) {
		this.deptApproverItems = deptApproverItems;
	}

	/** See {@link #productionApproverId}. */
	public Integer getProductionApproverId() {
		return productionApproverId;
	}
	/** See {@link #productionApproverId}. */
	public void setProductionApproverId(Integer productionApproverId) {
		this.productionApproverId = productionApproverId;
	}

	/** See {@link #productionApproverItems}. */
	public List<SelectItem> getProductionApproverItems() {
		return productionApproverItems;
	}
	/** See {@link #productionApproverItems}. */
	public void setProductionApproverItems(List<SelectItem> productionApproverItems) {
		this.productionApproverItems = productionApproverItems;
	}

	/** See {@link #projectApproverId}. */
	public Integer getProjectApproverId() {
		return projectApproverId;
	}
	/** See {@link #projectApproverId}. */
	public void setProjectApproverId(Integer projectApproverId) {
		this.projectApproverId = projectApproverId;
	}

	/** See {@link #projectApproverItems}. */
	public List<SelectItem> getProjectApproverItems() {
		return projectApproverItems;
	}
	/** See {@link #projectApproverItems}. */
	public void setProjectApproverItems(List<SelectItem> projectApproverItems) {
		this.projectApproverItems = projectApproverItems;
	}

	/**See {@link #selectedApproverId}. */
	public Integer getSelectedApproverId() {
		return selectedApproverId;
	}
	/**See {@link #selectedApproverId}. */
	public void setSelectedApproverId(Integer selectedApproverId) {
		this.selectedApproverId = selectedApproverId;
	}

	public List<SelectItem> getDisplayItems() {
		return displayItems;
	}
	public void setDisplayItems(List<SelectItem> displayItems) {
		this.displayItems = displayItems;
	}

	public Integer getDisplayItemId() {
		return displayItemId;
	}
	public void setDisplayItemId(Integer displayItemId) {
		this.displayItemId = displayItemId;
	}

	public boolean getProductionSelected() {
		return productionSelected;
	}
	public void setProductionSelected(boolean productionSelected) {
		this.productionSelected = productionSelected;
	}

	/** See {@link #projectSelected}. */
	public boolean getProjectSelected() {
		return projectSelected;
	}
	/** See {@link #projectSelected}. */
	public void setProjectSelected(boolean projectSelected) {
		this.projectSelected = projectSelected;
	}

	public List<SelectItem> getDeptTimeEntry() {
		return deptTimeEntry;
	}
	public void setDeptTimeEntry(List<SelectItem> deptTimeEntry) {
		this.deptTimeEntry = deptTimeEntry;
	}

	public Integer getDeptTimeEntryId() {
		return deptTimeEntryId;
	}
	public void setDeptTimeEntryId(Integer deptTimeEntryId) {
		//log.debug("............. set called, value=" + deptTimeEntryId);
		// value is set via valueChangeListener method. If we let it get
		// set here, it breaks the process.
//		this.deptTimeEntryId = deptTimeEntryId;
	}

	private ApproverDAO getApproverDAO() {
		if (approverDAO == null) {
			approverDAO = ApproverDAO.getInstance();
		}
		return approverDAO;
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

}
