package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.icu.util.Calendar;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Dpr;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ReportTime;
import com.lightspeedeps.object.DailyHours;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for Dpr
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Dpr
 */

public class DprDAO extends BaseTypeDAO<Dpr> {
	private static final Log log = LogFactory.getLog(DprDAO.class);
	// property constants
//	private static final String STATUS = "status";
//	private static final String SHOOT_DAY = "shootDay";
//	private static final String SHOOT_DAYS = "shootDays";
//	private static final String DIRECTOR = "director";
//	private static final String PRODUCER = "producer";
//	private static final String UPM = "upm";
//	private static final String FIRST_AD = "firstAd";
//	private static final String SECOND_AD = "secondAd";
//	private static final String SETS = "sets";
//	private static final String COMPLETED_SCENES = "completedScenes";
//	private static final String PARTIAL_SCENES = "partialScenes";
//	private static final String SCHEDULED_NOT_SHOT = "scheduledNotShot";
//	private static final String SCENE_NOTE = "sceneNote";
//	private static final String OVERTIME_USED_PRIOR = "overtimeUsedPrior";
//	private static final String OVERTIME_USED_TODAY = "overtimeUsedToday";
//	private static final String OVERTIME_USED_TOTAL = "overtimeUsedTotal";
//	private static final String OVERTIME_REMAINING = "overtimeRemaining";
//	private static final String SOUND_CARD_ID_NUM = "soundCardIdNum";
//	private static final String EQUIPMENT_NOTES = "equipmentNotes";
//	private static final String PRODUCTION_NOTES = "productionNotes";
//	private static final String CREW_NOTES = "crewNotes";

	public static DprDAO getInstance() {
		return (DprDAO)getInstance("DprDAO");
	}

	/**
	 * Returns a list of DPRs for the specified project, in DESCENDING date
	 * order.
	 * @param project The project whose DPRs are to be located.
	 * @return A (possibly empty) List of DPRs, sorted in descending date order.
	 */
	@SuppressWarnings("unchecked")
	public List<Dpr> findByProject(Project project) {
		log.debug("find by project, id: " + project.getId());
		try {
			String queryString = " from Dpr where project =? order by date desc ";
			return find(queryString, project);

		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

//	public static DprDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DprDAO) ctx.getBean("DprDAO");
//	}

	/**
	 * Determine if a DPR exists with a given status for a specific date
	 * and project.
	 * @param status The value of the status enumeration.
	 * @param date The date to match
	 * @param project The project containing the DPR.
	 * @return True if a callsheet is found with the specified date, for the
	 * specified project, and that has its status set as given.  False otherwise.
	 */
	public boolean existsByStatusDateAndProject(ReportStatus status, Date date, Project project) {
		Object[] values = { project, date, status };
		String queryString = "from Dpr where project = ? and date = ? and status = ?";
		boolean b = exists(queryString, values);
		log.debug("exists=" + b + ", proj=" + project.getId() + ", date=" +
				(date) + ", pub=" + status);
		return b;
	}

	/**
	 * Find the DPR matching the given date and Project. There should be at most
	 * one that matches this criteria.
	 *
	 * @param date
	 * @param project
	 * @return The matching DPR, or null if a match is not found.
	 */
	public Dpr findByDateAndProject(Date date, Project project) {
		Dpr dpr = null;
		try {
			Object[] values = { date, project };
			String queryString = "from Dpr where date = ? and project = ?";
			dpr = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return dpr;
	}

	/**
	 * Find the DPR matching the given date and Production. There may be multiple
	 * ones that match this criteria; the most recently updated is returned.
	 *
	 * @param date The date of the desired DPR.
	 * @param prod The Production which contains the DPR of interest.
	 * @return The matching DPR, or null if a match is not found.
	 */
	public Dpr findByDateAndProduction(Date date, Production prod) {
		Dpr dpr = null;
		//log.debug("date=" + date + ", prod=" + prod.getId());
		try {
			Object[] values = { date, prod };
			String queryString = "select d from Dpr d " +
					" where d.date = ? and " +
					" d.project.production = ? " +
					" order by d.updated desc ";
			dpr = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return dpr;
	}

	/**
	 * Find the most recent DPR prior to the given date.
	 * @param date
	 * @param project
	 * @return The DPR found, or null if none found.
	 */
	public Dpr findPrior( Date date, Project project ) {
		Dpr dpr = null;
		try {
			Object[] values = {project, date};
			String queryString = "from Dpr where  project = ? " +
					" and date < ? order by date desc ";
			dpr = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return dpr;
	}

//	@SuppressWarnings("unchecked")
//	public List<Dpr> findMaxDate(Project project) {
//		log.debug("finding max date: " + project.getId());
//
//		try {
//			Object[] values = { project, project };
//			String queryString = "from Dpr where project =? and date = (select max(dpr1.date) from Dpr dpr1 where dpr1.project = ?)";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Get a List of DailyHours objects that contain the crew times from the 7
	 * days ending on the specified "weekEndDate". These are used to determine
	 * discrepancies between DPRs and timecards.
	 *
	 * @param weekEndDate The week-ending date which determines the seven DPRs
	 *            this method will attempt to locate.
	 * @param contact The specific Contact whose reported call and wrap times
	 *            should be returned.
	 * @param occupation The occupation of the Contact; this value must match
	 *            one of the Contact's entries in the DPR.
	 * @param project The Project that the DPR should match; if null, the DPR's
	 *            project is ignored. Should be non-null only for Commercial
	 *            productions.
	 * @param weekOfDprs Array of 7 DPRs corresponding to the seven days of the
	 *            week. It may contain null entries, in which case this method
	 *            will attempt to find the appropriate DPR and will fill in the
	 *            entry. (This avoids the need to search for DPRs for every call
	 *            to this method.)
	 *
	 * @return A List of 7 DailyHours objects. Note that the objects
	 *         corresponding to dates when no DPR exists will be empty except
	 *         for the date. The weekOfDprs array may be updated.
	 */
	public List<DailyHours> findDailyHours(Date weekEndDate, Contact contact, String occupation,
			Project project, Dpr[] weekOfDprs) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(weekEndDate);
		Production prod = SessionUtils.getProduction();
		List<DailyHours> hours = new ArrayList<DailyHours>();
		DailyHours dh;
		Date dprDate = new Date();
		Boolean someDprs = null;
		for (int i = 6; i >= 0; i--) {
			//log.debug(cal.getTime());
			Dpr dpr = weekOfDprs[i];
			if (dpr != null) {
				// This method of extracting the date is necessary so the "equals" check later works.
				// It's because dpr.getDate() is a "Timestamp".
				dprDate.setTime(dpr.getDate().getTime());
			}
			if (dpr == null ||
					! dprDate.equals(cal.getTime()) ||
					(project != null && ! project.getId().equals(dpr.getProject().getId()))) {
				// Dpr not yet found, or it's the wrong one
				if (someDprs == null) { // haven't done our "quick check" yet...
					// do a quick check to see if ANY DPRs exist for the week
					someDprs = existsByProductionAndWeek(prod, weekEndDate);
				}
				if (someDprs != null && someDprs) {
					if (project == null) {
						dpr = findByDateAndProduction(cal.getTime(), prod);
					}
					else {
						dpr = findByDateAndProject(cal.getTime(), project);
					}
				}
				else {
					dpr = null;
				}
				weekOfDprs[i] = dpr;
			}
			else {
				dpr = refresh(dpr);
				someDprs = true; // at least one exists in the target week
			}
			if (dpr != null) {
				dh = new DailyHours(dpr); // creates a DailyHours with the common DPR info including meal times
				if (contact != null) {
					updateHours(dh, dpr, contact, occupation);
				}
			}
			else {
				dh = new DailyHours();
				dh.setDate(cal.getTime());
			}
			hours.add(0,dh); // insert at front of list
			cal.add(Calendar.DAY_OF_MONTH, -1);
		}
		return hours;
	}

	/**
	 * Determine if any DPR exists for the given Production within the week that
	 * ends on the specified date.
	 *
	 * @param prod The Production of interest.
	 * @param weekEndDate The last date of the week of interest.
	 * @return True iff at least one DPR exists in the specified Production,
	 *         with a date less than or equal to the given date, and greater
	 *         than or equal to the date 6 days earlier.
	 */
	private boolean existsByProductionAndWeek(Production prod, Date weekEndDate) {
		boolean ret = true;
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(weekEndDate); // calculate date...
		startCal.add(Calendar.DAY_OF_MONTH, -6); // ...of start of week.

		Object[] values = { weekEndDate, startCal.getTime(), prod };

		String queryString = "from Dpr d " +
				" where date <= ? and " +
				" date >= ? and " +
				" d.project.production = ? ";

		ret = exists(queryString, values);
		return ret;
	}

	/**
	 * Update a DailyHours object with information from the given DPR by finding
	 * the crew entry in the DPR that matches the given Contact and occupation.
	 *
	 * @param dh The existing DailyHours object to update. (input/output
	 *            parameter)
	 * @param dpr The DPR which will be searched for a matching Contact &
	 *            occupation.
	 * @param contact The Contact of interest.
	 * @param occupation The occupation (job name) of interest.
	 *
	 */
	private void updateHours(DailyHours dh, Dpr dpr, Contact contact, String occupation) {
		Integer id = contact.getId();
		List<ReportTime> matches = new ArrayList<ReportTime>();
		// Find all the DPR entries that match the desired Contact
		for (ReportTime tc : dpr.getCrewTimeCards()) {
			if (tc.getContact() != null && tc.getContact().getId().equals(id)) {
				matches.add(tc);
			}
		}
		if (matches.size() > 0) {
			// Found at least one
			ReportTime tc = matches.get(0);
			if (matches.size() > 1) {
				// If more than one match, try & match the occupation
				for (ReportTime tryTc : matches) {
					if (tryTc.getRole() != null && tryTc.getRole().equals(occupation)) {
						tc = tryTc;
						break;
					}
				}
			}
			dh.setContactMatch(true);
			dh.setCallTime(tc.getCallTime());
			dh.setWrap(tc.getWrap());
			Byte mpv1 = null, mpv2 = null;
			if (tc.getMpv1() != null && tc.getMpv1().length() > 0) {
				try {
					mpv1 = Byte.parseByte(tc.getMpv1());
				}
				catch (NumberFormatException e) {}
			}
			if (tc.getMpv2() != null && tc.getMpv2().length() > 0) {
				try {
					mpv2 = Byte.parseByte(tc.getMpv2());
				}
				catch (NumberFormatException e) {}
			}
			dh.setMpv1(mpv1);
			dh.setMpv2(mpv2);
			dh.calcHours();
		}
	}

}
