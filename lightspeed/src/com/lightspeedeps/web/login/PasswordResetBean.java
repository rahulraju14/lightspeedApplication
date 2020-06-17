package com.lightspeedeps.web.login;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This class supports both the password reset page, and the "invited user" first
 * login page (where the user must enter a new password). It validates the key
 * supplied in the http request and extracts the user's Id from it. It then gets
 * the User record from the database using the id.
 * <p>
 * If the user enters a new password and matching confirmation password, the
 * User's record is updated with the new password, and the user is also logged
 * in (via the LoginCommandBean).
 */
@ManagedBean
@ViewScoped
public class PasswordResetBean implements Serializable {
	/** */
	private static final long serialVersionUID = 2131675529960972480L;

	private static final Log log = LogFactory.getLog(PasswordResetBean.class);

	/** A reg-ex Pattern specifying all the rules for a valid password.  It must contain at least
	 * one digit, one lowercase alpha, one uppercase alpha, and one special character. Note the
	 * use of "?=", positive look-ahead with "zero-width assertion".  ESS-1462 */
	private static final Pattern validPassword = Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_])[^\\s]{8,20}$");

	/** The message "identifier" used by the JSP to determine the actual
	 * message text to display. */
	private String message;

	/** The actual message text to display, used for the "expired link" case, where
	 * we have substitution to do. */
	private String messageText;

	/** The password field on the page. */
	private String password;

	/** The confirm-password field on the page.*/
	private String confirmPassword;

	/** */
	private String userName;

	/** The User object, determined from the key passed on the URL. */
	private User user;

	/** The database id of the User. */
	private Integer userId;

	/** True iff the key from the URL was validated. */
	private Boolean keyValid;

	/** True iff the current page is resetpwreturn.jsp.  A registered user may use
	 * this page to reset their password; whereas the resetnew.jsp page may only be
	 * used by PENDING users. */
	private boolean pwReturn;

	private String saveKey;

	/** True iff this was a "new account" screen with an invalid key, and the
	 * user asked for a new email, and we sent it. */
	private boolean emailSent;

	/** Backing for the "accept terms of use" checkbox for new users. */
	private boolean acceptTerms;

	/** True iff the user should get the "welcome" page, because they are
	 * a new user, e.g., invited by some other user via Add Contact. */
//	private boolean welcome;


	public PasswordResetBean() {
		log.debug("");
	}

	/**
	 * Action method for the "Save new password" button. If the password passes
	 * validation, we save it in the database and call LoginBean.checkLogin(true),
	 * which will typically forward the user to the "My Account" page.
	 *
	 * @return Faces navigation string (null remains on same page)
	 */
	public String actionNewPassword() {

		boolean bRet = validatePassword( password, confirmPassword );
		if (! bRet) {
			return null;
		}
		// find User
		UserDAO userDao = UserDAO.getInstance();
		user = userDao.findById(userId);

		if (user.getStatus() == UserStatus.PENDING) {
			if (! getAcceptTerms()) {
				// new user must check "accept terms of use"
				setMessage("AcceptTerms");
				return null;
			}
			user.setStatus(UserStatus.REGISTERED);
			ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, user, "User Registered");
			DoNotification.getInstance().newAccount(user);
		}

		// update password
		getUser().setPassword(password);
		getUser().setLockedOut(false);
		getUser().setLockOutTime(null);
		getUser().setFailedLogonCount(0);
		userDao.attachDirty(getUser());
		log.debug("password updated for " + user.getEmailAddress());
		ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, user, "password updated");

		LoginBean loginBean = LoginBean.getInstance();
		if (loginBean != null) {
			loginBean.setUserName(user.getEmailAddress());
			loginBean.setPassword(confirmPassword);
			return loginBean.checkLogin(true); // LS-3800 - redirect to My Account
		}
		return null;
	}

	/**
	 * Action method for the "request new link" button for 3rd party new
	 * registration users (from resetnew.jsp).
	 *
	 * @return Navigation string to go to "email sent" page, or remain on the
	 *         same page (if error).
	 */
	public String actionNewLink() {
		String action = SessionUtils.mobilize("emailsent");
		Contact contact = ContactDAO.getInstance().findOneByProperty(ContactDAO.USER, user);
		if (contact != null) {
			if (user.getStatus() == UserStatus.PENDING) {
				DoNotification.getInstance().inviteNewAccountAgain(contact);
			}
			else {
				String url = PasswordBean.createResetURI(user.getId(), Constants.TIME_EMAIL_LINK_VALID);
				DoNotification.getInstance().passwordResetMessage(user, url);
			}
			emailSent = true;
			setMessage("");
			HeaderViewBean.clearAttributes(SessionUtils.getHttpSession());
		}
		else {
			emailSent = false;
			setMessage("EmailFailed");
			action = "";
		}
		return action;
	}

	/**
	 * Action method for the "request new link" button for "normal" users who
	 * requested a reset-password link, but now it has expired (from
	 * resetpwreturn.jsp).
	 *
	 * @return Navigation string to go to "email sent" page, or remain on the
	 *         same page (if error).
	 */
	public String actionResetLink() {
		String action = SessionUtils.mobilize("emailsent");
		emailSent = false;
		String msg = PasswordBean.sendEmail(user.getEmailAddress(), Constants.TIME_EMAIL_LINK_VALID);
		if (msg == null) {
			emailSent = true;
			setMessage("");
			HeaderViewBean.clearAttributes(SessionUtils.getHttpSession());
		}
		if (!emailSent) {
			setMessage("EmailFailed");
			action = "";
		}
		return action;
	}

	/**
	 * Validate a password (and its 'confirmation') matches all the rules.
	 *
	 * @param pw The input password.
	 * @param confirmPw The confirmation, which should be identical.
	 * @return null if successful, otherwise a text string which is used to look
	 *         up the appropriate message.
	 */
	public static String checkPassword(String pw, String confirmPw) {
		if (StringUtils.isEmpty(pw)) {
			return "Required";
		}
		pw = pw.trim();

		Matcher matcher = validPassword.matcher(pw); // ESS-1462
		if ( ! matcher.matches() ) {
			if (pw.length() < Constants.MIN_PASSWORD_LENGTH) {
				return "TooShort"; // specific error message helps user
			}
			if (pw.length() > Constants.MAX_PASSWORD_LENGTH) {
				return "TooLong"; // specific error message helps user
			}
			return ("MissingRequired");
		}

		if (confirmPw == null || ! pw.equals(confirmPw.trim())) {
			return "ConfirmNotMatched";
		}

		// Success!
		return null;
	}

	private boolean validatePassword(String pw, String confirmPw) {
		String msg = checkPassword(pw, confirmPw);
		if (msg != null) {
			setMessage(msg);
			return false;
		}
		setMessage("");
		return true;
	}

	/** Array of available characters to include in a randomly generated password. */
	private final static char[] chList = {
			'a','b','c','d','e','f','g','h','j','k','m',
			'n','p','q','r','s','t','u','v','x','y','z',
			'2','3','4','5','6','7','8','9'};

	/**
	 * Create a random password.
	 * @return A random alphanumeric password of length 10.
	 */
	public static String createPassword() {
		String pw = "";
		for (int i = 0; i < 10; i++) {
			int x = (int)(Math.random() * chList.length);
			//log.debug(x); // x ranges from 0 through chList.length-1
			pw += chList[x];
		}
		return pw;
	}

	/**
	 * Find the User object based on the key passed on the URL. This is
	 * normally called just once, when the page is first presented, as a
	 * result of the JSP reference to 'keyValid'.
	 *
	 * @return The relevant User object, or null if no valid User could be
	 *         determined from the key.
	 */
	private User findUser() {
		keyValid = false;
		emailSent = false;
		user = null;
		boolean expired = false;
		setMessage("Invalid");
		//HttpServletRequest request = SessionUtils.getHttpRequest();
		String key = findKey();
		if (key != null) {
			try {
				key = URLDecoder.decode(key, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				EventUtils.logEvent(EventType.DATA_ERROR, null, null, null, "password reset key decode failed.");
				return null;
			}
			key = key.replace(' ', '+');
			userId = PasswordBean.getIdFromKey(key);
			if (userId != null) {
				keyValid = true;
				if (userId < 0) { // indication of expired key
					log.debug("called with expired key");
					keyValid = false;
					expired = true;
					userId = - userId; // get actual userId
				}
			}
			if (userId != null) {
				UserDAO userDAO = UserDAO.getInstance();
				user = userDAO.findById(userId);
				if (user == null) {
					keyValid = false;
					expired = false;
					EventUtils.logEvent(EventType.DATA_ERROR, null, null, null,
							"User not found for id="+userId);
				}
				else if (! pwReturn && (user.getStatus() != UserStatus.PENDING)) {
					keyValid = false;
					expired = true;
					// if user jumps to login page, then after logging in, send to My Account:
					SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO,"/jsp/user/account.xhtml");
				}
			}
		}
		if (keyValid) {
			setMessage("");
		}
		else if (expired) { // link "expired" case - generate message text
			setMessage("Expired");
			messageText = MsgUtils.formatMessage("NewAcctKeyExpired", user.getEmailAddress());
		}
		return user;
	}

	/**
	 * See if there's a 'key' or 'nk' parameter in the current HttpRequest,
	 * and return it if so.
	 * @return The key/nk request parameter, or null if one does not exist.
	 */
	private static String findKey() {
		HttpServletRequest request = SessionUtils.getHttpRequest();
		String key = request.getParameter("key");
		if (key == null) {
			key = request.getParameter("nk");
		}
		if (key == null && SessionUtils.isMobileUser()) {
			key = SessionUtils.getString(Constants.ATTR_PW_RESET_KEY);
//			SessionUtils.put(Constants.ATTR_PW_RESET_KEY, null); // only use it once
		}
		log.debug(key);
		return key;
	}

	/** returns email address (as of v1.5) */
	public String getUserName() {
		if (userName == null) {
			if (user == null) {
				user = findUser();
			}
			if (user != null) {
				userName = user.getEmailAddress();
			}
			else {
				userName = "(Not available)";
			}
		}
		return userName;
	}
	public void setUserName(String userName) {
		log.debug(userName);
		this.userName = userName;
	}

	/**
	 * This method is invoked via a JSP reference, and thereby forces the key
	 * validation and determination of the User encoded in the key.
	 *
	 * @return True iff key is valid -- decrypts ok, hasn't expired, etc.
	 */
	public Boolean getKeyValid() {
		/* This 'savekey' manipulation determines if a user has entered a URL
		 * with a different key, without leaving this page. If so, we need to
		 * "start over" and evaluate the key again. (Recall that ICEfaces
		 * maintains our bean state across browser interactions as long as the
		 * user remains on the same page.)
		 */
		if (saveKey == null) {
			saveKey = findKey();
		}
		else {
			String newKey = findKey();
			if (newKey != null && ! newKey.equals(saveKey)) {
				keyValid = null; // force re-evaluation of key
				saveKey = newKey;
			}
		}

		if (keyValid == null) {
			// 1st call after bean instantiation, or user changed key values.
			findUser();	// this will set keyValid to true or false
			// ... and may also set 'message' if necessary.
		}
		return keyValid;
	}
	public void setKeyValid(Boolean validKey) {
		keyValid = validKey;
	}

	/**
	 * invoked by resetpwreturn.jsp so we can treat this page differently
	 * than resetnew.jsp; in particular, we allow a registered user to use
	 * this page to reset their password.
	 *
	 * @return false
	 */
	public String getPwReturn() {
		pwReturn = true;
		return "";
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}
	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/** See {@link #acceptTerms}. */
	public boolean getAcceptTerms() {
		return acceptTerms;
	}
	/** See {@link #acceptTerms}. */
	public void setAcceptTerms(boolean acceptTerms) {
		this.acceptTerms = acceptTerms;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	/** See {@link #messageText}. */
	public String getMessageText() {
		return messageText;
	}
	/** See {@link #messageText}. */
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	/**
	 * This method is called by the JSF framework when this bean is
	 * about to go 'out of scope', e.g., when the user is leaving the page
	 * or their session times out.  We use it to maximize the chances
	 * that a user re-using an email link will get the key re-evaluated.
	 */
	@PreDestroy
	public void dispose() {
		log.debug("");
		try {
			saveKey = null;
			keyValid = null;
		}
		catch (Exception e) {
			log.error("********* EXCEPTION ************" + e.getLocalizedMessage());
		}

	}

}
