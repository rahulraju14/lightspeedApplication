//	File Name:	ContactDAO.java
package com.lightspeedeps.dao;

import java.util.*;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.login.PasswordResetBean;

/**
 * A data access object (DAO) providing persistence and search support for
 * Contact entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Contact
 */
public class ContactDAO extends BaseTypeDAO<Contact> {
	private static final Log log = LogFactory.getLog(ContactDAO.class);
	// property constants
	public static final String PRODUCTION = "production";
	public static final String USER = "user";
	private static final String ASSISTANT = "assistant";
//	private static final String HOME_PHONE = "homePhone";
//	private static final String BUSINESS_PHONE = "businessPhone";
//	private static final String CELL_PHONE = "cellPhone";
//	private static final String PRIMARY_PHONE_INDEX = "primaryPhoneIndex";
	public static final String EMAIL_ADDRESS = "emailAddress";
//	private static final String CONTACT_ASST_FIRST = "contactAsstFirst";
//	private static final String NOTIFY_BY_EMAIL = "notifyByEmail";
//	private static final String NOTIFY_BY_TEXT_MSG = "notifyByTextMsg";
//	private static final String NOTIFY_BY_ASST_EMAIL = "notifyByAsstEmail";
//	private static final String NOTIFY_BY_ASST_TEXT_MSG = "notifyByAsstTextMsg";
//	private static final String NOTIFY_FOR_ALERTS = "notifyForAlerts";
//	private static final String NOTIFY_FOR_SCRIPT_CHANGES = "notifyForScriptChanges";
//	private static final String NOTIFY_FOR_NEW_TASK = "notifyForNewTask";
//	private static final String NOTIFY_FOR_OVERDUE_TASK = "notifyForOverdueTask";
//	private static final String SEND_CALLSHEET = "sendCallsheet";
//	private static final String CALLSHEET_VERSION = "callsheetVersion";
//	private static final String SEND_DPR = "sendDpr";
//	private static final String SEND_DIRECTIONS = "sendDirections";
//	private static final String SEND_STRIPBOARD = "sendStripboard";
//	private static final String SEND_ADVANCE_SCRIPT = "sendAdvanceScript";
//	private static final String SCRIPT_FULL_PAGE = "scriptFullPage";
//	private static final String SCRIPT_DIALOGUE_ONLY = "scriptDialogueOnly";
//	private static final String HOLD_ALLOWED = "holdAllowed";
//	private static final String DROP_PICKUP_ALLOWED = "dropPickupAllowed";
//	private static final String DROP_TO_USE = "dropToUse";
//	private static final String DAYS_HELD_BEFORE_DROP = "daysHeldBeforeDrop";

	private static final SelectItem contactListHead = new SelectItem("-1", Constants.SELECT_HEAD_NONE);

	public static ContactDAO getInstance() {
		return (ContactDAO)getInstance("ContactDAO");
	}

	@SuppressWarnings("unchecked")
	public List<Contact> findByPropertyProduction(String propertyName, Object value) {
		String query = "from Contact where " +
				PRODUCTION + " = ? and " +
				propertyName + " = ?";
		Object values[] = { SessionUtils.getProduction(), value };
		return find(query, values);
	}

	/**
	 * Find all the Contacts belonging to the given Production that have
	 * an active status (pending or accepted).
	 * @param prod The Production of interest.
	 * @return A non-null, but possibly empty, List of Contacts.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByProductionActive(Production prod) {
		String queryString = "from Contact c where " +
				PRODUCTION + " = ? " +
				" and c.status in " + MemberStatus.sqlActiveList();
		return find(queryString, prod);
	}

	/**
	 * Find all the Contacts belonging to the given Project that have an active
	 * status (pending or accepted).  This includes Contacts that only have
	 * production-wide roles.
	 *
	 * Split the query to retrieve production-wide roles from project member roles
	 * to optimize performance and to make sure all records are retrieved. Before
	 * splitting one of the records was not being retrieved. Not sure why??
	 *
	 * @param project The Project of interest; must NOT be null.
	 * @return A non-null, but possibly empty, List of Contacts.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByProjectActive(Project project) {
		// Production-wide roles
		String queryString = "select distinct c from Contact c, ProjectMember pm, Unit u " +
				" where pm.employment.contact = c " +
				" and c.production = ? " +
				" and pm.unit IS NULL";

		List<Contact> pm1 = find(queryString, project.getProduction());

		// Crew roles
		Object[] values = {project.getProduction(), project};
		queryString = "select distinct c from Contact c, ProjectMember pm, Unit u " +
				" where pm.employment.contact = c " +
				" and c.production = ? " +
				" and pm.unit = u and u.project = ?";
		List<Contact> pm2 = find(queryString, values);
		// Someone with LS Data Admin role may also have a crew role so we
		// need to remove any duplicates.
		for(Contact ct : pm1) {
			if(pm2.contains(ct)) {
				pm2.remove(ct);
			}
		}
		pm1.addAll(pm2);

		return pm1;
	}

	/**
	 * Find all the productions that a User belongs to which are not in
	 * "Deleted" status, and in which the Contact is not DELETED either. This is
	 * used by the My Productions page.
	 *
	 * @param user The user whose productions are to be found.
	 * @return A non-null, but possibly empty, list of Productions as described
	 *         above.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByUserActive(User user) {
		String query = "from Contact where " +
				"status <> 'DELETED' and " +
				PRODUCTION + ".status <> 'DELETED' and " +
				USER + " = ? ";
		return find(query, user);
	}

	/**
	 * Find the Contact associated with the given User and the given Production.
	 * No more than one Contact should match such criteria.
	 *
	 * @param user The User in question.
	 * @param prod The Production to which the Contact must belong.
	 * @return The matching Contact, or null if no such Contact exists.
	 */
	@SuppressWarnings("unchecked")
	public Contact findByUserProduction(User user, Production prod) {
		String queryString = "from Contact where " +
				USER + " = ? and " +
				PRODUCTION + " = ? ";
		Object values[] = {user, prod};
		List<Contact> list = find(queryString, values);
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * Find all Contact`s who have a Role in the given Production (or Project)
	 * where that Role is in one of the given Department`s (specified by their
	 * database id values).
	 *
	 * @param idList A Collection of Department.id values.
	 * @param prod The Production of interest.
	 * @param project The Project of interest.
	 * @return A non-null, but possibly empty, List of distinct Contact`s, where
	 *         each one has a Role in the given Production that is associated
	 *         with one of the Department`s identified in the given list of ids.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByDepartmentIds(Collection<Integer> idList, Production prod, Project project) {
		try {
			if (idList == null || idList.size() == 0) {
				return new ArrayList<>();
			}
			String deptIdList = "(";
			for (Integer id : idList) {
				deptIdList += id + ",";
			}
			deptIdList = deptIdList.substring(0,deptIdList.length()-1) + ")";

			String query;
			List<Contact> contacts;
			// Get the list of Contacts based on production or project roles
			if (project == null) {
				query = "select distinct c from ProjectMember pm, Contact c " +
						"where pm.employment.role.department.id in " +
						deptIdList +
						" and pm.employment.contact = c " +
						" and pm.employment.contact.production = ? ";
				contacts = find(query, prod);
			}
			else {
				query = "select distinct c from ProjectMember pm, Contact c, Unit unit " +
						"where pm.employment.role.department.id in " +
						deptIdList +
						" and pm.employment.contact = c " +
						" and ( (pm.unit is null and pm.employment.contact.production = ?) " +
						"   or (pm.unit = unit and unit.project = ?) )";
				Object[] values = {prod, project};
				contacts = find(query, values);
			}
			return contacts;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Find all Contact`s who have a Role in the given Production (or Project)
	 * where that Role is in one of the given Department`s (specified by their
	 * database id).
	 *
	 * @param id Department id.
	 * @param prod The Production of interest.
	 * @param project The Project of interest.
	 * @return A non-null, but possibly empty, List of distinct Contact`s, where
	 *         each one has a Role in the given Production that is associated
	 *         with one of the Department`s identified in the given id.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByDepartmentId(Integer id, Production prod, Project project) {
		try {
			if (id == null) {
				return new ArrayList<>();
			}
			String query;
			List<Contact> contacts;
			// Get the list of Contacts based on production or project roles
			if (project == null) {
				query = "select distinct c from ProjectMember pm, Contact c " +
						"where pm.employment.role.department.id = " +
						id +
						" and pm.employment.contact = c " +
						" and pm.employment.contact.production = ? ";
				contacts = find(query, prod);
			}
			else {
				query = "select distinct c from ProjectMember pm, Contact c, Unit unit " +
						"where pm.employment.role.department.id = " +
						id +
						" and pm.employment.contact = c " +
						" and ( (pm.unit is null and pm.employment.contact.production = ?) " +
						"   or (pm.unit = unit and unit.project = ?) )";
				Object[] values = {prod, project};
				contacts = find(query, values);
			}
			return contacts;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Find a contact by a "display" style name, which is presumed to be either
	 * "[first-name] [last-name]" or "[last-name], [first-name]".
	 *
	 * @param displayName The name to match.
	 * @return A (possibly empty) List of Contacts. It may contain more than one
	 *         entry if two Contacts have the same first and last names.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findByDisplayName(String displayName) {
		Object values[] = { SessionUtils.getProduction(), displayName };
		String queryString;
		queryString = "select c from Contact c, User u where c.production = ? and " +
				"c.user = u and ";
		if (displayName.indexOf(',') > 0) {
			queryString += " concat(u.lastName, ', ', u.firstName) = ? ";
		}
		else {
			queryString += " concat(u.firstName, ' ', u.lastName) = ? ";

		}
		List<Contact> list = find(queryString, values);
		if (list.size()==0) {
			log.warn("find of displayName `" + displayName + "` failed.");
		}
		return list;
	}

	public Contact findByAccountNumAndProduction(String accountNum, Production prod) {
		String queryString = "from Contact c " +
				" where c.user.accountNumber = ? " +
				" and c.production = ? ";
		Object values[] = { accountNum, prod };
		return findOne(queryString, values);
	}

	/**
	 * Create a List of active Contact`s that should be visible to a user, based on the
	 * supplied "showXxxx" flags, from the ProjectMember`s belonging to the given
	 * Project.  Only active Contacts are included.  This includes Contact`s with
	 * production-wide roles.
	 *
	 * @param project The Project from which the list should be generated.
	 * @param showCast If true, the user is allowed to see members with
	 *            cast-type roles.
	 * @param showCrew If true, the user is allowed to see members with
	 *            crew-type roles.
	 * @param showAdmin If true, the user is allowed to see members with
	 *            admin-type roles.
	 * @return A non-null, but possibly empty, List of Contacts.
	 */
	public List<Contact> findByProjectActive(Project project,
			boolean showCast, boolean showCrew, boolean showAdmin) {
		log.debug("showCast : " + showCast + ", showCrew : " + showCrew + ", showAdmin : " + showAdmin);

		Map<String, Object> values = new HashMap<>();
		values.put("production", project.getProduction());
		List<Employment> employments;
		// Include null projects to get admin employments (which are "cross-project")
		if (project.getProduction().getType().isAicp()) { // Commercial
			values.put("project", project);
			employments = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_OR_NULL_ACTIVE, values);
		}
		else if (project.getProduction().getType().getEpisodic())  { // TV-episodic
			// For TV, first get production-related cross-project employments
			employments = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_CROSS_PROJECT_ACTIVE, values);
			values.clear();
			// Then add project-specific employments
			values.put("project", project);
			employments.addAll(EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PROJECT_MEMBER_ACTIVE, values));
		}
		else {
			// For Feature, just get production-related employments
			employments = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_FEATURE_ACTIVE, values);
		}

		List<Contact> contactList = new ArrayList<>(employments.size());
		Set<Contact> rejectedContacts = new HashSet<>();
		Integer currentContactId = SessionUtils.getCurrentContact().getId();
		// Note that if a Contact has any role (in the current project) which is a cast-type
		// role, then they are considered cast, not crew, for purposes of filtering.
		boolean admin;
		for (Employment emp : employments) {
			Contact contact = emp.getContact();
			//log.debug("Contact : " + contact.getDisplayName() + " Employment : " + emp.getOccupation());
			boolean changeContact = false;
			if (contact != null ) {
				admin = false;
				if (emp.getRole().isCast()) {
					contact.setIsCast(true);
					changeContact = true;
				}
				else if (emp.getRole().isAdmin()) {
					admin = true;
				}
				if (emp.getDefRole() && ! emp.getRole().equals(contact.getRole())) {
					if (emp.getProject() == null || (emp.getProject() != null && emp.getProject().equals(project))) {
						changeContact = true;
						contact.setRole(emp.getRole());
						contact.setRoleName(emp.getRole().getName());
					}
				}
				boolean show = (showCast && showCrew && showAdmin) ||  // do quick test first!
					(showCast && contact.getIsCast()) ||
					(showCrew && ! (contact.getIsCast() || admin)) ||
					(contact.getId().equals(currentContactId)); // user can see himself (#785)

				show &= contact.getActive();

				//log.debug("show : " + show);

				if (contactList.contains(contact) && show && changeContact) {
					// showing contact, but Role was not the default for some reason.
					//log.debug("");
					contactList.remove(contact);
					contactList.add(contact);
				}
				else if ( ! contactList.contains(contact)) {
					if (! rejectedContacts.contains(contact)) {
						//log.debug("");
						if (show) { // note: Role & roleName should be set to default already
							contactList.add(contact);
						}
						else {
							rejectedContacts.add(contact);
							log.debug("rejected=" + contact + ", role=" + emp.getRole());
						}
					}
					else {
						log.debug("");
						if (show) { // note: Role & roleName should be set to default already
							contactList.add(contact);
							rejectedContacts.remove(contact);
							log.debug("rejected Contact is restored = " + contact + ", role=" + emp.getRole());
						}
					}
				}
				else if ( ! show) { // already included, but not valid due to another role
					// Note that this will prevent users with LS Admin role from appearing in the C&C list
					// when viewed by a non-LS-Admin user, even if those users have other (non-admin) roles.
					// That's OK; it's been that way since at least v2.9.
					log.debug("");
					contactList.remove(contact);
					rejectedContacts.add(contact);
					log.debug("removed=" + contact + ", role=" + emp.getRole());
				}
			}
		}
		return contactList;
	}

	/**
	 * Find the Contact (actor) who is assigned to play a particular
	 * character (ScriptElement).  The 'link' status must be "SELECTED"
	 * for the Contact to be returned.
	 * @param character - The ScriptElement of the character.
	 * @return The Contact representing the actor selected to play the
	 * specified character.
	 */
	public Contact findContactFromCharacter(ScriptElement character) {
		Contact contact = null;
		log.debug("character id="+character.getId());
		try {
			String queryString = "select c " +
					"from Contact c, RealLink rl, RealWorldElement re " +
					"where rl.scriptElement = ? " +
					"and  rl.status = 'SELECTED' " +
					"and rl.realWorldElement = re " +
					"and re.actor = c " +
					"and (c.status in " + MemberStatus.sqlActiveList() + " )";
			/* Equivalent SQL:
			 * Select c.* from Contact c, Real_Link rl, real_world_element re
					where rl.script_element_id=102
					and  rl.status = 'SELECTED'
					and rl.real_element_id=re.id
					and re.contact_id = c.id;
			 */
			contact = findOne(queryString, character);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
		return contact;
	}

	/**
	 * Create a sorted List of SelectItem`s matching the supplied list of
	 * Contact objects. The label will be in "last-name, first-name" style, and
	 * the value will be the Contact.id field.
	 *
	 * @param contactList A non-null List of Contact objects.
	 * @param addTopItem If true, a "none selected" item will be added to the
	 *            top of the selection list.
	 * @return A non-null List of SelectItems. There will be one entry for each
	 *         entry in the 'contactList' parameter, plus an additional
	 *         "heading" entry if 'addTopItem' is true.
	 */
	public List<SelectItem> createContactSelectList(List<Contact> contactList, boolean addTopItem) {
		Collections.sort(contactList);
		List<SelectItem> contactDL = new ArrayList<>(contactList.size());
		if (addTopItem) {
			contactDL.add(contactListHead);
		}
		if (contactList != null) {
			for (Contact contact : contactList) { /* DropDown List for the contact */
				contactDL.add(new SelectItem(contact.getId(), contact.getUser().getLastNameFirstName()));
			}
		}
		return contactDL;
	}

	/**
	 * Create a Set of all active Contact`s in the Production which should be visible
	 * to a user based on the supplied 'show' flags.
	 *
	 * @param showCast If true, the user is allowed to see members with
	 *            cast-type roles.
	 * @param showCrew If true, the user is allowed to see members with
	 *            crew-type roles.
	 * @param showAdmin If true, the user is allowed to see members with
	 *            admin-type roles.
	 * @return A non-null, but possibly empty, Set of Contact`s.
	 */
	public Set<Contact> createAllContactsSet(boolean showCast, boolean showCrew, boolean showAdmin) {
		Set<Contact> contacts = new HashSet<>();
		if (showCast) {
			List<Contact> cast = findCastOnly();
			for (Contact contact : cast) {
				contact.setIsCast(true); // set transient
			}
			contacts.addAll(cast);
		}
		if (showCrew) {
			List<Contact> crew = findCrew(showAdmin);
			// "non-production" crew: - never used
//			for (Contact contact : crew) {
//				if (contact.getDepartment() != null &&
//						contact.getDepartment().getId().equals(Constants.DEPARTMENT_ID_VENDOR)) {
//					contact.setIsNonProd(true);
//				}
//			}
			contacts.addAll(crew);
		}
		log.debug("set=" + contacts.size());
		return contacts;
	}

	/**
	 * Find all active contacts who do not have any role in the Cast department
	 * or the LS Admin department; unless 'includeAdmin' is true, in which case
	 * Contacts with LS Admin roles will be allowed.
	 *
	 * @param includeAdmin If true, Contacts with roles in the LS Admin
	 *            department will not be excluded from the results.
	 *
	 * @return A non-null, but possibly empty, List of Contact`s who are active
	 *         members of the Production, and do not have any Role assigned to
	 *         the Cast department. if 'includeAdmin' is false, a Contact with
	 *         any Role assigned to the LS Admin department will also be
	 *         excluded.
	 */
	@SuppressWarnings("unchecked")
	public List<Contact> findCrew(boolean includeAdmin) {
		String queryString =
			"select c from Contact c where c.production = ? " +
				" and c.status in " + MemberStatus.sqlActiveList() +
				" and c.id not in " +
					" (select c.id from Contact c, ProjectMember pm, Role r " +
					" where pm.employment.contact = c and pm.employment.role = r and " +
					" c.production = ? and ";

		if (includeAdmin) {
			queryString += " r.department.id = " + Constants.DEPARTMENT_ID_CAST + ")";
		}
		else {
			queryString += " r.department.id in (" +
			Constants.DEPARTMENT_ID_CAST + "," + Constants.DEPARTMENT_ID_LS_ADMIN + " ) )";
		}
		Production prod = SessionUtils.getProduction();
		Object[] values = {prod, prod};
		return find(queryString, values);
	}

	/**
	 * Find all active Contacts who have at least one Role in any project that
	 * is considered a cast-type role.  This would include production-wide Cast
	 * roles, if that circumstance existed -- but we're not planning on allowing
	 * that for now.
	 *
	 * @return A non-null, but possibly empty, List of Contact`s who are active
	 *         members of the Production, and who have at least one Role
	 *         assigned to the "Cast" department.
	 */
	@SuppressWarnings("unchecked")
	private List<Contact> findCastOnly() {
		String queryString = "select distinct pm.employment.contact from ProjectMember pm " +
				" where pm.employment.role.department.id = " + Constants.DEPARTMENT_ID_CAST +
				" and pm.employment.contact.production = ? ";
		return find(queryString, SessionUtils.getProduction());
	}

	/**
	 * Find the number of Contacts belonging to the given Production that have
	 * an active status (pending or accepted) not including those with LS Admin
	 * role.
	 *
	 * @param prod The Production of interest.
	 * @return The (non-negative) number of such contacts.
	 */
	@SuppressWarnings("unchecked")
	public int countByProductionActiveNonAdmin(Production prod) {
		int count = -1;
		Role admin = new Role();
		admin.setId(Constants.ROLE_ID_LS_ADMIN);
		Object[] values = {prod, admin};

		String queryString = "select count(*) from Contact c where " +
				PRODUCTION + " = ? " +
				" and role <> ? " +
				" and status in " + MemberStatus.sqlActiveList();
		List<Long> list = find(queryString, values);
		if (list.size() > 0) {
			count = list.get(0).intValue();
		}
		return count;
	}

	/**
	 * Determine if the given Contact has any "critical" resources, in which
	 * case we cannot delete it from the database.  Currently there are no
	 * "critical" resources.
	 *
	 * @param contact
	 * @return True.
	 *         <p/>
	 *         All contacts are currently considered removeable, since a
	 *         "remove" only marks the Contact record as "DELETED" status, but
	 *         leaves it in the database.
	 */
	public boolean isContactRemoveable(Contact contact) {
		log.debug("");
//		if (TimeCardDAO.getInstance().existsContact(contact)) {
//			return false;
//		}
//		if (MessageInstanceDAO.getInstance().existsContact(contact)) {
//			return false;
//		}
//		User user = contact.getUser();
//		if (user != null) {
//			if (NoteDAO.getInstance().existsUser(user)) {
//				return false;
//			}
//			if (StripboardDAO.getInstance().existsUser(user)) {
//				return false;
//			}
//		}
		return true;
	}

	/**
	 * Add or merge the supplied Contact, after first updating the Image objects so
	 * they are associated with the given Contact.
	 *
	 * @param contact The Contact to be saved or updated.
	 * @param images The Collection of images to associate with the Contact.
	 * @return The updated Contact
	 */
	@Transactional
	public Contact update(Contact contact, Collection<Image> images) {
		if (images != null) {
			for (Image image : images) {
				image.setContact(contact);
			}
		}
		if (contact.getId() == null) {
			save(contact);
		}
		else {
			contact = merge(contact);
		}
		return contact;
	}

	/**
	 * Attach a new or updated instance of a Contact and the associated User
	 * object. Having the code together here guarantees that either both actions
	 * are done, or neither.
	 * <p>
	 * If the User has not been persisted yet (id is still null), then its
	 * fields are initialized assuming it is being "invited" to the application.
	 * Its status will be set to PENDING, a random password will be set, and a
	 * new Account number will be generated for it. Then
	 * DoNotification.inviteNewAccount will be called to generate an email to
	 * the user.
	 *
	 * @param contact The Contact being added or updated.
	 * @param user The User being added or updated.
	 * @param sendInvitation If true, an invitation email will be sent to the user.
	 * @param employments Will mostly be null except for Canada when adding multiple occupations
	 * for one project or adding a user to multiple projects through the Add Contact popup LS-2538
	 */
	@Transactional
	public void attachContactUser(Contact contact, User user, boolean sendInvitation, List<Employment> employments) {
		try {
			attachContactUserPrivate(contact, user, sendInvitation, employments); // processing common to public/private callers
			boolean hasCastRole = isCastOrStuntMember(contact);
			// may need to add/remove RWE based on cast status:
			boolean updated = updateCastRWE(contact, false, false, hasCastRole);
			if (updated) {
				attachDirty(contact);
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Get the MemberStatus from the database for the specified contact, without
	 * retrieving the entire Contact. We don't want Hibernate to replace any
	 * existing Contact objects in the session cache.
	 *
	 * @param id The database id of the desired Contact.
	 * @return The Contact.status field from the specified Contact, or DELETED
	 *         if the Contact could not be loaded.
	 */
	@SuppressWarnings("unchecked")
	private MemberStatus findContactStatus(Integer id) {
		MemberStatus status = MemberStatus.DELETED;
		String query = "select c.status from Contact c where c.id = ?";
		List<MemberStatus> list = find(query, id);
		if (list != null && list.size() > 0) {
			status = list.get(0);
		}
		return status;
	}

	/**
	 * Called during Save processing to update role-related information in
	 * Contact and Department.
	 * <p>
	 * We also detect if the Contact has a RW Element Cast object associated,
	 * but no longer has any Cast role, in which case we delete the RWE Cast
	 * object.
	 *
	 * @param contact The Contact being updated.
	 * @param newEntry True if this is a newly-created Contact instance.
	 * @param employments The complete Collection of Employment instances to be
	 *            connected to the contact.
	 * @return The (possibly updated) Contact instance.
	 */
	@Transactional
	public Contact actionSave(Contact contact, boolean newEntry, Collection<Employment> employments) {
		try {
			boolean updated = false;
			if (employments == null || employments.size() == 0) {
				contact.setRole(null);
				contact.setStatus(MemberStatus.NO_ROLES);
			}
			if (! newEntry) {
				User user = contact.getUser();
				attachContactUserPrivate(contact, user, false, employments);
			}
			// figure out transient role-name value
			contact.setRoleName(null); // old one may be invalid
			boolean hasCastRole = false;

			// old code was patchwork from 2.9/3.0 issues.
			Collection<Employment> emps = employments;
			if (emps != null && emps.size() > 0) {
				Project project = SessionUtils.getCurrentProject();
				Role defaultRole = null;
				boolean isAdmin = AuthorizationBean.getInstance().isAdmin();
				//If selected contact has multiple employments in other project but no employment in current project.
				Role otherProjectDefRole = null;
			empLoop:
				for (Employment emp : emps) {
					if (! (emp.getProject() == null || emp.getProject().equals(project))) {
						// To store first def role of any other project to show in the contactlist if contact has no employment in current project.
						if (emp.getDefRole() && otherProjectDefRole == null) {
							otherProjectDefRole = emp.getRole();
						}
					}
					if (emp.getDefRole()) {
						for (ProjectMember pm : emp.getProjectMembers()) {
							// To hide admin def roles from non admin persons
							if (((isAdmin || (! emp.getRole().isAdmin())) && pm.getUnit() == null) ||
									pm.getUnit().getProject().equals(project)) {
								// have a cross-project PM, or matching current project...
								defaultRole = emp.getRole();
//								// For an unknown reason, defaultRole.getName() was returning null
//								// when processing a NEW contact with role information.
//								if (defaultRole.getName() == null) {
//									log.warn("null role name!");
//									defaultRole = RoleDAO.getInstance().findById(defaultRole.getId()); // get fresh copy
//								}
								if (defaultRole != null) {
									log.debug("default role found for current project: " + defaultRole.getName());
									break empLoop;
								}
							}
						}
					}
				}
				hasCastRole = isCastOrStuntMember(contact);
				if (defaultRole == null) { // none for current project, just pick the first
					if (otherProjectDefRole != null) {
						log.debug("default role found for Other project: " + otherProjectDefRole.getName());
						defaultRole = otherProjectDefRole;
					}
					else {
						defaultRole = emps.iterator().next().getRole();
					}
				}
				// If contact's "default role" doesn't match the found role, set it.
				if (defaultRole != null && ! defaultRole.equals(contact.getRole())) {
					contact.setRole(defaultRole);
					contact.setRoleName(defaultRole.getName());
					updated = true;
				}
			}
			updated = updateCastRWE(contact, newEntry, updated, hasCastRole);
			if (( ! newEntry) && updated) {
				log.debug("prior to attach");
				attachDirty(contact); // this attach used to fail in some cases; appears to be fixed. (10/27/16)
				log.debug("after attach");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return contact;
	}

	/**
	 * Add or update a User/Contact pair in the database. This method also logs
	 * the appropriate Change records, and issues an invitation, if either the
	 * contact or user is a new object (not already saved in the database).
	 * <p>
	 * NOTE! For an existing Contact, this code PREVENTS the Contact.status
	 * field from being set back to PENDING. This avoids user A, who is editing
	 * the contact for user B, from overwriting status changes made BY user B
	 * accepting or declining an invitation while user A was in the middle of
	 * the edit process. (See Bugzilla #713(rev2486) and rev 2628.)
	 *
	 * @param contact The Contact being created or updated.
	 * @param user The User being created or updated.
	 * @param sendInvitation If true, send an invitation email to the new user.
	 * @param list
	 */
	private void attachContactUserPrivate(Contact contact, User user, boolean sendInvitation, Collection<Employment> emps) {
		boolean newUser = false;
		DoNotification notify = DoNotification.getInstance();
		contact.setUser(user);
		contact.setProduction(SessionUtils.getProduction());
		if (user.getId() == null) {
			newUser = true;
			user.setStatus(UserStatus.PENDING);
			user.setPassword(PasswordResetBean.createPassword());
			user.setNewPasswordRequired(true);
			user.setEmailAddress(contact.getEmailAddress());
			user = UserDAO.getInstance().update(user, null);
			contact.setUser(user);
			ChangeUtils.logChange(ChangeType.USER, ActionType.CREATE, contact.getProject(),
					user, "New User account (invited)");
			if (sendInvitation) {
				notify.inviteNewAccountFirst(contact); // send new Account invite
			}
		}

		ActionType action;
		if (contact.getId() == null) {
			action = ActionType.CREATE;
			contact.setCreatedBy(SessionUtils.getCurrentUser().getAccountNumber());
		}
		else {
			action = ActionType.UPDATE;
			if (contact.getStatus() == MemberStatus.PENDING) {
				// Don't let Save accidentally set a status back to PENDING,
				// when the user might have just accepted or declined an invitation & changed the status.
				// See revs 2486, 2628, and 3760.
				MemberStatus status = findContactStatus(contact.getId());
				if (status == MemberStatus.ACCEPTED || status == MemberStatus.BLOCKED
						|| status == MemberStatus.DECLINED) {
					contact.setStatus(status); // keep existing status
				}
			}
		}

		if (emps != null) {
			contact.getEmployments().clear();
			for (Employment emp : emps) {
				if (emp.getId() == null) {
					save(emp);
				}
				else {
					attachDirty(emp);
				}
				contact.getEmployments().add(emp);
			}
		}

		// this seems to fix duplicate instance problem with StartForms
		List<StartForm> list = contact.getStartForms();
		for (int i = 0; i < list.size(); i++) {
			list.set(i, StartFormDAO.getInstance().refresh(list.get(i)));
		}

		/* rev 2.9.5318
		 * Somehow we are ending up with 2 Department objects for the same
		 * key (id); at least that's the error that Hibernate threw. Note that
		 * if the @Cascade() setting on Role.getDepartment() was removed,
		 * this error did not occur!!  Anyway, the following code fixed the
		 * "different object with the same identifier" error -- the goal is
		 * to ensure that all Department's with the same id are the same object.
		 */
		Map<Integer,Department> dps = new HashMap<>();
		for (Employment emp : contact.getEmployments()) {
			Integer id = emp.getRole().getDepartment().getId();
			if (dps.containsKey(id)) {
				emp.getRole().setDepartment(dps.get(id));
			}
			else {
				dps.put(id,emp.getRole().getDepartment());
			}
		}
		dps.clear();
		attachDirty(contact);
		ChangeUtils.logChange(ChangeType.CONTACT, action, contact.getProject(),
				user, "Add/update Contact");
		if (sendInvitation && action == ActionType.CREATE && ! newUser) { // new Contact created for old user
			notify.inviteContact(contact);
		}
		if (newUser) { // New user account created by a 3rd party
			notify.userCreated(user, false);
		}
	}

	/**
	 * @param contact The Contact whose related RealWorldElement should be
	 *            created or deleted.
	 * @param newEntry True if this is a new Contact which has not yet been
	 *            saved to the database. When this is true, and the Contact has
	 *            a cast role, the matching RealWorldElement will be
	 *            instantiated and linked to the Contact object, but will not be
	 *            saved to the database.
	 * @param updated A boolean value which affects the return value.
	 * @param hasCastRole True if this Contact has a role within the Cast
	 *            department.
	 * @return True if a change was made to the Contact object, or if the
	 *         'updated' parameter was true.
	 */
	private boolean updateCastRWE(Contact contact, boolean newEntry, boolean updated,
			boolean hasCastRole) {
		if (contact.getCastMember() == null) {
			if (hasCastRole) {
				// need to add cast member (RWE) object if user has a cast role
				RealWorldElement rwe = createCastMember(contact);
				updated = true;
				moveImages(contact, rwe);
				if (! newEntry) {
					save(rwe);
				}
			}
		}
		else if (! hasCastRole) {
			// user has RWE Cast object, but no longer has a Cast role; delete the RWE
			RealWorldElement elem = contact.getCastMember();
			contact.setCastMember(null);
			elem.setActor(null); // disconnect from Contact
			if (elem.getImages() != null) {
				List<Image> images = elem.getImages();
				elem.setImages(null);
				for (Image image : images) {
					image.setContact(contact);
					image.setRealWorldElement(null);
				}
				contact.setImages(images);
			}
			else {
				contact.setImages(new ArrayList<Image>(0));
			}
			updated = true;
			delete(elem);
		}
		return updated;
	}

	/**
	 * Determine if the given Contact has a Cast role. This includes any role
	 * that is a Cast or Stunt role; or any custom role in either the Cast
	 * SYSTEM department or a department equivalent to the Cast System
	 * Department.
	 * <p>
	 * This is used to determine whether the Contact needs to have a related
	 * RealWorldElement (Production element).
	 *
	 * @param contact The Contact to be tested.
	 * @return True iff the Contact has at least one Role in a Cast department.
	 */
	private static boolean isCastOrStuntMember(Contact contact) {
		boolean hasCastRole = false;
		for (Employment emp : contact.getEmployments()) {
			if (emp.getRole().isCastOrStunt()) {
				hasCastRole = true;
				break;
			}
		}

		// The situation outlined in the code below cannot currently happen, because the "department id"
		// assigned to a role will be the equivalent SYSTEM department id if there is an
		// equivalent.  Currently there is no way to create a truly "custom Cast" department,
		// only equivalents of the SYSTEM one that are created automatically due to assigning
		// a time-keeper or Timecard Approver to the Cast department.  If, in the future, we allow
		// a user-created department to be marked as "Cast", then the code below should be
		// used to detect adding a custom role in such a department to a Contact.

//		if (! hasCastRole) {
//			// no Cast role found yet ... do another, more 'expensive' test,
//			// in case of a custom Cast department.
//			for (ProjectMember mbr : contact.getProjectMembers()) {
//				if (mbr.getDepartmentId() > Constants.DEPARTMENT_ID_NORMAL_LIMIT &&
//						mbr.getRole().getId() > Constants.ROLE_ID_NORMAL_LIMIT) {
//					// a custom role in a custom department ... need to check db to see if it's a Cast dept
//					Department dept = DepartmentDAO.getInstance().findById(mbr.getDepartmentId());
//					if (dept != null && dept.getStandardDeptId().equals(Constants.DEPARTMENT_ID_CAST)) {
//						hasCastRole = true;
//						break;
//					}
//				}
//			}
//		}
		return hasCastRole;
	}

	/**
	 * Determine if the given Contact has a Crew role, not including Stunt
	 * roles. This includes any role that is not a Cast or Stunt role or LS
	 * Admin.
	 * <p>
	 * This is used to determine whether the Contact needs to have a Timecard
	 * created for them.
	 *
	 * @param contact The Contact to be tested.
	 * @return True iff the Contact has at least one Role in a crew department.
	 */
	public static boolean isCrewNotStuntMember(Contact contact) {
		boolean hasCrewRole = false;
		for (Employment emp : contact.getEmployments()) {
			if (emp.getRole().isCrewNotStunt()) {
				hasCrewRole = true;
				break;
			}
		}
		return hasCrewRole;
	}

	/**
	 * If the supplied contact has Images, move them from the Contact's image
	 * collection to the RW Element's image collection.
	 * @param contact
	 * @param rwe
	 */
	public static void moveImages(Contact contact, RealWorldElement rwe) {
		if (contact.getImages() != null) {
			List<Image> rwImageList = new ArrayList<>();
			for (Image image : contact.getImages()) {
				image.setContact(null);
				image.setRealWorldElement(rwe);
				rwImageList.add(image);
			}
			contact.getImages().clear();
			contact.setImages(null);
			rwe.setImages(rwImageList);
		}
	}

	/**
	 * Instantiate a RealWorldElement to function as the corresponding
	 * production element to a Contact who has a Cast role in a project. The RWE
	 * created is NOT saved to the database.
	 *
	 * @param ct The Contact to be associated with the new RealWorldElement.
	 * @return The newly instantiated (transient) RealWorldElement.
	 */
	public static RealWorldElement createCastMember(Contact ct) {
		RealWorldElement rwe = new RealWorldElement();
		rwe.setProduction(SessionUtils.getProduction());
		rwe.setActor(ct);
		rwe.setName(ct.getUser().getLastNameFirstName());
		rwe.setType(ScriptElementType.CHARACTER);
		ct.setCastMember(rwe);
		return rwe;
	}

	/**
	 *
	 * @param contact
	 * @param projectMember
	 * @param sendInvitation If true, an invitation email will be sent to the
	 *            user.
	 *
	 * @return boolean true, if new employment is created else false.
	 */
	@Transactional
	public boolean reinviteContact(Contact contact, ProjectMember projectMember, boolean sendInvitation) {
		boolean createEmp = true;
		try {
			log.debug("ReInvite");
			Integer roleId = projectMember.getEmployment().getRole().getId();
			for (Employment oldEmp : contact.getEmployments()) {
				if (oldEmp.getRole().getId().equals(roleId)){
					log.debug("Employment matches old Employment");
					createEmp = false;
					break;
				}
			}
			for (Employment oldEmp : contact.getEmployments()) {
				if (contact.getEmployments().size() == 1 && (! createEmp)) {
					oldEmp.setDefRole(true);
				}
				evict(oldEmp.getRole()); // avoid Duplicate instance exceptions...
				evict(oldEmp);
			}
			if (createEmp) {
				log.debug("Create New Employment");
				projectMember.getEmployment().setContact(contact);
				List<ProjectMember> members = new ArrayList<>();
				members.add(projectMember);
				projectMember.getEmployment().setProjectMembers(members);
				if (projectMember.getUnit() == null) { // true if new role is cross-project (admin)
					projectMember.getEmployment().setProject(null);
				}
				else {
					projectMember.getEmployment().setProject(projectMember.getUnit().getProject());
				}
				if (contact.getEmployments().isEmpty()) {
					projectMember.getEmployment().setDefRole(true);
				}
				attachDirty(projectMember.getEmployment());
				attachDirty(projectMember);
				contact.getEmployments().add(projectMember.getEmployment());
			}
			attachDirty(contact);
			ChangeUtils.logChange(ChangeType.CONTACT, ActionType.UPDATE, contact.getProject(),
					contact.getUser(), "Re-invite Contact");
			if (sendInvitation) {
				DoNotification.getInstance().inviteContact(contact);
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return createEmp;
	}

	/**
	 * Remove VISIBLE evidence of a Contact, and it's associated
	 * objects/relationships -- ProjectMember, ReportRequirement, etc. We don't
	 * actually delete the Contact object, as there are associated items we want
	 * to retain, such as TimeCard`s, Note`s, and MessageInstance`s. This method
	 * is used for both deleting a Contact via the Cast&Crew page, and also for
	 * a person declining an invitation to a Production.
	 * <p>
	 * If at least one WeeklyTimecard exists for the contact, then all of its
	 * StartForm`s will be retained. If no timecards exist, then all StartForm`s
	 * will be deleted.
	 *
	 * @param contact The Contact to be "removed"; the status value should
	 *            already be updated to either DELETED or DECLINED.
	 */
	@Transactional
	public void removeContact(Contact contact) {
		log.debug("");
		// Remove this contact as an assistant to anyone else
		List<Contact> asstFor = findByProperty(ASSISTANT, contact);
		for (Contact c : asstFor) {
			c.setAssistant(null);
			attachDirty(c);
		}
		if (contact.getReportRequirements() != null) {
			// remove as Responsible Party in ReportRequirements
			for (ReportRequirement rr : contact.getReportRequirements()) {
				rr.setContact(null);
				attachDirty(rr);
			}
			contact.getReportRequirements().clear();
		}
		if (contact.getCastMember() != null) {
			RealWorldElementDAO rwDAO = RealWorldElementDAO.getInstance();
			rwDAO.remove(contact.getCastMember()); // this should un-link SEs & delete RWE.
			contact.setCastMember(null);
		}
		if (contact.getManagerFor() != null) {
			// remove as Manager of any RWEs
			for (RealWorldElement rwe : contact.getManagerFor()) {
				rwe.setManager(null);
				attachDirty(rwe);
			}
			contact.getManagerFor().clear();
		}
		if (contact.getScriptElements() != null) {
			// remove as "responsible party" of any Script Elements
			for (ScriptElement se : contact.getScriptElements()) {
				se.setContact(null);
				attachDirty(se);
			}
			contact.getScriptElements().clear();
		}

		Iterator<Employment> iter = contact.getEmployments().iterator();
		while (iter.hasNext()) {
			Employment emp = iter.next();
			log.debug(" EMPLOYMENT: " + emp.getId() + ", "+ emp.getOccupation());
			if (StartFormDAO.getInstance().existsTimecardsForEmployment(emp)) {
				log.debug("Continue");
				continue;
			}
			log.debug("PASS");
			if (contact.getProduction().getAllowOnboarding()) {
				// Check if ContactDocuments exists
				boolean hasDocs = ContactDocumentDAO.getInstance().existsProperty("employment", emp);
				if (! hasDocs) {
					log.debug(" ");
					deleteEmployment(emp);
					iter.remove();
				}
			}
			else {
				log.debug(" ");
				deleteEmployment(emp);
				iter.remove();
			}
		}
		// Don't remove Contact; caller marked it either Deleted or Declined
		attachDirty(contact);
		// These related objects are retained:
		// 		Address, ContractState, MessageInstance, Note, TimeCard
		if (contact.getStatus() == MemberStatus.DELETED) {
			ChangeUtils.logChange(ChangeType.CONTACT, ActionType.DELETE, "delete Contact=" + contact.getDisplayName());
		}
		else {
			ChangeUtils.logChange(ChangeType.CONTACT, ActionType.UPDATE, "declining Contact=" + contact.getDisplayName());
		}
	}

	/**
	 * Method deletes the associated StartForms, project members and employment instances
	 *
	 * @param emp Employment to be deleted.
	 */
	private void deleteEmployment(Employment emp) {
		log.debug("Deleting Employment : " + emp.getId() + ", " + emp.getOccupation());
		emp = EmploymentDAO.getInstance().refresh(emp);
		List<StartForm> startFormList = new ArrayList<>();
		startFormList =  StartFormDAO.getInstance().findByProperty("employment", emp);
		for (StartForm sf : startFormList) {
			delete(sf);
		}
		for (ProjectMember pm : emp.getProjectMembers()) {
			delete(pm);
		}
		delete(emp);
	}

	/**
	 * Remove StartForm`s related to the given Contact and Project. The Project
	 * is only relevant for Commercial Productions. For a commercial production,
	 * if no timecards exist for the given Contact and Project, then all the
	 * StartForm`s for that Contact and Project will be deleted, otherwise none
	 * will be deleted. For a TV/Feature production, if no timecards exist for
	 * the given Contact, then all their StartForm`s (in that Production) will
	 * be deleted, otherwise none will be deleted.
	 *
	 * @param contact The Contact whose StartForm`s may be deleted.
	 * @param project The related Project, only used for commercial productions.
	 * @return A refreshed instance of the given Contact.
	 */
	@Transactional
	public Contact removeStarts(Contact contact, Project project) {
		contact = refresh(contact);
		Production prod = contact.getProduction();
		String acct = contact.getUser().getAccountNumber();
		if (contact.getProduction().getType().isAicp()) {
			if (! WeeklyTimecardDAO.getInstance().existsWeekEndDateAccountOccupation(prod, project, null, acct, null)) {
				Collection<StartForm> starts = StartFormDAO.getInstance().findByContactProject(contact, project, false);
				for (StartForm sf : starts) {
					delete(sf);
				}
			}
		}
		else {
			if (! WeeklyTimecardDAO.getInstance().existsUserAccount(prod, acct)) {
				Collection<StartForm> starts = StartFormDAO.getInstance().findByContact(contact);
				for (StartForm sf : starts) {
					delete(sf);
				}
			}
		}
//		attachDirty(contact);
		contact = refresh(contact);
		return contact;
	}

	/**
	 * Review each of the Contact`s in the List provided, and verify that
	 * their default Role and Department are still valid -- that the Contact
	 * still has that Role/Department in some project.
	 * @param contacts
	 */
	@Transactional
	public void updateRoles(List<Contact> contacts) {
		for (Contact cn : contacts) {
			cn = refresh(cn);
			boolean hasRole = false;
			List<Employment> emps = cn.getEmployments();
			if (cn.getRole() != null) {
				Integer roleId = cn.getRole().getId();
				for (Employment emp : emps) {
					if (emp.getRole().getId().equals(roleId)) {
						hasRole = true;
						break;
					}
				}
			}
			if (! hasRole) {
				Role role = null;
				Department dept = null;
				if (emps != null && emps.size() > 0) {
					role = emps.get(0).getRole(); // pick 'best' default role?
					dept = role.getDepartment();
				}
				cn.setRole(role);
				cn.setDepartment(dept);
				attachDirty(cn);
			}
		}
	}

//	public static ContactDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ContactDAO) ctx.getBean("ContactDAO");
//	}

}
