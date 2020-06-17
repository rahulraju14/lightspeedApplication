package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Talent entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name="talent")
public class Talent extends PersistentObject<Talent> {
	private static final long serialVersionUID = 3034660749020816123L;

	// Fields    

	/** Talent Name */
	private String name;
	/** Whether this person is a minor */
	private Boolean minor;
	/** Performance Category this person falls under. */
	//LS-1987
	private String category;
	/** Date that the performer is used */
	private Date shootDate;
	/** Where shoot happens */
	private String location;
	/**The Intent form this belongs to. */
	private FormActraIntent formActraIntent;
	
	// Constructors

	/** default constructor */
	public Talent() {
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

	/** See {@link #minor}. */
	@Column(name = "Minor")
	public Boolean getMinor() {
		return this.minor;
	}
	/** See {@link #minor}. */
	public void setMinor(Boolean minor) {
		this.minor = minor;
	}

	//LS-1987
	/** See {@link #category}. */
	@Column(name = "Category")
	public String getCategory() {
		return this.category;
	}
	/** See {@link #category}. */
	public void setCategory(String category) {
		this.category = category;
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

	/** See {@link #formActraIntent}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Intent_Id")
	public FormActraIntent getFormActraIntent() {
		return formActraIntent;
	}
	/** See {@link #formActraIntent}. */
	public void setFormActraIntent(FormActraIntent formActraIntent) {
		this.formActraIntent = formActraIntent;
	}
}
