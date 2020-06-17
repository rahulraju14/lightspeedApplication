//	File Name:	LoadPostalFile.java
package com.lightspeedeps.web.admin;

/**
 * An interface for loading postal-location files.
 * LoadPostalFileImpl implements this interface.
 * The interface is necessary for Spring proxy-generation to work properly.
 */
public interface LoadPostalFile {

	public boolean loadFile(String file);

}
