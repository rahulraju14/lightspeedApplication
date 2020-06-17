package com.lightspeedeps.dao;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.PointOfInterest;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.type.PointOfInterestType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * PointOfInterest entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.PointOfInterest
 */

public class PointOfInterestDAO extends BaseTypeDAO<PointOfInterest> {
	private static final Log log = LogFactory.getLog(PointOfInterestDAO.class);
	// property constants
	private static final String PRODUCTION = "production";
	private static final String TYPE = "type";
	private static final String NAME = "name";
//	private static final String DESCRIPTION = "description";
//	private static final String PHONE = "phone";

	public static PointOfInterestDAO getInstance() {
		return (PointOfInterestDAO)getInstance("PointOfInterestDAO");
	}

	public List<PointOfInterest> findByProduction() {
		return findByProperty(PRODUCTION, SessionUtils.getProduction());
	}

	public List<PointOfInterest> findByProduction(Production prod) {
		return findByProperty(PRODUCTION, prod);
	}

	@SuppressWarnings("unchecked")
	public List<PointOfInterest> findByPropertyProduction(String propertyName, Object value) {
		String query = "from PointOfInterest where " +
				PRODUCTION + " = ? and " +
				propertyName + "= ?";
		Object values[] = { SessionUtils.getProduction(), value };
		return find(query, values);
	}

	public List<PointOfInterest> findByProductionAndType(PointOfInterestType type) {
		return findByPropertyProduction(TYPE, type);
	}

	@SuppressWarnings("unchecked")
	public List<PointOfInterest> findAllProductionOrdered() {
		return find("from PointOfInterest where " +
				PRODUCTION + " = ? order by " + NAME, SessionUtils.getProduction());
	}

	/**
	 * Add or merge the supplied PointOfInterest, after first updating the Image objects so
	 * they are associated with the given element.
	 *
	 * @param point The PointOfInterest to be saved or updated.
	 * @param images The Collection of images to associate with the element.
	 * @return The updated PointOfInterest
	 */
	@Transactional
	public PointOfInterest update(PointOfInterest point, Collection<Image> images) {
		if (images != null) {
			for (Image image : images) {
				image.setPointOfInterest(point);
			}
		}
		if (point.getId() == null) {
			save(point);
		}
		else {
			point = merge(point);
		}
		return point;
	}

	/**
	 * Delete a pointOfInterest object from the database, and remove it from all the
	 * RealWorldElement "nearby" associations.
	 * @param poi The PointOfInterest object to delete.
	 */
	@Transactional
	public void remove(PointOfInterest poi) {
		log.debug("deleting PointOfInterest and its attachments");
		try {
			for ( RealWorldElement rw : poi.getRealWorldElements()) {
				rw.getPointOfInterests().remove(poi);
			}
			delete(poi);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

//	public static PointOfInterestDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PointOfInterestDAO) ctx.getBean("PointOfInterestDAO");
//	}

}