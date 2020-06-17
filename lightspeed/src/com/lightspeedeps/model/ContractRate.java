package com.lightspeedeps.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 * ContractRate entity. Each instance is related to a specific contract, and a
 * specific type of rate (indicated by the {@link #rateKey}, and a specific
 * {@link #category} within that rateKey. It specifies the {@link #rate} (dollar
 * amount) for that combination of contract, rateKey, and category. For the
 * Canadian UDA contract, the category is the "performance category" of the UDA
 * contract.
 * <p>
 * These entities are used by the Hours-to-Gross (HTG) engine when computing the
 * gross pay for a timecard or contract.
 */
@NamedQueries({
	@NamedQuery(name=ContractRate.GET_RATE_BY_CONTRACT_KEY_AND_CATEGORY, query = "from ContractRate cr where contractCode = :contractCode and rateKey = :rateKey and category = :category"),
})
@Entity
@Table(name = "contract_rate")
public class ContractRate extends PersistentObject<ContractRate> implements Serializable {

	/** serial version id */
	private static final long serialVersionUID = 7888220710024464515L;

	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(WeeklyTimecard.class);

	/** Name of NamedQuery to retrieve objects by contract code and rate key. */
	public static final String  GET_RATE_BY_CONTRACT_KEY_AND_CATEGORY = "getRateByContractKeyAndCategory";

	// Fields

	/** Contract code; this matches the contractCode field in both the Contract
	 * and Occupation model classes. It identifies the specific contract (e.g.,
	 * Local 399 Drivers Commercial contract) to which this rate instance applies. */
	private String contractCode;

	/**  first effective date of this rate */
	private Date startDate;

	/**  last effective date of this rate */
	private Date endDate;

	/**  paragraph, article, etc. where rate is defined */
	private String reference;

	/**  key/code unique per contract */
	private String rateKey;

	/**  textual description of rate */
	private String description;

	/** The category of the rate - an additional qualifier; for UDA
	 * this represents the performance category (i.e., occupation). */
	private String category;

	/**  rate (dollars) for this entry; may be hourly or daily or
	 * any other unit as appropriate. */
	private BigDecimal rate;

	// Constructors

	/** default constructor */
	public ContractRate() {
	}

	// Property accessors

	/** See {@link #contractCode}. */
	@Column(name = "Contract_Code", nullable = false, length = 20)
	public String getContractCode() {
		return this.contractCode;
	}
	/** See {@link #contractCode}. */
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}

	/** See {@link #startDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", nullable = false, length = 10)
	public Date getStartDate() {
		return this.startDate;
	}
	/** See {@link #startDate}. */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/** See {@link #endDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", nullable = false, length = 10)
	public Date getEndDate() {
		return this.endDate;
	}
	/** See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #reference}. */
	@Column(name = "Reference", length = 30)
	public String getReference() {
		return this.reference;
	}
	/** See {@link #reference}. */
	public void setReference(String reference) {
		this.reference = reference;
	}

	/** See {@link #rateKey}. */
	@Column(name = "Rate_Key", nullable = false, length = 30)
	public String getRateKey() {
		return this.rateKey;
	}
	/** See {@link #rateKey}. */
	public void setRateKey(String rateKey) {
		this.rateKey = rateKey;
	}

	/** See {@link #description}. */
	@Column(name = "Description", length = 100)
	public String getDescription() {
		return this.description;
	}
	/** See {@link #description}. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #category}. */
	@Column(name = "Category", length = 30)
	public String getCategory() {
		return category;
	}
	/** See {@link #category}. */
	public void setCategory(String category) {
		this.category = category;
	}

	/** See {@link #rate}. */
	@Column(name = "Rate", precision = 8)
	public BigDecimal getRate() {
		return this.rate;
	}
	/** See {@link #rate}. */
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

}
