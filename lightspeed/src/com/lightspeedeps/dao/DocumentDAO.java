package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.*;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.file.FileRepositoryUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Document entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Document
 */

public class DocumentDAO extends BaseTypeDAO<Document> {
	private static final Log log = LogFactory.getLog(DocumentDAO.class);

	// property constants
//	private static final String NAME = "name";
//	private static final String DESCRIPTION = "description";
//	private static final String AUTHOR = "author";
//	private static final String PRIVATE_B = "private_";
//	private static final String TYPE = "type";
//	private static final String CONTENT = "content";

	/** The default revision value used for the newly created document. */
	private static final Integer DEFAULT_REVISION_NUMBER = 0;
	/** The default document chain id value used for the newly created document. */
	private static final Integer DEFAULT_DOCUMENT_CHAIN_ID = 0;

	public static DocumentDAO getInstance() {
		return (DocumentDAO)getInstance("DocumentDAO");
	}

	/**
	 * Save the given data in a new Document, and place it within the specified
	 * Folder. The Document's owner is set to the current User, and both the
	 * Updated and Loaded timestamps are set to the current date & time.
	 *
	 * @param folder The Folder to which the given Document should be added.
	 * @param name The name of the Document.
	 * @param owner The User who "owns" the document.
	 * @param mt The type of document, usually derived from the file's
	 *            MimeType information.
	 * @param createDate The date to store as the "created" date.
	 * @param privat True if the Document is to be marked as "private".
	 * @param data The contents of the file.
	 */
	@Transactional
	public Integer saveData(Folder folder, String name, User owner, MimeType mt,
			Date createDate, boolean privat, byte[] data) {
		Document document = new Document();
		document.setName(name);
	//	document.setContent(data);
		document.setType("." + mt.getExtension());
		if (owner == null) {
			owner = SessionUtils.getCurrentUser();
		}
		document.setAuthor(owner.getDisplayName());
		document.setUser(owner);
		document.setFolder(folder);
		Date now = Calendar.getInstance().getTime();
		document.setCreated(createDate);
		document.setUpdated(now);
		document.setLoaded(now);
		document.setDescription("This file is of type " + mt.getExtension());
		document.setPrivate(privat);
		document.setDocChainId(DEFAULT_DOCUMENT_CHAIN_ID);
		document.setRevision(DEFAULT_REVISION_NUMBER);
		document.setMimeType(mt);
		document.setDeleted(false);
		folder.getDocuments().add(document);

		Integer documentId = save(document);
		saveContent(data, documentId);
		return documentId;
	}

	/**
	 * Save the contents of a Document using the Content storage system.
	 *
	 * @param data The data (document contents) to be stored.
	 * @param documentId The id under which the contents will be stored.
	 */
	@Transactional
	private void saveContent(byte[] data, Integer documentId) {
		Content content = new Content();
		content.setDocId(documentId);
		content.setContent(data);
		content.setXfdfData(XfdfContent.EMPTY_XFDF);
		content.setXodContent(Content.EMPTY_XOD.getBytes());
		ContentDAO.getInstance().insert(content);
	}

	/**
	 * Delete a Document and remove it from its parent Folder.
	 *
	 * @param id The database id of the Document to be deleted.
	 */
	@Transactional
	public void deleteDocument(Integer id) {
		Document document = findById(id);
		Folder parent = document.getFolder();
		boolean b = parent.getDocuments().remove(document);
		if ( ! b) {
			log.warn("failed removing document from folder, folder=" + parent.getId() + ", doc=" +
					document.getId());
		}
		delete(document);
		attachDirty(parent);
	}

	/**
	 * Remove a document from active status; if it is not in use, it is also
	 * deleted from the database. If it is deleted, the parent folder is updated
	 * accordingly.
	 *
	 * @param document The document to be removed.
	 * @param updateChain If true, the DocumentChain containing the given
	 *            document is updated appropriately.
	 * @return The parent folder of the given Document, whose document
	 *         collection may have been updated.
	 */
	@Transactional
	public Folder remove(Document document, boolean updateChain) {
		document = refresh(document);
		Folder parent = document.getFolder();
		DocumentChainDAO documentChainDAO = DocumentChainDAO.getInstance();

		DocumentChain docChain = documentChainDAO.findById(document.getDocChainId());
		Integer deletedDocumentRevision = document.getRevision();
		Integer documentChainId = document.getDocChainId();
		boolean isOldestDocument = document.getOldest();

		List<ContactDocument> cdList =  ContactDocumentDAO.getInstance().findByProperty("document", document);
		Boolean used = checkDocumentStatus(cdList);

		if (used != null && ! used) {
			log.debug("");
			for(ContactDocument cd : cdList) {
				delete(cd);
			}
			boolean b = parent.getDocuments().remove(document);
			if ( ! b) {
				log.warn("failed removing document from folder, folder=" + parent.getId() + ", doc=" +
						document.getId());
			}
			document.setPacketList(null);
			delete(document);
			attachDirty(parent);
		}
		else if (used != null && used) {
			log.debug("");
			for(ContactDocument cd : cdList) {
				if (cd.getStatus() == ApprovalStatus.PENDING || cd.getStatus() == ApprovalStatus.OPEN) {
					delete(cd);
				}
			}
			if (document.getDocChainId() != null) {
				DocumentChain chain = documentChainDAO.findById(document.getDocChainId());
				chain.getApprovalPath().clear();
				log.debug("");
				attachDirty(chain);
			}
			document.setDeleted(true);
			Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(SessionUtils.getNonSystemProduction());

			Map<String, Object> values = new HashMap<>();
			values.put("name", document.getName());
			values.put("folderId", onboardFolder.getId());
			String docName = Document.DELETED_PREFIX + document.getName();

			List<Document> oldDocs = findByNamedQuery(Document.GET_DELETED_DOCUMENT_BY_NAME_FOLDER, values);
			if (oldDocs != null && (! oldDocs.isEmpty())) {
				for (int i = 1; i <= oldDocs.size(); i++) {
					docName = Document.DELETED_PREFIX + docName;
				}
			}
			//String docName = Document.DELETED_PREFIX + document.getName();
			document.setName(docName);
			document.setPacketList(null);
			attachDirty(document);
			log.debug("document " + document.getId() + " marked deleted");
		}
		else {
			log.debug("");
			// The status is SUBMITTED; Document should not be deleted;
			// code skips delete, and caller, DocumentChainDAO.removeDocuments(), issues the 'NotDeletable' message
			return null;
		}

		if (updateChain) {
			if (deletedDocumentRevision == docChain.getRevisions()) {
				Integer maximumRevision = findHighestRevisionOfDocument(documentChainId);
				docChain = documentChainDAO.refresh(docChain);
				if (maximumRevision != null) {
					docChain.setRevisions(maximumRevision);
				}
				attachDirty(docChain);
				List<Document> listOfDocuments = findByDocumentChain(docChain);
				if (listOfDocuments.size() == 0) {
					docChain = documentChainDAO.refresh(docChain);
					documentChainDAO.deleteDocumentChain(docChain, parent);
				}
			}
			else if (isOldestDocument) {
				document = findDocumentWithLowestRevision(documentChainId);
				document.setOldest(true);
				attachDirty(document);
			}
		}
		return parent;
	}

	/**
	 * Method to determine the status of a Document to be deleted, based on the
	 * approval status of all the distributed copies (ContactDocument`s) of that
	 * Document.
	 *
	 * @param cdList ContactDocument list of the document.
	 * @return False if either no copies have been distributed, or none of the
	 *         distributed copies have been signed or submitted (i.e., they are
	 *         all in OPEN or PENDING status);
	 *         <p>
	 *         true if the document has been distributed but all copies have
	 *         reached a final state (i.e., APPROVED, VOID, or LOCKED);
	 *         <p>
	 *         null if at least one copy has been distributed and
	 *         signed/submitted, but not yet reached final approval.
	 */
	private Boolean checkDocumentStatus(List<ContactDocument> cdList) {
		Boolean used = false;
		for(ContactDocument cd : cdList) {
			if (cd.getStatus() == ApprovalStatus.PENDING || cd.getStatus() == ApprovalStatus.OPEN) {
				continue;
			}
			else {
				if (cd.getStatus() == ApprovalStatus.APPROVED || cd.getStatus() == ApprovalStatus.VOID || cd.getStatus() == ApprovalStatus.LOCKED) {
					used = true;
				}
				else {
					used = null;
					break;
				}
			}
		}
		return used;
	}

	/**
	 * Deletes both the MySQL instance and the document content from our Content
	 * database. Note that this method does not affect the parent Folder. The
	 * caller is responsible for updating the collection.
	 *
	 * @param doc The Document to delete.
	 */
	@Transactional
	public void delete(Document doc) {
		Integer id = doc.getId();
		super.delete(doc);
		ContentDAO.getInstance().removeByDocId(id);
	}

	public List<Document> findByDocumentChain(DocumentChain docChain) {
		int docChainId = docChain.getId();
		List<Document> documents = findByNamedQuery(Document.GET_DOCUMENT_LIST_BY_DOCUMENT_CHAIN, map("documentChainId", docChainId));
		return documents;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Integer findHighestRevisionOfDocument(Integer documentChainId) {
		List<Integer> document = (List)findByNamedQuery(Document.GET_HIGHEST_REVISION_OF_DOCUMENT, map("documentChainId", documentChainId));
		Integer maximumRevision = document.get(0);
		return maximumRevision;
	}

	/**
	 * Find the 'master' Payroll Start document for a given Production.
	 *
	 * @param prod The Production of interest.
	 * @return The PayrollStart Document.
	 */
	public static Document findPayrollDocument(Production prod) {
		Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(prod);
		Map<String, Object> values = new HashMap<>();
		values.put(Document.NAME, PayrollFormType.START.getName());
		values.put(Document.FOLDER_ID, onboardFolder.getId());
		Document payrollDocument = DocumentDAO.getInstance().findOneByNamedQuery(Document.GET_DOCUMENT_BY_NAME_FOLDER_ID, values);
		return payrollDocument;
	}

	public Document findDocumentWithLowestRevision(Integer documentChainId) {
		List<Document> document = findByNamedQuery(Document.GET_DOCUMENT_WITH_LOWEST_REVISION, map("documentChainId", documentChainId));
		Document documentWithLowestRevision = null;
		if (document.size() > 0) {
			documentWithLowestRevision = document.get(0);
		}
		return documentWithLowestRevision;
	}

	public Document findDocumentWithHighestRevision(DocumentChain documentChain) {
		Integer documentChainId = documentChain.getId();
		List<Document> document = findByNamedQuery(Document.GET_DOCUMENT_WITH_HIGHEST_REVISION, map("documentChainId", documentChainId));
		Document documentWithLowestRevision = null;
		if (document.size() > 0) {
			documentWithLowestRevision = document.get(0);
		}
		return documentWithLowestRevision;
	}

	/**
	 * @return List of all Documents in the current production's "Onboarding"
	 * folder, and all its children, sorted by document name.
	 */
	public List<Document> findAllOnboardingDocuments(Production production) {
		List<Integer> folderIdList = new ArrayList<>();
		List<Document> onboardDocuments;
		Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
		if (onboardFolder != null) {
			folderIdList = addFolderId(folderIdList, onboardFolder);
		}
		if (folderIdList.size() > 0) {
			onboardDocuments = findByNamedQuery(Document.GET_DOCUMENT_LIST_BY_FOLDER_IDS, "folderId", folderIdList);
		}
		else {
			onboardDocuments = new ArrayList<>();
		}
		return onboardDocuments;
	}

	/**
	 * Recursively add the database id's of all children of the given parent to
	 * the supplied List.
	 *
	 * @param folderIdList The list to which the child folder id's will be
	 *            added.
	 * @param parent The root of the tree for which the child folder id's will
	 *            be added.
	 * @return A List of the database id's for the entire tree of folders rooted
	 *         at the given parent.
	 */
	public List<Integer> addFolderId(List<Integer> folderIdList, Folder parent) {
		if (parent != null) {
			folderIdList.add(parent.getId());
			for (Folder child : parent.getFolders()) {
				addFolderId(folderIdList, child);
			}
		}
		return folderIdList;
	}

	/** Finds Document corresponding to the ACTRA Intent Form in the Onboard Folder
	 * of the given Production.
	 * @param production Production to which the currently viewed Project belongs on "Project/Projects" tab.
	 * @return returns Document corresponding to the ACTRA Intent Form in the given Production.
	 */
	public Document findIntentDocument(Production production) {
		Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
		if (onboardFolder != null) {
			log.debug("");
			String docName = PayrollFormType.ACTRA_INTENT.getName();
			String query = "from Document d where d.folder.id = ? and d.standard = true and d.name='" + docName + "'";
			return findOne(query, onboardFolder.getId());
		}
		else {
			log.debug("");
			//TODO
		}
		return null;
	}

	/** Method creates the document and document chain records for builtin documents.
	 * @param onboardFolder root folder of the document and chain
	 * @param type Type of form being added.
	 */
	public void saveStandardDocumentDocumentChain(Folder onboardFolder, PayrollFormType type) {
		Date date = new Date();
		DocumentChain chain = new DocumentChain();
		chain.setName(type.getName());
		chain.setType(MimeType.LS_FORM);
		chain.setCreated(date);
		chain.setRevised(date);
		chain.setRevisions(1);
		chain.setCreatorAcct(SessionUtils.getCurrentUser().getAccountNumber());
		chain.setDocFlowType(DocumentFlowType.RD_SN_SUB);
		chain.setFolder(onboardFolder);
		chain.setDeleted(false);
		Integer chainId = save(chain);

		Document document = new Document();
		document.setName(type.getName());
		document.setDescription(type.getDescription());
		document.setListOrder(type.ordinal()+100); // order after custom docs; in order of PayrollFormType list
		document.setAuthor(SessionUtils.getCurrentUser().getDisplayName());
		document.setUser(SessionUtils.getCurrentUser());
		document.setCreated(date);
		document.setLoaded(date);
		document.setUpdated(date);
		document.setPrivate(false);
		document.setType(type.getName());
		document.setContent(null);
		document.setFolder(onboardFolder);
		document.setMimeType(MimeType.LS_FORM);
		document.setDocChainId(chainId);
		document.setRevision(1);
		document.setOldest(true);
		document.setDeleted(false);
		document.setStandard(true);
		save(document);
	}

	/**
	 * Lock the specified Document, so no other User
	 * can edit it, and return True if successful.
	 *
	 * @param document The Document to be locked.
	 * @param user The User who will "hold" the lock and is allowed to edit the Document.
	 * @return True if the User has successfully been given the lock, which
	 *         means that no other User currently had the lock and we
	 *         successfully updated the database with the lock information, or
	 *         the same User already had the lock.
	 */
	@Transactional
	public boolean lock(Document document, User user) {
		boolean ret = false;
		try {
			if (document != null) {
				if (document.getLockedBy() == null) {
					document.setLockedBy(user.getId());
					attachDirty(document);
					log.debug("Document = "+ document.getId());
					ret = true;
				}
				else if (document.getLockedBy().equals(user.getId())) {
					ret = true;
				}
				log.debug("locked #" + document.getId() + "=" + ret + ", by=" + user.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
			if (document != null) {
				// Most likely attach failed; don't indicate object was locked
				document.setLockedBy(null);
			}
		}
		return ret;
	}

	/**
	 * Unlock a Document so any User (with appropriate permission) can now
	 * edit it. No error occurs if the Document was already unlocked.
	 *
	 * @param document The Document to be unlocked.
	 * @param userId The database id of the User that is trying to unlock the
	 *            document. If the Document is locked by a different user, the
	 *            unlock request is ignored.
	 * @return True if the document was updated in the database; false indicates
	 *         the document was not updated, because either (a) it was not
	 *         locked, or (b) it was locked by someone other than the given
	 *         userId.
	 */
	@Transactional
	public boolean unlock(Document document, Integer userId) {
		boolean bRet = false;
		try {
			log.debug("unlocking #" + document.getId() + "; locked by=" + document.getLockedBy());
			if (document.getLockedBy() != null) {
				if (userId == null || userId.equals(document.getLockedBy())) {
					document.setLockedBy(null);
					attachDirty(document);
					log.debug("Document Unlocked = "+ document.getId());
					bRet = true;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

}