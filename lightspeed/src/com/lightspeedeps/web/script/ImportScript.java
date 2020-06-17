//	File Name:	ImportScript.java
package com.lightspeedeps.web.script;

import java.io.*;
import java.util.EventObject;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;

import com.lightspeedeps.dao.ColorNameDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.importer.ImportFile;
import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.ListView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class for backing the "New Revision/Import Script" web page.
 * This is session-scoped!
 */
@ManagedBean
@SessionScoped
public class ImportScript implements Serializable {

	/** for serialization */
	private static final long serialVersionUID = 2706920012121075085L;

	private static final Log log = LogFactory.getLog(ImportScript.class);

	@SuppressWarnings("unused")
	private static final String IMPORT_METHOD_MANUAL = "manual";
	private static final String IMPORT_METHOD_AUTO = "auto";

	private enum FileType {
		PDF,
		FDX,
		SEX,
		TAGGER,
		FDR7,	// Final Draft v7 - not supported, but special message
		UNKNOWN
	}

	private boolean includeText = true;
	private boolean includeSceneElements = true;
	private transient ImportFile importer;

	/** This field is bound to the ICEfaces InputFile component on the
	 * Add Revision pop-up dialog. */
	private transient FileEntry inputFile;

	/** The java File object that is passed to us after the upload
	 * servlet has finished uploading the user's file to the server. */
	private transient File importFile;

	/** A duplicate copy of 'importFile'. Is this necessary? */
	private transient File file;

	/** The absolute (complete) path of the file created on the server by
	 * the upload servlet. This is extracted from the importFile object. */
	private String importFileName;

	/** The unqualified file name of the file created on the server, which
	 * should be identical to the file name selected by the user. It is only the
	 * final part of the file name, without any directory/path information. */
	private String displayFilename = "";

	/** The backing field for the user input of the script title/description. */
	private String description;

	/** User input field for script color selection. This is the backing field
	 * for the color drop-down selection list. */
	private ColorName colorName;

	/** A field displayed at the bottom of the "Add Revision" dialog pop-up. */
	private String uploadStatus;

	/** */
	private String fileContentType;

	/** A field displayed at the bottom of the "Add Revision" dialog pop-up. */
	private String messageText;

	/** */
	private String importMethod = IMPORT_METHOD_AUTO;
	private boolean importOk = false;
	private boolean importComplete = false;
	private boolean importAllowed = false;
	private boolean manualAllowed = true;
//	private boolean initialized = false;
	private boolean firstScript = false;

	/** A field displayed, if not null, in an error box at the top of the
	 * "Add Revision" dialog pop-up. */
	private String errorMessage;

	/** Percent progress (0-100) of file being uploaded. */
	private int uploadProgress;


	public ImportScript() {
		log.debug("this="+this);
		init();
	}

	public static ImportScript getInstance() {
		return (ImportScript)ServiceFinder.findBean("importScript");
	}

	/**
	 * Used by external caller(s) to initialize this bean for a new import. This
	 * should only necessary if this bean is defined with session scope instead
	 * of request scope.
	 */
	public void reset() {
		init();
	}

	/**
	 * Release (set to null) all our object fields.
	 */
	private void release() {
		importer = null;
		errorMessage = null;
		file = null;
		inputFile = null;
		importFile = null;
		uploadStatus = null;
		colorName = null;

		// Strings...
		displayFilename = "";
		importFileName = null;
		fileContentType = null;
		description = null;
		messageText = null;
		importMethod = null;
		errorMessage = null;
	}

	private void init() {
		log.debug("");
		// Since we're session-scope, we clear out these member variables in case this
		// is a return to the import page after a previous import finished.
//		initialized = true;
		release();
		importOk = false;			// this will suppress "Next" button if we return to this page
		importComplete = false;
		importAllowed = false;	// and indicate we need to do a new upload to re-import
		uploadStatus = null;
		displayFilename = "";
		importFileName = "";
		fileContentType = "";
		description = "";
		messageText = "";
		uploadProgress = 0;
		importMethod = IMPORT_METHOD_AUTO;

		Project project = SessionUtils.getCurrentProject();
//		log.debug("project="+project);

		setFirstScript(project.getScript() == null);
		manualAllowed = firstScript;

		// Find the color associated with the latest script, and figure out
		// the next color in sequence to display as the default.
		ScriptDAO scriptDAO = ScriptDAO.getInstance();
		int rev = scriptDAO.findMaxScriptRevision(project);
		log.debug("rev="+rev);
		if (rev > 0) { // should be a matching script!
			Script priorScript = scriptDAO.findByRevisionAndProject(rev, project);
			if (priorScript != null) {
				// Use the rev# associated with the color assigned to this
				// script revision; the user may have changed the color.
				rev = priorScript.getColorName().getScriptRevision();
				log.debug("new rev="+rev);
			}
		}
		setColorName(ColorNameDAO.getInstance().getColorByScriptRevision(rev+1));
	}

	/**
	 * Called from ScriptDraftsBean after the user clicks the "Continue" button
	 * on the "Add Revision" pop-up dialog.  Directs processing to either the
	 * manual or automatic methods.
	 *
	 * @return navigation string; null if the dialog box is still to be
	 *         displayed
	 */
	public String actionImport() {
		String ret;
		errorMessage = null;
		if (importMethod.equals(IMPORT_METHOD_AUTO)) {
			ret = actionAutoImport();
		}
		else {
			ret = actionManualImport();
		}
		if (errorMessage != null) {
			showPopupErrors();
			importAllowed = false;
			uploadProgress = 0;
			uploadStatus = null;
		}
		return ret;
	}

	/**
	 * Process an automatic (file) import. The filename extension is currently
	 * used as the first step to determine the type of import. If that doesn't
	 * work, read the first line (or two) of the file and attempt to decide from
	 * that. If the type is determined, an instance of the appropriate import
	 * class is acquired and invoked; otherwise, set an error message and return
	 * immediately.
	 *
	 * Returns navigation string to new revision page if the import was successful, null if not.
	 */
	private String actionAutoImport() {
		log.debug("file="+file);
		log.debug("importFileName="+importFileName);
		String filename = importFileName;
		log.info("ImportScript.autoImport started");
		log.info("file=" + filename + ", desc=" + description
				+ ", color=" + colorName
				+ ", includeText=" + includeText
				+ ", includeSceneElements=" + includeSceneElements);
		boolean bRet = false;

		if (filename == null || filename.length()==0) {
			errorMessage = MsgUtils.formatMessage("ImportScript.UploadFirst");
			return null;
		}
		if ( ! checkDescription()) {
			return null;
		}

		FileType type = findFileType(filename, fileContentType);
		switch(type) {
		case FDX:
			importer = (ImportFile)ServiceFinder.findBean("importFdx");
			break;
		case SEX:
			importer = (ImportFile)ServiceFinder.findBean("importSex");
			if (getIncludeText()) {
				messageText = MsgUtils.getMessage("ImportScript.NoTextInSex") + Constants.NEW_LINE;
			}
			break;
		case PDF:
			importer = (ImportFile)ServiceFinder.findBean("importPdf");
			break;
		case TAGGER:
			importer = (ImportFile)ServiceFinder.findBean("importTagger");
			break;
		case FDR7:
			errorMessage = MsgUtils.formatMessage("ImportScript.FinalDraft7");
			return null;
		case UNKNOWN:
			errorMessage = MsgUtils.formatMessage("ImportScript.NotRecognizedType");
			return null;
		}

		try {
			log.info("importer=" + importer);
			// Perform the import process!
			bRet = importer.importFile(filename,
					description, SessionUtils.getCurrentProject(), colorName, includeText, includeSceneElements);
		}
		catch (Exception e) {
			bRet = false;
			EventUtils.logError(e);
		}
		try {
			messageText += importer.getMessageLog();
			if (bRet) {
				importer.assignCastIds();
			}
		}
		catch (Exception e) {
			bRet = false;
			EventUtils.logError(e);
		}
		setImportComplete(true);	// processed finished, either successfully or not.
		setImportOk(bRet);			// show or hide the "Next step" button
		setManualAllowed( ! bRet);	// if it worked, disable Manual import button
		importAllowed = false;		// disable the Import button
		log.info("import file returned " + bRet);
		return HeaderViewBean.SCRIPT_MENU_IMPORT;
	}

	/**
	 * Determine the type of import file being processed, based on the filename
	 * and/or the text in the "file content type" string.
	 *
	 * @param filename The name of the import file.
	 * @param fileType the FileContentType (determined by the upload servlet).
	 * @return One of the FileType enumerated values.
	 */
	private FileType findFileType(String filename, String fileType) {
		FileType type = FileType.UNKNOWN;

		String lowFilename = filename.toLowerCase();
		int ix = lowFilename.lastIndexOf('.');
		String extension = "";
		if (ix > 0) {
			extension = lowFilename.substring(ix+1);
			if (extension.equals("pdf")) {
				type = FileType.PDF;
			}
			else if (extension.equals("fdx")) {
				type = FileType.FDX;
			}
			else if (extension.equals("xml")) {
				type = FileType.TAGGER;
			}
			else if (extension.equals("sex")) {
				type = FileType.SEX;
			}
		}
		if (type == FileType.UNKNOWN) {
			if (lowFilename.indexOf(".fdx") > 0) {
				type = FileType.FDX;
			}
			if (lowFilename.indexOf(".pdf") > 0 ||
					fileType.indexOf("pdf") >= 0) {
				type = FileType.PDF;
			}
			else if (lowFilename.indexOf(".sex") > 0 ) {
				type = FileType.SEX;
			}
			else {
				try {
					Reader file = new FileReader(filename);
					BufferedReader rdr = new BufferedReader(file);
					String line = rdr.readLine();
					line = line.trim();
					if (line.startsWith("<")) {
						type = FileType.TAGGER;
						line = rdr.readLine();
						if (line.startsWith("<FinalDraft")) {
							type = FileType.FDX;
						}
					}
					else if (line.startsWith("SSI*")) {
						type = FileType.SEX;
					}
					else if (extension.equals("fdr") ||
							line.contains("Final Draft")) {
						type = FileType.FDR7;
					}
					rdr.close();
				}
				catch (FileNotFoundException e) {
					log.debug(e.getMessage());
				}
				catch (IOException e) {
					log.debug(e.getMessage());
				}
			}
		}

		return type;
	}

	/**
	 * Called by the ice:inputFile control after the file has been uploaded from
	 * the client to the server, due to the file= attribute on the control. The
	 * FileEntryEvent object passed describes the file created on the server and
	 * the status of the upload.
	 *
	 * @param evt The FileEntryEvent created by ICEfaces, which describes the
	 *            file created on the server as a result of uploading from the
	 *            client.
	 */
	public void listenUpload(FileEntryEvent evt) {
		log.debug("");
		FileEntry inputFile = (FileEntry)evt.getSource();
	    FileEntryResults results = inputFile.getResults();
		FileInfo fileInfo = results.getFiles().get(0);
		fileContentType = fileInfo.getContentType();
		log.debug("file=" + fileInfo.getFileName() + ", status=" + fileInfo.getStatus() + ", type=" + fileContentType);
		String messageId = null;
		if (! fileInfo.isSaved()) {
			// upload failed, generate custom messages
			messageId = FileLoadBean.findErrorId(fileInfo);
//			errorMessage = MsgUtils.formatMessage("ImportScript.UploadFailed", fileInfo.getFileName());
			errorMessage = MsgUtils.formatMessage(messageId, fileInfo.getFileName());
		}
		else {
			importFile = fileInfo.getFile();
			file = importFile;	// copy so form get/post doesn't overlay it
			importFileName = importFile.getAbsolutePath().trim();
			log.debug("file loc="+importFileName);
			displayFilename = importFile.getName();
			importAllowed = true;
			errorMessage = null;
			if (description == null || description.length() == 0) {
				setDefaultDescription();
			}
			//errorMessage = MsgUtils.formatMessage("ImportScript.FileUploaded", importFile.getName());
		}
	}

	/**
	 * Called by the jsp page when the user clicks the "Next" button
	 * to proceed to the next step.  The button is only displayed after the import
	 * process has completed, when the results of the import are displayed.
	 *
	 * @return "step4" if this is the first script loaded for the project; otherwise "step2".
	 */
	public String actionNext() {
//		initialized = false;		// force re-initialization if we return to this page
		String next;
		if (importOk) {
			next = "step2";
			if (getFirstScript()) {		// if this is the first script for this project,
				next = "step4";			// jump straight to step 4 (final review)
			}
			release();	// release all our resources
		}
		else {
			next = HeaderViewBean.SCRIPT_MENU_DRAFTS; // return to Script Revisions page
		}
		return next;
	}

	/**
	 * The action method for the "too big" button on the Add Revision dialog
	 * box. This button is "clicked" by the JavaScript code that detects a file
	 * exceeding our maximum allowed size.  This displays the appropriate error
	 * message, and resets our fields to the same state as if an upload failed.
	 *
	 * @return null navigation string
	 */
	public String actionFileTooBig() {
		log.debug("");
		errorMessage = MsgUtils.getMessage("ImportScript.UploadFailed.TooBig");
		importAllowed = false;
		uploadProgress = 0;
		uploadStatus = null;
		showPopupErrors();
		return null;
	}

	/**
	 * Method called after the user clicked Continue and had selected
	 * "manual entry" for the import method. This does some minimal parameter
	 * checking, then sets up the manual import class instance and invokes it.
	 *
	 * @return null if the import should not proceed, or a navigation
	 *         string for the Script Breakdown page, which will be presented
	 *         with the first scene started.
	 */
	private String actionManualImport() {
		log.debug("");
		boolean bRet = true;
		String res = null;

		bRet = checkDescription();
		if (bRet) {
			ImportFile importer;
			try {
				importer = (ImportFile)ServiceFinder.findBean("importManualSetup");
				log.info("importer=" + importer);
				// Perform the import process!
				bRet = importer.importFile(importFileName,
						description, SessionUtils.getCurrentProject(), colorName, includeText, includeSceneElements);
				if (bRet) {
					res = HeaderViewBean.SCRIPT_MENU_BREAKDOWN;
					MsgUtils.addFacesMessage( "ImportScript.ManualOk", FacesMessage.SEVERITY_INFO);
				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			if (bRet) {
//				initialized = false;		// force re-initialization if we return to this page
			}
		}
		setImportOk(false);			// hide the "Next step" button
		log.info("manual import file returned " + bRet);
		return res;
	}

	/**
	 * Validate that the description field is non-blank.  Issues an error message
	 * if it is blank.
	 * @return True iff description is valid
	 */
	public boolean checkDescription() {
		boolean bRet = false;
		setDescription(description.trim());
		if (description.length() > 0) {
//			errorMessage = null;
			bRet = true;
		}
		else {
			errorMessage = MsgUtils.getMessage("ImportScript.EnterName");
		}
		return bRet;
	}

	/**
	 * Called via ICEfaces framework based on the actionListener= parameter of
	 * the inputFile component. Called when the upload has finished or an error
	 * occurs. Sometimes it is called multiple times on a successful upload!?
	 * (Called 4 times during a test of a 250MB file.) Our current usage of this
	 * method is not required, but it provides an additional text status string
	 * that may be included on the import page.
	 *
	 * @param actionEvent
	 */
	public void uploadActionListener(ActionEvent actionEvent) {
		FileEntry inputFile = (FileEntry)actionEvent.getSource();
	    FileEntryResults results = inputFile.getResults();
		FileInfo fileInfo = results.getFiles().get(0);
		String contentType = fileInfo.getContentType();
		log.debug("status=" + fileInfo.getStatus() + ", type=" + contentType);

		if (fileInfo.isSaved()) {
			if (uploadStatus == null) { // only do this once per file
				//errorMessage = MsgUtils.formatMessage("ImportScript.FileType", contentType);
			}
			uploadProgress = 100;
			uploadStatus = "File uploaded successfully.";
			if (description == null || description.length() == 0) {
				setDefaultDescription();
			}
		}

		if (! fileInfo.isSaved()) {
			String msgid = "ImportScript.UploadFailed.InvalidStatus";
			// upload failed, generate custom messages
//			switch (fileInfo.getStatus()) {
//			case FileInfo.INVALID:
//				msgid = "ImportScript.UploadFailed.InvalidStatus"; // Invalid file status after upload.
//				break;
//			case FileInfo.SIZE_LIMIT_EXCEEDED:
//				msgid = "ImportScript.UploadFailed.TooBig"; // Size limit exceeded.
//				break;
//			case FileInfo.INVALID_CONTENT_TYPE:
//				msgid = "ImportScript.UploadFailed.InvalidContent"; // Invalid content type
//				break;
//			case FileInfo.INVALID_NAME_PATTERN:
//				msgid = "ImportScript.UploadFailed.InvalidPattern"; // Invalid name pattern
//				break;
//			case FileInfo.UNSPECIFIED_NAME:
//				msgid = "ImportScript.UploadFailed.MissingFilename"; // No file specified.
//				break;
//			case FileInfo.UNKNOWN_SIZE:
//				msgid = "ImportScript.UploadFailed.UnknownSize";
//				break;
//			}
			uploadStatus = MsgUtils.getMessage(msgid);
			errorMessage = uploadStatus;	// in case checkFile() isn't called!
			importAllowed = false;
		}
		showPopupErrors();
	}

	/**
	 * This ProgressListener method is bound to the inputFile component and is
	 * called multiple times during the file upload process. Every call allows
	 * us to get the percentage of the file that has been uploaded. This
	 * progress information can then be used with a ice:outputProgress component
	 * to provide user feedback.
	 * <p>
	 * Testing (10/31/2010) indicates that it is called at either 1 second
	 * intervals, or 10% intervals, whichever comes later (that is, not more
	 * than once per second); and the reported percentage is always a multiple
	 * of 10. It is called one final time with a percentage of 100 when the
	 * upload has completed.
	 *
	 * @param event holds a InputFile object in its source which can be probed
	 *            for the file upload percentage complete, as a value from
	 *            0-100.
	 */
	public void progressListener(EventObject event) {
//		FileEntry ifile = (FileEntry) event.getSource();
//		FileEntryResults results = inputFile.getResults();
//		FileInfo info = results.getFiles().get(0);
		uploadProgress = 50; // ifile.getFileInfo().getPercent();
		log.debug("progress=" + uploadProgress);
	}

	private void showPopupErrors() {
		ListView.addJavascript("showPopErrors();");
	}

	/** Returns the percentage, from 0-100, of the upload that has completed. */
	public int getUploadProgress() {
		//log.debug(""+uploadProgress);
		return uploadProgress;
	}

	public String getImportFileName() {
		log.debug("file="+importFileName);
		return importFileName;
	}
	public void setImportFileName(String name) {
		importFileName = name;
		log.debug("file="+importFileName);
	}

	public ImportFile getImporter() {
		return importer;
	}
	public void setImporter(ImportFile importer) {
		this.importer = importer;
	}

	public boolean getIncludeText() {
		return includeText;
	}
	public void setIncludeText(boolean includeText) {
		this.includeText = includeText;
	}

	public boolean getIncludeSceneElements() {
		return includeSceneElements;
	}
	public void setIncludeSceneElements(boolean includeSceneElements) {
		this.includeSceneElements = includeSceneElements;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String title) {
		description = title;
		if (description == null || description.length() == 0) {
			setDefaultDescription();
		}
	}

	private void setDefaultDescription() {
		if (displayFilename != null && displayFilename.length() > 0) {
			description = displayFilename;
			if (description.length() > Script.MAX_TITLE_LENGTH) {
				description = description.substring(0, Script.MAX_TITLE_LENGTH);
			}
		}
	}

	public List<SelectItem> getColorList() {
		return ScriptUtils.getColorNameList();
	}

	public ColorName getColorName() {
		return colorName;
	}
	public void setColorName(ColorName name) {
		colorName = name;
	}

	public boolean getImportOk() {
		return importOk;
	}
	public void setImportOk(boolean importOk) {
		this.importOk = importOk;
	}

	public boolean getImportComplete() {
		return importComplete;
	}
	public void setImportComplete(boolean importComplete) {
		this.importComplete = importComplete;
	}

	public boolean getImportAllowed() {
		return importAllowed;
	}

	/** See {@link #errorMessage}. */
	public String getErrorMessage() {
		return errorMessage;
	}
	/** See {@link #errorMessage}. */
	public void setErrorMessage(String error) {
		errorMessage = error;
	}

	public boolean getManualAllowed() {
		return manualAllowed;
	}
	public void setManualAllowed(boolean allowed) {
		manualAllowed = allowed;
	}

	/** See {@link #importMethod}. */
	public String getImportMethod() {
		return importMethod;
	}
	/** See {@link #importMethod}. */
	public void setImportMethod(String importMethod) {
		this.importMethod = importMethod;
	}

	public boolean getFirstScript() {
		return firstScript;
	}
	public void setFirstScript(boolean script) {
		firstScript = script;
	}

	public String getDisplayFilename() {
		return displayFilename;
	}
	public void setDisplayFilename(String name) {
		displayFilename = name;
	}

	public String getUploadStatus() {
		return uploadStatus;
	}
	public void setUploadStatus(String uploadStatus) {
		this.uploadStatus = uploadStatus;
	}

	public String getFileContentType() {
		return fileContentType;
	}
	public void setFileContentType(String contentType) {
		fileContentType = contentType;
	}

	public FileEntry getInputFile() {
		return inputFile;
	}
	public void setInputFile(FileEntry file) {
		log.debug("");
		inputFile = file;
	}

	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

}
