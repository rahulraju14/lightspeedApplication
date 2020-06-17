package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Image;

/**
 * A data access object (DAO) providing persistence and search support for
 * Image entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Image
 */
public class ImageDAO extends BaseTypeDAO<Image> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ImageDAO.class);
	// property constants
//	private static final String TITLE = "title";
//	private static final String DESCRIPTION = "description";
//	private static final String ARTIST = "artist";
//	private static final String TYPE = "type";
//	private static final String CONTENT = "content";

	public static ImageDAO getInstance() {
		return (ImageDAO)getInstance("ImageDAO");
	}

//	public static ImageDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ImageDAO) ctx.getBean("ImageDAO");
//	}

}