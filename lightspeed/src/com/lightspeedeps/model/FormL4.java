package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.FormFieldType;

/**
 * This entity contains all the fields of the L4 form. It is used to maintain
 * the user wise record of the L4 entries.
 *
 */
@Entity
@Table(name = "form_l4")
public class FormL4 extends Form<FormL4> {

	private static final long serialVersionUID = 192167733349779875L;

	public static final Byte L4_VERSION_2017 = 17; // last 2 digit of the years
	public static final Byte DEFAULT_L4_VERSION = L4_VERSION_2017;

	private Integer blockA;

	private Integer blockB;

	private String firstNameMidInitial;

	private String lastName;

	private String socialSecurity;

	private String maritalStatus;

	private Address address;

	private Integer withheldAmountChange;

	private String employerName;

	private String employerStateAcctNum;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	public FormL4() {
		super();
		setVersion(DEFAULT_L4_VERSION);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		String maritalSingle = "Off";
		String maritalMarried = "Off";
		String maritalExemption = "Off";
		if (getMaritalStatus() != null) {
			switch(getMaritalStatus()) {
				case "ne":
					maritalExemption = "Yes";
					break;
				case "s":
					maritalSingle = "Yes";
					break;
				case "m":
					maritalMarried = "Yes";
					break;
			}
		}

		fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), getFirstNameMidInitial());
		fieldValues.put(FormFieldType.USER_LAST_NAME.name(), getLastName());
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), getAddress().getAddrLine1());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), getAddress().getCity());
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), getAddress().getState());
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), getAddress().getZip());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(),  "");
		}

		fieldValues.put(FormFieldType.L4_BLOCK_A.name(), intFormat(getBlockA()));
		fieldValues.put(FormFieldType.L4_BLOCK_B.name(), intFormat(getBlockB()));
		fieldValues.put(FormFieldType.L4_MARITAL_SINGLE.name(), maritalSingle);
		fieldValues.put(FormFieldType.L4_MARITAL_MARRIED.name(), maritalMarried);
		fieldValues.put(FormFieldType.L4_MARITAL_NO_EXEMPTION.name(), maritalExemption);

		fieldValues.put(FormFieldType.L4_EMP_ACCT_NUM.name(), getEmployerStateAcctNum());
		fieldValues.put(FormFieldType.L4_EMP_NAME_ADDRESS.name(), getEmployerName());
		fieldValues.put(FormFieldType.L4_WITHHELD_ADJUSTMENT.name(), intFormat(getWithheldAmountChange()));

		if (true) { //test for signature
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "Emp-date"); // page 1, Line 6
		}
		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}
	}

	@Override
	public void trim() {
		firstNameMidInitial = trim(firstNameMidInitial);
		lastName = trim(lastName);
		socialSecurity = trim(socialSecurity);
		maritalStatus = trim(maritalStatus);
		address.trimIsEmpty();
		employerName = trim(employerName);
		employerStateAcctNum = trim(employerStateAcctNum);
	}

	/** See {@link #blockA}. */
	@Column(name = "Block_A", nullable = true)
	public Integer getBlockA() {
		return blockA;
	}
	/** See {@link #blockA}. */
	public void setBlockA(Integer blockA) {
		this.blockA = blockA;
	}

	/** See {@link #blockB}. */
	@Column(name = "Block_B", nullable = true)
	public Integer getBlockB() {
		return blockB;
	}
	/** See {@link #blockB}. */
	public void setBlockB(Integer blockB) {
		this.blockB = blockB;
	}

	/** See {@link #firstNameMidInitial}. */
	@Column(name = "First_Name_Mid_Initial", nullable = true, length = 30)
	public String getFirstNameMidInitial() {
		return firstNameMidInitial;
	}
	/** See {@link #firstNameMidInitial}. */
	public void setFirstNameMidInitial(String firstNameMidInitial) {
		this.firstNameMidInitial = firstNameMidInitial;
	}

	/** See {@link #lastName}. */
	@Column(name = "Last_Name", nullable = true, length = 30)
	public String getLastName() {
		return lastName;
	}
	/** See {@link #lastName}. */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #socialSecurity}. */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", nullable = true, length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	/** See {@link #socialSecurity}. */
	public void setSocialSecurity(String socialSecurity) {
		this.socialSecurity = socialSecurity;
	}

	/** See {@link #viewSSN}. */
	@JsonIgnore
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

	@Transient
	public String getSSNFormatted() {
		String str = null;
		if (getSocialSecurity() != null && getSocialSecurity().length() == 9) {
			str = socialSecurity.substring(0,3) + "-" + socialSecurity.substring(3, 5) + "-" + socialSecurity.substring(5);
		}
		return str;
	}

	/** See {@link #maritalStatus}. */
	@Column(name = "Marital_Status", nullable = true, length = 100)
	public String getMaritalStatus() {
		return maritalStatus;
	}
	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
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

	/** See {@link #withheldAmountChange}. */
	@Column(name = "Withheld_Amount_Change", nullable = true)
	public Integer getWithheldAmountChange() {
		return withheldAmountChange;
	}
	/** See {@link #withheldAmountChange}. */
	public void setWithheldAmountChange(Integer withheldAmountChange) {
		this.withheldAmountChange = withheldAmountChange;
	}

	/** See {@link #employerName}. */
	@Column(name = "Employer_Name", nullable = true, length = 100)
	public String getEmployerName() {
		return employerName;
	}
	/** See {@link #employerName}. */
	public void setEmployerName(String employerName) {
		this.employerName = employerName;
	}

	/** See {@link #employerStateAcctNum}. */
	@Column(name = "Employer_State_Acct_Num", nullable = true, length = 100)
	public String getEmployerStateAcctNum() {
		return employerStateAcctNum;
	}
	/** See {@link #employerStateAcctNum}. */
	public void setEmployerStateAcctNum(String employerStateAcctNum) {
		this.employerStateAcctNum = employerStateAcctNum;
	}
}
