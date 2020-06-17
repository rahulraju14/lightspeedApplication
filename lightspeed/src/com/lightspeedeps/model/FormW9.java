package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.annotations.Type;

import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.type.TaxClassificationType;
import com.lightspeedeps.util.common.StringUtils;

/**
 * This entity contains all the fields of the Federal W-9 form. It is used to
 * maintain the user wise record of the W9 entries.
 */
@Entity
@Table(name = "form_w9")
@NamedQueries ({
	@NamedQuery(name=FormW9.GET_NON_NULL_SSN_FEDID, query = "select w9 from FormW9 w9 where w9.socialSecurity is not null and w9.fedidNumber is not null"),
})
public class FormW9 extends Form<FormW9> {

	private static final long serialVersionUID = -1431720057366865851L;

	public static final String GET_NON_NULL_SSN_FEDID = "getNonNullSSNFedid";

	public static final Byte W9_VERSION_2014 = 14; // version = last 2 digits of year
	public static final Byte W9_VERSION_2018 = 18;
	public static final Byte DEFAULT_W9_VERSION = W9_VERSION_2018;

	/** Full name of the employee obtaining the Taxpayer Id Number*/
	private String fullName;

	/** Business name, trade name, DBA name or disregard entity name. */
	private String businessName;

	/** Federal tax classification of the employee */
	private TaxClassificationType companyType;

	/**  LLC that is not a single member LLC, C Corporation or S Corporation */
	private String taxClassification;

	/** Other type of entity tax classification */
	private String otherTaxClassification;

	/** Exempt Payee reporting code */
	private Integer exemptPayeeCode;

	/** Exempt FATC reporting code */
	private String fATCAReportingCode;

	/** Employee address */
	private Address address;

	/** Requester Name and Address */
	private String requesterNameAddress;

	/** List of account numbers associated with the employee */
	private String accountNumbers;

	/** encrypted Federal Employer Id Number (FEIN) */
	private String fedidNumber;

	/** encrypted SSN */
	private String socialSecurity;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;


	/** default public constructor */
	public FormW9() {
		super();
		setVersion(DEFAULT_W9_VERSION);
	}

	/**
	 * Clean up data fields before persisting the object. In particular,
	 * zero-length SSN or Fed id strings are replaced by null.
	 */
	@PrePersist
	@PreUpdate
	private void prepareData(){
		if (socialSecurity != null && socialSecurity.length() == 0) {
			socialSecurity = null;
		}
		if (fedidNumber != null && fedidNumber.length() == 0) {
			fedidNumber = null;
		}
	}

	/**
	 * Used during printing of a W9 form, to map the fields in the XFDF of the
	 * master PDF to the data from the FormW9 record.
	 */
	@Override
	public void fillFieldValues(ContactDocument cd, Map<String, String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		fieldValues.put(FormFieldType.WTPA_NAME.name(), getFullName());

		fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(), getBusinessName());

		// Company type check-boxes:
		fieldValues.put(FormFieldType.W9_INDIVIDUAL.name(),        (getCompanyType()==TaxClassificationType.I ? "1" : "Off"));
		fieldValues.put(FormFieldType.W9_C_CORP.name(),            (getCompanyType()==TaxClassificationType.C ? "2" : "Off"));
		fieldValues.put(FormFieldType.W9_S_CORP.name(),            (getCompanyType()==TaxClassificationType.S ? "3" : "Off"));
		fieldValues.put(FormFieldType.W9_PARTNERSHIP.name(),       (getCompanyType()==TaxClassificationType.P ? "4" : "Off"));
		fieldValues.put(FormFieldType.W9_TRUST_ESTATE.name(),      (getCompanyType()==TaxClassificationType.T ? "5" : "Off"));
		fieldValues.put(FormFieldType.W9_LLC.name(),               (getCompanyType()==TaxClassificationType.L ? "6" : "Off"));
		fieldValues.put(FormFieldType.W9_OTHER_TAX_CLASSES.name(), (getCompanyType()==TaxClassificationType.O ? "7" : "Off"));

		fieldValues.put(FormFieldType.W9_TAX_CLASS_TYPES.name(), getTaxClassification());

		fieldValues.put(FormFieldType.W9_FATCA_CODE.name(), getfATCAReportingCode());
		fieldValues.put(FormFieldType.W9_EXEMPT_PAYEE_CODE.name(), intFormat(getExemptPayeeCode()));
		fieldValues.put(FormFieldType.W9_REQUESTER.name(), getRequesterNameAddress());
		fieldValues.put(FormFieldType.W9_OTHER_TAX_CLASS.name(), getOtherTaxClassification());

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), getAddress().getAddrLine1Line2());
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(),
					(getAddress().getCity() != null ? getAddress().getCity() + ", " : "") +
					(getAddress().getState() != null ? getAddress().getState() + ", " : "") +
					(getAddress().getZip() != null ? getAddress().getZip() : ""));
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
 			// W4 pdf doesn't have separate city, state, zip fields, so we don't need to "clear" them.
 			fieldValues.put(FormFieldType.HOME_ADDR_CITY_STATE_ZIP.name(), "");
 		}

		fieldValues.put(FormFieldType.ACCOUNT_NUMBER.name(), getAccountNumbers());

		if (getSocialSecurity() != null && getSocialSecurity().length() == 9) {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), getSocialSecurity().substring(0, 3));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), getSocialSecurity().substring(3, 5));
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), getSocialSecurity().substring(5));
		}
		else {
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_1.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_2.name(), "");
			fieldValues.put(FormFieldType.SOCIAL_SEC_PART_3.name(), "");
		}

		if (getFedidNumber() != null && getFedidNumber().length() == 9) { // stored unformatted as 123456789
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER_2.name(), getFedidNumber().substring(0, 2));
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER_7.name(), getFedidNumber().substring(2));
		}
		else {
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER_2.name(), "");
			fieldValues.put(FormFieldType.LOAN_OUT_FEDID_NUMBER_7.name(), "");
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

	/**
	 * Export the fields in this FormW4 using the supplied Exporter.
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append(getVersion()); // version number of form
		ex.append(getFullName());
		ex.append(getBusinessName());
		ex.append(getCompanyType().getLabel());
		ex.append(getTaxClassification());
		ex.append(getOtherTaxClassification());
		ex.append(getExemptPayeeCode());
		ex.append(getfATCAReportingCode());
		Address addr = getAddress();
		if (addr == null) {
			addr = new Address();
		}
		ex.append(addr.getAddrLine1());
		ex.append(addr.getCity());
		ex.append(addr.getState());
		ex.append(addr.getZip());
		ex.append(getRequesterNameAddress());
		ex.append(getAccountNumbers());
		ex.append(getSocialSecurity());
		ex.append(StringUtils.cleanTaxId(getFedidNumber()));
	}

	@Override
	public void trim() {
		fullName = trim(fullName);
		businessName = trim(businessName);
		socialSecurity = trim(socialSecurity);
		fedidNumber = trim(fedidNumber);
		taxClassification = trim(taxClassification);
		otherTaxClassification = trim(otherTaxClassification);
		fATCAReportingCode = trim(fATCAReportingCode);
		address.trimIsEmpty();
		requesterNameAddress = trim(requesterNameAddress);
		accountNumbers = trim(accountNumbers);
	}

	/**See {@link #fullName}. */
	@Column(name = "Full_Name", nullable = true, length = 60)
	public String getFullName() {
		return fullName;
	}
	/**See {@link #fullName}. */
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	/**See {@link #businessName}. */
	@Column(name = "Business_Name", nullable = true, length = 200)
	public String getBusinessName() {
		return businessName;
	}
	/**See {@link #businessName}. */
	public void setBusinessName(String businessName) {
		this.businessName = businessName;
	}

	/**See {@link #companyType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Company_Type", nullable = true, length = 10)
	public TaxClassificationType getCompanyType() {
		return companyType;
	}
	/**See {@link #companyType}. */
	public void setCompanyType(TaxClassificationType companyType) {
		this.companyType = companyType;
	}

	/**See {@link #taxClassification}. */
	@Column(name = "Tax_Classification", nullable = true, length = 10)
	public String getTaxClassification() {
		return taxClassification;
	}
	/**See {@link #taxClassification}. */
	public void setTaxClassification(String taxClassification) {
		if(taxClassification != null) {
			// LS-2153 - Help user by making it upper case
			taxClassification = taxClassification.toUpperCase();
		}
		this.taxClassification = taxClassification;
	}

	/**See {@link #otherTaxClassification}. */
	@Column(name = "Other_Tax_Classification", nullable = true, length = 100)
	public String getOtherTaxClassification() {
		return otherTaxClassification;
	}
	/**See {@link #otherTaxClassification}. */
	public void setOtherTaxClassification(String otherTaxClassification) {
		this.otherTaxClassification = otherTaxClassification;
	}

	/**See {@link #exemptPayeeCode}. */
	@Column(name = "Exempt_Payee_Code", nullable = true, length = 30)
	public Integer getExemptPayeeCode() {
		return exemptPayeeCode;
	}
	/**See {@link #exemptPayeeCode}. */
	public void setExemptPayeeCode(Integer exemptPayeeCode) {
		this.exemptPayeeCode = exemptPayeeCode;
	}

	/**See {@link #fATCAReportingCode}. */
	@Column(name = "FATCA_Reporting_Code", nullable = true, length = 30)
	public String getfATCAReportingCode() {
		return fATCAReportingCode;
	}
	/**See {@link #fATCAReportingCode}. */
	public void setfATCAReportingCode(String fATCAReportingCode) {
		this.fATCAReportingCode = fATCAReportingCode;
	}

	/**See {@link #accountNumbers}. */
	@Column(name = "Account_Numbers", nullable = true, length = 200)
	public String getAccountNumbers() {
		return accountNumbers;
	}
	/**See {@link #accountNumbers}. */
	public void setAccountNumbers(String accountNumbers) {
		this.accountNumbers = accountNumbers;
	}

	/**See {@link #fedidNumber}. */
	@Type(type = "encryptedString") // defined in User.java
	@Column(name = "Fedid_Number", nullable = true, length = 1000)
	public String getFedidNumber() {
		return fedidNumber;
	}
	/**See {@link #fedidNumber}. */
	public void setFedidNumber(String fedidNumber) {
		this.fedidNumber = fedidNumber;
	}

	/**See {@link #socialSecurity}. */
	@Type(type = "encryptedString") // defined in User.java
	@Column(name = "Social_Security", nullable = true, length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	/**See {@link #socialSecurity}. */
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

	@Transient
	public String getSSNFormatted() {
		String str = null;
		if (getSocialSecurity() != null && getSocialSecurity().length() == 9) {
			str = socialSecurity.substring(0,3) + "-" + socialSecurity.substring(3, 5) + "-" + socialSecurity.substring(5);
		}
		return str;
	}

	/**See {@link #address}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	/**See {@link #address}. */
	public void setAddress(Address address) {
		this.address = address;
	}

	/**See {@link #requesterNameAddress}. */
	@Column(name = "Requester_Name_Address", nullable = true, length = 200)
	public String getRequesterNameAddress() {
		return requesterNameAddress;
	}
	/**See {@link #requesterNameAddress}. */
	public void setRequesterNameAddress(String requesterNameAddress) {
		this.requesterNameAddress = requesterNameAddress;
	}

}
