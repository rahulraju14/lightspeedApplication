/**
 * FilterBean.java
 */
package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.FilterType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This bean is used to manage the controls at the top of the approver
 * Dashboard, as they are shared across three other beans -- one for each
 * mini-tab (except that currently the Review and Gross Pay tabs use the same
 * bean). To do this, it holds the drop-down lists, the currently selected value
 * for each list, and listener methods for each list.
 * <p>
 * The FilterBean also tracks which mini-tab is currently selected, and restores
 * this mini-tab when the user returns to the Dashboard. To share
 * responsibility, the FilterBean needs to call back to the other beans, so
 * there is a "register()" method on the FilterBean which each of the other
 * beans calls. Rather than having separate "listener" callbacks for each
 * drop-down (filter) type, we have a single callback and pass a "FilterType"
 * enum to indicate which filter was changed by the user.
 */
@ManagedBean
@ViewScoped
public class FilterBean {
	private static final Log log = LogFactory.getLog(FilterBean.class);

	private static final int MAX_TABS = 6;

	/** Relative mini-tab (origin 0) for the "Start Forms" (list) mini-tab. */
	public static final int TAB_STARTS = 0;
	/** Relative mini-tab (origin 0) for the "Timecard Review" mini-tab. */
	public static final int TAB_REVIEW = 1;
	/** Relative mini-tab (origin 0) for the "Hot Costs" mini-tab. */
	public static final int TAB_HOT_COSTS = 4;
	/** Relative mini-tab (origin 0) for the "Gross Payroll" mini-tab. */
	public static final int TAB_GROSS = 2;
	/** Relative mini-tab (origin 0) for the "Batch Transfer" mini-tab. */
	public static final int TAB_TRANSFER = 3;
	public static final int TAB_START_STATUS = 1;
	/** Relative mini-tab (origin 0) for the "Update Rates" (admin, not used) mini-tab. */
	public static final int TAB_UPDATE_RATES = 5;

	private final FilterHolder[] holders = new FilterHolder[MAX_TABS];

	/**
	 * The (zero-origin) index of the mini-tab that is currently displayed.
	 */
	private int currentTab;

	private FilterType filterType = FilterType.DEPT;

	/** The List of SelectItem`s for the "Filter by:" drop-down, for example,
	 * Department, Batch, and Status. */
	private List<SelectItem> filterByList;

	/** The current week-ending date filter value; for "All", it
	 * is set to Constants.SELECT_ALL_DATE.  */
	private Date weekEndDate;

	/** End Date list contains SelectItem objects for available week-ending
	 *  dates. */
	private List<SelectItem> endDateList;

	/** The selection list for filtering.  The types of the entries will depend
	 * on the current 'filterType'.  E.g., it could be a list of Department`s,
	 * WeeklyBatch`s, etc. */
	private List<SelectItem> selectFilterList;

	/** The User.accountNumber of the currently selected employee, or "ALL" if
	 * the "All" entry has been selected (in the drop-down list). */
	private String employeeAccount = Constants.CATEGORY_ALL;

	/** The List of SelectItem`s for the drop-down list of employees to
	 * choose from.  The SelectItem.value field is the User.accountNumber of
	 * the employee, and the label field is in "last name, first name" style. */
	private List<SelectItem> employeeList;

	/** The currently selected value from the selectFilterList. This will match
	 * one of the SelectItem.value fields from the list. It is usually either an
	 * Integer or a String. Integer values are typically the database id of the
	 * object represented by the SelectItem. */
	private String selectFilterValue;

	/**
	 * Default (normal) constructor.
	 * <p>
	 * It gets the HeaderViewBean's mini-tab index, which will be used to
	 * determine which "registered holder" will receive callbacks. (When a holder
	 * registers, it indicates which mini-tab it is supporting.)
	 * that value.
	 * <p>
	 *  In the JSP, the HeaderViewBean's 'miniTab' value is used to force
	 * instantiation of the appropriate Approver bean(s) BEFORE any other
	 * FilterBean methods are called, in particular those that lazy-initialize
	 * the drop-down lists. By instantiating the Approver beans, they have a
	 * chance to register themselves with this FilterBean so their methods will
	 * be called to populate whatever drop-down lists are needed on that
	 * particular mini-tab.
	 */
	public FilterBean() {
		log.debug("");

		// Make the default mini-tab 1 = Timecard Review
		String attr = Constants.ATTR_CURRENT_MINI_TAB + ".tcapprover";
		if (SessionUtils.get(attr) == null) { // no saved minitab selection yet
			if (SessionUtils.getNonSystemProduction() != null &&
					SessionUtils.getNonSystemProduction().getType().isTours()) {
				HeaderViewBean.getInstance().setMiniTab(TAB_STARTS); // ... so go to tab 0
			}
			else {
				HeaderViewBean.getInstance().setMiniTab(TAB_REVIEW); // ... so go to tab 1
			}
		}

		currentTab = HeaderViewBean.getInstance().getMiniTab();

		filterType = (FilterType)SessionUtils.get(Constants.ATTR_FILTER_BY, FilterType.DEPT);

	}

	public static FilterBean getInstance() {
		return (FilterBean)ServiceFinder.findBean("filterBean");
	}

	public void register(FilterHolder holder, int tab) {
		holders[tab] = holder;
	}

	/**
	 * valueChangeListener for the "filter by" drop-down selection list.
	 *
	 * @param event
	 */
	public void listenFilterByChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue() + "old val = " + event.getOldValue());
			if (event.getNewValue() != null) {
				FilterType newFt = (FilterType)event.getNewValue();
				FilterType oldFt = (FilterType)event.getOldValue();
				changeFilterBy(oldFt, newFt);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Change the current "Filter By" setting -- used by the
	 * valueChangeListener, and also available to subclasses to change the
	 * filter.
	 *
	 * @param oldFt The existing FilterBy setting.
	 * @param newFt The value the FilterBy setting should be changed to.
	 */
	protected void changeFilterBy(FilterType oldFt, FilterType newFt) {
		filterType = newFt;
		selectFilterList = null; // force refresh
		setSelectFilterValue("0"); // something to select "All" or default, for any filter
		if (oldFt != null && holders[currentTab] != null) {
			holders[currentTab].dropFilter(oldFt);
		}
		SessionUtils.put(Constants.ATTR_FILTER_BY, newFt);
	}

	/**
	 * ValueChangeListener for filtering drop-down list. The values in this list
	 * will vary depending on the current "Filter By" setting. The new value in
	 * the event will be passed through to our registered "holders".
	 * <p>
	 * It is expected that this will be called during the INVOKE_APPLICATION
	 * phase!
	 *
	 * @param event contains old and new values, provided by framework.
	 */
	public void listenSelectFilterChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue() + ", ID =" +  event.getComponent().getId());
			if (event.getNewValue() != null &&
					selectFilterList != null) {
				// selectFilterList may be null if we got a 'changeFilterBy' event on the
				// same input cycle as this event, due to user doing some tricky keyboard
				// selection steps. rev 2.2.5013.
				notify(filterType, event.getNewValue());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener for Employee drop-down list. It is expected that this
	 * will be called during the INVOKE_APPLICATION phase!
	 *
	 * @param event contains old and new values
	 */
	public void listenEmployeeChange(ValueChangeEvent event) {
		try {
			String acct = (String)event.getNewValue();
			log.debug("new val = " + acct + ", ID =" +  event.getComponent().getId());
			if (acct != null) {
				if (acct != null) {
					notify(FilterType.NAME, acct);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list. It is expected
	 * that this will be called during the INVOKE_APPLICATION phase!
	 *
	 * @param event contains old and new values
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		try {
			if (SessionUtils.getNonSystemProduction() != null) {
				log.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
				if (event.getNewValue() != null) {
					notify(FilterType.DATE, event.getNewValue());
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Called to send a notification to all our "holders" that a filter value
	 * has changed.  This is normally due to the user having made a change in
	 * a drop-down selection list.
	 *
	 * @param type The {@link FilterType} of the value that changed, e.g., DEPT.
	 * @param newValue The new value of the filter.
	 */
	private void notify(FilterType type, Object newValue) {
		currentTab = HeaderViewBean.getInstance().getMiniTab();
		if (newValue != null) {
			if (holders[currentTab] != null) {
				holders[currentTab].listenChange(type, newValue);
			}
		}
	}

	/**
	 * Create the List of SelectItem`s for the drop-down of types of entities
	 * that can be used for filtering, e.g., DEPT, BATCH, etc.
	 *
	 * @return A non-empty List of SelectItem`s created from the {@link FilterType}
	 *         enumeration.
	 */
	public static List<SelectItem> createFilterByList() {
		List<SelectItem> list = new ArrayList<>();
		for (FilterType ft : FilterType.values()) {
			if ("N_A".equals(ft.name())) {
				break;
			}
			list.add(new SelectItem(ft, "By " + ft.toString()));
		}
		return list;
	}

	/**
	 * Create a List of SelectItem`s of values described by the given
	 * {@link FilterType}.
	 *
	 * @param type The FilterType, which is passed to the current mini-tab's
	 *            "holder". It is up to the holder's "createList" method to
	 *            recognize the FilterType and generate an appropriate List.
	 * @return A List as described above.
	 */
	private List<SelectItem> createList(FilterType type) {
		currentTab = HeaderViewBean.getInstance().getMiniTab();
		List<SelectItem> list = null;
		if (holders[currentTab] != null) {
			list = holders[currentTab].createList(type);
		}
		return list;
	}

	private void updateTab(int oldTab) {
		for (int i = 0; i < MAX_TABS; i++) {
			if (holders[i] != null) {
				holders[i].changeTab(oldTab, currentTab);
			}
		}
	}

	/**
	 * This is meant to be called from JSP to cause our mini-tab setting to be set to the
	 * last saved value for the current page.
	 * @return null; the returned value is not used.
	 */
	public String getCheckMiniTab() {
		currentTab = HeaderViewBean.getInstance().getCheckMiniTab();
		return null;
	}

	/** See {@link #currentTab}. */
	public int getMiniTab() {
		return currentTab;
	}

	/** This sets the {@link #currentTab} value, and also stores the value
	 * in the user's session under an attribute name which is unique to the page currently
	 * displayed. (The Faces navigation string for the page is used to make
	 * it unique.) */
	public void setMiniTab(int n) {
		int oldTab = currentTab;
		currentTab = n;
		HeaderViewBean.getInstance().setMiniTab(n);
		updateTab(oldTab);
	}

	/**See {@link #filterType}. */
	public FilterType getFilterType() {
		return filterType;
	}
	/**See {@link #filterType}. */
	public void setFilterType(FilterType filterType) {
		this.filterType = filterType;
	}

	/**See {@link #filterByList}. */
	public List<SelectItem> getFilterByList() {
		if (filterByList == null) {
			filterByList = createFilterByList();
		}
		return filterByList;
	}
	/**See {@link #filterByList}. */
	public void setFilterByList(List<SelectItem> filterList) {
		filterByList = filterList;
	}

	/**See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}
	/**See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #endDateList}. */
	public List<SelectItem> getEndDateList() {
		if (endDateList == null) {
			endDateList = createList(FilterType.DATE);
		}
		return endDateList;
	}
	/** See {@link #endDateList}. */
	public void setEndDateList(List<SelectItem> endDateList) {
		this.endDateList = endDateList;
	}

	/** See {@link #selectFilterList}. */
	public List<SelectItem> getSelectFilterList() {
		if (selectFilterList == null) {
			try {
				if (filterType != null) {
					selectFilterList = createList(filterType);
				}
				else {
					log.info("type=" + filterType + ", value=" + selectFilterValue);
					selectFilterList = new ArrayList<>();
				}
				// See if selected value needs to be set, or reset (it may no longer be in the list)
				if (selectFilterList != null && selectFilterList.size() > 0) {
					if (selectFilterValue == null) { // not set, use 1st item in list
						selectFilterValue = selectFilterList.get(0).getValue().toString();
					}
					else {
						boolean found = false; // Is selected value in new list?
						for (SelectItem si : selectFilterList) {
							if (si.getValue().toString().equals(selectFilterValue.toString())) {
								found = true;
								break;
							}
						}
						if (! found) { // Not found, change selected to match first entry
							selectFilterValue = selectFilterList.get(0).getValue().toString();
						}
					}
				}
			}
			catch (Exception e) {
				log.error("type=" + filterType + ", value=" + selectFilterValue);
				EventUtils.logError(e);
				//MsgUtils.addGenericErrorMessage();
				if (selectFilterList == null) {
					selectFilterList = new ArrayList<>();
				}
			}
		}
		return selectFilterList;
	}
	/** See {@link #selectFilterList}. */
	public void setSelectFilterList(List<SelectItem> list) {
		selectFilterList = list;
	}

	/** See {@link #selectFilterValue}. */
	public String getSelectFilterValue() {
		return selectFilterValue;
	}
	/** See {@link #selectFilterValue}. */
	public void setSelectFilterValue(String value) {
		selectFilterValue = value;
	}

	/** See {@link #employeeAccount}. */
	public String getEmployeeAccount() {
		return employeeAccount;
	}
	/** See {@link #employeeAccount}. */
	public void setEmployeeAccount(String employeeAccount) {
		this.employeeAccount = employeeAccount;
	}

	/** See {@link #employeeList}. */
	public List<SelectItem> getEmployeeList() {
		if (employeeList == null) {
			employeeList = createList(FilterType.NAME);
		}
		return employeeList;
	}

	/** See {@link #employeeList}. */
	public void setEmployeeList(List<SelectItem> employeeList) {
		this.employeeList = employeeList;
	}

}
