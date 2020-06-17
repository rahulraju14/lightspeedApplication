package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.AlienUscisType;
import com.lightspeedeps.type.CitizenType;
import com.lightspeedeps.util.common.StringUtils;

/** This entity contains all the fields of the I9 form.
 * It is used to maintain the user wise record of the I9 entries.
 * @author root
 *
 */
@Entity
@Table(name = "form_i9")
public class FormI9 extends Form<FormI9> {

	private static final long serialVersionUID = -7103827011204716662L;

	public static final Byte I9_VERSION_2013 = 1; // 1 = 2013 version
	public static       Byte I9_VERSION_2017 = 2; // 2 = 2017 version
	public static       Byte I9_VERSION_2017_B = 3; // 3 = 2017-09 version
	public static       Byte I9_VERSION_2020 = 4; // 3 = 2020 version
	public static final Byte DEFAULT_I9_VERSION = I9_VERSION_2017_B;

	private String lastName;

	private String firstName;

	private String middleName;

	private String otherName;

	/** Lightspeed account# (User.account) */
	private String userAccount;

	/** Employee's Address field */
	private Address address;

	private Date dateOfBirth;

	private String socialSecurity;

	private String emailAddress;

	private String telephoneNumber;

	private String aptNumber;

	private CitizenType citizenshipStatus;

	private String alienRegNumber1;

	private AlienUscisType alienUscis1;

	private String alienRegNumber2;

	private AlienUscisType alienUscis2;

	private Date workAuthExpirationDate;

	/** LS-4278 String that will allow the employee to enter N/A or enter date */
	private String  alienWorkExpirationDate;
	private String formI9AdmissionNo;

	private String foreignPassportNo;

	private String foreignCountryOfIssuance;

	private boolean preparerNotUsed;

	private boolean preparerUsed;

	private Integer numberOfPreparers;

	private String preparerLastName;

	private String preparerFirstName;

	private Address preparerAddress;

	//private Date prepSignatureDate;

	//private String fullName;

	private String sec2LastName;

	private String sec2FirstName;

	private String sec2MiddleInitial;

	private CitizenType sec2CitizenshipStatus;

	private String additionalInformation;

	private String a1DocTitle;

	private String a1IssuingAuth;

	private String a1DocNumber;

	private Date a1DocExpiration;

	private String a2DocTitle;

	private String a2IssuingAuth;

	private String a2DocNumber;

	private Date a2DocExpiration;

	private String a3DocTitle;

	private String a3IssuingAuth;

	private String a3DocNumber;

	private Date a3DocExpiration;

	private String bDocTitle;

	private String bIssuingAuth;

	private String bDocNumber;

	private Date bDocExpiration;

	private String cDocTitle;

	private String cIssuingAuth;

	private String cDocNumber;

	private Date cDocExpiration;

	private Date firstDayOfEmployment;

	private String empTitle;

	private String empLastName;

	private String empFirstName;

	private String empBusinessName;

	/** Employer's Address field */
	private Address empAddress;

	private String sec3NewLastName;

	private String sec3NewFirstName;

	private String sec3NewMiddleInitial;

	private String sec3LastName;

	private String sec3FirstName;

	private String sec3MiddleInitial;

	private Date sec3RehireDate;

	private String sec3DocTitle;

	private String sec3DocNumber;

	private Date sec3DocExpiration;

	private String sec3EmpName;

	private Contact contact;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	public FormI9() {
		super();
		setVersion(DEFAULT_I9_VERSION);
	}

	public void setDefaults() {
		setCitizenshipStatus(CitizenType.CITIZEN);
		setAlienRegNumber1(Form.NOT_APPLICABLE);
		setAlienUscis1(AlienUscisType.NA);
		setWorkAuthExpirationDate(null);
		setAlienWorkExpirationDate(Form.NOT_APPLICABLE);
		setAlienRegNumber2(Form.NOT_APPLICABLE);
		setAlienUscis2(AlienUscisType.NA);
		setFormI9AdmissionNo(Form.NOT_APPLICABLE);
		setForeignPassportNo(Form.NOT_APPLICABLE);
		setForeignCountryOfIssuance(Form.NOT_APPLICABLE);
	}

	@Column(name = "Last_Name", nullable = true, length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column(name = "First_Name", nullable = true, length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name = "Middle_Name", nullable = true, length = 30)
	public String getMiddleName() {
		return middleName;
	}
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	@Column(name = "Other_Name", nullable = true, length = 200)
	public String getOtherName() {
		return otherName;
	}
	public void setOtherName(String otherName) {
		this.otherName = otherName;
	}

	@Column(name = "User_Account", nullable = true, length = 20)
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	/** See {@link #address}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	/** See {@link #address}. */
	public void setAddress(Address address) {
		this.address = address;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Date_Of_Birth", nullable = true, length = 10)
	public Date getDateOfBirth() {
		return dateOfBirth;
	}
	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	public void setSocialSecurity(String socialSecurity) {
		viewSSN = null;
		this.socialSecurity = socialSecurity;
	}

	/** See {@link #viewSSN}. */
	@Transient
	public String getViewSSN() {
		if (getSocialSecurity() == null) {
			viewSSN = null;
		}
		else if (viewSSN == null) {
			if (getSocialSecurity().length() >= 4) {
				viewSSN = "###-##-" + getSocialSecurity().substring(getSocialSecurity().length()-4);
			}
			else {
				viewSSN = getSocialSecurity();
			}
		}
		return viewSSN;
	}

	@Column(name = "Email_Address", nullable = true, length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Column(name = "Telephone_Number", nullable = true, length = 30)
	public String getTelephoneNumber() {
		return telephoneNumber;
	}
	public void setTelephoneNumber(String telephoneNumber) {
		this.telephoneNumber = telephoneNumber;
	}

	@Column(name = "Apt_Number", nullable = true, length = 15)
	public String getAptNumber() {
		return aptNumber;
	}
	public void setAptNumber(String aptNumber) {
		this.aptNumber = aptNumber;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Citizenship_Status" , nullable = true, length = 30)
	public CitizenType getCitizenshipStatus() {
		return citizenshipStatus;
	}
	public void setCitizenshipStatus(CitizenType citizenshipStatus) {
		this.citizenshipStatus = citizenshipStatus;
	}

	@Column(name = "Alien_Reg_Number1", nullable = true, length = 15)
	public String getAlienRegNumber1() {
		return alienRegNumber1;
	}
	public void setAlienRegNumber1(String alienRegNumber1) {
		this.alienRegNumber1 = alienRegNumber1;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Alien_Uscis1", nullable = true, length = 30)
	public AlienUscisType getAlienUscis1() {
		return alienUscis1;
	}
	public void setAlienUscis1(AlienUscisType alienUscis1) {
		this.alienUscis1 = alienUscis1;
	}

	@Column(name = "Alien_Reg_Number2", nullable = true, length = 15)
	public String getAlienRegNumber2() {
		return alienRegNumber2;
	}
	public void setAlienRegNumber2(String alienRegNumber2) {
		this.alienRegNumber2 = alienRegNumber2;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Alien_Uscis2", nullable = true, length = 30)
	public AlienUscisType getAlienUscis2() {
		return alienUscis2;
	}
	public void setAlienUscis2(AlienUscisType alienUscis2) {
		this.alienUscis2 = alienUscis2;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Work_Auth_Expiration_Date", length = 0)
	public Date getWorkAuthExpirationDate() {
		return workAuthExpirationDate;
	}
	public void setWorkAuthExpirationDate(Date workAuthExpirationDate) {
		this.workAuthExpirationDate = workAuthExpirationDate;
	}

	/** See {@link #alienWorkExpirationDate}. */
	@Column(name = " Alien_Work_Expiration_Date")
	public String getAlienWorkExpirationDate() {
		return alienWorkExpirationDate;
	}

	/** See {@link #alienWorkExpirationDate}. */
	public void setAlienWorkExpirationDate(String alienWorkExpirationDate) {
		this.alienWorkExpirationDate = alienWorkExpirationDate;
	}

	@Column(name = "Form_I9_Admission_No", nullable = true, length = 15)
	public String getFormI9AdmissionNo() {
		return formI9AdmissionNo;
	}
	public void setFormI9AdmissionNo(String formI9AdmissionNo) {
		this.formI9AdmissionNo = formI9AdmissionNo;
	}

	@Column(name = "Foreign_Passport_No", nullable = true, length = 15)
	public String getForeignPassportNo() {
		return foreignPassportNo;
	}
	public void setForeignPassportNo(String foreignPassportNo) {
		this.foreignPassportNo = foreignPassportNo;
	}

	@Column(name = "Foreign_Country_Of_Issuance", nullable = true, length = 30)
	public String getForeignCountryOfIssuance() {
		return foreignCountryOfIssuance;
	}
	public void setForeignCountryOfIssuance(String foreignCountryOfIssuance) {
		this.foreignCountryOfIssuance = foreignCountryOfIssuance;
	}

	@Column(name = "Preparer_Not_Used", nullable = false)
	public boolean getPreparerNotUsed() {
		return preparerNotUsed;
	}
	public void setPreparerNotUsed(boolean preparerNotUsed) {
		this.preparerNotUsed = preparerNotUsed;
	}

	@Column(name = "Preparer_Used", nullable = false)
	public boolean getPreparerUsed() {
		return preparerUsed;
	}
	public void setPreparerUsed(boolean preparerUsed) {
		this.preparerUsed = preparerUsed;
	}

	@Column(name = "Number_Of_Preparers", nullable = true)
	public Integer getNumberOfPreparers() {
		return numberOfPreparers;
	}
	public void setNumberOfPreparers(Integer numberOfPreparers) {
		this.numberOfPreparers = numberOfPreparers;
	}

	@Column(name = "Preparer_Last_Name", nullable = true, length = 30)
	public String getPreparerLastName() {
		return preparerLastName;
	}
	public void setPreparerLastName(String preparerLastName) {
		this.preparerLastName = preparerLastName;
	}

	@Column(name = "Preparer_First_Name", nullable = true, length = 30)
	public String getPreparerFirstName() {
		return preparerFirstName;
	}
	public void setPreparerFirstName(String preparerFirstName) {
		this.preparerFirstName = preparerFirstName;
	}

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Preparer_Address_Id")
	public Address getPreparerAddress() {
		return preparerAddress;
	}
	public void setPreparerAddress(Address preparerAddress) {
		this.preparerAddress = preparerAddress;
	}

	@Column(name = "Sec2_Last_Name", nullable = true, length = 30)
	public String getSec2LastName() {
		return sec2LastName;
	}
	public void setSec2LastName(String sec2LastName) {
		this.sec2LastName = sec2LastName;
	}

	@Column(name = "Sec2_First_Name", nullable = true, length = 30)
	public String getSec2FirstName() {
		return sec2FirstName;
	}
	public void setSec2FirstName(String sec2FirstName) {
		this.sec2FirstName = sec2FirstName;
	}

	@Column(name = "Sec2_Middle_Initial", nullable = true, length = 30)
	public String getSec2MiddleInitial() {
		return sec2MiddleInitial;
	}
	public void setSec2MiddleInitial(String sec2MiddleInitial) {
		this.sec2MiddleInitial = sec2MiddleInitial;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Sec2_Citizenship_Status" , nullable = true, length = 30)
	public CitizenType getSec2CitizenshipStatus() {
		return sec2CitizenshipStatus;
	}
	public void setSec2CitizenshipStatus(CitizenType sec2CitizenshipStatus) {
		this.sec2CitizenshipStatus = sec2CitizenshipStatus;
	}

	/**
	 * @return The numeric value of the Section 2 Citizenship/Immigration status
	 *         field, from 1 to 4. This should match the line number selected by
	 *         the employee in Section 1.
	 */
	@Transient
	public int getSec2CitizenshipStatusNumber() {
		if (getSec2CitizenshipStatus() == null) {
			return 0;
		}
		return getSec2CitizenshipStatus().ordinal() + 1;
	}

	@Column(name = "Additional_Information", nullable = true, length = 2000)
	public String getAdditionalInformation() {
		return additionalInformation;
	}
	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	@Transient
	public String getFullName() {
		String fullName = "";
		if (getLastName() != null) {
			fullName = getLastName();
		}
		if (getFirstName() != null) {
			fullName += ", " + getFirstName();
		}
		if (getMiddleName() != null && ! getMiddleName().equals("N/A")) {
			fullName = fullName + " " + getMiddleName();
		}
		return fullName;
	}

	@Column(name = "A1_Doc_Title", nullable = true, length = 100)
	public String getA1DocTitle() {
		return a1DocTitle;
	}
	public void setA1DocTitle(String a1DocTitle) {
		this.a1DocTitle = a1DocTitle;
	}

	@Transient
	public boolean getEmptyA1DocTitle() {
		return Form.isEmptyNA(a1DocTitle);
	}

	@Column(name = "A1_Issuing_Auth", nullable = true, length = 30)
	public String getA1IssuingAuth() {
		return a1IssuingAuth;
	}
	public void setA1IssuingAuth(String a1IssuingAuth) {
		this.a1IssuingAuth = a1IssuingAuth;
	}

	@Column(name = "A1_Doc_Number", nullable = true, length = 30)
	public String getA1DocNumber() {
		return a1DocNumber;
	}
	public void setA1DocNumber(String a1DocNumber) {
		this.a1DocNumber = a1DocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "A1_Doc_Expiration", length = 0)
	public Date getA1DocExpiration() {
		return a1DocExpiration;
	}
	public void setA1DocExpiration(Date a1DocExpiration) {
		this.a1DocExpiration = a1DocExpiration;
	}

	@Column(name = "A2_Doc_Title", nullable = true, length = 100)
	public String getA2DocTitle() {
		return a2DocTitle;
	}
	public void setA2DocTitle(String a2DocTitle) {
		this.a2DocTitle = a2DocTitle;
	}

	@Transient
	public boolean getEmptyA2DocTitle() {
		return Form.isEmptyNA(a2DocTitle);
	}

	@Column(name = "A2_Issuing_Auth", nullable = true, length = 30)
	public String getA2IssuingAuth() {
		return a2IssuingAuth;
	}
	public void setA2IssuingAuth(String a2IssuingAuth) {
		this.a2IssuingAuth = a2IssuingAuth;
	}

	@Column(name = "A2_Doc_Number", nullable = true, length = 30)
	public String getA2DocNumber() {
		return a2DocNumber;
	}
	public void setA2DocNumber(String a2DocNumber) {
		this.a2DocNumber = a2DocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "A2_Doc_Expiration", length = 0)
	public Date getA2DocExpiration() {
		return a2DocExpiration;
	}
	public void setA2DocExpiration(Date a2DocExpiration) {
		this.a2DocExpiration = a2DocExpiration;
	}

	@Column(name = "A3_Doc_Title", nullable = true, length = 100)
	public String getA3DocTitle() {
		return a3DocTitle;
	}
	public void setA3DocTitle(String a3DocTitle) {
		this.a3DocTitle = a3DocTitle;
	}

	@Transient
	public boolean getEmptyA3DocTitle() {
		return Form.isEmptyNA(a3DocTitle);
	}

	@Column(name = "A3_Issuing_Auth", nullable = true, length = 30)
	public String getA3IssuingAuth() {
		return a3IssuingAuth;
	}
	public void setA3IssuingAuth(String a3IssuingAuth) {
		this.a3IssuingAuth = a3IssuingAuth;
	}

	@Column(name = "A3_Doc_Number", nullable = true, length = 30)
	public String getA3DocNumber() {
		return a3DocNumber;
	}
	public void setA3DocNumber(String a3DocNumber) {
		this.a3DocNumber = a3DocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "A3_Doc_Expiration", length = 0)
	public Date getA3DocExpiration() {
		return a3DocExpiration;
	}
	public void setA3DocExpiration(Date a3DocExpiration) {
		this.a3DocExpiration = a3DocExpiration;
	}

	@Column(name = "B_Doc_Title", nullable = true, length = 100)
	public String getBDocTitle() {
		return bDocTitle;
	}
	public void setBDocTitle(String bDocTitle) {
		this.bDocTitle = bDocTitle;
	}

	@Transient
	public boolean getEmptyBDocTitle() {
		return Form.isEmptyNA(bDocTitle);
	}

	@Column(name = "B_Issuing_Auth", nullable = true, length = 30)
	public String getBIssuingAuth() {
		return bIssuingAuth;
	}
	public void setBIssuingAuth(String bIssuingAuth) {
		this.bIssuingAuth = bIssuingAuth;
	}

	@Column(name = "B_Doc_Number", nullable = true, length = 30)
	public String getBDocNumber() {
		return bDocNumber;
	}
	public void setBDocNumber(String bDocNumber) {
		this.bDocNumber = bDocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "B_Doc_Expiration", length = 0)
	public Date getBDocExpiration() {
		return bDocExpiration;
	}
	public void setBDocExpiration(Date bDocExpiration) {
		this.bDocExpiration = bDocExpiration;
	}

	@Column(name = "C_Doc_Title", nullable = true, length = 100)
	public String getCDocTitle() {
		return cDocTitle;
	}
	public void setCDocTitle(String cDocTitle) {
		this.cDocTitle = cDocTitle;
	}

	@Transient
	public boolean getEmptyCDocTitle() {
		return Form.isEmptyNA(cDocTitle);
	}

	@Column(name = "C_Issuing_Auth", nullable = true, length = 30)
	public String getCIssuingAuth() {
		return cIssuingAuth;
	}
	public void setCIssuingAuth(String cIssuingAuth) {
		this.cIssuingAuth = cIssuingAuth;
	}

	@Column(name = "C_Doc_Number", nullable = true, length = 30)
	public String getCDocNumber() {
		return cDocNumber;
	}
	public void setCDocNumber(String cDocNumber) {
		this.cDocNumber = cDocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "C_Doc_Expiration", length = 0)
	public Date getCDocExpiration() {
		return cDocExpiration;
	}
	public void setCDocExpiration(Date cDocExpiration) {
		this.cDocExpiration = cDocExpiration;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "First_Day_Of_Employment", length = 0)
	public Date getFirstDayOfEmployment() {
		return firstDayOfEmployment;
	}
	public void setFirstDayOfEmployment(Date firstDayOfEmployment) {
		this.firstDayOfEmployment = firstDayOfEmployment;
	}

	@Column(name = "Emp_Title", nullable = true, length = 100)
	public String getEmpTitle() {
		return empTitle;
	}
	public void setEmpTitle(String empTitle) {
		this.empTitle = empTitle;
	}

	@Column(name = "Emp_Last_Name", nullable = true, length = 30)
	public String getEmpLastName() {
		return empLastName;
	}
	public void setEmpLastName(String empLastName) {
		this.empLastName = empLastName;
	}

	@Column(name = "Emp_First_Name", nullable = true, length = 30)
	public String getEmpFirstName() {
		return empFirstName;
	}
	public void setEmpFirstName(String empFirstName) {
		this.empFirstName = empFirstName;
	}

	@Column(name = "Emp_Business_Name", nullable = true, length = 30)
	public String getEmpBusinessName() {
		return empBusinessName;
	}
	public void setEmpBusinessName(String empBusinessName) {
		this.empBusinessName = empBusinessName;
	}

	/** See {@link #empAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Emp_Address_Id")
	public Address getEmpAddress() {
		return empAddress;
	}
	/** See {@link #empAddress}. */
	public void setEmpAddress(Address empAddress) {
		this.empAddress = empAddress;
	}

	@Column(name = "Sec3_New_Last_Name", nullable = true, length = 30)
	public String getSec3NewLastName() {
		return sec3NewLastName;
	}
	public void setSec3NewLastName(String sec3NewLastName) {
		this.sec3NewLastName = sec3NewLastName;
	}

	@Column(name = "Sec3_New_First_Name", nullable = true, length = 30)
	public String getSec3NewFirstName() {
		return sec3NewFirstName;
	}
	public void setSec3NewFirstName(String sec3NewFirstName) {
		this.sec3NewFirstName = sec3NewFirstName;
	}

	@Column(name = "Sec3_New_Middle_Initial", nullable = true, length = 30)
	public String getSec3NewMiddleInitial() {
		return sec3NewMiddleInitial;
	}
	public void setSec3NewMiddleInitial(String sec3NewMiddleInitial) {
		this.sec3NewMiddleInitial = sec3NewMiddleInitial;
	}

	@Column(name = "Sec3_Last_Name", nullable = true, length = 30)
	public String getSec3LastName() {
		return sec3LastName;
	}
	public void setSec3LastName(String sec3LastName) {
		this.sec3LastName = sec3LastName;
	}

	@Column(name = "Sec3_First_Name", nullable = true, length = 30)
	public String getSec3FirstName() {
		return sec3FirstName;
	}
	public void setSec3FirstName(String sec3FirstName) {
		this.sec3FirstName = sec3FirstName;
	}

	@Column(name = "Sec3_Middle_Initial", nullable = true, length = 30)
	public String getSec3MiddleInitial() {
		return sec3MiddleInitial;
	}
	public void setSec3MiddleInitial(String sec3MiddleInitial) {
		this.sec3MiddleInitial = sec3MiddleInitial;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Sec3_Rehire_Date", length = 0)
	public Date getSec3RehireDate() {
		return sec3RehireDate;
	}
	public void setSec3RehireDate(Date sec3RehireDate) {
		this.sec3RehireDate = sec3RehireDate;
	}

	@Column(name = "Sec3_Doc_Title", nullable = true, length = 100)
	public String getSec3DocTitle() {
		return sec3DocTitle;
	}
	public void setSec3DocTitle(String sec3DocTitle) {
		this.sec3DocTitle = sec3DocTitle;
	}

	@Column(name = "Sec3_Doc_Number", nullable = true, length = 30)
	public String getSec3DocNumber() {
		return sec3DocNumber;
	}
	public void setSec3DocNumber(String sec3DocNumber) {
		this.sec3DocNumber = sec3DocNumber;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Sec3_Doc_Expiration", length = 0)
	public Date getSec3DocExpiration() {
		return sec3DocExpiration;
	}
	public void setSec3DocExpiration(Date sec3DocExpiration) {
		this.sec3DocExpiration = sec3DocExpiration;
	}

	@Column(name = "Sec3_Emp_Name", nullable = true, length = 30)
	public String getSec3EmpName() {
		return sec3EmpName;
	}
	public void setSec3EmpName(String sec3EmpName) {
		this.sec3EmpName = sec3EmpName;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}

//	@Override
//	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
	// Never used; deleted code in LS-1286
//	}

	/**
	 * Export the fields in this FormW4 using the supplied Exporter.
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append(getVersion());
		ex.append(getLastName());
		ex.append(getFirstName());
		ex.append(getMiddleName());
		ex.append(getOtherName());
		Address addr = getAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);
		ex.append(getAptNumber());
		ex.append(getDateOfBirth());
		ex.append(getSocialSecurity());
		ex.append(getEmailAddress());
		ex.append(getTelephoneNumber());
		ex.append(getCitizenshipStatus() == null ? "" : getCitizenshipStatus().name());
		ex.append(getAlienRegNumber1());
		ex.append(getAlienUscis1() == null ? "" : getAlienUscis1().name());
		ex.append(getAlienRegNumber2());
		ex.append(getAlienUscis2() == null ? "" : getAlienUscis2().name());
		ex.append(getWorkAuthExpirationDate());
		ex.append(getFormI9AdmissionNo());
		ex.append(getForeignPassportNo());
		ex.append(getForeignCountryOfIssuance());
		ex.append(getPreparerLastName());
		ex.append(getPreparerFirstName());
		addr = getPreparerAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);
		ex.append(getFullName());
		ex.append(getSec2LastName());
		ex.append(getSec2FirstName());
		ex.append(getSec2MiddleInitial());
		ex.append(getSec2CitizenshipStatus() == null ? "" : getSec2CitizenshipStatus().name());
		ex.append(StringUtils.saveHtml(getAdditionalInformation())); // convert line-ends to <br/>
		ex.append(getEmptyA1DocTitle() ? Form.NOT_APPLICABLE : getA1DocTitle());
		ex.append(getA1IssuingAuth());
		ex.append(getA1DocNumber());
		ex.append(getA1DocExpiration());
		ex.append(getEmptyA2DocTitle() ? Form.NOT_APPLICABLE : getA2DocTitle());
		ex.append(getA2IssuingAuth());
		ex.append(getA2DocNumber());
		ex.append(getA2DocExpiration());
		ex.append(getEmptyA3DocTitle() ? Form.NOT_APPLICABLE : getA3DocTitle());
		ex.append(getA3IssuingAuth());
		ex.append(getA3DocNumber());
		ex.append(getA3DocExpiration());
		ex.append(getEmptyBDocTitle() ? Form.NOT_APPLICABLE : getBDocTitle());
		ex.append(getBIssuingAuth());
		ex.append(getBDocNumber());
		ex.append(getBDocExpiration());
		ex.append(getEmptyCDocTitle() ? Form.NOT_APPLICABLE : getCDocTitle());
		ex.append(getCIssuingAuth());
		ex.append(getCDocNumber());
		ex.append(getCDocExpiration());
		ex.append(getFirstDayOfEmployment());
		ex.append(getEmpTitle());
		ex.append(getEmpLastName());
		ex.append(getEmpFirstName());
		ex.append(getEmpBusinessName());
		addr = getEmpAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);
		ex.append(getSec3LastName());
		ex.append(getSec3FirstName());
		ex.append(getSec3MiddleInitial());
		ex.append(getSec3RehireDate());
		ex.append(getSec3DocTitle());
		ex.append(getSec3DocNumber());
		ex.append(getSec3DocExpiration());
		ex.append(getSec3EmpName());
	}

	@Override
	public void trim() {
		firstName = trim(firstName);
		lastName = trim(lastName);
		middleName = trimNA(middleName);
		otherName = trimNA(otherName);
		userAccount = trim(userAccount);
		socialSecurity = trim(socialSecurity);
		address.trimIsEmpty();
		emailAddress = trimNA(emailAddress);
		telephoneNumber = trimNA(telephoneNumber);
		aptNumber = trimNA(aptNumber);
		alienRegNumber1 = trim(alienRegNumber1);
		alienRegNumber2 = trim(alienRegNumber2);
		formI9AdmissionNo = trim(formI9AdmissionNo);
		foreignPassportNo = trim(foreignPassportNo);
		foreignCountryOfIssuance = trim(foreignCountryOfIssuance);

		preparerLastName = trim(preparerLastName);
		preparerFirstName = trim(preparerFirstName);
		preparerAddress.trimIsEmpty();
		//fullName = trim(fullName);
		sec2LastName = trim(sec2LastName);
		sec2FirstName = trim(sec2FirstName);
		sec2MiddleInitial = trim(sec2MiddleInitial);
		a1IssuingAuth = trim(a1IssuingAuth);
		a1DocNumber = trim(a1DocNumber);
		a2IssuingAuth = trim(a2IssuingAuth);
		a2DocNumber = trim(a2DocNumber);
		a3IssuingAuth = trim(a3IssuingAuth);
		a3DocNumber = trim(a3DocNumber);
		bIssuingAuth = trim(bIssuingAuth);
		bDocNumber = trim(bDocNumber);
		cIssuingAuth = trim(cIssuingAuth);
		cDocNumber = trim(cDocNumber);

		empTitle = trim(empTitle);
		empLastName = trim(empLastName);
		empFirstName = trim(empFirstName);
		empBusinessName = trim(empBusinessName);
		empAddress.trimIsEmpty();

		sec3NewLastName = trim(sec3NewLastName);
		sec3NewFirstName = trim(sec3FirstName);
		sec3MiddleInitial = trim(sec3MiddleInitial);
		sec3LastName = trim(sec3LastName);
		sec3FirstName = trim(sec3FirstName);
		sec3EmpName = trim(sec3EmpName);
		sec3DocNumber = trim(sec3DocNumber);
		sec3MiddleInitial = trim(sec3MiddleInitial);
	}

}
