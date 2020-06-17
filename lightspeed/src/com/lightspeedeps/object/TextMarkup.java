/**
 * File: TextMarkup.java
 */
package com.lightspeedeps.object;

import java.util.List;

import com.lightspeedeps.util.app.Constants;

/**
 *  Holds data to be placed on a page, typically a PDF page, with
 *  position and page placement information.
 */
public class TextMarkup {

	/** Horizontal position of the water mark.  For center-aligned (the default),
	 * this is the page coordinate of the middle of the water mark.  Coordinates
	 * are in "points" (72 points = 1 inch), 0 is the left-hand edge.  Specifying
	 * a negative value is the same as specifying 1/2 of the page width, or 1/4
	 * of the page-width of 'twoUp' is true. */
	private int horizontalPos = Constants.WM_DEF_HPOS;

	/** Vertical position of the water mark.  For center-aligned (the default),
	 * this is the page coordinate of the middle of the water mark.  Coordinates
	 * are in "points" (72 points = 1 inch), 0 is the bottom of the page.
	 * Specifying a negative value is the same as specifying 1/2 of the page
	 * height. */
	private int verticalPos = Constants.WM_DEF_VPOS;

	/** Which page to place this text on.  If zero, the text should be placed on
	 * all pages of the document. */
	private int pageNumber;

	/** The text to be added to the page.  The first entry in the list is placed
	 * at the location specified; subsequent entries are positioned below the
	 * previous one. */
	private List<String> text;

	/**
	 * The no-arg constructor.
	 */
	public TextMarkup() {
	}

	/**
	 * The full constructor:
	 * @param hpos See {@link #horizontalPos}
	 * @param vpos See {@link #verticalPos}
	 * @param text See {@link #text}
	 */
	public TextMarkup(int hpos, int vpos, int pageNum, List<String> ptext) {
		horizontalPos = hpos;
		verticalPos = vpos;
		pageNumber = pageNum;
		text = ptext;
	}

	/** See {@link #horizontalPos}. */
	public int getHorizontalPos() {
		return horizontalPos;
	}
	/** See {@link #horizontalPos}. */
	public void setHorizontalPos(int horizontalPos) {
		this.horizontalPos = horizontalPos;
	}

	/** See {@link #verticalPos}. */
	public int getVerticalPos() {
		return verticalPos;
	}
	/** See {@link #verticalPos}. */
	public void setVerticalPos(int verticalPos) {
		this.verticalPos = verticalPos;
	}

	/** See {@link #pageNumber}. */
	public int getPageNumber() {
		return pageNumber;
	}
	/** See {@link #pageNumber}. */
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	/** See {@link #text}. */
	public List<String> getText() {
		return text;
	}
	/** See {@link #text}. */
	public void setText(List<String> text) {
		this.text = text;
	}

}
