/**
 * File: PopupCheckboxBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A class to display a simple confirmation dialog box, and get back an OK or Cancel result, and
 * pass that to our "holder", with the addition of a single checkbox field.
 * <p>
 * The page that fronts the bean which is using this class should include the
 * common/popupcheckbox.xhtml fragment.
 */
@ManagedBean
@ViewScoped
public class PopupCheckboxBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3684182298116569932L;

	private static final Log log = LogFactory.getLog(PopupCheckboxBean.class);

	/** Backing field for the user's check-box input. */
	private boolean check;

	/** Message to display next to check box. */
	private String checkMessage;

	/** Message to display below check box, typically "click ... to confirm". */
	private String confirmMessage;

	/**
	 * The default constructor, which should not be used in the application
	 * code. Callers should use the getInstance() method.
	 */
	public PopupCheckboxBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupCheckboxBean getInstance() {
		return (PopupCheckboxBean)ServiceFinder.findBean("popupCheckboxBean");
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * prefix is used to generate message-id Strings, which will be looked up in
	 * the message resource file.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix The prefix used to generate message ids for the title,
	 *            prompt messages, and Ok button for the dialog box.
	 */
	public void prompt(PopupHolder holder, int act, String prefix, boolean showCancel, String cancelId) {
		checkMessage = MsgUtils.getMessage(prefix + "Check");
		confirmMessage = MsgUtils.getMessage(prefix + "Confirm");
		if (cancelId == null) {
			cancelId = "Confirm.Cancel";
		}
		if (! showCancel) {
			cancelId = null;
		}
		show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", cancelId);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	@Override
	public String actionCancel() {
		return super.actionCancel();
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		return super.actionOk();
	}

	/**
	 * Method used to handle an ESCape key function.  If the confirmation dialog
	 * is visible, call our normal Cancel function and return true; otherwise
	 * return false.
	 */
	public static boolean actionEscape() {
		log.debug("");
		boolean ret = false;
		PopupCheckboxBean conf = getInstance();
		if (conf.isVisible()) {
			conf.actionCancel();
			ret = true;
		}
		return ret;
	}

	/** See {@link #check}. */
	public boolean getCheck() {
		return check;
	}
	/** See {@link #check}. */
	public void setCheck(boolean pin) {
		this.check = pin;
	}

	/** See {@link #checkMessage}. */
	public String getCheckMessage() {
		return checkMessage;
	}
	/** See {@link #checkMessage}. */
	public void setCheckMessage(String checkMessage) {
		this.checkMessage = checkMessage;
	}

	/** See {@link #confirmMessage}. */
	public String getConfirmMessage() {
		return confirmMessage;
	}
	/** See {@link #confirmMessage}. */
	public void setConfirmMessage(String confirmMessage) {
		this.confirmMessage = confirmMessage;
	}

}
