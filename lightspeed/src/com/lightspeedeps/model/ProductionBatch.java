package com.lightspeedeps.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * ProductionBatch entity. This object describes one template to be used to
 * create a WeeklyBatch each week. One or more StartForm`s may be associated
 * with this object; when a WeeklyTimecard is generated from one of these
 * StartForm`s, it is placed into a WeeklyBatch with the same name as this
 * ProductionBatch.
 */
@Entity
@Table(name = "production_batch")
public class ProductionBatch extends PersistentObject<ProductionBatch> implements Comparable<ProductionBatch> {
	private static final long serialVersionUID = 1L;

	public static final int MAX_NAME_LENGTH = 30;

	// Fields

	/** The Production that this ProductionBatch is associated with. */
	private Production production;

	/** The Project associated with this ProductionBatch.  Only used for Commercial
	 * productions.  For TV & Feature productions, this will be null. */
	private Project project;

	/** The name of the batch.  This is the name that will be assigned to
	 * the WeeklyBatch created to hold WeeklyTimeCard`s generated from the
	 * StartForm`s associated with this ProductionBatch. */
	private String name;

	// This relation is implied, but not used.  It is the collection of all
	// WeeklyBatch objects that have been created from this ProductionBatch.
//	private Set<WeeklyBatch> weeklyBatchs = new HashSet<WeeklyBatch>(0);

	/** The unordered collection of StartForm`s associated with this ProductionBatch. */
	private Set<Employment> employments = new HashSet<Employment>(0);

	// Constructors

	/** default constructor */
	public ProductionBatch() {
	}

	/**
	 * Our normal constructor.
	 *
	 * @param prod The Production this batch is associated with.
	 * @param proj The Project this batch is associated with -- null for TV
	 *            and Features; set to a specific Project for Commercials.
	 * @param pname The name of the batch.
	 */
	public ProductionBatch(Production prod, Project proj, String pname) {
		production = prod;
		project = proj;
		name = pname;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	@Column(name = "Name", nullable = false, length = 30)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "productionBatch")
//	public Set<WeeklyBatch> getWeeklyBatchs() {
//		return this.weeklyBatchs;
//	}
//	public void setWeeklyBatchs(Set<WeeklyBatch> weeklyBatchs) {
//		this.weeklyBatchs = weeklyBatchs;
//	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ProductionBatch other) {
		if (other == null)
			return 1;
		return getName().compareTo(other.getName());
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "productionBatch")
	public Set<Employment> getEmployments() {
		return employments;
	}
	public void setEmployments(Set<Employment> employments) {
		this.employments = employments;
	}

}
