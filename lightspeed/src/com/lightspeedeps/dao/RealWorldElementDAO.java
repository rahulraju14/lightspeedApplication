package com.lightspeedeps.dao;

import java.util.*;

import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * RealWorldElement entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.RealWorldElement
 */

public class RealWorldElementDAO extends BaseTypeDAO<RealWorldElement> {
	private static final Log log = LogFactory.getLog(RealWorldElementDAO.class);
	// property constants
	public static final String PRODUCTION = "production";
	private static final String TYPE = "type";
//	private static final String NAME = "name";
//	private static final String RATE = "rate";
//	private static final String RATE_PER = "ratePer";
//	private static final String DESCRIPTION = "description";
//	private static final String SPECIAL_INSTRUCTIONS = "specialInstructions";
//	private static final String PHONE = "phone";
//	private static final String DIRECTIONS = "directions";
//	private static final String PERMITS_REQUIRED = "permitsRequired";
//	private static final String PERMITS_OBTAINED = "permitsObtained";
//	private static final String PERMIT_DATA = "permitData";

	public static RealWorldElementDAO getInstance() {
		return (RealWorldElementDAO)getInstance("RealWorldElementDAO");
	}

	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByPropertyProduction(String propertyName, Object value) {
		String query = "from RealWorldElement where " +
				"production = ? and " +
				propertyName + "= ?";
		Object values[] = { SessionUtils.getProduction(), value };
		return find(query, values);
	}

	public List<RealWorldElement> findByProduction() {
		return findByProperty(PRODUCTION, SessionUtils.getProduction());
	}

//	@SuppressWarnings("unchecked")
//	public List<RealWorldElement> findByStatusAndType(String status, ScriptElementType elType, int projectId) {
//		try {
//			Object[] values = { status, elType, projectId };
//			String queryString = "select rwe from RealWorldElement rwe,RealLink  rl,ScriptElement se "
//					+ "where rl.status = ? and rwe.type = ? and rl.realWorldElement.id = rwe.id "
//					+ " and rl.scriptElement.id = se.id and se.project.id =? ";
//			return find(queryString, values);
//		}
//		catch (RuntimeException e) {
//			EventUtils.logError(e);
//			throw e;
//		}
//	}

	public List<RealWorldElement> findByProductionAndType(ScriptElementType type) {
		return findByPropertyProduction(TYPE, type);
	}

//	public List<RealWorldElement> findByName(Object name) {
//		return findByProperty(NAME, name);
//	}

//	public static RealWorldElementDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (RealWorldElementDAO) ctx.getBean("RealWorldElementDAO");
//	}

	/**
	 * Find all elements of a given type in the current Production, ordered by
	 * their name.
	 *
	 * @param type The ScriptElementType desired.
	 * @return A non-null, but possibly empty, List of RealWorldElement's,
	 *         sorted in ascending name order.
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByProductionAndTypeOrdered(ScriptElementType type) {
		log.debug("finding RWE's with type: " + type);
		String queryString = "from RealWorldElement where " +
				PRODUCTION + " = ? and " +
				TYPE + " = ? order by name";
		Object[] values = {SessionUtils.getProduction(), type};
		return find(queryString, values);
	}

	/**
	 * Finds the RealWorldElement with the specified name and type in the
	 * current Production. . Since this combination of fields is required to be
	 * unique, there should be at most one result.
	 *
	 * @param name The name of the element to be found; MySQL uses a case
	 *            insensitive comparison.
	 * @param type The ScriptElementType of the element to be found.
	 * @return The matching RealWorldElement, or null if a match is not found.
	 */
	@SuppressWarnings("unchecked")
	public RealWorldElement findByNameType(String name, ScriptElementType type) {
		try {
			Object[] values = { SessionUtils.getProduction(), name, type,  };
			log.debug("name=" + name + ", Type=" + type );
			String queryString = " from RealWorldElement where " +
					PRODUCTION + " = ? and name = ? and type = ? ";
			List<RealWorldElement> list = find(queryString, values);
			if (list.size() > 0) {
				return list.get(0);
			}
			return null;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Finds all RealWorldElements, except those of a specific type.
	 *
	 * @param type
	 * @return A non-null List
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByNotType(ScriptElementType type) {
		log.debug("Type=" + type );
		Object[] values = { SessionUtils.getProduction(), type };
		String queryString = " from RealWorldElement rw where " +
				PRODUCTION + " = ? and type != ? ";
		return find(queryString, values);
	}

	/**
	 * Finds all RealWorldElements of a specific type which are linked (via RealLink's) to any
	 * ScriptElement associated with the specified project.
	 *
	 * @param type
	 * @param project
	 * @return A non-null List
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByTypeAndProject(ScriptElementType type, Project project) {
		try {
			Object[] values = { type, project };
			log.debug("Type=" + type + ", Project=" + project.getId());
			String queryString = "select distinct rwe from RealWorldElement rwe, RealLink rl, ScriptElement se "
					+ "where rwe.id = rl.realWorldElement.id and rl.scriptElement.id = se.id and se.type=? and se.project =?";
			return find(queryString, values);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Finds all RealWorldElements, EXCEPT those of a specific type, which are linked (via RealLink's) to any
	 * ScriptElement associated with the specified project.
	 *
	 * @param type
	 * @param project
	 * @return A non-null List
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByNotTypeAndProject(ScriptElementType type, Project project) {
		try {
			Object[] values = { type, project };
			log.debug("Type=" + type + ", Project=" + project.getId());
			String queryString = "select distinct rwe from RealWorldElement rwe, RealLink rl, ScriptElement se "
					+ "where rwe.id = rl.realWorldElement.id and rl.scriptElement.id = se.id and "
					+ " se.type != ? and se.project = ?";
			return find(queryString, values);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Finds all RealWorldElements that are linked (via RealLink's) to any ScriptElement associated
	 * with the specified project.
	 *
	 * @param project
	 * @return A non-null List
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findByProject(Project project) {
		try {
			Object[] values = { project };
			log.debug("Project=" + project.getId());
			String queryString = "select distinct rwe from RealWorldElement rwe, RealLink rl, ScriptElement se "
					+ "where rwe.id = rl.realWorldElement.id and rl.scriptElement.id = se.id and se.project =?";
			return find(queryString, values);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Finds all CHARACTER-type Real World Elements that are associated with
	 * Contacts who are members of the specified Project, and who have a role
	 * in either the Cast or Stunt Department. This is used on the Script Element page,
	 * for the "Add Link" (for Linked To/Played By) to generate a list of
	 * candidate people to play a character.
	 *
	 * @param project The Project that the candidate people must belong to.
	 * @return A non-null, but possibly empty, List of RealWorldElement's,
	 *         ordered by their name field.
	 */
	@SuppressWarnings("unchecked")
	public List<RealWorldElement> findCastByProject(Project project) {
		try {
			log.debug("Project=" + project.getId());
			String queryString = "select distinct rwe from RealWorldElement rwe, ProjectMember pm " +
				" where rwe.type='CHARACTER' and pm.unit.project = ? and pm.employment.contact = rwe.actor " +
				" and pm.employment.role.department.id in ( " +
				Constants.DEPARTMENT_ID_CAST + "," + Constants.DEPARTMENT_ID_STUNTS + ") " +
				" order by rwe.name";
			return find(queryString, project);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * @return The database id of the Location (Real World Element) that
	 *         corresponds to the name given in the Location field of this
	 *         SceneCall. Returns null if a matching location is not found.
	 */
	@Transient
	public static Integer getLocationId(SceneCall sc) {
		if ( ! sc.getLocationChecked()) {
			try {
				if (sc.getLocation() != null && sc.getLocation().trim().length() > 0) {
					RealWorldElementDAO rwDAO = getInstance();
					RealWorldElement elem = rwDAO.findByNameType(sc.getLocation().trim(),ScriptElementType.LOCATION);
					if (elem != null) {
						sc.setLocationId(elem.getId());
					}
				}
				sc.setLocationChecked(true);
			}
			catch (Exception e) {
			}
		}
		return sc.getLocationId();
	}

	/**
	 * @param sc
	 * @return The Location (Real World Element) that corresponds to the name
	 *         given in the Location field of this SceneCall. Returns null if a
	 *         matching location is not found.
	 */
	@Transient
	public static RealWorldElement getLocationElement(SceneCall sc) {
		RealWorldElement elem = null;
		if ( ! sc.getLocationChecked()) {
			getLocationId(sc);
		}
		try {
			if (sc.getLocationId() != null) {
				RealWorldElementDAO rwDAO = getInstance();
				elem = rwDAO.findById(sc.getLocationId());
			}
		}
		catch (Exception e) {
		}
		return elem;
	}

	/**
	 * Find a RealWorldElement (production element), if any, that is assigned to
	 * a particular ScriptElement. The 'link' status must be "SELECTED" for the
	 * RealWorldElement to be returned.
	 *
	 * @param element - The ScriptElement
	 * @return A RealWorldElement that has been linked to satisfy the script
	 *         element, or null if no RealWorldElement is linked to the given
	 *         ScriptElement with a SELECTED status. If more than one RWE is
	 *         linked to the ScriptElement with SELECTED status, a random one of
	 *         them is returned.
	 */
	@SuppressWarnings("unchecked")
	public RealWorldElement findLinkedRealWorldElement(ScriptElement element) {
		RealWorldElement rwe = null;
		//log.debug("element id="+element.getId());
		try {
			String queryString = "select re " +
					"from RealLink rl, RealWorldElement re " +
					"where rl.scriptElement = ? " +
					"and  rl.status = 'SELECTED' " +
					"and rl.realWorldElement = re ";
			/* Equivalent SQL:
			 * Select re.* from Real_Link rl, real_world_element re
					where rl.script_element_id= <id>
					and  rl.status = 'SELECTED'
					and rl.real_element_id=re.id;
			 */
			List<RealWorldElement> rweList = find(queryString, element);
			if (rweList != null && rweList.size()>0) {
				rwe = rweList.get(0);
			}
			log.debug("RW elem id="+(rwe==null? "null" : rwe.getId()));
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
		return rwe;
	}

	/**
	 * Determine if there's any RealWorldElement that is assigned to a particular ScriptElement. The
	 * 'link' status must be "SELECTED" for this method to return 'true'.  The same information could
	 * be obtained by calling findLinkedRealWorldElement() and checking for a non-null result, but
	 * the SQL used here is more efficient, since it just gets the count() back instead of the object.
	 *
	 * @param element - The ScriptElement
	 * @return true if at least one Real World Element is linked to this script Element with a link
	 *         status of "SELECTED".
	 */
	@SuppressWarnings("unchecked")
	public boolean existsLinkedRealWorldElement(ScriptElement element) {
		boolean bRet = false;
		log.debug("element id="+element.getId());
		try {
			String queryString = "select count(re.id) " +
					"from RealLink rl, RealWorldElement re " +
					"where rl.scriptElement = ? " +
					"and  rl.status = 'SELECTED' " +
					"and rl.realWorldElement = re ";
			List<Long> list = find(queryString, element);
			if (list.size() > 0) {
				if (list.get(0).longValue() > 0) {
					bRet = true;
				}
			}
			log.debug("ret=" + bRet);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
		return bRet;
	}

	/**
	 * Add or merge the supplied realElement, after first updating the Image objects so
	 * they are associated with the given element.
	 *
	 * @param element The element to be saved or updated.
	 * @param images The Collection of images to associate with the element.
	 * @return The updated item.
	 */
	@Transactional
	public RealWorldElement update(RealWorldElement element, Collection<Image> images) {
		if (images != null) {
			for (Image image : images) {
				image.setRealWorldElement(element);
			}
		}
		if (element.getId() == null) {
			save(element);
		}
		else {
			element = merge(element);
		}
		return element;
	}

	/**
	 * Updates the RealLink collection and DateRange collection, removing those items that
	 * were "deleted" by the user but not yet deleted from the database.  It also updates
	 * a list of ScriptElements affected by the deletion of the RealLinks, so that their
	 * "requirements satisfied" flag setting can be checked later.
	 * @param realElement
	 * @param removedElemLinks
	 * @param removedDateRanges
	 * @return The Set of ScriptElement`s to be checked later.
	 */
	@Transactional
	public Set<ScriptElement> updateSubItems(RealWorldElement realElement,
			List<Integer> removedElemLinks, List<Integer> removedDateRanges) {
		Set<ScriptElement> checkElements = new HashSet<>();
		try {
			log.debug(removedElemLinks.size());
			log.debug(removedDateRanges.size());
			if (removedElemLinks.size() > 0 || removedDateRanges.size() > 0) {
				RealLinkDAO realLinkDAO = RealLinkDAO.getInstance();
				RealWorldElement tempElement = findById(realElement.getId());
				for (Integer linkId : removedElemLinks) {
					RealLink rl = realLinkDAO.findById(linkId); // fresh copy
					boolean remove1 = tempElement.getRealLinks().remove(rl);
					ScriptElement sElem = rl.getScriptElement();
					checkElements.add(sElem);
					boolean remove2 = sElem.getRealLinks().remove(rl);
					if (remove1 && remove2) {
						realLinkDAO.delete(rl);
						log.debug("remove successful");
					}
					else {
						EventUtils.logError("RealElemetnBean: remove-rw=" + remove1 + ", remove-se=" + remove2);
						log.debug("rl=" + rl + ", rw-set:" + tempElement.getRealLinks() + ", se-set:"
								+ sElem.getRealLinks());
					}
				}
				tempElement.getRealLinks().clear();
				tempElement.setRealLinks(null); // avoids "NonUniqueObjectException" on realLink's
				removedElemLinks.clear();  // just in case
				if (removedDateRanges.size() > 0) {
					DateRangeDAO dateRangeDAO = DateRangeDAO.getInstance();
					for (Integer id : removedDateRanges) {
						DateRange range = dateRangeDAO.findById(id); // fresh copy
						boolean remove1 = tempElement.getDateRanges().remove(range);
						if (remove1) {
							dateRangeDAO.delete(range);
							log.debug("remove successful");
						}
						else {
							EventUtils.logError("RealElemetnBean: remove-date=" + remove1);
						}
					}
					tempElement.setDateRanges(null); // avoids "NonUniqueObjectException"
					removedDateRanges.clear();  // just in case}
				}
				evict(tempElement);
				tempElement = null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return checkElements;
	}

	/**
	 * Remove -- delete -- a RealWorldElement entirely. This requires first
	 * removing it from its associations with ScriptElements (via RealLinks),
	 * then doing the actual delete.
	 *
	 * @param element The RealWorldElement to delete from the database.
	 */
	@Transactional
	public void remove(RealWorldElement element) {
		if (element.getRealLinks() != null) {
			RealLinkDAO realLinkDAO = RealLinkDAO.getInstance();
			for ( Iterator<RealLink> iter = element.getRealLinks().iterator(); iter.hasNext(); ) {
				RealLink r = iter.next();
				RealLink rl = realLinkDAO.findById(r.getId()); // fresh copy
				iter.remove();
				ScriptElement elem = rl.getScriptElement();
				boolean remove2 = elem.getRealLinks().remove(rl);
				if (remove2) {
					realLinkDAO.delete(rl);
					log.debug("remove successful");
					// TODO may need to update ScriptElement requirementSatisfied flag.
				}
				else {
					EventUtils.logError("remove-rw=" + remove2);
				}
			}
			element.getRealLinks().clear();
		}
		delete(element);
	}

}
