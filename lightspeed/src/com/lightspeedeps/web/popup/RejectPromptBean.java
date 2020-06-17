/**
 * File: RejectPromptBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Approver;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * This class extends the {@link PinPromptBean} (which accepts and validates a password
 * and PIN) by adding the facility for displaying a list of candidate Approvers
 * or rejection targets, and allowing the user to select one.  It also maintains
 * a reference to the timecard being approved (if only one) or rejected.
 */
@ManagedBean
@ViewScoped
public class RejectPromptBean extends PinPromptBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3910478611392588313L;

	private static final Log log = LogFactory.getLog(RejectPromptBean.class);

	/** List of SelectItem`s, where the value field is an index into our
	 * matched lists of Contact and Approver.  The
	 * user has the option of picking one of these to be the next approver.
	 * The label is in the format 'lastName, firstName - role'. */
	protected List<SelectItem> approverDL;

	/** The contact.id value of the selected approver, set when the user clicks
	 * the "Reject" (or equivalent) button. */
	private Integer contactId;

	/** Index (0 origin) of the user-selected list entry from the approverDL. */
	protected Integer approverIx;

	protected List<Contact> contacts;
	protected List<Approver> approvers;

	/** User's comment input field.  This will be added to the timecard's
	 * comments, if non-blank. */
	private String comment;

	/** The timecard which is being rejected. */
	private WeeklyTimecard weeklyTimecard;

	/** The Message/text describing the Comment text field on reject Popup. */
	private String commentMsg;

	public RejectPromptBean() {
	}

	public static RejectPromptBean getInstance() {
		return (RejectPromptBean)ServiceFinder.findBean("rejectPromptBean");
	}

	/**
	 * Show a dialog box that includes prompting for a PIN and password.  This method
	 * is meant to be used by our subclasses.
	 *
	 * @param holder Our caller, to be notified of OK/cancel events. May be null
	 *            if no callbacks are needed.
	 * @param act The 'action' code which will be returned to the 'holder' to
	 *            distinguish types of actions or dialog boxes presented.
	 * @param prefix The message prefix for the title, main message, and button
	 *            text.
	 * @param wtc The weeklyTimecard this dialog relates to; if null, it
	 *            probably relates to multiple time cards, so any
	 *            timecard-specific information or options will be omitted.
	 */
	protected void showPin(PopupHolder holder, int act, String prefix,
			WeeklyTimecard wtc) {
		approverDL = null;
		comment = "";
		setWeeklyTimecard(wtc);
		super.promptPin(holder, act, prefix);
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
	 * @param wtc The weeklyTimecard this dialog relates to; if null, it
	 *            probably relates to multiple time cards, so any
	 *            timecard-specific information or options will be omitted.
	 */
	public void show(PopupHolder holder, int act, String prefix,
			WeeklyTimecard wtc) {
		approverDL = null;
		comment = "";
		setWeeklyTimecard(wtc);
		if (prefix == null) {
			super.show(holder, act, null, null, null, "Confirm.Cancel");
		}
		else {
			super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
		}
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		log.debug("OK: next contact #" + getSelectedContactId());
		return super.actionOk();
	}

	/**
	 * Create the list of approvers (and employee) which the user may select from
	 * to choose who to send the timecard back to.
	 *
	 * @param wtc The timecard that is in the process of being rejected.
	 */
	private void createApproverDL(WeeklyTimecard wtc) {
		Integer stopApproverId = null;
		approverDL = new ArrayList<>();
		if (wtc != null) {
			stopApproverId = wtc.getApproverId();
		}
		else {
			// There is no associated timecard so just return.
			// This can happen when trying to go the full timecard view for the first time.
			return;
		}
		approvers = new ArrayList<>();
		String stopAccountNum = SessionUtils.getCurrentUser().getAccountNumber();
		contacts = TimecardUtils.createRejectionList(wtc, stopApproverId, stopAccountNum, approvers, null);
		String label;
		if (getWeeklyTimecard() != null) {
			String currentUserAcc = getWeeklyTimecard().getUserAccount();
			User user = null;
			if (contacts.size() > 0 ) {
				// The first approver may be the same as the employee, if that person
				// is the departmental approver.  In that case, remove the "approver"
				// entry so we'll only have the "employee" entry for that person.
				Integer lastContactId = contacts.get(0).getId();
				Contact c = ContactDAO.getInstance().findById(lastContactId);
				if (c != null && c.getUser().getAccountNumber().equals(currentUserAcc)) {
					contacts.remove(0); 	// match - remove approver
					approvers.remove(0);
					user = c.getUser();		// avoid the UserDAO database call below
				}
			}
			if (user == null) {
				user = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, currentUserAcc);
			}
			label = user.getLastNameFirstName() + " - employee";
		}
		else {
			label = "Employee";
		}
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

	/**
	 * @return the Contact.id value of the Contact selected by the user to
	 *         receive the rejected timecard. Returns null if the timecard
	 *         should be returned to the employee.
	 */
	public Integer getSelectedContactId() {
		Integer contId = null;
		if (approverIx != null) {
			if (approverIx < 0) { // probably -1, for employee
				// return null
			}
			else {
				if (approverIx < contacts.size()) {
					contId = contacts.get(approverIx).getId();
				}
			}
		}
		return contId;
	}

	/**
	 * @return the Approver.id value of the Approver object selected by the user,
	 *         which references the Contact who is to receive the rejected
	 *         timecard. Returns null if the timecard should be returned to the
	 *         employee. May also return null if the person selected was an
	 *         out-of-line approver who is no longer in the chain; in this case,
	 *         the Approver object no longer exists.
	 */
	public Integer getSelectedApproverId() {
		Integer approverId = null;
		if (approverIx != null) {
			if (approverIx >= 0 && approverIx < approvers.size()) {
				if (approvers.get(approverIx) != null) {
					// entry may be null for out-of-line approvers of WeeklyTimecards &
					//for pool approvers Of ContactDocuments (Contact id will be used)
					approverId = approvers.get(approverIx).getId();
				}
			}
		}
		return approverId;
	}

	/**See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/**See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #approverIx}. */
	public Integer getApproverIx() {
		return approverIx;
	}
	/**See {@link #approverIx}. */
	public void setApproverIx(Integer listIx) {
		approverIx = listIx;
	}

	/** See {@link #approverDL}. */
	public List<SelectItem> getApproverDL() {
		if (approverDL == null || approverDL.size() == 0) {
			createApproverDL(getWeeklyTimecard());
		}
		return approverDL;
	}
	/** See {@link #approverDL}. */
	public void setApproverDL(List<SelectItem> approverDL) {
		this.approverDL = approverDL;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	/** See {@link #comment}. */
	public String getComment() {
		return comment;
	}
	/** See {@link #comment}. */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/** See {@link #commentMsg}. */
	public String getCommentMsg() {
		return commentMsg;
	}
	/** See {@link #commentMsg}. */
	public void setCommentMsg(String commentMsg) {
		this.commentMsg = commentMsg;
	}

}
