package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.PostalLocation;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * PostalLocation entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.PostalLocation
 */

public class PostalLocationDAO extends BaseTypeDAO<PostalLocation> {
	private static final Log log = LogFactory.getLog(PostalLocationDAO.class);
	// property constants
//	private static final String COUNTRY = "country";
	private static final String POSTAL_CODE = "postalCode";
//	private static final String LATITUDE = "latitude";
//	private static final String LONGITUDE = "longitude";

	public static PostalLocationDAO getInstance() {
		return (PostalLocationDAO)getInstance("PostalLocationDAO");
	}

	public List<PostalLocation> findByPostalCode(Object postalCode) {
		return findByProperty(POSTAL_CODE, postalCode);
	}

	@SuppressWarnings("unchecked")
	public List<PostalLocation> findByCountryAndPostalCode(String country, String zip) {
		try {
			Object[] values = {country, zip};
			String queryString = "from PostalLocation where country = ? and postalCode = ?";
			List<PostalLocation> list = find(queryString, values);
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Save all the objects in the List to the database.
	 *
	 * @param buffer A List of non-null PostalLocation references. If buffer is
	 *            null, the operation is a no-op.
	 */
	@Transactional
	public void save(List<PostalLocation> buffer) {
		log.debug("");
		if (buffer != null) {
			try {
				HibernateTemplate t = getHibernateTemplate();
				for (PostalLocation pl : buffer) {
					t.save(pl);
				}
				log.debug(buffer.size() + " objects saved");
			}
			catch (RuntimeException re) {
				EventUtils.logError(re);
				throw re;
			}
		}
	}

//	public static PostalLocationDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PostalLocationDAO) ctx.getBean("PostalLocationDAO");
//	}

}