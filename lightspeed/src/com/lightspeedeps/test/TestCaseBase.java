/**
 * File: TestCaseBase.java
 */
package com.lightspeedeps.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.web.util.ApplicationScopeBean;

import junit.framework.TestCase;

/**
 * Base class for JUnit tests. Contains "utility" methods shared by all test
 * classes.
 */
public class TestCaseBase extends TestCase {

	private static final Log LOG = LogFactory.getLog(TestCaseBase.class);

	/**
	 * Default constructor.  Sets up ApplicationUtils for context-free running,
	 * and adds some test values for initialization parameters normally available
	 * (outside of JUnit) from web.xml.
	 */
	public TestCaseBase() {
		ApplicationUtils.setNoContext(true);

		ApplicationUtils.setInitializationParameter("timeZone", "PST");

		ApplicationUtils.setInitializationParameter("authClientId", "ttcguest");
		ApplicationUtils.setInitializationParameter("authClientPassword", "Ttc$#@2007!TTC");
		ApplicationUtils.setInitializationParameter("authStandardUserId", "ttcouser@theteamcompanies.com");
		ApplicationUtils.setInitializationParameter("authStandardUserPassword", "TTCO!Bur");
		ApplicationUtils.setInitializationParameter("apiServerDomain", "https://losangeles.teamservicesonline.net/");

		ApplicationUtils.setInitializationParameterNoprefix("ESS_URL", "https://ess-qa.ttconline.com/");
		ApplicationUtils.setInitializationParameterNoprefix("AUTHORIZATION_API_URL", "http://localhost:7073");
		ApplicationUtils.setInitializationParameterNoprefix("ONBOARDING_API_URL", "http://localhost:7074");
	}

	/**
	 * Creates an authorization token using test user/password values, and
	 * stores the token in the pseudo-Session maintained by ApplicationScopeBean.
	 *
	 * @return The access token created and saved.
	 * @throws Exception
	 */
	protected JSONObject initializeAccessToken() {
		JSONObject jsonObject = ApplicationScopeBean.createAccessToken();
		if (jsonObject != null) {
			ApplicationScopeBean.getInstance().setAccessToken(jsonObject);
			try {
				LOG.debug(jsonObject.toString(2));
			}
			catch (JSONException e) {
				LOG.error(e);
				throw new LoggedException(e);
			}
		}
		return jsonObject;
	}

}
