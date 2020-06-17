package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.lightspeedeps.type.BankAccountType;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.common.StringUtils;

/**
 * This entity contains all the fields of a single Direct Deposit form.
 */
@Entity
@Table(name = "form_deposit")
public class FormDeposit extends Form<FormDeposit> {

	private static final long serialVersionUID = -4151473828496772050L;

	private boolean firstBank;

	private boolean secondBank;

	private boolean changeAccount;

	private boolean stopDeposit;

	private String employeeName;

	private String clientName;

	private String socialSecurity;

	private String employeePhone;

	private String emailAddress;

	private ContactDocEvent bankInitial1;

	private BankAccountType accountType1;

	private String accountName1;

	private String accountNumber1;

	private String aBARouting1;

	private String bankName1;

	private String bankPhone1;

	private String bankAddress1;

	private ContactDocEvent bankInitial2;

	private BankAccountType accountType2;

	private String accountName2;

	private String accountNumber2;

	private String aBARouting2;

	private String bankName2;

	private String bankPhone2;

	private String bankAddress2;

	/** Amount or percent to deduct from wages and deposit into account
	 * number 2. */
	private BigDecimal bankAmount2;

	/** If true, {@link #bankAmount2} is a percentage; if false, {@link #bankAmount2}
	 * is a dollar amount. */
	private boolean bankPercent2;

	private BankAccountType oldAccountType;

	private String oldAccountName;

	private String oldAccountNumber;

	private String oldABARouting;

	private String oldBankName;

	private ContactDocEvent signature1;

	private ContactDocEvent signature2;

	private String loanOutName;

	private String fein;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	/** Default constructor */
	public FormDeposit() {
	}

	@Column(name = "First_Bank" , nullable = false)
	public boolean getFirstBank() {
		return firstBank;
	}
	public void setFirstBank(boolean firstBank) {
		this.firstBank = firstBank;
	}

	@Column(name = "Second_Bank" , nullable = false)
	public boolean getSecondBank() {
		return secondBank;
	}
	public void setSecondBank(boolean secondBank) {
		this.secondBank = secondBank;
	}

	@Column(name = "Change_Account" , nullable = false)
	public boolean getChangeAccount() {
		return changeAccount;
	}
	public void setChangeAccount(boolean changeAccount) {
		this.changeAccount = changeAccount;
	}

	@Column(name = "Stop_Deposit" , nullable = false)
	public boolean getStopDeposit() {
		return stopDeposit;
	}
	public void setStopDeposit(boolean stopDeposit) {
		this.stopDeposit = stopDeposit;
	}

	@Column(name = "Employee_Name", nullable = true, length = 60)
	public String getEmployeeName() {
		return employeeName;
	}
	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	@Column(name = "Client_Name", nullable = true, length = 60)
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", nullable = true, length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	public void setSocialSecurity(String socialSecurity) {
		this.socialSecurity = socialSecurity;
		viewSSN = null; // force refresh
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

	@Column(name = "Employee_Phone", nullable = true, length = 25)
	public String getEmployeePhone() {
		return employeePhone;
	}
	public void setEmployeePhone(String employeePhone) {
		this.employeePhone = employeePhone;
	}

	@Column(name = "Email_Address", nullable = true, length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Bank_Initial1")
	public ContactDocEvent getBankInitial1() {
		return bankInitial1;
	}
	public void setBankInitial1(ContactDocEvent bankInitial1) {
		this.bankInitial1 = bankInitial1;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Account_Type1" , nullable = true, length = 20)
	public BankAccountType getAccountType1() {
		return accountType1;
	}
	public void setAccountType1(BankAccountType accountType1) {
		this.accountType1 = accountType1;
	}

	@Column(name = "Account_Name1", nullable = true, length = 50)
	public String getAccountName1() {
		return accountName1;
	}
	public void setAccountName1(String accountName1) {
		this.accountName1 = accountName1;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Account_Number1", nullable = true, length = 25)
	public String getAccountNumber1() {
		return accountNumber1;
	}
	public void setAccountNumber1(String accountNumber1) {
		this.accountNumber1 = accountNumber1;
	}

	@Column(name = "ABA_Routing1", nullable = true, length = 10)
	public String getaBARouting1() {
		return aBARouting1;
	}
	public void setaBARouting1(String aBARouting1) {
		this.aBARouting1 = aBARouting1;
	}

	@Column(name = "Bank_Name1", nullable = true, length = 50)
	public String getBankName1() {
		return bankName1;
	}
	public void setBankName1(String bankName1) {
		this.bankName1 = bankName1;
	}

	@Column(name = "Bank_Phone1", nullable = true, length = 25)
	public String getBankPhone1() {
		return bankPhone1;
	}
	public void setBankPhone1(String bankPhone1) {
		this.bankPhone1 = bankPhone1;
	}

	@Column(name = "Bank_Address1", nullable = true, length = 100)
	public String getBankAddress1() {
		return bankAddress1;
	}
	public void setBankAddress1(String bankAddress1) {
		this.bankAddress1 = bankAddress1;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Bank_Initial2")
	public ContactDocEvent getBankInitial2() {
		return bankInitial2;
	}
	public void setBankInitial2(ContactDocEvent bankInitial2) {
		this.bankInitial2 = bankInitial2;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Account_Type2" , nullable = true, length = 20)
	public BankAccountType getAccountType2() {
		return accountType2;
	}
	public void setAccountType2(BankAccountType accountType2) {
		this.accountType2 = accountType2;
	}

	@Column(name = "Account_Name2", nullable = true, length = 50)
	public String getAccountName2() {
		return accountName2;
	}
	public void setAccountName2(String accountName2) {
		this.accountName2 = accountName2;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Account_Number2", nullable = true, length = 25)
	public String getAccountNumber2() {
		return accountNumber2;
	}
	public void setAccountNumber2(String accountNumber2) {
		this.accountNumber2 = accountNumber2;
	}

	@Column(name = "ABA_Routing2", nullable = true, length = 10)
	public String getaBARouting2() {
		return aBARouting2;
	}
	public void setaBARouting2(String aBARouting2) {
		this.aBARouting2 = aBARouting2;
	}

	@Column(name = "Bank_Name2", nullable = true, length = 50)
	public String getBankName2() {
		return bankName2;
	}
	public void setBankName2(String bankName2) {
		this.bankName2 = bankName2;
	}

	@Column(name = "Bank_Phone2", nullable = true, length = 25)
	public String getBankPhone2() {
		return bankPhone2;
	}
	public void setBankPhone2(String bankPhone2) {
		this.bankPhone2 = bankPhone2;
	}

	@Column(name = "Bank_Address2", nullable = true, length = 100)
	public String getBankAddress2() {
		return bankAddress2;
	}
	public void setBankAddress2(String bankAddress2) {
		this.bankAddress2 = bankAddress2;
	}

	/** See {@link #bankAmount2}. */
	@Column(name = "Bank_Amount2", nullable = true, precision = 8, scale = 4)
	public BigDecimal getBankAmount2() {
		return bankAmount2;
	}
	/** See {@link #bankAmount2}. */
	public void setBankAmount2(BigDecimal bank2Amount) {
		this.bankAmount2 = bank2Amount;
	}

	/** See {@link #bankPercent2}. */
	@Column(name = "Bank_Percent2", nullable = false)
	public boolean getBankPercent2() {
		return bankPercent2;
	}
	/** See {@link #bankPercent2}. */
	public void setBankPercent2(boolean bankPercent2) {
		this.bankPercent2 = bankPercent2;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Old_Account_Type" , nullable = true, length = 20)
	public BankAccountType getOldAccountType() {
		return oldAccountType;
	}
	public void setOldAccountType(BankAccountType oldAccountType) {
		this.oldAccountType = oldAccountType;
	}

	@Column(name = "Old_Account_Name", nullable = true, length = 50)
	public String getOldAccountName() {
		return oldAccountName;
	}
	public void setOldAccountName(String oldAccountName) {
		this.oldAccountName = oldAccountName;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Old_Account_Number", nullable = true, length = 25)
	public String getOldAccountNumber() {
		return oldAccountNumber;
	}
	public void setOldAccountNumber(String oldAccountNumber) {
		this.oldAccountNumber = oldAccountNumber;
	}

	@Column(name = "Old_ABA_Routing", nullable = true, length = 10)
	public String getOldABARouting() {
		return oldABARouting;
	}
	public void setOldABARouting(String oldABARouting) {
		this.oldABARouting = oldABARouting;
	}

	@Column(name = "Old_Bank_Name", nullable = true, length = 50)
	public String getOldBankName() {
		return oldBankName;
	}
	public void setOldBankName(String oldBankName) {
		this.oldBankName = oldBankName;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Signature1")
	public ContactDocEvent getSignature1() {
		return signature1;
	}
	public void setSignature1(ContactDocEvent signature1) {
		this.signature1 = signature1;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Signature2")
	public ContactDocEvent getSignature2() {
		return signature2;
	}
	public void setSignature2(ContactDocEvent signature2) {
		this.signature2 = signature2;
	}

	/** See {@link #loanOutName}. */
	@Column(name = "Loan_Out_Name", length = 100)
	public String getLoanOutName() {
		return this.loanOutName;
	}

	/** See {@link #loanOutName}. */
	public void setLoanOutName(String loanOutName) {
		this.loanOutName = loanOutName;
	}

	/** See {@link #fein}. */
	@Type(type="encryptedString")
	@Column(name = "FEIN", nullable = true, length = 1000)
	public String getFein() {
		return fein;
	}
	/** See {@link #fein}. */
	public void setFein(String fein) {
		this.fein = fein;
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		cd = cd.refresh(); // eliminate DAO reference. LS-2737
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		fieldValues.put(FormFieldType.DD_1ST_BANK.name(), getFirstBank() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_2ND_BANK.name(), getSecondBank() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_CHANGE_ACCT.name(), getChangeAccount() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_STOP.name(), getStopDeposit() ? "Yes" : "Off");
		fieldValues.put(FormFieldType.EMPLOYEE_NAME.name(), getEmployeeName());
		fieldValues.put(FormFieldType.EMP_BUSINESS_NAME.name(), getClientName());
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());
		fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), getEmailAddress());
		fieldValues.put(FormFieldType.TELEPHONE_NUMBER.name(), StringUtils.formatPhoneNumber(getEmployeePhone()));
		if ((! getStopDeposit()) && (cd.getEmpSignature() != null)) {
			fieldValues.put(FormFieldType.DD_CONFIRM.name(), "Yes");
			fieldValues.put(FormFieldType.DD_SIGNATURE1_NAME.name(),
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.DD_SIGNATURE1_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.DD_CONFIRM.name(), "Off");
			fieldValues.put(FormFieldType.DD_SIGNATURE1_NAME.name(), "");
			fieldValues.put(FormFieldType.DD_SIGNATURE1_DATE.name(), "");
		}

		// Output signer's initials, if any
		if (getBankInitial1() != null) {
			fieldValues.put(FormFieldType.DD_BANK_INITIAL1.name(), getBankInitial1().getInitials());
		}
		else {
			fieldValues.put(FormFieldType.DD_BANK_INITIAL1.name(), "");
		}

		if (getBankInitial2() != null) {
			fieldValues.put(FormFieldType.DD_BANK_INITIAL2.name(), getBankInitial2().getInitials());
		}
		else {
			fieldValues.put(FormFieldType.DD_BANK_INITIAL2.name(), "");
		}

		// Form field types for account types
		fieldValues.put(FormFieldType.DD_ACCT_TYPE1_CHCK.name(), getAccountType1()== BankAccountType.CHK ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_ACCT_TYPE1_SAV.name(), getAccountType1()== BankAccountType.SAV ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_ACCT_NAME1.name(), getAccountName1());
		fieldValues.put(FormFieldType.DD_ACCT_NUMBER1.name(), getAccountNumber1());
		fieldValues.put(FormFieldType.DD_ABA_ROUTING1.name(), getaBARouting1());
		fieldValues.put(FormFieldType.DD_BANK_NAME1.name(), getBankName1());
		fieldValues.put(FormFieldType.DD_BANK_PHONE1.name(), StringUtils.formatPhoneNumber(getBankPhone1()));
		fieldValues.put(FormFieldType.DD_BANK_ADDR1.name(), getBankAddress1());
		//Second Bank
		fieldValues.put(FormFieldType.DD_BANK_AMT_PERCENT.name(), getBankPercent2() ? "IsPercent" : "IsAmount"); // LS-3477
		fieldValues.put(FormFieldType.DD_BANK_AMOUNT2.name(), bigDecimalFormat2Places(getBankAmount2()));		 // LS-3477
		fieldValues.put(FormFieldType.DD_ACCT_TYPE2_CHCK.name(), getAccountType2()== BankAccountType.CHK ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_ACCT_TYPE2_SAV.name(), getAccountType2()== BankAccountType.SAV ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_ACCT_NAME2.name(), getAccountName2());
		fieldValues.put(FormFieldType.DD_ACCT_NUMBER2.name(), getAccountNumber2());
		fieldValues.put(FormFieldType.DD_ABA_ROUTING2.name(), getaBARouting2());
		fieldValues.put(FormFieldType.DD_BANK_NAME2.name(), getBankName2());
		fieldValues.put(FormFieldType.DD_BANK_PHONE2.name(), StringUtils.formatPhoneNumber(getBankPhone2()));
		fieldValues.put(FormFieldType.DD_BANK_ADDR2.name(), getBankAddress2());
		//Change account
		fieldValues.put(FormFieldType.DD_OLD_ACCT_TYPE_CHCK.name(), getOldAccountType() == BankAccountType.CHK ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_OLD_ACCT_TYPE_SAV.name(), getOldAccountType() == BankAccountType.SAV ? "Yes" : "Off");
		fieldValues.put(FormFieldType.DD_OLD_ACCT_NAME.name(), getOldAccountName());
		fieldValues.put(FormFieldType.DD_OLD_ACCT_NUMBER.name(), getOldAccountNumber());
		fieldValues.put(FormFieldType.DD_OLD_ABA_ROUTING.name(), getOldABARouting());
		fieldValues.put(FormFieldType.DD_OLD_BANK_NAME.name(), getOldBankName());
		fieldValues.put(FormFieldType.DD_LOAN_OUT_NAME.name(), getLoanOutName());
		fieldValues.put(FormFieldType.DD_FEIN.name(), getFein());
		if (getStopDeposit() && (cd.getEmpSignature() != null)) {
			fieldValues.put(FormFieldType.DD_SIGNATURE2_NAME.name(),
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.DD_SIGNATURE2_DATE.name(), dateFormat(format, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.DD_SIGNATURE2_NAME.name(), "");
			fieldValues.put(FormFieldType.DD_SIGNATURE2_DATE.name(), "");
		}
	}

}
