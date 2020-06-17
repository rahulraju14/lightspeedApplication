//	File Name:	StripboardReportDAO.java
package com.lightspeedeps.dao;

import com.lightspeedeps.model.StripboardReport;

/**
 * A data access object (DAO) providing persistence and search support for
 * StripboardReport entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.StripboardReport
 */
public class StripboardReportDAO extends BaseTypeDAO<StripboardReport> {

	public static StripboardReportDAO getInstance() {
		return (StripboardReportDAO)getInstance("StripboardReportDAO");
	}

//	public static StripboardReportDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StripboardReportDAO) ctx.getBean("StripboardReportDAO");
//	}

}