package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Mapping table to retrieve the Green Screen (GS) contract and union type
 * based on the LS unionKey and contractRateKey
 */
@Entity
@Table(name = "contract_mapping")

public class ContractMapping extends PersistentObject<ContractMapping> {
	private static final long serialVersionUID = 1L;

	// Fields
	/** LS Union key associated with the GS contract */
	private String unionKey;

	/** LS Contract Code associated with the GS contract. */
	private String contractCode;

	/** GS contract code associated with the LS unionKey and contractCode. */
	private String gsContractCode;

	/** GS contract code associated with the LS unionKey and contractCode. */
	private String gsUnionType;
	// Constructors

	/** default constructor */
	public ContractMapping() {
	}

	// Property accessors

	/** See {@link #unionKey}. */
	@Column(name = "Union_Key")
	public String getUnionKey() {
		return unionKey;
	}

	/** See {@link #unionKey}. */
	public void setUnionKey(String unionKey) {
		this.unionKey = unionKey;
	}

	/** See {@link #contractCode}. */
	@Column(name = "Contract_Code")
	public String getContractCode() {
		return contractCode;
	}

	/** See {@link #contractCode}. */
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}

	/** See {@link #gsContractCode}. */
	@Column(name = "GS_Contract_Code")
	public String getGsContractCode() {
		return gsContractCode;
	}

	/** See {@link #gsContractCode}. */
	public void setGsContractCode(String gsContractCode) {
		this.gsContractCode = gsContractCode;
	}

	/** See {@link #gsUnionType}. */
	@Column(name = "GS_Union_Type")
	public String getGsUnionType() {
		return gsUnionType;
	}

	/** See {@link #gsUnionType}. */
	public void setGsUnionType(String gsUnionType) {
		this.gsUnionType = gsUnionType;
	}
}
