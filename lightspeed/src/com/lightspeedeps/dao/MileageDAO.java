package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Mileage;

/**
 * A data access object (DAO) providing persistence and search support for
 * Mileage entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Mileage
 */

public class MileageDAO extends BaseTypeDAO<Mileage> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MileageDAO.class);

	// property constants
	//private static final String COMMENT = "comment";

	public static MileageDAO getInstance() {
		return (MileageDAO)getInstance("MileageDAO");
	}

//	public static MileageDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (MileageDAO)ctx.getBean("MileageDAO");
//	}

}
