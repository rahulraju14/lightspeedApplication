package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.dao.ContentDAO;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Document entity.  This is the unit of storage within the
 * Lightspeed File Repository.  A Document is contained within
 * one (and only one) Folder.
 * <p>
 * Document will be uploaded and distributed to production members,
 * those will then submit the document to the corresponding approver.
 */
@NamedQueries ({
	@NamedQuery(name=Document.GET_DOCUMENT_LIST_BY_FOLDER_IDS, query = "from Document d where d.folder.id in (:folderId) and d.deleted = false and d.docChainId != 0 order by d.name"),
	@NamedQuery(name=Document.GET_DOCUMENT_LIST_BY_DOCUMENT_CHAIN, query = "from Document d where d.docChainId =:documentChainId and Deleted = false order by d.revision DESC"),
	@NamedQuery(name=Document.GET_HIGHEST_REVISION_OF_DOCUMENT, query = "select MAX(d.revision) from Document d where d.docChainId =:documentChainId and Deleted = false"),
	@NamedQuery(name=Document.GET_DOCUMENT_WITH_LOWEST_REVISION, query = "from Document d where d.docChainId =:documentChainId and d.revision in (select MIN(d.revision) from d where d.docChainId =:documentChainId and Deleted = false)"),
	@NamedQuery(name=Document.GET_DOCUMENT_WITH_HIGHEST_REVISION, query = "from Document d where d.docChainId =:documentChainId and d.revision in (select MAX(d.revision) from d where d.docChainId =:documentChainId and Deleted = false)"),
	@NamedQuery(name=Document.GET_STANDARD_DOCUMENTS, query = "from Document d where d.standard = true"),
	@NamedQuery(name=Document.GET_DOCUMENT_BY_NAME_FOLDER_ID, query = "from Document d where d.name =:name and d.folder.id =:folderId and d.docChainId != 0"),
	@NamedQuery(name=Document.GET_STANDARD_DOCUMENT_BY_FOLDER_ID, query = "from Document d where d.folder.id =:folderId and d.standard = true"),
	@NamedQuery(name=Document.GET_NON_STANDARD_DOCUMENT_BY_FOLDER_ID, query = "from Document d where d.folder.id =:folderId and d.standard = false and d.deleted = false order by d.name"),
	@NamedQuery(name=Document.GET_REPLACEMENT_DOCUMENT_BY_NAME_FOLDER_ID, query = "from Document d where d.name =:name and d.folder.id =:folderId and d.docChainId is NULL"),
	@NamedQuery(name=Document.GET_DELETED_DOCUMENT_BY_NAME_FOLDER, query = "select trim(leading '*' from d.name) from Document d where d.name like CONCAT('*%',:name) and d.folder.id =:folderId and d.docChainId != 0"),
})
@Entity
@Table(name = "document", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Name", "Folder_Id","Revision","DocChainId" }))
public class Document extends PersistentObject<Document> {
	private static final long serialVersionUID = 2722699496457211654L;

	//Named Queries Literals
	public static final String GET_DOCUMENT_LIST_BY_FOLDER_IDS = "getDocumentListByFolderIds";
	public static final String GET_DOCUMENT_LIST_BY_DOCUMENT_CHAIN = "getDocumentListByDocumentChain";
	public static final String GET_HIGHEST_REVISION_OF_DOCUMENT = "getHighestRevisionOfDocument";
	public static final String GET_DOCUMENT_WITH_LOWEST_REVISION = "getDocumentWithLowestRevision";
	public static final String GET_DOCUMENT_WITH_HIGHEST_REVISION = "getDocumentWithHighestRevision";
	public static final String GET_STANDARD_DOCUMENTS = "getStandardDocument";
	public static final String GET_DOCUMENT_BY_NAME_FOLDER_ID = "getDocumentByNameFolderId";
	public static final String GET_STANDARD_DOCUMENT_BY_FOLDER_ID = "getStandardDocumentByFolderId";
	public static final String GET_NON_STANDARD_DOCUMENT_BY_FOLDER_ID = "getNonStandardDocumentByFolderId";
	public static final String GET_REPLACEMENT_DOCUMENT_BY_NAME_FOLDER_ID = "getReplacementDocumentByNameFolderId";
	public static final String GET_DELETED_DOCUMENT_BY_NAME_FOLDER = "getDeletedDocumentByNameFolder";

	/** Parameter string that identifies the Document.name value in the named queries. */
	public static final String NAME = "name";

	/** The following string is added as prefix to document names when they are "soft deleted", i.e.,
	 * marked deleted but remain in the database.  That's typically because copies of the document
	 * have been issued and approved. */
	public static final String DELETED_PREFIX = "*";

	/** Parameter string that identifies the Folder.id value in the named queries. */
	public static final String FOLDER_ID = "folderId";

	// Fields

	/** The Folder that contains this document. */
	private Folder folder;
	/** The Document's owner. */
	private User user;
	/** The name of the Document, which must be unique within
	 * it's containing Folder.  Document names are NOT case sensitive. */
	private String name;
	/** The Short/Abbreviated name of the Document. */
	private String shortName;
	/** Documents will be displayed in increasing 'listOrder' sequence. The values
	 * do not need to be contiguous. LS-4600 */
	private int listOrder;
	/** Arbitrary description of the Document. */
	private String description;
	/** Unused: meant to hold the name of the Document's author. */
	private String author;
	/** The date the original (underlying) document was created. Intended to
	 * be editable by the owner. */
	private Date created;
	/** When the Document was first added to the File Repository. */
	private Date loaded;
	/** The last time this Document was changed within the File Repository. */
	private Date updated;
	/** If true, then the Document is only visible to the owner. */
	private Boolean privateB;
	/** The document type, normally the Window's file extension. */
	private String type;
	/** The contents of the Document, which may be any arbitrary binary data. */
	private byte[] content;
	/** The id of the DocumentChain to which this document belongs. */
	private Integer docChainId;
	/** The revision number of Document, which is incremented by 1 on continuous upload of the same Document */
	private Integer revision;
	/** The enum for document's content type */
	private MimeType mimeType;
	/** The boolean field for documents, true if the document's revision is minimum */
	private boolean oldest;

	/** True iff this document has been deleted by the user. */
	private boolean deleted;

	/** The list of packets that contains this document */
	private List<Packet> packetList;

	private boolean standard;

	/** The Document id of the Original Document. */
	private Integer originalDocumentId;

	//Approval Path fields

	/** DocumentAction enum type, used to know what type of approval cycle document will undergo.  */
	private DocumentAction employeeAction;

	/** Boolean field used to know the selected document needs to under go approval process or not */
	private boolean approvalRequired;

	private int empSignatureCount;

	private int appSignatureCount;

	private Integer lockedBy;

	/** The equivalent PayrollFormType of this document, based on its {@link #type} field. */
	@Transient
	private PayrollFormType formType;

	/** Used in lists where selectable (clicked & highlighted); true iff this is the selected item in list. LS-4600 */
	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public Document() {
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY) // changed to LAZY in rev 2.2.4123
	@JoinColumn(name = "Folder_Id")
	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	@ManyToOne(fetch = FetchType.LAZY) // changed to LAZY in rev 2.2.4123
	@JoinColumn(name = "Owner_Id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	/** Maximum allowed length of a document name. */
	public final static int MAX_DOC_NAME_LENGTH = 100;

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Short_Name", nullable = true, length = 20)
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/** See {@link #listOrder}. */
	@Column(name = "list_order")
	public int getListOrder() {
		return listOrder;
	}
	/** See {@link #listOrder}. */
	public void setListOrder(int listOrder) {
		this.listOrder = listOrder;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "Author", length = 100)
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created", length = 0)
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Loaded", nullable = false, length = 0)
	public Date getLoaded() {
		return loaded;
	}
	public void setLoaded(Date loaded) {
		this.loaded = loaded;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	@Column(name = "Private", nullable = false)
	public Boolean getPrivate() {
		return privateB;
	}
	public void setPrivate(Boolean bool) {
		privateB = bool;
	}

	@Column(name = "Type", length = 100)
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
		formType = null; // force (re)evaluation
	}

	/**
	 * @return Equivalent PayrollFormType for this document, if applicable.
	 *         Returns "OTHER" if no match.
	 */
	@Transient
	public PayrollFormType getFormType() {
		if (formType == null) {
			formType = PayrollFormType.toValue(type);
		}
		return formType;
	}

//	@Column(name = "Content")
	@Transient
	public byte[] getContent() {
		if(content == null){
			Content ct = ContentDAO.getInstance().findByDocId(getId(), getOriginalDocumentId());
			if (ct != null) {
				content = ct.getContent();
			}
			else {
				content = new byte[0];
			}
		}
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}

	@Column(name = "DocChainId" , nullable = true)
	public Integer getDocChainId() {
		return docChainId;
	}
	public void setDocChainId(Integer docChainId) {
		this.docChainId = docChainId;
	}

	@Column(name = "Revision" , nullable = false)
	public Integer getRevision() {
		return revision;
	}
	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "MimeType" , nullable = false, length = 30)
	public MimeType getMimeType() {
		return mimeType;
	}
	public void setMimeType(MimeType mimeType) {
		this.mimeType = mimeType;
	}

	@Column(name = "Oldest", nullable = false)
	public boolean getOldest() {
		return oldest;
	}
	public void setOldest(boolean oldest) {
		this.oldest = oldest;
	}

	/** See {@link #packetList}. */
	@ManyToMany(
			targetEntity=Packet.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE},
			fetch = FetchType.LAZY
		)
	@JoinTable( name = "packet_document",
			joinColumns = {@JoinColumn(name = "Document_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Packet_Id")}
			)
	public List<Packet> getPacketList() {
		return packetList;
	}
	/** See {@link #packetList}. */
	public void setPacketList(List<Packet> packetList) {
		this.packetList = packetList;
	}

	@Column(name = "Deleted", nullable = false)
	public boolean getDeleted() {
		return deleted;
	}
	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	@Column(name = "Standard", nullable = false)
	public boolean getStandard() {
		return standard;
	}
	public void setStandard(boolean standard) {
		this.standard = standard;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #originalDocumentId}. */
	@Column(name = "Original_Document_Id")
	public Integer getOriginalDocumentId() {
		return originalDocumentId;
	}
	/** See {@link #originalDocumentId}. */
	public void setOriginalDocumentId(Integer originalDocumentId) {
		this.originalDocumentId = originalDocumentId;
	}

	/** See {@link #employeeAction}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Employee_Action" , nullable = true, length = 30)
	public DocumentAction getEmployeeAction() {
		return employeeAction;
	}
	/** See {@link #employeeAction}. */
	public void setEmployeeAction(DocumentAction employeeAction) {
		this.employeeAction = employeeAction;
	}

	/** See {@link #approvalRequired}. */
	@Column(name = "Approval_Required", nullable = false)
	public boolean getApprovalRequired() {
		return approvalRequired;
	}
	/** See {@link #approvalRequired}. */
	public void setApprovalRequired(boolean approvalRequired) {
		this.approvalRequired = approvalRequired;
	}

	/** See {@link #empSignatureCount}. */
	@Column(name = "Emp_Signature_Count" , nullable = false)
	public int getEmpSignatureCount() {
		return empSignatureCount;
	}
	/** See {@link #empSignatureCount}. */
	public void setEmpSignatureCount(int empSignatureCount) {
		this.empSignatureCount = empSignatureCount;
	}

	/** See {@link #appSignatureCount}. */
	@Column(name = "App_Signature_Count" , nullable = false)
	public int getAppSignatureCount() {
		return appSignatureCount;
	}
	/** See {@link #appSignatureCount}. */
	public void setAppSignatureCount(int appSignatureCount) {
		this.appSignatureCount = appSignatureCount;
	}

	/** See {@link #lockedBy}. */
	@Column(name = "Locked_By" , nullable = true)
	public Integer getLockedBy() {
		return lockedBy;
	}
	/** See {@link #lockedBy}. */
	public void setLockedBy(Integer lockedBy) {
		this.lockedBy = lockedBy;
	}

	/** Returns the Short name for the Document if any, else returns the normalized name of the Document. */
	@Transient
	public String getNormalizedName() {
		if (shortName != null && (! shortName.equals(""))) {
			return shortName;
		}
		else {
			return getDisplayName();
		}
	}

	/** Method used to omit the  deletion prefix ("*") from the name of deleted Document Chain. */
	@Transient
	public String getDisplayName() {
		String newName = getName();
		while(newName.startsWith(Document.DELETED_PREFIX)) {
			newName = newName.substring(1);
		}
		return newName;
	}

//	/** See {@link #contactCount}. */
//	@Transient
//	public Long getContactCount() {
//		return ContactDocumentDAO.getInstance().findContactCountByDocumentId(id);
//	}
//	/** See {@link #contactCount}. */
//	public void setContactCount(Long contactCount) {
//		this.contactCount = contactCount;
//	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getName()==null ? "null" : getName());
		s += ", type=" + (getType()==null ? "null" : getType());
		s += "]";
		return s;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Compares Document objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Document other = null;
		try {
			other = (Document)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
			else { // neither one is persisted, compare names
				return StringUtils.compare(name, other.name) == 0;
			}
		}
		return getId().equals(other.getId());
	}

	/**
	 * The default comparison for Document, which uses the document name.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Document other) {
		if (equals(other)) {
			return 0;
		}
		return StringUtils.compare(name, other.name);
	}

	/**
	 * Copy all the data fields from the 'other' Document into
	 * this Document.
	 * @param other The source object for the fields to be copied.
	 */
	@Transient
	public void copyFrom(Document other) {
		setName(other.getName());
		setShortName(other.getShortName());
		setDescription(other.getDescription());
		setListOrder(other.getListOrder());
		setAuthor(SessionUtils.getCurrentUser().getDisplayName());
		setUser(SessionUtils.getCurrentUser());
		setCreated(new Date());
		setLoaded(new Date());
		setUpdated(new Date());
		setPrivate(false);
		setType(other.getType());
		setContent(null);
		setMimeType(other.getMimeType());
		setRevision(other.getRevision());
		setOldest(true);
		setDeleted(false);
		setStandard(other.getStandard());
		setApprovalRequired(other.getApprovalRequired());
		setEmployeeAction(other.getEmployeeAction());
		setEmpSignatureCount(other.getEmpSignatureCount());
		setAppSignatureCount(other.getAppSignatureCount());
	}

}
