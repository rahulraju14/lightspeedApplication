package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyHotCosts;

/**
 * A data access object (DAO) providing persistence and search support for
 * WeeklyHotCosts entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.WeeklyHotCosts
 * @author MyEclipse Persistence Tools
 */
public class WeeklyHotCostsDAO extends BaseTypeDAO<WeeklyHotCosts> {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WeeklyHotCostsDAO.class);
    // property constants
    public static final String USER_ACCOUNT = "userAccount";
    public static final String PROD_ID = "prodId";
    public static final String GRAND_TOTAL = "grandTotal";

    public static WeeklyHotCostsDAO getInstance() {
    	return (WeeklyHotCostsDAO) getInstance("WeeklyHotCostsDAO");
    }

    public List<WeeklyHotCosts> findByUserAccount(Object userAccount) {
    	return findByProperty(USER_ACCOUNT, userAccount);
    }

    public List<WeeklyHotCosts> findByProdId(Object prodId) {
    	return findByProperty(PROD_ID, prodId);
    }

    public List<WeeklyHotCosts> findByGrandTotal(Object grandTotal) {
    	return findByProperty(GRAND_TOTAL, grandTotal);
    }

    @SuppressWarnings("unchecked")
    public List<WeeklyHotCosts> findByProdWeekEndDate(Production production, Date weekEndDate, String orderBy) {
		List<Object> objects = new ArrayList<Object>(0);
		String query = "from WeeklyHotCosts where Prod_id = ?";
	
		objects.add(production.getProdId());
		if (weekEndDate != null) {
		    query += " and end_date = ?";
		    objects.add(weekEndDate);
		}
		if (orderBy != null) {
		    query += " order by " + orderBy;
		}
		return find(query, objects.toArray());
    }

    public WeeklyHotCosts findByUserAcctProdWeekEndDate(String userAcct, Production production, Date weekEndDate) {
		List<Object> objects = new ArrayList<Object>(0);
		String query = "from WeeklyHotCosts where Prod_id = ? and User_Account = ?";
	
		objects.add(production.getProdId());
		objects.add(userAcct);
	
		if (weekEndDate != null) {
		    query += " and end_date = ?";
		    objects.add(weekEndDate);
		}
	
		return findOne(query, objects.toArray());
    }

    @SuppressWarnings("unchecked")
    public List<WeeklyHotCosts> findByProdProjWeekEndDate(Production production, Project project, Date weekEndDate,
	    String orderBy) {
		List<Object> objects = new ArrayList<Object>(0);
		String query = "from WeeklyHotCosts where Prod_id = ? ";
	
		objects.add(production.getProdId());
	
		if (weekEndDate != null) {
		    query += " and end_date = ? ";
		    objects.add(weekEndDate);
		}
	
		if (project != null) {
		    query += " and project_id = ? ";
		    objects.add(project.getId());
		}
	
		if (orderBy != null) {
		    query += " order by " + orderBy;
		}
	
		return find(query, objects.toArray());
    }

    // public WeeklyHotCosts findByUserAcctProdWeekEndDateStartForm(String
    // userAcct, Production production, Date weekEndDate, StartForm startForm) {
    // List<Object> objects = new ArrayList<Object>(0);
    // String query = "from WeeklyHotCosts where prod_id = ? and userAccount = ?
    // and endDate = ? and startForm = ?";
    //
    // objects.add(production.getProdId());
    // objects.add(userAcct);
    // objects.add(weekEndDate);
    // objects.add(startForm);
    //// if(weekEndDate != null) {
    //// query += " and end_date = ?";
    //// objects.add(weekEndDate);
    //// }
    //
    // return findOne(query, objects.toArray());
    // }

    @SuppressWarnings("unchecked")
	public List<WeeklyHotCosts> findByUserAcctProdWeekEndDateEmployment(String userAcct, Production production,
	    Project project, Date weekEndDate, Integer employmentId, String orderBy) {
		List<Object> objects = new ArrayList<Object>(0);
		String query = "from WeeklyHotCosts where prod_id = ? and userAccount = ? and employment_Id = ?";
	
		objects.add(production.getProdId());
		objects.add(userAcct);
		objects.add(employmentId);
		
		if(project != null) {
			query += " and project_id = ? ";
			objects.add(project.getId());
		}
		
		if(weekEndDate != null) {
			 query += " and end_date = ?";
			 objects.add(weekEndDate);
		}
		
		if(orderBy != null) {
			query += orderBy;
		}
		
		return find(query, objects.toArray());
    }
    
    /**
     * Retrieve all of the WeeklyHotCosts entries for this employee in the associated production and
     * project.
     * 
     * @param userAcct
     * @param production
     * @param project
     * @param employeeId
     * @param orderBy
     * @return
     */
    public List<WeeklyHotCosts> findByProdProjEmployment(String userAcct, Production production, Project project, Integer employeeId, String orderBy) {
    	return findByUserAcctProdWeekEndDateEmployment(userAcct, production, project, null, employeeId, orderBy);
    }

    public static WeeklyHotCostsDAO getFromApplicationContext(ApplicationContext ctx) {
    	return (WeeklyHotCostsDAO) ctx.getBean("WeeklyHotCostsDAO");
    }
}