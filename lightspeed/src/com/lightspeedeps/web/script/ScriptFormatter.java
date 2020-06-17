/*
 * File: ScriptFormatter.java
 */
package com.lightspeedeps.web.script;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.PageDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.script.ScriptUtils;

import static com.lightspeedeps.util.app.Constants.*;

/**
 * This class handles formatting Script`s that were imported from
 * a non-PDF file.
 * @see #format(Script)
 */
public class ScriptFormatter {
	private static final Log log = LogFactory.getLog(ScriptFormatter.class);

	/** The Script being printed. */
	private Script script;

	/** The script prior to the given one (in revision order), used for
	 * determining which pages have been revised in the new version. */
	private Script priorScript;

	/** The TextElement currently being processed. */
	private TextElement currentTe = null;

	/** The character name from the last CHARACTER TextElement processed;
	 * used for generating a "cont'd" CHARACTER element when a dialog is
	 * split across a page boundary. */
	private String lastCharacter = "";

	/** A List of TextElement`s created for the Scene currently being processed. These
	 * are elements that should be added to the Scene's collection after we're done
	 * processing the Scene.  Currently this is just the page-header entries. */
	private final List<TextElement> addedElements = new ArrayList<TextElement>();

	/** The physical page number currently being formatted. */
	private int pageNumber;
	/** The line number (within the current page) currently being created. */
	private int lineNumber;

	private final SceneDAO sceneDAO = SceneDAO.getInstance();
	private final PageDAO pageDAO = PageDAO.getInstance();
	private final ScriptDAO scriptDAO = ScriptDAO.getInstance();

	/** The line number assigned to the first line of text output on a page,
	 * not including the page header. */
	private final static int TOP_LINE = 2;


	/**
	 * A class to hold the format() method, for formatting non-PDF
	 * scripts.
	 */
	public ScriptFormatter() {
	}

	/**
	 * Generate page-layout data for a non-PDF script. This includes creating
	 * Page objects for the script, adding TextElement`s for page headers, and
	 * possibly splitting up existing TextElement`s such as dialog and action,
	 * if necessary, to accommodate page boundaries.
	 *
	 * @param scr The Script to be processed.
	 * @return The revised Script instance (which is a different instance than
	 *         the calling parameter) unless the reformatting routine throws an
	 *         Exception, in which case null is returned.
	 */
	public Script format(Script scr) {
		log.debug("");
		boolean bRet = false;
		try {
			script = scr;
			script = scriptDAO.refresh(script);
			if (script.getImportType().isFormatted()) {
				return script;
			}

			removePagination(script);
			bRet = reformat();	// build Page data and reformat the TextElements

			if (bRet) {
				scriptDAO.evict(script);
				// without 'evict', updateHash() calculated wrong hash value, probably due to
				// order of TextElements -- they're not necessarily in sequence after "reformat()".
				script = scriptDAO.refresh(script);
				int rev = script.getRevisionNumber();
				priorScript = scriptDAO.findByRevisionAndProject(rev-1, SessionUtils.getCurrentProject());
				log.debug("prior script=" + priorScript);
				for (Page page : script.getPages()) {
					page.updateHash();
					page.setLastRevised(rev);
					if (priorScript != null && priorScript.hasPageData()) {
						ScriptUtils.updateRevision(page, priorScript);
					}
					pageDAO.attachDirty(page);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			script = null;
		}
		return script;
	}

	/**
	 * This method does the bulk of the work of generating the page layout data.
	 * @return True.
	 */
	private boolean reformat() {
		Scene firstScene = script.getScenes().get(0);
		if (firstScene.getTextElements() != null &&
				firstScene.getTextElements().size() > 0) {
			currentTe = firstScene.getTextElements().get(0);
		}
		else {
			return false;
		}
		String lines[];
		StringBuffer buff;
		int linesPerPage = SCRIPT_MAX_LINES_PER_PAGE;
		if (script.getLinesPerPage() > 40 && script.getLinesPerPage() < linesPerPage) {
			// script value appears within reason, so use it.
			// The computed lines-per-page for FDX doesn't include the title line or space
			// after it, but we include that when formatting, so bump up the lines-per-page
			// to allow for that.
			linesPerPage = script.getLinesPerPage() + 2;
		}

		pageNumber = 1;
		if (firstScene.getPageNumber() != null && firstScene.getPageNumber() > 1) {
			pageNumber = firstScene.getPageNumber();
		}
		Page page = createPageBreak(firstScene);

		for (Scene scene : script.getScenes()) {
			scene = sceneDAO.refresh(scene);
			List<TextElement> textElems = scene.getTextElements();
			int priorDialogLines = 0;
			for (int elNum = 0; elNum < textElems.size(); elNum++ ) {
				TextElement te = textElems.get(elNum);
				currentTe = te;
				if ((scene.getPageNumber() > pageNumber)) {
					// scene forces new page; check after we have 1st TE set up
					pageNumber = scene.getPageNumber();
					page = createPageBreak(scene);
				}
				if (te.getType() == TextElementType.PAGE_HEADING ||
						te.getType() == TextElementType.PAGE_FOOTER) {
					textElems.remove(elNum);
					elNum--;
					continue;	// skip; we're generating our own page breaks
				}
				int minExtra = 0; // how many more lines must be guaranteed on this page?
				boolean addBlank = true; // several types require a blank line before them
				switch( te.getType()) {
					case ACTION:
					case TRANSITION:
						break;
					case CHARACTER:
						priorDialogLines = 0;
						lastCharacter = te.getText();
						if (lineNumber > (linesPerPage-4)) {
							// close to bottom of page, determine "minExtra" based on following dialog
							minExtra = calculateMinDialogLines(elNum, textElems);
						}
						else {
							minExtra = 1; // needs an extra line for at least 1 line of dialog
						}
						break;
					case SCENE_HEADING:
						minExtra = 2; // needs 2 more lines - blank plus 1 line of action
						break;
					default:	// other types do not need a preceding blank line (e.g., dialog)
						addBlank = false;
						break;
				}
				if (addBlank && (lineNumber != TOP_LINE)) {
					lineNumber++; // need a blank line before this type
				}
				if (lineNumber > (linesPerPage - minExtra) ) {
					// not enough room left on page for whatever type is coming
					pageNumber++;
					page = createPageBreak(scene);
				}
				if (te.getType() == TextElementType.SCENE_HEADING) {
					scene.setPageNumber(pageNumber);
					scene.setPageNumStr(""+pageNumber);
					scene.setLineNumber(lineNumber);
					buff = new StringBuffer(formatHeading(scene));
				}
				else {
					buff = splitLine(te.getType(), te.getText(), lineNumber);
				}
				te.setText(buff.toString());
				lines = buff.toString().split(SCRIPT_NEW_LINE);
				int numLines = lines.length;
				te.setLineNumber(lineNumber);
				te.setPage(page);
				if ((lineNumber + numLines - 1) > linesPerPage) {
					// it won't all fit on current page - either push or split
					boolean push = false;
					if (numLines <= 2 && ! te.getType().isDialogue()) {
						// any chunk of 2 lines should not get split (special rules below for dialog)
						push = true;
					}
					else if (lineNumber == linesPerPage) { // only room for 1 more line
						if (te.getType() == TextElementType.ACTION) {
							// don't split action text if only 1 line would fit on page
							push = true;
						}
						else if (te.getType().isDialogue() && (priorDialogLines == 0)) {
							// no prior dialog (e.g., parenthetical) so push; if there was
							// preceding parenthetical, it's ok to output single line -- this
							// dialog and preceding parenthetical are considered same unit.
							push = true;
						}
					}
					if (push) {
						pageNumber++;
						page = createPageBreak(scene);
						te.setLineNumber(lineNumber);
						te.setPage(page);
					}
					else { // split the TextElement
						numLines = linesPerPage - lineNumber + 1;
						if (te.getType().isDialogue() && numLines > 1) {
							numLines--;	// leave room for 'MORE' line
						}
						List<TextElement> newTes = splitText(scene, te, lines, numLines);
						scene.getTextElements().addAll(elNum+1, newTes);
					}
					priorDialogLines = 0;
				}
				if (te.getType().isDialogue()) {
					priorDialogLines += numLines;
				}
				lineNumber += numLines;
			}
			scene.getTextElements().addAll(addedElements);
			addedElements.clear();
			sceneDAO.attachDirty(scene);
		}

		String pg = "";
		for (int i = 0; i <= pageNumber; i++) {
			pg += "|" + i;
		}
		script.setLastPage(pageNumber);
		script.setPageNumbers(pg.substring(1));
		script.setLinesPerPage(linesPerPage);
		if (script.getImportType() == ImportType.TAGGER) {
			script.setImportType(ImportType.TAGGER_F);
		}
		else {
			script.setImportType(ImportType.FDX_F);
		}
		scriptDAO.attachDirty(script);
		return true;
	}

	/**
	 * We've found a Character line near the bottom of the page. Look at the
	 * following Dialog or Parenthetical text(s) to figure out the minimum number
	 * of remaining lines we need to allow on this page. If it's a single line
	 * of dialog, 1 line is enough; for 2 or 3 lines, we need to fit it all; for
	 * 4 or more lines, we need at least 2 on this page.  All the contiguous Dialog
	 * or Parenthetical elements following the Character count towards the formatted "dialog body"
	 * length.
	 *
	 * @param elNum The current element number (index) -- of the Character
	 *            TextElement.
	 * @param textElems The array of TextElements for the current Scene.
	 * @return An int of 1, 2, or 3, depending on the length of the dialog that
	 *         follows the current Character element.
	 */
	private int calculateMinDialogLines(int elNum, List<TextElement> textElems) {
		int minLines = 1;
		int numLines = 0; // count how many lines of dialog follow this character
		// Add up all lines in contiguous Dialogue & Parenthetical elements, except
		// that if we reach 5 lines, no need to count further, as this won't affect
		// the calculation.
		for (elNum++; elNum < textElems.size() && numLines < 5; elNum++) {
			TextElement te = textElems.get(elNum);
			if (te.getType().isDialogue()) {
				StringBuffer buff = splitLine(te.getType(), te.getText(), lineNumber);
				String[] lines = buff.toString().split(SCRIPT_NEW_LINE);
				numLines += lines.length; // how many lines of dialog follow this character?
			}
			else {
				break;
			}
		}
		if (numLines > 1) {
			minLines = 2;	// don't leave a single line of split dialog at bottom of page
			if (numLines > 2) {
				// for 3 or more lines, need 3 - either fit all 3 (if 3), or if > 3, need three
				// because it will need 2 lines of dialog + "more" line.
				minLines = 3;
			}
		}
		return minLines;
	}

	/**
	 * Create a Page object and add it to the database, and create a TextElement
	 * for the page heading and add it to the 'addedElements' field. It also
	 * resets the line number to the appropriate value for the first text line
	 * on the page.
	 *
	 * @param scene The Scene currently being processed -- the page header
	 *            TextElement will be associated with this Scene.
	 * @return The Page object created.
	 */
	private Page createPageBreak(Scene scene) {
		String string = "" + pageNumber;
		Page page = new Page(script, pageNumber, string);
		script.getPages().add(page);
		pageDAO.save(page);

		string += ".";
		StringBuffer buff = formatRightAligned(string, SCRIPT_FMT_TRANSITION_MARGIN);
		int sequence = currentTe.getSequence() - 5;
		TextElement te = new TextElement(scene, TextElementType.PAGE_HEADING, sequence, buff.toString());
		te.setPage(page);
		te.setPageNumber(pageNumber);
		te.setLineNumber(0);
		addedElements.add(te);

		lineNumber = TOP_LINE ;

		return page;
	}

	/**
	 * Split off the trailing data in the given TextElement into a new one, and
	 * add it to the scene so that it follows the current one.
	 *
	 * @param scn The Scene containing the TextElement being modified.
	 * @param te The current TextElement, which is to be split.
	 * @param lines The array of text lines generated from the existing
	 *            TextElement (te)
	 * @param keep The number of lines to keep in the existing TextElement.
	 * @return A non-null List of 1 or more TextElement`s to be added to the
	 *         Scene.
	 */
	private List<TextElement> splitText(Scene scn, TextElement te, String[] lines, int keep) {
		List<TextElement> elems = new ArrayList<TextElement>();
		TextElement teNew;
		String text = "";
		int i = 0;
		for (; i < keep; i++) {
			text += SCRIPT_NEW_LINE_CHAR + lines[i];
		}
		te.setText(text.substring(1));

		/*
		 * Note that the sequence number must be set so the elements are in the right order, and
		 * come between the existing elements; and are arranged so that when a page-break is generated
		 * it will be placed before the "cont'd" element.
		 */
		if (te.getType() == TextElementType.DIALOGUE) {
			teNew = new TextElement(scn, TextElementType.MORE, te.getSequence()+1, "(MORE)");
			elems.add(teNew);
			teNew = new TextElement(scn, TextElementType.CHARACTER, te.getSequence()+7, lastCharacter + " (CONT'D)");
			elems.add(teNew);
		}
		text = "";
		for (; i < lines.length; i++ ) {
			text += " " + lines[i];
		}
		teNew = new TextElement(scn, te.getType(), te.getSequence()+8, text.substring(1));
		elems.add(teNew);
		return elems;
	}

	/**
	 * Split or format a paragraph of text by inserting new-line symbols at the
	 * appropriate positions, or adding padding, based on the TextElementType
	 * (which determines the margins).
	 *
	 * @param type The TextElementType of the text being split.
	 * @param text The text to split.
	 * @param lineNumber The current line number on the page; this is used to
	 *            determine (guess) whether a CONTINUATION text element is at
	 *            the top or bottom of the page.
	 * @return A StringBuffer containing the given text, modified by the
	 *         addition of line-end symbols, and possibly blank padding for text
	 *         that is displayed right-aligned.
	 *
	 */
	private StringBuffer splitLine(TextElementType type, String text, int lineNumber) {
		boolean split = true;
		StringBuffer buffer = null;
		switch (type) {
		case SCENE_HEADING:
			buffer = new StringBuffer(text);
			break;
		case ACTION:
			buffer = splitText(text, SCRIPT_FMT_ACTION_WIDTH, split);
			break;
		case DIALOGUE:
			buffer = splitText(text, SCRIPT_FMT_DIALOGUE_WIDTH, split);
			break;
		case PARENTHETICAL:
			buffer = splitText(text, SCRIPT_FMT_PAREN_WIDTH, split);
			break;
		case CHARACTER:
		case MORE: // "(MORE)" has same indent as Character names
		case START_ACT: // Start Act & End Act could be centered, but this is close
		case END_ACT:
			buffer = splitText(text, SCRIPT_FMT_CHARACTER_WIDTH, split);
			break;
		case TRANSITION:
			if (text.indexOf(" IN") >= 0) {
				buffer = splitText(text, SCRIPT_FMT_ACTION_WIDTH, split);
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
				buffer = splitText(text, SCRIPT_FMT_RIGHT_MARGIN, split);
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
	 * Create a stream of delimited output from the given text, based on the
	 * given width values, and the 'split' flag.
	 *
	 * @param text The text to be formatted.
	 * @param width The maximum width of a line; this will be ignored if the
	 *            'split' flag is false.
	 * @param split If false, the text will only be split at existing new-line
	 *            symbols; no additional breaks will be added based on line
	 *            width.
	 * @return The text broken into lines of the appropriate length, with
	 *         system-dependent new-line symbols between lines.
	 */
	private StringBuffer splitText(String text, int width, boolean split) {
		int pos = 0;
		int len = text.length();
		int ix;
		StringBuffer output = new StringBuffer(len*2);
		while (pos < len) {
			if ( (len - pos <= width) ) {
				output.append(text.substring(pos));
				break;
			}
			ix = text.indexOf(SCRIPT_NEW_LINE_CHAR, pos);
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
			output.append(SCRIPT_NEW_LINE_CHAR);
		}
		return output;
	}

	/**
	 * Format the scene heading for screen display or printing.
	 * @param scene The scene whose heading is to be created.
	 * @return The formatted scene heading
	 */
	private static String formatHeading(Scene scene) {
		String heading;
		int sceneNumLen = scene.getNumber().length();
		int headLen = SCRIPT_FMT_RIGHT_MARGIN - sceneNumLen - SCRIPT_FMT_LEFT_MARGIN;
		heading = "";
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

	/**
	 * Delete any existing Page objects related to the given Script.
	 *
	 * @param scrpt The Script of interest.
	 */
	private void removePagination(Script scrpt) {
		List<Page> pages = scrpt.getPages();
		pages.size();
		scrpt.setPages(new ArrayList<Page>());
		scriptDAO.attachDirty(scrpt);
		if (pages != null) {
			for (Page page : pages) {
				page.setScript(null);
				pageDAO.delete(page);
			}
		}

	}

}
