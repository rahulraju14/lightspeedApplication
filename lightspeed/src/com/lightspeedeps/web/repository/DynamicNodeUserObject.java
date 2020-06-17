package com.lightspeedeps.web.repository;

import javax.faces.event.ActionEvent;
import javax.swing.tree.DefaultMutableTreeNode;

//import com.icesoft.faces.component.tree.IceUserObject;

/**
 * <p>
 * The <code>NodeUserObject</code> represents a nodes user object. This
 * particular IceUserobject implementation stores extra information on how many
 * times the parent node is clicked on. It is also responsible for copying and
 * deleting itself.
 * </p>
 * <p>
 * In this example pay particularly close attention to the <code>wrapper</code>
 * instance variable on IceUserObject. The <code>wrapper</code> allows for
 * direct manipulations of the parent tree.
 * </p>
 */
public class DynamicNodeUserObject /*extends IceUserObject*/ {

	/** */
	private static final long serialVersionUID = 1L;

	private final static String ICON_PREFIX = "../../i/";
	private final static String ICON_LEAF 					= ICON_PREFIX + "tree_document.gif";
	private final static String ICON_FOLDER_OPEN 			= ICON_PREFIX + "tree_folder_open.gif";
	private final static String ICON_FOLDER_OPEN_SELECTED 	= ICON_PREFIX + "tree_folder_open_selected.gif";
	private final static String ICON_FOLDER_CLOSED 			= ICON_PREFIX + "tree_folder_close.gif";
	private final static String ICON_FOLDER_CLOSED_SELECTED = ICON_PREFIX + "tree_folder_close_selected.gif";
	private final static String ICON_LEAF_SELECTED 			= ICON_PREFIX + "tree_document_selected.gif";

	/*
	 * panel stack which will be manipulated when a command links action is
	 * fired.
	 */
	private final RepositoryBean treeBean;

	private boolean leaf;

	private static String nodeToolTip; // (LS-DH: is this ever set? 'static' might be a problem if it was actually used.)

	/**
	 * Default constructor for a PanelSelectUserObject object. A reference is
	 * made to a backing bean with the name "panelStack", if possible.
	 *
	 * @param wrapper
	 */
	public DynamicNodeUserObject(DefaultMutableTreeNode wrapper,
			RepositoryBean tree) {
//		super(wrapper);

		treeBean = tree;

		setLeafIcon(ICON_LEAF);
		setBranchContractedIcon(ICON_FOLDER_CLOSED);
		setBranchExpandedIcon(ICON_FOLDER_OPEN);
		setText(generateLabel());
		setTooltip(nodeToolTip);
		setExpanded(true);
	}

	/**
	 * Generates a label for the node based on an incrementing int.
	 *
	 * @return the generated label (eg. 'Node 5')
	 */
	private String generateLabel() {
		return treeBean.getUserFileName();
	}

	/**
	 * Deletes this not from the parent tree.
	 */
	public void deleteNode() {
		((DefaultMutableTreeNode) getWrapper().getParent())
				.remove(getWrapper());
	}

	/**
	 * Copies this node and adds a it as a child node.
	 *
	 * @param event
	 *            that fired this method
	 */
	public void createNode(ActionEvent event) {
		DefaultMutableTreeNode clonedWrapper = new DefaultMutableTreeNode();
		DynamicNodeUserObject clonedUserObject = new DynamicNodeUserObject(
				clonedWrapper, treeBean);
		DynamicNodeUserObject originalUserObject = (DynamicNodeUserObject) getWrapper()
				.getUserObject();
		clonedUserObject.setAction(originalUserObject.getAction());
		clonedUserObject.setBranchContractedIcon(originalUserObject
				.getBranchContractedIcon());
		clonedUserObject.setBranchExpandedIcon(originalUserObject
				.getBranchExpandedIcon());
		clonedUserObject.setExpanded(originalUserObject.isExpanded());
		clonedUserObject.setLeafIcon(originalUserObject.getLeafIcon());
		clonedWrapper.setUserObject(clonedUserObject);
		getWrapper().insert(clonedWrapper, 0);
	}

	/**
	 * Registers a user click with this object and updates the selected node in
	 * the TreeBean.
	 *
	 * @param event
	 *            that fired this method
	 */
	public void nodeClicked(ActionEvent event) {
		if (treeBean.getSelectedNodeObject()!=null) {
			treeBean.getSelectedNodeObject().setLeafIcon(ICON_LEAF);
			treeBean.getSelectedNodeObject().setBranchContractedIcon(ICON_FOLDER_CLOSED);
			treeBean.getSelectedNodeObject().setBranchExpandedIcon(ICON_FOLDER_OPEN);
		}
		setLeafIcon(ICON_LEAF_SELECTED);
		setBranchContractedIcon(ICON_FOLDER_CLOSED_SELECTED);
		setBranchExpandedIcon(ICON_FOLDER_OPEN_SELECTED);
		treeBean.setSelectedNodeObject(this);
		if (treeBean.getSelectedNodeObject().leaf) {
			treeBean.showDocumentDetails(treeBean.getSelectedNodeObject()
					.getRowIndex());
		}
		else if (!treeBean.getSelectedNodeObject().leaf) {
			treeBean.showFolderDetails(treeBean.getSelectedNodeObject()
					.getRowIndex());
		}
	}


	/**
	 * @param nodeToolTip2
	 */
	private void setTooltip(String nodeToolTip2) {
		// TODO Auto-generated method stub
	}
	/**
	 * @return
	 */
	private Object getAction() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * @param action
	 */
	private void setAction(Object action) {
		// TODO Auto-generated method stub
	}
	/**
	 * @return
	 */
	private DefaultMutableTreeNode getWrapper() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * @return
	 */
	private Object isExpanded() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * @param expanded
	 */
	public void setExpanded(Object expanded) {
		// TODO Auto-generated method stub
	}
	/**
	 * @return
	 */
	private String getBranchContractedIcon() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * @param iconFolderClosedSelected
	 */
	private void setBranchContractedIcon(String iconFolderClosedSelected) {
		// TODO Auto-generated method stub
	}
	/**
	 * @return
	 */
	private String getBranchExpandedIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param iconFolderOpenSelected
	 */
	private void setBranchExpandedIcon(String iconFolderOpenSelected) {
		// TODO Auto-generated method stub
	}

	/**
	 * @return
	 */
	Integer getRowIndex() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param id
	 */
	public void setRowIndex(Integer id) {
		// TODO Auto-generated method stub
	}
	/**
	 * @return
	 */
	public String getText() {
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * @param name
	 */
	public void setText(String name) {
		// TODO Auto-generated method stub
	}

	/**
	 * @return
	 */
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param b
	 */
	public void setLeaf(boolean b) {
		// TODO Auto-generated method stub
	}

	/**
	 * @return
	 */
	private String getLeafIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param iconLeafSelected
	 */
	private void setLeafIcon(String iconLeafSelected) {
		// TODO Auto-generated method stub
	}

}
