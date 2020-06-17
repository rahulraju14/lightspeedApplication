package com.lightspeedeps.web.login;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.FeatureFlagType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * This is the backing bean for the "reset password" page, where a user
 * requests an email be sent to their registered address, so that they
 * can pick a new password when they've forgotten their old one.
 * <p/>
 * It also contains some static utility methods used for password management.
 */
@ManagedBean
@ViewScoped
public class PasswordBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 2166582376846542499L;

	private static final Log log = LogFactory.getLog(PasswordBean.class);

	/** Special value recognized by callers of "getIdFromKey" */
	public static final Integer USERID_EXPIRED = -1;

	private static final String RESET_DATE_FORMAT = "yyyyMMddHHmm";

//	private String extraResetURIParams;
	private String message;
	private final String userName;
	private User user;

	public PasswordBean() {
		log.debug("");
		userName = (String)SessionUtils.getHttpSession().getAttribute(Constants.ATTR_CURRENT_USER_NAME);
	}

	public String actionSendEmail() {
		log.debug("");
		if (userName == null || userName.trim().length() == 0) {
			setMessage("NoEmail");
		}
		else {
			if (sendEmailTo(userName)) {
				setMessage("EmailSent");
			}
		}
		return null;
	}

	/**
	 * Send a "password reset" email to the given email address, with a link
	 * that will be valid for the specified number of hours.
	 *
	 * @param emailAddr The email address to send the email to. This email must
	 *            exist as an existing User's email address, or the request will
	 *            be rejected.
	 * @param hours The number of hours (after the current time) that the email
	 *            link will be valid.
	 * @return null if the request was successful, otherwise a message "id" that
	 *         is used in the JSP pages to determine what error message to show
	 *         the user.
	 *
	 */
	public static String sendEmail(String emailAddr, int hours) {
		String msg = null;
		User user = UserDAO.getInstance().findSingleUser(emailAddr);
		if (user == null) {
			msg = "NoEmail";
			return msg;
		}
		String email = user.getEmailAddress();
		if (email == null || email.trim().length() == 0) {
			msg = "NoEmail";
		}
		else if (EmailValidator.isValidEmail(email)) {
			String url = createResetURI(user.getId(), hours);
			if (url != null) {
				DoNotification.getInstance().passwordResetMessage(user, url);
			}
		}
		else {
			msg = "InvalidEmail";
		}
		return msg; // null in successful case
	}

	private boolean sendEmailTo(String emailAddr) {
		String msg = sendEmail(emailAddr, Constants.TIME_EMAIL_LINK_VALID);
		setMessage(msg);
		return (msg == null);
	}

	/**
	 * Create the URL included in the password-reset email which links back to LS with
	 * encrypted id & time-stamp.
	 */
	public static String createResetURI(Integer userId, int hours) {
		String strUrl = MsgUtils.createBrandedPath(Constants.RESET_PW_PATH);

		strUrl += "?key=" + createKey(userId, hours);

//		if (extraResetURIParams != null && extraResetURIParams.trim().length() > 0) {
//			strUrl += extraResetURIParams.trim();
//		}

		String resetURI = strUrl;
		return resetURI;
	}

	/**
	 * Create the URL to be given to a new user, which will take them to the
	 * "welcome" page where they can enter a new password. This will include an
	 * encrypted key which can be associated with a specific User object, and
	 * which will only work for a specific length of time.
	 *
	 * @param userId The database id of the User to be associated with this URL.
	 * @param hoursValid The number of hours this URL will continue to work.
	 * @param useESS True if the email should redirect to ESS pages; this will
	 *            be ignored if the corresponding Feature Flag is turned off. ESS-1364
	 * @return The complete URL (with protocol)
	 */
	public static String createNewUserURI(Integer userId, int hoursValid, boolean useESS) {
		String strUrl;
		if (useESS) { // caller indicates OK to use ESS redirect ... check FeatureFlag. ESS-1364
			useESS = FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_REGISTER_INTEGRATION);
		}
		if (useESS) {
			strUrl = MsgUtils.createBrandedPath(Constants.NEW_USER_PATH_ESS, true);
			strUrl += "?source=TTCO&rgkey=" + createKey(userId, hoursValid);
		}
		else {
			strUrl = MsgUtils.createBrandedPath(Constants.NEW_USER_PATH, false);
			strUrl += "?nk=" + createKey(userId, hoursValid);
		}
		return strUrl;
	}

	private static String createKey(Integer userId, int hours) {
		final SimpleDateFormat dateFormat = new SimpleDateFormat(RESET_DATE_FORMAT);
		Date m_oDate = new java.util.Date();
		m_oDate.setTime(m_oDate.getTime() + (hours*60*60*1000)); // hours to milliseconds

		String key = dateFormat.format(m_oDate);

		key += userId.longValue();
		PBEStringEncryptor encryptor = LoginBean.getEncryptor();
		String enckey = encryptor.encrypt(key);
		log.debug("key=" + key + ", enc key='" + enckey + "'");
		try {
			enckey = URLEncoder.encode(enckey, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			EventUtils.logError("URLEncoder error: ", e);
		}
		return enckey;
	}

	/**
	 * Extract a User database id from an encrypted key.
	 *
	 * @param encKey The encrypted key, typically passed from an emailed URL.
	 * @return The user.id value encoded in the given key. Returns null if the
	 *         key fails decryption, or if the decrypted value is invalid (too
	 *         short, or has a non-integer userId portion). Returns the negative
	 *         value of user.id if the key has expired.
	 */
	public static Integer getIdFromKey(String encKey) {
		String strId = null;
		String strDate = null;
		Integer userId = null;
		String strKey = null;

		final SimpleDateFormat dateFormat = new SimpleDateFormat(RESET_DATE_FORMAT);
		int dateLen = RESET_DATE_FORMAT.length();
		PBEStringEncryptor encryptor = LoginBean.getEncryptor();
		try {
			strKey = encryptor.decrypt(encKey);
		}
		catch (Exception e1) {
			log.info("password reset - decrypt error: " + e1.getLocalizedMessage());
			strKey = null;
		}
		if (strKey != null && strKey.length() > dateLen) {
			try {
				strDate = strKey.substring(0,dateLen);
				strId = strKey.substring(dateLen);	// get remainder = contact id
				Date date = new java.util.Date();	// get current date
				String currtime = dateFormat.format(date);
				try {
					userId = Integer.parseInt(strId);
					if (strDate.compareTo(currtime) < 0) {
						// expiration timestamp earlier than current time - bad
						log.info("called with expired key, user id=" + userId);
						userId = - userId;
					}
				}
				catch (Exception e) {
					// invalid key, e.g., userid non-numeric; just return null userId
				}
			}
			catch (Exception e) {
				// return null userid
			}
		}
		log.debug("enc key=" + encKey + ", key=" + strKey + ", date=" + strDate + ", id=" + userId);

		return userId;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	public String getUserName() {
		return userName;
	}

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
