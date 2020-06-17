package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.SideLetter;

/**
 * A data access object (DAO) providing persistence and search support for
 * SideLetter entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.SideLetter
 * @author MyEclipse Persistence Tools
 */
public class SideLetterDAO extends BaseTypeDAO<SideLetter> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SideLetterDAO.class);
	
	//property constants
	public static final String SIDE_LETTER_CODE = "sideLetterCode";
	public static final String SIDE_LETTER_DESC = "sideLetterDesc";

	public static SideLetterDAO getInstance() {
		return (SideLetterDAO)getInstance("SideLetterDAO");
	}
	
	public List<SideLetter> findBySideLetterCode(Object sideLetterCode) {
		return findByProperty(SIDE_LETTER_CODE, sideLetterCode);
	}

	public List<SideLetter> findBySideLetterDesc(Object sideLetterDesc) {
		return findByProperty(SIDE_LETTER_DESC, sideLetterDesc);
	}

	public static SideLetterDAO getFromApplicationContext(ApplicationContext ctx) {
		return (SideLetterDAO)ctx.getBean("SideLetterDAO");
	}
}
