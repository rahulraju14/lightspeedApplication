package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * FilmMeasure entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "film_measure")
public class FilmMeasure extends PersistentObject<FilmMeasure> {
	private static final long serialVersionUID = - 6849368167949840546L;

	// Fields
	private Integer print;
	private Integer noGood;
	private Integer waste;

	// Constructors

	/** default constructor */
	public FilmMeasure() {
	}

	public FilmMeasure(int prt, int ngd, int wste) {
		print = prt;
		noGood = ngd;
		waste = wste;
	}

	// Property accessors

	@Transient
	public Integer getGross() {
		return getPrint()+getNoGood()+getWaste();
	}

	@Column(name = "Print")
	public Integer getPrint() {
		return this.print;
	}
	public void setPrint(Integer print) {
		if (print == null) {
			print = 0;
		}
		this.print = print;
	}

	@Column(name = "No_Good")
	public Integer getNoGood() {
		return this.noGood;
	}
	public void setNoGood(Integer noGood) {
		if (noGood == null) {
			noGood = 0;
		}
		this.noGood = noGood;
	}

	@Column(name = "Waste")
	public Integer getWaste() {
		return this.waste;
	}
	public void setWaste(Integer waste) {
		if (waste == null) {
			waste = 0;
		}
		this.waste = waste;
	}

}