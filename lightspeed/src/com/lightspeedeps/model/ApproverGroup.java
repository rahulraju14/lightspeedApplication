package com.lightspeedeps.model;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.object.ApproverChainRoot;

@NamedQueries ({
	@NamedQuery(name=ApproverGroup.GET_APPROVER_GROUP_BY_PRODUCTION, query ="from ApproverGroup a where a.production =:production"),
	@NamedQuery(name=ApproverGroup.GET_APPROVER_GROUP_BY_PROJECT, query ="from ApproverGroup a where a.project =:project"),
	@NamedQuery(name=ApproverGroup.GET_APPROVER_GROUP_NAME_BY_PROJECT, query ="from ApproverGroup a where a.groupName =:groupName and a.project =:project"),
	@NamedQuery(name=ApproverGroup.GET_APPROVER_GROUP_BY_NAME_AND_PRODUCTION, query ="from ApproverGroup a where a.groupName =:groupName and a.production =:production"),
	@NamedQuery(name=ApproverGroup.GET_DISTINCT_APPROVER_GROUP_BY_APPROVAL_PATH, query = "select distinct a.approverGroup from ApprovalPathAnchor a where a.approvalPath =:approvalPath")
})
@Entity
@Table (name = "approver_group")
public class ApproverGroup extends PersistentObject<ApproverGroup> implements ApproverChainRoot {

	private static final long serialVersionUID = 9208624639283721716L;

	public static final String GET_APPROVER_GROUP_BY_PRODUCTION = "getApproverGroupByProduction";

	public static final String GET_APPROVER_GROUP_BY_PROJECT = "getApproverGroupByProject";

	public static final String GET_APPROVER_GROUP_BY_NAME_AND_PRODUCTION = "getApproverGroupByNameAndProduction";

	public static final String GET_APPROVER_GROUP_NAME_BY_PROJECT = "getApproverGroupNameByProductionProject";

	public static final String GET_DISTINCT_APPROVER_GROUP_BY_APPROVAL_PATH = "getDistinctApproverGroupByApprovalPath";

	/** The Production to which this ApproverGroup belongs. */
	private Production production;

	/** Approver Group name as specified by the user from the drop down, or newly created one */
	private String groupName;

	/** Boolean field used to recognize approval action.
	 * True if it follows Pool method and false for Linear Hierarchy. */
	private boolean usePool;

	/** The Project to which this ApproverGroup belongs; only used for Commercial productions. It
	 * should be null for TV or Feature film productions. */
	private Project project;

	/** The first Approver in the chain. */
	private Approver approver;

	/** This is meant to store the collection of Contact's which have been added by an admin to the
	 *  "approver pool" list of the Approver Group when in "pool" (not "linear") mode. */
	private Set<Contact> groupApproverPool;

	/**
	 * Default Constructor
	 */
	public ApproverGroup() {
	}

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #groupName}. */
	@Column(name = "Group_Name", nullable = true, length = 50)
	public String getGroupName() {
		return groupName;
	}
	/** See {@link #groupName}. */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/** See {@link #usePool}. */
	@Column(name = "Use_Pool", nullable = false)
	public boolean getUsePool() {
		return usePool;
	}
	/** See {@link #usePool}. */
	public void setUsePool(boolean usePool) {
		this.usePool = usePool;
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

	/** See {@link #approver}. */
	@Override
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "First_Approver_Id")
	public Approver getApprover() {
		return approver;
	}
	/** See {@link #approver}. */
	@Override
	public void setApprover(Approver firstApprover) {
		this.approver = firstApprover;
	}

	/** See {@link #groupApproverPool}. */
	@ManyToMany(
			targetEntity=Contact.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approver_group_contact_pool",
			joinColumns = {@JoinColumn(name = "Approver_Group_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Contact_Id")}
			)
	public Set<Contact> getGroupApproverPool() {
		return groupApproverPool;
	}
	/** See {@link #groupApproverPool}. */
	public void setGroupApproverPool(Set<Contact> groupApproverPool) {
		this.groupApproverPool = groupApproverPool;
	}

}
