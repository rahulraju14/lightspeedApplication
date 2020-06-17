package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Commercial entity. Specific information for this commercial being shot.
 */
@Entity
@Table(name = "commercial")
public class Commercial extends PersistentObject<Commercial> {
	private static final long serialVersionUID = 3742701128458117803L;
	
	// Fields

	/** Commerecial Name **/
	private String name;
	/** When the commercial is being shot */
	private Date shootDate;
	/** Where the commercial will be shot */
	private String location;
	/** The length in seconds of the commercial when aired */
	private String length;

	// Constructors

	/** default constructor */
	public Commercial() {
	}

	// Property accessors
	
	/** See {@link #name}. */
	@Column(name = "Name")
	public String getName() {
		return this.name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #shootDate}. */
	@Column(name = "Shoot_Date")
	public Date getShootDate() {
		return this.shootDate;
	}
	/** See {@link #shootDate}. */
	public void setShootDate(Date shootDate) {
		this.shootDate = shootDate;
	}

	/** See {@link #location}. */
	@Column(name = "Location")
	public String getLocation() {
		return this.location;
	}
	/** See {@link #location}. */
	public void setLocation(String location) {
		this.location = location;
	}

	/** See {@link #length}. */
	@Column(name = "Length")
	public String getLength() {
		return this.length;
	}
	/** See {@link #length}. */
	public void setLength(String length) {
		this.length = length;
	}
}	
