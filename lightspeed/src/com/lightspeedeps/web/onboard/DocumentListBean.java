package com.lightspeedeps.web.onboard;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.*;

import org.apache.commons.logging.*;
import org.icefaces.ace.model.SimpleEntry;
import org.icefaces.ace.model.tree.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.onboard.form.CustomFormBean;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.util.*;
import com.lightspeedeps.web.view.View;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.Filter;
import com.pdftron.filters.FilterReader;
import com.pdftron.pdf.*;

/**
 * This class is the backing bean for the Document mini tab,
 * under Onboarding sub-main tab.
 * It is also useful in generating the list of documents and folder in the root folder.
 */
@ManagedBean
@ViewScoped
public class DocumentListBean extends View implements Serializable, Disposable {

	/**	 */
	private static final long serialVersionUID = 7887823811281206415L;

	private static String SUPER_ADMIN = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PROD_ADMIN);

	/** The logger  */
	private static final Log LOG = LogFactory.getLog(DocumentListBean.class);

	/** "Edit Info" action code for popup confirmation/prompt dialog. */
	private static final int ACT_EDIT_INFO = 10;

	/** "Create" action code for popup confirmation/prompt dialog. */
	//private static final int ACT_CREATE = 11;

	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 12;

	/** "Move" action code for popup confirmation/prompt dialog. */
	//private static final int ACT_MOVE = 13;

	/** "Upload" action code for popup confirmation/prompt dialog. */
	private static final int ACT_UPLOAD = 14;

	/** "Load XFDF" action code for popup confirmation/prompt dialog. */
	private static final int ACT_LOAD_XFDF = 15;

	/** "Order documents" action code for popup confirmation. LS-4600 */
	private static final int ACT_DOC_ORDER = 16;

	/** The filename entered (in the input field) by the user. */
//	private String userFileName;

	/** The FolderDAO instance */
	private transient FolderDAO folderDAO;

	/** The DocumentChainDAO instance */
	private transient DocumentChainDAO documentChainDAO;

	/** The DocumentDAO instance */
	private transient DocumentDAO documentDAO;

	/** The PayrollPreferenceDAO instance */
	private transient PayrollPreferenceDAO payrollPreferenceDAO;

	/** The documentList, list of all the documents and folders in the root folder	 */
	private List<DocRowItem> documentList;

	/** The currently selected folder on the bread crump */
	private Folder currentFolder = null;

	/** The root folder of the document store */
	private Folder onboardFolder = null;

	/** The list of folders to be shown on the bread crumb */
	private List<Folder> folderSelectedList = new ArrayList<>(0);

	/** The list of checked DocRoWItems from the data table */
	private List<DocRowItem> checkBoxSelectedItems = new ArrayList<>(0);

	/** The DocRowItem instance used to identify the check box selection */
	private DocRowItem item = null;

	/** List of FolderNodeImpl that is used to generate the nodes of the tree */
	private List<FolderNodeImpl> treeRoots;

	/** The NodeStateMap used to hold the state of the currently selected node on the tree */
	private NodeStateMap stateMap = new NodeStateMap();

	/** Boolean field used to check if the single node is selected or not, default is true */
	private boolean singleSelect = true;

	/** Current folder name , usually the folder selected on the tree */
	private String selectedFolderName = null;

	@SuppressWarnings("rawtypes")
	private ArrayList<Map.Entry<DocRowItem, List>> foldersData = null;

	/** The standardDocumentList, list of all the standard documents.*/
	//private final List<Document> standardDocumentList = null;

	private final Disposer disposer = Disposer.getInstance();

	/** Holds the database id of document chain of the document whose preview is shown on either browser window or on start forms mini tab */
	private Integer documentChainId;

	public DocumentListBean() {
		super("File.");
		LOG.debug("");
		disposer.register(this);
		documentList = null;
		checkBoxSelectedItems = new ArrayList<>(0);
		onboardFolder = FileRepositoryUtils.findOnboardingFolder(null);
		if (onboardFolder != null) {
			if (folderSelectedList.size() == 0) {
				folderSelectedList.add(onboardFolder);
			}
			setSelectedFolderName(onboardFolder.getName());
		}
		Integer documentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_UPLOADED_DOCUMENT_ID);
		if (documentId != null) {
			Document  document = getDocumentDAO().findById(documentId);
			if (document != null && document.getDocChainId() != null && document.getDocChainId() == 0) {
				getDocumentDAO().deleteDocument(document.getId());
			}
		}
		// To delete the uploaded attachments, if they are not uploaded successfully.
		Integer attachmentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_UPLOADED_ATTACHMENT_ID);
		if (attachmentId != null) {
			Attachment attachment = AttachmentDAO.getInstance().findById(attachmentId);
			if (attachment != null && attachment.getContactDocument() == null) {
				AttachmentDAO.getInstance().delete(attachment);
			}
		}
	}

	public static DocumentListBean getInstance() {
		return (DocumentListBean)ServiceFinder.findBean("documentListBean");
	}

	/** The method is used to create the contents for the currently selected folder
	 *  NOTE ->the method is intended to support revision system which is implemented but not shown on the UI
	 * @param notUsed (previously) the folder on which the UI operations are performed
	 * @return docList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ArrayList<Map.Entry<DocRowItem, List>> createDocumentList(Folder notUsed) {
		foldersData = new ArrayList<>();
		ArrayList<Map.Entry<DocRowItem, List>> documentChildsList = null;
		Production p = SessionUtils.getProduction();
		boolean canada = p.getType().isCanadaTalent();
		// Refresh PayrollPreference object to prevent LIE
		PayrollPreference pf = getPayrollPreferenceDAO().refresh(p.getPayrollPref());
		PayrollService ps = pf.getPayrollService();
		boolean hideModelRelease = ! FF4JUtils.useFeature(FeatureFlagType.TTCO_MODEL_RELEASE_FORM);
		// LS-2038 - Do not add Model Release to tours, hybrid, Canadian or Non-Team productions.
		hideModelRelease |= ps == null || !ps.getTeamPayroll() || p.getType().isTours() || p.getType().isCanadaTalent() || pf.getIncludeTouring();
		// Hide Payroll Start if 'use model release' option selected and FF is on. LS-4502; LS-4757
		boolean hideStartForm = (! hideModelRelease) && (pf.getUseModelRelease() && FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS));

		//List<DocRowItem> docList = new ArrayList<DocRowItem>();
		/*for (Folder f : getSubFolderList()) {
			foldersData.add(new SimpleEntry(new DocRowItem(false,null,f,null),null));
		}*/
		for (DocumentChain docChain : createDocumentChainList()) {
			if (canada && docChain.hideForCanada()) {
				continue;
			}
			documentChildsList = new ArrayList<>();
			for (Document d : createDocumentList(docChain)) {
				if (d.getFormType() == PayrollFormType.UDA_INM) {
					if (! FF4JUtils.useFeature(FeatureFlagType.TTCO_SHOW_UDA)) {
						continue;
					}
				}
				if (d.getFormType() == PayrollFormType.MODEL_RELEASE && hideModelRelease) {
					// Hide Model release based on Feature flags and production type. LS-2800, LS-2038
					continue;
				}
				if (d.getFormType() == PayrollFormType.START && hideStartForm) {
					// Hide Payroll Start if 'use model release' option selected and FF is on. LS-4502; LS-4757
					continue;
				}
				documentChildsList.add(new SimpleEntry(new DocRowItem(false,null,null,d),null));
			}
			if (! documentChildsList.isEmpty()) {
				foldersData.add(new SimpleEntry(new DocRowItem(false,docChain,null,null),documentChildsList));
			}
		}
		/*if((currentFolder != null) && (currentFolder.getName().equalsIgnoreCase("Onboarding")) && (! SessionUtils.getProduction().isSystemProduction())) {
			for (Document doc : getStandardDocumentList()) {
				documentChildsList = new ArrayList<Map.Entry<DocRowItem, List>>();
				documentChildsList.add(new SimpleEntry(new DocRowItem(false,null,null,doc),null));
				DocumentChain chain = getDocumentChainDAO().findById(doc.getDocChainId());
				foldersData.add(new SimpleEntry(new DocRowItem(false,chain,null,null),documentChildsList));
			}
		}*/
		LOG.debug("Returns list " + foldersData.size());

		Collections.sort(foldersData, new Comparator<Map.Entry<DocRowItem, List>>() {
			@Override
			public int compare(Map.Entry o1, Map.Entry o2) {
				DocRowItem d1 = (DocRowItem) o1.getKey();
				DocRowItem d2 = (DocRowItem) o2.getKey();
				return d1.getDocumentChain().getNormalizedName().compareTo(d2.getDocumentChain().getNormalizedName());
			}
		});
//		PopupMoveBean move = PopupMoveBean.getInstance();
//		move.initForOnboarding(onboardFolder);
		SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_FORM_I9_ID, null);
		return foldersData;
	}

	/** This method is used to get the child folders of the currently selected folder
	 * @return list
	 */
	protected List<Folder> getSubFolderList() {
		return getFolderDAO().findByProperty("folder", currentFolder);
	}

	/** This method is used to get the list of document chains present in the currently selected folder
	 * @return list
	 */
	protected List<DocumentChain> createDocumentChainList() {
		return getDocumentChainDAO().findByProperty("folder", currentFolder);
	}

	protected List<Document> createDocumentList(DocumentChain docChain) {
		return getDocumentDAO().findByDocumentChain(docChain);
	}

	/** Action method for creating a new folder
	 * @return null navigation string.
	 */
	/*public String actionCreateFolder() {
		PopupInputBean inputBean = PopupInputBean.getInstance();
		inputBean.show(this, ACT_CREATE, "DocumentListBean.NewFolder.");
		inputBean.setInput("New Folder");
		inputBean.setMaxLength(Folder.MAX_NAME_LENGTH);
	return null;
	}*/

	/** Action method for Deleting folder(s) / document(s)
	 * @return null navigation string.
	 */
	public String actionDelete() {
		try {
			PopupBean bean = PopupBean.getInstance();
			String message = null;
			String prefix= null;
			Integer docListSize = null;
			Document document = null;
			DocumentChain docChain = null;
			Folder folder = null;
			Integer countOfPackets = 0;
			Integer countOfDocs = 0;
			Integer countOfFolders = 0;
			Integer countLocked = 0;
			List<Document> docList = new ArrayList<>();
			List<Document> totalDocList = new ArrayList<>();
			if (currentFolder != null) {
				if (checkBoxSelectedItems.size() != 0) {
					if (checkBoxSelectedItems.size() > 1) {
						outer:
						for (DocRowItem selItem : checkBoxSelectedItems) {
							LOG.debug("");
							if (selItem.getDocumentChain() != null) {
								LOG.debug("");
								docChain = selItem.getDocumentChain();
								docChain = getDocumentChainDAO().refresh(docChain);
								/*if (docChain.getApprovalPath() != null && docChain.getApprovalPath().size() > 0) {
									MsgUtils.addFacesMessage("DocumentListBean.DocumentChainWithApprovalPath", FacesMessage.SEVERITY_ERROR);
									return null;
								}*/
								if (docChain != null) {
									docList = getDocumentDAO().findByDocumentChain(docChain);
									totalDocList.addAll(docList);
									for (Document doc : docList) {
										LOG.debug("Document = " + doc.getId());
										if (doc.getStandard()) {
											MsgUtils.addFacesMessage("DocumentListBean.StandardDocument", FacesMessage.SEVERITY_INFO, doc.getNormalizedName());
											return null;
										}
										Document d = getDocumentDAO().findOneByProperty("originalDocumentId", doc.getId());
										if (d != null) {
											MsgUtils.addFacesMessage("DocumentListBean.OriginalDocument", FacesMessage.SEVERITY_INFO, doc.getNormalizedName());
											return null;
										}
										countOfDocs++;
										boolean isLocked = getDocumentDAO().lock(doc, getvUser());
										LOG.debug("LOCKED = " + isLocked);
										if (! isLocked) {
											countLocked++;
											LOG.debug("count of locked documents = " + countLocked);
											MsgUtils.addFacesMessage("DocumentListBean.Delete.DocumentLocked", FacesMessage.SEVERITY_INFO, doc.getNormalizedName());
											continue outer;
										}
										if (doc.getPacketList().size() > 0) {
											countOfPackets++;
											LOG.debug("count of packets = "+countOfPackets);
										}
									}
								}
								else {
									countLocked++;
								}
							}
							/*else if (item.getDocument() != null) {
								document = item.getDocument();
								log.debug("");
								countOfDocs++;
								if (document.getPacketList().size() > 0) {
									log.debug("");
									countOfPackets++;
									log.debug("count of packets = "+countOfPackets);
								}
							}
							else if (item.getFolder() != null) {
								log.debug("");
								folder = item.getFolder();
								countOfFolders++;
								log.debug("count of folders = "+countOfFolders);
								if (getDocumentListOfFolder(folder) != null) {
									for (Document docInFolder : getDocumentListOfFolder(folder)) {
										if (docInFolder.getPacketList().size() > 0) {
											countOfPackets++;
											log.debug("count of packets = "+countOfPackets);
										}
									}
								}
							}*/
							if (totalDocList != null && document != null) {
								if (docList.contains(document)) {
									if(document.getPacketList().size() > 0) {
										countOfPackets--;
										LOG.debug("count of packets = "+countOfPackets);
									}
								}
							}
						}
						LOG.debug("count of packets = "+countOfPackets);
						docListSize = checkBoxSelectedItems.size();
						if (countLocked > 0) {
							MsgUtils.addFacesMessage("DocumentListBean.Delete.LockedError", FacesMessage.SEVERITY_ERROR, countLocked);
							return null; // don't bother trying to delete some of them
						}
						else if (countOfFolders > 0 && document == null && docChain == null) {
							LOG.debug("");
							if (countOfPackets <= 0) {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteMultipleFolder", countOfFolders)
										+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
							else {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteMultipleFolder", countOfFolders)
										+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
										+ MsgUtils.formatMessage("DocumentListBean.Delete.WarningMessage", countOfPackets)
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
						}
						else if (countOfDocs > 0 && folder == null) {
							LOG.debug("");
							if (countOfPackets <= 0) {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize);
								if (countLocked > 0) { // not currently used
									message = message + MsgUtils.formatMessage("DocumentListBean.Delete.LockedWarningMessage", countLocked);
								}
								message = message + MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
							else {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize)
										+ MsgUtils.formatMessage("DocumentListBean.Delete.WarningMessage", countOfPackets);
								if (countLocked > 0) { // not currently used
									message = message + MsgUtils.formatMessage("DocumentListBean.Delete.LockedWarningMessage", countLocked);
								}
								message = message + MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
						}
						else if (countOfPackets <= 0) {
							message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize)
									+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
									+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
						}
						else {
							message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize)
									+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
									+ MsgUtils.formatMessage("DocumentListBean.Delete.WarningMessage", countOfPackets)
									+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
						}
						bean.show(this, ACT_DELETE, "DocumentListBean.DeleteItems.");
						bean.setMessage(message);
					}
					else {
						// For single item
						LOG.debug("");
						DocRowItem selItem = checkBoxSelectedItems.get(0);
						if (selItem.getFolder() != null) {
							/*log.debug("");
							prefix = "DocumentListBean.DeleteFolderWOPacket.";
							itemName = item.getFolder().getName();
							for (Document docInFolder : getDocumentListOfFolder(item.getFolder())) {
								if (docInFolder.getPacketList().size() > 0) {
									countOfPackets++;
									log.debug("docs in packets count = "+countOfPackets);
								}
							}
							if (countOfPackets == 0) {
								log.debug("");
								message = MsgUtils.formatMessage(prefix + "Text", itemName)
										+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
							else {
								log.debug("");
								message = MsgUtils.formatMessage(prefix + "Text", itemName)
										+ MsgUtils.getMessage("DocumentListBean.Delete.InformationMessage")
										+ MsgUtils.formatMessage("DocumentListBean.Delete.WarningMessage", countOfPackets)
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}*/
						}
						else {
							if (selItem.getDocumentChain() != null) {
								LOG.debug("");
								DocumentChain chain = selItem.getDocumentChain();
								chain = getDocumentChainDAO().refresh(chain);
								if (chain != null) {
									docList = getDocumentDAO().findByDocumentChain(chain);
									docListSize = 1;
									prefix = "DocumentListBean.DeleteDocument.";
									/*if (chain.getApprovalPath() != null && chain.getApprovalPath().size() > 0) {
										MsgUtils.addFacesMessage("DocumentListBean.DocumentChainWithApprovalPath", FacesMessage.SEVERITY_ERROR);
										return null;
									}*/
									for (Document doc : docList) {
										if (doc.getStandard()) {
											MsgUtils.addFacesMessage("DocumentListBean.StandardDocument", FacesMessage.SEVERITY_INFO, doc.getNormalizedName());
											return null;
										}
										Document d = getDocumentDAO().findOneByProperty("originalDocumentId", doc.getId());
										if (d != null) {
											MsgUtils.addFacesMessage("DocumentListBean.OriginalDocument", FacesMessage.SEVERITY_INFO, doc.getNormalizedName());
											return null;
										}
										if (! checkLocked(doc, getvUser(), "Delete.")) {
											countLocked++;
											return null;
										}
										if (doc.getPacketList().size() > 0) {
											countOfPackets++;
										}
									}
								}
								else {
									PopupBean.getInstance().show(null, 0,
											"ContactFormBean.DocumentLocked.Title",
											"ContactFormBean.DocumentLocked.Delete.Text",
											"Confirm.OK", null);
								}
							}
							else if (selItem.getDocument() != null) {
								LOG.debug("");
								prefix = "DocumentListBean.DeleteDocument.";
								docListSize = 1;
								if (selItem.getDocument().getPacketList().size() > 0) {
									countOfPackets = 1;
								}
							}
							if (countOfPackets == 0) {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize)
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
							else {
								message = MsgUtils.formatMessage("DocumentListBean.DeleteItems.Text", docListSize)
										+ MsgUtils.formatMessage("DocumentListBean.Delete.WarningMessage", countOfPackets)
										+ MsgUtils.getMessage("DocumentListBean.DeleteFolderWOPacket.DeleteMessage");
							}
						}
						bean.show(this, ACT_DELETE, prefix);
						bean.setMessage(message);
					}
				}
				else {
					MsgUtils.addFacesMessage("DocumentListBean.SelectFolder", FacesMessage.SEVERITY_INFO);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

//	/** Utility method returns the list of documents for a particular folder
//	 * @param folder folder whose document list is returned
//	 * @return list of documents
//	 */
//	private List<Document> getDocumentListOfFolder(Folder folder) {
//		List<Document> documentList = new ArrayList<>();
//		List<Integer> folderIdList = new ArrayList<>();
//		folderIdList = getDocumentDAO().addFolderId (folderIdList, folder);
//		documentList = getDocumentDAO().findByNamedQuery(Document.GET_DOCUMENT_LIST_BY_FOLDER_IDS, "folderId",folderIdList);
//		return documentList;
//	}

	/** Action method for Moving folder(s) / document(s)
	 * @return null navigation string.
	 */
	/*public String actionMove(){
		if(checkBoxSelectedItems.size() != 0){
			DocRowItem item = checkBoxSelectedItems.get(0);
			if (checkBoxSelectedItems.size() > 1) {
				checkBoxSelectedItems = new ArrayList<>(0);
				MsgUtils.addFacesMessage("DocumentListBean.SelectSingleItem", FacesMessage.SEVERITY_INFO);
			}
			else if (item.getDocument() != null){
				MsgUtils.addFacesMessage("DocumentListBean.DeselectDocument", FacesMessage.SEVERITY_INFO);
			}
			else {
				PopupMoveBean moveBean = PopupMoveBean.getInstance();
				if(item.getDocumentChain() != null) {
					Document document = DocumentDAO.getInstance().findOneByProperty("docChainId", item.getDocumentChain().getId());
					if(document != null && document.getStandard() == true) {
						MsgUtils.addFacesMessage("DocumentListBean.StandardDocument", FacesMessage.SEVERITY_INFO, document.getNormalizedName());
						return null;
					}
				}
					moveBean.show(this, ACT_MOVE, "DocumentListBean.MoveFolder.", onboardFolder);
					moveBean.setInputError(false);
			}
		}
		else {
			MsgUtils.addFacesMessage("DocumentListBean.SelectFolder", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}
*/
	/**
	 * Action method for editing the document's stored information.
	 * @return null navigation string.
	 */
	public String actionEditInfo() {
		try {
			if (currentFolder != null ) {
				// can't rename the root node; this check is a failsafe in case
				// the rename method is somehow activated despite the button being disabled.
				item = null;
				if (checkBoxSelectedItems.size() != 0) {
					if (checkBoxSelectedItems.size() > 1) {
						MsgUtils.addFacesMessage("DocumentListBean.EditDocument", FacesMessage.SEVERITY_INFO);
					}
					else {
						item = checkBoxSelectedItems.get(0);
						if (item.getDocumentChain() != null) {
							Document document = getDocumentDAO().findOneByProperty("docChainId", item.getDocumentChain().getId());
							if (document != null && document.getStandard()) {
								MsgUtils.addFacesMessage("DocumentListBean.StandardDocument", FacesMessage.SEVERITY_INFO, document.getNormalizedName());
								return null;
							}
							else if (document != null) {
								if (! checkLocked(document, getvUser(), "")) {
									return null;
								}
								PopupUploadBean uploadBean = PopupUploadBean.getInstance();
								uploadBean.show(this, ACT_EDIT_INFO, "DocumentListBean.EditInfo.", currentFolder, document);
								return null;
							}
						}
					}
				}
				else {
					MsgUtils.addFacesMessage("DocumentListBean.SelectFolderRename", FacesMessage.SEVERITY_INFO);
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
	 * Action method for uploading a document
	 * @return null navigation string.
	 */
	public String actionUpload(){
		PopupUploadBean uploadBean = PopupUploadBean.getInstance();
		uploadBean.show(this, ACT_UPLOAD, "DocumentListBean.Upload.", currentFolder);
		return null;
	}

	/**
	 * Action method for uploading a document's Xfdf.
	 * @return null navigation string.
	 */
	public String actionLoadXFDF(){
		if (checkBoxSelectedItems.size() != 0) {
			if (checkBoxSelectedItems.size() > 1) {
				MsgUtils.addFacesMessage("DocumentListBean.LoadXfdf", FacesMessage.SEVERITY_INFO);
			}
			else {
				DocumentChain docchain = checkBoxSelectedItems.get(0).getDocumentChain();
				Document doc = documentDAO.findOneByProperty("docChainId", docchain.getId());
				PopupUploadBean uploadBean = PopupUploadBean.getInstance();
				uploadBean.show(this, ACT_LOAD_XFDF, "DocumentListBean.LoadXfdf.", doc);
			}
		}
		else {
			MsgUtils.addFacesMessage("DocumentListBean.SelectFolderRename", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/**
	 * Action method for the "Change Order" button -- just opens the
	 * "change order" pop-up dialog. LS-4600
	 *
	 * @return null navigation string
	 */
	public String actionOpenChangeOrder() {
		DocumentOrderBean bean = DocumentOrderBean.getInstance();
		bean.show(this, ACT_DOC_ORDER, null, null, null, createDocOrderList());
//		addClientResizeScroll();
		return null;
	}

	/**
	 * @return a simple List of Documents corresponding the the document list
	 *         displayed ({@link #foldersData}), which is a List of complex
	 *         items. LS-4600
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Document> createDocOrderList() {
		List<Document> docs = new ArrayList<Document>();
		for (Entry<DocRowItem, List> docRowItem : foldersData) {
			docRowItem.getKey().setChecked(false);
			List<Entry<DocRowItem, List>> docChildList = docRowItem.getValue();
			docs.add(docChildList.get(0).getKey().getDocument());
		}
		return docs;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_EDIT_INFO:
				res = actionEditInfoOk();
				break;
			/*case ACT_CREATE:
				res = actionCreateFolderOk();
				break;*/
			case ACT_DELETE:
				res = actionDeleteOk();
				break;
			case ACT_DOC_ORDER: // LS-4600
				// refresh current packet list in case order changed
				PacketListBean.getInstance().refreshPacketList();
				// refresh our list which contains order values
				foldersData = createDocumentList(currentFolder);
				break;
			case ACT_UPLOAD:
				res = actionUploadOk();
				break;
			case ACT_LOAD_XFDF:
				res = actioLoadXFDFOk();
				break;
		}
		return res;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public String confirmCancel(int action) {
		// checkBoxSelectedItems = new ArrayList<>(0) ;
		if (action == ACT_UPLOAD) {
			Integer id = PopupUploadBean.getInstance().getDocumentId(); // Get saved document.id value from bean
			Document document = getDocumentDAO().findById(id);
			if (document != null) {
				getDocumentDAO().deleteDocument(document.getId());
			}
			// force page refresh; seems to be only way to clear fileUpload message
			return HeaderViewBean.PEOPLE_MENU_ONBOARDING;
		}
		else if (action == ACT_EDIT_INFO || action == ACT_LOAD_XFDF) {
			for (Entry<DocRowItem, List> docRowItem : foldersData) {
				docRowItem.getKey().setChecked(false);
			}
			item = checkBoxSelectedItems.get(0);
			if (item.getDocumentChain() != null) {
				Document document = getDocumentDAO().findOneByProperty("docChainId", item.getDocumentChain().getId());
				if (document != null) {
					getDocumentDAO().unlock(document, getvUser().getId());
				}
			}
			checkBoxSelectedItems = new ArrayList<>(0);
		}
		else if (action == ACT_DELETE) {
			if (checkBoxSelectedItems != null && (! checkBoxSelectedItems.isEmpty())) {
				LOG.debug("");
				for (DocRowItem selItem : checkBoxSelectedItems) {
					LOG.debug("");
					if (selItem.getDocumentChain() != null) {
						LOG.debug("");
						Document document = getDocumentDAO().findOneByProperty("docChainId", selItem.getDocumentChain().getId());
						document = getDocumentDAO().refresh(document);
						LOG.debug("Document = " + document.getId());
						boolean isUnlocked = getDocumentDAO().unlock(document, getvUser().getId());
						LOG.debug("UNLOCKED = " + isUnlocked);
					}
				}
			}
		}
		else if (action == ACT_DOC_ORDER) {
			// no action necessary. LS-4600
		}
		return super.confirmCancel(action);
	}

	/**
	 * Called when the user clicks OK on the "create folder" pop-up dialog.
	 */
	/*private String actionCreateFolderOk() {
		try {
			if (currentFolder != null) {
				currentFolder = getFolderDAO().refresh(currentFolder);
				if (! userFileName.equals("")) {
					createNewFolder(userFileName, currentFolder.getId());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("DocumentListBean createSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage(); // Let the user know it failed.
		}
		return null;
	}

	public void createNewFolder(String userFileName, Integer parentId) {
		Folder folder=getFolderDAO().findFolderByParentIdAndName(userFileName, parentId);
		if (folder != null) {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			inputBean.displayError(MsgUtils.getMessage("DocumentListBean.DuplicateFolder"));
		}
		else {
			getFolderDAO().createFolder(userFileName, parentId, false);
			userFileName = "";
			foldersData = createDocumentList(currentFolder);
			refreshTree();
		}
	}
*/
	/**
	 * Called when the user clicks Delete on the "delete folder" pop-up dialog.
	 */
	public String actionDeleteOk() {
		try {
			if (checkBoxSelectedItems.size() != 0) {
				// can't delete the root node; this check is a failsafe in case
				// the delete method is somehow activated despite the button being disabled.
				for (DocRowItem selItem : checkBoxSelectedItems) {
					boolean bret = false;
					if (selItem.getFolder() != null) {
						LOG.debug("<>");
						getFolderDAO().remove(selItem.getFolder());
					}
					else if (selItem.getDocumentChain() != null) {
						currentFolder = getFolderDAO().refresh(currentFolder);
						DocumentChain docChain = selItem.getDocumentChain();
						docChain = getDocumentChainDAO().refresh(docChain);
						if (docChain != null) {
							Document doc = getDocumentDAO().findOneByProperty("docChainId", docChain.getId());
							bret = getDocumentDAO().unlock(doc, getvUser().getId());
							if (bret) {
								currentFolder = getDocumentChainDAO().remove(docChain);
							}
						}
					}
					else {
						currentFolder = getFolderDAO().refresh(currentFolder);
						Document document = selItem.getDocument();
						document = getDocumentDAO().refresh(document);
						bret = getDocumentDAO().unlock(document, getvUser().getId());
						if (bret) {
							currentFolder = getDocumentDAO().remove(document, true);
							if (currentFolder == null) {
								MsgUtils.addFacesMessage("Document.NotDeletable", FacesMessage.SEVERITY_ERROR);
							}
						}
					}
				}
				checkBoxSelectedItems = new ArrayList<>(0);
				foldersData = createDocumentList(currentFolder);
				refreshTree();
				PacketListBean.getInstance().refreshPacketList();
				StatusListBean.getInstance().setContactDocumentList(null);
				CopyDocumentBean.getInstance().setDocumentChainList(null);
				ApprovalPathsBean.getInstance().setDocumentChainList(null);
			}
		}
		catch (Exception e) {
			checkBoxSelectedItems = new ArrayList<>(0);
			foldersData = createDocumentList(currentFolder);
			EventUtils.logError("DocumentListBean delete failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user hits OK on the Edit Document Info prompt dialog.
	 * Edits the selected document.
	 */
	@SuppressWarnings("rawtypes")
	private String actionEditInfoOk() {
		DocumentChain renameDocumentChain = null;
		PopupUploadBean bean = PopupUploadBean.getInstance();
		String newName = bean.getDisplayFilename();
		try {
			if(item != null && item.getDocumentChain() != null) {
				boolean bret = false;
				renameDocumentChain = item.getDocumentChain();
				renameDocumentChain = getDocumentChainDAO().refresh(renameDocumentChain);
				if (renameDocumentChain != null) {
				Document renameDocument = getDocumentDAO().findOneByProperty("docChainId", renameDocumentChain.getId());
				bret = getDocumentDAO().unlock(renameDocument, getvUser().getId());
				if (bret) {
					if (! StringUtils.isEmpty(newName)) {
						if(item.getDocumentChain() != null) {
							//renameDocumentChain = item.getDocumentChain();
							DocumentChain docChain = getDocumentChainDAO()
									.findDocumentChainByFolderIdAndName(newName, renameDocumentChain.getFolder().getId());
							if (docChain != null && (! docChain.equals(renameDocumentChain))) {
								bean.setIsNameError(true);
								bean.displayError(MsgUtils.getMessage("DocumentListBean.DuplicateDocumentChain"));
								return null;
							}
							else {
								if ((bean.getEmployeeAction() == DocumentAction.RCV || bean.getEmployeeAction() == DocumentAction.RD)
										&& bean.getApprovalRequired()) {
									bean.setIsNameError(false);
									bean.displayError(MsgUtils.getMessage("DocumentListBean.InvalidEmployeeAction"));
									return null;
								}
								//renameDocumentChain = getDocumentChainDAO().refresh(renameDocumentChain);
								renameDocumentChain.setName(newName);
								renameDocumentChain.setShortName(bean.getShortName());
								getDocumentChainDAO().attachDirty(renameDocumentChain);
								//Document renameDocument = DocumentDAO.getInstance().findOneByProperty("docChainId", renameDocumentChain.getId());
								if (renameDocument != null) {
									renameDocument = getDocumentDAO().refresh(renameDocument);
									renameDocument.setName(newName);
									renameDocument.setShortName(bean.getShortName());
									renameDocument.setEmployeeAction(bean.getEmployeeAction());
									renameDocument.setApprovalRequired(bean.getApprovalRequired());
									getDocumentDAO().attachDirty(renameDocument);
								}
								currentFolder = renameDocumentChain.getFolder();
								foldersData = createDocumentList(currentFolder);
							}
						}
					}
					else {
						bean.setIsNameError(true);
						MsgUtils.addFacesMessage("DocumentListBean.MissingRename", FacesMessage.SEVERITY_INFO);
					}
				}
			}
			}
			checkBoxSelectedItems = new ArrayList<>(0);
			bean.setIsEditInfo(false);
			for (Entry<DocRowItem, List> docRowItem : foldersData) {
				docRowItem.getKey().setChecked(false);
			}
		}
		catch (Exception e) {
			EventUtils.logError("DocumentListBean renameSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public void renameCheckedFolder(Folder renameFolder, String userFileName) {
		renameFolder = getFolderDAO().rename(renameFolder, userFileName);
		if (renameFolder == null) {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			inputBean.displayError(MsgUtils.getMessage("DocumentListBean.DuplicateFolder"));
		}
		else {
			currentFolder = renameFolder.getFolder();
			foldersData = createDocumentList(currentFolder);
		}
	}

	/**
	 * Action method called when the user hits Move on the Move prompt dialog.
	 */
	/*public String actionMoveOk(){
		try {
			PopupMoveBean moveBean = PopupMoveBean.getInstance();
			log.debug("selected folder id "+moveBean.getClickedFolderId());
			String  itemName = null;
			DocumentChain documentChain = null;
			PopupMoveBean bean = PopupMoveBean.getInstance();
			Folder newParentFolder = getFolderDAO().findById(moveBean.getClickedFolderId());
			for (DocRowItem rowItem : checkBoxSelectedItems) {
				if (rowItem.getFolder() != null) {
					itemName = rowItem.getFolder().getName();
					Folder moveFolder = rowItem.getFolder();
					moveCheckedFolder(newParentFolder, moveFolder);
				}
				else if (rowItem.getDocumentChain() != null) {
					itemName = rowItem.getDocumentChain().getName();
					documentChain = getDocumentChainDAO().findDocumentChainByFolderIdAndName(itemName, moveBean.getClickedFolderId());
					if (documentChain != null) {
						log.debug("in second if duplicate document chain found " + documentChain.getName());
						bean.displayError(MsgUtils.getMessage("DocumentListBean.MoveDocumentChain"));
					}
					else {
						DocumentChain chain = getDocumentChainDAO().findById(rowItem.getDocumentChain().getId());
						chain.setFolder(newParentFolder);
						getDocumentChainDAO().attachDirty(chain);
					}
				}
			}
			foldersData = createDocumentList(currentFolder);
			refreshTree();
			checkBoxSelectedItems.clear();
		}
		catch (Exception e) {
			EventUtils.logError("DocumentListBean moveSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}
*/
	/*public void moveCheckedFolder(Folder newParentFolder, Folder moveFolder) {
		PopupMoveBean moveBean = PopupMoveBean.getInstance();
		Folder folder;
		folder = getFolderDAO().findFolderByParentIdAndName(moveFolder.getName(), newParentFolder.getId());
		if (folder != null) {
			log.debug("in if duplicate folder found");
			moveBean.displayError(MsgUtils.getMessage("DocumentListBean.MoveFolder"));
		}
		else {
			getFolderDAO().move(moveFolder, newParentFolder);
		}
	}*/

	/**
	 * Action method called when the user hits Save on the Upload prompt dialog.
	 */
	public String actionUploadOk(){
		try {
			PopupUploadBean uploadBean = PopupUploadBean.getInstance();
			String docChainName = uploadBean.getDisplayFilename();
			String docFlowType = uploadBean.getDocumentFlowType();
			Integer id = uploadBean.getDocumentId(); // Get saved document.id value from bean
			LOG.debug("document id in document list bean "+id);
			Document latestDocument = getDocumentDAO().findById(id);
			latestDocument.setName(docChainName);
			User user = SessionUtils.getCurrentUser();
			if (user.getEmailAddress().equalsIgnoreCase(SUPER_ADMIN) && (SessionUtils.getProduction().isSystemProduction())) {
				latestDocument.setStandard(true);
			}
			Date documentCreatedTime = latestDocument.getCreated();
			switch (uploadBean.getRevisionType()) {
				case PopupUploadBean.NEW_REVISION_TYPE:
					if (getDocumentChainDAO().findDocumentChainByFolderIdAndName(docChainName,
							currentFolder.getId()) == null) {
						Map<String, Object> values = new HashMap<> ();
						values.put("name", docChainName);
						values.put("folderId", currentFolder.getId());
						if (getDocumentDAO().findOneByNamedQuery(Document.GET_DOCUMENT_BY_NAME_FOLDER_ID, values) == null) {
							if ((uploadBean.getEmployeeAction() == DocumentAction.RCV || uploadBean.getEmployeeAction() == DocumentAction.RD)
									&& uploadBean.getApprovalRequired()) {
								uploadBean.setIsNameError(false);
								uploadBean.displayError(MsgUtils.getMessage("DocumentListBean.InvalidEmployeeAction"));
								return null;
							}
							Integer docChainId = getDocumentChainDAO().saveDocumentChain(currentFolder,
									docChainName, uploadBean.getShortName(), null, latestDocument.getMimeType(), docFlowType,
									documentCreatedTime);
							latestDocument.setDocChainId(docChainId);
							latestDocument.setRevision(1);
							latestDocument.setOldest(true);
							latestDocument.setShortName(uploadBean.getShortName());
							latestDocument.setEmployeeAction(uploadBean.getEmployeeAction());
							latestDocument.setApprovalRequired(uploadBean.getApprovalRequired());
							getDocumentDAO().attachDirty(latestDocument);
						}
						else {
							uploadBean.setIsNameError(true);
							uploadBean.displayError("Document Name duplicate.");
						}
						break;
					}
					else {
						uploadBean.setIsNameError(true);
						uploadBean.displayError("Document Chain Name duplicate.");
					}
					break;
				case PopupUploadBean.UPDATED_REVISION_TYPE:
					DocumentChain docChain = getDocumentChainDAO().findById(
							Integer.parseInt(uploadBean.getSelectedObject().toString()));
					docChain = getDocumentChainDAO().updateDocumentChain(docChain, new Date(),
							latestDocument.getMimeType());
					latestDocument.setRevision(docChain.getRevisions());
					latestDocument.setDocChainId(docChain.getId());
					latestDocument.setOldest(false);
					getDocumentChainDAO().attachDirty(latestDocument);
			}
			foldersData = createDocumentList(currentFolder);
			convertToXod(id);
			ApprovalPathsBean.getInstance().setDocumentChainList(null);
			CopyDocumentBean.getInstance().setDocumentChainList(null);
		}
		catch (Exception e) {
			EventUtils.logError("DocumentListBean upload document failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user hits Save on the Load Master XFDF prompt dialog.
	 */
	@SuppressWarnings("rawtypes")
	public String actioLoadXFDFOk(){
		try {
			PopupUploadBean uploadBean = PopupUploadBean.getInstance();
			String docChainName = uploadBean.getDisplayFilename();
			LOG.debug("Name of file uploaded =  "+docChainName);
			checkBoxSelectedItems = new ArrayList<>(0);
			for (Entry<DocRowItem, List> docRowItem : foldersData) {
				docRowItem.getKey().setChecked(false);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}


	/** Method converts the uploaded pdf's content into an XOD formats and saves ,
	 * it to the corresponding document id
	 * @param docId document ID whose content is to be converted
	 * @throws Exception PDFNetException
	 */
	private void convertToXod(Integer docId) throws Exception {
		//document.OriginalDocumentId will be null in this case, because it is not null for copied documents only.
		Content content = ContentDAO.getInstance().findByDocId(docId, null);
		try {
			LOG.debug("doc id=" + docId);
			PDFDoc pdfDoc = new PDFDoc(content.getContent());
//			XODOutputOptions opts = new XODOutputOptions();
//			opts.setAnnotationOutput(XODOutputOptions.e_flatten); // "flatten" annotations
			LOG.debug("converting PDF to XOD with NO options");
			Filter filter = Convert.toXod(pdfDoc);
			FilterReader filterRdr = new FilterReader(filter);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte [] buf = new byte[1024];
			for (long readNum; (readNum = filterRdr.read(buf)) > 0;) {
				// Write bytes from the byte array starting at offset 0 to the byte array output stream.
				bos.write(buf, 0, (int)readNum);
			}
			byte[] xodContent = bos.toByteArray();
			LOG.debug("xod data length >>>>> "+xodContent.length);
			ContentDAO.getInstance().updateXodContent(docId, xodContent);
//			saveAsFile(xodContent, docId); 	// may be used for debugging
		}
		catch (PDFNetException e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Temporary code to save the XOD data as an external file for debugging.
	 * @param xodContent
	 * @param docId
	 */
	@SuppressWarnings("unused")
	private void saveAsFile(byte[] xodContent, Integer docId) {
		DateFormat df = new SimpleDateFormat("MM-dd_HHmmss");
		String timestamp = df.format(new Date());
		String reportPath = SessionUtils.getRealReportPath();
		String fileName = reportPath +  "xod_" + docId + "_" + timestamp;
		try {
			OutputStream outputStream = new FileOutputStream(new File(fileName));
			outputStream.write(xodContent);
			outputStream.close();
		}
		catch (IOException e) {
			LOG.error(e);
		}
	}

	/** Action listener for folder clicked event in the Folder contents pane (not the tree view).
	 * @param evt
	 */
	public void listenFolderClick(ActionEvent evt) {
		checkBoxSelectedItems.clear();
		LOG.debug("in listenFolderClick");
		Folder clickedFolder = (Folder) evt.getComponent().getAttributes()
				.get("currentRow");
		LOG.debug("clicked folder name  " + clickedFolder.getName()
				+ " and current folder name" + currentFolder.getName());
		if (clickedFolder.getId() == currentFolder.getId()) {
			return;
		}
		// the clicked-on folder must be a child of the 'currentFolder'
		Folder parentFolder = currentFolder; // save it so we can set it's state
		currentFolder = clickedFolder;
		if (folderSelectedList.contains(currentFolder)) {
			folderSelectedList = folderSelectedList.subList(0,
					folderSelectedList.indexOf(currentFolder) + 1);
		}
		else {
			folderSelectedList.add(currentFolder);
		}
		foldersData = createDocumentList(currentFolder);
		setSelectedFolderName(currentFolder.getName());
		stateMap.setAllSelected(false);

		NodeState state = new NodeState();
		state.setSelected(true);
		FolderNodeImpl folder = new FolderNodeImpl();
		folder.setFolder(currentFolder);
		stateMap.put(folder, state);

		NodeState parentState = new NodeState();
		parentState.setExpanded(true);
		FolderNodeImpl parentNode = new FolderNodeImpl(null, parentFolder);
		stateMap.put(parentNode, parentState);

	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenSingleCheck (ValueChangeEvent evt) {
		DocRowItem docRowItem = (DocRowItem) evt.getComponent().getAttributes().get("selectedRow");
		if (docRowItem.getChecked()) {
			checkBoxSelectedItems.add(docRowItem);
		}
		else if (checkBoxSelectedItems.contains(docRowItem)) {
			checkBoxSelectedItems.remove(docRowItem);
		}
	}

	private void refreshTree() {
		if (treeRoots != null && treeRoots.size() > 0) {
			treeRoots.get(0).refreshChildren();
		}
	}

	/**
	 * Method used to create the root node of the folder tree structure.
	 */
	private void createTreeRoots() {
		treeRoots = new ArrayList<>();
		Folder root = FileRepositoryUtils.findOnboardingFolder(null);
		treeRoots.add(new FolderNodeImpl(null, root));
	}

	/** AjaxBehaviorEvent used to listen the action done on the folder tree.
	 * @param evt
	 */
	public void treeListener (AjaxBehaviorEvent evt) {
		FolderNodeImpl folder = (FolderNodeImpl)stateMap.getSelected().get(0);
		currentFolder = folder.getFolder();
		createDocumentList(currentFolder);
		setSelectedFolderName(folder.getFolder().getName());
		checkBoxSelectedItems.clear();
	}

	public String actionPreviewDocument() {
		try {
			LOG.debug(" doc chain id fetched "+documentChainId);
			if (documentChainId != null) {
				Document document = getDocumentDAO().findOneByProperty("docChainId", documentChainId);
				if(document != null && document.getMimeType().equals(MimeType.LS_FORM)) {
					LOG.debug(" doc id fetched " + document.getId());
					String fileName = document.getName(); // ArchiveUtils.retrieveItem(name, unit);
					if (fileName != null) {
						// Create a specially-patterned filename.  When the browser requests this "file",
						// LsFacesServlet will recognize the format, load the Image from the database,
						// and return the Image.contents field to the browser.
						fileName = "../../" + Constants.DOCUMENT_PSEUDO_DIRECTORY + "/" + document.getId() + "/" + fileName;
						String javascriptCode = "window.open('" + fileName + "','signatureWindow');";
						addJavascript(javascriptCode);
					}
				}
				else {
					CustomFormBean.getInstance().previewBlankCustomDocument(document);
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
	 * Attempt to lock the current Document. If this fails, put up a prompt
	 * explaining the problem to the user.
	 *
	 * @param currUser The User to be given the lock.
	 * @return True if the user has acquired the lock, false if not.
	 */
	public boolean checkLocked(Document document, User currUser, String msgType) {
		boolean isLocked = getDocumentDAO().lock(document, currUser);
		if (! isLocked) {
			PopupBean.getInstance().show(null, 0,
					"ContactFormBean.DocumentLocked.Title",
					"ContactFormBean.DocumentLocked." + msgType + "Text",
					"Confirm.OK", null); // no cancel button
			LOG.debug("Edt/Delete/etc prevented: locked by user #" + document.getLockedBy());
			editMode = false;
			return false;
		}
		return true;
	}

	/**
	 * This method is called by our subclasses (which are called by the JSF
	 * framework) when this bean is about to go 'out of scope', e.g., when the
	 * user is leaving the page. Note that in JSF 2.1, this method is not called
	 * for session expiration, so we handle that case via the Disposable
	 * interface.
	 */
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
	 * unlock the Document to make it available again for editing.
	 */
	@Override
	public void dispose() {
		LOG.debug("");
		try {
			if (item != null && item.getDocumentChain() != null && getvUser() != null) {
				LOG.debug("");
				Document docChain = getDocumentDAO().refresh(item.getDocumentChain()); // prevent "non-unique object" failure in logout case
				if (docChain != null) {
					Document document = getDocumentDAO().findOneByProperty("docChainId", docChain.getId());
					document = getDocumentDAO().refresh(document); // prevent "non-unique object" failure in logout case
					if (document != null && document.getLockedBy() != null
							 && getvUser().getId().equals(document.getLockedBy())) {
						LOG.debug("dispose");
						getDocumentDAO().unlock(document, getvUser().getId());
					}
				}

			}
			if (checkBoxSelectedItems != null && (! checkBoxSelectedItems.isEmpty()) && getvUser() != null) {
				for (DocRowItem selItem : checkBoxSelectedItems) {
					LOG.debug("");
					if (selItem.getDocumentChain() != null) {
						LOG.debug("");
						Document document = getDocumentDAO().findOneByProperty("docChainId", selItem.getDocumentChain().getId());
						document = getDocumentDAO().refresh(document);
						LOG.debug("Document = " + document.getId());
						boolean isUnlocked = getDocumentDAO().unlock(document, getvUser().getId());
						LOG.debug("UNLOCKED = " + isUnlocked);
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error("Exception: ", e);
		}
	}

	/** This method returns the FolderDAO instance
	 * @return folderDAO
	 */
	private FolderDAO getFolderDAO() {
		if (folderDAO == null) {
			folderDAO = FolderDAO.getInstance();
		}
		return folderDAO;
	}

	/** This method returns the DocumentChainDAO instance
	 * @return documentChainDAO
	 */
	private DocumentChainDAO getDocumentChainDAO() {
		if (documentChainDAO == null) {
			documentChainDAO = DocumentChainDAO.getInstance();
		}
		return documentChainDAO;
	}

	/** This method returns the DocumentDAO instance
	 * @return documentDAO
	 */
	private DocumentDAO getDocumentDAO() {
		if (documentDAO == null) {
			documentDAO = DocumentDAO.getInstance();
		}
		return documentDAO;
	}

	/** See {@link #approverAuditDAO}. */
	public PayrollPreferenceDAO getPayrollPreferenceDAO() {
		if (payrollPreferenceDAO == null) {
			payrollPreferenceDAO = PayrollPreferenceDAO.getInstance();
		}
		return payrollPreferenceDAO;
	}

	/**See {@link #documentList}. */
	public List<DocRowItem> getDocumentList() {
		if(documentList == null){
			if (currentFolder == null) {
				currentFolder = onboardFolder;
				foldersData = createDocumentList(currentFolder);
			}
		 }
		return documentList;
	}
	/**See {@link #documentList}. */
	public void setDocumentList(List<DocRowItem> documentList) {
		this.documentList = documentList;
	}

	/**See {@link #currentFolder}. */
	public Folder getCurrentFolder() {
		if(currentFolder == null){
			currentFolder = onboardFolder;
		}
		return currentFolder;
	}
	/**See {@link #currentFolder}. */
	public void setCurrentFolder(Folder currentFolder) {
		this.currentFolder = currentFolder;
	}

	/**See {@link #onboardFolder}. */
	public Folder getOnboardFolder() {
		return onboardFolder;
	}
	/**See {@link #onboardFolder}. */
	public void setOnboardFolder(Folder onboardFolder) {
		this.onboardFolder = onboardFolder;
	}

	/**See {@link #folderSelectedList}. */
	public List<Folder> getFolderSelectedList() {
		return folderSelectedList;
	}
	/**See {@link #folderSelectedList}. */
	public void setFolderSelectedList(List<Folder> folderList) {
		folderSelectedList = folderList;
	}

	/** See {@link #checkBoxSelectedItems}. */
	public List<DocRowItem> getCheckBoxSelectedItems() {
		return checkBoxSelectedItems;
	}
	/** See {@link #checkBoxSelectedItems}. */
	public void setCheckBoxSelectedItems(List<DocRowItem> checkBoxSelectedItems) {
		this.checkBoxSelectedItems = checkBoxSelectedItems;
	}

	@SuppressWarnings("rawtypes")
	public ArrayList<Map.Entry<DocRowItem, List>> getFoldersData() {
		if(foldersData == null){
			if (currentFolder == null) {
				currentFolder = onboardFolder;
				foldersData = createDocumentList(currentFolder);
			}
		 }
		return foldersData;
	}
	@SuppressWarnings("rawtypes")
	public void setFoldersData(ArrayList<Map.Entry<DocRowItem, List>> foldersData) {
		this.foldersData = foldersData;
	}

	/**See {@link #treeRoots}. */
	public List<FolderNodeImpl> getTreeRoots() {
		if (treeRoots == null) {
			createTreeRoots();
		}
		return treeRoots;
	}

	/**See {@link #stateMap}. */
	public NodeStateMap getStateMap() {
		return stateMap;
	}
	/**See {@link #stateMap}. */
	public void setStateMap(NodeStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/**See {@link #singleSelect}. */
	public boolean isSingleSelect() {
		return singleSelect;
	}
	/**See {@link #singleSelect}. */
	public void setSingleSelect(boolean singleSelect) {
		this.singleSelect = singleSelect;
	}

	/**See {@link #selectedFolderName}. */
	public String getSelectedFolderName() {
		return selectedFolderName;
	}
	/**See {@link #selectedFolderName}. */
	public void setSelectedFolderName(String selectedFolderName) {
		this.selectedFolderName = selectedFolderName;
	}

	/**See {@link #standardDocumentList}. *//*
	@SuppressWarnings("unchecked")
	public List<Document> getStandardDocumentList() {
		if (standardDocumentList == null) {
			Production prod = ProductionDAO.getInstance().findById(Constants.SYSTEM_PRODUCTION_ID);
			Folder systemOnboard = FileRepositoryUtils.findFolder(Constants.ONBOARDING_FOLDER, prod.getRootFolder());
			standardDocumentList = getDocumentDAO().findByNamedQuery(Document.GET_STANDARD_DOCUMENT_BY_FOLDER_ID, map("folderId", systemOnboard.getId()));
		}
		return standardDocumentList;
	}
	*//**See {@link #standardDocumentList}. *//*
	public void setStandardDocumentList(List<Document> standardDocumentList) {
		this.standardDocumentList = standardDocumentList;
	}
*/
	/**See {@link #documentChainId}. */
	public Integer getDocumentChainId() {
		return documentChainId;
	}
	/**See {@link #documentChainId}. */
	public void setDocumentChainId(Integer documentChainId) {
		this.documentChainId = documentChainId;
	}

}
