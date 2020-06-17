package com.lightspeedeps.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.lightspeedeps.port.Importer;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.type.FeatureFlagType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.FF4JUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * ContactImport entity. These database entities are use for the temporary
 * storage of data being imported into a Production.
 * @see com.lightspeedeps.web.project.ImportContactsBean
 */
@Entity
@Table(name = "contact_import")
public class ContactImport extends PersistentObject<ContactImport> implements java.io.Serializable, Importable {
	/** Serialization id */
	private static final long serialVersionUID = - 3963963100037340247L;

	// Fields

	/** Production to which this entry will be added. */
	private Production production;

	/** The User.accountNumber of the person who uploaded this data
	 * into the ContactImport table. */
	private String userAccount;

	/** The action to perform with this entry: Add, Replace, or Delete. */
	private ActionType action;

	/** The Project episode code (job number). */
	private String episodeCode;

	/** The Project title (job name) */
	private String episodeTitle;

	/** */
	private String jobId;

	/** The start date for this person. This will be put into
	 * the StartForm as the workStartDate. */
	private Date projectStartDate;

	/** The end date for this person. This will be put into
	 * the StartForm as the workEndDate. */
	private Date projectedEndDate;

	/** The city where the work is to be performed. This will
	 * be stored in the employee's StartForm. */
	private String workCity;

	/** The state where the work is to be performed. This will
	 * be stored in the employee's StartForm. */
	private String workState;

	/** Employee's first name. */
	private String firstName;

	/** Employee's last name. */
	private String lastName;

	/** Employee's email address. This must match their Lightspeed User
	 * email address. */
	private String emailAddress;

	/** encrypted SSN. This will be put into their StartForm. */
	private String socialSecurity;

	/** Employee Phone number. This will be put into their User record. */
	private String phone;

	/** Employee's occupation. This will be used to create the Employment object
	 * as well as the StartForm. A custom Role will be created to match this
	 * if necessary. */
	private String occupation;

	/** The Department to which the employee will be assigned. This is used
	 * when creating the Employment object.  A custom Department will be created
	 * if necessary. */
	private String department;

	/** how pay is calculated - Hourly, Daily, or Weekly */
	private EmployeeRateType rateType = EmployeeRateType.HOURLY;

	/** Studio rate.  It is possible to have different rates for Studio vs Location. */
	private BigDecimal rate;

	/** Guaranteed studio hours, to be set in Start Form. */
	private BigDecimal guarHours;

	/** The name of an onboarding 'packet' to be issued to the imported contact; if null
	 * or blank, or if onboard is not enabled, no distribution is done. */
	private String packetName;

	private String payIndicator;

	private String loanOutName;

	private String fein;

	private String taxClassification;

	// Constructors

	/** default constructor */
	public ContactImport() {
	}

	public ContactImport(Production production, String userAccount) {
		this.production = production;
		this.userAccount = userAccount;
	}

	/** full constructor */
	public ContactImport(Production production, String userAccount, String episodeCode,
			String episodeTitle, String jobId, Date projectStartDate, Date projectedEndDate,
			String workCity, String workState, String firstName, String lastName,
			String emailAddress, String phone, String occupation, String department) {
		this(production, userAccount);
		this.episodeCode = episodeCode;
		this.episodeTitle = episodeTitle;
		this.jobId = jobId;
		this.projectStartDate = projectStartDate;
		this.projectedEndDate = projectedEndDate;
		this.workCity = workCity;
		this.workState = workState;
		this.firstName = firstName;
		this.lastName = lastName;
		this.emailAddress = emailAddress;
		this.phone = phone;
		this.occupation = occupation;
		this.department = department;
	}

	/**
	 * Fill this ContactImport instance with data from the given Importer. Most
	 * data errors are ignored; it is expected that the data will be validated
	 * later.
	 *
	 * @param imp The Importer (data source).
	 * @throws IOException
	 */
	public void imports(Importer imp) throws IOException, ParseException {
		// Note that the content of each 'imp.getString()' call is dependent
		// on the field order in the source file.

		setEpisodeCode(imp.getString().trim());

		try {
			setProjectStartDate(imp.getDate());
		}
		catch (ParseException e) {
			setProjectStartDate(null);
		}

		try {
			setProjectedEndDate(imp.getDate());
		}
		catch (ParseException e) {
			setProjectedEndDate(null);
		}

		setEpisodeTitle(imp.getString().trim());
		setWorkCity(imp.getString().trim());
		setWorkState(imp.getString().trim());
		setFirstName(imp.getString().trim());
		setLastName(imp.getString().trim());
		setEmailAddress(imp.getString().trim());

		setPhone(imp.getString());
		String phon = PhoneNumberValidator.cleanNumber(getPhone());
		if (phon != null && phon.length() != 0) { // valid
			setPhone(phon); // save cleaned value
		}

		setOccupation(imp.getString().trim());
		setDepartment(imp.getString().trim());

		String ssn = StringUtils.cleanTaxId(imp.getString());
		setSocialSecurity(ssn); // invalid (empty) string will be caught later

		String str;
		EmployeeRateType type = EmployeeRateType.HOURLY; // default
		try {
			str = imp.getString().trim();
			if (! StringUtils.isEmpty(str)) {
				type = EmployeeRateType.valueOf(str.toUpperCase());
			}
		}
		catch (Exception e) {
			// null rate type setting will be flagged later.
			type = null;
		}
		setRateType(type);

		BigDecimal payrate = null;
		try {
			// first remove quotes, commas, and blanks
			str = imp.getString().replaceAll(" |\"|,", "");
			if (! StringUtils.isEmpty(str)) {
				payrate = new BigDecimal(str);
			}
		}
		catch (NumberFormatException e) {
			// Zero rate setting will be flagged later.
			payrate = BigDecimal.ZERO;
		}
		setRate(payrate);

		BigDecimal hours = null;
		try {
			// first remove quotes, commas, and blanks
			str = imp.getString().replaceAll(" |\"|,", "");
			if (! StringUtils.isEmpty(str)) {
				hours = new BigDecimal(str);
				if (hours.compareTo(Constants.MAX_WEEKLY_GUAR_HOURS) >= 0) {
					hours = Constants.MAX_WEEKLY_GUAR_HOURS.subtract(BigDecimal.ONE);
				}
			}
		}
		catch (NumberFormatException e) {
			// Zero rate setting will be flagged later.
			hours = BigDecimal.ZERO;
		}
		setGuarHours(hours);

		setPacketName(imp.getString().trim()); // TODO future enhancement

		if (FF4JUtils.useFeature(FeatureFlagType.TTCO_IMPORT_LOANOUT)) {
			// Additional fields expected for Loan-out/Individual distinction on import. LS-3043
			setPayIndicator(imp.getString().trim());
			setLoanOutName(imp.getString().trim());
			setFein(imp.getString().trim());
			setTaxClassification(imp.getString().trim());
		}
	}

	// Property accessors

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getProduction()
	 */
	@Override
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getUserAccount()
	 */
	@Column(name = "User_Account", length = 20)
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getAction()
	 */
	@Override
	@Enumerated(EnumType.STRING)
	@Column(name = "Action", nullable = false)
	public ActionType getAction() {
		return action;
	}
	/** See {@link #action}. */
	public void setAction(ActionType act) {
		action = act;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getEpisodeCode()
	 */
	@Override
	@Column(name = "Episode_Code", length = 30)
	public String getEpisodeCode() {
		return episodeCode;
	}
	public void setEpisodeCode(String episodeCode) {
		this.episodeCode = episodeCode;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getEpisodeTitle()
	 */
	@Override
	@Column(name = "Episode_Title", length = 100)
	public String getEpisodeTitle() {
		return episodeTitle;
	}
	public void setEpisodeTitle(String episodeTitle) {
		this.episodeTitle = episodeTitle;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getJobId()
	 */
	@Override
	@Column(name = "Job_Id", length = 30)
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	@Override
	@Temporal(TemporalType.DATE)
	@Column(name = "Project_Start_Date", length = 10)
	public Date getProjectStartDate() {
		return projectStartDate;
	}
	public void setProjectStartDate(Date projectStartDate) {
		this.projectStartDate = projectStartDate;
	}

	@Override
	@Temporal(TemporalType.DATE)
	@Column(name = "Projected_End_Date", length = 10)
	public Date getProjectedEndDate() {
		return projectedEndDate;
	}
	public void setProjectedEndDate(Date projectedEndDate) {
		this.projectedEndDate = projectedEndDate;
	}

	@Override
	@Column(name = "Work_City", length = 50)
	public String getWorkCity() {
		return workCity;
	}
	public void setWorkCity(String workCity) {
		this.workCity = workCity;
	}

	@Override
	@Column(name = "Work_State", length = 50)
	public String getWorkState() {
		return workState;
	}
	public void setWorkState(String workState) {
		this.workState = workState;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getFirstName()
	 */
	@Override
	@Column(name = "First_Name", length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getLastName()
	 */
	@Override
	@Column(name = "Last_Name", length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	@Transient
	public String getLastNameFirstName() {
		return getLastName() + ", " + getFirstName();
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getEmailAddress()
	 */
	@Override
	@Column(name = "Email_address", length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/** See {@link #socialSecurity} */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	/** See {@link #socialSecurity} */
	public void setSocialSecurity(String socialSecurity) {
		this.socialSecurity = socialSecurity;
	}

	@Override
	@Column(name = "Phone", length = 25)
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getOccupation()
	 */
	@Override
	@Column(name = "Occupation", length = 100)
	public String getOccupation() {
		return occupation;
	}
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Importable#getDepartment()
	 */
	@Override
	@Column(name = "Department", length = 100)
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}

	/**See {@link #rateType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Rate_Type")
	public EmployeeRateType getRateType() {
		return rateType;
	}
	/**See {@link #rateType}. */
	public void setRateType(EmployeeRateType rateType) {
		this.rateType = rateType;
	}

	/**See {@link #rate}. */
	@Column(name = "Rate", precision = 10, scale = 4)
	public BigDecimal getRate() {
		return rate;
	}
	/**See {@link #rate}. */
	public void setRate(BigDecimal Studio) {
		rate = Studio;
	}

	/** See {@link #guarHours}. */
	@Column(name = "Guar_Hours", precision = 4, scale = 2)
	public BigDecimal getGuarHours() {
		return guarHours;
	}
	/** See {@link #guarHours}. */
	public void setGuarHours(BigDecimal guarHours) {
		this.guarHours = guarHours;
	}

	/** See {@link #packetName}. */
	@Column(name = "Packet_Name", length = 100)
	public String getPacketName() {
		return packetName;
	}
	/** See {@link #packetName}. */
	public void setPacketName(String packetName) {
		this.packetName = packetName;
	}

	/** See {@link #loanOutName}. */
	@Column(name = "Loan_Out_Name", length = 100)
	public String getLoanOutName() {
		return loanOutName;
	}
	/** See {@link #loanOutName}. */
	public void setLoanOutName(String loanOutName) {
		this.loanOutName = loanOutName;
	}

	/** See {@link #fein}. */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "FEIN", nullable = true, length = 1000)
	public String getFein() {
		return fein;
	}
	/** See {@link #fein}. */
	public void setFein(String fein) {
		this.fein = fein;
	}

	/** See {@link #taxType}. */
	@Column(name = "Tax_Classification", length = 30)
	public String getTaxClassification() {
		return taxClassification;
	}
	/** See {@link #taxType}. */
	public void setTaxClassification(String taxClassification) {
		this.taxClassification = taxClassification;
	}

	/** See {@link #payIndicator}. */
	@Column(name = "Pay_Indicator", length = 100)
	public String getPayIndicator() {
		return payIndicator;
	}
	/** See {@link #payIndicator}. */
	public void setPayIndicator(String payIndicator) {
		this.payIndicator = payIndicator;
	}

}
