package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.util.payroll.WeeklyBatchUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * WeeklyBatch entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.WeeklyBatch
 */
public class WeeklyBatchDAO extends BaseTypeDAO<WeeklyBatch> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyBatchDAO.class);

	//property constants
	private static final String DATE = "date";
	private static final String NAME = "name";
	private static final String PRODUCTION = "production";
	private static final String PROJECT = "project";
	private static final String AGGREGATE = "aggregate";
	//private static final String STATUS = "status";

	public static WeeklyBatchDAO getInstance() {
		return (WeeklyBatchDAO)getInstance("WeeklyBatchDAO");
	}

	/**
	 * Find all the WeeklyBatch objects from the given Production and (optional)
	 * Project. The result will include "aggregate" batches.
	 *
	 * @param prod The Production of interest.
	 * @param project The project of interest; may be null if any project value
	 *            is acceptable.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances
	 *         matching the above description, sorted by ascending batch name.
	 */
	public List<WeeklyBatch> findByProductionProject(Production prod, Project project) {
		return findByProductionProjectDate(prod, project, null, true);
	}

	/**
	 * Find all the WeeklyBatch objects from the given Production and (optional)
	 * Project.
	 *
	 * @param prod The Production of interest.
	 * @param project The project of interest; may be null if any project value
	 *            is acceptable.
	 * @param aggregate If true, aggregate batches will be included in the
	 *            result; if false, aggregate batches are excluded from the
	 *            result.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances
	 *         matching the above description, sorted by ascending batch name.
	 */
	public List<WeeklyBatch> findByProductionProject(Production prod, Project project, boolean aggregate) {
		return findByProductionProjectDate(prod, project, null, aggregate);
	}

	/**
	 * Find all the WeeklyBatch objects from the given Production and (optional)
	 * Project with the given week-ending date, or within the preceding 6 days.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, this parameter is
	 *            ignored.
	 * @param date The date of interest or null if all dates are valid. This
	 *            date is assumed to be a Saturday date; any batch whose date is
	 *            within the given week will be included in the results.
	 * @param aggregate If true, aggregate batches will be included in the
	 *            result; if false, aggregate batches are excluded from the
	 *            result.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances
	 *         matching the above description, ordered by ascending batch name.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyBatch> findByProductionProjectDate(Production prod, Project project,
			Date date, boolean aggregate) {
		String query = "from WeeklyBatch where " + PRODUCTION + " = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod);

		if (! aggregate) {
			query += " and " + AGGREGATE + " = false ";
		}

		if (date != null) {
			valueList.add(date);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);	// last date in week
			cal.add(Calendar.DAY_OF_WEEK, -6); // = first date in week
			valueList.add(cal.getTime());
			query += " and " + DATE + " <= ? and " + DATE + " >= ? ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}
		query += " order by " + NAME;

		return find(query, valueList.toArray());
	}

	/**
	 * Find all the WeeklyBatch objects from the given Production and (optional)
	 * Project PRIOR TO the given week-ending date.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, this parameter is
	 *            ignored.
	 * @param date The date of interest; timecards with dates earlier
	 *            than this date will be selected.
	 * @param aggregate If true, aggregate batches will be included in the
	 *            result; if false, aggregate batches are excluded from the
	 *            result.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances
	 *         matching the above description, ordered by ascending batch name.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyBatch> findByProductionProjectPriorDate(Production prod, Project project,
			Date date, boolean aggregate) {
		String query = "from WeeklyBatch where " +
				PRODUCTION + " = ? and " +
				DATE + " < ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod);
		valueList.add(date);

		if (! aggregate) {
			query += "and " + AGGREGATE + " = false ";
		}

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}
		query += " order by " + NAME;

		return find(query, valueList.toArray());
	}

	/**
	 * Find all the WeeklyBatch objects from the given Production and (optional)
	 * Project with the given name and week-ending date.
	 *
	 * @param prod The Production of interest.
	 * @param project The project of interest; may be null if any project value
	 *            is acceptable.
	 * @param date The date of interest.
	 * @return A non-null, but possibly empty, List of WeeklyBatch instances
	 *         matching the above description.
	 */
	@SuppressWarnings("unchecked")
	public List<WeeklyBatch> findByProductionProjectNameDate(Production prod, Project project, String name, Date date) {
		String query = "from WeeklyBatch where " +
				PRODUCTION + " = ? and " +
				NAME + " = ? and " +
				DATE + " = ? ";

		List<Object> valueList = new ArrayList<>();
		valueList.add(prod);
		valueList.add(name);
		valueList.add(date);

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}

		return find(query, valueList.toArray());
	}

	/**
	 * Move a WeeklyTimecard from one batch (or unbatched state) to a different
	 * batch (or unbatched state).
	 *
	 * @param timecards The WeeklyTimecard to be moved.
	 * @param toWb The batch to which the timecard should be added. If null, the
	 *            timecard will be made un-batched.
	 */
	@Transactional
	public String moveTimecard(List<WeeklyTimecard> timecards, WeeklyBatch toWb) {
		String response = "ok";
		WeeklyBatch fromWb = timecards.get(0).getWeeklyBatch();
//		Set<WeeklyTimecard> sentTimecards = new HashSet<WeeklyTimecard>();
		for (WeeklyTimecard wtc : timecards) {
//			if (wtc.getTimeSent() != null) { // already transmitted to payroll service
//				sentTimecards.add(wtc);
//			}
			moveTimecard(wtc, fromWb, toWb);
		}

		if (fromWb != null && fromWb.getAggregate()) {
			if (! WeeklyBatchUtils.checkAndSetAggregate(fromWb)) {
				// was an aggregate batch, but is not now...
				attachDirty(fromWb); // update it.
			}
		}
		if (toWb != null && (! toWb.getAggregate()) && toWb.getProduction().getType().isAicp()) {
			if (WeeklyBatchUtils.checkAndSetAggregate(toWb)) {
				// was not an aggregate batch, but now it is...
				attachDirty(toWb); // update it.
			}
		}
		return response;
	}

	private void moveTimecard(WeeklyTimecard wtc, WeeklyBatch fromWb, WeeklyBatch toWb) {
		if (fromWb != null) {
			fromWb.getTimecards().remove(wtc);
		}
		wtc.setWeeklyBatch(toWb);
		if (toWb != null) {
			toWb.getTimecards().add(wtc);
		}
		attachDirty(wtc);
	}

}
