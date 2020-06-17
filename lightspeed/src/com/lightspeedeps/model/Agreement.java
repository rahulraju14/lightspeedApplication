package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Agreement entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "agreement")
public class Agreement extends PersistentObject<Agreement> implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// Fields

	/** */
	private Production production;
	private String name;
	private String type;
	private String productionType;
	private String groupName;
	private String subGroup;
	private Integer listOrder;
	private String occUnion;

	// Constructors

	/** default constructor */
	public Agreement() {
	}

	/** full constructor */
	public Agreement(Production production, String name, String type, String productionType,
			String groupName, String subGroup, Integer listOrder, String occUnion) {
		this.production = production;
		this.name = name;
		this.type = type;
		this.productionType = productionType;
		this.groupName = groupName;
		this.subGroup = subGroup;
		this.listOrder = listOrder;
		this.occUnion = occUnion;
	}

	// Property accessors
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}

	public void setProduction(Production production) {
		this.production = production;
	}

	@Column(name = "Name", length = 50)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Type", length = 20)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "ProductionType", length = 20)
	public String getProductionType() {
		return productionType;
	}

	public void setProductionType(String productionType) {
		this.productionType = productionType;
	}

	@Column(name = "Group_Name", length = 50)
	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Column(name = "Sub_Group", length = 50)
	public String getSubGroup() {
		return subGroup;
	}

	public void setSubGroup(String subGroup) {
		this.subGroup = subGroup;
	}

	@Column(name = "List_Order")
	public Integer getListOrder() {
		return listOrder;
	}

	public void setListOrder(Integer listOrder) {
		this.listOrder = listOrder;
	}

	@Column(name = "Occ_Union", length = 20)
	public String getOccUnion() {
		return occUnion;
	}

	public void setOccUnion(String occUnion) {
		this.occUnion = occUnion;
	}

}
