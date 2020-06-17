package com.lightspeedeps.util.app;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ff4j.security.AuthorizationsManager;

import com.lightspeedeps.model.User;
import com.unboundid.ldap.sdk.*;

/**
 * FeatureAuthorizationManager implementation for LS-2336
 * @author mabuthahir
 *
 */

public class FeatureAuthorizationManager implements AuthorizationsManager{
	private static final Log LOG = LogFactory.getLog(FeatureAuthorizationManager.class);

	@Override
	public String getCurrentUserName() {
		return null;
	}

	/**
	 * Get the current user groups from TTC Active Directory
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Set<String> getCurrentUserPermissions() {
		final Set<String> permissions = new HashSet<>();
		final ThreadLocal<User> threadLocal = new ThreadLocal();
		threadLocal.set(SessionUtils.getCurrentUser());

		try {
			if (threadLocal.get() != null && threadLocal.get().getEmailAddress() != null) {
				final Filter filter = Filter.createEqualityFilter("mail", threadLocal.get().getEmailAddress());
				final SearchRequest searchRequest = new SearchRequest("dc=teamservices,dc=net", SearchScope.SUB,filter,"cn","memberOf"); //LDAP Query
				final SearchResult searchResult =  ActiveDirectoryConfig.getLDAPConnectionPool().search(searchRequest);
				SearchResultEntry searchResultEntry = null;
				if (searchResult.getEntryCount() == 1 &&
						null != (searchResultEntry = searchResult.getSearchEntries().get(0))) { //check if the search returns exactly one result for the given email id
					final String[] arr = searchResultEntry.getAttributeValues("memberOf"); //Attributes to fetch from LDAP Query
					if (arr != null) { // null if user in LDAP but has no memberships
						for (String str : arr) {
							permissions.add(str.substring(str.indexOf("=") + 1, str.indexOf(",")));
						}
					}
				}
			}
		}
		catch (LDAPException e) {
			LOG.error("Error while finding processing getCurrentUserPermissions---->",e);
		}
		return permissions;
	}

	@Override
	public Set<String> listAllPermissions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toJson() {
		// TODO Auto-generated method stub
		return null;
	}

}
