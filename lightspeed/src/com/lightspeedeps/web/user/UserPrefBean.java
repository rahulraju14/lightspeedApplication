/** File: UserPrefBean.java */
package com.lightspeedeps.web.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This session-scoped bean holds user preferences (options) that could be set
 * directly by the user's request, or simply settings that the application
 * "remembers" so they are used as defaults for the user. Some of these settings
 * were originally kept as HttpSession attributes, but were moved here so that
 * they can be kept for the user across multiple logins.
 * <p>
 * The information is held in a Map as a set of pairs of <preference name,
 * Object>, where the Object is arbitrary and based on the particular needs of
 * the preference being saved. In general, however, these are simple items such
 * as Strings and Integers. If a complex Object was included, that could cause
 * problems (e.g., excessive memory usage) because this is a session-scoped
 * bean.
 * <p>
 * The bean is instantiated (indirectly, via bean lookup) during the login
 * process. It typically only writes updated information to the database when it
 * goes out of scope, due to either the user logging off, or the session timing
 * out.
 * <p>
 * It relies on the ICEfaces DisposableBean interface, which causes its
 * dispose() method to be called when the bean goes out of scope.
 * <p>
 * Since it is session-scoped, we want to minimize the number of objects it
 * holds references to. For example, instead of holding a User reference, it
 * only keeps the User.id value. One object it holds is a UserDAO, as that is
 * used in the dispose() processing, and cannot be reliably obtained at that
 * time.
 */
@ManagedBean
@SessionScoped
public class UserPrefBean implements Serializable {
	/** */
	private static final long serialVersionUID = 4903392380655523324L;

	private static final Log log = LogFactory.getLog(UserPrefBean.class);

	/** The database id of the User that this preference bean is related to. */
	private Integer userId;

	/** A mapping from preference names to their values. */
	private Map<String, Object> preferences = new HashMap<String, Object>();

	private transient UserDAO userDAO;

	/** The default (and only) constructor. */
	public UserPrefBean() {
		log.debug(this);
	}

	/**
	 * @return The instance for the current session; a new object may be
	 *         instantiated as a result of this call.
	 */
	public static UserPrefBean getInstance() {
		return (UserPrefBean)ServiceFinder.findBean("userPrefBean");
	}

	/**
	 * Store a user preference value.
	 *
	 * @param prefName The name to use for storing (and retrieving) the
	 *            preference value.
	 * @param value The value to be stored; if value is null, the preference is
	 *            removed.
	 */
	public void put(String prefName, Object value) {
		log.debug(prefName + "=" + value);
		if (value == null) {
			preferences.remove(prefName);
		}
		else {
			preferences.put(prefName, value);
		}
	}

	/**
	 * Return a User preference value as an Object.
	 *
	 * @param varName The name used to store the variable.
	 * @return The previously stored value; may be null.
	 */
	public Object get(String varName) {
		return preferences.get(varName);
	}

	/**
	 * Return a User preference value as a String.
	 *
	 * @param varName The name used to store the variable.
	 * @param def The default value to return if the stored value is null (or
	 *            there is no stored value).
	 * @return The string value; can only be null if 'def' is null.
	 */
	public String getString(String varName, String def) {
		String value = (String)preferences.get(varName);
		if (value == null) {
			value = def;
		}
		return value;
	}

	/**
	 * Return a User preference value as a boolean.
	 *
	 * @param varName The name used to store the variable.
	 * @param def The default value to return if the stored value is null (or
	 *            there is no stored value).
	 * @return The boolean value.
	 */
	public boolean getBoolean(String varName, boolean def) {
		Boolean value = (Boolean)preferences.get(varName);
		if (value == null) {
			return def;
		}
		return value;
	}

	/**
	 * Return a User preference value as a Integer.
	 *
	 * @param varName The name used to store the variable.
	 * @return The Integer value; may be null.
	 */
	public Integer getInteger(String varName) {
		return (Integer)preferences.get(varName);
	}

	/**
	 * Recover our saved information from the User object to populate our Map of
	 * preferences.
	 */
	@SuppressWarnings("unchecked")
	private void loadValues() {
		log.debug("");
		User user = findUser();
		try {
			if (user != null) {
				log.debug(user);
				preferences.clear();
				if (user.getPreferences() != null) {
					InputStream is = new ByteArrayInputStream(user.getPreferences());
					ObjectInputStream ois = new ObjectInputStream(is);
					preferences = (Map<String,Object>)ois.readObject();
					//dump(preferences);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Save all current preferences to the database in the User object.
	 */
	private void saveValues() {
		log.debug("");
		User user = findUser();
		if (user != null) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(baos);
				out.writeObject(preferences);
				out.close();
				user.setPreferences(baos.toByteArray());
				getUserDAO().attachDirty(user);
				//dump(preferences);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
	}

	/**
	 * @return The current User object.
	 */
	private User findUser() {
		User user = null;
		if (userId != null) {
			user = getUserDAO().findById(userId);
		}
		return user;
	}

	/**
	 * This method is called by the JSF framework when this bean is
	 * about to go 'out of scope', e.g., when the user is leaving the page
	 * or their session times out.  We use it to unlock the WeeklyTimecard
	 * to make it available again for editing.
	 */
	@PreDestroy
	public void dispose() {
		log.debug(this);
		try {
			saveValues();
		}
		catch (Exception e) {
			log.error("EXCEPTION: " + e.getLocalizedMessage());
		}
	}

	@SuppressWarnings("unused")
	private static void dump(Map<String, Object> map) {
		for (String s : map.keySet()) {
			log.debug("key=" + s + ", value=" + map.get(s));
		}
	}

	/** See {@link #userId}. */
	public Integer getUserId() {
		return userId;
	}
	/** See {@link #userId}. */
	public void setUserId(Integer usrId) {
		userId = usrId;
		if (preferences == null || preferences.size() == 0) {
			// only load it if not already loaded!
			loadValues();
		}
	}

	/**See {@link #userDAO}. */
	public UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}
	/**See {@link #userDAO}. */
	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

}
