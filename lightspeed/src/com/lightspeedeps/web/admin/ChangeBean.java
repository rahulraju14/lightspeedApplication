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
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ChangeDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Change;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * Class that supports the display and selection of Change entities (from the
 * "changes" table). (This is one of the few cases where the table name,
 * "changes", does not match the model name, "Change", because "change" is not
 * allowed as a MySQL table name -- it's a reserved word.)
 */
@ManagedBean
@ViewScoped
public class ChangeBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 933732554170644305L;

	private static final Log log = LogFactory.getLog(ChangeBean.class);

	@SuppressWarnings("unused")
	private final static int CHANGE_TAB_LIST = 2;
	private final static int CHANGE_TAB_DETAIL = 3;

	// Fields

	/** The currently displayed List of Change`s */
	private List<Change> changeList;

	/** The available ChangeType values. */
	private List<ChangeType> changeTypes;

	/** Checkboxes for each possible EventType */
	private ArrayList<Boolean> changeTypeCbs;

	/** List of the names of the selected ChangeType values. */
	private List<ChangeType> selectedChangeTypes = new ArrayList<>();

	/** The currently selected (Detail tab) Change. */
	private Change change;

	/** The User associated with the currently selected (Detail) Change,
	 * or null if the User object was not found. */
	private User user;

	/** Starting date (inclusive) for range selected by the user to be displayed. */
	private Date startDate = null;

	/** Ending date (inclusive) for range selected by the user to be displayed. */
	private Date endDate = null;

	private boolean showAllProductions = true;

	/** The tab number, origin zero, of the currently selected tab
	 * -- is shared with EventBean, as the Change minitabs are on the same page as Events */
//	private int selectedTab;

	private Production errorProduction;

	private transient ChangeDAO changeDAO;

	/** Select Item list holds the value of the ActionType enum for the action filter. */
	@SuppressWarnings("serial")
	private List<SelectItem> actionTypeDL = new ArrayList<SelectItem>() {{
		add(new SelectItem(""));
		add(new SelectItem(ActionType.CREATE));
		add(new SelectItem(ActionType.DELETE));
		add(new SelectItem(ActionType.UPDATE));
	}};

	/* Constructor */
	public ChangeBean() {
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

			changeTypes = new ArrayList<>();
			changeTypeCbs = new ArrayList<>();
			for (ChangeType type : ChangeType.values()) {
				changeTypes.add(type);
				changeTypeCbs.add(new Boolean(false));
			}

			errorProduction = new Production("production not found; deleted?");

			doQuery();
			//actionTypeDL = new ArrayList<SelectItem>(EnumList.getActionTypeList());
			//actionTypeDL.add(0, new SelectItem(null, ""));
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		try {
//			Integer id = (Integer)SessionUtils.getInteger("rowId");
//			if (id != null) {
//				change = getEventDAO().findById(id);
//			}
			if (change == null) {
				if (changeList.size() > 0) {
					change = changeList.get(0);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("EventBean failed Exception: ", e);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 */
	private void forceLazyInit() {
		if (change != null) {
			change = ChangeDAO.getInstance().refresh(change);
		}
		for (Change evt : changeList) {
			initEvent(evt);
		}
		if (change != null) {
			initEvent(change);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 * @param evt
	 */
	private void initEvent(Change evt) {
		try {
			if (evt.getProject() != null) {
				evt.getProject().getTitle();
			}
		}
		catch (Exception e) { // catch errors if project has been deleted.
			Project errorProject = new Project();
			errorProject.setTitle("project " + evt.getProjectId() + " not found; deleted?");
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

		changeList = getChangeDAO().findByQuery(query, values);
		log.debug("# of results: " + changeList.size());

		forceLazyInit();
	}

	/**
	 * Action method for the "refresh" button.
	 * @return null navigation string
	 */
	public String actionSearch() {
		doQuery();
		if (changeList.size() > 0) {
		}
		return null;
	}

	/**
	 * Value Change Listener for check box selecting whether to
	 * show "all productions" or not.
	 */
	public void listenShowAll(ValueChangeEvent chg) {
		try {
			showAllProductions = (Boolean)chg.getNewValue();
			doQuery();	// Refresh the list when the checkbox changes
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ActionListener method when the user clicks on a particular detail entry.
	 * An f:attribute tag has set the 'currentRow' attribute to the Change of
	 * interest.
	 *
	 * @param evt ActionEvent created by the framework.
	 */
	public void actionChangeView(ActionEvent evt) {
		try {
			setChange((Change)evt.getComponent().getAttributes().get("currentRow"));
			setSelectedTab(CHANGE_TAB_DETAIL);
			user = null;
			if (change != null) {
				String email = change.getUserName();
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
			q = "( c.production = ? ) ";
			values.add(SessionUtils.getProduction());
		}

		q = addQuery( q, "c.startTime >= ? " );
		startDate = CalendarUtils.setTime(startDate, 0, 0);
		values.add(startDate);

		q = addQuery( q, "c.startTime < ? " );
		Calendar date = new GregorianCalendar();
		endDate = CalendarUtils.setTime(endDate, 0, 0);
		date.setTime(endDate);
		date.add(Calendar.DAY_OF_MONTH, 1);
		values.add(date.getTime());

		if (getSelectedChangeTypes().size() > 0) {
			q = addQuery( q, createListQuery(getSelectedChangeTypes(), "c.type") );
		}

		q += " order by startTime desc, id desc";

		return q;
	}

//	private String addDateQueries(String q) {
//		Calendar date = new GregorianCalendar();
//		date.setTime(startDate);
//		q = addQuery( q, "c.startTime >= " + CalendarUtils.sqlDate(date));
//		date.setTime(endDate);
//		date.add(Calendar.DAY_OF_MONTH, 1);
//		q = addQuery( q, "c.startTime < " + CalendarUtils.sqlDate(date));
//		return q;
//	}

	private String createListQuery(List<ChangeType> selectedTypes, String field) {
		String s = "";
		if (selectedTypes.size() > 0) {
			for (ChangeType item : selectedTypes) {
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

	public void setChangeList(List<Change> eventList) {
		changeList = eventList;
	}
	public List<Change> getChangeList() {
		return changeList;
	}

	public Change getChange() {
		return change;
	}
	public void setChange(Change change) {
		this.change = change;
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

	public List<ChangeType> getChangeTypes() {
		return changeTypes;
	}

	/** See {@link #changeTypeCbs}. */
	public ArrayList<Boolean> getChangeTypeCbs() {
		return changeTypeCbs;
	}
	/** See {@link #changeTypeCbs}. */
	public void setChangeTypeCbs(ArrayList<Boolean> changeTypeCbs) {
		this.changeTypeCbs = changeTypeCbs;
	}

	private List<ChangeType> getSelectedChangeTypes() {
		int ix = 0;
		selectedChangeTypes.clear();
		for (Boolean b : changeTypeCbs) {
			if (b) {
				selectedChangeTypes.add(changeTypes.get(ix));
			}
			ix++;
		}
		return selectedChangeTypes;
	}

	/** @return the currently selected mini-tab, which is actually
	 * maintained by the EventBean, since we share the same JSP page. */
	public int getSelectedTab() {
		return EventBean.getInstance().getSelectedTab();
	}
	/**
	 * Called when the selected mini-tab has changed.
	 * @param n the minitab number, origin 0.
	 */
	public void setSelectedTab(int n) {
		//log.debug("n=" + n + ", current=" + selectedTab);
		EventBean eventBean = EventBean.getInstance();
		if (eventBean.getSelectedTab() != n) {
			if (n == CHANGE_TAB_DETAIL) {
				forceLazyInit();
			}
		}
		eventBean.setSelectedTab(n);
	}

	private ChangeDAO getChangeDAO() {
		if (changeDAO == null) {
			changeDAO = ChangeDAO.getInstance();
		}
		return changeDAO;
	}

	/** See {@link #actionTypeDL}. */
	public List<SelectItem> getActionTypeDL() {
		return actionTypeDL;
	}
	/** See {@link #actionTypeDL}. */
	public void setActionTypeDL(List<SelectItem> actionTypeDL) {
		this.actionTypeDL = actionTypeDL;
	}

}
