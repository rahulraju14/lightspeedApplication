package com.lightspeedeps.model;

import java.util.*;

import javax.persistence.*;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.object.ApproverChainRoot;
import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Project entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "project", uniqueConstraints = @UniqueConstraint(columnNames = "Title"))
public class Project extends PersistentObject<Project>
		implements Comparable<Project>, Cloneable, ApproverChainRoot {

	//private static final Log LOG = LogFactory.getLog(Project.class);

	private static final long serialVersionUID = -1704724363746209628L;

	public static final String SORTKEY_ID = "id";
	public static final String SORTKEY_CODE = "code";
	public static final String SORTKEY_START = "startdate";
	public static final String SORTKEY_STATUS = "status";

	// Fields

	/** A sequential number, within a Production, beginning with 1, assigned to
	 * each Project as it is created. */
	private Integer sequence;

	/** The name of the Project.  This is never null, and must be unique
	 * within a Production. For Commercial productions, this may be referred
	 *  to as the "job name". */
	private String title = "";

	/** An optional short String, edited by the user, to identify this Project/Episode.
	 * For Commercial productions, this is called the "job number". */
	private String episode;

	/** An identifying name (string) that may be used by the payroll service to identify
	 * this project. */
	private String payrollProjectId;

	/** An optional short String, edited by the user, to identify which season
	 *  this Project belongs to. */
	private String seasonNumber;

	/** The Production to which this Project belongs.  This is never null. */
	private Production production;

	/** The Project's current status. */
	private AccessStatus status = AccessStatus.ACTIVE;

	/** Timestamp of when this project was last updated. */
	private Date updated;

	/** Timestamp of when this project data was last transmitted to a payroll service. */
	private Date timeSent;

	/** Timestamp of when this project data was updated in an external service. */
	private Date interfaced;

	/** For projects within a Commercial production, this holds the payroll
	 * preferences. */
	private PayrollPreference payrollPref;

	/** First approver (in the chain of Approvers) for this project. This
	 * only applies to Commercial productions. */
	private Approver approver;

	/** Mask that tracks which Department`s should be displayed in selection lists. */
	private BitMask deptMask = new BitMask(Constants.ALL_DEPARTMENTS_MASK);

	/** True if notifications should be generated for appropriate events. */
	private Boolean notifying = true;

	/** Project skipped for some external process(es). Set/used externally to LS. */
	private Boolean skip = false;

	/** The timestamp of when the 'notifying' flag was last changed. */
	private Date notificationChanged;

	/** The current (active) Stripboard for this Project. This is only null
	 * if the Project has no stripboards at all. */
	private Stripboard stripboard;

	/** The current (active) Script for this Project. This is only null
	 * if the Project has no scripts at all.*/
	private Script script;

	/** True if users are allowed to view the text of Script`s within
	 * this Project. */
	private Boolean scriptTextAccessible = true;

	/** True if users who are members of this Project can view the File
	 * Repository. */
	private Boolean fileRepositoryEnabled = false;

	/** True if all users who are members of this Project automatically have
	 * read access to all non-private files in the repository. */
	private Boolean allUsersReadFiles = true;

	/** */
	private Byte autoRefreshCallSheet = 0;

	/** The project's end date as originally envisioned; this is
	 * a user-maintained field.  The only rule enforced by the software is
	 * that it must be later than the scheduled start date. */
	private Date originalEndDate;

	/** The time zone used to display timestamp values within this Project.
	 * Note that all timestamps in the database are in UTC (same as GMT). */
	private String timeZoneStr;

	/** The transient TimeZone object corresponding to timeZoneStr. */
	@Transient
	private TimeZone timeZone;

	/** A field that might be used for determining which Project an incoming
	 * SMS message is related to.  Not currently used. */
	private String smsKeyword;

	/** True if production elements are subject to hold/drop rules. */
	private Boolean useHoldDrop = true;

	/** The default number of days a production element (typically a cast
	 * character) will be held (paid without working) before being dropped.  This
	 * value may be overridden at the ScriptElement level.  */
	private Integer daysHeldBeforeDrop = new Integer(Constants.DEFAULT_HELD_BEFORE_DROP);

	/** The database id of the User who is currently editing this Stripboard,
	 * and therefore holds a "lock" on it until they leave the Editor page
	 * or their session times out. */
	private Integer stripsLockedBy;

	/** Details that are specific to Canada Forms for pre-populating forms. */
	private CanadaProjectDetail canadaProjectDetail;

	/** Detail information for project related to UDA-INM contract (Canada) */
	private UdaProjectDetail udaProjectDetail;

	/** The Set of Script`s belonging to this Project. */
	private Set<Script> scripts = new HashSet<>(0);

	/** The Set of Callsheet`s belonging to this Project. */
	private Set<Callsheet> callsheets = new HashSet<>(0);

	/** The Set of Dpr`s (production reports) belonging to this Project. */
	private Set<Dpr> dprs = new HashSet<>(0);

	/** The Set of ReportRequirement`s associated with this Project. */
	private List<ReportRequirement> reportRequirements = new ArrayList<>(0);

	/** The Set of User`s whose default Project is this one. */
	private Set<Contact> contacts = new HashSet<>(0);

	/** The Set of Stripboard`s belonging to this Project. */
	private Set<Stripboard> stripboards = new HashSet<>(0);

	/** A List of all the Unit's in the Project, ordered by unitNumber. */
	private List<Unit> units = new ArrayList<>(0);

	// Unused sets implied by foreign key relationships
//	private Set<ExhibitG> exhibitGs = new HashSet<ExhibitG>(0);
//	private Set<Change> changes = new HashSet<Change>(0);
//	private Set<TimeSheet> timeSheets = new HashSet<TimeSheet>(0);
//	private Set<ScriptElement> scriptElements = new HashSet<ScriptElement>(0);
//	private Set<ProjectMember> projectMembers = new HashSet<ProjectMember>(0);
//	private Set<Event> events = new HashSet<Event>(0);

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** True if this Project has more than one unit.  Facilitates JSP
	 * conditional formatting. */
	@Transient
	private Boolean hasUnits;

	@Transient
	private boolean checked;

	@Transient
	private boolean allowEnable;

	// Constructors

	/** default constructor */
	public Project() {
	}

	/** See {@link #sequence}. */
	@Column(name = "Sequence", nullable = false)
	public Integer getSequence() {
		return sequence;
	}
	/** See {@link #sequence}. */
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	/** See {@link #episode}. */
	@Column(name = "Code", length = 20)
	public String getEpisode() {
		return episode;
	}
	/** See {@link #episode}. */
	public void setEpisode(String code) {
		episode = code;
	}

	/** See {@link #payrollProjectId}. */
	@Column(name = "Payroll_Project_Id", length = 50)
	public String getPayrollProjectId() {
		return payrollProjectId;
	}
	/** See {@link #payrollProjectId}. */
	public void setPayrollProjectId(String payrollProjectId) {
		this.payrollProjectId = payrollProjectId;
	}

	/**See {@link #seasonNumber}. */
	@Column(name = "Season_Number", length = 10)
	public String getSeasonNumber() {
		return seasonNumber;
	}
	/**See {@link #seasonNumber}. */
	public void setSeasonNumber(String seasonNumber) {
		this.seasonNumber = seasonNumber;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Current_Stripboard_Id")
	public Stripboard getStripboard() {
		return stripboard;
	}
	public void setStripboard(Stripboard stripboard) {
		this.stripboard = stripboard;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Current_Script_Id")
	public Script getScript() {
		return script;
	}
	public void setScript(Script script) {
		this.script = script;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	@Column(name = "Title", unique = true, nullable = false, length = 35)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	/** See {@link #updated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return updated;
	}
	/** See {@link #updated}. */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/** See {@link #timeSent}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Sent", length = 0)
	public Date getTimeSent() {
		return timeSent;
	}
	/** See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	/** See {@link #interfaced}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Interfaced", length = 0)
	public Date getInterfaced() {
		return interfaced;
	}
	/** See {@link #interfaced}. */
	public void setInterfaced(Date interfaced) {
		this.interfaced = interfaced;
	}

	/** See {@link #payrollPref}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Payroll_Preference_Id")
	public PayrollPreference getPayrollPref() {
		return payrollPref;
	}
	/** See {@link #payrollPref}. */
	public void setPayrollPref(PayrollPreference payrollPref) {
		this.payrollPref = payrollPref;
	}

	/** See {@link #approver}. */
	@Override
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Approver_Id")
	public Approver getApprover() {
		return approver;
	}
	/** See {@link #approver}. */
	@Override
	public void setApprover(Approver approver) {
		this.approver = approver;
	}

	@Transient
	public BitMask getDeptMask() {
		getDeptMaskDb(); // ensure Hibernate has retrieved value
		return deptMask;
	}
	/** See {@link #deptMask}. */
	public void setDeptMask(BitMask deptMask) {
		this.deptMask = deptMask;
	}

	/** See {@link #deptMask}. */
	@Column(name = "Dept_Mask")
	public long getDeptMaskDb() {
		return deptMask.getLong();
	}
	/** See {@link #deptMask}. */
	public void setDeptMaskDb(long mask) {
		deptMask.set(mask);
	}

	/** See {@link #status}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public AccessStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(AccessStatus status) {
		this.status = status;
	}

	/** See {@link #notifying}. */
	@Column(name = "Notifying", nullable = false)
	public Boolean getNotifying() {
		return notifying;
	}
	public void setNotifying(Boolean notifying) {
		this.notifying = notifying;
	}

	/** See {@link #skip}. */
	@Column(name = "Skip", nullable = false)
	public Boolean getSkip() {
		return skip;
	}
	/** See {@link #skip}. */
	public void setSkip(Boolean skip) {
		this.skip = skip;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Notification_Changed", length = 0)
	public Date getNotificationChanged() {
		return notificationChanged;
	}
	public void setNotificationChanged(Date notificationChanged) {
		this.notificationChanged = notificationChanged;
	}

	@Column(name = "Script_Text_Accessible", nullable = false)
	public Boolean getScriptTextAccessible() {
		return scriptTextAccessible;
	}
	public void setScriptTextAccessible(Boolean scriptTextAccessible) {
		this.scriptTextAccessible = scriptTextAccessible;
	}

	@Column(name = "File_Repository_Enabled", nullable = false)
	public Boolean getFileRepositoryEnabled() {
		return fileRepositoryEnabled;
	}
	public void setFileRepositoryEnabled(Boolean fileRepositoryEnabled) {
		this.fileRepositoryEnabled = fileRepositoryEnabled;
	}

	@Column(name = "All_Users_Read_Files", nullable = false)
	public Boolean getAllUsersReadFiles() {
		return allUsersReadFiles;
	}
	public void setAllUsersReadFiles(Boolean allUsersReadFiles) {
		this.allUsersReadFiles = allUsersReadFiles;
	}

	@Column(name = "Auto_Refresh_Call_Sheet", nullable = false)
	public Byte getAutoRefreshCallSheet() {
		return autoRefreshCallSheet;
	}
	public void setAutoRefreshCallSheet(Byte autoRefreshCallSheet) {
		this.autoRefreshCallSheet = autoRefreshCallSheet;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Original_End_Date", length = 0)
	public Date getOriginalEndDate() {
		return originalEndDate;
	}
	public void setOriginalEndDate(Date originalEndDate) {
		this.originalEndDate = originalEndDate;
	}

	@Column(name = "Time_Zone", nullable = false, length = 50)
	public String getTimeZoneStr() {
		return timeZoneStr;
	}
	public void setTimeZoneStr(String tz) {
		if (tz == null || ! tz.equals(timeZoneStr)) {
			timeZoneStr = tz;
			timeZone = null;
		}
	}

	@Transient
	public TimeZone getTimeZone() {
		if (timeZone == null) {
			timeZone = TimeZone.getTimeZone(getTimeZoneStr());
		}
		return timeZone;
	}
	public void setTimeZone(TimeZone tz) {
		timeZone = tz;
		timeZoneStr = tz.getID();
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
//	public Set<ExhibitG> getExhibitGs() {
//		return this.exhibitGs;
//	}
//	public void setExhibitGs(Set<ExhibitG> exhibitGs) {
//		this.exhibitGs = exhibitGs;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	public Set<Script> getScripts() {
		return scripts;
	}
	public void setScripts(Set<Script> scripts) {
		this.scripts = scripts;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
//	public Set<Change> getChanges() {
//		return this.changes;
//	}
//	public void setChanges(Set<Change> changeses) {
//		this.changes = changeses;
//	}

	@ManyToMany(
			targetEntity=Callsheet.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}
		)
	@JoinTable( name = "project_callsheet",
			joinColumns = {@JoinColumn(name = "Project_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Callsheet_Id")}
			)
	public Set<Callsheet> getCallsheets() {
		return callsheets;
	}
	public void setCallsheets(Set<Callsheet> callsheets) {
		this.callsheets = callsheets;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
//	public Set<TimeSheet> getTimeSheets() {
//		return this.timeSheets;
//	}
//	public void setTimeSheets(Set<TimeSheet> timeSheets) {
//		this.timeSheets = timeSheets;
//	}

/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	public Set<ScriptElement> getScriptElements() {
		return this.scriptElements;
	}
	public void setScriptElements(Set<ScriptElement> scriptElements) {
		this.scriptElements = scriptElements;
	}
*/
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	public Set<Dpr> getDprs() {
		return dprs;
	}
	public void setDprs(Set<Dpr> dprs) {
		this.dprs = dprs;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	@OrderBy("id")
	public List<ReportRequirement> getReportRequirements() {
		return reportRequirements;
	}
	public void setReportRequirements(List<ReportRequirement> reportRequirements) {
		this.reportRequirements = reportRequirements;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
	public Set<Contact> getContacts() {
		return contacts;
	}
	public void setContacts(Set<Contact> users) {
		contacts = users;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	public Set<Stripboard> getStripboards() {
		return stripboards;
	}
	public void setStripboards(Set<Stripboard> stripboards) {
		this.stripboards = stripboards;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
//	public Set<ProjectMember> getProjectMembers() {
//		return this.projectMembers;
//	}
//	public void setProjectMembers(Set<ProjectMember> projectMembers) {
//		this.projectMembers = projectMembers;
//	}

	/** See {@link #units}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
	@OrderBy("number")
	public List<Unit> getUnits() {
		return units;
	}
	/** See {@link #units}. */
	public void setUnits(List<Unit> units) {
		this.units = units;
	}

	/** Returns the first (main) Unit of the Project. */
	@Transient
	public Unit getMainUnit() {
		if (getUnits().size() == 0) {
			return ProjectDAO.fixMissingUnit(this); // remove UnitDAO reference from model. LS-2737
		}
		return getUnits().get(0);
	}

	/** Returns the first (main) Unit of the Project.
	 * Used in locations where we think we'll need to change the code -- so use a Deprecated
	 * function to track this! */
//	@Transient
//	@Deprecated
//	public Unit getUnit() {
//		return getMainUnit();
//	}

	/** See {@link #hasUnits}. */
	@Transient
	public Boolean getHasUnits() {
		if (hasUnits == null) {
			hasUnits = (getUnits().size() > 1);
		}
		return hasUnits;
	}

	/** Returns the number of Units in this Project. */
	@Transient
	public int getUnitCount() {
		if (getUnits() != null) {
			return getUnits().size();
		}
		return 0;
	}

	/**
	 * Find the Unit within this Project with the specified unit number.
	 * @param unitNumber The number of the Unit to be found.
	 * @return Either the Unit with the matching number, or null if not found.
	 */
	@Transient
	public Unit getUnit(int unitNumber) {
		for (Unit u : getUnits()) {
			if (u.getNumber().intValue() == unitNumber) {
				return u;
			}
		}
		return null;
	}

	@Transient
	/** Returns the unitNumber of the last (highest numbered) Unit
	 * in this Project. */
	public int getHighestUnitNumber() {
		if (getUnits().size() == 0) { // shouldn't happen (bad data)
			ProjectDAO.fixMissingUnit(this);
		}
		return getUnits().get(getUnits().size()-1).getNumber();
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
//	public Set<Event> getEvents() {
//		return this.events;
//	}
//	public void setEvents(Set<Event> events) {
//		this.events = events;
//	}

	@Column(name = "Sms_keyword", length = 25)
	public String getSmsKeyword() {
		return smsKeyword;
	}
	public void setSmsKeyword(String smsKeyword) {
		this.smsKeyword = smsKeyword;
	}

	/** See {@link #useHoldDrop}. */
	@Column(name = "Use_Hold_Drop", nullable = false)
	public Boolean getUseHoldDrop() {
		return useHoldDrop;
	}
	/** See {@link #useHoldDrop}. */
	public void setUseHoldDrop(Boolean b) {
		useHoldDrop = b;
	}

	/** See {@link #daysHeldBeforeDrop}. */
	@Column(name = "Days_Held_Before_Drop")
	public Integer getDaysHeldBeforeDrop() {
		return daysHeldBeforeDrop;
	}
	/** See {@link #daysHeldBeforeDrop}. */
	public void setDaysHeldBeforeDrop(Integer daysHeldBeforeDrop) {
		this.daysHeldBeforeDrop = daysHeldBeforeDrop;
	}

	/** See {@link #stripsLockedBy}. */
	@Column(name = "Strips_Locked_By")
	public Integer getStripsLockedBy() {
		return stripsLockedBy;
	}
	/** See {@link #stripsLockedBy}. */
	public void setStripsLockedBy(Integer lockedBy) {
		stripsLockedBy = lockedBy;
	}

	/** See {@link #canadaProjectDetail}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Canada_Project_Details_Id")
	public CanadaProjectDetail getCanadaProjectDetail() {
		return canadaProjectDetail;
	}

	/** See {@link #canadaProjectDetail}. */
	public void setCanadaProjectDetail(CanadaProjectDetail canadaProjectDetail) {
		this.canadaProjectDetail = canadaProjectDetail;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #checked}. */
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/** See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/** See {@link #allowEnable}. */
	@Transient
	public boolean getAllowEnable() {
		return allowEnable;
	}
	/** See {@link #allowEnable}. */
	public void setAllowEnable(boolean allowEnable) {
		this.allowEnable = allowEnable;
	}

	/** See {@link #udaProjectDetail}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Uda_Project_Details_Id")
	public UdaProjectDetail getUdaProjectDetail() {
		return udaProjectDetail;
	}
	/** See {@link #udaProjectDetail}. */
	public void setUdaProjectDetail(UdaProjectDetail udaProjectDetail) {
		this.udaProjectDetail = udaProjectDetail;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
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
		Project other;
		try {
			other = (Project)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(other.getId()) ) {
			return true;
		}
		return (compareTo(other) == 0);
	}

	/**
	 * The default comparison of Projects, which compares their titles.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Project other) {
		int ret = 1;
		if (other != null) {
			ret = getTitle().compareToIgnoreCase(other.getTitle());
		}
		return ret;
	}

	public int compareTo(Project other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret));
	}

	public int compareTo(Project other, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by title (later)
		}
		else if (sortField.equals(SORTKEY_ID) ) {
			ret = getSequence().compareTo(other.getSequence());
		}
		else if (sortField.equals(SORTKEY_CODE) ) {
			ret = StringUtils.compareNumeric(getEpisode(),other.getEpisode());
		}
		else if (sortField.equals(SORTKEY_START) ) {
			ProjectSchedule sched = getMainUnit().getProjectSchedule();
			if (sched != null) {
				if (other.getMainUnit().getProjectSchedule() != null) {
					ret = sched.getStartDate().compareTo(other.getMainUnit().getProjectSchedule().getStartDate());
				}
				else {
					ret = 1;
				}
			}
			else if (other.getMainUnit().getProjectSchedule() != null) {
				ret = -1;
			}
		}
		else if (sortField.equals(SORTKEY_STATUS) ) {
			ret = getStatus().compareTo(other.getStatus());
			if (ret == 0 && getId() != null && other.getId() != null) {
				// make "default project" sort to top
				Integer defId = SessionUtils.getProduction().getDefaultProject().getId();
				if (id.equals(defId)) {
					ret = -1;
				}
				else if (other.getId().equals(defId)) {
					ret = 1;
				}
			}
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			ret = compareTo(other);
		}
		return ret;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "title=" + (getTitle()==null ? "null" : getTitle());
//		s += ", prod=" + (getProduction()==null ? "null" : getProduction()); // gets lazyInitExceptions
		s += ", notificationChanged=" + (getNotificationChanged()==null ? "null" : getNotificationChanged());
//		s += ", script=" + (getScript()==null ? "null" : getScript()); // gets lazyInitExceptions
		s += ", notifying?=" + getNotifying();
		s += ", script text?=" + getScriptTextAccessible();
//		s += ", file repos?=" + this.getFileRepositoryEnabled();
//		s += ", # of units=" + getUnitCount(); // can cause LazyInitExceptions
		s += ']';
		return s;
	}

	/**
	 * Generate export data for the Project record.
	 *
	 * @param ex The Exporter to use to output the data.
	 */
	public void exportFlat(Exporter ex) {
		Production prod = SessionUtils.getNonSystemProduction();
		PayrollPreference prodPref = prod.getPayrollPref();
		PayrollPreference projectPref = this.getPayrollPref();
		if (projectPref == null) {
			projectPref = prodPref;
		}

		ex.append(PayrollFormType.PROJECT.getExportType());
		ex.append(1);
		ex.append(getId());
		ex.append(prod.getProdId());
		ex.append(prod.getStudio());
		ex.append(prod.getTitle());
		ex.append(prod.getType().name());
		ex.append(prodPref.getPayrollProdId());
		ex.append(prod.getAllowOnboarding());

		Address addr = prod.getAddress(); // business address, one per production
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		addr = projectPref.getMailingAddress(); // WTPA payroll preference address, per Project
		if (addr == null) {
			addr = new Address();
		}
		addr.exportFlat(ex);

		ex.append(getTitle());
		ex.append(getEpisode());
		if (getTimeSent() == null) {
			ex.append(Constants.DOC_EXPORT_STATUS_NEW);	// transfer status
			setTimeSent(new Date());
			ProjectDAO.getInstance().attachDirty(this);
		}
		else {
			ex.append(Constants.DOC_EXPORT_STATUS_UPDATED);
		}

		ex.appendDateTime(updated);
		ex.append(projectPref.getWorkCity());
		ex.append(projectPref.getWorkState());
		ex.append(projectPref.getWorkZip());
	}

//	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//		log.debug("");
//		in.defaultReadObject();
//		log.debug("done");
//	}
//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("");
//		out.defaultWriteObject();
//		log.debug("done");
//	}

}
