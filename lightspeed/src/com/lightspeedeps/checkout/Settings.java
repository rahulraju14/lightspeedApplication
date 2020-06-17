/**
 * File: Settings.java
 */
package com.lightspeedeps.checkout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;

//import com.google.checkout.sdk.commands.ApiContext;
//import com.google.checkout.sdk.commands.Environment;

/**
 * Settings or constants specific to checkout or purchases.
 */
public class Settings {
	private static final Log log = LogFactory.getLog(Settings.class);

	/** Auth.net production account login id (NOT stored in SVN copy) */
	private static String authProdLoginId = "********";
	/** Auth.net test account login id (stored in SVN copy) */
	private static String authTestLoginId = "********";

	/** Auth.net production account transaction key (NOT stored in SVN copy) */
	private static String authProdTransactionKey = "****************";
	/** Auth.net test account transaction key (stored in SVN copy) */
	private static String authTestTransactionKey = "****************";

	/** Auth.net production transaction URL */
	private static String postProdUrl = "https://secure2.authorize.net/gateway/transact.dll";
	/** Auth.net test transaction URL */
	private static String postTestUrl = "https://test.authorize.net/gateway/transact.dll";

	/** True iff the application is running in "production mode", meaning that the live, production
	 * version of Auth.net will be used.  When false, the test account and settings are used, and
	 * no actual charges will be processed. */
	private static Boolean productionMode;

	/** private constructor prevents instantiation. */
	private Settings() {
	}

	/**See {@link #productionMode}. */
	public static Boolean getProductionMode() {
		if (productionMode == null) {
			productionMode = ApplicationUtils.getInitParameterBoolean(Constants.INIT_PARAM_AUTHORIZE_PRODUCTION, false);
			log.debug("Authorize.net production mode is " + productionMode);
		}
		return productionMode;
	}

	/** @return either {@link #postProdUrl} or {@link #postTestUrl}. */
	public static String getPostUrl() {
		if (getProductionMode()) {
			return postProdUrl;
		}
		else {
			return postTestUrl;
		}
	}

	/** @return either {@link #authProdLoginId} or {@link #authTestLoginId}. */
	public static String getAuthLoginId() {
		if (getProductionMode()) {
			return authProdLoginId;
		}
		else {
			return authTestLoginId;
		}
	}

	/** @return either {@link #authProdTransactionKey} or {@link #authTestTransactionKey}. */
	public static String getAuthTransactionKey() {
		if (getProductionMode()) {
			return authProdTransactionKey;
		}
		else {
			return authTestTransactionKey;
		}
	}

	// Google checkout
//	private static ApiContext API_CONTEXT;
//
//
//	public static ApiContext getApiContext() {
//		// Only create it if we actually go through Google checkout code
//		if (API_CONTEXT == null) {
//			API_CONTEXT = new ApiContext(
//					Environment.SANDBOX,  "465483986512910",    "adwGBKytWOv9_BBgRoAiMw", "USD");
//			// Google Sandbox merchant id: 465483986512910, key: adwGBKytWOv9_BBgRoAiMw
//		}
//		return API_CONTEXT;
//	}

}
