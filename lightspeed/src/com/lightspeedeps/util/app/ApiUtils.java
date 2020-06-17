/**
 * File: ApiUtils.java
 */
package com.lightspeedeps.util.app;

import java.io.*;
import java.net.URI;

import javax.ws.rs.core.*;

import org.apache.commons.logging.*;
import org.json.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.*;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.ttconline.dto.*;

/**
 * Utility methods related to calling microservice APIs.
 * <p>
 * Some of these methods were moved from ApplicationUtils, so that they would
 * not be included in the ttco-base-model project. LS-3067.
 */
public class ApiUtils {

	private static final Log LOG = LogFactory.getLog(ApiUtils.class);

	/** Get the Authorization API URL from web.xml */
	private static final String AUTH_API_URL =  ApplicationUtils.getInitParameterString("AUTHORIZATION_API_URL", false);

	/** The URL suffix for the get-token service in ttco-authorization-api. */
	private static final String GET_TOKEN_URL = "/user/hopoff/token";

	/** The URL suffix for the extract-from-token service in ttco-authorization-api. */
	private static final String EXTRACT_TOKEN_URL = "/user/hopoff/token/extract";

	/** The URL suffix for the validate-token service in ttco-authorization-api. */
	private static final String VALIDATE_TOKEN_URL = "/user/hopoff/token/validate";

	/** The URL suffix for the send-email service in ttco-authorization-api. ESS-1471 */
	private static final String SEND_EMAIL_URL = "/user/ttco/email";

	/**
	 * @return The user's current access token, if any. May be null.
	 */
	public static JSONObject getCurrentAccessToken() {
		JSONObject token = ApplicationScopeBean.getInstance().getAccessToken();
		// Following code is for session-specific (per user) token:
//		JSONObject token = null;
//		HttpSession session = getHttpSession();
//		if (session != null) {
//			token = (JSONObject)session.getAttribute(Constants.ATTR_ACCESS_TOKEN);
//		}
		LOG.debug("token is " + (token == null ? "null" : "non-null"));
		return token;
	}

	/**
	 * Currently we are using an application-wide access token, the same for all
	 * TTCO users.
	 * <p>
	 * In the future, this method would create a new authorization access token
	 * for the given User, and save it in the current session.
	 *
	 * @param user The User to be given the token.
	 * @return The newly-created access token, or null if authorization failed.
	 */
	public static JSONObject createAccessToken(User user) {
		return ApiUtils.getCurrentAccessToken();
		// in the future we may do this:
		// return createAccessToken(user.getEmailAddress(), user.getEncryptedPassword());
	}

	/**
	 * Invoke a Team REST API at the specified URL using the current user's
	 * access token. If null is returned, the caller should assume that access
	 * has been denied and take appropriate action. The access failure may be
	 * due to invalid authorization parameters, or an external server/service
	 * failure.
	 *
	 * @param apiUrl The (un-encoded) URL.
	 * @return Either null, if an access token could not be acquired; or the
	 *         JSONObject returned by the API; or an empty JSONObject if none
	 *         was returned by the API.
	 */
	public static JSONObject callRestApi(String apiUrl) {
		JSONObject accessToken = getCurrentAccessToken();
		if (accessToken == null) {
			return null; // failed, error already logged. LS-2458
		}
		JSONObject jsonObject = callRestApi(accessToken, apiUrl);
		return jsonObject;
	}

	/**
	 * Invoke a Team REST API at the specified URL using the given access token.
	 *
	 * @param accessToken The access (authorization) token. May not be null.
	 * @param apiUrl The (un-encoded) URL. May not be null.
	 * @return The JSONObject returned by the API, or an empty (new) JSONObject if no JSONObject was
	 * returned by the API.
	 * @throws IllegalArgumentException if either parameter is null.
	 */
	private static JSONObject callRestApi(JSONObject accessToken, String apiUrl) {
		if (accessToken == null || apiUrl == null) {
			throw new IllegalArgumentException("null token or URL parameter");
		}
		JSONObject jsonObject= new JSONObject();
		try {
			final Client client = Client.create();
			final URI uri = UriBuilder.fromPath(apiUrl).build(); // will encode blanks and special characters
			LOG.debug("URI=" + uri);
			final WebResource webResource = client.resource(uri);
			final WebResource.Builder webResourceBuilder  = webResource.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getString("access_token"));

			final ClientResponse clientResponse =
					webResourceBuilder.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			 final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientResponse.getEntityInputStream()));
			 final String line = bufferedReader.readLine();
			 LOG.debug(line);
			 if (line!=null) {
					final JSONArray array = new JSONArray(line);
					if (array.length() > 0) {
						jsonObject = array.getJSONObject(0);
					}
			 }
			 bufferedReader.close();
		}
		catch (UniformInterfaceException | ClientHandlerException | JSONException
				| IOException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return jsonObject;
	}

	/**
	 * Send an email via the email micro-service API. The EmailDTO object has
	 * all the necessary information such as the email template to use, and the
	 * recipient.  ESS-1471
	 *
	 * @param email An EmailDTO object with appropriate values set.
	 */
	public static void sendEmail(EmailDTO email) {
		try {
			final URI uri = UriBuilder.fromPath(AUTH_API_URL + SEND_EMAIL_URL).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			final ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, email);
			String response = clientResponse.getEntity(String.class);
			LOG.debug("response=" + response);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Get (create) a token for the specified user.
	 *
	 * @param user The User to be associated with the token.
	 * @return The token string; this is encrypted and base64 encoded.
	 */
	public static String getToken(User user) {
		ClientResponse clientResponse = null;
		try {
			final URI uri = UriBuilder.fromPath(AUTH_API_URL  + GET_TOKEN_URL).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			UserDTO userDto = new UserDTO(user);
			LOG.debug("User=" + user.getAccountNumber() + "[" + user.getEmailAddress() + "]");
			clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, userDto);
			userDto = clientResponse.getEntity(UserDTO.class);
			String token = userDto.getToken();
			LOG.debug("Token=`" + token + "`");
			return token;
		}
		catch(Exception e) {
			String msgText = "Error in authorization api response.";
			if (clientResponse != null) {
				msgText = clientResponse.toString();
			}
			EventUtils.logError(msgText, e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Validate a token against a known User. The token must have been created
	 * for the given User, and must not have expired yet.
	 *
	 * @param user The User who should 'match' the token.
	 * @param token The token string, base64 encoded.
	 * @return True iff the token was created for the given User, and has not
	 *         yet expired.
	 */
	public static boolean validateToken(User user, String token) {
		ClientResponse clientResponse = null;
		try {
			final URI uri = UriBuilder.fromPath(AUTH_API_URL  + VALIDATE_TOKEN_URL).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			UserDTO userDto = new UserDTO(user);
			userDto.setToken(token);
			clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, userDto);
			userDto = clientResponse.getEntity(UserDTO.class);
			LOG.debug("valid=" + userDto.isTokenValid());
			return userDto.isTokenValid();
		}
		catch(Exception e) {
			String msgText = "Error in authorization api response.";
			if (clientResponse != null) {
				msgText = clientResponse.toString();
			}
			EventUtils.logError(msgText, e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Validate a token and extract the user account number. The token must not
	 * have expired yet.
	 *
	 * @param token The token string, base64 encoded.
	 * @return User object retrieved based on the accountNumber encoded in the
	 *         token; or null if the token is invalid or expired.
	 */
	public static User validateToken(String token) {
		ClientResponse clientResponse = null;
		try {
			final URI uri = UriBuilder.fromPath(AUTH_API_URL  + EXTRACT_TOKEN_URL).build(); // will encode blanks and special characters
			final WebResource webResource = ApiUtils.buildWebResource(uri);
			UserDTO userDto = new UserDTO();
			userDto.setToken(token);
			clientResponse = webResource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, userDto);
			userDto = clientResponse.getEntity(UserDTO.class);
			User user = null;
			if (userDto.isTokenValid()) {
				user = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, userDto.getUserName());
			}
			LOG.debug("valid=" + userDto.isTokenValid() + ", user=" + user);
			return user;
		}
		catch(Exception e) {
			String msgText = "Error in authorization api response.";
			if (clientResponse != null) {
				msgText = clientResponse.toString();
			}
			EventUtils.logError(msgText, e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Build the WebResource instance to be used for interacting with
	 * micro-service api
	 *
	 * @param apiUri
	 * @return
	 */
	public static WebResource buildWebResource(URI apiUri) {
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
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Create authorization filter for access microservice api.
	 *
	 * @return
	 */
	private static HTTPBasicAuthFilter getAuthorizationFilter() {
		try {
			return new HTTPBasicAuthFilter("TTCO", "Ttc@123");
		}
		catch(Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Invoke a Team REST API at the specified URL using the current user's
	 * access token. If null is returned, the caller should assume that access
	 * has been denied and take appropriate action. The access failure may be
	 * due to invalid authorization parameters, or an external server/service
	 * failure.
	 *
	 * @param apiUrl The (un-encoded) URL.
	 * @return Either null, if an access token could not be acquired; or the
	 *         JSONArray returned by the API; or an empty JSONArray if none
	 *         was returned by the API.
	 */
	public static JSONArray callRestApiWithJSONArray(String apiUrl) {
		JSONObject accessToken = getCurrentAccessToken();
		if (accessToken == null) {
			return null; // failed, error already logged. LS-2458
		}
		JSONArray jsonArray = callRestApiWithJSONArray(accessToken, apiUrl);
		return jsonArray;
	}

	/**
	 * Invoke a Team REST API at the specified URL using the given access token.
	 *
	 * @param accessToken The access (authorization) token. May not be null.
	 * @param apiUrl The (un-encoded) URL. May not be null.
	 * @return The JSONArray returned by the API, or an empty (new) JSONArray if no JSONArray was
	 * returned by the API.
	 * @throws IllegalArgumentException if either parameter is null.
	 */
	private static JSONArray callRestApiWithJSONArray(JSONObject accessToken, String apiUrl) {
		final WebResource webResource;
		if (accessToken == null || apiUrl == null) {
			throw new IllegalArgumentException("null token or URL parameter");
		}
		JSONArray array = new JSONArray();
		try {
			final Client client = Client.create();
			final URI uri = UriBuilder.fromPath(apiUrl).build(); // will encode blanks and special characters
			LOG.debug("URI=" + uri);
			// API call to get the agency list doesn't accept encoded format for '?'
			if (apiUrl.contains("agents")) {
				webResource = client.resource(apiUrl);
			}
			else {
				webResource = client.resource(uri);
			}
			final WebResource.Builder webResourceBuilder  = webResource.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getString("access_token"));

			final ClientResponse clientResponse =
					webResourceBuilder.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			 final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientResponse.getEntityInputStream()));
			 final String line = bufferedReader.readLine();
			 LOG.debug(line);
				if (line != null) {
					array = new JSONArray(line);
				}
			 bufferedReader.close();
		}
		catch (UniformInterfaceException | ClientHandlerException | JSONException
				| IOException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return array;
	}

}
