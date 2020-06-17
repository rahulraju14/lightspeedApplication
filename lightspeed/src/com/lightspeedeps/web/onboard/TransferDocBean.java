package com.lightspeedeps.web.onboard;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIInput;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.datatable.DataTable;
import org.icefaces.ace.event.TableFilterEvent;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.service.DocumentTransferService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.PdfUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.view.View;

/**
 * This class is the backing bean for the "new" Document Transfer page, which is
 * used to select documents and transmit them to the client's payroll service.
 * This page presents one row per employee/occupation, and one column per document
 * type (e.g., I-9, W-4, etc.).  This page/bean does not currently support
 * transferring custom documents. (See transferDocument.xhtml.)
 * <p>
 * Note that this bean is instantiated due to an ace:dataTable reference, which
 * is evaluated by the ICEfaces framework code even when the table is NOT being
 * rendered! In fact, the entire mini-tab is not being rendered, but ICEfaces
 * still evaluates the var= parameter of ace:dataTables. Therefore, we have
 * extra measures to avoid some expensive initialization step(s) until the minitab
 * is actually rendered. The {@link #rendered} boolean is set true when the
 * mini-tab is actually being rendered; this is done by a reference on the page
 * to bean.setup, forcing a call to {@link #getSetUp()}.
 */
@ManagedBean
@ViewScoped
public class TransferDocBean extends View implements Serializable, ControlHolder {

	private static final long serialVersionUID = 8628328175892424408L;

	private static final Log log = LogFactory.getLog(TransferDocBean.class);

	/** Regular expression that matches the request parameter for the "source" component ID
	 *  of a clicked checkbox; the embedded numeric field is the row number of the checkbox and
	 *  the another one is the id of a cell in the table. */
	private static final String DATATABLE_ITEM_SOURCE_ID_REGEX = ".*:(\\d+):([A-Za-z0-9_]+)";

	/** Pattern for the {@link #DATATABLE_ITEM_SOURCE_ID_REGEX} regular expression. */
	private static final Pattern DATATABLE_ITEM_SOURCE_ID_PATTERN = Pattern.compile(DATATABLE_ITEM_SOURCE_ID_REGEX);

	/** Transfer documents to performer */
	private static final String TRANSFER_TO_PERFORMER = "toPerformer";

	/** Transfer documents to local union office */
	private static final String TRANSFER_TO_OFFICE = "toOffice";

	/** Transfer documents to Biller */
	private static final String TRANSFER_TO_BILLER = "toBiller";

	/** True iff the tab that uses this bean is really being rendered. */
	private boolean rendered = false;

	/** List of Employment rows. */
	private List<EmploymentDocuments> employmentList;

	/** Select item list for the approval status filter. */
	private static final List<SelectItem> statusList;

	/** Select item list for the transfer status filter. */
	private static final List<SelectItem> transferStatusList;

	/**
	 * Select item list of Biller to send documents to. Currenty just used for
	 * Canada
	 */
	private List<SelectItem> billersListDL;

	/** Map of possible Billers */
	private Map<Integer, SelectionItem> billerMap;

	/** List of form types to be shown on the transfer table. */
	private List<String> formTypeList;

	/** Selected approval status from the approval status filter. */
	private ApprovalStatus selectedStatus;

	/** Selected Biller */
	private SelectionItem selectedBiller;

	/** Value selected from the Transfer status filter. */
	private String selectedTransferStatus = "a";

	/** The list of checked Contact Documents from the data table */
	private final List<EmploymentDocuments> checkBoxSelectedItems = new ArrayList<>();

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAll = false;

	/** Count of rows in table after a filter event occurs. */
	private Integer filteredCount = null;

	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_SELECT = 10;

	/** If true then allow previously sent documents to be transferred again. */
	private Boolean allowSent = false;

	/** True if the user has chosen to view the contact documents list for all project in a commercial production */
	private Boolean showAllProjects = false;

	/** The list of checked Contact Document Infos from the data table */
	private final List<ContactDocumentInfo> selectedDocumentIds = new ArrayList<>();

	private static final int ACT_TRANSFER = 11;

	private static final int ACT_EXPORT = 12;

	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_INCLUDE = 13;

	/** Transfer to performer. Currently being used for Canada Talent */
	private static final int ACT_TRANSFER_TO_PERFORMER = 14;

	/* Transfer to Canadian Union Office */
	private static final int ACT_TRANSFER_TO_OFFICE = 15;

	/* Transfer to Biller - LS-2196 */
	private static final int ACT_TRANSFER_TO_BILLER = 16;

	/** action code for popup confirmation/prompt dialog. */
//	private static final int ACT_SHOW_WARNING = 14;

	private final Production production;

	private ContactDocumentDAO contactDocumentDAO;
	private transient FormActraContractDAO formActraContractDAO;
	private transient FormActraWorkPermitDAO formActraWorkPermitDAO;
	private transient FormUDAContractDAO formUDAContractDAO;

	/** The list of selected Contact Documents from the data table */
	private List<ContactDocument> selectedContactDocuments = new ArrayList<>();

	/** Unique Number of Contacts of Contact Documents to be transferred.*/
	private Integer contactCount = 0;

	/** Count of listed (displayed) documents. */
	private int documentCount = 0;

	/** Unique Number of Employments of Contact Documents to be transferred. */
	private Integer employmentCount = 0;

	/** List of form types to be shown on the transfer table. */
	private List<PayrollFormType> selectedFormTypeList = null;

	/** True iff the current production uses the Team payroll service. */
	private Boolean isTeamPayroll;

	static {
		statusList = new ArrayList<>();
		statusList.add(new SelectItem(null, "All"));
		statusList.add(new SelectItem(ApprovalStatus.APPROVED, "Approved/Final"));
		statusList.add(new SelectItem(ApprovalStatus.SUBMITTED, "Submitted/Unapproved"));
		statusList.add(new SelectItem(ApprovalStatus.OPEN, "Unsubmitted"));
		statusList.add(new SelectItem(ApprovalStatus.PENDING, "Pending"));

		transferStatusList = new ArrayList<>();
		transferStatusList.add(new SelectItem("a", "All"));
		transferStatusList.add(new SelectItem("s", "Sent"));
		transferStatusList.add(new SelectItem("u", "Unsent"));
	}


	public TransferDocBean() {
		super("TransferBean.");
		log.debug("---------TransferDocBean-------");
		production = SessionUtils.getNonSystemProduction();
		selectedBiller = new SelectionItem();
//		getEmploymentList();
	}

	public static TransferDocBean getInstance() {
		return (TransferDocBean)ServiceFinder.findBean("transferDocBean");
	}

	/** Creates the Employment wrapper for the Transfer page.
	 * @param sent
	 * @param status
	 */
	public void createEmploymentList(String sent, ApprovalStatus status, boolean showAll) {
		Boolean sentStatus = (sent != null && (! sent.equals("a"))) ? (sent.equals("s") ? true : false) : null;
		log.debug("selectedFormTypeList = " + ((selectedFormTypeList != null) ? selectedFormTypeList.size() : "null"));
		List<Object[]> result = EmploymentDAO.getInstance().fetchTransferRecords(sentStatus, status, showAll, selectedFormTypeList);
		employmentList = new ArrayList<>();
		formTypeList = new ArrayList<>();
		List<Integer> empIdList = new ArrayList<>();
		Map<String, TransferDocItem> empFormTypeCellMap = new HashMap<>();
		List<ContactDocumentInfo> cdInfoList = new ArrayList<>();
		filteredCount = null; // use list size until a filter event occurs
		documentCount = 0;

		for (Object[] row : result) {
			if (! empIdList.contains(row[0])) {
				EmploymentDocuments emp = new EmploymentDocuments((Integer)row[0], (String)row[1], (String)row[2], (String)row[3]);
				empIdList.add((Integer)row[0]);
				employmentList.add(emp);
				emp.getEmpMasterCheck().setId(emp);
				emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
				emp.getEmpMasterCheck().setHolder(this);
			}
			documentCount++;
		}
		log.debug("employmentList = " + employmentList.size());
		int innerCount = 0; // debug/performance analysis

		int ix = 0;

		for (EmploymentDocuments wrapper : employmentList) {
			empFormTypeCellMap = new HashMap<>();
			resultLoop:
			// loop for the Form Type
			for (; ix < result.size(); ix++) {
				// We can take advantage of the fact that the employmentList and the result list are both
				// in the same order (employmentId), so we don't need to re-scan the entire result list
				// each time we start on the next 'wrapper'. Just continue with the next unprocessed entry.
				Object[] row = result.get(ix);
				if (wrapper.getEmploymentId().equals(row[0])) {
					cdInfoList = new ArrayList<>();
					PayrollFormType formType = (PayrollFormType)row[4];
					if (!FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM) && formType.isModelRelease()) {
						// LS-3352  Do not show Model Release Form column on Transfer page
						continue;
					}
					if (formType.getName() != null && (! formTypeList.contains(formType.getName()))) {
						formTypeList.add(formType.getName());
					}
//					else if (formType == PayrollFormType.OTHER && (! formTypeList.contains("Other"))) {
//						formTypeList.add("Other"); // future - may do something with custom docs
//					}
					// since the result is sorted by formType, the following compare would never be true.
					// loop for the list of contact documents
					for (; ix < result.size(); ix++) {
						Object[] r = result.get(ix);
						innerCount++;
						if (wrapper.getEmploymentId().equals(r[0])) {
							if (formType.equals(r[4])) {
								cdInfoList.add(new ContactDocumentInfo((Integer)r[5], (ApprovalStatus)r[6], (Date)r[7], (Date)r[8]));
							}
							else {
								ix--; // re-test this entry in outer for-loop
								break;
							}
						}
						else {
							// done with this wrapper entry; since 'result' list is sorted
							// in employmentId order, no need to finish inner loop.
							// Add this form's list of docs to map
							if (cdInfoList != null && (! cdInfoList.isEmpty())) {
								empFormTypeCellMap.put(formType.name(), new TransferDocItem(cdInfoList, cdInfoList.size(), wrapper.getEmploymentId()));
							}
							// break to terminate outer result loop:
							break resultLoop;
						}
					}
					// we hit this once, when inner 'for' reaches end of result list:
					if (cdInfoList != null && (! cdInfoList.isEmpty())) {
						empFormTypeCellMap.put(formType.name(), new TransferDocItem(cdInfoList, cdInfoList.size(), wrapper.getEmploymentId()));
					}
				}
			}
			// finished gathering all documents for one employmentId
			wrapper.setMapOfDocumentItems(empFormTypeCellMap);
		}
		for (EmploymentDocuments emp : employmentList) {
			for (PayrollFormType type : PayrollFormType.values()) {
				if (emp.getMapOfDocumentItems().get(type.name()) == null && type != PayrollFormType.SUMMARY &&
						 type != PayrollFormType.PROJECT &&  type != PayrollFormType.T_W_ALLOC) {
					emp.getMapOfDocumentItems().put(type.name(), new TransferDocItem(emp.getEmploymentId()));
				}
				if (emp.getMapOfDocumentItems().get(type.name()) != null) {
					emp.getMapOfDocumentItems().get(type.name()).getCheckBox().setHolder(this);
					emp.setBoxVisibilityByType(type, getAllowSent());
				}
			}
		}
		if (selectedFormTypeList != null && (! selectedFormTypeList.isEmpty())) {
			for (PayrollFormType type : selectedFormTypeList) {
				if (type.getName() != null && (! formTypeList.contains(type.getName()))) {
					formTypeList.add(type.getName());
				}
//				else if (type == PayrollFormType.OTHER && (! formTypeList.contains("Other"))) {
//					formTypeList.add("Other"); // future - may do something with custom docs
//				}
			}
		}
		else if (selectedFormTypeList == null) {
			selectedFormTypeList = new ArrayList<>();
			for (String str : formTypeList) {
				selectedFormTypeList.add(PayrollFormType.toValue(str));
			}
		}
		log.debug("results=" + result.size() + ", employments=" + employmentList.size()+  ", inner loop executions=" + innerCount);
	}

	/** Listener for the components Approval status filter, Transfer status filter and
	 *  show all projects checkbox on the Transfer tab.
	 * @param event
	 */
	public void listenFilterType(ValueChangeEvent event) {
		log.debug("");
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// Need to schedule event for later - after "setXxxx()" are called from framework.
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		if (event != null && event.getComponent() != null) {
			String id = event.getComponent().getId();
			log.debug("event.getNewValue() = " + event.getNewValue());
			log.debug("ID = " + id);
			if (id.equals("status")) {
				setEmploymentList(null);
				createEmploymentList(getSelectedTransferStatus(), (ApprovalStatus)event.getNewValue(), getShowAllProjects());
			}
			else if (id.equals("transferStatus")) {
				setEmploymentList(null);
				createEmploymentList((String)event.getNewValue(), getSelectedStatus(), getShowAllProjects());
			}
			else if (id.equals("showAll")) {
				log.debug("SHOW ALL  = " + getShowAllProjects());
				selectedFormTypeList = null;
				setEmploymentList(null);
				createEmploymentList(getSelectedTransferStatus(), getSelectedStatus(), getShowAllProjects());
			}
			else if (id.equals("allowPrevious")) {
				log.debug("allow Previous  = " + getAllowSent());
				if (getAllowSent()) {
					for (EmploymentDocuments emp : employmentList) {
						Boolean empSelected = false;
						for (PayrollFormType type : PayrollFormType.values()) {
							emp.setBoxVisibilityByType(type, getAllowSent());
							if (empSelected != null && emp.getMapOfDocumentItems().get(type.name()) != null) {
								if (emp.getMapOfDocumentItems().get(type.name()).getCheckBox().isChecked()) {
									empSelected = true;
								}
								else if (emp.getMapOfDocumentItems().get(type.name()).getCheckBox().isUnchecked() && empSelected != null && empSelected) {
									empSelected = null;
								}
								else if (emp.getMapOfDocumentItems().get(type.name()).getCheckBox().isPartiallyChecked()) {
									empSelected = null;
								}
							}
						}
						if (empSelected == null) {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
						}
						else if (empSelected) {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
						}
						else {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
						}
					}
				}
				else if (! getAllowSent()) {
					log.debug("");
					for (EmploymentDocuments emp : employmentList) {
						// Determines the selection state of an employment record.
						Boolean empSelected = false;
						for (String key : emp.getMapOfDocumentItems().keySet()) {
							if (emp.getMapOfDocumentItems().get(key) != null &&
									emp.getMapOfDocumentItems().get(key).getDocInfoList() != null) {
								emp.setBoxVisibilityByType(PayrollFormType.stringToValue(key), getAllowSent());
								// Determines the selection state of a cell.
								Boolean selected = false;
								for (ContactDocumentInfo cd : emp.getMapOfDocumentItems().get(key).getDocInfoList()) {
									if (cd.getDisabled() && cd.getSelected()) {
										cd.setSelected(false);
										selectedDocumentIds.remove(cd);
										/*if (selected == null || selected) {
											selected = null;
										}*/
									}
									else if ((! cd.getDisabled()) && (! cd.getSelected()) && (selected == null || selected) ) {
										selected = null;
									}
									else if ((! cd.getDisabled()) && cd.getSelected() && (selected != null)) {
										selected = true;
									}
								}
								if (selected == null) {
									emp.getMapOfDocumentItems().get(key).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
									empSelected = null;
								}
								else if (selected) {
									emp.getMapOfDocumentItems().get(key).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_ON);
									if (empSelected != null) {
										empSelected = true;
									}
								}
								else if (! selected) {
									emp.getMapOfDocumentItems().get(key).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
								}
							}
						}
						if (empSelected == null) {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
						}
						else if (empSelected) {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
						}
						else {
							emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
						}
					}
				}
			}
		}
	}

	/** Value change listener for master-checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenCheckedForAll(ValueChangeEvent evt) {
		try {
			log.debug("");
			if (getCheckedForAll() && employmentList != null) {
				for (EmploymentDocuments empRow : employmentList) {
					empRow.setSelected(true);
					if (! checkBoxSelectedItems.contains(empRow)) {
						if (empRow.getMapOfDocumentItems() != null && (! empRow.getMapOfDocumentItems().isEmpty())) {
							boolean foundInRow = false;
							for (String type : empRow.getMapOfDocumentItems().keySet()) {
								boolean found = false;
								for (ContactDocumentInfo cdInfo : empRow.getMapOfDocumentItems().get(type).getDocInfoList()) {
									if (! cdInfo.getDisabled()) {
										log.debug("CD = " + cdInfo.getContactDocumentId());
										found = true;
										cdInfo.setSelected(true);
										if (! selectedDocumentIds.contains(cdInfo)) {
											selectedDocumentIds.add(cdInfo);
										}
									}
								}
								log.debug("Type = " + type);
								log.debug("Selected docs = " + selectedDocumentIds.size());
								if (found) {
									foundInRow = true;
									empRow.selectCheckBoxByType(PayrollFormType.stringToValue(type), TriStateCheckboxExt.CHECK_ON);
								}
							}
							if (foundInRow) { // at least one doc in this empRow was selected
								empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
								checkBoxSelectedItems.add(empRow); // only add to checked list if a document in row was selected
							}
						}
					}
				}
			}
			else {
				checkedForAll = false;
				for (EmploymentDocuments empRow : employmentList) {
					empRow.setSelected(false);
					for (PayrollFormType formType : PayrollFormType.values()) {
						empRow.selectCheckBoxByType(formType, TriStateCheckboxExt.CHECK_OFF);
					}
					empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
				}
				for (ContactDocumentInfo cdRow : selectedDocumentIds) {
					cdRow.setSelected(false);
				}
				checkBoxSelectedItems.clear();
				selectedDocumentIds.clear();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @return the total number of employments in the table.
	 */
	public int getTotalEmploymentCount() {
		if (filteredCount != null) {
			return filteredCount;
		}
		if (getEmploymentList() != null) {
			return employmentList.size();
		}
		return 0;
	}

	/**
	 * @return the number of employments selected from the list.
	 */
	public int getSelectedEmploymentCount() {
		if (checkBoxSelectedItems != null) {
			return checkBoxSelectedItems.size();
		}
		return 0;
	}

	/**
	 * @return the total number of documents in the table.
	 */
	public int getTotalDocumentCount() {
		getEmploymentList(); // may compute document count if list was not built yet.
		if (documentCount == 0) { // may need to be recalculated
			documentCount = calculateDocumentCount(employmentList);
		}
		return documentCount;
	}

	/**
	 * @return the number of documents selected from the list.
	 */
	public int getSelectedDocumentCount() {
		if (selectedDocumentIds != null) {
			return selectedDocumentIds.size();
		}
		return 0;
	}

	/** Action method used to open a popup to show multiple documents of same type,
	 * 	on the click of number which represent those documents.
	 * @param evt
	 * @return
	 */
	public String actionSelectDocuments(ActionEvent evt) {
		String type = (String) evt.getComponent().getAttributes().get("type");
		PopupSelectDocumentsBean bean = PopupSelectDocumentsBean.getInstance();
		bean.setCheckedForAll(false);
		PayrollFormType formType = PayrollFormType.valueOf(type);
		log.debug("formType = " + formType);
		Integer employmentId = (Integer) evt.getComponent().getAttributes().get("employmentId");
		log.debug("employmentId = " + employmentId);
		List<ContactDocumentInfo> cdList = new ArrayList<>();
		TransferDocItem item = null;
		for (EmploymentDocuments empRecord : getEmploymentList()) {
			if (empRecord.getEmploymentId().equals(employmentId)) {
				item = empRecord.getMapOfDocumentItems().get(type);
				cdList = empRecord.getMapOfDocumentItems().get(type).getDocInfoList();
				bean.setCheckedForAll(true);
				for (ContactDocumentInfo cd : cdList) {
					if ((! cd.getDisabled()) && (! cd.getSelected())) {
						bean.setCheckedForAll(false);
						break;
					}
				}
				break;
			}
		}
		bean.show(this, ACT_SELECT, "SelectDocumentsBean.SelectDocuments.", item, formType);
		bean.setMessage(MsgUtils.formatMessage("SelectDocumentsBean.SelectDocuments.Text", formType.getName()));
		return null;
	}

	/** Method called on the click event of "Ok" button
	 * on "Select Documents" popup.
	 * @return
	 */
	public String actionSelectDocumentsOk() {
		log.debug("");
		PopupSelectDocumentsBean bean = PopupSelectDocumentsBean.getInstance();
		if (bean.getTransferDocItem() != null) {
			Integer empId = bean.getTransferDocItem().getEmploymentId();
			EmploymentDocuments empRow = null;
			for (EmploymentDocuments row : getEmploymentList()) {
				if (row.getEmploymentId().equals(empId)) {
					empRow = row;
					break;
				}
			}
			if (empRow != null) { // we found matching employment entry
				Boolean allSelected = null;
				boolean unchecked = false; // have we found an un-selected item yet?
				if (bean.getContactDocumentInfoMap() != null && bean.getSelectedFormType() != null) {
					log.debug("");
					for (ContactDocumentInfo cd : empRow.getMapOfDocumentItems().get(bean.getSelectedFormType().name()).getDocInfoList()) {
						cd.setSelected(bean.getContactDocumentInfoMap().get(cd));
						if (! cd.getDisabled()) {
							if (cd.getSelected()) {
								if (! selectedDocumentIds.contains(cd)) {
									selectedDocumentIds.add(cd);
								}
								if (allSelected == null || allSelected) {
									allSelected = true;
								}
								log.debug("");
								if (unchecked) { // already had at least one un-checked
									allSelected = false; // must be mixed
									//break; // once it's false, it won't change
								}
								//allSelected = true; // so far, only checked items found
							}
							else if (! cd.getSelected()) {
								if (selectedDocumentIds.contains(cd)) {
									selectedDocumentIds.remove(cd);
								}
								if (allSelected != null && allSelected) { // already had at least one checked
									log.debug("");
									allSelected = false; // must be mixed
									//break; // once it's false/mixed, it won't change
								}
								unchecked = true;
							}
						}
					}
					if (allSelected == null) {
						log.debug("");
						empRow.getMapOfDocumentItems().get(bean.getSelectedFormType().name()).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
					}
					else if (allSelected) {
						log.debug("");
						empRow.getMapOfDocumentItems().get(bean.getSelectedFormType().name()).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_ON);
					}
					else { // must be false
						log.debug("");
						empRow.getMapOfDocumentItems().get(bean.getSelectedFormType().name()).getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
					}
				}
				log.debug("");
				allSelected = null;
				unchecked = false; // have we found an un-selected item yet?
				typeLoop:
				for (String key : empRow.getMapOfDocumentItems().keySet()) {
					if (empRow.getMapOfDocumentItems().get(key) != null && empRow.getMapOfDocumentItems().get(key).getDocInfoList() != null) {
						for (ContactDocumentInfo cd : empRow.getMapOfDocumentItems().get(key).getDocInfoList()) {
							if (! cd.getDisabled()) {
								if (cd.getSelected() && (allSelected == null || allSelected)) {
									log.debug("");
									if (unchecked) { // already had at least one un-checked
										allSelected = false; // must be mixed
										break; // once it's false, it won't change
									}
									allSelected = true;
								}
								else if (! cd.getSelected()) {
									if (allSelected != null && allSelected) { // already had at least one checked
										log.debug("");
										allSelected = false; // must be mixed
										break typeLoop; // once it's false/mixed, it won't change
									}
									unchecked = true;
								}
							}
						}
					}
				}
				if (allSelected == null) {
					log.debug("");
					empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
				}
				else if (allSelected) {
					log.debug("");
					empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
				}
				else { // must be false
					log.debug("");
					empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
				}
			}
		}
		return null;
	}

	/** Action method used to open a popup to show include documents popup
	 * 	on the click "Include Docs" button.
	 * @param evt
	 * @return
	 */
	public String actionIncludeDocuments() {
		PopupSelectDocumentsBean bean = PopupSelectDocumentsBean.getInstance();
		if (selectedFormTypeList == null) {
			selectedFormTypeList = new ArrayList<>();
		}
		for (String str : formTypeList) {
			if (! selectedFormTypeList.contains(PayrollFormType.toValue(str))) {
				selectedFormTypeList.add(PayrollFormType.toValue(str));
			}
		}
		List<PayrollFormType> availableFormTypeList =
				ContactDocumentDAO.getInstance().findProductionFormTypes(SessionUtils.getNonSystemProduction(), null);
		if (availableFormTypeList != null && (! availableFormTypeList.isEmpty())) {
			log.debug("");
			// LS-2236 TTCO document Transfer page options to exclude I9
//			if (isTeamPayroll()) {
//				Iterator<PayrollFormType> iter = selectedFormTypeList.iterator();
//				for (; iter.hasNext();) {
//					PayrollFormType item = iter.next();
//					if (item.equals(PayrollFormType.I9)) {
//						// LS-2236 TTCO document Transfer page options to exclude I9.
//						iter.remove();
//						break;
//					}
//				}
//				Iterator<PayrollFormType> listIter = availableFormTypeList.iterator();
//				for (; listIter.hasNext();) {
//					PayrollFormType item = listIter.next();
//					if (item.equals(PayrollFormType.I9)) {
//						// LS-2236 TTCO document Transfer page options to exclude I9.
//						listIter.remove();
//						break;
//					}
//				}
//			}
			bean.show(this, ACT_INCLUDE, "SelectDocumentsBean.IncludeDocuments.", selectedFormTypeList, availableFormTypeList);
		}
		else {
			log.debug("");
			MsgUtils.addFacesMessage("TransferBean.EmptyInclude", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	public String actionIncludeDocumentsOk() {
		log.debug("");
		PopupSelectDocumentsBean bean = PopupSelectDocumentsBean.getInstance();
		selectedFormTypeList = new ArrayList<>();
		if (bean.getFormTypeMap() != null) {
			for (PayrollFormType type : bean.getFormTypeMap().keySet()) {
				if (bean.getFormTypeMap().get(type)) {
					selectedFormTypeList.add(type);
				}
			}
//			if (selectedFormTypeList != null && (! selectedFormTypeList.isEmpty())) {
			setEmploymentList(null);
			createEmploymentList(getSelectedTransferStatus(), getSelectedStatus(), getShowAllProjects());
//			}
		}
		return null;
	}

	/** Action method called on the click event of "Transfer" button.
	 * @return null navigation string
	 */
	public String actionTransfer(String transferTo) {
		try {
			contactCount = 0;
			employmentCount = 0;
			PayrollPreference payrollPref = null;
			selectedContactDocuments = new ArrayList<>();
			if (production.getType().hasPayrollByProject()) {
				payrollPref = SessionUtils.getCurrentOrViewedProject().getPayrollPref();
			}
			else {
				payrollPref = production.getPayrollPref();
			}
			PayrollService service = production.getPayrollPref().getPayrollService();
			if (service == null) {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoService", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (! production.getType().isCanadaTalent()) {
				if (payrollPref != null &&
						(! payrollPref.isUseOnboardEmail() || StringUtils.isEmpty(payrollPref.getOnboardEmailAddress()))) {
					MsgUtils.addFacesMessage("TransferBean.SetEmailPreference", FacesMessage.SEVERITY_ERROR);
					return null;
				}
			}
			Set<Integer> contactIdList = new HashSet<>();
			Set<Integer> empIdList = new HashSet<>();
			if (selectedDocumentIds != null && selectedDocumentIds.size() > 0) {
				int transferCount = 0;
				for (ContactDocumentInfo cd : selectedDocumentIds) {
					transferCount++;
					ContactDocument cdoc = getContactDocumentDAO().findById(cd.getContactDocumentId());
					if (cdoc != null) {
						// LS-2455  if document is  I-9 and TeamPayrollClient then not adding in empIdList for employmentCount
						if ((! isTeamPayroll()) || (cdoc.getFormType() != PayrollFormType.I9)) {
							empIdList.add(cdoc.getEmployment().getId());
						}
						contactIdList.add(cdoc.getContact().getId());
						if (! selectedContactDocuments.contains(cdoc)) {
							selectedContactDocuments.add(cdoc);
						}
					}
				}
				if (transferCount > 0) {
					contactCount = contactIdList.size();
					employmentCount = empIdList.size();
					PopupBean bean = PopupBean.getInstance();
					int action = ACT_TRANSFER;

					if (production.getType().isCanadaTalent()) {
						if (transferTo.equals(TRANSFER_TO_PERFORMER)) {
							action = ACT_TRANSFER_TO_PERFORMER;
						}
						else if (transferTo.equals(TRANSFER_TO_OFFICE)) {
							action = ACT_TRANSFER_TO_OFFICE;
						}
						// LS-2196 Send to TPS button functionality
						else if (transferTo.equals(TRANSFER_TO_BILLER)) {
							if (selectedBiller == null || selectedBiller.getId() == null) {
								// No Biller was selected so return error message.
								MsgUtils.addFacesMessage("TransferBean.NoOfficeSelected.Error",
										FacesMessage.SEVERITY_ERROR);
								return null;
							}
							action = ACT_TRANSFER_TO_BILLER;
						}
					}

					bean.show(this, action, "TransferBean.Transfer.");
					if (employmentCount == contactCount) {

						//LS-1961 Transfer screen Verbiage
						if (production.getType().isCanadaTalent()) {
							if (transferTo.equals(TRANSFER_TO_OFFICE)) {
								bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.IsCanadaOffice.Text", transferCount, contactCount));
							}
							else if (transferTo.equals(TRANSFER_TO_PERFORMER)) {
								bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.IsCanadaPerformer.Text", transferCount, contactCount));
							}
							else if (transferTo.equals(TRANSFER_TO_BILLER)) {
								bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.IsCanadaBiller.Text", transferCount, contactCount));
							}
						}
						else {
							bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.Text", transferCount, contactCount));
						}

					}
					else {
						// show the employment count along with contact count when they're not equal
						bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.Text2", transferCount, contactCount, employmentCount));
					}
				}
				else {
					MsgUtils.addFacesMessage("TransferBean.NoneEligible", FacesMessage.SEVERITY_INFO);
				}
			}
			else {
				MsgUtils.addFacesMessage("TransferBean.TransferInfo", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Action method called on the click event of "Export" button.
	 * @return null navigation string
	 */
	public String actionExport() {
		try {
			log.debug(" Export ");
			int count = 0;
			selectedContactDocuments = new ArrayList<>();
			for (ContactDocumentInfo cd : selectedDocumentIds) {
				ContactDocument cdoc = getContactDocumentDAO().findById(cd.getContactDocumentId());
				if (cdoc != null) {
					if (cdoc.getDocument() != null && (! cdoc.getDocument().getStandard())) {
						count++;
						selectedContactDocuments.add(cdoc);
					}
				}
			}
			if (count > 0) {
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_EXPORT, "TransferBean.ExportDocuments.");
				bean.setMessage(MsgUtils.formatMessage("TransferBean.ExportDocuments.Text", count));
			}
			else {
				MsgUtils.addFacesMessage("TransferBean.ExportInvalid", FacesMessage.SEVERITY_INFO);
				return null;
			}
		}
		catch (Exception e) {
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
		String res = null;
		switch(action) {
			case ACT_TRANSFER:
				actionTransferOk();
				break;
			case ACT_EXPORT:
				actionExportOk();
				break;
			case ACT_SELECT:
				actionSelectDocumentsOk();
				break;
			case ACT_INCLUDE:
				actionIncludeDocumentsOk();
				break;
			case ACT_TRANSFER_TO_PERFORMER:
				actionTransferToIndividualOk();
				break;
			case ACT_TRANSFER_TO_OFFICE:
				actionTransferToOfficeOk();
				break;
			case ACT_TRANSFER_TO_BILLER:
				actionTransferToBillerOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/** Method called on the click event of "Send" button on "Transfer Documents"
	 *  popup. Transfers the selected documents to the payroll service.
	 * @return
	 */
	private String actionTransferOk() {
		try {
			if (selectedContactDocuments != null && (! selectedContactDocuments.isEmpty())) {
				log.debug("");
				Project project = null;
				String projectId = "";
				DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
				String timestamp = df.format(new Date());
				boolean ret = false;
				Integer bad = 0;
				PdfGroupingType type = production.getPayrollPref().getPdfGroupingType();
				Map<Object, List<ContactDocument>> pdfGroupingMap = new HashMap<>();
				PayrollPreference prefer = production.getPayrollPref();
				String realReportPath = SessionUtils.getRealReportPath();
				Map<String, Integer> mapOfErorrCounts = new HashMap<>();
				Integer docWithBadAttachment = 0;
				Integer success = selectedContactDocuments.size();

				if (production.getType().isAicp()) {
					project = SessionUtils.getCurrentProject();
					projectId = "-" + project.getEpisode(); // commercial "project number"
					prefer = project.getPayrollPref();
					log.debug("");
				}

				// create map
				pdfGroupingMap = DocumentService.groupContactDocuments(type, selectedContactDocuments);
				List<String> sendFileList = new ArrayList<>(); // list of files created from groups, to be sent with notification
				String sendFileNameText = ""; // list of filenames to go in email body.

				// Iterate over map and print documents
				for (Map.Entry<Object, List<ContactDocument>> entry : pdfGroupingMap.entrySet()) {
					log.debug("key=" + entry.getKey() + ", value=" + entry.getValue());
					Object key = entry.getKey();
					Employment emp = null;
					PayrollFormType formType = PayrollFormType.OTHER; // avoid NPE warning
					if (key instanceof Employment) {
						emp = (Employment) key;
					}
					else if (key instanceof PayrollFormType) {
						formType = (PayrollFormType) key;
					}

					// Print reports for the ContactDocuments/Attachments
					List<String> groupFileList = new ArrayList<>();
					List<String> userNames = new ArrayList<>();
					mapOfErorrCounts = DocumentService.printGroupDocuments(projectId, timestamp, mapOfErorrCounts, realReportPath,
							entry.getValue(), groupFileList, production, userNames);
					if (groupFileList != null && (! groupFileList.isEmpty())) {
						log.debug("");
						// Merge all the files in 'fileList' - that's one group of files.
						// For outputfilename add a unique string to the file name.
						// For ER group unique string will be employee name + emp.id;
						// and for doc type grouping, it will be doc type name (e.g., "W4").
						String uniqueGroupId = "";
						if (type == PdfGroupingType.EMPLOYEE) {
							emp = EmploymentDAO.getInstance().refresh(emp);
							uniqueGroupId = emp.getContact().getUser().getFirstNameLastName() + "-" + emp.getId();
						}
						else if (type == PdfGroupingType.DOC_TYPE) {
							uniqueGroupId = formType.name();
						}
						if (type != PdfGroupingType.NONE) {
							log.debug("uniqueGroupId = " + uniqueGroupId);
							String outputFileName = production.getProdId() + projectId + "-"
									+ StringUtils.cleanFilename(production.getTitle() + "-"
											+ uniqueGroupId + "-" + timestamp) + ".pdf";
							log.debug("outputFileName = " + outputFileName);
							int finalPdf = PdfUtils.combinePdfs(groupFileList, (realReportPath + outputFileName));
							log.debug("finalPdf = " + finalPdf);
							if (finalPdf != 0) {
								MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError", FacesMessage.SEVERITY_WARN);
							}
							// And THIS outputfilename needs to go into sendFileList to be used below in a notification...
							sendFileList.add(realReportPath + outputFileName);
							sendFileNameText += outputFileName + "<br/>"; // list of merged filenames to go into email body
							if (type == PdfGroupingType.DOC_TYPE) {
								for (String name : userNames) {
									sendFileNameText += "  " + name + "<br/>"; // list of users (related to last file) to go into email body
								}
								sendFileNameText += "<br/>";
							}
							DocumentService.deleteIntermediateFiles(groupFileList);
						}
						else {
							sendFileNameText = printDocsWithoutGrouping(realReportPath, sendFileList, groupFileList);
						}
					}
				}
				log.debug("");
				String attachmentString = "";
				if (mapOfErorrCounts != null && (! mapOfErorrCounts.isEmpty())) {
					for (String key : mapOfErorrCounts.keySet()) {
						String docName = key.substring(((key.indexOf("_")) + 1), (key.lastIndexOf("_")));
						String totalAttachments = key.substring((key.lastIndexOf("_")) + 1);
						if (mapOfErorrCounts.get(key) != null && mapOfErorrCounts.get(key) != 0) {
							docWithBadAttachment++;
							success--;
							attachmentString = attachmentString +
									"Document " + "<b>" + docName + "</b>, " + " with " +
									(Integer.parseInt(totalAttachments) - mapOfErorrCounts.get(key)) +
									" out of " + totalAttachments + " attachments was transferred successfully."
									+ "</br>";
						}
						else if (mapOfErorrCounts.get(key) == null) {
							bad++;
							success--;
						}
					}
				}

				if (production != null && pdfGroupingMap != null && pdfGroupingMap.size() > 0) {
					log.debug("");
					ret = transmitData(project);
					if (! ret) {
						sendFileNameText += " ** ERROR ** Delivery of data file(s) was unsuccessful ** <p/>";
					}
					prefer = PayrollPreferenceDAO.getInstance().refresh(prefer);
					//  LS-2455 email will not send if user selected only I9 document
					int selectedDocCount = selectedContactDocuments.size();
					if (selectedContactDocuments.size() > 0) {
						for (ContactDocument contact : selectedContactDocuments) {
							if ((isTeamPayroll()) && (contact.getFormType() == PayrollFormType.I9)) {
								selectedDocCount--;
							}
						}
					}
					if (selectedDocCount > 0) {
						String emailAddress = prefer.getOnboardEmailAddress();
						Collection<String> emails = new ArrayList<>();
						// Parse multiple email address. LS-1649
						// Replace semi-colon with commas
						emailAddress = emailAddress.replaceAll(";", ",");
						// Parse email address.
						emails.addAll(DocumentTransferService.parseEmailAddressByDelimitter(emailAddress, ","));
						DoNotification.getInstance().sendDocuments(emails, sendFileList, project,
								employmentCount,selectedDocCount , sendFileNameText);
					}
					if (bad == 0 && (docWithBadAttachment == null || docWithBadAttachment == 0)) {
						MsgUtils.addFacesMessage("TransferBean.TransferSuccessful", FacesMessage.SEVERITY_INFO,
								selectedContactDocuments.size());
					}
					else {
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, 0, null, null, "Confirm.Close", null);
						if (docWithBadAttachment == null || docWithBadAttachment == 0) {
							// either all or none
							if (success != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text1", bad,
										success));
							}
							// All documents failed.
							else {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text2", bad));
							}
						}
						else if (docWithBadAttachment != null && docWithBadAttachment != 0) {
							//All
							if (success != 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text3", bad,
										attachmentString, success));
							}
							else if (success != 0 && bad == 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text4",
										attachmentString, (selectedContactDocuments.size() - bad)));
							}
							else if (success == 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text5", bad,
										attachmentString));
							}
							else {
								// Unable to transfer all the document.
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text6",
										attachmentString));
							}
						}
					}
				}
				clearSelections();
				setEmploymentList(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method called on the click event of "Send" button on "Transfer Documents"
	 *  popup. Transfers the selected documents to the payroll service.
	 * @return
	 */
	private String actionTransferToOfficeOk() {
		try {
			if (selectedContactDocuments != null && (! selectedContactDocuments.isEmpty())) {
				log.debug("");
				Project project = null;
				String projectId = "";
				Integer bad = 0;
				String realReportPath = SessionUtils.getRealReportPath();
				Map<String, Integer> mapOfErorrCounts = new HashMap<>();
				Integer docWithBadAttachment = 0;
				Integer success = selectedContactDocuments.size();
				// List of all of the file names to send to the office
				if (production.getType().isAicp()) {
					project = SessionUtils.getCurrentProject();
					projectId = "-" + project.getEpisode(); // commercial "project number"
				}
				// LS-2197 Set ACTRA office, based on the selected Branch code on the ACTRA Contract document
				Map<Office, List<ContactDocument>> docsByOffice = new HashMap<>();
				for (ContactDocument cd : selectedContactDocuments) {
					if (cd.getFormType() == PayrollFormType.ACTRA_CONTRACT) {
						FormActraContract form = getFormActraContractDAO().findById(cd.getRelatedFormId());
						if (form != null && form.getOffice() != null) {
							List<ContactDocument> docList = docsByOffice.get(form.getOffice());
							//If the Office does not exist in the HashMap, create an entry for it.
							if (docList == null) {
								docList = new ArrayList<>();
							}
							docList.add(cd);
							docsByOffice.put(form.getOffice(), docList);//if same office then will update Document List
						}
					}
					// LS-2425 Work Permit document transfer
					else if (cd.getFormType() == PayrollFormType.ACTRA_PERMIT) {
						FormActraWorkPermit workPermitform =
								getFormActraWorkPermitDAO().findById(cd.getRelatedFormId());
						if (workPermitform != null && workPermitform.getOffice() != null) {
							List<ContactDocument> docList =
									docsByOffice.get(workPermitform.getOffice());
							//If the Office does not exist in the HashMap, create an entry for it.
							if (docList == null) {
								docList = new ArrayList<>();
							}
							docList.add(cd);
							docsByOffice.put(workPermitform.getOffice(), docList);//if same office then will update Document List
						}
					}
					else if (cd.getFormType() == PayrollFormType.UDA_INM) {
						FormUDAContract form =getFormUdaContractDao().findById(cd.getRelatedFormId());
						if (form != null && form.getOffice() != null) {
							List<ContactDocument> docList =docsByOffice.get(form.getOffice());
							//If the Office does not exist in the HashMap, create an entry for it.
							if (docList == null) {
								docList = new ArrayList<>();
							}
							docList.add(cd);
							docsByOffice.put(form.getOffice(), docList);//if same office then will update Document List
						}
					}
				}
				DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
				String timestamp = df.format(new Date());

				for (Map.Entry<Office, List<ContactDocument>> docs : docsByOffice.entrySet()) {
					List<ContactDocument> docsByOff = docs.getValue();
					List<String> groupFileList = new ArrayList<>();
					List<String> userNames = new ArrayList<>();
					Office office = docs.getKey();
					mapOfErorrCounts = DocumentService.printGroupDocuments(projectId, timestamp, mapOfErorrCounts, realReportPath,
							docs.getValue(), groupFileList, production, userNames);
					if (groupFileList != null && (! groupFileList.isEmpty())) {
						// Merge all the files in 'fileList' - that's one group of files.
						// For outputfilename add a unique string to the file name.
						// For ER group unique string will be employee name + emp.id;
						String uniqueGroupId = office.getBranchCode();
						String outputFileName = production.getProdId() + projectId + "-"
								+ StringUtils.cleanFilename(production.getTitle() + "-"
										+ uniqueGroupId + "-" + timestamp) + ".pdf";
						int finalPdf = PdfUtils.combinePdfs(groupFileList, (realReportPath + outputFileName));
						if(finalPdf != 0) {
							MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError", FacesMessage.SEVERITY_WARN);
						}
						else {
							String officeEmail = office.getEmailAddress();
							// Send to selected office and group all docs into one file.
							List<String> fileNames = new ArrayList<>();
							fileNames.add(realReportPath + outputFileName);
							if(officeEmail != null && !officeEmail.isEmpty()) {
								officeEmail = officeEmail.replaceAll(";", ",");
								Collection<String> emailList = new ArrayList<>();
								emailList.addAll(DocumentTransferService.parseEmailAddressByDelimitter(officeEmail, ","));
								for (ContactDocument doc : docsByOff) {
									if (doc.getFormType() == PayrollFormType.ACTRA_CONTRACT) {
										// Update the office sent list of the forms that track office history for Actra Contracts
										FormActraContract form = getFormActraContractDAO().findById(doc.getRelatedFormId());
										form.getOffices().add(office);
										getFormActraContractDAO().merge(form);
									}
									doc.setSentToUnion(true);
									ContactDocumentDAO.getInstance().merge(doc);
								}
								DoNotification.getInstance().sendIndividualOfficeDocuments(emailList, project,  fileNames, office.getOfficeType().getLabel());
							}
						}
						DocumentService.deleteIntermediateFiles(groupFileList);
					}
				}
				// create map - PDF Grouping will always be Employee. Only send documents to specified employee.
				log.debug("");
				String attachmentString = "";
				if (mapOfErorrCounts != null && (! mapOfErorrCounts.isEmpty())) {
					for (String key : mapOfErorrCounts.keySet()) {
						String docName = key.substring(((key.indexOf("_")) + 1), (key.lastIndexOf("_")));
						String totalAttachments = key.substring((key.lastIndexOf("_")) + 1);
						if (mapOfErorrCounts.get(key) != null && mapOfErorrCounts.get(key) != 0) {
							docWithBadAttachment++;
							success--;
							attachmentString = attachmentString +
									"Document " + "<b>" + docName + "</b>, " + " with " +
									(Integer.parseInt(totalAttachments) - mapOfErorrCounts.get(key)) +
									" out of " + totalAttachments + " attachments was transferred successfully."
									+ "</br>";
						}
						else if (mapOfErorrCounts.get(key) == null) {
							bad++;
							success--;
						}
					}
				}
				if (production != null) {
					transmitData(project);
					if (bad == 0 && (docWithBadAttachment == null || docWithBadAttachment == 0)) {
						MsgUtils.addFacesMessage("TransferBean.TransferSuccessful.CanadaOffice", FacesMessage.SEVERITY_INFO,
								selectedContactDocuments.size());
					}
					else {
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, 0, null, null, "Confirm.Close", null);
						if (docWithBadAttachment == null || docWithBadAttachment == 0) {
							// either all or none
							if (success != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text1", bad,
										success));
							}
							// All documents failed.
							else {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text2", bad));
							}
						}
						else if (docWithBadAttachment != null && docWithBadAttachment != 0) {
							//All
							if (success != 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text3", bad,
										attachmentString, success));
							}
							else if (success != 0 && bad == 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text4",
										attachmentString, (selectedContactDocuments.size() - bad)));
							}
							else if (success == 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text5", bad,
										attachmentString));
							}
							else {
								// Unable to transfer all the document.
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text6",
										attachmentString));
							}
						}
					}
				}
				clearSelections();
				setEmploymentList(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method called on the click event of "Send to TPS" button on "Transfer
	 * Documents". Transfers the selected documents to the Biller. - LS-2196
	 *
	 * @return
	 */
	private String actionTransferToBillerOk() {
		try {
			if (selectedContactDocuments != null && (! selectedContactDocuments.isEmpty())) {
				log.debug("");
				Project project = SessionUtils.getCurrentProject();
				String projectId = "";
				DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
				String timestamp = df.format(new Date());
				Integer bad = 0;
				int docCount = 0;
				// PDF Grouping will always be Employee. Only send documents to specified employee.
				PdfGroupingType type = PdfGroupingType.EMPLOYEE;
				Map<Object, List<ContactDocument>> pdfGroupingMap = new HashMap<>();
				String realReportPath = SessionUtils.getRealReportPath();
				Map<String, Integer> mapOfErorrCounts = new HashMap<>();
				Integer docWithBadAttachment = 0;
				Integer success = selectedContactDocuments.size();
				List<String> allFileNames = new ArrayList<>();

				if (production.getType().isAicp()) {
					project = SessionUtils.getCurrentProject();
					projectId = "-" + project.getEpisode(); // commercial "project number"
				}
				// create map - PDF Grouping will always be Employee. Only send documents to specified employee.
				pdfGroupingMap = DocumentService.groupContactDocuments(PdfGroupingType.EMPLOYEE,
						selectedContactDocuments);
				// Iterate over map and print documents
				for (Map.Entry<Object, List<ContactDocument>> entry : pdfGroupingMap.entrySet()) {
					log.debug("key=" + entry.getKey() + ", value=" + entry.getValue());
					Object key = entry.getKey();
					Employment emp = (Employment)key;
					// Print reports for the ContactDocuments/Attachments
					List<String> groupFileList = new ArrayList<>();
					List<String> userNames = new ArrayList<>();
					mapOfErorrCounts = DocumentService.printGroupDocuments(projectId, timestamp,
							mapOfErorrCounts, realReportPath, entry.getValue(), groupFileList,
							production, userNames);
					if (groupFileList != null && (! groupFileList.isEmpty())) {
						log.debug("");
						// Merge all the files in 'fileList' - that's one group of files.
						// For outputfilename add a unique string to the file name.
						// For ER group unique string will be employee name + emp.id;
						String uniqueGroupId = "";
						emp = EmploymentDAO.getInstance().refresh(emp);
						uniqueGroupId = emp.getContact().getUser().getFirstNameLastName() + "-" +
								emp.getId();

						if (type != PdfGroupingType.NONE) {
							log.debug("uniqueGroupId = " + uniqueGroupId);
							String outputFileName = production.getProdId() + projectId + "-" +
									StringUtils.cleanFilename(production.getTitle() + "-" +
											uniqueGroupId + "-" + timestamp) +
									".pdf";
							log.debug("outputFileName = " + outputFileName);
							// Collect all of the file names to send to the payroll service.
							allFileNames.add(realReportPath + outputFileName);
							int finalPdf = PdfUtils.combinePdfs(groupFileList,
									(realReportPath + outputFileName));
							log.debug("finalPdf = " + finalPdf);
							if (finalPdf != 0) {
								MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError",
										FacesMessage.SEVERITY_WARN);
							}
							else {
								docCount++;
							}
							for (ContactDocument doc : entry.getValue()) {
								doc.setSentToTPS(true);
								ContactDocumentDAO.getInstance().merge(doc);
							}
							DocumentService.deleteIntermediateFiles(groupFileList);
						}
					}
				}

				// Send to selected Biller and group all docs into one file.
				if (docCount > 0) {
					String outputFileName =
							production.getProdId() + projectId + "-" +
									StringUtils.cleanFilename(
											production.getTitle() + "-AllDocs-" + timestamp) +
									".pdf";
					Collection<String> fileNames = new ArrayList<>();

					fileNames.add(realReportPath + outputFileName);
					Collection<String> emailList = new ArrayList<>();

					String billerEmail = selectedBiller.getLabel();
					if (billerEmail != null) {
						billerEmail = billerEmail.replaceAll(";", ",");
						emailList.add(billerEmail);
					}

					PdfUtils.combinePdfs(allFileNames, (realReportPath + outputFileName));
					DoNotification.getInstance().sendDocuments(emailList, fileNames, project,
							contactCount, docCount, "");

				}

				log.debug("");
				String attachmentString = "";
				if (mapOfErorrCounts != null && (! mapOfErorrCounts.isEmpty())) {
					for (String key : mapOfErorrCounts.keySet()) {
						String docName =
								key.substring(((key.indexOf("_")) + 1), (key.lastIndexOf("_")));
						String totalAttachments = key.substring((key.lastIndexOf("_")) + 1);
						if (mapOfErorrCounts.get(key) != null && mapOfErorrCounts.get(key) != 0) {
							docWithBadAttachment++;
							success--;
							attachmentString = attachmentString + "Document " + "<b>" + docName +
									"</b>, " + " with " +
									(Integer.parseInt(totalAttachments) -
											mapOfErorrCounts.get(key)) +
									" out of " + totalAttachments +
									" attachments was transferred successfully." + "</br>";
						}
						else if (mapOfErorrCounts.get(key) == null) {
							bad++;
							success--;
						}
					}
				}

				if (production != null && pdfGroupingMap != null && pdfGroupingMap.size() > 0) {
					transmitData(project);

					if (bad == 0 && (docWithBadAttachment == null || docWithBadAttachment == 0)) {
						MsgUtils.addFacesMessage("TransferBean.TransferSuccessful.CanadaBiller",
								FacesMessage.SEVERITY_INFO, selectedContactDocuments.size());
					}
					else {
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, 0, null, null, "Confirm.Close", null);
						if (docWithBadAttachment == null || docWithBadAttachment == 0) {
							// either all or none
							if (success != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage(
										"TransferBean.TransferError.Text1", bad, success));
							}
							// All documents failed.
							else {
								log.debug("");
								bean.setMessage(MsgUtils
										.formatMessage("TransferBean.TransferError.Text2", bad));
							}
						}
						else if (docWithBadAttachment != null && docWithBadAttachment != 0) {
							//All
							if (success != 0 && bad != 0) {
								log.debug("");
								bean.setMessage(
										MsgUtils.formatMessage("TransferBean.TransferError.Text3",
												bad, attachmentString, success));
							}
							else if (success != 0 && bad == 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage(
										"TransferBean.TransferError.Text4", attachmentString,
										(selectedContactDocuments.size() - bad)));
							}
							else if (success == 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage(
										"TransferBean.TransferError.Text5", bad, attachmentString));
							}
							else {
								// Unable to transfer all the document.
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage(
										"TransferBean.TransferError.Text6", attachmentString));
							}
						}
					}
				}

				clearSelections();
				setEmploymentList(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method called on the click event of "Send" button on "Transfer Documents"
	 *  popup. Transfers only an individual's selected documents to that person.
	 * @return
	 */
	private String actionTransferToIndividualOk() {
		try {
			if (selectedContactDocuments != null && (! selectedContactDocuments.isEmpty())) {
				log.debug("");
				Project project = SessionUtils.getCurrentProject();
				String projectId = "";
				DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
				String timestamp = df.format(new Date());
				Integer bad = 0;
//				int docCount = 0;
				// PDF Grouping will always be Employee. Only send documents to specified employee.
				PdfGroupingType type = PdfGroupingType.EMPLOYEE;
				Map<Object, List<ContactDocument>> pdfGroupingMap = new HashMap<>();
				String realReportPath = SessionUtils.getRealReportPath();
				Map<String, Integer> mapOfErorrCounts = new HashMap<>();
				Integer docWithBadAttachment = 0;
				Integer success = selectedContactDocuments.size();
				// List of all of the file names to send to the payroll service
				List<String> allFileNames = new ArrayList<>();

				// create map - PDF Grouping will always be Employee. Only send documents to specified employee.
				pdfGroupingMap = DocumentService.groupContactDocuments(PdfGroupingType.EMPLOYEE, selectedContactDocuments);
				// Iterate over map and print documents
				for (Map.Entry<Object, List<ContactDocument>> entry : pdfGroupingMap.entrySet()) {
					log.debug("key=" + entry.getKey() + ", value=" + entry.getValue());
					Object key = entry.getKey();
					Employment emp = (Employment) key;;

					// Print reports for the ContactDocuments/Attachments
					List<String> groupFileList = new ArrayList<>();
					List<String> userNames = new ArrayList<>();
					mapOfErorrCounts = DocumentService.printGroupDocuments(projectId, timestamp, mapOfErorrCounts, realReportPath,
							entry.getValue(), groupFileList, production, userNames);
					if (groupFileList != null && (! groupFileList.isEmpty())) {
						log.debug("");
						// Merge all the files in 'fileList' - that's one group of files.
						// For outputfilename add a unique string to the file name.
						// For ER group unique string will be employee name + emp.id;
						String uniqueGroupId = "";
						emp = EmploymentDAO.getInstance().refresh(emp);
						uniqueGroupId = emp.getContact().getUser().getFirstNameLastName() + "-" + emp.getId();

						if (type != PdfGroupingType.NONE) {
							log.debug("uniqueGroupId = " + uniqueGroupId);
							String outputFileName = production.getProdId() + projectId + "-"
									+ StringUtils.cleanFilename(production.getTitle() + "-"
											+ uniqueGroupId + "-" + timestamp) + ".pdf";
							log.debug("outputFileName = " + outputFileName);
							// Collect all of the file names to send to the payroll service.
							allFileNames.add(realReportPath + outputFileName);
							int finalPdf = PdfUtils.combinePdfs(groupFileList, (realReportPath + outputFileName));
							log.debug("finalPdf = " + finalPdf);
							if (finalPdf != 0) {
								MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError", FacesMessage.SEVERITY_WARN);
							}
							else {
								// Send to email address that the individual as entered in the send documents to field.
								// If that field is empty then default to the user's account email address. LS-1640
								String emailAddress = emp.getContact().getUser().getSendDocumentsToEmail();

								if(emailAddress == null || emailAddress.isEmpty()) {
									emailAddress = emp.getContact().getUser().getEmailAddress();
								}
								for (ContactDocument cd : entry.getValue()) {
									cd.setSentToPerformer(true);
									ContactDocumentDAO.getInstance().merge(cd);
								}
								DoNotification.getInstance().sendIndividualEmpDocuments(emailAddress, project, realReportPath + outputFileName, "ACTRA/UDA");
//								docCount++;
							}
							DocumentService.deleteIntermediateFiles(groupFileList);
						}
					}
				}

				log.debug("");
				String attachmentString = "";
				if (mapOfErorrCounts != null && (! mapOfErorrCounts.isEmpty())) {
					for (String key : mapOfErorrCounts.keySet()) {
						String docName = key.substring(((key.indexOf("_")) + 1), (key.lastIndexOf("_")));
						String totalAttachments = key.substring((key.lastIndexOf("_")) + 1);
						if (mapOfErorrCounts.get(key) != null && mapOfErorrCounts.get(key) != 0) {
							docWithBadAttachment++;
							success--;
							attachmentString = attachmentString +
									"Document " + "<b>" + docName + "</b>, " + " with " +
									(Integer.parseInt(totalAttachments) - mapOfErorrCounts.get(key)) +
									" out of " + totalAttachments + " attachments was transferred successfully."
									+ "</br>";
						}
						else if (mapOfErorrCounts.get(key) == null) {
							bad++;
							success--;
						}
					}
				}

				if (production != null && pdfGroupingMap != null && pdfGroupingMap.size() > 0) {
					transmitData(project);
					if (bad == 0 && (docWithBadAttachment == null || docWithBadAttachment == 0)) {
						MsgUtils.addFacesMessage("TransferBean.TransferSuccessful.CanadaPerformer", FacesMessage.SEVERITY_INFO,
								selectedContactDocuments.size());
					}
					else {
						PopupBean bean = PopupBean.getInstance();
						bean.show(this, 0, null, null, "Confirm.Close", null);
						if (docWithBadAttachment == null || docWithBadAttachment == 0) {
							// either all or none
							if (success != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text1", bad,
										success));
							}
							// All documents failed.
							else {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text2", bad));
							}
						}
						else if (docWithBadAttachment != null && docWithBadAttachment != 0) {
							//All
							if (success != 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text3", bad,
										attachmentString, success));
							}
							else if (success != 0 && bad == 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text4",
										attachmentString, (selectedContactDocuments.size() - bad)));
							}
							else if (success == 0 && bad != 0) {
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text5", bad,
										attachmentString));
							}
							else {
								// Unable to transfer all the document.
								log.debug("");
								bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text6",
										attachmentString));
							}
						}
					}
				}

				clearSelections();
				setEmploymentList(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method prints each ContactDocumment separately with their attachments.
	 * @param realReportPath
	 * @param sendFileList
	 * @param groupFileList
	 * @return
	 */
	private String printDocsWithoutGrouping(String realReportPath, List<String> sendFileList,
			List<String> groupFileList) {
		String sendFileNameText;
		// If there is no grouping type.
		String priorFile = null;
		List<String> filesWithAttachments = new ArrayList<>();
		for (String fileName : groupFileList) {
			if (fileName.contains(Constants.ATTACHMENT) && priorFile != null) {
				List<String> files = new ArrayList<>();
				files.add(priorFile);
				files.add(fileName);
				log.debug("Prior FileName = " + priorFile);
				String fileNameNoPdf = priorFile.replace(realReportPath, "");
				fileNameNoPdf = fileNameNoPdf.replace(".pdf", "");
				String outputFileName = fileNameNoPdf + "-WithAttachments" + ".pdf";
				log.debug("outputFileName = " + outputFileName);
				int finalPdf = PdfUtils.combinePdfs(files, (realReportPath + outputFileName));
				log.debug("finalPdf = " + finalPdf);
				if (finalPdf != 0) {
					MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError", FacesMessage.SEVERITY_WARN);
				}
				DocumentService.deleteIntermediateFiles(files);
				// And THIS outputfilename needs to go into sendFileList to be used below in a notification...
				sendFileList.add(realReportPath + outputFileName);
				if (priorFile != null) {
					filesWithAttachments.add(priorFile);
				}
			}
			else {
				sendFileList.add(fileName);
			}
			priorFile = fileName;
		}
		sendFileNameText = "";
		Iterator<String> itr = sendFileList.iterator();
		while (itr.hasNext()) {
			String fileName = itr.next();
			// list of merged filenames to go into email body
			if (filesWithAttachments.contains(fileName)) {
				itr.remove();
			}
			else {
				String name = fileName.replace(realReportPath, "");
				log.debug("name = " + name);
				sendFileNameText += name + "<br/>";
			}
		}
		return sendFileNameText;
	}

	/** Method used to export the selected custom document's XFDF
	 * into a single file.
	 * @return
	 */
	private String actionExportOk() {
		String fileLocation = null;
		try {
			log.debug(" Export Ok ");
			DateFormat df = new SimpleDateFormat("yyMMdd_HHmmss");

			// Create file name
			String timestamp = df.format(new Date());
			String reportFileName = "customDocsExport"+ "_" + timestamp;

			reportFileName += ".xfdf";
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			reportFileName = SessionUtils.getRealReportPath() + reportFileName;
			log.debug(reportFileName);

			// Get stream on file, and Exporter on stream...
			log.debug("");
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			FlatExporter exp = new FlatExporter(outputStream);
			log.debug("");
			for (ContactDocument cd : selectedContactDocuments) {
				if (cd.getDocument() != null && (! cd.getDocument().getStandard())) {
					log.debug("");
					XfdfContent xfdfContent = XfdfContentDAO.getInstance().findByContactDocId(cd.getId());
					if (xfdfContent != null) {
						log.debug("xfdfContent = " + xfdfContent.getContactDocId());
						String xfdfString = xfdfContent.getContent();
						xfdfString = DocumentService.truncateXfdf(xfdfString);
						if (xfdfString != null) {
							log.debug("");
							exp.append(xfdfString);
							exp.next();
						}
					}
				}
			}
			outputStream.flush();
			outputStream.close();

			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + fileLocation
					+ "','LS_CustomDocuments');";
			addJavascript(javascriptCode);
			clearSelections();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}
		return null;
	}

	/** Performs transmission of selected documents
	 * to the payroll service.
	 * @param project
	 * @return true if transmission is successful else returns false.
	 */
	private boolean transmitData(Project project) {
		log.debug("");
		boolean ret = true;
		Date date = new Date();
		List<ContactDocument> transferDocs = new ArrayList<>();
		for (ContactDocument cd : selectedContactDocuments) {
			cd = getContactDocumentDAO().refresh(cd);
			cd.setTimeSent(date);
			cd.setChecked(false);
			cd.setLastSent(date);
			if (cd.getFormType() == PayrollFormType.MODEL_RELEASE) {
				cd.setStatus(ApprovalStatus.LOCKED);
			}
			ContactDocumentDAO.getInstance().attachDirty(cd);
			transferDocs.add(cd);
		}
		PayrollService service = production.getPayrollPref().getPayrollService();
		if (service.getSendBatchMethod() == ServiceMethod.TEAM_FTP) {
			Collection<Collection<ContactDocument>> docs = new ArrayList<>();
			docs.add(transferDocs);
			DocumentTransferService transferService = DocumentTransferService.getInstance();
			ret = transferService.transmit(docs, project, ExportType.FULL_TAB, ReportStyle.TC_AICP_TEAM, false, false);
		}
		return ret;
	}

	/**
	 * Calculate the number of documents included in the given list of
	 * EmploymentDocuments wrappers.
	 *
	 * @param filteredEmps The list to be scanned.
	 * @return The total number of documents included in the list.
	 */
	private int calculateDocumentCount(List<EmploymentDocuments> filteredEmps) {
		int docCount = 0;
		if (filteredEmps != null) {
			for (EmploymentDocuments empRecord : filteredEmps) {
				for (PayrollFormType pf : PayrollFormType.values()) {
					if (empRecord.getMapOfDocumentItems().get(pf.name()) != null) {
						docCount = docCount + empRecord.getMapOfDocumentItems().get(pf.name()).getDocCount();
					}
				}
			}
		}
		return docCount;
	}

	/** Method used to clear the selections. */
	private void clearSelections() {
		checkedForAll = false;
		for (EmploymentDocuments empRow : employmentList) {
			empRow.setSelected(false);
			for (PayrollFormType formType : PayrollFormType.values()) {
				empRow.selectCheckBoxByType(formType, TriStateCheckboxExt.CHECK_OFF);
			}
		}
		for (ContactDocumentInfo cdRow : selectedDocumentIds) {
			cdRow.setSelected(false);
		}
		checkBoxSelectedItems.clear();
		selectedDocumentIds.clear();
		selectedContactDocuments.clear();
	}

	/**
	 * Create a list of billers. Documents will be transferred to the selected
	 * biller. Currently only being used for Canada
	 */
	private void createBillersDL() {
		billerMap = new HashMap<>();
		billersListDL = new ArrayList<>();
		billersListDL.add(Constants.EMPTY_SELECT_ITEM);
		List<SelectionItem> billerList =
				SelectionItemDAO.getInstance().findBillers(SelectionItem.BILLER_TYPE_CA);
		selectedBiller = new SelectionItem();

		for (SelectionItem biller : billerList) {
			billersListDL.add(new SelectItem(biller.getId(), biller.getName()));
			billerMap.put(biller.getId(), biller);
		}
	}

	/**
	 * Called due to a hidden field value in our JSP page. Note that this must be
	 * public as it is called from JSP. Used to know when the page is actually
	 * being rendered, as opposed to some of our getXxxxxx methods being called
	 * by the framework for non-render purposes.
	 */
	public boolean getSetUp() {
		rendered = true; // some getter's may check this
		return false;
	}

	/** See {@link #statusList}. */
	public List<SelectItem> getStatusList() {
		return statusList;
	}

	/** See {@link #transferStatusList}. */
	public List<SelectItem> getTransferStatusList() {
		return transferStatusList;
	}

	/** See {@link #employmentList}. */
	public List<EmploymentDocuments> getEmploymentList() {
		if (rendered && employmentList == null) {
			// avoid doing this until the page is truly being rendered
			createEmploymentList(getSelectedTransferStatus(), getSelectedStatus(), getShowAllProjects());
		}
		return employmentList;
	}
	/** See {@link #employmentList}. */
	public void setEmploymentList(List<EmploymentDocuments> employmentList) {
		this.employmentList = employmentList;
	}

	/** See {@link #formTypeList}. */
	public List<String> getFormTypeList() {
		return formTypeList;
	}
	/** See {@link #formTypeList}. */
	public void setFormTypeList(List<String> formTypeList) {
		this.formTypeList = formTypeList;
	}

	/** See {@link #billersListDL}. */
	public List<SelectItem> getBillersListDL() {
		if (billersListDL == null) {
			createBillersDL();
		}
		return billersListDL;
	}

	/** See {@link #billersListDL}. */
	public void setBillersListDL(List<SelectItem> billersListDL) {
		this.billersListDL = billersListDL;
	}


	/** See {@link #selectedStatus}. */
	public ApprovalStatus getSelectedStatus() {
		return selectedStatus;
	}
	/** See {@link #selectedStatus}. */
	public void setSelectedStatus(ApprovalStatus selectedStatus) {
		this.selectedStatus = selectedStatus;
	}

	/** See {@link #selectedTransferStatus}. */
	public String getSelectedTransferStatus() {
		return selectedTransferStatus;
	}
	/** See {@link #selectedTransferStatus}. */
	public void setSelectedTransferStatus(String selectedTransferStatus) {
		this.selectedTransferStatus = selectedTransferStatus;
	}

	/** See {@link #checkedForAll}. */
	public Boolean getCheckedForAll() {
		return checkedForAll;
	}
	/** See {@link #checkedForAll}. */
	public void setCheckedForAll(Boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/** See {@link #allowSent}. */
	public Boolean getAllowSent() {
		return allowSent;
	}
	/** See {@link #allowSent}. */
	public void setAllowSent(Boolean allowSent) {
		this.allowSent = allowSent;
	}

	/**See {@link #showAllProjects}. */
	public Boolean getShowAllProjects() {
		return showAllProjects;
	}
	/**See {@link #showAllProjects}. */
	public void setShowAllProjects(Boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}
	/**See {@link #production}. */
	public Production getProduction() {
		return production;
	}

	public ContactDocumentDAO getContactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}

	/** See {@link #formActraContractDAO}. */
	private FormActraContractDAO getFormActraContractDAO() {
		if(formActraContractDAO == null) {
			formActraContractDAO = FormActraContractDAO.getInstance();
		}
		return formActraContractDAO;
	}

	/** See {@link #formActraWorkPermitDAO}. */
	private FormActraWorkPermitDAO getFormActraWorkPermitDAO() {
		if(formActraWorkPermitDAO == null) {
			formActraWorkPermitDAO = FormActraWorkPermitDAO.getInstance();
		}
		return formActraWorkPermitDAO;
	}

	/** See {@link #formUdaContractDao}. */
	private FormUDAContractDAO getFormUdaContractDao() {
		if(formUDAContractDAO == null) {
			formUDAContractDAO = FormUDAContractDAO.getInstance();
		}
		return formUDAContractDAO;
	}

	/** See {@link #selectedBiller}. */
	public SelectionItem getSelectedBiller() {
		return selectedBiller;
	}

	/** See {@link #selectedBiller}. */
	public void setSelectedBiller(SelectionItem selectedBiller) {
		this.selectedBiller = selectedBiller;
	}

	/**
	 * Listener for the "master" check-boxes in the left column -- applies to
	 * all the document entries for a given Employment entry (row). This needs
	 * to find the checkbox that was clicked, and pass the event to it.
	 */
	public void listenEmpMasterCheck(ValueChangeEvent event) {
		log.debug("");
		EmploymentDocuments empRow = null;
		// Retrieve the employment id attribute from the tri-state checkbox for fetching
		// documents relating to this id. LS-2492
		Integer employmentId = (Integer)((UIInput)event.getSource()).getAttributes().get("employmentId");

		if(!employmentList.isEmpty()) {
			for(EmploymentDocuments docs : employmentList) {
				if(docs.getEmploymentId() == employmentId) {
					empRow = docs;
					break;
				}
			}
		}
		if (empRow != null) {
			// Pass the check event to the checkbox instance.
			empRow.getEmpMasterCheck().listenChecked(event);
		}
	}

	/**
	 * Listener for the "master" check-box in a cell with multiple documents-- applies to
	 * all the document entries for a given form type(column). This needs
	 * to find the checkbox that was clicked, and pass the event to it.
	 */
	public void listenFormTypeCheck(ValueChangeEvent event) {
		log.debug("");
		TransferDocItem forms = null;
		EmploymentDocuments empRow = null;
		// Retrieve the employment id attribute from the tri-state checkbox for fetching
		// documents relating to this id. LS-2492
		Integer employmentId = (Integer)((UIInput)event.getSource()).getAttributes().get("employmentId");

		String source = SessionUtils.getRequestParam("javax.faces.source");
		// example param: key=javax.faces.source, value=ob:tableTransferDoc:2:DEPOSIT_form
		if (source != null && source.contains("form")) {
			String form = null;
			String formType = null;
			Matcher m = DATATABLE_ITEM_SOURCE_ID_PATTERN.matcher(source);
			if (m.matches()) {
				log.debug("Form = " + m.group(2));
				try {
					form = m.group(2);
					formType = form.substring(0, form.lastIndexOf("_"));
					log.debug("formType = " + formType);
				}
				catch (Exception e) {
					EventUtils.logError(e);
					MsgUtils.addGenericErrorMessage();

				}
			}
			if(!employmentList.isEmpty()) {
				for(EmploymentDocuments docs : employmentList) {
					if(docs.getEmploymentId() == employmentId) {
						empRow = docs;
						break;
					}
				}
				if(empRow != null) {
					forms = empRow.getMapOfDocumentItems().get(formType);
				}
			}
		}
		log.debug("clicked form type form checkbox in an emp row=" + forms + ", source=" + source);
		if (forms != null) {
			// Pass the check event to the checkbox instance.
			forms.getCheckBox().listenChecked(event);
		}
	}

	/**
	 * Listener for any filter event on the document list. When the filter(s)
	 * change, we need to recompute the employee and document counts displayed.
	 *
	 * @param evt Event supplied by the framework.
	 */
	public void listenFilter(TableFilterEvent evt) {
		log.debug(evt);
		if (evt != null && evt.getSource() instanceof DataTable) {
			DataTable source = (DataTable)evt.getSource();
			if (source != null) {
				log.debug(source.getRowCount());
				filteredCount = source.getRowCount();
				@SuppressWarnings("unchecked")
				List<EmploymentDocuments> filteredEmps = source.getFilteredData();
				if (filteredEmps == null) {
					documentCount = calculateDocumentCount(employmentList);
				}
				else {
					documentCount = calculateDocumentCount(filteredEmps);
				}
			}
		}
	}

	/**
	 * Listen for a change to the selected biller (for Canadian productions).
	 *
	 * @param event Event supplied by the framework.
	 */
	public void listenBillerChange(ValueChangeEvent event) {
		Integer billerId = (Integer)event.getNewValue();
		if (billerId != null) {
			selectedBiller = billerMap.get(billerId);
		}
		else {
			selectedBiller = new SelectionItem();
		}
	}

	/**
	 * @see com.lightspeedeps.object.ControlHolder#clicked(TriStateCheckboxExt, java.lang.Object)
	 */
	@Override
	public void clicked(TriStateCheckboxExt checkBox, Object id) {
		log.debug(""+ id);
		if (id != null) {
			log.debug("Id = " + id.getClass());
			if (id instanceof EmploymentDocuments) {
				log.debug(" ");
				EmploymentDocuments empRow = (EmploymentDocuments)id;
				Map<String, TransferDocItem> docMap = empRow.getMapOfDocumentItems();
				if (empRow.getEmpMasterCheck().isChecked() || empRow.getEmpMasterCheck().isPartiallyChecked()) {
					empRow.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
					log.debug(" ");
					if (! checkBoxSelectedItems.contains(empRow)) {
						checkBoxSelectedItems.add(empRow);
					}
					if (docMap != null && (! docMap.isEmpty())) {
						// make sure all eligible docs in the row are selected...
						for (String type : docMap.keySet()) {
							boolean found = false;
							for (ContactDocumentInfo cdInfo : docMap.get(type).getDocInfoList()) {
								if (! cdInfo.getDisabled()) {
									log.debug("CD = " + cdInfo.getContactDocumentId());
									found = true;
									cdInfo.setSelected(true);
									if (! selectedDocumentIds.contains(cdInfo)) {
										selectedDocumentIds.add(cdInfo);
									}
								}
							}
							log.debug("Type = " + type);
							log.debug("Selected docs = " + selectedDocumentIds.size());
							if (found) {
								log.debug(" CHECKED ");
								empRow.selectCheckBoxByType(PayrollFormType.stringToValue(type), TriStateCheckboxExt.CHECK_ON);
							}
							//empRow.selectCheckBoxByType(PayrollFormType.stringToValue(type), TriStateCheckboxExt.CHECK_ON);
						}
					}
				}
				else if (empRow.getEmpMasterCheck().isUnchecked() && checkBoxSelectedItems.contains(empRow)) {
					log.debug(" ");
					checkBoxSelectedItems.remove(empRow);
					setCheckedForAll(false);
					if (docMap != null && (! docMap.isEmpty())) {
						for (String type : docMap.keySet()) {
							for (ContactDocumentInfo cdInfo : docMap.get(type).getDocInfoList()) {
								if (! cdInfo.getDisabled()) {
									log.debug("CD = " + cdInfo.getContactDocumentId());
									cdInfo.setSelected(false);
									selectedDocumentIds.remove(cdInfo);
								}
							}
							log.debug("Type = " + type);
							log.debug("Selected docs = " + selectedDocumentIds.size());
							empRow.selectCheckBoxByType(PayrollFormType.stringToValue(type), TriStateCheckboxExt.CHECK_OFF);
						}
					}
				}
				else if (empRow.getEmpMasterCheck().isPartiallyChecked()) {
					log.debug(" ");
				}
				log.debug("Emp id = " + empRow.getEmploymentId());
				log.debug("Emp name = " + empRow.getEmpName());
			}
			else if (id instanceof TransferDocItem) {
				TransferDocItem item = (TransferDocItem)id;
				log.debug(" TransferDocItem " );
				log.debug("emp id = " + item.getEmploymentId());
				if (item.getCheckBox().isPartiallyChecked() || item.getCheckBox().isChecked()) { // skip partial-check state
					item.getCheckBox().setCheckValue(TriStateCheckboxExt.CHECK_ON);
					for (ContactDocumentInfo cd : item.getDocInfoList()) {
						log.debug(" CD Id " + cd.getContactDocumentId());
						log.debug(" CD Status " + cd.getDisabled());
						if (! cd.getDisabled()) {
							cd.setSelected(true);
							if (! selectedDocumentIds.contains(cd)) {
								selectedDocumentIds.add(cd);
							}
						}
					}
					for (EmploymentDocuments empl : getEmploymentList()) {
						if (item.getEmploymentId().equals(empl.getEmploymentId())) {
							boolean allSelected = true;
							for (String key : empl.getMapOfDocumentItems().keySet()) {
								if (empl.getMapOfDocumentItems().get(key) != null &&
										empl.getMapOfDocumentItems().get(key).getDocInfoList() != null) {
									for (ContactDocumentInfo cd : empl.getMapOfDocumentItems().get(key).getDocInfoList()) {
										if ((! cd.getDisabled()) && (! cd.getSelected())) {
											allSelected = false;
										}
									}
								}
							}
							if (allSelected) {
								empl.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_ON);
							}
							else {
								empl.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
							}
							break; // item will only match one entry
						}
					}
				}
				else if (item.getCheckBox().isUnchecked()) { // skip partial-check state
					for (ContactDocumentInfo cd : item.getDocInfoList()) {
						log.debug(" CD Id " + cd.getContactDocumentId());
						log.debug(" CD Status " + cd.getDisabled());
						cd.setSelected(false);
						selectedDocumentIds.remove(cd);
					}
					for (EmploymentDocuments emp : getEmploymentList()) {
						if (item.getEmploymentId().equals(emp.getEmploymentId())) {
							Boolean partiallySelected = false;
							outer:
							for (String key : emp.getMapOfDocumentItems().keySet()) {
								if (emp.getMapOfDocumentItems().get(key) != null &&
										emp.getMapOfDocumentItems().get(key).getDocInfoList() != null) {
									for (ContactDocumentInfo cd : emp.getMapOfDocumentItems().get(key).getDocInfoList()) {
										if ((! cd.getDisabled()) && cd.getSelected()) {
											partiallySelected = true;
											break outer;
										}
									}
								}
							}
							if (partiallySelected) {
								emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_MIXED);
							}
							else {
								emp.getEmpMasterCheck().setCheckValue(TriStateCheckboxExt.CHECK_OFF);
							}
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * @return the "is a Team payroll service" flag based on the current
	 *         production's Payroll Service. LS-2236
	 */
	private boolean isTeamPayroll() {
		if (isTeamPayroll == null) {
			isTeamPayroll = false;
			PayrollService payrollService = production.getPayrollPref().getPayrollService();
			if (payrollService != null) {
				isTeamPayroll = payrollService.getTeamPayroll();
			}
		}
		return isTeamPayroll;
	}

	/**
	 * @param typeName
	 * @return Transfer column heading, given PayrollFormType. Used in JSP, as
	 *         enum value isn't recognized as such when passed via ui param.
	 */
	public String getTypeColHeading(PayrollFormType typeName) {
		return typeName.getTransferColHeading();
	}

	/**
	 * @param typeName
	 * @return Document name, given PayrollFormType. Used in JSP, as
	 *         enum value isn't recognized as such when passed via ui param.
	 */
	public String getTypeLabel(PayrollFormType typeName) {
		return typeName.getName();
	}

}
