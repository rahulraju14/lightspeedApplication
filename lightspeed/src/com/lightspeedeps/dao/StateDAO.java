package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.State;

/**
 * A data access object (DAO) providing persistence and search support for State
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.State
 */

public class StateDAO extends BaseTypeDAO<State> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StateDAO.class);
	// property constants
//	private static final String CODE = "code";
//	private static final String NAME = "name";

	public static StateDAO getInstance() {
		return (StateDAO)getInstance("StateDAO");
	}

//	public static StateDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StateDAO) ctx.getBean("StateDAO");
//	}

	/**
	 * Find list of states belonging to the country with the given ISO code.
	 * @param countryCode The ISO code (usually 2 letters) for the country of interest.
	 * @return A List<State>, which may be empty, for all the States in that Country.
	 */
	@SuppressWarnings("unchecked")
	public List<State> findByCountryCode(String countryCode) {
		Object [] values={countryCode};
		String queryString = "from State st where  st.country.isoCode= ? " ;
		return find(queryString, values);
	}

}