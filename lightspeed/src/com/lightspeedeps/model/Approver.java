package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Approver entity. It represents one person responsible for approving time
 * cards. Approver objects are arranged in "chains". Each Production has a chain
 * (if in use) of Approvers who approve all time cards from that Production.
 * Each Department within a Production may also have its own chain; these
 * Approvers must approve the time cards within their department, before the
 * time cards are made available to the Production approvers.
 * <p>
 * The chain of a Production's approvers is rooted at Production.approver. A
 * chain of departmental approvers is rooted in an instance of ApprovalAnchor.
 * <p>
 * The database model also allows for a chain of approvers specific to one
 * Contact, which would be rooted in an instance of ApprovalAnchor, where
 * approvalAnchor.contact indicates the Contact being approved. However, this
 * feature is not yet implemented.
 */

@NamedQueries({
	@NamedQuery(name=Approver.GET_APPROVER_IDS_BY_CONTACT,
			query = "select app.id from Approver app where app.contact=:contact")
/*	@NamedQuery(name=Approver.GET_APPROVER_LIST_BY_CONTACT,
			query = "from Approver app where app.contact=:contact")*/
})

@Entity
@Table(name = "approver")
public class Approver extends PersistentObject<Approver> {

	private static final long serialVersionUID = 1L;

	//Named Queries Literal
	/** Get the list database ids of Approvers where the approver is a specific Contact. */
	public static final String  GET_APPROVER_IDS_BY_CONTACT = "getApproverIdsByContact";
	/** Get the list of Approvers where the approver is a specific Contact. */
	//public static final String  GET_APPROVER_LIST_BY_CONTACT = "getApproverListByContact";

	// Fields
	/** The Contact record describing this Approver. */
	private Contact contact;

	/** The next Approver (moving towards final approver) in the chain of Approver`s. */
	private Approver nextApprover;

	/** Allows direct access to id field of nextApprover.
	 * @see #nextApprover */
	private Integer nextApproverId;

	/** True iff this Approver is shared, e.g., because it is a
	 * Production approver. */
	private Boolean shared;

//	private Set<Production> productions = new HashSet<Production>(0);
//	private Set<Approver> approvers = new HashSet<Approver>(0);
//	private Set<ApprovalAnchor> approvalAnchors = new HashSet<ApprovalAnchor>(0);

	// Constructors

	/** default constructor */
	public Approver() {
	}

	/** typical constructor */
	public Approver(Contact contact, Approver approver, Boolean shared) {
		this.contact = contact;
		nextApprover = approver;
		this.shared = shared;
	}

	// Property accessors

	/** See {@link #contact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #nextApprover}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Next_Approver_Id")
	public Approver getNextApprover() {
		return nextApprover;
	}
	/** See {@link #nextApprover}. */
	public void setNextApprover(Approver approver) {
		nextApprover = approver;
	}

	@JsonIgnore
	@Column(name = "Next_Approver_Id", insertable=false, updatable=false)
	public Integer getNextApproverId() {
		return nextApproverId;
	}
	public void setNextApproverId(Integer id) {
		nextApproverId = id;
	}

	/** See {@link #shared}. */
	@Column(name = "Shared")
	public Boolean getShared() {
		return shared;
	}
	/** See {@link #shared}. */
	public void setShared(Boolean shared) {
		this.shared = shared;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "approver")
//	public Set<Production> getProductions() {
//		return this.productions;
//	}
//	public void setProductions(Set<Production> productions) {
//		this.productions = productions;
//	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "approver")
//	public Set<Approver> getApprovers() {
//		return this.approvers;
//	}
//	public void setApprovers(Set<Approver> approvers) {
//		this.approvers = approvers;
//	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "approver")
//	public Set<ApprovalAnchor> getApprovalAnchors() {
//		return this.approvalAnchors;
//	}
//	public void setApprovalAnchors(Set<ApprovalAnchor> approvalAnchors) {
//		this.approvalAnchors = approvalAnchors;
//	}

}