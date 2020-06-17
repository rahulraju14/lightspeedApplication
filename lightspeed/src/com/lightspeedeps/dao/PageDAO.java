package com.lightspeedeps.dao;

import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.Page;

/**
 * A data access object (DAO) providing persistence and search support for Page
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Page
 */

public class PageDAO extends BaseTypeDAO<Page> {

	// property constants
//	private static final String NUMBER = "number";
//	private static final String PAGE_NUM_STR = "pageNumStr";
//	private static final String LAST_REVISED = "lastRevised";
//	private static final String HASH = "hash";

	public static PageDAO getInstance() {
		return (PageDAO)getInstance("PageDAO");
	}

	public static PageDAO getFromApplicationContext(ApplicationContext ctx) {
		return (PageDAO)ctx.getBean("PageDAO");
	}

}
