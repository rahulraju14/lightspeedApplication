package com.lightspeedeps.web.contact;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.StartFormUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the createNewOccupation pop up on the Cast & Crew page. It
 * is used to create new occupations for the currently selected Contact.
 */
@ManagedBean
@ViewScoped
public class CreateNewOccupationBean extends ProjectRoleSelectBean implements Serializable {

	private static final long serialVersionUID = -1786065180235299690L;

	private static final Log log = LogFactory.getLog(CreateNewOccupationBean.class);

	/** List of projects in the current production */
	private List<SelectItem> projectList = null;

	/** List of start forms for the current contact */
	private List<SelectItem> startFormList = null;

	/** List of projects in the current production */
	private List<Project> projectListDL = null;

	/**  List of units in the current project */
	private List<Unit> unitList;

	/** True iff the currently selected Project has single unit.
	 * Used to render unit table on create new occupation pop up.*/
	private boolean singleUnit = true;

	/** True for special project members.*/
	private boolean isSpecialMember = false;

	/** True iff the selected Employment's department is not in current contact's department list.
	 * So, department name will appear on UI instead of a disabled drop down  */
	private boolean showDepartmentLabel = false;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	public static CreateNewOccupationBean getInstance() {
		return (CreateNewOccupationBean)ServiceFinder.findBean("createNewOccupationBean");
	}

	/**
	 * default constructor
	 */
	public CreateNewOccupationBean() {
		super();
		setProject(SessionUtils.getCurrentProject());
	}

	@Override
	public void show(PopupHolder holder, Employment emp, int act, String prefix, boolean isCreateOrEdit) {
		RowState state = new RowState();
		state.setSelected(false);
		getStateMap().put(getLastSelectedProject(), state);
		getStateMap().remove(getLastSelectedProject());
		super.show(holder, emp, act, prefix, isCreateOrEdit);
		Project localProject = null;
		startFormList = null; // force refresh
		getUnitList();
		showDepartmentLabel = false;
		getDepartmentDL();
		if (! isCreateOrEdit) {
			showDepartmentLabel = true;
			for (SelectItem item : getDepartmentDL()) {
				if (item.getValue().equals(getDepartmentId())) {
					showDepartmentLabel = false;
					break;
				}
			}
		}
		setLastSelectedProject(null);
		isSpecialMember = false;
		if (getIsCreate()) {
			employmentToCreate = new Employment();
			employmentToCreate.setContact(emp.getContact());
			for (Unit unit : getProject().getUnits()) { // un-checks the unit table on create case
				unit.setChecked(false);
			}
			setProjectListDL(null);
			getProjectListDL();
		}
		else {
			/*if (emp.getProjectMembers() != null && emp.getRole().isProductionWide() && emp.getProjectMembers().get(0).getUnit() == null) {
				log.debug("special project member : " + getEmployment().getRole().getName());
				isSpecialMember = true;
			}*/
			setEmployment(emp);
			if (production.getType().getFeature()) {
				if (! getProjectCheckBoxSelectedItems().contains(production.getDefaultProject())) {
					getProjectCheckBoxSelectedItems().add(production.getDefaultProject()); // only 1 project in a feature
				}
				localProject = production.getDefaultProject();
			}
			else if (production.getType().getEpisodic()) {
				List<ProjectMember> pmList = new ArrayList<>();
				if (getEmployment().getId() == null) {
					for (Object currObject : contactViewBean.getSetToCreate()) {
						if (currObject instanceof Employment) {
							if (currObject.equals(getEmployment())) {
								pmList = ((Employment) currObject).getProjectMembers();
								log.debug("pm list size "+pmList.isEmpty());
							}
						}
					}
				}
				else {
					pmList = getProjectMemberDAO().findByProperty("employment", getEmployment());
					log.debug("pm list size "+pmList.isEmpty());
				}
				for (ProjectMember pm : pmList) {
					if (pm.getUnit() != null) {
						if (! getProjectCheckBoxSelectedItems().contains(pm.getUnit().getProject())) {
							getProjectCheckBoxSelectedItems().add(pm.getUnit().getProject());
						}
					}
					else {
						if (getEmployment().getRole().isProductionWide()) {
							isSpecialMember = true;
						}
					}
				}
				setProjectListDL(null);
				getProjectListDL();
			}
			/*if (production.getType().isAicp() && isCreateOrEdit) {
				localProject = SessionUtils.getCurrentProject();
			}*/
			if (production.getType().isAicp()) {
				localProject = getEmployment().getProject();
			}
			else { // feature or TV Episodic; or display only
				Contact contact = getEmployment().getContact();
				contact = ContactDAO.getInstance().refresh(contact);
				contact.getDisplayName();
			}
			checkUnits(localProject);
			setDisplayUnits(false);
			if (production.getType().getCrossProject()) {
				setProjectUnitMap(fillProjectUnitMap());
			}
		}
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		View.addButtonClicked();
		return super.actionCancel();
	}

	/**
	 * Method called by "X" icon built-in to ace:dialog header.
	 * Called due to ace:ajax tag in pop-ups.
	 * @param evt Ajax event from the framework.
	 */
	@Override
	public void actionClose(AjaxBehaviorEvent evt) {
		log.debug("");
		// 'visible' was already set to false before this method is
		// called.  Set it true, so 'actionCancel()' will take it's normal
		// path, and not assume this was a double-click of the Cancel button.
		setVisible(true);
		actionCancel(); // note that this will call a sub-class's actionCancel() if it exists.
	}

	/** Utility method used to put check marks on the unit table
	 * for the passed in project
	 * @param project
	 */
	@Override
	protected void checkUnits(Project project) {
		log.debug("");
		List<ProjectMember> list = new ArrayList<>();
		if (project != null) {
			getUnitCheckBoxSelectedItems().clear();
			if (getEmployment().getId() == null) { // if employment not persisted
				log.debug("");
				for (Object currObject : contactViewBean.getSetToCreate()) {
					if (currObject instanceof ProjectMember) {
						ProjectMember pm = (ProjectMember) currObject;
						if (pm.getEmployment().equals(getEmployment()) && pm.getUnit() != null && pm.getUnit().getProject().equals(project)) {
							list.add((ProjectMember) currObject);
						}
					}
				}
			}
			else {
				log.debug("");
				Map<String, Object> values = new HashMap<>();
				values.put("employment", getEmployment());
				values.put("project", project);
				list = getProjectMemberDAO()
						.findByNamedQuery(ProjectMember.GET_PROJECT_MEMBER_BY_EMPLOYMENT_PROJECT, values);
				log.debug("size = " + list.size());
				if (contactViewBean.getSetToCreate() != null && ! contactViewBean.getSetToCreate().isEmpty()) {
					log.debug("");
					for (Object currObject : contactViewBean.getSetToCreate()) {
						if (currObject instanceof ProjectMember) {
							ProjectMember pm = (ProjectMember) currObject;
							if (pm.getEmployment().equals(getEmployment()) && pm.getUnit() != null && pm.getUnit().getProject().equals(project)) {
								list.add((ProjectMember) currObject);
								log.debug("size : " + list.size());
							}
						}
					}
				}
			}
			//Special member case
			if (isSpecialMember) {
				for (Unit unit : getUnitList())	 {
					if (! getUnitCheckBoxSelectedItems().contains(unit.getNumber())) {
						getUnitCheckBoxSelectedItems().add(unit.getNumber());
					}
					unit.setChecked(true);
				}
			}
			else {
				for (ProjectMember pm : list) {
					boolean toUncheck = false; // uncheck = true, if project member is in delete set
					for (Object currObject : contactViewBean.getSetToDelete()) {
						if (currObject instanceof ProjectMember) {
							ProjectMember memberToDelete = (ProjectMember)currObject;
							if (pm.equals(memberToDelete)) {
								toUncheck = true;
								break;
							}
						}
					}
					Integer n = pm.getUnit().getNumber();
//					pm.getUnit().setChecked(true);
					if (! toUncheck) {
						if (getUnitList() != null) {
							getUnitList().get(n-1).setChecked(true);
						}
						if (! getUnitCheckBoxSelectedItems().contains(n)) {
							getUnitCheckBoxSelectedItems().add(n);
						}
					}
				}
			}
		}
	}

	/** Method invoked by the value change listener in the super class,
	 * which in turn is invoked by the user selecting a different project
	 * from the project drop down
	 * @param projectId
	 */
	@Override
	protected void changeProject(Integer projectId) {
		super.changeProject(projectId);
		if (projectId != null && projectId != 0) {
//			project = ProjectDAO.getInstance().findById(projectId); // done by super
			setUnitList(null);
		}
	}

	@Override
	protected void departmentChanged() {
		super.departmentChanged();
		if (getAdminDept()) {
			projectList = null;
			setProjectId(0);
		}
		else {
			if (getProjectList() != null && (! getProjectList().isEmpty()) && getProjectList().get(0).getValue().equals(0)) {
				projectList = null;
				if (getEmployment() != null && ! getIsCreate()) { // viewing existing Employment record
					setProject(getEmployment().getProject());
				}
				else {
					setProject(SessionUtils.getCurrentOrViewedProject());
				}
			}
		}
	}

	/** Method used to check the units for Project.
	 * @param selectedProject
	 * @param units
	 */
	private void checkSavedUnits(Project selectedProject, List<Unit> units) {
		List<Unit> projectUnitList = new ArrayList<>();
		if (getProjectUnitMap() != null && getProjectUnitMap().containsKey(selectedProject.getId()) && units != null) {
			projectUnitList = getProjectUnitMap().get(selectedProject.getId());
			Iterator<Unit> iter = units.iterator();
			while (iter.hasNext()) {
				Unit unit = iter.next();
				log.debug("Unit : " + unit.getId() + ", " + unit.getName());
				if (projectUnitList != null && projectUnitList.contains(unit)) {
					unit.setChecked(true);
					getUnitCheckBoxSelectedItems().add(unit.getNumber());
				}
			}
		}
	}

	/**
	 * Method used to prevent the addition of new (custom) roles by Team
	 * clients.
	 * <p>
	 * NOT CURRENTLY USED. Left in place for probably future use when we decide
	 * how non-union occupations will be restricted.
	 *
	 * @param ValueChangeEvent - event created by framework
	 */
	public void changeDepartmentNonUnion(ValueChangeEvent evt) {
		Production currentProduction = SessionUtils.getProduction();
		if (currentProduction.getPayrollPref() != null ?
			currentProduction.getPayrollPref().getPayrollService() != null ?
			currentProduction.getPayrollPref().getPayrollService().getTeamPayroll() : false : false)
		{
			this.setAllowNewRole(false);
		}
		super.changeDepartment(evt);
	}

	/** See {@link #projectList}. */
	public List<SelectItem> getProjectList() {
		if (projectList == null) {
			projectList = new ArrayList<>();
			if (getAdminDept()) {
				projectList.add(0, new SelectItem(0, "All Projects"));
			}
			else {
				List<Project> projects = ProjectDAO.getInstance().findByProduction();
				for (Project project : projects) {
					projectList.add(new SelectItem(project.getId(), project.getTitle()));
				}
			}
		}
		return projectList;
	}
	/** See {@link #projectList}. */
	public void setProjectList(List<SelectItem> projectList) {
		this.projectList = projectList;
	}

	/** See {@link #startFormList}. */
	public List<SelectItem> getStartFormList() {
		if (startFormList == null) {
			startFormList = new ArrayList<>();
			if (contactViewBean.getContact() != null) {
				List<Employment> empList = contactViewBean.getContact().getEmployments();
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
				for (Employment emp : empList) {
					StartForm startForm = StartFormService.findCurrentStart(emp);
					if (startForm != null) {
						String label = StartFormUtils.createStartFormLabel(startForm, sdf);
						startFormList.add(new SelectItem(startForm.getId(), label));
					}
				}
				// Create list of StartForms for the selected contact.
				/*List<StartForm> list = contactViewBean.getContact().getStartForms();
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
				for (StartForm startForm : list) {
					// Eliminate "superceded" Starts from selection (#283 r6704)
					if (startForm.getPriorFormId() == null) {
						String label = StartFormUtils.createStartFormLabel(startForm, sdf);
						startFormList.add(new SelectItem(startForm.getId(), label));
					}
				}*/
			}
		}
		return startFormList;
	}
	/** See {@link #startFormList}. */
	public void setStartFormList(List<SelectItem> startFormList) {
		this.startFormList = startFormList;
	}

	/** See {@link #projectListDL}. */
	public List<Project> getProjectListDL() {
		if(projectListDL == null) {
			projectListDL = ProjectDAO.getInstance().findByProduction();
			if (isSpecialMember) { //Special member case.
				for (Project project : getProjectListDL()) {
					if (! getProjectCheckBoxSelectedItems().contains(project)) {
						getProjectCheckBoxSelectedItems().add(project);
					}
				}
			}
			for (Project proj : projectListDL) {
				if (getProjectCheckBoxSelectedItems().contains(proj)) {
					proj.setChecked(true);
				}
				boolean allowed = AuthorizationBean.getInstance().hasPermission(SessionUtils.getCurrentContact(), proj, Permission.EDIT_CONTACTS_CAST);
				if (allowed) {
					proj.setAllowEnable(true);
				}
				else {
					proj.setAllowEnable(false);
				}
			}
			if (getIsCreate()) {
				//To check the current project and first unit for create pop up.
				getProjectCheckBoxSelectedItems().add(getProject());
				getProject().setChecked(true);
				setDisplayUnits(true);
				Unit firstUnit = getProject().getUnits().get(0);
				firstUnit.setChecked(true);
				if (! getUnitCheckBoxSelectedItems().contains(firstUnit.getNumber())) {
					getUnitCheckBoxSelectedItems().add(firstUnit.getNumber());
				}
				List<Unit> units = new ArrayList<>();
				units.add(firstUnit);
				getProjectUnitMap().put(getProject().getId(), units);
			}
		}
		return projectListDL;
	}
	/** See {@link #projectListDL}. */
	public void setProjectListDL(List<Project> projectListDL) {
		this.projectListDL = projectListDL;
	}

	/** Creates a list, consisting of all the
	 * Units in the given Project.
	 *
	 * @return A non-empty List of Unit's for the given Project.
	 * (Every Project has at least one Unit.)
	 */
	public List<Unit> getUnitList() {
		log.debug("");
		Project project;
		if (unitList == null) {
			log.debug("");
			Integer unitCount = null;
			Production currentProduction = SessionUtils.getProduction();
			unitList = new ArrayList<>();
			//Project newProject = ProjectDAO.getInstance().findById(getProjectId());
			if (currentProduction.getType().isAicp() && getProject() != null) {
				project = getProject();
				project = ProjectDAO.getInstance().refresh(project);
				unitList = project.getUnits();
			}
			else if (currentProduction.getType().getEpisodic()) {
				if (getLastSelectedProject() != null) {
					project = getLastSelectedProject();
					project = ProjectDAO.getInstance().refresh(project);
					unitList = project.getUnits();
					getUnitCheckBoxSelectedItems().clear();
					checkSavedUnits(project, unitList);
				}
				else {
					project = currentProduction.getDefaultProject();
					project = ProjectDAO.getInstance().refresh(project);
					unitList = project.getUnits();
				}
			}
			else {
				project = currentProduction.getDefaultProject();
				project = ProjectDAO.getInstance().refresh(project);
				unitList = project.getUnits();
			}
			if (unitList != null) {
				unitCount = unitList.size(); // DH just an idea - do unitCount in one place?
			}
			if (unitCount != null && unitCount > 1) {
				singleUnit = false;
			}
		}
		//project = SessionUtils.getCurrentProject();
		return unitList;
	}
	/** See {@link #unitList}. */
	@Override
	public void setUnitList(List<Unit> unitList) {
		this.unitList = unitList;
	}

	/** See {@link #singleUnit}. */
	public boolean getSingleUnit() {
		return singleUnit;
	}
	/** See {@link #singleUnit}. */
	public void setSingleUnit(boolean singleUnit) {
		this.singleUnit = singleUnit;
	}

	/** See {@link #showDepartmentLabel}. */
	public boolean getShowDepartmentLabel() {
		return showDepartmentLabel;
	}
	/** See {@link #showDepartmentLabel}. */
	public void setShowDepartmentLabel(boolean showDepartmentLabel) {
		this.showDepartmentLabel = showDepartmentLabel;
	}

	/**See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}
	/**See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

}
