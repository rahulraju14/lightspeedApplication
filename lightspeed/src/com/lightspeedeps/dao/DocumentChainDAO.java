package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.DocumentFlowType;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;

public class DocumentChainDAO extends BaseTypeDAO<DocumentChain> {
	private static final Log log = LogFactory.getLog(DocumentChainDAO.class);

	/** The default revision value used for the newly created document chain. */
	private static final Integer DEFAULT_REVISION_NUMBER = 1;

	public static DocumentChainDAO getInstance() {
		return (DocumentChainDAO)getInstance("DocumentChainDAO");
	}

	/** This is used to save the document chain data
	 * @param folder parent folder of the document chain
	 * @param name name of the document chain
	 * @param owner creator of the document chain
	 * @param type mimetype of the document chain
	 * @param docFlowType document flow type of the document chain
	 * @param documentCreatedTime created time of the document chain
	 * @return id of the persisted document chain
	 */
	@Transactional
	public Integer saveDocumentChain(Folder folder, String name, String shortName, User owner, MimeType type,
			String docFlowType, Date documentCreatedTime) {
		DocumentChain docChain = new DocumentChain();
		docChain.setName(name);
		docChain.setShortName(shortName);
		docChain.setCreated(documentCreatedTime);
		docChain.setRevised(documentCreatedTime);
		docChain.setRevisions(DEFAULT_REVISION_NUMBER);
		docChain.setFolder(folder);
		docChain.setType(type);
		docChain.setDocFlowType(DocumentFlowType.NONE);
		if (owner == null) {
			owner = SessionUtils.getCurrentUser();
		}
		docChain.setCreatorAcct(owner.getAccountNumber());
		return save(docChain);

	}

	/** This is used to update the document chain with the new revision
	 * @param docChain document chain that needs to be updated
	 * @param revisedDate current date
	 * @param mimeType
	 * @return updated document chain
	 */
	public DocumentChain updateDocumentChain(DocumentChain docChain, Date revisedDate, MimeType mimeType) {
		docChain.setRevisions(docChain.getRevisions() + 1);
		docChain.setRevised(revisedDate);
		docChain.setType(mimeType);
		return merge(docChain);
	}

	/** This is used to find the document chain
	 * @param name Name of the document chain
	 * @param folderId the folder id of the document chain
	 * @return document chain
	 */
	public DocumentChain findDocumentChainByFolderIdAndName(String name,
			Integer folderId) {
		String queryString = "from DocumentChain where Folder_Id = ? and Name = ?";
		List<Object> valueList = new ArrayList<>();
		valueList.add(folderId);
		valueList.add(name);
		return findOne(queryString, valueList.toArray());
	}

	/** This is used to delete the document chain
	 * @param currentFolder
	 * @param id id of the document chain to be removed
	 */
	@Transactional
	public void deleteDocumentChain(DocumentChain docChain, Folder currentFolder){
	//	DocumentChain docChain = findById(id);
		if(docChain != null){
		//	Folder parent = docChain.getFolder();
			if ( currentFolder!= null) {
				boolean b = currentFolder.getDocumentChains().remove(docChain);
				if (! b) {
					log.warn("failed removing documentChain from parent, parent=" + currentFolder.getId() +
							", child =" + docChain.getId());
				}
				List<Document> documentList = DocumentDAO.getInstance().findByProperty("docChainId", docChain.getId());
				if (documentList != null) {
					for (Document document : documentList) {
						b = currentFolder.getDocuments().remove(document);
						if (! b) {
							log.warn("failed removing document from parent, parent=" + currentFolder.getId() +
									", child =" + currentFolder.getId());
						}
						Long contactDocumentCount = ContactDocumentDAO.getInstance().findCountByNamedQuery(ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT, map("documentId", document.getId()));
						if (contactDocumentCount > 0) {
							document.setDeleted(true);
							DocumentDAO.getInstance().attachDirty(document);
						}
						else {
							DocumentDAO.getInstance().delete(document);
						}
					}
					documentList.clear();
				}
				//upadateByProcedure(docChain.getId());
				attachDirty(currentFolder);
				delete(docChain);
				docChain = null;
			}
		}
	}

	/**
	 * Removes a DocumentChain from active status -- either deleting it, or
	 * marking it as "deleted". Any documents within its chain are likewise
	 * either deleted or marked as such.
	 * <p>
	 * First, documents that have not been distributed (are not referenced by
	 * any ContactDocument) are deleted; those is use are marked deleted. When
	 * this has completed, if all documents in the chain were physically
	 * deleted, then the chain is also deleted; otherwise, the chain is marked
	 * Deleted.
	 *
	 * @param chain The chain to be removed.
	 * @return The parent folder, whose collections may have been updated.
	 */
	@Transactional
	public Folder remove(DocumentChain chain) {
		// For any docs in chain, either delete them, or mark them deleted
		Folder parent = chain.getFolder();
		boolean hasDocs = removeDocuments(chain);
		if (! hasDocs) {
			// it's empty, OK to actually delete from database
			boolean b = parent.getDocumentChains().remove(chain);
			if ( ! b) {
				log.warn("failed removing chain from folder, folder=" + parent.getId() +
						", chain=" + chain.getId());
			}
			delete(chain);
			attachDirty(parent);
		}
		return parent;
	}

	/**
	 * Removes a DocumentChain from active status -- marking it as "deleted", or
	 * returning false if no documents remain in the chain. Any documents within
	 * its chain are either deleted or marked as such.
	 * <p>
	 * Note that this method never physically deletes a chain; that is left to
	 * the caller.
	 * <p>
	 * First, documents that have not been distributed (are not referenced by
	 * any ContactDocument) are deleted; those is use are marked deleted. When
	 * this has completed, if all documents in the chain were physically
	 * deleted, then false is returned; otherwise, the chain is marked Deleted
	 * and true is returned.
	 *
	 * @param chain The chain to be removed.
	 * @return True if there are any documents left in the chain.
	 */
	@Transactional
	public boolean removeDocuments(DocumentChain chain) {
		List<Document> documentList = DocumentDAO.getInstance().findByProperty("docChainId", chain.getId());
		for (Document document : documentList) {
			if (! document.getDeleted()) {
				if (DocumentDAO.getInstance().remove(document, false) == null) {
					MsgUtils.addFacesMessage("Document.NotDeletable", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		boolean hasDocs = DocumentDAO.getInstance().existsProperty("docChainId", chain.getId());
		// Any docs left in chain? (There could be "deleted" ones.)
		if (hasDocs) {
			Document document = DocumentDAO.getInstance().findOneByProperty("docChainId", chain.getId());
			// yes, so just mark chain deleted
			if (document.getDeleted()) {
				chain.setDeleted(true);
				String chainName = Document.DELETED_PREFIX + chain.getName();
				log.debug("Name = " + document.getName());
				int occurance = StringUtils.countOccurrencesOf(document.getName(), "*");
				log.debug("occurance = " + occurance);
				if (occurance != 0 && occurance > 1) {
					for (int i = 1; i < occurance; i++) {
						chainName = Document.DELETED_PREFIX + chainName;
					}
				}
				log.debug("chainName = " + chainName);
				chain.setName(chainName);
				attachDirty(chain);
				log.debug("documentChain " + chain.getId() + " marked deleted");
			}
		}
		return hasDocs;
	}

	public List<DocumentChain> findDocumentChainList(Integer folderId){
		List<DocumentChain> docChainList = findByNamedQuery(DocumentChain.GET_DOCUMENT_CHAIN_LIST, map("folderId", folderId));
		return docChainList;
	}

	public List<DocumentChain> findAllOnboardingDocumentChains(Production production) {
		List<Integer> folderIdList = new ArrayList<>();
		Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(production);
		folderIdList = addFolderId (folderIdList, onboardFolder);
		List<DocumentChain> onboardDocuments = findByNamedQuery(DocumentChain.GET_NON_DELETED_DOCUMENT_CHAIN_LIST_BY_FOLDER_IDS, "folderId",folderIdList);
		return onboardDocuments;
	}

	public List<Integer> addFolderId(List<Integer> folderIdList, Folder parent) {
		folderIdList.add(parent.getId());
		for (Folder child : parent.getFolders()) {
			addFolderId(folderIdList, child);
		}
		return folderIdList;
	}

	/** Method used to find all the standard Document chains for the given production.
	 * @param production Production whose standard document chains will be returned.
	 * @return List of standard Document chains.
	 */
	//@SuppressWarnings("unused")
	//we probably don't need this code any more.
	/*private List<DocumentChain> findAllOnboardingStandardDocumentChains(Production production) {
		log.debug("");
		List<DocumentChain> standardDocumentChainList = new ArrayList<>();
		Folder onboardFolder = FileRepositoryUtils.findFolder(Constants.ONBOARDING_FOLDER, FileRepositoryUtils.getRoot(production));
		if (onboardFolder != null) {
			Map<String, Object> values = new HashMap<>();
			log.debug("......folderId " + onboardFolder.getId());
			values.put("folderId", onboardFolder.getId());
			//I9
			addDocChainByName(PayrollFormType.I9.getName(), values, standardDocumentChainList);
			//Payroll
			addDocChainByName(PayrollFormType.START.getName(), values, standardDocumentChainList);
			//W4
			addDocChainByName(PayrollFormType.W4.getName(), values, standardDocumentChainList);
			//CA WTPA
			addDocChainByName(PayrollFormType.CA_WTPA.getName(), values, standardDocumentChainList);
			//NY WTPA
			addDocChainByName(PayrollFormType.NY_WTPA.getName(), values, standardDocumentChainList);
		}
		return standardDocumentChainList;
	}
*/
	/**
	 * @param chainName
	 * @param values
	 * @param standardDocumentChainList
	 */
	/*private void addDocChainByName(String chainName, Map<String, Object> values,
			List<DocumentChain> standardDocumentChainList) {
		values.put("documentChainName", chainName);
		DocumentChain chain = findOneByNamedQuery(DocumentChain.GET_DOCUMENT_CHAIN_BY_NAME_AND_FOLDER, values);
		standardDocumentChainList.add(chain);
	}*/

}
