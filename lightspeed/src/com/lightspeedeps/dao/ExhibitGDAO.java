package com.lightspeedeps.dao;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ExhibitG;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * ExhibitG entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ExhibitG
 */

public class ExhibitGDAO extends BaseTypeDAO<ExhibitG> {
	private static final Log log = LogFactory.getLog(ExhibitGDAO.class);
	// property constants
//	private static final String STATUS = "status";
//	private static final String TITLE = "title";
//	private static final String COMPANY = "company";
//	private static final String LOCATION = "location";
//	private static final String PRODUCTION_NUMBER = "productionNumber";
//	private static final String CONTACT_NAME = "contactName";
//	private static final String CONTACT_PHONE = "contactPhone";
//	private static final String DAY_OFF = "dayOff";

	public static ExhibitGDAO getInstance() {
		return (ExhibitGDAO)getInstance("ExhibitGDAO");
	}

	/**
	 * Returns a list of ExhibitG for the specified project, in DESCENDING date
	 * order.
	 * @param project The project whose ExhibitG are to be located.
	 * @return A (possibly empty) List of ExhibitG, sorted in descending date order.
	 */
	@SuppressWarnings("unchecked")
	public List<ExhibitG> findByProject(Project project) {
		log.debug("find by project, id: " + project.getId());
		try {
			String queryString = " from ExhibitG where project =? order by date desc ";
			return find(queryString, project);

		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

//	public static ExhibitGDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ExhibitGDAO) ctx.getBean("ExhibitGDAO");
//	}

	/**
	 * Find the last ExhibitG record prior to the specified date, within the specified
	 * project.
	 * @param date
	 * @param project
	 * @return The matching ExhibitG, or null if not found
	 */
	public ExhibitG findPrior(Date date, Project project) {
		log.debug("date: " + date + ", project: " + project.getId());
		ExhibitG exhibitG = null;
		try {
			Object[] values = { project, date };
			String queryString = "from ExhibitG eg where project = ? and date < ? order by date desc";
			exhibitG = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return exhibitG;
	}

	/**
	 * Returns a list of ExhibitG objects for the given project on the specified
	 * date.  Note that this should normally be a single instance (if any)
	 */
	public ExhibitG findByDateAndProject(Date date, Project project) {
		ExhibitG exhibitG = null;
		log.debug("finding ExhibitG with Project Id=" + project.getId() + ", date=" + date);
		try {
			Object[] values = { project, date };
			String queryString = "from ExhibitG where project =? and date = ?";
			exhibitG = findOne(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return exhibitG;
	}

//	/**
//	 * Determine if an ExhibitG exists with a given "publish" status for a specific date
//	 * and project.
//	 * @param status The value of the status flag byte.
//	 * @param date The date to match
//	 * @param project The project containing the DPR.
//	 * @return True if an ExhibitG is found with the specified date, for the
//	 * specified project, and that has its 'published' flag set to the value requested; false otherwise.
//	 */
//	public boolean existsByStatusDateAndProject(ReportStatus status, Date date, Project project) {
//		boolean ret = false;
//		try {
//			Object[] values = { project, date, status };
//			String queryString = "from ExhibitG "
//					+ " where project = ? and date = ? and status = ?";
//			ret = exists(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//		return ret;
//	}

}