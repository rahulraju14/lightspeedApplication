package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.ContractMappingDAO;
import com.lightspeedeps.dao.UnionsDAO;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * The Employment entity specifies that a Contact has an occupation. It defines
 * the department and project of that occupation. It holds occupation-related
 * information that may be changed without having to issue a new StartForm --
 * that is, data which may be changed without notifying the employee or
 * requiring their signature -- such as account codes, production batch, or
 * reporting department.
 * <p>
 * The Employment instance also relates the Contact to the set of documents
 * (ContactDocument instances) that have been distributed to the person in
 * relation to that particular occupation.
 * <p>
 * Currently there is one Employment instance for each active StartForm
 * instance. (That is, if a StartForm has been superseded by another "change"
 * StartForm, the superseded Start will not have an associated Employment
 * instance.)
 */
@NamedQueries ({
		@NamedQuery(name=Employment.GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT, query = "from Employment e where e.contact.production =:production and e.project =:project"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT_DEPARTMENT, query = "from Employment e where e.contact.production =:production and e.project =:project and e.role.department =:department"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT, query = "from Employment e where ((e.project =:project) or (e.contact.production =:production and e.project is null)) and e.productionBatch is null"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_OR_NULL_ACTIVE, query = "from Employment e where ((e.project =:project) or (e.contact.production =:production and e.project is null)) and e.contact.status <> 'DELETED'"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_FEATURE_ACTIVE, query = "from Employment e where e.contact.production =:production and e.contact.status <> 'DELETED'"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_CROSS_PROJECT_ACTIVE, query =
			"select distinct pm.employment from ProjectMember pm where (pm.unit is null and pm.employment.contact.production =:production) " +
			" and pm.employment.contact.status <> 'DELETED'"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PROJECT_MEMBER_ACTIVE, query =
			"select distinct pm.employment from ProjectMember pm where (pm.unit is not null and pm.unit.project =:project) " +
			" and pm.employment.contact.status <> 'DELETED'"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_DEPARTMENT, query = "from Employment e where ((e.contact.production =:production) and (e.project =:project or e.project is null)) and e.role.department =:department and e.productionBatch is null"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_DEPARTMENT, query = "from Employment e where e.contact.production =:production and e.role.department =:department"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_BATCH, query = "from Employment e where e.contact.production =:production and (e.project =:project or e.project is null) and e.productionBatch =:batch"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_CONTACT_PROJECT, query = "from Employment e where e.contact =:contact and e.project =:project"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID, query = "select distinct cd.employment from ContactDocument cd where cd.document.id =:documentId and cd.employment.contact.production =:production"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID_PROJECT, query = "select distinct cd.employment from ContactDocument cd where cd.document.id =:documentId and cd.employment.project =:project"),
		@NamedQuery(name=Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID, query = "select distinct cd.employment from ContactDocument cd where cd.approverId in (:approverId) and cd.employment.contact.production =:production"),
		@NamedQuery(name=Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_DOCUMENT_ID, query = "select distinct cd.employment from ContactDocument cd where cd.approverId in (:approverId) and cd.employment.contact.production =:production and cd.document.id =:documentId"),
		@NamedQuery(name=Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT, query = "select distinct cd.employment from ContactDocument cd where cd.approverId in (:approverId) and cd.employment.project =:project"),
		@NamedQuery(name=Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT_DOCUMENT_ID, query = "select distinct cd.employment from ContactDocument cd where cd.approverId in (:approverId) and cd.employment.project =:project and cd.document.id =:documentId"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_OCCUPATION_CONTACT_PROJECT, query = "from Employment e where e.contact =:contact and e.project =:project and e.occupation =:occupation"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_OCCUPATION_CONTACT, query = "from Employment e where e.contact =:contact and e.occupation =:occupation"),
		@NamedQuery(name=Employment.GET_EMPLOYMENT_BY_PRODUCTION, query = "from Employment e where e.contact.production =:production"),
})
@Entity
@Table(name = "employment")
public class Employment extends Form<Employment> implements Cloneable, Comparable<Employment>{
	private static final Log log = LogFactory.getLog(Employment.class);

	private static final long serialVersionUID = -541748123038907754L;

	public static final String GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT = "getEmploymentStartStatusByProductionProject";
	public static final String GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT_DEPARTMENT = "getEmploymentStartStatusByProductionProjectDepartment";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_PROJECT = "getEmploymentByProductionProject";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_OR_NULL_ACTIVE = "getEmploymentByProductionProjectOrNullActive";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_FEATURE_ACTIVE = "getEmploymentByProductionFeatureActive";
	public static final String GET_EMPLOYMENT_BY_CROSS_PROJECT_ACTIVE = "getEmploymentByCrossProjectActive";
	public static final String GET_EMPLOYMENT_BY_PROJECT_MEMBER_ACTIVE = "getEmploymentByProjectMemberActive";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_DEPARTMENT = "getEmploymentByProductionProjectDepartment";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_DEPARTMENT = "getEmploymentByProductionDepartment";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION_PROJECT_BATCH = "getEmploymentByProductionProjectBatch";
	public static final String GET_EMPLOYMENT_BY_CONTACT_PROJECT = "getEmploymentByContactProject";
	public static final String GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID = "getEmpListByDocumentId";
	public static final String GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID_PROJECT = "getEmpListByDocumentIdProject";
	public static final String GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID = "getContactDocumentEmploymentByApproverId";
	public static final String GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_DOCUMENT_ID = "getContactDocumentEmploymentByApproverIdDocumentId";
	public static final String GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT = "getContactDocumentEmploymentByApproverIdProject";
	public static final String GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT_DOCUMENT_ID = "getContactDocumentEmploymentByApproverIdProjectDocumentId";
	public static final String GET_EMPLOYMENT_BY_OCCUPATION_CONTACT_PROJECT= "getEmploymentByOccupationContactProject";
	public static final String GET_EMPLOYMENT_BY_OCCUPATION_CONTACT= "getEmploymentByOccupationContact";
	public static final String GET_EMPLOYMENT_BY_PRODUCTION = "getEmploymentByProduction";

	/** Key to sort by the default sort sequence: last name, first name, date, occupation.  */
	public static final String SORTKEY_NAME = "name";

	/** Key to sort by date, followed by default sort sequence. */
	public static final String SORTKEY_DATE = "date";

	/** key to sort by descending date & ascending occupation; this is used for
	 * the mobile timecard list. */
	public static final String SORTKEY_OCCUPATION = "occupation";

	/** Key to sort by account codes */
	public static final String SORTKEY_ACCT = "acct";

	/** Key to sort by the Free1 account code field. */
	public static final String SORTKEY_ACCT_FREE1 = "free1";

	/** Key to sort by the Free2 account code field. */
	public static final String SORTKEY_ACCT_FREE2 = "free2";

	/** key to sort by Union number */
	public static final String SORTKEY_BATCH = "batch";

	/** key to sort by Job number */
	public static final String SORTKEY_JOB_NUMBER = "jobNumber";

	/** key to sort by Job name */
	public static final String SORTKEY_JOB_NAME = "jobName";

	/** Key to sort by Dept, followed by default sort sequence. */
	public static final String SORTKEY_DEPT = "dept";

	/** key to sort by Union number, followed by default sort sequence. */
	public static final String SORTKEY_UNION = "union";

	/** key to sort by occupation code */
	public static final String SORTKEY_OCC_CODE = "occCode";

	/** Key to sort by the rate field. */
	public static final String SORTKEY_RATE = "rate";

	/** Key to sort by the rate field. */
	public static final String SORTKEY_GUAR_HOURS = "guar";

	/** key to sort by Job number */
	public static final String SORTKEY_EMP_TYPE = "empType";

	/** key to sort by Job number */
	public static final String SORTKEY_TERMS = "terms";

	/** Key to sort by Status of the startform's required fields. */
	public static final String SORTKEY_STATUS = "status";

	/** The Contact associated with the Employment instance. Note that a Contact is
	 * (by definition) associated with a single Production. */
	private Contact contact;

	//private Department department; //  remove set(); change get() to use getRole().getDepartment();

	// private StartForm startForm; reverse mapping done in start form

	//private Integer projectMemberId; // * DROP *

	/** The Permission`s, specified as a 64-bit mask, assigned to this Contact
	 * for this Employment (occupation/role).  These permissions may have been customized.  If they
	 * have not been customized, this field will be identical to Role.roleGroup.permissionMask. */
	private long permissionMask;

	/** The Set of Permission values matching the permissionMask value. */
	@Transient
	private Set<Permission> permissions;

	private String occupation;

	private boolean offProduction;

	/** This Employment defines the default role/occupation/dept to be displayed
	 * in crew lists, etc. */
	private boolean defRole;

	private AccountCodes account;

	private Date endDate;

	private ProductionBatch productionBatch;

	private boolean skipDeptApprovers;

	private String wageState;

	/** The Project associated with this StartForm.  Only used for Commercial
	 * productions.  For TV & Feature productions, this will be null. */
	private Project project;

	private boolean additionalStaff;

	/** The date when employment was last updated. */
	private Date lastUpdated;

	/** The date when employment was last sent. */
	private Date lastSent;

	//private Unit unit;  // Now an attribute of an associated UnitMember

	/* Added : */
	private Role role;

	private List<ProjectMember> projectMembers = new ArrayList<>();

	/** The collection of StartForm`s related to this Employment instance, maintained
	 * by a foreign key in the StartForm records. */
	private Set<StartForm> startForms = new HashSet<>();

	@Transient
	private boolean checked;

	@Transient
	private boolean allowDelete;

	@Transient
	private EmpStatus status;

	@Transient
	private Integer departmentId;

	@Transient
	private Integer relatedStartFormId;

	/** The effective start date to be assigned to the newly-created StartForm. */
	/*@Transient
	private Date effectiveStartDate;*/

	@Transient
	private boolean copyStartData;

	/** Used to hold the selection (highlighted) status when the Employment is displayed in a list. */
	@Transient
	private boolean selected;

//	@Transient
//	private StartForm startForm;

	/** The count of documents distributed to this employment instance which are
	 * in a final status (Approved or Locked). Although this may be determined
	 * by a specific database query, in it's most common usage, on the
	 * Dist&Review tab, it is filled in by StatusListBean using a much more
	 * efficient method. */
	@Transient
	Integer docCountByStatus;

	/** The count of documents distributed to this employment instance. Although this may be determined
	 * by a specific database query, in it's most common usage, on the Dist&Review tab, it is filled in
	 * by StatusListBean using a much more efficient method. */
	@Transient
	Integer docCount;

	/** The value used to sort the employment status list when sorted by the "Count" column. */
	@Transient
	Float docSortCount;

	@Transient
	private Integer[] percentageArray;


	/** Used to disable/enable the defRole check box in Commercial production.
	 *  True iff the employment is the only single employment in that project. */
	@Transient
	private boolean disableEmpDefRole = false;

	/** Used to Show/hide the Starts icon for a particular employment for the logged in person on Cast & Crew tab. */
	@Transient
	private boolean showStartsIcon = false;

	/** Used to disable the Data Admin employment record in occupation table for the logged in Non LS Admin person on Cast & Crew tab. */
	@Transient
	private boolean disableDataAdminEmp = false;

	public Employment() {
		super();
	}

	// LS-1196 - Added occupation.
	public Employment(Role role, String occupation, Contact contact, long mask) {
		this.role = role;
		this.occupation = occupation;
		this.contact = contact;
		permissionMask = mask;
	}

	// LS-1196 - Added occupation.
	public Employment(Role role, String occupation, Contact contact) {
		this.role = role;
		this.occupation = occupation;
		this.contact = contact;

		if (role != null) {
			permissionMask = role.getRoleGroup().getPermissionMask();
		}
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id", nullable = false)
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Dept_Id")
	@Transient
	public Department getDepartment() {
		if(getRole() != null) {
			Department dept = getRole().getDepartment();
			//dept = DepartmentDAO.getInstance().refresh(dept);
			return dept;
		}
		return null;
	}
	/*@Deprecated
	public void setDepartment(Department department) {
		//this.department = department; DROPPED
	}*/

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Project_Id")
//	public Project getProject() {
//		return project;
//	}
//	public void setProject(Project project) {
//		this.project = project;
//	}

	/*@ManyToOne(fetch = FetchType.LAZY) REMOVED - needed a reverse mapping on start form
	@JoinColumn(name = "Start_Form_Id")
	public StartForm getStartForm() {
		return startForm;
	}
	public void setStartForm(StartForm startForm) {
		this.startForm = startForm;
	}*/

	/**See {@link #occupation}. */
	@Column(name = "Occupation", length = 100)
	public String getOccupation() {
		return occupation;
	}
	/**See {@link #occupation}. */
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #permissionMask}. */
	@Column(name = "Permission_Mask")
	public long getPermissionMask() {
		return permissionMask;
	}
	/** See {@link #permissionMask}. */
	public void setPermissionMask(long permissionMask) {
		this.permissionMask = permissionMask;
	}

	/**
	 * See {@link #role}. It has the CascadeType.SAVE_UPDATE property so that when a
	 * (new) Employment is saved which refers to a new (custom) Role, it will
	 * save that Role as well.
	 * <p>
	 * Note that using the javax CascadeType.PERSIST did NOT work -- it did not cascade
	 * the save operation (from ProjectDAO.createNewProject).
	 */
	@Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE})
	@ManyToOne(fetch = FetchType.LAZY)
//	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "Role_Id", nullable = false)
	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}


	/** See {@link #permissions}. */
	@Transient
	public Set<Permission> getPermissions() {
		if (permissions == null) {
			permissions = Permission.createPermissionSet(getPermissionMask());
		}
		return permissions;
	}

	@Column(name = "Off_Production", nullable = false)
	public boolean getOffProduction() {
		return offProduction;
	}
	public void setOffProduction(boolean offProduction) {
		this.offProduction = offProduction;
	}

	/** See {@link #defRole}. */
	@Column(name = "Def_Role", nullable = false)
	public boolean getDefRole() {
		return defRole;
	}
	/** See {@link #defRole}. */
	public void setDefRole(boolean defRole) {
		this.defRole = defRole;
	}

	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 	column = @Column(name = "Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 		column = @Column(name = "Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 	column = @Column(name = "Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Acct_Free2", length = 10) ),
	} )
	public AccountCodes getAccount() {
		if (account == null) {
			account = new AccountCodes();
		}
		return account;
	}
	public void setAccount(AccountCodes account) {
		this.account = account;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 10)
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Batch_Id")
	public ProductionBatch getProductionBatch() {
		return productionBatch;
	}
	public void setProductionBatch(ProductionBatch productionBatch) {
		this.productionBatch = productionBatch;
	}

	@Column(name = "Skip_Dept_Approvers", nullable = false)
	public boolean getSkipDeptApprovers() {
		return skipDeptApprovers;
	}
	public void setSkipDeptApprovers(boolean skipDeptApprovers) {
		this.skipDeptApprovers = skipDeptApprovers;
	}

	@Column(name = "Wage_State", length = 10)
	public String getWageState() {
		return wageState;
	}
	public void setWageState(String wageState) {
		this.wageState = wageState;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Column(name = "Additional_Staff", nullable = false)
	public boolean getAdditionalStaff() {
		return additionalStaff;
	}
	public void setAdditionalStaff(boolean additionalStaff) {
		this.additionalStaff = additionalStaff;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Unit_Id")
//	public Unit getUnit() {
//		return unit;
//	}
//	public void setUnit(Unit unit) {
//		this.unit = unit;
//	}

	/** See {@link #lastUpdated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Updated", length = 0)
	public Date getLastUpdated() {
		return lastUpdated;
	}
	/** See {@link #lastUpdated}. */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/** See {@link #lastSent}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Sent", length = 0)
	public Date getLastSent() {
		return lastSent;
	}
	/** See {@link #lastSent}. */
	public void setLastSent(Date lastSent) {
		this.lastSent = lastSent;
	}

	@Transient
	public boolean getChecked() {
		return checked;
	}
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	@Transient
	public boolean getAllowDelete() {
		return allowDelete;
	}
	public void setAllowDelete(boolean allowDelete) {
		this.allowDelete = allowDelete;
	}

	@Transient
	public EmpStatus getStatus() {
		return status;
	}
	public void setStatus(EmpStatus status) {
		this.status = status;
	}

	/** See {@link #docCount}. */
	@Transient
	public Integer getDocCount() {
		if (docCount == null) {
			docCount = ContactDocumentDAO.getInstance().findDocCountByEmploymentId(id);
		}
		return docCount;
	}
	/** See {@link #docCount}. */
	public void setDocCount(Integer docCount) {
		this.docCount = docCount;
	}

	/** See {@link #docSortCount}. */
	@Transient
	public Float getDocSortCount() {
		if (docSortCount == null) {
			if (getDocCount() == 0) {
				docSortCount = 0.0f;
			}
			else {
				// ((% of Yellow docs)*0.5) + (% of Green docs) - ((% of Red)*0.1)) - ((% of Black)*0.2)
				// percentageArray[] = {%green, %red, %yellow, %black}
				// 'docFactor' adds slight adjustment so entries with same %, but more docs, sort worse.
				float docFactor = (float)(getDocCount()*.001);
				docSortCount = (float)((
						  (getPercentageArray()[2] * 0.5 * (1.0 + docFactor))
						+  getPercentageArray()[0] * (1.0 + docFactor)
						+ (getPercentageArray()[1] * (-0.1 - docFactor))
						+ (getPercentageArray()[3] * (-0.2 - (2.0f*docFactor))) ) );
			}
			log.debug(docSortCount);
		}
		return docSortCount;
	}

	/** See {@link #docCountByStatus}. */
	@Transient
	public Integer getDocCountByStatus() {
		if (docCountByStatus == null) {
			docCountByStatus = ContactDocumentDAO.getInstance().findDocCountByEmploymentIdAndStatus(id);
		}
		return docCountByStatus;
	}
	/** See {@link #docCountByStatus}. */
	public void setDocCountByStatus(Integer docCountByStatus) {
		this.docCountByStatus = docCountByStatus;
	}

	@Transient
	public Integer[] getPercentageArray() {
		if (percentageArray == null) {
			log.debug("");
			DocumentUtils.createStatusGraphForEmployment(this, null);
		}
		return percentageArray;
	}
	public void setPercentageArray(Integer[] array) {
		percentageArray = array;
	}


	/** surrogate for account.aloc
	 * @see AccountCodes#getAloc() */
	@JsonIgnore
	@Transient
	public String getAccountLoc() {
		return getAccount().getAloc();
	}
	public void setAccountLoc(String code) {
		getAccount().setAloc(code);
	}

	/** surrogate for account.major
	 * @see AccountCodes#getMajor() */
	@JsonIgnore
	@Transient
	public String getAccountMajor() {
		return getAccount().getMajor();
	}
	public void setAccountMajor(String accountMajor) {
		getAccount().setMajor(accountMajor);
	}

	/** surrogate for account.dtl
	 * @see AccountCodes#getDtl() */
	@JsonIgnore
	@Transient
	public String getAccountDtl() {
		return getAccount().getDtl();
	}
	public void setAccountDtl(String accountDtl) {
		getAccount().setDtl(accountDtl);
	}

	/** surrogate for account.Sub
	 * @see AccountCodes#getSub() */
	@JsonIgnore
	@Transient
	public String getAccountSub() {
		return getAccount().getSub();
	}
	public void setAccountSub(String accountSub) {
		getAccount().setSub(accountSub);
	}

	/** surrogate for account.set
	 * @see AccountCodes#getSet() */
	@JsonIgnore
	@Transient
	public String getAccountSet() {
		return getAccount().getSet();
	}
	public void setAccountSet(String accountSet) {
		getAccount().setSet(accountSet);
	}

	/** surrogate for account.free
	 * @see AccountCodes#getFree() */
	@JsonIgnore
	@Transient
	public String getAccountFree() {
		return getAccount().getFree();
	}
	public void setAccountFree(String free) {
		getAccount().setFree(free);
	}

	/** surrogate for account.free2
	 * @see AccountCodes#getFree2() */
	@JsonIgnore
	@Transient
	public String getAccountFree2() {
		return getAccount().getFree2();
	}
	public void setAccountFree2(String free) {
		getAccount().setFree2(free);
	}

	/** See {@link #departmentId}. */
	@Transient
	public Integer getDepartmentId() {
		if (departmentId == null) {
			try {
				if(getRole() != null) {
					departmentId = getRole().getDepartment().getId();
				}
			}
			catch (Exception e) {
				EventUtils.logError("Unable to fetch department id for ProjectMember", e);
				throw new LoggedException(e);
			}
		}
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/** See {@link #relatedStartFormId}. */
	@Transient
	public Integer getRelatedStartFormId() {
		return relatedStartFormId;
	}
	/** See {@link #relatedStartFormId}. */
	public void setRelatedStartFormId(Integer relatedStartFormId) {
		this.relatedStartFormId = relatedStartFormId;
	}

	/** See {@link #effectiveStartDate}. */
	/*@Transient
	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}*/
//	/** See {@link #effectiveStartDate}. */
//	public void setEffectiveStartDate(Date effectiveStartDate) {
//		this.effectiveStartDate = effectiveStartDate;
//	}

	/** See {@link #copyStartData}. */
	@Transient
	public boolean getCopyStartData() {
		return copyStartData;
	}
	/** See {@link #copyStartData}. */
	public void setCopyStartData(boolean copyStartData) {
		this.copyStartData = copyStartData;
	}

	/* 	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "employment")
	public List<ProjectMember> getProjectMembers() {
		return projectMembers;
	}
	public void setProjectMembers(List<ProjectMember> members) {
		projectMembers = members;
	}

	@Transient
	public boolean getForAllProjects() {
		if (getProjectMembers() != null && getProjectMembers().size() > 0) {
			return getProjectMembers().get(0).getUnit() == null;
		}
		return false;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "employment")
	@OrderBy("id DESC")
	public Set<StartForm> getStartForms() {
		return startForms;
	}
	public void setStartForms(Set<StartForm> startForms) {
		this.startForms = startForms;
	}

	/** See {@link #selected}. */
	@JsonIgnore
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** @return the most recently created StartForm related to
	 * this Employment, or null if no StartForm`s exist. */
	@Transient
	public StartForm getStartForm() {
		StartForm sf = null;
		if (getStartForms() != null && ! getStartForms().isEmpty()) {
			sf = getStartForms().iterator().next();
		}
		return sf;
	}

	@Transient
	/** See {@link #disableDefRole}. */
	public boolean getDisableEmpDefRole() {
		return disableEmpDefRole;
	}
	/** See {@link #disableEmpDefRole}. */
	public void setDisableEmpDefRole(boolean disableEmpDefRole) {
		this.disableEmpDefRole = disableEmpDefRole;
	}

	@Transient
	/** See {@link #showStartsIcon}. */
	public boolean getShowStartsIcon() {
		return showStartsIcon;
	}
	/** See {@link #showStartsIcon}. */
	public void setShowStartsIcon(boolean showStartsIcon) {
		this.showStartsIcon = showStartsIcon;
	}

	@Transient
	/** See {@link #disableDataAdminEmp}. */
	public boolean getDisableDataAdminEmp() {
		return disableDataAdminEmp;
	}
	/** See {@link #disableDataAdminEmp}. */
	public void setDisableDataAdminEmp(boolean disableDataAdminEmp) {
		this.disableDataAdminEmp = disableDataAdminEmp;
	}

	@Override
	public Employment clone() {
		Employment employment;
		try {
			employment = (Employment) super.clone();
			employment.id = null;
			employment.productionBatch = null;
			employment.role = null;
			employment.projectMembers = new ArrayList<>();
			employment.startForms = new HashSet<>();
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
		return employment;
	}

	/**
	 * Default compareTo - uses database id for equality check, then start
	 * Date, followed by last name, and first name for comparison.
	 *
	 * @param other
	 * @return Standard compare result -1/0/1.
	 */
	@Override
	public int compareTo(Employment other) {
		int ret = 1;
		if (other != null) {
			if (getId().equals(other.getId())) {
				return 0; // same entity
			}
			// note - use a compare that does NOT require loading the StartForm!
			return compareTo(other, SORTKEY_OCCUPATION);
		}
		return ret;
	}

	/**
	 * Compare using the specified field, with the specified ordering.
	 *
	 * @param other The WeeklyTimecard to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(Employment other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 *
	 * @param other The Employment to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(Employment other, String sortField) {
		int ret=0;
		if (other == null) {
			return 1;
		}
		else if (sortField == null) {
			return compareTo(other); // date/adjusted/name = default comparison
		}

		if (sortField.equals(SORTKEY_DATE) ) { // effective Start date
			ret = compareDate(other);
		}
		else if (sortField.equals(SORTKEY_STATUS)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = NumberUtils.compare((getStartForm().getIsComplete()?1:0) + (getStartForm().getHasRequiredFields()?1:0),
						(other.getStartForm().getIsComplete()?1:0) + (other.getStartForm().getHasRequiredFields()?1:0));
			}
		}
		else if (sortField.equals(SORTKEY_ACCT)) {
			ret = compareAccount(other);
		}
		else if (sortField.equals(SORTKEY_ACCT_FREE1)) {
			ret = StringUtils.compareIgnoreCase(getAccountFree(), other.getAccountFree());
			if (ret == 0) {
				ret = StringUtils.compareIgnoreCase(getAccountFree2(), other.getAccountFree2());
				if (ret == 0) {
					ret = compareAccount(other);
				}
			}
		}
		else if (sortField.equals(SORTKEY_ACCT_FREE2)) {
			ret = StringUtils.compareIgnoreCase(getAccountFree2(), other.getAccountFree2());
			if (ret == 0) {
				ret = StringUtils.compareIgnoreCase(getAccountFree(), other.getAccountFree());
				if (ret == 0) {
					ret = compareAccount(other);
				}
			}
		}
		else if (sortField.equals(SORTKEY_BATCH)) {
			ret = compareBatch(other);
		}
		else if (sortField.equals(SORTKEY_JOB_NUMBER)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = StringUtils.compare(getStartForm().getJobNumber(), other.getStartForm().getJobNumber());
			}
		}
		else if (sortField.equals(SORTKEY_JOB_NAME)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = StringUtils.compare(getStartForm().getJobName(), other.getStartForm().getJobName());
			}
		}
		else if (sortField.equals(SORTKEY_DEPT)) {
			ret = StringUtils.compare(getDepartment().getName(), other.getDepartment().getName());
		}
		else if (sortField.equals(SORTKEY_UNION)) {
			try {
				if (getStartForm() != null && other.getStartForm() != null) {
					ret = StringUtils.compareNumeric(getStartForm().getUnionLocalNum(), other.getStartForm().getUnionLocalNum());
				}
			}
			catch (Exception e) { // mostly ignore exceptions
				log.warn("Exception while sorting Employment by Union: ", e);
			}
		}
		else if (sortField.equals(SORTKEY_OCCUPATION)) {
			ret = StringUtils.compare(getOccupation(), other.getOccupation());
			if (ret == 0) {
				// use a 2nd-level compare that does NOT require loading the StartForm.
				ret = NumberUtils.compare(getId(), other.getId());
			}
		}
		else if (sortField.equals(SORTKEY_OCC_CODE)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = StringUtils.compare(getStartForm().getOccupationCode(), other.getStartForm().getOccupationCode());
			}
		}
		else if (sortField.equals(SORTKEY_RATE)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = NumberUtils.compare(getStartForm().getRate(), other.getStartForm().getRate());
			}
		}
		else if (sortField.equals(SORTKEY_GUAR_HOURS)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = NumberUtils.compare(getStartForm().getGuarHours(), other.getStartForm().getGuarHours());
			}
		}
		else if (sortField.equals(SORTKEY_EMP_TYPE)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = StringUtils.compare(getStartForm().getRateType().getLabel(), other.getStartForm().getRateType().getLabel());
			}
		}
		else if (sortField.equals(SORTKEY_TERMS)) {
			if (getStartForm() != null && other.getStartForm() != null) {
				ret = getStartForm().getUseStudioOrLoc().compareTo(other.getStartForm().getUseStudioOrLoc());
			}
		}

		if (ret == 0) { // this will also get run if sortField == SORTKEY_NAME
			ret = getContact().compareByName(other.getContact());
			if (ret == 0 && ! sortField.equals(SORTKEY_DATE)) { // same name, compare date
				ret = - compareDate(other); // put most recent first
			}
			if (ret == 0) { // still the same, compare occupation
				ret = StringUtils.compare(getOccupation(), other.getOccupation());
			}
		}

		return ret;
	}

	/**
	 * Compare the "date" of this employment, which actually checks the related
	 * StartForm's start/effective dates, if available.
	 *
	 * @param other The Employment to which this one is compared.
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareDate(Employment other) {
		if (getStartForm() != null ) {
			return getStartForm().compareDate(other.getStartForm());
		}
		else if (other.getStartForm() != null) {
			return -1;
		}
		return 0;
	}

	/**
	 * Compare the account number fields of two Employments.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareAccount(Employment other) {
		int ret = StringUtils.compareIgnoreCase(getAccountMajor(),other.getAccountMajor());
		if (ret == 0) {
			ret = StringUtils.compareIgnoreCase(getAccountDtl(), other.getAccountDtl());
			if (ret == 0) {
				ret = StringUtils.compareIgnoreCase(getAccountSet(), other.getAccountSet());
				if (ret == 0) {
					ret = StringUtils.compareIgnoreCase(getAccountFree(), other.getAccountFree());
					if (ret == 0) {
						ret = StringUtils.compareIgnoreCase(getAccountFree2(), other.getAccountFree2());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Compare the account number fields of two Employments.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareBatch(Employment other) {
		ProductionBatch batch = getProductionBatch();
		ProductionBatch otherBatch = other.getProductionBatch();
		if (batch == null) {
			if (otherBatch == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			return batch.compareTo(otherBatch);
		}
	}

	/**
	 * Generate export data for the Employment record, which is the
	 * "Summary Record" in the Team Start Documents export file.
	 *
	 * @param ex The Exporter to use to output the data.
	 * @param formW4 A W-4 form to be used for some fields; may be null,
	 * in which case the data is taken from another source.
	 */
	public void exportFlat(Exporter ex, FormW4 formW4) {
		Production prod = SessionUtils.getNonSystemProduction();
		User user = getContact().getUser();
		StartForm sf = getStartForm();

		ex.append(PayrollFormType.SUMMARY.getExportType());
		ex.append(getId());
		ex.appendDateTime(new Date());
		ex.append(prod.getProdId());
		ex.append(prod.getTitle());
		ex.append(prod.getPayrollPref().getPayrollProdId());
		Project proj = getProject();
		if (proj != null) {
			ex.append(proj.getTitle());
			ex.append(proj.getEpisode());
		}
		else {
			ex.append("");
			ex.append("");
		}
		if (true /*getLastSent() == null*/) { // TODO
			ex.append(Constants.DOC_EXPORT_STATUS_NEW);	// transfer status
		}
//		else {
//			ex.append(Constants.DOC_EXPORT_STATUS_UPDATED);
//		}
		if (formW4 != null) {
			ex.append(formW4.getLastName());
			exportFirstMiddle(ex, formW4.getFirstName(), formW4.getMiddleInitial());
		}
		else {
			ex.append(user.getLastName());
			exportFirstMiddle(ex, user.getFirstName(), user.getMiddleName());
		}

		GenderType gender = user.getGender(); // LS-2502 - gender as Enum
		if (gender == null && sf != null) {
			gender = sf.getGender();
		}
		ex.append(gender == null ? "" : gender.name());

		// W4 is preferred source for socialSecurity (SSN)
		String socSec = null;
		if (formW4 != null) {
			socSec = formW4.getSocialSecurity();
		}
		else {
			socSec = user.getSocialSecurity();
		}
		if (socSec == null && sf != null) {
			socSec = sf.getSocialSecurity();
		}
		ex.append(socSec);
		ex.append(getOccupation());

		// Determine TEAM Employer-of-record code
		EmployerOfRecord teamEor = EmployerOfRecord.TEAM_ALT; // default to union crew
		if (prod.getPayrollPref().getTeamEor() != null) {
			teamEor = prod.getPayrollPref().getTeamEor();
		}
		else if (prod.getType().isTours()) {
			teamEor = EmployerOfRecord.TEAM_TOURS;
		}
		else if (sf != null) {
			teamEor = sf.getTeamEor();
		}
		ex.append(teamEor.getExport());
		ex.append(user.getAccountNumber());

		// Export GS Union, Local, and Contract Code
		String gsContractCode = null;
		String gsUnionType = null;
		ContractMapping contractMapping = ContractMappingDAO.findContractMapping(sf);
		if (contractMapping != null) {
			gsContractCode = contractMapping.getGsContractCode();
			gsUnionType = contractMapping.getGsUnionType();
		}
		ex.append(gsUnionType);
		ex.append(gsContractCode);
		ex.append(UnionsDAO.findGsLocalNum(sf));
	}

	/**
	 * Analyzes the first name to see if it may contain a middle initial --
	 * either a trailing single letter, or single letter plus period. If so, and
	 * if there is no middle initial supplied, or the supplied middle initial
	 * matches this part of the first name, then that text is removed from the
	 * first name. The (possibly corrected) first name is then exported, and the
	 * middle initial, either supplied, or derived from the first name, is then
	 * exported.
	 *
	 * @param ex The Exporter.
	 * @param first The 'first name' string, may be null or empty.
	 * @param middle The 'middle initial' string, may be null or empty.
	 */
	private void exportFirstMiddle(Exporter ex, String first, String middle) {
		if (! StringUtils.isEmpty(first)) {
			int ix;
			first = first.trim();
			if (first.length() > 2 && (ix=first.indexOf(" ")) > 0 ) {
				String tail = first.substring(ix+1).trim();
				if (tail.endsWith(".")) {
					tail = tail.substring(0, tail.length()-1);
				}
				if (tail.length() == 1) {
					// looks like a middle initial is part of first Name
					if (StringUtils.isEmpty(middle) || tail.equalsIgnoreCase(middle)) {
						// No middle initial was supplied, or it's the same as the 'tail'
						first = first.substring(0, ix); // so remove it from first name.
						middle = tail.toUpperCase(); // and export this as middle initial
					}
				}
			}
		}
		ex.append(first); // original, or with embedded middle initial removed
		ex.append(middle); // original (may be null or blank), or extracted from firstName
	}

}
