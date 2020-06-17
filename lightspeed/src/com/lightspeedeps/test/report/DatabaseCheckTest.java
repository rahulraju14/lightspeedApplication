/**
 * File: DatabaseCheckTest.java
 */
package com.lightspeedeps.test.report;

import com.lightspeedeps.batch.DatabaseCheck;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;


/**
 * Test the "DatabaseCheck" class, which checks database availability.
 * As for all SpringTestCase subclasses, the WEB-INF folder must be added
 * to the classpath for the JUnit test, so the XML configuration files
 * can be located.
 */
public class DatabaseCheckTest extends SpringTestCase {

	DatabaseCheck check;

	public void testDatabaseCheck() throws Exception {

		check = (DatabaseCheck)ctx.getBean("databaseCheck");

		String result = check.execute();
		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
