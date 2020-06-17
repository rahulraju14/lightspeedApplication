/**
 * File: OverdueReportTest.java
 */
package com.lightspeedeps.test.report;

import com.lightspeedeps.batch.DueOverdueCheck;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;


/**
 * Test the "DueOverdueCheck" class, which sends out notifications for reports
 * that are due or overdue.
 */
public class OverdueReportTest extends SpringTestCase {

	DueOverdueCheck check;

	public void testOverdueReport() throws Exception {

		check = (DueOverdueCheck)ctx.getBean("dueOverdueCheck");

		String result = check.projectBasedNotifications();
		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
