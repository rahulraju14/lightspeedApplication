package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.lightspeedeps.type.OtherCallType;

/**
 * OtherCall entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "other_call")
public class OtherCall extends PersistentObject<OtherCall> {

	private static final long serialVersionUID = 2032122863381098217L;

	// Fields
	private Callsheet callsheet;
	private OtherCallType type;
	private Integer lineNumber;
	private String description;
	private Integer count;
	private Date time;

	// Constructors

	/** default constructor */
	public OtherCall() {
	}

	/** full constructor */
	public OtherCall(Callsheet callsheet, OtherCallType type, Integer lineNumber,
			String description, Integer count, Date time) {
		this.callsheet = callsheet;
		this.type = type;
		this.lineNumber = lineNumber;
		this.description = description;
		this.count = count;
		this.time = time;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public OtherCallType getType() {
		return this.type;
	}

	public void setType(OtherCallType type) {
		this.type = type;
	}

	@Column(name = "Line_Number")
	public Integer getLineNumber() {
		return this.lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "Count", nullable = false)
	public Integer getCount() {
		return this.count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time", length = 0)
	public Date getTime() {
		return this.time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + id;
		s += ", line=" + lineNumber;
		s += ", line=" + count;
		s += ", type=" + type;
		s += ", description=" + description;
		s += ", time=" + time;
		s += "]";
		return s;
	}

}