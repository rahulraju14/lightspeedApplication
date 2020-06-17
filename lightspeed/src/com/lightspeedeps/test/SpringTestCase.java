/**
 * File: SpringTestCase.java
 */
package com.lightspeedeps.test;

/**
 * This class serves as a superclass for JUnit testing of code which is
 * dependent on the Spring/Hibernate environment. It establishes the application
 * context for the subclass.
 * <p>
 * <b> Use this method when only a single test method is to be run!</b> If you
 * have several test methods to run within your class, all of which can share
 * the same Spring/Hibernate environment, subclass the SpringMultiTestCase class
 * instead.
 * <p>
 * When running any SpringTestCase subclass test, the WEB-INF folder must be
 * added to the classpath for the JUnit test, so the XML configuration files can
 * be located.
 */
public abstract class SpringTestCase extends SpringMultiTestCase {

	/**
	 * This automatically invokes the springTearDown method, so the caller
	 * doesn't have to do that via an extra "dummy" test case.
	 *
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		springTearDown();
		super.tearDown();
	}

}
