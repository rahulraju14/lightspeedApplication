package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.ApproverAuditType;

@NamedQueries ({
	@NamedQuery(name=ApproverAudit.GET_DELETED_APPROVERS_BY_APPROVAL_PATH,
			query ="from ApproverAudit a where a.production =:production and a.approvalPath =:approvalPath"),
	@NamedQuery(name=ApproverAudit.GET_OLD_PRODUCTION_APPROVERS,
			query ="from ApproverAudit a where a.production =:production and a.approvalPath =:approvalPath and a.department is NULL"),
	@NamedQuery(name=ApproverAudit.GET_OLD_DEPARTMENT_APPROVERS,
			query ="from ApproverAudit a where a.production =:production and a.approvalPath =:approvalPath and a.department =:department"),
	@NamedQuery(name=ApproverAudit.GET_DELETED_APPROVER_BY_APPROVAL_PATH_CONTACT,
			query ="from ApproverAudit a where a.approvalPath =:approvalPath and a.oldContact =:contact and a.approverGroup is NULL"),
	@NamedQuery(name=ApproverAudit.GET_OLD_GROUP_APPROVERS, query ="from ApproverAudit a where a.production =:production and a.approverGroup =:approverGroup"),
	@NamedQuery(name=ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE,
		query ="from ApproverAudit a where a.production =:production and a.approvalPath =:approvalPath and a.auditType =:auditType and a.approverGroup is NULL and a.department is NULL order by a.position"),
	@NamedQuery(name=ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APP_GROUP_AUDIT_TYPE,
		query ="from ApproverAudit a where a.production =:production and a.approverGroup =:approverGroup and a.auditType ='APP_GROUP' and a.approvalPath is NULL order by a.position"),
		@NamedQuery(name=ApproverAudit.GET_DELETED_APPROVERS_BY_PATH_DEPT_AUDIT_TYPE,
		query ="from ApproverAudit a where a.production =:production and a.approvalPath =:approvalPath and a.auditType ='PATH_ANCHOR' and a.department is NOT NULL and a.approverGroup is NOT NULL"),
})

@Entity
@Table(name = "approver_audit")
public class ApproverAudit extends PersistentObject<ApproverAudit> {

	private static final long serialVersionUID = 3181660646183591676L;

	public static final String GET_DELETED_APPROVERS_BY_APPROVAL_PATH = "getDeletedApproversByApprovalPath";
	public static final String GET_OLD_PRODUCTION_APPROVERS = "getOldProductionApprovers";
	public static final String GET_OLD_DEPARTMENT_APPROVERS= "getOldDepartmentApprovers";
	public static final String GET_DELETED_APPROVER_BY_APPROVAL_PATH_CONTACT= "getDeletedApproverByApprovalPathContact";
	public static final String GET_OLD_GROUP_APPROVERS = "getOldGroupApprovers";
	public static final String GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE= "getDeletedApproversByProdApprovalPathAuditType";
	public static final String GET_DELETED_APPROVERS_BY_PROD_APP_GROUP_AUDIT_TYPE= "getDeletedApproversByProdAppGroupAuditType";
	public static final String GET_DELETED_APPROVERS_BY_PATH_DEPT_AUDIT_TYPE= "getDeletedApproversByPathDeptAuditType";

	/** The Production this approver is associated with. */
	private Production production;

	private ApproverAuditType auditType;

	/** Department for ApprovalPathAnchor. */
	private Department department;

	private ApprovalPath approvalPath;

	private ApproverGroup approverGroup;

	/** The Position of Approver in Approval hierarchy, e.g. first position means first Approver of ApprovalPath or ApproverGroup . */
	private Integer position;

	/** The old approverId of contact in the Approval Path. */
	private Integer oldApproverId;

	/** Old Approver's Contact in the Approval Path. */
	private Contact oldContact;

	/** The new approverId of old contact in the Approval Path, null if contact is removed or replaced in updated Path. */
	private Integer newApproverId;

	/** New Contact at the old Contact's Position in the Approval Path. */
	private Contact newContact;

	/** The approverId of new contact at the old Contact's Position in the Approval Path. */
	private Integer newContactApproverId;

	public ApproverAudit() {
		super();
	}

	public ApproverAudit(Production production, ApproverAuditType auditType, Department department, ApprovalPath approvalPath,
			ApproverGroup approverGroup, Integer position, Integer oldApproverId, Contact oldContact) {
		super();
		this.production = production;
		this.auditType = auditType;
		this.department = department;
		this.approvalPath = approvalPath;
		this.approverGroup = approverGroup;
		this.position = position;
		this.oldApproverId = oldApproverId;
		this.oldContact = oldContact;
	}

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #auditType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Audit_Type" , nullable = true, length = 30)
	public ApproverAuditType getAuditType() {
		return auditType;
	}
	/** See {@link #auditType}. */
	public void setAuditType(ApproverAuditType auditType) {
		this.auditType = auditType;
	}

	/** See {@link #department}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id")
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department department) {
		this.department = department;
	}

	/** See {@link #approvalPath}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approval_Path_Id", nullable = true)
	public ApprovalPath getApprovalPath() {
		return approvalPath;
	}
	/** See {@link #approvalPath}. */
	public void setApprovalPath(ApprovalPath approvalPath) {
		this.approvalPath = approvalPath;
	}

	/** See {@link #approverGroup}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approver_Group_Id", nullable = true)
	public ApproverGroup getApproverGroup() {
		return approverGroup;
	}
	/** See {@link #approverGroup}. */
	public void setApproverGroup(ApproverGroup approverGroup) {
		this.approverGroup = approverGroup;
	}

	/** See {@link #position}. */
	@Column(name = "Position", nullable = true)
	public Integer getPosition() {
		return position;
	}
	/** See {@link #position}. */
	public void setPosition(Integer position) {
		this.position = position;
	}

	/** See {@link #oldApproverId}. */
	@Column(name = "Old_Approver_Id", nullable = true)
	public Integer getOldApproverId() {
		return oldApproverId;
	}
	/** See {@link #oldApproverId}. */
	public void setOldApproverId(Integer oldApproverId) {
		this.oldApproverId = oldApproverId;
	}

	/** See {@link #oldContact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Old_Contact_Id", nullable = true)
	public Contact getOldContact() {
		return oldContact;
	}
	/** See {@link #oldContact}. */
	public void setOldContact(Contact oldContact) {
		this.oldContact = oldContact;
	}

	/** See {@link #newApproverId}. */
	@Column(name = "New_Approver_Id", nullable = true)
	public Integer getNewApproverId() {
		return newApproverId;
	}
	/** See {@link #newApproverId}. */
	public void setNewApproverId(Integer newApproverId) {
		this.newApproverId = newApproverId;
	}

	/** See {@link #newContact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "New_Contact_Id", nullable = true)
	public Contact getNewContact() {
		return newContact;
	}
	/** See {@link #newContact}. */
	public void setNewContact(Contact newContact) {
		this.newContact = newContact;
	}

	/** See {@link #newContactApproverId}. */
	@Column(name = "New_Contact_Approver_Id", nullable = true)
	public Integer getNewContactApproverId() {
		return newContactApproverId;
	}
	/** See {@link #newContactApproverId}. */
	public void setNewContactApproverId(Integer newContactApproverId) {
		this.newContactApproverId = newContactApproverId;
	}

	/** See {@link #project}. */
	/*@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	*//** See {@link #project}. *//*
	public void setProject(Project project) {
		this.project = project;
	}*/

}
