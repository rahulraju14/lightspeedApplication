package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * PostalLocation entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "postal_location", uniqueConstraints = @UniqueConstraint(columnNames = "Country"))
public class PostalLocation extends PersistentObject<PostalLocation> implements java.io.Serializable {
	private static final long serialVersionUID = 6428652782542186544L;

	// Fields
	private String country;
	private String postalCode;
	private Double latitude;
	private Double longitude;

	// Constructors

	/** default constructor */
	public PostalLocation() {
	}

	/** full constructor */
	public PostalLocation(String country, String postalCode, Double latitude, Double longitude) {
		this.country = country;
		this.postalCode = postalCode;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	// Property accessors

	@Column(name = "Country", unique = true, nullable = false, length = 10)
	public String getCountry() {
		return this.country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Column(name = "Postal_Code", nullable = false, length = 10)
	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	@Column(name = "Latitude", nullable = false, precision = 10, scale = 5)
	public Double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	@Column(name = "Longitude", nullable = false, precision = 10, scale = 5)
	public Double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

}