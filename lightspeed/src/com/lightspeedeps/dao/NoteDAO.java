package com.lightspeedeps.dao;

import com.lightspeedeps.model.Note;

/**
 * A data access object (DAO) providing persistence and search support for Note
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Note
 */

public class NoteDAO extends BaseTypeDAO<Note> {

	// property constants
//	private static final String TEXT = "text";

	public static NoteDAO getInstance() {
		return (NoteDAO)getInstance("NoteDAO");
	}

//	public boolean existsUser(User user) {
//		String query = "select count(id) from Note where user = ? ";
//		return findCount(query, user) > 0;
//	}

//	public static NoteDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (NoteDAO) ctx.getBean("NoteDAO");
//	}

}