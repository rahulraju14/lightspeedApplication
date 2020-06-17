package com.lightspeedeps.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.persistence.*;

import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.StringUtils;


/**
 * "Page" entity for tracking script contents and changes. Each Script has
 * a List of unique Page objects.  Each Page has a List of TextElement's.
 * Note that the TextElement objects (rows) are shared between Page and
 * Scene objects.
 */
@Entity
@Table(name = "page", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Script_Id", "Number" }))
public class Page extends PersistentObject<Page> implements Cloneable {

	//private static final Log LOG = LogFactory.getLog(Page.class);

	private static final long serialVersionUID = 3250960131900918817L;

	// Fields

	/** The Script that owns this page. */
	private Script script;

	/** The physical page number within the Script, starting at 1. This is based
	 * on the page numbers supplied by the PDF parser for PDF imports. */
	private Integer number;

	/** The logical page number this scene starts on -- the page number
	 * (which may include alpha characters) printed on the page header
	 * in the imported file (typically only for PDFs). */
	private String pageNumStr;

	/** The internal revision number of the Script which last changed this
	 * page.  Used to determine page color. */
	private Integer lastRevised = 1;

	/**
	 * The MD5 hash generated from the List of TextElements of this page. This
	 * is used primarily for determining whether a page has changed from one
	 * revision to the next. If two Pages' hashes are the same, then they contain
	 * the exact same text and layout, as the hash includes both the textual
	 * content of the TextElements and the line number the TextElements begin
	 * on.
	 * <p>
	 * The hash generation has to deal with one peculiarity: change markers
	 * ("*"). When a script is imported, we normally store all the contiguous
	 * lines of one type in a single TextElement, e.g., a piece of dialogue that
	 * takes three lines on the page is stored in one TextElement. If all the
	 * lines in that piece are marked "changed", that is still the case, and the
	 * TextElement has its 'changed' field set to true. But if, for example, the
	 * first line is marked changed, and the next two are not, this will be
	 * stored as two TextElements: one for the first line, with 'changed' =
	 * true, and another for the next two lines, with 'changed' = false.
	 * However, we want to hash it as if it were one TextElement, because the
	 * next import may have all three lines identical, with no change markers,
	 * and therefore be stored as one TextElement, and we need these two cases
	 * to compare as equal.  See {@link #updateHash()} method for how this is handled.
	 */
	private byte[] hash;

	/** The List of all TextElements that make up this page. */
	private List<TextElement> textElements = new ArrayList<>(0);

	/** The hash code computed from the List of TextElements comprising this page. */

	// Constructors

	/** Default constructor; most fields are null, however the textElements field
	 * is set to an empty List. */
	public Page() {
	}

	/**
	 * Constructor used in PDF import.
	 * @param script The Script this Page is part of.
	 * @param number The physical page number this object corresponds to.
	 * @param pageNumStr The logical (printed) page number of this Page.
	 */
	public Page(Script script, Integer number, String pageNumStr) {
		this.script = script;
		this.number = number;
		this.pageNumStr = pageNumStr;
	}

	// Property accessors

	/** See {@link #script}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Id")
	public Script getScript() {
		return this.script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	/** See {@link #number}. */
	@Column(name = "Number")
	public Integer getNumber() {
		return this.number;
	}
	/** See {@link #number}. */
	public void setNumber(Integer number) {
		this.number = number;
	}

	/** See {@link #pageNumStr}. */
	@Column(name = "Page_Num_Str", length = 30)
	public String getPageNumStr() {
		return this.pageNumStr;
	}
	/** See {@link #pageNumStr}. */
	public void setPageNumStr(String pageNumStr) {
		this.pageNumStr = pageNumStr;
	}

	@Column(name = "LastRevised", nullable = false)
	public Integer getLastRevised() {
		return this.lastRevised;
	}
	public void setLastRevised(Integer lastRevised) {
		this.lastRevised = lastRevised;
	}

	/** See {@link #hash}. */
	@Column(name = "Hash", columnDefinition="VarBinary(32)")
	public byte[] getHash() {
		return this.hash;
	}
	/** See {@link #hash}. */
	public void setHash(byte[] hash) {
		this.hash = hash;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "page")
	@OrderBy("sequence")
	public List<TextElement> getTextElements() {
		return this.textElements;
	}
	public void setTextElements(List<TextElement> textElement) {
		textElements = textElement;
		//updateHash();
	}

	/**
	 * Updates this Page's hash field based on the contents of the List of
	 * TextElements. See {@link #hash}.
	 *
	 * @return 'this' object.
	 */
	public Page updateHash() {
		MessageDigest digest;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			for (ListIterator<TextElement> iter = getTextElements().listIterator(); iter.hasNext(); ) {
				TextElement te = iter.next();
				TextElementType type = te.getType();
				if (type == TextElementType.PAGE_HEADING ||
						type == TextElementType.BLANK || type == TextElementType.PAGE_FOOTER ||
						te.getLineNumber() < 1) {
					// ignore page headings and blank lines
				}
				else {
					digest.update(te.getLineNumber().byteValue());
					String paragraph = StringUtils.removeBlankPadding(te.getText());
					if (type == TextElementType.SCENE_HEADING || type == TextElementType.OTHER
							|| type == TextElementType.CONTINUATION) {
						// skip "merge" process for these line types
					}
					else {
						// Consume all lines that are contiguous and of the same type.
						// This solves the problem of the same text being stored in a
						// single TextElement in one script, but split up in another
						// script due to change markers on individual lines.
						int line = te.getLineNumber();
						while( iter.hasNext() ) {
							line += StringUtils.countOf(te.getText(), Constants.SCRIPT_NEW_LINE_CHAR) + 1;
							te = iter.next();
							if (te.getType() != type || te.getLineNumber() != line) {
								iter.previous(); // point iter to last processed element
								break;
							}
							paragraph += Constants.SCRIPT_NEW_LINE_CHAR +
									StringUtils.removeBlankPadding(te.getText());
						}
						//log.debug("L=" + paragraph.length() + ", p=" + paragraph);
					}
					digest.update(paragraph.getBytes());
					//log.debug("line=" + te.getLineNumber() + ", len=" + paragraph.length() + "`" + paragraph + "`");
				}
			}
			hash = digest.digest();
			//log.debug("len=" + hash.length + ", bytes=" + StringUtils.toHexString(hash));
		}
		catch (NoSuchAlgorithmException e) {
			EventUtils.logError(e);
		}
		return this;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : getId());
		s += ", #=" + (getNumber()==null ? "null" : getNumber());
		s += ", pg=" + (getPageNumStr()==null ? "null" : getPageNumStr());
		s += ", rv=" + (getLastRevised()==null ? "null" : getLastRevised());
		s += "]";
		return s;
	}

//	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//		log.debug("");
//		in.defaultReadObject();
//		log.debug("done, pg=" + number);
//	}
//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("start pg=" + number);
//		out.defaultWriteObject();
//		log.debug("done, pg=" + number);
//	}

}