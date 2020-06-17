package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.CheckoutOrder;

/**
 * A data access object (DAO) providing persistence and search support for
 * CheckoutOrder entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.CheckoutOrder
 */

public class CheckoutOrderDAO extends BaseTypeDAO<CheckoutOrder> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CheckoutOrderDAO.class);

	// property constants
	public static final String INVOICE_NUMBER = "invoiceNumber";
	public static final String ORDER_SUMMARY = "orderSummary";

	public static CheckoutOrderDAO getInstance() {
		return (CheckoutOrderDAO)getInstance("CheckoutOrderDAO");
	}

	public CheckoutOrder findByInvoiceNumber(String orderNumber) {
		CheckoutOrder order = null;
		List<CheckoutOrder> list = findByProperty(INVOICE_NUMBER, orderNumber);
		if (list.size() > 0) {
			order = list.get(0);
		}
		return order;
	}

	/**
	 * Determine if any order exists in the database beginning with the given
	 * prefix value.
	 *
	 * @param orderNum
	 * @return True iff a CheckoutOrder exists whose orderNumber begins with the
	 *         parameter given.
	 */
	public boolean existsInvoiceNumberPrefix(String orderNum) {
		String param = orderNum + "%";
		String queryString = "select count(id) from CheckoutOrder " +
			"  where " + INVOICE_NUMBER + " like ?";
		return findCount(queryString, param) > 0;
	}

	public static CheckoutOrderDAO getFromApplicationContext(ApplicationContext ctx) {
		return (CheckoutOrderDAO)ctx.getBean("CheckoutOrderDAO");
	}

}