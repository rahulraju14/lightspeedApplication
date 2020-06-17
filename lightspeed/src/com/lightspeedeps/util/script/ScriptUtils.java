package com.lightspeedeps.util.script;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ColorNameDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TextLine;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;


public class ScriptUtils {
	private static final Log log = LogFactory.getLog(ScriptUtils.class);

	/** An annotated character line, e.g., "MR. BILLINGS (ON PHONE)": checking for parens; to
	 * handle some weird (error) cases, only the opening paren is required. */
	private static final String RE_CHARACTER_ANNOTATED = "(\\S.*)\\([^\\)]+(\\)|\\z)";
	private static final Pattern P_CHARACTER_ANNOTATED = Pattern.compile(RE_CHARACTER_ANNOTATED);

	/** An off-screen or voice-over Character line. */
	private static final String RE_OFF_SCREEN =
			"(.*)" +
			"(" +
				"(" +
					"\\(" +
					"((V.O.)|(VO)|(O.S.)|(OS))" + 	// typical "(V.O.) possibilities"
					"(\\s[^a-z]*)?" +				// ...and optional trailing annotation
					"\\)" +
				")" +
				"|( V/O)" +		// have seen examples of this style -- no parens
				"|( V.O.)" +
				"|( O/S)" +
				"|( O.S.)" +
			")" +
			"(\\s[^a-z]*)?" ;
	private static final Pattern P_OFF_SCREEN = Pattern.compile(RE_OFF_SCREEN);

	/** A List of SelectItems for populating a drop-down list of possible
	 * Script colors.  The value fields are the ColorName objects corresponding
	 * to each color name.  When this list is used in a drop-down, the
	 * ColorNameConverter should be specified as a converterid. */
	private static List<SelectItem> colorNameList; // shared across entire application

	private ScriptUtils() {
	}

	/**
	 * Insert a new scene into a script, and optionally create a Strip to match. An appropriate
	 * scene number is generated based on the insertion position.
	 *
	 * @param script The Script to which the scene will be added.
	 * @param priorScene The Scene after which the new scene should be inserted. If null, the new
	 *            Scene is inserted at the beginning of the script.
	 * @param copyLocation If true, the old scene's location will be copied into the new scene; if
	 *            false, the new scene's location will be left null.
	 * @param addStrip If true, a new Strip is created, associated with this scene, and added as an
	 *            unscheduled strip to the current stripboard.
	 * @return The newly-created Scene.
	 */
//	public Scene insertNewScene( Script script, Scene priorScene, boolean copyLocation, boolean addStrip ) {
//		Scene scene = makeInsertCopy(script, priorScene, copyLocation, null);
//		sceneDAO.save(scene);
//		log.info("new scene Id::"+scene.getId());
//		script.getScenes().add(scene);
//		if (scene.getPageNumber() > script.getLastPage()) {
//			script.setLastPage(scene.getPageNumber());
//		}
//		script = scriptDAO.merge(script);
//		if (addStrip) {
//			insertStrip(scene, SessionUtils.getCurrentProject());
//		}
//		return scene;
//	}

	/**
	 * Create a new Scene object to be inserted into the script being edited.
	 *
	 * @param script The script into which the scene is being inserted.
	 * @param oldScene If oldScene is not null, it indicates the new Scene will
	 *            be inserted following that one. If null, the new Scene will be
	 *            inserted at the beginning of the script. The new scene number
	 *            will be computed differently in the two cases.
	 * @param copyLocation If true, the old scene's location will be copied into
	 *            the new scene; if false, the new scene's location will be left
	 *            null.
	 * @param newSceneNumber The scene number to be assigned to the newly
	 *            created scene. If null, the program will create an appropriate
	 *            scene number.
	 * @param copyScene The source scene for D/N, I/E and location values, if
	 *            not null; it may be the same as or different from 'oldScene'.
	 * @return The newly-created Scene. If oldScene was provided, appropriate
	 *         fields in the new Scene will be copied from the old one, such as
	 *         set location, day/night, INT/EXT, script day and hint. The page
	 *         number will be computed from the old scene's page number plus its
	 *         page length. Page length is set to 1/8th.
	 */
	public static Scene makeInsertCopy(Script script, Scene oldScene, boolean copyLocation,
			String newSceneNumber, Scene copyScene ) {
		SceneDAO sceneDAO = SceneDAO.getInstance();
		Scene newScene = new Scene();
		newScene.setScript(script);
		newScene.setLength(1);
		newScene.setLastRevised(script.getRevisionNumber());
		if (newSceneNumber == null || newSceneNumber.trim().length()==0) {
			newSceneNumber = sceneDAO.createNextSceneNumber(script, oldScene);
		}
		newScene.setNumber(newSceneNumber.trim().toUpperCase());
		Scene nextScene;
		if (oldScene != null) {
			if (copyScene == null) {
				copyScene = oldScene;
			}
			newScene.setPageNumber( oldScene.getPageNumber() + (oldScene.getLength()/8) );
			newScene.setPageNumStr(copyScene.getPageNumStr());
			newScene.setDnType(copyScene.getDnType());
			newScene.setIeType(copyScene.getIeType());
			newScene.setScriptDay(copyScene.getScriptDay());
			if (copyLocation) {
				newScene.setScriptElement(copyScene.getScriptElement());
			}
			newScene.setHint(copyScene.getHint());
			// now figure out the new sequence value to keep scenes in order
			nextScene = sceneDAO.findNextInSequence(oldScene.getSequence(), script);
			if (nextScene == null) { // inserting at end of script
				newScene.setSequence(oldScene.getSequence() + SceneDAO.SEQUENCE_INCREMENT);
			}
			else {
				int seqdiff = nextScene.getSequence() - oldScene.getSequence();
				if (seqdiff <= 1) { // problem - no room for new sequence between these two.
					sceneDAO.resequenceScenes(script);	// renumber everything
					oldScene = sceneDAO.findById(oldScene.getId());	// get a fresh copy of the old scene
					// ...and use its sequence + 1/2 of the standard increment value
					newScene.setSequence(oldScene.getSequence() + (SceneDAO.SEQUENCE_INCREMENT/2));
				}
				else { // enough room, split the gap between these two sequence values
					newScene.setSequence(oldScene.getSequence() + (seqdiff/2));
				}
			}
		}
		else { // inserting before the first existing scene
			newScene.setPageNumber(1);
			newScene.setPageNumStr("1");
			if (script.getScenes().size() > 0) {	// at least one existing scene
				nextScene = script.getScenes().get(0); // get the first scene in List
				int nextseq = nextScene.getSequence();
				if (nextseq > 1) {	// its sequence is high enough
					newScene.setSequence(nextseq/2); // split it in half for the new one
				}
				else { // first scene is already sequence 1; re-sequence all the scenes
					sceneDAO.resequenceScenes(script);
					// then use 1/2 the "standard increment" for the new scene's sequence number
					newScene.setSequence(SceneDAO.SEQUENCE_INCREMENT/2);
				}
				if (copyScene == null) {
					copyScene = nextScene;
				}
				newScene.setPageNumStr(copyScene.getPageNumStr());
				newScene.setDnType(copyScene.getDnType());
				newScene.setIeType(copyScene.getIeType());
				newScene.setScriptElement(copyScene.getScriptElement());
				if (copyLocation) {
					newScene.setScriptElement(copyScene.getScriptElement());
				}
			}
			else {	// no scenes yet -- this is the first.
				newScene.setDnType(DayNightType.DAY);
				newScene.setIeType(IntExtType.INTERIOR);
				newScene.setSequence(SceneDAO.SEQUENCE_INCREMENT);
			}
		}
		return newScene;
	}

	/**
	 * Determine if the supplied "new" scene number is a valid format and not a duplicate of an
	 * existing scene number.
	 *
	 * @param script The script which will contain the new scene; used to check for a duplicate
	 *            scene number.
	 * @param newSceneNumber The scene number to be validated.
	 * @return null if the scene number passes the tests; else a message id (contained in our
	 *         message resource file) that can be used in a call to MsgUtils.addFacesMessage.
	 */
	public static String validateSceneNumber(Script script, String newSceneNumber) {
		String msgId = null;
		if ( ! SceneNumber.isValidFormat(newSceneNumber)) { // invalid pattern
			msgId = "SceneList.InvalidSceneNumber";
		}
		else if (SceneDAO.getInstance().findByScriptAndNumber(script, newSceneNumber) != null) {
			msgId = "SceneList.DuplicateSceneNumber";
		}
		return msgId;
	}

	/**
	 * Insert a new Strip to match a newly-inserted Scene, if needed. Adds it as
	 * an unscheduled Strip to the current stripboard.
	 *
	 * @param newScene The Scene which the new Strip should specify.
	 * @return The Strip which matches the supplied Scene (contains its scene
	 *         number). This may be a Strip that was just created and added to
	 *         the current Stripboard, or an existing Strip that was found which
	 *         matched the given scene number.
	 */
//	public Strip insertStrip(Scene newScene, Project project) {
//		Stripboard stripboard = project.getStripboard();
//		Strip strip = null;
//		if ( stripboard != null) {
//			// see if a Strip already exists (maybe created due to earlier Script revision)
//			strip = stripDAO.findBySceneAndStripboard(newScene.getNumber(), stripboard);
//			if (strip == null) { // need to create one
//				strip = stripDAO.addStrip(stripboard, -1, -1, newScene);
//				// That added it to db & updated the stripboard too.
//			}
//			else {
//				stripDAO.updateStrip(stripboard, strip, newScene);
//			}
//		}
//		return strip;
//	}

//	/**
//	 * Create a new Stripboard, and populate it with Strips matching the Scenes in the supplied
//	 * Script. If the project does not have a default Script, the one given will be set as the
//	 * default; if there is no default Stripboard, the one created will be made the default.
//	 *
//	 * @param project The project containing the Script and Stripboard.
//	 * @param script The Script containing Scenes to be associated with the new Stripboard.
//	 * @param title The title to assign the new Stripboard.
//	 */
//	public Project initStripboard(Project project, Script script, String title) {
//		project = stripboardDAO.initStripboard(project, script, title);
//		return project;
//	}

	/**
	 * Replace all the references within a Script to a particular ScriptElement
	 * with a reference to a different ScriptElement.
	 *
	 * @param script The Script to be updated.
	 * @param source The ScriptElement to be removed from all Scene's in the
	 *            Script.
	 * @param target The ScriptElement which will be added to all Scene's that
	 *            had a reference to the 'source' element.
	 * @return The number of occurrences that were replaced, i.e., the number of
	 *         Scene's which referenced the source element.
	 */
	public static int replaceScriptElement(Script script, ScriptElement source, ScriptElement target) {
		int count = 0;
		List<Scene> scenes = ScriptElementDAO.getInstance().getOccurs(source, script);
		for (Scene scene : scenes) {
			boolean found = scene.getScriptElements().remove(source);
			if (found) {
				count++;
			}
			scene.getScriptElements().add(target);
		}
		ProductionDood.markProjectDirty(script.getProject(), source.getType()==ScriptElementType.CHARACTER);
		return count;
	}

	/**
	 * Remove any recognizable annotations from "name" and return the
	 * (remaining) character name. Markups such as (CONT'D), (O.S.) and so forth
	 * are recognized.
	 *
	 * @param name The String containing a Character name to extract.
	 * @param line NOT USED. (WAS: An optional TextLine object - if not null, and if an
	 *            "off-screen" or "voice over" annotation is found, then the
	 *            TextLine.offScreen flag will be set to true.)
	 * @return the "extracted" character name
	 */
	public static String extractCharacterName(String name, TextLine line) {
		//log.debug("`" + name + "`");
		int ix = name.indexOf("(CONT'D)");
		if (ix >= 0) {
			name = name.substring(0,ix) + name.substring(ix+8);
			name = name.trim();
		}
		Matcher matcher = P_OFF_SCREEN.matcher(name);
		if (matcher.matches()) {
//			Never used ... comment out in rev 2873
//			if (line != null) {
//				line.offScreen = true;
//			}
			name = matcher.group(1).trim();
		}
		matcher = P_CHARACTER_ANNOTATED.matcher(name);
		if (matcher.matches()) {
			name = matcher.group(1).trim();
		}
		name = name.replace("  ", " "); // doubled spaces seen in some import files
		//log.debug("`" + name + "`");
		return name;
	}

//	/**
//	 * Format a scene for printing. Given the Script and a SceneNumber, this looks up the Scene
//	 * object, and formats it. See {@link ScriptReporter#formatScene(Scene)}.
//	 *
//	 * @param script The script whose scene should be formatted.
//	 * @param sceneNumber The scene number (alphanumeric) of the scene to format.
//	 * @return The formatted text, using new-lines and blanks for padding.
//	 */
//	// Rev 3214 - obsolete - originally used to print scenes for emailing as text (not PDF)
//	public String formatScene(Script script, String sceneNumber) {
//		Scene scene = sceneDAO.findByScriptAndNumber(script, sceneNumber);
//		return ScriptReporter.formatScene(scene);
//	}

	/**
	 * Determine the last physical page on which text from the given Scene
	 * appears. Since the TextElement's that a Scene is associated extends until
	 * the next scene header, the last TextElement may be, for example, just a
	 * page header on the next page, in which case we don't need (or want) to
	 * print that page if we're just formatting one Scene's output. So we look
	 * at the last TextElement of the Scene, and back up until we find some
	 * "real" data (not blank or headings).
	 *
	 * @param scene The Scene for which the last page is to be found.
	 * @return The physical page number containing the last actual text of the
	 *         Scene, ignoring blank lines and page or act headings.
	 */
	public static int findLastPage(Scene scene) {
		int lastPage = scene.getPageNumber();
		if (scene.getTextElements() != null && scene.getTextElements().size() > 0) {
			int count = scene.getTextElements().size();
			TextElement last = null;
			for (int i=count-1; i >= 0; i--) {
				last = scene.getTextElements().get(i);
				if (last.getType() != TextElementType.PAGE_HEADING &&
						last.getType() != TextElementType.PAGE_FOOTER &&
						last.getType() != TextElementType.BLANK &&
						last.getType() != TextElementType.START_ACT) {
					break;
				}
			}
			lastPage = last.getPage().getNumber();
		}
		log.debug("scn=" + scene.getNumber() + ", lastPage=" + lastPage);
		return lastPage;
	}

	/**
	 * Determine the revision number for the given page, by comparing it to the
	 * prior script's page with the same logical page number.
	 *
	 * @param pPage The Page whose revision number is to be revised, if
	 *            possible.
	 * @param priorScript The Script prior to the current one, whose Page hash
	 *            value we will compare to this Page's hash value.
	 */
	public static void updateRevision(Page pPage, Script priorScript) {
		boolean bRevised = true;
		if (priorScript != null && priorScript.getPages() != null &&
				priorScript.getPages().size() > 0) {
			Page priorPage = findPage(priorScript, pPage.getNumber(), pPage.getPageNumStr());
			if (priorPage != null) {
				bRevised = ! StringUtils.equals(priorPage.getHash(), pPage.getHash());
			}
			else {
				log.debug("priorPage null, number="+pPage.getNumber());
			}
			if ( ! bRevised) {
				pPage.setLastRevised(priorPage.getLastRevised());
			}
		}
		log.debug("page is revised=" + bRevised);
	}

	/**
	 * Find the Page from the given script whose logical page number matches
	 * 'numStr'. The physical page number is supplied only as a "hint" that may
	 * speed up the match.
	 *
	 * @param pScript The Script to be searched.
	 * @param number The possible matching physical page number.
	 * @param numStr The actual logical page number to be matched.
	 * @return The Page from 'pScript' whose logical page number is equal to
	 *         'numStr', or null if no such Page is found.
	 */
	private static Page findPage(Script pScript, int number, String numStr) {
		Page pg = null;
		int index = number - pScript.getPages().get(0).getNumber();
		if (index >= 0 && index < pScript.getPages().size()) {
			pg = pScript.getPages().get(index);
			if (! pg.getPageNumStr().equals(numStr)) {
				log.debug("index found wrong page, pg.getPageNumStr=" + pg.getPageNumStr() + " != " + numStr);
				pg = null;
			}
		}
		if (pg == null) {
			for (Page p : pScript.getPages()) {
				if (p.getPageNumStr().equals(numStr)) {
					pg = p;
					log.debug("loop matched pg");
					break;
				}
			}
		}
		return pg;
	}

	public static void setDefaultValues(ScriptElement se, Project project) {
		if (project == null) {
			project = SessionUtils.getCurrentProject();
		}
		se.setProject(project);
		setDefaultRWERequired(se);
		if (se.getType() == ScriptElementType.CHARACTER) {
			se.setDropPickupAllowed(project.getUseHoldDrop());
			se.setDaysHeldBeforeDrop(project.getDaysHeldBeforeDrop());
		}
	}

	public static void setDefaultRWERequired(ScriptElement se) {
		boolean req = true; // all default to true (9/22/11 rev 2223)
//		ScriptElementType type = se.getType();
//		if (type == ScriptElementType.CHARACTER ||
//				type == ScriptElementType.EXTRA ||
//				type == ScriptElementType.VEHICLE ||
//				type == ScriptElementType.ANIMAL ||
//				type == ScriptElementType.LOCATION) {
//			req = true;
//		}
		se.setRealElementRequired(req);
	}

	/** See {@link #colorNameList}. */
	public static List<SelectItem> getColorNameList() {
		if (colorNameList == null) {
			createColorList();
		}
		return colorNameList;
	}

	/**
	 * Create a list of all possible script colors.  Used to populate
	 * some drop-down lists.  The values are the ColorName objects.
	 */
	private static void createColorList() {
		List<ColorName> colors = ColorNameDAO.getInstance().findAllScriptColors();
		List<SelectItem> cNameList = new ArrayList<SelectItem>();
		for (ColorName color : colors) {
			cNameList.add(new SelectItem(color, color.getName()));
		}
		colorNameList = cNameList;
	}

}
