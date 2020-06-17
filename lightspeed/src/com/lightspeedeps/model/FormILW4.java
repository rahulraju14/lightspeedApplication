package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * This entity contains all the fields of the IL-W4 form. It is used to maintain
 * the user wise record of the IL-W4 entries.
 *
 */
@Entity
@Table(name = "form_ilw4")
public class FormILW4 extends Form<FormILW4>  {

	private static final long serialVersionUID = -4711198021560546089L;

	public static final Byte ILW4_VERSION_2017 = 17; // last 2 digit of the years
	public static final Byte ILW4_VERSION_2019 = 19; // last 2 digit of the years
	public static final Byte DEFAULT_ILW4_VERSION = ILW4_VERSION_2019;

	private boolean dependent;

	private boolean spouseDependent;

	private Byte boxesChecked;

	private Byte numOfDependents;

	private Integer personalAllowances;

	private boolean over65;

	private boolean blind;

	private boolean spouseOver65;

	private boolean spouseBlind;

	private Byte boxesCheckedStep2;

	private Integer deductions;

	private Integer addtlAllowances;

	private String socialSecurity;

	private String name;

	private Address address;

	private boolean exempt;

	private Integer additionalAmount;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	public FormILW4() {
		super();
		setVersion(DEFAULT_ILW4_VERSION);
	}

	@Override
	public void trim() {
		socialSecurity = trim(socialSecurity);
		name = trim(name);
		address.trimIsEmpty();
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		fieldValues.put(FormFieldType.W4_PERSONAL_SELF.name(), getDependent() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.W4_PERSONAL_SPOUSE.name(), getSpouseDependent() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.ILW4_LINE_1.name(), byteFormat(getBoxesChecked()));
		fieldValues.put(FormFieldType.ILW4_LINE_2.name(), byteFormat(getNumOfDependents()));
		fieldValues.put(FormFieldType.ILW4_LINE_3.name(), byteFormat(getBasicPersonalAllowances()));
		fieldValues.put(FormFieldType.ILW4_LINE_4.name(), intFormat(getPersonalAllowances()));

		fieldValues.put(FormFieldType.G4_WORKSHEET_YOURSELF_OVER65.name(), getOver65() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_YOURSELF_BLIND.name(), getBlind() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_SPOUSE_OVER65.name(), getSpouseOver65() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_SPOUSE_BLIND.name(), getSpouseBlind() ? "Yes" : "Off");

		fieldValues.put(FormFieldType.ILW4_LINE_5.name(), byteFormat(getBoxesCheckedStep2()));
		fieldValues.put(FormFieldType.ILW4_LINE_6.name(), intFormat(getDeductions()));
		fieldValues.put(FormFieldType.ILW4_LINE_7.name(), intFormat(getDividedDeductions()));
		fieldValues.put(FormFieldType.ILW4_LINE_8.name(), intFormat(getTotalAddtlAllowances()));
		fieldValues.put(FormFieldType.ILW4_LINE_9.name(), intFormat(getAddtlAllowances()));

		fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), getName());
		fieldValues.put(FormFieldType.USER_LAST_NAME.name(), "");
		if (getSocialSecurity() != null) {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), getSocialSecurity().substring(0, 3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), getSocialSecurity().substring(3, 5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), getSocialSecurity().substring(5, 9));
		}
		else {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), null);
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), null);
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), null);
		}

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), (getAddress().getAddrLine1() != null ? getAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), (getAddress().getCity() != null ? getAddress().getCity(): ""));
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), (getAddress().getState() != null ? getAddress().getState() : ""));
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), (getAddress().getZip() != null ? getAddress().getZip() : ""));
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), "");
		}

		fieldValues.put(FormFieldType.W4_EXEMPT.name(), getExempt() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_MARITAL_DEPENDANT_ALLOWANCES.name(), intFormat(getPersonalAllowances()));
		fieldValues.put(FormFieldType.G4_MARITAL_ADDITIONAL_ALLOWANCES.name(), intFormat(getAddtlAllowances()));
		fieldValues.put(FormFieldType.W4_ADDTL_AMOUNT.name(), intFormat(getAdditionalAmount()));

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

	/** See {@link #dependent}. */
	@Column(name = "Dependent", nullable = false)
	public boolean getDependent() {
		return dependent;
	}
	/** See {@link #dependent}. */
	public void setDependent(boolean dependent) {
		this.dependent = dependent;
	}

	/** See {@link #spouseDependent}. */
	@Column(name = "Spouse_Dependent", nullable = false)
	public boolean getSpouseDependent() {
		return spouseDependent;
	}
	/** See {@link #spouseDependent}. */
	public void setSpouseDependent(boolean spouseDependent) {
		this.spouseDependent = spouseDependent;
	}

	/** See {@link #boxesChecked}. */
	@Column(name = "Boxes_Checked", nullable = true)
	public Byte getBoxesChecked() {
		int count = 0;
		if (getDependent()) {
			count+=1;
		}
		if (getSpouseDependent()) {
			count+=1;
		}
		boxesChecked = (byte) count;
		return boxesChecked;
	}
	/** See {@link #boxesChecked}. */
	public void setBoxesChecked(Byte boxesChecked) {
		this.boxesChecked = boxesChecked;
	}

	/** See {@link #numOfDependents}. */
	@Column(name = "Num_Of_Dependents", nullable = true)
	public Byte getNumOfDependents() {
		return numOfDependents;
	}
	/** See {@link #numOfDependents}. */
	public void setNumOfDependents(Byte numOfDependents) {
		this.numOfDependents = numOfDependents;
	}

	@Transient
	public Byte getBasicPersonalAllowances () {
		// note that safeAdd takes care of null checks
		Byte basicPersonalAllowances = NumberUtils.safeAdd(getNumOfDependents(), getBoxesChecked());
		return basicPersonalAllowances;
	}

	/** See {@link #personalAllowances}. */
	@Column(name = "Personal_Allowances", nullable = true)
	public Integer getPersonalAllowances() {
		return personalAllowances;
	}
	/** See {@link #personalAllowances}. */
	public void setPersonalAllowances(Integer personalAllowances) {
		this.personalAllowances = personalAllowances;
	}

	/** See {@link #over65}. */
	@Column(name = "Over_65", nullable = false)
	public boolean getOver65() {
		return over65;
	}
	/** See {@link #over65}. */
	public void setOver65(boolean over65) {
		this.over65 = over65;
	}

	/** See {@link #blind}. */
	@Column(name = "Blind", nullable = false)
	public boolean getBlind() {
		return blind;
	}
	/** See {@link #blind}. */
	public void setBlind(boolean blind) {
		this.blind = blind;
	}

	/** See {@link #spouseOver65}. */
	@Column(name = "Spouse_Over_65", nullable = false)
	public boolean getSpouseOver65() {
		return spouseOver65;
	}
	/** See {@link #spouseOver65}. */
	public void setSpouseOver65(boolean spouseOver65) {
		this.spouseOver65 = spouseOver65;
	}

	/** See {@link #spouseBlind}. */
	@Column(name = "Spouse_Blind", nullable = false)
	public boolean getSpouseBlind() {
		return spouseBlind;
	}
	/** See {@link #spouseBlind}. */
	public void setSpouseBlind(boolean spouseBlind) {
		this.spouseBlind = spouseBlind;
	}

	/** See {@link #boxesCheckedStep2}. */
	@Column(name = "Boxes_Checked_Step2", nullable = true)
	public Byte getBoxesCheckedStep2() {
		int count = 0;
		if (getOver65()) {
			count+=1;
		}
		if (getBlind()) {
			count+=1;
		}
		if (getSpouseOver65()) {
			count+=1;
		}
		if (getSpouseBlind()) {
			count+=1;
		}
		boxesCheckedStep2 = (byte) count;
		return boxesCheckedStep2;
	}
	/** See {@link #boxesCheckedStep2}. */
	public void setBoxesCheckedStep2(Byte boxesCheckedStep2) {
		this.boxesCheckedStep2 = boxesCheckedStep2;
	}

	/** See {@link #deductions}. */
	@Column(name = "Deductions", nullable = true)
	public Integer getDeductions() {
		return deductions;
	}
	/** See {@link #deductions}. */
	public void setDeductions(Integer deductions) {
		this.deductions = deductions;
	}

	@Transient
	public Integer getDividedDeductions() {
		Integer dividedDeductions = 0;
		if (getDeductions() != null && getDeductions() > 0) {
			dividedDeductions = ((getDeductions() + 499)/ 1000);
		}
		return dividedDeductions;
	}

	@Transient
	public Integer getTotalAddtlAllowances() {
		Integer totalAddtlAllowances = 0;
		if (getBoxesCheckedStep2() != null) {
			totalAddtlAllowances = getBoxesCheckedStep2().intValue();
		}
		totalAddtlAllowances = NumberUtils.safeAdd(totalAddtlAllowances, getDividedDeductions());
		return totalAddtlAllowances;
	}

	/** See {@link #addtlAllowances}. */
	@Column(name = "Addtl_Allowances", nullable = true)
	public Integer getAddtlAllowances() {
		return addtlAllowances;
	}
	/** See {@link #addtlAllowances}. */
	public void setAddtlAllowances(Integer addtlAllowances) {
		this.addtlAllowances = addtlAllowances;
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

	/** See {@link #name}. */
	@Column(name = "Name", nullable = true, length = 60)
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
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

	/** See {@link #exempt}. */
	@Column(name = "Exempt", nullable = false)
	public boolean getExempt() {
		return exempt;
	}
	/** See {@link #exempt}. */
	public void setExempt(boolean exempt) {
		this.exempt = exempt;
	}

	/** See {@link #additionalAmount}. */
	@Column(name = "Additional_Amount", nullable = true)
	public Integer getAdditionalAmount() {
		return additionalAmount;
	}
	/** See {@link #additionalAmount}. */
	public void setAdditionalAmount(Integer additionalAmount) {
		this.additionalAmount = additionalAmount;
	}

}
