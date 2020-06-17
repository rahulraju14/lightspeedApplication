/*
 * File: FileLoadBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.File;
import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;
import org.icefaces.ace.component.fileentry.FileEntryStatuses;

import com.lightspeedeps.type.UploadSuccessStatus;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A generic file prompt bean for backing a "file load" dialog box. See
 * jsp/common/fileload.xhtml for the user interface. It also serves as a
 * superclass for our "image load" dialog box bean.
 */
@ManagedBean
@ViewScoped
public class FileLoadBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 4816230658721574630L;

	/** Logger */
	private static final Log LOG = LogFactory.getLog(FileLoadBean.class);

	/** The message id in our message resource file of the message to be
	 * generated when a file has been successfully uploaded. */
	private String addedMessageId = "Image.FileUploaded";

	/** The error message to be displayed in the pop-up dialog. */
	private String errorMessage;

	/** The File object created by the upload process and passed to us
	 * by the framework. */
	private File uploadFile;

	/** The file content type, extracted from the uploaded File. */
	protected String fileContentType;

	/** The absolute file name (including path) of the file created
	 * by the upload process.  This is extracted from the File passed
	 * to us. */
	private String serverFilename;

	/** The unqualified filename of the file selected by the user. */
	protected String displayFilename;

	/** Indicates if Cancel button was clicked; JSF (ICEfaces?) calls the file
	 * listenUpload method even when the Cancel button is clicked. */
	protected boolean cancelled;

	/** Default Constructor */
	public FileLoadBean() {
	}

	/**
	 * @return the current instance of this managed bean.
	 */
	public static FileLoadBean getInstance() {
		return (FileLoadBean)ServiceFinder.findBean("fileLoadBean");
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionOk()
	 */
	@Override
	public String actionOk() {
		setErrorMessage(null);
		return super.actionOk();
	}

	/**
	 * Action method for the Cancel button on the File Upload (or Import)
	 * dialog box.
	 *
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		setErrorMessage(null);
		cancelled = true;
		return super.actionCancel();
	}

	/**
	 * The fileEntryListener method for the ace:fileEntry control. This method is called for both
	 * successful and unsuccessful file loads.
	 *
	 * @param evt The FileEntryEvent created by the framework, which contains the FileEntry component.
	 */
	public void listenUpload(FileEntryEvent evt) {
		LOG.debug("");
		FileEntry inputFile = (FileEntry)evt.getSource();
		FileEntryResults results = inputFile.getResults();
		FileInfo fileInfo = results.getFiles().get(0);
		if (cancelled) {
			fileInfo.updateStatus(new UploadSuccessStatus("Image.FileUploadCancelled"), false);
			cancelled = false;
			return;
		}
		String messageId = null;
		if (fileInfo != null) {
			fileContentType = fileInfo.getContentType();
			LOG.debug("file=" + fileInfo.getFileName() + ", status=" + fileInfo.getStatus() + ", type=" + fileContentType);
			uploadFile = fileInfo.getFile();
			if (uploadFile != null) { // NOPMD by DHarm on 8/24/18 4:10 PM
				displayFilename = uploadFile.getName();
			}
			else {
				displayFilename = fileInfo.getFileName();
			}
			if (fileInfo.isSaved()) {
				boolean fileOk = false;
				if (uploadFile.exists()) {
					serverFilename = uploadFile.getAbsolutePath().trim();
					LOG.debug("file loc=" + serverFilename);
					fileOk = true;
				}
				messageId = processFile(fileInfo);
				if (fileOk && messageId == null) {
					fileInfo.updateStatus(new UploadSuccessStatus(addedMessageId), false);
					super.actionOk();
				}
				else if (fileOk && messageId.length() == 0) {
					setVisible(true);
					messageId = null; // don't post any message
				}
				else {
					uploadFile = null;
				}
			}
			else {
				// upload failed, generate custom messages
				messageId = findErrorId(fileInfo);
			}
		}
		if (messageId != null) {
			setErrorMessage(MsgUtils.formatMessage(messageId, displayFilename));
		}
	}

	/**
	 * Default processing for an uploaded file. Subclasses may override this to
	 * do additional testing or processing of the file.
	 *
	 * @param fileInfo The FileInfo instance constructed by the framework.
	 * @return A message-id (for the message resource file) if a message other
	 *         than the normal completion message should be generated. Null if
	 *         the normal completion message should be generated.
	 */
	protected String processFile(FileInfo fileInfo) {
		return null;
	}

	/**
	 * Determine the appropriate message to be issued based on the status
	 * information in the 'fileInfo' field, and return the corresponding
	 * messageId.
	 *
	 * @param fileInfo The FileInfo related to the upload operation in question.
	 * @return The messageId (for our message resource bundle) for a message
	 *         describing the status of the file upload operation.
	 */
	public static String findErrorId(FileInfo fileInfo) {
		String messageId = "Image.FileErrorInvalid";
		if (fileInfo.getStatus() == FileEntryStatuses.UPLOADING) {
			messageId = "Image.FileUploadInProgress";
		}
		else if (fileInfo.getStatus() == FileEntryStatuses.MAX_FILE_SIZE_EXCEEDED) {
			messageId = "Image.FileErrorSize";
		}
		else if (fileInfo.getStatus() == FileEntryStatuses.UNKNOWN_SIZE) {
			messageId = "Image.FileErrorUnknownSize";
		}
		else if (fileInfo.getStatus() == FileEntryStatuses.UNSPECIFIED_NAME) {
			messageId = "Image.FileErrorNoName";
		}
		else if (fileInfo.getStatus() == FileEntryStatuses.INVALID_CONTENT_TYPE) {
			messageId = "Image.FileErrorContent";
		}
		return messageId;
	}

	/**See {@link #addedMessageId}. */
	public String getAddedMessageId() {
		return addedMessageId;
	}
	/**See {@link #addedMessageId}. */
	public void setAddedMessageId(String addedMessageId) {
		this.addedMessageId = addedMessageId;
	}

	/** See {@link #errorMessage}. */
	public String getErrorMessage() {
		return errorMessage;
	}
	/** See {@link #errorMessage}. */
	public void setErrorMessage(String message) {
		errorMessage = message;
	}

	/**
	 * Action method used from JSP to clear any existing error
	 * message.
	 * @return null navigation string
	 */
	public String clearErrorMessage() {
		setErrorMessage(null);
		return null;
	}

	/**See {@link #uploadFile}. */
	public File getUploadFile() {
		return uploadFile;
	}
	/**See {@link #uploadFile}. */
	public void setUploadFile(File uploadFile) {
		this.uploadFile = uploadFile;
	}

	/**See {@link #fileContentType}. */
	public String getFileContentType() {
		return fileContentType;
	}
	/**See {@link #fileContentType}. */
	public void setFileContentType(String contentType) {
		fileContentType = contentType;
	}

	/**See {@link #displayFilename}. */
	public String getDisplayFilename() {
		return displayFilename;
	}
	/**See {@link #displayFilename}. */
	public void setDisplayFilename(String displayFilename) {
		this.displayFilename = displayFilename;
	}

	/** See {@link #serverFilename}. */
	public String getServerFilename() {
		return serverFilename;
	}
	/** See {@link #serverFilename}. */
	public void setServerFilename(String serverFilename) {
		this.serverFilename = serverFilename;
	}

}
