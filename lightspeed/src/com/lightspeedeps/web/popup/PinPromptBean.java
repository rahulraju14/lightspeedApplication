/**
 * File: PinPromptBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.view.View;

/**
 * This class extends our basic {@link PopupBean} by adding the facility to capture
 * and validate password and PIN values.
 */
@ManagedBean
@ViewScoped
public class PinPromptBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 8161995123496127216L;

	private static final Log log = LogFactory.getLog(PinPromptBean.class);

	/** Input field for the user's password. */
	private String password;

	/** Input field for the user's PIN. */
	private String pin;

	/** True iff we must verify the user's PIN as part of the prompt. */
	private boolean askPin;

	/** True if only prompting user for pin and no password */
	private boolean pinOnly;

	/** True iff the user entered an invalid or blank PIN. */
	private boolean pinError;

	/** The error message to be displayed in the dialog box. */
	private String errorMsg;
	
	public PinPromptBean() {
	}

	public static PinPromptBean getInstance() {
		return (PinPromptBean)ServiceFinder.findBean("pinPromptBean");
	}

	public void promptPin(PopupHolder holder, int act, String prefix) {
		askPin = true;
		pinOnly = false;
		pinError = false;
		password = "";
		pin = "";
		if (prefix != null) {
			setMessage(MsgUtils.getMessage(prefix + "Text"));
			show(holder, act, prefix + "Title", prefix + "Ok", "Confirm.Cancel");
		}
		else {
			show(holder, act, null, null, "Confirm.Cancel");
		}
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * Strings are all message-ids, which will be looked up in the message
	 * resource file.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box.
	 * @param msgId The message-id of the message to be shown in the dialog.
	 * @param buttonOkId The message-id of the text for the "OK" button.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button.
	 */
	@Override
	public void show(PopupHolder holder, int act, String titleId, String msgId,
			String buttonOkId, String buttonCancelId) {
		log.debug("action=" + act);
		askPin = false;
		pinError = false;
		super.show(holder, act, titleId, msgId, buttonOkId, buttonCancelId);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	@Override
	public String actionCancel() {
		setAskPin(false);
		return super.actionCancel();
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
//		log.debug("OK, action=" + action);
		pinError = false;
		if (askPin) {
			User user = SessionUtils.getCurrentUser();
			if (pin != null) {
				pin = pin.trim();
			}
			if (pin == null || pin.length() == 0) {
				errorMsg = MsgUtils.getMessage("Timecard.Submit.PinBlank");
				log.info("missing PIN");
				pinError = true;
			}
			else if (pin.length() < Constants.MIN_PIN_LENGTH) {
				errorMsg = MsgUtils.getMessage("Timecard.Submit.PinShort");
				log.info("PIN too short");
				pinError = true;
			}
			else if (pin.length() > Constants.MIN_PIN_LENGTH) {
				errorMsg = MsgUtils.getMessage("Timecard.Submit.PinLong");
				log.info("PIN too long");
				pinError = true;
			}
			else if (getPassword() == null || getPassword().trim().length() == 0) {
				errorMsg = MsgUtils.getMessage("Timecard.Submit.PasswordBlank");
				log.info("missing password");
				pinError = true;
			}
			else if (! pin.equals(user.getPin()) ||
					! getPassword().trim().equals(user.getPassword())) {
				if (getPinOnly()) {
					errorMsg = MsgUtils.getMessage("Timecard.Submit.PinOnlyError");
				}
				else {
					errorMsg = MsgUtils.getMessage("Timecard.Submit.PinError.Desktop");
				}
				log.info("pw/PIN don't match stored values");
				pinError = true;
			}
			if (pinError) {
				View.addFocus("approve");
				return null;
			}
		}
		setAskPin(false);
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
		PinPromptBean conf = getInstance();
		if (conf.isVisible() || conf.getAskPin()) {
			conf.actionCancel();
			ret = true;
		}
		return ret;
	}

	/** See {@link #askPin}. */
	public boolean getAskPin() {
		return askPin;
	}
	/** See {@link #askPin}. */
	public void setAskPin(boolean askPin) {
		this.askPin = askPin;
	}

	/** See {@link #pinError}. */
	public boolean getPinError() {
		return pinError;
	}
	/** See {@link #pinError}. */
	public void setPinError(boolean pinError) {
		this.pinError = pinError;
	}

	/** See {@link #errorMsg}. */
	public String getErrorMsg() {
		return errorMsg;
	}
	/** See {@link #errorMsg}. */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/** See {@link #password}. */
	public String getPassword() {
		return password;
	}
	/** See {@link #password}. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** See {@link #pin}. */
	public String getPin() {
		return pin;
	}
	/** See {@link #pin}. */
	public void setPin(String pin) {
		this.pin = pin;
	}

	/** See {@link #pinOnly}. */
	public boolean getPinOnly() {
		return pinOnly;
	}
	/** See {@link #pinOnly}. */
	public void setPinOnly(boolean pinOnly) {
		this.pinOnly = pinOnly;
	}

}
