/**
 * File: TextStyle.java
 */
package com.lightspeedeps.object;

import java.awt.Color;

import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.app.Constants;

/**
 *  Holds the settings which describe a style of text.  This is used
 *  in managing the signatures and watermarks applied to PDFs.
 */
public class TextStyle {

	/** The font size in points. */
	private int fontSize = Constants.WM_DEF_FONTSIZE;

	/** The water mark's percent opacity, 0-100, 0 being invisible, and
	 * 100 being solid. */
	private int opacity = Constants.WM_DEF_OPACITY;

	/** The color of the water mark text. */
	private Color color = Constants.WM_DEF_COLOR;

	/** The direction of the water mark; the default is bottom-left to
	 * top-right.  See {@link com.lightspeedeps.type.TextDirection}. */
	private TextDirection direction = Constants.WM_DEF_DIRECTION;

	/** If true, only the outline of the letters is drawn. */
	private boolean outlineOnly = true;

	/**
	 * The no-arg constructor.  All values will be set to "reasonable" defaults.
	 */
	public TextStyle() {
	}

	/**
	 * The full constructor, allowing all fields in the WaterMark to be specified.
	 * @param fontsize See {@link #fontSize}
	 * @param dir See {@link #direction}
	 * @param pcolor See {@link #color}
	 * @param popacity See {@link #opacity}
	 * @param poutline See {@link #outlineOnly}
	 */
	public TextStyle(int fontsize, TextDirection dir, Color pcolor, int popacity, boolean poutline) {
		fontSize = fontsize;
		direction = dir;
		color = pcolor;
		opacity = popacity;
		outlineOnly = poutline;
	}

	/** See {@link #fontSize}. */
	public int getFontSize() {
		return fontSize;
	}
	/** See {@link #fontSize}. */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	/** See {@link #opacity}. */
	public int getOpacity() {
		return opacity;
	}
	/** See {@link #opacity}. */
	public void setOpacity(int opacity) {
		this.opacity = opacity;
	}

	/** See {@link #color}. */
	public Color getColor() {
		return color;
	}
	/** See {@link #color}. */
	public void setColor(Color color) {
		this.color = color;
	}

	/** See {@link #direction}. */
	public TextDirection getDirection() {
		return direction;
	}
	/** See {@link #direction}. */
	public void setDirection(TextDirection direction) {
		this.direction = direction;
	}

	/** See {@link #outlineOnly}. */
	public boolean getOutlineOnly() {
		return outlineOnly;
	}
	/** See {@link #outlineOnly}. */
	public void setOutlineOnly(boolean outlineOnly) {
		this.outlineOnly = outlineOnly;
	}

}
