package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.OfficeType;

/**
 * Office entity. Tie forms to office(s)
 */
@Entity
@Table(name = "office")

public class Office extends PersistentObject<Office> {
	private static final long serialVersionUID = - 8533905204744800291L;

	// Fields
	/** Name of office */
	private String officeName;
	/** Code associated with the office */
	private String branchCode;
	/**
	 * Office email address. Can contain multiple email address with a
	 * semi-colon delimiter
	 */
	private String emailAddress;
	/** The type of office. If it belongs to a group of offices */
	private OfficeType officeType;
	/** Sorting order */
	private Integer sortOrder;

	// Constructors

	/** default constructor */
	public Office() {
	}

	/** @see officeName */
	@Column(name = "Office_Name", nullable = false, length = 75)
	public String getOfficeName() {
		return this.officeName;
	}
	/** @see officeName */
	public void setOfficeName(String officeName) {
		this.officeName = officeName;
	}

	/** @see branchCode */
	@Column(name = "Branch_Code", length = 10)
	public String getBranchCode() {
		return this.branchCode;
	}
	/** @see branchCode */
	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}

	/** @see emailAddress */
	@Column(name = "Email_Address", length = 150)
	public String getEmailAddress() {
		return this.emailAddress;
	}
	/** @see emailAddress */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/** @see officeType */
	@Column(name = "Office_Type", length = 45)
	@Enumerated(EnumType.STRING)
	public OfficeType getOfficeType() {
		return this.officeType;
	}
	/** @see officeType */
	public void setOfficeType(OfficeType officeType) {
		this.officeType = officeType;
	}

	/** See {@link #sortOrder}. */
	@Column(name = "Sort_Order")
	public Integer getSortOrder() {
		return sortOrder;
	}
	/** See {@link #sortOrder}. */
	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

}
