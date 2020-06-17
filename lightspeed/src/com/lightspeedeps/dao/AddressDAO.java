package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;

/**
 * A data access object (DAO) providing persistence and search support for
 * Address entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Address
 */
public class AddressDAO extends BaseTypeDAO<Address> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AddressDAO.class);

	// property constants
//	private static final String ADDR_LINE1 = "addrLine1";
//	private static final String ADDR_LINE2 = "addrLine2";
//	private static final String CITY = "city";
//	private static final String STATE = "state";
//	private static final String ZIP = "zip";
//	private static final String COUNTRY = "country";
//	private static final String TIMEZONE = "timezone";
//	private static final String MAP_START = "mapStart";
//	private static final String MAP_LINK = "mapLink";

	public static AddressDAO getInstance() {
		return (AddressDAO)getInstance("AddressDAO");
	}

//	public List<Address> findByZip(Object zip) {
//		return findByProperty(ZIP, zip);
//	}

//	public static AddressDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (AddressDAO) ctx.getBean("AddressDAO");
//	}

}