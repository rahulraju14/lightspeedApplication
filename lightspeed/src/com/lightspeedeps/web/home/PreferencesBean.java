package com.lightspeedeps.web.home;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.login.PasswordResetBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the "My Preferences" mini-tab of the Home page and the My
 * Account page. This includes the "Change Password" function on the My Account
 * page and the mobile "Change Password" page.
 */
@ManagedBean
@ViewScoped
public class PreferencesBean implements Serializable {
	/** */
	private static final long serialVersionUID = 6411531273981168280L;

	private static final Log log = LogFactory.getLog(PreferencesBean.class);

	/** The User whose preferences are being displayed & modified. */
	private transient User user;

	/** The Contact corresponding to the User being displayed & modified. */
	private transient Contact contact;

	private boolean sendAdvanceScript;
	private boolean editMode = false;

	/** True if the Change Password pop-up should be displayed. */
	private boolean showChangePassword;

	/** True if "change password" is for the current user (from the Home
	 * | My preferences tab); false if it is from the Contact | Admin tab. */
	private boolean changeCurrentPw;

	/** For change password pop-up: existing ("old") password. */
	private String password;
	/** For change or reset password pop-up: new password. */
	private String newPassword;
	/** For change or reset password pop-up: confirmation of new password. */
	private String confirmPassword;

	/** The User Id of the contact whose password is being changed.  This will
	 * NOT be the current user if we are called from some (as yet undefined)
	 * administrative function. */
	private Integer userId;

	private transient ContactDAO contactDAO;

	public PreferencesBean() {
		log.debug("PreferencesBean");
		try {
			if (getUser() != null) {
				setupUser();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public static PreferencesBean getInstance() {
		return (PreferencesBean)ServiceFinder.findBean("preferencesBean");
	}

	public String actionSave() {
		log.debug("");
		try {
			if (getContact() != null) {
				if (! getSendAdvanceScript()) {
					getContact().setSendAdvanceScript(0);
				}
				else if (getContact().getSendAdvanceScript() == 0) {
					getContact().setSendAdvanceScript(2);
				}
				getContactDAO().attachDirty(getContact());
			}
//			ListView.addClientResize();
			editMode = false;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SaveFailed", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	public String actionEdit() {
		editMode = true;
//		ListView.addClientResize();
		return null;
	}

	public String actionCancel() {
		refreshUser();
		editMode = false;
//		ListView.addClientResize();
		return null;
	}

	public String actionEscape() {
		boolean ret = PopupBean.actionEscape();
		if (! ret && editMode) {
			actionCancel();
		}
		return null;
	}

	/**
	 * Action method for the "Change password" button on the Home | Preferences
	 * tab.  This method just opens the Change Password pop-up.  The User/Contact
	 * being affected is the current User.
	 * @return null navigation string
	 */
	public String actionOpenChangePassword() {
		showChangePassword = true;
		initChangePassword();
//		ListView.addClientResize();
		ListView.addFocus("changePassword");
		return null;
	}

	/**
	 * Initialize our fields; used for both desktop and
	 * mobile pages.
	 */
	private void initChangePassword() {
		changeCurrentPw = true;
		setPassword("");
		setNewPassword("");
		setConfirmPassword("");
		userId = getUser().getId();
	}

	/**
	 * Action method for the "Reset password" button on the Contact | Admin
	 * tab.  The 'userId' field should have been set via an f:setPropertyActionListener
	 * tag in the jsp.  This method just opens the Change Password pop-up.
	 * @return
	 */
/*	public String actionOpenResetPassword() {
		showChangePassword = true;
		changeCurrentPw = false;
		setPassword("");
		setNewPassword("");
		setConfirmPassword("");
		ListView.addClientResize();
		return null;
	}
*/
	/**
	 * Action method for the Cancel button on the "Change Password" pop-up.
	 * It just closes the pop-up.
	 * @return null navigation string
	 */
	public String actionCancelChangePassword() {
		showChangePassword = false;
		userId = null;
		ListView.addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "Save" button on the "Change password" pop-up.
	 * It validates the password and confirmation password; and in the case of
	 * changing the current user, validates the old password.  If everything
	 * is valid, the new password is stored.
	 * @return null navigation string
	 */
	public String actionChangePassword() {
		changePassword();
		return null;
	}

	/**
	 * Action method for the "Save" button on the "Change password" mobile page.
	 * It validates the password and confirmation password; and in the case of
	 * changing the current user, validates the old password. If everything is
	 * valid, the new password is stored.
	 *
	 * @return appropriate navigation string -- empty if there was an error, so
	 *         the user remains on the same page; or the navigation string to
	 *         return to the My Account mobile page if the Save was successful.
	 */
	public String actionChangePasswordMobile() {
		if (changePassword()) {
			return "myaccountm";
		}
		return null;
	}

	/**
	 * Action method for the "Save" button on the "Change password" pop-up. It
	 * validates the password and confirmation password; and in the case of
	 * changing the current user, validates the old password. If everything is
	 * valid, the new password is stored.
	 *
	 * @return True iff the new password was stored.
	 */
	private boolean changePassword() {
		log.debug("");
		boolean bRet = false;
		try {
			String msg = PasswordResetBean.checkPassword(newPassword, confirmPassword);
			if (msg != null) {
				msg = "Password_" + msg;
				MsgUtils.addFacesMessage(msg, FacesMessage.SEVERITY_ERROR);
			}
			else if ( changeCurrentPw && (password == null || password.trim().length() == 0)) {
				MsgUtils.addFacesMessage("Password.PasswordRequired", FacesMessage.SEVERITY_ERROR);
			}
			else {
				UserDAO userDAO = UserDAO.getInstance();
				User tuser = userDAO.findById(userId);
				if (tuser != null) {
					if ( changeCurrentPw && ! password.equals( tuser.getPassword())) {
						MsgUtils.addFacesMessage("Password.InvalidPassword", FacesMessage.SEVERITY_ERROR);
					}
					else {
						// update password
						tuser.setPassword(newPassword);
						userDAO.attachDirty(tuser);
						log.debug("password updated for " + tuser.getDisplayName());
						ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, tuser, "password updated");
						showChangePassword = false; // close the pop-up
						MsgUtils.addFacesMessage("Password.Updated", FacesMessage.SEVERITY_INFO);
						ListView.addClientResizeScroll();
						bRet = true;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	public void listenSendAdvanceScript(ValueChangeEvent evt) {
		Boolean b = (Boolean)evt.getNewValue();
		getContact().setSendAdvanceScript(b ? 2 : 0);
		log.debug(b + ", n=" + getContact().getSendAdvanceScript());
	}

	private void refreshUser() {
		user = UserDAO.getInstance().refresh(getUser());
		contact = getContactDAO().refresh(getContact());
		setupUser();
	}

	private void setupUser() {
		if (getContact() != null) {
			setSendAdvanceScript(
					(getContact().getSendAdvanceScript() != null && getContact().getSendAdvanceScript() > 0) ? true : false);
		}
	}

	/**
	 * Called from the mobile Change Password page, via a 'rendered'
	 * JSP tag, to initialize the bean.
	 * @return false
	 */
	public boolean getInitPassword() {
		if (userId == null || userId != getUser().getId()) {
			initChangePassword();
		}
		return false;
	}

	/**See {@link #user}. */
	private User getUser() {
		if (user == null) {
			user = SessionUtils.getCurrentUser();
		}
		return user;
	}

	/** See {@link #contact}. */
	public Contact getContact() {
		if (contact == null) {
			contact = SessionUtils.getCurrentContact();
		}
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #editMode}. */
	public boolean isEditMode() {
		return editMode;
	}
	/** See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/** See {@link #showChangePassword}. */
	public boolean getShowChangePassword() {
		return showChangePassword;
	}
	/** See {@link #showChangePassword}. */
	public void setShowChangePassword(boolean showChangePassword) {
		this.showChangePassword = showChangePassword;
	}

	/** See {@link #changeCurrentPw}. */
	public boolean getChangeCurrentPw() {
		return changeCurrentPw;
	}
	/** See {@link #changeCurrentPw}. */
	public void setChangeCurrentPw(boolean resetPw) {
		changeCurrentPw = resetPw;
	}

	/** See {@link #password}. */
	public String getPassword() {
		return password;
	}
	/** See {@link #password}. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** See {@link #newPassword}. */
	public String getNewPassword() {
		return newPassword;
	}
	/** See {@link #newPassword}. */
	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	/** See {@link #confirmPassword}. */
	public String getConfirmPassword() {
		return confirmPassword;
	}
	/** See {@link #confirmPassword}. */
	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	/** See {@link #userId}. */
	public Integer getUserId() {
		return userId;
	}
	/** See {@link #userId}. */
	public void setUserId(Integer contactId) {
		userId = contactId;
	}

	public boolean getSendAdvanceScript() {
		return sendAdvanceScript;
	}
	public void setSendAdvanceScript(boolean sendAdvanceScript) {
		this.sendAdvanceScript = sendAdvanceScript;
	}

	public List<SelectItem> getCallSheetVersionList() {
		return EnumList.getCallSheetVersionList();
	}
	public void setCallSheetVersionList(List<SelectItem> callSheetVersionList) {
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

}
