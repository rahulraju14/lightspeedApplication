package com.lightspeedeps.model;

import javax.persistence.*;

/**
 * CanadaProjectDetail entity. Used for Canada project specific fields to auto-fill
 * the contract and other forms.
 */
@Entity
@Table(name = "canada_project_detail")
public class CanadaProjectDetail extends PersistentObject<CanadaProjectDetail> {
	private static final long serialVersionUID = 744162788749922870L;

	// Fields
	/* Name of brand 150*/
	private String brandName;
	/* Name of the Ad Agency */
	private String agencyName;
	/** Producer representing the Ad Agency */
	private String agencyProducer;
	/** Ad Agency Address */
	private Address agencyAddress;
	/** Name of the Advertiser for whom this commercial is being made for. */
	private String advertiserName;
	/** Name of the advertiser's product being used in the commercial. */
	private String productName;
	/** Production company name */
	private String prodHouseName;
	/** Production company address */
	private Address prodHouseAddress;
	/** Name of the person directing the commercial. */
	private String directorName;
	/** Name of the commercials */
	private String commercialName;
	/** Any additional titles for the commercial */
	private String additionalTitles;

	/** Additional fields for Project detail */
	private String nationalTv;
	private String nationalRadio;
	private String nationalDigitalMediaVideo;
	private String nationalDigitalMediaAudio;
	private String demoTv;
	private String demoRadio;
	private String demoDigital;
	private String demoPresentation;
	private String demoInfomercial;
	private Integer tagsTv;
	private Integer tagsRadio;
	private Integer tagsDigitalMedia;
	private Integer regionalChangesTv;
	private Integer regionalChangesRadio;
	private Integer regionalChangesDigitalMedia;
	private String psaTv;
	private String psaRadio;
	private String psaDigitalMedia;
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
	private String docket;

	/** used for get office list saved value */
	private Office office;

	// Constructors

	/** default constructor */
	public CanadaProjectDetail() {
		agencyAddress = new Address(true);
		prodHouseAddress = new Address(true);
	}

	// Property accessors
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
	@Column(name = "Agency_Producer", length = 100)
	public String getAgencyProducer() {
		return this.agencyProducer;
	}
	/** See {@link #agencyProducer}. */
	public void setAgencyProducer(String agencyProducer) {
		this.agencyProducer = agencyProducer;
	}

	/** See {@link #agencyAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Agency_Address_Id")
	public Address getAgencyAddress() {
		return this.agencyAddress;
	}
	/** See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	/** See {@link #advertiserName}. */
	@Column(name = "Advertiser_Name", length = 100)
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
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Prod_House_Address_Id")
	public Address getProdHouseAddress() {
		return this.prodHouseAddress;
	}
	/** See {@link #prodHouseAddress}. */
	public void setProdHouseAddress(Address prodHouseAddress) {
		this.prodHouseAddress = prodHouseAddress;
	}

	/** See {@link #directorName}. */
	@Column(name = "Director_Name", length = 100)
	public String getDirectorName() {
		return this.directorName;
	}
	/** See {@link #directorName}. */
	public void setDirectorName(String directorName) {
		this.directorName = directorName;
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

	/** See {@link #additionalTitles}. */
	@Column(name = "Additional_Titles", length = 250)
	public String getAdditionalTitles() {
		return this.additionalTitles;
	}
	/** See {@link #additionalTitles}. */
	public void setAdditionalTitles(String additionalTitles) {
		this.additionalTitles = additionalTitles;
	}

	/** See {@link #brandName}. */
	@Column(name = "Brand_Name", length = 150)
	public String getBrandName() {
		return brandName;
	}
	/** See {@link #brandName}. */
	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	/** See {@link #nationalTv}. */
	@Column(name = "National_Tv", length = 15)
	public String getNationalTv() {
		return nationalTv;
	}
	/** See {@link #nationalTv}. */
	public void setNationalTv(String nationalTv) {
		this.nationalTv = nationalTv;
	}

	/** See {@link #nationalRadio}. */
	@Column(name = "National_Radio", length = 15)
	public String getNationalRadio() {
		return nationalRadio;
	}
	/** See {@link #nationalRadio}. */
	public void setNationalRadio(String nationalRadio) {
		this.nationalRadio = nationalRadio;
	}

	/** See {@link #nationalDigitalMediaVideo}. */
	@Column(name = "National_Digital_Media_Video", length = 15)
	public String getNationalDigitalMediaVideo() {
		return nationalDigitalMediaVideo;
	}
	/** See {@link #nationalDigitalMediaVideo}. */
	public void setNationalDigitalMediaVideo(String nationalDigitalMediaVideo) {
		this.nationalDigitalMediaVideo = nationalDigitalMediaVideo;
	}

	/** See {@link #nationalDigitalMediaAudio}. */
	@Column(name = "National_Digital_Media_Audio", length = 15)
	public String getNationalDigitalMediaAudio() {
		return nationalDigitalMediaAudio;
	}
	/** See {@link #nationalDigitalMediaAudio}. */
	public void setNationalDigitalMediaAudio(String nationalDigitalMediaAudio) {
		this.nationalDigitalMediaAudio = nationalDigitalMediaAudio;
	}

	/** See {@link #demoTv}. */
	@Column(name = "Demo_Tv", length = 15)
	public String getDemoTv() {
		return demoTv;
	}
	/** See {@link #demoTv}. */
	public void setDemoTv(String demoTv) {
		this.demoTv = demoTv;
	}

	/** See {@link #demoRadio}. */
	@Column(name = "Demo_Radio", length = 15)
	public String getDemoRadio() {
		return demoRadio;
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
		return demoPresentation;
	}
	/** See {@link #demoPresentation}. */
	public void setDemoPresentation(String demoPresentation) {
		this.demoPresentation = demoPresentation;
	}

	/** See {@link #demoInfomercial}. */
	@Column(name = "Demo_Infomercial", length = 15)
	public String getDemoInfomercial() {
		return demoInfomercial;
	}
	/** See {@link #demoInfomercial}. */
	public void setDemoInfomercial(String demoInfomercial) {
		this.demoInfomercial = demoInfomercial;
	}

	/** See {@link #tagsTv}. */
	@Column(name = "Tags_Tv")
	public Integer getTagsTv() {
		return tagsTv;
	}
	/** See {@link #tagsTv}. */
	public void setTagsTv(Integer tagsTv) {
		this.tagsTv = tagsTv;
	}

	/** See {@link #tagsRadio}. */
	@Column(name = "Tags_Radio")
	public Integer getTagsRadio() {
		return tagsRadio;
	}
	/** See {@link #tagsRadio}. */
	public void setTagsRadio(Integer tagsRadio) {
		this.tagsRadio = tagsRadio;
	}

	/** See {@link #tagsDigitalMedia}. */
	@Column(name = "Tags_Digital_Media")
	public Integer getTagsDigitalMedia() {
		return tagsDigitalMedia;
	}
	/** See {@link #tagsDigitalMedia}. */
	public void setTagsDigitalMedia(Integer tagsDigitalMedia) {
		this.tagsDigitalMedia = tagsDigitalMedia;
	}

	/** See {@link #regionalChangesTv}. */
	@Column(name = "Regional_Changes_Tv")
	public Integer getRegionalChangesTv() {
		return regionalChangesTv;
	}
	/** See {@link #regionalChangesTv}. */
	public void setRegionalChangesTv(Integer regionalChangesTv) {
		this.regionalChangesTv = regionalChangesTv;
	}

	/** See {@link #regionalChangesRadio}. */
	@Column(name = "Regional_Changes_Radio")
	public Integer getRegionalChangesRadio() {
		return regionalChangesRadio;
	}
	/** See {@link #regionalChangesRadio}. */
	public void setRegionalChangesRadio(Integer regionalChangesRadio) {
		this.regionalChangesRadio = regionalChangesRadio;
	}

	/** See {@link #regionalChangesDigitalMedia}. */
	@Column(name = "Regional_Changes_Digital_Media")
	public Integer getRegionalChangesDigitalMedia() {
		return regionalChangesDigitalMedia;
	}
	/** See {@link #regionalChangesDigitalMedia}. */
	public void setRegionalChangesDigitalMedia(Integer regionalChangesDigitalMedia) {
		this.regionalChangesDigitalMedia = regionalChangesDigitalMedia;
	}

	/** See {@link #psaTv}. */
	@Column(name = "Psa_Tv", length = 15)
	public String getPsaTv() {
		return psaTv;
	}
	/** See {@link #psaTv}. */
	public void setPsaTv(String psaTv) {
		this.psaTv = psaTv;
	}

	/** See {@link #psaRadio}. */
	@Column(name = "Psa_Radio", length = 15)
	public String getPsaRadio() {
		return psaRadio;
	}
	/** See {@link #psaRadio}. */
	public void setPsaRadio(String psaRadio) {
		this.psaRadio = psaRadio;
	}

	/** See {@link #psaDigitalMedia}. */
	@Column(name = "Psa_Digital_Media", length = 15)
	public String getPsaDigitalMedia() {
		return psaDigitalMedia;
	}
	/** See {@link #psaDigitalMedia}. */
	public void setPsaDigitalMedia(String psaDigitalMedia) {
		this.psaDigitalMedia = psaDigitalMedia;
	}

	/** See {@link #seasonalTv}. */
	@Column(name = "Seasonal_Tv", length = 15)
	public String getSeasonalTv() {
		return seasonalTv;
	}
	/** See {@link #seasonalTv}. */
	public void setSeasonalTv(String seasonalTv) {
		this.seasonalTv = seasonalTv;
	}

	/** See {@link #seasonalRadio}. */
	@Column(name = "Seasonal_Radio", length = 15)
	public String getSeasonalRadio() {
		return seasonalRadio;
	}
	/** See {@link #seasonalRadio}. */
	public void setSeasonalRadio(String seasonalRadio) {
		this.seasonalRadio = seasonalRadio;
	}

	/** See {@link #seasonalDealer}. */
	@Column(name = "Seasonal_Dealer", length = 15)
	public String getSeasonalDealer() {
		return seasonalDealer;
	}
	/** See {@link #seasonalDealer}. */
	public void setSeasonalDealer(String seasonalDealer) {
		this.seasonalDealer = seasonalDealer;
	}

	/** See {@link #seasonalDealerTv}. */
	@Column(name = "Seasonal_Dealer_Tv", length = 15)
	public String getSeasonalDealerTv() {
		return seasonalDealerTv;
	}
	/** See {@link #seasonalDealerTv}. */
	public void setSeasonalDealerTv(String seasonalDealerTv) {
		this.seasonalDealerTv = seasonalDealerTv;
	}

	/** See {@link #seasonalDealerRadio}. */
	@Column(name = "Seasonal_Dealer_Radio", length = 15)
	public String getSeasonalDealerRadio() {
		return seasonalDealerRadio;
	}
	/** See {@link #seasonalDealerRadio}. */
	public void setSeasonalDealerRadio(String seasonalDealerRadio) {
		this.seasonalDealerRadio = seasonalDealerRadio;
	}

	/** See {@link #seasonalDoubleShoot}. */
	@Column(name = "Seasonal_Double_Shoot", length = 15)
	public String getSeasonalDoubleShoot() {
		return seasonalDoubleShoot;
	}
	/** See {@link #seasonalDoubleShoot}. */
	public void setSeasonalDoubleShoot(String seasonalDoubleShoot) {
		this.seasonalDoubleShoot = seasonalDoubleShoot;
	}

	/** See {@link #seasonalJointPromo}. */
	@Column(name = "Seasonal_Joint_Promo", length = 15)
	public String getSeasonalJointPromo() {
		return seasonalJointPromo;
	}
	/** See {@link #seasonalJointPromo}. */
	public void setSeasonalJointPromo(String seasonalJointPromo) {
		this.seasonalJointPromo = seasonalJointPromo;
	}

	/** See {@link #localRegionalCategoryNum}. */
	@Column(name = "Local_Regional_Category_Num", length = 15)
	public String getLocalRegionalCategoryNum() {
		return localRegionalCategoryNum;
	}
	/** See {@link #localRegionalCategoryNum}. */
	public void setLocalRegionalCategoryNum(String localRegionalCategoryNum) {
		this.localRegionalCategoryNum = localRegionalCategoryNum;
	}

	/** See {@link #localRegionalTv}. */
	@Column(name = "Local_Regional_Tv", length = 15)
	public String getLocalRegionalTv() {
		return localRegionalTv;
	}
	/** See {@link #localRegionalTv}. */
	public void setLocalRegionalTv(String localRegionalTv) {
		this.localRegionalTv = localRegionalTv;
	}

	/** See {@link #localRegionalRadio}. */
	@Column(name = "Local_Regional_Radio", length = 15)
	public String getLocalRegionalRadio() {
		return localRegionalRadio;
	}
	/** See {@link #localRegionalRadio}. */
	public void setLocalRegionalRadio(String localRegionalRadio) {
		this.localRegionalRadio = localRegionalRadio;
	}

	/** See {@link #localRegionalDigitalMedia}. */
	@Column(name = "Local_Regional_Digital_Media", length = 15)
	public String getLocalRegionalDigitalMedia() {
		return localRegionalDigitalMedia;
	}
	/** See {@link #localRegionalDigitalMedia}. */
	public void setLocalRegionalDigitalMedia(String localRegionalDigitalMedia) {
		this.localRegionalDigitalMedia = localRegionalDigitalMedia;
	}

	/** See {@link #localRegionalDemo}. */
	@Column(name = "Local_Regional_Demo", length = 15)
	public String getLocalRegionalDemo() {
		return localRegionalDemo;
	}
	/** See {@link #localRegionalDemo}. */
	public void setLocalRegionalDemo(String localRegionalDemo) {
		this.localRegionalDemo = localRegionalDemo;
	}

	/** See {@link #localRegionalDigitalMediaBroadcast}. */
	@Column(name = "Local_Regional_Digital_Media_Broadcast", length = 15)
	public String getLocalRegionalDigitalMediaBroadcast() {
		return localRegionalDigitalMediaBroadcast;
	}
	/** See {@link #localRegionalDigitalMediaBroadcast}. */
	public void setLocalRegionalDigitalMediaBroadcast(String localRegionalDigitalMediaBroadcast) {
		this.localRegionalDigitalMediaBroadcast = localRegionalDigitalMediaBroadcast;
	}

	/** See {@link #localRegionalBroadcastDigitalMedia}. */
	@Column(name = "Local_Regional_Broadcast_Digital_Media", length = 15)
	public String getLocalRegionalBroadcastDigitalMedia() {
		return localRegionalBroadcastDigitalMedia;
	}
	/** See {@link #localRegionalBroadcastDigitalMedia}. */
	public void setLocalRegionalBroadcastDigitalMedia(String localRegionalBroadcastDigitalMedia) {
		this.localRegionalBroadcastDigitalMedia = localRegionalBroadcastDigitalMedia;
	}

	/** See {@link #localRegionalOther}. */
	@Column(name = "Local_Regional_Other", length = 15)
	public String getLocalRegionalOther() {
		return localRegionalOther;
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
		return shortLifeTv7Days;
	}
	/** See {@link #shortLifeTv7Days}. */
	public void setShortLifeTv7Days(String shortLifeTv7Days) {
		this.shortLifeTv7Days = shortLifeTv7Days;
	}

	/** See {@link #shortLifeTv14Days}. */
	@Column(name = "Short_Life_Tv_14_Days", length = 15)
	public String getShortLifeTv14Days() {
		return shortLifeTv14Days;
	}
	/** See {@link #shortLifeTv14Days}. */
	public void setShortLifeTv14Days(String shortLifeTv14Days) {
		this.shortLifeTv14Days = shortLifeTv14Days;
	}

	/** See {@link #shortLifeTv31Days}. */
	@Column(name = "Short_Life_Tv_31_Days", length = 15)
	public String getShortLifeTv31Days() {
		return shortLifeTv31Days;
	}
	/** See {@link #shortLifeTv31Days}. */
	public void setShortLifeTv31Days(String shortLifeTv31Days) {
		this.shortLifeTv31Days = shortLifeTv31Days;
	}

	/** See {@link #shortLifeTv45Days}. */
	@Column(name = "Short_Life_Tv_45_Days", length = 15)
	public String getShortLifeTv45Days() {
		return shortLifeTv45Days;
	}
	/** See {@link #shortLifeTv45Days}. */
	public void setShortLifeTv45Days(String shortLifeTv45Days) {
		this.shortLifeTv45Days = shortLifeTv45Days;
	}

	/** See {@link #shortLifeRadio7Days}. */
	@Column(name = "Short_Life_Radio_7_Days", length = 15)
	public String getShortLifeRadio7Days() {
		return shortLifeRadio7Days;
	}
	/** See {@link #shortLifeRadio7Days}. */
	public void setShortLifeRadio7Days(String shortLifeRadio7Days) {
		this.shortLifeRadio7Days = shortLifeRadio7Days;
	}

	/** See {@link #shortLifeRadio14Days}. */
	@Column(name = "Short_Life_Radio_14_Days", length = 15)
	public String getShortLifeRadio14Days() {
		return shortLifeRadio14Days;
	}
	/** See {@link #shortLifeRadio14Days}. */
	public void setShortLifeRadio14Days(String shortLifeRadio14Days) {
		this.shortLifeRadio14Days = shortLifeRadio14Days;
	}

	/** See {@link #shortLifeRadio31Days}. */
	@Column(name = "Short_Life_Radio_31_Days", length = 15)
	public String getShortLifeRadio31Days() {
		return shortLifeRadio31Days;
	}
	/** See {@link #shortLifeRadio31Days}. */
	public void setShortLifeRadio31Days(String shortLifeRadio31Days) {
		this.shortLifeRadio31Days = shortLifeRadio31Days;
	}

	/** See {@link #shortLifeRadio45Days}. */
	@Column(name = "Short_Life_Radio_45_Days", length = 15)
	public String getShortLifeRadio45Days() {
		return shortLifeRadio45Days;
	}
	/** See {@link #shortLifeRadio45Days}. */
	public void setShortLifeRadio45Days(String shortLifeRadio45Days) {
		this.shortLifeRadio45Days = shortLifeRadio45Days;
	}

	/** See {@link #tvBroadcastDigitalMedia}. */
	@Column(name = "Tv_Broadcast_Digital_Media", length = 15)
	public String getTvBroadcastDigitalMedia() {
		return tvBroadcastDigitalMedia;
	}
	/** See {@link #tvBroadcastDigitalMedia}. */
	public void setTvBroadcastDigitalMedia(String tvBroadcastDigitalMedia) {
		this.tvBroadcastDigitalMedia = tvBroadcastDigitalMedia;
	}

	/** See {@link #digitalMediaBroadcastTv}. */
	@Column(name = "Digital_Media_Broadcast_Tv", length = 15)
	public String getDigitalMediaBroadcastTv() {
		return digitalMediaBroadcastTv;
	}
	/** See {@link #digitalMediaBroadcastTv}. */
	public void setDigitalMediaBroadcastTv(String digitalMediaBroadcastTv) {
		this.digitalMediaBroadcastTv = digitalMediaBroadcastTv;
	}

	/** See {@link #digitalMediaOther}. */
	@Column(name = "Digital_Media_Other", length = 15)
	public String getDigitalMediaOther() {
		return digitalMediaOther;
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
		return article2403;
	}
	/** See {@link #article2403}. */
	public void setArticle2403(String article2403) {
		this.article2403 = article2403;
	}

	/** See {@link #article2404}. */
	@Column(name = "Article_2404", length = 15)
	public String getArticle2404() {
		return article2404;
	}
	/** See {@link #article2404}. */
	public void setArticle2404(String article2404) {
		this.article2404 = article2404;
	}

	/** See {@link #article2405}. */
	@Column(name = "Article_2405", length = 15)
	public String getArticle2405() {
		return article2405;
	}
	/** See {@link #article2405}. */
	public void setArticle2405(String article2405) {
		this.article2405 = article2405;
	}

	/** See {@link #article2406}. */
	@Column(name = "Article_2406", length = 15)
	public String getArticle2406() {
		return article2406;
	}
	/** See {@link #article2406}. */
	public void setArticle2406(String article2406) {
		this.article2406 = article2406;
	}

	/** See {@link #docket}. */
	@Column(name = "Docket", length = 50)
	public String getDocket() {
		return docket;
	}
	/** See {@link #docket}. */
	public void setDocket(String docket) {
		this.docket = docket;
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

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public CanadaProjectDetail clone() {
		CanadaProjectDetail cpd;
		try {
			cpd = (CanadaProjectDetail)super.clone();
			cpd.id = null;
			// Note: cpd.office can be shared, as it is many-to-one reference

			// any complex object references should be null:
			cpd.agencyAddress = null;
			cpd.prodHouseAddress = null;
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
		return cpd;
	}

	/**
	 * @return A copy of this object, including separate copies of all the
	 *         included data objects, such as Address. (This is significantly
	 *         different than the clone() method, which copies only the
	 *         primitive data items, and all referenced objects are null in the
	 *         returned copy.)
	 */
	public CanadaProjectDetail deepCopy() {
		CanadaProjectDetail cpd = clone();
		if (cpd == null) {
			cpd = new CanadaProjectDetail();
		}
		if (agencyAddress != null) {
			cpd.setAgencyAddress(agencyAddress.clone());
		}
		if (prodHouseAddress != null) {
			cpd.setProdHouseAddress(prodHouseAddress.clone());
		}
		cpd.setOffice(office); // Many-to-one, doesn't need to be cloned.

		return cpd;
	}

	/**
	 * Method to clear all the fields from project details page for canada
	 * production LS-2070
	 *
	 * @return
	 */
	public void clearFields() {
		setAgencyAddress(new Address(true));
		setProdHouseAddress(new Address(true));
		setOffice(null);
		setBrandName(null);
		setAgencyName(null);
		setAgencyProducer(null);
		setAdvertiserName(null);
		setProductName(null);
		setProdHouseName(null);
		setDirectorName(null);
		setCommercialName(null);
		setAdditionalTitles(null);
		setNationalTv(null);
		setNationalRadio(null);
		setNationalDigitalMediaVideo(null);
		setNationalDigitalMediaAudio(null);
		setDemoTv(null);
		setDemoRadio(null);
		setDemoDigital(null);
		setDemoPresentation(null);
		setDemoInfomercial(null);
		setTagsTv(null);
		setTagsRadio(null);
		setTagsDigitalMedia(null);
		setRegionalChangesTv(null);
		setRegionalChangesRadio(null);
		setRegionalChangesDigitalMedia(null);
		setPsaTv(null);
		setPsaRadio(null);
		setPsaDigitalMedia(null);
		setSeasonalTv(null);
		setSeasonalRadio(null);
		setSeasonalDealer(null);
		setSeasonalDealerTv(null);
		setSeasonalDealerRadio(null);
		setSeasonalDoubleShoot(null);
		setSeasonalJointPromo(null);
		setLocalRegionalCategoryNum(null);
		setLocalRegionalTv(null);
		setLocalRegionalRadio(null);
		setLocalRegionalDigitalMedia(null);
		setLocalRegionalDemo(null);
		setLocalRegionalDigitalMediaBroadcast(null);
		setLocalRegionalBroadcastDigitalMedia(null);
		setLocalRegionalOther(null);
		setLocalRegionalPilotProject(null);
		setShortLifeTv7Days(null);
		setShortLifeTv14Days(null);
		setShortLifeTv31Days(null);
		setShortLifeTv45Days(null);
		setShortLifeRadio7Days(null);
		setShortLifeRadio14Days(null);
		setShortLifeRadio31Days(null);
		setShortLifeRadio45Days(null);
		setTvBroadcastDigitalMedia(null);
		setDigitalMediaBroadcastTv(null);
		setDigitalMediaOther(null);
		setRadioDigitalMedia(null);
		setActraOnline(null);
		setArticle2403(null);
		setArticle2404(null);
		setArticle2405(null);
		setArticle2406(null);
		setDocket(null);
	}
}
