// File Name: ApplicationScopeBean.java
package com.lightspeedeps.web.util;

import java.net.URI;
import java.security.Security;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import org.quartz.Scheduler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.lightspeedeps.dao.*;
import com.lightspeedeps.dto.ApplicationReleaseDTO;
import com.lightspeedeps.model.Country;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.State;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.service.JWTService;
import com.lightspeedeps.service.JWTServiceImpl;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.popup.ReleaseNotesBean;
import com.pdftron.pdf.PDFNet;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

/**
 * A singleton bean with application scope, which:
 * <ul>
 * <li>logs the startup and current version in the Event log.
 * <li>initializes, and gives access to, some 'static' data retrieved from the
 * database at startup, such as country and state drop-down lists.
 * <li>clears any locks that might have been left after a "hard" crash.
 * </ul>
 * This bean gets instantiated during application initialization simply as a
 * result of being in the applicationContext beans list as a singleton (the default).
 * <p>
 * Note that JSF references will only resolve to non-static methods.
 */
public class ApplicationScopeBean {

	private static final Log LOG = LogFactory.getLog(ApplicationScopeBean.class);

	// Fields

	/** Application-wide Map of List's of states, keyed by country ISO code, for drop-down lists.
	 * The 'label' field (displayed) is the full state name, and
	 * the 'value' field in each SelectItem is the state code, as defined by the
	 * corresponding country's postal service.  Note that for the US, there are multiple
	 * entries for one (or more) of the APO/FPO postal codes, so the state code is not unique. */
	private Map<String, List<SelectItem>> stateDL;

	/**
	 * Application-wide Map of List's of states, keyed by country ISO code, for
	 * drop-down lists. The 'label' field (displayed) is the state code, and the
	 * 'value' field in each SelectItem is the state code, as defined by the
	 * corresponding country's postal service. Note that for the US, there are
	 * multiple entries for one (or more) of the APO/FPO postal codes, so the
	 * state code is not unique. Each list of states will begin with the
	 * "blank state" SelectItem defined in Constants.BLANK_STATE_ITEM. If a
	 * country does not have a list of states in our database, no entry will
	 * exist in this Map for that country code.
	 */
	private Map<String, List<SelectItem>> stateCodeDL;

	/** The list of state code`s for the user to select from; different for
	 * "touring/hybrid" timecards using standard Day types (not touring types).  LS-1491 */
	private List<SelectItem> stateCodeHybridDL;

	/** Application-wide List of Country's for drop-down lists. The 'value' field
	 * in each SelectItem is the country's ISO code. */
	private List<SelectItem> countryDL;

	/** Application-wide List of Country's codes for drop-down lists. The 'value' field
	 * and label in each SelectItem is the country's ISO code. */
	private List<SelectItem> countryCodeDL;

	/** Our version (release) string, initialized via bean property in XML.  This
	 * is displayed on the Home page and the Prod Admin | Productions page. */
	private String version = "N/A";

	/** True if this is running in a "Beta" (non-production) mode. This may
	 * influence features, such as not allowing self-created User accounts. The
	 * value is set in the bean properties in XML (applicationContext.xml);
	 * currently that XML pulls the value from the web.xml file. */
	private boolean isBeta;

	/** License key for PDFTron package, passed to PDFNet initialization method. */
	private String pdfNetKey;

	/** True if PDFTron "XOD" data should be returned to the WebViewer;
	 * false if the original PDF data should be used. */
	private boolean useXod = false;

	/** True iff running in a JUnit environment. This is set via an XML property
	 * in applicationContextHibernate, which is the context used for JUnit testing.
	 * The setting allows us to skip some of the startup processing that is
	 * inappropriate for JUnit tests. */
	private boolean junit = false;

	/** A global shared authorization access token, created on the first request. */
	private JSONObject accessToken;

	/** The date/time at which the current authorization access token will expire. */
	Calendar accessTokenExpires = Calendar.getInstance();

	/** The date/time at which we last had a failure trying to get an access token.  Used to
	 * prevent repeated failing attempts.  LS-2458 */
	Calendar accessFailureTime = null;

	/** Latest release date to display in update message when user login */
	private String latesReleaseDate;

	/** The Quartz scheduler bean; used during shutdown.
	 * Set via XML bean property in applicationContextVersion.xml */
	private Scheduler quartzScheduler;

	// The DAOs are all set via XML bean properties in applicationContextVersion.xml
	private transient ContactDocumentDAO contactDocumentDAO;
	private transient CountryDAO countryDAO;
	private transient EventDAO eventDAO;
	private transient ProjectDAO projectDAO;
	private transient StartFormDAO startFormDAO;
	private transient StateDAO stateDAO;
	private transient WeeklyTimecardDAO weeklyTimecardDAO;

	/* Constructor */
	public ApplicationScopeBean() {
		LOG.debug("");
	}

	public static ApplicationScopeBean getInstance() {
		return (ApplicationScopeBean)ServiceFinder.findBean("applicationScopeBean");
	}

	/**
	 * The '@PostConstruct' annotation causes this method to be called by the
	 * framework after our bean is fully constructed and all properties (such as
	 * our DAO's) have been set.
	 * <p> NOTE: Any DAO's used in this method must be set via the bean definition XML,
	 * currently in applicationContextVersion.xml.  Using the getInstance() methods
	 * will generally result in an IllegalArgumentException.
	 */
	@PostConstruct
	protected void init() {
		LOG.debug("");
		try {
			Security.addProvider(new BouncyCastleProvider());
			LOG.info("Security provider initialized.");
			//SecurityProviderList.listSecurityProviders();
		}
		catch (Exception e) {
			LOG.error("Security initialization failed.");
			LOG.error(e);
		}

		try {
			PDFNet.initialize(pdfNetKey); // license key
			LOG.info("PDFNet initialized.");
		}
		catch (Exception | UnsatisfiedLinkError e) {
			LOG.error("Unable to initialize PDFNet library -- WebViewer and related functions will fail.");
			LOG.error(e);
		}

		try {
			if (! junit) {
				 // add startup entry to Event log with version and "beta" indicator
				EventUtils.logStartup(getEventDAO(), version + (getIsBeta() ? " (BETA)" : " (PROD)"));
				initStateAndCountry(); // load country & state drop-down lists
				getContactDocumentDAO().unlockAllLocked();	// clear any left-over ContactDocument locks
				getProjectDAO().unlockAllLocked(); 			// clear any left-over stripboard locks
				getStartFormDAO().unlockAllLocked();		// clear any left-over Start Form locks
				getWeeklyTimecardDAO().unlockAllLocked();	// clear any left-over timecard locks
			}
		}
		catch (Exception e) {
			LOG.error(e);
		}
	}

	private void initStateAndCountry() {
		try {
			LOG.debug("");
			stateDL = new HashMap<>();
			stateCodeDL = new HashMap<>();
			countryDL = new ArrayList<>();
			countryCodeDL = new ArrayList<>();
			List<State> stateList;
			List<Country> countryList;
			List<SelectItem> selList;
			List<SelectItem> selCodeList;
			countryList = getCountryDAO().findAll();
			if ((countryList != null) && (countryList.size() > 0)) {
				for (Country c : countryList) {
					stateList = getStateDAO().findByCountryCode(c.getIsoCode());
					if (stateList.size() > 0) {
						selList = new ArrayList<>();
						selList.add(new SelectItem("  ", Constants.SELECT_HEAD_STATE));
						selCodeList = new ArrayList<>();
						selCodeList.add(Constants.BLANK_STATE_ITEM);
						for (State st : stateList) {
							selList.add(new SelectItem(st.getCode(), st.getName()));
							selCodeList.add(new SelectItem(st.getCode(), st.getCode()));
						}
						stateDL.put(c.getIsoCode(), selList);
						stateCodeDL.put(c.getIsoCode(), selCodeList);
						//log.debug("country code " + c.getIsoCode() + " has " + (selList.size()-1) + " states");
					}
					// Add country entry to drop-down List for country selection.
					countryDL.add(new SelectItem(c.getIsoCode(), c.getName()));
					// Add the country code to drop-down list for country code selection
					countryCodeDL.add(new SelectItem(c.getIsoCode(), c.getIsoCode()));
				}
			}
			createStateCodeHybridDL();
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
	}

	/** for use in JSP when we need a checkbox that's always false (un-checked) */
	public boolean getUnchecked() {
		return false;
	}

	/** for use in JSP when we need a checkbox that's always true (checked) */
	public boolean getChecked() {
		return true;
	}

	/** See {@link #stateDL}. */
	public Map<String,List<SelectItem>> getStateDL() {
		return stateDL;
	}

	/** See {@link #stateCodeDL}. */
	public Map<String, List<SelectItem>> getStateCodeDL() {
		return stateCodeDL;
	}

	/** See {@link #stateCodeDL}. */
	public List<SelectItem> getStateCodeHybridDL() {
		return stateCodeHybridDL;
	}

	/** See {@link #stateCodeDL}. */
	public List<SelectItem> getStateCodeProdDL() {
		return getStateCodeDL(SessionUtils.getProduction());
	}

	/** See {@link #stateCodeDL}. */
	public List<SelectItem> getStateCodeWorkedDL(Production prod) {
		List<SelectItem> list = getStateCodeDL(prod);
		list =  new ArrayList<>(list); // make a copy so we can update it
		list.set(0, new SelectItem(Constants.STATE_WORKED_CODE, Constants.STATE_WORKED_LABEL));
		return list;
	}

	/**
	 * Get a list of states for a production.
	 *
	 * @param prod The production whose state list is wanted. The production's
	 *            country is retrieved from the production's address.
	 * @return A non-empty SelectItem list of states for the production's
	 *         country. If no matching list is found, a minimal list of a blank
	 *         item and the "FO" (foreign) entry is returned.
	 * @see {@link #stateCodeDL}.
	 */
	private List<SelectItem> getStateCodeDL(Production prod) {
		String country;
		if (prod != null) {
			country = prod.getAddress().getCountry();
		}
		else { // Use the 'default' country (typically US)
			country = Constants.DEFAULT_COUNTRY_CODE;
		}
		return getStateCodeDL(country);
	}

	/**
	 * Get a list of states for a country code.
	 *
	 * @param country The 2-letter country code whose state list is wanted.
	 * @return A non-empty SelectItem list of states for the specified country.
	 *         If no matching list is found, a minimal list of a blank item and
	 *         the "FO" (foreign) entry is returned.
	 * @see {@link #stateCodeDL}.
	 */
	public List<SelectItem> getStateCodeDL(String country) {
		List<SelectItem> list = getStateCodeDL().get(country);
		if (list == null) {
			list = new ArrayList<>();
			list.add(Constants.BLANK_STATE_ITEM);
			list.add(Constants.FOREIGN_OT_STATE_ITEM);
		}
		return Collections.unmodifiableList(list); // Force run-time error if caller modifies list. LS-2497
	}

	/**
	 * Create the list of state codes used for "hybrid" productions.  LS-2341
	 */
	private void createStateCodeHybridDL() {
		stateCodeHybridDL = new ArrayList<>(getStateCodeDL(Constants.DEFAULT_COUNTRY_CODE));
		Iterator<SelectItem> iter = stateCodeHybridDL.iterator();
		for (; iter.hasNext();) {
			SelectItem item = iter.next();
			if (item.getLabel().equals(Constants.FOREIGN_FO_STATE)) {
				// remove "FO" for hybrid productions, as country can be specified
				iter.remove();
				// LS-4219 "HM" is no longer in the database state list.
			}
		}
		//stateCodeHybridDL.add(Constants.FOREIGN_OT_STATE_ITEM); // LS-2497 "OT" no longer wanted in US/Hybrid state list
	}

	/** See {@link #countryDL}. */
	public List<SelectItem> getCountryDL() {
		return countryDL;
	}

	/** See {@link #CountryCodeDL}. */
	public List<SelectItem> getCountryCodeDL() {
		return countryCodeDL;
	}

	/**
	 * Returns the timeZone to be used for entering and displaying dates and
	 * times. This is the same as getTimeZoneStatic(), but a non-static method
	 * is required for use in JSF pages.
	 *
	 * @return The desired timeZone, usually defined on a production-wide basis.
	 */
	public TimeZone getTimeZone() {
		return ApplicationUtils.getTimeZoneStatic();
	}

	/** See {@link #version}. */
	public String getVersion() {
		return version;
	}
	/** See {@link #version}. */
	public void setVersion(String versn) {
		version = versn;
		LOG.info(version);
	}

	/** See {@link #accessToken}. */
	public JSONObject getAccessToken() {
		Calendar cal = Calendar.getInstance();
		if (accessToken == null || accessTokenExpires.before(cal)) {
			// Haven't gotten a token yet, or the one we have is expired
			try {
				if (accessFailureTime != null && cal.getTimeInMillis() - accessFailureTime.getTimeInMillis() < (2 * 60 * 1000)/*2 minutes*/) {
					EventUtils.logEvent(EventType.DATA_ERROR, "Access token request skipped due to recent failure. (2 minute time-out.)"); // LS-2458
					return null;
				}
				accessTokenExpires = cal;
				accessToken = ApplicationScopeBean.createAccessToken();
				try {
					// Get expiration period of token
					String expiresStr = accessToken.getString("expires_in");
					if (expiresStr != null) {
						int expireSeconds = Integer.parseInt(expiresStr);
						// calculate expiration time; set it earlier than expected to avoid errors.
						cal.add(Calendar.SECOND, expireSeconds - Math.min(expireSeconds/2, 30));
						accessTokenExpires = cal;
						accessFailureTime = null; // clear error timeout. LS-2458
					}
				}
				catch (Exception e) {
					EventUtils.logError("error calculating access token expiration: ",e);
					LOG.info(accessToken); // Additional debugging info. LS-2458
					accessToken = null; // don't return whatever we got. (may be timeout or other failure)
					accessFailureTime = Calendar.getInstance(); // LS-2458
				}
			}
			catch (Exception e) {
				LOG.error("Unable to get global authorization access token; some functions will fail.", e);
				accessToken = null; // don't return old token
				accessFailureTime = Calendar.getInstance(); // LS-2458
			}
		}
		return accessToken;
	}
	/** See {@link #accessToken}. */
	public void setAccessToken(JSONObject accessToken) {
		this.accessToken = accessToken;
	}

	/** See {@link #isBeta}. */
	public boolean getIsBeta() {
		return isBeta;
	}
	/** See {@link #isBeta}. */
	public void setIsBeta(boolean isBeta) {
		this.isBeta = isBeta;
	}

	/** See {@link #pdfNetKey}. */
	public void setPdfNetKey(String pdfNetKey) {
		this.pdfNetKey = pdfNetKey;
	}

	/** See {@link #quartzScheduler}. */
	public Scheduler getQuartzScheduler() {
		return quartzScheduler;
	}
	/** See {@link #quartzScheduler}. */
	public void setQuartzScheduler(Scheduler sched) {
		quartzScheduler = sched;
	}

	/** See {@link #useXod}. */
	public boolean getUseXod() {
		return useXod;
	}
	/** See {@link #useXod}. */
	public void setUseXod(boolean useXod) {
		this.useXod = useXod;
	}

	/**See {@link #junit}. */
	public boolean getJunit() {
		return junit;
	}
	/**See {@link #junit}. */
	public void setJunit(boolean junit) {
		this.junit = junit;
	}

	public Date getToday() {
		return new Date();
	}

	/** See {@link #contactDocumentDAO}. */
	public ContactDocumentDAO getContactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}
	/** See {@link #contactDocumentDAO}. */
	public void setContactDocumentDAO(ContactDocumentDAO contactDocumentDAO) {
		this.contactDocumentDAO = contactDocumentDAO;
	}

	/** See {@link #countryDAO}. */
	public CountryDAO getCountryDAO() {
		if (countryDAO == null) {
			countryDAO = CountryDAO.getInstance();
		}
		return countryDAO;
	}
	/** See {@link #countryDAO}. */
	public void setCountryDAO(CountryDAO countryDAO) {
		this.countryDAO = countryDAO;
	}

	/** See {@link #eventDAO}. */
	public EventDAO getEventDAO() {
		if (eventDAO == null) {
			eventDAO = EventDAO.getInstance();
		}
		return eventDAO;
	}
	/** See {@link #eventDAO}. */
	public void setEventDAO(EventDAO eventDAO) {
		this.eventDAO = eventDAO;
	}

	/** See {@link #projectDAO}. */
	public ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}
	/** See {@link #projectDAO}. */
	public void setProjectDAO(ProjectDAO projectDAO) {
		this.projectDAO = projectDAO;
	}

	/**See {@link #startFormDAO}. */
	public StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}
	/**See {@link #startFormDAO}. */
	public void setStartFormDAO(StartFormDAO startFormDAO) {
		this.startFormDAO = startFormDAO;
	}

	/** See {@link #stateDAO}. */
	public StateDAO getStateDAO() {
		if (stateDAO == null) {
			stateDAO = StateDAO.getInstance();
		}
		return stateDAO;
	}
	/** See {@link #stateDAO}. */
	public void setStateDAO(StateDAO stateDAO) {
		this.stateDAO = stateDAO;
	}

	/** See {@link #weeklyTimecardDAO}. */
	public WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}
	/** See {@link #weeklyTimecardDAO}. */
	public void setWeeklyTimecardDAO(WeeklyTimecardDAO weeklyTimecardDAO) {
		this.weeklyTimecardDAO = weeklyTimecardDAO;
	}

	/**
	 * Create a new authorization access token for the given user name and
	 * password, and save it in the current session.
	 *
	 * @param userName The user's identity name (e.g., email address or unique
	 *            identifier).
	 * @param password The user's password.
	 * @return The newly-created access token, or null if authorization failed.
	 */
	public static JSONObject createAccessToken(String userName, String password) {
		JSONObject jsonObject = null;
		try {
			JWTService jwtService = new JWTServiceImpl();
			jsonObject = jwtService.getAccessToken(userName, password, "CREATE", null);
			LOG.debug(jsonObject.toString(2));
		}
		catch (Exception e) {
			EventUtils.logError("failed creating access token: ", e);
			throw new LoggedException(e);
		}
		return jsonObject;
	}

	/**
	 * Create a new authorization access token using the "standard" credentials, and
	 * save it in the current session; used by JUnit test classes, and also (for now)
	 * ALL user authorizations.
	 *
	 * @return The newly-created access token, or null if authorization failed.
	 */
	public static JSONObject createAccessToken() {
		return createAccessToken(
				ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_AUTH_STANDARD_USER_ID),
				ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_AUTH_STANDARD_USER_PW));
	}

	/**
	 * Build the WebResource instance to be used for interacting with
	 * micro-service api
	 *
	 * @param apiUri
	 * @return
	 */
	public WebResource buildWebResource(URI apiUri) {
		try {
			HTTPBasicAuthFilter auth = getAuthorizationFilter();
			ClientConfig cc = new DefaultClientConfig();
			ObjectMapper objectMapper = new ObjectMapper();

			objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			cc.getSingletons().add(new JacksonJsonProvider(objectMapper));
			final Client client = Client.create(cc);
			client.addFilter(auth);

			return client.resource(apiUri);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Create authorization filter for access microservice api.
	 *
	 * @return
	 */
	private HTTPBasicAuthFilter getAuthorizationFilter() {
		try {
			return new HTTPBasicAuthFilter("TTCO", "Ttc@123");
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Method to get latest release date for a given application, is fetched
	 * from Release Management API
	 *
	 * @return latest release date
	 */
	public String getLatestReleaseDate() {
		try {
			if(latesReleaseDate == null) {
				String apiUrl = ApplicationUtils.getInitParameterString("RELEASE_API_URL", false);
				final URI uri = UriBuilder.fromPath(apiUrl + ReleaseNotesBean.LATEST_RELEASE_DETAILS_URL).build(); // will encode blanks and special characters
				final WebResource webResource = buildWebResource(uri);
				final ClientResponse clientResponse =
						webResource.type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
				ApplicationReleaseDTO appReleaseDetails = new ObjectMapper()
						.readValue(clientResponse.getEntity(String.class), ApplicationReleaseDTO.class);
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
				latesReleaseDate =  appReleaseDetails.getReleaseDate().format(formatter);
			}
			return latesReleaseDate;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}
}
