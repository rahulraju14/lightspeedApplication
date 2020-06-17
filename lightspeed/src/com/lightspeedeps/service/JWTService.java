package com.lightspeedeps.service;

import org.json.JSONObject;

/**
* JWTService Interface that handles OAuth authorization.
*/
public interface JWTService {

	/**
	 * This method invokes the JWT authentication service to create/refresh the
	 * JWT token based on the actionType(CREATE/REFRESH)
	 *
	 * @param userName The individual's login name (may be email address or
	 *            other identifier).
	 * @param password Password matching the given userName.
	 * @param actionType If "CREATE", a new access token is retrieved; any other
	 *            value is assumed to be a refresh action.
	 * @param refreshToken The refresh token retrieved during a prior access.
	 * @return JSONObject The access object, which normally includes at least an
	 *         'access_token' and a 'refresh_token'.
	 * @throws Exception
	 */
	public JSONObject getAccessToken(String userName,String password,String actionType,String refreshToken) throws Exception;

}