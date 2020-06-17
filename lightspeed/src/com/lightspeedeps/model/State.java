package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * State entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "state", uniqueConstraints = @UniqueConstraint(columnNames = "Code"))
public class State extends PersistentObject<State> {
	private static final long serialVersionUID = 6160157233174207044L;

	// Fields
	private Country country;
	private String code;
	private String name;

	// Constructors

	/** default constructor */
	public State() {
	}

	/** full constructor */
/*	public State(Country country, String code, String name) {
		this.country = country;
		this.code = code;
		this.name = name;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Country_id", nullable = false)
	public Country getCountry() {
		return this.country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Column(name = "Code", unique = true, nullable = false, length = 10)
	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
