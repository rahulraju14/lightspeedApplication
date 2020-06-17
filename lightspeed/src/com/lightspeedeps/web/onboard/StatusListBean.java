package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.component.column.Column;
import org.icefaces.ace.component.datatable.DataTable;
import org.icefaces.ace.event.*;
import org.icefaces.ace.model.table.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.dao.ContactDocumentDAO.EmpDocInfo;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DocumentStatusCount;
import com.lightspeedeps.service.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.*;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.*;
import com.lightspeedeps.web.view.ListView;

/**
 * This class is the backing bean for the Start status mini tab,
 * under Onboarding sub-main tab.
 * It is useful in creating the list of contactdocuments,
 * it also contains methods to filter the list.
 */
@ManagedBean
@ViewScoped
public class StatusListBean extends ListView implements Serializable, FilterHolder, Disposable  {

	private static final long serialVersionUID = -6487791336112396029L;

	private static final Log log = LogFactory.getLog(StatusListBean.class);

	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELIVER = 10;

	/** The relative mini-tab number (origin 0) of the Dist & Review tab. */
	private static final int ONBOARD_TAB_DIST_REVIEW = 1;
	/** The relative mini-tab number (origin 0) of the Approval Paths tab. */
	private static final int ONBOARD_TAB_APPROVAL_PATHS = 4;

	/** Static literal for Model Release form */
	private static final String FORM_MODEL_RELEASE = "MODELRELEASE";
	/** Static literal for WTPA form */
	private static final String FORM_WTPA = "WTPA";

	/** The packet dao instance used to use the dao layer methods. */
	private transient ContactDAO contactDAO;

	/** The Employment instance . */
	private Employment employment;

	/** The packet instance . */
	private Packet packet;

	/** The packet dao instance . */
	private transient PacketDAO packetDao;

	/** The document Dao instance . */
	private transient DocumentDAO documentDao;

	/** The contact dao instance . */
	private transient ContactDocumentDAO contactDocumentDAO;

	/** The PayrollPreferenceDAO instance */
	private transient PayrollPreferenceDAO payrollPreferenceDAO;

	/** The list of contact to be shown on the UI. */
	private List<Employment> employmentList;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	/** The list of checked Employments from the data table, will receive the delivered documents */
	private List<Employment> employmentsSelected = new ArrayList<>();

	/** The list of ContactDocuments of currently selected Employment; this is the list
	 * displayed on the right-hand side of the Distribution & Review page */
	private List<ContactDocument> contactDocumentList = new ArrayList<>();

	/**Boolean field used for keeping track of master check box check uncheck event */
	private boolean checkedForAll = false;

	/** True iff the list of employments is currently filtered by one or more of
	 * the column-header filters (supported by ace). */
	private boolean filtered = false;

	/** A reference to our ace:dataTable, kept only when filtering is in effect,
	 * so that we can access the filtered result set. */
	private DataTable dataTable;

	/** The document instance used to know the checked document*/
	private Document checkedDocument;

	/** The documents to be removed from the Employment */
	private Document documentToRemove;

	/** The StartStatusType enum used to render the view on the UI for selected view type */
	private StartStatusType statusType = StartStatusType.EMPLOYEE_STATUS;

	/** List of StartStatusType used to filter the view on the UI  */
	private final List<SelectItem> statusTypeFilterList = new ArrayList<>();

	/**
	 * The FilterBean instance that manages filter settings and changes.
	 */
//	private final FilterBean filterBean;

	/**
	 * The current Department filter setting -- the database id of the
	 * Department that the displayed contacts should belong to. If 0, then the
	 * Department filter is set to "All" (no filtering).
	 */
//	private Integer departmentId;

	/** Boolean field used to show or hide the RED colored status icons */
	private boolean redChecked = true;

	/** Boolean field used to show or hide the YELLOW colored status icons */
	private boolean yellowChecked = true;

	/** Boolean field used to show or hide the GREEN colored status icons */
	private boolean greenChecked = true;

	private final boolean aicp;

	private ContactDocument contactDocToDelete;

	/** Action code for popup confirmation/prompt dialog to send pending docs. */
	private static final int ACT_SEND_PENDING = 20;

	/** List to hold the ContactDocuments with "PENDING" status which will be changed to OPEN status. */
	//private List<ContactDocument> docsToSend = null;

	/** Map used to hold the ContactDocuments list for employments,
	 * ContactDocument list will have documents with "PENDING" status which will be changed to OPEN status. */
	private Map<Employment, List<ContactDocument>> mapOfEmpPendingDocsToSend = null;

	/** Database Id of the selected Document from the drop down*/
	private Integer selectedDocumentId = null;

	/** If true, then only the documents that are waiting for current approver will be shown.  */
	private boolean docsRequireAttn = false;

	/** Used to hold the approval path ids of the paths, for which current contact is a pool approver. */
	Map<Integer,Boolean> contactInPath = new HashMap<>();

	/** Select item list for the document list drop down, the list is also useful in refreshing the view according to selected document */
	private List<SelectItem> documentListDL;

	/** List holds the list of all the Approver instances for the current logged in Contact */
	private List<Integer> approverListOfCurrentContact = null;

	/** Count of WTPA documents those are not validated to become OPEN for submittle */
	private Integer count;

	/** Count of modelrelease documents those are not validated to become OPEN for submittle */
	private Integer mrcount=0;

	/** Map of Project and a map for document chains for current contact in that project. Inner map holds the
	 * document chains and list of departments for which the current contact is an approver.
	 * List of department will be null if contact is Production approver for the document chain.*/
	private Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap = null;

	/** True iff the user has elected to show timecards or StartForm`s for all
	 * projects in the case of a Commercial (AICP) production.  This option is
	 * only available to users with Financial Data Admin permission.*/
	private boolean showAllProjects = false;

	/** The (commercial) project whose data is being displayed; this will be the current project,
	 * unless {@link #showAllProjects} is true, in which case this will be null. */
	Project viewProject = null;

	/** Holds the list of document names that are sent, for notification.*/
	private List<String> distributedDocumentList = new ArrayList<>();

	private final Disposer disposer = Disposer.getInstance();

	/** Count of Locked documents those are not validated to Send. */
	private Integer lockedDocCount;

	private final OnboardService onboardService;

	/** Map of Project and list of document chains for which current contact is an editor in that project. */
	private Map<Project, List<DocumentChain>> currContactChainEditorMap = null;

	/** List holds the status count values of all the employments. */
	private List<DocumentStatusCount> employmentStatusGraph = null;

	/** Action code for popup confirmation/prompt dialog to delete documents with its attachments. */
	private static final int ACT_DELETE_ATTACHMENT = 21;

	/** Project label based on the production type we are in. LS-2056 */
	private String projectLbl;

	/** The default constructor . */
	public StatusListBean() {
		super(Contact.SORTKEY_NAME, "Contact.");
		disposer.register(this);
		onboardService = new OnboardService();
		String attr = Constants.ATTR_CURRENT_MINI_TAB + ".onboarding";
		if (SessionUtils.get(attr) == null) { // no saved minitab selection yet
			int defTab = ONBOARD_TAB_DIST_REVIEW;
			if (! AuthorizationBean.getInstance().hasPageField("10.2,view")) {
				// user is not allowed to view Dist & Review mini-tab, so switch.
				// Very rare, someone would have to have 'manage approvers' but no other onboarding permissions.
				defTab = ONBOARD_TAB_APPROVAL_PATHS;
			}
			HeaderViewBean.getInstance().setMiniTab(defTab);
		}
		findDocumentChainsForCurrentContact();
		getEmploymentStatusGraph();
		getApproverListOfCurrentContact();
		Integer id = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID);
		setupSelectedItem(id);

		createStatusTypeFilterList();
		statusType = (StartStatusType) SessionUtils.get(Constants.ATTR_ONBOARDING_VIEW_STATUS);
		if (statusType == null) {
			statusType = StartStatusType.EMPLOYEE_STATUS;
		}

		selectedDocumentId = (Integer) SessionUtils.get(Constants.ATTR_ONBOARDING_SELECTED_DOCUMENT_ID);
		if (selectedDocumentId != null) {
			employmentList = createEmploymentList();
		}
		else {
			selectedDocumentId = 0; // prevent change event first time
		}

		// employment = (Employment) SessionUtils.get(Constants.ATTR_ONBOARDING_SELECTED_CONTACT);
		if (employment != null) {
			createDocListOfSelectedContact(employment);
		}
		else {
			employmentList = createEmploymentList();
			if (!employmentList.isEmpty()) {
				employment = employmentList.get(0);
				createDocListOfSelectedContact(employment);
			}
		}
		forceLazyInit();
		if (SessionUtils.getNonSystemProduction() != null) {
			aicp = SessionUtils.getNonSystemProduction().getType().hasPayrollByProject();
		}
		else {
			aicp = false;
		}
		if (aicp && ! getShowAllProjects()) {
			viewProject = SessionUtils.getCurrentProject();
		}
		// Set the production label based off of the produciton type we are in.
		if(SessionUtils.getCurrentUser().getShowCanada()) {
			setProjectLbl(Constants.PROJECT_S_TEXT_CANADA);
		}
		else {
			setProjectLbl(Constants.PROJECT_S_TEXT_US);
		}
	}

	/** Used to return the instance of the packet list bean . */
	public static StatusListBean getInstance() {
		return (StatusListBean)ServiceFinder.findBean("statusListBean");
	}

	/**
	 * Utility method to initialize the list on the selected view.
	 */
	public void inIt() {
		employmentList = null;
		//contactDocumentList = null;
	}

	/** Listener used for row selection, sets the contact with the currently selected row object . */
	@Override
	public void listenRowClicked (SelectEvent event) {
		try {
			employment = (Employment)event.getObject();
			SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, employment.getId());
			SessionUtils.put(Constants.ATTR_CONTACT_ID, employment.getContact().getId());
			SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force timecard pages to use attr_contact_id
			createContactDocumentList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

//	/** Listener used to change the value of the StartStatusType with the new selected value
//	 * @param evt ValueChangeEvent
//	 */
// 5/19/16 currently unused
//	public void listenStatusTypeChange (ValueChangeEvent evt) {
//		statusType = (StartStatusType) evt.getNewValue();
//		SessionUtils.put(Constants.ATTR_ONBOARDING_VIEW_STATUS, statusType);
//		switch (statusType) {
//		case  EMPLOYEE_STATUS:
//			getFilterBean().register(this, FilterBean.TAB_START_STATUS);
//			inIt();
//			break;
//		case DOCUMENT_STATUS:
//			getFilterBean().register(DocStatusBean.getInstance(), FilterBean.TAB_START_STATUS);
//			DocStatusBean.getInstance().inIt();
//			break;
//		case EMPLOYEE_DETAIL:
//			getFilterBean().register(EmpDetailBean.getInstance(), FilterBean.TAB_START_STATUS);
//			EmpDetailBean.getInstance().inIt();
//			for (DocumentChain doc : EmpDetailBean.getInstance().getDocumentChainList()) {
//				log.debug("document name "+doc.getName());
//			}
//			break;
//		}
//		forceLazyInit();
//	}

	/** Method used to create the list of contacts (Employments) that are part of the current production
	 * @return contact list
	 */
	@SuppressWarnings("unchecked")
	private List<Employment> createEmploymentList() {
		List<Employment> list = new ArrayList<>();
		try {
			Production production = SessionUtils.getProduction();
			Map<String, Object> values= new HashMap<>();
			if (production != null) {
				if (! docsRequireAttn) {
					if (selectedDocumentId != null && selectedDocumentId != 0) {
						if (production.getType().isAicp() && ! showAllProjects) {
							values.put("project", SessionUtils.getCurrentProject());
							values.put("documentId", selectedDocumentId);
							list = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID_PROJECT, values);
						}
						else {
							values.put("production", production);
							values.put("documentId", selectedDocumentId);
							list = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_LIST_BY_DOCUMENT_ID, values);
						}
					}
					else {
						if (production.getType().isAicp() && ! showAllProjects) {
							values.put("production", production);
							values.put("project", SessionUtils.getCurrentProject());
							list = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT, values);
						}
						else {
							list = EmploymentDAO.getInstance().findByProperty("contact.production", SessionUtils.getProduction());
						}
					}
				}
				// If display check box is true
				else {
					List<ContactDocument> poolContactDocList = new ArrayList<>();
					Contact curContact = SessionUtils.getCurrentContact();
					/*approverListOfCurrentContact = ApproverDAO.getInstance().
							findByNamedQuery(Approver.GET_APPROVER_LIST_BY_CONTACT, map("contact", curContact));
					for (Approver app : approverListOfCurrentContact) {
						integerList.add(app.getId());
					}*/ // find employments from CD whose approverId matches current contact's approver Id

					// With document filter
					if (selectedDocumentId != null && selectedDocumentId != 0) {
						if (production.getType().isAicp() && ! showAllProjects) {
							values.put("project", SessionUtils.getCurrentProject());
							values.put("documentId", selectedDocumentId);
							list = EmploymentDAO.getInstance().findByNamedQuery(
									Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT_DOCUMENT_ID,
									values, "approverId", getApproverListOfCurrentContact());
							poolContactDocList = ContactDocumentDAO.getInstance().
									findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT_DOCUMENT_ID, values);
						}
						else {
							values.put("production", production);
							values.put("documentId", selectedDocumentId);
							list = EmploymentDAO.getInstance().findByNamedQuery(
									Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_DOCUMENT_ID,
									values, "approverId", getApproverListOfCurrentContact());
							poolContactDocList = ContactDocumentDAO.getInstance().
									findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_DOCUMENT_ID, values);
						}
					}

					else {  // Without document filter
						if (production.getType().isAicp() && ! showAllProjects) {
							values.put("project", SessionUtils.getCurrentProject());
							list = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID_PROJECT,
									values, "approverId", getApproverListOfCurrentContact());
							poolContactDocList = ContactDocumentDAO.getInstance().
									findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER_PROJECT, values);
						}
						else {
							values.put("production", production);
							list = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_CONTACTDOCUMENT_EMPLOYMENT_BY_APPROVER_ID,
									values, "approverId", getApproverListOfCurrentContact());
							poolContactDocList = ContactDocumentDAO.getInstance().
									findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_POOL_APPROVER, values);
						}
					}

					// To Filter Pool Documents if display check box is true
					Boolean inPool;
					for (ContactDocument cDoc : poolContactDocList) {
						Set<Contact> contactPool = new HashSet<>();
						ApprovalPath path = null;
						//ApproverGroup Changes
						Integer approvalPathId = -(cDoc.getApproverId());
						if (cDoc.getApproverId() != null) {
							//Check for Department pool
							if (cDoc.getIsDeptPool()) { // Department pool, Group Id
								log.debug("Department Pool");
								// Currently approvalPathId is Approver Group Id, because contact document is in dept pool.
								ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(approvalPathId);
								if (approverGroup != null) {
									contactPool = approverGroup.getGroupApproverPool();
								}
								path = ContactDocumentDAO.getCurrentApprovalPath(cDoc, null, null);
								if (path != null) {
									// Replace Approver group id with Approval Path Id.
									approvalPathId = path.getId();
								}
							}
							inPool = contactInPath.get(approvalPathId);
							if (inPool == null) { // haven't seen this path before, query database...
								if (! cDoc.getIsDeptPool()) {
									// Production Pool
									path = ApprovalPathDAO.getInstance().findById(approvalPathId);
									contactPool = path.getApproverPool();
								}
								//ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
								if (path != null) {
									if (contactPool != null && contactPool.contains(curContact)) {
										if (! list.contains(cDoc.getEmployment())) {
											list.add(cDoc.getEmployment());
										}
										 contactInPath.put(approvalPathId, true);
									}
									else {
										 contactInPath.put(approvalPathId, false);
									}
								}
							}
							else if (inPool) { // found pool previously, & contact was not in pool
								if (! list.contains(cDoc.getEmployment())) {
									list.add(cDoc.getEmployment());
								}
							}
						}
					}
				}
			}

			if (list != null) {
				Iterator<Employment> itr = list.iterator();
				while (itr.hasNext()) {
					Employment emp = itr.next();
					Role role = emp.getRole();
					if (role.isAdmin() || role.isDataAdmin() ||
							role.isFinancialAdmin()) {
						itr.remove();
					}
					else {
						boolean found = false;
						inner:
						for (DocumentStatusCount status : getEmploymentStatusGraph()) {
							if (status.getEmploymentId() != null && status.getEmploymentId().equals(emp.getId())) { // TODO status.getEmploymentId was null. why?
								emp.setPercentageArray(DocumentUtils.setStatusGraph(status));
								found = true;
								break inner;
							}
						}
						if (! found) {
							emp.setPercentageArray(new Integer[] {0,0,0,0});
						}
					}
				}
			}
			initEmploymentDocCounts(list);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return list;
	}

	/**
	 * Update the approved-documents and total-documents counts for each of the
	 * Employment instances given, using current information from the database.
	 *
	 * @param list The Employment list to be updated.
	 */
	private void initEmploymentDocCounts(List<Employment> empList) {
		try {
			// Get approved- and unapproved-document counts
			List<EmpDocInfo> infoList = getContactDocumentDAO().findEmploymentDocStatus();

			Map<Integer,Integer> approvedCount = new HashMap<>();
			Map<Integer,Integer> unapprovedCount = new HashMap<>();
			// Place the counts into separate Maps for quick retrieval
			for (EmpDocInfo e : infoList) {
				if (e.getApproved() > 0) {
					approvedCount.put(e.getEmpId(), e.getCount());
				}
				else {
					unapprovedCount.put(e.getEmpId(), e.getCount());
				}
			}

			// Then scan the given list, updating each item's counts from
			// data pulled from the Maps.
			Integer approved, unapproved, total;
			for (Employment emp : empList) {
				approved = approvedCount.get(emp.getId());
				if (approved == null) {
					approved = 0;
				}
				unapproved = unapprovedCount.get(emp.getId());
				total = NumberUtils.safeAdd(approved, unapproved);
				emp.setDocCount(total);
				emp.setDocCountByStatus(approved);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * This is used to create the list of documents that are distributed to the currently selected contact
	 */
	private void createContactDocumentList() {
		try {
			log.debug("");
			Map<String,Object> values = new HashMap<>();
			RowState state = new RowState();
			state.setSelected(true);
			if (contactDocumentList == null) {
				contactDocumentList = new ArrayList<>();
			}
			if (getEmploymentList() != null && (! getEmploymentList().isEmpty())) {
				if (employment != null) {
					values.put("employmentId",employment.getId());
					stateMap.put(employment, state);
				}
				else if (employmentsSelected != null && ! employmentsSelected.isEmpty()) {
					values.put("employmentId",employmentsSelected.get(0).getId());
					stateMap.put(employmentsSelected.get(0), state);
				}
				if (selectedDocumentId != null && selectedDocumentId != 0) {
					if (SessionUtils.getProduction().getType().isAicp() && ! showAllProjects) {
						values.put("project", SessionUtils.getCurrentProject());
						values.put("documentId", selectedDocumentId);
						setContactDocumentList(getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_PROJECT_DOCUMENT_ID, values));
					}
					else {
						values.put("documentId", selectedDocumentId);
						setContactDocumentList(getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_DOCUMENT_ID, values));
					}
				}
				else {
					if (SessionUtils.getProduction().getType().isAicp() && ! showAllProjects) {
						values.put("project", SessionUtils.getCurrentProject());
						setContactDocumentList(getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID_PROJECT, values));
					}
					else {
						setContactDocumentList(getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_EMPLOYMENT_ID, values));
					}
				}
				if (getDocsRequireAttn()) {
					Contact curContact = SessionUtils.getCurrentContact();
					/*List<Integer> approverIdList = ApproverDAO.getInstance().
							findIntegerListByNamedQuery(Approver.GET_APPROVER_IDS_BY_CONTACT, map("contact", curContact));*/

					Boolean inPool;
					Iterator<ContactDocument> itr = getContactDocumentList().iterator();
					while (itr.hasNext()) {
						ContactDocument cd = itr.next();
						if (cd.getApproverId() != null) {
							if ((cd.getApproverId() > 0) && (! getApproverListOfCurrentContact().contains(cd.getApproverId()))) {
								itr.remove();
							}
							// POOL case
							else if (cd.getApproverId() < 0) {
								//ApproverGroup Changes
								Set<Contact> contactPool = new HashSet<>();
								ApprovalPath path = null;
								Integer approvalPathId = -(cd.getApproverId());
								//Check for Department pool
								if (cd.getIsDeptPool()) { // Department pool, Group Id
									log.debug("Department Pool");
									// Currently approvalPathId is Approver Group Id, because contact document is in dept pool.
									ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(approvalPathId);
									if (approverGroup != null) {
										contactPool = approverGroup.getGroupApproverPool();
									}
									path = ContactDocumentDAO.getCurrentApprovalPath(cd, null, null);
									if (path != null) {
										// Replace Approver group id with Approval Path Id.
										approvalPathId = path.getId();
									}
								}
								inPool = contactInPath.get(approvalPathId);
								if (inPool == null) { // haven't seen this path before, query database...
									if (! cd.getIsDeptPool()) {
										// Production Pool
										path = ApprovalPathDAO.getInstance().findById(approvalPathId);
										contactPool = path.getApproverPool();
									}
									//ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
									if (path != null) {
										contactPool = path.getApproverPool();
										if (contactPool != null && (! contactPool.contains(curContact))) {
											 itr.remove();
											 contactInPath.put(approvalPathId, false);
										}
										else {
											 contactInPath.put(approvalPathId, true);
										}
									}
									else {
										 itr.remove();
									}
								}
								else if (! inPool) { // found pool previously, & contact was not in pool
									itr.remove();
								}
							}
						}
						else {
							itr.remove();
						}
					}
				}
				Production production = SessionUtils.getProduction();
				Contact curContact = SessionUtils.getCurrentContact();
				AuthorizationBean bean = AuthorizationBean.getInstance();
				boolean viewAll = bean.hasPermission(Permission.VIEW_ALL_DISTRIBUTED_FORMS);
				if (! (viewAll || bean.hasPermission(Permission.MANAGE_START_DOCS))) {
					// Hide pending documents from employee and/or others who are not an approver for that document.
					// MANAGE_START_DOCS lets user see document name in list, even if they cannot view the actual document.
					ContactFormBean.removeHiddenDocs(employment.getContact(), curContact, production, getContactDocumentList(),
							getCurrentContactChainMap(), getCurrContactChainEditorMap(), false, false);
				}

				// Refresh PayrollPreference object to prevent LIE
				PayrollPreference pf = getPayrollPreferenceDAO().refresh(production.getPayrollPref());
				// Hide Payroll Start if 'use model release' option selected and FF is on. LS-4502; LS-4757
				if (pf.getUseModelRelease() && FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS)) {
					removePayrollStarts();
				}

				// Set relative status (for icons) and "waiting for"
				for (ContactDocument cd : getContactDocumentList()) {
					cd.setViewStatus(ApproverUtils.findRelativeStatus(cd, getvContact()));
					cd.setWaitingFor(ApproverUtils.calculateWaitingFor(cd));
					if (cd.getDocumentChain() != null) {
						cd.getDocumentChain().getNormalizedName(); // 6/7/16 prevent LIE on switch to Dist&Review tab
					}
					if (viewAll || employment.getContact().equals(curContact) ||
							ApproverUtils.isContactInChainMap(getCurrentContactChainMap(), cd, curContact, cd.getProject()) ||
							ApproverUtils.isEditorInChainMap(getCurrContactChainEditorMap(), cd, curContact, cd.getProject())) {
							cd.setDisableJump(false);
					}
					else {
						cd.setDisableJump(true);
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
	 * Remove any Payroll Start forms from the current contactDocumentList, as they
	 * are not to be displayed in this production.
	 */
	private void removePayrollStarts() {
		Iterator<ContactDocument> itr = getContactDocumentList().iterator();
		while (itr.hasNext()) {
			ContactDocument cd = itr.next();
			if (cd.getFormType() == PayrollFormType.START) {
				itr.remove();
			}
		}
	}

	/**
	 * Creates the list of selectitems of StartStatusType to display on the UI
	 */
	private void createStatusTypeFilterList() {
		try {
			for (StartStatusType status : StartStatusType.values()) {
				statusTypeFilterList.add(new SelectItem(status, status.toString()));
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Listener used for row de-selection, it sets the contact instance to null . */
	public void listenRowUnClicked (UnselectEvent event) {
		try {
			employment = null;
			setupSelectedItem(null);
			setContactDocumentList(null);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenSingleCheck (ValueChangeEvent evt) {
		try {
			Employment contactItem = (Employment) evt.getComponent().getAttributes().get("selectedRow");
			if (contactItem != null) {
				if (employmentsSelected == null) {
					employmentsSelected = new ArrayList<>();
				}
				if (contactItem.getChecked()) {
					employmentsSelected.add(contactItem);
				}
				else if (employmentsSelected.contains(contactItem)) {
					employmentsSelected.remove(contactItem);
				}
				setCheckedForAll(false);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener used for checked and unchecked event on the master check box.
	 * @param evt
	 */
	public void listenCheckedForAll (ValueChangeEvent evt) {
		try {
			employmentsSelected.clear();
			if (getCheckedForAll()) {
				if (filtered && dataTable != null && dataTable.getFilteredData() != null) {
					@SuppressWarnings("unchecked")
					List<Employment> filteredEmps = dataTable.getFilteredData();
					log.debug("filter size: " + filteredEmps.size());
					createSelectedFromFiltered(filteredEmps);
				}
				else {
					for (Employment emp : employmentList) {
						emp.setChecked(true);
						employmentsSelected.add(emp);
					}
				}
			}
			else {
				for (Employment emp : employmentList) {
					emp.setChecked(false);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Set the 'selected' flag on the given item in our list, which should be a
	 * Department.
	 *
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object,
	 *      boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Contact)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(java.lang.Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		try {
			log.debug("id=" + id);
			if (employment != null) {
				employment.getContact().setSelected(false);
			}
			Integer contactId = SessionUtils.getInteger(Constants.ATTR_CONTACT_ID);
			if (id == null && contactId != null) {
				Contact contact = getContactDAO().findById(contactId);
				if (contact != null && contact.getEmployments().size() > 0) {
					employment = contact.getEmployments().get(0); // default if none match project
					if (SessionUtils.getProduction().getType().isAicp()) {
						Project proj = SessionUtils.getCurrentProject();
						for (Employment emp : contact.getEmployments()) {
							if (emp.getProject() == null) {
								employment = emp; // cross-project employment
							}
							else if (proj.equals(emp.getProject())) {
								employment = emp;
								break; // found one specific to project, stop
							}
						}
					}
					id = employment.getId();
				}
			}
			if (id == null) {
				SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, null);
				SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
				employment = null;
				editMode = false;
				newEntry = false;
			}
			else {
				employment = EmploymentDAO.getInstance().findById(id);
				if (employment != null) {
					if (getItemList().indexOf(employment) < 0) {
						// Can happen if switched projects in Commercial production.
						employment = null; // Force use of default entry.
					}
					else {
						if (contactId != null && ! contactId.equals(employment.getContact().getId())) {
							Contact contact = getContactDAO().findById(contactId);
							if (contact != null) {
								if (contact.getEmployments().size() > 0) {
									employment = contact.getEmployments().get(0);
									id = employment.getId();
								}
								else {
									employment = null;
								}
							}
						}
					}
				}
				if (employment == null) {
					id = null;
				}
				SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, id);
				if (employment != null) {
					SessionUtils.put(Constants.ATTR_CONTACT_ID, employment.getContact().getId());
				}
				else {
					SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
				}
			}
			if (employment != null) {
				createDocListOfSelectedContact(employment);
				forceLazyInit();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility Method:
	 * used to put the specified items in the current hibernate session,
	 * to avoid lazyInitializationException
	 */
	private void forceLazyInit() {
		if (employmentList != null){
			for (Employment emp : employmentList) {
				emp.getDepartment().getName();
				emp.getContact().getUser().getLastNameFirstName();
			}
		}
	}

	/** Action method for creating a new packet
	 * @return null navigation string.
	 */
	public String actionDeliverStarts() {
		try {
			log.debug("selected: " + employmentsSelected.size());
			if (employmentsSelected.size() > 0) {
				PopupDeliverBean deliverBean = PopupDeliverBean.getInstance();
				deliverBean.setPacketList(null);
				deliverBean.setDocumentList(null);
				if (deliverBean.getPacketList() == null || deliverBean.getPacketList().isEmpty()) {
					deliverBean.setSelectedDocument(PopupDeliverBean.DOCUMENT_TYPE);
				}
				deliverBean.show(this, ACT_DELIVER, "StatusListBean.DeliverStarts.");
				deliverBean.setMessage(MsgUtils.formatMessage("StatusListBean.DeliverStarts.Text", employmentsSelected.size()));
			}
			else {
				MsgUtils.addFacesMessage("StatusListBean.SelectContact", FacesMessage.SEVERITY_INFO);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		//userPacketName = PopupInputBean.getInstance().getInput().trim();
		switch(action) {
			case ACT_DELIVER:
				res = actionDeliverStartsOk();
				break;
			case ACT_SEND_PENDING:
				res = actionSendPendingDocumentsOk();
				break;
			case ACT_DELETE_ATTACHMENT:
				res = actionDeleteDocsWithAttachmentsOk();
				break;
		}
		return res;
	}

	/**
	 * Method to delete the attachments associated with the given contact
	 * document on Distribution and Review tab. Called via confirmOk, when user
	 * confirms prompt to delete a document that has attachments. (There is no
	 * confirmation for documents that do not have attachments.)
	 *
	 * @return null navigation string.
	 */
	private String actionDeleteDocsWithAttachmentsOk() {
		try {
			if (contactDocToDelete != null) {
				contactDocToDelete = getContactDocumentDAO().refresh(contactDocToDelete); // fixed LIE, r9132
				log.debug("Deleting contact document: " + contactDocToDelete.getId());
				if (deleteRelatedForm(contactDocToDelete) && deleteAttachments(contactDocToDelete)) {
					contactDocumentList.remove(contactDocToDelete);
					if (employment == null) {
						employment = contactDocToDelete.getEmployment();
					}
					// Note: no need to unlock CD, as we are deleting it from database
					getContactDocumentDAO().delete(contactDocToDelete);
					if (employment != null) {
						employment.setPercentageArray(null);
						DocumentUtils.createStatusGraphForEmployment(employment, null);
					}
					initEmploymentDocCounts(getEmploymentList());
					createContactDocumentList();
				}
				else { // we did not delete related doc, so unlock CD
					getContactDocumentDAO().unlock(contactDocToDelete, getvUser().getId());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action Ok method is invoked when the OK button is clicked on the Deliver Popup
	 * @return null navigation string
	 */
	public String actionDeliverStartsOk() {
		try {
			PopupDeliverBean deliverBean = PopupDeliverBean.getInstance();
			ApprovalStatus status = ApprovalStatus.PENDING;
			Contact currContact = SessionUtils.getCurrentContact();
			boolean distribute = deliverBean.getPending();
			if (distribute) {
				status = ApprovalStatus.OPEN;
			}
			// To show single warning message for the WTPA forms.
			Map<String,Boolean> showWarningMap = new HashMap<>();
			if (PopupDeliverBean.START_PACKET_TYPE.equals(deliverBean.getSelectType())) {
				packet = getPacketDAO().findById(Integer.parseInt(deliverBean.getSelectedPacket().toString()));
				if (packet != null) {
					List<Document> documentList = packet.getDocumentList();
					showWarningMap = deliverStarts(documentList, status, currContact, distribute,
							packet.getName(), showWarningMap);
				}
			}
			else if (PopupDeliverBean.DOCUMENT_TYPE.equals(deliverBean.getSelectType())) {
				Document doc = getDocumentDAO().findById(Integer.parseInt(deliverBean.getSelectedDocument().toString()));
				List<Document> documentList = new ArrayList<>();
				documentList.add(doc);
				showWarningMap = deliverStarts(documentList, status, currContact, distribute,
						null, showWarningMap);
			}
			setCheckedForAll(false);
			createContactDocumentList();
			initEmploymentDocCounts(getEmploymentList());

			if (!showWarningMap.isEmpty()) {
				if (showWarningMap.containsKey(FORM_WTPA)) {
					MsgUtils.addFacesMessage("StatusListBean.PendingWtpaDocument", FacesMessage.SEVERITY_WARN);
				} else if (showWarningMap.containsKey(FORM_MODEL_RELEASE)) {
					MsgUtils.addFacesMessage("StatusListBean.PendingModelReleaseDocument", FacesMessage.SEVERITY_WARN);
				}
			}
			employmentsSelected.clear();
			//ApprovalsBean.getInstance().setFirstTime(false);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Deliver a copy of each Document in documentList to the Employment entries
	 * in {@link #employmentsSelected}.
	 *
	 * @param documentList
	 * @param status
	 * @param currContact The Contact of the currently logged-in user.
	 * @param distribute True if the user requested that the documents be
	 *            distributed immediately (i.e., marked as OPEN, not PENDING).
	 * @param packetName The name of the Packet associated with this delivery,
	 *            or null if a single document is being delivered.
	 * @param showWTPAWarning The current state of the caller's showWTPAWarning
	 *            flag, which may be changed from false to true, and which
	 *            becomes the return value of this method.
	 * @return True if showWarningMap was true on entry in value, or if a WTPA or ModelRelease document
	 *         was delivered and the user had requested 'distribute'.
	 */
	private Map<String, Boolean> deliverStarts(List<Document> documentList, ApprovalStatus status,
			Contact currContact, boolean distribute, String packetName, Map<String,Boolean> showWarningMap) {
		try {
			Integer numOfDocs;
			distributedDocumentList = new ArrayList<>();
			for (Employment emp : employmentsSelected){
				Contact empContact = getContactDAO().refresh(emp.getContact());
				//List<Document> contactDocList = getcontactDocumentDAO().findDocByEmploymentId(emp.getId());
				for (Document document : documentList) {
					//boolean duplicate = contactDocList.contains(document); // not checking this anymore
					showWarningMap = deliverStart(emp, document, status, distribute,
							packetName, showWarningMap, empContact);
				}
				numOfDocs = distributedDocumentList.size();
				if (numOfDocs > 0) {
					DoNotification.getInstance().documentDistributed(emp.getContact(), currContact, numOfDocs, distributedDocumentList);
				}
				distributedDocumentList.clear();
				emp.setChecked(false);
				emp.setPercentageArray(null);
			}
			DocumentUtils.createStatusGraphForEmployment(null, employmentList); // TODO could this use employmentsSelected instead?
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return showWarningMap;
	}

	/**
	 * Deliver a single Start document to one Employment instance.
	 *
	 * @param emp The Employment to which the document is being delivered.
	 * @param document The Document to be delivered.
	 * @param status The status of the ContactDocument to be set.
	 * @param distribute True if the user requested that the documents be
	 *            distributed immediately (i.e., marked as OPEN, not PENDING).
	 * @param packetName The name of the Packet associated with this delivery,
	 *            or null if a single document is being delivered.
	 * @param showWarningMap The current state of the caller's showWarningMap
	 *            flag, which may be changed from false to true in map value, and which
	 *            becomes the return value of this method.
	 * @param empContact The Contact of the Employment record to which the
	 *            document is being delivered.
	 * @return True if showWarningMap was true on value with key, or if a WTPA or MODELRELEASE document
	 *         was delivered and the user had requested 'distribute'.
	 */
	private Map<String,Boolean> deliverStart(Employment emp, Document document, ApprovalStatus status,
			boolean distribute, String packetName, Map<String,Boolean> showWarningMap, Contact empContact) {
		try {
			ApprovalStatus docStatus = status;
			PayrollFormType type = PayrollFormType.toValue(document.getName());
			if (type.isWtpa() && distribute) {
				docStatus = ApprovalStatus.PENDING;
				showWarningMap.put(FORM_WTPA, true);
			}
			if (type.isModelRelease() && distribute) {
				docStatus = ApprovalStatus.PENDING;
				showWarningMap.put(FORM_MODEL_RELEASE, true);
			}
			// 1/19/2017 - allow duplicate I9s.
	//		if (! duplicate || type != PayrollFormType.I9) { // don't allow duplicate I9
				Integer id = onboardService.saveContactDocument(emp, empContact, document, packetName, docStatus, null);
				if (docStatus == ApprovalStatus.OPEN && id != null) {
					distributedDocumentList.add(document.getNormalizedName());
				}
	//		}
	//		else if (duplicate && type == PayrollFormType.I9) {
	//			MsgUtils.addFacesMessage("StatusListBean.DeliverDocumentError", FacesMessage.SEVERITY_INFO, PayrollFormType.I9.getName());
	//		}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return showWarningMap;
	}

	/**
	 * Method used to create the start form instances for the
	 * employment who will be distributed with the payroll start.
	 *
	 * @param emp Employment to get the document
	 * @param empContact Employment's contact
	 * @return Id of newly created StartForm, or null if no StartForm was
	 *         created.
	 */
	/*private Integer updateCreateStartForm(Employment emp, Contact empContact) {
		// Either add StartForm, or do nothing.
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
		StartForm priorStartForm = null;
		StartForm newStartForm = null;
		//Project project = SessionUtils.getCurrentProject();
		Integer id = null;
		Date today = CalendarUtils.todaysDate();

		boolean oldValid = contactDocumentDAO.existsNonVoidByEmployment(emp);
		if (! oldValid) {
			// Create a "NEW" start form, with effective-start date of today.
			newStartForm = StartFormService.createStartForm(StartForm.FORM_TYPE_NEW, null, null, empContact,
					emp, emp.getProject(), today, today, null, false, true);
			if (newStartForm != null) {
				//newStartForm.setStatus(ApprovalStatus.OPEN);
				startFormDAO.attachDirty(newStartForm);
				id = newStartForm.getId();
			}
		}
		else {
			priorStartForm = startFormDAO.findOneByNamedQuery(
					StartForm.GET_START_FORM_NON_VOID_BY_EMPLOYMENT_DESCENDING_ORDER, map("employment", emp));
			if (priorStartForm != null) {
				if (priorStartForm.getEffectiveStartDate() == null) {
					return null; // indicates StartForm was not created.
				}
				String type = StartForm.FORM_TYPE_CHANGE; // Default form type
				if (priorStartForm.getEffectiveEndDate() != null && priorStartForm.getEffectiveEndDate().before(today)) {
					// prior (non-VOID) form has expired, so create the new one as "re-hire"
					type = StartForm.FORM_TYPE_REHIRE;
				}
				else if (priorStartForm.getWorkEndDate() != null && priorStartForm.getWorkEndDate().before(today)) {
					// prior (non-VOID) form has expired, so create the new one as "re-hire"
					type = StartForm.FORM_TYPE_REHIRE;
				}
				// Note: leave Effective Start Date null
				newStartForm = StartFormService.createStartForm(type, priorStartForm, null, empContact,
						emp, emp.getProject(), today, null, null, false, true);
				newStartForm.setPriorFormId(priorStartForm.getId());
				startFormDAO.attachDirty(newStartForm);
				id = newStartForm.getId();
			}
		}
		return id;
	}*/

	/** Method used to save the ContactDocument instance to be created
	 * and also looks for document name to set its form type
	 * @param emp
	 * @param currentContact
	 * @param document document to be delivered
	 * @param packetName packet name if document belongs to a packet
	 * @return Integer id of the saved contact document.
	 */
	/*private Integer saveContactDocument(Employment emp, Contact currentContact, Document document, String packetName, ApprovalStatus status) {
		Integer contactDocId = null;
		try {
			contactDocId = null;
			ContactDocument contactDoc = new ContactDocument(currentContact, document, packetName, aicp, emp, status);
			if (document.getMimeType() == MimeType.LS_FORM) {
				if (PayrollFormType.I9.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.I9);
				}
				else if (PayrollFormType.START.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.START);
					// Then update StartForm if necessary
					Integer id = updateCreateStartForm(emp, currentContact);
					if (id == null) { // StartForm was not created...
						MsgUtils.addFacesMessage("StatusListBean.DeliverDocumentError", FacesMessage.SEVERITY_INFO, PayrollFormType.START.getName());
						return null; // do NOT create ContactDocument!
					}
					contactDoc.setRelatedFormId(id);
				}
				else if (PayrollFormType.W4.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.W4);
				}
				else if (PayrollFormType.CA_WTPA.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.CA_WTPA);
				}
				else if (PayrollFormType.NY_WTPA.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.NY_WTPA);
				}
				else if (PayrollFormType.DEPOSIT.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.DEPOSIT);
				}
				else if (PayrollFormType.W9.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.W9);
				}
				else if (PayrollFormType.MTA.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.MTA);
				}
				else if (PayrollFormType.INDEM.getName().equals(document.getName())) {
					contactDoc.setFormType(PayrollFormType.INDEM);
				}
				else {
					contactDoc.setFormType(PayrollFormType.OTHER);
				}
			}
			else {
				contactDoc.setFormType(PayrollFormType.OTHER);
			}
			contactDocId = getcontactDocumentDAO().save(contactDoc);
			if (contactDoc.getFormType() == PayrollFormType.OTHER && contactDoc.getStatus() == ApprovalStatus.OPEN) {
				// Custom docs only, & skip if PENDING.  If path has action=Receive, mark CD approved.
				boolean aicp = SessionUtils.getProduction().getType().isAicp();
				ApprovalPath path = ContactFormBean.findApprovalPath(contactDoc, aicp, null);
				if (path != null && path.getEmployeeAction() == DocumentAction.RCV) {
					contactDoc.setStatus(ApprovalStatus.APPROVED);
					getcontactDocumentDAO().attachDirty(contactDoc);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return contactDocId;
	}*/

	@Override
	public String confirmCancel(int action) {
		try {
			if (action == ACT_DELIVER) {
				for (Employment emp: employmentsSelected) {
					emp.setChecked(false);
					//emp.setPercentageArray(null);
				}
				employmentsSelected.clear();
				setCheckedForAll(false);
			}
			if (action == ACT_SEND_PENDING) {
				unlockPendingDocs();
			}
			return super.confirmCancel(action);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public void changeTab(int priorTab, int currentTab) {
	}

	@Override
	public List<SelectItem> createList(FilterType type) {
		List<SelectItem> list = null;
		switch(type) {
			case DEPT:
				list = createDepartmentList();
				break;
			case NAME:
				list = ApproverUtils.createEmployeeList(getvContact(), null, true, false);
				break;
			default:
				break;
		}
		return list;
	}

	@Override
	public void dropFilter(FilterType type) {

	}

	@Override
	public void listenChange(FilterType type, Object value) {
		switch(type) {
		case DEPT:
//			departmentId = Integer.valueOf((String)value);
//			listenDeptChange();
			break;
		case NAME:
//			setEmployeeAccount((String)value);
//			listenEmployeeChange();
			break;
		case N_A:
			break;
		default:
			break;
		}
	}

//	/**
//	 * ValueChangeListener for Department drop-down list
//	 * Upon entry, {@link #getDepartmentId()} has been set to the new value.
//	 */
// 5/19/16 currently unused
//	protected void listenDeptChange() {
//		try {
//			log.debug("<>");
//			SessionUtils.put(Constants.ATTR_ONBOARDING_DEPT, getDepartmentId());
//			SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_CONTACT, null);
//			createDocumentListByFilteredDepartment();
//		}
//		catch(Exception e) {
//			EventUtils.logError(e);
//			MsgUtils.addGenericErrorMessage();
//		}
//	}

	/**
	 * Creates the filtered list of documents according to the selected department
	 */
//	private void createDocumentListByFilteredDepartment() {
//		if (employment != null) {
//			if (getDepartmentId() != 0) {
//				clearSelectedRow(employment);
//				//contactList = getContactDAO().findByDepartmentId(getDepartmentId(), SessionUtils.getProduction(), SessionUtils.getCurrentProject());
//				Department dept = DepartmentDAO.getInstance().findById(
//						getDepartmentId());
//				log.debug("dept id fethced >>>>>>>>>>>>>> "+dept.getId());
//				Map<String, Object> values = new HashMap<>();
//				if (SessionUtils.getProduction().getType().isAicp()) {
//					values.put("production", SessionUtils.getProduction());
//					values.put("project", SessionUtils.getCurrentProject());
//					values.put("department", dept);
//					employmentList = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_START_STATUS_BY_PRODUCTION_PROJECT_DEPARTMENT, values);
//				}
//				else {
//					values.put("production", SessionUtils.getProduction());
//					values.put("department", dept);
//					employmentList = EmploymentDAO.getInstance().findByNamedQuery(Employment.GET_EMPLOYMENT_BY_PRODUCTION_DEPARTMENT, values);
//				}
//			} else {
//				clearSelectedRow(employment);
//				employmentList = createEmploymentList();
//			}
//			if (!employmentList.isEmpty()) {
//				employment = employmentList.get(0);
//				createDocListOfSelectedContact(employment);
//			}
//			else {
//				setContactDocumentList(null);
//			}
//		}
//	}

//	/**
//	 * ValueChangeListener for Employee drop-down list
//	 * Upon entry, {@link #getEmployeeAccount()} has been set to the new value.
//	 */
// 5/19/16 currently unused
//	protected void listenEmployeeChange() {
//		try {
//			log.debug("<>");
//			SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYEE, getEmployeeAccount());
//			SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_CONTACT, null);
//			employmentList.clear();
//			//createDocumentListByFilteredEmployee();
//		}
//		catch(Exception e) {
//			EventUtils.logError(e);
//			MsgUtils.addGenericErrorMessage();
//		}
//	}

	/**
	 * Creates list of documents for the selected employee from the name filter
	 */
	/*private void createDocumentListByFilteredEmployee() {
		if (getEmployeeAccount() != null) {
			clearSelectedRow(contact);
			contact = getContactDAO().findByAccountNumAndProduction(getEmployeeAccount(), SessionUtils.getProduction());
			contactList = new ArrayList<>();
			contactList.add(contact);
			createDocListOfSelectedContact(contact);
		}
		else {
			clearSelectedRow(contact);
			contactList = createContactList();
			contact = contactList.get(0);
			createDocListOfSelectedContact(contact);
		}
	}*/

	/** Utility Method,
	 * Used to set the first contact of the filtered list with the
	 * updated state that is 'selected'.
	 * It also create the corresponding document list
	 * @param contact
	 */
	private void createDocListOfSelectedContact(Employment contact) {
		try {
			RowState state = new RowState();
			state.setSelected(true);
			stateMap.put(contact, state);
			createContactDocumentList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Create the list of Department`s for the drop-down list. For a Production
	 * Approver the list includes all Departments; for a departmental approver,
	 * it includes just those Departments for which the Contact is an approver.
	 * <p>
	 * An "All" entry is added at the top of the list if there is more than one
	 * department.
	 *
	 * @return the list of Departments as SelectItem`s, where the value field is
	 *         the Department.id field.
	 */
	protected List<SelectItem> createDepartmentList() {
		try {
			ArrayList<SelectItem> deptList = (ArrayList<SelectItem>) DepartmentUtils.getDepartmentDataAdminDL();
				deptList.add(0, new SelectItem(0, "All"));
			return deptList;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method creates the array of color percentages used on the status graph.
	 *
	 * @param id Employment id
	 * @return array of four percentages (totaling 100) of documents for the
	 *         given Employment record, corresponding to the four different
	 *         colored segments of the status graph: completed, open, submitted,
	 *         and pending.
	 */
	/*public Integer[] createStatusGraph(Integer employmentId) {
		DocumentStatusCount statusGraph = null;
		try {
			log.debug("");
			statusGraph = getContactDocumentDAO().findDocStatusGraphForEmployment(employmentId);
			, SessionUtils.getNonSystemProduction(), viewProject
		//	int allDocs = ContactDocumentDAO.getInstance().findDocCountByEmploymentId(employmentId, viewProject);
			if (statusGraph != null) {
				int greenCount =  (statusGraph.getGreenCount() == null  ? 0 : statusGraph.getGreenCount().intValue());
				int yellowCount = (statusGraph.getYellowCount() == null ? 0 : statusGraph.getYellowCount().intValue());
				int redCount =    (statusGraph.getRedCount() == null    ? 0 : statusGraph.getRedCount().intValue());
				int blackCount =  (statusGraph.getBlackCount() == null  ? 0 : statusGraph.getBlackCount().intValue());
				int allDocs = greenCount + yellowCount + redCount + blackCount;

				if (allDocs != 0) {
					greenCount = findColorPercentage(allDocs, greenCount);
					redCount = findColorPercentage(allDocs, redCount);
					yellowCount = findColorPercentage(allDocs, yellowCount);
					blackCount = findColorPercentage(allDocs, blackCount);
					int diff = 100 - (blackCount + greenCount + redCount + yellowCount);
					if (diff != 0) {
						// rounding errors can get us here; add diff to any non-zero value
						if (blackCount != 0) {
							blackCount += diff;
						}
						else if (redCount != 0) {
							redCount += diff;
						}
						else if (yellowCount != 0) {
							yellowCount += diff;
						}
					}
					Integer percentageArray[] = { greenCount, redCount, yellowCount, blackCount };
					return percentageArray;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return new Integer[] {0, 0, 0, 0};
	}*/

	/**
	 * Called by the framework when any one of the column filter values
	 * is changed by the user.
	 * @param evt Event created by the framework.
	 */
	public void onFilterChange(TableFilterEvent evt) {
		try {
			DataTable dt = (DataTable) evt.getComponent();
			Column col = dt.getColumns().get(1); // "Name" column
			log.debug("column value: "+col.getFilterValue());
			@SuppressWarnings("unchecked")
			List<Employment> filteredEmps = dt.getFilteredData();
			if (filteredEmps != null) {
				log.debug(filteredEmps.size());
				if (getCheckedForAll()) {
					createSelectedFromFiltered(filteredEmps);
				}
				if (filteredEmps.size() == 0) {
					selectRow(null);
				}
				filtered = true;
				dataTable = dt;
			}
			else { // all filters have been cleared
				employmentsSelected.clear();
				dataTable = null;
				filtered = false;
				for (Employment emp : employmentList) {
					if (emp.getChecked()) {
						employmentsSelected.add(emp);
					}
				}
				log.debug("Employments Selected = " + employmentsSelected.size());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Update the list of "selected" (checked) Employment entries, based on
	 * what's still showing in the filtered list.
	 *
	 * @param filteredEmps List of employments being displayed in the dataTable
	 *            after filtering by one or more column filters.
	 */
	private void createSelectedFromFiltered(List<Employment> filteredEmps) {
		employmentsSelected.clear();
		for (Employment emp : employmentList) {
			emp.setChecked(false);
		}
		for (Employment emp : filteredEmps) {
			emp.setChecked(true);
			employmentsSelected.add(emp);
		}
	}

	/**
	 * Method to delete the contact documents on Distribution and Review tab;
	 * called from JSF. Field {@link #contactDocToDelete} has been set via
	 * f:setPropertyActionListener in xhtml.
	 *
	 * @return null navigation string.
	 */
	public String actionDeleteContactDoc() {
		try {
			if (contactDocToDelete != null) {
				log.debug("contactDocToDelete: " + contactDocToDelete.getId());
//				if (AuthorizationBean.getInstance().hasPermission(Permission.MANAGE_START_DOCS)) {
				contactDocToDelete = getContactDocumentDAO().refresh(contactDocToDelete);
				boolean isLocked = ContactDocumentDAO.getInstance().lock(contactDocToDelete, getvUser());
				PopupBean bean = PopupBean.getInstance();
				if (! isLocked) {
					bean.show(null, 0, "StatusListBean.DocumentLocked.Title",
							"StatusListBean.DocumentLocked." + "Text", "Confirm.OK", null); // no cancel button
					log.debug("Delete prevented: locked by user #" + (contactDocToDelete != null ? contactDocToDelete.getLockedBy() : "Document Deleted"));
					return null;
				}
				if (contactDocToDelete != null) {
					if (contactDocToDelete.getAttachments() != null && (! contactDocToDelete.getAttachments().isEmpty())) {
						bean.show(this, ACT_DELETE_ATTACHMENT, "StatusListBean.DeleteAttachment."); // no cancel buttonmessage
						bean.setMessage(MsgUtils.formatMessage("StatusListBean.DeleteAttachment.Text", contactDocToDelete.getAttachments().size()));
						log.debug("");
						return null;
					}
					log.debug("Deleting contact document: " + contactDocToDelete.getId());
					if (deleteRelatedForm(contactDocToDelete)) {
						contactDocumentList.remove(contactDocToDelete);
						if (employment == null) {
							employment = contactDocToDelete.getEmployment();
						}
						// Note: no need to unlock CD, as we are deleting it from database
						getContactDocumentDAO().delete(contactDocToDelete);
						if (employment != null) {
							employment.setPercentageArray(null);
							DocumentUtils.createStatusGraphForEmployment(employment, null);
						}
						initEmploymentDocCounts(getEmploymentList());
						createContactDocumentList();
					}
					else { // we did not delete related doc, so unlock CD
						getContactDocumentDAO().unlock(contactDocToDelete, getvUser().getId());
					}
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
	 * Delete the standard form record associated with the given
	 * ContactDocument.
	 *
	 * @param contactDoc The relevant ContactDocument -- usually one that is
	 *            about to be deleted.
	 * @return True if no form existed, or the form was ok to delete; false
	 *         indicates the form is not allowed to be deleted, e.g., a
	 *         StartForm with related timecards.  In the 'false' case, a
	 *         message has been generated for the user.
	 */
	@SuppressWarnings("rawtypes")
	public static boolean deleteRelatedForm(ContactDocument contactDoc) {
		boolean deleted = true;
		try {
			PayrollFormType formType = contactDoc.getFormType();
			Form form = null;
			Integer formId;
			if ((formId = contactDoc.getRelatedFormId()) != null) {
				// retrieve the form to be deleted ...
				if (formType == PayrollFormType.MODEL_RELEASE) {
					// LS-3154 Get the form to delete.
					form = FormService.getInstance().findById(formId, PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
				}
				else if (formType.isFormStateW4Type()) {
					// State W4s use onboarding API. LS-3576
					form = FormService.getInstance().findById(formId, PayrollFormType.CA_W4.getApiFindUrl(), FormStateW4.class);
				}
				else {
					// This handles most form types by using the BaseDAO.findById method
					form = (Form)FormW4DAO.getInstance().findById(contactDoc.getFormType().getClassName(),formId);
				}
				if (form != null) {
					// Have a form; see if there's a StartForm with timecards tied to it
					StartForm sf = null;
					if (formType == PayrollFormType.START) {
						sf = (StartForm)form;
					}
					else if (formType == PayrollFormType.MODEL_RELEASE) {
						sf = ((FormModelRelease)form).getStartForm();
					}
					if (sf != null) {
						if (WeeklyTimecardDAO.getInstance().existsTimecardsForStartForm(sf.getId())) {
							// Don't allow delete if the StartForm has associated timecards.
							log.debug("has start form with timecards");
							MsgUtils.addFacesMessage("StartForm.PayrollStartHasTimecards", FacesMessage.SEVERITY_ERROR, "deleted");
							form = null; // prevent delete attempt
							deleted = false;
						}
					}
				}
				if (form != null) { // Have the related form, and it's ok to delete it...
					log.debug("deleting related " + contactDoc.getFormType() + " form, id=" + formId);
					// LS-3154 Delete using TTC)-Onboarding API
					if(formType == PayrollFormType.MODEL_RELEASE) {
						FormService.getInstance().delete(formId, PayrollFormType.MODEL_RELEASE.getApiDeleteUrl());
					}
					else if (formType.isFormStateW4Type()) { // LS-3576
						FormService.getInstance().delete(formId, PayrollFormType.CA_W4.getApiDeleteUrl());
					}
					else {
						ContactDocumentDAO.getInstance().delete(form);
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return deleted;
	}

	/**
	 * Delete the attachments associated with the given
	 * ContactDocument.
	 *
	 * @param contactDoc The relevant ContactDocument -- usually one that is
	 *            about to be deleted.
	 * @return True if no attachment existed, or the attachment was ok to delete; false
	 *         indicates the attachment is not allowed to be deleted, e.g., a
	 *         comtact document with related attachments. In the 'false' case, a
	 *         message has been generated for the user.
	 */
	public static boolean deleteAttachments(ContactDocument contactDoc) {
		boolean deleted = true;
		try {
			if (contactDoc != null && contactDoc.getAttachments() != null) {
				contactDoc = ContactDocumentDAO.getInstance().refresh(contactDoc);
				AttachmentDAO attachmentDAO = AttachmentDAO.getInstance();
				Iterator<Attachment> itr = contactDoc.getAttachments().iterator();
				while (itr.hasNext()) {
					Attachment atc = itr.next();
					itr.remove();
					attachmentDAO.delete(atc);
				}
				if (contactDoc.getAttachments() != null && (! contactDoc.getAttachments().isEmpty())) {
					deleted = false;
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return deleted;
	}


	/**
	 * Method used to refresh the view on the employee status screen and also
	 * sets the state of the data table accordingly.
	 */
	public void refreshView() {
		try {
			employmentList = createEmploymentList(); // create new list according to check box value
			forceLazyInit();
			clearSelectedRow(employment); // remove previous state
			if (employmentList != null && employmentList.size() > 0) {
				selectRow(employmentList.get(0));// set new selected Employment and state
				createContactDocumentList(); // refresh list according to new employment
			}
			else {
				selectRow(null);
			}
//			createEmploymentList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Utility method,
	 * used to clear the row selection on the UI
	 * @param contact the contact whose state changes
	 */
	private void clearSelectedRow(Employment contact) {
		RowState state = new RowState();
		state.setSelected(false);
		stateMap.put(contact, state);
		stateMap.remove(contact);
	}

	/**
	 * Method used to set the "selected" (highlighted) row state for the passed
	 * employment
	 *
	 * @param employment Employment entry from the data table list to be
	 *            highlighted.
	 */
	private void selectRow(Employment emp) {
		employment = emp;
		if (employment == null) {
			getContactDocumentList().clear();
		}
		else {
			RowState state = new RowState();
			state.setSelected(true);
			stateMap.put(employment, state);
		}
	}

	/** This method returns the ContactDAO instance
	 * @return ContactDAO
	 */
	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	/** Utility method
	 * @return PacketDAO instance
	 */
	public PacketDAO getPacketDAO() {
		if (packetDao == null) {
			packetDao = PacketDAO.getInstance();
		}
		return packetDao;
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
		if (documentDao == null) {
			documentDao = DocumentDAO.getInstance();
		}
		return documentDao;
	}

	/** Utility method
	 * See {@link #approverAuditDAO}.
	 */
	public PayrollPreferenceDAO getPayrollPreferenceDAO() {
		if (payrollPreferenceDAO == null) {
			payrollPreferenceDAO = PayrollPreferenceDAO.getInstance();
		}
		return payrollPreferenceDAO;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List getItemList() {
		return getEmploymentList();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getComparator() {
		return null;
	}

	/** Action method used to change the CD's status from Pending to Open
	 * @return null
	 */
	public String actionSendPendingDocuments() {
		try {
			log.debug("selected: " + employmentsSelected.size());
			lockedDocCount = 0; // NPE's due to this not set, in actionSendPendingDocumentsOk() l. 1724
			count = 0; // Count of docs that returns false.
			// Map is used to send notification.
			mapOfEmpPendingDocsToSend = new HashMap<>();
			// To hold the pending contact documents for the current employment in loop.
			List<ContactDocument> contactDocsToSend = null;

			if (employmentsSelected.size() > 0) {
				for (Employment emp : employmentsSelected) {
					contactDocsToSend = new ArrayList<>();
					List<ContactDocument> contactDocList = new ArrayList<>();
					contactDocList = ContactDocumentDAO.getInstance().findByProperty("employment", emp);

					for (ContactDocument cd : contactDocList) {
						if (cd.getStatus().equals(ApprovalStatus.PENDING)) {
							FormWTPA form = null;
							FormModelRelease mdForm = null;
							// For any WTPA Form
							if (cd.getFormType().isWtpa()) {
								if (cd.getRelatedFormId() != null) {
									form = FormWtpaDAO.getInstance().findById(cd.getRelatedFormId());
									if (form != null) {
										if (form.getHasRequiredFields(cd.getFormType())) {
											boolean isLocked = ContactDocumentDAO.getInstance().lock(cd, getvUser());
											if (isLocked) {
												contactDocsToSend.add(cd);
												log.debug("Contact Doc : " + cd.getId());
											}
											else {
												lockedDocCount++;
											}
										}
										else {
											count++;
										}
									}
								}
								else {
									count++;
								}
							}

							// For  Model release
							else if (cd.getFormType().isModelRelease()) {
								if (cd.getRelatedFormId() != null) {
									mdForm = (FormModelRelease)FormService.getInstance().findById(cd.getRelatedFormId(), PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
									if (mdForm != null) {
										if (cd.getEmployerSignature()!=null){
											boolean isLocked = ContactDocumentDAO.getInstance().lock(cd, getvUser());
											if (isLocked) {
												contactDocsToSend.add(cd);
												log.debug("Contact Doc : " + cd.getId());
											}
											else {
												lockedDocCount++;
											}
										}
										else {
											mrcount++;
										}
									}
								}
								else {
									mrcount++;
								}
							}

							// For Other Forms
							else {
								boolean isLocked = ContactDocumentDAO.getInstance().lock(cd, getvUser());
								if (isLocked) {
									contactDocsToSend.add(cd);
									log.debug("Contact Doc : " + cd.getId());
								}
								else {
									lockedDocCount++;
								}
							}
						}
					}
					// Put pending documents for the employment in map.
					if (contactDocsToSend != null && contactDocsToSend.size() > 0) {
						mapOfEmpPendingDocsToSend.put(emp, contactDocsToSend);
					}
				}
				if (mapOfEmpPendingDocsToSend.size() == 0) {
					if (count > 0) {
						MsgUtils.addFacesMessage("StatusListBean.SendPendingDocsInfo", FacesMessage.SEVERITY_INFO);
					}
					if(mrcount>0) {
						MsgUtils.addFacesMessage("StatusListBean.SendPendingDocsModelReleaseInfo", FacesMessage.SEVERITY_INFO);
					}
					MsgUtils.addFacesMessage("StatusListBean.SendPendingNoDocuments", FacesMessage.SEVERITY_INFO);
				}
				else {
					PopupBean bean = PopupBean.getInstance();
					bean.show(this, ACT_SEND_PENDING, "StatusListBean.SendPendingDocs.");
				}
			}
			else {
				MsgUtils.addFacesMessage("StatusListBean.SendPendingDocsMessage", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action Ok method is invoked (via confirmOk) when the SEND button is
	 * clicked on the Send Pending documents Popup (Confirmation Popup).
	 *
	 * @return null navigation string
	 */
	private String actionSendPendingDocumentsOk() {
		try {
			log.info("size of map: " + mapOfEmpPendingDocsToSend.size());
			Contact currContact = SessionUtils.getCurrentContact();

			if (mapOfEmpPendingDocsToSend != null && mapOfEmpPendingDocsToSend.size() > 0) {
				log.info("Num of emp for notification: " + mapOfEmpPendingDocsToSend.size());
				contactDocumentList.clear();
				//boolean aicp = SessionUtils.getProduction().getType().isAicp();

				for (Employment emp : mapOfEmpPendingDocsToSend.keySet()) {
					distributedDocumentList = new ArrayList<>();
					List<ContactDocument> contactDocsToSend = mapOfEmpPendingDocsToSend.get(emp);
					for (ContactDocument cd : contactDocsToSend) {
						cd = getContactDocumentDAO().refresh(cd);
						cd.setStatus(ApprovalStatus.OPEN);
						if (cd.getFormType() == PayrollFormType.OTHER) {
							//ApprovalPath path = ContactFormBean.findApprovalPath(cd, aicp, null);
							//if (path != null && path.getEmployeeAction() == DocumentAction.RCV) {
							if (cd.getDocument() != null && cd.getDocument().getEmployeeAction() == DocumentAction.RCV) {
								cd.setStatus(ApprovalStatus.APPROVED);
							}
							else {
								cd.setStatus(ApprovalStatus.OPEN);
							}
						}
						distributedDocumentList.add(cd.getDocument().getNormalizedName());
						cd.setLockedBy(null);
						cd = getContactDocumentDAO().merge(cd);
					}
					if (contactDocsToSend != null && contactDocsToSend.size() > 0) {
						DoNotification.getInstance().documentDistributed(emp.getContact(), currContact, contactDocsToSend.size(), distributedDocumentList);
					}
					emp.setChecked(false);
					emp.setPercentageArray(null);
					distributedDocumentList.clear();
				}
				DocumentUtils.createStatusGraphForEmployment(null, employmentsSelected);
				setCheckedForAll(false);
				createContactDocumentList();
				employmentsSelected.clear();
				mapOfEmpPendingDocsToSend.clear();
			}
			if (count > 0) {
				log.info("count " + count);

				MsgUtils.addFacesMessage("StatusListBean.SendPendingDocsInfo", FacesMessage.SEVERITY_INFO);
				return null;
			}
			if (lockedDocCount > 0) {
				log.info("lockedDocCount " + lockedDocCount);
				PopupBean.getInstance().show(null, 0,
						"ContactFormBean.DocumentLocked.Title",
						"ContactFormBean.DocumentLocked." + "Submit." + "Text",
						"Confirm.OK", null); // no cancel button
				return null;
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Value change listener for Document drop-down.
	 * @param evt
	 */
	public void listenDocumentChange(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			log.debug("Document id: " + id);
			selectedDocumentId = id;
			SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_DOCUMENT_ID, id);
			employmentsSelected.clear();
			setCheckedForAll(false);
			/*createEmploymentList();
			createContactDocumentList();*/
			refreshView();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenRequireAttention(ValueChangeEvent evt) {
		log.debug("");
		try {
			if (evt.getNewValue() != null) {
				docsRequireAttn = (boolean) evt.getNewValue();
				refreshView();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * The method used to create the Document drop down list for filtering the
	 * display of distributed forms.
	 *
	 * @return {@link documentListDL}
	 */
	private List<SelectItem> createDocumentListDL() {
		try {
			documentListDL = createDocumentList();
			documentListDL.add(0, new SelectItem(0, "All"));
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return documentListDL;
	}

	/**
	 * Create a SelectItem list for the drop-down in both the document filter,
	 * and the document delivery pop-up.
	 *
	 * @return List<SelectItem> of the names of all the document types in the
	 *         current production.
	 */
	/*package*/ static List<SelectItem> createDocumentList() {
		List<SelectItem> documentList = new ArrayList<>();
		List<Document> list = new ArrayList<>();
		Production production = SessionUtils.getProduction();
		// Refresh PayrollPreference object to prevent LIE
		PayrollPreference pf = PayrollPreferenceDAO.getInstance().refresh(production.getPayrollPref());
		PayrollService ps = pf.getPayrollService();

		list = DocumentDAO.getInstance().findAllOnboardingDocuments(production);
		if (list != null) {
			if (AuthorizationBean.getInstance().isAdmin()) { // if Ls Admin add the standard I9 to the list for self distribution
				log.debug("is admin");
				// Add all NON-standard docs (e.g., uploaded ones) that are in the SYSTEM Onboarding folder.
				//Folder sysFolder = ApplicationUtils.getSystemProduction().getRootFolder();
				//sysFolder = FileRepositoryUtils.findFolder(Constants.ONBOARDING_FOLDER, sysFolder);
				Folder sysFolder = FileRepositoryUtils.findOnboardingFolder(ProductionDAO.getSystemProduction());
				Map<String, Object> valueMap = new HashMap<>();
				valueMap.put("folderId", sysFolder.getId());
				List<Document> sysDocs = DocumentDAO.getInstance().
						findByNamedQuery(Document.GET_NON_STANDARD_DOCUMENT_BY_FOLDER_ID, valueMap);
				list.addAll(sysDocs);
			}
			boolean canada = production.getType().isCanadaTalent();

			boolean hideModelRelease = ! FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM);
			// LS-2038 - Do not add Model Release to tours, hybrid, Canadian or Non-Team productions.
			hideModelRelease |= ps == null || !ps.getTeamPayroll() || production.getType().isTours() || production.getType().isCanadaTalent() || pf.getIncludeTouring();

			// Hide Payroll Start if 'use model release' option selected and FF is on. LS-4502; LS-4757
			boolean hideStartForm = (! hideModelRelease) && (pf.getUseModelRelease() && FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS));

			boolean hideUDA = !FF4JUtils.useFeature(FeatureFlagType.TTCO_SHOW_UDA);

			for (Document doc : list) {
				if (canada && // LS-1632 Hide these forms for Canada
						doc.getFormType().hideForCanada()) {
					continue;
				}
				if (doc.getFormType() == PayrollFormType.START && hideStartForm) {
					continue; // LS-4502 Hide Payroll Start if using Model Release instead
				}
				if (doc.getFormType() == PayrollFormType.UDA_INM && hideUDA) {
					continue;
				}
				if (doc.getFormType() == PayrollFormType.MODEL_RELEASE && hideModelRelease) {
					// LS-2038 - Do not add Model Release to tours, hybrid, Canadian or Non-Team productions.
					continue;
				}
				documentList.add(new SelectItem(doc.getId(), doc.getNormalizedName()));
			}
		}
		return documentList;
	}

	/**
	 * Method used to find all the document chains for which the current contact
	 * is an approver (or an approver in their approval paths).
	 */
	private void findDocumentChainsForCurrentContact() {
		log.debug("");
		currentContactChainMap = findDocumentChainsForCurrentContact(SessionUtils.getCurrentContact(), SessionUtils.getNonSystemProduction());
	}

	/**
	 * Method used to find all the document chains for which the given contact
	 * is an approver (or an approver in their approval paths).
	 *
	 * @param currContact
	 * @param production
	 * @return
	 */
	public static Map<Project, Map<DocumentChain, List<Department>>> findDocumentChainsForCurrentContact(Contact currContact, Production production) {
		log.debug("");
		Map<Project, Map<DocumentChain, List<Department>>> currContactChainMap = new HashMap<>();
		try {
			if (production != null) {
				List<ApprovalPath> approvalPathList;
				currContact = ContactDAO.getInstance().refresh(currContact);
				if (production.getType().isAicp()) {
					// Find all approval paths for current production(Commercial) and project.
					Set<Project> projectSet = new HashSet<>();
					currContact.setProduction(ProductionDAO.getInstance().refresh(currContact.getProduction()));
					if (AuthorizationBean.getInstance().hasPermission(currContact, Permission.VIEW_ALL_PROJECTS)) {
						projectSet = production.getProjects();
					}
					else {
						projectSet.add(SessionUtils.getCurrentOrViewedProject());
					}
					for (Project project : projectSet) {
						Map<String, Object> values = new HashMap<>();
						values.put("project", project);
						approvalPathList = new ArrayList<>();
						approvalPathList = ApprovalPathDAO.getInstance().findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, values);
						currContactChainMap = fillChainMapForCurrentContact(currContact, currContactChainMap, approvalPathList, project);
					}
				}
				else {
					// Find all approval paths for current production(non-Commercial).
					approvalPathList = ApprovalPathDAO.getInstance().findByProperty("production", production);
					currContactChainMap = fillChainMapForCurrentContact(currContact, currContactChainMap, approvalPathList, null);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return currContactChainMap;
	}

	/** Method to fill the current contact's chain map.
	 * @param currContact
	 * @param currContactChainMap
	 * @param approvalPathList
	 * @param project
	 * @return
	 */
	private static Map<Project, Map<DocumentChain, List<Department>>> fillChainMapForCurrentContact(Contact currContact,
			Map<Project, Map<DocumentChain, List<Department>>> currContactChainMap, List<ApprovalPath> approvalPathList, Project project) {
		try {
			Map<DocumentChain, List<Department>> documentChainMap = new HashMap<>();
			for (ApprovalPath path : approvalPathList) {
				// Iterate over the paths and check if the current contact belongs to them or not.
				Map<List<Department>, Boolean> appoverChainMap = new HashMap<>();
				appoverChainMap = ApproverUtils.isContactInPath(path, currContact);
				List<Department> departmentList = null;
				boolean app = false;
				if (appoverChainMap.containsKey(null) && appoverChainMap.get(null)) {
					log.debug("Prod Approver for current path");
					app = true;
				}
				else if (appoverChainMap.size() > 0 && appoverChainMap.containsValue(true)) {
					log.debug("Dept Approver for current path");
					for (List<Department> deptList : appoverChainMap.keySet()) {
						if (appoverChainMap.get(deptList)) {
							app = true;
							departmentList = new ArrayList<>();
							departmentList.addAll(deptList);
						}
					}
				}
				// get Document chain set for that path and add them to a map.
				if (app) {
					for (DocumentChain dc : path.getDocumentChains()) {
						if (departmentList != null && departmentList.size() > 0) {
							documentChainMap.put(dc, departmentList);
						}
						else {
							documentChainMap.put(dc, null);
						}
					}
				}
			}
			currContactChainMap.put(project, documentChainMap);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return currContactChainMap;
	}

	/**
	 * Method used to find all the document chains for which the current contact
	 * is an editor in their approval paths.
	 */
	private void findDocumentChainEditorForCurrentContact() {
		log.debug("");
		currContactChainEditorMap = findDocumentChainEditorForCurrentContact(SessionUtils.getCurrentContact(), SessionUtils.getNonSystemProduction());
	}

	/** Method to fill the map with DocumentChains project wise,
	 * where current contact is an editor for those chains.
	 * @param currContact
	 * @param currContactChainMap
	 * @param approvalPathList
	 * @param project
	 * @return
	 */
	public static Map<Project, List<DocumentChain>> findDocumentChainEditorForCurrentContact(Contact currContact, Production production) {
		log.debug("");
		Map<Project, List<DocumentChain>> documentChainEditorMap = new HashMap<>();
		try {
			if (production != null) {
				List<ApprovalPath> approvalPathList;
				List<DocumentChain> documentChainList;
				if (production.getType().isAicp()) {
					documentChainList = new ArrayList<>();
					// Find all approval paths for current production(Commercial) and project.
					Set<Project> projectSet = new HashSet<>();
					if (AuthorizationBean.getInstance().hasPermission(currContact, Permission.VIEW_ALL_PROJECTS)) {
						projectSet = production.getProjects();
					}
					else {
						projectSet.add(SessionUtils.getCurrentOrViewedProject());
					}
					for (Project project : projectSet) {
						Map<String, Object> values = new HashMap<>();
						values.put("project", project);
						approvalPathList = new ArrayList<>();
						approvalPathList = ApprovalPathDAO.getInstance().findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, values);
						for (ApprovalPath path : approvalPathList) {
							if (path.getEditors() != null && (! path.getEditors().isEmpty()) &&
									path.getEditors().contains(currContact)) {
								for (DocumentChain dc : path.getDocumentChains()) {
									documentChainList.add(dc);
								}
							}
						}
						documentChainEditorMap.put(project, documentChainList);
					}
				}
				else {
					documentChainList = new ArrayList<>();
					approvalPathList = new ArrayList<>();
					// Find all approval paths for current production(non-Commercial).
					approvalPathList = ApprovalPathDAO.getInstance().findByProperty("production", production);
					for (ApprovalPath path : approvalPathList) {
						if (path.getEditors() != null && (! path.getEditors().isEmpty()) &&
								path.getEditors().contains(currContact)) {
							for (DocumentChain dc : path.getDocumentChains()) {
								documentChainList.add(dc);
							}
						}
					}
					documentChainEditorMap.put(null, documentChainList);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return documentChainEditorMap;
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
				refreshView();
				//StatusListBean.getInstance().refreshView();
				//EmpDetailBean.getInstance().refreshView();
			}
			if (aicp && ! getShowAllProjects()) {
				viewProject = SessionUtils.getCurrentProject();
			}
			else {
				viewProject = null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to clear current contact's chain set.
	 */
	public void clearCurrentContactChainMap() {
		try {
			log.debug("");
			contactInPath.clear();
			currentContactChainMap = null;
			currContactChainEditorMap = null;
			findDocumentChainsForCurrentContact();
			findDocumentChainEditorForCurrentContact();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to clear current contact's chain set.
	 */
	public void clearStatusGraphs() {
		try {
			log.debug("");
			employmentStatusGraph = null;
			getEmploymentStatusGraph();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
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
		log.debug("");
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
		log.debug("");
		try {
			unlockPendingDocs();
			if (contactDocToDelete != null && getvUser() != null) {
				contactDocToDelete = getContactDocumentDAO().refresh(contactDocToDelete); // prevent "non-unique object" failure in logout case
				if (contactDocToDelete != null && contactDocToDelete.getLockedBy() != null
						 && getvUser().getId().equals(contactDocToDelete.getLockedBy())) {
					log.debug("dispose");
					getContactDocumentDAO().unlock(contactDocToDelete, getvUser().getId());
				}
			}
		}
		catch (Exception e) {
			log.error("Exception: ", e);
		}
	}

	/**
	 * Method used to unlock pending contact documents.
	 */
	private void unlockPendingDocs() {
		try {
			if (mapOfEmpPendingDocsToSend != null) {
				for (Employment emp : mapOfEmpPendingDocsToSend.keySet()) {
					List<ContactDocument> contactDocsToSend = mapOfEmpPendingDocsToSend.get(emp);
					for (ContactDocument cd : contactDocsToSend) {
						cd = getContactDocumentDAO().refresh(cd);
						if (cd != null && cd.getLockedBy() != null && getvUser().getId().equals(cd.getLockedBy())) {
							log.debug("dispose");
							cd.setLockedBy(null);
							getContactDocumentDAO().attachDirty(cd);
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

	/**See {@link #employmentList}. */
	public List<Employment> getEmploymentList() {
		if(employmentList == null){
			employmentList = createEmploymentList();
			forceLazyInit();
		}
		return employmentList;
	}

	/**See {@link #employmentList}. */
	public void setEmploymentList(List<Employment> employmentList) {
		this.employmentList = employmentList;
	}

	/**See {@link #stateMap}. */
	@Override
	public RowStateMap getStateMap() {
		return stateMap;
	}
	/**See {@link #stateMap}. */
	@Override
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/**See {@link #contactDocumentList}. */
	public List<ContactDocument> getContactDocumentList() {
		if (contactDocumentList == null) {
			createContactDocumentList();
		}
		return contactDocumentList;
	}
	/**See {@link #contactDocumentList}. */
	public void setContactDocumentList(List<ContactDocument> contactDocumentList) {
		this.contactDocumentList = contactDocumentList;
	}

	/**See {@link #checkedForAll}. */
	public boolean getCheckedForAll() {
		return checkedForAll;
	}
	/**See {@link #checkedForAll}. */
	public void setCheckedForAll(boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/**See {@link #checkedDocument}. */
	public Document getCheckedDocument() {
		return checkedDocument;
	}
	/**See {@link #checkedDocument}. */
	public void setCheckedDocument(Document checkedDocument) {
		this.checkedDocument = checkedDocument;
	}

	/**See {@link #documentToRemove}. */
	public Document getDocumentToRemove() {
		return documentToRemove;
	}
	/**See {@link #documentToRemove}. */
	public void setDocumentToRemove(Document documentToRemove) {
		this.documentToRemove = documentToRemove;
	}

	/**See {@link #packet}. */
	public Packet getPacket() {
		return packet;
	}
	/**See {@link #packet}. */
	public void setPacket(Packet packet) {
		this.packet = packet;
	}

//	/** See {@link #departmentId}. */
//	public Integer getDepartmentId() {
//		return departmentId;
//	}
//	/** See {@link #departmentId}. */
//	public void setDepartmentId(Integer deptId) {
//		departmentId = deptId;
//		if (deptId != null && filterBean.getFilterType() == FilterType.DEPT) {
//			filterBean.setSelectFilterValue(deptId.toString());
//		}
//	}

//	/** See {@link FilterBean#getEmployeeAccount()}. */
//	public String getEmployeeAccount() {
//		String acct = filterBean.getEmployeeAccount();
//		if (acct != null && acct.equals(Constants.CATEGORY_ALL)) {
//			acct = null;
//		}
//		return acct;
//	}
//	/** See {@link FilterBean#setEmployeeAccount(String)}. */
//	public void setEmployeeAccount(String employeeAccount) {
//		filterBean.setEmployeeAccount(employeeAccount);
//		if (filterBean.getFilterType() == FilterType.NAME) {
//			filterBean.setSelectFilterValue(employeeAccount);
//		}
//	}

//	/**See {@link #filterBean}. */
//	public FilterBean getFilterBean() {
//		return filterBean;
//	}

	/**See {@link #statusType}. */
	public StartStatusType getStatusType() {
		return statusType;
	}
	/**See {@link #statusType}. */
	public void setStatusType(StartStatusType statusType) {
		this.statusType = statusType;
	}

	/**See {@link #statusTypeFilterList}. */
	public List<SelectItem> getStatusTypeFilterList() {
		return statusTypeFilterList;
	}

	/**See {@link #redChecked}. */
	public boolean getRedChecked() {
		return redChecked;
	}
	/**See {@link #redChecked}. */
	public void setRedChecked(boolean redChecked) {
		this.redChecked = redChecked;
	}

	/**See {@link #yellowChecked}. */
	public boolean getYellowChecked() {
		return yellowChecked;
	}
	/**See {@link #yellowChecked}. */
	public void setYellowChecked(boolean yellowChecked) {
		this.yellowChecked = yellowChecked;
	}

	/**See {@link #greenChecked}. */
	public boolean getGreenChecked() {
		return greenChecked;
	}
	/**See {@link #greenChecked}. */
	public void setGreenChecked(boolean greenChecked) {
		this.greenChecked = greenChecked;
	}

	/**See {@link #contactDocToDelete}. */
	public ContactDocument getContactDocToDelete() {
		return contactDocToDelete;
	}
	/**See {@link #contactDocToDelete}. */
	public void setContactDocToDelete(ContactDocument contactDocToDelete) {
		this.contactDocToDelete = contactDocToDelete;
	}

	/**See {@link #employment}. */
	public Employment getEmployment() {
		return employment;
	}
	/**See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/**See {@link #selectedDocumentId}. */
	public Integer getSelectedDocumentId() {
		return selectedDocumentId;
	}
	/**See {@link #selectedDocumentId}. */
	public void setSelectedDocumentId(Integer selectedDocumentId) {
		this.selectedDocumentId = selectedDocumentId;
	}

	/**See {@link #docsRequireAttn}. */
	public boolean getDocsRequireAttn() {
		return docsRequireAttn;
	}
	/**See {@link #docsRequireAttn}. */
	public void setDocsRequireAttn(boolean docsRequireAttn) {
		this.docsRequireAttn = docsRequireAttn;
	}

	/**See {@link #documentListDL}. */
	public List<SelectItem> getDocumentListDL() {
		if (documentListDL == null) {
			documentListDL = createDocumentListDL();
		}
		return documentListDL;
	}
	/**See {@link #documentListDL}. */
	public void setDocumentListDL(List<SelectItem> documentListDL) {
		this.documentListDL = documentListDL;
	}

	/**See {@link #approverListOfCurrentContact}. */
	public List<Integer> getApproverListOfCurrentContact() {
		if (approverListOfCurrentContact == null) {
			Contact curContact = SessionUtils.getCurrentContact();
			approverListOfCurrentContact =  ApproverDAO.getInstance().
					findIntegerListByNamedQuery(Approver.GET_APPROVER_IDS_BY_CONTACT, map("contact", curContact));
		}
		return approverListOfCurrentContact;
	}
	/**See {@link #approverListOfCurrentContact}. */
	public void setApproverListOfCurrentContact(
			List<Integer> approverListOfCurrentContact) {
		this.approverListOfCurrentContact = approverListOfCurrentContact;
	}

	/**See {@link #currentContactChainMap}. */
	public Map<Project, Map<DocumentChain, List<Department>>> getCurrentContactChainMap() {
		if (currentContactChainMap == null) {
			findDocumentChainsForCurrentContact();
		}
		return currentContactChainMap;
	}
	/**See {@link #currentContactChainMap}. */
	public void setCurrentContactChainMap(Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap) {
		this.currentContactChainMap = currentContactChainMap;
	}

	/** See {@link #showAllProjects}. */
	public boolean getShowAllProjects() {
		return showAllProjects;
	}
	/** See {@link #showAllProjects}. */
	public void setShowAllProjects(boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}

	/** See {@link #viewProject}. */
	public Project getViewProject() {
		return viewProject;
	}
	/** See {@link #viewProject}. */
	public void setViewProject(Project viewProject) {
		this.viewProject = viewProject;
	}

	/** See {@link #currContactChainEditorMap}. */
	public Map<Project, List<DocumentChain>> getCurrContactChainEditorMap() {
		if (currContactChainEditorMap == null) {
			findDocumentChainEditorForCurrentContact();
		}
		return currContactChainEditorMap;
	}
	/** See {@link #currContactChainEditorMap}. */
	public void setCurrContactChainEditorMap(Map<Project, List<DocumentChain>> currContactChainEditorMap) {
		this.currContactChainEditorMap = currContactChainEditorMap;
	}

	/** See {@link #employmentStatusGraph}. */
	public List<DocumentStatusCount> getEmploymentStatusGraph() {
		if (employmentStatusGraph == null) {
			employmentStatusGraph = new ArrayList<>();
			employmentStatusGraph = getContactDocumentDAO().findDocStatusGraphForAllEmployments(
					SessionUtils.getNonSystemProduction(), viewProject);
		}
		return employmentStatusGraph;
	}
	/** See {@link #employmentStatusGraph}. */
	public void setEmploymentStatusGraph(
			List<DocumentStatusCount> employmentStatusGraph) {
		this.employmentStatusGraph = employmentStatusGraph;
	}

	/** See {@link #projectLbl}. */
	public String getProjectLbl() {
		return projectLbl;
	}

	/** See {@link #projectLbl}. */
	public void setProjectLbl(String projectLbl) {
		this.projectLbl = projectLbl;
	}

}