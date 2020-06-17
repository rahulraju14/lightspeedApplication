package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.lightspeedeps.model.Agreement;

/**
 * A data access object (DAO) providing persistence and search support for
 * Agreement entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Agreement
 * @author MyEclipse Persistence Tools
 */
public class AgreementDAO extends BaseTypeDAO<Agreement> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AgreementDAO.class);

	//property constants
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String PRODUCTION_TYPE = "productionType";
	public static final String GROUP_NAME = "groupName";
	public static final String SUB_GROUP = "subGroup";
	public static final String LIST_ORDER = "listOrder";
	public static final String OCC_UNION = "occUnion";

	public static AgreementDAO getInstance() {
		return (AgreementDAO)getInstance("AgreementDAO");
	}

//	public static AgreementDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (AgreementDAO)ctx.getBean("AgreementDAO");
//	}

}
