package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.type.EmploymentBasisType;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * This entity is used to represent a custom PDF form. It is not persisted. The
 * main purpose is to hold some methods specific to custom forms.
 */
public class CustomForm extends Form<CustomForm> {

	private static final long serialVersionUID = 1;
	private static final Log log = LogFactory.getLog(CustomForm.class);

	public CustomForm() {
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat shortDateFormat = new SimpleDateFormat("M/d/yyyy");
		DateFormat monthFormat = new SimpleDateFormat("MMM");
		DateFormat dayFormat = new SimpleDateFormat("d");
		DateFormat yearFormat = new SimpleDateFormat("yyyy");
		DateFormat shortYearFormat = new SimpleDateFormat("yy");
		DateFormat monthDayFormat = new SimpleDateFormat("M/d");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737
		Contact contact = cd.getContact();
		User user = contact.getUser();
		Date today = CalendarUtils.todaysDate();

		Production production = cd.getProduction();
		fieldValues.put(FormFieldType.PRODUCTION_COMPANY.name(), production.getStudio());
		fieldValues.put(FormFieldType.PRODUCTION_TITLE.name(), production.getTitle());
		fieldValues.put(FormFieldType.EMPLOYER_ADDRESS_COMPLETE.name(), production.getAddress().getCompleteAddress());

		// Benefit Year start and end dates for NJ Sick Leave
		fieldValues.put(FormFieldType.BENEFIT_YEAR_START.name(), dateFormat(monthDayFormat, production.getPayrollPref().getBenefitYearStart()));
		fieldValues.put(FormFieldType.BENEFIT_YEAR_END.name(), dateFormat(monthDayFormat, production.getPayrollPref().getBenefitYearEnd()));

		// WTPA_REGULAR_PAY_DAY
		String regularPayDayWeekDay = "";
		PayrollPreference payrollPref = production.getPayrollPref();
		payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
		if (payrollPref.getRegularPayDay() != null) {
			String[] weekdays = new DateFormatSymbols().getWeekdays();
			regularPayDayWeekDay = weekdays[payrollPref.getRegularPayDay()];
		}
		fieldValues.put(FormFieldType.WTPA_REGULAR_PAY_DAY.name(), regularPayDayWeekDay);

		// Employer title - we might find approver & get role/occupation?
		fieldValues.put(FormFieldType.EMP_TITLE.name(), "");

		fieldValues.put(FormFieldType.DATE_DAY.name(), dateFormat(dayFormat, today));
		fieldValues.put(FormFieldType.DATE_MONTH_SHORT.name(), dateFormat(monthFormat, today));
		fieldValues.put(FormFieldType.DATE_YEAR.name(), dateFormat(yearFormat, today));
		fieldValues.put(FormFieldType.DATE_SHORT_YEAR.name(), dateFormat(shortYearFormat, today)); // LS-1550
		fieldValues.put(FormFieldType.TODAYS_DATE.name(), dateFormat(shortDateFormat, today));
		fieldValues.put(FormFieldType.WTPA_START_DATE.name(), dateFormat(shortDateFormat, today));

		fieldValues.put(FormFieldType.USER_FIRST_NAME.name(), user.getFirstName());
		fieldValues.put(FormFieldType.USER_LAST_NAME.name(), user.getLastName());
		fieldValues.put(FormFieldType.USER_MIDDLE_NAME.name(), user.getMiddleName());
		fieldValues.put(FormFieldType.FULL_NAME.name(), user.getFullName());
		fieldValues.put(FormFieldType.USER_CELL_PHONE.name(), PhoneNumberValidator.formatted(user.getCellPhone()));
		fieldValues.put(FormFieldType.USER_HOME_PHONE.name(), PhoneNumberValidator.formatted(user.getHomePhone()));
		if (user.getPrimaryPhone() != null) {
			fieldValues.put(FormFieldType.TELEPHONE_NUMBER.name(), PhoneNumberValidator.formatted(user.getPrimaryPhone()));
		}
		else {
			fieldValues.put(FormFieldType.TELEPHONE_NUMBER.name(), "");
		}

		if (StringUtils.isEmpty(user.getLoanOutCorpName())) {
			fieldValues.put(FormFieldType.USER_OR_LOAN_OUT_NAME.name(), user.getDisplayName());
		}
		else {
			fieldValues.put(FormFieldType.USER_OR_LOAN_OUT_NAME.name(), user.getLoanOutCorpName());
		}

		fieldValues.put(FormFieldType.USER_DOB_MDY.name(), dateFormat(shortDateFormat, user.getBirthdate()));

		if (StringUtils.isEmpty(contact.getEmailAddress())) {
			fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), user.getEmailAddress());
		}
		else {
			fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), contact.getEmailAddress());
		}

		if (user.getHomeAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), user.getHomeAddress().getAddrLine1());
			fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), user.getHomeAddress().getAddrLine2());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), user.getHomeAddress().getCity());
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), user.getHomeAddress().getState());
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), user.getHomeAddress().getZip());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), user.getHomeAddress().getCityStateZip());
			fieldValues.put(FormFieldType.HOME_ADDR_COMPLETE.name(), user.getHomeAddress().getCompleteAddress());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_LINE2.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_COMPLETE.name(), "");
		}

		if (user.getMailingAddress() != null) {
			fieldValues.put(FormFieldType.MAILING_ADDR_STREET.name(), user.getMailingAddress().getAddrLine1());
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), user.getMailingAddress().getAddrLine2());
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), user.getMailingAddress().getCity());
			fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), user.getMailingAddress().getState());
			fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), user.getMailingAddress().getZip());
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_STATE_ZIP.name(), user.getMailingAddress().getCityStateZip());
			fieldValues.put(FormFieldType.MAILING_ADDR_COMPLETE.name(), user.getMailingAddress().getCompleteAddress());
		}
		else {
			fieldValues.put(FormFieldType.MAILING_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_LINE2.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_STATE.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_ZIP.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.MAILING_ADDR_COMPLETE.name(), "");
		}

		fieldValues.put(FormFieldType.EMERGENCY_CONTACT.name(), user.getEmergencyName());
		fieldValues.put(FormFieldType.EMERGENCY_PHONE.name(), PhoneNumberValidator.formatted(user.getEmergencyPhone()));
		fieldValues.put(FormFieldType.EMERGENCY_RELATION.name(), user.getEmergencyRelation());

		fieldValues.put(FormFieldType.AGENCY_NAME.name(), user.getAgencyName());
		fieldValues.put(FormFieldType.AGENCY_PHONE.name(), "");

		String maritalChoice = "";
		if (user.getW4Marital() != null) {
			switch(user.getW4Marital()) {
				case "s":
					maritalChoice = "Choice1";
					break;
				case "m":
					maritalChoice = "Choice2";
					break;
				case "w":
					maritalChoice = "Choice3";
					break;
			}
		}
		fieldValues.put(FormFieldType.W4_MARITAL_CHOICE.name(), maritalChoice); // works in Team-designed forms
		fieldValues.put(FormFieldType.W4_NAME_DIFFERS.name(), user.getW4NameDiffers() ? "On" : "Off"); // page 1, Line 4
		fieldValues.put(FormFieldType.W4_ALLOWANCES.name(), intFormat(user.getW4Allowances())); // page 1, Line 5
		fieldValues.put(FormFieldType.W4_ADDTL_AMOUNT.name(), intFormat(user.getW4AddtlAmount())); // page 1, Line 6
		fieldValues.put(FormFieldType.W4_EXEMPT.name(), user.getW4Exempt() ? "Exempt" : ""); // page 1, Line 7

		Employment employment = cd.getEmployment();
		StartForm startForm = null;
		if (employment != null) {
			log.debug("Employment:" + employment.getId());
			employment = EmploymentDAO.getInstance().refresh(employment);
			fieldValues.put(FormFieldType.WTPA_OCCUPATION.name(), employment.getOccupation());
			fieldValues.put(FormFieldType.DEPARTMENT.name(), employment.getDepartment().getName());
			startForm = employment.getStartForm();
		}
		else {
			fieldValues.put(FormFieldType.WTPA_OCCUPATION.name(), "");
			fieldValues.put(FormFieldType.DEPARTMENT.name(), "");
		}
		if (startForm != null) {
			fillStartFields(cd, fieldValues, startForm);
		}
		else {
			clearStartFields(fieldValues, user);
			if (! StringUtils.isEmpty(user.getLoanOutCorpName())) {
				fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), user.getLoanOutCorpName());
				fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), user.getFederalTaxIdFmtd());
				fieldValues.put(FormFieldType.LOAN_OUT_SIGNER_NAME.name(), user.getDisplayName());
				if (user.getLoanOutAddress() != null) {
					fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), user.getLoanOutAddress().getAddrLine1());
					fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), user.getLoanOutAddress().getCity());
					fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), user.getLoanOutAddress().getState());
					fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), user.getLoanOutAddress().getZip());
					fieldValues.put(FormFieldType.LOAN_OUT_CITY_STATE_ZIP.name(), user.getLoanOutAddress().getCityStateZip());
				}
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), user.getIncorporationState());
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), dateFormat(shortDateFormat, user.getIncorporationDate()));
				fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), user.getStateTaxId());
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), (user.getLoanOutQualifiedCa() ? "Yes" : "No"));
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), (user.getLoanOutQualifiedNy() ? "Yes" : "No"));
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), user.getLoanOutQualifiedStates());
				String allLoanStates = "";
				if (user.getLoanOutQualifiedStates() != null) {
					allLoanStates += user.getLoanOutQualifiedStates();
				}
				if (user.getLoanOutQualifiedCa()) {
					allLoanStates += " CA";
				}
				if (user.getLoanOutQualifiedNy()) {
					allLoanStates += " NY";
				}
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_ALL.name(), allLoanStates);
			}
			else { // User does NOT have a Loan-Out corporation defined...
				fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_SIGNER_NAME.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_SIGNER_NAME.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_CITY_STATE_ZIP.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), "No");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), "No");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), "");
				fieldValues.put(FormFieldType.LOAN_OUT_VALID_ALL.name(), "");
			}
		}

		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output needs 'multiline' text field
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(),
					dateFormat(shortDateFormat, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output needs 'multiline' text field
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(),
					dateFormat(shortDateFormat, cd.getEmployerSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), "");
		}
	}

	private static void fillStartFields(ContactDocument cd, Map<String,String> fieldValues, StartForm startForm) {
		DateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");
		Production production = cd.getProduction();
		/*
		 * A lot of this code, copied from FormWTPABean.populateForm(), as been left as comments, as we don't need
		 * all the information in any custom form yet, but may in the future.
		 */
		fieldValues.put(FormFieldType.WTPA_PHONE.name(), StringUtils.formatPhoneNumber(startForm.getPhone()));
		fieldValues.put(FormFieldType.WTPA_OCCUPATION.name(), startForm.getJobClass());
		fieldValues.put(FormFieldType.WTPA_START_DATE.name(), dateFormat(shortDateFormat, startForm.getWorkStartDate()));

		fieldValues.put(FormFieldType.UNION_LOCAL.name(), startForm.getUnionLocalNum());

		if (StringUtils.isEmpty(startForm.getSocialSecurity())) {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_3.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_4.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_5.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_6.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_7.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_8.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_9.name(), "");
		}
		else {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), startForm.getSocialSecurity().substring(0, 3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), startForm.getSocialSecurity().substring(3, 5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), startForm.getSocialSecurity().substring(5));
			fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), startForm.getSocialSecurityFmtd());
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_1.name(), startForm.getSocialSecurity().substring(0,1));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_2.name(), startForm.getSocialSecurity().substring(1,2));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_3.name(), startForm.getSocialSecurity().substring(2,3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_4.name(), startForm.getSocialSecurity().substring(3,4));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_5.name(), startForm.getSocialSecurity().substring(4,5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_6.name(), startForm.getSocialSecurity().substring(5,6));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_7.name(), startForm.getSocialSecurity().substring(6,7));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_8.name(), startForm.getSocialSecurity().substring(7,8));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_9.name(), startForm.getSocialSecurity().substring(8,9));
		}

		if (production.getType().hasPayrollByProject()) {
			fieldValues.put(FormFieldType.WTPA_PROJECT_NAME.name(), startForm.getJobName());
			fieldValues.put(FormFieldType.WTPA_PROJECT_NUMBER.name(), startForm.getJobNumber());
		}
		else {
			fieldValues.put(FormFieldType.WTPA_PROJECT_NAME.name(), startForm.getProdTitle());
			fieldValues.put(FormFieldType.WTPA_PROJECT_NUMBER.name(), "");
		}

		if (! StringUtils.isEmpty(startForm.getLoanOutCorpName())) {
			fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), startForm.getLoanOutCorpName());
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), startForm.getFederalTaxIdFmtd());
			fieldValues.put(FormFieldType.LOAN_OUT_SIGNER_NAME.name(), startForm.getFirstName() + " " + startForm.getLastName());
			if (startForm.getLoanOutAddress() != null) {
				fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), startForm.getLoanOutAddress().getAddrLine1());
				fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), startForm.getLoanOutAddress().getCity());
				fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), startForm.getLoanOutAddress().getState());
				fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), startForm.getLoanOutAddress().getZip());
				fieldValues.put(FormFieldType.LOAN_OUT_CITY_STATE_ZIP.name(), startForm.getLoanOutAddress().getCityStateZip());
			}

			fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), startForm.getIncorporationState());
			fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), dateFormat(shortDateFormat, startForm.getIncorporationDate()));
			fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), startForm.getStateTaxId());
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), (startForm.getLoanOutQualifiedCa() ? "Yes" : "No"));
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), (startForm.getLoanOutQualifiedNy() ? "Yes" : "No"));
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), startForm.getLoanOutQualifiedStates());
			String allLoanStates = "";
			if (startForm.getLoanOutQualifiedStates() != null) {
				allLoanStates += startForm.getLoanOutQualifiedStates();
			}
			if (startForm.getLoanOutQualifiedCa()) {
				allLoanStates += " CA";
			}
			if (startForm.getLoanOutQualifiedNy()) {
				allLoanStates += " NY";
			}
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_ALL.name(), allLoanStates);
		}
		else {
			fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_SIGNER_NAME.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_STREET.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_CITY_ONLY.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_STATE.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_ZIP.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_CITY_STATE_ZIP.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_INCORP_STATE.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_INCORP_DATE.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_TAXID_STATE.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_CA.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_IN_NY.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_OTHERS.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_VALID_ALL.name(), "");
		}

		// TV/Feature account codes
		fieldValues.put(FormFieldType.PS_ACCT_CODE_LOC.name(), startForm.getAccountLoc());
		fieldValues.put(FormFieldType.PS_ACCT_CODE_MAJOR.name(), startForm.getAccountMajor());
		fieldValues.put(FormFieldType.PS_ACCT_CODE_DETAIL.name(), startForm.getAccountDtl());
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SUB.name(), startForm.getAccountSub());
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SET.name(), startForm.getAccountSet());

		// Commercial account codes
		fieldValues.put(FormFieldType.PS_ACCT_CODE_PREP.name(), startForm.getAccountMajor());
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SHOOT.name(), startForm.getAccountDtl());

		fieldValues.put(FormFieldType.PS_DOB_MDY.name(), dateFormat(shortDateFormat, startForm.getDateOfBirth()));
		fieldValues.put(FormFieldType.PS_TYPE_GENDER.name(), startForm.getGender() == null ? "" : startForm.getGender().getLabel());
		fieldValues.put(FormFieldType.PS_MINOR.name(), (startForm.getMinor() ? "Yes" : "No"));
		fieldValues.put(FormFieldType.PS_HIRE_DATE_MDY.name(), dateFormat(shortDateFormat, startForm.getHireDate()));
		fieldValues.put(FormFieldType.PS_START_DATE_MDY.name(), dateFormat(shortDateFormat, startForm.getAnyStartDate()));

		fieldValues.put(FormFieldType.PS_WORK_CITY.name(), startForm.getWorkCity());
		fieldValues.put(FormFieldType.PS_WORK_STATE.name(), startForm.getWorkState());

		if (startForm.getEmploymentBasis() != null) {
			fieldValues.put(FormFieldType.PS_ACA_BASIS.name(), startForm.getEmploymentBasis().getLabel());
			fieldValues.put(FormFieldType.PS_ACA_FULL_TIME.name(), startForm.getEmploymentBasis() == EmploymentBasisType.FT ? "Yes" : "Off");
			fieldValues.put(FormFieldType.PS_ACA_VARIABLE_TIME.name(), startForm.getEmploymentBasis() == EmploymentBasisType.VAR ? "Yes" : "Off");
			fieldValues.put(FormFieldType.PS_ACA_PART_TIME.name(), startForm.getEmploymentBasis() == EmploymentBasisType.PT ? "Yes" : "Off");
			fieldValues.put(FormFieldType.PS_ACA_SEASONAL.name(), startForm.getEmploymentBasis() == EmploymentBasisType.SNL ? "Yes" : "Off");
		}
		else {
			fieldValues.put(FormFieldType.PS_ACA_BASIS.name(), "");
			fieldValues.put(FormFieldType.PS_ACA_FULL_TIME.name(), "Off");
			fieldValues.put(FormFieldType.PS_ACA_VARIABLE_TIME.name(), "Off");
			fieldValues.put(FormFieldType.PS_ACA_PART_TIME.name(), "Off");
			fieldValues.put(FormFieldType.PS_ACA_SEASONAL.name(), "Off");
		}

		fieldValues.put(FormFieldType.PS_OCC_CODE.name(), startForm.getOccupationCode());
		fieldValues.put(FormFieldType.PS_SCHEDULE_LETTER.name(), startForm.getScheduleCode());

		fieldValues.put(FormFieldType.PS_BOX_RENTAL_STUDIO.name(), bigDecimalFormat(startForm.getBoxRental().getStudio()));
		fieldValues.put(FormFieldType.PS_BOX_RENTAL_DISTANT.name(), bigDecimalFormat(startForm.getBoxRental().getLoc()));
		fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_STUDIO.name(), bigDecimalFormat(startForm.getCarAllow().getStudio()));
		fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_DISTANT.name(), bigDecimalFormat(startForm.getCarAllow().getLoc()));
		fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_STUDIO.name(), bigDecimalFormat(startForm.getMealAllow().getStudio()));
		fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_DISTANT.name(), bigDecimalFormat(startForm.getMealAllow().getLoc()));

		// For single "Exempt?" checkbox:
		fieldValues.put(FormFieldType.WTPA_IS_EXEMPT.name(), (startForm.getAllowWorked() ? "Yes" : "Off"));
		// For two "Exempt" checkboxes (Yes/No):
		fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_TYPE_BTN.name(), (startForm.getAllowWorked() ? "Yes" : "No"));

		StartRateSet srs = startForm.getProd();
		BigDecimal otRate = null;
		if (startForm.getAllowWorked()) {
			fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), "");
			fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), "");
		}
		else {
			fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), bigDecimalFormat(startForm.getRate()));
			if (srs.getOt1Rate() != null && srs.getOt1Rate().signum() != 0) {
				otRate = srs.getOt1Rate();
			}
			else {
				if (startForm.isStudioRate()) {
					if (srs.getX15StudioRate() != null && srs.getX15StudioRate().signum() != 0) {
						otRate = srs.getX15StudioRate();
					}
				}
				else {
					if (srs.getX15LocRate() != null && srs.getX15LocRate().signum() != 0) {
						otRate = srs.getX15LocRate();
					}
				}
			}
			fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), bigDecimalFormat(otRate));
		}
		BigDecimal exemptRate = null;
		if (startForm.getRateType() == EmployeeRateType.WEEKLY) {
			exemptRate = startForm.getRate();
			fieldValues.put(FormFieldType.WEEKLY_RATE.name(), bigDecimalFormat(exemptRate));
			fieldValues.put(FormFieldType.WEEKLY_OR_HOURLY_RATE.name(), bigDecimalFormat(exemptRate) + " (weekly)");
			fieldValues.put(FormFieldType.WEEKLY_DISTANT_RATE.name(), bigDecimalFormat(srs.getWeekly().getLoc()));
			fieldValues.put(FormFieldType.DAYS_PER_WEEK.name(), "5");
			fieldValues.put(FormFieldType.HOURS_PER_WEEKLY_DAY.name(), bigDecimalFormat(srs.getHourly().getStudioHrs())); // is this used?
			fieldValues.put(FormFieldType.HOURS_PER_WEEK_STUDIO.name(), bigDecimalFormat(srs.getWeekly().getStudioHrs())); // LS-2757
			fieldValues.put(FormFieldType.HOURS_PER_WEEK_DISTANT.name(), bigDecimalFormat(srs.getWeekly().getLocHrs()));   // LS-2757
		}
		else {
			fieldValues.put(FormFieldType.WEEKLY_RATE.name(), "");
			fieldValues.put(FormFieldType.WEEKLY_OR_HOURLY_RATE.name(), bigDecimalFormat(srs.getHourly().getStudio()) + " (hourly)");
			fieldValues.put(FormFieldType.WEEKLY_DISTANT_RATE.name(), "");
			fieldValues.put(FormFieldType.DAYS_PER_WEEK.name(), "");
			fieldValues.put(FormFieldType.HOURS_PER_WEEKLY_DAY.name(), "");
			fieldValues.put(FormFieldType.HOURS_PER_WEEK_STUDIO.name(), "");
			fieldValues.put(FormFieldType.HOURS_PER_WEEK_DISTANT.name(), "");
		}

		if (startForm.getRateType() == EmployeeRateType.DAILY) {
			exemptRate = startForm.getRate();
			fieldValues.put(FormFieldType.DAILY_RATE.name(), bigDecimalFormat(exemptRate));
			fieldValues.put(FormFieldType.DAILY_DISTANT_RATE.name(), bigDecimalFormat(srs.getDaily().getLoc()));
			fieldValues.put(FormFieldType.HOURS_PER_DAY.name(), bigDecimalFormat(srs.getDaily().getStudioHrs()));
		}
		else {
			fieldValues.put(FormFieldType.DAILY_RATE.name(), "");
			fieldValues.put(FormFieldType.DAILY_DISTANT_RATE.name(), "");
			fieldValues.put(FormFieldType.HOURS_PER_DAY.name(), "");
		}

		// Our Payroll Start allows Hourly rates to be entered for any employee type: Hourly, Daily, or Weekly,
		// so we will attempt to fill the hourly fields regardless of employee type:
		fieldValues.put(FormFieldType.WTPA_DAILY_HOURS.name(), bigDecimalFormat(srs.getHourly().getStudioHrs()));
		fieldValues.put(FormFieldType.PS_GUAR_STUDIO_HOURS.name(), bigDecimalFormat(srs.getHourly().getStudioHrs()));
		fieldValues.put(FormFieldType.PS_GUAR_DISTANT_HOURS.name(), bigDecimalFormat(srs.getHourly().getLocHrs()));
		fieldValues.put(FormFieldType.PS_HOURLY_STUDIO_RATE.name(), bigDecimalFormat(srs.getHourly().getStudio()));
		fieldValues.put(FormFieldType.PS_HOURLY_DISTANT_RATE.name(), bigDecimalFormat(srs.getHourly().getLoc()));

	}

	/**
	 * Clear out the (potential) field names related to Start Form information.
	 * In a few cases, we can use information from the User record instead.
	 *
	 * @param fieldValues The Map of field values being filled.
	 * @param user The User object for the owner of the document (not
	 *            necessarily the currently logged-in user).
	 */
	private void clearStartFields(Map<String, String> fieldValues, User user) {

		fieldValues.put(FormFieldType.WTPA_PHONE.name(), PhoneNumberValidator.formatted(user.getPrimaryPhone()));
		if (! StringUtils.isEmpty(user.getSocialSecurity())) {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), user.getSocialSecurity().substring(0, 3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), user.getSocialSecurity().substring(3, 5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), user.getSocialSecurity().substring(5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_1.name(), user.getSocialSecurity().substring(0,1));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_2.name(), user.getSocialSecurity().substring(1,2));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_3.name(), user.getSocialSecurity().substring(2,3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_4.name(), user.getSocialSecurity().substring(3,4));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_5.name(), user.getSocialSecurity().substring(4,5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_6.name(), user.getSocialSecurity().substring(5,6));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_7.name(), user.getSocialSecurity().substring(6,7));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_8.name(), user.getSocialSecurity().substring(7,8));
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_9.name(), user.getSocialSecurity().substring(8,9));
			}
		else {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_3.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_4.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_5.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_6.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_7.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_8.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_DIGIT_9.name(), "");
		}
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), user.getSocialSecurityFmtd());

		fieldValues.put(FormFieldType.UNION_LOCAL.name(), "");

		fieldValues.put(FormFieldType.WTPA_PROJECT_NAME.name(), "");
		fieldValues.put(FormFieldType.WTPA_PROJECT_NUMBER.name(), "");

		fieldValues.put(FormFieldType.PS_ACCT_CODE_LOC.name(), "");
		fieldValues.put(FormFieldType.PS_ACCT_CODE_MAJOR.name(), "");
		fieldValues.put(FormFieldType.PS_ACCT_CODE_DETAIL.name(), "");
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SUB.name(), "");
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SET.name(), "");

		fieldValues.put(FormFieldType.PS_ACCT_CODE_PREP.name(), "");
		fieldValues.put(FormFieldType.PS_ACCT_CODE_SHOOT.name(), "");

		fieldValues.put(FormFieldType.PS_DOB_MDY.name(), "");
		fieldValues.put(FormFieldType.PS_TYPE_GENDER.name(), "");
		fieldValues.put(FormFieldType.PS_MINOR.name(), "No");
		fieldValues.put(FormFieldType.PS_HIRE_DATE_MDY.name(), "");
		fieldValues.put(FormFieldType.PS_START_DATE_MDY.name(), "");

		fieldValues.put(FormFieldType.PS_WORK_CITY.name(), "");
		fieldValues.put(FormFieldType.PS_WORK_STATE.name(), "");

		fieldValues.put(FormFieldType.PS_ACA_BASIS.name(), "");
		fieldValues.put(FormFieldType.PS_ACA_FULL_TIME.name(), "");
		fieldValues.put(FormFieldType.PS_ACA_VARIABLE_TIME.name(), "");
		fieldValues.put(FormFieldType.PS_ACA_PART_TIME.name(), "");
		fieldValues.put(FormFieldType.PS_ACA_SEASONAL.name(), "");

		fieldValues.put(FormFieldType.PS_OCC_CODE.name(), "");
		fieldValues.put(FormFieldType.PS_SCHEDULE_LETTER.name(), "");

		fieldValues.put(FormFieldType.PS_BOX_RENTAL_STUDIO.name(), "");
		fieldValues.put(FormFieldType.PS_BOX_RENTAL_DISTANT.name(), "");
		fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_STUDIO.name(), "");
		fieldValues.put(FormFieldType.PS_CAR_ALLOWANCE_DISTANT.name(), "");
		fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_STUDIO.name(), "");
		fieldValues.put(FormFieldType.PS_MEAL_ALLOWANCE_DISTANT.name(), "");

		fieldValues.put(FormFieldType.WTPA_IS_EXEMPT.name(), "False");
		fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_TYPE_BTN.name(), "Off");
		fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), "");
		fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), "");

		fieldValues.put(FormFieldType.WEEKLY_RATE.name(), "");
		fieldValues.put(FormFieldType.WEEKLY_DISTANT_RATE.name(), "");
		fieldValues.put(FormFieldType.DAYS_PER_WEEK.name(), "");
		fieldValues.put(FormFieldType.HOURS_PER_WEEKLY_DAY.name(), "");

		fieldValues.put(FormFieldType.DAILY_RATE.name(), "");
		fieldValues.put(FormFieldType.DAILY_DISTANT_RATE.name(), "");
		fieldValues.put(FormFieldType.HOURS_PER_DAY.name(), "");
		fieldValues.put(FormFieldType.WTPA_DAILY_HOURS.name(), "");
		fieldValues.put(FormFieldType.PS_GUAR_STUDIO_HOURS.name(), "");
		fieldValues.put(FormFieldType.PS_GUAR_DISTANT_HOURS.name(), "");
		fieldValues.put(FormFieldType.PS_HOURLY_STUDIO_RATE.name(), "");
		fieldValues.put(FormFieldType.PS_HOURLY_DISTANT_RATE.name(), "");
	}

}
