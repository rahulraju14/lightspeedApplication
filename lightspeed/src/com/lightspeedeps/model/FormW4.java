package com.lightspeedeps.model;

import java.text.*;
import java.util.*;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.common.*;

/** This entity contains all the fields of the W4 form.
 * It is used to maintain the user wise record of the W4 entries.
 *
 */
@Entity
@Table(name = "form_w4")
public class FormW4 extends Form<FormW4> {

	private static final long serialVersionUID = 8467423764184245296L;

	public static final Byte W4_VERSION_2016 = 16; // version = last 2 digits of year
	public static final Byte W4_VERSION_2017 = 17;
	public static final Byte W4_VERSION_2018 = 18;
	public static final Byte W4_VERSION_2019 = 19; // LS-1667
	public static final Byte W4_VERSION_2020 = 20; // LS-2933
	public static final Byte DEFAULT_W4_VERSION = W4_VERSION_2019;  // LS-1667

	/** 2017: line A; 2018: line A; 1 or 0 */
	private Byte personalSelf;

	/** 2017: line B; 2018: line B; 1 or 0 */
	private Byte personalMarried;

	/** 2017: line C; 2018: line D; 1 or 0 */
	private Byte personalSpouse;

	/** 2017: line D; 2018: line F "Other Dependents" */
	private Byte personalDependents;

	/** 2017: line E; 2018: line C; 1 or 0 */
	private Byte personalHoH;

	/** 2017: line F (1 or 0); 2018: line G "Other credits" (0 or more) */
	private Byte personalCare;

	/** 2017: line G; 2018: line E */
	private Byte personalChild;

	private String firstName;

	private String middleInitial;

	private String lastName;

	/** encrypted SSN */
	private String socialSecurity;

	private Address address;

	private String marital;

	private boolean nameDiffers;

	private Integer allowances;

	private Integer addtlAmount;

	private boolean exempt;

	private String employerName;

	private String employerAddress;

	private String officeCode;

	private String empIdNumber;

	private Integer deductions;

	private Integer automatic;

	private Integer adjustments;

	private Integer estNonwage;

	private Integer dividedBy;

	private Byte estExemptions;

	private Byte table1Lookup;

	private Integer table2Lookup;

	private Integer addtlPerPayPeriod;

	private Integer sumDeductions;

	/** Determines whether the employee is working multiple jobs LS-2946 */
	private Boolean multipleJobs;

	/** Dollar amount for child dependencies LS-2946 */
	private Integer childDependencyAmt;

	/** Dollar amount for other dependencies LS-2946 */
	private Integer otherDependencyAmt;

	/** Total dependency amount */
	private Integer totalDependencyAmt;

	/** Additional income dollar amount */
	private Integer otherIncomeAmt;

	/** Total deductions dollar amount */
	private Integer deductionsAmt;

	/** Total dollar amount for extra tax amount to be withheld */
	private Integer extraWithholdingAmt;

	/** Number of pay periods for the year */
	private Integer numPayPeriods;

	/** Intersection of the taxable wage table for two jobs. The intersection point
	 * is where the the two jobs meet in the table. For worksheet.
	 */
	private Integer twoJobsTaxableWages;

	/** Intersection from the annual taxable wages chart for three jobs.
	 * The intersection point is where the two highest jobs meet in the table.
	 * For worksheet.
	 */
	private Integer threeJobsHighTaxableWages;

	/** The intersection point for three jobs where the sum of the highest
	 * two jobs and the lowest job meet in the table. For worksheet.
	 */
	private Integer threeJobsHighLowTaxableWages;

	/** Total amount of the two intersection points for three jobs. For worksheet. */
	private Integer threeJobsTotalTaxableWages;

	/** Total amount of the multiple jobs section on the Worksheet */
	private Integer multipleJobsTotal;

	/** Estimation of itemized deductions for the tax year. For worksheet*/
	private Integer estItemizedDeductions;

	/**
	 * Estimation of qualified deductions for the tax year. Examples are student
	 * load interest, IRA contributions, ... For worksheet
	 */
	private Integer estQualifiedDeductions;

	/** Filing status deduction for the worksheet. */
	private Integer filingStatusAmt;

	/** Sub-total of the greater value of the estimated itemized deductions and
	 * filing status amount for the worksheet.
	 */
	private Integer deductionsSubTotal;

	/** Estimation of the sum of other amounts such as student loan interest, IRA
	 * contributions and other qualifying deductable amounts. For worksheet.
	 */
	private Integer otherDeductibleAmts;

	/** The sum of the sub-total of deductions + otherDeductableAmts. For worksheet. */
	private Integer deductionsTotal;

	/** LS-3244 Start date of employment taken from the Payroll Start Form */
	private Date firstDateEmployment;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;


	/** default constructor */
	public FormW4() {
		super();
		setVersion(DEFAULT_W4_VERSION);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		String nameDiffersOn = "On";
		if (getVersion() == W4_VERSION_2016) {
			nameDiffersOn = "Yes";
		}
		else if (getVersion() >= W4_VERSION_2019) {
			// works for 2019, and possibly for future. LS-1667
			nameDiffersOn = "1";
		}

		String maritalSingle = "Off";
		String maritalMarried = "Off";
		String maritalOther = "Off";
		String maritalChoice = "";
		if (getMarital() != null) {
			switch(getMarital()) {
				case "s":
					maritalSingle = "1";
					maritalChoice = "Choice1";
					break;
				case "m":
				case "a":
					maritalMarried = "2";
					maritalChoice = "Choice2";
					break;
				case "w":
				case "h":
					maritalOther = "3";
					maritalChoice = "Choice3";
					break;
			}
		}

		fieldValues.put(FormFieldType.W4_PERSONAL_SELF.name(), byteFormat(getPersonalSelf())); // Line A
		fieldValues.put(FormFieldType.W4_PERSONAL_MARRIED.name(), byteFormat(getPersonalMarried())); // Line B
		fieldValues.put(FormFieldType.W4_PERSONAL_SPOUSE.name(), byteFormat(getPersonalSpouse())); // Line C
		fieldValues.put(FormFieldType.W4_PERSONAL_DEPENDENTS.name(), byteFormat(getPersonalDependents())); // Line D
		fieldValues.put(FormFieldType.W4_PERSONAL_HOH.name(), byteFormat(getPersonalHoH())); // Line E
		fieldValues.put(FormFieldType.W4_PERSONAL_CARE.name(), byteFormat(getPersonalCare())); // Line F
		fieldValues.put(FormFieldType.W4_PERSONAL_CHILD.name(), byteFormat(getPersonalChild())); // Line G
		fieldValues.put(FormFieldType.W4_PERSONAL_SUM.name(), intFormat(getPersonalSum())); // Line H

		fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), getFirstName());
		fieldValues.put(FormFieldType.USER_LAST_NAME.name(), getLastName());
		fieldValues.put(FormFieldType.USER_MIDDLE_NAME.name(), getMiddleInitial());
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());

		fieldValues.put(FormFieldType.W4_MARITAL_CHOICE.name(), maritalChoice); // works in Team-designed form
		fieldValues.put(FormFieldType.W4_MARITAL_MARRIED.name(), maritalMarried); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_SINGLE.name(), maritalSingle); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_OTHER.name(), maritalOther); // only used in original Fed form & 2019

		fieldValues.put(FormFieldType.W4_NAME_DIFFERS.name(), getNameDiffers() ? nameDiffersOn : "Off");

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(),
					(getAddress().getAddrLine1() != null ? getAddress().getAddrLine1() : "") + " " +
							(getAddress().getAddrLine2() != null ? getAddress().getAddrLine2()
									: ""));
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), getAddress().getCityStateZip());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			// W4 pdf doesn't have separate city, state, zip fields, so we don't need to "clear" them.
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), "");
		}

		fieldValues.put(FormFieldType.W4_ALLOWANCES.name(), intFormat(getAllowances())); // page 1, Line 5
		fieldValues.put(FormFieldType.W4_ADDTL_AMOUNT.name(), intFormat(getAddtlAmount())); // page 1, Line 6
		fieldValues.put(FormFieldType.W4_EXEMPT.name(), getExempt() ? "Exempt" : ""); // page 1, Line 7

		/*if (true) { //test for signature
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "Emp-date"); // page 1, Line 6
		}*/
		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		fieldValues.put(FormFieldType.W4_EMPLOYER_NAME.name(), getEmployerName()); // page 1, Line 8(a)
		fieldValues.put(FormFieldType.W4_EMPLOYER_ADDRESS.name(), getEmployerAddress()); // page 1, Line 8(b)
		fieldValues.put(FormFieldType.W4_OFFICE_CODE.name(), getOfficeCode()); // page 1, Line 9
		fieldValues.put(FormFieldType.W4_EMP_ID_NUMBER.name(), getEmpIdNumber()); // page 1, Line 10

		fieldValues.put(FormFieldType.W4_DEDUCTIONS.name(), patternFormat("#,##0.", getDeductions())); // Deductions&Adjustments, line 1
		fieldValues.put(FormFieldType.W4_AUTOMATIC.name(), patternFormat("#,##0.", getAutomatic())); // Deductions&Adjustments, line 2
		fieldValues.put(FormFieldType.W4_NET_DEDUCTIONS.name(), patternFormat("#,##0.", getNetDeductions())); // Deductions&Adjustments, line 3
		fieldValues.put(FormFieldType.W4_ADJUSTMENTS.name(), patternFormat("#,##0.", getAdjustments())); // Deductions&Adjustments, line 4
		fieldValues.put(FormFieldType.W4_SUM_DEDUCTIONS.name(), patternFormat("#,##0.", getSumDeductions())); // Deductions&Adjustments, // line 5
		fieldValues.put(FormFieldType.W4_EST_NON_WAGE.name(), patternFormat("#,##0.", getEstNonwage())); // Deductions&Adjustments, line 6
		fieldValues.put(FormFieldType.W4_SUBTRACT_NON_WAGE.name(), patternFormat("#,##0.;(#,##0.)", getSubtractNonwage())); // Deductions&Adjustments, line 7
		fieldValues.put(FormFieldType.W4_DIVIDED_BY.name(), patternFormat("#0;(#0)", getDividedBy())); // Deductions&Adjustments, line 8
		fieldValues.put(FormFieldType.W4_PERSONAL_SUM.name(), intFormat(getPersonalSum())); // Deductions&Adjustments, line 9
		fieldValues.put(FormFieldType.W4_TOTAL_DEDUCTS.name(), intFormat(getTotalDeducts())); // Deductions&Adjustments, line 10

		fieldValues.put(FormFieldType.W4_EST_EXEMPTIONS_1.name(), byteFormat(getEstExemptions())); // 2-Earner: line 1
		fieldValues.put(FormFieldType.W4_TABLE1_LOOKUP_1.name(), byteFormat(getTable1Lookup())); // 2-Earner: line 2
		fieldValues.put(FormFieldType.W4_COMPARE_1_AND_2.name(), byteFormat(getCompare1and2())); // 2-Earner: line 3

		if (getCompare1and2() == null) { // lines 4-9 of two-earner sheet may be filled in
			fieldValues.put(FormFieldType.W4_TABLE1_LOOKUP_2.name(), byteFormat(getTable1Lookup())); // 2-Earner: line 4(=2)
			fieldValues.put(FormFieldType.W4_EST_EXEMPTIONS_2.name(), byteFormat(getEstExemptions())); // 2-Earner: line 5(=1)
			fieldValues.put(FormFieldType.W4_DIFF_EXEMPTIONS.name(), byteFormat(getDiffExemptions())); // 2-Earner: line 6
			fieldValues.put(FormFieldType.W4_TABLE2_LOOKUP.name(), patternFormat("#,##0.", getTable2Lookup())); //  2-Earner: line 7
			fieldValues.put(FormFieldType.W4_ADDTL_WITHHOLDING.name(), patternFormat("#,##0.", getAddtlWithholding())); //  2-Earner: line 8
			fieldValues.put(FormFieldType.W4_ADDTL_PER_PAY_PERIOD.name(), patternFormat("#,##0.", getAddtlPerPayPeriod())); // line 9
		}
		else {
			fieldValues.put(FormFieldType.W4_TABLE1_LOOKUP_2.name(), null); // 2-Earner: line 4(=2)
			fieldValues.put(FormFieldType.W4_EST_EXEMPTIONS_2.name(), null); // 2-Earner: line 5(=1)
			fieldValues.put(FormFieldType.W4_DIFF_EXEMPTIONS.name(), null); // 2-Earner: line 6
			fieldValues.put(FormFieldType.W4_TABLE2_LOOKUP.name(), null); //  2-Earner: line 7
			fieldValues.put(FormFieldType.W4_ADDTL_WITHHOLDING.name(), null); //  2-Earner: line 8
			fieldValues.put(FormFieldType.W4_ADDTL_PER_PAY_PERIOD.name(), null); // line 9
		}
		// LS-3248 W4 2020 fields
		fieldValues.put(FormFieldType.W4_MULTIPLE_JOBS.name(), (getMultipleJobs() != null && getMultipleJobs()) ? "Yes" : "Off");
		fieldValues.put(FormFieldType.W4_NUM_CHILDREN_AMT.name(), intFormat(getChildDependencyAmt()));
		fieldValues.put(FormFieldType.W4_NUM_DEPENDENTS_AMT.name(), intFormat(getOtherDependencyAmt()));
		fieldValues.put(FormFieldType.W4_TOTAL_DEPENDENTS_AMT.name(), intFormat(getTotalDependencyAmt()));
		fieldValues.put(FormFieldType.W4_OTHER_INCOME_WITHHOLDING_AMT.name(), intFormat(getOtherIncomeAmt()));
		fieldValues.put(FormFieldType.W4_DEDUCTIONS_WITHHOLDINGS_AMT.name(), intFormat(getDeductionsAmt()));
		fieldValues.put(FormFieldType.W4_EXTRA_WITHHOLDINGS_AMT.name(), intFormat(getExtraWithholdingAmt()));

		// LS-3247 W4 2020 Worksheet fields
		fieldValues.put(FormFieldType.W4_NUM_PAY_PERIODS.name(), intFormat(getNumPayPeriods()));
		fieldValues.put(FormFieldType.W4_TWO_JOBS_TOTAL_TAXABLE_WAGES.name(), intFormat(getTwoJobsTaxableWages()));
		fieldValues.put(FormFieldType.W4_THREE_JOBS_HIGH_TAXABLE_WAGES.name(), intFormat(getThreeJobsHighTaxableWages()));
		fieldValues.put(FormFieldType.W4_THREE_JOBS_HIGH_LOW_TAXABLE_WAGES.name(), intFormat(getThreeJobsHighLowTaxableWages()));
		fieldValues.put(FormFieldType.W4_THREE_JOBS_TOTAL_TAXABLE_WAGES.name(), intFormat(getThreeJobsTotalTaxableWages()));
		fieldValues.put(FormFieldType.W4_MULTIPLE_JOBS_TOTAL.name(), intFormat(getMultipleJobsTotal()));
		fieldValues.put(FormFieldType.W4_EST_ITEMIZED_DEDUCTIONS.name(), intFormat(getEstItemizedDeductions()));
		fieldValues.put(FormFieldType.W4_FILING_STATUS_AMT.name(), intFormat(getFilingStatusAmt()));
		fieldValues.put(FormFieldType.W4_SUB_TOTAL_DEDUCTIONS.name(), intFormat(getDeductionsSubTotal()));
		fieldValues.put(FormFieldType.W4_OTHER_DEDUCTIONS_AMT.name(), intFormat(getOtherDeductibleAmts()));
		fieldValues.put(FormFieldType.W4_DEDUCTIONS_TOTAL_AMT.name(), intFormat(getDeductionsTotal()));

		// LS-3244 Employee's Start Date from Payroll Start Form
		fieldValues.put(FormFieldType.W4_FIRST_DATE_EMPLOYMENT.name(), CalendarUtils.formatDate(getFirstDateEmployment(), "MM/dd/yyyy"));
	}

	/**
	 * Export the fields in this FormW4 using the supplied Exporter.
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append(getLastName());
		ex.append(getFirstName());
		ex.append(getMiddleInitial());
		ex.append(getSocialSecurity());
		ex.append(getPersonalSelf());
		ex.append(getPersonalMarried());
		ex.append(getPersonalSpouse());
		ex.append(getPersonalDependents());
		ex.append(getPersonalHoH());
		ex.append(getPersonalCare());
		ex.append(getPersonalChild());
		Address addr = getAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);
		ex.append(getMarital());
		ex.append(getNameDiffers());
		if (getExempt()) { // LS-849: Exempt Employee should transfer as 999 allowances
			ex.append(999);
		}
		else {
			ex.append(getAllowances());
		}
		ex.append(getAddtlAmount());
		ex.append(getExempt());
		ex.append(getEmployerName());
		ex.append(getEmployerAddress());
		ex.append(getOfficeCode());
		ex.append(getEmpIdNumber());
		// Start of Deductions and  Adj. worksheet
		ex.append(getDeductions());		// line 1
		ex.append(getAutomatic());		// line 2
		ex.append(getNetDeductions());	// line 3
		ex.append(getAdjustments());	// line 4
		ex.append(getSumDeductions());	// line 5
		ex.append(getEstNonwage());		// line 6
		ex.append(getDividedBy());		// line 8
		// Start of two-earner worksheet
		ex.append(getEstExemptions());	// Line 1
		ex.append(getTable1Lookup());	// Line 2
		ex.append(getTable2Lookup());	// Line 7
		ex.append(getAddtlPerPayPeriod());	// Line 9

		// LS_3328 New W4-2020 fields to be exported.
		ex.append(getMultipleJobs());
		ex.append(getChildDependencyAmt());
		ex.append(getOtherDependencyAmt());
		ex.append(getTotalDependencyAmt());
		ex.append(getOtherIncomeAmt());
		ex.append(getDeductionsAmt());
		ex.append(getExtraWithholdingAmt());
		//LS-3848 Add two digit year code on export format for AS400 logic
		ex.append(getVersion());
	}

	@Override
	public void trim() {
		firstName = trim(firstName);
		lastName = trim(lastName);
		middleInitial = trim(middleInitial);
		socialSecurity = trim(socialSecurity);
		address.trimIsEmpty();
		employerName = trim(employerName);
		employerAddress = trim(employerAddress);
		officeCode = trim(officeCode);
		empIdNumber = trim(empIdNumber);
		employerName = trim(employerName);
	}

	@Column(name = "Personal_Self", nullable = true)
	public Byte getPersonalSelf() {
		return personalSelf;
	}
	public void setPersonalSelf(Byte personalSelf) {
		this.personalSelf = personalSelf;
	}

	@Column(name = "Personal_Married", nullable = true)
	public Byte getPersonalMarried() {
		return personalMarried;
	}
	public void setPersonalMarried(Byte personalMarried) {
		this.personalMarried = personalMarried;
	}

	@Column(name = "Personal_Spouse", nullable = true)
	public Byte getPersonalSpouse() {
		return personalSpouse;
	}
	public void setPersonalSpouse(Byte personalSpouse) {
		this.personalSpouse = personalSpouse;
	}

	@Column(name = "Personal_Dependents", nullable = true)
	public Byte getPersonalDependents() {
		return personalDependents;
	}
	public void setPersonalDependents(Byte personalDependents) {
		this.personalDependents = personalDependents;
	}

	@Column(name = "Personal_HoH", nullable = true)
	public Byte getPersonalHoH() {
		return personalHoH;
	}
	public void setPersonalHoH(Byte personalHoH) {
		this.personalHoH = personalHoH;
	}

	@Column(name = "Personal_Care", nullable = true)
	public Byte getPersonalCare() {
		return personalCare;
	}
	public void setPersonalCare(Byte personalCare) {
		this.personalCare = personalCare;
	}

	@Column(name = "Personal_Child", nullable = true)
	public Byte getPersonalChild() {
		return personalChild;
	}
	public void setPersonalChild(Byte personalChild) {
		this.personalChild = personalChild;
	}

	/** 2017: line H; 2018: line H */
	@Transient
	public Integer getPersonalSum() {
		Integer personalSum;
		// Add the single-digit fields as Bytes
		Byte temp = NumberUtils.safeAdd(personalSelf, personalMarried, personalSpouse,
				personalHoH, personalCare);
		if (temp == null)
			temp = 0;
		personalSum = temp.intValue() +
				(personalDependents == null ? 0 : personalDependents.intValue()) +
				(personalChild == null ? 0 : personalChild.intValue());
		return personalSum;
	}

	@Column(name = "First_Name", nullable = true, length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Column(name = "Middle_Initial", nullable = true, length = 1)
	public String getMiddleInitial() {
		return middleInitial;
	}
	public void setMiddleInitial(String middleInitial) {
		this.middleInitial = middleInitial;
	}

	@Column(name = "Last_Name", nullable = true, length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", nullable = true, length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	public void setSocialSecurity(String socialSecurity) {
		viewSSN = null;
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

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@Column(name = "Marital", nullable = true, length = 1)
	public String getMarital() {
		return marital;
	}
	public void setMarital(String marital) {
		this.marital = marital;
	}

	@Column(name = "Name_Differs", nullable = false)
	public boolean getNameDiffers() {
		return nameDiffers;
	}
	public void setNameDiffers(boolean nameDiffers) {
		this.nameDiffers = nameDiffers;
	}

	@Column(name = "Allowances", nullable = true)
	public Integer getAllowances() {
		return allowances;
	}
	public void setAllowances(Integer allowances) {
		this.allowances = allowances;
	}

	@Column(name = "Addtl_Amount", nullable = true)
	public Integer getAddtlAmount() {
		return addtlAmount;
	}
	public void setAddtlAmount(Integer addtlAmount) {
		this.addtlAmount = addtlAmount;
	}

	@Column(name = "Exempt", nullable = false)
	public boolean getExempt() {
		return exempt;
	}
	public void setExempt(boolean exempt) {
		this.exempt = exempt;
	}

	@Column(name = "Employer_Name", nullable = true, length = 100)
	public String getEmployerName() {
		return employerName;
	}
	public void setEmployerName(String employerName) {
		this.employerName = employerName;
	}

	@Column(name = "Employer_Address", nullable = true, length = 100)
	public String getEmployerAddress() {
		return employerAddress;
	}
	public void setEmployerAddress(String employerAddress) {
		this.employerAddress = employerAddress;
	}

	@Column(name = "Office_Code", nullable = true, length = 10)
	public String getOfficeCode() {
		return officeCode;
	}
	public void setOfficeCode(String officeCode) {
		this.officeCode = officeCode;
	}

	@Column(name = "Emp_Id_Number", nullable = true, length = 10)
	public String getEmpIdNumber() {
		return empIdNumber;
	}
	public void setEmpIdNumber(String empIdNumber) {
		this.empIdNumber = empIdNumber;
	}

	@Column(name = "Deductions", nullable = true)
	public Integer getDeductions() {
		return deductions;
	}
	public void setDeductions(Integer deductions) {
		this.deductions = deductions;
	}

	@Column(name = "Automatic", nullable = true)
	public Integer getAutomatic() {
		return automatic;
	}
	public void setAutomatic(Integer automatic) {
		this.automatic = automatic;
	}

	@Transient
	public Integer getNetDeductions() {
		Integer netDeductions = 0;
		if (deductions != null) {
			netDeductions = deductions;
		}
		if (automatic != null) {
			netDeductions = netDeductions - automatic;
		}
		if (netDeductions < 0) {
			return 0;
		}
		return netDeductions;
	}

	@Column(name = "Adjustments", nullable = true)
	public Integer getAdjustments() {
		return adjustments;
	}
	public void setAdjustments(Integer adjustments) {
		this.adjustments = adjustments;
	}

	/*@Transient
	public Integer getSumDeductions() {
		Integer sumDeductions = null;
		sumDeductions = NumberUtils.safeAdd(getNetDeductions(), adjustments);
		return sumDeductions;
	}*/

	@Column(name = "Est_Non_Wage", nullable = true)
	public Integer getEstNonwage() {
		return estNonwage;
	}
	public void setEstNonwage(Integer estNonwage) {
		this.estNonwage = estNonwage;
	}

	/**
	 * @return The value for Line 7 of the Deductions worksheet.
	 */
	@Transient
	public Integer getSubtractNonwage() {
		Integer subtractNonwage = 0;
		if (getSumDeductions() != null) {
			subtractNonwage = getSumDeductions();
		}
		if (estNonwage != null) {
			subtractNonwage = subtractNonwage - estNonwage;
		}
		if (subtractNonwage < 0 && getVersion().byteValue() < W4_VERSION_2018) {
			// prior to 2018, enter 0 if negative result
			subtractNonwage = 0;
		}
		setDividedBy(subtractNonwage / getDivisor());
		return subtractNonwage;
	}

	/**
	 * @return The value required for computing Line 8 of the Deductions worksheet.
	 */
	@Transient
	public int getDivisor() {
		if (getVersion().byteValue() >= W4_VERSION_2019) { // LS-1667
			return 4200;
		}
		else if (getVersion().byteValue() == W4_VERSION_2018) {
			return 4150;
		}
		return 4050;
	}

	@Column(name = "Divided_By", nullable = true)
	public Integer getDividedBy() {
		return dividedBy;
	}
	public void setDividedBy(Integer dividedBy) {
		this.dividedBy = dividedBy;
	}

	/**
	 * @return The value for Line 10 of the Deductions worksheet.
	 */
	@Transient
	public Integer getTotalDeducts() {
		Integer totalDeducts = null;
		if (getPersonalSum() != null) {
			totalDeducts = NumberUtils.safeAdd(dividedBy, getPersonalSum().intValue());
			if (totalDeducts < 0) {
				totalDeducts = 0;
			}
		}
		return totalDeducts;
	}

	@Column(name = "Est_Exemptions", nullable = true)
	public Byte getEstExemptions() {
		return estExemptions;
	}
	public void setEstExemptions(Byte estExemptions) {
		this.estExemptions = estExemptions;
	}

	@Column(name = "Table1_Lookup", nullable = true)
	public Byte getTable1Lookup() {
		return table1Lookup;
	}
	public void setTable1Lookup(Byte table1Lookup) {
		this.table1Lookup = table1Lookup;
	}

	/**
	 * @return The difference between the Two Earner worksheet fields 1 and 2
	 *         (line 1 minus line 2) IF line 1 is greater than or equal to
	 *         line2; ELSE return null. When this returns null, lines 4 through
	 *         9 of the Two Earner worksheet may be used.
	 */
	@Transient
	public Byte getCompare1and2() {
		Byte compare1and2 = 0;
		if (estExemptions != null) {
			compare1and2 = estExemptions;
		}
		if (table1Lookup != null) {
			compare1and2 = (byte) (compare1and2 - table1Lookup);
		}
		if (compare1and2 < 0) {
			return null;
		}
		return compare1and2;
	}

	@Transient
	public Byte getDiffExemptions() {
		Byte diffExemption = 0;
		if (table1Lookup != null) {
			diffExemption = table1Lookup;
		}
		if (estExemptions != null) {
			diffExemption = (byte) (diffExemption - estExemptions);
		}
		if (diffExemption < 0) {
			return null;
		}
		return diffExemption;
	}

	@Column(name = "Table2_Lookup", nullable = true)
	public Integer getTable2Lookup() {
		return table2Lookup;
	}
	public void setTable2Lookup(Integer table2Lookup) {
		this.table2Lookup = table2Lookup;
	}

	@Transient
	public Integer getAddtlWithholding() {
		Integer addtlWithholding = null;
		if (getDiffExemptions() != null && table2Lookup != null) {
			addtlWithholding = getDiffExemptions() * table2Lookup;
		}
		return addtlWithholding;
	}

	@Column(name = "Addtl_Per_Pay_Period", nullable = true)
	public Integer getAddtlPerPayPeriod() {
		return addtlPerPayPeriod;
	}
	public void setAddtlPerPayPeriod(Integer addtlPerPayPeriod) {
		this.addtlPerPayPeriod = addtlPerPayPeriod;
	}

	@Column(name = "Sum_Deductions", nullable = true)
	public Integer getSumDeductions() {
		return sumDeductions;
	}
	public void setSumDeductions(Integer sumDeductions) {
		this.sumDeductions = sumDeductions;
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

	/** See {@link #totalDependencyAmt}. */
	@Column(name = "Total_Dependency_Amt")
	public Integer getTotalDependencyAmt() {
		return totalDependencyAmt;
	}

	/** See {@link #totalDependencyAmt}. */
	public void setTotalDependencyAmt(Integer totalDependencyAmt) {
		this.totalDependencyAmt = totalDependencyAmt;
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
	@Column(name="Extra_Withholding_Amt")
	public Integer getExtraWithholdingAmt() {
		return extraWithholdingAmt;
	}

	/** See {@link #extraWithholdingAmt}. */
	public void setExtraWithholdingAmt(Integer extraWithholdingAmt) {
		this.extraWithholdingAmt = extraWithholdingAmt;
	}

	/** See {@link #numPayPeriods}. */
	@Column(name = "Num_Pay_Periods")
	public Integer getNumPayPeriods() {
		return numPayPeriods;
	}

	/** See {@link #numPayPeriods}. */
	public void setNumPayPeriods(Integer numPayPeriods) {
		this.numPayPeriods = numPayPeriods;
	}

	/** See {@link #twoJobsTaxableWages}. */
	@Column(name = "Two_jobs_Taxable_Wages")
	public Integer getTwoJobsTaxableWages() {
		return twoJobsTaxableWages;
	}

	/** See {@link #twoJobsTaxableWage}. */
	public void setTwoJobsTaxableWages(Integer twoJobsTaxableWages) {
		this.twoJobsTaxableWages = twoJobsTaxableWages;
	}

	/** See {@link #threeJobsHighLowTaxableWages}. */
	@Column(name = "Three_Jobs_High_Low_Taxable_Wages")
	public Integer getThreeJobsHighLowTaxableWages() {
		return threeJobsHighLowTaxableWages;
	}

	/** See {@link #threeJobsHighLowTaxableWages}. */
	public void setThreeJobsHighLowTaxableWages(Integer threeJobsHighLowTaxableWages) {
		this.threeJobsHighLowTaxableWages = threeJobsHighLowTaxableWages;
	}

	/** See {@link #threeJobsHighTaxableWages}. */
	@Column(name = "Three_Jobs_High_Taxable_Wages")
	public Integer getThreeJobsHighTaxableWages() {
		return threeJobsHighTaxableWages;
	}

	/** See {@link #threeJobsHighTaxableWages}. */
	public void setThreeJobsHighTaxableWages(Integer threeJobsHighTaxableWages) {
		this.threeJobsHighTaxableWages = threeJobsHighTaxableWages;
	}

	/** See {@link #threeJobsTotalTaxableWages}. */
	@Column(name = "Three_Jobs_Total_Taxable_Wages")
	public Integer getThreeJobsTotalTaxableWages() {
		return threeJobsTotalTaxableWages;
	}

	/** See {@link #threeJobsTotalTaxableWages}. */
	public void setThreeJobsTotalTaxableWages(Integer threeJobsTotalTaxableWages) {
		this.threeJobsTotalTaxableWages = threeJobsTotalTaxableWages;
	}

	/** See {@link #estItemizedDeductions}. */
	@Column(name = "Est_Itemized_Deductions")
	public Integer getEstItemizedDeductions() {
		return estItemizedDeductions;
	}

	/** See {@link #estItemizedDeductions}. */
	public void setEstItemizedDeductions(Integer estItemizedDeductions) {
		this.estItemizedDeductions = estItemizedDeductions;
	}

	/** See {@link #filingStatusAmt}. */
	@Column(name = "Filing_Status_Amt")
	public Integer getFilingStatusAmt() {
		return filingStatusAmt;
	}

	/** See {@link #filingStatusAmt}. */
	public void setFilingStatusAmt(Integer filingStatusAmt) {
		this.filingStatusAmt = filingStatusAmt;
	}

	/** See {@link #deductionsSubTotal}. */
	@Column(name = "Deductions_Sub_Total")
	public Integer getDeductionsSubTotal() {
		return deductionsSubTotal;
	}

	/** See {@link #deductionsSubTotal}. */
	public void setDeductionsSubTotal(Integer deductionsSubTotal) {
		this.deductionsSubTotal = deductionsSubTotal;
	}

	/** See {@link #otherDeductibleAmts}. */
	@Column(name = "Other_Deductible_Amts")
	public Integer getOtherDeductibleAmts() {
		return otherDeductibleAmts;
	}

	/** See {@link #otherDeductibleAmts}. */
	public void setOtherDeductibleAmts(Integer otherDeductibleAmts) {
		this.otherDeductibleAmts = otherDeductibleAmts;
	}

	/** See {@link #deductionsTotal}. */
	@Column(name = "Deductions_Total")
	public Integer getDeductionsTotal() {
		return deductionsTotal;
	}

	/** See {@link #deductionsTotal}. */
	public void setDeductionsTotal(Integer deductionsTotal) {
		this.deductionsTotal = deductionsTotal;
	}

	/** See {@link #estQualifiedDeductions}. */
	@Column(name = "Est_Qualified_Deductions")
	public Integer getEstQualifiedDeductions() {
		return estQualifiedDeductions;
	}

	/** See {@link #estQualifiedDeductions}. */
	public void setEstQualifiedDeductions(Integer estQualifiedDeductions) {
		this.estQualifiedDeductions = estQualifiedDeductions;
	}

	/** See {@link #multipleJobsTotal}. */
	@Column(name = "Multiple_Jobs_Total")
	public Integer getMultipleJobsTotal() {
		return multipleJobsTotal;
	}

	/** See {@link #multipleJobsTotal}. */
	public void setMultipleJobsTotal(Integer multipleJobsTotal) {
		this.multipleJobsTotal = multipleJobsTotal;
	}

	/** See {@link #firstDateEmployment}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "First_Date_Employment")
	public Date getFirstDateEmployment() {
		return firstDateEmployment;
	}

	/** See {@link #firstDateEmployment}. */
	public void setFirstDateEmployment(Date firstDateEmployment) {
		this.firstDateEmployment = firstDateEmployment;
	}

}
