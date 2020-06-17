//	File Name:	ImportManualSetup.java
package com.lightspeedeps.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.util.app.EventUtils;

import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import static com.lightspeedeps.type.DayNightType.*;
import static com.lightspeedeps.type.IntExtType.*;

/**
 * A class for importing scripts saved in Movie Magic ".sex" format.
 */
@ManagedBean
@ViewScoped
public class ImportManualSetup extends ImportFileImpl implements Serializable {
	/** */
	private static final long serialVersionUID = - 3801084412518967180L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ImportManualSetup.class);

	Map<String, ScriptElement> elementMap = new HashMap<String, ScriptElement>();

	public ImportManualSetup() {
	}

	/**
	 * Called from superclass to import the file.  All parameters have been stored
	 * in member variables.
	 */
	@Override
	protected boolean doImport() {
		boolean bRet = false;
		startTransaction(); // only needed in batch
		try {
			createScript(ImportType.MANUAL);

			bRet = createFirstScene();
			if (bRet) {
				doFinalUpdates();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			bRet = false;
		}
		endTransaction(bRet);

		return bRet;
	}

	/**
	 * For Manual import, create the first Scene and add it to the Script.  Create
	 * a new stripboard and initialize it to match the script, i.e., with one Strip.
	 */
	private boolean createFirstScene() {
		boolean bRet = true;
		Scene scene = new Scene();
		scene.setNumber("1");
		scene.setIeType(INTERIOR);
		scene.setDnType(DAY);
		scene.setScriptElement(getLocation("FIRST LOCATION"));
		scene.setScriptElements(new HashSet<ScriptElement>());
		scene.setLength(1);
		scene.setPageNumber( 1 );
		scene.setScript(script);
		scene.setSequence(SceneDAO.SEQUENCE_INCREMENT);
		scene.setLastRevised(script.getRevisionNumber());
		addScene(scene, false);
		script.setLastPage(1);
		// create stripboard & build first strip
		project = StripboardDAO.getInstance().initStripboard(project, script, script.getDescription());

		return bRet;
	}

}
