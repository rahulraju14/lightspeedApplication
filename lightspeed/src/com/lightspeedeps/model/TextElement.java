//	File Name:	TextElement.java
package com.lightspeedeps.model;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.TextElementType;

/**
 * TextElement entity.  A collection of these holds the textual part of a script.
 * They are grouped by Scene and ordered by a sequence number.  The TextElements
 * are normally created during a script import process.
 */
@Entity
@Table(name = "text_element", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Scene_Id", "Sequence" }))
public class TextElement extends PersistentObject<TextElement> implements Cloneable {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TextElement.class);

	private static final long serialVersionUID = 2712895963630490056L;

	public static final int SEQUENCE_INCREMENT = 10;

	// Fields

	/** The Scene to which this TextElement belongs. */
	private Scene scene;

	/** The Page to which this TextElement belongs. */
	private Page page;

	/** The physical page number this section of text starts on. */
	private Integer pageNumber;

	/** The type of text (dialogue, action, etc.). */
	private TextElementType type;

	/** The sequence number used to keep the bits of text in the right order.  Typically
	 * this begins at 1 and increases sequentially (by 1), but that's not a requirement. */
	private Integer sequence;

	/** The line number on the (printed) page where this text should
	 * appear.  Set based on the imported PDF appearance. */
	private Integer lineNumber = -1;

	/** The text itself.  Leading & trailing whitespace is normally removed during
	 * the script import process. */
	private String text;

	/** True if the imported script had a change marker such as "*" on the line corresponding
	 * to this TextElement. */
	private boolean changed;

	/** The CSS class name used to display this text (used by ScriptPageBean). */
	@Transient
	private String cssClass;

	/** Text highlighting (as user's dialog): 0=none, 1=normal, 2=white.
	 * (white highlight is used if page revision color is yellow.) */
	@Transient
	private int highLight;

	// Constructors

	/** default constructor */
	public TextElement() {
	}

	/** full constructor */
	public TextElement(Scene scene, TextElementType type, Integer sequence, String text) {
		this.scene = scene;
		this.type = type;
		this.sequence = sequence;
		this.text = text;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Scene_Id")
	public Scene getScene() {
		return this.scene;
	}
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Page_Id", nullable = true)
	public Page getPage() {
		return page;
	}
	public void setPage(Page pg) {
		page = pg;
		if (page == null) {
			setPageNumber(null);
		}
		else {
			setPageNumber(page.getNumber());
		}
	}

	/** See {@link #pageNumber}. */
	@Transient
	public Integer getPageNumber() {
		return pageNumber;
	}
	/** See {@link #pageNumber}. */
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false)
	public TextElementType getType() {
		return this.type;
	}
	public void setType(TextElementType type) {
		this.type = type;
	}

	@Column(name = "Sequence", nullable = false)
	public Integer getSequence() {
		return this.sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/** See {@link #lineNumber}. */
	@Column(name = "Line_Number", nullable = false)
	public Integer getLineNumber() {
		return lineNumber;
	}
	/** See {@link #lineNumber}. */
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	/** See {@link #changed}. */
	public boolean getChanged() {
		return changed;
	}
	/** See {@link #changed}. */
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	@Column(name = "Text", length = 5000)
	public String getText() {
		return this.text;
	}
	public void setText(String text) {
		this.text = text;
	}

	public boolean contentMatches(TextElement textElement) {
		boolean bRet = false;
		if ( textElement != null &&
				getType() == textElement.getType() &&
				( (getText() == null && textElement.getText() == null) ||
					(getText() != null && getText().equals(textElement.getText())) )
				) {
			bRet = true;
		}
		return bRet;
	}


	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		result = prime * result + ((getSequence() == null) ? 0 : getSequence().hashCode());
		result = prime * result + ((getLineNumber() == null) ? 0 : getLineNumber().hashCode());
		result = prime * result + ((getText() == null) ? 0 : getText().hashCode());
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
		TextElement other;
		try {
			other = (TextElement)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(other.getId()) ) {
			return true;
		}
		boolean b = false;
		b = (getType() == null && other.getType() == null) ||
			(getType() == other.getType());
		if (!b) return false;

		b = (getSequence() == null && other.getSequence() == null) ||
			(getSequence() != null && getSequence().equals(other.getSequence()) );
		if (!b) return false;

		b = (getLineNumber() == null && other.getLineNumber() == null) ||
			(getLineNumber() != null && getLineNumber().equals(other.getLineNumber()) );
		if (!b) return false;

		b = (getText() == null && other.getText() == null) ||
			(getText() != null && getText().equals(other.getText()) );

		return b;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "#" + getId() + ": " + getSequence() + ". (" + getType() + ") " + getText();
		s += ']';
		return s;
	}

	/** See {@link #cssClass}. */
	@Transient
	public String getCssClass() {
		return cssClass;
	}
	/** See {@link #cssClass}. */
	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/** See {@link #highLight}. */
	@Transient
	public int getHighLight() {
		return highLight;
	}
	/** See {@link #highLight}. */
	public void setHighLight(int highLight) {
		this.highLight = highLight;
	}

	/**
	 * Called by the Java serialization framework when this object is being
	 * serialized. We will null out the Page reference while this is serialized,
	 * and restore it immediately afterward. We do this because of the way
	 * Script`s are constructed -- with both Scene and Page lists. Since Scene
	 * and Page objects both of have lists of TextElement`s, but their lists
	 * rarely end with the same item (only if a Scene happens to end at the
	 * bottom of a page), the process of serializing (or deserializing) can
	 * result in an extremely deep stack -- causing stack overflow.
	 * <p>
	 * So instead of saving the Page reference, we save the page.number value;
	 * when the containing Script object is de-serialized, it will fill in the
	 * appropriate Page references based on the pageNumber values.
	 * <p>
	 * See {@link Script#readObject}.
	 *
	 * @param out The output stream being used to serialize this object.
	 * @throws java.io.IOException Iff in.defaultReadObject() throws it. See
	 *             {@link java.io.ObjectInputStream#defaultReadObject}.
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
		// log.debug("start pg=" + (page==null?"?":page.getNumber()) + " ln=" + lineNumber);
		// Writing Page may cause Script to be 'nearly recursive' & get stack overflow;
		// so only serialize page.number.
		Page wpage = getPage();
		if (wpage != null && getPageNumber() == null) {
			setPageNumber(wpage.getNumber());
		}
		this.page = null;

		try {
			out.defaultWriteObject();
		}
		catch (Exception e) {
		}
		finally {
			this.page = wpage;
		}
		// log.debug("done, pg=" + (page==null?"?":page.getNumber()) + " ln=" + lineNumber);
	}

}