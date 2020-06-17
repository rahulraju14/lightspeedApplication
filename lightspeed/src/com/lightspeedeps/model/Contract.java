package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.ContractType;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Contract entity.
 */
@NamedQueries ({
	@NamedQuery(name=Contract.FIND_CONTRACTS_INCLUDED_WITH, query = "from Contract c where ( mod( c.includedWith, (:mask * 2)) / :mask) > 0")
})
@Entity
@Table(name = "contract")
public class Contract extends PersistentObject<Contract> implements Comparable<Contract> {
	private static final long serialVersionUID = 1L;

	//Named queries
	public static final String FIND_CONTRACTS_INCLUDED_WITH = "findContractsIncludedWith";

	/** Key to sort by name */
	public static final String SORTKEY_NAME = "name";

	/** Key to sort by date, last-name, first-name (the default compareTo sequence). */
	public static final String SORTKEY_YEAR = "year";

	/** key to sort by Union number */
	public static final String SORTKEY_UNION = "union";

	/** Contracts whose contractKey begins with this prefix will be included when
	 * processing Non-Union timecards. */
	public static final String CONTRACT_CODE_NON_UNION = "NU";

	// Fields

	/** The Production that has "defined" this agreement.  For all industry-standard contracts,
	 * this will reference the "System" production (id=1). */
	private Production production;

	/** Indicates the type of this agreement; currently that means either a full contract
	 * or a side-letter. */
	private ContractType type;

	/** A unique business-logic key for the Contract. This typically incorporates the year the
	 * agreement took effect and the union it relates to, e.g., "12LA80". */
	private String contractKey;

	/** The name of the Contract for presentation to the user. */
	private String name;

	/** The first year the Contract is effective. */
	private Integer year;

	/** The starting date of the Contract -- the first day it is effective. */
	private Date startDate;

	/** The ending date of the Contract -- the last day it is effective. */
	private Date endDate;

	/** The Lightspeed business key used for the union related to the Contract,
	 * e.g., "80" or "600C". */
	private String unionKey;

	/** The code used in the PayRate table to reference this contract.  This code
	 * will NOT have a year reference (as the contractKey field does), and typically
	 * is hyphenated to make it more readable in the PayRate spreadsheet, which is
	 * the source document for the PayRate table. For example, "LA-80" or "TM-399T". */
	private String contractCode;

	/** Defines the bit in {@link #includedWith} which indicates that contract
	 * should also be added when this contract is added to a Production. */
	private Byte includesMask;

	/** A set of bit flags indicating which other contract(s) will include this one.
	 * The bits are defined by the {@link #includesMask} in other contract entries,
	 * e.g., HBO = 1. */
	private Integer includedWith;

//	/** The List of Production`s associated with this contract. This association
//	 * is supposed to indicate that the Production is a "signatory" to the
//	 * contract (agreement). The presence of the association will cause our
//	 * payroll rules engine to includes rules specific to this Contract. It may
//	 * also affect the available list of job classifications (occupations) on
//	 * the Start Form page. */
//	private List<Production> productions;

	/** Used to track row selection (highlighting) on Contract lists. */
	@Transient
	private boolean selected;

	/** Used to track row check-box marks on Contract lists. */
	@Transient
	private boolean checked;

	// Constructors

	/** default constructor */
	public Contract() {
	}

	// Property accessors

	/** See {@link #production} */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	/** See {@link #production} */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**See {@link #type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Type", length = 20)
	public ContractType getType() {
		return type;
	}
	/**See {@link #type}. */
	public void setType(ContractType type) {
		this.type = type;
	}

	/**See {@link #contractKey}. */
	@Column(name = "Contract_Key", length = 20)
	public String getContractKey() {
		return contractKey;
	}
	/**See {@link #contractKey}. */
	public void setContractKey(String contractKey) {
		this.contractKey = contractKey;
	}

	/** See {@link #name} */
	@Column(name = "Name", length = 100)
	public String getName() {
		return name;
	}
	/** See {@link #name} */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #year} */
	@Column(name = "Year")
	public Integer getYear() {
		return year;
	}
	/** See {@link #year} */
	public void setYear(Integer year) {
		this.year = year;
	}

	/** See {@link #startDate} */
	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", length = 10)
	public Date getStartDate() {
		return startDate;
	}
	/** See {@link #startDate} */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/** See {@link #endDate} */
	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 10)
	public Date getEndDate() {
		return endDate;
	}
	/** See {@link #endDate} */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #unionKey} */
	@Column(name = "Union_Code", length = 20)
	public String getUnionKey() {
		return unionKey;
	}
	/** See {@link #unionKey} */
	public void setUnionKey(String unionCode) {
		unionKey = unionCode;
	}

	/** See {@link #contractCode} */
	@Column(name = "Contract_Code", length = 30)
	public String getContractCode() {
		return contractCode;
	}
	/** See {@link #contractCode} */
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}

//	/**See {@link #productions}. */
//	@ManyToMany(
//			targetEntity = Production.class,
//			cascade = {CascadeType.PERSIST, CascadeType.MERGE},
//			fetch = FetchType.LAZY,
//			mappedBy = "contracts"
//		)
//	public List<Production> getProductions() {
//		return productions;
//	}
//	/**See {@link #productions}. */
//	public void setProductions(List<Production> prods) {
//		productions = prods;
//	}

	/** See {@link #includesMask}. */
	@Column(name = "Includes_Mask", nullable = false)
	public Byte getIncludesMask() {
		return includesMask;
	}
	/** See {@link #includesMask}. */
	public void setIncludesMask(Byte includesMask) {
		this.includesMask = includesMask;
	}

	/** See {@link #includeWith}. */
	@Column(name = "Included_With", nullable = false)
	public Integer getIncludedWith() {
		return includedWith;
	}
	/** See {@link #includeWith}. */
	public void setIncludedWith(Integer includeWith) {
		includedWith = includeWith;
	}

//	/**
//	 * @param c Contract to be checked for inclusion.
//	 * @return True if this contract's {@link #includesMask} is present in c's
//	 * {@link #includedWith} list of bits, meaning that the contract 'c' should
//	 * be included if this contract is being added.
//	 */
//	public boolean includes(Contract c) {
//		return (includesMask & c.includedWith) != 0;
//	}

	/**See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/**See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**See {@link #checked}. */
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/**See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * Default compareTo - uses database id for equality check, then week-end
	 * Date, followed by adjusted status, last name, and first name for comparison.
	 *
	 * @param other
	 * @return Standard compare result -1/0/1.
	 */
	@Override
	public int compareTo(Contract other) {
		int ret = 1;
		if (other != null) {
			if (getId().equals(other.getId())) {
				return 0; // same entity
			}
			ret = getName().compareTo(other.getName());
			if (ret == 0) { // Same week-ending date & adjusted; check name
				ret = getYear().compareTo(other.getYear());
				if (ret == 0) {
					// identical fields, sort by id so order will be consistent from one sort to the next
					ret = id.compareTo(other.id);
				}
			}
		}
		return ret;
	}

	/**
	 * Compare using the specified field, with the specified ordering.
	 *
	 * @param other The Contract to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(Contract other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 *
	 * @param other The WeeklyTimecard to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(Contract other, String sortField) {
		int ret=0;
		if (other == null) {
			return 1;
		}
		else if (sortField == null || sortField.equals(SORTKEY_NAME) ) {
			return compareTo(other); // name = default comparison
		}

		if (sortField.equals(SORTKEY_YEAR) ) {
			ret = NumberUtils.compare(getYear(), other.getYear());
		}
		else if (sortField.equals(SORTKEY_UNION)) {
			ret = StringUtils.compareIgnoreCase(getUnionKey(), other.getUnionKey());
		}

		if (ret == 0) { // this will also get run if sortField == SORTKEY_NAME
			ret = compareTo(other);
			if (ret == 0) { // same name, compare date & position
				ret = NumberUtils.compare(getYear(), other.getYear());
			}
		}

		return ret;
	}

}
