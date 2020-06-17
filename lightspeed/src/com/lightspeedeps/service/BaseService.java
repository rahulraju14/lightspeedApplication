/**
 * File: BaseService.java
 */
package com.lightspeedeps.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * Provide some common methods shared across multiple "Service" classes.
 */
public class BaseService {
	private static final Log log = LogFactory.getLog(BaseService.class);

	public static Object getInstance(String beanName) {
		return ServiceFinder.findBean(beanName);
	}

	/**
	 * For testing
	 */
	public void fail() {
		if (log.isDebugEnabled()) {
			throw new RuntimeException();
		}
	}

}
