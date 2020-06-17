package com.lightspeedeps.batch;

import java.util.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

public class DocumentDistributor extends SpringBatch {

	private static final Log log = LogFactory.getLog(DocumentDistributor.class);
	//private ScheduleUtils scheduleUtils;
	@SuppressWarnings("unused")
	private DoNotification notifier;
	private AuthorizationBean authBean;

	private transient NotificationDAO notificationDAO;
	private transient ProjectDAO projectDAO;

	private Date currentDate;	// today's date only, time=12:00am
	private Calendar currentCal;
	private Calendar tomorrow;

	public DocumentDistributor() {
		log.debug("");
	}

	@Override
	protected void setUp() {
		log.info("");
		super.setUp();
		try {
			currentCal = Calendar.getInstance();
			CalendarUtils.setStartOfDay(currentCal);
			currentDate = currentCal.getTime(); // date only, time=12:00:00am
			tomorrow = CalendarUtils.getInstance(currentDate);
			tomorrow.add(Calendar.DAY_OF_MONTH, 1);
			notifier = (DoNotification)ServiceFinder.findBean("NotificationBean");

			if (authBean == null) { // test invocation probably
				log.info("self-initialization required"); // only if not run as a "bean"
				authBean = AuthorizationBean.getInstance();
			}
		}
		catch (Exception e) {
			EventUtils.logError(" DocumentDistributor initialization error = ", e);
		}
	}

	/**
	 * Called via a scheduled job, such as a Quartz task. (See file
	 * applicationContextScheduler.xml.)
	 * <p>
	 * This method loops through all the the Productions.
	 *
	 * @return "success", unless an exception is caught, in which case "failure"
	 *         is returned.
	 */
	public String execute() {
		log.debug(" <><><><><><> DOCUMENT DISTRIBUTOR <><><><><><><> " );
		String result = Constants.SUCCESS;
		setUp();
		try {
			if (getProjectDAO().checkDb()) {
				List<Production> prods = ProductionDAO.getInstance().findByNamedQuery(
						Production.GET_PRODUCTION_LIST_BY_ACTIVE_STATUS_AND_ALLOW_ONBOARDING);
				for (Production prod : prods) {
					if (prod.getNotify()) { // notifications are on for the production
						doProduction(prod);
					}
				}
			}
		}
		catch (Exception e) {
			result = Constants.FAILURE;
			EventUtils.logError(e);
		}
		finally {
			tearDown();
		}
		return result;
	}

	/**
	 * Do the document distribution processing for all the projects
	 * in the given Production.
	 */
	private void doProduction(Production prod) {

		log.debug("production = " + prod.getId());
		try {
			/*List<Employment> recipientEmplList ;
			String packetName;*/
			//TODO
		}

		catch (Exception e) {
			EventUtils.logEvent(EventType.APP_ERROR, prod, null, null,
					null + Constants.NEW_LINE + EventUtils.traceToString(e) );
		}
	}

	/** See {@link #notificationDAO}. */
	public NotificationDAO getNotificationDAO() {
		if (notificationDAO == null) {
			notificationDAO = NotificationDAO.getInstance();
		}
		return notificationDAO;
	}
	/** See {@link #notificationDAO}. */
	public void setNotificationDAO(NotificationDAO notificationDAO) {
		this.notificationDAO = notificationDAO;
	}

	/** See {@link #projectDAO}. */
	public ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}
	/** See {@link #projectDAO}. */
	public void setProjectDAO(ProjectDAO projectDao) {
		projectDAO = projectDao;
	}

	/** See {@link #authBean}. */
	public AuthorizationBean getAuthBean() {
		return authBean;
	}
	/** See {@link #authBean}. */
	public void setAuthBean(AuthorizationBean authBean) {
		this.authBean = authBean;
	}
}
