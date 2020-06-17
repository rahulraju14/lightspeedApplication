package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ApprovalPathDAO;
import com.lightspeedeps.dao.ApproverGroupDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.Approvable;
import com.lightspeedeps.model.ApprovalPath;
import com.lightspeedeps.model.ApproverGroup;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.popup.DocumentApprovePromptBean;
import com.lightspeedeps.web.popup.DocumentRejectPromptBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.user.ChangePinBean;
import com.lightspeedeps.web.view.View;

/** This class is the backing bean of the Onboarding/Review and Approval tab,
 * it is used to lazy load the contact document table and also handles the approve
 * and rejection of distributed documents.
 */
@ManagedBean
@ViewScoped
public class ApproverDocBase extends View implements Serializable{

	private static final long serialVersionUID = -6910810267378129134L;

	private static final Log log = LogFactory.getLog(ApproverDocBase.class);

	/** "Approve" action code for popup confirmation/prompt dialog. */
	private static final int ACT_APPROVE = 12;

	/** "Reject" action code for popup confirmation/prompt dialog. */
	private static final int ACT_REJECT = 13;

	private ContactDocument approveDoc;

	/** True if the "change PIN" dialog should be displayed. */
	private boolean showChangePin;

	private boolean isPool = false;

	/** True, if the Me check box is checked on the Review and Approval tab */
	private boolean meCheckBox;

	/**  List of final approver and pool contacts  to whom the document can be forwarded. */
	private List<SelectItem> poolApproverList = null;

	/** True if the checked contact document's path uses final approval */
	private boolean useFinalApprover = false;

	public ApproverDocBase(String prefix) {
		super(prefix);
	}

	/** */
	public boolean getSetUp() {
		return false;
	}

	/**
	 * Action method called by subclass to approve a document.
	 *
	 * @return null
	 */
	protected String actionApprove(ContactDocument cd) {
		try {
			approveDoc = cd;
//			if (checkBoxSelectedItems.size() > 0) {
				User user = SessionUtils.getCurrentUser();
				if (user.getPin() == null) { // check for user pin , if null ask them to create one
					showChangePin = true;
					ChangePinBean.getInstance().show(this);
					addFocus("pin");
					return null;
				}

				// make sure all the selected timecards are still in "ready for approval" state
				List<Integer> contactIdList = new ArrayList<>();
				String msg = validateApproveList(approveDoc, contactIdList);
				if (msg == null) { // error - no selected timecards ready for approval
					return null;	// facesMessage already queued; just return.
				}
				else if (msg.length() == 0) { // normal case
					msg = null;	// don't give any additional message in prompt
				}
				// else 'msg' contains some additional text; pass it through to prompt bean.
//				for (ContactDocument cd : checkBoxSelectedItems) {
					if (approveDoc.getApproverId() < 0) {
						Integer pathId = -(approveDoc.getApproverId());
						if (pathId != null) {
							ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathId);
							if (path != null) {
								useFinalApprover = path.getUseFinalApprover();
								isPool = path.getUsePool();
							}
						}
					}
//				}
				//ApproverUtils.approvePrompt(contactIdList, msg, this, ACT_APPROVE, getMessagePrefix());
				if (! checkLocked(cd, getvUser(), "Approve.")) {
					return null;
				}
				promptApprove(this, ACT_APPROVE, getMessagePrefix());
				addFocus("documentApprove");
//			}
//			else {
//				MsgUtils.addFacesMessage("Document.NoneSelected", FacesMessage.SEVERITY_ERROR);
//				return null;
//			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Verify that the given contactDocument is
	 * ready for approval by the current user.
	 *
	 * @param cd The ContactDocument about to be approved.
	 * @param contactIdList If non-null, then an empty list of Integers. On
	 *            return, the list will contain the contact.id values of all the
	 *            next approvers of the given ContactDocument.
	 * @return - null if the given document is NOT ready for approval; or <br/>
	 *         - a zero-length String if everything is ok; or <br/>
	 *         - non-blank String if a final-approved document was found,
	 *         in which case the String contains the message to be added to the
	 *         approval prompt dialog box.
	 */
	protected String validateApproveList(ContactDocument cd, List<Integer> contactIdList) {
		Approvable approvable = null;
		String msg = ""; // normal response
		int readyTc = 0;
		int finalTc = 0;
		Integer currentId = getvContact().getId();
//		for (ContactDocument cd : contactDocumentList) {
//			if (cd.getChecked()) {
				approvable = cd;
				approvable = ContactDocumentDAO.getInstance().refresh(approvable);
				if (approvable.getApproverId() == null) {
					if (approvable.getStatus() != ApprovalStatus.SUBMITTED_NO_APPROVERS) {
						finalTc++;
					}
				}
				else if (! ApproverUtils.isNextApprover(approvable, currentId)) {
					// oops! Current user is not the next approver!
					ApproverUtils.calculateApprovalViewStatus(cd, SessionUtils.getCurrentContact());
					/*
					 * At this point, we can't tell the difference between (a) a document that was
					 * in the current user's queue and which he marked for approval and then was
					 * taken from him before he clicked Approve, and (b) a document that has been
					 * marked for approval by some other user and just happens to be in the current
					 * user's displayed list.  So we assume (for now) that it's case (b), and just
					 * ignore it.
					 */
//					continue;
				}
				else {
					readyTc++; // ERROR IN findNextApproverContactId METHOD
					if (contactIdList != null) {
						if (cd.getApproverId() < 0) {
							//ApproverGroup Changes
							Integer pathOrGroupId = -(cd.getApproverId());
							if (pathOrGroupId != null) {
								Set<Contact> contactPool = new HashSet<>();
								if (cd.getIsDeptPool()) { // Department pool, Group Id
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
								for (Contact ct : contactPool) {
									contactIdList.add(ct.getId());
								}
							}
						}
						//TODO can we block this code?, to avoid LI Exception
						/*  In findNextApproverContactId method, we are passing ApprovalPathId null in findNextApprover().
						 *  So, should I block this? or write code to find approvalPathId in this method? */
						/*else {
							Integer id = TimecardUtils.findNextApproverContactId(cd);
							contactIdList.add(id);
						}*/
					}
				}
//			}
//		}
		if (readyTc == 0) {
			MsgUtils.addFacesMessage("Approval.Doc.NoneReady", FacesMessage.SEVERITY_ERROR);
			msg = null; // error response
		}
		else if (finalTc > 0) {
			// ok to proceed, but warn user some timecards will be ignored
			msg = MsgUtils.formatMessage("Approval.Doc.Approve.SomeFinal", finalTc);
		}
		return msg;
	}

	/** Action method invoked when the user click Reject button on approvals tab.
	 * @return null
	 */
	protected String actionReject(ContactDocument cd) {
		try {
			approveDoc = cd;
			//Code to reject single document
			approveDoc = ContactDocumentDAO.getInstance().refresh(approveDoc);
			if (approveDoc.getApproverId() == null) {
				// Can't reject a final-approved timecard
				//tce.setWeeklyTc(wtc);
				ApproverUtils.calculateApprovalViewStatus(approveDoc, SessionUtils.getCurrentContact());
				MsgUtils.addFacesMessage("Approval.CantRejectApproved", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			else if (! ApproverUtils.isNextApprover(approveDoc, getvContact().getId())) {
				// oops! Current user is no longer the next approver!
				//tce.setWeeklyTc(wtc);
				ApproverUtils.calculateApprovalViewStatus(approveDoc, SessionUtils.getCurrentContact());
				MsgUtils.addFacesMessage("Approval.RemovedFromQueue", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (! checkLocked(cd, getvUser(), "Reject.")) {
				return null;
			}
			promptReject(approveDoc, this, getMessagePrefix());
			return null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Display the "Reject Timecard" dialog box.  Used from the Timecard
	 * pages and the Approval Dashboard page.
	 *
	 * @param cd The WeeklyTimecard that is being rejected.
	 * @param holder The holding instance for the popup.
	 * @param prefix The prefix for message id's for messages generated in the
	 *            dialog box.
	 */
	protected static void promptReject(ContactDocument cd, PopupHolder holder, String prefix) {
		DocumentRejectPromptBean bean = DocumentRejectPromptBean.getInstance();
		bean.show(holder, ACT_REJECT, prefix+"PinRejectOne.", cd);
		String name = cd.getContact().getUser().getFirstName() + " " + cd.getContact().getUser().getLastName();
		bean.setMessage(MsgUtils.formatMessage(prefix+"PinRejectOne.Text", name));
		bean.setCommentMsg(MsgUtils.getMessage(prefix+"PinRejectOne.CommentMsg"));
		addFocus("reject");
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
		case ChangePinBean.ACT_PROMPT_PIN:
			showChangePin = false;
			break;
		case ACT_APPROVE:
			actionApproveOk();
			break;
		case ACT_REJECT:
			actionRejectOk();
			break;
		default:
			res = super.confirmOk(action);
			break;
		}
		return res;
	}

	/**
	 * Action method called when the user has "OK"d the approval pop-up.
	 * @return null navigation string
	 */
	protected String actionApproveOk() {
		try {
			approveList();
			//setPriorUnapprovedTcs(null); // force refresh of unapproved timecard count
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	protected String actionRejectOk() {
		try {
			Contact apprContact = SessionUtils.getCurrentContact();
			DocumentRejectPromptBean bean = DocumentRejectPromptBean.getInstance();
			Integer approverId = bean.getSelectedApproverId();
			log.debug("approverId = " + approverId);
			Integer sendToId = bean.getSelectedContactId();
//			for (ContactDocument cd : contactDocumentList) {
//				if (approveDoc.getChecked()) {
					ContactDocument wtc = approveDoc;
					wtc = ContactDocumentDAO.getInstance().reject(wtc, getvContact(), sendToId, approverId, bean.getComment());
					ApproverUtils.calculateApprovalViewStatus(approveDoc, apprContact);
//				}
//			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Approve all the "checked" entries in the current timecardEntryList.
	 */
	protected void approveList() {
		Contact apprContact = SessionUtils.getCurrentContact();
		DocumentApprovePromptBean bean = DocumentApprovePromptBean.getInstance();
		boolean multiple = bean.getMultipleApprovers();

		if (! multiple) {
			Integer nextAppContactId = bean.getApproverContactId();
//			for (ContactDocument cd : checkBoxSelectedItems) {
//				if (approveDoc.getChecked()) {
					// Skip timecards not in this user's approval queue
					if(approveDoc.getApproverId() < 0){
						setIsPool(true);
					}
					else {
						setIsPool(false);
					}
					if (! approveEntry(approveDoc, apprContact, nextAppContactId)) {
						MsgUtils.addFacesMessage("Approval.Doc.OneNotReady", FacesMessage.SEVERITY_ERROR);
					}
//				}
//			}
		}
		else {
//			for (ContactDocument cd : checkBoxSelectedItems) {
				if (approveDoc.getStatus().isReady()) {
					// Skip timecards not in this user's approval queue
					if (! approveEntry(approveDoc, apprContact, TimecardUtils.findNextApproverContactId(approveDoc))) {
						MsgUtils.addFacesMessage("Approval.Doc.OneNotReady", FacesMessage.SEVERITY_ERROR);
					}
				}
//			}
		}
	}

	/**
	 * Do the approval process for one ContactDocument.
	 *
	 * @param cd The ContactDocument to be approved.
	 * @param apprContact The Contact of the current approver.
	 * @param nextAppContactId The Contact.id of the next approver; this may be
	 *            an out-of-line approver if the user has selected a next
	 *            approver other than the normal one in the chain.
	 * @return True iff the approval was successful. The approval can fail if
	 *         another user has changed the status of the timecard in the
	 *         meantime, e.g., by a "pull" or "recall".
	 */
	private boolean approveEntry(ContactDocument cd, Contact apprContact, Integer nextAppContactId) {
		ContactDocument contactDoc = cd;
		contactDoc = ContactDocumentDAO.getInstance().approve(contactDoc, nextAppContactId);
		if (contactDoc == null) { // approval did not complete!
			return false;
		}
		ApproverUtils.calculateApprovalViewStatus(cd, apprContact);
		return true;
	}

	/**
	 * Called when the user Cancels one of our pop-up dialogs.
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ChangePinBean.ACT_PROMPT_PIN:
				showChangePin = false;
				break;
		}
		return res;
	}

	/**
	 * Display the "Approve Document" dialog box.  Used from the Onboarding
	 * pages and the Approval Dashboard page.
	 *
	 * @param holder The holding instance for the popup.
	 * @param prefix The prefix for message id's for messages generated in the
	 *            dialog box.
	 */
	public static void promptApprove(PopupHolder holder, int act, String prefix) {
		DocumentApprovePromptBean bean = DocumentApprovePromptBean.getInstance();
		bean.show(holder, act, prefix+"PinApproveOne.");
		addFocus("documentApprove");
	}

	/**
	 * Attempt to lock the current contactDocument. If this fails, put up a prompt
	 * explaining the problem to the user.
	 *
	 * @param currUser The User to be given the lock.
	 * @return True if the user has acquired the lock, false if not.
	 */
	public boolean checkLocked(ContactDocument contactDocument, User currUser, String msgType) {
		boolean isLocked = ContactDocumentDAO.getInstance().lock(contactDocument, currUser);
		if (! isLocked) {
			PopupBean.getInstance().show(null, 0,
					"ContactFormBean.DocumentLocked.Title",
					"ContactFormBean.DocumentLocked." + msgType + "Text",
					"Confirm.OK", null); // no cancel button
			log.debug("submit/approve/etc prevented: locked by user #" + contactDocument.getLockedBy());
			editMode = false;
			return false;
		}
		return true;
	}

	/** See {@link #showChangePin}. */
	public boolean getShowChangePin() {
		return showChangePin;
	}
	/** See {@link #showChangePin}. */
	public void setShowChangePin(boolean showChangePin) {
		this.showChangePin = showChangePin;
	}

	/** See {@link #meCheckBox}. */
	public boolean getMeCheckBox() {
		return meCheckBox;
	}
	/** See {@link #meCheckBox}. */
	public void setMeCheckBox(boolean meCheckBox) {
		this.meCheckBox = meCheckBox;
	}

	/** See {@link #isPool}. */
	public boolean getIsPool() {
		return isPool;
	}
	/** See {@link #isPool}. */
	public void setIsPool(boolean isPool) {
		this.isPool = isPool;
	}

	/** See {@link #poolApproverList}. */
	public List<SelectItem> getPoolApproverList() {
		if (poolApproverList == null) {
			if (approveDoc != null) {
				Integer pathId = -(approveDoc.getApproverId());
				ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathId);
				poolApproverList = new ArrayList<>();
				poolApproverList.add(0, new SelectItem(0, "(Any approver)"));
				Contact finalApprover = path.getFinalApprover().getContact();
				poolApproverList.add(1, new SelectItem(finalApprover.getId(), finalApprover.getDisplayName()));
				for (Contact contact : path.getApproverPool()) {
					poolApproverList.add(new SelectItem(contact.getId(), contact.getDisplayName()));
				}
			}
		}
		return poolApproverList;
	}
	/** See {@link #poolApproverList}. */
	public void setPoolApproverList(List<SelectItem> poolApproverList) {
		this.poolApproverList = poolApproverList;
	}

	/** See {@link #useFinalApprover}. */
	public boolean getUseFinalApprover() {
		return useFinalApprover;
	}
	/** See {@link #useFinalApprover}. */
	public void setUseFinalApprover(boolean useFinalApprover) {
		this.useFinalApprover = useFinalApprover;
	}

}
