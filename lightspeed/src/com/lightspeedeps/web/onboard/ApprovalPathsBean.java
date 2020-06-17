package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.component.UIData;
import javax.faces.event.*;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.View;


/**
 * Backing bean for the Approval Paths mini tab under OnBoarding tab. It is also
 * useful in maintaining the approval hierarchy for the distributed documents.
 * <p>
 * Note that this bean is instantiated due to an ace:dataTable reference, which
 * is evaluated by the ICEfaces framework code even when the table is NOT being
 * rendered! In fact, the entire mini-tab is not being rendered, but ICEfaces
 * still evaluates the var= parameter of ace:dataTables. Therefore, we have
 * extra measures to avoid most of our bean's initialization until the minitab
 * is actually rendered. The {@link #initialized} boolean is set true when the
 * mini-tab is actually being rendered; this is done by a reference on the page
 * to bean.setup, forcing a call to {@link #getSetUp()}, which then calls the
 * {@link #init()} method.
 */
@ManagedBean
@ViewScoped
public class ApprovalPathsBean extends View implements SelectContactsHolder, Serializable {

	private static final long serialVersionUID = -6469263481851376583L;

	private static final Log log = LogFactory.getLog(ApprovalPathsBean.class);

	/** A string to be displayed along with the Contact entries when that Contact has the
	 * "Approve Documents" permission.  This is primarily a QA tool. */
//	private static final String APPROVER_INDICATOR = " [Appr]";

	/** True iff we have initialized ourselves. */
	private boolean initialized = false;

	/** List of all the document chains to be shown on the right most panel of approval paths */
	private List<DocumentChain> documentChainList;

	/** List of all the approval path names for the corresponding production */
	private List<SelectItem> approvalPathNameList;

	/** Enum list of DocumnetAction, used to determine the approval process */
	private List<SelectItem> documentActionList;

	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_CREATE = 11;

	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 12;

	/** "Rename" action code for popup confirmation/prompt dialog for renaming approval paths. */
	private static final int ACT_RENAME = 13;

	private static final int ACT_SELECT_APP = 14;

	/** "Create Approver Group" action code for popup confirmation/prompt dialog. */
	private static final int ACT_CREATE_GROUP = 15;

	/** "Select Approvers for Production" action code for popup confirmation/prompt dialog. */
	private static final int ACT_ADD_PROD_APP = 16;

	/** "Select Document Editors" action code for popup confirmation/prompt dialog. */
	private static final int ACT_ADD_EDITOR = 17;

	/** "Delete Approver Group" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE_GROUP = 18;

	/** The approval path name entered (in the input field) by the user. */
	private String approvalPathName;

	/** The list of SelectItem's of production members. The label includes both
	 * the contact name and role. The value is the Contact.id field.*/
	private List<SelectItem> memberItems;

	/** The database id of the ProjectMember linked to the currently
	 * selected Production Member list entry. */
	private Integer memberId = -1;

	/** The Contact ids of the approvers present in either the Production,
	 * Project or Department approver list (whichever is currently selected).
	 * This is used to filter the "available members" list (right-hand list) so
	 * that existing approvers are not listed as available. */
	private List<Integer> approverContactIds = new ArrayList<>();

	/** true if the current User is allowed to see "Admin" roles listed. */
	private final boolean showAdmin =false;

	/** Display Project Approvers list. Only true for Commercial productions. */
	private boolean showProjectApprovers;

	/** true iff the current production is AICP (Commercial). */
	private boolean aicp;

	/** The current Project; only used for Commercial productions. */
	private Project project;

	/** The ContactDAO instance */
	private transient ContactDAO contactDAO;

	/** The ApprovalPathDAO instance */
	private transient ApprovalPathDAO approvalPathDAO;

	/** The ApproverDAO instance */
	private transient ApproverDAO approverDAO;

	/** The ContactDocumentDAO instance */
	private transient ContactDocumentDAO contactDocumentDAO;

	/** The ApproverGroupDAO instance */
	private transient ApproverGroupDAO approverGroupDAO;

	/** The ApprovalPathAnchorDAO instance */
	private transient ApprovalPathAnchorDAO approvalPathAnchorDAO;

	/** The ApproverAuditDAO instance */
	private transient ApproverAuditDAO approverAuditDAO;

	/** The PayrollPreferenceDAO instance */
	private transient PayrollPreferenceDAO payrollPreferenceDAO;
	
	/** String literal used to store the current user selection of radio buttons
	 * (Linear Hierarchy or Approval Pool) */
	private String approvalMethod = LINEAR_HIERARCHY;
	public static final String LINEAR_HIERARCHY= "l";
	public static final String APPROVER_POOL= "a";

	/** String literal used to store the current user selection of radio buttons
	 * (Production Approver or Department Approver) */
	/*private String approverType = PRODUCTION_APPROVER;
	public static final String PRODUCTION_APPROVER= "p";
	public static final String DEPARTMENT_APPROVER= "d";*/

	/** Production members list in the current production or project */
	private List<SelectItem> productionMembers;

	/** Department list in the current production or project */
	private List<Department> departmentList;

	/** The list of checked Document chains from the data table */
	private List<DocumentChain> checkBoxSelectedItems = new ArrayList<>(0);

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAll = false;

	/** True, if the approval path view is in the default view */
//	private boolean isDefaultView;

	/** The current Production. */
	private Production currentProduction;

	/** Database Id of the selected Approval Path from the approval path drop down*/
	private Integer selectedApprovalPathId;

	/** Used to store the currently selected approval path from the drop down */
	private ApprovalPath currentApprovalPath;

	/** Database Id of the selected Final Approver from the final approver drop down under Approver Pool */
	private Integer finalApproverId;

	/** Select item list used to store the production approvers list */
	private List<SelectItem> productionApproverList = new ArrayList<>();

	/** Department instance used to hold the currently selected department from department drop down */
	private Department selectedDepartment;

	/** Department id used to hold the currently selected department id from department drop down */
	private Integer selectedDepartmentId;

	/** String literal used to store the current user selection of radio buttons
	 * (Final Approver or First Approver) */
	private String finalPoolApprover = FINAL_APPROVER;
	public static final String FINAL_APPROVER= "u";
	public static final String FIRST_APPROVER= "o";

	/** List of all the approver group names for the corresponding Production or Project. */
	private List<SelectItem> appGroupNameList;

	/** List of selected Contacts as Group Approvers. */
	private List<SelectItem> groupApproverList;

	/** The approver group name entered (in the input field) by the user. */
	private String approverGroupName;

	/** List of selected Departments for the selected Approver Group. */
	private List<Department> selectedDeptList;

	/** List of selected Contacts as Document Editors. */
	private List<SelectItem> documentEditorList;

	/** Database Id of the selected Approver Group from the Approver Group drop down*/
	private Integer selectedAppGroupId;

	/** ApproverGroup instance used to hold the currently selected ApproverGroup from ApproverGroup drop down */
	private ApproverGroup selectedApproverGroup;

	/** The list of selected production members. The label includes both
	 * the contact name and role. The value is the Contact.id field.*/
	private List<SelectItem> selectedMemberItems;

	/** The list of unselected production members. The label includes both
	 * the contact name and role. The value is the Contact.id field.*/
	private List<SelectItem> unselectedMemberItems;

	/** String literal used to store the current user selection of radio buttons
	 * for the view of Approval Path tab, either "Full instructions" or "Short instructions". */
	private String viewType = FULL_INSTRUCTIONS;
	public static final String FULL_INSTRUCTIONS = "f";
	public static final String SHORT_INSTRUCTIONS= "s";

	/** Boolean used to show or hide the Step 1. */
	private boolean showStep1 = true;

	/** Boolean used to show or hide the Step 2. */
	private boolean showStep2 = false;

	/** Boolean used to show or hide the Step 3. */
	private boolean showStep3 = false;

	/** Boolean used to show or hide the Step 4. */
	private boolean showStep4 = false;

	/** Boolean used to show or hide the Step 5. */
	private boolean showStep5 = false;

	/** Boolean used to show or hide the Step 6. */
	private boolean showStep6 = false;

	/** The RowStateMap instance used to manage the clicked row on the Approver Group's data table */
	private RowStateMap groupStateMap = new RowStateMap();

	/** Used to hold the index of selected row in table for Group Approvers.
	 * This index is used by the Up/Down arrows to move the selected row up or down. */
	private Integer selectedGroupRowIndex;

	/** The RowStateMap instance used to manage the clicked row on the Production Approver's data table */
	private RowStateMap prodStateMap = new RowStateMap();

	/** Used to hold the index of selected row in table for Production Approvers.
	 * This index is used by the Up/Down arrows to move the selected row up or down. */
	private Integer selectedProdRowIndex;

	/** String literal used to store the current user selection of radio buttons for Approver Pool type.
	 * (Linear Hierarchy or Approval Pool) */
	private String groupApprovalMethod = LINEAR_HIERARCHY;

	/** True, if the master check box for Departments is checked otherwise false */
	///private Boolean checkedForAllDepts = false;

	/** Map used to hold the ApproverGroup wise list of Departments for a corresponding approval path */
	private Map<ApproverGroup, List<Department>> mapOfAppGroupDepartment = new HashMap<>();

	/** Map used to hold the ApproverGroup wise list of approvers for a corresponding approval path */
	//private Map<Integer, List<SelectItem>> mapOfAppGroupApprovers = new HashMap<>();

	/** Constructor */
	public ApprovalPathsBean() {
		super("ApprovalPathBean.");
		viewType = UserPrefBean.getInstance().getString(Constants.ATTR_INSTR_VIEW_PREF, viewType);
		log.debug("View type = " + getViewType());
		if (getTalentProd()) {
			finalPoolApprover = FIRST_APPROVER;
		}
	}

	/**
	 * Handle most initialization of the bean. This is delayed until we are
	 * called via a JSP reference on the Approval Paths mini-tab, to avoid the
	 * initialization overhead when the mini-tab is not being rendered.
	 */
	private void init() {
		try {
			initialized = true;
			documentActionList = new ArrayList<>(EnumList.getDocumentActionTypeList());
			/*AuthorizationBean authBean = AuthorizationBean.getInstance();
			showAdmin = authBean.isAdmin();*/
			currentProduction = SessionUtils.getNonSystemProduction();

			aicp = currentProduction.getType().hasPayrollByProject();
			showProjectApprovers = false;
			if (aicp) {
				showProjectApprovers = true;
				project = SessionUtils.getCurrentProject();
			}
			else {
				showProjectApprovers = false;
			}

			refreshMemberItems();
			createDepartmentList();
			//selectedDepartmentId = departmentList.get(0).getId();
			//selectedDepartment = departmentList.get(0);

			getApprovalPathNameList();
			getAppGroupNameList();
			currentApprovalPath = findDefaultPath();
			if (currentApprovalPath != null) {
				selectedApprovalPathId = currentApprovalPath.getId();
				createApprovalPathDefaultView(currentApprovalPath); // if in session create view with session variable
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Determine the path to be displayed, either based on a saved session
	 * variable, or just the first one in the list of paths for this production.
	 *
	 * @return The default ApprovalPath to display.
	 */
	private ApprovalPath findDefaultPath() {
		ApprovalPath path = null;
		try {
			Integer id = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID);
			if (id != null) {
				path = getApprovalPathDAO().findById(id);
			}
			if (path == null) {
				if (approvalPathNameList != null && ! approvalPathNameList.isEmpty()) {
					id = (Integer) approvalPathNameList.get(0).getValue();
					if (id != null && id > 0) {
						path = getApprovalPathDAO().findById(id);
					}
				}
				if (path == null) {
					path = new ApprovalPath(); // in case we don't find one.
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
	 * Called due to hidden field value in our JSP page. Make sure we're
	 * initialized to show something. Note that this must be public as
	 * it is called from JSP.
	 */
	public boolean getSetUp() {
		try {
			if (! initialized) {
				init();
			}
			if (currentApprovalPath == null) {
				if (! editMode) {
	//				documentChainList = null;
					getDocumentChainList(); // ensure it's initialized
				}
				currentApprovalPath = findDefaultPath();
				if (currentApprovalPath != null) {
					checkDocumentChain(currentApprovalPath);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/** Used to return the instance of the ApprovalPathBean */
	public static ApprovalPathsBean getInstance() {
		return (ApprovalPathsBean)ServiceFinder.findBean("approvalPathsBean");
	}

	/** Method creates the list of document chains for the current production
	 * @return document chain list
	 */
	private List<DocumentChain> createDocumentChainListByProd() {
		List<DocumentChain> list = new ArrayList<>();
		try {
			boolean canada = currentProduction.getType().isCanadaTalent();
			// Refresh PayrollPreference object to prevent LIE
			PayrollPreference pf = getPayrollPreferenceDAO().refresh(currentProduction.getPayrollPref());
			PayrollService ps = pf.getPayrollService();
			boolean isTeamProd = (ps == null ? false : ps.getTeamPayroll());
			boolean hideModelRelease = ! isTeamProd || ! FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM);
			// Hide Payroll Start if 'use model release' option selected and FF is on. LS-4502; LS-4757
			boolean hideStartForm = ! hideModelRelease && (pf.getUseModelRelease() && FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS));
			Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(null);
			list = DocumentChainDAO.getInstance().findByProperty("folder", onboardFolder);
			Iterator<DocumentChain> itr = list.iterator();
			while (itr.hasNext()) {
				DocumentChain chain = itr.next();
				if ((chain.getDeleted() && (chain.getApprovalPath() == null || chain.getApprovalPath().size() == 0)) ||
						(canada && chain.hideForCanada()) ||
						(hideModelRelease && chain.getFormType() == PayrollFormType.MODEL_RELEASE) ||
						(hideStartForm && chain.getFormType() == PayrollFormType.START)
						) {
					itr.remove();
				}
			}

		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return list;
	}

	/**
	 * Method used to create the list of approval paths for the current
	 * Production. The value field (of the SelectItem) is the database
	 * id of the ApprovalPath.
	 *
	 * @return list of approval paths for a drop-down list.
	 */
	private List<SelectItem> createApprovalPathNameList() {
		List<SelectItem> pathList = new ArrayList<>();
		try {
			Production production = SessionUtils.getProduction();
			List<ApprovalPath> list = null;
			if (production.getType().isAicp()) {
				list = getApprovalPathDAO().findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, map("project", SessionUtils.getCurrentProject()));
			}
			else {
				list = getApprovalPathDAO().findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PRODUCTION, map("production", SessionUtils.getProduction()));
			}
			selectedApprovalPathId = null;
			if (list.size() > 0) {
				log.debug("Approval Path names list size = "+list.size());
				Integer savedPathId = (Integer) SessionUtils.get(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID);
				approvalPathNameList = new ArrayList<>();
				for (ApprovalPath path : list) {
					pathList.add(new SelectItem(path.getId(), path.getName()));
					if (path.getId().equals(savedPathId)) { // prior path exists in list
						selectedApprovalPathId = savedPathId; // so keep it as default
					}
				}
				Collections.sort(pathList, getSelectItemComparator());
				if (selectedApprovalPathId == null) { // no prior one, or it wasn't in this list
					selectedApprovalPathId = (Integer)pathList.get(0).getValue(); // so use 1st as default
				}
			}
			else {
				pathList.add(0, new SelectItem(-1, Constants.SELECT_OR_CREATE_APPROVAL_PATH));
			}
			SessionUtils.put(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID, selectedApprovalPathId);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return pathList;
	}

	/**
	 * Creates entries for the Production Members display/selection list. The
	 * display includes the member's name and role in this project. The list is
	 * filtered based on the user's viewing permissions for cast, crew, and
	 * admin roles.
	 *
	 * @param dept If null, all qualified production members are listed; if not
	 *            null, then only those members whose Role is within the given
	 *            Department are listed.
	 * @param aggregateOnly If true and 'dept' is null, only Contacts with
	 *            View__All_Projects permission are included. (If 'dept' is not
	 *            null, this parameter is ignored.)
	 */
	private void createMemberItems(Department dept, boolean aggregateOnly) {
		try {
			log.debug("");
			List<SelectItem> items = new ArrayList<>();
			Collection<Contact> contacts;

			// Get the list of eligible Contacts
			/*if (dept == null && aggregateOnly) {
				contacts = ProjectMemberDAO.getInstance()
						.findByProductionPermissionDistinctContact(SessionUtils.getProduction(), Permission.VIEW_ALL_PROJECTS);
			}
			else*/ if (showProjectApprovers) {
				contacts = getContactDAO().findByProjectActive(project);
			}
			else {
				contacts = getContactDAO().findByProductionActive(currentProduction);
			}

			for (Contact contact : contacts) {
				// We need to exclude from the list we're building the contacts
				// that are already in the selected Production/Project/Department approver list.
				if (! approverContactIds.contains(contact.getId())) {
					for (Employment member : contact.getEmployments()) {
						Role role = member.getRole();
						// Only allow if no department screening, or matches the department requested.
						if (dept == null || role.getDepartment().getId().equals(dept.getId())) {
							// Omit Admin roles, unless user has privilege to see them.
							if (! role.isAdmin() || showAdmin) {
								String label = contact.getUser().getLastNameFirstName();
								String apprLabel = ""; // (Permission.APPROVE_START_DOCS.inMask(contact.getPermissionMask()) ? APPROVER_INDICATOR : "");
								label += " - " + role.getName() + apprLabel;
								items.add( new SelectItem(contact.getId(), label) );
								break;
							}
						}
					}
				}
			}
			Collections.sort(items, getSelectItemComparator());
			memberItems = items;
			if (memberItems.size() == 1) {
				// selectOneListBox doesn't work right with 1 entry; "pre-select" it.
				memberId = (Integer)memberItems.get(0).getValue();
			}
			else {
				memberId = null; // clear selection
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Refresh the Members list with either all Production members, or
	 * current department members, depending on the user's Display selection.
	 */
	private void refreshMemberItems() {
		// Populate the approver contact list to filter in member list box
		//createApproverContactIds();
		createMemberItems(null, true & aicp);
	}

	/**
	 * Method creates the list of production members for the drop down, in the
	 * currently active production
	 *
	 * @return list of production members
	 */
	private List<SelectItem> createProductionMembersList() {
		productionMembers = new ArrayList<>();
		try {
			Collection<Contact> contacts = getContactDAO().findByProductionActive(currentProduction);
			if (contacts != null) {
				for (Contact contact : contacts) {
					User user = contact.getUser();
					productionMembers.add(new SelectItem(contact.getId(), user.getLastNameFirstName()));
				}
				Collections.sort(productionMembers, getSelectItemComparator());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return productionMembers;
	}

	/** Method creates the department list of the current production
	 * @return list of department
	 */
	private List<Department> createDepartmentList() {
		departmentList = new ArrayList<>();
		try {
			for (SelectItem item : DepartmentUtils.getDepartmentCastCrewDL()) {
				Department dept = DepartmentDAO.getInstance().findById((Integer) item.getValue());
				if (dept != null) {
					departmentList.add(dept);
				}
			}
		//	departmentList.addAll(DepartmentUtils.getDepartmentCastCrewDL());
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return departmentList;
	}

	/** Action method used to open the create pop up, for creating
	 * a new approval path for the document chain
	 * @return null
	 */
	public String actionCreateApprovalPath() {
		PopupInputBean inputBean = PopupInputBean.getInstance();
		inputBean.show(this, ACT_CREATE, "ApprovalPathBean.NewPath.");
		inputBean.setInput("New Path");
		return null;
	}

	/** ActionOk method invoked when the user clicks "Create" on the new path create pop up
	 * @return null
	 */
	private String actionCreateApprovalPathOk() {
		try{
			if (approvalPathName != null) {
				approvalPathName = approvalPathName.trim();

				int approvalPathId = 0;
				ApprovalPath approvalPath = null;
				if (currentProduction.getType().isAicp()) {
					approvalPath = getApprovalPathDAO().findApprovalPathByNameProject(approvalPathName, SessionUtils.getCurrentProject());
					if (approvalPath != null) {
						PopupInputBean inputBean = PopupInputBean.getInstance();
						inputBean.displayError(MsgUtils.getMessage("ApprovalPathBean.DuplicatePathName"));
						return null;
					}
					else {
						approvalPathId = getApprovalPathDAO().createApprovalPath(approvalPathName, currentProduction, SessionUtils.getCurrentProject());
					}
				}
				else {
					approvalPath = getApprovalPathDAO().findApprovalPathByNameProduction(approvalPathName, currentProduction);
					if (approvalPath != null) {
						PopupInputBean inputBean = PopupInputBean.getInstance();
						inputBean.displayError(MsgUtils.getMessage("ApprovalPathBean.DuplicatePathName"));
						return null;
					}
					else {
						approvalPathId = getApprovalPathDAO().createApprovalPath(approvalPathName, currentProduction, null);
					}
					log.debug("New approver path with Id and name:" + approvalPathId + " " + approvalPathName);
				}
				//currentApprovalPath.setApprovalRequired(false);
				//mapOfDepartmentApprovers.clear();
				checkBoxSelectedItems.clear();
				approvalPathNameList = null;
				getApprovalPathNameList();
//				isDefaultView = false;
				productionApproverList = new ArrayList<>();
				documentEditorList = new ArrayList<>();
				mapOfAppGroupDepartment.clear();
				getSelectedDeptList().clear();
				updateDepartments(getDepartmentList(), false, false);
				//departmentApproverList = new ArrayList<>();
				ApprovalPath path = getApprovalPathDAO().findById(approvalPathId);
				SessionUtils.put(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID, path.getId());
				setSelectedApprovalPathId(path.getId());
				createApprovalPathDefaultView(path);
				refreshMemberItems();
			}
		}
		catch (Exception e) {
			EventUtils.logError("ApprovalPathBean createApprovalPath failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action method used to open the delete pop up, for deleting
	 * a approval path for the document chain
	 * @return null
	 */
	public String actionDeleteApprovalPath() {
		if (selectedApprovalPathId != null) {
			PopupBean bean = PopupBean.getInstance();
			bean.show(this, ACT_DELETE, "ApprovalPathBean.DeletePath.");
		}
		else {
			MsgUtils.addFacesMessage("ApprovalPathBean.SelectApprovalPathForDelete", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** ActionOk method invoked when the user clicks "Delete" on the delete path pop up
	 * @return null
	 */
	private String actionDeleteApprovalPathOk() {
		try {
			ApprovalPath approvalPath = getApprovalPathDAO().findById(selectedApprovalPathId);
			if (approvalPath != null) {
				for (DocumentChain chain : approvalPath.getDocumentChains()) {
					updateRemovedChainContactDocs(chain);
				}
				Approver firstPathApprover = approvalPath.getApprover();
				approvalPath.setApprover(null);
				deleteApproversAndAnchor(approvalPath, firstPathApprover);
				getApprovalPathDAO().delete(approvalPath);
			}
			resetApprovalPathList();
			refreshDistReviewTab();
	//		isDefaultView = true;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action method used to open the rename pop up, for renaming
	 * an approval path.
	 * @return null
	 */
	public String actionRenameApprovalPath() {
		if (selectedApprovalPathId != null) {
			ApprovalPath approvalPath = getApprovalPathDAO().findById(selectedApprovalPathId);
			if (approvalPath != null) {
				setCurrentApprovalPath(approvalPath);
				PopupInputBean inputBean = PopupInputBean.getInstance();
				inputBean.show(this, ACT_RENAME, "ApprovalPathsBean.RenamePath.");
				inputBean.setInput(approvalPath.getName());
			}
		}
		else {
			MsgUtils.addFacesMessage("ApprovalPathsBean.SelectApprovalPathToRename", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** ActionOk method invoked when the user clicks "Rename" on the Rename path pop up
	 * @return null
	 */
	private String actionRenameApprovalPathOk() {
		try {
			if (currentApprovalPath != null) {
				String newName;
				ApprovalPath approvalPath = null;
				PopupInputBean inputBean = PopupInputBean.getInstance();
				newName = inputBean.getInput().trim();
				if (StringUtils.isEmpty(newName)) {
					inputBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.SelectApprovalPathToRename"));
					return null;
				}
				if (currentProduction.getType().isAicp()) {
					approvalPath = getApprovalPathDAO().findApprovalPathByNameProject(newName, SessionUtils.getCurrentProject());
				}
				else {
					approvalPath = getApprovalPathDAO().findApprovalPathByNameProduction(newName, currentProduction);
				}
				if (approvalPath != null) {
					approvalPath = getApprovalPathDAO().refresh(approvalPath);
					if ( ! approvalPath.getId().equals(currentApprovalPath.getId())) {
						inputBean.displayError(MsgUtils.getMessage("ApprovalPathBean.DuplicatePathName"));
						return null;
					}
				}
				else {
					currentApprovalPath.setName(newName);
					getApprovalPathDAO().attachDirty(currentApprovalPath);
				}
			}
			approvalPathNameList = createApprovalPathNameList();
			selectedApprovalPathId = currentApprovalPath.getId();
			SessionUtils.put(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID, selectedApprovalPathId);
			createApprovalPathDefaultView(currentApprovalPath);
			setSelectedApprovalPathId(currentApprovalPath.getId());
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}


	/**
	 * Utility method used to re-create the approval path list when the user deletes
	 * an approval path.
	 */
	private void resetApprovalPathList() {
		try {
			approvalPathNameList = createApprovalPathNameList();
	//		if ( ! approvalPathNameList.get(0).getLabel().toString().equalsIgnoreCase(Constants.SELECT_OR_CREATE_APPROVAL_PATH)) {
	//			approvalPathNameList.add(0, new SelectItem(null, Constants.SELECT_OR_CREATE_APPROVAL_PATH));
	//		}
	//		currentApprovalPath.setApprovalRequired(false);x
			currentApprovalPath = findDefaultPath();
			if (currentApprovalPath != null) {
				selectedApprovalPathId = currentApprovalPath.getId();
				createApprovalPathDefaultView(currentApprovalPath); // if in session create view with session variable
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	public String confirmOk(int action) {
		String res = null;

		approvalPathName = PopupInputBean.getInstance().getInput();
		switch(action) {
			case ACT_CREATE:
				res = actionCreateApprovalPathOk();
				break;
			case ACT_DELETE:
				res = actionDeleteApprovalPathOk();
				break;
			case ACT_DELETE_GROUP:
				res = actionDeleteApproverGroupOk();
				break;
			case ACT_RENAME:
				res = actionRenameApprovalPathOk();
				break;
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		try {
			ApprovalPath path = null;
			if (selectedApprovalPathId != null) {
				path = getApprovalPathDAO().findById(selectedApprovalPathId);
			}
			createApprovalPathDefaultView(path);
			return super.confirmCancel(action);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** actionSave method invoked when the user clicks "Save" button on the edit view of the Approval Paths.
	 * @return String
	 */
	@Override
	public String actionSave() {
		try {
			ApprovalPath path = getApprovalPathDAO().findById(selectedApprovalPathId);
			//String contactList = "";
			/*if (productionApproverList.isEmpty()) {
				if (mapOfDepartmentApprovers.size() == 1) {
					for (Integer key : mapOfDepartmentApprovers.keySet()) {
						List<SelectItem> approverList = mapOfDepartmentApprovers.get(key);
						if (approverList.isEmpty()) {
							MsgUtils.addFacesMessage("ApprovalPathBean.NoApprovers", FacesMessage.SEVERITY_ERROR);
						}
					}
				}
			}*/
			//Put in map, the last selected approver group's deptartments or the first group if other groups are not used.
			if (selectedApproverGroup != null && getMapOfAppGroupDepartment() != null && getSelectedDeptList() != null && (! getSelectedDeptList().isEmpty())) {
				log.debug("selectedApproverGroup = " + selectedApproverGroup.getGroupName());
				mapOfAppGroupDepartment.put(selectedApproverGroup, new ArrayList<>(getSelectedDeptList()));
				log.debug("AppGroupDepartment map size = " + mapOfAppGroupDepartment.size());
			}
			if (path != null) {
				/*boolean isValid = checkPathValidityToUpdate(path);
				if (isValid) {
					log.debug("");
					// show pop up
					for (Approver approver : removedApproversMap.keySet()) {
						if (removedApproversMap.get(approver) != null && removedApproversMap.get(approver)) {
							contactList = contactList + approver.getContact().getDisplayName() + ", ";
						}
					}
					if (contactList.length() > 1) {
						contactList = contactList.substring(0, contactList.length()-2);
						log.debug(">>>>>> deletedApproverContactList" + contactList);
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, ACT_REMOVE_APPROVER, "ApprovalPathBean.DeleteApprover.");
						bean.setMessage(MsgUtils.formatMessage("ApprovalPathBean.DeleteApprover.Text", contactList));
						return null;
					}
				}*/
				//if (! isValid || contactList.length() < 1) {
					log.debug("");
					//Set<DocumentChain> chainSet = path.getDocumentChains();
					saveApprovalPath(path);
					/*Set<DocumentChain> newChainSet = path.getDocumentChains();
					for (DocumentChain chain : chainSet) {
						if (! newChainSet.contains(chain)) {
							updateChainContactDocuments(chain);
						}
					}*/
					log.debug("Action Save");
					refreshDistReviewTab();
					//updateContactDocs(path);
					return super.actionSave();
				//}
			}
			return null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			return null;
		}
	}

	@Override
	public String actionEdit() {
		try {
			for(DocumentChain chain : documentChainList) {
				if(chain.getChecked() == true) {
					checkBoxSelectedItems.add(chain);
				}
			}
			if (getSelectedApproverGroup() == null && mapOfAppGroupDepartment != null && (! mapOfAppGroupDepartment.isEmpty())) {
				//Fetch first approver group for the path.
				selectedApproverGroup = mapOfAppGroupDepartment.keySet().iterator().next();
			}
			if (selectedApproverGroup == null) {
				//Fetch first approver group.
				selectedAppGroupId = (Integer) getAppGroupNameList().get(0).getValue();
				selectedApproverGroup = getApproverGroupDAO().findById(selectedAppGroupId);
			}
			if (selectedApproverGroup != null) {
				setSelectedAppGroupId(selectedApproverGroup.getId());
				disableDepartments(selectedApproverGroup);
			}
			return super.actionEdit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private void saveApprovalPath(ApprovalPath path) {
		try {
			Approver priorFinalApp = path.getFinalApprover();
			Approver firstPathApprover = path.getApprover();
			path.setApprover(null);
			deleteApproversAndAnchor(path, firstPathApprover);
			boolean priorWasPool = path.getUsePool();
			if (getApprovalMethod().equals(APPROVER_POOL)) {
				createApprovalPathPool(path);
			}
			else {
				//For linear hierarchy
				path.setUsePool(false);
				path.setFinalApprover(null);
				if (! productionApproverList.isEmpty()) {
					createProductionApprovers(path);
				}
			}

			//Create Department anchors and update Audit entries
			if (getMapOfAppGroupDepartment() != null) {
				log.debug("MapOfAppGroupDepartment Size = " + getMapOfAppGroupDepartment().size());
				for (ApproverGroup group : mapOfAppGroupDepartment.keySet()) {
					List<Department> deptList = mapOfAppGroupDepartment.get(group);
					if (deptList != null && ! deptList.isEmpty()) {
						createDepartmentAnchors(path, group, deptList);
					}
				}
			}

			//Create Document Editors
			if (getDocumentEditorList() != null) {
				path.setEditors(createContactSet(getDocumentEditorList()));
				for (Contact ct : path.getEditors()) {
					addRemovePermission(ct, true);
				}
			}
			Set<DocumentChain> chainSet = path.getDocumentChains();
			if (checkBoxSelectedItems != null) {
				log.debug("checkBoxSelectedItems size= " + checkBoxSelectedItems.size());
				Set<DocumentChain> docChainSet = new HashSet<>();
				if(aicp) { // check for commercials
					for (DocumentChain docChain : checkBoxSelectedItems) {
						docChain = DocumentChainDAO.getInstance().refresh(docChain);
						if (docChain != null) { // may happen if someone just deleted the (custom) document
							Set<ApprovalPath> allPathsOfChain = docChain.getApprovalPath(); // take out associated paths of the chain
							boolean flag = false; // false if document chain has no approval path in current project.
							if (allPathsOfChain != null && allPathsOfChain.size() > 0) {
								Iterator itr = allPathsOfChain.iterator();
								while (itr.hasNext()) {
									ApprovalPath pathFetched = (ApprovalPath)itr.next();
									if (SessionUtils.getCurrentProject().equals(pathFetched.getProject()) && (! pathFetched.equals(path))) {
										flag = true;
										break;
									}
								}
								if (flag == false) {
									docChainSet.add(docChain);
								}
							}
							else {
								docChainSet.add(docChain);
							}
						}
					}
				}
				else { // for non-commercials
					for (DocumentChain docChain : checkBoxSelectedItems) {
						docChain = DocumentChainDAO.getInstance().refresh(docChain);
						if (docChain.getApprovalPath().isEmpty() || docChain.getApprovalPath().contains(path)) { // if no path associated associate the checked chain
							docChainSet.add(docChain);
							log.debug("docChainSet size= " + docChainSet.size());
						}
					}
				}
				path.setDocumentChains(docChainSet);
			}
			getApprovalPathDAO().attachDirty(path);
			log.debug("path updated " + path.getName());

			// 1. Check for removal of Document Chain and Update the Contact Documents.
			if (path.getDocumentChains() != null && chainSet != null) {
				for (DocumentChain chain : chainSet) {
					if (! path.getDocumentChains().contains(chain)) {
						updateRemovedChainContactDocs(chain);
					}
				}
			}

			// 2. Check for Change of Approval Method for Production Approvers
			if (path.getUsePool() && (! priorWasPool)) { // Changed from Linear to Pool
				changeApprovalMethodForDocuments(path, null, true, null, null);
			}
			else if ((! path.getUsePool()) && priorWasPool) { // Changed from Pool to Linear
				changeApprovalMethodForDocuments(path, null, false, path.getApprover(), priorFinalApp);
			}

			// 3. Update ContactDocuments if Path is Updated (Add/ Remove/ Replace/ Move Approvers).
			updateContactDouments(path, null);

			// 4. Update contact documents of newly Added Document Chains
			assignApproversToContactDocuments(path, null);

			// 5. Update Department Contact Documents (if removed any departments)
			updateDeptContactDocuments(path);

			// Delete all the ApproverAudits for the ApprovalPath
			List<ApproverAudit> pathAudits = getApproverAuditDAO().findByProperty("approvalPath", path);
			if (pathAudits != null) {
				for (ApproverAudit appRecord : pathAudits) {
					getApproverAuditDAO().delete(appRecord);
				}
				log.debug("**** DELETED AUDIT FOR PATH ****");
			}

			setEditMode(false);
			checkBoxSelectedItems.clear();
			checkedForAll = false;
			checkDocumentChain(path);
			//StatusListBean.getInstance().clearCurrentContactChainMap();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Re-create the approver pool for the given path, using the contacts
	 * currently in the {@link #productionApproverList}. The 'final approver'
	 * settings will be based on the current UI values for the associated radio
	 * button and drop-down list. (Refactored by LS-3661.)
	 *
	 * @param path The path whose approver pool is to be replaced.
	 */
	private void createApprovalPathPool(ApprovalPath path) {
		Set<Contact> contacts = createContactSet(getProductionApproverList());
		createApprovalPathPool(path, contacts, finalApproverId, currentProduction, getFinalPoolApprover().equals(FINAL_APPROVER));
	}

	/**
	 * Re-create the approver pool for the given path, using the Set of
	 * Contact's given. If 'hasFinalApprover' is true, then 'finalApprId' should
	 * not be null -- it should be the Contact.id value of the final approver.
	 * (Refactored by LS-3661.)
	 *
	 * @param path The path whose approver pool is to be replaced.
	 * @param contacts The Set of Contact instances which are the new pool of
	 *            approvers.
	 * @param finalApprId The Contact.id value of the 'final approver'; may be
	 *            null if 'hasFinalApprover' is false.
	 * @param prod The production owning the path; this is only used for
	 *            tracking changes in the final approver.
	 * @param hasFinalApprover True if the user has specified that a 'final
	 *            approver' exists for this pool path.
	 */
	public void createApprovalPathPool(ApprovalPath path, Set<Contact> contacts, Integer finalApprId, Production prod, boolean hasFinalApprover) {
		path.setUsePool(true);
		Contact finalApprover = null;
		if (hasFinalApprover) {
			path.setUseFinalApprover(true);
			finalApprover = getContactDAO().findById(finalApprId);
			Approver approver = new Approver();
			approver.setContact(finalApprover);
			approver.setNextApprover(null);
			approver.setShared(true);
			Integer finalAppId = getApproverDAO().save(approver);
			log.debug("final approver id " + finalAppId);
			path.setFinalApprover(approver);
			// Update Approver's info in Audit table
			Map<String, Object> values= new HashMap<>();
			values.put("production", prod);
			values.put("approvalPath", path);
			values.put("auditType", ApproverAuditType.FINAL);
			ApproverAudit deletedFinalApp = getApproverAuditDAO().findOneByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE, values);
			if (deletedFinalApp != null) { // null if previously did not use Final Approver.
				deletedFinalApp.setNewApproverId(finalAppId);
				deletedFinalApp.setNewContact(finalApprover);
				getApproverAuditDAO().attachDirty(deletedFinalApp);
			}
		}
		else {
			path.setUseFinalApprover(false);
			path.setFinalApprover(null);
		}
		path.setApproverPool(contacts); // TODO

		// Make sure all approvers have special permission
		if (finalApprover != null) {
			addRemovePermission(finalApprover, true);
		}
		for (Contact ct : path.getApproverPool()) {
			addRemovePermission(ct, true);
		}
	}

	/** Method updates the contact documents of the Document chain that is removed from the path.
	 * @param chain, Document chain whose contact documents need to be updated.
	 */
	private void updateRemovedChainContactDocs(DocumentChain chain) {
		try {
			log.debug("");
			List<ContactDocument> cdList = new ArrayList<>();
			cdList = getContactDocumentDAO().findByProperty("documentChain", chain);
			for (ContactDocument cd : cdList) {
				if (cd.getApproverId() != null) {
					if (cd.getStatus().isPendingApproval()) {
						if ((aicp && cd.getProject().equals(SessionUtils.getCurrentOrViewedProject())) || (! aicp)) {
							log.debug("Contact document: " + cd.getId());
							cd.setStatus(ApprovalStatus.SUBMITTED_NO_APPROVERS);
							cd.setApproverId(null);
							cd.setIsDeptPool(false);
							getContactDocumentDAO().attachDirty(cd);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method used to update the contact documents when the type of path is
	 * changed. Any CD waiting for old pool is moved to new first approver; or a
	 * CD waiting for old approver in linear list is moved to pool.
	 *
	 * @param path
	 * @param group
	 * @param newIsPool
	 * @param newFirstApprover
	 * @param priorFinalApp
	 */
	public void changeApprovalMethodForDocuments(ApprovalPath path, ApproverGroup group, boolean newIsPool, Approver newFirstApprover, Approver priorFinalApp) {
		try {
			log.debug("");
			List<ContactDocument> contactDocList;
			Integer poolApproverId = null;
			boolean emptyPool = false;
			List<ApproverAudit> approverAuditList = null;
			Map<String, Object> values= new HashMap<>();
			values.put("production", currentProduction);
			if (path != null) {
				log.debug("");
				poolApproverId = -(path.getId());
				// If changed from linear to pool
				if (newIsPool) {
					log.debug("");
					if (path.getApproverPool() == null || path.getApproverPool().isEmpty()) {
						emptyPool = true;
					}
					values.put("approvalPath", path);
					values.put("auditType", ApproverAuditType.PROD);
					approverAuditList = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE, values);
				}
			}
			else if (group != null) {
				log.debug("");
				poolApproverId = -(group.getId());
				// If changed from linear to pool
				if (newIsPool) {
					log.debug("");
					if (group.getGroupApproverPool() == null || group.getGroupApproverPool().isEmpty()) {
						emptyPool = true;
					}
					values.put("approverGroup", group);
					approverAuditList = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APP_GROUP_AUDIT_TYPE, values);
				}
			}

			// If changed from linear to pool
			if (newIsPool && approverAuditList != null) {
				log.debug("");
				for (ApproverAudit oldAppRecord : approverAuditList) {
					contactDocList = new ArrayList<>();
					log.debug("oldApp's ApproverId= " + oldAppRecord.getOldApproverId());
					contactDocList = ContactDocumentDAO.getInstance().findByProperty("approverId", oldAppRecord.getOldApproverId());
					if (! emptyPool) {
						for (ContactDocument cd : contactDocList) {
							cd.setApproverId(poolApproverId);
							if (group != null) {
								cd.setIsDeptPool(true);
							}
							ContactDocumentDAO.getInstance().attachDirty(cd);
						}
					}
					else {
						if (path != null) {
							if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
								for (ContactDocument cd : contactDocList) {
									cd.setApproverId(path.getFinalApprover().getId());
									cd.setApprovalLevel(ApprovalLevel.FINAL);
									ContactDocumentDAO.getInstance().attachDirty(cd);
								}
							}
							else {
								Map<Department, Integer> mapOfDeptTopApproverIds = new HashMap<>();
								// Store Departments with their Approver group's topmost Approver's Id in a map.
								mapOfDeptTopApproverIds = getMapOfDeptGroupApproverIds(path, mapOfDeptTopApproverIds, true);
								assignDocumentsToRespectiveDeptApprovers(contactDocList, mapOfDeptTopApproverIds);
							}
						}
						else if (group != null) {
							Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
							mapOfPathDocuments = createMapOfPathDocuments(contactDocList, mapOfPathDocuments);
							assignDocsToRespectivePathProdApprovers(mapOfPathDocuments);
						}
					}
				}
			}
			else if (! newIsPool) { // If changed from pool to linear.
				log.debug("");
				contactDocList = new ArrayList<>();
				contactDocList = getContactDocumentDAO().findByProperty("approverId", poolApproverId);
				if (newFirstApprover != null) {
					for (ContactDocument cd : contactDocList) {
						cd.setApproverId(newFirstApprover.getId());
						cd.setIsDeptPool(false);
						ContactDocumentDAO.getInstance().attachDirty(cd);
					}
					if (priorFinalApp != null) {
						contactDocList = new ArrayList<>();
						contactDocList = getContactDocumentDAO().findByProperty("approverId", priorFinalApp.getId());
						for (ContactDocument cd : contactDocList) {
							// Get the top approver in production list
							Approver app = getTopApproverInChain(newFirstApprover);
							cd.setApproverId(app.getId());
							cd.setApprovalLevel(ApprovalLevel.PROD);
							ContactDocumentDAO.getInstance().attachDirty(cd);
						}
					}
				}
				else if (newFirstApprover == null && path != null) {
					Map<Department, Integer> mapOfDeptTopApproverIds = new HashMap<>();
					// Store Departments with their Approver group's topmost Approver's Id in a map.
					mapOfDeptTopApproverIds = getMapOfDeptGroupApproverIds(path, mapOfDeptTopApproverIds, true);
					assignDocumentsToRespectiveDeptApprovers(contactDocList, mapOfDeptTopApproverIds);
					if (priorFinalApp != null) {
						contactDocList = new ArrayList<>();
						contactDocList = getContactDocumentDAO().findByProperty("approverId", priorFinalApp.getId());
						assignDocumentsToRespectiveDeptApprovers(contactDocList, mapOfDeptTopApproverIds);
					}
				}
				else if (newFirstApprover == null && group != null) {
					Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
					mapOfPathDocuments = createMapOfPathDocuments(contactDocList, mapOfPathDocuments);
					assignDocsToRespectivePathProdApprovers(mapOfPathDocuments);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to assign Contact Documents to their Department Approvers if any.
	 *  If no Department Approvers found then it will set the status of those contact documentss to SUBMITTED_NO_APRROVERS.
	 * @param contactDocList
	 * @param mapOfDeptTopApproverIds
	 */
	private void assignDocumentsToRespectiveDeptApprovers(List<ContactDocument> contactDocList,
			Map<Department, Integer> mapOfDeptTopApproverIds) {
		try {
			if (contactDocList != null) {
				for (ContactDocument cd : contactDocList) {
					Department cdDept = cd.getEmployment().getDepartment();
					if (mapOfDeptTopApproverIds != null && cdDept != null && mapOfDeptTopApproverIds.containsKey(cdDept)) {
						cd.setApproverId(mapOfDeptTopApproverIds.get(cdDept));
						cd.setApprovalLevel(ApprovalLevel.DEPT);
						if (cd.getApproverId() < 0) {
							cd.setIsDeptPool(true);
						}
						log.debug(" Contact Document Id = " + cd.getId());
						log.debug(" Approver Id = " + cd.getApproverId());
					}
					else {
						cd.setApproverId(null);
						cd.setStatus(ApprovalStatus.SUBMITTED_NO_APPROVERS);
						log.debug(" Contact Document Id = " + cd.getId());
						log.debug(" Approver Id = " + cd.getApproverId());
					}
					getContactDocumentDAO().attachDirty(cd);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to assign Contact Documents to their ApprovalPath's Production Approvers if any.
	 *  If no Production Approvers found then it will set the status of those contact documentss to SUBMITTED_NO_APRROVERS.
	 * @param mapOfPathDocuments
	 */
	private void assignDocsToRespectivePathProdApprovers(Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments) {
		try {
			if (! mapOfPathDocuments.isEmpty()) {
				log.debug("Map Size = " + mapOfPathDocuments.size());
				for (ApprovalPath path : mapOfPathDocuments.keySet()) {
					log.debug("PATH = " + path.getId());
					List<ContactDocument> pathContactDocList = mapOfPathDocuments.get(path);
					if (pathContactDocList != null) {
						for (ContactDocument cd : pathContactDocList) {
							log.debug("Contact document = " + cd.getId());
							if (path.getUsePool()) {
								if (path.getApproverPool() != null && (! path.getApproverPool().isEmpty())) {
									Integer poolId = (-1)*(path.getId());
									cd.setApproverId(poolId);
									cd.setApprovalLevel(ApprovalLevel.PROD);
									log.debug(" Approver id = " + cd.getApproverId());
								}
								else if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
									cd.setApproverId(path.getFinalApprover().getId());
									cd.setApprovalLevel(ApprovalLevel.FINAL);
									log.debug(" Approver id = " + cd.getApproverId());
								}
								else {
									cd.setApproverId(null);
									cd.setStatus(ApprovalStatus.SUBMITTED_NO_APPROVERS);
									log.debug("Approver id with SUBMITTED_NO_PATH status = " + cd.getApproverId());
								}
							}
							else if (path.getApprover() != null) {
								cd.setApproverId(path.getApprover().getId());
								cd.setApprovalLevel(ApprovalLevel.PROD);
								log.debug(" Approver id = " + cd.getApproverId());
							}
							else {
								cd.setApproverId(null);
								cd.setStatus(ApprovalStatus.SUBMITTED_NO_APPROVERS);
								log.debug("Approver id with SUBMITTED_NO_PATH status = " + cd.getApproverId());
							}
							cd.setIsDeptPool(false);
							getContactDocumentDAO().attachDirty(cd);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to update the contact documents for move/replace/remove of Approvers.
	 * @param path
	 * @param group
	 */
	private void updateContactDouments(ApprovalPath path, ApproverGroup group) {
		try {
			log.debug("");
			List<ContactDocument> contactDocList = null;
			Integer poolApproverId = null;
			ApproverAuditType auditType = null;
			List<ApproverAudit> approverAuditList = null;
			Map<Department, Integer> mapOfDeptTopApproverIds = new HashMap<>();
			Map<String, Object> values= new HashMap<>();
			values.put("production", currentProduction);
			if (path != null) {
				log.debug("");
				// Store Departments with their Approver group's topmost Approver's Id in a map.
				mapOfDeptTopApproverIds = getMapOfDeptGroupApproverIds(path, mapOfDeptTopApproverIds, true);
				contactDocList = new ArrayList<>();
				// Update Documents of Production Pool
				if (path.getUsePool()) {
					log.debug("");
					poolApproverId = -(path.getId());
					List<ContactDocument> contactDocumentList = new ArrayList<>();
					// Fetch Audit for Final Approver in both the cases (getUseFinalApprover() true or false),
					// in case user has changed the radio button's selection for specific or first Approver.
					values.put("approvalPath", path);
					values.put("auditType", ApproverAuditType.FINAL);
					ApproverAudit deletedFinalApp = getApproverAuditDAO().findOneByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE, values);
					// Fetch documents waiting for Final Approver
					if (deletedFinalApp != null) {
						log.debug("");
						contactDocumentList = getContactDocumentDAO().findByProperty("approverId", deletedFinalApp.getOldApproverId());
						contactDocList.addAll(contactDocumentList);
					}
					// Fetch documents waiting for Pool Approvers
					if (path.getApproverPool() == null || path.getApproverPool().isEmpty()) {
						log.debug("");
						contactDocumentList = getContactDocumentDAO().findByProperty("approverId", poolApproverId);
						contactDocList.addAll(contactDocumentList);
					}
					if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
						log.debug("");
						for (ContactDocument cd : contactDocList) {
							cd.setApprovalLevel(ApprovalLevel.FINAL);
							cd.setApproverId(path.getFinalApprover().getId());
							getContactDocumentDAO().attachDirty(cd);
						}
					}
					// Case when user has changed the radio button's selection from specific/final approver to first Approver.
					else if ((! path.getUseFinalApprover()) && deletedFinalApp != null &&
							(! (path.getApproverPool() == null || path.getApproverPool().isEmpty()))) {
						log.debug("");
						for (ContactDocument cd : contactDocList) {
							cd.setApproverId(poolApproverId);
							cd.setApprovalLevel(ApprovalLevel.PROD);
							getContactDocumentDAO().attachDirty(cd);
						}
					}
					else {
						log.debug("");
						assignDocumentsToRespectiveDeptApprovers(contactDocList, mapOfDeptTopApproverIds);
					}
				}
				// Update Documents of Linear Path
				else {
					log.debug("");
					auditType = ApproverAuditType.PROD;
					values.put("approvalPath", path);
					values.put("auditType", auditType);
					approverAuditList = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE, values);
					if (approverAuditList != null && (! approverAuditList.isEmpty())) {
						for (ApproverAudit audit : approverAuditList) {
							if (path.getApprover() == null) {
								log.debug("");
								List<ContactDocument> contactDocumentList;
								contactDocumentList = getContactDocumentDAO().findByProperty("approverId", audit.getOldApproverId());
								contactDocList.addAll(contactDocumentList);
							}
							else {
								log.debug("");
								contactDocList = new ArrayList<>();
								contactDocList = getContactDocumentDAO().findByProperty("approverId", audit.getOldApproverId());
								Integer approverId = null;
								if (audit.getNewApproverId() != null) { //Handles move case, old Contact is in updated Path.
									log.debug("");
									approverId = audit.getNewApproverId();
								}
								// Handles Remove/ Replace case, old Contact is not in updated Path.
								else if (audit.getOldContact() != null) { // Prevents new Approver contacts in Audit where OldContact is NULL.
									//int position = audit.getPosition();
									// Contact removed or replaced
									log.debug("");
									if (audit.getNewContact() == null ||
											(audit.getNewContact() != null && (! audit.getOldContact().equals(audit.getNewContact())))) {
										approverId =  path.getApprover().getId();
										/*int newPos = position - 1;
										ApproverAudit appAudit = approverAuditList.get(newPos);
										// Find next non-deleted Approver.
										while (appAudit.getNewContact() == null) {
											newPos = newPos - 1;
											appAudit = approverAuditList.get(newPos);
										}
										if (appAudit != null) {
											approverId = appAudit.getNewContactApproverId();
										}
										else {
											// Here path.getApprover() is not null. It has been checked previously.
											approverId = path.getApprover().getId();
										}*/
									}
									// Contact Replaced
									/*if (audit.getNewContact() != null && (! audit.getOldContact().equals(audit.getNewContact()))) {
										// Replaced First Approver
										if (position == 1) {
											approverId = audit.getNewContactApproverId(); //or approverId = path.getApprover().getId();
										}
										// Replaced other Approver, set the ApproverId to First Approver.
										else {
											ApproverAudit appAudit = approverAuditList.get(position - 1);
											approverId = appAudit.getNewContactApproverId();
										}
									}*/
								}
								if (approverId != null) {
									log.debug("");
									for (ContactDocument cd : contactDocList) {
										cd.setApproverId(approverId);
										getContactDocumentDAO().attachDirty(cd);
									}
								}
							}
						}
						if (path.getApprover() == null && contactDocList != null) {
							log.debug("");
							assignDocumentsToRespectiveDeptApprovers(contactDocList, mapOfDeptTopApproverIds);
						}
					}
				}
			}
			else if (group != null) {
				if (group.getUsePool()) {
					log.debug("");
					poolApproverId = -(group.getId());
					if (group.getGroupApproverPool() == null || group.getGroupApproverPool().isEmpty()) {
						contactDocList = new ArrayList<>();
						contactDocList = getContactDocumentDAO().findByProperty("approverId", poolApproverId);
						Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
						mapOfPathDocuments = createMapOfPathDocuments(contactDocList, mapOfPathDocuments);
						assignDocsToRespectivePathProdApprovers(mapOfPathDocuments);
					}
				}
				else {
					log.debug("");
					contactDocList = new ArrayList<>();
					values.put("approverGroup", group);
					approverAuditList =  getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APP_GROUP_AUDIT_TYPE, values);
					if (approverAuditList != null && (! approverAuditList.isEmpty())) {
						for (ApproverAudit audit : approverAuditList) {
							if (group.getApprover() == null && audit.getOldApproverId() != null) {
								List<ContactDocument> contactDocumentList;
								contactDocumentList = getContactDocumentDAO().findByProperty("approverId", audit.getOldApproverId());
								contactDocList.addAll(contactDocumentList);
							}
							else {
								contactDocList = new ArrayList<>();
								if (audit.getOldApproverId() != null) {
									contactDocList = getContactDocumentDAO().findByProperty("approverId", audit.getOldApproverId());
								}
								Integer approverId = null;
								if (audit.getNewApproverId() != null) { //Handles move case, old Contact is in updated Path.
									approverId = audit.getNewApproverId();
								}
								// Handles Remove/ Replace case, old Contact is not in updated Approver Group.
								else if (audit.getOldContact() != null) { // Prevents new Approver contacts in Audit where OldContact is NULL.
									// Contact removed or replaced
									if (audit.getNewContact() == null ||
											(audit.getNewContact() != null && (! audit.getOldContact().equals(audit.getNewContact())))) {
										approverId =  group.getApprover().getId();
									}
								}
								if (approverId != null) {
									for (ContactDocument cd : contactDocList) {
										cd.setApproverId(approverId);
										getContactDocumentDAO().attachDirty(cd);
									}
								}
							}
						}
						if (group.getApprover() == null && contactDocList != null) {
							Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
							mapOfPathDocuments = createMapOfPathDocuments(contactDocList, mapOfPathDocuments);
							assignDocsToRespectivePathProdApprovers(mapOfPathDocuments);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to update the Contact Documents at Department level, for the departments
	 * that have no Anchor (or removed) in the updated ApprovalPath.
	 * @param path
	 */
	private void updateDeptContactDocuments(ApprovalPath path) {
		try {
			List<Department> newDeptList = new ArrayList<>();
			List<Integer> deletedDeptIdList = new ArrayList<>();
			List<ApproverGroup> deletedDeptGroupList = new ArrayList<>();
			List<Integer> approverIdList = new ArrayList<>();
			List<ContactDocument> contactDocList = new ArrayList<>();
			// Fetch all the audits for Department anchors.
			Map<String, Object> values= new HashMap<>();
			values.put("production", currentProduction);
			values.put("approvalPath", path);
			List<ApproverAudit> approverAuditList = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PATH_DEPT_AUDIT_TYPE, values);

			// Fetch all the new Department anchors.
			List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath", path);
			if (anchorList != null) {
				for (ApprovalPathAnchor anchor : anchorList) {
					newDeptList.add(anchor.getDepartment());
				}
			}

			if (approverAuditList != null) {
				// Search the removed Department anchors in updated path.
				for (ApproverAudit audit : approverAuditList) {
					if (audit.getDepartment() != null && (! newDeptList.contains(audit.getDepartment()))) {
						deletedDeptIdList.add(audit.getDepartment().getId());
						if (audit.getApproverGroup() != null && (! deletedDeptGroupList.contains(audit.getApproverGroup()))) {
							deletedDeptGroupList.add(audit.getApproverGroup());
						}
					}
				}
			}

			// Store the Approver Ids, the removed Department anchors were following.
			for (ApproverGroup group : deletedDeptGroupList) {
				if (group.getUsePool()) {
					Integer poolId = -(group.getId());
					approverIdList.add(poolId);
				}
				else {
					List<Approver> appList = new ArrayList<>();
					appList = getApproverChain(group.getApprover(), appList);
					for (Approver app : appList) {
						approverIdList.add(app.getId());
					}
				}
			}

			// Map for list of Approver ids and Department ids.
			Map<String, List<Integer>> values2= new HashMap<>();
			values2.put("approverIds", approverIdList);
			values2.put("departmentIds", deletedDeptIdList);
			values = new HashMap<>();
			if (! (approverIdList.isEmpty() || deletedDeptIdList.isEmpty())) {
				if (currentProduction.getType().isAicp()) {
					values.put("project", path.getProject());
					contactDocList = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PROJECT, values, values2);
				}
				else {
					values.put("production", currentProduction);
					contactDocList = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS_DEPT_IDS_PRODUCTION, values, values2);
				}
			}
			if (contactDocList != null) {
				for (ContactDocument cd : contactDocList) {
					cd.setApproverId(null);
					cd.setStatus(ApprovalStatus.SUBMITTED_NO_APPROVERS);
					cd.setIsDeptPool(false);
					getContactDocumentDAO().attachDirty(cd);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method Creates a map of Department and ApproverId (Pool's/ First Approver's/ Last Approver's ApproverId)
	 *  of the Approver Group followed by that Department.
	 * @param path ApprovalPath
	 * @param mapOfDeptApproverIds
	 * @param storeTopOrBottom True if we want to store the highest approver's id else false for lowest approver.
	 * @return
	 */
	private Map<Department, Integer> getMapOfDeptGroupApproverIds(ApprovalPath path, Map<Department,
			Integer> mapOfDeptApproverIds, boolean storeTopOrBottom) {
		try {
			List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath", path);
			if (anchorList != null) {
				mapOfDeptApproverIds = new HashMap<>();
				for (ApprovalPathAnchor anchor : anchorList) {
					Integer groupApproverId = null;
					ApproverGroup appGroup = anchor.getApproverGroup();
					if (appGroup != null) {
						appGroup = getApproverGroupDAO().refresh(appGroup);
						log.debug("Approver Group = " + appGroup.getId() + ", " + appGroup.getGroupName());
						if (appGroup.getUsePool() && appGroup.getGroupApproverPool() != null && (! appGroup.getGroupApproverPool().isEmpty())) {
							groupApproverId = (-1)*(appGroup.getId());
							log.debug("Group Approver Id = " + groupApproverId);
						}
						else if (appGroup.getApprover() != null) {
							log.debug("");
							if (storeTopOrBottom) { // Store highest Approver's Approver id.
								// Get the highest Approver.
								Approver app = getTopApproverInChain(appGroup.getApprover());
								groupApproverId = app.getId();
								log.debug("Group Approver Id = " + groupApproverId);
							}
							else {
								groupApproverId = appGroup.getApprover().getId();
								log.debug("Group Approver Id = " + groupApproverId);
							}
						}
						if (groupApproverId != null) {
							mapOfDeptApproverIds.put(anchor.getDepartment(), groupApproverId);
							log.debug(" Department = " + anchor.getDepartment().getName());
							log.debug(" Approver Id = " + groupApproverId);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return mapOfDeptApproverIds;
	}

	/** Method used to assign Approvers to the Contact Documents of the newly added Document chains and
	 * to change the status of ContactDocuments from SUBMITTED_NO_APRROVERS to OPEN.
	 * @param path Updated ApprovalPath.
	 * @param group Updated ApproverGroup.
	 */
	private void assignApproversToContactDocuments(ApprovalPath path, ApproverGroup appGroup) {
		try {
			log.debug("");
			if (path != null) {
				Integer prodApproverId = null;
				Map<Department, Integer> mapOfDeptApproverIds = null;
				ApprovalLevel level = null;
				if (path.getUsePool()) {
					if (path.getApproverPool() != null && (! path.getApproverPool().isEmpty())) {
						prodApproverId = (-1)*(path.getId());
						level = ApprovalLevel.PROD;
					}
					else if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
						prodApproverId = path.getFinalApprover().getId();
						level = ApprovalLevel.FINAL;
					}
				}
				else {
					if (path.getApprover() != null) {
						prodApproverId = path.getApprover().getId();
						level = ApprovalLevel.PROD;
					}
				}
				log.debug(" prodApproverId = " + prodApproverId);
				mapOfDeptApproverIds = new HashMap<>();
				mapOfDeptApproverIds = getMapOfDeptGroupApproverIds(path, mapOfDeptApproverIds, false);

				/*List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath", path);
				if (anchorList != null) {
					mapOfDeptApproverIds = new HashMap<>();
					for (ApprovalPathAnchor anchor : anchorList) {
						Integer groupApproverId = null;
						ApproverGroup group = anchor.getApproverGroup();
						if (group != null) {
							if (group.getUsePool() && group.getGroupApproverPool() != null && (! group.getGroupApproverPool().isEmpty())) {
								groupApproverId = (-1)*(group.getId());
							}
							else if (group.getApprover() != null) {
								groupApproverId = group.getApprover().getId();
							}
							if (groupApproverId != null) {
								mapOfDeptApproverIds.put(anchor.getDepartment(), groupApproverId);
								log.debug(" Department = " + anchor.getDepartment().getName());
								log.debug(" Approver Id = " + groupApproverId);
							}
						}
					}
				}*/

				List<ContactDocument> contactDocList = new ArrayList<>();
				if (path.getDocumentChains() != null && (! path.getDocumentChains().isEmpty())) {
					for (DocumentChain docChain : path.getDocumentChains()) {
						List<ContactDocument> contactDocuments = new ArrayList<>();
						Map<String, Object> values = new HashMap<>();
					    values.put("documentChainId", docChain.getId());
					    if (currentProduction.getType().isAicp()) {
					    	values.put("project", SessionUtils.getCurrentProject());
					    	contactDocuments = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS_PROJECT, values);
					    	if (contactDocuments != null && (! contactDocuments.isEmpty())) {
					    		contactDocList.addAll(contactDocuments);
					    	}
					    }
					    else {
						    values.put("production", currentProduction);
						    contactDocuments = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_SUBMITTED_NO_APPROVERS_STATUS, values);
							if (contactDocuments != null && (! contactDocuments.isEmpty())) {
					    		contactDocList.addAll(contactDocuments);
					    	}
					    }
					}
					if (contactDocList != null && (! contactDocList.isEmpty())) {
						log.debug("Contact document list size : " + contactDocList.size());
						for (ContactDocument cd : contactDocList) {
							log.debug("Contact document Id = " + cd.getId());
							if (cd.getApprovalLevel() == ApprovalLevel.PROD && prodApproverId != null) {
								cd.setApproverId(prodApproverId);
								cd.setApprovalLevel(level);
								cd.setStatus(ApprovalStatus.SUBMITTED);
								log.debug(" Approver Id = " + cd.getApproverId());
							}
							else if (cd.getApprovalLevel() == ApprovalLevel.FINAL && prodApproverId != null) {
								if (prodApproverId < 0 ||  path.getUsePool()) {
									if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
										cd.setApproverId(path.getFinalApprover().getId());
										cd.setStatus(ApprovalStatus.SUBMITTED);
									}
									else if (prodApproverId < 0) {
										cd.setApprovalLevel(ApprovalLevel.PROD);
										cd.setApproverId(prodApproverId);
										cd.setStatus(ApprovalStatus.SUBMITTED);
									}
								}
								else {
									Approver app = getTopApproverInChain(path.getApprover());
									cd.setApproverId(app.getId());
									cd.setApprovalLevel(ApprovalLevel.PROD);
									cd.setStatus(ApprovalStatus.SUBMITTED);
								}
								//cd.setStatus(ApprovalStatus.SUBMITTED);
								log.debug(" Approver Id = " + cd.getApproverId());
							}
							else if (cd.getApprovalLevel() == ApprovalLevel.DEPT || prodApproverId == null ||
										cd.getApprovalLevel() == ApprovalLevel.FINAL) {
								Department cdDept = cd.getEmployment().getDepartment();
								if (cdDept != null && mapOfDeptApproverIds != null && mapOfDeptApproverIds.containsKey(cdDept)) {
									cd.setApproverId(mapOfDeptApproverIds.get(cdDept));
									cd.setStatus(ApprovalStatus.SUBMITTED);
									cd.setApprovalLevel(ApprovalLevel.DEPT);
									if (cd.getApproverId() < 0) {
										cd.setIsDeptPool(true);
									}
									log.debug(" Approver Id = " + cd.getApproverId());
								}
								else if (prodApproverId != null) {
									cd.setApproverId(prodApproverId);
									cd.setStatus(ApprovalStatus.SUBMITTED);
									cd.setApprovalLevel(level);
									cd.setIsDeptPool(false);
									log.debug(" Approver Id = " + cd.getApproverId());
								}
							}
							getContactDocumentDAO().attachDirty(cd);
						}
					}
				}
			}
			else if (appGroup != null) {
				List<ContactDocument> cdList = new ArrayList<>();
				cdList = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_OF_DEPT_LEVEL_BY_PRODUCTION_STATUS, map("production", currentProduction));
				Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
				mapOfPathDocuments = createMapOfPathDocuments(cdList, mapOfPathDocuments);
				for (ApprovalPath appPath : mapOfPathDocuments.keySet()) {
					Integer approverId = null;
					if (appGroup.getUsePool()) {
						if (appGroup.getGroupApproverPool() != null && (! appGroup.getGroupApproverPool().isEmpty())) {
							approverId = -(appGroup.getId());
						}
					}
					else if (appGroup.getApprover() != null) {
						approverId = appGroup.getApprover().getId();
					}
					if (approverId != null) {
						List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath", appPath);
						if (anchorList != null) {
							for (ApprovalPathAnchor anchor : anchorList) {
								if (anchor.getApproverGroup() != null && anchor.getApproverGroup().equals(appGroup)) {
									Department dept = anchor.getDepartment();
									for (ContactDocument cd : cdList) {
										if (cd.getEmployment().getDepartment() != null &&
												cd.getEmployment().getDepartment().equals(dept)) {
											cd.setApproverId(approverId);
											cd.setStatus(ApprovalStatus.SUBMITTED);
											cd.setApprovalLevel(ApprovalLevel.DEPT);
											if (appGroup.getUsePool()) {
												cd.setIsDeptPool(true);
											}
											else {
												cd.setIsDeptPool(false);
											}
										}
										getContactDocumentDAO().attachDirty(cd);
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to refresh the view on Distribution and review tab.
	 */
	private void refreshDistReviewTab() {
		log.debug("");
		StatusListBean bean = StatusListBean.getInstance();
		bean.setContactDocumentList(null);
		bean.clearCurrentContactChainMap();
		bean.clearStatusGraphs();
	}

	/*private boolean isEmptyPath(ApprovalPath path, Department dept) {
		if (path != null) {
			if (path.getUsePool()) {
				if (path.getApproverPool() != null && (! path.getApproverPool().isEmpty())) {
					return false;
				}
				else if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
					return false;
				}
			}
			else {
				if (path.getApprover() != null) {
					return false;
				}
			}
			if (dept != null) {
				ApprovalPathAnchor anchor = ApproverUtils.getApprovalPathAnchor(SessionUtils.getCurrentOrViewedProject(), dept, path.getId());
				if (anchor != null && anchor.getApproverGroup() != null && anchor.getApproverGroup().getApprover() != null) {
					return false;
				}
			}
		}
		return false;
	}
*/

	/** Method deletes all the associations of a Approval Path (approvers, anchors and contact pool)
	 * @param path The approval path whose associations need to be removed from the database.
	 */
	private void deleteApproversAndAnchor(ApprovalPath path, Approver firstPathApprover) {
		try  {
			// check for production approver list
			List<Approver> list = new ArrayList<>();
			if (firstPathApprover != null) {
				deleteApprover(firstPathApprover, list, path, null);
			}
			//path.setApprover(null);

			// check for department approver list find approval anchor and then approvers
			List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath", path);

			if (anchorList != null) {
				for (ApprovalPathAnchor anchor : anchorList) {
					if (anchor.getApproverGroup() != null) {
						ApproverAudit app = new ApproverAudit(currentProduction, ApproverAuditType.PATH_ANCHOR,
								anchor.getDepartment(), path, anchor.getApproverGroup(), null, null, null);
						getApproverAuditDAO().save(app);
					}
					getApprovalPathAnchorDAO().delete(anchor); // delete the anchor
				}
			}

			// check for contact pool if exists clear the Set
			if (path.getApproverPool() != null && (! path.getApproverPool().isEmpty())) {
				log.debug("");
				List<Integer> removePermissions= new ArrayList<>();
				for (Contact ct : path.getApproverPool()) {
					removePermissions.add(ct.getId());
				}
				path.getApproverPool().clear();
				getApprovalPathDAO().attachDirty(path);

				for (Integer id : removePermissions) {
					Contact ct = getContactDAO().findById(id);
					addRemovePermission(ct, false);
				}
			}

			if (path.getUseFinalApprover() && path.getFinalApprover() != null) {
				log.debug("");
				Approver finalApp = path.getFinalApprover();
				addRemovePermission(finalApp.getContact(), false);
				ApproverAudit app = new ApproverAudit(currentProduction, ApproverAuditType.FINAL, null, path,
						null, null, finalApp.getId(), finalApp.getContact());
				getApproverAuditDAO().save(app);
				path.setFinalApprover(null);
				getApproverDAO().delete(finalApp);
				log.debug("Deleting Final Approver");
				getApprovalPathDAO().attachDirty(path);
			}

			// check for Editors if exists clear the Set
			if (path.getEditors() != null && (! path.getEditors().isEmpty())) {
				List<Integer> removePermissions= new ArrayList<>();
				for (Contact ct : path.getEditors()) {
					removePermissions.add(ct.getId());
				}
				path.getEditors().clear();
				getApprovalPathDAO().attachDirty(path);
				for (Integer id : removePermissions) {
					Contact ct = getContactDAO().findById(id);
					addRemovePermission(ct, false);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method deletes the chain of Approvers for the given First Approver.
	 * It also creates Approver Audits for the Approver to be deleted.
	 * @param approver First Approver.
	 * @param list Blank list of Approvers.
	 * @param path ApprovalPath approvers belongs to, if approvers are production approvers.
	 * @param appGroup ApproverGroup approvers belongs to, if approvers are Department/Group approvers.
	 */
	private void deleteApprover(Approver approver, List<Approver> list, ApprovalPath path, ApproverGroup appGroup) {
		try {
			list = getApproverChain(approver, list);
			int pos = 1;
			ApproverAuditType type = null;
			if (path != null) {
				type = ApproverAuditType.PROD;
			}
			else {
				type = ApproverAuditType.APP_GROUP;
			}
			for (Approver approverToDelete : list) {
				ApproverAudit app = new ApproverAudit(currentProduction, type, null, path, appGroup, pos,
						approverToDelete.getId(), approverToDelete.getContact());
				getApproverAuditDAO().save(app);
				getApproverDAO().delete(approverToDelete);
				addRemovePermission(approverToDelete.getContact(), false);
				pos++;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method creates a linear Production Approver chain for the path from the
	 * current {@link #productionApproverList}.  It takes the first person in the
	 * list as the first approver and last person in list will have its next
	 * approver as NULL. It will also give all Contacts in the list the "Approve
	 * start docs" permission. (Refactored by LS-3661.)
	 *
	 * @param path The Approval Path for which the chain is built.
	 */
	private void createProductionApprovers(ApprovalPath path) {
		try {
			List<Integer> contactIdList = new ArrayList<Integer>();
			for (SelectItem item : productionApproverList) {
				Integer contactId = Integer.parseInt(item.getValue().toString());
				contactIdList.add(contactId);
			}
			createProductionApprovers(path, contactIdList);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method creates a linear Production Approver chain for the path from the given
	 * list. It takes the first person in the list as the first approver and
	 * last person in list will have its next approver as NULL. It will also
	 * give all Contacts in the list the "Approve start docs" permission.
	 * (Refactored by LS-3661.)
	 *
	 * @param path The Approval Path for which the chain is built.
	 * @param contactIdList The List of integers whose value is the Contact.id
	 *            for the Contact's to be included in the Approver chain.
	 */
	private void createProductionApprovers(ApprovalPath path, List<Integer> contactIdList) {
		try {
			Approver approver = null;
			Integer savedApprover = null;
			for (Integer contactId : contactIdList) {
				Contact contact = getContactDAO().findById(contactId);
				approver = new Approver();
				approver.setContact(contact);
				if (savedApprover != null) {
					Approver priorApprover = getApproverDAO().findById(savedApprover);
					if (priorApprover != null) {
						approver.setNextApprover(priorApprover);
					}
				}
				else {
					approver.setNextApprover(null);
				}
				approver.setShared(true);
				savedApprover = getApproverDAO().save(approver);
				addRemovePermission(contact, true); // ensure approver has Permission
			}
			// put the last approver in approval path
			path.setApprover(approver);
			getApprovalPathDAO().attachDirty(path);
			// To save new approver id for deleted approvers
			updateApproverAuditEntries(path, null, path.getApprover(), null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method called when the user clicks "Cancel" on the Approval Paths page
	 * (to exit edit mode). Refreshes the path view(s).
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		try {
			ApprovalPath path = getApprovalPathDAO().findById(selectedApprovalPathId);
			createApprovalPathDefaultView(path);
			return super.actionCancel();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method created the approver instances from the list of department approvers
	 *  and also creates the approver anchor from the first record.
	 * @param path approval path for which approvers and anchor are created
	 * @param deptList
	 * @param group
	 */
	private void createDepartmentAnchors(ApprovalPath path, ApproverGroup group, List<Department> deptList) {
		// create anchors for the list of Departments
		try {
			ApprovalPathAnchor anchor = null;
			for (Department dept : deptList) {
				log.debug("Department = " + dept.getId() + ", " + dept.getName());
				anchor = new ApprovalPathAnchor();
				anchor.setApprovalPath(path);
				anchor.setDepartment(dept);
				anchor.setProduction(currentProduction);
				if (aicp) {
					anchor.setProject(SessionUtils.getCurrentProject());
				}
				anchor.setApproverGroup(group);
				anchor.setApprover(null);
				getApprovalPathAnchorDAO().attachDirty(anchor);
				log.debug("Anchor saved = " + anchor.getId());
			}
			updateApproverAuditEntries(path, group, null, deptList);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to set the new approver ids of the deleted approvers if they are in the modified path.
	 * @param path Approval Path of the new approver belongs
	 * @param appGroup Approver Group of the new approver belongs
	 * @param newFirstApprover New First Approver of Path / Anchor.
	 * @param deptList List of Departments for the anchors in updated path.
	 */
	private void updateApproverAuditEntries(ApprovalPath path, ApproverGroup appGroup, Approver newFirstApprover, List<Department> deptList) {
		try {
			log.debug("");
			ApproverAuditType auditType = null;
			List<Approver> newApprovers = new ArrayList<>();
			newApprovers = getApproverChain(newFirstApprover, newApprovers);
			List<ApproverAudit> oldApprovers = new ArrayList<>();;
			Map<String, Object> values = new HashMap<>();
			values.put("production", currentProduction);
			if (path != null) { // For Production Appprovers
				auditType = ApproverAuditType.PROD;
				values.put("approvalPath", path);
				values.put("auditType", auditType);
				// Production Approvers
				log.debug("");
				oldApprovers = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APPROVAL_PATH_AUDIT_TYPE, values);
			}
			else if (deptList == null) { // For Approver Groups
				auditType = ApproverAuditType.APP_GROUP;
				values.put("approverGroup", appGroup);
				// Group Approvers
				log.debug("");
				oldApprovers = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PROD_APP_GROUP_AUDIT_TYPE, values);
			}
			else if (deptList != null) { // For Approval Path Anchors
				Map<String, List<Integer>> values2 = new HashMap<>();
				List<Integer> deptIdList = new ArrayList<>();
				for (Department dept : deptList) {
					deptIdList.add(dept.getId());
				}
				values2.put("department", deptIdList);
				values.put("approvalPath", path);
				log.debug("");
				oldApprovers = getApproverAuditDAO().findByNamedQuery(ApproverAudit.GET_DELETED_APPROVERS_BY_PATH_DEPT_AUDIT_TYPE, values, values2);
			}

			if (deptList == null) {
				Integer pos = 1;
				for (Approver newApp : newApprovers) {
					boolean foundPosition = false;
					boolean foundApprover = false;
					for (ApproverAudit oldApp : oldApprovers) {
						if (oldApp.getOldContact() != null && oldApp.getOldContact().equals(newApp.getContact())) {
							log.debug("*** FOUND ***" );
							foundApprover = true;
							log.debug(" OLD = " + oldApp.getOldApproverId());
							log.debug(" NEW = " + newApp.getId());
							oldApp.setNewApproverId(newApp.getId());
							if (pos.equals(oldApp.getPosition())) {
								foundPosition = true;
								oldApp.setNewContact(newApp.getContact());
								oldApp.setNewContactApproverId(newApp.getId());
								getApproverAuditDAO().attachDirty(oldApp);
								break;
							}
							getApproverAuditDAO().attachDirty(oldApp);
						}
						if ((! foundPosition) && pos.equals(oldApp.getPosition())) {
							oldApp.setNewContact(newApp.getContact());
							oldApp.setNewContactApproverId(newApp.getId());
							getApproverAuditDAO().attachDirty(oldApp);
						}
					}
					if ((! foundApprover) && (! foundPosition) && (newApprovers.size() > oldApprovers.size())) {
						log.debug("**** NOT FOUND (New Approver) **** " + newApp.getId() +", "+ newApp.getContact().getDisplayName());
						ApproverAudit app = new ApproverAudit(currentProduction, auditType, null, path, appGroup, pos, null, null);
						app.setNewContact(newApp.getContact());
						app.setNewContactApproverId(newApp.getId());
						getApproverAuditDAO().save(app);
					}
					pos++;
				}

				/*if (log.isDebugEnabled()) {
					for (ApproverAudit oldApp : oldApprovers) {
						if (oldApp.getNewApproverId() == null) {
							if (oldApp.getNewContact() != null) {
								log.debug("*** CONTACT REPLACED ***" + oldApp.getOldContact().getDisplayName() );
							}
							else {
								log.debug("*** CONTACT REMOVED ***" + oldApp.getOldContact().getDisplayName() );
							}
						}
					}
				}*/
			}
			else { // Update record for Anchors, if thier Approver group is null it means that anchors are removed in the updated path.
				if (deptList != null && (! deptList.isEmpty())) {
					for (ApproverAudit oldApp : oldApprovers) {
						if (deptList.contains(oldApp.getDepartment())) {
							log.debug("*** Department ***" + oldApp.getDepartment().getId());
							oldApp.setApproverGroup(appGroup);
							getApproverAuditDAO().attachDirty(oldApp);
						}
					}
				}

			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for master-checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenCheckedForAll(ValueChangeEvent evt) {
		try {
			if (getCheckedForAll()) {
				for (DocumentChain docChain : documentChainList) {
					if (! docChain.getDisabled()) {
						docChain.setChecked(getCheckedForAll());
						checkBoxSelectedItems.add(docChain);
					}
				}
			}
			else {
				checkBoxSelectedItems.clear();
				checkedForAll = false;
				for (DocumentChain docChain : documentChainList) {
					docChain.setChecked(false);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenSingleCheck (ValueChangeEvent evt) {
		try {
			DocumentChain docChain = (DocumentChain) evt.getComponent().getAttributes().get("selectedRow");
			if (docChain.getChecked()) {
				checkBoxSelectedItems.add(docChain);
			}
			else if (checkBoxSelectedItems.contains(docChain)) {
				checkBoxSelectedItems.remove(docChain);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for Approval Path Change event
	 * @param evt
	 */
	public void listenApprovalPathChange(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			log.debug("Approval Path id: " + id);
			if (id == null || id < 0) {
				selectedApprovalPathId = null;
			}
			else {
				selectedApprovalPathId = id;
			}
			//mapOfDepartmentApprovers.clear();
			checkBoxSelectedItems.clear();
			mapOfAppGroupDepartment.clear();
			getSelectedDeptList().clear();
			updateDepartments(getDepartmentList(), false, false);
			/*for (ApproverGroup group : mapOfAppGroupDepartment.keySet()) {
				List<Department> deptList = mapOfAppGroupDepartment.get(group);
				for (Department dept : deptList) {
					dept.setSelected(false);
					dept = DepartmentDAO.getInstance().refresh(dept);
				}
			}*/
			if (selectedApprovalPathId == null) {
	//			isDefaultView = true;
			}
			else {
				productionApproverList = new ArrayList<>();
				documentEditorList = new ArrayList<>();
	//			isDefaultView = false;
				ApprovalPath path = getApprovalPathDAO().findById(selectedApprovalPathId);
				SessionUtils.put(Constants.ATTR_ONBOARDING_APPROVAL_PATH_ID, selectedApprovalPathId);
				createApprovalPathDefaultView(path);
				refreshMemberItems();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for Instruction view change event
	 * @param evt
	 */
	public void listenChangeInstructionView(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				String view = (String) evt.getNewValue();
				setViewType(view);
				log.debug("view type = " + getViewType());
				UserPrefBean.getInstance().put(Constants.ATTR_INSTR_VIEW_PREF, getViewType());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for select / unselect event of final approver's radio buttons.
	 * @param evt
	 */
	public void listenChangeFinalApprover(ValueChangeEvent evt) {
		try {
			String newVal = (String)evt.getNewValue();
			log.debug("Value = " + newVal);
			if (newVal.equals(FINAL_APPROVER)) {
				finalPoolApprover = FINAL_APPROVER;
			}
			else if (newVal.equals(FIRST_APPROVER)) {
				finalPoolApprover = FIRST_APPROVER;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to create the default view for the selected approval path.
	 * @param approvalPath
	 */
	private void createApprovalPathDefaultView(ApprovalPath approvalPath) {
		try {
			log.debug("");
			if (approvalPath != null) {
				setCurrentApprovalPath(approvalPath);
				boolean usePool = approvalPath.getUsePool();
				boolean canada = currentProduction.getType().isCanadaTalent();
				if (canada) {
					usePool = true;
				}
				if (usePool) {
					setApprovalMethod(APPROVER_POOL);
					if (approvalPath.getUseFinalApprover()) {
						setFinalPoolApprover(FINAL_APPROVER);
						if (approvalPath.getFinalApprover() != null) {
							setFinalApproverId(approvalPath.getFinalApprover().getContact().getId());
						}
						else {
							setFinalApproverId(null);
						}
					}
					else {
						setFinalPoolApprover(FIRST_APPROVER);
						setFinalApproverId(null);
					}
					setProductionApproverList(createListFromSet(approvalPath.getApproverPool()));
				}
				else {
					setApprovalMethod(LINEAR_HIERARCHY);
					setProductionApproverList(createLinearProductionApproverList(approvalPath));
				}
				if (approvalPath.getEditors() != null && (! approvalPath.getEditors().isEmpty())) {
					setDocumentEditorList(createListFromSet(approvalPath.getEditors()));
				}
				else {
					documentEditorList = new ArrayList<>();
				}
				log.debug("");
				mapOfAppGroupDepartment.clear();
				getSelectedDeptList().clear();
				updateDepartments(getDepartmentList(), false, false);
				createMapOfAppGroupDepartment(approvalPath);
				if (selectedAppGroupId == null) {
					selectedAppGroupId = (Integer) getAppGroupNameList().get(0).getValue();
				}
				if (selectedAppGroupId != null) {
					groupApproverList = createGroupApproverList(selectedAppGroupId);
				}
			}
			documentChainList = null; // for chain refresh.
			// iterate over path's chain to check them
			checkDocumentChain(approvalPath);
			disableDocumentChain();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @param approvalPath
	 */
	private void checkDocumentChain(ApprovalPath approvalPath) {
		try {
			for (DocumentChain chain : getDocumentChainList()) {
				chain.setChecked(false);
			}
			if (approvalPath != null && approvalPath.getDocumentChains() != null) {
				log.debug("current approval path name" + approvalPath.getName());
				for (DocumentChain chain : approvalPath.getDocumentChains()) {
					int entry = getDocumentChainList().indexOf(chain);
					if (entry >= 0) {
						DocumentChain listChain = getDocumentChainList().get(entry);
						listChain.setChecked(true);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to check the corresponding document chain belonging
	 * to the current approval path selected from the drop down
	 */
	@SuppressWarnings("rawtypes")
	private void disableDocumentChain() {
		try {
	//		List<DocumentChain> documentChainSet = createDocumentChainListByProd();
			List<DocumentChain> documentChainSet = getDocumentChainList();
			for (DocumentChain chain : documentChainSet) {
				if(! chain.getChecked()) {
					if (SessionUtils.getProduction().getType().isAicp()) {
						Iterator itr = chain.getApprovalPath().iterator();
						while (itr.hasNext()) {
							ApprovalPath path = (ApprovalPath) itr.next();
							if (SessionUtils.getCurrentProject().equals(path.getProject())) {
								chain.setDisabled(true);
							}
						}
					}
					else {
						if(chain.getApprovalPath().size() > 0) {
							chain.setDisabled(true);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method creates the Production Approvers list for a path when the approval type is Linear Hierarchy,
	 * for the approval pool case we use the Set<Contacts>
	 * @param approvalPath the path for which the production approver belong
	 * @return select item list of production approvers
	 */
	private List<SelectItem> createLinearProductionApproverList(ApprovalPath approvalPath) {
		List<SelectItem> linearProductionApproverList = new ArrayList<>();
		try {
			Approver firstProductionApprover = approvalPath.getApprover();
			if (firstProductionApprover != null) {
				log.debug("first production approver" + firstProductionApprover.getId());
				List<Approver> list = new ArrayList<>();
				list = getApproverChain(firstProductionApprover, list);
				for(int i = list.size()-1; i >= 0; i--) {
					Contact newContact = list.get(i).getContact();
					User user = newContact.getUser();
					String label = user.getLastNameFirstName();
					String apprLabel = ""; // (Permission.APPROVE_START_DOCS.inMask(newContact.getPermissionMask()) ? APPROVER_INDICATOR : "");
					label = label + " - " + (newContact.getRoleName()) + apprLabel;
					linearProductionApproverList.add(new SelectItem(newContact.getId(), label));
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return linearProductionApproverList;
	}

	/** Method creates the list of production approvers for the corresponding approval path selected
	 * @param contactSet Set of contacts of the selected approval path
	 * @return list of production approvers
	 */
	public List<SelectItem> createListFromSet(Set<Contact> contactSet) {
		List<SelectItem> approverList = new ArrayList<>();
		try {
			if (contactSet != null) {
				Enumeration<Contact> e = Collections.enumeration(contactSet);
				while (e.hasMoreElements()) {
					Contact newContact = e.nextElement();
					User user = newContact.getUser();
					String label = user.getLastNameFirstName();
					String apprLabel = ""; // (Permission.APPROVE_START_DOCS.inMask(newContact.getPermissionMask()) ? APPROVER_INDICATOR : "");
					label = label + " - " + (newContact.getRoleName()) + apprLabel;
					approverList.add(new SelectItem(newContact.getId(), label));
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approverList;
	}

	/** Method creates the map of Approver Groups and their Departments for the selected path.
	 * @param approvalPath selected ApprovalPath.
	 * @return Map of ApproverGroup and Departments.
	 */
	private Map<ApproverGroup, List<Department>> createMapOfAppGroupDepartment(ApprovalPath approvalPath) {
		mapOfAppGroupDepartment = new HashMap<>();
		try {
			if (approvalPath != null && approvalPath.getId() != null) {
				log.debug("");
				List<ApprovalPathAnchor> pathAnchorList = getApprovalPathAnchorDAO().findByProperty("approvalPath",
						approvalPath);
				if (pathAnchorList != null) {
					ApproverGroup groupKey = null;
					for (ApprovalPathAnchor groupAnchor : pathAnchorList) {
						log.debug("groupAnchor." + groupAnchor.getId());
						//Fetch distinct Approver groups in anchors.
						if (groupKey == null) {
							groupKey = groupAnchor.getApproverGroup();
							if (groupKey != null) {
								log.debug("Group Key = " + groupKey.getGroupName());
							}
						}
						if (groupKey != null && (!mapOfAppGroupDepartment.containsKey(groupKey))) {
							log.debug("Group Key = " + groupKey.getGroupName());
							List<Department> departments = new ArrayList<>();
							for (ApprovalPathAnchor anchor : pathAnchorList) {
								if (anchor.getApproverGroup() != null && anchor.getApproverGroup().equals(groupKey)) {
									departments.add(anchor.getDepartment());
								}
							}
							if (!departments.isEmpty()) {
								mapOfAppGroupDepartment.put(groupKey, departments);
								log.debug("Size of mapOfAppGroupDepartment = " + mapOfAppGroupDepartment.size());
							}
						}
						groupKey = null;
					}
					log.debug("Size of mapOfAppGroupDepartment = " + mapOfAppGroupDepartment.size());
					if (! mapOfAppGroupDepartment.isEmpty()) {
						selectedApproverGroup = mapOfAppGroupDepartment.keySet().iterator().next();
						if (selectedApproverGroup != null) {
							log.debug("selectedApproverGroup = " + selectedApproverGroup.getGroupName());
							setSelectedAppGroupId(selectedApproverGroup.getId());
							getSelectedDeptList().addAll(mapOfAppGroupDepartment.get(selectedApproverGroup));
							checkDepartments(selectedApproverGroup);
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return mapOfAppGroupDepartment;
	}

	/** Utility method to check Departments for the given approver group.
	 * @param appGroup ApproverGroup
	 */
	private void checkDepartments(ApproverGroup appGroup) {
		try {
			log.debug("");
			for (Department dept : getDepartmentList()) {
				dept.setSelected(false);
			}
			if (mapOfAppGroupDepartment != null && (! mapOfAppGroupDepartment.isEmpty())) {
				List<Department> deptList = mapOfAppGroupDepartment.get(appGroup);
				if (deptList != null) {
					updateDepartments(deptList, true, null);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to disable the departments that belongs
	 * to the other Approver Groups.
	 * @param appGroup ApproverGroup
	 */
	private void disableDepartments(ApproverGroup appGroup) {
		try {
			log.debug("");
			List<Department> disabledDeptList = new ArrayList<>();
			for (ApproverGroup key : mapOfAppGroupDepartment.keySet()) {
				log.debug("Group = " + key.getGroupName());
				if (! key.equals(appGroup)) {
					disabledDeptList.addAll(mapOfAppGroupDepartment.get(key));
				}
			}
			if (! disabledDeptList.isEmpty()) {
				updateDepartments(disabledDeptList, null, true);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Utility method used to Select/Unselect and Disable/Enable
	 * the checkboxes of the Departments in the original DepartmentList.
	 * @param deptList List of departments to be updated.
	 * @param selected Value for the selected field of Departments. If null, no change to the selected field.
	 * @param disabled Value for the disabled field of Departments. If null, no change to the disabled field.
	 */
	private void updateDepartments(List<Department> deptList, Boolean selected, Boolean disabled) {
		try {
			if (deptList != null) {
				log.debug("Dept = " + deptList.size() + ", Selected = " + selected + ", Disabled = " + disabled);
				for (Department dept : deptList) {
					dept = DepartmentDAO.getInstance().refresh(dept);
					int entry = getDepartmentList().indexOf(dept);
					if (entry >= 0) {
						Department listDept = getDepartmentList().get(entry);
						if (listDept != null) {
							log.debug("Dept = " + listDept.getName());
							if (selected != null) {
								listDept.setSelected(selected);
							}
							if (disabled != null) {
								listDept.setDisabled(disabled);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Utility method used to get the approver chain for a department
	 * @param approver first approver (anchor approver)
	 * @param list list of approvers
	 * @return list
	 */
	private List<Approver> getApproverChain(Approver approver, List<Approver> list) {
		try {
			if (approver != null) {
				list.add(approver);
				if (approver.getNextApprover() != null) {
					getApproverChain(approver.getNextApprover(), list);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return list;
	}

	/** Utility method used to get the Topmost Approver in chain
	 * for the given First Approver in the chain.
	 * @param approver first approver
	 * @return Topmost Approver in approver hierarchy
	 */
	private Approver getTopApproverInChain(Approver approver) {
		try {
			if (approver != null) {
				approver = getApproverDAO().refresh(approver);
				while (approver.getNextApprover() != null) {
					approver = approver.getNextApprover();
				}
				return approver;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Utility method used to create a set of contacts from the given select
	 * item list
	 *
	 * @param list The list of SelectItems, where the value is a contact id.
	 * @return contactSet The set of Contact's represented in the
	 *         SelectItemList.
	 */
	private Set<Contact> createContactSet(List<SelectItem> list) {
		Set<Contact> contactSet = new LinkedHashSet<>();
		try {
			if(! list.isEmpty()) {
				for (SelectItem item : list) {
					Integer contactId = Integer.parseInt(item.getValue().toString());
					Contact contact = getContactDAO().findById(contactId);
					contactSet.add(contact);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return contactSet;
	}

	/**
	 * See {@link #documentChainList}. Note that this method is called by the
	 * ICEfaces framework code even when the table using it is NOT being
	 * rendered! So we avoid filling this table until the 'initialized' boolean
	 * is true by {@link #init()}.
	 */
	public List<DocumentChain> getDocumentChainList() {
		if (initialized && documentChainList == null) {
			documentChainList = createDocumentChainListByProd();
			Collections.sort(documentChainList);
		}
		return documentChainList;
	}
	/**See {@link #documentChainList}. */
	public void setDocumentChainList(List<DocumentChain> documentChainList) {
		this.documentChainList = documentChainList;
	}

	/**
	 * A Comparator used for sorting the DocumentChain entries by name.
	 */
	protected Comparator<DocumentChain> documentNameComparator = new Comparator<DocumentChain>() {
		@Override
		public int compare(DocumentChain d1, DocumentChain d2) {
			return d1.compareTo(d2, "");
		}
	};

	/**See {@link #approvalPathNameList}. */
	public List<SelectItem> getApprovalPathNameList() {
		if (approvalPathNameList == null) {
			approvalPathNameList = createApprovalPathNameList();
			if (approvalPathNameList.get(0).getLabel().toString().equalsIgnoreCase(Constants.SELECT_OR_CREATE_APPROVAL_PATH)) {
//				isDefaultView = true;
			}
			else {
//				isDefaultView = false;
			}
		}
		return approvalPathNameList;
	}

	/** Method used to add/remove the Approve start docs permission for a contact.
	 * @param contact
	 * @param addOrRemove True, if the method is called to add permission. False for removing the permission.
	 */
	private void addRemovePermission(Contact contact, boolean addOrRemove) {
		try {
			AuthorizationBean bean = AuthorizationBean.getInstance();
			// True , if the contact is approver for another path also.
			boolean isApprover = false;
			boolean hasPermission = Permission.APPROVE_START_DOCS.inMask(contact.getPermissionMask()) ||
					bean.hasPermission(contact, (Permission.APPROVE_START_DOCS));
			if (! hasPermission && addOrRemove) { // addOrRemove, true for add
				contact.setPermissionMask(contact.getPermissionMask() | Permission.APPROVE_START_DOCS.getMask());
				getContactDAO().attachDirty(contact);
			}
			else if (hasPermission && (! addOrRemove)) {
				ApprovalPath currentPath = getApprovalPathDAO().findById(selectedApprovalPathId);
				List<ApprovalPath> allProdApprovalPath = getApprovalPathDAO().findByProperty("production", currentProduction);
				if (allProdApprovalPath != null) {
					for (ApprovalPath path : allProdApprovalPath) {
						if (! path.equals(currentPath)) {
							isApprover = ApproverUtils.isContactInPath(path, contact, null);
							if (isApprover) {
								break;
							}
						}
					}
					log.debug("Number of Approval Paths in Production : " + allProdApprovalPath.size());
				}
				// Search if contact is a part of an Approver Group that does not belong to any approval path.
				if (! isApprover) {
					ApproverGroup currentGroup = getApproverGroupDAO().findById(selectedAppGroupId);
					List<ApproverGroup> allProdApproverGroups = getApproverGroupDAO().findByProperty("production", currentProduction);
					if (allProdApproverGroups != null) {
						for (ApproverGroup group : allProdApproverGroups) {
							if (! group.equals(currentGroup)) {
								if (group.getUsePool()) {
									if (group.getGroupApproverPool() != null) {
										if (group.getGroupApproverPool().contains(contact)) {
											isApprover = true;
										}
									}
								}
								else {
									Approver app = group.getApprover();
									while (app != null) { // follow each anchor's chain and add all the approver contacts
										if (app.getContact().equals(contact)) {
											isApprover = true;
										}
										app = app.getNextApprover();
									}
								}
								if (isApprover) {
									break;
								}
							}
						}
					}
				}
				if (! isApprover) {
					contact.setPermissionMask(contact.getPermissionMask() & (~ Permission.APPROVE_START_DOCS.getMask()));
					getContactDAO().attachDirty(contact);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method used to create the list of approver groups for the current
	 * Production or Project. The value field (of the SelectItem) is the database
	 * id of the ApproverGroups.
	 *
	 * @return list of approver groups for a drop-down list.
	 */
	private List<SelectItem> createGroupNameList() {
		appGroupNameList = new ArrayList<>();
		try {
			Production production = SessionUtils.getProduction();
			List<ApproverGroup> list = new ArrayList<>();
			if (production.getType().isAicp()) {
				list = getApproverGroupDAO().findByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_BY_PROJECT, map("project", SessionUtils.getCurrentProject()));
			}
			else {
				list = getApproverGroupDAO().findByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_BY_PRODUCTION, map("production", SessionUtils.getProduction()));
			}
			if (list.size() > 0) {
				log.debug("list size returned... "+list.size());
				for (ApproverGroup group : list) {
					appGroupNameList.add(new SelectItem(group.getId(), group.getGroupName()));
				}
				Collections.sort(appGroupNameList, getSelectItemComparator());
				selectedAppGroupId = (Integer)appGroupNameList.get(0).getValue();
			}
			else {
				appGroupNameList.add(0, new SelectItem(-1, Constants.SELECT_OR_CREATE_APPROVER_GROUP));
				selectedAppGroupId = null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return appGroupNameList;
	}

	/** Action method used to open the create pop up, for creating
	 * a new approver group.
	 * @return null
	 */
	public String actionCreateApproverGroup() {
		setApproverGroupName("New Group");
		groupApproverList = new ArrayList<>();
		log.debug("");
		actionOpenSelectApprovers(ACT_CREATE_GROUP, "ApprovalPathsBean.AddGroupApprovers.Title", groupApproverList, true, false);
		return null;
	}

	/** Action method used to open the Approver Group pop up, for editing
	 * the existing approver group.
	 * @return null
	 */
	public String actionEditApproverGroup() {
		if (selectedAppGroupId != null) {
			ApproverGroup group = getApproverGroupDAO().findById(selectedAppGroupId);
			setApproverGroupName(group.getGroupName());
			if (group.getUsePool()) {
				setGroupApprovalMethod(APPROVER_POOL);
			}
			else {
				setGroupApprovalMethod(LINEAR_HIERARCHY);
			}
		}
		log.debug("");
		actionOpenSelectApprovers(ACT_SELECT_APP, "ApprovalPathsBean.AddGroupApprovers.Title", getGroupApproverList(), true, false);
		return null;
	}

	/** Action method used to open the select pop up, for adding new approver to the Production.
	 * @return null
	 */
	public String actionAddProductionApprovers() {
		log.debug("");
		actionOpenSelectApprovers(ACT_ADD_PROD_APP, "ApprovalPathsBean.AddProdApprovers.Title", getProductionApproverList(), false, true);
		return null;
	}

	/** Action method used to open the select pop up, for adding new approver to the Document Editors.
	 * @return null
	 */
	public String actionAddEditors() {
		log.debug("");
		actionOpenSelectApprovers(ACT_ADD_EDITOR, "ApprovalPathsBean.AddEditors.Title", getDocumentEditorList(), false, false);
		return null;
	}

	/**
	 * @return Action string for faces navigation
	 */
	public String actionOpenSelectApprovers(int action, String msgId, List<SelectItem> list, boolean isApproverGroup, boolean isProductionApprover) {
		log.debug("");
		try {
			setUnselectedMemberItems(null);
			selectedMemberItems = new ArrayList<>();
			if (list != null) {
				selectedMemberItems.addAll(list);
				approverContactIds = new ArrayList<>();
				approverContactIds = getContactIds(list);
				if (getUnselectedMemberItems() != null && (! getUnselectedMemberItems().isEmpty()) && (! approverContactIds.isEmpty())) {
					log.debug("");
					Iterator<SelectItem> itr = unselectedMemberItems.iterator();
					while(itr.hasNext()) {
						SelectItem item = itr.next();
						if (approverContactIds.contains(item.getValue())) {
							log.debug(" REMOVE = " + item.getLabel());
							itr.remove();
						}
					}
				}
			}
			SelectContactsBean.getInstance().show(action, this, msgId, isApproverGroup, isProductionApprover); // control returns via the contactsSelected() method
			addFocus("groupName");
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Populate {@link #approverContactIds} with the Contact ids derived from the
	 * given list of SelectItem`s. The value field of each SelectItem should be
	 * an Approver.id value.
	 * @return
	 */
	private List<Integer> getContactIds(List<SelectItem> items) {
		try {
			if(items != null && approverContactIds != null) {
				for (SelectItem filterItem : items) {
					log.debug("Item = " + filterItem.getValue() + ", " + filterItem.getLabel());
					approverContactIds.add((Integer)filterItem.getValue());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approverContactIds;
	}

	@Override
	public void contactsSelected(int action, Collection<Contact> list) {
		Set<Contact> contacts = new LinkedHashSet<>();
		switch(action) {
		case ACT_CREATE_GROUP:
			if (list != null) { // For action Cancel
				contacts = getSelectedAppContacts();
				actionCreateApproverGroupOk(contacts);
			}
			break;
		case ACT_SELECT_APP:
			if (list != null) {
				contacts = getSelectedAppContacts();
				actionEditApproverGroupOk(contacts);
			}
			break;
		case ACT_ADD_PROD_APP:
			if (list != null) {
				actionAddProductionApproversOk();
			}
			break;
		case ACT_ADD_EDITOR:
			actionAddEditorsOk();
			break;
		}
	}

	/** ActionOk method invoked when the user clicks "Save" on the Approver Group pop up for "New" button.
	 * @param contacts
	 * @return null
	 */
	private String actionCreateApproverGroupOk(Set<Contact> contacts) {
		try{
			log.debug("");
			SelectContactsBean selectBean = SelectContactsBean.getInstance();
			// Restrict user to have at least one Approver in new Approver Group.
			if (contacts == null || contacts.isEmpty()) {
				log.debug("");
				selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.NoGroupApprovers"));
				return null;
			}
			if (approverGroupName != null) {
				log.debug("");
				int approverGroupId = 0;
				ApproverGroup approverGroup = null;
				Map<String, Object> values = new HashMap<>();
				values.put("groupName", approverGroupName);
				if (currentProduction.getType().isAicp()) {
					values.put("project", SessionUtils.getCurrentProject());
					approverGroup = getApproverGroupDAO().findOneByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_NAME_BY_PROJECT, values);
					if (approverGroup != null) {
						selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.DuplicateGroupName"));
						return null;
					}
					else {
						selectBean.setInputError(false);
						approverGroupId = getApproverGroupDAO().createApproverGroup(approverGroupName, currentProduction, SessionUtils.getCurrentProject(), false);
					}
				}
				else {
					values.put("production", currentProduction);
					approverGroup = getApproverGroupDAO().findOneByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_BY_NAME_AND_PRODUCTION, values);
					if (approverGroup != null) {
						selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.DuplicateGroupName"));
						return null;
					}
					else {
						selectBean.setInputError(false);
						approverGroupId = getApproverGroupDAO().createApproverGroup(approverGroupName, currentProduction, SessionUtils.getCurrentProject(), false);
					}
					log.debug("New approver group with Id and name:" + approverGroupId + " " + approverGroupName);
				}
				if (approverGroupId != 0) {
					if (approverGroup == null) {
						approverGroup = getApproverGroupDAO().findById(approverGroupId);
					}
					if (approverGroup != null) {
						if (getGroupApprovalMethod().equals(APPROVER_POOL)) {
							approverGroup.setUsePool(true);
							approverGroup.setGroupApproverPool(contacts);
							// Make sure all approvers have special permission
							for (Contact ct : approverGroup.getGroupApproverPool()) {
								addRemovePermission(ct, true);
							}
							getApproverGroupDAO().attachDirty(approverGroup);
						}
						else {
							//For linear hierarchy
							approverGroup.setUsePool(false);
							createGroupApprovers(contacts, approverGroup);
						}
					}
					appGroupNameList = null;
					getAppGroupNameList();
					if (getSelectedDeptList() != null && (! getSelectedDeptList().isEmpty()) &&
							getMapOfAppGroupDepartment() != null && selectedApproverGroup != null) {
						log.debug("selectedApproverGroup = " + selectedApproverGroup.getGroupName());
						mapOfAppGroupDepartment.put(selectedApproverGroup, new ArrayList<>(getSelectedDeptList()));
						updateDepartments(getSelectedDeptList(), false, true);
					}
					setSelectedAppGroupId(approverGroupId);
					selectedApproverGroup = approverGroup;
					getSelectedDeptList().clear();
					groupApproverList = createGroupApproverList(approverGroupId);
					List<ApproverAudit> groupAudits = getApproverAuditDAO().findByProperty("approverGroup", selectedApproverGroup);
					for (ApproverAudit appRecord : groupAudits) {
						getApproverAuditDAO().delete(appRecord);
					}
					log.debug("**** DELETED AUDIT FOR APPROVER GROUP ****");

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
	 * Method creates the Production Approver chain for the path, it takes the
	 * first person in the list as the first approver and last person in list
	 * will have its next approver as NULL. It will also give all Contacts in
	 * the list the "Approve start docs" permission.
	 *
	 * @param path The Approval Path for which the chain is build
	 */
	private void createGroupApprovers(Set<Contact> contacts, ApproverGroup approverGroup) {
		try {
			if (contacts != null && (! contacts.isEmpty())) {
				Integer savedApprover = null;
				Approver approver = null;
				for (Contact contact : contacts) {
					approver = new Approver();
					approver.setContact(contact);
					if (savedApprover != null) {
						Approver priorApprover = getApproverDAO().findById(savedApprover);
						if (priorApprover != null) {
							approver.setNextApprover(priorApprover);
						}
					}
					else {
						approver.setNextApprover(null);
					}
					approver.setShared(true);
					savedApprover = getApproverDAO().save(approver);
					addRemovePermission(contact, true); // ensure approver has Permission
					log.debug("Saved Contact = " + contact.getId() + ", " + contact.getDisplayName() + " as an approver");
				}
				// put the last approver in approverGroup
				approverGroup.setApprover(approver);
				getApproverGroupDAO().attachDirty(approverGroup);
			}
			// To save new approver id for deleted approvers
			updateApproverAuditEntries(null, approverGroup, approverGroup.getApprover(), null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** ActionOk method invoked when the user clicks "Save" on the Approver Group pop up for "Edit Group" button.
	 * @param contacts
	 * @return null
	 */
	private String actionEditApproverGroupOk(Set<Contact> contacts) {
		try{
			log.debug("");
			SelectContactsBean selectBean = SelectContactsBean.getInstance();
			// Restrict user to have at least one Approver in new Approver Group.
			/*if (contacts == null || contacts.isEmpty()) {
				log.debug("");
				selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.NoGroupApprovers"));
				return null;
			}*/
			selectedApproverGroup = getApproverGroupDAO().refresh(selectedApproverGroup);
			if (selectedApproverGroup != null) {
				if (approverGroupName != null && (! approverGroupName.equals(selectedApproverGroup.getGroupName()))) {
					ApproverGroup approverGroup = null;
					Map<String, Object> values = new HashMap<>();
					values.put("groupName", approverGroupName);
					if (currentProduction.getType().isAicp()) {
						values.put("project", SessionUtils.getCurrentProject());
						approverGroup = getApproverGroupDAO().findOneByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_NAME_BY_PROJECT, values);
						if (approverGroup != null) {
							selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.DuplicateGroupName"));
							return null;
						}
						else {
							selectBean.setInputError(false);
							selectedApproverGroup.setGroupName(approverGroupName);
							appGroupNameList = null;
							getAppGroupNameList();
							selectedAppGroupId = selectedApproverGroup.getId();
						}
					}
					else {
						values.put("production", currentProduction);
						approverGroup = getApproverGroupDAO().findOneByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_BY_NAME_AND_PRODUCTION, values);
						if (approverGroup != null) {
							selectBean.displayError(MsgUtils.getMessage("ApprovalPathsBean.DuplicateGroupName"));
							return null;
						}
						else {
							selectBean.setInputError(false);
							selectedApproverGroup.setGroupName(approverGroupName);
							appGroupNameList = null;
							getAppGroupNameList();
							selectedAppGroupId = selectedApproverGroup.getId();
						}
						log.debug("approver group with Id and name:" + selectedApproverGroup.getId() + " " + approverGroupName);
					}
				}
				Approver firstGroupApprover = selectedApproverGroup.getApprover();
				selectedApproverGroup.setApprover(null);
				deleteGroupApprovers(selectedApproverGroup, firstGroupApprover);
				boolean priorWasPool = selectedApproverGroup.getUsePool();
				if (getGroupApprovalMethod().equals(APPROVER_POOL)) {
					selectedApproverGroup.setUsePool(true);
					selectedApproverGroup.setGroupApproverPool(contacts);
					// Make sure all approvers have special permission
					for (Contact ct : selectedApproverGroup.getGroupApproverPool()) {
						addRemovePermission(ct, true);
					}
					getApproverGroupDAO().attachDirty(selectedApproverGroup);
				}
				else {
					//For linear hierarchy
					selectedApproverGroup.setUsePool(false);
					createGroupApprovers(contacts, selectedApproverGroup);
				}
				groupApproverList = createGroupApproverList(selectedAppGroupId);
				selectedApproverGroup = getApproverGroupDAO().refresh(selectedApproverGroup);
				// 2. Check for Change of Approval Method for Production Approvers
				if (selectedApproverGroup.getUsePool() && (! priorWasPool)) { // Changed from Linear to Pool
					changeApprovalMethodForDocuments(null, selectedApproverGroup, true, null, null);
				}
				else if ((! selectedApproverGroup.getUsePool()) && priorWasPool) { // Changed from Pool to Linear
					changeApprovalMethodForDocuments(null, selectedApproverGroup, false, selectedApproverGroup.getApprover(), null);
				}

				// 1. Update ContactDocuments if Path is Updated (Add/ Remove/ Replace/ Move Approvers).
				updateContactDouments(null, selectedApproverGroup);

				// 2. Update contact documents of newly Added Document Chains
				assignApproversToContactDocuments(null, selectedApproverGroup);

				// Delete all the ApproverAudits for the ApprovalPath
				List<ApproverAudit> groupAudits = getApproverAuditDAO().findByProperty("approverGroup", selectedApproverGroup);
				for (ApproverAudit appRecord : groupAudits) {
					getApproverAuditDAO().delete(appRecord);
				}
				log.debug("**** DELETED AUDIT FOR APPROVER GROUP ****");
				refreshDistReviewTab();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method deletes all the associations of ApproverGroup (approvers and contact pool)
	 * @param group The ApproverGroup whose associations need to be removed from the database.
	 * @param firstGroupApprover
	 */
	private void deleteGroupApprovers(ApproverGroup group, Approver firstGroupApprover) {
		try {
			// check for Group approver list
			List<Approver> list = new ArrayList<>();
			if (firstGroupApprover != null) {
				deleteApprover(firstGroupApprover, list, null, group);
			}
			// check for contact pool if exists clear the Set
			if (group.getGroupApproverPool() != null && (! group.getGroupApproverPool().isEmpty())) {
				log.debug("");
				List<Integer> removePermissions= new ArrayList<>();
				for (Contact ct : group.getGroupApproverPool()) {
					removePermissions.add(ct.getId());
				}
				group.getGroupApproverPool().clear();
				getApproverGroupDAO().attachDirty(group);
				for (Integer id : removePermissions) {
					Contact ct = getContactDAO().findById(id);
					addRemovePermission(ct, false);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Action method used to open the delete Approver Group pop up, for deleting
	 * an Approver Group.
	 * @return null
	 */
	public String actionDeleteApproverGroup() {
		if (selectedApproverGroup != null) {
			PopupBean bean = PopupBean.getInstance();
			log.debug("");
			bean.show(this, ACT_DELETE_GROUP, "ApprovalPathsBean.DeleteApproverGroup.");
		}
		else {
			MsgUtils.addFacesMessage("ApprovalPathsBean.SelectApproverGroupForDelete", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** ActionOk method invoked when the user clicks "Delete" on the delete Approver Group pop up
	 * @return null
	 */
	private String actionDeleteApproverGroupOk() {
		try {
			if (selectedApproverGroup != null) {
				List<ContactDocument> prodContactDocList = new ArrayList<>();
				Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments = new HashMap<>();
				if (selectedApproverGroup.getUsePool()) {
					Integer approverId = (-1)*(selectedApproverGroup.getId());
					log.debug("POOL = " + approverId);
					if (approverId != null) {
						prodContactDocList = getContactDocumentDAO().findByProperty("approverId", approverId);
					}
				}
				else if (selectedApproverGroup.getApprover() != null) {
					log.debug("LINEAR First app = " + selectedApproverGroup.getApprover());
					List<Approver> approversList = new ArrayList<>();
					approversList = getApproverChain(selectedApproverGroup.getApprover(), approversList);
					List<Integer> approverIdList = new ArrayList<>();
					for (Approver app : approversList) {
						if (app.getId() != null) {
							approverIdList.add(app.getId());
						}
					}
					if (! approverIdList.isEmpty()) {
						prodContactDocList = getContactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOC_LIST_BY_APPROVER_IDS, "approverIds", approverIdList);
					}
				}
				mapOfPathDocuments = createMapOfPathDocuments(prodContactDocList, mapOfPathDocuments);
				assignDocsToRespectivePathProdApprovers(mapOfPathDocuments);
				Approver firstApprover = selectedApproverGroup.getApprover();
				selectedApproverGroup = getApproverGroupDAO().refresh(selectedApproverGroup);
				List<ApprovalPathAnchor> anchorList = getApprovalPathAnchorDAO().findByProperty("approverGroup", selectedApproverGroup);
				for (ApprovalPathAnchor anchor : anchorList) {
					log.debug("Deleting Approval Path Anchor = " + anchor.getId());
					getApprovalPathAnchorDAO().delete(anchor);
				}
				selectedApproverGroup.setApprover(null);
				deleteGroupApprovers(selectedApproverGroup, firstApprover);
				if (getMapOfAppGroupDepartment() != null && getMapOfAppGroupDepartment().containsKey(selectedApproverGroup)) {
					getMapOfAppGroupDepartment().remove(selectedApproverGroup);
				}
				log.debug("Deleting Approver Group = " + selectedApproverGroup.getId());
				getApproverGroupDAO().delete(selectedApproverGroup);
			}
			updateDepartments(getSelectedDeptList(), false, false);
			getSelectedDeptList().clear();
			groupApproverList = null;
			selectedAppGroupId = null;
			selectedApproverGroup = null;
			appGroupNameList = null;
			if (getAppGroupNameList() != null && getGroupApproverList() != null &&
					getSelectedAppGroupId() != null && getSelectedApproverGroup() != null) {
				if (getMapOfAppGroupDepartment().containsKey(getSelectedApproverGroup())) {
					List<Department> deptList = new ArrayList<>();
					deptList = mapOfAppGroupDepartment.get(selectedApproverGroup);
					setSelectedDeptList(deptList);
					updateDepartments(deptList, true, false);
				}
			}
			refreshDistReviewTab();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method used to store ContactDocuments ApprovalPath wise.
	 * It will create a map of Approval Path and its ContactDocuments.
	 * @param prodContactDocList
	 * @param mapOfPathDocuments
	 * @return
	 */
	private Map<ApprovalPath, List<ContactDocument>> createMapOfPathDocuments(List<ContactDocument> contactDocList,
			Map<ApprovalPath, List<ContactDocument>> mapOfPathDocuments) {
		try {
			if (contactDocList != null) {
				for (ContactDocument cd : contactDocList) {
					log.debug("Contact document = " + cd.getId());
					ApprovalPath path = ContactDocumentDAO.getCurrentApprovalPath(cd, null, null);
					if (mapOfPathDocuments.containsKey(path) && mapOfPathDocuments.get(path) != null) {
						mapOfPathDocuments.get(path).add(cd);
					}
					else {
						List<ContactDocument> pathContactDocList = new ArrayList<>();
						pathContactDocList.add(cd);
						mapOfPathDocuments.put(path, pathContactDocList);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return mapOfPathDocuments;
	}

	/** ActionOk method invoked when the user clicks "Save" on the Production Approvers pop up for "Add Approver" button.
	 * @return  null navigation string
	 */
	private String actionAddProductionApproversOk() {
		try{
			log.debug("");
			if (getSelectedMemberItems() != null) {
				productionApproverList = new ArrayList<>();
				productionApproverList.addAll(selectedMemberItems);
				setSelectedMemberItems(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** ActionOk method invoked when the user clicks "Save" on the Document Editors pop up for "Add Editor" button.
	 * @return  null navigation string
	 */
	private String actionAddEditorsOk() {
		try{
			log.debug("");
			if (getSelectedMemberItems() != null) {
				documentEditorList = new ArrayList<>();
				documentEditorList.addAll(selectedMemberItems);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}


	/**
	 * The Action method for one of the line-item Add buttons (e.g., "+" icon),
	 * indicating the given item should be moved from the {@link #unselectedMemberItems}
	 * list to the {@link #selectedMemberItems} list.
	 */
	public void listenAddApprover(ActionEvent event) {
		try {
			log.debug("");
			UIData addApprover = (UIData)event.getComponent().findComponent("unselectedAppTable"); // id of data table in tcExpense
			Integer row = null;
			if(addApprover != null) {
				row = addApprover.getRowIndex();
				log.debug("<><> Row <><> " + row);
			}
			if (getSelectedMemberItems() == null) {
				selectedMemberItems = new ArrayList<>();
			}
			else if (row != null) {
				SelectItem item = getUnselectedMemberItems().get(row);
				if (item != null) {
					log.debug("UNSELECTED SIZE = " + getUnselectedMemberItems().size());
					getUnselectedMemberItems().remove(item);
					log.debug("UNSELECTED SIZE = " + getUnselectedMemberItems().size());
					getSelectedMemberItems().add(item);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * The Action method for one of the line-item Remove buttons (e.g., "X" icon),
	 * indicating the given item should be moved from the {@link #selectedMemberItems}
	 * list to the {@link #unselectedMemberItems} list.
	 */
	public void listenRemoveApprover(ActionEvent event) {
		try {
			log.debug("");
			UIData removeApprover = (UIData)event.getComponent().findComponent("selectedAppTable"); // id of data table in tcExpense
			Integer row = null;
			if(removeApprover != null) {
				row = removeApprover.getRowIndex();
				log.debug("<><> Row <><> " + row);
			}
			if (getUnselectedMemberItems() == null) {
				unselectedMemberItems = new ArrayList<>();
			}
			else if (row != null) {
				SelectItem item = getSelectedMemberItems().get(row);
				if (item != null) {
					log.debug("SELECTED SIZE = " + getUnselectedMemberItems().size());
					getSelectedMemberItems().remove(item);
					log.debug("SELECTED SIZE = " + getUnselectedMemberItems().size());
					getUnselectedMemberItems().add(item);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * The Action method for one of the Approver's Remove buttons (e.g., "X" icon),
	 * indicating the given Approver should be removed from the approver list.
	 */
	public void listenRemoveApproverFromTable(ActionEvent event) {
		try {
			String tableId = event.getComponent().getClientId();
			log.debug("Table Id = " + tableId);
			String tableName = null;
			UIData removeApprover = null;
			Integer row = null;
			if (tableId.contains("groupApprovers")) {
				log.debug("");
				tableName = "groupApprovers";
				removeApprover = (UIData)event.getComponent().findComponent(tableName);
				if(removeApprover != null) {
					row = removeApprover.getRowIndex();
					log.debug("<><> Row <><> " + row);
					if (getGroupApproverList() != null  && getGroupApproverList().size() > row) {
						SelectItem item = getGroupApproverList().get(row);
						if (item != null) {
							log.debug("SELECTED SIZE = " + getGroupApproverList().size());
							getGroupApproverList().remove(item);
							log.debug("SELECTED SIZE = " + getGroupApproverList().size());
						}
					}
				}
			}
			else if (tableId.contains("prodApprovers")) {
				log.debug("");
				tableName = "prodApprovers";
				removeApprover = (UIData)event.getComponent().findComponent(tableName);
				if(removeApprover != null) {
					row = removeApprover.getRowIndex();
					log.debug("<><> Row <><> " + row);
					if (getProductionApproverList() != null && getProductionApproverList().size() > row) {
						SelectItem item = getProductionApproverList().get(row);
						if (item != null) {
							log.debug("SELECTED SIZE = " + getProductionApproverList().size());
							getProductionApproverList().remove(item);
							log.debug("SELECTED SIZE = " + getProductionApproverList().size());
						}
					}
				}
			}
			else if (tableId.contains("docEditors")) {
				log.debug("");
				tableName = "docEditors";
				removeApprover = (UIData)event.getComponent().findComponent(tableName);
				if(removeApprover != null) {
					row = removeApprover.getRowIndex();
					log.debug("<><> Row <><> " + row);
					if (getDocumentEditorList() != null && getDocumentEditorList().size() > row) {
						SelectItem item = getDocumentEditorList().get(row);
						if (item != null) {
							log.debug("SELECTED SIZE = " + getDocumentEditorList().size());
							getDocumentEditorList().remove(item);
							log.debug("SELECTED SIZE = " + getDocumentEditorList().size());
						}
					}
				}
			}
			log.debug("Table Name = " + tableName);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**  Method used to extract the Set of selected Contacts from the SelectedMemberItems list . */
	private Set<Contact> getSelectedAppContacts() {
		Set<Contact> contacts = new LinkedHashSet<>();
		try {
			if (getSelectedMemberItems() != null && (! getSelectedMemberItems().isEmpty())) {
				for (SelectItem item : getSelectedMemberItems()) {
					Integer id = (Integer) item.getValue();
					if (id > 0) {
						log.debug("Contact = " + item.getLabel());
						Contact contact = getContactDAO().findById(id);
						log.debug("Contact = " + contact.getId() + ", " + contact.getDisplayName());
						contacts.add(contact);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return contacts;
	}

	/** Method creates the Approvers list for a Approver Group.
	 * @param approverGroupId the Id of the Approver Group.
	 * @return select item list of group approvers
	 */
	private List<SelectItem> createGroupApproverList(Integer approverGroupId) {
		List<SelectItem> approverList = new ArrayList<>();
		try {
			ApproverGroup approverGroup = getApproverGroupDAO().findById(approverGroupId);
			if (approverGroup != null) {
				setSelectedApproverGroup(approverGroup);
				if (approverGroup.getUsePool()) {
					approverList = createListFromSet(approverGroup.getGroupApproverPool());
				}
				else {
					Approver firstGroupApprover = approverGroup.getApprover();
					if (firstGroupApprover != null) {
						log.debug("first group approver" + firstGroupApprover.getId());
						List<Approver> list = new ArrayList<>();
						list = getApproverChain(firstGroupApprover, list);
						for(int i = list.size()-1; i >= 0; i--) {
							Contact newContact = list.get(i).getContact();
							User user = newContact.getUser();
							String label = user.getLastNameFirstName();
							String apprLabel = ""; // (Permission.APPROVE_START_DOCS.inMask(newContact.getPermissionMask()) ? APPROVER_INDICATOR : "");
							label = label + " - " + (newContact.getRoleName()) + apprLabel;
							approverList.add(new SelectItem(newContact.getId(), label));
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approverList;
	}

	/** Method creates a Map of ApproverGroups and list of their Approvers.
	 * @param approvalPath Selected Approval Path.
	 * @return mapOfAppGroupApprovers
	 */
	/*@SuppressWarnings("static-access")
	private Map<Integer, List<SelectItem>> createAppGroupApproversMap(ApprovalPath approvalPath) {
		mapOfAppGroupApprovers = new HashMap<>();
		if (approvalPath != null) {
			List<ApproverGroup> appGroupList = getApproverGroupDAO().getInstance().findByNamedQuery(
					ApproverGroup.GET_DISTINCT_APPROVER_GROUP_BY_APPROVAL_PATH, map("approvalPath", approvalPath));
			if (appGroupList != null) {
				List<SelectItem> approverList;
				for (ApproverGroup appGroup : appGroupList) {
					log.debug(">>>>>>. group = " + appGroup.getGroupName());
					approverList = createGroupApproverList(appGroup.getId());
					if (approverList != null && (! approverList.isEmpty())) {
						mapOfAppGroupApprovers.put(appGroup.getId(), approverList);
					}
				}
			}
		}
		return mapOfAppGroupApprovers;
	}*/

	/** Value change listener for Approver Group Change event.
	 * @param evt
	 */
	public void listenApproverGroupChange(ValueChangeEvent evt) {
		try {
			Integer id = (Integer)evt.getNewValue();
			log.debug("Approver Group id: " + id);
			if (id == null || id < 0) {
				selectedAppGroupId = null;
			}
			else {
				selectedAppGroupId = id;
			}
			if (selectedAppGroupId != null) {
				groupApproverList = new ArrayList<>();
				log.debug("AppGroupDepartment map size Before = " + getMapOfAppGroupDepartment().size());
				//Currently selectedApproverGroup has previously selected Approver Group. It will be changed in createGroupApproverList().
				if (selectedApproverGroup != null && getSelectedDeptList() != null &&
						(! getSelectedDeptList().isEmpty()) && getMapOfAppGroupDepartment() != null ) {
					log.debug("selectedApproverGroup = " + selectedApproverGroup.getGroupName());
					mapOfAppGroupDepartment.put(selectedApproverGroup, new ArrayList<>(getSelectedDeptList()));
					updateDepartments(getSelectedDeptList(), false, true);
				}
				/*else if (getSelectedDeptList().isEmpty() && getMapOfAppGroupDepartment().containsKey(selectedApproverGroup)) {
					getMapOfAppGroupDepartment().remove(selectedApproverGroup);
				}*/
				log.debug("AppGroupDepartment map size After = " + mapOfAppGroupDepartment.size());
				groupApproverList = createGroupApproverList(selectedAppGroupId);
				log.debug("selectedApproverGroup = " + selectedApproverGroup.getGroupName());
				getSelectedDeptList().clear();
				/*if (log.isDebugEnabled()) {
					for (ApproverGroup key : mapOfAppGroupDepartment.keySet()) {
						log.debug("Group = " + key.getGroupName());
						List<Department> approverList = mapOfAppGroupDepartment.get(key);
						if (! approverList.isEmpty()) {
							for (Department dept : approverList) {
								//dept = DepartmentDAO.getInstance().refresh(dept);
								log.debug("Dept = " + dept);
							}
						}
						log.debug("************");
					}
				}*/
				if (getMapOfAppGroupDepartment().containsKey(selectedApproverGroup)) {
					log.debug("");
					List<Department> deptList = new ArrayList<>();
					deptList = mapOfAppGroupDepartment.get(selectedApproverGroup);
					setSelectedDeptList(deptList);
					updateDepartments(deptList, true, false);
				}
				approverContactIds = new ArrayList<>();
				refreshMemberItems();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Listener used for row selection of Approver list tables,
	 *  sets the selectedGroupRowIndex with the currently selected row number for Group Approver table
	 *  and sets the selectedProdRowIndex with the currently selected row number for Production Approver table. */
	public void listenRowClicked (SelectEvent event) {
		try {
			SelectItem item = (SelectItem)event.getObject();
			if (item != null) {
				//int index = getGroupApproverList().indexOf(item);
				int index = getSelectedMemberItems().indexOf(item);
				if (index < 0) {
					index = getProductionApproverList().indexOf(item);
					setSelectedProdRowIndex(index);
				}
				else {
					setSelectedGroupRowIndex(index);
				}
				log.debug("Row Index = " + index);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for the "up arrow" control. This moves the currently
	 * selected Approver name up one position in the list. If the Approver
	 * is already first in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveApproverUp(ActionEvent evt) {
		try {
			if (evt != null) {
				String id = evt.getComponent().getId();
				log.debug("Id of button = " + id);
				if (id != null) {
					List<SelectItem> list = null;
					RowState state = new RowState();
					state.setSelected(true);
					if (id.contains("groupUp")) {
						//list = groupApproverList;
						list = getSelectedMemberItems();
						if(list != null && getSelectedGroupRowIndex() != null && getSelectedGroupRowIndex() > 0) {
							SelectItem item = list.get(getSelectedGroupRowIndex());
							list.remove(item);
							list.add(getSelectedGroupRowIndex() - 1, item);
							groupStateMap.put(item, state);
							setSelectedGroupRowIndex(getSelectedGroupRowIndex() - 1);
						}
					}
					else if (id.contains("prodUp")) {
						list = productionApproverList;
						if(list != null && getSelectedProdRowIndex() != null && getSelectedProdRowIndex() > 0) {
							SelectItem item = list.get(getSelectedProdRowIndex());
							list.remove(item);
							list.add(getSelectedProdRowIndex() - 1, item);
							prodStateMap.put(item, state);
							setSelectedProdRowIndex(getSelectedProdRowIndex() - 1);
						}
					}
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
	 * Action method for the "down arrow" control. This moves the currently
	 * selected Approver name down one position in the list. If the Approver
	 * is already last in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveApproverDown(ActionEvent evt) {
		try {
			if (evt != null) {
				String id = evt.getComponent().getId();
				log.debug("Id of button = " + id);
				if (id != null) {
					List<SelectItem> list = null;
					RowState state = new RowState();
					state.setSelected(true);
					if (id.contains("groupDown")) {
						//list = groupApproverList;
						list = getSelectedMemberItems();
						if (list != null && getSelectedGroupRowIndex() != null &&
								getSelectedGroupRowIndex() >= 0 && (getSelectedGroupRowIndex() < (list.size() - 1))) {
							SelectItem item = list.get(getSelectedGroupRowIndex());
							list.remove(item);
							list.add(getSelectedGroupRowIndex()+1, item);
							groupStateMap.put(item, state);
							setSelectedGroupRowIndex(getSelectedGroupRowIndex() + 1);
						}
					}
					else if (id.contains("prodDown")) {
						list = productionApproverList;
						if (list != null && getSelectedProdRowIndex() != null &&
								getSelectedProdRowIndex() >= 0 && (getSelectedProdRowIndex() < (list.size() - 1))) {
							SelectItem item = list.get(getSelectedProdRowIndex());
							list.remove(item);
							list.add(getSelectedProdRowIndex() + 1, item);
							prodStateMap.put(item, state);
							setSelectedProdRowIndex(getSelectedProdRowIndex() + 1);
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Value change listener for Department list's master-checkbox's checked / unchecked event
	 * @param evt
	 */
	/*public void listenCheckedForAllDepts(ValueChangeEvent evt) {
		if (selectedDeptList == null) {
			selectedDeptList = new ArrayList<>();
		}
		log.debug("Selected dept list size Before = " + selectedDeptList.size());
		if (getCheckedForAllDepts()) {
			for (Department dept : getDepartmentList()) {
				dept.setSelected(true);
				if (! selectedDeptList.contains(dept)) {
					selectedDeptList.add(dept);
				}
			}
		}
		else {
			selectedDeptList.clear();
			checkedForAllDepts = false;
			for (Department dept : getDepartmentList()) {
				dept.setSelected(false);
			}
		}
		log.debug("Selected dept list size After = " + selectedDeptList.size());
	}*/

	/** Value change listener for Department list's individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenSingleCheckDept(ValueChangeEvent evt) {
		try {
			Department dept = (Department) evt.getComponent().getAttributes().get("selectedRow");
			if (dept != null) {
				log.debug(" Selected dept  = " + dept.getName());
				if (selectedDeptList == null) {
					selectedDeptList = new ArrayList<>();
				}
				//dept = DepartmentDAO.getInstance().refresh(dept);
				log.debug(" Selected dept list size Before = " + selectedDeptList.size());
				if (dept.getSelected() && (! selectedDeptList.contains(dept))) {
					selectedDeptList.add(dept);
				}
				else if (selectedDeptList.contains(dept)) {
					selectedDeptList.remove(dept);
				}
				log.debug(" Selected dept list size After = " + selectedDeptList.size());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value change listener for show step icon's click.
	 * @param evt Action event provided by framework.
	 */
	public void listenShowStep(ActionEvent evt) {
		try {
			if (evt != null) {
				String id = evt.getComponent().getId();
				if (id != null) {
					log.debug("");
					if (id.equals("step1")) {
						if (showStep1) {
							showStep1 = false;
						}
						else {
							showStep1 = true;
						}
					}
					if (id.equals("step2")) {
						if (showStep2) {
							showStep2 = false;
						}
						else {
							showStep2 = true;
						}

					}
					if (id.equals("step3")) {
						if (showStep3) {
							showStep3 = false;
						}
						else {
							showStep3 = true;
						}

					}
					if (id.equals("step4")) {
						if (showStep4) {
							showStep4 = false;
						}
						else {
							showStep4 = true;
						}

					}
					if (id.equals("step5")) {
						if (showStep5) {
							showStep5 = false;
						}
						else {
							showStep5 = true;
						}

					}
					if (id.equals("step6")) {
						if (showStep6) {
							showStep6 = false;
						}
						else {
							showStep6 = true;
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

	/**See {@link #approvalPathNameList}. */
	public void setApprovalPathNameList(List<SelectItem> approvalPathNameList) {
		this.approvalPathNameList = approvalPathNameList;
	}

	/**See {@link #documentActionList}. */
	public List<SelectItem> getDocumentActionList() {
		return documentActionList;
	}
	/**See {@link #documentActionList}. */
	public void setDocumentActionList(List<SelectItem> documentActionList) {
		this.documentActionList = documentActionList;
	}

	/** See {@link #memberId}. */
	public Integer getMemberId() {
		return memberId;
	}
	/** See {@link #memberId}. */
	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	/** See {@link #memberItems}. */
	public List<SelectItem> getMemberItems() {
		if (memberItems == null) {
			return new ArrayList<>();
		}
		return memberItems;
	}
	/** See {@link #memberItems}. */
	public void setMemberItems(List<SelectItem> memberItems) {
		this.memberItems = memberItems;
	}

	/** See {@link #contactDAO}. */
	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	/** See {@link #approvalPathDAO}. */
	private ApprovalPathDAO getApprovalPathDAO() {
		if (approvalPathDAO == null) {
			approvalPathDAO = ApprovalPathDAO.getInstance();
		}
		return approvalPathDAO;
	}

	/** See {@link #approverDAO}. */
	private ApproverDAO getApproverDAO() {
		if (approverDAO == null) {
			approverDAO = ApproverDAO.getInstance();
		}
		return approverDAO;
	}

	/** See {@link #contactDocumentDAO}. */
	private ContactDocumentDAO getContactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}

	/** See {@link #approverGroupDAO}. */
	private ApproverGroupDAO getApproverGroupDAO() {
		if (approverGroupDAO == null) {
			approverGroupDAO = ApproverGroupDAO.getInstance();
		}
		return approverGroupDAO;
	}

	/** See {@link #approvalPathAnchorDAO}. */
	public ApprovalPathAnchorDAO getApprovalPathAnchorDAO() {
		if (approvalPathAnchorDAO == null) {
			approvalPathAnchorDAO = ApprovalPathAnchorDAO.getInstance();
		}
		return approvalPathAnchorDAO;
	}

	/** See {@link #approverAuditDAO}. */
	public ApproverAuditDAO getApproverAuditDAO() {
		if (approverAuditDAO == null) {
			approverAuditDAO = ApproverAuditDAO.getInstance();
		}
		return approverAuditDAO;
	}

	/** See {@link #approverAuditDAO}. */
	public PayrollPreferenceDAO getPayrollPreferenceDAO() {
		if (payrollPreferenceDAO == null) {
			payrollPreferenceDAO = PayrollPreferenceDAO.getInstance();
		}
		return payrollPreferenceDAO;
	}
	
	/** See {@link #approvalMethod}. */
	public String getApprovalMethod() {
		return approvalMethod;
	}
	/** See {@link #approvalMethod}. */
	public void setApprovalMethod(String approvalMethod) {
		this.approvalMethod = approvalMethod;
	}

	/** See {@link #productionMembers}. */
	public List<SelectItem> getProductionMembers() {
		if (productionMembers == null) {
			productionMembers = createProductionMembersList();
		}
		return productionMembers;
	}
	/** See {@link #productionMembers}. */
	public void setProductionMembers(List<SelectItem> productionMembers) {
		this.productionMembers = productionMembers;
	}

	/** See {@link #departmentList}. */
	public List<Department> getDepartmentList() {
		if (departmentList == null) {
			createDepartmentList();
		}
		return departmentList;
	}
	/** See {@link #departmentList}. */
	public void setDepartmentList(List<Department> departmentList) {
		this.departmentList = departmentList;
	}

	/**
	 * @return name of currently selected department, or null if no department
	 *         is currently selected.
	 */
	public String getDepartNameSelected() {
		if (selectedDepartment == null) {
			return null;
		}
		return selectedDepartment.getName();
	}

	/**See {@link #checkedForAll}. */
	public Boolean getCheckedForAll() {
		return checkedForAll;
	}
	/**See {@link #checkedForAll}. */
	public void setCheckedForAll(Boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/** See {@link #checkBoxSelectedItems}. */
	public List<DocumentChain> getCheckBoxSelectedItems() {
		return checkBoxSelectedItems;
	}
	/** See {@link #checkBoxSelectedItems}. */
	public void setCheckBoxSelectedItems(List<DocumentChain> checkBoxSelectedItems) {
		this.checkBoxSelectedItems = checkBoxSelectedItems;
	}

	/** See {@link #approverType}. */
	/*public String getApproverType() {
		return approverType;
	}
	*//** See {@link #approverType}. *//*
	public void setApproverType(String approverType) {
		this.approverType = approverType;
	}*/

//	/** See {@link #isDefaultView}. */
//	public boolean getIsDefaultView() {
//		return isDefaultView;
//	}
//	/** See {@link #isDefaultView}. */
//	public void setDefaultView(boolean isDefaultView) {
//		this.isDefaultView = isDefaultView;
//	}

	/** See {@link #selectedApprovalPathId}. */
	public Integer getSelectedApprovalPathId() {
		return selectedApprovalPathId;
	}
	/** See {@link #selectedApprovalPathId}. */
	public void setSelectedApprovalPathId(Integer selectedApprovalPath) {
		selectedApprovalPathId = selectedApprovalPath;
	}

	/** See {@link #currentApprovalPath}. */
	public ApprovalPath getCurrentApprovalPath() {
		return currentApprovalPath;
	}
	/** See {@link #currentApprovalPath}. */
	public void setCurrentApprovalPath(ApprovalPath approvalPath) {
		currentApprovalPath = approvalPath;
	}

	/** See {@link #finalApproverId}. */
	public Integer getFinalApproverId() {
		return finalApproverId;
	}
	/** See {@link #finalApproverId}. */
	public void setFinalApproverId(Integer finalApproverId) {
		this.finalApproverId = finalApproverId;
	}

	/** See {@link #productionApproverList}. */
	public List<SelectItem> getProductionApproverList() {
		if (productionApproverList == null) {
			return new ArrayList<>();
		}
		return productionApproverList;
	}
	/** See {@link #productionApproverList}. */
	public void setProductionApproverList(List<SelectItem> productionApproverList) {
		this.productionApproverList = productionApproverList;
	}

	/** See {@link #selectedDepartmentId}. */
	public Integer getSelectedDepartmentId() {
		return selectedDepartmentId;
	}
	/** See {@link #selectedDepartmentId}. */
	public void setSelectedDepartmentId(Integer selectedDepartmentId) {
		this.selectedDepartmentId = selectedDepartmentId;
	}

	/** See {@link #finalPoolApprover}. */
	public String getFinalPoolApprover() {
		return finalPoolApprover;
	}
	/** See {@link #finalPoolApprover}. */
	public void setFinalPoolApprover(String finalPoolApprover) {
		this.finalPoolApprover = finalPoolApprover;
	}

	/**See {@link #appGroupNameList}. */
	public List<SelectItem> getAppGroupNameList() {
		if (appGroupNameList == null) {
			appGroupNameList = createGroupNameList();
		}
		return appGroupNameList;
	}

	/** See {@link #groupApproverList}. */
	public List<SelectItem> getGroupApproverList() {
		if (groupApproverList == null) {
			groupApproverList = new ArrayList<>();
			if (selectedAppGroupId == null) {
				selectedAppGroupId = (Integer) getAppGroupNameList().get(0).getValue();
			}
			if (selectedAppGroupId != null) {
				groupApproverList = createGroupApproverList(selectedAppGroupId);
			}
		}
		return groupApproverList;
	}
	/** See {@link #groupApproverList}. */
	public void setGroupApproverList(List<SelectItem> groupApproverList) {
		this.groupApproverList = groupApproverList;
	}

	/** See {@link #approverGroupName}. */
	public String getApproverGroupName() {
		return approverGroupName;
	}
	/** See {@link #approverGroupName}. */
	public void setApproverGroupName(String approverGroupName) {
		this.approverGroupName = approverGroupName;
	}

	/** See {@link #selectedDeptList}. */
	public List<Department> getSelectedDeptList() {
		if (selectedDeptList == null) {
			selectedDeptList = new ArrayList<>();
		}
		return selectedDeptList;
	}
	/** See {@link #selectedDeptList}. */
	public void setSelectedDeptList(List<Department> selectedDeptList) {
		this.selectedDeptList = selectedDeptList;
	}

	/** See {@link #documentEditorList}. */
	public List<SelectItem> getDocumentEditorList() {
		if (documentEditorList == null) {
			documentEditorList = new ArrayList<>();
		}
		return documentEditorList;
	}
	/** See {@link #documentEditorList}. */
	public void setDocumentEditorList(List<SelectItem> documentEditorList) {
		this.documentEditorList = documentEditorList;
	}

	/** See {@link #selectedAppGroupId}. */
	public Integer getSelectedAppGroupId() {
		return selectedAppGroupId;
	}
	/** See {@link #selectedAppGroupId}. */
	public void setSelectedAppGroupId(Integer selectedAppGroupId) {
		this.selectedAppGroupId = selectedAppGroupId;
	}

	/** See {@link #selectedApproverGroup}. */
	public ApproverGroup getSelectedApproverGroup() {
		if (selectedApproverGroup == null && selectedAppGroupId != null) {
			selectedApproverGroup = getApproverGroupDAO().findById(selectedAppGroupId);
		}
		return selectedApproverGroup;
	}
	/** See {@link #selectedApproverGroup}. */
	public void setSelectedApproverGroup(ApproverGroup selectedApproverGroup) {
		this.selectedApproverGroup = selectedApproverGroup;
	}

	/** See {@link #selectedMemberItems}. */
	public List<SelectItem> getSelectedMemberItems() {
		if (selectedMemberItems == null) {
			selectedMemberItems = new ArrayList<>();
		}
		return selectedMemberItems;
	}
	/** See {@link #selectedMemberItems}. */
	public void setSelectedMemberItems(List<SelectItem> selectedMemberItems) {
		this.selectedMemberItems = selectedMemberItems;
	}

	/** See {@link #unselectedMemberItems}. */
	public List<SelectItem> getUnselectedMemberItems() {
		if (unselectedMemberItems == null && memberItems != null) {
			unselectedMemberItems = new ArrayList<>();
			for (SelectItem item : getMemberItems()) {
				unselectedMemberItems.add(item);
			}
		}
		return unselectedMemberItems;
	}
	/** See {@link #unselectedMemberItems}. */
	public void setUnselectedMemberItems(List<SelectItem> unselectedMemberItems) {
		this.unselectedMemberItems = unselectedMemberItems;
	}

	/** See {@link #viewType}. */
	public String getViewType() {
		return viewType;
	}
	/** See {@link #viewType}. */
	public void setViewType(String viewType) {
		this.viewType = viewType;
	}

	/** See {@link #showStep1}. */
	public boolean getShowStep1() {
		return showStep1;
	}
	/** See {@link #showStep1}. */
	public void setShowStep1(boolean showStep1) {
		this.showStep1 = showStep1;
	}

	/** See {@link #showStep2}. */
	public boolean getShowStep2() {
		return showStep2;
	}
	/** See {@link #showStep2}. */
	public void setShowStep2(boolean showStep2) {
		this.showStep2 = showStep2;
	}

	/** See {@link #showStep3}. */
	public boolean getShowStep3() {
		return showStep3;
	}
	/** See {@link #showStep3}. */
	public void setShowStep3(boolean showStep3) {
		this.showStep3 = showStep3;
	}

	/** See {@link #showStep4}. */
	public boolean getShowStep4() {
		return showStep4;
	}
	/** See {@link #showStep4}. */
	public void setShowStep4(boolean showStep4) {
		this.showStep4 = showStep4;
	}

	/** See {@link #showStep5}. */
	public boolean getShowStep5() {
		return showStep5;
	}
	/** See {@link #showStep5}. */
	public void setShowStep5(boolean showStep5) {
		this.showStep5 = showStep5;
	}

	/** See {@link #showStep6}. */
	public boolean getShowStep6() {
		return showStep6;
	}
	/** See {@link #showStep6}. */
	public void setShowStep6(boolean showStep6) {
		this.showStep6 = showStep6;
	}

	/**See {@link #groupStateMap}. */
	public RowStateMap getGroupStateMap() {
		return groupStateMap;
	}
	/**See {@link #groupStateMap}. */
	public void setGroupStateMap(RowStateMap groupStateMap) {
		this.groupStateMap = groupStateMap;
	}

	/**See {@link #selectedGroupRowIndex}. */
	public Integer getSelectedGroupRowIndex() {
		return selectedGroupRowIndex;
	}
	/**See {@link #selectedGroupRowIndex}. */
	public void setSelectedGroupRowIndex(Integer selectedGroupRowIndex) {
		this.selectedGroupRowIndex = selectedGroupRowIndex;
	}

	/**See {@link #prodStateMap}. */
	public RowStateMap getProdStateMap() {
		return prodStateMap;
	}
	/**See {@link #prodStateMap}. */
	public void setProdStateMap(RowStateMap prodStateMap) {
		this.prodStateMap = prodStateMap;
	}

	/**See {@link #selectedProdRowIndex}. */
	public Integer getSelectedProdRowIndex() {
		return selectedProdRowIndex;
	}
	/**See {@link #selectedProdRowIndex}. */
	public void setSelectedProdRowIndex(Integer selectedProdRowIndex) {
		this.selectedProdRowIndex = selectedProdRowIndex;
	}

	/**See {@link #groupApprovalMethod}. */
	public String getGroupApprovalMethod() {
		return groupApprovalMethod;
	}
	/**See {@link #groupApprovalMethod}. */
	public void setGroupApprovalMethod(String groupApprovalMethod) {
		this.groupApprovalMethod = groupApprovalMethod;
	}

	/**See {@link #checkedForAllDepts}. */
	/*public Boolean getCheckedForAllDepts() {
		return checkedForAllDepts;
	}
	*//**See {@link #checkedForAllDepts}. *//*
	public void setCheckedForAllDepts(Boolean checkedForAllDepts) {
		this.checkedForAllDepts = checkedForAllDepts;
	}*/

	/**See {@link #mapOfAppGroupDepartment}. */
	public Map<ApproverGroup, List<Department>> getMapOfAppGroupDepartment() {
		if (mapOfAppGroupDepartment == null) {
			mapOfAppGroupDepartment = new HashMap<>();
		}
		return mapOfAppGroupDepartment;
	}
	/**See {@link #mapOfAppGroupDepartment}. */
	public void setMapOfAppGroupDepartment(Map<ApproverGroup, List<Department>> mapOfAppGroupDepartment) {
		this.mapOfAppGroupDepartment = mapOfAppGroupDepartment;
	}

	public boolean getTalentProd() {
		if (currentProduction == null) {
			currentProduction = SessionUtils.getNonSystemProduction();
		}
		if (currentProduction.getType().isCanadaTalent()) {
			return true;
		}
		else {
			return false;
		}
	}

	/**See {@link #mapOfAppGroupApprovers}. */
	/*public Map<Integer, List<SelectItem>> getMapOfAppGroupApprovers() {
		if (mapOfAppGroupApprovers == null) {
			mapOfAppGroupApprovers = new HashMap<>();
			mapOfAppGroupApprovers = createAppGroupApproversMap(currentApprovalPath);
		}
		return mapOfAppGroupApprovers;
	}
	*//**See {@link #mapOfAppGroupApprovers}. *//*
	public void setMapOfAppGroupApprovers(Map<Integer, List<SelectItem>> mapOfAppGroupApprovers) {
		this.mapOfAppGroupApprovers = mapOfAppGroupApprovers;
	}*/

}
