package com.lightspeedeps.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
* Service implementation class that handles OAuth authorization.
*/
public class JWTServiceImpl implements JWTService {

	private static final Log LOG = LogFactory.getLog(OnboardService.class);

	@Override
	public JSONObject getAccessToken(String userName, String password, String actionType, String refreshToken) throws Exception {
		LOG.debug("Invoking JWT Token for user "+userName);
		JSONObject jsonObject = null;

		String jwtUserId = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_AUTH_CLIENT_ID); // clientId
		String jwtPassword = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_AUTH_CLIENT_PW); // client secret
		String jwtUrl = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_API_SERVER_DOMAIN) + "oauth/token"; // authorization service URL

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
