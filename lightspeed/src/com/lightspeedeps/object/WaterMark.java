/**
 * File: WaterMark.java
 */
package com.lightspeedeps.object;

import java.awt.Color;

import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.app.Constants;

/**
 *  Holds all the settings necessary to create a water-mark on a PDF.
 */
public class WaterMark extends TextStyle {

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

	/** If true, the print-out should be "2-up".  The water mark will be
	 * output twice on each page -- the first one at the specified location,
	 * and the second one shifted right half of the page's width. */
	private boolean twoUp = false;

	/** The text to be used as the water mark. */
	private String text = "";

	/** True iff today's date should be included in the watermark text. */
	private boolean includeDate;


	/**
	 * The no-arg constructor.  All values will be set to "reasonable" defaults,
	 * except that no text is set.  You will need to set the text field to some
	 * non-blank value for the water mark to be visible!
	 */
	public WaterMark() {
	}

	/**
	 * Construct a WaterMark with all the default values, and the given text.
	 * @param msg
	 */
	public WaterMark(String msg) {
		text = msg;
	}

	/**
	 * The full constructor, allowing all fields in the WaterMark to be specified.
	 * @param hpos See {@link #horizontalPos}
	 * @param vpos See {@link #verticalPos}
	 * @param fontsize See {@link TextStyle#fontSize}
	 * @param dir See {@link TextStyle#direction}
	 * @param pcolor See {@link TextStyle#color}
	 * @param popacity See {@link TextStyle#opacity}
	 * @param poutline See {@link TextStyle#outlineOnly}
	 * @param msg See {@link #text}
	 */
	public WaterMark(int hpos, int vpos, int fontsize,
			TextDirection dir, Color pcolor, int popacity, boolean poutline, String msg) {
		super(fontsize, dir, pcolor, popacity, poutline);
		horizontalPos = hpos;
		verticalPos = vpos;
		text = msg;
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

	/** See {@link #twoUp}. */
	public boolean getTwoUp() {
		return twoUp;
	}
	/** See {@link #twoUp}. */
	public void setTwoUp(boolean twoUp) {
		this.twoUp = twoUp;
	}

	/** See {@link #includeDate}. */
	public boolean isIncludeDate() {
		return includeDate;
	}
	/** See {@link #includeDate}. */
	public void setIncludeDate(boolean includeDate) {
		this.includeDate = includeDate;
	}

	/** See {@link #text}. */
	public String getText() {
		return text;
	}
	/** See {@link #text}. */
	public void setText(String text) {
		this.text = text;
	}

}
