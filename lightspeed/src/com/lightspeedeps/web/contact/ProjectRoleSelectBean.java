package com.lightspeedeps.web.contact;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.event.UnselectEvent;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.type.EmpStatus;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.payroll.StartFormUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.View;

/**
 * This is the backing bean for the Project/Role selection pop-up used on the
 * Contacts (People) page for adding a new position for a Contact.
 * <p>
 * The additional function this class adds over RoleSelectBean is that the Project
 * may be changed, which, in turn, changes the list of Unit`s displayed.
 */
@ManagedBean
@ViewScoped
public class ProjectRoleSelectBean extends RoleSelectBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 8408093229611152256L;

	private static final Log log = LogFactory.getLog(ProjectRoleSelectBean.class);

	/** The currently selected Project */
	private Project project;

	/** The database id of the currently selected project. */
	private Integer projectId = 0;

	/** The Contact to whom the new role is being added. This is used to find
	 * a "source" StartForm (if necessary) if we generate a new StartForm. */
	private Contact contact;

	/** Field for corresponding check-box on Create Role pop-up.  If checked by user,
	 * we will copy all data from latest StartForm into the new StartForm. */
	private boolean copyStartData;

	/** The StartForm selected as the source for copying data, if that is
	 * applicable. */
	private StartForm startForm;

	/** The display name for the StartForm to be used as the data source if we
	 * copy data into the new StartForm. This should have the same style as used
	 * in the Start Form page drop-down list. */
	private String startFormName;

	/** The effective start date to be assigned to the newly-created StartForm. */
	private Date workStartDate;

	/** True iff the current Production is a Commercial (not TV/Feature) production.
	 * Used to control actions related to StartForm creation. */
	private boolean commercial;

	private Date startDate;

	private Date endDate;

	private transient StartFormDAO startFormDAO;

	/** Currently selected contact's employment record */
	//private Employment currentEmployment;

	/** The list of checked DocRoWItems from the data table */
	private List<Project> projectCheckBoxSelectedItems = new ArrayList<>();

	/** The list of checked DocRoWItems from the data table */
	private List<Integer> unitCheckBoxSelectedItems = new ArrayList<>();

	/** ProjectMember DAO instance */
	private ProjectMemberDAO projectMemberDAO;

	/** Boolean field for displaying error message on the popup dialog */
	private boolean projectOrUnitError;

	/** Message string to be displayed on the popup dialog */
	private String errorMessage;

	/** New Employment instance that is instantiated if the user has clicked the create button on the positions table */
	protected Employment employmentToCreate;

	/** True iff the changeProject() method is called from the listenChangeProject listener
	 * and false if it is called from the setProject method.
	 */
	private boolean isProjectListener = false;

	/** True iff the user picked the "Use the same terms" check box */
	private boolean useSameTerms = false;

	/** Database id of the currently selected StartForm */
	private Integer startFormId;

	ContactViewBean contactViewBean = ContactViewBean.getInstance();

	/** To keep track of the last selected Project from the project/episode data table */
	private Project  lastSelectedProject = null;

	/** True if the user has selected a project/episode from the data table on the pop up */
	private boolean displayUnits = false;

	/** Map used to hold the units selected for a project. */
	private Map<Integer, List<Unit>> projectUnitMap = new HashMap<>();

	public ProjectRoleSelectBean() {
	}

	public static ProjectRoleSelectBean getInstance() {
		return (ProjectRoleSelectBean)ServiceFinder.findBean("projectRoleSelectBean");
	}

	/**
	 * Display the Role-selection pop-up dialog.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param emp The Employment associated with the new role.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box or null.
	 * @param messageId The message-id of the extra text to be displayed in the
	 *            pop-up.
	 * @see com.lightspeedeps.web.contact.RoleSelectBean#show(com.lightspeedeps.web.popup.PopupHolder,
	 *      int, String, String)
	 */
	public void show(PopupHolder holder, Employment emp, int act, String prefix, boolean isCreateOrEdit) {
		super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
		setCreate(isCreateOrEdit); // true for create
		contact = emp.getContact();
		setEmployment(emp);
		production = SessionUtils.getNonSystemProduction(); // refresh
		production.getPayrollPref().getIncludeTouring(); // avoid LIE
		unitCheckBoxSelectedItems = new ArrayList<>();
		projectCheckBoxSelectedItems = new ArrayList<>();
		projectUnitMap.clear();
		setLastSelectedProject(null);
		setProjectOrUnitError(false);

		Production prod = SessionUtils.getProduction();
		commercial = prod.getType().isAicp();
		if (commercial) {
			findStartForm();
			workStartDate = new Date();
		}

		if (prod.getType().getEpisodic()) {
			View.addFocus("project");
		}
		else {
			if (project.getHasUnits()) {
				View.addFocus("unit");
			}
			else {
				View.addFocus("dept");
			}
		}
	}

	/**
	 * Listener method invoked when the user changes the selected value of the
	 * Project drop-down list.
	 * @param evt The change-event information.
	 */
	public void listenChangeProject(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		log.debug("project=" + id);
		if (id != null) {
			isProjectListener = true;
			setProjectId(id);
		}
		setRoleDL(null); // project change can affect available Roles
	}

	/**
	 * User picked a new Department - we may need to update the default
	 * StartForm to be used as a source for the copy.
	 *
	 * @see com.lightspeedeps.web.contact.RoleSelectBean#departmentChanged()
	 */
	@Override
	protected void departmentChanged() {
		super.departmentChanged();
		findStartForm();
	}

	/**
	 * User picked a new role - we may need to update the default StartForm to
	 * be used as a source for the copy.
	 *
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#changeRole(javax.faces.event.ValueChangeEvent)
	 */
	@Override
	public void changeRole(ValueChangeEvent evt) {
		super.changeRole(evt);
		findStartForm();
	}

	/**
	 * The user has picked a new Project: update fields as necessary.
	 *
	 * @param id The Project.id value of the selected project.
	 */
	protected void changeProject(Integer id) {
		if (projectId != null) {
			project = ProjectDAO.getInstance().findById(id);
			if (project != null) {
				setDepartmentDL(null); // force refresh of department list - may change for commercial productions
				setUnitId(project.getMainUnit().getId());
				// see if we need a source StartForm & if so, find one
				findStartForm();
				Iterator<Integer> itr = unitCheckBoxSelectedItems.iterator();
				while (itr.hasNext()) {
					Integer unitNum = itr.next();
					Unit unit = project.getUnit(unitNum);
					if (unit == null) {
						itr.remove();
					}
				}
				if (isProjectListener && (! getIsCreate())) {
					checkUnits(project);
				}
			}
		}
	}

	/**
	 * Find the right StartForm to use as the source for creating a new
	 * StartForm, and set up the dialog-box fields to display this.
	 */
	private void findStartForm() {
		copyStartData = false;
		startForm = null;
		startFormName = null;
		if (commercial && (! getAdminDept())) {
			StartForm sf = null;
			if (getRole() != null) {
				sf = getStartFormDAO().findByContactPosition(contact, getRole().getName());
			}
			if (sf == null) {
				sf = getStartFormDAO().findLatestForContact(contact);
			}
			if (sf != null) {
				copyStartData = true;
				startForm = sf;
				startFormName = StartFormUtils.createStartFormLabel(sf);
			}
		}
	}

	/**
	 * Override the superclass version, since we may want the custom departments
	 * that are specific to the project selected in our drop-down, not the
	 * "current project" that the user has active (i.e., in the page header).
	 *
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#createDepartmentDL()
	 */
	@Override
	protected void createDepartmentDL() {
		ProductionType type = production.getType();
		if (type.isTours()) { // Tours production - use superclass. No project involved; uses dept mask.
			super.createDepartmentDL();
		}
		else if (! type.isAicp()) { // TV/Feature - no project-specific settings; ignore dept mask
			departmentDL = DepartmentUtils.getDepartmentDL2(SessionUtils.getCurrentContact(), null);
		}
		else {
			PayrollPreference pref = production.getPayrollPref();
			pref = PayrollPreferenceDAO.getInstance().refresh(pref);
			if (pref.getIncludeTouring()) {
				 // "Hybrid" production - use superclass. Uses dept mask.
				super.createDepartmentDL();
			}
			else {
				departmentDL = DepartmentUtils.getDepartmentDL2(SessionUtils.getCurrentContact(), project);
			}
		}
	}

	/**
	 * Override superclass version so that we can use the project selected in
	 * our drop-down, not the "current project".
	 *
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#createRoleDL(java.lang.Integer)
	 */
	@Override
	protected void createRoleDL(Integer deptId) {
		createRoleDL(deptId, project);
	}

	/**
	 * Method invoked from ContactViewBean when the user clicks "Save"
	 * on the Create occupation dialog box.
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#actionSave()
	 */
	@Override
	protected boolean actionSave() {
		if (production.getType().getCrossProject()) { // for Episodic add the last selected project to map
			if (lastSelectedProject != null && lastSelectedProject.getChecked()) {
				log.debug("lastSelectedProject : " + lastSelectedProject.getId() + ", " + lastSelectedProject.getTitle());
				if (projectUnitMap.containsKey(lastSelectedProject.getId())) {
					projectUnitMap.remove(lastSelectedProject.getId());
				}
				setProject(lastSelectedProject);
				if (getUnits() != null && ! getUnits().isEmpty()) {
					projectUnitMap.put(lastSelectedProject.getId(), getUnits());
					log.debug("projectUnitMap size : " + projectUnitMap.size());
				}
			}
		}
		if (! super.actionSave()) { // handles department/role fields
			return false;
		}
		return actionCreateOccupation();
	}

	/**
	 * Action method invoked when the user clicks Save on the create occupation
	 * pop up. It also checks for each project and unit records for a project
	 * member.
	 *
	 * @return True if no errors were found.
	 */
	private boolean actionCreateOccupation() {
		try {
			production = SessionUtils.getProduction();
			setProjectOrUnitError(false);

			if (production.getType().getFeature()) { // for feature add the default project
				isProjectListener = false;
				setProject(production.getDefaultProject());
				projectCheckBoxSelectedItems = new ArrayList<>();
				projectCheckBoxSelectedItems.add(getProject());
			}
			else if (production.getType().isAicp()) { // for commercial add the currently selected project
				projectCheckBoxSelectedItems = new ArrayList<>();
				projectCheckBoxSelectedItems.add(getProject());
			}
			if (getProject().getUnitCount() == 1) { // if project has only one unit then add that to the unit list as unit list will be hidden from UI
				unitCheckBoxSelectedItems = new ArrayList<>();
				unitCheckBoxSelectedItems.add(getProject().getUnits().get(0).getNumber());
			}
			if (projectCheckBoxSelectedItems == null || projectCheckBoxSelectedItems.isEmpty()) { // this case may arise when in episodic production
				// delete project members for any unit in the current project
				setProjectOrUnitError(true);
				displayError(MsgUtils.getMessage("ProjectRoleSelectBean.CheckProjects"));
				return false;
			}
			else if (unitCheckBoxSelectedItems == null || unitCheckBoxSelectedItems.isEmpty()) {
				if (! (production.getType().getCrossProject() && lastSelectedProject != null &&
						(! lastSelectedProject.getChecked()))) {
					setProjectOrUnitError(true);
					displayError(MsgUtils.getMessage("ProjectRoleSelectBean.CheckUnits"));
					return false;
				}
			}
			else { // project check box list is not empty
				/*if (! isCreate) {
					//contactViewBean.getSetToAttach().add(getEmployment());
					//EmploymentDAO.getInstance().attachDirty(getEmployment());
				}*/
				for (Project proj : production.getProjects()) {
					if (! updateProject(proj)) {
						//setProjectOrUnitError(true);
						//displayError(MsgUtils.getMessage("Contact.DuplicateRoles"));
						return false;
					}
				}
			}
			//forceRefresh();
			if (! getIsCreate() && contactViewBean.getSetToCreate().isEmpty()) {
				Employment emp = EmploymentDAO.getInstance().findById(getEmployment().getId());
				if (! getEmployment().getOccupation().equals(emp.getOccupation())) {
					PopupBean bean = PopupBean.getInstance();
					bean.show(null, 0, "CreateNewOccupationBean.OccupationChangeReminder.Title",
							"CreateNewOccupationBean.OccupationChangeReminder.Text", "Confirm.OK", null);
				}
			}
			return true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			return false;
		}
	}

	/**
	 * @param proj Project to be updated
	 */
	public boolean updateProject(Project proj) {
		if (projectCheckBoxSelectedItems.contains(proj)) {
			log.debug("selected project " + proj.getTitle());
			// Not null check is to avoid NPE, for the contacts who cannot create new roles.
			if (getIsCreate() && getRole() != null) {
				return createProjectMember(proj);
			}
			else {
				validateProject(proj);
			}
		}
		else if (! getIsCreate()) {
			// un-checked project -- delete ProjectMembers (if any) that relate to
			// this project for the current Employment instance.
			// find PMs and delete
			log.debug("project id" + proj.getId());
			if (getEmployment().getId() != null) {
				Map<String, Object> values = new HashMap<>();
				values.put("employment", getEmployment());
				values.put("project", proj);
				List<ProjectMember> list = getProjectMemberDAO()
						.findByNamedQuery(ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_PROJECT, values);
				for (ProjectMember pm : list) {
					//getProjectMemberDAO().delete(pm);
					log.debug("delete - project member, added in set: " + pm.getId());
					contactViewBean.getSetToDelete().add(pm);
					log.debug("size of delete set: " + contactViewBean.getSetToDelete().size());
				}
			}
			else {
				Iterator<Object> itr = contactViewBean.getSetToCreate().iterator();
				log.debug("Size of Create set : " + contactViewBean.getSetToCreate().size());
				while (itr.hasNext()) {
					Object currObject = itr.next();
					if (currObject instanceof ProjectMember) {
						ProjectMember pm = (ProjectMember) currObject;
						if (pm.getEmployment().equals(getEmployment()) && pm.getUnit() != null &&
								(pm.getUnit().getProject().equals(proj))) {
							getEmployment().getProjectMembers().remove(pm);
							itr.remove();
							log.debug("Size of Create set : " + contactViewBean.getSetToCreate().size());
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 *
	 * @param proj Project to be validated.
	 */
	private void validateProject(Project proj) {
		try {
			for (Unit unit : proj.getUnits()) {
				log.debug("Unit : " + unit.getId() + ", " + unit.getName());
				// check for project member instance with emp and unit
				// if exists, unit checked in the check box list, if checked- do nothing
				// else delete the project member instance
				ProjectMember member = null;
				if (getEmployment().getId() == null) {
					for (Object currObject : contactViewBean.getSetToCreate()) {
						if (currObject instanceof ProjectMember) {
							ProjectMember pm = (ProjectMember) currObject;
							if ((pm.getEmployment().equals(getEmployment())) && (pm.getUnit().equals(unit))) {
								member = (ProjectMember) currObject;
								break; // only one should match
							}
						}
					}
				}
				else {
					Map<String, Object> values = new HashMap<>();
					values.put("employment", getEmployment());
					values.put("unit", unit);
					member = getProjectMemberDAO().findOneByNamedQuery(ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_UNIT, values);
					if (member == null && contactViewBean.getSetToCreate() != null && ! contactViewBean.getSetToCreate().isEmpty()) {
						for (Object currObject : contactViewBean.getSetToCreate()) {
							if (currObject instanceof ProjectMember) {
								ProjectMember pm = (ProjectMember) currObject;
								if (pm.getEmployment().equals(getEmployment()) && pm.getUnit().equals(unit)) {
									member = (ProjectMember) currObject;
								}
							}
						}
					}
				}

				//Create Unit list according to the production.
				List<Unit> projectUnitList = new ArrayList<>();
				if (production.getType().getCrossProject()) {
					if (projectUnitMap.containsKey(proj.getId())) {
						projectUnitList = projectUnitMap.get(proj.getId());
					}
				}
				else {
					for (Integer unitNumber : unitCheckBoxSelectedItems) {
						Unit u = proj.getUnit(unitNumber);
						projectUnitList.add(u);
					}
				}
				if (member != null) {
					if (! projectUnitList.contains(unit)) {
						if (member.getId() == null) {
							contactViewBean.getSetToCreate().remove(member);
							log.debug("Removed new project member from create set: " + member +
									"........size of create set: "+ contactViewBean.getSetToCreate().size());
							if (getEmployment() != null && getEmployment().getId() == null) {
								getEmployment().getProjectMembers().remove(member);
							}
						}
						else {
							contactViewBean.getSetToDelete().add(member);
							getEmployment().getProjectMembers().remove(member);
							log.debug("delete - project member, added in delete set: " + member.getId() +
									"..........size of delete set: " + contactViewBean.getSetToDelete().size());
						}
						//getProjectMemberDAO().delete(member);
					}
					// condition for employment.getId() != null
					else if (projectUnitList.contains(unit) && contactViewBean.getSetToDelete().contains(member)) {
							contactViewBean.getSetToDelete().remove(member);
							getEmployment().getProjectMembers().add(member);
							log.debug("size of SetToDelete : " + contactViewBean.getSetToDelete().size());
					}
				}
				else {
					// if no project member exits for emp n unit
					if (projectUnitList.contains(unit)) { // if unit in check box list, create project member
						member = new ProjectMember(unit, getEmployment()); 	// else do nothing
						//Integer memberId = getProjectMemberDAO().save(member);
						if (! contactViewBean.getSetToCreate().contains(member)) {
							contactViewBean.getSetToCreate().add(member);
							getEmployment().getProjectMembers().add(member);
							log.debug("Create new project member, added in set: " + member +
									".....size of create set: " +  contactViewBean.getSetToCreate().size());
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/** Method creates the new project member instances for the checked units
	 * and the current employment record
	 * @param proj
	 */
	private boolean createProjectMember(Project proj) {
		Integer empId = employmentToCreate.getId();
		if (empId == null && ! contactViewBean.getSetToCreate().contains(employmentToCreate)) {
			log.debug("contact id " + employmentToCreate.getContact().getId());
			employmentToCreate.setRole(getRole());
			if (employmentToCreate.getRole() == null) {
				MsgUtils.addFacesMessage("Contact.MissingOccupation", FacesMessage.SEVERITY_ERROR);
				return false;
			}
			employmentToCreate.setOccupation(getRole().getName());
			Long mask = null;
			if (getRoleMasks() != null) { // map provided by caller (ContactViewBean)
				mask = getRoleMasks().get(getRole().getId()); // see if there's a matching role w/mask.
			}
			if (mask == null) {
				if (getRole().getRoleGroup() != null) {
					mask = getRole().getRoleGroup().getPermissionMask(); // default mask
				}
				else { // This shouldn't happen!
					// See rev 3.1.7450 & 3.2.8732 comments re Role.getRoleGroup()==null errors
					mask = 577022603114315777L; // hard-code default mask - RoleGroup 12 (General Crew)
					String msg = "Null Role Group for role: " + getRole().getId() + ", " + getRole().getName();
					log.debug(msg);
					EventUtils.logError(msg);
				}
			}
			employmentToCreate.setPermissionMask(mask);
			employmentToCreate.setDisableEmpDefRole(false);
			// Code to set the defRole true for the first employment of a project in commercial production.
			if (production.getType().isAicp()) {
				boolean singleEmployment = true;
				for (Employment emp : contact.getEmployments()) {
					if (emp.getProject() == null || emp.getProject().equals(proj)) {
						singleEmployment = false;
						emp.setDisableEmpDefRole(false);
						break;
					}
				}
				for (Object empl : contactViewBean.getSetToCreate()) {
					if (empl instanceof Employment) {
						if (((Employment) empl).getProject() == null || ((Employment) empl).getProject().equals(proj)) {
							singleEmployment = false;
							((Employment) empl).setDisableEmpDefRole(false);
							break;
						}
					}
				}
				if (singleEmployment) {
					employmentToCreate.setDefRole(true);
					employmentToCreate.setDisableEmpDefRole(true);
				}
			}
			else {
				if(contact.getEmployments().size() == 1) {
					contact.getEmployments().get(0).setDisableEmpDefRole(false);
				}
				else if (contact.getEmployments().isEmpty()) {
					employmentToCreate.setDisableEmpDefRole(true);
					employmentToCreate.setDefRole(true);
				}
			}
			if (employmentToCreate.getRole().isProductionWide() || ! production.getType().isAicp()) {
				employmentToCreate.setProject(null);
			}
			else {
				employmentToCreate.setProject(proj);
			}
			if (checkDuplicateRole(employmentToCreate, employmentToCreate.getRole())) {
				return false;
			}
			//empId = EmploymentDAO.getInstance().save(employmentToCreate);
			contact.getEmployments().add(employmentToCreate);
			contactViewBean.getSetToCreate().add(employmentToCreate);
			// TO enable primary check box if there was only single employment earlier.
			if (contactViewBean.getContactEmploymentList().size() == 1) {
				contactViewBean.getContactEmploymentList().get(0).setDisableEmpDefRole(false);
			}
			contactViewBean.getContactEmploymentList().add(employmentToCreate);
			log.debug("Create new employment, added in set: " + employmentToCreate +
					" size of create set: " +  contactViewBean.getSetToCreate().size());
		}
		if (employmentToCreate.getDepartmentId().equals(Constants.DEPARTMENT_ID_LS_ADMIN) ||
				employmentToCreate.getDepartmentId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) {
			log.debug("Create new employment, for admin " );
			ProjectMember newProjectMember = new ProjectMember(null, employmentToCreate);
			contactViewBean.getSetToCreate().add(newProjectMember);
			if (! employmentToCreate.getProjectMembers().contains(newProjectMember)) {
				employmentToCreate.getProjectMembers().add(newProjectMember);
			}
		}
		else {
			List<Unit> selectedUnitList = new ArrayList<>();
			if (production.getType().getCrossProject()) {
				if (projectUnitMap.containsKey(proj.getId())) {
					selectedUnitList = projectUnitMap.get(proj.getId());
				}
			}
			else {
				for (Integer unitNumber : unitCheckBoxSelectedItems) {
					Unit unit = proj.getUnit(unitNumber);
					selectedUnitList.add(unit);
				}
			}
			for (Unit unit : selectedUnitList) {
			//for (Integer unitNumber : unitCheckBoxSelectedItems) {
				//Unit unit = project.getUnit(unitNumber);
				ProjectMember newProjectMember = new ProjectMember(unit, employmentToCreate);
				contactViewBean.getSetToCreate().add(newProjectMember);
				log.debug("Create new project member, added in set: " + newProjectMember +
						".....size of create set: " +  contactViewBean.getSetToCreate().size());
				if (! employmentToCreate.getProjectMembers().contains(newProjectMember)) {
					employmentToCreate.getProjectMembers().add(newProjectMember);
				}
				boolean talent = production.getType().isTalent();
				if (production.getType().isAicp() || talent) {
					if (useSameTerms || talent) {
						StartForm relatedStartForm = null;
						if (startFormId != null) {
							relatedStartForm = getStartFormDAO().findById(startFormId);
						}
						boolean saveIt = (empId != null);
						StartForm newSf = StartFormService.createStartForm(StartForm.FORM_TYPE_NEW, relatedStartForm, relatedStartForm,
								contact, employmentToCreate, proj, CalendarUtils.todaysDate(), null, null,
								(relatedStartForm != null), // copy if we have a source form
								saveIt);
						Date date = getWorkStartDate();
						newSf.setWorkStartDate(date);
						if (date.before(newSf.getHireDate())) {
							newSf.setHireDate(date);
						}
						if (! saveIt) {
							log.debug("");
							contactViewBean.getSetToCreate().add(newSf);
							boolean showStarts = contactViewBean.showStartsIcon(employmentToCreate);
							employmentToCreate.setShowStartsIcon(showStarts);
							employmentToCreate.setStatus(EmpStatus.STARTED);
						}
					}
				}
			}
		}
		return true;
	}

	/** Method used to fill the {@link #projectUnitMap} for TV Episodic production,
	 * this	method is called when the user View/Edit an occupation/employment.
	 *
	 * @return projectUnitMap with values for current employment.
	 */
	protected Map<Integer, List<Unit>> fillProjectUnitMap() {
		projectUnitMap = new HashMap<>();
		log.debug("");
		production = ProductionDAO.getInstance().refresh(production);
		if (production.getType().getCrossProject() && getEmployment() != null) {
			for (Project proj : production.getProjects()) {
				log.debug("Project :"  + proj.getId() + ", " + proj.getTitle());
				List<Unit> projectUnitList = new ArrayList<>();
				if (getEmployment().getId() == null) {
					for (Object currObject : contactViewBean.getSetToCreate()) {
						if (currObject instanceof ProjectMember) {
							ProjectMember pm = (ProjectMember) currObject;
							if (pm.getEmployment().equals(getEmployment()) && pm.getUnit() != null && pm.getUnit().getProject().equals(proj)) {
								log.debug("Unit : " + pm.getUnit().getId() + ", " + pm.getUnit().getName());
								projectUnitList.add(pm.getUnit());
							}
						}
					}
				}
				else {
					Map<String, Object> values = new HashMap<>();
					values.put("employment", getEmployment());
					values.put("project", proj);
					List<ProjectMember> list = getProjectMemberDAO()
							.findByNamedQuery(ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_PROJECT, values);
					for (ProjectMember p : list) {
						log.debug("Unit : " + p.getUnit().getId() + ", " + p.getUnit().getName());
						projectUnitList.add(p.getUnit());
					}
					if (contactViewBean.getSetToCreate() != null && ! contactViewBean.getSetToCreate().isEmpty()) {
						log.debug("");
						for (Object currObject : contactViewBean.getSetToCreate()) {
							if (currObject instanceof ProjectMember) {
								ProjectMember pm = (ProjectMember) currObject;
								if (pm.getEmployment().equals(getEmployment()) && pm.getUnit() != null && pm.getUnit().getProject().equals(proj)) {
									projectUnitList.add(pm.getUnit());
									log.debug("Unit : " + pm.getUnit().getId() + ", " + pm.getUnit().getName());
								}
							}
						}
					}
				}
				log.debug("Unit list size : " + projectUnitList.size());
				if (! projectUnitList.isEmpty()) {
					projectUnitMap.put(proj.getId(), projectUnitList);
				}
				log.debug("projectUnitMap size : " + projectUnitMap.size());
			}
		}
		return projectUnitMap;
	}

	/** Value change listener for individual checkbox's checked / unchecked event [episode table]
	 * @param evt
	 */
	public void listenProjectSingleCheck (ValueChangeEvent evt) {
		log.debug("");
		Project proj = (Project) evt.getComponent().getAttributes().get("selectedRow");
		if (proj.getChecked()) {
			projectCheckBoxSelectedItems.add(proj);
			if (! projectUnitMap.containsKey(proj.getId())) {
				proj = ProjectDAO.getInstance().refresh(proj);
				proj.setChecked(true);
				Unit firstUnit = proj.getUnits().get(0);
				firstUnit.setChecked(true);
				if (! getUnitCheckBoxSelectedItems().contains(firstUnit.getNumber()) && proj.equals(lastSelectedProject)) {
					getUnitCheckBoxSelectedItems().add(firstUnit.getNumber());
				}
				List<Unit> units = new ArrayList<>();
				units.add(firstUnit);
				getProjectUnitMap().put(proj.getId(), units);
				if (lastSelectedProject != null && proj.equals(lastSelectedProject)) {
					setUnitList(null);
				}
			}
		}
		else if (projectCheckBoxSelectedItems.contains(proj)) {
			projectCheckBoxSelectedItems.remove(proj);
			proj = ProjectDAO.getInstance().refresh(proj);
			if (projectUnitMap != null && projectUnitMap.containsKey(proj.getId())) {
				projectUnitMap.remove(proj.getId());
			}
			for (Unit unit : proj.getUnits()) {
				unit.setChecked(false);
				getUnitCheckBoxSelectedItems().remove(unit.getNumber());
			}
			setUnitList(null);
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event [unit table]
	 * @param evt
	 */
	public void listenUnitSingleCheck (ValueChangeEvent evt) {
		Unit unit = (Unit) evt.getComponent().getAttributes().get("selectedRow");
		if (unit != null) {
			if (unit.getChecked()) {
				unitCheckBoxSelectedItems.add(unit.getNumber());
			}
			else if (unitCheckBoxSelectedItems.contains(unit.getNumber())) {
				unitCheckBoxSelectedItems.remove(unit.getNumber());
			}
		}
	}

	/**
	 * Redisplay the popup with an error message. This method is useful to call
	 * from a holder's "confirmOk" method when an error is found in the input,
	 * and the program wishes to keep the pop-up displayed with an error
	 * message, to give the user an opportunity to correct the error.
	 *
	 * @param message The error message text to be displayed within the popup.
	 */
	public void displayError(String message) {
		setVisible(true);
		setErrorMessage(message);
	}

	/** Utility method used to put check marks on the unit table
	 * for the passed in project
	 * @param proj
	 */
	protected void checkUnits(Project proj) {
		// overridden by subclass(es)
	}

	/**
	 * Method used to refresh the employment list when a create/delete operation is performed
	 */
	/*private void forceRefresh() {
//		contactViewBean.setContactEmploymentList(null);
	}*/

	/** Listener sets the object returned from the data table into
	 * the local project instance, invoked every time the user selects a project/episode
	 * @param event
	 */
	public void listenProjectRowSelect(SelectEvent event) {
		try {
			log.debug("");
			if (lastSelectedProject != null) {
				setProject(lastSelectedProject);
				lastSelectedProject = ProjectDAO.getInstance().refresh(lastSelectedProject);
				List<Unit> selectedProjectUnitList = new ArrayList<>();
				selectedProjectUnitList = getUnits();
				if (projectUnitMap.containsKey(lastSelectedProject.getId())) {
					projectUnitMap.remove(lastSelectedProject.getId());
					if (selectedProjectUnitList != null && ! selectedProjectUnitList.isEmpty()) {
						projectUnitMap.put(lastSelectedProject.getId(), selectedProjectUnitList);
						log.debug("projectUnitMap size : " + projectUnitMap.size());
					}
				}
				else {
					if (selectedProjectUnitList != null && ! selectedProjectUnitList.isEmpty()) {
						projectUnitMap.put(lastSelectedProject.getId(), selectedProjectUnitList);
						log.debug("projectUnitMap size : " + projectUnitMap.size());
					}
				}
				for (Integer unitNumber : unitCheckBoxSelectedItems) {
					Unit unit = lastSelectedProject.getUnit(unitNumber);
					log.debug("Unit : " + unit.getId() + ", " + unit.getName());
					unit.setChecked(false);
				}
			}
			unitCheckBoxSelectedItems.clear();

			lastSelectedProject = (Project) event.getObject();
			setProject(lastSelectedProject);
			setDisplayUnits(true);

			if (projectUnitMap.containsKey(lastSelectedProject.getId())) {
				for (Unit unit : projectUnitMap.get(lastSelectedProject.getId())) {
					log.debug("Unit : " + unit.getId() + ", " + unit.getName());
					unit.setChecked(true);
					unitCheckBoxSelectedItems.add(unit.getNumber());
				}
			}
			else {
				log.debug("");
				checkUnits(lastSelectedProject);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/** Listener to perform operations on unselect event of episode row.
	 * @param UnselectEvent event
	 */
	public void listenProjectRowUnselect(UnselectEvent event) {
		try {
			log.debug("");
			displayUnits = false;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/** Utility method  returns list of unit with checked in unit numbers
	 * @return
	 */
	private List<Unit> getUnits() {
		List<Unit> unitList = new ArrayList<>();
		if (getProject() != null) {
			for (Integer unitNumber : unitCheckBoxSelectedItems) {
				Unit unit = getProject().getUnit(unitNumber);
				log.debug("Unit : " + unit.getId() + ", " + unit.getName());
				unit.setChecked(false);
				unitList.add(unit);
			}
		}
		return unitList;
	}

	/** See {@link #projectId}. */
	public Integer getProjectId() {
		return projectId;
	}
	/** See {@link #projectId}. */
	public void setProjectId(Integer id) {
		if (id != null && (! id.equals(projectId)) && id != 0) {
			changeProject(id);
			// CreateNewOccupationBean.getInstance().changeProject(id); //not necessary - line above will call it.
		}
		projectId = id;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project pProject) {
		project = pProject;
		projectId = 0;
		if (project != null) {
			setProjectId(project.getId());
		}
	}

	/** See {@link #copyStartData}. */
	public boolean getCopyStartData() {
		return copyStartData;
	}
	/** See {@link #copyStartData}. */
	public void setCopyStartData(boolean copyStartData) {
		this.copyStartData = copyStartData;
	}

	/** See {@link #startForm}. */
	public StartForm getStartForm() {
		return startForm;
	}

	/** See {@link #startFormName}. */
	public String getStartFormName() {
		return startFormName;
	}
	/** See {@link #startFormName}. */
	public void setStartFormName(String startFormName) {
		this.startFormName = startFormName;
	}

	/** See {@link #workStartDate}. */
	public Date getWorkStartDate() {
		return workStartDate;
	}
	/** See {@link #workStartDate}. */
	public void setWorkStartDate(Date workStartDate) {
		this.workStartDate = workStartDate;
	}

	/** See {@link #startFormDAO}. */
	public StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}

	/** See {@link #startDate}. */
	public Date getStartDate() {
		return startDate;
	}
	/** See {@link #startDate}. */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/** See {@link #endDate}. */
	public Date getEndDate() {
		return endDate;
	}
	/** See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #currentEmployment}. *//*
	public Employment getCurrentEmployment() {
		return currentEmployment;
	}
	*//** See {@link #currentEmployment}. *//*
	public void setCurrentEmployment(Employment currentEmployment) {
		this.currentEmployment = currentEmployment;
	}*/

	/**See {@link #projectCheckBoxSelectedItems}. */
	public List<Project> getProjectCheckBoxSelectedItems() {
		return projectCheckBoxSelectedItems;
	}
	/**See {@link #projectCheckBoxSelectedItems}. */
	public void setProjectCheckBoxSelectedItems(List<Project> projectCheckBoxSelectedItems) {
		this.projectCheckBoxSelectedItems = projectCheckBoxSelectedItems;
	}

	/**See {@link #unitCheckBoxSelectedItems}. */
	public List<Integer> getUnitCheckBoxSelectedItems() {
		return unitCheckBoxSelectedItems;
	}
	/**See {@link #unitCheckBoxSelectedItems}. */
	public void setUnitCheckBoxSelectedItems(List<Integer> unitCheckBoxSelectedItems) {
		this.unitCheckBoxSelectedItems = unitCheckBoxSelectedItems;
	}

	/** This method returns the ProjectMemberDAO instance
	 * @return projectMemberDAO
	 */
	protected ProjectMemberDAO getProjectMemberDAO() {
		if (projectMemberDAO == null) {
			projectMemberDAO = ProjectMemberDAO.getInstance();
		}
		return projectMemberDAO;
	}

	/**See {@link #projectOrUnitError}. */
	public boolean isProjectOrUnitError() {
		return projectOrUnitError;
	}
	/**See {@link #projectOrUnitError}. */
	public void setProjectOrUnitError(boolean projectOrUnitError) {
		this.projectOrUnitError = projectOrUnitError;
	}

	/**See {@link #errorMessage}. */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**See {@link #errorMessage}. */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/** See {@link #useSameTerms}. */
	public boolean isUseSameTerms() {
		return useSameTerms;
	}
	/** See {@link #useSameTerms}. */
	public void setUseSameTerms(boolean useSameTerms) {
		this.useSameTerms = useSameTerms;
	}

	/** See {@link #startFormId}. */
	public Integer getStartFormId() {
		return startFormId;
	}
	/** See {@link #startFormId}. */
	public void setStartFormId(Integer startFormId) {
		this.startFormId = startFormId;
	}

	/** See {@link #lastSelectedProject}. */
	public Project getLastSelectedProject() {
		return lastSelectedProject;
	}
	/** See {@link #lastSelectedProject}. */
	public void setLastSelectedProject(Project lastSelectedProject) {
		this.lastSelectedProject = lastSelectedProject;
	}

	/** See {@link #displayUnits}. */
	public boolean getDisplayUnits() {
		return displayUnits;
	}
	/** See {@link #displayUnits}. */
	public void setDisplayUnits(boolean displayUnits) {
		this.displayUnits = displayUnits;
	}

	protected void setUnitList(List<Unit> unitList) {
		// override by sub class
	}

	/** See {@link #projectUnitMap}. */
	public Map<Integer, List<Unit>> getProjectUnitMap() {
		return projectUnitMap;
	}
	/** See {@link #projectUnitMap}. */
	public void setProjectUnitMap(Map<Integer, List<Unit>> projectUnitMap) {
		this.projectUnitMap = projectUnitMap;
	}

}
