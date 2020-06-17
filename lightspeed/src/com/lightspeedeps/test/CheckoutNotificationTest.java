/**
 * File: CheckoutNotificationTest.java
 */
package com.lightspeedeps.test;

import com.lightspeedeps.dao.CheckoutNotificationDAO;
import com.lightspeedeps.model.CheckoutNotification;


/**
 * Test some CheckoutNotificationDAO methods.
 * <p>
 * As for all SpringTestCase subclasses, the WEB-INF folder must be added
 * to the classpath for the JUnit test, so the XML configuration files
 * can be located.
 */
public class CheckoutNotificationTest extends SpringTestCase {


	public void testCheckoutNotificationDAO() throws Exception {

		String serialNumber = "1234";
		CheckoutNotificationDAO cnDAO = CheckoutNotificationDAO.getFromApplicationContext(ctx);
		CheckoutNotification cn = new CheckoutNotification(serialNumber);
		cnDAO.save(cn);

		boolean result = cnDAO.exists(serialNumber);
		assertEquals("test failed", true, result);

		result = cnDAO.exists("xxxxxxxxxxxxxxxxxx");
		assertEquals("test failed", false, result);

	}

}
