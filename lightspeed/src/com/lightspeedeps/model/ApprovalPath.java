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

/**
 * ApprovalPath entity is used to define (and name) a sequence of approvers (the
 * "approval path") required for one or more Documents. It also stores a
 * Production's first approver for the path, and the final approver for the pool
 * paths, and is also mapped with DocumentChain to locate the path for a
 * specific ContactDocument.
 * <p>
 * A path may be either "a linear path" or "a pool path"; a pool path has
 * {@link #usePool} set to true, and should have a non-empty set of Contact`s
 * in {@link #approverPool}.
 */
@NamedQueries ({
	@NamedQuery(name=ApprovalPath.GET_APPROVAL_PATH_BY_PRODUCTION, query ="from ApprovalPath a where a.production =:production"),
	@NamedQuery(name=ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, query ="from ApprovalPath a where a.project =:project"),
	@NamedQuery(name=ApprovalPath.GET_APPROVAL_PATH_NAME_BY_PROJECT, query ="from ApprovalPath a where a.name =:pathName and a.project =:project"),
	@NamedQuery(name=ApprovalPath.GET_APPROVAL_PATH_BY_NAME_AND_PRODUCTION, query ="from ApprovalPath a where a.name =:pathName and a.production =:production")
})
@Entity
@Table (name = "approval_path")
public class ApprovalPath extends PersistentObject<ApprovalPath> implements ApproverChainRoot {

	private static final long serialVersionUID = 6130089966982736215L;

	public static final String GET_APPROVAL_PATH_BY_PRODUCTION = "getApprovalPathByProduction";

	public static final String GET_APPROVAL_PATH_BY_NAME_AND_PRODUCTION = "getApprovalPathByNameAndProduction";

	public static final String GET_APPROVAL_PATH_NAME_BY_PROJECT = "getApprovalPathNameByProductionProject";

	public static final String GET_APPROVAL_PATH_BY_PROJECT = "getApprovalPathByProject";

	/** Approval path name as specified by the user from the drop down, or newly created one */
	private String name;

	/** DocumentAction enum type, used to know what type of approval cycle document will undergo  */
	//private DocumentAction employeeAction;

	/** Boolean field used to recognize approval action */
	private boolean usePool;

	/** Boolean field used to know the selected document needs to under go approval process or not */
	//private boolean approvalRequired;

	/** A reference to the first (earliest) Approver in the chain of approvers for a linear
	 * approval path.  It is considered the 'root' of the approval chain. For a Pool approval path,
	 * this field is not used, and should be null. */
	private Approver approver;

	/** The Production to which this ApprovalPath belongs. */
	private Production production;

	/** For POOL paths only, this is the optional final approver.  When this is null,
	 * then the first approval by anyone in the pool constitutes final approval of the
	 * document.  When this is not null, an approval by anyone in the pool moves
	 * the document to the 'queue' of this approver.  For a linear approval path, this
	 * value will not be used and should be null. */
	private Approver finalApprover;

	/** This is meant to store the collection of Contact's which have been added by an admin to the
	 *  "approver pool" list in the lower part of the Approval Path screen when in "pool" (not "linear") mode. */
	private Set<Contact> approverPool;

	/** The Project to which this ApprovalPath belongs; only used for Commercial productions. It
	 * should be null for TV or Feature film productions. */
	private Project project;

	/** This is used to associate the document chains with approval path */
	private Set<DocumentChain> documentChains;

	/** Indicates, for a Pool path, if there is a Final Approver specified. If true, after anyone in the
	 * pool approves a document, the document moves to the queue of the final approver. If false, then
	 * after anyone in the pool approves a document, the document is marked as Approved.  This boolean
	 * is not used for linear paths. */
	private boolean useFinalApprover;

	/** This is meant to store the collection of Contact's which have been added by an admin to the
	 *  "Document Editors" list in the lower part of the Approval Path screen. */
	private Set<Contact> editors;

	/**
	 * Default Constructor
	 */
	public ApprovalPath() {
	}

	/** See {@link #name}. */
	@Column(name = "Name", nullable = true, length = 50)
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #employeeAction}. */
	/*@Enumerated(EnumType.STRING)
	@Column(name = "Employee_Action" , nullable = true, length = 30)
	public DocumentAction getEmployeeAction() {
		return employeeAction;
	}
	*//** See {@link #employeeAction}. *//*
	public void setEmployeeAction(DocumentAction employeeAction) {
		this.employeeAction = employeeAction;
	}*/

	/** See {@link #usePool}. */
	@Column(name = "Use_Pool", nullable = false)
	public boolean getUsePool() {
		return usePool;
	}
	/** See {@link #usePool}. */
	public void setUsePool(boolean usePool) {
		this.usePool = usePool;
	}

	/** See {@link #approvalRequired}. */
	/*@Column(name = "Approval_Required", nullable = false)
	public boolean getApprovalRequired() {
		return approvalRequired;
	}
	*//** See {@link #approvalRequired}. *//*
	public void setApprovalRequired(boolean approvalRequired) {
		this.approvalRequired = approvalRequired;
	}*/

	/** See {@link #approver}. */
	@Override
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approver_Id")
	public Approver getApprover() {
		return approver;
	}
	/** See {@link #approver}. */
	@Override
	public void setApprover(Approver approver) {
		this.approver = approver;
	}

	/** See {@link #approverPool}. */
	@ManyToMany(
			targetEntity=Contact.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approval_path_contact_pool",
			joinColumns = {@JoinColumn(name = "Approval_Path_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Contact_Id")}
			)
	public Set<Contact> getApproverPool() {
		return approverPool;
	}
	/** See {@link #approverPool}. */
	public void setApproverPool(Set<Contact> approverPool) {
		this.approverPool = approverPool;
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

	/** See {@link #finalApprover}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Final_Approver_Id")
	public Approver getFinalApprover() {
		return finalApprover;
	}
	/** See {@link #finalApprover}. */
	public void setFinalApprover(Approver finalApprover) {
		this.finalApprover = finalApprover;
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

	/** See {@link #documentChains}. */
	@ManyToMany(
			targetEntity=DocumentChain.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approval_path_document_chain",
			joinColumns = {@JoinColumn(name = "Approval_Path_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Document_Chain_Id")}
			)
	public Set<DocumentChain> getDocumentChains() {
		return documentChains;
	}
	/** See {@link #documentChains}. */
	public void setDocumentChains(Set<DocumentChain> documentChains) {
		this.documentChains = documentChains;
	}

	/** See {@link #useFinalApprover}. */
	@Column(name = "Use_Final_Approver", nullable = false)
	public boolean getUseFinalApprover() {
		return useFinalApprover;
	}
	/** See {@link #useFinalApprover}. */
	public void setUseFinalApprover(boolean useFinalApprover) {
		this.useFinalApprover = useFinalApprover;
	}

	/** See {@link #editors}. */
	@ManyToMany(
			targetEntity=Contact.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approval_path_editor",
			joinColumns = {@JoinColumn(name = "Approval_Path_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Contact_Id")}
			)
	public Set<Contact> getEditors() {
		return editors;
	}
	/** See {@link #editors}. */
	public void setEditors(Set<Contact> editors) {
		this.editors = editors;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		//result = prime * result + (approvalRequired ? 1231 : 1237);
		result = prime * result
				+ ((approver == null) ? 0 : approver.hashCode());
		result = prime * result
				+ ((approverPool == null) ? 0 : approverPool.hashCode());
		result = prime * result
				+ ((documentChains == null) ? 0 : documentChains.hashCode());
		/*result = prime * result
				+ ((employeeAction == null) ? 0 : employeeAction.hashCode());*/
		result = prime * result
				+ ((finalApprover == null) ? 0 : finalApprover.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((production == null) ? 0 : production.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + (usePool ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApprovalPath other = (ApprovalPath) obj;
		/*if (approvalRequired != other.approvalRequired)
			return false;*/
		if (approver == null) {
			if (other.approver != null)
				return false;
		} else if (!approver.equals(other.approver))
			return false;
		if (approverPool == null) {
			if (other.approverPool != null)
				return false;
		} else if (!approverPool.equals(other.approverPool))
			return false;
		if (documentChains == null) {
			if (other.documentChains != null)
				return false;
		} else if (!documentChains.equals(other.documentChains))
			return false;
		/*if (employeeAction != other.employeeAction)
			return false;*/
		if (finalApprover == null) {
			if (other.finalApprover != null)
				return false;
		} else if (!finalApprover.equals(other.finalApprover))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (production == null) {
			if (other.production != null)
				return false;
		} else if (!production.equals(other.production))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (usePool != other.usePool)
			return false;
		return true;
	}


}
