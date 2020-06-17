//	File Name:	LoadPostal.java
package com.lightspeedeps.web.admin;

import java.io.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;
import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;

/**
 * A class for backing the "Load Postal Location file" web page.
 */
@ManagedBean
@RequestScoped
public class LoadPostalBean implements Serializable {
	/** */
	private static final long serialVersionUID = 6596363410557927L;

	private static final Log log = LogFactory.getLog(LoadPostalBean.class);

	private File m_importFile;
	private String m_importFileName;
	private String m_displayFilename;
	private File m_file;
	private boolean m_importOk = false;
	private boolean m_initialized = false;

	public LoadPostalBean() {
		log.debug("this="+this);
	}

	private void init() {
		log.debug("");
		// Since we're session-scope, we clear out these member variables in case this
		// is a return to the import page after a previous import finished.
		m_initialized = true;
		m_importOk = false;			// this will suppress "successful" msg if we return to this page
		m_displayFilename = "";
		m_importFileName = "";
		m_importFile = null;
		m_file = null;
	}

	public boolean getImportOk() {
		if (!m_initialized) {
			init();
		}
		return m_importOk;
	}
	public void setImportOk(boolean importOk) {
		this.m_importOk = importOk;
	}

	public String getDisplayFilename() {
		return m_displayFilename;
	}
	public void setDisplayFilename(String name) {
		m_displayFilename = name;
	}

	/**
	 * Returns "success" if the import was successful, "failure" if not.
	 */
	private String doLoad() {
		log.debug("file="+m_file + "importFileName="+m_importFileName);
		String res = Constants.FAILURE;
		log.info("import started; file=" + m_importFileName);
		boolean bRet = false;

		LoadPostalFile fileLoader = LoadPostalFileImpl.getInstance();
		try {
			log.info("loader=" + fileLoader);
			// Perform the import process!
			bRet = fileLoader.loadFile(m_importFileName);
			if (bRet) {
				res = Constants.SUCCESS;
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
		}
		setImportOk(bRet);			// show or hide the "Next step" button
		m_initialized = false;
		log.info("import file returned " + bRet);
		return res;
	}

	public void listenFileEntry(FileEntryEvent event) {
		FileEntry inputFileComponent = (FileEntry)event.getSource();
	    FileEntryResults results = inputFileComponent.getResults();
		FileInfo info = results.getFiles().get(0);
		if (info.isSaved()) {
			File file = new File(info.getFile().getAbsolutePath());
			this.m_importFile = file;
			log.debug("file="+m_importFile);
			m_file = m_importFile;	// copy so form get/post doesn't overlay it
			m_importFileName = m_importFile.getAbsolutePath().trim();
			log.debug("file loc="+m_importFileName);
			m_displayFilename = m_importFile.getName();
			doLoad();
		}
		else {
			MsgUtils.addFacesMessage( "ImportScript.UploadFailed", FacesMessage.SEVERITY_ERROR, m_importFile.getName());
		}
	}

	public String getImportFileName() {
		log.debug("file="+m_importFileName);
		return m_importFileName;
	}
	public void setImportFileName(String name) {
		m_importFileName = name;
		log.debug("file="+m_importFileName);
	}

}
