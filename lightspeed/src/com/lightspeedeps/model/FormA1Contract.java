package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * FormA1Contract entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "form_a1_contract")

public class FormA1Contract extends Form<FormA1Contract> {
	private static final long serialVersionUID = - 4632477795522967096L;
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
	/** If performer is provided multiple tracks or sweetening to a song in commercial. */
	private Boolean multiTrackSweetening;
	/** Numer of dance-wear items supplied by the performer. */
	private Integer dancerFootwearCnt;
	/** Total fee to be paid to the performer for dancd-wear items. */
	private Double totalDancerFootwearFee;
	/** Whether this is a dealer commercial. */
	private Boolean dealerCommercial;
	/** Whether this a dealer commercial of type A */
	private Boolean dealerComTypeA;
	/** Whether this a dealer commercial of type B */
	private Boolean dealerComTypeB;
	/** Whether this a dealer commercial of type B */
	private Boolean seasonalCom;
	/** Whether this a dealer commercial of type B */
	private Boolean testMarketCom;
	/** Whether this a dealer commercial of type B */
	private Boolean nonAirCom;
	/** Whether this a dealer commercial of type B */
	private Boolean cableOnlyCom;
	/** Whether this a dealer commercial of type B */
	private Boolean internetOnlyCom;
	/** Whether this a dealer commercial of type B */
	private Boolean newMediaCom;
	/** Whether this a dealer commercial of type B */
	private Boolean socialMediaCom;
	/** Whether this a dealer commercial of type B */
	private Boolean workInSmoke;
	/** Whether this a dealer commercial of type B */
	private Boolean langTransService;
	/** Whether this a dealer commercial of type B */
	private Boolean otherResidual;
	/** Whether this a dealer commercial of type B */
	private String otherResidualText;
	/** Whether consenting to performer's services to appear on the Internet. */
	private Boolean consentInternet;
	/** Whether consenting to performer's services to appear in the New Media. */
	private Boolean consentNewMedia;
	/** Whether consenting to performer's services to appear in the Social Media. */
	private Boolean consentSocialMedia;
	/** Whether consenting to performer's services to be paid using dealer rates. */
	private Boolean consentDealerRates;
	/** Whether does not allow this commercial to used for foreign markets. */
	private Boolean strikeForeignUse;
	/** Whether does not allow this commercial to used for industrial uses. */
	private Boolean strikeTheatricalIndustryUse;

	// Constructors

	/** default constructor */
	public FormA1Contract() {
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

	/** See {@link #multiTrackSweetening}. */
	@Column(name = "Multi_Track_Sweetening")
	public Boolean getMultiTrackSweetening() {
		return this.multiTrackSweetening;
	}

	/** See {@link #multiTrackSweetening}. */
	public void setMultiTrackSweetening(Boolean multiTrackSweetening) {
		this.multiTrackSweetening = multiTrackSweetening;
	}

	/** See {@link #dancerFootwearCnt}. */
	@Column(name = "Dancer_Footwear_Cnt")
	public Integer getDancerFootwearCnt() {
		return this.dancerFootwearCnt;
	}

	/** See {@link #dancerFootwearCnt}. */
	public void setDancerFootwearCnt(Integer dancerFootwearCnt) {
		this.dancerFootwearCnt = dancerFootwearCnt;
	}

	/** See {@link #totalDancerFootwearFee}. */
	@Column(name = "Total_Dancer_Footwear_Fee", precision = 6)
	public Double getTotalDancerFootwearFee() {
		return this.totalDancerFootwearFee;
	}

	/** See {@link #totalDancerFootwearFee}. */
	public void setTotalDancerFootwearFee(Double totalDancerFootwearFee) {
		this.totalDancerFootwearFee = totalDancerFootwearFee;
	}

	/** See {@link #dealerCommmercial}. */
	@Column(name = "Dealer_Commmercial")
	public Boolean getDealerCommercial() {
		return this.dealerCommercial;
	}

	/** See {@link #dealerCommmercial}. */
	public void setDealerCommercial(Boolean dealerCommercial) {
		this.dealerCommercial = dealerCommercial;
	}

	/** See {@link #dealerComTypeA}. */
	@Column(name = "Dealer_Com_TypeA")
	public Boolean getDealerComTypeA() {
		return this.dealerComTypeA;
	}

	/** See {@link #dealerComTypeA}. */
	public void setDealerComTypeA(Boolean dealerComTypeA) {
		this.dealerComTypeA = dealerComTypeA;
	}

	/** See {@link #dealerComTypeB}. */
	@Column(name = "Dealer_Com_TypeB")
	public Boolean getDealerComTypeB() {
		return this.dealerComTypeB;
	}

	/** See {@link #dealerComTypeB}. */
	public void setDealerComTypeB(Boolean dealerComTypeB) {
		this.dealerComTypeB = dealerComTypeB;
	}

	/** See {@link #seasonalCom}. */
	@Column(name = "Seasonal_Com")
	public Boolean getSeasonalCom() {
		return this.seasonalCom;
	}

	/** See {@link #seasonalCom}. */
	public void setSeasonalCom(Boolean seasonalCom) {
		this.seasonalCom = seasonalCom;
	}

	/** See {@link #testMarketCom}. */
	@Column(name = "Test_Market_Com")
	public Boolean getTestMarketCom() {
		return this.testMarketCom;
	}

	/** See {@link #testMarketCom}. */
	public void setTestMarketCom(Boolean testMarketCom) {
		this.testMarketCom = testMarketCom;
	}

	/** See {@link #nonAirCom}. */
	@Column(name = "Non_Air_Com")
	public Boolean getNonAirCom() {
		return this.nonAirCom;
	}

	/** See {@link #nonAirCom}. */
	public void setNonAirCom(Boolean nonAirCom) {
		this.nonAirCom = nonAirCom;
	}

	/** See {@link #cableOnlyCom}. */
	@Column(name = "Cable_Only_Com")
	public Boolean getCableOnlyCom() {
		return this.cableOnlyCom;
	}

	/** See {@link #cableOnlyCom}. */
	public void setCableOnlyCom(Boolean cableOnlyCom) {
		this.cableOnlyCom = cableOnlyCom;
	}

	/** See {@link #internetOnlyCom}. */
	@Column(name = "Internet_Only_Com")
	public Boolean getInternetOnlyCom() {
		return this.internetOnlyCom;
	}

	/** See {@link #agencyProducer}. */
	public void setInternetOnlyCom(Boolean internetOnlyCom) {
		this.internetOnlyCom = internetOnlyCom;
	}

	/** See {@link #newMediaCom}. */
	@Column(name = "New_Media_Com")
	public Boolean getNewMediaCom() {
		return this.newMediaCom;
	}

	/** See {@link #newMediaCom}. */
	public void setNewMediaCom(Boolean newMediaCom) {
		this.newMediaCom = newMediaCom;
	}

	/** See {@link #socialMediaCom}. */
	@Column(name = "Social_Media_Com")
	public Boolean getSocialMediaCom() {
		return this.socialMediaCom;
	}

	/** See {@link #socialMediaCom}. */
	public void setSocialMediaCom(Boolean socialMediaCom) {
		this.socialMediaCom = socialMediaCom;
	}

	/** See {@link #workInSmoke}. */
	@Column(name = "Work_In_Smoke")
	public Boolean getWorkInSmoke() {
		return this.workInSmoke;
	}

	/** See {@link #workInSmoke}. */
	public void setWorkInSmoke(Boolean workInSmoke) {
		this.workInSmoke = workInSmoke;
	}

	/** See {@link #langTransService}. */
	@Column(name = "Lang_Trans_Service")
	public Boolean getLangTransService() {
		return this.langTransService;
	}

	/** See {@link #langTransService}. */
	public void setLangTransService(Boolean langTransService) {
		this.langTransService = langTransService;
	}

	/** See {@link #otherResidual}. */
	@Column(name = "Other_Residual")
	public Boolean getOtherResidual() {
		return this.otherResidual;
	}

	/** See {@link #otherResidual}. */
	public void setOtherResidual(Boolean otherResidual) {
		this.otherResidual = otherResidual;
	}

	/** See {@link #otherResidualText}. */
	@Column(name = "Other_Residual_Text", length = 75)
	public String getOtherResidualText() {
		return this.otherResidualText;
	}

	/** See {@link #otherResidualText}. */
	public void setOtherResidualText(String otherResidualText) {
		this.otherResidualText = otherResidualText;
	}

	/** See {@link #consentInternet}. */
	@Column(name = "Consent_Internet")
	public Boolean getConsentInternet() {
		return this.consentInternet;
	}

	/** See {@link #consentInternet}. */
	public void setConsentInternet(Boolean consentInternet) {
		this.consentInternet = consentInternet;
	}

	/** See {@link #consentNewMedia}. */
	@Column(name = "Consent_New_Media", nullable = false)
	public Boolean getConsentNewMedia() {
		return this.consentNewMedia;
	}

	/** See {@link #consentNewMedia}. */
	public void setConsentNewMedia(Boolean consentNewMedia) {
		this.consentNewMedia = consentNewMedia;
	}

	/** See {@link #consentSocialMedia}. */
	@Column(name = "Consent_Social_Media")
	public Boolean getConsentSocialMedia() {
		return this.consentSocialMedia;
	}

	/** See {@link #consentSocialMedia}. */
	public void setConsentSocialMedia(Boolean consentSocialMedia) {
		this.consentSocialMedia = consentSocialMedia;
	}

	/** See {@link #consentDealerRates}. */
	@Column(name = "Consent_Dealer_Rates")
	public Boolean getConsentDealerRates() {
		return this.consentDealerRates;
	}

	/** See {@link #consentDealerRates}. */
	public void setConsentDealerRates(Boolean consentDealerRates) {
		this.consentDealerRates = consentDealerRates;
	}

	/** See {@link #strikeForeignUse}. */
	@Column(name = "Strike_Foreign_Use")
	public Boolean getStrikeForeignUse() {
		return this.strikeForeignUse;
	}

	/** See {@link #strikeForeignUse}. */
	public void setStrikeForeignUse(Boolean strikeForeignUse) {
		this.strikeForeignUse = strikeForeignUse;
	}

	/** See {@link #strikeTheatricalIndustryUse}. */
	@Column(name = "Strike_Theatrical_Industry_Use")
	public Boolean getStrikeTheatricalIndustryUse() {
		return this.strikeTheatricalIndustryUse;
	}

	/** See {@link #strikeTheatricalIndustryUse}. */
	public void setStrikeTheatricalIndustryUse(Boolean strikeTheatricalIndustryUse) {
		this.strikeTheatricalIndustryUse = strikeTheatricalIndustryUse;
	}
}
