/**
 * File: IndivTimecardBean.java
 */
package com.lightspeedeps.web.timecard;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the Individual Time Card pages -- both "in production"
 * (Payroll | Timecards) and "out of production" (My Timecards). Much of the
 * underlying functionality is in one of the superclasses, which also underly
 * the {@link FullTimecardBean} and/or {@link MobileTimecardBean} classes.
 *
 * @see TimecardEmpSelectBase
 * @see TimecardBase
 */
@ManagedBean
@ViewScoped
public class IndivTimecardBean extends TimecardEmpSelectBase implements Serializable {
	/** */
	private static final long serialVersionUID = 4506877014355826044L;

	private static final Log log = LogFactory.getLog(IndivTimecardBean.class);

	/** True iff the user is creating an "adjusted timecard" in the Create Timecard
	 * dialog. */
	private boolean adjustedTimecard;

	/** True iff the current user MAY have the authority to create a new time card. */
	private boolean createAuth;

	/** True iff the production selection menu (drop-down) should be disabled.  We
	 * disable it when the Submit dialog box is displayed, so that password
	 * managers (like RoboForm) won't save or change the drop-down setting. */
	private boolean prodSelectDisabled = false;

	/** True iff the Create Timecard pop-up should be displayed. */
	private boolean showCreate;

	/** The occupation drop-down list for the Create Timecard pop-up, based
	 * on existing Start Forms for the user. */
	private List<SelectItem> occupationDL;

	/** Database id of the selected StartForm from the occupation drop-down
	 * on the Create Timecard pop-up. */
	private Integer startFormId;

	/** The project drop-down list for the Create Timecard pop-up, based
	 * on existing Start Forms for the user. */
	private List<SelectItem> projectDL;

	/** The project to be used for creating a timecard. Selected by the user in the case
	 * of a commercial production. Null for non-commercial productions. */
	private Project createProject;

	/** The id of the project to be used for creating a timecard. Selected by the user
	 * via the drop-down list in the Create Timecard dialog. */
	private Integer createProjectId;

	/** The week-ending date selection list for the Create Timecard pop-up. */
	private List<SelectItem> weekEndDateDL;

	/**
	 * Default (only) constructor.
	 */
	public IndivTimecardBean() {
		super();	// Let superclass get constructed
		initThis();		// ...then do initialization.
		SessionUtils.put(Constants.ATTR_TIMECARD_VIEW, 0);
	}

	/**
	 * Initialize the values to display when the Individual Timecard page loads.
	 * We load the list of all the user's time cards for this production, and
	 * display the first one in the list.
	 */
	private void initThis() {
		teamPayroll = null;
		if (weeklyTimecard == null) {
			/* There was no "default" timecard (typically saved in the session).  We want
			 * to create the timecard list here and pick the first one to make it the viewed
			 * timecard, so we can set button flags (particularly createAuth) appropriately,
			 * rather than waiting for the JSP rendering code to cause it.  If we wait for
			 * the render calls (e.g., getSortedItemList()) to do it, the createAuth field (and
			 * maybe others) can be tested by the JSP before they have been computed in
			 * relation to the timecard that is ultimately selected for viewing. */
			getWeeklyTimecardList();
		}
		checkTab();
		restoreSortOrder();
		scrollToRow(getWeeklyTimecard()); // on page load, make sure item is visible in list
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (weeklyTimecard != null) {
			weeklyTimecard.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		setShowCopyPrior(null); // force recalculation of value.
		if (id == null) {
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
			weeklyTimecard = null;
			editMode = false;
			newEntry = false;
			calculateCreateFlag(null);
		}
		else if ( ! id.equals(NEW_ID)) {
			if (weeklyTimecard == null || ! id.equals(weeklyTimecard.getId())) {
				weeklyTimecard = getWeeklyTimecardDAO().findById(id);
				if (weeklyTimecard == null) {
					id = findDefaultId();
					if (id != null) {
						weeklyTimecard = getWeeklyTimecardDAO().findById(id);
					}
				}
			}
			else {
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			}

			if (weeklyTimecard != null) {
				statusChanged(weeklyTimecard);
				calculateCreateFlag(weeklyTimecard);
				TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
				weeklyTimecard.setSelected(true);
				getStateMap().clear(); // clear ICEfaces selections
				selectRowState(weeklyTimecard);  // set new ICEfaces selection
				timecardChanged(); // notify others of new selection
			}
			else {
				calculateCreateFlag(null);
			}
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		}
	}

	/**
	 * Get the first default id
	 * @return id
	 */
	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<WeeklyTimecard> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	/**
	 * Action method for the "Create" button - opens the Create Timecard dialog
	 * and sets up data fields used by the dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionOpenCreate() {
		try {
			adjustedTimecard = false;
			if (updateCreateDialogLists()) {
				setShowCreate(true);
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Ok" button on the "Create timecard" dialog.
	 * If selection is valid, create the timecard and make it the selected one.
	 *
	 * @return null navigation string.
	 */
	public String actionCreateOk() {
		try {
			if (getWeekEndDate().getTime() == 0) {
				// only happens in rare cases due to framework/browser issues.
				MsgUtils.addFacesMessage("Timecard.AlreadyExists", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			Production prod = getViewProduction();
			prod = ProductionDAO.getInstance().refresh(prod);
			StartForm sd = StartFormDAO.getInstance().findById(getStartFormId());
			if ((! adjustedTimecard) &&
					getWeeklyTimecardDAO().existsWeekEndDateAccountOccupation(prod,
					sd.getProject(), getWeekEndDate(), tcUser.getAccountNumber(), sd.getJobClass())) {
				MsgUtils.addFacesMessage("Timecard.AlreadyExists", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (sd == null || sd.getEmployment() == null) {
				MsgUtils.addFacesMessage("Timecard.CreateFailed", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			setShowCreate(false);
			weeklyTimecard = TimecardUtils.createTimecard(tcUser, prod, getWeekEndDate(), sd, adjustedTimecard);
			if (weeklyTimecard != null) {
				Integer id = getWeeklyTimecard().getId(); // id of new timecard
//				weeklyTimecardList = null; // force refresh
				createWeeklyTimecardList();
				setupSelectedItem(id);
			}
			else {
				MsgUtils.addFacesMessage("Timecard.CreateFailed", FacesMessage.SEVERITY_ERROR);
				weeklyTimecard = new WeeklyTimecard();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionCloseCreate() {
		setShowCreate(false);
		return null;
	}

	/**
	 * Action method for the Submit button.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionSubmit()
	 */
	@Override
	public String actionSubmit() {
		prodSelectDisabled = true;
		String res = super.actionSubmit();
		if (weeklyTimecard == null) { // someone else deleted the current timecard
			weeklyTimecardList = null; // force refresh
		}
		return res;
	}

	/**
	 * Action method for the Submit/Ok button on the Submit dialog
	 * box.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionSubmitOk(boolean)
	 */
	@Override
	protected boolean actionSubmitOk(boolean approve) {
		prodSelectDisabled = false;
		boolean bRet = super.actionSubmitOk(approve);
		if (! bRet || weeklyTimecard == null) { // submit failed
			weeklyTimecardList = null; // force list refresh
		}
		return bRet;
	}

	/**
	 * Action method for the Edit button.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionEdit()
	 */
	@Override
	public String actionEdit() {
		String res = super.actionEdit();
		if (weeklyTimecard == null) { // someone else deleted the current timecard
			weeklyTimecardList = null; // force list refresh
		}
		else {
			insertExtraPbLine(weeklyTimecard);
		}
		return res;
	}

	/**
	 * Action method for the Save button. If save completes, force a page
	 * refresh, to clear out the ICEfaces DOM tree. This is necessary because
	 * some input fields get changed from rendered to non-rendered based on user
	 * input, but the tree does not get updated. (see rev 2.2.4341).
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionSave()
	 */
	@Override
	public String actionSave() {
		super.actionSave();
		if (! editMode) {
			if (getNotInProduction()) {
				return HeaderViewBean.MYTIMECARDS_MENU_DETAILS;
			}
			return HeaderViewBean.PAYROLL_TIMECARD;
		}
		return null; // Save failed - still in edit mode
	}

	/**
	 * Action method for the Cancel button. Force a page refresh, since
	 * the Cancel button has "immediate=true" attribute.
	 */
	@Override
	public String actionCancel() {
		prodSelectDisabled = false;
		super.actionCancel();
		if (getNotInProduction()) {
			return HeaderViewBean.MYTIMECARDS_MENU_DETAILS;
		}
		return HeaderViewBean.PAYROLL_TIMECARD;
	}

	/**
	 * Check for timecard deleted by someone else.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionDelete()
	 */
	@Override
	public String actionDelete() {
		String res = super.actionDelete();
		if (weeklyTimecard == null) { // someone else deleted the current timecard
			weeklyTimecardList = null; // force list refresh
		}
		return res;
	}
	/**
	 * Method that deletes the current weekly timecard.
	 * @return null navigation string
	 */
	@Override
	public String actionDeleteOk() {
		super.actionDeleteOk();
		weeklyTimecardList = null; // force list refresh
		return null;
	}

	/**
	 * Action method for the Print button.  Display the print options
	 * pop-up dialog.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionPrint() {
		super.actionPrint();
		if (weeklyTimecard == null) { // someone else deleted the current timecard
			weeklyTimecardList = null; // force list refresh
		}
		return null;
	}

	/**
	 * Print the timecards as requested by the user.
	 * @return null navigation string
	 */
	@Override
	protected String actionPrintOk() {
		String res = super.actionPrintOk();
		if (weeklyTimecard == null) {	// timecard was deleted
			weeklyTimecardList = null;	// force list refresh
		}
		return res;
	}

	/**
	 * Action method for the "legend" button to switch to viewing this time card
	 * in the "Full Timecard" page.
	 *
	 * @return Navigation string for Full Timecard page, or null navigation
	 *         string if the timecard has been deleted.
	 */
	public String actionViewFull() {
		weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
		if (weeklyTimecard == null) { // someone else deleted the current timecard
			weeklyTimecardList = null; // force refresh
			MsgUtils.addFacesMessage("Timecard.View.Deleted", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
		SessionUtils.put(Constants.ATTR_WEEKEND_DATE, weeklyTimecard.getEndDate());
		SessionUtils.put(Constants.ATTR_TIMECARD_VIEW, 1);
		if (getNotInProduction()) {
			return HeaderViewBean.MYTIMECARDS_FULL_TC;
		}
		else {
			return HeaderViewBean.PAYROLL_FULL_TC;
		}
	}

	/**
	 * ValueChangeListener method for the 'adjusted timecard' checkbox in the
	 * Create Timecard dialog.
	 *
	 * @param event contains old and new values
	 */
	public void listenAdjustedTimecard(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			createOccupationDL();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener method for the project drop-down on the Create
	 * Timecard dialog box.
	 *
	 * @param evt Event supplied by the framework, containing the new value,
	 *            which will be a Project.id.
	 */
	public void listenProject(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			if (id != null) {
				createProject = ProjectDAO.getInstance().findById(id);
				if (createProject != null) {
					createProjectId = id;
					createOccupationDL();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener method for the occupation drop-down list on the
	 * Create Timecard pop-up.
	 *
	 * @param evt The change event generated by the framework.
	 */
	public void listenStartForm(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			if (id != null) {
				setStartFormId(id);
			}
			StartForm sd = StartFormDAO.getInstance().findById(getStartFormId());
			createWeekEndDateDL(sd);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Determines the default ascending/descending order for the given column name.
	 *
	 * @param sortColumn Name of the column for which the default order is requested.
	 * @return whether sortColumn's default order is ascending or descending.
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (sortColumn.equals(WeeklyTimecard.SORTKEY_DATE) || sortColumn.equals(WeeklyTimecard.SORTKEY_HOURS)) {
			return false; // Date & hours default to descending
		}
		return true;	// all other columns default to ascending
	}

	/**
	 * @see com.lightspeedeps.web.timecard.TimecardBase#setupUserChanged(com.lightspeedeps.model.User)
	 */
	@Override
	protected void setupUserChanged(User user) {
		super.setupUserChanged(user);
		calculateCreateFlag(null); // do this early, in case timecard doesn't get selected
	}

	/**
	 * Determine if the current user may have authority to create a timecard
	 * for the currently viewed employee.
	 * @param wtc
	 */
	private void calculateCreateFlag(WeeklyTimecard wtc) {
		createAuth = getCloneAuth() || getApprovalAuth() ||
				tcUser.getAccountNumber().equals(getvUser().getAccountNumber());
		if (! createAuth) { // not set yet, check if any approval authority
			Contact contact = SessionUtils.getCurrentContact();
			if (contact != null) {
				createAuth = ApproverUtils.isApprover(contact, getCommProject());
				if ((! createAuth) &&
						(wtc == null || wtc.getStatus() == ApprovalStatus.VOID)) {
					// not set yet, check if time-keeper. cloneAuth may be false if VOID wtc, or no wtc
					createAuth = ApproverUtils.isTimeKeeper(contact, getCommProject());
					if (! createAuth) {
						createAuth = authBean.getPseudoApprover(); // last chance, may be super-user.
					}
				}
			}
		}
		if (createAuth && ! getNotInProduction()) {
			// Turn off createAuth if the production is read-only
			createAuth = SessionUtils.getNonSystemProduction().getStatus().getAllowsWrite();
		}
	}

	/**
	 * Update the lists used to populate the drop-downs in the Create Timecard
	 * dialog box.
	 *
	 * @return True iff the current logged-in user has access to at least one
	 *         StartForm for the currently selected employee.
	 */
	private boolean updateCreateDialogLists() {
		boolean bRet = false;
		if (createProjectDL()) { // Create the list of available projects if Commercial production
			StartForm selectSd = createOccupationDL(createProject); // create list of Starts
			if (selectSd != null) {
				setStartFormId(selectSd.getId());
				createWeekEndDateDL(selectSd);
				bRet = true;
			}
			// if selectSd was null, no start form accessible - message was generated already.
		}
		return bRet;
	}

	/**
	 * Create the list of occupations (based on available Start Forms) and the
	 * list of week-ending dates for the selected Start Form.
	 */
	private void createOccupationDL() {
		StartForm selectSd = createOccupationDL(createProject); // create list of Starts
		if (selectSd != null) {
			setStartFormId(selectSd.getId());
			createWeekEndDateDL(selectSd);
		}
	}

	/**
	 * Create the list of Projects for the Create Timecard dialog box when the
	 * current production is a Commercial one. (An empty list is created for
	 * TV/Feature productions.) The list of projects is based on the available,
	 * valid Start Forms for the person being viewed.  For "non-aggregate" viewers,
	 * the list will contain at most the current project.
	 *
	 * @return True if (a) this is a TV/Feature production, or (b) this is a
	 *         Commercial production and there is at least one project
	 *         containing a valid, accessible StartForm. See
	 *         {@link #createOccupationDL(Project)} for notes about "accessible"
	 *         Starts.
	 */
	private boolean createProjectDL() {
		createProject = null;
		createProjectId = null;
		projectDL = new ArrayList<>();
		if (getViewProduction() == null || ! getViewProduction().getType().isAicp()) {
			return true;
		}
		Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getViewProduction());
		List<StartForm> sds = StartFormDAO.getInstance().findByContactProject(contact, getViewProject(), getViewProduction().getAllowOnboarding());
		if (sds.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.NeedsStartForm", FacesMessage.SEVERITY_ERROR);
			return false;
		}

		boolean accessAll = getvUser().equals(tcUser) || ApproverUtils.isProdApprover(getvContact())
				|| getAuthBean().getPseudoApprover();

		// create drop-down from all qualifying SFs except excluded ones
		StartForm lastSd = null;
		StartForm selectSd = null;
		List<StartForm> validSds = new ArrayList<>();
		for (StartForm sd : sds) {
			if (accessAll ||
					getCreateAllDepts() ||
					(sd.getEmployment() != null && getDeptIdList().contains(sd.getEmployment().getDepartment().getId()))) {
				if (sd.getAllowTimecardCreate() &&
						sd.getProject() != null && sd.getProject().getStatus() == AccessStatus.ACTIVE) { // LS-2863
					validSds.add(sd);
					lastSd = sd;
					if (existsWeekEndDate(sd)) {
						selectSd = sd;
					}
				}
			}
		}

		if (selectSd == null && // none qualified (accessible to user & dates available)
				lastSd != null) { // at least one accessible to user
			selectSd = lastSd; // so show it, even though no dates will be available.
		}
		if (selectSd != null) {
			if (createProjectDL(validSds)) {
				createProject = selectSd.getProject();
				createProjectId = createProject.getId();
			}
			else { // no active projects. LS-2863
				MsgUtils.addFacesMessage("Timecard.StartFormsUnavailable", FacesMessage.SEVERITY_ERROR);
			}
		}
		else {
			MsgUtils.addFacesMessage("Timecard.StartFormsUnavailable", FacesMessage.SEVERITY_ERROR);
		}
		return selectSd != null;
	}

	/**
	 * Create the list of occupations (from Start Forms) to populate the
	 * selection drop-down list on the Create Timecard popup. Note that the
	 * StartForm`s "accessible" to the current user will depend on the user's
	 * status as an approver. If the user is only a department approver, they
	 * can only create timecards for those departments for which they are an
	 * approver; StartForm`s for other departments will not be "accessible" to
	 * them.
	 *
	 * @param proj The project that the Start Forms should be associated with;
	 *            this only applies to commercial productions. If null, no
	 *            filtering by Project will be done.
	 * @return A StartForm that the current user is allowed to create timecards
	 *         for. If at least one StartForm is associated with missing
	 *         timecards, it will be the one returned. If all timecards have
	 *         been created for all accessible StartForm`s, then any accessible
	 *         StartForm will be returned. If no accessible StartForm`s are
	 *         found, then null is returned. In the cases where null is
	 *         returned, a message is generated to tell the user what happened.
	 */
	private StartForm createOccupationDL(Project proj) {
		if (getViewProduction() == null) { // unusual! rev 3.1.6774
			return null;
		}
		Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, getViewProduction());
		List<StartForm> sds = StartFormDAO.getInstance().findByContactProject(contact, proj, getViewProduction().getAllowOnboarding());
		if (sds.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.NeedsStartForm", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		setOccupationDL(new ArrayList<SelectItem>());
		boolean accessAll = getvUser().equals(tcUser) || ApproverUtils.isProdApprover(getvContact())
				|| getAuthBean().getPseudoApprover();

		// create drop-down from all qualifying SFs except excluded ones
		StartForm lastSd = null;
		StartForm selectSd = null;
		int missingRequired = 0;
		for (StartForm sd : sds) {
			if (accessAll ||
					getCreateAllDepts() ||
					(sd.getEmployment() != null && getDeptIdList().contains(sd.getEmployment().getDepartment().getId()))) {
				if (sd.getAllowTimecardCreate()) {
					lastSd = sd;
					if (existsWeekEndDate(sd)) {
						selectSd = sd;
						getOccupationDL().add(new SelectItem(sd.getId(), sd.getJobClass() + " (" + sd.getSequence() + ")"));
					}
				}
				else {
					missingRequired++;
				}
			}
		}

		if (selectSd == null) { // none qualified (accessible to user & dates available)
			if (lastSd != null) { // at least one accessible to user
				selectSd = lastSd; // so show it, even though no dates will be available.
				getOccupationDL().add(new SelectItem(selectSd.getId(), selectSd.getJobClass() + " (" + selectSd.getSequence() + ")"));
				if (missingRequired > 0) {
					MsgUtils.addFacesMessage("Timecard.StartFormsIncomplete", FacesMessage.SEVERITY_INFO, missingRequired);
				}
			}
			else if (missingRequired > 0) {
				MsgUtils.addFacesMessage("Timecard.StartFormsAllIncomplete", FacesMessage.SEVERITY_ERROR);
			}
			else {
				MsgUtils.addFacesMessage("Timecard.NoStartFormInDepartment", FacesMessage.SEVERITY_ERROR);
			}
		}
		else if (missingRequired > 0) {
			MsgUtils.addFacesMessage("Timecard.StartFormsIncomplete", FacesMessage.SEVERITY_ERROR, missingRequired);
		}
		return selectSd;
	}

	/**
	 * Create the Project drop-down list that corresponds to the given set of
	 * StartForm`s.
	 *
	 * @param sds The List of StartForm`s whose related Project references
	 *            should be placed into the Project drop-down selection list.
	 * @return True iff the drop-down list contains at least one entry.
	 */
	private boolean createProjectDL(List<StartForm> sds) {
		projectDL.clear();
		Set<Project> projects = new HashSet<>();
		for (StartForm sd : sds) {
			Project proj = sd.getProject();
			if (proj != null && proj.getStatus() == AccessStatus.ACTIVE && ! projects.contains(proj)) {
				// LS-2849 only include Active projects
				projects.add(proj);
				projectDL.add(new SelectItem(proj.getId(), proj.getTitle()));
			}
		}
		return ! projectDL.isEmpty();
	}

	/**
	 * Create the drop-down list of week-ending dates for the Create Timecard
	 * pop-up. The list contains all the week-end dates starting with the given
	 * StartForm's work-start date or effective-start date (whichever is later),
	 * extending through the earlier of the work-end date or 10+ days from now,
	 * excluding dates for any existing timecards that correspond to the given
	 * StartForm. The last date included may be affected by the
	 * PayrollPreference.maxWeeksInAdvance value. The dates will fall on the day
	 * of the week specified in the PayrollPreference.weekFirstDay field,
	 * typically Saturday.
	 *
	 * @param sd The StartForm the timecard will be related to.
	 */
	private void createWeekEndDateDL(StartForm sd) {

		// get a list of the dates for which no timecard exists, and which
		// are valid for the given StartForm
		List<Date> dateList = createMissingWeekEndDateList(sd);

		// Turn the dateList into a list of SelectItem in weekEndDateDL
		createWeekEndDateDL(dateList);
	}

	/**
	 * Create a list of the dates for which no timecard exists, and which are
	 * valid for the given StartForm.
	 *
	 * @param sd The StartForm of interest.
	 * @return A List of Date`s for which a timecard may still be created for
	 *         the given Start.
	 */
	private List<Date> createMissingWeekEndDateList(StartForm sd) {
		//SimpleDateFormat sdfDebug = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

		// Get all possible dates for this Start Form, regardless of existing timecards
		List<Date> dateList = createPossibleWeekEndDateList(sd);

		if ((! adjustedTimecard) && dateList.size() > 0) {
			// If 'adjusted' is not selected, remove any dates for timecards that already exist.
			// Note that if dateList is empty, there's no need to go through this process.

			// Find dates for which a timecard has already been created
			List<Date> existingDates = getWeeklyTimecardDAO().findDatesForUserAccountOccupation(
					getViewProduction(), sd.getProject(), tcUser.getAccountNumber(), sd.getJobClass());
			// remove those dates from the list of all valid dates
			for (Date edate : existingDates) {
				dateList.remove(edate);
			}
		}

		return dateList;
	}

	/**
	 * Create a list of all possible W/E dates for a given StartForm, regardless
	 * of any existing timecards. This is based on the start and end dates in
	 * the StartForm, and may be affected by the
	 * PayrollPreference.maxWeeksInAdvance value.
	 *
	 * @param sd The StartForm of interest.
	 * @return A non-null, but possibly empty, list of Date values.
	 */
	private List<Date> createPossibleWeekEndDateList(StartForm sd) {
		//SimpleDateFormat sdfDebug = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

		// note: only use production preference for max weeks
		PayrollPreference pref = TimecardUtils.findProduction(sd).getPayrollPref();

		// Get last week-end date to show, based on ending dates in StartForm
		Date lastDate = TimecardUtils.calculateLastNewDate(sd, pref);
		//log.debug(sdfDebug.format(lastDate));

		Calendar cal = Calendar.getInstance();
		cal.setTime(sd.getWorkStartOrHireDate());
		if (sd.getEffectiveStartDate() != null &&
				sd.getEffectiveStartDate().after(sd.getWorkStartOrHireDate())) {
			cal.setTime(sd.getEffectiveStartDate());
		}
		// Set the calendar to the week-ending day of the user's first week
		int wkEndDay = TimecardUtils.findWeekEndDay(sd); // pref.getWeekEndDay();
		cal.set(Calendar.DAY_OF_WEEK, wkEndDay);
		Date date = cal.getTime();
		//log.debug("date=" + sdfDebug.format(date) + ", lastdate=" + sdfDebug.format(lastDate));

		List<Date> dateList = new ArrayList<>();
		while(! date.after(lastDate)) {
			dateList.add(date);
			cal.add(Calendar.DAY_OF_MONTH, 7);
			date = cal.getTime();
		}
		return dateList;
	}

	/**
	 * Create a new List instance for the {@link #weekEndDateDL} field,
	 * containing a list of SelectItem entries created from the Date values
	 * provided.  Each SelectItem created has a value of type Date, and
	 * a label of a String representation of that Date.
	 *
	 * @param dateList The List of Date values to put in the list.
	 */
	private void createWeekEndDateDL(List<Date> dateList) {
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);

		weekEndDateDL = new ArrayList<>();

		for (Date date : dateList) {
			weekEndDateDL.add(0,new SelectItem(date,sdf.format(date)));
		}
		if (weekEndDateDL.size() == 0) {
			setWeekEndDate(new Date(0)); // weekEndDate not useable; actionCreate will check.
		}
		else {
			setWeekEndDate((Date)weekEndDateDL.get(0).getValue());
		}
	}

	/**
	 * Determine if the specified StartForm includes any week-ending dates for
	 * which there is no WeeklyTimecard. If so, return true, else return false.
	 * This is used to determine which StartForm`s are listed in the drop-down
	 * list within the "Create Timecard" dialog box.
	 *
	 * @param sd The StartForm of interest.
	 * @return True iff the selection of this StartForm on the "Create Timecard"
	 *         pop-up would result in the listing of at least one week-ending
	 *         date for which a timecard could be created.
	 */
	protected boolean existsWeekEndDate(StartForm sd) {
		boolean exists = false;

		// Get all possible dates for this Start Form, regardless of existing timecards
		List<Date> dateList = createPossibleWeekEndDateList(sd);

		if (dateList.size() > 0) { // Any possible dates for this SF?
			if (adjustedTimecard) { // if 'adjusted' selected, then
				exists = true;	// all possible dates are available.
			}
			else {
				// If 'adjusted' is not selected, remove any dates for timecards that already exist.
				// Find dates for which a timecard has already been created
				List<Date> existingDates = getWeeklyTimecardDAO().findDatesForUserAccountOccupation(
						getViewProduction(), sd.getProject(), tcUser.getAccountNumber(), sd.getJobClass());
				if (dateList.size() > existingDates.size()) {
					exists = true; // will be at least one date available
				}
				else {
					// remove those dates from the list of all valid dates
					for (Date edate : existingDates) {
						dateList.remove(edate);
					}
					if (dateList.size() > 0) {
						exists = true; // at least one date available
					}
				}
			}
		}

		return exists;
	}

	/**
	 * This method is used by the "Create timecard" dialog box, to determine if
	 * the "adjusted" option should be available.
	 *
	 * @return true if user is an approver or has Edit HTG privileges.
	 */
	public boolean getApprover() {
		if ((authBean != null && authBean.getPseudoApprover())
				|| getUserHasEditHtg()) {
			return true;
		}
		Contact contact = ContactDAO.getInstance().refresh(getvContact()); // LS-1270
		return ApproverUtils.isApprover(contact, SessionUtils.getCurrentProject());
	}

	/**
	 * @see com.lightspeedeps.web.timecard.TimecardBase#statusChanged(com.lightspeedeps.model.WeeklyTimecard)
	 */
	@Override
	protected void statusChanged(WeeklyTimecard wtc) {
		super.statusChanged(wtc);
		updateItemInList(wtc);
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public List getItemList() {
		return getWeeklyTimecardList();
	}

	/** See {@link #adjustedTimecard}. */
	public boolean getAdjustedTimecard() {
		return adjustedTimecard;
	}
	/** See {@link #adjustedTimecard}. */
	public void setAdjustedTimecard(boolean adjustedTimecard) {
		this.adjustedTimecard = adjustedTimecard;
	}

	/**See {@link #createAuth}. */
	public boolean getCreateAuth() {
		return createAuth;
	}
	/**See {@link #createAuth}. */
	public void setCreateAuth(boolean createAuth) {
		this.createAuth = createAuth;
	}

	/** See {@link #prodSelectDisabled}. */
	public boolean getProdSelectDisabled() {
		return prodSelectDisabled;
	}
	/** See {@link #prodSelectDisabled}. */
	public void setProdSelectDisabled(boolean prodSelectDisabled) {
		this.prodSelectDisabled = prodSelectDisabled;
	}

	/** See {@link #showCreate}. */
	@Override
	public boolean getShowCreate() {
		return showCreate;
	}
	/** See {@link #showCreate}. */
	@Override
	public void setShowCreate(boolean showCreate) {
		this.showCreate = showCreate;
	}

	@Override
	public boolean getShowOccupationDL() {
		return (occupationDL != null && occupationDL.size() > 1);
	}

	/** See {@link #occupationDL}. */
	@Override
	public List<SelectItem> getOccupationDL() {
		return occupationDL;
	}
	/** See {@link #occupationDL}. */
	@Override
	public void setOccupationDL(List<SelectItem> occupationDL) {
		this.occupationDL = occupationDL;
	}

	/** See {@link #startFormId}. */
	@Override
	public Integer getStartFormId() {
		return startFormId;
	}
	/** See {@link #startFormId}. */
	@Override
	public void setStartFormId(Integer startFormId) {
		this.startFormId = startFormId;
	}

	public boolean getEnableProjectDL() {
		return (projectDL != null && projectDL.size() > 1);
	}

	/** See {@link #projectDL}. */
	public List<SelectItem> getProjectDL() {
		return projectDL;
	}
	/** See {@link #projectDL}. */
	public void setProjectDL(List<SelectItem> projectDL) {
		this.projectDL = projectDL;
	}

	/** See {@link #createProjectId}. */
	public Integer getCreateProjectId() {
		return createProjectId;
	}
	/** See {@link #createProjectId}. */
	public void setCreateProjectId(Integer createProjectId) {
		this.createProjectId = createProjectId;
	}

	/** See {@link #weekEndDateDL}. */
	@Override
	public List<SelectItem> getWeekEndDateDL() {
		return weekEndDateDL;
	}
	/** See {@link #weekEndDateDL}. */
	@Override
	public void setWeekEndDateDL(List<SelectItem> createDateDL) {
		weekEndDateDL = createDateDL;
	}

	/**
	 * Invoke the base class' preDestroy method.
	 * @see com.lightspeedeps.web.timecard.TimecardBase#preDestroy()
	 */
	@Override
	@PreDestroy
	public void preDestroy() {
		log.debug("");
		super.preDestroy();
	}

}
