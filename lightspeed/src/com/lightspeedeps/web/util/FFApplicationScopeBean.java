/**
 * File: FFApplicationScopeBean.java
 */
package com.lightspeedeps.web.util;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ff4j.core.Feature;

import com.lightspeedeps.util.app.FF4JConfig;
import com.lightspeedeps.util.app.FF4JUtils;

/**
 * Managed bean to implement Feature Flag which allows a user to view a feature
 * based on whether the user is included in the Roles list for this feature in
 * the Feature Manager. LS-2336
 */
@ApplicationScoped
@ManagedBean
public class FFApplicationScopeBean {
//	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FFApplicationScopeBean.class);

	/** running total of time spent in the 'useFeature' method */
	private static long sumTime = 0;

	public FFApplicationScopeBean() {
	}

	/**
	 * Determine if a given feature is enabled or disabled depending on the
	 * Feature toggle switch on the Feature Manager. This method is not static,
	 * and takes a String parameter, so that it may be called from JSF. Java
	 * code should use the FF4JUtils method instead.
	 *
	 * Check to see if this user is able to access the feature. See
	 * {@link #isAllowedToAccessFeature}.
	 *
	 * @param featureFlagId The feature flag to test.
	 *
	 * @return - True if the feature is enabled, false if disabled.
	 */
	public boolean useFeature(String featureFlagId) {
		boolean useFeature = true;

		long startTime = System.nanoTime();

		//Feature feature = FF4JConfig.getFF4j().getFeature(featureFlagId);

		/*if (FF4JUtils.isAllowedToAccessFeature(feature)) {
			useFeature = FF4JConfig.getFF4j().check(featureFlagId);
		}*/

		sumTime +=  System.nanoTime() - startTime;
		LOG.debug("checking FF " + featureFlagId + "; total 'useFeature' time(ms)=" + (sumTime/1000000));
		if (sumTime > 100000000000L) {
			sumTime = 0; // reset periodically (every 100 'used' seconds) so it won't overflow
		}

		return useFeature;
	}

	/**
	 * Get text from the description field of the Feature.
	 * This can be used to display a message when the feature has been turned on.
	 * LS-2965
	 *
	 * @param featureFlagId
	 * @return
	 */
	/*public String getFeatureDesc(String featureFlagId) {
		//String desc = "";

		Feature feature = FF4JConfig.getFF4j().getFeature(featureFlagId);
		String desc = feature.getDescription();
		if(feature != null) {
			desc = feature.getDescription();
		}
		return desc;
	}*/

}
