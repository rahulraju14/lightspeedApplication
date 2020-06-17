/** File: StartForm.java */
package com.lightspeedeps.model;

import java.math.*;
import java.text.*;
import java.util.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.*;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.*;
import com.lightspeedeps.dao.SelectionItemDAO;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.util.payroll.OccupationUtils;

/**
 * StartForm entity.
 * This object maintains the "start form" data for an individual Contact.
 * Note that one Contact may have more than one StartForm object (corresponding
 * to more than one set of "start forms".
 *
 * LS-3825 and LS-3915 added printing using PDFTron instead of Jasper reports
 * for commercial clients.
 */
@NamedQueries ({
	@NamedQuery(name = StartForm.GET_DISTINCT_UNION_LOCAL_FOR_CURRENT_PRODUCTION, query = "select distinct sf.unionLocalNum from StartForm sf where sf.contact.production =:production"),
	@NamedQuery(name = StartForm.GET_START_FORM_BY_EMPLOYMENT_DESCENDING_ORDER, query = "from StartForm sf where sf.employment =:employment ORDER BY sf.id DESC"),
	@NamedQuery(name = StartForm.GET_START_FORM_NON_VOID_BY_EMPLOYMENT_DESCENDING_ORDER, query = "select sf from StartForm sf, ContactDocument cd " +
			"where sf.id = cd.relatedFormId and cd.employment =:employment and cd.status <> 'VOID' and cd.formType='START' ORDER BY sf.id DESC"),
	@NamedQuery(name=StartForm.GET_START_FORM_BY_MODEL_RELEASE, query = "from StartForm s where s.modelRelease = :form"),
	@NamedQuery(name = StartForm.GET_START_FORM_BY_PRODUCTION, query = "from StartForm sf where sf.contact.production =:production")
})

@Entity
@Table(name = "start_form")
public class StartForm extends Form<StartForm> implements Cloneable, Comparable<StartForm> {
	private static final Log log = LogFactory.getLog(StartForm.class);

	private static final long serialVersionUID = 1L;

	// Version numbers
	public static final Byte PAROLL_START_VERSION_NON_PREP = 1;
	public static final Byte PAROLL_START_VERSION_PREP = 2;
	public static final Byte DEFAULT_PAROLL_START_VERSION = PAROLL_START_VERSION_NON_PREP;

	// Named Query strings
	/** Get a List of distinct local union numbers (String - sf.unionLocalNum) from all StartForms in a Production. */
	public static final String GET_DISTINCT_UNION_LOCAL_FOR_CURRENT_PRODUCTION = "getDistinctUnionLocalForCurrentProduction";
	/** Get a List of StartForm`s related to a specific Employment instance, in descending order of database id (sf.id). */
	public static final String GET_START_FORM_BY_EMPLOYMENT_DESCENDING_ORDER = "getStartFormByEmploymentInDescendingOrder";
	/** Get a List of StartForm`s related to a specific Employment instance, where the ContactDocument status is not VOID, ordered in
	 * descending database id order (sf.id). */
	public static final String GET_START_FORM_NON_VOID_BY_EMPLOYMENT_DESCENDING_ORDER = "getStartFormNonVoidByEmploymentInDescendingOrder";
	/** Get a List of all StartForm`s for a given Production. */
	public static final String GET_START_FORM_BY_PRODUCTION = "getStartFormByProduction";
	/** Get the StartForm associated with a specific FormModelRelease instance. */
	public static final String GET_START_FORM_BY_MODEL_RELEASE = "getStartFormForModelRelease";

	/** Selection value for studioLocDefault = Studio */
	public static final String USE_STUDIO_RATE = "S";
	/** Selection value for studioLocDefault = Location */
	public static final String USE_LOCATION_RATE= "L";

	/** Key to sort by <last name>, <first name>, <date (reversed)>, adjust flag, occupation */
	public static final String SORTKEY_NAME = "name";

	/** Key to sort by SSN */
	public static final String SORTKEY_SSN = "ssn";

	/** Key to sort by date, last-name, first-name (the default compareTo sequence). */
	public static final String SORTKEY_DATE = "date";

	/** Key to sort by date, adjust flag, last-name, first-name (the default compareTo sequence). */
	public static final String SORTKEY_DEPT = "dept";

	/** key to sort by descending date & ascending occupation; this is used for
	 * the mobile timecard list. */
	public static final String SORTKEY_OCCUPATION = "occupation";

	/** key to sort by occupation code */
	public static final String SORTKEY_OCC_CODE = "occCode";

	/** key to sort by Union number */
	public static final String SORTKEY_UNION = "union";

	/** key to sort by Union number */
	public static final String SORTKEY_BATCH = "batch";

	/** Key to sort by the status field. */
	public static final String SORTKEY_STATUS = "status";

	/** Key to sort by Job Number (Commercial productions) */
	public static final String SORTKEY_JOB_NUMBER = "jobNumber";

	/** Key to sort by Job Number (Commercial productions) */
	public static final String SORTKEY_JOB_NAME = "jobName";

	/** Key to sort by account codes */
	public static final String SORTKEY_ACCT = "acct";

	/** Key to sort by the Free1 account code field. */
	public static final String SORTKEY_ACCT_FREE1 = "free1";

	/** Key to sort by the Free2 account code field. */
	public static final String SORTKEY_ACCT_FREE2 = "free2";

	/** Key to sort by the rate field. */
	public static final String SORTKEY_RATE = "rate";

	/** Key to sort by the Guar. Hours field. */
	public static final String SORTKEY_GUAR_HOURS = "guar";

	/** Key to sort by Employee Type/ Rate Type */
	public static final String SORTKEY_EMP_TYPE = "empType";

	/** Key to sort by Terms field */
	public static final String SORTKEY_TERMS = "terms";

	// Fields

	/** The Contact associated with this StartForm */
	private Contact contact;

	/** The Project associated with this StartForm.  Only used for Commercial
	 * productions.  For TV & Feature productions, this will be null. */
	private Project project;

	/** The database id of the LS project membership (ProjectMember) object referenced when this StartForm
	 * was first created. */
//	private ProjectMember projectMember;
//	private Integer projectMemberId;

	/** The ProductionBatch that names the WeeklyBatch into which timecards generated
	 * as a result of this StartForm will be place. *//*
	private ProductionBatch productionBatch;*/

	/** The database id of the User object who has this timecard locked
	 * for editing. */
	private Integer lockedBy;

	/** A sequential number assigned when the StartForm is created.  The StartForm's for
	 * a single Contact are numbered sequentially beginning with 1. */
	private Integer sequence;

	/** The id field of a previous StartForm that this StartForm supercedes. Applicable
	 * only if the formType is Change or Rehire.  Null if there's no previous related form. */
	private Integer priorFormId;

	/** payroll company's unique form id number */
	private String formNumber;

	/** This attribute marks whether the start form is for a New Employee, a Re-Hire, or a Change to an existing start form */
	private String formType = FORM_TYPE_NEW;
	public static final String FORM_TYPE_NEW = "N";
	public static final String FORM_TYPE_REHIRE = "R";
	public static final String FORM_TYPE_CHANGE = "C";

	// LLC Type
	private static final String LLC_C_TYPE = "C Corporation";
	private static final String LLC_S_TYPE = "S Corporation";

	// Text for cast field labels
	private static final String CAST_OFFICE_FEE_LBL = "Other fees:";
	private static final String CAST_REUSE_FEE_LBL = "Reuse fees ($)";
	private static final String CAST_AGENT_COMMISSION_LBL = "Agent commision (%)";

	/** Status of start form, such as OPEN or SUBMITTED. */
	//private ApprovalStatus status = ApprovalStatus.OPEN;

	/** Production company name */
	private String prodCompany;

	/** Job (project) name (for Commercials) */
	private String jobName;

	/** Job (project) number (for Commercials) */
	private String jobNumber;

	/** The associated Model Release form, if any.  Only exists in productions
	 * with 'useModelRelease' payroll preference set.  LS-4504 */
	private FormModelRelease modelRelease;

	/** Production title */
	private String prodTitle;

	/** Person's first name */
	private String firstName;

	/** Person's middle name - not currently used. */
	private String middleName;

	/** Person's last name (surname) */
	private String lastName;

	/** M/F */
	private GenderType gender;

	/** employee's date of birth */
	private Date dateOfBirth;

	/** Address line 1 */
	/*private String addrLine1;

	*//** Address line 2 *//*
	private String addrLine2;

	*//** City name *//*
	private String city;

	*//** State, usually 2-character postal abbreviation *//*
	private String state;

	*//** zip code *//*
	private String zip;

	*//** Country code (2 characters) *//*
	private String country = Constants.DEFAULT_COUNTRY_CODE;*/

	/** telephone number - validated on input */
	private String phone;

	/** encrypted SSN */
	private String socialSecurity;

	/** The entire set of account codes for the PayJob */
	private AccountCodes account;

	/** Date this StartForm was created (editable); required. */
	private Date creationDate;

	/** Effective starting date; optional. No timecards may be created that
	 * precede this date. If null, use workStartDate. */
	private Date effectiveStartDate;

	/** Effective ending date -- the last effective date of this StartForm. May be null, which means
	 * there is no ending date. If specified, it must be equal to or later than {@link #effectiveStartDate}. */
	private Date effectiveEndDate;

	/** First day of work; required. No timecards may be created that precede this date.
	 * Must be equal to or later than {@link #hireDate}. */
	private Date workStartDate;

	/** Last day is only used when the person is permanently leaving the production. Optional.
	 * If specified, must be equal to or later than {@link #workStartDate}. */
	private Date workEndDate;

	/** May be different than start date; does not affect timecards. Required. */
	private Date hireDate;

	/** The employee's employment basis, for ACA purposes. */
	private EmploymentBasisType employmentBasis;

	/** Retirement plan employee contributes to, if any. */
	private String retirementPlan;

	/** True if the job is off-production. */
	private boolean offProduction;

	/** The union Local # to be displayed.  Typically numeric. This can be considered
	 * the "short name" for the union. */
	private String unionLocalNum = Unions.NON_UNION;

	/** The unique business key to get the {@link Unions} object from the database.  Since some unions have
	 * multiple occupational groups, the same "short name" -- local number -- can have multiple
	 * "long name" descriptions.  The unionKey distinguishes between those variations. */
	private String unionKey = Unions.NON_UNION;

	/** The PayMaster Occupation Code for a specific occupation.  These codes are taken from the contracts
	 * negotiated by the unions, or, in some cases, have been assigned by EP. */
	private String occupationCode;

	/** The Lightspeed-assigned Occupation Code for a specific occupation. These
	 * codes will match {@link #occupationCode} value unless that would cause a duplication.
	 * Also, Lightspeed may assign occupation codes for occupations that do not
	 * have an assigned code, e.g., occupations from the DGA contract. */
	private String lsOccCode;

	/** The contractCode field from the corresponding PayRate entity. */
	private String contractCode;

	/** Person's normal department, if defined in Lightspeed. This is the department
	 * which contains the Lightspeed Role that was selected in the "Create Start form"
	 * dialog, or in the initial creation of the Contact within the Production. Note
	 * that this will be the "system" department when the Production has an equivalent
	 * "pseudo-custom" department (one created just to hold a special list order).  *//*
	private Department department;*/

	/** Name of person's normal department; maintained so the StartForm could
	 * be presented independently of the production's (or LS) department table. *//*
	private String deptName;*/

	/** The official (union) position or job name, e.g., �Location Manager� */
	private String jobClass;

	/** The original contract schedule code, typically alphabetic. If a contract has
	 * schedules without a letter or number, LS assigns one that begins with "X-". */
	private String contractSchedule;

	/** Schedule code from payMaster, e.g., '01'. This code is also used
	 * in the contract rules. */
	private String scheduleCode;

	/** For Videotape agreement, indicates "additional staff" employee. */
	private boolean additionalStaff;

	/** For ASA agreement, indicates employee is a "nearby hire". */
	private boolean nearbyHire;

	/** Which state's overtime rules will apply. If blank, use WorkState. */
	//private String overtimeRule;

	/** This is the Loan out corp. */
	private String loanOutCorpName;

	/**  Loan-out corp address */
	private Address loanOutAddress;

	/**  LS-3635 Loan-out corp mailing address */
	private Address loanOutMailingAddress;

	/** telephone number - validated on input */
	private String loanOutPhone;

	/** Loan-out corp State, usually 2-character postal abbreviation */
	private String incorporationState;

	/** No timecards before this date */
	private Date incorporationDate;

	/** This is federal tax id# for loan-out corp, not SSN */
	private String federalTaxId;

	/** This is state tax id# for loan-out corp, not SSN */
	private String stateTaxId;

	/** True if the loan-out corp is qualified to do business in California. */
	private boolean loanOutQualifiedCa;

	/** True if the loan-out corp is qualified to do business in New York. */
	private boolean loanOutQualifiedNy;

	/** Other states loan-out corp is qualified to do business in. */
	private String loanOutQualifiedStates;

	/** Used for SAG, AFTRA, and DGA required wrap reports on demographics */
	private String ethnicCode;

	/** U/A/O  (U=US), (A=RES. ALIEN), or (O=OTHER); required for reporting & IRS */
	private String citizenStatus = " ";

	/** This is used to determine what laws the person falls under. Also used for State tax reciprocity laws. */
	private String stateOfResidence;

	/** Location of work, usually city or county */
	private String workCity;

	/** State where work will be done */
	private String workState;

	/** Zip code (postal code) where work will be done. */
	private String workZip;

	/** Work country. (was originally "county", but never used for that. */
	private String workCountry = Constants.DEFAULT_COUNTRY_CODE;

	/** True if the Employee is a minor */
	private boolean minor;

	/** This used to indicate to withhold money for the agent; true/false setting. */
	private boolean agentRep;

	/** Employee's Agency or Agent name */
	private String agencyName;

	/**  Agency address */
	private Address agencyAddress;

	/**  Employee Reuse */
	private BigDecimal empReuse;

	/**  Employee Agent Commisssion */
	private BigDecimal empAgentCommisssion;

	/** If true, employee may check "worked" w/o entering hours */
	private boolean allowWorked;

	/** If true, non-union employee's with flat rate (checking "Worked") will be
	 * paid for 6th and 7th days worked; if false, flat rate will be a max of 5 days. */
	private boolean pay6th7thDay = false;

	/** how pay is calculated - Hourly, Daily, or Weekly */
	private EmployeeRateType rateType = EmployeeRateType.HOURLY;

	/** true if user has signed contract; TC process will skip dept approval */
	//private boolean underContract ;

	/** true for contract user - timecard skips dept approver */
	private boolean skipDeptApproval;

	/** use (S) studio rates/hours or use (L)location (Distant) rates/hours by default */
	private String useStudioOrLoc = StartForm.USE_STUDIO_RATE;

	/** Start Rates Type - Standard, Prep or Touring. LS-1346 */
	private StartRatesType startRates = StartRatesType.RATES_STD;

	/** in-Production (shooting) rates, hours, and account codes. */
	private StartRateSet prod;

	/** Pre-Production ("prep") rates, hours, and account codes. */
	private StartRateSet prep;

	/** Holds the additional amount paid for the "Overnight" DayType. */
	private BigDecimal overnight;

	/** Rates and account codes for Box kit rental */
	private AllowanceCap boxRental;

	/** Rates and account codes for car allowance */
	private AllowanceCap carAllow;

	/** Rates and account codes for meal allowance*/
	private AllowanceCheck mealAllow;

	/** Account codes for meal penalties */
	private AccountCodes mealPenalty;

	/** Rates and account codes for taxable (personal) per diem */
	private PerDiem perdiemTx;

	/** Rates and account codes for for non-taxable (business) per diem */
	private PerDiem perdiemNtx;

	/** Rates and account codes for for per diem advance */
	private PerDiem perdiemAdv;

	/** Rates and account codes for meal money */
	private Allowance mealMoney;

	/** Rates and account codes for meal money advance */
	private Allowance mealMoneyAdv;

	/** Account codes for fringes */
	private AccountCodes fringe;

	/** Mileage rate default for expense table; pre-populated from payroll
	 * preferences. */
	private BigDecimal mileageRate;

	/** same as role name; not currently used */
	private String screenCreditRole;
	/** Name to display on screen credits; not currently used. */
	private String screenCreditName;

	/** Is person listed on industry experience roster? True/false; not currently used */
	private boolean industryExpRosterConf = false;

	/** emergency contact information */
	private String emergencyName;

	/** emergency contact phone number */
	private String emergencyPhone;

	/** Relationship of emergency contact to this employee. */
	private String emergencyRelation;

	/** free text; not currently used */
	private String medicalConditions;

	/** List of events that have affected this StartForm, e.g., approvals and rejections.*/
	//private List<StartFormEvent> events = new ArrayList<StartFormEvent>(0);

	/** Employment instance of the start form */
	private Employment employment;

	/**  Mailing address */
	private Address mailingAddress;

	/**  Permanent address */
	private Address permAddress;

	/** Work Zone for this employee. Currently being used for Tours */
	private WorkZone workZone = WorkZone.DL;

	/**  Rate table fields for Tours Production */
	/** Rate for day of the show */
	private BigDecimal toursShowRate;
	/** Rate for preparing for the show */
	private BigDecimal toursPrepRate;
	/** Rate for tear down after the show */
	private BigDecimal toursPostRate;
	/** Rate for traveling to next event */
	private BigDecimal toursTravelRate;
	/** Rate for a day off when on tour */
	private BigDecimal toursDownRate;
	/** Rate when working as staff for home office */
	private BigDecimal toursHomeWorkRate;
	/** Rate when off as staff for home office */
	private BigDecimal toursHomeOffRate;
	/** Whether employee is to paid as an Individual or as a Loan-out. LS-2562 */
	private PaidAsType paidAs;
	/** Tax Classification type for Loan-Out section. LS-2562. */
	private TaxClassificationType taxClassification;
	/** Type of LLC for Loan-Out corporation. LS-2562 */
	private String llcType;
	/** Indicates whether we should use xfdf or jasper reports for printing. LS-3816*/
	private boolean useXfdf;

	// Percent of show rate based on day type */
	/** Paid 100% of show rate */
	@Transient
	private BigDecimal toursShowPercent;
	/** Percentage of show rate paid for Prep day type. */
	@Transient
	private BigDecimal toursPrepPercent;
	/** Percentage of show rate paid for Post day type. */
	@Transient
	private BigDecimal toursPostPercent;
	/** Percentage of show rate paid for Travel day type. */
	@Transient
	private BigDecimal toursTravelPercent;
	/** Percentage of show rate paid for Down day type. */
	@Transient
	private BigDecimal toursDownPercent;
	/** Percentage of work rate paid for Home work zone. */
	@Transient
	private BigDecimal toursHomeWorkPercent;
	/** Percentage of work rate paid for Home work zone. */
	@Transient
	private BigDecimal toursHomeOffPercent;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	/** Used to hold the selection (highlighted) status when the StartForm is displayed in a list. */
	@Transient
	private boolean selected;

	/** Used to hold a check-box status when the StartForm is displayed in a list. */
	@Transient
	private boolean checked;

//	/** Holds the associated LS Role.name field, if any.  When needed, this is looked up
//	 * via the projectMemberId field. */
//	@Transient
//	private String roleName;

	/** Field used to hold the new rate for the start forms */
	@Transient
	private BigDecimal newRate;

	// Constructors

	/** default constructor */
	public StartForm() {
		setVersion(DEFAULT_PAROLL_START_VERSION);
		prod = new StartRateSet();
	}

	// Property accessors

	/** See {@link #contact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id", nullable = false)
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

//	/**See {@link #projectMember}. */
//	@JsonIgnore
//	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//	@JoinColumn(name = "Project_Member_Id")
//	public ProjectMember getProjectMember() {
//		return projectMember;
//	}
//	/**See {@link #projectMember}. */
//	public void setProjectMember(ProjectMember projectMember) {
//		this.projectMember = projectMember;
//	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

//	/**See {@link #projectMemberId}. */
//	@Column(name = "Project_Member_Id")
//	public Integer getProjectMemberId() {
//		return projectMemberId;
//	}
//	/**See {@link #projectMemberId}. */
//	public void setProjectMemberId(Integer projectMemberId) {
//		this.projectMemberId = projectMemberId;
//	}

	/**See {@link #productionBatch}. *//*
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Batch_Id")
	public ProductionBatch getProductionBatch() {
		return productionBatch;
	}
	*//**See {@link #productionBatch}. *//*
	public void setProductionBatch(ProductionBatch productionBatch) {
		this.productionBatch = productionBatch;
	}*/

	/** See {@link #lockedBy}. */
	@JsonIgnore
	@Column(name = "Locked_By")
	public Integer getLockedBy() {
		return lockedBy;
	}
	/** See {@link #lockedBy}. */
	public void setLockedBy(Integer lockedBy) {
		this.lockedBy = lockedBy;
	}

	/** See {@link #sequence}. */
	@Column(name = "Sequence", nullable = false)
	public Integer getSequence() {
		return sequence;
	}
	/** See {@link #sequence}. */
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/** See {@link #priorFormId}. */
	@Column(name = "Prior_Form_Id")
	public Integer getPriorFormId() {
		return priorFormId;
	}
	/** See {@link #priorFormId}. */
	public void setPriorFormId(Integer priorFormId) {
		this.priorFormId = priorFormId;
	}

	/** See {@link #formNumber} */
	@Column(name = "Form_Number", length = 10)
	public String getFormNumber() {
		return formNumber;
	}
	/** See {@link #formNumber} */
	public void setFormNumber(String formNumber) {
		this.formNumber = formNumber;
	}

	/** See {@link #formType} */
	@Column(name = "Form_Type", length = 1)
	public String getFormType() {
		return formType;
	}
	/** See {@link #formType} */
	public void setFormType(String formType) {
		this.formType = formType;
	}

	/**See {@link #status}. */
	/*@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public ApprovalStatus getStatus() {
		return status;
	}
	*//**See {@link #status}. *//*
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}*/

	/** See {@link #prodCompany}. */
	@Column(name = "Prod_Company", length = 100)
	public String getProdCompany() {
		return prodCompany;
	}
	/** See {@link #prodCompany}. */
	public void setProdCompany(String prodCompany) {
		this.prodCompany = prodCompany;
	}

	/** See {@link #prodTitle}. */
	@Column(name = "Prod_Title", length = 100)
	public String getProdTitle() {
		return prodTitle;
	}
	/** See {@link #prodTitle}. */
	public void setProdTitle(String prodTitle) {
		this.prodTitle = prodTitle;
	}

	/** See {@link #jobName}. */
	@Column(name = "Job_Name", length = 100)
	public String getJobName() {
		return jobName;
	}
	/** See {@link #jobName}. */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/** See {@link #jobNumber}. */
	@Column(name = "Job_Number", length = 20)
	public String getJobNumber() {
		return jobNumber;
	}
	/** See {@link #jobNumber}. */
	public void setJobNumber(String jobNumber) {
		this.jobNumber = jobNumber;
	}

	/** See {@link #startForm}. */
	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Model_Release_Id", nullable = true) // , referencedColumnName = "id"
	public FormModelRelease getModelRelease() {
		return modelRelease;
	}
	/** See {@link #startForm}. */
	public void setModelRelease(FormModelRelease modelRelease) {
		this.modelRelease = modelRelease;
	}

	/** See {@link #firstName} */
	@Column(name = "First_Name", nullable = false, length = 30)
	public String getFirstName() {
		return firstName;
	}
	/** See {@link #firstName} */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/** See {@link #middleName} */
	@Column(name = "Middle_Name", length = 30)
	public String getMiddleName() {
		return middleName;
	}
	/** See {@link #middleName} */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	/** See {@link #lastName} */
	@Column(name = "Last_Name", nullable = false, length = 30)
	public String getLastName() {
		return lastName;
	}
	/** See {@link #lastName} */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #gender}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Gender", length = 1)
	public GenderType getGender() {
		return gender;
	}
	/** See {@link #gender}. */
	public void setGender(GenderType gender) {
		this.gender = gender;
	}

	/**See {@link #dateOfBirth}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date_Of_Birth", length = 10)
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	/**See {@link #dateOfBirth}. */
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	/** See {@link #addrLine1} */
	/*@Column(name = "Addr_Line1", length = 100)
	public String getAddrLine1() {
		return addrLine1;
	}
	*//** See {@link #addrLine1} *//*
	public void setAddrLine1(String addrLine1) {
		this.addrLine1 = addrLine1;
	}

	*//** See {@link #addrLine2} *//*
	@Column(name = "Addr_Line2", length = 100)
	public String getAddrLine2() {
		return addrLine2;
	}
	*//** See {@link #addrLine2} *//*
	public void setAddrLine2(String addrLine2) {
		this.addrLine2 = addrLine2;
	}

	*//** See {@link #city} *//*
	@Column(name = "City", length = 50)
	public String getCity() {
		return city;
	}
	*//** See {@link #city} *//*
	public void setCity(String city) {
		this.city = city;
	}

	*//** See {@link #state} *//*
	@Column(name = "State", length = 50)
	public String getState() {
		return state;
	}
	*//** See {@link #state} *//*
	public void setState(String state) {
		this.state = state;
	}

	*//** See {@link #zip} *//*
	@Column(name = "Zip", length = 10)
	public String getZip() {
		return zip;
	}
	*//** See {@link #zip} *//*
	public void setZip(String zip) {
		this.zip = zip;
	}

	*//** See {@link #country} *//*
	@Column(name = "Country", length = 2)
	public String getCountry() {
		return country;
	}
	*//** See {@link #country} *//*
	public void setCountry(String country) {
		this.country = country;
	}*/

	/** See {@link #phone} */
	@Column(name = "Phone", length = 25)
	public String getPhone() {
		return phone;
	}
	/** See {@link #phone} */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/** See {@link #socialSecurity} */
	@JsonIgnore
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	/** See {@link #socialSecurity} */
	public void setSocialSecurity(String socialSecurity) {
		viewSSN = null;
		this.socialSecurity = socialSecurity;
	}

	/**
	 * @return The SSN formatted as nnn-nn-nnnn; used for export or PDFs.
	 *         <p>
	 *         Note that screen (UI) formatting is handled by a Faces converter
	 *         class, SSNConverter.
	 */
	@Transient
	@JsonProperty("socialSecurity")
	public String getSocialSecurityFmtd() {
		String str = getSocialSecurity();
		if (str != null && str.length() == 9) {
			str = str.substring(0,3) + "-" + str.substring(3, 5) + "-" + str.substring(5);
		}
		return str;
	}

	/** See {@link #viewSSN}. */
	@JsonIgnore
	@Transient
	public String getViewSSN() {
		String str = getSocialSecurity();
		if (str == null) {
			viewSSN = null;
		}
		else if (viewSSN == null) {
			if (str.length() >= 4) {
				viewSSN = "###-##-" + str.substring(str.length()-4);
			}
			else {
				viewSSN = str;
			}
		}
		return viewSSN;
	}

	/** Returns just the last 4 digits of the SSN.
	 * See {@link #viewSSN}. */
	@JsonIgnore
	@Transient
	public String getViewSSNmin() {
		String s = null;
		if ((s=getViewSSN()) != null) {
			if (s.length() > 5) {
				s = s.substring(s.length()-5);
			}
		}
		return s;
	}

	/** Returns a composite string of the 3 main accounting fields, the major, detail
	 * and set fields, with each one padded to its maximum length. */
	@JsonIgnore
	@Transient
	public String getAccountsHtml() {
		return pad(getAccountMajor(),6) + "-" + pad(getAccountDtl(),5) + "-" + pad(getAccountSet(),4);
	}

	private static String pad(String str, int len) {
		int xlen = len - str.length();
		for (int i=xlen; i > 0; i--) {
			str = "&#160;" + str;
		}
		return str;
	}

	/** See {@link #account}. */
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
	/** See {@link #account}. */
	public void setAccount(AccountCodes account) {
		this.account = account;
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

	/**
	 * Copy all of the account code fields from the given source into this
	 * object's corresponding account fields.
	 *
	 * @param ac The source of the account codes.
	 */
	@JsonIgnore
	@Transient
	public void setAccountCodes(AccountCodes ac) {
		getAccount().copyFrom(ac);
	}

	/** See {@link #creationDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Creation_Date", nullable = false, length = 10)
	public Date getCreationDate() {
		return creationDate;
	}
	/** See {@link #creationDate}. */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/** See {@link #effectiveStartDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Effective_Start_Date", length = 10)
	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}
	/** See {@link #effectiveStartDate}. */
	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}


	/**
	 * @return The Effective Start Date if not null, otherwise the Work Start
	 *         date. This method will return null if both those dates are null.
	 */
	@JsonIgnore
	@Transient
	public Date getEffectiveOrStartDate() {
		if (getEffectiveStartDate() != null) {
			return effectiveStartDate;
		}
		return getWorkStartDate();
	}

	/**
	 * @return The Effective Start Date if not null, otherwise the Work Start
	 *         date if not null, else the Hire date. Since the Hire Date is not
	 *         allowed to be null, this method should never return null.
	 */
	@JsonIgnore
	@Transient
	public Date getAnyStartDate() {
		if (getEffectiveStartDate() != null) {
			return effectiveStartDate;
		}
		return getWorkStartOrHireDate();
	}

	/** See {@link #effectiveEndDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Effective_End_Date", length = 10)
	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}
	/** See {@link #effectiveEndDate}. */
	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	/** See {@link #workStartDate} */
	@Temporal(TemporalType.DATE)
	@Column(name = "Work_Start_Date", length = 10)
	public Date getWorkStartDate() {
		return workStartDate;
	}
	/** See {@link #workStartDate} */
	public void setWorkStartDate(Date workStartDate) {
		this.workStartDate = workStartDate;
	}

	/** @return The {@link #workStartDate} if not null,
	 * otherwise the {@link #hireDate} */
	@JsonIgnore
	@Transient
	public Date getWorkStartOrHireDate() {
		if (getWorkStartDate() != null) {
			return getWorkStartDate();
		}
		return getHireDate();
	}

	/** See {@link #workEndDate} */
	@Temporal(TemporalType.DATE)
	@Column(name = "Work_End_Date", length = 10)
	public Date getWorkEndDate() {
		return workEndDate;
	}
	/** See {@link #workEndDate} */
	public void setWorkEndDate(Date workEndDate) {
		this.workEndDate = workEndDate;
	}

	/**
	 * @return The earlier of this StartForm's {@link #workEndDate} and
	 *         {@link #effectiveEndDate}. Returns null if both those dates are
	 *         null.
	 */
	@JsonIgnore
	@Transient
	public Date getEarliestEndDate() {
		if (getWorkEndDate() != null) {
			if (getEffectiveEndDate() == null || getWorkEndDate().before(getEffectiveEndDate())) {
				return getWorkEndDate();
			}
		}
		return getEffectiveEndDate(); // Note: May be null
	}

	/** See {@link #hireDate} */
	@Temporal(TemporalType.DATE)
	@Column(name = "Hire_Date", nullable = false, length = 10)
	public Date getHireDate() {
		return hireDate;
	}
	/** See {@link #hireDate} */
	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}

	/** See {@link #employmentBasis}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Employment_Basis")
	public EmploymentBasisType getEmploymentBasis() {
		return employmentBasis;
	}
	/** See {@link #employmentBasis}. */
	public void setEmploymentBasis(EmploymentBasisType employmentBasis) {
		this.employmentBasis = employmentBasis;
	}

	/** See {@link #retirementPlan}. */
	@Column(name = "retirement_plan", length = 50)
	public String getRetirementPlan() {
		return retirementPlan;
	}
	/** See {@link #retirementPlan}. */
	public void setRetirementPlan(String retirementPlan) {
		this.retirementPlan = retirementPlan;
	}

	/** See {@link #offProduction}. */
	@Column(name = "Off_Production", nullable = false)
	public boolean getOffProduction() {
		return offProduction;
	}
	/** See {@link #offProduction}. */
	public void setOffProduction(boolean offProduction) {
		this.offProduction = offProduction;
	}

	/** See {@link #unionLocalNum} */
	@Column(name = "Union_Local_Num", length = 10)
	public String getUnionLocalNum() {
		return unionLocalNum;
	}
	/** See {@link #unionLocalNum} */
	public void setUnionLocalNum(String unionLocalNum) {
		this.unionLocalNum = unionLocalNum;
	}

	/**
	 * @return The single-character code for the TEAM Employer-of-Record
	 * based on the union settings in this Start Form.  This value may be
	 * overridden by a Payroll Preference.
	 */
	@JsonIgnore
	@Transient
	public EmployerOfRecord getTeamEor() {
		EmployerOfRecord teamEor = EmployerOfRecord.TEAM_ALT; // union crew
		if (getUnionLocalNum() != null && getUnionLocalNum().equals(Unions.DGA)) {
			teamEor = EmployerOfRecord.TEAM_DGA;
		}
		else if (getUnionLocalNum() == null || getUnionLocalNum().equals(Unions.NON_UNION)) {
			teamEor = EmployerOfRecord.TEAM_TALENT;
		}
		else if (getUnionLocalNum().equals("SAG")) {
			teamEor = EmployerOfRecord.TEAM_TALENT;
		}
		return teamEor;
	}

	/**See {@link #unionKey}. */
	@Column(name = "Union_Key", length = 10)
	public String getUnionKey() {
		return unionKey;
	}
	/**See {@link #unionKey}. */
	public void setUnionKey(String unionKey) {
		this.unionKey = unionKey;
	}

	/** See {@link #occupationCode} */
	@Column(name = "Occupation_Code", length = 10)
	public String getOccupationCode() {
		return occupationCode;
	}
	/** See {@link #occupationCode} */
	public void setOccupationCode(String occupationCode) {
		this.occupationCode = occupationCode;
	}

	/**See {@link #lsOccCode}. */
	@Column(name = "Ls_Occ_Code", length = 10)
	public String getLsOccCode() {
		return lsOccCode;
	}
	/**See {@link #lsOccCode}. */
	public void setLsOccCode(String lSoccCode) {
		lsOccCode = lSoccCode;
	}

	/**See {@link #contractCode}. */
	@Column(name = "Occ_Rule_Key", length = 20)
	public String getContractCode() {
		return contractCode;
	}
	/**See {@link #contractCode}. */
	public void setContractCode(String occRuleKey) {
		contractCode = occRuleKey;
	}

	/** See {@link #department}. *//*
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Dept_Id")
	public Department getDepartment() {
		return department;
	}
	*//** See {@link #department}. *//*
	public void setDepartment(Department department) {
		this.department = department;
	}*/

	/** See {@link #deptName}. *//*
	@Column(name = "Dept_Name", length = 50)
	public String getDeptName() {
		return deptName;
	}
	*//** See {@link #deptName}. *//*
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}*/

	/** See {@link #jobClass} */
	@Column(name = "Job_Class", length = 100)
	public String getJobClass() {
		return jobClass;
	}
	/** See {@link #jobClass} */
	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	/**See {@link #contractSchedule}. */
	@Column(name = "Contract_Schedule", length = 10)
	public String getContractSchedule() {
		return contractSchedule;
	}
	/**See {@link #contractSchedule}. */
	public void setContractSchedule(String contractSchedule) {
		this.contractSchedule = contractSchedule;
	}

	/** See {@link #scheduleCode} */
	@Column(name = "Schedule_Code", length = 10)
	public String getScheduleCode() {
		return scheduleCode;
	}
	/** See {@link #scheduleCode} */
	public void setScheduleCode(String scheduleLtr) {
		scheduleCode = scheduleLtr;
	}

	/** See {@link #additionalStaff}. */
	@Column(name = "Additional_Staff", nullable = false)
	public boolean getAdditionalStaff() {
		return additionalStaff;
	}
	/** See {@link #additionalStaff}. */
	public void setAdditionalStaff(boolean additionalStaff) {
		this.additionalStaff = additionalStaff;
	}

	/** See {@link #nearbyHire}. */
	@Column(name = "Nearby_Hire", nullable = false)
	public boolean getNearbyHire() {
		return nearbyHire;
	}
	/** See {@link #nearbyHire}. */
	public void setNearbyHire(boolean nearbyHire) {
		this.nearbyHire = nearbyHire;
	}

	/** See {@link #overtimeRule}. */
	/*@Column(name = "Overtime_Rule", length = 10)
	public String getOvertimeRule() {
		return overtimeRule;
	}
	*//** See {@link #overtimeRule}. *//*
	public void setOvertimeRule(String overtimeRule) {
		this.overtimeRule = overtimeRule;
	}*/

	/** See {@link #loanOutCorpName} */
	@Column(name = "Company_Name", length = 35)
	public String getLoanOutCorpName() {
		return loanOutCorpName;
	}
	/** See {@link #loanOutCorpName} */
	public void setLoanOutCorpName(String companyName) {
		loanOutCorpName = companyName;
	}

	/**See {@link #loanOutAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Loan_Out_Address_Id")
	public Address getLoanOutAddress() {
		return loanOutAddress;
	}
	/**See {@link #loanOutAddress}. */
	public void setLoanOutAddress(Address loanOutAddress) {
		this.loanOutAddress = loanOutAddress;
	}

	/**See {@link #loanOutMailingAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Loan_Out_Mailing_Address_Id")
	public Address getLoanOutMailingAddress() {
		return loanOutMailingAddress;
	}
	/**See {@link #loanOutMailingAddress}. */
	public void setLoanOutMailingAddress(Address loanOutMailingAddress) {
		this.loanOutMailingAddress = loanOutMailingAddress;
	}

	/**See {@link #loanOutPhone}. */
	@Column(name = "Loan_Out_Phone", length = 25)
	public String getLoanOutPhone() {
		return loanOutPhone;
	}
	/**See {@link #loanOutPhone}. */
	public void setLoanOutPhone(String loanOutPhone) {
		this.loanOutPhone = loanOutPhone;
	}

	/**See {@link #incorporationState}. */
	@Column(name = "Incorporation_State", length = 50)
	public String getIncorporationState() {
		return incorporationState;
	}
	/**See {@link #incorporationState}. */
	public void setIncorporationState(String incorporationState) {
		this.incorporationState = incorporationState;
	}

	/**See {@link #incorporationDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Incorporation_Date", length = 10)
	public Date getIncorporationDate() {
		return incorporationDate;
	}
	/**See {@link #incorporationDate}. */
	public void setIncorporationDate(Date incorporationDate) {
		this.incorporationDate = incorporationDate;
	}

	/** See {@link #federalTaxId} */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Federal_Tax_ID", nullable = true, length = 1000)
	public String getFederalTaxId() {
		return federalTaxId;
	}
	/** See {@link #federalTaxId} */
	public void setFederalTaxId(String federalTaxId) {
		this.federalTaxId = federalTaxId;
	}

	@Transient
	@JsonIgnore
	public String getFederalTaxIdFmtd() {
		String str = getFederalTaxId();
		if (str != null && str.length() == 9) {
			str = str.substring(0,2) + "-" + str.substring(2);
		}
		return str;
	}

	/** See {@link #stateTaxId}. */
	@Column(name = "State_Tax_ID", length = 12)
	public String getStateTaxId() {
		return stateTaxId;
	}
	/** See {@link #stateTaxId}. */
	public void setStateTaxId(String stateTaxId) {
		this.stateTaxId = stateTaxId;
	}

	/**See {@link #loanOutQualifiedCa}. */
	@Column(name = "Loan_Out_Qualified_CA", nullable = false)
	public boolean getLoanOutQualifiedCa() {
		return loanOutQualifiedCa;
	}
	/**See {@link #loanOutQualifiedCa}. */
	public void setLoanOutQualifiedCa(boolean loanOutQualifiedCa) {
		this.loanOutQualifiedCa = loanOutQualifiedCa;
	}

	/**See {@link #loanOutQualifiedNy}. */
	@Column(name = "Loan_Out_Qualified_NY", nullable = false)
	public boolean getLoanOutQualifiedNy() {
		return loanOutQualifiedNy;
	}
	/**See {@link #loanOutQualifiedNy}. */
	public void setLoanOutQualifiedNy(boolean loanOutQualifiedNy) {
		this.loanOutQualifiedNy = loanOutQualifiedNy;
	}

	/**See {@link #loanOutQualifiedStates}. */
	@Column(name = "Loan_Out_Qualified_States", length = 200)
	public String getLoanOutQualifiedStates() {
		return loanOutQualifiedStates;
	}
	/**See {@link #loanOutQualifiedStates}. */
	public void setLoanOutQualifiedStates(String loanOutQualifiedStates) {
		this.loanOutQualifiedStates = loanOutQualifiedStates;
	}

	/** See {@link #ethnicCode} */
	@Column(name = "Ethnic_Code", length = 5)
	public String getEthnicCode() {
		return ethnicCode;
	}
	/** See {@link #ethnicCode} */
	public void setEthnicCode(String ethnicCode) {
		this.ethnicCode = ethnicCode;
	}

	/** See {@link #citizenStatus} */
	@Column(name = "Citizen_Status", length = 1)
	public String getCitizenStatus() {
		return citizenStatus;
	}
	/** See {@link #citizenStatus} */
	public void setCitizenStatus(String citizenStatus) {
		this.citizenStatus = citizenStatus;
	}

	/** See {@link #stateOfResidence} */
	@Column(name = "State_of_Residence", length = 50)
	public String getStateOfResidence() {
		return stateOfResidence;
	}
	/** See {@link #stateOfResidence} */
	public void setStateOfResidence(String stateOfResidence) {
		this.stateOfResidence = stateOfResidence;
	}

	/** See {@link #workCity}. */
	@Column(name = "Work_Location", length = 100)
	public String getWorkCity() {
		return workCity;
	}
	/** See {@link #workCity}. */
	public void setWorkCity(String workLocation) {
		workCity = workLocation;
	}

	/**See {@link #workState}. */
	@Column(name = "Work_State", length = 50)
	public String getWorkState() {
		return workState;
	}
	/**See {@link #workState}. */
	public void setWorkState(String workState) {
		this.workState = workState;
	}

	/** See {@link #workZip}. */
	@Column(name = "Work_Zip", length = 10)
	public String getWorkZip() {
		return workZip;
	}
	/** See {@link #workZip}. */
	public void setWorkZip(String workZip) {
		this.workZip = workZip;
	}

	/** See {@link #workCountry}. */
	@Column(name = "Work_County", length = 100)
	public String getWorkCountry() {
		return workCountry;
	}
	/** See {@link #workCountry}. */
	public void setWorkCountry(String workCounty) {
		this.workCountry = workCounty;
	}

	/** See {@link #minor} */
	@Column(name = "Minor", nullable = false)
	public boolean getMinor() {
		return minor;
	}
	/** See {@link #minor} */
	public void setMinor(boolean minor) {
		this.minor = minor;
	}

	/** See {@link #agentRep} */
	@Column(name = "Agent_Rep", nullable = false)
	public boolean getAgentRep() {
		return agentRep;
	}
	/** See {@link #agentRep} */
	public void setAgentRep(boolean agentRep) {
		this.agentRep = agentRep;
	}

	/** See {@link #agencyName} */
	@Column(name = "Agency_Name", length = 35)
	public String getAgencyName() {
		return agencyName;
	}
	/** See {@link #agencyName} */
	public void setAgencyName(String companyName) {
		agencyName = companyName;
	}

	/**See {@link #agencyAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Agency_Address_Id")
	public Address getAgencyAddress() {
		return agencyAddress;
	}
	/**See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	/**See {@link #allowWorked}. */
	@Column(name = "Allow_Worked", nullable = false)
	public boolean getAllowWorked() {
		return allowWorked;
	}
	/**See {@link #allowWorked}. */
	public void setAllowWorked(boolean allowWorked) {
		this.allowWorked = allowWorked;
	}

	/** See {@link #pay6th7thDay}. */
	@Column(name = "Pay_6th_7th_Day", nullable = false)
	public boolean getPay6th7thDay() {
		return pay6th7thDay;
	}
	/** See {@link #pay6th7thDay}. */
	public void setPay6th7thDay(boolean pay6th7thDay) {
		this.pay6th7thDay = pay6th7thDay;
	}

	/**See {@link #rateType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Rate_Type", nullable = false)
	public EmployeeRateType getRateType() {
		return rateType;
	}
	/**See {@link #rateType}. */
	public void setRateType(EmployeeRateType rateType) {
		this.rateType = rateType;
	}

	/** See {@link #underContract}. */
	/*@Column(name = "Under_Contract", nullable = false)
	public boolean getUnderContract() {
		return underContract;
	}
	*//** See {@link #underContract}. *//*
	public void setUnderContract(boolean underContract) {
		this.underContract = underContract;
	}*/

	/** See {@link #useXfdf}. */
	@Column(name = "Use_Xfdf")
	public boolean getUseXfdf() {
		return useXfdf;
	}

	/** See {@link #useXfdf}. */
	public void setUseXfdf(boolean useXfdf) {
		this.useXfdf = useXfdf;
	}

	/** See {@link #skipDeptApproval}. */
	@Column(name = "Skip_Dept_Approval", nullable = false)
	public boolean getSkipDeptApproval() {
		return skipDeptApproval;
	}
	/** See {@link #skipDeptApproval}. */
	public void setSkipDeptApproval(boolean skipDeptApproval) {
		this.skipDeptApproval = skipDeptApproval;
	}

	/**See {@link #useStudioOrLoc}. */
	@Column(name = "Use_Studio_or_Loc", nullable = false, length = 1)
	public String getUseStudioOrLoc() {
		return useStudioOrLoc;
	}
	/**See {@link #useStudioOrLoc}. */
	public void setUseStudioOrLoc(String useStudioOrLoc) {
		this.useStudioOrLoc = useStudioOrLoc;
	}

	/**
	 * @return True if the Studio/Location rate selection field is set to
	 *         Location (Distant).
	 */
	@JsonIgnore
	@Transient
	public boolean isDistantRate() {
		return getUseStudioOrLoc().equals(USE_LOCATION_RATE);
	}

	/**
	 * @return True if the Studio/Location rate selection field is set to
	 *         Studio.
	 */
	@JsonIgnore
	@Transient
	public boolean isStudioRate() {
		return getUseStudioOrLoc().equals(USE_STUDIO_RATE);
	}

	/** This StartForm has pre-production rate information. LS-1346
	 * @see #startRates */
	@Transient
	public boolean getHasPrepRates() {
		if (getStartRates() != null && getStartRates() == StartRatesType.RATES_PREP) {
			return true;
		}
		return false;
	}

	/** True if this Payroll Start includes daily "touring" rates. LS-1346
	 * @see #startRates */
	@Transient
	public boolean getHasTourRates() {
		if (getStartRates() != null && getStartRates() == StartRatesType.RATES_TOURS) {
			return true;
		}
		return false;
	}

	/**See {@link #startRates}. LS-1346 */
	@Enumerated(EnumType.STRING)
	@Column(name = "Start_Rates", nullable = false)
	public StartRatesType getStartRates() {
		return startRates;
	}
	/**See {@link #startRates}. */
	public void setStartRates(StartRatesType startRates) {
		this.startRates = startRates;
	}

	/** See {@link #prod}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Prod_Rate_Id")
	public StartRateSet getProd() {
		return prod;
	}
	/** See {@link #prod}. */
	public void setProd(StartRateSet rates) {
		if (rates == null) {
			prod = new StartRateSet();
		}
		else {
			prod = rates;
		}
	}

	/**See {@link #prep}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Prep_Rate_Id")
	public StartRateSet getPrep() {
		return prep;
	}
	/**See {@link #prep}. */
	public void setPrep(StartRateSet rates) {
		if (rates == null && getHasPrepRates()) {
			prep = new StartRateSet();
		}
		else {
			prep = rates;
		}
	}

	/** See {@link #overnight}. */
	@Column(name = "Overnight", precision = 9, scale = 4)
	public BigDecimal getOvernight() {
		return overnight;
	}
	/** See {@link #overnight}. */
	public void setOvernight(BigDecimal overnight) {
		this.overnight = overnight;
	}

	/**See {@link #boxRental}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 		column = @Column(name = "Box_Rental_Studio", precision = 7, scale = 2)),
		@AttributeOverride(name="loc", 			column = @Column(name = "Box_Rental_Loc", precision = 7, scale = 2)),
		@AttributeOverride(name="paymentCap",	column = @Column(name = "Box_Rental_Cap", precision = 7, scale = 2)),
		@AttributeOverride(name="sepCheck",		column = @Column(name = "Box_Rental_Sep_Check", nullable = false)),
		@AttributeOverride(name="weekly",		column = @Column(name = "Box_Rental_Weekly", nullable = false)),
		@AttributeOverride(name="taxable",		column = @Column(name = "Box_Rental_Taxable", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Box_Rental_Acct_Loc", length = 10)),
		@AttributeOverride(name="major", 		column = @Column(name = "Box_Rental_Acct_Major", length = 10)),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Box_Rental_Acct_Dtl", length = 10)),
		@AttributeOverride(name="sub", 			column = @Column(name = "Box_Rental_Acct_Sub", length = 10)),
		@AttributeOverride(name="set", 			column = @Column(name = "Box_Rental_Acct_Set", length = 10)),
		@AttributeOverride(name="free", 		column = @Column(name = "Box_Rental_Acct_Free", length = 10)),
		@AttributeOverride(name="free2", 		column = @Column(name = "Box_Rental_Acct_Free2", length = 10)),
	} )
	public AllowanceCap getBoxRental() {
		if (boxRental == null) {
			boxRental = new AllowanceCap();
		}
		return boxRental;
	}
	/**See {@link #boxRental}. */
	public void setBoxRental(AllowanceCap boxRental) {
		this.boxRental = boxRental;
	}

	/**See {@link #carAllow}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 		column = @Column(name = "Car_Allow_Studio", precision = 7, scale = 2)),
		@AttributeOverride(name="loc", 			column = @Column(name = "Car_Allow_Loc", precision = 7, scale = 2)),
		@AttributeOverride(name="paymentCap",	column = @Column(name = "Car_Allow_Cap", precision = 7, scale = 2)),
		@AttributeOverride(name="sepCheck",		column = @Column(name = "Car_Allow_Sep_Check", nullable = false)),
		@AttributeOverride(name="weekly",		column = @Column(name = "Car_Allow_Weekly", nullable = false)),
		@AttributeOverride(name="taxable",		column = @Column(name = "Car_Allow_Taxable", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Car_Allow_Acct_Loc", length = 10)),
		@AttributeOverride(name="major", 		column = @Column(name = "Car_Allow_Acct_Major", length = 10)),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Car_Allow_Acct_Dtl", length = 10)),
		@AttributeOverride(name="sub", 			column = @Column(name = "Car_Allow_Acct_Sub", length = 10)),
		@AttributeOverride(name="set", 			column = @Column(name = "Car_Allow_Acct_Set", length = 10)),
		@AttributeOverride(name="free", 		column = @Column(name = "Car_Allow_Acct_Free", length = 10)),
		@AttributeOverride(name="free2", 		column = @Column(name = "Car_Allow_Acct_Free2", length = 10)),
	} )
	public AllowanceCap getCarAllow() {
		if (carAllow == null) {
			carAllow = new AllowanceCap();
		}
		return carAllow;
	}
	/**See {@link #carAllow}. */
	public void setCarAllow(AllowanceCap carAllow) {
		this.carAllow = carAllow;
	}

	/**See {@link #mealAllow}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 		column = @Column(name = "Meal_Allow_Studio", precision = 7, scale = 2) ),
		@AttributeOverride(name="loc", 			column = @Column(name = "Meal_Allow_Loc", precision = 7, scale = 2) ),
		@AttributeOverride(name="sepCheck", 	column = @Column(name = "Meal_Allow_Sep_Check", nullable = false) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Meal_Allow_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Meal_Allow_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Meal_Allow_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Meal_Allow_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Meal_Allow_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Meal_Allow_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Meal_Allow_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Meal_Allow_Acct_Free2", length = 10) ),
	} )
	public AllowanceCheck getMealAllow() {
		if (mealAllow == null) {
			mealAllow = new AllowanceCheck();
		}
		return mealAllow;
	}
	/**See {@link #mealAllow}. */
	public void setMealAllow(AllowanceCheck mealAllow) {
		this.mealAllow = mealAllow;
	}

	/**See {@link #mealPenalty}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Meal_Penalty_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 	column = @Column(name = "Meal_Penalty_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Meal_Penalty_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Meal_Penalty_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 		column = @Column(name = "Meal_Penalty_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 	column = @Column(name = "Meal_Penalty_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Meal_Penalty_Acct_Free2", length = 10) ),
	} )
	public AccountCodes getMealPenalty() {
		if (mealPenalty == null) {
			mealPenalty = new AccountCodes();
		}
		return mealPenalty;
	}
	/**See {@link #mealPenalty}. */
	public void setMealPenalty(AccountCodes mealPenalty) {
		this.mealPenalty = mealPenalty;
	}

	/**See {@link #perdiemTx}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="amt", 			column = @Column(name = "Perdiem_Tx_Amt", precision = 7, scale=2) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Perdiem_Tx_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Perdiem_Tx_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Perdiem_Tx_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Perdiem_Tx_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Perdiem_Tx_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Perdiem_Tx_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Perdiem_Tx_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Perdiem_Tx_Acct_Free2", length = 10) ),
	} )
	public PerDiem getPerdiemTx() {
		if (perdiemTx == null) {
			perdiemTx = new PerDiem();
		}
		return perdiemTx;
	}
	/**See {@link #perdiemTx}. */
	public void setPerdiemTx(PerDiem perdiemTx) {
		this.perdiemTx = perdiemTx;
	}

	/**See {@link #perdiemNtx}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="amt", 			column = @Column(name = "Perdiem_Ntx_Amt", precision = 7, scale=2) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Perdiem_Ntx_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Perdiem_Ntx_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Perdiem_Ntx_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Perdiem_Ntx_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Perdiem_Ntx_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Perdiem_Ntx_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Perdiem_Ntx_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Perdiem_Ntx_Acct_Free2", length = 10) ),
	} )
	public PerDiem getPerdiemNtx() {
		if (perdiemNtx == null) {
			perdiemNtx = new PerDiem();
		}
		return perdiemNtx;
	}
	/**See {@link #perdiemNtx}. */
	public void setPerdiemNtx(PerDiem perdiemNtx) {
		this.perdiemNtx = perdiemNtx;
	}

	/**See {@link #perdiemAdv}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="amt", 			column = @Column(name = "Perdiem_Adv_Amt", precision = 7, scale=2) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Perdiem_Adv_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Perdiem_Adv_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Perdiem_Adv_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Perdiem_Adv_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Perdiem_Adv_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Perdiem_Adv_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Perdiem_Adv_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Perdiem_Adv_Acct_Free2", length = 10) ),
	} )
	public PerDiem getPerdiemAdv() {
		if (perdiemAdv == null) {
			perdiemAdv = new PerDiem();
		}
		return perdiemAdv;
	}
	/**See {@link #perdiemAdv}. */
	public void setPerdiemAdv(PerDiem perdiemAdv) {
		this.perdiemAdv = perdiemAdv;
	}

	/**See {@link #mealMoney}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 		column = @Column(name = "Meal_Money_Amt_Studio", precision = 7, scale=2) ),
		@AttributeOverride(name="loc", 			column = @Column(name = "Meal_Money_Amt_Loc", precision = 7, scale=2) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Meal_Money_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Meal_Money_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Meal_Money_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Meal_Money_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Meal_Money_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Meal_Money_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Meal_Money_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Meal_Money_Acct_Free2", length = 10) ),
	} )
	public Allowance getMealMoney() {
		if (mealMoney == null) {
			mealMoney = new Allowance();
		}
		return mealMoney;
	}
	/**See {@link #mealMoney}. */
	public void setMealMoney(Allowance mealMoney) {
		this.mealMoney = mealMoney;
	}

	/**See {@link #mealMoneyAdv}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 		column = @Column(name = "Meal_Money_Adv_Amt_Studio", precision = 7, scale=2) ),
		@AttributeOverride(name="loc", 			column = @Column(name = "Meal_Money_Adv_Amt_Loc", precision = 7, scale=2) ),
		@AttributeOverride(name="weekly",		column = @Column(name = "Meal_Money_Adv_Weekly", nullable = false)),
		@AttributeOverride(name="aloc", 		column = @Column(name = "Meal_Money_Adv_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 		column = @Column(name = "Meal_Money_Adv_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 			column = @Column(name = "Meal_Money_Adv_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 			column = @Column(name = "Meal_Money_Adv_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 			column = @Column(name = "Meal_Money_Adv_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 		column = @Column(name = "Meal_Money_Adv_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 		column = @Column(name = "Meal_Money_Adv_Acct_Free2", length = 10) ),
	} )
	public Allowance getMealMoneyAdv() {
		if (mealMoneyAdv == null) {
			mealMoneyAdv = new Allowance();
		}
		return mealMoneyAdv;
	}
	/**See {@link #mealMoneyAdv}. */
	public void setMealMoneyAdv(Allowance mealMoneyAdv) {
		this.mealMoneyAdv = mealMoneyAdv;
	}

	/**See {@link #fringe}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Fringe_Acct_Loc", length = 10) ),
		@AttributeOverride(name="major", 	column = @Column(name = "Fringe_Acct_Major", length = 10) ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Fringe_Acct_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Fringe_Acct_Sub", length = 10) ),
		@AttributeOverride(name="set", 		column = @Column(name = "Fringe_Acct_Set", length = 10) ),
		@AttributeOverride(name="free", 	column = @Column(name = "Fringe_Acct_Free", length = 10) ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Fringe_Acct_Free2", length = 10) ),
	} )
	public AccountCodes getFringe() {
		if (fringe == null) {
			fringe = new AccountCodes();
		}
		return fringe;
	}
	/**See {@link #fringe}. */
	public void setFringe(AccountCodes fringe) {
		this.fringe = fringe;
	}

	/** See {@link #mileageRate}. */
	@Column(name = "Mileage_Rate", precision = 4, scale = 3)
	public BigDecimal getMileageRate() {
		return mileageRate;
	}
	/** See {@link #mileageRate}. */
	public void setMileageRate(BigDecimal mileageRate) {
		this.mileageRate = mileageRate;
	}

	/** See {@link #screenCreditRole} */
	@Column(name = "Screen_Credit_Role", length = 50)
	public String getScreenCreditRole() {
		return screenCreditRole;
	}
	/** See {@link #screenCreditRole} */
	public void setScreenCreditRole(String screenCreditRole) {
		this.screenCreditRole = screenCreditRole;
	}

	/** See {@link #screenCreditName} */
	@Column(name = "Screen_Credit_Name", length = 30)
	public String getScreenCreditName() {
		return screenCreditName;
	}
	/** See {@link #screenCreditName} */
	public void setScreenCreditName(String screenCreditName) {
		this.screenCreditName = screenCreditName;
	}

	/** See {@link #industryExpRosterConf} */
	@Column(name = "Industry_Exp_Roster_Conf", nullable = false)
	public boolean getIndustryExpRosterConf() {
		return industryExpRosterConf;
	}
	/** See {@link #industryExpRosterConf} */
	public void setIndustryExpRosterConf(boolean industryExpRosterConf) {
		this.industryExpRosterConf = industryExpRosterConf;
	}

	/** See {@link #emergencyName} */
	@Column(name = "Emergency_Name", length = 30)
	public String getEmergencyName() {
		return emergencyName;
	}
	/** See {@link #emergencyName} */
	public void setEmergencyName(String emergencyName) {
		this.emergencyName = emergencyName;
	}

	/** See {@link #emergencyPhone} */
	@Column(name = "Emergency_Phone", length = 25)
	public String getEmergencyPhone() {
		return emergencyPhone;
	}
	/** See {@link #emergencyPhone} */
	public void setEmergencyPhone(String emergencyPhone) {
		this.emergencyPhone = emergencyPhone;
	}

	/** See {@link #emergencyRelation} */
	@Column(name = "Emergency_Relation", length = 30)
	public String getEmergencyRelation() {
		return emergencyRelation;
	}
	/** See {@link #emergencyRelation} */
	public void setEmergencyRelation(String emergencyRelation) {
		this.emergencyRelation = emergencyRelation;
	}

	/** See {@link #medicalConditions} */
	@Column(name = "Medical_Conditions", length = 200)
	public String getMedicalConditions() {
		return medicalConditions;
	}
	/** See {@link #medicalConditions} */
	public void setMedicalConditions(String medicalConditions) {
		this.medicalConditions = medicalConditions;
	}

	/** See {@link #events}. */
	/*@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "startForm")
	@OrderBy("date")
	public List<StartFormEvent> getEvents() {
		return events;
	}
	*//** See {@link #events}. *//*
	public void setEvents(List<StartFormEvent> evs) {
		events = evs;
	}*/

	/** See {@link #employment}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Employment_Id")
	public Employment getEmployment() {
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/** See {@link #mailingAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Mailing_Address_Id")
	public Address getMailingAddress() {
		return mailingAddress;
	}
	/** See {@link #mailingAddress}. */
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	/** See {@link #permAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Permanent_Address_Id")
	public Address getPermAddress() {
		return permAddress;
	}
	/** See {@link #permAddress}. */
	public void setPermAddress(Address permAddress) {
		this.permAddress = permAddress;
	}

	/**
	 * @param dt The DayType of interest
	 * @return The daily rate for the given DayType, which should be one of the
	 *         "touring" Day types. Returns zero if the DayType is not one of
	 *         the "touring" ones, or if the selected rate has not been set.
	 */
	@Transient
	public BigDecimal getToursRate(DayType dt) {
		BigDecimal rate = null;
		switch (dt) {
			case TPR:
				rate = getToursPrepRate();
				break;
			case TSH:
				rate = getToursShowRate();
				break;
			case TPO:
				rate = getToursPostRate();
				break;
			case TDO:
				rate = getToursDownRate();
				break;
			case TTR:
				rate = getToursTravelRate();
				break;
			case HOW:
				rate = getToursHomeWorkRate();
				break;
			case HOO:
				rate = getToursHomeOffRate();
				break;
			case NONE :
				rate = BigDecimal.ZERO;
			default:
		}
		if (rate == null) {
			rate = BigDecimal.ZERO;
		}
		return rate;
	}

	/** See {@link #toursShowRate}. */
	@Column(name = "Tours_Show_Rate", precision = 7, scale = 2)
	public BigDecimal getToursShowRate() {
		return toursShowRate;
	}
	/** See {@link #toursShowRate}. */
	public void setToursShowRate(BigDecimal toursShowRate) {
		this.toursShowRate = toursShowRate;
	}

	/** See {@link #toursPrepRate}. */
	@Column(name = "Tours_Prep_Rate", precision = 7, scale = 2)
	public BigDecimal getToursPrepRate() {
		return toursPrepRate;
	}
	/** See {@link #toursPrepRate}. */
	public void setToursPrepRate(BigDecimal toursPrepRate) {
		this.toursPrepRate = toursPrepRate;
	}

	/** See {@link #toursPostRate}. */
	@Column(name = "Tours_Post_Rate", precision = 7, scale = 2)
	public BigDecimal getToursPostRate() {
		return toursPostRate;
	}
	/** See {@link #toursPostRate}. */
	public void setToursPostRate(BigDecimal toursPostRate) {
		this.toursPostRate = toursPostRate;
	}

	/** See {@link #toursTravelRate}. */
	@Column(name = "Tours_Travel_Rate", precision = 7, scale = 2)
	public BigDecimal getToursTravelRate() {
		return toursTravelRate;
	}
	/** See {@link #toursTravelRate}. */
	public void setToursTravelRate(BigDecimal toursTravelRate) {
		this.toursTravelRate = toursTravelRate;
	}

	/** See {@link #toursDownRate}. */
	@Column(name = "Tours_Down_Rate", precision = 7, scale = 2)
	public BigDecimal getToursDownRate() {
		return toursDownRate;
	}
	/** See {@link #toursDownRate}. */
	public void setToursDownRate(BigDecimal toursDownRate) {
		this.toursDownRate = toursDownRate;
	}
	/** See {@link #toursHomeWorkRate}. */
	@Column(name = "Tours_Home_Work_Rate", precision = 7, scale = 2)
	public BigDecimal getToursHomeWorkRate() {
		return toursHomeWorkRate;
	}
	/** See {@link #toursHomeWorkRate}. */
	public void setToursHomeWorkRate(BigDecimal toursHomeWorkRate) {
		this.toursHomeWorkRate = toursHomeWorkRate;
	}
	/** See {@link #toursHomeOffRate}. */
	@Column(name = "Tours_Home_Off_Rate", precision = 7, scale = 2)
	public BigDecimal getToursHomeOffRate() {
		return toursHomeOffRate;
	}
	/** See {@link #toursHomeOffRate}. */
	public void setToursHomeOffRate(BigDecimal toursHomeOffRate) {
		this.toursHomeOffRate = toursHomeOffRate;
	}

	/**See {@link #toursShowPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursShowPercent() {
		if (toursShowRate != null) {
			toursShowPercent = toursShowRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursShowPercent = BigDecimal.ZERO;
			}
			else {
				toursShowPercent = toursShowPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursShowPercent;
	}
	/**See {@link #toursShowPercent}. */
	public void setToursShowPercent(BigDecimal toursShowPercent) {
		this.toursShowPercent = toursShowPercent;
	}

	/**See {@link #toursPrepPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursPrepPercent() {
		toursPrepPercent = null;
		if (toursShowRate != null && toursPrepRate != null) {
			toursPrepPercent = toursPrepRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursPrepPercent = BigDecimal.ZERO;
			}
			else {
				toursPrepPercent = toursPrepPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursPrepPercent;
	}
	/**See {@link #toursPrepPercent}. */
	public void setToursPrepPercent(BigDecimal toursPrepPercent) {
		this.toursPrepPercent = toursPrepPercent;
	}

	/**See {@link #toursPostPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursPostPercent() {
		toursPostPercent = null;
		if (toursShowRate != null && toursPostRate != null) {
			toursPostPercent = toursPostRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursPostPercent = BigDecimal.ZERO;
			}
			else {
				toursPostPercent = toursPostPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursPostPercent;
	}
	/**See {@link #toursPostPercent}. */
	public void setToursPostPercent(BigDecimal toursPostPercent) {
		this.toursPostPercent = toursPostPercent;
	}

	/**See {@link #toursTravelPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursTravelPercent() {
		toursTravelPercent = null;
		if (toursShowRate != null && toursTravelRate != null) {
			toursTravelPercent = toursTravelRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursTravelPercent = BigDecimal.ZERO;
			}
			else {
				toursTravelPercent = toursTravelPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursTravelPercent;
	}
	/**See {@link #toursTravelPercent}. */
	public void setToursTravelPercent(BigDecimal toursTravelPercent) {
		this.toursTravelPercent = toursTravelPercent;
	}

	/**See {@link #toursDownPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursDownPercent() {
		toursDownPercent = null;
		if (toursShowRate != null && toursDownRate != null) {
			toursDownPercent = toursDownRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursDownPercent = BigDecimal.ZERO;
			}
			else {
				toursDownPercent = toursDownPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursDownPercent;
	}
	/**See {@link #toursDownPercent}. */
	public void setToursDownPercent(BigDecimal toursDownPercent) {
		this.toursDownPercent = toursDownPercent;
	}

	/**See {@link #toursHomeWorkPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursHomeWorkPercent() {
		toursHomeWorkPercent = null;
		if (toursShowRate != null && toursHomeWorkRate != null) {
			toursHomeWorkPercent = toursHomeWorkRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursHomeWorkPercent = BigDecimal.ZERO;
			}
			else {
				toursHomeWorkPercent = toursHomeWorkPercent.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursHomeWorkPercent;
	}

	/**See {@link #toursHomeWorkPercent}. */
	public void setToursHomeWorkPercent(BigDecimal toursHomeWorkPercent) {
		this.toursHomeWorkPercent = toursHomeWorkPercent;
	}

	/**See {@link #toursHomeOffPercent}. */
	@JsonIgnore
	@Transient
	public BigDecimal getToursHomeOffPercent() {
		toursHomeOffRate = null;
		if (toursShowRate != null && toursHomeOffRate != null) {
			toursHomeOffRate = toursHomeOffRate.multiply(new BigDecimal(100));
			if(NumberUtils.compare(toursShowRate, BigDecimal.ZERO) == 0) {
				toursHomeOffRate = BigDecimal.ZERO;
			}
			else {
				toursHomeOffRate = toursHomeOffRate.divide(toursShowRate, 2, RoundingMode.HALF_UP);
			}
		}
		return toursHomeOffRate;
	}

	/**See {@link #toursHomeOffPercent}. */
	public void setToursHomeOffPercent(BigDecimal toursHomeOffRate) {
		this.toursHomeOffRate = toursHomeOffRate;
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

	/**See {@link #checked}. */
	@JsonIgnore
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/**See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * @return The number of guaranteed hours assuming we are using Production
	 *         (not Prep) rates, and using the StartForm's default of Studio vs
	 *         Location rate default setting.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getGuarHours() {
		return getGuarHours(getProd(), isStudioRate());
	}

	/**
	 * Get the appropriate guaranteed hours value.
	 *
	 * @param rates The StartRateSet from which to extract the guaranteed hours.
	 * @param useStudioRates If true, use studio rates, else use Location rates.
	 * @return The guaranteed hours set in the StartForm, in the specified rate
	 *         section (Prod or Prep, Studio or Location).
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getGuarHours(StartRateSet rates, boolean useStudioRates) {
		BigDecimal hours;
		if (useStudioRates) {
			if (getRateType() == EmployeeRateType.WEEKLY) {
				hours = rates.getWeekly().getStudioHrs();
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				hours = rates.getDaily().getStudioHrs();
			}
			else {
				hours = rates.getHourly().getStudioHrs();
			}
		}
		else { // use Location rates
			if (getRateType() == EmployeeRateType.WEEKLY) {
				hours = rates.getWeekly().getLocHrs();
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				hours = rates.getDaily().getLocHrs();
			}
			else {
				hours = rates.getHourly().getLocHrs();
			}
		}
		return hours;
	}

	/**
	 * Set the appropriate guaranteed hours field, based on the Start's
	 * studio-vs-location setting, and employee rate type (hourly, daily, or
	 * weekly).
	 *
	 * @param rates The StartRateSet (prod or prep) to be updated.
	 * @param hours The guaranteed hours value to be stored.
	 */
	public void setGuarHours(StartRateSet rates, BigDecimal hours) {
		if (isStudioRate()) {
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rates.getWeekly().setStudioHrs(hours);
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rates.getDaily().setStudioHrs(hours);
			}
			else {
				rates.getHourly().setStudioHrs(hours);
			}
		}
		else { // use Location (Distant) rates
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rates.getWeekly().setLocHrs(hours);
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rates.getDaily().setLocHrs(hours);
			}
			else {
				rates.getHourly().setLocHrs(hours);
			}
		}
	}

	/**
	 * @return The default rate for production (not prep) for this employee.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getRate() {
		return getRate(getProd());
	}
	public void setRate(BigDecimal rate) {
		setRate(getProd(), rate);
	}

	/**
	 * @return The default rate for either production or prep rates for this employee.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getRate(StartRateSet rates) {
		BigDecimal rate;
		if (isStudioRate()) {
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rate = rates.getWeekly().getStudio();
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rate = rates.getDaily().getStudio();
			}
			else {
				rate = rates.getHourly().getStudio();
			}
		}
		else { // use Location (Distant) rates
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rate = rates.getWeekly().getLoc();
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rate = rates.getDaily().getLoc();
			}
			else {
				rate = rates.getHourly().getLoc();
			}
		}
		return rate;
	}

	public void setRate(StartRateSet rates, BigDecimal rate) {
		if (isStudioRate()) {
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rates.getWeekly().setStudio(rate);
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rates.getDaily().setStudio(rate);
			}
			else {
				rates.getHourly().setStudio(rate);
			}
		}
		else { // use Location (Distant) rates
			if (getRateType() == EmployeeRateType.WEEKLY) {
				rates.getWeekly().setLoc(rate);
			}
			else if (getRateType() == EmployeeRateType.DAILY) {
				rates.getDaily().setLoc(rate);
			}
			else {
				rates.getHourly().setLoc(rate);
			}
		}
	}

	/** See {@link #workZone}. */
	@Enumerated(value = EnumType.STRING)
	@Column(name = "Work_Zone", length = 30)
	public WorkZone getWorkZone() {
		return workZone;
	}

	/** See {@link #workZone}. */
	public void setWorkZone(WorkZone workZone) {
		this.workZone = workZone;
	}

	/**
	 * @return True iff all the fields necessary for the Start to be considered
	 *         "complete" have been filled in with some non-blank data. Note
	 *         that this includes more fields than the minimum required to
	 *         generate a timecard.
	 */
	@JsonIgnore
	@Transient
	public boolean getIsComplete() {
		if (! getHasRequiredFields()) { // missing a required field
			return false;				// ... so it's not complete.
		}
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				StringUtils.isEmpty(getAccountMajor()) ||
				StringUtils.isEmpty(getAccountDtl()) ||
				StringUtils.isEmpty(getAccountSet()) ||
				StringUtils.isEmpty(getUnionLocalNum()) ||
				getGender() == null ||
				StringUtils.isEmpty(getCitizenStatus()) ||
				(getPermAddress() == null ||
					StringUtils.isEmpty(getPermAddress().getAddrLine1()) ||
					StringUtils.isEmpty(getPermAddress().getCity()) ||
					StringUtils.isEmpty(getPermAddress().getState())
				) ||
				StringUtils.isEmpty(getPhone()) ||
				StringUtils.isEmpty(getStateOfResidence());
		if (! missingData) {
			StartRateSet rates = getProd();
			missingData = // if ALL rates are empty, we're missing something
					rates.getHourly().getLoc() == null &&
					rates.getHourly().getStudio() == null &&
					rates.getDaily().getLoc() == null &&
					rates.getDaily().getStudio() == null &&
					rates.getWeekly().getLoc() == null &&
					rates.getWeekly().getStudio() == null;
			if (! missingData) {
				missingData = // Need 3 account fields in either Hourly, Daily, or Weekly line
					(StringUtils.isEmpty(rates.getHourly().getMajor()) ||
						StringUtils.isEmpty(rates.getHourly().getDtl()) ||
						StringUtils.isEmpty(rates.getHourly().getSet())) &&

					(StringUtils.isEmpty(rates.getDaily().getMajor()) ||
						StringUtils.isEmpty(rates.getDaily().getDtl()) ||
						StringUtils.isEmpty(rates.getDaily().getSet())) &&

					(StringUtils.isEmpty(rates.getWeekly().getMajor()) ||
						StringUtils.isEmpty(rates.getWeekly().getDtl()) ||
						StringUtils.isEmpty(rates.getWeekly().getSet()));
			}
		}
		return ! missingData;
	}

	/**
	 * @return True iff all the fields required in a TOURS production have been
	 *         filled in with some non-blank data. This checks for the fields
	 *         required to create a timecard for this StartForm.
	 */
	@JsonIgnore
	@Transient
	public boolean getHasRequiredToursFields() {
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				getHireDate() == null ||
				getWorkStartDate() == null ||
				StringUtils.isEmpty(getFirstName()) ||
				StringUtils.isEmpty(getLastName()) ||
				StringUtils.isEmpty(getSocialSecurity()) ||
				StringUtils.isEmpty(getProdTitle()) ||
				StringUtils.isEmpty(getProdCompany()) ||
				StringUtils.isEmpty(getJobClass());
		return ! missingData;
	}

	/**
	 * @return True iff all the required fields have been filled in with some
	 * non-blank data. This checks for the fields required to create a timecard
	 * for this StartForm.
	 */
	@JsonIgnore
	@Transient
	public boolean getHasRequiredFields() {
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				getHireDate() == null ||
				getWorkStartDate() == null ||
				getPaidAs() == null ||		// LS-3611
				StringUtils.isEmpty(getFirstName()) ||
				StringUtils.isEmpty(getLastName()) ||
				StringUtils.isEmpty(getSocialSecurity()) ||
				StringUtils.isEmpty(getProdTitle()) ||
				StringUtils.isEmpty(getProdCompany()) ||
				StringUtils.isEmpty(getJobClass()) ||
				StringUtils.isEmpty(getWorkState()) ||
				StringUtils.isEmpty(getWorkCity());
		return ! missingData;
	}

	/**
	 * @return True iff enough fields are filled in to allow a timecard to be
	 *         created. As of rev 2.9.5330, a timecard can be created even
	 *         though we don't have an SSN, work-city, or work-state. Note,
	 *         however, that the timecard cannot be submitted until those fields
	 *         are filled in.
	 */
	@JsonIgnore
	@Transient
	public boolean getAllowTimecardCreate() {
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				getWorkStartDate() == null ||
				StringUtils.isEmpty(getFirstName()) ||
				StringUtils.isEmpty(getLastName()) ||
				//StringUtils.isEmpty(getSocialSecurity()) ||
				StringUtils.isEmpty(getProdTitle()) ||
				StringUtils.isEmpty(getProdCompany()) ||
				StringUtils.isEmpty(getJobClass());
				//StringUtils.isEmpty(getWorkState()) ||
				//StringUtils.isEmpty(getWorkCity());
		return ! missingData;
	}

	/**
	 * @return return True if either effectiveEndDate or workEndDate is not null and
	 * earlier than today's date.This checks where the start form has been expired or not.
	 */
	@JsonIgnore
	@Transient
	public boolean getExpired() {
		Date currentDate = new Date();
		if (getEffectiveEndDate()!=null
				|| getWorkEndDate()!=null) {
			if (getEffectiveEndDate()!=null) {
				if (getEffectiveEndDate().before(currentDate)) {
					return true;
				}
			}
			else if (getWorkEndDate()!=null) {
				if (getWorkEndDate().before(currentDate)) {

					return true;
				}
			}
		}
		return false;
	}

	@JsonIgnore
	@Transient
	public AccountCodes getByPayCategory(PayCategory type) {
		AccountCodes ret = null;
		switch(type) {
			case BOX_RENT_NONTAX:
			case BOX_RENT_TAX:
				ret = getBoxRental();
				break;
			case CAR_ALLOWANCE:
			case CAR_TAXABLE:
				ret = getCarAllow();
				break;
			case MEAL_ALLOWANCE:
				ret = getMealAllow();
				break;
			case MEAL_MONEY:
				ret = getMealMoney();
				break;
			case MM_PER_DIEM_ADVANCE:
				ret = getMealMoneyAdv();
				break;
			case MEAL_PENALTY:
				ret = getMealPenalty();
				break;
			case PER_DIEM_NONTAX:
				ret = getPerdiemNtx();
				break;
			case PER_DIEM_TAX:
				ret = getPerdiemTx();
				break;
			case PER_DIEM_ADVANCE:
				ret = getPerdiemAdv();
				break;
			default:
				break;
		}
		return ret;
	}

	@JsonIgnore
	@Transient
	public boolean getWeekly(PayCategory type) {
		boolean ret = false;
		switch(type) {
			case BOX_RENT_NONTAX:
			case BOX_RENT_TAX:
				ret = getBoxRental().getWeekly();
				break;
			case CAR_ALLOWANCE:
			case CAR_TAXABLE:
				ret = getCarAllow().getWeekly();
				break;
			case MEAL_ALLOWANCE:
				ret = getMealAllow().getWeekly();
				break;
			case MEAL_MONEY:
				ret = getMealMoney().getWeekly();
				break;
			case MM_PER_DIEM_ADVANCE:
				ret = getMealMoneyAdv().getWeekly();
				break;
			case PER_DIEM_NONTAX:
				ret = getPerdiemNtx().getWeekly();
				break;
			case PER_DIEM_TAX:
				ret = getPerdiemTx().getWeekly();
				break;
			case PER_DIEM_ADVANCE:
				ret = getPerdiemAdv().getWeekly();
				break;
			case MEAL_PENALTY:
			default:
				break;
		}
		return ret;
	}

	/** See {@link #newRate}. */
	@Transient
	public BigDecimal getNewRate() {
		return newRate;
	}
	/** See {@link #newRate}. */
	public void setNewRate(BigDecimal newRate) {
		this.newRate = newRate;
	}

	/**
	 * Default compareTo - uses database id for equality check, then start
	 * Date, followed by last name, and first name for comparison.
	 *
	 * @param other
	 * @return Standard compare result -1/0/1.
	 */
	@Override
	public int compareTo(StartForm other) {
		int ret = 1;
		if (other != null) {
			if (getId().equals(other.getId())) {
				return 0; // same entity
			}
			ret = - getAnyStartDate().compareTo(other.getAnyStartDate()); // reverse date!
			if (ret == 0) { // Same week-ending date & adjusted; check name
				ret = compareName(other);
				if (ret == 0) { // Same dates, compare occupation
					ret = StringUtils.compare(getJobClass(), other.getJobClass());
					if (ret == 0) {
						// identical fields, sort by id so order will be consistent from one sort to the next
						ret = id.compareTo(other.id);
					}
				}
			}
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
	public int compareTo(StartForm other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 *
	 * @param other The WeeklyTimecard to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(StartForm other, String sortField) {
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
		else if (sortField.equals(SORTKEY_STATUS) ) {
			ret = NumberUtils.compare((getIsComplete()?1:0) + (getHasRequiredFields()?1:0),
					(other.getIsComplete()?1:0) + (other.getHasRequiredFields()?1:0));
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
		else if (sortField.equals(SORTKEY_DEPT)) {
			ret = StringUtils.compare(getEmployment().getDepartment().getName(), other.getEmployment().getDepartment().getName());
		}
		else if (sortField.equals(SORTKEY_EMP_TYPE)) {
			ret = getRateType().compareTo(other.getRateType());
		}
		else if (sortField.equals(SORTKEY_JOB_NAME)) {
			ret = StringUtils.compare(getJobName(), other.getJobName());
		}
		else if (sortField.equals(SORTKEY_JOB_NUMBER)) {
			ret = StringUtils.compare(getJobNumber(), other.getJobNumber());
		}
		else if (sortField.equals(SORTKEY_OCCUPATION)) {
			ret = StringUtils.compare(getJobClass(), other.getJobClass());
		}
		else if (sortField.equals(SORTKEY_OCC_CODE)) {
			ret = StringUtils.compare(getOccupationCode(), other.getOccupationCode());
		}
		else if (sortField.equals(SORTKEY_RATE)) {
			ret = NumberUtils.compare(getRate(), other.getRate());
		}
		else if (sortField.equals(SORTKEY_GUAR_HOURS)) {
			ret = NumberUtils.compare(getGuarHours(), other.getGuarHours());
		}
		else if (sortField.equals(SORTKEY_SSN)) {
			ret = StringUtils.compare(getViewSSNmin(), other.getViewSSNmin());
		}
		else if (sortField.equals(SORTKEY_TERMS)) {
			ret = getUseStudioOrLoc().compareTo(other.getUseStudioOrLoc());
		}
		else if (sortField.equals(SORTKEY_UNION)) {
			ret = StringUtils.compareNumeric(getUnionLocalNum(), other.getUnionLocalNum());
		}

		if (ret == 0) { // this will also get run if sortField == SORTKEY_NAME
			ret = compareName(other);
			if (ret == 0 && ! sortField.equals(SORTKEY_DATE)) { // same name, compare date
				ret = - getAnyStartDate().compareTo(other.getAnyStartDate()); // default descending
			}
			if (ret == 0) { // still the same, compare occupation
					ret = StringUtils.compare(getJobClass(), other.getJobClass());
				}
			}

		return ret;
	}

	/**
	 * Compare the account number fields of two StartForms.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareAccount(StartForm other) {
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
	 * Compare the account number fields of two StartForms.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareBatch(StartForm other) {
		ProductionBatch batch = getEmployment().getProductionBatch();
		ProductionBatch otherBatch = other.getEmployment().getProductionBatch();
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
	 * Compare the starting dates of two StartForms.
	 *
	 * @param other The StartForm to be compared to this one.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareDate(StartForm other) {
		if (other == null) {
			return 1;
		}
		return getAnyStartDate().compareTo(other.getAnyStartDate());
	}

	/**
	 * Compare the lastName and firstName fields between this StartForm and
	 * another one.
	 *
	 * @param other The other timecard.
	 * @return Standard compare result: negative/zero/positive. The names are
	 *         compared in case-sensitive fashion.
	 */
	private int compareName(StartForm other) {
		int ret = StringUtils.compareIgnoreCase(getLastName(), other.getLastName());
		if (ret == 0) {
			ret = StringUtils.compareIgnoreCase(getFirstName(), other.getFirstName());
		}
		return ret;
	}

	/**
	 * Generate the text for an SQL "order by" clause for the given sortCol.
	 *
	 * @param sortCol The column of interest; should be one of the static
	 *            literals defined in this class.
	 * @return A string appropriate for use in an SQL query as the "sort by"
	 *         string appropriate for the given sortCol.
	 */
	public static String getSqlSortKey(String sortCol, boolean ascending) {
		String key = "";
		String order = ascending ? " asc " : " desc ";
		String dateOrder = ascending ? " desc " : " asc "; // date uses reverse sort

		if (sortCol == null) {
			key = "";
		}
		else if (sortCol.equals(SORTKEY_DATE)) {
			key = " start_date "; // use "calculated" field for sorting; see ReportQueries.startFormList
			key += order + ", ";
		}
		else {
			if (sortCol.equals(SORTKEY_NAME)) {
				key = null; // use default
			}
			else if (sortCol.equals(SORTKEY_STATUS)) {
				key = null; // TODO can't sort on transient value in SQL - Start Form report
			}
			else if (sortCol.equals(SORTKEY_ACCT)) {
				key = " acct_major" + order + ", acct_dtl" + order + ", acct_set" + order + ", acct_free";
			}
			else if (sortCol.equals(SORTKEY_ACCT_FREE1)) {
				key = " acct_free";
			}
			else if (sortCol.equals(SORTKEY_ACCT_FREE2)) {
				key = " acct_free2";
			}
			else if (sortCol.equals(SORTKEY_BATCH)) {
				key = " production_batch_id";
			}
			else if (sortCol.equals(SORTKEY_DEPT)) {
				key = " dept_name";
			}
			else if (sortCol.equals(SORTKEY_EMP_TYPE)) {
				key = " rate_type";
			}
			else if (sortCol.equals(SORTKEY_OCCUPATION)) {
				key = " job_class";
			}
			else if (sortCol.equals(SORTKEY_OCC_CODE)) {
				key = " occupation_code";
			}
			else if (sortCol.equals(SORTKEY_RATE)) {
				key = null; // TODO can't sort on transient value in SQL - Start Form report
			}
			else if (sortCol.equals(SORTKEY_GUAR_HOURS)) {
				key = null; // TODO can't sort on transient value in SQL - Start Form report
			}
			else if (sortCol.equals(SORTKEY_SSN)) {
				key = null; // TODO can't sort encrypted field in SQL - Start Form report
			}
			else if (sortCol.equals(SORTKEY_TERMS)) {
				key = " use_studio_or_loc";
			}
			else if (sortCol.equals(SORTKEY_UNION)) {
				key = " union_local_num";
			}
			if (key == null) {
				key = "";
			}
			else {
				key += order + ", ";
			}
		}
		key += " last_name " + order + ", first_name" + order + ", start_date" + dateOrder;
		return key;
	}

	/**
	 * LS-3825 and LS-3915 Added Non-Jasper report printing.
	 */
	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		// Formatting for the rate fields
		String ratePattern = "#,##0.00##";

		try {
			cd = cd.refresh(); // eliminate DAO reference. LS-2737

			fieldValues.put(FormFieldType.PS_PAID_AS.name(), (getPaidAs() != null ? getPaidAs().getLabel() : ""));
			fieldValues.put(FormFieldType.PS_PROD_NAME.name(), getProdCompany());
			fieldValues.put(FormFieldType.PS_CREATION_DATE.name(),format.format(getCreationDate()));
			// Prep/Wrap
			String fieldValue;
			if(getAccount() != null && getAccount().getMajor() != null) {
				fieldValue = getAccount().getMajor();
			}
			else {
				fieldValue = "";
			}
			fieldValues.put(FormFieldType.PS_ACCT_CODE_PREP.name(), fieldValue);
			// Shoot
			if(getAccount() != null && getAccount().getDtl() != null) {
				fieldValue = getAccount().getDtl();
			}
			else {
				fieldValue = "";
			}
			fieldValues.put(FormFieldType.PS_ACCT_CODE_SHOOT.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_DOC_ID.name(),getFormNumber());
			fieldValue = "New";
			if(getFormType().equals("C")) {
				fieldValue = "Changed";
			}
			else if(getFormType().equals("D")) {
				fieldValue = "Deleted";
			}
			fieldValues.put(FormFieldType.PS_FORM_TYPE.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_DESC_GENDER.name(), (getGender() != null ? getGender().getLabel() : ""));

			fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), getFirstName());
			fieldValues.put(FormFieldType.USER_LAST_NAME.name(), getLastName());
	//		fieldValues.put(FormFieldType.USER_MIDDLE_NAME.name(), getMiddleInitial());
			fieldValues.put(FormFieldType.TELEPHONE_NUMBER.name(), getPhone());
			fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), (getSocialSecurity() != null ? getSocialSecurityFmtd() : ""));
			fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), contact.getUser().getEmailAddress());
			fieldValues.put(FormFieldType.PS_WORK_CITY.name(), (getWorkCity() != null ? getWorkCity() : ""));
			fieldValues.put(FormFieldType.PS_WORK_STATE.name(), (getWorkState() != null ? getWorkState() : ""));
			fieldValues.put(FormFieldType.PS_WORK_ZIP.name(), (getWorkZip() != null ? getWorkZip() : ""));
			fieldValues.put(FormFieldType.PS_WORK_COUNTRY.name(), (getWorkCountry() != null ? getWorkCountry() : ""));
			fieldValues.put(FormFieldType.PS_JOB_NAME.name(), (getJobName() != null ? getJobName() : ""));
			fieldValues.put(FormFieldType.PS_JOB_NUMBER.name(), (getJobNumber() != null ? getJobNumber() : ""));
			fieldValues.put(FormFieldType.PS_JOB_CLASS.name(), (getJobClass() != null ? getJobClass() : ""));
			fieldValues.put(FormFieldType.UNION_LOCAL.name(), (getUnionLocalNum() != null ? getUnionLocalNum() : ""));
			fieldValues.put(FormFieldType.PS_RETIREMENT.name(), (getRetirementPlan() != null ? SelectionItemDAO.getInstance().findLabel("RETIRE", getRetirementPlan()) : ""));
			fieldValues.put(FormFieldType.PS_ACA_BASIS.name(), (getEmploymentBasis() != null ? getEmploymentBasis().getLabel() : ""));
			fieldValues.put(FormFieldType.PS_ETHNICITY.name(), (getEthnicCode() != null ? getEthnicCode() : ""));
			fieldValues.put(FormFieldType.AGENCY_NAME.name(), (getAgencyName() != null ? getAgencyName() : ""));

			EmployeeRateType employmentType;
			employmentType = getRateType();
			fieldValues.put(FormFieldType.PS_EXEMPT_TYPE.name(), (employmentType != EmployeeRateType.HOURLY ? employmentType.getDescription() : ""));
			fieldValue = employmentType.getLongName() + " / ";
			if(getUseStudioOrLoc().equals("L")) {
				fieldValue += WorkZone.DL.getShortLabel();
			}
			else if(getUseStudioOrLoc().equals("S")) {
				fieldValue += WorkZone.SL.getShortLabel();
			}
			fieldValues.put(FormFieldType.PS_EMPLOYMENT_TYPE.name(), (fieldValue));
			fieldValues.put(FormFieldType.PS_PAY_6_7_DAY.name(), (getPay6th7thDay() ? "Pay 6th/7th day" : ""));

			// Address fields
			if(getPermAddress() != null) {
				fieldValues.put(FormFieldType.HOME_ADDR_LINE1.name(), (getPermAddress().getAddrLine1() != null ? getPermAddress().getAddrLine1() : ""));
				fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), (getPermAddress().getAddrLine2() != null ? getPermAddress().getAddrLine2() : ""));
				fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), (getPermAddress().getCity() != null ?  getPermAddress().getCity() : ""));
				fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), (getPermAddress().getState() != null ? getPermAddress().getState() : ""));
				fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), (getPermAddress().getZip() != null ? getPermAddress().getZip()  : ""));
			}
			else {
				fieldValues.put(FormFieldType.HOME_ADDR_LINE1.name(), "");
				fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), "");
				fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), "");
				fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), "");

			}
			if(getMailingAddress() != null) {
				fieldValues.put(FormFieldType.MAILING_ADDR_LINE1.name(), (getMailingAddress().getAddrLine1() != null ? getMailingAddress().getAddrLine1() : ""));
				fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), (getMailingAddress().getAddrLine2() != null ? getMailingAddress().getAddrLine2() : ""));
				fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), (getMailingAddress().getCity() != null ?  getMailingAddress().getCity() : ""));
				fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), (getMailingAddress().getState() != null ? getMailingAddress().getState() : ""));
				fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), (getMailingAddress().getZip() != null ? getMailingAddress().getZip()  : ""));
			}
			else {
				fieldValues.put(FormFieldType.MAILING_ADDR_LINE1.name(), "");
				fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), "");
				fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), "");
				fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), "");
			}
			if(getAgencyAddress() != null) {
				fieldValues.put(FormFieldType.AGENCY_ADDR_LINE1.name(), (getAgencyAddress().getAddrLine1() != null ? getAgencyAddress().getAddrLine1() : ""));
				fieldValues.put(FormFieldType.AGENCY_ADDR_LINE2.name(), (getAgencyAddress().getAddrLine2() != null ? getAgencyAddress().getAddrLine2() : ""));
				fieldValues.put(FormFieldType.AGENCY_ADDR_CITY_ONLY.name(), (getAgencyAddress().getCity() != null ?  getAgencyAddress().getCity() : ""));
				fieldValues.put(FormFieldType.AGENCY_ADDR_STATE.name(), (getAgencyAddress().getState() != null ? getAgencyAddress().getState() : ""));
				fieldValues.put(FormFieldType.AGENCY_ADDR_ZIP.name(), (getAgencyAddress().getZip() != null ? getAgencyAddress().getZip()  : ""));
			}
			else {
				fieldValues.put(FormFieldType.AGENCY_ADDR_LINE1.name(), "");
				fieldValues.put(FormFieldType.AGENCY_ADDR_LINE2.name(), "");
				fieldValues.put(FormFieldType.AGENCY_ADDR_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.AGENCY_ADDR_STATE.name(), "");
				fieldValues.put(FormFieldType.AGENCY_ADDR_ZIP.name(), "");
			}
			// Rates and OT
			String rate;
			if(getRateType() == EmployeeRateType.HOURLY ) {
				fieldValues.put(FormFieldType.PS_EXEMPT_RATE.name(), "");
				fieldValues.put(FormFieldType.PS_HOURLY_RATE.name(), patternFormatBigDecimal(ratePattern, getRate(getProd())));

				if(getWorkZone() == WorkZone.ST) {
					rate = bigDecimalFormat(getProd().getX15StudioRate());
				}
				else {
					rate = bigDecimalFormat(getProd().getX15LocRate());
				}
				fieldValues.put(FormFieldType.PS_OVERTIME_RATE_1.name(), rate);

				if(getWorkZone() == WorkZone.ST) {
					rate = bigDecimalFormat(getProd().getX20StudioRate());
				}
				else {
					rate = bigDecimalFormat(getProd().getX20LocRate());
				}
				fieldValues.put(FormFieldType.PS_OVERTIME_RATE_2.name(), rate);
				fieldValues.put(FormFieldType.PS_GUAR_HOURS.name(), bigDecimalFormat(getGuarHours()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_1.name(), bigDecimalFormat(getProd().getOt1AfterHours()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_2.name(), bigDecimalFormat(getProd().getOt2AfterHours()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_3.name(), bigDecimalFormat(getProd().getOt3AfterHours()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_1.name(), patternFormatBigDecimal(ratePattern, getProd().getOt1Rate()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_2.name(), patternFormatBigDecimal(ratePattern, getProd().getOt2Rate()));
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_3.name(), patternFormatBigDecimal(ratePattern, getProd().getOt3Rate()));
				fieldValues.put(FormFieldType.PS_MULTIPLIER_1.name(),  patternFormatBigDecimal("###.0###", getProd().getOt1Multiplier()));
				fieldValues.put(FormFieldType.PS_MULTIPLIER_2.name(),  patternFormatBigDecimal("###.0###", getProd().getOt2Multiplier()));
				fieldValues.put(FormFieldType.PS_MULTIPLIER_3.name(),  patternFormatBigDecimal("###.0###", getProd().getOt3Multiplier()));
			}
			else {
				// Exempt
				fieldValues.put(FormFieldType.PS_EXEMPT_RATE.name(), patternFormatBigDecimal("##,###.00", getRate(getProd())));
				fieldValues.put(FormFieldType.PS_HOURLY_RATE.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_RATE_1.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_GUAR_HOURS.name(),"");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_1.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_2.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_3.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_1.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_OVERTIME_AFTER_RATE_3.name(), "");
				fieldValues.put(FormFieldType.PS_MULTIPLIER_1.name(), "");
				fieldValues.put(FormFieldType.PS_MULTIPLIER_2.name(), "");
				fieldValues.put(FormFieldType.PS_MULTIPLIER_3.name(), "");
			}

			// Prep Rates and OT
			if(getHasPrepRates()) {
				String preGuarHrs = "";
				fieldValue = employmentType.getLongName() + " / ";
				fieldValue += (getUseStudioOrLoc().equals("S") ? WorkZone.SL.getShortLabel() : WorkZone.DL.getShortLabel());
				fieldValues.put(FormFieldType.PS_PREP_EMPLOYMENT_TYPE.name(), (fieldValue));

				if(getRateType() == EmployeeRateType.HOURLY ) {
					employmentType = getRateType();
					fieldValues.put(FormFieldType.PS_PREP_EXEMPT_TYPE.name(), "");
					fieldValue = employmentType.getLongName() + " / ";
					if(getUseStudioOrLoc().equals("L")) {
						preGuarHrs = bigDecimalFormat(getPrep().getHourly().getLocHrs());
					}
					else if(getUseStudioOrLoc().equals("S")) {
						preGuarHrs =  bigDecimalFormat(getPrep().getHourly().getStudioHrs());
					}
					fieldValues.put(FormFieldType.PS_PREP_PAY_6_7_DAY.name(), (getPay6th7thDay() ? "Pay 6th/7th day" : ""));
					fieldValues.put(FormFieldType.PS_PREP_EXEMPT_RATE.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_HOURLY_RATE.name(), patternFormatBigDecimal(ratePattern, getRate(getPrep())));

					if(getWorkZone() == WorkZone.ST) {
						rate = bigDecimalFormat(getPrep().getX15StudioRate());
					}
					else {
						rate = bigDecimalFormat(getPrep().getX15LocRate());
					}
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_1.name(), rate);

					if(getWorkZone() == WorkZone.ST) {
						rate = bigDecimalFormat(getPrep().getX20StudioRate());
					}
					else {
						rate = bigDecimalFormat(getPrep().getX20LocRate());
					}
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_2.name(), rate);
					fieldValues.put(FormFieldType.PS_PREP_GUAR_HOURS.name(), preGuarHrs);
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_1.name(), bigDecimalFormat(getPrep().getOt1AfterHours()));
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_2.name(), bigDecimalFormat(getPrep().getOt2AfterHours()));
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_3.name(), bigDecimalFormat(getPrep().getOt3AfterHours()));
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_1.name(), patternFormatBigDecimal(ratePattern, getPrep().getOt1Rate()));
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_2.name(), patternFormatBigDecimal(ratePattern, getPrep().getOt2Rate()));
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_3.name(), patternFormatBigDecimal(ratePattern, getPrep().getOt3Rate()));
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_1.name(), patternFormatBigDecimal("###.0###", getPrep().getOt1Multiplier()));
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_2.name(), patternFormatBigDecimal("###.0###", getPrep().getOt2Multiplier()));
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_3.name(), patternFormatBigDecimal("###.0###", getPrep().getOt3Multiplier()));
				}
				else {
					// Exempt
					if(getUseStudioOrLoc().equals("L")) {
						if(getRateType() == EmployeeRateType.DAILY) {
							preGuarHrs = bigDecimalFormat(getPrep().getDaily().getLocHrs());
						}
						else {
							preGuarHrs = bigDecimalFormat(getPrep().getWeekly().getLocHrs());
						}
					}
					else if(getUseStudioOrLoc().equals("S")) {
						if(getRateType() == EmployeeRateType.DAILY) {
							preGuarHrs = bigDecimalFormat(getPrep().getDaily().getStudioHrs());
						}
						else {
							preGuarHrs = bigDecimalFormat(getPrep().getWeekly().getStudioHrs());
						}
					}
					fieldValues.put(FormFieldType.PS_PREP_GUAR_HOURS.name(),preGuarHrs);
					fieldValues.put(FormFieldType.PS_PREP_EXEMPT_TYPE.name(), getRateType().getDescription());
					fieldValues.put(FormFieldType.PS_PREP_EXEMPT_RATE.name(), patternFormatBigDecimal("##,###.00", getRate(getPrep())));
					fieldValues.put(FormFieldType.PS_PREP_HOURLY_RATE.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_1.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_2.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_2.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_1.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_2.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_3.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_1.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_2.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_3.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_1.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_2.name(), "");
					fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_3.name(), "");
				}
			}
			else {
				fieldValues.put(FormFieldType.PS_PREP_EXEMPT_RATE.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_HOURLY_RATE.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_1.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_GUAR_HOURS.name(),"");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_1.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_2.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_3.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_1.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_2.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_OVERTIME_AFTER_RATE_3.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_1.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_2.name(), "");
				fieldValues.put(FormFieldType.PS_PREP_MULTIPLIER_3.name(), "");
			}

			// Emergency Info
			fieldValues.put(FormFieldType.EMERGENCY_CONTACT.name(), (getEmergencyName() != null ? getEmergencyName() : ""));
			fieldValues.put(FormFieldType.EMERGENCY_PHONE.name(), (getEmergencyPhone() != null ? StringUtils.formatPhoneNumber(getEmergencyPhone()) : ""));
			fieldValues.put(FormFieldType.EMERGENCY_RELATION.name(), (getEmergencyRelation() != null ? getEmergencyRelation() : ""));

			// Allowances
			// Box Rentals
			fieldValue = "";
			if(getBoxRental().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getBoxRental().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_STUDIO.name(), bigDecimalFormat(getBoxRental().getStudio()));
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_DISTANT.name(), bigDecimalFormat(getBoxRental().getLoc()));
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_MAX.name(), bigDecimalFormat(getBoxRental().getPaymentCap()));
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_ACCT_FREE.name(), (getBoxRental().getFree() != null ? getBoxRental().getFree() : ""));
			fieldValues.put(FormFieldType.PS_BOX_RENTAL_ACCT_MAJOR.name(), (getBoxRental().getMajor() != null ? getBoxRental().getMajor() : ""));
			// Car Allowances
			fieldValue = "";
			if(getCarAllow().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getCarAllow().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_STUDIO.name(), bigDecimalFormat(getCarAllow().getStudio()));
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_DISTANT.name(), bigDecimalFormat(getCarAllow().getLoc()));
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_MAX.name(), bigDecimalFormat(getCarAllow().getPaymentCap()));
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_ACCT_FREE.name(), (getCarAllow().getFree() != null ? getCarAllow().getFree() : ""));
			fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_ACCT_MAJOR.name(), (getCarAllow().getMajor() != null ? getCarAllow().getMajor() : ""));
			// Meal Allowances
			fieldValue = "";
			if(getMealAllow().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getMealAllow().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_STUDIO.name(), bigDecimalFormat(getMealAllow().getStudio()));
			fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_DISTANT.name(), bigDecimalFormat(getMealAllow().getLoc()));
			fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_ACCT_FREE.name(), (getMealAllow().getFree() != null ? getMealAllow().getFree() : ""));
			fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_ACCT_MAJOR.name(), (getMealAllow().getMajor() != null ? getMealAllow().getMajor() : ""));
			// Per Diem Non-Taxable
			fieldValue = "";
			if(getPerdiemNtx().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getPerdiemNtx().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_PER_DIEM_NON_TAX_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_PER_DIEM_NON_TAX_DISTANT.name(), bigDecimalFormat(getPerdiemNtx().getAmt()));
			fieldValues.put(FormFieldType.PS_PER_DIEM_NON_TAX_ACCT_FREE.name(), (getPerdiemNtx().getFree() != null ? getPerdiemNtx().getFree() : ""));
			fieldValues.put(FormFieldType.PS_PER_DIEM_NON_TAX_ACCT_MAJOR.name(), (getPerdiemNtx().getMajor() != null ? getPerdiemNtx().getMajor() : ""));
			// Per Diem Taxable
			fieldValue = "";
			if(getPerdiemTx().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getPerdiemTx().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_PER_DIEM_TAX_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_PER_DIEM_TAX_DISTANT.name(), bigDecimalFormat(getPerdiemTx().getAmt()));
			fieldValues.put(FormFieldType.PS_PER_DIEM_TAX_ACCT_FREE.name(), (getPerdiemTx().getFree() != null ? getPerdiemTx().getFree() : ""));
			fieldValues.put(FormFieldType.PS_PER_DIEM_TAX_ACCT_MAJOR.name(), (getPerdiemTx().getMajor() != null ? getPerdiemTx().getMajor() : ""));
			// Per Diem Advance
			fieldValue = "";
			if(getPerdiemAdv().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getPerdiemAdv().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_PER_DIEM_ADV_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_PER_DIEM_ADV_DISTANT.name(), bigDecimalFormat(getPerdiemAdv().getAmt()));
			fieldValues.put(FormFieldType.PS_PER_DIEM_ADV_ACCT_FREE.name(), (getPerdiemAdv().getFree() != null ? getPerdiemAdv().getFree() : ""));
			fieldValues.put(FormFieldType.PS_PER_DIEM_ADV_ACCT_MAJOR.name(), (getPerdiemAdv().getMajor() != null ? getPerdiemAdv().getMajor() : ""));
			// Meal Money
			fieldValue = "";
			if(getMealMoney().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getMealMoney().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_STUDIO.name(), bigDecimalFormat(getMealMoney().getStudio()));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_DISTANT.name(), bigDecimalFormat(getMealMoney().getLoc()));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ACCT_FREE.name(), (getMealMoney().getFree() != null ? getMealMoney().getFree() : ""));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ACCT_MAJOR.name(), (getMealMoney().getMajor() != null ? getMealMoney().getMajor() : ""));
			// Meal Money Advance
			fieldValue = "";
			if(getMealMoneyAdv().getDaily()) {
				fieldValue = EmployeeRateType.DAILY.getLabel();
			}
			else if(getMealMoneyAdv().getWeekly()) {
				fieldValue = EmployeeRateType.WEEKLY.getLabel();
			}
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ADV_FREQ.name(), fieldValue);
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ADV_STUDIO.name(), bigDecimalFormat(getMealMoneyAdv().getStudio()));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ADV_DISTANT.name(), bigDecimalFormat(getMealMoneyAdv().getLoc()));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ADV_ACCT_FREE.name(), (getMealMoneyAdv().getFree() != null ? getMealMoneyAdv().getFree() : ""));
			fieldValues.put(FormFieldType.PS_MEAL_MONEY_ADV_ACCT_MAJOR.name(), (getMealMoneyAdv().getMajor() != null ? getMealMoneyAdv().getMajor() : ""));
			// Meal Penalty
			fieldValues.put(FormFieldType.PS_MEAL_PENALTY_ACCT_FREE.name(), (getMealPenalty().getFree() != null ? getMealPenalty().getFree() : ""));
			fieldValues.put(FormFieldType.PS_MEAL_PENALTY_ACCT_MAJOR.name(), (getMealPenalty().getMajor() != null ? getMealPenalty().getMajor() : ""));
			// Fringe
			fieldValues.put(FormFieldType.PS_FRINGE_ACCT_FREE.name(), (getFringe().getFree() != null ? getFringe().getFree() : ""));
			fieldValues.put(FormFieldType.PS_FRINGE_ACCT_MAJOR.name(), (getFringe().getMajor() != null ? getFringe().getMajor() : ""));

			// For Cast Memebers
			boolean isCast = (getEmployment().getRole().getDepartment().getId() == Constants.DEPARTMENT_ID_CAST);
			fieldValues.put(FormFieldType.PS_OTHER_FEES_LBL.name(), (isCast ? CAST_OFFICE_FEE_LBL : ""));
			fieldValues.put(FormFieldType.PS_REUSE_LBL.name(), (isCast ? CAST_REUSE_FEE_LBL : ""));
			fieldValues.put(FormFieldType.PS_REUSE_FEE.name(), (isCast ? bigDecimalFormat(getEmpReuse()) : ""));
			fieldValues.put(FormFieldType.PS_ACTOR_AGENT_COMMISION_LBL.name(), (isCast ? CAST_AGENT_COMMISSION_LBL : ""));
			fieldValues.put(FormFieldType.PS_ACTOR_AGENT_COMMISION_AMT.name(), (isCast ? bigDecimalFormat(getEmpAgentCommisssion()) : ""));

			// Checkboxes
			fieldValues.put(FormFieldType.PS_OFF_PRODUCTION.name(), (getOffProduction() ? "Yes" : "Off"));
			fieldValues.put(FormFieldType.PS_AGENT.name(), (getAgentRep() ? "Yes" : "Off"));
			fieldValues.put(FormFieldType.PS_MINOR.name(), (getMinor() ? "Yes" : "Off"));
			fieldValues.put(FormFieldType.PS_SEP_CHECK_BOX.name(), (getBoxRental().getSepCheck() ? "Yes" : "No"));
			fieldValues.put(FormFieldType.PS_SEP_CHECK_CAR.name(), (getCarAllow().getSepCheck() ? "Yes" : "No"));
			fieldValues.put(FormFieldType.PS_SEP_CHECK_MEAL.name(), (getMealAllow().getSepCheck() ? "Yes" : "No"));
			fieldValues.put(FormFieldType.PS_PAY_6_7_CHECK.name(), (getPay6th7thDay()  ?"Yes" : "Off"));

			// Date Fields
			fieldValues.put(FormFieldType.PS_START_DATE_MDY.name(), (getAnyStartDate() != null ? format.format(getAnyStartDate()) : ""));
			fieldValues.put(FormFieldType.PS_HIRE_DATE_MDY.name(), (getHireDate() != null ? format.format(getHireDate()) : ""));
			fieldValues.put(FormFieldType.PS_END_DATE_MDY.name(), (getEarliestEndDate() != null ? format.format(getEarliestEndDate()) : ""));
			fieldValues.put(FormFieldType.PS_EFFECTIVE_FROM_DATE_MDY.name(), (getEffectiveStartDate() != null ? format.format(getEffectiveStartDate()) : ""));
			fieldValues.put(FormFieldType.PS_EFFECTIVE_TO_DATE_MDY.name(), (getEffectiveEndDate() != null ? format.format(getEffectiveEndDate()) : ""));
			fieldValues.put(FormFieldType.PS_DOB_MDY.name(), (getDateOfBirth() != null ? format.format(getDateOfBirth()) : ""));

			// Signature fields
			if (cd.getEmpSignature() != null) {
				fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
						cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			}
			else {
				fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			}
			if (cd.getFinalSignature() != null) {
				// LS-4430 Use the last signature of the employer
				fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
						cd.getFinalSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			}
			else {
				fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			}

			// Loan-Out fields
			if(getPaidAs() == PaidAsType.LO) {
				fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), (getLoanOutCorpName() != null ? getLoanOutCorpName() : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), (getFederalTaxId() != null ? getFederalTaxIdFmtd() : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), (getStateTaxId() != null ?  getStateTaxId() : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), (getIncorporationState() != null ? getIncorporationState() : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), (getIncorporationDate() != null ? format.format(getIncorporationDate())  : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), (getLoanOutQualifiedCa() ? "Yes" : "Off"));
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), (getLoanOutQualifiedNy() ? "Yes" : "Off"));
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), (getLoanOutQualifiedStates() != null ? getLoanOutQualifiedStates() : ""));
				fieldValues.put(FormFieldType.LOAN_OUT_PHONE.name(), (getLoanOutPhone() != null ? StringUtils.formatPhoneNumber(getLoanOutPhone()) : ""));

				fieldValue = getTaxClassification() != null ? getTaxClassification().getLabel() : "";
				if(getTaxClassification() == TaxClassificationType.L && getLlcType() != null) {
					// LS-4430 We should only go through this code if the tax classification is for LLC
					fieldValue += " ";
					if(getLlcType().equalsIgnoreCase("C")) {
						fieldValue += LLC_C_TYPE;
					}
					else {
						fieldValue += LLC_S_TYPE;
					}
				}
				fieldValues.put(FormFieldType.LOAN_OUT_TAX_CLASSIFICATION.name(), fieldValue);

				// Loanout Addresses
				if(getLoanOutAddress() != null) {
					fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), (getLoanOutAddress().getAddrLine1() != null ? getLoanOutAddress().getAddrLine1() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_ADDR_2.name(), (getLoanOutAddress().getAddrLine2() != null ? getLoanOutAddress().getAddrLine2() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), (getLoanOutAddress().getCity() != null ?  getLoanOutAddress().getCity() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), (getLoanOutAddress().getState() != null ? getLoanOutAddress().getState() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), (getLoanOutAddress().getZip() != null ? getLoanOutAddress().getZip()  : ""));
				}
				else {
					fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_ADDR_2.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), "");

				}
				if(getLoanOutMailingAddress() != null) {
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STREET.name(), (getLoanOutMailingAddress().getAddrLine1() != null ? getLoanOutMailingAddress().getAddrLine1() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ADDR_2.name(), (getLoanOutMailingAddress().getAddrLine2() != null ? getLoanOutMailingAddress().getAddrLine2() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_CITY_ONLY.name(), (getLoanOutMailingAddress().getCity() != null ?  getLoanOutMailingAddress().getCity() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STATE.name(), (getLoanOutMailingAddress().getState() != null ? getLoanOutMailingAddress().getState() : ""));
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ZIP.name(), (getLoanOutMailingAddress().getZip() != null ? getLoanOutMailingAddress().getZip()  : ""));
				}
				else {
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STREET.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ADDR_2.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_CITY_ONLY.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STATE.name(), "");
					fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ZIP.name(), "");
				}
			}
			else {
				// If not load-out, clear the fields.
				fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), (""));
				fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), "Off");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), "Off");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_PHONE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_TAX_CLASSIFICATION.name(), "");

				fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_ADDR_2.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STREET.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ADDR_2.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_MAILING_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_MAILING_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_MAILING_ZIP.name(), "");
			}
		}
		catch(Exception ex) {
			EventUtils.logError(ex);
			log.debug(ex);
		}
	}
	/**
	 * Export the fields in this StartForm using the supplied Exporter.
	 * @param ex
	 * @param isTeamPayroll True if the current payroll service is for Team.
	 */
	public void exportFlat(Exporter ex, boolean isTeamPayroll) {
		ex.append(getFormNumber());
		ex.append(getFormType());
		ex.append(getProdCompany());
		ex.append(getJobName());
		ex.append(getJobNumber());
		ex.append(getProdTitle());
		ex.append(getFirstName());
		ex.append(getMiddleName());
		ex.append(getLastName());
		ex.append(getGender() == null ? "" : getGender().name()); // LS-2502
		ex.append(getDateOfBirth());

		Address addr = getMailingAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		addr = getPermAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		ex.append(getPhone());
		ex.append(getSocialSecurity());
		getAccount().exportTabbed(ex);
		ex.append(getCreationDate());
		ex.append(getEffectiveStartDate());
		ex.append(getEffectiveEndDate());
		ex.append(getWorkStartDate());
		ex.append(getWorkEndDate());
		ex.append(getOffProduction());
		ex.append(getHireDate());
		ex.append(getRetirementPlan());
		ex.append(getEmploymentBasis() == null ? "" : getEmploymentBasis().name());
		ex.append(getUnionLocalNum());
		ex.append(getJobClass());
		String expOccCode = getOccupationCode();
		if (StringUtils.isEmpty(expOccCode)) {
			expOccCode = getLsOccCode();
		}
		if (isTeamPayroll) {
			expOccCode = OccupationUtils.mapOccCode(expOccCode);
		}
		ex.append(expOccCode);
		ex.append(getContractSchedule());
		ex.append(getLsOccCode());
		ex.append(getContractCode());
		ex.append(getAdditionalStaff());
		ex.append(getNearbyHire());
		ex.append(getRateType() == null ? "" : getRateType().name());
		ex.append(getLoanOutCorpName());

		addr = getLoanOutAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		ex.append(getLoanOutPhone());
		ex.append(getIncorporationState());
		ex.append(getIncorporationDate());
		ex.append(StringUtils.cleanTaxId(getFederalTaxId()));
		ex.append(getStateTaxId());
		ex.append(getLoanOutQualifiedCa());
		ex.append(getLoanOutQualifiedNy());
		ex.append(getLoanOutQualifiedStates());
		ex.append(getEthnicCode());
		ex.append(getCitizenStatus());
		ex.append(getStateOfResidence());
		ex.append(getWorkCity());
		ex.append(getWorkState());
		ex.append(getWorkCountry());
		ex.append(getWorkZip());
		ex.append(getMinor());
		ex.append(getAgentRep());
		ex.append(getAgencyName());

		addr = getAgencyAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		ex.append(getMileageRate());
		ex.append(getAllowWorked());
		ex.append(getPay6th7thDay());
		ex.append(getSkipDeptApproval());
		ex.append(getUseStudioOrLoc());
		ex.append(getHasPrepRates());
		// NOT IN EXPORT: ex.append(getStartRates() == null ? "" : getStartRates().name()); LS-1346

		getProd().exportTabbed(ex); // Export all production rates

		 // Export all prep rates, or dummy fields
		if (getPrep() == null) {
			(new StartRateSet()).exportTabbed(ex);
		}
		else {
			getPrep().exportTabbed(ex);
		}

		getBoxRental().exportTabbed(ex);
		getCarAllow().exportTabbed(ex);
		getMealAllow().exportTabbed(ex);
		getMealMoney().exportTabbed(ex);
		getMealMoneyAdv().exportTabbed(ex);
		getMealPenalty().exportTabbed(ex);
		getPerdiemTx().exportTabbed(ex);
		getPerdiemNtx().exportTabbed(ex);
		getPerdiemAdv().exportTabbed(ex);
		getFringe().exportTabbed(ex);

		ex.append(getScreenCreditRole());
		ex.append(getScreenCreditName());
		ex.append(getIndustryExpRosterConf());
		ex.append(getEmergencyName());
		ex.append(getEmergencyPhone());
		ex.append(getEmergencyRelation());
		ex.append(getMedicalConditions());

//		ex.append(getPaidAs() == null ? "" : getPaidAs().name()); // LS-2562
//		ex.append(getTaxClassification() == null ? "" : getTaxClassification().name()); // LS-2562

	}

	/**
	 * @return A copy of this object, including separate copies of all the
	 *         included data objects, such as rate information. (This is
	 *         significantly different than the clone() method, which copies
	 *         only the primitive data items, and all objects, such as rates,
	 *         are null in the returned copy.)
	 */
	public StartForm deepCopy() {
		StartForm newDoc;
		try {
			newDoc = clone();
			newDoc.prod = prod.deepCopy();
			if (prep != null) {
				newDoc.prep = prep.deepCopy();
			}
			if (boxRental != null) {
				newDoc.boxRental = boxRental.clone();
			}
			if (carAllow != null) {
				newDoc.carAllow = carAllow.clone();
			}
			if (mealAllow != null) {
				newDoc.mealAllow = mealAllow.clone();
			}
			if (mealPenalty != null) {
				newDoc.mealPenalty = mealPenalty.clone();
			}
			if (perdiemTx != null) {
				newDoc.perdiemTx = perdiemTx.clone();
			}
			if (perdiemNtx != null) {
				newDoc.perdiemNtx = perdiemNtx.clone();
			}
			if (perdiemAdv != null) {
				newDoc.perdiemAdv = perdiemAdv.clone();
			}
			if (mealMoney != null) {
				newDoc.mealMoney = mealMoney.clone();
			}
			if (mealMoneyAdv != null) {
				newDoc.mealMoneyAdv = mealMoneyAdv.clone();
			}
			if (fringe != null) {
				newDoc.fringe = fringe.clone();
			}
			if (loanOutAddress != null) {
				newDoc.setLoanOutAddress(loanOutAddress.clone());
			}
			if (agencyAddress != null) {
				newDoc.setAgencyAddress(agencyAddress.clone());
			}
			if (mailingAddress != null) {
				newDoc.setMailingAddress(mailingAddress.clone());
			}
			if (permAddress != null) {
				newDoc.setPermAddress(permAddress.clone());
			}
		}
		catch (CloneNotSupportedException e) {
			log.error("StartForm clone error: ", e);
			return null;
		}
		return newDoc;
	}

	/**
	 * @return A copy of this object, but only including the primitive data
	 *         items; all embedded object references, primarily the rates, are
	 *         null in the returned copy.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public StartForm clone() {
		StartForm newDoc;
		try {
			newDoc = (StartForm)super.clone();
			newDoc.id = null;
			// don't null newDoc.contact. 2.9.5295
			newDoc.project = null;
			newDoc.formNumber = null;
//			newDoc.projectMemberId = null;
			newDoc.priorFormId = null;
			newDoc.loanOutAddress = null;
			newDoc.loanOutMailingAddress = null;
			newDoc.agencyAddress = null;
			//newDoc.productionBatch = null;
			//newDoc.events = new ArrayList<StartFormEvent>(0);
			newDoc.prod = new StartRateSet();
			newDoc.prep = null;
			newDoc.boxRental = null;
			newDoc.carAllow = null;
			newDoc.mealAllow = null;
			newDoc.mealPenalty = null;
			newDoc.perdiemTx = null;
			newDoc.perdiemNtx = null;
			newDoc.perdiemAdv = null;
			newDoc.mealMoney = null;
			newDoc.mealMoneyAdv = null;
			newDoc.fringe = null;
			newDoc.selected = false;
			newDoc.checked = false;
		}
		catch (CloneNotSupportedException e) {
			log.error("StartForm clone error: ", e);
			return null;
		}
		return newDoc;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getLastName()==null ? "null" : getLastName());
		s += "]";
		return s;
	}

	@Override
	public void trim() {
		formNumber = trim(formNumber);
		prodCompany = trim(prodCompany);
		jobName = trim(jobName);
		jobNumber = trim(jobNumber);
		prodTitle = trim(prodTitle);
		firstName = trim(firstName);
		middleName = trim(middleName);
		lastName = trim(lastName);

		/*addrLine1 = trim(addrLine1);
		addrLine2 = trim(addrLine2);
		city = trim(city);
		zip = trim(zip);
		country = trim(country);*/
		phone = trim(phone);
		socialSecurity = trim(socialSecurity);
		occupationCode = trim(occupationCode);
		lsOccCode = trim(lsOccCode);
		contractCode = trim(contractCode);
		jobClass = trim(jobClass);
		contractSchedule = trim(contractSchedule);
		scheduleCode = trim(scheduleCode);

		loanOutCorpName = trim(loanOutCorpName);
		loanOutAddress.trimIsEmpty();
		loanOutPhone = trim(loanOutPhone);
		incorporationState = trim(incorporationState);
		federalTaxId = trim(federalTaxId);
		stateTaxId = trim(stateTaxId);
		loanOutQualifiedStates = trim(loanOutQualifiedStates);
		ethnicCode = trim(ethnicCode);
		workCity = trim(workCity);
		workZip = trim(workZip);
		workCountry = trim(workCountry);

		agencyName = trim(agencyName);
		agencyAddress.trimIsEmpty();
		useStudioOrLoc = trim(useStudioOrLoc);
		screenCreditRole = trim(screenCreditRole);
		screenCreditName = trim(screenCreditName);
		emergencyName = trim(emergencyName);
		emergencyPhone = trim(emergencyPhone);
		emergencyRelation = trim(emergencyRelation);
		medicalConditions = trim(medicalConditions);
		mailingAddress.trimIsEmpty();
		permAddress.trimIsEmpty();
	}

	/**
	 * @return A Comparator which will sort StartForm`s based on the
	 *         employee name within the associated StartForm.
	 */
	public static Comparator<StartForm> getNameComparator() {
		Comparator<StartForm> comparator = new Comparator<StartForm>() {
			@Override
			public int compare(StartForm c1, StartForm c2) {
				return c1.compareTo(c2, StartForm.SORTKEY_NAME, true);
			}
		};
		return comparator;
	}

	/** See {@link #empReuse}. */
	@Column(name = "Emp_Reuse", precision = 8, scale = 2)
	public BigDecimal getEmpReuse() {
		return empReuse;
	}

	/** See {@link #empReuse}. */
	public void setEmpReuse(BigDecimal empReuse) {
		this.empReuse = empReuse;
	}

	/** See {@link #empAgentCommisssion}. */
	@Column(name = "Emp_Agent_Commission", precision = 5, scale = 2)
	public BigDecimal getEmpAgentCommisssion() {
		return empAgentCommisssion;
	}

	/** See {@link #empAgentCommisssion}. */
	public void setEmpAgentCommisssion(BigDecimal empAgentCommisssion) {
		this.empAgentCommisssion = empAgentCommisssion;
	}

	/** See {@link #paidAs}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Paid_As")
	public PaidAsType getPaidAs() {
		return paidAs;
	}
	/** See {@link #paidAs}. */
	public void setPaidAs(PaidAsType paidAs) {
		this.paidAs = paidAs;
	}

	/** See {@link #taxClassification}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Tax_Classification")
	public TaxClassificationType getTaxClassification() {
		return taxClassification;
	}
	/** See {@link #taxClassification}. */
	public void setTaxClassification(TaxClassificationType taxClassification) {
		this.taxClassification = taxClassification;
	}

	/** See {@link #llcType}. */
	@Column(name = "LLC_Type", length = 1)
	public String getLlcType() {
		return llcType;
	}
	/** See {@link #llcType}. */
	public void setLlcType(String llcType) {
		this.llcType = llcType;
	}

}