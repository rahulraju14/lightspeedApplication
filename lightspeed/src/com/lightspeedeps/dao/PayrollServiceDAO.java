package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayrollService;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayrollService entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayrollService
 */
public class PayrollServiceDAO  extends BaseTypeDAO<PayrollService> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayrollServiceDAO.class);

	private static List<PayrollService> payrollServices = null;

	//property constants
//	private static final String NAME = "name";
//	private static final String CONTACT1_NAME = "contact1Name";
//	private static final String CONTACT1_PHONE = "contact1Phone";
//	private static final String CONTACT1_EMAIL = "contact1Email";
//	private static final String CONTACT2_NAME = "contact2Name";
//	private static final String CONTACT2_PHONE = "contact2Phone";
//	private static final String CONTACT2_EMAIL = "contact2Email";
//	private static final String CONTACT3_NAME = "contact3Name";
//	private static final String CONTACT3_PHONE = "contact3Phone";
//	private static final String CONTACT3_EMAIL = "contact3Email";
//	private static final String SEND_ONLY_CHANGES = "sendOnlyChanges";
//	private static final String SEND_BATCH_METHOD = "sendBatchMethod";
//	private static final String LOGIN_NAME = "loginName";
//	private static final String PASSWORD = "password";
//	private static final String AUTH_URL = "authUrl";
//	private static final String BATCH_URL = "batchUrl";
//	private static final String STATUS_URL = "statusUrl";

	public static PayrollServiceDAO getInstance() {
		return (PayrollServiceDAO)getInstance("PayrollServiceDAO");
	}

	/**
	 * Load all the defined payroll services once.
	 */
	private static List<PayrollService> createPayrollServices() {
		return PayrollServiceDAO.getInstance().findAll();
	}

	/** See {@link #payrollServices}. */
	public static List<PayrollService> getPayrollServices() {
		if (payrollServices == null) {
			payrollServices = createPayrollServices();
		}
		return payrollServices;
	}

	/** See {@link #payrollServices}. */
	public static void resetPayrollServices() {
		payrollServices = createPayrollServices();
	}

//	public static PayrollServiceDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayrollServiceDAO)ctx.getBean("PayrollServiceDAO");
//	}

}
