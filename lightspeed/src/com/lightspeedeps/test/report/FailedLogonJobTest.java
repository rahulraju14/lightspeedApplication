/**
 * File: FailedLogonJobTest.java
 */
package com.lightspeedeps.test.report;

import com.lightspeedeps.batch.FailedLogonJob;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;


/**
 * Test the "FailedLogonJob" class, which logs the number of users who are
 * currently "locked out" (due to excessive invalid password entries), and
 * optionally emails that information to someone.
 */
public class FailedLogonJobTest extends SpringTestCase {

	FailedLogonJob check;

	public void testFailedLogonJob() throws Exception {

		check = (FailedLogonJob)ctx.getBean("failedLogonJob");

		String result = check.execute();
		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
