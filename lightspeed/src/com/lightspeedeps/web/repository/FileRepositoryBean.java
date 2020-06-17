package com.lightspeedeps.web.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;

import com.lightspeedeps.dao.ContentDAO;
import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.FolderDAO;
import com.lightspeedeps.model.Content;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.XfdfContent;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupInputBean;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the File Repository page.
 */
@ManagedBean
@ViewScoped
public class FileRepositoryBean extends View implements Serializable, RepositoryBean {
	/** */
	private static final long serialVersionUID = - 1307755517434773324L;

	private static final Log log = LogFactory.getLog(FileRepositoryBean.class);

	/** root node label, used to insure that it can't be deleted. */
	private static final String ROOT_NODE_TEXT = "Central Repository";

	/** "Rename" action code for popup confirmation/prompt dialog. */
	private static final int ACT_RENAME = 10;
	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_CREATE = 11;
	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 12;

	/** The currently selected Folder. */
	private Folder folder;
	/** The currently selected Document. */
	private Document document;

	/** True if the currently selected item is a Folder. */
	private boolean showfolder;
	/** True if the currently selected item is a Document. */
	private boolean showdocument;

	/** The filename entered (in the input field) by the user. */
	private String userFileName;

	/** True if the "make private" checkbox on the page is checked. */
	private Boolean makePrivate;

	/** Tree 'default model', used in the JSP by the ice:tree tag, to
	 * generate the entire tree display. */
	private DefaultTreeModel model;


	/** The currently selected "node" in the tree; set when the user clicks on a
	 * tree item. This object reference is used to delete and copy the node, or
	 * to insert new nodes beneath it.*/
	private DynamicNodeUserObject selectedNodeObject = null;

	/** The data field for the 'inputFile' control (for uploads). */
	private String filepath;

	private transient FolderDAO folderDAO;

	/* Constructor */
	public FileRepositoryBean() {
		super("File.");
		log.debug("Initializing FileRepositoryBean instance");
		init();
	}

	public static FileRepositoryBean getInstance() {
		return (FileRepositoryBean)ServiceFinder.findBean("fileRepositoryBean");
	}

	private void init() {
		showfolder = false;
		showdocument = false;
		try {
			// create root node with its children expanded
			DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
			DynamicNodeUserObject rootObject = new DynamicNodeUserObject(rootTreeNode, this);
			folder = FileRepositoryUtils.getRoot(); // The root of the repository.
			if (folder != null) { // should never be null!
				rootObject.setText(folder.getName());
				rootObject.setRowIndex(folder.getId());
				rootTreeNode.setUserObject(rootObject);
				// 'model' is accessed by the JSP ice:tree component, as root of tree
				model = new DefaultTreeModel(rootTreeNode);
				refreshTree();
			}
		}
		catch (Exception e) {
			EventUtils.logError("FileRepository failed Exception: ", e);
		}
	}

	private void refreshTree() {
//		if (log.isDebugEnabled()) {
//			for (Folder f : getFolderDAO().findAll()) {
//				log.debug("Folder: " + f.getName() + ", id: " + f.getId());
//			}
//		}
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> enumTree = root.postorderEnumeration();
		List<DefaultMutableTreeNode> list = new ArrayList<>();
		while( enumTree.hasMoreElements() ) {
			list.add(enumTree.nextElement());
		}
		for (DefaultMutableTreeNode node : list) {
			node.removeFromParent();	// remove from tree
	//		node.setUserObject(null);	// release reference
		}
		list.clear();

		folder = FileRepositoryUtils.getRoot();
		if (null != folder.getFolders()) {
			traverseChilds(this, folder, root, 1);
		}
	}

	/**
	 * Add all the document children of the given folder to the branchNode, and
	 * create sub-branches for all the folder children of the given folder, then
	 * recursively call itself to add the children of the sub-folders.
	 * @param repositoryBean
	 *
	 * @param folder The folder whose descendants are to be added to the tree.
	 * @param branchNode The node to which the documents and sub-folders will be
	 *            added.
	 */
	private static void traverseChilds(RepositoryBean repositoryBean, Folder folder, DefaultMutableTreeNode branchNode, int depth) {
		if (null != folder.getDocuments()) {
			for (Document document : folder.getDocuments())
				addDocument(repositoryBean, document, branchNode);
		}

		for (Folder subFolder : folder.getFolders()) {
			DefaultMutableTreeNode subBranchNode = new DefaultMutableTreeNode();
			DynamicNodeUserObject subBranchObject = new DynamicNodeUserObject(subBranchNode, repositoryBean);
			subBranchObject.setText(subFolder.getName());
			subBranchObject.setRowIndex(subFolder.getId());
			subBranchNode.setUserObject(subBranchObject);
			subBranchObject.setExpanded(depth < 2);
			branchNode.add(subBranchNode);
			traverseChilds(repositoryBean, subFolder, subBranchNode, depth+1);
		}
	}


	/**
	 * Action method for the "create folder" button.
	 * @return null navigation string.
	 */
	public String actionCreateFolder() {
		if (selectedNodeObject != null && ! selectedNodeObject.isLeaf()) {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			inputBean.show( this, ACT_CREATE,
					"FileRepository.NewFolder.");
			inputBean.setInput("New Folder");
			inputBean.setMaxLength(Folder.MAX_NAME_LENGTH);
		}
		else {
			MsgUtils.addFacesMessage("FileRepository.SelectFolder", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on the "create folder" pop-up dialog.
	 */
	private String actionCreateFolderOk() {
		try {
			if (selectedNodeObject != null) {
				if (! userFileName.equals("")) {
					if (selectedNodeObject.isLeaf()) {
						MsgUtils.addFacesMessage("FileRepository.SelectFolder", FacesMessage.SEVERITY_ERROR);
					}
					else if (!selectedNodeObject.isLeaf()) {
						getFolderDAO().createFolder(userFileName, selectedNodeObject.getRowIndex(), makePrivate);
						userFileName = "";
						refreshTree();
					}
				}
				else {
					MsgUtils.addFacesMessage("FileRepository.MissingName", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("FileRepository createSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionDelete() {
		if (selectedNodeObject != null && !selectedNodeObject.getText().equals(ROOT_NODE_TEXT)) {
			// can't delete the root node; this check is a failsafe in case
			// the delete method is somehow activated despite the button being disabled.
			PopupBean.getInstance().show(this, ACT_DELETE, "FileRepository.Delete.");
		}
		return null;
	}

	/**
	 * Action method for the Delete button.
	 * Deletes the selected tree node. The node object reference is set to null
	 * so that the delete and copy buttons will be disabled.
	 *
	 * @see #getDeleteDisabled()
	 * @return null navigation string.
	 */
	public String actionDeleteOk() {
		try {
			if (selectedNodeObject != null && !selectedNodeObject.getText().equals(ROOT_NODE_TEXT)) {
				// can't delete the root node; this check is a failsafe in case
				// the delete method is somehow activated despite the button being disabled.
				if (selectedNodeObject.isLeaf()) {
					DocumentDAO.getInstance().deleteDocument(selectedNodeObject.getRowIndex());
				}
				else {
					getFolderDAO().deleteFolder(selectedNodeObject.getRowIndex());
				}
				selectedNodeObject.deleteNode();
				selectedNodeObject = null;
				refreshTree();
			}
		}
		catch (Exception e) {
			EventUtils.logError("FileRepository deleteSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		showdocument = false;
		showfolder = false;
		return null;
	}

	/**
	 * Action method for the Rename button
	 * @return null navigation string.
	 */
	public String actionRename() {
		try {
			if (selectedNodeObject != null && !selectedNodeObject.getText().equals(ROOT_NODE_TEXT)) {
				// can't rename the root node; this check is a failsafe in case
				// the rename method is somehow activated despite the button being disabled.
				PopupInputBean inputBean = PopupInputBean.getInstance();
				inputBean.show( this, ACT_RENAME,
						"FileRepository.Rename.");
				inputBean.setInput(selectedNodeObject.getText());
				inputBean.setMaxLength(Folder.MAX_NAME_LENGTH);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user hits OK on the Rename prompt dialog.
	 * Rename the selected tree node. The node object reference is set to null
	 * so that the delete and rename buttons will be disabled.
	 */
	private String actionRenameOk() {
		try {
			if (selectedNodeObject != null && !selectedNodeObject.getText().equals(ROOT_NODE_TEXT)) {
				// can't rename the root node; this check is a failsafe in case
				// the rename method is somehow activated despite the button being disabled.
				if (selectedNodeObject != null && !userFileName.equals("")) {
					if (selectedNodeObject.isLeaf()) {
						document = DocumentDAO.getInstance().findById(selectedNodeObject.getRowIndex());
						document.setName(userFileName);
						document.setUpdated(new Date());
						DocumentDAO.getInstance().attachDirty(document);
					}
					else if (!selectedNodeObject.isLeaf()) {
						folder = getFolderDAO().findById(selectedNodeObject.getRowIndex());
						folder.setName(userFileName);
						getFolderDAO().attachDirty(folder);
					}
					userFileName = "";
					refreshTree();
				}
				else {
					MsgUtils.addFacesMessage("FileRepository.MissingRename", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("FileRepository renameSelectedNode failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the Send button.
	 * @return null navigation string
	 */
	public String actionSend() {
		log.warn("SEND DOCUMENT NOT IMPLEMENTED");
		return null;
	}

	/**
	 * Download the currently selected document.  This uses our FileDownloadServlet support.
	 * @return null navigation string
	 */
	public String actionDownloadDocument() {
		Random random = new Random();
		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();
			ServletContext sc = (ServletContext)externalContext.getContext();
			int randomNumber = Math.abs(random.nextInt());
			String filename = "version" + randomNumber + document.getType();
			FileOutputStream fos = new FileOutputStream(sc.getRealPath(Constants.ARCHIVE_RETRIEVAL_FOLDER)
					+ File.separator + filename);
			fos.write(document.getContent());
			fos.flush();
			fos.close();
			String javascriptCode = "window.open('../../servlet/FileDownloadServlet?fileName="
					+ filename + "','Version');";
			addJavascript(javascriptCode);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionViewDocument() {
		Random random = new Random();
		try {
			if (document.getContent().length > 0) {
				DateFormat df = new SimpleDateFormat("ddHHmmssSSS");
				String timestamp = df.format(new Date());
				FacesContext facesContext = FacesContext.getCurrentInstance();
				ExternalContext externalContext = facesContext.getExternalContext();
				ServletContext sc = (ServletContext)externalContext.getContext();
				int randomNumber = Math.abs(random.nextInt());
				String filename = "version" + randomNumber + document.getType();
				FileOutputStream fos = new FileOutputStream(sc.getRealPath(Constants.ARCHIVE_RETRIEVAL_FOLDER)
						+ File.separator + filename);
				fos.write(document.getContent());
				fos.flush();
				fos.close();

				String javascriptCode = "reportOpen('../../"
						+ Constants.ARCHIVE_RETRIEVAL_FOLDER + "/" + filename
						+ "','LSrep" + timestamp + "');";
				addJavascript(javascriptCode);
			}
			else {
				MsgUtils.addFacesMessage("FileRepository.NoContent", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * fileEntryListener for the File Upload button/event.
	 * @param event
	 */
	public void listenUploadFile(FileEntryEvent event) {

		if (selectedNodeObject == null) {
			folder = FileRepositoryUtils.getRoot();// getFolderDAO().findById(rootFolderId);
			log.debug("Root Folder Id " + folder.getId());
		}
		folder = getFolderDAO().refresh(folder); // refresh object
		FileEntry inputFileComponent = (FileEntry)event.getSource();
	    FileEntryResults results = inputFileComponent.getResults();
		FileInfo info = results.getFiles().get(0);

		String fileName = info.getFileName();
		if (!fileName.equals("")) {
			File file = new File(info.getFile().getAbsolutePath());
			String contentType = info.getContentType();
			FileRepositoryUtils.storeFile(folder, fileName, null, file, null, contentType,
					new Date(), getMakePrivate());
		}
		else {
			MsgUtils.addFacesMessage("FileRepository.MissingFileName", FacesMessage.SEVERITY_ERROR);
		}
		showfolder = false;
		showdocument = true;
		refreshTree();
	}

	private static void addDocument(RepositoryBean repositoryBean, Document document, DefaultMutableTreeNode branchNode) {
		try {
			int userid = SessionUtils.getCurrentUser().getId().intValue();
			if (userid == document.getUser().getId().intValue() || !document.getPrivate()) {
				DefaultMutableTreeNode docBranchNode = new DefaultMutableTreeNode();
				DynamicNodeUserObject docBranchObject = new DynamicNodeUserObject(docBranchNode,
						repositoryBean);
				docBranchObject.setText(document.getName());
				docBranchObject.setRowIndex(document.getId());
				docBranchObject.setLeaf(true);
				docBranchNode.setUserObject(docBranchObject);
				branchNode.add(docBranchNode);
			}
		}
		catch (Exception e) {
			EventUtils.logError("FileRepository addDocument failed Exception: ", e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.repository.RepositoryBean#showFolderDetails(java.lang.Integer)
	 */
	@Override
	public void showFolderDetails(Integer folderID) {
		log.debug("Folder Id >>>>" + folderID);
		folder = getFolderDAO().findById(folderID);
		makePrivate = folder.getPrivate();
		showfolder = true;
		showdocument = false;
	}

	/**
	 * @see com.lightspeedeps.web.repository.RepositoryBean#showDocumentDetails(java.lang.Integer)
	 */
	@Override
	public void showDocumentDetails(Integer documentID) {
		log.debug("Docuument Id >>>>>" + documentID);
		document = DocumentDAO.getInstance().findById(documentID);
		folder = document.getFolder();
		makePrivate = document.getPrivate();
		showdocument = true;
		showfolder = false;
	}

	/**
	 * Determines whether the delete button is disabled. The delete button
	 * should be disabled if the node that was previously selected was deleted
	 * or if no node is otherwise selected. The root node is a special case and
	 * cannot be deleted.
	 *
	 * @return True if the delete button should be disabled.
	 */
	public boolean getDeleteDisabled() {
		// can't delete the root node
		return selectedNodeObject == null || selectedNodeObject.getText().equals(ROOT_NODE_TEXT);
	}

	/**
	 * Determines whether the copy button is disabled. This should only occur
	 * when there is no node selected, which occurs at initialization and when a
	 * node is deleted.
	 *
	 * @return True if the copy button should be disabled.
	 */
	public boolean getCopyDisabled() {
		return selectedNodeObject == null;
	}


	@Override
	public String confirmOk(int action) {
		String res = null;
		userFileName = PopupInputBean.getInstance().getInput().trim();
		switch(action) {
			case ACT_RENAME:
				res = actionRenameOk();
				break;
			case ACT_CREATE:
				res = actionCreateFolderOk();
				break;
			case ACT_DELETE:
				res = actionDeleteOk();
				break;
		}
		return res;
	}

	/** See {@link #model}. */
	public DefaultTreeModel getModel() {
		return model;
	}
	/** See {@link #model}. */
	public void setModel(DefaultTreeModel model) {
		this.model = model;
	}

	/** See {@link #folder}. */
	public Folder getFolder() {
		return folder;
	}
	/** See {@link #folder}. */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	/** See {@link #document}. */
	public Document getDocument() {
		return document;
	}
	/** See {@link #document}. */
	public void setDocument(Document document) {
		this.document = document;
	}

	/** See {@link #showfolder}. */
	public boolean isShowfolder() {
		return showfolder;
	}
	/** See {@link #showfolder}. */
	public void setShowfolder(boolean showfolder) {
		this.showfolder = showfolder;
	}

	/** See {@link #showdocument}. */
	public boolean isShowdocument() {
		return showdocument;
	}
	/** See {@link #showdocument}. */
	public void setShowdocument(boolean showdocument) {
		this.showdocument = showdocument;
	}

	public int getDocumentLength() {
		return document.getContent().length;
	}

	public String getXfdfLength() {
		String ret = "";
		Content content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
		if (content == null) {
			ret = "(Content not found)";
		}
		else if (content.getXfdfData() == null) {
			ret = "(Null)";
		}
		else if (XfdfContent.EMPTY_XFDF.equals(content.getXfdfData())) {
			ret = "Empty ('" + XfdfContent.EMPTY_XFDF + "')";
		}
		else {
			ret = "" + content.getXfdfData().length();
		}
		return ret;
	}

	public String getXodLength() {
		String ret = "";
		Content content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
		if (content == null) {
			ret = "(None)";
		}
		else if (Arrays.equals(content.getXodContent(),Content.EMPTY_XOD.getBytes())) {
			ret = "Empty ('" + Content.EMPTY_XOD + "')";
		}
		else {
			ret = "" + content.getXodContent().length;
		}
		return ret;
	}

	/**
	 * @see com.lightspeedeps.web.repository.RepositoryBean#getSelectedNodeObject()
	 */
	@Override
	public DynamicNodeUserObject getSelectedNodeObject() {
		return selectedNodeObject;
	}
	/** See {@link #selectedNodeObject}. */
	@Override
	public void setSelectedNodeObject(DynamicNodeUserObject selectedNodeObject) {
		this.selectedNodeObject = selectedNodeObject;
	}

	/** See {@link #userFileName}. */
	@Override
	public String getUserFileName() {
		return userFileName;
	}
	/** See {@link #userFileName}. */
	public void setUserFileName(String userFileName) {
		this.userFileName = userFileName;
	}

	/** See {@link #makePrivate}. */
	public Boolean getMakePrivate() {
		return makePrivate;
	}
	/** See {@link #makePrivate}. */
	public void setMakePrivate(Boolean makePrivate) {
		this.makePrivate = makePrivate;
	}

	/** See {@link #filepath}. */
	public String getFilepath() {
		return filepath;
	}
	/** See {@link #filepath}. */
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	private FolderDAO getFolderDAO() {
		if (folderDAO == null) {
			folderDAO = FolderDAO.getInstance();
		}
		return folderDAO;
	}

}
