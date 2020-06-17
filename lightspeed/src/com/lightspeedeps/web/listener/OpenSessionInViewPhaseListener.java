package com.lightspeedeps.web.listener;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.jsf.FacesContextUtils;
/**
 * This class is responsible for creating and closing the Hibernate
 * session which corresponds to the http request.
 *
 * It's possible we could use the Spring-supplied filter class to
 * do the same service (org.springframework.orm.hibernate3.support.OpenSessionInViewFilter).
 */
public class OpenSessionInViewPhaseListener implements PhaseListener {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(OpenSessionInViewPhaseListener.class);

	/**
	 *
	 */
	private static final long serialVersionUID = -3263031646854899605L;

	@SuppressWarnings("rawtypes")
	private static ThreadLocal ts = new ThreadLocal();

	private class ThreadData {
		private boolean participate;
		private Session session;
		private SessionFactory sessionFactory;
		private int beforeTimes;
		private int afterTimes;
		@Override
		public String toString() {
			String str = super.toString();
			str += "[session=" + session.getClass() + "@" + session.hashCode() +
					", bt=" + beforeTimes +
					", at=" + afterTimes +
					", p=" + participate +
					"]";
			return str;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void beforePhase(PhaseEvent pe) {
		ThreadData td = (ThreadData) ts.get();
		if (td == null) {
			td = new ThreadData();
		}
		else {
			td.beforeTimes++;
			return;
		}
		if (pe.getPhaseId() == PhaseId.RESTORE_VIEW) {
			td.sessionFactory = lookupSessionFactory(pe.getFacesContext());
			td.session = null;
			td.participate = false;

			if (isSingleSession()) {
				// single session mode
				if (TransactionSynchronizationManager
						.hasResource(td.sessionFactory)) {
					// Do not modify the Session: just set the participate flag.
					td.participate = true;
				}
				else {
					td.session = getSession(td.sessionFactory);
					TransactionSynchronizationManager.bindResource(
							td.sessionFactory, new SessionHolder(td.session));
				}
			}
			else {
				// deferred close mode
				if (SessionFactoryUtils
						.isDeferredCloseActive(td.sessionFactory)) {
					// Do not modify deferred close: just set the participate
					// flag.
					td.participate = true;
				}
				else {
					SessionFactoryUtils.initDeferredClose(td.sessionFactory);
				}
			}
			//log.debug(td);

		}
		ts.set(td);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterPhase(PhaseEvent pe) {
		if (pe.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			ThreadData td = (ThreadData) ts.get();
			if (td == null)
				return;
			if (td.afterTimes != 0) {
				td.afterTimes++;
				return;
			}
			td.afterTimes++;
			if (!td.participate) {
				//log.debug(td);
				if (isSingleSession()) {
					// single session mode
					TransactionSynchronizationManager
							.unbindResource(td.sessionFactory);
					try {
						closeSession(td.session, td.sessionFactory);
					}
					catch (RuntimeException ex) {
						ex.printStackTrace();
					}
				}
				else {
					// deferred close mode
					SessionFactoryUtils.processDeferredClose(td.sessionFactory);
				}
			}
			ts.set(null);
		}
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;

	private boolean singleSession = true;

	/**
	 * Set the bean name of the SessionFactory to fetch from Spring's root
	 * application context. Default is "sessionFactory".
	 *
	 * @see #DEFAULT_SESSION_FACTORY_BEAN_NAME
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * Return the bean name of the SessionFactory to fetch from Spring's root
	 * application context.
	 */
	protected String getSessionFactoryBeanName() {
		return sessionFactoryBeanName;
	}

	/**
	 * Set whether to use a single session for each request. Default is "true".
	 * <p>
	 * If set to false, each data access operation or transaction will use its
	 * own session (like without Open Session in View). Each of those sessions
	 * will be registered for deferred close, though, actually processed at
	 * request completion.
	 *
	 * @see SessionFactoryUtils#initDeferredClose
	 * @see SessionFactoryUtils#processDeferredClose
	 */
	public void setSingleSession(boolean singleSession) {
		this.singleSession = singleSession;
	}

	/**
	 * Return whether to use a single session for each request.
	 */
	protected boolean isSingleSession() {
		return singleSession;
	}

	/**
	 * Look up the SessionFactory that this filter should use.
	 * <p>
	 * Default implementation looks for a bean with the specified name in
	 * Spring's root application context.
	 *
	 * @return the SessionFactory to use
	 * @see #getSessionFactoryBeanName
	 */
	protected SessionFactory lookupSessionFactory(FacesContext facesContext) {
		WebApplicationContext wac = FacesContextUtils
				.getRequiredWebApplicationContext(facesContext);
		return (SessionFactory) wac.getBean(getSessionFactoryBeanName(),
				SessionFactory.class);
	}

	/**
	 * Get a Session for the SessionFactory that this filter uses. Note that
	 * this just applies in single session mode!
	 * <p>
	 * The default implementation delegates to SessionFactoryUtils' getSession
	 * method and sets the Session's flushMode to NEVER.
	 * <p>
	 * Can be overridden in subclasses for creating a Session with a custom
	 * entity interceptor or JDBC exception translator.
	 *
	 * @param sessionFactory
	 *            the SessionFactory that this filter uses
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException
	 *             if the Session could not be created
	 * @see org.springframework.orm.hibernate3.SessionFactoryUtils#getSession(SessionFactory,
	 *      boolean)
	 * @see org.hibernate.FlushMode#NEVER
	 */
	protected Session getSession(SessionFactory sessionFactory)
			throws DataAccessResourceFailureException {
		Session session = SessionFactoryUtils.getSession(sessionFactory, true);
		session.setFlushMode(FlushMode.AUTO);
		return session;
	}

	/**
	 * Close the given Session. Note that this just applies in single session
	 * mode!
	 * <p>
	 * The default implementation delegates to SessionFactoryUtils'
	 * releaseSession method.
	 * <p>
	 * Can be overridden in subclasses, e.g. for flushing the Session before
	 * closing it. See class-level javadoc for a discussion of flush handling.
	 * Note that you should also override getSession accordingly, to set the
	 * flush mode to something else than NEVER.
	 *
	 * @param session
	 *            the Session used for filtering
	 * @param sessionFactory
	 *            the SessionFactory that this filter uses
	 */
	protected void closeSession(Session session, SessionFactory sessionFactory) {
		SessionFactoryUtils.releaseSession(session, sessionFactory);
	}

}