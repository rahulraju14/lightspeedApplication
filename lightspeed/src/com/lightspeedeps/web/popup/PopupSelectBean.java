/**
 * File: PopupInputBean.java
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

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.view.View;

/**
 * This extends the PopupBean class to include a drop-down list of selections.
 * <p>
 * The page that fronts the bean which is using this class should include the
 * common/popupselect.xhtml fragment.
 */
@ManagedBean
@ViewScoped
public class PopupSelectBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 6013795720837282468L;

	private static final Log log = LogFactory.getLog(PopupSelectBean.class);

	/** Backing field for the user's input. */
	private Object selectedObject;

	private List<SelectItem> selectList;

	/** A message to display in error situations. */
	private String errorMessage;

//	/** True iff a non-blank input should be required. */
//	private boolean inputRequired;
//
//	/** True iff the user hit OK with a blank input field and inputRequired
//	 * was true. */
//	private boolean inputError;

	/**
	 * The default constructor, which should not be used in the application
	 * code. Callers should use the getInstance() method.
	 */
	public PopupSelectBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupSelectBean getInstance() {
		return (PopupSelectBean)ServiceFinder.findBean("popupSelectBean");
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
	 *            prompt message, and Ok button for the dialog box.
	 */
	public void prompt(PopupHolder holder, int act, String prefix) {
		show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
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
//		inputRequired = true;
//		inputError = false;
		selectedObject = null;
		View.addFocus("popSelect");
		super.show(holder, act, titleId, msgId, buttonOkId, buttonCancelId);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	@Override
	public String actionCancel() {
//		setInputRequired(false);
		return super.actionCancel();
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
//		if (inputRequired) {
//			if (getInput() == null || getInput().trim().length() == 0) {
//				inputError = true;
//				errorMessage = MsgUtils.getMessage("Confirm.InputRequired");
//				return null;
//			}
//			inputError = false;
//		}
//		setInputRequired(false);
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
		PopupSelectBean conf = getInstance();
		if (conf.isVisible()) {
			conf.actionCancel();
			ret = true;
		}
		return ret;
	}

//	/** See {@link #inputError}. */
//	public boolean getInputError() {
//		return inputError;
//	}
//	/** See {@link #inputError}. */
//	public void setInputError(boolean pinError) {
//		inputError = pinError;
//	}
//
//	/** See {@link #inputRequired}. */
//	public boolean getInputRequired() {
//		return inputRequired;
//	}
//	/** See {@link #inputRequired}. */
//	public void setInputRequired(boolean askPin) {
//		inputRequired = askPin;
//	}


	/**See {@link #selectedObject}. */
	public Object getSelectedObject() {
		return selectedObject;
	}
	/**See {@link #selectedObject}. */
	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

	/**See {@link #selectList}. */
	public List<SelectItem> getSelectList() {
		if (selectList == null) {
			return new ArrayList<>();
		}
		return selectList;
	}
	/**See {@link #selectList}. */
	public void setSelectList(List<SelectItem> selectList) {
		this.selectList = selectList;
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
