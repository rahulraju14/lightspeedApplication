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
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ContactRole;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This is the backing bean for the department and role drop-down lists in the
 * "Create Role" popup (from the Contact page). The contact drop-down is used
 * for the ExhibitG Add Cast popup.  The DPR Add Crew popup uses this class via
 * the subclass ContactRoleSelectBean.
 */
@ManagedBean
@ViewScoped
public class RoleSelectBean extends DeptRoleSelect implements Serializable {
	/** */
	private static final long serialVersionUID = - 1733876290181397262L;

	private static final Log log = LogFactory.getLog(RoleSelectBean.class);

	/** A Map from Department id to List of Contact id's.  This is optionally supplied
	 * by our caller if they want to omit some Contact's from the list presented
	 * to the user.  E.g., for the Callsheet Add Crew popup, we want to omit those
	 * Contact's that are already listed. */
	private Map<Integer, List<Integer>> omitContactMap;

	/** List of Contact's with roles in the currently selected Department. */
	List<SelectItem> contactDL;

	/** The Contact entry selected from the drop-down list by the user. */
	private ContactRole newContact;

	/* Constructor */
	public RoleSelectBean() {
		log.debug("");
		super.init();
	}

	public static RoleSelectBean getInstance() {
		return (RoleSelectBean) ServiceFinder.findBean("roleSelectBean");
	}

	/**
	 * Display the pop-up dialog.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box or null.
	 * @param messageId The message-id of the extra text to be displayed in the
	 *            pop-up.
	 */
	public void show(PopupHolder holder, int act, String titleId, String messageId) {
		super.init();
		show(holder, act, titleId, null, null);
		if (messageId != null) {
			setMessage(MsgUtils.getMessage(messageId));
		}
	}

	private void createContactDL(Integer deptId) {
		log.debug(":" + this + ", dept id=" + deptId);
		contactDL = new ArrayList<SelectItem>();
		Department dept = DepartmentDAO.getInstance().findById(deptId);
		if (dept != null) {
			Unit unit = UnitDAO.getInstance().findById(getUnitId());
			Collection<Contact> contacts = ProjectMemberDAO.getInstance()
					.findByUnitAndDepartmentDistinctContact(unit, dept);
			Collection<Integer> omitContacts = null;
			if (omitContactMap != null) {
				omitContacts = omitContactMap.get(deptId);
			}
			for (Contact contact : contacts) {
				if (omitContacts == null || ! omitContacts.contains(contact.getId())) {
					contactDL.add(new SelectItem(contact, contact.getDisplayName()));
				}
			}
		}
	}

	/**
	 * Called by base class when the selected Department has changed.
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#departmentChanged()
	 */
	@Override
	protected void departmentChanged() {
		contactDL = null;
	}

	/**
	 * Called by base class when the selected Unit has changed.
	 * @see com.lightspeedeps.web.contact.DeptRoleSelect#unitChanged()
	 */
	@Override
	protected void unitChanged() {
		contactDL = null;
	}


	// * * * accessors & mutators * * *

	/** The department list used for the Call sheet Add Crew Call pop-up. */
	public List<SelectItem> getDepartmentCrewDL() {
		return DepartmentUtils.getDepartmentCrewDL();
	}

	/** See {@link #contactDL}. */
	public List<SelectItem> getContactDL() {
		if (contactDL == null) {
			createContactDL(getDepartmentId());
		}
		return contactDL;
	}

	/** See {@link #newContact}. */
	public ContactRole getNewContact() {
		return newContact;
	}
	/** See {@link #newContact}. */
	public void setNewContact(ContactRole newContact) {
		this.newContact = newContact;
	}

	public void setOmitContactMap(Map<Integer, List<Integer>> map) {
		omitContactMap = map;
		contactDL = null; // force refresh
	}

}
