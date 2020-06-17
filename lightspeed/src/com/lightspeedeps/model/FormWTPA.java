package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/** This entity contains all the fields of the CA/NY WTPA form.
 * It is used to maintain the user wise record of the CA/NY WTPA entries.
 *
 */
@Entity
@Table(name = "form_wtpa")
public class FormWTPA extends Form<FormWTPA> {

	private static final long serialVersionUID = -7224590955587812853L;

	/** Employee Information Start*/
	private String name;

	private Date startDate;

	private String email;

	private String phone;

	private String occupation;

	private String projectName;

	private String projectNumber;

	/** Employer Information Start*/
	private String employerName;

	private String dba;

	private String fedidNumber;

	private Address address;

	private Address mailingAddress;

	private String employerPhone;

	/** Employee Pay Rates*/

	/** CA WTPA's Regular rate. */
	private BigDecimal regularRate;

	/** CA WTPA's Regular rate of pay. */
	private PayFrequency regularFreq;

	private BigDecimal dailyHours;

	private String regularFreqOther;

	/** NY WTPA's Overtime method. */
	private PayFrequency overtimeMethod;

	/** CA WTPA's Overtime rate. */
	private BigDecimal overtimeRate;

	/** CA WTPA's Regular rate of pay. */
	private PayFrequency overtimeFreq;

	private String overtimeFreqOther;

	private BigDecimal overtimeTier1;

	private BigDecimal overtimeTier1Amt;

	private boolean overtime;

	private BigDecimal overtimeTier2;

	private BigDecimal overtimeTier2Amt;

	//private boolean underCba;

	private String cbaAgreement;

	private ExemptPayType exemptPayType;

	private BigDecimal exemptPayAmt;

	private boolean isExempt;

	private String exemptReason;

	private String exemptInfoAt;

	private boolean allowNone;

	private boolean allowTips;

	private BigDecimal allowTipsAmt;

	private boolean allowMeal;

	private BigDecimal allowMealAmt;

	private boolean allowLodging;

	private BigDecimal allowLodgingAmt;

	private boolean allowOther;

	private BigDecimal allowOtherAmt;

	private Byte regularPayDay;


	private PaydayFrequency paydayFreq;

	private String paydayFreqOther;

	private boolean noticeGivenAtHire;

	/** Paid Sick Leave Start*/
	private CalifSickLeaveType sickLeaveType;

	private String sickLeaveReason;

	/** True = employee's primary language is English (defaults to true);
	 * if false, {@link #payNoticePrimaryLang} should be non-blank. */
	private boolean payNoticeEnglish = true;

	/** The employee's primary language if not English. */
	private String payNoticePrimaryLang;

	//private ContactDocEvent signature;

	private boolean employeeDeclinesSign;

	//private ContactDocEvent employeeRepSignature;

	private String employeeRepPhone;

	private String employeeRepName;

	private String employeeRepTitle;

	private String employeeRepEmail;

	private static final Log log = LogFactory.getLog(FormWTPA.class);

	@Column(name = "name", nullable = true, length = 60)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Start_Date", length = 0)
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Column(name = "Email", nullable = true, length = 100)
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	@Column(name = "Phone", nullable = true, length = 30)
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column(name = "Occupation", nullable = true, length = 100)
	public String getOccupation() {
		return occupation;
	}
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	@Column(name = "Project_Name", nullable = true, length = 35)
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Column(name = "Project_Number", nullable = true, length = 20)
	public String getProjectNumber() {
		return projectNumber;
	}
	public void setProjectNumber(String projectNumber) {
		this.projectNumber = projectNumber;
	}

	@Column(name = "Employer_Name", nullable = true, length = 100)
	public String getEmployerName() {
		return employerName;
	}
	public void setEmployerName(String employerName) {
		this.employerName = employerName;
	}

	@Column(name = "Dba", nullable = true, length = 100)
	public String getDba() {
		return dba;
	}
	public void setDba(String dba) {
		this.dba = dba;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Fedid_Number", nullable = true, length = 1000)
	public String getFedidNumber() {
		return fedidNumber;
	}
	public void setFedidNumber(String fedidNumber) {
		this.fedidNumber = fedidNumber;
	}

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id", nullable = true)
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Mailing_Address_Id", nullable = true)
	public Address getMailingAddress() {
		return mailingAddress;
	}
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}

	@Column(name = "Employer_Phone", nullable = true, length = 30)
	public String getEmployerPhone() {
		return employerPhone;
	}
	public void setEmployerPhone(String employerPhone) {
		this.employerPhone = employerPhone;
	}

	@Column(name = "Regular_Rate", nullable = true)
	public BigDecimal getRegularRate() {
		return regularRate;
	}
	public void setRegularRate(BigDecimal regularRate) {
		this.regularRate = regularRate;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Regular_Freq" , nullable = true, length = 10)
	public PayFrequency getRegularFreq() {
		return regularFreq;
	}
	public void setRegularFreq(PayFrequency regularFreq) {
		this.regularFreq = regularFreq;
	}

	@Column(name = "Daily_Hours", nullable = true)
	public BigDecimal getDailyHours() {
		return dailyHours;
	}
	public void setDailyHours(BigDecimal dailyHours) {
		this.dailyHours = dailyHours;
	}

	@Column(name = "Regular_Freq_Other", nullable = true, length = 100)
	public String getRegularFreqOther() {
		return regularFreqOther;
	}
	public void setRegularFreqOther(String regularFreqOther) {
		this.regularFreqOther = regularFreqOther;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Overtime_Method" , nullable = true, length = 10)
	public PayFrequency getOvertimeMethod() {
		return overtimeMethod;
	}
	public void setOvertimeMethod(PayFrequency overtimeMethod) {
		this.overtimeMethod = overtimeMethod;
	}

	@Column(name = "Overtime_Rate", nullable = true)
	public BigDecimal getOvertimeRate() {
		return overtimeRate;
	}
	public void setOvertimeRate	(BigDecimal overtimeRate) {
		this.overtimeRate = overtimeRate;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Overtime_Freq" , nullable = true, length = 10)
	public PayFrequency getOvertimeFreq() {
		return overtimeFreq;
	}
	public void setOvertimeFreq(PayFrequency overtimeFreq) {
		this.overtimeFreq = overtimeFreq;
	}

	@Column(name = "Overtime_Freq_Other" , nullable = true, length = 100)
	public String getOvertimeFreqOther() {
		return overtimeFreqOther;
	}
	public void setOvertimeFreqOther(String overtimeFreqOther) {
		this.overtimeFreqOther = overtimeFreqOther;
	}

	@Column(name = "Overtime_Tier1" , nullable = true)
	public BigDecimal getOvertimeTier1() {
		return overtimeTier1;
	}
	public void setOvertimeTier1(BigDecimal overtimeTier1) {
		this.overtimeTier1 = overtimeTier1;
	}

	@Column(name = "Overtime_Tier1_Amt" , nullable = true)
	public BigDecimal getOvertimeTier1Amt() {
		return overtimeTier1Amt;
	}
	public void setOvertimeTier1Amt(BigDecimal overtimeTier1Amt) {
		this.overtimeTier1Amt = overtimeTier1Amt;
	}

	@Column(name = "Overtime" , nullable = false)
	public boolean getOvertime() {
		return overtime;
	}
	public void setOvertime(boolean overtime) {
		this.overtime = overtime;
	}

	@Column(name = "Overtime_Tier2" , nullable = true)
	public BigDecimal getOvertimeTier2() {
		return overtimeTier2;
	}
	public void setOvertimeTier2(BigDecimal overtimeTier2) {
		this.overtimeTier2 = overtimeTier2;
	}

	@Column(name = "Overtime_Tier2_Amt" , nullable = true)
	public BigDecimal getOvertimeTier2Amt() {
		return overtimeTier2Amt;
	}
	public void setOvertimeTier2Amt(BigDecimal overtimeTier2Amt) {
		this.overtimeTier2Amt = overtimeTier2Amt;
	}

	/*@Column(name = "under_Cba" , nullable = false)
	public boolean getUnderCba() {
		return underCba;
	}
	public void setUnderCba(boolean underCba) {
		this.underCba = underCba;
	}*/

	@Column(name = "Cba_Agreement" , nullable = true, length = 100)
	public String getCbaAgreement() {
		return cbaAgreement;
	}
	public void setCbaAgreement(String cbaAgreement) {
		this.cbaAgreement = cbaAgreement;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Exempt_Pay_Type" , nullable = true, length = 10)
	public ExemptPayType getExemptPayType() {
		return exemptPayType;
	}
	public void setExemptPayType(ExemptPayType exemptPayType) {
		this.exemptPayType = exemptPayType;
	}

	@Column(name = "Exempt_Pay_Amt" , nullable = true)
	public BigDecimal getExemptPayAmt() {
		return exemptPayAmt;
	}
	public void setExemptPayAmt(BigDecimal exemptPayAmt) {
		this.exemptPayAmt = exemptPayAmt;
	}

	@Column(name = "Exempt" , nullable = false)
	public boolean getExempt() {
		return isExempt;
	}
	public void setExempt(boolean isExempt) {
		this.isExempt = isExempt;
	}

	@Column(name = "Exempt_Reason" , nullable = true, length = 100)
	public String getExemptReason() {
		return exemptReason;
	}
	public void setExemptReason(String exemptReason) {
		this.exemptReason = exemptReason;
	}

	@Column(name = "Exempt_Info_At" , nullable = true, length = 100)
	public String getExemptInfoAt() {
		return exemptInfoAt;
	}
	public void setExemptInfoAt(String exemptInfoAt) {
		this.exemptInfoAt = exemptInfoAt;
	}

	@Column(name = "Allow_None" , nullable = true)
	public boolean getAllowNone() {
		return allowNone;
	}
	public void setAllowNone(boolean allowNone) {
		this.allowNone = allowNone;
	}

	@Column(name = "Allow_Tips" , nullable = false)
	public boolean getAllowTips() {
		return allowTips;
	}
	public void setAllowTips(boolean allowTips) {
		this.allowTips = allowTips;
	}

	@Column(name = "Allow_Tips_Amt" , nullable = true)
	public BigDecimal getAllowTipsAmt() {
		return allowTipsAmt;
	}
	public void setAllowTipsAmt(BigDecimal allowTipsAmt) {
		this.allowTipsAmt = allowTipsAmt;
	}

	@Column(name = "Allow_Meal" , nullable = false)
	public boolean getAllowMeal() {
		return allowMeal;
	}
	public void setAllowMeal(boolean allowMeal) {
		this.allowMeal = allowMeal;
	}

	@Column(name = "Allow_Meal_Amt" , nullable = true)
	public BigDecimal getAllowMealAmt() {
		return allowMealAmt;
	}
	public void setAllowMealAmt(BigDecimal allowMealAmt) {
		this.allowMealAmt = allowMealAmt;
	}

	@Column(name = "Allow_Lodging" , nullable = false)
	public boolean getAllowLodging() {
		return allowLodging;
	}
	public void setAllowLodging(boolean allowLodging) {
		this.allowLodging = allowLodging;
	}

	@Column(name = "Allow_Lodging_Amt" , nullable = true)
	public BigDecimal getAllowLodgingAmt() {
		return allowLodgingAmt;
	}
	public void setAllowLodgingAmt(BigDecimal allowLodgingAmt) {
		this.allowLodgingAmt = allowLodgingAmt;
	}

	@Column(name = "Allow_Other" , nullable = false)
	public boolean getAllowOther() {
		return allowOther;
	}
	public void setAllowOther(boolean allowOther) {
		this.allowOther = allowOther;
	}

	@Column(name = "Allow_Other_Amt" , nullable = true)
	public BigDecimal getAllowOtherAmt() {
		return allowOtherAmt;
	}
	public void setAllowOtherAmt(BigDecimal allowOtherAmt) {
		this.allowOtherAmt = allowOtherAmt;
	}

	@Column(name = "Regular_Pay_Day" , nullable = true)
	public Byte getRegularPayDay() {
		return regularPayDay;
	}
	public void setRegularPayDay(Byte regularPayDay) {
		this.regularPayDay = regularPayDay;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Payday_Freq" , nullable = true, length = 10)
	public PaydayFrequency getPaydayFreq() {
		return paydayFreq;
	}
	public void setPaydayFreq(PaydayFrequency paydayFreq) {
		this.paydayFreq = paydayFreq;
	}

	@Column(name = "Payday_Freq_Other" , nullable = true, length = 30)
	public String getPaydayFreqOther() {
		return paydayFreqOther;
	}
	public void setPaydayFreqOther(String paydayFreqOther) {
		this.paydayFreqOther = paydayFreqOther;
	}

	@Column(name = "Notice_Given_At_Hire" , nullable = false)
	public boolean getNoticeGivenAtHire() {
		return noticeGivenAtHire;
	}
	public void setNoticeGivenAtHire(boolean noticeGivenAtHire) {
		this.noticeGivenAtHire = noticeGivenAtHire;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Sick_Leave_Type" , nullable = true, length = 10)
	public CalifSickLeaveType getSickLeaveType() {
		return sickLeaveType;
	}
	public void setSickLeaveType(CalifSickLeaveType sickLeaveType) {
		this.sickLeaveType = sickLeaveType;
	}

	@Column(name = "Sick_Leave_Reason" , nullable = true, length = 100)
	public String getSickLeaveReason() {
		return sickLeaveReason;
	}
	public void setSickLeaveReason(String sickLeaveReason) {
		this.sickLeaveReason = sickLeaveReason;
	}

	@Column(name = "Pay_Notice_English" , nullable = false)
	public boolean getPayNoticeEnglish() {
		return payNoticeEnglish;
	}
	public void setPayNoticeEnglish(boolean payNoticeEnglish) {
		this.payNoticeEnglish = payNoticeEnglish;
	}

	@Column(name = "Pay_Notice_Primary_Lang" , nullable = true, length = 30)
	public String getPayNoticePrimaryLang() {
		return payNoticePrimaryLang;
	}
	public void setPayNoticePrimaryLang(String payNoticePrimaryLang) {
		this.payNoticePrimaryLang = payNoticePrimaryLang;
	}

	/*@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Signature_Id", nullable = true)
	public ContactDocEvent getSignature() {
		return signature;
	}
	public void setSignature(ContactDocEvent signature) {
		this.signature = signature;
	}*/

	@Column(name = "Employee_Declines_Sign" , nullable = false)
	public boolean getEmployeeDeclinesSign() {
		return employeeDeclinesSign;
	}
	public void setEmployeeDeclinesSign(boolean employeeDeclinesSign) {
		this.employeeDeclinesSign = employeeDeclinesSign;
	}

	/*@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Employee_Rep_Signature_Id", nullable = true)
	public ContactDocEvent getEmployeeRepSignature() {
		return employeeRepSignature;
	}
	public void setEmployeeRepSignature(ContactDocEvent employeeRepSignature) {
		this.employeeRepSignature = employeeRepSignature;
	}*/

	@Column(name = "Employee_Rep_Phone" , nullable = true, length = 30)
	public String getEmployeeRepPhone() {
		return employeeRepPhone;
	}
	public void setEmployeeRepPhone(String employeeRepPhone) {
		this.employeeRepPhone = employeeRepPhone;
	}

	@Column(name = "Employee_Rep_Name" , nullable = true, length = 60)
	public String getEmployeeRepName() {
		return employeeRepName;
	}
	public void setEmployeeRepName(String employeeRepName) {
		this.employeeRepName = employeeRepName;
	}

	@Column(name = "Employee_Rep_Title" , nullable = true, length = 100)
	public String getEmployeeRepTitle() {
		return employeeRepTitle;
	}
	public void setEmployeeRepTitle(String employeeRepTitle) {
		this.employeeRepTitle = employeeRepTitle;
	}

	@Column(name = "Employee_Rep_Email" , nullable = true, length = 100)
	public String getEmployeeRepEmail() {
		return employeeRepEmail;
	}
	public void setEmployeeRepEmail(String employeeRepEmail) {
		this.employeeRepEmail = employeeRepEmail;
	}

	/**
	 * Determine if this form has all required fields filled in.
	 *
	 * @param type The PayrollFormType of the document.
	 * @return True iff all the required fields have been filled in with some
	 *         non-blank data. This checks for the fields required to for Wtpa
	 *         form.
	 */
	@JsonIgnore
	@Transient
	public boolean getHasRequiredFields(PayrollFormType type) {
		boolean formType = type == PayrollFormType.CA_WTPA;
		log.debug("");
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				StringUtils.isEmpty(getName()) ||
				getStartDate() == null ||
				StringUtils.isEmpty(getOccupation()) ||
				StringUtils.isEmpty(getProjectName()) ||
				StringUtils.isEmpty(getEmployerName()) ||
				StringUtils.isEmpty(getAddress().getAddrLine1()) ||
				StringUtils.isEmpty(getAddress().getState()) ||
				StringUtils.isEmpty(getAddress().getCity()) ||
				StringUtils.isEmpty(getAddress().getZip()) ||
				StringUtils.isEmpty(getEmployerPhone()) ||
				getRegularPayDay() == null ||
				getRegularPayDay() == 0 ||
				getPaydayFreq() == null;
		if (! missingData) {
			// For CA WTPA Form
			if (formType) {
				missingData = missingData ||
						NumberUtils.isEmpty(getRegularRate()) ||
						getRegularFreq() == null ||
						NumberUtils.isEmpty(getOvertimeRate()) ||
						getOvertimeFreq() == null ||
						getSickLeaveType() == null;
				// To prevent NPE
				if (! missingData) {
					if (getSickLeaveType().equals(CalifSickLeaveType.EXEMPT)) {
						missingData = missingData || StringUtils.isEmpty(getSickLeaveReason());
					}
					if (getRegularFreq().equals(PayFrequency.O)) {
						missingData = missingData || StringUtils.isEmpty(getRegularFreqOther());
					}
					else if (getOvertimeFreq().equals(PayFrequency.O)) {
						missingData = missingData || StringUtils.isEmpty(getOvertimeFreqOther());
					}
				}
			}
			// For NY WTPA Form
			else {
				if(! getExempt()) { // Non-Exempt
					missingData = missingData || getOvertimeMethod() == null;
					if (! missingData) {
						if (getOvertimeMethod().equals(PayFrequency.W)) {
							missingData = missingData || NumberUtils.isEmpty(getOvertimeRate());
						}
						else if (getOvertimeMethod().equals(PayFrequency.D)) {
							missingData = missingData ||
								NumberUtils.isEmpty(getOvertimeTier1()) ||
								NumberUtils.isEmpty(getOvertimeTier1Amt());
						}
					}
					if(! missingData){
						missingData = missingData || NumberUtils.isEmpty(getRegularRate());
					}
				}
				else { //Exempt Case
					missingData = missingData || getExemptPayType() == null;
					if (! missingData) {
						missingData = missingData || NumberUtils.isEmpty(getExemptPayAmt());
					}
				}
			}
			// For Other field
			if (! missingData && getPaydayFreq().equals(PaydayFrequency.O)) {
				missingData = missingData || StringUtils.isEmpty(getPaydayFreqOther());
			}
		}
		return ! missingData;
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737
		PayrollFormType type = cd.getFormType();

//		String accruesSick = "Off"; // for original PDF, never used in production
		String sick = "Off";		// for v1 PDF
		if (getSickLeaveType() != null) {
			switch (getSickLeaveType()) {
				case CA:
//					accruesSick = "0";
					sick = "Choice1";
					break;
				case POLICY:
//					accruesSick = "1";
					sick = "Choice2";
					break;
				case PLUS_24:
//					accruesSick = "2";
					sick = "Choice3";
					break;
				case EXEMPT:
//					accruesSick = "3";
					sick = "Choice4";
					break;
			}
		}

		String regularPayDayWeekDay = "";
		String[] weekdays = new DateFormatSymbols().getWeekdays();
		if (getRegularPayDay() != null) {
			regularPayDayWeekDay = weekdays[getRegularPayDay()];
		}

		String payrollCompany = "", payrollAll = "";
		// LS-2043 Changed to use current or viewed production so that we would get the correct
		// Payroll Service to display the wtpa custom text fields. This was failing when
		// printing from the My Starts tab.
		Production prod = SessionUtils.getCurrentOrViewedProduction();
		if (prod != null) {
			PayrollService service = prod.getPayrollPref().getPayrollService();
			if (service != null) {
				if (service.getWtpaPayrollCompany() != null) {
					payrollCompany = service.getWtpaPayrollCompany();
				}
				payrollAll = payrollCompany + "\n";
				if (service.getWtpaWorkersComp() != null) {
					payrollAll += service.getWtpaWorkersComp();
				}
			}
		}

		fieldValues.put(FormFieldType.WTPA_NAME.name(), getName());
		fieldValues.put(FormFieldType.WTPA_START_DATE.name(), dateFormat(format, getStartDate()));
		fieldValues.put(FormFieldType.WTPA_EMAIL.name(), getEmail());
		fieldValues.put(FormFieldType.WTPA_PHONE.name(), StringUtils.formatPhoneNumber(getPhone()));
		fieldValues.put(FormFieldType.WTPA_OCCUPATION.name(), getOccupation());
		fieldValues.put(FormFieldType.WTPA_PROJECT_NAME.name(), getProjectName());
		fieldValues.put(FormFieldType.WTPA_PROJECT_NUMBER.name(), getProjectNumber());
		fieldValues.put(FormFieldType.WTPA_EMPLOYER_NAME.name(), getEmployerName());
		fieldValues.put(FormFieldType.WTPA_DBA.name(), getDba());
		fieldValues.put(FormFieldType.WTPA_FEDID_NUMBER.name(), getFedidNumber());

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.WTPA_ADDRESS_LINE1.name(), getAddress().getAddrLine1());
			fieldValues.put(FormFieldType.WTPA_ADDRESS_CITY_ONLY.name(), getAddress().getCity());
			fieldValues.put(FormFieldType.WTPA_ADDRESS_STATE.name(), getAddress().getState());
			fieldValues.put(FormFieldType.WTPA_ADDRESS_ZIP.name(), getAddress().getZip());
			fieldValues.put(FormFieldType.WTPA_ADDRESS_CITY_STATE_ZIP.name(), getAddress().getCityStateZip());
		}
		else {
			fieldValues.put(FormFieldType.WTPA_ADDRESS_LINE1.name(), "");
			fieldValues.put(FormFieldType.WTPA_ADDRESS_CITY_ONLY.name(),  "");
			fieldValues.put(FormFieldType.WTPA_ADDRESS_STATE.name(),  "");
			fieldValues.put(FormFieldType.WTPA_ADDRESS_ZIP.name(),  "");
			fieldValues.put(FormFieldType.WTPA_ADDRESS_CITY_STATE_ZIP.name(),  "");
		}

		if (getMailingAddress() != null) {
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_LINE1.name(), getMailingAddress().getAddrLine1());
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_CITY_ONLY.name(), getMailingAddress().getCity());
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_STATE.name(), getMailingAddress().getState());
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_ZIP.name(), getMailingAddress().getZip());
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_CITY_STATE_ZIP.name(), getMailingAddress().getCityStateZip());
		}
		else {
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_LINE1.name(), "");
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_CITY_ONLY.name(),  "");
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_STATE.name(),  "");
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_ZIP.name(),  "");
			fieldValues.put(FormFieldType.WTPA_MAILING_ADDRESS_CITY_STATE_ZIP.name(),  "");
		}
		fieldValues.put(FormFieldType.WTPA_EMPLOYER_PHONE.name(), StringUtils.formatPhoneNumber(getEmployerPhone()));

		if (type == PayrollFormType.NY_WTPA) {
			fieldValues.put(FormFieldType.WTPA_PAYROLL_TEXT.name(), payrollCompany);
			fieldValues.put(FormFieldType.WTPA_RATE_CHANGE.name(), (getNoticeGivenAtHire() ? "Yes" : "Off"));
			// Exempt
			if (getExempt()) {
				if (getExemptPayType() != null) {
					fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_TYPE_BTN.name(), getExemptPayType().toString());
				}
				else {
					fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_TYPE_BTN.name(), "Off");
				}
				fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_AMT.name(), bigDecimalFormat(getExemptPayAmt()));
				fieldValues.put(FormFieldType.WTPA_EXEMPT_REASON.name(), getExemptReason());
				fieldValues.put(FormFieldType.WTPA_HAS_CBA.name(), StringUtils.isEmpty(getCbaAgreement()) ? "Off" : "Yes");
				fieldValues.put(FormFieldType.WTPA_CBA_EX_AGREEMENT.name(), getCbaAgreement());
				fieldValues.put(FormFieldType.WTPA_EXEMPT_INFO_AT.name(), getExemptInfoAt());
				// clear non-exempt fields
				fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), null);
				fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), null);
				fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_HRS.name(), null);
				fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_AMT.name(), null);
				fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_HRS.name(), null);
				fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_AMT.name(), null);
				fieldValues.put(FormFieldType.WTPA_CBA_NONEX_AGREEMENT.name(), null);
			}
			// Non-Exempt
			else {
				// Clear exempt fields
				fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_TYPE_BTN.name(), "Off");
				fieldValues.put(FormFieldType.WTPA_EXEMPT_PAY_AMT.name(), null);
				fieldValues.put(FormFieldType.WTPA_EXEMPT_REASON.name(), null);
				fieldValues.put(FormFieldType.WTPA_HAS_CBA.name(), "Off");
				fieldValues.put(FormFieldType.WTPA_CBA_EX_AGREEMENT.name(), null);
				fieldValues.put(FormFieldType.WTPA_EXEMPT_INFO_AT.name(), null);

				// Fill non-exempt fields
				fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), bigDecimalFormat(getRegularRate()));
				if (getOvertimeMethod() == null || getOvertimeMethod().equals(PayFrequency.W)) {	// Weekly Overtime
					fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), bigDecimalFormat(getOvertimeRate()));
					// clear daily
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_HRS.name(), null);
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_AMT.name(), null);
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_HRS.name(), null);
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_AMT.name(), null);
				}
				else if (getOvertimeMethod().equals(PayFrequency.D)) {	// Daily Overtime
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_HRS.name(), bigDecimalFormat(getOvertimeTier1()));
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER1_AMT.name(), bigDecimalFormat(getOvertimeTier1Amt()));
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_HRS.name(), bigDecimalFormat(getOvertimeTier2()));
					fieldValues.put(FormFieldType.WTPA_OVERTIME_TIER2_AMT.name(), bigDecimalFormat(getOvertimeTier2Amt()));
					fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), null); // clear weekly
				}
				fieldValues.put(FormFieldType.WTPA_UNION_CONTRACT.name(), StringUtils.isEmpty(getCbaAgreement()) ? "Off" : "Yes");
				fieldValues.put(FormFieldType.WTPA_CBA_NONEX_AGREEMENT.name(), getCbaAgreement());
			}
		}
		else { // CA WTPA
			fieldValues.put(FormFieldType.WTPA_PAYROLL_TEXT.name(), payrollAll);
			fieldValues.put(FormFieldType.WTPA_REGULAR_RATE.name(), bigDecimalFormat(getRegularRate()));
			fieldValues.put(FormFieldType.WTPA_REGULAR_FREQ_HOUR.name(), getRegularFreq()==PayFrequency.H ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_REGULAR_FREQ_DAY.name(), getRegularFreq()==PayFrequency.D ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_REGULAR_FREQ_OTHER.name(), getRegularFreq()==PayFrequency.O ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_REGULAR_FREQ_EXPLAIN.name(), getRegularFreqOther());
			fieldValues.put(FormFieldType.WTPA_DAILY_HOURS.name(), bigDecimalFormat(getDailyHours()));
			fieldValues.put(FormFieldType.WTPA_OVERTIME_RATE.name(), bigDecimalFormat(getOvertimeRate()));
			fieldValues.put(FormFieldType.WTPA_OVERTIME_FREQ_HOUR.name(), getOvertimeFreq()==PayFrequency.H ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_OVERTIME_FREQ_OTHER.name(), getOvertimeFreq()==PayFrequency.O ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_OVERTIME_FREQ_EXPLAIN.name(), getOvertimeFreqOther());
			fieldValues.put(FormFieldType.WTPA_NOTICE_GIVEN_AT_HIRE.name(), getNoticeGivenAtHire() ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_NOTICE_GIVEN_WITHIN_7_DAYS.name(), getNoticeGivenAtHire() ? "Off" : "Yes");
		}
		fieldValues.put(FormFieldType.WTPA_OVERTIME_METHOD_WEEKLY.name(), getOvertimeMethod()==PayFrequency.W ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_OVERTIME_METHOD_DAILY.name(),  getOvertimeMethod()==PayFrequency.D ? "Yes" : "Off");
//		fieldValues.put(FormFieldType.WTPA_OVERTIME.name(), getOvertime() ? "Yes" : "Off"); // Unused field
		fieldValues.put(FormFieldType.WTPA_IS_EXEMPT.name(), getExempt() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_NONE.name(), getAllowNone() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_TIPS_CHK.name(), getAllowTips() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_TIPS_AMT.name(), bigDecimalFormat(getAllowTipsAmt()));
		fieldValues.put(FormFieldType.WTPA_ALLOW_MEAL_CHK.name(), getAllowMeal() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_MEAL_AMT.name(), bigDecimalFormat(getAllowMealAmt()));
		fieldValues.put(FormFieldType.WTPA_ALLOW_LODGING_CHK.name(), getAllowLodging() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_LODGING_AMT.name(), bigDecimalFormat(getAllowLodgingAmt()));
		fieldValues.put(FormFieldType.WTPA_ALLOW_OTHER_CHK.name(), getAllowOther() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_ALLOW_OTHER_AMT.name(), bigDecimalFormat(getAllowOtherAmt()));
		fieldValues.put(FormFieldType.WTPA_REGULAR_PAY_DAY.name(), regularPayDayWeekDay);
		if (type == PayrollFormType.NY_WTPA) {
			if (getPaydayFreq() != null) {
				fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_BTN.name(), getPaydayFreq().toString());
			}
			else {
				fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_BTN.name(), "Off");
			}
		}
		else { // CA WTPA
			fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_WEEKLY.name(), getPaydayFreq()==PaydayFrequency.W ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_BIWEEKLY.name(), getPaydayFreq()==PaydayFrequency.B ? "Yes" : "Off");
			fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_OTHER.name(), getPaydayFreq()==PaydayFrequency.O ? "Yes" : "Off");
		}
		fieldValues.put(FormFieldType.WTPA_PAYDAY_FREQ_EXPLAIN.name(), getPaydayFreqOther());
//		fieldValues.put(FormFieldType.WTPA_SICK_LEAVE_TYPE_BTN.name(), accruesSick);// for pre-production PDFs
		fieldValues.put(FormFieldType.WTPA_SICK_LEAVE_TYPE.name(), sick);// for v1 PDF

		fieldValues.put(FormFieldType.WTPA_SICK_LEAVE_REASON.name(), getSickLeaveReason());
		fieldValues.put(FormFieldType.WTPA_PAY_NOTICE_ENGLISH.name(), getPayNoticeEnglish() ? "English" : "Not English");
		fieldValues.put(FormFieldType.WTPA_PAY_NOTICE_PRIMARY_LANG.name(), getPayNoticePrimaryLang());
		fieldValues.put(FormFieldType.WTPA_EMPLOYEE_DECLINES_SIGN.name(), getEmployeeDeclinesSign() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.WTPA_EMPLOYEE_REP_PHONE.name(), StringUtils.formatPhoneNumber(getEmployeeRepPhone()));
		fieldValues.put(FormFieldType.WTPA_EMPLOYEE_REP_NAME.name(), getEmployeeRepName());
		fieldValues.put(FormFieldType.WTPA_EMPLOYEE_REP_TITLE.name(), getEmployeeRepTitle());
		fieldValues.put(FormFieldType.WTPA_EMPLOYEE_REP_EMAIL.name(), getEmployeeRepEmail());

		if (cd.getEmpSignature() != null) {
//			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), getContactDoc().getEmpSignature().getDisplay());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(),
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified I9 PDF.
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), dateFormat(format, cd.getEmployerSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), "");
		}
	}

	/**
	 * Export the fields in this FormW4 using the supplied Exporter.
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append(getName());
		ex.append(getStartDate());
		ex.append(getEmail());
		ex.append(getPhone());
		ex.append(getOccupation());
		ex.append(getProjectName());
		ex.append(getProjectNumber());
		ex.append(getEmployerName());
		ex.append(getDba());
		ex.append(getFedidNumber());

		Address addr = getAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);

		addr = getMailingAddress();
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlatShort(ex);

		ex.append(getEmployerPhone());
		ex.append(getRegularRate());
		ex.append(getRegularFreq() == null ? "" : getRegularFreq().name());
		ex.append(getDailyHours());
		ex.append(getRegularFreqOther());
		ex.append(getOvertimeMethod() == null ? "" : getOvertimeMethod().name());
		ex.append(getOvertimeRate());
		ex.append(getOvertimeFreq() == null ? "" : getOvertimeFreq().name());
		ex.append(getOvertimeFreqOther());
		ex.append(getOvertimeTier1());
		ex.append(getOvertimeTier1Amt());
		ex.append(getOvertime());
		ex.append(getOvertimeTier2());
		ex.append(getOvertimeTier2Amt());
		ex.append(getCbaAgreement());
		ex.append(getExemptPayType() == null ? "" : getExemptPayType().name());
		ex.append(getExemptPayAmt());
		ex.append(getExempt());
		ex.append(getExemptReason());
		ex.append(getExemptInfoAt());
		ex.append(getAllowNone());
		ex.append(getAllowTips());
		ex.append(getAllowTipsAmt());
		ex.append(getAllowMeal());
		ex.append(getAllowMealAmt());
		ex.append(getAllowLodging());
		ex.append(getAllowLodgingAmt());
		ex.append(getAllowOther());
		ex.append(getAllowOtherAmt());
		ex.append(getRegularPayDay());
		ex.append(getPaydayFreq() == null ? "" : getPaydayFreq().name());
		ex.append(getPaydayFreqOther());
		ex.append(getNoticeGivenAtHire());
		ex.append(getSickLeaveType() == null ? "" : getSickLeaveType().name());
		ex.append(getSickLeaveReason());
		ex.append(getPayNoticeEnglish());
		ex.append(getPayNoticePrimaryLang());
		ex.append(getEmployeeDeclinesSign());
		ex.append(getEmployeeRepPhone());
		ex.append(getEmployeeRepName());
		ex.append(getEmployeeRepTitle());
		ex.append(getEmployeeRepEmail());
	}

	@Override
	public void trim() {
		name = trim(name);
		email = trim(email);
		phone = trim(phone);
		occupation = trim(occupation);
		projectName = trim(projectName);
		projectNumber = trim(projectNumber);
		employerName = trim(employerName);
		dba = trim(dba);
		fedidNumber = trim(fedidNumber);
		address.trimIsEmpty();
		mailingAddress.trimIsEmpty();
		employerPhone = trim(employerPhone);
		regularFreqOther = trim(regularFreqOther);
		overtimeFreqOther = trim(overtimeFreqOther);
		cbaAgreement = trim(cbaAgreement);
		exemptReason = trim(exemptReason);
		exemptInfoAt = trim(exemptInfoAt);
		paydayFreqOther = trim(paydayFreqOther);
		sickLeaveReason = trim(sickLeaveReason);
		payNoticePrimaryLang = trim(payNoticePrimaryLang);
		employeeRepPhone = trim(employeeRepPhone);
		employeeRepName = trim(employeeRepName);
		employeeRepTitle = trim(employeeRepTitle);
		employeeRepEmail = trim(employeeRepEmail);
	}

}
