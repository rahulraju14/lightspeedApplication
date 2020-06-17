package com.lightspeedeps.web.user;

import java.io.Serializable;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the Create PIN and Change PIN pop-up dialogs, and the
 * mobile Change PIN page.
 */
@ManagedBean
@ViewScoped
public class ChangePinBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 1939942738366168986L;

	private static final Log log = LogFactory.getLog(ChangePinBean.class);

	public static final int ACT_PROMPT_PIN = 30;

	/** Regular expression for numeric string */
	private static final String RE_NUMERIC = "[0-9]+";

	/** The compiled regular expression. See {@link #RE_NUMERIC}. */
	private static final Pattern P_NUMERIC  = Pattern.compile(RE_NUMERIC);

	private User user;

	/** The input field for the password, for validating a change of PIN. */
	private String password;

	/** The PIN input field. */
	private String newPin;

	/** The confirmation PIN input field. */
	private String confirmPin;

	/** JSF navigation string used for alternate routing after
	 * successfully creating a PIN. */
	private String nextPage;

	/** True iff the user is creating their PIN for the first time. */
	private boolean create;

	/** backing field for the checkbox next to the legal text agreeing
	 * to use an electronic signature. */
	private boolean confirmation;

	/** Default Constructor */
	public ChangePinBean() {
		log.debug("");
		user = SessionUtils.getCurrentUser();
		create = (user == null || user.getPin() == null);
	}

	public static ChangePinBean getInstance() {
		return (ChangePinBean)ServiceFinder.findBean("changePinBean");
	}

	/**
	 * The normal method for the desktop-browser code to use to
	 * initialize the bean fields and show the dialog box.
	 * @param holder The optional callback reference.
	 * @return null navigation string
	 */
	public String show(PopupHolder holder) {
		initChange(holder);
		if (user.getPin() == null) {
			create = true;
			setTitle("Create PIN");
		}
		else {
			create = false;
			setTitle("Change PIN");
		}
		setVisible(true);
		return null;
	}

	/**
	 * Action method for the Save button on the Change PIN pop-up dialog.
	 *
	 * @return null navigation string
	 */
	public String actionChangePin() {
		user = UserDAO.getInstance().refresh(user);
		changePin();
		return null;
	}

	/**
	 * Action method for the Save button on the mobile Change PIN page.
	 *
	 * @return appropriate navigation string -- null if there was an error, so
	 *         the user remains on the same page; or the navigation string to
	 *         return to the My Account mobile page if the Save was successful.
	 */
	public String actionChangePinMobile() {
		user = UserDAO.getInstance().refresh(user);
		if (changePin()) {
			return "myaccountm";
		}
		return null;
	}

	/**
	 * Action method for the "Enter" (create) button on the full-page version of
	 * "Create PIN" (not the pop-up dialog), used for mobile devices.
	 *
	 * @return navigation string - either null, or based on the 'nextPage'
	 *         value supplied via the invoking JSP.
	 */
	public String actionCreatePinPage() {
		user = SessionUtils.getCurrentUser();
		if (changePin()) {
			String nav = SessionUtils.getString(Constants.ATTR_PIN_NEXT_PAGE);
			if (nav == null) {
				nav = SessionUtils.mobilize(HeaderViewBean.MYPROD_MENU_PROD);
			}
			else {
				SessionUtils.put(Constants.ATTR_PIN_NEXT_PAGE, null); // clear for next time
			}
			return nav;
		}
		return null;
	}

	public String actionCancelChangePin() {
		if (getConfirmationHolder() != null) {
			getConfirmationHolder().confirmCancel(ACT_PROMPT_PIN);
		}
		setVisible(false);
		return null;
	}

	private boolean changePin() {
		try {
			boolean number = false;
			if (newPin != null) {
				newPin = newPin.trim();
				number = P_NUMERIC.matcher(newPin).matches();
			}
			if (newPin != null) {
				newPin = newPin.trim();
			}
			if (newPin == null || newPin.length() < Constants.MIN_PIN_LENGTH ||
					newPin.length() > Constants.MIN_PIN_LENGTH ||
					(! number && ! getNewPin().equals("null"))) {
				// Trick - you can enter the word "null" and we'll set the PIN to null (below)!
				MsgUtils.addFacesMessage("User.Pin.Invalid", FacesMessage.SEVERITY_ERROR);
				return false;
			}
			if (getConfirmPin() == null ||
					! newPin.equals(getConfirmPin())) {
				MsgUtils.addFacesMessage("User.Pin.InvalidConfirm", FacesMessage.SEVERITY_ERROR);
				return false;
			}
			if (getPassword() == null ||
					! getPassword().equals(user.getPassword())) {
				MsgUtils.addFacesMessage("User.Pin.InvalidPassword", FacesMessage.SEVERITY_ERROR);
				//log.debug("" + user.getEncryptedPassword());
				return false;
			}
			if (create && ! confirmation) {
				MsgUtils.addFacesMessage("User.Pin.CheckLegal", FacesMessage.SEVERITY_ERROR);
				return false;
			}
			String changeMsg = "PIN updated";
			if (user.getPin() == null) {
				changeMsg = "PIN created";
			}
			user.setPin(newPin);
			if (newPin.equals("null")) {
				user.setPin(null);
			}
			UserDAO.getInstance().attachDirty(user);
			ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, changeMsg);
			MsgUtils.addFacesMessage("User.Pin.Changed", FacesMessage.SEVERITY_INFO);
			setVisible(false);
			if (getConfirmationHolder() != null) {
				getConfirmationHolder().confirmOk(ACT_PROMPT_PIN);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return true;
	}

	/**
	 * Initialize our bean fields.
	 * @param holder
	 */
	private void initChange(PopupHolder holder) {
		user = SessionUtils.getCurrentUser();
		setConfirmationHolder(holder);
		setPassword("");
		setNewPin("");
		setConfirmPin("");
	}

	/**
	 * Called from the mobile Change PIN page, via a 'rendered'
	 * JSP tag, to initialize the bean.
	 * @return false
	 */
	public boolean getInitBean() {
		if (user == null) {
			initChange(null);
		}
		return false;
	}

	/** See {@link #user}. */
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	/** See {@link #password}. */
	public String getPassword() {
		return password;
	}
	/** See {@link #password}. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** See {@link #newPin}. */
	public String getNewPin() {
		return newPin;
	}
	/** See {@link #newPin}. */
	public void setNewPin(String newPin) {
		this.newPin = newPin;
	}

	/** See {@link #confirmPin}. */
	public String getConfirmPin() {
		return confirmPin;
	}
	/** See {@link #confirmPin}. */
	public void setConfirmPin(String confirmPin) {
		this.confirmPin = confirmPin;
	}

	/** See {@link #create}. */
	public boolean getCreate() {
		return create;
	}
	/** See {@link #create}. */
	public void setCreate(boolean create) {
		this.create = create;
	}

	/** See {@link #confirmation}. */
	public boolean getConfirmation() {
		return confirmation;
	}
	/** See {@link #confirmation}. */
	public void setConfirmation(boolean confirmation) {
		this.confirmation = confirmation;
	}

	/** See {@link #nextPage}. */
	public String getNextPage() {
		return nextPage;
	}
	/** See {@link #nextPage}. */
	public void setNextPage(String nextPage) {
		this.nextPage = nextPage;
	}

}
