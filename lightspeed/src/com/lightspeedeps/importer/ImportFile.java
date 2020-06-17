//	File Name:	ImportFile.java
package com.lightspeedeps.importer;

import com.lightspeedeps.model.ColorName;
import com.lightspeedeps.model.Project;

/**
 * An interface for importing script files.
 * ImportFileImpl implements this interface.
 * The interface is necessary for Spring proxy-generation to work properly.
 */
public interface ImportFile {

	public boolean importFile(String file,
			String desc, Project project, ColorName colorName, boolean includeText, boolean includeSceneElements);

	public String getMessageLog();

	public int getNewScriptElements();
	public int getOldScriptElements();
	public int getNewLocations();
	public int getScenesAdded();
	public int getPageCount();

	public boolean assignCastIds();

}
