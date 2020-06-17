//	File Name:	LoginBean.java
package com.lightspeedeps.web.login;

import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.FeatureFlagType;
import com.lightspeedeps.type.UserStatus;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.popup.ReleaseNotesBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * The backing bean for the Login page. It also includes some static methods
 * used for user access control at other points in our processing.
 */
@ManagedBean
@RequestScoped
public class LoginBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 339059371720388358L;

	private static final Log LOG = LogFactory.getLog(LoginBean.class);

	private static final String USER_COOKIE = "lightspeedeps.user.name";
	private static final long RESET_FAILED_LOGIN_MILLISECS = 1000 * 60 * 60 * 24;
	private static final String DB_FAILURE_MSG = MsgUtils.getMessage("Login.DatabaseFailure");

	private transient AuthorizationBean authBean;

	/** Current date/time for various comparisons and calculations. */
	private transient Date currentDate;

	/** True iff the User has been locked out, typically due to too many invalid
	 * password attempts. */
	private boolean lockedOut;

	/** Field backing the "remember me" checkbox on the login page.  True if the
	 * user wants us to remember him, which we do by pushing a cookie with his
	 * user name (email address). */
	private boolean remember;

	/** True iff we reached the login page due to a session expired condition. */
	private boolean expired;

	/** The username (email address) entered on the login page. */
	private String userName;

	/** Essentially a flag: any non-null value causes the "messageText" to be displayed. (Previously
	 * held a value that was tested by the jsp to determine the message text.) */
	private String message;

	/** The text to be displayed in the error message area on the login screen. */
	private String messageText;

	/** The password entered (not encrypted, but hidden) on the login page. */
	private String password;

	/** when true, reset the failed Login Attempt count to zero */
	private boolean resetCount;

	/** The User instance that matches the entered email address. */
	private transient User user;

	/** The Contact for the matching User, if any. */
	private transient Contact contact;

	/** Company name to use based on domain. LS-1691*/
	private String companyName;

	/**
	 * Constructor Checks cookie from the browser
	 */
	public LoginBean() {
		LOG.debug("----------------- LoginCommandBean ------------------");
		try {
			HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_ADMIN_PROD_IX); // was HOME_MENU_MYHOME_IX; rev 7386
			checkCookie();

			// Get the company name for domain dependent messages.
			companyName = Constants.LS_COMPANY_NAME;
			if(SessionUtils.isTTCOnline()) {
				companyName = Constants.TTC_ONLINE_COMPANY_NAME;
			}

			HttpServletRequest request = SessionUtils.getHttpRequest();
			String refer = request.getParameter("ref");
			if (refer != null) {
				SessionUtils.put(Constants.ATTR_REFERRED_BY, refer);
			}
			String exp = request.getParameter("ex");
			if (exp == null && SessionUtils.isMobileUser()) {
				exp = SessionUtils.getString(Constants.ATTR_SESSION_EXPIRED);
			}
			if (exp != null && exp.equals("1")) {
				expired = true;
				SessionUtils.put(Constants.ATTR_SESSION_EXPIRED, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public static LoginBean getInstance() {
		return (LoginBean)ServiceFinder.findBean("loginBean");
	}

	/**
	 * Action method for the "Login" button on the login page. See
	 * validateLogin() for validation steps.
	 *
	 * @return Navigation string to either (a) leave the user on the login page,
	 *         (b) go to the Home page (normal), or (c) go to some page the user
	 *         was attempting to go to but login was required first, which may
	 *         have been saved by the PageAuthenticatePhaseListener.
	 */
	public String checkLogin() {
		return checkLogin(false);
	}

	/**
	 * Check a user's login. Called from main login page and from 'New Account'
	 * and 'Reset Password' pages. See validateLogin() for validation steps.
	 *
	 * @param newAccount If true, this user is a 'new account' and should be
	 *            directed to the My Account page if the login is successful.
	 *
	 * @return Navigation string to either (a) leave the user on the login page,
	 *         (b) go to the Home page (normal), or (c) go to some page the user
	 *         was attempting to go to but login was required first, which may
	 *         have been saved by the PageAuthenticatePhaseListener.
	 */
	protected String checkLogin(boolean newAccount) {
		String str = Constants.FAILURE;
		boolean bRet = validateLogin();
		boolean noAccess = false;
		if (bRet) { // have valid login credentials, not locked out.
			str = SessionUtils.mobilize(HeaderViewBean.MYPROD_MENU_PROD); // default: go to My Productions
			Contact c = null;
			Unit unit = null;
			// See if we have a saved URL and Unit id to jump to a production page
			String idStr = SessionUtils.getString(Constants.ATTR_SAVED_PAGE_UNIT);
			if (idStr != null) {
				try {
					Integer id = Integer.parseInt(idStr);
					if (id != null) {
						unit = UnitDAO.getInstance().findById(id);
						if (unit != null) {
							Production prod = unit.getProject().getProduction();
							c = ContactDAO.getInstance().findByUserProduction(user, prod);
						}
					}
				}
				catch (Exception e) {
				}
			}
			if (user != null) {
				LOG.debug("");
				//List<Contact> userContactList = ContactDAO.getInstance().findByUserActive(user);
				noAccess = true;
				if (user.getShowCanada() && ! user.getShowUS()) {
					// LS-1820 Canada user will be redirected to myaccount page when logging in
					str = SessionUtils.mobilize(HeaderViewBean.MYACCT_MENU_DETAILS);
				}
				else if (newAccount) {
					// from New Account page, or Reset Password page, go to My Account
					str = SessionUtils.mobilize(HeaderViewBean.MYACCT_MENU_DETAILS);
				}
				else {
					Map<String, Object> values = new HashMap<>();
					values.put("user", user);
					Contact activeContact = ContactDAO.getInstance().findOneByNamedQuery(
							Contact.GET_NON_DELETED_CONTACT_BY_USER_WITH_LOGIN_ALLOWED, values);
					if (activeContact != null) {
						LOG.debug("Contact's Status= " + activeContact.getStatus());
						noAccess = false;
					}
					else if (noAccess && (! SessionUtils.isMobileUser())) {
						LOG.debug("No access to any production!!");
						List<Contact> anyContacts = ContactDAO.getInstance().findByUserActive(user);
						if (anyContacts.size() > 0) { // belongs to at least one active production
							Contact contactUsingOnboard = ContactDAO.getInstance().findOneByNamedQuery(
									Contact.GET_NON_DELETED_CONTACT_BY_ONBOARD_PRODUCTION, values);
							if (contactUsingOnboard != null) {
								LOG.debug("Has at least one Onboarding Production");
								//LS-1820 Canada user will be redirected to myaccount page when logging in
								if (user.getShowCanada() && ! user.getShowUS()) {
									str = HeaderViewBean.MYACCT_MENU_DETAILS;
								}
								else {
									str = HeaderViewBean.MYFORMS_MENU_DETAILS;
								}
							}
							else {
								LOG.debug("Has no Onboarding Production");
								str = HeaderViewBean.MYTIMECARDS_MENU_DETAILS;
							}
						}
						// else: not member of any active prod, so leave on My Productions, may want to create one.
					}
				}
			}
			if (c == null && (! noAccess)) { // normal condition
				Production prod = ProductionDAO.getSystemProduction();
				if (prod != null) {
					c = ContactDAO.getInstance().findByUserProduction(user, prod);
					if (c != null) { // user has credentials on "SYSTEM" production (e.g.,LS admin)
						if (SessionUtils.isMobileUser()) {
							SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, Constants.HOME_PATH_MOBILE);
						}
						else {
							SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, Constants.HOME_PATH);
						}
						unit = prod.getDefaultProject().getMainUnit();
					}
				}
			}
			if (c != null && c.getStatus().isActive() && c.getLoginAllowed() && (! noAccess)) {
				// user is member of SYSTEM; or using an emailed link that is jumping to
				// a page within a specific production, e.g., a callsheet link.
				contact = c;
				String url = SessionUtils.getString(Constants.ATTR_SAVED_PAGE_INFO);
				if (url != null && url.length() > 0) {
					SessionSetUtils.setProduction(contact.getProduction());
					Integer defProjectId = null;
					if (contact.getProject() != null) {
						defProjectId = contact.getProject().getId();
					}
					if (unit != null) {
						contact.setProject(unit.getProject());
					}
					if (checkProjectAccess(contact, getAuthBean())) {
						if (! contact.getProject().getId().equals(defProjectId)) {
							// save contact's new default project in db
							ContactDAO.getInstance().attachDirty(contact);
						}
						SessionUtils.setCurrentContact(contact);
						if (HeaderViewBean.navigateToUrl(url)) {
							str = "";
						}
					}
				}
			}
			else { // check for jump to page 'outside' of a production
				// ...includes product listing page (from www), or My Account, My Timecards, etc.
				String url = SessionUtils.getString(Constants.ATTR_SAVED_PAGE_INFO);
				if (url != null && url.length() > 0 &&
						(url.indexOf("/product/") >= 0 || url.indexOf("/user/") >= 0)) {
					if (HeaderViewBean.navigateToUrl(url)) {
						str = "";
					}
				}
			}
			// LS-2622 Display banner message for release updates after the user logged in
			/*if(FF4JUtils.useFeature(FeatureFlagType.TTCO_RELEASE_NOTES)) {
				ReleaseNotesBean.getInstance().displayBannerMessage(getUser().getEmailAddress());
			}*/
		}
		LOG.debug("login routing to: " + str);
		return str;
	}

	/**
	 * Called by checkLogin() to do login validation.
	 *
	 * Checks
	 *        1. User exists or not.
	 *        2. User is Locked or not
	 *        3. User is authenticated (password validity) or not.
	 *        4. User has some access privileges on his default project.
	 *        5. Update failed login attempts
	 *
	 * @return True if user has been logged in successfully, false if the
	 * login attempt has been rejected.
	 */
	private boolean validateLogin() {
		user = null;
		contact = null;
		setMessage("");
//		 * old "step 0": Is User already logged in on this session?
//		if (SessionUtils.getBoolean(Constants.ATTR_LOGGED_IN, false) &&
//				! ApplicationUtil.isOffline()) {
//			setMessage("UserInvalid");
//			setMessageText(MsgUtils.getMessage("Login.AlreadyLoggedIn"));
//			log.info("Attempted login from second browser window (or tab) was rejected.");
//			return false;
//		}
		if (userName == null || userName.length() == 0) {
			setMessage("UserInvalid");
			setMessageText(MsgUtils.getMessage("Login.BlankUser"));
			return false;
		}
		if ( password == null || password.length() == 0 ) {
			if (ApplicationUtils.isOffline()) {
				password = "password";
			}
			else {
				//log.error("Missing password");
				setMessage("UserInvalid");
				setMessageText(MsgUtils.getMessage("Login.BlankPassword"));
				return false;
			}
		}

		password = password.trim();
		userName = userName.trim();
		// Provide shortcuts for staff!
		if (userName.endsWith("@ttc")) {
			userName = userName.substring(0, userName.indexOf('@')) + "@theTeamCompanies.com";
		}
		else if (userName.endsWith("@ls")) {
			userName = userName.substring(0, userName.indexOf('@')) + "@LightSpeedEps.com";
		}
		else if (userName.endsWith("@t") && ApplicationScopeBean.getInstance().getIsBeta()) {
			userName = userName.substring(0, userName.indexOf('@')) + "@ls.test.ttc";
		}
		LOG.debug("Inside Check login, username: " + getUserName());
		UserDAO userDao = null;
		userDao = UserDAO.getInstance();
		try {
			List<User> usrlist = userDao.findByEmailAddress(getUserName());
			if (usrlist == null) { // only occurs if database error or other unexpected exception
				setMessageText(DB_FAILURE_MSG);
			}
			else if (usrlist.size() > 0) { // matched - valid
				user = usrlist.get(0);
				if (user.getStatus() == UserStatus.DELETED) { // sorry, not allowed
					user = null;
					setMessageText(MsgUtils.getMessage("Login.UserInvalid"));
				}
				else {
					List<Contact> contacts = ContactDAO.getInstance().findByProperty("user", user);
					if (contacts.size() > 0) {
						contact = contacts.get(0); // from any random production
						setMessageText(null);
					}
					else {
						// User without contact - ok to proceed (not in any Production)
					}
				}
			}
			else { // no match (but no database error)
				setMessageText(MsgUtils.getMessage("Login.UserInvalid"));
			}
		}
		catch (Exception e) {
			EventUtils.logError("Exception retrieving User: " + e.toString(), e);
			setMessageText(DB_FAILURE_MSG);
		}

		if (getUser() == null) {
			setMessage("UserInvalid");
			try { // log failure
				EventUtils.logEvent(EventType.LOGIN_FAILED, SessionUtils.getProduction(),
						null, getUserName(),
						"username not in database" +
						", IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR));
			}
			catch (Exception e) {
				LOG.error("exception: ", e);
			}
			return false;
		}

		userName = user.getEmailAddress(); // might be nicer mixed case, for displaying
		boolean userLocked = checkUserAccount();

		LOG.debug("userLocked = " + userLocked);
		if (!userLocked) {
			boolean authenticated = authenticate();
			if (authenticated) {
				try {
					SessionSetUtils.setCurrentUser(getUser());
					SessionUtils.setCurrentContact(contact);
					getAuthBean().loggedIn(user, contact);

//					SessionUtils.setProduction(contact.getProduction());
//					if ( ! checkProjectAccess(contact, getAuthz())) {
//						setMessage("UserInvalid");
//						setMessageText(MsgUtils.getMessage("Login.NoProject"));
//						EventUtils.logEvent(EventType.LOGIN_FAILED, SessionUtils.getProduction(),
//								null, getUserName(), "username has no projects"); // log failure
//						return false;
//					}
					if (getUser().getFailedLogonCount() != 0) {
						getUser().setFailedLogonCount(0);
						userDao.attachDirty(getUser());
					}
					String IP = SessionUtils.getClientIpAddr(SessionUtils.getHttpRequest());
					if (IP != null) {
						SessionUtils.put(Constants.ATTR_IP_ADDR, IP);
					}
					String msg = "session id=" + SessionUtils.getHttpSession().getId() +
							", IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR);
					if (SessionUtils.isMobileUser()) {
						msg += ", mobile";
					}
					EventUtils.logEvent(EventType.LOGIN_OK, msg);
					setCookie();
					SessionUtils.put(Constants.ATTR_LOGGED_IN, Boolean.TRUE);
					SessionUtils.put(Constants.TOMCAT_USER_KEY, user.getEmailAddress());
					setMessage(""); // DH clear message
					return true;
				}
				catch (Exception e) {
					EventUtils.logError("Exception while logging in", e);
				}
			}
			else { /*  Failed Login; check the failCount
							==5  set LockedOut=true, set LockOutTime=current,setFailedLogonCount=0
							==0  set LockOutTime=current, set FailedLogonCount++
							1-5  set FailedLogonCount++
			 	*/
				setMessage("UserInvalid"); // default message
				setMessageText(MsgUtils.getMessage("Login.UserInvalid"));
				EventUtils.logEvent(EventType.LOGIN_FAILED, SessionUtils.getProduction(),
						null, user.getEmailAddress(),
						"invalid password" +
						", IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR));
				try {
					int failCount = getUser().getFailedLogonCount();
					LOG.debug("old failcount="+failCount + ", reset=" + getResetCount());
					if (failCount == 0 || getResetCount() || ApplicationUtils.isOffline()) {
						getUser().setLockOutTime(getCurrentDate());
						getUser().setFailedLogonCount(1);
					}
					else if (failCount == Constants.LOGIN_DAILY_FAILURE_COUNT) {
						getUser().setLockedOut(true);
						getUser().setFailedLogonCount(0);
						getUser().setLockOutTime(getCurrentDate());
						setMessage("UserLocked"); // change to "locked out" message
						setMessageText(MsgUtils.getMessage("Login.UserLocked"));
						 // log user getting locked out
						EventUtils.logEvent(EventType.USER_LOCKED_OUT, SessionUtils.getProduction(),
								null, user.getEmailAddress(),
								"IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR));
						DoNotification notify = DoNotification.getInstance();
						notify.lockoutMessage(getUser());
					}
					else {
						getUser().setFailedLogonCount(++failCount);
					}
					userDao.attachDirty(getUser());
				}
				catch (Exception e) {
					EventUtils.logError(e);
				}
			}

		} // ----- End of userLocked if.
		else {
//			if ( ! getUser().getLoginAllowed()) {
//				setMessage("UserInvalid");
//				setMessageText(MsgUtils.getMessage("Login.UserInvalid"));
//				EventUtils.logEvent(EventType.LOGIN_FAILED, SessionUtils.getProduction(),
//						null, contact.getEmailAddress(), "login not enabled");
//			}
//			else {
				setMessage("UserLocked");
				setMessageText(MsgUtils.getMessage("Login.UserLocked"));
				// log failure due to user already locked out
				EventUtils.logEvent(EventType.LOGIN_FAILED, SessionUtils.getProduction(),
						null, user.getEmailAddress(), "username is locked out" +
								", IP=" + SessionUtils.getString(Constants.ATTR_IP_ADDR));
//			}
		}
		return false;
	}

	public String reset() {
		LOG.debug("Inside reset");
		return "myhome";
	}

	public static boolean checkProjectAccess(Contact contact) {
		return checkProjectAccess(contact, AuthorizationBean.getInstance());
	}

	/**
	 * Set the user's default project as the current project, and load the authorization map
	 * (pgFields). If the user does not have any privileges in the current project, try and find
	 * another project. This could happen, for example, if the user's role in their default project
	 * was changed to "Candidate" by some other user.
	 */
	private static boolean checkProjectAccess(Contact pContact, AuthorizationBean authBean) {
		boolean bRet = false;
		try {
			SessionUtils.setCurrentProject(pContact.getProject());
			authBean.auth(pContact, true);
			if (! authBean.getHasPermissions()) {
				// user had no access right in his default project; try to find another one
				LOG.warn("User had no rights in current project, user=" + pContact.getEmailAddress() +
						", project=" + (pContact.getProject()==null ? "null" : pContact.getProject().getTitle()));
				Map<String, Object> values = new HashMap<>();
				values.put("contact", pContact);
				Project project = ProjectDAO.getInstance()
						.findOneByNamedQuery(ProjectMember.GET_PROJECT_BY_PROJECTMEMBER_UNIT_EMPLOYMENT, values);
				if (project != null) {
					SessionUtils.setCurrentProject(project);
					pContact.setProject(project);
					authBean.auth(pContact);
					if (authBean.getHasPermissions()) {
						ContactDAO.getInstance().attachDirty(pContact);
					}
				}
			}
			bRet = (authBean.getHasPermissions());
			if (! bRet) {
				authBean.loggedIn(pContact.getUser(), pContact); // at least give the user logged-in privilege(s)
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	private void setCookie() {
		try {
			HttpServletResponse response =
					(HttpServletResponse)FacesContext.getCurrentInstance().getExternalContext().getResponse();

			Cookie btuser = new Cookie(USER_COOKIE, "?"); // some browsers don't like empty values
			btuser.setPath("/"); // empty path to allow sharing with ESS. LS-3758
			LOG.debug("domain=" + btuser.getDomain());
			if (remember == true) {
				btuser.setValue(getUserName());
				btuser.setMaxAge(60*60*24*365); // about 1 year (in seconds)
			}
			else {
				btuser.setMaxAge(1); // about 1 second
			}
			response.addCookie(btuser);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Get the array of Cookies from the ExternalContext, and look for the lightspeed cookie,
	 * which holds user name.  If found, set our userName field.
	 */
	private void checkCookie() {
		try {
			Cookie cookies[] = SessionUtils.getHttpRequest().getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(USER_COOKIE)) {
						if (cookie.getValue() != null) {
							setUserName(cookie.getValue());
							setRemember(true);
						}
						else {
							setUserName("");
							setRemember(false);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * User authentication with Jasypt.
	 *
	 * @return true if the user-supplied password matches the stored (encrypted)
	 *         password.
	 */
	private boolean authenticate() {
		boolean bRet = false;
		if (ApplicationUtils.isOffline()) {
			return true; // no password required
		}
		try {
			if (getUser().getPassword() == null) {
				LOG.debug("setting pw");
				//PBEStringEncryptor encryptor = getEncryptor();
				//String encpw = encryptor.encrypt(password);
				//log.debug("user=" + getUserName() + ", pw=" + password + ", enc pw2='" + encpw + "'");
				getUser().setPassword(password);
				UserDAO.getInstance().attachDirty(getUser());
			}

			if ( password.equals( getUser().getPassword())) {
				LOG.debug("matched");
				return true;
			}
			else {
				LOG.debug("un-matched");
				//log.debug(" DB Password=`" + getUser().getEncryptedPassword() + "`");
				return false;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * Check the User's lock-out status, and return true if the user is locked out.
	 * Also sets two flags:
	 * <p> initialFail: true
	 * <p> resetCount: true if the "failedLogonCount" value should be reset to zero if the
	 * user logs in successfully.
	 *
	 * @return boolean: true if user is locked out; false if not locked out.
	 * Also returns true if the user is not allowed to login at all (user.loginAllowed is false).
	 *
	 */
	private boolean checkUserAccount() {
		//setInitialFail(false);
		setResetCount(false);
		try {
			setLockedOut(getUser().getLockedOut());
			Date lockOutTime = getUser().getLockOutTime();

//			if ( ! getUser().getLoginAllowed()) {
//				setMessage("UserInvalid");
//				return true;
//			}
			if (null == getUser().getFailedLogonCount() || null == lockOutTime) {
				return false;
			}
			if (getLockedOut() && ! ApplicationUtils.isOffline()) {
				setMessage("UserLocked");
				return true;
			}
			if ( ((getCurrentDate().getTime() - lockOutTime.getTime()) > RESET_FAILED_LOGIN_MILLISECS)
					|| ApplicationUtils.isOffline() ) {
				// after 'n' milliseconds (e.g., 24 hrs) reset the failed count to 0
				setResetCount(true);
			}
			return false;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			setMessage("UserLocked");
			return true;
		}
	}

	/**
	 * Automatically login the specified User. LS-3758
	 *
	 * @param pUser The User to login.
	 * @return the navigation string appropriate for this user
	 */
	public String autoLogin(User pUser) {
		setUserName(pUser.getEmailAddress());
		setPassword(pUser.getPassword());
		String nav = checkLogin(); 	// Get the user logged in
		if (nav != null && nav.compareTo(Constants.FAILURE) != 0) {
			SessionUtils.put(Constants.ATTR_CURRENT_USER, user.getId());
			SessionUtils.put(Constants.ATTR_CROSS_APP, true);
		}
		return nav;
	}

	/**
	 * Automatically login the specified User. ESS-1189
	 *
	 * @param token The token, which encodes an existing user's identity, and
	 *            must not be expired.
	 * @return the navigation string appropriate for this user, or null if the
	 *         user was not logged in (e.g., due to invalid token).
	 */
	public String autoLogin(String token) {
		user = SessionUtils.getCurrentUser();
		if (user != null) {
			LOG.debug("user=" + user);
			// TODO validate token is for current user? Or login user from token?
		}
		String nav = null; 	// navigation string, TBD
		try {
			user = ApiUtils.validateToken(token);
			if (user != null) {
				setUserName(user.getEmailAddress());
				setPassword(user.getPassword());
				nav = checkLogin();
				if (nav != null && nav.compareTo(Constants.FAILURE) != 0) {
					SessionUtils.put(Constants.ATTR_CURRENT_USER, user.getId());
					SessionUtils.put(Constants.ATTR_CROSS_APP, true);  // remember user is running cross-app
					SessionUtils.put(Constants.ATTR_ORIGIN_ESS, true); // remember user logged in via ESS. LS-3758
				}
			}
		}
		catch (Exception e) {
			// catch validateToken failure -- returns null
			EventUtils.logError(e);
		}
		return nav;
	}

	public String actionGotoReset() {
		if (userName == null || userName.length() == 0) {
			setMessage("UserInvalid");
			setMessageText(MsgUtils.getMessage("Login.ResetBlankUser"));
			return Constants.FAILURE;
		}
		userName = userName.trim();
		SessionUtils.getHttpSession().setAttribute(Constants.ATTR_CURRENT_USER_NAME, userName);
		return SessionUtils.mobilize("reset");
	}

	public String actionNewAccount() {
		if (userName == null) {
			userName = "";
		}
		userName = userName.trim();
		SessionUtils.getHttpSession().setAttribute(Constants.ATTR_CURRENT_USER_NAME, userName);
		return SessionUtils.mobilize("newaccount");
	}

	/*
	 *
	 * Getters and Setters
	 *
	 */

	/**See {@link #currentDate}. */
	private Date getCurrentDate() {
		if (currentDate == null) {
			currentDate = new Date();
		}
		return currentDate;
	}

	/** See {@link #remember}. */
	public boolean getRemember() {
		return remember;
	}
	/** See {@link #remember}. */
	public void setRemember(boolean remember) {
		this.remember = remember;
	}

	/** See {@link #userName}. */
	public String getUserName() {
		return userName;
	}
	/** See {@link #userName}. */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/** See {@link #password}. */
	public String getPassword() {
		return password;
	}
	/** See {@link #password}. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** See {@link #message}. */
	public String getMessage() {
		return message;
	}
	/** See {@link #message}. */
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

	/**See {@link #expired}. */
	public boolean getExpired() {
		return expired;
	}
	/**See {@link #expired}. */
	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	/**See {@link #user}. */
	private User getUser() {
		return user;
	}

	/**See {@link #lockedOut}. */
	public boolean getLockedOut() {
		return lockedOut;
	}
	/**See {@link #lockedOut}. */
	public void setLockedOut(boolean lockedOut) {
		this.lockedOut = lockedOut;
	}

	/**See {@link #resetCount}. */
	private boolean getResetCount() {
		return resetCount;
	}
	/**See {@link #resetCount}. */
	private void setResetCount(boolean counter) {
		resetCount = counter;
	}

	private AuthorizationBean getAuthBean() {
		if (authBean == null) {
			authBean = AuthorizationBean.getInstance();
		}
		return authBean;
	}

	/**
	 * @return the encryptor instance used for password encryption & decryption,
	 * which is defined in the applicationContext xml file.
	 */
	public static PBEStringEncryptor getEncryptor() {
		StandardPBEStringEncryptor encryptor = (StandardPBEStringEncryptor)ServiceFinder.findBean("strongEncryptor");
		return encryptor;
	}

	/**
	 * Domain specific new account question msg.
	 */
	public String getNewAccountQuestion() {
		return MsgUtils.formatMessage("Login.NewAccount.Question", companyName);
	}

	/**
	 * Domain specific new account msg.
	 */
	public String getNewAccountText() {
		return MsgUtils.formatMessage("Login.NewAccount.Text", companyName);
	}

	/**
	 * Domain specific forgotten password msg.
	 */
	public String getForgotPassword() {
		return MsgUtils.formatMessage("Login.ForgotPassword", companyName);
	}

	/**
	 * Prints a list of available Java Security providers.
	 */
//	private void listProviders() {
//		for (Provider provider : java.security.Security.getProviders()) {
//			System.out.println("Provider: " + provider.getName());
//			for (Provider.Service service : provider.getServices()) {
//				System.out.println("  Algorithm: " + service.getAlgorithm() + ", type: " + service.getType());
//			}
//		}
//	}

}
