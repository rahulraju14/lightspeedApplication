package com.lightspeedeps.dao;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Notification;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.type.NotificationType;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Notification entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Notification
 */

public class NotificationDAO extends BaseTypeDAO<Notification> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(NotificationDAO.class);

	// property constants

	public static NotificationDAO getInstance() {
		return (NotificationDAO)getInstance("NotificationDAO");
	}

//	@SuppressWarnings("unchecked")
//	public List<Notification> findByTypeProjectDateReportType(NotificationType type, Project project, Date date, ReportType reportType) {
//		log.debug("findByTypeProjectDateReportType: " + type + ", proj-id: " + project.getId() + ", date: " + date + ", rpt: " + type);
//		try {
//			Object[] values = { type, project, date, reportType };
//			String queryString = "from Notification where type = ? and project = ? and date =? and reportType = ? ";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	public boolean existsByTypeProjectDate(NotificationType type, Project project, Date date) {
		boolean ret = false;
		try {
			Object[] values = { type, project, date };
			String queryString = "from Notification where type = ? and project = ? and date = ? ";
			ret = exists(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return ret;
	}

	public boolean existsByTypeProjectDateReportType(NotificationType type, Project project, Date date, ReportType reportType) {
		boolean ret = false;
		try {
			Object[] values = { type, project, date, reportType };
			String queryString = "from Notification where type = ? and project = ? and date =? and reportType = ? ";
			ret = exists(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return ret;
	}

	public boolean existsByTypeProjectDateReportTypeWithinHours(NotificationType type, Project project,
			Date date, ReportType reportType, int hours) {
		boolean ret = false;
		try {
			Calendar timeLimit = Calendar.getInstance();
			timeLimit.add(Calendar.HOUR, - hours);
			Object[] values = { type, project, date, reportType, timeLimit.getTime() };
			String queryString = "from Notification " +
					"where type = ? and project = ? and date =? and reportType = ? " +
					"and createTime > ?";
			ret = exists(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return ret;
	}

//	public static NotificationDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (NotificationDAO) ctx.getBean("NotificationDAO");
//	}

}