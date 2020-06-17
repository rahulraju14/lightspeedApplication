package com.lightspeedeps.web.approver;

import java.util.*;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ApproverChainRoot;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.onboard.ApprovalPathsBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.ListView;
import com.lightspeedeps.web.view.View;

/**
 * Contains static utility methods that are exclusively used by classes in
 * the web.approver package.
 */
public class ApproverUtils {
	private static final Log log = LogFactory.getLog(ApproverUtils.class);

	/** Text to display for an Approved timecard/document in "waiting for" column on dashboard pages */
	private static final String WAITING_FOR_TEXT_APPROVED = MsgUtils.formatMessage("Timecard.WaitingFor.Approved");

	/** Text to display for a document in PENDING status, in "waiting for" column on dashboard pages */
	private static final String WAITING_FOR_TEXT_NO_PATH = MsgUtils.formatMessage("Document.WaitingFor.NoPath");

	/** Text to display for a document in PENDING status, in "waiting for" column on dashboard pages */
	private static final String WAITING_FOR_TEXT_PENDING = MsgUtils.formatMessage("Timecard.WaitingFor.Pending");

	/** Text to display for an un-submitted timecard/document in "waiting for" column on dashboard pages */
	private static final String WAITING_FOR_TEXT_SUBMITTAL = MsgUtils.formatMessage("Timecard.WaitingFor.Submittal");

	/** Text to display for a VOID timecard in "waiting for" column on dashboard pages. */
	private static final String WAITING_FOR_TEXT_TC_VOID = MsgUtils.formatMessage("Timecard.WaitingFor.Void");

	/** Text to display for a VOID document in "waiting for" column on onboard pages. */
	private static final String WAITING_FOR_TEXT_VOID = MsgUtils.formatMessage("Document.WaitingFor.Void");

	/** Text to display for documents in pool in "waiting for" column on onboard pages */
	//private static final String WAITING_FOR_TEXT_POOL = MsgUtils.formatMessage("Document.WaitingFor.Pool");

	/** Text to display for documents in Production pool in "waiting for" column on onboard pages */
	private static final String WAITING_FOR_TEXT_PROD_POOL = MsgUtils.formatMessage("Document.WaitingFor.ProdPool");

	/** Text to display for documents in Department pool in "waiting for" column on onboard pages */
	private static final String WAITING_FOR_TEXT_DEPT_POOL = MsgUtils.formatMessage("Document.WaitingFor.DeptPool");

	/** Text to display for documents in pool in "waiting for" column on onboard pages */
	private static final String WAITING_FOR_TEXT_UNKNOWN = MsgUtils.formatMessage("Document.WaitingFor.Unknown");

	private ApproverUtils() {
	}

	/**
	 * Find out if any Approver exists, either for the specified Department,
	 * Project, or for the Production.
	 *
	 * @param dept The Department whose approval chain should be examined.
	 * @param prodId The Production.prodId value of the Production to be
	 *            checked.
	 * @param project The Project to be checked for the existence of approvers;
	 *            this should be null for TV/Feature productions and non-null
	 *            for Commercial productions. For Commercial productions, this
	 *            also determines which department approval chain is checked,
	 *            since the department approvers are specific to an individual
	 *            project in this case.
	 * @return True iff at least one approver exists either as a Production
	 *         Approver, a project approver, or as an approver for the given
	 *         Department.
	 */
	public static boolean existsApprover(Department dept, String prodId, Project project) {
		try {
			Production prod = TimecardUtils.findProduction(prodId);
			if (prod.getApprover() != null) {
				return true;
			}
			if (project != null && project.getApprover() != null) {
				return true;
			}
			if (ApprovalAnchorDAO.getInstance().existsApproverForProductionProjectDept(prod, project, dept)) {
				return true;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return false;
	}

	/**
	 * Find the Approver.id value of the first approver for the specified
	 * Department. If there are no departmental approvers, it will be the first
	 * Production approver. If there are no approvers at all, null is returned.
	 *
	 * @param dept The Department whose approval chain should be examined.
	 * @return The Approver.id value for the first approver for the given
	 *         department.
	 */
	public static Integer findFirstApproverId(Production prod, Project project, Department dept) {
		Integer id = null;
		Approver app = findFirstApprover(prod, project, dept, null);
		if (app != null) {
			id = app.getId();
		}
		return id;
	}

	/**
	 * Find the first Approver instance for the specified Department. If there
	 * are no departmental approvers, it will be the first Project approver (for
	 * commercial production). If there are no Project approvers, it will be the
	 * first Production approver. If there are no approvers at all, null is
	 * returned.
	 *
	 * @param dept The Department whose approval chain should be examined.
	 * @param project The Project whose approval chain should be examined.
	 * @param approvalPathId
	 * @return The Approver for the first approver for the given department, if
	 *         there is one, or the first Production approver if not. If neither
	 *         one exists, null is returned.
	 */
	public static Approver findFirstApprover(Production prod, Project project, Department dept, Integer approvalPathId) {
		Approver app = null;
		try {
			if (approvalPathId == null) {
				ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance()
						.findByProductionProjectDept(prod, project, dept);
				if (anchor != null) {
					app = anchor.getFirstApprover();
				}
				if (app == null) { // no departmental approvers
					if (project != null) {
						app = project.getApprover();
					}
					if (app == null) { // no project approver or not commercial production
						app = prod.getApprover(); // return 1st production approver, if any
					}
				}
			}
			else {
				ApprovalPathAnchor pathAnchor = getApprovalPathAnchor(project, dept, approvalPathId);
				if (pathAnchor != null) {
					//ApproverGroup Changes
					ApproverGroup approverGroup = pathAnchor.getApproverGroup();
					if (approverGroup != null) {
						approverGroup = ApproverGroupDAO.getInstance().refresh(approverGroup);
						if (! approverGroup.getUsePool()) { // Linear hierarchy
							app = approverGroup.getApprover();
						}
						else { // Department Pool
							//TODO
							// Pool case
							Approver approver = new Approver();
							Integer id = (-1)*(approverGroup.getId());
							approver.setId(id);
							app = approver;
						}
					}
				}
				if (app == null) {
					log.debug(" ");
					ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
					if(path != null) {
						if(! path.getUsePool()) {
							app = path.getApprover();
						}
						/*else {
							// Pool case, Handled by calling methods.
							Approver approver = new Approver();
							Integer id = (-1)*(path.getId());
							approver.setId(id);
							app = approver;
						}*/
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return app;
	}

	/**
	 * Find the Approver who is next in the approval chain after a specific
	 * Approver. This will follow dept, project and production approval chains
	 * (if necessary) to find the specified entry.
	 *
	 * @param prod The relevant Production
	 * @param project The relevant Project; this should be null except in
	 *            Commercial productions.
	 * @param currentAppId The Approver.id value, identifying the next person to
	 *            approve this timecard. This will be null if (a) the timecard
	 *            has not been submitted, (b) was rejected back to the employee,
	 *            or (c) has gotten final approval.
	 * @param dept The employee's department, taken from the StartForm that was
	 *            used when creating this timecard.
	 * @param approvalPathId The id of the ApprovalPath being followed. Will be
	 *            null for timecards, and not null for other documents.
	 * @return the Approver who is next in the approval chain after the Approver
	 *         identified by the database id 'currentAppId'.
	 */
	public static Approver findApproverAfter(Production prod, Project project, Integer currentAppId, Department dept, Integer approvalPathId) {
		log.debug("curr app id=" + currentAppId);
		if (currentAppId == null) { // currently in employee's queue
			log.debug("");
			return findFirstApprover(prod, project, dept, approvalPathId);
		}
		if (currentAppId < 0 && approvalPathId != null) {
			ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
			if (path != null) {
				//ApproverGroup Changes
				Integer prodPoolAppId = -(approvalPathId);
				if (! currentAppId.equals(prodPoolAppId)) {
					// Department Pool, currentAppId is negation of Approver Group Id.
					Integer groupId = -(currentAppId);
					ApproverGroup appGroup = ApproverGroupDAO.getInstance().findById(groupId);
					if (appGroup != null) {
						if (! path.getUsePool()) { // Linear production-approver path
							Approver app = path.getApprover();
							log.debug("next linear approver id: " + (app != null ? app.getId() : "NULL"));
							return app;
						}
						else { // production-approver uses Pool
							if ((! path.getApproverPool().isEmpty())) {
								log.debug("production pool path NOT empty");
								Approver approver = new Approver();
								approver.setId((-1)*path.getId());
								log.debug("next approver id: " + approver.getId());
								return approver;
							}
							else if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
								log.debug("production pool path is empty; has 'final approver'");
								Approver app = path.getFinalApprover();
								log.debug("next approver id: " + (app != null ? app.getId() : "NULL"));
								return app;
							}
							log.debug("production pool path is empty, and NO 'final approver'");
							return null;
						}
					}
				}
				else {
					if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
						Approver app = path.getFinalApprover();
						log.debug("next approver id: " + app.getId());
						return app;
					}
					else { // If first approver finalizes
						/*Approver approver = new Approver();
						approver.setId(currentAppId);*/
						return null;
					}
				}

			}
		}
		Approver app = ApproverDAO.getInstance().findById(currentAppId);
		if (app == null) { // shouldn't happen
			return null;	// but treat as end of chain
		}
		if (app.getNextApprover() != null) {
			// typical case - just follow the current chain
			return app.getNextApprover();
		}
		/*
		 * At this point, since the 'next approver' was null, the currentAppId
		 * must refer to either the end of the departmental chain, or the end of
		 * the project chain, or the end of the production chain. Check the
		 * department chain...
		 */
		//log.debug("check dept chain");
		boolean found = false;
		if (approvalPathId != null) {
			ApprovalPathAnchor pathAnchor = getApprovalPathAnchor(project, dept, approvalPathId);
			if (pathAnchor != null) {
				//ApproverGroup Changes
				ApproverGroup approverGroup = pathAnchor.getApproverGroup();
				if (approverGroup != null) {
					approverGroup = ApproverGroupDAO.getInstance().refresh(approverGroup);
					if (! approverGroup.getUsePool()) { // Linear hierarchy
						app = approverGroup.getApprover();
						while(app != null && ! found) {
							found = app.getId().equals(currentAppId);
							app = app.getNextApprover();
						}
					}
					else { // Department Pool
						//TODO AM Unnecessary code, will never be executed. Need to test. BUG WITH THIS CODE in case of Final approver
						/*log.debug("><><><><><");
						Approver approver = new Approver();
						approver.setId((-1)*approverGroup.getId());
						return approver;*/
					}
				}
			}
		}
		else {
			ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, dept);
			if (anchor != null) {
				app = anchor.getFirstApprover();
				while(app != null && ! found) {
					found = app.getId().equals(currentAppId);
					app = app.getNextApprover();
				}
			}
		}
		if (found) { // found, must be last one in dept chain...
			if (project != null && approvalPathId == null) {
				app = project.getApprover(); // so get first Project approver, if any for timecard
			}
			if (app == null) { // no project approver or not commercial production
				if(approvalPathId != null) {
					ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
					if (path != null) {
						if (path.getUsePool()) {
							Approver approver = new Approver();
							approver.setId((-1)*path.getId());
							return approver;
						}
						else {
							app = path.getApprover();
						}
					}
				}
				else {
					app = prod.getApprover();  // so next is first production approver(For Timecard).
				}
			}
		}
		else { // not found in dept list, so last of either project approvers or production approvers
			if (project == null) { // not commercial production
				app = null; // must be at end of production approver list, so 'next approver' is null.
			}
			else {
				if (approvalPathId != null) {
					app = null; // must be at end of production approver list, so 'next approver' is null.
				}
				else {
					// TODO approve case(from StartForms tab directly) for document is giving LIE after this.
					app = project.getApprover();
					while(app != null && ! found) {
						found = app.getId().equals(currentAppId);
						app = app.getNextApprover();
					}
					if (found) { // matched, must be at end of project chain
						app = prod.getApprover();  // so next is first production approver.
					}
				}
			}
		}
		return app;
	}

	/** Utility method finds the ApprovalPathAnchor for the given Department, ApprovalPath, Project and Production.
	 * @param project
	 * @param dept
	 * @param approvalPathId
	 * @return
	 */
	public static ApprovalPathAnchor getApprovalPathAnchor(Project project, Department dept, Integer approvalPathId) {
		ApprovalPathAnchor pathAnchor = null;
		Map<String, Object> values = new HashMap<>();
		values.put("production", SessionUtils.getProduction());
		values.put("department", dept);
		values.put("approvalPathId", approvalPathId);
		if (project != null) {
			values.put("project", project);
			pathAnchor = ApprovalPathAnchorDAO.getInstance().findOneByNamedQuery(ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_PROJECT_APPROVAL_PATH, values);
		}
		else {
			pathAnchor = ApprovalPathAnchorDAO.getInstance().findOneByNamedQuery(ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_APPROVAL_PATH, values);
		}
		return pathAnchor;
	}

	/**
	 * Determine the current Approval level of the ContactDocument
	 * whether the ContactDocument is at Production level or at Department level.
	 *
	 * @param approverId The current or new Approver.id value of the ContactDocument.
	 * @param approvalPath The ApprovalPath followed by the ContactDocument.
	 * @return true if ContactDocument is at Production level and false if it is at Department level.
	 */
	public static ApprovalLevel isProductionOrDeptApprover(Integer approverId, Integer approvalPathId) {
		log.debug("");
		if (approverId == null) {
			log.debug("");
			return null;
		}
		ApprovalPath approvalPath = null;
		if (approvalPathId != null) {
			approvalPath = ApprovalPathDAO.getInstance().findById(approvalPathId);
		}
		if (approvalPath != null) {
			if (approverId < 0) {
				if (approverId.equals((-1)*approvalPath.getId())) { //Production Pool
					log.debug("");
					return ApprovalLevel.PROD;
				}
				else { //Department Pool
					log.debug("");
					return ApprovalLevel.DEPT;
				}
			}
			else {
				Approver app = approvalPath.getApprover();
				boolean found = false;
				while(app != null && ! found) {
					if (app.getId().equals(approverId)) {
						found = true;
						break;
					}
					app = app.getNextApprover();
				}
				if (found) { // Linear case for Production
					log.debug("");
					return ApprovalLevel.PROD;
				}
				// Pool's final approver
				if ((! found) && approvalPath.getUsePool() && approvalPath.getUseFinalApprover()) {
					if (approvalPath.getFinalApprover() != null &&
							(approvalPath.getFinalApprover().getId().equals(approverId))) {
						found = true;
						log.debug("");
						return ApprovalLevel.FINAL;
					}
				}
				else if (! found) { // Linear case for Department
					log.debug("");
					return ApprovalLevel.DEPT;
				}
			}
		}
		return null;
	}

	/**
	 * Determine the User who is next in line to approve the given Approvable Object.
	 *
	 * @param appr The Approvable object to be examined.
	 *
	 * @return The User whose is supposed to approve the given Approvable object. Returns
	 *         null if no approver exists.
	 */
	public static User findApproverUser(Approvable appr) {
		User user = null;
		if (appr.getApproverId() != null) {
			if (appr.getApproverContactId() == null) {
				if (appr.getApproverId() > 0) {
					Approver app = ApproverDAO.getInstance().findById(appr.getApproverId());
					if (app != null && app.getContact() != null) {
						appr.setApproverContactId(app.getContact().getId());
						user = app.getContact().getUser();
					}
				}
				// code for pool case.
				else {
					//ApproverGroup Changes
					Contact currContact = SessionUtils.getCurrentContact();
					Integer pathOrGroupId = -(appr.getApproverId());
					Set<Contact> contactPool = new HashSet<>();
					if (appr.getIsDeptPool()) { // Department pool, Group Id
						ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(pathOrGroupId);
						if (approverGroup != null) {
							contactPool = approverGroup.getGroupApproverPool();
						}
					}
					else { // Production pool, Path Id
						ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathOrGroupId);
						if (path != null) {
							contactPool = path.getApproverPool();
						}
					}
					if ((! contactPool.isEmpty()) && contactPool.contains(currContact)) {
						appr.setApproverContactId(currContact.getId());
						user = currContact.getUser();
					}
				}
			}
			else {
				Contact contact = ContactDAO.getInstance().findById(appr.getApproverContactId());
				if (contact != null) {
					user = contact.getUser();
				}
			}
		}
		return user;
	}

	/**
	 * Determine the name of the user who is next in line to approve the given
	 * Approvable object.
	 *
	 * @param appr The Approvable object to be examined.
	 *
	 * @return The "first-name last-name" format of the user whose is supposed
	 *         to approve the Approvable object. Returns an empty String ("") if no
	 *         approver exists.
	 */
	public static String findApproverName(Approvable appr) {
		String name = "";
		User user = findApproverUser(appr);
		if (user != null) {
			name = user.getLastNameFirstName();
		}
		return name;
	}

	/**
	 * Method used to get a list of Contacts of all the types of approvers
	 * (dept, project, and production) for an Approval path.
	 *
	 * @param path the ApprovalPath of interest.
	 * @return List of unique contacts of all approvers for the given Approval Path
	 */
	public static List<Contact> findApproverContacts(ApprovalPath path) {
		List<Contact> allApproverContactList = new ArrayList<>();
		try {
			if (path.getUsePool()) { // take out all the contacts from the pool
				if (path.getApproverPool() != null && (! path.getApproverPool().isEmpty())) {
					allApproverContactList.addAll(path.getApproverPool());
				}
				Approver finalApp = path.getFinalApprover();
				if(finalApp != null) { // add the only final approver contact
					if (! allApproverContactList.contains(finalApp.getContact())) {
						allApproverContactList.add(finalApp.getContact());
					}
				}
			}
			else { // linear case
				if (path.getApprover() != null) { // production's first approver
					if (! allApproverContactList.contains(path.getApprover().getContact())) {
						allApproverContactList.add(path.getApprover().getContact());
					}
					Approver app = path.getApprover().getNextApprover();
					while (app != null) { // follow the chain and add all the production approver contacts
						if (! allApproverContactList.contains(app.getContact())) {
							allApproverContactList.add(app.getContact());
						}
						app = app.getNextApprover();
					}
				}
			}
			List<ApprovalPathAnchor> pathAnchorList = ApprovalPathAnchorDAO.getInstance().findByProperty("approvalPath", path);
			if (pathAnchorList != null) {
				//ApproverGroup Changes
				List<ApproverGroup> distinctAppGroupList = new ArrayList<>();
				for (ApprovalPathAnchor anchor : pathAnchorList) {
					ApproverGroup appGroup = anchor.getApproverGroup();
					if (appGroup != null && (! distinctAppGroupList.contains(appGroup))) {
						distinctAppGroupList.add(appGroup);
					}
				}
				for (ApproverGroup group : distinctAppGroupList) { // loop through all the  Approver Groups
					if (group.getUsePool()) {
						if (group.getGroupApproverPool() != null && (! group.getGroupApproverPool().isEmpty())) {
							allApproverContactList.addAll(group.getGroupApproverPool());
						}
					}
					else {
						if (group.getApprover() != null) { // Group's first approver
							if (! allApproverContactList.contains(group.getApprover().getContact())) {
								allApproverContactList.add(group.getApprover().getContact());
							}
							Approver app = group.getApprover().getNextApprover();
							while (app != null) { // follow the chain and add all the group approver contacts
								if (! allApproverContactList.contains(app.getContact())) {
									allApproverContactList.add(app.getContact());
								}
								app = app.getNextApprover();
							}
						}
					}
				}
				/* Old Code
				for (ApprovalPathAnchor anchor : pathAnchorList) { // loop through all the department's anchor
					if (anchor.getApprover() != null) {
						if (! allApproverContactList.contains(anchor.getApprover().getContact())) {
							allApproverContactList.add(anchor.getApprover().getContact());
						}
						Approver anchorApprover = anchor.getApprover().getNextApprover();
						while (anchorApprover != null) { // follow each anchor's chain and add all the approver contacts
							if (! allApproverContactList.contains(anchorApprover.getContact())) {
								allApproverContactList.add(anchorApprover.getContact());
							}
							anchorApprover = anchorApprover.getNextApprover();
						}
					}
				}*/
			}
			if (log.isDebugEnabled()) {
				log.debug("Total approvers : " + allApproverContactList.size());
				for (Contact ct : allApproverContactList) {
					log.debug("approver: " + ct.getDisplayName());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return allApproverContactList;
	}

	/**
	 * Determine if a given Contact is one of the approvers in a given
	 * ApprovalPath.
	 *
	 * @param path The ApprovalPath to search, or null.
	 * @param contact The Contact to look for in the path.
	 * @return True iff the given Contact is found in the specified path;
	 *         returns false if 'path' is null.
	 */
	public static boolean isContactInPath(ApprovalPath path, Contact contact, Department dept) {
		try {
			if (path == null) {
				return false;
			}
			if (path.getUsePool()) { // take out all the contacts from the pool
				if (path.getApproverPool() != null) {
					if (path.getApproverPool().contains(contact)) {
						return true;
					}
				}
				Approver finalApp = path.getFinalApprover();
				if (finalApp != null && finalApp.getContact().equals(contact)) {
					return true;
				}
			}
			else { // linear case
				if (path.getApprover() != null) { // production's first approver
					Approver app = path.getApprover();
					while (app != null) { // follow the chain and add all the production approver contacts
						if (app.getContact().equals(contact)) {
							return true;
						}
						app = app.getNextApprover();
					}
				}
			}
			List<ApprovalPathAnchor> pathAnchorList = ApprovalPathAnchorDAO.getInstance().findByProperty("approvalPath", path);
			if (pathAnchorList != null) {
				for (ApprovalPathAnchor anchor : pathAnchorList) { // loop through all the department's anchor
					// TODO Attempted fix for LIE 2/6/17 #482897 (error was not recreatable)
					anchor = ApprovalPathAnchorDAO.getInstance().refresh(anchor);
					// TODO Attempted fix for LIE 3/17/17 #55758 (error was not recreatable)
					if (dept != null) {
						dept = DepartmentDAO.getInstance().refresh(dept);
					}
					//ApproverGroup Changes
					if (anchor.getApproverGroup() != null && (dept == null || anchor.getDepartment().equals(dept))) {
						ApproverGroup group = anchor.getApproverGroup();
						//group = ApproverGroupDAO.getInstance().refresh(group);
						if (group.getUsePool()) {
							if (group.getGroupApproverPool() != null) {
								if (group.getGroupApproverPool().contains(contact)) {
									return true;
								}
							}
						}
						else {
							Approver app = group.getApprover();
							while (app != null) { // follow each anchor's chain and add all the approver contacts
								if (app.getContact().equals(contact)) {
									return true;
								}
								app = app.getNextApprover();
							}
						}
						/*Approver app = anchor.getApprover();
						while (app != null) { // follow each anchor's chain and add all the approver contacts
							if (app.getContact().equals(contact)) {
								return true;
							}
							app = app.getNextApprover();
						}
*/					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Determine if a given Contact is one of the approvers in a given ApprovalPath.
	 *
	 * @param path The ApprovalPath to search, or null.
	 * @param contact The Contact to look for in the path.
	 * @return Map<Department, Boolean> the map of Department and boolean value for it, department will be null if
	 * given Contact is found as a production approver for the specified path; else it will have the department for which the contact is an approver.
	 */
	public static Map<List<Department>, Boolean> isContactInPath(ApprovalPath path, Contact contact) {
		Map<List<Department>, Boolean> approverMap = new HashMap<>();
		try {
			if (path == null) {
				approverMap.put(null, false);
				return approverMap;
			}
			if (path.getUsePool()) { // take out all the contacts from the pool
				if (path.getApproverPool() != null) {
					if (path.getApproverPool().contains(contact)) {
						approverMap.put(null, true);
						return approverMap;
					}
				}
				Approver finalApp = path.getFinalApprover();
				if (finalApp != null && finalApp.getContact().equals(contact)) {
					approverMap.put(null, true);
					return approverMap;
				}
			}
			else { // linear case
				if (path.getApprover() != null) { // production's first approver
					Approver app = path.getApprover();
					while (app != null) { // follow the chain and add all the production approver contacts
						if (app.getContact().equals(contact)) {
							approverMap.put(null, true);
							return approverMap;
						}
						app = app.getNextApprover();
					}
				}
			}
			List<ApprovalPathAnchor> pathAnchorList = ApprovalPathAnchorDAO.getInstance().findByProperty("approvalPath", path);
			if (pathAnchorList != null) {
				//ApproverGroup Changes
				Map<ApproverGroup, List<Department>> mapOfAppGroupDepartments = new HashMap<>();
				ApproverGroup groupKey = null;
				for (ApprovalPathAnchor groupAnchor : pathAnchorList) {
					//Fetch distinct Approver groups in anchors.
					if (groupKey == null ) {
						groupKey = groupAnchor.getApproverGroup();
						if (groupKey != null) {
							log.debug("Group Key = " + groupKey.getGroupName());
						}
					}
					if (groupKey != null && (! mapOfAppGroupDepartments.containsKey(groupKey))) {
						log.debug("Group Key = " + groupKey.getGroupName());
						List<Department> departments = new ArrayList<>();
						for (ApprovalPathAnchor anchor : pathAnchorList) {
							if (anchor.getApproverGroup() != null && anchor.getApproverGroup().equals(groupKey)) {
								departments.add(anchor.getDepartment());
							}
						}
						if (! departments.isEmpty()) {
							mapOfAppGroupDepartments.put(groupKey, departments);
						    log.debug("Size of mapOfAppGroupDepartment = " + mapOfAppGroupDepartments.size());
						}
					}
					groupKey = null;
				}
				if (! mapOfAppGroupDepartments.isEmpty()) {
					List<Department> deptList = new ArrayList<>();
					for (ApproverGroup key : mapOfAppGroupDepartments.keySet()) {
						key = ApproverGroupDAO.getInstance().refresh(key);
						if (key.getUsePool()) { // take out all the contacts from the pool
							if (key.getGroupApproverPool() != null) {
								if (key.getGroupApproverPool().contains(contact)) {
									deptList.addAll(mapOfAppGroupDepartments.get(key));
								}
							}
						}
						else { // linear case
							if (key.getApprover() != null) { // Group's first approver
								Approver app = key.getApprover();
								while (app != null) { // follow the chain and check all the approver contacts
									app = ApproverDAO.getInstance().refresh(app);
									if (app.getContact().equals(contact)) {
										deptList.addAll(mapOfAppGroupDepartments.get(key));
									}
									app = app.getNextApprover();
								}
							}
						}
					}
					if (deptList != null && deptList.size() > 0) {
						approverMap.put(deptList, true);
						return approverMap;
					}
				}
				/*for (ApprovalPathAnchor anchor : pathAnchorList) { // loop through all the department's anchor
					if (anchor.getApprover() != null) {
						Approver app = anchor.getApprover();
						Inner:
						while (app != null) { // follow each anchor's chain and add all the approver contacts
							if (app.getContact().equals(contact)) {
								deptList.add(anchor.getDepartment());
								break Inner;
							}
							app = app.getNextApprover();
						}
					}
				}
				if (deptList != null && deptList.size() > 0) {
					approverMap.put(deptList, true);
					return approverMap;
				}*/
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approverMap;
	}

	/** Method to determine whether the given contact is an approver or not, It will also check the department of approver, if contact
	 *  is a department approver, method will return true for the contact documents of same department and false for others departments.
	 * @param currentContactChainMap Map of chain and list of department for which the current contact is an approver.
	 * @param cd Contact Document
	 * @param contact Logged in contact
	 * @return True if the current contact is an approver for the given contact document, considering the department (if any) of the approver also.
	 */
	public static boolean isContactInChainMap (Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap, ContactDocument cd, Contact contact, Project project) {
		try {
			if (cd != null && currentContactChainMap != null && currentContactChainMap.size() > 0) {
				cd = ContactDocumentDAO.getInstance().refresh(cd);
				Map<DocumentChain, List<Department>> chainMap = new HashMap<>();
				if (SessionUtils.getNonSystemProduction() == null) {
					return false; // not in a production, e.g., in My Starts.
				}
				if (SessionUtils.getNonSystemProduction().getType().isAicp()) {
					chainMap = currentContactChainMap.get(project);
				}
				else {
					chainMap = currentContactChainMap.get(null);
				}
				DocumentChain chain = cd.getDocumentChain();
				if (chainMap != null ) {
					if (! chainMap.containsKey(chain)) {
						return false;
					}
					else if (chainMap.get(chain) == null) { // Current contact is Production Approver
						return true;
					}
					else if (chainMap.get(chain) != null) { // Current contact is Department Approver
						List<Department> deptList = chainMap.get(chain);
						if (deptList.contains(cd.getDepartment())) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/** Method to determine whether the given contact is an editor or not.
	 * @param chainEditorMap Map of project and list of DocumentChain for which the current contact is an editor.
	 * @param cd Contact Document
	 * @param contact Logged in contact
	 * @return True if the current contact is an editor for the given contact document.
	 */
	public static boolean isEditorInChainMap(Map<Project, List<DocumentChain>> chainEditorMap, ContactDocument cd, Contact contact, Project project) {
		try {
			if (cd != null && chainEditorMap != null && chainEditorMap.size() > 0) {
				List<DocumentChain> chainList = new ArrayList<>();
				if (SessionUtils.getNonSystemProduction() == null) {
					return false; // not in a production, e.g., in My Starts.
				}
				if (SessionUtils.getNonSystemProduction().getType().isAicp()) {
					chainList = chainEditorMap.get(project);
				}
				else {
					chainList = chainEditorMap.get(null);
				}
				DocumentChain chain = cd.getDocumentChain();
				if (chainList != null ) {
					if (chainList.contains(chain)) {
						return true;
					}
					else {
						return false;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Determine if a Contact is any type of approver for their Production,
	 * including a Project approver for any project if this is a Commercial
	 * production.
	 *
	 * @param contact The Contact of interest, which also determines the
	 *            Production of interest.
	 * @return True if the given Contact is either a Production approver, a
	 *         Project approver (for any Project), a departmental approver for
	 *         any Department, or a one-time (out-of-line) approver. The
	 *         Production checked is the one associated with the Contact.
	 */
	public static boolean isAnyApprover(Contact contact) {
		if (isProdApprover(contact)) {
			return true;
		}
		return ApproverDAO.getInstance().existsContact(contact);
	}

	/**
	 * Determines if the given Contact is an approver for the Production to
	 * which the Contact belongs, or for the specified Project, or for any
	 * department within the project.
	 *
	 * @param contact The Contact of interest, which also determines the
	 *            Production of interest.
	 * @param project The Project of interest; if null, then for commercial
	 *            productions, true will be returned if the Contact is a project
	 *            approver for any project in the production.
	 * @return True iff 'contact' is an approver either for their Production, or
	 *         for the given Project.
	 */
	public static boolean isApprover(Contact contact, Project project) {
		project = ProjectDAO.getInstance().refresh(project);
		boolean found = isProdOrProjectApprover(contact, project);
		if (! found) {
			Collection<Department> list = createDepartmentsApproved(contact, project);
			if (list.size() > 0) {
				found = true;
			}
		}
		return found;
	}

	/**
	 * Determines if the given Contact is an approver for the specified
	 * Department, or the specified Project, or the Production to which the
	 * Contact belongs.
	 *
	 * @param contact The Contact of interest, which also determines the
	 *            Production of interest.
	 * @param project The Project of interest; if null, then for commercial
	 *            productions, true will be returned if the Contact is a project
	 *            approver for any project in the production.
	 * @param dept The Department of interest.
	 * @return True iff 'contact' is an approver either for the Department
	 *         given, or for their Production, or for the project given (if not
	 *         null).
	 */
	public static boolean isApprover(Contact contact, Project project, Department dept) {
		boolean found = isProdOrProjectApprover(contact, project);
		if (! found) {
			found = isDeptApprover(contact, project, dept);
		}
		return found;
	}

	/**
	 * Determines if the given User (Contact) is an approver for the Production
	 * to which the Contact belongs.
	 *
	 * @param contact The Contact to be checked.
	 * @return True iff 'contact' is a Production approver.
	 */
	public static boolean isProdApprover(Contact contact) {
		boolean found = false;
		if (contact != null) {
			Production prod = contact.getProduction();
			prod = ProductionDAO.getInstance().refresh(prod);
			Approver app = prod.getApprover();
			while (app != null && ! found) {
				found = app.getContact().getId().equals(contact.getId());
				app = app.getNextApprover();
			}
		}
		return found;
	}

	/**
	 * Determines if the given User (Contact) is an approver for the specified
	 * Project. This is only meaningful in a Commercial production.
	 *
	 * @param contact The Contact to be checked; if null, false is returned.
	 * @param project The Project to be checked; if null, for commercial
	 *            productions true will be returned if the contact is an
	 *            approver for any project in their production.
	 * @return True iff 'contact' is a Project approver within the specified
	 *         project (if specified), or any project (if not specified). For
	 *         TV/Feature productions, this will always return false.
	 */
	public static boolean isProjectApprover(Contact contact, Project project) {
		boolean found = false;
		if (contact != null) {
			Integer id = contact.getId();
			if (project != null) {
				found = isInProjectApproverChain(project, id);
			}
			else if (contact.getProduction().getType().isAicp()) {
				// check all projects in a Commercial production
				for (Project proj : contact.getProduction().getProjects()) {
					found = isInProjectApproverChain(proj, id);
					if (found) {
						break;
					}
				}
			}
		}
		return found;
	}

	/**
	 * Determine if the given id is a contact.id value in the chain of project
	 * approvers for the given Project.
	 *
	 * @param project The project of interest; if null, false is returned.
	 * @param id The contact.id value of interest; if null, false is returned.
	 * @return True iff the id value is found in one of the Contact_id fields in
	 *         an Approver object in the chain of approvers rooted at the given
	 *         Project.
	 */
	private static boolean isInProjectApproverChain(Project project, Integer id) {
		boolean found = false;
		if (id != null  && project != null) {
			Approver app = project.getApprover();
			while (app != null && ! found) {
				app = ApproverDAO.getInstance().refresh(app);
				found = app.getContact().getId().equals(id);
				app = app.getNextApprover();
			}
		}
		return found;
	}

	/**
	 * Determines if the given User (Contact) is either a Production approver,
	 * or an approver for the specified Project.
	 *
	 * @param contact The Contact to be checked.
	 * @param project The Project of interest; if null, then for commercial
	 *            productions, true will be returned if the Contact is a project
	 *            approver for any project in the production.
	 * @return True iff 'contact' is a Production approver, or a Project approver
	 *         within the specified project.
	 */
	public static boolean isProdOrProjectApprover(Contact contact, Project project) {
		if (isProdApprover(contact)) {
			return true;
		}
		return isProjectApprover(contact, project);
	}

	/**
	 * Determines if the given Contact is an approver for the specified Department.
	 *
	 * @param contact The Contact to be checked.
	 * @param dept The Department of interest.
	 * @return True iff 'contact' is an approver for the Department given.
	 */
	public static boolean isDeptApprover(Contact contact, Project project, Department dept) {
		boolean found = false;
		Production prod = contact.getProduction();
		ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, dept);
		if (anchor != null) {
			Approver app = anchor.getFirstApprover();
			while(app != null && ! found) {
				found = app.getContact().getId().equals(contact.getId());
				app = app.getNextApprover();
			}
		}
		return found;
	}

	/**
	 * Determine if 'id' is the Contact.id value of the next approver for the
	 * given Approvable object.
	 *
	 * @param appr The Approvable of interest.
	 * @param id The value to be compared against the next approver's
	 *            Contact.id.
	 * @return True iff the given WeeklyTimecard has a next approver, and the
	 *         Contact representing that approver has an id value of the 'id'
	 *         parameter.
	 */
	public static boolean isNextApprover(Approvable appr, Integer id) {
		if (appr.getApproverId() == null || id == null) {
			return false;
		}
		if (appr.getApproverId() != null && appr.getApproverId() < 0) { // This is the POOL case
			//ApproverGroup Changes
			Integer pathOrGroupId = -(appr.getApproverId());
			if (pathOrGroupId != null) {
				Set<Contact> contactPool = new HashSet<>();
				if (appr.getIsDeptPool()) { // Department pool, Group Id
					ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(pathOrGroupId);
					if (approverGroup != null) {
						contactPool = approverGroup.getGroupApproverPool();
					}
				}
				else { // Production Pool, Path Id
					ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathOrGroupId);
					if (path != null) {
						contactPool = path.getApproverPool();
					}
				}
				if (! contactPool.isEmpty()) {
					for (Contact con : contactPool) {
						if (con.getId().equals(id)) {
							return true;
						}
					}
				}
			}
			return false; // for DH testing, assume user is in pool of approvers
		}
		if (appr.getApproverContactId() == null) {
			Approver app = ApproverDAO.getInstance().findById(appr.getApproverId());
			if (app == null || app.getContact() == null) {
				appr.setApproverContactId(-1);
				return false;
			}
			appr.setApproverContactId(app.getContact().getId());
		}
		return appr.getApproverContactId().equals(id);
	}

	/**
	 * Determines if the given Contact is a 'time keeper' for any Department in
	 * the specified Project, or for any project in the Production.
	 *
	 * @param contact The Contact to be checked.
	 * @param project The Project to be checked; if null, then check for any
	 *            Project (or no project for TV/Feature productions).
	 * @return True iff 'currentUser' is the designated time-keeper for the
	 *         given parameters.
	 */
	public static boolean isTimeKeeper(Contact contact, Project project) {
		return DepartmentDAO.getInstance().existsTimekeeper(contact, project);
	}

	/**
	 * Determines if the given Contact is a 'time keeper' for the specified
	 * Department.
	 *
	 * @param contact The Contact to be checked.
	 * @param project
	 * @param dept The Department of interest.
	 * @return True iff 'currentUser' is the designated time-keeper for the
	 *         given Department.
	 */
	public static boolean isTimeKeeper(Contact contact, Project project, Department dept) {
		Production prod = contact.getProduction();
		dept = DepartmentDAO.getInstance().refresh(dept);
		Department customDept = DepartmentDAO.getInstance().findByProductionProjectDept(prod, project, dept.getName());
		if (customDept != null && customDept.getTimeKeeper() != null) {
			if (customDept.getTimeKeeper().getId().equals(contact.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if a given Contact follows a given current approver.
	 *
	 * @param currApproverId The current approver id for the Approvable [WeeklyTimecard or ContactDocument].
	 * @param contact The Contact we are looking for in the approval chain.
	 * @param department The department associated with the timecard.
	 * @param path The ApprovalPath followed by the ContactDocument being pulled.
	 * @param isDeptPool Null if the Approvable is a WeeklyTimecard and
	 * not Null if the Approvable is ContactDocument, true if ContactDocument is waiting for Department Pool else false.
	 * @return True iff the given Contact appears 'later' in the approval chain
	 *         than the current approver id. (The contact may ALSO appear earlier
	 *         in the chain; this does not affect the result.)
	 */
	public static boolean followsCurrentApprover(Integer currApproverId, Contact contact, Project project,
			Department department, ApprovalPath path, Boolean isDeptPool) {
		log.debug("currApproverId = " + currApproverId + ", isDeptPool = " + isDeptPool);
		if (currApproverId == null) { // no more approvers (has final approval)
			// so "contact" cannot be later in the chain!
			log.debug(" null app id ");
			return false;
		}

		Production prod = contact.getProduction();
		Boolean bRet = null;
		//ApproverGroup Changes
		// For Approval Path
		if (path != null) {
			//Approver Pool case.
			if (path.getUsePool()) {
				//Current contact is Final Approver.
				if (path.getUseFinalApprover() && path.getFinalApprover() != null &&
						path.getFinalApprover().getContact().equals(contact)) {
					if (currApproverId < 0) {
						bRet = true;
						log.debug(" bRet = " + bRet);
					}
					else if (currApproverId.equals(path.getFinalApprover().getId())) {
						bRet = false;
						log.debug(" bRet = " + bRet);
					}
				}
				// Current contact is a Production Pool Approver.
				else if (path.getApproverPool() != null && path.getApproverPool() != null && path.getApproverPool().contains(contact)) {
					log.debug("currApproverId = " + currApproverId + ", isDeptPool = " + isDeptPool);
					// Document is in Production Pool itself.
					if (currApproverId < 0 && (! isDeptPool)) {
						bRet = false;
						log.debug(" bRet = " + bRet);
					}
					// Document is in Department Pool.
					else if (currApproverId < 0 && isDeptPool) {
						/* Suppose, the current contact belongs to the both Production pool and Department Pool and currently
					 	logged in as a Department Pool Approver then we need to hide the Pull Button as the Document is
						also waiting for the Department pool's Approval.
						The following check should prevent an unwanted case, Pull Button with Approve/Reject Buttons.*/
						Integer groupId = -(currApproverId);
						ApproverGroup group = ApproverGroupDAO.getInstance().findById(groupId);
						if (group != null && group.getGroupApproverPool() != null &&
								group.getGroupApproverPool().contains(contact)) {
							bRet = false;
							log.debug(" bRet = " + bRet);
						}
						else {
							bRet = true;
							log.debug(" bRet = " + bRet);
						}
					}
				}
				/* Old Code
				 if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
					if (path.getFinalApprover().getContact().equals(contact)) {
						bRet = true;
					}
					else if (currApproverId.equals(path.getFinalApprover().getId())) {
						bRet = false;
					}
					else {
						bRet = path.getApproverPool().contains(contact);
					}
				}
				else {
					bRet = path.getApproverPool().contains(contact);
				}*/
			}
			// Linear Path
			else {
				//TODO Resolve the problem, recall and pull buttons are visible at same time since same contact exists in both,
				//department approvers and production approvers.
				bRet = followsApprover(currApproverId, contact, path);
				log.debug(" bRet = " + bRet);
			}
		}
		// For Weekly Timecard
		else {
			bRet = followsApprover(currApproverId, contact, prod);
		}
		// For Weekly Timecard
		if (bRet == null && project != null && path == null) {
			// no decision from production chain; check Project chain (if any)
			bRet = followsApprover(currApproverId, contact, project);
			log.debug(" bRet = " + bRet);
		}

		if (bRet == null && department != null) {
			// no decision yet; check Department chain (if any)
			// For Approval Path
			if (path != null) {
				ApprovalPathAnchor pathAnchor = getApprovalPathAnchor(project, department, path.getId());
				if (pathAnchor != null && pathAnchor.getApproverGroup() != null) {
					ApproverGroup group = pathAnchor.getApproverGroup();
					if (group != null) {
						if (! group.getUsePool()) {
							bRet = followsApprover(currApproverId, contact, group);
							log.debug(" bRet = " + bRet);
						}
						else if (group.getGroupApproverPool() != null && isDeptPool &&
								group.getGroupApproverPool().contains(contact)) {
							// Department's Pool Approver can never pull a Document.
							bRet = false;
							log.debug(" bRet = " + bRet);
						}
					}
				}
			}
			// For Weekly Timecard
			else {
				ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, department);
				if (anchor != null) {
					bRet = followsApprover(currApproverId, contact, anchor);
					log.debug(" bRet = " + bRet);
				}
			}
		}

		if (bRet == null) { // odd - never found 'contact' at all!
			bRet = false; // so return false
			log.debug(" bRet = " + bRet);
		}
		log.debug(" bRet = " + bRet);
		return bRet;
	}

	/**
	 * Determine if a given Contact follows a given approver in a given
	 * approval chain.
	 *
	 * @param currApproverId The current approver id for the Approvable [WeeklyTimecard or ContactDocument].
	 * @param contact The Contact we are looking for in the approval chain.
	 * @param root The root of the chain to be searched.
	 * @return null if neither the "currApproverId" nor the "contact" was found
	 *         in the list; returns True if the 'currApproverId' is found
	 *         first; returns False if the 'contact' is found first.
	 */
	private static Boolean followsApprover(Integer currApproverId, Contact contact, ApproverChainRoot root) {
		log.debug("");
		boolean foundContact = false;
		boolean foundApprover = false;
		Approver app = root.getApprover(); // get the first approver in the chain, if any.
		if (app != null) {
			app = ApproverDAO.getInstance().refresh(app); // fixes LazyInit Exception
		}
		while (app != null) {
			if (app.getId().equals(currApproverId)) {
				if (foundContact) {
					return false; // approver follows contact
				}
				foundApprover = true;	// found current approver
			}
			if (app.getContact() != null && app.getContact().equals(contact)) {
				if (foundApprover) {
					return true;	// contact follows approver
				}
				foundContact = true;	// found 'contact'
			}
			app = app.getNextApprover();
		}
		// at this point, we did not find both contact and approver
		if (foundContact) {
			return true; // Only found contact
		}
		if (foundApprover) {
			return false; // only found approver
		}
		return null; // neither entry found in this chain.
	}

	/**
	 * Determine if a given Contact precedes a given current approver.
	 *
	 * @param currApproverId The current approver id for the Approvable [WeeklyTimecard or ContactDocument].
	 * @param contact The Contact we are looking for in the approval chain.
	 * @param department The department associated with the timecard.
	 * @param path The ApprovalPath followed by the ContactDocument being recalled.
	 * @param isDeptPool Null if the Approvable is a WeeklyTimecard and
	 * not Null if the Approvable is ContactDocument, true if ContactDocument is waiting for Department Pool else false.
	 * @return True iff the given Contact comes 'earlier' in the approval chain
	 *         than the current approver id.
	 */
	public static boolean precedesCurrentApprover(Integer currApproverId, Contact contact, Project project,
			Department department, ApprovalPath path, Boolean isDeptPool) {
		log.debug("");
		if (currApproverId == null) { // no more approvers (has final approval)
			// so "contact" MUST be earlier in the chain!
			log.debug(" Null app id " );
			return true;
		}
		Boolean bRet = null;
		Production prod = contact.getProduction();
		if (department != null) {
			// For Approval Path
			if (path != null) {
				//ApproverGroup Changes
				ApprovalPathAnchor pathAnchor = getApprovalPathAnchor(project, department, path.getId());
				if (pathAnchor != null && pathAnchor.getApproverGroup() != null) {
					ApproverGroup group = pathAnchor.getApproverGroup();
					if (group != null) {
						if (! group.getUsePool()) {
							bRet = precedesApprover(currApproverId, contact, group);
							log.debug(" bRet = " + bRet);
						}
						// isDeptPool is to prevent the condition where same Contact exists in both Department Pool and Production Pool
						else if (group.getGroupApproverPool() != null && (! isDeptPool) &&
								group.getGroupApproverPool().contains(contact)) {
							bRet = true;
							log.debug(" bRet = " + bRet);

							// Document is in Department Pool itself.
							if (currApproverId < 0 && isDeptPool) {
								bRet = false;
								log.debug(" bRet = " + bRet);
							}
							// Document is in Production Pool.
							else if (currApproverId < 0 && (! isDeptPool)) {
								/* Suppose, the current contact belongs to the both Production pool and Department Pool and currently
							 	logged in as a Production Pool Approver then we need to hide the Recall Button as the Document is
								also waiting for the Production pool's Approval.
								The following check should prevent an unwanted case, Recall Button with Approve/Reject Buttons.*/
								if (path != null && path.getApproverPool() != null &&
										path.getApproverPool().contains(contact)) {
									bRet = false;
									log.debug(" bRet = " + bRet);
								}
								else {
									bRet = true;
									log.debug(" bRet = " + bRet);
								}
							}
						}
					}
				}
				/*if (pathAnchor != null) {
					bRet = precedesApprover(currApproverId, contact, pathAnchor);
				}*/
			}
			// For Weekly Timecard
			else {
				ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, department);
				if (anchor != null) {
					bRet = precedesApprover(currApproverId, contact, anchor);
				}
			}
		}

		// For Weekly Timecard
		if (bRet == null && project != null && path == null) {
			// no decision from department chain (if any); check Project chain (if any)
			bRet = precedesApprover(currApproverId, contact, project);
			log.debug(" bRet = " + bRet);
		}

		// For Weekly Timecard
		if (bRet == null && path == null) {
			// no decision from department or project chains (if any); check production chain
			bRet = precedesApprover(currApproverId, contact, prod);
		}

		// For Approval Path
		if (bRet == null && path != null) {
			bRet = precedesApprover(currApproverId, contact, path);
			log.debug(" bRet = " + bRet);
		}

		// For Approval Path
		if (bRet == null && path != null && path.getUsePool() && path.getUseFinalApprover() &&
				path.getFinalApprover().getId().equals(currApproverId)) {
			if (path.getApproverPool().contains(contact)) {
				// Pool path, and doc is waiting for Final approver, so anyone in pool "precedes" the final approver.
				bRet = true;
				log.debug(" bRet = " + bRet);
			}
		}

		if (bRet == null) { // odd - never found 'contact' at all!
			bRet = false; // so return false
			log.debug(" bRet = " + bRet);
		}
		log.debug(" bRet = " + bRet);
		return bRet;
	}

	/**
	 * Determine if a given Contact precedes a given approver in a given
	 * approval chain.
	 *
	 * @param currApproverId The current approver id for the Approvable [WeeklyTimecard or ContactDocument].
	 * @param contact The Contact we are looking for in the approval chain.
	 * @param root The root of the chain to be searched.
	 * @return null if neither the "currApproverId" nor the "contact" was found
	 *         in the list; returns False if the 'currApproverId' is found
	 *         first; returns True if the 'contact' is found first.
	 */
	private static Boolean precedesApprover(Integer currApproverId, Contact contact, ApproverChainRoot root) {
		Approver app = root.getApprover(); // get the first approver in the chain, if any.
		if (app != null) {
			app = ApproverDAO.getInstance().refresh(app); // fixes LazyInit Exception
		}
		while (app != null) {
			if (app.getId().equals(currApproverId)) {
				return false;	// found current approver before 'contact'
			}
			if (app.getContact() != null && app.getContact().equals(contact)) {
				return true;	// found 'contact' first
			}
			app = app.getNextApprover();
		}
		return null; // neither entry found in this chain.
	}

	/**
	 * Update the status values of each TimecardEntry in the list based on whether the
	 * current user is the next approver or not for that Timecard.
	 */
	public static void calculateListApprovalStatus(List<TimecardEntry> tceList) {
		Contact currentApprContact = SessionUtils.getCurrentContact();
		if (currentApprContact == null) {
			// happens rarely, if a logout event and a valueChangeListener event are generated
			// on the same input cycle.
			return;
		}
		for (TimecardEntry tce : tceList) {
			ApproverUtils.calculateApprovalStatus(tce, currentApprContact);
		}
	}

	/**
	 * Update the *VIEW* status value of one ContactDocument based on whether the given
	 * user is the next approver or not for that ContactDocument.
	 */
	public static void calculateApprovalViewStatus(ContactDocument appr, Contact currentApprContact) {
		appr.setViewStatus(findRelativeStatus(appr, currentApprContact));
//		String name;
//		if (appr.getStatus().isFinal()) {
//			name = WAITING_FOR_TEXT_APPROVED;
//		}
//		else {
//			name = findApproverName(appr);
//			if (name == null || name.length() == 0) {
//				name = WAITING_FOR_TEXT_SUBMITTAL;
//			}
//		}
//		return name;
	}

	/** Utility method used to calculate the waiting for field for the passed contact document
	 * @param cd contact document to figure who is it waiting for
	 * @return waiting for string
	 */
	public static String calculateWaitingFor(ContactDocument cd) {
		String name = null;
		Approver approver = null;
		if (cd.getStatus() == ApprovalStatus.VOID) {
			name = WAITING_FOR_TEXT_VOID;
		}
		else if (cd.getStatus() == ApprovalStatus.PENDING) {
			name = WAITING_FOR_TEXT_PENDING;
		}
		else if (cd.getStatus().isFinal()) {
			name = WAITING_FOR_TEXT_APPROVED;
		}
		else if (cd.getStatus() == ApprovalStatus.SUBMITTED_NO_APPROVERS) {
			name = WAITING_FOR_TEXT_NO_PATH;
		}
		else {
			if (cd.getApproverId() == null) {
				name = WAITING_FOR_TEXT_SUBMITTAL;
			}
			else if (cd.getApproverId() < 0) {
				if (cd.getIsDeptPool()) {
					name = WAITING_FOR_TEXT_DEPT_POOL;
				}
				else {
					name = WAITING_FOR_TEXT_PROD_POOL;
				}
				//name = WAITING_FOR_TEXT_POOL;
			}
			else {
				approver = ApproverDAO.getInstance().findById(cd.getApproverId());
				if (approver != null) {
					name = approver.getContact().getUser().getLastNameFirstName();
				}
				else {
					name = WAITING_FOR_TEXT_UNKNOWN;
				}
			}
		}
		return name;
	}

	/**
	 * Update the status values of one TimecardEntry based on whether the given
	 * user is the next approver or not for that Timecard. Also sets the
	 * approver name field for display on the dashboards.
	 */
	public static void calculateApprovalStatus(TimecardEntry tce, Contact currentApprContact) {
		tce.setStatus(findRelativeStatus(tce.getWeeklyTc(), currentApprContact));
		String name;
		if (tce.getStatus().isFinal()) {
			name = WAITING_FOR_TEXT_APPROVED;
		}
		else if (tce.getStatus() == ApprovalStatus.VOID) {
			name = WAITING_FOR_TEXT_TC_VOID;
		}
		else {
			name = findApproverName(tce.getWeeklyTc());
			if (name == null || name.length() == 0) {
				name = WAITING_FOR_TEXT_SUBMITTAL;
			}
		}
		tce.setApproverName(name);
	}

	/**
	 * Determine the appropriate WeeklyStatus that represents the relationship
	 * between the given WeeklyTimecard and the person whose Contact.id value is
	 * 'currentApprId'. This is used by the Approval Dashboard page.
	 *
	 * @param appr The WeeklyTimecard of interest.
	 * @param currentApprContact The Contact representing the current viewer of the dashboard.
	 * @return The current WeeklyStatus for the given WeeklyTimecard, modified
	 *         to reflect the approval status with relation to the current
	 *         viewer, e.g., the timecard was already approved by this user or
	 *         not.
	 */
	@SuppressWarnings("rawtypes")
	public static ApprovalStatus findRelativeStatus(Approvable appr, Contact currentApprContact) {
		Integer apprContactId = currentApprContact.getId();
		ApprovalStatus stat = appr.getStatus();
		if (stat == ApprovalStatus.SUBMITTED) {
			if (isNextApprover(appr, apprContactId)) {
				stat = ApprovalStatus.APPROVED_READY;
			}
			else if (appr.getApproverId() != null) { // handles both linear and Pool cases.
				String currAcct = currentApprContact.getUser().getAccountNumber();
				for (SignedEvent evt : appr.getEvents()) {
					if (evt.getType() == TimedEventType.APPROVE
							&& evt.getUserAccount().equals(currAcct)) {
						stat = ApprovalStatus.APPROVED_PAST; // already approved
						break;
					}
				}
				if (stat != ApprovalStatus.APPROVED_PAST && appr.getUnderContract()) {
					Production prod = currentApprContact.getProduction();
					Project project = TimecardUtils.findProject(prod, appr);
					if (! ApproverUtils.isProdOrProjectApprover(currentApprContact, project)) {
						// must be department approver - auto-skipped due to "under contract" setting
						stat = ApprovalStatus.APPROVED_PAST; // treat same as "already approved"
					}
				}
			}
		}
		else if (stat.isPendingApproval()) { // recalled, resubmitted, rejected
			if (appr.getApproverId() != null) {
				if (isNextApprover(appr, apprContactId)) {
					if (stat == ApprovalStatus.RESUBMITTED) {
						stat = ApprovalStatus.RESUBMITTED_READY;
					}
					else if (stat == ApprovalStatus.RECALLED) {
						stat = ApprovalStatus.RECALLED_READY;
					}
					else {
						stat = ApprovalStatus.REJECTED_READY;
					}
				}
				else if (appr.getApproverId() > 0) { // to avoid the "else" code for Pool case.
					String currAcct = currentApprContact.getUser().getAccountNumber();
					for (int i = appr.getEvents().size()-1; i > 0; i--) {
						SignedEvent evt = appr.getEvents().get(i);
						if (evt.getType() == TimedEventType.REJECT ||
								evt.getType() == TimedEventType.RECALL) {
							break; // no Approve since reject or recall
						}
						if (evt.getType() == TimedEventType.APPROVE
								&& evt.getUserAccount().equals(currAcct)) {
							stat = ApprovalStatus.REJECTED_PAST; // approved after Reject/recall
							break;
						}
					}
				}
			}
		}
		return stat;
	}

	/**
	 * Create the SelectItem list representing the list of approvers for the
	 * given Department. The values in the SelectItem entries are Approver.id
	 * (not Contact.id); the labels are of the form 'lastName, firstName - role'.
	 *
	 * @param department The Department of interest.
	 * @return A non-null, but possibly empty, List of SelectItem`s representing
	 *         all the Department-level approvers for the given Department.
	 */
	public static List<SelectItem> createDeptApproverList(Production prod, Project project, Department department) {
		List<SelectItem> listOut = new ArrayList<>();
		List<SelectItem> listIn = createDeptApproverList(prod, project, department, null, false);
		// reverse the order of the list:
		for (SelectItem se : listIn) {
			listOut.add(0,se);
		}
		return listOut;
	}

	/**
	 * Create a SelectItem List representing the list of all possible approvers
	 * in the given Production. The values in the SelectItem entries are
	 * Contact.id, not Approver.id; the labels are of the form 'lastName,
	 * firstName'.
	 *
	 * @param prod The Production of interest.
	 * @return A non-null, but possibly empty, List of SelectItem`s representing
	 *         all the users in the current Production who have the
	 *         APPROVE_TIMECARD Permission.
	 */
	public static List<SelectItem> createApproverContactList(Production prod) {
		List<SelectItem> approverItems = new ArrayList<>();
		List<Contact> contacts = ProjectMemberDAO.getInstance()
				.findByProductionPermissionDistinctContact(prod, Permission.APPROVE_TIMECARD);
//		Integer current = SessionUtils.getCurrentContact().getId();
		for (Contact contact : contacts) {
//			if (! contact.getId().equals(current)) {
				approverItems.add(new SelectItem(contact.getId(), contact.getUser().getLastNameFirstName()));
//			}
		}
		Collections.sort(approverItems, ListView.getSelectItemComparator());
		return approverItems;
	}

	/**
	 * Create SelectItems for all Approvers in a chain and add them to the given
	 * list. Used to populate Production, Department, and rejection selection
	 * lists. The values in the SelectItem entries are Approver.id (not
	 * Contact.id); the labels are of the form 'lastName, firstName - role'.
	 *
	 * @param approver The first Approver in the chain.
	 * @param items The List to be populated; any existing entries in the List
	 *            are not changed or removed.
	 */
	public static void createApproverList(Approver approver, List<SelectItem> items) {
		createApproverList(approver, items, null, false);
	}

	/**
	 * Create a Collection of Department`s for which the given Contact is an
	 * approver.
	 *
	 * @param contact The Contact of interest.
	 * @param project The Project of interest; if null, this parameter is ignored.
	 * @return A non-null, but possibly empty, Collection of Department objects.
	 *         The given Contact is a member of the approval chain for each of
	 *         the Department`s in this Collection.
	 */
	public static Collection<Department> createDepartmentsApproved(Contact contact, Project project) {
		Collection<Department> depts = new ArrayList<>();
		int contactId = contact.getId().intValue();
		List<ApprovalAnchor> anchors = ApprovalAnchorDAO.getInstance()
				.findByProductionProjectDepartmental(contact.getProduction(), project);
		for (ApprovalAnchor anchor : anchors) {
			Approver app = anchor.getFirstApprover();
			while(app != null) {
				if (app.getContact() != null) {
					if (app.getContact().getId().intValue() == contactId) {
						depts.add(anchor.getDepartment());
						break;
					}
				}
				app = app.getNextApprover();
			}
		}
		return depts;
	}

	/**
	 * Update a List of Contact`s representing the approvers in a contiguous
	 * portion of the approval chain for the specified department (within the
	 * specified Production). Entries are added to the List beginning with the
	 * first approver after the Contact identified by the 'startAfter'
	 * parameter, and continuing at least up to the Contact identified by the
	 * Approver object with the given 'stopApproverId' value. If 'includesStop'
	 * is true, then this last Contact is also added to the List.
	 *
	 * @param prod
	 *
	 * @param department The Department whose approval chain should be used.
	 * @param list The List to be updated with the entries found. Existing
	 *            entries in the List are not changed; the new entries are added
	 *            to the end of the List.
	 * @param startAfter The Contact to be searched for in the approval chain;
	 *            Contacts are not added to the List until this Contact is
	 *            found. (This Contact is NOT added to the List.) If this is
	 *            null, entries will be added beginning with the first
	 *            departmental approver.
	 * @param stopApproverId The Approver.id value which stops the process of
	 *            adding Contacts to the List. The Contact represented by this
	 *            Approver object will be added to the List iff 'includeStop' is
	 *            true.
	 * @param includeStop determines if the Contact described by the Approver
	 *            matching the stopApproverId value is added to the List or not.
	 * @param approvalPath
	 * @return True iff the Approver whose id value is 'stopApproverId' was
	 *         found during the processing. In other words, it will be false iff
	 *         the end of the approval chain was found without ever finding a
	 *         match on the id.
	 */
	public static boolean createDeptApproverContactList(Production prod, Project project, Department department,
			Collection<Contact> list, Contact startAfter, Integer stopApproverId, boolean includeStop, ApprovalPath approvalPath) {
		boolean stopFound = false;
		if (department != null) {
			Approver app = null;
			if (approvalPath != null) { // non-null path - this is for document approver
				Map<String, Object> values = new HashMap<>();
				values.put("production", prod);
				values.put("department", department);
				values.put("approvalPathId", approvalPath.getId());
				values.put("project", project);
				ApprovalPathAnchor pathAnchor = ApprovalPathAnchorDAO.getInstance().findOneByNamedQuery(ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_PROJECT_APPROVAL_PATH, values);
				if (pathAnchor != null) {
					//app = pathAnchor.getApprover();
					//ApproverGroup Changes
					ApproverGroup group = pathAnchor.getApproverGroup();
					if (group != null) {
						if (group.getUsePool() && group.getGroupApproverPool() != null) {
							// TODO DH: Is this change right or wrong?
							list.addAll(group.getGroupApproverPool());
						}
						else {
							app = group.getApprover();
						}
					}
				}
			}
			else { // null path - this is for timecard approver
				ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, department);
				if (anchor != null) {
					app = anchor.getFirstApprover();
				}
			}
			if (app != null) {
				stopFound = createApproverContactList(app, list, startAfter, stopApproverId, includeStop);
			}
		}
		return stopFound;
	}

	/**
	 * Update a List of Contact`s representing the approvers in a contiguous
	 * portion of any approval chain. Entries are added to the List beginning
	 * with the first approver after the Contact identified by the 'startAfter'
	 * parameter, and continuing at least up to the Contact identified by the
	 * Approver object with the given 'stopApproverId' value. If 'includesStop'
	 * is true, then this last Contact is also added to the List.
	 *
	 * @param approver The Approver object at which the scan of the approval
	 *            chain will begin. This may be any part of a departmental or
	 *            production approval chain, although it is typically the first
	 *            Approver in such a chain. If this is null, no contacts will be
	 *            added to the list.
	 * @param list The List to be updated with the entries found. Existing
	 *            entries in the List are not changed; the new entries are added
	 *            to the end of the List.
	 * @param startAfter The Contact to be searched for in the approval chain;
	 *            Contacts are not added to the List until this Contact is
	 *            found. (This Contact is NOT added to the List.) If this is
	 *            null, start adding entries immediately, with the approver
	 *            provided.
	 * @param stopApproverId The Approver.id value which stops the process of
	 *            adding Contacts to the List. The Contact represented by this
	 *            Approver object will be added to the List iff 'includeStop' is
	 *            true. If this is null, contacts will be added until the end of
	 *            the provided approver chain is reached.
	 * @param includeStop determines if the Contact described by the Approver
	 *            matching the stopApproverId value is added to the List or not.
	 * @return True iff the Approver whose id value is 'stopApproverId' was
	 *         found during the processing. In other words, it will be false iff
	 *         the end of the approval chain was found without ever finding a
	 *         match on the id.
	 */
	public static boolean createApproverContactList(Approver approver, Collection<Contact> list,
			Contact startAfter, Integer stopApproverId, boolean includeStop) {
		log.debug("");
		boolean add = (startAfter == null);
		boolean stopFound = false;
		while(approver != null) {
			if (stopApproverId != null && stopApproverId.equals(approver.getId())) {
				stopFound = true;
				if (includeStop && approver.getContact() != null) {
					list.add(approver.getContact());
				}
				break;
			}
			if (approver.getContact() != null) {
				if (add) {
					list.add(approver.getContact());
				}
				else if (approver.getContact().equals(startAfter)) {
					add = true;
				}
			}
			approver = approver.getNextApprover();
		}
		return stopFound;
	}

	/**
	 * Create a SelectItem List representing the list of approvers for the given
	 * Department, up to, but not including, the approver represented by the
	 * 'stop' parameter.
	 *
	 * @param department The Department of interest.
	 * @param stop The Contact which, if found as an approver, stops the process
	 *            of creating entries. This Contact is NOT added to the list,
	 *            and a 'null' SelectItem is added to the List instead. If null,
	 *            no check is done for a stopping point.
	 * @param useContactId If true, then the 'value' field of the SelectItem
	 *            entries created will be the Contact.id field; if false, they
	 *            will be the Approver.id field.
	 *
	 * @return A non-null, but possibly empty, List of SelectItem`s representing
	 *         the Department-level approvers for the given Department. The
	 *         values in the SelectItem entries are either Approver.id or
	 *         Contact.id, depending on the 'useContactId' parameter; the labels
	 *         are of the form 'lastName, firstName - role'. The last entry in
	 *         the List will be a null value if a non-null 'stop' parameter was
	 *         provided, and it was found.
	 */
	public static List<SelectItem> createDeptApproverList(Production prod, Project project, Department department,
			Contact stop, boolean useContactId) {
		List<SelectItem> list = new ArrayList<>();
		if (department != null) {
			ApprovalAnchor anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, department);
			if (anchor != null) {
				Approver app = anchor.getFirstApprover();
				createApproverList(app, list, stop, useContactId);
			}
		}
		return list;
	}

	/**
	 * Create SelectItems for all Approvers in a chain, up to the designated
	 * Contact, and add them to the given list. Used to populate the Production,
	 * Department, and rejection selection lists.
	 *
	 * @param approver The first Approver in the chain.
	 * @param items The List to be populated; any existing entries in the List
	 *            are not changed or removed. The values in the SelectItem
	 *            entries are either Approver.id or Contact.id, depending on the
	 *            'useContactId' parameter; the labels are of the form
	 *            'lastName, firstName - role'.
	 * @param stop The Contact which, if found as an approver, stops the process
	 *            of creating entries. This Contact is NOT added to the list,
	 *            and a 'null' SelectItem is added to the List instead. If null,
	 *            no check is done for a stopping point.
	 * @param useContactId If true, then the 'value' field of the SelectItem
	 *            entries created will be the Contact.id field; if false, they
	 *            will be the Approver.id field.
	 */
	public static void createApproverList(Approver approver, List<SelectItem> items, Contact stop, boolean useContactId) {
		StringBuffer label = new StringBuffer("");
		while(approver != null) {
			label.setLength(0);
			Contact contact = approver.getContact();
			contact = ContactDAO.getInstance().refresh(contact);
			if (stop != null && contact.equals(stop)) {
				items.add(null); // "flag" that we found stopping point
				break;
			}
			label.append(contact.getUser().getLastNameFirstName());
			if (contact.getRoleName() != null) {
				label.append(" - " + contact.getRoleName());
			}
			Integer id;
			if (useContactId) {
				id = approver.getContact().getId();
			}
			else {
				id = approver.getId();
			}
			items.add(new SelectItem(id, label.toString()));
			approver = approver.getNextApprover();
		}
	}

	/** Utility method used to get the approver chain for a department
	 * @param approver first approver (anchor approver)
	 * @param list list of approvers
	 * @return list
	 */
	public static List<Approver> getApproverChain(Approver approver, List<Approver> list) {
		try {
			if (approver != null) {
				list.add(approver);
				if (approver.getNextApprover() != null) {
					getApproverChain(approver.getNextApprover(), list);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return list;
	}

	/** Utility method used to copy approvers in an approval chain in the same order.
	 * @param sourceProject
	 * @return
	 */
	public static Approver copyApproverChain(ApproverChainRoot source) {
		List<Approver> list = new ArrayList<>();
		list = getApproverChain(source.getApprover(), list);
		Approver approver = null;
		Integer savedApprover = null;
		ApproverDAO approverDAO = ApproverDAO.getInstance();
		for (int i = (list.size() - 1); i >= 0; i--) {
			Approver sourceApp = list.get(i);
			approver = new Approver();
			approver.setContact(sourceApp.getContact());
			approver.setShared(sourceApp.getShared());
			if (savedApprover != null) {
				Approver priorApprover = approverDAO.findById(savedApprover);
				if (priorApprover != null) {
					approver.setNextApprover(priorApprover);
				}
			}
			else {
				approver.setNextApprover(null);
			}
			savedApprover = approverDAO.save(approver);
		}
		return approver;
	}

	/**
	 * Add the specified Contact as a Production approver to the first approval
	 * path within the specified Project.  As this is only used for Canadian
	 * productions, the path is assumed to be using an approver POOL (not a linear
	 * list).  LS-3661
	 *
	 * @param ct The Contact to be added as an approver.
	 * @param proj The Project whose approval path pool is to be updated.
	 */
	public static void addContactToApprovalPool(Contact ct, Project proj) {
		ApprovalPath path = ApprovalPathDAO.getInstance().findOneByProperty("project", proj);
		if (path != null) {
			Set<Contact> contacts = (path.getApproverPool() == null ?
					new HashSet<>() :
					new HashSet<>(path.getApproverPool()));
			contacts.add(ct);
			ApprovalPathsBean.getInstance().createApprovalPathPool(path, contacts, null, proj.getProduction(), false);
			ApprovalPathDAO.getInstance().attachDirty(path);
		}
	}

	/**
	 * Create a list of Contact`s to receive the Pull notification. This
	 * includes all approvers in the current chain from the person the timecard
	 * was removed from, up to, but not including, the approver who is pulling
	 * the timecard. For example, if the approval chain consists of A-B-C-D-E,
	 * and the timecard is currently waiting for B's approval, and E pulls it,
	 * then the Contacts for the approvers B, C, and D are placed in this list
	 * of recipients.
	 *
	 * @param puller The Contact who is doing the Pull -- into whose queue the
	 *            timecard is being (or has been) moved.
	 * @param tcProj The project the timecard is in, for Commercial productions.
	 * @param oldApproverId The Approver.id value of the Approver from whose
	 *            queue the timecard was pulled. This could be a Department,
	 *            Project, or Production Approver.
	 * @param path The ApprovalPath followed by the ContactDocument being recalled.
	 * @return A non-null, but possibly empty, List of Contact`s as described
	 *         above.
	 */
	public static List<Contact> createPullNotify(Contact puller, Project tcProj, Integer oldApproverId, ApprovalPath path) {
		List<Contact> list = new ArrayList<>();
		Approver approver = ApproverDAO.getInstance().findById(oldApproverId);
		if (approver == null) { // shouldn't happen!
			return list;
		}
		while(approver != null) {
			if (puller.getId().equals(approver.getContact().getId())) {
				break;
			}
			list.add(approver.getContact());
			approver = approver.getNextApprover();
		}
		if (approver == null) {
			/*
			 * this means we didn't find the "puller" in the above chain scan, so that chain
			 * was either the department or project chain, and now we need to keep adding
			 * approvers from the production chain until we find the "puller".
			 */
			if (tcProj != null && path == null) { // there may be a project approver chain
				approver = tcProj.getApprover();
				if (approver != null) {
					// we don't know if the oldApproverId was in dept or project chain.
					boolean found = false;
					while (approver != null && ! found) {
						found = approver.getId().equals(oldApproverId);
						approver = approver.getNextApprover();
					}
					if (! found) { // oldApproverId was a dept approver; add project approvers to list
						approver = tcProj.getApprover(); // start over
						while(approver != null) {
							if (puller.getId().equals(approver.getContact().getId())) {
								break;
							}
							list.add(approver.getContact());
							approver = approver.getNextApprover();
						}
					}
					else { // oldApproverId was project approver, so
						approver = null; // make sure we scan production chain
					}
				}
			}
			if (approver == null && path == null) {
				// We didn't find "puller" in project chain either, so add production approvers
				approver = puller.getProduction().getApprover();
				while(approver != null) {
					if (puller.getId().equals(approver.getContact().getId())) {
						break;
					}
					list.add(approver.getContact());
					approver = approver.getNextApprover();
				}
			}
			if (approver == null && path != null) {
				// To notify pool contacts if final approver pulls the document.
				if (path.getUsePool() && path.getUseFinalApprover() &&
						path.getFinalApprover() != null &&
						path.getFinalApprover().getContact().equals(puller)) {
					for (Contact con : path.getApproverPool()) {
						list.add(con);
					}
				}
				else {
					// We didn't find "puller" in project chain either, so add production approvers
					approver = path.getApprover();
					while(approver != null) {
						if (puller.getId().equals(approver.getContact().getId())) {
							break;
						}
						list.add(approver.getContact());
						approver = approver.getNextApprover();
					}
				}
			}
		}
		return list;
	}

	/**
	 * For production approvers, generate a sorted List of SelectItem
	 * representing the employees (User`s) for which the current Contact has
	 * approval authority. For a user who is not a production approver, an empty
	 * list is returned.
	 *
	 * @param contact The currently logged-in Contact.
	 * @param project The currently selected project for Commercial productions.
	 * @param addAll True iff an "All" select-item should be added at the top of
	 *            the list.
	 * @param userHasViewHtg True iff the current User has the ViewHtg
	 *            permission.
	 * @return A non-null (but possibly empty) List of SelectItem`s. The value
	 *         of each entry is the User.accountNumber field; the label is
	 *         "last name, first name".
	 */
	public static List<SelectItem> createEmployeeList(Contact contact, Project project, boolean addAll, boolean userHasViewHtg) {
		List<SelectItem> selectList = new ArrayList<>();
		if (ApproverUtils.isProdOrProjectApprover(contact, project) || userHasViewHtg) {
			List<User> users = TimecardUtils.findTimecardUsers();
			for (User u : users) {
				selectList.add(new SelectItem(u.getAccountNumber(), u.getLastNameFirstName()));
			}
			if (addAll && selectList.size() > 0) {
				Collections.sort(selectList, View.getSelectItemComparator());
				selectList.add(0, Constants.GENERIC_ALL_ITEM);
			}
		}
		return selectList;
	}

	/**
	 * Initialize the ApprovalPromptBean, either for (a) showing the prompt
	 * pop-up dialog on the desktop, (b) showing the Approval confirmation page
	 * on mobile, or (c) showing the "Submit & Approve" confirmation page on
	 * mobile.
	 *
	 * @param contactIdList A non-null (but possibly empty) List of Integers,
	 *            where each one is the Contact.id value of the next approver of
	 *            one or more of the timecard(s) (or other objects) being
	 *            approved. This list is used to determine if all the objects
	 *            will be forwarded to the same approver, or to multiple
	 *            approvers.
	 * @param message If non-null, an additional message to be displayed in the
	 *            prompt pop-up.
	 * @param holder The PopupHolder, used on the desktop, for the callback when
	 *            the user clicks Ok or Cancel.
	 * @param act The action code passed on the OK or Cancel callbacks.
	 * @param prefix The message prefix (only for desktop) used to generate text
	 *            within the pop-up dialog.
	 *            <p>
	 *            The last three parameters are simply passed through to the
	 *            {@link ApprovePromptBean#showPin(PopupHolder, int, String, WeeklyTimecard)}
	 *            method.
	 */
	public static void approvePrompt(List<Integer> contactIdList, String message,
			PopupHolder holder, int act, String prefix) {
		int n = contactIdList.size();
		boolean multiple = false;
		Integer lastId = null;
		boolean first = true;
		for (Integer id : contactIdList) {
			if (first) {
				first = false;
				lastId = id;
			}
			else if (id == null) { // this entry is for an object getting final approval
				if (lastId != null) {
					multiple = true;
					break;
				}
			}
			else if (lastId == null || ! lastId.equals(id)) {
				multiple = true;
				break;
			}
		}
		ApprovePromptBean bean = ApprovePromptBean.getInstance();
		if (n <= 1) {
			bean.showPin(holder, act,
					(prefix == null ? null : prefix+"PinApproveOne."), null);
		}
		else {
			bean.showPin(holder, act,
					(prefix == null ? null : prefix+"PinApprove."), null);
			if (prefix != null) {
				bean.setMessage(MsgUtils.formatMessage(prefix+"PinApprove.Text", n));
			}
		}
		bean.setMultipleApprovers(multiple);
		if (! multiple) {
			bean.getApproverContactDL(); // make sure list is initialized
			bean.setApproverContactId(lastId);
		}
		if (message != null) {
			// pass caller's extra message through to prompt bean
			bean.setAlertMessage(message);
		}
		log.debug("n=" + n + ", lastId=" + lastId + ", multiple=" + multiple);
	}

	/** Returns the name of ApprovalPath followed by the Production.
	 * @param production
	 */
//	public static String getProductionPathName(Production production) {
//		if (production != null) {
//			return production.getId() + Constants.TC_PROD_PATH_SUFFIX;
//		}
//		return null;
//	}

	/** Returns the name of ApprovalPath followed by the Project.
	 * @param project
	 */
//	public static String getProjectPathName(Project project) {
//		if (project != null) {
//			return project.getId() + Constants.TC_PROJECT_PATH_SUFFIX;
//		}
//		return null;
//	}

	/** Returns the ApprovalPath followed by the Project.
	 * @param project
	 * @return ApprovalPath
	 */
//	public static ApprovalPath getProjectPath(Project project) {
//		ApprovalPath projPath = null;
//		if (project != null) {
//			projPath = ApprovalPathDAO.getInstance().findApprovalPathByNameProject(
//					ApproverUtils.getProjectPathName(project), project);
//		}
//		return projPath;
//	}

	/** Returns the ApprovalPath followed by the Production.
	 * @param prod
	 * @return ApprovalPath
	 */
//	public static ApprovalPath getProductionPath(Production prod) {
//		ApprovalPath prodPath = ApprovalPathDAO.getInstance().findApprovalPathByNameProduction(
//				ApproverUtils.getProductionPathName(prod), prod);
//		return prodPath;
//	}

}
