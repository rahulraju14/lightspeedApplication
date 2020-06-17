package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Country;

/**
 * A data access object (DAO) providing persistence and search support for
 * Country entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Country
 */

public class CountryDAO extends BaseTypeDAO<Country> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CountryDAO.class);
	// property constants
	private static final String ISO_CODE = "isoCode";
	private static final String NAME = "name";

	public static CountryDAO getInstance() {
		return (CountryDAO)getInstance("CountryDAO");
	}

	public List<Country> findByIsoCode(Object isoCode) {
		return findByProperty(ISO_CODE, isoCode);
	}

	public List<Country> findByName(Object name) {
		return findByProperty(NAME, name);
	}

//	public static CountryDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CountryDAO) ctx.getBean("CountryDAO");
//	}

}