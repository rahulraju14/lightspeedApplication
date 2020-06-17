package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.lightspeedeps.type.TrustAccountType;

/** This entity contains all the fields of the MTA form.
 * It is used to maintain the user wise record of the MTA form entries.
 *
 */
@Entity
@Table(name = "form_mta")
public class FormMTA extends Form<FormMTA> {

	private static final long serialVersionUID = 5684466342813058965L;

	private String nativeState;

	private boolean workedCa;

	private boolean workedNy;

	private boolean workedLa;

	private boolean workedNm;

	private boolean workedNc;

	private boolean workedPn;

	private boolean workedTn;

	private String firstName;

	private String middleName;

	private String lastName;

	private Date dateOfBirth;

	private String socialSecurity;

	private Address address;

	private boolean noTrustAccount;

	private BigDecimal trustPercent;

	private ContactDocEvent trustInitial;

	private String accountName;

	private String trusteeName;

	private TrustAccountType trustAccountType;

	private String bankName;

	private Address bankAddress;

	private String routingNumber;

	private String accountNumber;

	private String bankRepresentative;

	private String bankPhone;

	private boolean parentOrGuardian;

	private String parentName;

	private String parentPhone;

	private String emailAddress;

	private String teamClient;

	private String datesWorked;

	private String productionCompany;

	private String project;

	private String producer;

	private String producerPhone;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	@Column(name = "Native_State", nullable = true, length = 10)
	public String getNativeState() {
		return nativeState;
	}
	public void setNativeState(String nativeState) {
		this.nativeState = nativeState;
	}

	@Column(name = "Worked_Ca", nullable = false)
	public boolean getWorkedCa() {
		return workedCa;
	}
	public void setWorkedCa(boolean workedCa) {
		this.workedCa = workedCa;
	}

	@Column(name = "Worked_Ny", nullable = false)
	public boolean getWorkedNy() {
		return workedNy;
	}
	public void setWorkedNy(boolean workedNy) {
		this.workedNy = workedNy;
	}

	@Column(name = "Worked_La", nullable = false)
	public boolean getWorkedLa() {
		return workedLa;
	}
	public void setWorkedLa(boolean workedLa) {
		this.workedLa = workedLa;
	}

	@Column(name = "Worked_Nm", nullable = false)
	public boolean getWorkedNm() {
		return workedNm;
	}
	public void setWorkedNm(boolean workedNm) {
		this.workedNm = workedNm;
	}

	@Column(name = "Worked_Nc", nullable = false)
	public boolean getWorkedNc() {
		return workedNc;
	}
	public void setWorkedNc(boolean workedNc) {
		this.workedNc = workedNc;
	}

	@Column(name = "Worked_Pn", nullable = false)
	public boolean getWorkedPn() {
		return workedPn;
	}
	public void setWorkedPn(boolean workedPn) {
		this.workedPn = workedPn;
	}

	@Column(name = "Worked_Tn", nullable = false)
	public boolean getWorkedTn() {
		return workedTn;
	}
	public void setWorkedTn(boolean workedTn) {
		this.workedTn = workedTn;
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

	@Column(name = "Last_Name", nullable = true, length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
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

	@Transient
	public String getSSNFormatted() {
		String str = null;
		if (getSocialSecurity() != null && getSocialSecurity().length() == 9) {
			str = socialSecurity.substring(0,3) + "-" + socialSecurity.substring(3, 5) + "-" + socialSecurity.substring(5);
		}
		return str;
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

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@Column(name = "No_Trust_Account" , nullable = false)
	public boolean getNoTrustAccount() {
		return noTrustAccount;
	}
	public void setNoTrustAccount(boolean noTrustAccount) {
		this.noTrustAccount = noTrustAccount;
	}

	@Column(name = "Trust_Percent", nullable = true,  precision = 5, scale = 3)
	public BigDecimal getTrustPercent() {
		return trustPercent;
	}
	public void setTrustPercent(BigDecimal trustPercent) {
		this.trustPercent = trustPercent;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Trust_Initial")
	public ContactDocEvent getTrustInitial() {
		return trustInitial;
	}
	public void setTrustInitial(ContactDocEvent trustInitial) {
		this.trustInitial = trustInitial;
	}

	@Column(name = "Account_Name", nullable = true, length = 30)
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	@Column(name = "Trustee_Name", nullable = true, length = 30)
	public String getTrusteeName() {
		return trusteeName;
	}
	public void setTrusteeName(String trusteeName) {
		this.trusteeName = trusteeName;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Trust_Account_Type" , nullable = true, length = 10)
	public TrustAccountType getTrustAccountType() {
		return trustAccountType;
	}
	public void setTrustAccountType(TrustAccountType trustAccountType) {
		this.trustAccountType = trustAccountType;
	}

	@Column(name = "Bank_Name", nullable = true, length = 30)
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Bank_Address_Id")
	public Address getBankAddress() {
		return bankAddress;
	}
	public void setBankAddress(Address bankAddress) {
		this.bankAddress = bankAddress;
	}

	@Column(name = "Routing_Number", nullable = true, length = 10)
	public String getRoutingNumber() {
		return routingNumber;
	}
	public void setRoutingNumber(String routingNumber) {
		this.routingNumber = routingNumber;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Account_Number", nullable = true, length = 1000)
	public String getAccountNumber() {
		return accountNumber;
	}
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	@Column(name = "Bank_Representative", nullable = true, length = 30)
	public String getBankRepresentative() {
		return bankRepresentative;
	}
	public void setBankRepresentative(String bankRepresentative) {
		this.bankRepresentative = bankRepresentative;
	}

	@Column(name = "Bank_Phone", nullable = true, length = 25)
	public String getBankPhone() {
		return bankPhone;
	}
	public void setBankPhone(String bankPhone) {
		this.bankPhone = bankPhone;
	}

	@Column(name = "Parent_Or_Guardian", nullable = false)
	public boolean isParentOrGuardian() {
		return parentOrGuardian;
	}
	public void setParentOrGuardian(boolean parentOrGuardian) {
		this.parentOrGuardian = parentOrGuardian;
	}

	@Column(name = "Parent_Name", nullable = true, length = 30)
	public String getParentName() {
		return parentName;
	}
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

	@Column(name = "Parent_Phone", nullable = true, length = 25)
	public String getParentPhone() {
		return parentPhone;
	}
	public void setParentPhone(String parentPhone) {
		this.parentPhone = parentPhone;
	}

	@Column(name = "Email_Address", nullable = true, length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Column(name = "Team_Client", nullable = true, length = 30)
	public String getTeamClient() {
		return teamClient;
	}
	public void setTeamClient(String teamClient) {
		this.teamClient = teamClient;
	}

	@Column(name = "Dates_Worked", nullable = true, length = 30)
	public String getDatesWorked() {
		return datesWorked;
	}
	public void setDatesWorked(String datesWorked) {
		this.datesWorked = datesWorked;
	}

	@Column(name = "Production_Company", nullable = true, length = 30)
	public String getProductionCompany() {
		return productionCompany;
	}
	public void setProductionCompany(String productionCompany) {
		this.productionCompany = productionCompany;
	}

	@Column(name = "Project", nullable = true, length = 30)
	public String getProject() {
		return project;
	}
	public void setProject(String project) {
		this.project = project;
	}

	@Column(name = "Producer", nullable = true, length = 30)
	public String getProducer() {
		return producer;
	}
	public void setProducer(String producer) {
		this.producer = producer;
	}

	@Column(name = "Producer_Phone", nullable = true, length = 15)
	public String getProducerPhone() {
		return producerPhone;
	}
	public void setProducerPhone(String producerPhone) {
		this.producerPhone = producerPhone;
	}

	@Override
	public void trim() {
		firstName = trim(firstName);
		lastName = trim(lastName);
		middleName = trim(middleName);
		socialSecurity = trim(socialSecurity);
		address.trimIsEmpty();
		accountName = trim(accountName);
		trusteeName = trim(trusteeName);
		bankName = trim(bankName);
		bankAddress.trimIsEmpty();
		routingNumber = trim(routingNumber);
		accountNumber = trim(accountNumber);
		bankRepresentative = trim(bankRepresentative);
		bankPhone = trim(bankPhone);
		parentName = trim(parentName);
		parentPhone = trim(parentPhone);
		emailAddress = trim(emailAddress);
		teamClient = trim(teamClient);
		datesWorked = trim(datesWorked);
		productionCompany = trim(productionCompany);
		project = trim(project);
		producer = trim(producer);
		producerPhone = trim(producerPhone);
	}

}
