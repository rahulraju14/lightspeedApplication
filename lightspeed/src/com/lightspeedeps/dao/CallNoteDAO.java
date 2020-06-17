package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.CallNote;

/**
 * A data access object (DAO) providing persistence and search support for
 * CallNote entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.CallNote
 */

public class CallNoteDAO extends BaseTypeDAO<CallNote> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CallNoteDAO.class);
	// property constants
//	private static final String SECTION = "section";
//	private static final String HEADING = "heading";
//	private static final String LINE_NUMBER = "lineNumber";
//	private static final String BODY = "body";

	public static CallNoteDAO getInstance() {
		return (CallNoteDAO)getInstance("CallNoteDAO");
	}

//	public static CallNoteDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (CallNoteDAO) ctx.getBean("CallNoteDAO");
//	}

}