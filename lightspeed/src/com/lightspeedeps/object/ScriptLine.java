//	File Name:	ScriptLine.java
package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.model.TextElement;
import com.lightspeedeps.type.TextElementType;


/**
 * The ScriptLine class is used to hold the script data to be displayed
 * on the Script Revisions page.
 */
public class ScriptLine implements Serializable {
	/** */
	private static final long serialVersionUID = 1634037849106551191L;

	/** The type of text (dialogue, action, etc.). */
	private TextElementType type;

	/** The text itself.  Leading & trailing whitespace is normally removed during
	 * the script import process. */
	private String text;

	/** The CSS class name used to display this text (used by ScriptPageBean). */
	private String cssClass;

	/** Text highlighting (as user's dialog): 0=none, 1=normal, 2=white.
	 * (white highlight is used if page revision color is yellow.) */
	private int highLight;

	// Constructors

	/** default constructor */
	public ScriptLine() {
	}

	/** constructor */
	public ScriptLine(TextElementType type, String text) {
		this.type = type;
		this.text = text;
	}

	/** constructor */
	public ScriptLine(TextElementType type, String text, String css) {
		this(type, text);
		cssClass = css;
	}

	public ScriptLine(TextElement te) {
		this(te.getType(), te.getText(), te.getCssClass());
		highLight = te.getHighLight();
	}

	public TextElementType getType() {
		return this.type;
	}
	public void setType(TextElementType type) {
		this.type = type;
	}

	public String getText() {
		return this.text;
	}
	public void setText(String text) {
		this.text = text;
	}

	/** See {@link #cssClass}. */
	public String getCssClass() {
		return cssClass;
	}
	/** See {@link #cssClass}. */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/** See {@link #highLight}. */
	public int getHighLight() {
		return highLight;
	}
	/** See {@link #highLight}. */
	public void setHighLight(int highLight) {
		this.highLight = highLight;
	}

}
