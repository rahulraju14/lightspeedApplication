/**
 * File: SpringBatch.java
 */
package com.lightspeedeps.batch;

import org.apache.commons.logging.*;
import org.hibernate.*;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.orm.hibernate3.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This class serves as a superclass for batch or timed (Quartz) execution of code which is
 * dependent on the Spring/Hibernate environment. It establishes the Hibernate session, and passes
 * the application context to ServiceFinder so "findBean" works.
 * <p>
 * Subclasses MUST call our setUp() method when they begin execution, and tearDown() when
 * they have completed. (The method names follow the JUnit TestCase convention.)
 * <p>
 * By virtue of being a subclass of ApplicationObjectSupport, we have immediate access
 * to our ApplicationContext instance.
 */
public abstract class SpringBatch extends ApplicationObjectSupport {

	private static Log log = LogFactory.getLog(SpringBatch.class);
	protected SessionFactory sessionFactory = null;
	private boolean unbindResource = false;

	/**
	 * Default constructor
	 */
	public SpringBatch() {
	}

	protected void setUp() {
		log.info("-----  setup   ---- vvvvvvvvvvvvvvvvvvvvvvvvvv SpringBatch setup ");
		if (ServiceFinder.getBatchApplicationContext() == null) {
			// pass our context to ServiceFinder for use in "findBean".
			// If we're running under JUnit, then SpringTestCase should have set it already.
			ServiceFinder.setBatchApplicationContext(getApplicationContext());
		}

		if (sessionFactory != null) {
			// Establish the hibernate (or whatever JPA is specified) session;
			// the sessionFactory is set via the bean definition in the applicationContext xml.
			Session session = SessionFactoryUtils.getSession(sessionFactory, true);
			if (TransactionSynchronizationManager.getResource(sessionFactory) == null) {
				// (could have been set by SpringTestCase if running as JUnit.)
				unbindResource = true;
				TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
			}
		}
		//log.debug("Context established");
	}

	protected void tearDown() {
		log.info("----- teardown ---- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ SpringBatch complete ");
		if (sessionFactory != null) {
			if (unbindResource) {
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
				SessionFactoryUtils.releaseSession(sessionHolder.getSession(), sessionFactory);
			}
		}
	}

	/** See {@link #sessionFactory}. */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/** See {@link #sessionFactory}. */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

//	public void setCurrentProject(Project project) {
//		SessionUtils.setCurrentProject(project);
//	}

}
