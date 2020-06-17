package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.StartRateSet;

/**
 * A data access object (DAO) providing persistence and search support for
 * StartRateSet entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.StartRateSet
 */
public class StartRateSetDAO extends BaseTypeDAO<StartRateSet> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StartRateSetDAO.class);


	public static StartRateSetDAO getInstance() {
		return (StartRateSetDAO)getInstance("StartRateSetDAO");
	}

}
