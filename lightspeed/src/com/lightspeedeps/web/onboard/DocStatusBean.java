package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.ApprovableStatusFilter;
import com.lightspeedeps.type.FilterType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.approver.FilterBean;
import com.lightspeedeps.web.approver.FilterHolder;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListView;

/**
 * This class is the backing bean of the Start Status mini-tab [Document status view],
 * under the Onboarding page.
 * It is useful in creating the list of contacts that are distributed to the contacts,
 * it also holds the methods to filter the list.
 */
@ManagedBean
@ViewScoped
public class DocStatusBean extends ListView implements Serializable,FilterHolder {

	private static final long serialVersionUID = -1590430998023323211L;

	private static final Log log = LogFactory.getLog(DocStatusBean.class);

	/** The list of distinct document chains that are distributed to contacts */
	private List<DocumentChain> distinctDocumentChainList;

	/** The list of contacts that were distributed with the currently selected document from the document list */
	private List<ContactDocument> distinctContactList;

	/** The instance of Document chain */
	private DocumentChain documentChain;

	/** The ContactDocumentDAO instance . */
	private transient ContactDocumentDAO contactDocumentDAO;

	/** The packet dao instance used to use the dao layer methods. */
	private transient ContactDAO contactDAO;

	/**
	 * The FilterBean instance that manages filter settings and changes.
	 */
	private final FilterBean filterBean;

	/**
	 * The current Department filter setting -- the database id of the
	 * Department that the displayed contacts should belong to. If 0, then the
	 * Department filter is set to "All" (no filtering).
	 */
	private Integer departmentId = 0;

	/**
	 * The current WeeklyStatusFilter filter setting, which (indirectly) defines
	 * the possible WeeklyStatus values that the displayed timecards should
	 * have. If set to WeeklyStatusFilter.ALL, then the WeeklyStatusFilter
	 * selection is set to "All" -- no filtering.
	 */
	private ApprovableStatusFilter statusFilter;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private final RowStateMap stateMap = new RowStateMap();

	private ContactDocument contactDocToDelete;

	/** True iff the user has elected to show timecards or StartForm`s for all
	 * projects in the case of a Commercial (AICP) production.  This option is
	 * only available to users with Financial Data Admin permission.*/
	private boolean showAllProjects = false;

	/**
	 * The default constructor
	 */
	public DocStatusBean() {
		super(Contact.SORTKEY_NAME, "Contact.");
		filterBean = FilterBean.getInstance();
		getDistinctDocumentChainList();
		setEmployeeAccount(SessionUtils.getString(Constants.ATTR_ONBOARDING_EMPLOYEE));
		if (getEmployeeAccount() == null) {
			setEmployeeAccount(Constants.CATEGORY_ALL);
		}
		setDepartmentId(SessionUtils.getInteger(Constants.ATTR_ONBOARDING_DEPT , 0));
		String status = SessionUtils.getString(Constants.ATTR_ONBOARDING_STATUS);
		if (status != null) {
			setStatusFilter(ApprovableStatusFilter.valueOf(status));
		}
		else {
			setStatusFilter(ApprovableStatusFilter.ALL);
		}
		if (distinctDocumentChainList != null) {
			RowState state = new RowState();
			state.setSelected(true);
			documentChain =  (DocumentChain) SessionUtils.get(Constants.ATTR_ONBOARDING_CONTACT_DOCUMENT_ID);
			if (documentChain != null) {
				stateMap.put(documentChain, state);
			}
			else {
				if (distinctDocumentChainList != null) {
					if (distinctDocumentChainList.size() > 0) {
						documentChain = distinctDocumentChainList.get(0);
						stateMap.put(documentChain, state);
					}
				}
			}
			createDocumentContactList();
		}
		if (getEmployeeAccount() != null) {
			createContactListByEmployeeFilter();
		}
		else if (getDepartmentId() != 0) {
			getFilteredContactListByDepartment();
		}
		else if(!getStatusFilter().name().equalsIgnoreCase("ALL")) {
			getContactListByStatusFilter();
		}
		forceLazyInit();
	}

	/** Used to return the instance of the DocStatusBean . */
	public static DocStatusBean getInstance() {
		return (DocStatusBean)ServiceFinder.findBean("docStatusBean");
	}

	/**
	 * Utility method to initialize the list on the selected view.
	 */
	public void inIt() {
		distinctDocumentChainList = null;
	}

	/**
	 * Utility Method:
	 * used to put the specified items in the current hibernate session,
	 * to avoid lazy initialization
	 */
	private void forceLazyInit() {
		if (distinctDocumentChainList != null) {
			for (DocumentChain document : distinctDocumentChainList) {
				document.getName();
			}
		}
		if (distinctContactList != null) {
			for (ContactDocument contactDoc : distinctContactList) {
				contactDoc.getContact().getRole().getName();
				contactDoc.getContact().getUser().getLastNameFirstName();
				if (contactDoc.getEmployment() != null) {
					contactDoc.getEmployment().getDepartment().getName();
					contactDoc.getEmployment().getOccupation();
				}
			}
		}
	}

	/** Listener used for row selection, sets the Document with the currently selected row object . */
	@Override
	public void listenRowClicked (SelectEvent event) {
		documentChain = (DocumentChain)event.getObject();
		SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACT_DOCUMENT_ID, documentChain);
		if (getDepartmentId() != 0) {
			log.debug("<>");
			createFilteredContactList();
		}
		else if (getEmployeeAccount() != null) {
			log.debug("<>");
			createContactListByEmployeeFilter();
		}
		else {
			createDocumentContactList();
		}
		if (!filterBean.getFilterType().equals(FilterType.DEPT)) {
			if (statusFilter != null) {
				if (statusFilter.equals("ALL")) {
					createDocumentContactList();
				}
				else {
					refreshContactByStatusList();
				}
			}
		}
	}

	/**
	 * Method creates the list of contacts that are distributed with the currently selected document chain
	 */
	private void createDocumentContactList() {
		log.debug("<>");
	    distinctContactList = new ArrayList<>();
		if (documentChain != null) {
			Production production = SessionUtils.getProduction();
		    Map<String, Object> values = new HashMap<>();
		    values.put("documentChainId", documentChain.getId());
		    if (production.getType().isAicp() && ! getShowAllProjects()) {
		    	log.debug(">>>>>>>>>>>>>>>>>>>>> " + showAllProjects);
		    	values.put("project", SessionUtils.getCurrentProject());
		    	setDistinctContactList(getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOCUMENT_LIST_BY_DOCUMENT_CHAIN_PROJECT, values));
		    }
		    else {
		    	log.debug(">>>>>>>>>>>>>>>>>>>>> " + showAllProjects);
			    values.put("production", production);
				setDistinctContactList(getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACT_DOCUMENT_BY_DOCUMENT_CHAIN_ID, values));
		    }
			for (ContactDocument cd : getDistinctContactList()) {
				cd.setWaitingFor(ApproverUtils.calculateWaitingFor(cd));
			}
			removeAdminRole();
		}
	}

	/** Utility method
	 * @return ContactDocumentDAO instance
	 */
	public ContactDocumentDAO getcontactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
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

	@Override
	protected void setSelected(Object item, boolean b) {

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return getDistinctDocumentChainList();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getComparator() {
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
			case STATUS:
				list = createStatusList();
			default:
				break;
		}
		return list;
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
		ArrayList<SelectItem> deptList = (ArrayList<SelectItem>) DepartmentUtils.getDepartmentDataAdminDL();
			deptList.add(0, new SelectItem(0, "All"));
		return deptList;
	}

	/**
	 * Create a list of WeeklyStatusFilter items for a drop-down.
	 *
	 * @return default list generated from all WeeklyStatusFilter values;
	 *         subclasses may override this method to provide a different list,
	 *         e.g., for the status of Start Forms instead of timecards.
	 */
	protected List<SelectItem> createStatusList() {
		return EnumList.getWeeklyStatusFilterList();
	}

	@Override
	public void dropFilter(FilterType type) {
		switch(type) {
		case DEPT:
			departmentId = 0;
			listenDeptChange();
			break;
		case NAME:
			setEmployeeAccount(Constants.CATEGORY_ALL);
			listenEmployeeChange();
			break;
		case STATUS:
			statusFilter = ApprovableStatusFilter.ALL;
			listenStatusChange();
			break;
		default:
			break;
		}
	}

	@Override
	public void listenChange(FilterType type, Object value) {
		switch(type) {
		case DEPT:
			departmentId = Integer.valueOf((String)value);
			listenDeptChange();
			break;
		case NAME:
			setEmployeeAccount((String)value);
			listenEmployeeChange();
			break;
		case STATUS:
			try {
				statusFilter = ApprovableStatusFilter.valueOf((String)value);
			}
			catch (Exception e) { // can happen when switching "filter by"
				statusFilter = ApprovableStatusFilter.ALL;
			}
			listenStatusChange();
			break;
		default:
			break;
		}
	}

	/**
	 * ValueChangeListener for Department drop-down list
	 * Upon entry, {@link #getDepartmentId()} has been set to the new value.
	 */
	protected void listenDeptChange() {
		try {
			//setEmployeeAccount(null);
			setStatusFilter(null);
			log.debug("<>");
			SessionUtils.put(Constants.ATTR_ONBOARDING_DEPT, getDepartmentId());
			getFilteredContactListByDepartment();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Creates the list of contacts with the selected department value from the department filter
	 */
	private void getFilteredContactListByDepartment() {
		if (documentChain != null) {
			if (getDepartmentId() != 0) {
				createFilteredContactList();
			}
			else {
				createDocumentContactList();
			}
		}
	}

	/**
	 * ValueChangeListener for Status drop-down filter list
	 */
	protected void listenStatusChange() {
		try {
			log.debug("<>");
			setDepartmentId(0);
			//setEmployeeAccount(null);
			SessionUtils.put(Constants.ATTR_ONBOARDING_STATUS, getStatusFilter().name());
			getContactListByStatusFilter();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Creates the list of contacts for corresponding status value selected from the
	 * status filer
	 */
	private void getContactListByStatusFilter() {
		if (documentChain != null) {
			refreshContactByStatusList();
		}
		else {
			createDocumentContactList();
		}
	}

	/**
	 * ValueChangeListener for Employee drop-down list
	 * Upon entry, {@link #getEmployeeAccount()} has been set to the new value.
	 */
	protected void listenEmployeeChange() {
		try {
			setDepartmentId(0);
			setStatusFilter(null);
			log.debug("<>");
			SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYEE, getEmployeeAccount());
			if (getEmployeeAccount() != null) {
				createContactListByEmployeeFilter();
			}
			else {
				createDocumentContactList();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Creates a single contact with the selected employee from the name filter
	 */
	private void createContactListByEmployeeFilter() {
		Contact contact = getContactDAO().findByAccountNumAndProduction(getEmployeeAccount(), SessionUtils.getProduction());
		if (contact != null) {
			Map<String, Object> values = new HashMap<>();
			if (documentChain != null) {
				values.put("documentChainId", documentChain.getId());
			}
			values.put("contactId", contact.getId());
			log.debug("contact id >>>>> " + contact.getId() + "----chain id  "
					+ documentChain.getId());
			distinctContactList = new ArrayList<>();
			distinctContactList = getcontactDocumentDAO()
					.findByNamedQuery(
							ContactDocument.GET_CONTACT_DOCUMENT_BY_CONTACT_ID_AND_DOCUMENT_CHAIN_ID,
							values);
			for (ContactDocument doc : distinctContactList) {
				log.debug("contact document generated >>>>>>>> "
						+ doc.getContact().getUser().getFirstNameLastName());
			}
		}
	}

	/**
	 * Method creates the contact list by selected status filter
	 */
	private void refreshContactByStatusList() {
		if (getStatusFilter().name().equalsIgnoreCase("ALL")) {
			createDocumentContactList();
		}
		else {
			String sqlStatus = statusFilter.sqlFilter();
			distinctContactList = new ArrayList<>();
			setDistinctContactList(getcontactDocumentDAO().findFilteredContactListByStatus(documentChain.getId(), sqlStatus));
			removeAdminRole();
		}
	}

	/**
	 * Method used to create the filtered list of contacts according to the department selected from the drop down
	 */
	private void createFilteredContactList() {
		Map<String, Object> values = new HashMap<>();
		values.put("documentChainId", documentChain.getId());
		values.put("departmentId", departmentId);
		distinctContactList = new ArrayList<>();
		if (SessionUtils.getProduction().getType().isAicp() && ! getShowAllProjects()) {
			values.put("project", SessionUtils.getCurrentProject());
			distinctContactList = getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_DEPARTMENT_DOCUMENT_CHAIN_PROJECT, values);
		}
		else {
			values.put("production", SessionUtils.getProduction());
			distinctContactList = getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_CONTACTDOCUMENT_BY_DEPARTMENT_ID_AND_DOCUMENT_CHAIN_ID, values);
		}
		removeAdminRole();
	}

	public String actionDeleteContactDoc() {
		if (contactDocToDelete != null) {
			log.debug("<><><><><><> "+contactDocToDelete.getId());
			distinctContactList.remove(contactDocToDelete);
			getcontactDocumentDAO().delete(contactDocToDelete);
			createDocumentContactList();
		}
		return null;
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
				StatusListBean.getInstance().refreshView();
//				EmpDetailBean.getInstance().refreshView();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method used to refresh the document chain list according to the show all projects check box value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void refreshView(){
		if (showAllProjects) {
			distinctDocumentChainList = (List) getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN, map("production", SessionUtils.getProduction()));
		}
		else {
			distinctDocumentChainList = (List) getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN_LIST_BY_PROJECT, map("project", SessionUtils.getCurrentProject()));
		}
		clearSelectedRow();
		if (distinctDocumentChainList != null && ! distinctDocumentChainList.isEmpty()) {
			documentChain = distinctDocumentChainList.get(0);
			selectRow(documentChain);
		}
	}

	/**
	 * Method used to clear the row state for the previous document chain if their was any.
	 */
	private void clearSelectedRow() {
		RowState state = new RowState();
		state.setSelected(false);
		if (documentChain != null) {
			stateMap.put(documentChain, state);
		}
	}

	/** Method used to set the row state for the passed document chain and forces the contact document list to refresh for the corresponding chain
	 * @param docChain first chain entry from the data table list
	 */
	private void selectRow(DocumentChain docChain) {
		RowState state = new RowState();
		state.setSelected(true);
		stateMap.put(docChain, state);
		createDocumentContactList();
	}

	/**
	 * Method used to remove the admin roles from the data table lists
	 */
	private void removeAdminRole() {
		Iterator<ContactDocument> itr = getDistinctContactList().iterator();
		while (itr.hasNext()) {
			ContactDocument cd = itr.next();
			if (cd.getEmployment() != null) {
				Integer id = cd.getEmployment().getRole().getId();
				if ((id.equals(Constants.ROLE_ID_DATA_ADMIN))
						|| (id.equals(Constants.ROLE_ID_FINANCIAL_ADMIN))
						|| (id.equals(Constants.ROLE_ID_LS_ADMIN))
						|| (id.equals(Constants.ROLE_ID_LS_ADMIN_VIEW))) {
					itr.remove();
				}
			}
		}
	}

	/**See {@link #distinctDocumentList}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<DocumentChain> getDistinctDocumentChainList() {
		if (distinctDocumentChainList == null) {
			if (SessionUtils.getProduction().getType().isAicp() && ! getShowAllProjects()) {
				distinctDocumentChainList = (List) getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN_LIST_BY_PROJECT, map("project", SessionUtils.getCurrentProject()));
			}
			else {
				distinctDocumentChainList = (List) getcontactDocumentDAO().findByNamedQuery(ContactDocument.GET_DISTINCT_DOCUMENT_CHAIN, map("production", SessionUtils.getProduction()));
			}
		}
		return distinctDocumentChainList;
	}
	/**See {@link #distinctDocumentList}. */
	public void setDistinctDocumentChainList(List<DocumentChain> distinctDocumentChainList) {
		this.distinctDocumentChainList = distinctDocumentChainList;
	}

	/**See {@link #distinctContactList}. */
	public List<ContactDocument> getDistinctContactList() {
		for (ContactDocument cd : distinctContactList) { // lazy initialization
			cd.getContact().getUser().getLastNameFirstName();
			if (cd.getEmployment() != null) {
				cd.getEmployment().getDepartment().getName();
				cd.getEmployment().getOccupation();
			}
		}
		return distinctContactList;
	}
	/**See {@link #distinctContactList}. */
	public void setDistinctContactList(List<ContactDocument> distinctContactList) {
		this.distinctContactList = distinctContactList;
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		return departmentId;
	}
	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer deptId) {
		departmentId = deptId;
		if (deptId != null && filterBean.getFilterType() == FilterType.DEPT) {
			filterBean.setSelectFilterValue(deptId.toString());
		}
	}

	/** See {@link #statusFilter}. */
	public ApprovableStatusFilter getStatusFilter() {
		return statusFilter;
	}
	/** See {@link #statusFilter}. */
	public void setStatusFilter(ApprovableStatusFilter status) {
		statusFilter = status;
		if (statusFilter != null && filterBean.getFilterType() == FilterType.STATUS) {
			filterBean.setSelectFilterValue(statusFilter.name());
		}
	}

	/** See {@link FilterBean#getEmployeeAccount()}. */
	public String getEmployeeAccount() {
		String acct = filterBean.getEmployeeAccount();
		if (acct != null && acct.equals(Constants.CATEGORY_ALL)) {
			acct = null;
		}
		return acct;
	}
	/** See {@link FilterBean#setEmployeeAccount(String)}. */
	public void setEmployeeAccount(String employeeAccount) {
		filterBean.setEmployeeAccount(employeeAccount);
		if (filterBean.getFilterType() == FilterType.NAME) {
			filterBean.setSelectFilterValue(employeeAccount);
		}
	}

	/** See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}

	/**See {@link #contactDocToDelete}. */
	public ContactDocument getContactDocToDelete() {
		return contactDocToDelete;
	}
	/**See {@link #contactDocToDelete}. */
	public void setContactDocToDelete(ContactDocument contactDocToDelete) {
		this.contactDocToDelete = contactDocToDelete;
	}
	/** See {@link #showAllProjects}. */
	public boolean getShowAllProjects() {
		return showAllProjects;
	}
	/** See {@link #showAllProjects}. */
	public void setShowAllProjects(boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}

}
