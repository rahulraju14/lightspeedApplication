package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Message;

/**
 * A data access object (DAO) providing persistence and search support for
 * Message entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Message
 */
public class MessageDAO extends BaseTypeDAO<Message> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MessageDAO.class);
	// property constants
//	private static final String METHOD = "method";
//	private static final String SENDER = "sender";
//	private static final String SUBJECT = "subject";
//	private static final String BODY = "body";

	public static MessageDAO getInstance() {
		return (MessageDAO)getInstance("MessageDAO");
	}

//	public static MessageDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (MessageDAO) ctx.getBean("MessageDAO");
//	}

}