package com.lightspeedeps.dao;

import com.lightspeedeps.model.ReportRequirement;

/**
 * A data access object (DAO) providing persistence and search support for
 * ReportRequirement entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ReportRequirement
 */

public class ReportRequirementDAO extends BaseTypeDAO<ReportRequirement> {

	// property constants
//	private static final String TYPE = "type";
//	private static final String DESCRIPTION = "description";
//	private static final String FREQUENCY = "frequency";

	public static ReportRequirementDAO getInstance() {
		return (ReportRequirementDAO)getInstance("ReportRequirementDAO");
	}

//	public static ReportRequirementDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (ReportRequirementDAO) ctx.getBean("ReportRequirementDAO");
//	}

}