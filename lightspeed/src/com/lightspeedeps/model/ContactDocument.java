package com.lightspeedeps.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.service.FormService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * A class that associates a Contact with one or more documents that have been "distributed" to
 * that person for review and/or signing. Part of the On-Boarding system.
 */
@NamedQueries({
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_BY_CONTACT_ID_AND_DOCUMENT_CHAIN_ID, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.contact.id =:contactId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_BY_DOCUMENT_CHAIN_ID, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_LIST_BY_DOCUMENT_CHAIN_PROJECT, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT, query = "select count(cd.id) from ContactDocument cd where cd.document.id =:documentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN, query = "select count(cd.id) from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.project =:project and cd.status in ('APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS, query = "select count(cd.id) from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.contact.production =:production and cd.status in ('APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_CONTACT_ID, query = "from ContactDocument where contact.id =:contactId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_DOCUMENT_ID, query = "from ContactDocument cd where cd.employment.id =:employmentId and cd.document.id =:documentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID, query = "from ContactDocument where employment.id =:employmentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID_PROJECT, query = "from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_PROJECT_DOCUMENT_ID, query = "from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.document.id =:documentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS_PROJECT, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.status ='SUBMITTED_NO_APPROVERS' and cd.approverId is NULL and cd.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.status ='SUBMITTED_NO_APPROVERS' and cd.approverId is NULL and cd.employment.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_BY_EMPLOYMENT_AND_STATUS, query = "select count(cd.id) from ContactDocument cd where cd.employment =:employment and cd.status !='OPEN'"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_LIST_BY_DOCUMENT_IDS, query = "from ContactDocument cd where cd.document.id in (:documentId)"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOCUMENT_STATUS_BY_CONTACT_AND_DOCUMENT_CHAIN, query = "select cd.status from ContactDocument cd where cd.contact.id =:contactId and cd.documentChain.id =:documentChainId"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_DEPARTMENT_DOCUMENT_CHAIN_PROJECT, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.employment.role.department.id =:departmentId and cd.contact.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_DEPARTMENT_ID_AND_DOCUMENT_CHAIN_ID, query = "from ContactDocument cd where cd.documentChain.id =:documentChainId and cd.employment.role.department.id =:departmentId and cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER, query = "from ContactDocument cd where cd.approverId < 0 and cd.employment.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_DOCUMENT_ID, query = "from ContactDocument cd where cd.approverId < 0 and cd.employment.contact.production =:production and cd.document.id =:documentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT_DOCUMENT_ID, query = "from ContactDocument cd where cd.approverId < 0 and cd.employment.project =:project and cd.document.id =:documentId"),
	@NamedQuery(name=ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT, query = "from ContactDocument cd where cd.approverId < 0 and cd.employment.project =:project"),
	@NamedQuery(name=ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN, query = "select distinct cd.documentChain from ContactDocument cd where cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN_LIST_BY_PROJECT, query = "select distinct cd.documentChain from ContactDocument cd where cd.project =:project"),
	@NamedQuery(name=ContactDocument.GET_DISTINCT_DOCUMENT_FROM_CONTACT_DOCUMENT, query = "select distinct cd.document from ContactDocument cd where cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.status <> 'VOID'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.status <> 'VOID'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.status in ('APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.status in ('APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.status='OPEN'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.status='OPEN'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.status='PENDING'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.status='PENDING'"),
	@NamedQuery(name=ContactDocument.GET_DOC_LIST_BY_EMPLOYMENT_ID, query = "from Document where id in (select cd.document.id from ContactDocument cd where cd.employment.id =:employmentId)"),
	@NamedQuery(name=ContactDocument.GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID_PROJECT, query="select count(cd.id) from ContactDocument cd where cd.employment.id=:employmentId and cd.project =:project and cd.status not in ('OPEN','APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID, query="select count(cd.id) from ContactDocument cd where cd.employment.id=:employmentId and cd.status not in ('OPEN','APPROVED','LOCKED')"),
	@NamedQuery(name=ContactDocument.GET_SUBMITTED_I9_CONTACT_DOCUMENT_LIST_BY_PRODUCTION, query = "from ContactDocument cd where cd.formType ='I9' and cd.status ='SUBMITTED' and cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.status='VOID'"),
	@NamedQuery(name=ContactDocument.GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT_PROJECT, query = "select count(cd.id) from ContactDocument cd where cd.employment.id =:employmentId and cd.project =:project and cd.status='VOID'"),
	@NamedQuery(name=ContactDocument.GET_COUNT_START_FORM_NON_VOID_BY_EMPLOYMENT, query = "select count(id) from ContactDocument cd where cd.employment =:employment and cd.status <> 'VOID' and cd.formType='START'"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_START_FORM_NON_VOID_BY_EMPLOYMENT, query = "from ContactDocument cd where cd.employment =:employment and cd.status <> 'VOID' and cd.formType='START'"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_PRODUCTION, query = "from ContactDocument cd where cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PRODUCTION, query = "from ContactDocument cd where cd.approverId in (:approverIds) and cd.employment.role.department.id in (:departmentIds) and cd.contact.production =:production"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PROJECT, query = "from ContactDocument cd where cd.approverId in (:approverIds) and cd.employment.role.department.id in (:departmentIds) and cd.contact.project =:project"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_OF_DEPT_LEVEL_BY_PRODUCTION_STATUS, query = "from ContactDocument cd where cd.contact.production =:production and cd.approvalLevel='DEPT' and cd.status ='SUBMITTED_NO_APPROVERS'"),
	@NamedQuery(name=ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS, query = "from ContactDocument cd where cd.approverId in (:approverIds)"),
	@NamedQuery(name=ContactDocument.GET_APPROVED_W4_CONTACT_DOCUMENT_BY_CONTACT, query = "from ContactDocument cd where cd.contact =:contact and cd.status ='APPROVED' and cd.formType='W4' and cd.relatedFormId is not null"),
	@NamedQuery(name=ContactDocument.GET_INTENT_CONTACT_DOCUMENT_BY_PROJECT, query = "from ContactDocument cd where cd.formType ='ACTRA_INTENT' and cd.project =:project"),
})

@Entity
@Table (name = "contact_document")
public class ContactDocument extends Approvable implements Comparable<ContactDocument> {

	private static final Log log = LogFactory.getLog(ContactDocument.class);

	private static final long serialVersionUID = -8929198681264579811L;

	// Named Queries Literals
	public static final String  GET_CONTACT_DOCUMENT_BY_CONTACT_ID_AND_DOCUMENT_CHAIN_ID = "getContactDocumentByContactIdAndDocumentChainId";
	public static final String  GET_CONTACT_DOCUMENT_BY_DOCUMENT_CHAIN_ID = "getContactDocumentByDocumentChainId";
	public static final String  GET_CONTACT_DOCUMENT_LIST_BY_DOCUMENT_CHAIN_PROJECT = "getContactDocumentListByDocumentChainProject";
	public static final String  GET_CONTACT_COUNT_BY_DOCUMENT = "getContactCountByDocumentId";
	public static final String  GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN = "getContactCountByDoucmentChainId";
	public static final String  GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS = "getContactCountByDocumentChainIdAndStatus";
	public static final String  GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS_PROJECT = "getContactCountByDocumentChainIdAndStatusProject";
	public static final String  GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_PROJECT = "getContactCountByDoucmentChainIdProject";
	public static final String  GET_CONTACT_DOC_LIST_BY_CONTACT_ID = "getContactDocListByContactId";
	public static final String  GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_DOCUMENT_ID = "getContactDocListByEmploymentDocumentId";
	public static final String  GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID = "getContactDocListByEmploymentId";
	public static final String  GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID_PROJECT = "getContactDocListByEmploymentIdProject";
	public static final String  GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_PROJECT_DOCUMENT_ID = "getContactDocListByEmploymentProjectDocumentId";
	public static final String  GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS_PROJECT = "getContactDocListBySubmittedNoApproversStatusProject";
	public static final String  GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS = "getContactDocListBySubmittedNoApproversStatus";
	public static final String  GET_CONTACT_DOCUMENT_BY_EMPLOYMENT_AND_STATUS = "getContactDocumentByEmploymentAndStatus";
	public static final String  GET_CONTACT_DOCUMENT_LIST_BY_DOCUMENT_IDS ="getContactDocumentListByDocumentIds";
	public static final String  GET_CONTACT_DOCUMENT_STATUS_BY_CONTACT_AND_DOCUMENT_CHAIN = "getContactDocumentStatusByContactAndDocumentChain";
	public static final String  GET_CONTACTDOCUMENT_BY_DEPARTMENT_DOCUMENT_CHAIN_PROJECT = "getContactDocumentByDepartmentDocumentChainProject";
	public static final String  GET_CONTACTDOCUMENT_BY_DEPARTMENT_ID_AND_DOCUMENT_CHAIN_ID = "getContactDocumentByDepartmentIdAndDocumentChainId";
	public static final String  GET_CONTACTDOCUMENT_BY_POOL_APPROVER = "getContactDocumentByPoolApprover";
	public static final String  GET_CONTACTDOCUMENT_BY_POOL_APPROVER_DOCUMENT_ID = "getContactDocumentByPoolApproverDocumentId";
	public static final String  GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT_DOCUMENT_ID = "getContactDocumentByPoolApproverProjectDocumentId";
	public static final String  GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT = "getContactDocumentByPoolApproverProject";
	public static final String  GET_DISTINCT_DOCUMENT_CHAIN = "getDistinctDocumentChainFromContactDocument";
	public static final String  GET_DISTINCT_DOCUMENT_CHAIN_LIST_BY_PROJECT = "getContactListByProject";
	public static final String  GET_DISTINCT_DOCUMENT_FROM_CONTACT_DOCUMENT = "getDistinctDocumentFromContactDocument";
	public static final String  GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT = "getDocCountNonVoidByEmployment";
	public static final String  GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT_PROJECT = "getDocCountNonVoidByContactProject";
	public static final String  GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL = "getDocCountByEmploymentAndStatus";
	public static final String  GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL_PROJECT = "getDocCountByEmploymentAndStatusProject";
	public static final String  GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT_PROJECT = "getDocCountOfOpenStatusProject";
	public static final String  GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT = "getDocCountOfOpenStatus";
	public static final String  GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT = "getDocCountOfPendingStatus";
	public static final String  GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT_PROJECT = "getDocCountOfPendingStatusProject";
	public static final String  GET_DOC_LIST_BY_EMPLOYMENT_ID = "getDocListByEmploymentId";
	public static final String  GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID = "getRemainingDocCountByContactId";
	public static final String  GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID_PROJECT = "getRemainingDocCountByContactIdProject";
	public static final String  GET_SUBMITTED_I9_CONTACT_DOCUMENT_LIST_BY_PRODUCTION = "getSubmittedI9ContactDocumentListByProduction";
	public static final String  GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT = "getDocCountOfVoidStatus";
	public static final String  GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT_PROJECT = "getDocCountOfVoidStatusProject";
	public static final String GET_COUNT_START_FORM_NON_VOID_BY_EMPLOYMENT = "getCountStartFormNonVoidByEmployment";
	public static final String GET_CONTACT_DOC_LIST_START_FORM_NON_VOID_BY_EMPLOYMENT = "getContactDocListStartFormNonVoidByEmployment";
	public static final String GET_CONTACT_DOC_LIST_BY_PRODUCTION = "getContactDocListByProduction";
	public static final String GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PRODUCTION = "getContactDocListByApproverIdsDeptIdsProduction";
	public static final String GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PROJECT = "getContactDocListByApproverIdsDeptIdsProject";
	public static final String GET_CONTACT_DOC_LIST_OF_DEPT_LEVEL_BY_PRODUCTION_STATUS = "getContactDocListOfDeptLevelByProductionStatus";
	public static final String GET_CONTACT_DOC_LIST_BY_APPROVER_IDS = "getContactDocListByApproverIds";
	public static final String GET_APPROVED_W4_CONTACT_DOCUMENT_BY_CONTACT = "getApprovedW4ContactDocumentByContact";
	public static final String GET_INTENT_CONTACT_DOCUMENT_BY_PROJECT = "getIntentContactDocumentByProject";

	// Sort keys for compareTo() method

	/** Specifies sorting by the ordinal value of the formType (enumeration) field. */
	public static final String SORTKEY_FORM_TYPE = "FormType";

	/** Specifies sorting for the Transfer process, which means that the primary
	 * sort is by contact, then by Employment, and finally by the ordinal value of the
	 * formType (enumeration) field. */
	public static final String SORTKEY_TRANSFER = "Transfer";

	public static final String SORTKEY_STATUS = "status";

	/** The contact to which document is distributed */
	private Contact contact;

	/** The distributed document */
	private Document document;

	/** Distributed document's packet name, if part of a packet */
	private String packetName;

	/** The date when document was delivered to contact(s) */
	private Date delivered;

	/** The time this document was sent to the payroll service. This will be null
    * if the document has not been transmitted yet. */
	private Date timeSent;

	/** The document chain of which the distributed document is part of */
	private DocumentChain documentChain;

	/** Used to hold the paper image id for the contact document */
	private Image paperImage;

	/** Project instance, null for TV or Feature film productions */
	private Project project;

	/** List of contact doc events for the corresponding contact document */
	private List<ContactDocEvent> contactDocEvents = new ArrayList<>();

	private Employment employment;

	private Integer relatedFormId;

	/** Indicates the type of form this entry refers to, primarily used when an actual
	 * Document entry does not exist, such as for a Summary Sheet. */
	private PayrollFormType formType = PayrollFormType.OTHER;

	/** Used to store the Contact document's comments for void and reject case. */
	private String comments;

	/** The date when document was last updated. */
	private Date lastUpdated;

	/** The date when document was last sent. */
	private Date lastSent;

	/** Field for Team processing */
	private Integer payrollAddressNumber;

	/** The level of Approver ContactDocument is waiting for.
	 * It can be Production Approver/ Department Approver/ Production's Final Approver. */
	private ApprovalLevel approvalLevel;

	/** List of Attachments for the corresponding contact document */
	private Set<Attachment> attachments = new HashSet<>();

	/** Used to track row selection on Contact List page. */
	@Transient
	private boolean selected = false;

	/** Used to track row selection on Approvals tab. */
	@Transient
	private boolean checked = false;

	/** The relative status of the contact document, only used for view purpose */
	@Transient
	private ApprovalStatus viewStatus = null;

	/** String literal used to hold the the waiting for field for the contact document */
	@Transient
	private String waitingFor;

	/** The formatted version of {@link #timeSent} */
	@Transient
	private String timeSentFmtd = null;

	/** Used to disable jump from status icon on Distribution & Review tab. */
	@Transient
	private boolean disableJump = false;

	/** Temporary StartForm (Payroll Start) created during transfer process. */
	@Transient
	private StartForm transferStart;

	private boolean sentToPerformer = false;
	private boolean sentToUnion = false;
	private boolean sentToTPS = false;

	/**
	 * The default constructor
	 */
	public ContactDocument(){
		 setStatus(ApprovalStatus.PENDING);
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#refresh()
	 */
	public ContactDocument refresh() { // added by LS-2737
		return ContactDocumentDAO.getInstance().refresh(this);
	}

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

	/** See {@link #document}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Document_Id")
	public Document getDocument() {
		return document;
	}
	/** See {@link #document}. */
	public void setDocument(Document document) {
		this.document = document;
	}

	/** See {@link #packetName}. */
	@Column(name = "Packet_Name", nullable = true, length = 50)
	public String getPacketName() {
		return packetName;
	}
	/** See {@link #packetName}. */
	public void setPacketName(String packetName) {
		this.packetName = packetName;
	}

	/** See {@link #delivered}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Delivered", length = 0)
	public Date getDelivered() {
		return delivered;
	}
	/** See {@link #delivered}. */
	public void setDelivered(Date delivered) {
		this.delivered = delivered;
	}

	/** See {@link #timeSent}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "time_sent", length = 0)
	public Date getTimeSent() {
		return timeSent;
	}
	/** See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	@Transient
	public String getTimeSentFmtd() {
		if (timeSentFmtd == null) {
			if (timeSent != null) {
				DateFormat df = new SimpleDateFormat(Constants.TRANSFER_TOOLTIP_SENT_DATE_FORMAT);
				df.setTimeZone(SessionUtils.getProduction().getTimeZone());
				timeSentFmtd = df.format(timeSent);
			}
			else {
				timeSentFmtd = "(not sent)";
			}
		}
		return timeSentFmtd;
	}

	/** See {@link #documentChain}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Document_Chain_Id")
	public DocumentChain getDocumentChain() {
		return documentChain;
	}
	/** See {@link #documentChain}. */
	public void setDocumentChain(DocumentChain documentChain) {
		this.documentChain = documentChain;
	}

	/** See {@link #paperImage}. */
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Paper_Image_Id")
	public Image getPaperImage() {
		return paperImage;
	}
	/** See {@link #paperImage}. */
	public void setPaperImage(Image paperImage) {
		this.paperImage = paperImage;
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

	/** See {@link #contactDocEvents}. */
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contactDocument")
	@OrderBy("date")
	public List<ContactDocEvent> getContactDocEvents() {
		return contactDocEvents;
	}
	/** See {@link #contactDocEvents}. */
	public void setContactDocEvents(List<ContactDocEvent> contactDocEvents) {
		this.contactDocEvents = contactDocEvents;
	}

	/**
	 * @return The most recent employee signature if the form is not submitable.
	 *         This is not necessarily the first signature, as a document may be
	 *         signed, then rejected, then signed again by the employee. In the
	 *         case of a "submitable" document, this returns null, since we
	 *         should be waiting for the employee to sign.
	 */
	@Transient
	public ContactDocEvent getEmpSignature() {
		if ((getContactDocEvents() != null && getContactDocEvents().size() == 0) || getSubmitable()) {
			return null;
		}
		if (getFormType() == PayrollFormType.MODEL_RELEASE &&
				getContactDocEvents().get(getContactDocEvents().size()-1).getType() == TimedEventType.RECALL) {
			return null; // Last event is RECALL, no signature is valid
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType().equals(TimedEventType.SUBMIT)) {
				event = evt; // return last Submit event found.
			}
		}

		// LS-3163 - Added check for Model Release form.
		if (event == null && (! getFormType().isWtpa()) && ! getFormType().isModelRelease() &&
				(getFormType() != PayrollFormType.ACTRA_CONTRACT && getFormType() != PayrollFormType.UDA_INM)) {
			// No "SUBMIT" found, but this document allows employees to create "SIGN" events...
			for (ContactDocEvent evt : getContactDocEvents()) {
				if (evt.getType() == TimedEventType.SIGN) {
					event = evt;
				}
			}
		}
		return event;
	}

	/**
	 * @return The last signature with a type of "APPROVE" that is applied to
	 *         the document; returns null if no such signature is found.
	 */
	@Transient
	public ContactDocEvent getApprovalSignature() {
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.APPROVE) {
				event = evt;
				// don't break, as we want the last one
			}
		}
		return event;
	}

	/**
	 * @return The most recent Employer's (that is, approver's) signature
	 *         applied to the document; returns null if no such signature is
	 *         found. Includes both SIGN and APPROVE events.
	 *         <p>
	 *         For Model Release: (a) if the last event was a RECALL, returns
	 *         null; (b) event must be APPROVE.
	 */
	@Transient
	public ContactDocEvent getEmployerSignature() {
		if ((getContactDocEvents().size() == 0)) {
			return null;
		}
		if (getFormType() == PayrollFormType.MODEL_RELEASE &&
				getContactDocEvents().get(getContactDocEvents().size()-1).getType() == TimedEventType.RECALL) {
			return null; // Last event is RECALL, no signature is valid
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.APPROVE ||
					(evt.getType() == TimedEventType.SIGN && getFormType() != PayrollFormType.MODEL_RELEASE)
					) {
				event = evt; // return the most recent SIGN or APPROVE
			}
		}
		return event;
	}

	/**
	 * @return the last (most recent) signature applied to the document; returns
	 *         null if there are no signatures.
	 */
	@Transient
	public ContactDocEvent getLastSignature() {
		if (getContactDocEvents().size() == 0) {
			return null;
		}
		return getContactDocEvents().get(getContactDocEvents().size()-1);
	}

	/**
	 * @return the signature which made this document "Final", i.e., gave it the
	 *         final approval. Returns null if the document is not in a Final
	 *         approved state, or if there are no signatures.
	 */
	@Transient
	public ContactDocEvent getFinalSignature() {
		if (getContactDocEvents().size() == 0 || ! getStatus().isFinal()) {
			return null;
		}
		// We have one or more signatures, and the document is in a final approved
		// state, so the last signature should be the one that "finalized" it.
		// TODO this will need to change if the Section 3 signature for Form I-9 can be added later!
		return getContactDocEvents().get(getContactDocEvents().size()-1);
	}

	/**
	 * @return the signature from the I9 Section 3 area, if any.
	 */
	@Transient
	public ContactDocEvent getSec3Signature() {
		return null;
		/*if (getFormType() == null || getFormType() != PayrollFormType.I9) {
			return null; // no possible Section 3 signature
		}
		if (getContactDocEvents().size() < 3) {
			return null;
		}
		int index = getContactDocEvents().size() - 1;
		ContactDocEvent evt = getContactDocEvents().get(index);
		if (evt.getType() == TimedEventType.LOCK) { // don't return LOCK
			if (index > 2) { // there were 3 signatures prior to Lock
				index--; // so return the one before the LOCK
			}
			else { // only 2 signatures prior to Lock,
				return null; // not enough signatures for a section 3 to exist
			}
		}
		return getContactDocEvents().get(index);*/
	}

	/**
	 * @return the signature of preparer for the I9, if any.
	 */
	@Transient
	public ContactDocEvent getEmpPrepSignature () {
		if ((getContactDocEvents() != null && getContactDocEvents().size() == 0) ||
				getFormType() == null || getFormType() != PayrollFormType.I9) {
			return null;
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.PREPARE) {
				event = evt; // return last PREPARE event found.
			}
		}
		return event;
	}

	/**
	 * @return the last {@link com.lightspeedeps.type.TimedEventType#SIGN_OPT
	 *         SIGN_OPT} event found, if any. This was added for the ACTRA
	 *         contract. LS-1411
	 */
	@Transient
	public ContactDocEvent getOptSignature () {
		if ((getContactDocEvents().size() == 0)) {
			return null;
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.SIGN_OPT) {
				event = evt; // don't break, so the last one is returned
			}
		}
		return event;
	}

	/**
	 * @return the last {@link com.lightspeedeps.type.TimedEventType#AGREE
	 *         AGREE} event found, if any. This was added for the ACTRA
	 *         contract. LS-1434
	 */
	@Transient
	public ContactDocEvent getEmpAgrees () {
		if ((getContactDocEvents().size() == 0)) {
			return null;
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.AGREE) {
				event = evt; // don't break, so the last one is returned
			}
		}
		return event;
	}

	/**
	 * @return the last {@link com.lightspeedeps.type.TimedEventType#DISAGREE
	 *         DISAGREE} event found, if any. This was added for the ACTRA
	 *         contract. LS-1434
	 */
	@Transient
	public ContactDocEvent getEmpDisagrees () {
		if ((getContactDocEvents().size() == 0)) {
			return null;
		}
		ContactDocEvent event = null;
		for (ContactDocEvent evt : getContactDocEvents()) {
			if (evt.getType() == TimedEventType.DISAGREE) {
				event = evt; // don't break, so the last one is returned
			}
		}
		return event;
	}

	/** See {@link #employment}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Employment_Id")
	public Employment getEmployment() {
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	@Column(name = "Related_Form_Id")
	public Integer getRelatedFormId() {
		return relatedFormId;
	}
	public void setRelatedFormId(Integer relatedFormId) {
		this.relatedFormId = relatedFormId;
	}

	/** See {@link #formType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Form_Type" , nullable = false, length = 30)
	public PayrollFormType getFormType() {
		return formType;
	}
	/** See {@link #formType}. */
	public void setFormType(PayrollFormType formType) {
		this.formType = formType;
	}

	/** See {@link #comments}. */
	@Column(name = "Comments", nullable = true, length = 5000)
	public String getComments() {
		return comments;
	}
	/** See {@link #comments}. */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** See {@link #lastUpdated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Updated", length = 0)
	public Date getLastUpdated() {
		return lastUpdated;
	}
	/** See {@link #lastUpdated}. */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	/** See {@link #lastSent}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Sent", length = 0)
	public Date getLastSent() {
		return lastSent;
	}
	/** See {@link #lastSent}. */
	public void setLastSent(Date lastSent) {
		this.lastSent = lastSent;
	}

	/** See {@link #payrollAddressNumber}. */
	@Column(name = "payroll_address_number")
	public Integer getPayrollAddressNumber() {
		return payrollAddressNumber;
	}
	/** See {@link #payrollAddressNumber}. */
	public void setPayrollAddressNumber(Integer payrollAddressNumber) {
		this.payrollAddressNumber = payrollAddressNumber;
	}

	/** See {@link #approvalLevel}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Approval_Level" , nullable = true, length = 30)
	public ApprovalLevel getApprovalLevel() {
		return approvalLevel;
	}
	/** See {@link #approvalLevel}. */
	public void setApprovalLevel(ApprovalLevel approvalLevel) {
		this.approvalLevel = approvalLevel;
	}

	/** See {@link #attachments}. */
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "contactDocument")
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	/** See {@link #attachments}. */
	public void setAttachments(Set<Attachment> attachments) {
		this.attachments = attachments;
	}

	@SuppressWarnings({"rawtypes"})
	@Override
	@Transient
	public List<? extends SignedEvent> getEvents() {
		return getContactDocEvents();
	}

	/** See {@link #viewStatus}. */
	@Transient
	public ApprovalStatus getViewStatus() {
		if (viewStatus == null) {
			log.warn("CD has null viewStatus: replaced with status");
			viewStatus = status;
		}
		return viewStatus;
	}
	/** See {@link #viewStatus}. */
	public void setViewStatus(ApprovalStatus viewStatus) {
		this.viewStatus = viewStatus;
	}

//	@Transient // LS-2737 this method appears to be unused.
//	public ApprovalStatus getWorseStatus() {
//		return ContactDocumentDAO.getInstance().findWorseStatus(contact.getId(),documentChain.getId());
//	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
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

	@Transient
	public boolean isSummarySheet() {
		return formType == PayrollFormType.SUMMARY;
	}

	@Transient
	@Override
	public Production getProduction() {
		return getContact().getProduction();
	}

	@Transient
	@Override
	public Department getDepartment() {
		return getEmployment().getDepartment();
	}

	@Transient
	public String getWaitingFor() {
		return waitingFor;
	}
	public void setWaitingFor(String waitingFor) {
		this.waitingFor = waitingFor;
	}

	/** See {@link #transferStart}. */
	@Transient
	public StartForm getTransferStart() {
		return transferStart;
	}
	/** See {@link #transferStart}. */
	public void setTransferStart(StartForm transferStart) {
		this.transferStart = transferStart;
	}

	/** See {@link #disableJump}. */
	@Transient
	public boolean getDisableJump() {
		return disableJump;
	}
	/** See {@link #disableJump}. */
	public void setDisableJump(boolean disableJump) {
		this.disableJump = disableJump;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((contact == null) ? 0 : contact.hashCode());
		result = prime * result
				+ ((document == null) ? 0 : document.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		ContactDocument other;
		try {
			other = (ContactDocument)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getContact() == null) {
			if (other.getContact() != null)
				return false;
		}
		else if (! getContact().equals(other.getContact())) {
			return false;
		}
		if (getDocument() == null) {
			if (other.getDocument() != null) {
				return false;
			}
		}
		else if (! getDocument().equals(other.getDocument()))
			return false;
		return true;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ContactDocument other) {
		if (equals(other)) {
			return 0;
		}
		int ret = 0;
		if (contact == null) {
			if (other.contact != null)
				return -1;
		}
		else {
			ret = contact.compareTo(other.contact);
		}
		if (ret == 0) {
			if (document == null) {
				if (other.document != null) {
					return -1;
				}
			}
			else {
				ret = document.compareTo(other.document);
			}
		}
		return ret;
	}

	/**
	 * Compare using the specified field, with the specified ordering.
	 *
	 * @param other The ContactDocument to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(ContactDocument other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}
	/**
	 * Compare using the specified field.
	 *
	 * @param other The ContactDocument to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(ContactDocument other, String sortField) {
		int ret;
		if (other == null) {
			ret = 1;
		}
		else if (sortField.equals(SORTKEY_TRANSFER)) {
			ret = NumberUtils.compare(getContact().getId(), other.getContact().getId());
			if (ret == 0) {
				ret = NumberUtils.compare(getEmployment().getId(), other.getEmployment().getId());
				if (ret == 0) {
					ret = NumberUtils.compare(getFormType().ordinal(), other.getFormType().ordinal());
					if (ret == 0) { // for equal status, sort descending (reverse) by W/E date & adjusted
							ret = compareTo(other);
					}
				}
			}
		}
		else if (sortField.equals(SORTKEY_FORM_TYPE)) {
			ret = NumberUtils.compare(getFormType().ordinal(), other.getFormType().ordinal());
			if (ret == 0) { // for equal status, sort descending (reverse) by W/E date & adjusted
					ret = compareTo(other);
			}
		}
		else { // should not happen!
			log.error("Unknown sort-field value specified =`" + sortField + "`");
			ret = 0;
		}

		return ret;
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = "[";
		s += super.toString();
		s += "c=" + (getContact()==null ? "null" : getContact().toString());
		s += ", doc=" + (getDocument()==null ? "null" : getDocument().toString());
		s += "]";
		return s;
	}

	/**
	 * @param ex The exporter to use for outputing the document's data.
	 * @param isTeamPayroll True if the current payroll service is for Team.
	 */
	public void exportFlat(Exporter ex, boolean isTeamPayroll) {
		// output standard export 'header' items
		ex.append(getFormType().getExportType()); // identifies the form type
		ex.append(getEmployment().getId()); // unique ER id
		ex.append(getRelatedFormId());		// id of form record
		ex.append(getId());					// CD's id
		if (getLastSent() == null) {		// never sent (transferred) before
			ex.append(Constants.DOC_EXPORT_STATUS_NEW);
		}
		else {
			ex.append(Constants.DOC_EXPORT_STATUS_UPDATED);
		}
		ex.appendDateTime(getLastUpdated());		// time form was last updated (saved)
		ex.appendDateTime(getDelivered());			// time form was delivered to the employee
		// end 'header' fields

		if (getFormType().isWtpa()) {
			// first field for WTPA is the state code = 1st 2 chars of FormType
			ex.append(getFormType().name().substring(0, 2));
		}

		@SuppressWarnings("rawtypes")
		Form form;
		if (getFormType() == PayrollFormType.START && getTransferStart() != null) {
			form = getTransferStart();
		}
		else {
			// get related form, or a blank form if it hasn't been persisted yet.
			form = FormService.findRelatedOrBlankForm(this);
		}

		if (getFormType() == PayrollFormType.START) {
			((StartForm)form).exportFlat(ex, isTeamPayroll); // add the StartForm's export fields
		}
		else {
			form.exportFlat(ex); // add the form's export fields
		}

		// Export common "comments" field
		ex.append(StringUtils.saveHtml(getComments()));

		// Signatures - ones defined + fill to 6
		int n = 0;
		for (ContactDocEvent tce : getContactDocEvents()) {
			if (tce.getType().getSigned()) {
				tce.exportFlat(ex);
				n++;
				if (n >= Constants.DOC_EXPORT_NUMBER_SIGNATURES) {
					break;
				}
			}
		}
		ContactDocEvent tce = new ContactDocEvent();
		for (int i = n; i < Constants.DOC_EXPORT_NUMBER_SIGNATURES; i++) {
			tce.exportFlat(ex);
		}
	}

	/** See {@link #sentToPerformer. */
	@Column(name = "Sent_To_Performer")
	public boolean isSentToPerformer() {
		return sentToPerformer;
	}

	/** See {@link #sentToPerformer. */
	public void setSentToPerformer(boolean sentToPerformer) {
		this.sentToPerformer = sentToPerformer;
	}

	/** See {@link #sentToUnion. */
	@Column(name = "Sent_To_Union")
	public boolean isSentToUnion() {
		return sentToUnion;
	}

	/** See {@link #sentToUnion. */
	public void setSentToUnion(boolean sentToUnion) {
		this.sentToUnion = sentToUnion;
	}

	/** See {@link #sentToTPS. */
	@Column(name = "Sent_To_TPS")
	public boolean isSentToTPS() {
		return sentToTPS;
	}

	/** See {@link #sentToTPS. */
	public void setSentToTPS(boolean sentToTPS) {
		this.sentToTPS = sentToTPS;
	}

}
