package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.TaxWageAllocationRow;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 	* A data access object (DAO) providing persistence and search support for TaxWageAllocationRow entities.
 			* Transaction control of the save(), update() and delete() operations 
		can directly support Spring container-managed transactions or they can be augmented	to handle user-managed Spring transactions. 
		Each of these methods provides additional information for how to configure it for the desired type of transaction control. 	
	 * @see com.lightspeedeps.model.TaxWageAllocationRow
  * @author MyEclipse Persistence Tools 
 */
public class TaxWageAllocationRowDAO extends BaseTypeDAO<TaxWageAllocationRow>  {
	 @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TaxWageAllocationRowDAO.class);
	 
	//property constants
	public static final String VERSION = "version";
	public static final String ROW_NUMBER = "rowNumber";
	public static final String TEAM_TAX_AREA_CODE = "teamTaxAreaCode";
	public static final String SPECIAL_INSTRUCTIONS = "specialInstructions";

	public static TaxWageAllocationRowDAO getInstance() {
		return (TaxWageAllocationRowDAO)ServiceFinder.findBean("TaxWageAllocationRowDAO");
	}
	
	public List<TaxWageAllocationRow> findByVersion(Object version) {
		return findByProperty(VERSION, version
		);
	}
	
	public List<TaxWageAllocationRow> findByRowNumber(Object rowNumber) {
		return findByProperty(ROW_NUMBER, rowNumber
		);
	}
	
	public List<TaxWageAllocationRow> findByTeamTaxAreaCode(Object teamTaxAreaCode) {
		return findByProperty(TEAM_TAX_AREA_CODE, teamTaxAreaCode
		);
	}
	
	public List<TaxWageAllocationRow> findBySpecialInstructions(Object specialInstructions) {
		return findByProperty(SPECIAL_INSTRUCTIONS, specialInstructions
		);
	}

	public static TaxWageAllocationRowDAO getFromApplicationContext(ApplicationContext ctx) {
    	return (TaxWageAllocationRowDAO) ctx.getBean("TaxWageAllocationRowDAO");
	}
}