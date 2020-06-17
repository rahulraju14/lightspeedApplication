/**
 * FolderNodeImpl.java
 */
package com.lightspeedeps.object;

import java.util.*;

import javax.swing.tree.TreeNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.FolderDAO;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Folder;

/**
 * This implements the TreeNode swing class, required by the ace:tree component.
 * This implementation uses our Folder and Document classes, so that the tree
 * represents a portion of our file repository. Each node represents either a
 * Folder or a Document. Only Folder nodes can have children (of course!).
 */
public class FolderNodeImpl implements TreeNode {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FolderNodeImpl.class);

	/** Our parent node, which will always be of type Folder. Will be null for
	 * the top/root node. */
	FolderNodeImpl parent;

	/** The Folder this node represents; null if the node represents a Document. */
	Folder folder;

	/** The Document this node represents; null if the node represents a Folder. */
	Document document;

	/** The type of node -- either Folder or Document. */
	FolderNodeType type;

	/** Our list of children, including both folders and documents, as necessary. Note
	 * that each child must be wrapped in a FolderNodeImpl. */
	List<FolderNodeImpl> childList;

	public enum FolderNodeType {
		FOLDER,
		DOCUMENT;
	}

	/**
	 * Default constructor.
	 */
	public FolderNodeImpl() {
	}

	/**
	 * Constructs a Folder-type node.
	 * @param parnt
	 * @param f
	 */
	public FolderNodeImpl(FolderNodeImpl parnt, Folder f) {
		parent = parnt;
		folder = f;
		type = FolderNodeType.FOLDER;
	}

	/**
	 * Constructs a Document-type node.
	 * @param parnt
	 * @param doc
	 */
	public FolderNodeImpl(FolderNodeImpl parnt, Document doc) {
		parent = parnt;
		document = doc;
		type = FolderNodeType.DOCUMENT;
		childList = new ArrayList<>();
	}

	/**
	 * @see javax.swing.tree.TreeNode#children()
	 */
	@Override
	public Enumeration<FolderNodeImpl> children() {
		return Collections.enumeration(getChildList());
	}

	/**
	 * @see javax.swing.tree.TreeNode#getAllowsChildren()
	 */
	@Override
	public boolean getAllowsChildren() {
		// only folders allow children
		return type == FolderNodeType.FOLDER;
	}

	/**
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int arg0) {
		if (getChildList().size() > arg0) {
			return getChildList().get(arg0);
		}
		return null;
	}

	/**
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return getChildList().size();
	}

	/**
	 * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
	 */
	@Override
	public int getIndex(TreeNode arg0) {
		return getChildList().indexOf(arg0);
	}

	/**
	 * @see javax.swing.tree.TreeNode#getParent()
	 */
	@Override
	public TreeNode getParent() {
		return parent;
	}

	/**
	 * @see javax.swing.tree.TreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		return type == FolderNodeType.DOCUMENT;
	}

	/**
	 * @return our List of children; creates the list if it has not yet been
	 *         created.
	 */
	private List<FolderNodeImpl> getChildList() {
		if (childList == null) {
			createChildList();
		}
		return childList;
	}

	/**
	 * Force the list of children to be recreated.
	 */
	public void refreshChildren() {
		childList = null;
	}

	/**
	 * Create a list of all our children, consisting of folders and documents,
	 * each one wrapped in a FolderNodeImpl.
	 */
	private void createChildList() {
		childList = new ArrayList<>();
		folder = FolderDAO.getInstance().refresh(folder);
		if (folder != null) {
			for (Folder f : folder.getFolders()) {
				childList.add(new FolderNodeImpl(this, f));
			}
			/*for (Document doc : folder.getDocuments()) {
				childList.add(new FolderNodeImpl(this, doc));
			}*/
		}
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

	/** See {@link #type}. */
	public FolderNodeType getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(FolderNodeType type) {
		this.type = type;
	}

	/** See {@link #parent}. */
	public void setParent(FolderNodeImpl parent) {
		this.parent = parent;
	}

	/**
	 * Compares Department objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		FolderNodeImpl other = null;
		try {
			other = (FolderNodeImpl)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getFolder().getId() == null) {
			if (other.getFolder().getId() != null)
				return false;
		}
		return getFolder().getId().equals(other.getFolder().getId());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getFolder().getId() == null) ? 0 : getFolder().getId().hashCode());
		return result;
	}
}
