package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;

/**
 * SpecialRule entity.
 */
@Entity
@Table(name = "special_rule", uniqueConstraints = {
		@UniqueConstraint(columnNames = "Long_Name"), @UniqueConstraint(columnNames = "Rule_Key") })
public class SpecialRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = - 3722418710563792844L;

	// Fields

	private String longName;

	// Constructors

	/** default constructor */
	public SpecialRule() {
		setType(RuleType.SP);
	}

//	/** full constructor */
//	public SpecialRule(Integer productionId, String ruleKey, String longName, String description,
//			String notes) {
//		super(productionId, ruleKey, description, notes);
//		setType(RuleType.SP);
//		this.longName = longName;
//	}

	// Property accessors

	@Column(name = "Long_Name", unique = true, nullable = false, length = 50)
	public String getLongName() {
		return longName;
	}
	public void setLongName(String longName) {
		this.longName = longName;
	}

}
