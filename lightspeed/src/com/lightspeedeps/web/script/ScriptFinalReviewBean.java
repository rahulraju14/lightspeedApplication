//	File Name:	ScriptFinalReviewBean.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

@ManagedBean
@ViewScoped
public class ScriptFinalReviewBean extends ScriptReviewBean implements Serializable {
	/** */
	private static final long serialVersionUID = 2979550448147683801L;
	private static final Log log = LogFactory.getLog(ScriptFinalReviewBean.class);

	public ScriptFinalReviewBean() {
		//log.debug(""+this);
	}

	@Override
	@PostConstruct
	protected void init() {
		log.debug(""+this);
		setFinalReview(true);
		try {
			super.init();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_REVISIONS_IX);
		}
	}

	/**
	 * The sub-class version of this does nothing as it does not use
	 * the map;  our class does, so here we ensure that it exists and is
	 * up to date.
	 */
	@Override
	public Map<String, Integer> getBreakdownMap() {
		return checkBreakdownMap();
	}

	/**
	 * Called when user hits "Done" button to exit this page and
	 * continue to the Script | Drafts page.
	 */
	public void exitFinalReview(ActionEvent evt) {
		log.debug("");
		if (! busy) { // skip if user double-clicked "Finish" button
			busy = true;
			try {
				saveScript();	// save any scene changes
				sceneViewList.clear(); // in case we're called again by a double-click, don't resave scenes!
				// Clear the script id, so we'll go back to default script for Scene List page.
				SessionUtils.put(Constants.ATTR_IMPORT_SCRIPT_ID, null);
				if (script.getImportType() == ImportType.FDX || script.getImportType() == ImportType.TAGGER) {
					scriptDAO.evict(script);
					ScriptFormatter formatter = new ScriptFormatter();
					script = formatter.format(script); // get revised Script instance
				}
				HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_REVISIONS_IX);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			finally {
				log.debug("done");
				busy = false;
			}
		}
	}

	/**
	 * Insert a new Strip to match a newly-inserted Scene.  Adds it as
	 * an unscheduled Strip to the current stripboard.
	 * @param newScene The Scene which the new Strip should specify.
	 */
	@Override
	protected Strip insertStrip(Scene newScene) {
		Strip strip = doInsertStrip(newScene);
		return strip;
	}

	/**
	 * Called during initialization (post-construction) to take care of
	 * initializing a stripboard so that it matches the script under review.
	 */
	@Override
	protected void checkStripboard() {
		// Create Strips for any new Scenes in this script.
		// Scenes added by this revision won't have existing strips in the current stripboard.
		fillStripboard();	// this updates (or creates) the 'current' (active) stripboard.
	}

//	protected void mergeBreakdown( Map<String, Integer> breakdownMap, Integer pSceneId, String nextSceneNum) {
//		boolean exists = sceneDAO.existsSceneInProject(nextSceneNum, project);
//		log.debug("scene="+nextSceneNum+", exists="+exists);
//		// if the following scene number (the one that got merged out of existence)
//		// exists in some other script in this project, then we do NOT remove the strip.
//		if (! exists) {
//			Scene scene = sceneDAO.findById(pSceneId);
//			if (scene != null) {
//				Integer prevSheetNumber = breakdownMap.get(scene.getNumber());
//				Integer nextSheetNumber = breakdownMap.get(nextSceneNum);
//				stripDAO.mergeNext(project.getStripboard(), prevSheetNumber, nextSheetNumber);
//			}
//		}
//	}

}
