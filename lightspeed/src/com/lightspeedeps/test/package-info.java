/**
 * Contains classes used to test other classes, typically in a JUnit
 * environment. Currently these classes are NOT included in the lightspeed.jar
 * file used for production and QA environments.
 * <p>
 * Note that JUnit test classes which require the Hibernate environment should
 * subclass {@link com.lightspeedeps.test.SpringTestCase}, which handles the
 * necessary context setup and termination.
 * <p>
 * Classes which are JSF-managed beans which support "test" web pages that are
 * usually only accessible to LightSPEED administrative personnel should NOT be
 * in this package, but in the com.lightspeedeps.web.test package. That package
 * is included in lightspeed.jar, and is therefore for web testing in QA or
 * other environments.
 */
package com.lightspeedeps.test;
