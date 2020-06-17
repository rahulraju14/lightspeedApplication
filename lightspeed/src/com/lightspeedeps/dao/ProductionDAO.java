package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.service.OnboardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Production entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Production
 */

public class ProductionDAO extends BaseTypeDAO<Production> {
	private static final Log LOG = LogFactory.getLog(ProductionDAO.class);
	// property constants
	public static final String PROD_ID = "prodId";
	private static final String STUDIO = "studio";
	public static final String TITLE = "title";
	private static final String TYPE = "type";
	public static final String STATUS = "status";
	private static final String ORDER_STATUS = "orderStatus";
	private static final String OWNING_ACCOUNT = "owningAccount";
	private static final String SKU = "Sku";
//	private static final String MAX_PROJECTS = "maxProjects";
//	private static final String MAX_LOGON_ATTEMPTS = "maxLogonAttempts";
//	private static final String LOCK_OUT_DELAY = "lockOutDelay";
//	private static final String CONTACT_NAME = "contactName";
//	private static final String PHONE = "phone";
//	private static final String FAX = "fax";
//	private static final String TIME_ZONE = "timeZone";

	public static ProductionDAO getInstance() {
		return (ProductionDAO)getInstance("ProductionDAO");
	}

	public static ProductionDAO getFromApplicationContext(ApplicationContext ctx) {
		return (ProductionDAO) ctx.getBean("ProductionDAO");
	}

	/**
	 * Find a specific Production by its database id.
	 * <p>
	 * We don't use the superclass version, as we want to have more explicit
	 * control over error handling.  This routine is called in situations
	 * where we don't want the generic error handling, e.g., during Event
	 * LOG creation.
	 * @see com.lightspeedeps.dao.BaseTypeDAO#findById(java.lang.Integer)
	 */
	@Override
	public Production findById(java.lang.Integer id) {
		LOG.debug("getting Production instance with id: " + id);
		try {
			Production instance = (Production) getHibernateTemplate().get(
					"com.lightspeedeps.model.Production", id);
			return instance;
		}
		catch (DataAccessException e1) {
			// Skip EventUtil to avoid recursion in case db is not accessible
			LOG.error("Exception: ", e1);
			throw e1;
		}
		catch (RuntimeException e) {
			String msg = "exception fetching Production (id=" + id + "): " + e.getLocalizedMessage();
			// Use low-level logEvent to avoid recursion when EventUtils does getProduction()
			EventUtils.logEvent(EventType.APP_ERROR, null, null, null,
					msg + Constants.NEW_LINE + EventUtils.traceToString(e));
			LOG.error(msg, e);
			throw e;
		}
	}

	/**
	 *      Note that we don't use the normal findAll, since we need to do
	 *      special error handling to avoid a recursive error condition.
	 *
	 * @see com.lightspeedeps.dao.BaseTypeDAO#findAll()
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Production> findAll() {
		try {
			String queryString = "from Production";
			List list = getHibernateTemplate().find(queryString);
			LOG.debug("size=" + (list==null ? 0 : list.size()));
			return list;
		}
		catch (RuntimeException e) {
			String msg = "exception in Production.findAll: " + e.getLocalizedMessage();
			// Use low-level logEvent to avoid recursion when EventUtils does getProduction()
			EventUtils.logEvent(EventType.APP_ERROR, null, null, null,
					msg + Constants.NEW_LINE + EventUtils.traceToString(e));
			LOG.error(msg, e);
			throw e;
		}
	}

	/**
	 * Find a specific Production by its production_id, which is unique.
	 * @param prodId
	 * @return The matching Production, or null if not found.
	 */
	public Production findByProdId(String prodId) {
		return findOneByProperty(PROD_ID, prodId);
	}

	public List<Production> findByType(ProductionType type) {
		return findByProperty(TYPE, type);
	}

	/**
	 * Find all Production`s that a particular User "belongs to" -- all those
	 * containing a Contact which is linked to the given User, even if the
	 * Contact has a Deleted, Declined, etc. status. This list is used for
	 * timecard access, which is independent of the Contact's status within the
	 * Production.
	 *
	 * @param user The User in question.
	 * @return A possibly empty, but non-null, List of Production`s, sorted in
	 *         ascending title order, each of which contains a Contact
	 *         associated with the given User.
	 */
	@SuppressWarnings("unchecked")
	public List<Production> findByUser(User user) {
		String queryString = "select p from Production p, Contact c " +
				" where c.production = p and c.user = ? " +
				" order by p.title ";
		List<Production> list = find(queryString, user);
		return list;
	}

	/**
	 * @return A list of Productions which may need "report due" or "report overdue"
	 * notifications to be sent.  This is used by the scheduled process class, DueOverdueCheck.
	 * For a Production to qualify, it must have at least one "ReportRequirement" instance that
	 * has a non-null responsibleParty value.
	 */
	@SuppressWarnings("unchecked")
	public List<Production> findForReportDue() {
		String queryString =
				"select distinct p from Production p, Project pj, ReportRequirement rr " +
				" where p.status = 'ACTIVE' and p.notify = 1 and " +
					" pj.production = p and " +
					" pj.notifying = 1 and " +
					" rr.project = pj and " +
					" rr.contact is not null ";
		return find(queryString);
	}

	/**
	 * See if the payroll service associated with this production is a Team service
	 *
	 * @param prodId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Boolean findIsTeamPayroll(String prodId) {
		String queryString =
				"Select ps from Production prod, PayrollPreference pp, PayrollService ps " +
				" where prod.prodId = '" + prodId + "' and pp  = prod.payrollPref and ps = pp.payrollService";
		List<PayrollService> services = find(queryString);

		if(!services.isEmpty()) {
			return services.get(0).getTeamPayroll();
		}
		return false;
	}

	/**
	 * Find out how many productions are owned by the given user that have an
	 * OrderStatus of Free and are not Deleted.
	 *
	 * @param user The user whose productions we are looking for.
	 * @return The number of productions owned by the specified user (based on
	 *         their account number), which also have the specified OrderStatus.
	 *         The returned value is zero or greater.
	 */
	public int findCountOwnedFree(User user) {
		String queryString = "select count(id) from Production " +
				" where " + OWNING_ACCOUNT + " = ? " +
				" and " + ORDER_STATUS + " = ? " +
				" and " + SKU + " not like '%-ED-%' " +
				" and " + STATUS + " <> ? ";
		Object[] values = {user.getAccountNumber(), OrderStatus.FREE, AccessStatus.DELETED};
		int n = findCount(queryString, values).intValue();
		return n;
	}

	/**
	 * Find out how many productions are owned by the given user that have a SKU
	 * indicating an Educational type and are not Deleted.
	 *
	 * @param user The user whose productions we are looking for.
	 * @return The number of productions owned by the specified user (based on
	 *         their account number), which also have a SKU containing the
	 *         string '-ED-'. The returned value is zero or greater.
	 */
	public int findCountOwnedEducational(User user) {
		String queryString = "select count(id) from Production " +
				" where " + OWNING_ACCOUNT + " = ? " +
				" and " + SKU + " like '%-ED-%' " +
				" and " + STATUS + " <> ? ";
		Object[] values = {user.getAccountNumber(), AccessStatus.DELETED};
		int n = findCount(queryString, values).intValue();
		return n;
	}

	/**
	 * Create a new Production based on the supplied Product and name.
	 * Production settings, where applicable, are taken from the Product object.
	 * The start date will be set to today's date, and the owner is set to the
	 * current User. This method is used for productions created by the usual
	 * method -- the Create button on My Productions.
	 *
	 * @param product The Product matching the Production being created.
	 * @param name The Production name (title).
	 * @return The newly-created Production.
	 */
	public Production create(String accountNumber, Product product, String name,
			User currentUser, String transId) {
		Production production = new Production(name.trim()); // Sets production title
		production.setSku(product.getSku());
		production.setTransactionId(transId);
		production.setType(product.getType());
		production.setMaxUsers(product.getMaxUsers());
		production.setMaxProjects(product.getMaxProjects());
		production.setSmsEnabled(product.getSmsEnabled());
		production.setOwningAccount(accountNumber);
		//production.setAllowOnboarding(false);
		if (production.getType().isAicp()) { // for Commercial,
			production.setStudio(production.getTitle()); // studio/company = title
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(production.getStartDate());
		if (product.getDuration() == 0) { // "unlimited"
			production.setEndDate(null);
		}
		else {
			Calendar end = Calendar.getInstance();
			end.setTime(cal.getTime());
			end.add(Calendar.MONTH, product.getDuration());
			production.setEndDate(end.getTime());
		}
		if (product.getPrice().signum() != 0) {
			// either negative or positive price is marked paid.
			production.setOrderStatus(OrderStatus.PAID);
			cal.add(Calendar.MONTH, 1);
			production.setNextBillDate(cal.getTime());
			if (product.getDuration() == 1) { // "monthly"
				production.setEndDate(null);  // prevent prompting for "resubscribe"
			}
			production.setBillingAmount(product.getPrice());
		}
		else { // a free trial of some sort.
			production.setOrderStatus(OrderStatus.FREE);
		}

		production = create(production, currentUser);
		return production;
	}

	/**
	 * Save a new Production, and create all the basic objects it needs,
	 * including a first Project, an LS Admin, and (usually) a Data Admin.
	 * This method is used for productions created by either the "Add" button
	 * on the Prod Admin / Productions page, or by the (usual) Create button
	 * on My Productions.
	 *
	 * @param production The mostly initialized Production to be added to the
	 *            database.
	 * @return The updated Production object.
	 */
	@Transactional
	public Production create(Production production, User currentUser) {
		try {
			attachDirty(production); // make sure we have an id
			production.setProdId(ApplicationUtils.getProductionPrefix() + production.getId().toString());

			PayrollPreference pref = production.getPayrollPref();

			if (production.getType().isTours()) {
				production.setDeptMaskB(Constants.TOURS_DEPARTMENTS_MASK);
				pref.setCreateIncompleteTimecards(true);
			}

			if (production.getType().isCanadaTalent()) {
				// Set for Canada production
				production.setDeptMaskB(Constants.CANADA_DEPARTMENTS_MASK);
				pref.setCreateIncompleteTimecards(false);
				pref.setWorkCountry(Constants.COUNTRY_CODE_CANADA);
			}

			if (production.getType().isUsTalent()) {
				// Set for US Talent production
				production.setDeptMaskB(Constants.US_TALENT_DEPARTMENTS_MASK);
				pref.setCreateIncompleteTimecards(false);
				pref.setWorkCountry(Constants.DEFAULT_COUNTRY_CODE);
			}

			if (production.getType().isTours() || production.getType().isAicp()) {
				production.setShowScriptTabs(false);
			}

			// Set default preferences
			pref.setFirstPayrollDate(TimecardUtils.calculateWeekEndDate(production.getStartDate()));
			//SD-1867 set option 1 as default sick leave type
			pref.setSickLeaveType(CalifSickLeaveType.CA);
			// LS-1709 set mileage rate to current year's IRS specification
			pref.setMileageRate(TimecardUtils.findMileageRate(new Date()));
			pref.setStudioType(StudioType.IN); // assume independent, since they are much more common

			if (production.getType().getForTelevision()) {
				pref.setMediumType(MediumType.TV);
				pref.setDetailType(PayrollPreference.DETAIL_TYPE_ONE_HOUR_SERIES);
			}
			else {
				pref.setMediumType(MediumType.FT);
				pref.setDetailType(PayrollPreference.DETAIL_TYPE_BASIC);
			}

			if (production.getType().hasPayrollByProject()) { // Commercial production
				pref.setStudioType(StudioType.MJ); // assume "major" = AICP, since it's more common
				pref.setHourRoundingType(HourRoundingType.FOURTH);
			}
			else { // TV or Feature production
				pref.setStudioType(StudioType.IN); // assume independent, since they are much more common
				pref.setHourRoundingType(HourRoundingType.TENTH);
				pref.setFirstWorkWeekDay(Calendar.MONDAY); // LS-2515
			}

			pref.setTvSeason(PayrollPreference.SEASON_1);
			pref.setTvEra(PayrollPreference.STARTED_2013);
			pref.setNyRegion(PayrollPreference.NEW_YORK_REGION);

			// Set up a new Project for the production
			ProjectDAO projectDAO = ProjectDAO.getInstance();
			Project newProject = new Project();
			if (production.getType().isAicp()) {
				newProject.setTitle("Project 1");
			}
			else if (production.getType().getEpisodic()) {
				newProject.setTitle("Episode 1");
			}
			else {
				newProject.setTitle(production.getTitle());
			}
			newProject.setProduction(production);
			newProject.setOriginalEndDate(production.getEndDate());
			newProject.setTimeZoneStr(production.getTimeZoneStr());
			newProject.setDeptMask(production.getDeptMaskB());

			boolean ret = projectDAO.createNewProject(newProject, null,
					production.getStartDate(), false, false, true, false, false, false);

			if (ret) {
				production.setDefaultProject(newProject);
				production.getProjects().add(newProject);

				Folder repository = new Folder("Central Repository", currentUser, null, false, new Date());
				production.setRootFolder(repository);

				boolean isCanada = (production.getType() == ProductionType.CANADA_TALENT); // LS-1816
				if (production.getAddress() == null) {
					Address addr = new Address(isCanada);
					save(addr);
					production.setAddress(addr);
				}
				attachDirty(production);
				if (production.getType().isTalent()) {
					production.setAllowOnboarding(true);
					LOG.debug("");
					OnboardService service = new OnboardService();
					production = service.enableProductionOnboarding(production);
					attachDirty(production);
				}
//				if (production.getAllowOnboarding()) {
					//Create new Approval Paths and Approvers for the Production and Project.
					// LS-1645: this is already done by 'enableProductionOnboarding'.
// LS-1645			createPathAndApprovers(production, newProject);
//				}
				Contact contact;
				// Find the "production administrator" from the context param, and
				// add it as LS Admin
				User pUser = UserDAO.getInstance().findSingleUser(
						ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PROD_ADMIN));
				if (pUser != null) {
					contact = new Contact(pUser, pUser.getEmailAddress());
					initContact(newProject, contact, Constants.ROLE_ID_LS_ADMIN);
					contact.setLoginAllowed(true);
					contact.setNotifyByEmail(false);
					contact.setNotifyByTextMsg(false);
					attachDirty(contact);
				}
				// Find the "production view administrator" from the context param, and
				// add it as LS Admin / Viewer
				User vipUser = UserDAO.getInstance().findSingleUser(
						ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PROD_ADMIN_VIEW));
				if (vipUser != null) {
					contact = new Contact(vipUser, vipUser.getEmailAddress());
					initContact(newProject, contact, Constants.ROLE_ID_LS_ADMIN_VIEW);
					contact.setLoginAllowed(true);
					contact.setNotifyByEmail(false);
					contact.setNotifyByTextMsg(false);
					attachDirty(contact);
				}
				// Add a new Contact for the current User to the production, and make it a
				// data admin of the project, unless current user was made LS Admin above.
				if (pUser == null || ! pUser.getId().equals(currentUser.getId())) {
					contact = new Contact(currentUser, currentUser.getEmailAddress());
					contact.setLoginAllowed(true);
					int userRole = Constants.ROLE_ID_DATA_ADMIN;
					if (currentUser.getMemberType() == MemberType.PREMIUM) {
						// LS-1844 give LS eps Admin role to Premium members in any environment
						userRole = Constants.ROLE_ID_LS_ADMIN;
					}
					initContact(newProject, contact, userRole);
					// 3/25/16 do NOT make the owner a Financial Data admin...
					//Role role = RoleDAO.getInstance().findById(Constants.ROLE_ID_FINANCIAL_ADMIN);
					//Employment employment = new Employment(role, contact);
					//employment.setDefRole(true); // turn the defRole ON for every first employment
					//employment.setOccupation(role.getName());
					//ProjectMember newMember = new ProjectMember(null, employment);
					//contact.getEmployments().add(employment);
					//employment.getProjectMembers().add(newMember);
					attachDirty(contact);
				}

				Unit unit = newProject.getMainUnit();
				attachDirty(unit);

				production = refresh(production);
				ChangeUtils.logChange(ChangeType.PRODUCTION, ActionType.CREATE, production,
						currentUser,
						production.getSku() + ": " + production.getTitle());
			}
			else {
				EventUtils.logError("project creation failed");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return production;
	}

	/** Creates blank ApprovalPaths for the new Production
	 * and for its default Project.
	 * @param production
	 * @param newProject
	 */
//	private void createPathAndApprovers(Production production, Project newProject) {
//		ApprovalPathDAO approvalPathDAO = ApprovalPathDAO.getInstance();
//		ApprovalPath newProdPath = new ApprovalPath();
//		newProdPath.setName(Constants.DEFAULT_TC_APPROVAL_PATH);
//		newProdPath.setProduction(production);
//		newProdPath.setProject(null);
//		approvalPathDAO.attachDirty(newProdPath);
//		ApprovalPath newProjPath = new ApprovalPath();
//		newProjPath.setName(Constants.DEFAULT_APPROVAL_PATH);
//		newProjPath.setProduction(production);
//		newProjPath.setProject(newProject);
//		approvalPathDAO.attachDirty(newProjPath);
//	}

	/**
	 * Re-subscribe or upgrade a production, setting the status to either Active
	 * or Archived, depending on its current status.
	 * <p>
	 * If the nextBillDate is null -- expected for an upgrade -- then it is set
	 * to one month from today's date.
	 *
	 * @param production The Production to be updated.
	 * @param product The related Product - used as source for billing amount.
	 */
	@Transactional
	public void resubscribe(Production production, Product product) {
		LOG.debug("");
		production.setEndDate(null);
		production.setOrderStatus(OrderStatus.PAID);
		if (production.getNextBillDate() == null) {
			Calendar cal = Calendar.getInstance();
			CalendarUtils.setStartOfDay(cal);
			cal.add(Calendar.MONTH, 1);
			production.setNextBillDate(cal.getTime());
		}
		if (product != null) { // shouldn't happen, but just in case...
			production.setBillingAmount(product.getPrice());
		}
		if (production.getStatus() == AccessStatus.EXPIRED_ACTIVE) {
			production.setStatus(AccessStatus.ACTIVE);
		}
		else if (production.getStatus() == AccessStatus.EXPIRED_ARCHIVED) {
			production.setStatus(AccessStatus.ARCHIVED);
		}
		attachDirty(production);
	}

	/**
	 * Initialize the given Contact object by creating its project membership
	 * and setting other necessary fields. The object is not saved by this
	 * method.  Since this method is only used for the LS Admin and Prod Data Admin
	 * roles, the roles are added as production-wide roles (not specific to one
	 * Unit or Project).
	 *
	 * @param project The Project to which this Contact will be added.
	 * @param contact The new Contact object to be initialized.
	 * @param roleId The database id of the Role to be assigned to the Contact
	 *            within the first Unit of the given Project.
	 */
	private void initContact(Project project, Contact contact, int roleId) {
		contact.setProduction(project.getProduction()); // put it in the new production
		contact.setProject(project); // set its default project
		contact.setStatus(MemberStatus.ACCEPTED);
		contact.setDisplayName(contact.getUser().getDisplayName());

//		Unit unit = project.getMainUnit();
		Role role = RoleDAO.getInstance().findById(roleId);
		contact.setRole(role);

		Employment employment = new Employment(role, role.getName(), contact);

		ProjectMember newMember = new ProjectMember( null, employment);

//		unit.getProjectMembers().add(newMember);
		List<ProjectMember> projectMembers = new ArrayList<>();
		projectMembers.add(newMember);
		employment.setProjectMembers(projectMembers);
		List<Employment> emps = new ArrayList<>();
		emps.add(employment);
		contact.setEmployments(emps );
	}

	/**
	 * Mark a production deleted.
	 * @param prod
	 */
	@Transactional
	public void pseudoDelete(Production prod) {
		prod.setStatus(AccessStatus.DELETED);
		attachDirty(prod);
	}

	/**
	 * Delete a production and all its related objects.
	 * @param prod
	 */
	@Transactional
	public void remove(Production prod) {
		List<RealWorldElement> rwelems = RealWorldElementDAO.getInstance().findByProperty(RealWorldElementDAO.PRODUCTION, prod);

		// delete Material & FilmStock entries
		List<Material> mats = MaterialDAO.getInstance().findByProduction(prod);
		for (Material mat : mats) {
			MaterialDAO.getInstance().remove(mat);
		}

		// Delete all timecards
		List<WeeklyTimecard> wtcs = WeeklyTimecardDAO.getInstance().findByProduction(prod);
		for (WeeklyTimecard wtc : wtcs) {
			delete(wtc);
		}

		// Delete all Approvers
		Approver ap = prod.getApprover();
		while (ap != null) {
			Approver nextAp = ap.getNextApprover();
			delete(ap);
			ap = nextAp;
		}
		prod.setApprover(null);

		List<ApprovalAnchor> anchors = ApprovalAnchorDAO.getInstance().findByProductionProjectDepartmental(prod, null);
		for (ApprovalAnchor anch : anchors) {
			Approver app = anch.getApprover();
			while (app != null) {
				Approver nextAp = app.getNextApprover();
				delete(app);
				app = nextAp;
			}
			delete(anch);
		}

		// Delete all weekly (timecard) batches
		List<WeeklyBatch> wkBatches = WeeklyBatchDAO.getInstance().findByProductionProject(prod, null);
		for (WeeklyBatch wb : wkBatches) {
			delete(wb);
		}

		// Delete all production batches
		List<ProductionBatch> batches = ProductionBatchDAO.getInstance().findByProductionProject(prod, null);
		for (ProductionBatch pb : batches) {
			delete(pb);
		}

//		// Delete custom departments
//		List<Department> depts = DepartmentDAO.getInstance().findByProperty(DepartmentDAO.PRODUCTION, prod);
//		for (Department d : depts) {
//			delete(d);
//		}

		// Delete points of interest
		List<PointOfInterest> points = PointOfInterestDAO.getInstance().findByProduction(prod);
		for (PointOfInterest p : points) {
			delete(p);
		}

		// Clear all default project references in Contacts
		List<Contact> cs = ContactDAO.getInstance().findByProperty(ContactDAO.PRODUCTION, prod);
		for (Contact c : cs) {
			c.setProject(null);
		}

		// Delete all the projects in the production
		prod.setDefaultProject(null);
		Set<Project> pp = prod.getProjects();
		prod.setProjects(null); // need to release reference before we delete projects
		for (Project p : pp) {
			p.setCallsheets(null);
		}
		for (Project p : pp) {
			List<Callsheet> calls = CallsheetDAO.getInstance().findByProperty(CallsheetDAO.PROJECT, p);
			for (Callsheet c : calls) {
//				c.setProject(null);
//				c.setProjects(null);
				delete(c);
			}
			ProjectDAO.getInstance().remove(p, false);
		}

		// Delete all Start Forms
//		List<StartForm> starts = StartFormDAO.getInstance().findByProduction(prod);
//		for (StartForm start : starts) {
//			delete(start);
//		}

		// Delete all Contacts in the production
		for (Contact c : cs) {
			delete(c);
		}

		for (RealWorldElement rwe : rwelems) {
			delete(rwe);
		}

		if (prod.getAddress() != null) {
			delete(prod.getAddress());
			prod.setAddress(null);
		}

//		List<Role> roles = RoleDAO.getInstance().findByProperty(RoleDAO.PRODUCTION, prod);
//		for (Role r : roles) {
//			delete(r);
//		}

		/*List<ContactDocument> cdList = ContactDocumentDAO.getInstance().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_PRODUCTION, map("production",prod));
		for (ContactDocument cd : cdList) {
			delete(cd);
		}

		List<Employment> employments = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION, map("production",prod));
		for (Employment emp : employments) {
			delete(emp);
		}*/

		// delete production folders and documents?

		delete(prod);
	}

	/**
	 * Mark the given production expired.
	 * @param prod
	 */
	@Transactional
	public void expire(Production prod, AccessStatus status) {
		prod.setStatus(status);
		prod.setSmsEnabled(false);
		attachDirty(prod);

		// Max 5 Prod Admin users, all other users do not have login access.
		int admins = 0;
		List<Contact> list = ContactDAO.getInstance().findByProductionActive(prod);
		for (Contact c : list) {
			boolean admin = false;
			boolean lsAdmin = false;
			for (Employment emp : c.getEmployments()) {
				if (emp.getRole().isLsAdmin()) {
					lsAdmin = true;
					break;
				}
				if (emp.getRole().isDataAdmin()) {
					admins++;
					admin = true;
					break;
				}
			}
			if (! lsAdmin && (! admin || admins > Constants.MAX_EXPIRED_ADMINS)) {
				if (c.getLoginAllowed() || c.getStatus() != MemberStatus.NO_ACCESS) {
					c.setLoginAllowed(false);
					c.setStatus(MemberStatus.NO_ACCESS);
					attachDirty(c);
				}
			}
		}

		// If more than 5 projects, set all but latest 5 to read-only status
//		if (prod.getProjects().size() > Constants.MAX_EXPIRED_WRITE_PROJECTS) {
//			List<Project> projects = new ArrayList<Project>(prod.getProjects());
//			Collections.sort(projects, getComparator());
//			for (int i = 5; i < projects.size(); i++) {
//				Project proj = projects.get(i);
//				if (proj.getStatus() != AccessStatus.READ_ONLY) {
//					proj.setStatus(AccessStatus.READ_ONLY);
//					attachDirty(proj);
//				}
//			}
//		}

	}

	protected static Comparator<Project> getComparator() {
		Comparator<Project> comparator = new Comparator<Project>() {
			@Override
			public int compare(Project one, Project two) {
				return one.compareTo(two, Project.SORTKEY_ID, false);
			}
		};
		return comparator;
	}

	/**
	 * Associate the given contracts with the given Production.
	 *
	 * @param production The Production to which the contracts will be
	 * added.
	 * @param list List of Contract instances to be added to the
	 * specified Production.
	 */
	@Transactional
	public void addContracts(Production production, List<Contract> list) {
		List<Contract> included = new ArrayList<>();
		for (Contract con : list) {
			production.getContracts().add(con);
			if (con.getIncludesMask() != 0) {
				// This contract causes the inclusion of other contracts
				Map<String, Object> values = new HashMap<>();
				values.put("mask", con.getIncludesMask().intValue());
				List<Contract> more = ContractDAO.getInstance().findByNamedQuery(Contract.FIND_CONTRACTS_INCLUDED_WITH, values);
				included.addAll(more);
			}

			// We currently don't define the inverse relation
//			con.getProductions().add(production);
//			attachDirty(con);
		}
		for (Contract con : included) { // Add any additional ones found
			production.getContracts().add(con);
		}
		attachDirty(production);
	}

	/**
	 * @param production
	 * @param list
	 */
	@Transactional
	public void removeContracts(Production production, List<Contract> list) {
		boolean b;
		for (Contract con : list) {
			b = production.getContracts().remove(con);
			if (! b) {
				LOG.warn("contract not found");
			}

			// We currently don't define the inverse relation
//			b = con.getProductions().remove(production);
//			if (! b) {
//				LOG.warn("production not found");
//			}
//			attachDirty(con);
		}
		attachDirty(production);
	}

	/** Method creates the filtered list of production according to the parameters passed
	 * @param titleValue production title
	 * @param studioValue production's studio
	 * @param ownerValue production owner
	 * @param pageSize no of productions to fetch at a time
	 * @param first index of the production from where the list will start
	 * @param type production type
	 * @return list of production
	 */
	@SuppressWarnings("unchecked")
	public List<Production> findByFilter(String titleValue, String studioValue, String ownerValue, int pageSize, int first, ProductionType type) {
		try {
			Criteria criteria = createFilterCritera(titleValue, studioValue, ownerValue, type);
			criteria.setFirstResult(first);
			criteria.setMaxResults(pageSize);
			criteria.addOrder(Order.asc(TITLE));
			List<Production> list = criteria.list();
			LOG.debug("Count=" + list.size() + ", String to be matched =" + titleValue +"-"+ studioValue+"-"+"-"+ ownerValue+"--"+type);
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/** Method used to get the count of the filtered production list
	 * @param titleValue production title
	 * @param studioValue production's studio
	 * @param ownerValue production owner
	 * @param type production type
	 * @return count of productions
	 */
	public long findCountByFilter(String titleValue, String studioValue, String ownerValue,
			ProductionType type) {
		long count = 0L;
		try {
			Criteria criteria = createFilterCritera(titleValue, studioValue, ownerValue, type);
			Projection pj = Projections.rowCount();
			criteria.setProjection(pj);
			@SuppressWarnings("unchecked")
			List<Long> list = criteria.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
		LOG.debug("count=" + count);
		return count;
	}

	/** Method creates the criteria for getting the filtered list of production
	 * @param titleValue production title
	 * @param studioValue production's studio
	 * @param ownerValue production owner
	 * @param type production's type
	 * @return criteria
	 */
	public Criteria createFilterCritera(String titleValue, String studioValue, String ownerValue, ProductionType type) {
		Criteria criteria = getHibernateSession().createCriteria(Production.class);

		if (titleValue != null) {
			titleValue = "%"+titleValue+"%";
			criteria.add(Restrictions.like(TITLE, titleValue));
		}
		if (studioValue != null) {
			studioValue = "%"+studioValue+"%";
			criteria.add(Restrictions.like(STUDIO, studioValue));
		}
		if (ownerValue != null) {
			ownerValue = "%"+ownerValue+"%";
			criteria.add(Restrictions.like(OWNING_ACCOUNT, ownerValue));
		}
		if (type != null) {
			criteria.add(Restrictions.eq(TYPE, type));
		}
		return criteria;
	}

	/**
	 * Get the "SYSTEM" Production object, which controls access to capabilities
	 * before the user has entered any 'real' Production.
	 * @return The SYSTEM production, or null if it does not exist.
	 */
	public static Production getSystemProduction() {
		Production prod = getInstance().findById(Constants.SYSTEM_PRODUCTION_ID);
		return prod;
	}

	/** Method creates the document and document chain records for standard I9 and payroll start.
	 * @param onboardFolder root folder of the document and chain
	 * @param name name of the document and chain
	 * @param description description of the document and chain
	 */
	/*private void saveStandardDocumentDocumentChain(Folder onboardFolder, PayrollFormType type) {
		Date date = new Date();
		DocumentChain chain = new DocumentChain();
		chain.setName(type.getName());
		chain.setType(MimeType.LS_FORM);
		chain.setCreated(date);
		chain.setRevised(date);
		chain.setRevisions(1);
		chain.setCreatorAcct(SessionUtils.getCurrentUser().getAccountNumber());
		chain.setDocFlowType(DocumentFlowType.RD_SN_SUB);
		chain.setFolder(onboardFolder);
		chain.setDeleted(false);
		Integer chainId = DocumentChainDAO.getInstance().save(chain);

		Document document = new Document();
		document.setName(type.getName());
		document.setDescription(type.getDescription());
		document.setAuthor(SessionUtils.getCurrentUser().getDisplayName());
		document.setUser(SessionUtils.getCurrentUser());
		document.setCreated(date);
		document.setLoaded(date);
		document.setUpdated(date);
		document.setPrivate(false);
		document.setType(type.getName());
		document.setContent(null);
		document.setFolder(onboardFolder);
		document.setMimeType(MimeType.LS_FORM);
		document.setDocChainId(chainId);
		document.setRevision(1);
		document.setOldest(true);
		document.setDeleted(false);
		document.setStandard(true);
		DocumentDAO.getInstance().save(document);
	}*/

}
