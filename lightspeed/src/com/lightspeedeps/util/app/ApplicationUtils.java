// File Name: ApplicationUtils.java
package com.lightspeedeps.util.app;

import java.util.*;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * A utility providing data that is of application scope, and which can be
 * obtained statically. Information that requires the current context, or cannot
 * be static because it is called from JSF, is provided in ApplicationScopeBean.
 * <p>
 * Primarily this provides access to initialization parameters from web.xml.
 */
public class ApplicationUtils {

	private static final Log LOG = LogFactory.getLog(ApplicationUtils.class);

	// Fields

	private static final String CONTEXT_PREFIX = "com.lightspeedeps.model.";

	/** The base URL for the ESS application; used for redirecting ESS menu items, emailed
	 * registration links, etc.  Part of "seamless integration" between TTCO and ESS. ESS-1364 */
	private static String essBaseUrl;

	/** Set when we're running under a non-Faces request, e.g., Authorize.net or
	 * incoming SMS. */
	private static ServletContext requestContext;

	/** The time zone used for presenting (and entering) dates & times. */
	private static TimeZone timeZone = null; // "America/Los_Angeles"

	/** True if Daylight Savings Time is in effect. */
	private static Boolean dst = null;

	/** The prefix for User account #'s created on this instance. */
	private static String accountPrefix;

	/** The prefix for Production id strings created on this instance. */
	private static String productionPrefix;

	/** True if we are running in offline mode (i.e., on a local PC, not a server). */
	private static Boolean offline = null;

	/** If True, we are running without a context/session.  Typically set by JUnit
	 * tests.  When true, a static HashMap is used to return initialization parameters. */
	private static boolean noContext;

	/** A Map to simulate initialization parameters when no context exists. */
	private static Map<String, String> pseudoContextMap = new HashMap<>();

	/** The URL to the current application instance; may contain "*" to be replaced by the
	 * domain from which the current user logged in. */
	private static String secureBase;

	/** The application instance name, e.g., "ls" or "betawin".  Extracted from
	 * the secure base URL, includes all text following the last slash. */
	private static String instancePath;

	/** Character that pose a security and what to replace them with. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Map<CharSequence, CharSequence> riskReplacementChars = MapUtils.putAll(new HashMap(), new CharSequence [][] {
			{"<",  ">"}
	});
	// FF4J context-params from web.xml
	/** Host Name */
	public static final String LDAP_HOSTNAME_PARAM = "LDAP_HOSTNAME";

	/** Port Number */
	public static final String LDAP_PORT_PARAM = "LDAP_PORT";

	/** User Name */
	public static final String LDAP_USERNAME_PARAM  = "LDAP_USERNAME";

	/** User Password */
	public static final String LDAP_PASSWORD_PARAM = "LDAP_PASSWORD";

	/** Initial LDAP connections in pool */
	public static final String LDAP_INITIAL_POOL_SIZE_PARAM = "LDAP_INITIAL_POOL_SIZE";

	/** Max LDAP connections in pool */
	public static final String  LDAP_MAX_POOL_SIZE_PARAM = "LDAP_MAX_POOL_SIZE";

	/** Feature Flag feature/property store URL */
	public static final String FF_STORE_URL_PARAM = "FF_STORE_URL";

	/* Constructor */
	private ApplicationUtils() {
	}

	/**
	 * Look up a String context initialization parameter.  The "name" is automatically
	 * qualified with "com.lightspeedeps.model" to prevent any ambiguity.
	 * @param name The name to lookup, as "com.lightspeedeps.model.<name>".
	 * @return The String value found from the context initial parameters.
	 */
	public static String getInitParameterString(String name) {
		return getInitParameterString(name, true);
	}

	/**
	 * Look up a String context initialization parameter.  The "name" is automatically
	 * qualified with "com.lightspeedeps.model" to prevent any ambiguity.
	 * @param name The name to lookup, as "com.lightspeedeps.model.<name>".
	 * @param useContextName Determines whether to use context prefix. LS-2336
	 * @return The String value found from the context initial parameters.
	 */
	public static String getInitParameterString(String name, boolean useContextName) {
		String param = null;

		if(useContextName) {
			// LS-2336
			name = CONTEXT_PREFIX + name;
		}
		try {
			FacesContext context= FacesContext.getCurrentInstance();
			if (context == null) {
				if (requestContext != null) {
					param = requestContext.getInitParameter(name);
					LOG.debug(name + " param value via requestContext: " + param);
				}
				else if (noContext) {
					param = pseudoContextMap.get(name);
				}
				if (param == null && name.contains("timeZone")) {
					param = "PST"; // handle JUnit testing - needs timezone for some tests.
				}
				if (param == null) {
					EventUtils.logError("web.xml context parameter lookup failed for '" + name + "' -- no context (scheduled task?).");
				}
			}
			else {
				ServletContext servletContext = (ServletContext)context.getExternalContext().getContext();
				param = servletContext.getInitParameter(name);
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError("Error finding web.xml initialization parameter `"+name+"`", e);
		}
		LOG.debug("name="+name+", param="+param);
		return param;
	}

	/**
	 * Look up an integer context initialization parameter.  The "name" is automatically
	 * qualified with "com.lightspeedeps.model" to prevent any ambiguity.
	 * @param name The name to lookup, as "com.lightspeedeps.model.<name>".
	 * @return The 'int' value found from the context initial parameters.
	 */
	public static int getInitParameterInt(String name) {
		int param = 0;
		String str = getInitParameterString(name);
		try {
			param = Integer.parseInt(str);
		}
		catch (RuntimeException e) {
			EventUtils.logError("Error converting parameter "+CONTEXT_PREFIX+name+"=`"+str+"` to int.", e);
		}
		return param;
	}

	/**
	 * Look up a boolean context initialization parameter.  The "name" is automatically
	 * qualified with "com.lightspeedeps.model" to prevent any ambiguity.
	 * @param name The name to lookup, as "com.lightspeedeps.model.<name>".
	 * @return The boolean value found from the context initial parameters.
	 */
	public static boolean getInitParameterBoolean(String name, boolean def) {
		boolean param = def;
		String str = getInitParameterString(name);
		if (str != null) {
			try {
				param = Boolean.parseBoolean(str);
			}
			catch (RuntimeException e) {
				EventUtils.logError("Error converting parameter "+CONTEXT_PREFIX+name+"=`"+str+"` to Boolean.", e);
			}
		}
		return param;
	}

	/**
	 * Store an "initialization" parameter, when testing, for later retrieval by
	 * 'normal' (non-test) code.
	 *
	 * @param key The initialization parameter key -- this is the suffix only,
	 *            without the leading qualifier that is automatically prepended
	 *            by the getter methods.
	 * @param obj The object to be stored at that key.
	 */
	public static void setInitializationParameter(String key, Object obj) {
		pseudoContextMap.put(CONTEXT_PREFIX + key, obj.toString());
	}

	/**
	 * Store an "initialization" parameter, when testing, for later retrieval by
	 * 'normal' (non-test) code. The parameter is NOT prefixed with our default qualifier.
	 *
	 * @param key The initialization parameter key -- this is the complete key.
	 * @param obj The object to be stored at that key.
	 */
	public static void setInitializationParameterNoprefix(String key, Object obj) {
		pseudoContextMap.put(key, obj.toString());
	}

	/** See {@link #productionPrefix}. */
	public static String getProductionPrefix() {
		if (productionPrefix == null) {
			productionPrefix = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PRODUCTION_PREFIX);
			if (productionPrefix == null) {
				return "P";
			}
		}
		return productionPrefix;
	}

	/** See {@link #accountPrefix}. */
	public static String getAccountPrefix() {
		if (accountPrefix == null) {
			accountPrefix = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_ACCOUNT_PREFIX);
			if (accountPrefix == null) {
				return "AP";
			}
		}
		return accountPrefix;
	}

	/**
	 * Generates an account number based on the integer value given.
	 *
	 * @param id A non-null positive value.
	 * @return A String that starts with this instance's "account prefix",
	 *         followed by a 4-digit (or longer) number which is the integer
	 *         supplied, left-padded with zeroes (if necessary) to be at least 4
	 *         digits long.
	 */
	public static String createAccountNumber(Integer id) {
		String acct = id.toString();
		if (acct.length() < 4) {
			acct = "0000".substring(acct.length()) + acct;
		}
		acct = getAccountPrefix() + acct;
		return acct;
	}

	private static int sequenceNumber = 1;
	private static Date sequenceDate = null;


	/**
	 * Get the next available sequence number for filenames or other uses. The
	 * number is guaranteed unique only within the current day!
	 *
	 * @return The new sequence number.
	 */
	public static int findNextSequenceNumber() {
		if (sequenceDate == null) {
			sequenceDate = CalendarUtils.todaysDate();
		}
		else {
			Date date = CalendarUtils.todaysDate();
			if (date.equals(sequenceDate)) {
				sequenceNumber++;
				if (sequenceNumber > 599) {
					// Some FTP processes stop looking at 600.
					sequenceNumber = 1;
				}
			}
			else { // new day, reset sequence number
				sequenceDate = date;
				sequenceNumber = 1;
			}
		}
		return sequenceNumber;
	}

	public static TimeZone getTimeZoneStatic() {
		if (timeZone == null) {
			String tz = getInitParameterString(Constants.INIT_PARAM_TIME_ZONE);
			LOG.debug("tz string from init Parameter: " + tz);
			timeZone = TimeZone.getTimeZone(tz);
			if (timeZone == null) {
				Production prod = SessionUtils.getProduction();
				timeZone = prod.getTimeZone();
				LOG.debug("tz from Production: " + timeZone);
			}
			LOG.debug("timezone found: " + timeZone);
		}
		return timeZone;
	}

	public static boolean getDst() {
		if (dst == null) {
			dst = getInitParameterBoolean(Constants.INIT_PARAM_DST, false);
		}
		return dst;
	}

	/** See {@link #offline}. */
	public static Boolean isOffline() {
		if (offline == null) {
			offline = false; // getInitParameterBoolean(Constants.INIT_PARAM_IS_OFFLINE, false);

			// The "Offline" version is not available!

			/* There was a situation in which this method is called while running under
			 * a scheduled task, which will not have a Faces context.  It's called from
			 * DoNotification, when DueOverdueCheck is sending a notification, and the
			 * code checks to see if the system is online.  Since the DueOverdue task is
			 * never scheduled in offline mode, we must be online, so we default this
			 * parameter to "false" if we don't find a Context.
			 */
		}
		return offline;
	}
	/** See {@link #offline}. */
	public static void setOffline(Boolean offline) {
		ApplicationUtils.offline = offline;
	}

	/** See {@link #noContext}. */
	public static boolean getNoContext() {
		return noContext;
	}
	/** See {@link #noContext}. */
	public static void setNoContext(boolean noCtx) {
		noContext = noCtx;
	}

	/**
	 * @return The URL that corresponds to our context root, using the secure
	 * http protocol, assuming it is specified in the web.xml parameter.
	 * The web xml paramater contains a '*' that will be replaced with the
	 * proper domain. Either the lightspeed or ttconline doman.
	 */
	public static String getSecureBaseUrl() {
		if (secureBase == null) {
			secureBase = getInitParameterString(Constants.INIT_PARAM_SECURE_BASE_URL);
		}
		// Since this may be coming from ls or ttc online, we need to use the domain
		// that is stored in the session.
		String base = secureBase.replace("*", SessionUtils.getString(Constants.ATTR_HTTP_REQUEST_DOMAIN));

		return base;
	}

	/**
	 * @return The base URL (e.g., https://ess.ttconline.com) for the ESS
	 *         application. ESS-1364
	 */
	public static String getEssBaseUrl() {
		if (essBaseUrl == null) {
			essBaseUrl =  ApplicationUtils.getInitParameterString("ESS_URL", false);
		}
		return essBaseUrl;
	}

	public static String getInstancePath() {
		if (instancePath == null) {
			String s = getSecureBaseUrl();
			String parts[] = s.split("/");
			instancePath = parts[parts.length-1];
			LOG.debug("instance path=" + instancePath);
		}
		return instancePath;
	}

	/**
	 * LS-2336
	 * Pull the Feature Flag/LDAP parameters from web.xml.
	 * These will be used to verify the currently logged user
	 * against the active directory. Will also setup the
	 * FF4J feature and property stores.
	 * @return
	 */
	public static Map<String, String> getFf4jParams() {
		Map<String, String> params = new HashMap<>();
		try {
			params.put(LDAP_HOSTNAME_PARAM, getInitParameterString(LDAP_HOSTNAME_PARAM, false));
			params.put(LDAP_PORT_PARAM, getInitParameterString(LDAP_PORT_PARAM, false));
			params.put(LDAP_USERNAME_PARAM, getInitParameterString(LDAP_USERNAME_PARAM, false));
			params.put(LDAP_PASSWORD_PARAM, getInitParameterString(LDAP_PASSWORD_PARAM, false));
			params.put(LDAP_INITIAL_POOL_SIZE_PARAM, getInitParameterString(LDAP_INITIAL_POOL_SIZE_PARAM, false));
			params.put(LDAP_MAX_POOL_SIZE_PARAM, getInitParameterString(LDAP_MAX_POOL_SIZE_PARAM, false));

		}
		catch(RuntimeException ex) {
			EventUtils.logError(ex);
			LOG.debug(ex);
		}

		return params;
	}
	/** See {@link #requestContext}. */
	public static ServletContext getRequestContext() {
		return requestContext;
	}
	/** See {@link #requestContext}. */
	public static void setRequestContext(ServletContext requestContext) {
		ApplicationUtils.requestContext = requestContext;
	}

	/**
	 * Display icefaces request parameters. Used for debug purposes.
	 *
	 * @param paramPrefix - to search for parameters containing the passed in prefix.
	 * 						If null, search for all parameters.
	 */
	public static void dumpParams(String paramPrefix) {
		try {
			if(paramPrefix == null) {
				paramPrefix = "";
			}

			if (LOG.isDebugEnabled()) {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				Map<String,String> requestMap = facesContext.getExternalContext().getRequestParameterMap();

				if (requestMap != null) {
					for (String k : requestMap.keySet()) {
						if (k != null && k.startsWith(paramPrefix)) {
							String val = requestMap.get(k);
							LOG.debug("key=" + k + ", value=" + val);
						}
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error("error: ", e);
		}
	}

	/**
	 * Replace characters that could pose a security risk with innocuous characters.
	 * Ex. H<script>alert('')</script> would be replaced with H>script>alert('')>script>
	 *
	 * @param potentialRisk String containing possible security risk.
	 *
	 * @return String with characters posing a security risk removed.
	 */
	public static String fixSecurityRiskForStrings(String potentialRisk) {
		for(Entry<CharSequence, CharSequence> entry : riskReplacementChars.entrySet()) {
			potentialRisk = potentialRisk.replace(entry.getKey(), entry.getValue());
		}

		return potentialRisk;
	}
}
