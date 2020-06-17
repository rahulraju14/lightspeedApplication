package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PageFieldAccess;

/**
 * A data access object (DAO) providing persistence and search support for
 * PageFieldAccess entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.PageFieldAccess
 */

public class PageFieldAccessDAO extends BaseTypeDAO<PageFieldAccess> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PageFieldAccessDAO.class);
	// property constants
//	private static final String PAGE = "page";
//	private static final String FIELD = "field";

	public static PageFieldAccessDAO getInstance() {
		return (PageFieldAccessDAO)getInstance("PageFieldAccessDAO");
	}

//	public static PageFieldAccessDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (PageFieldAccessDAO) ctx.getBean("PageFieldAccessDAO");
//	}

	/**
	 * Find all the PageFieldAcess records whose Permission value matches any
	 * one of the id's listed in the permIds parameter.
	 *
	 * @param permIds A list of database ids of Permission objects, separated by
	 *            commas and enclosed in parentheses. E.g., "(1,5,10,11,12)".
	 * @return A non-null, but possibly empty, List of matching PageFieldAccess
	 *         objects.
	 */
	@SuppressWarnings("unchecked")
	public List<PageFieldAccess> findByPermIds(String permIds) {
		String queryString = "from PageFieldAccess where permissionId in " + permIds;
		return find(queryString);
	}

}