//	File Name:	Scene.java
package com.lightspeedeps.model;

import static com.lightspeedeps.util.app.Constants.NEW_LINE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.TaggedExporter;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.script.SceneNumber;

/**
 * The Scene entity represents all the information about one scene within a
 * specific Script. Each Script will have its own set of distinct Scene objects.
 * The sequence field specifies the order of Scene`s within the Script.
 * <p>
 * A Scene is also associated with a Set of ScriptElement`s. The ScriptElement`s
 * are shared across Scene`s (and Script`s).
 * <p>
 * Note that compareTo and equals are not compatible. That is, it is NOT the
 * case that <br>
 * (s1.compareTo(s2) == 0) == (s1.equals(s2)) <br>
 * for all Scene`s s1 and s2. The compareTo method is strictly used for ordering
 * a list of Scenes all from the same Script, and so it only compares the
 * sequence field.
 */
@Entity
@Table(name = "scene", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Script_Id", "Number" }))
public class Scene extends PersistentObject<Scene> implements Comparable<Scene>, Cloneable {
	private static final long serialVersionUID = 8014552877949200839L;

	private static final Log log = LogFactory.getLog(Scene.class);

	public static final int BEST_MATCH = 13;
	public static final int SIMILAR_MATCH = 8; // minimum match score for "POSSIBLE" status
	public static final int HINT_MAX_LENGTH = 60;

	// Fields

	/** The ScriptElement which identifies the "set" (location) for the Scene. */
	private ScriptElement scriptElement;

	/** The Script this Scene belongs to.  Each Script contains an ordered List
	 * of Scenes which are not shared with any other Script. */
	private Script script;

	/** An arbitrary text string that is appended to the Set name when displaying
	 * this Scene's "header". It often describes this Scene's relationship to
	 * the prior Scene, such as "Continuous", "Same day", "Later that morning".
	 * This field is not currently editable by the user; it is set by the Script
	 * import process. */
	private String hint;

	/** The scene number, which must be unique within a Script, but can be any
	 * alphanumeric (mixed-case) string up to 6 characters long. */
	private String number;

	/** The sequence number determines this Scene's order within the Script. */
	private Integer sequence;

	/** Interior/Exterior indicator. */
	private IntExtType ieType = IntExtType.INTERIOR;

	/** Day/Night value. */
	private DayNightType dnType = DayNightType.DAY;

	/** Arbitrary text field, entered by user, which is meant to describe when
	 * this Scene occurs chronologically within the time frame of the Script. */
	private String scriptDay;

	/** The physical page number this scene starts on. */
	private Integer pageNumber;

	/** The logical page number this scene starts on -- the page number
	 * (which may include alpha characters) printed on the page header
	 * in the imported file (typically only for PDFs). */
	private String pageNumStr;

	/** The line number on the (printed) page where the scene header should
	 * appear.  Set based on the imported PDF appearance. */
	private Integer lineNumber = -1;

	/** The length of the Scene in eighth's of a page.  E.g., if length = 11,
	 * the Scene is 1 3/8 pages long. */
	private Integer length;

	/** The revision number of the Script in which this Scene last had any
	 * changes.  This is set by the Script Import process based on the results of
	 * the compare(Scene) method. */
	private Integer lastRevised;

	/** True if the Scene is in "Omitted" status.  This is typically set by
	 * the user, although in some cases the import process may recognize
	 * an omitted scene. */
	private Boolean omitted = new Boolean(false);

	/** A textual summary of the scene(s) for this Strip.  This field is filled in automatically
	 * if a script is imported with its text included, by using the first portion of the "Action"
	 * text from each Scene.  In any case, it may be entered and edited on the Breakdown Edit page. */
	private String synopsis;

	/** The ordered list of TextElement`s comprising the text of this Scene.
	 * The first entry in the List should be the scene header element. */
	private List<TextElement> textElements = new ArrayList<>(0);

	/** The Set of ScriptElement`s used in this Scene, NOT including the
	 * "set" (location) element.  (The {@link #scriptElement} field holds that
	 * information.) */
	private Set<ScriptElement> scriptElements = new HashSet<>(0);

	@Transient
	private List<ScriptElement> scriptElementList = null;

	@Transient
	private List<TextElement> screenElements;

	/** Used by BreakdownBean -- set to true if any Scene fields have been modified. */
	//@Transient
	//private boolean dirty = false;

	/** A transient flag, for breakdown page, to track which Scene in the "scene list" is
	 * currently selected. */
	@Transient
	private boolean selected = false;

	// Constructors

	/** default constructor */
	public Scene() {
	}

	/** full constructor */
/*	public Scene(ScriptElement scriptElement, Script script, String hint, Integer script_Id,
			String number, Integer sequence, IntExtType ieType, DayNightType dnType,
			String scriptDay, Integer pageNumber, Integer length,
			Integer lastRevised, List<TextElement> textElements,
			Set<Callsheet> callsheets, Set<ScriptElement> scriptElements
			Set<Callsheet> callsheets_1, Set<Callsheet> callsheets_2) {
		this.scriptElement = scriptElement;
		this.script = script;
		this.hint = hint;
		this.number = number;
		this.sequence = sequence;
		this.ieType = ieType;
		this.dnType = dnType;
		this.scriptDay = scriptDay;
		this.pageNumber = pageNumber;
		this.length = length;
		this.lastRevised = lastRevised;
		this.textElements = textElements;
		//this.callsheets = callsheets;
		this.scriptElements = scriptElements;
		//this.callsheets_1 = callsheets_1;
		//this.callsheets_2 = callsheets_2;
		//this.textElement=textElement;
		//this.script_Id = script_Id;
	}
*/
	// Property accessors

	/** See {@link #scriptElement}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Set_Id")
	public ScriptElement getScriptElement() {
		return scriptElement;
	}
	/** See {@link #scriptElement}. */
	public void setScriptElement(ScriptElement scriptElement) {
		//log.debug("id="+id+", loc="+(scriptElement==null?"null":scriptElement.getName()));
		this.scriptElement = scriptElement;
	}
	@Transient
	public ScriptElement getSet() { // alternate name
		return getScriptElement();
	}

	/** See {@link #script}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Id")
	public Script getScript() {
		return script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	/** See {@link #hint}. */
	@Column(name = "Hint", length = 60)
	public String getHint() {
		return hint;
	}
	/** See {@link #hint}. */
	public void setHint(String hint) {
		this.hint = hint;
	}

	/** See {@link #number}. */
	@Column(name = "Number", length = 10)
	public String getNumber() {
		return number;
	}
	/** See {@link #number}. */
	public void setNumber(String number) {
		sceneNumber = null; // clear transient to force re-calculation
		this.number = number;
	}

	/** See {@link #sequence}. */
	@Column(name = "Sequence", nullable = false)
	public Integer getSequence() {
		return sequence;
	}
	/** See {@link #sequence}. */
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/** See {@link #ieType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "IE_Type", nullable = false, length = 30)
	public IntExtType getIeType() {
		return ieType;
	}
	/** See {@link #ieType}. */
	public void setIeType(IntExtType ieType) {
		/* double-clicking on entries in Script Review pages sometimes caused the
		 * framework to send null values via the drop-down selection.  Ignore it. */
		if (ieType !=  null) {
			this.ieType = ieType;
		}
	}

	/** See {@link #dnType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "DN_Type", nullable = false, length = 30)
	public DayNightType getDnType() {
		return dnType;
	}
	/** See {@link #dnType}. */
	public void setDnType(DayNightType dnType) {
		/* double-clicking on entries in Script Review pages sometimes caused the
		 * framework to send null values via the drop-down selection.  Ignore it. */
		if (dnType != null) {
			this.dnType = dnType;
		}
	}

	/** See {@link #scriptDay}. */
	@Column(name = "Script_Day", length = 30)
	public String getScriptDay() {
		return scriptDay;
	}
	/** See {@link #scriptDay}. */
	public void setScriptDay(String str) {
		if (str != null) {
			str = str.trim();
			if (str.length() == 0) {
				str = null;
			}
		}
		scriptDay = str;
	}

	/** See {@link #pageNumber}. */
	@Column(name = "Page_Number")
	public Integer getPageNumber() {
		return pageNumber;
	}
	/** See {@link #pageNumber}. */
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	/** See {@link #pageNumStr}. */
	@Column(name = "Page_Num_Str")
	public String getPageNumStr() {
		return pageNumStr;
	}
	/** See {@link #pageNumStr}. */
	public void setPageNumStr(String pageNumStr) {
		this.pageNumStr = pageNumStr;
	}

	/** See {@link #lineNumber}. */
	@Column(name = "Line_Number")
	public Integer getLineNumber() {
		return lineNumber;
	}
	/** See {@link #lineNumber}. */
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	/** See {@link #length}. */
	@Column(name = "Length")
	public Integer getLength() {
		return length;
	}
	/** See {@link #length}. */
	public void setLength(Integer length) {
		this.length = length;
	}

	/** See {@link #lastRevised}. */
	@Column(name = "LastRevised", nullable = false)
	public Integer getLastRevised() {
		return lastRevised;
	}
	/** See {@link #lastRevised}. */
	public void setLastRevised(Integer lastRevised) {
		this.lastRevised = lastRevised;
	}

	/** See {@link #scriptElements}. */
	@ManyToMany(
			targetEntity=ScriptElement.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "scene_script_element",
			joinColumns = {@JoinColumn(name = "SCENE_ID")},
			inverseJoinColumns = {@JoinColumn(name = "SCRIPT_ELEMENT_ID")}
			)
	public Set<ScriptElement> getScriptElements() {
		return scriptElements;
	}
	/** See {@link #scriptElements}. */
	public void setScriptElements(Set<ScriptElement> scriptElements) {
		this.scriptElements = scriptElements;
	}

	@Transient
	public List<ScriptElement> getScriptElementList() {
		if (scriptElementList == null) {
			scriptElementList = new ArrayList<>(getScriptElements());
			Collections.sort(scriptElementList);
		}
		return scriptElementList;
	}

	/** See {@link #textElements}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scene")
	@OrderBy("sequence")
	public List<TextElement> getTextElements() {
		return textElements;
	}
	/** See {@link #textElements}. */
	public void setTextElements(List<TextElement> textElement) {
		textElements = textElement;
	}

	/** See {@link #omitted}. */
	@Column(name = "Omitted", nullable=false)
	public boolean getOmitted() {
		return omitted;
	}
	/** See {@link #omitted}. */
	public void setOmitted(boolean b) {
		omitted = b;
	}

	/** See {@link #synopsis}*/
	@Column(name = "Synopsis", length = 200)
	public String getSynopsis() {
		return synopsis;
	}
	/** See {@link #synopsis}*/
	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}

	/**
	 * Return the page length as "n m/8"
	*/
	@Transient()
	public String getPageLength() {
		int len = (getLength()==null ? 0 : getLength().intValue());
		return Scene.formatPageLength(len);
	}

	@Transient()
	public void setPageLength(String s) {
		int n = Scene.convertPageLength(s);
		if (n > 0) {
			setLength(n);
			//log.debug(s+"="+n);
		}
	}

	/** A transient SceneNumber object, which is used to determine the
	 * separate numeric and non-numeric portions of the Scene's {@link #number} string.  */
	@Transient
	private SceneNumber sceneNumber = null;

	/** @return The numeric part of the Scene's {@link #number} string. */
	@Transient()
	public int getSceneNumber() {
		if (sceneNumber == null) {
			sceneNumber = new SceneNumber(number);
		}
		return sceneNumber.getNumber();
	}

	/** @return The alphabetic portion of the scene number, either prefix,
	 * suffix, or both.  May return an empty String, but never returns null. */
	@Transient()
	public String getSceneAlpha() {
		if (sceneNumber == null) {
			sceneNumber = new SceneNumber(number);
		}
		return sceneNumber.getAlpha();
	}

	/**
	 * Is the given 'scene' revised compared to this scene?  That is, is it unequal
	 * in data items that would be loaded from a script during the import process?
	 * <p/>
	 * This does NOT compare the scriptDay field, as that is not loaded from the script, and
	 * may be updated by the user after loading.
	 * <p/>
	 * It also does NOT check script elements; although some elements may be loaded from the
	 * script, they are often added afterwards, making the check unreliable.  The text comparison
	 * (although slower) should still reveal any differences in this area. (rev 1588)
	 */
	@Transient()
	public boolean isRevised(Scene scene) {
		boolean bRet = true;	// assume it's different

		if (getDnType() != scene.getDnType()) {
			log.debug("unequal Day/Night");
		}
		else if (getIeType() != scene.getIeType()) {
			log.debug("unequal Int/Ext");
		}
		else if (! getNumber().equals(scene.getNumber())) {
			log.debug("unequal scene number");
		}
		else if (StringUtils.compare(getHint(), scene.getHint()) != 0) {
			log.debug("unequal hint field");
		}
		else if (( ( getScriptElement() == null && scene.getScriptElement() == null ) ||
				getScriptElement() != null && scene.getScriptElement() != null &&
				getScriptElement().getId().equals(scene.getScriptElement().getId()) ) ) {
//			Set<ScriptElement> thisSe = getScriptElements();
//			Set<ScriptElement> otherSe = scene.getScriptElements();
//			if (thisSe.size() == otherSe.size()) {
//				if (thisSe.size() != 0) {
//					for ( ScriptElement se : thisSe ) {
//						if ( ! otherSe.contains(se)) {
//							bRet = true;
//							log.debug("unequal script elements");
//							break;
//						}
//					}
//				}
//			if (!bRet) {
			// equal so far, check Text Elements
			List<TextElement> thisTeSet = getTextElements();
			List<TextElement> otherTeSet = scene.getTextElements();
			bRet = false;	// assume scenes are equal
			if (thisTeSet.size() != 0 || otherTeSet.size() != 0) {
				TextElement thisT, otherT;
				Iterator<TextElement> thisIter = thisTeSet.iterator();
				Iterator<TextElement> otherIter = otherTeSet.iterator();
				do { // until run out of data, or mismatch found
					do { // find next non-page-heading/footer element for this Scene
						thisT = (thisIter.hasNext() ? thisIter.next() : null);
					} while(thisT != null &&
							(thisT.getType() == TextElementType.PAGE_HEADING ||
							 thisT.getType() == TextElementType.PAGE_FOOTER));

					do { // find next non-page-heading/footer element for other Scene
						otherT = (otherIter.hasNext() ? otherIter.next() : null);
					} while(otherT != null &&
							(otherT.getType() == TextElementType.PAGE_HEADING ||
							 otherT.getType() == TextElementType.PAGE_FOOTER));

					if (thisT != null && otherT != null) {
						if (! thisT.contentMatches(otherT)) {
							log.debug("unequal text elements:\n this="+thisT+"\n other="+otherT);
							bRet = true;
							break;
						}
					}
					else {
						break;
					}
				} while(true);

				if (! bRet && (thisT != null || otherT != null)) {
					log.debug("extra text element(s):\n this="+thisT+"\n other="+otherT);
					bRet = true;
				}
			}
			else if (NumberUtils.compare(getLength(), scene.getLength()) != 0) {
				// if no text to compare, check scene length
				log.debug("unequal scene length");
				bRet = true;
			}
//				}
//				}
//				else {
//					log.debug("unequal script element count");
//				}
		}
		else {
			log.debug("unequal set(location)");
		}

		log.debug("isRevised="+bRet);
		return bRet;
	}

//	/**
//	 * Merge the data from "scene2" into this scene.
//	 * @param scene2 - the Scene to be combined into this one; if null,
//	 * no operation occurs.
//	 * @return The merged scene, which is this object.
//	 * ** NOT CURRENTLY USED ** Was part of old Script Review page features
//	 */
//	public Scene merge(Scene scene2) {
//		this.setLength(this.getLength() + scene2.getLength());
//
//		// merge script elements into our object
//		getScriptElements().addAll(scene2.getScriptElements());
//		// and remove from scene2
//		scene2.setScriptElements(null);
//
//		// merge text elements; set sequence numbers in the added
//		// elements appropriately.
//		List<TextElement> teSet1 = this.getTextElements();
//		List<TextElement> teSet2 =  scene2.getTextElements();
//		int teNum = 0;
//		if (teSet1.size()>0) {
//			// get the sequence number of the last one in our current list
//			teNum = teSet1.get(teSet1.size()-1).getSequence();
//		}
//		for (TextElement te : teSet2) {
//			te.setSequence(++teNum);
//			addTextElement(te);	// add each one to this scene
//		}
//		// and remove text from scene2
//		scene2.setTextElements(null);
//
//		return this;
//	}

	public void addTextElement(TextElement textElement) {
		textElement.setScene(this);
		textElements.add(textElement);
	}

	/**
	 * Sets matchLevel and status based on comparing contents of this pair of Scenes.
	 * Returns matchLevel.
	 */
	public int compare(Scene other) {

		if (this == null || other == null) {
			return 0;
		}
		// see if import found that the script source was identical for the two scenes:
		boolean scriptMatches = (getLastRevised().intValue() == other.getLastRevised().intValue());

		// Compare various Scene components.  The sum of the various comparisons
		// becomes our "match score".

		int matchLevel;
		matchLevel = ((length.intValue() == other.length.intValue()) ? 1 : 0 ); // equal length gets 1 point

		matchLevel += compareSceneNumbers(other, 4);	// worth 4 points if equal,
														// 3 if same number with different alpha,
														// 2 points if numbers differ by only 1
		if (scriptMatches) {	// script unchanged,
			matchLevel += 3;	// skip element comparison
		}
		else {
			matchLevel += compareScriptElements(other, 3); // worth 0-3 points based on number of matching elements
		}

		matchLevel += compareSet(other, 3);		// worth 1 each for same location, D/N, & I/E values

		// Since comparing text elements is a bit expensive, we may skip it...
		if (scriptMatches) {	// script unchanged,
			matchLevel += 2;	// skip text comparison & give full credit
		}
		else if (matchLevel >= SIMILAR_MATCH-2) {
			// has enough points that we might make it to "similar" level
			matchLevel += compareTextElements(other, 2); // worth 0-2 points based on number of matching elements
		}
		else {
			// too far apart, even full text match won't reach "SIMILAR" match level
			log.debug("scene "+getNumber()+": bypass text compare ("+matchLevel+")");
		}

		return matchLevel;
	}

	/**
	 * Compare the scene numbers, and return:
	 * 	best: if equal
	 *  best-1: if numeric portion is equal
	 *  best-2: if numeric portions are within 1 of each other
	 *  0: otherwise.
	 */
	private int compareSceneNumbers(Scene other, int best) {
		int diff = 0;
		if (getNumber().equalsIgnoreCase(other.getNumber())) {
			diff = best;
		}
		else if (getSceneNumber() == other.getSceneNumber()) {
			diff = (best-1);
		}
		else {
			int i = getSceneNumber() - other.getSceneNumber();
			if (i == 1 || i == -1 ) {
				diff = (best-2);
			}
		}
		log.debug("scene numbers: "+diff+" out of "+best);
		return diff;
	}

	/**
	 * Compare the set of script elements, and return range from 'best'
	 * to 0, depending on how much overlap there is. Some of the code
	 * really assumes that "best" == 4!!
	 *		best: identical sets
	 *		3: sets differ by less than 25%
	 *		2: sets differ by 25-50%, or one is empty & the other has a single item
	 *		1: sets differ by 50-75%, or one is empty & the other has two items
	 *		0: sets differ by 75-100%, or one is empty & the other has three or more items
	 */
	private int compareScriptElements(Scene other, int best) {
		// Only compare Character elements (rev 2304)
		Set<ScriptElement> sLeft = new HashSet<>();
		for (ScriptElement se : getScriptElements()) {
			if (se.getType() == ScriptElementType.CHARACTER) {
				sLeft.add(se);
			}
		}
		Set<ScriptElement> sRight = new HashSet<>();
		for (ScriptElement se : other.getScriptElements()) {
			if (se.getType() == ScriptElementType.CHARACTER) {
				sRight.add(se);
			}
		}
		int diff = 0;
		log.debug("size: l="+sLeft.size() + " r="+sRight.size());
		if (sLeft.size()==0 || sRight.size()==0) {
			if (sLeft.size()==sRight.size()) {
				diff = best; // both empty
			}
			else {	// one or the other is zero, but not both
				diff = 3 - sRight.size() - sLeft.size();
				if (diff < 0) diff = 0;
				// For non-empty side with 1 item, diff=2;
				// if 2 items, diff=1; 3 or more items, diff=0
			}
		}
		else {	// both sets are non-empty
			int match = 0;
			for ( ScriptElement se : sLeft ) {
				if (sRight.contains(se)) {
					match++;
				}
				else { // entities could be detached(?); we have cases
					// where "se" exists in right as different object but same id.
					for (ScriptElement rse : sRight) {
						if (rse.getId().equals(se.getId())) {
							match++;
						}
					}
				}
			}
			if (match == sLeft.size()) { // all items on left are also on the right
				if (match == sRight.size()) {
					diff = best;	// completely equal
				}
				else { // right must have extra entries
					diff = ((sRight.size() - sLeft.size())*4) / sRight.size(); // ranges 0-3
					diff = (3-diff);
				}
			}
			else {
				match = 0;
				for ( ScriptElement se : sRight ) {
					if (sLeft.contains(se)) {
						match++;
					}
					else { // entities could be detached(?); we have cases
						// where "se" exists in left as different object but same id.
						for (ScriptElement lse : sLeft) {
							if (lse.getId().equals(se.getId())) {
								match++;
							}
						}
					}
				}
				if (match == sRight.size()) { // all items on right are also on the left
					// left must have extra entries
					diff = ((sLeft.size() - sRight.size())*4) / sLeft.size(); // ranges 0-3
					diff = (3-diff);
				}
				else {
					// each side has unique items not on the other side
					diff = ((match*2)*3) / (sLeft.size()+sRight.size()); // ranges 0-2
					diff = (2-diff);
				}
			}
		}
		log.debug("script elements: "+diff+" out of "+best);
		return diff;
	}

	/**
	 * Compare the set of Text Elements comprising the action and dialogue on
	 * each side. Returns: <br>
	 *   best: contents are identical (including both empty) <br>
	 *   best/2: one set is empty and the other is not, or more than half of the
	 * 			items match each other. <br>
	 *   0: half or less of the items match.
	 * <p>
	 * Note that we don't compare elements that are not relevant to the
	 * "scene contents", that is, we ignore page headers and footers and blank
	 * lines, and also the Scene_heading record, since the components of the
	 * scene heading (number, location, I/E, and D/N) are compared individually
	 * outside of this routine.
	 */
	private int compareTextElements(Scene other, int best) {
		List<TextElement> sLeft = getTextElements();
		List<TextElement> sRight = other.getTextElements();
		int diff = 0;
		if (sLeft.size()==0 || sRight.size()==0) {
			if (sLeft.size()==sRight.size()) {
				diff = best; // both empty gets full count; will be situation if text not loaded
			}
			else {	// one or the other is zero, but not both
				diff = best/2;
			}
		}
		else if (sLeft.size() > sRight.size()*2 || sRight.size() > sLeft.size()*2) {
			log.debug("scene "+getNumber()+": short-cut=0, "+sLeft.size()+"/"+sRight.size());
			diff = 0; // no way for more than 1/2 to match since sizes are so different.
		}
		else {	// both sets are non-empty
			int nonMatch = 0;
			int leftSize = sLeft.size();
			int rightSize = sRight.size();
			TextElement tLeft, tRight;
			Iterator<TextElement> leftIter = sLeft.iterator();
			Iterator<TextElement> rightIter = sRight.iterator();
			while (true) {
				do { // find next non-page-heading/footer element for this Scene
					tLeft = (leftIter.hasNext() ? leftIter.next() : null);
				} while (tLeft != null && ! tLeft.getType().isSceneContent());
				do { // find next non-page-heading/footer element for this Scene
					tRight = (rightIter.hasNext() ? rightIter.next() : null);
				} while (tRight != null && ! tRight.getType().isSceneContent());
				if (tLeft == null || tRight == null) {
					break;
				}
				if (! tLeft.contentMatches(tRight)) {
					nonMatch++;
					if (! tLeft.getType().equals(tRight.getType())) {
						log.debug("mismatch-type: \n"+tLeft.toString()+"\n"+tRight.toString());
						// TODO - try to "align" matching types at least
					}
					else {
						log.debug("mismatch-data: \n"+tLeft.toString()+"\n"+tRight.toString());
					}
					if (nonMatch > (leftSize/2) || nonMatch > (rightSize/2)) {
						break; // no way to get even 1/2 credit
					}
				}
			}
			log.debug("scene "+getNumber()+": non-matched="+nonMatch+" of "+sLeft.size()+"/"+sRight.size());
			if (nonMatch == 0 && leftSize == rightSize) {
				diff = best;
			}
			else if (nonMatch < (leftSize/2) && nonMatch < (rightSize/2)) {
				diff = best/2;
			}
		}
		log.debug("text elements: "+diff+" out of "+best);
		return diff;
	}

	/**
	 * Compare the two Sets and the D/N and I/E indicators.  Returns:
	 * 		best: all equal (including no Set defined for either)
	 * 		best-1: Sets are equal, but one of D/N or I/E don't match.
	 * 		best-2: Sets are equal, but both D/N and I/E don't match.
	 * 		1: Sets don't match, D/N and I/E both match
	 * 		0: Sets don't match, and one or both of D/N and I/E doesn't match.
	 */
	private int compareSet(Scene other, int best) {
		int diff = 0;
		if ( (getScriptElement() == null && other.getScriptElement() == null) ||
				(getScriptElement() != null && other.getScriptElement() != null &&
				getScriptElement().getId().equals(other.getScriptElement().getId()) ) ) {
			diff = best;
			if (getDnType() != other.getDnType()) {
				diff--;
			}
			if (getIeType() != other.getIeType() ) {
				diff--;
			}
		}
		else { // different Set
			if (getDnType() == other.getDnType() &&
					getIeType() != other.getIeType()) {
				diff++;
			}
		}
		log.debug("Set elements: "+diff+" out of "+best);
		return diff;
	}

	@Override
	public String toString() {
		String s = "";
		s = "id="+id+
			", #=" + number +
			", ie=" + ieType +
			", dn=" + dnType +
			", day=" + scriptDay +
			", pg=" + pageNumber +
			", len=" + length +
			", rev=" + lastRevised + NEW_LINE;
		if (scriptElement != null)
			s += "Set: " + scriptElement.toString() + NEW_LINE;
		if (script != null)
			s += "Script id: " + script.getId() + NEW_LINE;

//		for (TextElement e : textElements) {
//			s += "TE: " + e.toString() + NEW_LINE;
//		}
//		for (ScriptElement e : scriptElements) {
//			s += "SE: " + e.toString() + NEW_LINE;
//		}
		return s;
	}

	/**
	 * Return the Interior/Exterior type as a 3-character string,
	 * used in most of the displays.
	*/
	@Transient()
	public String getShortIeType() {
		return getIeType().getShortLabel();
	}

	@Transient()
	public String getColorKey() {
		// ieType & dnType should not be null, but we had a bizarre NPE logged
		// during an import (on importreview.jsp) where one of these was null.
		String key = (getIeType() != null ? getIeType() : IntExtType.N_A).name() + "/";
		key += (getDnType() != null ? getDnType() : DayNightType.N_A).name();
		return key; // matches StripUtils.getStripColor()
	}

	/**
	 * Returns the standard scene heading (slug line), with the addition
	 * of the "hint" field, if non-blank.  The hint field is created when
	 * importing a script file, and typically includes text that follows the
	 * Set name but is not (apparently) part of the day/night indication.
	 * So the returned value is the Int/Ext indicator (ending with a period),
	 * then a space and the name of the Set (if any), then the hint (if it
	 * exists) surrounded by square brackets [], then a hyphen, and the Day/Night indicator.
	 */
	@Transient()
	public String getHeading() {
		return getHeading(true);
	}

	/**
	 * Returns the standard scene heading (slug line) which includes
	 * the Int/Ext indicator (ending with a period), then a space and
	 * the name of the Set (if any), a hyphen, and the Day/Night indicator.
	 */
	@Transient()
	public String getShortHeading() {
		return getHeading(false);
	}

	/**
	 * Returns either the long or short form of the scene heading (slug line).
	 * If 'includeHint' is true, the long form is returned, which includes the
	 * hint field enclosed in square brackets, if it is not blank.
	 * @param includeHint
	 */
	public String getHeading(boolean includeHint) {
		String s;
		if (getOmitted()) {
			s = getSetName(); // force reference to avoid LazyInit error if scene is un-omitted
			s = "(Omitted)";
		}
		else {
			s = getShortIeType() + ".  ";
			s += getSetName();
			if (includeHint && getHint() != null && getHint().length() > 0) {
				s += " [" + getHint() + "] ";
			}
			s += " - " + getDnType();
		}
		return s;
	}

	@Transient
	public String getSceneHeading() {
	return getHeading();
	}
	@Transient
	public void setSceneHeading(String sceneHeading) {
	// do nothing
	}

	@Transient
	public boolean getSelected() {
		return selected;
	}
	public void setSelected(boolean b) {
		selected = b;
	}

	@Transient()
	public String getSetName() {
		String s;
		if (getScriptElement() == null) {
			s = Constants.NO_SET_NAME;
		}
		else {
			s = getScriptElement().getName();
		}
		return s;
	}

	/**
	 * Generates a List of TextElements from this scene, adjusted to suit screen display. The
	 * contents are based on the textElements of the Scene, with some types removed, and blank lines
	 * inserted before Character and Action entries. Used by ScriptPageBean.
	 */
	@Transient
	public List<TextElement> getScreenElements() {
		if (screenElements == null) {
			boolean lastAction = false;
			TextElement blank = new TextElement(this, TextElementType.BLANK, 1, " ");
			List<TextElement> list = new ArrayList<>();
			for (TextElement tx : getTextElements()) {
				switch (tx.getType()) {
					case ACTION:
						if ( ! lastAction) {
							list.add(blank);
						}
						lastAction = true;
						list.add(tx);
						break;
					case CHARACTER:
						list.add(blank);
						// fall thru...
					default:
					//case DIALOGUE:
					//case TRANSITION:
					//case PARENTHETICAL:
					//case OTHER:
					//case BLANK:
						list.add(tx);
						// fall thru...
					case PAGE_HEADING:
					case PAGE_FOOTER:
					case START_ACT:
					case END_ACT:
					case CONTINUATION:
					case MORE:
						lastAction = false;
						break;
					case SCENE_HEADING:
						lastAction = true; // suppress blank between heading and action
						break;
				}
			}
			screenElements = list;
			log.debug("screen elements generated, #=" + list.size() + ", text elems=" + getTextElements().size());
		}
		return screenElements;
	}

//	/** See {@link #dirty}. */
//	@Transient
//	public boolean isDirty() {
//		return dirty;
//	}
//	/** See {@link #dirty}. */
//	public void setDirty(boolean dirty) {
//		this.dirty = dirty;
//	}

	@Override
	public int compareTo(Scene arg0) {
		//log.debug("seq=" + getSequence() + ", arg0=" + (arg0==null ? "null" : arg0.getId()) );
		if (arg0 == null) {
			return 1;
		}
		if (getSequence() == null) {
			return -1;
		}
		return getSequence().compareTo(arg0.getSequence());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getNumber() == null) ? 0 : getNumber().hashCode());
		result = prime * result + ((getScript() == null) ? 0 : getScript().getId()==null ? 0 : getScript().getId().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Scene other;
		try {
			other = (Scene)obj;
			if (getId() != null && other.getId() != null) {
				return getId().equals(other.getId());
			}
			// if either id is null, don't use that check, use number & script instead.

			if (StringUtils.compare(getNumber(), other.getNumber()) != 0) {
				return false;
			}
			return getScript().getId().equals(other.getScript().getId());
		}
		catch (Exception e) {
			return false;
		}
	}

	public void merge(Scene scene) {
		setDnType(scene.getDnType());
		setHint(scene.getHint());
		setIeType(scene.getIeType());
		setLength(scene.getLength());
		setOmitted(scene.getOmitted());
		setPageNumStr(scene.getPageNumStr());
		setScriptDay(scene.getScriptDay());
//		setScriptElement(scene.getScriptElement());
//		setScriptElements(scene.getScriptElements());
//		setTextElements(scene.getTextElements());
	}


	/**
	 * Export data in this instance via a TaggedExporter. This is currently used
	 * to output this object into an XML file.
	 *
	 * @param ex The TaggedExporter to which each field should be passed.
	 * @param shootDate The date to be included in the output data.
	 * @param strip The related Strip; the elapsed time for the scene is taken
	 *            from the Strip and included in the output data.
	 */
	public void flatten(TaggedExporter ex, Date shootDate, Strip strip) {
		ex.append("ID", getId());
		ex.append("Scene Number", getNumber());
		ex.append("Day Part", getDnType().getLabel());
		ex.append("Interior Exterior", getIeType().getShortLabel());
		if (getScriptElement() != null) {
			ex.append("Set", getScriptElement().getName());
		}
		else {
			ex.append("Set", Constants.NO_SET_NAME);
		}
		if (shootDate != null) {
			ex.append("Date", shootDate);
		}
		ex.append("Synopsis", getSynopsis());
		ex.append("Start Time", "");
		if (strip.getElapsedTime() != null) {
			BigDecimal hours = NumberUtils.scaleTo2Places(new BigDecimal((strip.getElapsedTime()/60.0)));
			ex.append("Hours", hours);
		}
		else {
			ex.append("Hours", "");
		}
	}

	/**
	 * Convert a script Page length from the "n m/8" style to an integer
	 * representing the total number of 1/8th pages (the way lengths are stored
	 * in the database)
	 *
	 * @param pageStr The page length as a formatted string, "m/8" or "n m/8" or
	 *            "n".
	 * @return The number of 1/8th pages; e.g., "2 3/8" becomes 19. Returns 0 if
	 *         'pageStr' does not match one of the three patterns described
	 *         above.
	 */
	public static int convertPageLength(String pageStr) {
		final Pattern PAGE_LENGTH_PATTERN = Pattern.compile("([0-9]+ +)?(([1-7])/8 )?");
		int frac = 0, pages = 0, n = 0;
		if (pageStr == null) {
			return 0;
		}

		Matcher m = PAGE_LENGTH_PATTERN.matcher(pageStr.trim() + " ");
		if (m.matches()) {
			if (m.group(1) != null) {
				pages = Integer.parseInt(m.group(1).trim());
			}
			if (m.group(3) != null) {
				frac = Integer.parseInt(m.group(3).trim());
			}
		}
		if (frac > 0 || pages > 0) {
			n = pages * 8 + frac;
		}
		return n;
	}

	/**
	 * Format a page length in eighth's as "n m/8"
	 *
	 * @param length The length in eighths.
	 * @return A String with the formatted length as either "m/8", "n", or
	 *         "n m/8". The fractional portion is only included if 'm' is
	 *         non-zero. The integer portion is included if it is more than 0,
	 *         or if 'length' is zero or negative, in which case the returned
	 *         String is just "0".
	 */
	public static String formatPageLength(int length) {
		String s = "";
		if (length > 0) {
			if (length >= 8) {
				s = length/8 + " ";
				length = length % 8;
			}
			if (length > 0 || s.length()==0) {
				s += length + "/8";
			}
		}
		else {
			s = "0";
		}
		return s;
	}

//	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//		log.debug("");
//		in.defaultReadObject();
//		log.debug("done, scn=" + number);
//	}
//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("start scn=" + number);
//		out.defaultWriteObject();
//		log.debug("done, scn=" + number);
//	}

}