/**
 * File: SpringTestCase.java
 */
package com.lightspeedeps.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * This class serves as a superclass for JUnit testing of code which is
 * dependent on the Spring/Hibernate environment. It establishes the application
 * context for the subclass.
 * <p>
 * <b> Use this method when running multiple test methods within your class, all
 * of which can share the same Spring/Hibernate environment.</b>
 * <p>
 * To release the Spring/Hibernate resources when the test methods have
 * completed, you must invoke the {@link #springTearDown()} method. Do this by
 * adding a final test method to your test class, which simply calls this one
 * method. See the {@link com.lightspeedeps.test.payroll.MpvRuleTest MpvRuleTest} class as an example.
 * <p>
 * When running any SpringTestCase subclass test, the WEB-INF folder must be
 * added to the classpath for the JUnit test, so the XML configuration files can
 * be located.
 */
public abstract class SpringMultiTestCase extends TestCaseBase {

	private static Log log = LogFactory.getLog(SpringMultiTestCase.class);
	protected static ApplicationContext ctx = null;
	protected static String[] contextPaths = { "applicationContextHibernate.xml" };
	protected static SessionFactory sessionFactory = null;

	public SpringMultiTestCase() {
		// default constructor
	}

	public SpringMultiTestCase(String contextFile) {
		contextPaths = new String[] {contextFile};
	}

	@Override
	protected void setUp() throws Exception {
		log.debug("Begin setup");
		super.setUp();

		if (sessionFactory == null) {
			try {
				// Establish Spring context, which includes Hibernate support
				ctx = new ClassPathXmlApplicationContext(contextPaths);
				sessionFactory = (SessionFactory) ctx.getBean("sessionFactory");
				Session session = SessionFactoryUtils.getSession(sessionFactory, true);
				TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));

				ServiceFinder.setBatchApplicationContext(ctx); // so "findBean" doesn't need FacesContext

				// Set SessionUtils to support JUnit:
				SessionUtils.setjUnitUnderEclipse(true);

				log.debug("\n\n--------------- Context established -- proceeding with test ------------------------- [SpringTestCase::setUp]\n");
			}
			catch (Exception e) {
				log.error("Spring setup failed: ");
				log.error(e);
				throw new Exception(e);
			}
		}
	}

	protected void springTearDown() throws Exception {
		log.debug("\n\n*************** Begin tearDown (test completed) ************************************* [SpringTestCase::tearDown]\n");

		try {
			SessionFactory mySessionFactory = sessionFactory;
			sessionFactory = null;
			SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(mySessionFactory);
			//SessionFactoryUtils.closeSessionIfNecessary(sessionHolder.getSession(), sessionFactory);
			SessionFactoryUtils.releaseSession(sessionHolder.getSession(), mySessionFactory);
			ctx = null;
			// Clear SessionUtils Junit support setting:
			SessionUtils.setjUnitUnderEclipse(false);
		}
		catch (Exception e) {
			log.error(e);
		}
	}

}