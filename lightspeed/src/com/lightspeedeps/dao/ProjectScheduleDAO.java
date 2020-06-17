package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ProjectSchedule;

/**
 * A data access object (DAO) providing persistence and search support for
 * ProjectSchedule entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ProjectSchedule
 */

public class ProjectScheduleDAO extends BaseTypeDAO<ProjectSchedule> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ProjectScheduleDAO.class);
	// property constants
//	private static final String DAYS_OFF = "daysOff";

	public static ProjectScheduleDAO getInstance() {
		return (ProjectScheduleDAO)getInstance("ProjectScheduleDAO");
	}

//	public static ProjectScheduleDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (ProjectScheduleDAO) ctx.getBean("ProjectScheduleDAO");
//	}

}