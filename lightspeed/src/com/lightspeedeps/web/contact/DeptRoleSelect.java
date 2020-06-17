/**
 * File: DeptRoleSelect.java
 */
package com.lightspeedeps.web.contact;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.dao.RoleDAO;
import com.lightspeedeps.dao.RoleGroupDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.model.Role;
import com.lightspeedeps.model.RoleGroup;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.EmployerOfRecord;
import com.lightspeedeps.type.RoleSelectType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.view.View;

/**
 * This is the base class shared by the AddContactBean and RoleSelectBean.
 * It contains most of the functionality for supporting the Department
 * and Role drop-down lists used in the pop-ups backed by those beans,
 * including creating custom Role`s.
 */
public abstract class DeptRoleSelect extends PopupBean {
	/** */
	private static final long serialVersionUID = - 4817425678432492398L;

	private static final Log log = LogFactory.getLog(DeptRoleSelect.class);

	protected static final int ROLE_ID_NEW = -2; // don't use -1 as that returns as 'null' Role

	/** The initial project membership that will be assigned to the new contact/user. */
	protected ProjectMember projectMember;

	/** The current production. */
	Production production = SessionUtils.getNonSystemProduction();

	/** The database id of the currently selected Unit. */
	protected Integer unitId;

	/** List of Unit's in the current project. */
	protected List<SelectItem> unitDL;

	/** The database id of the currently selected Department. */
	protected Integer departmentId;

	/** List of Department's available to the current user. This is affected
	 * by their permissions and roles. */
	protected List<SelectItem> departmentDL;

	/** True iff user is allowed to create a new Department. */
	private boolean allowNewDept;

	/** True iff the input field for a new Department name should be displayed. */
	private boolean showDeptName;

	/** The user-entered new Department name. */
	private String deptName;

	/** The currently selected Role entry in the drop-down. */
	private Role role;

	/** List of Role's available in the currently selected Department. */
	protected List<SelectItem> roleDL;

	/** True iff user is allowed to create a new Role. */
	private boolean allowNewRole;

	/** True iff the input field for a new Role name should be displayed. */
	private boolean showRoleName;

	/** The user-entered new Role name. */
	private String roleName;

	/** True iff the current user can create new role*/
	private boolean createNewRole = false;

	/** The currently selected Employment's existing Role. */
	private Role employmentRole;

	private Employment employment;

	/** Mapping of role.id values to PermissionMask values. */
	private Map<Integer, Long> roleMasks;


	/** True iff the user opens the create pop up
	 * also used to render the ui components on the create/edit pop up */
	private boolean isCreate;

	public DeptRoleSelect() {
		log.debug("");
		// Note: subclasses should call init() whenever appropriate
	}

	protected void init() {
		log.debug("");
		setDepartmentId(Constants.DEFAULT_DEPARTMENT_ID);
		projectMember = new ProjectMember();
		employment = new Employment(null, null, SessionUtils.getCurrentContact());
		projectMember.setEmployment(employment);
		if (SessionUtils.getCurrentProject() != null) {
			Unit selectedUnit = SessionUtils.getCurrentProject().getMainUnit();
			unitId = selectedUnit.getId();
			projectMember.setUnit(selectedUnit);
		}
		role = null;
		showRoleName = false;
		showDeptName = false;
		roleName = null;
		deptName = null;
		if (production.getType().isTours()) {
			allowNewRole = false; // Touring productions not allowed to create roles
		}
		else if (production.getType().isAicp() && production.getPayrollPref().getIncludeTouring()) {
			allowNewRole = false; // 'hybrid' productions not allowed to create roles
		}
		else {
			allowNewRole = AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_CUSTOM_ROLE);
		}
		allowNewDept = allowNewRole;
	}

	/**
	 * Method to be called by subclasses if the user is saving the
	 * results of the dialog box.  This method takes care of creating a
	 * new Department and/or new Role if so requested by the user.
	 */
	protected boolean actionSave() {
		boolean bRet = true;
		if (showDeptName) {
			if (deptName == null || deptName.trim().length() == 0) {
				MsgUtils.addFacesMessage("Contact.BlankDeptName", FacesMessage.SEVERITY_ERROR);
				setVisible(true);
				return false;
			}
		}

		if (! isCreate && getRole() != null && checkDuplicateRole(employment, getRole()) && employment.getContact().getId() != null) {
			return false;
		}

		RoleDAO roleDAO = RoleDAO.getInstance();
		if (showRoleName) {
			if (roleName == null || roleName.trim().length() == 0) {
				MsgUtils.addFacesMessage("Contact.BlankRoleName", FacesMessage.SEVERITY_ERROR);
				setVisible(true);
				View.addFocus("addNewRole");
				return false;
			}
			roleName = roleName.trim();
			// Make sure there are no characters that could pose a security risk
			roleName = ApplicationUtils.fixSecurityRiskForStrings(roleName);
			Department department = DepartmentDAO.getInstance().findById(departmentId);
			if (roleDAO.existsRole(roleName, SessionUtils.getProduction(), department)) {
				MsgUtils.addFacesMessage("Contact.DuplicateRoleName", FacesMessage.SEVERITY_ERROR);
				setVisible(true);
				return false;
			}
			Role newRole = new Role();
			newRole.setProduction(SessionUtils.getProduction());
			newRole.setName(roleName);
			RoleGroup roleGroup = RoleGroupDAO.getInstance().findById(Constants.ROLE_GROUP_ID_CUSTOM_ROLES);
			newRole.setRoleGroup(roleGroup);
			newRole.setDepartment(department);
			int priority = roleDAO.findMaxPriority(SessionUtils.getProduction(), department);
			newRole.setListPriority(priority + 1);
			roleDAO.save(newRole);
			projectMember.getEmployment().setRole(newRole);
			setRole(newRole); // TODO NTD, To create new role in edit employment pop up.
			roleDL = null; // force refresh of list; r3.2.8056
		}
		else {
			if (role != null) {
				projectMember.getEmployment().setRole(roleDAO.refresh(role));
			}
			else {
				MsgUtils.addFacesMessage("Contact.MissingOccupation", FacesMessage.SEVERITY_ERROR);
				return false;
			}
		}
		Role tRole = getRole();
		if (tRole != null) {
			employment.setRole(tRole);
			// Determine if person had same role in another project, & copy permissionMask from there.
			Long mask = null;
			if (roleMasks != null) { // map provided by caller (ContactViewBean)
				mask = roleMasks.get(tRole.getId()); // see if there's a matching role w/mask.
			}
			if (mask == null) {
				if (tRole.getRoleGroup() != null) {
					mask = tRole.getRoleGroup().getPermissionMask(); // default mask
				}
				else { // This shouldn't happen!
					// See rev 3.1.7450 comments re tRole.getRoleGroup()==null errors
					mask = 577022603114315777L; // hard-code default mask - RoleGroup 12 (General Crew)
					String msg = "Null Role Group for role: " + tRole.getId() + ", " + tRole.getName();
					log.debug(msg);
					EventUtils.logError(msg);
				}
			}
			employment.setPermissionMask(mask);
			employment.setOccupation(tRole.getName());
		}
		employment.setLastUpdated(new Date());
		log.debug("Set last updated for existing employment : " + employment.getId() + ", " + employment.getLastUpdated());
		return bRet;
	}

	/**
	 * Create a SelectItem list for the Role selection for the current project.
	 *
	 * @param deptId The department whose Role's should be included.
	 */
	protected void createRoleDL(Integer deptId) {
		createRoleDL(deptId, SessionUtils.getCurrentProject());
	}

	/**
	 * Create a SelectItem list for the Role selection.
	 *
	 * @param deptId The department whose Role's should be included.
	 * @param proj The project this list relates to; this is only used for
	 *            "hybrid" productions.
	 */
	protected void createRoleDL(Integer deptId, Project proj) {
		if (Constants.RESTRICTED_ROLES_DEPARTMENT_IDS.contains(deptId)) {
			// Department is one of those where we use 'selectors' to get a particular set of Roles
			RoleSelectType selectType = RoleSelectType.EOR_D;
			if (production.getType().isTours()) {
				selectType = RoleSelectType.EOR_D;
				if (production.getPayrollPref().getTeamEor() == EmployerOfRecord.TEAM_TOURS) {
					selectType = RoleSelectType.EOR_S;
				}
			}
			else if (production.getType().isAicp()) {
				PayrollPreference pref = PayrollPreferenceDAO.getInstance().refresh(proj.getPayrollPref());
				boolean comp = pref.getWorkersComp();
				selectType = (comp ? RoleSelectType.WC_Y : RoleSelectType.WC_N);
			}
			roleDL = RoleDAO.getInstance().createRoleSelectList(deptId, true, selectType, "listPriority");
		}
		else {
			roleDL = RoleDAO.getInstance().createRoleSelectList(deptId, true);
		}
		if (allowNewRole && (roleDL != null && roleDL.size() > 0)) {
			Role r = new Role(ROLE_ID_NEW);
			roleDL.add(1, new SelectItem(r, Constants.SELECT_HEAD_NEW_ROLE));
		}
		log.debug("dept id=" + deptId + ", n=" + roleDL.size());
	}

	/**
	 * Returns the appropriate List of SelectItems related to Departments, based
	 * on the current Contact's permissions. The list WILL be filtered based on
	 * the current Production's (or Project's) department mask. Admin
	 * departments are included.
	 *
	 * @return A non-null, but possibly empty, List.
	 */
	protected void createDepartmentDL() {
		departmentDL = DepartmentUtils.getDepartmentDL(SessionUtils.getCurrentContact(), false, true);
		departmentDL.add(0, new SelectItem(new Integer(-1), Constants.SELECT_HEAD_DEPARTMENT));
	}

	protected List<SelectItem> createUnitDL() {
		return ScheduleUtils.createUnitList(SessionUtils.getCurrentProject());
	}

	/**
	 * ValueChangeListener method for the Department list drop-down. When the
	 * user picks a different department, we re-build the list of roles
	 * available in the role drop-down.
	 *
	 * @param evt
	 *            The ValueChangeEvent created by the framework. The 'newValue'
	 *            will be the 'value' field from the SelectItem entry picked by
	 *            the user.
	 */
	public void changeDepartment(ValueChangeEvent evt) {
//		if (!evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
//			// simpler to schedule event for later - after "setRole()" is called from framework
//			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
//			evt.queue();
//			return;
//		}
		Integer id = (Integer)evt.getNewValue();
		log.debug(" dept = " + id);
		if (id != null) {
			setDepartmentId(id);
			//ContactViewBean contactViewBean = new ContactViewBean();
			if (createNewRole) {
				log.debug("createNewRole= " + createNewRole);
				createRoleDL(id);
				Role empRole = getEmploymentRole();
				if(empRole != null) {
					log.debug("Existing Role name= " + empRole.getName());
					boolean showCreateNewRoleField = true;
					for(SelectItem deptRole : roleDL) {
						if(deptRole.getValue() != null) {
							Role dRole = (Role)deptRole.getValue();
							if(dRole.getId() > 0) {
								if(dRole.equals(empRole)) {
									showCreateNewRoleField = false;
								}
							}
						}
					}
					if(showCreateNewRoleField) {
						if (allowNewRole && roleDL.size() > 0) {
							Role r = new Role(ROLE_ID_NEW);
							role = r;
							setRole(r);
							setRoleName(empRole.getName());
							showRoleName = true;
						}
					}
					else {
						role = null;
						showRoleName = false;
						projectMember.getEmployment().setRole(null);
					}
				}
			}
		}
	}

	public void changeUnit(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		log.debug("unit=" + id);
		if (id != null) {
			setUnitId(id);
		}
	}

	public void changeRole(ValueChangeEvent evt) {
		Role newRole = (Role)evt.getNewValue();
		showRoleName = false;
		if (newRole != null) {
			if (newRole.getId() >= 0) {
				projectMember.getEmployment().setRole(newRole);
				role = newRole;
			}
			else if (newRole.getId() == ROLE_ID_NEW) {
				showRoleName = true;
			}
		}
		View.addFocus("role");		// for "add role" pop-up
		View.addFocus("addRole");	// for "add/invite contact" pop-up
	}

	/**
	 * Called when the selected Department has changed.
	 * May be overridden by subclasses to do whatever.
	 */
	protected void departmentChanged() {
		// nothing to do in base class
	}

	/**
	 * Called when the selected Unit has changed.
	 * May be overridden by subclasses to do whatever.
	 */
	protected void unitChanged() {
		// nothing to do in base class
	}

	/** Method used to check whether the new occupation is duplicate for the selected contact or not.
	 * @return true if new occupation is duplicate else false.
	 */
	protected boolean checkDuplicateRole(Employment empl, Role pRole) {
		ContactViewBean contactViewBean = ContactViewBean.getInstance();
		// Skip this whole method in the "Add" case.
		if (contactViewBean.getContact() != null && empl.getContact().equals(contactViewBean.getContact())) {
			// Check for two identical roles in the same unit...
			String key;
			Project project;
			String key2 = "";

			if (pRole == null) {
				log.debug("");
				MsgUtils.addFacesMessage("Contact.MissingRole2", FacesMessage.SEVERITY_ERROR);
				return true;
			}

			project = empl.getProject();
			if (empl.getProject() != null) { // Commercial production and not Admin role
				key2 = project.getId().toString();
			}
			else { // Admin (cross-project) or TV/Feature occupation
				key2 = "";
			}
			if (pRole != null) {
				key2 += "-" + pRole.getId();
				log.debug("New key : " + key2);
			}

			for (Employment emp : contactViewBean.getContactEmploymentList()) {
				project = null;
				project = emp.getProject();
				if (emp.getProject() != null) { // Commercial production and not Admin role
					key = project.getId().toString();
				}
				else { // Admin (cross-project) or TV/Feature occupation
					key = "";
				}
				key += "-" + emp.getRole().getId();
				log.debug("Old key : " + key);
				if (key.equals(key2) && ! emp.equals(empl)) {
					MsgUtils.addFacesMessage("Contact.DuplicateRoles", FacesMessage.SEVERITY_ERROR);
					return true;
				}
			}
		}
		return false;
	}

	// * * * Getters & Setters * * *

	/** See {@link #projectMember}. */
	public ProjectMember getProjectMember() {
		return projectMember;
	}
	/** See {@link #projectMember}. */
	public void setProjectMember(ProjectMember projectMember) {
		this.projectMember = projectMember;
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		if (departmentId == null) {
			departmentId = Constants.DEFAULT_DEPARTMENT_ID;
		}
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer id) {
		if ((id == null && departmentId != null) ||
				(id != null && ! id.equals(departmentId))) {
			roleDL = null;
			departmentId = id;
			departmentChanged();
		}
		departmentId = id;
	}

	/** Sets the departmentId from the given department.
	 *  See {@link #departmentId}. */
	public void setDepartment(Department dept) {
		setDepartmentId( dept==null ? null : dept.getId() );
	}

	/** Determine if the currently selected department is an Admin department.
	 * Returns true iff the current department is LS Admin or LS Data Admin. */
	public boolean getAdminDept() {
		if (departmentId != null &&
				(departmentId.intValue() == Constants.DEPARTMENT_ID_LS_ADMIN ||
				departmentId.intValue() == Constants.DEPARTMENT_ID_DATA_ADMIN) ) {
			return true;
			}
		return false;
	}

	/** See {@link #departmentDL}. */
	public List<SelectItem> getDepartmentDL() {
		if (departmentDL == null) {
			createDepartmentDL();
		}
		return departmentDL;
	}
	/** See {@link #departmentDL}. */
	public void setDepartmentDL(List<SelectItem> deptDL) {
		departmentDL = deptDL;
	}

	/** See {@link #allowNewDept}. */
	public boolean getAllowNewDept() {
		return allowNewDept;
	}
	/** See {@link #allowNewDept}. */
	public void setAllowNewDept(boolean allowNewDept) {
		this.allowNewDept = allowNewDept;
	}

	/** See {@link #showDeptName}. */
	public boolean getShowDeptName() {
		return showDeptName;
	}
	/** See {@link #showDeptName}. */
	public void setShowDeptName(boolean showDeptName) {
		this.showDeptName = showDeptName;
	}

	/** See {@link #deptName}. */
	public String getDeptName() {
		return deptName;
	}
	/** See {@link #deptName}. */
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	/** See {@link #roleDL}. */
	public List<SelectItem> getRoleDL() {
		if (roleDL == null) {
			createRoleDL(getDepartmentId());
		}
		return roleDL;
	}
	/** See {@link #roleDL}. */
	public void setRoleDL(List<SelectItem> roleDL) {
		this.roleDL = roleDL;
	}

	/** See {@link #role}. */
	public Role getRole() {
		return role;
	}
	/** See {@link #role}. */
	public void setRole(Role role) {
		this.role = role;
	}

	/** See {@link #allowNewRole}. */
	public boolean getAllowNewRole() {
		return allowNewRole;
	}
	/** See {@link #allowNewRole}. */
	public void setAllowNewRole(boolean allowNewRole) {
		this.allowNewRole = allowNewRole;
	}

	/** See {@link #showRoleName}. */
	public boolean getShowRoleName() {
		return showRoleName;
	}
	/** See {@link #showRoleName}. */
	public void setShowRoleName(boolean showRoleName) {
		this.showRoleName = showRoleName;
	}

	/** See {@link #roleName}. */
	public String getRoleName() {
		return roleName;
	}
	/** See {@link #roleName}. */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/** See {@link #roleMasks}. */
	public Map<Integer, Long> getRoleMasks() {
		return roleMasks;
	}

	/** See {@link #roleMasks}. */
	public void setRoleMasks(Map<Integer, Long> roleMasks) {
		this.roleMasks = roleMasks;
	}

	/** See {@link #unitId}. */
	public Integer getUnitId() {
		return unitId;
	}
	/** See {@link #unitId}. */
	public void setUnitId(Integer id) {
		if (! id.equals(unitId)) {
			//roleDL = null; // Role list not dependent on Unit currently.
			final Unit unit = UnitDAO.getInstance().findById(id);
			projectMember.setUnit(unit);
			unitChanged();
		}
		unitId = id;
	}

	/** See {@link #unitDL}. */
	public List<SelectItem> getUnitDL() {
		if (unitDL == null) {
			unitDL = createUnitDL();
		}
		return unitDL;
	}
	/** See {@link #unitDL}. */
	public void setUnitDL(List<SelectItem> unitDL) {
		this.unitDL = unitDL;
	}

	/** See {@link #createNewRole}. */
	public boolean getCreateNewRole() {
		return createNewRole;
	}
	/** See {@link #createNewRole}. */
	public void setCreateNewRole(boolean createNewRole) {
		this.createNewRole = createNewRole;
	}

	/** See {@link #employmentRole}. */
	public Role getEmploymentRole() {
		return employmentRole;
	}
	/** See {@link #employmentRole}. */
	public void setEmploymentRole(Role employmentRole) {
		this.employmentRole = employmentRole;
	}

	/** See {@link #employment}. */
	public Employment getEmployment() {
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/**See {@link #isCreate}. */
	public boolean getIsCreate() {
		return isCreate;
	}
	/**See {@link #isCreate}. */
	public void setCreate(boolean isCreate) {
		this.isCreate = isCreate;
	}

}
