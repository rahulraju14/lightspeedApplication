package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.object.ApproverChainRoot;

/** ApprovalPathAnchor entity is considered as the first approver in a Department chain.
 * The ApprovalPathAnchor holds an Approver instance that indicates the start of the
 * chain.
 * @author root
 *
 */
@NamedQueries ({
	@NamedQuery(name=ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_AND_APPROVAL_PATH,
			query = "from ApprovalPathAnchor a where a.department.id =:departmentId and a.approvalPath.id =:approvalPathId"),
	@NamedQuery(name=ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_PROJECT_APPROVAL_PATH,
			query = "from ApprovalPathAnchor a where a.production = :production and a.department =:department and a.approvalPath.id =:approvalPathId and a.project =:project"),
	@NamedQuery(name=ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_APPROVAL_PATH,
			query = "from ApprovalPathAnchor a where a.production = :production and a.department =:department and a.approvalPath.id =:approvalPathId")
})

@Entity
@Table (name = "approval_path_anchor")
public class ApprovalPathAnchor extends ApprovalAnchorMapped implements ApproverChainRoot {

	private static final long serialVersionUID = 7229755531406229041L;

	public static final String GET_ANCHOR_BY_DEPARTMENT_AND_APPROVAL_PATH = "getAnchorByDepartmentAndApprovalPath";

	public static final String GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_PROJECT_APPROVAL_PATH = "getAnchorByDepartmentProductionProjectApprovalPath";

	public static final String GET_ANCHOR_BY_DEPARTMENT_PRODUCTION_APPROVAL_PATH = "getAnchorByDepartmentProductionApprovalPath";

	private ApprovalPath approvalPath;

	private ApproverGroup approverGroup;

	public ApprovalPathAnchor() {
		super();
	}

	/** See {@link #approvalPath}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approval_Path_Id")
	public ApprovalPath getApprovalPath() {
		return approvalPath;
	}
	/** See {@link #approvalPath}. */
	public void setApprovalPath(ApprovalPath approvalPath) {
		this.approvalPath = approvalPath;
	}

	/** See {@link #approverGroup}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approver_Group_Id")
	public ApproverGroup getApproverGroup() {
		return approverGroup;
	}
	/** See {@link #approverGroup}. */
	public void setApproverGroup(ApproverGroup approverGroup) {
		this.approverGroup = approverGroup;
	}

}
