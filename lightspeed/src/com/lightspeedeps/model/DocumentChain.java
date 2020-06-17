package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * DocumentChain entity.  This is the unit of storage within the
 * Lightspeed File Repository for Onboarding.  A DocumentChain is contained within
 * one (and only one) Folder.
 * <p>
 * This entity is currently hidden from user's view but it is useful in maintaining
 * the relationship between Document and the ApprovalPath that it will follow.
 */

@NamedQueries ({
	@NamedQuery(name=DocumentChain.GET_DOCUMENT_CHAIN_LIST, query = "from DocumentChain dc where dc.folder.id =:folderId and dc.deleted = false"),
	@NamedQuery(name=DocumentChain.GET_DISTINCT_DOCUMENT_CHAIN_LIST,query = "from DocumentChain dc where id in (select distinct cd.documentChain.id from ContactDocument cd where cd.contact.production =:production)"),
	@NamedQuery(name=DocumentChain.GET_NON_DELETED_DOCUMENT_CHAIN_LIST_BY_FOLDER_IDS, query = "from DocumentChain d where d.folder.id in (:folderId) and d.deleted = false"),
	@NamedQuery(name=DocumentChain.GET_DOCUMENT_CHAIN_LIST_BY_FOLDER_IDS, query = "from DocumentChain d where d.folder.id in (:folderId)"),
	@NamedQuery(name=DocumentChain.GET_DISTINCT_DOCUMENT_CHAIN_LIST_PROJECT, query = "from DocumentChain dc where id in (select distinct cd.documentChain.id from ContactDocument cd where cd.project =:project)"),
	@NamedQuery(name=DocumentChain.GET_START_DOCUMENT_CHAIN_OF_PRODUCTION, query = "from DocumentChain dc where dc.name = 'Payroll Start' and dc.folder.id =:folderId"),
	@NamedQuery(name=DocumentChain.GET_DOCUMENT_CHAIN_BY_NAME_AND_FOLDER, query = "from DocumentChain dc where dc.name =:documentChainName and dc.folder.id =:folderId")
})

@Entity
@Table(name = "document_chain", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Name","Folder_Id"}))
public class DocumentChain extends PersistentObject<DocumentChain> implements Comparable<DocumentChain> {

	private static final long serialVersionUID = 2722699496457211654L;

	public static final String GET_DOCUMENT_CHAIN_LIST = "getDocumentChainList";
	public static final String GET_DISTINCT_DOCUMENT_CHAIN_LIST = "getDistinctDocumentChain";
	public static final String GET_NON_DELETED_DOCUMENT_CHAIN_LIST_BY_FOLDER_IDS = "getNonDeletedDocumentChainListByFolderIds";
	public static final String GET_DOCUMENT_CHAIN_LIST_BY_FOLDER_IDS = "getDocumentChainListByFolderIds";
	public static final String GET_DISTINCT_DOCUMENT_CHAIN_LIST_PROJECT = "getDistinctDocumentChainProject";
	public static final String GET_START_DOCUMENT_CHAIN_OF_PRODUCTION = "getStartDocumentChainOfProduction";
	public static final String GET_DOCUMENT_CHAIN_BY_NAME_AND_FOLDER = "getDocumentChainByNameAndFolder";

	//Fields

	/** The name of the DocumentChain, which must be unique within
	 * it's containing Folder. DocumentChain names are NOT case sensitive. */
	private String name;
	/** The Short/Abbreviated name of the Document. */
	private String shortName;
	/** The enum for documentchain's content type */
	private MimeType type;

	/** True iff this chain has been deleted by the user. */
	private boolean deleted;

	/** The date the original (underlying) documentchain was created. Intended to
	 * be editable by the owner. */
	private Date created;
	/** The last time this DocumentChain was updated with the new revision. */
	private Date revised;
	/** The revision number of DocumentChain and incremented by 1 on continuous upload of the same Document under the documentChain */
	private Integer revisions;
	/** The user's account number */
	private String creatorAcct;
	/** The enum for document flow type selected by the user */
	private DocumentFlowType docFlowType;
	/** The Folder that contains this documentchain. */
	private Folder folder;
	/** The only approval path the corresponding document chain belongs to. */
	private Set<ApprovalPath> approvalPath;
	/**  Boolean field used to check if the single document chain is selected or not in Approval Paths, default is false */

	@Transient
	private boolean checked = false;

	/**  Boolean field used to disable document chain for an approval path if it has another approval path, default is false */
	@Transient
	private boolean disabled = false;

	/** The equivalent PayrollFormType of this document, based on its {@link #type} field. */
	@Transient
	private PayrollFormType formType;

	// Constructors
	/** default constructor */
	 public DocumentChain() {
	}

	// Property accessors

	 /** See {@link #name}. */
    @Column(name = "Name", nullable = false, length = 200)
	public String getName() {
		return name;
	}
    /** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Equivalent PayrollFormType for this documentChain, if applicable.
	 *         Returns "OTHER" if no match.
	 */
	@Transient
	public PayrollFormType getFormType() {
		if (formType == null) {
			formType = PayrollFormType.OTHER;
			if (getType() == MimeType.LS_FORM) {
				formType = PayrollFormType.toValue(name);
			}
		}
		return formType;
	}

	/** See {@link #shortName}. */
	@Column(name = "Short_Name", nullable = true, length = 20)
	public String getShortName() {
		return shortName;
	}
	/** See {@link #shortName}. */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/** See {@link #type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Type" , nullable = false, length = 30)
	public MimeType getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(MimeType type) {
		this.type = type;
	}

	/** See {@link #deleted}. */
	@Column(name = "Deleted", nullable = false)
	public boolean getDeleted() {
		return deleted;
	}
	/** See {@link #deleted}. */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/** See {@link #created}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created", nullable = false)
	public Date getCreated() {
		return created;
	}
	/** See {@link #created}. */
	public void setCreated(Date created) {
		this.created = created;
	}

	/** See {@link #revised}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Revised", nullable = false)
	public Date getRevised() {
		return revised;
	}
	/** See {@link #revised}. */
	public void setRevised(Date revised) {
		this.revised = revised;
	}

	/** See {@link #revisions}. */
    @Column(name = "Revisions", nullable = false)
	public Integer getRevisions() {
		return revisions;
	}
    /** See {@link #revisions}. */
	public void setRevisions(Integer revisions) {
		this.revisions = revisions;
	}

	/** See {@link #creatorAcct}. */
	@Column(name = "Creator_Acct", length = 20)
	public String getCreatorAcct() {
		return creatorAcct;
	}
	/** See {@link #creatorAcct}. */
	public void setCreatorAcct(String creatorAcct) {
		this.creatorAcct = creatorAcct;
	}

	/** See {@link #docFlowType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Document_Flow_Type", nullable = false, length = 10)
	public DocumentFlowType getDocFlowType() {
		return docFlowType;
	}
	/** See {@link #docFlowType}. */
	public void setDocFlowType(DocumentFlowType docFlowType) {
		this.docFlowType = docFlowType;
	}

	/** See {@link #folder}. */
	@ManyToOne(fetch = FetchType.LAZY) // changed to LAZY in rev 2.2.4123
	@JoinColumn(name = "Folder_Id")
	public Folder getFolder() {
		return folder;
	}
	/** See {@link #folder}. */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	/** See {@link #approvalPath}. */
	@ManyToMany(
			targetEntity=ApprovalPath.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "approval_path_document_chain",
			joinColumns = {@JoinColumn(name = "Document_Chain_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Approval_Path_Id")}
			)
	public Set<ApprovalPath> getApprovalPath() {
		return approvalPath;
	}
	/** See {@link #approvalPath}. */
	public void setApprovalPath(Set<ApprovalPath> approvalPath) {
		this.approvalPath = approvalPath;
	}

	/** See {@link #disabled}. */
	@Transient
	public boolean getDisabled() {
		return disabled;
	}
	/** See {@link #disabled}. */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/** Returns the Short name for the chain if any, else returns the normalized name of Document chain. */
	@Transient
	public String getNormalizedName() {
		if (shortName != null && (! shortName.equals(""))) {
			return shortName;
		}
		else {
			return getDisplayName();
		}
	}

	/** Method used to omit the deletion prefix ("*") from the name of deleted Document Chain. */
	@Transient
	public String getDisplayName() {
		String newName = getName();
		while (newName.startsWith(Document.DELETED_PREFIX)) {
			newName = newName.substring(1);
		}
		return newName;
	}

	/** See {@link #checked}. */
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/** See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/**
	 * @return True if this item should be hidden (not displayed) in Canadian
	 *         talent productions.
	 */
	public boolean hideForCanada() {
		boolean hide = false;
		if (getType() == MimeType.LS_FORM && // LS-1632/LS-1611 Hide these forms for Canada
				(getName().equalsIgnoreCase(PayrollFormType.START.getName()) ||
				getName().equalsIgnoreCase(PayrollFormType.ACTRA_INTENT.getName()) )) {
			hide = true;
		}
		return hide;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getName()==null ? "null" : getName());
		s += "]";
		return s;
	}

	/**
	 * Copy all the data fields from the 'other' DocumentChain into
	 * this DocumentChain.
	 * @param other The source object for the fields to be copied.
	 */
	@Transient
	public void copyFrom(DocumentChain other) {
		setName(other.getName());
		setShortName(other.getShortName());
		setType(other.getType());
		setCreated(new Date());
		setRevised(new Date());
		setRevisions(other.getRevisions());
		setCreatorAcct(SessionUtils.getCurrentUser().getAccountNumber());
		setDocFlowType(other.getDocFlowType());
		setDeleted(false);
	}

	@Override
	public int compareTo(DocumentChain documentChain) {
		int ret = 1;
		if (documentChain != null) {
			return compareTo(documentChain, "");
		}
		return ret;
	}

	public int compareTo(DocumentChain doc, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by name (later)
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			ret = compareByName(doc); // ... so do name compare
		}
		return ret;
	}

	public int compareByName(DocumentChain doc) {
		int ret = StringUtils.compareIgnoreCase(getNormalizedName(), doc.getNormalizedName());
		if (ret == 0) {
			ret = StringUtils.compareIgnoreCase(getNormalizedName(), doc.getNormalizedName());
		}
		return ret;
	}

}
