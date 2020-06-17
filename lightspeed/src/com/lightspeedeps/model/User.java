//	File Name:	User.java
package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.logging.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.Parameter;
import org.icefaces.ace.util.IceOutputResource;
import org.jasypt.hibernate3.type.EncryptedStringType;

import com.fasterxml.jackson.annotation.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.EncryptUtils;
import com.lightspeedeps.util.common.*;

/**
 * User entity. This holds the "account information" for a registered LightSPEED
 * user. Each User has a unique emailAddress and a unique accountNumber. A
 * person may change the emailAddress assigned to their user account, but the
 * accountNumber should never be changed.
 * <p>
 * Note that when a User is "deleted" through the normal UI, this object is NOT
 * deleted from the database. Instead, the status is changed to deleted, and the
 * emailAddress is made invalid by prefixing it with "@" (at-sign).
 */
@NamedQueries ({
	@NamedQuery (name = User.GET_USER_COUNT_BY_STATUS, query = "select count(u.id) from User u where u.status =:status"),
	@NamedQuery (name = User.GET_ACTIVE_USER_COUNT, query = "select count(u.id) from User u where u.status != 'DELETED'"),
	@NamedQuery (name = User.GET_NON_NULL_SSN, query = "select user from User user where user.socialSecurity is not null"),
})

@TypeDefs(
	{
	@TypeDef(
		// TypeDef for encrypted strings - this TypeDef is shared by all models that use encrypted fields
		name="encryptedString",
		typeClass=EncryptedStringType.class,
		parameters= {
			@Parameter(name="encryptorRegisteredName", value="hibernateStringEncryptor")
			// Registered name is defined in applicationContextPart1.xml
		}
	)
	}
)

@Entity
@Table(name = "user",uniqueConstraints = @UniqueConstraint(columnNames = "Email_Address"))
public class User extends PersistentObject<User> implements Cloneable, Comparable<User> {
	private static final Log log = LogFactory.getLog(User.class);
	private static final long serialVersionUID = -7929316568377447863L;

	/** Encryption key used for password, SSN, FEIN, etc. LS-4572 */
	private final static String ENCRYPT_KEY = "concat('tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq', 0x3f, 'p/m5\n\t\t\t')";
	//                        key without hex coding: "tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t"
	// Note that the "?" in the key must be coded in hex to avoid the ColumnTransformer throwing an error.

	//Named Queries
	public static final String GET_USER_COUNT_BY_STATUS = "getUserCountByStatus";
	public static final String GET_ACTIVE_USER_COUNT = "getActiveUserCount";
	public static final String GET_NON_NULL_SSN = "getNonNullSSN";

	public static final String SORTKEY_NAME = "name";
	private static final String SORTKEY_EMAIL = "email";
	private static final String SORTKEY_STATUS = "status";
	private static final String SORTKEY_ACCOUNT = "account";
	private static final String SORTKEY_FIRST_NAME = "firstname";
	private static final String SORTKEY_LAST_NAME = "lastname";

	// The values for primaryPhoneIndex:
	@SuppressWarnings("unused")
	private static final int PRIMARY_PHONE_OFFICE = 0;
	public static final int PRIMARY_PHONE_CELL = 1;
	@SuppressWarnings("unused")
	private static final int PRIMARY_PHONE_HOME = 2;

	// Fields

	/** Name to display in ? */
	private String displayName;

	/** Unique LightSpeed account number. */
	private String accountNumber = "?";

	/** user name TOCS User migration - add new fields to TTCO database */
	private String userName;

	/** Status of user - pending, registered, etc. */
	private UserStatus status = UserStatus.REGISTERED;

	/** Date this User entry was created. */
	private Date created;

	/** The account number of the creating User.  Will be the same
	 * as 'accountNumber' if the user self-registered.  Will be the
	 * "inviting" user's accountNumber otherwise. */
	private String createdBy;

	/** The 'referred by' code stored in the Session (if any), based
	 * on the user having followed a marketing partner's link to our site. */
	private String referredBy;

	/** The user's unique email address.  This is used as the login
	 * name. */
	private String emailAddress;

	/** Name prefix, e.g., Dr, Ms -- not currently used. */
	private String prefix;

	/** First (given) name. */
	private String firstName;

	/** Middle initial. As of LS-4744, there is an input field on the
	 * My Account page for this, and it is limited to a single character. Prior
	 * to this fix, this field was only updated from the I9 or Minor Trust forms. */
	private String middleName;

	/** Last (family) name. */
	private String lastName;

	/** User's date of birth. */
	private Date birthdate;

	/** True iff the user is legally a minor. */
	private Boolean minor = false;

	/** True iff the user has provided Lightspeed management with
	 * authentication of student status.  May provide benefits beyond
	 * those of a non-student user. */
	private Boolean student = false;

	/** Team Employee  TOCS User migration - add new fields to TTCO database */
	private boolean teamEmployee = false;

	/** Team Employee - true if user has been validated by ESS application. */
	private boolean agreeToTerms = false;

	/** Type of membership in Lightspeed - not currently used. */
	private MemberType memberType = MemberType.FREE;

	/** Source System TOCS User migration - add new fields to TTCO database*/
	private String sourceSystem;

	/** Mailing address - not currently used. */
	private Address mailingAddress;

	/** Home address - currently used for all address purposes. */
	private Address homeAddress;

	/** Business phone number. */
	private String businessPhone;

	/** Cell phone number.*/
	private String cellPhone;

	/** Home phone number. */
	private String homePhone;

	//	/** True iff user is a member of SAG. */
//	private Boolean sagMember = false;
//	/** True iff user is a member of DGA. */
//	private Boolean dgaMember = false;
//	/** True iff user is a member of AFTRA. */
//	private Boolean aftraMember = false;
//	/** True iff user is a member of IATSE. */
//	private Boolean iatseMember = false;
//	/** True iff user is a member of Teamsters. */
//	private Boolean teamstersMember = false;

	/** Which phone is "primary" - 0:office, 1:cell, 2:home */
	private Integer primaryPhoneIndex = 0;

	/** The URL to this person's (free/public) IMDB entry. */
	private String imdbLink;

	/** The IM service used by this person. */
	private String imService = IMServiceType.NONE.name();

	/** The user's id or address within the given IM service. */
	private String imAddress;

	/** The user's agent -- not currently used. */
	private User talentAgent;

	/** The user's manager -- not currently used. */
	private User manager;

//	private Boolean loginAllowed = false;

	/** serialized Map of persisted preferences. */
	private byte[] preferences;

	/** Reset Click Count TOCS User migration - add new fields to TTCO database */
	private Integer resetClickCount = 0;

	/** True iff the user has been locked out of the login process,
	 * typically due to making too many invalid password attempts.  This
	 * condition may be circumvented by using the "reset password" via
	 * email function. */
	private Boolean lockedOut = false;

	/** The timestamp when the user was locked out. */
	private Date lockOutTime;

	/** Password_expiration_days TOCS User migration - add new fields to TTCO database */
	private Integer passwordExpirationDays = 0;

	/** Password_last_changed TOCS User migration - add new fields to TTCO database */
	private Date passwordLastChanged;

	/** The user must create a new password during his next login;
	 * not currently used. */
	private boolean newPasswordRequired;

	/** The number of invalid login attempts (i.e., wrong passwords)
	 * the user has made. */
	private Integer failedLogonCount = 0;

	/** The user's encrypted password.  The clear-text password
	 * is never stored in the database.  */
	private String encryptedPassword;

	/** The encrypted PIN (personal id number) used for validating
	 * timecard transactions such as "Submit". */
	private String pin;

	// * * * Fields related to Start Form auto-fill * * *

	/** encrypted SSN */
	private String socialSecurity;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	/** M/F (or null) */
	private GenderType gender;

	/** when 'Other' Gender is selected LS-1958 */
	private String otherGenderDesc;

	/** This is the Loan out corp. */
	private String loanOutCorpName;

	/**  Loan-out corp address */
	private Address loanOutAddress;

	/** LS-3631 Loan-out corp mailing address */
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

	/** This used to indicate to withhold money for the agent; true/false setting. */
	private boolean agentRep;

	/** Employee's Agency name */
	private String agencyName;

	/**  Agency address */
	private Address agencyAddress;
	/** how pay is calculated - Hourly, Daily, or Weekly */

	/** emergency contact information */
	private String emergencyName;

	/** emergency contact phone number */
	private String emergencyPhone;

	/** relationship of emergency contact person */
	private String emergencyRelation;

	// * * * Fields related to W4 Form auto-fill * * *

	/** Marital status of the user. */
	private String w4Marital;

	/** True if user's last name differs from that shown on his/her social security card. */
	private boolean w4NameDiffers;

	private Integer w4Allowances;

	private Integer w4AddtlAmount;

	private boolean w4Exempt;

	/** The List of Image`s related to this User -- maintained by
	 * the user on the My Account page. */
	private List<Image> images = new ArrayList<>(0);

	/** If this is a performer, this is a list of talent agents associated with this user. */
	private List<Agent> agentsList = new ArrayList<>(0);

	/** A transient flag, for the User list page, to track which item is
	 * currently selected. */
	@Transient
	private boolean selected = false;

	/** List of Image resource generated from the list of images */
	@Transient
	private transient List<IceOutputResource> imageResources;

//	/** The Set of Folder`s owned by this User. */
//	private Set<Folder> folders = new HashSet<Folder>(0);
//	private Set<Note> notes = new HashSet<Note>(0);
//	private Set<Stripboard> stripboards = new HashSet<Stripboard>(0);
//	private Set<Document> documents = new HashSet<Document>(0);
//	private Set<Changes> changeses = new HashSet<Changes>(0);

	/** GST Number - Canadian Tax Id*/
	private String gstNumber;

	/** QST Number Canada Tax Id*/
	private String qstNumber;

	/** Full Member Canada ACTRA union membership Id*/
	private String fullMemberNum;

	/** Apprentice number, for Canadian talent. */
	private String apprenticeNum;

	/** Flag to show US specific functionality */
	private Boolean showUS;

	/** Flag to show Canadian specific functionality */
	private Boolean showCanada;

	/** Guardian Name if minor is selected */
	private String guardianName;

	/**UDA Member */
	private String udaMember;

	/** Agent email address */
	private String agentEmailAddress;

	/** Agent name */
	private String agentName;

	/** Agent's Phone */
	private String agentPhone;

	/** Send Documents to Email Address */
	private String sendDocumentsToEmail;

	/** clientIds For migration of TOCS users from AS400 to TTCOnline*/
	private Set<Integer> clientIds = new HashSet<>();

	/** Tax Classification type for Loan-Out section. LS-2574. */
	private TaxClassificationType taxClassfication;
	/** Type of LLC. LS-2574 */
	private String llcType;

	/** LS-3420 Check box to set the mailing address the same as the home address. */
	private boolean sameAsHomeAddr;

	/** LS-3632 Check box to set the corp mailing address the same as the corp address */
	private boolean sameAsCorpAddr;

	/** LS-3412 If citizenship status is Alien Authorized to Work they need to set their country */
	private String alienAuthCountryCode;

	/** Dollar amount for child dependencies - LS-3453 */
	private Integer childDependencyAmt;

	/** Dollar amount for other dependencies - LS-3453 */
	private Integer otherDependencyAmt;

	/** Additional income dollar amount - LS-3454 */
	private Integer otherIncomeAmt;

	/** Total deductions dollar amount - LS-3454 */
	private Integer deductionsAmt;

	/** Total dollar amount for extra tax amount to be withheld - LS-3454 */
	private Integer extraWithholdingAmt;

	/** Determines whether the employee is working multiple jobs LS-3452 */
	private Boolean multipleJobs;

	/** LS-4756 Flag set by ESS if user is validated by ESS or the user has a signed W4 */
	private boolean ssnLocked;

	// Constructors

	/** default constructor */
	public User() {
		this(false);
	}

	/**
	 * Create User, setting {@link #showUS} and {@link #showCanada} flags
	 * appropriately.
	 *
	 * @param isCanada True if this is a Canadian/Talent production.
	 */
	public User(boolean isCanada) {
		showUS = ! isCanada;
		showCanada = isCanada;
	}

	/**
	 * ** NOTE!!! ** This method is NOT called with the current (ver 4.7.0) LS
	 * code. The 3.6 version of Hibernate (at least with our implementation),
	 * does not recognize the @PrePersist or @PreUpdate annotations.
	 * <p>
	 * Clean up data fields before persisting the object. In particular, a
	 * zero-length SSN is replaced by null.
	 */
	@PrePersist
	@PreUpdate
	private void prepareData(){
		if (socialSecurity != null && socialSecurity.length() == 0) {
			socialSecurity = null;
		}
	}

	// Property accessors

	/**
	 * Ensure that we have a non-null Address object for each of
	 * our Address fields.
	 */
	public void  initAddresses() {
		if (getHomeAddress() == null) {
			setHomeAddress(new Address(showCanada && ! showUS));
		}
		if (getMailingAddress() == null) {
			setMailingAddress(new Address(showCanada && ! showUS));
		}
		if (getLoanOutAddress() == null) {
			setLoanOutAddress(new Address(showCanada && ! showUS));
		}
		// LS-3578
		if (getLoanOutMailingAddress() == null) {
			setLoanOutMailingAddress(new Address(showCanada && ! showUS));
		}
		if (getAgencyAddress() == null) {
			setAgencyAddress(new Address(showCanada && ! showUS));
		}
	}

	/**
	 * @return Either the 'displayName' field, if non-blank, otherwise the value
	 *         of {@link #getFullName()}.
	 */
	@Column(name = "Display_Name", length = 50)
	public String getDisplayName() {
		if (StringUtils.isEmpty(displayName)) {
			return getFullName();
		}
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return A non-blank name of the person. Currently the same as
	 *         {@link #getDisplayName()}.
	 */
	@Transient
	public String getAnyName() {
		return getDisplayName();
	}

	/** See {@link #accountNumber}. */
	@Column(name = "Account_Number", nullable = false, length = 20)
	public String getAccountNumber() {
		return accountNumber;
	}
	/** See {@link #accountNumber}. */
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	/** See {@link #status}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public UserStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(UserStatus status) {
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created", length = 0)
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date updated) {
		created = updated;
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

	/** See {@link #referredBy}. */
	@Column(name = "Referred_By", length = 30)
	public String getReferredBy() {
		return referredBy;
	}
	/** See {@link #referredBy}. */
	public void setReferredBy(String referredBy) {
		this.referredBy = referredBy;
	}

	@Column(name = "Prefix", length = 10)
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Column(name = "First_Name", nullable = false, length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name = "Middle_Name", length = 30)
	public String getMiddleName() {
		return middleName;
	}
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	@Column(name = "Last_Name", nullable = false, length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #agentsList}. */
	@ManyToMany(
			targetEntity=Agent.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "agent_user",
			joinColumns = {@JoinColumn(name = "User_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Agent_Id")}
			)
	public List<Agent> getAgentsList() {
		return agentsList;
	}
	/** See {@link #agentsList}. */
	public void setAgentsList(List<Agent> agentsList) {
		this.agentsList = agentsList;
	}

	/**See {@link #birthdate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Birthdate", length = 10)
	public Date getBirthdate() {
		return birthdate;
	}
	/**See {@link #birthdate}. */
	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}

	/**See {@link #sameAsHomeAddr}. */
	@Column(name = "Same_As_Home_Addr")
	public boolean getSameAsHomeAddr() {
		return sameAsHomeAddr;
	}

	/**See {@link #sameAsHomeAddr}. */
	public void setSameAsHomeAddr(boolean sameAsHomeAddr) {
		this.sameAsHomeAddr = sameAsHomeAddr;
	}

	/**See {@link #sameAsCorpAddr}. */
	@Column(name = "Same_As_Corp_Addr")
	public boolean getSameAsCorpAddr() {
		return sameAsCorpAddr;
	}

	/**See {@link #sameAsCorpAddr}. */
	public void setSameAsCorpAddr(boolean sameAsCorpAddr) {
		this.sameAsCorpAddr = sameAsCorpAddr;
	}

	/**
	 * @return A concatenation of the first-name and last-name fields separated
	 *         by a blank.
	 */
	@Transient
	public String getFirstNameLastName() {
		String s = "";
		if (getFirstName() != null) {
			s = getFirstName() + " ";
		}
		if (getLastName() != null) {
			s += getLastName();
		}
		return s;
	}

	@Transient
	public String getLastNameFirstName() {
		String s = "";
		if (getLastName() != null) {
			s = getLastName();
		}
		if (getFirstName() != null) {
			s += ", " + getFirstName();
		}
		return s;
	}

	/**
	 * @return A concatenation of the first-name, middle-name (initial) and last-name fields separated
	 *         by a blank.
	 */
	@Transient
	public String getFullName() {
		String s = "";
		if (! StringUtils.isEmpty(getFirstName())) {
			s = getFirstName() + " ";
		}
		if ((! StringUtils.isEmpty(getMiddleName())) && (! getMiddleName().equals("N/A"))) {
			s +=  getMiddleName() + " ";
		}
		if (! StringUtils.isEmpty(getLastName())) {
			s += getLastName();
		}
		return s;
	}


//	@Transient
//	public String getInformalName() {
//		if (getDisplayName()!= null) {
//			return getDisplayName();
//		}
//		else {
//			return getFirstName();
//		}
//	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Mailing_Address_Id")
	public Address getMailingAddress() {
		return mailingAddress;
	}
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Home_Address_Id")
	public Address getHomeAddress() {
		return homeAddress;
	}
	public void setHomeAddress(Address homeAddress) {
		this.homeAddress = homeAddress;
	}

//	@Column(name = "SAG_Member", nullable = false)
//	public Boolean getSagMember() {
//		return this.sagMember;
//	}
//	public void setSagMember(Boolean sagMember) {
//		this.sagMember = sagMember;
//	}
//
//	@Column(name = "DGA_Member", nullable = false)
//	public Boolean getDgaMember() {
//		return this.dgaMember;
//	}
//	public void setDgaMember(Boolean dgaMember) {
//		this.dgaMember = dgaMember;
//	}
//
//	@Column(name = "AFTRA_Member", nullable = false)
//	public Boolean getAftraMember() {
//		return this.aftraMember;
//	}
//	public void setAftraMember(Boolean aftraMember) {
//		this.aftraMember = aftraMember;
//	}
//
//	@Column(name = "IATSE_Member", nullable = false)
//	public Boolean getIatseMember() {
//		return this.iatseMember;
//	}
//	public void setIatseMember(Boolean iatseMember) {
//		this.iatseMember = iatseMember;
//	}
//
//	@Column(name = "Teamsters_Member", nullable = false)
//	public Boolean getTeamstersMember() {
//		return this.teamstersMember;
//	}
//	public void setTeamstersMember(Boolean teamstersMember) {
//		this.teamstersMember = teamstersMember;
//	}

	@Column(name = "Business_Phone", length = 25)
	public String getBusinessPhone() {
		return businessPhone;
	}
	public void setBusinessPhone(String businessPhone) {
		this.businessPhone = businessPhone;
	}

	@Column(name = "Cell_Phone", length = 25)
	public String getCellPhone() {
		return cellPhone;
	}
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}

	@Column(name = "Home_Phone", length = 25)
	public String getHomePhone() {
		return homePhone;
	}
	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	@Column(name = "Primary_Phone_Index", nullable = false)
	public Integer getPrimaryPhoneIndex() {
		return primaryPhoneIndex;
	}
	public void setPrimaryPhoneIndex(Integer index) {
		// Because the Contact page does not always render all options for the phone index
		// values (due to permissions or "hidden" contact info), this method may be
		// called with a null value; just ignore it.
		if (index != null) {
			primaryPhoneIndex = index;
		}
	}

	/**
	 * @return the "primary" phone number, as indicated by the user having
	 *         selected one of their phone numbers via a radio button.
	 */
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

	@Column(name = "Email_Address", length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Column(name = "IMDB_link", length = 200)
	public String getImdbLink() {
		return imdbLink;
	}
	public void setImdbLink(String imdbLink) {
		this.imdbLink = imdbLink;
	}

	@Column(name = "IM_Service", nullable = false, length = 30)
	public String getImService() {
		return imService;
	}
	public void setImService(String imService) {
		this.imService = imService;
	}

	@Column(name = "IM_Address", length = 100)
	public String getImAddress() {
		return imAddress;
	}
	public void setImAddress(String imAddress) {
		this.imAddress = imAddress;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Agent_Id")
	public User getTalentAgent() {
		return talentAgent;
	}
	public void setTalentAgent(User talentAgent) {
		this.talentAgent = talentAgent;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Manager_Id")
	public User getManager() {
		return manager;
	}
	public void setManager(User manager) {
		this.manager = manager;
	}

	@Column(name = "Minor", nullable = false)
	public Boolean getMinor() {
		return minor;
	}
	public void setMinor(Boolean minor) {
		this.minor = minor;
	}
//
//	@Column(name = "Login_Allowed", nullable = false)
//	public Boolean getLoginAllowed() {
//		return this.loginAllowed;
//	}
//	public void setLoginAllowed(Boolean loginAllowed) {
//		this.loginAllowed = loginAllowed;
//	}

	/** See {@link #student}. */
	@Column(name = "Student", nullable = false)
	public Boolean getStudent() {
		return student;
	}
	/** See {@link #student}. */
	public void setStudent(Boolean student) {
		this.student = student;
	}

	/** See {@link #memberType}. */
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "Member_Type", nullable = false)
	public MemberType getMemberType() {
		return memberType;
	}
	/** See {@link #memberType}. */
	public void setMemberType(MemberType memberType) {
		this.memberType = memberType;
	}

	/** See {@link #preferences}. */
	@Column(name = "Preferences")
	public byte[] getPreferences() {
		return preferences;
	}
	/** See {@link #preferences}. */
	public void setPreferences(byte[] preferences) {
		this.preferences = preferences;
	}

	@Column(name = "Locked_Out", nullable = false)
	public Boolean getLockedOut() {
		return lockedOut;
	}
	public void setLockedOut(Boolean lockedOut) {
		this.lockedOut = lockedOut;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Lock_Out_Time", length = 0)
	public Date getLockOutTime() {
		return lockOutTime;
	}
	public void setLockOutTime(Date lockOutTime) {
		this.lockOutTime = lockOutTime;
	}

	@Column(name = "New_Password_Required", nullable = false)
	public boolean getNewPasswordRequired() {
		return newPasswordRequired;
	}
	public void setNewPasswordRequired(boolean newPasswordRequired) {
		this.newPasswordRequired = newPasswordRequired;
	}

	@Column(name = "Failed_Logon_Count")
	public Integer getFailedLogonCount() {
		return failedLogonCount;
	}
	public void setFailedLogonCount(Integer failedLogonCount) {
		this.failedLogonCount = failedLogonCount;
	}

	/** See {@link #encryptedPassword}. */
	@Column(name = "Password", length = 1000)
	public String getEncryptedPassword() {
		return encryptedPassword;
	}
	/** See {@link #encryptedPassword}. */
	public void setEncryptedPassword(String encryptedPassword) {
		this.encryptedPassword = encryptedPassword;
	}

	/** A transient method that returns the decrypted value of the password.  LS-3204 */
	@Transient
	public String getPassword() {
		return EncryptUtils.decrypt(getEncryptedPassword());
	}
	/** A transient method used to set the password; the parameter should
	 * be the clear text (unencrypted) password.  This will be encrypted before persisting
	 * in the database.  LS-3204 */
	@Transient
	public void setPassword(String password) {
		setEncryptedPassword(EncryptUtils.encrypt(password));
	}

	/** See {@link #pin}.  Method renamed in LS-3204. */
	@Column(name = "Pin", length = 1000)
	public String getEncryptedPin() {
		return pin;
	}
	/** See {@link #pin}.  Method renamed in LS-3204. */
	public void setEncryptedPin(String pin) {
		this.pin = pin;
	}

	/** A transient method that returns the decrypted value of the PIN. LS-3204 */
	@Transient
	public String getPin() {
		return EncryptUtils.decrypt(getEncryptedPin());
	}
	/** A transient method used to set the PIN; the parameter should
	 * be the clear text (unencrypted) PIN.  This will be encrypted before persisting
	 * in the database. */
	@Transient
	public void setPin(String pin) {
		setEncryptedPin(EncryptUtils.encrypt(pin));
	}

	/** See {@link #socialSecurity} */
	@JsonIgnore
	@Column(name = "Social_Security_Aes", length = 1000)
	@ColumnTransformer( // LS-2568 encrypt using MySql AES; LS-4572 use static key
			  read="AES_DECRYPT(Social_Security_Aes, " + ENCRYPT_KEY + ")",
			  write="AES_ENCRYPT(?, " + ENCRYPT_KEY + ")" )
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

	/** See {@link #otherGenderDesc}. */
	@Column(name = "Other_Gender_Desc", length = 50)
	public String getOtherGenderDesc() {
		return otherGenderDesc;
	}

	/** See {@link #otherGenderDesc}. */
	public void setOtherGenderDesc(String otherGenderDesc) {
		this.otherGenderDesc = otherGenderDesc;
	}

	@Column(name = "Loan_Out_Corp_Name", length = 35)
	public String getLoanOutCorpName() {
		return loanOutCorpName;
	}
	public void setLoanOutCorpName(String companyName) {
		loanOutCorpName = companyName;
	}

	/**See {@link #loanOutAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Loan_Out_Address_Id")
	public Address getLoanOutAddress() {
		return loanOutAddress;
	}
	/**See {@link #loanOutAddress}. */
	public void setLoanOutAddress(Address loanOutAddress) {
		this.loanOutAddress = loanOutAddress;
	}

	/**See {@link #loanOutMailingAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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

	/** See {@link #federalTaxId}. */
	@JsonIgnore
	@Column(name = "Federal_Tax_Id_Aes", length = 1000)
	@ColumnTransformer( // LS-4572 encrypt using MySql AES
			  read="AES_DECRYPT(Federal_Tax_Id_Aes, " + ENCRYPT_KEY + ")",
			  write="AES_ENCRYPT(?, " + ENCRYPT_KEY + ")" )
	public String getFederalTaxId() {
		return federalTaxId;
	}
	/** See {@link #federalTaxId}. */
	public void setFederalTaxId(String fedTaxId) {
		federalTaxId = fedTaxId;
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

	@Column(name = "Ethnic_Code", length = 5)
	public String getEthnicCode() {
		return ethnicCode;
	}
	public void setEthnicCode(String ethnicCode) {
		this.ethnicCode = ethnicCode;
	}

	@Column(name = "Citizen_Status", length = 1)
	public String getCitizenStatus() {
		return citizenStatus;
	}
	public void setCitizenStatus(String citizenStatus) {
		this.citizenStatus = citizenStatus;
	}

	@Column(name = "State_of_Residence", length = 50)
	public String getStateOfResidence() {
		return stateOfResidence;
	}
	public void setStateOfResidence(String stateOfResidence) {
		this.stateOfResidence = stateOfResidence;
	}

	@Column(name = "Agent_Rep", nullable = false)
	public boolean getAgentRep() {
		return agentRep;
	}
	public void setAgentRep(boolean agentRep) {
		this.agentRep = agentRep;
	}

	@Column(name = "Agency_Name", length = 35)
	public String getAgencyName() {
		return agencyName;
	}
	public void setAgencyName(String companyName) {
		agencyName = companyName;
	}

	/**See {@link #agencyAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Agency_Address_Id")
	public Address getAgencyAddress() {
		return agencyAddress;
	}
	/**See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	@Column(name = "Emergency_Name", length = 30)
	public String getEmergencyName() {
		return emergencyName;
	}
	public void setEmergencyName(String emergencyName) {
		this.emergencyName = emergencyName;
	}

	@Column(name = "Emergency_Phone", length = 25)
	public String getEmergencyPhone() {
		return emergencyPhone;
	}
	public void setEmergencyPhone(String emergencyPhone) {
		this.emergencyPhone = emergencyPhone;
	}

	@Column(name = "Emergency_Relation", length = 30)
	public String getEmergencyRelation() {
		return emergencyRelation;
	}
	public void setEmergencyRelation(String emergencyRelation) {
		this.emergencyRelation = emergencyRelation;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user") // GnG had EAGER
//	public Set<Folder> getFolders() {
//		return this.folders;
//	}
//	public void setFolders(Set<Folder> folders) {
//		this.folders = folders;
//	}

	/**See {@link #w4Marital}. */
	@Column(name = "W4_Marital", nullable = true, length = 1)
	public String getW4Marital() {
		return w4Marital;
	}
	/**See {@link #w4Marital}. */
	public void setW4Marital(String w4Marital) {
		this.w4Marital = w4Marital;
	}

	/**See {@link #w4NameDiffers}. */
	@Column(name = "W4_Name_Differs", nullable = false)
	public boolean getW4NameDiffers() {
		return w4NameDiffers;
	}
	/**See {@link #w4NameDiffers}. */
	public void setW4NameDiffers(boolean w4NameDiffers) {
		this.w4NameDiffers = w4NameDiffers;
	}

	/**See {@link #w4Allowances}. */
	@Column(name = "W4_Allowances", nullable = true)
	public Integer getW4Allowances() {
		return w4Allowances;
	}
	/**See {@link #w4Allowances}. */
	public void setW4Allowances(Integer w4Allowances) {
		this.w4Allowances = w4Allowances;
	}

	/**See {@link #w4AddtlAmount}. */
	@Column(name = "W4_Addtl_Amount", nullable = true)
	public Integer getW4AddtlAmount() {
		return w4AddtlAmount;
	}
	/**See {@link #w4AddtlAmount}. */
	public void setW4AddtlAmount(Integer w4AddtlAmount) {
		this.w4AddtlAmount = w4AddtlAmount;
	}

	/**See {@link #w4Exempt}. */
	@Column(name = "W4_Exempt", nullable = false)
	public boolean getW4Exempt() {
		return w4Exempt;
	}
	/**See {@link #w4Exempt}. */
	public void setW4Exempt(boolean w4Exempt) {
		this.w4Exempt = w4Exempt;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
	@OrderBy("date")
	public List<Image> getImages() {
		return images;
	}
	public void setImages(List<Image> images) {
		this.images = images;
	}

	/** See {@link #showUS}. */
	@Column(name = "Show_US")
	public Boolean getShowUS() {
		return showUS;
	}
	/** See {@link #showUS}. */
	public void setShowUS(Boolean showUS) {
		this.showUS = showUS;
	}

	/** See {@link #showCanada}. */
	@Column(name = "Show_Canada")
	public Boolean getShowCanada() {
		return showCanada;
	}
	/** See {@link #showCanada}. */
	public void setShowCanada(Boolean showCanada) {
		this.showCanada = showCanada;
	}

	/** See {@link #alienAuthCountryCode}. */
	@Column(name = "Alien_Auth_Country_Code")
	public String getAlienAuthCountryCode() {
		return alienAuthCountryCode;
	}

	/** See {@link #alienAuthCountryCode}. */
	public void setAlienAuthCountryCode(String alienAuthCountryCode) {
		this.alienAuthCountryCode = alienAuthCountryCode;
	}

	@Transient
	public int getImageCount() {
		if (getImages() != null) {
			return getImages().size();
		}
		return 0;
	}

	@Transient
	public boolean getSelected() {
		return selected;
	}
	public void setSelected(boolean b) {
		selected = b;
	}

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if(imageResources == null) {
			imageResources = ImageUtils.createImageResources(images);
		}
		return imageResources;
	}

	/** See {@link #imageResources}. */
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
//	public Set<Document> getDocuments() {
//		return this.documents;
//	}
//	public void setDocuments(Set<Document> documents) {
//		this.documents = documents;
//	}

	@Override
	public int compareTo(User user) {
		int ret = 1;
		if (user != null) {
			ret = StringUtils.compare(getEmailAddress(), user.getEmailAddress());
		}
		return ret;
	}

	public int compareByName(User other) {
		int ret = StringUtils.compareIgnoreCase(getLastName(), other.getLastName());
		if (ret == 0) {
			ret = StringUtils.compareIgnoreCase(getFirstName(), other.getFirstName());
		}
		return ret;
	}


	public int compareTo(User other, String sortField, boolean ascending) {
		int ret = 0;
		if (sortField == null || sortField.equals(SORTKEY_NAME)) {
			// will sort by name at end
		}
		else if (sortField.equals(SORTKEY_EMAIL)) {
			ret = StringUtils.compareIgnoreCase(getEmailAddress(), other.getEmailAddress());
		}
		else if (sortField.equals(SORTKEY_ACCOUNT)) {
			ret = StringUtils.compareIgnoreCase(getAccountNumber(), other.getAccountNumber());
		}
		else if (sortField.equals(SORTKEY_FIRST_NAME)) {
			ret = StringUtils.compareIgnoreCase(getFirstName(), other.getFirstName());
			if (ret == 0) {
				ret = StringUtils.compareIgnoreCase(getLastName(), other.getLastName());
			}
		}
		else if (sortField.equals(SORTKEY_LAST_NAME)) {
			ret = StringUtils.compareIgnoreCase(getLastName(), other.getLastName());
			if (ret == 0) {
				ret = StringUtils.compareIgnoreCase(getFirstName(), other.getFirstName());
			}
		}
		else if (sortField.equals(SORTKEY_STATUS)) {
			ret = NumberUtils.compare(getStatus().ordinal(), other.getStatus().ordinal());
		}
		else {
			log.error("unexpected sort field=" + sortField);
		}
		if (ret == 0) {
			ret = compareByName(other);
		}
		if ( ! ascending ) {
			ret = 0 - ret;	// swap 1 and -1 return values
			// Note that we do NOT invert non-equal Type compares
		}
		return ret;
	}


	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getEmailAddress() == null) ? 0 : getEmailAddress().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		User other;
		try {
			other = (User)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(other.getId()) ) {
			return true;
		}
		return (compareTo(other) == 0);
	}

	/** See {@link #gstNumber}. */
	@Column(name = "GST_Number", length = 20)
	public String getGstNumber() {
		return gstNumber;
	}
	/** See {@link #gstNumber}. */
	public void setGstNumber(String gstNumber) {
		this.gstNumber = gstNumber;
	}

	/** See {@link #qstNumber}. */
	@Column(name = "QST_Number", length = 20)
	public String getQstNumber() {
		return qstNumber;
	}
	/** See {@link #qstNumber}. */
	public void setQstNumber(String qstNumber) {
		this.qstNumber = qstNumber;
	}

	/** See {@link #fullMemberNum}. */
	@Column(name = "Full_Member_Num", length = 20)
	public String getFullMemberNum() {
		return fullMemberNum;
	}
	/** See {@link #fullMemberNum}. */
	public void setFullMemberNum(String fullMemberNum) {
		this.fullMemberNum = fullMemberNum;
	}

	/** See {@link #apprenticeNum}. */
	@Column(name = "Apprentice_Num", length = 20)
	public String getApprentice() {
		return apprenticeNum;
	}
	/** See {@link #apprenticeNum}. */
	public void setApprentice(String apprentice) {
		this.apprenticeNum = apprentice;
	}

	/** See {@link #guardianName}. */
	@Column(name = "Guardian_Name", length = 75)
	public String getGuardianName() {
		return guardianName;
	}
	/** See {@link #guardianName}. */
	public void setGuardianName(String guardianName) {
		this.guardianName = guardianName;
	}

	/** See {@link #agentEmailAddress}. */
	@Column(name = "Agent_Email", length = 100)
	public String getAgentEmailAddress() {
		return agentEmailAddress;
	}
	/** See {@link #agentEmailAddress}. */
	public void setAgentEmailAddress(String agentEmailAddress) {
		this.agentEmailAddress = agentEmailAddress;
	}

	/** See {@link #agentName}. */
	@Column(name = "Agent_Name", length = 60)
	public String getAgentName() {
		return agentName;
	}
	/** See {@link #agentName}. */
	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	/** See {@link #agentPhone}. */
	@Column(name = "Agent_Phone", length = 25)
	public String getAgentPhone() {
		return agentPhone;
	}
	/** See {@link #agentPhone}. */
	public void setAgentPhone(String agentPhone) {
		this.agentPhone = agentPhone;
	}

	/** See {@link #sendDocumentsToEmail}. */
	@Column(name = "SendDocumentsTo_Email", length = 100)
	public String getSendDocumentsToEmail() {
		return sendDocumentsToEmail;
	}
	/** See {@link #sendDocumentsToEmail}. */
	public void setSendDocumentsToEmail(String sendDocumentsToEmail) {
		this.sendDocumentsToEmail = sendDocumentsToEmail;
	}

	/** See {@link #userName}. */
	@Column(name = "User_Name", length = 20)
	public String getUserName() {
		return userName;
	}

	/** See {@link #userName}. */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/** See {@link #teamEmployee}. */
	@Column(name = "Team_Employee", nullable = false)
	public boolean getTeamEmployee() {
		return teamEmployee;
	}
	/** See {@link #teamEmployee}. */
	public void setTeamEmployee(boolean teamEmployee) {
		this.teamEmployee = teamEmployee;
	}

	/** See {@link #agreeToTerms}. */
	@Column(name = "Agree_To_Terms", nullable = false)
	public boolean getAgreeToTerms() {
		return agreeToTerms;
	}
	/** See {@link #agreeToTerms}. */
	public void setAgreeToTerms(boolean agreeToTerms) {
		this.agreeToTerms = agreeToTerms;
	}

	/** See {@link #sourceSystem}. */
	@Column(name = "Source_System", length = 20)
	public String getSourceSystem() {
		return sourceSystem;
	}

	/** See {@link #sourceSystem}. */
	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	/** See {@link #resetClickCount}. */
	@Column(name = "Reset_Click_Count",  nullable = false)
	public Integer getResetClickCount() {
		return resetClickCount;
	}

	/** See {@link #resetClickCount}. */
	public void setResetClickCount(Integer resetClickCount) {
		this.resetClickCount = resetClickCount;
	}

	/** See {@link #passowrdExpirationDays}. */
	@Column(name = "Password_Expiration_Days")
	public Integer getPasswordExpirationDays() {
		return passwordExpirationDays;
	}

	/** See {@link #passwordExpirationDays}. */
	public void setPasswordExpirationDays(Integer passwordExpirationDays) {
		this.passwordExpirationDays = passwordExpirationDays;
	}

	/** See {@link #passwordLastChanged}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Password_Last_Changed", length = 0)
	public Date getPasswordLastChanged() {
		return passwordLastChanged;
	}

	/** See {@link #passwordLastChanged}. */
	public void setPasswordLastChanged(Date passwordLastChanged) {
		this.passwordLastChanged = passwordLastChanged;
	}

	/** See {@link #clientIds}. */
	@ElementCollection
	@CollectionTable(name = "user_client", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "client_id")
	public Set<Integer> getClientIds() {
		return clientIds;
	}

	/** See {@link #clientIds}. */
	public void setClientIds(Set<Integer> clientIds) {
		this.clientIds = clientIds;
	}

	/** See {@link #udaMember}. */
	@Column(name = "Uda_Member", length = 20)
	public String getUdaMember() {
		return udaMember;
	}
	/** See {@link #udaMember}. */
	public void setUdaMember(String udaMember) {
		this.udaMember = udaMember;
	}

	/** See {@link #taxClassfication}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Tax_Classification")
	public TaxClassificationType getTaxClassification() {
		return taxClassfication;
	}

	/** See {@link #taxClassfication}. */
	public void setTaxClassification(TaxClassificationType taxClassfication) {
		this.taxClassfication = taxClassfication;
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

	/** See {@link #childDependencyAmt}. */
	@Column(name = "Child_Dependency_Amt")
	public Integer getChildDependencyAmt() {
		return childDependencyAmt;
	}

	/** See {@link #childDependencyAmt}. */
	public void setChildDependencyAmt(Integer childDependencyAmt) {
		this.childDependencyAmt = childDependencyAmt;
	}

	/** See {@link #otherDependencyAmt}. */
	@Column(name = "Other_Dependency_Amt")
	public Integer getOtherDependencyAmt() {
		return otherDependencyAmt;
	}

	/** See {@link #otherDependencyAmt}. */
	public void setOtherDependencyAmt(Integer otherDependencyAmt) {
		this.otherDependencyAmt = otherDependencyAmt;
	}

	/** See {@link #otherIncomeAmt}. */
	@Column(name = "Other_Income_Amt")
	public Integer getOtherIncomeAmt() {
		return otherIncomeAmt;
	}

	/** See {@link #otherIncomeAmt}. */
	public void setOtherIncomeAmt(Integer otherIncomeAmt) {
		this.otherIncomeAmt = otherIncomeAmt;
	}

	/** See {@link #deductionsAmt}. */
	@Column(name = "Deductions_Amt")
	public Integer getDeductionsAmt() {
		return deductionsAmt;
	}

	/** See {@link #deductionsAmt}. */
	public void setDeductionsAmt(Integer deductionsAmt) {
		this.deductionsAmt = deductionsAmt;
	}

	/** See {@link #extraWithholdingAmt}. */
	@Column(name = "Extra_Withholding_Amt")
	public Integer getExtraWithholdingAmt() {
		return extraWithholdingAmt;
	}

	/** See {@link #extraWithholdingAmt}. */
	public void setExtraWithholdingAmt(Integer extraWithholdingAmt) {
		this.extraWithholdingAmt = extraWithholdingAmt;
	}

	/** See {@link #multipleJobs}. */
	@Column(name = "Multiple_Jobs")
	public Boolean getMultipleJobs() {
		return multipleJobs;
	}

	/** See {@link #multipleJobs}. */
	public void setMultipleJobs(Boolean multipleJobs) {
		this.multipleJobs = multipleJobs;
	}

	/** See {@link #ssnLocked}. */
	@Column(name = "ssn_locked")
	public boolean getSsnLocked() {
		return ssnLocked;
	}

	/** See {@link #ssnLocked}. */
	public void setSsnLocked(boolean ssnLocked) {
		this.ssnLocked = ssnLocked;
	}

}
