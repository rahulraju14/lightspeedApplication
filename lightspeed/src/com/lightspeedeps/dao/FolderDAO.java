package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Folder entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Folder
 */

public class FolderDAO extends BaseTypeDAO<Folder> {
	private static final Log log = LogFactory.getLog(FolderDAO.class);

	// property constants
//	private static final String NAME = "name";
//	private static final String PRIVATE_ = "private_";

	public static FolderDAO getInstance() {
		return (FolderDAO)getInstance("FolderDAO");
	}

//	public static FolderDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (FolderDAO) ctx.getBean("FolderDAO");
//	}

	/**
	 * Create a new folder and add it to the given parent's collection.
	 *
	 * @param name The name of the new folder.
	 * @param parentId The Folder.id value of the intended parent Folder.
	 * @param isPrivate The 'private' setting to be assigned to the new Folder.
	 */
	@Transactional
	public Folder createFolder(String name, Integer parentId, boolean isPrivate) {
		Folder parent = findById(parentId);
		Folder childFolder = new Folder(name, SessionUtils.getCurrentUser(),
				parent, isPrivate, new Date());
		parent.getFolders().add(childFolder);
		attachDirty(parent); // new folder (child) will be saved via cascade
//		save(childFolder);
		return childFolder;
	}

	/**
	 * Move a folder from one parent to another.
	 * @param moveFolder
	 * @param newParentFolder
	 */
	@Transactional
	public void move(Folder moveFolder, Folder newParentFolder) {
		moveFolder = refresh(moveFolder);
		// remove from existing parent
		moveFolder.getFolder().getFolders().remove(moveFolder);
		attachDirty(moveFolder.getFolder());
		// add to new parent
		newParentFolder = refresh(newParentFolder);
		moveFolder.setFolder(newParentFolder);
		newParentFolder.getFolders().add(moveFolder);
		attachDirty(newParentFolder);
		attachDirty(moveFolder);
	}

	/**
	 * Rename the given folder.
	 *
	 * @param renameFolder The folder to be updated
	 * @param newName The new folder name to apply.
	 * @return The refreshed copy of the given folder. Returns null if the
	 *         rename would conflict with an existing folder of the same name.
	 */
	@Transactional
	public Folder rename(Folder renameFolder, String newName) {
		renameFolder = refresh(renameFolder);
		Folder folder = findFolderByParentIdAndName(newName, renameFolder.getFolder().getId());
		if (folder == null) {
			renameFolder.setName(newName);
			attachDirty(renameFolder);
		}
		else {
			renameFolder = null;
		}
		return renameFolder;
	}

	/**
	 * Delete a folder given it's database id.
	 *
	 * @param id The id of the Folder to be deleted.
	 */
	@Transactional
	public void deleteFolder(Integer id) {
		Folder folder = findById(id);
		remove(folder);
	}

	/**
	 * Delete a folder from the database. The documents and documentChains that
	 * it contains are updated as well -- either deleted, or marked deleted. Any
	 * items not deleted will be moved into the given folder's parent folder.
	 *
	 * @param folder The Folder to be deleted.
	 */
	@Transactional
	public void remove(Folder folder) {
		if (folder != null) {
			folder = refresh(folder);
			if (folder != null) {
				log.debug("id=" + folder.getId() + ", " + folder.getName());
				if (folder.getFolders().size() > 0) {
					while( folder.getFolders().size() > 0) {
						Folder child = folder.getFolders().get(0);
						remove(child);
						folder = refresh(folder);
					}
					attachDirty(folder);
				}
			}
			// First delete each of the contained document chains.
			// This process also takes care of all the documents in those chains.
			for (Iterator<DocumentChain> itr = folder.getDocumentChains().iterator(); itr.hasNext(); ) {
				DocumentChain chain = itr.next();
				log.debug(chain.toString());
				if (! chain.getDeleted()) { // skip any previously-deleted chains
					boolean hasDocs = DocumentChainDAO.getInstance().removeDocuments(chain);
					if (! hasDocs) { // it's empty, OK to actually delete from database
						log.debug("<>");
						itr.remove();
						delete(chain);
					}
				}
			}
			Folder parent = folder.getFolder();
			if (parent != null) {
				log.debug("<>");
				// remove this folder from its parent, in preparation for deleting it.
				boolean b = parent.getFolders().remove(folder);
				if (! b) {
					log.warn("failed removing folder from parent, parent=" + parent.getId() +
							", child folder=" + folder.getId());
				}
				// Now move any remaining ("deleted") chains and documents to the parent.
				for (Document doc : folder.getDocuments()) {
					doc.setFolder(parent);
					parent.getDocuments().add(doc);
					log.debug("<>");
					attachDirty(doc);
				}
				for (DocumentChain chain : folder.getDocumentChains()) {
					chain.setFolder(parent);
					parent.getDocumentChains().add(chain);
					log.debug("<>");
					attachDirty(chain);
				}
				log.debug("<>");
				attachDirty(parent);
			}
			// Almost done ... clear the collections and delete the folder.
			folder.getDocumentChains().clear();
			folder.getDocuments().clear();
			log.debug("<>");
			delete(folder);
		}
	}

    public Folder findFolderByParentIdAndName(String userFileName,
			Integer parentId) {
		String queryString = "from Folder where Parent_Id = ? and Name = ?";
		List<Object> valueList = new ArrayList<Object>();
		valueList.add(parentId);
		valueList.add(userFileName);
		return findOne(queryString, valueList.toArray());
	}

}