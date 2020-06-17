package com.lightspeedeps.web.popup;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.approver.ApprovePromptBean;

/** Managed bean for the approve dialog box used for document approvals
 * @author
 */
@ManagedBean
@ViewScoped
public class DocumentApprovePromptBean extends ApprovePromptBean implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -8825975129434779150L;

	public static DocumentApprovePromptBean getInstance() {
		return (DocumentApprovePromptBean)ServiceFinder.findBean("documentApprovePromptBean");
	}

	/**
	 * Show the Approve Prompt (or similar) dialog box, which does NOT include
	 * prompting for a PIN and password.
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
		approverDL = null;
		setPassword("");
		setPin("");
		if (prefix == null) {
			super.show(holder, act, null, null, null, "Confirm.Cancel");
		}
		else {
			super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
		}
		setAskPin(true);
	}

}
