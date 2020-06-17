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

import com.lightspeedeps.type.AuditEventType;
import com.lightspeedeps.type.ObjectType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;

/**
 * AuditEvent entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent extends PersistentObject<AuditEvent> {

	/** */
	private static final long serialVersionUID = 1L;
	// Fields

	public static final int MAX_SUMMARY_LENGTH = 2000;
	public static final int MAX_DETAIL_LENGTH = 10000;

	private Date date;
	private AuditEventType type;
	private Production production;
	private AuditEvent parent;
	private Short depth;
	private String userAccount;
	private ObjectType relatedObjectType;
	private Integer relatedObjectId;
	private String summary;
	private String detail;

	// Constructors

	/** default constructor */
	public AuditEvent() {
	}

	/** minimal constructor */
	public AuditEvent(Date time, AuditEventType typ, Production prod, AuditEvent par,
			Short deep, String userAcct, ObjectType obj, Integer objId, String sum, String dtl) {
		date = time;
		type = typ;
		production = prod;
		parent = par;
		depth = deep;
		userAccount = userAcct;
		relatedObjectType = obj;
		relatedObjectId = objId;
		summary = sum;
		detail = dtl;
	}

	// Property accessors

	/** See {@link #production} */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	/** See {@link #production} */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #parent} */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Parent_Id")
	public AuditEvent getParent() {
		return parent;
	}
	/** See {@link #parent} */
	public void setParent(AuditEvent auditEvent) {
		parent = auditEvent;
	}

	/** See {@link #date} */
	@Column(name = "Date", nullable = false, length = 19)
	public Date getDate() {
		return date;
	}
	/** See {@link #date} */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #type} */
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "Type", nullable = false)
	public AuditEventType getType() {
		return type;
	}
	/** See {@link #type} */
	public void setType(AuditEventType type) {
		this.type = type;
	}

	/** See {@link #depth} */
	@Column(name = "Depth")
	public Short getDepth() {
		return depth;
	}
	/** See {@link #depth} */
	public void setDepth(Short depth) {
		this.depth = depth;
	}

	/** See {@link #userAccount} */
	@Column(name = "User_Account", length = 20)
	public String getUserAccount() {
		return userAccount;
	}
	/** See {@link #userAccount} */
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	@Enumerated(EnumType.ORDINAL)
	@Column(name = "Related_Object_Type")
	public ObjectType getRelatedObjectType() {
		return relatedObjectType;
	}
	/** See {@link #relatedObjectType} */
	public void setRelatedObjectType(ObjectType relatedObjectType) {
		this.relatedObjectType = relatedObjectType;
	}

	/** See {@link #relatedObjectId} */
	@Column(name = "Related_Object_Id")
	public Integer getRelatedObjectId() {
		return relatedObjectId;
	}
	/** See {@link #relatedObjectId} */
	public void setRelatedObjectId(Integer relatedObjectId) {
		this.relatedObjectId = relatedObjectId;
	}

	/** See {@link #summary} */
	@Column(name = "Summary", length = 2000)
	public String getSummary() {
		return summary;
	}
	/** See {@link #summary} */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/** See {@link #detail} */
	@Column(name = "Detail", length = 10000)
	public String getDetail() {
		return detail;
	}
	/** See {@link #detail} */
	public void setDetail(String detail) {
		this.detail = detail;
	}

	/**
	 * Append a message to the "Summary" portion of this event.
	 *
	 * @param msgId A message id to be looked up in our message resource file.
	 */
	public void appendMsg(String msgId) {
		appendSummary(MsgUtils.getMessage( msgId ));
	}

	/**
	 * Append a message to the Summary portion of this event, after
	 * formatting/substituting with the supplied arguments.
	 * 
	 * @param msgId A message id to be looked up in our message resource file.
	 * @param args One or more arguments for substitution into the message text.
	 */
	public void appendMsg(String msgId, Object... args) {
		appendSummary(MsgUtils.formatMessage(msgId, args));
	}

	/**
	 * Append the given text to the "Summary" portion of this event.
	 *
	 * @param text Text to be added to the end of the summary.
	 */
	public void appendSummary(String text) {
		if (getSummary() == null) {
			summary = text;
		}
		else {
			summary = getSummary() + "; " + text;
		}
	}

	/**
	 * Append the given text to the "Summary" portion of this event, starting on
	 * a new line.
	 *
	 * @param text Text to be added to the end of the summary.
	 */
	public void appendSummaryLine(String text) {
		if (getSummary() == null) {
			summary = text;
		}
		else {
			summary = getSummary() + Constants.NEW_LINE + text;
		}
	}

	/**
	 * Append the given text to the "Detail" portion of this event, starting on
	 * a new line.
	 *
	 * @param text Text to be added to the end of the detail.
	 */
	public void appendDetail(String text) {
		if (getDetail() == null) {
			detail = text;
		}
		else {
			detail = getDetail() + Constants.NEW_LINE + text;
		}
	}

}
