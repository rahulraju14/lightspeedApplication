package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.TimeZone;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.ServiceMethod;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.common.TimeZoneUtils;

/**
 * PayrollService entity. This defines a payroll service company, its name,
 * logos to be used, contact personnel, and the connection information for
 * transmitting data to and/or receiving data from the company.
 */
@Entity
@Table(name = "payroll_service")
public class PayrollService extends PersistentObject<PayrollService> implements Comparable<PayrollService> {
	private static final Log log = LogFactory.getLog(User.class);

	private static final long serialVersionUID = 1L;

	public static final String SORTKEY_NAME = "name";
	private static final String SORTKEY_ID = "id";
	private static final String SORTKEY_CONTACT1 = "contact1";

	// Fields

	/** company name */
	private String name;

	/** company address */
	private Address address;

	/** mailing address */
	private Address mailingAddress;

	/** Logo for bottom of report pages */
	private Image reportLogo;

	/** Logo for desktop screens. */
	private Image desktopLogo;

	/** Logo for mobile screens. */
	private Image mobileLogo;

	/** First contact person's name. */
	private String contact1Name;

	/** Phone number for the first contact. */
	private String contact1Phone;

	/** E-mail address for the first contact. */
	private String contact1Email;

	/** Second contact person's name. */
	private String contact2Name;

	/** Phone number for the second contact. */
	private String contact2Phone;

	/** E-mail address for the second contact. */
	private String contact2Email;

	/** Third contact person's name. */
	private String contact3Name;

	/** Phone number for the third contact. */
	private String contact3Phone;

	/** E-mail address for the third contact. */
	private String contact3Email;

	/** E-mail address to which notifications will be sent after an Allocation Form as been exported. */
	private String toursNotificationEmail;

	/** The time-zone used to control the display of dates & times when
	 * logged into this Production.  This is the Java time zone id string,
	 * which is not displayed to the user. */
	private String timeZoneStr;

	private BigDecimal timecardFeePercent;

	private BigDecimal documentFeeAmount;

	/** The transient TimeZone object corresponding to timeZoneStr. */
	@Transient
	private TimeZone timeZone;

	/** The transient String which is the Windows equivalent description for
	 * this Production's time zone.  This is the value displayed to the user. */
	@Transient
	private String timeZoneName;

	/** True iff this PayrollService is associated with TEAM. */
	private boolean teamPayroll;

	/** Transmit only timecards that have changed (or been added) since
	 * the previous transmission. */
	private boolean sendOnlyChanges;

	/** Pay Breakdown should be split so each line item is for a specific
	 * day, at least for labor and Meal penalties. */
	private boolean breakByDay;

	/** True iff the PDFs and data files created during transfer/export should
	 * be split by the EOR - Employer of Record. Added for TEAM, rev 7399. */
	private boolean splitByEor;

	/**  */
	private boolean onlyTransferCompleted;

	/** Whether to disable PayBreakdown line items. */
	private boolean disablePayBreakdownLines;

	/** Text to place on printed box rental form */
	private String boxRentalText;

	/** Text to place on other printed form(s). */
	private String otherText;

	/** Name for app to use if "login" required. */
	private String loginName;

	/** Password for app to use if "login" required. */
	private String password;

	/** The ServiceMethod to be used when sending batches to this payroll service. */
	private ServiceMethod sendBatchMethod;

	/** Default email address for batch data transfer. */
	private String batchEmailAddress;

	/** URL for authorization request */
	private String authUrl;

	/** URL for transferring a batch of timecards. */
	private String batchUrl;

	/** URL for querying the status of a batch or timecard. */
	private String statusUrl;

	/** User name for app to use for FTP transfers. */
	private String ftpLoginName;

	/** Password for app to use for FTP transfers. */
	private String ftpPassword;

	/** The server domain name for (s)FTP file transfers. */
	private String ftpDomain;

	/** The port on which to connect to the SFTP or FTPS server. */
	private Integer ftpPort = 22;

	/** The directory to receive (s)FTP file transfers. */
	private String ftpDirectory;

	/** The ref= parameter value passed to our login page to trigger branding
	 * of the login page. */
	private String referParam;

	/** Any unique portion of the referring URL passed to our login page to trigger branding
	 * of the login page. */
	private String referUrlPart;

	private String wtpaPayrollCompany;

	private String wtpaWorkersComp;

	//	private Set<Production> productions = new HashSet<Production>(0);
	/** A transient flag, for the Payroll Service list page, to track which item is
	 * currently selected. */
	@Transient
	private boolean selected = false;

	// Constructors

	/** default constructor */
	public PayrollService() {
		sendBatchMethod = ServiceMethod.PDF_ONLY;
		teamPayroll = false;
		name = "New Service";
		timeZoneStr = "US/Pacific";
		sendOnlyChanges = true;
		breakByDay = false;
		splitByEor = false;
		onlyTransferCompleted = false;
	}

	/** minimal constructor */
	public PayrollService(String name, Boolean sendOnlyChanges) {
		this.name = name;
		this.sendOnlyChanges = sendOnlyChanges;
	}

	// Property accessors

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Report_Logo_Id")
	public Image getReportLogo() {
		return reportLogo;
	}
	public void setReportLogo(Image imageByReportLogoId) {
		reportLogo = imageByReportLogoId;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Desktop_Logo_Id")
	public Image getDesktopLogo() {
		return desktopLogo;
	}
	public void setDesktopLogo(Image imageByDesktopLogoId) {
		desktopLogo = imageByDesktopLogoId;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Mobile_Logo_Id")
	public Image getMobileLogo() {
		return mobileLogo;
	}
	public void setMobileLogo(Image imageByMobileLogoId) {
		mobileLogo = imageByMobileLogoId;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	/**See {@link #mailingAddress}. */
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Mailing_Address_Id")
	public Address getMailingAddress() {
		return mailingAddress;
	}
	/**See {@link #mailingAddress}. */
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Contact1_Name", length = 50)
	public String getContact1Name() {
		return contact1Name;
	}
	public void setContact1Name(String contact1Name) {
		this.contact1Name = contact1Name;
	}

	@Column(name = "Contact1_Phone", length = 25)
	public String getContact1Phone() {
		return contact1Phone;
	}
	public void setContact1Phone(String contact1Phone) {
		this.contact1Phone = contact1Phone;
	}

	@Column(name = "Contact1_Email", length = 100)
	public String getContact1Email() {
		return contact1Email;
	}
	public void setContact1Email(String contact1Email) {
		this.contact1Email = contact1Email;
	}

	@Column(name = "Contact2_Name", length = 50)
	public String getContact2Name() {
		return contact2Name;
	}
	public void setContact2Name(String contact2Name) {
		this.contact2Name = contact2Name;
	}

	@Column(name = "Contact2_Phone", length = 25)
	public String getContact2Phone() {
		return contact2Phone;
	}
	public void setContact2Phone(String contact2Phone) {
		this.contact2Phone = contact2Phone;
	}

	@Column(name = "Contact2_Email", length = 100)
	public String getContact2Email() {
		return contact2Email;
	}
	public void setContact2Email(String contact2Email) {
		this.contact2Email = contact2Email;
	}

	@Column(name = "Contact3_Name", length = 50)
	public String getContact3Name() {
		return contact3Name;
	}
	public void setContact3Name(String contact3Name) {
		this.contact3Name = contact3Name;
	}

	@Column(name = "Contact3_Phone", length = 25)
	public String getContact3Phone() {
		return contact3Phone;
	}
	public void setContact3Phone(String contact3Phone) {
		this.contact3Phone = contact3Phone;
	}

	@Column(name = "Contact3_Email", length = 100)
	public String getContact3Email() {
		return contact3Email;
	}
	public void setContact3Email(String contact3Email) {
		this.contact3Email = contact3Email;
	}

	/** See {@link #toursNotificationEmail}. */
	@Column(name = "Tours_Notification_Email")
	public String getToursNotificationEmail() {
		return toursNotificationEmail;
	}

	/** See {@link #toursNotificationEmail}. */
	public void setToursNotificationEmail(String toursNotificationEmail) {
		this.toursNotificationEmail = toursNotificationEmail;
	}

	/**See {@link #timeZoneStr}. */
	@Column(name = "Time_Zone", nullable = false, length = 50)
	public String getTimeZoneStr() {
		return timeZoneStr;
	}
	/**See {@link #timeZoneStr}. */
	public void setTimeZoneStr(String tz) {
		if (tz == null || ! tz.equals(timeZoneStr)) {
			timeZoneStr = tz;
			timeZone = null;
			timeZoneName = null;
		}
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

	/** See {@link #disablePayBreakdownLines}. */
	@Column(name = "Disable_PayBreakdown_Lines", nullable = false)
	public boolean getDisablePayBreakdownLines() {
		return disablePayBreakdownLines;
	}
	/** See {@link #disablePayBreakdownLines}. */
	public void setDisablePayBreakdownLines(boolean disablePayBreakdownLines) {
		this.disablePayBreakdownLines = disablePayBreakdownLines;
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

	/**See {@link #timeZone}. */
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

	/**See {@link #timeZoneName}. */
	@Transient
	public String getTimeZoneName() {
		if (timeZoneName == null) {
			timeZoneName = TimeZoneUtils.getTimeZoneName(timeZoneStr);
		}
		return timeZoneName;
	}

	/** See {@link #teamPayroll}. */
	@Column(name = "Team_Payroll", nullable = false)
	public boolean getTeamPayroll() {
		return teamPayroll;
	}
	/** See {@link #teamPayroll}. */
	public void setTeamPayroll(boolean teamPayroll) {
		this.teamPayroll = teamPayroll;
	}

	@Column(name = "Send_Only_Changes", nullable = false)
	public boolean getSendOnlyChanges() {
		return sendOnlyChanges;
	}
	public void setSendOnlyChanges(boolean sendOnlyChanges) {
		this.sendOnlyChanges = sendOnlyChanges;
	}

	/** See {@link #breakByDay}. */
	@Column(name = "Break_By_Day", nullable = false)
	public boolean getBreakByDay() {
		return breakByDay;
	}
	/** See {@link #breakByDay}. */
	public void setBreakByDay(boolean breakByDay) {
		this.breakByDay = breakByDay;
	}

	/** See {@link #splitByEor}. */
	@Column(name = "Split_By_Eor", nullable = false)
	public boolean getSplitByEor() {
		return splitByEor;
	}
	/** See {@link #splitByEor}. */
	public void setSplitByEor(boolean splitByEor) {
		this.splitByEor = splitByEor;
	}

	/** See {@link #onlyTransferCompleted}. */
	@Column(name = "Only_Transfer_Completed", nullable = false)
	public boolean getOnlyTransferCompleted() {
		return onlyTransferCompleted;
	}
	/** See {@link #onlyTransferCompleted}. */
	public void setOnlyTransferCompleted(boolean onlyTransferCompleted) {
		this.onlyTransferCompleted = onlyTransferCompleted;
	}

	/**See {@link #boxRentalText}. */
	@Column(name = "Box_Rental_Text", length = 5000)
	public String getBoxRentalText() {
		return boxRentalText;
	}
	/**See {@link #boxRentalText}. */
	public void setBoxRentalText(String boxRentalText) {
		this.boxRentalText = boxRentalText;
	}

	/**See {@link #otherText}. */
	@Column(name = "Other_Text", length = 5000)
	public String getOtherText() {
		return otherText;
	}
	/**See {@link #otherText}. */
	public void setOtherText(String otherText) {
		this.otherText = otherText;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Send_Batch_Method", length = 30)
	public ServiceMethod getSendBatchMethod() {
		return sendBatchMethod;
	}
	public void setSendBatchMethod(ServiceMethod sendBatchMethod) {
		this.sendBatchMethod = sendBatchMethod;
	}

	/** See {@link #batchEmailAddress}. */
	@Column(name = "Batch_Email_Address", length = 100)
	public String getBatchEmailAddress() {
		return batchEmailAddress;
	}
	/** See {@link #batchEmailAddress}. */
	public void setBatchEmailAddress(String batchEmailAddress) {
		this.batchEmailAddress = batchEmailAddress;
	}

	@Column(name = "Login_Name", length = 30)
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	@Column(name = "Password", length = 30)
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Column(name = "Auth_Url", length = 200)
	public String getAuthUrl() {
		return authUrl;
	}
	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	@Column(name = "Batch_Url", length = 200)
	public String getBatchUrl() {
		return batchUrl;
	}
	public void setBatchUrl(String batchUrl) {
		this.batchUrl = batchUrl;
	}

	@Column(name = "Status_Url", length = 200)
	public String getStatusUrl() {
		return statusUrl;
	}
	public void setStatusUrl(String statusUrl) {
		this.statusUrl = statusUrl;
	}

	/**See {@link #ftpLoginName}. */
	@Column(name = "Ftp_Login_Name", length = 30)
	public String getFtpLoginName() {
		return ftpLoginName;
	}
	/**See {@link #ftpLoginName}. */
	public void setFtpLoginName(String ftpLoginName) {
		this.ftpLoginName = ftpLoginName;
	}

	/**See {@link #ftpPassword}. */
	@Column(name = "Ftp_Password", length = 30)
	public String getFtpPassword() {
		return ftpPassword;
	}
	/**See {@link #ftpPassword}. */
	public void setFtpPassword(String ftpPassword) {
		this.ftpPassword = ftpPassword;
	}

	/**See {@link #ftpDomain}. */
	@Column(name = "Ftp_Domain", length = 100)
	public String getFtpDomain() {
		return ftpDomain;
	}
	/**See {@link #ftpDomain}. */
	public void setFtpDomain(String ftpDomain) {
		this.ftpDomain = ftpDomain;
	}

	/** See {@link #ftpPort}. */
	@Column(name = "Ftp_Port", nullable = true)
	public Integer getFtpPort() {
		return ftpPort;
	}
	/** See {@link #ftpPort}. */
	public void setFtpPort(Integer ftpPort) {
		this.ftpPort = ftpPort;
	}

	/**See {@link #ftpDirectory}. */
	@Column(name = "Ftp_Directory", length = 100)
	public String getFtpDirectory() {
		return ftpDirectory;
	}
	/**See {@link #ftpDirectory}. */
	public void setFtpDirectory(String ftpDirectory) {
		this.ftpDirectory = ftpDirectory;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "payrollService")
//	public Set<Production> getProductions() {
//		return productions;
//	}
//	public void setProductions(Set<Production> productions) {
//		this.productions = productions;
//	}

	/** See {@link #referParam}. */
	@Column(name = "Refer_Param", length = 10)
	public String getReferParam() {
		return referParam;
	}
	/** See {@link #referParam}. */
	public void setReferParam(String referParam) {
		this.referParam = referParam;
	}

	/** See {@link #referUrlPart}. */
	@Column(name = "Refer_Url_Part", length = 50)
	public String getReferUrlPart() {
		return referUrlPart;
	}
	/** See {@link #referUrlPart}. */
	public void setReferUrlPart(String referUrlText) {
		referUrlPart = referUrlText;
	}

	@Column(name = "Wtpa_Payroll_Company", length = 300)
	/** See {@link #wtpaPayrollCompany}. */
	public String getWtpaPayrollCompany() {
		return wtpaPayrollCompany;
	}
	/** See {@link #wtpaPayrollCompany}. */
	public void setWtpaPayrollCompany(String wtpaPayrollCompany) {
		this.wtpaPayrollCompany = wtpaPayrollCompany;
	}

	@Column(name = "Wtpa_Workers_Comp", length = 300)
	/** See {@link #wtpaWorkersComp}. */
	public String getWtpaWorkersComp() {
		return wtpaWorkersComp;
	}
	/** See {@link #wtpaWorkersComp}. */
	public void setWtpaWorkersComp(String wtpaWorkersComp) {
		this.wtpaWorkersComp = wtpaWorkersComp;
	}

	/**See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/**See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(PayrollService other) {
		if (other == null) {
			return 1;
		}
		return StringUtils.compare(getName(),other.getName());
	}

	public int compareTo(PayrollService other, String sortField, boolean ascending) {
		int ret = 0;
		if (sortField == null || sortField.equals(SORTKEY_NAME)) {
			// will sort by name at end
		}
		else if (sortField.equals(SORTKEY_ID)) {
			ret = NumberUtils.compare(getId(), other.getId());
		}
		else if (sortField.equals(SORTKEY_CONTACT1)) {
			ret = StringUtils.compare(getContact1Name(), other.getContact1Name());
		}
		else {
			log.error("unexpected sort field=" + sortField);
		}
		if (ret == 0) {
			ret = compareTo(other);
		}
		if ( ! ascending ) {
			ret = 0 - ret;	// swap 1 and -1 return values
		}
		return ret;
	}

}
