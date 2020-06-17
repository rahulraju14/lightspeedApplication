package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Talent;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * Talent entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.Talent
 * @author MyEclipse Persistence Tools
 */
public class TalentDAO extends BaseTypeDAO<Talent> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(TalentDAO.class);
	//property constants
	public static final String NAME = "name";
	public static final String MINOR = "minor";
	public static final String CATEGORY_ID = "categoryId";
	public static final String LOCATION = "location";
	public static final String SHOOT_DATE = "shootDate";

	public static TalentDAO getInstance() {
		return (TalentDAO)ServiceFinder.findBean("TalentDAO");
	}
	
	public List<Talent> findByName(Object name) {
		return findByProperty(NAME, name);
	}

	public List<Talent> findByMinor(Object minor) {
		return findByProperty(MINOR, minor);
	}

	public List<Talent> findByCategoryId(Object categoryId) {
		return findByProperty(CATEGORY_ID, categoryId);
	}

	public List<Talent> findByLocation(Object location) {
		return findByProperty(LOCATION, location);
	}
	
	public List<Talent> findByShootDate(Object shootDate) {
		return findByProperty(SHOOT_DATE, shootDate);
	}
}
