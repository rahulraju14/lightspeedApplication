/*
 * File: ScriptReporter.java
 */
package com.lightspeedeps.util.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.ScriptReportDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.script.ScriptUtils;

import static com.lightspeedeps.util.app.Constants.*;

/**
 * This class handles formatting Script`s for output to a PDF via Jasper
 * reports. In general, it takes parameters indicating the range of pages,
 * scenes, or shooting days to print, and generates all the lines of output as
 * records in the ScriptReport table. After that process completes, a Jasper
 * report is invoked to generate the PDF from the report table data.
 */
public class ScriptReporter {
	private static final Log log = LogFactory.getLog(ScriptReporter.class);

	private final List<Scene> sceneList;

	/** A list of the revision color numbers that apply to each page.  This will be the
	 * maximum of the Scene.lastRevised value for all the Scenes that appear
	 * on that page. */
	private List<Integer> pageColors;

	/** a list of revision numbers describing the revision level of each
	 * page in the given Script, indexed by the physical page number. */
	private List<Integer> pageRevisions;

	/** The starting physical page number, if a page range is to be printed. */
	private final int firstPage;

	/** The last physical page number to print, if a page range is to be printed. */
	private final int lastPage;

	/** The revision level of the current page, for coloring purposes. */
	private int pageRev;

	/** The database report Id field, which uniquely identifies all the records
	 * for this report request. */
	private String reportId;

	/** The Script being printed. */
	private Script script;

	/** The Scene.sequence value of the first scene eligible to be printed. This will only
	 * be greater than 0 if the user was displaying a range of scenes, and asked to
	 * print the current display. */
	private Integer fromSceneSeq;

	/** The Scene.sequence value of the last scene eligible to be printed. This will only
	 * be less than Integer.MAX_VALUE if the user was displaying a range of scenes, and asked to
	 * print the current display. */
	private Integer toSceneSeq;

	/** The Character names of the current user, whose dialogue may be highlighted. */
	private List<String> characterNames;

	/** If true, the user's dialogue lines are highlighted on the report. */
	private boolean highlight;

	/** True iff the last Character text record found was for the highlighted Character;
	 * any Dialogue lines encountered will be highlighted when this is true. */
	private boolean foundChar;

	/** True iff the current Script Report is based on a "shooting day range", or
	 * if the user entered a specific list of scene numbers to print. */
	private boolean dayRange;

	/** True iff the current Script Report is based on a "Page range", or
	 * if the entire script is to be printed. */
	private boolean pageRange;

	/** True iff the current Script Report is based on a revision number. */
	private boolean revision;

	/** The low end (inclusive) of the revision number range determining
	 * which pages to print. */
	private int revisionNumberLow;

	/** The high end (inclusive) of the revision number range determining
	 * which pages to print. */
	private int revisionNumberHigh;

	/** */
	private boolean reformat;

	/** Include the script's title page(s), if any, at the beginning of the report. */
	private boolean includeTitlePages = false;

	/** True if the output should be printed as one of the "Sides" styles. */
	private boolean sidesStyle;

	/** Determines which of the "sides" styles to use. */
	private int sidesType;
	public final static int SIDES_TYPE_SEQUENTIAL = 1; // pages 'n' and 'n+1' on same physical page, 2-up
	public final static int SIDES_TYPE_DUPLICATE = 2; // every page duplicated, 2-up

	private transient SceneDAO sceneDAO;

	/**
	 * Instantiate a new ScriptReporter which may be used to generate a Script
	 * report. After this object's parameters have been set as desired, the
	 * object is passed to the ReportBean's generateScript() method.
	 *
	 * @param pScript The Script to be partially or completely printed.
	 * @param psceneList An optional List of Scene`s to be printed.
	 * @param pfirstPage The starting page, if a page range is to be printed.
	 *            The caller must call setPageRange(true) before calling the
	 *            ReportBean.
	 * @param plastPage The last page, if a page range is to be printed. The
	 *            caller must call setPageRange(true) before calling the
	 *            ReportBean.
	 */
	public ScriptReporter(Script pScript, List<Scene> psceneList, int pfirstPage, int plastPage) {
		sceneList = psceneList;
		firstPage = pfirstPage;
		lastPage = plastPage;
		script = pScript;
	}

	private final static String LEFT_MARGIN =
		"                                                 ".substring(0, Constants.SCRIPT_FMT_LEFT_MARGIN);

	// "style" values for formatHeading()
	public final static int HEADING_NEW_LINE = 0; 	// for email (plain text) script output
	public final static int HEADING_SCREEN = 1;		// for display on screen (web page)
	public final static int HEADING_NO_MARGIN = 2; 	// for PDF output

	/**
	 * Format the scene heading for screen display or printing.
	 * @param scene The scene whose heading is to be created.
	 * @param style The "style" -- various versions of the heading are
	 * created -- for on-screen display, for emailed reports, or
	 * for PDF reports.
	 * @return The formatted scene heading
	 */
	public static String formatHeading(Scene scene, int style) {
		String heading;
		int sceneNumLen = scene.getNumber().length();
		int headLen = SCRIPT_FMT_RIGHT_MARGIN - sceneNumLen;
		switch(style) {
		case HEADING_NEW_LINE:
			heading = NEW_LINE + LEFT_MARGIN;
			break;
		case HEADING_SCREEN:
			heading = " ";	// single leading blank
			sceneNumLen++;	// leading blank uses up one position
			headLen -= (Constants.SCRIPT_FMT_LEFT_MARGIN);
			break;
		default:
		case HEADING_NO_MARGIN:
			heading = "";
			headLen -= Constants.SCRIPT_FMT_LEFT_MARGIN;
			break;
		}
		heading += scene.getNumber();
		if (sceneNumLen < SCRIPT_FMT_ACTION_INDENT) { // pad to point where slug-line starts
			heading += BLANK_LINE.substring(0, SCRIPT_FMT_ACTION_INDENT-sceneNumLen);
		}
		heading += scene.getHeading();
		// compute desired length before we append scene number
		if (heading.length() > headLen) {
			heading = heading.substring(0, headLen);
		}
		else if (heading.length() < headLen) {
			heading += BLANK_LINE.substring(0, headLen - heading.length());
		}
		heading += ' ' + scene.getNumber();
		//log.debug("heading='" + heading + "'");
		return heading;
	}

	/**
	 * Create one or more formatted lines of output from the given TextElement.
	 * This is used for output via email as well as report PDFs. (The PDF report
	 * processor splits the returned text at new-line symbols to create separate
	 * report records.)
	 *
	 * @param tel The TextElement to be formatted.
	 * @return The text formatted with leading blanks (if necessary) preceding
	 *         each line, and system-dependent new-line symbols between lines.
	 */
	private static StringBuffer padLine(TextElement tel) {
		// If the text already has new-line's in it, don't let the format routine break
		// the line at arbitrary positions -- set "split" = false in that case.
		boolean split = !(tel.getText().indexOf(Constants.SCRIPT_NEW_LINE_CHAR) >= 0);
		StringBuffer text = padLine(tel.getType(), tel.getText(), tel.getLineNumber(), split);
		return text;
	}

	/**
	 * Create a stream of formatted output from the given text, based on the
	 * given element type, and the 'split' flag.
	 *
	 * @param type The TextElementType is used to determine the left margin and
	 *            maximum line width of the data.
	 * @param text The text to be formatted.
	 * @param split If false, the text will only be split at existing new-line
	 *            symbols; no additional breaks will be added based on line
	 *            width.
	 * @return The text formatted with leading blanks (if necessary) preceding
	 *         each line, and system-dependent new-line symbols between lines.
	 */
	private static StringBuffer padLine(TextElementType type, String text, int lineNumber, boolean split) {
		StringBuffer buffer = null;
		switch (type) {
		case SCENE_HEADING:
			buffer = new StringBuffer(LEFT_MARGIN).append(text);
			break;
		case ACTION:
			buffer = formatText(text, SCRIPT_FMT_ACTION_INDENT, SCRIPT_FMT_ACTION_WIDTH, split);
			break;
		case DIALOGUE:
			buffer = formatText(text, SCRIPT_FMT_DIALOGUE_INDENT, SCRIPT_FMT_DIALOGUE_WIDTH, split);
			break;
		case PARENTHETICAL:
			buffer = formatText(text, SCRIPT_FMT_PAREN_INDENT, SCRIPT_FMT_PAREN_WIDTH, split);
			break;
		case CHARACTER:
		case MORE: // "(MORE)" has same indent as Character names
		case START_ACT: // Start Act & End Act could be centered, but this is close
		case END_ACT:
			buffer = formatText(text, SCRIPT_FMT_CHARACTER_INDENT, SCRIPT_FMT_CHARACTER_WIDTH, split);
			break;
		case TRANSITION:
			if (text.indexOf(" IN") >= 0) {
				buffer = formatText(text, SCRIPT_FMT_ACTION_INDENT, SCRIPT_FMT_ACTION_WIDTH, split);
			}
			else {
				buffer = formatRightAligned(text, SCRIPT_FMT_TRANSITION_MARGIN);
			}
			break;
		case BLANK:
			buffer = new StringBuffer("");
			break;
		case CONTINUATION:
			// differentiate left-aligned (intro) & right-aligned (ending) continuations
			if (lineNumber > 40) {
				buffer = formatRightAligned(text, SCRIPT_FMT_TRANSITION_MARGIN);
			}
			else {
				buffer = formatText(text, 0, SCRIPT_FMT_RIGHT_MARGIN, split);
			}
			break;
		case PAGE_HEADING:
		case PAGE_FOOTER:
			if (text.length() < 8) { // probably single page number, imported prior to rev 2643
				buffer = formatRightAligned(text, SCRIPT_FMT_RIGHT_MARGIN);
			}
			else { // rev 2643 - page header/footer in db includes original indentation
				buffer = formatAsIsOrTrimmed(text);
			}
			break;
		case OTHER:
			buffer = formatAsIsOrTrimmed(text);
			break;
		case PAGE_TOP:
		case IGNORE:
			break;
		}
		return buffer;
	}

	/**
	 * Create a stream of formatted output from the given text, based on the
	 * given indent and width values, and the 'split' flag.
	 *
	 * @param text The text to be formatted.
	 * @param indent The number of blanks to insert at the beginning of each
	 *            line of output.
	 * @param width The maximum width of a line; this will be ignored if the
	 *            'split' flag is false.
	 * @param split If false, the text will only be split at existing new-line
	 *            symbols; no additional breaks will be added based on line
	 *            width.
	 * @return The text formatted with leading blanks (if necessary) preceding
	 *         each line, and system-dependent new-line symbols between lines.
	 */
	private static StringBuffer formatText(String text, int indent, int width, boolean split) {
		int pos = 0;
		int len = text.length();
		int ix;
		StringBuffer output = new StringBuffer(len*2);
		String indenter = LEFT_MARGIN + BLANK_LINE.subSequence(0, indent);
		while (pos < len) {
			output.append(indenter);
			if ( (len - pos <= width) ) {
				output.append(text.substring(pos));
				break;
			}
			ix = text.indexOf(Constants.SCRIPT_NEW_LINE_CHAR, pos);
			if (! split && ix < 0) {
				// we're not supposed to insert line breaks, and none exist in text
				output.append(text.substring(pos)); // so append the remaining without changes
				break; // and exit.
			}
			if (ix < 0) { // no embedded new-line found; break at a blank if we can:
				ix = pos + width;
				for ( ; ix > pos; ix--) {
					if (text.charAt(ix) == ' ') {
						break;
					}
				}
			}
			if (ix > pos) {
				output.append(text.substring(pos, ix));
				pos = ix + 1; // skip blank
			}
			else { // no blank (word boundary); take max width & add hyphen
				output.append(text.substring(pos, pos+width)).append('-');
				pos += width;
			}
			while (pos < len && text.charAt(pos) == ' ') {
				pos++;
			}
			output.append(Constants.NEW_LINE);
		}
		return output;
	}

	/**
	 * Place the given text into a StringBuffer with sufficient blanks added to
	 * the beginning to cause it to display right-aligned to the margin
	 * specified.
	 *
	 * @param text The String to process.
	 * @param margin The positive margin to which the text should be
	 *            right-aligned.
	 * @return A StringBuffer containing the right-aligned text.
	 */
	private static StringBuffer formatRightAligned(String text, int margin) {
		int len = text.length();
		StringBuffer buff = new StringBuffer(BLANK_LINE.substring(0, margin-len));
		buff.append(text);
		return buff;
	}

	/**
	 * Place the given text as-is into a StringBuffer, unless it is too long to
	 * fit in one line, and has leading blanks ... then we will trim enough
	 * leading blanks off to make the text fit on one line.
	 *
	 * @param text The String to be processed.
	 * @return A StringBuffer containing the text, possibly with some leading
	 *         blanks removed.
	 */
	private static StringBuffer formatAsIsOrTrimmed(String text) {
		int x = text.length();
		if (x >= SCRIPT_FMT_RIGHT_MARGIN) { // only "fix" if too big for one line
			while(text.length() >= SCRIPT_FMT_RIGHT_MARGIN && text.charAt(0)==' ') {
				text = text.substring(1);
			}
		}
		StringBuffer buff = new StringBuffer(text); // print as-is
		return buff;
	}

	public void createReport(String pReportId) {
		reportId = pReportId;
		highlight = (characterNames != null);
		reformat = ! script.getImportType().isFormatted();
		log.debug("1st pg=" + firstPage + ", last pg=" + lastPage);
		log.debug("hilite=" + highlight + ", days=" + dayRange +
				", reformat=" + reformat);
		pageRevisions = createPageRevisionList(script);
		pageColors = createPageColorList(script, pageRevisions);

		if (includeTitlePages) {
			createTitlePages();
		}

		if (dayRange && script.hasPageData()) {
			createReportDays();
		}
		else if (script.hasPageData() && (pageRange || revision)) {
			createReportPages();
		}
		else if (reformat || dayRange) {
			createReportOther();
		}
		else { // most likely a scene range, with page data (formatted)
			createReportPdf();
		}
		flush(); // push any unsaved ScriptReport objects to the database.
	}

	/**
	 * Output any title pages in the script.
	 */
	private void createTitlePages() {
		int firstPageNum = 1;
		if (script.getScenes().size() > 0) {
			Scene firstScene = script.getScenes().get(0);
			firstPageNum = firstScene.getPageNumber();
		}
		if (firstPageNum > 1) {
			int outputPage = -1000;
			for (int pgNum = 1; pgNum < firstPageNum; pgNum++) {
				createReportPage(pgNum, null, outputPage, true);
				outputPage++;
			}
		}
	}

	/**
	 * Prints a range of Scenes
	 */
	private void createReportPdf() {
		if (sceneList.size() == 0) {
			// unusual; possibly corrupted script. rev 4063.
			return;
		}
		Scene firstScene = sceneList.get(0);
		int pageNumber = firstScene.getPageNumber();
		pageRev = pageColors.get(pageNumber);
		ScriptReport sr;
		int lastLineNumber = -1;
		int lineNumber;
		StringBuffer buff;
		boolean output = (pageNumber >= firstPage);
		boolean requested = true;
		boolean priorRequested = true; // track change from requested to non-requested section
		boolean didPageHeading = false;
		Byte status;

		sceneLoop:
		for (Scene scene : sceneList) {
			scene = getSceneDAO().refresh(scene);
			priorRequested = requested;
			requested = (scene.getSequence() >= fromSceneSeq && scene.getSequence() <= toSceneSeq);
			status = (requested ? ScriptReport.STATUS_NORMAL : ScriptReport.STATUS_GRAY);
			pageNumber = scene.getPageNumber();
			if (pageNumber < pageColors.size()) {
				pageRev = pageColors.get(pageNumber);
			}
			else {
				log.debug(pageNumber);
			}
			boolean firstElement = true;
			for (TextElement te : scene.getTextElements()) {
				lineNumber = te.getLineNumber();
				status = determineStatus(te, requested);
				if (te.getType() == TextElementType.PAGE_HEADING ||
						((te.getPageNumber() != null) && (te.getPageNumber() > pageNumber))) {
					if (! didPageHeading) {
						if (!firstElement) {
							pageNumber++;
						}
						pageRev = pageColors.get(pageNumber);
						if (pageNumber > lastPage) {
							break sceneLoop;
						}
						output = (pageNumber >= firstPage);
						if (output) putBlankLines(0, lineNumber, pageNumber, ScriptReport.STATUS_NORMAL);
						didPageHeading = true;
					}
					else {
						if (output) putBlankLines(lastLineNumber+1, lineNumber, pageNumber, ScriptReport.STATUS_NORMAL);
					}
				}
				else {
					didPageHeading = false;
					if (lineNumber > lastLineNumber+1) {
						if (output) putBlankLines(lastLineNumber+1, te.getLineNumber(), pageNumber,
								(priorRequested ? ScriptReport.STATUS_NORMAL : status));
					}
				}
				String lines[];
				if (te.getType() == TextElementType.SCENE_HEADING) {
					// generate our own heading instead of using the line from the PDF (bug#473)
					lines = new String[1];
					lines[0] = ScriptReporter.formatHeading(scene,HEADING_NO_MARGIN);
				}
				else if (te.getText().indexOf(Constants.HTML_BREAK) >= 0) {
					lines = te.getText().split(Constants.HTML_BREAK);
				}
				else {
					lines = te.getText().split(Constants.SCRIPT_NEW_LINE);
				}
				//log.debug(lines.length + ", text=`" + te.getText() + "`");
				for (int i = 0; i < lines.length; i++) {
					if (output) {
						buff = padLine(te.getType(), lines[i], lineNumber, false);
						if (te.getChanged()) {
							buff = addChangeMarker(buff);
						}
						sr = new ScriptReport(reportId, te.getType(), pageNumber, lineNumber,
								te.getChanged(), status, pageRev, buff.toString());
						queue(sr);
					}
					lineNumber++;
				}
				lineNumber--;
				lastLineNumber = lineNumber;
				priorRequested = requested;
				firstElement = false;
			}
		}
	}

	private static StringBuffer addChangeMarker(StringBuffer buff) {
		int len = buff.length();
		if (len < Constants.SCRIPT_FMT_CHANGE_MARGIN) {
			buff.append(BLANK_LINE.substring(0, Constants.SCRIPT_FMT_CHANGE_MARGIN-len));
		}
		buff.append("*");
		return buff;
	}

	/**
	 * Format the output data for a PDF script when the requested data is a
	 * shooting-day range, or a list of Scene numbers. For each Scene, or
	 * contiguous set of scenes, in the sceneList, we will format all the pages
	 * that encompass that Scene or set of Scenes. Scenes within those pages
	 * that were not requested will be printed in light gray.  (In 'sides'
	 * style printing, they will also be "crossed out".)
	 */
	private void createReportDays() {
		log.debug("");
		int sceneCount = sceneList.size();
		Map<Integer,Integer> sceneIndex = new HashMap<Integer,Integer>(sceneCount);
		int ix = 0;
		for (Scene scene : script.getScenes()) {
			// build a mapping from scene.sequence to a sequential scene number with origin 0
			sceneIndex.put(scene.getSequence(), ix);
			ix++;
		}
		int outputPage = 0; // generated page number just for sorting scriptRecords

		List<Integer> requestedSeq = new ArrayList<Integer>();
		for (ix = 0; ix < sceneCount; ix++) {
			Scene firstScene = sceneList.get(ix);
			// build list of all requested scene sequence values (for normal/gray status)
			requestedSeq.clear();
			requestedSeq.add(firstScene.getSequence());
			int lastPage = ScriptUtils.findLastPage(firstScene);

			// Find all subsequent scene(s) that are contiguous or on the same page
			int nextIndex = sceneIndex.get(firstScene.getSequence());
			while (ix < sceneCount-1) {
				Scene nextScene = sceneList.get(ix+1);
				nextIndex++; // number of next contiguous scene
				if (sceneIndex.get(nextScene.getSequence()).intValue() == nextIndex) {
					// next scene to print is the next sequential one in script, so continue
				}
				else if (nextScene.getPageNumber() != lastPage) {
					break; // end of contiguous series of scenes
				}
				requestedSeq.add(nextScene.getSequence());
				lastPage = ScriptUtils.findLastPage(nextScene);
				nextIndex = sceneIndex.get(nextScene.getSequence());
				ix++;
			}

			// determine the pages to format for each scene or range of contiguous scenes
			int firstPage = firstScene.getPageNumber();
			for (int page = firstPage; page <= lastPage; page++) {
				outputPage++;
				createReportPage(page, requestedSeq, outputPage, false); // Get one page's worth of data
			}
		}
	}

	/**
	 * Format the output data for a PDF script when the requested data is a page
	 * range. Note that a user request to print by a revision (color) is also
	 * processed here, as it is marked as a "page range", even though it's
	 * really a revision range.
	 */
	private void createReportPages() {
		log.debug("");
		boolean prtPage;
		for (int pageNum = firstPage; pageNum <= lastPage; pageNum++) {
			prtPage = true;
			if (revision) {
				prtPage = false;
				Page page = script.getPages().get(pageNum-1);
				if (page != null) {
					Integer rev = page.getLastRevised();
					if ((rev != null) && (rev.intValue() <= revisionNumberHigh) &&
							(rev.intValue() >= revisionNumberLow)) {
						prtPage = true;
					}
				}
			}
			if (prtPage) {
				createReportPage(pageNum, null, pageNum, false); // Get one page's worth of data
			}
		}
	}

	/**
	 * Create ScriptReport objects for one page of output when the Script has
	 * Page objects (usually because it was a PDF import).
	 *
	 * @param pageNumber The physical page number for which ScriptReport data
	 *            should be generated.
	 * @param requestedSeq a List of Scene.sequence values, representing all the
	 *            "requested" data.
	 * @param outputPage The page number assigned to the output (ScriptReport)
	 *            records, for purposes of sorting when the printed PDF is
	 *            generated by Jasper. For 'shooting day' reports we cannot use
	 *            the physical page number for this value, since the pages will
	 *            be ordered by scenes within shooting days, and not by the
	 *            original physical page number.
	 * @param includeNonScene if true, non-scene data (such as title pages) will
	 *            be included; if false, only text associated with a scene will
	 *            be output.
	 */
	private void createReportPage(int pageNumber, List<Integer> requestedSeq, int outputPage,
			boolean includeNonScene) {
		ScriptReport sr;
		int lastLineNumber = -1;
		int lineNumber;
		StringBuffer buff;
		boolean requested = true;
		boolean priorRequested = true; // track change from requested to non-requested section
		boolean didPageHeading = false;
		Byte status;
		Page page = script.getPages().get(pageNumber-1);
		pageRev = pageColors.get(pageNumber);

		for (TextElement te : page.getTextElements()) {
			Scene scene = te.getScene();
			if (scene == null && ! includeNonScene) {
				continue;
			}
			priorRequested = requested;
			if (requestedSeq != null && scene != null) {
				requested = requestedSeq.contains(scene.getSequence());
			}
			lineNumber = te.getLineNumber();
			status = determineStatus(te, requested);
			if (te.getType() == TextElementType.PAGE_HEADING) {
				if (! didPageHeading) {
					putBlankLines(0, lineNumber, outputPage, ScriptReport.STATUS_NORMAL);
					didPageHeading = true;
				}
				else {
					putBlankLines(lastLineNumber+1, lineNumber, outputPage, ScriptReport.STATUS_NORMAL);
				}
			}
			else {
				didPageHeading = false;
				if (lineNumber > lastLineNumber+1) {
					putBlankLines(lastLineNumber+1, te.getLineNumber(), outputPage,
							(priorRequested ? ScriptReport.STATUS_NORMAL : status));
					// priorRequested test forces blank lines following a requested section
					// to have normal status, not grayed-out, for jasper x-out processing.
				}
			}
			String lines[];
			if (te.getType() == TextElementType.SCENE_HEADING && scene != null) {
				// generate our own heading instead of using the line from the PDF (bug#473)
				lines = new String[1];
				lines[0] = ScriptReporter.formatHeading(scene,HEADING_NO_MARGIN);
			}
			else {
				lines = te.getText().split(Constants.SCRIPT_NEW_LINE);
			}
			//log.debug(lines.length + ", text=`" + te.getText() + "`");
			for (int i = 0; i < lines.length; i++) {
				buff = padLine(te.getType(), lines[i], lineNumber, false);
				if (te.getChanged()) {
					buff = addChangeMarker(buff);
				}
				sr = new ScriptReport(reportId, te.getType(), outputPage, lineNumber,
						te.getChanged(), status, pageRev, buff.toString());
				queue(sr);
				lineNumber++;
			}
			lineNumber--;
			lastLineNumber = lineNumber;
		}
	}

	/**
	 * Format the output data for a non-PDF script.
	 */
	private void createReportOther() {
		foundChar = false;
		boolean requested = true;
		Scene firstScene = sceneList.get(0);
		int pageNumber = firstScene.getPageNumber();
		boolean output = doOutput(pageNumber);
		int lineNumber = 1;
		String lines[];
		Byte status;
		ScriptReport sr;
		StringBuffer buff;
		int linesPerPage = Constants.SCRIPT_MAX_LINES_PER_PAGE;
		if (script.getLinesPerPage() > 40 && script.getLinesPerPage() < linesPerPage) {
			linesPerPage = script.getLinesPerPage();
		}

		if (dayRange) { // shooting day range, generate page numbers starting at 1
			lineNumber = insertPageBreak(1, "1");
			pageNumber = 1;
		}
		else { // not shooting day range
			if (firstPage == firstScene.getPageNumber()) {
				pageRev = pageColors.get(pageNumber);
				lineNumber = insertPageBreak(pageNumber, ""+pageNumber);
			}
		}
		pageRev = pageColors.get(pageNumber);

		sceneLoop:
		for (Scene scene : sceneList) {
			scene = getSceneDAO().refresh(scene);
			if (reformat && (scene.getPageNumber() > pageNumber)) {
				pageNumber = scene.getPageNumber();
				lineNumber = 0;
				pageRev = pageColors.get(pageNumber);
				output = doOutput(pageNumber);
				if (output) {
					lineNumber = insertPageBreak(pageNumber, ""+pageNumber);
				}
			}
			requested = (scene.getSequence() >= fromSceneSeq && scene.getSequence() <= toSceneSeq);
			status = (requested ? ScriptReport.STATUS_NORMAL : ScriptReport.STATUS_GRAY);
			for (TextElement te : scene.getTextElements()) {
				if (te.getType() == TextElementType.PAGE_HEADING ||
						te.getType() == TextElementType.PAGE_FOOTER ||
						te.getType() == TextElementType.MORE) {
					continue;	// skip; we're generating our own page breaks
					// TODO Skip CONTINUATION?
				}
				lineNumber++;
				if (lineNumber > linesPerPage ||
						(lineNumber >= linesPerPage-1 && te.getType() == TextElementType.CHARACTER) ||
						(lineNumber >= linesPerPage-2 && te.getType() == TextElementType.SCENE_HEADING) ) {
					// don't put a Character name on the last line of the page
					pageNumber++;
					lineNumber = 0;
					if (pageNumber <= lastPage) {
						if (pageNumber < pageColors.size()) {
							pageRev = pageColors.get(pageNumber);
						}
						output = doOutput(pageNumber);
						if (output) {
							lineNumber = insertPageBreak(pageNumber, ""+pageNumber);
						}
					}
				}
				if (pageNumber > lastPage) {
					break sceneLoop;
				}
				output = doOutput(pageNumber);
				status = determineStatus(te, requested);
				if (reformat) { // TAGGER or other non-PDF input; break lines on width
					if (te.getType() == TextElementType.SCENE_HEADING) {
						buff = new StringBuffer(formatHeading(scene,HEADING_NEW_LINE));
					}
					else {
						buff = padLine(te);
					}
					lines = buff.toString().split(Constants.NEW_LINE);
				}
				else { // PDF - use stored line breaks
					if (te.getText().indexOf(Constants.HTML_BREAK) >= 0) {
						lines = te.getText().split(Constants.HTML_BREAK);
					}
					else {
						lines = te.getText().split(Constants.SCRIPT_NEW_LINE);
					}
					for (int i = 0; i < lines.length; i++) {
						lines[i] = padLine(te.getType(), lines[i], lineNumber, false).toString();
					}
				}
				if (te.getType() == TextElementType.ACTION ||
						te.getType() == TextElementType.CHARACTER ||
						te.getType() == TextElementType.SCENE_HEADING) {
					// need to insert a blank line before these types
					if (output)
						putBlankLines(lineNumber, lineNumber+1, pageNumber, status);
					lineNumber++;
				}
				for (int i = 0; i < lines.length; i++) {
					if (lineNumber > linesPerPage) {
						pageNumber++;
						lineNumber = 0;
						if (pageNumber > lastPage) {
							break sceneLoop;
						}
						if (pageNumber < pageColors.size()) {
							pageRev = pageColors.get(pageNumber);
						}
						output = doOutput(pageNumber);
						if (output) {
							lineNumber = insertPageBreak(pageNumber, ""+pageNumber);
						}
					}
					if (output) {
						sr = new ScriptReport(reportId, te.getType(), pageNumber, lineNumber,
								te.getChanged(), status, pageRev, lines[i]);
						if (sr.getText().length() > 100) { // maybe bad import?
							sr.setText(sr.getText().substring(0, 100));
						}
						queue(sr);
					}
					lineNumber++;
				}
				lineNumber--;
			}
		}
	}

	/**
	 * Output blank lines for line numbers 'start' through 'end-1'.
	 * @param start The line number of the first ScriptReport object to be created.
	 * @param end The line number one greater than the last ScriptReport object to be created.
	 * @param pageNumber The current pageNumber; this value is set in the ScriptReport object,
	 * but is not otherwise used.
	 * @param status
	 */
	private void putBlankLines(int start, int end, int pageNumber, Byte status) {
		ScriptReport sr;
		for (int line = start; line < end; line++) {
			sr = new ScriptReport(reportId, TextElementType.BLANK, pageNumber, line,
					false, status, pageRev, "");
			queue(sr);
		}
	}

	/**
	 * Outputs the ScriptReport records for a page heading, and the following
	 * blank line, if the pageNumber falls within the requested range. It also
	 * resets the flag indicating we are in the middle of highlighted dialog.
	 *
	 * @param pageNumber The physical page number for the page heading.
	 * @param pageNumStr The logical page number -- what is actually printed in
	 *            the page heading.
	 * @return The line number of the last line output; currently 2, even if no
	 *         output is produced, as our caller is probably tracking the
	 *         "virtual" page location, waiting to reach the requested page.
	 */
	private int insertPageBreak(int pageNumber, String pageNumStr) {
		foundChar = false; // don't carry highlighting across page boundaries.
		if (pageNumber >= firstPage && pageNumber <= lastPage) {
			String str = MsgUtils.formatMessage("ScriptReport.PageHeader", pageNumStr);
			//StringBuffer buff = padLine(TextElementType.PAGE_HEADING, str, 0, false);
			StringBuffer buff = formatRightAligned(str, SCRIPT_FMT_TRANSITION_MARGIN);
			ScriptReport sr = new ScriptReport(reportId, TextElementType.PAGE_HEADING, pageNumber, 1,
					false, ScriptReport.STATUS_NORMAL, pageRev, buff.toString());
			queue(sr);
			putBlankLines(2, 3, pageNumber, ScriptReport.STATUS_NORMAL);
		}
		return 2;
	}

	/**
	 * Determine if the current text element should be formatted as normal,
	 * grayed out, or highlighted.
	 *
	 * @param text The TextElement under consideration.
	 * @param requested The current "requested" status, usually based on if the
	 *            Scene containing this TextElement was in the requested range
	 *            of data.
	 * @return One of the status values defined in the ScriptReport class.
	 */
	private Byte determineStatus(TextElement text, boolean requested) {
		Byte status = (requested ? ScriptReport.STATUS_NORMAL : ScriptReport.STATUS_GRAY);
		if (text.getType() == TextElementType.PAGE_HEADING ||
				text.getType() == TextElementType.PAGE_FOOTER) {
			status = ScriptReport.STATUS_NORMAL;
		}
		if (highlight && (characterNames != null)) {
			if (text.getType() == TextElementType.CHARACTER) {
				String name = ScriptUtils.extractCharacterName(text.getText(), null);
				if (characterNames.contains(name)) {
					foundChar = true;
					//status = ScriptReport.STATUS_HIGHLIGHT; // this highlights the character heading, too.
				}
				else {
					foundChar = false;
				}
			}
			else if (foundChar && (text.getType() == TextElementType.DIALOGUE) && requested) {
				status = ScriptReport.STATUS_HIGHLIGHT;
			}
		}
		//log.debug("status=" + status);
		return status;
	}


	private boolean doOutput(int pageNum) {
		boolean bRet = pageNum >= firstPage;
		if (revision && pageNum < pageRevisions.size()) {
			int rev = pageRevisions.get(pageNum);
			bRet = (rev >= revisionNumberLow && rev <= revisionNumberHigh);
		}
		return bRet;
	}

	/** Returns a list of all the logical page numbers, indexed by the physical page number.
	 * That is, list.get(i) is the logical page number of the i'th physical page. */
	public static List<String> createPageNumberList(Script script) {
		List<String> scriptPageNumbers;
		int page = 0;
		// avoid later problems if script.lastPage == 0 :
		int lastPage = Math.max(1, script.getLastPage().intValue());
		if (script.getPageNumbers().length() > 0) {
			scriptPageNumbers = new ArrayList<String>(Arrays.asList(script.getPageNumbers().split("\\|")));
			page = scriptPageNumbers.size();
		}
		else {
			scriptPageNumbers = new ArrayList<String>(lastPage+1);
		}
		// This takes care of scripts loaded before 12/16/10, or have other problems with
		// their "pageNumbers" value.
		for ( ; page <= lastPage; page++) {
			scriptPageNumbers.add(page, ""+page);
		}
		return scriptPageNumbers;
	}

	/**
	 * Create a list of Integers describing the revision color of each page in
	 * the given Script.
	 *
	 * @param script The script to be processed.
	 * @return A List with one more entry than there are pages in the script.
	 *         Each entry represents the color number, from 1 to 20, to be used
	 *         when displaying the corresponding page.
	 */
	public static List<Integer> createPageColorList(Script script, List<Integer> pageRevs) {
		ScriptDAO scriptDAO = ScriptDAO.getInstance();
		Project project = script.getProject();
		Map<Integer,Integer> revToColor = new HashMap<Integer,Integer>();
		for (int ix = 0; ix < pageRevs.size(); ix++) {
			Integer rev = pageRevs.get(ix);
			Integer color = revToColor.get(rev);
			if (color == null) {
				Script s = scriptDAO.findByRevisionAndProject(rev, project);
				if (s != null) {
					color = s.getColorName().getScriptRevision();
				}
				else {
					color = rev;
				}
				revToColor.put(rev, color);
			}
			pageRevs.set(ix, color);
		}
		return pageRevs;
	}

	/**
	 * Create a list of revision numbers describing the revision level of each
	 * page in the given Script. For Script's imported from a PDF there should
	 * be a collection of Page objects which contain the revision information.
	 * For other Script's, the Scene objects hold the revision data and must be
	 * mapped to the appropriate page numbers.
	 *
	 * @param script The Script whose page revision information is to be
	 *            generated.
	 * @return A List of Integer's, where the n'th entry in the List is the
	 *         revision number of (physical) page n. The List will not be null
	 *         and will have one more entry than the number of pages in the
	 *         script. It will always have at least one entry (the zeroth).
	 */
	public static List<Integer> createPageRevisionList(Script script) {
		int lastPage = 0;
		boolean usePages = false;
		if (script.getLastPage() != null) { // only null if import failed
			lastPage = script.getLastPage();
		}
		if (script.getPages() != null && script.getPages().size() > 1) {
			// get the physical page # from the last Page in the List.
			int pageNum = script.getPages().get(script.getPages().size()-1).getNumber();
			lastPage = Math.max(lastPage, pageNum);
			usePages = true;
		}

		List<Integer> pageRevisions = new ArrayList<Integer>(lastPage+1);
		for (int i = 0; i <= lastPage; i++) {
			pageRevisions.add(i, 1); // create all entries & set to rev 1
		}

		if (usePages) { // typical for PDF imports
			for (Page page : script.getPages()) {
				pageRevisions.set(page.getNumber(), page.getLastRevised());
			}
		}
		else {
			List<Scene> listscene = script.getScenes();
			int highRev = 0;
			int lastRev = 1;
			int pageRev = 1;
			int pageNum = 0;
			for (Scene scene : listscene) {
				if (scene.getPageNumber() > pageNum) {
					for ( ; pageNum < scene.getPageNumber(); pageNum++) {
						if (pageNum > lastPage) { // can happen for manual 'imports'
							for (int i = lastPage+1; i <= pageNum; i++) {
								pageRevisions.add(i, 1); // create extra entries & set to rev 1
							}
							lastPage = pageNum;
						}
						pageRevisions.set(pageNum, Math.max(pageRev, highRev));
						pageRev = 1;
					}
					pageRev = lastRev; // assume last scene spilled onto new page
					highRev = 1;
				}
				lastRev = scene.getLastRevised();
				if (lastRev > highRev) {
					highRev = lastRev;
				}
			}
			for ( ; pageNum <= lastPage; pageNum++) {
				pageRevisions.set(pageNum,  Math.max(pageRev, highRev));
				pageRev = 1;
			}
		}
		return pageRevisions;
	}

	private static final int BUFFER_TRIGGER = 200;
	private List<ScriptReport> buffer;

	/**
	 * Save the ScriptReport object passed so that a number of them can be
	 * pushed to the database all at once.
	 *
	 * @param sr The ScriptReport record to save.
	 */
	private void queue(ScriptReport sr) {
		if (buffer == null) {
			buffer = new ArrayList<ScriptReport>(BUFFER_TRIGGER);
		}
		if (sr.getText().length() < 200) {
			buffer.add(sr);
		}
		else {
			splitLines(sr);
		}
		if (buffer.size() >= BUFFER_TRIGGER) {
			flush();
		}
	}

	/**
	 * @param sr
	 */
	public void splitLines(ScriptReport sr) {
		String text = sr.getText();
		log.debug("" + text);
		int lineLen = 59;
		ScriptReport s;
		String line;
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
				line = "         " + text.substring(0, n);
				line = line.replace('!', ' ');
				line = line.replace('%', ' ');
				s = new ScriptReport(sr.getReportId(), sr.getType(), sr.getPageNumber(), sr.getLineNumber(),
						sr.getChanged(), sr.getStatus(), sr.getRevColor(), line);
				buffer.add(s);
				if (n >= text.length()) {
					break;
				}
				text = text.substring(n+1);
			}
		}
		else {
			int i = 0;
			int len = text.length();
			for (; i < len-lineLen; i += lineLen) {
				line = "         " + text.substring(i, i+lineLen);
				s = new ScriptReport(sr.getReportId(), sr.getType(), sr.getPageNumber(), sr.getLineNumber(),
						sr.getChanged(), sr.getStatus(), sr.getRevColor(), line);
				buffer.add(s);
			}
			line = "         " + text.substring(i);
			s = new ScriptReport(sr.getReportId(), sr.getType(), sr.getPageNumber(), sr.getLineNumber(),
					sr.getChanged(), sr.getStatus(), sr.getRevColor(), line);
			buffer.add(s);
		}
	}

	/**
	 * Send any saved ScriptReport records to the database.
	 */
	private void flush() {
		if (buffer != null) {
			ScriptReportDAO.getInstance().save(buffer);
			buffer.clear();
		}
	}

	/** See {@link #script}. */
	public Script getScript() {
		return script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	/** See {@link #fromSceneSeq}. */
	public Integer getFromSceneSeq() {
		return fromSceneSeq;
	}
	/** See {@link #fromSceneSeq}. */
	public void setFromSceneSeq(Integer fromSceneSeq) {
		this.fromSceneSeq = fromSceneSeq;
	}

	/** See {@link #toSceneSeq}. */
	public Integer getToSceneSeq() {
		return toSceneSeq;
	}
	/** See {@link #toSceneSeq}. */
	public void setToSceneSeq(Integer toSceneSeq) {
		this.toSceneSeq = toSceneSeq;
	}

	/** See {@link #reportId}. */
	public String getReportId() {
		return reportId;
	}
	/** See {@link #reportId}. */
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	/** See {@link #includeTitlePages}. */
	public boolean getIncludeTitlePages() {
		return includeTitlePages;
	}
	/** See {@link #includeTitlePages}. */
	public void setIncludeTitlePages(boolean includeTitlePages) {
		this.includeTitlePages = includeTitlePages;
	}

	/** See {@link #sidesStyle}. */
	public boolean getSidesStyle() {
		return sidesStyle;
	}
	/** See {@link #sidesStyle}. */
	public void setSidesStyle(boolean sidesStyle) {
		this.sidesStyle = sidesStyle;
	}

	/** See {@link #sidesType}. */
	public int getSidesType() {
		return sidesType;
	}
	/** See {@link #sidesType}. */
	public void setSidesType(int sidesType) {
		this.sidesType = sidesType;
	}

	/** See {@link #characterNames}. */
	public List<String> getCharacterNames() {
		return characterNames;
	}
	/** See {@link #characterNames}. */
	public void setCharacterNames(List<String> characterName) {
		characterNames = characterName;
	}

	/** See {@link #dayRange}. */
	public boolean getDayRange() {
		return dayRange;
	}
	/** See {@link #dayRange}. */
	public void setDayRange(boolean dayRange) {
		this.dayRange = dayRange;
	}

	/** See {@link #pageRange}. */
	public boolean getPageRange() {
		return pageRange;
	}
	/** See {@link #pageRange}. */
	public void setPageRange(boolean pageRange) {
		this.pageRange = pageRange;
	}

	/** See {@link #revision}. */
	public boolean getRevision() {
		return revision;
	}
	/** See {@link #revision}. */
	public void setRevision(boolean revision) {
		this.revision = revision;
	}

	/** See {@link #revisionNumberLow}. */
	public int getRevisionNumberLow() {
		return revisionNumberLow;
	}
	/** See {@link #revisionNumberLow}. */
	public void setRevisionNumberLow(int revisionNumberLow) {
		this.revisionNumberLow = revisionNumberLow;
	}

	/** See {@link #revisionNumberHigh}. */
	public int getRevisionNumberHigh() {
		return revisionNumberHigh;
	}
	/** See {@link #revisionNumberHigh}. */
	public void setRevisionNumberHigh(int revisionNumber) {
		revisionNumberHigh = revisionNumber;
	}

	/** See {@link #sceneDAO}. */
	public SceneDAO getSceneDAO() {
		if (sceneDAO == null) {
			sceneDAO = SceneDAO.getInstance();
		}
		return sceneDAO;
	}

}
