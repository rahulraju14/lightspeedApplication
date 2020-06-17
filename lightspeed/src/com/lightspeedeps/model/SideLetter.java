package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Entity representing a single Side Letter. This Side Letter
 * can be linked to multiple Contracts.
 */
@Entity
@Table(name = "side_letter")
public class SideLetter extends PersistentObject<SideLetter> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SideLetter.class);
	private static final long serialVersionUID = - 5512666272438234732L;

	// Fields

	/** A unique (business) code representing this Side Letter; this should not
	 * be changed (for a given side-letter) once this feature is in production.
	 * The linkage between a sideLetter and the contracts to which is has been
	 * applied for a given Production is maintained by storing this value in the
	 * {@link com.lightspeedeps.model.Production#contractSideLetters} field. */
	private String sideLetterCode;

	/** A comma-delimited list of contract codes, indicating to which contracts
	 * this side-letter may be applied. Each code should match a value of
	 * {@link com.lightspeedeps.model.Contract#contractCode
	 * Contract.contractCode}. */
	private String contracts;

	/** Description (full name) of this Side Letter */
	private String sideLetterDesc;

	// Constructors

	/** default constructor */
	public SideLetter() {
	}

	/** See {@link #sideLetterCode}." */
	@Column(name = "Side_Letter_Code", nullable = false, length = 25)
	public String getSideLetterCode() {
		return this.sideLetterCode;
	}
	/** See {@link #sideLetterCode}." */
	public void setSideLetterCode(String sideLetterCode) {
		this.sideLetterCode = sideLetterCode;
	}

	/** See {@link #contracts}. */
	@Column(name = "Contracts", nullable = false, length = 200)
	public String getContracts() {
		return contracts;
	}
	/** See {@link #contracts}. */
	public void setContracts(String contracts) {
		this.contracts = contracts;
	}

	/** See {@link #sideLetterDesc}." */
	@Column(name = "Side_Letter_Desc", nullable = false, length = 200)
	public String getSideLetterDesc() {
		return this.sideLetterDesc;
	}
	/** See {@link #sideLetterDesc}." */
	public void setSideLetterDesc(String sideLetterDesc) {
		this.sideLetterDesc = sideLetterDesc;
	}
}
