//	File Name:	Contact.java
package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.icefaces.ace.util.IceOutputResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.object.ImageResource;
import com.lightspeedeps.type.FileAccessType;
import com.lightspeedeps.type.MemberStatus;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Contact entity.  This represents the information relating to a particular
 * User within a particular Production.
 */
@NamedQueries ({
	@NamedQuery(name=Contact.GET_CONTACT_LIST_BY_DEPARTMENT_ID_AND_PRODUCTION, query = "select distinct e.contact from Employment e where e.role.department.id =:departmentId and e.contact.production.id =:productionId"),
	@NamedQuery(name=Contact.GET_CONTACT_LIST_BY_PRODUCTION_PROJECT, query = "select distinct e.contact from Employment e where e.project =:project and e.contact.production =:production"),
	@NamedQuery(name=Contact.GET_CONTACT_LIST_BY_PRODUCTION_PROJECT_DEPARTMENT, query = "select distinct e.contact from Employment e where e.contact.project =:project and e.role.department.id =:departmentId"),
	@NamedQuery(name=Contact.GET_CONTACT_LIST_BY_PRODUCTION_NOT_ADMIN, query = "select distinct e.contact from Employment e, ProjectMember pm where e.contact.production =:production and pm.employment = e and pm.unit is not null"),
	@NamedQuery(name=Contact.GET_NON_DELETED_CONTACT_BY_USER_WITH_LOGIN_ALLOWED, query = "from Contact c where c.status <> 'DELETED' and c.production.status <> 'DELETED' and c.loginAllowed = true and c.user =:user"),
	@NamedQuery(name=Contact.GET_NON_DELETED_CONTACT_BY_ONBOARD_PRODUCTION, query = "from Contact c where c.status <> 'DELETED' and c.production.status <> 'DELETED' and c.production.allowOnboarding = true and c.user =:user"),
})
@Entity
@Table(name = "contact")
public class Contact extends PersistentObject<Contact> implements Cloneable, Comparable<Contact> {
	private static final Log log = LogFactory.getLog(Contact.class);

	//Named queries
	public static final String  GET_CONTACT_LIST_BY_DEPARTMENT_ID_AND_PRODUCTION = "getContactListByDepartIdAndProduction";
	public static final String  GET_CONTACT_LIST_BY_PRODUCTION_PROJECT = "getContactListByProductionProject";
	public static final String  GET_CONTACT_LIST_BY_PRODUCTION_PROJECT_DEPARTMENT = "getContactListByProductionProjectDepartment";
	public static final String  GET_CONTACT_LIST_BY_PRODUCTION_NOT_ADMIN = "getContactListByProduction";
	public static final String  GET_NON_DELETED_CONTACT_BY_USER_WITH_LOGIN_ALLOWED = "getNonDeletedContactByUserStatusNotNoAccess";

	//TODO DH: DO we to add the 'NO_ACCESS' member status check in this query also?
	public static final String  GET_NON_DELETED_CONTACT_BY_ONBOARD_PRODUCTION = "getNonDeletedContactByOnboardProduction";

	// Fields
	private static final long serialVersionUID = 8162805199106630187L;

	public static final String SORTKEY_NAME = "name";
	public static final String SORTKEY_DEPT = "department";
	public static final String SORTKEY_ROLE = "role";
	public static final String SORTKEY_STATUS = "status";

	/** The User instance this Contact belongs to. */
	private User user;

	/** Allows direct access to the User.id field.
	 * @see #user */
	private Integer userId;

	/** The Production associated with this Contact. */
	private Production production;
	private MemberStatus status;
	/** Whether or not the user may click on the production name to "enter" into the production.
	 * If this is true, they may; if false, they may not, but can still access their timecards and
	 * start forms via the "My Timecards" and "My Starts" tabs. */
	private Boolean loginAllowed = false;

	/** The Permission`s, specified as a 64-bit mask, assigned to this Contact
	 * which extend across all Project`s.  These permissions are only set via customization
	 * or by adding this Contact to the timecard approval hierarchy.  In many cases,
	 * this mask will be zero. */
	private long permissionMask;

	/** The Project to be active when this person enters the Production. */
	private Project project;

	/** The default department to display on the Contact (Cast&Crew) list. */
	@Transient
	private Department department;
//	private Contact standIn;

	private String emailAddress;
	private Address homeAddress;
	private String businessPhone;
	private String cellPhone;
	private String homePhone;
	/** Which phone is "primary" - 0:office, 1:cell, 2:home */
	private Integer primaryPhoneIndex = 0;

	private Role role;
	private Contact assistant;
	private String pseudonym;
	private Boolean minor = false;
	private String displayName;

	/**
	 * Set to true for Contacts with high privacy requirements. By default this is false; if true,
	 * some Roles that can view Contact info will not see "hidden" fields for this Contact, such as
	 * home address and home telephone.
	 */
	private Boolean hidden = false;
	private Boolean contactAsstFirst = false;
	private Boolean notifyByEmail = true;
	private Boolean notifyByTextMsg = false;
	private Boolean notifyByAsstEmail = false;
	private Boolean notifyByAsstTextMsg = false;
	/** If true, send Call Sheet changes to this user. */
	private Boolean notifyForAlerts = true;
	/** If true, send Schedule changes to this user.  This includes starting date, or day
	 * 'type' changing (e.g., from Off to Work). */
	private Boolean notifyForCalendarChanges = true;
	/** This flag controls both initial and overdue task reminders. */
	private Boolean notifyForNewTask = false;
	/** Currently unused; 'notifyForNewTask' is used for both due & overdue notifications. */
//	private Boolean notifyForOverdueTask = false;

	/** if true, email notification when timecard is moved to approver's queue. */
	private boolean notifyForApproval;

	/** if true, email notification when an I-9 is pending for approval. */
	private boolean notifyForPendingI9;

	/** If true, send the full Call Sheet to this user when it is Published. */
	private Boolean sendCallsheet = true;
	/** Currently unused; meant to allow a choice of long or short styles of callsheet
	 * to be delivered to the contact. */
//	private final String callsheetVersion = CallSheetVersion.BRIEF.name();
	/** If true, send the full DPR to this user when it is Published; currently unused. */
	private Boolean sendDpr = false;
	/** If true, send Location directions when a Callsheet is published (finalized). */
	private Boolean sendDirections = true;
	/** If true, send the stripboard when it is published; currently unused. */
	private Boolean sendStripboard = false;
	/** If true, send the pages of script to be shot 'n' days in advance. */
	private Integer sendAdvanceScript;
	/** Currently unused. */
//	private final Boolean scriptFullPage = true;
	/** Currently unused. */
//	private final Boolean scriptDialogueOnly = false;
	/** File repository access allowed for this contact. */
	private String fileAccess = FileAccessType.DEFAULT.name();
	/** The production element (RealWorldElement) representing the 'actor' that
	 * corresponds to this Contact. */
	private RealWorldElement castMember;

	/** The account number of the user who created (invited) this Contact. */
	private String createdBy;

	/**Number of document attached with the Contact. */
//	private Long docCount;

	/**To get Contact Document Dao instance*/
//	private transient ContactDocumentDAO contactDocDao;


//	private Set<DateEvent> dateEvents = new HashSet<DateEvent>(0);
//	private Set<Script> scripts = new HashSet<Script>(0);
//	private Set<Contact> assistantFor = new HashSet<Contact>(0);

	private List<Employment> projectMembers = new ArrayList<>(0);

	private List<StartForm> startForms = new ArrayList<>(0);

	/** The scriptElements for which this Contact is the "Responsible Party". */
	private Set<ScriptElement> scriptElements = new HashSet<>(0);

	private Set<RealWorldElement> managerFor = new HashSet<>(0);
//	private Set<Contact> standInFor = new HashSet<Contact>(0);
//	private Set<ContractState> contractStates = new HashSet<ContractState>(0);
//	private Set<Contact> talentAgentFor = new HashSet<Contact>(0);
	private Set<ReportRequirement> reportRequirements = new HashSet<>(0);
	private List<MessageInstance> messageInstances = new ArrayList<>(0);
	private List<Image> images = new ArrayList<>(0);

	/** Project-based role name or default role name; used in Contact List. */
	@Transient
	private String roleName;

	/** Used to track row selection on Contact List page. */
	@Transient
	private boolean selected = false;

	/** Transient flag indicating that this contact has at least one role which
	 * is considered a Cast type.  Used in Contact List/View page. */
	@Transient
	private boolean isCast = false;

	/** Transient flag indicating that this contact only has a "non-production"
	 * role; this typically means in the Vendor department. Used in Contact List/View page.*/
	@Transient
	private boolean isNonProd = false;

	/** The Set of Permission values matching the permissionMask value. */
	@Transient
	private Set<Permission> permissions;

	@Transient
	private boolean checked;

	/** List of Image resource generated from the list of images Required for Iceface 4.x upgrade.*/
	@Transient
	private List<IceOutputResource> imageResources;
	//private Set<ApprovalPath> approvalPaths;


	// Constructors

	/** default constructor */
	public Contact() {
	}

	public Contact(User user, String emailAddress) {
		this.user = user;
		this.emailAddress = emailAddress;
		status = MemberStatus.PENDING;
	}

	// Property accessors
	@Override
	public void setId(Integer id) {
		if (id != null && id == 0) {
			// force to null; some version of JSF or EL evaluation is making "" into 0 from menu selections.
			Exception ex = new IllegalArgumentException("id=" + this.id + ", new id=" + id + " ** CHANGING TO NULL **");
			log.error(ex);
			id = null;
		}
		this.id = id;
	}

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #status}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public MemberStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	/**
	 * @return True iff the Contact is an active member of the Production,
	 * which means it is either in PENDING or ACCEPTED status.
	 */
	@Transient
	public boolean getActive() {
		return getStatus().isActive();
	}

	/** See {@link #loginAllowed}. */
	@Column(name = "Login_Allowed", nullable = false)
	public Boolean getLoginAllowed() {
		return loginAllowed;
	}
	/** See {@link #loginAllowed}. */
	public void setLoginAllowed(Boolean loginAllowed) {
		this.loginAllowed = loginAllowed;
	}

	@Column(name = "Permission_Mask")
	public long getPermissionMask() {
		return permissionMask;
	}
	public void setPermissionMask(long permissionMask) {
		this.permissionMask = permissionMask;
	}

	/** See {@link #permissions}. */
	@Transient
	public Set<Permission> getPermissions() {
		if (permissions == null) {
			permissions = Permission.createPermissionSet(getPermissionMask());
		}
		return permissions;
	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Default_Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Department_Id")
	/** See {@link #department}. */
	@Transient
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department department) {
		this.department = department;
	}

	@Column(name = "Minor", nullable = false)
	public Boolean getMinor() {
		return minor;
	}
	public void setMinor(Boolean minor) {
		this.minor = minor;
	}

	@Column(name = "Display_Name", length = 50)
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/** See {@link #emailAddress}. */
	@Column(name = "Email_Address", length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	/** See {@link #emailAddress}. */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/** See {@link #homeAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Home_Address_Id")
	public Address getHomeAddress() {
		return homeAddress;
	}
	/** See {@link #homeAddress}. */
	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

	/** See {@link #businessPhone}. */
	@Column(name = "Business_Phone", length = 25)
	public String getBusinessPhone() {
		return businessPhone;
	}
	/** See {@link #businessPhone}. */
	public void setBusinessPhone(String businessPhone) {
		this.businessPhone = businessPhone;
	}

	/** See {@link #cellPhone}. */
	@Column(name = "Cell_Phone", length = 25)
	public String getCellPhone() {
		return cellPhone;
	}
	/** See {@link #cellPhone}. */
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}

	/** See {@link #homePhone}. */
	@Column(name = "Home_Phone", length = 25)
	public String getHomePhone() {
		return homePhone;
	}
	/** See {@link #homePhone}. */
	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	/** See {@link #primaryPhoneIndex}. */
	@Column(name = "Primary_Phone_Index", nullable = false)
	public Integer getPrimaryPhoneIndex() {
		return primaryPhoneIndex;
	}
	/** See {@link #primaryPhoneIndex}. */
	public void setPrimaryPhoneIndex(Integer index) {
		// Because the Contact page does not always render all options for the phone index
		// values (due to permissions or "hidden" contact info), this method may be
		// called with a null value; just ignore it.
		if (index != null) {
			primaryPhoneIndex = index;
		}
	}

	@Transient
	public String getPrimaryPhone() {
		switch(getPrimaryPhoneIndex()) {
			case 0:
				return getBusinessPhone();
			case 1:
				return getCellPhone();
			case 2:
				return getHomePhone();
			default:
				return null;
		}
	}

	//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Standin_Id")
//	public Contact getStandIn() {
//		return this.standIn;
//	}
//	public void setStandIn(Contact standIn) {
//		this.standIn = standIn;
//	}

	/*	Not used
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Writer_Id")
	public Script getScript() {
		return this.script;
	}
	public void setScript(Script script) {
		this.script = script;
	}
	 */

	/** See {@link #role}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Role_Id")
	public Role getRole() {
		//log.debug("id="+id);
		return role;
	}
	/** See {@link #role}. */
	public void setRole(Role role) {
		this.role = role;
		if (role != null) {
			roleName = role.getName();
		}
	}

	/** See {@link #assistant}. */
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "Assistant_Id")
	public Contact getAssistant() {
		return assistant;
	}
	/** See {@link #assistant}. */
	public void setAssistant(Contact assistant) {
		this.assistant = assistant;
	}

	/** See {@link #pseudonym}. */
	@Column(name = "Pseudonym", length = 50)
	public String getPseudonym() {
		return pseudonym;
	}
	/** See {@link #pseudonym}. */
	public void setPseudonym(String pseudonym) {
		this.pseudonym = pseudonym;
	}

	/** See {@link #hidden}. */
	@Column(name = "hidden", nullable = false)
	public Boolean getHidden() {
		return hidden;
	}
	/** See {@link #hidden}. */
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	/** See {@link #contactAsstFirst}. */
	@Column(name = "Contact_Asst_First", nullable = false)
	public Boolean getContactAsstFirst() {
		return contactAsstFirst;
	}
	/** See {@link #contactAsstFirst}. */
	public void setContactAsstFirst(Boolean contactAsstFirst) {
		this.contactAsstFirst = contactAsstFirst;
	}

	/** See {@link #notifyByEmail}. */
	@Column(name = "Notify_By_Email", nullable = false)
	public Boolean getNotifyByEmail() {
		return notifyByEmail;
	}
	/** See {@link #notifyByEmail}. */
	public void setNotifyByEmail(Boolean notifyByEmail) {
		this.notifyByEmail = notifyByEmail;
	}

	/** See {@link #notifyByTextMsg}. */
	@Column(name = "Notify_By_Text_Msg", nullable = false)
	public Boolean getNotifyByTextMsg() {
		return notifyByTextMsg;
	}
	/** See {@link #notifyByTextMsg}. */
	public void setNotifyByTextMsg(Boolean notifyByTextMsg) {
		this.notifyByTextMsg = notifyByTextMsg;
	}

	/** See {@link #notifyByAsstEmail}. */
	@Column(name = "Notify_By_Asst_Email", nullable = false)
	public Boolean getNotifyByAsstEmail() {
		return notifyByAsstEmail;
	}
	/** See {@link #notifyByAsstEmail}. */
	public void setNotifyByAsstEmail(Boolean notifyByAsstEmail) {
		this.notifyByAsstEmail = notifyByAsstEmail;
	}

	/** See {@link #notifyByAsstTextMsg}. */
	@Column(name = "Notify_By_Asst_Text_Msg", nullable = false)
	public Boolean getNotifyByAsstTextMsg() {
		return notifyByAsstTextMsg;
	}
	/** See {@link #notifyByAsstTextMsg}. */
	public void setNotifyByAsstTextMsg(Boolean notifyByAsstTextMsg) {
		this.notifyByAsstTextMsg = notifyByAsstTextMsg;
	}

	/** See {@link #notifyForAlerts}. */
	@Column(name = "Notify_For_Alerts", nullable = false)
	public Boolean getNotifyForAlerts() {
		return notifyForAlerts;
	}
	/** See {@link #notifyForAlerts}. */
	public void setNotifyForAlerts(Boolean notifyForAlerts) {
		this.notifyForAlerts = notifyForAlerts;
	}

	/** See {@link #notifyForCalendarChanges}. */
	@Column(name = "Notify_For_Script_Changes", nullable = false)
	public Boolean getNotifyForCalendarChanges() {
		return notifyForCalendarChanges;
	}
	/** See {@link #notifyForCalendarChanges}. */
	public void setNotifyForCalendarChanges(Boolean notifyForScriptChanges) {
		notifyForCalendarChanges = notifyForScriptChanges;
	}

	/** See {@link #notifyForNewTask}. */
	@Column(name = "Notify_For_New_Task", nullable = false)
	public Boolean getNotifyForNewTask() {
		return notifyForNewTask;
	}
	/** See {@link #notifyForNewTask}. */
	public void setNotifyForNewTask(Boolean notifyForNewTask) {
		this.notifyForNewTask = notifyForNewTask;
	}

	/** See {@link #notifyForApproval}. */
	@Column(name = "Notify_For_Overdue_Task", nullable = false)
	public boolean getNotifyForApproval() {
		return notifyForApproval;
	}
	/** See {@link #notifyForApproval}. */
	public void setNotifyForApproval(boolean notifyForApproval) {
		this.notifyForApproval = notifyForApproval;
	}

	/** See {@link #notifyForPendingI9}. */
	@Column(name = "Notify_For_Pending_I9", nullable = false)
	public boolean getNotifyForPendingI9() {
		return notifyForPendingI9;
	}
	/** See {@link #notifyForPendingI9}. */
	public void setNotifyForPendingI9(boolean notifyForPendingI9) {
		this.notifyForPendingI9 = notifyForPendingI9;
	}

	/** See {@link #sendCallsheet}. */
	@Column(name = "Send_Callsheet", nullable = false)
	public Boolean getSendCallsheet() {
		return sendCallsheet;
	}
	/** See {@link #sendCallsheet}. */
	public void setSendCallsheet(Boolean sendCallsheet) {
		this.sendCallsheet = sendCallsheet;
	}

	/**
	 * @deprecated This field is currently unused.
	 * See {@link #callsheetVersion}. */
	/*@Deprecated // currently unused
	@Column(name = "Callsheet_Version", nullable = false, length = 30)
	public String getCallsheetVersion() {
		return callsheetVersion;
	}*/
	/**
	 * @deprecated This field is currently unused.
	 * See {@link #callsheetVersion}. */
	/*@Deprecated // currently unused
	public void setCallsheetVersion(String callsheetVersion) {
		this.callsheetVersion = callsheetVersion;
	}*/

	/** See {@link #sendDpr}. */
	@Column(name = "Send_DPR", nullable = false)
	public Boolean getSendDpr() {
		return sendDpr;
	}
	/** See {@link #sendDpr}. */
	public void setSendDpr(Boolean sendDpr) {
		this.sendDpr = sendDpr;
	}

	/** See {@link #sendDirections}. */
	@Column(name = "Send_Directions", nullable = false)
	public Boolean getSendDirections() {
		return sendDirections;
	}
	/** See {@link #sendDirections}. */
	public void setSendDirections(Boolean sendDirections) {
		this.sendDirections = sendDirections;
	}

	/** See {@link #sendStripboard}. */
	@Column(name = "Send_Stripboard", nullable = false)
	public Boolean getSendStripboard() {
		return sendStripboard;
	}
	/** See {@link #sendStripboard}. */
	public void setSendStripboard(Boolean sendStripboard) {
		this.sendStripboard = sendStripboard;
	}

	/** See {@link #sendAdvanceScript}. */
	@Column(name = "Send_Advance_Script")
	public Integer getSendAdvanceScript() {
		return sendAdvanceScript;
	}
	/** See {@link #sendAdvanceScript}. */
	public void setSendAdvanceScript(Integer sendAdvanceScript) {
		this.sendAdvanceScript = sendAdvanceScript;
	}

	/**
	 * @deprecated This field is currently unused.
	 * See {@link #scriptFullPage}. */
	/*@Deprecated // currently unused
	@Column(name = "Script_Full_Page", nullable = false)
	public Boolean getScriptFullPage() {
		return scriptFullPage;
	}*/
	/**
	 * @deprecated This field is currently unused.
	 * See {@link #scriptFullPage}. */
	/*@Deprecated // currently unused
	public void setScriptFullPage(Boolean scriptFullPage) {
		this.scriptFullPage = scriptFullPage;
	}*/

	/**
	 * @deprecated This field is currently unused.
	 * See {@link #scriptDialogueOnly}. */
	/*@Deprecated // currently unused
	@Column(name = "Script_Dialogue_Only", nullable = false)
	public Boolean getScriptDialogueOnly() {
		return scriptDialogueOnly;
	}*/
	/**
	 * @deprecated This field is currently unused.
	 * See {@link #scriptDialogueOnly}. */
	/*@Deprecated // currently unused
	public void setScriptDialogueOnly(Boolean scriptDialogueOnly) {
		this.scriptDialogueOnly = scriptDialogueOnly;
	}*/

	/** See {@link #fileAccess}. */
	@Column(name = "File_Access", nullable = false, length = 30)
	public String getFileAccess() {
		return fileAccess;
	}
	/** See {@link #fileAccess}. */
	public void setFileAccess(String fileAccess) {
		this.fileAccess = fileAccess;
	}

	/** See {@link #castMember}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "actor")
	public RealWorldElement getCastMember() {
		return castMember;
	}
	/** See {@link #castMember}. */
	public void setCastMember(RealWorldElement castMember) {
		this.castMember = castMember;
	}

	/** See {@link #createdBy}. */
	@Column(name = "Created_By", length = 30)
	public String getCreatedBy() {
		return createdBy;
	}
	/** See {@link #createdBy}. */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
//	public Set<DateEvent> getDateEvents() {
//		return this.dateEvents;
//	}
//	public void setDateEvents(Set<DateEvent> dateEvents) {
//		this.dateEvents = dateEvents;
//	}

//	@OneToMany(fetch = FetchType.LAZY, mappedBy = "assistant")
//	public Set<Contact> getAssistantFor() {
//		return this.assistantFor;
//	}
//	public void setAssistantFor(Set<Contact> assistantFor) {
//		this.assistantFor = assistantFor;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
	@OrderBy("id")
	public List<Employment> getEmployments() {
		return projectMembers;
	}
	public void setEmployments(List<Employment> projectMembers) {
		this.projectMembers = projectMembers;
	}

	/*@Deprecated
	@Transient
	public List<ProjectMember> getProjectMembers() {
		List<ProjectMember> pms = new ArrayList<>();
		for (Employment emp : getEmployments()) {
			pms.addAll(emp.getProjectMembers());
		}
		return pms;
	}*/

	/**
	 * @return True iff this Contact has at least one ProjectMember associated
	 *         with one of its Employment objects.
	 */
	@Transient
	public boolean hasProjectMembers() {
		for (Employment emp : getEmployments()) {
			if (emp.getProjectMembers().size() > 0) {
				return true;
			}
		}
		return false;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
	@OrderBy("id")
	public List<StartForm> getStartForms() {
		return startForms;
	}
	public void setStartForms(List<StartForm> startForms) {
		this.startForms = startForms;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "contact")
	public Set<ScriptElement> getScriptElements() {
		return scriptElements;
	}
	public void setScriptElements(Set<ScriptElement> scriptElements) {
		this.scriptElements = scriptElements;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "manager")
	public Set<RealWorldElement> getManagerFor() {
		return managerFor;
	}
	public void setManagerFor(Set<RealWorldElement> managerFor) {
		this.managerFor = managerFor;
	}

	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "User_Id")
	public User getUser() {
		return user;
	}
	public void setUser(User users) {
		user = users;
	}

	@JsonIgnore
	@Column(name = "User_Id", insertable=false, updatable=false)
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer id) {
		userId = id;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "standIn")
//	public Set<Contact> getStandInFor() {
//		return this.standInFor;
//	}
//	public void setStandInFor(Set<Contact> standInFor) {
//		this.standInFor = standInFor;
//	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
//	public Set<ContractState> getContractStates() {
//		return this.contractStates;
//	}
//	public void setContractStates(Set<ContractState> contractStates) {
//		this.contractStates = contractStates;
//	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "talentAgent")
//	public Set<Contact> getTalentAgentFor() {
//		return this.talentAgentFor;
//	}
//	public void setTalentAgentFor(Set<Contact> talentAgentFor) {
//		this.talentAgentFor = talentAgentFor;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
	public Set<ReportRequirement> getReportRequirements() {
		return reportRequirements;
	}

	public void setReportRequirements(Set<ReportRequirement> reportRequirements) {
		this.reportRequirements = reportRequirements;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
	@OrderBy("sent DESC")
	public List<MessageInstance> getMessageInstances() {
		return messageInstances;
	}

	public void setMessageInstances(List<MessageInstance> messageInstances) {
		this.messageInstances = messageInstances;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contact")
	@OrderBy("date")
	public List<Image> getImages() {
		return images;
	}
	public void setImages(List<Image> images) {
		this.images = images;
	}

	/** Returns the image list from the "castMember" (associated Real World Element) if
	 * there is one, otherwise the Contact's image list. */
	@Transient
	public List<Image> getImageList() {
		if (getCastMember() != null) {
			return getCastMember().getImages();
		}
		return getImages();
	}

	@Transient
	public int getImageCount() {
		Collection<Image> pics;
		if ((pics = getImageList()) != null) {
			return pics.size();
		}
		return 0;
	}

	@Transient()
	public boolean getNotify() {
		return getNotifyForAlerts() || getNotifyForCalendarChanges() ||
				getNotifyForNewTask();

		// getNotifyByEmail() || getNotifyByTextMsg() ||
		//		getNotifyByAsstEmail() || this.getNotifyByAsstTextMsg();
	}

//	@Transient()
//	public Set<Image> getContactImages() // Used on Contact page for "head shots"
//	{
//		Set<RealWorldElement> set = getManagerFor();
//		Set<Image> totalimage = new HashSet<Image>(0);
//		for (RealWorldElement rwe : set) {
//			totalimage.addAll(rwe.getImages());
//		}
//		return totalimage;
//	}

	/** See {@link #roleName}. */
	@Transient
	public String getRoleName() {
		if (roleName == null) {
			if (getRole() != null) {
				roleName = getRole().getName();
			}
		}
		return roleName;
	}
	/** See {@link #roleName}. */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #isCast}. */
	@Transient
	public boolean getIsCast() {
		return isCast;
	}
	/** See {@link #isCast}. */
	public void setIsCast(boolean b) {
		isCast = b;
	}

	/** See {@link #isNonProd}. */
	@Transient
	public boolean getIsNonProd() {
		return isNonProd;
	}
	/** See {@link #isNonProd}. */
	public void setIsNonProd(boolean isNonProd) {
		this.isNonProd = isNonProd;
	}

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if(imageResources == null) {
			imageResources = new ArrayList<>();
			ImageResource imageResource = null;

			for(Image image : getImageList()) {
				try {
					imageResource = new ImageResource(image.getId().toString(), image.getThumbnailB(), "image/png");
					imageResource.setId(image.getId());
					imageResource.setTitle(image.getTitle());
					imageResource.setImage(image);
				}
				catch (Exception e) {
					EventUtils.logError(e.getMessage());
				}
				if(imageResource != null) {
					imageResources.add(imageResource);
				}
			}
		}
		return imageResources;
	}

	/** See {@link #imageResources}. */
	@Transient
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
	}

	/*@Transient
	public Long getDocCount() {
		return getContactDocDao().findDocCountByContactId(id);*/
//		Map<String,Object> param = new HashMap<String,Object>();
//		param.put("contactId",id);
//		return getContactDocDao().findCountByNamedQuery("getDocCountAsPerContact",param);
//	}
//	public void setDocCount(Long docCount) {
//		this.docCount = docCount;
//	}

	//for green
	/*@Transient
	public Long getDocCountByStatus() {
		return getContactDocDao().findDocCountByContactIdAndStatus(id);
	}

	@Transient
	public Long[] getPercentageArray() {
		return StatusListBean.getInstance().createStatusGraph(id);
	}*/


	/*	@Transient
	public ContactDocumentDAO getContactDocDao() {
		if (contactDocDao == null) {
			contactDocDao = ContactDocumentDAO.getInstance();
		}
		return contactDocDao;
	}
*/
	/** See {@link #checked}. */
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/** See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/*@ManyToMany(
			targetEntity=Document.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approval_path_contact_pool",
			joinColumns = {@JoinColumn(name = "Contact_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Approval_Path_Id")}
			)
	public Set<ApprovalPath> getapprovalPaths() {
		return approvalPaths;
	}

	public void setapprovalPaths(Set<ApprovalPath> approvalPaths) {
		this.approvalPaths = approvalPaths;
	}*/

	@Override
	public int compareTo(Contact contact) {
		int ret = 1;
		if (contact != null) {
			return compareTo(contact, "");
		}
		return ret;
	}

	public int compareTo(Contact c2, String sortField, boolean ascending) {
		int ret = compareTo(c2, sortField);
		return (ascending ? ret : (0-ret));
	}

	public int compareTo(Contact c2, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by name (later)
		}
		else if (sortField.equals(SORTKEY_ROLE) ) {
			ret = StringUtils.compareIgnoreCase(getRoleName(), c2.getRoleName());
		}
		else if (sortField.equals(SORTKEY_STATUS) ) {
			ret = NumberUtils.compare(getStatus().ordinal(), c2.getStatus().ordinal());
		}
		else  if (sortField.equals(SORTKEY_DEPT)) {
			if (getRole() != null && getRole().getDepartment() != null ) {
				if (c2.getRole() == null || c2.getRole().getDepartment() == null) {
					ret = 1;
				}
				else {
					ret = getRole().getDepartment().compareTo(c2.getRole().getDepartment());
				}
			}
			else if (c2.getRole() != null) {
				ret = -1;
			}
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			ret = compareByName(c2); // ... so do name compare
		}
		return ret;
	}

	public int compareByName(Contact c2) {
		int ret = getUser().compareByName(c2.getUser());
		return ret;
	}

	@Override
	public Contact clone() {
		Contact newcontact;
		try {
			newcontact = (Contact)super.clone();
			newcontact.id = null;
			if (getHomeAddress() != null) {
				newcontact.setHomeAddress(getHomeAddress().clone());
			}
			newcontact.assistant = null;
			newcontact.castMember = null;
			newcontact.department = null;
			newcontact.images = null;
			newcontact.managerFor = null;
			newcontact.projectMembers = null;
			newcontact.messageInstances = null;
			newcontact.reportRequirements = null;
			newcontact.project = null;
			newcontact.role = null;
			newcontact.scriptElements = null;
			newcontact.startForms = null;
		}
		catch (CloneNotSupportedException e) {
			log.error("contact clone error: ", e);
			return null;
		}
		return newcontact;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getEmailAddress()==null ? "null" : getEmailAddress());
		s += "]";
		return s;
	}

}