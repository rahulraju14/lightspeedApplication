//	File Name:	ScenePair.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.model.*;

/**
 * A class for tracking pairs of Scene's presented on the script comparison
 * pages.
 */
public class ScenePair implements Serializable {

	/**
	 * ** IMPORTANT NOTE ABOUT SERIALIZATION & SESSION PERSISTENCE **
	 *
	 * For some reason, ICEfaces is serializing data that is presented on the comparison
	 * page, including an ArrayList<ScenePair> ! We don't really need this, but
	 * to prevent NotSerializableException's, we make this object Serializable,
	 * but set all of its data to transient to minimize the size of the saved
	 * session and the processing overhead.
	 */

	/** for serialization */
	private static final long serialVersionUID = 1L;

	private transient Scene m_left = null;
	private transient Scene m_right = null;
	private transient SceneCompareStatus m_compareStatus = SceneCompareStatus.NO_MATCH;
	private transient SceneTransferStatus m_transferStatus = SceneTransferStatus.N_A;
	private transient int m_matchLevel = 0;		// match rating from 0 (worst) to 11 (best)
	private transient boolean m_expanded = false;	// if this scene is expanded in display

	private transient int rowNumber;
	private transient boolean showData=false;

	/**
	 * Holds column headers for Script element boxes (when expanded).
	 */
	private transient String[] columns;
	/**
	 * Holds list of script elements for each box for left-hand script
	 */
	private transient List<String>[] leftElems;

	/**
	 * Holds list of script elements for each box for right-hand script
	 */
	private transient List<String>[] rightElems;


	public ScenePair() {
	}

	public ScenePair(Scene l, Scene r) {
		setLeft(l);
		setRight(r);
	}

	public enum SceneCompareStatus {
		MATCH,
		SIMILAR,
		NO_MATCH,
		N_A;
		@Override
		public String toString() {
			switch(this) {
				case MATCH:
					return "Match";
				case SIMILAR:
					return "Similar";
				case NO_MATCH:
					return "No match";
				case N_A:
					break;
			}
			return "N/A";
		}
	}

	public enum SceneTransferStatus {
		N_A,
		ACCEPTED,
		TRANSFERRED;
		@Override
		public String toString() {
			switch(this) {
				case ACCEPTED:
					return "Accepted";
				case TRANSFERRED:
					return "Transferred";
				case N_A:
					break;
			}
			return "N/A";
		}
	}

	/**
	 * Sets matchLevel and status based on comparing contents of this pair of Scenes.
	 * Returns matchLevel.
	 */
	public int compare() {
		if (m_left==null || m_right==null) {
			setMatchLevel(0); // set as unmatched if either or both are null
		}
		else if (m_left.getOmitted() || m_right.getOmitted()) {
			if (m_left.getOmitted() && m_right.getOmitted()) {
				// both are marked deleted, do normal compare
				setMatchLevel( m_left.compare(m_right) );
			}
			else { // one is deleted, the other is not, set to unmatched
				setMatchLevel(0);
			}
		}
		else {
			// Get comparison value from Scene.compare().
			setMatchLevel( m_left.compare(m_right) );
		}

		// Set the compare status based on the total score.
		setCompareStatus();

		return m_matchLevel;
	}

	/**
	 * Transfer all ScriptElements, script-day, and synopsis from the right-hand
	 * scene to the left-hand scene.
	 */
	public void transferElements(SceneDAO sceneDAO) {
		if (m_left != null && m_right != null) {
			m_left = sceneDAO.merge(m_left);
			m_left.setScriptElements(new HashSet<ScriptElement>(m_right.getScriptElements()));
			m_left.setSynopsis(m_right.getSynopsis());
			m_left.setScriptDay(m_right.getScriptDay());
			sceneDAO.save(m_left);
			setTransferStatus(SceneTransferStatus.TRANSFERRED);
		}
		else {
			setTransferStatus(SceneTransferStatus.N_A);
		}
	}

	public Scene getLeft() {
		return m_left;
	}
	public void setLeft(Scene m_left) {
		this.m_left = m_left;
	}

	public Scene getRight() {
		return m_right;
	}
	public void setRight(Scene m_right) {
		this.m_right = m_right;
	}

	/**
	 * @return A string to be displayed on the comparison screen, based on
	 * the match level between the two scenes in this pair.
	 */
	public String getDisplayStatus() {
		String status = "";
		if (getTransferStatus() == SceneTransferStatus.N_A) {
			status = getCompareStatus().toString();
			if (getCompareStatus() == SceneCompareStatus.SIMILAR) {
				// scale similarity level to 1...9
				int score = getMatchLevel() - Scene.SIMILAR_MATCH + 1;
				score = (score * 9) / 5;
				// status +=  "(" + score + ")"; // "Similar(8)"
				status = "" + (score*10) + "% " + status; // "80% Similar"
			}
		}
		else {
			status = getTransferStatus().toString();
		}
		return status;
	}

	/**
	 * @return A string to be used as the style class for displaying
	 * the comparison value of this ScenePair.
	 */
	public String getDisplayStyle() {
		String style = "";
		if (getTransferStatus() == SceneTransferStatus.N_A) {
			style = getCompareStatus().name();
		}
		else {
			style = getTransferStatus().name();
		}
		return style;
	}

	public SceneCompareStatus getCompareStatus() {
		return m_compareStatus;
	}
	public void setCompareStatus(SceneCompareStatus status) {
		m_compareStatus = status;
	}

	public void setCompareStatus() {
		// Set the compare status based on the pair's match level.
		if (m_matchLevel == Scene.BEST_MATCH) {
			setCompareStatus(SceneCompareStatus.MATCH);
		}
		else if (m_matchLevel >= Scene.SIMILAR_MATCH) {
			setCompareStatus(SceneCompareStatus.SIMILAR);
		}
		else {
			setCompareStatus(SceneCompareStatus.NO_MATCH);
		}
	}

	public SceneTransferStatus getTransferStatus() {
		return m_transferStatus;
	}
	public void setTransferStatus(SceneTransferStatus transferStatus) {
		m_transferStatus = transferStatus;
	}

	public int getMatchLevel() {
		return m_matchLevel;
	}
	public void setMatchLevel(int level) {
		m_matchLevel = level;
	}

	public boolean getExpanded() {
		return m_expanded;
	}
	public void setExpanded(boolean expanded) {
		m_expanded = expanded;
	}

	public int getRowNumber() {
		return rowNumber;
	}
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public boolean isShowData() {
		return showData;
	}
	public void setShowData(boolean showData) {
		this.showData = showData;
	}

	/** See {@link #leftElems}. */
	public List<String>[] getLeftElems() {
		return leftElems;
	}
	/** See {@link #leftElems}. */
	public void setLeftElems(List<String>[] list) {
		leftElems = list;
	}

	/** See {@link #leftElems}. */
	public void setLeftElem(int index, List<String> list) {
		leftElems[index] = list;
	}

	/** See {@link #rightElems}. */
	public List<String>[] getRightElems() {
		return rightElems;
	}
	/** See {@link #rightElems}. */
	public void setRightElems(List<String>[] list) {
		rightElems = list;
	}

	public void setRightElem(int index, List<String> list) {
		rightElems[index] = list;
	}

	/** See {@link #columns}. */
	public String[] getColumns() {
		return columns;
	}
	/** See {@link #columns}. */
	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public void setColumn(int index, String s) {
		columns[index] = s;
	}

}
