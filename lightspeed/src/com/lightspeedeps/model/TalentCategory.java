package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * TalentCategory entity. @author MyEclipse Persistence Tools
 */

@Entity
@Table(name = "talent_category")
public class TalentCategory extends com.lightspeedeps.model.PersistentObject<TalentCategory> {
	private static final long serialVersionUID = - 5333622080667990626L;
	
	// Fields    
	/** Code used on the form */
	private String categoryCode;
	/** Internal code use by LS */
	private String lsOccCode;
	/** Green Screen occCode */
	private String gsOccCode;
	
	// Constructors

	/** default constructor */
	public TalentCategory() {
	}

	/** full constructor */
	public TalentCategory(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	
	/** See {@link #categoryCode}. */
	@Column(name = "Category_Code")
	public String getCategoryCode() {
		return this.categoryCode;
	}

	/** See {@link #categoryCode}. */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	/** See {@link #lsOccCode}. */
	@Column(name = "Ls_Occ_Code")
	public String getLsOccCode() {
		return lsOccCode;
	}

	/** See {@link #lsOccCode}. */
	public void setLsOccCode(String lsOccCode) {
		this.lsOccCode = lsOccCode;
	}

	/** See {@link #gsOccCode}. */
	@Column(name = "Gs_Occ_Code")
	public String getGsOccCode() {
		return gsOccCode;
	}

	/** See {@link #gsOccCode}. */
	public void setGsOccCode(String gsOccCode) {
		this.gsOccCode = gsOccCode;
	}
}
