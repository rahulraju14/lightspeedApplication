package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.*;

import org.hibernate.annotations.Type;

import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.StringUtils;

/**
 * FormActraContract entity. This holds the data for the ACTRA contract, used in
 * Canadian productions. Each instance has a related {@link #weeklyTimecard}
 * instance to hold the daily time information (in "Section B" of the contract).
 * The weeklyTimecard is initialized with 13 DailyTime items, as this is the
 * maximum number of line items allowed in the ACTRA contract.
 * <p>
 * The {@link SignedEvent} created for the various signatures (or initials)
 * within the contract are distinguished by the
 * {@link com.lightspeedeps.type.TimedEventType TimedEventType} assigned:
 * <ul>
 * <li>In Section A,</li>
 * <ul>
 * <li>the performer's signature will be a
 * {@link com.lightspeedeps.type.TimedEventType#SUBMIT SUBMIT} event.</li>
 * <li>the producer's signature will be a
 * {@link com.lightspeedeps.type.TimedEventType#SIGN SIGN} event.</li>
 * </ul>
 * <li>In Section B,</li>
 * <ul>
 * <li>the performer initialing the "Agree" box will be a
 * {@link com.lightspeedeps.type.TimedEventType#AGREE AGREE} event.</li>
 * <li>the performer initialing the "Disagree" box will be a
 * {@link com.lightspeedeps.type.TimedEventType#DISAGREE DISAGREE} event.</li>
 * <li>the representative's signature will be a
 * {@link com.lightspeedeps.type.TimedEventType#APPROVE APPROVE} event.</li>
 * </ul>
 * </ul>
 */
@Entity
@Table(name = "form_actra_contract")
public class FormActraContract extends Form<FormActraContract> {
	private static final long serialVersionUID = - 7595975265527774181L;

	/** The maximum number of line items for time-worked on the ACTRA contract. */
	public static final byte ACTRA_NUM_TIME_ENTRIES = 13;

	// Fields

	/** Form version number for 2018. */
	public static final Byte ACTRA_CONTRACT_VERSION_2018 = 1;
	/** Form version number for ACTRA Long Form. */
	public static final Byte ACTRA_CONTRACT_LONG_VERSION_2018 = 2;
	/** The current form version number. */
	public static final Byte DEFAULT_ACTRA_CONTRACT_VERSION = ACTRA_CONTRACT_VERSION_2018;

	private String contractNum;
	private String agencyName;
	private String agencyProducer;
	private Address agencyAddress;
	private String advertiserName;
	private String productName;
	private String prodHouseName;
	private Address prodHouseAddress;
	private Boolean paySessionFee;
	private String directorName;
	private String loanOutName;
	private String talentName;
	private Address talentAddress;
	private String talentEmailAddress;
	private String talentPhoneNum;
	private String socialInsuranceNum;
	private String gstHst;
	private String qst;
	private Date dob;
	private String talentAgencyName;
	private String agencyContact;
	private String performanceCategory;
	private String fullMemberNum;
	private String apprenticeNum;
	private String workPermitNum;
	private String nationalTv;
	private String nationalRadio;
	private String nationalDigitalMediaVideo;
	private String nationalDigitalMediaAudio;
	private Integer tagsTv;
	private Integer tagsRadio;
	private Integer tagsDigitalMedia;
	private Integer regionalChangesTv;
	private Integer regionalChangesRadio;
	private Integer regionalChangesDigitalMedia;
	private String psaTv;
	private String psaRadio;
	private String psaDigitalMedia;
	private String demoTv;
	private String demoRadio;
	private String demoDigital;
	private String demoPresentation;
	private String demoInfomercial;
	private String seasonalTv;
	private String seasonalRadio;
	private String seasonalDealer;
	private String seasonalDealerTv;
	private String seasonalDealerRadio;
	private String seasonalDoubleShoot;
	private String seasonalJointPromo;
	private String localRegionalCategoryNum;
	private String localRegionalTv;
	private String localRegionalRadio;
	private String localRegionalDigitalMedia;
	private String localRegionalDemo;
	private String localRegionalDigitalMediaBroadcast;
	private String localRegionalBroadcastDigitalMedia;
	private String localRegionalOther;
	private String localRegionalPilotProject;
	private String shortLifeTv7Days;
	private String shortLifeTv14Days;
	private String shortLifeTv31Days;
	private String shortLifeTv45Days;
	private String shortLifeRadio7Days;
	private String shortLifeRadio14Days;
	private String shortLifeRadio31Days;
	private String shortLifeRadio45Days;
	private String tvBroadcastDigitalMedia;
	private String digitalMediaBroadcastTv;
	private String digitalMediaOther;
	private String radioDigitalMedia;
	private String actraOnline;
	private String article2403;
	private String article2404;
	private String article2405;
	private String article2406;
	private String commercialName;
	private String docket;
	private String additionalTitles;
	private String digitalMediaFees;
	private String sessionFees;
	private String residualFees;
	private String otherFees;
	private String specialProvisions;
	private Boolean ndm;
//	private ContactDocEvent talentInitialAgree;
//	private ContactDocEvent talentInitialDisagree;
//	private ContactDocEvent producerInitial;
	private WeeklyTimecard weeklyTimecard;
	private User createdBy;
	private Date createdDate;
	private User updatedBy;
	private Date updatedDate;
	/** Collection of Offices that this contract has been sent to. */
	private Set<Office> offices;
	/** Whether to show the long-form */

	/** used for get office list saved value */
	private Office office;

	// Constructors

	/** default constructor */
	public FormActraContract() {
		super();
		offices = new HashSet<>();
		setVersion(ACTRA_CONTRACT_VERSION_2018);
	}

	/** minimal constructor */
	public FormActraContract(WeeklyTimecard weeklyTimecard, User createdBy, Date createdDate,
			User updatedBy, Date updatedDate) {
		this.weeklyTimecard = weeklyTimecard;
		this.createdBy = createdBy;
		this.createdDate = createdDate;
		this.updatedBy = updatedBy;
		this.updatedDate = updatedDate;
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DateFormat dateFmt = new SimpleDateFormat(Constants.SHORT_DATE_FORMAT_CANADA);
		DateFormat timeFmt = new SimpleDateFormat("h:mm a");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737
		User user = cd.getContact().getUser();

		fieldValues.put(FormFieldType.FULL_NAME.name(), getTalentName() );
		fieldValues.put(FormFieldType.SOCIAL_SECURITY.name(), getSocialInsuranceNum());
		fieldValues.put(FormFieldType.USER_EMAIL_ADDRESS.name(), user.getEmailAddress());

		if (getTalentAddress() != null) {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), (getTalentAddress().getAddrLine1() != null ? getTalentAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(), getTalentAddress().getCity());
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(), getTalentAddress().getState());
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(), getTalentAddress().getZip());
		}
		else {
			fieldValues.put(FormFieldType.HOME_ADDR_STREET.name(), "");
			fieldValues.put(FormFieldType.HOME_ADDR_CITY_ONLY.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_STATE.name(),  "");
			fieldValues.put(FormFieldType.HOME_ADDR_ZIP.name(),  "");
		}
		fieldValues.put(FormFieldType.TELEPHONE_NUMBER.name(), StringUtils.formatPhoneNumber(this.getTalentPhoneNum()));
		fieldValues.put(FormFieldType.ACTRA_DOB_CA.name(), dateFormat(dateFmt, this.getDob()));

		if (getProdHouseAddress() != null) {
			fieldValues.put(FormFieldType.PROD_HOUSE_ADDRESS_COMPLETE.name(),              getProdHouseAddress().getCompleteAddress() );
		}
		else {
			fieldValues.put(FormFieldType.PROD_HOUSE_ADDRESS_COMPLETE.name(),			   "" );
		}

		if (getAgencyAddress() != null) {
			fieldValues.put(FormFieldType.AGENCY_ADDRESS_COMPLETE.name(),                  getAgencyAddress().getCompleteAddress() );
		}
		else {
			fieldValues.put(FormFieldType.AGENCY_ADDRESS_COMPLETE.name(),                  "" );
		}

		fieldValues.put(FormFieldType.ACTRA_CONTRACT_NUM.name(),                           getContractNum() );

		if (getOffice() != null) {
			fieldValues.put(FormFieldType.ACTRA_BRANCH_CODE.name(), 					   getOffice().getBranchCode());
		}
		else {
			fieldValues.put(FormFieldType.ACTRA_BRANCH_CODE.name(), 					   "");
		}
		fieldValues.put(FormFieldType.AGENCY_COMPANY_NAME.name(),                          getAgencyName() );
		fieldValues.put(FormFieldType.AGENCY_PRODUCER.name(),                              getAgencyProducer() );
		fieldValues.put(FormFieldType.ADVERTISER_NAME.name(),                              getAdvertiserName() );
		fieldValues.put(FormFieldType.PRODUCT_NAME.name(),                                 getProductName() );
		fieldValues.put(FormFieldType.PROD_HOUSE_NAME.name(),                              getProdHouseName() );
		fieldValues.put(FormFieldType.PAY_SESSION_FEE.name(),                (getPaySessionFee() != null && getPaySessionFee() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.DIRECTOR_NAME.name(),                                getDirectorName() );
		fieldValues.put(FormFieldType.LOAN_OUT_CORP_NAME.name(),                           getLoanOutName() );
		fieldValues.put(FormFieldType.ACTRA_GST_HST.name(),                                getGstHst() );
		fieldValues.put(FormFieldType.ACTRA_QST.name(),                                    getQst() );
		fieldValues.put(FormFieldType.TALENT_AGENCY_NAME.name(),                           getTalentAgencyName() );
		fieldValues.put(FormFieldType.AGENCY_CONTACT.name(),                               getAgencyContact() );
		fieldValues.put(FormFieldType.PERFORMANCE_CATEGORY.name(),                         getPerformanceCategory() );
		fieldValues.put(FormFieldType.ACTRA_FULL_MEMBER_NUM.name(),                        getFullMemberNum() );
		fieldValues.put(FormFieldType.ACTRA_APPRENTICE_NUM.name(),                         getApprenticeNum() );
		fieldValues.put(FormFieldType.ACTRA_WORK_PERMIT_NUM.name(),                        getWorkPermitNum() );
		fieldValues.put(FormFieldType.ACTRA_NATIONAL_TV.name(),                            getNationalTv() );
		fieldValues.put(FormFieldType.ACTRA_NATIONAL_RADIO.name(),                         getNationalRadio() );
		fieldValues.put(FormFieldType.ACTRA_NATIONAL_DIGITAL_MEDIA_VIDEO.name(),           getNationalDigitalMediaVideo() );
		fieldValues.put(FormFieldType.ACTRA_NATIONAL_DIGITAL_MEDIA_AUDIO.name(),           getNationalDigitalMediaAudio() );
		fieldValues.put(FormFieldType.ACTRA_TAGS_TV.name(),                                intFormat(getTagsTv() ));
		fieldValues.put(FormFieldType.ACTRA_TAGS_RADIO.name(),                             intFormat(getTagsRadio() ));
		fieldValues.put(FormFieldType.ACTRA_TAGS_DIGITAL_MEDIA.name(),                     intFormat(getTagsDigitalMedia() ));
		fieldValues.put(FormFieldType.ACTRA_REGIONAL_CHANGES_TV.name(),                    intFormat(getRegionalChangesTv() ));
		fieldValues.put(FormFieldType.ACTRA_REGIONAL_CHANGES_RADIO.name(),                 intFormat(getRegionalChangesRadio() ));
		fieldValues.put(FormFieldType.ACTRA_REGIONAL_CHANGES_DIGITAL_MEDIA.name(),         intFormat(getRegionalChangesDigitalMedia() ));
		fieldValues.put(FormFieldType.ACTRA_PSA_TV.name(),                                 getPsaTv() );
		fieldValues.put(FormFieldType.ACTRA_PSA_RADIO.name(),                              getPsaRadio() );
		fieldValues.put(FormFieldType.ACTRA_PSA_DIGITAL_MEDIA.name(),                      getPsaDigitalMedia() );
		fieldValues.put(FormFieldType.ACTRA_DEMO_TV.name(),                                getDemoTv() );
		fieldValues.put(FormFieldType.ACTRA_DEMO_RADIO.name(),                             getDemoRadio() );
		fieldValues.put(FormFieldType.ACTRA_DEMO_DIGITAL.name(),                           getDemoDigital() );
		fieldValues.put(FormFieldType.ACTRA_DEMO_PRESENTATION.name(),                      getDemoPresentation() );
		fieldValues.put(FormFieldType.ACTRA_DEMO_INFOMERCIAL.name(),                       getDemoInfomercial() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_TV.name(),                            getSeasonalTv() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_RADIO.name(),                         getSeasonalRadio() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_DEALER_A.name(),                      getSeasonalDealer() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_DEALER_TV.name(),                     getSeasonalDealerTv() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_DEALER_RADIO.name(),                  getSeasonalDealerRadio() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_DOUBLE_SHOOT.name(),                  getSeasonalDoubleShoot() );
		fieldValues.put(FormFieldType.ACTRA_SEASONAL_JOINT_PROMO.name(),                   getSeasonalJointPromo() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_CATEGORY_NUM.name(),            getLocalRegionalCategoryNum() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_TV.name(),                      getLocalRegionalTv() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_RADIO.name(),                   getLocalRegionalRadio() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_DIGITAL_MEDIA_A.name(),         getLocalRegionalDigitalMedia() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_DEMO.name(),                    getLocalRegionalDemo() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_DIGITAL_MEDIA_BROADCAST.name(), getLocalRegionalDigitalMediaBroadcast() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_BROADCAST_DIGITAL_MEDIA.name(), getLocalRegionalBroadcastDigitalMedia() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_OTHER.name(),                   getLocalRegionalOther() );
		fieldValues.put(FormFieldType.ACTRA_LOCAL_REGIONAL_PILOT_PROJECT.name(),           getLocalRegionalPilotProject() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_TV_7_DAYS.name(),                   getShortLifeTv7Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_TV_14_DAYS.name(),                  getShortLifeTv14Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_TV_31_DAYS.name(),                  getShortLifeTv31Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_TV_45_DAYS.name(),                  getShortLifeTv45Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_RADIO_7_DAYS.name(),                getShortLifeRadio7Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_RADIO_14_DAYS.name(),               getShortLifeRadio14Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_RADIO_31_DAYS.name(),               getShortLifeRadio31Days() );
		fieldValues.put(FormFieldType.ACTRA_SHORT_LIFE_RADIO_45_DAYS.name(),               getShortLifeRadio45Days() );
		fieldValues.put(FormFieldType.ACTRA_TV_BROADCAST_DIGITAL_MEDIA.name(),             getTvBroadcastDigitalMedia() );
		fieldValues.put(FormFieldType.ACTRA_DIGITAL_MEDIA_BROADCAST_TV.name(),             getDigitalMediaBroadcastTv() );
		fieldValues.put(FormFieldType.ACTRA_DIGITAL_MEDIA_OTHER.name(),                    getDigitalMediaOther() );
		fieldValues.put(FormFieldType.ACTRA_RADIO_DIGITAL_MEDIA.name(),                    getRadioDigitalMedia() );
		fieldValues.put(FormFieldType.ACTRA_PILOT_PROJECT.name(),                          getActraOnline() );
		fieldValues.put(FormFieldType.ACTRA_ARTICLE_2403.name(),                           getArticle2403() );
		fieldValues.put(FormFieldType.ACTRA_ARTICLE_2404.name(),                           getArticle2404() );
		fieldValues.put(FormFieldType.ACTRA_ARTICLE_2405.name(),                           getArticle2405() );
		fieldValues.put(FormFieldType.ACTRA_ARTICLE_2406.name(),                           getArticle2406() );
		fieldValues.put(FormFieldType.COMMERCIAL_NAME.name(),                              getCommercialName() );
		fieldValues.put(FormFieldType.ACTRA_DOCKET.name(),                                 getDocket() );
		fieldValues.put(FormFieldType.ACTRA_ADDITIONAL_TITLES.name(),                      getAdditionalTitles() );
		fieldValues.put(FormFieldType.SESSION_FEES.name(),                                 getSessionFees() );
		fieldValues.put(FormFieldType.RESIDUAL_FEES.name(),                                getResidualFees() );
		fieldValues.put(FormFieldType.OTHER_FEES.name(),                                   getOtherFees() );
		fieldValues.put(FormFieldType.DIGITAL_MEDIA_FEES.name(),                           getDigitalMediaFees() );
		fieldValues.put(FormFieldType.SPECIAL_PROVISIONS.name(),                           getSpecialProvisions() );
		fieldValues.put(FormFieldType.ACTRA_SNACK_YES.name(),            (getNdm() == null ? "Off" : getNdm() ? "Yes" : "Off" ));
		fieldValues.put(FormFieldType.ACTRA_SNACK_NO.name(),             (getNdm() == null ? "Off" : getNdm() ? "Off" : "Yes" ));

		// TODO Fill any missing fields from form into XFDF map

		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), dateFormat(dateFmt, cd.getEmpSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_DATE.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), dateFormat(dateFmt, cd.getEmployerSignature().getDate()));
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
			fieldValues.put(FormFieldType.EMP_SIGNATURE_DATE.name(), "");
		}

		if (cd.getOptSignature() != null) {
			fieldValues.put(FormFieldType.SPECIAL_PROVISIONS_SIGNATURE.name(), // Note: 2-line output requires modified PDF.
					cd.getOptSignature().getSignedBy() + "\n" + cd.getOptSignature().getUuid());
		}
		else {
			fieldValues.put(FormFieldType.SPECIAL_PROVISIONS_SIGNATURE.name(), "");
		}

		if (cd.getEmpAgrees() != null) {
			fieldValues.put(FormFieldType.ACTRA_TALENT_INITIAL_AGREE.name(), cd.getEmpAgrees().getInitials());
		}
		else {
			fieldValues.put(FormFieldType.ACTRA_TALENT_INITIAL_AGREE.name(), "");
		}

		if (cd.getEmpDisagrees() != null) {
			fieldValues.put(FormFieldType.ACTRA_TALENT_INITIAL_DISAGREE.name(), cd.getEmpDisagrees().getInitials());
		}
		else {
			fieldValues.put(FormFieldType.ACTRA_TALENT_INITIAL_DISAGREE.name(), "");
		}

		if (cd.getApprovalSignature() != null) {
			fieldValues.put(FormFieldType.ACTRA_PRODUCER_INITIAL.name(), cd.getApprovalSignature().getInitials());
		}
		else {
			fieldValues.put(FormFieldType.ACTRA_PRODUCER_INITIAL.name(), "");
		}

		// Fill in work-time section
		int ix = 1;
		for (DailyTime dt : this.getWeeklyTimecard().getDailyTimes()) {
			fillTime(dt, ix++, fieldValues, timeFmt, dateFmt);
		}
	}

	private void fillTime(DailyTime dt, int ix, Map<String, String> fieldValues, DateFormat timeFmt, DateFormat dateFmt) {
		String suffix = "" + ix + "-";

		fieldValues.put("DATE-"+suffix, (dt.getDate() == null) ? "" : dateFmt.format(dt.getDate()));
		fieldValues.put("CALL-"+suffix, timeFormat(dt.getCallTime()));
		fieldValues.put("MEAL-FROM-"+suffix, timeFormat(dt.getM1Out()));
		fieldValues.put("MEAL-TO-"+suffix, timeFormat(dt.getM1In()));
		fieldValues.put("WRAP-"+suffix, timeFormat(dt.getWrap()));
		fieldValues.put("FROM1-"+suffix, timeFormat(dt.getTrvlToLocFrom()));
		fieldValues.put("TO-LOC-"+suffix, timeFormat(dt.getTrvlToLocTo()));
		fieldValues.put("FROM-LOC-"+suffix, timeFormat(dt.getTrvlFromLocFrom()));
		fieldValues.put("TO-HOME-"+suffix, timeFormat(dt.getTrvlFromLocTo()));
		fieldValues.put("TYPE"+suffix, (dt.getCallType() == null ? "" : dt.getCallType().name()));
		fieldValues.put("DATE2-"+suffix, (dt.getDate2() == null) ? "" : dateFmt.format(dt.getDate2()));
		fieldValues.put("CALL2-"+suffix, timeFormat(dt.getCallTime2()));
		fieldValues.put("WRAP2-"+suffix, timeFormat(dt.getWrap2()));

	}

	/**
	 * Convert a decimal time to printable.
	 * @param time
	 * @return "h:mm AM/PM"
	 */
	private String timeFormat(BigDecimal time) {
		// TODO this could be cleaned up, also need AM/PM support; we might have code somewhere that does this already!!
		if (time == null) {
			return "";
		}
		int hours = time.intValue();
		time = time.subtract(new BigDecimal(hours));
		int minutes = time.multiply(new BigDecimal(60)).intValue();
		String s = "0" + minutes;
		s = s.substring(s.length()-2);
		s = hours + ":" + s;
		return s;
	}

	/** See {@link #contractNum}. */
	@Column(name = "Contract_Num", length = 25)
	public String getContractNum() {
		return this.contractNum;
	}

	/** See {@link #contractNum}. */
	public void setContractNum(String contractNum) {
		this.contractNum = contractNum;
	}

	/** See {@link #agencyName}. */
	@Column(name = "Agency_Name", length = 150)
	public String getAgencyName() {
		return this.agencyName;
	}

	/** See {@link #agencyName}. */
	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	/** See {@link #agencyProducer}. */
	@Column(name = "Agency_Producer", length = 150)
	public String getAgencyProducer() {
		return this.agencyProducer;
	}

	/** See {@link #agencyProducer}. */
	public void setAgencyProducer(String agencyProducer) {
		this.agencyProducer = agencyProducer;
	}

	/** See {@link #agencyAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Agency_Address_Id")
	public Address getAgencyAddress() {
		return this.agencyAddress;
	}

	/** See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	/** See {@link #advertiserName}. */
	@Column(name = "Advertiser_Name", length = 150)
	public String getAdvertiserName() {
		return this.advertiserName;
	}

	/** See {@link #advertiserName}. */
	public void setAdvertiserName(String advertiserName) {
		this.advertiserName = advertiserName;
	}

	/** See {@link #productName}. */
	@Column(name = "Product_Name", length = 150)
	public String getProductName() {
		return this.productName;
	}

	/** See {@link #productName}. */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/** See {@link #prodHouseName}. */
	@Column(name = "Prod_House_Name", length = 150)
	public String getProdHouseName() {
		return this.prodHouseName;
	}

	/** See {@link #prodHouseName}. */
	public void setProdHouseName(String prodHouseName) {
		this.prodHouseName = prodHouseName;
	}

	/** See {@link #prodHouseAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Prod_House_Address_Id")
	public Address getProdHouseAddress() {
		return this.prodHouseAddress;
	}

	/** See {@link #prodHouseAddress}. */
	public void setProdHouseAddress(Address prodHouseAddress) {
		this.prodHouseAddress = prodHouseAddress;
	}

	/** See {@link #paySessionFee}. */
	@Column(name = "Pay_Session_Fee")
	public Boolean getPaySessionFee() {
		return this.paySessionFee;
	}

	/** See {@link #paySessionFee}. */
	public void setPaySessionFee(Boolean paySessionFee) {
		this.paySessionFee = paySessionFee;
	}

	/** See {@link #directorName}. */
	@Column(name = "Director_Name", length = 150)
	public String getDirectorName() {
		return directorName;
	}

	/** See {@link #directorName}. */
	public void setDirectorName(String directorName) {
		this.directorName = directorName;
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

	/** See {@link #talentName}. */
	@Column(name = "Talent_Name", length = 100)
	public String getTalentName() {
		return this.talentName;
	}

	/** See {@link #talentName}. */
	public void setTalentName(String talentName) {
		this.talentName = talentName;
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

	/** See {@link #talentEmailAddress}. */
	@Column(name = "Talent_Email_Address", length = 100)
	public String getTalentEmailAddress() {
		return this.talentEmailAddress;
	}

	/** See {@link #talentEmailAddress}. */
	public void setTalentEmailAddress(String talentEmailAddress) {
		this.talentEmailAddress = talentEmailAddress;
	}

	/** See {@link #talentPhoneNum}. */
	@Column(name = "Talent_Phone_Num", length = 15)
	public String getTalentPhoneNum() {
		return this.talentPhoneNum;
	}

	/** See {@link #talentPhoneNum}. */
	public void setTalentPhoneNum(String talentPhoneNum) {
		this.talentPhoneNum = talentPhoneNum;
	}

	/** See {@link #socialInsuranceNum}. */
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Insurance_Num", length = 1000)
	public String getSocialInsuranceNum() {
		return this.socialInsuranceNum;
	}

	/** See {@link #socialInsuranceNum}. */
	public void setSocialInsuranceNum(String socialInsuranceNum) {
		this.socialInsuranceNum = socialInsuranceNum;
	}

	/** See {@link #gstHst}. */
	@Column(name = "Gst_Hst", length = 20)
	public String getGstHst() {
		return this.gstHst;
	}

	/** See {@link #gstHst}. */
	public void setGstHst(String gstHst) {
		this.gstHst = gstHst;
	}

	/** See {@link #qst}. */
	@Column(name = "Qst", length = 20)
	public String getQst() {
		return this.qst;
	}

	/** See {@link #qst}. */
	public void setQst(String qst) {
		this.qst = qst;
	}

	/** See {@link #dob}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "DOB", length = 0)
	public Date getDob() {
		return this.dob;
	}

	/** See {@link #dob}. */
	public void setDob(Date dob) {
		this.dob = dob;
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

	/** See {@link #agencyContact}. */
	@Column(name = "Agency_Contact", length = 15)
	public String getAgencyContact() {
		return agencyContact;
	}
	/** See {@link #agencyContact}. */
	public void setAgencyContact(String agencyContact) {
		this.agencyContact = agencyContact;
	}

	/** See {@link #performanceCategory}. */
	@Column(name = "Performance_Category", length = 50)
	public String getPerformanceCategory() {
		return this.performanceCategory;
	}

	/** See {@link #performanceCategory}. */
	public void setPerformanceCategory(String performanceCategory) {
		this.performanceCategory = performanceCategory;
	}

	/** See {@link #fullMemberNum}. */
	@Column(name = "Full_Member_Num", length = 15)
	public String getFullMemberNum() {
		return this.fullMemberNum;
	}

	/** See {@link #fullMemberNum}. */
	public void setFullMemberNum(String fullMemberNum) {
		this.fullMemberNum = fullMemberNum;
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

	/** See {@link #workPermitNum}. */
	@Column(name = "Work_Permit_Num", length = 25)
	public String getWorkPermitNum() {
		return this.workPermitNum;
	}

	/** See {@link #workPermitNum}. */
	public void setWorkPermitNum(String workPermitNum) {
		this.workPermitNum = workPermitNum;
	}

	/** See {@link #nationalTv}. */
	@Column(name = "National_Tv", length = 15)
	public String getNationalTv() {
		return this.nationalTv;
	}

	/** See {@link #nationalTv}. */
	public void setNationalTv(String nationalTv) {
		this.nationalTv = nationalTv;
	}

	/** See {@link #nationalRadio}. */
	@Column(name = "National_Radio", length = 15)
	public String getNationalRadio() {
		return this.nationalRadio;
	}

	/** See {@link #nationalRadio}. */
	public void setNationalRadio(String nationalRadio) {
		this.nationalRadio = nationalRadio;
	}

	/** See {@link #nationalDigitalMediaVideo}. */
	@Column(name = "National_Digital_Media_Video", length = 15)
	public String getNationalDigitalMediaVideo() {
		return this.nationalDigitalMediaVideo;
	}

	/** See {@link #nationalDigitalMediaVideo}. */
	public void setNationalDigitalMediaVideo(String nationalDigitalMediaVideo) {
		this.nationalDigitalMediaVideo = nationalDigitalMediaVideo;
	}

	/** See {@link #nationalDigitalMediaAudio}. */
	@Column(name = "National_Digital_Media_Audio", length = 15)
	public String getNationalDigitalMediaAudio() {
		return this.nationalDigitalMediaAudio;
	}

	/** See {@link #nationalDigitalMediaAudio}. */
	public void setNationalDigitalMediaAudio(String nationalDigitalMediaRadio) {
		this.nationalDigitalMediaAudio = nationalDigitalMediaRadio;
	}

	/** See {@link #tagsTv}. */
	@Column(name = "Tags_Tv")
	public Integer getTagsTv() {
		return this.tagsTv;
	}

	/** See {@link #tagsTv}. */
	public void setTagsTv(Integer tagsTv) {
		this.tagsTv = tagsTv;
	}

	/** See {@link #tagsRadio}. */
	@Column(name = "Tags_Radio")
	public Integer getTagsRadio() {
		return this.tagsRadio;
	}

	/** See {@link #tagsRadio}. */
	public void setTagsRadio(Integer tagsRadio) {
		this.tagsRadio = tagsRadio;
	}

	/** See {@link #tagsDigitalMedia}. */
	@Column(name = "Tags_Digital_Media")
	public Integer getTagsDigitalMedia() {
		return this.tagsDigitalMedia;
	}

	/** See {@link #tagsDigitalMedia}. */
	public void setTagsDigitalMedia(Integer tagsDigitalMedia) {
		this.tagsDigitalMedia = tagsDigitalMedia;
	}

	/** See {@link #regionalChangesTv}. */
	@Column(name = "Regional_Changes_Tv")
	public Integer getRegionalChangesTv() {
		return this.regionalChangesTv;
	}

	/** See {@link #regionalChangesTv}. */
	public void setRegionalChangesTv(Integer regionalChangesTv) {
		this.regionalChangesTv = regionalChangesTv;
	}

	/** See {@link #regionalChangesRadio}. */
	@Column(name = "Regional_Changes_Radio")
	public Integer getRegionalChangesRadio() {
		return this.regionalChangesRadio;
	}

	/** See {@link #regionalChangesRadio}. */
	public void setRegionalChangesRadio(Integer regionalChangesRadio) {
		this.regionalChangesRadio = regionalChangesRadio;
	}

	/** See {@link #regionalChangesDigitalMedia}. */
	@Column(name = "Regional_Changes_Digital_Media")
	public Integer getRegionalChangesDigitalMedia() {
		return this.regionalChangesDigitalMedia;
	}

	/** See {@link #regionalChangesDigitalMedia}. */
	public void setRegionalChangesDigitalMedia(Integer regionalChangesDigitalMedia) {
		this.regionalChangesDigitalMedia = regionalChangesDigitalMedia;
	}

	/** See {@link #psaTv}. */
	@Column(name = "Psa_Tv", length = 15)
	public String getPsaTv() {
		return this.psaTv;
	}

	/** See {@link #psaTv}. */
	public void setPsaTv(String psaTv) {
		this.psaTv = psaTv;
	}

	/** See {@link #psaRadio}. */
	@Column(name = "Psa_Radio", length = 15)
	public String getPsaRadio() {
		return this.psaRadio;
	}

	/** See {@link #psaRadio}. */
	public void setPsaRadio(String psaRadio) {
		this.psaRadio = psaRadio;
	}

	/** See {@link #psaDigitalMedia}. */
	@Column(name = "Psa_Digital_Media", length = 15)
	public String getPsaDigitalMedia() {
		return this.psaDigitalMedia;
	}

	/** See {@link #psaDigitalMedia}. */
	public void setPsaDigitalMedia(String psaDigitalMedia) {
		this.psaDigitalMedia = psaDigitalMedia;
	}

	/** See {@link #demoTv}. */
	@Column(name = "Demo_Tv", length = 15)
	public String getDemoTv() {
		return this.demoTv;
	}

	/** See {@link #demoTv}. */
	public void setDemoTv(String demoTv) {
		this.demoTv = demoTv;
	}

	/** See {@link #demoRadio}. */
	@Column(name = "Demo_Radio", length = 15)
	public String getDemoRadio() {
		return this.demoRadio;
	}

	/** See {@link #demoRadio}. */
	public void setDemoRadio(String demoRadio) {
		this.demoRadio = demoRadio;
	}

	/** See {@link #demoDigital}. */
	@Column(name = "Demo_Digital", length = 15)
	public String getDemoDigital() {
		return demoDigital;
	}
	/** See {@link #demoDigital}. */
	public void setDemoDigital(String demoDigital) {
		this.demoDigital = demoDigital;
	}

	/** See {@link #demoPresentation}. */
	@Column(name = "Demo_Presentation", length = 15)
	public String getDemoPresentation() {
		return this.demoPresentation;
	}

	/** See {@link #demoPresentation}. */
	public void setDemoPresentation(String demoPresentation) {
		this.demoPresentation = demoPresentation;
	}

	/** See {@link #demoInfomercial}. */
	@Column(name = "Demo_Infomercial", length = 15)
	public String getDemoInfomercial() {
		return this.demoInfomercial;
	}

	/** See {@link #demoInfomercial}. */
	public void setDemoInfomercial(String demoInfomercial) {
		this.demoInfomercial = demoInfomercial;
	}

	/** See {@link #seasonalTv}. */
	@Column(name = "Seasonal_Tv", length = 15)
	public String getSeasonalTv() {
		return this.seasonalTv;
	}

	/** See {@link #seasonalTv}. */
	public void setSeasonalTv(String seasonalTv) {
		this.seasonalTv = seasonalTv;
	}

	/** See {@link #seasonalRadio}. */
	@Column(name = "Seasonal_Radio", length = 15)
	public String getSeasonalRadio() {
		return this.seasonalRadio;
	}

	/** See {@link #seasonalRadio}. */
	public void setSeasonalRadio(String seasonalRadio) {
		this.seasonalRadio = seasonalRadio;
	}

	/** See {@link #seasonalDealer}. */
	@Column(name = "Seasonal_Dealer", length = 15)
	public String getSeasonalDealer() {
		return this.seasonalDealer;
	}

	/** See {@link #seasonalDealer}. */
	public void setSeasonalDealer(String seasonalDealer) {
		this.seasonalDealer = seasonalDealer;
	}

	/** See {@link #seasonalDealerTv}. */
	@Column(name = "Seasonal_Dealer_Tv", length = 15)
	public String getSeasonalDealerTv() {
		return this.seasonalDealerTv;
	}

	/** See {@link #seasonalDealerTv}. */
	public void setSeasonalDealerTv(String seasonalDealerTv) {
		this.seasonalDealerTv = seasonalDealerTv;
	}

	/** See {@link #seasonalDealerRadio}. */
	@Column(name = "Seasonal_Dealer_Radio", length = 15)
	public String getSeasonalDealerRadio() {
		return this.seasonalDealerRadio;
	}

	/** See {@link #seasonalDealerRadio}. */
	public void setSeasonalDealerRadio(String seasonalDealerRadio) {
		this.seasonalDealerRadio = seasonalDealerRadio;
	}

	/** See {@link #seasonalDoubleShoot}. */
	@Column(name = "Seasonal_Double_Shoot", length = 15)
	public String getSeasonalDoubleShoot() {
		return this.seasonalDoubleShoot;
	}

	/** See {@link #seasonalDoubleShoot}. */
	public void setSeasonalDoubleShoot(String seasonalDoubleShoot) {
		this.seasonalDoubleShoot = seasonalDoubleShoot;
	}

	/** See {@link #seasonalJointPromo}. */
	@Column(name = "Seasonal_Joint_Promo", length = 15)
	public String getSeasonalJointPromo() {
		return this.seasonalJointPromo;
	}

	/** See {@link #seasonalJointPromo}. */
	public void setSeasonalJointPromo(String seasonalJointPromo) {
		this.seasonalJointPromo = seasonalJointPromo;
	}

	/** See {@link #localRegionalCategoryNum}. */
	@Column(name = "Local_Regional_Category_Num", length = 15)
	public String getLocalRegionalCategoryNum() {
		return this.localRegionalCategoryNum;
	}

	/** See {@link #localRegionalCategoryNum}. */
	public void setLocalRegionalCategoryNum(String localRegionalCategoryNum) {
		this.localRegionalCategoryNum = localRegionalCategoryNum;
	}

	/** See {@link #localRegionalTv}. */
	@Column(name = "Local_Regional_Tv", length = 15)
	public String getLocalRegionalTv() {
		return this.localRegionalTv;
	}

	/** See {@link #localRegionalTv}. */
	public void setLocalRegionalTv(String localRegionalTv) {
		this.localRegionalTv = localRegionalTv;
	}

	/** See {@link #localRegionalRadio}. */
	@Column(name = "Local_Regional_Radio", length = 15)
	public String getLocalRegionalRadio() {
		return this.localRegionalRadio;
	}

	/** See {@link #localRegionalRadio}. */
	public void setLocalRegionalRadio(String localRegionalRadio) {
		this.localRegionalRadio = localRegionalRadio;
	}

	/** See {@link #localRegionalDigitalMedia}. */
	@Column(name = "Local_Regional_Digital_Media", length = 15)
	public String getLocalRegionalDigitalMedia() {
		return this.localRegionalDigitalMedia;
	}

	/** See {@link #localRegionalDigitalMedia}. */
	public void setLocalRegionalDigitalMedia(String localRegionalDigitalMedia) {
		this.localRegionalDigitalMedia = localRegionalDigitalMedia;
	}

	/** See {@link #localRegionalDemo}. */
	@Column(name = "Local_Regional_Demo", length = 15)
	public String getLocalRegionalDemo() {
		return this.localRegionalDemo;
	}

	/** See {@link #localRegionalDemo}. */
	public void setLocalRegionalDemo(String localRegionalDemo) {
		this.localRegionalDemo = localRegionalDemo;
	}

	/** See {@link #localRegionalDigitalMediaBroadcast}. */
	@Column(name = "Local_Regional_Digital_Media_Broadcast", length = 15)
	public String getLocalRegionalDigitalMediaBroadcast() {
		return this.localRegionalDigitalMediaBroadcast;
	}

	/** See {@link #localRegionalDigitalMediaBroadcast}. */
	public void setLocalRegionalDigitalMediaBroadcast(String localRegionalDigitalMediaBroadcast) {
		this.localRegionalDigitalMediaBroadcast = localRegionalDigitalMediaBroadcast;
	}

	/** See {@link #localRegionalBroadcastDigitalMedia}. */
	@Column(name = "Local_Regional_Broadcast_Digital_Media", length = 15)
	public String getLocalRegionalBroadcastDigitalMedia() {
		return this.localRegionalBroadcastDigitalMedia;
	}

	/** See {@link #localRegionalBroadcastDigitalMedia}. */
	public void setLocalRegionalBroadcastDigitalMedia(String localRegionalBroadcastDigitalMedia) {
		this.localRegionalBroadcastDigitalMedia = localRegionalBroadcastDigitalMedia;
	}

	/** See {@link #localRegionalOther}. */
	@Column(name = "Local_Regional_Other", length = 15)
	public String getLocalRegionalOther() {
		return this.localRegionalOther;
	}

	/** See {@link #localRegionalOther}. */
	public void setLocalRegionalOther(String localRegionalOther) {
		this.localRegionalOther = localRegionalOther;
	}

	/** See {@link #localRegionalPilotProject}. */
	@Column(name = "Local_Regional_Pilot_Project", length = 15)
	public String getLocalRegionalPilotProject() {
		return localRegionalPilotProject;
	}

	/** See {@link #localRegionalPilotProject}. */
	public void setLocalRegionalPilotProject(String localRegionalPilotProject) {
		this.localRegionalPilotProject = localRegionalPilotProject;
	}

	/** See {@link #shortLifeTv7Days}. */
	@Column(name = "Short_Life_Tv_7_Days", length = 15)
	public String getShortLifeTv7Days() {
		return this.shortLifeTv7Days;
	}

	/** See {@link #shortLifeTv7Days}. */
	public void setShortLifeTv7Days(String shortLifeTv7Days) {
		this.shortLifeTv7Days = shortLifeTv7Days;
	}

	/** See {@link #shortLifeTv14Days}. */
	@Column(name = "Short_Life_Tv_14_Days", length = 15)
	public String getShortLifeTv14Days() {
		return this.shortLifeTv14Days;
	}

	/** See {@link #shortLifeTv14Days}. */
	public void setShortLifeTv14Days(String shortLifeTv14Days) {
		this.shortLifeTv14Days = shortLifeTv14Days;
	}

	/** See {@link #shortLifeTv31Days}. */
	@Column(name = "Short_Life_Tv_31_Days", length = 15)
	public String getShortLifeTv31Days() {
		return this.shortLifeTv31Days;
	}

	/** See {@link #shortLifeTv31Days}. */
	public void setShortLifeTv31Days(String shortLifeTv31Days) {
		this.shortLifeTv31Days = shortLifeTv31Days;
	}

	/** See {@link #shortLifeTv45Days}. */
	@Column(name = "Short_Life_Tv_45_Days", length = 15)
	public String getShortLifeTv45Days() {
		return this.shortLifeTv45Days;
	}

	/** See {@link #shortLifeTv45Days}. */
	public void setShortLifeTv45Days(String shortLifeTv45Days) {
		this.shortLifeTv45Days = shortLifeTv45Days;
	}

	/** See {@link #shortLifeRadio7Days}. */
	@Column(name = "Short_Life_Radio_7_Days", length = 15)
	public String getShortLifeRadio7Days() {
		return this.shortLifeRadio7Days;
	}

	/** See {@link #shortLifeRadio7Days}. */
	public void setShortLifeRadio7Days(String shortLifeRadio7Days) {
		this.shortLifeRadio7Days = shortLifeRadio7Days;
	}

	/** See {@link #shortLifeRadio14Days}. */
	@Column(name = "Short_Life_Radio_14_Days", length = 15)
	public String getShortLifeRadio14Days() {
		return this.shortLifeRadio14Days;
	}

	/** See {@link #shortLifeRadio14Days}. */
	public void setShortLifeRadio14Days(String shortLifeRadio14Days) {
		this.shortLifeRadio14Days = shortLifeRadio14Days;
	}

	/** See {@link #shortLifeRadio31Days}. */
	@Column(name = "Short_Life_Radio_31_Days", length = 15)
	public String getShortLifeRadio31Days() {
		return this.shortLifeRadio31Days;
	}

	/** See {@link #shortLifeRadio31Days}. */
	public void setShortLifeRadio31Days(String shortLifeRadio31Days) {
		this.shortLifeRadio31Days = shortLifeRadio31Days;
	}

	/** See {@link #shortLifeRadio45Days}. */
	@Column(name = "Short_Life_Radio_45_Days", length = 15)
	public String getShortLifeRadio45Days() {
		return this.shortLifeRadio45Days;
	}

	/** See {@link #shortLifeRadio45Days}. */
	public void setShortLifeRadio45Days(String shortLifeRadio45Days) {
		this.shortLifeRadio45Days = shortLifeRadio45Days;
	}

	/** See {@link #tvBroadcastDigitalMedia}. */
	@Column(name = "Tv_Broadcast_Digital_Media", length = 15)
	public String getTvBroadcastDigitalMedia() {
		return this.tvBroadcastDigitalMedia;
	}

	/** See {@link #tvBroadcastNewMedia}. */
	public void setTvBroadcastDigitalMedia(String tvBroadcastDigitalMedia) {
		this.tvBroadcastDigitalMedia = tvBroadcastDigitalMedia;
	}

	/** See {@link digitalMediaBroadcastTv}. */
	@Column(name = "Digital_Media_Broadcast_Tv", length = 15)
	public String getDigitalMediaBroadcastTv() {
		return this.digitalMediaBroadcastTv;
	}

	/** See {@link #newMediaBroadcastTv}. */
	public void setDigitalMediaBroadcastTv(String digitalMediaBroadcastTv) {
		this.digitalMediaBroadcastTv = digitalMediaBroadcastTv;
	}

	/** See {@link #digitalMediaOther}. */
	@Column(name = "Digital_Media_Other", length = 15)
	public String getDigitalMediaOther() {
		return this.digitalMediaOther;
	}
	/** See {@link #digitalMediaOther}. */
	public void setDigitalMediaOther(String digitalMediaOther) {
		this.digitalMediaOther = digitalMediaOther;
	}

	/** See {@link #radioDigitalMedia}. */
	@Column(name = "Radio_Digital_Media", length = 15)
	public String getRadioDigitalMedia() {
		return radioDigitalMedia;
	}
	/** See {@link #radioDigitalMedia}. */
	public void setRadioDigitalMedia(String radioDigitalMedia) {
		this.radioDigitalMedia = radioDigitalMedia;
	}

	/** See {@link #actraOnline}. */
	@Column(name = "Actra_Online", length = 15)
	public String getActraOnline() {
		return actraOnline;
	}
	/** See {@link #actraOnline}. */
	public void setActraOnline(String actraOnline) {
		this.actraOnline = actraOnline;
	}

	/** See {@link #article2403}. */
	@Column(name = "Article_2403", length = 15)
	public String getArticle2403() {
		return this.article2403;
	}

	/** See {@link #article2403}. */
	public void setArticle2403(String article2403) {
		this.article2403 = article2403;
	}

	/** See {@link #article2404}. */
	@Column(name = "Article_2404", length = 15)
	public String getArticle2404() {
		return this.article2404;
	}

	/** See {@link #article2404}. */
	public void setArticle2404(String article2404) {
		this.article2404 = article2404;
	}

	/** See {@link #article2405}. */
	@Column(name = "Article_2405", length = 15)
	public String getArticle2405() {
		return this.article2405;
	}

	/** See {@link #article2405}. */
	public void setArticle2405(String article2405) {
		this.article2405 = article2405;
	}

	/** See {@link #article2406}. */
	@Column(name = "Article_2406", length = 15)
	public String getArticle2406() {
		return this.article2406;
	}

	/** See {@link #article2406}. */
	public void setArticle2406(String article2406) {
		this.article2406 = article2406;
	}

	/** See {@link #commercialName}. */
	@Column(name = "Commercial_Name", length = 250)
	public String getCommercialName() {
		return this.commercialName;
	}

	/** See {@link #commercialName}. */
	public void setCommercialName(String commercialName) {
		this.commercialName = commercialName;
	}

	/** See {@link #docket}. */
	@Column(name = "Docket", length = 50)
	public String getDocket() {
		return this.docket;
	}

	/** See {@link #docket}. */
	public void setDocket(String docket) {
		this.docket = docket;
	}

	/** See {@link #additionalTitles}. */
	@Column(name = "Additional_Titles", length = 250)
	public String getAdditionalTitles() {
		return this.additionalTitles;
	}

	/** See {@link #additionalTitles}. */
	public void setAdditionalTitles(String additionalTitles) {
		this.additionalTitles = additionalTitles;
	}

	/** See {@link #sessionFees}. */
	@Column(name = "Session_Fees", length = 100)
	public String getSessionFees() {
		return this.sessionFees;
	}

	/** See {@link #sessionFees}. */
	public void setSessionFees(String sessionFees) {
		this.sessionFees = sessionFees;
	}

	/** See {@link #residualFees}. */
	@Column(name = "Residual_Fees", length = 100)
	public String getResidualFees() {
		return this.residualFees;
	}

	/** See {@link #residualFees}. */
	public void setResidualFees(String residualFees) {
		this.residualFees = residualFees;
	}

	/** See {@link #otherFees}. */
	@Column(name = "Other_Fees", length = 100)
	public String getOtherFees() {
		return this.otherFees;
	}

	/** See {@link #otherFees}. */
	public void setOtherFees(String otherFees) {
		this.otherFees = otherFees;
	}

	/** See {@link #specialProvisions}. */
	@Column(name = "special_provisions", length = 1000)
	public String getSpecialProvisions() {
		return specialProvisions;
	}
	/** See {@link #specialProvisions}. */
	public void setSpecialProvisions(String specialProvisions) {
		this.specialProvisions = specialProvisions;
	}

	/** See {@link #digitalMediaFees}. */
	@Column(name = "Digital_Media_Fees", length = 100)
	public String getDigitalMediaFees() {
		return digitalMediaFees;
	}

	/** See {@link #digitalMediaFees}. */
	public void setDigitalMediaFees(String digitalMediaFees) {
		this.digitalMediaFees = digitalMediaFees;
	}

	/** See {@link #ndm}. */
	@Column(name = "NDM", nullable = true)
	public Boolean getNdm() {
		return this.ndm;
	}
	/** See {@link #ndm}. */
	public void setNdm(Boolean ndm) {
		this.ndm = ndm;
	}

//	/** See {@link #talentInitialAgree}. */
//	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//	@JoinColumn(name = "Talent_Initial_Agree_Id")
//	public ContactDocEvent getTalentInitialAgree() {
//		return this.talentInitialAgree;
//	}
//
//	/** See {@link #talentInitialAgree}. */
//	public void setTalentInitialAgree(ContactDocEvent talentInitialAgree) {
//		this.talentInitialAgree = talentInitialAgree;
//	}
//
//	/** See {@link #talentInitialDisagree}. */
//	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//	@JoinColumn(name = "Talent_Initial_Disagree_Id")
//	public ContactDocEvent getTalentInitialDisagree() {
//		return this.talentInitialDisagree;
//	}
//
//	/** See {@link #talentInitialDisagree}. */
//	public void setTalentInitialDisagree(ContactDocEvent talentInitialDisagree) {
//		this.talentInitialDisagree = talentInitialDisagree;
//	}
//
//	/** See {@link #producerInitial}. */
//	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//	@JoinColumn(name = "Producer_Initial_Id")
//	public ContactDocEvent getProducerInitial() {
//		return this.producerInitial;
//	}
//	/** See {@link #producerInitial}. */
//	public void setProducerInitial(ContactDocEvent producerInitial) {
//		this.producerInitial = producerInitial;
//	}

	/** See {@link #weeklyTimecard}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Weekly_Timecard_Id", nullable = true)
	public WeeklyTimecard getWeeklyTimecard() {
		return this.weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #createdBy}. */
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "Created_By_Id", nullable = false)
	public User getCreatedBy() {
		return this.createdBy;
	}
	/** See {@link #createdBy}. */
	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	/** See {@link #createdDate}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created_Date", nullable = false, length = 0)
	public Date getCreatedDate() {
		return this.createdDate;
	}
	/** See {@link #createdDate}. */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	/** See {@link #updatedBy}. */
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "Updated_By_Id", nullable = false)
	public User getUpdatedBy() {
		return this.updatedBy;
	}
	/** See {@link #updatedBy}. */
	public void setUpdatedBy(User updatedBy) {
		this.updatedBy = updatedBy;
	}

	/** See {@link #updatedDate}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated_Date", nullable = false, length = 0)
	public Date getUpdatedDate() {
		return this.updatedDate;
	}
	/** See {@link #updatedDate}. */
	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

	@ManyToMany(
			targetEntity=Office.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "actra_contract_office",
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

	@Transient
	public boolean hasMoreInfo() {
		if ((getSpecialProvisions() != null && (! getSpecialProvisions().isEmpty())) ||
				(getWeeklyTimecard() != null && getWeeklyTimecard().getDailyTimes() != null &&
				getWeeklyTimecard().getDailyTimes().get(4).getDate() != null)) {
			return true;
		}
		return false;
	}

	@Override
	public void trim() {
		agencyAddress.trimIsEmpty();
		prodHouseAddress.trimIsEmpty();
		talentAddress.trimIsEmpty();
		contractNum = trim(contractNum);
		//branchCode = trim(office.getBranchCode());
		agencyName = trim(agencyName);
		agencyProducer = trim(agencyProducer);
		advertiserName = trim(advertiserName);
		productName = trim(productName);
		prodHouseName = trim(prodHouseName);
		loanOutName = trim(loanOutName);
		talentName = trim(talentName);
		talentEmailAddress = trim(talentEmailAddress);
		talentPhoneNum = trim(talentPhoneNum);
		socialInsuranceNum = trim(socialInsuranceNum);
		gstHst = trim(gstHst);
		qst = trim(qst);
		talentAgencyName = trim(talentAgencyName);
		performanceCategory = trim(performanceCategory);
		fullMemberNum = trim(fullMemberNum);
		apprenticeNum = trim(apprenticeNum);
		workPermitNum = trim(workPermitNum);
		nationalTv = trim(nationalTv);
		nationalRadio = trim(nationalRadio);
		nationalDigitalMediaVideo = trim(nationalDigitalMediaVideo);
		nationalDigitalMediaAudio = trim(nationalDigitalMediaAudio);
		psaTv = trim(psaTv);
		psaRadio = trim(psaRadio);
		psaDigitalMedia = trim(psaDigitalMedia);
		demoTv = trim(demoTv);
		demoRadio = trim(demoRadio);
		demoPresentation = trim(demoPresentation);
		demoInfomercial = trim(demoInfomercial);
		seasonalTv = trim(seasonalTv);
		seasonalRadio = trim(seasonalRadio);
		seasonalDealer = trim(seasonalDealer);
		seasonalDealerTv = trim(seasonalDealerTv);
		seasonalDealerRadio = trim(seasonalDealerRadio);
		seasonalDoubleShoot = trim(seasonalDoubleShoot);
		seasonalJointPromo = trim(seasonalJointPromo);
		localRegionalCategoryNum = trim(localRegionalCategoryNum);
		localRegionalTv = trim(localRegionalTv);
		localRegionalRadio = trim(localRegionalRadio);
		localRegionalDigitalMedia = trim(localRegionalDigitalMedia);
		localRegionalDemo = trim(localRegionalDemo);
		localRegionalDigitalMediaBroadcast = trim(localRegionalDigitalMediaBroadcast);
		localRegionalBroadcastDigitalMedia = trim(localRegionalBroadcastDigitalMedia);
		localRegionalOther = trim(localRegionalOther);
		shortLifeTv7Days = trim(shortLifeTv7Days);
		shortLifeTv14Days = trim(shortLifeTv14Days);
		shortLifeTv31Days = trim(shortLifeTv31Days);
		shortLifeTv45Days = trim(shortLifeTv45Days);
		shortLifeRadio7Days = trim(shortLifeRadio7Days);
		shortLifeRadio14Days = trim(shortLifeRadio14Days);
		shortLifeRadio31Days = trim(shortLifeRadio31Days);
		shortLifeRadio45Days = trim(shortLifeRadio45Days);
		tvBroadcastDigitalMedia = trim(tvBroadcastDigitalMedia);
		digitalMediaBroadcastTv = trim(digitalMediaBroadcastTv);
		digitalMediaOther = trim(digitalMediaOther);
		article2403 = trim(article2403);
		article2404 = trim(article2404);
		article2405 = trim(article2405);
		article2406 = trim(article2406);
		commercialName = trim(commercialName);
		docket = trim(docket);
		additionalTitles = trim(additionalTitles);
		sessionFees = trim(sessionFees);
		residualFees = trim(residualFees);
		otherFees = trim(otherFees);
		digitalMediaFees = trim(digitalMediaFees);
	}
}
