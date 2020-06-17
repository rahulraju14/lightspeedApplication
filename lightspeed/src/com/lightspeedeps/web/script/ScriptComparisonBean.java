//	File Name:	ScriptComparisonBean.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.script.StripUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.script.ScenePair.SceneTransferStatus;

/**
 * A class for backing the Script Comparison web page.
 * This is a request-scoped bean.
 */
@ManagedBean
@ViewScoped
public class ScriptComparisonBean implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 7374123090599698595L;

	private static final Log log = LogFactory.getLog(ScriptComparisonBean.class);

	private transient ScriptDAO scriptDAO;
	private transient SceneDAO sceneDAO;

	private transient List<ScenePair> m_scenePairs = new ArrayList<ScenePair>();
	private transient Map<String,Integer> m_matchValues = null;
	private transient Script m_leftScript = null;
	private transient Script m_rightScript = null;

	private int m_leftScriptId = -1;
	private int m_rightScriptId = -1;

	private boolean m_compareDone = false;

	/** If true, the compare method should refresh the Scene objects before it
	 * does the compare.  Necessary to avoid LazyInitializationException's when "Match Again"
	 * is used, or row inserts are done (via Add column buttons). */
	private boolean refresh;

	/** Refer to {@link #getImportStep} */
	private boolean importStep = true;

	/** Refer to {@link #getExcludeMatches} */
	private boolean excludeMatches = false;

	protected int showRowNumber = 0;
	protected int acceptRowNumber = 0;

	public ScriptComparisonBean() {
		importStep = SessionUtils.getBoolean(Constants.ATTR_SCRIPT_COMPARE_IMPORT, true);
		SessionUtils.put(Constants.ATTR_SCRIPT_COMPARE_IMPORT, null);
		if (! importStep) {
			int leftId = SessionUtils.getInteger(Constants.ATTR_SCRIPT_COMPARE_LEFT_ID, -1);
			int rightId = SessionUtils.getInteger(Constants.ATTR_SCRIPT_COMPARE_RIGHT_ID, -1);
			if (leftId != -1 && rightId != -1) {
				doCompare(leftId, rightId);
			}
		}
	}

	/**
	 * Used to trigger a comparison, this method is called from the beginning
	 * of the comparison jsp page.
	 * @return False
	 */
	public boolean getLoad() {
		if (getImportStep()) { // Only do this for import comparison, not draft comparison
			checkComparison();
		}
		return false;
	}

	/**
	 * See if we need to (re)run a comparison at this time.  The id's of the scripts to
	 * be compared is determined from the project settings and the saved Session
	 * attribute ATTR_IMPORT_SCRIPT_ID.
	 */
	private void checkComparison() {
		Project project = SessionUtils.getCurrentProject();
		int rightId = -1;
		int leftId = -1;
		Script script = null;
		if (project != null) {
			script = project.getScript();
			if (script != null) {
				rightId = script.getId().intValue();
			}
		}

		Integer id = SessionUtils.getInteger(Constants.ATTR_IMPORT_SCRIPT_ID);
		log.debug("session attr script id="+id);
		if (id == null) {
			// This normally shouldn't happen.  Let's just find the last script loaded.
			int rev = getScriptDAO().findMaxScriptRevision(project);
			log.debug("max script revision= "+rev);
			script = getScriptDAO().findByRevisionAndProject(rev, project);
			if (script != null) {
				leftId = script.getId().intValue();
				SessionUtils.put(Constants.ATTR_IMPORT_SCRIPT_ID, id);
			}
		}
		else {
			leftId = id.intValue();
		}
		if (leftId != getLeftScriptId() || rightId != getRightScriptId()) {
			setRightScript(null); // prevents compare from running twice
			setLeftScriptId(leftId);
			setRightScriptId(rightId);
		}
	}

	public List<ScenePair> getScenePairs() {
		return m_scenePairs;
	}
	public void setScenePairs(List<ScenePair> scenePairs) {
		m_scenePairs = scenePairs;
	}

	public int getLeftScriptId() {
		return m_leftScriptId;
	}
	public void setLeftScriptId(int leftScriptId) {
		log.debug("id="+leftScriptId);
		m_leftScriptId = leftScriptId;
		if (m_leftScriptId >= 0) {
			if (m_leftScript != null && m_leftScript.getId().intValue() != m_leftScriptId) {
				m_compareDone = false;
			}
			setLeftScript(getScriptDAO().findById(new Integer(m_leftScriptId)));
		}
		else {
			m_compareDone = false;
			setLeftScript(null);
		}
	}

	public int getRightScriptId() {
		return m_rightScriptId;
	}

	public void setRightScriptId(int rightScriptId) {
		log.debug("id="+rightScriptId);
		m_rightScriptId = rightScriptId;
		if (m_rightScriptId >= 0) {
			if (m_rightScript != null && m_rightScript.getId().intValue() != m_rightScriptId) {
				m_compareDone = false;
			}
			setRightScript(getScriptDAO().findById(new Integer(m_rightScriptId)));
		}
		else {
			m_compareDone = false;
			setRightScript(null);
		}
	}

	public Script getLeftScript() {
		return m_leftScript;
	}
	public void setLeftScript(Script leftScript) {
		m_leftScript = leftScript;
		runCompare();
	}

	public Script getRightScript() {
		return m_rightScript;
	}
	public void setRightScript(Script rightScript) {
		m_rightScript = rightScript;
		runCompare();
	}

	/**
	 * showRowNumber is set by the f:setPropertyActionListener on the +/-
	 * web page controls.
	 */
	public int getShowRowNumber() {
		return showRowNumber;
	}
	public void setShowRowNumber(int showRowNumber) {
		this.showRowNumber = showRowNumber;
	}

	/**
	 * @return true if this page is part of the Script Import process.
	 * Returns false if this is the Draft Comparison page.
	 */
	public boolean getImportStep() {
		return importStep;
	}
	public void setImportStep(boolean importStep) {
		this.importStep = importStep;
	}

	/**
	 * Should the web page display matching scenes?
	 * @return True if scenes which match exactly are to be excluded from the display.
	 * False if matches are included, i.e., all scenes are listed.
	 */
	public boolean getExcludeMatches() {
		return excludeMatches;
	}
	/** Refer to {@link #getExcludeMatches} */
	public void setExcludeMatches(boolean b) {
		excludeMatches = b;
	}

	/**
	 * acceptRowNumber is set by the f:setPropertyActionListener on the 'accept' toggle
	 * web page controls.
	 */
	public int getAcceptRowNumber() {
		return acceptRowNumber;
	}
	public void setAcceptRowNumber(int acceptRowNumber) {
		this.acceptRowNumber = acceptRowNumber;
	}

	public boolean getCompareDone() {
		return m_compareDone;
	}
	public void setCompareDone(boolean compareDone) {
		log.debug("set compareDone"+compareDone);
		m_compareDone = compareDone;
	}

	/**
	 * Called from our constructor; user has selected two script to be compared
	 * and we are opening the comparison page.
	 *
	 * @param left - id of left-hand script
	 * @param right - id of right-hand script
	 */
	private void doCompare(int left, int right) {
		setLeftScriptId(left);
		setRightScriptId(right);
	}

	/**
	 * Do full comparison of the two script versions if the compareDone
	 * flag is false; otherwise it is a no-op.
	 * Returns "success".
	 */
	private void runCompare() {
		log.debug("runCompare "+m_compareDone+", left=" + (m_leftScript==null?"null":m_leftScript.getId())
				+", right=" + (m_rightScript==null?"null":m_rightScript.getId()));
		if (!m_compareDone) {
			if (m_leftScript == null || m_rightScript == null
					|| m_leftScript.getId().equals(m_rightScript.getId())) {
				// missing a script, or the script are identical
			}
			else {
				// make sure we have current versions of script objects
				m_leftScript = getScriptDAO().refresh(m_leftScript);
				m_rightScript = getScriptDAO().refresh(m_rightScript);
				createPairs();
				m_compareDone = true;
				log.debug("pair count="+getScenePairs().size());
			}
		}
	}

	/**
	 * Create the list of pairs of scenes.  Multiple algorithms may be attempted, to
	 * try and get the most likely alignment of matching scenes.
	 */
	private void createPairs() {
		List<ScenePair> numberPair, matchPair, orderPair;
		int numberScore, matchScore, orderScore;

		m_matchValues = new HashMap<String,Integer>(); // for saving compare results

		orderScore = pairByOrder(); // simple pairing by sequential order of scenes
		if (m_rightScript.getScenes().size() != 0 && // if either script is empty, bypass other pairings
				m_leftScript.getScenes().size() != 0 ) {
			orderPair = getScenePairs();	// save this variation
			numberScore = pairByNumber();	// try pairing by scene numbers & save it
			numberPair = getScenePairs();
			matchScore = pairByMatch();		// try pairing by best "match" value & save it
			matchPair = getScenePairs();

			int maxScore = Math.max(orderScore, Math.max(matchScore, numberScore));
			// Which variation had the best score?  Use that one's pairing.
			// If there's a 2- or 3-way tie, the order of "if"s gives preference
			// to pair by scene number, then pair by order, and finally pair by match value.
			if (maxScore == numberScore) {
				setScenePairs(numberPair);
			}
			else if (maxScore == orderScore) {
				setScenePairs(orderPair);
			}
			else {
				setScenePairs(matchPair);
			}
		}
		createPairElements();
	}

	/**
	 * Compare one pair of scenes.  If these two scenes have already
	 * been compared, the value from the previous compare is returned.
	 * Returns an integer indicating level of match, as determined
	 * by the ScenePair.compare() method, which actually comes from the
	 * Scene.compare() method.
	 */
	private int compare(ScenePair sp) {
		int match = 0;
		String key = (sp.getLeft()==null ? "null" : sp.getLeft().getNumber());
		key += ",";
		key += (sp.getRight()==null ? "null" : sp.getRight().getNumber());
		Integer oldMatch = m_matchValues.get(key);
		if (oldMatch != null) {	// we compared these 2 before ... return that value.
			match = oldMatch.intValue();
			sp.setMatchLevel(match);
			sp.setCompareStatus();
			log.debug("OLD compare: " + key + "=" + match);
		}
		else {	// not compared yet
			if (refresh) {
				refresh(sp);
			}
			match = sp.compare();	// so do compare
			Integer m = new Integer(match);
			m_matchValues.put(key, m);	// then save the result
			log.debug("NEW compare: " + key + "=" + match);
		}
		return match;
	}


	/**
	 * Create pairs of scenes in strictly sequential order, and then
	 * compare all the pairs.
	 * @return The total "match score" of the paired scenes.
	 */
	private int pairByOrder() {
		List<ScenePair> pairs = new ArrayList<ScenePair>();
		Iterator<Scene> rightIter = m_rightScript.getScenes().iterator();
		Scene right;
		for (Scene left : m_leftScript.getScenes()) {
			right = null;
			if (rightIter.hasNext()) right = rightIter.next();
			pairs.add(new ScenePair(left, right));
		}
		while (rightIter.hasNext()) { // generate pairs for any additional right-hand scenes
			pairs.add(new ScenePair(null,rightIter.next()));
		}
		setScenePairs(pairs);
		int match = reComparePairs();
		log.debug("pairByOrder="+ match);
		return match;
	}

	/**
	 * Create pairs of scenes by matching scene numbers, and then
	 * compare all the pairs.
	 * @return The total "match score" of the paired scenes.
	 */
	private int pairByNumber() {
		List<ScenePair> pairs = new ArrayList<ScenePair>();
		Iterator<Scene> rightIter = m_rightScript.getScenes().iterator();
		Iterator<Scene> leftIter = m_leftScript.getScenes().iterator();
		Scene right, left;
		right = rightIter.next();
		left = leftIter.next();
		while (right != null && left != null) {
			if (left.getNumber().equals(right.getNumber())) {
				pairs.add(new ScenePair(left, right));
				left = (leftIter.hasNext() ? leftIter.next() : null);
				right = (rightIter.hasNext() ? rightIter.next() : null);
			}
			else {
				if (left.getSceneNumber() < right.getSceneNumber()) {
					pairs.add(new ScenePair(left, null));
					left = (leftIter.hasNext() ? leftIter.next() : null);
				}
				else if (left.getSceneNumber() > right.getSceneNumber()) {
					pairs.add(new ScenePair(null, right));
					right = (rightIter.hasNext() ? rightIter.next() : null);
				}
				else {
					int comp = right.getSceneAlpha().compareTo(left.getSceneAlpha());
					if (comp == 0) {
						pairs.add(new ScenePair(left, right));
						left = (leftIter.hasNext() ? leftIter.next() : null);
						right = (rightIter.hasNext() ? rightIter.next() : null);
					}
					else if (comp < 0) {
						pairs.add(new ScenePair(null, right));
						right = (rightIter.hasNext() ? rightIter.next() : null);
					}
					else {
						pairs.add(new ScenePair(left, null));
						left = (leftIter.hasNext() ? leftIter.next() : null);
					}
				}
			}
		}
		// we've processed the full set on one or both sides;
		// now pick up extras on either side
		if (left != null || right != null) {
			// one side was already advanced to the next entry
			pairs.add(new ScenePair(left,right));
		}
		while (leftIter.hasNext()) {
			pairs.add(new ScenePair(leftIter.next(),null));
		}
		while (rightIter.hasNext()) {
			pairs.add(new ScenePair(null,rightIter.next()));
		}
		setScenePairs(pairs);

		int match = reComparePairs();
		log.debug("pairByNumber="+match);
		return match;
	}

	/**
	 * Pair up the scenes by best match.  For now, we only "look ahead"
	 * one more scene to try and find a better match.  This algorithm
	 * works well if some scenes have been inserted, and then all scenes
	 * renumbered, as long as no more than one scene was inserted in
	 * any one position.
	 * @return The total "match score" of the paired scenes.
	 */
	private int pairByMatch() {
		List<ScenePair> pairs = new ArrayList<ScenePair>();
		Iterator<Scene> rightIter = m_rightScript.getScenes().iterator();
		Iterator<Scene> leftIter = m_leftScript.getScenes().iterator();
		Scene right, left, nextRight, nextLeft;
		ScenePair sp, leftSp=null, rightSp=null;
		int match, leftMatch, rightMatch;
		int retMatch = 0;
		right = rightIter.next();
		left = leftIter.next();
		while (right != null && left != null) {
			sp = new ScenePair(left, right);
			match = compare(sp);
			if (match == Scene.BEST_MATCH) { // can't get any better
				pairs.add(sp);
				retMatch += match;
				left = (leftIter.hasNext() ? leftIter.next() : null);
				right = (rightIter.hasNext() ? rightIter.next() : null);
			}
			else {
				//log.debug("not best, " + left.getNumber() + " vs " + right.getNumber() + " = " + match);
				leftMatch = rightMatch = 0;
				nextLeft = nextRight = null;
				// evaluate match between current right & next one on left
				if (leftIter.hasNext()) {
					nextLeft = leftIter.next();
					leftSp = new ScenePair(nextLeft, right);
					leftMatch = compare(leftSp);
				}
				// evaluate match between current left & next one on right
				if (rightIter.hasNext()) {
					nextRight = rightIter.next();
					rightSp = new ScenePair(left, nextRight);
					rightMatch = compare(rightSp);
				}
				if (leftMatch > rightMatch && leftMatch > match) {
					// "left+1" pair is better; insert current left by itself, then this pair.
					ScenePair nullPair = new ScenePair(left,null);
					nullPair.compare();	// just to set status properly
					pairs.add(nullPair);
					pairs.add(leftSp);
					retMatch += leftMatch;
					left = (leftIter.hasNext() ? leftIter.next() : null);
					right = nextRight;
				}
				else if (rightMatch > leftMatch && rightMatch > match) {
					// "right+1" pair is better; insert current right by itself, then this pair.
					ScenePair nullPair = new ScenePair(null, right);
					nullPair.compare();	// just to set status properly
					pairs.add(nullPair);
					pairs.add(rightSp);
					retMatch += rightMatch;
					left = nextLeft;
					right = (rightIter.hasNext() ? rightIter.next() : null);
				}
				else {
					// look-ahead didn't do any better, use normal pair & advance
					pairs.add(sp);
					retMatch += match;
					left = nextLeft;
					right = nextRight;
				}
			}
		}
		// we've processed the full set on one or both sides;
		// now pick up extras on either side
		while (leftIter.hasNext()) {
			pairs.add(new ScenePair(leftIter.next(),null));
		}
		while (rightIter.hasNext()) {
			pairs.add(new ScenePair(null,rightIter.next()));
		}
		setScenePairs(pairs);
		log.debug("pairByMatch="+retMatch);
		return retMatch;
	}

	/**
	 * Compare all the pairs of scenes in 'scenePairs'.
	 * @return The total "match level" of all pairs.
	 */
	private int reComparePairs() {
		int match = 0;
		for (ScenePair sp : getScenePairs()) {
			match += compare(sp);
		}
		log.debug("compared, match=" + match);
		m_compareDone = true;
		refresh = false; // reset our refresh flag
		return match;
	}

	public String actionInsertRowLeft() {
		log.debug("acceptrownumber="+acceptRowNumber);
		Scene saveScene;
		Scene oldScene = null;
		int ix = acceptRowNumber;
		for ( ; ix < m_scenePairs.size(); ix++) {
			ScenePair pair = m_scenePairs.get(ix);
			refresh(pair);
			saveScene = pair.getLeft();
			pair.setLeft(oldScene);
			pair.setTransferStatus(SceneTransferStatus.N_A);
			createPairElements(pair);
			oldScene = saveScene;
		}
		if (oldScene != null) {
			ScenePair newPair = new ScenePair(oldScene, null);
			newPair.setRowNumber(ix);
			createPairElements(newPair);
			m_scenePairs.add(newPair);
		}
		reComparePairs(); // re-compute match scores
		return null;
	}

	public String actionInsertRowRight() {
		log.debug("acceptrownumber="+acceptRowNumber);
		Scene saveScene;
		Scene oldScene = null;
		int ix = acceptRowNumber;
		for ( ; ix < m_scenePairs.size(); ix++) {
			ScenePair pair = m_scenePairs.get(ix);
			refresh(pair);
			saveScene = pair.getRight();
			pair.setRight(oldScene);
			pair.setTransferStatus(SceneTransferStatus.N_A);
			createPairElements(pair);
			oldScene = saveScene;
		}
		if (oldScene != null) {
			ScenePair newPair = new ScenePair(null, oldScene);
			newPair.setRowNumber(ix);
			createPairElements(newPair);
			m_scenePairs.add(newPair);
		}
		reComparePairs(); // re-compute match scores
		return null;
	}

	public String actionRescan() {
		for (ScenePair sp : getScenePairs() ) {
			sp.setTransferStatus(ScenePair.SceneTransferStatus.N_A);
		}
		refresh = true;
		reComparePairs();		// re-compute match scores
		return null;
	}
	/**
	 * Swaps the left and right scripts in the comparison.
	 * @return null navigation string
	 */
	public String swapSides() {
		log.debug("swapSides");
		int right = getRightScriptId();
		int left = getLeftScriptId();
		setRightScript(null);
		setLeftScriptId(right);
		setRightScriptId(left);
		return null;
	}

	/**
	 * User clicked "back" button; reset our fields so we'll do a new
	 * compare if/when we return to this page.
	 */
	public String actionBack() {
		log.debug("");
		setLeftScriptId(-1);
		setRightScriptId(-1);
		return "back";
	}

	/**
	 * Set the transfer status of all scenes to "n/a".
	 * @return null navigation string
	 */
	public String acceptNone() {
		log.debug("acceptNone");
		for (ScenePair sp : getScenePairs() ) {
			sp.setTransferStatus(ScenePair.SceneTransferStatus.N_A);
		}
		return null;
	}

	/**
	 * Set the transfer status of all "matching" scenes to "accepted".
	 * @return null navigation string
	 */
	public String acceptMatching() {
		log.debug("acceptMatching");
		for (ScenePair sp : getScenePairs() ) {
			if (sp.getCompareStatus() == ScenePair.SceneCompareStatus.MATCH) {
				sp.setTransferStatus(ScenePair.SceneTransferStatus.ACCEPTED);
			}
		}
		return null;
	}

	/**
	 * Set the transfer status of all "similar" or "matching" scenes to "accepted".
	 * @return null navigation string
	 */
	public String acceptSimilar() {
		log.debug("acceptSimilar");
		for (ScenePair sp : getScenePairs() ) {
			if (sp.getCompareStatus() == ScenePair.SceneCompareStatus.MATCH ||
					sp.getCompareStatus() == ScenePair.SceneCompareStatus.SIMILAR) {
				sp.setTransferStatus(ScenePair.SceneTransferStatus.ACCEPTED);
			}
		}
		return null;
	}

	/**
	 * Transfers script elements from rightScript to leftScript for
	 * all scenes that have a transfer status of "Accepted".
	 * @return null navigation string
	 */
	public String transferAccepted() {
		log.debug("transferAccepted");
		int xfers = getSceneDAO().transferAccepted(getScenePairs());
		log.debug("transferred: " + xfers);
		if (xfers > 0) {
			// Flush old compare results...
			m_matchValues = new HashMap<String,Integer>(); // for saving new compare results
			setLeftScript(null);
			setRightScript(null);
			setLeftScriptId(getLeftScriptId()); // reset left script object
			setRightScriptId(getRightScriptId()); // reset right script object & re-compare
			refresh = true;
			reComparePairs();		// re-compute match scores
			createPairElements();	// re-create script element lists
		}
		return null;
	}

	/**
	 * Called when user clicks on +/- expansion button on any row in table.
	 * @return null navigation string
	 */
	public String expandTableListener() {
		log.debug("showrownumber="+showRowNumber);
		int rowNumber = showRowNumber;
		for (ScenePair pair : m_scenePairs ) {
			int rowNum=pair.getRowNumber();
			if (rowNumber==rowNum) {
				pair.setShowData(!pair.isShowData());
			}
			else {
				pair.setShowData(false);
			}
		}
		return null;
	}

	/**
	 * Called when user clicks on "accept" toggle button on any row in table.
	 * @return null navigation string
	 */
	public String toggleAcceptListener() {
		log.debug("acceptrownumber="+acceptRowNumber);
		int rowNumber = acceptRowNumber;
		for (ScenePair pair : m_scenePairs ) {
			if (rowNumber==pair.getRowNumber()) {
				if (pair.getLeft() != null && pair.getRight() != null
						&& ! pair.getLeft().getOmitted() && ! pair.getRight().getOmitted()
						&& pair.getTransferStatus() != SceneTransferStatus.TRANSFERRED ) {
					if (pair.getTransferStatus() == SceneTransferStatus.ACCEPTED) {
						pair.setTransferStatus(SceneTransferStatus.N_A);
					}
					else {
						pair.setTransferStatus(SceneTransferStatus.ACCEPTED);
					}
				}
				else {
					pair.setTransferStatus(SceneTransferStatus.N_A);
				}
			}
		}
		return null;
	}

	/**
	 * Fill in the lists of script element names for all the scenes in all the
	 * scene-pairs.
	 */
	private void createPairElements() {
		log.debug("");
		int rowNumberScenes = 0;
		for (ScenePair pair : m_scenePairs ) {
			createPairElements(pair);
			pair.setRowNumber(rowNumberScenes);
			rowNumberScenes++;
		}
	}

	/**
	 * Fill in the lists of script element names for the given ScenePair.
	 */
	@SuppressWarnings("unchecked")
	private void createPairElements(ScenePair pair) {
		//log.debug("");
		List<String> leftList, rightList;
		Set<ScriptElement> leftSceneScriptElements, rightSceneScriptElements;
		if (pair.getLeft()!=null) {
			leftSceneScriptElements=pair.getLeft().getScriptElements();
			pair.getLeft().getHeading(); // fix lazy init errors
		}
		else {
			leftSceneScriptElements = new HashSet<ScriptElement>();
		}
		if (pair.getRight()!=null) {
			rightSceneScriptElements=pair.getRight().getScriptElements();
			pair.getRight().getHeading(); // fix lazy init errors
		}
		else {
			rightSceneScriptElements = new HashSet<ScriptElement>();
		}
		pair.setColumns(new String[ScriptElementType.ELEMENT_TABLE_SIZE]);
		pair.setLeftElems(new List[ScriptElementType.ELEMENT_TABLE_SIZE]);
		pair.setRightElems(new List[ScriptElementType.ELEMENT_TABLE_SIZE]);
		Integer columnCount = 0;
//		int ix = 0;
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type.equals(ScriptElementType.N_A)) {
				break;
			}
			leftList = SceneDAO.getScriptElements(leftSceneScriptElements, type);
			rightList = SceneDAO.getScriptElements(rightSceneScriptElements, type);
			// setting the column values
			if (leftList.size() != 0 || rightList.size() != 0) {
				showMismatch(leftList, rightList);
				pair.setLeftElem(columnCount,leftList);
				pair.setRightElem(columnCount,rightList);
				pair.setColumn(columnCount, type.toString());
				columnCount++;
			}
//			ix++;
		}
		//log.info("columnCount:::"	+ scenePair.getColumnCount());
		if (columnCount == 0) {
			pair.setLeftElem(0, new ArrayList<String>());
			pair.setRightElem(0, new ArrayList<String>());
			pair.setColumn(0, Constants.NO_SCRIPT_ELEMENTS);
		}
	}

	/**
	 * Update the left-hand side and right-hand side lists of element names by adding
	 * an asterisk ("*") in front of an element if it does not exist on the other side.
	 * @param leftList List of element names (Strings) for the left-hand-side script.
	 * @param rightList List of element names (Strings) for the right-hand-side script.
	 */
	private void showMismatch(List<String> leftList, List<String> rightList) {
		int i=0, j=0;
		while( i < leftList.size() || j < rightList.size() ) {
			// Still some items remaining on one side or the other.
			if (i < leftList.size() && j < rightList.size()) {
				// have not reached the end of either list ... compare the two entries
				int c = leftList.get(i).compareTo(rightList.get(j));
				if (c == 0) { // Equal - don't change text, bump ahead on both sides
					i++;
					j++;
				}
				else if (c < 0) { // Left is less - so not on right - add an "*" on left.
					leftList.set(i, "*" + leftList.get(i));
					i++; // only increment left-hand index
				}
				else { // Right is less - so not on left - add an "*" on right.
					rightList.set(j, "*" + rightList.get(j));
					j++; // only increment right-hand index
				}
			}
			else if (i < leftList.size()) { // done on right, but not left
				leftList.set(i, "*" + leftList.get(i)); // so remaining ones are different
				i++;
			}
			else { // done on left, but not right
				rightList.set(j, "*" + rightList.get(j)); // so remaining ones are different
				j++;
			}
		}
	}

	/**
	 * Refresh the left & right Scene's referenced by the given pair.
	 * @param sp
	 */
	private void refresh(ScenePair sp) {
		if (sp.getLeft() != null) {
			sp.setLeft(getSceneDAO().refresh(sp.getLeft()));
		}
		if (sp.getRight() != null) {
			sp.setRight(getSceneDAO().refresh(sp.getRight()));
		}
	}

	public String actionCancelImport() {
		PopupBean.getInstance().show(
				this, ScriptReviewBean.ACT_CANCEL_IMPORT, "ImportScript.ConfirmCancel.");
		//ListView.addClientResize();
		return null;
	}

	@Override
	public String confirmCancel(int action) {
		return null;
	}

	@Override
	public String confirmOk(int action) {
		ScriptReviewBean.purgeImport(getLeftScript());
		return "cancel";
	}

	/**
	 * Non-static get() method so jsp's can retrieve the colorClassMap, which
	 * maps a scene's "colorKey" to the proper CSS class for the corresponding
	 * strip's color.
	 *
	 * @return the color class map
	 */
	public Map<String, String> getColorClassMap() {
		return StripUtils.getColorClassMap();
	}

	/** See {@link #scriptDAO}. Lazy-initialization support. */
	public ScriptDAO getScriptDAO() {
		if (scriptDAO == null) {
			scriptDAO = ScriptDAO.getInstance();
		}
		return scriptDAO;
	}

	/** See {@link #sceneDAO}. Lazy-initialization support. */
	public SceneDAO getSceneDAO() {
		if (sceneDAO == null) {
			sceneDAO = SceneDAO.getInstance();
		}
		return sceneDAO;
	}

}
