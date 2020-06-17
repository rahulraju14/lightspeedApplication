/**
 * File: ApplicationContextListener.java
 */
package com.lightspeedeps.web.util;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.FF4JConfig;
import com.lightspeedeps.util.app.ServiceFinder;
import com.pdftron.pdf.PDFNet;

/**
 * Terminate the PDFNet instance so there is not an extra instance of PDFNet when
 * Tomcat restarts.
 */
@WebListener
public class ApplicationContextListener	implements ServletContextListener {
	private static final Log LOG = LogFactory.getLog(ApplicationContextListener.class);

	/**
	 * Actions to perform when the application closes:
	 * - Terminate/free the PDFNet library resources;
	 * - Shutdown any Quartz threads;
	 * - Shutdown any JDBC drivers;
	 * - Shutdown FF4J
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {

		// Clean up PDFNet (PDFTron library)
		try {
			PDFNet.terminate();
			LOG.info("PDFNet Terminated.");
		}
		catch (Exception | UnsatisfiedLinkError e) {
			LOG.error("Error terminating PDFNet library.", e);
		}

		// Clean up Quartz scheduling tasks
		try {
			ApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
			ServiceFinder.setRequestContext(appContext);
			/** get Quartz scheduler created via XML in application context */
			Scheduler quartzScheduler = ApplicationScopeBean.getInstance().getQuartzScheduler();
			Scheduler scheduler = quartzScheduler;
			if (scheduler != null) {
				LOG.info("Calling referenced Quartz Scheduler shutdown");
				scheduler.shutdown(true);
			}

			scheduler = StdSchedulerFactory.getDefaultScheduler();
			if (scheduler != null) {
				LOG.info("Calling default Quartz Scheduler shutdown");
				scheduler.shutdown(true);
			}
		}
		catch (Exception e) {
			LOG.error("Error shutting down Quartz scheduler.", e);
		}

		// Clean up JDBC driver(s)
		try {
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				Driver driver = drivers.nextElement();
				LOG.info(String.format("deregistering JDBC driver: %s", driver));
				DriverManager.deregisterDriver(driver);
			}
		}
		catch (Exception e) {
			LOG.error("Error deregistering JDBC drivers", e);
		}

		// Cleanup FF4J
		/*try {
			LOG.info("Stopping FF4J");
			FF4JConfig.getFF4j().stop();
		}
		catch (Exception e) {
			LOG.error("Error stopping FF4J", e);
		}*/
	}

	/**
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("ApplicationListener called.");

		// ensure that we have a context available if needed
        ServletContext context = sce.getServletContext();
        ApplicationUtils.setRequestContext(context);
	}

}
