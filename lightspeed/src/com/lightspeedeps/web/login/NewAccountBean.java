package com.lightspeedeps.web.login;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * This class supports the New Registration (new user account) page.
 * <p>
 * If the user enters valid email address, password and matching confirmation
 * password, a new User's record is added, and the user is also logged in (via
 * the LoginCommandBean).
 *
 */
@ManagedBean
@ViewScoped
public class NewAccountBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 6379426111765449294L;

	private static final Log log = LogFactory.getLog(NewAccountBean.class);

	/** The message text to display. */
	private String messageText;

	/** The password field on the page. */
	private String password;

	/** The confirm-password field on the page.*/
	private String confirmPassword;

	/** */
	private String emailAddr;

	/** The User object, determined from the key passed on the URL. */
	private User user;

	private boolean acceptTerms;

	/** Company name to use when determining domain specific text. LS-1691*/
	private String companyName;

	/** account type to identify if an employee working in CANADA */
	private String accountType;

	public NewAccountBean() {
		log.debug("");
		user = new User();
		emailAddr = (String)SessionUtils.getHttpSession().getAttribute(Constants.ATTR_CURRENT_USER_NAME);

		// Get the message suffix for domain dependent messages.
		companyName = Constants.LS_COMPANY_NAME;
		if(SessionUtils.isTTCOnline()) {
			companyName = Constants.TTC_ONLINE_COMPANY_NAME;
		}
	}

	/**
	 * Method for the button on the "New account" page used to submit the email
	 * address and password information for verification.
	 *
	 * @return A navigation string, which will typically forward the user to the
	 *         "My Account" page.
	 */
	public String actionSubmit() {
		if (! StringUtils.hasText(emailAddr)) {
			setMessageText(MsgUtils.getMessage("Contact.BlankEmail"));
			return null;
		}
		emailAddr = emailAddr.trim();
		if ( ! EmailValidator.isValidEmail(emailAddr)) {
			setMessageText(MsgUtils.getMessage("Contact.InvalidEmail"));
			return null;
		}

		if (UserDAO.getInstance().existsEmailAddress(emailAddr)) {
			setMessageText(MsgUtils.getMessage("Account.EmailExists"));
			return null;
		}
		if (! StringUtils.hasText(user.getFirstName())) {
			setMessageText(MsgUtils.getMessage("Contact.BlankFirstName"));
			return null;
		}
		if (! StringUtils.hasText(user.getLastName())) {
			setMessageText(MsgUtils.getMessage("Contact.BlankLastName"));
			return null;
		}

		boolean bRet = validatePassword( password, confirmPassword );
		if (! bRet) {
			return null;
		}
		if (! getAcceptTerms()) {
			setMessageText(MsgUtils.getMessage("Account_AcceptTerms"));
			return null;
		}
		// LS-3398 An Account Type must be selected in order to proceed creating a new account.
		if(StringUtils.isEmpty(accountType)) {
			setMessageText(MsgUtils.getMessage("Account.NoAccountType"));
			return null;
		}

		if (ApplicationScopeBean.getInstance().getIsBeta()) {
			// Prevent the public from creating a User account in our test environments!
//			setMessageText(MsgUtils.getMessage("Acount.NoNewAccount"));
//			return null;
		}

		// create User
		user.setEmailAddress(emailAddr);
		user.setPassword(password);
		user.setFirstName(user.getFirstName().trim());
		user.setLastName(user.getLastName().trim());
		user.setReferredBy(SessionUtils.getString(Constants.ATTR_REFERRED_BY));
		// LS-3335 checking account type if an employee working in Canada
		if (accountType.equals(Constants.CANADA_STATE_CODE)) {
			user.setShowCanada(true);
			user.setShowUS(false);
		}
		user = UserDAO.getInstance().createUser(user);
		DoNotification.getInstance().newAccount(user);
		DoNotification.getInstance().userCreated(user, true);


		LoginBean loginBean = LoginBean.getInstance();
		if (loginBean != null) {
			loginBean.setUserName(user.getEmailAddress());
			loginBean.setPassword(confirmPassword);
			return loginBean.checkLogin(true); // LS-3800 redirect to My Account after login
		}
		return null;
	}

	private boolean validatePassword(String pw, String confirmPw) {
		String msg = PasswordResetBean.checkPassword(pw, confirmPw);
		if (msg != null) {
			setMessageText(MsgUtils.getMessage("Password_" + msg));
			return false;
		}
		setMessageText("");
		return true;
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

	/** See {@link #acceptTerms}. */
	public boolean getAcceptTerms() {
		return acceptTerms;
	}
	/** See {@link #acceptTerms}. */
	public void setAcceptTerms(boolean acceptTerms) {
		this.acceptTerms = acceptTerms;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	/** returns email address */
	public String getEmailAddr() {
		return emailAddr;
	}
	public void setEmailAddr(String emailAddr) {
		this.emailAddr = emailAddr;
	}

	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String message) {
		messageText = message;
	}

	/** See {@link #welcomeText}. */
	/** Welcome text based on the domain the user is coming from. LS-1691*/
	public String getWelcomeText() {
		return MsgUtils.formatMessage("Account.NewAccount.Welcome", companyName);
	}

	/** See {@link #postRegisterText}. */
	/** Post registration text based on the domain the user is coming from. LS-1691*/
	public String getPostRegisterText() {
		return MsgUtils.formatMessage("Account.NewAccount.After.Register", companyName, companyName);
	}

	/** See {@link #accountType}. */
	public String getAccountType() {
		return accountType;
	}

	/** See {@link #accountType}. */
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}
}
