package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.BoxRental;

/**
 * A data access object (DAO) providing persistence and search support for
 * BoxRental entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.BoxRental
 */

public class BoxRentalDAO extends BaseTypeDAO<BoxRental> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(BoxRentalDAO.class);

	// property constants
	public static final String AMOUNT = "amount";
	public static final String INVENTORY_ON_FILE = "inventoryOnFile";
	public static final String INVENTORY = "inventory";
	public static final String COMMENTS = "comments";

	public static BoxRentalDAO getInstance() {
		return (BoxRentalDAO)getInstance("BoxRentalDAO");
	}

//	public static BoxRentalDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (BoxRentalDAO)ctx.getBean("BoxRentalDAO");
//	}

}
