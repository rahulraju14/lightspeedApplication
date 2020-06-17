/**
 * RepositoryBean.java
 */
package com.lightspeedeps.web.repository;

/**
 * The interface required by callers of DynamicNodeUserObject.
 */
public interface RepositoryBean {

	public abstract String getUserFileName();

	public abstract void showFolderDetails(Integer folderID);

	public abstract void showDocumentDetails(Integer documentID);

	public abstract DynamicNodeUserObject getSelectedNodeObject();

	public abstract void setSelectedNodeObject(DynamicNodeUserObject selectedNodeObject);

}
