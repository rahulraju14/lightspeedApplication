package com.lightspeedeps.batch;

import java.util.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.message.DoNotification.I9Message;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * This class is designed to run as a scheduled task.  It will find I-9 forms
 * that are waiting for approval, and send email notifications to the approver
 * to tell them about the I-9(s) that are waiting for their approval.
 */
public class FormI9Check extends SpringBatch {

	private static Log log = LogFactory.getLog(FormI9Check.class);

	//private ScheduleUtils scheduleUtils;
	private DoNotification notifier;
	private AuthorizationBean authBean;

	private transient NotificationDAO notificationDAO;
	private transient ProjectDAO projectDAO;

	private Date currentDate;	// today's date only, time=12:00am
	private Calendar currentCal;
	private Calendar tomorrow;

	public FormI9Check() {
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
			EventUtils.logError(" FormI9Check initialization error = ", e);
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
	 * Do the Advance Script pages processing for all the projects
	 * in the given Production.
	 */
	private void doProduction(Production prod) {
		log.debug("production = " + prod.getId());
		try {
			Map<String, Object> values = new HashMap<>();
			values.put("production", prod);
			List<ContactDocument> i9ContactDocList = ContactDocumentDAO.getInstance().
					findByNamedQuery(ContactDocument.GET_SUBMITTED_I9_CONTACT_DOCUMENT_LIST_BY_PRODUCTION, values);
			List<ContactDocEvent> events;

			// Map key is contact id instead of approver id, since the approver ids are different for
			// different commercial projects and using approver id would send separate notification for each project.
			Map<Integer,List<I9Message>> mapofContactI9Messages = new HashMap<>();

			for (ContactDocument i9Doc : i9ContactDocList) {
				Integer days = 0;
				log.debug("contactDocumentId = " + i9Doc.getId());

				if (prod.getType().isAicp()) {
					// Skip notifications for document if its Project has them turned off
					if (! i9Doc.getProject().getNotifying()) {
						continue;
					}
				}

				events = ContactDocEventDAO.getInstance().findByProperty("contactDocument", i9Doc);
				if (events != null && ! events.isEmpty() && i9Doc.getApproverId() != null) {
					// (approverId is only null due to other bugs allowing that to happen!)
					for (ContactDocEvent event : events) {

						if (event.getType().equals(TimedEventType.SUBMIT)) {
							Calendar c = Calendar.getInstance();
							c.setTime(event.getDate());
							c.add(Calendar.DATE, 3);
							days = CalendarUtils.durationInDays(currentDate, c.getTime());
							log.debug("date of expiration = " + c.getTime());

							I9Message i9Message;
							User user = i9Doc.getContact().getUser();
							String projTitle = "";
							if (prod.getType().isAicp()) {
								projTitle = i9Doc.getProject().getTitle();
							}
							i9Message = new I9Message(user.getFirstName(), user.getLastName(),
									i9Doc.getEmployment().getOccupation(), days, projTitle);

							if (i9Doc.getApproverId() < 0) {
								Integer pathId = - (i9Doc.getApproverId());
								Set<Contact> contactPool = new HashSet<>();
								if (i9Doc.getIsDeptPool()) { // Department pool, Group Id
									ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(pathId);
									if (approverGroup != null) {
										contactPool = approverGroup.getGroupApproverPool();
									}
								}
								else { // Production pool, Path Id
									ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathId);
									if (path != null) {
										contactPool = path.getApproverPool();
									}
								}
								if (contactPool != null) {
									for (Contact con : contactPool) {
										if (mapofContactI9Messages.containsKey(con.getId())) {
											mapofContactI9Messages.get(con.getId()).add(i9Message);
										}
										else {
											if (con.getNotifyForPendingI9()) {
												List<I9Message> i9MessageList = new ArrayList<>();
												i9MessageList.add(i9Message);
												mapofContactI9Messages.put(con.getId(),
														i9MessageList);
											}
										}
									}
								}
							}
							else {
								Approver approver = ApproverDAO.getInstance().findById(i9Doc.getApproverId());
								log.debug("Approver id= " + approver.getId());
								log.debug("Contact id= " + approver.getContact().getId());
								if (approver != null) {
									log.debug("i9 Document = " + i9Doc.getId());
									log.debug("Date of Submission = " + event.getDate());
									log.debug("Days left = " + days);

									Contact contact = approver.getContact();
									// Check whether the approver is already in the map or not.
									if (mapofContactI9Messages.containsKey(contact.getId())) {
										mapofContactI9Messages.get(contact.getId()).add(i9Message);
									}
									else {
										if (contact.getNotifyForPendingI9()) {
											List<I9Message> i9MessageList = new ArrayList<>();
											i9MessageList.add(i9Message);
											mapofContactI9Messages.put(contact.getId(), i9MessageList);
										}
									}
								}
							}
						}
					}
				}
			}

			if (mapofContactI9Messages != null && ! mapofContactI9Messages.isEmpty()) {
				List<I9Message> messageList;
				for (Integer contactId : mapofContactI9Messages.keySet()) {
					messageList = mapofContactI9Messages.get(contactId);
					Collections.sort(messageList, I9Message.getI9Comparator());
					notifier.formI9Waiting(contactId, messageList);
				}
			}
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
