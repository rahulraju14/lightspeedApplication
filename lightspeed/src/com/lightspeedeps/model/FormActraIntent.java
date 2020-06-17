package com.lightspeedeps.model;

import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * FormActraIntent entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name="form_actra_intent")
public class FormActraIntent extends Form<FormActraIntent> {
	private static final long serialVersionUID = 7899649548036269216L;

	// Fields    
	// Form version numbers
	private static final Byte ACTRA_INTENT_VERSION_2018 = 1;
	public static final Byte DEFAULT_ACTRA_INTENT_VERSION = ACTRA_INTENT_VERSION_2018;   

     private String advertiser;
     private String product;
     private String agencyName;
     private String producerName;
     private String producerEmail;
     private String signatoryEngager;
     private String directorName;
     private String productionHouseName;
     private String lineProducerName;
     private String castingDirectorName;
     private Boolean multiBranch;
     private String multiBranchLocations;
     private Boolean useCanada;
     private Boolean useUs;
     private Boolean digitalMedia1;
     private Boolean digitalMedia2;
     private Boolean other1;
     private Boolean other2;
     private Boolean nationalCom;
     private Boolean tv;
     private Boolean radio;
     private Boolean psa;
     private Boolean locRegionCom;
     private Boolean doubleShoot;
     private Boolean demo;
     private Boolean a706Exclusions;
     private Boolean a707Waivers;  
     private Boolean seasonalCom;
     private Boolean tags;
     private Boolean dealer;
     private Boolean informercial;
     private Boolean shortLife7Days;
     private Boolean shortLife14Days;
     private Boolean shortLife31Days;
     private Boolean shortLife45Days;
     private Commercial commercial1;
     private Commercial commercial2;
     private Commercial commercial3;
     private Boolean minor;
     private Integer numMinors;
     private String minorAges;
     private Integer numExtrasGeneral;
     private Integer numExtrasGroup;
     private Integer numExtrasGroup31;
     private Boolean stunts;
     private String stuntCoordinator;
     private String stuntType;
     private Boolean extScenes;
     private String extScenesType;
     private Boolean locationShoot40Radius;
     private Boolean weatherPermitting;
     private Boolean weekendNight;
     private Boolean nudeScenes;
     private List<Talent> talent;
	 private Integer officeId;

	// LS-3093
	private String locationShootDetails;
	private String weatherPermittingDetails;
	private String weekEndNightDetails;
	private String nudeScenesDetails;

    // Constructors

    /** default constructor */
    public FormActraIntent() {
    	super();
    	setVersion(ACTRA_INTENT_VERSION_2018);
    }
    
	/** See {@link #advertiser}. */	
    @Column(name="Advertiser", length=150)
    public String getAdvertiser() {
        return this.advertiser;
    }
	/** See {@link #advertiser}. */	    
    public void setAdvertiser(String advertiser) {
        this.advertiser = advertiser;
    }
    
	/** See {@link #product}. */	    
    @Column(name="Product", length=150)
    public String getProduct() {
        return this.product;
    }
	/** See {@link #product}. */	   
    public void setProduct(String product) {
        this.product = product;
    }
    
	/** See {@link #agencyName}. */	    
    @Column(name="Agency_Name", length=150)
    public String getAgencyName() {
        return this.agencyName;
    }
	/** See {@link #agencyName}. */	    
    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }
    
	/** See {@link #producerName}. */	    
    @Column(name="Producer_Name", length=150)
    public String getProducerName() {
        return this.producerName;
    }
	/** See {@link #producerName}. */	   
    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }
    
    /** See {@link #producerEmail}. */	    
    @Column(name="Producer_Email", length=150)    
	public String getProducerEmail() {
		return producerEmail;
	}
    /** See {@link #producerEmail}. */	  
	public void setProducerEmail(String producerEmail) {
		this.producerEmail = producerEmail;
	}

	/** See {@link #signatoryEngager}. */	    
    @Column(name="Signatory_Engager", length=150)
    public String getSignatoryEngager() {
        return this.signatoryEngager;
    }
	/** See {@link #signatoryEngager}. */	   
    public void setSignatoryEngager(String signatoryEngager) {
        this.signatoryEngager = signatoryEngager;
    }
    
	/** See {@link #directorName}. */	   
    @Column(name="Director_Name", length=150)
    public String getDirectorName() {
        return this.directorName;
    }
	/** See {@link #directorName}. */	  
    public void setDirectorName(String directorName) {
        this.directorName = directorName;
    }
    
	/** See {@link #Production_House_Name}. */	   
    @Column(name="Production_House_Name", length=150)
    public String getProductionHouseName() {
        return this.productionHouseName;
    }
	/** See {@link #Production_House_Name}. */	   
    public void setProductionHouseName(String productionHouseName) {
        this.productionHouseName = productionHouseName;
    }
    
	/** See {@link #lineProducerName}. */	    
    @Column(name="Line_Producer_Name", length=150)
    public String getLineProducerName() {
        return this.lineProducerName;
    }
	/** See {@link #lineProducerName}. */	   
    public void setLineProducerName(String lineProducerName) {
        this.lineProducerName = lineProducerName;
    }
    
	/** See {@link #castingDirectorName}. */	   
    @Column(name="Casting_Director_Name", length=150)
    public String getCastingDirectorName() {
        return this.castingDirectorName;
    }
	/** See {@link #castingDirectorName}. */	    
    public void setCastingDirectorName(String castingDirectorName) {
        this.castingDirectorName = castingDirectorName;
    }
    
	/** See {@link #multiBranch}. */	    
    @Column(name="Multi_Branch")
    public Boolean getMultiBranch() {
        return this.multiBranch;
    }
	/** See {@link #multiBranch}. */	    
    public void setMultiBranch(Boolean multiBranch) {
        this.multiBranch = multiBranch;
    }
    
	/** See {@link #multiBranchLocations}. */	   
    @Column(name="Multi_Branch_Locations", length=250)
    public String getMultiBranchLocations() {
        return this.multiBranchLocations;
    }
	/** See {@link #multiBranchLocations}. */	   
    public void setMultiBranchLocations(String multiBranchLocations) {
        this.multiBranchLocations = multiBranchLocations;
    }
    
	/** See {@link #useCanada}. */	    
    @Column(name="Use_Canada")
    public Boolean getUseCanada() {
        return this.useCanada;
    }
	/** See {@link #useCanada}. */	   
    public void setUseCanada(Boolean useCanada) {
        this.useCanada = useCanada;
    }
    
	/** See {@link #useUs}. */	   
    @Column(name="Use_Us")
    public Boolean getUseUs() {
        return this.useUs;
    }
	/** See {@link #useUs}. */	   
    public void setUseUs(Boolean useUs) {
        this.useUs = useUs;
    }
    
	/** See {@link #digitalMedia1}. */	    
    @Column(name="Digital_Media_1")
    public Boolean getDigitalMedia1() {
        return this.digitalMedia1;
    }
	/** See {@link #digitalMedia1}. */	    
    public void setDigitalMedia1(Boolean digitalMedia1) {
        this.digitalMedia1 = digitalMedia1;
    }
    
	/** See {@link #digitalMedia2}. */	    
    @Column(name="Digital_Media_2")
    public Boolean getDigitalMedia2() {
        return this.digitalMedia2;
    }
	/** See {@link #digitalMedia2}. */	    
    public void setDigitalMedia2(Boolean digitalMedia2) {
        this.digitalMedia2 = digitalMedia2;
    }
    
	/** See {@link #other1}. */	    
    @Column(name="Other_1")
    public Boolean getOther1() {
        return this.other1;
    }
	/** See {@link #other1}. */	   
    public void setOther1(Boolean other1) {
        this.other1 = other1;
    }
    
	/** See {@link #other2}. */	   
    @Column(name="Other_2")
    public Boolean getOther2() {
        return this.other2;
    }
	/** See {@link #other2}. */	    
    public void setOther2(Boolean other2) {
        this.other2 = other2;
    }
    
	/** See {@link #nationalCom}. */	   
    @Column(name="National_Com")
    public Boolean getNationalCom() {
        return this.nationalCom;
    }
	/** See {@link #nationalCom}. */	   
    public void setNationalCom(Boolean nationalCom) {
        this.nationalCom = nationalCom;
    }
    
	/** See {@link #tv}. */	    
    @Column(name="Tv")
    public Boolean getTv() {
        return this.tv;
    }
	/** See {@link #tv}. */	   
    public void setTv(Boolean tv) {
        this.tv = tv;
    }

	/** See {@link #radio}. */	    
    @Column(name="Radio")
    public Boolean getRadio() {
        return this.radio;
    }
	/** See {@link #radio}. */	    
    public void setRadio(Boolean radio) {
        this.radio = radio;
    }
    
	/** See {@link #psa}. */	   
    @Column(name="Psa")
    public Boolean getPsa() {
        return this.psa;
    }
	/** See {@link #psa}. */	    
    public void setPsa(Boolean psa) {
        this.psa = psa;
    }
 
	/** See {@link #locRegionCom}. */	    
    @Column(name="Loc_Region_Com")
    public Boolean getLocRegionCom() {
        return this.locRegionCom;
    }
	/** See {@link #locRegionCom}. */	    
    public void setLocRegionCom(Boolean locRegionCom) {
        this.locRegionCom = locRegionCom;
    }
    
	/** See {@link #doubleShoot}. */	   
    @Column(name="Double_Shoot")
    public Boolean getDoubleShoot() {
        return this.doubleShoot;
    }
	/** See {@link #doubleShoot}. */	   
    public void setDoubleShoot(Boolean doubleShoot) {
        this.doubleShoot = doubleShoot;
    }
    
	/** See {@link #demo}. */	    
    @Column(name="Demo")
    public Boolean getDemo() {
        return this.demo;
    }
	/** See {@link #demo}. */	   
    public void setDemo(Boolean demo) {
        this.demo = demo;
    }
    
	/** See {@link #a706Exclusions}. */	   
    @Column(name="A706_Exclusions")
    public Boolean getA706Exclusions() {
        return this.a706Exclusions;
    }
	/** See {@link #a706Exclusions}. */	    
    public void setA706Exclusions(Boolean a706Exclusions) {
        this.a706Exclusions = a706Exclusions;
    }
    
    /** See {@link #a707Waivers}. */	   
    @Column(name="A707_Waivers")
	public Boolean getA707Waivers() {
		return a707Waivers;
	}
    /** See {@link #a707Waivers}. */	   
	public void setA707Waivers(Boolean a707Waivers) {
		this.a707Waivers = a707Waivers;
	}

	/** See {@link #seasonalCom}. */	    
    @Column(name="Seasonal_Com")
    public Boolean getSeasonalCom() {
        return this.seasonalCom;
    }
	/** See {@link #seasonalCom}. */	    
    public void setSeasonalCom(Boolean seasonalCom) {
        this.seasonalCom = seasonalCom;
    }
    
	/** See {@link #tags}. */	    
    @Column(name="Tags")
    public Boolean getTags() {
        return this.tags;
    }
	/** See {@link #tags}. */	    
    public void setTags(Boolean tags) {
        this.tags = tags;
    }
    
	/** See {@link #dealer}. */	   
    @Column(name="Dealer")
    public Boolean getDealer() {
        return this.dealer;
    }
	/** See {@link #dealer}. */	    
    public void setDealer(Boolean dealer) {
        this.dealer = dealer;
    }
    
	/** See {@link #informercial}. */	    
    @Column(name="Informercial")
    public Boolean getInformercial() {
        return this.informercial;
    }
	/** See {@link #informercial}. */	   
    public void setInformercial(Boolean informercial) {
        this.informercial = informercial;
    }
    
	/** See {@link #shortLife7Days}. */	   
    @Column(name="Short_Life_7_Days")
    public Boolean getShortLife7Days() {
        return this.shortLife7Days;
    }
	/** See {@link #shortLife7Days}. */	  
    public void setShortLife7Days(Boolean shortLife7Days) {
        this.shortLife7Days = shortLife7Days;
    }
 
	/** See {@link #shortLife14Days}. */	   
    @Column(name="Short_Life_14_Days")
    public Boolean getShortLife14Days() {
        return this.shortLife14Days;
    }
	/** See {@link #shortLife14Days}. */	  
    public void setShortLife14Days(Boolean shortLife14Days) {
        this.shortLife14Days = shortLife14Days;
    }
 
	/** See {@link #shortLife31Days}. */	   
    @Column(name="Short_Life_31_Days")
    public Boolean getShortLife31Days() {
        return this.shortLife31Days;
    }
	/** See {@link #shortLife31Days}. */	   
    public void setShortLife31Days(Boolean shortLife31Days) {
        this.shortLife31Days = shortLife31Days;
    }
 
	/** See {@link #shortLife45Days}. */	  
    @Column(name="Short_Life_45_Days")
    public Boolean getShortLife45Days() {
        return this.shortLife45Days;
    }
	/** See {@link #shortLife45Days}. */	   
    public void setShortLife45Days(Boolean shortLife45Days) {
        this.shortLife45Days = shortLife45Days;
    }
 
	/** See {@link #commercial1}. */	
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="Commercial_Id1")
    public Commercial getCommercial1() {
        return this.commercial1;
    }
	/** See {@link #commercial1}. */	   
    public void setCommercial1(Commercial commercial1) {
        this.commercial1 = commercial1;
    }
    
    /** See {@link #commercial2}. */	   
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="Commercial_Id2")
    public Commercial getCommercial2() {
        return this.commercial2;
    }
	/** See {@link #commercial2}. */	   
    public void setCommercial2(Commercial commercial2) {
        this.commercial2 = commercial2;
    }
    
    /** See {@link #commercial3}. */	   
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="Commercial_Id3")
    public Commercial getCommercial3() {
        return this.commercial3;
    }
	/** See {@link #commercial3}. */	   
    public void setCommercial3(Commercial commercial3) {
        this.commercial3 = commercial3;
    }
 
	/** See {@link #minor}. */	    
    @Column(name="Minor")
    public Boolean getMinor() {
        return this.minor;
    }
	/** See {@link #minor}. */	    
    public void setMinor(Boolean minor) {
        this.minor = minor;
    }
  
	/** See {@link #numMinors}. */	   
    @Column(name="Num_Minors")
    public Integer getNumMinors() {
        return this.numMinors;
    }
	/** See {@link #numMinors}. */	    
    public void setNumMinors(Integer numMinors) {
        this.numMinors = numMinors;
    }

	/** See {@link #minorAges}. */	   
    @Column(name="Minor_Ages", length=75)
    public String getMinorAges() {
        return this.minorAges;
    }
	/** See {@link #minorAges}. */	   
    public void setMinorAges(String minorAges) {
        this.minorAges = minorAges;
    }
 
	/** See {@link #numExtrasGeneral}. */	  
    @Column(name="Num_Extras_General")
    public Integer getNumExtrasGeneral() {
        return this.numExtrasGeneral;
    }
	/** See {@link #numExtrasGeneral}. */	   
    public void setNumExtrasGeneral(Integer numExtrasGeneral) {
        this.numExtrasGeneral = numExtrasGeneral;
    }
    
	/** See {@link #numExtrasGroup}. */	  
    @Column(name="Num_Extras_Group")
    public Integer getNumExtrasGroup() {
        return this.numExtrasGroup;
    }
	/** See {@link #numExtrasGroup}. */	   
    public void setNumExtrasGroup(Integer numExtrasGroup) {
        this.numExtrasGroup = numExtrasGroup;
    }
    
	/** See {@link #numExtrasGroup31}. */	    
    @Column(name="Num_Extras_Group_31")
    public Integer getNumExtrasGroup31() {
        return this.numExtrasGroup31;
    }
	/** See {@link #numExtrasGroup31}. */	   
    public void setNumExtrasGroup31(Integer numExtrasGroup31) {
        this.numExtrasGroup31 = numExtrasGroup31;
    }
    
	/** See {@link #stunts}. */	   
    @Column(name="Stunts")
    public Boolean getStunts() {
        return this.stunts;
    }
	/** See {@link #stunts}. */	    
    public void setStunts(Boolean stunts) {
        this.stunts = stunts;
    }
    
	/** See {@link #stuntCoordinator}. */	   
    @Column(name="Stunt_Coordinator", length=150)
	public String getStuntCoordinator() {
		return stuntCoordinator;
	}
    /** See {@link #stuntCoordinator}. */
	public void setStuntCoordinator(String stuntCoordinator) {
		this.stuntCoordinator = stuntCoordinator;
	}

	/** See {@link #stuntType}. */	   
    @Column(name="Stunt_Type", length=100)
    public String getStuntType() {
        return this.stuntType;
    }
	/** See {@link #stuntType}. */	    
    public void setStuntType(String stuntType) {
        this.stuntType = stuntType;
    }
    
	/** See {@link #extScenes}. */	   
    @Column(name="Ext_Scenes")
    public Boolean getExtScenes() {
        return this.extScenes;
    }
	/** See {@link #extScenes}. */	    
    public void setExtScenes(Boolean extScenes) {
        this.extScenes = extScenes;
    }
    
	/** See {@link #extScenesType}. */	    
    @Column(name="Ext_Scenes_Type", length=100)
    public String getExtScenesType() {
        return this.extScenesType;
    }
	/** See {@link #extScenesType}. */	    
    public void setExtScenesType(String extScenesType) {
        this.extScenesType = extScenesType;
    }
    
	/** See {@link #locationShoot40Radius}. */	    
    @Column(name="Location_Shoot_40_Radius")
    public Boolean getLocationShoot40Radius() {
        return this.locationShoot40Radius;
    }
	/** See {@link #locationShoot40Radius}. */	    
    public void setLocationShoot40Radius(Boolean locationShoot40Radius) {
        this.locationShoot40Radius = locationShoot40Radius;
    }
    
	/** See {@link #weatherPermitting}. */	    
    @Column(name="Weather_Permitting")
    public Boolean getWeatherPermitting() {
        return this.weatherPermitting;
    }
	/** See {@link #weatherPermitting}. */	    
    public void setWeatherPermitting(Boolean weatherPermitting) {
        this.weatherPermitting = weatherPermitting;
    }
    
	/** See {@link #weekendNight}. */	    
    @Column(name="Weekend_Night")
    public Boolean getWeekendNight() {
        return this.weekendNight;
    }
	/** See {@link #weekendNight}. */	    
    public void setWeekendNight(Boolean weekendNight) {
        this.weekendNight = weekendNight;
    }
    
	/** See {@link #nudeScenes}. */	    
    @Column(name="Nude_Scenes")
    public Boolean getNudeScenes() {
        return this.nudeScenes;
    }
	/** See {@link #nudeScenes}. */	   
    public void setNudeScenes(Boolean nudeScenes) {
        this.nudeScenes = nudeScenes;
    }
    
	/** See {@link #talent}. */	
    @JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "formActraIntent")
    public List<Talent> getTalent() {
        return this.talent;
    }
	/** See {@link #talent}. */	
    public void setTalent(List<Talent> talent) {
        this.talent = talent;
    }

	/** See {@link #officeId}. */
	@Column(name = "office_id")
	public Integer getOfficeId() {
		return officeId;
	}

	/** See {@link #officeId}. */
	public void setOfficeId(Integer officeId) {
		this.officeId = officeId;
	}

	/** See {@link #locationShootDetails}. */
	@Column(name = "Location_Shoot_Details", length = 100)
	public String getLocationShootDetails() {
		return locationShootDetails;
	}

	/** See {@link #locationShootDetails}. */
	public void setLocationShootDetails(String locationShootDetails) {
		this.locationShootDetails = locationShootDetails;
	}

	/** See {@link #weatherPermittingDetails}. */
	@Column(name = "Weather_Permitting_Details", length = 100)
	public String getWeatherPermittingDetails() {
		return weatherPermittingDetails;
	}

	/** See {@link #weatherPermittingDetails}. */
	public void setWeatherPermittingDetails(String weatherPermittingDetails) {
		this.weatherPermittingDetails = weatherPermittingDetails;
	}

	/** See {@link #weekEndNightDetails}. */
	@Column(name = "Weekend_Night_Details", length = 100)
	public String getWeekEndNightDetails() {
		return weekEndNightDetails;
	}

	/** See {@link #weekEndNightDetails}. */
	public void setWeekEndNightDetails(String weekEndNightDetails) {
		this.weekEndNightDetails = weekEndNightDetails;
	}

	/** See {@link #nudeScenesDetails}. */
	@Column(name = "Nude_Scenes_Details", length = 100)
	public String getNudeScenesDetails() {
		return nudeScenesDetails;
	}

	/** See {@link #nudeScenesDetails}. */
	public void setNudeScenesDetails(String nudeScenesDetails) {
		this.nudeScenesDetails = nudeScenesDetails;
	}

	/**
	 * Method to clear all fields from intent to produce form LS-2070
	 */
	public void clearFields() {
		setAdvertiser(null);
		setProduct(null);
		setAgencyName(null);
		setProducerName(null);
		setProducerEmail(null);
		setSignatoryEngager(null);
		setDirectorName(null);
		setProductionHouseName(null);
		setLineProducerName(null);
		setCastingDirectorName(null);
		setMultiBranch(false);
		setMultiBranchLocations(null);
		setUseCanada(false);
		setUseUs(false);
		setDigitalMedia1(false);
		setDigitalMedia2(false);
		setOther1(false);
		setOther2(false);
		setNationalCom(false);
		setTv(false);
		setRadio(false);
		setPsa(false);
		setLocRegionCom(false);
		setDoubleShoot(false);
		setDemo(false);
		setA706Exclusions(false);
		setA707Waivers(false);
		setSeasonalCom(false);
		setTags(false);
		setDealer(false);
		setInformercial(false);
		setShortLife7Days(false);
		setShortLife14Days(false);
		setShortLife31Days(false);
		setShortLife45Days(false);
		setCommercial1(new Commercial());
		setCommercial2(new Commercial());
		setCommercial3(new Commercial());
		setMinor(false);
		setNumMinors(null);
		setMinorAges(null);
		setNumExtrasGeneral(null);
		setNumExtrasGroup(null);
		setNumExtrasGroup31(null);
		setStunts(false);
		setStuntCoordinator(null);
		setStuntType(null);
		setExtScenes(false);
		setExtScenesType(null);
		setLocationShoot40Radius(false);
		setWeatherPermitting(false);
		setWeekendNight(false);
		setNudeScenes(false);
		setTalent(null);
		setLocationShootDetails(null);
		setWeatherPermittingDetails(null);
		setWeekEndNightDetails(null);
		setNudeScenesDetails(null);
	}
}