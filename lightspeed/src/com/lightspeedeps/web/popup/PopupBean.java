/**
 * File: PopupBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A class to display a simple confirmation dialog box, and get back an OK or Cancel result, and
 * pass that to our "holder". This gives us complete control over the styling of the dialog box,
 * which is not possible with the simple javascript prompt, and still limited with the ICEfaces
 * PanelConfirmation tag.
 * <p>
 * The page that fronts the bean which is using this class should include the
 * common/confirmpopup.xhtml fragment.
 */
@ManagedBean
@ViewScoped
public class PopupBean implements Serializable{
	/** */
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(PopupBean.class);

	/**
	 * The reference to our "owner", used to call back for
	 * certain operations. See the ConfirmationHolder interface.
	 */
	private PopupHolder confirmationHolder;

	/** The message to display in the dialog box. */
	private String message;

	/** The label for the "Ok" button. */
	private String buttonOkLabel = "Ok";

	/** The label for the "Cancel" button. */
	private String buttonCancelLabel = "Cancel";

	/** The title for the dialog box. */
	private String title;

	/** Flag that determines whether to show warning message in popup */
	private boolean showWarningMessage = false;

	/** Warning message to be displayed */
	private String warningMessage;

	/** The action value is set when the confirmation box is "created", and it
	 * is returned to the holder on the confirmCancel and confirmOk callbacks,
	 * so the holder will know which confirmation was in progress without any
	 * additional tracking required. */
	private int action = -1;

	/** If true, then our dialog box is visible. */
	private boolean visible = false;
	

	public PopupBean() {
	}

	public static PopupBean getInstance() {
		return (PopupBean)ServiceFinder.findBean("popupBean");
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * String is used to create three message-ids, for the title, text, and OK
	 * button. The composed message-ids will be looked up in the message
	 * resource file. The text of the Cancel button will be from the message
	 * resource "Confirm.Cancel", which is currently set to "Cancel".
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix The prefix of the message-ids for the title, text, and OK
	 *            button for the dialog box. The actual message ids looked up
	 *            will be the prefix string plus "Title", "Text", and "Ok".
	 */
	public void show(PopupHolder holder, int act, String prefix) {
		show(holder, act, prefix + "Title",
				prefix + "Text", prefix + "Ok", "Confirm.Cancel");
//		ListView.addClientResize();
	}

	/**
	 * Display a confirmation dialog with only a single button, "Close". This is
	 * designed for displaying text that needs more than just a one-line message
	 * in the message area.
	 *
	 * @param prefix
	 *            The message-id prefix; this will be used to determine the
	 *            message-id for the title and the message text. Note that no
	 *            button text is derived from this prefix.
	 */
	public void show(String prefix) {
		show(null, 0, prefix + "Title", prefix + "Text", "Confirm.Close", null);
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * Strings are all message-ids, which will be looked up in the message
	 * resource file.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box or null.
	 * @param msgId The message-id of the message to be shown in the dialog or null.
	 * @param buttonOkId The message-id of the text for the "OK" button or null.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button or null.
	 */
	public void show(PopupHolder holder, int act, String titleId, String msgId,
			String buttonOkId, String buttonCancelId) {
		log.debug("action=" + act);
		if (msgId == null) {
			message = "";
		}
		else {
			message = MsgUtils.getMessage(msgId);
		}
		show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the Strings are all
	 * message-ids, which will be looked up in the message resource file. This call does NOT specify
	 * a message text -- it should be set via the setMessage() call. This invocation is designed for
	 * cases in which the message text is not just a constant text.
	 *
	 * @param holder The object which is "calling" us, and will get the callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box or null.
	 * @param buttonOkId The message-id of the text for the "OK" button or null.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button or null.
	 */
	public void show(PopupHolder holder, int act, String titleId,
			String buttonOkId, String buttonCancelId) {
		log.debug("action=" + act);
		confirmationHolder = holder;
		action = act;
		if (titleId == null) {
			title = "";
		}
		else {
			title = MsgUtils.getMessage(titleId);
		}

		if (buttonOkId == null) {
			buttonOkLabel = null;
		}
		else {
			buttonOkLabel = MsgUtils.getMessage(buttonOkId);
		}

		if (buttonCancelId == null) {
			buttonCancelLabel = null;
		}
		else {
			buttonCancelLabel = MsgUtils.getMessage(buttonCancelId);
		}
		
		setVisible(true);
	}

	/**
	 * Action method for the Cancel button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmCancel() method.
	 */
	public String actionCancel() {
		log.debug("Cancel, action=" + action);
		/*TODO DH: Due to this action cancel method,
		  pop up on transfer tab (info popup for failure of some documents & success for the others) 
		  disappears automatically after 2 3 seconds.*/
		String res = null;
		if (visible) {
			setVisible(false);
			if (confirmationHolder != null) {
				res = confirmationHolder.confirmCancel(action);
			}
		}
		else {
			log.warn("extra Cancel -- double click? -- ignored.");
		}
		return res;
		
	}

	/**
	 * Method called by "X" icon built-in to ace:dialog header.
	 * Called due to ace:ajax tag in pop-ups.
	 * @param evt Ajax event from the framework.
	 */
	public void actionClose(AjaxBehaviorEvent evt) {
		log.debug("");
		// 'visible' was already set to false before this method is
		// called.  Set it true, so 'actionCancel()' will take it's normal
		// path, and not assume this was a double-click of the Cancel button.
		visible = true;
		actionCancel(); // note that this will call a sub-class's actionCancel() if it exists.
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This
	 * closes the dialog box, and calls our holder's confirmOk() method.
	 */
	public String actionOk() {
		log.debug("OK, action=" + action);
		String res = null;
		if (visible) {
			setVisible(false);
			if (confirmationHolder != null) {
				res = confirmationHolder.confirmOk(action);
			}
			else {
				//ListView.addClientResize();
			}
		}
		else {
			log.warn("extra OK -- double click? -- ignored.");
		}
		return res;
	}

	/**
	 * Method used to handle an ESCape key function.  If the confirmation dialog
	 * is visible, call our normal Cancel function and return true; otherwise
	 * return false.
	 */
	public static boolean actionEscape() {
		log.debug("");
		boolean ret = false;
		PopupBean conf = getInstance();
		if (conf.isVisible()) {
			conf.actionCancel();
			ret = true;
		}
		return ret;
	}

	/** See {@link #confirmationHolder}. */
	public PopupHolder getConfirmationHolder() {
		return confirmationHolder;
	}
	/** See {@link #confirmationHolder}. */
	public void setConfirmationHolder(PopupHolder confirmationHolder) {
		this.confirmationHolder = confirmationHolder;
	}

	/** See {@link #visible}. */
	public boolean isVisible() {
		return visible;
	}
	/** See {@link #visible}. */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/** See {@link #title}. */
	public String getTitle() {
		return title;
	}
	/** See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

	/** See {@link #message}. */
	public String getMessage() {
		return message;
	}
	/** See {@link #message}. */
	public void setMessage(String message) {
		this.message = message;
	}

	/** See {@link #action}. */
	public int getAction() {
		return action;
	}
	/** See {@link #action}. */
	public void setAction(int action) {
		this.action = action;
	}

	/** See {@link #buttonOkLabel}. */
	public String getButtonOkLabel() {
		return buttonOkLabel;
	}
	/** See {@link #buttonOkLabel}. */
	public void setButtonOkLabel(String buttonOkLabel) {
		this.buttonOkLabel = buttonOkLabel;
	}

	/** See {@link #buttonCancelLabel}. */
	public String getButtonCancelLabel() {
		return buttonCancelLabel;
	}
	/** See {@link #buttonCancelLabel}. */
	public void setButtonCancelLabel(String buttonCancelLabel) {
		this.buttonCancelLabel = buttonCancelLabel;
	}

	/** See {@link #showWarningMessage}. */
	public boolean getShowWarningMessage() {
		return showWarningMessage;
	}

	/** See {@link #showWarningMessage}. */
	public void setShowWarningMessage(boolean showWarningMessage) {
		this.showWarningMessage = showWarningMessage;
	}

	/** See {@link #warningMessage}. */
	public String getWarningMessage() {
		return warningMessage;
	}

	/** See {@link #warningMessage}. */
	public void setWarningMessage(String warningMessage) {
		this.warningMessage = warningMessage;
	}

	
	
}	
