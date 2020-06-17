package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.util.app.Constants;

/**
 * This represents an instance of a call sheet for a Production. It is normally
 * associated with a specific Unit within a Production, and for a specific
 * date.  It may represent a single Project, or it may be "cross-project" for
 * a television series.
 */
@Entity
@Table(name = "callsheet", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Date", "Project_Id", "Unit_Number" }))
public class Callsheet extends PersistentObject<Callsheet> {
	private static final long serialVersionUID = -7846424024719015810L;

	// Fields
	/** The (main) project that this callsheet relates to. */
	private Project project;

	/** The List of projects whose Scene's appear on this call sheet.  May be
	 * more than one for "cross-project" productions, e.g., a TV series. */
	private List<Project> projects;

	/** The shooting date that this callsheet applies to. */
	private Date date;

	/** Last date/time this callsheet was saved. */
	private Date updated;

	/** user-editable title; defaults to project.title. */
	private String title;

	/** The Unit number associated with this Callsheet. */
	private Integer unitNumber = 1;

	/** Displayed list of episode numbers for this Callsheet, generated
	 * automatically when the call sheet is created. */
	private String episodeList;

	/** The TimeZone used to display dates & times on this callsheet - as a String,
	 * which is how it is stored in the database */
	private String timeZoneStr;

	/** The equivalent TimeZone object for this callsheet's timeZoneStr;
	 * this is a transient field. */
	@Transient
	private TimeZone timeZone;


	/** */
	private String executives = "";

	/** */
	private Integer shootDay;

	/** */
	private Integer shootDays;
	/** Crew-call time */
	private Date callTime;

	/** */
	private Date rehearseTime;

	/** */
	private Date shootTime;

	/** */
	private String pages;

	/** */
	private Integer advanceDays;

	/** */
	private ReportStatus status;

	/** */
	private Date sunrise;

	/** */
	private Date sunset;

	/** */
	private String weather;

	/** Holds data for free-form entry in Catering crew section. */
	private CateringLog cateringLog;

	/** */
	private List<SceneCall> sceneCalls = new ArrayList<SceneCall>(0);

	/** */
	private List<CastCall> castCalls = new ArrayList<CastCall>(0);

	/** */
	private List<DeptCall> deptCalls = new ArrayList<DeptCall>(0);

	/** */
	private Set<CallNote> callNotes = new HashSet<CallNote>(0);

	/** */
	private List<SceneCall> advanceSceneCalls = new ArrayList<SceneCall>(0);

	/** */
	private List<OtherCall> otherCalls = new ArrayList<OtherCall>(0);

	// Constructors

	/** default constructor */
	public Callsheet() {
	}

	/** full constructor */
/*	public Callsheet(Stripboard stripboard, Project project, Date date,
			Date updated, Integer shootDay, Integer shootDays, Date callTime,
			Date rehearseTime, Date shootTime, String pages,
			Integer advanceDays, ReportStatus status, Boolean published, Date sunrise,
			Date sunset, String weather, Set<Scene> scenes,
			List<SceneCall> sceneCallsForCallsheetId, List<CastCall> castCalls,
			Set<Scene> scenes_1, Set<DeptCall> deptCalls,
			Set<CallNote> callNotes, List<SceneCall> sceneCallsForAdvanceId,
			Set<OtherCall> otherCalls) {
		this.stripboard = stripboard;
		this.project = project;
		this.date = date;
		this.updated = updated;
		this.shootDay = shootDay;
		this.shootDays = shootDays;
		this.callTime = callTime;
		this.rehearseTime = rehearseTime;
		this.shootTime = shootTime;
		this.pages = pages;
		this.advanceDays = advanceDays;
		this.status = status;
		//this.published = published;
		this.sunrise = sunrise;
		this.sunset = sunset;
		this.weather = weather;
		//this.scenes = scenes;
		this.sceneCallsForCallsheetId = sceneCallsForCallsheetId;
		this.castCalls = castCalls;
		//this.scenes_1 = scenes_1;
		this.deptCalls = deptCalls;
		this.callNotes = callNotes;
		this.sceneCallsForAdvanceId = sceneCallsForAdvanceId;
		this.otherCalls = otherCalls;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	/**See {@link #projects}. */
	@ManyToMany(
			targetEntity = Project.class,
			cascade = {CascadeType.PERSIST, CascadeType.MERGE},
			fetch = FetchType.LAZY,
			mappedBy = "callsheets"
		)
	public List<Project> getProjects() {
		return projects;
	}
	/**See {@link #projects}. */
	public void setProjects(List<Project> projects) {
		this.projects = projects;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", nullable = false, length = 0)
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/**See {@link #title}. */
	@Column(name = "Title", length = 100)
	public String getTitle() {
		return title;
	}
	/**See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name = "Unit_Number", nullable = false)
	/** See {@link #unitNumber}. */
	public Integer getUnitNumber() {
		return unitNumber;
	}
	/** See {@link #unitNumber}. */
	public void setUnitNumber(Integer number) {
		unitNumber = number;
	}

	/**See {@link #episodeList}. */
	@Column(name = "Episode_List", length = 100)
	public String getEpisodeList() {
		return episodeList;
	}
	/**See {@link #episodeList}. */
	public void setEpisodeList(String episodeList) {
		this.episodeList = episodeList;
	}

	@Column(name = "Time_Zone", nullable = false, length = 50)
	public String getTimeZoneStr() {
		return timeZoneStr;
//		return getTimeZone().getDisplayName(dst, TimeZone.SHORT);
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

	/** See {@link #executives}. */
	@Column(name = "Executives", length = 500)
	public String getExecutives() {
		return executives;
	}
	/** See {@link #executives}. */
	public void setExecutives(String executives) {
		this.executives = executives;
	}

	@Column(name = "Shoot_Day")
	public Integer getShootDay() {
		return shootDay;
	}
	public void setShootDay(Integer shootDay) {
		this.shootDay = shootDay;
	}

	@Column(name = "Shoot_Days")
	public Integer getShootDays() {
		return shootDays;
	}
	public void setShootDays(Integer shootDays) {
		this.shootDays = shootDays;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Call_Time", length = 0)
	public Date getCallTime() {
		return callTime;
	}
	public void setCallTime(Date callTime) {
		this.callTime = callTime;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Rehearse_Time", length = 0)
	public Date getRehearseTime() {
		return rehearseTime;
	}
	public void setRehearseTime(Date rehearseTime) {
		this.rehearseTime = rehearseTime;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Shoot_Time", length = 0)
	public Date getShootTime() {
		return shootTime;
	}
	public void setShootTime(Date shootTime) {
		this.shootTime = shootTime;
	}

	@Column(name = "Pages", length = 10)
	public String getPages() {
		return pages;
	}
	public void setPages(String pages) {
		this.pages = pages;
	}

	@Column(name = "Advance_Days", nullable = false)
	public Integer getAdvanceDays() {
		return advanceDays;
	}
	public void setAdvanceDays(Integer advanceDays) {
		this.advanceDays = advanceDays;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public ReportStatus getStatus() {
		return status;
	}
	public void setStatus(ReportStatus status) {
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Sunrise", length = 0)
	public Date getSunrise() {
		return sunrise;
	}
	public void setSunrise(Date sunrise) {
		this.sunrise = sunrise;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Sunset", length = 0)
	public Date getSunset() {
		return sunset;
	}
	public void setSunset(Date sunset) {
		this.sunset = sunset;
	}

	@Column(name = "Weather", length = 1000)
	public String getWeather() {
		return weather;
	}
	public void setWeather(String weather) {
		this.weather = weather;
	}

	/**See {@link #cateringLog}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Catering_Log_Id")
	public CateringLog getCateringLog() {
		return cateringLog;
	}
	/**See {@link #cateringLog}. */
	public void setCateringLog(CateringLog cateringLog) {
		this.cateringLog = cateringLog;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "callsheetByCallsheetId")
	@OrderBy("lineNumber")
	public List<SceneCall> getSceneCalls() {
		return sceneCalls;
	}
	public void setSceneCalls(List<SceneCall> sceneCalls) {
		this.sceneCalls = sceneCalls;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "callsheet")
	@OrderBy("actorId")
	public List<CastCall> getCastCalls() {
		return castCalls;
	}
	public void setCastCalls(List<CastCall> castCalls) {
		this.castCalls = castCalls;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "callsheet")
	@OrderBy("priority")
	public List<DeptCall> getDeptCalls() {
		return deptCalls;
	}
	public void setDeptCalls(List<DeptCall> deptCalls) {
		this.deptCalls = deptCalls;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "callsheet")
	//@OrderBy ("lineNumber")
	public Set<CallNote> getCallNotes() {
		return callNotes;
	}
	public void setCallNotes(Set<CallNote> callNotes) {
		this.callNotes = callNotes;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "callsheetByAdvanceId")
	@OrderBy("lineNumber")
	public List<SceneCall> getAdvanceSceneCalls() {
		return advanceSceneCalls;
	}
	public void setAdvanceSceneCalls(List<SceneCall> sceneCall) {
		advanceSceneCalls = sceneCall;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "callsheet")
	@OrderBy("lineNumber")
	public List<OtherCall> getOtherCalls() {
		return otherCalls;
	}
	public void setOtherCalls(List<OtherCall> otherCalls) {
		this.otherCalls = otherCalls;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "date=" + getDate();
		s += ", updated=" + getUpdated();
		s += ", status=" + (getStatus()==null ? "null" : status);
		s += ", project=" + (getProject()==null ? "null" : project.getId());
		s += ", shootDay=" + shootDay;
		s += ", shootDays=" + shootDays;
		s += ", advanceDays=" + advanceDays;
		s += ", calltime=" + callTime;
		s += ", shoottime=" + shootTime;
		s += ", rehearsetime=" + rehearseTime;
		s += ", pages=" + pages;
		s += ", sunrise=" + sunrise;
		s += ", sunset=" + sunset;
		s += ", weather=" + weather;
		if (sceneCalls != null) {
			s += ", scene calls: ";
			for (SceneCall sc : sceneCalls) {
				s += sc.toString() + Constants.NEW_LINE;
			}
		}
		if (castCalls != null) {
			s += ", cast: ";
			for (CastCall cc : castCalls) {
				s += cc.toString() + Constants.NEW_LINE;
			}
		}
		if (otherCalls != null) {
			s += ", other calls: ";
			for (OtherCall oc : otherCalls) {
				s += oc.toString() + Constants.NEW_LINE;
			}
		}
		if (advanceSceneCalls != null) {
			s += ", adv scene calls: ";
			for (SceneCall sc : advanceSceneCalls) {
				s += sc.toString() + Constants.NEW_LINE;
			}
		}
		if (deptCalls != null) {
			s += ", dept calls: ";
			for (DeptCall dc : deptCalls) {
				s += dc.toString() + Constants.NEW_LINE;
			}
		}
		if (callNotes != null) {
			s += ", call Notes: ";
			for (CallNote cn : callNotes) {
				s += cn.toString() + Constants.NEW_LINE;
			}
		}
		s += "]";
		return s;
	}

}