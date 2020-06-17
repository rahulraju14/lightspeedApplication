package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.type.GenderType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * FormActraWorkPermit entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "form_actra_work_permit")
public class FormActraWorkPermit extends Form<FormActraWorkPermit> {
	private static final long serialVersionUID = - 7332616709092271069L;

	/** Form version number for 2018. */
	private static final Byte ACTRA_PERMIT_VERSION_2018 = 1;
	/** The current form version number. */
	public static final Byte DEFAULT_ACTRA_PERMIT_VERSION_2018 = ACTRA_PERMIT_VERSION_2018;

	// Fields
	private String professionalName;
	private String citizenship;
	private String legalName;
	private String homePhone;
	private String talentEmailAddress;
	private String cellPhone;
	private Address talentAddress;
	private String talentAgencyName;
	private String talentAgencyPhone;
	private Date talentDob;
	private String guardianName;
	/* LS-2502 Talent Gender field changed    */
	private GenderType talentGender;
	private String talentGenderOther;
	private String sinNum;
	private Boolean sagAftraMember;
	private Boolean apprenticeMember;
	private Boolean equityMember;
	private String apprenticeNum;

	// Production Section
	private String adheredEngager;
	private String advertiser;
	private String productName;
	private String productionHouse;
	private String commercialName;
	private String charNameDesc;
	private Integer membersAuditionedNum;
	private String membersAuditionedNames;
	private String membersAuditionedNamesLine2;
	private String performanceCategory;
	private Boolean comTypeTv;
	private Boolean comTypeRadio;
	private Boolean comTypeDigitalMedia;
	private Integer numCom;
	private Date comDate;
	private String comLocation;
	private Integer applicantSignId;

	// Signature & Payment Section
	private Double workPermitFee;
	private String paidBy;
	private String paymentMethod;
	private Boolean receiptByEmail;
	private String receiptTo;
	private String ccHolderName;
	private Integer ccHolderSignId;
	private String ccNum;
	private Date ccExpirationDate;

	//Office section
	private String engagerId;
	private String comId;
	private Date dateApproved;
	private Date dateProcessed;
	private String approvedDeniedBy;
	private Boolean qualifying;
	private String workPermitNum;
	private String denialReason;
	/** Collection of Offices that this contract has been sent to. */
	private Set<Office> offices;

	/** used for get office list saved value */
	private Office office;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	// Constructors

	/** default constructor */
	public FormActraWorkPermit() {
		super();
		setVersion(ACTRA_PERMIT_VERSION_2018);
	}

	public FormActraWorkPermit(String professionalName, String citizenship, String legalName, String homePhone,
			String talentEmailAddress, String cellPhone, Address talentAddress, String talentAgencyName, String talentAgencyPhone,
			Date talentDob, String guardianName, GenderType talentGender, String sinNum, Boolean sagAftraMember,
			Boolean apprenticeMember, Boolean equityMember, String apprenticeNum, String adheredEngager,
			String advertiser, String productName, String productionHouse, String commercialName, String charNameDesc,
			Integer membersAuditionedNum, String membersAuditionedNames, String membersAuditionedNamesLine2,
			String performanceCategory, Boolean comTypeTv, Boolean comTypeRadio, Boolean comTypeDigitalMedia,
			Integer numCom, Date comDate, String comLocation, Integer applicantSignId, Double workPermitFee,
			String paidBy, String paymentMethod, Boolean receiptByEmail, String receiptTo, String ccHolderName,
			Integer ccHolderSignId, String ccNum, Date ccExpirationDate, String engagerId, String comId,
			Date dateApproved, Date dateProcessed, String approvedDeniedBy, Boolean qualifying, String workPermitNum,
			String denialReason) {
		super();
		this.professionalName = professionalName;
		this.citizenship = citizenship;
		this.legalName = legalName;
		this.homePhone = homePhone;
		this.talentEmailAddress = talentEmailAddress;
		this.cellPhone = cellPhone;
		this.talentAddress = talentAddress;
		this.talentAgencyName = talentAgencyName;
		this.talentAgencyPhone = talentAgencyPhone;
		this.talentDob = talentDob;
		this.guardianName = guardianName;
		this.talentGender = talentGender;
		this.sinNum = sinNum;
		this.sagAftraMember = sagAftraMember;
		this.apprenticeMember = apprenticeMember;
		this.equityMember = equityMember;
		this.apprenticeNum = apprenticeNum;
		this.adheredEngager = adheredEngager;
		this.advertiser = advertiser;
		this.productName = productName;
		this.productionHouse = productionHouse;
		this.commercialName = commercialName;
		this.charNameDesc = charNameDesc;
		this.membersAuditionedNum = membersAuditionedNum;
		this.membersAuditionedNames = membersAuditionedNames;
		this.membersAuditionedNamesLine2 = membersAuditionedNamesLine2;
		this.performanceCategory = performanceCategory;
		this.comTypeTv = comTypeTv;
		this.comTypeRadio = comTypeRadio;
		this.comTypeDigitalMedia = comTypeDigitalMedia;
		this.numCom = numCom;
		this.comDate = comDate;
		this.comLocation = comLocation;
		this.applicantSignId = applicantSignId;
		this.workPermitFee = workPermitFee;
		this.paidBy = paidBy;
		this.paymentMethod = paymentMethod;
		this.receiptByEmail = receiptByEmail;
		this.receiptTo = receiptTo;
		this.ccHolderName = ccHolderName;
		this.ccHolderSignId = ccHolderSignId;
		this.ccNum = ccNum;
		this.ccExpirationDate = ccExpirationDate;
		this.engagerId = engagerId;
		this.comId = comId;
		this.dateApproved = dateApproved;
		this.dateProcessed = dateProcessed;
		this.approvedDeniedBy = approvedDeniedBy;
		this.qualifying = qualifying;
		this.workPermitNum = workPermitNum;
		this.denialReason = denialReason;
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat dateFmt = new SimpleDateFormat(Constants.SHORT_DATE_FORMAT_CANADA);
		cd = cd.refresh(); // eliminate DAO reference. LS-2737
//		User user = cd.getContact().getUser();
		fieldValues.put(FormFieldType.PERMIT_PROFESSIONAL_NAME.name(), getProfessionalName());
		fieldValues.put(FormFieldType.PERMIT_CITIZENSHIP.name(), getCitizenship());
		fieldValues.put(FormFieldType.PERMIT_LEGAL_NAME.name(), getLegalName());
		fieldValues.put(FormFieldType.USER_HOME_PHONE.name(),  PhoneNumberValidator.formatted(getHomePhone()));
		fieldValues.put(FormFieldType.USER_CELL_PHONE.name(),  PhoneNumberValidator.formatted(getCellPhone()));
		fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), getTalentEmailAddress());

		if (getTalentAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), (getTalentAddress().getAddrLine1() != null ? getTalentAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(),
				((getTalentAddress().getCity() == null || getTalentAddress().getCity().isEmpty()) ? "" : (getTalentAddress().getCity() + ", ")));
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), getTalentAddress().getState());
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), getTalentAddress().getZip());
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTRY.name(), getTalentAddress().getCountry());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_COUNTRY.name(), "");
		}
		fieldValues.put(FormFieldType.AGENCY_NAME.name(), getTalentAgencyName());
		fieldValues.put(FormFieldType.AGENCY_PHONE.name(), PhoneNumberValidator.formatted(getTalentAgencyPhone()));
		fieldValues.put(FormFieldType.USER_DOB_YMD.name(), dateFormat(dateFmt, getTalentDob()));
		fieldValues.put(FormFieldType.PERMIT_GUARDIAN_NAME.name(), getGuardianName());
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSSNFormatted());
		fieldValues.put(FormFieldType.PS_TYPE_GENDER.name(), getTalentGender() == null ? "" : getTalentGender().getLabel()); //LS-2502 changed as GenderType
		fieldValues.put(FormFieldType.PS_DESC_GENDER.name(), getTalentGenderOther() == null ? "" : getTalentGenderOther());
		fieldValues.put(FormFieldType.PERMIT_SAG_AFTRA_YES_MEMBER.name(), (getSagAftraMember() == null ? "Off" : getSagAftraMember() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PERMIT_SAG_AFTRA_NO_MEMBER.name(), (getSagAftraMember() == null ? "Off" : getSagAftraMember() ? "Off" : "Yes"));
		fieldValues.put(FormFieldType.PERMIT_APPRENTICE_YES_MEMBER.name(), (getApprenticeMember() == null ? "Off" : getApprenticeMember() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PERMIT_APPRENTICE_NO_MEMBER.name(), (getApprenticeMember() == null ? "Off" : getApprenticeMember() ? "Off" : "Yes"));
		fieldValues.put(FormFieldType.PERMIT_EQUITY_YES_MEMBER.name(), (getEquityMember() == null ? "Off" : getEquityMember() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.PERMIT_EQUITY_NO_MEMBER.name(), (getEquityMember() == null ? "Off" : getEquityMember() ? "Off" : "Yes"));
		fieldValues.put(FormFieldType.PERMIT_APPRENTICE_NUM.name(), getApprenticeNum());
		fieldValues.put(FormFieldType.PERMIT_ADHERED_ENGAGER.name(), getAdheredEngager());
		fieldValues.put(FormFieldType.ADVERTISER_NAME.name(), getAdvertiser());
		fieldValues.put(FormFieldType.PRODUCT_NAME.name(), getProductName());
		fieldValues.put(FormFieldType.PROD_HOUSE_NAME.name(), getProductionHouse());
		fieldValues.put(FormFieldType.COMMERCIAL_NAME.name(), getCommercialName());
		fieldValues.put(FormFieldType.PERMIT_CHAR_NAME_DESC.name(), getCharNameDesc());
		fieldValues.put(FormFieldType.PERMIT_MEMBERS_AUDITIONED_NUM.name(), intFormat(getMembersAuditionedNum()));
		fieldValues.put(FormFieldType.PERMIT_MEMBERS_AUDITIONED_NAMES1.name(), getMembersAuditionedNames());
		fieldValues.put(FormFieldType.PERMIT_MEMBERS_AUDITIONED_NAMES2.name(), getMembersAuditionedNamesLine2());
		fieldValues.put(FormFieldType.PERFORMANCE_CATEGORY.name(), getPerformanceCategory());
		fieldValues.put(FormFieldType.PERMIT_COM_TYPE_TV.name(), (getComTypeTv() == null ? "Off" : getComTypeTv() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_COM_TYPE_RADIO.name(), (getComTypeRadio() == null ? "Off" : getComTypeRadio() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_COM_TYPE_DIGITAL_MEDIA.name(), (getComTypeDigitalMedia() == null ? "Off" : getComTypeDigitalMedia() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_NUM_COM.name(), intFormat(getNumCom()));
		fieldValues.put(FormFieldType.PERMIT_COM_DATE.name(), dateFormat(dateFmt, getComDate()));
		fieldValues.put(FormFieldType.PERMIT_COM_LOCATION.name(), getComLocation());
		if (getOffice() != null) {
			fieldValues.put(FormFieldType.ACTRA_BRANCH_CODE.name(), getOffice().getBranchCode());
		}
		else {
			fieldValues.put(FormFieldType.ACTRA_BRANCH_CODE.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			//fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), dateFormat(dateFmt, cd.getEmployerSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			//fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), "");
		}
		if (getApplicantSignId() != null) {
			//TODO
		}
		else {
			fieldValues.put(FormFieldType.PERMIT_CC_HOLDER_SIGN_ID.name(), "");
		}
		fieldValues.put(FormFieldType.PERMIT_WORK_PERMIT_FEE.name(), doubleFormat(getWorkPermitFee()));
		fieldValues.put(FormFieldType.PERMIT_PERFORMER_PAID_BY.name(), (getPaidBy() == null ? "Off" : getPaidBy().equals("P") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_AGENT_PAID_BY.name(), (getPaidBy() == null ? "Off" : getPaidBy().equals("A") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_PROODUCTION_PAID_BY.name(), (getPaidBy() == null ? "Off" : getPaidBy().equals("D") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_OTHER_PAID_BY.name(), (getPaidBy() == null ? "Off" : getPaidBy().equals("O") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_VISA_PAYMENT_METHOD.name(), (getPaymentMethod() == null ? "Off" : getPaymentMethod().equals("V") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_MASTER_CARD_PAYMENT_METHOD.name(), (getPaymentMethod() == null ? "Off" : getPaymentMethod().equals("M") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_DEDUCT_SOURCE_PAYMENT_METHOD.name(), (getPaymentMethod() == null ? "Off" : getPaymentMethod().equals("I") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_CHEQUE_PAYMENT_METHOD.name(), (getPaymentMethod() == null ? "Off" : getPaymentMethod().equals("Q") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_CASH_PAYMENT_METHOD.name(), (getPaymentMethod() == null ? "Off" : getPaymentMethod().equals("C") ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_EMAIL_RECEIPT.name(), (getReceiptByEmail() == null ? "Off" : getReceiptByEmail() ? "Off" : "Yes" ));
		fieldValues.put(FormFieldType.PERMIT_MAIL_RECEIPT.name(), (getReceiptByEmail() == null ? "Off" : getReceiptByEmail() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_ADDRESS_MAIL_RECEIPT.name(), getReceiptTo());
		fieldValues.put(FormFieldType.PERMIT_CC_HOLDER_NAME.name(), getCcHolderName());
		if (getCcHolderSignId() != null) {
			//TODO
		}
		fieldValues.put(FormFieldType.PERMIT_CC_NUM.name(), getCcNum());
		fieldValues.put(FormFieldType.PERMIT_CC_EXPIRATION_DATE.name(), dateFormat(dateFmt, getCcExpirationDate()));
		fieldValues.put(FormFieldType.PERMIT_ENGAGER_ID.name(), getEngagerId());
		fieldValues.put(FormFieldType.PERMIT_COM_ID.name(), getComId());
		fieldValues.put(FormFieldType.PERMIT_APPROVED_DATE.name(), dateFormat(dateFmt, getDateApproved()));
		fieldValues.put(FormFieldType.PERMIT_PROCESSED_DATE.name(), dateFormat(dateFmt, getDateProcessed()));
		fieldValues.put(FormFieldType.PERMIT_APPROVED_DENIED_BY_SIGN_ID.name(), getApprovedDeniedBy());
		fieldValues.put(FormFieldType.PERMIT_YES_QUALIFYING.name(), (getQualifying() == null ? "Off" : getQualifying() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.PERMIT_NO_QUALIFYING.name(), (getQualifying() == null ? "Off" : getQualifying() ? "Off" : "Yes" ));
		fieldValues.put(FormFieldType.PERMIT_WORK_PERMIT_NUM.name(), getWorkPermitNum());
		fieldValues.put(FormFieldType.PERMIT_DENIAL_REASON.name(), getDenialReason());
	}

	/** See {@link #viewSSN}. */
	@JsonIgnore
	@Transient
	public String getViewSSN() {
		if (getSinNum() == null) {
			viewSSN = null;
		}
		else if (viewSSN == null) {
			if (getSinNum().length() >= 4) {
				viewSSN = "###-##-" + getSinNum().substring(getSinNum().length()-4);
			}
			else {
				viewSSN = getSinNum();
			}
		}
		return viewSSN;
	}

	@Transient
	public String getSSNFormatted() {
		String str = null;
		if (getSinNum() != null && getSinNum().length() == 9) {
			str = sinNum.substring(0,3) + "-" + sinNum.substring(3, 5) + "-" + sinNum.substring(5);
		}
		return str;
	}

	/** See {@link #professionalName}. */
	@Column(name = "Professional_Name", length = 150)
	public String getProfessionalName() {
		return this.professionalName;
	}
	/** See {@link #professionalName}. */
	public void setProfessionalName(String professionalName) {
		this.professionalName = professionalName;
	}

	/** See {@link #citizenship}. */
	@Column(name = "Citizenship", length = 50)
	public String getCitizenship() {
		return this.citizenship;
	}
	/** See {@link #citizenship}. */
	public void setCitizenship(String citizenship) {
		this.citizenship = citizenship;
	}

	/** See {@link #legalName}. */
	@Column(name = "Legal_Name", length = 150)
	public String getLegalName() {
		return this.legalName;
	}
	/** See {@link #legalName}. */
	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}

	/** See {@link #homePhone}. */
	@Column(name = "Home_Phone", length = 25)
	public String getHomePhone() {
		return this.homePhone;
	}
	/** See {@link #homePhone}. */
	public void setHomePhone(String homePhone) {
		this.homePhone = homePhone;
	}

	/** See {@link #cellPhone}. */
	@Column(name = "Cell_Phone", length = 25)
	public String getCellPhone() {
		return this.cellPhone;
	}
	/** See {@link #cellPhone}. */
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}

	/** See {@link #talentEmailAddress}. */
	@Column(name = "Talent_Email_Address", length = 100)
	public String getTalentEmailAddress() {
		return this.talentEmailAddress;
	}
	/** See {@link #talentEmailAddress}. */
	public void setTalentEmailAddress(String talentEmailAddress) {
		this.talentEmailAddress = talentEmailAddress;
	}

	/** See {@link #talentAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Talent_Address_Id")
	public Address getTalentAddress() {
		return this.talentAddress;
	}
	/** See {@link #talentAddress}. */
	public void setTalentAddress(Address talentAddress) {
		this.talentAddress = talentAddress;
	}

	/** See {@link #talentAgencyName}. */
	@Column(name = "Talent_Agency_Name", length = 150)
	public String getTalentAgencyName() {
		return this.talentAgencyName;
	}
	/** See {@link #talentAgencyName}. */
	public void setTalentAgencyName(String talentAgencyName) {
		this.talentAgencyName = talentAgencyName;
	}

	/** See {@link #talentAgencyName}. */
	@Column(name = "Talent_Agency_Phone", length = 25)
	public String getTalentAgencyPhone() {
		return talentAgencyPhone;
	}
	/** See {@link #talentAgencyName}. */
	public void setTalentAgencyPhone(String talentAgencyPhone) {
		this.talentAgencyPhone = talentAgencyPhone;
	}

	/** See {@link #talentDob}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Talent_Dob", length = 0)
	public Date getTalentDob() {
		return talentDob;
	}
	/** See {@link #talentDob}. */
	public void setTalentDob(Date talentDob) {
		this.talentDob = talentDob;
	}

	/** See {@link #guardianName}. */
	@Column(name = "Guardian_Name", length = 150)
	public String getGuardianName() {
		return guardianName;
	}
	/** See {@link #guardianName}. */
	public void setGuardianName(String guardianName) {
		this.guardianName = guardianName;
	}

	/** See {@link #talentGender}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Talent_Gender", length = 1)
	public GenderType getTalentGender() {
		return talentGender;
	}
	/** See {@link #talentGender}. */
	public void setTalentGender(GenderType talentGender) {
		this.talentGender = talentGender;
	}

	/** See {@link #talentGenderOther}. */
	@Column(name = "Talent_Gender_Other", length = 50)
	public String getTalentGenderOther() {
		return talentGenderOther;
	}

	/** See {@link #talentGenderOther}. */
	public void setTalentGenderOther(String talentGenderOther) {
		this.talentGenderOther = talentGenderOther;
	}

	/** See {@link #sinNum}. */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Sin_Num", nullable = true, length = 1000)
	public String getSinNum() {
		return sinNum;
	}
	/** See {@link #sinNum}. */
	public void setSinNum(String sinNum) {
		this.sinNum = sinNum;
	}

	/** See {@link #sagAftraMember}. */
	@Column(name = "Sag_Aftra_Member")
	public Boolean getSagAftraMember() {
		return this.sagAftraMember;
	}
	/** See {@link #sagAftraMember}. */
	public void setSagAftraMember(Boolean sagAftraMember) {
		this.sagAftraMember = sagAftraMember;
	}

	/** See {@link #equityMember}. */
	@Column(name = "Equity_Member")
	public Boolean getEquityMember() {
		return this.equityMember;
	}
	/** See {@link #equityMember}. */
	public void setEquityMember(Boolean equityMember) {
		this.equityMember = equityMember;
	}

	/** See {@link #apprenticeMember}. */
	@Column(name = "Apprentice_Member")
	public Boolean getApprenticeMember() {
		return this.apprenticeMember;
	}
	/** See {@link #apprenticeMember}. */
	public void setApprenticeMember(Boolean apprenticeMember) {
		this.apprenticeMember = apprenticeMember;
	}

	/** See {@link #apprenticeNum}. */
	@Column(name = "Apprentice_Num", length = 45)
	public String getApprenticeNum() {
		return this.apprenticeNum;
	}
	/** See {@link #apprenticeNum}. */
	public void setApprenticeNum(String apprenticeNum) {
		this.apprenticeNum = apprenticeNum;
	}

	/** See {@link #adheredEngager}. */
	@Column(name = "Adhered_Engager", length = 150)
	public String getAdheredEngager() {
		return adheredEngager;
	}
	/** See {@link #adheredEngager}. */
	public void setAdheredEngager(String adheredEngager) {
		this.adheredEngager = adheredEngager;
	}

	/** See {@link #advertiser}. */
	@Column(name = "Advertiser", length = 150)
	public String getAdvertiser() {
		return advertiser;
	}
	/** See {@link #advertiser}. */
	public void setAdvertiser(String advertiser) {
		this.advertiser = advertiser;
	}

	/** See {@link #productionHouse}. */
	@Column(name = "Production_House", length = 150)
	public String getProductionHouse() {
		return productionHouse;
	}
	/** See {@link #productionHouse}. */
	public void setProductionHouse(String productionHouse) {
		this.productionHouse = productionHouse;
	}

	/** See {@link #productName}. */
	@Column(name = "Product_Name", length = 150)
	public String getProductName() {
		return productName;
	}
	/** See {@link #productName}. */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/** See {@link #membersAuditionedNum}. */
	@Column(name = "Members_Auditioned_Num")
	public Integer getMembersAuditionedNum() {
		return this.membersAuditionedNum;
	}
	/** See {@link #membersAuditionedNum}. */
	public void setMembersAuditionedNum(Integer membersAuditionedNum) {
		this.membersAuditionedNum = membersAuditionedNum;
	}

	/** See {@link #membersAuditionedNames}. */
	@Column(name = "Members_Auditioned_Names", length = 1000)
	public String getMembersAuditionedNames() {
		return this.membersAuditionedNames;
	}
	/** See {@link #membersAuditionedNames}. */
	public void setMembersAuditionedNames(String membersAuditionedNames) {
		this.membersAuditionedNames = membersAuditionedNames;
	}

	/** See {@link #membersAuditionedNamesLine2}. */
	@Column(name = "Members_Auditioned_Names_Line2", length = 1000)
	public String getMembersAuditionedNamesLine2() {
		return membersAuditionedNamesLine2;
	}
	/** See {@link #membersAuditionedNamesLine2}. */
	public void setMembersAuditionedNamesLine2(String membersAuditionedNamesLine2) {
		this.membersAuditionedNamesLine2 = membersAuditionedNamesLine2;
	}

	/** See {@link #commercialName}. */
	@Column(name = "Commercial_Name", length = 100)
	public String getCommercialName() {
		return this.commercialName;
	}
	/** See {@link #commercialName}. */
	public void setCommercialName(String commercialName) {
		this.commercialName = commercialName;
	}

	/** See {@link #comDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Com_Date", length = 0)
	public Date getComDate() {
		return this.comDate;
	}
	/** See {@link #comDate}. */
	public void setComDate(Date comDate) {
		this.comDate = comDate;
	}

	/** See {@link #charNameDesc}. */
	@Column(name = "Char_Name_Desc", length = 500)
	public String getCharNameDesc() {
		return this.charNameDesc;
	}
	/** See {@link #charNameDesc}. */
	public void setCharNameDesc(String charNameDesc) {
		this.charNameDesc = charNameDesc;
	}

	/** See {@link #numCom}. */
	@Column(name = "Num_Com")
	public Integer getNumCom() {
		return this.numCom;
	}
	/** See {@link #numCom}. */
	public void setNumCom(Integer numCom) {
		this.numCom = numCom;
	}

	/** See {@link #comId}. */
	@Column(name = "Com_Id", length = 30)
	public String getComId() {
		return this.comId;
	}
	/** See {@link #comId}. */
	public void setComId(String comId) {
		this.comId = comId;
	}

	/** See {@link #comTypeTv}. */
	@Column(name = "Com_Type_Tv")
	public Boolean getComTypeTv() {
		return this.comTypeTv;
	}
	/** See {@link #comTypeTv}. */
	public void setComTypeTv(Boolean comTypeTv) {
		this.comTypeTv = comTypeTv;
	}

	/** See {@link #comTypeRadio}. */
	@Column(name = "Com_Type_Radio")
	public Boolean getComTypeRadio() {
		return this.comTypeRadio;
	}
	/** See {@link #comTypeRadio}. */
	public void setComTypeRadio(Boolean comTypeRadio) {
		this.comTypeRadio = comTypeRadio;
	}

	/** See {@link #comTypeDigitalMedia}. */
	@Column(name = "Com_Type_Digital_Media", length = 45)
	public Boolean getComTypeDigitalMedia() {
		return this.comTypeDigitalMedia;
	}
	/** See {@link #comTypeDigitalMedia}. */
	public void setComTypeDigitalMedia(Boolean comTypeDigitalMedia) {
		this.comTypeDigitalMedia = comTypeDigitalMedia;
	}

	/** See {@link #performanceCategory}. */
	@Column(name = "Performance_Category", length = 11)
	public String getPerformanceCategory() {
		return this.performanceCategory;
	}
	/** See {@link #performanceCategory}. */
	public void setPerformanceCategory(String performanceCategory) {
		this.performanceCategory = performanceCategory;
	}

	/** See {@link #comLocation}. */
	@Column(name = "Com_Location", length = 100)
	public String getComLocation() {
		return this.comLocation;
	}
	/** See {@link #comLocation}. */
	public void setComLocation(String comLocation) {
		this.comLocation = comLocation;
	}

	/** See {@link #applicantSignId}. */
	@Column(name = "Applicant_Sign_Id")
	public Integer getApplicantSignId() {
		return this.applicantSignId;
	}
	/** See {@link #applicantSignId}. */
	public void setApplicantSignId(Integer applicantSignId) {
		this.applicantSignId = applicantSignId;
	}

	/** See {@link #workPermitFee}. */
	@Column(name = "Work_Permit_Fee", precision = 6)
	public Double getWorkPermitFee() {
		return this.workPermitFee;
	}
	/** See {@link #workPermitFee}. */
	public void setWorkPermitFee(Double workPermitFee) {
		this.workPermitFee = workPermitFee;
	}

	/** See {@link #workPermitNum}. */
	@Column(name = "Work_Permit_Num", length = 45)
	public String getWorkPermitNum() {
		return this.workPermitNum;
	}
	/** See {@link #workPermitNum}. */
	public void setWorkPermitNum(String workPermitNum) {
		this.workPermitNum = workPermitNum;
	}

	/** See {@link #paidBy}. */
	@Column(name = "Paid_By")
	public String getPaidBy() {
		return this.paidBy;
	}
	/** See {@link #paidBy}. */
	public void setPaidBy(String paidBy) {
		this.paidBy = paidBy;
	}

	/** See {@link #paymentMethod}. */
	@Column(name = "Payment_Method", length = 20)
	public String getPaymentMethod() {
		return this.paymentMethod;
	}
	/** See {@link #paymentMethod}. */
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	/** See {@link #receiptByEmail}. */
	@Column(name = "Receipt_By_Email")
	public Boolean getReceiptByEmail() {
		return this.receiptByEmail;
	}
	/** See {@link #receiptByEmail}. */
	public void setReceiptByEmail(Boolean receiptByEmail) {
		this.receiptByEmail = receiptByEmail;
	}

	/** See {@link #receiptTo}. */
	@Column(name = "Receipt_To", length = 500)
	public String getReceiptTo() {
		return this.receiptTo;
	}
	/** See {@link #receiptTo}. */
	public void setReceiptTo(String receiptTo) {
		this.receiptTo = receiptTo;
	}

	/** See {@link #ccHolderName}. */
	@Column(name = "CC_Holder_Name", length = 100)
	public String getCcHolderName() {
		return this.ccHolderName;
	}
	/** See {@link #ccHolderName}. */
	public void setCcHolderName(String ccHolderName) {
		this.ccHolderName = ccHolderName;
	}

	/** See {@link #ccHolderSignId}. */
	@Column(name = "CC_Holder_Sign_Id")
	public Integer getCcHolderSignId() {
		return this.ccHolderSignId;
	}
	/** See {@link #ccHolderSignId}. */
	public void setCcHolderSignId(Integer ccHolderSignId) {
		this.ccHolderSignId = ccHolderSignId;
	}

	/** See {@link #ccNum}. */
	@Column(name = "CC_Num", length = 45)
	public String getCcNum() {
		return this.ccNum;
	}
	/** See {@link #ccNum}. */
	public void setCcNum(String ccNum) {
		this.ccNum = ccNum;
	}

	/** See {@link #ccExpirationDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "CC_Expiration_Date", length = 0)
	public Date getCcExpirationDate() {
		return this.ccExpirationDate;
	}
	/** See {@link #ccExpirationDate}. */
	public void setCcExpirationDate(Date ccExpirationDate) {
		this.ccExpirationDate = ccExpirationDate;
	}

	/** See {@link #engagerId}. */
	@Column(name = "Engager_Id", length = 30)
	public String getEngagerId() {
		return this.engagerId;
	}
	/** See {@link #engagerId}. */
	public void setEngagerId(String engagerId) {
		this.engagerId = engagerId;
	}

	/** See {@link #dateApproved}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date_Approved", length = 0)
	public Date getDateApproved() {
		return this.dateApproved;
	}
	/** See {@link #dateApproved}. */
	public void setDateApproved(Date dateApproved) {
		this.dateApproved = dateApproved;
	}

	/** See {@link #dateProcessed}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date_Processed", length = 0)
	public Date getDateProcessed() {
		return this.dateProcessed;
	}
	/** See {@link #dateProcessed}. */
	public void setDateProcessed(Date dateProcessed) {
		this.dateProcessed = dateProcessed;
	}

	/** See {@link #approvedDeniedBy}. */
	@Column(name = "Approved_Denied_By", length = 100)
	public String getApprovedDeniedBy() {
		return this.approvedDeniedBy;
	}
	/** See {@link #approvedDeniedBy}. */
	public void setApprovedDeniedBy(String approvedDeniedBy) {
		this.approvedDeniedBy = approvedDeniedBy;
	}

	/** See {@link #qualifying}. */
	@Column(name = "Qualifying")
	public Boolean getQualifying() {
		return this.qualifying;
	}
	/** See {@link #qualifying}. */
	public void setQualifying(Boolean qualifying) {
		this.qualifying = qualifying;
	}

	/** See {@link #denialReason}. */
	@Column(name = "Denial_Reason", length = 150)
	public String getDenialReason() {
		return this.denialReason;
	}
	/** See {@link #denialReason}. */
	public void setDenialReason(String denialReason) {
		this.denialReason = denialReason;
	}
	@ManyToMany(
			targetEntity=Office.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "actra_work_permit_office",
			joinColumns = {@JoinColumn(name = "Form_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Office_Id")}
			)
	/** See {@link #offices}. */
	public Set<Office> getOffices() {
		return offices;
	}

	/** See {@link #offices}. */
	public void setOffices(Set<Office> offices) {
		this.offices = offices;
	}

	/** See {@link #office}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Office_Id", nullable = true)
	public Office getOffice() {
		return office;
	}

	/** See {@link #office}. */
	public void setOffice(Office office) {
		this.office = office;
	}

	@Override
	public void trim() {
		talentAddress.trimIsEmpty();
		professionalName = trim(professionalName);
		citizenship = trim(citizenship);
		legalName = trim(legalName);
		homePhone = trim(homePhone);
		talentEmailAddress = trim(talentEmailAddress);
		cellPhone = trim(cellPhone);
		talentAgencyName = trim(talentAgencyName);
		talentAgencyPhone = trim(talentAgencyPhone);
		guardianName = trim(guardianName);
		sinNum = trim(sinNum);
		apprenticeNum = trim(apprenticeNum);
		adheredEngager = trim(adheredEngager);
		advertiser = trim(advertiser);
		productName = trim(productName);
		productionHouse = trim(productionHouse);
		commercialName = trim(commercialName);
		charNameDesc = trim(charNameDesc);
		membersAuditionedNames = trim(membersAuditionedNames);
		membersAuditionedNamesLine2 = trim(membersAuditionedNamesLine2);
		performanceCategory = trim(performanceCategory);
		comLocation = trim(comLocation);
		paidBy = trim(paidBy);
		paymentMethod = trim(paymentMethod);
		receiptTo = trim(receiptTo);
		ccHolderName = trim(ccHolderName);
		ccNum = trim(ccNum);
		engagerId = trim(engagerId);
		comId = trim(comId);
		approvedDeniedBy = trim(approvedDeniedBy);
		workPermitNum = trim(workPermitNum);
		denialReason = trim(denialReason);
	}

}
