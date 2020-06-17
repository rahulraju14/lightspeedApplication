package com.lightspeedeps.model;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.*;

import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Event entity. Stores information about significant system events, such as
 * logins and logoffs, exceptions caught, etc.
 */
@Entity
@Table(name = "event")
public class Event extends PersistentObject<Event> implements Comparable<Event> {

	private static final long serialVersionUID = 1L;

	/** Timestamp style for toString formatting */
	private final Format eventDateFormat = new SimpleDateFormat("MM/dd HH:mm:ss");

	// Fields

	private Project project;
	private Production production;
	private EventType type;
	private Date startTime;
	private String username;
	private String description;

	@Transient
	private String shortDescription;
	// Constructors

	/** default constructor */
	public Event() {
	}

	/** minimal constructor */
	public Event(EventType type) {
		this.type = type;
	}

	/** full constructor */
	public Event(Project project, Production production, EventType type,
			Date startTime, String username, String description) {
		this.project = project;
		this.production = production;
		this.type = type;
		this.startTime = startTime;
		this.username = username;
		this.description = description;
	}

	/** Short constructor for events not associated with a production. */
	public Event(EventType type, Date startTime, String username, String description) {
		this(null, null, type, startTime, username, description);
	}

	// Property accessors}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false)
	public EventType getType() {
		return type;
	}
	public void setType(EventType type) {
		this.type = type;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Start_Time", length = 0)
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Column(name = "Username", length = 100)
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public static final int MAX_DESC_LENGTH = 20000;

	@Column(name = "Description", length = 20000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Transient
	public String getDescriptionHtml() {
		String text = description;
		if (text != null) {
			text = text.replaceAll("\\n", Constants.HTML_BREAK);
		}
		return text;
	}

	/** See {@link #shortDescription}. */
	@Transient
	public String getShortDescription() {
		if (shortDescription == null) {
			if (description == null) {
				shortDescription = "";
			}
			else {
				if (description.length() < 200) {
					shortDescription = description;
				}
				else {
					shortDescription = description.substring(0, 200);
				}
				shortDescription = shortDescription.replaceAll("\\n", Constants.HTML_BREAK);
			}
		}
		return shortDescription;
	}

	@Override
	public int compareTo(Event event) {
		int rc = 1;
		rc = getStartTime().compareTo(event.getStartTime());
		if (rc == 0) {
			rc = getType().compareTo(event.getType());
			if (rc == 0) {
				rc = StringUtils.compare(getUsername(), event.getUsername());
			}
		}
		return rc;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = ""; // Omit default Object toString();
		String text = "none";
		if (getDescription() != null) {
			text = getDescription();
			int i = text.indexOf(Constants.NEW_LINE);
			if (i > 0) {
				text = text.substring(0, i);
			}
		}
		s += "[";
		s += "" + (getStartTime()==null ? "?" : eventDateFormat.format(getStartTime()));
		s += " " + (getType()==null ? "?" : getType());
		s += ", Desc: " + text;
		s += "]";
		return s;
	}

}