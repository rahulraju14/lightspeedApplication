package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayrollPreference entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayrollPreference
 */
public class PayrollPreferenceDAO extends BaseTypeDAO<PayrollPreference> {

	/** local logger instance */
	private static final Log LOG = LogFactory.getLog(PayrollPreferenceDAO.class);

	public static PayrollPreferenceDAO getInstance() {
		return (PayrollPreferenceDAO)getInstance("PayrollPreferenceDAO");
	}

	/**
	 * @param production
	 * @return
	 */
	public int findCountWeekFirstDay(Production production) {
		String query = "select count(distinct pp.weekFirstDay) from PayrollPreference pp, Project p " +
				" where p.production = ? and p.payrollPref = pp";
		@SuppressWarnings("unchecked")
		List<Long> list = find(query, production);
		int count = list.get(0).intValue();
		LOG.debug(count);
		return count;
	}

//	public static ImageDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ImageDAO) ctx.getBean("ImageDAO");
//	}

}