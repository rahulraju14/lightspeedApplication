package com.lightspeedeps.util.app;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * ActiveDirectoryConfig for setting up connection to LDAP
 * @author mabuthahir LS-2336
 *
 */

public class ActiveDirectoryConfig {

	private static final Log LOGGER = LogFactory.getLog(ActiveDirectoryConfig.class);

	private LDAPConnectionPool ldapConnectionPool;
	private LDAPConnection ldapConnection;

	/**
	 * <p>
	 * Creates ActiveDirectoryConfig object which instantiates LDAP Connection and LDAP Connection pools. It is a singleton instance so it is declared as private constructor
	 * @param
	 * @return ActiveDirectoryConfig
	 */

	private ActiveDirectoryConfig() {
		try {
			Map<String, String> params = ApplicationUtils. getFf4jParams();
			ldapConnection	= new LDAPConnection(params.get(ApplicationUtils.LDAP_HOSTNAME_PARAM), new Integer(params.get(ApplicationUtils.LDAP_PORT_PARAM)).intValue(),
					params.get(ApplicationUtils.LDAP_USERNAME_PARAM), params.get(ApplicationUtils.LDAP_PASSWORD_PARAM));
			ldapConnectionPool = new LDAPConnectionPool(ldapConnection,  new Integer(params.get(ApplicationUtils.LDAP_INITIAL_POOL_SIZE_PARAM)).intValue(),
					new Integer(params.get(ApplicationUtils.LDAP_MAX_POOL_SIZE_PARAM)).intValue());
		}
		catch (LDAPException e) {
			LOGGER.error("Error creating LDAP connection pool",e);
		}
	}

	/**
	 * <p>
	 * Helper class to instantiate ActiveDirectoryConfig as a singleton
	 */

	private static class ActiveDirectoryConfigHelper{
		private static final ActiveDirectoryConfig INSTANCE = new ActiveDirectoryConfig();
	}

	/**
	 * <p>
	 * Creates LDAPConnectionPool object
	 * @param
	 * @return LDAPConnectionPool
	 */

	public static LDAPConnectionPool getLDAPConnectionPool(){
		return ActiveDirectoryConfigHelper.INSTANCE.ldapConnectionPool;
	}


	/**
	 * <p>
	 * Closes the LdapConnectionPool
	 */

	public static void closeLDAPConnectionPool(){
		LOGGER.debug("closeLDAPConnectionPool starts");
		ActiveDirectoryConfigHelper.INSTANCE.ldapConnectionPool.close();
		LOGGER.debug("closeLDAPConnectionPool ends");
	}
}
