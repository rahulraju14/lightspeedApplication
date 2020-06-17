package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * ApprovalAnchor entity. This is the starting point for chains of Approver`s
 * for a particular Department, within either a Production or a Project.
 */
@Entity
@Table(name = "approval_anchor")
public class ApprovalAnchor extends ApprovalAnchorMapped {

	private static final long serialVersionUID = 1L;

	// Constructors

	/** default constructor */
	public ApprovalAnchor() {
	}

	/** full constructor */
	public ApprovalAnchor(Production production, Project project, Department department,
			Approver approver) {
		super (production, project, department, approver);
	}
}