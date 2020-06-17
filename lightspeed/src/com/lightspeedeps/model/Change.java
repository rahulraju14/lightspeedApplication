package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;

/**
 * "Change" entity. The data in this table is meant to track significant changes
 * to a production's data, e.g., new projects, stripboards, callsheets, etc.
 * <p>
 * Note that the table name is "changeS" -- "change" is an SQL keyword and is
 * not allowed as a table name.
 */
@Entity
@Table(name = "changes")
public class Change extends PersistentObject<Change> implements Cloneable {

	private static final long serialVersionUID = - 2330299247323318277L;

	// Fields

	/** Generally describes object affected or area of system affected, e.g.,
	 * Project, Script, Payroll. */
	private ChangeType type;

	/** Action taken, e.g., create or delete. */
	private ActionType action;

	/** The Production affected. */
	private Production production;

	/** The Project affected. */
	private Project project;

	/** Allows direct access to the Project.id field.
	 * @see #project */
	private Integer projectId;

	/** The Unit number associated with this Change, if any. */
	private Integer unitNumber;

	/** The Stripboard affected, if any. */
	private Stripboard stripboard;

	/** The name of the User responsible for this action, generally
	 * the currently logged-in user. */
	private String userName;

	/** Timestamp when this change event began. */
	private Date startTime;

	/**  Timestamp when this change event ended. This is
	 * typically the same as the startTime. */
	private Date endTime;

	/** Other descriptive information; contents are generally specific
	 * to the event being recorded. */
	private String description;

	// Constructors

	/** default constructor */
	public Change() {
	}

	/**
	 * Constructor for Change events outside of any Production.
	 *
	 * @param type Type of change.
	 * @param action Action taken by user or system.
	 * @param user Either the User performing the action, or, in some cases, the
	 *            affected User.
	 * @param sTime Timestamp for the change event.
	 * @param description Free-form description, often used to include
	 *            additional information not covered in the above parameters.
	 */
	public Change(ChangeType type, ActionType action,  String user,
			Date sTime, String description) {
		this(type, action, null, null, user, sTime, description);
	}

	/**
	 * Standard constructor for Change events.
	 *
	 * @param type Type of change.
	 * @param action Action taken by user or system.
	 * @param production The relevant Production, may be null.
	 * @param project The relevant Project, may be null.
	 * @param user Either the User performing the action, or, in some cases, the
	 *            affected User.
	 * @param sTime Timestamp for the change event.
	 * @param description Free-form description, often used to include
	 *            additional information not covered in the above parameters.
	 */
	public Change(ChangeType type, ActionType action, Production production, Project project, String user,
			Date sTime, String description) {
		this.type = type;
		this.action = action;
		this.production = production;
		this.project = project;
		this.userName = user;
		this.startTime = sTime;
		this.endTime = sTime;
		this.description = description;
	}

	/** full constructor */
/*	public Change(Stripboard stripboard, Project project,
			Production production, String user, Date startTime, Date endTime,
			String type, String description) {
		this.stripboard = stripboard;
		this.project = project;
		this.production = production;
		this.userName = user;
		this.startTime = startTime;
		this.endTime = endTime;
		this.type = type;
		this.description = description;
	}
*/
	// Property accessors

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ChangeType getType() {
		return this.type;
	}
	public void setType(ChangeType type) {
		this.type = type;
	}

	/** See {@link #action}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Action", nullable = false, length = 30)
	public ActionType getAction() {
		return action;
	}
	/** See {@link #action}. */
	public void setAction(ActionType action) {
		this.action = action;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return this.production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return this.project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/** Allows direct access to the project_id field.  In cases where
	 * the Project no longer exists, referencing the 'project' field
	 * gets an error, but with this method we can retrieve the id itself.
	 *  See {@link #project}. */
	@Column(name = "Project_Id", insertable=false, updatable=false)
	public Integer getProjectId() {
		return projectId;
	}
	public void setProjectId(Integer id) {
		projectId = id;
	}

	@Column(name = "Unit_Number")
	/** See {@link #unitNumber}. */
	public Integer getUnitNumber() {
		return this.unitNumber;
	}
	/** See {@link #unitNumber}. */
	public void setUnitNumber(Integer number) {
		this.unitNumber = number;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Stripboard_Id")
	public Stripboard getStripboard() {
		return this.stripboard;
	}
	public void setStripboard(Stripboard stripboard) {
		this.stripboard = stripboard;
	}

	@Column(name = "User_Name")
	public String getUserName() {
		return this.userName;
	}
	public void setUserName(String user) {
		this.userName = user;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Start_Time", nullable = false, length = 0)
	public Date getStartTime() {
		return this.startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "End_Time", nullable = false, length = 0)
	public Date getEndTime() {
		return this.endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Object clone() {
		try {
			Change copy = (Change)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

}