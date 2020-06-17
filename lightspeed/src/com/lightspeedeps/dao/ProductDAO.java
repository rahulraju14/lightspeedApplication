package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Product;
import com.lightspeedeps.type.ProductionType;

/**
 * A data access object (DAO) providing persistence and search support for
 * Product entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Product
 */

public class ProductDAO extends BaseTypeDAO<Product> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(ProductDAO.class);

	// property constants
	public static final String SKU = "sku";
//	private static final String BUTTON = "button";
//	private static final String TITLE = "title";
//	private static final String DESCRIPTION = "description";
	private static final String TYPE = "type";
//	private static final String MAX_PROJECTS = "maxProjects";
//	private static final String MAX_USERS = "maxUsers";
//	private static final String DURATION = "duration";
//	private static final String SMS_ENABLED = "smsEnabled";
//	private static final String PRICE = "price";


	public static ProductDAO getInstance() {
		return (ProductDAO)getInstance("ProductDAO");
	}

	/**
	 * Retrieve a list of Product's of the given type which should be displayed
	 * to all 'normal' users. SKU's containing the string "-99" are NOT
	 * included; these are designed for testing and are only displayed to LS
	 * Admin users.
	 *
	 * @param type The production type to select on.
	 * @return A non-null, but possibly empty, List of Product objects of the
	 *         specified type, and without '-99' in their SKU.
	 */
	@SuppressWarnings("unchecked")
	public List<Product> findPublic(ProductionType type) {
		return find("from Product where " +
				TYPE + " = ? and " +
				SKU + " not like '%-99%'", type);
	}

	/**
	 * Retrieve a list of Product's of the given type which should be displayed
	 * to all 'Premium' users. SKU's containing the string "-88" are NOT
	 * included; these are ones that don't allow immediate creation, but have
	 * the user call the sales staff.
	 *
	 * @param type The production type to select on.
	 * @return A non-null, but possibly empty, List of Product objects of the
	 *         specified type, and without '-88' in their SKU.
	 */
	@SuppressWarnings("unchecked")
	public List<Product> findPremium(ProductionType type) {
		return find("from Product where " +
				TYPE + " = ? and " +
				SKU + " not like '%-88%'", type);
	}

//	public static ProductDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ProductDAO)ctx.getBean("ProductDAO");
//	}

}