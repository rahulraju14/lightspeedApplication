package com.lightspeedeps.web.popup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.Approver;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;

@ManagedBean
@ViewScoped
public class DocumentRejectPromptBean extends RejectPromptBean implements Serializable{

	private static final long serialVersionUID = -1226508625611967921L;

	/** The contact document which is being rejected. */
	private ContactDocument contactDocument;

	/*//** List of SelectItem`s, where the value field is an index into our
	 * matched lists of Contact and Approver.  The
	 * user has the option of picking one of these to be the next approver.
	 * The label is in the format 'lastName, firstName - role'. *//*
	private List<SelectItem> approverDL;

	private List<Contact> contacts;
	private List<Approver> approvers;*/

	public static DocumentRejectPromptBean getInstance() {
		return (DocumentRejectPromptBean)ServiceFinder.findBean("documentRejectPromptBean");
	}

	/**
	 * Show the Reject Prompt (or similar) dialog box, which does NOT include
	 * prompting for a PIN and password.
	 *
	 * @param holder Our caller, to be notified of OK/cancel events. May be null
	 *            if no callbacks are needed.
	 * @param act The 'action' code which will be returned to the 'holder' to
	 *            distinguish types of actions or dialog boxes presented.
	 * @param prefix The message prefix for the title, main message, and button
	 *            text.
	 * @param cd The weeklyTimecard this dialog relates to; if null, it
	 *            probably relates to multiple time cards, so any
	 *            timecard-specific information or options will be omitted.
	 */
	public void show(PopupHolder holder, int act, String prefix, ContactDocument cd) {
		approverDL = null;
		setContactDocument(cd);
		if (prefix == null) {
			super.show(holder, act, null, null, null, "Confirm.Cancel");
		}
		else {
			super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
		}
	}

	/**
	 * Create the list of approvers (and employee) which the user may select from
	 * to choose who to send the document back to.
	 *
	 * @param cd The ContactDocument that is in the process of being rejected.
	 */
	private void createApproverDL(ContactDocument cd) {
		Integer stopApproverId = null;
		if (cd != null) {
			stopApproverId = cd.getApproverId();
		}
		approvers = new ArrayList<Approver>();
		String stopAccountNum = SessionUtils.getCurrentUser().getAccountNumber();
		Integer approvalPathId = null;
		if (cd != null) {
			approvalPathId = ContactDocumentDAO.getCurrentApprovalPathId(cd);
		}
		contacts = TimecardUtils.createRejectionList(cd, stopApproverId, stopAccountNum, approvers, approvalPathId);
		String label;
		if (cd != null) {
			User ownerUser = cd.getContact().getUser();
			User user = null;
			if (contacts.size() > 0 ) {
				// The first approver may be the same as the employee, if that person
				// is the departmental approver.  In that case, remove the "approver"
				// entry so we'll only have the "employee" entry for that person.
				Integer lastContactId = contacts.get(0).getId();
				Contact c = ContactDAO.getInstance().findById(lastContactId);
				if (c != null && c.getUser().equals(ownerUser)) {
					contacts.remove(0); 	// match - remove approver
					approvers.remove(0);
					user = c.getUser();		// avoid the UserDAO database call below
				}
			}
			if (user == null) {
				user = ownerUser;
			}
			label = user.getLastNameFirstName() + " - employee";
		}
		else {
			label = "Employee";
		}
		approverDL = new ArrayList<SelectItem>();
		for (int ix = 0; ix < contacts.size(); ix++ ) {
			Contact ct = contacts.get(ix);
			String name = ct.getUser().getLastNameFirstName();
			if (ct.getRoleName() != null) {
				name += " - " + ct.getRoleName();
			}
			approverDL.add(new SelectItem(ix, name));
		}
		approverDL.add(0, new SelectItem(-1,label)); // '-1' value will be recognized as employee by caller
		approverIx = approverDL.size() - 1;
	}

	/** See {@link #contactDocument}. */
	public ContactDocument getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}. */
	public void setContactDocument(ContactDocument contactDocument) {
		this.contactDocument = contactDocument;
	}

	/** See {@link #approverDL}. */
	@Override
	public List<SelectItem> getApproverDL() {
		if (approverDL == null) {
			createApproverDL(getContactDocument());
		}
		return approverDL;
	}
	/** See {@link #approverDL}. */
	@Override
	public void setApproverDL(List<SelectItem> approverDL) {
		this.approverDL = approverDL;
	}

}
