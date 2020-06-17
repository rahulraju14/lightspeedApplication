/**
 * File: RunSomethingTest.java
 */
package com.lightspeedeps.test;

import com.lightspeedeps.util.app.Constants;


/**
 * Test the "RunSomething" class, which can do anything you want!
 * As for all SpringTestCase subclasses, the WEB-INF folder must be added
 * to the classpath for the JUnit test, so the XML configuration files
 * can be located.
 */
public class RunSomethingTest extends SpringTestCase {

	RunSomething test;

	public void testRunSomething() throws Exception {

		test = (RunSomething)ctx.getBean("runSomething");

		String result = test.execute();

		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
