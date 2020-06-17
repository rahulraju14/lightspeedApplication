package com.lightspeedeps.web.contact;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.MemberStatus;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.user.UserViewBean;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.view.ListView;
import com.lightspeedeps.web.view.View;

/**
 * This is the backing bean for Add Contact pop-up, which is available in the
 * Cast&Crew page. See jsp/common/inviteUser.xhtml and
 * jsp/contacts/addnewcontact.xhtml.
 */
@ManagedBean
@ViewScoped
public class AddContactBean extends DeptRoleSelect {
	/** */
	private static final long serialVersionUID = 2983834960679176171L;

	private static final Log log = LogFactory.getLog(AddContactBean.class);

	public static final String PRODUCTION = "Production";


	private static final String NAME_TYPE_EMAIL = "email";
//	private static final String NAME_TYPE_NAME = "name";

	/* Fields */

	/** The Contact object referenced by the JSP fields. */
	private Contact contact;

	/** The fresh Contact object used if the inviter ends up adding a
	 * new Contact to the Production (typical). */
	private Contact newContact;

	/** The previously-existing Contact used if the inviter is re-inviting
	 * a Contact that had already Declined an invitation. */
	private Contact oldContact;

	/** The User object, which will be new if the Contact has no LS membership
	 * yet. */
	private User user;

	private String selectedNameType = NAME_TYPE_EMAIL;

	/** If true, this is a new User. */
	private boolean newAccount;

	/** True if we found an existing User with the given email address. */
	private boolean searchMatched;

	/** Controls the JSP: display the "User Name" section. */
	private boolean showNames;

	/** Controls the JSP: display the "Occupation" section. */
	private boolean showRoles;

	/** If true, allow production access to the new user; this
	 * is the backing field for the corresponding check-box. */
	private boolean accessAllowed;

	/** If true, email an invitation to the new user; this
	 * is the backing field for the corresponding check-box. */
	private boolean sendInvitation;

	/** The supplied email address is not a valid email address format,
	 * e.g., missing an "@" sign. */
	private boolean emailInvalid;

	/** The message text to be displayed in the "search" area of
	 * the dialog box. */
	private String searchMessage;

	private transient ContactDAO contactDAO;
	private transient UserDAO userDAO;
	private transient EmploymentDAO employmentDAO;

	/** True iff, No Email button for Tours is clicked. */
	private boolean noEmailClicked = false;

	/*
	 * Check user canada or US then set Text Accordingly
	 */
	private String productionTitle;

	/* Constructor */
	public AddContactBean() {
		log.debug("");
	}

	public static AddContactBean getInstance() {
		return (AddContactBean) ServiceFinder.findBean("addContactBean");
	}

	@Override
	protected void init() {
		log.debug("");
		super.init();
		setSelectedNameType(NAME_TYPE_EMAIL);
		Production prod =SessionUtils.getProduction();
		contact = new Contact();
		user = new User(prod.getType().isCanadaTalent());
		user.setGstNumber(UserViewBean.QST_GST_NOT_REGISTERED); //setting GST field Not Registered
		user.setQstNumber(UserViewBean.QST_GST_NOT_REGISTERED); //setting QST field Not Registered
		contact.setUser(user);
		contact.setProduction(prod);
		contact.setStatus(MemberStatus.PENDING);
		projectMember.getEmployment().setContact(contact);
		List<ProjectMember> projectMembers = new ArrayList<>();
		projectMembers.add(projectMember);
//		contact.setProjectMembers(projectMembers);
		contact.getEmployments().add(getEmployment());
		getEmployment().getProjectMembers().add(projectMember);
		newContact = contact;
		checkDepartment();
		searchMessage = MsgUtils.getMessage("Contact.SearchPrompt");
		searchMatched = false;
		newAccount = false;
		emailInvalid = true;
		sendInvitation = true;
		accessAllowed = true;
		if (! prod.getDefaultAccess()) {
			accessAllowed = false; // default to NOT allow access
		}
		if(user.getShowCanada()) {
			setProductionTitle(Constants.CANADA_PRODUCTION_TEXT);
		}
		else {
			setProductionTitle(Constants.STANDARD_PRODUCTION_TEXT);
		}
	}

	public void show() {
		init();
		setVisible(true);
		showNames = false;
		showRoles = false;
	}

	/**
	 * Save the new contact and related objects, if inputs are valid, set the
	 * "visible" flag to false, and return true. If input validation fails,
	 * messages will be added to the Faces queue, false is returned, and the
	 * "visible" flag remains true. This method should be called by our
	 * instantiating class, e.g. ContactViewBean.
	 *
	 * @return true if save completes normally; false otherwise.
	 */
	public boolean save() {
		log.debug("");
		boolean bRet = validateData();
		if (bRet) {
			setVisible(false);
			try {
				contact.setProject(SessionUtils.getCurrentProject());
				contact.setStatus(MemberStatus.PENDING);
				contact.setLoginAllowed(getAccessAllowed());
				if(contact.getProduction().getType().isCanadaTalent()) {
					user.setGstNumber(UserViewBean.QST_GST_NOT_REGISTERED);
					user.setQstNumber(UserViewBean.QST_GST_NOT_REGISTERED);
					user.setShowCanada(true);
					//LS-1960 and LS-2093 if the person belongs to "PRODUCTION" OR "LS DATA ADMINSTRATOR" department, should have access to the production.
					if (contact.getDepartment().getName().equals(PRODUCTION) ||	contact.getDepartment().getId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) {
						contact.setLoginAllowed(true);
					}
					else {
						contact.setLoginAllowed(false);
					}
				}
				else {
					user.setShowUS(true);
				}

				if (projectMember.getEmployment().getRole().isProductionWide()) {
					// it is a production-wide role, leave the Unit null
					projectMember.setUnit(null);
				}
				if (projectMember.getUnit() != null) {
					final Unit u = UnitDAO.getInstance().refresh(projectMember.getUnit());
					projectMember.setUnit(u);
				}
				// Only go through this code if the contact had been previously deleted. LS-2538
				if (contact == oldContact && oldContact.getStatus() == MemberStatus.DELETED) {
					boolean bret = getContactDAO().reinviteContact(contact, projectMember, sendInvitation);
					if (! bret) {
						log.debug("");
						setEmployment(null);
						projectMember = null;
					}
				}
				else {
					if (getEmployment().getRole().isProductionWide() || ! contact.getProduction().getType().isAicp()) {
						getEmployment().setProject(null);
					}
					else {
						getEmployment().setProject(SessionUtils.getCurrentProject());
					}
					getEmployment().setDefRole(true);
					List<Employment> employments = null;
					if (contact == oldContact) {
						// Pass in new employment record to be added plus any
						// existing employment records. LS-2538
						employments = new ArrayList<>();
						if(contact.getEmployments() != null) {
							for(Employment emp : contact.getEmployments()) {
								emp = getEmploymentDAO().refresh(emp);
								employments.add(emp);
							}
						}
						getEmployment().setContact(contact);
						employments.add(getEmployment());
					}
					getContactDAO().attachContactUser(contact, user, sendInvitation, employments);
				}
				//user = getUserDAO().findById(user.getId()); // refresh? LazyInit error in contactViewBean later
				user = getUserDAO().merge(user);
				contact.setUser(user);
				noEmailClicked = false;
			}
			catch (Exception e) {
				EventUtils.logError("QuickContactBean save failed, Exception: ", e);
				throw new LoggedException(e);
			}
		}
		return bRet;
	}

	public String actionSearch() {
		newAccount = false;
		doSearch();
		if (searchMatched) {
			showNames = true;
		}
		return null;
	}

	public String actionNewAccount() {
		newAccount = false;
		if (doSearch()) {
			if (! searchMatched) {
				newAccount = true;
				searchMessage = MsgUtils.getMessage("Contact.NewAccount");
				showNames = true;
				showRoles = true;
			}
		}
		return null;
	}

	/**
	 * Closes the quick-add popup (by setting the "visible" flag to false).
	 *
	 * @return superclass's cancel value
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		roleDL = null;
		noEmailClicked = false;
		ListView.addClientResizeScroll();
		return super.actionCancel();
	}

	private boolean doSearch() {
		boolean bRet = false;
		searchMatched = false;
		showRoles = false;
		showNames = false;
		emailInvalid = true;
		log.debug("NoEmailClicked = " + getNoEmailClicked());
		if (contact == oldContact) {
			// user re-did search after getting a match on an existing contact
			newContact.setEmailAddress(contact.getEmailAddress()); // copy email==search string
		}
		oldContact = null;
		Production prod = SessionUtils.getProduction();
		if (! (prod.getType().isTours() && getNoEmailClicked())) {
			if (contact.getEmailAddress() == null ||
					contact.getEmailAddress().trim().length() == 0) {
				searchMessage = MsgUtils.getMessage("Contact.EnterEmail");
				View.addFocus("addContact"); // In ICEfaces 3.2, this gets overridden
			}
			else if (! EmailValidator.isValidEmail(contact.getEmailAddress().trim())) {
				searchMessage = MsgUtils.getMessage("Contact.InvalidSearchEmail");
				View.addFocus("addContact"); // In ICEfaces 3.2, this gets overridden
			}
			else {
				bRet = true;
				emailInvalid = false;
				contact.setEmailAddress(contact.getEmailAddress().trim());
				List<Contact> contacts = getContactDAO().findByPropertyProduction(ContactDAO.EMAIL_ADDRESS, contact.getEmailAddress());
				if (contacts.size() > 0) { // existing Contact in this production
					searchMatched = true;
					oldContact = contacts.get(0);
				}
				else {
					User oldUser = getUserDAO().findSingleUser(contact.getEmailAddress());
					if (oldUser != null) { // existing User (email address) in system
						searchMatched = true;
						searchMessage = MsgUtils.getMessage("Contact.SearchMatched");
						user = oldUser;
						contacts = getContactDAO().findByPropertyProduction(ContactDAO.USER, user);
						if (contacts.size() > 0) {
							// this happens if Contact in this production has a different email
							// address than the User account.
							oldContact = contacts.get(0);
							oldContact.setEmailAddress(user.getEmailAddress());
							getContactDAO().attachDirty(oldContact);
						}
						else {
							showRoles = true;
						}
					}
					else {
						searchMessage = MsgUtils.getMessage("Contact.SearchFailed");
						View.addFocus("newAcct"); // In ICEfaces 3.2, this gets overridden
					}
				}
			}
		}
		else {
			bRet = true;
			showRoles = true;
		}
		contact = newContact;
		if (! searchMatched) {
			user = new User(prod.getType().isCanadaTalent());
			contact.setUser(user);
		}
		else if (oldContact != null) {
			contact = oldContact;
			contact.getEmployments().size(); // force initialization
			contact.getStartForms().size(); // force initialization. LS-2572
			user = contact.getUser();
			if (contact.getStatus() == MemberStatus.DECLINED) {
				searchMessage = MsgUtils.getMessage("Contact.UserDeclined");
				for (Employment emp : contact.getEmployments()) {
					for (ProjectMember pm : emp.getProjectMembers()) {
						pm.getEmployment().getPermissionMask(); // force lazy init
					}
				}
				showRoles = true;
			}
			else if (contact.getStatus() == MemberStatus.DELETED) {
				searchMessage = MsgUtils.getMessage("Contact.UserDeleted");
				showRoles = true; // allow re-invite
			}
			else if (contact.getStatus() == MemberStatus.BLOCKED) {
				searchMessage = MsgUtils.getMessage("Contact.UserBlocked");
			}
			else {
				if (prod.getType().isCanadaTalent()) {
					showRoles = true;
					searchMessage = MsgUtils.getMessage("Contact.SearchMatched");
				}
				else {
					searchMessage = MsgUtils.getMessage("Contact.UserInProduction");
				}
			}
		}
		return bRet;
	}

	/**
	 * Validate the input fields.
	 * <p>First and last names must be non-blank.
	 * <p>If a user name is given, it must not duplicate an existing user name,
	 * and a non-blank password must be supplied, which must match the confirm-password
	 * field.
	 * @return True iff the fields are valid.
	 */
	public boolean validateData() {
		boolean bRet = true;
		Production prod = SessionUtils.getProduction();
		if (newAccount) {
			if (user.getFirstName()==null || user.getFirstName().trim().length()==0) {
				//MsgUtils.addFacesMessage("Contact.BlankFirstName", FacesMessage.SEVERITY_ERROR);
				MsgUtils.addFacesMessage("Contact.BlankFirstName", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			else {
				user.setFirstName(user.getFirstName().trim());
			}

			if (user.getLastName()==null || user.getLastName().trim().length()==0) {
				MsgUtils.addFacesMessage("Contact.BlankLastName", FacesMessage.SEVERITY_ERROR);
				bRet &= false;
			}
			else {
				user.setLastName(user.getLastName().trim());
			}
			user.setDisplayName(user.getFirstNameLastName());

			bRet &= (UserUtils.validateEmail(contact, true) == 0);
		}
		contact.setDisplayName(user.getDisplayName());

		if (! actionSave()) { // handle optional new dept/role names
			return false;
		}

		Role role = getEmployment().getRole();
		if (role == null) {
			MsgUtils.addFacesMessage("Contact.MissingRole", FacesMessage.SEVERITY_ERROR);
			bRet &= false;
		}
		else {
			//LS-1935 error message while adding cast with same occupation
			if (prod.getType().isCanadaTalent()) {
				Project project = SessionUtils.getCurrentProject();
				for (Employment emp : contact.getEmployments()) {
					Project empProject = ProjectDAO.getInstance().refresh(emp.getContact().getProject());
					if (project.equals(empProject)) {
						if (emp.getContact().getRole().equals(role)) {
							MsgUtils.addFacesMessage("Contact.UserInProject", FacesMessage.SEVERITY_ERROR, role.getName());
							bRet &= false;
							break;
						}
					}
				}
			}
			contact.setDepartment(role.getDepartment());
			contact.setRole(role);
			getEmployment().setPermissionMask(role.getRoleGroup().getPermissionMask());
		}

		return bRet;
	}

	@Override
	protected void createRoleDL(Integer deptId) {
		log.debug("dept id=" + deptId);
		final Department dept = DepartmentDAO.getInstance().findById(deptId);
		if (dept != null) {
			contact.setDepartment(dept);
		}
		super.createRoleDL(deptId);
	}

	@Override
	protected void createDepartmentDL() {
		super.createDepartmentDL();
		//departmentDL = DepartmentUtils.getDepartmentDL2(SessionUtils.getCurrentContact());
		checkDepartment();
	}

	/**
	 * make sure the caller-specified department is included in the
	 * list of available departments!
	 */
	private void checkDepartment() {
		boolean hasDept = false;
		if (departmentId != null && departmentDL != null) {
			for (SelectItem si : departmentDL) {
				if (((Integer)si.getValue()).equals(departmentId)) {
					hasDept = true;
					break;
				}
			}
		}
		if (! hasDept) {
			roleDL = null; // force a refresh of role list
			if (departmentDL != null) {
				departmentId = (Integer)departmentDL.get(0).getValue();
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

	// * * * accessors & mutators * * *

	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** @return True iff the "New Account" button should be enabled.  */
	public boolean getEnableNewAccount() {
		return (! searchMatched && ! newAccount && ! emailInvalid);
	}

	/**
	 * @return The button text to use on the "invitation" button -- either
	 *         "Invite" if the Contact does not yet exist on this production, or
	 *         "Re-invite" if the Contact was invited before and declined.
	 */
	public String getInviteButton() {
		return (contact == oldContact ? "Add" : "Add");
	}

	/** See {@link #showNames}. */
	public boolean getShowNames() {
		return showNames;
	}
	/** See {@link #showNames}. */
	public void setShowNames(boolean showNames) {
		this.showNames = showNames;
	}

	/** See {@link #showRoles}. */
	public boolean getShowRoles() {
		return showRoles;
	}
	/** See {@link #showRoles}. */
	public void setShowRoles(boolean showRoles) {
		this.showRoles = showRoles;
	}

	/** See {@link #searchMatched}. */
	public boolean getSearchMatched() {
		return searchMatched;
	}
	/** See {@link #searchMatched}. */
	public void setSearchMatched(boolean searchMatched) {
		this.searchMatched = searchMatched;
	}

	/** See {@link #searchMessage}. */
	public String getSearchMessage() {
		return searchMessage;
	}
	/** See {@link #searchMessage}. */
	public void setSearchMessage(String searchMessage) {
		this.searchMessage = searchMessage;
	}

	/** See {@link #newAccount}. */
	public boolean getNewAccount() {
		return newAccount;
	}
	/** See {@link #newAccount}. */
	public void setNewAccount(boolean newAccount) {
		this.newAccount = newAccount;
	}

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	/** See {@link #accessAllowed}. */
	public boolean getAccessAllowed() {
//		if (! SessionUtils.getCurrentProject().getProdAccess()){
//			accessAllowed = false;
//		}
		return accessAllowed;
	}
	/** See {@link #accessAllowed}. */
	public void setAccessAllowed(boolean accessAllowed) {
		this.accessAllowed = accessAllowed;
	}

	/** See {@link #sendInvitation}. */
	public boolean getSendInvitation() {
		return sendInvitation;
	}
	/** See {@link #sendInvitation}. */
	public void setSendInvitation(boolean sendInvitation) {
		this.sendInvitation = sendInvitation;
	}

	/** See {@link #selectedNameType}. */
	public String getSelectedNameType() {
		return selectedNameType;
	}
	/** See {@link #selectedNameType}. */
	public void setSelectedNameType(String selectedNameType) {
		this.selectedNameType = selectedNameType;
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	private EmploymentDAO getEmploymentDAO() {
		if(employmentDAO == null) {
			employmentDAO = EmploymentDAO.getInstance();
		}
		return employmentDAO;
	}

	private UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

	/** See {@link #noEmailClicked}. */
	public boolean getNoEmailClicked() {
		return noEmailClicked;
	}
	/** See {@link #noEmailClicked}. */
	public void setNoEmailClicked(boolean noEmailClicked) {
		this.noEmailClicked = noEmailClicked;
	}

	/** See {@link #productionTitle}. */
	public String getProductionTitle() {
		return productionTitle;
	}

	/** See {@link #productionTitle}. */
	public void setProductionTitle(String productionTitle) {
		this.productionTitle = productionTitle;
	}



}
