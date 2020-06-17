package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.TaxWageAllocationRowTemplate;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * TaxWageAllocationForm entities. Transaction control of the save(), update()
 * and delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.TaxWageAllocationRowTemplate
 * @author MyEclipse Persistence Tools
 */
public class TaxWageAllocationRowTemplateDAO extends BaseTypeDAO<TaxWageAllocationRowTemplate> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TaxWageAllocationRowTemplateDAO.class);
	
	//property constants
	public static final String VERSION = "version";
	public static final String STATE = "state";
	public static final String TEAM_TAX_AREA_CODE = "teamTaxAreaCode";
	public static final String STATE_CODE = "stateCode";
	public static final String CITY = "city";
	public static final String CALCULATE_TAX = "calculateTax";
	public static final String CALCULATE_TAX_EDITABLE = "calculateTaxEditable";
	public static final String SPECIAL_INSTRUCTIONS = "specialInstructions";

	public static TaxWageAllocationRowTemplateDAO getInstance() {
		return (TaxWageAllocationRowTemplateDAO)ServiceFinder.findBean("TaxWageAllocationRowTemplateDAO");
	}
	
	public List<TaxWageAllocationRowTemplate> findByVersion(Object version) {
		return findByProperty(VERSION, version);
	}

	public List<TaxWageAllocationRowTemplate> findByState(Object state) {
		return findByProperty(STATE, state);
	}

	public List<TaxWageAllocationRowTemplate> findByTeamTaxAreaCode(Object teamTaxAreaCode) {
		return findByProperty(TEAM_TAX_AREA_CODE, teamTaxAreaCode);
	}

	public List<TaxWageAllocationRowTemplate> findByStateCode(Object stateCode) {
		return findByProperty(STATE_CODE, stateCode);
	}

	public List<TaxWageAllocationRowTemplate> findByCity(Object city) {
		return findByProperty(CITY, city);
	}

	public List<TaxWageAllocationRowTemplate> findByCalculateTax(Object calculateTax) {
		return findByProperty(CALCULATE_TAX, calculateTax);
	}

	public List<TaxWageAllocationRowTemplate> findByCalculateTaxEditable(Object calculateTaxEditable) {
		return findByProperty(CALCULATE_TAX_EDITABLE, calculateTaxEditable);
	}

	public List<TaxWageAllocationRowTemplate> findBySpecialInstructions(Object specialInstructions) {
		return findByProperty(SPECIAL_INSTRUCTIONS, specialInstructions);
	}

	public static TaxWageAllocationRowTemplateDAO getFromApplicationContext(ApplicationContext ctx) {
		return (TaxWageAllocationRowTemplateDAO)ctx.getBean("TaxWageAllocationFormDAO");
	}
}
