package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Commercial;

/**
 * A data access object (DAO) providing persistence and search support for
 * Commercial entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.Commercial
 * @author MyEclipse Persistence Tools
 */
public class CommercialDAO extends BaseTypeDAO<Commercial> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(CommercialDAO.class);
	//property constants
	public static final String NAME = "name";
	public static final String LOCATION = "location";
	public static final String LENGTH = "length";

	public List<Commercial> findByName(Object name) {
		return findByProperty(NAME, name);
	}

	public List<Commercial> findByLocation(Object location) {
		return findByProperty(LOCATION, location);
	}

	public List<Commercial> findByLength(Object length) {
		return findByProperty(LENGTH, length);
	}


}
