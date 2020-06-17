package com.lightspeedeps.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.MessageFormat;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import junit.framework.TestCase;

/**
 * Test the validateCityState utility method.
 * <p>
 * This is a modification of CityStateValidatorTest which includes all the code
 * necessary for testing -- no dependencies on other lightspeed.com classes. All
 * the code that's normally distributed in ApplicationUtils, SessionUtils, etc.,
 * has been pulled into this class so that it can run "standalone". This may be
 * useful for programmers who do not have the Lightspeed project installed.
 */
public class CityStateStandalone extends TestCase {

	private static final Log LOG = LogFactory.getLog(CityStateStandalone.class);

	private int success = 0;

	private JSONObject mytoken;

	public void testCityStateValidator() throws Exception {

		// Create a token and save it in "session"
		initializeAccessToken();

		// See CityStateValidatorTest for an expanded range of test cases.

		testOne( "Burbank", "CA", "US", true );
		testOne( "Los Angeles", "CA", "US", true );

		testOne( "New York", "NY", true );

		System.out.println(success + " successful tests completed.");
	}

	/**
	 * Call the validateCityState with one set of parameters, including the country.
	 * @param city  The city name to check.
	 * @param state The state containing the city.
	 * @param country The country for the city/state.
	 * @param result The expected result (true or false).
	 */
	private void testOne(String city, String state, String country, boolean result) {
		boolean res = false;

		res = /*LocationUtils.*/validateCityState(city, state, country);

		assertEquals("city=`"+city+"`, state=`" + state + "`, country=" + country + "` ", result, res);

		success++;
	}

	/**
	 * Call the validateCityState with one set of parameters, without a country.
	 * @param city  The city name to check.
	 * @param state The state containing the city.
	 * @param result The expected result (true or false).
	 */
	private void testOne(String city, String state, boolean result) {
		boolean res = false;

		res = /*LocationUtils.*/validateCityState(city, state);

		assertEquals("city=`"+city+"`, state=`" + state + "` ", result, res);

		success++;
	}

	/**
	 * Creates an authorization token using test user/password values, and
	 * stores the token in the pseudo-Session maintained by SessionUtils for
	 * JUnit tests.
	 *
	 * @return The access token created and saved.
	 * @throws Exception
	 */
	protected JSONObject initializeAccessToken() {
		JSONObject jsonObject = createAccessToken();
		if (jsonObject != null) {
			try {
				LOG.debug(jsonObject.toString(2));
			}
			catch (JSONException e) {
				LOG.error(e);
				//throw new LoggedException(e);
			}
		}
		return jsonObject;
	}

	/** pseudo-user name used for JUnit testing */
	private static final String TEST_USER_NAME = "ttcouser@theteamcompanies.com";
	/** pseudo-user's password used for JUnit testing */
	private static final String TEST_PASSWORD = "TTCO!Bur";

	/**
	 * Create a new authorization access token using the test credentials, and
	 * save it in the current session; used by JUnit test classes.
	 *
	 * @return The newly-created access token, or null if authorization failed.
	 */
	public  JSONObject createAccessToken() {
		return createAccessToken(TEST_USER_NAME, TEST_PASSWORD);
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
	public  JSONObject createAccessToken(String userName, String password) {
		JSONObject jsonObject = null;
		//setCurrentAccessToken(null); // clear any existing token
		try {
//			JWTService jwtService = new JWTServiceImpl();
			jsonObject = /*jwtService.*/getAccessToken(userName, password, "CREATE", null);
			LOG.debug(jsonObject.toString(2));
		}
		catch (Exception e) {
			LOG.error(e);
			//EventUtils.logError("failed creating access token: ", e);
			//throw new LoggedException(e);
		}
		mytoken = jsonObject; //setCurrentAccessToken(jsonObject);
		return jsonObject;
	}

	/**
	 * Verify if a city/state combination is valid (a known city) for the
	 * default country (currently US).
	 *
	 * @param city City name to be tested.
	 * @param state State containing the city; this is a state code, i.e., the
	 *            2-character code for U.S. states.
	 * @return True if the city/state exists in the applications "default"
	 *         country.
	 */
	public  boolean validateCityState(String city, String state) {
		return validateCityState(city, state, "US"); //Constants.DEFAULT_COUNTRY_CODE);
	}

	public static final String API_HOST_URL = "https://losangeles.teamservicesonline.net/";

	private static final String CITY_LOOKUP_PATH = /*Constants.*/API_HOST_URL + "lookup/cities/";
	private static final String CITY_LOOKUP_PARAM_PATTERN = "country/{0}/state/{1}/city/{2}";

	/**
	 * Verify if a city/state/country combination is valid (a known city).
	 *
	 * @param city City name to be tested.
	 * @param state State containing the city; this is a state code, i.e., the
	 *            2-character code for U.S. states.
	 * @param country Country (2-letter ISO code) containing the city/state.
	 * @return True if the city/state exists.
	 */
	public  boolean validateCityState(String city, String state, String country) {

		LOG.debug("");
		boolean foundCity = false;
		if (StringUtils.isEmpty(city) || StringUtils.isEmpty(state) || StringUtils.isEmpty(country)) {
			return false;
		}

		try {
			//String cityParams = /*MsgUtils.*/formatText(CITY_LOOKUP_PARAM_PATTERN, country, state, encodeValue(city));
			String cityParams = /*MsgUtils.*/formatText(CITY_LOOKUP_PARAM_PATTERN, country, state, city);
			String apiUrl = CITY_LOOKUP_PATH + cityParams;

			JSONObject jsonObject = /*ApplicationUtils.*/callRestApi(apiUrl);

			if (jsonObject != null) {
				String ctoken = null;
				try {
					ctoken = jsonObject.getString("city");
				}
				catch (Exception e) {
					LOG.error(e);
				}
				LOG.debug(ctoken);
				if (StringUtils.isNotEmpty(ctoken)) {
					foundCity = true;
				}
			}
		}
		catch (Exception e) {
			LOG.error(e);
			//EventUtils.logError(e);
			//throw new LoggedException(e);
		}

		return foundCity;
	}

	public static String formatText(String text, Object... args) {
		MessageFormat formatter;
//		if (getCurrentLocale() != null) {
//			formatter = new MessageFormat(text, getCurrentLocale());
//		}
//		else { // probably a quartz (scheduled) task calling us
			formatter = new MessageFormat(text); // use default locale
//		}
		text = formatter.format(args);
		return text;
	}

	/**
	 * @param jwtUrl
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public  JSONObject callRestApi(String apiUrl) {
		JSONObject accessToken = mytoken; //SessionUtils.getCurrentAccessToken();
		JSONObject jsonObject = callRestApi(accessToken, apiUrl);
		return jsonObject;
	}

	/**
	 * @param accessToken
	 * @param apiUrl
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public  JSONObject callRestApi(JSONObject accessToken, String apiUrl) {
		JSONObject jsonObject=null;
		try {
			Client client = Client.create();
			URI uri = UriBuilder.fromPath(apiUrl).build();
			final WebResource webResource = client.resource(uri);
			final WebResource.Builder webResourceBuilder  = webResource.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getString("access_token"));

			final ClientResponse clientResponse =
					webResourceBuilder.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientResponse.getEntityInputStream()));
			 String line = bufferedReader.readLine();
			 LOG.debug(line);
			 if (line!=null) {
					JSONArray array = new JSONArray(line);
					if (array.length() > 0) {
						jsonObject = array.getJSONObject(0);
					}
					else {
						// validation failed, no entries returned
						LOG.debug("validation failed, no entries found; apiUrl =" + apiUrl);
					}
			 }
			 bufferedReader.close();
		}
		catch (UniformInterfaceException | ClientHandlerException | JSONException
				| IOException e) {
			LOG.error(e);
			//EventUtils.logError(e);
			//throw new LoggedException(e);
		}
		return jsonObject;
	}

	public JSONObject getAccessToken(String userName, String password, String actionType, String refreshToken) throws Exception {
		LOG.debug("Invoking JWT Token for user "+userName);
		JSONObject jsonObject = null;

		String jwtUserId = "ttcguest"; //clientId
		String jwtPassword = "Ttc$#@2007!TTC"; //client secret
		String jwtUrl = "https://losangeles.teamservicesonline.net/oauth/token"; // service URL

		final ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		Client client = Client.create(clientConfig);
		client.addFilter(new HTTPBasicAuthFilter(jwtUserId, jwtPassword));// authorizes the authentication api call
		final WebResource webResource = client.resource(jwtUrl);
		final MultivaluedMapImpl formData = new MultivaluedMapImpl();

		formData.add("client_id", jwtUserId);
		if("CREATE".equals(actionType)) {
			formData.add("grant_type", "password");// for new token
			formData.add("username", userName);
			formData.add("password", password);
		}else {
			formData.add("grant_type", "refresh_token");// for refresh token
			formData.add("refresh_token", refreshToken);
		}

		final ClientResponse clientResponse =  webResource.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON).
				 post(ClientResponse.class, formData);
		 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientResponse.getEntityInputStream()));
		 String line = bufferedReader.readLine();
		 if (line != null) {
			jsonObject = new JSONObject(line);
		 }
		 bufferedReader.close();
		 LOG.info("Successfully executed getAccessToken for " + userName);
		 return jsonObject;
	}

}
