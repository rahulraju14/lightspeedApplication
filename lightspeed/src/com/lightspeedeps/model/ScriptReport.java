package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.TextElementType;

/**
 * ScriptReport entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "script_report")
public class ScriptReport extends PersistentObject<ScriptReport> implements java.io.Serializable {
	private static final long serialVersionUID = 7866283619013075333L;

	public static final Byte STATUS_NORMAL = 0;
	public static final Byte STATUS_GRAY = 1;
	public static final Byte STATUS_HIGHLIGHT = 2;

	// Fields
	private String reportId;
	private TextElementType type;
	private Integer pageNumber;
	private Integer lineNumber;
	private Boolean changed;
	private Byte status = STATUS_NORMAL;
	private Integer revColor = 1;
	private String text;

	// Constructors

	/** default constructor */
	public ScriptReport() {
	}

	/** full constructor */
	public ScriptReport(String reportId, TextElementType type, Integer pageNumber, Integer lineNumber,
			Boolean changed, Byte status, int color, String text) {
		this.reportId = reportId;
		this.type = type;
		this.pageNumber = pageNumber;
		this.lineNumber = lineNumber;
		this.changed = changed;
		this.status = status;
		this.revColor = color;
		this.text = text;
	}

	// Property accessors

	@Column(name = "report_id", nullable = false, length = 100)
	public String getReportId() {
		return this.reportId;
	}
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public TextElementType getType() {
		return this.type;
	}
	public void setType(TextElementType type) {
		this.type = type;
	}

	@Column(name = "Page_Number", nullable = false)
	public Integer getPageNumber() {
		return this.pageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Column(name = "Line_Number", nullable = false)
	public Integer getLineNumber() {
		return this.lineNumber;
	}
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Changed", nullable = false)
	public Boolean getChanged() {
		return this.changed;
	}
	public void setChanged(Boolean changed) {
		this.changed = changed;
	}

	/** See {@link #status}. */
	@Column(name = "Status", nullable = false)
	public Byte getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(Byte status) {
		this.status = status;
	}

	@Column(name = "Rev_Color", nullable = false)
	public Integer getRevColor() {
		return revColor;
	}
	public void setRevColor(Integer revColor) {
		this.revColor = revColor;
	}

	@Column(name = "Text", length = 200)
	public String getText() {
		return this.text;
	}
	public void setText(String text) {
		this.text = text;
	}

}