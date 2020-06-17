package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.MileageLine;

/**
 * A data access object (DAO) providing persistence and search support for
 * MileageLine entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.MileageLine
 */

public class MileageLineDAO extends BaseTypeDAO<MileageLine> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MileageLineDAO.class);

	// property constants
	public static final String DESTINATION = "destination";
	public static final String ODOMETER_START = "odometerStart";
	public static final String ODOMETER_END = "odometerEnd";
	public static final String MILES = "miles";

	public static MileageLineDAO getInstance() {
		return (MileageLineDAO)getInstance("MileageLineDAO");
	}

//	public static MileageLineDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (MileageLineDAO)ctx.getBean("MileageLineDAO");
//	}

}
