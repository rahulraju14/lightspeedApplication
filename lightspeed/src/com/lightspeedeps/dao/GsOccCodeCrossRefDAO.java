/**
 * File: GsOccCodeCrossRefDAO.java
 */
package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.GsOccCodeCrossRef;

/**
 * A data access object (DAO) providing persistence and search support for
 * GsOccCodeCrossRef entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.GsOccCodeCrossRef
 */
public class GsOccCodeCrossRefDAO  extends BaseTypeDAO<GsOccCodeCrossRef> {

	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(GsOccCodeCrossRefDAO.class);

	private static final int LS_CLIENT = 2;

	public static final String CLIENT = "clientId";

	public static GsOccCodeCrossRefDAO getInstance() {
		return (GsOccCodeCrossRefDAO)getInstance("GsOccCodeCrossRefDAO");
	}

	/**
	 * NOTE: This does NOT return all of the entries in the associated table!
	 * The table is used across multiple applications; this method returns all
	 * the values that are relevant to our application.
	 *
	 * @return A non-null List of all the OccCode entries for Lightspeed.
	 */
	@Override
	public List<GsOccCodeCrossRef> findAll() {
		return findByProperty(CLIENT, LS_CLIENT);
	}

}
