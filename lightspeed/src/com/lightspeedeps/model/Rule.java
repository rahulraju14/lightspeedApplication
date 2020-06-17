package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.lightspeedeps.type.RuleType;

/**
 * MpvRule entity.
 */
@MappedSuperclass
public class Rule extends PersistentObject<Rule> {

	private static final long serialVersionUID = 6462726301546417138L;

	// Fields

	/** What production is this rule for? (1=System rule) */
	private Integer productionId;

	/** The identifying key for this rule. Usually unique. */
	private String ruleKey;

	/** A human-readable description of the rule. */
	private String description;

	/** Additional usage notes, contract references, etc. */
	private String notes;

	/** The RuleType of this rule, set in the sub-class constructor. */
	@Transient
	private RuleType type;

	// Constructors

	/** default constructor */
	public Rule() {
	}

//	/** full constructor */
//	public Rule(Integer productionId, String ruleKey, String description, String notes) {
//		this.productionId = productionId;
//		this.ruleKey = ruleKey;
//		this.description = description;
//		this.notes = notes;
//	}

	@Column(name = "Production_Id")
	public Integer getProductionId() {
		return productionId;
	}
	public void setProductionId(Integer productionId) {
		this.productionId = productionId;
	}

	@Column(name = "Rule_Key", unique = true, nullable = false, length = 20)
	public String getRuleKey() {
		return ruleKey;
	}
	public void setRuleKey(String ruleKey) {
		this.ruleKey = ruleKey;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "Notes", length = 1000)
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}

	/**See {@link #type}. */
	@Transient
	public RuleType getType() {
		return type;
	}
	/**See {@link #type}. */
	public void setType(RuleType type) {
		this.type = type;
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "key=" + (getRuleKey()==null ? "null" : getRuleKey());
		s += "]";
		return s;
	}

	public String toAudit() {
		String s = "";
		//s += "Rule=";
		s += getType().getBrief() + "=";
		s += getRuleKey();
		if (getDescription() != null && getDescription().length() > 0) {
			s += " [" + getDescription() + "]";
		}
		return s;
	}

}
