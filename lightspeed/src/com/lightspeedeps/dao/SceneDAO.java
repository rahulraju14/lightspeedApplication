//	File Name:	SceneDAO.java
package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Note;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.TextElement;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.script.ScenePair;

/**
 * A data access object (DAO) providing persistence and search support for Scene
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Scene
 */
public class SceneDAO extends BaseTypeDAO<Scene> {
	private static final Log log = LogFactory.getLog(SceneDAO.class);
	ScriptDAO scriptDAO;
	// property constants
//	private static final String HINT = "hint";
//	private static final String NUMBER = "number";
//	private static final String SEQUENCE = "sequence";
//	private static final String IE_TYPE = "ieType";
//	private static final String DN_TYPE = "dnType";
//	private static final String SCRIPT_DAY = "scriptDay";
//	private static final String PAGE_NUMBER = "pageNumber";
//	private static final String LENGTH = "length";
//	private static final String LAST_REVISED = "lastRevised";
	public static final int SEQUENCE_INCREMENT = 100;
	//public static final int NUMBER_INCREMENT = 1;

	private static final Pattern P_SENTENCE = Pattern.compile(
			"(?s-i)" +			// flags: '.' includes line-ends; set case-insensitive Off
			"\\s*[A-Z\\d]" + 	// Sentence starts with (spaces) cap letter or digit;
			"(" +				// Grouping of things that might have ". " but are not sentence end
						// following line is for "mon. dd" or "mon. yyyy"
			"(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.\\s+\\d{1,4}" +
			"|Mr\\.\\s|Mrs\\.\\s|Ms\\.\\s|Dr\\.\\s|" + // any more patterns with period followed by space that aren't sentence-end?
			"[^!?]" +	// any non-sentence-ending letters
			")*?" +		// lots of times, but reluctant quantifier
			"[.!?]" +	// valid sentence-end symbols
			"(?=\\s|$)"	);	// followed by space or end of input, via "zero-width positive lookahead"


	public static SceneDAO getInstance() {
		return (SceneDAO)getInstance("SceneDAO");
	}

	protected ScriptDAO getScriptDAO() {
		if (scriptDAO == null) {
			scriptDAO = ScriptDAO.getInstance();
		}
		return scriptDAO;
	}

//	public static SceneDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (SceneDAO) ctx.getBean("SceneDAO");
//	}

	/**
	 * Find a Scene with a specific scene number, in the Script specified. There should
	 * never be more than one scene in a script with the same number.
	 *
	 * @param sceneNumber The scene number (which may contain alphas, e.g., "12A").
	 * @param script The script containing the scene to be matched.
	 * @return A List which (normally) should contain a single Scene or no scenes; never returns null.
	 */
//	@SuppressWarnings("unchecked")
//	public List<Scene> findBySceneNumberScript(String sceneNumber, Script script) {
//		log.debug("getting Scene number: " + sceneNumber + " from script id: " + script.getId());
//		try {
//			Object values[] = { sceneNumber, script };
//			String queryString = " from Scene s where  s.number =? and s.script = ?";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Find by page number and script id
	 *
	 * @param pageNumber
	 * @param script_Id
	 * @return
	 */
//	@SuppressWarnings("unchecked")
//	public List<Scene> findByPageAndScript(Integer pageNumber, Integer script_Id) {
//		log.debug("getting Scene instance with id: " + script_Id);
//		try {
//			Object values[] = { script_Id, pageNumber };
//			String queryString = " from Scene s where  s.script.id =? and  s.pageNumber =? order by s.sequence ";
//			return find(queryString, values);
//
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Return a List of Scenes, in script order, that match a series of scene numbers supplied in
	 * "numbers", which are in "script".
	 *
	 * @param sceneNumbers A List of scene numbers, e.g., {"3","5A","7"}. May be a single scene
	 *            number.
	 * @param script The script whose scenes are to be searched.
	 * @return The List of matching Scenes, in scene.sequence order. Returns null if either
	 *         parameter is null, and returns empty list if no matches are found.
	 */
	@SuppressWarnings("unchecked")
	public List<Scene> findByNumbersAndScript(Collection<String> sceneNumbers, Script script) {
		if (sceneNumbers == null || script == null) {
			return new ArrayList<>();
		}
		String numberString = StringUtils.getStringFromList(sceneNumbers, "','");
		try {
			Object values[] = { script };
			String queryString = "from Scene s where s.number in ('" + numberString + "') and s.script = ? " +
					" order by s.sequence";
			log.debug("query=" + queryString);
			return find(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}

	}

//	@SuppressWarnings("unchecked")
//	public List<Scene> findByPageNumberAndScript(Integer pagenumber, Integer script_id) {
//		try {
//			Object values[] = { pagenumber, script_id };
//			String queryString = " from Scene s where s.pageNumber= ? and s.script.id=? order by s.sequence";
//			return find(queryString, values);
//		}
//		catch (RuntimeException re) {
//			EventUtils.logError(re);
//			throw re;
//		}
//	}

	/**
	 * Return the List of Scenes whose starting page number is within the
	 * (inclusive) specified range of pages.
	 *
	 * @param fromPagenumber The start of the page range.
	 * @param toPagenumber The end of the page range.
	 * @param script The script whose scenes are being selected.
	 * @return A (possibly empty, but not null) List of Scenes. The List
	 *         includes every Scene in the given script where the starting page
	 *         number of the Scene is within the inclusive range of pages given.
	 *         The Scenes are in script order.
	 */
	@SuppressWarnings("unchecked")
	public List<Scene> findByPageRangeAndScript(Integer fromPagenumber, Integer toPagenumber, Script script) {
		log.debug("find page range, from=" + fromPagenumber + " to " + toPagenumber);
		try {
			Object values[] = { fromPagenumber, toPagenumber, script };
			String queryString = "from Scene s where s.pageNumber between ? and ? and s.script=? order by s.sequence";
			if (fromPagenumber > toPagenumber) {
				values[0] = toPagenumber;	// reverse the page number range
				values[1] = fromPagenumber;
			}
			return find(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

//	/**
//	 * Create a string consisting of the element_id's of the provided Set of
//	 * ScriptElements, separated by commas, in ascending numeric order,
//	 * without duplicates.
//	 * @param scriptElementsSet
//	 * @return
//	 */
/*	public static StringBuffer getScriptElementIdList(Set<ScriptElement> scriptElementsSet) {
		Set<Integer> scriptElements = new TreeSet<Integer>();
		for (ScriptElement elem : scriptElementsSet) {
			if (elem.getType() == ScriptElementType.CHARACTER) {
				scriptElements.add(elem.getElementId());
			}
		}
		StringBuffer elements = new StringBuffer();
		boolean first = true;
		for (Integer id : scriptElements) {
			if ( ! first) {
				elements.append(", ");
			}
			elements.append(id);
			first = false;
		}
		log.debug("elem ids=" + elements);
		return elements;
	}
*/

/*	public List<StripBoardSceneBean> getListBasedOnFilter(
			List<StripBoardSceneBean> list, String filterIE, String filterDN) {
		List<StripBoardSceneBean> filteredUnScheduledList = new ArrayList<StripBoardSceneBean>();
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				StripBoardSceneBean stripBoardSceneBeanObj = list.get(i);
				if (filterDN.equalsIgnoreCase("ALL")) {
					if (stripBoardSceneBeanObj.getIntEType()!=null && stripBoardSceneBeanObj.getIntEType().equalsIgnoreCase(
							filterIE.substring(0, 1))) {
						filteredUnScheduledList.add(stripBoardSceneBeanObj);
					}
				} else if (filterIE.equalsIgnoreCase("ALL")) {
					if (stripBoardSceneBeanObj.getDayNType()!=null &&stripBoardSceneBeanObj.getDayNType().equalsIgnoreCase(
							filterDN.substring(0, 1))) {
						filteredUnScheduledList.add(stripBoardSceneBeanObj);
					}
				}
				else if (stripBoardSceneBeanObj.getIntEType()!=null && stripBoardSceneBeanObj.getDayNType()!=null && stripBoardSceneBeanObj.getDayNType().equalsIgnoreCase(
						filterDN.substring(0, 1))
						&& stripBoardSceneBeanObj.getIntEType()
								.equalsIgnoreCase(filterIE.substring(0, 1))) {
					filteredUnScheduledList.add(stripBoardSceneBeanObj);
				}
			}
		}
		return filteredUnScheduledList;
	}
*/

	public static List<String> getScriptElements(Set<ScriptElement> scriptElementsSet, ScriptElementType type) {
		Set<String> scriptElementsName = new TreeSet<>();
		for (ScriptElement element : scriptElementsSet) {
			// find the matching elements & gather the names
			if (element.getType() == type) {
				scriptElementsName.add(element.getName());
			}
		}
		// Generate a List -- it will be sorted due to TreeSet used above.
		List<String> elements = new ArrayList<>(scriptElementsName);
		return elements;
	}

	/**
	 * Find a Scene with a specific scene number, in the Script specified. There
	 * should never be more than one scene in a script with the same number.
	 *
	 * @param script The script containing the scene to be matched.
	 * @param number The scene number (which may contain alphas, e.g.,
	 *            "12A").
	 * @return Scene object matching the parameters. Returns null if not found.
	 */
	public Scene findByScriptAndNumber(Script script, String number) {
		final String FIND_BY_NUMBER =
			"from Scene s where s.script = ? and s.number = ?";
		Scene scene = findOne(FIND_BY_NUMBER, new Object[]{script,number});
		if (scene == null) {
			log.info("requested Scene " + number +" in script " + script.getId() + " not found");
		}

		return scene;
	}

	/**
	 * Given a Scene.sequence value, find the scene that would follow
	 * that one in sequence order.
	 * @param seqno the sequence value to be exceeded.
	 * @param script the script whose scenes are being searched.
	 * @return The scene whose sequence value would place it immediately following
	 * the given seqno value.
	 */
	public Scene findNextInSequence(int seqno, Script script) {
		final String FIND_NEXT_IN_SEQUENCE =
			"from Scene s where s.sequence > ? and s.script = ? order by s.sequence";
		Object values[] = { seqno, script };
		Scene scene = findOne(FIND_NEXT_IN_SEQUENCE, values);
		return scene;
	}

//	/**
//	 * Merge the given scene with the one following it, including ScriptElements, TextElements,
//	 * and the corresponding breakdown sheets.
//	 * @param script - The Script to which the scene belongs.
//	 * @param sceneId - The id of the scene to be kept; the one following it
//	 * in sequence order will be merged into it.
//	 * ** NOT CURRENTLY USED - was used for old Script Review page **
//	 */
//	@Transactional
//	public String mergeNext(Script script, int sceneId) {
//		Scene scene1 = findById(sceneId);
//		Scene scene2 = null;
//		String nextSceneNum = null;
//		if (scene1 != null) {
//			int seqno = scene1.getSequence();
//			scene2 = findNextInSequence(seqno, script);
//			if (scene2 != null) {
//				nextSceneNum = scene2.getNumber();
//				// Merge the scene data - length, script elements, text elements
//				scene1.merge(scene2);
//
//				// TODO merge breakdown sheets / Strips
//
//				// remove following scene from script
//				if (script != null) {
//					Collection<Scene> scenes = script.getScenes();
//					if (scenes != null) {
//						boolean done = scenes.remove(scene2);
//						log.debug("removed = "+done);
//					}
//					//script = getScriptDAO().merge(script);
//				}
//				// finally, delete the second from db
//				delete(scene2);
//				log.debug("next scene deleted");
//			}
//		}
//		return nextSceneNum;
//	}

	/**
	 * Get a list of Scenes for a Script from a list of scene numbers.
	 * @param sceneNumbers List<String> where each entry is a (possibly un-trimmed) Scene number.
	 * @return List<Scene> comprising all the scenes with matching scene numbers
	 * from the specified Script, in the same order as the list of scene numbers provided.
	 * Never returns null.  Will return an empty List if no matching scenes are found.
	 */
	public List<Scene> getScenes(Script script, List<String> sceneNumbers) {
		List<Scene> scenes = new ArrayList<>();
		for (String sn : sceneNumbers) {
			Scene scene = findByScriptAndNumber(script, sn.trim());
			if (scene != null) {
				scenes.add(scene);
			}
		}
		return scenes;
	}

	/**
	 * Returns the (first) Scene that corresponds to the given Strip.
	 *
	 * @param strip The Strip for which the matching Scene is to be found.
	 * @param script The Script to search for the scene number.
	 * @return The first Scene found in the specified Script whose scene number
	 *         matches one of the scene numbers listed in the given Strip.
	 *         Returns null if the current Script does not contain any scene
	 *         number in the Strip.
	 */
	public Scene findByStripAndScript(Strip strip, Script script) {
		Scene scene = strip.getScene();
		if (scene == null) {
			List<String> list = strip.getScenes();
			for (String sceneNum : list) {
				scene = findByScriptAndNumber(script, sceneNum);
				if (scene != null) {
					strip.setScene(scene);
					break;
				}
			}
		}
		return scene;
	}

	/**
	 * Determines if the given scene number exists in any script in the give project.
	 * @param sceneNum The scene number to look for.
	 * @param project The project in which to check.
	 * @return True if any script in the project contains a Scene with the given
	 * scene number; false otherwise.
	 */
	public boolean existsSceneInProject(String sceneNum, Project project) {
		String queryString = "from Scene s where s.number = ? and s.script.project = ?";
		boolean ret = exists(queryString, new Object[]{sceneNum,project});
		return ret;
	}

	@Transactional
	public Scene saveScene(Script script, Scene scene, Strip strip, Collection<Note> addedNotes, List<Scene> mergedScenes) {
		if (addedNotes != null) {
			// Save any notes added during edit mode...
			for (Note note : addedNotes) {
				save(note);
			}
		}
		Scene newScene = scene;
		for (Scene s : mergedScenes) {
			if (s.equals(scene)) {
				attachDirty(scene);
			}
			else {
				attachDirty(s);
			}
		}
		scene = newScene;
		if (strip.getHasMultipleScenes()) {
			int length = 0;
			List<Scene> scenes = findByNumbersAndScript(strip.getScenes(), script);
			for (Scene scn : scenes) {
				length += scn.getLength();
			}
			strip.setLength(length);
		}
		else {
			strip.setLength(scene.getLength());
		}
		attachDirty(strip);
		return scene;
	}

	/**
	 * Mark all the scenes Omitted or not Omitted, whose scene numbers are given, within the
	 * specified Script.
	 *
	 * @param scenes The list of scene numbers of Scenes to update.
	 * @param script The script that contains all those Scenes.
	 * @param omit True or false -- the value to be put in the Scene.omit flag.
	 */
	@Transactional
	public void updateScenesOmit(List<String> scenes, Script script, boolean omit) {
		for (String num : scenes) {
			Scene scene = findByScriptAndNumber(script, num);
			if (scene != null) { // there should one
				scene.setOmitted(omit);
			}
		}
	}

	/**
	 * Update the sequence numbers for all the scenes in a script.  The scenes
	 * are sequence-numbered with an increment of SEQUENCE_INCREMENT (100).
	 * This allows new scenes to be inserted in order without having to
	 * re-sequence them each time.
	 * @param script - The script whose scenes should be re-sequenced.
	 */
	public void resequenceScenes(Script script) {
		List<Scene> scenes = script.getScenes();
		int seq = SEQUENCE_INCREMENT;
		for (Scene scene : scenes) {
			scene.setSequence(seq);
			merge(scene); // GnG had attachDirty() instead
			seq += SEQUENCE_INCREMENT;
		}
	}

	/**
	 * Create a new scene number that is "greater" than the given scene's
	 * number, but "less" than the next scene's number.  This usually involves
	 * creating an alphabetic suffix that is higher than the given scene's
	 * suffix (if any).
	 * <p>If the next scene has the same numeric portion of
	 * the scene number, the created suffix must be "less" than the next
	 * scene's alphabetic suffix.
	 * <p>Note that if there is no "next scene"
	 * (that is, the given scene is the last in the script), then the returned
	 * scene number will be strictly numeric, and 1 greater than the numeric
	 * portion of the given scene's number.
	 * @param script The script containing the given scene (so that the
	 * following scene can be located).
	 * @param scene The existing scene; the returned scene number will be
	 * "greater" than this scene's number.  If this is null, then we assume
	 * the new scene will precede the first existing scene in the script.
	 * @return The new scene number, typically to be used for an inserted
	 * scene.
	 */
	public String createNextSceneNumber(Script script, Scene scene) {
		Scene nextScene = null;
		if (scene != null) {
			nextScene = findNextInSequence(scene.getSequence(), script);
		}
		else if (script.getScenes().size() > 0) {
			nextScene = script.getScenes().get(0);
		}
		String scenenum = createNextSceneNumber(script, scene, nextScene);
		return scenenum;
	}

	/**
	 * Create a new scene number greater than "scene"s, and less than
	 * "nextScene"s.  See createNextSceneNumber(Script,Scene) for more info.
	 */
	public String createNextSceneNumber(Script script, Scene scene, Scene nextScene) {
		String sceneNum = ""; // numeric part of new scene number
		String sceneAlpha = ""; // alphabetic part of new scene number
		if (scene == null) {	// adding at beginning
			if (nextScene == null) {	// empty script
				sceneNum = "1";
			}
			else if (nextScene.getSceneNumber() > 1) {
				// first existing scene's number isn't 1, so use that for new one.
				sceneNum = "1";
			}
			else { // first scene is "1" or "1" plus alpha...
				sceneNum = "1"; // will start with this, no matter what
				String alpha2 = nextScene.getSceneAlpha();
				int len2 = alpha2.length();
				if (len2 == 0 || alpha2.charAt(0) != 'A') {
					sceneAlpha = "A"; // we may put "1A" before "1", but no other choice
				}
				else if (alpha2.charAt(len2-1)=='A') { // e.g., null, '1CA'
					sceneAlpha = alpha2 + "A"; // new='1CAA': not quite right, but no other way?
				}
				else {
					sceneAlpha = alpha2.substring(0, alpha2.length()-1) + "A";
					// e.g., null, '1CH' (unusual); new=1CA
					// another case: null, '1ABC', new='1ABA'
				}
			}
		}
		else if (nextScene == null) {	// no more: adding after the last scene
			if (scene.getSceneNumber() >= 0) {
				sceneNum = "" + (scene.getSceneNumber()+1);	// return will start with same number
			}
			else { // no numeric part in prior scene
				sceneAlpha = "A";
			}
		}
		else {
			if (scene.getSceneNumber() >= 0) {
				sceneNum = "" + scene.getSceneNumber();	// return will start with same number
			}
			if (nextScene.getSceneNumber() == scene.getSceneNumber()) {
				// same numeric value on both
				String alpha1 = scene.getSceneAlpha();
				String alpha2 = nextScene.getSceneAlpha();
				int len1 = alpha1.length();
				if (alpha1.length() == alpha2.length()) { // e.g., '6BF', '6BG'
					sceneAlpha = alpha1 + 'A';		// new one is '6BFA'
				}
				else if (alpha1.length() < alpha2.length()) {
					int len2 = alpha2.length();
					if (alpha2.charAt(len2-1)=='A') { // e.g., '6C', '6CA'
						sceneAlpha = alpha2 + "A"; // new='6CAA': not quite right, but no other way?
					}
					else {
						sceneAlpha = alpha2.substring(0, alpha2.length()-1) + "A";
						// e.g., '6C', '6CH' (unusual); new=6CA
						// another case: '6', '6ABC', new='6ABA'
					}
				}
				else { // alpha1 length > alpha2 length
					if (alpha1.charAt(len1-1)=='Z') { // e.g., '6BZ', '6C'
						sceneAlpha = alpha1 + 'A';  // new is '6BZA'
					}
					else { // e.g., '6BH', '6C', new is '6BI'
						sceneAlpha = alpha1.substring(0, len1-1) + (char)((alpha1.charAt(len1-1))+1);
					}
				}
			}
			else {
				// Next scene has different numeric value. Just increment this suffix.
				sceneAlpha = incrementAlpha(scene.getSceneAlpha());
			}
		}
		if (script != null) { // only null for JUnit testing?
			// Verify that our proposed new scene number doesn't already exist.
			// It could exist if an imported file had "out of order" scene numbers.
			while (findByScriptAndNumber(script, sceneAlpha+sceneNum) != null) {
				sceneAlpha = incrementAlpha(sceneAlpha);
			}
		}
		sceneNum = sceneAlpha + sceneNum;
		log.debug("old number="+(scene==null? "null" : scene.getNumber())+", new number="+sceneNum);
		return sceneNum;
	}

	/**
	 * Increments an alphabetic value, typically the suffix of a
	 * scene number.
	 * @param alpha the existing alphabetic suffix to be incremented
	 * @return The next higher alphabetic suffix.
	 */
	public static String incrementAlpha(String alpha) {
		String nextAlpha = "";
		if (alpha == null || alpha.length()==0) {
			nextAlpha = "A";
		}
		else {
			int n;
			for (n=0; n < alpha.length(); n++) {
				if (alpha.charAt(n) < 'Z') {
					char x = (char)((alpha.charAt(n))+1);
					nextAlpha += x;
					break;
				}
				else {
					nextAlpha += "Z";
				}
			}
			if (n >= alpha.length()) { // alpha was all Z's
				nextAlpha += "A";
			}
		}
		return nextAlpha;
	}

	/**
	 * Renumber the scenes in a script, beginning with the scene "firstScene"
	 * and continuing to the end of the script.
	 *
	 * @param script: The script whose scenes will be renumbered.
	 * @param firstScene: The first scene to be examined -- its scene number
	 *            will be set to "newNumber", and the next scene's number will
	 *            be one higher, and so forth, to the end of the script.
	 */
/*	@Transactional
	public void renumberScenes(Script script, Scene firstScene, int newNumber) {

		 * Because the scene numbers are defined in the database as
		 * unique within a script, we can't just renumber them in order,
		 * as we would be updating scenes with numbers that might
		 * duplicate existing scene numbers later in the list.
		 * SO, we create the new numbers and add a leading "*", and
		 * update the database with these values.  They have to be
		 * flushed to the database before the next step, which is
		 * to go back and remove the "*" from all of them.


		//List<Scene> scenes = script.getScenes();
		List<Scene> scenes = script.getScenes();
		boolean started = false;
		int sceneNum = newNumber;
		Scene oldScene;
		int ix = 0, first = 0;
		for (Scene scene : scenes) {
			if (scene.getId() == firstScene.getId()) {
				log.debug("starting renumbering at "+sceneNum);
				started = true;
			}
			if (started) {
				scene.setNumber("*"+Integer.toString(sceneNum));
				sceneNum++;
				oldScene = scene;
				scene = merge(scene);
				if (scene != oldScene) {// remove this code later
					script.getScenes().remove(oldScene);
					script.getScenes().add(scene);
				}
				//scenes.set(ix, scene);
			}
			else { // count skipped entries
				first++;
			}
			ix++;
		}
		flush();
		log.debug("flushed");
		script = getScriptDAO().findById(script.getId());
		scenes = script.getScenes();
		Scene scene;
		// "first" should be the first entry that has a "*" to be removed
		for (ix=first; ix < scenes.size(); ix++) {
			scene = scenes.get(ix);
			if (scene.getNumber().charAt(0) == '*') {
				scene.setNumber(scene.getNumber().substring(1));
				oldScene = scene;
				scene = merge(scene);
				//scenes.set(ix, scene);
				if (scene != oldScene) {// remove this code later
					script.getScenes().remove(oldScene);
					script.getScenes().add(scene);
				}
			}
		}
		super.merge(script); // GnG had attachDirty()
		log.debug("last scene was renumbered to " + (sceneNum-1));
	}
*/

	/**
	 * Update the scene numbers for all the scenes in a script following
	 * the "sceneIndex"-th entry.
	 * The scenes are sequence-numbered with an increment of 1.
	 */
/*	public void resequenceNumbers(Script script,int sceneIndex) {
		List<Scene> sceneList = script.getScenes();
		int listSize=sceneList.size()-1;
		log.info(listSize);
		int number = listSize+2;
		for (int index=listSize;index>=sceneIndex-1;index--) {
			sceneList.get(index).setNumber(String.valueOf(number));
			merge(sceneList.get(index));
			number --;
		}
	}
*/

	/**
	 * Function for fetching a List of scene numbers from the "current" script
	 * for the specified project id.
	 */
/*	public List<Integer> getSceneNumbers(Integer proj_id) {
		try {
			Object [] values={proj_id};
			String queryString = "select scene.number from Project proj, Scene scene where proj.script.id = scene.script.id and proj.id=? order by scene.sequence";
			return find(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}
*/

/*	public void createElementArrays(Scene scene, List<String>[] elementNames, String[] headings, boolean includeEmpty) {
		Set<ScriptElement> sceneScriptElements=scene.getScriptElements();
		Integer columnCount = 0;
		int ix = 0;
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type.equals(ScriptElementType.N_A)) { // ignore N/A and any following values
				break;
			}
			List<String> list = getScriptElements(sceneScriptElements, type);
			if (includeEmpty || list.size() != 0) {
				elementNames[columnCount] = list; // save the list of matching elements
				headings[columnCount] = type.toString() + ":";
				columnCount++;
			}
			ix++;
		}
	}
*/
	/**
	 * Get the list of script elements for a Collection of scenes, and fill an array of Lists with
	 * the names of the script elements, one array entry for each ScriptElementType. A corresponding
	 * array of headings is also filled, containing the "listHeading" for each ScriptElementType.
	 * <p>
	 * If 'includeEmpty' is true, then the array may include empty Lists, where no element of a
	 * particular type existed -- in this case, all the ScriptElementType values will be represented
	 * in the array of Lists and the array of headings. If 'includeEmpty' is false, the Lists for
	 * those ScriptElementType's with no matching ScriptElements are omitted from the arrays.
	 *
	 * @param scenes The Collection of scenes from which the ScriptElements should be retrieved; the
	 *            set of ScriptElements will be the union of all the script elements from all the
	 *            scenes.
	 * @param elementNames An (output) array of List<String>; each entry in the array will be a List
	 *            of ScriptElement.name values.
	 * @param headings An (output) array of String; the n'th entry in this array has the
	 *            'listHeading' (i.e., display name) of the ScriptElementType whose matching
	 *            ScriptElements are in the n'th entry of the elementNames array.
	 * @param includeEmpty True if empty Lists of ScriptElement names should be included in the
	 *            output arrays. (This is true for breakdown sheet pages, but false for scene-list
	 *            type pages.)
	 */
//	public static void createElementArrays(Collection<Scene> scenes, List<String>[] elementNames, String[] headings,
//			boolean includeEmpty) {
//		Set<ScriptElement> sceneScriptElements = new HashSet<ScriptElement>();
//		for (Scene scene : scenes) {
//			sceneScriptElements.addAll(scene.getScriptElements());
//		}
//		Integer columnCount = 0;
//		for (ScriptElementType type : ScriptElementType.values()) {
//			if (type.equals(ScriptElementType.N_A)) { // ignore N/A and any following values
//				break;
//			}
//			List<String> list = getScriptElements(sceneScriptElements, type);
//			if (includeEmpty || list.size() != 0) {
//				elementNames[columnCount] = list; // save the list of matching elements
//				headings[columnCount] = type.toString();
//				columnCount++;
//			}
//		}
//	}

	/**
	 * Use the text from the first Action paragraph of the scene to create a synopsis value.
	 * It will look for complete sentences, and include one or more of them until it has at
	 * least SYNOPSIS_MIN_CREATE_LENGTH characters.  If the result is longer than
	 * SYNOPSIS_MAX_CREATE_LENGTH, it gets truncated and an ellipsis (...) is added.
	 * @param scene The scene containing the TextElements to be examined for Action
	 * paragraphs.
	 * @return A String no more than SYNOPSIS_MAX_CREATE_LENGTH characters long. Returns
	 * null if no ACTION TextElements are found with a length of at least 3 (probably just
	 * blanks, tabs, or other junk).
	 */
	public static String createSynopsis(Scene scene) {
		String action = "";
		// Accumulate contiguous ACTION text elements.  Import PDF creates separate elements
		// for each line in the script.
		for (TextElement te : scene.getTextElements()) {
			if (te.getType() == TextElementType.ACTION) {
				action += te.getText() + " ";
			}
			else if (action.length() > 0) {
				break;
			}
		}
		return createSynopsis(action);
	}

	/**
	 * Create a shortened form of the "action" string passed, for use as the
	 * Strip.synopsis field.  This attempts to break the string at sentence
	 * boundaries, and compose the synopsys of entire sentences, if that fits
	 * within the minimum and maximum length parameters.  If not, the end of
	 * the synopsis may be removed (breaking at a blank) and replaced by an ellipsis.
	 * @param action  The String that should be shortened (if necessary) to a synopsis.
	 * @return The synopsis.
	 */
	public static String createSynopsis(String action) {
		String synopsis = null;
		if (action.length() > 3) {
			action = action.replace(Constants.SCRIPT_NEW_LINE_CHAR, ' ');
			if (action.length() < Constants.SYNOPSIS_MAX_CREATE_LENGTH) {
				synopsis = action;
			}
			else {
				synopsis = "";
				Matcher match = P_SENTENCE.matcher(action);

				while(match.find() && synopsis.length() < Constants.SYNOPSIS_MIN_CREATE_LENGTH) {
					synopsis += match.group();
					//log.debug("match="+match.group());
				}
				synopsis = synopsis.trim();
				if (synopsis.length() < Constants.SYNOPSIS_MIN_CREATE_LENGTH) {
					synopsis = action;
				}
				if (synopsis.length() > Constants.SYNOPSIS_MAX_CREATE_LENGTH) {
					synopsis = synopsis.substring(0, Constants.SYNOPSIS_MAX_CREATE_LENGTH-3);
					int i = synopsis.lastIndexOf(' ');
					if (i > synopsis.length() - 7) { // arbitrary ... lose up to 6 chars to get word boundary
						synopsis = synopsis.substring(0, i+1);
					}
					synopsis += "...";
				}
			}
			//log.debug("syn=" + synopsis + ", action=" + action);
		}
		return synopsis;
	}

	/**
	 * Transfers script elements from rightScript to leftScript for
	 * all scenes that have a transfer status of "Accepted".
	 */
	@Transactional
	public int transferAccepted(List<ScenePair> spList) {
		int xfers = 0;
		try {
			for (ScenePair sp : spList ) {
				if (sp.getTransferStatus() == ScenePair.SceneTransferStatus.ACCEPTED ) {
					sp.transferElements(this);
					xfers++;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		log.debug("transferred: "+xfers);
		return xfers;
	}

}