package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Occupation entity. The Occupation table is used to maintain a list of
 * occupation names and related occupation codes which is used to populate
 * fields in the {@link StartForm}. A list of occupation names from this table
 * may be presented to the user based on a previously-selected union.
 */
@Entity
@Table(name = "occupation", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Union_Code", "Occ_Code", "Name" }))
public class Occupation extends PersistentObject<Occupation> {

	/** */
	private static final long serialVersionUID = 5990212437927886116L;

	// Fields

	/** The unionCode is a value closely related to a Union local number or
	 * abbreviation. A {@link StartForm} contains a unionCode; the value
	 * typically originates from the {@link Unions#occupationUnion} field. */
	private String occUnion;

	/** The Occupation Code for a specific occupation.  These codes are taken from the contracts
	 * negotiated by the unions. */
	private String occCode;

	/** The Lightspeed-assigned Occupation Code for a specific occupation. These
	 * codes will match occCode value unless that would cause a duplication.
	 * Also, Lightspeed may assign occupation codes for occupations that do not
	 * have an assigned code, e.g., occupations from the New York and DGA contracts. */
	private String lsOccCode;

	/** The "official" name of the occupation, taken (where applicable) from a union contract. */
	private String name;

	/** This indicates from which contract this occupation and its qualifiers was
	 * derived. This is used to qualify the list of Occupations presented based
	 * on the contracts (Production.contracts) to which the current Production is
	 * a signatory. */
	private String contractCode;

	/** This indicates from which contract, and sub-part of a contract, this occupation and its
	 * qualifiers was derived.  This value may be used to look up the proper entry in the PayRate
	 * table.  This value, plus the lsOccCode, plus an effective date, should match either one
	 * or two (Studio and Distant, or "All") entries in the PayRate table. This code integrates
	 * qualifiers such as season and production type to create a unique lookup value. */
	private String contractRateKey;

	// Qualifying fields -- these restrict the productions to which this occupation applies.

	/** A geographical region restriction, or 'A' for any/all regions. */
	private char region;

	/** Major or Independent production, or 'A' for all. */
	private char majorIndie;

	/** Feature or TV, or 'A' for all. */
	private char featureTv;

	/** One or more agreement codes, or 'A' for all. */
	private String agreement;

	/** One or more qualifying detailed production types (MOW, Pilot, etc.) or 'A' for all. */
	private String prodType;

	/** One or more TV season codes (1/2/3/N), or 'A' for all. */
	private String season;

	/** One or more codes indicating the period of time during which this TV series
	 * first started filming, or 'A' for all. */
	private String startRange;


	// Constructors

	/** default constructor */
	public Occupation() {
	}

//	/** full constructor */
//	public Occupation(String unionCode, String occCode, String name) {
//		this.unionCode = unionCode;
//		this.occCode = occCode;
//		this.name = name;
//	}

	@Column(name = "Union_Code", length = 10)
	public String getOccUnion() {
		return occUnion;
	}
	public void setOccUnion(String unionCode) {
		occUnion = unionCode;
	}

	@Column(name = "Occ_Code", nullable = false, length = 10)
	public String getOccCode() {
		return occCode;
	}
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	/**See {@link #lsOccCode}. */
	@Column(name = "Ls_Occ_Code", nullable = false, length = 10)
	public String getLsOccCode() {
		return lsOccCode;
	}
	/**See {@link #lsOccCode}. */
	public void setLsOccCode(String lSoccCode) {
		lsOccCode = lSoccCode;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/**See {@link #contractCode}. */
	@Column(name = "Contract_Code", nullable = false, length = 20)
	public String getContractCode() {
		return contractCode;
	}
	/**See {@link #contractCode}. */
	public void setContractCode(String contractRateCode) {
		contractCode = contractRateCode;
	}

	/** See {@link #contractRateKey}. */
	@Column(name = "Contract_Rate_Key", nullable = false, length = 30)
	public String getContractRateKey() {
		return contractRateKey;
	}
	/** See {@link #contractRateKey}. */
	public void setContractRateKey(String contractRateKey) {
		this.contractRateKey = contractRateKey;
	}

	/**See {@link #region}. */
	@Column(name = "Region", nullable = false)
	public char getRegion() {
		return region;
	}
	/**See {@link #region}. */
	public void setRegion(char region) {
		this.region = region;
	}

	/**See {@link #majorIndie}. */
	@Column(name = "Major_Indie", nullable = false)
	public char getMajorIndie() {
		return majorIndie;
	}
	/**See {@link #majorIndie}. */
	public void setMajorIndie(char majorIndie) {
		this.majorIndie = majorIndie;
	}

	/**See {@link #featureTv}. */
	@Column(name = "Feature_TV", nullable = false)
	public char getFeatureTv() {
		return featureTv;
	}
	/**See {@link #featureTv}. */
	public void setFeatureTv(char featureTv) {
		this.featureTv = featureTv;
	}

	/**See {@link #agreement}. */
	@Column(name = "Agreement", nullable = false, length = 10)
	public String getAgreement() {
		return agreement;
	}
	/**See {@link #agreement}. */
	public void setAgreement(String agreement) {
		this.agreement = agreement;
	}

	/**See {@link #prodType}. */
	@Column(name = "Prod_Type", nullable = false, length = 10)
	public String getProdType() {
		return prodType;
	}
	/**See {@link #prodType}. */
	public void setProdType(String prodType) {
		this.prodType = prodType;
	}

	/**See {@link #season}. */
	@Column(name = "Season", nullable = false, length = 10)
	public String getSeason() {
		return season;
	}
	/**See {@link #season}. */
	public void setSeason(String season) {
		this.season = season;
	}

	/**See {@link #startRange}. */
	@Column(name = "Start_Range", nullable = false, length = 10)
	public String getStartRange() {
		return startRange;
	}
	/**See {@link #startRange}. */
	public void setStartRange(String startRange) {
		this.startRange = startRange;
	}

}
