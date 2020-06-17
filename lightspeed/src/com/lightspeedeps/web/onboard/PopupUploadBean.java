package com.lightspeedeps.web.onboard;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;

import com.lightspeedeps.dao.AttachmentDAO;
import com.lightspeedeps.dao.DocumentChainDAO;
import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.model.Attachment;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.type.DocumentAction;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.EnumList;

/**
 * This class is the backing bean for the Upload popup window. On the Documents
 * & Packets mini tab under Onboarding sub-main tab, it handles uploading custom
 * documents, loading the XFDF for custom documents. In other places (such as
 * Payroll / Start Forms), it handles uploading of various types of attachments.
 */
@ManagedBean
@ViewScoped
public class PopupUploadBean extends FileLoadBean implements Serializable {

	private static final long serialVersionUID = -745970927882665063L;

	/** Logger */
	private static final Log LOG = LogFactory.getLog(PopupUploadBean.class);
	/** The current folder on the bread crumb and also the folder in which the document will be uploaded */
	private Folder uploadFolder;
	/** True , if the document is uploaded successfully */
	private boolean uploaded = false;

	/** The database id of the latest Document uploaded. */
	private Integer documentId;

	/** Backing field for radio buttons -- indicates the type of document,
	 * New revision or updated revision is selected by the user */
	private String revisionType = NEW_REVISION_TYPE;
	/** Indicates New revision being uploaded. */
	public static final String NEW_REVISION_TYPE= "n";
	/** Indicates Updated (replacement) revision being uploaded. */
	public static final String UPDATED_REVISION_TYPE= "u";

	/** The list of Select Item to show the DocumentFlowTypeEnum values
	 *  on to the select item list.
	 */
	private List<SelectItem> documentFlowTypeDL;

	/** The list of Select item to show the Document Chain list
	 *  on the select item list , if the updated revision type is uploaded
	 */
	private List<SelectItem> documentChainList;

	/** The object field used to get the selected Document Chain object from the select item list */
	private Object selectedObject;

	/** The string variable used to store the value of the Enum type DocumentFlowType */
	private String documentFlowType;

	/** The document selected for which the Master XFDF will be uploaded. */
	private Document uploadDocument;

	/** The list of Select Item to show the Approver Action values
	 *  on to the select item list.
	 */
	private List<SelectItem> appActionTypeList;

	/** The 'abbreviated name' to be used when presenting the document being uploaded. This
	 * is the backing field for the on-screen input area. */
	private String shortName;

	/** DocumentAction enum type, used to know what type of approval cycle document will undergo.  */
	private DocumentAction employeeAction = DocumentAction.SIGN;

	/** Boolean field used to know the selected document needs to under go approval process or not */
	private boolean approvalRequired = true;

	/** True for the Edit Info pop up. */
	private boolean isEditInfo = false;

	/** True for duplicate name error and false for error in employee action. */
	private boolean isNameError;

	/** The database id of the latest Attachment uploaded. */
	private Integer attachmentId;

	/** True for the Upload Attachment pop up. */
	private boolean isAttach = false;

	/** True for the Upload Master XFDF pop up. */
	private boolean isXfdf = false;

	/** True for a private attachment -- backing field for a
	 * checkbox in the JSP. */
	private boolean isPrivateAttachment = false;

	/** True only for the 'Upload Replacement' pop up, which is not
	 * yet finished. */
	private boolean isReplacement = false;

	/** Select item list for the document list drop down, for upload user document popup. */
	private List<SelectItem> documentTypeList;

	/** Return our current instance. */
	public static PopupUploadBean getInstance() {
		return (PopupUploadBean)ServiceFinder.findBean("popupUploadBean");
	}

	/** The default constructor of the Bean, used to initialize the {@link #documentFlowTypeDL}
	 *  and set the {@link #documentChainList}
	 */
	public PopupUploadBean(){
		documentFlowTypeDL = new ArrayList<>(EnumList.getDocumentflowtypelist());
		documentChainList = null;
	}

	/** The method used to open the popup window for "Upload Document"
	 * and "Upload Replacement" actions.
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix Message id prefix for title, etc.
	 * @param currentFolder The current folder on the bread crumb
	 */
	public void show(PopupHolder holder, int act, String prefix, Folder currentFolder) {
		LOG.debug("");
		// Set defaults for initial display
		initShow();
		setUploadFolder(currentFolder);
		employeeAction = DocumentAction.SIGN;
		approvalRequired = true;
		super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
	}

	/** The method used to open the popup window for "Upload Attachment" action.
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix Message id prefix for title, etc.
	 * @param contactDoc The ContactDocument to which the attachment belongs.
	 */
	@Override
	public void show(PopupHolder holder, int act, String prefix) {
		LOG.debug("");
		// Set defaults for initial display
		initShow();
		setIsAttach(true);
		super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
	}

	/** The method used to open the popup window for Load XFDF action.
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix Message id prefix for title, etc.
	 * @param selectedDocument The Document selected to upload the XFDF.
	 */
	public void show(PopupHolder holder, int act, String prefix, Document selectedDocument) {
		LOG.debug("");
		// Set defaults for initial display
		initShow();
		isXfdf = true;
		setUploadDocument(selectedDocument);
		super.show(holder, act, prefix + "Title", prefix + "Text", "Confirm.OK", null);
	}

	/** The method used to open the pop up window for Edit Info action.
	 * @param holder The object which is "calling" us, and will get the
	 *        callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param prefix Message id prefix for title, etc.
	 * @param currentFolder The currentfolder on the bread crumb
	 * @param selectedDocument The Document selected to Edit.
	 */
	public void show(PopupHolder holder, int act, String prefix, Folder currentFolder, Document selectedDocument) {
		// Set defaults for initial display
		initShow();
		setIsEditInfo(true);
		documentId = selectedDocument.getId();
		setUploadFolder(currentFolder);
		setDisplayFilename(selectedDocument.getDisplayName());
		shortName = selectedDocument.getShortName();
		employeeAction = selectedDocument.getEmployeeAction();
		approvalRequired = selectedDocument.getApprovalRequired();
		super.show(holder, act, prefix + "Title", prefix + "Text", prefix + "Ok", "Confirm.Cancel");
	}

	private void initShow() {
		documentId = 0;
		attachmentId = 0;
		shortName = null;
		fileContentType = null;
		isXfdf = false;
		setServerFilename(null);
		setUploadDocument(null);
		setUploadFile(null);
		setDisplayFilename(null);
		setUploadFolder(null);
		setIsEditInfo(false);
		setIsAttach(false);
		setIsPrivateAttachment(false);
		setIsReplacement(false);
		setUploaded(false);
		setDocumentChainList(null);
		setIsNameError(true);
		setErrorMessage(null);
		revisionType = NEW_REVISION_TYPE;
	}

//	public void listenRevisionType(ValueChangeEvent event) {
//		LOG.debug("");
//	}

	/**
	 * Called from the superclass when the file upload is complete, and general
	 * error checking has been done. This method should take the file and do
	 * whatever is needed, e.g., store it in the database.
	 *
	 * @see com.lightspeedeps.web.popup.FileLoadBean#processFile(org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo)
	 */
	@Override
	protected String processFile(FileInfo fileInfo) {
		String messageId = "";
		MimeType mt;
		if (displayFilename.length() > 0) {
			File file = new File(getServerFilename());
			if (fileContentType != null) { // NOPMD by DHarm on 8/24/18 3:46 PM
				mt = MimeType.toValue(fileContentType);
			}
			else {
				mt = MimeType.N_A;
			}
			if (uploadFolder != null && (! isReplacement)) { // NOPMD by DHarm on 8/24/18 3:46 PM
				LOG.debug("");
				if (mt.equals(MimeType.PDF)) {
					documentId = FileRepositoryUtils.storeFile(uploadFolder, displayFilename, null, file, mt, fileContentType,
							new Date(), false);
					uploaded = documentId > 0;
					SessionUtils.put(Constants.ATTR_ONBOARDING_UPLOADED_DOCUMENT_ID, documentId);
					if (uploaded) {
						setDisplayFilename(displayFilename);
					}
					setErrorMessage(null);
				}
				else {
					messageId = "FileRepository.OnlyUploadPDF";
				}
			}
			else if (uploadFolder != null && isReplacement) {
				// uploading "Replacement" document (not yet finished)
				if (getSelectedObject() == null) {
					displayError("Please select a document type.");
				}
				else if (getSelectedObject() != null && mt.equals(MimeType.PDF)) {
					LOG.debug("");
					displayFilename = ((PayrollFormType.valueOf((String)getSelectedObject())).getLabel() + " (Scan)");
					documentId = FileRepositoryUtils.storeFile(uploadFolder, displayFilename, null, file, mt, fileContentType,
							new Date(), false);
					uploaded = documentId > 0;
					SessionUtils.put(Constants.ATTR_ONBOARDING_UPLOADED_DOCUMENT_ID, documentId);
					if (uploaded) {
						setDisplayFilename(displayFilename);
					}
					setErrorMessage(null);
				}
				else {
					messageId = "FileRepository.OnlyUploadPDF";
				}
			}
			else if (uploadDocument != null) { // NOPMD by DHarm on 8/24/18 3:46 PM
				if (mt.equals(MimeType.TEXT)) {
					uploaded = FileRepositoryUtils.storeXfdfFile(file, uploadDocument);
					if (uploaded) {
						setDisplayFilename(displayFilename);
					}
				}
				else {
					messageId = "FileRepository.OnlyTextXFDF";
				}
			}
			else if (getIsAttach()) {
				LOG.debug("");
				if (mt.equals(MimeType.PDF) || mt.isImage()) {
					LOG.debug("Size = " + displayFilename.length());
					if (displayFilename.length() > Document.MAX_DOC_NAME_LENGTH) {
						setIsNameError(true);
						messageId = "FileRepository.NameTooLong";
					}
					else {
						attachmentId = FileRepositoryUtils.storeAttachment(displayFilename, file, mt);
						if (attachmentId == null) {
							setIsNameError(true);
							messageId = "FileRepository.DuplicateAttachment";
						}
						else {
							uploaded = attachmentId > 0;
							SessionUtils.put(Constants.ATTR_ONBOARDING_UPLOADED_ATTACHMENT_ID, attachmentId);
							if (uploaded) {
								setDisplayFilename(displayFilename);
							}
							setErrorMessage(null);
						}
					}
				}
				else {
					messageId = "FileRepository.NotAttachmentType";
				}
			}
		}
		else {
			messageId = "FileRepository.MissingFileName";
		}
		LOG.debug("returned msg id=" + messageId);
		return messageId;
	}

	/**
	 * Action method called when user hits Cancel after a file had been
	 * uploaded, but before it is saved.
	 */
	@Override
	public String actionCancel() {
		LOG.debug("");
		if (documentId != null) {
			Document  document = DocumentDAO.getInstance().findById(documentId);
			if (document != null && document.getDocChainId() != null && document.getDocChainId() == 0) {
				DocumentDAO.getInstance().deleteDocument(document.getId());
			}
		}
		if (attachmentId != null) {
			Attachment attachment = AttachmentDAO.getInstance().findById(attachmentId);
			if (attachment != null && attachment.getContactDocument() == null) {
				AttachmentDAO.getInstance().delete(attachment);
			}
		}
		isEditInfo = false;
		cancelled = true;
		return super.actionCancel();
	}

	/** The method used to create the Document Chain list for the updated revision of the document
	 * @return {@link documentChainList}
	 */
	private List<SelectItem> createDocumentChainList() {
		List<DocumentChain> list = new ArrayList<>();
		list = DocumentChainDAO.getInstance().findDocumentChainList(uploadFolder.getId());
		if (list != null) {
			documentChainList = new ArrayList<>();
			for (DocumentChain docChain : list) {
				documentChainList.add(new SelectItem(docChain.getId(), docChain.getName())); // NOPMD by DHarm on 8/24/18 3:39 PM
			}
		}
		return documentChainList;
	}

	/** The method used to create the Document drop down list for refreshing the Start Forms View
	 * @return {@link #documentList}
	 */
	private List<SelectItem> createDocumentTypeList() {
		try {
			LOG.debug("");
			documentTypeList = new ArrayList<>();
			documentTypeList.add(0, new SelectItem(null, "--"));
			for (PayrollFormType type : PayrollFormType.values()) {
				String formName = type.getName();
				if (formName == null) {
					formName = "Other";
				}
				if (type.isReplaceAllowed()) {
					documentTypeList.add(new SelectItem(type, formName)); // NOPMD by DHarm on 8/24/18 3:39 PM
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return documentTypeList;
	}

	/**
	 * Redisplay the popup with an error message. This method is useful to call
	 * from a holder's "confirmOk" method when an error is found in the input,
	 * and the program wishes to keep the pop-up displayed with an error
	 * message, to give the user an opportunity to correct the error.
	 *
	 * @param message The error message text to be displayed within the popup.
	 */
	public void displayError(String message) {
		setVisible(true);
		setErrorMessage(message);
	}

	/**See {@link #uploadFolder}. */
	public Folder getUploadFolder() {
		return uploadFolder;
	}
	/**See {@link #uploadFolder}. */
	public void setUploadFolder(Folder uploadFolder) {
		this.uploadFolder = uploadFolder;
	}

	/**See {@link #uploaded}. */
	public boolean isUploaded() {
		return uploaded;
	}
	/**See {@link #uploaded}. */
	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	/**See {@link #revisionType}. */
	public String getRevisionType() {
		return revisionType;
	}
	/**See {@link #revisionType}. */
	public void setRevisionType(String revisionType) {
		this.revisionType = revisionType;
	}

	/**See {@link #documentFlowTypeDL}. */
	public List<SelectItem> getDocumentFlowTypeDL() {
		return documentFlowTypeDL;
	}
	/**See {@link #documentFlowTypeDL}. */
	public void setDocumentFlowTypeDL(List<SelectItem> documentFlowTypeDL) {
		this.documentFlowTypeDL = documentFlowTypeDL;
	}

	/**See {@link #documentFlowType}. */
	public String getDocumentFlowType() {
		return documentFlowType;
	}
	/**See {@link #documentFlowType}. */
	public void setDocumentFlowType(String documentFlowType) {
		this.documentFlowType = documentFlowType;
	}

	/**See {@link #documentChainList}. */
	public List<SelectItem> getDocumentChainList() {
		if (documentChainList == null) {
			documentChainList = createDocumentChainList();
		}
		return documentChainList;
	}
	/**See {@link #documentChainList}. */
	public void setDocumentChainList(List<SelectItem> documentChainList) {
		this.documentChainList = documentChainList;
	}

	/**See {@link #selectedObject}. */
	public Object getSelectedObject() {
		return selectedObject;
	}
	/**See {@link #selectedObject}. */
	public void setSelectedObject(Object selectedObject) {
		this.selectedObject = selectedObject;
	}

    /** See {@link #documentId}. */
	public Integer getDocumentId() {
		return documentId;
	}
	/** See {@link #documentId}. */
	public void setDocumentId(Integer documentId) {
		this.documentId = documentId;
	}

	/** See {@link #inputError}. */
	public boolean isInputError() {
		return getErrorMessage() != null;
	}

	/** See {@link #uploadDocument}. */
	public Document getUploadDocument() {
		return uploadDocument;
	}
	/** See {@link #uploadDocument}. */
	public void setUploadDocument(Document uploadDocument) {
		this.uploadDocument = uploadDocument;
	}

	/** The list of Select Item to show the Employee Action values
	 *  on to the select item list.
	 */
	public DocumentAction[] getEmpActionTypeList() {
		 return DocumentAction.values();
	}

	/** See {@link #appActionTypeList}. */
	public List<SelectItem> getAppActionTypeList() {
		if (appActionTypeList == null) {
			appActionTypeList = new ArrayList<>();
			appActionTypeList.add(new SelectItem(true, "Review and Approve the Document"));
			appActionTypeList.add(new SelectItem(false, "No Approval Required"));
		}
		return appActionTypeList;
	}
	/** See {@link #appActionTypeList}. */
	public void setAppActionTypeList(List<SelectItem> appActionTypeList) {
		this.appActionTypeList = appActionTypeList;
	}

	/** See {@link #shortName}. */
	public String getShortName() {
		return shortName;
	}
	/** See {@link #shortName}. */
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/** See {@link #employeeAction}. */
	public DocumentAction getEmployeeAction() {
		return employeeAction;
	}
	/** See {@link #employeeAction}. */
	public void setEmployeeAction(DocumentAction employeeAction) {
		this.employeeAction = employeeAction;
	}

	/** See {@link #approvalRequired}. */
	public boolean getApprovalRequired() {
		return approvalRequired;
	}
	/** See {@link #approvalRequired}. */
	public void setApprovalRequired(boolean approvalRequired) {
		this.approvalRequired = approvalRequired;
	}

	/** See {@link #isEditInfo}. */
	public boolean getIsEditInfo() {
		return isEditInfo;
	}
	/** See {@link #isEditInfo}. */
	public void setIsEditInfo(boolean isEditInfo) {
		this.isEditInfo = isEditInfo;
	}

	/** See {@link #isNameError}. */
	public boolean getIsNameError() {
		return isNameError;
	}
	/** See {@link #isNameError}. */
	public void setIsNameError(boolean isNameError) {
		this.isNameError = isNameError;
	}

	/** See {@link #attachmentId}. */
	public Integer getAttachmentId() {
		return attachmentId;
	}
	/** See {@link #attachmentId}. */
	public void setAttachmentId(Integer attachmentId) {
		this.attachmentId = attachmentId;
	}

	/** See {@link #isAttach}. */
	public boolean getIsAttach() {
		return isAttach;
	}
	/** See {@link #isAttach}. */
	public void setIsAttach(boolean isAttach) {
		this.isAttach = isAttach;
	}

	/** See {@link #isPrivateAttachment}. */
	public boolean getIsPrivateAttachment() {
		return isPrivateAttachment;
	}
	/** See {@link #isPrivateAttachment}. */
	public void setIsPrivateAttachment(boolean isPrivateAttachment) {
		this.isPrivateAttachment = isPrivateAttachment;
	}

	/** See {@link #isXfdf}. */
	public boolean getIsXfdf() {
		return isXfdf;
	}
	/** See {@link #isXfdf}. */
	public void setIsXfdf(boolean isXfdf) {
		this.isXfdf = isXfdf;
	}

	/** See {@link #isReplacement}. */
	public boolean getIsReplacement() {
		return isReplacement;
	}
	/** See {@link #isReplacement}. */
	public void setIsReplacement(boolean isReplacement) {
		this.isReplacement = isReplacement;
	}

	/** See {@link #documentTypeList}*/
	public List<SelectItem> getDocumentTypeList() {
		if (documentTypeList == null) {
			createDocumentTypeList();
		}
		return documentTypeList;
	}
	/** See {@link #documentTypeList}*/
	public void setDocumentTypeList(List<SelectItem> documentTypeList) {
		this.documentTypeList = documentTypeList;
	}

}
