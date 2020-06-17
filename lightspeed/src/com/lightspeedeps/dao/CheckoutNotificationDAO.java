package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.CheckoutNotification;

/**
 * A data access object (DAO) providing persistence and search support for
 * CheckoutNotification entities. Transaction control of the save(), update()
 * and delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.CheckoutNotification
 */

public class CheckoutNotificationDAO extends BaseTypeDAO<CheckoutNotification> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CheckoutNotificationDAO.class);

	// property constants
	public static final String SERIAL_NUMBER = "serialNumber";

	public static CheckoutNotificationDAO getInstance() {
		return (CheckoutNotificationDAO)getInstance("CheckoutNotificationDAO");
	}

	public boolean exists(String serialNumber) {
		return existsProperty(SERIAL_NUMBER, serialNumber);
	}

	public static CheckoutNotificationDAO getFromApplicationContext(ApplicationContext ctx) {
		return (CheckoutNotificationDAO)ctx.getBean("CheckoutNotificationDAO");
	}

}