/**
 * ReportRequirementsUtils.java
 */
package com.lightspeedeps.util.project;

import java.util.ArrayList;
import java.util.List;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ReportRequirement;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * This class holds utility methods related to the ReportRequirement entity.
 */
public class ReportRequirementsUtils {

	private ReportRequirementsUtils() {
	}

	public static ReportRequirement findRequirement(ReportType type) {
		for (ReportRequirement requirement : SessionUtils.getCurrentProject().getReportRequirements()) {
			if (requirement.getType() == type) {
				return requirement;
			}
		}
		return null;
	}

	/**
	 * Generate a list of Contacts from the settings in the given
	 * ReportRequirement.
	 *
	 * @param requirement The ReportRequirement used as input.
	 * @return A List which is either empty, or contains the Contact specified
	 *         in the ReportRequirement. (Previously, it would also include
	 *         contacts that had the role (if any) specified in the
	 *         ReportRequirement; this is no longer supported.) The list may be
	 *         empty, but will never be null.
	 */
	public static List<Contact> getContactList(ReportRequirement requirement) {
		List<Contact> contactList = new ArrayList<Contact>();
		if (requirement != null) {
			// we no longer support the ReportRequirement.role facility.
//			if (requirement.getRole() != null) {
//				ProjectMemberDAO projectMemberDAO = (ProjectMemberDAO)ServiceFinder.findBean("ProjectMemberDAO");
//				List<ProjectMember> recipientsList = projectMemberDAO.findByRoleAndUnit(requirement.getRole(),
//						requirement.getProject().getUnit());
//				Contact contact;
//				for (ProjectMember pm : recipientsList) {
//					contact = pm.getUser().getContact();
//					if (contact.getNotifyForNewTask() == true) {
//						contactList.add(contact);
//					}
//				}
//			}
			if (requirement.getContact() != null &&
					requirement.getContact().getNotifyForNewTask()) {
				contactList.add(requirement.getContact());
			}
		}
		return contactList;
	}

}
