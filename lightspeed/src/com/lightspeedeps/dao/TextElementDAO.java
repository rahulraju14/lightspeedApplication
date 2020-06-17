package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.TextElement;

/**
 * A data access object (DAO) providing persistence and search support for
 * TextElement entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.TextElement
 */

public class TextElementDAO extends BaseTypeDAO<TextElement> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TextElementDAO.class);
	// property constants
//	private static final String TYPE = "type";
//	private static final String SEQUENCE = "sequence";
//	private static final String TEXT = "text";

	public static TextElementDAO getInstance() {
		return (TextElementDAO)getInstance("TextElementDAO");
	}

//	@SuppressWarnings("unchecked")
//	public List<TextElement> findSceneRange(Script script, Scene firstScene, Scene lastScene) {
//		Object[] values = { script, firstScene.getSequence(), lastScene.getSequence() };
//		String queryString =
//				"Select te " +
//				" from TextElement te, Scene s where te.scene = s and " +
//				" s.script = ? and " +
//				" s.sequence between ? and ? " +
//				" order by s.sequence, te.sequence";
//		return find(queryString, values);
//	}

	public boolean hasText(Script script) {
		Object[] values = { script };
		String queryString =
				" from TextElement te, Scene s where te.scene = s and " +
				" s.script = ? ";
		return exists(queryString, values);
	}

//	public static TextElementDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (TextElementDAO) ctx.getBean("TextElementDAO");
//	}

}
