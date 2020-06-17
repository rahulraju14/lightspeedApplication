package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.TalentCategory;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * TalentCategory entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.TalentCategory
 * @author MyEclipse Persistence Tools
 */
public class TalentCategoryDAO extends BaseTypeDAO<TalentCategory> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TalentCategoryDAO.class);
	//property constants
	public static final String CATEGORY_CODE = "categoryCode";

	public static TalentCategoryDAO getInstance() {
		return (TalentCategoryDAO)ServiceFinder.findBean("TalentCategoryDAO");
	}

	public List<TalentCategory> findByCategoryCode(Object categoryCode) {
		return findByProperty(CATEGORY_CODE, categoryCode);
	}
}
