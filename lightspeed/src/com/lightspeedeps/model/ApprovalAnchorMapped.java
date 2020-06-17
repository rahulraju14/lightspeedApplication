package com.lightspeedeps.model;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.lightspeedeps.object.ApproverChainRoot;

@MappedSuperclass
public class ApprovalAnchorMapped extends PersistentObject<ApprovalAnchorMapped> implements ApproverChainRoot{

	private static final long serialVersionUID = -5419087012535614061L;

	/** The Production this approver chain is associated with. */
	private Production production;

	/** The Project this approver chain is associated with. */
	private Project project;

	/** The Department this approver chain is for, or null if this
	 * is a Contact chain anchor. */
	private Department department;

	/** The first (and possibly only) Approver in the chain. */
	private Approver approver;

	// Property accessors

	public ApprovalAnchorMapped() {

	}

	public ApprovalAnchorMapped(Production production, Project project,
		Department department, Approver approver) {
		this.production = production;
		this.project = project;
		this.department = department;
		this.approver = approver;
	}

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id")
	public Department getDepartment() {
		return department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}

	@Override
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "First_Approver_Id")
	public Approver getApprover() {
		return approver;
	}
	@Override
	public void setApprover(Approver approver) {
		this.approver = approver;
	}

	@Transient
	/** Synonym for getApprover() */
	public Approver getFirstApprover() {
		return getApprover();
	}
	/** Synonym for setApprover() */
	public void setFirstApprover(Approver approver) {
		setApprover(approver);
	}

}
