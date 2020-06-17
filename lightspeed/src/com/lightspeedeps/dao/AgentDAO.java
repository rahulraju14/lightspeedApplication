package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Agent;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * Agent entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Agent
 */
public class AgentDAO extends BaseTypeDAO<Agent> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AgentDAO.class);
	//property constants
//	public static final String FIRST_NAME = "firstName";
//	public static final String LAST_NAME = "lastName";
//	public static final String DISPLAY_NAME = "displayName";
//	public static final String EMAIL_ADDRESS = "emailAddress";
//	public static final String OFFICE_PHONE = "officePhone";
//	public static final String AGENCY_NAME = "agencyName";
//	public static final String AGENCY_ADDRESS = "agencyAddress";

	public static AgentDAO getInstance() {
		return (AgentDAO)ServiceFinder.findBean("AgentDAO");
	}

}
