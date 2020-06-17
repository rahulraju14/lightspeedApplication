/**
 * File: TextLine.java
 */
package com.lightspeedeps.object;

import com.lightspeedeps.type.TextElementType;

/**
 * Used in ImportPDF and ScriptUtils.  This object describes a single
 * line of input from an imported PDF file.
 */
public class TextLine {

	/** The type of entity this TextLine represents, such as a scene heading
	 * or page header. */
	public TextElementType type = TextElementType.OTHER;

	/** The character string for this line. */
	public String text = null;

	/** Starting x (horizontal) position of this line of text. */
	public int startX = 0;

	/** The y (vertical) position of this line, as reported by the PDF scanner. */
	public float yPos;

	/** calculated indent, based on average character width passed to writeLine */
	public int indent = 0;

	/** Line number on the page computed based on the "Y" coordinate of the text. */
	public int lineNumber;

	/** A sequential line number, set to 1 for the first line of each
	 * page, and incremented by 1 for each line passed to us from the PDF scanner. */
	public int seqLineNumber;

	/** physical page number, from the PDF scanning process */
	public int pdfPageNumber;

	/** logical (script) page number, printed in a page header or footer */
//	public String scriptPageNumber;

	/** Set to true if this is the last line on a page. */
	public boolean endOfPage;
//	public boolean continued;

	/** Set to true for Character lines where we detect a "V.O." (voice-over)
	 *  or "O.S." (off-screen) annotation. */
	public boolean offScreen;

	/** Set to true if this line had a "*" change marker in the right margin. */
	public boolean changed;

	/** The scene number - only set for scene heading lines. */
	public String sceneNumberStr;

	/** set for scene heading lines if this was a duplicate or other error */
	public String originalSceneNumber;

	/** The scene heading text -- only set for scene heading lines. */
	public String sceneHeading;


	public TextLine() {
	}

	@Override
	public String toString() {
		String s;
		s = String.format("%3d-%2d %3d(%3.0f) ", pdfPageNumber, lineNumber, indent, yPos);
		s += " '" + text + "' ";
		s += "[" + type + "] ";
		s += (offScreen ? "(offScreen) " : "");
		s += (endOfPage ? "(endPage) " : "");
		s += (sceneHeading == null ? "" : "(Hdg:" + sceneHeading + ")" );
		return s;
	}

}
