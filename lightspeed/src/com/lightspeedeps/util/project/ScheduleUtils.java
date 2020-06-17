//	File Name:	ScheduleUtils.java
package com.lightspeedeps.util.project;

import java.text.DateFormat;
import java.util.*;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DateEventDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.DateEvent;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * Scheduling utility used by DooD calculations (see ProjectDood).
 */
public class ScheduleUtils {
	private static final Log log = LogFactory.getLog(ScheduleUtils.class);

	private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

	/** The Project this schedule is related to. */
	private Project project;

	/** The Unit this schedule is related to. */
	private Unit unit;

	/** The stripboard used for determining number of shooting dates. This may
	 * be null if the project has no script loaded. */
	private Stripboard stripboard;

	/** The starting date from the project schedule, which is typically the first
	 * shooting date. However, the first shooting day will be different than the
	 * project start date if the project start date is a non-working day. */
	private Date startDate;

	/** The calculated date of the last day of shooting, based on the start date
	 * specified in the project schedule, and the current stripboard.  If there
	 * is no stripboard, the end date will be the same as the project start date. */
	private Date endDate;

	/** Today's date, at start of day, 12:00:00am */
	private Date todayStart;

	/**
	 * A message to be displayed on the Home page or Calendar page that describes "today" in terms
	 * of the production schedule, such as "Day 3 of 15". Appropriate messages are given if shooting
	 * has not yet started, or has completed. */
	private String shootDayMsg = null;

	/** The phase this project is in (pre-prod, shooting, or post). */
//	private ProductionPhase phase = null;

	/**
	 * The shooting day number, from 1 to the number of shooting days defined by the current
	 * (active) stripboard, or 0 if the date is out of range of the project's shooting period or is
	 * not a working day, or if there is no stripboard.
	 */
	private int currentShootDayNumber = - 1;

	private Collection<DateEvent> dateEventSetDB = null;
	private Date useWeekDaysOffLimit;

	/**
	 * A List of Dates corresponding to all the shooting days in the project, based
	 * on the project schedule's start date and the current stripboard.  If there is
	 * no stripboard, the List will be empty (not null).
	 */
	private List<Date> shootingDatesList;

	/** A mapping based on all the holidays or other days off listed in the database. */
	private Map<String,DateEvent> daysOffMap;

	/**
	 * true or false for each day of week, numbered 1-7 to match Calendar DAY_OF_WEEK values; [0]
	 * entry is unused
	 */
	protected boolean[] weeklyDaysOff = null;
	protected boolean[] oldWeeklyDaysOff = null;

	public  ScheduleUtils(Unit u) {
		log.debug("");
		init(u);
	}

	public  ScheduleUtils(Unit u, Stripboard board) {
		log.debug("");
		init(u, board);
	}

	private void init(Unit u) {
		init(u, u.getProject().getStripboard());
	}

	private void init(Unit u, Stripboard board) {
		unit = u;
		project = unit.getProject();
		stripboard = board;
		todayStart = new Date();
		todayStart = CalendarUtils.setTime(todayStart, 0, 0);
		//dateEventSetDB = project.getProjectSchedule().getDateEvents();
		dateEventSetDB = DateEventDAO.getInstance().findByProperty(DateEventDAO.PROJECT_SCHEDULE, unit.getProjectSchedule());
		log.debug("# of events=" + dateEventSetDB.size());
		daysOffMap = null;
		initWeekDayLimit();
	}

	/**
	 * Set the date, 'useWeekDaysOffLimit', after which the code will use
	 * the current 'weeklyDaysOff' array to determine if a date is a normal
	 * day off.  Prior to the 'useWeekDaysOffLimit' date, the code will rely
	 * on the existence of dateEvent entries.
	 */
	private void initWeekDayLimit() {
		useWeekDaysOffLimit = unit.getProjectSchedule().getLastDaysOffChange();
		if (useWeekDaysOffLimit == null) {
			Calendar cal = new GregorianCalendar();
			cal.set(2010,1,1);
			useWeekDaysOffLimit = cal.getTime();
		}
	}

	public void refresh() {
		unit = UnitDAO.getInstance().refresh(unit);
		project =  unit.getProject();
		stripboard = project.getStripboard();
	}

	/**
	 * Called when we switch to the Calendar-Edit page.  We save the
	 * current weeklyDaysOff value, so that it may be applied to dates
	 * prior to today.  As the user edits the weeklyDaysOff value, we
	 * only want to apply the changed (edited) value to future dates.
	 */
	public void startEditMode() {
		oldWeeklyDaysOff = createWeeklyDaysOff(); // save copy of current days-off setting
	}

	/**
	 * Called when exiting the Calendar Edit page.  Clear out the saved
	 * weeklyDaysOff value; it is no longer needed.
	 */
	public void endEditMode() {
		oldWeeklyDaysOff = null; // old copy is no longer needed
	}

	/**
	 * Creates a SelectItem list for use in drop-downs, consisting of all the
	 * Unit names in the given Project, with a "key" value of the Unit's
	 * database id.
	 *
	 * @param project
	 * @return A non-empty List of Unit's for the given Project. (Every Project
	 *         has at least one Unit.)
	 */
	public static List<SelectItem> createUnitList(Project project) {
		// TODO createUnitList should only show Unit's available to the current User.
		List<SelectItem> unitNames = new ArrayList<SelectItem>();
		for (Unit unit : project.getUnits()) {
			unitNames.add(new SelectItem(unit.getId(), unit.getName()));
		}
		return unitNames;
	}

	/**
	 * Create a weeklyDaysOff boolean array, setting each entry to true or false
	 * to indicate if that day of the week is a normal day off. Note that
	 * weeklyDaysOff[0] is unused; we reference Sunday as [1], since
	 * Calendar.DAY_OF_WEEK returns 1 for Sunday.
	 */
	private boolean[] createWeeklyDaysOff() {
		return createWeeklyDaysOff(unit.getProjectSchedule().getDaysOff());
	}

	private boolean[] createWeeklyDaysOff(String daysOff) {
		boolean[] wkDaysOff = new boolean[8]; // [0] entry is unused
		for (int itr=0; itr < daysOff.length(); itr++) {
			wkDaysOff[itr+1] = (daysOff.charAt(itr) == '1');
		}
		return wkDaysOff;
	}

	/**
	 * Create a daysOffMap that includes all the holiday dates for the
	 * current project.  (This normally includes "standard" holidays
	 * that are included when a project is created.)
	 */
	private Map<String,DateEvent> createDaysOffMap() {
		Map<String,DateEvent> map = new HashMap<String,DateEvent>();
		if ( dateEventSetDB != null) {
			for (DateEvent dateEvent : dateEventSetDB) {
				addDaysOff(map, dateEvent);
			}
		}
		log.debug("# of daysOffMap entries=" + map.size());
		return map;
	}

	/**
	 * Add one or more entries to the daysOffMap for the given
	 * dateEvent -- one entry for each day included in the dateEvent's
	 * range.
	 * @param dateEvent
	 */
	private void addDaysOff(Map<String,DateEvent> map, DateEvent dateEvent) {
		Calendar calStart = new GregorianCalendar();
		Calendar calEnd = new GregorianCalendar();
		calStart.setTime(dateEvent.getStart());
		calEnd.setTime(dateEvent.getEnd());
		String strStart = dateFormat.format(calStart.getTime());
		String strEnd = dateFormat.format(calEnd.getTime());

		int incr = 0;
		if (calStart.before(calEnd)) {
			while (!strStart.equalsIgnoreCase(strEnd) && calStart.before(calEnd)) {
				calStart.set(Calendar.DAY_OF_MONTH, calStart.get(Calendar.DAY_OF_MONTH) + incr);
				strStart = dateFormat.format(calStart.getTime());
				map.put(strStart, dateEvent);
				//log.debug("(a) added date=" + strStart);
				incr = 1;
			}
		}
		else { // Single date (the most common)
			map.put(strStart, dateEvent);
			//log.debug("(b) added date=" + strStart);
		}
	}

	/**
	 * Create a list of Strip-Board/current-shoot-days Dates.
	 * Outside callers should use getShootingDatesList() to retrieve
	 * the list; the list will be built if it has not already been
	 * constructed.
	 * @return A non-null, but possibly empty, List of Dates as described.
	 */
	private List<Date> createShootingDateList() {
		List<Date> datesList = new ArrayList<Date>();
		Calendar calItr = new GregorianCalendar();
		calItr.setTime(getStartDate());
		int strpBrddays = getShootingDays();
		int incr = 0;
		if (getStartDate() != null) {
			for (int itr = 0; itr < strpBrddays;) {
				calItr.add(Calendar.DAY_OF_MONTH, incr);
				if ( ! isDayOff(calItr.getTime())) {
					datesList.add(calItr.getTime());
					//log.debug("added stripboard shooting date: " + calItr.getTime());
					itr++;
				}
				incr = 1;
			}
		}
		log.debug("unit=" + unit.getNumber() + ", # of shooting dates=" + datesList.size());
		return datesList;
	}

	public void updateDaysOffList(DateEvent dateEvent) {
		addDaysOff(getDaysOffMap(), dateEvent);
	}

	/**
	 * The list of "weekly" days off has changed.  We want to make permanent
	 * the days-off that were in effect with the previous pattern, up through
	 * today's date.
	 */
	public void updateDaysOff(String oldDaysOff) {
		try {
			Calendar yesterday = new GregorianCalendar();
			yesterday.add(Calendar.DAY_OF_MONTH, -1);
			if (yesterday.getTime().before(getStartDate())) {
				// if shooting hasn't started, ignore the change
				return;
			}
			Date start = unit.getProjectSchedule().getLastDaysOffChange();
			if (start == null) {
				start = getStartDate();
			}
			Calendar startCal = new GregorianCalendar();
			startCal.setTime(start);
			// Don't change the setting on the date that the change was last made, because
			// that date was actually controlled by the week-days-off setting that was in
			// effect prior to that change.
			startCal.add(Calendar.DAY_OF_MONTH, 1); // So add 1 day.

			Calendar endCal = new GregorianCalendar(); // today
			CalendarUtils.setStartOfDay(endCal);

			DateEventDAO dateEventDAO = DateEventDAO.getInstance();
			// from latest til start of this week, add Standard-off DateEvents
			boolean[] daysOff = createWeeklyDaysOff(oldDaysOff);
			dateEventDAO.addDaysOff(unit, startCal, endCal, daysOff);
			unit = UnitDAO.getInstance().findById(unit.getId()); // refresh it
			init(unit);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Returns TRUE if the given date is either a weekly day off, or a scheduled
	 * DateEvent other than "shooting". That is, the day type is not "WORK".
	 * <p/>
	 * Note: prior to rev 1336, this returned FALSE for a date before the start
	 * of the project if it was not a weekly day off; as of rev 1336, this
	 * returns TRUE for any date before the project start date.
	 */
	public boolean isDayOff(Date dateToCheck) {
		return findDayType(dateToCheck) != WorkdayType.WORK;
	}

	/**
	 * Returns TRUE if the given date falls on a day of the week which is
	 * marked as a "normal day off".
	 * @param dateToCheck
	 */
	public boolean isWeeklyDayOff(Date dateToCheck) {
		if (dateToCheck.after(useWeekDaysOffLimit)) {
			return isCurrentWeeklyDayOff(dateToCheck);
		}
		else {
			String dateToCheckStr = dateFormat.format(dateToCheck);
			DateEvent de = getDaysOffMap().get(dateToCheckStr);
			return de != null && de.getType() == WorkdayType.STANDARD_OFF;
		}
	}

	private boolean isCurrentWeeklyDayOff(Date dateToCheck) {
		Calendar dateToCheckCal = new GregorianCalendar();
		dateToCheckCal.setTime(dateToCheck);
		int weekday = dateToCheckCal.get(Calendar.DAY_OF_WEEK);
		if (oldWeeklyDaysOff != null) {
			if (! dateToCheck.after(todayStart)) {
				return oldWeeklyDaysOff[weekday];
			}
		}
		return getWeeklyDaysOff()[weekday];
	}

//	public static boolean isSystemHoliday(Date date) {
//		return DateEventDAO.getInstance().existsSystemHoliday(date);
//	}

	/**
	 * Determine the WorkdayType of the given date. Checks for regular (weekly)
	 * days off as well as specific events set in the project calendar.
	 *
	 * @param dateToCheck - the date to look up.
	 * @return the matching WorkdayType, which should be one of WORK, OFF, PREP,
	 *         HOLIDAY, COMPANY_TRAVEL, or TRAVEL.
	 *         <p/>
	 *         Note: prior to rev 1336, this returned WORK for a date before the
	 *         start of the project if it was not a weekly day off, and OFF if
	 *         it was a weekly day off. As of rev 1336, this returns PREP for
	 *         any date before the project start date.
	 */
	public WorkdayType findDayType(Date dateToCheck) {
		WorkdayType type;
		DateEvent de = findDateEvent(dateToCheck);
		if (de != null) {
			type = de.getType();
		}
		else if (dateToCheck.before(getStartDate())) {
			type = WorkdayType.PREP;
		}
		else if (dateToCheck.after(useWeekDaysOffLimit) && isCurrentWeeklyDayOff(dateToCheck)) {
			type = WorkdayType.OFF;
		}
		else {
			type = WorkdayType.WORK;
		}
		return type;
	}

	/**
	 * Get the stored DateEvent associated with a particular date. Returns null if no DateEvent is
	 * set for the given date.
	 *
	 * @param dateToCheck
	 */
	public DateEvent findDateEvent(Date dateToCheck) {
		DateEvent de = null;
		String dateToCheckStr = dateFormat.format(dateToCheck);
		de = getDaysOffMap().get(dateToCheckStr);
		//log.debug("dateEvent for " + dateToCheckStr + "=" + de);
		if (de != null && de.getType() == WorkdayType.STANDARD_OFF) {
			de.setType(WorkdayType.OFF);
		}
		return de;
	}

	/**
	 * Return the sequential shooting day number for the specified Date. Returns 0 if the given date
	 * is before the project start date, after the project end date, a non-working day, or if no
	 * start date has been set yet.
	 *
	 * @param date The Date value to be checked; the time of day setting in the Date object is
	 *            irrelevant.
	 * @return The shooting day number, from 1 to the number of shooting days defined by the current
	 *         (active) stripboard, or 0 if the date is out of range of the project's shooting
	 *         period or is not a working day.
	 */
	public int findShootingDayNumber(Date date) {
		if (getStartDate() == null) {
			return 0;
		}
		// Use calendar functions to clear time-of-day
		Calendar matchDate = new GregorianCalendar();
		matchDate.setTime(date);
		CalendarUtils.setStartOfDay(matchDate);
		int n = getShootingDatesList().indexOf(matchDate.getTime());
		return n + 1;
	}

	/**
	 * Find the Date of the given shooting day number. If there is no
	 * stripboard, the project's start date is returned. If the day number is
	 * less than one, the date of the first day of shooting is returned. If the
	 * day number is greater than the number of days of shooting, the date of
	 * the last day of shooting is returned.
	 * <p/>
	 * Note that the first shooting day will be different than the project start
	 * date if the project start date is a non-working day.
	 *
	 * @param dayNumber The shooting day number whose Date is wanted.
	 * @return The Date of the requested shooting day number, or the first or
	 *         last day of shooting, or the project start date, as outlined
	 *         above.
	 */
	public Date findShootingDay(int dayNumber) {
		Date date;
		List<Date> list = getShootingDatesList();
		if (list.size() > 0 ) {
			if (dayNumber <= list.size()) {
				if (dayNumber < 1) {
					dayNumber = 1;
				}
				date = list.get(dayNumber-1);
			}
			else {
				date = list.get(list.size()-1);
			}
		}
		else {
			date = getStartDate();
		}
		return date;
	}

	/**
	 * Return the next WORK date following the date given in 'calendar'.
	 *
	 * @param calendar The date after which the next work day (shooting day)
	 *            should be found. If null, the first shooting date is returned,
	 *            or the project start date if there are no shooting days
	 *            defined.
	 * @return The next shooting date after 'calendar'. If 'calendar' is null,
	 *         or if 'calendar' precedes the project start date, then the first
	 *         shooting date is returned; however, if no shooting dates are
	 *         defined, then the project start date is returned instead. Returns
	 *         null only if 'calendar' is not null and after the last day of
	 *         shooting.
	 */
	public Calendar findNextWorkDate(Calendar calendar) {
		List<Date> dates = getShootingDatesList();
		if (calendar == null || calendar.getTime().before(getStartDate()) ) {
			if (dates.size() > 0) {
				return CalendarUtils.getInstance(dates.get(0));
			}
			return CalendarUtils.getInstance(getStartDate());
		}
		int ix = dates.indexOf(calendar.getTime());
		if (ix >= 0 && ix < dates.size()-1) {
			log.debug("return from list, ix=" + ix + ", next date=" + dates.get(ix+1));
			return CalendarUtils.getInstance(dates.get(ix+1));
		}
		// If the date given to us was not a shooting day, then
		// the "quick" method above won't work.  Try it another way...
		Calendar newcal = CalendarUtils.getInstance(calendar.getTime());
		for (int i=0; i < 30; i++) { // assume less than a month between working days!!
			newcal.add(Calendar.DAY_OF_MONTH, 1);
			if (findDayType(newcal.getTime()) == WorkdayType.WORK) {
				return newcal;
			}
		}
		return null;
	}

	/**
	 * Find the first work (shooting) day prior to the date specified.
	 *
	 * @param calendar The date in question.  If null, the LAST shooting day is returned.
	 * @return The date of the shooting day before, and closest to, the given date. Returns null if
	 *         no shooting day occurs before the given date.
	 */
	public Calendar findPreviousWorkDate(Calendar calendar) {
		List<Date> dates = getShootingDatesList();
		if (calendar == null ) {
			if (dates.size() > 0) {
				return CalendarUtils.getInstance(dates.get(dates.size()-1));
			}
			return null;
		}
		if (dates.size() == 0) {
			return null;
		}
		int ix = dates.indexOf(calendar.getTime());
		if (ix > 0) {
			log.debug(dates.get(ix-1));
			return CalendarUtils.getInstance(dates.get(ix-1));
		}
		if (ix == 0) { // parameter was the first shooting day
			log.debug("first day");
			return null; // so none previous
		}
		Date lastDate = dates.get(dates.size()-1);
		if (calendar.getTime().after(lastDate)) {
			return CalendarUtils.getInstance(lastDate);
		}
		// If the date given to us was not a shooting day, then
		// the "quick" method above won't work.  Try it another way...
		Calendar newcal = CalendarUtils.getInstance(calendar.getTime());
		CalendarUtils.setStartOfDay(newcal);
		for (int i=0; i < 30; i++) { // assume less than a month between working days!!
			newcal.add(Calendar.DAY_OF_MONTH, -1);
			if (findDayType(newcal.getTime()) == WorkdayType.WORK) {
				log.debug(newcal.getTime());
				return newcal;
			}
		}
		log.debug("none");
		return null;
	}

	/**
	 * Return the next WORK date following the date given in 'calendar'.
	 *
	 * @param date
	 *            The date after which the next work day (shooting day) should
	 *            be found. If null, the first shooting day is returned.
	 * @return The shooting day as explained, or null if there are no shooting
	 *         days following the date given.
	 */
	public Calendar findNextWorkDate(Date date) {
		return findNextWorkDate(date == null ? null : CalendarUtils.getInstance(date));
	}

	/**
	 * Determine the project's last shooting date, based on the current
	 * (active) stripboard and project schedule.
	 * @return The Date of the last day of shooting; the time-of-day value
	 * will be zero (12:00am).
	 */
	private Date findEndDate() {
		log.debug("");
		Date date;
		project = ProjectDAO.getInstance().refresh(project);
		List<Date> list = getShootingDatesList();
		if (list.size() > 0) {
			date = list.get(list.size()-1);
		}
		else {
			date = getStartDate();
		}
		return date;
	}

	private void createShootDayMessage() {
		log.debug("");
		Calendar todaysDateTmp = new GregorianCalendar();
		CalendarUtils.setStartOfDay(todaysDateTmp);

		// shoot day message on screen
		if (stripboard == null ||
				todaysDateTmp.getTime().before(getStartDate())) {
			setShootDayMsg(MsgUtils.getMessage("Schedule.ProjectNotStarted"));
//			phase = ProductionPhase.PRE_PRODUCTION;
		}
		else if (stripboard.getShootingDays(unit) == 0 ) {
			setShootDayMsg(MsgUtils.getMessage("Schedule.NoShootingScheduled"));
//			phase = ProductionPhase.PRE_PRODUCTION;
		}
		else if (todaysDateTmp.getTime().after(getEndDate())) {
			setShootDayMsg(MsgUtils.getMessage("Schedule.ProjectCompleted"));
//			phase = ProductionPhase.POST_PRODUCTION;
		}
		else if (isDayOff(todaysDateTmp.getTime()) == true) {
			setShootDayMsg(MsgUtils.getMessage("Schedule.NotShootingDay"));
//			phase = ProductionPhase.PRODUCTION;
		}
		else { // currentShootDay
			setShootDayMsg(MsgUtils.formatMessage("Schedule.ShootingDay", getCurrentShootDayNumber(), getShootingDays()));
//			phase = ProductionPhase.PRODUCTION;
		}
	}

	/** See {@link #daysOffMap}. */
	private Map<String, DateEvent> getDaysOffMap() {
		if (daysOffMap == null) {
			daysOffMap = createDaysOffMap();
		}
		return daysOffMap;
	}

	private int getShootingDays() {
		if (stripboard != null) {
			return stripboard.getShootingDays(unit);
		}
		return 0;
	}

	/** See {@link #weeklyDaysOff}. */
	public boolean[] getWeeklyDaysOff() {
		if (weeklyDaysOff == null) {
			weeklyDaysOff = createWeeklyDaysOff();
		}
		return weeklyDaysOff;
	}

	public String getWeeklyDaysOffAsString() {
		String s = "";
		for (int i=1; i<8; i++) {
			s += ( getWeeklyDaysOff()[i] ? '1' : '0');
		}
		log.debug("days off="+s);
		return s;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** Used to refresh our Unit when external holder refreshes its copy.
	 * See {@link #unit}. */
	public void setUnit(Unit u) {
		unit = u;
		project =  unit.getProject();
		stripboard = project.getStripboard();
	}

	/** See {@link #shootDayMsg}. */
	public String getShootDayMsg() {
		if (shootDayMsg == null) {
			createShootDayMessage();
		}
		return shootDayMsg;
	}
	/** See {@link #shootDayMsg}. */
	private void setShootDayMsg(String shootDayMsg) {
		this.shootDayMsg = shootDayMsg;
	}

	/** See {@link #currentShootDayNumber}. */
	public int getCurrentShootDayNumber() {
		if (currentShootDayNumber < 0) { // not computed yet
			currentShootDayNumber = findShootingDayNumber(new Date());
		}
		return currentShootDayNumber;
	}
	/** See {@link #currentShootDayNumber}. */
	public void setCurrentShootDayNumber(int curShootDate) {
		currentShootDayNumber = curShootDate;
	}

	/** See {@link #startDate}. */
	public Date getStartDate() {
		if (startDate == null) {
			startDate = unit.getProjectSchedule().getStartDate();
		}
		return startDate;
	}
	/** See {@link #startDate}. */
	public void setStartDate(Date date) {
		startDate = date;
	}

	/** See {@link #endDate}. */
	public Date getEndDate() {
		if (endDate == null) {
			endDate = findEndDate();
		}
		return endDate;
	}
	/** See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #shootingDatesList}. */
	public List<Date> getShootingDatesList() {
		if (shootingDatesList == null) {
			shootingDatesList = createShootingDateList();
		}
		return shootingDatesList;
	}
	/** See {@link #shootingDatesList}. */
	public void setShootingDatesList(List<Date> shootingDatesList) {
		this.shootingDatesList = shootingDatesList;
	}

}
