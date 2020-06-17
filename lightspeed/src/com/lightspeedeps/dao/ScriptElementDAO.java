//	File Name:	ScriptElementDAO.java
package com.lightspeedeps.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.RealLink;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.script.ScriptUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * ScriptElement entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ScriptElement
 */
public class ScriptElementDAO extends BaseTypeDAO<ScriptElement> {
	private static final Log log = LogFactory.getLog(ScriptElementDAO.class);
	// property constants
//	private static final String NAME = "name";
//	private static final String ELEMENT_ID = "elementId";
//	private static final String DESCRIPTION = "description";
//	private static final String TYPE = "type";
//	private static final String REAL_ELEMENT_REQUIRED = "realElementRequired";
//	private static final String REQUIREMENT_SATISFIED = "requirementSatisfied";
	private static final String PROJECT = "project";

	public static ScriptElementDAO getInstance() {
		return (ScriptElementDAO)getInstance("ScriptElementDAO");
	}

//	public static ScriptElementDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ScriptElementDAO) ctx.getBean("ScriptElementDAO");
//	}

	public List<ScriptElement> findByProject(Project project) {
		return findByProperty(PROJECT, project);
	}

	/**
	 * Find the ScriptElement corresponding to a specific element name, type, and project.  There
	 * should only be one element (or none) for a given combination.
	 * @param strName The name of the ScriptElement
	 * @param type The ScriptElementType of the element
	 * @param project Which project the element is associated with.
	 * @return The matching element, or null if not found
	 */
	public ScriptElement findByNameTypeProject(String strName, ScriptElementType type, Project project) {
		final String FIND_BY_NAME = "from ScriptElement s where s.name = ? and s.type = ? and s.project = ?";
		ScriptElement se = findOne(FIND_BY_NAME, new Object[]{strName,type,project});
		return se;
	}

	public boolean existsNameTypeProject(String strName, ScriptElementType type, Project project) {
		return (findByNameTypeProject(strName, type, project) != null);
	}

//	/**
//	 * Find the ScriptElements matching the specified status, element type, and project.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<ScriptElement> findScriptElementType(String status, ScriptElementType type, Project project) {
//		List<ScriptElement> list = null;
//		try {
//			Object[] values = { status, type, project };
//			String queryString = "select distinct se from ScriptElement se, RealLink  rl " +
//					" where rl.status = ? and se.type = ? " +
//					" and rl.scriptElement.id = se.id  and se.project = ?";
//			list = find(queryString, values);
//			return list;
//		}
//		catch (RuntimeException e) {
//			EventUtils.logError(e);
//			throw e;
//		}
//	}

	/**
	 * Find ScriptElements with the specified type that are part of the given project,
	 * sorted in ascending Name order.
	 * @param type
	 * @param project
	 * @return A (possibly empty) List of elements; will not return null.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findByTypeAndProject(ScriptElementType type, Project project) {
		Object[] values = { type, project };
		String queryString = "from ScriptElement where type = ? and project = ? order by name";
		return find(queryString, values);
	}

	/**
	 * Find CHARACTER ScriptElements that are part of the given project and which have the
	 * "include on callsheet" flag set to true.
	 *
	 * @param project
	 * @return A non-null, but possibly empty, List of elements.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findByProjectForAllCallsheets(Project project) {
		String queryString = "from ScriptElement where project = ? " +
				" and type = 'CHARACTER' " +
				" and includeOnCallsheet = 1 ";
		return find(queryString, project);
	}

	/** Query used in findNonOrphansByProject and findNonOrphansByTypeAndProject. */
	private final static String QUERY_NON_ORPHAN = "project_id = :projectId and ( id in " +
	"(select se.id from script_element se, scene_script_element sse, scene scn " +
		"where sse.scene_id = scn.id " +
		"and scn.script_id = :scriptId " +
		"and sse.script_element_id = se.id " +
		"group by se.id ) ";

	/** Query used in findNonOrphansByProject and findNonOrphansByTypeAndProject. */
	private final static String QUERY_NON_ORPHAN_LOCATION = " or id in " +
		"(select distinct sn.set_id from scene sn " +
			"where sn.script_id = :scriptId " +
			"and sn.set_id is not null)";

	/**
	 * Find ScriptElement`s that are part of the given project, and are
	 * referenced by at least one scene (in the current script) in the project.
	 *
	 * @param project The Project to be searched.
	 * @return A (possibly empty) List of ScriptElement`s; will not return null.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findNonOrphansByProject(Project project) {
		try {
			if (project.getScript() == null) {
				// no script, so no ScriptElements can match
				return new ArrayList<ScriptElement>();
			}
			String queryString =
				"select * from script_element where " + QUERY_NON_ORPHAN +
				QUERY_NON_ORPHAN_LOCATION + ")";
			Query query = getHibernateSession().createSQLQuery(queryString)
					.addEntity(ScriptElement.class)
					.setInteger("scriptId", project.getScript().getId())
					.setInteger("projectId", project.getId());
			List<ScriptElement> list = query.list();
			log.debug("project# "+project.getId() + ", count=" + list.size());
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Find all ScriptElement`s with the specified type that are part of the
	 * given project, and are referenced by at least one scene in the Project's
	 * current Script.
	 *
	 * @param type The ScriptElementType of the entries wanted.
	 * @param project The Project to be searched.
	 * @return A (possibly empty) List of elements; will not return null.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findNonOrphansByTypeAndProject(ScriptElementType type, Project project) {
		try {
			if (project.getScript() == null) {
				// no script, so no ScriptElements can match
				return new ArrayList<ScriptElement>();
			}
			String queryString =
				"select * from script_element where type = :type and " + QUERY_NON_ORPHAN;
			if (type == ScriptElementType.LOCATION) {
				queryString += QUERY_NON_ORPHAN_LOCATION;
			}
			queryString += ")";

			Query query = getHibernateSession().createSQLQuery(queryString)
					.addEntity(ScriptElement.class)
					.setString("type", type.name())
					.setInteger("scriptId", project.getScript().getId())
					.setInteger("projectId", project.getId());
			List<ScriptElement> list = query.list();
			log.debug("type: " + type.name() + ", project# "+project.getId() + ", count=" + list.size());
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Determine if the given Location (Set) ScriptElement is an orphan, i.e.,
	 * not in use by any scene in the given Project.
	 *
	 * @param project The Project to be searched.
	 * @param location The ScriptElement, with ScriptElementType LOCATION, to be
	 *            searched for.
	 * @return True iff the location ScriptElement is NOT used as the Set in any
	 *         scene in any script in the specified Project.
	 */
	@SuppressWarnings("unchecked")
	public boolean isLocationOrphanByProject(Project project, ScriptElement location) {
		try {
			if (project.getScript() == null) {
				// no script, so any Location is an orphan!
				return true;
			}
			String queryString = "select count(sn.id) from scene sn, script s " +
					"where sn.script_id = s.id " +
					" and s.project_id = :projectId " +
					" and sn.set_id = :locationId";
			Query query = getHibernateSession().createSQLQuery(queryString)
					.setInteger("locationId", location.getId())
					.setInteger("projectId", project.getId());
			List<BigInteger> list = query.list();
			BigInteger count = BigInteger.ZERO;
			if (list.size() > 0) {
				count = list.get(0);
			}
			log.debug("project# "+project.getId() + ", count=" + count);
			return count.intValue() == 0;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/** Query used in findOrphansByProject and findOrphansByTypeAndProject. */
//	private final static String QUERY_ORPHAN = "project_id = :projectId and id not in " +
//	"(select se.id from script_element se, scene_script_element sse, scene scn " +
//		"where sse.scene_id = scn.id " +
//		"and scn.script_id = :scriptId " +
//		"and sse.script_element_id = se.id " +
//		"group by se.id ) ";
//
//	/** Query used in findOrphansByProject and findOrphansByTypeAndProject. */
//	private final static String QUERY_ORPHAN_LOCATION = " and id not in " +
//		"(select distinct sn.set_id from scene sn " +
//			"where sn.script_id = :scriptId " +
//			"and sn.set_id is not null)";

	/**
	 * Find ScriptElement`s that are part of the given project, but are not
	 * referenced by any scene (in the current script) in the project.
	 *
	 * @param project The Project to be searched.
	 * @return A (possibly empty) List of ScriptElement`s; will not return null.
	 */
//	@SuppressWarnings("unchecked")
//	public List<ScriptElement> findOrphansByProject(Project project) {
//		try {
//			if (project.getScript() == null) {
//				// no script, so no ScriptElements can match
//				return new ArrayList<ScriptElement>();
//			}
//			String queryString =
//				"select * from script_element where " + QUERY_ORPHAN +
//				QUERY_ORPHAN_LOCATION;
//			Query query = getHibernateSession().createSQLQuery(queryString)
//					.addEntity(ScriptElement.class)
//					.setInteger("scriptId", project.getScript().getId())
//					.setInteger("projectId", project.getId());
//			List<ScriptElement> list = query.list();
//			log.debug("project# "+project.getId() + ", count=" + list.size());
//			return list;
//		}
//		catch (RuntimeException e) {
//			EventUtils.logError(e);
//			throw e;
//		}
//	}

	/**
	 * Find all ScriptElement`s with the specified type that are part of the
	 * given project, but are not referenced by any scene in the Project's
	 * current Script.
	 *
	 * @param type The ScriptElementType of the entries wanted.
	 * @param project The Project to be searched.
	 * @return A (possibly empty) List of elements; will not return null.
	 */
//	@SuppressWarnings("unchecked")
//	public List<ScriptElement> findOrphansByTypeAndProject(ScriptElementType type, Project project) {
//		try {
//			if (project.getScript() == null) {
//				// no script, so no ScriptElements can match
//				return new ArrayList<ScriptElement>();
//			}
//			String queryString =
//				"select * from script_element where type = :type and " + QUERY_ORPHAN;
//			if (type == ScriptElementType.LOCATION) {
//				queryString += QUERY_ORPHAN_LOCATION;
//			}
//			Query query = getHibernateSession().createSQLQuery(queryString)
//					.addEntity(ScriptElement.class)
//					.setString("type", type.name())
//					.setInteger("scriptId", project.getScript().getId())
//					.setInteger("projectId", project.getId());
//			List<ScriptElement> list = query.list();
//			log.debug("type: " + type.name() + ", project# "+project.getId() + ", count=" + list.size());
//			return list;
//		}
//		catch (RuntimeException e) {
//			EventUtils.logError(e);
//			throw e;
//		}
//	}

	/**
	 * Find all the "parent" ScriptElements of the given one -- that is, all
	 * those ScriptElements that have a childElement relation pointing to the
	 * given one.
	 *
	 * @param child The child element to be searched for in the child_element
	 *            table.
	 * @return A non-null but possibly empty List of ScriptElements. Every
	 *         ScriptElement in the returned list should have a childElements
	 *         Set that contains the given 'child'.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findParentsOf(ScriptElement child) {
		List<ScriptElement> list = null;
		try {
			String queryString = "select se.* " +
					"from Script_Element se, child_element ce " +
					"where ce.child_id = :childId " +
					" and se.id = ce.parent_id ";				// 2nd parameter = Project
			Query query = getHibernateSession().createSQLQuery(queryString)
					.addEntity(ScriptElement.class)
					.setInteger("childId", child.getId());
			list = query.list();
			log.debug("parent count=" + list.size());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			list = new ArrayList<ScriptElement>();
		}
		return list;
	}

	/**
	 * Find the "CHARACTER" ScriptElements which correspond to a given
	 * Contact who is the actor portraying the character(s). Note that one
	 * actor may be assigned to more than one character!  The 'link' status
	 * must be "SELECTED" for the ScriptElement to be returned.  Project must
	 * be specified, since one Contact may be assigned to different characters
	 * within different projects.
	 * @param contact - The actor's contact object.
	 * @return The ScriptElements of the character the actor is assigned to play.
	 */
	@SuppressWarnings("unchecked")
	public List<ScriptElement> findCharacterFromContact(Contact contact, Project project) {
		List<ScriptElement> elements = null;
		/* Equivalent SQL:
		 * Select se.* from Script_Element se, Real_Link rl, Real_World_Element re
					where re.contact_id = :contactId
					and  rl.real_element_id=re.id
					and  rl.status = "SELECTED"
					and  rl.script_element_id= se.id
					and se.project_id = :projectId;
		 */
		String queryString = "Select se " +
				"from ScriptElement se, RealLink rl, RealWorldElement re " +
				"where re.actor = ? " + // 1st parameter = Contact
				"and  rl.realWorldElement = re " +
				"and  rl.status = 'SELECTED' " +
				"and  rl.scriptElement = se " +
				"and  se.project = ?";				// 2nd parameter = Project
		Object [] values={contact, project};
		elements = find(queryString, values);
		if (log.isDebugEnabled()) {
			String s = "";
			if (elements != null) for (ScriptElement e : elements) { s += e.getId()+", "; }
			log.debug("character (ScriptElement) ids="+(elements==null ? "null" : s) );
		}
		return elements;
	}

	/**
	 * Find the ScriptElement matching these values. Normally this should
	 * identify a unique item, so we return just a ScriptElement instead of a
	 * list.
	 *
	 * @param elementIdStr : the element_id String (this is the cast-id for cast
	 *            members); this is the alphanumeric text id, not the generated
	 *            integer equivalent.
	 * @param project : the project that contains the requested item
	 * @param type : the ScriptElementType of the ScriptElement to match. Note
	 *            that the same elementIdStr may exist for different element
	 *            types within a single project. E.g., there may be both a
	 *            Character and a Prop with an elementIdStr of "1".
	 */
	public ScriptElement findByElementIdProjectType(String elementIdStr, Project project, ScriptElementType type) {
		final String FIND_BY_ELEMENTID_PROJECT_TYPE =
				"from ScriptElement s where s.elementIdStr = ? and s.project = ? and s.type = ?";
		return findOne(FIND_BY_ELEMENTID_PROJECT_TYPE, new Object[]{elementIdStr,project, type});
	}

	/**
	 * Determine if any ScriptElement matching these values exists.
	 *
	 * @param elementid : the element_id String (this is the cast-id for cast
	 *            members); this is the generated integer equivalent, not the
	 *            alphanumeric text id.
	 * @param project : the project that contains the requested item
	 * @param type : the ScriptElementType of the ScriptElement to match. Note
	 *            that the same elementIdStr may exist for different element
	 *            types within a single project. E.g., there may be both a
	 *            Character and a Prop with an elementIdStr of "1".
	 */
	public boolean existsElementIdProjectType(Integer elementid, Project project, ScriptElementType type) {
		final String EXISTS_BY_ELEMENTID_PROJECT_TYPE = "from ScriptElement s " +
				" where s.elementId = ? and s.project = ? and s.type = ?";
		boolean bRet = exists(EXISTS_BY_ELEMENTID_PROJECT_TYPE, new Object[]{elementid, project, type});
		return bRet;
	}

	/***
	 * Determine if the given database id, which should refer to a ScriptElement
	 * object of type LOCATION, is referenced by any Scene in the database.
	 * Since the database id is unique, we don't need to qualify the search by
	 * Script or Project or anything else.
	 *
	 * @param locationId The ScriptElement.id value to be searched for.
	 * @return True iff any Scene in the database has the specified LOCATION as
	 *         its Scene.scriptElement (set/location) value.
	 */
	public boolean isLocationElementReferenced(Integer locationId) {
		final String query = "from Scene s where s.scriptElement.id = ?";
		boolean bRet = exists(query, new Object[]{locationId});
		return bRet;
	}

	/**
	 * Determine if the given database id, which can refer to a ScriptElement
	 * object of any type except LOCATION, is referenced by any Scene in the
	 * database. Since the database id is unique, we don't need to qualify the
	 * search by Script or Project or anything else.
	 *
	 * @param elementId The ScriptElement.id value to be searched for.
	 * @return True iff any Scene in the database has a link to the specified
	 *         ScriptElement
	 */
	@SuppressWarnings("unchecked")
	public boolean isElementReferenced(Integer elementId) {
		try {
			boolean bRet = false;
			// it's much simpler and faster to use a native query against the relation table,
			// then to construct a query referencing all the Scenes in the database.
			String queryString =
				"select count(scene_id) from scene_script_element where script_element_id = :id";
			Query query = getHibernateSession().createSQLQuery(queryString)
					.setInteger("id", elementId);
			List<BigInteger> list = query.list();
			if (list != null && list.size() > 0 && list.get(0).longValue() > 0) {
				bRet = true;
			}
			return bRet;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Get the Scenes in which the given script element appears within the script.
	 * @param scriptElement - the element of interest
	 * @param script - the script of interest
	 * @return A List of Scenes in 'script' that contain 'scriptElement'.
	 */
	@SuppressWarnings("unchecked")
	public List<Scene> getOccurs(ScriptElement scriptElement, Script script) {
		List<Scene> list = null;
		try {
			if (scriptElement.getType() == ScriptElementType.LOCATION) {
				list = getHibernateSession()
						.createFilter( script.getScenes(), "where this.scriptElement = ?" )
						.setEntity(0, scriptElement)
						.list();
				log.debug("count for SET "+scriptElement.getId()+" in script "+script.getId()+" = "+list.size());
			}
			else {
				list = getHibernateSession()
						.createFilter( scriptElement.getScenes(), "where this.script = ?" )
						.setEntity(0, script)
						.list();
				log.debug("count for element "+scriptElement.getId()+" in script "+script.getId()+" = "+list.size());
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}

		return list;
	}

	/**
	 * Returns the id's of all the ScriptElements in the specified script and
	 * project which are CHARACTER elements that do not have an element_id (cast-id)
	 * value yet.  The information is returned as a List of arrays.  Each
	 * array has 2 Integer objects: the first is the id, and the second is the
	 * count of occurrences of that element in the script (that is, the number
	 * of scenes in which it occurs).  The array is sorted in descending order
	 * by the number of occurrences.  This method is used by script import, to assign
	 * new cast-ids after an import completes.
	 * @param project The Project that owns the script and script elements.
	 * @param script The script that uses the script elements; this is used to
	 * determine the number of occurrences in the script for each ScriptElement.
	 * @return List of Object arrays, each array with two Integer entries: the ScriptElement.id
	 * field, and the number of its occurrences.
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findUnassignedCastIds(Project project, Script script) {
		String strQuery = "select se.id, count(se.id) " +
				"from Scene s left join s.scriptElements se " +
				"where se.type = ? " +
				"and se.project = ? " +
				"and se.elementId is null " +
				"and s.script = ? " +
				"group by se.id order by count(se.id) desc";

		List<Object[]> pairList = null;
		ScriptElementType cast = ScriptElementType.CHARACTER;
		Object [] values={cast, project, script};
		pairList = find(strQuery, values );

		log.debug("Unassigned cast id list count="+pairList.size() +
				", script#=" + script.getId() + ", project#="+project.getId());
		if (pairList.size() == 0) {
			log.info("NO Unassigned cast id found -- query=" + strQuery);
			log.info("values=" + values);
			log.info("script=" + script);
			log.info("project=" + project);
			for (Scene s : script.getScenes()) {
				log.debug("scene=" + s);
			}
		}
		return pairList;
	}

	/**
	 * Find the highest elementId (cast-id) value for all ScriptElements in the given Project
	 * whose cast-ids are purely numeric (no leading or trailing alphabetic characters).
	 * Returns 0 if there are no scriptElements in the Project, or if none of them have
	 * numeric cast-ids.
	 */
	@SuppressWarnings("unchecked")
	private int findMaxElementId(Project project, ScriptElementType type) {
		String strQuery = "select max(s.elementId) from ScriptElement s " +
				"where s.project = ? and s.type = ? and " +
				" s.elementId < " + ScriptElement.CASTID_PREFIX_FACTOR + " and " +
				" mod(s.elementId," + ScriptElement.CASTID_NUMBER_FACTOR + ") = 0 ";
		int elementId = 0;
		List<Integer> list = find(strQuery, new Object[]{project, type});
		if (list != null && list.size() > 0) {
			if (list.get(0) != null) {
				elementId = list.get(0).intValue();
				elementId /= ScriptElement.CASTID_NUMBER_FACTOR;	// get actual cast-id value
			}
		}
		log.debug("max numeric cast-id found=" + elementId);
		return elementId;
	}

	public String findNextElementId(Project project, ScriptElementType type) {
		return "" + (findMaxElementId(project, type)+1);
	}

	/**
	 * Determine the appropriate setting of the given ScriptElement's "requirement satisfied"
	 * boolean, and update it in the database if it is not correct in the given object.
	 * Note that if the ScriptElement's "realElementRequired" flag is false, this method
	 * does nothing.
	 *
	 * @param scriptElement
	 */
	@Transactional
	public void updateRequirementSatisfied(ScriptElement scriptElement) {
		if (scriptElement.getRealElementRequired()) {
			boolean satisfied = RealWorldElementDAO.getInstance().existsLinkedRealWorldElement(scriptElement);
			if (satisfied != scriptElement.getRequirementSatisfied().booleanValue()) {
				scriptElement.setRequirementSatisfied(satisfied);
				attachDirty(scriptElement);
			}
		}
	}

	/**
	 * Add or merge the supplied ScriptElement, after first updating the Image objects so
	 * they are associated with the given element, and assigning the next available cast-id
	 * (elementId) for Character elements if it does not already have one.
	 *
	 * @param element The element to be saved or updated.
	 * @param images The Collection of images to associate with the element.
	 * @return The updated element.
	 */
	@Transactional
	public ScriptElement update(ScriptElement element, Collection<Image> images) {
		if (images != null) {
			for (Image image : images) {
				image.setScriptElement(element);
			}
		}
		if (element.getType() == ScriptElementType.CHARACTER && element.getElementId() == null) {
			element.setElementIds(findNextElementId(element.getProject(), ScriptElementType.CHARACTER));
		}
		if (element.getId() == null) { // New element
			// Save a refreshed copy of any linked scenes
			SceneDAO sceneDAO = SceneDAO.getInstance();
			Set<Scene> copy = new HashSet<Scene>();
			for (Scene s : element.getScenes()) {
				s = sceneDAO.refresh(s);
				copy.add(s);
			}
			save(element); // Add the new ScriptElement to the database
			// Then update any linked scenes appropriately...
			for (Scene s : copy) {
				s.getScriptElements().add(element);
				attachDirty(s);
			}
		}
		else {
			attachDirty(element);
		}
		return element;
	}

	/**
	 * Update the daysHeldBeforeDrop in all the CHARACTER ScriptElements for the
	 * specified Project to match the default value in the Project.
	 *
	 * @param project
	 */
	@Transactional
	public void updateDaysHeld(Project project) {
		Collection<ScriptElement> elements = findByTypeAndProject(ScriptElementType.CHARACTER, project);
		Integer days = project.getDaysHeldBeforeDrop();
		log.debug("days=" + days + ", elements=" + elements.size());
		for (ScriptElement se : elements) {
			se.setDaysHeldBeforeDrop(days);
			attachDirty(se);
		}
	}

	/**
	 * Add each of the ScriptElements in the Set provided to the given Scene.
	 *
	 * @param linkedElems The Set of elements that are to be added to the given
	 *            Scene. May be empty but not null.
	 * @param scene The Scene to which the elements will be added. May not be
	 *            null.
	 */
	@Transactional
	public void addElementsToScene(Set<ScriptElement> linkedElems, Scene scene) {
		for (ScriptElement se : linkedElems) {
			se = refresh(se);
			if (se.getType() != ScriptElementType.LOCATION) {
				se.getScenes().add(scene);
				se.setScenes(se.getScenes()); // forces sceneList refresh
				attachDirty(se);
			}
		}
	}

	/**
	 * Replace all the references within a Script to a particular ScriptElement
	 * with a reference to a different ScriptElement. Used by the Script Element
	 * "merge" facility.
	 *
	 * @param script The Script to be updated.
	 * @param sourceId The database id of the ScriptElement to be removed from
	 *            all Scene's in the Script.
	 * @param targetId The database id of the ScriptElement which will be added
	 *            to all Scene's that had a reference to the 'source' element.
	 * @return The number of occurrences that were replaced, i.e., the number of
	 *         Scene's which referenced the source element.
	 */
	@Transactional
	public int replaceElement(Script script, Integer sourceId, Integer targetId) {
		ScriptElement source = findById(sourceId);
		ScriptElement target = findById(targetId);
		int reps = ScriptUtils.replaceScriptElement(script, source, target);
		return reps;
	}

	/**
	 * Delete all the ScriptElements in the given Project. This is typically done when the last
	 * Script in a project is deleted.  The RealLinks associating any of these with Real World
	 * Elements will be automatically deleted due to the Cascade setting.
	 *
	 * @param project The project whose ScriptElements should be deleted.
	 */
	@Transactional
	public void deleteAll(Project project) {
		log.debug("");
		Collection<ScriptElement> elements = findByProject(project);
		for (ScriptElement se : elements) {
			delete(se);
		}
	}

	/**
	 * Delete all the ScriptElements defined by the List of database ids
	 * (Integers) provided.
	 *
	 * @param idList A List of Integers; each one is the database id of a
	 *            ScriptElement to be deleted.
	 */
	@Transactional
	public void deleteAll(List<Integer> idList) {
		log.debug("");
		for (Integer id : idList) {
			ScriptElement se = findById(id);
			if (se != null) {
				delete(se);
			}
		}
	}

	/**
	 * Remove -- delete -- a scriptElement entirely. This requires first
	 * removing it from its associations with Scenes and RealWorldElements (via
	 * RealLinks), and from any childElement relations (linked elements), then
	 * doing the actual delete.
	 *
	 * @param element The object to be deleted.
	 */
	@Transactional
	public void remove(ScriptElement element) {
		// Remove from "appears in Scene" relations
		if (element.getScenes() != null && element.getScenes().size() > 0) {
			for (Scene scene : element.getScenes()) {
				scene.getScriptElements().remove(element);
				attachDirty(scene);
			}
			element.getScenes().clear();
		}
		// Remove from "played by" relations (links to RealWorldElements)
		if (element.getRealLinks() != null && element.getRealLinks().size() > 0) {
			RealLinkDAO realLinkDAO = RealLinkDAO.getInstance();
			for (Iterator<RealLink> iter = element.getRealLinks().iterator(); iter.hasNext(); ) {
				RealLink r = iter.next();
				RealLink rl = realLinkDAO.findById(r.getId()); // fresh copy
				iter.remove();
				RealWorldElement realElem = rl.getRealWorldElement();
				boolean remove2 = realElem.getRealLinks().remove(rl);
				if (remove2) {
					realLinkDAO.delete(rl);
					log.debug("remove successful");
				}
				else {
					EventUtils.logError("remove-rw=" + remove2);
				}
			}
			element.getRealLinks().clear();
		}
		// Remove from "linked element" relations
		List<ScriptElement> list = findParentsOf(element);
		for (ScriptElement se : list) {
			se.getChildElements().remove(element);
			attachDirty(se);
		}
		delete(element);
	}

	/**
	 * Create a String consisting of the elementId's (cast ids) of the supplied
	 * elements, concatenated with embedded commas, in ascending order.
	 *
	 * @param elements
	 * @param castOnly If true, only element-ids of cast members (CHARACTERs) are
	 * included. Otherwise, all non-null element-ids are included.
	 * @return The non-null string of element ids
	 */
	public static String createElementIdString(Collection<ScriptElement> elements, boolean castOnly) {
		String idList = "";
		Map<Integer,String> idMap = createElementIdMap(elements, castOnly);
		if (idMap.size() > 0) {
			StringBuffer buff = new StringBuffer();
			boolean first = true;
			for (Integer key : idMap.keySet()) {
				if ( ! first) {
					buff.append(", ");
				}
				buff.append(idMap.get(key));
				first = false;
			}
			idList = buff.toString();
		}
		return idList;
	}

	/**
	 * Create a SortedMap between the numeric elementId values and their
	 * matching elementIdStr (cast-id) strings, for all the entries in the given
	 * collection of ScriptElements. This is typically used to construct a
	 * textual list of cast-ids in their proper order.
	 *
	 * @param elements The collection of elements whose elementIds are eligible
	 *            to be put into the map.
	 * @param castOnly If true, only element-ids of cast members (CHARACTERs)
	 *            are included. Otherwise, all non-null element-ids are
	 *            included.
	 * @return A non-null, but possibly empty Map as described above.
	 */
	private static SortedMap<Integer,String> createElementIdMap(Collection<ScriptElement> elements, boolean castOnly) {
		SortedMap<Integer,String> idMap = new TreeMap<Integer,String>();
		String id;
		for (ScriptElement se : elements) {
			if (!castOnly || se.getType() == ScriptElementType.CHARACTER) {
				if (se.getElementId() != null &&
						(id=se.getElementIdStr()) != null && id.length() > 0) {
					idMap.put(se.getElementId(), id);
				}
			}
		}
		return idMap;
	}

}
