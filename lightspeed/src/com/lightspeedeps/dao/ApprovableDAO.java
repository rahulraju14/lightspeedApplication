package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Approvable;
import com.lightspeedeps.model.Approver;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.TimecardEvent;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ApprovalLevel;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Approvable entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Approvable
 */
public abstract class ApprovableDAO<T extends Approvable> extends BaseTypeDAO<T> {

	private static final Log log = LogFactory.getLog(ApprovableDAO.class);

	/**
	 * Mark the given ContactDocument approved, and set the Approver.id value of
	 * the next approver in the chain. This will be null if the ContactDocument
	 * has received its final approval. An approval event is added to the
	 * history of the ContactDocument. The updated document is saved.
	 *
	 * @param cd The ContactDocument to be updated.
	 * @param approverContactId If not null, the Contact.id value of the person
	 *            who should be the next approver for this ContactDocument. If
	 *            null, this method determines the next approver based on the
	 *            current approval chain for the given ContactDocument.
	 * @return A (possibly refreshed) reference to the ContactDocument object.
	 */
	@Transactional
	public Approvable approve(Approvable cd, Integer approverContactId, boolean usePath) {
		log.debug("");
		cd = refresh(cd);
		if (cd.getStatus() == ApprovalStatus.OPEN || cd.getStatus().isFinal() ||
				cd.getStatus() == ApprovalStatus.VOID || cd.getApproverId() == null) {
			EventUtils.logError("Attempt to approve a document that was not pending approval!");
			log.error(cd);
			return null;
		}
		User expectedAppUser = ApproverUtils.findApproverUser(cd);
		if (! SessionUtils.getCurrentUser().equals(expectedAppUser)) {
			EventUtils.logError("Attempt to approve a document waiting for a different approver!");
			log.error(cd);
			return null;
		}
		log.debug("approverContactId : " +  approverContactId);
		approveSub(cd, approverContactId, usePath);
		return cd;
	}

	/**
	 * Record an APPROVE event for the given document (ContactDocument or
	 * WeeklyTimecard), and set the Approver.id value of the next approver in
	 * the chain. This will be null if the document has received its final
	 * approval. An approval event is added to the history of the document. The
	 * status of the document is updated appropriately, to either Submitted,
	 * Resubmitted, or Approved, and the updated document is saved.
	 *
	 * @param cd The Approvable document to be updated.
	 * @param approverId If not null, the Contact.id value of the person who
	 *            should be the next approver for this document. If null, this
	 *            method determines the next approver based on the current
	 *            approval chain for the given document.
	 * @param usePath If true, the Approvable supports ApprovalPaths. Currently
	 *            that is only the case for ContactDocuments.
	 */
	protected void approveSub(Approvable cd, Integer approverId, boolean usePath) {
		byte outOfLine = 0;
		Contact contact = null; // Contact of new approver
		Integer approvalPathId = null;
		if (usePath) {
			//ApproverGroup Changes
			// Determine next approver
			if (cd.getApproverId() != null && cd.getApproverId() < 0 && (! cd.getIsDeptPool())) {
				approvalPathId = - (cd.getApproverId());
			}
			else {
				approvalPathId = findCurrentApprovalPathId(cd);
			}
		}
		log.debug("approverId : " +  approverId);
		Approver nextApprover = TimecardUtils.findNextApprover(cd, approvalPathId);
		if (approverId == null) { // use normal chain
			if (nextApprover != null) { // not at end of chain
				if (usePath && nextApprover.getId() < 0) { // negative id means dummy approver and throw that in the pool
					log.debug(" nextApprover: " + nextApprover.getId());
					approverId = nextApprover.getId();
					if (approvalPathId != null && ! (approverId.equals((-1)*approvalPathId))) {
						//Department Pool
						log.debug("");
						cd.setIsDeptPool(true);
					}
				}
				else {
					approverId = nextApprover.getId(); // this is Approver.id of next one
					contact = nextApprover.getContact();
				}
			}
		}
		else if (nextApprover != null && nextApprover.getContact() != null &&
				nextApprover.getContact().getId().equals(approverId)) {
			approverId = nextApprover.getId(); // use next one in chain
			contact = nextApprover.getContact();
		}
		// Code for "DROP-DOWN" For pool case (if use final approver is true), Currently we are not using that drop down.
		/*else if (usePath && nextApprover != null && nextApprover.getId() < 0 && approverId != null) {
			ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
			if (approverId.equals(path.getFinalApprover().getContact().getId())) {
				approverId = path.getFinalApprover().getId();
				log.debug("final approver :  " +  approverId);
			}
			else {
				if (approverId == 0) {
					log.debug(" any approver : " +  approverId);
					approverId = nextApprover.getId();
				}
				else {
					//If selects any other approver from "Forward to" drop down.
					contact = ContactDAO.getInstance().findById(approverId);
					Approver app = new Approver(contact, path.getFinalApprover(), false);
					save(app);
					approverId = app.getId();
					log.debug(" new approver : "+app.getId());
				}
			}
		}*/
		else { // out-of-line approval: create new Approver instance
			outOfLine = TimecardEvent.DAY_OUT_OF_LINE; // remember for event
			contact = ContactDAO.getInstance().findById(approverId);
			Approver app = new Approver(contact, nextApprover, false);
			save(app);
			approverId = app.getId();
		}

		// NOW approverId is an Approver.id value!
		if (approverId == null) { // no more approvers in the chain ... has final approval!
			cd.setStatus(ApprovalStatus.APPROVED);
		}
		else if (cd.getStatus() == ApprovalStatus.REJECTED ||
				cd.getStatus() == ApprovalStatus.RECALLED) {
			cd.setStatus(ApprovalStatus.RESUBMITTED);
		}
		Integer priorApproverId = cd.getApproverId();
		cd.setApproverId(approverId);
		ApprovalLevel approvalLevel = ApproverUtils.isProductionOrDeptApprover(approverId, approvalPathId);
		if (approvalLevel != null) {
			log.debug("<<<<<<<<< LEVEL >>>>>>>>>>>>" + approvalLevel);
			((ContactDocument)cd).setApprovalLevel(approvalLevel);
		}
		log.debug("<>");
		// Set the dept pool false if previously it was true.
		if (cd.getApproverId() != null && priorApproverId != null &&
				priorApproverId < 0 && approvalPathId != null &&  cd.getIsDeptPool() &&
				(! priorApproverId.equals((-1)*approvalPathId))) {
			log.debug("");
			cd.setIsDeptPool(false);
		}

		// Create event
		createEvent(cd, contact, priorApproverId, outOfLine);

		if (cd.getStatus() == ApprovalStatus.APPROVED_PAST) {
			EventUtils.logError(new IllegalArgumentException("invalid status for persisted Approvable = " +
					cd.getStatus().name()));
			cd.setStatus(ApprovalStatus.SUBMITTED);
		}
		else if (cd.getStatus() == ApprovalStatus.REJECTED_PAST) {
			EventUtils.logError(new IllegalArgumentException(
					"invalid status for persisted ContactDocument = " + cd.getStatus().name()));
			cd.setStatus(ApprovalStatus.RESUBMITTED);
		}
		attachDirty(cd);
/*		// Moved to ContactDocumentDAO
		// Update employment after startform is approved
		if (cd.getStatus() == ApprovalStatus.APPROVED && cd instanceof ContactDocument) {
			ContactDocument cdoc = ((ContactDocument) cd);
			if (cdoc.getFormType() == PayrollFormType.START) {
				Employment emp = cdoc.getEmployment();
				if (emp != null && cdoc.getRelatedFormId() != null) {
					StartForm sf = StartFormDAO.getInstance().findById(cdoc.getRelatedFormId());
					emp.setOffProduction(sf.getOffProduction());
					if (emp.getAccount() != null) {
						emp.getAccount().copyFrom(sf.getAccount());
					}
					emp.setEndDate(sf.getWorkEndDate());
					EmploymentDAO.getInstance().attachDirty(emp);
				}
			}
		}*/

		// If the prior Approver was "out of line" (un-shared), then delete it.
		if (priorApproverId != null) {
			Approver app = ApproverDAO.getInstance().findById(priorApproverId);
			if (app != null && ! app.getShared()) {
				log.debug("deleting non-shared Approver, id=" + app.getId());
				delete(app);
			}
		}

		if (approverId != null && approverId > 0) {
			notifyReady(cd);
		}
		updateBatchStatus(cd);
		log.info("document approved, id=" + cd.getId() + ", next appr=" + approverId);
	}

	/**
	 * @param cd
	 * @param approvalPathId
	 * @return
	 */
	protected Integer findCurrentApprovalPathId(Approvable cd) {
		// subclass will override for ApprovalPath case (i.e., ContactDocumentDAO).
		return null;
	}

	protected abstract void createEvent(Approvable doc, Contact contact, Integer priorApproverId,
			byte outOfLine);

	protected abstract void notifyReady(Approvable doc);

	protected void updateBatchStatus(Approvable doc) {
		// subclass will override for timecard case (i.e., WeeklyTimecardDAO).
	}

	/**
	 * Lock the specified WeeklyTimecard (for editing) so no other User
	 * can edit it, and return True if successful.
	 *
	 * @param approvable The Approvable (WeeklyTimecard or ContactDocument) to be locked.
	 * @param user The User who will "hold" the lock and is allowed to edit the WeeklyTimecard.
	 * @return True if the User has successfully been given the lock, which
	 *         means that no other User currently had the lock and we
	 *         successfully updated the database with the lock information, or
	 *         the same User already had the lock.
	 */
	@Transactional
	public boolean lock(Approvable approvable, User user) {
		boolean ret = false;
		try {
			if (approvable != null) {
				if (approvable.getLockedBy() == null) {
					approvable.setLockedBy(user.getId());
					attachDirty(approvable);
					log.debug(approvable.getId());
					ret = true;
				}
				else if (approvable.getLockedBy().equals(user.getId())) {
					ret = true;
				}
				log.debug("locked #" + approvable.getId() + "=" + ret + ", by=" + user.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
			if (approvable != null) {
				// Most likely attach failed; don't indicate object was locked
				approvable.setLockedBy(null);
			}
		}
		return ret;
	}

	/**
	 * Unlock a WeeklyTimecard so any User (with appropriate permission) can now
	 * edit it. No error occurs if the WeeklyTimecard was already unlocked.
	 *
	 * @param approvable The Approvable (WeeklyTimecard or ContactDocument) to be unlocked.
	 * @param userId The database id of the User that is trying to unlock the
	 *            timecard. If the timecard is locked by a different user, the
	 *            unlock request is ignored.
	 * @return True if the timecard was updated in the database; false indicates
	 *         the timecard was not updated, because either (a) it was not
	 *         locked, or (b) it was locked by someone other than the given
	 *         userId.
	 */
	@Transactional
	public boolean unlock(Approvable approvable, Integer userId) {
		boolean bRet = false;
		try {
			log.debug("unlocking #" + approvable.getId() + "; locked by=" + approvable.getLockedBy());
			if (approvable.getLockedBy() != null) {
				if (userId == null || userId.equals(approvable.getLockedBy())) {
					approvable.setLockedBy(null);
					if (approvable.getStatus() == ApprovalStatus.APPROVED_PAST) {
						EventUtils.logError(new IllegalArgumentException("invalid status for persisted Approvable = " +
									approvable.getStatus().name()));
						approvable.setStatus(ApprovalStatus.SUBMITTED);
					}
					try {
						attachDirty(approvable);
					}
					catch (Exception e) {
						log.error(e.getMessage(),e);
					}
					log.debug(approvable);
					bRet = true;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * Search an approver chain for the given approverId.
	 *
	 * @param app An Approver in the chain (typically the first).
	 * @param approverId The Approver.id value to search for.
	 * @return The matching Approver object if found; null if the end of the
	 *         chain is reached without a match.
	 */
	protected Approver searchChain(Approver app, Integer approverId) {
		while (app != null) {
			if (app.getId().equals(approverId)) {
				break;
			}
			app = app.getNextApprover();
		}
		return app;
	}

	/**
	 * Search an approver chain for the given Contact id.
	 *
	 * @param app An Approver in the chain (typically the first).
	 * @param contactId The Contact.id value to search for.
	 * @return The matching Approver object if found; null if the end of the
	 *         chain is reached without a match.
	 */
	protected Approver searchChainContact(Approver app, Integer contactId) {
		while (app != null) {
			if (app.getContact() != null && app.getContact().getId().equals(contactId)) {
				break;
			}
			app = app.getNextApprover();
		}
		return app;
	}

}
