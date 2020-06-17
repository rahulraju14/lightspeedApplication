package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.object.ApproverChainRoot;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.common.TimeZoneUtils;

/**
 * Production entity. This is the top-level object of an entire production,
 * which may include multiple projects. The objects that belong to a Production
 * include Project`s, Contact`s, User`s, RealWorldElement`s, Contract`s,
 * StartForm`s, WeeklyTimecard`s, and Event`s. Note that even a "feature"
 * Production has a Project, so the same hierarchy is present for both feature
 * and TV (or commercial) productions.
 */

@NamedQueries ({
	@NamedQuery(name = Production.GET_PRODUCTION_LIST_BY_ACTIVE_STATUS_AND_ALLOW_ONBOARDING, query = "from Production p where p.status ='ACTIVE' and p.allowOnboarding = true"),
	@NamedQuery(name = Production.GET_PRODUCTION_LIST_BY_USER_AND_ALLOW_ONBOARDING, query =
			"select p from Production p, Contact c where c.production = p and c.user =:user and p.allowOnboarding = true and p.status <> 'DELETED' order by p.title"),
	@NamedQuery(name = Production.HAS_PRODUCTION_BY_USER_AND_IS_TEAM_CLIENT, query =
			"select case when count(p) <> 0 then true else false end as b " +
			"from Production p, Contact c, PayrollPreference pp, PayrollService ps " +
			"where c.production = p and p.type <> 'CANADA_TALENT' and c.user =:user and p.payrollPref = pp and pp.payrollService = ps and ps.teamPayroll = 1"),
})

@Entity
@Table(name = "production", uniqueConstraints = @UniqueConstraint(columnNames = "Prod_Id"))
public class Production extends PersistentObject<Production>
		implements Cloneable, Comparable<Production>, ApproverChainRoot {
	private static final Log log = LogFactory.getLog(Production.class);

	// Fields
	private static final long serialVersionUID = -1945485555827173008L;

	public static final String SORTKEY_ID = "id";
	public static final String SORTKEY_NAME = "name";
	public static final String SORTKEY_STUDIO = "studio";
	public static final String SORTKEY_START = "start";
	public static final String SORTKEY_PRODID = "prodid";
	public static final String SORTKEY_OWNER = "owner";
//	public static final String SORTKEY_PHASE = "phase";

	// Named Query Literals
	/** Returns a list of all Active productions that have Onboarding enabled. */
	public static final String GET_PRODUCTION_LIST_BY_ACTIVE_STATUS_AND_ALLOW_ONBOARDING = "getProductionByActiveStatusAndAllowOnboarding";
	/** Returns a list of all Active or Archived productions that have Onboarding enabled to which the given User belongs. */
	public static final String GET_PRODUCTION_LIST_BY_USER_AND_ALLOW_ONBOARDING = "getProductionByUserAndAllowOnboarding";
	/** Query to see if a given user is, or ever was, a member of a production that is (or was) a Team payroll client. */
	public static final String HAS_PRODUCTION_BY_USER_AND_IS_TEAM_CLIENT = "hasProductionByUserAndIsTeamClient";

	/** The account number of the User that "owns" (or created) this Production. */
	private String owningAccount;

	/** This Production's business address. */
	private Address address;

	/** The starting folder for the repository of this Production. */
	private Folder rootFolder;

	/** A unique identifier for this production. This is used as part of the email
	 * "from" field if the "emailSender" field has not been set. */
	private String prodId;

	/** The name of the studio or production company associated with this Production. Note
	 * that for Commercial (AICP) Productions, this field is not directly editable by the
	 * user, but is kept in sync with the {@link #title} field. */
	private String studio;

	/** The name of the Production; not required to be unique.  For Commercial (AICP)
	 * Productions this is normally the production company name. */
	private String title;

	/** An image used to represent the production. */
	private Image logo;

	/** The type of production: TV series, movie, documentary, etc.
	 * @see ProductionType */
	private ProductionType type;

	/** The Production's status - online, offline, etc.
	 * @see AccessStatus */
	private AccessStatus status;

	/** The status of the order (payment) for this production.
	 * @see OrderStatus */
	private OrderStatus orderStatus;

	/** The billing transaction id last associated with this Production. */
	private String transactionId;

	/** The SKU used when creating this Production. */
	private String sku;

	/** The maximum number of Projects allowed within this Production. */
	private Integer maxProjects;

	/** The maximum number of users that can be associated with this Production.
	 * This is equivalent to the number of Contact records that are linked to
	 * this Production, not counting any LS Admin users. */
	private Integer maxUsers;

	/** Mask that tracks which Department`s should be displayed in selection lists. */
	@Transient
	private BitMask deptMask = new BitMask();

	/** True if this production is allowed to send SMS messages. */
	private Boolean smsEnabled;

	/** True if notifications should be generated for appropriate events. If this
	 * is false, notifications will be off for all projects. If this is true, notifications
	 * are controlled on a project-by-project basis by the Project.notifying field. */
	private Boolean notify = true;

	/** True iff Contacts with cast roles should be allowed to create crew-style
	 * timecards (WeeklyTimecard).  This also affects what StartForm`s can be created. */
	private Boolean castUseCrewTc = false;

	/** Watermark control setting for Script printouts. */
	private WatermarkPreference watermark = WatermarkPreference.OPTIONAL;

	/** If watermark is required on full pages, this indicates if it is
	 * also required on "sides" style printouts. */
	private boolean enforceWatermarkSides = false;

//	private OptionSetting watermarkSides = OptionSetting.OPTIONAL;

	/** The maximum number of failed logon attempts into this production before
	 * a user is locked out. */
	private Integer maxLogonAttempts;

	/** The length of time (in ?) that a user will be locked out.  After this time
	 * has elapsed, the user can once again attempt to login. */
	private Integer lockOutDelay;

	/** The Production's starting date; this is NOT the same as the shooting start
	 * date, which is set at the Project/Unit level. */
	private Date startDate;

	/** The Production's expected end date; null if no expiration date. */
	private Date endDate;

	/** The next date the production's owner will be billed; null for free
	 * productions. */
	private Date nextBillDate;

	/** Billing amount, typically monthly, for this production.  Usually set
	 * at creation, based on SKU, possibly adjusted due to a discount coupon. */
	private BigDecimal billingAmount;

	private BigDecimal timecardFeePercent;

	private BigDecimal documentFeeAmount;

	/** The name of a person to contact regarding this Production. */
	private String contactName;

	/** A contact phone number for the Production. */
	private String phone;

	/** A fax number for the Production. */
	private String fax;

	/** An SMS "short code" for the Production; unused at this time.  This might
	 * be used to handle message acknowledgments. */
	private String smsShortCode;

	/** The time-zone used to control the display of dates & times when
	 * logged into this Production.  This is the Java time zone id string,
	 * which is not displayed to the user. */
	private String timeZoneStr;

	/** The transient TimeZone object corresponding to timeZoneStr. */
	@Transient
	private TimeZone timeZone;

	/** The transient String which is the Windows equivalent description for
	 * this Production's time zone.  This is the value displayed to the user. */
	@Transient
	private String timeZoneName;

	/** The text to be used as the email "from" field when sending notifications and
	 * documents from this Production. */
	private String emailSender;

	/** The Project that users will log into, by default, if they have not logged
	 * in before, or if the last project they were logged into is no longer available. */
	private Project defaultProject;

	//  * * *  PAYROLL FIELDS  * * *

	/** Holds all the production-wide payroll preferences.  Note that for Commercial
	 * productions, there will be a PayrollPreference for each Project. */
	private PayrollPreference payrollPref;

	/** First approver (in the chain of Approvers) for this production. */
	private Approver approver;

	//  * * *  END PAYROLL FIELDS  * * *

	/** Text used to customize outgoing emails. */
	private String customText1;

	/** Text used to customize prompt on My Starts and Payroll / Start Forms page. LS-4219 */
	private String customText2;

//	/** The set of Event objects associated with this Production. */
//	private Set<Event> events = new HashSet<Event>(0);
//	private Set<Changes> changes = new HashSet<Changes>(0);

	/** The set of Project`s associated with this Production. */
	private Set<Project> projects = new HashSet<>(0);

	/** The set of Contracts that this production is a signatory to, which can
	 * be applied during HTG processing. */
	private Set<Contract> contracts;

	/** True if this production is using Onboarding (document management) features. */
	private Boolean allowOnboarding = false;

	/** True if 'enable Production access' checkbox is checked by default when adding
	 * new cast and crew. */
	private Boolean defaultAccess = false;

	/** True if this production is allowed to transfer timecard batches with modified options */
	private boolean batchTransferExtraField;

	/** True if the "script/scheduling" tabs should be displayed in this production. This defaults
	 * to false for Commercial and Tours production types. */
	private Boolean showScriptTabs = true;

	/** Collection of contract codes and associated side letters */
	private Map<String, String>contractSideLetters;

	// Transient fields

	/**Transient  Map of Contract Codes and associated side letters.
	 * Key is contract code and value is list of string built from a comma
	 * delimited string of associated side letters. This comma delimited
	 * String is pulled from the contractSideLetters map.
	 */
	@Transient
	private Map<String, List<String>> contractSideLettersDisplayMap;

	/** Transient field used in display of My Productions list.  Indicates the
	 * current shooting day number and the number of shooting days of the
	 * default project of this Production. */
	@Transient
	private String dayStatus;

	/** Transient field used in the display of the "My Productions" list. Set to the
	 * scheduled (shooting) start date of the default project of this production. */
	@Transient
	private Date scheduleStartDate;

	/** Transient field used to track row selection on a ListView page. */
	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public Production() {
	}

	/**
	 * A constructor to supply our normal default values.  We don't do this in
	 * the default constructor, so we don't bother doing this for Hibernate-constructed
	 * instances.
	 *
	 * @param pTitle
	 */
	public Production(String pTitle) {
		title = pTitle;
		prodId = ("?" + (int)(Math.random()*1000000.0));
		status = AccessStatus.ACTIVE;
		type = ProductionType.OTHER;
		maxProjects = 100;
		maxUsers = 5;
		orderStatus = OrderStatus.PENDING;
		smsEnabled = false;
		sku = "?";
		maxLogonAttempts = 5;
		lockOutDelay = 10;
		Calendar cal = Calendar.getInstance();
		CalendarUtils.setStartOfDay(cal);
		startDate = cal.getTime();
		timeZone = ApplicationUtils.getTimeZoneStatic();
		timeZoneStr = timeZone.getID();
		emailSender = Constants.DEFAULT_EMAIL_SENDER;
		setPayrollPref(new PayrollPreference());
		deptMask = new BitMask(Constants.ALL_DEPARTMENTS_MASK);
		contractSideLettersDisplayMap = null;
		batchTransferExtraField = false;
	}

	// Property accessors

	/** See {@link #owningAccount}. */
	@Column(name = "owning_account", length = 20)
	public String getOwningAccount() {
		return owningAccount;
	}
	/** See {@link #owningAccount}. */
	public void setOwningAccount(String owningAccount) {
		this.owningAccount = owningAccount;
	}

	@ManyToOne(fetch = FetchType.LAZY) // changed to lazy in 3.0.4770
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Repository_Id")
	public Folder getRootFolder() {
		return rootFolder;
	}
	public void setRootFolder(Folder folder) {
		rootFolder = folder;
	}

	@Column(name = "Prod_Id", unique = true, nullable = false, length = 10)
	public String getProdId() {
		return prodId;
	}
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	@Column(name = "Studio", length = 100)
	public String getStudio() {
		return studio;
	}
	public void setStudio(String studio) {
		this.studio = studio;
	}

	@Column(name = "Title", length = 100)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Transient
	public String getTitleLowerCase() {
		if (title == null) {
			return null;
		}
		return title.toLowerCase();
	}

	/** See {@link #logo}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Logo_Id")
	public Image getLogo() {
		return logo;
	}
	/** See {@link #logo}. */
	public void setLogo(Image logo) {
		this.logo = logo;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ProductionType getType() {
		return type;
	}
	public void setType(ProductionType type) {
		this.type = type;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public AccessStatus getStatus() {
		return status;
	}
	public void setStatus(AccessStatus status) {
		this.status = status;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Order_Status", nullable = false, length = 30)
	public OrderStatus getOrderStatus() {
		return orderStatus;
	}
	public void setOrderStatus(OrderStatus status) {
		orderStatus = status;
	}

	/** See {@link #transactionId}. */
	@Column(name = "Transaction_Id", length = 30)
	public String getTransactionId() {
		return transactionId;
	}
	/** See {@link #transactionId}. */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/** See {@link #sku}. */
	@Column(name = "Sku", length = 100)
	public String getSku() {
		return sku;
	}
	/** See {@link #sku}. */
	public void setSku(String sku) {
		this.sku = sku;
	}

	/** See {@link #maxUsers}. */
	@Column(name = "Max_Users", nullable = false)
	public Integer getMaxUsers() {
		return maxUsers;
	}
	/** See {@link #maxUsers}. */
	public void setMaxUsers(Integer maxUsers) {
		this.maxUsers = maxUsers;
	}

	/** See {@link #maxProjects}. */
	@Column(name = "Max_Projects", nullable = false)
	public Integer getMaxProjects() {
		return maxProjects;
	}
	/** See {@link #maxProjects}. */
	public void setMaxProjects(Integer maxProjects) {
		this.maxProjects = maxProjects;
	}

	@Transient
	/** See {@link #deptMask}. Provides access as a BitMask object. */
	public BitMask getDeptMaskB() {
		getDeptMask(); // ensure Hibernate has retrieved value
		return deptMask;
	}
	/** See {@link #deptMask}. Provides access as a BitMask object. */
	public void setDeptMaskB(BitMask deptMask) {
		this.deptMask = deptMask;
	}

	/** See {@link #deptMask}. Method used by Hibernate. This method will also be
	 * 'found' by BeanPropertyRowMapper's -- that class requires the table
	 * column name to "match" the accessor method names. */
	@Column(name = "Dept_Mask")
	public long getDeptMask() {
		return deptMask.getLong();
	}
	/** See {@link #deptMask}. Method used by Hibernate. */
	public void setDeptMask(long mask) {
		deptMask.set(mask);
	}

	/** See {@link #smsEnabled}. */
	@Column(name = "SMS_Enabled", nullable = false)
	public Boolean getSmsEnabled() {
		return smsEnabled;
	}
	/** See {@link #smsEnabled}. */
	public void setSmsEnabled(Boolean b) {
		smsEnabled = b;
	}

	/** See {@link #notify}. */
	@Column(name = "Notify", nullable = false)
	public Boolean getNotify() {
		return notify;
	}
	/** See {@link #notify}. */
	public void setNotify(Boolean notify) {
		this.notify = notify;
	}

	/** See {@link #castUseCrewTc}. */
	@Column(name = "Cast_Use_Crew_Tc", nullable = false)
	public Boolean getCastUseCrewTc() {
		return castUseCrewTc;
	}
	/** See {@link #castUseCrewTc}. */
	public void setCastUseCrewTc(Boolean castUseCrewTc) {
		this.castUseCrewTc = castUseCrewTc;
	}

	/** See {@link #watermark}. */
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "Watermark", nullable = false)
	public WatermarkPreference getWatermark() {
		return watermark;
	}
	/** See {@link #watermark}. */
	public void  setWatermark(WatermarkPreference wm) {
		watermark = wm;
	}

	/**
	 * Returns watermark-required setting for "Sides" style of printing; value
	 * is based on the watermark preference, and the 'enforce watermark for
	 * sides' setting.
	 *
	 * @return WatermarkSides option setting
	 */
	@Transient
	public WatermarkPreference getWatermarkSides() {
		WatermarkPreference ret = getWatermark();
		if (ret == WatermarkPreference.REQUIRED && ! getEnforceWatermarkSides()) {
			ret = WatermarkPreference.OPTIONAL;
		}
		return ret;
	}

	/** See {@link #enforceWatermarkSides}. */
	@Column(name = "Enforce_watermark_sides", nullable = false)
	public boolean getEnforceWatermarkSides() {
		return enforceWatermarkSides;
	}
	/** See {@link #enforceWatermarkSides}. */
	public void setEnforceWatermarkSides(boolean enforceWatermarkSides) {
		this.enforceWatermarkSides = enforceWatermarkSides;
	}

	@Column(name = "Max_Logon_Attempts", nullable = false)
	public Integer getMaxLogonAttempts() {
		return maxLogonAttempts;
	}

	public void setMaxLogonAttempts(Integer maxLogonAttempts) {
		this.maxLogonAttempts = maxLogonAttempts;
	}
	@Column(name = "Lock_Out_Delay", nullable = false)
	public Integer getLockOutDelay() {
		return lockOutDelay;
	}

	public void setLockOutDelay(Integer lockOutDelay) {
		this.lockOutDelay = lockOutDelay;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", nullable = false, length = 0)
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 0)
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #payrollPref}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Payroll_Preference_Id")
	public PayrollPreference getPayrollPref() {
		return payrollPref;
	}
	/** See {@link #payrollPref}. */
	public void setPayrollPref(PayrollPreference payrollPref) {
		this.payrollPref = payrollPref;
	}

	@ManyToMany(
			targetEntity=Contract.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "production_contract",
			joinColumns = {@JoinColumn(name = "Production_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Contract_Key", referencedColumnName="Contract_key")}
			)
	public Set<Contract> getContracts() {
		return contracts;
	}
	public void setContracts(Set<Contract> cons) {
		contracts = cons;
	}

	/** See {@link #allowOnboarding}. */
	@Column(name = "Allow_Onboarding", nullable = false)
	public Boolean getAllowOnboarding() {
		return allowOnboarding;
	}
	/** See {@link #allowOnboarding}. */
	public void setAllowOnboarding(Boolean allowOnboarding) {
		this.allowOnboarding = allowOnboarding;
	}

	/** See {@link #showScriptTabs}. */
	@Column(name = "Show_Script_Tabs", nullable = false)
	public Boolean getShowScriptTabs() {
		return showScriptTabs;
	}
	/** See {@link #showScriptTabs}. */
	public void setShowScriptTabs(Boolean showScriptTabs) {
		this.showScriptTabs = showScriptTabs;
	}

	/**
	 * @return A comma-delimited list of the Contract.contractCode values for
	 *         all the Contract`s associated with this Production. Each code is
	 *         enclosed in primes ('). The intention is that this returned list
	 *         may be used in an SQL Where clause.  If no Contract is associated
	 *         with the Production, an empty, non-null, String ("") is returned.
	 */
	@Transient
	public String getContractCodes() {
		String codes = "";
		if (getContracts() != null && getContracts().size() > 0) {
			StringBuffer buffer = new StringBuffer(getContracts().size()*10);
			for (Contract c : getContracts()) {
				buffer.append(",'").append(c.getContractCode()).append('\'');
			}
			codes = buffer.toString().substring(1);
		}
		return codes;
	}


	/** See {@link #nextBillDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Next_Bill_Date", length = 0)
	public Date getNextBillDate() {
		return nextBillDate;
	}
	/** See {@link #nextBillDate}. */
	public void setNextBillDate(Date nextBillDate) {
		this.nextBillDate = nextBillDate;
	}

	/** See {@link #billingAmount}. */
	@Column(name = "Billing_Amount", precision = 8, scale = 2)
	public BigDecimal getBillingAmount() {
		return billingAmount;
	}
	/** See {@link #billingAmount}. */
	public void setBillingAmount(BigDecimal billingAmount) {
		this.billingAmount = billingAmount;
	}

	/** See {@link #timecardFeePercent}. */
	@Column(name = "Timecard_Fee_Percent", precision = 5, scale = 3)
	public BigDecimal getTimecardFeePercent() {
		return timecardFeePercent;
	}
	/** See {@link #timecardFeePercent}. */
	public void setTimecardFeePercent(BigDecimal timecardFeePercent) {
		this.timecardFeePercent = timecardFeePercent;
	}

	/** See {@link #documentFeeAmount}. */
	@Column(name = "Document_Fee_Amount", precision = 5, scale = 3)
	public BigDecimal getDocumentFeeAmount() {
		return documentFeeAmount;
	}
	/** See {@link #documentFeeAmount}. */
	public void setDocumentFeeAmount(BigDecimal documentFeeAmount) {
		this.documentFeeAmount = documentFeeAmount;
	}

	@Column(name = "Contact_Name", length = 50)
	public String getContactName() {
		return contactName;
	}
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	@Column(name = "Phone", length = 25)
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column(name = "Fax", length = 25)
	public String getFax() {
		return fax;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}

	/** See {@link #timeZoneStr}. */
	@Column(name = "Time_Zone", nullable = false, length = 50)
	public String getTimeZoneStr() {
		return timeZoneStr;
	}
	/** See {@link #timeZoneStr}. */
	public void setTimeZoneStr(String tz) {
		if (tz == null || ! tz.equals(timeZoneStr)) {
			timeZoneStr = tz;
			timeZone = null;
			timeZoneName = null;
		}
	}

	/** See {@link #timeZone}. */
	@Transient
	public TimeZone getTimeZone() {
		if (timeZone == null) {
			timeZone = TimeZone.getTimeZone(getTimeZoneStr());
		}
		return timeZone;
	}
	/** See {@link #timeZone}. */
	public void setTimeZone(TimeZone tz) {
		timeZone = tz;
		timeZoneStr = tz.getID();
		timeZoneName = null;
	}

	/** See {@link #timeZoneName}. */
	@Transient
	public String getTimeZoneName() {
		if (timeZoneName == null) {
			timeZoneName = TimeZoneUtils.getTimeZoneName(timeZoneStr);
		}
		return timeZoneName;
	}

	/** See {@link #emailSender}. */
	@Column(name = "Email_Sender", length = 30)
	public String getEmailSender() {
		return emailSender;
	}
	/** See {@link #emailSender}. */
	public void setEmailSender(String emailSender) {
		this.emailSender = emailSender;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "production")
//	public Set<Event> getEvents() {
//		return this.events;
//	}
//	public void setEvents(Set<Event> events) {
//		this.events = events;
//	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "production")
//	public Set<Changes> getChanges() {
//		return this.changes;
//	}
//	public void setChanges(Set<Changes> changeses) {
//		this.changes = changeses;
//	}

	/** See {@link #customText1}. */
	@Column(name = "Custom_Text1", length = 500)
	public String getCustomText1() {
		return customText1;
	}
	/** See {@link #customText1}. */
	public void setCustomText1(String customText1) {
		this.customText1 = customText1;
	}

	/** See {@link #customText2}. */
	@Column(name = "Custom_Text2", length = 500)
	public String getCustomText2() {
		return customText2;
	}
	/** See {@link #customText2}. */
	public void setCustomText2(String customText2) {
		this.customText2 = customText2;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "production")
	public Set<Project> getProjects() {
		return projects;
	}
	public void setProjects(Set<Project> projects) {
		this.projects = projects;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Default_Project_Id")
	public Project getDefaultProject() {
		return defaultProject;
	}
	public void setDefaultProject(Project defaultProject) {
		this.defaultProject = defaultProject;
	}

	@Column(name = "Sms_short_code", length = 10)
	public String getSmsShortCode() {
		return smsShortCode;
	}
	public void setSmsShortCode(String smsShortCode) {
		this.smsShortCode = smsShortCode;
	}

	@Override
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approver_Id")
	public Approver getApprover() {
		return approver;
	}
	@Override
	public void setApprover(Approver approver) {
		this.approver = approver;
	}

	/** See {@link #contractSideLetters}. */
	@ElementCollection
	@MapKeyColumn(name = "Contract_Code")
	@Column(name = "Side_Letter_Code")
	@CollectionTable(name = "Contract_Code_Side_Letter", joinColumns = @JoinColumn(name = "Production_Id"))
	public Map<String, String> getContractSideLetters() {
		return contractSideLetters;
	}

	/** See {@link #contractSideLetters}. */
	public void setContractSideLetters(Map<String, String> contractSideLetters) {
		this.contractSideLetters = contractSideLetters;
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

	/** See {@link #defaultAccess}. */
	@Column(name = "Default_Access")
	public Boolean getDefaultAccess() {
		return defaultAccess;
	}
	/** See {@link #defaultAccess}. */
	public void setDefaultAccess(Boolean defaultAccess) {
		this.defaultAccess = defaultAccess;
	}

//	/** See {@link #phase}. */
//	@Transient
//	public ProductionPhase getPhase() {
//		if (phase == null) {
//			phase = ProductionPhase.UNKNOWN;
//		}
//		return phase;
//	}
//	/** See {@link #phase}. */
//	public void setPhase(ProductionPhase phase) {
//		this.phase = phase;
//	}

	/** See {@link #batchTransferExtraField}. */
	@Column(name = "Batch_Transfer_Extra_Field", nullable = false)
	public boolean getBatchTransferExtraField() {
		return batchTransferExtraField;
	}
	/** See {@link #batchTransferExtraField}. */
	public void setBatchTransferExtraField(boolean batchTransfer) {
		this.batchTransferExtraField = batchTransfer;
	}

	@Transient
	public String getDayStatus() {
		return dayStatus;
	}
	/** See {@link #dayStatus}. */
	public void setDayStatus(String dayStatus) {
		this.dayStatus = dayStatus;
	}

	/** See {@link #scheduleStartDate}. */
	@Transient
	public Date getScheduleStartDate() {
		if (scheduleStartDate == null) {
			scheduleStartDate = getDefaultProject().getMainUnit().getProjectSchedule().getStartDate();
		}
		return scheduleStartDate;
	}
	/** See {@link #scheduleStartDate}. */
	public void setScheduleStartDate(Date scheduleStartDate) {
		this.scheduleStartDate = scheduleStartDate;
	}

	/** See {@link #contractSideLettersDisplayMap}. */
	@Transient
	public Map<String, List<String>> getContractSideLettersDisplayMap() {
		if(contractSideLettersDisplayMap == null) {
			contractSideLettersDisplayMap = new HashMap<>();

			if(contractSideLetters != null) {
				Set<Entry<String,String>> entrySet = contractSideLetters.entrySet();

				Iterator<Entry<String,String>> itr = entrySet.iterator();

				while(itr.hasNext()) {
					Entry<String,String> entry = itr.next();
					String key = entry.getKey();
					String value = entry.getValue();

					// value is comma delimited String so we need to parse it.
					String[] values = value.split(",");
					List<String>valuesList = new ArrayList<>(0);
					for(int i = 0; i < values.length; i++) {
						valuesList.add(values[i]);
					}

					contractSideLettersDisplayMap.put(key, valuesList);
				}
			}
		}

		return contractSideLettersDisplayMap;
	}

	@Transient
	public boolean isBillable() {
		return getStatus().isBillable() && (getOrderStatus() != OrderStatus.FREE);
	}

	/**
	 * @return True iff this Production is the "SYSTEM" production.
	 */
	@Transient
	public boolean isSystemProduction() {
		return getId() != null && getId().intValue() == Constants.SYSTEM_PRODUCTION_ID;
	}

	/**
	 * @return True iff the production may be updated.  This is a convenient shortcut
	 * for JSP code.
	 */
	@Transient
	public boolean isWritable() {
		return getStatus().isWritable();
	}

	/**
	 * Compare this Production to another one based on the given sort key, and
	 * 'ascending' flag.
	 * @param other
	 * @param sortField
	 * @param ascending
	 * @return standard -1/0/1 for the comparison
	 */
	public int compareTo(Production other, String sortField, boolean ascending) {
		int ret = 0; // SortableList.compareInt(getType().ordinal(), other.getType().ordinal());
		if (ret == 0) {
			if (sortField == null || sortField.equals(SORTKEY_NAME)) {
				// will sort by name at end
			}
			else if (sortField.equals(SORTKEY_ID)) {
				ret = getId().compareTo(other.getId());
			}
			else if (sortField.equals(SORTKEY_PRODID)) {
				ret = StringUtils.compare(getProdId(), other.getProdId());
			}
			else if (sortField.equals(SORTKEY_STUDIO)) {
				ret = StringUtils.compare(getStudio(), other.getStudio());
			}
			else if (sortField.equals(SORTKEY_OWNER)) {
				ret = StringUtils.compare(getOwningAccount(), other.getOwningAccount());
			}
			else if (sortField.equals(SORTKEY_START)) {
				ret = getScheduleStartDate().compareTo(other.getScheduleStartDate());
			}
//			else if (sortField.equals(SORTKEY_PHASE)) {
//				ret = SortableList.compareInt(getPhase().ordinal(), other.getPhase().ordinal());
//			}
			else {
				log.error("unexpected sort field=" + sortField);
			}
			if (ret == 0) {
				ret = getTitle().compareToIgnoreCase(other.getTitle());
			}
			if ( ! ascending ) {
				ret = 0 - ret;	// swap 1 and -1 return values
				// Note that we do NOT invert non-equal Type compares
			}
		}
		return ret;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Production other) {
		int ret = 1;
		if (other != null) {
			ret = compareTo(other, null, true);
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
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
		Production other;
		try {
			other = (Production)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}
		// one of them is transient, use type/name comparison
		return (compareTo(other) == 0);
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "title=" + (getTitle()==null ? "null" : getTitle());
		s += ", start=" + (getStartDate()==null ? "null" : getStartDate());
		s += ']';
		return s;
	}

	@Transient
	public Locale getLocale() {
		if (type.equals(ProductionType.CANADA_TALENT)) {
			return Constants.LOCALE_FRENCH_CANADA;
		}
		return Constants.LOCALE_US;
	}

}