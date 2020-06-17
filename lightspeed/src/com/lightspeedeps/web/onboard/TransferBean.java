package com.lightspeedeps.web.onboard;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.model.table.LazyDataModel;
import org.icefaces.ace.model.table.SortCriteria;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.service.DocumentTransferService;
import com.lightspeedeps.service.FormService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.view.View;

/**
 * This class is the backing bean for the "old" Document Transfer page, which is
 * used to select documents and transmit them to the client's payroll service.
 * This page presents one row per document, and supports transferring custom
 * documents.  It is currently only used for non-Team clients.
 * See documentTransfer.xhtml.
 */
@ManagedBean
@ViewScoped
public class TransferBean extends View implements Serializable {

	private static final long serialVersionUID = -5353141404098134420L;

	private static final Log log = LogFactory.getLog(TransferBean.class);

	/** The document status drop-down list */
	private List<SelectItem> documentStatus = null;

	/** The sent status drop-down list */
	private List<SelectItem> sentStatus = null;

	private LazyDataModel<ContactDocument> lazyContactDocList;

	/** Empty lazy data model, referenced by xhtml when page is not actually rendered. */
	@SuppressWarnings("serial")
	private LazyDataModel<ContactDocument> emptyContactDocList = new LazyDataModel<ContactDocument>() {
		@Override
		public List<ContactDocument> load(int first, int pageSize, final SortCriteria[] criteria, Map<String, String> filters) {
			log.debug("empty (dummy) data model invoked.");
			return new ArrayList<>();
		}
	};

	/** Lazy loaded contact document list of the current production */
	private List<ContactDocument> contactDocumentList = null;

	/** The list of checked Contact Documents from the data table */
	private final List<ContactDocument> checkBoxSelectedItems = new ArrayList<>();

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAll = false;

	/** Map of Project and a map for document chains for current contact in that project. Inner map holds the
	 * document chains and list of departments for which the current contact is an approver.
	 * List of department will be null if contact is Production approver for the document chain.*/
	private Map<Project, Map<DocumentChain, List<Department>>> currentContactChainMap = new HashMap<>();

	private static final int ACT_TRANSFER = 21;

	private static final int ACT_EXPORT = 22;

	/** Unique Number of Contacts of Contact Documents to be transferred.*/
	private Integer contactCount = 0;

	private Integer lazyContactDocListSize = null;

	private String filterValue = "2";

	/** True if the user has chosen to view the contact documents list for all project in a commercial production */
	private Boolean showAllProjects = false;

	private final Production production;

	private final Contact currentContact;

	/** Map of Project and list of document chains for which current contact is an editor in that project. */
	private Map<Project, List<DocumentChain>> currContactChainEditorMap = new HashMap<>();

	public TransferBean() {
		super("TransferBean.");
		production = SessionUtils.getProduction();
		currentContact = SessionUtils.getCurrentContact();
		if (production != null) {
			if (! production.getType().isAicp()) {
				showAllProjects = null;
			}
			currentContactChainMap = StatusListBean.getInstance().getCurrentContactChainMap();
			currContactChainEditorMap = StatusListBean.getInstance().getCurrContactChainEditorMap();
			inItLazyList();
			//		getLazyContactDocListSize();
			//		if (getRequestBean().getStatusColumn() != null) {
			//			getRequestBean().getStatusColumn().setFilterValue("1");
			//		}
		}

	}

	/** Used to return the instance of the TransferBean */
	public static TransferBean getInstance() {
		return (TransferBean) ServiceFinder.findBean("transferBean");
	}

	/**
	 * Method used to form the lazy loaded ace datatable on the approvals tab
	 */
	@SuppressWarnings("serial")
	public void inItLazyList() {
		lazyContactDocList = new LazyDataModel<ContactDocument>() {
			@Override
			public List<ContactDocument> load(int first, int pageSize, final SortCriteria[] criteria, Map<String, String> filters) {
				String departmentName = filters.get("employment.role.department.name");
				String contactName = filters.get("contact.user.lastNameFirstName");
				String occupation = filters.get("employment.role.name");
				String documentChainName = filters.get("documentChain.normalizedName");
				String status = filters.get("viewStatus");
				String sentStat = filters.get("timeSent");

				log.debug("Transfer: lazy loader, page size=" + pageSize + ", first=" + first);
				// Boolean false for descending order and true for ascending order.
				Map<String, Boolean> sortMap = new HashMap<>();
				/*aceColumn.setFilterValue(filterValue);
				sentStatus = filterValue;
				log.debug(" sentStatus " + sentStatus);*/

				// construct sortMap based on dataTable's sort criteria
				if (criteria != null) {
					for (SortCriteria sc : criteria) {
						if (sc.getPropertyName().equals("employment.role.department.name")) {
							log.debug("");
							sortMap.put("department", sc.isAscending());
						}
						else if (sc.getPropertyName().equals("contact.user.lastNameFirstName")) {
							log.debug("");
							sortMap.put("name", sc.isAscending());
						}
						else if (sc.getPropertyName().equals("employment.role.name")) {
							log.debug("");
							sortMap.put("occupation", sc.isAscending());

						}
						else if (sc.getPropertyName().equals("documentChain.normalizedName")) {
							log.debug("");
							sortMap.put("document", sc.isAscending());
						}
						else if (sc.getPropertyName().equals("viewStatus")) {
							log.debug("");
							sortMap.put(ContactDocument.SORTKEY_STATUS, sc.isAscending());
						}
					}
				}
				if (! production.getType().isAicp()) {
					showAllProjects = null;
				}
				long count = ContactDocumentDAO.getInstance().findCountByFilter(status,departmentName, contactName, occupation, documentChainName, false, sentStat, showAllProjects);
				setRowCount((int)count);
				//lazyContactDocListSize = (int) count;
				lazyContactDocListSize = null;
				Set<Integer> checked = new HashSet<>();
				if (contactDocumentList != null) {
					// Save id's of all "checked" items
					for (ContactDocument cd : contactDocumentList) {
						if (cd.getChecked()) {
							checked.add(cd.getId());
						}
					}
				}
				contactDocumentList = new ArrayList<>(ContactDocumentDAO.getInstance().findByFilter(status, departmentName, contactName, occupation,
						pageSize, first, documentChainName, false, sentStat, showAllProjects, sortMap));
				for (ContactDocument contactDoc : contactDocumentList) {
					contactDoc.setViewStatus(ApproverUtils.findRelativeStatus(contactDoc, getvContact()));
					contactDoc.setWaitingFor(ApproverUtils.calculateWaitingFor(contactDoc));
				}

				//Contact curContact = SessionUtils.getCurrentContact();
				AuthorizationBean bean = AuthorizationBean.getInstance();
				boolean viewAll = bean.hasPermission(Permission.VIEW_ALL_DISTRIBUTED_FORMS);

				// Restore "checked" status of matching items
				for (ContactDocument cd : contactDocumentList) {
					if (checked.contains(cd.getId())) {
						cd.setChecked(true);
					}
					cd.setViewStatus(ApproverUtils.findRelativeStatus(cd, getvContact()));
					cd.setWaitingFor(ApproverUtils.calculateWaitingFor(cd));
					if (viewAll || cd.getContact().equals(currentContact) ||
							ApproverUtils.isContactInChainMap(currentContactChainMap, cd, currentContact, cd.getProject()) ||
							ApproverUtils.isEditorInChainMap(currContactChainEditorMap, cd, currentContact, cd.getProject())) {
						cd.setDisableJump(false);
					}
					else {
						cd.setDisableJump(true);
					}
				}
				Iterator<ContactDocument> itr = checkBoxSelectedItems.iterator();
				while (itr.hasNext()) {
					ContactDocument cd = itr.next();
					if (! contactDocumentList.contains(cd)) {
						itr.remove();
					}
				}
					/*if (first >= contactDocumentList.size()) {
						return Collections.emptyList();
					}*/
//				getLazyContactDocListSize();
				return contactDocumentList;
			}
		};
	}

	/** Value change listener for master-checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenCheckedForAll(ValueChangeEvent evt) {
		try {
			if (getCheckedForAll() && contactDocumentList != null) {
				for (ContactDocument contactDocument : contactDocumentList) {
					// Only select Approved or Locked documents r3.1.7915
					if (contactDocument.getStatus().isFinal()) {
						contactDocument.setChecked(true);
						if (! checkBoxSelectedItems.contains(contactDocument)) {
							checkBoxSelectedItems.add(contactDocument);
						}
					}
				}
			}
			else {
				checkBoxSelectedItems.clear();
				checkedForAll = false;
				for (ContactDocument contactDocument : contactDocumentList) {
					contactDocument.setChecked(false);
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
			ContactDocument contactDocument = (ContactDocument) evt.getComponent().getAttributes().get("selectedRow");
			if (contactDocument.getChecked()) {
				checkBoxSelectedItems.add(contactDocument);
			}
			else if (checkBoxSelectedItems.contains(contactDocument)) {
				checkBoxSelectedItems.remove(contactDocument);
				setCheckedForAll(false);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @return the number of items in our item list.
	 */
	public int getLazyContactDocListSize() {
		if (lazyContactDocList != null) {
			lazyContactDocListSize = lazyContactDocList.getRowCount();
		}
		else {
			lazyContactDocListSize = 0;
		}
		return lazyContactDocListSize;
	}

	/**
	 * @return the number of items displayed per page.
	 */
	public int getLazyContactDocNumDisplayed() {
		if (lazyContactDocList != null) {
			return lazyContactDocList.getPageSize();
		}
		return 0;
	}

	/**
	 * @return the number of documents selected from the list.
	 */
	public int getSelectedDocumentCount() {
		if (checkBoxSelectedItems != null) {
			return checkBoxSelectedItems.size();
		}
		return 0;
	}

	public String actionTransfer() {
		try {
			contactCount = 0;
			PayrollPreference payrollPref = null;
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
			if (payrollPref != null &&
					(! payrollPref.isUseOnboardEmail() || StringUtils.isEmpty(payrollPref.getOnboardEmailAddress()))) {
				MsgUtils.addFacesMessage("TransferBean.SetEmailPreference", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			Set<Integer> contactIdList = new HashSet<>();
			if (checkBoxSelectedItems != null && checkBoxSelectedItems.size() > 0) {
				int transferCount = 0;
				for (ContactDocument cd : checkBoxSelectedItems) {
					if (cd.getStatus() != ApprovalStatus.VOID || (cd.getStatus() == ApprovalStatus.VOID && cd.getLastSent() != null)) {
						transferCount++;
						contactIdList.add(cd.getContact().getId());
					}
				}
				if (transferCount > 0) {
					contactCount = contactIdList.size();
					PopupBean bean = PopupBean.getInstance();
					bean.show(this, ACT_TRANSFER, "TransferBean.Transfer.");
					bean.setMessage(MsgUtils.formatMessage("TransferBean.Transfer.Text", transferCount, contactCount));
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

	/**
	 * @return
	 */
	public String actionExport() {
		try {
			log.debug(" Export ");
			int count = 0;
			for (ContactDocument cd : checkBoxSelectedItems) {
				if (cd.getDocument() != null && (! cd.getDocument().getStandard())) {
					count++;
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
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	@SuppressWarnings("rawtypes")
	private String actionTransferOk() {
		try {
			Project project = null;
			String projectId = "";
			PayrollPreference prefer = production.getPayrollPref();
			if (production.getType().isAicp()) {
				project = SessionUtils.getCurrentProject();
				projectId = "-" + project.getEpisode(); // commercial "project number"
				prefer = project.getPayrollPref();
			}
			String fileNameText = "";
			DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
			String timestamp = df.format(new Date());
			String docName = null;
			List<String> fileList = new ArrayList<>();
			boolean ret = false;
			Integer bad = 0;
			String realReportPath = SessionUtils.getRealReportPath();

			Iterator<ContactDocument> itr = checkBoxSelectedItems.iterator();
				while (itr.hasNext()) {
					ContactDocument cd =  itr.next();
					List<String> attachmentFileList = new ArrayList<>();
					if (cd.getStatus() != ApprovalStatus.VOID || (cd.getStatus() == ApprovalStatus.VOID && cd.getLastSent() != null)) {
						User user = cd.getContact().getUser();
						user = UserDAO.getInstance().refresh(user);
						docName = cd.getFormType().getReportPrefix();
						docName = docName.substring(0, docName.length()-1);
						String fileNamePdf = production.getProdId() + "-" +
								StringUtils.cleanFilename(production.getTitle() + projectId + "-" +
								user.getLastName() + "-" + user.getFirstName()+ "-" + docName + "-" +
							timestamp + "-" + cd.getId() + ".pdf");
					Form prtForm = FormService.findRelatedOrBlankForm(cd);
					fileNamePdf = DocumentService.printDocument(cd.getFormType(), cd, prtForm , fileNamePdf, true);
					if (fileNamePdf != null) {
						ret = true;
						attachmentFileList.add(realReportPath + fileNamePdf);
						cd = ContactDocumentDAO.getInstance().refresh(cd);
						if (cd.getAttachments() != null && (! cd.getAttachments().isEmpty())) {
							String fileWithAttachments = "";
							fileWithAttachments = DocumentService.mergeAttachments(bad, cd, fileNamePdf, user);
							fileList.add(realReportPath + fileWithAttachments);
							fileNameText += fileWithAttachments + "<br/>";
							log.debug("");
						}
						/*if (cd.getAttachments() != null && (! cd.getAttachments().isEmpty())) {
							String attachmentOutputFileName = production.getProdId() + "-" +
									StringUtils.cleanFilename(production.getTitle() + projectId + "-" +
									user.getLastName() + "-" + user.getFirstName()+ "-" + docName + "-A-" +
									timestamp + "-" + cd.getId() + ".pdf");
							DocumentService.printAttachments(cd, user, fileNameText, attachmentFileList, timestamp, attachmentOutputFileName, bad);
							if (attachmentFileList != null && (! attachmentFileList.isEmpty())) {
								fileList.add(realReportPath + attachmentOutputFileName);
								fileNameText += attachmentOutputFileName + "<br/>";
								log.debug("");
							}
						}*/
						else {
							fileList.add(realReportPath + fileNamePdf);
							fileNameText += fileNamePdf + "<br/>";
						}
					}
					else {
						bad++;
						cd.setChecked(false);
						itr.remove();
					}
				}
				else {
					cd.setChecked(false);
					itr.remove();
				}
			}
			if (production != null && fileList.size() > 0) {
				ret = transmitData(project);
				if (! ret) {
					fileNameText += " ** ERROR ** Delivery of data file(s) was unsuccessful ** <p/>";
				}
				prefer = PayrollPreferenceDAO.getInstance().refresh(prefer);
				Collection<String> emailAddressList = new ArrayList<>();
				String emailAddress = prefer.getOnboardEmailAddress();
				emailAddressList.add(emailAddress);
				DoNotification.getInstance().sendDocuments(emailAddressList, fileList, project, contactCount, checkBoxSelectedItems.size(), fileNameText);
				if (bad == 0) {
					MsgUtils.addFacesMessage("TransferBean.TransferSuccessful", FacesMessage.SEVERITY_INFO, checkBoxSelectedItems.size());
				}
				else {
					PopupBean bean = PopupBean.getInstance();
					bean.show(null, 0, null, null, "Confirm.Close", null);
					bean.setMessage(MsgUtils.formatMessage("TransferBean.TransferError.Text1", bad, (checkBoxSelectedItems.size())));
				}
			}
			checkBoxSelectedItems.clear();
			contactDocumentList = null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
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
			for (ContactDocument cd : checkBoxSelectedItems) {
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
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}
		return null;
	}

	/**
	 * @param project
	 * @return
	 */
	private boolean transmitData(Project project) {
		boolean ret = true;
		Date date = new Date();
		List<ContactDocument> transferDocs = new ArrayList<>();
		for (ContactDocument cd : checkBoxSelectedItems) {
			cd = ContactDocumentDAO.getInstance().refresh(cd);
			cd.setTimeSent(date);
			cd.setChecked(false);
			cd.setLastSent(date);
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

	/**See {@link #documentStatus}. */
	public List<SelectItem> getDocumentStatus() {
		if (documentStatus == null) {
			documentStatus = new ArrayList<>();
			documentStatus.add(new SelectItem(0, "All"));
			documentStatus.add(new SelectItem(1, "Approved"));
			documentStatus.add(new SelectItem(2, "Unapproved"));
		}
		return documentStatus;
	}
	/**See {@link #documentStatus}. */
	public void setDocumentStatus(List<SelectItem> documentStatus) {
		this.documentStatus = documentStatus;
	}

	/** See {@link #lazyContactDocList}. */
	public LazyDataModel<ContactDocument> getLazyContactDocList() {
		return lazyContactDocList;
	}
	/** See {@link #lazyContactDocList}. */
	public void setLazyContactDocList(
			LazyDataModel<ContactDocument> lazyContactDocList) {
		this.lazyContactDocList = lazyContactDocList;
	}

	/** See {@link #emptyContactDocList}. */
	public LazyDataModel<ContactDocument> getEmptyContactDocList() {
		return emptyContactDocList;
	}

	/** See {@link #contactDocumentList}. */
	public List<ContactDocument> getContactDocumentList() {
		if (contactDocumentList == null) {
			//createContactDocumentList();
			lazyContactDocListSize = null;
			Map<String, String> filters = new HashMap<>();
			getLazyContactDocList().load(0, 2, null, filters);
		}
		return contactDocumentList;
	}
	/** See {@link #contactDocumentList}. */
	public void setContactDocumentList(List<ContactDocument> contactDocumentList) {
		this.contactDocumentList = contactDocumentList;
	}

	/**See {@link #checkedForAll}. */
	public Boolean getCheckedForAll() {
		return checkedForAll;
	}
	/**See {@link #checkedForAll}. */
	public void setCheckedForAll(Boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/**See {@link #documentStatus}. */
	public List<SelectItem> getSentStatus() {
		if (sentStatus == null) {
			sentStatus = new ArrayList<>();
			sentStatus.add(new SelectItem(0, "All"));
			sentStatus.add(new SelectItem(1, "Sent"));
			sentStatus.add(new SelectItem(2, "Unsent"));
		}
		return sentStatus;
	}
	/**See {@link #documentStatus}. */
	public void setSentStatus(List<SelectItem> sentStatus) {
		this.sentStatus = sentStatus;
	}

	public String getFilterValue() {
		return filterValue;
	}

	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}

	/**See {@link #showAllProjects}. */
	public Boolean getShowAllProjects() {
		return showAllProjects;
	}
	/**See {@link #showAllProjects}. */
	public void setShowAllProjects(Boolean showAllProjects) {
		this.showAllProjects = showAllProjects;
	}

//	private TransferRequestBean getRequestBean() {
//		return TransferRequestBean.getInstance();
//	}

}
