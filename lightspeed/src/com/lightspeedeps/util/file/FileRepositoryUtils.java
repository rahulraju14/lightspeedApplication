/**
 * File: FileRepositoryUtils.java
 */
package com.lightspeedeps.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.AttachmentDAO;
import com.lightspeedeps.dao.ContentDAO;
import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.FolderDAO;
import com.lightspeedeps.model.Content;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.XfdfContent;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * Utility functions related to archiving reports or other
 * data.  It currently uses the FileRepository for storage,
 * but the idea is that the "back end" could change without
 * having to change users of the archive functionality.
 */
public final class FileRepositoryUtils {
	/** Logger */
	private static final Log LOG = LogFactory.getLog(FileRepositoryUtils.class);

	private FileRepositoryUtils() {
	}

//	public static FileRepositoryUtils getInstance() {
//		return new FileRepositoryUtils();
//	}

	/**
	 * @return The root folder, in the repository, of the current Production.
	 */
	public static Folder getRoot() {
		if (SessionUtils.getProduction() != null) {
			return SessionUtils.getProduction().getRootFolder();
		}
		return null;
	}

	/**
	 * @param prod
	 * @return The root folder, in the repository, of the given Production.
	 */
	public static Folder getRoot(Production prod) {
		if (prod != null) {
			return prod.getRootFolder();
		}
		return null;
	}

	/**
	 * Determine if a particular folder name already exists within a particular
	 * sub-tree.
	 *
	 * @param folderName The name of the folder to be found. Note that Folder
	 *            names are NOT case-sensitive.
	 * @param root The root at which to begin the search.
	 * @return True if the folder exists in the sub-tree, false if not.
	 */
	public static boolean existsFolder(String folderName, Folder root) {
		Folder folder = findFolder(folderName, root);
		boolean bRet = (folder != null);
		return bRet;
	}

	/**
	 * Find the specified folder. Note that Folder names are NOT case-sensitive.
	 *
	 * @param folderName The fully-qualifed name of the Folder, starting from
	 *            the repository root.
	 * @param root The folder at which to begin the search; must not be null.
	 * @return The requested Folder if found, else null.
	 */
	public static Folder findFolder(String folderName, Folder root) {
		String names[] = folderName.split("/");
		Folder folder = root;
		Folder child;
		folder = FolderDAO.getInstance().refresh(folder);
		String name;
		for (int i=0; i < names.length; i++) {
			name = names[i];
			child = null;
			for (Folder f : folder.getFolders()) {
				if (f.getName().equalsIgnoreCase(name)) {
					child = f;
					break;
				}
			}
			if (child == null) {
				folder = null;
				break;
			}
			folder = child;
		}
		LOG.debug("folderName=" + folderName + ", folder=" + folder);
		return folder;
	}

	/**
	 * Find the specified document in the given folder. Note that Document and
	 * Folder names are NOT case-sensitive.
	 *
	 * @param folderName The fully-qualifed name of the containing Folder,
	 *            starting from the repository root.
	 * @param docName The name of the Document.
	 * @return The requested Document if found, else null.
	 */
	public static Document findDocument(String folderName, String docName, Folder root) {
		Document doc = null;
		Folder folder = findFolder(folderName, root);
		if (folder != null) {
			for (Document d : folder.getDocuments()) {
				if (d.getName().equalsIgnoreCase(docName)) {
					doc = d;
					break;
				}
			}
		}
		return doc;
	}

	/**
	 * Helper method to get the root On-boarding folder for a Production.
	 *
	 * @param prod The Production of interest; if null, the current Production
	 *            is used.
	 * @return the Production's Onboarding folder, or null if no Onboarding
	 *         folder exists.
	 */
	public static Folder findOnboardingFolder(Production prod) { // NOPMD by DHarm on 8/24/18 3:14 PM
		if (prod == null) {
			prod = SessionUtils.getProduction();
			if (prod == null) {
				return null;
			}
		}
		return findFolder(Constants.ONBOARDING_FOLDER, FileRepositoryUtils.getRoot(prod));
	}

	/**
	 * Determine if the specified document exists in the given folder. Note that
	 * Document and Folder names are NOT case-sensitive.
	 *
	 * @param folderName The fully-qualifed name of the containing Folder,
	 *            starting from the repository root.
	 * @param docName The name of the Document.
	 * @return True if the Document if found, else false.
	 */
//	public static boolean existsDocument(String folderName, String docName) {
//		return findDocument(folderName, docName, root) != null;
//	}

	/**
	 * Create the specified folder path, which may involve creating multiple
	 * folders.
	 *
	 * @param folderName The fully-qualified folder name (includes all parent
	 *            folders starting at the root).
	 * @param user The User that will own all Folder`s created.
	 * @param privat The 'private' flag setting for the Folder. Folder's marked
	 *            private are generally only visible to the owner.
	 * @return True if the folder was successfully created, or already existed.
	 */
	public static boolean createFolder(String folderName, User user, boolean privat, Folder root) {
		boolean bRet = false;
		try {
			String names[] = folderName.split("/");
			FolderDAO folderDAO = FolderDAO.getInstance();
			Folder parent = root;
			Folder child;
			String name = "";
			for (int i=0; i < names.length; i++) {
				name += names[i];
				child = findFolder(name, root);
				if (child == null) {
					child = new Folder(names[i], user, parent, privat, new Date()); // NOPMD by DHarm on 8/24/18 3:14 PM
					parent.getFolders().add(child);
					folderDAO.save(child);
				}
				parent = child;
				name += '/';
			}
			bRet = true;
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}

		return bRet;
	}

	/**
	 * Creates a new Document and saves it in the repository in the specified
	 * Folder, with the contents of the given File. The Document will be owned
	 * by the current User, and be marked private. The Document's 'loaded' and
	 * 'updated' dates will both be set to the current date & time.
	 *
	 * @param folderName The fully-qualified folder name (includes all parent
	 *            folders starting at the root). The folder must already exist.
	 * @param name The name to be assigned to the new Document.
	 * @param owner The User who will be made the "owner" of the new Document.
	 * @param file The File object whose contents are to be copied into the
	 *            Document.
	 * @param type The content type of the File.
	 * @param createDate The creation Date to be set in the Document.
	 * @param root The root folder containing the requested folderName.
	 * @return True iff the file's data was successfully stored in a new
	 *         Document.
	 */
	public static boolean storeItem(String folderName, String name, User owner,
			File file, MimeType type, Date createDate, Folder root) {
		boolean bRet = false;
		Folder folder = findFolder(folderName, root);
		if (folder != null) { // NOPMD by DHarm on 8/24/18 3:14 PM
			Integer id = storeFile(folder, name, owner, file, type, null, createDate, true);
			bRet = id > 0;
		}
		else {
			LOG.warn("Folder " + folderName + " not found.");
		}
		return bRet;
	}

	/**
	 * Creates a new Document and saves it in the repository in the specified
	 * Folder, with the contents of the given File. The Document will be owned
	 * by the current User, and the Document's 'loaded' and 'updated' dates will
	 * both be set to the current date & time.
	 *
	 * @param folder The Folder in which the new Document should be placed.
	 * @param documentName The name to be assigned to the new Document.
	 * @param owner The User who should be recorded as owning the document.
	 * @param file The File object whose contents are to be copied into the
	 *            Document.
	 * @param mt The MimeType of the document; if this is null, this method will
	 *            try and determine the value from the 'contentType' parameter.
	 * @param contentType The content type of the File, typically the Window's
	 *            file extension for this Document type. If the 'mt' parameter
	 *            is non-null, this parameter is ignored.
	 * @param createDate The creation Date to be set in the Document.
	 * @param privat True if the new Document should be marked as private.
	 * @return True if the file's data was successfully stored in a new
	 *         Document.
	 */
	public static Integer storeFile(Folder folder, String documentName, User owner, File file,
			MimeType mt, String contentType, Date createDate, boolean privat) {
		Integer id = 0;
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				byte[] data = new byte[(int)file.length()];
				in.read(data);
				id = DocumentDAO.getInstance().saveData(folder, documentName, owner,
						mt, createDate, privat, data);
			}
			finally {
				in.close();
			}
		}
		catch (FileNotFoundException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("file not found: ", e);
		}
		catch (IOException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("IO exception: ", e);
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
//		return bRet;
		return id;
	}

	/**
	 * Uploads the Master XFDF for the given document.
	 * @param file The File object whose contents are to be copied into the
	 *         Document.
	 * @param document The Document for which the Master XFDF is uploaded.
	 * @return True if the file's data was successfully stored for Document.
	 */
	public static boolean storeXfdfFile( File file, Document document) {
		boolean bRet = false;
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				byte[] data = new byte[(int)file.length()];
				in.read(data);
				String xfdf = new String(data);
				Content content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
				if (content != null) {
					LOG.debug("EMPTY XFDF STRING = " + content.getXfdfData());
					ContentDAO.getInstance().updateContent(xfdf, document.getId()); // save the updated xfdf
					// fetch updated content. LS-1323
					content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
					if (content != null) {
						LOG.debug("UPDATED XFDF STRING 2 = " + content.getXfdfData());
						String xfdfString = content.getXfdfData();
						if (xfdfString != null &&
								(! xfdfString.equalsIgnoreCase(XfdfContent.EMPTY_XFDF))) {
							bRet = true;
							LOG.debug("XFDF STRING = " + xfdfString);
						}
					}
				}
			}
			finally {
				in.close();
			}
		}
		catch (FileNotFoundException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("file not found: ", e);
		}
		catch (IOException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("IO exception: ", e);
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * Uploads the Attachment for the given contact document.
	 *
	 * @param contactDocument The ContactDocument for which the Attachment is uploaded.
	 * @param file The File object whose contents are to be copied into the
	 *            Document.
	 * @param mt The MimeType of the attachment being uploaded.
	 * @return True if the file's data was successfully stored for Document.
	 */
	public static Integer storeAttachment(String fileName, File file, MimeType mt) {
		Integer id = 0;
		try {
			FileInputStream in = new FileInputStream(file);
			try {
				byte[] data = new byte[(int)file.length()];
				in.read(data);
				id = AttachmentDAO.getInstance().saveData(null, fileName, data, mt);
			}
			finally {
				in.close();
			}
		}
		catch (FileNotFoundException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("file not found: ", e);
		}
		catch (IOException e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError("IO exception: ", e);
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return id;
	}


}
