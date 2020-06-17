/**
 * File: TimeEmpSelectBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.*;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.*;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.onboard.AttachmentBean;
import com.lightspeedeps.web.user.UserPrefBean;

/**
 * A superclass which extends TimecardBase by handling (a) the selection of an employee
 * from a list of employees, to which the current user has approval or editing rights;
 * and (b) the selection of a Production (for the My Timecards page).
 * Subclasses of this class include IndivTimecardBean and MobileTimecardBean.
 */
public class TimecardEmpSelectBase extends TimecardBase {
	/** */
	private static final long serialVersionUID = - 4875579729037672711L;

	private static final Log log = LogFactory.getLog(TimecardEmpSelectBase.class);

	/** True iff the current user has the right to view timecards from all
	 * departments.  This is typically because the user is a production
	 * approver, or has View_HTG permission. */
	private boolean viewAllDepts = false;

	/** True iff the current user has the right to create timecards in all
	 * departments. This is typically because the user is a production
	 * approver. */
	private boolean createAllDepts;

	/** A List of Integers, where each Integer is the Department.id
	 * value of a Department for which the user is either an approver or a
	 * time-keeper.  This is not used if 'allDepts' is true. */
	private List<Integer> deptIdList;

	/** For time-keepers or approvers, list of employees whose time cards
	 * may be viewed. */
	/* package */ List<SelectItem> employeeDL;

	/** List of timecards displayed for the user being edited/reviewed. (This is not
	 * necessarily the logged-in user.) */
	/* package */ List<WeeklyTimecard> weeklyTimecardList;

	/** A List containing the Production`s available to a user on the
	 * My Timecards page.  The value field of each SelectItem is the
	 * Production.id and the label is the Production.name. */
	private List<SelectItem> productionList;

	/**
	 * Default (only) constructor.
	 */
	public TimecardEmpSelectBase() {
		super(WeeklyTimecard.SORTKEY_DATE);	// Let superclass get constructed; sort is date (followed by name)
		init();		// ...then do initialization.
	}

	/**
	 * Initialize the values to display when the Individual Timecard page loads.
	 * We load the list of all the user's time cards for this production, and
	 * display the first one in the list.
	 * <p>
	 * May be overridden by subclasses, to do their own initialization before
	 * ours!
	 */
	protected void init() {
		if (getNotInProduction()) {
			if (tcUser == null) {
				tcUser = SessionUtils.getCurrentUser();
			}
			productionList = new ArrayList<>();
			selectProductionId(createProductionList(productionList, getvUser()));
			initTimecardFromSession(); // reset particular TC to show, if any
		}
		else {
			Integer id = SessionUtils.getInteger(Constants.ATTR_TC_TCUSER_ID);
			if (id != null) {
				tcUser = UserDAO.getInstance().findById(id);
			}
			else {
				id = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
				if (id != null) {
					// probably coming here after change in selection by Onboarding or Cast&Crew pages
					Contact contact = ContactDAO.getInstance().findById(id);
					if (contact != null) {
						tcUser = contact.getUser();
						weeklyTimecard = null; // force timecard from this user
					}
				}
			}
		}
		if (tcUser == null) {
			tcUser = SessionUtils.getCurrentUser();
		}

		if (weeklyTimecard == null) {
			getPaddedWeeklyTimecardList();
		}
		if (weeklyTimecard != null) { // we're supposed to display this timecard
			String acct = weeklyTimecard.getUserAccount();
			// make sure 'tcUser' matches the selected timecard...
			if (! acct.equals(tcUser.getAccountNumber())) {
				if (getNotInProduction()) {
					weeklyTimecard = null;
				}
				else {
					tcUser = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, acct);
					if (tcUser == null) { // should never happen! (timecard without matching User)
						EventUtils.logError("WeeklyTimecard with account# " + acct + " has no matching User record.");
						tcUser = SessionUtils.getCurrentUser();
						weeklyTimecard = null;
					}
				}
			}
		}
		userId = tcUser.getId();
		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, userId);
		if (tcUser.equals(getvUser())) {
			// Let employee see all their project timecards (for Commercials)
			setViewProject(null);
			if (getCommProject() != null) {
				setWeeklyTimecardList(null);
			}
		}

		// Set Full Timecard aggregate view option based on individual view
//		SessionUtils.put(Constants.ATTR_FULL_TC_SHOW_ALL, getViewProject()==null);

		if (weeklyTimecard != null) { // we're supposed to display this timecard
			setupSelectedItem(weeklyTimecard.getId());
		}
		else {
			setupSelectedItem(null);
		}

		setupUserChanged(tcUser);	// allow superclass to initialize
	}

	/**
	 * Determine which departments the current user has approval or time-keeping
	 * rights for. This will either set the 'allDepts' field to true, or it will
	 * construct a List of Integers, where each Integer is the Department.id
	 * value of a Department for which the user is either an approver or a
	 * time-keeper. Note that someone with viewHtg privilege is allowed to see
	 * all timecards, regardless of their approver role.
	 */
	private void createDeptIdList() {
		deptIdList = new ArrayList<>(0);
		createAllDepts = false;
		viewAllDepts = false;
		if (! AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_PRODUCTION)) {
			return;
		}
		if (getUserHasViewHtg()) {
			viewAllDepts = true;
		}
		boolean isApprover = ApproverDAO.getInstance().existsProperty(ApproverDAO.CONTACT, getvContact());
		if (isApprover) { // some kind of approver ... figure out what departments s/he covers
			boolean isProdProjApprover = ApproverUtils.isProdApprover(getvContact());
			if ((! isProdProjApprover) && getViewProduction().getType().isAicp()) {
				// not production approver; this is commercial production - check for project approver.
				Project proj = null;
				if (! userHasViewAllProjects) {
					proj = SessionUtils.getCurrentProject();
				}
				isProdProjApprover = ApproverUtils.isProjectApprover(getvContact(), proj);
			}
			if (isProdProjApprover) {
				viewAllDepts = true;
				createAllDepts = true; // user can create timecards for all departments
			}
			else {
				Integer vUserId = getvUser().getId();
				// First get list of database id's for Department`s the current user is a time-keeper for.
				// Note that we pass viewProject, so aggregate users on Commercial productions (financial data
				// admins) will see all possible departments for which they are an approver, regardless of Project.
				deptIdList = DepartmentDAO.getInstance().findStdIdsByContact(getvContact(), getViewProject());

				// Then add ids of Departments where contact is an approver
				List<ApprovalAnchor> anchors = ApprovalAnchorDAO.getInstance()
						.findByProductionProjectDepartmental(getViewProduction(), getViewProject());
				for (ApprovalAnchor anchor : anchors) {
					Approver app = anchor.getFirstApprover();
					boolean found = false;
					while (app != null && ! found) {
						found = app.getContact().getUser().getId().equals(vUserId);
						app = app.getNextApprover();
					}
					if (found) {
						deptIdList.add(anchor.getDepartment().getId());
					}
				}
			}
		}
		else { // not an approver; check time-keeper status
			deptIdList = DepartmentDAO.getInstance().findStdIdsByContact(getvContact(), getCommProject());
		}
	}

	/**
	 * ValueChangeListener method for the employee drop-down list. When the user
	 * chooses a different employee, we have to re-populate the list of time
	 * cards and the occupation list.
	 *
	 * @param evt The change event generated by the framework.
	 */
	public void listenEmployee(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		log.debug("id =" + id);
		if (id != null) {
			try {
				tcUser = UserDAO.getInstance().findById(id);
				setupUserChanged(tcUser);
				weeklyTimecard = null; // force selection of timecard from refreshed list
				SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
				weeklyTimecardList = null;	// force refresh of list
				// list of visible Departments changes if changing from viewing own TCs to others (or vice-versa)
				deptIdList = null;			// ...so force refresh of department list
				SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, id);
				Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getViewProduction());
				SessionUtils.put(Constants.ATTR_CONTACT_ID, contact == null ? null : contact.getId());
				SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null); // so onboarding will use attr_contact_id
				SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID,null);
				if (((!SessionUtils.isMobileUser()) && tcUser.equals(getvUser()))
						|| userHasViewAllProjects) {
					setViewProject(null);
				}
				else {
					setViewProject(getCommProject());
				}
				getPaddedWeeklyTimecardList();	// refresh list; rev 4775
			}
			catch (Exception e) {
				EventUtils.logError(e);
				MsgUtils.addGenericErrorMessage();
			}
		}
	}

	/**
	 * ValueChangeListener method for the Production drop-down list. When the user
	 * chooses a different production, we have to re-populate the list of time
	 * cards.
	 *
	 * @param evt The change event generated by the framework.
	 */
	public void listenProduction(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		selectProductionId(id);
	}

	/**
	 * Creates the List that populates the left-hand displayed list (on
	 * the Basic Timecard page) of all the timecards for one person within
	 * the current Production.  This is based on the current 'tcUser' value.
	 * It also selects the first item in the list, and sets the 'weeklyTimecard'
	 * value to this entry.
	 */
	protected WeeklyTimecard createWeeklyTimecardList() {
		weeklyTimecardList = createWeeklyTimecardListVisible();
		if (weeklyTimecardList.size() > 0) {
			setSelectedRow(-1);
			sort(); // sort the list in default order
			if (weeklyTimecard == null || ! weeklyTimecardList.contains(weeklyTimecard)) {
				// Pick a list entry to set the initially displayed timecard
				weeklyTimecard = null;
				if (weeklyTimecardList.size() > 0) {
// LS-1304			Integer id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
//					if (id != null) {
//						weeklyTimecard = getWeeklyTimecardDAO().findById(id);
//					}
//					else {
					weeklyTimecard = weeklyTimecardList.get(0); // default to top entry...
//					}
					AttachmentBean.getInstance().setWeeklyTimecard(weeklyTimecard);
					// ...but prefer to find timecard matching the current week
					Date date = TimecardUtils.calculateLastDayOfCurrentWeek();
					for (WeeklyTimecard wt : weeklyTimecardList) {
						if (wt.getEndDate().equals(date)) {
							weeklyTimecard = wt;
							break;
						}
					}
					setupSelectedItem(weeklyTimecard.getId());
				}
			}
			if (weeklyTimecard != null) {
				weeklyTimecard.setSelected(true);
			}
		}
		else {
			// can happen if user switches to view an employee with no timecards
			setupSelectedItem(null);
		}
		return null;
	}

	/**
	 * Creates the List of all the timecards for one person (based on the value
	 * of {@link TimecardBase#tcUser}) within the current Production which
	 * should be visible to the current (logged-in) user. For example, a
	 * department approver can only see those timecards related to the
	 * Department`s for which they are an approver. If the current Production is
	 * null, an empty List is created.
	 *
	 * @return A non-null List of WeeklyTimecard`s.
	 */
	protected List<WeeklyTimecard> createWeeklyTimecardListVisible() {
		List<WeeklyTimecard> tcList = createWeeklyTimecardListUser();
		boolean includeAll = getNotInProduction() || tcUser.equals(getvUser()) || getViewAllDepts();
		for (Iterator<WeeklyTimecard> iter = tcList.iterator(); iter.hasNext(); ) {
			WeeklyTimecard wtc = iter.next();
			if (includeAll || getDeptIdList().contains(wtc.getDepartmentId())) {
				wtc.setSelected(false);
				TimecardCalc.calculateWeeklyTotals(wtc);
			}
			else {
				iter.remove();
			}
		}
		return tcList;
	}

	/**
	 * Initialize the list of employees available for selection on the
	 * Individual Timecard page. This will also force the initialization of the
	 * list of visible departments if that hasn't been done yet.
	 */
	private void createEmployeeDL() {
		if (getViewAllDepts() || deptIdList.size() > 0) {
			createEmployeeDL(deptIdList, viewAllDepts);
		}
		else {
			employeeDL = new ArrayList<>();
		}
	}

	/**
	 * Create the list of employee's for a time-keeper or approver to select
	 * from, on the Individual time card page.
	 *
	 * @param idList A List of Integer values, where each value is the id of a
	 *            Department. The resulting List should include all employees
	 *            who have any Role in any one of those departments. This may
	 *            only be null if 'includeAllDepts' is true.
	 * @param includeAllDepts If true, all employees from the production should be
	 *            included in the list; the 'idList' is ignored in this case.
	 */
	private void createEmployeeDL(Collection<Integer> idList, boolean includeAllDepts) {
		boolean foundDisplayedUser = false; // was User whose timecard we're supposed to display found?
		boolean foundCurrentUser = false; // was the current logged in User found?
		User currUser = SessionUtils.getCurrentUser();
		Integer currUserId = currUser.getId();

		employeeDL = new ArrayList<>();
		// find the users with Roles or Start Forms in
		// any of the departments represented by the list of department ids.
		if (includeAllDepts) {
			List<User> users = TimecardUtils.findTimecardUsers();
			for (User user : users) {
				employeeDL.add(new SelectItem(user.getId(), user.getLastNameFirstName()));
				if (user.getId().equals(userId)) {
					foundDisplayedUser = true;
				}
				if (user.getId().equals(currUserId)) {
					foundCurrentUser = true;
				}
			}
		}
		else {
			List<User> users = UserDAO.getInstance()
					.findByDepartments(idList, getViewProduction(), getViewProject());
			for (User usr : users) {
				employeeDL.add(new SelectItem(usr.getId(), usr.getLastNameFirstName()));
				if (usr.getId().equals(userId)) {
					foundDisplayedUser = true;
				}
				if (usr.getId().equals(currUserId)) {
					foundCurrentUser = true;
				}
			}
		}
		if (! foundCurrentUser) { // current (logged in) User not found, add to list
			employeeDL.add(new SelectItem(currUserId, currUser.getLastNameFirstName()));
			if (currUserId.equals(userId)) {
				foundDisplayedUser = true;
			}
		}
		Collections.sort(employeeDL, getSelectItemComparator());
		if (! foundDisplayedUser && employeeDL.size() > 0) { // default selection (logged in user) is not in list
			// Make first entry the selection
			userId = (Integer)employeeDL.get(0).getValue();
			tcUser = UserDAO.getInstance().findById(userId);
			SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, userId);
			Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getViewProduction());
			SessionUtils.put(Constants.ATTR_CONTACT_ID, contact == null ? null : contact.getId());
		}
	}

	/**
	 * Create the List of SelectItem`s for the Production selection drop-down.
	 * This is used in the My Timecards view.
	 *
	 * @param prodList The list of productions to be updated (cleared and
	 *            filled).
	 * @param user The User currently viewing the list; used to determine which
	 *            productions are in the list.
	 * @return The database id of the default Production to be selected/displayed.
	 */
	public static Integer createProductionList(List<SelectItem> prodList, User user) {
		prodList.clear(); // just in case
		prodList.add(new SelectItem(0, "select a production"));

		List<Production> list = ProductionDAO.getInstance().findByUser(user);
		Production recent = null;
		for (Production p : list) {
			if ((! p.isSystemProduction()) && (! p.getType().isTours()) && (p.getStatus() != AccessStatus.DELETED)) {
				//LS-1703 Limit the list of Productions shown in My Timecards
				if (p.getType().getMyTimeCardProdList()) {
					prodList.add(new SelectItem(p.getId(), p.getTitle()));
					if (recent == null || p.getStartDate().after(recent.getStartDate())) {
						recent = p; // remember the most recently created production in list
					}
				}
			}
		}
		Integer id = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
		if (id == null || id == 0) {
			id = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID); // check user preference. ESS-1513
			if (id == null && recent != null) { // 4.16.0.RC11
				id = recent.getId();
			}
		}
		return id;
	}

	/**
	 * Select the particular Production whose timecards will be listed. This is
	 * used for the My Timecards page, and also for the mobile timecards page
	 * when a user picks a Production.
	 *
	 * @param id The Production.id value of the selected Production.
	 */
	protected void selectProductionId(Integer id) {
		if (id != null) {
			weeklyTimecardList = null; // force refresh
			weeklyTimecard = null;
			Production prod = ProductionDAO.getInstance().findById(id);
			setViewProduction(prod);
			if (prod != null) {
				setViewProductionId(id);
				Project viewProj = null;
				if (prod.getType().hasPayrollByProject()) {
					viewProj = SessionUtils.getCurrentProject();
					if (viewProj == null || ! viewProj.getProduction().equals(prod)) {
						viewProj = prod.getDefaultProject();
					}
				}
				setViewProject(viewProj);
				if (getNotInProduction()) { // "My timecards" - find & save contact. LS-1270
					Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, prod);
					setvContact(contact);
				}
			}
			else {
				setViewProductionId(0);
				setViewProject(null);
			}
			setCommProject(getViewProject());
			if (getViewProject() != null && tcUser != null && tcUser.equals(getvUser())) {
				// Let employee see all their project timecards (for Commercials)
				setViewProject(null);
			}
			SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, getViewProductionId());
		}
	}

	/**
	 * Get the list of timecards; on mobile, include the missing timecards. This
	 * method is overridden by MobileTimecardBean to accomplish this.
	 *
	 * @return Non-null (but possibly empty) list of available timecards. See
	 *         {@link #weeklyTimecardList}.
	 */
	public List<WeeklyTimecard> getPaddedWeeklyTimecardList() {
		return getWeeklyTimecardList();
	}

	/** See {@link #weeklyTimecardList}. */
	public List<WeeklyTimecard> getWeeklyTimecardList() {
		if (weeklyTimecardList == null) {
			createWeeklyTimecardList();
		}
		return weeklyTimecardList;
	}
	/** See {@link #weeklyTimecardList}. */
	public void setWeeklyTimecardList(List<WeeklyTimecard> weeklyTimecardList) {
		this.weeklyTimecardList = weeklyTimecardList;
	}


	/** See {@link #employeeDL}. */
	public List<SelectItem> getEmployeeDL() {
		if (employeeDL == null) {
			createEmployeeDL();
		}
		return employeeDL;
	}
	/** See {@link #employeeDL}. */
	public void setEmployeeDL(List<SelectItem> employeeDL) {
		this.employeeDL = employeeDL;
	}

	/**See {@link #productionList}. */
	public List<SelectItem> getProductionList() {
		if (productionList == null) {
			productionList = new ArrayList<>();
			selectProductionId(createProductionList(productionList, getvUser()));
		}
		return productionList;
	}

	/**See {@link #productionList}. */
	public void setProductionList(List<SelectItem> productionList) {
		this.productionList = productionList;
	}

	/** See {@link #viewAllDepts}. */
	public boolean getViewAllDepts() {
		if (deptIdList == null) {
			createDeptIdList(); // this initializes 'viewAllDepts' !!
		}
		return viewAllDepts;
	}

	/**See {@link #createAllDepts}. */
	public boolean getCreateAllDepts() {
		if (deptIdList == null) {
			createDeptIdList(); // this initializes 'createAllDepts' !!
		}
		return createAllDepts;
	}

	/** See {@link #deptIdList}. */
	public List<Integer> getDeptIdList() {
		if (deptIdList == null) {
			createDeptIdList();
		}
		return deptIdList;
	}

	/**
	 * This method is called when this bean is about to go 'out of scope', e.g.,
	 * when the user is leaving the page or their session expires. We use it to
	 * unlock the WeeklyTimecard to make it available again for editing.
	 */
	@Override
	public void dispose() {
		log.debug("");
		super.dispose();
		weeklyTimecardList = null;
	}
}
