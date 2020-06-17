package com.lightspeedeps.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Country entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "country", uniqueConstraints = @UniqueConstraint(columnNames = "Iso_Code"))
public class Country extends PersistentObject<Country> {
	private static final long serialVersionUID = - 6082264895938997370L;

	// Fields
	private String isoCode;
	private String name;
	private Set<State> states = new HashSet<State>(0);

	// Constructors

	/** default constructor */
	public Country() {
	}

	/** full constructor */
/*	public Country(String isoCode, String name, Set<State> states) {
		this.isoCode = isoCode;
		this.name = name;
		this.states = states;
	}
*/
	// Property accessors

	@Column(name = "Iso_Code", unique = true, nullable = false, length = 10)
	public String getIsoCode() {
		return this.isoCode;
	}

	public void setIsoCode(String isoCode) {
		this.isoCode = isoCode;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "country")
	public Set<State> getStates() {
		return this.states;
	}

	public void setStates(Set<State> states) {
		this.states = states;
	}

}