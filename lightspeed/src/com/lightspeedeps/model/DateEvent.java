package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

import com.lightspeedeps.type.WorkdayType;

/**
 * DateEvent entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "date_event")
public class DateEvent extends PersistentObject<DateEvent> implements Cloneable {

	// Fields
	private static final long serialVersionUID = 3566687725826721732L;

	private ProjectSchedule projectSchedule;
	private Contact contact;
	private Date start;
	private Date end;
	private WorkdayType type;
	private String description;

	// Constructors

	/** default constructor */
	public DateEvent() {
	}

	/** popular constructor */
	public DateEvent(ProjectSchedule projectSchedule,
			Date startAndEnd, WorkdayType type) {
		this.projectSchedule = projectSchedule;
		this.start = startAndEnd;
		this.end = startAndEnd;
		this.type = type;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Schedule_Id")
	public ProjectSchedule getProjectSchedule() {
		return this.projectSchedule;
	}

	public void setProjectSchedule(ProjectSchedule projectSchedule) {
		this.projectSchedule = projectSchedule;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getContact() {
		return this.contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Start", nullable = false, length = 0)
	public Date getStart() {
		return this.start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "End", length = 0)
	public Date getEnd() {
		return this.end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public WorkdayType getType() {
		return this.type;
	}
	public void setType(WorkdayType type) {
		this.type = type;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}