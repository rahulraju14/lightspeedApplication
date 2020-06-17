/**
 * FolderTreeBean.java
 */
package com.lightspeedeps.web.onboard;

import org.icefaces.ace.model.tree.NodeStateMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Folder;
import com.lightspeedeps.object.FolderNodeImpl;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.file.FileRepositoryUtils;

/**
 *
 */
@ManagedBean
@ViewScoped
public class FolderTreeBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FolderTreeBean.class);

	/**
	 *
	 */
	public FolderTreeBean() {
	}

	/** Used to return our current instance. */
	public static FolderTreeBean getInstance() {
		return (FolderTreeBean)ServiceFinder.findBean("folderTreeBean");
	}

	private List<FolderNodeImpl> treeRoots;
	private NodeStateMap stateMap;
	private boolean singleSelect = true;

	private void createTreeRoots() {
		treeRoots = new ArrayList<>();
		Folder root = FileRepositoryUtils.findFolder("Onboarding", FileRepositoryUtils.getRoot());
		treeRoots.add(new FolderNodeImpl(null, root));
	}

	public List<FolderNodeImpl> getTreeRoots() {
		if (treeRoots == null) {
			createTreeRoots();
		}
		return treeRoots;
	}

	public NodeStateMap getStateMap() {
		return stateMap;
	}

	public void setStateMap(NodeStateMap stateMap) {
		this.stateMap = stateMap;
	}

	public boolean isSingleSelect() {
		return singleSelect;
	}

	public void setSingleSelect(boolean singleSelect) {
		this.singleSelect = singleSelect;
	}

	/* Proxy method to avoid JBossEL accessing stateMap like map for method invocations */
	@SuppressWarnings("rawtypes")
	public List getSelected() {
		if (stateMap == null)
			return Collections.emptyList();
		return stateMap.getSelected();
	}

}
