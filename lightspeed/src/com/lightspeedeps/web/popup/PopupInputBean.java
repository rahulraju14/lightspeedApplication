/**
 * File: PopupInputBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.View;

/**
 * A class to display a simple confirmation dialog box, and get back an OK or Cancel result, and
 * pass that to our "holder", with the addition of a single input field.
 * <p>
 * The page that fronts the bean which is using this class should include the
 * common/popupinput.xhtml fragment.
 */
@ManagedBean
@ViewScoped
public class PopupInputBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 6013795720837282468L;

	private static final Log log = LogFactory.getLog(PopupInputBean.class);

	/** Backing field for the user's input. */
	private String input;

	/** The input field used when the 'numeric' flag is true. */
	private BigDecimal decimalInput;

	/** A message to display in error situations. */
	private String errorMessage;

	/** True iff a non-blank input should be required. */
	private boolean inputRequired;

	/** True iff the user hit OK with a blank input field and inputRequired
	 * was true. */
	private boolean inputError;

	/** True iff the input field should be treated as a BigDecimal numeric value. */
	private boolean numeric;

	/** The maximum length allowed for the input field. */
	private int maxLength = 100;

	/**
	 * The default constructor, which should not be used in the application
	 * code. Callers should use the getInstance() method.
	 */
	public PopupInputBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupInputBean getInstance() {
		return (PopupInputBean)ServiceFinder.findBean("popupInputBean");
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
		inputRequired = true;
		inputError = false;
		input = "";
		maxLength = 100;
		View.addFocus("popInput");
		super.show(holder, act, titleId, msgId, buttonOkId, buttonCancelId);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	@Override
	public String actionCancel() {
		setInputRequired(false);
		return super.actionCancel();
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		if (inputRequired) {
			inputError = false;
			if (numeric) {
				if (getDecimalInput() == null) {
					inputError = true;
					errorMessage = MsgUtils.getMessage("Confirm.InputRequired");
					return null;
				}
			}
			else {
				if (getInput() == null || getInput().trim().length() == 0) {
					inputError = true;
					errorMessage = MsgUtils.getMessage("Confirm.InputRequired");
					return null;
				}
			}
		}
		setInputRequired(false);
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
		PopupInputBean conf = getInstance();
		if (conf.isVisible()) {
			conf.actionCancel();
			ret = true;
		}
		return ret;
	}

	/**
	 * Redisplay the popup with an error message. This method is useful to call
	 * from a holder's "confirmOk" method when an error is found in the input,
	 * and the program wishes to keep the pop-up displayed with an error
	 * message, to give the user an opportunity to correct the error.
	 *
	 * @param message The error message text to be displayed within the popup.
	 */
	public void displayError(String message) {
		setVisible(true);
		setInputRequired(true);
		setErrorMessage(message);
		setInputError(true);
	}

	/** See {@link #inputError}. */
	public boolean getInputError() {
		return inputError;
	}
	/** See {@link #inputError}. */
	public void setInputError(boolean error) {
		inputError = error;
	}

	/** See {@link #inputRequired}. */
	public boolean getInputRequired() {
		return inputRequired;
	}
	/** See {@link #inputRequired}. */
	public void setInputRequired(boolean required) {
		inputRequired = required;
	}

	/** See {@link #numeric}. */
	public boolean getNumeric() {
		return numeric;
	}
	/** See {@link #numeric}. */
	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

	/**See {@link #maxLength}. */
	public int getMaxLength() {
		return maxLength;
	}
	/**See {@link #maxLength}. */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	/** See {@link #input}. */
	public String getInput() {
		return input;
	}
	/** See {@link #input}. */
	public void setInput(String in) {
		input = in;
	}

	/** See {@link #decimalInput}. */
	public BigDecimal getDecimalInput() {
		return decimalInput;
	}
	/** See {@link #decimalInput}. */
	public void setDecimalInput(BigDecimal decimalInput) {
		this.decimalInput = decimalInput;
	}

	/** See {@link #errorMessage}. */
	public String getErrorMessage() {
		return errorMessage;
	}
	/** See {@link #errorMessage}. */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
