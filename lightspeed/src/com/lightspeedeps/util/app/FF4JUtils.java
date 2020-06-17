/**
 * File: FeatureFlagUtils.java
 */
package com.lightspeedeps.util.app;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ff4j.core.Feature;

import com.lightspeedeps.type.FeatureFlagType;

/**
 * Managed bean to implement Feature Flag which allows a user to view a feature
 * based on whether the user is included in the Roles list for this feature in
 * the Feature Manager. LS-2336
 */
public class FF4JUtils {
	private static final Log LOG = LogFactory.getLog(FF4JUtils.class);

	private FF4JUtils() {
	}

	/**
	 * Enable or disable the feature depending on the Feature toggle
	 * switch on the Feature Manager.
	 *
	 * Check to see if this user is able to access the feature.
	 * See {@link #isAllowedToAccessFeature}.
	 * @param featureFlagId
	 *
	 * @return - If feature is enabled or disabled.
	 */
	public static boolean useFeature(FeatureFlagType featureFlag) {
		boolean useFeature = true;
		/*Feature feature = FF4JConfig.getFF4j().getFeature(featureFlag.name());

		if(isAllowedToAccessFeature(feature)) {
			useFeature = FF4JConfig.getFF4j().check(featureFlag.name());
		}
*/		return useFeature;
	}

	/**
	 * Check to see if this user can access the feature.
	 *
	 * @param feature
	 * @return
	 */
	public static boolean isAllowedToAccessFeature(Feature feature) {
		/*try {
			// If the feature is null or the permissions are empty
			// then everyone should be able to access this feature
			if (feature == null) {
				return true;
			}
			if (feature.getPermissions().isEmpty()) {
				return true;
			}

			FeatureAuthorizationManager fam = (FeatureAuthorizationManager)FF4JConfig.getFF4j().getAuthorizationsManager();
			if (fam == null) {
				return true;
			}

			Set<String> userRoles = fam.getCurrentUserPermissions();
			if (userRoles.isEmpty()) {
				// All users should be able to access this feaure
				return true;
			}
			// Check to see if this user belongs to a Outlook group
			for (String expectedRole : feature.getPermissions()) {
				if (userRoles.contains(expectedRole)) {
					return true;
				}
			}
		}
		catch(Exception ex) {
			LOG.debug(ex);
			EventUtils.logError(ex);
		}

*/		return true;
	}
}
