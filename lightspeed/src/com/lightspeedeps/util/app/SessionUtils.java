//	File Name:	SessionUtils.java
package com.lightspeedeps.util.app;

import java.io.*;
import java.util.*;

import javax.faces.context.*;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.commons.logging.*;
import org.json.*;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.web.login.LoginBean;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.timecard.TimecardBase;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A class for various utility functions, typically static, related to
 * data that is session-specific, such as the current user, project, etc.
 */
public final class SessionUtils {
	private static final Log LOG = LogFactory.getLog(SessionUtils.class);

	private static boolean jUnitUnderEclipse = false;

	/*package*/ static boolean noSession = false;

	private static HttpSessionMap sessionMap = new HttpSessionMap();

	private static String realReportPath = null;

	private SessionUtils() {
		// prevent instantiation
	}

	//private static SessionUtils getInstance() {
	//	return (SessionUtils)ServiceFinder.findBean("SessionUtils");
	//}

	/**
	 * @return The User object of the currently logged-in user.
	 */
	public static User getCurrentUser() {
		User user = null;
		try {
			HttpSession session = getHttpSession();
			if (session != null) {
				Integer userId = (Integer)session.getAttribute(Constants.ATTR_CURRENT_USER);
				if (userId != null) {
					user = UserDAO.getInstance().findById(userId);
				}
			}
			LOG.debug("user#: " + (user==null?"null":user.getId()));
		}
		catch (Exception e) {
			// Note: do NOT use EventUtils logging here, as it may call this method!
			LOG.error(e);
		}
		return user;
	}

	/**
	 * Save the current user's access token in their session.
	 * @param token The access token to be saved.
	 */
	public static void setCurrentAccessToken(JSONObject token) {
		HttpSession session = getHttpSession();
		if (token == null) {
			session.removeAttribute(Constants.ATTR_ACCESS_TOKEN);
		}
		else {
			session.setAttribute(Constants.ATTR_ACCESS_TOKEN, token);
		}
	}

	/**
	 * @return The user's current access-refresh token, if any.  May be null.
	 */
	public static String getCurrentRefreshToken() {
		String token = null;
		HttpSession session = getHttpSession();
		if (session != null) {
			JSONObject accessToken = (JSONObject)session.getAttribute(Constants.ATTR_ACCESS_TOKEN);
			if (accessToken != null) {
				try {
					token = accessToken.getString("refresh_token");
				}
				catch (JSONException e) {
					EventUtils.logError(e);
					throw new LoggedException(e);
				}
			}
		}
		LOG.debug("refresh token is " + (token == null ? "null" : "non-null"));
		return token;
	}

	public static Contact getCurrentContact() {
		Contact contact = null;
		try {
			HttpSession session = getHttpSession();
			if (session != null) {
				Integer contactId = (Integer)session.getAttribute(Constants.ATTR_CURRENT_CONTACT);
				if (contactId != null) {
					contact = ContactDAO.getInstance().findById(contactId);
				}
			}
			LOG.debug("contact#: " + (contact==null?"null":contact.getId()));
		}
		catch (Exception e) {
			LOG.error(e);
		}
		return contact;
	}

	public static void setCurrentContact(Contact contact) {
		HttpSession session = getHttpSession();
		if (contact == null) {
			session.removeAttribute(Constants.ATTR_CURRENT_CONTACT);
		}
		else {
			session.setAttribute(Constants.ATTR_CURRENT_CONTACT, contact.getId());
		}
		// clear current project, so it will get looked up on next request
		session.setAttribute(Constants.ATTR_CURRENT_PROJECT, null);
	}

	/**
	 * Setup session parameters, etc., to show that the given contact has
	 * "logged into" the production associations with that Contact, and also
	 * navigate the user to the "inside" Home page, or to the given URL that is
	 * "inside" a production. Returns null if the contact no longer has access
	 * to the production. ESS-1513.
	 *
	 * @param contact The Contact, representing the user and production, to be
	 *            entered into the production.
	 * @param url The URL to navigate to, if any. Ignored if null.
	 * @return null if access was not allowed. Returns an empty string if the
	 *         URL was already used for navigation. Otherwise returns the JSF
	 *         navigation string to get to the 'inner' home page for the
	 *         production.
	 */
	public static String logIntoProduction(Contact contact, String url) { // ESS-1513
		Production prod;
		if (contact != null) {
			// Clear session variables possibly shared by My Starts or My Timecards and in-production code.
			ContactFormBean.clearMyStarts();
			TimecardBase.clearSession();

			put(Constants.ATTR_SELECT_CONTACT_ID, contact.getId());
			prod = contact.getProduction(); // production still active?
			setCurrentContact(contact);
			SessionSetUtils.setProduction(prod);

			if ( ! LoginBean.checkProjectAccess(contact)) {
				setCurrentContact(null);
				SessionSetUtils.setProduction(ProductionDAO.getSystemProduction());
				return null;
			}
			// set attribute so PageAuthenticatePhaseListener lets us in!
			put(Constants.ATTR_ENTERING_PROD, 1);
			String ret = HeaderViewBean.getHomeNavigation(prod);
			if (url != null && url.length() > 0 && url.indexOf("user/") < 0 && url.indexOf("sys/") < 0
					&& url.indexOf("product/") < 0 && url.indexOf("logout") < 0) {
				if (HeaderViewBean.navigateToUrl(url)) {
					ret = "";
				}
			}
			return ret;
		}
		return null;
	}

	/**
	 * @return The Project that the user is currently signed into, represented
	 *         by the project name displayed at the top of every page. For a
	 *         Feature production, the Project returned is the one (and only)
	 *         project defined for each such production.
	 *         <p>
	 *         Null is returned if the user has not "entered" a production,
	 *         e.g., if they are on the My Productions or similar page. EXCEPT
	 *         that for "super-users", who are LS eps Admins of the SYSTEM
	 *         production, this will return the SYSTEM project, since such users
	 *         are essentially "entered" into the SYSTEM production as soon as
	 *         they login.
	 */
	public static Project getCurrentProject() {
		Project project = null;
		try {
			HttpSession session = getHttpSession();
			project = null;
			final ProjectDAO projectDAO = ProjectDAO.getInstance();
			Integer projectId = null;
			if (session != null) {
				projectId = (Integer)session.getAttribute(Constants.ATTR_CURRENT_PROJECT);
			}
			else { // no session, maybe running in JUnit or Quartz environment
				//projectId = getInstance().batchProjectId;
			}
			if (projectId != null) {
				project = projectDAO.findById(projectId);
			}
			if (project == null && getProduction() != null) {
				Contact contact = getCurrentContact();
				if (contact != null) {
					project = contact.getProject();
				}
				if (project == null) { // nothing yet?
					LOG.warn("No project available for Event log; probably running in scheduled task.");
//					Production production = getProduction();
//					Set<Project> projects = production.getProjects();
//					if (projects != null && projects.size() > 0) {
//						project = projects.iterator().next(); // just take the first one
//					}
//					log.warn("Used default project selection: " + project);
				}
				if (project != null && session != null) {
					session.setAttribute(Constants.ATTR_CURRENT_PROJECT,project.getId());
				}
			}
		}
		catch (Exception e) {
			LOG.error(e);
		}
		return project;
	}

	/**
	 * Set the current Project for this user. This value is then widely
	 * available via the {@link #getCurrentProject()} method.
	 *
	 * @param project The Project to be set as 'current'.
	 */
	public static void setCurrentProject(Project project) {
		LOG.debug("proj id=" + (project==null ? "null" : project.getId()));
		HttpSession session = getHttpSession();
		if (session != null) {
			session.removeAttribute(Constants.ATTR_TC_WEEK_END_DAY);
			if (project == null) {
				session.removeAttribute(Constants.ATTR_CURRENT_PROJECT);
			}
			else {
				session.setAttribute(Constants.ATTR_CURRENT_PROJECT, project.getId());
			}
		}
		else {
			//getInstance().batchProjectId = (project == null ? null : project.getId());
		}
		HeaderViewBean.reset();	// force refresh of header information
	}

	/**
	 * @return The Project that the user is currently working with. If the user
	 *         has entered a production, this returns the same as
	 *         {@link #getCurrentProject()}. If the user is not in a production,
	 *         e.g., they are in My Starts, My Timecards, or similar
	 *         environment, then this will attempt to determine the appropriate
	 *         project based on other session variables.
	 */
	public static Project getCurrentOrViewedProject() {
		Project project = null;
		try {
			project = getCurrentProject();
			if (project == null || project.getId() == 1) { // On 'outer' page (outside a Production) such as "My Starts", "My Timecards"
				// (project w/ id=1 is the "SYSTEM" project; ignore that one for this method. LS-1994
				Integer projId = SessionUtils.getInteger(Constants.ATTR_LAST_PROJECT_ID);
				if (projId != null) {
					project = ProjectDAO.getInstance().findById(projId);
				}
				if (project == null) {
					Integer id = getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
					if (id != null) {
						Production prod = ProductionDAO.getInstance().findById(id);
						project = prod.getDefaultProject();
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error(e);
		}
		return project;
	}

	/**
	 * Creates a SelectItem List for use in populating Project drop-down lists.
	 * @param contact The User whose project membership will determine the Project's
	 * included in the list.
	 * @param addAll If true, an "All" entry will be added at the beginning of
	 * the list.
	 * @return a non-null List
	 */
	public static List<SelectItem> createProjectList(Contact contact, boolean addAll) {
		Set<Project> projects = new TreeSet<>();
		// Using a TreeSet automatically sorts and ignores duplicates
		for (Employment emp : contact.getEmployments()) {
			for (ProjectMember mbr : emp.getProjectMembers()) {
				if (mbr.getUnit() == null) {
					projects.clear();
					projects.addAll(contact.getProduction().getProjects());
					break;
				}
				projects.add(mbr.getUnit().getProject());
			}
		}
		List<SelectItem> projectTitles = new ArrayList<>(projects.size());
		if (addAll) {
			projectTitles.add(new SelectItem(Constants.INTEGER_ZERO, "All"));
		}
		for (Project proj : projects) {
			if (proj.getStatus() != AccessStatus.OFFLINE) {
				projectTitles.add(new SelectItem(proj.getId(), proj.getTitle()));
			}
		}
		return projectTitles;
	}

	/**
	 * Returns the current Production object's database id.
	 */
	public static Integer getProductionId() {
		HttpSession session = getHttpSession();
		Integer productionId = null;
		if (session != null) {
			productionId = (Integer)session.getAttribute(Constants.ATTR_PRODUCTION);
		}
//		if (productionId == null) {
//			Production production = ProductionDAO.getInstance().findFirst();
//			if (production != null) {
//				productionId = production.getId();
//				if (session != null) {
//					session.setAttribute(Constants.ATTR_PRODUCTION, production.getId());
//				}
//			}
//		}
		return productionId;
	}

	/**
	 * @return the current Production object if it is not the SYSTEM production.
	 *         If the current production is the System production, returns null.
	 */
	public static Production getNonSystemProduction() {
		Production production = getProduction();
		if (production != null && production.isSystemProduction()) {
			production = null;
		}
		return production;
	}

	/**
	 * @return The Production that the user is currently working with. If the
	 *         user has entered a production, this returns the same as
	 *         {@link #getProduction()}. If the user is not in a production,
	 *         e.g., they are in My Starts, My Timecards, or similar
	 *         environment, then this will attempt to determine the appropriate
	 *         Production based on other session variables.
	 */
	public static Production getCurrentOrViewedProduction() {
		Production production = null;
		try {
			production = getNonSystemProduction();
			if (production == null) { // On 'outer' page (outside a Production) such as "My Starts", "My Timecards"
				Integer prodId = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
				if (prodId != null) {
					production = ProductionDAO.getInstance().findById(prodId);
				}
			}
		}
		catch (Exception e) {
			LOG.error(e);
		}
		return production;
	}

	/**
	 * @return the current Production object. This will usually be null if the
	 *         user has not "entered" a production, e.g., if they are on the My
	 *         Productions or similar page. Note, however, that "super-users" --
	 *         LS eps Admins -- "enter" the SYSTEM production upon login.
	 */
	public static Production getProduction() {
		Production production = null;
		Integer productionId = getProductionId();
		if (productionId != null) {
			production = ProductionDAO.getInstance().findById(productionId);
		}
//		if (production == null) {
//			production = ProductionDAO.getInstance().findFirst();
//			if (session != null) {
//				session.setAttribute(Constants.ATTR_PRODUCTION, production.getId());
//			}
//		}
		return production;
	}

	/**
	 * @return the Department mask for the current Production or current Project
	 *         (for Commercial productions).
	 */
	public static BitMask getDeptMask() {
		Production prod = getNonSystemProduction();
		return getDeptMask(prod, false);
	}

	/**
	 * Get the current applicable department mask, either from the current
	 * Production or the current Project.
	 *
	 * @param prod The production of interest.
	 * @param useProdOnly If true, the Production mask is always returned, even
	 *            for Commercial productions.
	 * @return the BitMask for the given Production or current Project (for
	 *         Commercial productions).
	 */
	public static BitMask getDeptMask(Production prod, boolean useProdOnly) {
		BitMask activeMask;
		if (prod != null) {
			activeMask = prod.getDeptMaskB();
			if ((! useProdOnly) && prod.getType().isAicp()) {
				activeMask = SessionUtils.getCurrentProject().getDeptMask();
			}
		}
		else {
			activeMask = new BitMask();
		}
		LOG.debug(activeMask);
		return activeMask;
	}

	/**
	 * Find the current HttpSession.
	 *
	 * @return The current HttpSession. Returns reference to our static
	 *         'sessionMap' if there is no current session, which happens if the
	 *         code is running under a scheduled (e.g., Quartz) task.
	 */
	public static HttpSession getHttpSession() {
		HttpSession session = null;
		if (noSession) {
			// explicitly running without session - used in Web Service
			session = sessionMap;
		}
		else if (getHttpRequest() != null) {
			session = getHttpRequest().getSession();
		}
		else {
			// possibly running from authorize.net post, or
			// from JUnit or Quartz (scheduled) task.
			session = sessionMap;
		}
		return session;
	}

	/**
	 * Get a parameter from the current Faces request parameter map.
	 *
	 * @param key The key within the request parameter map.
	 * @return The value found; null if no matching key exists.
	 */
	public static String getRequestParam(String key) {
		String param = null;
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			ExternalContext externalContext = facesContext.getExternalContext();
			Map<String, String> requestMap =
					externalContext.getRequestParameterMap();
			if (requestMap != null) {
				param = requestMap.get(key);
			}
		}
		return param;
	}

	/**
	 * Find the current HttpServletRequest.
	 *
	 * @return The current HttpServletRequest. Returns null if there is no current
	 *         request, which happens if the code is running under a scheduled
	 *         (e.g., Quartz) task.
	 */
	public static HttpServletRequest getHttpRequest() {
		HttpServletRequest request = null;
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			ExternalContext externalContext = facesContext.getExternalContext();
			if (externalContext != null) {
				request =(HttpServletRequest) externalContext.getRequest();
			}
		}
		return request;
	}

	/**
	 * @return the current phaseId of the current Faces Context, formatted for
	 *         inclusion in logging output.
	 */
	public static String getPhaseIdFmtd() {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			return " [" + facesContext.getCurrentPhaseId() + "] ";
		}
		return " [N/A] ";
	}

	/**
	 * Determine the real (OS) path, given a virtual path; supports both online
	 * (Faces) contexts as well as "batch" contexts (e.g., invoked via a
	 * TaskExecutor).
	 * <p>
	 * Set {@link #jUnitUnderEclipse} to true in JUnit test classes that use OS
	 * files, that is, when exercising code that calls this method. This
	 * applies, for example, to report-generation tests.
	 *
	 * @param path The virtual (relative) path of interest.
	 * @return The real, fully-qualified, path equivalent to the supplied
	 *         relative path.
	 */
	public static String getRealPath(String path) {
		boolean logError = true;
		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			if (facesContext == null) {
				ApplicationContext ac;
				Resource res;
				File f;
				ac = ServiceFinder.getBatchApplicationContext();
				if (ac != null) {
					if (path.contains(":") ||
							path.startsWith(".") ||
							path.startsWith("\\") ||
							path.startsWith("/") ) {
						// no change if drive reference or absolute path notation,
						// or '.', only used by JUnit test cases.
					}
					else {
						// jUnitUnderEclipse should have been set to true ONLY if running
						// JUnit tests under Eclipse.
						// It MUST BE FALSE for PRODUCTION!! (Scheduled tasks will break otherwise.)
						// 'false' also works for JUnit tests run from the command line.
						if (jUnitUnderEclipse) {
							// ** JUNIT UNDER ECLIPSE CODE **
							logError = false;	// do not log errors
							LOG.info("***** JUNIT-UNDER-ECLISE MODE IS TURNED ON *****");
							// In particular, the following allows JasperReport calls to work!
							path = "WebRoot\\" + path;
						}
					}
					res = ac.getResource(path);
					if (res != null) {
						f = res.getFile();
						if (f != null) {
							path = f.getAbsolutePath();
						}
					}
				}
				//log.debug("res=" + res + ", f=" + f + ", path=" + path);
			}
			else {
				ExternalContext externalContext = null;
				externalContext = facesContext.getExternalContext();
				ServletContext sc = (ServletContext)externalContext.getContext();
				path = sc.getRealPath(path);
			}
		}
		catch (IOException e) {
			if (logError) {
				EventUtils.logError(e);
			}
			else {
				LOG.warn("real path determination failed; may be irrelevant for JUnit test: " + e.getLocalizedMessage());
			}
		}
		return path;
	}

	/**
	 * @return The real OS-dependent file path to our standard report output folder,
	 * that is, ...webapps/<instance>/report/.
	 */
	public static String getRealReportPath() {
		if (realReportPath == null) {
			realReportPath = getRealPath(Constants.REPORT_FOLDER) + File.separator;
		}
		return realReportPath;
	}

	/** See {@link #jUnitUnderEclipse}. */
	public static void setjUnitUnderEclipse(boolean jUnitUnderEclipse) {
		SessionUtils.jUnitUnderEclipse = jUnitUnderEclipse;
	}

	/**
	 * Returns True if the user is on a mobile device - either smart phone or tablet.
	 */
	public static boolean isMobileUser() {
		return isPhoneUser() || isTabletUser();
	}

	/**
	 * Returns True if the user is on a mobile smart-phone (not tablet) device.
	 */
	public static boolean isPhoneUser() {
		return getBoolean(Constants.ATTR_USER_PHONE, false);
	}

	/**
	 * Returns True if the user is on a tablet device.
	 */
	public static boolean isTabletUser() {
		return getBoolean(Constants.ATTR_USER_TABLET, false);
	}

	public static String mobilize(String string) {
		if (isMobileUser()) {
			return string + "m";
		}
		return string;
	}

	/** Returns true if this coming from the ttconline domain */
	public static boolean isTTCOnline() {
		return getBoolean(Constants.ATTR_IS_TTCONLINE_DOMAIN, false);
	}

	/** Returns true if the current production is a Team (TTC) payroll client. */
	public static boolean isTTCProd() {
		return getBoolean(Constants.ATTR_IS_TTC_PROD, false);
	}

	/**
	 * Determine if the given user should be given access to the "ESS"
	 * (Self-Service) menu. This is based on whether the user is "likely" to
	 * have been, or be, paid by Team. I.e., is an employee of a Team "client",
	 * or is registered on ESS already.
	 *
	 * @param user The User to be checked.
	 * @return True if the user should be shown the 'Self-Service' menu (access
	 *         to ESS).
	 */
	public static boolean isShownEssMenu(User user) {
		Boolean bRet = getBoolean(Constants.ATTR_USER_SHOWN_ESS_MENU);
		if (bRet == null) { // not yet "calculated"
			bRet = user.getShowUS() && (isTTCOnline() || isTTCProd()); // Canadian users don't qualify here
			if (! bRet) {
				bRet = (!user.getTeamEmployee() && user.getAgreeToTerms()) ||
						Constants.SOURCE_SYSTEM_ESS.equals(user.getSourceSystem());
				// NOTE: UDA Calc users have 'agreeToTerms' set True (manually) even though they are not ESS-validated.
				if (! bRet) { // See if the user belongs to any Production using the Team payroll service
					Map<String, Object> values = new HashMap<>();
					values.put("user", user);
					bRet = ProductionDAO.getInstance().findBooleanByNamedQuery(Production.HAS_PRODUCTION_BY_USER_AND_IS_TEAM_CLIENT, values);
				}
			}
			put(Constants.ATTR_USER_SHOWN_ESS_MENU, bRet); // save in session for next time
		}
		return bRet.booleanValue();
	}

	/**
	 * Returns True if this application instance was initialized to only
	 * serve mobile devices (not currently used).
	 */
	public static boolean isMobileApp() {
		return false;
//		return ApplicationScopeBean.getInitParameterBoolean(Constants.INIT_PARAM_IS_MOBILE);
	}

	/**
	 * Store a session variable.
	 * @param varName The name to use for storing (and retrieving) the value.
	 * @param value The value to be stored; if value is null, the session variable is removed.
	 */
	public static void put(String varName, Object value) {
		LOG.debug(varName + "=" + value);
		if (value == null) {
			getHttpSession().removeAttribute(varName);
		}
		else {
			getHttpSession().setAttribute(varName, value);
		}
	}

	/**
	 * Return a Session variable as a String.
	 * @param varName The name used to store the variable in the session.
	 * @return The string value; may be null.
	 */
	public static String getString(String varName) {
		return (String)getHttpSession().getAttribute(varName);
	}


	/**
	 * Return a Session variable as a Date.
	 * @param varName The name used to store the variable in the session.
	 * @return The Date value; may be null.
	 */
	public static Date getDate(String varName) {
		return (Date)getHttpSession().getAttribute(varName);
	}

	/**
	 * Return a Session variable as an Integer. The actual session variable may
	 * be an Integer, a Long, or a String, as long as it can be safely converted
	 * to an Integer. This helps for session values stored from JSP, where
	 * integral numeric literals are treated as Long instead of Integer.
	 *
	 * @param varName The name used to store the variable in the session.
	 * @return The Integer value; may be null.
	 */
	public static Integer getInteger(String varName) {
		Object var;
		try {
			var = getHttpSession().getAttribute(varName);
		}
		catch (Exception e1) {
			return null; // running in batch/JUnit mode
		}
		Integer ret = null;
		if (var instanceof Integer) {
			ret = (Integer)var;
		}
		else if (var instanceof Long) {
			ret = ((Long)var).intValue();
		}
		else if (var instanceof String) {
			try {
				ret = Integer.parseInt((String)var);
			}
			catch (NumberFormatException e) {
			}
		}
		return ret;
	}

	/**
	 * Return a Session variable as an Boolean.
	 * @param varName The name used to store the variable in the session.
	 * @return The Boolean value; may be null.
	 */
	public static Boolean getBoolean(String varName) {
		return (Boolean)getHttpSession().getAttribute(varName);
	}

	/**
	 * Return a Session variable as an Object.
	 * @param varName The name used to store the variable in the session.
	 * @return The value; may be null.
	 */
	public static Object get(String varName) {
		return getHttpSession().getAttribute(varName);
	}

	/**
	 * Return a Session variable as an Object, or the supplied default value if
	 * not found.
	 *
	 * @param varName The name used to store the variable in the session.
	 * @return The value; may be null.
	 */
	public static Object get(String varName, Object defaultValue) {
		Object value = getHttpSession().getAttribute(varName);
		if (value == null) {
			LOG.debug(varName + "=" + defaultValue);
			return defaultValue;
		}
		LOG.debug(varName + "=" + value);
		return value;
	}

	/**
	 * Return a Session variable as a String, or the supplied default value if not found.
	 *
	 * @param varName The name used to store the variable in the session.
	 * @param defaultValue The default value of the variable.
	 * @return The String value found in the Session, or the 'defaultValue' if the variable was not
	 *         in the Session.
	 */
	public static String getString(String varName, String defaultValue) {
		String value = getString(varName);
		if (value == null) {
			LOG.debug(varName + "=" + defaultValue);
			return defaultValue;
		}
		LOG.debug(varName + "=" + value);
		return value;
	}

	/**
	 * Return a Session variable as an Integer, or the supplied default value if not found.
	 *
	 * @param varName The name used to store the variable in the session.
	 * @param defaultValue The default value of the variable.
	 * @return The Integer value found in the Session, or the 'defaultValue' if the variable was not
	 *         in the Session.
	 */
	public static Integer getInteger(String varName, Integer defaultValue) {
		Integer value = getInteger(varName);
		if (value == null) {
			LOG.debug(varName + "=" + defaultValue);
			return defaultValue;
		}
		LOG.debug(varName + "=" + value);
		return value;
	}

	/**
	 * Return a Session variable as a Boolean, or the supplied default value if not found.
	 *
	 * @param varName The name used to store the variable in the session.
	 * @param defaultValue The default value of the variable.
	 * @return The Boolean value found in the Session, or the 'defaultValue' if the variable was not
	 *         in the Session.
	 */
	public static boolean getBoolean(String varName, boolean defaultValue) {
		Boolean value = getBoolean(varName);
		if (value == null) {
			LOG.debug(varName + "=" + defaultValue);
			return defaultValue;
		}
		LOG.debug(varName + "=" + value);
		return value;
	}

	public static String getClientIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for"); // Apache or mod_jk seems to set this
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	/** See {@link #noSession}. */
	public static boolean getNoSession() {
		return noSession;
	}
	/** See {@link #noSession}. */
	public static void setNoSession(boolean noSession) {
		SessionUtils.noSession = noSession;
	}

}
