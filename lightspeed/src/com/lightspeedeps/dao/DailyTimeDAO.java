package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyTimecard;

/**
 * A data access object (DAO) providing persistence and search support for
 * DailyTime entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.DailyTime
 */

public class DailyTimeDAO extends BaseTypeDAO<DailyTime> {
	private static final Log log = LogFactory.getLog(DailyTimeDAO.class);

	// property constants
//	public static final String DAY_NUM = "dayNum";
//	public static final String CALL_TIME = "callTime";
//	public static final String M1_OUT = "m1Out";
//	public static final String M1_IN = "m1In";
//	public static final String M2_OUT = "m2Out";
//	public static final String M2_IN = "m2In";
//	public static final String WRAP = "wrap";
//	public static final String HOURS = "hours";
//	public static final String DAY_TYPE = "dayType";
//	public static final String OPPOSITE = "opposite";
//	public static final String NON_DEDUCT_MEAL = "nonDeductMeal";
//	public static final String MPV_USER = "mpvUser";
//	public static final String GRACE1 = "grace1";
//	public static final String GRACE2 = "grace2";
//	public static final String LOCATION_CODE = "locationCode";
//	public static final String PROD_EPISODE = "prodEpisode";
//	public static final String ACCT_SET = "acctSet";
//	public static final String RE_RATE = "reRate";
//	public static final String OCC_CODE = "occCode";
//	public static final String STATE = "state";
//	public static final String MPV1_PAYROLL = "mpv1Payroll";
//	public static final String MPV2_PAYROLL = "mpv2Payroll";
//	public static final String JOB_NUM1 = "jobNum1";
//	public static final String JOB_NUM2 = "jobNum2";
//	public static final String JOB_NUM3 = "jobNum3";


	public static DailyTimeDAO getInstance() {
		return (DailyTimeDAO)getInstance("DailyTimeDAO");
	}

	/**
	 * Find all the DailyTime instances matching the given date and production
	 * prodId.
	 *
	 * @param date The date of interest.
	 * @param prodId The Production.prodId field to match.
	 * @return A non-null, but possibly empty, List of matching DailyTime`s.
	 */
	@SuppressWarnings("unchecked")
	public List<DailyTime> findByDateAndProdid(Date date, String prodId) {
		log.debug("date=" + date + ", prod=" + prodId);
		Object[] values = { date, prodId };
		String queryString = "from DailyTime dt where dt.date = ? " +
				" and dt.weeklyTimecard.prodId = ? ";
		return find(queryString, values);
	}

	/**
	 * Find all the DailyTime instances matching the given date and project.
	 * This is only applicable to Commercial productions.
	 *
	 * @param date The date of interest.
	 * @param project The Project that this timecard is related to.
	 * @return A non-null, but possibly empty, List of matching DailyTime`s.
	 */
	@SuppressWarnings("unchecked")
	public List<DailyTime> findByDateAndProject(Date date, Project project) {
		//log.debug("date=" + date);
		Object[] values = { date, project };
		String queryString = "from DailyTime dt where dt.date = ? " +
				" and dt.weeklyTimecard.startForm.project = ? ";
		return find(queryString, values);
	}

	public DailyTime findByDatedWtc(Date weekEndDate, WeeklyTimecard wtc) {
		String query = "from DailyTime where date=? and weeklyTimecard=?";
		List<Object> queryParms = new ArrayList<Object>();

		queryParms.add(weekEndDate);
		queryParms.add(wtc);
		return findOne(query, queryParms.toArray());
	}

//	public static DailyTimeDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DailyTimeDAO)ctx.getBean("DailyTimeDAO");
//	}
}