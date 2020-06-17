/**
 * File: SubmitPromptBean.java
 */
package com.lightspeedeps.web.timecard;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.TimecardSubmitType;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PinPromptBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class extends the PinPromptBean (which accepts and validates a password
 * and PIN) by adding the facility for displaying a list of choices as to why the
 * timecard is being submitted on behalf of another person.
 */
@ManagedBean
@ViewScoped
public class SubmitPromptBean extends PinPromptBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 3871815758684565920L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SubmitPromptBean.class);

	/** The reason/type of submit chosen by the user. */
	private TimecardSubmitType submitType;

	/** The user's comment, required for the "Other" submit choice. */
	private String comment;

	private boolean underContract;

	/** String literal used to hold the type of current object(timecard/document) for the pop up. */
	private String documentType = "timecard";

	public SubmitPromptBean() {
	}

	public static SubmitPromptBean getInstance() {
		return (SubmitPromptBean)ServiceFinder.findBean("submitPromptBean");
	}

	/**
	 * Show a dialog box that includes prompting for a PIN and password.
	 *
	 * @param holder Our caller, to be notified of OK/cancel events. May be null
	 *            if no callbacks are needed.
	 * @param act The 'action' code which will be returned to the 'holder' to
	 *            distinguish types of actions or dialog boxes presented.
	 * @param prefix The message prefix for the title, main message, and button
	 *            text.
	 * @param contract True if the person whose timecard is being submitted is
	 *            working "under contract" (instead of hourly or weekly).
	 */
	public void show(PopupHolder holder, int act, String prefix, boolean contract) {
		submitType = TimecardSubmitType.PAPER_SIGNATURE;
		underContract = contract;
		comment = "";
		super.promptPin(holder, act, prefix);
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		if (submitType == TimecardSubmitType.OTHER) {
			if (comment == null || comment.trim().length() < 3) {
//				setMessage(MsgUtils.getMessage("Timecard.Submit.CommentRequired"));
				MsgUtils.addFacesMessage("Timecard.Submit.CommentRequired", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		return super.actionOk();
	}

	/** See {@link #submitType}. */
	public TimecardSubmitType getSubmitType() {
		return submitType;
	}
	/** See {@link #submitType}. */
	public void setSubmitType(TimecardSubmitType submitType) {
		this.submitType = submitType;
	}

	/** See {@link #comment}. */
	public String getComment() {
		return comment;
	}
	/** See {@link #comment}. */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/** @return a non-null and non-empty List of SelectItem`s representing
	 * the types of "submit" action allowed for the current timecard.
	 * @see com.lightspeedeps.type.TimecardSubmitType#getTypeContractSelectList()
	 * @see com.lightspeedeps.type.TimecardSubmitType#getTypeDefaultSelectList() */
	public List<SelectItem> getSubmitTypeDL() {
		if (underContract) {
			return TimecardSubmitType.getTypeContractSelectList();
		}
		else if (getDocumentType().equals("document")) {
			return TimecardSubmitType.getTypeDocumentSelectList();
		}
		return TimecardSubmitType.getTypeDefaultSelectList();
	}

	/** See {@link #documentType}. */
	public String getDocumentType() {
		return documentType;
	}
	/** See {@link #documentType}. */
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

}
