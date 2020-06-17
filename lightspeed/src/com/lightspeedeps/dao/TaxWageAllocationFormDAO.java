package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * TaxWageAllocationForm entities. Transaction control of the save(), update()
 * and delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.TaxWageAllocationForm
 * @author MyEclipse Persistence Tools
 */
public class TaxWageAllocationFormDAO extends BaseTypeDAO<TaxWageAllocationForm> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TaxWageAllocationFormDAO.class);
	//property constants
	public static final String EMPLOYEE_NAME = "employeeName";
	public static final String RESIDENT_CITY = "residentCity";
	public static final String RESIDENT_STATE = "residentState";
	public static final String SOCIAL_SECURITY_NUM = "socialSecurityNum";
	public static final String REALLOCATION_WAGES = "reallocationWages";
	public static final String FREQUENCY = "frequency";
	public static final String CASH_ADVANCE_DEDUCTION =	"cashAdvanceDeduction";
	public static final String RETIREMENT_DEDUCTION = "retirementDeduction";
	public static final String FRINGE = "fringe";
	public static final String SHAREHOLER_INSURANCE = "shareholerInsurance";
	public static final String NET_ZERO_CHECK = "netZeroCheck";
	public static final String SPECIAL_INSTRUCTIONS = "specialInstructions";
	public static final String PRODUCTION = "production";
	public static final String CONTACT = "contact";
	
	public static TaxWageAllocationFormDAO getInstance() {
		return (TaxWageAllocationFormDAO)ServiceFinder.findBean("TaxWageAllocationFormDAO");
	}
	
	public List<TaxWageAllocationForm> findByEmployeeName(Object employeeName) {
		return findByProperty(EMPLOYEE_NAME, employeeName);
	}

	public List<TaxWageAllocationForm> findByResidentCity(Object residentCity) {
		return findByProperty(RESIDENT_CITY, residentCity);
	}

	public List<TaxWageAllocationForm> findByResidentState(Object residentState) {
		return findByProperty(RESIDENT_STATE, residentState);
	}

	public List<TaxWageAllocationForm> findBySocialSecurityNum(Object socialSecurityNum) {
		return findByProperty(SOCIAL_SECURITY_NUM, socialSecurityNum);
	}

	public List<TaxWageAllocationForm> findByReallocationWages(Object reallocationWages) {
		return findByProperty(REALLOCATION_WAGES, reallocationWages);
	}

	public List<TaxWageAllocationForm> findByFrequency(Object frequency) {
		return findByProperty(FREQUENCY, frequency);
	}

	public List<TaxWageAllocationForm> findByCashAdvanceDeduction(Object cashAdvanceDeduction) {
		return findByProperty(CASH_ADVANCE_DEDUCTION, cashAdvanceDeduction);
	}

	public List<TaxWageAllocationForm> findByRetirementDeduction(Object retirementDeduction	) {
		return findByProperty(RETIREMENT_DEDUCTION, retirementDeduction	);
	}

	public List<TaxWageAllocationForm> findByFringe(Object fringe) {
		return findByProperty(FRINGE, fringe);
	}

	public List<TaxWageAllocationForm> findByShareholerInsurance(Object shareholerInsurance) {
		return findByProperty(SHAREHOLER_INSURANCE, shareholerInsurance);
	}

	public List<TaxWageAllocationForm> findByNetZeroCheck(Object netZeroCheck) {
		return findByProperty(NET_ZERO_CHECK, netZeroCheck);
	}

	public List<TaxWageAllocationForm> findBySpecialInstructions(Object specialInstructions) {
		return findByProperty(SPECIAL_INSTRUCTIONS, specialInstructions);
	}

	public List<TaxWageAllocationForm> findByProduction(Object production) {
		return findByProperty(PRODUCTION, production);
	}

	@SuppressWarnings("unchecked")
	public List<TaxWageAllocationForm> findByContact(Object contact, String orderBy) {
		String query = "from TaxWageAllocationForm where contact = ?";
		
		if(orderBy != null) {
			query += " order by " + orderBy;
		}
		return find(query, contact);
	}
	
	/**
	 * Find all of the revision date
	 * @param query
	 * @param parms
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<TaxWageAllocationForm> findRevisionsByContactProd(Production production, Contact contact, String orderBy) {
		String query;
		List<Object> parms = new ArrayList<>();
		
		query = " from TaxWageAllocationForm where production = ? ";
		parms.add(production);
		
		if(contact != null) {
			query += " and contact = ? ";
			parms.add(contact);
		}
		
		if(orderBy != null) {
			query += orderBy;
		}
		
		return find(query, parms.toArray());
	}
	
	
	@SuppressWarnings("unchecked")
	public List<Contact> findContactsWithFormsByProduction(Production production, String orderBy) {
		String query = "select distinct ct from TaxWageAllocationForm twaf, Contact ct where twaf.production = ? and ct = twaf.contact";
		
		return find(query, production);
	}
	
	public static TaxWageAllocationFormDAO getFromApplicationContext(ApplicationContext ctx) {
		return (TaxWageAllocationFormDAO)ctx.getBean("TaxWageAllocationFormDAO");
	}
}
