package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayJobDaily;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayJobDaily entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayJobDaily
 */

public class PayJobDailyDAO extends BaseTypeDAO<PayJobDaily> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayJobDailyDAO.class);

	// property constants
//	private static final String DAY_NUM = "dayNum";
//	private static final String HOURS10 = "hours10";
//	private static final String HOURS15 = "hours15";
//	private static final String HOURS20 = "hours20";
//	private static final String HOURS30 = "hours30";
//	private static final String HOURS_CUST = "hoursCust";
//	private static final String MPV1 = "mpv1";
//	private static final String MPV2 = "mpv2";


	public static PayJobDailyDAO getInstance() {
		return (PayJobDailyDAO)getInstance("PayJobDailyDAO");
	}

//	public static PayJobDailyDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayJobDailyDAO)ctx.getBean("PayJobDailyDAO");
//	}

}
