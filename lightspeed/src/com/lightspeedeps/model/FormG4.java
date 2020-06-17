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
 * This entity contains all the fields of the G4 form. It is used to maintain
 * the user wise record of the G4 entries.
 *
 */
@Entity
@Table(name = "form_g4")
public class FormG4 extends Form<FormG4> {

	private static final long serialVersionUID = 9102541524049419518L;

	public static final Byte G4_VERSION_2014 = 14; // last 2 digit of the years
	public static final Byte G4_VERSION_2019 = 19; // last 2 digit of the years
	public static final Byte DEFAULT_G4_VERSION = G4_VERSION_2019;

	private String fullName;

	private String socialSecurity;

	private Address address;

	private Byte maritalA;

	private Byte maritalB;

	private Byte maritalC;

	private Byte maritalD;

	private Byte maritalE;

	private Byte dependents;

	private Integer additionalAllow;

	private Integer addtlAmount;

	private boolean over65;

	private boolean blind;

	private boolean spouseOver65;

	private boolean spouseBlind;

	private Byte boxesChecked;

	private Integer fedEstimated;

	private Integer gaStdDeduction;

	private Integer allowableDeductions;

	private Integer estNonWage;

	private String maritalLetter;

	private boolean noGAIncome;

	private String myState;

	private String spouseState;

	private boolean sameExemptStates;

	private String employerName;

	private String employerAddress;

	private String employerFEIN;

	private String employerWHNumber;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	public FormG4() {
		super();
		setVersion(DEFAULT_G4_VERSION);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		fieldValues.put(FormFieldType.FULL_NAME.name(), getFullName() );
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), (getAddress().getAddrLine1() != null ? getAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), getAddress().getCityStateZip());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			// W4 pdf doesn't have separate city, state, zip fields, so we don't need to "clear" them.
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), "");
		}

		fieldValues.put(FormFieldType.G4_MARITAL_SINGLE.name(), byteFormat(getMaritalA())); // Line A
		fieldValues.put(FormFieldType.G4_MARITAL_MARRIED_BOTH_WORKING.name(), byteFormat(getMaritalB())); // Line B
		fieldValues.put(FormFieldType.G4_MARITAL_MARRIED_ONE_WORKING.name(), byteFormat(getMaritalC())); // Line C
		fieldValues.put(FormFieldType.G4_MARITAL_MARRIED_FILING_SEPARATE.name(), byteFormat(getMaritalD())); // Line D
		fieldValues.put(FormFieldType.G4_MARITAL_HEAD_OF_HOUSEHOLD.name(), byteFormat(getMaritalE())); // Line E
		fieldValues.put(FormFieldType.G4_MARITAL_DEPENDANT_ALLOWANCES.name(), byteFormat(getDependents())); // Line 4
		fieldValues.put(FormFieldType.G4_MARITAL_ADDITIONAL_ALLOWANCES.name(), intFormat(getAdditionalAllow())); // Line 5
		fieldValues.put(FormFieldType.G4_MARITAL_ADDITIONAL_WITHHOLDINGS.name(), intFormat(getAddtlAmount())); // Line 6

		fieldValues.put(FormFieldType.G4_WORKSHEET_YOURSELF_OVER65.name(), getOver65() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_YOURSELF_BLIND.name(), getBlind() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_SPOUSE_OVER65.name(), getSpouseOver65() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_WORKSHEET_SPOUSE_BLIND.name(), getSpouseBlind() ? "Yes" : "Off");

		fieldValues.put(FormFieldType.G4_WORKSHEET_NUMBER_OF_BOXES.name(), byteFormat(getBoxesChecked()));

		fieldValues.put(FormFieldType.G4_WORKSHEET_STANDARD_DEDUCTION.name(), intFormat(getStdDeduction()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_FEDERAL_DEDUCTIONS.name(), intFormat(getFedEstimated()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_GEORGIA_DEDUCTIONS.name(), intFormat(getGaStdDeduction()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_B_A.name(), intFormat(getSubtractDeductions()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_ALLOWABLE_DEDUCTIONS.name(), intFormat(getAllowableDeductions()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_1_2C_2D.name(), intFormat(getTotalDeducts()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_TAXABLE_INCOME.name(), intFormat(getEstNonWage()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_F_E.name(), intFormat(getEstDeductions()));
		fieldValues.put(FormFieldType.G4_WORKSHEET_G_3000.name(), intFormat(getDividedDeductions()));

		fieldValues.put(FormFieldType.G4_TOTAL_ALLOWANCES.name(), intFormat(getTotalAllowances()));
		fieldValues.put(FormFieldType.G4_MARITAL_STATUS_LETTER.name(), getMaritalLetter());
		fieldValues.put(FormFieldType.G4_EXEMPT_CHECK.name(), getNoGAIncome() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_EXEMPT_EMPSTATE.name(), getMyState());
		fieldValues.put(FormFieldType.G4_EXEMPT_SPOUSESTATE.name(), getSpouseState());
		fieldValues.put(FormFieldType.G4_EXEMPT_SAME_STATES_CHECK.name(), getSameExemptStates() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.G4_EMP_ID_NUMBER.name(), getEmployerFEIN());
		fieldValues.put(FormFieldType.G4_EMPLOYERS_WH.name(), getEmployerWHNumber());
		fieldValues.put(FormFieldType.W4_EMPLOYER_NAME.name(), getEmployerName());
		fieldValues.put(FormFieldType.W4_EMPLOYER_ADDRESS.name(), getEmployerAddress());

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
		fullName = trim(fullName);
		socialSecurity = trim(socialSecurity);
		address.trimIsEmpty();
		maritalLetter = trim(maritalLetter);
		myState = trim(myState);
		spouseState = trim(spouseState);
		employerName = trim(employerName);
		employerAddress = trim(employerAddress);
		employerFEIN = trim(employerFEIN);
		employerWHNumber = trim(employerWHNumber);
	}

	/** See {@link #fullName}. */
	@Column(name = "Full_Name", nullable = true, length = 60)
	public String getFullName() {
		return fullName;
	}
	/** See {@link #fullName}. */
	public void setFullName(String fullName) {
		this.fullName = fullName;
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

	/** See {@link #maritalA}. */
	@Column(name = "Marital_A", nullable = true)
	public Byte getMaritalA() {
		return maritalA;
	}
	/** See {@link #maritalA}. */
	public void setMaritalA(Byte maritalA) {
		this.maritalA = maritalA;
	}

	/** See {@link #maritalB}. */
	@Column(name = "Marital_B", nullable = true)
	public Byte getMaritalB() {
		return maritalB;
	}
	/** See {@link #maritalB}. */
	public void setMaritalB(Byte maritalB) {
		this.maritalB = maritalB;
	}

	/** See {@link #maritalC}. */
	@Column(name = "Marital_C", nullable = true)
	public Byte getMaritalC() {
		return maritalC;
	}
	/** See {@link #maritalC}. */
	public void setMaritalC(Byte maritalC) {
		this.maritalC = maritalC;
	}

	/** See {@link #maritalD}. */
	@Column(name = "Marital_D", nullable = true)
	public Byte getMaritalD() {
		return maritalD;
	}
	/** See {@link #maritalD}. */
	public void setMaritalD(Byte maritalD) {
		this.maritalD = maritalD;
	}

	/** See {@link #maritalE}. */
	@Column(name = "Marital_E", nullable = true)
	public Byte getMaritalE() {
		return maritalE;
	}
	/** See {@link #maritalE}. */
	public void setMaritalE(Byte maritalE) {
		this.maritalE = maritalE;
	}

	/** See {@link #dependents}. */
	@Column(name = "Dependents", nullable = true)
	public Byte getDependents() {
		return dependents;
	}
	/** See {@link #dependents}. */
	public void setDependents(Byte dependents) {
		this.dependents = dependents;
	}

	/** See {@link #additionalAllow}. */
	@Column(name = "Additional_Allow", nullable = true)
	public Integer getAdditionalAllow() {
		if (getDividedDeductions() != null && getDividedDeductions() != 0) {
			additionalAllow = getDividedDeductions();
		}
		return additionalAllow;
	}
	/** See {@link #additionalAllow}. */
	public void setAdditionalAllow(Integer additionalAllow) {
		this.additionalAllow = additionalAllow;
	}

	/** See {@link #addtlAmount}. */
	@Column(name = "Addtl_Amount", nullable = true)
	public Integer getAddtlAmount() {
		return addtlAmount;
	}
	/** See {@link #addtlAmount}. */
	public void setAddtlAmount(Integer addtlAmount) {
		this.addtlAmount = addtlAmount;
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

	/** See {@link #boxesChecked}. */
	@Column(name = "Boxes_Checked", nullable = true)
	public Byte getBoxesChecked() {
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
		boxesChecked = (byte) count;
		return boxesChecked;
	}
	/** See {@link #boxesChecked}. */
	public void setBoxesChecked(Byte boxesChecked) {
		this.boxesChecked = boxesChecked;
	}

	/** See {@link #stdDeduction}. */
	@Transient
	public Integer getStdDeduction() {
		Integer stdDeduction = 0;
		if (getBoxesChecked() != null) {
			stdDeduction = getBoxesChecked().intValue() * 1300;
		}
		return stdDeduction;
	}

	/** See {@link #fedEstimated}. */
	@Column(name = "Fed_Estimated", nullable = true)
	public Integer getFedEstimated() {
		return fedEstimated;
	}

	/** See {@link #fedEstimated}. */
	public void setFedEstimated(Integer fedEstimated) {
		this.fedEstimated = fedEstimated;
	}

	/** See {@link #gaStdDeduction}. */
	@Column(name = "GA_Std_Deduction", nullable = true)
	public Integer getGaStdDeduction() {
		return gaStdDeduction;
	}

	/** See {@link #gaStdDeduction}. */
	public void setGaStdDeduction(Integer gaStdDeduction) {
		this.gaStdDeduction = gaStdDeduction;
	}

	@Transient
	public Integer getSubtractDeductions() {
		Integer subtractDeductions = 0;
		if (getFedEstimated() != null) {
			subtractDeductions = getFedEstimated();
		}
		if (getGaStdDeduction() != null) {
			subtractDeductions = subtractDeductions - getGaStdDeduction();
		}
		if (subtractDeductions < 0) {
			subtractDeductions = 0;
		}
		return subtractDeductions;
	}

	/** See {@link #allowableDeductions}. */
	@Column(name = "Allowable_Deductions", nullable = true)
	public Integer getAllowableDeductions() {
		return allowableDeductions;
	}

	/** See {@link #allowableDeductions}. */
	public void setAllowableDeductions(Integer allowableDeductions) {
		this.allowableDeductions = allowableDeductions;
	}

	@Transient
	public Integer getTotalDeducts() {
		Integer totalDeducts = 0;
		if (getStdDeduction() != null) {
			totalDeducts = NumberUtils.safeAdd(totalDeducts, getStdDeduction());
		}
		if (getSubtractDeductions() != null) {
			totalDeducts = NumberUtils.safeAdd(totalDeducts, getSubtractDeductions());
		}
		if (getAllowableDeductions() != null) {
			totalDeducts = NumberUtils.safeAdd(totalDeducts, getAllowableDeductions());
		}
		return totalDeducts;
	}

	/** See {@link #estNonWage}. */
	@Column(name = "Est_Non_Wage", nullable = true)
	public Integer getEstNonWage() {
		return estNonWage;
	}

	/** See {@link #estNonWage}. */
	public void setEstNonWage(Integer estNonWage) {
		this.estNonWage = estNonWage;
	}

	@Transient
	public Integer getEstDeductions() {
		Integer estDeductions = 0;
		if (getTotalDeducts() != null) {
			estDeductions = getTotalDeducts();
		}
		if (getEstNonWage() != null) {
			estDeductions = estDeductions - getEstNonWage();
		}
		if (estDeductions < 0) {
			estDeductions = 0;
		}
		return estDeductions;
	}

	@Transient
	public Integer getDividedDeductions() {
		Integer dividedDeductions = 0;
		if (getEstDeductions() != null && getEstDeductions() > 0) {
			dividedDeductions = ((getEstDeductions()+1499) / 3000);
		}
		additionalAllow = dividedDeductions;
		return dividedDeductions;
	}

	/** See {@link #maritalLetter}. */
	@Column(name = "Marital_Letter", nullable = true)
	public String getMaritalLetter() {
		if (getMaritalA() != null && getMaritalA().equals((byte)1)) {
			maritalLetter = "A";
		}
		else if (getMaritalB() != null && getMaritalB().equals((byte)1)) {
			maritalLetter = "B";
		}
		else if (getMaritalC() != null && (getMaritalC().equals((byte)1) || getMaritalC().equals((byte)2))) {
			maritalLetter = "C";
		}
		else if (getMaritalD() != null && getMaritalD().equals((byte)1)) {
			maritalLetter = "D";
		}
		else if (getMaritalE() != null && getMaritalE().equals((byte)1)) {
			maritalLetter = "E";
		}
		return maritalLetter;
	}
	/** See {@link #maritalLetter}. */
	public void setMaritalLetter(String maritalLetter) {
		this.maritalLetter = maritalLetter;
	}

	/**
	 * @return The total of line items 3 through 5 -- all allowances. Note that
	 *         "line 3" is really 5 separate items for marital status A through
	 *         E.  All the allowances are small integers.
	 */
	@Transient
	public Integer getTotalAllowances() {
		Byte totalAllowances = NumberUtils.safeAdd(
				maritalA,
				maritalB,
				maritalC,
				maritalD,
				maritalE,
				dependents,
				additionalAllow.byteValue()
				);
		if (totalAllowances != null) {
			return totalAllowances.intValue();
		}
		return null;
	}

	/** See {@link #noGAIncome}. */
	@Column(name = "No_GA_Income", nullable = false)
	public boolean getNoGAIncome() {
		return noGAIncome;
	}
	/** See {@link #noGAIncome}. */
	public void setNoGAIncome(boolean noGAIncome) {
		this.noGAIncome = noGAIncome;
	}

	/** See {@link #myState}. */
	@Column(name = "My_State", nullable = true, length = 10)
	public String getMyState() {
		return myState;
	}
	/** See {@link #myState}. */
	public void setMyState(String myState) {
		this.myState = myState;
	}

	/** See {@link #spouseState}. */
	@Column(name = "Spouse_State", nullable = true, length = 10)
	public String getSpouseState() {
		return spouseState;
	}
	/** See {@link #spouseState}. */
	public void setSpouseState(String spouseState) {
		this.spouseState = spouseState;
	}

	/** See {@link #sameExemptStates}. */
	@Column(name = "Same_Exempt_States", nullable = false)
	public boolean getSameExemptStates() {
		return sameExemptStates;
	}
	/** See {@link #sameExemptStates}. */
	public void setSameExemptStates(boolean sameExemptStates) {
		this.sameExemptStates = sameExemptStates;
	}

	/** See {@link #employerName}. */
	@Column(name = "Employer_Name", nullable = true, length = 60)
	public String getEmployerName() {
		return employerName;
	}
	/** See {@link #employerName}. */
	public void setEmployerName(String employerName) {
		this.employerName = employerName;
	}

	/** See {@link #employerAddress}. */
	@Column(name = "Employer_Address", nullable = true, length = 100)
	public String getEmployerAddress() {
		return employerAddress;
	}
	/** See {@link #employerAddress}. */
	public void setEmployerAddress(String employerAddress) {
		this.employerAddress = employerAddress;
	}

	/** See {@link #employerFEIN}. */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Employer_FEIN", nullable = true, length = 1000)
	public String getEmployerFEIN() {
		return employerFEIN;
	}
	/** See {@link #employerFEIN}. */
	public void setEmployerFEIN(String employerFEIN) {
		this.employerFEIN = employerFEIN;
	}

	/** See {@link #employerWHNumber}. */
	@Column(name = "Employer_WH_Number", nullable = true, length = 20)
	public String getEmployerWHNumber() {
		return employerWHNumber;
	}
	/** See {@link #employerWHNumber}. */
	public void setEmployerWHNumber(String employerWHNumber) {
		this.employerWHNumber = employerWHNumber;
	}

}
