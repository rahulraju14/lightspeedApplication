package com.lightspeedeps.util.app;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * This class implements findBean(), used by numerous classes to locate
 * a bean instance.
 */
@SuppressWarnings("deprecation")
public class ServiceFinder extends ApplicationObjectSupport {
	private static final Log log = LogFactory.getLog(ServiceFinder.class);

	/** Context set when running in JUnit environment or under Quartz (scheduled) task. */
	private static ApplicationContext batchContext = null;

	/** Context set when processing servlet request, e.g., Authorize.net posting */
	private static ApplicationContext requestContext = null;

	private ServiceFinder() {
	}

	/**
	 * Find an instance of a managed bean. Note that all of our DAO-type beans,
	 * as well as many other beans which need to be "looked up" by other Java
	 * code, have getInstance() methods. It is preferable to call the class'
	 * getInstance() method rather than calling findBean, since the findBean()
	 * call requires knowledge of the bean's registered name whereas the
	 * getInstance() call does not.
	 *
	 * @param beanName The registered name of the bean. This is ALMOST always
	 *            the Java class name of the bean, with the first letter changed
	 *            to lower case.
	 * @return The bean instance.
	 * @throws IllegalArgumentException if the bean is not found.
	 */
	public static Object findBean(String beanName) {
		FacesContext context = null;
		Object bean = null;

		try {
			context = FacesContext.getCurrentInstance();
		}
		catch (Exception e) {
			log.warn("FacesContext.getCurrentInstance() failed: " + e.getLocalizedMessage());
		}

		if (context != null) {
			ApplicationContext appContext = FacesContextUtils.getWebApplicationContext(context);
			// Favor appContext resolution, as it supports @Autowired
			try {
				bean = appContext.getBean(beanName);
			}
			catch (BeansException e) {
			}
			if (bean == null) {
				//log.debug("app resolution failed for '" + beanName + "', trying faces resolution");
				// This works for beans that are only in faces-config.xml; but @Autowired will be ignored
				bean = context.getApplication().getELResolver().getValue(context.getELContext(), null, beanName);
				//bean = context.getApplication().getVariableResolver().resolveVariable(context, beanName);
			}
		}
		else if (requestContext != null) { // For non-faces requests, e.g. Authorize.net or incoming SMS
			// Note that this will not find beans that are only defined in faces-config.xml.
			try {
				bean = requestContext.getBean(beanName);
				// log.debug("using requestContext; bean=" + bean);
			}
			catch (Exception e) {
			}
		}
		else if (batchContext != null) { // Used for JUnit and Quartz-scheduled executions
			// Note that this will not find beans that are only defined in faces-config.xml.
			try {
				bean = batchContext.getBean(beanName);
				//log.debug("using batchContext; bean=" + bean);
			}
			catch (Exception e) {
			}
		}

		if (bean == null) { // failed resolution
			log.error("findBean resolution failed for " + beanName);
			//EventUtils.logError("findBean resolution failed for " + beanName);
			// give developer a hint ...
			if (context == null) {
				if (requestContext != null) {
					log.error("findBean cannot resolve beans defined only in faces-config.xml when running in servlet environment.");
				}
				else if (batchContext != null) {
					log.error("findBean cannot resolve beans defined only in faces-config.xml when running in JUnit/Quartz environment.");
				}
				else {
					log.error("No current context found.");
				}
			}
			throw(new IllegalArgumentException());
		}
		return bean;
	}

	public static Object findBean(String beanName, ServletContext servletContext ) {
		//log.debug("servletContext="+servletContext);
		ApplicationContext appContext =
				WebApplicationContextUtils.getWebApplicationContext(servletContext);
		//log.debug("appContext="+appContext);

		Object o = null;
		try {
			o = appContext.getBean(beanName);
		}
		catch (BeansException e) {
			log.error("bean not found: '" + beanName + "'");
		}

		return o;
	}

	/**
	 * Find a Icefaces-managed bean.  This should be used for web-page-backing
	 * beans -- ones that are listed in faces-config.xml.
	 * @param beanName The bean name as listed in faces-config.
	 * @return The bean reference.
	 */
	public static Object findFacesBean(String beanName) {
		FacesContext context= FacesContext.getCurrentInstance();
		//Object o = context.getApplication().getVariableResolver().resolveVariable(context, beanName);
		Object o = context.getApplication().getELResolver().getValue(context.getELContext(), null, beanName);
		return o;
	}

	/**
	 * Get managed bean based on the bean name.
	 *
	 * @param beanName the bean name
	 * @return the managed bean associated with the bean name
	 */
	public static Object getManagedBean(String beanName) {

		return getValueBinding(getJsfEl(beanName)).getValue(FacesContext.getCurrentInstance());
	}

	private static ValueBinding getValueBinding(String el) {
		return getApplication().createValueBinding(el);
	}

	private static Application getApplication() {
		ApplicationFactory appFactory = (ApplicationFactory) FactoryFinder
				.getFactory(FactoryFinder.APPLICATION_FACTORY);
		return appFactory.getApplication();
	}

	private static String getJsfEl(String value) {
		return "#{" + value + "}";
	}


	/** See {@link #requestContext}. */
	public static ApplicationContext getRequestContext() {
		return requestContext;
	}
	/** See {@link #requestContext}. */
	public static void setRequestContext(ApplicationContext requestContext) {
		ServiceFinder.requestContext = requestContext;
	}

	/** See {@link #batchContext}. */
	public static ApplicationContext getBatchApplicationContext() {
		return batchContext;
	}
	/** See {@link #batchContext}. */
	public static void setBatchApplicationContext(ApplicationContext context) {
		batchContext = context;
		log.debug("batch context set: " + context);
	}

}
