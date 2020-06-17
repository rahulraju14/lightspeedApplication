/**
 * SecurityProviderList.java
 */
package com.lightspeedeps.test.util;

import java.security.Provider;
import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class will list all the security providers (i.e., for encryption and
 * decryption) that are installed in the system.  It may be run as a standalone
 * Java application, or its static method may be called from elsewhere.
 */
public class SecurityProviderList {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SecurityProviderList.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		listSecurityProviders();
	}

	/**
	 * List all the registered security providers and their supported
	 * algorithms. Output is via System.out.
	 */
	public static void listSecurityProviders() {
		for (Provider provider : Security.getProviders()) {
			System.out.println("Provider: " + provider.getName());
			for (Provider.Service service : provider.getServices()) {
				System.out.println("  Algorithm: " + service.getAlgorithm());
				System.out.println("    -- " + service.toString());
			}
		}
	}

}
