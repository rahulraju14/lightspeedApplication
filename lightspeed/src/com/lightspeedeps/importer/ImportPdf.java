//	File Name:	ImportPdf.java
package com.lightspeedeps.importer;

import static com.lightspeedeps.importer.ImportPdf.Action.*;
import static com.lightspeedeps.importer.ImportPdf.State.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFScanStrategy;
import org.apache.pdfbox.util.PDFTextScanner;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.model.Page;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.TextElement;
import com.lightspeedeps.object.TextLine;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.script.ScriptUtils;

/**
 * A class for importing scripts in PDF format.
 *<p>
 * Some notes on expected data:
 * Top of page: a line ending with a page number; may have other page
 * heading information.  Page number may be followed by a period.  Page 1
 * may not have a page heading or page number.
 *<p>
 * Some scripts will have a scene heading with the text "CONTINUED:", typically
 * preceded by the scene number.  If the same scene has continued onto more
 * than one page, it may have "(n)" after the colon:
 *  15  CONTINUED:  (3)                                                    15
 *<p>
 * Note that if a scene heading line has no scene number, its margin is the
 * same as action text.
 *<p>
 * Character names may be followed by (CONT'D) or other annotations, such as
 * (O.S.), (V.O.), etc.
 */
@ManagedBean
@ViewScoped
public class ImportPdf extends ImportFileImpl implements PDFScanStrategy, Serializable {
	/** */
	private static final long serialVersionUID = - 8542825538365443472L;

	private static final Log log = LogFactory.getLog(ImportPdf.class);

	/** Minimum indent for a line to be considered a Character line.  We use this because the
	 * only pattern that distinguishes most Character lines is that they are all caps, and
	 * Action lines are sometimes all caps, too.  (Used during the pass where we attempt to
	 * identify lines by their contents, not their indentation.)
	 */
	private static final int MIN_CHARACTER_INDENT = 30;

	/** Minimum indent for a line to be considered a Transition line. */
	private static final int MIN_TRANSITION_INDENT = 45;

	/** The maximum y (vertical) position of a line (in pixels) that we'll recognize as a
	 * page header in some circumstances. */
	private static final float MAX_HEADER_YPOS = 67f;
	/** The minimum y (vertical) position of a line that we'll recognize as a
	 * page footer in some circumstances. */
	private static final float MIN_FOOTER_YPOS = 760f;

	/** Width of a 10-pitch character. Used for some blank-padding issues. */
	private static final float CHAR_WIDTH_10PITCH = 72f/10f;


	/** The maximum indent for a line to be considered a Scene heading, if it does not match
	 * the "Int/Ext" pattern.  If a line's indent is less than (or equal) to this, and it has
	 * matched numeric values on the left & right sides, then it's considered a scene heading
	 * even without an Int/Ext or Day/Night qualifier. */
	private static final int MAX_NUM_SCENE_HEADING_INDENT = 11; // typical is 6-8
	private static final int MAX_ANY_SCENE_HEADING_INDENT = 20; // typical is 12-15 if not numbered (same as Action)

	// Regular Expressions (RE_...)
	/** Transition line, has specific words in it, and ends with ":" */
	private static final String RE_TRANSITION = "[A-Z0-9 ]*((\\bCUT\\b)|(\\bFADE ((IN)|(OUT)|(TO)|(UP))\\b)|(\\bSLAM TO\\b)|(\\bSMASH TO\\b)|(\\bDISSOLVE\\b))[A-Z ]*(:|\\.)?";

	/** Minimal Transition line, no specific words in it, all uppercase; also limited by indentation. */
	private static final String RE_MIN_TRANSITION = "[A-Z0-9 ]*(:|\\.)?";

	/** Intercut transition, which sometimes includes lots of additional text, and may not have ending ":". */
	private static final String RE_INTERCUT = ".*\\bINTERCUT\\b.*";

	/** "Start of Act" line */
	private static final String RE_START_ACT = "ACT\\s+[A-Z0-9]*";

	/** "end of act" (or script) line */
	private static final String RE_END_ACT = "(END\\s+(OF\\s+)?(ACT|PILOT|EPISODE|STORY|SCRIPT|SHOW|TEASER)\\b[^a-z]*)|" +
											"(THE END\\.?)";

	/** A page heading line, may have script title besides page number.  At least TWO blanks
	 * must precede the page number, or the word "page" and one blank.
	 * Page number may end with alpha, and may have a period at the end. */
	private static final String RE_PAGE_HEADER_A = "(.*  )?\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // page number & optional alpha
	private static final String RE_PAGE_HEADER_A2 = "(.* )?PAGE \\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // "page" + number
	private static final String RE_PAGE_HEADER_A3 = "(.* )?\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // only single blank before number

	/** A page heading line, maybe script title plus page number with decimal, e.g., "15.1". */
	private static final String RE_PAGE_HEADER_B = "(.*  )?\\d{1,3}\\.\\d{1,2}"; // page number with decimal
	private static final String RE_PAGE_HEADER_B2 = "(.* )?PAGE \\d{1,3}\\.\\d{1,2}"; // "page" + number
	private static final String RE_PAGE_HEADER_B3 = "(.* )?\\d{1,3}\\.\\d{1,2}"; // only single blank before number

	/** A page heading line, maybe script title plus page number range, for "merged" pages. */
	private static final String RE_PAGE_HEADER_C = "(.*  )?\\d{1,3}[A-Za-z]?[A-Za-z]?-\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // page number range
	private static final String RE_PAGE_HEADER_C2 = "(.* )?PAGE \\d{1,3}[A-Za-z]?[A-Za-z]?-\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // "page" + number range
	private static final String RE_PAGE_HEADER_C3 = "(.* )?\\d{1,3}[A-Za-z]?[A-Za-z]?-\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?"; // only single blank before number

	/** A page heading with numbers on the left end, only recognized if the line begins to
	 * the left of any likely scene heading line. */
	private static final String RE_PAGE_HEADER_LEFT = "\\d{1,3}[A-Za-z]?[A-Za-z]?\\.?  .*";

	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_A}. */
	private static final Pattern P_PAGE_HEADER_A  = Pattern.compile(RE_PAGE_HEADER_A);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_A2}. */
	private static final Pattern P_PAGE_HEADER_A2 = Pattern.compile(RE_PAGE_HEADER_A2);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_A3}. */
	private static final Pattern P_PAGE_HEADER_A3 = Pattern.compile(RE_PAGE_HEADER_A3);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_B}. */
	private static final Pattern P_PAGE_HEADER_B  = Pattern.compile(RE_PAGE_HEADER_B);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_B2}. */
	private static final Pattern P_PAGE_HEADER_B2 = Pattern.compile(RE_PAGE_HEADER_B2);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_B3}. */
	private static final Pattern P_PAGE_HEADER_B3 = Pattern.compile(RE_PAGE_HEADER_B3);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_C}. */
	private static final Pattern P_PAGE_HEADER_C  = Pattern.compile(RE_PAGE_HEADER_C);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_C2}. */
	private static final Pattern P_PAGE_HEADER_C2 = Pattern.compile(RE_PAGE_HEADER_C2);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_C3}. */
	private static final Pattern P_PAGE_HEADER_C3 = Pattern.compile(RE_PAGE_HEADER_C3);
	/** A compiled regular expression. See {@link #RE_PAGE_HEADER_C}. */
	private static final Pattern P_PAGE_HEADER_LEFT = Pattern.compile(RE_PAGE_HEADER_LEFT);

	/** A Character line, which precedes dialogue. */
	private static final String RE_CHARACTER = "[A-Z][A-Z#0-9().' ]*"; // Caps & misc, e.g., "COP#1 (O.S.)"

	// Scene heading patterns.  Note that scene numbers are limited to 3 digits, to
	// avoid matching on years that are a leading or trailing part of a heading.

	/** A numbered scene heading line; leading & trailing numbers must match, and may include
	 *  leading or trailing alpha characters. */
	private static final String RE_NUMBERED_SCENE_HEADING =
		"([A-Z]*(\\d{1,3})[A-Z]*)\\.?\\s+" + 		// scene number; groups 1 & 2 (optional trailing period)
		"(((INT\\.?)|(EXT\\.?)|(I/E\\.?)|(E/I\\.?)|(INT\\.?/EXT\\.?)|(EXT\\.?/INT\\.?))\\s+" + 	// int/ext indicator; note group 3 extends thru set & D/N
		"[^a-z]*)" +							// set name & day/night (end group 3)
		"\\s\\1\\.?(\\s+\\*)?"; 				// trailing scene number - same as leading scene #; opt'l "*"
	/** A compiled regular expression. See {@link #RE_NUMBERED_SCENE_HEADING}. */
	private static final Pattern P_NUMBERED_SCENE_HEADING = Pattern.compile(RE_NUMBERED_SCENE_HEADING);

	/** A numbered scene heading line with ONLY trailing scene number; may include
	 *  leading or trailing alpha characters (as part of scene number). */
	private static final String RE_NUMBERED_RIGHT_SCENE_HEADING =
		"(((INT\\.?)|(EXT\\.?)|(I/E\\.?)|(E/I\\.?)|(INT\\.?/EXT\\.?)|(EXT\\.?/INT\\.?))\\s+" + 	// int/ext indicator; note group 1 extends thru set & D/N
		"[^a-z]*)" +							// set name & day/night (end group 1)
		"\\s([A-Z]*(\\d{1,3})[A-Z]*\\.?)(\\s+\\*)?"; 	// trailing scene number; groups 2 & 3; opt'l "*"
	/** A compiled regular expression. See {@link #RE_NUMBERED_RIGHT_SCENE_HEADING}. */
	private static final Pattern P_NUMBERED_RIGHT_SCENE_HEADING = Pattern.compile(RE_NUMBERED_RIGHT_SCENE_HEADING);

	/** A numbered scene heading line with ONLY leading (left-hand) numbers; may include
	 *  leading or trailing alpha characters (as part of scene number). */
	private static final String RE_NUMBERED_LEFT_SCENE_HEADING =
		"([A-Z]*(\\d{1,3})[A-Z]*)\\.?\\s+" + 		// scene number; groups 1 & 2 (optional trailing period)
		"(((INT\\.?)|(EXT\\.?)|(I/E\\.?)|(E/I\\.?)|(INT\\.?/EXT\\.?)|(EXT\\.?/INT\\.?))\\s+" + 	// int/ext indicator; note group 3 extends thru set & D/N
		"[^a-z]*)" +							// set name & day/night (end group 3)
		"(\\s+\\*)?"; 							// optional trailing "*"
	/** A compiled regular expression. See {@link #RE_NUMBERED_LEFT_SCENE_HEADING}. */
	private static final Pattern P_NUMBERED_LEFT_SCENE_HEADING = Pattern.compile(RE_NUMBERED_LEFT_SCENE_HEADING);

	/** A "continued" scene header (usually at the top of a page). */
	private static final String RE_CONTINUED_SCENE_HEADING =
		"([A-Z]*(\\d{1,3})[A-Z]*\\.?)\\s+" + 	// scene number; groups 1 & 2
		"(CONTINUED):?" + 					// "continued" tag
		"(\\s*\\(\\d+\\))?" +				// optional continuation number
		"\\s+\\1(\\s+\\*)?"; 				// trailing scene number - same as leading scene #; opt'l "*"
	/** A compiled regular expression. See {@link #RE_CONTINUED_SCENE_HEADING}. */
	private static final Pattern P_CONTINUED_SCENE_HEADING = Pattern.compile(RE_CONTINUED_SCENE_HEADING);

	/** An "omitted" scene header. */
	private static final String RE_OMITTED_SCENE_HEADING =
		"([A-Z]*(\\d{1,3})[A-Z]*\\.?)\\s+" + 		// scene number; groups 1 & 2
		"(SCENE )?(OMITTED)" + 					// "omitted" tag
		"\\s+(\\*\\s+)?\\1(\\s+\\*)?"; 			// trailing scene number - same as leading scene #
	/** A compiled regular expression. See {@link #RE_OMITTED_SCENE_HEADING}. */
	private static final Pattern P_OMITTED_SCENE_HEADING = Pattern.compile(RE_OMITTED_SCENE_HEADING);
	private static final String OMITTED_SCENE_HEADING = "OMITTED";

	/** An un-numbered scene header; identified simply by leading I/E qualifiers. */
	private static final String RE_SCENE_HEADING =
		"((INT\\.?)|(EXT\\.?)|(I/E\\.?)|(E/I\\.?)|(INT\\.?/EXT\\.?)|(EXT\\.?/INT\\.?))\\s+" + // int/ext indicator
		"[^a-z]*";				// set name & day/night
	/** A compiled regular expression. See {@link #RE_SCENE_HEADING}. */
	private static final Pattern P_SCENE_HEADING = Pattern.compile(RE_SCENE_HEADING);

	/** A numbered scene heading that did not match any of our INT/EXT attempts. */
	private static final String RE_REDUCED_SCENE_HEADING =
			"([A-Z]*(\\d{1,3})[A-Z]*\\.?)\\s+" + 		// scene number; groups 1 & 2
			"([^a-z]*)" +							// group 3: scene name and maybe D/N; all uppercase
			"\\s+(\\*\\s+)?\\1(\\s+\\*)?"; 			// trailing scene number - same as leading scene #
	/** A compiled regular expression. See {@link #RE_REDUCED_SCENE_HEADING}. */
	private static final Pattern P_REDUCED_SCENE_HEADING = Pattern.compile(RE_REDUCED_SCENE_HEADING);

	private static final int PROGRESS_LOAD = 5; // this percentage for loading the document
	private static final int PROGRESS_SCAN = 10; // up to this percentage for first scan (into text_lines)
	private static final int PROGRESS_LAST = 90; // up to this percentage for remaining processing

	protected enum State {
		PRE_SCENE,	// waiting for scene header
		POST_SCENE, // just had scene header, expect action or character
		ACTION,		// had 1 or more action texts
		DIALOGUE	// had 1 or more dialogue texts
	}

	protected enum Action {
		ERROR,
		START_SCENE,
		END_SCENE,
		ADD_CHARACTER,
		ACC_ACTION,
		ACC_DIALOGUE,
		ACC_PAREN,
		NOTHING,
	}
	StateChange[] st_pre_scene = { // transitions from PRE_SCENE
			new StateChange(PRE_SCENE,ERROR),		// 0 ACTION
			new StateChange(PRE_SCENE,ERROR),		// 1 DIALOGUE
			new StateChange(PRE_SCENE,ERROR),		// 2 CHARACTER
			new StateChange(PRE_SCENE,ERROR),		// 3 PARENTHETICAL
			new StateChange(POST_SCENE,START_SCENE),// 4 SCENE_HEADING
			new StateChange(PRE_SCENE,NOTHING),		// 5 TRANSITION
			new StateChange(PRE_SCENE,ERROR),		// 6 OTHER
			new StateChange(PRE_SCENE,NOTHING),		// 7 PAGE_HEADING
			new StateChange(PRE_SCENE,NOTHING),		// 8 START_ACT
			new StateChange(PRE_SCENE,NOTHING),		// 9 END_ACT
			new StateChange(PRE_SCENE,NOTHING),		// 10 CONTINUATION
			new StateChange(PRE_SCENE,NOTHING),		// 11 BLANK
			new StateChange(PRE_SCENE,NOTHING),		// 12 MORE
			new StateChange(PRE_SCENE,NOTHING),		// 13 PAGE_FOOTER
			};

	StateChange[] st_post_scene = { // transitions from POST_SCENE
			new StateChange(ACTION,ACC_ACTION),		// 0 ACTION
			new StateChange(POST_SCENE,ERROR),		// 1 DIALOGUE
			new StateChange(DIALOGUE,ADD_CHARACTER),// 2 CHARACTER
			new StateChange(POST_SCENE,ERROR),		// 3 PARENTHETICAL
			new StateChange(POST_SCENE,START_SCENE),// 4 SCENE_HEADING
			new StateChange(POST_SCENE,NOTHING),	// 5 TRANSITION
			new StateChange(POST_SCENE,ERROR),		// 6 OTHER
			new StateChange(POST_SCENE,NOTHING),	// 7 PAGE_HEADING
			new StateChange(POST_SCENE,ERROR),		// 8 START_ACT
			new StateChange(PRE_SCENE,END_SCENE),	// 9 END_ACT
			new StateChange(POST_SCENE,NOTHING),	// 10 CONTINUATION
			new StateChange(POST_SCENE,NOTHING),	// 11 BLANK
			new StateChange(POST_SCENE,NOTHING),	// 12 MORE
			new StateChange(POST_SCENE,NOTHING),	// 13 PAGE_FOOTER
			};

	StateChange[] st_action = { // transitions from ACTION
			new StateChange(ACTION,ACC_ACTION),		// 0 ACTION
			new StateChange(ACTION,ERROR),			// 1 DIALOGUE
			new StateChange(DIALOGUE,ADD_CHARACTER),// 2 CHARACTER
			new StateChange(ACTION,ERROR),			// 3 PARENTHETICAL
			new StateChange(POST_SCENE,START_SCENE),// 4 SCENE_HEADING
			new StateChange(POST_SCENE,NOTHING),	// 5 TRANSITION
			new StateChange(ACTION,ACC_ACTION),		// 6 OTHER
			new StateChange(ACTION,NOTHING),		// 7 PAGE_HEADING
			new StateChange(PRE_SCENE,END_SCENE),	// 8 START_ACT
			new StateChange(PRE_SCENE,END_SCENE),	// 9 END_ACT
			new StateChange(ACTION,NOTHING),		// 10 CONTINUATION
			new StateChange(ACTION,NOTHING),		// 11 BLANK
			new StateChange(ACTION,NOTHING),		// 12 MORE
			new StateChange(ACTION,NOTHING),		// 13 PAGE_FOOTER
			};

	StateChange[] st_dialogue = { // transitions from DIALOG
			new StateChange(ACTION,ACC_ACTION),		// 0 ACTION
			new StateChange(DIALOGUE,ACC_DIALOGUE),	// 1 DIALOGUE
			new StateChange(DIALOGUE,ADD_CHARACTER),// 2 CHARACTER
			new StateChange(DIALOGUE,ACC_PAREN),	// 3 PARENTHETICAL
			new StateChange(POST_SCENE,START_SCENE),// 4 SCENE_HEADING
			new StateChange(POST_SCENE,NOTHING),	// 5 TRANSITION
			new StateChange(DIALOGUE,ACC_DIALOGUE),	// 6 OTHER
			new StateChange(DIALOGUE,NOTHING),		// 7 PAGE_HEADING
			new StateChange(PRE_SCENE,END_SCENE),	// 8 START_ACT
			new StateChange(PRE_SCENE,END_SCENE),	// 9 END_ACT
			new StateChange(DIALOGUE,NOTHING),		// 10 CONTINUATION
			new StateChange(DIALOGUE,NOTHING),		// 11 BLANK
			new StateChange(DIALOGUE,NOTHING),		// 12 MORE
			new StateChange(DIALOGUE,NOTHING),		// 13 PAGE_FOOTER
			};

	StateChange[][] transitions = {
			st_pre_scene,
			st_post_scene,
			st_action,
			st_dialogue
		};

	/** The scanner reads the PDF and decodes it for us. */
	private PDFTextScanner scanner;

	/** This is the PDF document. */
	private PDDocument document;
	private Writer output;

	/** The current scene being processed. Set by actionStartScene.  Set to
	 * null when actionEndScene finishes processing a scene. */
	private Scene currentScene;

	/** The scene which is accumulating textLine objects.  This is usually the
	 * currentScene, but in some cases we switch currentScene to a new "expected"
	 * scene but let some remaining text get added into the scene just "finished". */
	private Scene textScene;

	/** The previous scene to the current one. */
	private Scene lastScene;

	/** Additional scene information for the current scene. */
	private SceneInfo info;

	/** The current Page object being constructed. */
	private Page page;

	/** The current Character, i.e., the one whose dialogue lines are being processed. */
	private ScriptElement character;

	/**
	 * The last TextLine that was adding to a TextElement.  This is used by addTextElement()
	 * to determine if the next TextLine can be merged into the existing TextElement.
	 */
	private TextLine lastLine = null;

	/** The physical page number within the PDF, passed to us by the PDF
	 * management routines.  See startPage(). */
	private int pdfPageNum;

	/** The PDF page number of the prior line processed for text additions. Used by
	 * addTextElement to recognize that a new page is being processed, to force creation
	 * of a Page object in cases where no page heading line exists. */
	private int priorPdfPageNum;

	/** The page count reported by PdfBox methods */
	private int documentPages;

	/** The total (in 1/8ths) of all the page lengths set in the Scene's processed.
	 * This is only used for debugging information. */
	private int totalScenePageLength;

	/** A list of all the logical page numbers, indexed by the physical page number.
	 * That is, pageNumbers.get(i) is the logical page number of the i'th physical
	 * page. */
	private List<String> pageNumbers;

	/** A running count of the lines passed to writeLine() on the current page. */
	private int lineNumber;

	/** The lowest value for "startY" we encounter on any page, i.e., the smallest
	 * value of Y coordinate of any line.  This is at the top of the page, and
	 * is in inches. */
	private float smallestY;

	/** The highest value for "startY" we encounter on any page, i.e., the largest
	 * value of Y coordinate of any line.  This is at the bottom of the page, and
	 * is in inches. */
	private float largestY;

	/** Used in addTextElement to track whether a Page includes some text other than a
	 * PAGE_HEADING.  This is so that multiple contiguous PAGE_HEADING's do not cause
	 * a new Page object to be created.   */
	private boolean pageHasText;

	/** Set to true the first time we write an Event log for an indent calculation
	 * problem; this is used to avoid numerous Events from one bad script. */
	private boolean indentErrorLogged;

	/** The numeric portion of the scene number for the last scene processed.  Used to
	 * check for (and warn about) scene numbers that are in non-ascending order. */
	private int sceneNumber;

	/** The alphabetic portion (if any) of the last scene number processed.  Used primarily
	 * in the situation where we generate a new scene number due to duplicate numbers found. */
	private String sceneAlpha;

	/** The number of Scene headers processed that had recognizable scene numbers. */
	private int numberedScenes;

	/** The number of Scene headers processed that did not have recognizable scene numbers. */
	private int unNumberedScenes;

	/** The number of Scene's found during early stages of the script analysis, prior to any
	 * data being stored in the database.  One use of this is to set our progress indicator during
	 * the later (slower) stages of import. */
	private int totalScenes;

	/** The number of Scene's which have completed processing, including being stored
	 * in the database. */
	private int scenesDone;

	/** A list of all the lines in the script, created during the initial
	 * scan of the PDF, and processed multiple times during the import process. */
	private List<TextLine> lines;

	/** The set of ScriptElements (usually just Characters) associated with the scene currently
	 * being processed. */
	private Set<ScriptElement> scriptElements;

	/** The set of TextElements (lines) associated with the scene currently
	 * being processed. */
	private List<TextElement> textElements;

	/** The last TextElement added to the current scene.  See addTextElement(). */
	private TextElement lastTextElement;

	/** The lineNumber of the last textLine added to the lastTextElement.
	 * See addTextElement(). */
	private int lastTextLineNumber;

	TextElementType textType = TextElementType.OTHER;

	private static final int MAX_INDENT = 120;
	private int[] indentCount;
	private final static int MAX_TAB_STOPS = 10;
	private int[] tabPos;
	private TextElementType[] tabType;

	public ImportPdf() {
		log.debug(this);
	}

	protected void init() {
		log.debug(this);
		lines = new ArrayList<TextLine>(5000);
		scriptElements = new HashSet<ScriptElement>(100);
		//textElements = new ArrayList<TextElement>();
		unNumberedScenes = 0;
		numberedScenes = 0;
		totalScenes = 0;
		scenesDone = 0;
		sceneNumber = 0;
		sceneAlpha = "";
		pdfPageNum = 0;
		priorPdfPageNum = -1;
		pageHasText = false;
		lineNumber = 0;
		smallestY = 72000; // a thousand inches! Any large value works here.
		largestY = -1;
		totalScenePageLength = 0;
		page = null;
		currentScene = null;
		textScene = null;
		lastScene = null;
		info = null;
		indentCount = new int[MAX_INDENT];
		tabPos = new int[MAX_TAB_STOPS];
		tabType = new TextElementType[MAX_TAB_STOPS];
	}

	/**
	 * Called by our superclass to perform the import.
	 */
	@Override
	protected boolean doImport() {
		boolean bRet = false;
		init();

		try {
			document = PDDocument.load(file, true);
		}
		catch (IOException e) {
			userExceptionMessage("IOException", e);
			return false;
		}

		setProgress(PROGRESS_LOAD);
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		output = new OutputStreamWriter( byteOut );
			  //  new FileOutputStream( "c:\\tmp\\temp.txt" ));

		if (document.isEncrypted()) {
			userErrorMessage("ImportFile.Encrypted");
		}
		else {
			String encoding = "ISO-8859-1"; // "UTF-8" "ISO-8859-1"
				// with "UTF-8", a special prime (x92) came through as "?" (on 30page.pdf)
			try {
				scanner = new PDFTextScanner(encoding);
				scanner.setSortByPosition(true);
				scanner.setPadding(true);
				scanner.setStrategy(this);
				documentPages = document.getNumberOfPages();
				bRet = processDocument();
			}
			catch (IOException e) {
				userExceptionMessage("IOException", e);
			}
			finally {
				// 'document' will be null if processDocument() closed in normally.
				if (document != null) {
					try {
						document.close();
					}
					catch(Exception e) {
						EventUtils.logError("Exception in final document close.", e);
					}
				}
			}
		}

		if (bRet) {
			try {
				//bRet = assignCastIds(project);
				finalDebug();
			}
			catch (Exception e) {
				bRet = false;
				userExceptionMessage("Exception", e);
			}
		}
		setProgress(PROGRESS_LAST);
		log.debug("output stream (len="+ byteOut.toString().length() + ")=`"  + byteOut.toString() + "`");

		return bRet;
	}

	/**
	 * This manages the majority of the PDF import process.  It:
	 * <ul>
	 * <li> creates a Script object
	 * <li> calls the PDF scanner (which returns lines via callbacks)
	 * <li> closes the PDF
	 * <li> calls processLines() to analyze and load the data
	 * <li> calls doFinalUpdates() to wrap the process
	 * </ul>
	 * If running in batch mode, the database transaction is started
	 * and ended within this method.
	 * @return False only if a major error, typically a runtime exception, occurs;
	 * true otherwise.
	 * Data errors, such as missing D/N values, scene numbers out of sequence, or
	 * any other foreseen and avoidable/correctable problems, do NOT cause a False
	 * return.
	 */
	private boolean processDocument() {
		boolean bRet = setup();
		if (!bRet)
			return false;

		startTransaction();
		try {
			createScript(ImportType.PDF);
			script.setLastPage(0);
			pageNumbers = new ArrayList<String>(100);
			pageNumbers.add("0");

			// writeText will call us back using the PDFScanStrategy-defined methods:
			// writeLine(), startPage(), endPage(), startArticle(), endArticle().
			scanner.writeText(document, output);

			// At this point, all the document data is in the "lines" List of TextLines.

			try {	// catch close errors separately, so they won't stop the import.
				document.close();	// close the PDF file
				document = null;	// indicate it was closed successfully.
			}
			catch (Exception e) {
				userExceptionMessage("Exception(closing document)", e);
			}

			bRet = processLines();
			if (bRet) {
				String pg = StringUtils.getStringFromList(pageNumbers, "|");
				script.setPageNumbers(pg);
				doFinalUpdates();
			}
		}
		catch (Exception e) {
			userExceptionMessage("Exception", e);
			page = null;
			lastScene = null;
			currentScene = null;
			bRet = false;
		}

		endTransaction(bRet);
		return bRet;
	}

	/**
	 * The PDF input processing starts here.  This method is called from the scanner
	 * for each line in the PDF.
	 * <p>
	 * Here we accumulate all the individual lines as TextLine objects, and do
	 * some preliminary analysis -- counting all lines at each indent point,
	 * and looking for page headings to set script page numbers.  Only the first
	 * two lines of each page are considered possible page headers.
	 * <p>
	 * Also, non-ASCII hyphens, quotes & primes are replaced with their ASCII
	 * equivalents.
	 */
	@Override
	public void writeLine(String text, float startX, float startY, float averageCharWidth) {

		TextLine line = new TextLine();
		int indent = (int)(startX/averageCharWidth);
		line.startX = (int)startX;
		//log.debug("x=" + startX + ", width=" + averageCharWidth + ", indent=" + indent);
		if (text.equals("*") || text.equals("**")) { // empty "changed" line
			text = "";
			line.changed = true;
			//log.debug("empty changed line");
		}
		else {
			if (text.endsWith("*") &&
					(indent + text.length()) > Constants.SCRIPT_FMT_RIGHT_MARGIN-5) {
				// probable "change" marker from Final Draft or other
				//log.debug("changed: `" + text + "`");
				text = text.substring(0, text.length()-1);
				while (text.endsWith("*")) {
					text = text.substring(0, text.length()-1);
				}
				line.changed = true;
			}
			if (text.length() > 0 && text.charAt(0) == ' ') {
				int i = 0;
				for (; i < text.length(); i++) {
					if (text.charAt(i) != ' ') {
						break;
					}
				}
				indent += i;
				line.startX += (int)((i)*CHAR_WIDTH_10PITCH);
			}
			char altPeriod = '…'; // ellipsis - appears as x'85' in hex editor
			text = text.trim() // replace non-ASCII punctuation with ASCII equivalents
					.replace('‘', '\'')
					.replace('’', '\'')
					.replace('“', '"')
					.replace('”', '"')
					.replace(altPeriod, '.') // rev 2.9.5874; in pre-formatted "cast list".
					.replace('–', '-'); // x'96'
		}
		if (text.length() > 200) {
			line = splitLine(text, startY, indent);
		}
		else {
			line.text = text;
			line.pdfPageNumber = pdfPageNum; // pdfPageNum is maintained by the startPage() callback.
			line.lineNumber = (int)(startY * 100); // 'processLines' will turn this into a real line number
			lineNumber++;		// count lines passed to us from current page (zeroed in startPage()).
			line.seqLineNumber = lineNumber;
			line.yPos = startY;

			if (text.length()==0) {
				line.type = TextElementType.BLANK;
			}
			else {
				//int indent = (int)(startX/averageCharWidth);
				line.indent = indent < 0 ? 0 : indent;
				if (startY < smallestY) { // determine top-most printed line on any page
					smallestY = startY;
				}
				if (startY > largestY) { // find position of lowest line in script
					largestY = startY;
				}
			}
			lines.add(line);
		}
		lastLine = line;
		//log.debug("line at " + line.yPos + ": " + line.text);
	}

	/**
	 * Split a long line of text into multiple TextLine objects, and add them to
	 * the {@link #lines} array.
	 *
	 * @param text The text to be split.
	 * @param startY The y position of the text line.
	 * @param indent The computed indentation of the line.
	 * @return The last TextLine instance created.
	 */
	private TextLine splitLine(String text, float startY, int indent) {
		TextLine line = new TextLine();
		indent = Math.min(indent,10);
		log.debug("" + text);
		int lineLen = 59;
		if (text.indexOf("!!") > -1) {
			text = text.replaceAll("!!", "\n");
			for (int x=0; x < 10; x++) {
				text = text.replaceAll("\\n\\n", "\n");
			}
			while (text.length() > 1) {
				int n = text.indexOf('\n');
				if (n == -1) {
					n = Math.min(lineLen, text.length());
				}
				if (n > 190) {
					n = 190;
				}
				String aLine = text.substring(0, n);
				aLine = aLine.replace('!', ' ');
				aLine = aLine.replace('%', ' ');
				line = makeLine(aLine, startY, indent);
				lines.add(line);
				if (n >= text.length()) {
					break;
				}
				text = text.substring(n+1);
			}
		}
		else {
			int i = 0;
			String aLine;
			int len = text.length();
			for (; i < len-lineLen; i += lineLen) {
				aLine = text.substring(i, i+lineLen);
				line = makeLine(aLine, startY, indent);
				lines.add(line);
			}
			aLine = text.substring(i);
			line = makeLine(aLine, startY, indent);
			lines.add(line);
		}
		return line;
	}

	/**
	 * Create a new TextLine instance with the given data.
	 *
	 * @param text The string of text.
	 * @param startY The y-position of the text.
	 * @param indent The character indentation detected for this line.
	 * @return The newly-created TextLine instance.
	 */
	private TextLine makeLine(String text, float startY, int indent) {
		TextLine line = new TextLine();
		line.text = text;
		line.pdfPageNumber = pdfPageNum; // pdfPageNum is maintained by the startPage() callback.
		line.lineNumber = (int)(startY * 100); // 'processLines' will turn this into a real line number
		lineNumber++;		// count lines passed to us from current page (zeroed in startPage()).
		line.seqLineNumber = lineNumber;
		line.yPos = startY;
		if (indent > 0) {
			line.startX = (int)((indent)*CHAR_WIDTH_10PITCH);
			line.indent = indent;
		}
		return line;
	}

	/**
	 * This is a major processing section of the PDF import. It makes multiple passes
	 * over the List of text lines.
	 * <p>
	 * The first pass tries to identify line types by matching them to patterns
	 * (regular expressions) -- see checkPatterns().
	 * <p>
	 * The next pass calculates line numbers from the PDF y-position values.
	 * <p>
	 * The last pass is a state-driven process, controlled by the state
	 * transition tables ('transitions') defined near the top of this class.
	 * During the second pass we also attempt to identify line types by
	 * indentation (for lines not identified by patterns).
	 *
	 * @return True in all cases (so far).
	 */
	private boolean processLines() {
		boolean findFirstScene = true;
		/** The physical page number where the first scene heading was found. */
		int firstScenePdfPage = 0;
		int lineSpacing = 0;
		int lastHeading = 0;

		// determine layout of page headers/footers & page numbers
		PageLayout pageLayout = analyzePageNumbers();

		// now extract and associate logical (printed) page numbers with physical page numbers
		processPageNumbers(pageLayout);

		// scan (backwards) to find the last numbered scene (if any) & save the numeric portion
		int lastSceneNum = findLastSceneNumber();

		// NEXT PASS - set line.type; count indent positions; find first scene; calculate line spacing
		for (TextLine line : lines) {
			analyzeLineType(line, lastSceneNum); // Determine line type by analyzing text; parse scene headers
			if ( line.type != TextElementType.BLANK &&
					line.type != TextElementType.SCENE_HEADING &&
					line.type != TextElementType.CONTINUATION &&
					line.type != TextElementType.TRANSITION &&
					line.type != TextElementType.START_ACT &&
					line.type != TextElementType.END_ACT &&
					line.type != TextElementType.PAGE_HEADING &&
					line.type != TextElementType.PAGE_FOOTER &&
					line.type != TextElementType.MORE &&
					line.indent < MAX_INDENT ) {
				// Avoid counting lines that are well-identified and may cause
				// a problem for indentation analysis.
				indentCount[line.indent]++; // otherwise, track # of lines at each indent position
			}
			if (findFirstScene && line.type == TextElementType.SCENE_HEADING) {
				firstScenePdfPage = line.pdfPageNumber;
				findFirstScene = false;
			}
			if (lineSpacing == 0) { // still to be determined
				if (line.type == TextElementType.SCENE_HEADING) {
					lastHeading = line.lineNumber;
				}
				else if (lastHeading != 0 && line.type != TextElementType.SCENE_HEADING
						&& line.type != TextElementType.BLANK) {
					lineSpacing = (line.lineNumber - lastHeading) / 2; // should be a blank line after heading
					log.debug("lineNumber=" + line.lineNumber + ", heading=" + lastHeading + ", line spacing=" + lineSpacing);
					if (lineSpacing < 0 || (largestY*100.)/lineSpacing > 80) { // doesn't make sense
						lineSpacing = 0;	// try again with next scene
						lastHeading = 0;
					}
				}
			}
		}

		totalScenes = Math.max((unNumberedScenes + numberedScenes), 1);
		if (lineSpacing == 0) { // curious
			lineSpacing = 1200;	// 12 point = 6 lines per inch
			log.debug("using default line spacing of " + lineSpacing);
		}

		int lineCnt = (int)( (largestY - smallestY)*100.0 / lineSpacing);
		log.debug("Y range: " + smallestY + " to " + largestY + ", lines/page=" + lineCnt);
		if (lineCnt > 63) {
			log.warn("calculated lines/page too high (" + lineCnt + "); set to " + Constants.SCRIPT_MAX_LINES_PER_PAGE);
			lineCnt = Constants.SCRIPT_MAX_LINES_PER_PAGE;
		}
		else if (lineCnt < 50) {
			log.warn("calculated lines/page too low (" + lineCnt + "); set to 50");
			lineCnt = 50;
		}
		script.setLinesPerPage(lineCnt);

		// NEXT PASS - fix up page numbers
		fillPageNumbers(firstScenePdfPage);

		// analyze the "tab stops" - common indentation values - to determine
		// which indent values indicate particular line types (e.g., Action or Dialogue).
		analyzeTabStops();

		// NEXT PASS: Calculate line numbers (based on y-positions)
		assignLineNumbers(lineSpacing);

		// NEXT PASS: Extract title pages & intro
		processTitle(firstScenePdfPage);

		// NEXT PASS: Do Action-table processing. This is where most of the database
		// objects get created, including Scenes, Pages, and TextElements.
		TextLine lastLine = null;
		boolean inParenthetical = false; // used to check for poorly-formatted parenthetical text
		State currentState = PRE_SCENE;
		for (TextLine line : lines) {
			if (line.type == TextElementType.OTHER) {
				if (line.seqLineNumber == 1 && line.yPos < MAX_HEADER_YPOS) {
					line.type = TextElementType.PAGE_HEADING;
				}
				else {
					// for lines whose type is not yet set, use their indentation to figure it out:
					line.type = findTypeFromIndent(line.indent);
					if (line.type == TextElementType.CHARACTER && currentState == State.DIALOGUE) {
						if (line.text.startsWith("(")) {
							line.type = TextElementType.PARENTHETICAL;
							if (! line.text.endsWith(")")) {
								inParenthetical = true;
							}
						}
						else if (inParenthetical && line.text.endsWith(")")) {
							inParenthetical = false;
							line.type = TextElementType.PARENTHETICAL;
						}
					}
				}
				if (line.type == TextElementType.OTHER) {
					log.info("unknown type, line=" + line);
					if (currentState == State.DIALOGUE &&
							line.text.startsWith("(") &&
							line.text.endsWith(")") ) {
						line.type = TextElementType.PARENTHETICAL;
					}
					else {
						restoreIndent(line); // add back blank characters to match indent
					}
				}
			}
			else if (line.type == TextElementType.TRANSITION && currentState == DIALOGUE) {
				/* The earlier identification of TRANSITION lines is quite broad, and may
				 * accidentally include dialog that happens to be all upper case.  Here we
				 * check the indentation, and will switch the line to DIALOGUE type if
				 * the indent matches.
				 */
				TextElementType t = findTypeFromIndent(line.indent);
				if ( t == TextElementType.DIALOGUE ) {
					line.type = t;
					log.debug("changing possible Transition line to Dialogue");
				}
			}

			if (line.type != TextElementType.IGNORE) {
				//log.debug("State: " + currentState + ", line: " + line);
				StateChange nextState = transitions[currentState.ordinal()][line.type.ordinal()];
				currentState = nextState.nextState;
				Action act = nextState.action;
				doAction(act, line);
				addTextElement(line);
				lastLine = line;
			}
			if (currentState != State.DIALOGUE || lastLine.type == TextElementType.DIALOGUE) {
				inParenthetical = false;
			}
		}

		// Majority of processing has finished at this point.  Wrap up some loose ends...

		if (currentScene != null) { // wrap up the last scene
			doAction(END_SCENE, lastLine);
		}
		if (page != null) { // wrap up the last page
			addPage(page);
		}
		if (textScene != null) { // Calculate revision for last scene in script
			updateRevision(textScene);
		}

		return true;
	}

	/**
	 * Figure out if page numbers are in headers or footers, and on left or
	 * right side of page.
	 */
	private PageLayout analyzePageNumbers() {

		// Decide which pages to use for analysis.
		int page1 = 1;
		int page2 = documentPages - 1; // next-to-last page

		if (documentPages > 6) {
			page1 = (documentPages+1) / 2; // pick a middle page
		}
		else if (documentPages > 3) {
			page1 = page2 - 1; // page# 2&3, or 3&4, or 4&5
		}
		else if (documentPages == 3) {
			page1 = 2;
			page2 = 3;
		}
		else if (documentPages == 2) {
			page2 = 2;
		}
		else if (documentPages == 1) {
			page2 = 0;
		}
		PageLayout one = analyzePageNumbers(page1);
		if (page2 == 0) {
			return one;
		}
		PageLayout two = analyzePageNumbers(page2);
		if (one.pageNumLoc == two.pageNumLoc) {
			return one;
		}
		return two;
	}

	/**
	 * Figure out if the page numbers on the given page are in headers or
	 * footers, and on left or right side of page.
	 *
	 * @param pageNum The physical page number of the page to be analyzed.
	 * @return A PageLayout instance whose members describe the format of
	 *         the given page.
	 */
	private PageLayout analyzePageNumbers(int pageNum) {
		PageLayout pl = new PageLayout();
		boolean foundSceneHeader = false;
		for (TextLine line : lines) {
			if (line.pdfPageNumber == pageNum && // found the page to analyze
					((! foundSceneHeader && line.seqLineNumber < 3) || // check top two lines
					line.endOfPage )) {							// and bottom line.
				String uptext = line.text.toUpperCase().trim(); // up-case to match "PAGE", EXT, etc.
				if (P_NUMBERED_SCENE_HEADING.matcher(uptext).matches() ||
						P_OMITTED_SCENE_HEADING.matcher(uptext).matches() ||
						P_NUMBERED_RIGHT_SCENE_HEADING.matcher(uptext).matches() ||
						P_CONTINUED_SCENE_HEADING.matcher(uptext).matches() ) {
					// Matches Scene heading pattern, so don't try it as page heading (which
					// would likely also match, due to trailing number).
					foundSceneHeader = true; // prevent line #2 from matching, but continue to end of page
				}
				else if (containsPageNumber(line)) {
					if (line.endOfPage) {
						pl.pageNumLoc = PageLayout.PN_LOCATION_BOTTOM_RIGHT;
					}
					else {
						pl.pageNumLoc = PageLayout.PN_LOCATION_TOP_RIGHT;
					}
					break; // done analyzing this page
				}
				else if (line.seqLineNumber == 1 && line.indent < MAX_ANY_SCENE_HEADING_INDENT
						&& line.yPos < MAX_HEADER_YPOS
						&& P_PAGE_HEADER_LEFT.matcher(uptext).matches()) {
					// first line, starts reasonably far left, & has leading numbers --
					// assume it's a page heading with page number on the left.
					if (line.endOfPage) {
						pl.pageNumLoc = PageLayout.PN_LOCATION_BOTTOM_LEFT;
					}
					else {
						pl.pageNumLoc = PageLayout.PN_LOCATION_TOP_LEFT;
					}
					break; // done analyzing this page
				}
//				else if (uptext.indexOf("©") >= 0) {
//				}
			}
		}
		return pl;
	}

	/**
	 * Extract page numbers based on the layout provided, and put them into
	 * the pageNumbers List.  Also marks the appropriate TextLine objects as
	 * PAGE_HEADING or PAGE_FOOTER.
	 */
	private void processPageNumbers(PageLayout pl) {
		TextLine lineOne = null;
		String pageNumber;
		boolean foundIt = false;
		boolean foundSceneHeader = false;
		for (TextLine line : lines) {
			if (! foundIt &&
					((pl.pageNumAtTop() && line.seqLineNumber < 3 && !foundSceneHeader) ||
					(! pl.pageNumAtTop() && line.endOfPage))) {
				String uptext = line.text.toUpperCase(); // some patterns have text like "PAGE"
				// look for page header, and extract page number if possible
				if (P_NUMBERED_SCENE_HEADING.matcher(uptext).matches() ||
						P_OMITTED_SCENE_HEADING.matcher(uptext).matches() ||
						P_NUMBERED_RIGHT_SCENE_HEADING.matcher(uptext).matches() ||
						P_CONTINUED_SCENE_HEADING.matcher(uptext).matches() ) {
					// Matches Scene heading pattern, so don't try it as page heading (which
					// would likely also match, due to trailing number).
					foundSceneHeader = true; // prevent line #2 from matching, but continue to end of page
				}
				else if (pl.pageNumOnRight()) {
					if (containsPageNumber(line)) {
						pageNumber = uptext;
						int i = pageNumber.lastIndexOf(' ');
						if (i > 0) {
							pageNumber = pageNumber.substring(i+1).trim();
						}
						if (line.seqLineNumber == 2 && lineOne != null && ! line.endOfPage) {
							// page number found on line 2; mark line 1 as heading also
							lineOne.type = TextElementType.PAGE_HEADING;
						}
						foundPageNumber(line, pageNumber);
						if (! line.endOfPage) { // (don't set flag if already at end of page)
							foundIt = true; // flag to skip to end of page
						}
					}
				}
				else { // layout has page number on left
					if (line.indent < MAX_ANY_SCENE_HEADING_INDENT
							&& P_PAGE_HEADER_LEFT.matcher(uptext).matches()) {
						// starts reasonably far left, & has leading numbers --
						// assume it's a page heading or footer with page number on the left.
						pageNumber = uptext;
						int i = pageNumber.indexOf(' ');
						if (i > 0) {
							pageNumber = pageNumber.substring(0,i);
						}
						if (pageNumber.endsWith(".")) {
							pageNumber = pageNumber.substring(0, pageNumber.length()-1);
						}
						foundPageNumber(line, pageNumber);
						if (! line.endOfPage) { // (don't set flag if already at end of page)
							foundIt = true; // flag to skip to end of page
						}
					}
				}
				if (! foundIt && (line.seqLineNumber == 1)) {
					lineOne = line; // save, to set type if line 2 is heading
				}
				else {
					lineOne = null;
				}
			}
			else if (line.endOfPage) {
				foundIt = false;
				foundSceneHeader = false;
			}
		}
	}

	private void foundPageNumber(TextLine line, String pageNumber) {
		if (pageNumber.endsWith(".")) {
			pageNumber = pageNumber.substring(0, pageNumber.length()-1);
		}
		while (pageNumbers.size() <= line.pdfPageNumber) {
			pageNumbers.add(null);
		}
		pageNumbers.set(line.pdfPageNumber, pageNumber);
		if (line.endOfPage) {
			line.type = TextElementType.PAGE_FOOTER;
		}
		else {
			line.type = TextElementType.PAGE_HEADING;
		}
	}

	private boolean containsPageNumber(TextLine line) {
		String uptext = line.text.toUpperCase().trim(); // up-case to match "PAGE", EXT, etc.
		boolean bRet =
				P_PAGE_HEADER_A.matcher(uptext).matches() ||
				P_PAGE_HEADER_A2.matcher(uptext).matches() ||
				P_PAGE_HEADER_B.matcher(uptext).matches() ||
				P_PAGE_HEADER_B2.matcher(uptext).matches() ||
				P_PAGE_HEADER_C.matcher(uptext).matches() ||
				P_PAGE_HEADER_C2.matcher(uptext).matches() ;
		if (! bRet &&
				(line.yPos < MAX_HEADER_YPOS || line.yPos > MIN_FOOTER_YPOS)) {
			// if close to top or bottom of page, allow just a single blank before numeric page value
			bRet =	P_PAGE_HEADER_A3.matcher(uptext).matches() ||
					P_PAGE_HEADER_B3.matcher(uptext).matches() ||
					P_PAGE_HEADER_C3.matcher(uptext).matches();

		}
		return bRet;
	}

	/**
	 * Make sure we have a logical page number for every physical page. If the
	 * value hasn't been set in the 'pageNumbers' List, then we will generate a
	 * printable page number.
	 *
	 * @param firstScenePdfPage The first physical page that contains a scene
	 *            header.
	 */
	private void fillPageNumbers(int firstScenePdfPage) {
		boolean doPage = true;
		for (TextLine line : lines) {
			if (doPage) {
				while(line.pdfPageNumber >= pageNumbers.size()) {
					pageNumbers.add(null);
				}
				if (pageNumbers.get(line.pdfPageNumber) == null) {
					// no printable page number assigned to this page yet
					String scriptPageNum;
					if (line.pdfPageNumber >= firstScenePdfPage) {
						int pageno = line.pdfPageNumber - firstScenePdfPage + 1;
						scriptPageNum = "" + pageno;
					}
					else { // label as Title page, "T1", "T2", etc.
						scriptPageNum = "T" + line.pdfPageNumber;
					}
					pageNumbers.set(line.pdfPageNumber, scriptPageNum);
					doPage = false; // this page is done; skip til end of page
				}
				else {
					doPage = false; // this page already had page number assigned
				}
			}
			if (line.endOfPage) {
				doPage = true;
			}
		}
	}

	/**
	 * Put back leading blanks into the TextLine's data to match the indentation
	 * of the original line.
	 * @param line
	 */
	private void restoreIndent(TextLine line) {
		// startX in points; startX / 72 = inches; output is assumed to be 10-pitch courier
		int indent = (line.startX*10)/72; // startX converted to number of characters
		if (indent >= 0 && indent < 250) {
			line.text = Constants.BLANK_LINE.substring(0, indent) + line.text.trim();
		}
		else {
			String msg = "ImportPdf: invalid indent calculated=" + indent + "; textLine=" + line.toString();
			log.warn(msg);
			if (! indentErrorLogged) { // only log it once per script
				EventUtils.logEvent(EventType.DATA_ERROR, msg);
				indentErrorLogged = true;
			}
		}
		log.debug("in=" + line.indent + ", sx=" + indent + ", '" + line.text);
	}

	/**
	 * Scan the List of TextLine's and calculate line numbers within each page for
	 * each TextLine.
	 * @param lineSpacing
	 */
	private void assignLineNumbers(int lineSpacing) {
		int topMargin = (int)(smallestY * 100) - (lineSpacing / 2); // to round the line number
		float lastY = 0;
		int lineNum = 0;
		for (TextLine line : lines) {
			if (line.type == TextElementType.PAGE_HEADING) {
				lineNum = 0;
				// For page headings, put indentation back in as blanks
				restoreIndent(line);
			}
			else if (line.yPos < lastY) {
				// new line higher than previous - recalculate. (Probably new page.)
				lineNum = ((int)(line.yPos * 100) - topMargin) / lineSpacing;
			}
			else {
				float diff = (line.yPos - lastY) * 100f;
				int lineIncr = (int)((diff / lineSpacing) + 0.5);
				if (lineIncr > 2) {
					lineNum = ((int)(line.yPos * 100) - topMargin) / lineSpacing;
				}
				else {
					lineNum += lineIncr;
				}
				//log.debug("yPos=" + line.yPos + ", diff=" + diff + ", lines=" + lineIncr + ", lineNum=" + lineNum);
			}
			if (line.type == TextElementType.PAGE_FOOTER) {
				// For page footers, put indentation back in as blanks
				restoreIndent(line);
			}
			if (lineNum == 0 && line.type == TextElementType.OTHER && line.yPos < MAX_HEADER_YPOS) {
				line.type = TextElementType.PAGE_HEADING;
				restoreIndent(line);
			}
			line.lineNumber = lineNum;
			lastY = line.yPos;
		}
	}

	/**
	 * Take any TextLines that are on one or more pages preceding the first
	 * Scene heading and save them as title pages.
	 *
	 * @param firstScenePage The physical page number of the page containing the
	 *            first Scene header.
	 */
	private void processTitle(int firstScenePage) {
		boolean didPageStart = false;
		boolean newPage = true; // next TextLine is a top-of-page line (even if not PAGE_HEADING)
		String scriptPage;
		if (firstScenePage > 1) {
			for (TextLine line : lines) {
				if (line.pdfPageNumber < firstScenePage) {
					if (line.type == TextElementType.PAGE_HEADING || newPage) {
						newPage = false;
						if (! didPageStart) {
							if (page != null) {
								addPage(page);
							}
							scriptPage = pageNumbers.get(line.pdfPageNumber);
							if (scriptPage == null) {
								scriptPage = "T" + line.pdfPageNumber;
								pageNumbers.set(line.pdfPageNumber, scriptPage);
							}
							page = new Page(script, line.pdfPageNumber, scriptPage);
							didPageStart = true;
						}
					}
					else {
						didPageStart = false;
					}
					if (line.type != TextElementType.BLANK &&
							line.type != TextElementType.PAGE_FOOTER &&
							line.type != TextElementType.PAGE_HEADING) {
						line.type = TextElementType.OTHER;
						restoreIndent(line);
					}
					TextElement textElement = makeTextElement(line);
					page.getTextElements().add(textElement);
					textElement.setPage(page);
					line.type = TextElementType.IGNORE; // is this ok for headers/footers?
					if (line.endOfPage) {
						newPage = true;
					}
				}
			}
			addPage(page);
			page = null;
		}
	}

	/**
	 * Determine the line type from the amount the line is indented.
	 * @param indent The indentation of the line.
	 * @return The likely TextElementType of the line.  Returns type "OTHER"
	 * if the indentation does not match an expected value.
	 */
	private TextElementType findTypeFromIndent(int indent) {
		TextElementType type = TextElementType.OTHER;
		int tab = matchTab(indent);
		if (tab >= 0 && tabType[tab] != null) {
			type = tabType[tab];
		}
		return type;
	}

	/**
	 * Given the action value (derived from the transition tables), call the
	 * appropriate action method.
	 * @param action The action to be performed.
	 * @param line The line that caused this action.
	 */
	private void doAction(Action action, TextLine line) {
		switch (action) {
		case ERROR:
			actionError(line);
			break;
		case START_SCENE:
			actionStartScene(line);
			break;
		case END_SCENE:
			actionEndScene(line, currentScene, info);
			break;
		case ADD_CHARACTER:
			actionAddCharacter(line);
			break;
		case ACC_ACTION:
			character = null;
			break;
		case ACC_DIALOGUE:
			actionAccumulateDialogue(line);
			break;
		case ACC_PAREN:
			actionAccumulateParen(line);
			break;
		case NOTHING:
			break;
		default:
			break;
		}
	}

	/**
	 * Perform the ERROR action.  Currently this just creates a log entry.
	 * @param line The TextLine that triggered the ERROR action.
	 */
	private void actionError(TextLine line) {
		// TODO Review action error handling
		log.warn(line);
	}

	/**
	 * Perform the START_SCENE action.  This is probably the most important action
	 * in the state machine.  It finishes up the prior scene (if not done already),
	 * and creates a new scene, including analysis of the Set name, I/E and D/N values,
	 * and scene number determination.  (Note that much of the processing is
	 * done by methods in our superclass, which should perform any functions that
	 * are common to all import sources.)
	 *
	 * @param line The TextLine that triggered the START_SCENE action.
	 */
	private void actionStartScene(TextLine line) {
		log.debug("");
		actionEndScene(line, currentScene, info);	// end prior scene, if any
		if (textScene != null) { // Calculate revision for scene just finished
			if (page != null) {
				scriptDAO.attachDirty(page);
			}
			updateRevision(textScene);
		}
		currentScene = createScene(line.sceneNumberStr);
		textScene = currentScene;	// scene for this & following TextElements
		textElements = new ArrayList<TextElement>(100);
		if (preSceneText != null) {
			// There was text preceding the (first) scene header -- attach to Scene
			for (TextElement te : preSceneText) {
				te.setScene(textScene);
				textElements.add(te);
			}
			preSceneText = null;
		}
		lastTextElement = null;
		info = new SceneInfo(currentScene);
		currentScene.setLength(new Integer(1));
		info.pageYpos = line.yPos;
		if (line.sceneHeading != null) {
			// checkPatterns() already parsed the text and may have removed scene
			// numbers, etc.
			info.heading = line.sceneHeading;
		}
		else {
			info.heading = line.text;
		}
		if (lastScene != null) { // pass default I/E and D/N values if available
			cleanHeading(info, lastScene.getIeType(), lastScene.getDnType());
		}
		else {
			cleanHeading(info, IntExtType.INTERIOR, DayNightType.DAY);
		}
		if (line.originalSceneNumber != null) {
			// Unusual case, typically a duplicate scene number; we already created our own
			// unique number; here we add the invalid/duplicate value to the "hint" field.
			String hint = currentScene.getHint();
			if (hint == null) {
				hint = line.originalSceneNumber;
			}
			else {
				hint = '(' + line.originalSceneNumber + ") " +  hint;
			}
			if (hint.length() > Scene.HINT_MAX_LENGTH) {
				hint = hint.substring(0,Scene.HINT_MAX_LENGTH);
			}
			currentScene.setHint(hint);
		}
		currentScene.setScriptElement(getLocation(info.location));
		currentScene.setPageNumber(line.pdfPageNumber);
		currentScene.setPageNumStr(pageNumbers.get(line.pdfPageNumber));
		currentScene.setLineNumber(line.lineNumber);
		addScene(info);
		lastScene = currentScene;
	}

	/**
	 * Process the END_SCENE action: finish up a scene.
	 *
	 * @param line The TextLine that caused the end of the scene to be
	 *            recognized. This could be a line starting a new scene
	 *            (a scene header), or something that should be the end of
	 *            a scene, such as an END_ACT or TRANSITION line.
	 * @param endScene The Scene object for the Scene to be ended; if null,
	 *            there is no scene to be ended!
	 * @param endInfo The SceneInfo object for the ending Scene; if null, there
	 *            is no Scene.
	 */
	private void actionEndScene(TextLine line, Scene endScene, SceneInfo endInfo) {
		//log.debug("");
		script.setLastPage(line.pdfPageNumber);
		if (endScene != null) {
			int lines = endScene.getLength(); // debug
			float len = (8.0f * endScene.getLength()) / (float)script.getLinesPerPage();
			endScene.setLength(Math.max(1,Math.round(len)));
			log.debug("start: " + endScene.getPageNumber() + "/" + endInfo.pageYpos +
					", end: " + line.pdfPageNumber + "/" + line.yPos
					+ ", lines=" + lines + ", len=" + len + ", pglen=" + endScene.getLength());
			totalScenePageLength += endScene.getLength(); // debugging, mostly
			//debug endScene.setScriptDay(UtilHelper.formatPageLength(totalScenePageLength));
			if (includeText) {
				endScene.setTextElements(textElements);
			}
			if (includeSceneElements) {
				endScene.setScriptElements(scriptElements);
			}
			scenesDone++;
			log.debug("end scene " + scenesDone + " of " + totalScenes);
			if (totalScenes < scenesDone) {
				totalScenes = scenesDone;
			}
			setProgress(((PROGRESS_LAST-PROGRESS_SCAN) * scenesDone / totalScenes) + PROGRESS_SCAN);
		}
		currentScene = null;	// set "current scene" to null, as we've finished it.
		scriptElements = new HashSet<ScriptElement>();
		// Don't reset textElements yet; it may accumulate some miscellaneous
		// lines following the 'end' of the scene.
		character = null;
	}


	private void actionAccumulateDialogue(TextLine line) {
		//log.debug("");
		if (character != null) {
			// This counts lines by actual lines in script
			character.setLineCount(character.getLineCount()+1); // dialogue count
		}
	}

	private void actionAccumulateParen(TextLine line) {
		//log.debug("");
	}

	/**
	 * Perform the ADD_CHARACTER action.  Some analysis of the text is
	 * done to eliminate possible mis-diagnosed Character lines, to
	 * remove annotations (such as V.O.), and handle cases where multiple
	 * character names appear on the line (delimited by either '&' or '/').
	 *
	 * <p> Once the Character name is isolated, the find the ScriptElement
	 * of type Character that matches the text.
	 * If not found in our map of characters, add it to
	 * the database and our character map.  The local character map avoids
	 * issuing a database call for every Character line.
	 *
	 * @param line The TextLine that triggered the ADD_CHARACTER action.
	 */
	private void actionAddCharacter(TextLine line) {
		ScriptElement se = getCharacterElement(line.text, scriptElements);
		if (se == null) {
			line.type = TextElementType.CONTINUATION;
		}
		else {
			character = se; // used for counting dialog lines, etc.
		}
	}

	/**
	 * Create a TextElement from the TextLine object and add it to the set of
	 * elements for the current Scene and current Page. This code also attempts
	 * to combine multiple lines of text into a single TextElement object if the
	 * type of text (e.g., Action, Dialogue) is the same.
	 *
	 * @param line The TextLine representing the text to be added to the current
	 *            Page and/or Scene.
	 */
	private void addTextElement(TextLine line) {
		if (textScene != null) {
			if (currentScene == null) {
				//log.debug("skip text length calculation for 'extra' lines");
			}
			else {
				// update scene length -- during processing it is a line count
				if (line.type != TextElementType.PAGE_HEADING && // don't include headers/footers
						line.type != TextElementType.PAGE_FOOTER) {
					if (lastTextLineNumber < line.lineNumber) {
						textScene.setLength(textScene.getLength() + (line.lineNumber - lastTextLineNumber));
					}
					else {
						textScene.setLength(textScene.getLength() + 1);
					}
				}
			}
		}

		if (includeText) {
			if (textScene != null) {
				// See if we can accumulate lines of the same type into a "paragraph"
				if (lastTextElement != null &&
						line.type == lastTextElement.getType() &&
						(line.type == TextElementType.DIALOGUE ||
							line.type == TextElementType.ACTION ||
							line.type == TextElementType.PARENTHETICAL) &&
						line.changed == lastTextElement.getChanged() &&
						line.lineNumber == (lastTextLineNumber+1) // only join consecutive lines
						) {
					lastTextElement.setText(lastTextElement.getText() + Constants.SCRIPT_NEW_LINE_CHAR + line.text);
					lastTextLineNumber = line.lineNumber;
				}
				else {
					TextElement textElement = makeTextElement(line);
					textElements.add(textElement); // add to List for scene
					// Hibernate will add textElement to database when Scene is saved
					lastTextElement = textElement;
					if ((line.type == TextElementType.PAGE_HEADING && pageHasText) || page == null ||
							priorPdfPageNum != line.pdfPageNumber) {
						if (page != null) {
							addPage(page);
						}
						page = new Page(script, line.pdfPageNumber, pageNumbers.get(line.pdfPageNumber));
						pageHasText = false;
					}
					textElement.setPage(page);
					page.getTextElements().add(textElement);
					if ((textElement.getType() != TextElementType.PAGE_HEADING) &&
							(textElement.getType() != TextElementType.BLANK)) {
						pageHasText = true;
					}
				}
			}
			else { // capture text elements preceding the first Scene
				if (preSceneText == null) {
					preSceneText = new ArrayList<TextElement>();
				}
				if (page == null) {
					page = new Page(script, line.pdfPageNumber, pageNumbers.get(line.pdfPageNumber));
				}
				TextElement textElement = makeTextElement(line);
				textElement.setPage(page);
				page.getTextElements().add(textElement);
				pageHasText = true;
				preSceneText.add(textElement); // add to List for scene
			}
			priorPdfPageNum = line.pdfPageNumber;
		}
	}

	/**
	 * Create a TextElement with the equivalent information from the given
	 * TextLine, and update the global TextElement sequence number.
	 *
	 * @param line The TextLine containing the text, etc., to be copied into a
	 *            new TextElement.
	 * @return A new TextElement with all the same values as the TextLine, plus
	 *         the current sequence number.  This TextElement is also linked
	 *         to the Scene specified by 'textScene'.
	 */
	private TextElement makeTextElement(TextLine line) {
		TextElement textElement = new TextElement();
		textElement.setScene(textScene);
		textElement.setType(line.type);
		textElement.setSequence(textElementSequence);
		textElementSequence += TextElement.SEQUENCE_INCREMENT;
		textElement.setLineNumber(line.lineNumber);
		textElement.setChanged(line.changed);
		lastTextLineNumber = line.lineNumber;
		if (line.text.indexOf((char)0) >= 0) {
			//log.debug("null character at index=" + line.text.indexOf((char)0));
			dumpText(line.text);
			line.text = line.text.replace((char)0, '?');
			String msg = "Null character replaced by '?' in script, scene#=" +
					(textScene==null ? "null" : textScene.getNumber())
					+ ", text=`" + line.text + '`';
			log.warn(msg);
			EventUtils.logEvent(EventType.DATA_ERROR, msg);
			userInfoMessage("ImportScript.UnprintableCharacter", (textScene==null ? "0?" : textScene.getNumber()), line.text);
		}
		textElement.setText(line.text);
		// Hibernate will add textElement to database when Scene or Page is saved
		return textElement;
	}

	/**
	 * Add a page to the current script and insert in the database.
	 */
	private boolean addPage(Page pPage) {
		boolean bRet = true;
		pPage.setScript(script);
		if (script.getPages() == null) {
			script.setPages(new ArrayList<Page>());
		}
		script.getPages().add(pPage);
		pPage.updateHash();
		updateRevision(pPage);
		log.debug("saving page: " + pPage);
		scriptDAO.attachDirty(pPage);
		return bRet;
	}

	/**
	 * Determine the revision number for the given page, by comparing
	 * it to the prior script's page with the same logical page number.
	 * @param pPage
	 */
	private void updateRevision(Page pPage) {
		pPage.setLastRevised(script.getRevisionNumber());
		ScriptUtils.updateRevision(pPage, priorScript);
	}

	/**
	 * Attempt to determine line type by comparing to various patterns
	 * (typically regular expressions). This will result in setting the
	 * TextLine.type field for most of the following types: <br>
	 * Scene headings, <br>
	 * Characters, <br>
	 * Continuation indicators, <br>
	 * Transitions, <br>
	 * Start of Act, <br>
	 * End of Act.
	 * <p>
	 * For Scene headings, analyze the scene number, assign scene numbers if no
	 * scene number exists, and issue warnings for scene numbers out of order,
	 * duplicate scene numbers, and similar problems.
	 *
	 * @param line The TextLine to be analyzed.
	 * @param lastSceneNumber The highest (numeric portion of) scene number
	 *            found in the script.
	 */
	private void analyzeLineType(TextLine line, int lastSceneNumber) {
		TextElementType type = line.type;

		if (type == TextElementType.OTHER &&
				line.indent < MAX_ANY_SCENE_HEADING_INDENT) {
			// test for various possible Scene Heading patterns
			Matcher m = P_NUMBERED_SCENE_HEADING.matcher(line.text);
			if (m.matches()) {
				numberedScenes++;
				type = TextElementType.SCENE_HEADING;
				String sceneNumberStr = m.group(1);
				setSceneNumber(line, sceneNumberStr);
				line.sceneHeading = m.group(3).trim();
				log.debug("scene="+sceneNumberStr + ", #=" + sceneNumber);
			}
			else if ((m=P_NUMBERED_RIGHT_SCENE_HEADING.matcher(line.text)).matches()) {
				numberedScenes++;
				type = TextElementType.SCENE_HEADING;
				String sceneNumberStr = m.group(9);
				setSceneNumber(line, sceneNumberStr);
				line.sceneHeading = m.group(1).trim();
				log.debug("scene="+sceneNumberStr + ", #=" + sceneNumber);
			}
			else if ((m=P_NUMBERED_LEFT_SCENE_HEADING.matcher(line.text)).matches()) {
				numberedScenes++;
				type = TextElementType.SCENE_HEADING;
				String sceneNumberStr = m.group(1);
				setSceneNumber(line, sceneNumberStr);
				line.sceneHeading = m.group(3).trim();
				log.debug("scene="+sceneNumberStr + ", #=" + sceneNumber);
			}
			else if (P_CONTINUED_SCENE_HEADING.matcher(line.text).matches()) {
				type = TextElementType.CONTINUATION;
			}
			else if ((m=P_OMITTED_SCENE_HEADING.matcher(line.text)).matches()) {
				numberedScenes++;
				type = TextElementType.SCENE_HEADING;
				String sceneNumberStr = m.group(1);
				setSceneNumber(line, sceneNumberStr);
				line.sceneHeading = OMITTED_SCENE_HEADING;
				log.debug("scene="+sceneNumberStr + ", #=" + sceneNumber);
			}
			else if (P_SCENE_HEADING.matcher(line.text).matches()) {
				unNumberedScenes++;
				type = TextElementType.SCENE_HEADING;
				line.sceneHeading = line.text;
				if (numberedScenes > 0 && sceneNumber < lastSceneNumber) {
					// generate alpha-style scene numbers if we have some numbered scenes,
					// and we haven't past the last numbered scene header yet.
					if (sceneNumber <= 0) {
						sceneNumber = 1;
					}
					if (sceneAlpha.length() == 0) {
						sceneAlpha = "A";
					}
					else {
						sceneAlpha = SceneDAO.incrementAlpha(sceneAlpha);
					}
					setSceneNumber(line, null);
				}
				else {
					// generate pure numeric scene numbers if the script doesn't have
					// any numbered scenes, or if we've past the last numbered scene.
					sceneNumber++;
					sceneAlpha = "";
					setSceneNumber(line, null);
				}
				log.debug("(unnumbered) scene #=" + sceneNumber);
			}
			else if (line.indent <= MAX_NUM_SCENE_HEADING_INDENT && (m=P_REDUCED_SCENE_HEADING.matcher(line.text)).matches()) {
				numberedScenes++;
				type = TextElementType.SCENE_HEADING;
				String sceneNumberStr = m.group(1);
				setSceneNumber(line, sceneNumberStr);
				line.sceneHeading = m.group(3).trim();
				log.debug("scene="+sceneNumberStr + ", #=" + sceneNumber);
			}
		}

		if (type == TextElementType.OTHER) { // not found yet
			if (line.text.matches(RE_TRANSITION)) {
				/*
				 * Note that this regular expression is quite broad, but we double-check in a
				 * later pass, and may switch it to DIALOGUE type if that seems more appropriate.
				 */
				type = TextElementType.TRANSITION;
			}
			else if (line.text.matches(RE_INTERCUT)) {
				// Use Action for InterCut; Transition sometimes causes premature scene end.
				type = TextElementType.ACTION;
			}
			else if (line.text.matches(RE_END_ACT)) {
				type = TextElementType.END_ACT;
			}
			else if (line.text.matches(RE_START_ACT)) {
				type = TextElementType.START_ACT;
			}
			else if (line.indent >= MIN_CHARACTER_INDENT && line.text.matches(RE_CHARACTER)) {
				type = TextElementType.CHARACTER;
			}
			else if (line.text.equalsIgnoreCase("(MORE)") ) {
				type = TextElementType.MORE;
			}
			else if (line.text.equalsIgnoreCase("(CONTINUED)") ) {
				type = TextElementType.CONTINUATION;
			}
			else if (line.indent >= MIN_TRANSITION_INDENT && line.text.matches(RE_MIN_TRANSITION)) {
				type = TextElementType.TRANSITION;
			}
		}
		line.type = type;
	}

	private int findLastSceneNumber() {
		int sceneNum = 0;
		String sceneNumStr = null;
		for (int i = lines.size()-1; i > 0; i--) {
			TextLine line = lines.get(i);
			Matcher m = null;
			if ((m=P_NUMBERED_SCENE_HEADING.matcher(line.text)).matches() ||
					(m=P_NUMBERED_LEFT_SCENE_HEADING.matcher(line.text)).matches()) {
				sceneNumStr = m.group(1);
				break;
			}
			else if ((m=P_NUMBERED_RIGHT_SCENE_HEADING.matcher(line.text)).matches()) {
				sceneNumStr = m.group(9);
				break;
			}
		}
		setSceneNumber(new TextLine(), sceneNumStr);
		sceneNum = sceneNumber;
		// reset fields changed by setSceneNumber():
		sceneNumber = 0;
		sceneAlpha = "";
		sceneNumbers.clear(); // this is overkill! Seems like it could cause trouble.
		return sceneNum;
	}

	private boolean hadPrefix;
	private boolean hadSuffix;
	/**
	 * Determine a valid scene number for a new scene and set it in the
	 * TextLine object.  Scene numbers are NOT required to be in ascending
	 * order, but must be unique within the script.  A warning message
	 * is issued for cases where the numbers are not ascending.
	 * @param line
	 * @param sceneNumberString
	 */
	private void setSceneNumber(TextLine line, String sceneNumberString) {
		String prefix = "";
		String suffix = "";
		int num = sceneNumber;
		if (sceneNumberString != null && sceneNumberString.length() > 0) {
			Matcher match = pSceneNumber.matcher(sceneNumberString);
			boolean bMatch = match.matches();
			String numberToConvert = sceneNumberString;
			if (bMatch) {
				prefix = match.group(1).trim();
				numberToConvert = match.group(2).trim();
				suffix = match.group(3).trim();
				hadPrefix = hadPrefix || prefix.length() > 0;
				hadSuffix = hadSuffix || suffix.length() > 0;
			}
			try {
				num = Integer.parseInt(numberToConvert);
				if (num > sceneNumber) {
					sceneNumber = num;
					sceneAlpha = "";
				}
				else if (num < sceneNumber) {
					log.warn("out of sequence scene number=" + num + "; last scene #=" + sceneNumber);
					userInfoMessage("ImportFile.SceneOutOfSequence", num, sceneNumber);
				}
				if (sceneNumbers.contains(sceneNumberString)) {
					line.originalSceneNumber = sceneNumberString;
					log.warn("duplicate scene number=" + sceneNumberString);
					userInfoMessage("ImportFile.DuplicateSceneNumber", sceneNumberString);
					sceneNumberString = null; // duplicate, create our own.
				}
			}
			catch (Exception e) {
				line.originalSceneNumber = sceneNumberString;
				sceneNumberString = null; // error in number(?), create our own.
			}
		}
		else {
			line.originalSceneNumber = sceneNumberString;
			sceneNumberString = null; // not supplied, create our own.
		}

		if (sceneNumberString == null) {
			sceneNumberString = sceneAlpha + num;
		}
		// Verify that our proposed new scene number doesn't already exist.
		// It could exist if an imported file had "out of order" or duplicate scene numbers.
		while ( sceneNumbers.contains(sceneNumberString) ) {
			sceneAlpha = SceneDAO.incrementAlpha(sceneAlpha);
			sceneNumberString = sceneAlpha + num;
		}
		sceneNumbers.add(sceneNumberString);
		line.sceneNumberStr = sceneNumberString;
	}

	/**
	 * Figure out which "tab stops" correspond to which line types.
	 * findTabStopsIgnore() uses the overall count of lines at each indent
	 * position to determine a set of tab points.  Typically there
	 * will be at least these (in increasing indent order):
	 *		scene heading
	 *		action text
	 *		dialogue text
	 *		parenthetical text (possibly too few to be counted as tab stop)
	 *		character name
	 * Note that if scene headings are NOT numbered, they will have the
	 * same indentation as action text.
	 * Additional tab points may show up for transitions and page headings.
	 */
	private void analyzeTabStops() {
		int indentAction = 0;
		int indentDialogue = 0;
		int indentParen = 0;
		int indentCharacter = 0;
		if (log.isDebugEnabled()) {
			log.debug("line count=" + lines.size());
			for (int i=0; i < MAX_INDENT; i++) {
				if (indentCount[i] != 0) {
					log.debug(i + ": " + indentCount[i] + " (" + (indentCount[i]*100)/lines.size() + "%)");
				}
			}
		}
		int avgCharIndent = averageIndent(TextElementType.CHARACTER);

		int avgIndentHeading = -1;
		int avgIndentSceneNumbers = -1;
		if (unNumberedScenes > numberedScenes) {
			avgIndentHeading = averageIndent(TextElementType.SCENE_HEADING);
		}
		else {
			avgIndentSceneNumbers = averageIndent(TextElementType.SCENE_HEADING);
		}
		int tabs = 0;
		int ignore;
		for (ignore = 50; tabs < 3 && ignore > 1; ignore-=2 ) {
			tabs = findTabStopsIgnore(ignore);
			log.debug("ignore=" + ignore + ", tabs=" + tabs);
		}
		if (tabs >= 2) {
			if (tabs == 2) {
				// unusual? almost all action or almost all dialog?
				if (tabPos[0] == avgIndentHeading) {
					indentAction = tabPos[0];
					indentDialogue = tabPos[1];
				}
				else if (tabPos[1] == avgCharIndent) { // tab[1] matches Character lines, assume [0] is dialog
					indentDialogue = tabPos[0];
					indentCharacter = tabPos[1];
				}
				else if (tabPos[0] <= (avgIndentSceneNumbers+5)) {
					indentAction = tabPos[1];
				}
			}
			else if (tabs == 3) {
				indentAction = tabPos[0];
				indentDialogue = tabPos[1];
				indentCharacter = tabPos[2];
			}
			else if (tabs == 4) {
				indentAction = tabPos[0];
				indentDialogue = tabPos[1];
				indentParen = tabPos[2];
				indentCharacter = tabPos[3];
			}
		}
		else {
			indentAction = tabPos[0];
			//throw new IllegalArgumentException("Cannot find 2 tab positions over 10%");
		}

		if (indentAction == 0) {
			for ( ignore=5; tabs < 3 && ignore > 0; ignore--) {
				tabs = findTabStopsIgnore(ignore);
				log.debug("ignore=" + ignore + ", tabs=" + tabs);
			}
			if (tabs >= 3) {
				if (tabPos[0] <= (avgIndentSceneNumbers+5)) {
					indentAction = tabPos[1];
				}
				else {
					indentAction = tabPos[0];
				}
			}
		}
		if (indentAction == 0) {
			indentAction = 15; // reasonable guess
		}

		if (indentCharacter == 0) {
			indentCharacter = avgCharIndent;
			if (indentCharacter == 0) { // no Character lines recognized
				indentCharacter = indentAction + 20;
			}
		}

		if (indentDialogue == 0) {
			indentDialogue = indentAction + 10;
		}

		if (indentParen == 0) {
			for ( ignore=5; tabs < 4 && ignore > 0; ignore--) {
				tabs = findTabStopsIgnore(ignore);
				log.debug("ignore=" + ignore + ", tabs=" + tabs);
			}
			if (tabs >= 4) {
				log.debug("tabPos=" + tabPos);
				int n = matchTab(indentDialogue);
				indentParen = tabPos[n+1];
				if (indentParen - indentDialogue < 4) { // probably too small, check next tabPos
					if (tabPos[n+2] > indentParen && tabPos[n+2] < indentCharacter-3) {
						indentParen = tabPos[n+2];
					}
				}
				if (indentParen >= indentCharacter) { // probably no parentheticals at all
					indentParen -= 3;		// but separate the two just in case
				}
			}
		}

		tabPos[0] = indentAction;
		tabType[0] = TextElementType.ACTION;
		tabPos[1] = indentDialogue;
		tabType[1] = TextElementType.DIALOGUE;
		tabPos[2] = indentParen;
		tabType[2] = TextElementType.PARENTHETICAL;
		tabPos[3] = indentCharacter;
		tabType[3] = TextElementType.CHARACTER;
		tabPos[4] = 0;
		log.debug("action=" + indentAction + ", dlg=" + indentDialogue +
				", paren=" + indentParen + ", char=" + indentCharacter);
	}

	private int findTabStopsIgnore(int percentIgnore) {
		tabPos = new int[MAX_TAB_STOPS];
		int tabNumber = -1;
		int ignoreCount = (percentIgnore * lines.size())/100;
		boolean atTab = false;
		for (int i=0; i < MAX_INDENT; i++) {
			if (indentCount[i] > ignoreCount) {
				if (atTab) {
					tabPos[tabNumber] = i;
				}
				else {
					atTab = true;
					if (tabNumber < MAX_TAB_STOPS-1) {
						tabNumber++;
					}
					else {
						EventUtils.logError("Too many 'tab stops' detected.");
					}
					tabPos[tabNumber] = i;
				}
			}
			else {
				atTab = false;
			}
		}
		return tabNumber + 1;
	}

/*	private void analyzeTabPos() {
		boolean done = false;
		int ind;
		int tab;
		tab = matchTab(averageIndent(TextElementType.CHARACTER));
		if (tab >= 0) {
			tabType[tab] = TextElementType.CHARACTER;
			if (tab == 4) {	// most likely situation
				tabType[0] = TextElementType.SCENE_HEADING;
				tabType[1] = TextElementType.ACTION;
				tabType[2] = TextElementType.DIALOGUE;
				tabType[3] = TextElementType.PARENTHETICAL;
				done = true;
				log.debug("tabs t-4");
			}
			else if (tab == 3) { // might be missing parenthetical, or numbered scene headings
				if (numberedScenes < unNumberedScenes && numberedScenes < lines.size()/200) {
					// assume tab for numbered scene headings is missing
					tabType[0] = TextElementType.ACTION;
					tabType[1] = TextElementType.DIALOGUE;
					tabType[2] = TextElementType.PARENTHETICAL;
					log.debug("tabs t-3a");
				}
				else {
					// assume not enough parentheticals existed for a tab stop to be recognized
					tabType[0] = TextElementType.SCENE_HEADING;
					tabType[1] = TextElementType.ACTION;
					tabType[2] = TextElementType.DIALOGUE;
					log.debug("tabs t-3b");
				}
				done = true;
			}
		}
		else if (numberedScenes > unNumberedScenes*5) { // less than 10% unnumbered
			tab = matchTab(averageIndent(TextElementType.SCENE_HEADING));
			log.debug("tabs sh-1, tab=" + tab);
			if (tab >= 0) {
				tabType[tab] = TextElementType.SCENE_HEADING;
				tabType[++tab] = TextElementType.ACTION;
				tabType[++tab] = TextElementType.DIALOGUE;
				if (tabPos[tab+1] - tabPos[tab] < 8) { // small gap, probably one for parenthetical
					tabType[++tab] = TextElementType.PARENTHETICAL;
				}
				tabType[++tab] = TextElementType.CHARACTER;
				done = true;
				log.debug("tabs sh-2, tab=" + tab);
			}
		}
		if ( ! done) {
			tabType[0] = TextElementType.SCENE_HEADING;
			tabType[1] = TextElementType.ACTION;
			tabType[2] = TextElementType.DIALOGUE;
			tabType[3] = TextElementType.PARENTHETICAL;
			tabType[4] = TextElementType.CHARACTER;
			log.warn("tabs default used");
		}
		for (int i=0; i < MAX_TAB_STOPS; i++) {
			log.debug("tab " + i + "=" + tabPos[i]+ ": " + tabType[i]);
		}
	}
*/
	/**
	 * Find the tabPos entry whose indent value matches the 'indent' parameter,
	 * plus or minus 1.
	 * @param indent The indent value to look up.
	 * @return The index of the matching tabPos array entry.  This can be used
	 * as an index to the tabType array to determine the line type.
	 */
	private int matchTab(int indent) {
		int match = -1;
		for (int i=0; i < MAX_TAB_STOPS && tabPos[i] != 0; i++) {
			if (indent >= tabPos[i]-1 && indent <= tabPos[i]+1 ) {
				match = i;
				break;
			}
		}
		return match;
	}

	private int averageIndent(TextElementType t) {
		int sum = 0;
		int n = 0;
		for (TextLine line : lines) {
			if (line.type == t) {
				sum += line.indent;
				n++;
			}
		}
		if (n > 0) {
			sum = sum / n;
		}
		log.debug("avg=" + sum);
		return sum;
	}

	/**
	 * Output any final debugging info, typically corresponding to the overall
	 * import process, but unique to the PDF import.
	 */
	private void finalDebug() {
		if (log.isDebugEnabled()) {
			log.debug("Scenes-- numbered: " + numberedScenes + ", un-numbered: " + unNumberedScenes);
			log.debug("total calculated length of all scenes=" +
					 Scene.formatPageLength(totalScenePageLength));
			log.debug("Character line counts:");
			List<String> list = new ArrayList<String>(characterMap.keySet());
			Collections.sort(list);
			for (String s : list) {
				ScriptElement se = characterMap.get(s);
				log.debug(s + ":  " + se.getLineCount());
			}
		}
	}

	/**
	 * This method may be used by a JUnit test class to test the page-header
	 * matching expressions.
	 *
	 * @param text The text to be matched.
	 * @return True iff ImportPDF would recognize 'text' as a valid page header.
	 */
	public static boolean testHeaderMatch(String text) {
		return (text.matches(RE_PAGE_HEADER_A) ||
				text.matches(RE_PAGE_HEADER_B) ||
				text.matches(RE_PAGE_HEADER_C));
	}

	// * * * PDFScanStrategy implementation methods

	@Override
	public void startPage(PDPage page, int pageNumber) {
		lineNumber = 0;
		pdfPageNum = pageNumber;
		lastLine = null; // used to track if any lines on page
		if (pageNumber <= documentPages) {
			int n = PROGRESS_SCAN - PROGRESS_LOAD;
			n = (n * pageNumber) / documentPages;
			setProgress(PROGRESS_LOAD + n);
		}
	}

	@Override
	public void endPage(PDPage page) {
		if (lastLine != null) {
			lastLine.endOfPage = true;
		}
		else {
			// No text on page (probably a graphic page) -
			// generate 2 lines for page heading and body. rev 4064.
			TextLine line = new TextLine();
			line.text = "";
			line.pdfPageNumber = pdfPageNum; // pdfPageNum is set by the startPage() callback.
			line.lineNumber = 0;	// origin 0
			line.seqLineNumber = 1;	// origin 1
			line.yPos = 0;	// Will be interpreted as a "page heading" line.
			lines.add(line);
			line = new TextLine();
			line.text = "";
			line.pdfPageNumber = pdfPageNum;
			line.lineNumber = 1;
			line.seqLineNumber = 2;
			line.yPos = 100; // Anything past header area is fine. (72=1 inch).
			line.endOfPage = true; // So next input line is considered start of new page.
			lines.add(line);
		}
	}

	/**
	 * This will dump (to debug log) all the "Character point" values in the
	 * given String. Sometimes handy for debugging import problems caused by odd
	 * character values.
	 *
	 * @param text The String to be dumped.
	 */
	private void dumpText(String text) {
		log.debug(text);
		for (int ii = 0; ii < text.length(); ii++) {
			int cp = Character.codePointAt(text.toCharArray(), ii);
			log.debug("numeric value=" + Character.getNumericValue(cp) + ", type=" + Character.getType(cp)
					+ ", name= " + Character.getName(cp));
			if (Character.getType(cp) == Character.CONTROL) {
				log.debug("control char!!");
			}
		}
		/** Common Character.getType() responses:
		 = Character.CONTROL; // getType(0) or other "control" (non-printable) values
		 = Character.DASH_PUNCTUATION; // EM DASH got this = 20
		 = Character.LETTER_NUMBER; // = 10
		 = Character.OTHER_LETTER; // = 5
		 = Character.OTHER_NUMBER; // = 11
		 = Character.OTHER_PUNCTUATION;//=24
		 = Character.UPPERCASE_LETTER; // =1
		 = Character.LOWERCASE_LETTER; //=2
		 = Character.DECIMAL_DIGIT_NUMBER; // =9
		 */
	}

	@Override
	public void startArticle(boolean isltr) {
		// not used for scripts
	}

	@Override
	public void endArticle() {
		// not used for scripts
	}

	/** The object stored in each "node" of our state tables. It defines an action
	 * to be done, and the new state to go to, for a given input. */
	static class StateChange {
		State nextState;
		Action action;
		StateChange(State next, Action act) {
			nextState = next;
			action = act;
		}
	}

	public static class PageLayout {
		int pageNumLoc = PN_LOCATION_UNKNOWN;
		// true iff page numbering is in the format "Page x of y"
		boolean isPageOfStyle = false;

		public final static int PN_LOCATION_UNKNOWN = 0;
		public final static int PN_LOCATION_TOP_RIGHT = 1;
		public final static int PN_LOCATION_TOP_LEFT = 2;
		public final static int PN_LOCATION_BOTTOM_RIGHT = 3;
		public final static int PN_LOCATION_BOTTOM_LEFT = 4;

		public boolean pageNumAtTop() {
			return pageNumLoc == PN_LOCATION_TOP_RIGHT ||
					pageNumLoc == PN_LOCATION_TOP_LEFT;
		}

		public boolean pageNumOnRight() {
			return pageNumLoc == PN_LOCATION_TOP_RIGHT ||
					pageNumLoc == PN_LOCATION_BOTTOM_RIGHT;
		}

	}

}
