package com.lightspeedeps.web.contact;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.*;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.model.table.RowState;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.util.project.*;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.image.ImageHolder;
import com.lightspeedeps.web.login.*;
import com.lightspeedeps.web.onboard.ApprovalPathsBean;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.view.ListImageView;

/**
 * Backing bean for the "view" (contact details) aspect of contactlist.jsp page. Note that
 * originally there were two separate pages for List and View. When the pages were combined into
 * one, we just made the ContactViewBean a subclass of the ContactListBean, rather than merging the
 * two java classes entirely -- just in case we separate the pages in the future!
 *
 * The "ImageHolder" interface supports the New and Delete functions on the Image tab.
 */
@ManagedBean
@ViewScoped
public class ContactViewBean extends ListImageView implements ImageHolder,  Serializable {
	/** */
	private static final long serialVersionUID = 429161809373737359L;

	private static final Log log = LogFactory.getLog(ContactViewBean.class);

//	public static final String ATTR_CONTACTVIEW_CONTACT = Constants.ATTR_PREFIX + "contactViewContact";

	private static final int TAB_DETAIL = 0;
	//private static final int TAB_IMAGES = 1;
//	private static final int TAB_ROLES = 2; // no longer exists
	//private static final int TAB_PREFS = 3;
	private static final int TAB_STARTS = 4;

	private static final int NUMBER_OF_ROWS = 10;

	private static final int ACT_DELETE_ROLE = 10;
//	private static final int ACT_ADD_ROLE = 11;
	private static final int ACT_CHANGE_EMP = 12;
	private static final int ACT_CREATE_OCCUPATION = 13;

	public static final String PRODUCTION = "Production";

	/* Variables */
	private List<Contact> allContacts;
	private List<Contact> projectContacts;

	/** If true, the left-hand list only shows Contacts with a role in the
	 * current project. */
	private boolean showProject;
	/** Allow this user to view contacts with Cast roles */
	private boolean showCast;
	/** Allow this user to view contacts with Crew roles */
	private boolean showCrew;
	/** Allow this user to view contacts with Admin roles */
	private boolean showAdmin;

	/** True iff the currently selected Contact matches the current (logged in) User. */
	private boolean matchesUser;

	private int firstRow = 0;

	/* Fields */

	/** The contact currently selected. */
	private Contact contact;

	/** The Contact's email address before editing. Used for informational messages. */
	private String saveEmailAddress;

	/** The union of all department masks for the Contact's roles at the beginning of
	 * an edit cycle.  Used to determine if any Departments should be added to the
	 * Production's mask when the Contact is saved. */
	private BitMask saveDeptMask;

	/** The unit in which the current Contact has its "default" role. */
	private Unit unit;

	private Integer departmentId;

	/** Mapping of department ids to True; if an entry exists, the current
	 * user has edit privileges for roles in that department. */
//	private Map<Integer,Boolean> validDept;

	/** Mapping of Unit ids to True; if an entry exists, the current
	 * user has edit privileges for roles in that Unit. This is used to
	 * control which Position (role) line-items this user may edit. */
	private Map<Integer,Boolean> editableUnits;

	/** A List of SelectItem's of Project names (and ids) used in the
	 * "Add Role" pop-up dialog.  It only includes Project's for which
	 * the current user has EDIT_CONTACT_xxx privileges. */
	private List<SelectItem> editableProjectDL;

	/** A Map from Project id to a List of SelectItem`s of Unit names
	 * and ids.  The Unit`s are those for which the current User has
	 * role-editing privileges within the given Project.  The SelectItem
	 * List is used within the Create Role pop-up. */
	private Map<Integer,List<SelectItem>> unitDLmap;

	/** The id of the Contact's Assistant. Held separately to simplify management
	 * of the "select assistant" drop-down list. */
	private Integer assistantId = -1;

	/** List of SelectItem`s for the "Assistant" drop-down list, from which a
	 * user may select an assistant to be assigned to the contact being edited.
	 * This list does NOT include cast-only personnel or LS Admins.  The values
	 * are Contact.id and the labels are 'last name, first name'.  The first
	 * entry will be "(none selected)" or similar. */
	private List<SelectItem> contactDL;

	/** List of SelectItem's which populates the drop-down lists on the Positions
	 * mini-tab.   The values are department.id and labels are department.name. */
	private List<SelectItem> departmentDL = null;

	/** A Map from Role.id values to permission masks, for all the Roles
	 * the Contact has at the beginning of an Edit cycle.  Used to propagate
	 * customized permissions when the same role is added in a different
	 * project. */
	private Map<Integer, Long> roleMasks;

	/** A transient boolean used to manage the "send advanced script" setting
	 * for the current Contact. */
	private boolean sendAdvanceScript;

	private boolean showAddContact;

	/** Boolean to show the error messages on pop up for occupation edit/create. */
	private boolean showAddOccupation;

	/** The title text for the "Add new person/asst" dialog box */
	private String addPopupTitle;

	/** This set holds all the database ids of the ProjectMember objects that have
	 * been deleted (if any) during the current edit session on the current user. */
//	private final Set<Integer> deletedPms = new HashSet<Integer>();

	private Integer newRoleId;

	/** The database id of the ProjectMember object representing the
	 * role that the user has requested be deleted. */
//	private Integer removePmId;

	/** A computed/transient setting. The returned value is
	 * 		user.loginAllowed && (!user.lockedOut)
	 *  Setting it to true (checking the box) both turns on user.loginAllowed
	 *  and turns off user.lockedOut. */
	private boolean loginEnabled;

	private transient ApprovalPathsBean approvalPathsBean;

	private static final SelectItem[] statusList = {
			new SelectItem("1","Assigned"),
			new SelectItem("0","Candidate") };

	/** SelectItem's for the radio buttons for "primary phone" selection.  Note that
	 * the 'value' fields must be Integer, not String, to match the class of Contact.primaryPhoneIndex*/
	private static final SelectItem[] phoneItems = {
		new SelectItem(new Integer(0)," "),
		new SelectItem(new Integer(1)," "),
		new SelectItem(new Integer(2)," ") };

	private static final List<SelectItem> statusDL = new ArrayList<>( Arrays.asList(statusList));

	/** If true, then a new Contact was created (typically for Assistant) using
	 * the "quick add" pop-up.  For existing contacts, the new assistant is saved
	 * immediately upon clicking Save in the Quick Add dialog.  But for a NEW contact,
	 * the new assistant is not saved until the entire contact is saved. */
	private static final boolean saveQuickAdd = false; // "Assistant" code is no longer active
	private AddContactBean quickContact;

	private transient ContactDAO contactDAO;
	private transient ProjectMemberDAO projectMemberDAO;
	private transient StartFormDAO startFormDAO;

	/** List of employments of the selected contact on cast and crew tab.*/
	private List<Employment> contactEmploymentList = null;

	private List<SelectItem> unitDL;

	private Employment currentEmployment;

	/** Variable holds the Employment which the user has chosen
	 * to delete using the red crosses on the employment data table on Cast n Crew / Contacts page */
	private Employment employmentToDelete;

	/** Set holds the objects of employment, project member and contact,
	 *  that will be saved on action save of contact view bean.*/
	private Set<Object> setToCreate = new HashSet<>();

	/** Set holds the objects that will be deleted on action save of contact view bean.*/
	private Set<Object> setToDelete= new HashSet<>();

	/** Set holds the objects that will be attached on action save of contact view bean.*/
	private Set<Object> setToAttach= new HashSet<>();

	/** True if current user/viewer is a start approver.*/
	//private boolean isStartApprover = false;

	private Production production;

	/** Radio button values for Primary Phone.  */
	private Boolean primaryPhone[] = new Boolean[3];

	/*
	 * Check user and set Project and Production to text accordingly LS-1962
	 */
	private String projectTitle;
	private String projectsTitle;
	/** Production label based on which production type we are in, */
	private String productionLbl;

	public static ContactViewBean getInstance() {
		return (ContactViewBean)ServiceFinder.findBean("contactViewBean");
	}

	/** Default Constructor */
	public ContactViewBean() {
		super(Contact.SORTKEY_NAME, "Contact.");
		log.debug("");
		try {
			Contact currContact = SessionUtils.getCurrentContact();
			production = SessionUtils.getProduction();
			//isStartApprover = checkStartApprover();
			AuthorizationBean authBean = AuthorizationBean.getInstance();
			showCast = authBean.hasPermission(currContact, currContact.getProject(), Permission.VIEW_CONTACTS_CAST);
			showCrew = authBean.hasPermission(currContact, currContact.getProject(), Permission.VIEW_CONTACTS_CREW);
			showAdmin = authBean.isAdmin();
			if (production.getType().getEpisodic()) {
				showProject = true;
			}
			if (production.getType().isCanadaTalent()) {
				setProjectsTitle(Constants.PROJECT_S_TEXT_CANADA);
				setProjectTitle(Constants.PROJECT_TEXT_CANADA);
				setProductionLbl(Constants.CANADA_PRODUCTION_TEXT);
			}
			else {
				setProjectsTitle(Constants.PROJECT_S_TEXT_US);
				setProjectTitle(Constants.PROJECT_TEXT_US);
				setProductionLbl(Constants.STANDARD_PRODUCTION_TEXT);
			}
			//SessionUtils.put(Constants.ATTR_START_FORM_BACK_PAGE, HeaderViewBean.PEOPLE_MENU_CONTACTS);
			log.debug(this);
			initView();
			scrollToRow(); // on page load, make sure item is visible in list
			checkTab(); // restore last selected mini-tab
			restoreSortOrder();
			Arrays.fill(primaryPhone, Boolean.FALSE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** Method used to check whether the current approver is approver for the start form or not.
	 * @return True if current contact is approver, else false.
	 */
	/*private boolean checkStartApprover(Employment empl) {
		isStartApprover = false;
		if (AuthorizationBean.getInstance().hasPermission(Permission.VIEW_EMPLOYEE_START_DOCS)) {
			isStartApprover = true;
		}
		else if (production.getAllowOnboarding() && ! matchesUser) {
			//Contact curContact = SessionUtils.getCurrentContact();
			Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
			DocumentChain chain = DocumentChainDAO.getInstance().findOneByNamedQuery(
					DocumentChain.GET_START_DOCUMENT_CHAIN_OF_PRODUCTION, map("folderId", onboardFolder.getId()));
			//Set<ApprovalPath> pathSet = chain.getApprovalPath();

			ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(null, chain, SessionUtils.getCurrentOrViewedProject());
			isStartApprover = ApproverUtils.isContactInPath(approvalPath, contact, null);

			for (ApprovalPath path : pathSet) {
				isStartApprover =  ApproverUtils.isContactInPath(path, curContact, null);
				if (isStartApprover) {
					break;
				}
			}
		}
		else if (matchesUser) {
			isStartApprover = true;
		}
		return isStartApprover;
	}*/

	/**
	 * Initialize our fields based on the contact id we find in the Session.
	 */
	private void initView() {
		log.debug("");
		try {
			Contact aContact;
			Integer id = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
			if (id != null) {
				log.debug("");
				aContact = getContactDAO().findById(id);
			}
			else {
				log.debug("");
				aContact = SessionUtils.getCurrentContact();
				id = aContact.getId();
				SessionUtils.put(Constants.ATTR_CONTACT_ID, aContact.getId());
			}
			if (aContact != null) {
				log.debug("");
				setSelected(aContact, true);
				setupSelectedItem(id);
				RowState state = new RowState();
				state.setSelected(true);
				getStateMap().put(aContact, state);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Determine which Contact we are supposed to view. If the id given is null or invalid, we try
	 * to display the current user's Contact info. If that fails also, we just take the "first"
	 * Contact in the database.
	 *
	 * @param id
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		unit = SessionUtils.getCurrentProject().getMainUnit();
		if (id != null && id.equals(NEW_ID)) {
			contact = quickContact.getContact();
			if (contact.getDepartment() != null) {
				departmentId = contact.getDepartment().getId();
			}
			else {
				departmentId = Constants.DEFAULT_DEPARTMENT_ID;
			}
			contact.setHomeAddress(new Address());
			updateSessionData(null);
			setSelectedTab(0);
		}
		else {
			int ix = getSelectedRow();
			if (ix >= 0 && ix < getContactList().size()) {
				Contact oldContact = contact;
				contact = getContactList().get(ix);
				if (contact.getId().equals(id)) {
					log.debug("editMode : " + editMode);
					//TODO DH: Sometimes this "if" condition evaluates to true can't figure out how...since the problem occurs rarely
					//Therefore, before putting the contact id in session variable Constants.ATTR_CONTACT_ID it returns.
					//And then in StartformBean it searches the startform for the contact id stored in the session variable.
					if (editMode && oldContact != null && oldContact.equals(contact)) {
						contact = oldContact;
						return;
					}
					Contact saveContact = contact;
					contact = getContactDAO().refresh(contact);
					contact.setIsCast(saveContact.getIsCast());
					contact.setIsNonProd(saveContact.getIsNonProd());
				}
				else {
					contact = null; // wrong one!
					log.debug("list lookup failed");
				}
			}
			if (contact == null && id != null) {
				contact = getContactDAO().findById(id);
			}
			if (contact == null) { // may happen if user goes to this page via URL entry
				contact = getvContact(); // current user
				if (contact == null) { // can this happen?
					if ((getContactList() != null) && (getContactList().size() > 0)) {
						contact = getContactList().get(0);
					}
				}
				else { // need to refresh it; rev 4071.
					contact = getContactDAO().refresh(contact);
				}
			}
			if (contact != null) {
				Integer oldId = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
				if (oldId == null || oldId != contact.getId()) {
					updateSessionData(contact.getId());
				}
				setup();
				contact.setImageResources(null);
			}
			else {
				setSelectedTab(TAB_DETAIL);
				actionNew(); // no other choice
			}
		}
		setSendAdvanceScript(
				(contact.getSendAdvanceScript() != null && contact.getSendAdvanceScript() > 0) ? true : false);
		resetImages();
	}

	/**
	 * We probably have a Contact object; initialize any fields that are
	 * necessary for display.
	 */
	private void setup() {
		boolean found = select(contact);	// tell list to select this item
		if ( ! found && getContactList().size() > 0) {
			contact = getContactList().get(0);
			contact = getContactDAO().refresh(contact);
			updateContactInList(contact);
			select(contact);
			updateSessionData(contact.getId());
		}
		//roleMap = null; // refresh when next needed
		editableUnits = null; // refresh when next needed
		contactEmploymentList = null; // refresh the table view on new row selection
		//getStartFormBean().setup();

		// Set default department for role-selection popup
//		if (contact != null && contact.getDepartment() != null) {
//			departmentId = contact.getDepartment().getId();
//		}
//		else {
			departmentId = null;
//		}
		if (contact.getRole() != null) {
			departmentId = contact.getRole().getDepartment().getId();
		}
		log.debug("dept=" + departmentId);
		if (contact != null && contact.getHomeAddress() == null) {
			contact.setHomeAddress(new Address());
		}
		saveEmailAddress = contact.getEmailAddress();
		matchesUser = false;
		if (contact.getUser() != null) {
			matchesUser = contact.getUser().getId().equals(SessionUtils.getCurrentUser().getId());

			if (!newEntry) {
				forceLazyInit();
			}
		}
		setupTabs();
	}

	/**
	 * Update the Session attributes when our displayed Contact value has
	 * changed.
	 *
	 * @param contactId The Contact.id value (or null) to store in the
	 *            appropriate Session attribute.
	 *
	 */
	private void updateSessionData(Integer contactId) {
		SessionUtils.put(Constants.ATTR_CONTACT_ID, contactId);
		SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null); // so onboarding will use attr_contact_id
		SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID,null);
		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force timecard pages to use attr_contact_id
	}

	/**
	 * These fields are referenced on some tab (such as Role), and are not
	 * initialized when the Contact object is still in the session that obtained
	 * it.  We need to force Hibernate to load them while the Contact is
	 * still in the original session.  This applies to fields marked as Fetch=LAZY
	 * in the Contact object.
	 */
	private void forceLazyInit() {
		log.debug("");
		String str = "";
		try {
			// Fix for Unit LIE LS-2538
			if(contact.getEmployments() != null) {
				for(Employment emp : contact.getEmployments()) {
					if(emp.getProjectMembers() != null) {
						for(ProjectMember pm : emp.getProjectMembers()) {
							pm.getUnit();
						}
					}
				}
			}
			if (contact.getAssistant() != null) {
				Contact asst = getContactDAO().refresh(contact.getAssistant()); // fix lazyInit
				contact.setAssistant(asst);
				str = asst.getUser().getDisplayName(); // force initialization
				Address addr = asst.getHomeAddress();
				if (addr != null) {
					str = addr.getAddrLine1(); // force initialization
				}
			}
			if (contact.getHomeAddress() != null) {
				str = contact.getHomeAddress().getAddrLine1();
			}
			if (contact.getRole() != null) {
				Role r = contact.getRole();
				r = RoleDAO.getInstance().refresh(r); // fix LazyInitializationExceptions; rev 2642
				contact.setRole(r);
				str = r.getName();
				str = r.getDepartment().getName();
			}
			if (contact.getCastMember() != null && contact.getCastMember().getImages() != null) {
				for (Image image : contact.getCastMember().getImages()) {
					str = image.getTitle();
				}
			}
			else if (contact.getImages() != null) {
				for (Image image : contact.getImages()) {
					str = image.getTitle();
				}
			}
			if (editMode) { // "Create" pop-up will need StartForms List.
				contact.getStartForms().size();
			}
			str = contact.getProduction().getOwningAccount();
			contact.getDisplayName();
			for (Employment emp : getContactEmploymentList()) {
				emp = EmploymentDAO.getInstance().refresh(emp);
				emp.getOccupation();
				if (emp.getProject() != null) {
					Project proj = emp.getProject();
					proj = ProjectDAO.getInstance().refresh(proj);
					proj.getUnit(0);
				}
				emp.getProjectMembers().size();
				emp.getRole();
				emp.getStartForm();
				emp.getDepartment().getName();
				if(contact.getEmployments().size() == 1) {
					emp.setDefRole(true);
					emp.setDisableEmpDefRole(true);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			log.debug(str);
		}
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Contact)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The Action method of the "Edit" button on the View page.
	 * @return The faces navigation string to go to the edit page.
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		try {
			super.actionEdit();
			Department dept = contact.getDepartment();
			contact = getContactDAO().refresh(contact); // in case someone else changed it recently
			contact.setDepartment(dept); // restore transient field

			forceLazyInit();
			if (contact.getHomeAddress() == null) {
				contact.setHomeAddress(new Address());
				log.debug("new address for Contact: " + contact.getHomeAddress());
			}
			if (contact.getUser() == null) { // should not happen! But clean up...
				createUser(contact);
			}
			createRoleMasks();
//			deletedPms.clear();
			saveEmailAddress = contact.getEmailAddress();
			saveDeptMask = DepartmentDAO.getInstance().findMaskForRoles(contact.getEmployments());

		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 *  Action method for the Save button.
	 */
	public String actionSaveContact() {
		try {
			log.debug("");
			if (! checkFields() ) { // update/clean/test various fields before saving
				return null;
			}
			else if (! checkDefRoles()) { // check validity of def roles before saving
				return null;
			}
			boolean updated = false;
			checkSaveAddress();
			User user = contact.getUser();
			user.setDisplayName(user.getDisplayName()); // Will set it if previously blank. LS-4744
			if (! getSetToAttach().isEmpty()) {
				for (Object curObject : getSetToAttach()) {
					getContactDAO().attachDirty(curObject);
				}
				updated = true;
			}
			if (! getSetToDelete().isEmpty()) {
				for (Object curObject : getSetToDelete()) {
					if (curObject instanceof ProjectMember) { // first delete PM(s)
						//getCurrentEmployment().getProjectMembers().remove(curObject);
						for (Employment emp : contact.getEmployments()) {
							if (emp.equals(((ProjectMember)curObject).getEmployment())) {
								emp.getProjectMembers().remove(curObject);
								break;
							}
						}
						getContactDAO().delete(curObject);
					}
				}
				for (Object curObject : getSetToDelete()) {
					if (curObject instanceof Employment) { // then delete Employment(s)
						getContactDAO().delete(curObject);
					}
					else if (curObject instanceof StartForm) {
						// StartForm is deleted via cascade; remove from Contact collection
						contact.getStartForms().remove(curObject);
					}
				}
				getSetToDelete().clear();
				updated = true;
			}
			if (! getSetToCreate().isEmpty()) {
//				Production currProd = SessionUtils.getProduction();
				for (Object curObject : getSetToCreate()) {
					if (curObject instanceof Employment) {
						Employment emp = (Employment) curObject;
						// fix NonUniqueObjectException on (role.)Department
						emp.setRole(RoleDAO.getInstance().refresh(emp.getRole()));
						log.debug("Set last updated for new employment : " + emp.getId() + ", " + emp.getLastUpdated());
						emp.setLastUpdated(new Date());
						getContactDAO().save(emp);
						// 3.1.6665 - do NOT automatically create new StartForm for new Employment records.
//						if (! currProd.getAllowOnboarding()) {
//							if (! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_LS_ADMIN) &&
//								! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) {
//								boolean createStart = true;
//								for (Object object : getSetToCreate()) {
//									if (object instanceof StartForm) {
//										if (((StartForm)object).getEmployment() != null) {
//											Employment e = ((StartForm)object).getEmployment();
//											if (emp == e) {
//												createStart = false;
//												break;
//											}
//										}
//									}
//								}
//								if (createStart) {
//									StartFormService.createFirstStartForm(contact, emp, emp.getProject());
//								}
//							}
//						}
					}
				}
				for (Object curObject : getSetToCreate()) {
					if (! (curObject instanceof Employment)) {
						getContactDAO().save(curObject);
						if (curObject instanceof StartForm && ((StartForm)curObject).getEmployment() != null) {
							((StartForm)curObject).getEmployment().getStartForms().add((StartForm) curObject);
						}
					}
				}
				updated = true;
			}
			setToAttach.clear();
			setToCreate.clear();
			setToDelete.clear();
			currentEmployment = null;

			if (updated) {
				List<StartForm> list = contact.getStartForms();
				for (int i = 0; i < list.size(); i++) {
					list.set(i, getStartFormDAO().refresh(list.get(i)));
				}
				for (Employment emp : contactEmploymentList) {
					refresh(emp.getStartForms());
				}
			}
			if (hasImageChanges() && contact.getCastMember() != null) {
				getContactDAO().attachDirty(contact.getCastMember());
			}
			if (contact.getStatus() == MemberStatus.NO_ROLES && contact.hasProjectMembers()) {
				if (contact.getLoginAllowed()) {
					contact.setStatus(MemberStatus.PENDING);
				}
				else {
					contact.setStatus(MemberStatus.NO_ACCESS);
				}
			}
			actionSaveChanges();

			// Previously we were removing the def roles for individual projects if user selects primary check box for all projects.
			//Now we allow the def role for individual projects and for all projects also. As it is mentioned in "issue #308" and we will not need this code now.*/

			// To set non admin role as def role, if any non admin role exists.
			/* Previously we were removing the def roles for individual projects if user selects primary check box for all projects.
			So was that right? If yes, then this code will change the def role if contact has admin role as def role and has some non-admin roles also.
			Then in Commercial prod, what will be the def role for contact in contactlist, if current project has no employment for that contact?
			On UI it was showing any other project's def role.

			for (Employment emp : getContactEmploymentList()) {
				if (emp.getDefRole() && emp.getRole().isAdmin()) {
					boolean nonAdminRole = false;
					Map<Project, Employment> nonAdminEmplMap = new HashMap<>();
					for (Employment empl : contact.getEmployments()) {
						//isProductionWide is for data admin roles
						if (! (empl.getRole().isAdmin() || empl.getRole().isProductionWide())) {
							nonAdminRole = true;
							if (production.getType().isAicp() && empl.getProject() != null) {
								if (! nonAdminEmplMap.containsKey(empl.getProject())) {
									log.debug(" >>>>>>>>" + empl.getProject().getTitle() + " >> " + empl.getOccupation());
									nonAdminEmplMap.put(empl.getProject(), empl);
								}
							}
							else {
								nonAdminEmplMap.put(production.getDefaultProject(), empl);
								break;
							}
						}
					}
					if (nonAdminRole && nonAdminEmplMap != null) {
						for (Project proj : nonAdminEmplMap.keySet()) {
							Employment nonAdminEmpl = nonAdminEmplMap.get(proj);
							nonAdminEmpl.setDefRole(true);
							EmploymentDAO.getInstance().attachDirty(nonAdminEmpl);
							if ((! production.getType().isAicp()) || proj.equals(SessionUtils.getCurrentProject())) {
								contact.setRole(nonAdminEmpl.getRole());
								contact.setRoleName(nonAdminEmpl.getRole().getName());
							}
						}
						emp.setDefRole(false);
						EmploymentDAO.getInstance().attachDirty(emp);
					}
					break;
				}
			}*/

			if (newEntry) {
				saveNew();
			}

			else {
				if (primaryPhone[0]) {
					contact.setPrimaryPhoneIndex(0);
				}
				else if (primaryPhone[1]) {
					contact.setPrimaryPhoneIndex(1);
				}
				else if (primaryPhone[2]) {
					contact.setPrimaryPhoneIndex(2);
				}
				if (contact.getCastMember() == null && addedImages != null) {
					contact = getContactDAO().update(contact, addedImages);
				}
				else {
					getContactDAO().attachDirty(getContact());
				}
				updateContactInList(contact);
			}
			roleMasks = null;

			// If any roles were added in departments that are not already "active",
			updateDeptMask(saveDeptMask); // make them active now.

			select(contact); // we were losing selection highlighting without this
			super.actionSave();

			// If contact lost role in time-keeping dept, remove as time-keeper. rev 3767
			DepartmentDAO.getInstance().updateTimeKeeper(contact);

			if (saveEmailAddress != null && ! saveEmailAddress.equals(contact.getEmailAddress())) {
				// rev 2632: show info message pop-up when user changes production email address
				PopupBean conf = PopupBean.getInstance();
				conf.show("Contact.EmailChanged.");
				String msg = MsgUtils.formatMessage("Contact.EmailChanged.Text",
						contact.getEmailAddress());
				conf.setMessage(msg);
			}
			contactEmploymentList = null; // force refresh (and sort)
			// Force refresh of the image resource list.
			contact.setImageResources(null);
			return null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SaveFailed", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Refresh all the StartForms in the given Set.
	 * @param startForms
	 */
	private void refresh(Set<StartForm> startForms) {
		List<StartForm> list = new ArrayList<>();
		try {
			startForms.size();
			for (StartForm sf : startForms) {
				list.add(startFormDAO.refresh(sf));
			}
			startForms.clear();
			startForms.addAll(list);
		}
		catch (Exception e) {
			log.warn("ignoring exception");
		}
	}

	/**
	 * Check default roles, whether there exist at least one checkbox per project,
	 * and one per "All Projects" if applicable.
	 * In the case of if any expected default role is missing,
	 * this method will issue a FacesMessage and return false.
	 *
	 * @return True if passed validation; false if not valid,
	 *       in which case a message has already been issued to the user.
	 */
	private boolean checkDefRoles() {
		boolean bRet = true;
		try {
			if (getContactEmploymentList().size() == 0) {
				return true; // no employments left at all!
			}
			Map<Project, Boolean> projectDefRoleMap = new HashMap<>();
			List<Project> projectList = new ArrayList<>();
			boolean allProjects = false;
			for (Employment emp : getContactEmploymentList()) {
				if (emp.getProject() != null) {
					production = ProductionDAO.getInstance().refresh(production);
					if (production.getType().isAicp()) {
						if (! projectList.contains(emp.getProject())) {
							projectList.add(emp.getProject());
						}
						if (emp.getDefRole()) {
							projectDefRoleMap.put(emp.getProject(), true);
						}
					}
					else {
						if (emp.getDefRole()) {
							projectDefRoleMap.put(production.getDefaultProject(), true);
						}
					}
				}
				else {
					allProjects = true;
					if (emp.getDefRole()) {
						projectDefRoleMap.put(null, true);
						break;
					}
				}
			}
			String msgString = "";
			if (allProjects && (! projectDefRoleMap.containsKey(null))) {
				if (production.getType().isAicp()) {
					msgString = msgString + "All Projects / Admin roles, ";
				}
				else if (production.getType().getEpisodic()) {
					msgString = msgString + " this person in the production, ";
				}
				else {
					msgString = msgString + "this person, ";
				}
				//MsgUtils.addFacesMessage("Contact.MissingDefRole", FacesMessage.SEVERITY_ERROR, "All Projects");
				bRet = false;
			}
			if (production.getType().isAicp()) {
				for (Project project : projectList) {
					if (! projectDefRoleMap.containsKey(project)) {
						msgString = msgString + project.getTitle() + ", ";
						//MsgUtils.addFacesMessage("Contact.MissingDefRole", FacesMessage.SEVERITY_ERROR, project.getTitle());
						bRet = false;
					}
				}
			}
			else if (production.getType().getEpisodic()) {
				if (projectDefRoleMap.size() == 0 /*! projectDefRoleMap.containsKey(production.getDefaultProject())*/) {
					// TODO discuss with Jim requirements for default values in episodic productions
					// msgString = msgString + "person, ";
					MsgUtils.addFacesMessage("Contact.MissingDefRole", FacesMessage.SEVERITY_ERROR, "Contact");
					bRet = false;
				}
			}
			if (! bRet && msgString.length() > 2) {
				msgString = msgString.substring(0, (msgString.length() - 2));
				MsgUtils.addFacesMessage("Contact.MissingDefRole", FacesMessage.SEVERITY_ERROR, msgString);
			}
		}
		catch (Exception e) {
			bRet = false;
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return bRet;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		try {
			super.actionCancel();
			if (isNewEntry()) {
				newEntry = false;
				contact = null;
			}
//			deletedPms.clear();
			roleMasks = null;
			setToCreate.clear();
			setToAttach.clear();
			setToDelete.clear();
			initView();	// find some contact to display
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Delete a Contact. This is the Action method called by the Delete button.
	 * First we verify if the Contact can be safely deleted. If not, put up an
	 * error message. If so, call our superclass to put up the standard
	 * confirmation dialog.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionDelete() {
		try {
			if (ApproverUtils.isAnyApprover(contact)) {
				MsgUtils.addFacesMessage("Contact.CantDeleteApprover", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (getContactDAO().isContactRemoveable(contact)) {
				if (contact.getUser().getAccountNumber().equals(SessionUtils.getProduction().getOwningAccount())) {
					MsgUtils.addFacesMessage("Contact.CantDeleteOwner", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				if (checkStopDeleteProdAdmin()) {
					PopupBean conf = PopupBean.getInstance();
					conf.show("Contact.CantDeleteAdmin.");
					String msg = MsgUtils.formatMessage("Contact.CantDeleteAdmin.Text",
							contact.getUser().getFirstNameLastName());
					conf.setMessage(msg);
					return null;
				}
				if (checkStopDeleteFinancialAdmin()) {
					PopupBean conf = PopupBean.getInstance();
					conf.show("Contact.CantDeleteFinancialAdmin.");
					String msg = MsgUtils.formatMessage("Contact.CantDeleteFinancialAdmin.Text",
							contact.getUser().getFirstNameLastName());
					conf.setMessage(msg);
					return null;
				}
				super.actionDelete();
			}
			else {
				MsgUtils.addFacesMessage("Contact.CantDelete", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method to delete the current contact from the production.
	 * @return null navigation string
	 */
	@Override
	protected String actionDeleteOk() {
		log.debug("");
		editMode = false;
		try {
			deleteContact(contact); // this rebuilds list, too.
			setupSelectedItem(getRowId(getSelectedRow()));
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;	// stay on same page
	}

	/**
	 * Action method for the "Add" button on the Cast&Crew tab. This does some
	 * validation, then puts up the "Invite contact" dialog.  Control will
	 * return to actionAddContact (via confirmOk).
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionNew()
	 */
	@Override
	public String actionNew() {
		log.debug("");
		try {
			Production prod = SessionUtils.getProduction();
			int count = getContactDAO().countByProductionActiveNonAdmin(prod);
			if (count >= prod.getMaxUsers()) {
				String phoneExt = Constants.LS_SUPPORT_PHONE + ", " + Constants.LS_MARKETING_EXT;
				if (SessionUtils.isTTCProd() || SessionUtils.isTTCOnline()) {
					phoneExt = Constants.TTC_ONLINE_SUPPORT_PHONE;
				}

				PopupBean conf = PopupBean.getInstance();
				conf.show("Contact.MaxUsersInProduction.");
				String msg = MsgUtils.formatMessage("Contact.MaxUsersInProduction.Text",
						prod.getMaxUsers(), phoneExt);
				conf.setMessage(msg);
				return null;
			}
			if (editMode) {
				PopupBean.getInstance().show(
						null, 0,
						getMessagePrefix()+"AddSaveFirst.Title",
						getMessagePrefix()+"AddSaveFirst.Text",
						"Confirm.OK",
						null); // no cancel button
				return null;
			}
			quickContact = AddContactBean.getInstance();
			quickContact.show();
			if (contact != null) {
				quickContact.setDepartment(contact.getDepartment());
			}
			showAddContact = true;
//			showAsst = false;
			addPopupTitle = "Add New Person to Production";
//			addClientResize();
			addFocus("addContact");
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}


	/**
	 * The Action method for Cancel button in the add contact/assistant popup.
	 *
	 * @return Failure navigation string.
	 */
	public String actionCancelAdd() {
		addButtonClicked();
		showAddContact = false;
//		showAsst = false;
		return quickContact.actionCancel();
	}

	/**
	 * The action method for the Save/Invite/OK button on the Add
	 * (Contact/Assistant) pop-up dialog. Check our boolean flag to see which
	 * pop-up is displayed, and proceed accordingly.
	 *
	 * @return null navigation string.
	 */
	public String actionAddOk() {
//		if (showAsst) {
//			actionAddAsst();
//		}
//		else {
			actionAddContact();
//		}
		return null;
	}

	/**
	 * Action method for the "Save/Invite" button on the "Add new person"
	 * pop-up dialog.
	 * @return null navigation string
	 */
	private String actionAddContact() {
		log.debug("");
		try {
			if (SessionUtils.getNonSystemProduction().getType().isTours() && quickContact.getNoEmailClicked()) {
				User usr = quickContact.getContact().getUser();
				String emailAddress = EmailValidator.makeFakeEmail(usr.getFirstName(), usr.getLastName());
				log.debug("Fake Email Address = " + emailAddress);
				quickContact.getContact().setEmailAddress(emailAddress);
			}
			if (quickContact.save()) {
				showAddContact = false;
				editMode = true;
				log.debug("........Before.........." + contact.getId() + ", " + contact.getDisplayName());
				RowState state = new RowState();
				state.setSelected(false);
				getStateMap().put(contact, state);
				contact.setSelected(false);
				contact = quickContact.getContact();
				log.debug("........After.........." + contact.getId() + ", " + contact.getDisplayName());
				state.setSelected(true);
				getStateMap().put(contact, state);
				Employment emp = quickContact.getEmployment();
				if (emp != null) {
					if (! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_LS_ADMIN) &&
							! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN) &&
							((! SessionUtils.getProduction().getAllowOnboarding()) ||
									SessionUtils.getProduction().getType().isTalent())) {
						// Create a StartForm for the FIRST Employment record for NON-onboarding or Talent productions;
						// but don't create one for Admin departments (roles).
						StartForm sf = StartFormService.createFirstStartForm(contact, quickContact.getEmployment());
						emp.getStartForms().add(sf);
					}
					log.debug("employment: " + emp.getId());
					if (emp.getId() == null) {
						if (emp.getRole().isProductionWide()) {
							emp.setProject(null);
						}
						else {
							emp.setProject(SessionUtils.getCurrentProject());
						}
						EmploymentDAO.getInstance().save(emp);
					}
					if (checkExistingProject()) {
						emp.setDefRole(true);
					}
					else {
						emp.setDefRole(false);
					}
				}

				//LS-1894 Setting person having production department to Approval Path in a Canadian production
				if (contact.getDepartment().getId() == Constants.DEPARTMENT_ID_ACTRA_PRODUCTION && production.getType().isCanadaTalent()) {
					ApproverUtils.addContactToApprovalPool(contact, SessionUtils.getCurrentProject()); // LS-3661
				}

				clearLists(); // add new guy to contact list in view
				setupSelectedItem(contact.getId());
				//TODO Temporary solution for the issue (new contact's Startform).
				if (contact != null) {
					updateSessionData(contact.getId());
				}
				setSelectedTab(0);
				getContactEmploymentList();
				// If the role chosen was in a department that's not already "active",
				updateDeptMask(new BitMask()); // make it active now.
				saveDeptMask = DepartmentDAO.getInstance().findMaskForRoles(contact.getEmployments());
//				addClientResize();
				setup();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Method to check if the contact has more than one Employment whose project
	 * value matches the current project
	 *
	 * @return defRole value
	 */
	private boolean checkExistingProject() {
		int matches = 0;
		boolean defRole = true;
		Project project = SessionUtils.getCurrentProject();
		for (Employment emp : contact.getEmployments()) {
			if (project.equals(emp.getProject())) {
				matches++;
				if (matches == 2) {
					defRole = false;
					break;
				}
			}
		}
		return defRole;
	}

	/**
	 * Action method for the "Re-Invite" button - re-send the email inviting the
	 * current Contact to the current Production.
	 * <p>
	 * If the logged-in user is a Team employee, this will also generate a
	 * pop-up message with a link that may be directly mailed to the selected
	 * contact. This is useful when the email from Lightspeed is being blocked
	 * by a spam filter.
	 *
	 * @return null navigation string
	 */
	public String actionReinvite() {
		if (contact != null) {
			contact = getContactDAO().refresh(contact);
			User user = contact.getUser();
			String inviteLink = null;
			if (user.getStatus() == UserStatus.PENDING) {
				// User has not completed registration - assume this production created user account.
				inviteLink = DoNotification.getInstance().inviteNewAccountFirst(contact);
			}
			else {
				// User has completed registration - assume this production just invited them.
				DoNotification.getInstance().inviteContact(contact);
			}
			MsgUtils.addFacesMessage("Contact.ReinviteSent", FacesMessage.SEVERITY_INFO);
			if (SessionUtils.getCurrentUser().getTeamEmployee()) { // LS-2465
				// for Team employees, display the link in case a direct email is needed
				String msgId = "Contact.ReinviteLink";
				if (inviteLink == null) {
					// contact has completed registration; generate a "reset password" link
					inviteLink = PasswordBean.createResetURI(user.getId(), Constants.TIME_EMAIL_LINK_VALID);
					msgId = "Contact.ResetPasswordLink";
				}
				PopupInputBean bean = PopupInputBean.getInstance();
				bean.show(this, 0, msgId + ".Title", msgId + ".Text", "Confirm.OK", null);
				bean.setInput(inviteLink);
			}
			forceLazyInit();
		}
		return null;
	}

	/**
	 * Jump from one Contact to another contact, within the Contact View page. The new contact's
	 * database id should be the "currentRow" attribute of the event.
	 *
	 * @param evt
	 */
	public void contactLinkJump(ActionEvent evt) {
		log.debug("");
		try {
			Integer contactid = (Integer)evt.getComponent().getAttributes().get("currentRow");
			setSelectedTab(0);
			setupSelectedItem(contactid);
			scrollToRow(); // ensure new contact is visible in list
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Jump to the Start Form view page if permissions allow.
	 *
	 * @return Navigation string for the Start Forms page, if the user is
	 *         allowed to go there, otherwise null.
	 */
	// not using this method currently
	/*public String actionViewStarts() {
		String nav = HeaderViewBean.PEOPLE_MENU_STARTS;

		// Set property for "Return" button on Start Form page
		SessionUtils.put(Constants.ATTR_START_FORM_BACK_PAGE, HeaderViewBean.PEOPLE_MENU_CONTACTS);

		// Set properties so Start Form will open one of this contact's Start Forms
		SessionUtils.put(Constants.ATTR_CONTACT_ID,contact.getId()); // we know the Contact id
		SessionUtils.put(Constants.ATTR_START_FORM_ID, null);		// we don't know the start form id

		// if user tries to view start forms for a contact other than himself,
		// make sure he has the appropriate permission; otherwise put up an error.
		if (! contact.getId().equals(SessionUtils.getCurrentContact().getId())) {
			if (! AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_VIEW_STARTS)) {
				MsgUtils.addFacesMessage("Contact.NoViewPermission", FacesMessage.SEVERITY_ERROR);
				nav = null;
			}
		}
		return nav;
	}*/

	public void actionOpenChangeEmp(ActionEvent evt) {
		try {
//			showAddRole = true;
//			newMember = new ProjectMember();
			showAddOccupation = true;
			CreateNewOccupationBean selectBean = CreateNewOccupationBean.getInstance();
			if (/*editMode &&*/ currentEmployment == null || currentEmployment.getProject() == null) {
				selectBean.setProject(SessionUtils.getCurrentProject());
				if (currentEmployment == null) {
					currentEmployment = findDefaultEmployment();
				}
			}
			else { // viewing existing Employment record
				selectBean.setProject(currentEmployment.getProject());
			}
			//currentEmployment = selectBean.getEmploymentToEdit();
			selectBean.show(this, currentEmployment, ACT_CHANGE_EMP, "CreateNewOccupationBean.OccupationDetails.", false);
			selectBean.setCreateNewRole(true);
			selectBean.setShowRoleName(false);
			if (currentEmployment != null) {
				/*if (currentEmployment.getStartForm() != null) {
					selectBean.setStartDate(currentEmployment.getStartForm().getEffectiveOrStartDate());
				}*/
				selectBean.setEndDate(currentEmployment.getEndDate());
				selectBean.setDepartment(currentEmployment.getDepartment());
				selectBean.setRole(currentEmployment.getRole());
				selectBean.setEmploymentRole(currentEmployment.getRole());
				log.debug("Existing role: " + currentEmployment.getRole().getName());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
	}

	/**
	 * Action method for the "Save/Ok" button on the Create Occupation (Role)
	 * pop-up dialog.
	 *
	 * @return null navigation string
	 */
	public String actionAddRole() {
		log.debug("");
		try {
			addButtonClicked();
			CreateNewOccupationBean selectBean = CreateNewOccupationBean.getInstance();
			if (! selectBean.actionSave()) {
				return null;
			}

//				if (selectBean.getIsCreate()) {
					if (! showProject) { // showing all users...
						// force refresh of project-only list, it may change; rev 4071.
						projectContacts = null;
					}
					addClientResizeScroll();
//				}
				selectBean.actionOk();
				showAddOccupation = false;
//			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * The Action method for the Cancel button on the "Create Position"
	 * dialog box.
	 * @return null navigation string
	 */
	public String actionCancelAddRole() {
		//newMember = null;
//		showAddRole = false;
		showAddOccupation = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * The Action method of the Delete icon ("X") for individual roles on the
	 * Cast&Crew Occupation tab. The field 'employmentToDelete' should be set
	 * already, via the f:setPropertyActionListener tag, to the instance of the
	 * Employment to be deleted. If the user only has a single Cast role left,
	 * and it is being deleted, we issue a prompt before proceeding. Note that
	 * if the Employment (occupation) was just added in this edit cycle (prior
	 * to Save), it will not have been persisted yet.
	 *
	 * @return null navigation string
	 */
	public String actionDeleteEmployment() {
		log.debug("");
		String res = null;
		try {
			if (employmentToDelete != null) {
				if (employmentToDelete.getDepartmentId().equals(Constants.DEPARTMENT_ID_CAST)) {
					int castCount = 0;
					//Integer castId = null;
					for (Employment emp : contact.getEmployments()) {
						if (emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_CAST)) {
							castCount++;
							if (castCount > 1) {
								break;
							}
							//castId = emp.getId();
						}
					}
					if (castCount == 1) {
						// User has a single Cast role, and it is being deleted
						PopupBean.getInstance().show(this, ACT_DELETE_ROLE, getMessagePrefix()+"DeleteCastRole.");
						log.debug("");
						return null;
					}
					else {
						log.debug("");
					}
				}
				res = actionDeleteEmploymentOk();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return res;
	}

	/**
	 * Value change listener for the "Assistant" drop-down list.
	 * @param evt
	 */
	public void listenAssistant(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) { // null happens, don't know why!
				Integer id = Integer.parseInt(evt.getNewValue().toString());
				log.debug("id=" + id);
				if (id.intValue() == -1) {
					log.debug("clearing assistant");
					getContact().setAssistant(null);
				}
				else {
					Contact assistantContact = getContactDAO().findById(id);
					if (assistantContact != null) {
						getContact().setAssistant(assistantContact);
						setupAssistantTab();
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener method for the Department list drop-downs in the list
	 * of roles on the Positions mini-tab. When the user picks a different
	 * department, we re-build the list of roles available in the role
	 * drop-downs for each project membership.
	 *
	 * @param evt The change event passed by the framework. The newValue in the
	 *            event is the database id of the Department selected (the value
	 *            field from the SelectItem chosen).
	 */
	/*public void listenDepartment(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			if (id != null) {
				log.debug("dept=" + id);
				//addRoleMap(roleMap, id);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}*/

	/**
	 * ValueChangeListener for the Role drop-down list on the Positions
	 * mini-tab.  The purpose of this method is to propagate customized
	 * permission masks from existing Role`s into changed entries that
	 * are for the same Role in a different Project.
	 *
	 * @param evt The change event from the framework. The evt.newValue is the
	 *            Role selected.
	 */
	/*public void listenRole(ValueChangeEvent evt) {
		try {
			Role role = (Role)evt.getNewValue();
			if (role != null) {
				// log.debug("role=" + role.toString());
				ProjectMember pm = (ProjectMember)ServiceFinder.getManagedBean("projectMember");
				pm.getEmployment().setRole(role);
				Long mask = roleMasks.get(role.getId());
				if (mask == null) {
					mask = role.getRoleGroup().getPermissionMask();
				}
				pm.getEmployment().setPermissionMask(mask);
				log.debug(pm.toString());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}*/

	/**
	 * ValueChangeListener for the "send script in advance" checkbox.
	 * Sets the "advance days" value to 2 (the default) when the user
	 * sets the checkbox on.
	 *
	 * @param evt The change event from the framework.
	 */
	public void listenSendAdvanceScript(ValueChangeEvent evt) {
		Boolean b = (Boolean)evt.getNewValue();
		contact.setSendAdvanceScript(b ? 2 : 0);
		log.debug(b + ", n=" + contact.getSendAdvanceScript());
	}

	/**
	 * Value Change Listener for check box selecting whether to
	 * show "all contacts" or "project-only contacts".
	 */
	public void listenShowProject(ValueChangeEvent event) {
		try {
			boolean b = (Boolean)event.getNewValue();
			if (! b && showProject) {
				// list will probably get longer; recalculate selected row for scrolling.
				showProject = false;
				setSelectedRow(-1); // clear so it will be recalculated
				scrollToRow();
			}
			else if (b && ! showProject) {
				// list will probably get shorter; make sure selected contact is showing
				if (allContacts != null) {
					for (Contact c : allContacts) {
						c.setSelected(false);
					}
					setSelectedRow(-1); // clear so it will be recalculated
					showProject = true;
					scrollToRow(); // scroll to previously selected item
					if (getSelectedRow() >= 0) { // worked, mark item selected
						setSelected(getItemList().get(getSelectedRow()),true);
					}
					else if (getItemList().size() > 0) {
						// old selected item not in new list, select the first instead
						setSelectedRow(0);
						setupSelectedItem(null); // force right-side page update
					}
				}
			}
			showProject = b;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addClientResizeScroll();
	}

	private void saveNew() {
		try {
			if (contact.getEmailAddress() == null || contact.getEmailAddress().trim().length() == 0) {
				contact.setEmailAddress(generateEmailAddress(contact));
			}
			if (saveQuickAdd) {
				if (quickContact.save()) {
					contact.setAssistant(quickContact.getContact());
				}
			}

			getContactDAO().save(getContact()); // User saved by cascade action
			ChangeUtils.logChange(ChangeType.CONTACT, ActionType.CREATE, "create Contact=" + contact.getUser().getDisplayName());
			ChangeUtils.logChange(ChangeType.USER, ActionType.CREATE, "create Contact->user=" + contact.getEmailAddress());

//			if (getContact().getDepartment() != null) {
//				DepartmentDAO departmentDAO = (DepartmentDAO) ServiceFinder.findBean("DepartmentDAO");
//				departmentDAO.merge(getContact().getDepartment());
//			}
			setContactDL(null); // force refresh of drop-down list
			clearLists(); // add new guy to contact lists in view
			updateSessionData(getContact().getId());
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private void createUser(Contact pContact) {
		User user = new User(getProduction().getType().isCanadaTalent());
		pContact.setProject(SessionUtils.getCurrentProject());
		pContact.setEmailAddress(generateEmailAddress(pContact));
		getContactDAO().attachContactUser(pContact, user, false, null);
	}

	public static String generateEmailAddress(Contact contact) {
		String name = contact.getEmailAddress();
		if (name == null || name.trim().length() == 0) {
			name = contact.getUser().getFirstName()+contact.getUser().getLastName();
		}
		name = name.trim();
		if (name.length() > Constants.MAX_EMAIL_ADDR_LENGTH-12) {
			name = name.substring(0,Constants.MAX_EMAIL_ADDR_LENGTH-12);
		}
		name += "@unknown.com";
		if (UserDAO.getInstance().existsEmailAddress(name)) {
			if (name.length() > Constants.MAX_USER_NAME_LENGTH-3) {
				name = name.substring(0,Constants.MAX_USER_NAME_LENGTH-3);
			}
			String prefix = name;
			for (int i=2; i < 1000; i++) {
				name = prefix + i;
				if ( ! UserDAO.getInstance().existsEmailAddress(name)) {
					break;
				}
			}
		}
		return name;
	}

	/**
	 * Validate and clean various input fields. In the case of invalid data,
	 * this method will issue a FacesMessage and return false.
	 *
	 * @return True if data passed validation; false if one or more fields were
	 *         invalid, in which case a message has already been issued to the
	 *         user.
	 */
	private boolean checkFields() {
		boolean bRet = false;
		try {
			checkImdb();
			int ret = UserUtils.validateEmail(contact, false);
			bRet = (ret == 0);
			if (! bRet) {
				setSelectedTab(TAB_DETAIL);
			}
			if (ret > 0) {
				PopupBean conf = PopupBean.getInstance();
				conf.show("Contact.DuplicateEmailInfo.");
				String msg = MsgUtils.formatMessage("Contact.DuplicateEmailInfo.Text",
						contact.getEmailAddress(), saveEmailAddress);
				conf.setMessage(msg);
			}
			else if (checkLostProdAdmin()) {
				PopupBean conf = PopupBean.getInstance();
				conf.show("Contact.CantDeleteAdminRole.");
				String msg = MsgUtils.formatMessage("Contact.CantDeleteAdminRole.Text",
						contact.getUser().getFirstNameLastName());
				conf.setMessage(msg);
				setSelectedTab(TAB_DETAIL);
				bRet = false;
			}
			else if (checkLostFinancialAdmin()) {
				PopupBean conf = PopupBean.getInstance();
				conf.show("Contact.CantDeleteFinancialAdminRole.");
				String msg = MsgUtils.formatMessage("Contact.CantDeleteFinancialAdminRole.Text",
						contact.getUser().getFirstNameLastName());
				conf.setMessage(msg);
				setSelectedTab(TAB_DETAIL);
				bRet = false;
			}
			if (! checkName(contact)) {
				setSelectedTab(TAB_DETAIL);
				bRet = false;
			}
			// Check for two identical roles in the same unit...
			List<String> roleKeys = new ArrayList<>();
			String key;
			for (Employment emp : getContactEmploymentList()) {
				if (emp.getRole() == null) {
					MsgUtils.addFacesMessage("Contact.MissingRole2", FacesMessage.SEVERITY_ERROR);
					bRet = false;
					break;
				}
				Project project = emp.getProject();
				if (emp.getProject() != null) { // Commercial production and not Admin role
					//project = ProjectDAO.getInstance().refresh(project);
					key = project.getId().toString();
				}
				else { // Admin (cross-project) or TV/Feature occupation
					key = "";
				}
				key += "-" + emp.getRole().getId();
				if (roleKeys.contains(key)) {
					MsgUtils.addFacesMessage("Contact.DuplicateRoles", FacesMessage.SEVERITY_ERROR);
					setSelectedTab(TAB_DETAIL);
					bRet = false;
					break;
				}
				roleKeys.add(key);
			}
			if (! getSendAdvanceScript()) {
				contact.setSendAdvanceScript(0);
			}
			else if (contact.getSendAdvanceScript() == null || contact.getSendAdvanceScript() == 0) {
				contact.setSendAdvanceScript(2);
			}
		}
		catch (Exception e) {
			bRet = false;
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return bRet;
	}

	/**
	 * Determine if the user is trying to Save changes that will result in no
	 * Prod Data Admin roles left in the production.
	 *
	 * @return True iff there is a problem -- there won't be a Prod Data Admin
	 *         left in the production if the Save completes.
	 */
	private boolean checkLostProdAdmin() {
		AuthorizationBean auth = AuthorizationBean.getInstance();
		if (auth.isAdmin()) {
			// LS Admin can do no wrong!
			return false;
		}
		if (! auth.isDataAdmin()) {
			// only ProdAdmin should be able to remove a Prod Admin role
			return false;
		}
		if (! SessionUtils.getCurrentUser().getId().equals(contact.getUser().getId())) {
			// if editing a different person, no problem (current user is still ProdAdmin)
			return false;
		}
		// if at least one remaining Employment.role is a data admin, then ok to proceed
		for (Employment emp : contact.getEmployments()) {
			if (emp.getRole() != null && emp.getRole().isDataAdmin()) {
				// contact being updated still has Prod Admin role, so we're fine.
				return false;
			}
		}
		// User is removing their own ProdAdmin role.
		// That's a problem if there are no other ProdAdmin's in the production.
		return ! getProjectMemberDAO().existsOtherProdAdmin(contact, contact.getProduction());
	}

	/**
	 * Determine if the user is trying to Save changes that will result in no
	 * Financial Data Admin roles left in the production.
	 *
	 * @return True iff there is a problem -- there won't be a Financial Data
	 *         Admin left in the production if the Save completes.
	 */
	private boolean checkLostFinancialAdmin() {
		AuthorizationBean auth = AuthorizationBean.getInstance();
		if (auth.isAdmin()) {
			// LS Admin can do no wrong!
			return false;
		}
		if (! auth.isFinancialAdmin()) {
			// only Financial Data Admin should be able to remove a similar role
			return false;
		}
		if (! SessionUtils.getCurrentUser().getId().equals(contact.getUser().getId())) {
			// if editing a different person, no problem (current user is still ProdAdmin)
			return false;
		}
		// if at least one remaining Employment.role is a financial admin, then ok to proceed
		for (Employment emp : contact.getEmployments()) {
			if (emp.getRole() != null && emp.getRole().isFinancialAdmin()) {
				// contact being updated still has Financial Data Admin role, so we're fine.
				return false;
			}
		}
		// User is removing their own Financial Data Admin role.
		// That's a problem if there are no other Financial Data Admin's in the production.
		return ! getProjectMemberDAO().existsOtherFinancialAdmin(contact, contact.getProduction());
	}

	/**
	 * Determine if the user is deleting a Contact that will result in no Prod
	 * Data Admin roles left in the production.
	 *
	 * @return True iff there is a problem -- there won't be a Prod Data Admin
	 *         left in the production if the Delete completes.
	 */
	private boolean checkStopDeleteProdAdmin() {
		AuthorizationBean auth = AuthorizationBean.getInstance();
		if (auth.isAdmin()) {
			// LS Admin can do no wrong!
			return false;
		}
		boolean delAdmin = false;
		if (auth.isDataAdmin()) {
			if (! SessionUtils.getCurrentUser().getId().equals(contact.getUser().getId())) {
				// current user is Prod Admin, and deleting someone else...
				// no problem (current user is still ProdAdmin)
				return false;
			}
			delAdmin = true;
		}
		else {
			for (Employment emp : contact.getEmployments()) {
				if (emp.getRole() != null && emp.getRole().isDataAdmin()) {
					// contact being deleted has Prod Admin role, maybe problem
					delAdmin = true;
					break;
				}
			}
		}
		if (! delAdmin) { // contact being deleted is not a ProdAdmin
			return false; // so no problem
		}
		// User is removing a Contact with ProdAdmin role.
		// That's a problem if there are no other ProdAdmin's in the production.
		return ! getProjectMemberDAO().existsOtherProdAdmin(contact, contact.getProduction());
	}

	/**
	 * Determine if the user is deleting a Contact that will result in no
	 * Financial Data Admin roles left in the production.
	 *
	 * @return True iff there is a problem -- there won't be a Financial Data
	 *         Admin left in the production if the Delete completes.
	 */
	private boolean checkStopDeleteFinancialAdmin() {
		AuthorizationBean auth = AuthorizationBean.getInstance();
		if (auth.isAdmin()) {
			// LS Admin can do no wrong!
			return false;
		}
		boolean delAdmin = false;
		if (auth.isFinancialAdmin()) {
			if (! SessionUtils.getCurrentUser().getId().equals(contact.getUser().getId())) {
				// current user is Financial Data Admin, and deleting someone else...
				// no problem (current user is still Financial Data Admin)
				return false;
			}
			delAdmin = true;
		}
		else {
			for (Employment emp : contact.getEmployments()) {
				if (emp.getRole() != null && emp.getRole().isFinancialAdmin()) {
					// contact being deleted has Financial Data Admin role, maybe problem
					delAdmin = true;
					break;
				}
			}
		}
		if (! delAdmin) { // contact being deleted is not a ProdAdmin
			return false; // so no problem
		}
		// User is removing a Contact with ProdAdmin role.
		// That's a problem if there are no other Financial Data Admin`s in the production.
		return ! getProjectMemberDAO().existsOtherFinancialAdmin(contact, contact.getProduction());
	}

	public static boolean checkName(Contact pContact) {
		boolean ret = true;
		if (pContact.getUser().getFirstName() != null) {
			pContact.getUser().setFirstName(pContact.getUser().getFirstName().trim());
		}
		if (pContact.getUser().getLastName() != null) {
			pContact.getUser().setLastName(pContact.getUser().getLastName().trim());
		}
		if (pContact.getUser().getFirstName() == null || pContact.getUser().getFirstName().length() == 0) {
			MsgUtils.addFacesMessage("Contact.BlankFirstName", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		if (pContact.getUser().getLastName() == null || pContact.getUser().getLastName().length() == 0) {
			MsgUtils.addFacesMessage("Contact.BlankLastName", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		return ret;
	}

	/**
	 * If the HomeAddress instance is empty, throw it out. Otherwise, update
	 * it in the database.
	 */
	private void checkSaveAddress() {
		Address addr = getContact().getHomeAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				contact.setHomeAddress(addr);
			}
			else {
				contact.setHomeAddress(null);
			}
		}
	}

	private void checkImdb() {
		if (contact.getUser().getImdbLink() != null) {
			String link = contact.getUser().getImdbLink().toLowerCase();
			if (link.startsWith("http://")) {
				contact.getUser().setImdbLink(contact.getUser().getImdbLink().substring(7));
			}
		}
	}

	/**
	 *  Called during Save processing to update role-related information in
	 *  Contact and Department, and commit deleted Images, as this needs to
	 *  be done prior to the "refresh" we do here to avoid Lazy init errors.
	 */
	private String actionSaveChanges() {
		List<Employment> newEmps = new ArrayList<>();
		if (getContactEmploymentList() != null) {
			for (Employment emp : getContactEmploymentList()) {
				if (emp.getId() != null && emp.getId() < 0) { // temporary value for added roles
					emp.setId(null); // prepare for save, which assigns new id
					newEmps.add(emp); // gather list for (possibly) creating StartForms
				}
				// rev 2.9.5670 - Duplicate instances of Role in hibernate session;
				// drop-down had newer instances and these were pushed into PM's by JSF cycle.
				Role role = RoleDAO.getInstance().refresh(emp.getRole()); // Get fresh copy
				emp.setRole(role);	// make sure they're all using fresh ones.
			}
		}
		log.debug("... before actionSave()");
		// TODO Exception executing batch, most frequently after deleting multiple occupations at one time.
		//org.hibernate.StaleStateException: Batch update returned unexpected row count from update [0]; actual row count: 0; expected: 1
		contact = getContactDAO().actionSave(contact, newEntry, getContactEmploymentList());
		log.debug("... after actionSave()");
		contact = getContactDAO().refresh(contact);
		log.debug("... after refresh");
		commitImages(); // actually delete images from db that user 'deleted' on this cycle.
		ProductionType type = SessionUtils.getProduction().getType();
		if (type.isAicp() || type.isCanadaTalent()) {
			// For commercial, user had option (in popup) to create a matching StartForm;
			// For Canada, we always create a StartForm (not seen by users)
			for (Employment emp : newEmps) {
				StartForm relatedSf = null;
				StartForm newStartForm = null;
				if (emp.getRelatedStartFormId() != null) {
					relatedSf = getStartFormDAO().findById(emp.getRelatedStartFormId());
				}
				if (relatedSf != null) {
					newStartForm = StartFormService.createStartForm(
							StartForm.FORM_TYPE_NEW, null, relatedSf, contact, emp, emp.getProject(),
							CalendarUtils.todaysDate(), null,
							null, emp.getCopyStartData(), true);
				}
				else if (type.isCanadaTalent()) {
					newStartForm = StartFormService.createStartForm(
							StartForm.FORM_TYPE_NEW, null, null, contact, emp, emp.getProject(),
							CalendarUtils.todaysDate(), null,
							null, false, true);
				}
				if (newStartForm != null) {
					newStartForm.setSequence(getStartFormDAO().findMaxSequence(contact) + 1);
					newStartForm.setFormNumber(contact.getId() + "-" + newStartForm.getSequence());
					getStartFormDAO().attachDirty(newStartForm);
				}
			}
		}

		// Without evicting the user/role/dept, we had LazyInitializationException's
		// either in AuthorizationBean, forceLazyInit, or during page render. Seems related to
		// ContactDAO.actionSave() code that updates ProjectMember & Contact's role/dept data.
		getContactDAO().evict(contact.getUser());
		if (contact.getRole() != null) {
			getContactDAO().evict(contact.getRole());
		}
		contact = getContactDAO().refresh(contact); // bug#284, rev 1835, LazyInitializationException
		if (contact.equals(SessionUtils.getCurrentContact())) {
			AuthorizationBean.getInstance().auth(contact, true); // update role permissions
			AddContactBean.getInstance().setDepartmentDL(null);
			CreateNewOccupationBean.getInstance().setDepartmentDL(null);
			editableUnits = null; // refresh when next needed, in case own roles changed
//			validDept = null;	// ditto, enabled-for-edit department list may change
			departmentDL = null; // ditto, selection department list may change
		}
		forceLazyInit();
		return null;
	}

	private void updateDeptMask(BitMask priorMask) {
		// See if any roles were added in departments that are not already "active"
		BitMask deptMask = DepartmentDAO.getInstance().findMaskForRoles(contact.getEmployments());
		if (! priorMask.equals(deptMask)) { // set of departments involved changed.
			deptMask.andNot(priorMask); // Turn off all originally set bits (departments).
			if (! deptMask.isEmpty()) { // Any left on? If so, departments were added.
				// we need to make sure they are active for the Production/project.
				BitMask activeMask = SessionUtils.getDeptMask();
				deptMask.andNot(activeMask);
				if (! deptMask.isEmpty()) { // any left on? If so, make them active.
					activeMask.or(deptMask);
					DepartmentUtils.updateMask(activeMask);
				}
			}
		}
	}

	/**
	 * Method called when user selects a 'mini-tab'. Don't let user switch away
	 * from the Start Form mini-tab if it is in Edit mode.
	 *
	 * @see com.lightspeedeps.web.view.View#setSelectedTab(int)
	 */
	@Override
	public void setSelectedTab(int n) {
		if (getSelectedTab() == TAB_STARTS || n == TAB_STARTS) {
			contact = getContactDAO().refresh(contact);
			for (StartForm sf : contact.getStartForms()) {
				sf.getRate();
			}
		}
		super.setSelectedTab(n);
	}

	@Override
	protected void setupTabs() {
		if (contact != null) { // only null if initialization failed
			setupAssistantTab();
			//getStartFormBean().setupStartTab(getSelectedTab());
		}
	}

	/**
	 * Create the roleMasks mapping, used when saving a new role.
	 */
	private void createRoleMasks() {
		log.debug("");
		roleMasks = new HashMap<>();
		for (Employment emp : contact.getEmployments()) {
			if (emp.getRole() != null) {
				roleMasks.put(emp.getRole().getId(), emp.getPermissionMask());
				//log.debug("role=" + emp.getRole().getId() + ", mask=" + emp.getPermissionMask());
			}
		}
	}

	/**
	 * For Assistant tab, make sure the assistant (if any) has a home address object,
	 * so jsp won't fail displaying it.
	 */
	private void setupAssistantTab() {
		if (getSelectedTab() == TAB_DETAIL) {
			if (contact.getAssistant() != null) {
				setAssistantId(contact.getAssistant().getId());
				if (contact.getAssistant().getHomeAddress() == null) {
					Address addr = new Address();
					contact.getAssistant().setHomeAddress(addr);
				}
			}
			else {
				setAssistantId(-1);
			}
		}
	}

	private void createContactDL() {
		log.debug("");
		contactDL = getContactDAO().createContactSelectList(getContactDAO().findCrew(false), true);
	}

	/**
	 * Fill in the editableUnits map.
	 * See {@link #editableUnits}.
	 * @return A non-null Map.
	 */
	private Map<Integer, Boolean> createEditableUnits() {
		Map<Integer, Boolean> unitMap = new HashMap<>();
		Permission perm = Permission.EDIT_CONTACTS_CREW;
		if (contact.getIsCast()) {
			perm = Permission.EDIT_CONTACTS_CAST;
		}
		else if (contact.getIsNonProd()) {
			perm = Permission.EDIT_CONTACTS_NONPROD;
		}
		List<Integer> pList = getProjectMemberDAO().findByContactPermissionDistinctUnits(getvContact(), perm);
		for (Integer pid : pList) {
			unitMap.put(pid, true);
		}
		return unitMap;
	}

	/**
	 * Create a List of Project SelectItem's for the Add Role pop-up.
	 *  See {@link #editableProjectDL}.
	 * @return the non-null List.
	 */
	private List<SelectItem> createEditableProjectDL() {
		List<SelectItem> projList = new ArrayList<>();
		unitDLmap = new HashMap<>();
		Set<Integer> pids = getEditableUnits().keySet();
		UnitDAO unitDAO = UnitDAO.getInstance();
		List<SelectItem> unitList;
		Set<Integer> projectIds = new HashSet<>();
		for (Integer id : pids) {
			Unit aUnit = unitDAO.findById(id);
			Project proj = aUnit.getProject();
			// don't add projects to select list more than once:
			if (projectIds.add(proj.getId())) {
				projList.add(new SelectItem(proj.getId(), proj.getTitle()));
			}
			// add or update list of Units corresponding to this project
			unitList = unitDLmap.get(proj.getId());
			if (unitList == null) {
				unitList = new ArrayList<>();
				unitDLmap.put(proj.getId(), unitList);
			}
			unitList.add(new SelectItem(aUnit.getId(), aUnit.getName()));
		}
		return projList;
	}

//	/**
//	 * Open the "Add New Assistant" dialog, for the purpose of creating a new Contact, and making
//	 * that Contact the assistant for the Contact being edited. This uses the QuickContactBean,
//	 * which supplies most of the services for the dialog.
//	 *
//	 * @return navigation string
//	 */
//	public String actionOpenQuickAdd() {
//		try {
//			log.debug(this + ", " + newEntry + ", contact=" + contact);
//			quickContact = (QuickContactBean) ServiceFinder.findBean("quickContactBean");
//			quickContact.setDepartmentId(Constants.DEPARTMENT_ID_SET_PROD);
//			quickContact.show();
//			Role asst = RoleDAO.findByName(RoleDAO.PRODUCTION_ASSISTANT);
//			quickContact.getProjectMember().setRole(asst);
//			showAsst = true;
//			showAddContact = true;
//			addPopupTitle = "Add New Assistant";
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return Constants.SUCCESS;
//	}

//	/**
//	 * The Action method for the Save button on the Add New Assistant dialog. This uses the
//	 * QuickContactBean, which supplies most of the services for the dialog.
//	 *
//	 * @return
//	 */
//	private String actionAddAsst() {
//		log.debug(this + ", " + newEntry);
//		try {
//			if ( ! newEntry) {
//				saveQuickAdd = false;
//				if (quickContact.save()) {
//					contact.setAssistant(quickContact.getContact());
//					setContactDL(null); // force refresh of drop-down list
//					refreshContactList();
//					setupAssistantTab();
//				}
//			}
//			else if (quickContact.validateData()) {
//				saveQuickAdd = true;
//				quickContact.setVisible(false);
//			}
//			showAsst = quickContact.getVisible();
//			showAddContact = showAsst;
//			addClientResizeScroll();
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return Constants.SUCCESS;
//	}

//	/**
//	 * The Action method for Cancel button in the add assistant contact popup.
//	 * @return
//	 */
//	public String actionCancelAsst() {
//		showAsst = false;
//		return quickContact.actionCancel();
//	}

	/**
	 * When the user presses the Delete button to delete an image,
	 * the ImagePaginatorBean is called; it calls back to us at
	 * two methods: getImageList, to get the list of images currently
	 * displayed; and removeImage, for the image to be removed
	 * from this object's set of images.  The ImagePaginatorBean
	 * takes care of deleting the Image object from the database.
	 * @return List of Image
	 */
	@Override
	public List<Image> getImageList() {
		return contact.getImageList();
	}

	/**
	 * Callback from ImagePaginatorBean when user has confirmed to
	 * delete an image.
	 */
	@Override
	public void removeImage(Image image) {
		boolean rem = false;
		if (contact.getCastMember() != null) {
			rem = contact.getCastMember().getImages().remove(image);
		}
		else {
			rem = contact.getImages().remove(image);
		}
		// Refresh image resources list
		contact.setImageResources(null);
		log.debug("image="+image+", removed="+rem);
	}

	// * * * Support for "New Image" button * * *
	@Override
	public String actionOpenNewImage() {
		if (contact.getCastMember() == null && contact.getIsCast()) {
			ContactDAO.createCastMember(contact);
			// The RWE this created will be saved by cascade if/when user saves the Contact
		}
		return super.actionOpenNewImage();
	}

	/**
	 * Callback from ImageAddBean when the user adds a new image to our object.
	 */
	@Override
	public void updateImage(Image image, String filename) {
		log.debug("");
		try {
			if (image != null) {
				if (contact.getCastMember() != null) {
					image.setRealWorldElement(contact.getCastMember());
					contact.getCastMember().getImages().add(image);
				}
				else {
					image.setContact(contact);
					contact.getImages().add(image);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** See {@link #addedImages}. */
	@Override
	public Set<Image> getAddedImages() {
		return addedImages;
	}

	/** See {@link #addedImages}. */
	@Override
	public void setAddedImages(Set<Image> addedImages) {
		this.addedImages = addedImages;
	}

	/** Called by the popupBean when the user clicks OK
	 * on a confirmation dialog. */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_DELETE_ROLE:
					res = actionDeleteEmploymentOk();
					addClientResizeScroll();
					break;
				case ACT_CHANGE_EMP:
					// actionAddRole() was already called from JSP.
					break;
				case ACT_CREATE_OCCUPATION:
					//res = actionAddRole();
					break;
				default:
					res = super.confirmOk(action);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		try {
			switch(action) {
				case ACT_DELETE_ROLE:
					break;
				case ACT_CHANGE_EMP:
				case ACT_CREATE_OCCUPATION:
					showAddOccupation = false;
					break;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		super.confirmCancel(action);
		return null;
	}

	private void clearLists() {
		projectContacts = null;
		allContacts = null;
		contactEmploymentList = null;
	}

//	private void refreshContactList() {
//		clearLists();
//		createAllContacts();
//		scrollToRow();
//		updateContactInList(contact);
//	}

	/**
	 * Generate the list of "all" contacts, which means across all projects, but
	 * it is still filtered by the user's permission to see cast and/or crew.
	 * While it is expected that any user with "view cast" permission will also
	 * have "view crew" permission, the code has been written to support the case
	 * where a user has only "view cast" and not "view crew".
	 */
	private void createAllContacts() {
		if (projectContacts == null) {
			createProjectContacts();
		}

		// Use projectContacts, plus (maybe) additional, so default dept/role is
		// for this project, and picked in sorted order.
		// Even for non-episodic productions, we need to do this additional lookup, as it
		// will find Contacts who are members of the production but do not have any Role.
		Set<Contact> contacts = new HashSet<>(projectContacts);
		contacts.addAll(getContactDAO().createAllContactsSet(showCast, showCrew, showAdmin));
		allContacts = new ArrayList<>(contacts);
		sort();
	}

	/**
	 * Generate the list of Contacts who are assigned at least one role in the
	 * current project -- that is, they are "members" of the project.  This list
	 * also is filtered by the user's permission to see cast vs crew.
	 */
	private void createProjectContacts() {
		projectContacts = getContactDAO().findByProjectActive(SessionUtils.getCurrentProject(),
				showCast, showCrew, showAdmin);
		sort();
	}

	/**
	 * Perform the "delete" of a Contact -- which doesn't actually delete the Contact
	 * object from the database, but sets its status to Deleted, removes any Role`s
	 * it has, and removes it from other ancillary connections.
	 * @param delContact The Contact to be removed from the current Production.
	 */
	private void deleteContact(Contact delContact) {
		delContact = getContactDAO().findById(delContact.getId()); // refresh
		delContact.setStatus(MemberStatus.DELETED);
		// ensure User and Contact email's match, so we don't get problems during "re-invites"
		delContact.setEmailAddress(delContact.getUser().getEmailAddress());

		getContactDAO().removeContact(delContact);

		// remove Contact as a time-keeper if necessary...
		DepartmentDAO.getInstance().updateTimeKeeper(delContact);

		clearLists(); // force recreation of contact lists
	}

	/**
	 * Mark a single Contact object in the list as selected. This entails scanning the list and
	 * marking all the other Contacts as not selected, so they will be displayed properly on the
	 * page.
	 *
	 * @param pContact
	 */
	private boolean select(Contact pContact) {
		pContact.setSelected(true);
		RowState state = new RowState();
		state.setSelected(true);
		getStateMap().put(pContact, state);
		//log.debug(contact);
		List<Contact> list = getContactList();
		int i = 0, ix = -1;
		for (Contact c : list) {
			if (pContact.getId().equals(c.getId())) {
				ix = i;
				c.setSelected(true);
				selectRowState(pContact);
			}
			else if (c.getSelected()) {
				c.setSelected(false);
			}
			i++;
		}
		/*int i = 0, ix = -1;
		for (Object item : getItemList()) {
			Contact ct = (Contact)item;
			if (contact.getId().equals(ct.getId())) {
				ix = i;
				((Contact)item).setSelected(true);
			}
			else if (ct.getSelected()) {
				((Contact)item).setSelected(false);
			}
			i++;
		}*/
		boolean bRet = true;
		if (ix >= 0) {
			log.debug("selected row: " + ix);
			setSelectedRow(ix);
		}
		else {
			log.debug("contact not found");
			bRet = false;
		}
		return bRet;
	}

	private void updateContactInList(Contact pContact) {
		List<Contact> list = getContactList();
		int ix = list.indexOf(pContact);
		if (ix < 0) {
			ix = getSelectedRow();
		}
		if (ix >= 0) {
			log.debug("row: " + ix);
			list.set(ix, pContact);
		}
	}

	private void refreshDepartments(List<Contact> contacts) {
		if (contacts != null) {
			@SuppressWarnings("unused")
			String str;
			RoleDAO roleDAO = RoleDAO.getInstance();
			for (Contact ct : contacts) {
				Role role;
				if ((role = ct.getRole()) != null) {
					role = roleDAO.refresh(role);
					ct.setRole(role);
					if (role.getDepartment() != null) {
						str = role.getDepartment().getName();
					}
				}
			}

		}
	}

	/**
	 * Sorts the list of Contact data.
	 */
	@Override
	protected void sort() {
		try {
			if (getSortColumnName().equals(Contact.SORTKEY_DEPT)) {
				refreshDepartments(allContacts);
				refreshDepartments(projectContacts);
			}
			super.sort();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * If no item is currently selected, find the current element in
	 * the main (left-hand) list, select it, and send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	private void scrollToRow() {
		scrollToRow(getElement());
	}

	/**
	 * Method used to create list of employments for the selected contact.
	 */
	private void createEmploymentList() {
		try {
			if (contact != null) {
				contactEmploymentList = new ArrayList<>();
				// TODO Is this refresh still necessary? AM: Yes it is still giving LIE on list.size() in some productions. What to do?
				List<StartForm> list = contact.getStartForms();
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						list.set(i, getStartFormDAO().refresh(list.get(i)));
					}
				}
				AuthorizationBean bean = AuthorizationBean.getInstance();
				boolean isAdmin = bean.isAdmin();
				boolean dataAdmin = bean.isDataAdmin() || bean.isFinancialAdmin();
				log.debug("Employment list size:" + contact.getEmployments().size());
				for (Employment emp : contact.getEmployments()) {
					emp = EmploymentDAO.getInstance().refresh(emp);
					log.debug("Occ:" + emp.getOccupation());
					if (emp.getStatus() == null) { // newly added Employment
						calculateEmpStatus(emp);
					}
					if (isAdmin) {
						log.debug("");
						contactEmploymentList.add(emp);
					}
					else if (! (emp.getRole().isAdmin())) {
						log.debug("");
						if (! dataAdmin && (emp.getRole().isDataAdmin() || emp.getRole().isFinancialAdmin())) {
							emp.setDisableDataAdminEmp(true);
						}
						contactEmploymentList.add(emp);
					}
					if (! production.getAllowOnboarding()) {
						emp.setShowStartsIcon(true);
					}
				}
				production = ProductionDAO.getInstance().refresh(production);
				if (production.getAllowOnboarding()) {
					Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
					DocumentChain chain = DocumentChainDAO.getInstance().findOneByNamedQuery(
							DocumentChain.GET_START_DOCUMENT_CHAIN_OF_PRODUCTION, map("folderId", onboardFolder.getId()));
					for (Employment empl : contactEmploymentList) {
						if (isStartApproverForEmployment(empl, chain)) {
							empl.setShowStartsIcon(true);
						}
						else {
							empl.setShowStartsIcon(false);
						}
					}
				}
				Collections.sort(contactEmploymentList, EmploymentDAO.getEmpComparator());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to check whether the current approver is approver for the given employment's start form or not.
	 * @param emp Employment, to which the startform belongs
	 * @param chain Payroll Start Document chain for the production.
	 * @return True if current contact is approver, else false.
	 */
	private boolean isStartApproverForEmployment(Employment emp, DocumentChain chain) {
		Map<List<Department>, Boolean> currentContactApproverMap = new HashMap<>();
		if (AuthorizationBean.getInstance().hasPermission(Permission.VIEW_ALL_DISTRIBUTED_FORMS)) {
			return true;
		}
		else if (production.getAllowOnboarding() && ! matchesUser) {
			ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(null, chain, emp.getProject());
			currentContactApproverMap = ApproverUtils.isContactInPath(approvalPath, SessionUtils.getCurrentContact());
			if (currentContactApproverMap.containsKey(null) && currentContactApproverMap.get(null)) {
				return true;
			}
			else if (currentContactApproverMap.size() > 0 && currentContactApproverMap.containsValue(true)) {
				log.debug("");
				for (List<Department> deptList : currentContactApproverMap.keySet()) {
					if (currentContactApproverMap.get(deptList) && deptList.contains(emp.getDepartment())) {
						log.debug("");
						return true;
					}
				}
			}
		}
		else if (matchesUser) {
			log.debug("");
			return true;
		}
		return false;
	}

	/** Method to show starts icon for new employment.
	 * @param emp
	 * @return boolean describing whether to show starts icon or not.
	 */
	public boolean showStartsIcon(Employment emp) {
		if (production.getAllowOnboarding()) {
			Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
			DocumentChain chain = DocumentChainDAO.getInstance().findOneByNamedQuery(
					DocumentChain.GET_START_DOCUMENT_CHAIN_OF_PRODUCTION, map("folderId", onboardFolder.getId()));
			if (isStartApproverForEmployment(emp, chain)) {
				return true;
			}
		}
		else {
			return true;
		}
		return false;
	}

	private void calculateEmpStatus(Employment emp) {
		emp.setStatus(EmpStatus.N_A);
		StartForm startForm = null;
		if (emp.getId() != null) { // skip if transient (newly created & not yet saved)
			startForm = getStartFormDAO().findLatestForEmployment(emp);
		}
		if (startForm != null) {
			Date startDate = startForm.getHireDate();
			if (startForm.getWorkStartDate() != null) {
				startDate = startForm.getWorkStartDate();
			}
			Date effectiveEndDate = startForm.getEarliestEndDate();
			Date today = new Date();

			if (startDate != null && startDate.after(today)) {
				emp.setStatus(EmpStatus.NOT_STARTED);
			}
			else if ((effectiveEndDate != null && effectiveEndDate.before(today)) ||
					(emp.getEndDate() != null && emp.getEndDate().before(today))) {
				emp.setStatus(EmpStatus.WRAPPED);
			}
			else {
				emp.setStatus(EmpStatus.STARTED);
			}
		}

		emp.setAllowDelete(true);
		if (emp.getId() != null) { // skip if transient (newly created & not yet saved)
			Long countOfContactDocs = ContactDocumentDAO.getInstance()
					.findCountByNamedQuery(ContactDocument.GET_CONTACT_DOCUMENT_BY_EMPLOYMENT_AND_STATUS, map("employment", emp));
			emp.setAllowDelete(countOfContactDocs <= 0);
		}
	}

	/** Action Ok method invoked when the user tries to edit the occupation
	 * @return
	 */
	/*public String actionChangeEmp() {
		try {
			CreateNewOccupationBean selectBean = CreateNewOccupationBean.getInstance();
			if (! selectBean.actionSave()) {
				return null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}*/

	/**
	 * Action method called after the user clicks the red cross button and we've
	 * done preliminary validation, or we've prompted the user due to the last
	 * "Cast" occupation being deleted. The method only deletes those
	 * employments whose start form's have no timecards attached with them.
	 *
	 * @return null navigation string
	 */
	private String actionDeleteEmploymentOk() {
		if (employmentToDelete != null) {
			log.debug("");

			if (employmentToDelete.getId() == null && getSetToCreate().contains(employmentToDelete)) {
				// Not yet persisted (added this edit cycle); just remove it & related items from 'setToCreate'
				for (ProjectMember pm : employmentToDelete.getProjectMembers()) {
					getSetToCreate().remove(pm);
				}
				employmentToDelete.getProjectMembers().clear();
				StartForm sf = null;
				for (Object curObject : getSetToCreate()) {
					// See if there's a related StartForm that needs to be removed
					if (curObject instanceof StartForm) {
						if (((StartForm)curObject).getEmployment() == employmentToDelete) {
							// Note: using instance identity, not equality!
							sf = (StartForm)curObject;
							break;
						}
					}
				}
				if (sf != null) {
					getSetToCreate().remove(sf);
					contact.getStartForms().remove(sf);
				}
				getSetToCreate().remove(employmentToDelete);
				contact.getEmployments().remove(employmentToDelete);
			}
			else {
				// if employment to be deleted is admin-type, then logged in user must also be similar type...
				if (employmentToDelete.getRole().isDataAdmin() || employmentToDelete.getRole().isFinancialAdmin()) {
					AuthorizationBean bean = AuthorizationBean.getInstance();
					if (! bean.isAdmin()) { // if non-LS Admin user ...
						// Data admin and Financial admin roles can only be deleted by
						// a user with similar capability.
						if ((employmentToDelete.getRole().isDataAdmin() && ! bean.isDataAdmin()) ||
								(employmentToDelete.getRole().isFinancialAdmin() && ! bean.hasPermission(Permission.EDIT_FINANCE_PERMISSIONS))) {
							// entry to delete is Prod Data admin & user is not; or one to delete is Financial data admin & user is not...
							MsgUtils.addFacesMessage("Contact.NoAuthority", FacesMessage.SEVERITY_ERROR);
							return null;
						}
					}
					deleteEmployment(null);
				}
				else { // normal employment to be deleted, delete if no documents associated with employment
					if (getProduction().getAllowOnboarding()) {
						boolean hasDocs = ContactDocumentDAO.getInstance().existsProperty("employment", employmentToDelete);
						if (hasDocs) {
							PopupBean.getInstance().show("ContactViewBean.DeleteEmployment.");
							return null;
						}
						else {
							deleteEmployment(null);
						}
					}
					else { // for non-OnBoarding, check if any Starts have timecards
						List<StartForm> starts = getStartFormDAO().findByProperty("employment", employmentToDelete);
						boolean hasTc = false;
						for (StartForm sf : starts) {
							if (WeeklyTimecardDAO.getInstance().existsTimecardsForStartForm(sf.getId())) {
								hasTc = true;
								break;
							}
						}
						if (hasTc) {
							log.debug("has start form with timecards");
							MsgUtils.addFacesMessage("StartForm.HasTimecards", FacesMessage.SEVERITY_ERROR);
							return null;
						}
						else {
							deleteEmployment(starts);
						}
					}
				}
			}
		}
		employmentToDelete = null; // free reference
		if (contact.getEmployments().size() == 1) {
			contact.getEmployments().get(0).setDefRole(true);
			contact.getEmployments().get(0).setDisableEmpDefRole(true);
		}
		setContactEmploymentList(null);
		return null;
	}

	/**
	 * Method deletes the associated project member and employment instances
	 *
	 * @param starts List of StartForm`s to be deleted along with the Employment
	 *            record.
	 */
	private void deleteEmployment(List<StartForm> starts) {
		employmentToDelete = EmploymentDAO.getInstance().refresh(employmentToDelete);
		List<ProjectMember> list = ProjectMemberDAO.getInstance().findByProperty("employment", employmentToDelete);
		for (ProjectMember pm : list) {
			//getProjectMemberDAO().delete(pm);
			setToDelete.add(pm);
		}
		contact.getEmployments().remove(employmentToDelete);
		/*if (employmentToDelete.getDefRole()) {
			contact.getEmployments().get(0).setDefRole(true);
		}*/
		//EmploymentDAO.getInstance().delete(employmentToDelete);
		setToDelete.add(employmentToDelete);
		if (starts != null) {
			setToDelete.addAll(starts);
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event [primary column]
	 * @param evt
	 */
	public void listenPrimarySingleCheck (ValueChangeEvent evt) {
		Employment employment = (Employment) evt.getComponent().getAttributes().get("selectedRow");
		if (employment.getDefRole()) {
			for (Employment emp : contactEmploymentList) {
				if (employment.getProject() == null) {
					if (emp.getProject() == null) {
						log.debug("emp : " + emp.getOccupation() + " employment : " + employment.getOccupation());
						emp.setDefRole(false);
					}
				}
				else if (emp.getProject() != null) {
					if (SessionUtils.getProduction().getType().isAicp()) {
						if (emp.getProject().equals(employment.getProject())) {
							log.debug("emp : " + emp.getOccupation() + "employment : " + employment.getOccupation());
							emp.setDefRole(false);
						}
					}
					else {
						if (! emp.equals(employment)) {
							log.debug("emp : " + emp.getOccupation() + "employment : " + employment.getOccupation());
							emp.setDefRole(false);
						}
					}
				}
				if (emp.equals(employment)) {
					emp.setDefRole(true);
				}
			}
			contact.setDepartment(employment.getDepartment());
			contact.setRole(employment.getRole());
			contact.setRoleName(employment.getRole().getName());
			updateContactInList(contact);
		}
	}

	/** Value change listener for Primary Phone radio buttons.
	 * @param evt
	 */
	public void listenPhoneChange(ValueChangeEvent evt) {
		try {
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean) evt.getNewValue();
			log.debug("Id = " + id);
			log.debug("Value = " + newVal);
			if (id.equals("officePhone") && newVal) {
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
				primaryPhone[0] = true;
				primaryPhone[1] = false;
				primaryPhone[2] = false;
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
			}
			else if (id.equals("cellPhone") && newVal) {
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
				primaryPhone[0] = false;
				primaryPhone[1] = true;
				primaryPhone[2] = false;
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
			}
			else if (id.equals("homePhone") && newVal) {
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
				primaryPhone[0] = false;
				primaryPhone[1] = false;
				primaryPhone[2] = true;
				log.debug("Ofc Phone = " + primaryPhone[0]);
				log.debug("Cell Phone = " + primaryPhone[1]);
				log.debug("Home Phone = " + primaryPhone[2]);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}


	/* Code for condition: Check on "All project" employment removes checks from individual projects.
	 public void listenPrimarySingleCheck (ValueChangeEvent evt) {
		Employment employment = (Employment) evt.getComponent().getAttributes().get("selectedRow");
		if (employment.getDefRole()) {
			for (Employment emp : contactEmploymentList) {
				if (SessionUtils.getProduction().getType().isAicp() && employment.getProject() != null) {
					if (emp.getProject() == null || (emp.getProject() != null && emp.getProject().equals(employment.getProject()))) {
						log.debug("emp id : " + emp.getId() + "employment id : " + employment.getId());
						emp.setDefRole(false);
					}
				}
				else {
					if (! emp.equals(employment)) {
						log.debug("emp id : " + emp.getId() + "employment id : " + employment.getId());
						emp.setDefRole(false);
					}
				}
				if (emp.equals(employment)) {
					emp.setDefRole(true);
				}
			}
			contact.setDepartment(employment.getDepartment());
			contact.setRole(employment.getRole());
			contact.setRoleName(employment.getRole().getName());
			updateContactInList(contact);
		}
	}*/

	/**
	 * Return the id of the Contact that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 */
	private Integer getRowId(int row) {
		Integer id = null;
		List<Contact> list = getContactList();
		if (list == null || list.size() == 0) {
			return null;
		}
		if (row < 0) {
			row = 0;
		}
		if (row >= list.size()) {
			row = list.size() - 1;
		}
		setSelectedRow(row);
		id = list.get(row).getId();
		return id;
	}

	@Override
	protected Comparator<Contact> getComparator() {
		Comparator<Contact> comparator = new Comparator<Contact>() {
			@Override
			public int compare(Contact c1, Contact c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/** Method to return departmentId list for current contact, departments which cabe accessed by the current contact. */
	public List<Integer> getDeptIdListForCurrContact() {
		List<Integer> deptList = new ArrayList<>();
		for (SelectItem dept : getDepartmentDL()) {
			deptList.add((Integer) dept.getValue());
		}
		return deptList;
	}

//	/** See {@link #startFormBean}. */
//	public StartFormBean getStartFormBean() {
//		// accessing start form bean - make sure our Contact object is available & current
//		SessionUtils.put(ATTR_CONTACTVIEW_CONTACT, contact);
//		if (startFormBean == null) {
//			startFormBean = (StartFormBean)ServiceFinder.findBean("startFormBean");
//		}
//		return startFormBean;
//	}

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #matchesUser}.  Used in JSP. */
	public boolean getMatchesUser() {
		return matchesUser;
	}

	/**
	 * @return the currently selected/displayed item - to match similar beans
	 */
	public Contact getElement() { // to simplify JSP
		return contact;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return contact.getUser().getDisplayName();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List getItemList() {
		return getContactList();
	}

	public List<Contact> getContactList() {
		if (showProject) {
			if (projectContacts == null) {
				createProjectContacts();
			}
		}
		else if (allContacts == null) {
			createAllContacts();
		}
		sortIfNeeded(); // only sort if the sort column or ordering has changed.
		if (showProject) {
			return projectContacts;
		}
		return allContacts;
	}

	public void setContactList(List<Contact> contactList) {
	}

	public List<SelectItem> getContactDL() {
		if (contactDL == null) {
			createContactDL();
		}
		return contactDL;
	}

	public void setContactDL(List<SelectItem> contactDL) {
		this.contactDL = contactDL;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/** See {@link #showAddContact}. */
	public boolean getShowAddContact() {
		return showAddContact;
	}
	/** See {@link #showAddContact}. */
	public void setShowAddContact(boolean showAddContact) {
		this.showAddContact = showAddContact;
	}

	/** See {@link #showAddOccupation}. */
	public boolean getShowAddOccupation() {
		return showAddOccupation;
	}
	/** See {@link #showAddOccupation}. */
	public void setShowAddOccupation(boolean showAddOccupation) {
		this.showAddOccupation = showAddOccupation;
	}

	/** See {@link #addPopupTitle}. */
	public String getAddPopupTitle() {
		return addPopupTitle;
	}
	/** See {@link #addPopupTitle}. */
	public void setAddPopupTitle(String addPopupTitle) {
		this.addPopupTitle = addPopupTitle;
	}

	/** See {@link #loginEnabled}. */
	public boolean getLoginEnabled() {
		loginEnabled = false;
		if (contact != null) {
			if (contact.getActive()) {
				if (contact.getUser() != null) {
					loginEnabled = contact.getLoginAllowed() &&
						(! contact.getUser().getLockedOut());
				}
			}
		}
		return loginEnabled;
	}

	/** See {@link #loginEnabled}. */
	public void setLoginEnabled(boolean b) {
		if (b != loginEnabled) {
			if (contact != null && contact.getUser() != null) {
				if (b) {
					if (contact.getStatus() == MemberStatus.NO_ACCESS) {
						contact.setStatus(MemberStatus.ACCEPTED);
						loginEnabled = true;
					}
					contact.getUser().setLockedOut(false);
					contact.setLoginAllowed(true);
				}
				else {
					contact.setStatus(MemberStatus.NO_ACCESS);
					contact.setLoginAllowed(false);
				}
			}
			loginEnabled = b;
		}
	}

	public boolean getSendAdvanceScript() {
		return sendAdvanceScript;
	}
	public void setSendAdvanceScript(boolean sendAdvanceScript) {
		this.sendAdvanceScript = sendAdvanceScript;
	}

//	/** See {@link #showAddRole}. */
//	public boolean getShowAddRole() {
//		return showAddRole;
//	}
//	/** See {@link #showAddRole}. */
//	public void setShowAddRole(boolean showAddRole) {
//		this.showAddRole = showAddRole;
//	}

	/** See {@link #newRoleId}. */
	public Integer getNewRoleId() {
		return newRoleId;
	}
	/** See {@link #newRoleId}. */
	public void setNewRoleId(Integer newRoleId) {
		this.newRoleId = newRoleId;
	}

//	/** See {@link #removePmId}. */
//	public Integer getRemovePmId() {
//		return removePmId;
//	}
//	/** See {@link #removePmId}. */
//	public void setRemovePmId(Integer id) {
//		removePmId = id;
//	}

	/** See {@link #roleMap}. */
	/*public Map<Integer, List<SelectItem>> getRoleMap() {
		if (roleMap == null) {
			roleMap = createRoleMap();
		}
		return roleMap;
	}
	*//** See {@link #roleMap}. *//*
	public void setRoleMap(Map<Integer, List<SelectItem>> roleMap) {
		this.roleMap = roleMap;
	}*/

//	/** See {@link #newMember}. */
//	public ProjectMember getNewMember() {
//		return newMember;
//	}
//	/** See {@link #newMember}. */
//	public void setNewMember(ProjectMember newMember) {
//		this.newMember = newMember;
//	}

	public List<SelectItem> getStatusDL() {
		return statusDL;
	}

	/** See {@link #phoneItems}. */
	public SelectItem[] getPhoneItems() {
		return phoneItems;
	}

	public List<SelectItem> getFileAccessTypeDL() {
		return EnumList.getFileAccessTypeList();
	}

	public List<SelectItem> getImServiceTypeDL() {
		return EnumList.getImServiceTypeList();
	}

//	public User getNewUser() {
//		User user = getContact().getUser();
//		return user;
//	}

	public List<SelectItem> getDepartmentDL() {
		if (departmentDL == null) {
			// get the full department list - without filtering by "active" departments
			departmentDL = DepartmentUtils.getDepartmentDL(getvContact(), true, false);
		}
		return departmentDL;
	}

//	/** See {@link #validDept}. */
//	public Map<Integer, Boolean> getValidDept() {
//		if (validDept == null) {
//			Project project = null;
//			if (SessionUtils.getProduction().getType().hasPayrollByProject()) {
//				project = SessionUtils.getCurrentProject();
//			}
//
//			validDept = DepartmentUtils.getDepartmentIds(getvContact(), project);
//		}
//		return validDept;
//	}
//	/** See {@link #validDept}. */
//	public void setValidDept(Map<Integer, Boolean> validDept) {
//		this.validDept = validDept;
//	}

	/** See {@link #editableUnits}. */
	public Map<Integer, Boolean> getEditableUnits() {
		if (editableUnits == null) {
			editableUnits = createEditableUnits();
		}
		return editableUnits;
	}
	/** See {@link #editableUnits}. */
	public void setEditableUnits(Map<Integer, Boolean> editableProjects) {
		editableUnits = editableProjects;
	}

	/** See {@link #editableProjectDL}. */
	public List<SelectItem> getEditableProjectDL() {
		if (editableProjectDL == null) {
			editableProjectDL = createEditableProjectDL();
		}
		return editableProjectDL;
	}
	/** See {@link #editableProjectDL}. */
	public void setEditableProjectDL(List<SelectItem> editableProjectDL) {
		this.editableProjectDL = editableProjectDL;
	}

	/** See {@link #unitDLmap}. */
	public Map<Integer, List<SelectItem>> getUnitDLmap() {
		if (unitDLmap == null) {
			createEditableProjectDL();
		}
		return unitDLmap;
	}
	/** See {@link #unitDLmap}. */
	public void setUnitDLmap(Map<Integer, List<SelectItem>> unitDLmap) {
		this.unitDLmap = unitDLmap;
	}

	public Integer getDepartmentId() {
		return departmentId;
	}
	public void setDepartmentId(Integer id) {
		departmentId = id;
	}

//	/** See {@link #showAsst}. */
//	public boolean isShowAsst() {
//		return showAsst;
//	}
//	/** See {@link #showAsst}. */
//	public void setShowAsst(boolean showAsst) {
//		this.showAsst = showAsst;
//	}

	/** See {@link #assistantId}. */
	public Integer getAssistantId() {
		return assistantId;
	}
	/** See {@link #assistantId}. */
	public void setAssistantId(Integer assistantId) {
		this.assistantId = assistantId;
	}

	public boolean getShowProject() {
		return showProject;
	}
	public void setShowProject(boolean showProject) {
		this.showProject = showProject;
	}

	/**
	 * For the mobile contact list page, scroll up by decrementing the row number of
	 * the first contact to display from the list.  The row
	 * number is zero-origin.
	 * @param evt - ActionEvent, not used.
	 */
	public void actionPrevious(ActionEvent evt) {
		firstRow -= NUMBER_OF_ROWS;
		if (firstRow < 0) {
			firstRow = 0;
		}
	}

	/**
	 * For the mobile contact list page, scroll down by incrementing the row number of
	 * the first contact to display from the list.  The row
	 * number is zero-origin.
	 * @param evt - ActionEvent, not used.
	 */
	public void actionNext(ActionEvent evt) {
		if ( (firstRow + NUMBER_OF_ROWS) < getContactList().size() ) {
			firstRow += NUMBER_OF_ROWS;
		}
		if (firstRow >= getContactList().size()) {
			firstRow = getContactList().size() - 1;
		}
	}

	/** Action invoked when the user clicks the Create button on the Cast n Crew/Contacts mini-tab in the edit mode.
	 * Also used to open up the pop up to allow user to create its own occupation
	 * @return
	 */
	public String actionCreateNewOccupation() {
//		showAddRole = true;
		//currentEmployment = EmploymentDAO.getInstance().findOneByProperty("contact", contact);
		showAddOccupation = true;
		currentEmployment = findDefaultEmployment();
		CreateNewOccupationBean occBean = CreateNewOccupationBean.getInstance();
		occBean.setProjectId(SessionUtils.getCurrentProject().getId());
		occBean.setProject(SessionUtils.getCurrentProject());
		occBean.setCreateNewRole(false);
		occBean.setShowRoleName(false);
		occBean.setRoleMasks(roleMasks);
		Employment emp;
		if (currentEmployment != null) {
			log.debug("");
//			StartForm startForm = getStartFormDAO().findOneByProperty("employment", currentEmployment);
//			if (startForm != null) {
//				occBean.setStartDate(startForm.getEffectiveOrStartDate());
//			}
			if (occBean.getStartDate() == null) {
				occBean.setStartDate(CalendarUtils.todaysDate());
			}
			occBean.setEndDate(currentEmployment.getEndDate());
			occBean.setDepartment(currentEmployment.getDepartment());
//			occBean.setDepartmentId(currentEmployment.getDepartment().getId());
			occBean.setRole(currentEmployment.getRole());
			occBean.setEmploymentRole(currentEmployment.getRole());
			currentEmployment.setContact(contact);
			log.debug("Existing role = " + currentEmployment.getRole());
			// CreateNewOccupation bean shouldn't be changing our current one; pass a temp...
			emp = currentEmployment.clone();
		}
		else {
			emp = new Employment(null, null, contact);
		}
		occBean.show(this, emp, ACT_CREATE_OCCUPATION, "CreateNewOccupationBean.NewOccupation.", true);
		return null;
	}

	/**
	 * @return The default Employment instance for the currently selected
	 *         Contact, matching (if possible) the current Project.
	 */
	private Employment findDefaultEmployment() {
		Integer projectId = SessionUtils.getCurrentProject().getId();
		Employment defEmp = null;
		if (contactEmploymentList != null && ! contactEmploymentList.isEmpty()) {
			defEmp = contactEmploymentList.get(0);
		}
		for (Employment emp : contactEmploymentList) {
			if (emp.getProject() != null && emp.getDefRole()) {
				if (emp.getProject().getId().equals(projectId)) {
					defEmp = emp;
				}
			}
		}
		return defEmp;
	}

	public int getFirstRow() {
		return firstRow;
	}
	public void setFirstRow(int firstRow) {
		this.firstRow = firstRow;
	}

	public int getNumberOfRows() {
		return NUMBER_OF_ROWS;
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	private ProjectMemberDAO getProjectMemberDAO() {
		if (projectMemberDAO == null) {
			projectMemberDAO = ProjectMemberDAO.getInstance();
		}
		return projectMemberDAO;
	}

	/** See {@link #startFormDAO}. */
	public StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}

	/** See {@link #contactEmploymentList}. */
	public List<Employment> getContactEmploymentList() {
		if (contactEmploymentList == null) {
			createEmploymentList();
		}
		return contactEmploymentList;
	}
	/** See {@link #contactEmploymentList}. */
	public void setContactEmploymentList(List<Employment> contactEmploymentList) {
		this.contactEmploymentList = contactEmploymentList;
	}

	/** See {@link #unitDL}. */
	public List<SelectItem> getUnitDL() {
		if (unitDL == null) {
			unitDL = ScheduleUtils.createUnitList(SessionUtils.getCurrentProject());
		}
		return unitDL;
	}
	/** See {@link #unitDL}. */
	public void setUnitDL(List<SelectItem> unitDL) {
		this.unitDL = unitDL;
	}

	/** See {@link #currentEmployment}. */
	public Employment getCurrentEmployment() {
		return currentEmployment;
	}
	/** See {@link #currentEmployment}. */
	public void setCurrentEmployment(Employment currentEmployment) {
		this.currentEmployment = currentEmployment;
	}

	/** See {@link #employmentToDelete}. */
	public Employment getEmploymentToDelete() {
		return employmentToDelete;
	}
	/** See {@link #employmentToDelete}. */
	public void setEmploymentToDelete(Employment employmentToDelete) {
		this.employmentToDelete = employmentToDelete;
	}

	/** See {@link #setToCreate}. */
	public Set<Object> getSetToCreate() {
		return setToCreate;
	}
	/** See {@link #setToCreate}. */
	public void setSetToCreate(Set<Object> setToCreate) {
		this.setToCreate = setToCreate;
	}

	/** See {@link #setToDelete}. */
	public Set<Object> getSetToDelete() {
		return setToDelete;
	}
	/** See {@link #setToDelete}. */
	public void setSetToDelete(Set<Object> setToDelete) {
		this.setToDelete = setToDelete;
	}

	/** See {@link #setToAttach}. */
	public Set<Object> getSetToAttach() {
		return setToAttach;
	}
	/** See {@link #setToAttach}. */
	public void setSetToAttach(Set<Object> setToAttach) {
		this.setToAttach = setToAttach;
	}

	/** See {@link #isStartApprover}. */
	/*public boolean getIsStartApprover() {
		return isStartApprover;
	}
	*//** See {@link #isStartApprover}. *//*
	public void setStartApprover(boolean isStartApprover) {
		this.isStartApprover = isStartApprover;
	}*/

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**See {@link #primaryPhone}. */
	public Boolean[] getPrimaryPhone() {
		return primaryPhone;
	}
	/**See {@link #primaryPhone}. */
	public void setPrimaryPhone(Boolean[] primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	/** Returns true if the Email Address for the selected contact
	 * is a Fake Email address else, returns false.
	 */
	public boolean getIsFakeEmail() {
		if (contact != null) {
			if (EmailValidator.isFakeEmail(contact.getEmailAddress())) {
				return true;
			}
		}
		return false;
	}

	/** See {@link #aprovalPathsBean}. */
	public ApprovalPathsBean getApprovalPathsBean() {
		if (approvalPathsBean == null) {
			approvalPathsBean = ApprovalPathsBean.getInstance();
		}
		return approvalPathsBean;
	}

	/** See {@link #projectTitle}. */
	public String getProjectTitle() {
		return projectTitle;
	}

	/** See {@link #projectTitle}. */
	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	/** See {@link #projectsTitle}. */
	public String getProjectsTitle() {
		return projectsTitle;
	}

	/** See {@link #projectsTitle}. */
	public void setProjectsTitle(String projectsTitle) {
		this.projectsTitle = projectsTitle;
	}

	/** See {@link #productionLbl}. */
	public String getProductionLbl() {
		return productionLbl;
	}

	/** See {@link #productionLbl}. */
	public void setProductionLbl(String productionLbl) {
		this.productionLbl = productionLbl;
	}

}
