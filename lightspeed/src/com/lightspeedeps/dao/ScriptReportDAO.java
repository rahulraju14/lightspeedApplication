package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.ScriptReport;
import com.lightspeedeps.util.app.EventUtils;


/**
 * A data access object (DAO) providing persistence and search support for
 * ScriptReport entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ScriptReport
 */

public class ScriptReportDAO extends BaseTypeDAO<ScriptReport> {
	private static final Log log = LogFactory.getLog(ScriptReportDAO.class);

	// property constants
//	private static final String REPORT_ID = "reportId";
//	private static final String TYPE = "type";
//	private static final String PAGE_NUMBER = "pageNumber";
//	private static final String LINE_NUMBER = "lineNumber";
//	private static final String CHANGED = "changed";
//	private static final String TEXT = "text";

	public static ScriptReportDAO getInstance() {
		return (ScriptReportDAO)getInstance("ScriptReportDAO");
	}

//	public List<ScriptReport> findByReportId(Object reportId) {
//		return findByProperty(REPORT_ID, reportId);
//	}

	/**
	 * Save all the objects in the List to the database.
	 *
	 * @param buffer A List of non-null ScriptReport references. If buffer is
	 *            null, the operation is a no-op.
	 */
	@Transactional
	public void save(List<ScriptReport> buffer) {
		log.debug("");
		if (buffer != null) {
			try {
				for (ScriptReport sr : buffer) {
					getHibernateTemplate().save(sr);
				}
				log.debug(buffer.size() + " objects saved");
			}
			catch (RuntimeException re) {
				EventUtils.logError(re);
				throw re;
			}
		}
	}

//	public static ScriptDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ScriptDAO) ctx.getBean("ScriptDAO");
//	}

}
