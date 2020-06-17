package com.lightspeedeps.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.ReportTime;

/**
 * A data access object (DAO) providing persistence and search support for
 * TimeCard entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ReportTime
 */

public class ReportTimeDAO extends BaseTypeDAO<ReportTime> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ReportTimeDAO.class);

	// property constants

//	private static final String DTYPE = "dtype";
	private static final String CONTACT = "contact";
	private static final String DPR_CREW = "dprCrew";
//	private static final String ROLE = "role";
//	private static final String CAST_ID = "castId";
//	private static final String MINOR = "minor";
//	private static final String DAY_TYPE = "dayType";
//	private static final String WARDROBE_OUTFITS = "wardrobeOutfits";
//	private static final String MEAL_PERIOD_VIOLATIONS = "mealPeriodViolations";
//	private static final String FORCED_CALL = "forcedCall";

	public static ReportTimeDAO getInstance() {
		return (ReportTimeDAO)getInstance("ReportTimeDAO");
	}

//	public static TimeCardDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (TimeCardDAO) ctx.getBean("TimeCardDAO");
//	}

//	/***
//	 * Returns the Cast TimeCards for a particular TimeSheet. Used for Exhibit G
//	 * and the Cast section of DPR.
//	 *
//	 * @param timeSheet The TimeSheet whose TimeCard's are to be returned.
//	 * @return A non-null (but possibly empty) List of TimeCard's for Cast
//	 *         members, where the TimeCard is associated with the given
//	 *         TimeSheet. The List is in ascending order by the castId values.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<TimeCard> timeCardExhibitGData(TimeSheet timeSheet) {
//		log.debug("finding timeCardExhibitGData ");
//		try {
//			Object[] values = { timeSheet, TimeCard.DTYPE_CAST };
//			String queryString = "select tc from TimeCard tc " +
//					" where tc.timeSheet = ? and tc.dtype = ? order by castId";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Returns the TimeCards corresponding to a particular TimeSheet and which
	 * are for crew members (not cast). Used for the Crew section (back page) of
	 * DPR.
	 */
//	@SuppressWarnings("unchecked")
//	public List<TimeCard> timeCardCrewData(Date date, Integer projectId) {
//		log.debug("findingtimeCardExhibitGData ");
//		try {
//				Object [] values={date, projectId};
//				String queryString = "select tc from TimeSheet ts, TimeCard tc where ts.date = ? and ts.project.id = ? and tc.timeSheet.id = ts.id and tc.castId is null";
//				return find(queryString,values);
//	        }
//			catch (RuntimeException re) {
//	        	EventUtils.logError(re);
//				throw re;
//			}
//	}

//	/**
//	 * Find all TimeCard`s that match the given date and project id.
//	 *
//	 * @param date The Date of interest.
//	 * @param projectId The project.id value of interest.
//	 * @return A non-null List of matching items.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<TimeCard> findByDateProject(Date date, Integer projectId) {
//		Object [] values={date, projectId};
//		String queryString = "select tc from TimeSheet ts, TimeCard tc " +
//				" where ts.date = ? and ts.project.id = ? and tc.timeSheet.id = ts.id ";
//		return find(queryString,values);
//	}

	/**
	 * Find the User.accountNumber of all the Contact`s that appear in a week's
	 * worth of DPR crew entries.
	 *
	 * @param prod The Production to which all the searched DPRs must belong.
	 * @param date The ending date of the week's worth of DPRs to be searched.
	 * @return A non-null, but possibly empty, List of String`s, where each
	 *         entry is the User.accountNumber of the User relating to a Contact
	 *         which appears in the crew section of a DPR, and that DPR is for
	 *         the given Production, and the date of the DPR is within the 7-day
	 *         period ending on the given date.
	 */
	@SuppressWarnings("unchecked")
	public List<String> findCrewOnDPRs(Production prod, Date date) {

		// calculate week start date from given end date
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, -6);
		Date startDate = cal.getTime();

		String query = "select distinct u.accountNumber from ReportTime tc, Dpr d, User u, Contact c" +
				" where tc." + DPR_CREW + " = d " +
				" and d.date >= ? " +
				" and d.date <= ? " +
				" and tc." + CONTACT + " = c " +
				" and c.production = ? " +
				" and c.user = u ";
		Object [] values={startDate, date, prod};

		return find(query, values);
	}

	/**
	 * Determine if any TimeCard`s reference the given Department.
	 *
	 * @param dept The Department of interest.
	 * @return True iff at least one TimeCard references the specified
	 *         Department, which means that some DPR has a crew-time
	 *         entry referring to that Department.
	 */
	public boolean referencesDept(Department dept) {
		String query = "from ReportTime where department = ?";
		return exists(query, dept);
	}

//	public boolean existsContact(Contact contact) {
//		String query = "select count(id) from TimeCard where contact = ? ";
//		return findCount(query, contact) > 0;
//	}

}