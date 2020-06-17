// File Name: HeaderViewBean.java
package com.lightspeedeps.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Callsheet entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Callsheet
 */

public class CallsheetDAO extends BaseTypeDAO<Callsheet> {
	private static final Log log = LogFactory.getLog(CallsheetDAO.class);
	// property constants
	public static final String PROJECT = "project";
//	private static final String SHOOT_DAY = "shootDay";
//	private static final String SHOOT_DAYS = "shootDays";
//	private static final String PAGES = "pages";
//	private static final String ADVANCE_DAYS = "advanceDays";
//	private static final String STATUS = "status";
//	//private static final String PUBLISHED = "published";
//	private static final String WEATHER = "weather";

	public static CallsheetDAO getInstance() {
		return (CallsheetDAO)getInstance("CallsheetDAO");
	}

//	public static CallsheetDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CallsheetDAO) ctx.getBean("CallsheetDAO");
//	}

	/**
	 * Find all Callsheet`s for the given Date and Project. This will range from
	 * 0 to as many as there are Unit`s in the Project.
	 *
	 * @param date The Date the Callsheet is for; the time portion of the Date
	 *            object must be 0 (12:00am) for this to match.
	 * @param project The Project the Callsheet is for.
	 * @return A non-null, but possibly empty, List of Callsheet`s meeting the
	 *         given criteria. The List is ordered in increasing order of
	 *         unitNumber.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findByDateAndProject(Date date, Project project) {
		Object[] values = {project, date};
		String queryString = "from Callsheet where project = ? and date = ?" +
				" order by unitNumber";
		return find(queryString, values);
	}

	/**
	 * Find the specific callsheet for the given Date and Project, for the Main
	 * unit (unit 1). There can never be more than one for this criteria.
	 *
	 * @param date The desired callsheet date.
	 * @param project The project the callsheet is for.
	 * @return Either the callsheet matching the request, or null if it does not
	 *         exist.
	 */
	public Callsheet findByDateAndProjectMain(Date date, Project project) {
		//log.debug("find by date & project, proj=" + project.getId() + ", date=" + date);
		Callsheet callsheet = null;
		try {
			Object[] values = {project, date};
			String queryString = "from Callsheet where project = ? and date = ?" +
					" and unitNumber = 1";
			callsheet = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return callsheet;
	}

	/**
	 * Find the specific callsheet for the given Date, Project, and Unit.
	 * There can never be more than one for this criteria.
	 *
	 * @param date The desired callsheet date.
	 * @param project The project the callsheet is for.
	 * @param unit The unit the callsheet is for.
	 * @return Either the callsheet matching the request, or null if it does not
	 *         exist.
	 */
	public Callsheet findByDateProjectUnit(Date date, Project project, Unit unit) {
		return findByDateProjectUnitNumber(date, project, unit.getNumber());
	}

	/**
	 * Find the specific callsheet for the given Date, Project, and Unit number.
	 * There can never be more than one for this criteria.
	 *
	 * @param date The desired callsheet date.
	 * @param project The project the callsheet is for.
	 * @param unitNumber The number of the unit the callsheet is for.
	 * @return Either the callsheet matching the request, or null if it does not
	 *         exist.
	 */
	public Callsheet findByDateProjectUnitNumber(Date date, Project project, Integer unitNumber) {
		//log.debug("proj=" + project.getId() + ", unit#=" + unitNumber + ", date=" + date);
		Callsheet callsheet = null;
		try {
			Object[] values = {project, date, unitNumber};
			String queryString = "from Callsheet where project = ? and date = ?" +
					" and unitNumber = ?"; // get lowest unit number if multiple exist
			callsheet = findOne(queryString, values);
			if (callsheet == null) {
				callsheet = findByDateRelatedProjectUnitNumber(date, project, unitNumber);
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return callsheet;
	}

	private Callsheet findByDateRelatedProjectUnitNumber(Date date, Project project, Integer unitNumber) {
		//log.debug("proj=" + project.getId() + ", unit#=" + unitNumber + ", date=" + date);
		try {
			Callsheet callsheet = null;
			project = ProjectDAO.getInstance().refresh(project);
			for (Callsheet cs : project.getCallsheets()) {
				if (date.equals(cs.getDate()) && unitNumber.equals(cs.getUnitNumber())) {
					callsheet = cs;
					break;
				}
			}
			return callsheet;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
	}

	/**
	 * Determine if a callsheet has been published for a specific date
	 * and project.
	 * @param date
	 * @param project
	 * @return True if a callsheet is found with the specified date, for the
	 * specified project, and that has its 'published' flag on; false otherwise.
	 */
	public boolean existsByStatusDateAndProject(ReportStatus status, Date date, Project project) {
		boolean found = false;
		try {
			Object[] values = { project, date, status };
			String queryString = "from Callsheet "
					+ " where project = ? and date = ? and status = ? ";
			found = exists(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return found;
	}

	/**
	 * Finds the update-date of the most recently updated, Published, Callsheet
	 * for the given project.
	 *
	 * @param project The project which contains the desired Callsheet.
	 * @return The update-date of the most recently updated Callsheet, or null
	 *         if no matching callsheet exists.
	 */
	public Date findLastUpdatedByProject( Project project ) {
		log.debug("id: " + project.getId());
		Date date = null;
		try {
			Object[] values = { project, ReportStatus.PUBLISHED };
			String queryString = "select updated from Callsheet where project = ? " +
					" and status = ? order by updated desc ";
			date = (Date)((BaseDAO)this).findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		log.debug("date: " + date);
		return date;
	}

	/**
	 * Finds the "current" call sheet that is in PUBLISHED status, within the
	 * given project.
	 *
	 * @param project The project which contains the desired call sheet.
	 * @return The first PUBLISHED call sheet associated with the specified
	 *         project with a crew-call time that is later than 4 hours ago; or
	 *         null if no such call sheet exists.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findCurrentByProjectAndUnit( Project project, Unit unit ) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -Constants.CS_PREFERRED_HOURS_USED); // backup to limit of "current" definition
		Object[] values = { cal.getTime(), project, unit.getNumber(), ReportStatus.PUBLISHED };
		String queryString = " from Callsheet where callTime > ? and project = ? " +
				" and unitNumber = ? and status = ? order by date asc ";
		// Note: the above query is "fragile" due to a Hibernate bug related to the text
		// "call" appearing (in 'callTime'). The "call" text must be before the "?" and "="
		// occurrences (in other words, keep 'callTime' as the first term in the query).
		// See http://opensource.atlassian.com/projects/hibernate/browse/HHH-3216
		// Fixed in Hibernate 3.2.7+ & 3.3.2+
		return find(queryString, values);
	}

	/**
	 * Find the latest callsheet within a given project whose date is prior to
	 * (less than) the given date.
	 *
	 * @param date The date of interest.
	 * @param project The project to be searched.
	 * @param unitNum The unit number to match against the callsheet; if null,
	 *            the unit number is ignored.
	 * @return The callsheet whose date precedes the specified date, and is in
	 *         the specified project with the matching unit number. Returns null
	 *         if no such callsheet is found.
	 */
	public Callsheet findPriorProject(Date date, Project project, Integer unitNum) {
		log.debug("find prior, proj=" + project.getId() + ", date=" + date);
		Callsheet callsheet = null;
		try {
			Object[] values = { project, date, unitNum };
			String queryString;
			if (unitNum == null) {
				Object[] values2 = { project, date };
				values = values2;
				queryString = "from Callsheet c where  c.project = ? and c.date < ? " +
						" order by date desc ";
			}
			else {
				queryString = "from Callsheet c where  c.project = ? and c.date < ? and unitNumber = ? " +
						" order by date desc ";
			}
			callsheet = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return callsheet;
	}

	/**
	 * Find the latest callsheet within a given Production whose date is prior
	 * to (less than) the given date, optionally matching on a Unit number.
	 *
	 * @param date The date of interest.
	 * @param prod The Production to be searched.
	 * @param unitNum The unit number to match against the callsheet; if null,
	 *            the unit number is ignored.
	 * @return The callsheet whose date precedes the specified date, and is in
	 *         the specified Production with the matching unit number. Returns
	 *         null if no such callsheet is found.
	 */
	public Callsheet findPriorProduction(Date date, Production prod, Integer unitNum) {
		log.debug("date=" + date);
		Callsheet callsheet = null;
		try {
			Object[] values = { prod, date, unitNum };
			String queryString;
			if (unitNum == null) {
				Object[] values2 = { prod, date };
				values = values2;
				queryString = "from Callsheet c where  c.project.production = ? and c.date < ? " +
						" order by date desc ";
			}
			else {
				queryString = "from Callsheet c where  c.project.production = ? and c.date < ? and unitNumber = ? " +
						" order by date desc ";
			}
			callsheet = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		return callsheet;
	}

	/**
	 * Return the List of published Callsheets for the given Unit, arranged in
	 * descending date order.
	 *
	 * @param unit The Unit for which Callsheets will be retrieved.
	 * @return A possibly empty (but not null) List of Callsheet's.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findByUnit(Unit unit) {
		log.debug("unit=" + unit.getId());
		Object[] values = { unit.getProject(), unit.getNumber() };
		return find(" from Callsheet where project = ? and unitNumber = ? order by date desc ",
				values);
	}

	/**
	 * Return the List of published Callsheets for the given Unit and status,
	 * arranged in descending date order.
	 *
	 * @param unit The Unit for which Callsheets will be retrieved.
	 * @param status The ReportStatus which all returned callsheets must have.
	 * @return A possibly empty (but not null) List of Callsheet's.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findByUnitAndStatus(Unit unit, ReportStatus status) {
		log.debug("unit=" + unit.getId());
		Object[] values = { unit.getProject(), unit.getNumber(), status };
		return find(" from Callsheet where project = ? and unitNumber = ? and status = ? order by date desc ",
				values);
	}

	/**
	 * Return the List of Callsheets for the given Project.
	 * @param project The Project for which Callsheets will be retrieved.
	 * @return A possibly empty (but not null) List of Callsheet's.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findByProject(Project project) {
		return find(" from Callsheet where project = ? ", project);
	}

	/**
	 * Return the List of Callsheets for the given Project and Unit, arranged in descending date order.
	 * @param project The Project for which Callsheets will be retrieved.
	 * @param unit The Unit for which Callsheets will be retrieved.
	 * @return A possibly empty (but not null) List of Callsheet's.
	 */
	@SuppressWarnings("unchecked")
	public List<Callsheet> findByProjectAndUnit(Project project, Unit unit) {
		log.debug("project=" + project.getId() + ", unit=" + unit.getId());
		Object[] values = { project, unit.getNumber() };
		return find(" from Callsheet where project = ? and unitNumber = ? order by date desc ",
				values);
	}

	/**
	 * Save a new Callsheet -- also adds the call sheet to all Project`s that it
	 * references.
	 *
	 * @see com.lightspeedeps.dao.BaseDAO#save(java.lang.Object)
	 */
	@Transactional
	public void store(Callsheet callsheet) {
		for (Project proj : callsheet.getProjects()) {
			proj.getCallsheets().add(callsheet);
		}
		save(callsheet);
	}

	/**
	 * Delete a call sheet and remove it from the "callsheets" collections of
	 * the Project`s to which it refers.
	 *
	 * @param callsheet The Callsheet to be deleted.
	 */
	@Transactional
	public void remove(Callsheet callsheet) {
		for (Project proj : callsheet.getProjects()) {
			proj.getCallsheets().remove(callsheet);
		}
		delete(callsheet);
	}

}
