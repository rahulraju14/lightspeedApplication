/**
 * File: CommentPinPromptBean.java
 */
package com.lightspeedeps.web.timecard;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PinPromptBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class extends the PinPromptBean (which accepts and validates a password
 * and PIN) by adding the facility for accepting a comment regarding the action
 * being taken.
 */
@ManagedBean
@ViewScoped
public class CommentPinPromptBean extends PinPromptBean implements Serializable {
	/** */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CommentPinPromptBean.class);

	/** The user's comment. */
	private String comment;

	/** String literal used to hold the type of current object(timecard/document) for the pop up. */
	private String documentType = "timecard";

	public CommentPinPromptBean() {
	}

	public static CommentPinPromptBean getInstance() {
		return (CommentPinPromptBean)ServiceFinder.findBean("commentPinPromptBean");
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
	 */
	@Override
	public void show(PopupHolder holder, int act, String prefix) {
		comment = "";
		super.promptPin(holder, act, prefix);
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		if (comment == null || comment.trim().length() < 3) {
			MsgUtils.addFacesMessage("Timecard.Submit.CommentRequired", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		return super.actionOk();
	}

	/** See {@link #comment}. */
	public String getComment() {
		return comment;
	}
	/** See {@link #comment}. */
	public void setComment(String comment) {
		this.comment = comment;
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