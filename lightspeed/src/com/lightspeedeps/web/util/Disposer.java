/**
 * Disposer.java
 */
package com.lightspeedeps.web.util;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This session-scoped bean keeps track of ViewScoped beans which need
 * to have a dispose() method called at session expiration.
 * <p>
 * Due to an oversight in the JSF 2.1 spec, the dispose method of a ViewScoped
 * bean is not called during session expiration.  This is supposed to be
 * fixed in JSF 2.2.
 * <p>
 * In the meantime, those few ViewScoped beans of ours which have a dispose
 * method 'register' themselves with this bean, and it calls their dispose
 * methods when it gets destroyed.
 */
@ManagedBean
@SessionScoped
public class Disposer {
	private static final Log log = LogFactory.getLog(Disposer.class);

	private final Collection<Disposable> beans = new HashSet<Disposable>();

	/**
	 * @return the current (session-scoped) instance of this bean.
	 */
	public static Disposer getInstance() {
		return (Disposer)ServiceFinder.findBean("disposer");
	}

	/**
	 * Register a bean whose dispose() method should be called when the user's
	 * session expires.
	 *
	 * @param bean
	 */
	public void register(Disposable bean) {
		boolean b = beans.add(bean);
		log.debug("added=" + b + ", reg'd=" + beans.size() + ", bean=" + bean);
	}

	/**
	 * Unregister a previously registered bean. No error occurs if the bean was
	 * not previously registered or has already been unregistered.
	 *
	 * @param bean
	 */
	public void unregister(Disposable bean) {
		boolean b = beans.remove(bean);
		log.debug("removed=" + b + ", reg'd=" + beans.size() + ", bean=" + bean);
	}

	/**
	 * The method called by the JSF framework when the user's session is being
	 * destroyed. This will call the dispose() method of any 'registered' beans.
	 */
	@PreDestroy
	public void dispose() {
		log.debug("reg'd=" + beans.size());
		for (Disposable obj : beans) {
			try {
				log.debug(obj);
				obj.dispose();
			}
			catch (Exception e) {
				log.error(e);
			}
		}
		beans.clear();
	}

}
