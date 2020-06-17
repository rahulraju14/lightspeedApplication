package com.lightspeedeps.util.app;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ff4j.FF4j;
import org.ff4j.web.jersey1.store.FeatureStoreHttp;
import org.ff4j.web.jersey1.store.PropertyStoreHttp;

/**
 * FF4J configuration class -- used to instantiate and reference the FF4j singleton object.
 */
public class FF4JConfig {

	private static final Log LOGGER = LogFactory.getLog(FF4JConfig.class);

	private static FF4j FF4J;

	/**
	 * default constructor - private so it cannot be instantiated.
	 */

	private FF4JConfig() {
	}

	/**
	 * Create the static FF4J instance.
	 */
	private static void createFf4j() {
		try {
			FF4J = new FF4j();
	    	FF4J.setFeatureStore(new FeatureStoreHttp(ApplicationUtils.getInitParameterString(ApplicationUtils.FF_STORE_URL_PARAM, false)));
	        FF4J.setPropertiesStore(new PropertyStoreHttp(ApplicationUtils.getInitParameterString(ApplicationUtils.FF_STORE_URL_PARAM, false)));
	        FF4J.autoCreate(true);
	        FF4J.audit(true);
	        FF4J.setAuthorizationsManager(new FeatureAuthorizationManager());
	        LOGGER.debug("FF4J object created");
		}
		catch (Exception e) {
			LOGGER.error("Error while initializing the FF4J object", e);
		}
	}

	/**
	 * @return FF4j - current FF4j object; calling this method will cause
	 * it to be created if it does not already exist.
	 */

	/*public static FF4j getFF4j() {
		if (FF4J == null) {
			default FF4J==null
			createFf4j();
		}
		return FF4J;
	}*/

}
