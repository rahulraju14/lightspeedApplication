package com.lightspeedeps.web.schedule;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.DateEvent;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.SceneItem;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.web.login.AuthorizationBean;

/**
 * Backing bean for the Calendar (project schedule) View page.
 * <p>
 * This is also the superclass for the Calendar Edit page backing
 * bean, CalendarEditBean.
 * <p>
 * Much of the manipulation of the scheduling data is done
 * by an instance of the helper class, ScheduleUtils.
 */
@ManagedBean
@ViewScoped
public class CalendarViewBean implements Serializable {
	/** */
	private static final long serialVersionUID = 2345015699156026474L;

	private static final Log log = LogFactory.getLog(CalendarViewBean.class);

	protected static final String ATTR_CALENDAR_UNIT_ID = Constants.ATTR_PREFIX + "calendarUnitId";

	protected static final String STYLE_HILITE_ON = "cal_highlight_on";
	protected static final String STYLE_HILITE_OFF = "cal_highlight_off";
	protected static final String BLANK_IMAGE_PATH = "spacer.gif"; //dh
	protected static final String SHOOTING_IMAGE = "icon_work.gif";
	protected static final String OFF_IMAGE = "icon_off.gif";
	protected static final String PRE_IMAGE = "cal_pre.gif";
	protected static final String POST_IMAGE = "cal_post.gif";
	protected static final String PATH_PREFIX = Constants.IMAGE_PATH;

	private	static final String[] monthNames = (new DateFormatSymbols()).getMonths();

	protected Date endDate;

	protected String shootDayMsg ;
	protected String shootingDays;

	protected int shootingDay;	// the shooting day number within the current project
//	protected int stripBrdId ;
	protected int curMonth;
	protected int curYear;

	protected boolean editMode = false; // set to true by Edit subclass

	protected Calendar todaysDate = new GregorianCalendar();

	/** */
	protected Project project;
	/** */
	protected Stripboard stripboard;

	/** The currently selected Unit. */
	protected Unit unit;

	/** The database id of the currently selected Unit.  Set via the drop-down
	 * list on the Calendar View page. */
	private Integer selectedUnitId = 1;

	/** A selection list for choosing which Unit's calendar to view. */
	private List<SelectItem> unitDL;

	protected List<OneWeek> weekList = new ArrayList<OneWeek>();
	protected List<DateEvent> monthDateEvent = new ArrayList<DateEvent>();
	protected List<Date> shootingDatesList = new ArrayList<Date>();

	protected SortedMap<Date, List<String>> scenesMap = new TreeMap<Date, List<String>>();

	protected transient ScheduleUtils scheduleUtils;
	DateFormat formatShort = DateFormat.getDateInstance(DateFormat.SHORT);

	public  CalendarViewBean() {
		log.debug("");
		initView();
	}

	protected void initView() {
		log.debug("Initializing CalendarViewBean");
		try {
			project = SessionUtils.getCurrentProject();
			Integer unitId = SessionUtils.getInteger(ATTR_CALENDAR_UNIT_ID);
			if (unitId != null) {
				unit = UnitDAO.getInstance().findById(unitId);
				if (unit == null || ! unit.getProject().equals(project)) {
					unit = null;	// not usable
				}
			}
			if (unit == null) {
				List<Unit> units = AuthorizationBean.getInstance().getUnits();
				if (units.size() > 0) {
					unit = units.get(0);
				}
				else {
					unit = project.getMainUnit();
				}
			}
			log.debug("unit=" + unit);
			createCalendar();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Creates all the data needed to display one month.
	 */
	private void createCalendar() {
		try {
			scheduleUtils = null;	// force creation of new ScheduleUtils
			SessionUtils.put(ATTR_CALENDAR_UNIT_ID, unit.getId());
			selectedUnitId = unit.getId();
			Calendar currDateCal = new GregorianCalendar();
			int year = currDateCal.get(Calendar.YEAR);
			int monthNo = currDateCal.get(Calendar.MONTH);

			//Retrieving values from Edit screen
			HttpSession session = SessionUtils.getHttpSession();
			if (session.getAttribute("curMonth") != null) {
				monthNo = (Integer) session.getAttribute("curMonth");
			}
			if (session.getAttribute("curYear")!= null) {
				year = (Integer) session.getAttribute("curYear");
			}
			setCurYear(year);
			if (monthNo  >= 0 && monthNo  <= 11 ) {
				setCurMonth(monthNo);
			}

			Calendar todaysDateTmp = new GregorianCalendar();
			CalendarUtils.setStartOfDay(todaysDateTmp);

			stripboard = project.getStripboard();
			findEndDate();

			// shoot day message on screen
			setShootDayMsg(getScheduleUtils().getShootDayMsg());

			//Create list of shooting dates for this StripBoard & schedule
			shootingDatesList = getScheduleUtils().getShootingDatesList();

			// create Map(date, scene) of scenes
			createSceneList();

			//create month's calendar
			setWeekList(createMonthCalendar(year,monthNo));

		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Calculate Project's end date according to start date and total shooting days
	 */
	protected void findEndDate() {
		endDate = getScheduleUtils().getEndDate();
	}

	/**
	 * The ValueChangeListener method for the "Unit" drop-down list. Changes the
	 * unit selected.
	 */
	public void actionChangeUnit(ValueChangeEvent event) {
		if (null != event.getNewValue()) {
			try {
				//User user = SessionUtils.getCurrentUser();
				int newId = (Integer)event.getNewValue();
				setSelectedUnitId(newId);
				project = ProjectDAO.getInstance().refresh(project);
				unit = UnitDAO.getInstance().findById(selectedUnitId);
				createCalendar();
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
//		ListView.addClientResize();
	}


	/*
	 * Jump to Stripboard - Was ActionListener for stripboard icon, not currently
	 * used; might be needed if we add capability to jump to specific shooting
	 * day.
	 */
/*	public void stripBrdMenu(ActionEvent ae) {
		HeaderViewBean.setMenu(HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_VIEW_IX);
	}
*/

	/**
	 * ActionListener method for the Edit button.
	 * @param evt
	 */
	public void edit(ActionEvent evt) {
		HttpSession session = SessionUtils.getHttpSession();
		session.setAttribute("curMonth", new Integer(getCurMonth()));
		session.setAttribute("curYear", getCurYear());
	}

	/**
	 *  Create a Map(date, List<scene numbers>) for all scenes
	 *  covered by the current stripboard.  The Map will have
	 *  one entry for each shooting day in the stripboard.
	 */
	private void createSceneList() {
		scenesMap.clear();
		Script script = project.getScript();
		if (script == null || project.getStripboard() == null || shootingDatesList.size() == 0) {
			// no stripboard, or no shooting dates (no scheduled strips, or no start date)
			return;
		}
		List<Strip> allStripList = StripDAO.getInstance().findByUnitAndStripboard(unit, project.getStripboard());
		if (allStripList != null) { // not used for edit mode
			SceneDAO sceneDao = SceneDAO.getInstance();
			Scene scene;
			Calendar calItr = new GregorianCalendar();

			Iterator<Date> shootingDatesListitr  = shootingDatesList.iterator();
			calItr.setTime(shootingDatesListitr.next());

			List<String> scenesList = new ArrayList<String>();
			for (Strip strip : allStripList) {
				if ((strip.getType() == StripType.END_OF_DAY)) {
					if (shootingDatesListitr.hasNext()) {
						calItr.setTime(shootingDatesListitr.next());
					}
					else {
						break;
					}
					scenesList = new ArrayList<String>();
				}
				else if (strip.getType() == StripType.BREAKDOWN) {
					scene = sceneDao.findByStripAndScript(strip, script);
					if (scene != null) {
						// TODO performance/cleaner? - don't build List here, just save String
						String[] result = strip.getSceneNumbers().split(",");
						for (String s : result) {
							scenesList.add(s);
						}
					}
				}
				scenesMap.put(calItr.getTime(), scenesList);
			}
		}
		if (log.isDebugEnabled()) {
			for (Date d : scenesMap.keySet()) {
				log.debug(d + ": " + StringUtils.getStringFromList(scenesMap.get(d)) );
			}
		}
	}

	/**
	 * Create a List of SceneBeans, one for each Scene, corresponding to
	 * all the scenes shot in one day.  Uses the scenesMap built earlier by
	 * createSceneList().  May return an empty List, but never null.
	 */
	private List<SceneItem> createScenesListForDate(Calendar cal) {
		List<SceneItem> stripObjList = new ArrayList<SceneItem>();
		if (scenesMap.get(cal.getTime()) != null && getStartDate() != null) {
			boolean first = true;
			for (String scene : scenesMap.get(cal.getTime())) {
				if ( ! first) { // just a comma delimiter
					stripObjList.add(new SceneItem(", ", false));
				}
				stripObjList.add(new SceneItem(scene, true)); // JSP makes a link with this
				first = false;
			}
		}
		return stripObjList;
	}


	/**
	 * Action method for the "next month" graphic button. Sets the current month
	 * to next month.
	 *
	 * @return null navigation string
	 */
	public String nextMonth() {
		//log.debug("curr month="+curMonth);
		try {
			project =  SessionUtils.getCurrentProject();
			if (curMonth == 11) {
				curYear++;
				curMonth = 0;
			}
			else {
				curMonth++;
			}
			setWeekList(createMonthCalendar(getCurYear(), getCurMonth()));
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "previous month" graphic button. Sets current month
	 * to previous month.
	 *
	 * @return null navigation string
	 */
	public String prevMonth() {
		//log.debug("curr month="+curMonth);
		try {
			project =  SessionUtils.getCurrentProject();
			if (curMonth == 0) {
				curYear--;
				curMonth = 11;
			}
			else {
				curMonth--;
			}
			setWeekList(createMonthCalendar(getCurYear(), getCurMonth()));
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "next year" button. Sets current year to next year.
	 *
	 * @return null navigation string
	 */
	public String nextYear() {
		try {
			project =  SessionUtils.getCurrentProject();
			curYear++;
			setWeekList(createMonthCalendar(getCurYear(), getCurMonth()));
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for the "previous year" button. Sets current year to
	 * previous year.
	 *
	 * @return null navigation string
	 */
	public String prevYear() {
		try {
			project =  SessionUtils.getCurrentProject();
			curYear--;
			setWeekList(createMonthCalendar(getCurYear(), getCurMonth()));
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Generates a List of 4-6 "OneWeek" objects which will be used to
	 * display a particular month's calendar.
	 * @param year The year the calendar will represent.
	 * @param monthNo The month represented.
	 * @return The non-null List as described above.
	 */
	protected List<OneWeek> createMonthCalendar(int year, int monthNo) {
		log.debug("");
		OneWeek week;
		List<OneWeek> wkList = new ArrayList<OneWeek>();
		Calendar cal = new GregorianCalendar(year, monthNo, 1);

		Calendar lastDate = (Calendar)cal.clone();
		lastDate.add(Calendar.MONTH, 1); // first day of next month
		lastDate.add(Calendar.DAY_OF_YEAR, -1); // last day of month
		int dayOfWeek = lastDate.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 7=Saturday
		if (dayOfWeek != Calendar.SATURDAY) { // not end of week
			lastDate.add(Calendar.DAY_OF_YEAR, (7-dayOfWeek)); // so push it up to Saturday
		}

		dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 7=Saturday
		if (dayOfWeek != Calendar.SUNDAY) { // not start of week
			cal.add(Calendar.DAY_OF_YEAR, -(dayOfWeek-1)); // so back it up to Sunday
		}

		Calendar today = new GregorianCalendar();
		CalendarUtils.setStartOfDay(today); // zero the time-of-day fields (for compares)
		String imagePathTemp;
		Integer dateEventId=0;
		project = SessionUtils.getCurrentProject(); // refresh

		while(! cal.after(lastDate)) { // loop iterates once per week in month
			week = new OneWeek(); // object holds one week's worth of display data
			for (int dayofWeek = 1; dayofWeek <= 7; dayofWeek++) {
				week.dayNumber[dayofWeek] = "" + cal.get(Calendar.DAY_OF_MONTH);
				week.date[dayofWeek] = cal.getTime();
				week.highlightDay[dayofWeek] = (cal.equals(today) ? STYLE_HILITE_ON : STYLE_HILITE_OFF);
				week.showDataDay[dayofWeek] = true; // assume showing icons
				String[] dateEventStr = dayOffImageAndId(cal.getTime());
				if (dateEventStr[1] != null && dateEventStr[1] != "") {
					dateEventId = Integer.valueOf(dateEventStr[1]);
				}
				else {
					dateEventId = -1;
				}
				week.dateEventIdDay[dayofWeek] = dateEventId;
				imagePathTemp = dateEventStr[0];
				if (imagePathTemp != null && imagePathTemp.length() > 0) {
					if (! editMode) {
						week.showDataDay[dayofWeek] = false;
					}
					week.imagePathDay[dayofWeek] = PATH_PREFIX + imagePathTemp;
				}
				else { // a shooting day
					week.imagePathDay[dayofWeek] = PATH_PREFIX + SHOOTING_IMAGE; // ignored in View mode
					week.sceneListDay[dayofWeek] = createScenesListForDate(cal);
					String time = findCallTime(cal, project.getCallsheets());
					week.callTimeDay[dayofWeek] = time;
					week.callSheetDayNo[dayofWeek] = (time.length() > 0); // found call time?
					if (stripboard != null) {
						week.stripBrdDay[dayofWeek] = true;
						week.strpBrdNo[dayofWeek] = findShootDayNumber(cal);
					}
				}
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}
			wkList.add(week);
		}
		return wkList;

	}

	/**
	 *  Returns a pair of values (only the first is used in View mode).
	 *  [0]: the image filename for a date if it is: after EndDate or weekly days-off
	 *   or in the DateEvent table; otherwise it returns null (i.e., for a shooting day).
	 *   [1]: the id of the DateEvent object related to the date, if any (else null);
	 *   this is used in Edit mode.
	 * @param dateToCheck
	 */
	private String[] dayOffImageAndId(Date dateToCheck) {
		String[] dateEventStr = new String[2];
		dateEventStr[0] = null;
		dateEventStr[1] = null;
		Calendar dateToCheckTmp = Calendar.getInstance();
		dateToCheckTmp.setTime(dateToCheck);
		CalendarUtils.setStartOfDay(dateToCheckTmp);

		//check date is between startDate and endDate of Schedule
		if (dateToCheckTmp.getTimeInMillis() < getStartDate().getTime()) {
			dateEventStr[0] = PRE_IMAGE;
			return dateEventStr;
		}
		else if (dateToCheckTmp.getTimeInMillis() > getEndDate().getTime()) {
			dateEventStr[0] = POST_IMAGE;
			return dateEventStr;
		}

		DateEvent dateEvent = getScheduleUtils().findDateEvent(dateToCheck);
		if (dateEvent != null) { // special day type scheduled
			dateEventStr[1] = dateEvent.getId().toString();
			if (dateEvent.getType() != WorkdayType.WORK) {
				dateEventStr[0] = "icon_" +
					dateEvent.getType().name().toLowerCase() + ".gif";
			}
		}
		else if (getScheduleUtils().isWeeklyDayOff(dateToCheck)) {
			dateEventStr[0] = OFF_IMAGE;
			return dateEventStr;
		}

		return dateEventStr;
	}

	/**
	 *  Returns current-shoot-day number for date, as String, to be displayed
	 *  inside the stripboard icon.
	 */
	private String findShootDayNumber(Calendar cal) {
		int n = getScheduleUtils().findShootingDayNumber(cal.getTime());
		if (n != 0) {
			return (String.valueOf(n));
		}
		else {
			return "";
		}
	}

	/**
	 * Returns the call time, if any, for the given date and the current Unit.
	 *
	 * @param cal The date of interest.
	 * @param cSheetList The list of Callsheet`s to search for a match; this is
	 *            typically all the Callsheet`s for the current Project.
	 * @return The call time, formatted as "h:mma", that matches the current
	 *         unit on the specified date, or an empty String if no matching
	 *         call sheet is found, or "TBD" if a matching call sheet is found
	 *         which has no crew call time set. Never returns null.
	 */
	private String findCallTime(Calendar cal, Set<Callsheet> cSheetList) {
		Long date = cal.getTime().getTime();
		String time = "";
		for (Callsheet cSheet : cSheetList) {
			if (cSheet.getDate().getTime() == date && cSheet.getUnitNumber().equals(unit.getNumber())) {
				if (cSheet.getCallTime() == null) {
					time = "TBD";
				}
				else {
					time = CalendarUtils.formatDate(cSheet.getCallTime(), "h:mma");
					time = time.replace("AM", "a").replace("PM", "p");
				}
			}
		}
		return time;
	}

	public Date getStartDate() {
		return getScheduleUtils().getStartDate();
	}
	public void setStartDate(Date date) {
		getScheduleUtils().setStartDate(date);
	}

	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDateD) {
		this.endDate = endDateD;
	}

	/** See {@link #selectedUnitId}. */
	public Integer getSelectedUnitId() {
		return selectedUnitId;
	}
	/** See {@link #selectedUnitId}. */
	public void setSelectedUnitId(Integer selectedUnitId) {
		this.selectedUnitId = selectedUnitId;
	}

	/** See {@link #unitDL}. */
	public List<SelectItem> getUnitDL() {
		if (unitDL == null) {
			unitDL = ScheduleUtils.createUnitList(project);
		}
		return unitDL;
	}
	/** See {@link #unitDL}. */
	public void setUnitDL(List<SelectItem> unitDL) {
		this.unitDL = unitDL;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/**
	 * @return the weekList
	 */
	public List<OneWeek> getWeekList() {
		return weekList;
	}
	/**
	 * @param weekList the weekList to set
	 */
	public void setWeekList(List<OneWeek> weekList) {
		this.weekList = weekList;
	}

	/**
	 * @return the curMonth
	 */
	public int getCurMonth() {
		return curMonth;
	}
	/**
	 * @param curMonth the curMonth to set
	 */
	public void setCurMonth(int curMonth) {
		this.curMonth = curMonth;
	}

	/**
	 * @return the curYear
	 */
	public int getCurYear() {
		return curYear;
	}
	/**
	 * @param curYear the curYear to set
	 */
	public void setCurYear(int curYear) {
		this.curYear = curYear;
	}

	public String[] getMonthNames() {
		return monthNames;
	}

	/**
	 * @return the shootDayMsg
	 */
	public String getShootDayMsg() {
		return shootDayMsg;
	}
	/**
	 * @param shootDayMsg the shootDayMsg to set
	 */
	public void setShootDayMsg(String shootDayMsg) {
		this.shootDayMsg = shootDayMsg;
	}

//	public int getStripBrdId() {
//		return stripBrdId;
//	}
//	public void setStripBrdId(int stripBrdId) {
//		this.stripBrdId = stripBrdId;
//	}

	protected ScheduleUtils getScheduleUtils() {
		if (scheduleUtils == null) {
			scheduleUtils = new ScheduleUtils(unit);
		}
		return scheduleUtils;
	}

}