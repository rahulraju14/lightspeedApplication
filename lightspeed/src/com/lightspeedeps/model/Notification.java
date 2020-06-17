package com.lightspeedeps.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.lightspeedeps.type.NotificationType;
import com.lightspeedeps.type.ReportType;

/**
 * Notification entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "notification")
public class Notification extends PersistentObject<Notification> {

	// Fields
	private static final long serialVersionUID = 1L;

	/** Type of Notification. */
	private NotificationType type;
	/** Unused. (Originally planned to be the Unit number associated with this
	 * Notification, for Callsheet-related notifications. */
	private Integer unitNumber = 1;
//	private Trigger triggers;
	/** When this notification was created. */
	private Date createTime;
	/** The Project this notification relates to, if any. */
	private Project project;
	/** A date associated with the notification, typically a report date. */
	private Date date;
	/** The type of report, if this Notification is report-related. */
	private ReportType reportType;
	/** The Set of Message`s generated as part of the Notification. */
	private Set<Message> messages = new HashSet<>();

	// Constructors

	/** default constructor */
	public Notification() {
	}

	/** typical constructor */
	public Notification(NotificationType type, Date createTime, Project project) {
		this.createTime = createTime;
		this.type = type;
		this.project = project;
	}

	/** full constructor */
	public Notification(NotificationType type, Date createTime, Project project, Date rptDate, ReportType rpt) {
		this.createTime = createTime;
		this.type = type;
		this.project = project;
		date = rptDate;
		reportType = rpt;
	}

	// Property accessors

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 100)
	public NotificationType getType() {
		return this.type;
	}
	public void setType(NotificationType type) {
		this.type = type;
	}

	@Column(name = "Unit_Number", nullable = false)
	/** See {@link #unitNumber}. */
	public Integer getUnitNumber() {
		return this.unitNumber;
	}
	/** See {@link #unitNumber}. */
	public void setUnitNumber(Integer number) {
		this.unitNumber = number;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "notification")
	public Set<Message> getMessages() {
		return this.messages;
	}
	public void setMessages(Set<Message> messages) {
		this.messages = messages;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Trigger_Id")
//	public Trigger getTriggers() {
//		return this.triggers;
//	}
//	public void setTriggers(Trigger triggers) {
//		this.triggers = triggers;
//	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CreateTime", nullable = false, length = 0)
	public Date getCreateTime() {
		return this.createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return this.project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Date", length = 0)
	public Date getDate() {
		return this.date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Report_Type", length = 30)
	public ReportType getReportType() {
		return this.reportType;
	}
	public void setReportType(ReportType reportType) {
		this.reportType = reportType;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
		result = prime * result + ((getReportType() == null) ? 0 : getReportType().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
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
		Notification other = null;
		try {
			other = (Notification)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		}
		else if (! id.equals(other.getId()))
			return false;
		if (getCreateTime() == null) {
			if (other.getCreateTime() != null)
				return false;
		}
		else if (! getCreateTime().equals(other.getCreateTime()))
			return false;
		if (getReportType() != other.getReportType())
			return false;
		if (getType() != other.getType())
			return false;
		return true;
	}

}
