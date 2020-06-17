package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * FormA2Contract entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "form_a2_contract")

public class FormA2Contract extends Form<FormA2Contract> {
	private static final long serialVersionUID = 7474337414074568939L;

	// Fields

	/** Wardrobe provided by Producer or Performer */
	private String wardrobeProvidedBy;
	/** Special instructions that are not an option on the rest of the contract */
	private String specialProvisions;
	/** Special Provisions signature event */
	private Integer specialProvisionsSignId;
	/** The Person signing for the Minor: Mother, Father, Guardian */
	private String minorConsentSigner;
	/** Minor Signer signature event */
	private Integer minorConsentSignId;
	/** Determines how the performer will be paid. */
	private String compensation;
	/** Name of performer as it appears on the contract */
	private String talentName;
	/** Performer's phone # */
	private String talentPhone;
	/** Performer's email address */
	private String talentEmailAddress;
	/** Performer's primary address */
	private Address talentPrimaryAddress;
	/** The name of the person if so designated by the performer to receive their check */
	private String talentCOName;
	/** The address of the person if so designated by the performer to receive their check */
	private Address talentCOAddress;
	/** Whether the performers payment goes to them or a designated person. */
	private String talentPayTo;
	/** Performer signature event */
	private Integer talentSignId;
	/** Date the contract was created */
	private Date contractCreationDate;
	/** Used by client for tracking. */
	private String estimateNum;
	/** Used by client for tracking. */
	private String jobNum;
	/** Name of the advertising agency */
	private String agencyName;
	/** Address of the advertising agency */
	private Address agencyAddress;
	/** Email address of the advertising agency */
	private String agencyEmailAddress;
	/** Name of the producer creating the contract */
	private String producerName;
	/** Name of the producer or representative signing the contract */
	private String producerSignName;
	/** Event of producer or representative signing the contract */
	private Integer producerSignId;
	/** Name of the production company making the commercial. Could also be the Advertising agency. */
	private String prodCompanyName;
	/** Name of the advertiser who this commercial is being made for. */
	private String advertiserName;
	/** The advertiser's product(s) represented in the commercial */
	private String advertiserProducts;
	/** Number of non-evening wear used in commercial. */
	private Integer nonEveningWearCnt;
	/** Number of evening wear used in commercial. */
	private Integer eveningWearCnt;
	/** Total wardrobe fee if wear providied by performer */
	private Double totalWardrobeFee;
	/** Flight Insurance paid on behalf of the performer. */
	private Integer flightInsuranceDays;
	/** Place where the commercial is being shot. */
	private String placeEngagement;
	/** When the commercial is being shot. */
	private String dateHourEngagement;
	/** Address of where the commercial is being shot. */
	private Address servicesRenderedAddress;
	/** Part played by the performer in the associated commercial. */
	private String partPlayed;
	/** Unlimited usage of the commercial */
	private Boolean useUnlimited;
	/** Whether the usage has to be renewed every 13 weeks. */
	private Boolean use13Weeks;
	/** Whether to use for cable only */
	private Boolean useCableOnly;
	/** Whether to use for Internet only */
	private Boolean useInternetOnly;
	/** Whether to use for New Media only */
	private Boolean useNewMediaOnly;
	/** Whether to use for Social Media only */
	private Boolean useSocialMediaOnly;
	/** Name of the performer's talent agency */
	private String talentAgencyName;
	/** Percentage of performers pay to use for his/her agent*/
	private Double agentCommission;
	/** Number of days the performer if working within studio zone. */
	private Integer travelWithinZoneDays;
	/** Whether performer is being paid for the usage of their moped. */
	private Boolean usingMoped;
	/** Type of moped being used. */
	private String mopedType;
	/** Any tolls paid by the performer. */
	private Double mopedTollsAmt;
	/** Amount to be paid to the performer based on current mileage charge */
	private Double mopedMileageAmt;
	/** Amount paid by Performer for parking. */
	private Double mopedParkingAmt;
	/** Whether performer is being paid for the usage of their car/motorcycle. */
	private Boolean usingAuto;
	/** Type of car/motorcycle being used. */
	private String autoType;
	/** Any tolls paid by the performer. */
	private Double autoTollsAmt;
	/** Amount to be paid to the performer based on current mileage charge */
	private Double autoMileageAmt;
	/** Amount paid by Performer for parking. */
	private Double autoParkingAmt;

	// Constructors

	/** default constructor */
	public FormA2Contract() {
	}

	/** See {@link #wardrobeProvidedBy}. */
	@Column(name = "Wardrobe_Provided_By", length = 30)
	public String getWardrobeProvidedBy() {
		return this.wardrobeProvidedBy;
	}

	/** See {@link #wardrobeProvidedBy}. */
	public void setWardrobeProvidedBy(String wardrobeProvidedBy) {
		this.wardrobeProvidedBy = wardrobeProvidedBy;
	}

	/** See {@link #specialProvisions}. */
	@Column(name = "Special_Provisions", length = 1000)
	public String getSpecialProvisions() {
		return this.specialProvisions;
	}

	/** See {@link #specialProvisions}. */
	public void setSpecialProvisions(String specialProvisions) {
		this.specialProvisions = specialProvisions;
	}

	/** See {@link #specialProvisionsSign}. */
	@Column(name = "Special_Provisions_Sign_Id")
	public Integer getSpecialProvisionsSignId() {
		return this.specialProvisionsSignId;
	}

	/** See {@link #specialProvisionsSign}. */
	public void setSpecialProvisionsSignId(Integer specialProvisionsSignId) {
		this.specialProvisionsSignId = specialProvisionsSignId;
	}

	/** See {@link #minorConsentSigner}. */
	@Column(name = "Minor_Consent_Signer", length = 30)
	public String getMinorConsentSigner() {
		return this.minorConsentSigner;
	}

	/** See {@link #minorConsentSigner}. */
	public void setMinorConsentSigner(String minorConsentSigner) {
		this.minorConsentSigner = minorConsentSigner;
	}

	/** See {@link #minorConsentSign}. */
	@Column(name = "Minor_Consent_Sign_Id")
	public Integer getMinorConsentSignId() {
		return this.minorConsentSignId;
	}

	/** See {@link #minorConsentSign}. */
	public void setMinorConsentSignId(Integer minorConsentSignId) {
		this.minorConsentSignId = minorConsentSignId;
	}

	/** See {@link #compensation}. */
	@Column(name = "Compensation", length = 25)
	public String getCompensation() {
		return this.compensation;
	}

	/** See {@link #compensation}. */
	public void setCompensation(String compensation) {
		this.compensation = compensation;
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

	/** See {@link #talentPhone}. */
	@Column(name = "Talent_Phone", length = 12)
	public String getTalentPhone() {
		return this.talentPhone;
	}

	/** See {@link #talentPhone}. */
	public void setTalentPhone(String talentPhone) {
		this.talentPhone = talentPhone;
	}

	/** See {@link #talentEmailAddress}. */
	@Column(name = "Talent_Email_Address", length = 100)
	/** See {@link #agencyProducer}. */
	public String getTalentEmailAddress() {
		return this.talentEmailAddress;
	}

	/** See {@link #talentEmailAddress}. */
	public void setTalentEmailAddress(String talentEmailAddress) {
		this.talentEmailAddress = talentEmailAddress;
	}

	/** See {@link #talentPrimaryAddress}. */
	@Column(name = "Talent_Primary_Address_Id")
	public Address getTalentPrimaryAddress() {
		return this.talentPrimaryAddress;
	}

	/** See {@link #talentPrimaryAddress}. */
	public void setTalentPrimaryAddress(Address talentPrimaryAddress) {
		this.talentPrimaryAddress = talentPrimaryAddress;
	}

	/** See {@link #talentCOName}. */
	@Column(name = "Talent_C_O_Name", length = 30)
	public String getTalentCOName() {
		return this.talentCOName;
	}

	/** See {@link #talentCOName}. */
	public void setTalentCOName(String talentCOName) {
		this.talentCOName = talentCOName;
	}

	/** See {@link #talentCOAddress}. */
	@Column(name = "Talent_C_O_Address_Id")
	public Address getTalentCOAddress() {
		return this.talentCOAddress;
	}

	/** See {@link #talentCOAddress}. */
	public void setTalentCOAddress(Address talentCOAddress) {
		this.talentCOAddress = talentCOAddress;
	}

	/** See {@link #talentPayTo}. */
	@Column(name = "Talent_Pay_To", length = 30)
	public String getTalentPayTo() {
		return this.talentPayTo;
	}

	/** See {@link #talentPayTo}. */
	public void setTalentPayTo(String talentPayTo) {
		this.talentPayTo = talentPayTo;
	}

	/** See {@link #talentSign}. */
	@Column(name = "Talent_Sign_Id")
	public Integer getTalentSignId() {
		return this.talentSignId;
	}

	/** See {@link #talentSign}. */
	public void setTalentSignId(Integer talentSignId) {
		this.talentSignId = talentSignId;
	}

	/** See {@link #contractCreationDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Contract_Creation_Date", length = 0)
	public Date getContractCreationDate() {
		return this.contractCreationDate;
	}

	/** See {@link #contractCreationDate}. */
	public void setContractCreationDate(Date contractCreationDate) {
		this.contractCreationDate = contractCreationDate;
	}

	/** See {@link #estimateNum}. */
	@Column(name = "Estimate_Num", length = 30)
	public String getEstimateNum() {
		return this.estimateNum;
	}

	/** See {@link #estimateNum}. */
	public void setEstimateNum(String estimateNum) {
		this.estimateNum = estimateNum;
	}

	/** See {@link #jobNum}. */
	@Column(name = "Job_Num", length = 30)
	public String getJobNum() {
		return this.jobNum;
	}

	/** See {@link #jobNum}. */
	public void setJobNum(String jobNum) {
		this.jobNum = jobNum;
	}

	/** See {@link #agencyName}. */
	@Column(name = "Agency_Name", length = 30)
	public String getAgencyName() {
		return this.agencyName;
	}

	/** See {@link #agencyName}. */
	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	/** See {@link #agencyAddress}. */
	@Column(name = "Agency_Address_Id")
	public Address getAgencyAddress() {
		return this.agencyAddress;
	}

	/** See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	/** See {@link #agencyEmailAddress}. */
	@Column(name = "Agency_Email_Address", length = 100)
	public String getAgencyEmailAddress() {
		return this.agencyEmailAddress;
	}

	/** See {@link #agencyEmailAddress}. */
	public void setAgencyEmailAddress(String agencyEmailAddress) {
		this.agencyEmailAddress = agencyEmailAddress;
	}

	/** See {@link #producerName}. */
	@Column(name = "Producer_Name", length = 30)
	public String getProducerName() {
		return this.producerName;
	}

	/** See {@link #producerName}. */
	public void setProducerName(String producerName) {
		this.producerName = producerName;
	}

	/** See {@link #producerSignName}. */
	@Column(name = "Producer_Sign_Name", length = 30)
	public String getProducerSignName() {
		return this.producerSignName;
	}

	/** See {@link #producerSignName}. */
	public void setProducerSignName(String producerSignName) {
		this.producerSignName = producerSignName;
	}

	/** See {@link #.producerSign}. */
	@Column(name = "Producer_Sign_Id")
	public Integer getProducerSignId() {
		return this.producerSignId;
	}

	/** See {@link #.producerSign}. */
	public void setProducerSignId(Integer producerSignId) {
		this.producerSignId = producerSignId;
	}

	/** See {@link #prodCompanyName}. */
	@Column(name = "Prod_Company_Name", length = 50)
	public String getProdCompanyName() {
		return this.prodCompanyName;
	}

	/** See {@link #prodCompanyName}. */
	public void setProdCompanyName(String prodCompanyName) {
		this.prodCompanyName = prodCompanyName;
	}

	/** See {@link #advertiserName}. */
	@Column(name = "Advertiser_Name", length = 30)
	public String getAdvertiserName() {
		return this.advertiserName;
	}

	/** See {@link #advertiserName}. */
	public void setAdvertiserName(String advertiserName) {
		this.advertiserName = advertiserName;
	}

	/** See {@link #advertiserProducts}. */
	@Column(name = "Advertiser_Products", length = 250)
	public String getAdvertiserProducts() {
		return this.advertiserProducts;
	}

	/** See {@link #advertiserProducts}. */
	public void setAdvertiserProducts(String advertiserProducts) {
		this.advertiserProducts = advertiserProducts;
	}

	/** See {@link #nonEveningWearCnt}. */
	@Column(name = "Non_Evening_Wear_Cnt")
	public Integer getNonEveningWearCnt() {
		return this.nonEveningWearCnt;
	}

	/** See {@link #nonEveningWearCnt}. */
	public void setNonEveningWearCnt(Integer nonEveningWearCnt) {
		this.nonEveningWearCnt = nonEveningWearCnt;
	}

	/** See {@link #eveningWearCnt}. */
	@Column(name = "Evening_Wear_Cnt")
	public Integer getEveningWearCnt() {
		return this.eveningWearCnt;
	}

	/** See {@link #eveningWearCnt}. */
	public void setEveningWearCnt(Integer eveningWearCnt) {
		this.eveningWearCnt = eveningWearCnt;
	}

	/** See {@link #totalWardrobeFee}. */
	@Column(name = "Total_Wardrobe_Fee", precision = 6)
	public Double getTotalWardrobeFee() {
		return this.totalWardrobeFee;
	}

	/** See {@link #totalWardrobeFee}. */
	public void setTotalWardrobeFee(Double totalWardrobeFee) {
		this.totalWardrobeFee = totalWardrobeFee;
	}

	/** See {@link #flightInsuranceDays}. */
	@Column(name = "Flight_Insurance_Days")
	public Integer getFlightInsuranceDays() {
		return this.flightInsuranceDays;
	}

	/** See {@link #flightInsuranceDays}. */
	public void setFlightInsuranceDays(Integer flightInsuranceDays) {
		this.flightInsuranceDays = flightInsuranceDays;
	}

	/** See {@link #placeEngagement}. */
	@Column(name = "Place_Engagement", length = 100)
	public String getPlaceEngagement() {
		return this.placeEngagement;
	}

	/** See {@link #placeEngagement}. */
	public void setPlaceEngagement(String placeEngagement) {
		this.placeEngagement = placeEngagement;
	}

	/** See {@link #dateHourEngagement}. */
	@Column(name = "Date_Hour_Engagement", length = 50)
	public String getDateHourEngagement() {
		return this.dateHourEngagement;
	}

	/** See {@link #dateHourEngagement}. */
	public void setDateHourEngagement(String dateHourEngagement) {
		this.dateHourEngagement = dateHourEngagement;
	}

	/** See {@link #servicesRenderedAddress}. */
	@Column(name = "Services_Rendered_Address_Id")
	public Address getServicesRenderedAddress() {
		return this.servicesRenderedAddress;
	}

	/** See {@link #servicesRenderedAddress}. */
	public void setServicesRenderedAddress(Address servicesRenderedAddress) {
		this.servicesRenderedAddress = servicesRenderedAddress;
	}

	/** See {@link #partPlayed}. */
	@Column(name = "Part_Played", length = 100)
	public String getPartPlayed() {
		return this.partPlayed;
	}

	/** See {@link #partPlayed}. */
	public void setPartPlayed(String partPlayed) {
		this.partPlayed = partPlayed;
	}

	/** See {@link #useUnlimited}. */
	@Column(name = "Use_Unlimited")
	public Boolean getUseUnlimited() {
		return this.useUnlimited;
	}

	/** See {@link #useUnlimited}. */
	public void setUseUnlimited(Boolean useUnlimited) {
		this.useUnlimited = useUnlimited;
	}

	/** See {@link #use13Weeks}. */
	@Column(name = "Use_13_Weeks")
	public Boolean getUse13Weeks() {
		return this.use13Weeks;
	}

	/** See {@link #use13Weeks}. */
	public void setUse13Weeks(Boolean use13Weeks) {
		this.use13Weeks = use13Weeks;
	}

	/** See {@link #useCableOnly}. */
	@Column(name = "Use_Cable_Only")
	public Boolean getUseCableOnly() {
		return this.useCableOnly;
	}

	/** See {@link #useCableOnly}. */
	public void setUseCableOnly(Boolean useCableOnly) {
		this.useCableOnly = useCableOnly;
	}

	/** See {@link #useInternetOnly}. */
	@Column(name = "Use_Internet_Only")
	public Boolean getUseInternetOnly() {
		return this.useInternetOnly;
	}

	/** See {@link #useInternetOnly}. */
	public void setUseInternetOnly(Boolean useInternetOnly) {
		this.useInternetOnly = useInternetOnly;
	}

	/** See {@link #useNewMediaOnly}. */
	@Column(name = "Use_New_Media_Only")
	public Boolean getUseNewMediaOnly() {
		return this.useNewMediaOnly;
	}

	/** See {@link #useNewMediaOnly}. */
	public void setUseNewMediaOnly(Boolean useNewMediaOnly) {
		this.useNewMediaOnly = useNewMediaOnly;
	}

	/** See {@link #useSocialMediaOnly}. */
	@Column(name = "Use_Social_Media_Only")
	public Boolean getUseSocialMediaOnly() {
		return this.useSocialMediaOnly;
	}

	/** See {@link #useSocialMediaOnly}. */
	public void setUseSocialMediaOnly(Boolean useSocialMediaOnly) {
		this.useSocialMediaOnly = useSocialMediaOnly;
	}

	/** See {@link #talentAgencyName}. */
	@Column(name = "Talent_Agency_Name", length = 50)
	public String getTalentAgencyName() {
		return this.talentAgencyName;
	}

	/** See {@link #talentAgencyName}. */
	public void setTalentAgencyName(String talentAgencyName) {
		this.talentAgencyName = talentAgencyName;
	}

	/** See {@link #agentCommission}. */
	@Column(name = "Agent_Commission", precision = 6)
	public Double getAgentCommission() {
		return this.agentCommission;
	}

	/** See {@link #agentCommission}. */
	public void setAgentCommission(Double agentCommission) {
		this.agentCommission = agentCommission;
	}

	/** See {@link #travelWithinZoneDays}. */
	@Column(name = "Travel_Within_Zone_Days")
	public Integer getTravelWithinZoneDays() {
		return this.travelWithinZoneDays;
	}

	/** See {@link #travelWithinZoneDays}. */
	public void setTravelWithinZoneDays(Integer travelWithinZoneDays) {
		this.travelWithinZoneDays = travelWithinZoneDays;
	}

	/** See {@link #usingMoped}. */
	@Column(name = "Using_Moped")
	public Boolean getUsingMoped() {
		return this.usingMoped;
	}

	/** See {@link #usingMoped}. */
	public void setUsingMoped(Boolean usingMoped) {
		this.usingMoped = usingMoped;
	}

	/** See {@link #mopedType}. */
	@Column(name = "Moped_Type", length = 30)
	public String getMopedType() {
		return this.mopedType;
	}

	/** See {@link #mopedType}. */
	public void setMopedType(String mopedType) {
		this.mopedType = mopedType;
	}

	/** See {@link #mopedTollsAmt}. */
	@Column(name = "Moped_Tolls_Amt", precision = 6)
	public Double getMopedTollsAmt() {
		return this.mopedTollsAmt;
	}

	/** See {@link #mopedTollsAmt}. */
	public void setMopedTollsAmt(Double mopedTollsAmt) {
		this.mopedTollsAmt = mopedTollsAmt;
	}

	/** See {@link #mopedMileageAmt}. */
	@Column(name = "Moped_Mileage_Amt", precision = 6)
	public Double getMopedMileageAmt() {
		return this.mopedMileageAmt;
	}

	/** See {@link #mopedMileageAmt}. */
	public void setMopedMileageAmt(Double mopedMileageAmt) {
		this.mopedMileageAmt = mopedMileageAmt;
	}

	/** See {@link #mopedParkingAmt}. */
	@Column(name = "Moped_Parking_Amt", precision = 6)
	public Double getMopedParkingAmt() {
		return this.mopedParkingAmt;
	}

	/** See {@link #mopedParkingAmt}. */
	public void setMopedParkingAmt(Double mopedParkingAmt) {
		this.mopedParkingAmt = mopedParkingAmt;
	}

	/** See {@link #usingAuto}. */
	@Column(name = "Using_Auto")
	public Boolean getUsingAuto() {
		return this.usingAuto;
	}

	/** See {@link #usingAuto}. */
	public void setUsingAuto(Boolean usingAuto) {
		this.usingAuto = usingAuto;
	}

	/** See {@link #autoType}. */
	@Column(name = "Auto_Type", length = 30)
	public String getAutoType() {
		return this.autoType;
	}

	/** See {@link #autoType}. */
	public void setAutoType(String autoType) {
		this.autoType = autoType;
	}

	/** See {@link #autoTollsAmt}. */
	@Column(name = "Auto_Tolls_Amt", precision = 6)
	public Double getAutoTollsAmt() {
		return this.autoTollsAmt;
	}

	/** See {@link #autoTollsAmt}. */
	public void setAutoTollsAmt(Double autoTollsAmt) {
		this.autoTollsAmt = autoTollsAmt;
	}

	/** See {@link #autoMileageAmt}. */
	@Column(name = "Auto_Mileage_Amt", precision = 6)
	public Double getAutoMileageAmt() {
		return this.autoMileageAmt;
	}

	/** See {@link #autoMileageAmt}. */
	public void setAutoMileageAmt(Double autoMileageAmt) {
		this.autoMileageAmt = autoMileageAmt;
	}

	/** See {@link #autoParkingAmt}. */
	@Column(name = "Auto_Parking_Amt", precision = 6)
	public Double getAutoParkingAmt() {
		return this.autoParkingAmt;
	}

	/** See {@link #autoParkingAmt}. */
	public void setAutoParkingAmt(Double autoParkingAmt) {
		this.autoParkingAmt = autoParkingAmt;
	}
}
