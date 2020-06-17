package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.type.WithholdPercentage;

/**
 * This entity contains all the fields of the A4 form. It is used to maintain
 * the user wise record of the A4 entries.
 *
 */
@Entity
@Table(name = "form_a4")
public class FormA4 extends Form<FormA4> {

	private static final long serialVersionUID = -6876338681998282126L;

	public static final Byte A4_VERSION_2017 = 17; // last 2 digit of the years
	public static final Byte A4_VERSION_2020 = 20; // last 2 digit of the years
	public static final Byte DEFAULT_A4_VERSION = A4_VERSION_2020;

	private String fullName;

	private String socialSecurity;

	private Address address;

	private Boolean zeroWithholding;

	private WithholdPercentage withholdPercentage;

	private Integer extraAmount;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	public FormA4() {
		super();
		setVersion(DEFAULT_A4_VERSION);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		fieldValues.put(FormFieldType.FULL_NAME.name(), getFullName() );
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());

		if (getAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), (getAddress().getAddrLine1() != null ? getAddress().getAddrLine1() : ""));
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

		fieldValues.put(FormFieldType.A4_CHECK1_WITHHLD.name(),  (getZeroWithholding() != null && getZeroWithholding()) ? "Off" : "Yes");
		fieldValues.put(FormFieldType.A4_CHECK2_ZERO_WITHHOLD.name(), (getZeroWithholding() != null && getZeroWithholding()) ? "Yes" : "Off");
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_08.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_08 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_13.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_13 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_18.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_18 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_27.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_27 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_36.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_36 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_42.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_42 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK1_WITHHOLD_51.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_51 ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_CHECK_EXTRA_AMOUNT.name(),
				(getWithholdPercentage() == WithholdPercentage.Per_Extra ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.A4_EXTRA_AMOUNT.name(), intFormat(getExtraAmount()));

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

	/** See {@link #withholdPercentage}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Withhold_Percentage", nullable = true, length = 10)
	public WithholdPercentage getWithholdPercentage() {
		return withholdPercentage;
	}
	/** See {@link #withholdPercentage}. */
	public void setWithholdPercentage(WithholdPercentage withholdPercentage) {
		this.withholdPercentage = withholdPercentage;
	}

	/** See {@link #extraAmount}. */
	@Column(name = "Extra_Amount", nullable = true)
	public Integer getExtraAmount() {
		return extraAmount;
	}
	/** See {@link #extraAmount}. */
	public void setExtraAmount(Integer extraAmount) {
		this.extraAmount = extraAmount;
	}

	/** See {@link #zeroWithholding}. */
	@Column(name = "Zero_Withholding", nullable = true)
	public Boolean getZeroWithholding() {
		return zeroWithholding;
	}
	/** See {@link #zeroWithholding}. */
	public void setZeroWithholding(Boolean zeroWithholding) {
		this.zeroWithholding = zeroWithholding;
	}

}
