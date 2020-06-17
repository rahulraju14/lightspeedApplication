package com.lightspeedeps.web.login;

import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.PageFieldAccessDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.util.ApplicationScopeBean;

/**
 * This is the session-scoped bean responsible for loading and holding the Map
 * of page-field-access authorizations for a Contact. The pgFields Map is used
 * by all the JSP pages to determine which fields and controls should be
 * available to the current Contact.
 * <p>
 * Which keys are added to the {@link #pgFields} Map is determined by the
 * contact's permission settings, as well as attributes of the Production being
 * entered. The keys are maintained in the PageFieldAccess table.
 *
 * @see com.lightspeedeps.model.PageFieldAccess
 * @see com.lightspeedeps.type.AllowedMode
 */
@ManagedBean(name="authBean")
@SessionScoped
public class AuthorizationBean implements Serializable {

	/** for serialization */
	private static final long serialVersionUID = -6688866914783127030L;

	/**
	 * The pgFields authorization Map.
	 * Keys are page-field-access strings, e.g., "4.1,view_script".  The value that
	 * a key maps to is irrelevant; all that matters is the existence of the key
	 * in the Map.  For our usage, all values are the String "1".
	 */
	private final Map<String,String> pgFields = new HashMap<>(100);
	private static final Log log = LogFactory.getLog(AuthorizationBean.class);

	private static final long ONBOARDING_MASK = Permission.APPROVE_START_DOCS.getMask() | Permission.VIEW_ALL_DISTRIBUTED_FORMS.getMask()
			| Permission.MANAGE_START_DOC_APPROVERS.getMask() | Permission.MANAGE_START_DOCS.getMask();

	private static final long NO_ONBOARDING_MASK = ~ ONBOARDING_MASK;

	private static Boolean isBeta = null; // same across application instance

	private Integer currentContactId;
	private Integer currentProjectId;

	/** The list of ProjectMember`s for the current Contact and Project. */
	private transient List<ProjectMember> projectMembers;

	/** The permission mask for the current Contact and Project. */
	private long permissionMask;

	/** A List of the Unit's in the current Project in which the current Contact has
	 *  one or more Role's. */
	private transient List<Unit> units;

	/** true if any Role the Contact is assigned to in the current project is considered a "Cast"
	 * or "Stunt" role. */
	private boolean castOrStunt = false;

	/** true if any Role the Contact is assigned to in the current project is considered a "Crew" role,
	 * excluding Stunt roles. */
	private boolean crewNotStunt = false;

	/** true if any Role the Contact is assigned to in the current project is the "LS Admin" role. */
	private boolean admin = false;

	/** true if any Role the Contact is assigned to in the current project is the "LS Coordinator" role. */
	private boolean lsCoord;

	/** true if any Role the Contact is assigned to in the current project is the "Prod Data Admin" role. */
	private boolean dataAdmin = false;

	/** true if any Role the Contact is assigned to in the current project is the "Financial Data Admin" role. */
	private boolean financialAdmin = false;

	/** True if an LS Admin has turned on "pseudo approver" status, which gives them any rights usually
	 * associated with being a production approver, as well as acting as if any timecard being viewed is
	 * in their "queue".  This gives them the maximum flexibility to edit, create, and delete timecards. */
	private boolean pseudoApprover = false;

	/** True iff the Contact has at least one permission in the current project. Note that for timecard users,
	 * they may be able to use a production, and have 1 or 2 'special' page-field-keys set, without having any
	 * permissions. */
	private boolean hasPermissions = false;

	private final Set<Integer> roleIds = new HashSet<>();

	private Integer productionId;

	public AuthorizationBean() {
		log.debug("this=" + this + "**************************** AUTH BEAN ");
	}

	/**
	 * Returns the current instance of our bean. Note that this may not be
	 * available in a batch environment, in which case null is returned.
	 */
	public static AuthorizationBean getInstance() {
		log.debug("");
		AuthorizationBean bean = null;
		try {
			bean = (AuthorizationBean)ServiceFinder.findBean("authBean");
			if (isBeta == null) { // not yet set (first use), so initialize it
				isBeta = ApplicationScopeBean.getInstance().getIsBeta();
			}
		}
		catch (Exception e) {
		}
		return bean;

	}

	private void clear() {
		pgFields.clear();
		roleIds.clear();
		castOrStunt = false;
		crewNotStunt = false;
		admin = false;
		lsCoord = false;
		dataAdmin = false;
		financialAdmin = false;
		pseudoApprover = false;
		hasPermissions = false;
		units = null;
	}

	/**
	 * Load the pgFields authorization map for use by all the JSP pages. The list of
	 * page-field-access values is based on the Contact, and their Role in the current Project. If they
	 * have more than one Role assigned in the Project, they will get the "union" of all the
	 * permissions for those Roles.
	 * <p>
	 * This process happens once at login, and again any time the person switches to a different
	 * project.
	 * <p>
	 * If the current project is read-only, then Permissions marked with the "allows-write"
	 * attribute are skipped.
	 *
	 * @param contact The Contact whose page-field-access values are to be loaded.
	 */
	public void auth(Contact contact) {
		try {
			clear();
			if (contact != null) {
				log.debug("contact Id=" + contact.getId());
				Production prod = contact.getProduction();
				boolean onboarding = prod.getAllowOnboarding();
				boolean scripts = prod.getShowScriptTabs();	// LS-1327
				boolean prodWriteable = (prod.isWritable());
				if (contact.getProject() != null && contact.getLoginAllowed()) {
					boolean writeable = (contact.getProject().getStatus() == AccessStatus.ACTIVE);
					findMembers(contact, contact.getProject());
					if (projectMembers.size() > 0) {
						long permMask = contact.getPermissionMask();
						for (ProjectMember pm : projectMembers) {
							Role role = pm.getEmployment().getRole();
							roleIds.add(role.getId());
							permMask |= pm.getEmployment().getPermissionMask();
							if (role.isCrewNotStunt()) {
								crewNotStunt = true;
							}
							else if (role.isCastOrStunt()) {
								castOrStunt = true;
							}
							if (role.isLsAdmin()) {
								admin = true;
							}
							else if (role.isDataAdmin()) {
								dataAdmin = true;
							}
							else if (role.isFinancialAdmin()) {
								financialAdmin = true;
							}
							else if (role.isLsCoord()) {
								lsCoord = true;
							}
						}
						permMask = checkRemoveOnboarding(contact, permMask);
							// Generate a list of permission ids for our query
						String permIds = "( ";
						for (Permission per : Permission.createPermissionSet(permMask)) {
							if (prodWriteable || per.getStyle() != PermissionStyle.WRITE) {
								if (writeable || ! per.getAllowsWrite()) {
									permIds += per.getId() + ",";
								}
							}
						}
						permIds = permIds.substring(0, permIds.length()-1) + ")";
						boolean tours = prod.getType().isTours();
						boolean talent = prod.getType().isTalent();
						// Get all the PFA's matching any of the permission ids in our list
						List<PageFieldAccess> pf = PageFieldAccessDAO.getInstance().findByPermIds(permIds);
						for (PageFieldAccess pfs : pf) {
							if (pfs.getAllowedMode() == AllowedMode.ANY ||
									(pfs.getAllowedMode() == AllowedMode.ONBOARD && onboarding) ||
									(pfs.getAllowedMode() == AllowedMode.SCRIPTS && scripts) || 	// LS-1327
									(pfs.getAllowedMode() == AllowedMode.AGENCY && talent) || // Canada
									(pfs.getAllowedMode() == AllowedMode.TOURS && tours) ||
									(pfs.getAllowedMode() == AllowedMode.NONTOURS && ! tours) ||
									(pfs.getAllowedMode() == AllowedMode.BETA && isBeta) ) {
								pgFields.put(pfs.getPageField(), "1");
							}
						}
						if (pgFields.size() > 0) {
							hasPermissions = true;
							User user = contact.getUser();
							/*if (FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_SEAMLESS_INTEGRATION)) { // LS-3758
								if (SessionUtils.isShownEssMenu(user)) {
									pgFields.put(Constants.PGKEY_ESS, "1"); // give user access to ESS links/menus
								}
							}*/
							if (user.getShowUS()) { // LS-1658
								pgFields.put(Constants.PGKEY_ONLINE_US, "1");
							}
							if (user.getShowCanada()) { // LS-1658
								pgFields.put(Constants.PGKEY_ONLINE_CA, "1");
							}
						}
					}
				}
				if (! prod.isSystemProduction()) {
					pgFields.put(Constants.PGKEY_PRODUCTION, "1");
					if (onboarding) {
						pgFields.put(Constants.PGKEY_ALLOW_ONBOARDING, "1");
					}
				}
				if (prodWriteable) {
					pgFields.put(Constants.PGKEY_WRITABLE_PRODUCTION, "1");
				}
			}
			if (! hasPermissions) {
				log.warn("*************  NO field access keys for this Contact *** " +
						" Contact: " + (contact == null ? "null" : contact.getId()) +
						" project: " + ((contact == null || contact.getProject() == null) ? "null" : contact.getProject().getId()));
			}
			if (log.isDebugEnabled()) {
				log.debug("pgFields dump for " + (contact == null ? "null" : contact.getDisplayName()));
				Set<String> keys = new TreeSet<>(pgFields.keySet());
				String msg = "keys: ";
				for (String s : keys) {
					msg += s + "; ";
				}
				log.debug(msg);
			}
		}
		catch (Exception e) {
			//EventUtils.logError(e); // Use special case for auth:
			EventUtils.logEvent(EventType.APP_ERROR, SessionUtils.getProduction(),
					(contact == null ? null : contact.getProject()),
					(contact == null ? null : contact.getUser().getEmailAddress()),
					EventUtils.traceToString(e));
		}
	}

	/**
	 * Load pgFields authorization map, with optional forced refresh of the Contact's ProjectMembers
	 * information. See {@link #auth}.
	 *
	 * @param contact The Contact whose page-field-access values are to be loaded.
	 * @param refresh If true, read the current ProjectMembers data from the database; if false, a
	 *            cached copy may be used (if available).
	 */
	public void auth(Contact contact, boolean refresh) {
		if (refresh) {
			currentContactId = new Integer(-1); // force refresh of project membership
		}
		auth(contact);
	}

	/**
	 * Give a person authorization based on the fact that they have logged in to their
	 * user account, but not into any Production yet.  One of the auth() methods will
	 * be called once they select a Production.
	 * @param user
	 */
	public void loggedIn(User user, Contact contact) {
		clear();		// erase all authorizations
		productionId = null;
		pgFields.put(Constants.PGKEY_LOGGED_IN_ONLY, "1"); // this is all they need
		pgFields.put(Constants.PGKEY_ONLINE, "1");
		if (user.getShowUS()) { // LS-1658
			pgFields.put(Constants.PGKEY_ONLINE_US, "1");
		}
		if (user.getShowCanada()) { // LS-1658
			pgFields.put(Constants.PGKEY_ONLINE_CA, "1");
		}
		if (contact != null && contact.getProduction().isWritable()) {
			pgFields.put(Constants.PGKEY_WRITABLE_PRODUCTION, "1");
		}
		/*if (FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_SEAMLESS_INTEGRATION)) 
		{ // LS-3758
			if (SessionUtils.isShownEssMenu(user)) {
				pgFields.put(Constants.PGKEY_ESS, "1"); // give user access to ESS links/menus
			}
		}*/
	}

	/**
	 * Determine if the given Contact has the specified Permission, base on
	 * whatever role(s) they are assigned for any Unit in the given project.
	 *
	 * @param contact The Contact whose permission we are testing.
	 * @param project The Project within which the Contact must have the specified
	 *            Permission.
	 * @param permission The Permission we are looking for.
	 * @return True iff the given Contact has the given Permission within any
	 *         Unit of the given Project.
	 */
	public boolean hasPermission(Contact contact, Project project, Permission permission) {
		findMembers(contact, project);
		return hasPermission(permission);
	}

	/**
	 * Determine if the given Contact has the specified Permission, base on
	 * whatever role(s) they are assigned in the current Production, regardless
	 * of Project or Unit.
	 *
	 * @param contact The Contact whose permission we are testing.
	 * @param permission The Permission we are looking for.
	 * @return True iff the given Contact has the given Permission.
	 */
	public boolean hasPermission(Contact contact, Permission permission) {
		if (contact != null) {
			// To avoid LIE
			contact = ContactDAO.getInstance().refresh(contact);
			if ((! contact.getProduction().getAllowOnboarding()) && (permission.getMask() & ONBOARDING_MASK) != 0) {
				// Always return false for onboarding permissions if non-onboarding production
				return false;
			}
			List<Employment> empList = contact.getEmployments();
			for (Employment emp : empList) {
				if ((permission.getMask() & emp.getPermissionMask()) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	// Probably keep this one, altho' effect should be the same?
	public boolean hasPermission(Contact contact, Permission permission, Project project) {
		if (contact != null) {
			// To avoid LIE
			contact = ContactDAO.getInstance().refresh(contact);
			if ((! contact.getProduction().getAllowOnboarding()) && (permission.getMask() & ONBOARDING_MASK) != 0) {
				// Always return false for onboarding permissions if non-onboarding production
				return false;
			}
			List<Employment> empList = contact.getEmployments();
			for (Employment emp : empList) {
				if (emp.getProject() == null || emp.getProject().equals(project)) {
					if ((permission.getMask() & emp.getPermissionMask()) != 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determine if the current Contact has the specified Permission within
	 * the current project.
	 *
	 * @param permission The Permission we are looking for.
	 * @return True iff the given Contact has the given Permission within any
	 *         Unit of the current Project.
	 */
	public boolean hasPermission(Permission permission) {
		log.debug("perm=" + permission);
		return (permissionMask & permission.getMask()) != 0;
	}

	/**
	 * Determine if the current Contact has the Role with database id "roleId"
	 * in their list of assigned roles (in the current project).
	 *
	 * @param role
	 * @return True iff the current Contact has the role.
	 */
	public boolean hasRole(Role role) {
		boolean b = roleIds.contains(role.getId());
		log.debug(role.getId() + "=" + b);
		return b;
	}

	/**
	 * Refill 'projectMembers' if the Contact and/or project has changed from the last query,
	 * and update the permissionMask field to match.
	 * @param contact
	 * @param project
	 */
	private void findMembers(Contact contact, Project project) {
		// This could possibly use Employment instances directly instead of ProjectMember
		if (contact == null || project == null) {
			projectMembers = new ArrayList<>();
			currentContactId = new Integer(-1); // force refresh on next query
			permissionMask = 0;
		}
		else if (! contact.getId().equals(currentContactId) || ! project.getId().equals(currentProjectId)) {
			ProjectMemberDAO projectMemberDAO = ProjectMemberDAO.getInstance();
			projectMembers = projectMemberDAO.findByContactAndProject(contact, project);
			currentContactId = contact.getId();
			currentProjectId = project.getId();
			permissionMask = contact.getPermissionMask();
			for (ProjectMember pm : projectMembers) {
				permissionMask |= pm.getEmployment().getPermissionMask();
				pm.getEmployment().getRole().isCrewNotStunt(); // force lazy initialization. rev 4057
			}
			permissionMask = checkRemoveOnboarding(contact, permissionMask);
		}
		if (projectMembers == null) {
			projectMembers = new ArrayList<>();
			currentContactId = new Integer(-1); // force refresh on next query
			permissionMask = 0;
		}
	}

	/**
	 * @param permMask
	 * @return
	 */
	private long checkRemoveOnboarding(Contact contact, long permMask) {
		if (! contact.getProduction().getAllowOnboarding()) {
			//log.debug(Permission.toBinaryString(NO_ONBOARDING_MASK));
			//log.debug(Permission.toBinaryString(permMask));
			permMask &= NO_ONBOARDING_MASK;
			//log.debug(Permission.toBinaryString(permMask));
		}
		return permMask;
	}

	/**
	 * Construct a List of the Unit's in the current Project in which the
	 * current Contact has one or more Role's.  The List will not contain
	 * any duplicate entries.
	 *
	 * @return A possibly empty List of Unit's as specified above. Will not
	 *         return null.
	 */
	private List<Unit> findUnits() {
		Contact contact = SessionUtils.getCurrentContact();
		List<Unit> list = new ArrayList<>();
		if (contact.getProject().getHasUnits()) {
			findMembers(contact, contact.getProject());
			for (ProjectMember pm : projectMembers) {
				pm = ProjectMemberDAO.getInstance().refresh(pm);
				if (pm.getUnit() == null) { // production-wide PM
					list.clear();
					list.addAll(contact.getProject().getUnits());
					break;
				}
				else {
					if (! list.contains(pm.getUnit())) {
						list.add(pm.getUnit());
					}
				}
			}
		}
		else {
			list.add(contact.getProject().getMainUnit());
		}
		log.debug("unit count=" + list.size());
		return list;
	}

	/**
	 * Determine if the current Contact has a particular page-field-access key in
	 * their permissions map.
	 *
	 * @param key The key to look for, e.g., "2.1,edit". These are usually used
	 *            in JSP, and refer to a particular page (e.g., 2.1), and some
	 *            control or feature (e.g., "edit") within that page.
	 * @return True iff the Contact has the given page-field-access key.
	 */
	public boolean hasPageField(String key) {
		return pgFields.get(key) != null;
	}

	/** See {@link #pgFields}. */
	public Map<String, String> getPgFields() {
		return pgFields;
	}
	//public void setPgFields(Map<String, String> pgFields) {
	//	this.pgFields = pgFields;
	//}

	/** See {@link #units}. */
	public List<Unit> getUnits() {
		if (units == null) {
			units = findUnits();
		}
		return units;
	}

	/**
	 * @return true if this instance does not actually refer to any User/Contact.
	 */
	public boolean isEmpty() {
		return (currentContactId == null || currentContactId == 0) &&
				(productionId == null || productionId == 0) &&
				(pgFields == null || pgFields.size() == 0);
	}

	/** See {@link #castOrStunt}. */
	public boolean isCastOrStunt() {
		return castOrStunt;
	}

	/** See {@link #crewNotStunt}. */
	public boolean isCrewNotStunt() {
		return crewNotStunt;
	}

	/** See {@link #admin}. */
	public boolean isAdmin() {
		return admin;
	}

	/** See {@link #dataAdmin}. */
	public boolean isDataAdmin() {
		return dataAdmin;
	}

	/** See {@link #financialAdmin}. */
	public boolean isFinancialAdmin() {
		return financialAdmin;
	}

	/** See {@link #lsCoord}. */
	public boolean isLsCoord() {
		return lsCoord;
	}
	/** See {@link #hasPermissions}. */
	public boolean getHasPermissions() {
		return hasPermissions;
	}

	/** See {@link #pseudoApprover}. */
	public boolean getPseudoApprover() {
		return pseudoApprover;
	}
	/** See {@link #pseudoApprover}. */
	public void setPseudoApprover(boolean pseudoApprover) {
		this.pseudoApprover = pseudoApprover;
	}

	/** See {@link #productionId}. */
	public Integer getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(Integer productionId) {
		this.productionId = productionId;
	}

//	public Production getProduction() {
//		if (productionId == null) {
//			return null;
//		}
//		return ProductionDAO.getInstance().findById(productionId);
//	}

}
