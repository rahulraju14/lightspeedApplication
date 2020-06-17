/**
 * File: FormStateW4.java
 */
package com.lightspeedeps.model;

import java.text.*;
import java.util.*;

import javax.persistence.*;

import org.hibernate.annotations.ColumnTransformer;

import com.fasterxml.jackson.annotation.*;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.common.*;

/**
 * Shared entity class for all state W-4 forms.  The fields include all those fields
 * necessary to support most state W-4s in the TTCO system.  Note that several of
 * the state W4s (GA, AZ, IL, LA) were completed prior to this style/method was
 * developed, so those W4s have individual form classes and tables. LS-3627
 */
//@Entity
//@Table(name = "form_state_w4")
public class FormStateW4 extends Form<FormStateW4> {

	private final static String ENCRYPT_KEY = "concat('tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq', 0x3f, 'p/m5\n\t\t\t')";

	/** Serialization id */
	private static final long serialVersionUID = 8467423764184245296L;

	public static final Byte DEFAULT_VERSION = 20;

	private static final String EXEMPT_TEXT = "Exempt";

	// Domiciled States
	private static final String DOMICILED_STATE_DC = "DC";
	private static final String DOMICILED_STATE_VA = "VA";
	private static final String DOMICILED_STATE_WV = "WV";

	// Marital Statuses
	public static final String MARITAL_STATUS_SINGLE = "s";
	public static final String MARITAL_STATUS_MARRIED = "m";
	public static final String MARITAL_STATUS_MARRIED_SEPARATE = "j";
	public static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";
	public static final String MARITAL_HEAD_HOUSEHOLD = "h";
	public static final String MARITAL_MILITARY_SPOUSE = "y";
	public static final String MARITAL_DISABLED = "d";
	// LS-4229 Marital statuses for PR
	public static final String MARITAL_STATUS_SINGLE_NONE = "n";
	public static final String MARITAL_STATUS_MARRIED_NONE = "p";

	// LS-4234 Veteran Exemptions
	public static final String VET_EXEMPTION = "v";
	public static final String VET_NONE_EXEMPTION = "w";

	/** This determines which state the form is for. */
	private PayrollFormType formType;

	// Several varieties of name fields are defined; one or more will be used
	// depending on the particular state's W4 layout.

	/** Employee's full name (where applicable -- when form has a single
	 * name field) */
	private String fullName;

	/** Employee's first name (where applicable) */
	private String firstName;

	/** Employee's middle initial (where applicable) */
	private String middleInitial;

	/** Employee's first name plus middle initial (where applicable) */
	private String firstAndInitial;

	/** Employee's last name (where applicable) */
	private String lastName;

	/** Employee's encrypted SSN */
	private String socialSecurity;

	/** Home address */
	private Address address;

	/** Marital status - letter code */
	private String marital;

	/** number of allowances */
	private Integer allowances;

	/** additional amount to withhold per paycheck. */
	private Integer additionalAmount;

	/** True if employee is exempt from deductions */
	private boolean exempt;

    /** Generic exempt values. MD has six exempt fields */
    private boolean exemptStatus1;
    private boolean exemptStatus2;
    private boolean exemptStatus3;
    private boolean exemptStatus4;
    private boolean exemptStatus5;
    private boolean exemptStatus6;

    /** Marital Status code of S for single or MS for married filing separately for AL*/
    private String maritalStatusCode1;

    /** Marital Status code of H for single/head of household or M for married for AL*/
    private String maritalStatusCode2;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

    /** Resident state */
    private String residentState;

    /** Domicile State */
    private String domicileState;

    /** Year when applicable */
    private Integer applicableYear;

    /** true, if employee is resident of new york city. */
	private Boolean nycResident;

	/** true, if employee is resident of Yonkers. */
	private Boolean yonkersResident;

	/** Total number of allowances for New York city. */
	private Integer nycAllownces;

	/** Additional withholding per pay, New York State amount. */
	private Integer nysAmount;

	/** Additional withholding per pay, New York city amount. */
	private Integer nycAmount;

	/** Additional withholding per pay, Yonkers amount. */
	private Integer yonkersAmount;

	/** true if employee claimed more than 14 exemption allowances for NY state. */
	private boolean isNYEmpClaimed;

	/** true if employee is a new hire or re-hire. */
	private boolean isEmpNewHire;

	/** first date employee performed services for pay. */
	private Date firstPayDate;

	/** true if  dependent Health Insurance benefit is available for the employee. */
	private Boolean dependentHealthInsurance;

	/** date when employee qualifies for dependent health insurance benefit. */
	private Date empQualifyDate;

	/** Employer's name and address. */
	private String employerNameAddr;

	/** Employer's identification number. */
	private String empIdNumber;

	//CA-W4 form field
	/** Employer payroll Tax account number. */
	private Integer empTaxAccountNumber;

	/** true if employee certify for penalty of perjury */
	private boolean isCertifiedForPenalty;
	// MA-W4 form field
	/** personal expemptions */
	private Integer personalExemptions;

	/** if married and exemptions for spouse is allowed */
	private Integer spouseExemptions;

	/** number of qualified dependents */
	private Integer qualifiedDependents;

	/** Head of Household Exemptions */
	private Integer headOfHouseholdExemptions;

	/** total claimed exemptions */
	private Integer claimedExemptions;

	/** true if you will file as head of household on your tax return */
	private boolean isHeadOfHousehold;

	/** true if employee is blind */
	private boolean isBlind;

	/** true if employee's spouse is blind */
	private boolean isSpouseBlind;

	/** true if employee is full time student engaged in seasonal */
	private boolean isFulltimeStudent;

	//OR-W4 form field
	/** Employee's redetermination */
	private boolean reDetermination;

	/** Exemption from withholding -exemption code */
	private String exemptCode;

	// MI-W4 form field

	private Date dateofBirth;

	private String licenseNo;

	private boolean isEmployerYes;

	private boolean isEmployerNo;

	private Date dateofHire;

	private Integer claiming;

	private Integer deductionAmount;

    /** 8a Michigan income tax liabilty  */
	private boolean is8a;

    /** 8b Wages are exempt for withholding */
	private boolean is8b;

	private String explain;

    /** 8c Permanent Home  */
	private boolean is8c;

	private String renaissanceZone;

	// NJ-W4

	/** Filling status single */
	private boolean single;

	/** Filling status Married/Civil Union Couple Joint */
	private boolean unionCoupleJoint;

	/** Filling status Married/Civil Union Partner Separate */
	private boolean unionCoupleSeparate;

	/** Filling status Head of Household */
	private boolean headofHousehold;

	/** Qualifying Widow(er)/Surviving Civil Union Partner */
	private boolean widower;

	private String instructionLetter;

	//IN-W4 form field
	/** Indiana County of residence as of January 1 */
	private String countyOfResidence;

	/** Indiana County of principal as of January 1 */
	private String countyOfPrincipal;

	/** Employee is 65 or older */
	private boolean isOlder;

	/** Employee's Spouse is 65 or older */
	private boolean isSpouseOlder;

	/** Total number of boxes checked */
	private Integer totalBoxesChecked;

	/** Total Exemptions */
	private Integer totalExemptions;

	/** Amount of additional state withholding each pay period */
	private Integer addStateWithHold;

	/** Amount of additional County withholding each pay period */
	private Integer addCountyWithHold;

    /** LS-3997
     * Complete address field to hold the complete address if the
     * State form does not have the address broken out into
     * separate fields.
     */
    private String completeAddress;

    /** Complete mailing address populated from mailing address */
    private String completeMailingAddress;

    //OH-W4 form field
	/** School district of residence */
	private String schoolDisName;

	/** School district no */
	private String schoolDisNo;

	//VA-W4 form field

	private Integer blindnessExemption;

	//NC-W4 form field
	/** Country Name */
	private String countryName;

	// MO-W4 form field
	/** Reduced Withholding */
	private Integer reducedWithHold;

   /** common checkboxes  */
    private boolean checkBox1;
    private boolean checkBox2;
    private boolean checkBox3;
    private boolean checkBox4;
    private boolean checkBox5;
    private boolean checkBox6;
    private boolean checkBox7;
	private boolean checkBox8;
	private boolean checkBox9;
	private boolean checkBox10;
	private boolean checkBox11;
	private boolean checkBox12;

	/** common exemptSatus  */
	private String exemptReason;

    // MN-W4 form field

    /** Martial Status */
	private boolean married;

	private boolean marriedWithhold;

    /** Minnesota Allowance */
	private boolean mnAllowances;

    /** Minnesota Allowance Section A*/
	private Integer mnAllowancesSectionA;

    /** Minnesota Allowance Section B*/
	private Integer mnAllowancesSectionB;

    /** Minnesota Allowance Section C*/
	private Integer mnAllowancesSectionC;

    /** Minnesota Allowance Section D*/
	private Integer mnAllowancesSectionD;

    /** Minnesota Allowance Section E*/
	private Integer mnAllowancesSectionE;

    /** Minnesota Allowance Section F*/
	private Integer mnAllowancesSectionF;

    /** Minnesota Withholding*/
    private boolean mnWithholding;

    /** Minnesota Withholding Section A*/
	private boolean mnWithholdingSectionA;

    /** Minnesota Withholding Section B*/
    private boolean mnWithholdingSectionB;

    /** Minnesota Withholding Section C*/
	private boolean mnWithholdingSectionC;

    /** Minnesota Withholding Section D*/
	private boolean mnWithholdingSectionD;

 	/** Minnesota Withholding Section E*/
	private boolean mnWithholdingSectionE;

    /** Minnesota Withholding Section F*/
	private boolean mnWithholdingSectionF;

	private Integer mnAllowancesSectionTotal;

	private Integer mnWithholdingSectionTotal;

	private String domicileLine;

	private String daytimePhoneNumber;

	// CT form fields
	/** withholding code letter */
	private String withholdingCode;

	/** true, iff employee is claiming MSRRA exmeption */
	private boolean msrraExempt;

	/** State of legal residence/domicile */
	private String legalStateOfRes;

	// AR form fields
	/** true iff qualify for low-income tax rate */
	private Boolean lowIncomeTax;

	//ME W4

	private boolean federalForm;

	private boolean taxLiability;

	private boolean perodicRetirement;

	private boolean militarySpouseResidency;

	//IA form fields
    /** if you are not exempt   */
    /** Personal Allowances   */
    private Integer personalAllowances;

    /** Allowances for dependent   */
    private Integer dependentAllowances;

   /**  Allowances for itemized deductions  */
    private Integer deductionAllowances;

    /**  Allowances for Adjustment  */
    private Integer adjustmentAllowances;

    /**  Allowances child and dependent care credit   */
    private Integer childDependentAllowances;

    /** Effective year */
    private Integer effectiveYear;

    // DC W4 fields
 	/** Total no of Allowance Section A */
 	private Integer allowancesSectionA;

	/** Total no of Allowance Section B */
	private Integer allowancesSectionB;

	private Integer allowancesSectionTotal;

	/** common radioButton */
	private Boolean radioButton1;
	private Boolean radioButton2;

	/** Ok W4 */
	private Integer allowancesSectionC;

	private Integer allowancesSectionD;

	private Integer allowancesSectionE;

	/** MT W4 */
	private Integer mtAllowancesTotal;

	/** PR W4 */
	/** Mailing address */
	private Address mailingAddress;
	/** Spouse's full name */
	private String spouseFullName;
	/** Spouse's social security number */
	private String spouseSocialSecurity;
	/** Optional computation of tax in the case of married individuals living
	 *  together and filing a joint return.
	 */
	private boolean optionalTaxComputation;
	/** provisions of the Military Spouses Residency Relief Act */
	private boolean militarySpouseResidencyRelief;
	/** Choose that the employer not consider exemption	 */
	private boolean employerNotConsiderExemption;
	/** Personal exemption status */
	private String personalExemptionStatus;
	/** Veteran exemption status */
	private String vetExemptionStatus;
	/** Home mortgage interest Allowance */
	private Integer homeMortgageInterestAllowance;
	/** Allowance for total charitable contributions */
	private Integer charitableContributationAllowance;
	/** Allowance for total amount of medical expenses */
	private Integer medicalExpensesAllowance;
	/** Allowance for total amount of student loan interest paid */
	private Integer studentLoanInterestAllowance;
	/** Goverment pension or retirement account contribution allowance */
	private Integer govPensionContributionsAllowance;
	/** Individual retirement account contribution allowance */
	private Integer individRetirementAcctAllowances;
	/** Allowance for total amount of educational contributions */
	private Integer educationContributionAllowance;
	/** Allowance for total amount of health saving account contributions */
	private Integer healthSavingAcctAllowance;
	/** Allowance for total amount on casualty losses of the principal residence */
	private Integer casualtyResidenceLossAllowance;
	/** Allowance for total amount of personal property loss for specific casualties */
	private Integer personalPropertyLossAllowance;
	/** Total amount of allowances */
	private Integer totalAllowancesAmt;
	/** Authorization for employer to withholding amount per pay period */
	private boolean authEmployerPayPeriodWithholding;
	/** Amount of employer withholding per pay period */
	private Integer employerWithholdPayPeriodAmt;
	/** Percentage of employer withholding per pay period */
	private Integer employerWithholdPayPeriodPercent;
	/** Number of Joint Custody dependents claimed. */
	private Integer jointCustodyDependents;
	/** If the employee participates in an government pension. */
	private boolean employeeParticipatesGovPension;
	/** Month of date of birth */
	private Integer dateOfBirthMonth;
	/** Day of date of birth */
	private Integer dateOfBirthDay;
	/** Year of date of birth */
	private Integer dateOfBirthYear;
	/** illinois */
	private boolean illinois;
	/** indiana */
	private boolean indiana;
	/** Michingan */
	private boolean michingan;
	/** WestVirgina */
	private boolean westVirgina;
	/** Wisconsin */
	private boolean wisconsin;
	/** Virginia */
	private boolean virginia;
	/** Ohio*/
	private boolean ohio;

	// WV W4 fields
	/** Employee's non-resident address if they are living out of state. */
	private Address nonResidentAddress;
	/** Full name of a non-resident employee */
	private String nonResidentFullName;
	/** Social Security number for a non-resident employee. */
	private String nonResidentSocialSecurity;
	/** Employee certifies by signing the form this is their State of legal residence/domicile */
	private String certifiedLegalStateOfRes;
	/** Number of Married exemptions */
	private Integer marriedExemptions;

	/**  true iff  for CA-W4 form  additional amount input type is $(Dollar) else %(Percent)*/
	private boolean additionalRadio = true;

	/** LS-4240 Flag to determine whether to show the signed event for the second or
	 * first page
	 */
	private boolean showSecondSignature;

	/** Default constructor */
	public FormStateW4() {
		super();
		setVersion(DEFAULT_VERSION);
	}

	/** Usual constructor - includes type of form. */
	public FormStateW4(PayrollFormType type) {
		this();
		setFormType(type);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		String maritalSingle = "Off";
		String maritalMarried = "Off";
		String maritalMarriedSep = "Off"; // LS-4099 Married filing separately. AS400 code is "j"
		String maritalOther = "Off";
		String maritalDisabled = "Off";
		String maritalMilitarySpouse = "Off";
		// LS-4234 Additional marital status;
		String maritalSingleNone = "Off";
		String maritalMarriedNone = "Off";
		String maritalChoice = "";
		if (getMarital() != null) {
			switch(getMarital()) {
				case MARITAL_STATUS_SINGLE:
					maritalSingle = "1";
					maritalChoice = "Choice1";
					break;
				case  MARITAL_STATUS_MARRIED:
				case "a":
					maritalMarried = "2";
					maritalChoice = "Choice2";
					break;
				case MARITAL_STATUS_EXTRA_WITHOLDINGS:
				case MARITAL_HEAD_HOUSEHOLD:
					maritalOther = "3";
					maritalChoice = "Choice3";
					break;
				case MARITAL_STATUS_MARRIED_SEPARATE: // Married filing separately
				case MARITAL_DISABLED: // Certified Disabled
					maritalMarriedSep = "4";
					maritalDisabled = "4";
					maritalChoice = "Choice4";
					break;
				case MARITAL_MILITARY_SPOUSE: //Military Spouse
					maritalMilitarySpouse = "5";
					break;
				case MARITAL_STATUS_SINGLE_NONE:
					maritalSingleNone = "6";
					maritalChoice = "Choice6";
					break;
				case MARITAL_STATUS_MARRIED_NONE:
					maritalMarriedNone = "7";
					maritalChoice = "Choice7";
					break;
			}
		}

		//LS-4234 Veteran Exemption
		String vetExemption = "Off";
		String vetExemptionNone = "Off";
		if (getVetExemptionStatus() != null) {
			switch(getVetExemptionStatus()) {
				case VET_EXEMPTION:
					vetExemption = "1";
					break;
				case  VET_NONE_EXEMPTION:
					vetExemptionNone = "2";
					break;
			}
		}
		fieldValues.put(FormFieldType.PRW4_VET_EXEMPTION_STATUS.name(), vetExemption);
		fieldValues.put(FormFieldType.PRW4_VET_NONE_EXEMPTION_STATUS.name(), vetExemptionNone);

		fieldValues.put(FormFieldType.FULL_NAME.name(), getFullName());
		fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), getFirstName());
		fieldValues.put(FormFieldType.USER_LAST_NAME.name(), getLastName());
		fieldValues.put(FormFieldType.USER_DOB_MONTH.name(), intFormat(getDateOfBirthMonth()));
		fieldValues.put(FormFieldType.USER_DOB_DAY.name(), intFormat(getDateOfBirthDay()));
		fieldValues.put(FormFieldType.USER_DOB_YEAR.name(), intFormat(getDateOfBirthYear()));

		fieldValues.put(FormFieldType.USER_MIDDLE_NAME.name(), getMiddleInitial());
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());
		// LS-4234
		fieldValues.put(FormFieldType.W4_SPOUSE_SOCIAL_SECURITY.name(), getSpouseSSNFormatted());

		fieldValues.put(FormFieldType.W4_MARITAL_CHOICE.name(), maritalChoice); // works in Team-designed form
		fieldValues.put(FormFieldType.W4_MARITAL_MARRIED.name(), maritalMarried); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_MARR_SEP.name(), maritalMarriedSep); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_SINGLE.name(), maritalSingle); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_OTHER.name(), maritalOther); // only used in original Fed form & 2019
		fieldValues.put(FormFieldType.W4_MARITAL_DISABLED.name(), maritalDisabled);
		fieldValues.put(FormFieldType.W4_MARITAL_MILITARY_SPOUSE.name(), maritalMilitarySpouse);
		fieldValues.put(FormFieldType.W4_MARITAL_NONE_SINGLE.name(), maritalSingleNone);
		fieldValues.put(FormFieldType.W4_MARITAL_NNE_MARRIED.name(), maritalMarriedNone);

		if (getAddress() != null) {
			// Populate all common forms off address fields (not all used in any one W4 form)
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(),
					(getAddress().getAddrLine1() != null ? getAddress().getAddrLine1() : "") + " " +
							(getAddress().getAddrLine2() != null ? getAddress().getAddrLine2()
									: ""));
			fieldValues.put(FormFieldType.HOME_ADDR_LINE1.name(), getAddress().getAddrLine1());
			fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), getAddress().getAddrLine2());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), getAddress().getCityStateZip());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), getAddress().getCity());
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), getAddress().getState());
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), getAddress().getZip());
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTRY.name(), getAddress().getCountry());
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTY.name(), getAddress().getCounty());
			fieldValues.put(FormFieldType.HOME_ADDR_COMPLETE.name(), getAddress().getCompleteAddress());
		}
		else {
			// "clear" all common forms off address fields
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_LINE1.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTRY.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTY.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_COMPLETE.name(), "");
		}

		// LS-4234
		if (getMailingAddress() != null) {
			// Populate all common forms off address fields (not all used in any one W4 form)
			fieldValues.put(FormFieldType.MAILING_ADDR_STREET.name(),
					(getMailingAddress().getAddrLine1() != null ? getMailingAddress().getAddrLine1() : "") + " " +
							(getMailingAddress().getAddrLine2() != null ? getMailingAddress().getAddrLine2()
									: ""));
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE1.name(), getMailingAddress().getAddrLine1());
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), getMailingAddress().getAddrLine2());
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_STATE_ZIP.name(), getMailingAddress().getCityStateZip());
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), getMailingAddress().getCity());
			fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), getMailingAddress().getState());
			fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), getMailingAddress().getZip());
			fieldValues.put(FormFieldType.MAILING_ADDR_COUNTRY.name(), getMailingAddress().getCountry());
			fieldValues.put(FormFieldType.MAILING_ADDR_COUNTY.name(), getMailingAddress().getCounty());
			fieldValues.put(FormFieldType.MAILING_ADDR_COMPLETE.name(), getMailingAddress().getCompleteAddress());
		}
		else {
			// "clear" all common forms off address fields
			fieldValues.put(FormFieldType.MAILING_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE1.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_COUNTRY.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_COUNTY.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_COMPLETE.name(), "");
		}

		// LS-4242
		if (getNonResidentAddress() != null) {
			// Populate all common forms off address fields (not all used in any one W4 form)
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_STREET.name(),
					(getNonResidentAddress().getAddrLine1() != null ? getNonResidentAddress().getAddrLine1() : "") + " " +
							(getNonResidentAddress().getAddrLine2() != null ? getNonResidentAddress().getAddrLine2()
									: ""));
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_LINE1.name(), getNonResidentAddress().getAddrLine1());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_LINE2.name(), getNonResidentAddress().getAddrLine2());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_CITY_STATE_ZIP.name(), getNonResidentAddress().getCityStateZip());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_CITY_ONLY.name(), getNonResidentAddress().getCity());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_STATE.name(), getNonResidentAddress().getState());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_ZIP.name(), getNonResidentAddress().getZip());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COUNTRY.name(), getNonResidentAddress().getCountry());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COUNTY.name(), getNonResidentAddress().getCounty());
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COMPLETE.name(), getNonResidentAddress().getCompleteAddress());
		}
		else {
			// "clear" all common forms off address fields
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_LINE1.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_LINE2.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_ZIP.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COUNTRY.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COUNTY.name(), "");
			fieldValues.put(FormFieldType.NONRESIDENT_ADDR_COMPLETE.name(), "");
		}
		fieldValues.put(FormFieldType.W4_ALLOWANCES.name(), patternFormat("##,###", getAllowances())); // page 1, Line 5
		fieldValues.put(FormFieldType.W4_ADDTL_AMOUNT.name(), patternFormat("##,###", getAdditionalAmount())); // page 1, Line 6
		fieldValues.put(FormFieldType.W4_EXEMPT.name(), getExempt() ? "Exempt" : ""); // page 1, Line 7

		/*if (true) { //test for signature
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "Emp-date"); // page 1, Line 6
		}*/
		if (cd.getEmpSignature() != null && ! showSecondSignature) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		if (cd.getEmpSignature() != null && showSecondSignature) { // Show the second signature if valid
			fieldValues.put(FormFieldType.EMPLOYEE_2_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_2_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_2_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_2_SIGNATURE_DATE.name(), "");
		}

		fieldValues.put(FormFieldType.W4_DEDUCTIONS.name(), patternFormat("##0.", getAllowances())); // Deductions&Adjustments, line 1
		fieldValues.put(FormFieldType.W4_TOTAL_ALLOWANCES_AMT.name(), patternFormat("##0.", getTotalAllowancesAmt())); // Deductions&Adjustments, line 1
		fieldValues.put(FormFieldType.W4_COUNTRY_NAME.name(), getCountryName());
		fieldValues.put(FormFieldType.W4_COMPLETE_ADDRESS.name(), getCompleteAddress());
		fieldValues.put(FormFieldType.W4_COMPLETE_MAILING_ADDRESS.name(), getCompleteMailingAddress());
		// Personal Exemptions LS-4099
		fieldValues.put(FormFieldType.W4_PERSONAL_SELF.name(), patternFormat("##,###",getPersonalExemptions()));
		fieldValues.put(FormFieldType.W4_PERSONAL_DEPENDENTS.name(),  patternFormat("##,###",getQualifiedDependents()));
		fieldValues.put(FormFieldType.W4_PERSONAL_MARRIED.name(),  patternFormat("##,###",getMarriedExemptions()));
		fieldValues.put(FormFieldType.W4_PERSONAL_HOH.name(),  patternFormat("##,###",getHeadOfHouseholdExemptions()));
		fieldValues.put(FormFieldType.W4_PERSONAL_SPOUSE.name(),  patternFormat("##,###",getSpouseExemptions()));
		fieldValues.put(FormFieldType.W4_TOTAL_EXEMPTIONS.name(), patternFormat("##,###",getTotalExemptions()));
		// ls=4234
		fieldValues.put(FormFieldType.W4_FULL_SPOUSE_NAME.name(), getSpouseFullName());
		fieldValues.put(FormFieldType.W4_SUM_DEDUCTIONS.name(), patternFormat("##,##0", getDeductionAmount()));
		// LS-4242
		fieldValues.put(FormFieldType.W4_NONRESIDENT_NAME.name(), getNonResidentFullName());
		fieldValues.put(FormFieldType.W4_NONRESIDENT_SOCIAL_SECURITY.name(), getNonResidentSSNFormatted());
		fieldValues.put(FormFieldType.W4_LEGAL_STATE_OF_RESIDENCE.name(), getLegalStateOfRes());
		fieldValues.put(FormFieldType.W4_LESS_WITHHOLDINGS.name(), (isCheckBox1() ? "Yes" : "Off"));

		// AL W4 fields
		fieldValues.put(FormFieldType.AL_MARITAL_STATUS_CODE_S_MS.name(), getMaritalStatusCode1());
		fieldValues.put(FormFieldType.AL_MARITAL_STATUS_CODE_M_H.name(), getMaritalStatusCode2());

		// AR W4 fields
		String lowIncomeTaxPrt = "Off";
		if (getLowIncomeTax() != null) {
			if (getLowIncomeTax()) {
				lowIncomeTaxPrt = "Yes";
			}
			else {
				lowIncomeTaxPrt = "No";
			}
		}
		fieldValues.put(FormFieldType.AR_LOW_INCOME_TAX.name(), lowIncomeTaxPrt);
		fieldValues.put(FormFieldType.AR_EXEMPT_STATUS1.name(), getExemptStatus1() ? "Yes" : "No");
		fieldValues.put(FormFieldType.AR_EXEMPT_STATUS2.name(), getExemptStatus2() ? "Yes" : "No");
		fieldValues.put(FormFieldType.AR_EXEMPT_STATUS3.name(), getExemptStatus3() ? "Yes" : "No");


		// CA W4 fields LS-4476
		String amountType;
		if (getAdditionalRadio()) {
			amountType = "$";
		}
		else {
			amountType = "%";
		}
		fieldValues.put(FormFieldType.CAW4_CERTIFY_FOR_PENALTY.name(), (isCertifiedForPenalty() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.CAW4_EMP_TAX_ACCOUNT_NUMBER.name(), intFormat(getEmpTaxAccountNumber()));
		fieldValues.put(FormFieldType.CAW4_AMOUNT_TYPE.name(), amountType);

		// CT W4 fields
		fieldValues.put(FormFieldType.CTW4_WITHHOLDING_CODE.name(), getWithholdingCode());
		fieldValues.put(FormFieldType.CTW4_MSRRA_EXEMPTION.name(), getMsrraExempt() ? "Yes" :"Off");
		fieldValues.put(FormFieldType.CTW4_DOMICILE_STATE.name(), getLegalStateOfRes());

		//DC W4 fields
		fieldValues.put(FormFieldType.DC_W4_TOTAL_SECTION_A.name(), patternFormat("##0", getAllowancesSectionA()));
		fieldValues.put(FormFieldType.DC_W4_TOTAL_SECTION_B.name(), patternFormat("##0", getAllowancesSectionB()));
		fieldValues.put(FormFieldType.DC_W4_TOTAL_WITHHOLD_ALLOWANCE.name(), patternFormat("##0", getAllowancesSectionTotal()));

		String isDistColumbia = "Off";
		if (isRadioButton1() != null) {
			if (isRadioButton1()) {
				isDistColumbia = "Yes";
			}
			else {
				isDistColumbia = "No";
			}
		}
		String isFulltimeStud = "Off";
		if (isRadioButton2() != null) {
			if (isRadioButton2()) {
				isFulltimeStud = "Yes";
			}
			else {
				isFulltimeStud = "No";
			}
		}
		fieldValues.put(FormFieldType.DC_W4_DISTRICT_COLUMBIA.name(), isDistColumbia);
		fieldValues.put(FormFieldType.DC_W4_FULLTIME_STUDENT.name(), isFulltimeStud);
		String zip1 = "";
		String zip2 = "";
		if(!StringUtils.isEmpty(getAddress().getZip())) {
			zip1 = getAddress().getZip().substring(0 , 5);
			zip2 = getAddress().getZip().substring(5);
		}
		fieldValues.put(FormFieldType.DC_ZIPCODE.name(), zip1);
		fieldValues.put(FormFieldType.DC_EXT_ZIPCODE.name(), zip2);

		// IA W4 fields
		fieldValues.put(FormFieldType.IA_W4_EFFECTIVE_YEAR.name(), intFormat(getEffectiveYear()));
		fieldValues.put(FormFieldType.IA_W4_EXEMPT_LOW_INCOME.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.IA_W4_PERSONAL_ALLOWANCES.name(), intFormat(getPersonalAllowances()));
		fieldValues.put(FormFieldType.IA_W4_DEPENDENTS_ALLOWANCES.name(), intFormat(getDependentAllowances()));
		fieldValues.put(FormFieldType.IA_W4_DEDUCTION_ALLOWANCE.name(), intFormat(getDeductionAllowances()));
		fieldValues.put(FormFieldType.IA_W4_ADJUSTMENT_ALLOWANCES.name(), intFormat(getAdjustmentAllowances()));
		fieldValues.put(FormFieldType.IA_W4_CHILD_DEPENDENT_ALLOWANCES.name(), intFormat(getChildDependentAllowances()));

		// IN W4 fields
		fieldValues.put(FormFieldType.INW4_COUNTY_OF_RESIDENCE.name(), getCountyOfResidence());
		fieldValues.put(FormFieldType.INW4_COUNTY_OF_PRINCIPAL.name(), getCountyOfPrincipal());
		fieldValues.put(FormFieldType.INW4_OLDER.name(), (isOlder() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.INW4_SPOUSE_OLDER.name(), (isSpouseOlder() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.INW4_TOTAL_CHECKED_BOXES.name(), intFormat(getTotalBoxesChecked()));
		fieldValues.put(FormFieldType.INW4_TOTAL_EXEMPTIONS.name(), intFormat(getTotalExemptions()));
		fieldValues.put(FormFieldType.INW4_ADD_STATE_WITHHOLD.name(), patternFormat("##,##0", getAddStateWithHold()));
		fieldValues.put(FormFieldType.INW4_ADD_COUNTY_WITHHOLD.name(), intFormat(getAddCountyWithHold()));

		// KS W4 field
		fieldValues.put(FormFieldType.KS_W4_UNION_COUPLE_JOINT.name(), (isUnionCoupleJoint() ? "Yes" : "Off"));

		// KY-W4
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_1.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_2.name(), (isCheckBox2() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_3.name(), (isCheckBox3() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_4.name(), (getCheckBox4() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_5.name(), (getCheckBox5() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_6.name(), (getCheckBox6() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_7.name(), (isCheckBox7() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_8.name(), (isCheckBox8() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_9.name(), (isCheckBox9() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_10.name(), (isCheckBox10() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_11.name(), (isCheckBox11() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_CHECK_BOX_12.name(), (isCheckBox12() ? "No" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_ILLINIOS_CHECK_BOX.name(), (isIllinois() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_INDIANA_CHECK_BOX.name(), (isIndiana() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_MICHINGAN_CHECK_BOX.name(), (isMichingan() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_WEST_VIRGIN_CHK_BOX.name(), (isWestVirgina() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_WISCONSIN_CHK_BOX.name(), (isWisconsin() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_VIRGINIA_CHK_BOX.name(), (isVirginia() ? "On" : "Off"));
		fieldValues.put(FormFieldType.KY_W4_OHIO_CHK_BOX.name(), (isOhio() ? "On" : "Off"));

		// MA W4 fields
		fieldValues.put(FormFieldType.MAW4_PERSONAL_EXEMPTIONS.name(), intFormat(getPersonalExemptions()));
		fieldValues.put(FormFieldType.MAW4_SPOUSE_EXEMPTIONS.name(), intFormat(getSpouseExemptions()));
		fieldValues.put(FormFieldType.MAW4_QUALIFIED_DEPENDENTS.name(), intFormat(getQualifiedDependents()));
		fieldValues.put(FormFieldType.MAW4_CLAIMED_EXEMPTIONS.name(), patternFormat("##,###", getClaimedExemptions()));

		fieldValues.put(FormFieldType.MAW4_HEAD_OF_HOUSEHOLD.name(),(isHeadOfHousehold() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MAW4_BLIND.name(),(isBlind() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MAW4_SPOUSE_BLIND.name(),(isSpouseBlind() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MAW4_FULLTIME_STUDENT.name(),(isFulltimeStudent() ? "Yes" : "No"));

		// MD-W4 fields
		fieldValues.put(FormFieldType.MDW4_DID_NOT_OWE_TAX.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MDW4_DO_NOT_EXPECT_OWE_TAX.name(), (isCheckBox2() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MDW4_DC_EXEMPTION.name(), (DOMICILED_STATE_DC.equals(getDomicileState()) ? "DC" : "Off"));
		fieldValues.put(FormFieldType.MDW4_VA_EXEMPTION.name(), (DOMICILED_STATE_VA.equals(getDomicileState()) ? "VA" : "Off"));
		fieldValues.put(FormFieldType.MDW4_WV_EXEMPTION.name(), (DOMICILED_STATE_WV.equals(getDomicileState()) ? "WV" : "Off"));
		fieldValues.put(FormFieldType.MDW4_1EXEMPT_STATUS.name(), (getExemptStatus1() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_2EXEMPT_STATUS.name(), (getExemptStatus2() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_3EXEMPT_STATUS.name(), (getExemptStatus3() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_4EXEMPT_STATUS.name(), (getExemptStatus4() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_5EXEMPT_STATUS.name(), (getExemptStatus5() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_6EXEMPT_STATUS.name(), (getExemptStatus6() ? EXEMPT_TEXT : ""));
		fieldValues.put(FormFieldType.MDW4_YEAR_EFFECTIVE.name(), intFormat(getApplicableYear()));
		fieldValues.put(FormFieldType.MDW4_RESIDENT_STATE.name(), getResidentState());

		// MI W4 fields
		fieldValues.put(FormFieldType.MIW4_LICENCE_NO.name(), getLicenseNo());
		fieldValues.put(FormFieldType.MIW4_EMPLOYER_YES.name(), (isEmployerYes() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MIW4_EMPLOYER_NO.name(), (isEmployerNo() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MIW4_HIRE_DATE.name(), dateFormat(format, getDateofHire()));
		fieldValues.put(FormFieldType.MIW4_DEDUCTION_AMOUNT.name(), intFormat(getDeductionAmount()));
		fieldValues.put(FormFieldType.MIW4_8A.name(), (isIs8a() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MIW4_8B.name(), (isIs8b() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MIW4_8C.name(), (isIs8c() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.MIW4_EXPLAIN.name(), getExplain());
		fieldValues.put(FormFieldType.MIW4_RENAISSANCE_ZONE.name(), getRenaissanceZone());
		fieldValues.put(FormFieldType.MIW4_CLAIMING.name(), intFormat(getClaiming()));
		fieldValues.put(FormFieldType.MIW4_DOB.name(), dateFormat(format,getDateofBirth()));

		// MN W4 fields
		fieldValues.put(FormFieldType.MN_W4_SINGLE.name(), (isSingle() ? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_MARRIED.name(), (isMarried() ? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_MARIED_WITHHOLD.name(), (isMarriedWithhold() ? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_ALLOWNCE.name(), (isMnAllowances() ? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_A.name(), intFormat(getMnAllowancesSectionA()));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_B.name(), intFormat(getMnAllowancesSectionB()));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_C.name(), intFormat(getMnAllowancesSectionC()));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_D.name(), intFormat(getMnAllowancesSectionD()));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_E.name(), intFormat(getMnAllowancesSectionE()));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_F.name(), intFormat(getMnAllowancesSectionF()));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLDING.name(), (isMnWithholding() ? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_A.name(), (isMnWithholdingSectionA()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_B.name(), (isMnWithholdingSectionB()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_C.name(), (isMnWithholdingSectionC()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_D.name(), (isMnWithholdingSectionD()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_E.name(), (isMnWithholdingSectionE()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_F.name(), (isMnWithholdingSectionF()? "On" : "Off"));
		fieldValues.put(FormFieldType.MN_W4_ALLOW_SECTION_TOTAL.name(), intFormat(getMnAllowancesSectionTotal()));
		fieldValues.put(FormFieldType.MN_W4_WITHHOLD_SECTION_TOTAL.name(), intFormat(getMnWithholdingSectionTotal()));
		fieldValues.put(FormFieldType.MN_W4_DOMICILE_LINE.name(), getDomicileLine());
		fieldValues.put(FormFieldType.MN_W4_DAY_TIME_PHONE_NUM.name(), getDaytimePhoneNumber());

		//MO-W4 fields
		fieldValues.put(FormFieldType.MOW4_REDUCED_WITHHOLD.name(), patternFormat("##,###", getReducedWithHold()));
		String exemptChoice = "";
		if (getExemptReason() != null) {
			switch(getExemptReason()) {
				case "m":
				exemptChoice = "Choice1";
					break;
				case "s":
				exemptChoice = "Choice2";
					break;
				case "i":
				exemptChoice = "Choice3";
					break;
			}
		}

		fieldValues.put(FormFieldType.MOW4_EXMPT_CHOICE.name(), exemptChoice);

		//MS fields
		fieldValues.put(FormFieldType.MS_W4_OLDER_HUSBAND.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_OLDER_WIFE.name(), (isCheckBox2() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_OLDER_SINGLE.name(), (isCheckBox3() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_BLIND_HUSBAND.name(), getCheckBox4() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.MS_W4_BLIND_WIFE.name(), (getCheckBox5() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_BLIND_SINGLE.name(), (getCheckBox6() ? "Yes" : "Off"));

		// MT W4 fields
		fieldValues.put(FormFieldType.MT_W4_ALLOWANCE_SECTION_TOTAL.name(), patternFormat("##,###", getMtAllowancesTotal()));

		// NJ W4 fields
		fieldValues.put(FormFieldType.NJ_W4_SINGLE.name(), (isSingle() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.NJ_W4_UNION_COUPLE_JOINT.name(), (isUnionCoupleJoint() ? "no" : "Off"));
		fieldValues.put(FormFieldType.NJ_W4_UNION_COUPLE_SEP.name(), (isUnionCoupleSeparate() ? "maybe" : "Off"));
		fieldValues.put(FormFieldType.NJ_W4_HEAD_HOUSE.name(), (isHeadofHousehold() ? "maybe1" : "Off"));
		fieldValues.put(FormFieldType.NJ_W4_WIDOWER.name(), (isWidower() ? "maybe2" : "Off"));
		fieldValues.put(FormFieldType.NJ_W4_CLAIM.name(), intFormat(getClaiming()));
		fieldValues.put(FormFieldType.NJ_W4_INSTRUCTION_LETTER.name(), getInstructionLetter());

		// NY W4 fields
		fieldValues.put(FormFieldType.NYW4_FIRST_AND_MIDDLE.name(), getFirstAndInitial());
		String nycResidentPrt = "Off";
		if (getNycResident() != null) {
			if (getNycResident()) {
				nycResidentPrt = "Yes";
			}
			else {
				nycResidentPrt = "No";
			}
		}
		String yonkersRes = "Off";
		if (getYonkersResident() != null) {
			if (getYonkersResident()) {
				yonkersRes = "Yes";
			}
			else {
				yonkersRes = "No";
			}
		}
		fieldValues.put(FormFieldType.NYW4_NYC_RESIDENT.name(), nycResidentPrt);
		fieldValues.put(FormFieldType.NYW4_YONKERS_RESIDENT.name(), yonkersRes);
		fieldValues.put(FormFieldType.NYW4_NYC_ALLOWANCES.name(), intFormat(getNycAllownces()));
		fieldValues.put(FormFieldType.NYW4_NYS_AMOUNT.name(), intFormat(getNysAmount()));
		fieldValues.put(FormFieldType.NYW4_NYC_AMOUNT.name(), intFormat(getNycAmount()));
		fieldValues.put(FormFieldType.NYW4_YONKERS_AMOUNT.name(), intFormat(getYonkersAmount()));
		fieldValues.put(FormFieldType.NYW4_EMP_CLAIMED_NYS.name(), ( getIsNYEmpClaimed()  ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.NYW4_EMP_NEW_HIRE.name(), (getIsEmpNewHire()  ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.NYW4_DEP_HEALTH_INSURANCE.name(),
				(getDependentHealthInsurance() != null && getDependentHealthInsurance() ? "Yes"
						: "No"));
		fieldValues.put(FormFieldType.NYW4_FIRST_PAY_DATE.name(),
				CalendarUtils.formatDate(getFirstPayDate(), "MM/dd/yyyy"));
		fieldValues.put(FormFieldType.NYW4_EMPLOYEE_QUALIFY_DATE.name(), CalendarUtils.formatDate(getEmpQualifyDate(), "MM/dd/yyyy"));
		fieldValues.put(FormFieldType.NYW4_EMPLOYER_NAME_ADDR.name(), getEmployerNameAddr());
		fieldValues.put(FormFieldType.W4_EMP_ID_NUMBER.name(), getEmpIdNumber());

		// ME W4
		fieldValues.put(FormFieldType.ME_W4_SINGLE.name(), (isSingle() ? "Single" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_MARRIED.name(), (isMarried() ? "Married" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_MARIED_WITHHOLD.name(),	(isMarriedWithhold() ? "Married but withholding at a higer single rate" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_EXMPT.name(), getExempt() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.ME_W4_FEDERAL_FORM.name(), (isFederalForm() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_TAX_LIABIITY.name(), (isTaxLiability() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_PERODIC_RETIREMENT.name(), (isPerodicRetirement() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.ME_W4_MILITARY_RESIDENCY.name(), (isMilitarySpouseResidency() ? "Yes" : "Off"));

		//MS fields
		fieldValues.put(FormFieldType.MS_W4_OLDER_HUSBAND.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_OLDER_WIFE.name(), (isCheckBox2() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_OLDER_SINGLE.name(), (isCheckBox3() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_BLIND_HUSBAND.name(), getCheckBox4() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.MS_W4_BLIND_WIFE.name(), (getCheckBox5() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.MS_W4_BLIND_SINGLE.name(), (getCheckBox6() ? "Yes" : "Off"));

		// OH-W4 field
		fieldValues.put(FormFieldType.OHW4_SCHOOL_DIS_NAME.name(), getSchoolDisName());
		fieldValues.put(FormFieldType.OHW4_SCHOOL_DIS_No.name(), getSchoolDisNo());

		// OK W4
		fieldValues.put(FormFieldType.OK_W4_MARIED_WITHHOLD.name(),
				(isMarriedWithhold() ? "Married higher single rate" : "Off"));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_A.name(), intFormat(getAllowancesSectionA()));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_B.name(), intFormat(getAllowancesSectionB()));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_C.name(), intFormat(getAllowancesSectionC()));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_D.name(), intFormat(getAllowancesSectionD()));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_E.name(), intFormat(getAllowancesSectionE()));
		fieldValues.put(FormFieldType.OK_W4_ALLOWANCE_SECTION_TOTAL.name(), patternFormat("##,###", getAllowancesSectionTotal()));
		fieldValues.put(FormFieldType.OK_W4_EMPLOYER_YES.name(), (isEmployerYes() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.OK_W4_EMPLOYER_NO.name(), (isEmployerNo() ? "No" : "Off"));
		String allowanceExemptionsA = "";
		if (null != getMnAllowancesSectionA() && getMnAllowancesSectionA() == 1) {
			allowanceExemptionsA = "Exempt";
		}
		String allowanceExemptionsB = "";
		if (null != getMnAllowancesSectionB() && getMnAllowancesSectionB() == 1) {
			allowanceExemptionsB = "Exempt";
		}
		String allowanceExemptionsC = "";
		if (null != getMnAllowancesSectionC() && getMnAllowancesSectionC() == 1) {
			allowanceExemptionsC = "Exempt";
		}
		fieldValues.put(FormFieldType.OK_W4_ALLOW_EXEMPTIONS_A.name(), allowanceExemptionsA);
		fieldValues.put(FormFieldType.OK_W4_ALLOW_EXEMPTIONS_B.name(), allowanceExemptionsB);
		fieldValues.put(FormFieldType.OK_W4_ALLOW_EXEMPTIONS_C.name(), allowanceExemptionsC);

		// OR-W4 field
		fieldValues.put(FormFieldType.ORW4_RE_DETERMINATION.name(), (getReDetermination() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.ORW4_EX_CODE.name(), getExemptCode());

		// PR-W4 fields
		fieldValues.put(FormFieldType.PRW4_OPTIONAL_TAX_COMPUTATION.name(), (getOptionalTaxComputation() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PRW4_MIL_SPOUSE_RELIEF_ACT.name(), (getMilitarySpouseResidencyRelief() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PRW4_EMPLOYER_NOT_CONSIDER_WITHHOLDING.name(), (getEmployerNotConsiderExemption() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PRW4_EMPLOYEE_PARTICIPATE_GOV_PENSION.name(), (getEmployeeParticipatesGovPension() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PRW4_AUTH_EMPLOYER_PAY_PERIOD_WITHHOLDING.name(), (getAuthEmployerPayPeriodWithholding() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PRW4_JOINT_CUSTODY_DEPENDENTS.name(), patternFormat("##0", getJointCustodyDependents()));
		fieldValues.put(FormFieldType.PRW4_HOME_MORTGAGE_INTERESTS_ALLOWANCE.name(), patternFormat("#,##0", getHomeMortgageInterestAllowance()));
		fieldValues.put(FormFieldType.PRW4_CHARITABLE_CONTRIBUTIONS_ALLOWANCE.name(), patternFormat("#,##0", getCharitableContributationAllowance()));
		fieldValues.put(FormFieldType.PRW4_MEDICAL_EXPENSES_ALLOWANCE.name(), patternFormat("#,##0", getMedicalExpensesAllowance()));
		fieldValues.put(FormFieldType.PRW4_STUDENT_LOANS_ALLOWANCE.name(), patternFormat("#,##0", getStudentLoanInterestAllowance()));
		fieldValues.put(FormFieldType.PRW4_GOV_PENSION_CONTRIBUTION_ALLOWANCE.name(), patternFormat("#,##0", getGovPensionContributionsAllowance()));
		fieldValues.put(FormFieldType.PRW4_INDIV_RETIREMENT_CONTRIBUTIONS_ALLOWANCE.name(), patternFormat("#,##0", getIndividRetirementAcctAllowances()));
		fieldValues.put(FormFieldType.PRW4_EDUCATION_CONTRIBUTION_ALLOWANCE.name(), patternFormat("#,##0", getEducationContributionAllowance()));
		fieldValues.put(FormFieldType.PRW4_HEALTH_SAVINGS_ACCTS_ALLOWANCE.name(), patternFormat("#,##0", getHealthSavingAcctAllowance()));
		fieldValues.put(FormFieldType.PRW4_CAUSALTY_RESIDENCE_LOSS_ALLOWANCE.name(), patternFormat("#,##0", getCasualtyResidenceLossAllowance()));
		fieldValues.put(FormFieldType.PRW4_PERSONAL_PROPERTY_LOSS_ALLOWANCE.name(), patternFormat("#,##0", getPersonalPropertyLossAllowance()));
		fieldValues.put(FormFieldType.PRW4_EMPLOYER_WITHHOLDING_PAY_PERIOD_AMT.name(), patternFormat("#,##0", getEmployerWithholdPayPeriodAmt()));
		fieldValues.put(FormFieldType.PRW4_EMPLOYER_WITHHOLDING_PAY_PERIOD_PERCENT.name(), patternFormat("#,##0", getEmployerWithholdPayPeriodPercent()));
		fieldValues.put(FormFieldType.PRW4_AMT_TOTAL_ALLOWANCES.name(), patternFormat("##,##0", getTotalAllowancesAmt()));


		// RI W4
		String personalExemptionsPrt = "";
		if (getPersonalExemptions() != null) {
			switch (getPersonalExemptions()) {
			case 1:
				personalExemptionsPrt = "Exempt";
				break;
			case 2:
				personalExemptionsPrt = "Exempt-MS";
				break;
			}
		}
		fieldValues.put(FormFieldType.RI_W4_PERSNL_EXEMPTIONS.name(), personalExemptionsPrt);

		//SC W4 form field
		fieldValues.put(FormFieldType.SC_W4_LASTNAME_DIFF.name(), (isCheckBox1() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.SC_W4_CHECK_EXEMPTION.name(), (isCheckBox2() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.SC_W4_DOMICILE_CHECK.name(),(isCheckBox3() ? "Yes" : "Off"));

		// VA W4 fields
		fieldValues.put(FormFieldType.VAW4_BLINDNESS_EXEMPT.name(), patternFormat("##,###", getBlindnessExemption()));
		fieldValues.put(FormFieldType.VAW4_TOTAL_EXEMPT.name(), intFormat(getTotalExemptions()));
		fieldValues.put(FormFieldType.VAW4_8A.name(), (isIs8a() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.VAW4_8B.name(), (isIs8b() ? "Yes" : "Off"));

		// WV W4
		fieldValues.put(FormFieldType.WVW4_CERT_STATE_OR_RESIDENCE.name(), getCertifiedLegalStateOfRes());
	}

	/**
	 * Export the fields in this instance using the supplied Exporter.
	 *
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append(getLastName());
		ex.append(getFirstName());
		ex.append(getMiddleInitial());
		ex.append(getSocialSecurity());
		Address addr = getAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);
		ex.append(getMarital());
		ex.append(getExemptReason());
		if (getExempt()) { // LS-849: Exempt Employee should transfer as 999 allowances
			ex.append(999);
		}
		else {
			ex.append(getAllowances());
		}
		ex.append(getAdditionalAmount());
		ex.append(getExempt());
	}

	@Override
	public void trim() {
		firstName = trim(firstName);
		lastName = trim(lastName);
		middleInitial = trim(middleInitial);
		socialSecurity = trim(socialSecurity);
		address.trimIsEmpty();
		employerNameAddr = trim(employerNameAddr);
		empIdNumber = trim(empIdNumber);
	}

	/** See {@link #formType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Form_Type", nullable = true, length = 20)
	public PayrollFormType getFormType() {
		return formType;
	}
	/** See {@link #formType}. */
	public void setFormType(PayrollFormType formType) {
		this.formType = formType;
		setVersion(DEFAULT_VERSION);
	}

	/** See {@link #fullName}. */
	@Column(name = "Full_Name", nullable = true, length = 62)
	public String getFullName() {
		return fullName;
	}
	/** See {@link #fullName}. */
	public void setFullName(String firstName) {
		fullName = firstName;
	}

	@Column(name = "First_Name", nullable = true, length = 30)
	/** See {@link #firstName}. */
	public String getFirstName() {
		return firstName;
	}
	/** See {@link #firstName}. */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/** See {@link #middleInitial}. */
	@Column(name = "Middle_Initial", nullable = true, length = 1)
	public String getMiddleInitial() {
		return middleInitial;
	}
	/** See {@link #middleInitial}. */
	public void setMiddleInitial(String middleInitial) {
		this.middleInitial = middleInitial;
	}

	/** See {@link #firstAndInitial}. */
	@Column(name = "First_and_initial", nullable = true, length = 32)
	public String getFirstAndInitial() {
		return firstAndInitial;
	}
	/** See {@link #firstAndInitial}. */
	public void setFirstAndInitial(String firstAndInitial) {
		this.firstAndInitial = firstAndInitial;
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
	/** LS-4356 Encrypt Social Security number */
	@Column(name = "Social_Security_Enc", length = 1000)
	@ColumnTransformer( // LS-2568 encrypt using MySql AES
			  read="AES_DECRYPT(Social_Security_Enc," + ENCRYPT_KEY + ")",
			  write="AES_ENCRYPT(?," + ENCRYPT_KEY + ")" )
	public String getSocialSecurity() {
		return socialSecurity;
	}
	/** See {@link #socialSecurity}. */
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

	/**
	 * @return Social security number formatted as nnn-nn-nnnn.  (If the value is not 9 characters long,
	 * then the unformatted value is returned.)
	 */
	@Transient
	public String getSSNFormatted() {
		String str = null;
		if (getSocialSecurity() != null && getSocialSecurity().length() == 9) {
			str = socialSecurity.substring(0,3) + "-" + socialSecurity.substring(3, 5) + "-" + socialSecurity.substring(5);
		}
		return str;
	}

	/**
	 * @return Spouse_Social security number formatted as nnn-nn-nnnn.  (If the value is not 9 characters long,
	 * then the unformatted value is returned.)
	 */
	@Transient
	public String getSpouseSSNFormatted() {
		String str = null;
		if (getSpouseSocialSecurity() != null && getSpouseSocialSecurity().length() == 9) {
			str = spouseSocialSecurity.substring(0,3) + "-" + spouseSocialSecurity.substring(3, 5) + "-" + spouseSocialSecurity.substring(5);
		}
		return str;
	}

	/**
	 * @return Non_Resident_Social security number formatted as nnn-nn-nnnn.  (If the value is not 9 characters long,
	 * then the unformatted value is returned.)
	 */
	@Transient
	public String getNonResidentSSNFormatted() {
		String str = null;
		if (getNonResidentSocialSecurity() != null && getNonResidentSocialSecurity().length() == 9) {
			str = nonResidentSocialSecurity.substring(0,3) + "-" + nonResidentSocialSecurity.substring(3, 5) + "-" + nonResidentSocialSecurity.substring(5);
		}
		return str;
	}

	/** See {@link #address}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	public Address getAddress() {
		return address;
	}
	/** See {@link #address}. */
	public void setAddress(Address address) {
		this.address = address;
	}

	/** See {@link #marital}. */
	@Column(name = "Marital", nullable = true, length = 1)
	public String getMarital() {
		return marital;
	}
	/** See {@link #marital}. */
	public void setMarital(String marital) {
		this.marital = marital;
	}

	/** See {@link #allowances}. */
	@Column(name = "Allowances", nullable = true)
	public Integer getAllowances() {
		return allowances;
	}
	/** See {@link #allowances}. */
	public void setAllowances(Integer allowances) {
		this.allowances = allowances;
	}

	/** See {@link #additionalAmount}. */
	@Column(name = "Additional_Amount", nullable = true)
	public Integer getAdditionalAmount() {
		return additionalAmount;
	}
	/** See {@link #additionalAmount}. */
	@Column(name = "Exempt", nullable = false)
	public void setAdditionalAmount(Integer addtlAmount) {
		additionalAmount = addtlAmount;
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

    /** See {@link #exemptStatus1}. */
    @Column(name = "Exempt_Status_1", nullable = false)
    public boolean getExemptStatus1() {
        return exemptStatus1;
    }

    /** See {@link #exemptStatus1}. */
    public void setExemptStatus1(boolean exemptStatus1) {
        this.exemptStatus1 = exemptStatus1;
    }

    /** See {@link #exemptStatus2}. */
    @Column(name = "Exempt_Status_2", nullable = false)
    public boolean getExemptStatus2() {
        return exemptStatus2;
    }

    /** See {@link #exemptStatus2}. */
    public void setExemptStatus2(boolean exemptStatus2) {
        this.exemptStatus2 = exemptStatus2;
    }

    /** See {@link #exemptStatus3}. */
    @Column(name = "Exempt_Status_3", nullable = false)
    public boolean getExemptStatus3() {
        return exemptStatus3;
    }

    /** See {@link #exemptStatus3}. */
    public void setExemptStatus3(boolean exemptStatus3) {
        this.exemptStatus3 = exemptStatus3;
    }

    /** See {@link #exemptStatus4}. */
    @Column(name = "Exempt_Status_4", nullable = false)
    public boolean getExemptStatus4() {
        return exemptStatus4;
    }

    /** See {@link #exemptStatus4}. */
    public void setExemptStatus4(boolean exemptStatus4) {
        this.exemptStatus4 = exemptStatus4;
    }

    /** See {@link #exemptStatus5}. */
    @Column(name = "Exempt_Status_5", nullable = false)
    public boolean getExemptStatus5() {
        return exemptStatus5;
    }

    /** See {@link #exemptStatus5}. */
    public void setExemptStatus5(boolean exemptStatus5) {
        this.exemptStatus5 = exemptStatus5;
    }

    /** See {@link #exemptStatus6}. */
    @Column(name = "Exempt_Status_6", nullable = false)
    public boolean getExemptStatus6() {
        return exemptStatus6;
    }

    /** See {@link #exemptStatus6}. */
    public void setExemptStatus6(boolean exemptStatus6) {
        this.exemptStatus6 = exemptStatus6;
    }

	/** See {@link #nycResident}. */
	@Column(name = "NYC_Resident")
	public Boolean getNycResident() {
		return nycResident;
	}

	/** See {@link #nycResident}. */
	public void setNycResident(Boolean nycResident) {
		this.nycResident = nycResident;
	}

	/** See {@link #yonkersResident}. */
	@Column(name = "Yonkers_Resident")
	public Boolean getYonkersResident() {
		return yonkersResident;
	}

	/** See {@link #yonkersResident}. */
	public void setYonkersResident(Boolean yonkersResident) {
		this.yonkersResident = yonkersResident;
	}

	/** See {@link #nycAllownces}. */
	@Column(name = "NYC_Allowances", nullable = true)
	public Integer getNycAllownces() {
		return nycAllownces;
	}

	/** See {@link #nycAllownces}. */
	public void setNycAllownces(Integer nycAllownces) {
		this.nycAllownces = nycAllownces;
	}

	/** See {@link #nysAmount}. */
	@Column(name = "NYS_Amount", nullable = true)
	public Integer getNysAmount() {
		return nysAmount;
	}

	/** See {@link #nyAmount}. */
	public void setNysAmount(Integer nysAmount) {
		this.nysAmount = nysAmount;
	}

	/** See {@link #nycAmount}. */
	@Column(name = "NYC_Amount", nullable = true)
	public Integer getNycAmount() {
		return nycAmount;
	}

	/** See {@link #nycAmount}. */
	public void setNycAmount(Integer nycAmount) {
		this.nycAmount = nycAmount;
	}

	/** See {@link #yonkersAmount}. */
	@Column(name = "Yonkers_Amount", nullable = true)
	public Integer getYonkersAmount() {
		return yonkersAmount;
	}

	/** See {@link #yonkersAmount}. */
	public void setYonkersAmount(Integer yonkersAmount) {
		this.yonkersAmount = yonkersAmount;
	}

	/** See {@link #isNYEmpClaimed}. */
	@Column(name = "NYS_EMP_Claim", nullable = false)
	public boolean getIsNYEmpClaimed() {
		return isNYEmpClaimed;
	}

	/** See {@link #isNYEmpClaimed}. */
	public void setIsNYEmpClaimed(boolean isNYEmpClaimed) {
		this.isNYEmpClaimed = isNYEmpClaimed;
	}

	/** See {@link #isEmpNewHire}. */
	@Column(name = "Emp_NewHire", nullable = false)
	public boolean getIsEmpNewHire() {
		return isEmpNewHire;
	}

	/** See {@link #isEmpNewHire}. */
	public void setIsEmpNewHire(boolean isEmpNewHire) {
		this.isEmpNewHire = isEmpNewHire;
	}

	/** See {@link #firstPayDate}. */
	@Column(name = "First_Date_Employment")
	public Date getFirstPayDate() {
		return firstPayDate;
	}

	/** See {@link #firstPayDate}. */
	public void setFirstPayDate(Date firstPayDate) {
		this.firstPayDate = firstPayDate;
	}

	/** See {@link #dependentHealthInsurance}. */
	@Column(name = "Dependent_Health_Ins", nullable = false)
	public Boolean getDependentHealthInsurance() {
		return dependentHealthInsurance;
	}

	/** See {@link #dependentHealthInsurance}. */
	public void setDependentHealthInsurance(Boolean dependentHealthInsurance) {
		this.dependentHealthInsurance = dependentHealthInsurance;
	}

	/** See {@link #empQualifyDate}. */
	@Column(name = "Emp_Qualify_Date")
	public Date getEmpQualifyDate() {
		return empQualifyDate;
	}

	/** See {@link #empQualifyDate}. */
	public void setEmpQualifyDate(Date empQualifyDate) {
		this.empQualifyDate = empQualifyDate;
	}

	/** See {@link #employerNameAddr}. */
	@Column(name = "Emp_Name_Address", nullable = true)
	public String getEmployerNameAddr() {
		return employerNameAddr;
	}

	/** See {@link #employerNameAddr}. */
	public void setEmployerNameAddr(String employerNameAddr) {
		this.employerNameAddr = employerNameAddr;
	}

	/** See {@link #empIdNumber}. */
	@Column(name = "Emp_Id_Number" , nullable = true)
	public String getEmpIdNumber() {
		return empIdNumber;
	}

	/** See {@link #empIdNumber}. */
	public void setEmpIdNumber(String empIdNumber) {
		this.empIdNumber = empIdNumber;
	}

	/** See {@link #empTaxAccountNumber}. */
	@Column(name = "Emp_Tax_Account_Number" , nullable = true)
	public Integer getEmpTaxAccountNumber() {
		return empTaxAccountNumber;
	}

	/** See {@link #empTaxAccountNumber}. */
	public void setEmpTaxAccountNumber(Integer empTaxAccountNumber) {
		this.empTaxAccountNumber = empTaxAccountNumber;
	}

	/** See {@link #isCertifyPenalty}. */
	@Column(name = "Certified_For_Penalty", nullable = false)
	public boolean isCertifiedForPenalty() {
		return isCertifiedForPenalty;
	}

	/** See {@link #isCertifyPenalty}. */
	public void setCertifiedForPenalty(boolean isCertifiedForPenalty) {
		this.isCertifiedForPenalty = isCertifiedForPenalty;
	}

	/** See {@personalExemptions .*/
	@Column(name = "Personal_Exemptions", nullable = true)
	public Integer getPersonalExemptions() {
		return personalExemptions;
	}

	/** See {@personalExemptions}. */
	public void setPersonalExemptions(Integer personalExemptions) {
		this.personalExemptions = personalExemptions;
	}

	/** See {@marriedExemptions}. */
	@Column(name = "Spouse_Exemptions", nullable = true)
	public Integer getSpouseExemptions() {
		return spouseExemptions;
	}
	/** See {@marriedExemptions}. */
	public void setSpouseExemptions(Integer spouseExemptions) {
		this.spouseExemptions = spouseExemptions;
	}

	/** See {@link #headOfHouseholdExemptions}. */
	@Column(name = "Head_Household_Exemptions", nullable = true)
	public Integer getHeadOfHouseholdExemptions() {
		return headOfHouseholdExemptions;
	}

	/** See {@link #headOfHouseholdExemptions}. */
	public void setHeadOfHouseholdExemptions(Integer headOfHouseholdExemptions) {
		this.headOfHouseholdExemptions = headOfHouseholdExemptions;
	}

	/** See {@qualifiedDependents}. */
	@Column(name = "Qualified_Dependents", nullable = true)
	public Integer getQualifiedDependents() {
		return qualifiedDependents;
	}

	/** See {@qualifiedDependents }. */
	public void setQualifiedDependents(Integer qualifiedDependents) {
		this.qualifiedDependents = qualifiedDependents;
	}

	/** See {@claimedExemptions}. */
	@Column(name = "Claimed_Exemptions ", nullable = true)
	public Integer getClaimedExemptions() {
		return claimedExemptions;
	}

	/** See {@claimedExemptions}. */
	public void setClaimedExemptions(Integer claimedExemptions) {
		this.claimedExemptions = claimedExemptions;
	}

	/** See {@isHeadOfHousehold}. */
	@Column(name = "Head_Of_Household", nullable = false)
	public boolean isHeadOfHousehold() {
		return isHeadOfHousehold;
	}

	/** See {@isHeadOfHousehold}. */
	public void setHeadOfHousehold(boolean isHeadOfHousehold) {
		this.isHeadOfHousehold = isHeadOfHousehold;
	}

	/** See {@isBlind}. */
	@Column(name = "Blind", nullable = false)
	public boolean isBlind() {
		return isBlind;
	}

	/** See {@isBlind}. */
	public void setBlind(boolean isBlind) {
		this.isBlind = isBlind;
	}

	/** See {@isSpouseBlind}. */
	@Column(name = "Spouse_Blind", nullable = false)
	public boolean isSpouseBlind() {
		return isSpouseBlind;
	}

	/** See {@isSpouseBlind}. */
	public void setSpouseBlind(boolean isSpouseBlind) {
		this.isSpouseBlind = isSpouseBlind;
	}

	/** See {@isFulltimeStudent}. */
	@Column(name = "Fulltime_Student", nullable = false)
	public boolean isFulltimeStudent() {
		return isFulltimeStudent;
	}

	/** See {@isFulltimeStudent}. */
	public void setFulltimeStudent(boolean isFulltimeStudent) {
		this.isFulltimeStudent = isFulltimeStudent;
	}

	/** See {@exemptCode}. */
	@Column(name = "Exempt_Code", nullable = false)
	public String getExemptCode() {
		return exemptCode;
	}

	/** See {@exemptCode}. */
	public void setExemptCode(String exemptCode) {
		this.exemptCode = exemptCode;
	}

	/** See {@reDetermination}. */
	@Column(name = "Re_Determination", nullable = false)
	public boolean getReDetermination() {
		return reDetermination;
	}

	/** See {@reDetermination}. */
	public void setReDetermination(boolean reDetermination) {
		this.reDetermination = reDetermination;
	}

	/** See {@link #dateofBirth}. */
	@Column(name = "Date_of_Birth", nullable = false)
	public Date getDateofBirth() {
		return dateofBirth;
	}
	/** See {@link #dateofBirth}. */
	public void setDateofBirth(Date dateofBirth) {
		this.dateofBirth = dateofBirth;
	}

	/** See {@link #dateOfBirthMonth}. */
	@Column(name = "Date_Of_Birth_Month")
	public Integer getDateOfBirthMonth() {
		return dateOfBirthMonth;
	}

	/** See {@link #dateOfBirthMonth}. */
	public void setDateOfBirthMonth(Integer dateOfBirthMonth) {
		this.dateOfBirthMonth = dateOfBirthMonth;
	}

	/** See {@link #dateOfBirthDay}. */
	@Column(name = "Date_Of_Birth_Day")
	public Integer getDateOfBirthDay() {
		return dateOfBirthDay;
	}

	/** See {@link #dateOfBirthDay}. */
	public void setDateOfBirthDay(Integer dateOfBirthDay) {
		this.dateOfBirthDay = dateOfBirthDay;
	}

	/** See {@link #dateOfBirthYear}. */
	@Column(name = "Date_Of_Birth_Year")
	public Integer getDateOfBirthYear() {
		return dateOfBirthYear;
	}

	/** See {@link #dateOfBirthYear}. */
	public void setDateOfBirthYear(Integer dateOfBirthYear) {
		this.dateOfBirthYear = dateOfBirthYear;
	}

	/** See {@link #licenseNo}. */
	@Column(name = "License_No", nullable = false)
	public String getLicenseNo() {
		return licenseNo;
	}
	/** See {@link #licenseNo}. */
	public void setLicenseNo(String licenseNo) {
		this.licenseNo = licenseNo;
	}


	/** See {@link #dateofHire}. */
	@Column(name = "Date_of_Hire", nullable = false)
	public Date getDateofHire() {
		return dateofHire;
	}
	/** See {@link #dateofHire}. */
	public void setDateofHire(Date dateofHire) {
		this.dateofHire = dateofHire;
	}

	/** See {@link #claiming}. */
	@Column(name = "Claiming", nullable = false)
	public Integer getClaiming() {
		return claiming;
	}

	/** See {@link #isEmployerYes}. */
	@Column(name = "Employer_Yes", nullable = false)
	public boolean isEmployerYes() {
		return isEmployerYes;
	}
	/** See {@link #isEmployerYes}. */
	public void setEmployerYes(boolean isEmployerYes) {
		this.isEmployerYes = isEmployerYes;
	}

	/** See {@link #isEmployerNo}. */
	@Column(name = "Employer_No", nullable = false)
	public boolean isEmployerNo() {
		return isEmployerNo;
	}
	/** See {@link #isEmployerNo}. */
	public void setEmployerNo(boolean isEmployerNo) {
		this.isEmployerNo = isEmployerNo;
	}

	/** See {@link #claiming}. */
	public void setClaiming(Integer claiming) {
		this.claiming = claiming;
	}

	/** See {@link #deductionAmount}. */
	@Column(name = "Deduction_Amount", nullable = false)
	public Integer getDeductionAmount() {
		return deductionAmount;
	}
	/** See {@link #deductionAmount}. */
	public void setDeductionAmount(Integer deductionAmount) {
		this.deductionAmount = deductionAmount;
	}

	/** See {@link #is8a}. */
	@Column(name = "8a", nullable = false)
	public boolean isIs8a() {
		return is8a;
	}
	/** See {@link #is8a}. */
	public void setIs8a(boolean is8a) {
		this.is8a = is8a;
	}

	/** See {@link #is8b}. */
	@Column(name = "8b", nullable = false)
	public boolean isIs8b() {
		return is8b;
	}
	/** See {@link #is8b}. */
	public void setIs8b(boolean is8b) {
		this.is8b = is8b;
	}

	/** See {@link #explain}. */
	@Column(name = "Explaination", nullable = false)
	public String getExplain() {
		return explain;
	}
	/** See {@link #explain}. */
	public void setExplain(String explain) {
		this.explain = explain;
	}

	/** See {@link #is8c}. */
	@Column(name = "8c", nullable = false)
	public boolean isIs8c() {
		return is8c;
	}
	/** See {@link #is8c}. */
	public void setIs8c(boolean is8c) {
		this.is8c = is8c;
	}

	/** See {@link #renaissanceZone}. */
	@Column(name = "Renaissance_Zone", nullable = false)
	public String getRenaissanceZone() {
		return renaissanceZone;
	}
	/** See {@link #renaissanceZone}. */
	public void setRenaissanceZone(String renaissanceZone) {
		this.renaissanceZone = renaissanceZone;
	}

	/** See {@link #single}. */
	@Column(name = "Single", nullable = false)
	public boolean isSingle() {
		return single;
	}
	/** See {@link #single}. */
	public void setSingle(boolean single) {
		this.single = single;
	}

	/** See {@link #unionCoupleJoint}. */
	@Column(name = "Union_Couple_Joint", nullable = false)
	public boolean isUnionCoupleJoint() {
		return unionCoupleJoint;
	}
	/** See {@link #unionCoupleJoint}. */
	public void setUnionCoupleJoint(boolean unionCoupleJoint) {
		this.unionCoupleJoint = unionCoupleJoint;
	}

	/** See {@link #unionCoupleSeparate}. */
	@Column(name = "Union_Couple_Separate", nullable = false)
	public boolean isUnionCoupleSeparate() {
		return unionCoupleSeparate;
	}
	/** See {@link #unionCoupleSeparate}. */
	public void setUnionCoupleSeparate(boolean unionCoupleSeparate) {
		this.unionCoupleSeparate = unionCoupleSeparate;
	}

	/** See {@link #headofHousehold}. */
	@Column(name = "Head_of_House", nullable = false)
	public boolean isHeadofHousehold() {
		return headofHousehold;
	}
	/** See {@link #headofHousehold}. */
	public void setHeadofHousehold(boolean headofHousehold) {
		this.headofHousehold = headofHousehold;
	}

	/** See {@link #widower}. */
	@Column(name = "Widower", nullable = false)
	public boolean isWidower() {
		return widower;
	}
	/** See {@link #widower}. */
	public void setWidower(boolean widower) {
		this.widower = widower;
	}

	/** See {@link #instructionLetter}. */
	@Column(name = "Instruction_Letter", nullable = false)
	public String getInstructionLetter() {
		return instructionLetter;
	}
	/** See {@link #instructionLetter}. */
	public void setInstructionLetter(String instructionLetter) {
		this.instructionLetter = instructionLetter;
	}

	/** See {@link #blindnessExemption}. */
    @Column(name = "Blindness_Exemption", nullable = false)
	public Integer getBlindnessExemption() {
		return blindnessExemption;
	}
	/** See {@link #blindnessExemption}. */
	public void setBlindnessExemption(Integer blindnessExemption) {
		this.blindnessExemption = blindnessExemption;
	}

	/** See {@link #countyOfResidence}. */
	@Column(name = "County_Of_Residence" , nullable = true)
	public String getCountyOfResidence() {
		return countyOfResidence;
	}

	/** See {@link #countyOfResidence}. */
	public void setCountyOfResidence(String countyOfResidence) {
		this.countyOfResidence = countyOfResidence;
	}

	/** See {@link #countyOfPrincipal}. */
	@Column(name = "County_Of_Principal" , nullable = true)
	public String getCountyOfPrincipal() {
		return countyOfPrincipal;
	}

	/** See {@link #countyOfPrincipal}. */
	public void setCountyOfPrincipal(String countyOfPrincipal) {
		this.countyOfPrincipal = countyOfPrincipal;
	}

	/** See {@link #isOlder}. */
	@Column(name = "Older", nullable = false)
	public boolean isOlder() {
		return isOlder;
	}

	/** See {@link #isOlder}. */
	public void setOlder(boolean isOlder) {
		this.isOlder = isOlder;
	}

	/** See {@link #isSpouseOlder}. */
	@Column(name = "Spouse_Older", nullable = false)
	public boolean isSpouseOlder() {
		return isSpouseOlder;
	}

	/** See {@link #isSpouseOlder}. */
	public void setSpouseOlder(boolean isSpouseOlder) {
		this.isSpouseOlder = isSpouseOlder;
	}

	/** See {@link #totalBoxesChecked}. */
	@Column(name = "Total_Boxes" , nullable = true)
	public Integer getTotalBoxesChecked() {
		return totalBoxesChecked;
	}

	/** See {@link #totalBoxesChecked}. */
	public void setTotalBoxesChecked(Integer totalBoxesChecked) {
		this.totalBoxesChecked = totalBoxesChecked;
	}

	/** See {@link #totalExemptions}. */
	@Column(name = "Total_Exemptions" , nullable = true)
	public Integer getTotalExemptions() {
		return totalExemptions;
	}

	/** See {@link #totalExemptions}. */
	public void setTotalExemptions(Integer totalExemptions) {
		this.totalExemptions = totalExemptions;
	}

	/** See {@link #addStateWithHold}. */
	@Column(name = "Add_State_Withhold" , nullable = true)
	public Integer getAddStateWithHold() {
		return addStateWithHold;
	}

	/** See {@link #addStateWithHold}. */
	public void setAddStateWithHold(Integer addStateWithHold) {
		this.addStateWithHold = addStateWithHold;
	}

	/** See {@link #addCountyWithHold}. */
	@Column(name = "Add_County_Withhold" , nullable = true)
	public Integer getAddCountyWithHold() {
		return addCountyWithHold;
	}

	/** See {@link #addCountyWithHold}. */
	public void setAddCountyWithHold(Integer addCountyWithHold) {
		this.addCountyWithHold = addCountyWithHold;
	}

	/** See {@link #schoolDisName}. */
	@Column(name = "School_Dis_Name", nullable = true, length = 50)
	public String getSchoolDisName() {
		return schoolDisName;
	}

	/** See {@link #schoolDisName}. */
	public void setSchoolDisName(String schoolDisName) {
		this.schoolDisName = schoolDisName;
	}

	/** See {@link #schoolDisNo}. */
	@Column(name = "School_Dis_No", nullable = true, length = 30)
	public String getSchoolDisNo() {
		return schoolDisNo;
	}

	/** See {@link #schoolDisNo}. */
	public void setSchoolDisNo(String schoolDisNo) {
		this.schoolDisNo = schoolDisNo;
	}

	/** See {@link #county}. */
	@Column(name = "Country_Name", nullable = true, length = 30)
	public String getCountryName() {
		return countryName;
	}

	/** See {@link #county}. */
	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	/** See {@link #reducedWithHold}. */
	@Column(name = "Reduced_Withholding" , nullable = true)
	public Integer getReducedWithHold() {
		return reducedWithHold;
	}

	/** See {@link #reducedWithHold}. */
	public void setReducedWithHold(Integer reducedWithHold) {
		this.reducedWithHold = reducedWithHold;
	}

	/** See {@link #exemptReason}. */
	@Column(name = "Exempt_Reason", nullable = true, length = 1)
	public String getExemptReason() {
		return exemptReason;
	}

	/** See {@link #exemptReason}. */
	public void setExemptReason(String exemptReason) {
		this.exemptReason = exemptReason;
	}

	/** See {@link #married}. */
	@Column(name = "Married", nullable = false)
	public boolean isMarried() {
		return married;
	}
	/** See {@link #married}. */
	public void setMarried(boolean married) {
		this.married = married;
	}

	/** See {@link #marriedWithhold}. */
	@Column(name = "Married_Withhold", nullable = false)
	public boolean isMarriedWithhold() {
		return marriedWithhold;
    }
	/** See {@link #marriedWithhold}. */
	public void setMarriedWithhold(boolean marriedWithhold) {
		this.marriedWithhold = marriedWithhold;
	}

	/** See {@link #mnAllowances}. */
	@Column(name = "MN_Allownaces", nullable = false)
	public boolean isMnAllowances() {
		return mnAllowances;
	}
	/** See {@link #mnAllowances}. */
	public void setMnAllowances(boolean mnAllowances) {
		this.mnAllowances = mnAllowances;
	}

	/** See {@link #mnAllowancesSectionA}. */
	@Column(name = "MN_Allownaces_Section_A", nullable = false)
	public Integer getMnAllowancesSectionA() {
		return mnAllowancesSectionA;
    }
	/** See {@link #mnAllowancesSectionA}. */
	public void setMnAllowancesSectionA(Integer mnAllowancesSectionA) {
		this.mnAllowancesSectionA = mnAllowancesSectionA;
	}

	/** See {@link #mnAllowancesSectionB}. */
	@Column(name = "MN_Allownaces_Section_B", nullable = false)
	public Integer getMnAllowancesSectionB() {
		return mnAllowancesSectionB;
	}
	/** See {@link #mnAllowancesSectionB}. */
	public void setMnAllowancesSectionB(Integer mnAllowancesSectionB) {
		this.mnAllowancesSectionB = mnAllowancesSectionB;
	}

	/** See {@link #mnAllowancesSectionC}. */
	@Column(name = "MN_Allownaces_Section_C", nullable = false)
	public Integer getMnAllowancesSectionC() {
		return mnAllowancesSectionC;
	}
	/** See {@link #mnAllowancesSectionC}. */
	public void setMnAllowancesSectionC(Integer mnAllowancesSectionC) {
		this.mnAllowancesSectionC = mnAllowancesSectionC;
	}

	/** See {@link #mnAllowancesSectionD}. */
	@Column(name = "MN_Allownaces_Section_D", nullable = false)
	public Integer getMnAllowancesSectionD() {
		return mnAllowancesSectionD;
	}
	/** See {@link #mnAllowancesSectionD}. */
	public void setMnAllowancesSectionD(Integer mnAllowancesSectionD) {
		this.mnAllowancesSectionD = mnAllowancesSectionD;
	}

	/** See {@link #mnAllowancesSectionE}. */
	@Column(name = "MN_Allownaces_Section_E", nullable = false)
	public Integer getMnAllowancesSectionE() {
		return mnAllowancesSectionE;
	}
	/** See {@link #mnAllowancesSectionE}. */
	public void setMnAllowancesSectionE(Integer mnAllowancesSectionE) {
		this.mnAllowancesSectionE = mnAllowancesSectionE;
	}

	/** See {@link #mnAllowancesSectionF}. */
	@Column(name = "MN_Allownaces_Section_F", nullable = false)
	public Integer getMnAllowancesSectionF() {
		return mnAllowancesSectionF;
	}
	/** See {@link #mnAllowancesSectionF}. */
	public void setMnAllowancesSectionF(Integer mnAllowancesSectionF) {
		this.mnAllowancesSectionF = mnAllowancesSectionF;
	}

	/** See {@link #mnWithholding}. */
	@Column(name = "MN_Withholding", nullable = false)
	public boolean isMnWithholding() {
		return mnWithholding;
	}
	/** See {@link #mnWithholding}. */
	public void setMnWithholding(boolean mnWithholding) {
		this.mnWithholding = mnWithholding;
	}

	/** See {@link #mnWithholdingSectionA}. */
	@Column(name = "MN_Withholding_Section_A", nullable = false)
	public boolean isMnWithholdingSectionA() {
		return mnWithholdingSectionA;
    }
	/** See {@link #mnWithholdingSectionA}. */
	public void setMnWithholdingSectionA(boolean mnWithholdingSectionA) {
		this.mnWithholdingSectionA = mnWithholdingSectionA;
	}

	/** See {@link #mnWithholdingSectionB}. */
	@Column(name = "MN_Withholding_Section_B", nullable = false)
	public boolean isMnWithholdingSectionB() {
		return mnWithholdingSectionB;
	}
	/** See {@link #mnWithholdingSectionB}. */
	public void setMnWithholdingSectionB(boolean mnWithholdingSectionB) {
		this.mnWithholdingSectionB = mnWithholdingSectionB;
	}

	/** See {@link #mnWithholdingSectionC}. */
	@Column(name = "MN_Withholding_Section_C", nullable = false)
	public boolean isMnWithholdingSectionC() {
		return mnWithholdingSectionC;
    }
	/** See {@link #mnWithholdingSectionC}. */
	public void setMnWithholdingSectionC(boolean mnWithholdingSectionC) {
		this.mnWithholdingSectionC = mnWithholdingSectionC;
	}

	/** See {@link #mnWithholdingSectionD}. */
	@Column(name = "MN_Withholding_Section_D", nullable = false)
	public boolean isMnWithholdingSectionD() {
		return mnWithholdingSectionD;
	}
	/** See {@link #mnWithholdingSectionD}. */
	public void setMnWithholdingSectionD(boolean mnWithholdingSectionD) {
		this.mnWithholdingSectionD = mnWithholdingSectionD;
	}

	/** See {@link #mnWithholdingSectionE}. */
	@Column(name = "MN_Withholding_Section_E", nullable = false)
	public boolean isMnWithholdingSectionE() {
		return mnWithholdingSectionE;
    }
	/** See {@link #mnWithholdingSectionE}. */
	public void setMnWithholdingSectionE(boolean mnWithholdingSectionE) {
		this.mnWithholdingSectionE = mnWithholdingSectionE;
	}

	/** See {@link #checkBox3}. */
	@Column(name = "MN_Withholding_Section_F", nullable = false)
	public boolean isMnWithholdingSectionF() {
		return mnWithholdingSectionF;
	}
	/** See {@link #mnWithholdingSectionF}. */
	public void setMnWithholdingSectionF(boolean mnWithholdingSectionF) {
		this.mnWithholdingSectionF = mnWithholdingSectionF;
	}

	/** See {@link #mnAllowancesSectionTotal}. */
	@Column(name = "MN_Allowances_Section_Total", nullable = false)
	public Integer getMnAllowancesSectionTotal() {
		return mnAllowancesSectionTotal;
	}
	/** See {@link #mnAllowancesSectionTotal}. */
	public void setMnAllowancesSectionTotal(Integer mnAllowancesSectionTotal) {
		this.mnAllowancesSectionTotal = mnAllowancesSectionTotal;
	}

    /** See {@link #checkBox1}. */
	@Column(name = "Check_Box_1", nullable = false)
    public boolean isCheckBox1() {
        return checkBox1;
    }

    /** See {@link #checkBox1}. */
    public void setCheckBox1(boolean checkBox1) {
        this.checkBox1 = checkBox1;
    }

    /** See {@link #checkBox2}. */
	@Column(name = "Check_Box_2", nullable = false)
	public boolean isCheckBox2() {
        return checkBox2;
    }

    /** See {@link #checkBox2}. */
    public void setCheckBox2(boolean checkBox2) {
        this.checkBox2 = checkBox2;
    }

    /** See {@link #checkBox3}. */
	@Column(name = "Check_Box_3", nullable = false)
    public boolean isCheckBox3() {
        return checkBox3;
    }

    /** See {@link #checkBox3}. */
    public void setCheckBox3(boolean checkBox3) {
        this.checkBox3 = checkBox3;
    }

    /** See {@link #checkBox4}. */
    @Column(name = "Check_Box_4", nullable = false)
    public boolean getCheckBox4() {
        return checkBox4;
    }

    /** See {@link #checkBox4}. */
    public void setCheckBox4(boolean checkBox4) {
        this.checkBox4 = checkBox4;
    }

    /** See {@link #checkBox5}. */
    @Column(name = "Check_Box_5", nullable = false)
    public boolean getCheckBox5() {
        return checkBox5;
    }

    /** See {@link #checkBox5}. */
    public void setCheckBox5(boolean checkBox5) {
        this.checkBox5 = checkBox5;
    }

    /** See {@link #checkBox6}. */
	@Column(name = "Check_Box_6", nullable = false)
	public boolean getCheckBox6() {
		return checkBox6;
	}

	/** See {@link #checkBox6}. */
	public void setCheckBox6(boolean checkBox6) {
		this.checkBox6 = checkBox6;
	}

	/** See {@link #checkBox7}. */
	@Column(name = "Check_Box_7", nullable = false)
	public boolean isCheckBox7() {
		return checkBox7;
	}
	/** See {@link #checkBox7}. */
	public void setCheckBox7(boolean checkBox7) {
		this.checkBox7 = checkBox7;
	}

	/** See {@link #checkBox8}. */
	@Column(name = "Check_Box_8", nullable = false)
	public boolean isCheckBox8() {
		return checkBox8;
	}
	/** See {@link #checkBox8}. */
	public void setCheckBox8(boolean checkBox8) {
		this.checkBox8 = checkBox8;
	}

	/** See {@link #checkBox9}. */
	@Column(name = "Check_Box_9", nullable = false)
	public boolean isCheckBox9() {
		return checkBox9;
	}
	/** See {@link #checkBox9}. */
	public void setCheckBox9(boolean checkBox9) {
		this.checkBox9 = checkBox9;
	}

	/** See {@link #checkBox10}. */
	@Column(name = "Check_Box_10", nullable = false)
	public boolean isCheckBox10() {
		return checkBox10;
	}
	/** See {@link #checkBox10}. */
	public void setCheckBox10(boolean checkBox10) {
		this.checkBox10 = checkBox10;
	}

	/** See {@link #checkBox11}. */
	@Column(name = "Check_Box_11", nullable = false)
	public boolean isCheckBox11() {
		return checkBox11;
	}
	/** See {@link #checkBox11}. */
	public void setCheckBox11(boolean checkBox11) {
		this.checkBox11 = checkBox11;
	}

	/** See {@link #checkBox12}. */
	@Column(name = "Check_Box_12", nullable = false)
	public boolean isCheckBox12() {
		return checkBox12;
	}
	/** See {@link #checkBox12}. */
	public void setCheckBox12(boolean checkBox12) {
		this.checkBox12 = checkBox12;
	}

    /** See {@link #completeAddress}. */
    @Column(name = "Complete_Address")
    public String getCompleteAddress() {
        return completeAddress;
    }

    /** See {@link #completeAddress}. */
    public void setCompleteAddress(String completeAddress) {
        this.completeAddress = completeAddress;
    }

    /** See {@link #residentState}. */
    @Column(name = "Resident_State")
    public String getResidentState() {
        return residentState;
    }

    /** See {@link #residentState}. */
    public void setResidentState(String residentState) {
        this.residentState = residentState;
    }

    /** See {@link #domicileState}. */
    @Column(name = "Domicile_State")
    public String getDomicileState() {
    	return domicileState;
    }

    /** See {@link #domicileState}. */
    public void setDomicileState(String domicileState) {
        this.domicileState = domicileState;
    }

    /** See {@link #applicableYear}. */
    @Column(name = "Applicable_Year")
    public Integer getApplicableYear() {
        return applicableYear;
    }

    /** See {@link #applicableYear}. */
    public void setApplicableYear(Integer applicableYear) {
        this.applicableYear = applicableYear;
    }

	/** See {@link #maritalStatusCode1}. */
    @Column(name = "Marital_Status_Code_1")
	public String getMaritalStatusCode1() {
		return maritalStatusCode1;
	}

	/** See {@link #maritalStatusCode1}. */
	public void setMaritalStatusCode1(String maritalStatusCode1) {
		this.maritalStatusCode1 = maritalStatusCode1;
	}

	/** See {@link #maritalStatusCode2}. */
	@Column(name = "Marital_Status_Code_2")
	public String getMaritalStatusCode2() {
		return maritalStatusCode2;
	}

	/** See {@link #maritalStatusCode2}. */
	public void setMaritalStatusCode2(String maritalStatusCode2) {
		this.maritalStatusCode2 = maritalStatusCode2;
	}

	/** See {@link #mnWithholdingSectionTotal}. */
    @Column(name = "MN_Withholding_Section_Total")
 	public Integer getMnWithholdingSectionTotal() {
 		return mnWithholdingSectionTotal;
 	}

	/** See {@link #mnWithholdingSectionTotal}. */
	public void setMnWithholdingSectionTotal(Integer mnWithholdingSectionTotal) {
		this.mnWithholdingSectionTotal = mnWithholdingSectionTotal;
	}

	/** See {@link #domicileLine}. */
	@Column(name = "Domicile_Line", nullable = false)
	public String getDomicileLine() {
		return domicileLine;
	}

	/** See {@link #domicileLine}. */
	public void setDomicileLine(String domicileLine) {
		this.domicileLine = domicileLine;
	}

    /** See {@link #daytimePhoneNumber}. */
    @Column(name = "Daytime_PhoneNumber", nullable = false)
    public String getDaytimePhoneNumber() {
        return daytimePhoneNumber;
    }

    /** See {@link #daytimePhoneNumber}. */
    public void setDaytimePhoneNumber(String daytimePhoneNumber) {
        this.daytimePhoneNumber = daytimePhoneNumber;
    }

    /** See {@link #withholdingCode}. */
	@Column(name = "Withholding_Code", nullable = true, length = 1)
	public String getWithholdingCode() {
		return withholdingCode;
	}

	/** See {@link #withholdingCode}. */
	public void setWithholdingCode(String withholdingCode) {
		this.withholdingCode = withholdingCode;
	}

	/** See {@link #msrraExempt}. */
	@Column(name = "MSRRA_EXEMPT", nullable = false)
	public boolean getMsrraExempt() {
		return msrraExempt;
	}

	/** See {@link #msrraExempt}. */
	public void setMsrraExempt(boolean msrraExempt) {
		this.msrraExempt = msrraExempt;
	}

	/** See {@link #legalStateOfRes}. */
	@Column(name = "Legal_State", nullable = true)
	public String getLegalStateOfRes() {
		return legalStateOfRes;
	}

	/** See {@link #legalStateOfRes}. */
	public void setLegalStateOfRes(String legalStateOfRes) {
		this.legalStateOfRes = legalStateOfRes;
	}

	/** See {@link #lowIncomeTax}. */
	@Column(name = "Low_Income_Tax", nullable = false)
	public Boolean getLowIncomeTax() {
		return lowIncomeTax;
	}

	/** See {@link #lowIncomeTax}. */
	public void setLowIncomeTax(Boolean lowIncomeTax) {
		this.lowIncomeTax = lowIncomeTax;
	}

	 /** See {@link #federalForm}. */
    @Column(name = "Federal_Form", nullable = false)
	public boolean isFederalForm() {
		return federalForm;
	}
	/** See {@link #federalForm}. */
	public void setFederalForm(boolean federalForm) {
		this.federalForm = federalForm;
	}

	/** See {@link #taxLiability}. */
	@Column(name = "Tax_Liability", nullable = false)
	public boolean isTaxLiability() {
		return taxLiability;
	}
	/** See {@link #taxLiability}. */
	public void setTaxLiability(boolean taxLiability) {
		this.taxLiability = taxLiability;
	}

	/** See {@link #perodicRetirement}. */
	@Column(name = "Perodic_Retirement", nullable = false)
	public boolean isPerodicRetirement() {
		return perodicRetirement;
	}
	/** See {@link #perodicRetirement}. */
	public void setPerodicRetirement(boolean perodicRetirement) {
		this.perodicRetirement = perodicRetirement;
	}

	/** See {@link #militarySpouseResidency}. */
	@Column(name = "Military_Spouse_Residency", nullable = false)
	public boolean isMilitarySpouseResidency() {
		return militarySpouseResidency;
	}
	/** See {@link #militarySpouseResidency}. */
	public void setMilitarySpouseResidency(boolean militarySpouseResidency) {
		this.militarySpouseResidency = militarySpouseResidency;
	}

	/** See {@link #personalAllowances}. */
	@Column(name = "Personal_Allownaces", nullable = true)
	public Integer getPersonalAllowances() {
		return personalAllowances;
	}

	/** See {@link #personalAllowances}. */
	public void setPersonalAllowances(Integer personalAllowances) {
		this.personalAllowances = personalAllowances;
	}

	/** See {@link #dependentAllowances}. */
	@Column(name = "Dependent_Allownaces", nullable = true)
	public Integer getDependentAllowances() {
		return dependentAllowances;
	}

	/** See {@link #dependentAllowances}. */
	public void setDependentAllowances(Integer dependentAllowances) {
		this.dependentAllowances = dependentAllowances;
	}

	/** See {@link #deductionAllowances}. */
	@Column(name = "Deduction_Allownaces", nullable = true)
	public Integer getDeductionAllowances() {
		return deductionAllowances;
	}

	/** See {@link #deductionAllowances}. */
	public void setDeductionAllowances(Integer deductionAllowances) {
		this.deductionAllowances = deductionAllowances;
	}

	/** See {@link #adjustmentAllowances}. */
	@Column(name = "Adjustment_Allownaces", nullable = true)
	public Integer getAdjustmentAllowances() {
		return adjustmentAllowances;
	}

	/** See {@link #adjustmentAllowances}. */
	public void setAdjustmentAllowances(Integer adjustmentAllowances) {
		this.adjustmentAllowances = adjustmentAllowances;
	}

	/** See {@link #childDependentAllowances}. */
	@Column(name = "Child_Dependent_Allownaces", nullable = true)
	public Integer getChildDependentAllowances() {
		return childDependentAllowances;
	}

	/** See {@link #childDependentAllowances}. */
	public void setChildDependentAllowances(Integer childDependentAllowances) {
		this.childDependentAllowances = childDependentAllowances;
	}

	/** See {@link #effectiveYear}. */
	@Column(name = "Effective_Year", nullable = true)
	public Integer getEffectiveYear() {
		return effectiveYear;
	}

	/** See {@link #effectiveYear}. */
	public void setEffectiveYear(Integer effectiveYear) {
		this.effectiveYear = effectiveYear;
	}

	/** See {@link #taxIdNumber}. */

	/** See {@link #allowancesSectionA}. */
    @Column(name = "Allownaces_Section_A", nullable = true)
	public Integer getAllowancesSectionA() {
		return allowancesSectionA;
	}

	/** See {@link #allowancesSectionA}. */
	public void setAllowancesSectionA(Integer allowancesSectionA) {
		this.allowancesSectionA = allowancesSectionA;
	}

	/** See {@link #allowancesSectionB}. */
	@Column(name = "Allownaces_Section_B", nullable = true)
	public Integer getAllowancesSectionB() {
		return allowancesSectionB;
	}

	/** See {@link #allowancesSectionB}. */
	public void setAllowancesSectionB(Integer allowancesSectionB) {
		this.allowancesSectionB = allowancesSectionB;
	}

	/** See {@link #allowancesSectionTotal}. */
	@Column(name = "Allownaces_Section_Total", nullable = true)
	public Integer getAllowancesSectionTotal() {
		return allowancesSectionTotal;
	}

	/** See {@link #allowancesSectionTotal}. */
	public void setAllowancesSectionTotal(Integer allowancesSectionTotal) {
		this.allowancesSectionTotal = allowancesSectionTotal;
	}

	/** See {@link #radioButton1}. */
	@Column(name = "Radio_Button1", nullable = true)
	public Boolean isRadioButton1() {
		return radioButton1;
	}

	/** See {@link #radioButton1}. */
	public void setRadioButton1(Boolean radioButton1) {
		this.radioButton1 = radioButton1;
	}

	/** See {@link #radioButton2}. */
	@Column(name = "Radio_Button2", nullable = true)
	public Boolean isRadioButton2() {
		return radioButton2;
	}

	/** See {@link #radioButton2}. */
	public void setRadioButton2(Boolean radioButton2) {
		this.radioButton2 = radioButton2;
	}

	/** See {@link #allowancesSectionC}. */
	@Column(name = "Allowances_Section_C", nullable = true)
	public Integer getAllowancesSectionC() {
		return allowancesSectionC;
	}

	/** See {@link #allowancesSectionC}. */
	public void setAllowancesSectionC(Integer allowancesSectionC) {
		this.allowancesSectionC = allowancesSectionC;
	}

	/** See {@link #allowancesSectionD}. */
	@Column(name = "Allowances_Section_D", nullable = true)
	public Integer getAllowancesSectionD() {
		return allowancesSectionD;
	}

	/** See {@link #allowancesSectionD}. */
	public void setAllowancesSectionD(Integer allowancesSectionD) {
		this.allowancesSectionD = allowancesSectionD;
	}

	/** See {@link #allowancesSectionE}. */
	@Column(name = "Allowances_Section_E", nullable = true)
	public Integer getAllowancesSectionE() {
        allowancesSectionE = 0;
        if (getAllowancesSectionA() != null) {
            allowancesSectionE = allowancesSectionE + getAllowancesSectionA();
        }
        if (getAllowancesSectionB() != null) {
            allowancesSectionE = allowancesSectionE + getAllowancesSectionB();
        }
        if (getAllowancesSectionC() != null) {
            allowancesSectionE = allowancesSectionE + getAllowancesSectionC();
        }
        if (getAllowancesSectionD() != null) {
            allowancesSectionE = allowancesSectionE + getAllowancesSectionD();
        }
        return allowancesSectionE;
    }

	/** See {@link #allowancesSectionE}. */
	public void setAllowancesSectionE(Integer allowancesSectionE) {
		this.allowancesSectionE = allowancesSectionE;
	}

	/** See {@link #mailingAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Mailing_Addr_Id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	public Address getMailingAddress() {
		return mailingAddress;
	}

	/** See {@link #mailingAddress}. */
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	/** See {@link #spouseSocialSecurity}. */
	@Column(name = "Spouse_Social_Security", length = 1000)
	@ColumnTransformer( // LS-2568 encrypt using MySql AES
			  read="AES_DECRYPT(Spouse_Social_Security," + ENCRYPT_KEY + ")",
			  write="AES_ENCRYPT(?," + ENCRYPT_KEY + " )" )
	public String getSpouseSocialSecurity() {
		return spouseSocialSecurity;
	}

	/** See {@link #spouseSocialSecurity}. */
	public void setSpouseSocialSecurity(String spouseSocialSecurity) {
		this.spouseSocialSecurity = spouseSocialSecurity;
	}

	/** See {@link #optionalTaxComputation}. */
	@Column(name = "Optional_Tax_Computation", nullable = false)
	public boolean getOptionalTaxComputation() {
		return optionalTaxComputation;
	}

	/** See {@link #optionalTaxComputation}. */
	public void setOptionalTaxComputation(boolean optionalTaxComputation) {
		this.optionalTaxComputation = optionalTaxComputation;
	}

	/** See {@link #militarySpouseResidencyRelief}. */
	@Column(name = "Military_Spouse_Residency_Relief", nullable = false)
	public boolean getMilitarySpouseResidencyRelief() {
		return militarySpouseResidencyRelief;
	}

	/** See {@link #militarySpouseResidencyRelief}. */
	public void setMilitarySpouseResidencyRelief(boolean militarySpouseResidencyRelief) {
		this.militarySpouseResidencyRelief = militarySpouseResidencyRelief;
	}

	/** See {@link #homeMortgageInterestAllowance}. */
	@Column(name = "Home_Mortgage_Interest_Allowance")
	public Integer getHomeMortgageInterestAllowance() {
		return homeMortgageInterestAllowance;
	}

	/** See {@link #homeMortgageInterestAllowance}. */
	public void setHomeMortgageInterestAllowance(Integer homeMortgageInterestAllowance) {
		this.homeMortgageInterestAllowance = homeMortgageInterestAllowance;
	}

	/** See {@link #charitableContributationAllowance}. */
	@Column(name = "Charitable_Contributation_Allowance")
	public Integer getCharitableContributationAllowance() {
		return charitableContributationAllowance;
	}

	/** See {@link #charitableContributationAllowance}. */
	public void setCharitableContributationAllowance(Integer charitableContributationAllowance) {
		this.charitableContributationAllowance = charitableContributationAllowance;
	}

	/** See {@link #medicalExpensesAllowance}. */
	@Column(name = "Medical_Expenses_Allowance")
	public Integer getMedicalExpensesAllowance() {
		return medicalExpensesAllowance;
	}

	/** See {@link #medicalExpensesAllowance}. */
	public void setMedicalExpensesAllowance(Integer medicalExpensesAllowance) {
		this.medicalExpensesAllowance = medicalExpensesAllowance;
	}

	/** See {@link #studentLoanInterestAllowance}. */
	@Column(name = "Student_Loan_Interest_Allowance")
	public Integer getStudentLoanInterestAllowance() {
		return studentLoanInterestAllowance;
	}

	/** See {@link #studentLoanInterestAllowance}. */
	public void setStudentLoanInterestAllowance(Integer studentLoanInterestAllowance) {
		this.studentLoanInterestAllowance = studentLoanInterestAllowance;
	}

	/** See {@link #govPensionContributionsAllowance}. */
	@Column(name = "Gov_Pension_Contributions_Allowance")
	public Integer getGovPensionContributionsAllowance() {
		return govPensionContributionsAllowance;
	}

	/** See {@link #govPensionContributionsAllowance}. */
	public void setGovPensionContributionsAllowance(Integer govPensionContributionsAllowance) {
		this.govPensionContributionsAllowance = govPensionContributionsAllowance;
	}

	/** See {@link #individRetirementAcctAllowances}. */
	@Column(name = "Individ_Retirement_Acct_Allowances")
	public Integer getIndividRetirementAcctAllowances() {
		return individRetirementAcctAllowances;
	}

	/** See {@link #individRetirementAcctAllowances}. */
	public void setIndividRetirementAcctAllowances(Integer individRetirementAcctAllowances) {
		this.individRetirementAcctAllowances = individRetirementAcctAllowances;
	}

	/** See {@link #educationContributionAllowance}. */
	@Column(name = "Education_Contribution_Allowance")
	public Integer getEducationContributionAllowance() {
		return educationContributionAllowance;
	}

	/** See {@link #educationContributionAllowance}. */
	public void setEducationContributionAllowance(Integer educationContributionAllowance) {
		this.educationContributionAllowance = educationContributionAllowance;
	}

	/** See {@link #healthSavingAcctAllowance}. */
	@Column(name = "Health_Saving_Acct_Allowance")
	public Integer getHealthSavingAcctAllowance() {
		return healthSavingAcctAllowance;
	}

	/** See {@link #healthSavingAcctAllowance}. */
	public void setHealthSavingAcctAllowance(Integer healthSavingAcctAllowance) {
		this.healthSavingAcctAllowance = healthSavingAcctAllowance;
	}

	/** See {@link #casualtyResidenceLossAllowance}. */
	@Column(name = "Casualty_Residence_Loss_Allowance")
	public Integer getCasualtyResidenceLossAllowance() {
		return casualtyResidenceLossAllowance;
	}

	/** See {@link #casualtyResidenceLossAllowance}. */
	public void setCasualtyResidenceLossAllowance(Integer casualtyResidenceLossAllowance) {
		this.casualtyResidenceLossAllowance = casualtyResidenceLossAllowance;
	}

	/** See {@link #personalPropertyLossAllowance}. */
	@Column(name = "Personal_Property_Loss_Allowance")
	public Integer getPersonalPropertyLossAllowance() {
		return personalPropertyLossAllowance;
	}

	/** See {@link #personalPropertyLossAllowance}. */
	public void setPersonalPropertyLossAllowance(Integer personalPropertyLossAllowance) {
		this.personalPropertyLossAllowance = personalPropertyLossAllowance;
	}

	/** See {@link #totalAllowancesAmt}. */
	@Column(name = "Total_Allowances_Amt")
	public Integer getTotalAllowancesAmt() {
		return totalAllowancesAmt;
	}

	/** See {@link #totalAllowancesAmt}. */
	public void setTotalAllowancesAmt(Integer totalAllowancesAmt) {
		this.totalAllowancesAmt = totalAllowancesAmt;
	}

	/** See {@link #authEmployerPayPeriodWithholding}. */
	@Column(name = "Auth_Employer_Pay_Period_Withholding", nullable = false)
	public boolean getAuthEmployerPayPeriodWithholding() {
		return authEmployerPayPeriodWithholding;
	}

	/** See {@link #authEmployerPayPeriodWithholding}. */
	public void setAuthEmployerPayPeriodWithholding(boolean authEmployerPayPeriodWithholding) {
		this.authEmployerPayPeriodWithholding = authEmployerPayPeriodWithholding;
	}

	/** See {@link #employerWithholdPayPeriodAmt}. */
	@Column(name = "Employer_Withhold_Pay_Period_Amt")
	public Integer getEmployerWithholdPayPeriodAmt() {
		return employerWithholdPayPeriodAmt;
	}

	/** See {@link #employerWithholdPayPeriodAmt}. */
	public void setEmployerWithholdPayPeriodAmt(Integer employerWithholdPayPeriodAmt) {
		this.employerWithholdPayPeriodAmt = employerWithholdPayPeriodAmt;
	}

	/** See {@link #employerWithholdPayPeriodPercent}. */
	@Column(name = "Employer_Withhold_Pay_Period_Percent")
	public Integer getEmployerWithholdPayPeriodPercent() {
		return employerWithholdPayPeriodPercent;
	}

	/** See {@link #employerWithholdPayPeriodPercent}. */
	public void setEmployerWithholdPayPeriodPercent(Integer employerWithholdPayPeriodPercent) {
		this.employerWithholdPayPeriodPercent = employerWithholdPayPeriodPercent;
	}

	/** See {@link #spouseFullName}. */
	@Column(name = "Spouse_Full_Name")
	public String getSpouseFullName() {
		return spouseFullName;
	}

	/** See {@link #spouseFullName}. */
	public void setSpouseFullName(String spouseFullName) {
		this.spouseFullName = spouseFullName;
	}

	/** See {@link #employerNotConsiderExemption}. */
	@Column(name = "Employer_Not_Consider_Exemption", nullable = false)
	public boolean getEmployerNotConsiderExemption() {
		return employerNotConsiderExemption;
	}

	/** See {@link #employerNotConsiderExemption}. */
	public void setEmployerNotConsiderExemption(boolean employerNotConsiderExemption) {
		this.employerNotConsiderExemption = employerNotConsiderExemption;
	}

	/** See {@link #jointCustodyDependents}. */
	@Column(name = "Joint_Custody_Dependents")
	public Integer getJointCustodyDependents() {
		return jointCustodyDependents;
	}

	/** See {@link #jointCustodyDependents}. */
	public void setJointCustodyDependents(Integer jointCustodyDependents) {
		this.jointCustodyDependents = jointCustodyDependents;
	}

	/** See {@link #employeeParticipatesGovPension}. */
	@Column(name = "Employee_Participates_Gov_Pension", nullable = false)
	public boolean getEmployeeParticipatesGovPension() {
		return employeeParticipatesGovPension;
	}

	/** See {@link #employeeParticipatesGovPension}. */
	public void setEmployeeParticipatesGovPension(boolean employeeParticipatesGovPension) {
		this.employeeParticipatesGovPension = employeeParticipatesGovPension;
	}

	/** See {@link #completeMailingAddress}. */
	@Column(name = "Complete_Mailing_Address")
	public String getCompleteMailingAddress() {
		return completeMailingAddress;
	}

	/** See {@link #completeMailingAddress}. */
	public void setCompleteMailingAddress(String completeMailingAddress) {
		this.completeMailingAddress = completeMailingAddress;
	}

	/** See {@link #personalExemptionStatus}. */
	@Column(name = "Personal_Exemption_Status")
	public String getPersonalExemptionStatus() {
		return personalExemptionStatus;
	}

	/** See {@link #personalExemptionStatus}. */
	public void setPersonalExemptionStatus(String personalExemptionStatus) {
		this.personalExemptionStatus = personalExemptionStatus;
	}

	/** See {@link #vetExemptionStatus}. */
	@Column(name = "Vet_Exemption_Status")
	public String getVetExemptionStatus() {
		return vetExemptionStatus;
	}

	/** See {@link #vetExemptionStatus}. */
	public void setVetExemptionStatus(String vetExemptionStatus) {
		this.vetExemptionStatus = vetExemptionStatus;
	}

	/** See {@link #mtAllowancesTotal} */
	@Transient
	public Integer getMtAllowancesTotal() {
		mtAllowancesTotal = NumberUtils.safeAdd(getAllowancesSectionA(), getAllowancesSectionB(), getAllowancesSectionC(),
				getQualifiedDependents(), getDeductionAmount(),getAllowances());
		return mtAllowancesTotal;
	}
	/** See {@link #mtAllowancesTotal}. */
	@Transient
	public void setMtAllowancesTotal(Integer mtAllowancesTotal) {
		this.mtAllowancesTotal = mtAllowancesTotal;
	}

	/** See {@link #illinois}. */
	@Column(name = "Illinois", nullable = true)
	public boolean isIllinois() {
		return illinois;
	}
	/** See {@link #illinois}. */
	public void setIllinois(boolean illinois) {
		this.illinois = illinois;
	}

	/** See {@link #indiana}. */
    @Column(name = "Indiana", nullable = true)
	public boolean isIndiana() {
		return indiana;
	}
	/** See {@link #indiana}. */
	public void setIndiana(boolean indiana) {
		this.indiana = indiana;
	}

	/** See {@link #michingan}. */
    @Column(name = "Michingan", nullable = true)
	public boolean isMichingan() {
		return michingan;
	}
	/** See {@link #michingan}. */
	public void setMichingan(boolean michingan) {
		this.michingan = michingan;
	}

	/** See {@link #westVirgina}. */
    @Column(name = "West_Virgina", nullable = true)
	public boolean isWestVirgina() {
		return westVirgina;
	}
	/** See {@link #westVirgina}. */
	public void setWestVirgina(boolean westVirgina) {
		this.westVirgina = westVirgina;
	}

	/** See {@link #wisconsin}. */
    @Column(name = "Wisconsin", nullable = true)
	public boolean isWisconsin() {
		return wisconsin;
	}
	/** See {@link #wisconsin}. */
	public void setWisconsin(boolean wisconsin) {
		this.wisconsin = wisconsin;
	}

	/** See {@link #virginia}. */
    @Column(name = "Virginia", nullable = true)
	public boolean isVirginia() {
		return virginia;
	}
	/** See {@link #virginia}. */
	public void setVirginia(boolean virginia) {
		this.virginia = virginia;
	}

	/** See {@link #ohio}. */
    @Column(name = "Ohio", nullable = true)
	public boolean isOhio() {
		return ohio;
	}
	/** See {@link #ohio}. */
	public void setOhio(boolean ohio) {
		this.ohio = ohio;
	}

	/** See {@link #nonResidentAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Non_Resident_Addr_Id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	public Address getNonResidentAddress() {
		return nonResidentAddress;
	}

	/** See {@link #nonResidentFullName}. */
	@Column(name="Non_Resident_Full_Name")
	public String getNonResidentFullName() {
		return nonResidentFullName;
	}

	/** See {@link #nonResidentFullName}. */
	public void setNonResidentFullName(String nonResidentFullName) {
		this.nonResidentFullName = nonResidentFullName;
	}

	/** See {@link #nonResidentSocialSecurity}. */
	@Column(name = "Non_Resident_Social_Security", length = 1000)
	@ColumnTransformer( // LS-2568 encrypt using MySql AES
			  read="AES_DECRYPT(Non_Resident_Social_Security," + ENCRYPT_KEY + ")",
			  write="AES_ENCRYPT(?," + ENCRYPT_KEY + " )" )
	public String getNonResidentSocialSecurity() {
		return nonResidentSocialSecurity;
	}

	/** See {@link #nonResidentSocialSecurity}. */
	public void setNonResidentSocialSecurity(String nonResidentSocialSecurity) {
		this.nonResidentSocialSecurity = nonResidentSocialSecurity;
	}
	/** See {@link #nonResidentAddress}. */
	public void setNonResidentAddress(Address nonResidentAddress) {
		this.nonResidentAddress = nonResidentAddress;
	}

	/** See {@link #certifiedLegalStateOfRes}. */
	@Column(name = "Certified_Legal_State_Or_Res", length = 2)
	public String getCertifiedLegalStateOfRes() {
		return certifiedLegalStateOfRes;
	}

	/** See {@link #certifiedLegalStateOfRes}. */
	public void setCertifiedLegalStateOfRes(String certifiedLegalStateOfRes) {
		this.certifiedLegalStateOfRes = certifiedLegalStateOfRes;
	}

	/** See {@link #marriedExemptions}. */
	@Column(name = "Married_Exemptions")
	public Integer getMarriedExemptions() {
		return marriedExemptions;
	}

	/** See {@link #marriedExemptions}. */
	public void setMarriedExemptions(Integer marriedExemptions) {
		this.marriedExemptions = marriedExemptions;
	}

	/** See {@link #showSecondSignature}. */
	@Column(name = "Show_Second_Signature", nullable = false)
	public boolean getShowSecondSignature() {
		return showSecondSignature;
	}

	/** See {@link #showSecondSignature}. */
	public void setShowSecondSignature(boolean showSecondSignature) {
		this.showSecondSignature = showSecondSignature;
	}

	/** See {@link #additionalRadio. */
	@Column(name = "Additional_Radio")
	public boolean getAdditionalRadio() {
		return additionalRadio;
	}

	/** See {@link #additionalRadio. */
	public void setAdditionalRadio(boolean additionalRadio) {
		this.additionalRadio = additionalRadio;
	}

}
