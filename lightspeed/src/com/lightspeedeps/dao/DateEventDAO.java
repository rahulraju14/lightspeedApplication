package com.lightspeedeps.dao;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.DateEvent;
import com.lightspeedeps.model.ProjectSchedule;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.HolidayType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * DateEvent entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DateEvent
 */

public class DateEventDAO extends BaseTypeDAO<DateEvent> {
	private static final Log log = LogFactory.getLog(DateEventDAO.class);
	// property constants
//	private static final String TYPE = "type";
//	private static final String DESCRIPTION = "description";
	public static final String PROJECT_SCHEDULE = "projectSchedule";

	public static DateEventDAO getInstance() {
		return (DateEventDAO)getInstance("DateEventDAO");
	}

//	public DateEvent findLastStandardDayOff() {
//		DateEvent event = null;
//		Object[] values = { WorkdayType.STANDARD_OFF };
//		String queryString = "from DateEvent" +
//				" where type = ? order by start desc";
//		event = findOne(queryString, values);
//		log.debug("event start=" + (event == null ? "null" : event.getStart()));
//		return event;
//	}

	public boolean existsByScheduleDateType(ProjectSchedule projectSchedule, Date date,
			WorkdayType type) {
		boolean bRet = false;
		Object[] values = { projectSchedule, date, type };
		String queryString = "from DateEvent " +
				" where projectSchedule = ? and start = ? and type = ?";
		bRet = exists(queryString, values);
		log.debug("existsByScheduleDateType, date=" + date + ", type=" + type + ", ret=" + bRet);
		return bRet;
	}

	/**
	 * Determine if a particular date is considered a HOLIDAY within the entire
	 * LS system.
	 *
	 * @param date The date to check.
	 * @return True iff the given date is stored as a "HOLIDAY" type in the
	 *         SYSTEM production.
	 */
//	public boolean existsSystemHoliday(Date date) {
//		String queryString = "from DateEvent " +
//				" where start = ? and type = 'HOLIDAY' and projectSchedule.id = 1 ";
//		boolean bRet = exists(queryString, date);
//		return bRet;
//	}

	/**
	 * Determine if a particular date is considered a HOLIDAY within the entire
	 * LS system. If so, return the type of the holiday.
	 *
	 * @param date The date to check.
	 * @return The appropriate HolidayType if the given date is stored as a
	 *         "HOLIDAY" type in the SYSTEM production, otherwise returns null.
	 */
	public HolidayType findSystemHolidayType(Date date) {
		String queryString = "from DateEvent " +
				" where start = ? and type = 'HOLIDAY' and projectSchedule.id = 1 ";
		DateEvent event = findOne(queryString, date);
		HolidayType type = null;
		if (event != null) {
			try {
				type = HolidayType.valueOf(event.getDescription());
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
		return type;
	}

	/**
	 * This method is used by ScheduleUtils to add a series of Standard-off days
	 * when the user changes the "normal weekdays off" setting.
	 *
	 * @param unit The Unit whose schedule is associated with these days
	 *            off.
	 * @param startCal The first day for which days off should be added.
	 * @param endCal The last day for which days off should be added. That is,
	 *            days off will be added for days up to, and including,
	 *            'endCal'.
	 * @param daysOff The boolean array indicating which days of the week are
	 *            off. Entry [0] is unused; [1] represents Sunday, and [7] is
	 *            Saturday.
	 */
	@Transactional
	public void addDaysOff(Unit unit, Calendar startCal, Calendar endCal, boolean[] daysOff) {
		while (! startCal.after(endCal)) { // loop on each week
			int i = startCal.get(Calendar.DAY_OF_WEEK);
			if (daysOff[i]) {
				Date date = startCal.getTime();
				if (! existsByScheduleDateType(unit.getProjectSchedule(),
						date, WorkdayType.STANDARD_OFF )) {
					DateEvent dateEvent = new DateEvent(unit.getProjectSchedule(),
							date, WorkdayType.STANDARD_OFF);
					save(dateEvent);
				}
			}
			startCal.add(Calendar.DAY_OF_MONTH, 1);
		}
//		evict(unit.getProjectSchedule());
//		evict(unit);
//		unit = UnitDAO.getInstance().findById(unit.getId());
	}

//	public static DateEventDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DateEventDAO) ctx.getBean("DateEventDAO");
//	}

}
