/**
 * File: EventLogSummaryTest.java
 */
package com.lightspeedeps.test.report;

import com.lightspeedeps.batch.EventLogSummary;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;


/**
 * Test the {@link com.lightspeedeps.batch.EventLogSummary} class, which
 * summarizes and logs recent Event activity, particularly errors, and
 * optionally emails that information to someone.
 */
public class EventLogSummaryTest extends SpringTestCase {

	EventLogSummary check;

	public void testEventLogSummary() throws Exception {

		check = (EventLogSummary)ctx.getBean("eventLogSummary");

		String result = check.execute();
		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
