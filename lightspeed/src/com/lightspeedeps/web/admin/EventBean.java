package com.lightspeedeps.web.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EventDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Event;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * Class that supports the display and selection of Event entities (from the
 * "event" table).
 */
@ManagedBean
@ViewScoped
public class EventBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 933732554170644305L;

	private static final Log log = LogFactory.getLog(EventBean.class);

	@SuppressWarnings("unused")
	private final static int TAB_LIST = 0;
	private final static int TAB_DETAIL = 1;

	// Fields

	/** The currently displayed List of Event`s */
	private List<Event> eventList;

	/** The available EventType values. */
	private List<EventType> eventTypes;

	/** Checkboxes for each possible EventType */
	private ArrayList<Boolean> eventTypeCbs;

	/** List of the names of the selected EventType values. */
	private List<EventType> selectedEventTypes = new ArrayList<>();

	/** The currently selected (Detail tab) Event. */
	private Event event;

	/** The User associated with the currently selected (Detail) Event,
	 * or null if the User object was not found. */
	private User user;

	/** Starting date (inclusive) for range selected by the user to be displayed. */
	private Date startDate = null;

	/** Ending date (inclusive) for range selected by the user to be displayed. */
	private Date endDate = null;

	private boolean showAllProductions = false;

	/** The tab number, origin zero, of the currently selected tab. */
	private int selectedTab;

	private Project errorProject;
	private Production errorProduction;

	private transient EventDAO eventDAO;

	/* Constructor */
	public EventBean() {
		log.debug("");
		try {
			endDate = SessionUtils.getDate(Constants.ATTR_EVENT_END);
			if (endDate == null) {
				endDate = new Date();
			}
			startDate = SessionUtils.getDate(Constants.ATTR_EVENT_START);
			if (startDate == null || startDate.after(endDate)) {
				Calendar date = new GregorianCalendar();
				date.setTime(endDate);
				date.add(Calendar.DAY_OF_MONTH, -1);
				startDate = date.getTime();
			}

			eventTypes = new ArrayList<>();
			eventTypeCbs = new ArrayList<>();
			for (EventType type : EventType.values()) {
				eventTypes.add(type);
				eventTypeCbs.add(new Boolean(false));
			}
			errorProject = new Project();
			errorProject.setTitle("project not found; deleted?");

			errorProduction = new Production("production not found; deleted?");

			doQuery();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		try {
			if (event == null) {
				if (eventList.size() > 0) {
					event = eventList.get(0);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("EventBean failed Exception: ", e);
		}
	}

	public static EventBean getInstance() {
		return (EventBean)ServiceFinder.findBean("eventBean");
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 */
	private void forceLazyInit() {
		if (event != null) {
			event = EventDAO.getInstance().refresh(event);
		}
		for (Event evt : eventList) {
			initEvent(evt);
		}
		if (event != null) {
			initEvent(event);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 * @param evt
	 */
	private void initEvent(Event evt) {
		try {
			if (evt.getProject() != null) {
				evt.getProject().getTitle();
			}
		}
		catch (Exception e) { // catch errors if project has been deleted.
			evt.setProject(errorProject);
			log.warn("error fetching project (deleted?): " + e.getLocalizedMessage());
		}
		try {
			if (evt.getProduction() != null) {
				evt.getProduction().getTitle();
			}
		}
		catch (Exception e) { // catch errors if production has been deleted.
			evt.setProduction(errorProduction);
		}
		return;
	}

	/**
	 * Create a query matching the user's selections, and execute it.
	 */
	private void doQuery() {
		SessionUtils.put(Constants.ATTR_EVENT_END, endDate);
		SessionUtils.put(Constants.ATTR_EVENT_START, startDate);

		List<Object> values = new ArrayList<>();
		String query = createQuery(values);

		eventList = getEventDAO().findByQuery(query, values);
		log.debug("# of results: " + eventList.size());

		forceLazyInit();
	}

	/**
	 * Action method for the "refresh" button.
	 * @return null navigation string
	 */
	public String actionSearch() {
		doQuery();
		if (eventList.size() > 0) {
		}
		return null;
	}

	/**
	 * Value Change Listener for check box selecting whether to
	 * show "all productions" or not.
	 */
	public void listenShowAll(ValueChangeEvent evt) {
		try {
			showAllProductions = (Boolean)evt.getNewValue();
			doQuery();	// Refresh the list when the checkbox changes
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ActionListener method when the user clicks on a particular detail entry.
	 * An f:attribute tag has set the 'currentRow' attribute to the Event of
	 * interest.
	 *
	 * @param evt ActionEvent created by the framework.
	 */
	public void actionEventView(ActionEvent evt) {
		try {
			setEvent((Event)evt.getComponent().getAttributes().get("currentRow"));
			setSelectedTab(TAB_DETAIL);
			user = null;
			if (event != null) {
				String email = event.getUsername();
				user = UserDAO.getInstance().findOneByProperty(UserDAO.EMAIL_ADDRESS, email);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private String createQuery(List<Object> values) {
		String q = "";
		if (! showAllProductions) {
			q = "( e.production = ? ) ";
			values.add(SessionUtils.getProduction());
		}

		q = addQuery( q, "e.startTime >= ? " );
		startDate = CalendarUtils.setTime(startDate, 0, 0);
		values.add(startDate);

		q = addQuery( q, "e.startTime < ? " );
		Calendar date = new GregorianCalendar();
		endDate = CalendarUtils.setTime(endDate, 0, 0);
		date.setTime(endDate);
		date.add(Calendar.DAY_OF_MONTH, 1);
		values.add(date.getTime());

		if (getSelectedEventTypes().size() > 0) {
			q = addQuery( q, createListQuery(getSelectedEventTypes(), "e.type") );
		}

		q += " order by startTime desc, id desc";

		return q;
	}

//	private String addDateQueries(String q) {
//		Calendar date = new GregorianCalendar();
//		date.setTime(startDate);
//		q = addQuery( q, "e.startTime >= " + CalendarUtils.sqlDate(date));
//		date.setTime(endDate);
//		date.add(Calendar.DAY_OF_MONTH, 1);
//		q = addQuery( q, "e.startTime < " + CalendarUtils.sqlDate(date));
//		return q;
//	}

	private String createListQuery(List<EventType> selectedEvents, String field) {
		String s = "";
		if (selectedEvents.size() > 0) {
			for (EventType item : selectedEvents) {
				s = s + ", '" + item.name() + "'";
			}
			s = s.substring(1);
			s = field + " in (" + s + ")";
		}
		return s;
	}

	private String addQuery(String q1, String q2) {
		if ( q1.length() > 0) {
			if (q2.length() > 0) {
				q1 += " and " + q2;
			}
			return q1;
		}
		return q2;
	}

	private List<EventType> getSelectedEventTypes() {
		int ix = 0;
		selectedEventTypes.clear();
		for (Boolean b : eventTypeCbs) {
			if (b) {
				selectedEventTypes.add(eventTypes.get(ix));
			}
			ix++;
		}
		return selectedEventTypes;
	}

	public void setEventList(List<Event> eventList) {
		this.eventList = eventList;
	}
	public List<Event> getEventList() {
		return eventList;
	}

	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}

	/** See {@link #user}. */
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	/** See {@link #showAllProductions}. */
	public boolean getShowAllProductions() {
		return showAllProductions;
	}
	/** See {@link #showAllProductions}. */
	public void setShowAllProductions(boolean showAllProductions) {
		this.showAllProductions = showAllProductions;
	}

	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public List<EventType> getEventTypes() {
		return eventTypes;
	}

	/** See {@link #eventTypeCbs}. */
	public ArrayList<Boolean> getEventTypeCbs() {
		return eventTypeCbs;
	}
	/** See {@link #eventTypeCbs}. */
	public void setEventTypeCbs(ArrayList<Boolean> eventTypeCbs) {
		this.eventTypeCbs = eventTypeCbs;
	}

	/** See {@link #selectedTab}. */
	public int getSelectedTab() {
		return selectedTab;
	}
	/** See {@link #selectedTab}. */
	public void setSelectedTab(int n) {
		//log.debug("n=" + n + ", current=" + selectedTab);
		if (selectedTab != n) {
			if (n == TAB_DETAIL) {
				forceLazyInit();
			}
		}
		selectedTab = n;
	}

	private EventDAO getEventDAO() {
		if (eventDAO == null) {
			eventDAO = EventDAO.getInstance();
		}
		return eventDAO;
	}

}
