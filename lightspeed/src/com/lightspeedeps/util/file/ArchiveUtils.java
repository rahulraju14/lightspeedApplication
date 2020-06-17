/**
 * File: ArchiveUtils.java
 */
package com.lightspeedeps.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.model.User;
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
public class ArchiveUtils {
	private static final Log log = LogFactory.getLog(ArchiveUtils.class);

	private ArchiveUtils() {
	}

	private static boolean existsFolder(String folderName, Folder root) {
		boolean bRet = FileRepositoryUtils.existsFolder(folderName, root);
		return bRet;
	}

//	public boolean existsItem(String itemName) {
//		boolean bRet = FileRepositoryUtils.getInstance().existsDocument(itemName);;
//		return bRet;
//	}

	/**
	 * Create a NON-private folder with the given name.
	 */
	private static boolean createFolder(String folderName, Folder root) {
		User user = SessionUtils.getCurrentUser();
		if (user == null) {
			user = root.getUser();
		}
		boolean bRet = FileRepositoryUtils.createFolder(folderName, user, false, root);
		return bRet;
	}

	/**
	 * Get an ordered list of all the dates for which an archived Callsheet
	 * exists for the specified Unit.
	 *
	 * @param unit The Unit whose Callsheet`s will be examined.
	 * @return A possibly empty, but non-null, List of Date`s, in descending Date
	 *         order. For each Date in the List, at least one archived Callsheet
	 *         exists that matches the given Unit.
	 */
	public static List<Date> findCallsheetDates(Unit unit) {
		List<Date> dates = new ArrayList<Date>();
		String folderName = MsgUtils.formatText(Constants.ARCHIVE_CALLSHEET,
				""+unit.getProject().getId(), ""+unit.getNumber());
		Folder folder = FileRepositoryUtils.findFolder(folderName, FileRepositoryUtils.getRoot());
		if (folder != null) {
			DateFormat df = new SimpleDateFormat("yyyyMMdd");
			for (Document doc : folder.getDocuments()) {
				log.debug(doc.getName());
				String names[] = doc.getName().split("_"); // call_<projid>_<unit#>_date_updated
				if (names.length > 3 && names[3] != null) {
					String sDate = names[3];
					Date date = null;
					try {
						date = df.parse(sDate);
					}
					catch (ParseException e) {
					}
					if (date != null && ! dates.contains(date)) {
						dates.add(date);
						//log.debug(date);
					}
				}
			}
		}
		Collections.sort(dates, Collections.reverseOrder());
		return dates;
	}

	/**
	 * Get a List of all the archived callsheet Document`s for the given Unit and Date.
	 *
	 * @param unit The Unit whose Callsheet`s will be examined.
	 * @param date Only Callsheet`s whose call-date matches this value will be
	 *            returned.
	 * @return A possibly empty, but non-null, List of Document`s matching the
	 *         given Unit and Date.
	 */
	public static List<Document> findCallsheets(Unit unit, Date date) {
		List<Document> docs = new ArrayList<Document>();
		if (date != null && unit != null) {
			String folderName = MsgUtils.formatText(Constants.ARCHIVE_CALLSHEET,
					""+unit.getProject().getId(), ""+unit.getNumber());
			Folder folder = FileRepositoryUtils.findFolder(folderName, FileRepositoryUtils.getRoot());
			log.debug("folder=" + folder);
			if (folder != null) {
				DateFormat df = new SimpleDateFormat("_yyyyMMdd_");
				String csDate = df.format(date);
				for (Document doc : folder.getDocuments()) {
					if (doc.getName().indexOf(csDate) > 0) {
						docs.add(doc);
					}
				}
			}
		}
		return docs;
	}

	/**
	 * Retrieves an archived item with the given name, placing it into a
	 * temporary file on the server, and returns the generated filename of that
	 * file.
	 *
	 * @param name The fully-qualifed name of the item to be retrieved,
	 *            including the complete folder path from the root. The name may
	 *            contain the following substitution fields: {0} for project id,
	 *            and {1} for Unit number.
	 * @return The qualified name of the file created that contains the
	 *         requested item. This name is relative to this application
	 *         instance's root, and is designed to be used in an "window.open()"
	 *         browser request.
	 */
	public static String retrieveItem(String name, Unit unit) {
		name = MsgUtils.formatText(name, ""+unit.getProject().getId(), unit.getNumber());
		log.debug("item=" + name);
		String fileName = null;
		String folderName;
		String docName;
		int ix = name.lastIndexOf('/');
		if (ix > 0) {
			folderName = name.substring(0, ix);
			docName = name.substring(ix+1);
			Document document = FileRepositoryUtils.findDocument(folderName, docName, FileRepositoryUtils.getRoot());
			if (document != null) {
				Random random = new Random();
				try {
					FacesContext facesContext = FacesContext.getCurrentInstance();
					ExternalContext externalContext = facesContext.getExternalContext();
					ServletContext sc = (ServletContext)externalContext.getContext();
					int randomNumber = random.nextInt();
					fileName = Constants.ARCHIVE_RETRIEVAL_FOLDER + "/t" +
							randomNumber + document.getType();
					String realFile = sc.getRealPath(fileName);
					FileOutputStream fos = new FileOutputStream(realFile);
					fos.write(document.getContent());
					fos.flush();
					fos.close();
					log.debug("item=" + name + " retrieved into=" + fileName);
				}
				catch (Exception e) {
					EventUtils.logError(e);
				}
			}
		}
		return fileName;
	}

	/**
	 * Store the callsheet contained in the specified File into the system
	 * archive. This creates a new Document and places it in the appropriate
	 * Folder based on the Unit specified. For ease of lookup and retrieval, the
	 * document's name will reflect both the supplied call-date and the current
	 * date & time. If the appropriate Folder does not yet exist, it will be
	 * created.
	 *
	 * @param callsheetDate The call-date of the Callsheet being archived.
	 * @param unit The Unit with which this Callsheet is associated.
	 * @param file The File containing the Callsheet to be stored.
	 * @param cUser The User who will be made the "owner" of the stored
	 *            document.
	 * @return True if the Callsheet was successfully archived.
	 */
	public static boolean storeCallsheet(Date callsheetDate, Unit unit, File file, User cUser) {
		log.debug("");
		boolean bRet = false;
		Project project = unit.getProject();
		DateFormat df = new SimpleDateFormat("_yyyyMMdd");
		String csDate = df.format(callsheetDate);
		df = new SimpleDateFormat("_yyMMdd_HHmmss");
		String now = df.format(new Date());
		String name = "Call_" + project.getId() + "_"
			+ unit.getNumber() + csDate + now;

		String folderName = MsgUtils.formatText(Constants.ARCHIVE_CALLSHEET,
				""+project.getId(), ""+unit.getNumber());
		Folder root = unit.getProject().getProduction().getRootFolder();

		if (! existsFolder(folderName, root)) {
			createFolder(folderName, root);
		}
		bRet = storeItem(folderName, name, cUser, file, MimeType.PDF, callsheetDate, root);
		return bRet;
	}

	/**
	 * Store a new item in the File Repository.
	 *
	 * @param folderName The name of the folder in which the document should be
	 *            stored. This folder must already exist.
	 * @param name The name of the document to be stored.
	 * @param owner The User who will be made the "owner" of the stored
	 *            document.
	 * @param file The data to be stored.
	 * @param type The MimeType of the data to be stored.
	 * @param createDate The creation date to be assigned to the document.
	 * @param root The root folder containing the requested folderName. This is
	 *            typically the top-level folder for a Production.
	 * @return True iff the document was successfully archived.
	 */
	private static boolean storeItem(String folderName, String name, User owner, File file,
			MimeType type, Date createDate, Folder root) {
		log.debug(name + ", folder=" + folderName + ", type=" + type.getExtension());
		boolean bRet =
			FileRepositoryUtils.storeItem(folderName, name, owner, file, type, createDate, root);
		return bRet;
	}

}
