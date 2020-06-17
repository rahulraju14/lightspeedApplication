package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.text.*;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.*;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.EmploymentWrapper;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.image.*;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.onboard.form.*;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.timecard.*;
import com.lightspeedeps.web.user.*;
import com.lightspeedeps.web.util.*;
import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;

/**
 * This class is the backing bean for the Start Forms mini tab, under Payroll
 * sub-main tab. It creates and manages the list of contactDocuments, for the
 * logged in contact or user.  It interfaces to the various beans that display,
 * edit, and save the various on-boarding forms, such as the I-9 and W-4.
 */
@ManagedBean
@ViewScoped
public class ContactFormBean extends ApproverDocBase implements Serializable, ImageManage, PopupHolder, Disposable {

	private static final long serialVersionUID = -2992658109222398053L;
	private static final Log LOG = LogFactory.getLog(ContactFormBean.class);

	// Mini-tabs on Payroll | Start Forms page
	public static final int TAB_DOCUMENTS = 0; // List of documents
//	private static final int TAB_EVENTS = 1;	// Document events (I-9 only)
//	private static final int TAB_INSTRUCTIONS = 2; // Document instructinos (I-9 only)
	private static final int TAB_ATTACHMENTS = 3; // Document attachments

	// Exempt popup action
	private static final int ACT_EXEMPT_SELECTED = 10;

	// private static String fileName = "updated_";

	/** The list of contact documents corresponding to the logged in user */
//	private List<ContactDocument> contactDocumentList = null;

	/** The contactDocument instance currently selected (and probably displayed). */
	private ContactDocument contactDocument;

	/** The contact DAO instance . */
	private transient ContactDAO contactDAO;

	/** The contact document DAO instance . */
	private transient ContactDocumentDAO contactDocumentDAO;

	/** The document DAO instance . */
	private transient DocumentDAO documentDAO;

	/** The employment DAO instance . */
	private transient EmploymentDAO employmentDAO;

	/** The user DAO instance . */
	private transient UserDAO userDAO;

	/** The AttachmentDAO instance . */
	private transient AttachmentDAO attachmentDAO;

	/** The content DAO instance . */
	private transient ContentDAO contentDAO;

	/** The address document DAO instance . */
	private transient AddressDAO addressDAO;

	/** The form I9 DAO instance . */
	private transient FormI9DAO formI9DAO;

	/** The xdf content DAO instance . */
	private transient XfdfContentDAO xfdfContentDAO;

	/** The form W4 DAO instance . */
	private transient FormW4DAO formW4DAO;

	/** The ProjectDAO instance . */
	private transient ProjectDAO projectDAO;

	/** The ContactDocEventDAO instance . */
	private transient ContactDocEventDAO contactDocEventDAO;

	/** The ProductionDAO instance . */
	private transient ProductionDAO productionDAO;

	/** The ApproverDAO instance . */
	private transient ApproverDAO approverDAO;

	/** The StartFormDAO instance . */
	private transient StartFormDAO startFormDAO;

	/** The DocumentChainDAO instance . */
	private transient DocumentChainDAO documentChainDAO;

	/** The WeeklyTimecardDAO instance . */
	private transient WeeklyTimecardDAO weeklyTimecardDAO;

	/** The SignatureBoxDAO instance . */
	private transient SignatureBoxDAO signatureBoxDAO;

	/** The FormDepositDAO instance . */
	private transient FormDepositDAO formDepositDAO;

	/** The ApprovalPathAnchorDAO instance . */
	private transient ApprovalPathAnchorDAO approvalPathAnchorDAO;

	/** The ApprovalPathDAO instance . */
	private transient ApprovalPathDAO approvalPathDAO;

	/** The ApproverGroupDAO instance . */
	private transient ApproverGroupDAO approverGroupDAO;

	/** The FolderDAO instance . */
	private transient FolderDAO folderDAO;

	/** The FolderDAO instance . */
	private transient PayrollPreferenceDAO payrollPreferenceDAO;

	/** The AttachmentBean instance . */
	private transient AttachmentBean attachmentbean;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	/**
	 * the string variable used to hold the name of the document for the currently
	 * selected document chain
	 */
	private String clickedDocumentName;

	/** The bean managing the currently selected form. */
	@SuppressWarnings("rawtypes")
	private StandardFormBean formBean;

	/** The manager for uploading scanned timecard images. */
	private ImageManager imageManager;

	/** True, if it a commercial production */
	private boolean aicp;

	/**
	 * True if the current production allows attachments to an I9 form. Controls tab
	 * visibility in JSF.
	 */
	private boolean allowI9attachment;

//	/** True if the "change PIN" dialog should be displayed. */
//	private boolean showChangePin;

	/** "Submit" or "Sign" or "Sign & Submit" action, usually by the employee. */
	private static final int ACT_SUBMIT = 21;

	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 22;

	private static final int ACT_VOID = 23;

	/** "Upload" action code for popup confirmation/prompt dialog. */
	private static final int ACT_UPLOAD = 24;

	/** "Lock" action code for popup confirmation/prompt dialog. */
	private static final int ACT_LOCK = 25;

	/** "Lock" action code for popup confirmation/prompt dialog. */
	private static final int ACT_AUTO_FILL = 26;

	private static final int ACT_UPDATE_USER = 27;

	private static final int ACT_RECALL = 28;

	private static final int ACT_RECALL_TO_EMPL = 29;

	private static final int ACT_PULL = 31;

	/** "Sign" action code for popup confirmation/prompt dialog. */
	public static final int ACT_EMPLOYER_SIGN = 32;

	private static final int ACT_SEND = 33;

	/** Action code used when a "Preparer" is signing an I-9. */
	private static final int ACT_PREPARE = 34;

	/**
	 * "Sign" action code for employee signature popup confirmation/prompt dialog.
	 */
	public static final int ACT_EMPLOYEE_SIGN = 35;

	/** "Initial" action code for employee popup confirmation/prompt dialog. */
	public static final int ACT_EMPLOYEE_INITIAL = 36;

	/** "Acknowledge" action code for popup confirmation/prompt dialog. */
	private static final int ACT_ACK = 37;

	/** "Attach" action code for popup confirmation/prompt dialog. */
	private static final int ACT_ATTACH = 38;

	/**
	 * Action code for popup confirmation/prompt dialog to delete documents with its
	 * attachments.
	 */
	private static final int ACT_DELETE_ATTACHMENT = 39;

	/** "Initial" action code for employee popup confirmation/prompt dialog. */
	public static final int ACT_EMPLOYEE_INITIAL_DISAGREE = 40;

	/** "Initial" action code for employer popup confirmation/prompt dialog. */
	public static final int ACT_EMPLOYER_INITIAL = 41;

	/**
	 * An 'extra' or 'optional' "Submit" or "Sign" or "Sign & Submit" action,
	 * usually by the employee.
	 */
	private static final int ACT_SIGN_OPT = 42;

	// LS-1963
	private static final int ACT_OVERIDE_INITIAL = 43;

	// LS-4240 Second signature if doing separate validation
	// than first employee submit;
	private static final int ACT_SECOND_EMPLOYEE_SIGN = 44;

	private boolean standardDoc = false;

	/**
	 * Wrapper object list used to hold the occupation wise contact document list to
	 * show the employment record wise data table
	 */
	private List<EmploymentWrapper> empContactList = new ArrayList<>();

	/** True, iff the Summary Sheet is selected from the data table */
	private boolean isSummarySheet = false;

	/** True, iff the Form I9 is selected from the data table */
	private boolean isFormI9 = false;

	/** True, iff the Form W4 is selected from the data table */
	private boolean isFormW4 = false;

	/**
	 * Current contact instance holds the currently logged in contact or the
	 * employment's contact
	 */
	// private Contact currentContact;

	/**
	 * String literal used to hold the current contact's name to display on the
	 * heading of the Payroll / Start Forms
	 */
	private String currentContactName;

	/**
	 * True if the user jumps from list of available onboarding documents to Payroll
	 * / Start forms, to view a blank and unassigned copy of the form.
	 */
	private boolean isPreviewDocument = false;

	/** The form type when 'previewing' a form. */
	private PayrollFormType previewType;

	/** True if the user jumps from Cast n Crew to payroll start forms */
	private boolean isJump = false;

	/**
	 * True if the user clicks Edit Master button, to view/edit the 'master' XFDF of
	 * the current Document in the text editor.
	 */
	private boolean editMaster = false;

	/**
	 * True if the user clicks Edit Private button, to view the 'private' XFDF
	 * (corresponding to a single CD instance) of the current Document in the text
	 * editor.
	 */
	private boolean editPrivate = false;

	/** String literal holds the XFDF String for the selected document from Mongo */
	private String textEditor = "";

	private static final String NO_MASTER = "No Master XFDF Found";

	/**
	 * The signature index of the last button clicked, for custom documents. This
	 * will be used as the suffix of the signature field name for the field to be
	 * updated with the signature-event text.
	 */
	private int btnSignIndex = 1;

	/** List of contacts drop down to be shown on Payroll Start Forms tab */
	private List<SelectItem> contactsDL;

	private Contact selectedContact;

	/**
	 * True if the user jumps from Onboarding/ Review & Approval to payroll start
	 * forms
	 */
	private boolean isOnboardingJump = false;

	/**
	 * True if the user has chosen to view the employment wise contact document list
	 * for all project in a commercial production
	 */
	private boolean showAllProjects = false;

	/**
	 * If true, then display Payroll Start forms that correspond to Model Release
	 * forms. These Starts are normally hidden. LS-4504
	 */
	private boolean showMrStarts = false;

	/**
	 * Boolean field used to render the 'Edit' button on the Payroll/Start form tab.
	 * True iff the current user has authority to edit one or more fields in the
	 * current document.
	 */
	private boolean editAuth = false;

	/** Used to show message on startform view. */
	private String infoMessage = null;

	/**
	 * Boolean field used to render the Approve/Reject buttons on the Payroll/Start
	 * form tab, or, in some cases, for embedded (in-document) "Sign & Approve" (or
	 * equivalent) buttons. It is True when the user has the right to Approve or
	 * Reject the current document.
	 */
	private boolean mayApprove = false;

	/**
	 * True iff an approver (employer) may Sign the current document. Generally used
	 * to render the Employer's "Sign" button as enabled if true.
	 */
	private boolean appSign = false;

	/** True iff the current contact is an Editor for the current document. */
	private boolean isEditor = false;

	/** True if the user jumps from Timecard page to payroll start forms */
//	private boolean isTimecardJump = false;

	/**
	 * Map of Project and map for document chains for current contact in that
	 * project. Inner map holds the document chains and list of departments for
	 * which the current contact is an approver. List of department will be null if
	 * contact is Production approver for the document chain.
	 */
	private Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap = new HashMap<>();

	/** The Primary folder, where we will store replacements of the documents. */
	private Folder primaryFolder = null;

	/**
	 * Select item list for the document list drop down, for upload user document
	 * popup.
	 */
	private List<SelectItem> documentList;

	/**
	 * True if we are in the "My Starts" environment. Set when we first try to
	 * access the current Production, in {@link #getProduction()}.
	 */
	private boolean myStarts;

	/**
	 * Production whose Start documents will be listed. It will be Current
	 * Production for Start Forms tab and for My Starts tab it will be the
	 * production selected in the drop-down.
	 */
	private Production production = null;

	/**
	 * A List containing the Production`s available to a user on the My Starts page.
	 * The value field of each SelectItem is the Production.id and the label is the
	 * Production.name.
	 */
	private List<SelectItem> productionList;

	/**
	 * The Production.id value of the selected Production. Production whose
	 * StartForms will be listed. This is used for the My Starts page.
	 */
	private Integer selectedProdId = null;

	/** Name of occupation for which user is uploading new replacement. */
	private String occupationOfReplacement = null;

	/**
	 * True iff the production selection menu (drop-down) should be disabled. We
	 * disable it when the Submit dialog box is displayed, so that password managers
	 * (like RoboForm) won't save or change the drop-down setting.
	 */
	boolean prodSelectDisabled = false;

	/** Used to hold the user to be updated with new form details. */
	private User userToUpdate = null;

	/**
	 * A List containing the Commercial Production`s list of projects, only those
	 * projects for which the current contact has Employment records user on the My
	 * Starts page. The value field of each SelectItem is the Project.id and the
	 * label is the Project.name.
	 */
	private List<SelectItem> projectList;

	/**
	 * The Project.id value of the selected Project; only used for commercial
	 * Productions. Only StartForms associated with this Project will be listed.
	 */
	private Integer selectedProjectId = null;

	/**
	 * True iff the current user has 'pull' capability with respect to the currently
	 * displayed document. A person who is in the approval chain, but has not yet
	 * approved it, has this authority. Also, the document must have been submitted
	 * already (may not be in OPEN status).
	 */
	private boolean pullAuth = false;

	/**
	 * True iff the current user has 'recall' capability with respect to the
	 * currently displayed document. A person who has already approved the document
	 * has this capability, unless the document was Rejected back to an approver
	 * earlier in the chain than the current user.
	 */
	private boolean recallAuth = false;

	/**
	 * True iff the current user is an approver for the selected document; that is,
	 * they are in the Approval Path for the document. (They are not necessarily the
	 * next approver of the document.)
	 */
	private boolean isApprover = false;

	/** True iff the current user is Pseudo Approver. */
	private boolean pseudoApprover;

	/**
	 * If true,previously selected contactDocument was a custom doc; used in
	 * refreshing WebViewer (by forcing page refresh) if next selected doc is also
	 * custom.
	 */
	private boolean priorCustomDocument = false;

	private final Disposer disposer = Disposer.getInstance();

	/**
	 * True iff the selected contact document is unavailable for the current user.
	 */
	private boolean isUnavailable;

	/**
	 * Map of Project and list of document chains for which current contact is an
	 * editor in that project.
	 */
	private Map<Project, List<DocumentChain>> currContactChainEditorMap = new HashMap<>();

	/** True iff we have initialized ourselves. */
	private boolean initialized = false;

	/**
	 * The dialog box action currently being processed. Set by
	 * {@link #confirmOk(int)} or {@link #confirmCancel(int)}. It may be used by
	 * form beans to customize processing.
	 */
	private int currentAction;

	/** UI text: "project" for US, "production" for Canada */
	private String projectTitle;
	/** UI text: "projects" for US, "productions" for Canada */
	private String projectsTitle;
	/** UI text: "Production" for US, "Agency" for Canada */
	private String startFormLabelTitle;

	/**
	 * true if user is "Auto Fill" button should be displayed. Currently used for
	 * Canadian ACTRA Work Permit.
	 */
	private boolean showAutoFill;

	/** The default constructor. */
	public ContactFormBean() {
		super("Contact.");
		super.setMessagePrefix("ApprovalBean.");
		super.setAttributePrefix("ContactForm.");
		disposer.register(this);
		LOG.debug(getvUser());
		setScrollable(true);
//		HeaderViewBean.getInstance().setMiniTab(0);
		pseudoApprover = AuthorizationBean.getInstance().getPseudoApprover();
		getProduction(); /** this also sets {@link #myStarts} */
		showAllProjects = false;
//		if (! (Constants.ATTR_ONBOARDING_CONTACTDOC_ID != null && Constants.ATTR_ONBOARDING_EMPLOYMENT_ID != null
//				&& Constants.ATTR_CONTACT_ID != null)) {
//			SessionUtils.put(Constants.ATTR_LAST_PROJECT_ID, null);
//		}
		setvContact(getContactDAO().findByUserProduction(getvUser(), production));

		findDocumentChainsForCurrentContact();
		findDocumentChainEditorForCurrentContact();
		// To delete the uploaded documents, if they are not uploaded successfully.
		Integer documentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_UPLOADED_DOCUMENT_ID);
		if (documentId != null) {
			Document document = getDocumentDAO().findById(documentId);
			if (document != null && document.getDocChainId() != null && document.getDocChainId() == 0) {
				getDocumentDAO().deleteDocument(document.getId());
			}
		}
		// To delete the uploaded attachments, if they are not uploaded successfully.
		Integer attachmentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_UPLOADED_ATTACHMENT_ID);
		if (attachmentId != null) {
			Attachment attachment = getAttachmentDAO().findById(attachmentId);
			if (attachment != null && attachment.getContactDocument() == null) {
				getAttachmentDAO().delete(attachment);
			}
		}

		Integer contactDocId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_CONTACTDOC_ID);
		if (contactDocId != null && contactDocId != 0) {
			LOG.debug("Onboarding Jump");
			isOnboardingJump = true;
		}
		if (myStarts) {
			// in My Starts, logged in user can only see their own docs.
			selectedContact = getvContact();
		}
		else {
			Integer employmentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID);
			Integer contactId = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
			Integer startFormId = SessionUtils.getInteger(Constants.ATTR_START_FORM_ID);
			if (! isOnboardingJump && contactId != null && contactId != 0 && startFormId != null  && startFormId != 0) {
				// From Full Timecard view page, or from Start Form List page
//				isTimecardJump = true;
				Contact contact = getContactDAO().findById(contactId);
				viewStartForm(contact, startFormId);
				setSelectedContact(contact);
			}
			else if (employmentId != null && employmentId != 0 /*&& ! isTimecardJump*/) {
				LOG.debug("Contact list jump");
				isJump = true;
				Employment employment = getEmploymentDAO().findById(employmentId);
				// If employment is deleted.
				if (employment == null) {
					if (getvContact() != null && getvContact().getEmployments() != null &&
							(! getvContact().getEmployments().isEmpty())) {
						for (Employment emp : getvContact().getEmployments()) {
							if ((! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_LS_ADMIN)) &&
									(! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) &&
									emp.getProject().equals(SessionUtils.getCurrentOrViewedProject())) {
								employment = emp;
								SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, emp.getId());
								break;
							}
						}
					}
				}
				if (employment != null) {
					selectedContact = employment.getContact();
				}
			}
			else if (contactId != null && contactId != 0) {
				// default viewed contact set elsewhere (e.g., on Cast&Crew page)
				Contact contact = getContactDAO().findById(contactId);
				setSelectedContact(contact);
			}
			else {
				LOG.debug("");
//				currentContact = SessionUtils.getCurrentContact();
//				if (currentContact == null) { // My Starts environment
//					currentContact = ContactDAO.getInstance().findByUserProduction(getvUser(), production);
//				}
//				setvContact(currentContact);
				if (getvContact().getEmployments().size() > 0) {
					SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, getvContact().getEmployments().get(0).getId());
				}
			}

		}
		if (selectedContact != null) {
			currentContactName = selectedContact.getUser().getLastNameFirstName();
		}
		else if (myStarts) {
			currentContactName = getvUser().getLastNameFirstName();
		}
		else {
			currentContactName = getvContact().getUser().getLastNameFirstName();
		}

		if (selectedContact == null) {
			setSelectedContact(getvContact());
		}

		previewStandardDocument();
		if (isOnboardingJump) {
			selectContactDocument();
			if (contactDocument == null) { // selected doc no longer exists
				selectDefaultDocument();
			}
		}
		else {
			LOG.debug("");
			if (!isPreviewDocument && (contactDocument == null)) {
				selectDefaultDocument();
			}
		}
		if ((!isOnboardingJump && !isJump) || myStarts) {
			clearSessionVariables();
		}
//		checkTab(); // set last-used mini-tab
		calculateAuthFlags(); // calc editAuth, mayApprove, ...

		if (getProduction() != null) {
			aicp = production.getType().isAicp();
			allowI9attachment = !(production.getPayrollPref().getI9Attachment() == ExistencePolicy.FORBID); // fix LIE.
																											// LS-
		}
		/*
		 * contactDocument = (ContactDocument)
		 * SessionUtils.get(Constants.ATTR_PAYROLL_CONTACT_DOCUMENT_ID); if
		 * (contactDocument != null) { createSelectedRow(contactDocument); } else { if
		 * (getContactDocumentList() != null && getContactDocumentList().size() > 0) {
		 * contactDocument = getContactDocumentList().get(0);
		 * createSelectedRow(contactDocument); } }
		 */
		createContactsDL();
		if (contactDocument != null) {
			getAttachmentBean().setDefaultAttachment(contactDocument, null);
		}

		// Checking the user for US or CANADA setting the values to the FormPage.xhtml
		// common fields
		if (SessionUtils.getCurrentUser().getShowUS()) {
			setProjectTitle(Constants.PROJECT_TEXT_US);
			setProjectsTitle(Constants.PROJECT_S_TEXT_US);
			setStartFormLabelTitle(Constants.STANDARD_PRODUCTION_TEXT);
		}
		else if(SessionUtils.getCurrentUser().getShowCanada()) {
			setProjectTitle(Constants.PROJECT_TEXT_CANADA);
			setProjectsTitle(Constants.PROJECT_S_TEXT_CANADA);
			setStartFormLabelTitle(Constants.CANADA_PRODUCTION_TEXT);
		}

		LOG.debug("");
	}

	/**
	 * Handle some initialization after construction of the bean. This is delayed
	 * until we are called via a JSP reference on the Start Forms mini-tab.
	 */
	private void init() {
		try {
			LOG.debug("");
			initialized = true;
			String attr = Constants.ATTR_CURRENT_MINI_TAB + ".prstartform";
			if (SessionUtils.get(attr) != null) {
				String attrHdr = Constants.ATTR_CURRENT_MINI_TAB + "." + getAttributePrefix();
				SessionUtils.put(attrHdr, SessionUtils.get(attr));
			}
			checkTab(); // set last-used mini-tab
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Called due to hidden field value in our JSP page. Make sure we're initialized
	 * to show something. Note that this must be public as it is called from JSP.
	 */
	@Override
	public boolean getSetUp() {
		try {
			LOG.debug("");
			if (!initialized) {
				LOG.debug("");
				init();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Utility method used to open the StartForm when user jumps from Timecard's
	 * view starts.
	 */
	private void viewStartForm(Contact contact, Integer startFormId) {
		try {
			LOG.debug("");
			RowState state = new RowState();
			state.setSelected(true);
			LOG.debug("StartForm : " + startFormId + " for contact: " + contact);
			if (contact != null) {
				createEmpContactList(contact);
				wrapLoop:
				for (EmploymentWrapper ep : empContactList) {
					for (ContactDocument cd : ep.getContactDocument()) { //allowing document to process further if type is MODEL_RELEASE LS-4590
						if ((cd.getFormType().equals(PayrollFormType.START) ||
								cd.getFormType().equals(PayrollFormType.MODEL_RELEASE)) &&
								cd.getRelatedFormId() != null &&
								(cd.getRelatedFormId().equals(startFormId))) {
							contactDocument = cd;
							LOG.debug("contact document: " + cd.getId());
							break wrapLoop;
						}
					}
				}
			}

			if (contactDocument != null) {
				stateMap.put(contactDocument, state);
				if (contactDocument.getDocument() != null) {
					setClickedDocumentName(contactDocument.getDocument().getNormalizedName());
				}
				// get any type of bean instance LS-4590
				formBean = getBeanInstance(contactDocument.getFormType());
				if (contactDocument.getDocument().getMimeType().equals(MimeType.LS_FORM)) {
					standardDoc = true;
				}
				setSelectedContact(contact);
				if (formBean != null) {
					// let individual form beans do specialized document setup
					formBean.rowClicked(contactDocument);
					formBean.setvContact(getvContact());
					formBean.setProduction(production);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method to select and show a document as per conditions. */
	private void selectDefaultDocument() {
		try {
			LOG.debug("");
			setStandardDoc(false);
			setClickedDocumentName("");
			if (selectedContact != null) {
				createEmpContactList(selectedContact);
				// If current contact is an approver then show summary sheet
				AuthorizationBean bean = AuthorizationBean.getInstance();
				boolean hasPermission = Permission.APPROVE_START_DOCS.inMask(getvContact().getPermissionMask()) ||
								bean.hasPermission(getvContact(), (Permission.APPROVE_START_DOCS));
				if (hasPermission && ! myStarts) {
					selectSummarySheet();
				}
				else {
					// If current contact is not an approver then show Payroll if exist else show a message.
					List<ContactDocument> contactDocList = null;
					if (getEmpContactList().size() > 0) {
						contactDocList = getEmpContactList().get(0).getContactDocument();
					}
					// contactDocList = ContactDocumentDAO.getInstance().findByProperty("contact",
					// selectedContact);
					boolean payrollStart = false;
					if (contactDocList != null && contactDocList.size() > 0) {
						infoMessage = "Click on a document in the list on the left to view its content.";
						for (ContactDocument cd : contactDocList) {
							if (! cd.isSummarySheet() && cd.getDocument() != null &&
									cd.getDocument().getName().equals(PayrollFormType.START.getLabel())) {
								payrollStart = true;
								infoMessage = null;
								break;
							}
						}
						if (payrollStart) {
							selectFirstPayrollStart();
						}
					}
					else {
						infoMessage = "No Start Forms are available to you at this time.";
						if (production == null) {
							if(SessionUtils.getCurrentUser().getShowCanada()) {
								infoMessage = "Please select an " + Constants.CANADA_PRODUCTION_TEXT + " to view Start Forms.";
							}
							else {
								infoMessage = "Please select a " + Constants.STANDARD_PRODUCTION_TEXT + " to view Start Forms.";
							}
						}
					}
				}
				calculateAuthFlags();
			}
			else {
				infoMessage = "No Start Forms are available to you at this time.";
				if (production == null && ! getProductionList().isEmpty()) {
					if(SessionUtils.getCurrentUser().getShowCanada()) {
						infoMessage = "Please select an " + Constants.CANADA_PRODUCTION_TEXT + " to view Start Forms.";
					}
					else {
						infoMessage = "Please select a " + Constants.STANDARD_PRODUCTION_TEXT + " to view Start Forms.";
					}				}
			}
			// getAttachmentBean().setContactDocument(contactDocument);
			getAttachmentBean().setDefaultAttachment(contactDocument, null);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Used to return the instance of the ContactFormBean */
	public static ContactFormBean getInstance() {
		return (ContactFormBean) ServiceFinder.findBean("contactFormBean");
	}

	/**
	 * Listener used for row selection, sets the contactDocument with the currently
	 * selected row object .
	 */
	public void listenRowClicked(SelectEvent evt) {
		try {
			if (contactDocument != null && getvUser() != null) {
				if (editMode || (formBean != null && formBean.getEditMode())) {
					// In Edit on built-in doc, or on custom doc, and switching to a different row;
					// existing CD is probably locked, so unlock it. Need to refresh to get current
					// lock info.
					contactDocument = getContactDocumentDAO().refresh(contactDocument);
					getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
				}
			}
			isFormI9 = false;
			isFormW4 = false;
			editMaster = false;
			setEditMode(false);
			setInfoMessage(null);
			isApprover = false;
			editAuth = false;
			pullAuth = false;
			recallAuth = false;
			appSign = false;
			mayApprove = false;
			isEditor = false;
			contactDocument = (ContactDocument) evt.getObject();
//			getAttachmentBean().setAttachmentList(null);
			formBean = null; // switched docs, will need new form bean
			isUnavailable = contactDocument.getDisableJump();
			if (!refreshContactDoc()) { // doc was deleted by another user
				return; //
			}
			contactDocument.setDisableJump(isUnavailable);
			getAttachmentBean().setContactDocument(contactDocument); // inform AttachmentBean of change in CD
			// Force refresh for webviewer for different custom document with same status.
			if (priorCustomDocument && (contactDocument.getFormType() != PayrollFormType.OTHER)) {
				LOG.debug("");
				priorCustomDocument = false; // switching to builtin doc, so ignore prior doc setting.
			}
			SessionUtils.put(Constants.ATTR_PAYROLL_CONTACT_DOCUMENT_ID, contactDocument.getId());
			SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_FORM_I9_ID, null);
			SessionUtils.put(Constants.ATTR_START_FORM_ID, null);
			setvContact(getContactDAO().refresh(getvContact()));
			LOG.debug("Disable document: " + contactDocument.getDisableJump());
			infoMessage = "The selected document is only available to approvers";
			if (contactDocument.getFormType() == PayrollFormType.OTHER) {
				setClickedDocumentName(contactDocument.getDocument().getDisplayName());
			}
			else {
				setClickedDocumentName(contactDocument.getFormType().getLabel());
			}
			if (! isUnavailable) {
				setInfoMessage(null);
				LOG.debug("ContactDocument id: " + contactDocument.getId());
				DocumentEventsBean.getInstance().setDocumentEventList(null);
				textEditor = NO_MASTER;
				// For the Documents with "Read Only" or "No Action Required (RCV)"  Employee Action.
				if (contactDocument.getDocument() != null && contactDocument.getContact().equals(getvContact()) &&
						contactDocument.getDocument().getEmployeeAction() != null &&
						contactDocument.getDocument().getEmployeeAction().isReadOnly() &&
						contactDocument.getStatus() != ApprovalStatus.APPROVED &&
						contactDocument.getStatus() != ApprovalStatus.PENDING &&
						(! contactDocument.getStatus().isSealed())) {
					LOG.debug("");
					contactDocument.setStatus(ApprovalStatus.APPROVED);
					getContactDocumentDAO().attachDirty(contactDocument);
					getContactDocEventDAO().createEvent(contactDocument, TimedEventType.VIEW);
					clearEmpTable();
				}
				calculateAuthFlags();

				isSummarySheet = false;
				standardDoc = true;
				formBean = getBeanInstance(contactDocument.getFormType());
				switch (contactDocument.getFormType()) {
				case I9:
					isFormI9 = true;
					break;
				case SUMMARY:
					isSummarySheet = true;
					break;
				case W4:
					isFormW4 = true;
					break;
				case OTHER:
					standardDoc = false;
					break;
				default:
					break;
				}
				if (formBean != null) {
					formBean.setvContact(getvContact());
					formBean.setProduction(production);
					formBean.exitEdit(); // reset any edit-related flags and data
					if (! priorCustomDocument) { // common case
						// let individual form beans do specialized document setup
						formBean.rowClicked(contactDocument);
					}
					else { // switching from one custom doc to another custom doc; force page refresh
						LOG.debug("");
						String targetNav;
						if (myStarts) { // User is in My Starts already
							targetNav = HeaderViewBean.MYFORMS_MENU_DETAILS; // go to My Starts page
						}
						else { // Normally switch to Payroll / Start Forms page
							targetNav = HeaderViewBean.PAYROLL_START_FORMS;
						}
						SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, contactDocument.getId());
						SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, contactDocument.getEmployment().getId());
						SessionUtils.put(Constants.ATTR_CONTACT_ID, contactDocument.getContact().getId());
						// NOTE: the 'navigate' call will result in dispose() being called on this
						// instance!
						HeaderViewBean.navigate(targetNav);
						return; // skip actionCancel, etc.
					}
					if (contactDocument.getFormType() == PayrollFormType.OTHER) {
						priorCustomDocument = true;
						LOG.debug("priorCustomDocument=true");
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method invoked when user clicks the "Send" button. This is similar to
	 * the "Distribute" action -- it takes a Pending document and changes it to Open
	 * status so that the employee make complete and sign it.
	 *
	 * @return null navigation string.
	 */
	public String actionSend() {
		// Send the currently viewed PENDING document to the employee.
		try {
			if (contactDocument != null && contactDocument.getStatus() == ApprovalStatus.PENDING) {
				// - if wtpa, do validation (required fields); fail w/ error message.
				if (contactDocument.getFormType().isWtpa()) {
					if (! refreshContactDoc()) { // doc was deleted by another user
						return null;
					}
					if (contactDocument.getRelatedFormId() != null) {
						if (! formBean.checkSendValid()) {
							// fail w/ error message.
							MsgUtils.addFacesMessage("ContactFormBean.EmployerSignatureInfo", FacesMessage.SEVERITY_ERROR);
							return null;
						}
					}
				}

				// - if model release, do validation (required fields); fail w/ error message.
				if (contactDocument.getFormType().isModelRelease()) {
					if (!refreshContactDoc()) { // doc was deleted by another user
						return null;
					}
					if (contactDocument.getRelatedFormId() != null) {
						if (!formBean.checkSendValid()) {
							// fail w/ error message.
							MsgUtils.addFacesMessage("StatusListBean.SendPendingDocsModelReleaseInfo", FacesMessage.SEVERITY_ERROR);
							return null;
						}
					}
				}
				if (!checkLocked(getvUser(), "Send.")) {
					return null;
				}
				// - issue confirmation dialog"
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_SEND, "ContactFormBean.SendPendingDoc.");
				bean.setMessage(MsgUtils.formatMessage("ContactFormBean.SendPendingDoc.Text",  contactDocument.getContact().getDisplayName()));
			}
			else {
				// - issue error msg if not PENDING status (shouldn't happen, but...)
				MsgUtils.addFacesMessage("ContactFormBean.SendNonPendingDocInfo", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionSendOk() {
		try {
			if (contactDocument != null && contactDocument.getStatus() == ApprovalStatus.PENDING) {
				// change status to OPEN
				if (! refreshContactDoc()) { // doc was deleted by another user
					return null;
				}
				contactDocument.setLockedBy(null);
				contactDocument.setStatus(ApprovalStatus.OPEN);
				if (contactDocument.getFormType() == PayrollFormType.OTHER) {
					//ApprovalPath path = findApprovalPath(contactDocument, aicp, getSelectedProjectId());
					//if (path != null && path.getEmployeeAction() == DocumentAction.RCV) {
					if (contactDocument.getDocument() != null && contactDocument.getDocument().getEmployeeAction() == DocumentAction.RCV) {
						contactDocument.setStatus(ApprovalStatus.APPROVED);
					}
				}
				getContactDocumentDAO().attachDirty(contactDocument);
				List<String> distributedDocumentList = new ArrayList<>();
				distributedDocumentList.add(contactDocument.getDocument().getNormalizedName());
				// send notification to employee
				DoNotification.getInstance().documentDistributed(contactDocument.getContact(), getvContact(), 1, distributedDocumentList);
				clearEmpTable();
				calculateAuthFlags();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for Preparer's signature - same as Submit except for the type
	 * of event created.
	 * 
	 * @return null navigation string.
	 */
	public String actionPrepare() {
		return submit(ACT_PREPARE);
	}

	/**
	 * Action method used to (usually) open the E-signature pop up, in response to a
	 * "Submit", "Sign" or "Sign & Submit" button. If the user's PIN is not set then
	 * the PIN prompt pop up is opened first.
	 * 
	 * @return null navigation string.
	 */
	public String actionSubmit() {
		return submit(ACT_SUBMIT);
	}

	/**
	 * LS-4240 Action method for a second employee signature. This could occur if
	 * only one of the two signatures is required and needs different validation.
	 *
	 * @return null navigation string.
	 */
	public String actionSecondEmployeeSubmit() {
		return submit(ACT_SECOND_EMPLOYEE_SIGN);
	}

	/**
	 * LS-3720
	 *
	 * Action method for Federal W-4 form. If exempt is true then we need to display
	 * a popup
	 * 
	 * @return
	 */
	public String actionW4Submit() {
		// LS-3720 if the data is valid and Exempt has been selected,
		// Display a popup giving the user the option of erasing
		// non-exempt field values or not submitting the form.
		// Only do for Team clients.
		if (FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_EXEMPT_DISABLE)) {
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			Production prod = contactDocument.getProduction();
			FormW4 form = (FormW4) formBean.getForm(); // LS-3943

			if (form != null) {
				if (form.getExempt() && prod.getPayrollPref().getPayrollService().getTeamPayroll()) {
					PopupBean popupBean = PopupBean.getInstance();

					popupBean.setMessage(MsgUtils.getMessage("FormW4Bean.Exempt.Message"));
					popupBean.show(this, ACT_EXEMPT_SELECTED, "FormW4Bean.Exempt.Title", "FormW4Bean.Exempt.ok", "FormW4Bean.Exempt.cancel");
					return null;
				}
			}
			else {
				//. This should not happen but flag it anyways.
				MsgUtils.addGenericErrorMessage();
				return null;
			}
		}
		return submit(ACT_SUBMIT);
	}

	/**
	 * Action method used to open the E-signature pop up, in response to an
	 * 'optional' "Sign" or similar button. Used for ACTRA contract. LS-1411
	 *
	 * @return null navigation string.
	 */
	public String actionSignOptional() {
		return submit(ACT_SIGN_OPT);
	}

	/**
	 * Action method used to (usually) open the E-signature pop up, in response to a
	 * "Submit", "Sign" or "Sign & Submit" button. If the user's PIN is not set then
	 * the PIN prompt pop up is opened first.
	 *
	 * @param action Indicates the particular "type" of submit requested.
	 * @return null navigation string.
	 */
	private String submit(int action) {
		try {
			LOG.debug("");
			prodSelectDisabled = true;
			if (formBean != null) {
				if (editMode) {
					if (formBean.getForm() != null) {
						formBean.getForm().trim(); // trim blanks on all String fields
					}
					if ((action == ACT_SECOND_EMPLOYEE_SIGN && ! formBean.checkSecondSubmitValid()) ||
							(action != ACT_SECOND_EMPLOYEE_SIGN && ! formBean.checkSubmitValid())) {
						prodSelectDisabled = false;
						return null;
					}
					if (!formBean.checkSaveValid()) {
						prodSelectDisabled = false;
						return null;
					}
				}
				else if ((action == ACT_SECOND_EMPLOYEE_SIGN && ! formBean.checkSecondSubmitValid()) ||
						(action != ACT_SECOND_EMPLOYEE_SIGN && ! formBean.checkSubmitValid())) {
					prodSelectDisabled = false;
					return null;
				}
			}
			if (!refreshContactDoc()) { // doc was deleted by another user
				prodSelectDisabled = false;
				return null;
			}
			if (contactDocument.getApproverId() != null && action != ACT_SIGN_OPT) {
				// LS-1663 "optional submit" is allowed after a Submit.
				// log.warn("Attempt to submit a contactDocument that was already submitted!");
				MsgUtils.addFacesMessage("Timecard.Submit.Submitted", FacesMessage.SEVERITY_ERROR);
				// statusChanged(contactDocument);
				prodSelectDisabled = false;
				return null;
			}
			if (contactDocument.getDocumentChain() != null) {
				Set<ApprovalPath> paths = contactDocument.getDocumentChain().getApprovalPath();
				if (paths != null && paths.size() > 0) {
					User currUser = SessionUtils.getCurrentUser(); // Note: don't use vUser, it may be stale!
					if (!checkPin(currUser)) {
						prodSelectDisabled = false;
						return null;
					}
					if (!checkLocked(currUser, "Submit.")) {
						prodSelectDisabled = false;
						return null;
					}
					String name = contactDocument.getContact().getUser().getFirstNameLastName();
					if (currUser.equals(contactDocument.getContact().getUser())) {
						PinPromptBean bean = PinPromptBean.getInstance();
						bean.promptPin(this, action, "ContactFormBean." + "PinSubmitSelf.");
						addFocus("submit");
					}
					else {
						SubmitPromptBean bean = SubmitPromptBean.getInstance();
						bean.show(this, action,
								"ContactFormBean."+"PinSubmitOther.", false);
						bean.setMessage(MsgUtils.formatMessage("ContactFormBean.PinSubmitOther.Text", name));
						bean.setDocumentType("document");
						addFocus("submitOther");
					}
				}
				else {
					MsgUtils.addFacesMessage("ContactFormBean.NoApprovalPath", FacesMessage.SEVERITY_INFO);
					prodSelectDisabled = false;
				}
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.NoApprovalPath", FacesMessage.SEVERITY_INFO);
				prodSelectDisabled = false;
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		currentAction = action;
		String res = null;
		switch (action) {
		case ACT_SUBMIT:
		case ACT_SECOND_EMPLOYEE_SIGN:
		case ACT_PREPARE:
			actionSubmitOk(action);
			break;
		case ACT_EXEMPT_SELECTED:
			// LS-3720
			submit(ACT_SUBMIT);
			break;
		case ACT_DELETE:
			actionDeleteOk();
			break;
		case ACT_DELETE_ATTACHMENT:
			actionDeleteDocsWithAttachmentsOk();
			break;
		case ChangePinBean.ACT_PROMPT_PIN:
			setShowChangePin(false);
			break;
		case ACT_VOID:
			actionVoidOk();
			break;
		case ACT_UPLOAD:
			res = actionUploadReplacementOk();
			break;
		case ACT_LOCK:
			res = actionLockOk();
			break;
		case ACT_AUTO_FILL:
			if (formBean != null) {
				res = autoFillForm(true);
			}
			break;
		case ACT_UPDATE_USER:
			res = actionUpdateUser();
			break;
		case ACT_RECALL:
			res = actionRecallOk();
			break;
		case ACT_RECALL_TO_EMPL:
			res = actionRecallToEmplOk();
			break;
		case ACT_PULL:
			res = actionPullOk();
			break;
		case ACT_EMPLOYER_SIGN:
		case ACT_EMPLOYER_INITIAL:
			res = actionEmployerSignOk();
			break;
		case ACT_SEND:
			res = actionSendOk();
			break;
		case ACT_EMPLOYEE_INITIAL:
		case ACT_EMPLOYEE_INITIAL_DISAGREE:
			res = actionEmployeeInitOk(action);
			break;
		case ACT_EMPLOYEE_SIGN:
		case ACT_SIGN_OPT:
			res = actionEmployeeSignOk(action);
			break;
		case ACT_ACK:
			res = actionAcknowledgeOk();
			break;
		case ACT_ATTACH:
			res = actionUploadAttachmentOk();
			break;
		case ACT_OVERIDE_INITIAL: // LS-1963
			// LS-1963 Approver has decided to continue with the initializing process
			// even though there are no time entries.
			res = actionSubmitEmployerInits(ACT_EMPLOYER_INITIAL, true, "PinInitialSelf.");
			break;
		default:
			res = super.confirmOk(action);
			break;
		}
		return res;
	}

	/**
	 * Method to delete the attachments associated with the given contact document
	 * on Distribution and Review tab.
	 * 
	 * @return null navigation string.
	 */
	private String actionDeleteDocsWithAttachmentsOk() {
		try {
			if (contactDocument != null) {
				LOG.debug("Deleting contact document: " + contactDocument.getId());
				if (contactDocument.getStatus() == ApprovalStatus.PENDING
						|| (contactDocument.getSubmitable())) {
					// Try to delete the Standard document, if any
					if (StatusListBean.deleteRelatedForm(contactDocument) && StatusListBean.deleteAttachments(contactDocument)) {
						// Now delete the contact document
						deleteDocument();
					}
				}
				else { // we did not delete related doc, so unlock CD
					getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method invoked when the user press "Update" button on Update "My
	 * Account" details Pop up. It is used to update the User details with the
	 * details filled in the form, if details are not same.
	 *
	 * @return null navigation string.
	 */
	private String actionUpdateUser() {
		try {
			LOG.debug("");
			if (getUserToUpdate() != null) {
				LOG.debug("");
				boolean show = PopupCheckboxBean.getInstance().getCheck();
				UserPrefBean.getInstance().put(Constants.ATTR_UPDATE_ACCT_PROMPT, show);
				saveAddresses();
				getUserDAO().merge(userToUpdate);
				userToUpdate = null;
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Check attached address objects - if empty, reset our references, else save
	 * the data.
	 */
	private void saveAddresses() {
		try {
			if (userToUpdate != null) {
				Address addr = userToUpdate.getHomeAddress();
				if (addr != null) {
					if (! addr.trimIsEmpty()) {
						addr = getAddressDAO().merge(addr);
						userToUpdate.setHomeAddress(addr);
					}
					else {
						userToUpdate.setHomeAddress(null);
					}
				}

				if (contactDocument.getFormType().equals(PayrollFormType.START)) {
					Address addr2 = userToUpdate.getLoanOutAddress();
					if (addr2 != null) {
						if (! addr2.trimIsEmpty()) {
							addr2 = getAddressDAO().merge(addr2);
							userToUpdate.setLoanOutAddress(addr2);
						}
						else {
							userToUpdate.setLoanOutAddress(null);
						}
					}

					// LS-3578
					Address addr5 = userToUpdate.getLoanOutMailingAddress();
					if (addr5 != null) {
						if (! addr5.trimIsEmpty()) {
							addr5 = getAddressDAO().merge(addr5);
							userToUpdate.setLoanOutMailingAddress(addr5);
						}
						else {
							userToUpdate.setLoanOutMailingAddress(null);
						}
					}

					Address addr3 = userToUpdate.getAgencyAddress();
					if (addr3 != null) {
						if (! addr3.trimIsEmpty()) {
							addr3 = getAddressDAO().merge(addr3);
							userToUpdate.setAgencyAddress(addr3);
						}
						else {
							userToUpdate.setAgencyAddress(null);
						}
					}

					Address addr4 = userToUpdate.getMailingAddress();
					if (addr4 != null) {
						if (! addr4.trimIsEmpty()) {
							addr4 = getAddressDAO().merge(addr4);
							userToUpdate.setMailingAddress(addr4);
						}
						else {
							userToUpdate.setMailingAddress(null);
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Called when the user Cancels one of our pop-up dialogs.
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		currentAction = action;
		String res = null;
		switch(action) {
			case ChangePinBean.ACT_PROMPT_PIN:
				setShowChangePin(false);
				break;
			case ACT_SUBMIT:
				actionSubmitCancel();
				break;
			case ACT_UPDATE_USER:
				actionCancelUpdateUser();
				break;
			case ACT_ATTACH:
				Integer id = PopupUploadBean.getInstance().getAttachmentId();// Get saved document.id value from bean
				Attachment attachment = getAttachmentDAO().findById(id);
				if (attachment != null) {
					getAttachmentDAO().delete(attachment);
				}
				// force page refresh; seems to be only way to clear fileUpload message
				String targetNav;
				if (myStarts) { // User is in My Starts
					targetNav = HeaderViewBean.MYFORMS_MENU_DETAILS; // redisplay My Starts page
				}
				else { // Normal - redisplay Payroll / Start Forms page
					targetNav = HeaderViewBean.PAYROLL_START_FORMS;
				}
				return targetNav;
			default:
				LOG.debug("Cancel");
				if (! editMode && contactDocument != null) {
					getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
				}
		}
		return res;
	}

	/**
	 * Action method used to cancel the submit event for the document,
	 * if user press the "Cancel" button  on Submit Pop up.
	 */
	private void actionSubmitCancel() {
		try {
			prodSelectDisabled = false;
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to set user's choice for the CheckBox, if user clicks Cancel button on
	 * pop up.
	 */
	private void actionCancelUpdateUser() {
		try {
			LOG.debug("");
			boolean show = PopupCheckboxBean.getInstance().getCheck();
			LOG.debug("show:" + show);
			UserPrefBean.getInstance().put(Constants.ATTR_UPDATE_ACCT_PROMPT, show);
			userToUpdate = null;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method used to update User's social security number, if it is null and not
	 * null in Form.
	 */
	private void updateSocialSecurity() {
		Contact ct = getContactDAO().refresh(contactDocument.getContact());
		User user = ct.getUser();
		user = getUserDAO().refresh(user);
		if (contactDocument.getRelatedFormId() != null && StringUtils.isEmpty(user.getSocialSecurity())) {
			if (contactDocument.getFormType() == PayrollFormType.I9) {
				FormI9 formI9 = getFormI9DAO().findById(contactDocument.getRelatedFormId());
				if (!StringUtils.isEmpty(formI9.getSocialSecurity())) {
					LOG.debug("ATTACH USER");
					user.setSocialSecurity(formI9.getSocialSecurity());
					getUserDAO().merge(user);
				}
			}
			else if (contactDocument.getFormType() == PayrollFormType.W4) {
				FormW4 formW4 = getFormW4DAO().findById(contactDocument.getRelatedFormId());
				if (!StringUtils.isEmpty(formW4.getSocialSecurity())) {
					LOG.debug("ATTACH USER");
					user.setSocialSecurity(formW4.getSocialSecurity());
					getUserDAO().merge(user);
				}
			}
			else if (contactDocument.getFormType() == PayrollFormType.START) {
				StartForm startForm = getStartFormDAO().findById(contactDocument.getRelatedFormId());
				if (!StringUtils.isEmpty(startForm.getSocialSecurity())) {
					LOG.debug("ATTACH USER");
					user.setSocialSecurity(startForm.getSocialSecurity());
					getUserDAO().merge(user);
				}
			}
		}
	}

	/**
	 * Action method called (via confirmOk) when an employee has e-signed a
	 * document. This will submit the contact document to the first approver, if it
	 * finds a department approver then the document is submitted to first
	 * department approver in the chain and if not then it is forwarded to first
	 * production approver
	 *
	 * @param action The action code that was used when the prompt was displayed to
	 *               the user.
	 * @return null navigation string.
	 */
	private String actionSubmitOk(int action) {
		try {
			prodSelectDisabled = false;

			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			TimedEventType event = TimedEventType.SUBMIT;
			if (action == ACT_PREPARE) { // "Preparer" signed an I-9.
				event = TimedEventType.PREPARE;
			}

			// Add signature event and update status:
			contactDocument = submitContactDocument(contactDocument, true, event);

			if (!contactDocument.getSubmitable()) {
				if (contactDocument.getFormType().isWtpa()) {
					if (contactDocument.getEmployerSignature() != null) {
						contactDocument.setStatus(ApprovalStatus.APPROVED);
						getContactDocumentDAO().attachDirty(contactDocument);
						getContactDocEventDAO().createEvent(contactDocument, TimedEventType.APPROVE);
						calculateAuthFlags();
					}
				}
				/**
				 * LS-2514 Display info message re incorporation docs when employee signs W9
				 * form.
				 */
				if (contactDocument.getFormType() == PayrollFormType.W9) {
					MsgUtils.addFacesMessage("FormW9Bean.EmployeeSignedReminder", FacesMessage.SEVERITY_INFO);
				}
				updateSocialSecurity();
				clearEmpTable();
			}

			if (formBean != null) {
				// allow form-specific processing during Submit
				formBean.submitted();
			}

			// code to force rebuild of WebView display
			if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
				// code for custom documents
				List<ContactDocEvent> events = contactDocument.getContactDocEvents();
				if (events != null && events.size() > 0) {
					DocumentService.updateXfdf(contactDocument, events.get(events.size() - 1), btnSignIndex);
				}
				formBean.rowClicked(contactDocument);
			}

		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		// see if the user wants to update My Account data from this form
		// checkUpdateAcct();
		return null;
	}

	/**
	 * Method used to set the appropriate approver id after the contact document is
	 * submitted. This also updates the status and creates a signature event, if
	 * appropriate.
	 *
	 * @param contactDoc  The contactDocument being submitted.
	 * @param createEvent True if an event should be created and added to the
	 *                    contactDocument.
	 * @param event       The type of event to be added.
	 */
	public ContactDocument submitContactDocument(ContactDocument contactDoc, boolean createEvent, TimedEventType event) {
		try {
			Map<String, Object> values = new HashMap<>();
			Contact contact = getvContact(); // SessionUtils.getCurrentContact();
			if (contactDoc.getApproverId() == null) {
				if (contactDoc.getDocument() != null && (!contactDoc.getDocument().getApprovalRequired())
						&& contactDoc.getDocument().getEmployeeAction() != null
						&& (contactDoc.getDocument().getEmployeeAction().isDocSubmitable())) {
					LOG.debug("SIGN EVENT, APPROVAL REQUIRED FALSE");
					contactDoc.setApproverId(null);
					// Saving the Approve event instead of Submit event, for aprrovalRequired =
					// false.
					saveSubmitEvent(ApprovalStatus.APPROVED, TimedEventType.SIGN, contactDoc);
				}
				else {
				ApprovalPath path = findApprovalPath(contactDoc, aicp, getSelectedProjectId());
				if (path != null) {
					//if (path.getApprovalRequired()) {
					LOG.debug("SUBMIT EVENT, APPROVAL REQUIRED TRUE" );
					if (contact != null) {
						values.put("departmentId", contactDoc.getEmployment().getDepartment().getId());
						values.put("approvalPathId", path.getId());
						ApprovalPathAnchor anchor = getApprovalPathAnchorDAO().findOneByNamedQuery(
										ApprovalPathAnchor.GET_ANCHOR_BY_DEPARTMENT_AND_APPROVAL_PATH, values);
						if (anchor != null) {
							LOG.debug("anchor id"+ anchor.getId());
							ApproverGroup approverGroup = anchor.getApproverGroup();
							if (approverGroup != null) {
								approverGroup = getApproverGroupDAO().refresh(approverGroup);
								if (! approverGroup.getUsePool() && approverGroup.getApprover() != null) { // Linear hierarchy
									contactDoc.setApproverId(approverGroup.getApprover().getId());
									contactDoc.setApprovalLevel(ApprovalLevel.DEPT);
									contactDoc.setIsDeptPool(false);
								}
								else if (! approverGroup.getGroupApproverPool().isEmpty()) { // Department Pool
									contactDoc.setApproverId((-1)*approverGroup.getId());
									contactDoc.setIsDeptPool(true);
									contactDoc.setApprovalLevel(ApprovalLevel.DEPT);
								}
							}
						}
						if (contactDoc.getApproverId() == null) {
							LOG.debug("");
							if (! path.getUsePool()) { // Linear hierarchy
								// set the first approver
								LOG.debug("");
								if (path.getApprover() != null) {
									LOG.debug("");
									contactDoc.setApproverId(path.getApprover().getId());
									contactDoc.setApprovalLevel(ApprovalLevel.PROD);
								}
							}
							else { // approver pool
								LOG.debug("");
								 if (! path.getApproverPool().isEmpty()) {
									LOG.debug("pool is not empty, setting the negative approver id");
									// pool is empty set the approver to a negative value
									contactDoc.setApproverId((-1)*path.getId());
									contactDoc.setApprovalLevel(ApprovalLevel.PROD);
								}
								else if (path.getFinalApprover() != null) {
									LOG.debug("");
									// if no department approvers and pool is empty set the approver as the final approver from the drop down
									contactDoc.setApproverId(path.getFinalApprover().getId());
									contactDoc.setApprovalLevel(ApprovalLevel.FINAL);
								}
							}
						}
						if (contactDoc.getApproverId() == null) {
							MsgUtils.addFacesMessage("ContactFormBean.EmptyApprovalPath", FacesMessage.SEVERITY_ERROR);
						}
					}
					if (contactDoc.getApproverId() != null && createEvent) {
						ApprovalStatus stat = ApprovalStatus.SUBMITTED;
						if (contactDoc.getStatus() != ApprovalStatus.OPEN) { // not the first time submitted,
							stat = ApprovalStatus.RESUBMITTED; // it was probably rejected or recalled.
						}
						saveSubmitEvent(stat, event, contactDoc);
					}
	//				createContactDocList();
					//}
					/*else {
						LOG.debug("SIGN EVENT, APPROVAL REQUIRED FALSE" );
						contactDoc.setApproverId(null);
						//Saving the Approve event instead of Submit event, for aprrovalRequired = false.
						saveSubmitEvent(ApprovalStatus.APPROVED, TimedEventType.SIGN, contactDoc);
					}*/
				}
				else {
					// No approval path - probably defined for some other projects, but not this one.
					//EventUtils.logError("Approval path is null. ContactDocument=" + document);
					MsgUtils.addFacesMessage("ContactFormBean.NoApprovalPath", FacesMessage.SEVERITY_ERROR);
				}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return contactDoc;
	}

	/**
	 * @param cd The ContactDocument for which the approval path is desired.
	 * @return The ApprovalPath for the given ContactDocument, or null if one was
	 *         not found.
	 */
	public static ApprovalPath findApprovalPath(ContactDocument cd, boolean isAicp, Integer projectId) {
		ApprovalPath path = null;
		try {
			Set<ApprovalPath> approvalPaths = cd.getDocumentChain().getApprovalPath();
			if (approvalPaths != null && approvalPaths.size() > 0) {
				@SuppressWarnings("rawtypes")
				Iterator itr = approvalPaths.iterator();
				if (isAicp) {
					Project project = SessionUtils.getCurrentProject();
					if (project == null) {
						project = ProjectDAO.getInstance().findById(projectId);
					}
					if (project != null) {
						while (itr.hasNext()) {
							ApprovalPath localPath = (ApprovalPath) itr.next();
							if (project.equals(localPath.getProject())) {
								path = localPath;
								break;
							}
						}
					}
					else { // shouldn't happen
						EventUtils.logError("no current project! ContactDocument=" + cd);
					}
				}
				else {
					// for non-commercials set<> will only have one member so return that.
					path = (ApprovalPath) itr.next();
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return path;
	}

	/**
	 * Method to change the contact document's status and to save the submit/approve
	 * event.
	 */
	private void saveSubmitEvent(ApprovalStatus status, TimedEventType type, ContactDocument cd) {
		try {
			LOG.debug("" + type.name() + " Event");
			if (editMode) {
				boolean ret = save();
				LOG.debug("RET = " + ret);
				if (!ret) {
					return;
				}
			}
			cd.setStatus(status);
			getContactDocumentDAO().attachDirty(cd); // TODO this causes the changes in 'userToUpdate' to be persisted!
			ContactDocEvent evt = getContactDocEventDAO().createEvent(cd, type);
			/*if (editMode) {
				boolean ret = save();
				if (! ret) {
					return;
				}
			}*/
			if (cd.getFormType() == PayrollFormType.DEPOSIT) {
				if (formBean.getForm() != null) {
					FormDeposit formDeposit = (FormDeposit) formBean.getForm();
					if (! formDeposit.getStopDeposit()) {
						formDeposit.setSignature1(evt);
					}
					else {
						formDeposit.setSignature2(evt);
					}
					getFormDepositDAO().attachDirty(formDeposit);
				}
			}
			else {
				// see if the user wants to update My Account data from this form
				checkUpdateAcct();
			}
			calculateAuthFlags();
			/*if (cd.getFormType() == PayrollFormType.I9) {
				// "formI9Submitted" notification is not currently used.
				if (cd.getApproverId() > 0) {
					Approver app = ApproverDAO.getInstance().findById(cd.getApproverId());
					List<Contact> contactList = new ArrayList<>();
					contactList.add(app.getContact());
					DoNotification.getInstance().formI9Submitted(cd.getContact(), new Date(), contactList);
				}
				else if (cd.getApproverId() != null) {
					Integer pathId = - (cd.getApproverId());
					ApprovalPath path = ApprovalPathDAO.getInstance().findById(pathId);
					DoNotification.getInstance().formI9Submitted(cd.getContact(), new Date(), path.getApproverPool());
				}
			}*/
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

	}

	/** Utility method used to set select the specified row object. */
	private void selectSummarySheet() {
		try {
			LOG.debug("");
			contactDocument = null;
			RowState state = new RowState();
			state.setSelected(true);
			Integer empId = (Integer) SessionUtils.get(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID);
			if (empId != null) {
				formBean = EmploymentBean.getInstance();
				formBean.setProduction(production);
				Employment employment = getEmploymentDAO().findById(empId);
				if (employment != null) {
					wrapLoop:
					for (EmploymentWrapper ep : empContactList) {
						if (employment.getOccupation() != null && ep.getOccupationName() != null) {
							if (ep.getOccupationName().equals(employment.getOccupation())) {
								for (ContactDocument cd : ep.getContactDocument()) {
									if (cd.isSummarySheet()) {
										contactDocument = cd;
										break wrapLoop;
									}
								}
							}
						}
					}
				}
				if (contactDocument != null) {
//				isJump = true;
					refreshContactDoc();
					stateMap.put(contactDocument, state);
					standardDoc = true;
					isSummarySheet = true;
					isFormI9 = false;
					isFormW4 = false;
					setClickedDocumentName(PayrollFormType.SUMMARY.getLabel());
					isUnavailable = false;
					((EmploymentBean)formBean).setUpSelectedItem(false);
				}
				else {
					LOG.debug("");
					infoMessage = "No Start Forms are available to you at this time.";
				}
			}
			else { // No Employment record available (maybe Admin, or contact with no role)
				LOG.debug("");
				infoMessage = "No Start Forms are available to you at this time.";
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Utility method used to select the first found Payroll Start. */
	private void selectFirstPayrollStart() {
		try {
			RowState state = new RowState();
			state.setSelected(true);
			contactDocument = null; // in case we don't find one.
			wrapLoop:
			for (EmploymentWrapper ep : empContactList) {
				for (ContactDocument cd : ep.getContactDocument()) {
					if (cd.getDocument() != null) {
						if (cd.getDocument().getName().equals(PayrollFormType.START.getLabel())) {
							contactDocument = cd;
							break wrapLoop;
						}
					}
				}
			}
			if (contactDocument != null) {
				stateMap.put(contactDocument, state);
				standardDoc = true;
				isSummarySheet = false;
				isFormI9 = false;
				isFormW4 = false;
				setClickedDocumentName(PayrollFormType.START.getLabel());
				formBean = StartFormBean.getInstance();
				formBean.setProduction(production);
				formBean.rowClicked(contactDocument);
				formBean.setvContact(getvContact());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method invoked when the user press the "Update Master" button on
	 * Payroll->Start Forms tab. It is used to update the master documents XFDF data
	 * with the updated XFDF. [button is restricted to specific users]
	 */
	public void actionUpdateMaster() {
		try {
			if (contactDocument != null) {
				LOG.debug("contact document selected " + contactDocument.getId());
				Document document = contactDocument.getDocument();
				Content content = getContentDAO().findByDocId(document.getId(), document.getOriginalDocumentId());
				if (content != null) {
					XfdfContent xfdfContent = getXfdfContentDAO().findByContactDocId(contactDocument.getId());
					if (xfdfContent != null) {
						LOG.debug("xfdf content id " + xfdfContent.getContactDocId());
						getContentDAO().updateContent(xfdfContent.getContent(), content.getDocId());
						//Add signature box
						if (contactDocument.getFormType() == PayrollFormType.OTHER && xfdfContent.getContent() != null) {
							List<SignatureBox> signatureBoxList = null;
							if (contactDocument != null && contactDocument.getDocument() != null) {
								LOG.debug("document id=" + contactDocument.getDocument().getId()); // SIGNATURE'S DOCUMENT ID
								String xfdf = xfdfContent.getContent();
								signatureBoxList = DocumentService.parseXmlData(xfdf, signatureBoxList);
								List<SignatureBox> oldSignatureBoxList = getSignatureBoxDAO().findByProperty("documentId", contactDocument.getDocument().getId());
								if (oldSignatureBoxList != null && oldSignatureBoxList.size() > 0) {
									for (SignatureBox oldSignBox : oldSignatureBoxList) {
											LOG.debug("Old Signature box : "  + oldSignBox.getX1() +", " +oldSignBox.getY1()+", "+oldSignBox.getX2()+", "+ oldSignBox.getY2());
											getSignatureBoxDAO().delete(oldSignBox);
									}
								}
								if (signatureBoxList != null && signatureBoxList.size() > 0) {
									for (SignatureBox box : signatureBoxList) {
										LOG.debug("New Signature box: "  + box.getX1() +", " +box.getY1()+", "+box.getX2()+", "+ box.getY2());
										box.setDocumentId(contactDocument.getDocument().getId());
										signatureBoxDAO.save(box);
									}
								}
							}
						}
						MsgUtils.addFacesMessage("ContactFormBean.MasterUpdated", FacesMessage.SEVERITY_INFO);
					}
					else {
						MsgUtils.addFacesMessage("ContactFormBean.MasterXfdfMissing", FacesMessage.SEVERITY_ERROR);
					}
				}
				else {
					MsgUtils.addFacesMessage("ContactFormBean.MasterContentMissing", FacesMessage.SEVERITY_ERROR);
				}
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.SelectContactDocument", FacesMessage.SEVERITY_INFO);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Allows a (privileged) user to edit the 'master' XFDF content of a custom
	 * document directly as a text file.
	 */
	public void actionEditMaster() {
		try {
			Content content;
			editMaster = true;
			if (standardDoc) { // if standard form is selected.
				// ** This code is not currently used! **
				// Instead we 'Edit Master' the XFDF of the "<master-form-name.pdf>" file,
				// which is considered by LS to be a "custom document".

//				// find the standard ('master') document uploaded by Admin
//				Document document = DocumentService.getMasterDocument(contactDocument.getFormType());
//				if (document != null) {
//					// find the master XFDF
//					content = ContentDAO.getInstance().findByDocId(document.getId());
//					if (content != null) {
//						if (content.getXfdfData() != null && ! content.getXfdfData().equals(XfdfContent.EMPTY_XFDF)) {
//							setTextEditor(content.getXfdfData());
//						}
//					}
//				}
			}
			else { // edit 'master' XFDF for a custom document
				content = getContentDAO().findByDocId(contactDocument.getDocument().getId(),
						contactDocument.getDocument().getOriginalDocumentId());
				if (content != null) {
					String xfdf = content.getXfdfData();
					if (xfdf != null && !xfdf.equals(XfdfContent.EMPTY_XFDF)) {
						setTextEditor(xfdf);
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Allows a (privileged) user to edit the XFDF content of a specific
	 * (distributed) custom document directly as a text file. This is for
	 * debugging/technical support purposes.
	 */
	public void actionEditPrivateXfdf() {
		try {
			editMaster = true;
			editPrivate = true;
			if (!standardDoc) { // edit XFDF for a custom document
				XfdfContent xfdfCt = getXfdfContentDAO().findByContactDocId(contactDocument.getId());
				if (xfdfCt != null) {
					String xfdf = xfdfCt.getContent();
					if (xfdf != null && !xfdf.equals(XfdfContent.EMPTY_XFDF)) {
						setTextEditor(xfdf);
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for "add/replace attachment" -- use an ImageManager instance to
	 * prompt the user and load the file.
	 *
	 * @return null navigation string
	 */
	public String actionOpenImage() {
		try {
			LOG.debug("<>");
			if (contactDocument != null) {
				if (imageManager == null) {
					imageManager = new ImageManager(this);
				}
				DocumentChain documentChain = contactDocument.getDocumentChain();
				getDocumentChainDAO().refresh(documentChain);
				imageManager.setElementName(documentChain.getName()
						+ " " + documentChain.getName());
				imageManager.setImageAddedMessageId("Image.Uploaded.Timecard");
			}
			return imageManager.actionOpenNewImage("Image.AddPrompt.Timecard", true);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public void updateImage(Image image, String filename) {
		try {
			if (contactDocument != null) {
				contactDocument.setPaperImage(image);
				getContactDocumentDAO().attachDirty(contactDocument);
				imageManager.resetImages();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for uploading a user document.
	 * 
	 * @return null navigation string.
	 */
	public String actionUploadReplacement(ActionEvent evt) {
		try {
			getProduction();
			String occupation = (String) evt.getComponent().getAttributes().get("occupation");
			LOG.debug("Replacement Form for Occupation: " + occupation);
			setOccupationOfReplacement(occupation);
			PopupUploadBean uploadBean = PopupUploadBean.getInstance();
			uploadBean.show(this, ACT_UPLOAD, "ContactFormBean.UploadReplacement.", getPrimaryFolder());
			uploadBean.setIsReplacement(true);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user hits Save on the Upload User Document, a prompt dialog.
	 *
	 * @return null navigation string.
	 */
	public String actionUploadReplacementOk(){
		try {
			PopupUploadBean uploadBean = PopupUploadBean.getInstance();
			String docName = uploadBean.getDisplayFilename();
			Integer id = uploadBean.getDocumentId(); // Get saved document.id value from bean
			LOG.debug("document id in document list bean "+id);
			/*Document document = DocumentDAO.getInstance().findById(
					Integer.parseInt(uploadBean.getSelectedObject().toString()));*/
			Document latestDocument = getDocumentDAO().findById(id);
			latestDocument.setName(docName);
			User user = getSelectedContact().getUser();
			Map<String, Object> values = new HashMap<> ();
			values.put("name", docName);
			values.put("folderId", getPrimaryFolder().getId());
			if (getDocumentDAO().findOneByNamedQuery(Document.GET_REPLACEMENT_DOCUMENT_BY_NAME_FOLDER_ID, values) == null) {
				latestDocument.setDocChainId(Constants.REPLACEMENT_DOCUMENT_CHAIN);
				latestDocument.setRevision(1);
				latestDocument.setOldest(true);
				latestDocument.setStandard(false);
				//document's user will be the User whose documents are being viewed (not the logged-in user)
				latestDocument.setUser(user);
				//if (document != null) {
					//if (document.getStandard()) {
						/*PayrollFormType type = PayrollFormType.toValue(document.getName());
						if (type != null) {
							latestDocument.setType(type.name());
						}*/
					//}
					/*else {
						latestDocument.setType(document.getName());
					}*/
					PayrollFormType type = PayrollFormType.valueOf((String) uploadBean.getSelectedObject());
					if (type != null) {
						latestDocument.setType(type.name());
					}
					getDocumentDAO().attachDirty(latestDocument);
					ContactDocument contactDoc = new ContactDocument();
					contactDoc.setContact(getSelectedContact());
					contactDoc.setDocumentChain(null);
					contactDoc.setDocument(latestDocument);
					Employment emp = null;
					if (getOccupationOfReplacement() != null) {
						Map<String, Object> mapValues = new HashMap<>();
						mapValues.put("contact", getvContact());
						mapValues.put("occupation", getOccupationOfReplacement());
						if (! myStarts && aicp) {
							mapValues.put("project", SessionUtils.getCurrentProject());
							emp = getEmploymentDAO().findOneByNamedQuery(Employment.GET_EMPLOYMENT_BY_OCCUPATION_CONTACT_PROJECT, mapValues);
						}
						else {
							emp = getEmploymentDAO().findOneByNamedQuery(Employment.GET_EMPLOYMENT_BY_OCCUPATION_CONTACT, mapValues);
						}
					}
					if (emp == null) {
						emp = getSelectedContact().getEmployments().get(0);
					}
					contactDoc.setEmployment(emp);
					contactDoc.setProject(emp.getProject());
					contactDoc.setStatus(ApprovalStatus.APPROVED);
					contactDoc.setRelatedFormId(null);
					contactDoc.setFormType(PayrollFormType.OTHER);
					// Add Store event
					ContactDocEvent event = new ContactDocEvent();
					event.setContactDocument(contactDoc);
					event.setType(TimedEventType.STORE);
					event.setDate(new Date());
					getContactDocEventDAO().initEvent(event);
					contactDoc.getContactDocEvents().add(event);
					getContactDocumentDAO().save(contactDoc);
					clearEmpTable();
				//}
				/*else {
					uploadBean.displayError("Please select the type of Document.");
				}*/
			}
			else {
				uploadBean.displayError("Document Name duplicate.");
			}
		}
		catch (Exception e) {
			EventUtils.logError("ContactFormBean upload document failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Used to create Onboarding / Primary folder, if not exists.
	 * @return Primary Folder, if exists otherwise it will create a new Primary folder.
	 */
	private Folder createPrimaryFolder() {
		LOG.debug("");
		Folder primary = null;
		try {
			if (getProduction() != null) {
				Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
				primary = getFolderDAO().findFolderByParentIdAndName(Constants.PRIMARY_FOLDER, onboardFolder.getId());
				if (primary == null) {
					primary = getFolderDAO().createFolder(Constants.PRIMARY_FOLDER, onboardFolder.getId(), false);
				}
			}
			LOG.debug(" primaryFolder: " + primary);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return primary;
	}

	/** Action method for deleting Contact Document
	 * @return null navigation string.
	 */
	public String actionDelete() {
		try {
			if (contactDocument != null && contactDocument.getDocument() != null) {
				LOG.debug("contact document id to delete " + contactDocument.getId());
				if (!refreshContactDoc()) { // doc was deleted by another user
					return null;
				}
				if (contactDocument.getFormType() == PayrollFormType.START && contactDocument.getRelatedFormId() != null) {
					StartForm sf = getStartFormDAO().findById(contactDocument.getRelatedFormId());
					if (sf != null) {
						if (getWeeklyTimecardDAO().existsTimecardsForStartForm(sf.getId())) {
							MsgUtils.addFacesMessage("StartForm.PayrollStartHasTimecards", FacesMessage.SEVERITY_ERROR, "deleted");
							return null;
						}
					}
				}
				if (! checkLocked(getvUser(), "Delete.")) {
					return null;
				}
				PopupBean bean = PopupBean.getInstance();
				if (contactDocument.getAttachments() != null && (! contactDocument.getAttachments().isEmpty())) {
					bean.show(this, ACT_DELETE_ATTACHMENT, "StatusListBean.DeleteAttachment."); // no cancel buttonmessage
					bean.setMessage(MsgUtils.formatMessage("StatusListBean.DeleteAttachment.Text", contactDocument.getAttachments().size()));
					LOG.debug("");
					return null;
				}
				bean.show(this, ACT_DELETE, "ContactFormBean.DeleteContactDocument.");
				bean.setMessage(MsgUtils.formatMessage("ContactFormBean.DeleteContactDocument.Text", contactDocument.getDocument().getNormalizedName()));
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.SelectDocument", FacesMessage.SEVERITY_INFO);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on the "Delete Document" pop-up dialog.
	 * 
	 * @return null navigation string.
	 */
	private String actionDeleteOk() {
		try {
			if (contactDocument != null) {
				if (contactDocument.getStatus() == ApprovalStatus.PENDING
						|| (contactDocument.getSubmitable())) {
					// Try to delete the Standard document, if any
					if (StatusListBean.deleteRelatedForm(contactDocument)) {
						// Now delete the contact document
						deleteDocument();
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("ContactFormBean delete Contact Document failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private void deleteDocument() {
		contactDocument = getContactDocumentDAO().refresh(contactDocument);
		getContactDocumentDAO().delete(contactDocument);
		setClickedDocumentName(null);
		setContactDocument(null);
		clearEmpTable();
		RowState state = new RowState();
		state.setSelected(false);
		stateMap.put(contactDocument, state);
		LOG.debug("<>");
		selectDefaultDocument();
	}

	/** Method to refresh PanelSeries/ Employment wrapper. */
	public void clearEmpTable() {
		LOG.debug("");
		empContactList = null;
		ContactFormRequestBean reqBean = ContactFormRequestBean.getInstance();
		if (reqBean.getEmpTable() != null) {
			try {
				// necessary to force refresh of nested "panelSeries" constructs in IceFaces:
				//reqBean.getEmpTable().getSavedChildren().clear();
				reqBean.getEmpTable().getChildren().clear();
			}
			catch (Exception e) {
				// ignore any errors
			}
		}
	}

	/** Action Edit for editing Standard Documents */
	@Override
	public String actionEdit() {
		String res = null;
		try {
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			if (formBean != null) {
				if (!checkLocked(getvUser(), "")) {
					return null;
				}
				// Make sure formBean has current copy of related form:
				formBean.refreshForm();
				// this will handle most of functionality for specific forms...
				formBean.actionEdit();
				contactDocument.getContactDocEvents();
			}
			res = super.actionEdit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
		return res;
	}

	/** Action Save for Saving data entered by the user in Standard Documents */
	@Override
	public String actionSave() {
		try {
			save();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Save data entered by the user in Standard Documents, or master and private
	 * XFDF data.
	 */
	private boolean save() {
		try {
			LOG.debug("");
			addButtonClicked();
			if (contactDocument != null) {
				if (! editMaster) {
					if (formBean != null) {
						// this should handle most of functionality for standard forms...
						if (formBean.getForm() != null) {
							formBean.getForm().trim(); // trim blanks on all String fields
						}
						if (!(formBean.checkSaveValid())) {
							return false;
						}
						String ret = formBean.actionSave();
						LOG.debug("Ret = " + ret);
						if (ret != null && ret.equals(Constants.ERROR_RETURN)) {
							// Solution for LIE, occurred while fetching empSignature on xhtml file.
							List<ContactDocEvent> evtList = getContactDocEventDAO().findByProperty("contactDocument", contactDocument);
							contactDocument.setContactDocEvents(evtList);
							return false;
						}
						getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
						clearEmpTable();

						if (contactDocument.getFormType() != PayrollFormType.SUMMARY) {
							// LS-1288 Summary Sheet doesn't use relatedFormId
							if (contactDocument.getRelatedFormId() == null) {
								contactDocument.setRelatedFormId(formBean.getForm().getId());
							}
							contactDocument.setLastUpdated(new Date());
							// changed attachDirty to merge to fix intermittent LIE
							contactDocument = contactDocumentDAO.merge(contactDocument);
						}
					}
				}
				else { // if the edit master's save button clicked
					if (editPrivate) {
						Integer id = contactDocument.getId();
						XfdfContent xfdf = getXfdfContentDAO().findByContactDocId(id);
						if (xfdf != null) {
							getXfdfContentDAO().updateXFDFContent(textEditor, id);
							LOG.debug("Updated XFDFContent, contactDocId=" + id + ", len=" + textEditor.length());
						}
					}
					else if (! NO_MASTER.equals(textEditor)) { // if the xfdf string is not 'empty'
						if (standardDoc) { // for standard documents, find master PDF and its XFDF
							// ** This code is not currently used! **
							// Instead we Edit-master & Save the XFDF of the "<master-form-name.pdf>" file,
							// which is considered by LS to be a "custom document".
	//							Document document = DocumentService.getMasterDocument(contactDocument.getFormType());
	//							if (document != null) {
	//								ContentDAO.getInstance().updateContent(textEditor, document.getId());
	//								MsgUtils.addFacesMessage("ContactFormBean.MasterUpdated", FacesMessage.SEVERITY_INFO);
	//							}
						}
						else {
							getContentDAO().updateContent(textEditor, contactDocument.getDocument().getId()); // save the updated xfdf
							MsgUtils.addFacesMessage("ContactFormBean.MasterUpdated", FacesMessage.SEVERITY_INFO);
							//To update the xfdf content of the contact document of admin with the master xfdf,
							//replacing the dummy xfdf created by CustomFormBean.createXfdfFromPdf().
							XfdfContent xfdfContent = getXfdfContentDAO().findByContactDocId(contactDocument.getId());
							if (xfdfContent != null) {
								getXfdfContentDAO().updateXFDFContent(textEditor, contactDocument.getId());
								LOG.debug("Updated XFDFContent, contactDocId=" + contactDocument.getId() + ", len=" + textEditor.length());
							}
						}
						//LOG.debug("updated xfdf "+textEditor);
					}
					else {
						MsgUtils.addFacesMessage("ContactFormBean.MasterXfdfMissing", FacesMessage.SEVERITY_ERROR);
					}
				}
			}
			// LS-2092 keeps in edit mode if date1 or date 2 validation fails on Actra timecard
//			if (! Constants.ERROR_RETURN.equals(ret)) {
				super.actionSave();
//			}
			if (! editMaster) {
				checkUpdateAcct(); // This may leave a prompt up to update User (My Account) info.
			}
			editMaster = false;
			editPrivate = false;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return false;
		}
		return true;
	}

	/**
	 * Method to give warning pop up to update User details and compares the User details with the form details.
	 */
	private void checkUpdateAcct() {
		try {
			setvContact(getContactDAO().findByUserProduction(getvUser(), production));
			if (getvContact().equals(contactDocument.getContact()) &&
					(isFormW4 || contactDocument.getFormType() == PayrollFormType.START || isFormI9) &&
					contactDocument.getSubmitable()) {
				boolean hidePrompt = UserPrefBean.getInstance().getBoolean(Constants.ATTR_UPDATE_ACCT_PROMPT, false);
				if (! hidePrompt) { // If false, pop up appears.
					// LS-3727 Do not display popup for Team clients.
					boolean	isTeamPayroll = false;
					Production prod = getProduction();
					PayrollPreference pref = prod.getPayrollPref();
					pref = getPayrollPreferenceDAO().refresh(pref);
					if(pref != null) {
						PayrollService service = pref.getPayrollService();
						isTeamPayroll = service.getTeamPayroll();
					}
					if(!FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_W9_REMOVE_SUBMIT_VALIDATION) ||
							(FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_W9_REMOVE_SUBMIT_VALIDATION) && !isTeamPayroll)) {
						userToUpdate = null;
						Contact ct = getContactDAO().refresh(contactDocument.getContact());
						userToUpdate = ct.getUser();
						userToUpdate = getUserDAO().refresh(userToUpdate);
						userToUpdate = formBean.compareFormAccountDetails(userToUpdate);
						if (! formBean.getValidData()) {
							PopupCheckboxBean bean = PopupCheckboxBean.getInstance();
							bean.prompt(this, ACT_UPDATE_USER, "ContactFormBean.UpdateMyAccount.", true, "ContactFormBean.UpdateMyAccount.Cancel");
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	public String actionCancel() {
		try {
			editMaster = false;
			// TODO Would refresh here fix duplicate key errors in unlock?
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			if (formBean != null) {
				// this should handle most of functionality for standard forms...
				formBean.actionCancel();
			}
			return super.actionCancel();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method creates the list of EmploymentWrapper object to show employment wise data table records on the payroll/start forms page
	 */
	private void createEmpContactList(Contact contact) {
		try {
			// Since, Getting wrong vContact; why/how is vContact wrong?
			/* When a different contact was selected from the contact list drop down on StartForms tab,
			then in this method, don't why vContact was same as the selected contact due to which
			removeHiddenDocs() was not giving desired results.*/
			LOG.debug("Wrapper for contact");
			empContactList = new ArrayList<>();
			if (getProduction() != null) {
				setvContact(getContactDAO().findByUserProduction(getvUser(), production));
				boolean savedSession = false;
				if (contactDocument == null) {
					// CD=null indicates no current selection, so clear formBean...
					formBean = null; // ...so we don't 'accidentally' call any bean methods.
				}
				List<Employment> employmentList;
				aicp = getProduction().getType().isAicp();
				boolean talent = getProduction().getType().isTalent();
				if (aicp && myStarts) {
					getProjectList();
				}
				if (aicp && ! showAllProjects) {
					Map<String, Object> values = new HashMap<>();
					values.put("contact", contact);
					if (myStarts && selectedProjectId != null) {
						values.put("project", getProjectDAO().findById(selectedProjectId));
					}
					else {
						values.put("project", SessionUtils.getCurrentOrViewedProject());
					}
					employmentList = getEmploymentDAO().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_CONTACT_PROJECT, values);
				}
				else {
					employmentList = getEmploymentDAO().findByProperty("contact", contact);
				}
				AuthorizationBean bean = AuthorizationBean.getInstance();
				boolean hasPermission = myStarts || bean.hasPermission(Permission.MANAGE_START_DOCS) || bean.hasPermission(Permission.APPROVE_START_DOCS)
						|| bean.hasPermission(Permission.VIEW_ALL_DISTRIBUTED_FORMS);
				for (Employment emp : employmentList) {
					if (! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_LS_ADMIN) &&
							! emp.getDepartmentId().equals(Constants.DEPARTMENT_ID_DATA_ADMIN)) {
						List<ContactDocument> contactDocList = new ArrayList<>();
						contactDocList = getContactDocumentDAO().findByProperty("employment", emp);
						if (hasPermission) {
							if (emp.getStartForms().size() > 0) {
								boolean havePayrollStart = false;
								for (ContactDocument cd : contactDocList) {
									if (cd.getDocument().getName().equals(PayrollFormType.START.getLabel())) {
										cd.setFormType(PayrollFormType.START);
										havePayrollStart = true;
										break;
									}
								}
								if (!havePayrollStart && (!talent)) { // create entry to allow view/edit of payroll
																		// start
									StartForm sf = emp.getStartForms().iterator().next();
									// Create and save new CD for this StartForm.
									ContactDocument cd = getStartFormDAO().createContactDocument(sf, production);
									if (cd != null) { // only null if got here with null production or non-onboarding!
										contactDocList.add(cd);
									}
								}
							}
						}
						boolean viewSummary = (! talent) && (! myStarts) && bean.hasPageField(Constants.PGKEY_VIEW_SUMMARY);
						// Note: Summary is never displayed on My Starts page
						if (viewSummary) {
							ContactDocument contactDoc = new ContactDocument();
							contactDoc.setStatus(ApprovalStatus.OPEN);
							contactDoc.setFormType(PayrollFormType.SUMMARY);
							contactDoc.setEmployment(emp);
							contactDoc.setContact(contact);
							contactDocList.add(0, contactDoc);
						}
						if (! contactDocList.isEmpty()) {
							// Note: need to filter non-approver employee seeing own docs, to hide Pending docs,
							// even on My Starts page.
							// Remove contactDocuments for which the current contact is not an approver or owner.
							// Note that someone with APPROVE_START_DOCS still needs to be "filtered", to disable
							// viewing the contents of documents for which they are not an approver.
							removeHiddenDocs(contact, getvContact(), getProduction(), contactDocList, getCurrentContactChainSet(),
									getCurrContactChainEditorMap(), myStarts, showMrStarts);

							for (ContactDocument cd : contactDocList) {
								cd.setViewStatus(ApproverUtils.findRelativeStatus(cd, getvContact()));
								if (cd.getDocument() != null) {
									cd.getDocument().hashCode(); // Fixed one LIE after mini-tab switch, r8769.
									if (cd.getDocumentChain() != null) {
										cd.getDocumentChain().getNormalizedName(); // Fixed second LIE after mini-tab switch; r8824.
									}
								}
								cd.getEmpSignature();  // Fixed an LIE after mini-tab switch, r8880
								cd.getAttachments().size();   // Fixed an LIE after mini-tab switch, r9098
							}
							empContactList.add(new EmploymentWrapper(emp.getOccupation(), contactDocList, emp.getProject()));
							if (! savedSession && ! isJump) {
								SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, emp.getId());
								savedSession = true;
							}
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Remove or 'disable' documents from a list, if those documents should not
	 * be visible to the currently logged-in user. Document names are visible to
	 * any approver; contents of a document are only visible to an approver of
	 * that document. The "owner" of the document can see the names and content
	 * except for Pending ones, which are hidden.
	 * <p>
	 * Note that if the owner is also an approver of a given document, then they
	 * can still see it, even if it is Pending. That is, their status as an
	 * approver will let them see the Pending document even if their "ownership"
	 * rights would not let them see it.
	 *
	 * @param contact The Contact whose list of documents is being viewed; i.e.,
	 *            the "selected" Contact.
	 * @param prod The currently viewed production.
	 * @param contactDocList List of ContactDocuments belonging to the selected
	 *            user. Items will be removed from this list if the currently
	 *            logged-in user is not allowed to see them.
	 * @param currContactChainEditorMap
	 * @param showMrStarts If true, Payroll Starts associated with Model release forms will be displayed. LS-4504
	 * @param approverChainSet The Set of DocumentChain's for which the
	 *            currently logged-in user is an approver, i.e., somewhere in
	 *            the ApprovalPath for these document types.
	 */
	public static void removeHiddenDocs(Contact contact, Contact curContact, Production prod, List<ContactDocument> contactDocList,
			Map<Project, Map<DocumentChain, List<Department>>> chainMap, Map<Project, List<DocumentChain>> currContactChainEditorMap, boolean inMyStarts, boolean showMrStarts) {
		try {
			boolean isCurrent = contact.equals(curContact);
			// Refresh PayrollPreference object to prevent LIE
			PayrollPreference pf = PayrollPreferenceDAO.getInstance().refresh(prod.getPayrollPref());
			boolean hideStarts = pf.getUseModelRelease() && // Hide start forms if "use model release" and FF's are on. LS-4502; LS-4757
					FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS) &&
					FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM);
			hideStarts &= ! showMrStarts; // allow admins to see PS anyway. LS-4504
			// Hide pending documents from employee, and disable docs for which user is not an approver
			Iterator<ContactDocument> itr = contactDocList.iterator();
			while (itr.hasNext()) {
				ContactDocument cd = itr.next();
				if (hideStarts && cd.getFormType() == PayrollFormType.START) { // LS-4502
					itr.remove();
					continue;
				}
				if (! AuthorizationBean.getInstance().hasPermission(curContact, Permission.VIEW_ALL_DISTRIBUTED_FORMS, cd.getProject())) {
					if (cd.getFormType() == PayrollFormType.SUMMARY) {
						continue; // any one can see Summary Sheet
					}
					if (isCurrent && ! cd.getStatus().equals(ApprovalStatus.PENDING)) {
						// employee can see all own docs, except (maybe) Pending
						continue;
					}
					boolean isAnApprover = (! inMyStarts) && ApproverUtils.isContactInChainMap(chainMap, cd, curContact, cd.getProject());
					boolean isAnEditor = (! inMyStarts) && ApproverUtils.isEditorInChainMap(currContactChainEditorMap, cd, curContact, cd.getProject());
					if (! isAnApprover) {
						if (! (isCurrent || isAnEditor)) {
							// Not an approver & not owner of this doc, so hide content if selected
							cd.setDisableJump(true);
						}
						else if (isCurrent && cd.getStatus().equals(ApprovalStatus.PENDING) && (! isAnEditor)) {
							// not approver; but owner of Pending doc - remove from list
							itr.remove();
							continue;
						}
					}
				// Note that an employee can see a Pending doc IF they are an approver of that doc
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to navigate back to the page from where the user jumped to payroll start forms page
	 * to preview the documents
	 * @return null
	 */
	/*public String navigateToPreviousPage() {
		if (isPreviewDocument) {
			HeaderViewBean.navigate(HeaderViewBean.PEOPLE_MENU_ONBOARDING);
		}
		else if (isOnboardingJump){
			HeaderViewBean.getInstance().setMiniTab(1);
			HeaderViewBean.navigate(HeaderViewBean.PEOPLE_MENU_ONBOARDING);
		}
		else if (isJump){
			HeaderViewBean.navigate("contactlist");
		}
		clearSessionVariables();
		return null;
	}*/

	/**
	 * Method used to preview the standard documents on the payroll start when user jump from Onboarding
	 */
	private void previewStandardDocument() {
		try {
			Integer docChainId = (Integer) SessionUtils.get(Constants.ATTR_ONBOARDING_SELECTED_DOCUMENT_CHAIN_ID);
			if (docChainId != null) {
				Document document = getDocumentDAO().findOneByProperty("docChainId", docChainId);
				if (document != null) {
					isPreviewDocument = true;
					//Document document = DocumentDAO.getInstance().findById(docId);
					if (document.getMimeType().equals(MimeType.LS_FORM)) {
						setClickedDocumentName(document.getNormalizedName());
						isSummarySheet = false;
						standardDoc = true;
						@SuppressWarnings("rawtypes")
						Form form = null;
						previewType = PayrollFormType.toValue(document.getName());
						if (previewType != null) {
							formBean = getBeanInstance(previewType);
							form = formBean.getBlankForm();
							switch(previewType) {
								case START:
									form.setId(0);
									break;
								default:
									break;
							}
						}
						formBean.setForm(form);
						formBean.setvContact(getvContact());
						formBean.setProduction(production);
					}
				}
				clearSessionVariables();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to clear the session variables and set the flags to false after return button is clicked.
	 */
	private void clearSessionVariables() {
		SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_DOCUMENT_CHAIN_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, null);
	}

	/**
	 * Clear all our session variables. Used when a person enters a new production, as we
	 * want to be sure no settings from "My Starts" page are left over.
	 */
	public static void clearMyStarts() {
		SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_DOCUMENT_CHAIN_ID, null);
		SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_FORM_I9_ID, null);
		SessionUtils.put(Constants.ATTR_PAYROLL_CONTACT_DOCUMENT_ID, null);
		SessionUtils.put(Constants.ATTR_START_FORM_ID, null);
		SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, null);
	}

	/**
	 * Action method for the "Print" button on the Start Forms mini-tab,
	 * for printing any Start document.
	 *
	 * @return null navigation string
	 */
	public String actionPrint() {
		try {
			if (refreshContactDoc()) { // doc still intact
				if (formBean != null) {
					formBean.actionPrint();
				}
				else {
					actionPrintDocument();
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * LS-3247
	 * Print documents supporting main document.
	 * This is for printing documents like the w-4 worksheet and instructions.
	 * @param docType
	 * @return
	 */
	public String actionPrintSupportingDoc(String docType) {
		return formBean.actionPrintSupportingDoc(docType);
	}

	/**
	 * Action method for the "Sign" button on Signature fields of the Form I9,
	 * for signing the form.
	 *
	 * @return null navigation string
	 */
	public String actionSignature() {
		try {
			if (contactDocument != null) {
				if (contactDocument.getSubmitable()) {
					return actionSubmit();
				}
				else { // shouldn't happen (button should be disabled or not shown)
					MsgUtils.addFacesMessage("ContactFormBean.SignatureFailed", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action method invoked to print the documents
	 * @return null navigation string.
	 */
	public String actionPrintDocument() {
		try {
			DateFormat df = new SimpleDateFormat("mmssSSS");
			String timestamp = df.format(new Date());
			String filePrefix = PayrollFormType.OTHER.getReportPrefix();
			User user = contactDocument.getContact().getUser();
			user = getUserDAO().refresh(user);
			String fileNamePdf = filePrefix + SessionUtils.getCurrentProject().getId() + "_" + user.getId() + "_" + timestamp + ".pdf";
			if (contactDocument != null) {
				LOG.debug("contact document id "+contactDocument.getId());
				XfdfContent xfdf = getXfdfContentDAO().findByContactDocId(contactDocument.getId());
				Content content = getContentDAO().findByDocId(contactDocument.getDocument().getId(),
						contactDocument.getDocument().getOriginalDocumentId());
				PDFDoc in_doc;
				if (content != null) {
					LOG.debug("content id "+content.getDocId());
					in_doc = new PDFDoc(content.getContent());
					in_doc.initSecurityHandler();
					if (xfdf != null) {
						String xfdfString = xfdf.getContent();

						// **************** TEST CODE ************
//						Document standardDocTest = DocumentDAO.getInstance().findOneByProperty("name", "Form-I-9-unlockedClean.pdf");
//						Content contentTest = ContentDAO.getInstance().findByDocId(standardDocTest.getId()); // test content from Mongodb
//						String xfdfTest = contentTest.getXfdfData();
//						xfdfString = xfdfTest;
						// ************** END TEST CODE *************

						if (! XfdfContent.EMPTY_XFDF.equals(xfdfString)) {
							//StandardFormBean.saveXfdf(xfdfString, filePrefix, timestamp); // for debugging XFDF issues
							FDFDoc fdf_doc = FDFDoc.createFromXFDF(xfdfString);
							in_doc.fdfMerge(fdf_doc);
							String outputFile = SessionUtils.getRealReportPath() + fileNamePdf;
							LOG.debug("output file=" + outputFile);
							in_doc.save(outputFile, SDFDoc.e_linearized, null);
							LOG.debug("saved");
							in_doc.close();
							openDocumentInNewTab(fileNamePdf);
						}
					}
				}
			}
		}
		catch(PDFNetException e) {
			EventUtils.logError("ContactFormBean actionPrintDocument failed PDFNetException: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Utility method used to open the specified document in the browser window
	 * @param fileName document to be opened
	 */
	private void openDocumentInNewTab(String fileName) {
		try {
			LOG.debug("fileName "+fileName);
			String javascriptCode = "reportOpen('../../" + Constants.REPORT_FOLDER
					+ "/" + fileName + "','LSreport');";
			addJavascript(javascriptCode);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method creates the drop down list of contacts, whose start forms can be
	 * viewed.
	 */
	private void createContactsDL() {
		try {
			List<SelectItem> list = new ArrayList<>();
			//Production production = SessionUtils.getProduction();
			if (production != null && ! myStarts) { // the contact drop-down is not available in My Starts
				List<Contact> contactList = new ArrayList<>();
				Map<String, Object> values = new HashMap<>();
				values.put("production", production);
				if (showAllProjects || ! aicp) { // TV/Feature ignores project
					contactList = getContactDAO().findByNamedQuery(Contact.GET_CONTACT_LIST_BY_PRODUCTION_NOT_ADMIN, values);
				}
				else {
					values.put("project", SessionUtils.getCurrentOrViewedProject());
					contactList = getContactDAO().findByNamedQuery(Contact.GET_CONTACT_LIST_BY_PRODUCTION_PROJECT, values);
				}
				for (Contact contact : contactList) {
					list.add(new SelectItem(contact.getId(), contact.getUser().getLastNameFirstName()));
				}
				Collections.sort(list, getSelectItemComparator());
				Contact currContact = SessionUtils.getCurrentContact();
				if (! contactList.contains(currContact)) {
					if (! list.isEmpty()) {
						list.add(0, new SelectItem(currContact.getId(), "Select a Contact"));
					}
				}
			}
			setContactsDL(list);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Listener for the Contact list drop down on StartForms tab. Used to display
	 * the contact document list for the selected contact.
	 * 
	 * @param evt
	 */
	public void listenContactChange(ValueChangeEvent evt) {
		try {
			Integer contactId = (Integer) evt.getNewValue();
			LOG.debug("Selected contact's id: " + contactId);
			clearSessionVariables();
			setSelectedTab(0);
			isOnboardingJump = false;
			isJump = false;
			contactDocument = null;
			setInfoMessage(null);
			setClickedDocumentName(null);
			if (contactId != null) {
				selectedContact = getContactDAO().findById(contactId);
				if (selectedContact != null) {
					SessionUtils.put(Constants.ATTR_CONTACT_ID, contactId);
					SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force timecard pages to use attr_contact_id
					if (selectedContact.getEmployments() != null && selectedContact.getEmployments().size() > 0) {
						SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, selectedContact.getEmployments().get(0).getId());
					}
					clearEmpTable();
					setvContact(getContactDAO().findByUserProduction(getvUser(), production));
					/*
					 * createEmpContactList(selectedContact); selectSummarySheet();
					 */
					selectDefaultDocument();
					previewStandardDocument();
					getAttachmentBean().setContactDocument(contactDocument);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to open the contact document when user jumps from
	 * Onboarding/Review & Approval tab
	 */
	private void selectContactDocument() {
		try {
			LOG.debug(" ");
			RowState state = new RowState();
			state.setSelected(true);
			Integer contactDocId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_CONTACTDOC_ID);
			LOG.debug("contactDocument id from session:  "+contactDocId);
			if (contactDocId != null) {
				ContactDocument contactDoc = getContactDocumentDAO().findById(contactDocId);
				if (contactDoc != null) {
					// If user wants to see the contact document of other project.
					if (contactDoc.getProject() != SessionUtils.getCurrentOrViewedProject()) {
						setShowAllProjects(true);
					}
					createEmpContactList(contactDoc.getContact());
					wrapLoop:
					for (EmploymentWrapper ep : empContactList) {
						for (ContactDocument cd : ep.getContactDocument()) {
							if (cd.equals(contactDoc)) {
								contactDocument = contactDoc;
								LOG.debug("contactDocument = " + contactDocument.getId());
								if (cd.getDisableJump()) {
									contactDocument.setDisableJump(true);
									infoMessage = "The selected document is only available to approvers";
									setUnavailable(true);
								}
								break wrapLoop;
							}
						}
					}
				}
			}
			if (contactDocument != null) {
				stateMap.put(contactDocument, state);
				if (contactDocument.getDocument() != null) {
					setClickedDocumentName(contactDocument.getDocument().getNormalizedName());
				}
				formBean = getBeanInstance(contactDocument.getFormType());
				switch (contactDocument.getFormType()) {
				case I9:
					isFormI9 = true;
					break;
				case W4:
					isFormW4 = true;
					break;
				case OTHER:
					setClickedDocumentName(contactDocument.getDocument().getNormalizedName());
					standardDoc = false;
					priorCustomDocument = true;
					break;
				default:
					break;
				}

				if (contactDocument.getDocument() != null &&
						contactDocument.getDocument().getMimeType() != null &&
						contactDocument.getDocument().getMimeType().equals(MimeType.LS_FORM)) {
					standardDoc = true;
					// setUpStandardForm();
				}
				setSelectedContact(contactDocument.getContact());
				getAttachmentBean().setDefaultAttachment(contactDocument, null);
				if (formBean != null) {
					// let individual form beans do specialized document setup
					calculateAuthFlags(); // need to set flags before formBean is called
					formBean.setvContact(getvContact());
					formBean.setProduction(production);
					formBean.rowClicked(contactDocument, this);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Get a reference to a bean instance based on the given PayrollFormType,
	 * e.g., a FormI9Bean instance for type 'I9'.
	 *
	 * @param payrollFormType The PayrollFormType to be displayed or otherwise
	 *            managed.
	 * @return The appropriate bean for the give type of form.
	 */
	@SuppressWarnings("rawtypes")
	private StandardFormBean getBeanInstance(PayrollFormType payrollFormType) {
		StandardFormBean bean = null;
		switch (payrollFormType) {
			case CA_WTPA:
			case NY_WTPA:
				bean = FormWtpaBean.getInstance();
				break;
			case I9:
				bean = FormI9Bean.getInstance();
				break;
			case START:
				bean = StartFormBean.getInstance();
				break;
			case SUMMARY:
				bean = EmploymentBean.getInstance();
				break;
			case W4:
				bean = FormW4Bean.getInstance();
				break;
			case DEPOSIT:
				bean = FormDepositBean.getInstance();
				break;
			case W9:
				bean = FormW9Bean.getInstance();
				break;
			case MTA:
				bean = FormMtaBean.getInstance();
				break;
			case INDEM:
				bean = FormIndemBean.getInstance();
				break;
			case AL_W4:
				bean = FormStateALW4Bean.getInstance();
				break;
			case AR_W4:
				bean = FormStateARW4Bean.getInstance();
				break;
			case AZ_W4:
				bean = FormA4Bean.getInstance();
				break;
			case CA_W4:
				bean = FormStateCAW4Bean.getInstance();
				break;
			case CT_W4:
				bean = FormStateCTW4Bean.getInstance();
				break;
			case DC_W4:
				bean = FormStateDCW4Bean.getInstance();
				break;
			case DE_W4:
				bean = FormStateDEW4Bean.getInstance();
				break;
			case GA_W4:
				bean = FormG4Bean.getInstance();
				break;
			case HI_W4:
				bean = FormStateHIW4Bean.getInstance();
				break;
			case IA_W4:
				bean = FormStateIAW4Bean.getInstance();
				break;
			case ID_W4:
				bean = FormStateIDW4Bean.getInstance();
				break;
			case IL_W4:
				bean = FormILW4Bean.getInstance();
				break;
			case IN_W4:
				bean = FormStateINW4Bean.getInstance();
				break;
			case KS_W4:
				bean = FormStateKSW4Bean.getInstance();
				break;
			case KY_W4:
				bean = FormStateKYW4Bean.getInstance();
				break;
			case LA_W4:
				bean = FormL4Bean.getInstance();
				break;
			case MA_W4:
				bean = FormStateMAW4Bean.getInstance();
				break;
			case MD_W4:
				bean = FormStateMDW4Bean.getInstance();
				break;
			case ME_W4:
				bean = FormStateMEW4Bean.getInstance();
				break;
			case MI_W4:
				bean = FormStateMIW4Bean.getInstance();
				break;
			case MN_W4:
				bean = FormStateMNW4Bean.getInstance();
				break;
			case MO_W4:
				bean = FormStateMOW4Bean.getInstance();
				break;
			case MS_W4:
				bean = FormStateMSW4Bean.getInstance();
				break;
			case MT_W4:
				bean = FormStateMTW4Bean.getInstance();
				break;
			case NC_W4:
				bean = FormStateNCW4Bean.getInstance();
				break;
			case NE_W4:
				bean = FormStateNEW4Bean.getInstance();
				break;
			case NJ_W4:
				bean = FormStateNJW4Bean.getInstance();
				break;
			case OK_W4:
				bean = FormStateOKW4Bean.getInstance();
				break;
			case NY_W4:
				bean = FormStateNYW4Bean.getInstance();
				break;
			case OH_W4:
				bean = FormStateOHW4Bean.getInstance();
				break;
			case OR_W4:
				bean = FormStateORW4Bean.getInstance();
				break;
			case PR_W4:
				bean = FormStatePRW4Bean.getInstance();
				break;
			case RI_W4:
				bean = FormStateRIW4Bean.getInstance();
				break;
			case SC_W4:
				bean = FormStateSCW4Bean.getInstance();
				break;
			case VA_W4:
				bean = FormStateVAW4Bean.getInstance();
				break;
			case VT_W4:
				bean = FormStateVTW4Bean.getInstance();
				break;
			case WI_W4:
				bean = FormStateWIW4Bean.getInstance();
				break;
			case WV_W4:
				bean = FormStateWVW4Bean.getInstance();
				break;
			case MODEL_RELEASE:
				bean = FormModelReleaseBean.getInstance();
				break;
			case ACTRA_CONTRACT:
				bean = FormActraContractBean.getInstance();
				break;
			case ACTRA_PERMIT:
				bean = FormActraWorkPermitBean.getInstance();
				break;
			case ACTRA_INTENT:
				bean = FormActraIntentBean.getInstance();
				break;
			case UDA_INM:
				bean = FormUDAContractBean.getInstance();
				break;
			case OTHER:
				bean = CustomFormBean.getInstance();
				break;
			default:
				// no applicable bean, or maybe state W4
				if (payrollFormType.isFormStateW4Type()) {
					bean = FormStateW4Bean.getInstance();
				}
		}

		if (bean == null) {
			throw new IllegalArgumentException();
		}

		return bean;
	}

	/**
	 * ValueChangeListener for the "show all projects" check-box.
	 *
	 * @param event The event created by the framework.
	 */
	public void listenAllProjects(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				showAllProjects = (boolean) event.getNewValue();
				clearEmpTable();
				createEmpContactList(selectedContact);
				createContactsDL();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for the "show Model Release starts" check-box. LS-4504
	 *
	 * @param event The event created by the framework.
	 */
	public void listenShowMrStarts(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				showMrStarts = (boolean) event.getNewValue();
				clearEmpTable(); // recreate CD list with new flag setting
				createEmpContactList(selectedContact);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Will do a database refresh of {@link #contactDocument}, and notify any
	 * existing formBean of the refresh. If the contactDocument is null after the
	 * refresh, a message is issued to the user and calls are made to set up the
	 * default document display.
	 *
	 * @return True if the contactDocument is refreshed successfully. False
	 *         indicates that the contactDocument value is null. If it was non-null
	 *         on entry, and the refresh returned null, then a message was issued.
	 */
	/* package */ boolean refreshContactDoc() {
		boolean ret = false;
		try {
			LOG.debug("");
			if (contactDocument != null) {
				contactDocument = getContactDocumentDAO().refresh(contactDocument);
				if (formBean != null) {
					formBean.setContactDoc(contactDocument);
				}
				if (contactDocument == null) { // doc was probably deleted by another user
					MsgUtils.addFacesMessage("ContactFormBean.DocumentDeleted", FacesMessage.SEVERITY_ERROR);
					clearEmpTable(); // force refresh of table and screen display
					selectDefaultDocument(); // go back to default view
					LOG.debug("");
				}
				else {
					updateEmpList();
					ret = true;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Update the {@link #empContactList} List with the (possibly) refreshed copy of
	 * {@link #contactDocument}.
	 */
	private void updateEmpList() {
		try {
			Integer id = contactDocument.getId();
			if (id != null && empContactList != null) {
				for (EmploymentWrapper w : empContactList) {
					for (ListIterator<ContactDocument> i = w.getContactDocument().listIterator(); i.hasNext();) {
						ContactDocument cd = i.next();
						if (id.equals(cd.getId())) {
							i.set(contactDocument);
							contactDocument.setViewStatus(cd.getViewStatus());
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to control the visibility of edit button and other action
	 * controls. This method, or ones it calls, set the {@link #editAuth},
	 * {@link #mayApprove}, {@link #isApprover}, {@link #appSign},
	 * {@link #recallAuth} and {@link #pullAuth} flags.
	 */
	public void calculateAuthFlags() {
		try {
			setvContact(getContactDAO().findByUserProduction(getvUser(), production));
			if (! refreshContactDoc()) { // doc was deleted by another user
				return;
			}
			calulateApproverFlag();
			calculateMayApprove();
			calulateEditorFlag();
			editAuth = mayApprove; // generally, if they are the next approver, they can edit.
			LOG.debug("EditAuth:" + editAuth);

			// Using MyStarts check, since employees can't edit the Summary and we are using contact from session(getting wrong vcontact).
			if (contactDocument != null && contactDocument.getFormType() == PayrollFormType.SUMMARY && ! myStarts) {
				LOG.debug("");
				Employment empl = contactDocument.getEmployment();
				Contact contact = SessionUtils.getCurrentContact();
				Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(getProduction());
				DocumentChain chain = getDocumentChainDAO().findOneByNamedQuery(
						DocumentChain.GET_START_DOCUMENT_CHAIN_OF_PRODUCTION, map("folderId", onboardFolder.getId()));
				LOG.debug("");
				if (pseudoApprover) {
					LOG.debug("");
					editAuth = true;
				}
				else if (getProduction().getType().isAicp() && empl.getProject() != null) {
					LOG.debug("");
					ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(null, chain, empl.getProject());
					editAuth = ApproverUtils.isContactInPath(approvalPath, contact, empl.getDepartment());
				}
				else {
					ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(null, chain, null);
					editAuth = ApproverUtils.isContactInPath(approvalPath, contact, empl.getDepartment());
				}
				LOG.debug("editAuth = " + editAuth);
			}
			else if (contactDocument != null && (! contactDocument.getStatus().isSealed()) &&
					(contactDocument.getStatus() != ApprovalStatus.APPROVED) &&
					contactDocument.getFormType() != PayrollFormType.SUMMARY) {

				if (pseudoApprover) {
					LOG.debug("");
					editAuth = true;
				}
				else if (contactDocument.getStatus() == ApprovalStatus.PENDING && isEditor) {
					LOG.debug(" IS EDITOR ");
					editAuth = true;
				}
				else if ((! editAuth) && contactDocument.getId() != null) { // avoids summary sheet
					Contact currentContact = getvContact();
					boolean hideEdit = ((contactDocument.getFormType().isW4Type() &&
							! contactDocument.getFormType().getAllowW4Edit()) ||
										(contactDocument.getFormType() == PayrollFormType.DEPOSIT) ||
										(contactDocument.getFormType() == PayrollFormType.W9) ||
										(contactDocument.getFormType().getAllowW4Edit() &&
												(contactDocument.getStatus() == ApprovalStatus.PENDING ||
												contactDocument.getStatus() == ApprovalStatus.OPEN)) ||
										(contactDocument.getFormType() == PayrollFormType.I9 &&
											(contactDocument.getStatus() == ApprovalStatus.PENDING ||
											contactDocument.getStatus() == ApprovalStatus.OPEN)));
					LOG.debug("hideEdit: " + hideEdit);
					if (contactDocument.getContact() != null &&
							(contactDocument.getContact().equals(currentContact)) &&
							contactDocument.getSubmitable()) {
						// "owner" (employee) of submitable doc can edit it
						editAuth = true;
						//Hide edit button for the Document Actions other than SUBMIT.
						Document document = contactDocument.getDocument();
						if (document.getEmployeeAction() != null && (! document.getEmployeeAction().isDocSubmitable())) {
							LOG.debug("");
							editAuth = false;
						}
					}
					else if (isApprover && ! hideEdit) {
						// Any approver in path for this doc can edit an OPEN or PENDING
						// document, limited by "hide" checks above.
						// (E.g., hide the edit button from approvers of the form W4, Direct Deposit and W9.)
						if (contactDocument.getStatus() == ApprovalStatus.PENDING ||
								contactDocument.getSubmitable()) {
							editAuth = true;
						}
					}
				}
				// To hide the edit button from approvers of the form W4.
				else if (editAuth && ((contactDocument.getFormType().isW4Type() &&
						! contactDocument.getFormType().getAllowW4Edit()) ||
						contactDocument.getFormType() == PayrollFormType.DEPOSIT)) {
					editAuth = false;
				}
				// To hide the edit button for the documents with DocumentAction Acknowledge and status other than Pending.
				if (editAuth && contactDocument.getDocument() != null &&
						contactDocument.getDocument().getEmployeeAction() == DocumentAction.ACK &&
						contactDocument.getStatus() != ApprovalStatus.PENDING) {
					editAuth = false;
				}
				if (editAuth && (! pseudoApprover) &&
						contactDocument.getFormType().isWtpa() &&
						contactDocument.getEmployerSignature() != null &&
						(! contactDocument.getContact().equals(getvContact())) &&
						contactDocument.getEmpSignature() != null) {
					editAuth = false;
				}
				if (contactDocument.getStatus().isSealed()) {
					editAuth = false;
				}
			}
			else {
				editAuth = false;
			}
			LOG.debug("editAuth: " + editAuth);
			calculateRecallPullFlags();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method to set {@link #isApprover} boolean.
	 * True, if current user is an approver for the document.
	 */
	private void calulateApproverFlag() {
		try {
			if (contactDocument != null) {
				if (pseudoApprover) {
					LOG.debug("");
					isApprover = true;
				}
				else {
					LOG.debug("");
					ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(contactDocument, null, null);
					isApprover = ApproverUtils.isContactInPath(approvalPath, getvContact(), contactDocument.getDepartment());
				}
				LOG.debug("isApprover: " + isApprover);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method to set {@link #mayApprove} and {@link #appSign} booleans
	 * according to user's permissions and document conditions, to render Sign,
	 * Approve and Reject buttons.
	 */
	private void calculateMayApprove() {
		try {
			LOG.debug("");
			mayApprove = false;
			appSign = false;
			if (contactDocument != null && contactDocument.getId() != null) {
				Contact currentContact = getvContact(); // SessionUtils.getCurrentContact();
				AuthorizationBean authBean = AuthorizationBean.getInstance();
	//			refreshContactDoc(); (not needed - done in caller)
				if ((contactDocument.getStatus().isPendingApproval() && ! contactDocument.getSubmitable()) ||
						(contactDocument.getFormType().isWtpa() && ! contactDocument.getStatus().isFinal())) {
					if (authBean.hasPermission(Permission.APPROVE_START_DOCS)) {
						// For Pool case; user may be anyone in pool to get approval authority
						// Linear approval path; user must be next approver
						mayApprove = ApproverUtils.isNextApprover(contactDocument, currentContact.getId());
						LOG.debug("mayApprove: " + mayApprove);
						appSign = mayApprove;
						if (contactDocument.getFormType().isWtpa()) {
							if (! appSign && contactDocument.getEmployerSignature() == null) {
								// Allow any WTPA approver access to signature
								//ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(contactDocument);
								//appSign = ApproverUtils.isContactInPath(approvalPath, currentContact);
								appSign = isApprover;
							}
							if (appSign && contactDocument.getEmployerSignature() != null) {
								appSign = false;
							}
						}
					}
				}
			}
			LOG.debug("appSign: " + appSign);
			LOG.debug("mayApprove: " + mayApprove);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method to set {@link #isEditor} boolean.
	 * True, if current user is an editor for the document.
	 */
	private void calulateEditorFlag() {
		try {
			if (contactDocument != null && contactDocument.getFormType() != PayrollFormType.SUMMARY &&
					 contactDocument.getStatus() == ApprovalStatus.PENDING) {
				LOG.debug("");
				ApprovalPath approvalPath = ContactDocumentDAO.getCurrentApprovalPath(contactDocument, null, null);
				if (approvalPath != null && approvalPath.getEditors() != null && (! approvalPath.getEditors().isEmpty())) {
					if (approvalPath.getEditors().contains(getvContact())) {
						isEditor = true;
					}
				}
				LOG.debug("isEditor: " + isEditor);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Utility method
	 * @return ContactDAO instance
	 */
	public ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	/** Utility method
	 * @return ContactDocumentDAO instance
	 */
	public ContactDocumentDAO getContactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}

	/** Utility method
	 * @return DocumentDAO instance
	 */
	public DocumentDAO getDocumentDAO() {
		if (documentDAO == null) {
			documentDAO = DocumentDAO.getInstance();
		}
		return documentDAO;
	}

	/** Utility method
	 * @return EmploymentDAO instance
	 */
	public EmploymentDAO getEmploymentDAO() {
		if (employmentDAO == null) {
			employmentDAO = EmploymentDAO.getInstance();
		}
		return employmentDAO;
	}

	/** Utility method
	 * @return PayrollPreferenceDAO instance
	 */
	public PayrollPreferenceDAO getPayrollPreferenceDAO() {
		if (payrollPreferenceDAO == null) {
			payrollPreferenceDAO = PayrollPreferenceDAO.getInstance();
		}
		return payrollPreferenceDAO;
	}

	/** Utility method
	 * @return UserDAO instance
	 */
	public UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

	/** Utility method
	 * @return AttachmentDAO instance
	 */
	public AttachmentDAO getAttachmentDAO() {
		if (attachmentDAO == null) {
			attachmentDAO = AttachmentDAO.getInstance();
		}
		return attachmentDAO;
	}

	/** Utility method
	 * @return ContentDAO instance
	 */
	public ContentDAO getContentDAO() {
		if (contentDAO == null) {
			contentDAO = ContentDAO.getInstance();
		}
		return contentDAO;
	}

	/** Utility method
	 * @return Form I9 DAO instance
	 */
	public FormI9DAO getFormI9DAO() {
		if (formI9DAO == null) {
			formI9DAO = FormI9DAO.getInstance();
		}
		return formI9DAO;
	}

	/** Utility method
	 * @return FormW4DAO instance
	 */
	public FormW4DAO getFormW4DAO() {
		if (formW4DAO == null) {
			formW4DAO = FormW4DAO.getInstance();
		}
		return formW4DAO;
	}

	/** Utility method
	 * @return AddressDAO instance
	 */
	public AddressDAO getAddressDAO() {
		if (addressDAO == null) {
			addressDAO = AddressDAO.getInstance();
		}
		return addressDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return XfdfContentDAO instance
	 */
	public XfdfContentDAO getXfdfContentDAO() {
		if (xfdfContentDAO == null) {
			xfdfContentDAO = XfdfContentDAO.getInstance();
		}
		return xfdfContentDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ProjectDAO instance
	 */
	public ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ContactDocEventDAO instance
	 */
	public ContactDocEventDAO getContactDocEventDAO() {
		if (contactDocEventDAO == null) {
			contactDocEventDAO = ContactDocEventDAO.getInstance();
		}
		return contactDocEventDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ProductionDAO instance
	 */
	public ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return StartFormDAO instance
	 */
	public StartFormDAO getStartFormDAO() {
		if (startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ApproverDAO instance
	 */
	public ApproverDAO getApproverDAO() {
		if (approverDAO == null) {
			approverDAO = ApproverDAO.getInstance();
		}
		return approverDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return DocumentChainDAO instance
	 */
	public DocumentChainDAO getDocumentChainDAO() {
		if (documentChainDAO == null) {
			documentChainDAO = DocumentChainDAO.getInstance();
		}
		return documentChainDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return WeeklyTimecardDAO instance
	 */
	public WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return SignatureBoxDAO instance
	 */
	public SignatureBoxDAO getSignatureBoxDAO() {
		if (signatureBoxDAO == null) {
			signatureBoxDAO = SignatureBoxDAO.getInstance();
		}
		return signatureBoxDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return FormDepositDAO instance
	 */
	public FormDepositDAO getFormDepositDAO() {
		if (formDepositDAO == null) {
			formDepositDAO = FormDepositDAO.getInstance();
		}
		return formDepositDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ApprovalPathAnchorDAO instance
	 */
	public ApprovalPathAnchorDAO getApprovalPathAnchorDAO() {
		if (approvalPathAnchorDAO == null) {
			approvalPathAnchorDAO = ApprovalPathAnchorDAO.getInstance();
		}
		return approvalPathAnchorDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ApprovalPathDAO instance
	 */
	public ApprovalPathDAO getApprovalPathDAO() {
		if (approvalPathDAO == null) {
			approvalPathDAO = ApprovalPathDAO.getInstance();
		}
		return approvalPathDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return ApproverGroupDAO instance
	 */
	public ApproverGroupDAO getApproverGroupDAO() {
		if (approverGroupDAO == null) {
			approverGroupDAO = ApproverGroupDAO.getInstance();
		}
		return approverGroupDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return FolderDAO instance
	 */
	public FolderDAO getFolderDAO() {
		if (folderDAO == null) {
			folderDAO = FolderDAO.getInstance();
		}
		return folderDAO;
	}

	/**
	 * Utility method
	 * 
	 * @return AttachmentBean instance
	 */
	public AttachmentBean getAttachmentBean() {
		if (attachmentbean == null) {
			attachmentbean = AttachmentBean.getInstance();
		}
		return attachmentbean;
	}

	/**
	 * Action method called by standard "Approve" button -- NOT embedded document
	 * button
	 *
	 * @return null navigation string
	 */
	public String actionApprove() {
		try {
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			btnSignIndex = 9; // Assume value to prevent on-document signature
			LOG.debug("");
			if (contactDocument != null && formBean != null) {
				boolean valid = formBean.checkApproveValid();
				if (!valid) {
					return null;
				}
			}
			if (contactDocument != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
				btnSignIndex = 2; // Assume second approver if below cases don't match.
				if (contactDocument.getSubmitable()) { // unusual case!
					btnSignIndex = 1; // allow signature to fill first on-document slot
				}
				else {
					// if last event was submit, assume this is on-doc signature; set index = 1;
					List<ContactDocEvent> events = contactDocument.getContactDocEvents();
					if (events != null && events.size() > 0 &&
							((events.get(events.size()-1).getType() == TimedEventType.SUBMIT) ||
							(events.get(events.size()-1).getType() == TimedEventType.RECALL))) {
						btnSignIndex = 1; // allow signature to fill first on-document slot
					}
				}
				// TODO calculate real value of btnSignIndex
				/**
				 * Setting index to 2 prevents over-writing an existing on-document
				 * signature for our existing 'typical' custom documents that only have
				 * a single on-document approval. We need to figure out the 'real' value
				 * to be able to handle custom docs with multiple on-document approvals.
				 * We can do this more easily once we store the number of on-document
				 * approvals at the time of document upload.
				 */
			}
			return approve();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Process an 'approve' request, from either an embedded document button, or
	 * our standard "Approve" window button.  This will normally result in the display
	 * of the e-sign prompt dialog.  This handles both built-in and custom documents.
	 *
	 * @return null navigation string
	 */
	private String approve() {
		return super.actionApprove(contactDocument);
	}

	/**
	 * Called when a valid e-signature has been received for an Approve request.
	 * This handles both built-in and custom document approvals.
	 *
	 * @return null navigation string
	 */
	@Override
	protected String actionApproveOk() {
		try {
			updateEmpList(); // Fix 'duplicate instance' error. 3.1.7878
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			super.actionApproveOk(); // this updates the document status & records the event
			refreshContactDoc();
			//code for custom documents, add signature value to the XFDF
			if (contactDocument != null && formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
				List<ContactDocEvent> events = contactDocument.getContactDocEvents();
				LOG.debug("") ;
				if (events != null && events.size() > 0 && btnSignIndex > 0) {
//					int signIndex = events.size();
//					int lastSubmitEvt = 0;
//					for (int i = 0; i < events.size(); i++) {
//						ContactDocEvent evt = events.get(i);
//						if (evt.getType() == TimedEventType.SIGN || evt.getType() == TimedEventType.SUBMIT) {
//							lastSubmitEvt = i;
//						}
//						if (i == (signIndex - 1)) {
//							if (evt.getType() == TimedEventType.SIGN || evt.getType() == TimedEventType.SUBMIT) {
//								signIndex = i;
//							}
//							else {
//								signIndex = i - lastSubmitEvt;
//							}
//						}
//					}
					LOG.debug(" ");
					DocumentService.updateXfdf(contactDocument, events.get(events.size()-1), btnSignIndex);
				}
				// To hide buttons after approve/reject
				calculateAuthFlags();
				formBean.rowClicked(contactDocument);
			}
			else {
				// To hide buttons after approve/reject
				calculateAuthFlags();
			}
			// To change the status of document.
			clearEmpTable();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionReject() {
		try {
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			return super.actionReject(contactDocument);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	protected String actionRejectOk() {
		try {
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			super.actionRejectOk();
			if (refreshContactDoc()) {
				if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
					((CustomFormBean) formBean).reject(contactDocument);
				}
				// To hide buttons after approve/reject
				calculateAuthFlags();
				// To change the status of document.
				clearEmpTable();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method used to find all the document chains for which the current contact is
	 * an approver (or an approver in their approval paths).
	 */
	private void findDocumentChainsForCurrentContact() {
		try {
			LOG.debug("");
			setvContact(getContactDAO().findByUserProduction(getvUser(), production));
			currentContactChainMap = StatusListBean.findDocumentChainsForCurrentContact(getvContact(), production);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method used to find all the document chains for which the current contact is
	 * an editor in their approval paths.
	 */
	private void findDocumentChainEditorForCurrentContact() {
		try {
			LOG.debug("");
			setvContact(getContactDAO().findByUserProduction(getvUser(), production));
			currContactChainEditorMap = StatusListBean.findDocumentChainEditorForCurrentContact(getvContact(), production);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for the "Void" button.
	 * 
	 * @return null navigation string
	 */
	public String actionVoid() {
		try {
			if (contactDocument != null) {
				if (contactDocument.getStatus().equals(ApprovalStatus.OPEN) ||
						contactDocument.getStatus().equals(ApprovalStatus.VOID)) {
					MsgUtils.addFacesMessage("ContactFormBean.Void.StatusOpen", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				// Don't allow void if the StartForm has associated timecards.
				else if (contactDocument.getFormType() == PayrollFormType.START && contactDocument.getRelatedFormId() != null) {
					StartForm sf = getStartFormDAO().findById(contactDocument.getRelatedFormId());
					if (sf != null) {
						if (getWeeklyTimecardDAO().existsTimecardsForStartForm(sf.getId())) {
							MsgUtils.addFacesMessage("StartForm.PayrollStartHasTimecards", FacesMessage.SEVERITY_ERROR, "voided");
							return null;
						}
					}
				}
				contactDocument = getContactDocumentDAO().refresh(contactDocument);
				if (!checkLocked(getvUser(), "Void.")) {
					return null;
				}
				String message = MsgUtils.formatMessage("ContactFormBean.VoidContactDocument.Text",
						contactDocument.getContact().getDisplayName());

				CommentPinPromptBean bean = CommentPinPromptBean.getInstance();
				bean.show(this, ACT_VOID, "ContactFormBean.VoidContactDocument.");
				bean.setDocumentType("document");
				bean.setMessage(message);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionVoidOk() {
		try {
			CommentPinPromptBean bean = CommentPinPromptBean.getInstance();
			// note: voidStatus() will unlock CD
			contactDocument = getContactDocumentDAO().voidStatus(contactDocument, bean.getComment());
			if (refreshContactDoc()) {
				calculateAuthFlags();
				// To update the displayed status of document:
				clearEmpTable();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Recall" button. This takes a document away from its
	 * current approver, and gives it back to either a prior approver, or to the
	 * owning employee.
	 *
	 * @return null navigation string
	 */
	public String actionRecall() {
		try {
			if (contactDocument != null) {
				calculateAuthFlags(); // re-calc flags -- doc status may have been changed by another user.
				if (recallAuth) {
					if (!checkLocked(getvUser(), "Recall.")) {
						return null;
					}
					// past approver, or employee/owner, may have this authority.
					// (note that current user could not be the next approver)
					boolean userOwnsDocument = false;
					User cdUser = null;
					if (contactDocument.getContact() != null) {
						cdUser = contactDocument.getContact().getUser();
						cdUser = getUserDAO().refresh(cdUser);
						userOwnsDocument = cdUser.getAccountNumber().equals(
								getvUser().getAccountNumber());
					}
					boolean didApprove = findDidApprove(contactDocument, getvUser());
					if (userOwnsDocument || didApprove) {
						// This is a "recall" action
						PopupBean bean = PopupBean.getInstance();
						String msgPrefix = "ContactFormBean.";
						String param = null;
						int action;
						if (didApprove) {
							// earlier approver recalling doc from later approver or final approved state
							msgPrefix += "Recall.";
							action = ACT_RECALL;
							if (cdUser != null)
								param = cdUser.getFirstNameLastName();
						}
						else { // employee recalling doc from approver
							msgPrefix += "RecallToEmployee.";
							action = ACT_RECALL_TO_EMPL;
						}
						bean.show(this, action, msgPrefix);
						bean.setMessage(MsgUtils.formatMessage(msgPrefix + "Text", param));
						return null;
					}
				}
				else {
					MsgUtils.addFacesMessage("ContactFormBean.RecallNotAllowed", FacesMessage.SEVERITY_ERROR);
					clearEmpTable();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public static boolean findDidApprove(ContactDocument cd, User user) {
		try {
			String acct = user.getAccountNumber();
			for (ContactDocEvent evt : cd.getContactDocEvents()) {
				if (evt.getType() == TimedEventType.APPROVE && evt.getUserAccount().equals(acct)) {
					return true;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Action method for the "Pull" button. This takes a document away from its
	 * current approver, and gives it to an approver "higher" (later) in the
	 * hierarchy.
	 *
	 * @return null navigation string
	 */
	public String actionPull() {
		try {
			if (contactDocument != null) {
				Integer appContactId = null;
				Integer appId = contactDocument.getApproverId();
				boolean valid = false;
				// Contact appContact = null;
				if (appId != null) {
					if (appId < 0) {
						// ApproverGroup Changes
						Integer pathOrGroupId = -(appId);
						if (pathOrGroupId != null) {
							if (contactDocument.getIsDeptPool()) { // Department Pool, Approver Group Id.
								ApproverGroup group = getApproverGroupDAO().findById(pathOrGroupId);
								if (group.getGroupApproverPool() != null && (! group.getGroupApproverPool().isEmpty()) &&
										(! group.getGroupApproverPool().contains(getvContact()))) {
									// current user is not the next approver
									valid = true;
								}
							}
							else { // Production Pool, Approval Path Id
								ApprovalPath path = getApprovalPathDAO().findById(pathOrGroupId);
								if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
									if (path.getFinalApprover().getContact().equals(getvContact())) {
										valid = true;
									}
								}
							}
						}
					}
					else {
						Approver app = getApproverDAO().findById(appId);
						if (app != null && app.getContact() != null) {
							appContactId = app.getContact().getId();
							if (appContactId != null && !appContactId.equals(getvContact().getId())) {
								// appContact = ContactDAO.getInstance().findById(appContactId);
								// current user is not the next approver
								valid = true;
							}
						}
					}
					if (valid) {
						if (!checkLocked(getvUser(), "Pull.")) {
							return null;
						}
						// This is a "pull" action -- later approver taking Document from earlier
						// approver
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, ACT_PULL, "ContactFormBean." + "Pull.");
						return null;
					}
				}
				else {
					LOG.error("logic error - attempt to 'Pull' an Approved or Open doc, cd=" + contactDocument.getId());
					MsgUtils.addGenericErrorMessage();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The method used to create the Document drop down list for refreshing the
	 * Start Forms View
	 * 
	 * @return {@link #documentList}
	 */
	private List<SelectItem> createDocumentList() {
		LOG.debug("");
		List<Document> list = new ArrayList<>();
		try {
			if (getProduction() != null) {
				list = getDocumentDAO().findAllOnboardingDocuments(production);
				if (list != null) {
					documentList = new ArrayList<>();
					documentList.add(0, new SelectItem(0, "All"));
					for (Document doc : list) {
						documentList.add(new SelectItem(doc.getId(), doc.getName()));
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return documentList;
	}

	/**
	 * ValueChangeListener method for the Production drop-down list. When the user
	 * chooses a different production, we have to re-populate the list of start
	 * forms. This is only used on the My Starts page.
	 *
	 * @param evt The change event generated by the framework.
	 */
	public void listenProduction(ValueChangeEvent evt) {
		try {
			Integer id = (Integer) evt.getNewValue();
			setSelectedProdId(id);
			contactDocument = null;
			infoMessage = null;
			documentList = null;
			primaryFolder = null;
			clearEmpTable();
			if (id != null && id > 0) {
				getProduction();
				if (production != null && (aicp = production.getType().isAicp())) {
					SessionUtils.put(Constants.ATTR_LAST_PROJECT_ID, null);
					// Build the project list here to make sure that the correct onboarding forms
					// are displayed when the production company is selected. LS-1181
					projectList = createProjectList(getvContact());
				}
				getPrimaryFolder();
				createEmpContactList(getvContact());
				selectDefaultDocument();
				SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, production.getId());
				if (production != null) {
					allowI9attachment = !(production.getPayrollPref().getI9Attachment() == ExistencePolicy.FORBID); // fix
																													// LIE.
																													// LS-2251
				}
			}
			else {
				infoMessage = "Please select a Production to view Start Forms.";
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener method for the Project drop-down list. When the user
	 * chooses a different project, we have to re-populate the list of startforms
	 *
	 * @param evt The change event generated by the framework.
	 */
	public void listenProject(ValueChangeEvent evt) {
		try {
			Integer id = (Integer) evt.getNewValue();
			setSelectedProjectId(id);
			// projectList = null; // shouldn't need to recreate project list
			infoMessage = null;
			documentList = null;
			primaryFolder = null;
			contactDocument = null;
			clearEmpTable();
			if (id != null && id != 0) {
				getPrimaryFolder();
				selectDefaultDocument();
				getEmpContactList(); // probably created already, but just in case.
				// createEmpContactList(selectedContact);
			}
			if (getSelectedProdId() == null || getSelectedProdId() == 0) {
				infoMessage = "Please select a Production to view Start Forms.";
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for the "Lock" button.
	 * 
	 * @return null navigation string
	 */
	public String actionLock() {
		try {
			if (contactDocument != null && (contactDocument.getStatus() == ApprovalStatus.APPROVED)) {
				contactDocument = getContactDocumentDAO().refresh(contactDocument);
				if (!checkLocked(getvUser(), "Lock.")) {
					return null;
				}
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_LOCK, "ContactFormBean.LockDocument.");
				// test code to force rebuild of WebView display
//				if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
//					formBean.rowClicked(contactDocument);
//				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionLockOk() {
		try {
			if (contactDocument != null && contactDocument.getStatus() == ApprovalStatus.APPROVED) {
				contactDocument.setStatus(ApprovalStatus.LOCKED);
				contactDocument.setLockedBy(null);
				ContactDocEvent event = new ContactDocEvent();
				event.setContactDocument(contactDocument);
				event.setType(TimedEventType.LOCK);
				event.setDate(new Date());
				getContactDocEventDAO().initEvent(event);
				getContactDocumentDAO().attachDirty(contactDocument);
				contactDocument.getContactDocEvents().add(event);
			}
			else {
				getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			}
			refreshContactDoc();
			calculateAuthFlags();
			// To change the status of document.
			clearEmpTable();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Auto-fill" button. This checks the user's preference,
	 * and will either prompt them to OK the auto-fill, or just do it immediately.
	 * In the case of a prompt, we'll be called back via either the
	 * {@link #confirmOk(int)} or {@link #confirmCancel(int)} methods.
	 *
	 * @return null navigation string
	 */
	public String actionAutoFillForm() {
		try {
			LOG.debug("");
			if (contactDocument != null && contactDocument.getFormType().isAutoFilled()) {
				String msgId = "ContactFormBean.AutoFillForm.";
				if ((contactDocument.getFormType().isWtpa())) {
					msgId = "ContactFormBean.AutoFillWtpaForm.";
				}
				if (!editMode) { // if not in edit mode, we need to refresh & lock the form first
					if (!refreshContactDoc()) {
						return null; // doc was deleted by another user
					}
					if (!checkLocked(getvUser(), "AutoFill.")) {
						return null; // doc locked by another user
					}
				}

				// LS-3279 Autofill for Model Release Form is only available for the approver
				// before they have signed the form. There is not a need for a prompt.
				if (contactDocument.getFormType() == PayrollFormType.MODEL_RELEASE) {
					return autoFillForm(false);
				}

				boolean hidePrompt = UserPrefBean.getInstance().getBoolean(Constants.ATTR_AUTO_FILL_PROMPT, false);
				if (!hidePrompt) { // If hidePrompt is false, pop up appears.
					LOG.debug("");
					PopupCheckboxBean bean = PopupCheckboxBean.getInstance();
					bean.prompt(this, ACT_AUTO_FILL, msgId, true, null);
				}
				else {
					LOG.debug("");
					autoFillForm(false);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Auto-Fill our current form, and save it if we are in View mode.
	 *
	 * @param prompted True if the user was prompted for the auto-fill action.
	 * @return null navigation String
	 */
	private String autoFillForm(boolean prompted) {
		String res = null;
		try {
			LOG.debug("");
			refreshContactDoc();
			if (contactDocument.getFormType().isWtpa()) {
				 formBean.populateForm(prompted);
			}
			else {
				res = formBean.autoFillForm(prompted);
			}
			if (!editMode) {
				res = formBean.actionSave();
				getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return res;
	}

	/**
	 * Method to create project list for a contact.
	 * 
	 * @param contact
	 * @return list of projects.
	 */
	private List<SelectItem> createProjectList(Contact contact) {
		projectList = new ArrayList<>();
		try {
			if (contact != null) {
				List<Integer> projIdList = new ArrayList<>();
				List<Employment> emplList = contact.getEmployments();
				for (Employment emp : emplList) {
					Project project = emp.getProject();
					if (project != null && (!projIdList.contains(project.getId()))) {
						projIdList.add(project.getId());
						projectList.add(new SelectItem(project.getId(), project.getTitle()));
					}
				}
				if (projIdList.size() > 0) {
					Collections.sort(projectList, getSelectItemComparator());
					if (SessionUtils.get(Constants.ATTR_LAST_PROJECT_ID) != null) {
						setSelectedProjectId(SessionUtils.getInteger(Constants.ATTR_LAST_PROJECT_ID));
					}
					else {
						setSelectedProjectId(projIdList.get(0));
						SessionUtils.put(Constants.ATTR_LAST_PROJECT_ID, getSelectedProjectId());
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return projectList;
	}

	/**
	 * Determine the current user authority to perform RECALL or PULL on the current
	 * document. Their authority is based on several factors, including whether they
	 * are in the approval chain for the supplied document, if they are the owner of
	 * the document, whether or not they have already approved the document, and the
	 * document's status.
	 * <p>
	 * The method sets two boolean fields {@link #recallAuth} and {@link #pullAuth}
	 * accordingly. These fields are used both in the Java code, and in the JSP
	 * pages to control the display of data and controls.
	 * <p>
	 * {@link #editAuth} should have been calculated prior to calling this method.
	 */
	protected void calculateRecallPullFlags() {
		try {
			LOG.debug("");
			recallAuth = false;
			pullAuth = false;
			// To skip editAuth checks for approvers other than the current approver and
			// userOwnsDocument contact.
			boolean edit = editAuth;
			if (contactDocument != null && contactDocument.getContact() != null) {
				// true iff the document being viewed belongs to the current user:
				boolean userOwnsDocument = contactDocument.getContact().equals(getvContact());
				if (edit) {
					LOG.debug("");
					// To show pull/recall buttons to approvers and to hide from owner & current
					// approver, since editAuth will be true for approvers also.
					if (!userOwnsDocument && !mayApprove) {
						LOG.debug("");
						edit = false;
					}
				}
				// skip all checks if document is VOID, LOCKED, PENDING, or submittable or
				// editable -- can't be Recalled or Pulled
				if (contactDocument.getFormType() == PayrollFormType.MODEL_RELEASE) {
					// special checks for Model Release
					if ((!contactDocument.getStatus().isSealed()) && contactDocument.getEmployerSignature() != null && contactDocument.getTimeSent() == null) {
						if (userOwnsDocument) {
							recallAuth = false;
							setEditAuth(true);
						} else {
							if (contactDocument.getStatus() != ApprovalStatus.PENDING) {
								recallAuth = true;
								// setEditAuth(false);
							} else {
								recallAuth = false;
							}
							if (contactDocument.getStatus() != ApprovalStatus.RECALLED) {
								setEditAuth(false);
							} else {
								setEditAuth(true);
							}
						}
					}
					if (contactDocument.getEmployerSignature() != null && contactDocument.getEmpSignature() != null) {
						if (userOwnsDocument) {
							recallAuth = false;
							setEditAuth(false);
						}
					}
				}
                else if (! edit && contactDocument != null && (! contactDocument.getStatus().isSealed()) &&
						contactDocument.getStatus() != ApprovalStatus.PENDING && ! contactDocument.getSubmitable()) {
					LOG.debug("");
					ApprovalPath path = null;
					if (contactDocument.getApproverId() != null && contactDocument.getApproverId() < 0) {
						Integer pathId = -(contactDocument.getApproverId());
						path = getApprovalPathDAO().findById(pathId);
					}
					else {
						path = ContactDocumentDAO.getCurrentApprovalPath(contactDocument, null, null);
					}
					if (contactDocument.getFormType().getAllowsRecall()) {
						// (Recall is not allowed for WTPA, I9, or some Canadian forms)
						if (userOwnsDocument) {
							boolean approvedAfterSubmit = isApprovedAfterSubmit(contactDocument);
							LOG.debug("");
							recallAuth = (!approvedAfterSubmit);
						}
						/*
						 * if ((! recallAuth) && contactDocument.getLastSignature() != null &&
						 * contactDocument.getLastSignature().getUserAccount().equals(getvContact().
						 * getUser().getAccountNumber()) && contactDocument.getStatus() !=
						 * ApprovalStatus.RECALLED && contactDocument.getStatus() !=
						 * ApprovalStatus.REJECTED && ! mayApprove) { // Hide recall button if current
						 * user's last signature was for pull event. recallAuth = true; }
						 */
						if (!recallAuth) {
							calculateRecallAuth(contactDocument, path);
						}
						if (recallAuth && contactDocument.getStatus() == ApprovalStatus.APPROVED &&
								contactDocument.getFormType() == PayrollFormType.DEPOSIT &&
								contactDocument.getContactDocEvents() != null &&
								(! contactDocument.getContactDocEvents().isEmpty())) {
							if (contactDocument.getContactDocEvents().get(contactDocument.getContactDocEvents().size() - 1).getType() == TimedEventType.SIGN) {
								LOG.debug("Hide recall for Approved   Direct Deposit form");
								recallAuth = false;
							}
						}
					}

					if (! mayApprove && ! userOwnsDocument && (contactDocument.getStatus() != ApprovalStatus.APPROVED)) { // (if user can already edit it, it makes no sense to Pull it)
						LOG.debug("");
						Project tcProj = TimecardUtils.findProject(getProduction(), contactDocument);
						pullAuth = TimecardUtils.followsCurrentApprover(contactDocument, tcProj, getvContact(), path);
					}
				}
			}
			LOG.debug("recall=" + recallAuth + ", pull=" + pullAuth);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Determine if the current user should get "Recall" authority. This will be
	 * true if the user has approved the document at least once, and the document
	 * has not been recalled back "in front of them" since their last approval.
	 * Assumes {@link #recallAuth} is false upon entry, and may change
	 * {@link #recallAuth} before returning.
	 * <p>
	 * Note that if a document is waiting for approver Andy, and is Pulled from him
	 * by Baker (a later approver), then Andy cannot Recall the document from Baker.
	 *
	 * @param cd   The contactDocument of interest.
	 * @param path the ApprovalPath for the given document.
	 */
	private void calculateRecallAuth(Approvable cd, ApprovalPath path) {
		try {
			// Will qualify if the user approved the document at least once,
			// unless the document has been recalled "in front of" them.
			LOG.debug("");
			String currAcct = getvUser().getAccountNumber();
			int rejIx = -1;
			int apprIx = -1;
			int pullIx = -1;
			// Find last approval (by this user) & last reject/recall (by anyone)
			for (int i = 0; i < cd.getEvents().size(); i++) {
				@SuppressWarnings("rawtypes")
				SignedEvent evt = cd.getEvents().get(i);
				LOG.debug("");
				if (evt.getType() == TimedEventType.REJECT ||
						evt.getType() == TimedEventType.RECALL) {
					rejIx = i; // track last reject/recall
				}
				else if (evt.getType() == TimedEventType.APPROVE
						&& evt.getUserAccount().equals(currAcct)) {
					apprIx = i; // track current user's last approval
				}
				else if (evt.getType() == TimedEventType.PULL
						&& (! evt.getUserAccount().equals(currAcct))) {
					pullIx = i; // track current user's last approval
				}
			}
			if (apprIx >= 0) { // at least one approval by this user
				if (apprIx > rejIx) { // no rejects, or approved after the last reject
					recallAuth = true;
					LOG.debug("recallAuth: " + recallAuth);
				}
				else {
					// have to see if this approver is earlier in the chain than
					// the current approver.
					Project tcProj = TimecardUtils.findProject(getProduction(), cd);
					recallAuth = TimecardUtils.precedesCurrentApprover(cd, tcProj, getvContact(), path);
					LOG.debug("recallAuth: " + recallAuth);
				}
			}
			else if (apprIx < 0 && pullIx >= 0) {
				// If Document is pulled from the user and user has never approved the document
				Project tcProj = TimecardUtils.findProject(getProduction(), cd);
				recallAuth = TimecardUtils.precedesCurrentApprover(cd, tcProj, getvContact(), path);
				LOG.debug("recallAuth: " + recallAuth);
			}
			if (recallAuth) {
				boolean userOwnsDocument = false;
				User cdUser = null;
				if (contactDocument.getContact() != null) {
					cdUser = contactDocument.getContact().getUser();
					cdUser = getUserDAO().refresh(cdUser);
					userOwnsDocument = cdUser.getAccountNumber().equals(currAcct);
				}
				boolean didApprove = findDidApprove(contactDocument, getvUser());
				if (! (userOwnsDocument || didApprove)) {
					recallAuth = false;
					LOG.debug("recallAuth: " + recallAuth);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Determine if an approval occurred after the last Submit
	 *
	 * @param wtc The timecard of interest.
	 * @return True iff the given timecard has been approved by anyone since it was
	 *         last submitted.
	 */
	private boolean isApprovedAfterSubmit(ContactDocument cd) {
		// Find last submit event for Submit msg on Full Timecard page;
		// also determine if any approval has occurred after the last Submit
		boolean approvedAfterSubmit = false;
		try {
			for (ContactDocEvent evt : cd.getContactDocEvents()) {
				if (evt.getType() == TimedEventType.SUBMIT) {
					// submitMsg = "Submitted by " + evt.getName() + " on " + evt.getDisplayTime();
					approvedAfterSubmit = false;
				}
				else if (evt.getType() == TimedEventType.APPROVE) {
					approvedAfterSubmit = true;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approvedAfterSubmit;
	}

	/**
	 * Return whether the owner of the current document is the current user. We want
	 * the employee to still be able to attach documents if the form is not voided
	 * or pending.
	 *
	 * LS-1285
	 * 
	 * @return
	 */
	public boolean getIsOwner() {
		boolean isOwner = false;

		if (contactDocument != null) {
			Contact docOwner = contactDocument.getContact();
			Contact currContact = getvContact();
			if (currContact != null && docOwner != null &&
					currContact.getId() != null) {
				isOwner = currContact.getId().equals(docOwner.getId());
			}
		}

		return isOwner;
	}

	/**
	 * Action method for the OK button on the "Recall" confirmation dialog box when
	 * the document is being recalled to the current user (as an approver, not the
	 * employee). First, change the approver to the current user. Notifications will
	 * normally be issued during that step.
	 *
	 * @return null navigation string
	 */
	private String actionRecallOk() {
		try {
			// Note: recall() will unlock the CD.
			contactDocument = getContactDocumentDAO().recall(contactDocument, getvContact().getId());
			if (refreshContactDoc()) {
				clearEmpTable();
				calculateAuthFlags();
				if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
					((CustomFormBean) formBean).recall(contactDocument);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the OK button on the "Recall" confirmation dialog box, when
	 * the document is being recalled to the employee. First, change the approver to
	 * no approver (the document will be ready to submit again). Notifications will
	 * normally be issued during that step.
	 *
	 * @return null navigation string
	 */
	private String actionRecallToEmplOk() {
		try {
			// Note: recall() will unlock the CD.
			contactDocument = getContactDocumentDAO().recall(contactDocument, null);
			if (refreshContactDoc()) {
				clearEmpTable();
				calculateAuthFlags();
				if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
					((CustomFormBean) formBean).recallToEmpl(contactDocument);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the OK button on the "Pull" confirmation dialog box. Change
	 * the approver to be the current user.
	 *
	 * @return null navigation string
	 */
	private String actionPullOk() {
		try {
			// Note: pull() will unlock the CD.
			contactDocument = getContactDocumentDAO().pull(contactDocument, getvContact().getId());
			clearEmpTable();
			calculateAuthFlags();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionEmployerInit() {
		LOG.debug("EmployER's Initial (not sign or submit)");
		actionEmployeeSign(ACT_EMPLOYER_INITIAL);
		return null;
	}

	/**
	 * Method to handle of Sign event of employer for WTPA and I9.
	 * 
	 * @return null navigation string.
	 */
	public String actionEmployerSign() {
		LOG.debug("Employer's Signature ");
		try {
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			if (contactDocument.getRelatedFormId() != null && formBean.getForm() != null) {
				if (editMode) {
					if (formBean.getForm() != null) {
						formBean.getForm().trim(); // trim blanks on all String fields
					}
					if (!formBean.checkSaveValid()) {
						return null;
					}
				}
				if (!formBean.checkEmployerSignValid()) {
					// form-specific checks failed; message was issued already.
					return null;
				}
			}
			User currUser = SessionUtils.getCurrentUser(); // Note: don't use vUser, it may be stale!
			if (!checkPin(currUser)) {
				return null;
			}
			if (!checkLocked(currUser, "Sign.")) {
				return null;
			}
			PinPromptBean bean = PinPromptBean.getInstance();
			if (contactDocument.getFormType() == PayrollFormType.OTHER) {
				bean.promptPin(this, ACT_EMPLOYEE_SIGN, "ContactFormBean."+"PinSubmitSelf.");
			}
			else {
				bean.promptPin(this, ACT_EMPLOYER_SIGN, "ContactFormBean."+"PinSubmitSelf.");
			}
			addFocus("submit");
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called (via confirmOk) when an Employer (approver) e-signs a
	 * Standard Form.
	 *
	 * @return null navigation string.
	 */
	private String actionEmployerSignOk() {
		try {
			LOG.debug("");
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			if (editMode) {
				boolean ret = save();
				if (!ret) {
					return null;
				}
			}
			formBean.employerSign(currentAction); // do any special processing based on the form type
			contactDocument.setLockedBy(null); // make sure it's unlocked
			getContactDocumentDAO().attachDirty(contactDocument);
			refreshContactDoc();
			calculateAuthFlags();
			clearEmpTable();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Handle an employee's "Initial" action. This is similar to an employee
	 * signing, except that the prompt does not require a password, only the PIN.
	 *
	 * @return empty navigation string
	 */
	public String actionEmployeeInit() {
		LOG.debug("Employee's Initial (not sign or submit)");
		actionEmployeeSign(ACT_EMPLOYEE_INITIAL);
		return null;
	}

	/**
	 * Handle an employee's "Initial" action to "disagree" (reject) a contract. This
	 * was created for the ACTRA contract. Note that for an Initial the prompt does
	 * not require a password, only the PIN.
	 *
	 * @return empty navigation string
	 */
	public String actionEmployeeInitDisagree() {
		LOG.debug("Employee's Initial to disagree (not sign or submit)");
		actionEmployeeSign(ACT_EMPLOYEE_INITIAL_DISAGREE);
		return null;
	}

	/**
	 * Method to handle Sign event of employee OR EMPLOYER which is NOT also a
	 * Submit event. This is used for custom documents that require more than one
	 * employee signature, and for some builtin documents (such as the ACTRA
	 * contract).
	 *
	 * @param action The action value to be used in the dialog box presented for
	 *               signing. This is also used to determine whether this is a full
	 *               signature or just an "initial" (no password required).
	 *
	 * @return null navigation string.
	 */
	private String actionEmployeeSign(int action) {
		LOG.debug("Employee's Signature (not submit)");
		try {
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			User currUser = SessionUtils.getCurrentUser(); // Note: don't use vUser, it may be stale!
			if (!checkPin(currUser)) {
				return null;
			}
			if (!checkLocked(currUser, "Sign.")) {
				return null;
			}
			String msgSuffix;
			//LS-1963
			if (action == ACT_EMPLOYER_INITIAL && contactDocument.getEmpAgrees() == null &&
					contactDocument.getEmpDisagrees() == null) {
				PopupBean.getInstance().show(this, ACT_OVERIDE_INITIAL,
						"ContactFormBean.MissingPerformerInitials.Title",
						"ContactFormBean.MissingPerformerInitials.Text", "Confirm.OK",
						"Confirm.Cancel");
			}
			else {
				boolean initial = false;
				if (action == ACT_EMPLOYEE_SIGN) {
					msgSuffix = "PinSignSelf.";
				}
				else {
					msgSuffix = "PinInitialSelf.";
					initial = true;
				}
				actionSubmitEmployerInits(action, initial, msgSuffix);
			}

		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * method to display prompt for pin to submit employee/employer initials -
	 * LS-1963
	 *
	 * @param action
	 */
	private String actionSubmitEmployerInits(int action, boolean initial, String msgSuffix) {
		PinPromptBean bean = PinPromptBean.getInstance();
		bean.promptPin(this, action, "ContactFormBean." + msgSuffix);
		if (initial) {
			bean.setPinOnly(true);
			bean.setPassword(getvUser().getPassword());
		}
		addFocus("submit");
		return null;
	}

	/**
	 * Action method called when the employee's first (and only) Sign&Submit button
	 * is clicked on a custom document.
	 */
	public void actionEmployeeSign1of1() {
		LOG.debug("first and only employee signature/submit on custom doc");
		if (!checkDisable(Constants.BUTTON_EMP_SIGN)) {
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = 1;
			actionSubmit();
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when the employee's first (and only) Sign&Submit button
	 * is clicked on a custom document, but ONE prior Initial is required.
	 */
	public void actionEmployeeSign1of1Init1() {
		actionEmployeeSign1of1InitN(1);
	}

	/**
	 * Action method called when the employee's first (and only) Sign&Submit button
	 * is clicked on a custom document, but TWO prior Initials are required.
	 */
	public void actionEmployeeSign1of1Init2() {
		actionEmployeeSign1of1InitN(2);
	}

	/**
	 * Action method called when the employee's first (and only) Sign&Submit button
	 * is clicked on a custom document, but "n" prior Initials are required.
	 *
	 * @param initCount The number of initials required before this signing.
	 */
	private void actionEmployeeSign1of1InitN(int initCount) {
		LOG.debug("first and only employee signature/submit on custom doc; inits required=" + initCount);
		if (contactDocument.getContactDocEvents() != null && (contactDocument.getContactDocEvents().size() < initCount)) {
//			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "two Initials");
//			PopupBean bean = PopupBean.getInstance();
//			bean.show("ContactFormBean.SendPendingDoc.");
		}
		else if (! checkDisable(Constants.BUTTON_EMP_SIGN)) {
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = 1;
			actionSubmit();
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when employee's first Sign button is clicked on a custom
	 * document, and a second Sign&Submit button exists. The signature and date
	 * fields populated will be EmployeeSignature1 and EmployeeSignatureDate1.
	 */
	public void actionEmployeeSign1of2() {
		actionEmployeeSignMofN(1, 2);
	}

	/**
	 * Action method called when employee's first Sign button is clicked on a custom
	 * document, and a total of 3 signature buttons exists. The signature and date
	 * fields populated will be EmployeeSignature1 and EmployeeSignatureDate1.
	 */
	public void actionEmployeeSign1of3() {
		actionEmployeeSignMofN(1, 3);
	}

	/**
	 * Action method called when employee's second Sign button is clicked on a
	 * custom document, and a total of 3 signature buttons exists. The signature and
	 * date fields populated will be EmployeeSignature2 and EmployeeSignatureDate2.
	 */
	public void actionEmployeeSign2of3() {
		actionEmployeeSignMofN(2, 3);
	}

	/**
	 * Action method called when employee's first Sign button is clicked on a custom
	 * document, and one or more additional Sign buttons exist. The signature and
	 * date fields populated will be EmployeeSignature1 and EmployeeSignatureDate1.
	 */
	public void actionEmployeeSign1ofN() {
		actionEmployeeSignMofN(1, 99);
	}

	/**
	 * Action method called when employee's second Sign button is clicked on a
	 * custom document, and one or more additional Sign buttons exist. The signature
	 * and date fields populated will be EmployeeSignature2 and
	 * EmployeeSignatureDate2.
	 */
	public void actionEmployeeSign2ofN() {
		actionEmployeeSignMofN(2, 99);
	}

	/**
	 * Action method called when employee's third Sign button is clicked on a custom
	 * document, and one or more additional Sign buttons exist. The signature and
	 * date fields populated will be EmployeeSignature3 and EmployeeSignatureDate3.
	 */
	public void actionEmployeeSign3ofN() {
		actionEmployeeSignMofN(3, 99);
	}

	/**
	 * Action method called for employee's signature #4 out of 'N' (more than 4).
	 */
	public void actionEmployeeSign4ofN() {
		actionEmployeeSignMofN(4, 7);
	}

	/**
	 * Action method called for employee's signature #5 out of 'N' (more than 5).
	 */
	public void actionEmployeeSign5ofN() {
		actionEmployeeSignMofN(5, 7);
	}

	/**
	 * Action method called for employee's signature #6 out of 'N' (more than 6).
	 */
	public void actionEmployeeSign6ofN() {
		actionEmployeeSignMofN(6, 7);
	}

	/**
	 * Action method called for employee's signature #6 out of 6. Since this is the
	 * last signature, it will be treated as Sign & Submit.
	 */
	public void actionEmployeeSign6of6() {
		actionEmployeeSignLast(6, 6);
	}

	/**
	 * Action method called for employee's signature #7 out of 7. Since this is the
	 * last signature, it will be treated as Sign & Submit.
	 */
	public void actionEmployeeSign7of7() {
		actionEmployeeSignLast(7, 7);
	}

	/**
	 * Action method called when an employee's Sign button is clicked on a custom
	 * document, and additional Sign, or Sign&Submit buttons exists.
	 *
	 * @param thisSign  Which signature number this is.
	 * @param totalSign The total number of signatures expected, where the last one
	 *                  will be treated as a "Sign & Submit".
	 */
	private void actionEmployeeSignMofN(int thisSign, int totalSign) {
		LOG.debug("employee signature #" + thisSign + " on custom doc with " + totalSign + " signatures");
		if (!refreshContactDoc()) { // doc was deleted by another user
			return;
		}
		if (contactDocument.getSubmitable() && (!checkDisable(Constants.BUTTON_EMP_SIGN))) { // double-check status;
																								// button should be
																								// disabled if not
																								// submitable
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = thisSign; // specifies which button to remove, and which signature/date fields to populate
			actionEmployeeSign(ACT_EMPLOYEE_SIGN);
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when the employee's second and last Sign (& submit)
	 * button is clicked on a custom document, and the first signature is required.
	 * The signature and date fields populated will be EmployeeSignature2 and
	 * EmployeeSignatureDate2.
	 */
	public void actionEmployeeSign2of2() {
		actionEmployeeSignLast(2, 2);
	}

	/**
	 * Action method called when the employee's third and last Sign (& submit)
	 * button is clicked on a custom document, and the prior signatures are
	 * required. The signature and date fields populated will be EmployeeSignature3
	 * and EmployeeSignatureDate3.
	 */
	public void actionEmployeeSign3of3() {
		actionEmployeeSignLast(3, 3);
	}

	/**
	 * Action method called when the employee's last signature button, i.e., the
	 * Sign & Submit button, is clicked on a custom document. "3 of 4" indicates
	 * that this is the 4th signature button available, but only 3 signatures total
	 * (including this one) should have been done. The signature and date fields
	 * populated will be EmployeeSignature4 and EmployeeSignatureDate4.
	 */
	public void actionEmployeeSignLast3of4() {
		actionEmployeeSignLast(4, 3);
	}

	/**
	 * Action method called when the employee's last signature button, i.e., the
	 * Sign & Submit button, is clicked on a custom document.
	 *
	 * @param thisSign      - which signature fields should be populated with this
	 *                      signing data. This should be greater than or equal to
	 *                      'requiredSigns'.
	 * @param requiredSigns - the number of required signatures (including this one)
	 *                      for this document.
	 */
	private void actionEmployeeSignLast(int thisSign, int requiredSigns) {
		LOG.debug("last employee signature (for signature fields #" + thisSign + ") on custom doc; total signatures required = " + requiredSigns);
		if (contactDocument.getContactDocEvents() == null || (contactDocument.getContactDocEvents().size() < (requiredSigns-1))) {
			// too few prior signatures
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Prior Signature(s)");
		}
		else if (! checkDisable(Constants.BUTTON_EMP_SIGN)) {
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = thisSign; // specifies which button to remove, and which signature/date fields to populate
			actionSubmit(); // Last signature also acts as Submit
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when the employee's second and last Sign (& submit)
	 * button is clicked on a custom document, and the first signature is optional,
	 * e.g., for a Loan-out corporation.
	 */
	public void actionEmployeeSign2of2Opt() {
		LOG.debug("last employee signature on custom doc, may be first or second");
		// No need to check for prior signature, as it is optional
		if (!checkDisable(Constants.BUTTON_EMP_SIGN)) {
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = 2; // specifies which button to remove, and which signature/date fields to populate
			actionSubmit();
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when employee's first Initial button is clicked on
	 * custom document, and a second Initial button exists.
	 */
	public void actionEmployeeInit1of2() {
		LOG.debug("first employee initial on custom doc with 2 initials");
		actionEmployeeInit(1);
	}

	/**
	 * Action method called when employee's Initial button is clicked on custom
	 * document that has a single initial field.
	 */
	public void actionEmployeeInit1of1() {
		LOG.debug("first employee initial on custom doc with 1 initials");
		actionEmployeeInit(1);
	}

	/**
	 * Action method called when employee's Initial button is clicked on custom
	 * document.
	 */
	public void actionEmployeeInit1() {
		LOG.debug("first employee initial on custom doc with any number of initials");
		actionEmployeeInit(1);
	}

	/**
	 * Action method called when employee's second Initial button is clicked on
	 * custom document.
	 */
	public void actionEmployeeInit2() {
		LOG.debug("second employee initial on custom doc with any number of initials");
		actionEmployeeInit(2);
	}

	/**
	 * Action method called when employee's third Initial button is clicked on
	 * custom document.
	 */
	public void actionEmployeeInit3() {
		LOG.debug("Third employee initial on custom doc with any number of initials");
		actionEmployeeInit(3);
	}

	/**
	 * Action method called when employee's second & last Initial button is clicked
	 * on custom document.
	 */
	public void actionEmployeeInit2of2() {
		LOG.debug("second employee initial on custom doc with 2 initials");
		actionEmployeeInit(2);
	}

	/**
	 * Action method called when employee's second & last Initial button is clicked
	 * on custom document.
	 */
	private void actionEmployeeInit(int initNum) {
		if (!refreshContactDoc()) { // doc was deleted by another user
			return;
		}
		if (contactDocument.getSubmitable() && (!checkDisable(Constants.BUTTON_EMP_INIT))) { // double-check status;
																								// button should be
																								// disabled if not
																								// submitable
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = initNum;
			actionEmployeeInit();
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.EmployeeInitialDisabled"));
//			MsgUtils.addFacesMessage("Form.EmployeeInitialDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called when the (only) approver's Sign button is clicked on a
	 * custom document.
	 */
	public void actionApproverSign1of1() {
		LOG.debug("first and only approver signature on custom doc");
		approverSign(1);
	}

	/**
	 * Action method called when the first approver's Sign button is clicked on a
	 * custom document having two approvers.
	 */
	public void actionApproverSign1of2() {
		LOG.debug("first of two approver signatures on custom doc");
		approverSign(1);
	}

	/**
	 * Action method called when the second approver's Sign button is clicked on a
	 * custom document having two approvers. Currently this is the same as
	 * 'actionApproverSign2()'; that is, the total number of approval buttons in the
	 * document is irrelevant.
	 */
	public void actionApproverSign2of2() {
		LOG.debug("second (& final) of two approver signatures on custom doc");
		approverSign(2);
	}

	/**
	 * Action method called when the second in-document approval Sign button is
	 * clicked on a custom document. The total number of signatures in the document
	 * is not important for this process. The '3' controls which signature/date
	 * fields within the custom document will be updated.
	 */
	public void actionApproverSign2() {
		LOG.debug("second approver signature on custom doc");
		approverSign(2);
	}

	/**
	 * Action method called when the third in-document approval Sign button is
	 * clicked on a custom document. The total number of signatures in the document
	 * is not important for this process. The '3' controls which signature/date
	 * fields within the custom document will be updated.
	 */
	public void actionApproverSign3() {
		LOG.debug("third approver signature on custom doc");
		approverSign(3);
	}

	/**
	 * Action method called when an approver's Sign button is clicked on a custom
	 * document.
	 *
	 * @param signIndex The approver sequence number, e.g., 1 or 2, based on the JS
	 *                  script calling a particular method.
	 */
	private void approverSign(int signIndex) {
		if (contactDocument.getSubmitable() || (!getIsApprover())) {
			LOG.info(MsgUtils.formatMessage("Form.ApproverSignatureDisabled"));
			// displaying a message causes doc to disappear :(
//			MsgUtils.addFacesMessage("ContactFormBean.NotReadyForApproval", FacesMessage.SEVERITY_ERROR);
		}
		else if (! checkDisable(Constants.BUTTON_APP_SIGN)) {
			checkSaveCustom(); // run custom form Save code if necessary.
			btnSignIndex = signIndex;
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			approve();
		}
		else {
			LOG.info(MsgUtils.formatMessage("Form.ApproverSignatureDisabled"));
//			MsgUtils.addFacesMessage("Form.ApproverSignatureDisabled", FacesMessage.SEVERITY_INFO);
		}
	}

	/**
	 * Action method called (via confirmOk) when an employee successfully initials a
	 * custom Form.
	 * 
	 * @param action The action event passed from confirmOk
	 *
	 * @return null navigation string.
	 */
	private String actionEmployeeInitOk(int action) {
		return actionEmployeeSignOk(action, true);
	}

	/**
	 * Action method called (via confirmOk) when an employee successfully e-signs a
	 * custom Form.
	 * 
	 * @param action The action event passed from confirmOk
	 *
	 * @return null navigation string.
	 */
	private String actionEmployeeSignOk(int action) {
		return actionEmployeeSignOk(action, false);
	}

	/**
	 * Action method called when an employee successfully e-signs or initials a
	 * custom Form.
	 *
	 * @param action  The action event passed from confirmOk
	 * @param initial True if this is an "Initial" action, false if a "Sign" action.
	 *
	 * @return null navigation string.
	 */
	private String actionEmployeeSignOk(int action, boolean initial) {
		try {
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			if (editMode) {
				boolean ret = save();
				if (!ret) {
					return null;
				}
			}
			TimedEventType event = TimedEventType.SIGN;
			if (initial) {
				if (formBean == null) {
					event = TimedEventType.INITIAL;
				}
				else {
					event = formBean.calculateInitialEvent(currentAction); // determine the event type based on the form type
				}
			}
			else if (action == ACT_SIGN_OPT) {
				event = TimedEventType.SIGN_OPT;
			}
			getContactDocEventDAO().createEvent(contactDocument, event);
//			contactDocument.getContactDocEvents().add(evt);
			contactDocument.setLockedBy(null);	// make sure it's unlocked
			getContactDocumentDAO().attachDirty(contactDocument);
			refreshContactDoc();
			calculateAuthFlags();
			clearEmpTable();
			if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
				// code for custom documents
				List<ContactDocEvent> events = contactDocument.getContactDocEvents();
				if (events != null && events.size() > 0) {
					DocumentService.updateXfdf(contactDocument, events.get(events.size() - 1), btnSignIndex);
				}
//				formBean = CustomFormBean.getInstance();
				LOG.debug("<>");
				String targetNav;
				if (myStarts) { // User is in My Starts already
					targetNav = HeaderViewBean.MYFORMS_MENU_DETAILS; // go to My Starts page
				}
				else { // Normally switch to Payroll / Start Forms page
					targetNav = HeaderViewBean.PAYROLL_START_FORMS;
				}

				// Navigate may cause immediate dispose() of this bean, so don't reference any
				// fields after that!
				ContactDocument cd = contactDocument; // save this field for use following navigate ...
				HeaderViewBean.navigate(targetNav);
				SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, cd.getId());
				SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, cd.getEmployment().getId());
				SessionUtils.put(Constants.ATTR_CONTACT_ID, cd.getContact().getId());
				// formBean.rowClicked(contactDocument);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * If the current form is a custom form and we have the formBean for it, and the
	 * custom form is in edit mode, run the custom Save code.
	 */
	private void checkSaveCustom() {
		if (formBean != null && formBean instanceof CustomFormBean) {
			CustomFormBean bean = (CustomFormBean) formBean;
			if (!bean.getReadOnly()) { // in edit mode on custom form
				bean.actionSave(); // do Java "save" action; javaScript already ran the save-annotations.
			}
		}
	}

	/**
	 * Check if the given user has a PIN created; if not, prompt him.
	 *
	 * @param currUser The User to be checked.
	 * @return True if the user has a PIN, false if not.
	 */
	private boolean checkPin(User currUser) {
		if (currUser.getPin() == null) {
			setShowChangePin(true);
			ChangePinBean.getInstance().show(this);
			addFocus("pin");
			return false;
		}
		return true;
	}

	/**
	 * Attempt to lock the current contactDocument. If this fails, put up a prompt
	 * explaining the problem to the user.
	 *
	 * @param currUser The User to be given the lock.
	 * @param msgType  The additional text that modifies the message id used for the
	 *                 message to the user.
	 * @return True if the user has acquired the lock, false if not.
	 */
	private boolean checkLocked(User currUser, String msgType) {
		if (contactDocument.getFormType() == PayrollFormType.SUMMARY) {
			// CD for the Summary Sheet is not persisted & therefore can't be locked.
			return true;
		}
		return super.checkLocked(contactDocument, currUser, msgType);
	}

	/**
	 * Determine if the given button name will be disabled, or is considered
	 * disabled, for the current contact.
	 *
	 * @param btnName The name of the Button to be disabled.
	 * @return True if the button is considered disabled for the current user, false
	 *         if not.
	 */
	private boolean checkDisable(String btnName) {
		try {
			if (DocumentService.getMapOfContactDocButtonsToDisable() != null) {
				List<String> disabledButtons = DocumentService.getMapOfContactDocButtonsToDisable().get(contactDocument.getId());
				if (disabledButtons != null && disabledButtons.contains(btnName)) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Action method invoked when user clicks the "Acknowledge" button. This method
	 * is invoked by a Open document with Document Action Acknowledge.
	 *
	 * @return null navigation string.
	 */
	public String actionAcknowledge() {
		// Send the currently viewed PENDING document to the employee.
		try {
			contactDocument = getContactDocumentDAO().refresh(contactDocument);
			if (contactDocument != null && contactDocument.getStatus() == ApprovalStatus.OPEN) {
				User currUser = SessionUtils.getCurrentUser(); // Note: don't use vUser, it may be stale!
				if (!checkPin(currUser)) {
					return null;
				}
				if (!checkLocked(currUser, "Acknowledge.")) {
					return null;
				}
				// "issue confirmation dialog"
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_ACK, "ContactFormBean.AcknowledgeDocument.");
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when an employee successfully e-signs or initials a
	 * custom Form.
	 *
	 * @param initial True if this is an "Initial" action, false if a "Sign"
	 *
	 * @return null navigation string.
	 */
	private String actionAcknowledgeOk() {
		try {
			LOG.debug("<>");
			if (!refreshContactDoc()) { // doc was deleted by another user
				return null;
			}
			getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
			Document doc = contactDocument.getDocument();
			if (doc.getApprovalRequired()) {
				contactDocument = submitContactDocument(contactDocument, true, TimedEventType.ACK);
			}
			else {
				// No Approval Required
				contactDocument.setStatus(ApprovalStatus.APPROVED);
				getContactDocumentDAO().attachDirty(contactDocument);
				getContactDocEventDAO().createEvent(contactDocument, TimedEventType.ACK);
			}
			clearEmpTable();
			calculateAuthFlags();
			if (formBean != null && contactDocument.getFormType() == PayrollFormType.OTHER) {
				formBean.rowClicked(contactDocument);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * This method is called by our subclasses (which are called by the JSF
	 * framework) when this bean is about to go 'out of scope', e.g., when the
	 * user is leaving the page. Note that in JSF 2.1, this method is not called
	 * for session expiration, so we handle that case via the Disposable
	 * interface.
	 */
	@PreDestroy
	public void preDestroy() {
		LOG.debug("");
		if (disposer != null) {
			disposer.unregister(this);
		}
		dispose();
	}

	/**
	 * This method is called when this bean is about to go 'out of scope', e.g.,
	 * when the user is leaving the page or their session expires. We use it to
	 * unlock the WeeklyTimecard to make it available again for editing.
	 */
	@Override
	public void dispose() {
		LOG.debug("");
		try {
			if (contactDocument != null && contactDocument.getId() != null && getvUser() != null) {
				contactDocument = getContactDocumentDAO().refresh(contactDocument); // prevent "non-unique object" failure in logout case
				if (contactDocument != null && contactDocument.getLockedBy() != null
						 && getvUser().getId().equals(contactDocument.getLockedBy())) {
					LOG.debug("dispose");
					getContactDocumentDAO().unlock(contactDocument, getvUser().getId());
				}
			}
			contactDocument = null;
			if (formBean != null) {
				formBean.setContactDoc(null);
				formBean.setForm(null);
			}
			if (stateMap != null) {
				stateMap.clear();
			}
		}
		catch (Exception e) {
			LOG.error("Exception: ", e);
		}
	}

	/**
	 * Action method for uploading an Attachment.
	 * 
	 * @return null navigation string.
	 */
	public String actionUploadAttachment() {
		if (contactDocument != null && contactDocument.getId() != null) {
			// LS-4700 special MRF check to not allow user to upload when in edit mode
			if (contactDocument.getFormType() == PayrollFormType.MODEL_RELEASE && editMode) { 
				MsgUtils.addFacesMessage("ContactFormBean.UploadAttachment.SaveBeforeUpload", FacesMessage.SEVERITY_INFO);
			} else {
				PopupUploadBean uploadBean = PopupUploadBean.getInstance();
				uploadBean.show(this, ACT_ATTACH, "ContactFormBean.UploadAttachment.");
			}
		}
		return null;
	}

	/**
	 * Action method called when the user hits Save on the Upload prompt dialog.
	 */
	private String actionUploadAttachmentOk(){
		try {
			PopupUploadBean uploadBean = PopupUploadBean.getInstance();
			String attactmentName = uploadBean.getDisplayFilename();
			// Get saved attactment.id value from bean
			Integer id = uploadBean.getAttachmentId();
			LOG.debug("attactment id = "+id);
			Attachment latestAttactment = getAttachmentDAO().findById(id);
			latestAttactment.setName(attactmentName);
			Map<String, Object> values = new HashMap<> ();
			values.put("name", attactmentName);
			values.put("contactDocument", contactDocument);
			if (getAttachmentDAO().findOneByNamedQuery(Attachment.GET_ATTACHMENT_BY_NAME_CONTACT_DOC, values) != null) {
				uploadBean.setIsNameError(true);
				uploadBean.displayError("Attachment Name duplicate.");
				return null;
			}
			else {
				latestAttactment.setContactDocument(contactDocument);
				latestAttactment.setIsPrivate(uploadBean.getIsPrivateAttachment());
				getAttachmentDAO().attachDirty(latestAttactment);
			}
			if (latestAttactment.getMimeType().isPdf()) {
				getAttachmentDAO().convertToXod(id);
			}
			LOG.debug("");
			SessionUtils.put(Constants.ATTR_PAYROLL_ATTACHMENT_ID, latestAttactment.getId());
			getAttachmentBean().viewAttachments(contactDocument, latestAttactment, null);
			if (getSelectedTab() != TAB_ATTACHMENTS) {
				setSelectedTab(TAB_ATTACHMENTS);
				HeaderViewBean.getInstance().setMiniTab(TAB_ATTACHMENTS);
				// LS-1086 navigate added to prevent screen from being "blocked"
				if (myStarts) { // User is in My Starts, use appropriate target (LS-1322)
					HeaderViewBean.navigate(HeaderViewBean.MYFORMS_MENU_DETAILS);
				}
				else { // usual "in-production" Start Forms page.
					HeaderViewBean.navigate(HeaderViewBean.PAYROLL_START_FORMS);
				}
			}
			else {
				contactDocument.getEmpSignature(); // for LIEs
			}
		}
		catch (Exception e) {
			EventUtils.logError("DocumentListBean upload document failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action method called when user clicks the attachment icon for a contact document,
	 * to view the attachments linked with the contact documents.
	 * @return null navigation string
	 */
	public String showAttachments() {
		// get CD id, set via f:setPropertyActionListener associated with attachment (paper-clip) icon:
		Integer id = (Integer) SessionUtils.get(Constants.ATTR_ATTACHMENT_CONTACT_DOCUMENT);
		LOG.debug("Id = " + id);
		if (id != null) {
			ContactDocument cd = getContactDocumentDAO().findById(id);
			if (cd != null && cd.getAttachments() != null) {

				// TODO need to switch OUR current cd to this one, in case user clicks Attach button
				// while looking at the Attachments tab.

				getAttachmentBean().viewAttachments(cd, null, null);
				HeaderViewBean.getInstance().setMiniTab(TAB_ATTACHMENTS);
				setSelectedTab(TAB_ATTACHMENTS);
			}
		}
		return null;
	}

	/**
	 * @return The current document's type. Used from JSP/xhtml.
	 */
	public PayrollFormType getFormType() {
		PayrollFormType type = PayrollFormType.OTHER;
		if (contactDocument != null) {
			type = contactDocument.getFormType();
		}
		else if (previewType != null) {
			type = previewType;
		}
		return type;
	}

	/** See {@link #clickedDocumentName}. */
	public String getClickedDocumentName() {
		return clickedDocumentName;
	}

	/** See {@link #clickedDocumentName}. */
	public void setClickedDocumentName(String clickedDocumentName) {
		this.clickedDocumentName = clickedDocumentName;
	}

	/** See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}

	/** See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/** See {@link #aicp}. */
	public boolean getAicp() {
		return aicp;
	}

	/** See {@link #aicp}. */
	public void setAicp(boolean aicp) {
		this.aicp = aicp;
	}

	/** See {@link #contactDocument}. */
	public ContactDocument getContactDocument() {
		return contactDocument;
	}

	/** See {@link #contactDocument}. */
	public void setContactDocument(ContactDocument contactDocument) {
		this.contactDocument = contactDocument;
	}

//	/** See {@link #showChangePin}. */
//	public boolean isShowChangePin() {
//		return showChangePin;
//	}
//	/** See {@link #showChangePin}. */
//	@Override
//	public void setShowChangePin(boolean showChangePin) {
//		this.showChangePin = showChangePin;
//	}

	/** See {@link #standardDoc}. */
	public boolean getStandardDoc() {
		return standardDoc;
	}

	/** See {@link #standardDoc}. */
	public void setStandardDoc(boolean standardDoc) {
		this.standardDoc = standardDoc;
	}

	/** See {@link #empContactList}. */
	public List<EmploymentWrapper> getEmpContactList() {
		if (empContactList == null) {
			createEmpContactList(selectedContact);
		}
		return empContactList;
	}

	/** See {@link #empContactList}. */
	public void setEmpContactList(List<EmploymentWrapper> empContactList) {
		this.empContactList = empContactList;
	}

	/** See {@link #currentContactName}. */
	public String getCurrentContactName() {
		return currentContactName;
	}

	/** See {@link #currentContactName}. */
	public void setCurrentContactName(String currentContactName) {
		this.currentContactName = currentContactName;
	}

	/** See {@link #isPreviewDocument}. */
	public boolean getIsPreviewDocument() {
		return isPreviewDocument;
	}

	/** See {@link #isPreviewDocument}. */
	public void setIsPreviewDocument(boolean isPreviewDocument) {
		this.isPreviewDocument = isPreviewDocument;
	}

	/** See {@link #isJump}. */
	public boolean getIsJump() {
		return isJump;
	}

	/** See {@link #isJump}. */
	public void setJump(boolean isJump) {
		this.isJump = isJump;
	}

	/** See {@link #editMaster}. */
	public boolean getEditMaster() {
		return editMaster;
	}

	/** See {@link #editMaster}. */
	public void setEditMaster(boolean editMaster) {
		this.editMaster = editMaster;
	}

	/** See {@link #isSummarySheet}. */
	public boolean getSummarySheet() {
		return isSummarySheet;
	}

	/** See {@link #isSummarySheet}. */
	public void setSummarySheet(boolean isSummarySheet) {
		this.isSummarySheet = isSummarySheet;
	}

	/** See {@link #textEditor}. */
	public String getTextEditor() {
		return textEditor;
	}

	/** See {@link #textEditor}. */
	public void setTextEditor(String textEditor) {
		this.textEditor = textEditor;
	}

	/** See {@link #contactsDL}. */
	public List<SelectItem> getContactsDL() {
		return contactsDL;
	}

	/** See {@link #contactsDL}. */
	public void setContactsDL(List<SelectItem> contactsDL) {
		this.contactsDL = contactsDL;
	}

	/** See {@link #selectedContact}. */
	public Contact getSelectedContact() {
		return selectedContact;
	}

	/** See {@link #selectedContact}. */
	public void setSelectedContact(Contact selectedContact) {
		this.selectedContact = selectedContact;
	}

	/**
	 * Method called when user selects a 'mini-tab'. Don't switch to
	 * Attachment's tab if no attachments exist. Although the user shouldn't be
	 * able to click the tab in this case (it shouldn't be visible), the
	 * automatic "restore mini-tab setting" when returning to the Start Forms
	 * page could attempt to set the mini-tab to one that doesn't exist.
	 *
	 * @see com.lightspeedeps.web.view.View#setSelectedTab(int)
	 */
	@Override
	public void setSelectedTab(int n) {
		LOG.debug("");
		if (n == TAB_DOCUMENTS) {
			if (n != getSelectedTab()) {
				refreshContactDoc();
				if (contactDocument.getFormType() == PayrollFormType.OTHER) {
					SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, contactDocument.getId());
					SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, contactDocument.getEmployment().getId());
					SessionUtils.put(Constants.ATTR_CONTACT_ID, contactDocument.getContact().getId());
					// NOTE: the 'navigate' call will result in dispose() being called on this instance!
					HeaderViewBean.navigate(HeaderViewBean.PAYROLL_START_FORMS);
				}
			}
		}
		else {
			if (contactDocument != null) {
				if (n == TAB_ATTACHMENTS) {
					refreshContactDoc();
					LOG.debug("");
					List<Attachment> list = getAttachmentBean().getAttachmentList();
					if (list == null || list.size() == 0) {
						n = TAB_DOCUMENTS;
					}
					else {
						Integer id = SessionUtils.getInteger(Constants.ATTR_PAYROLL_ATTACHMENT_ID);
						if (id != null) {
							//attachment = AttachmentDAO.getInstance().findById(id);
							getAttachmentBean().previewAttachment(getAttachmentDAO().findById(id));
						}
						else {
							getAttachmentBean().previewAttachment(list.get(0));
						}
					}
				}
				else if (contactDocument.getFormType() != PayrollFormType.I9 &&
						contactDocument.getFormType() != PayrollFormType.W4) {
					n = TAB_DOCUMENTS;
				}
			}
			else {
				n = TAB_DOCUMENTS;
			}
		}
		super.setSelectedTab(n);
	}

	/** See {@link #showAllProjects}. */
	public boolean getShowAllProjects() {
		return showAllProjects;
	}

	/** See {@link #showAllProjects}. */
	public void setShowAllProjects(boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}

	/** See {@link #showMrStarts}. */
	public boolean getShowMrStarts() {
		return showMrStarts;
	}

	/** See {@link #showMrStarts}. */
	public void setShowMrStarts(boolean showMrStarts) {
		this.showMrStarts = showMrStarts;
	}

	/** See {@link #editAuth}. */
	public boolean getEditAuth() {
		return editAuth;
	}

	/** See {@link #editAuth}. */
	public void setEditAuth(boolean editAuth) {
		this.editAuth = editAuth;
	}

	/** See {@link #infoMessage}. */
	public String getInfoMessage() {
		return infoMessage;
	}

	/** See {@link #infoMessage}. */
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}

	/** See {@link #mayApprove}. */
	public boolean getMayApprove() {
		return mayApprove;
	}

	/** See {@link #mayApprove}. */
	public void setMayApprove(boolean mayApprove) {
		this.mayApprove = mayApprove;
	}

	/** See {@link #isOnboardingJump}. */
	public boolean getOnboardingJump() {
		return isOnboardingJump;
	}

	/** See {@link #isOnboardingJump}. */
	public void setOnboardingJump(boolean isOnboardingJump) {
		this.isOnboardingJump = isOnboardingJump;
	}

	/** See {@link #currentContactChainMap}. */
	public Map<Project, Map<DocumentChain, List<Department>>> getCurrentContactChainSet() {
		if (currentContactChainMap == null) {
			findDocumentChainsForCurrentContact();
		}
		return currentContactChainMap;
	}

	/** See {@link #currentContactChainMap}. */
	public void setCurrentContactChainSet(Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap) {
		this.currentContactChainMap = currentContactChainMap;
	}

	/** See {@link #primaryFolder}. */
	public Folder getPrimaryFolder() {
		if (primaryFolder == null && getProduction() != null && !getProduction().isSystemProduction()) {
			primaryFolder = createPrimaryFolder();
		}
		return primaryFolder;
	}

	/** See {@link #primaryFolder}. */
	public void setPrimaryFolder(Folder primaryFolder) {
		this.primaryFolder = primaryFolder;
	}

	/** See {@link #documentList}. */
	public List<SelectItem> getDocumentList() {
		if (documentList == null) {
			documentList = createDocumentList();
		}
		return documentList;
	}

	/** See {@link #documentList}. */
	public void setDocumentList(List<SelectItem> documentList) {
		this.documentList = documentList;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getNonSystemProduction();
			if (production == null) {
				myStarts = true;
				getProductionList();
				if (getSelectedProdId() == null || getSelectedProdId() == 0) {
					selectedProdId = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
					if (selectedProdId == null) { // See if there is a User preferences setting. ESS-1513
						selectedProdId = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID);
					}
					if (selectedProdId == null) {
						if (productionList.size() == 2) { // one production in list, plus "Select a production" item
							selectedProdId = (Integer)productionList.get(1).getValue();
						}
					}
				}
				if (getSelectedProdId() != null && getSelectedProdId() != 0) {
					production = getProductionDAO().findById(getSelectedProdId());
					LOG.debug("Selected Production = " + production.getTitle());
					if (!production.getAllowOnboarding()) {
						production = null;
						selectedProdId = null;
					}
					SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, selectedProdId);
				}
			}
		}
		return production;
	}

	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #productionList}. */
	public List<SelectItem> getProductionList() {
		if (productionList == null) {
			productionList = new ArrayList<>();
			List<Production> prodList = getProductionDAO().findByNamedQuery(Production.GET_PRODUCTION_LIST_BY_USER_AND_ALLOW_ONBOARDING, map("user",getvUser()));
			for (Production p : prodList) {
				productionList.add(new SelectItem(p.getId(), p.getTitle()));
			}
			LOG.debug("Selected prod id:" + getSelectedProdId());
			if (!productionList.isEmpty()) {
				if (SessionUtils.getCurrentUser().getShowCanada()) {
					productionList.add(0, new SelectItem(0, "select an " + Constants.CANADA_PRODUCTION_TEXT));
				}
				else if (SessionUtils.getCurrentUser().getShowUS())  {
					productionList.add(0, new SelectItem(0, "select a " + Constants.STANDARD_PRODUCTION_TEXT));
				}
			}
		}
		return productionList;
	}

	/** See {@link #productionList}. */
	public void setProductionList(List<SelectItem> productionList) {
		this.productionList = productionList;
	}

	/** See {@link #selectedProdId}. */
	public Integer getSelectedProdId() {
		return selectedProdId;
	}

	/** See {@link #selectedProdId}. */
	public void setSelectedProdId(Integer selectedProdId) {
		this.selectedProdId = selectedProdId;
		// Save id - other beans may use this to determine production
		SessionUtils.put(Constants.ATTR_VIEW_PRODUCTION_ID, selectedProdId);
		production = null; // force refresh
		getProduction();
		selectedContact = getContactDAO().findByUserProduction(getvUser(), production);
		LOG.debug(selectedContact);
		// currentContact = selectedContact;
		setvContact(selectedContact);
	}

	/** See {@link #occupationOfReplacement}. */
	public String getOccupationOfReplacement() {
		return occupationOfReplacement;
	}

	/** See {@link #occupationOfReplacement}. */
	public void setOccupationOfReplacement(String occupationOfReplacement) {
		this.occupationOfReplacement = occupationOfReplacement;
	}

	/** See {@link #prodSelectDisabled}. */
	public boolean isProdSelectDisabled() {
		return prodSelectDisabled;
	}

	/** See {@link #prodSelectDisabled}. */
	public void setProdSelectDisabled(boolean prodSelectDisabled) {
		this.prodSelectDisabled = prodSelectDisabled;
	}

	/** See {@link #userToUpdate}. */
	public User getUserToUpdate() {
		return userToUpdate;
	}

	/** See {@link #userToUpdate}. */
	public void setUserToUpdate(User userToUpdate) {
		this.userToUpdate = userToUpdate;
	}

	/** See {@link #projectList}. */
	public List<SelectItem> getProjectList() {
		if (projectList == null) {
			projectList = createProjectList(getvContact());
		}
		return projectList;
	}

	/** See {@link #projectList}. */
	public void setProjectList(List<SelectItem> projectList) {
		this.projectList = projectList;
	}

	/** See {@link #selectedProjectId}. */
	public Integer getSelectedProjectId() {
		return selectedProjectId;
	}

	/** See {@link #selectedProjectId}. */
	public void setSelectedProjectId(Integer projectId) {
		selectedProjectId = projectId;
		SessionUtils.put(Constants.ATTR_LAST_PROJECT_ID, selectedProjectId);
	}

	/** Method to get the appEdit value for void button. */
	public boolean getAppEdit() {
		if (formBean != null) {
			return formBean.getAppEdit();
		}
		else { // shouldn't happen? But if so, assume they are NOT an approver
			return false;
		}
	}

	/** See {@link #appSign}. */
	public boolean getAppSign() {
		return appSign;
	}

	/** See {@link #appSign}. */
	public void setAppSign(boolean appSign) {
		this.appSign = appSign;
	}

	/** See {@link #isEditor}. */
	public boolean getIsEditor() {
		return isEditor;
	}

	/** See {@link #isEditor}. */
	public void setIsEditor(boolean isEditor) {
		this.isEditor = isEditor;
	}

	/** See {@link #pullAuth} */
	public boolean getPullAuth() {
		return pullAuth;
	}

	/** See {@link #pullAuth} */
	public void setPullAuth(boolean pullAuth) {
		this.pullAuth = pullAuth;
	}

	/** See {@link #recallAuth} */
	public boolean getRecallAuth() {
		return recallAuth;
	}

	/** See {@link #recallAuth} */
	public void setRecallAuth(boolean recallAuth) {
		this.recallAuth = recallAuth;
	}

	/** See {@link #isApprover} */
	public boolean getIsApprover() {
		return isApprover;
	}

	/** See {@link #isApprover} */
	public void setApprover(boolean isApprover) {
		this.isApprover = isApprover;
	}

	/** See {@link #pseudoApprover} */
	public boolean getPseudoApprover() {
		return pseudoApprover;
	}

	/** See {@link #pseudoApprover} */
	public void setPseudoApprover(boolean pseudoApprover) {
		this.pseudoApprover = pseudoApprover;
	}

	/** See {@link #allowI9attachment}. */
	public boolean getAllowI9attachment() {
		return allowI9attachment;
	}

	/** See {@link #currentAction}. */
	public int getCurrentAction() {
		return currentAction;
	}

	/** See {@link #currentAction}. */
	public void setCurrentAction(int currentAction) {
		this.currentAction = currentAction;
	}

	/** See {@link #isUnavailable} */
	public boolean getIsUnavailable() {
		return isUnavailable;
	}

	/** See {@link #isUnavailable} */
	public void setUnavailable(boolean isUnavailable) {
		this.isUnavailable = isUnavailable;
	}

	/** See {@link #currContactChainEditorMap} */
	public Map<Project, List<DocumentChain>> getCurrContactChainEditorMap() {
		if (currContactChainEditorMap == null) {
			findDocumentChainEditorForCurrentContact();
		}
		return currContactChainEditorMap;
	}

	/** See {@link #currContactChainEditorMap} */
	public void setCurrContactChainEditorMap(Map<Project, List<DocumentChain>> currContactChainEditorMap) {
		this.currContactChainEditorMap = currContactChainEditorMap;
	}

	/** See {@link #projectTitle}. */
	public String getProjectTitle() {
		return projectTitle;
	}

	/** See {@link #projectTitle}. */
	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	/** See {@link #projectsTitle}. */
	public String getProjectsTitle() {
		return projectsTitle;
	}

	/** See {@link #projectsTitle}. */
	public void setProjectsTitle(String projectsTitle) {
		this.projectsTitle = projectsTitle;
	}

	/** See {@link #startFormLabelTitle}. */
	public String getStartFormLabelTitle() {
		return startFormLabelTitle;
	}

	/** See {@link #startFormLabelTitle}. */
	public void setStartFormLabelTitle(String startFormLabelTitle) {
		this.startFormLabelTitle = startFormLabelTitle;
	}

	/** See {@link #showAutoFill}. */
	public boolean getShowAutoFill() {
		return showAutoFill;
	}

	/** See {@link #showAutoFill}. */
	public void setShowAutoFill(boolean showAutoFill) {
		this.showAutoFill = showAutoFill;
	}

}
