package com.lightspeedeps.dao;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.DprEpisode;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for DprEpisode
 * entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.DprEpisode
 */

public class DprEpisodeDAO extends BaseTypeDAO<DprEpisode> {
	private static final Log log = LogFactory.getLog(DprEpisodeDAO.class);

	public static DprEpisodeDAO getInstance() {
		return (DprEpisodeDAO)getInstance("DprEpisodeDAO");
	}

	/**
	 * Returns a list of DprEpisode for the specified project, in DESCENDING date
	 * order.
	 * @param project The project whose DPRs are to be located.
	 * @return A (possibly empty) List of DPRs, sorted in descending date order.
	 */
	@SuppressWarnings("unchecked")
	public List<DprEpisode> findByProject(Project project) {
		log.debug("find by project, id: " + project.getId());
		try {
			String queryString = " from DprEpisode where project = ? ";
			return find(queryString, project);

		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

//	/**
//	 * Find the DprEpisode matching the given date and Project. There should be at most
//	 * one that matches this criteria.
//	 *
//	 * @param date
//	 * @param project
//	 * @return The matching DPR, or null if a match is not found.
//	 */
//	public DprEpisode findByDateAndProject(Date date, Project project) {
//		DprEpisode dpr = null;
//		//log.debug("date=" + date + ", project=" + project.getId());
//		try {
//			Object[] values = { date, project };
//			String queryString = "from DprEpisode where dpr.date = ? and project = ?";
//			dpr = findOne(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//		return dpr;
//	}

	/**
	 * Find the last DprEpisode for the given Project which is earlier
	 * than the specified date.
	 * @param date
	 * @param project
	 * @return The specified DprEpisode, or null if not found.
	 */
	public DprEpisode findPrior( Date date, Project project ) {
		log.debug("find prior, proj=" + project.getId() + ", date=" + date);
		DprEpisode dprEpisode = null;
		try {
			Object[] values = {project, date};
			String queryString = "from DprEpisode where  project = ?" +
					" and dpr.date < ? order by dpr.date desc ";
			dprEpisode = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return dprEpisode;
	}

}
