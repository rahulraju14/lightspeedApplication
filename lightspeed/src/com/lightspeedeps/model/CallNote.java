package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * CallNote entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "call_note")
public class CallNote extends PersistentObject<CallNote> implements Cloneable {
	//private static final Log log = LogFactory.getLog(CallNote.class);

	// Fields
	private static final long serialVersionUID = 1L;

	private Callsheet callsheet;
	private Integer section;
	private String heading;
	private Integer lineNumber;
	private String body;

	// Constructors

	/** default constructor */
	public CallNote() {
	}

	/** full constructor */
	public CallNote(Callsheet callsheet, Integer section, String heading,
			Integer lineNumber, String body) {
		this.callsheet = callsheet;
		this.section = section;
		this.heading = heading;
		this.lineNumber = lineNumber;
		this.body = body;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Callsheet_Id", nullable = false)
	public Callsheet getCallsheet() {
		return this.callsheet;
	}

	public void setCallsheet(Callsheet callsheet) {
		this.callsheet = callsheet;
	}

	@Column(name = "Section", nullable = false)
	public Integer getSection() {
		return this.section;
	}

	public void setSection(Integer section) {
		this.section = section;
	}

	@Column(name = "Heading", length = 100)
	public String getHeading() {
		return this.heading;
	}

	public void setHeading(String heading) {
		this.heading = heading;
	}

	@Column(name = "Line_Number")
	public Integer getLineNumber() {
		return this.lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Body", length = 5000)
	public String getBody() {
		return this.body;
	}

	public void setBody(String body) {
		//log.debug("body="+body);
		this.body = body;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + id;
		s += ", sec=" + section;
		s += ", line=" + lineNumber;
		s += ", hd=" + heading;
		s += ", body=" + body;
		s += "]";
		return s;
	}

	@Override
	public Object clone() {
		try {
			CallNote copy = (CallNote)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

}