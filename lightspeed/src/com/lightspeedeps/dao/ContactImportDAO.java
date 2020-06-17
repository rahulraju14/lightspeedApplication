/** File: ContactImportDAO.java */
package com.lightspeedeps.dao;

import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;

import com.lightspeedeps.model.ContactImport;
import com.lightspeedeps.model.Production;

/**
 * A data access object (DAO) providing persistence and search support for
 * ContactImport entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ContactImport
 */
public class ContactImportDAO extends BaseTypeDAO<ContactImport> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ContactImportDAO.class);

	// property constants

	public static final String PRODUCTION = "production";

	public static ContactImportDAO getInstance() {
		return (ContactImportDAO)getInstance("ContactImportDAO");
	}

	/**
	 * @param production The Production whose data is to be deleted from the table.
	 */
	@Transactional
	public void deleteByProduction(Production production) {
		Query q = getHibernateSession().createQuery("delete ContactImport where production = :prod");
		q.setParameter("prod", production);
		q.executeUpdate();
	}

}
