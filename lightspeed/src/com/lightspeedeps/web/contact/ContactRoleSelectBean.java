package com.lightspeedeps.web.contact;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.object.ContactRole;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * This is the backing bean for the Contact+Role selection pop-up used on the
 * Callsheet Edit page for adding members to the Crew Call area.
 * <p>
 * The additional function this class adds over RoleSelectBean is that the list
 * of omitted contacts is contained in a Map, indexed by Department id, so that
 * the omitted entries can vary by Department selected.
 */
@ManagedBean
@ViewScoped
public class ContactRoleSelectBean extends RoleSelectBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 7069788197159085954L;

	private static final Log log = LogFactory.getLog(ContactRoleSelectBean.class);

	private List<SelectItem> contactRoleDL;
	private Map<Integer, List<ContactRole>> omitContactRoleMap;
	private Department department;

	private List<SelectItem> departmentCrewDL;

	public ContactRoleSelectBean() {
	}

	public static ContactRoleSelectBean getInstance() {
		return (ContactRoleSelectBean) ServiceFinder.findBean("contactRoleSelectBean");
	}

	private void createContactRoleList(Integer deptId) {
		log.debug(":" + this + ", dept id=" + deptId);
		if (deptId == null) {
			deptId = Constants.DEFAULT_DEPARTMENT_ID;
		}
		contactRoleDL = new ArrayList<SelectItem>();
		Department dept = DepartmentDAO.getInstance().findById(deptId);
		if (dept != null) {
			Collection<ContactRole> omitContacts = null;
			if (omitContactRoleMap != null) {
				omitContacts = omitContactRoleMap.get(deptId);
			}
			ContactRole cr;
			Unit unit = UnitDAO.getInstance().findById(getUnitId());
			Department pmDept = department;
			if (department.getStandardDeptId() != null) {
				pmDept = DepartmentDAO.getInstance().findById(department.getStandardDeptId());
			}
			Collection<ProjectMember> members = ProjectMemberDAO.getInstance()
					.findByUnitAndDepartment(unit, pmDept);
			for (ProjectMember pm : members ) {
				Contact contact = pm.getEmployment().getContact();
				cr = new ContactRole(contact.getId(), pm.getEmployment().getRole().getName(), pm.getEmployment().getRole().getListPriority().shortValue());
				log.debug(cr.getRoleName());
				if (omitContacts == null || ! omitContacts.contains(cr)) {
					contactRoleDL.add(new SelectItem(cr, contact.getDisplayName() + ", " + cr.getRoleName()));
				}
			}
		}
	}

	/** The department list used for the Call sheet Add Crew Call pop-up. */
	@Override
	public List<SelectItem> getDepartmentCrewDL() {
		if (departmentCrewDL == null) {
			Production prod = SessionUtils.getProduction();
			Project project = null;
			if (prod.getType().hasPayrollByProject()) {
				project = SessionUtils.getCurrentProject();
			}
			BitMask deptMask = SessionUtils.getDeptMask(prod, false);
			departmentCrewDL = DepartmentDAO.getInstance().findByProductionCompleteSelect(prod, project, deptMask);
		}
		return departmentCrewDL;
	}

	/** See {@link #contactRoleDL}. */
	public List<SelectItem> getContactRoleDL() {
		if (contactRoleDL == null) {
			createContactRoleList(departmentId);
		}
		return contactRoleDL;
	}

	/** See {@link #departmentId}. */
	@Override
	public void setDepartmentId(Integer id) {
		if (! id.equals(departmentId)) {
			contactRoleDL = null;
			department = DepartmentDAO.getInstance().findById(id);
		}
		super.setDepartmentId(id);
	}

	@Override
	public void setUnitId(Integer id) {
		if (! id.equals(unitId)) {
			contactRoleDL = null;
		}
		super.setUnitId(id);
	}

	/** See {@link #omitContactRoleMap}. */
	public Map<Integer, List<ContactRole>> getOmitContactRoleMap() {
		return omitContactRoleMap;
	}
	/** See {@link #omitContactRoleMap}. */
	public void setOmitContactRoleMap(Map<Integer, List<ContactRole>> omitContactRoleMap) {
		this.omitContactRoleMap = omitContactRoleMap;
	}

}
