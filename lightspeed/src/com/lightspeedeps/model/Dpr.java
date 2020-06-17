package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
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

/**
 * Dpr entity, which holds the information for a single "Daily Production Report",
 * sometimes simply referred to as a "PR" (production report).
 *
 */
@Entity
@Table(name = "dpr", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Date", "Project_Id", "Unit_Number" }))
public class Dpr extends PersistentObject<Dpr> {

	// Fields
	private static final long serialVersionUID = 8943477653209876001L;

	/** */
	private Date date;

	/** */
	private Date updated;

	/** */
	private Project project;
	/** The Unit number associated with this DPR. */
	private Integer unitNumber = 1;

	/** */
	private ReportStatus status;

	/** */
	private Date startDate;

	/** */
	private Date endDate;

	/** */
	private Date revisedEndDate;

	/** */
	private List<DprEpisode> dprEpisodes = new ArrayList<>(0);

	/** */
	private DprDays dprDaysScheduled;

	/** */
	private DprDays dprDaysToDate;

	/** */
	private String daysStatus;

	/** */
	private Integer shootDay;

	/** */
	private Integer shootDays;


	/** */
	private String director;

	/** */
	private String producer;

	/** */
	private String contact1;

	/** */
	private String contact2;

	/** */
	private String contact3;

	/** */
	private String contact4;


	/** */
	private String comment1;

	/** */
	private String comment2;

	/** */
	private String comment3;

	/** */
	private String castNotes;

	/** */
	private String backgroundSummary1 = "Stand-Ins:";

	/** */
	private String backgroundSummary2 = "Total background:";

	/** */
	private String sets;

	/** */
	private String sceneNote;

	/** */
	private String cameraRolls;


	/** */
	private Date ndmEnd;

	/** */
	private Date crewCall;

	/** */
	private Date shootingCall;

	/** */
	private Date firstShot;

	/** */
	private Date firstMealBegin;

	/** */
	private Date firstMealEnd;

	/** */
	private Date lastManIn;

	/** */
	private Date firstShotAfter1stMeal;

	/** */
	private Date secondMealBegin;

	/** */
	private Date secondMealEnd;

	/** */
	private Date firstShotAfter2ndMeal;

	/** */
	private Date cameraWrap;

	/** */
	private Date lastManOut;

	/** */
	private BigDecimal overtimeUsedPrior;

	/** */
	private BigDecimal overtimeUsedToday;

	/** */
	private BigDecimal overtimeUsedTotal;

	/** */
	private BigDecimal overtimeRemaining;


	/** */
	private String soundCardIdNum;

	/** */
	private String generalNotes;

	/** */
	private String equipmentNotes;

	/** */
	private String productionNotes;

	/** */
	private String crewNotes;

	/** */
	private Set<VehicleLog> vehicleLogs = new HashSet<>(0);

	/** */
	private List<ExtraTime> extraTimes = new ArrayList<>(0);

	/** */
	private Set<CateringLog> cateringLogs = new HashSet<>(0);

	/** */
	private List<DprScene> dprScenes = new ArrayList<>(0);

	/** */
	private List<ReportTime> castTimeCards = new ArrayList<>(0);

	/** */
	private Set<ReportTime> crewTimeCards = new HashSet<>(0);


	/** */
	List<ExtraTime> backgrounds;

	/** */
	List<ExtraTime> standins;

	/** Convert the Set to an ArrayList that can be used on the xhtml page. */
	@Transient
	private List<CateringLog> cateringLogsList;

	// Constructors

	/** default constructor */
	public Dpr() {
	}


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
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

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Days_Scheduled_Id")
	public DprDays getDprDaysScheduled() {
		return dprDaysScheduled;
	}
	public void setDprDaysScheduled(DprDays days) {
		dprDaysScheduled = days;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Days_To_Date_Id")
	public DprDays getDprDaysToDate() {
		return dprDaysToDate;
	}
	public void setDprDaysToDate(DprDays days) {
		dprDaysToDate = days;
	}

	/**See {@link #daysStatus}. */
	@Column(name = "Days_Status", length = 50)
	public String getDaysStatus() {
		return daysStatus;
	}
	/**See {@link #daysStatus}. */
	public void setDaysStatus(String daysStatus) {
		this.daysStatus = daysStatus;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false)
	public ReportStatus getStatus() {
		return status;
	}
	public void setStatus(ReportStatus status) {
		this.status = status;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", length = 0)
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 0)
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Revised_End_Date", length = 0)
	public Date getRevisedEndDate() {
		return revisedEndDate;
	}
	public void setRevisedEndDate(Date revisedEndDate) {
		this.revisedEndDate = revisedEndDate;
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

	@Column(name = "Director", length = 50)
	public String getDirector() {
		return director;
	}
	public void setDirector(String director) {
		this.director = director;
	}

	@Column(name = "Producer", length = 500)
	public String getProducer() {
		return producer;
	}
	public void setProducer(String producer) {
		this.producer = producer;
	}

	@Column(name = "UPM", length = 200)
	public String getContact1() {
		return contact1;
	}
	public void setContact1(String upm) {
		contact1 = upm;
	}

	@Column(name = "First_AD", length = 200)
	public String getContact2() {
		return contact2;
	}
	public void setContact2(String firstAd) {
		contact2 = firstAd;
	}

	@Column(name = "Second_AD", length = 200)
	public String getContact3() {
		return contact3;
	}
	public void setContact3(String secondAd) {
		contact3 = secondAd;
	}

	@Column(name = "Contact4", length = 200)
	public String getContact4() {
		return contact4;
	}
	public void setContact4(String str) {
		contact4 = str;
	}

	/**See {@link #comment1}. */
	@Column(name = "Comment1", length = 200)
	public String getComment1() {
		return comment1;
	}
	/**See {@link #comment1}. */
	public void setComment1(String comment1) {
		this.comment1 = comment1;
	}

	/**See {@link #comment2}. */
	@Column(name = "Comment2", length = 200)
	public String getComment2() {
		return comment2;
	}
	/**See {@link #comment2}. */
	public void setComment2(String comment2) {
		this.comment2 = comment2;
	}

	/**See {@link #comment3}. */
	@Column(name = "Comment3", length = 200)
	public String getComment3() {
		return comment3;
	}
	/**See {@link #comment3}. */
	public void setComment3(String comment3) {
		this.comment3 = comment3;
	}

	/**See {@link #castNotes}. */
	@Column(name = "Cast_Notes", length = 500)
	public String getCastNotes() {
		return castNotes;
	}
	/**See {@link #castNotes}. */
	public void setCastNotes(String castNotes) {
		this.castNotes = castNotes;
	}

	/**See {@link #backgroundSummary1}. */
	@Column(name = "Background_Summary_1", length = 50)
	public String getBackgroundSummary1() {
		return backgroundSummary1;
	}
	/**See {@link #backgroundSummary1}. */
	public void setBackgroundSummary1(String backgroundSummary1) {
		this.backgroundSummary1 = backgroundSummary1;
	}

	/**See {@link #backgroundSummary2}. */
	@Column(name = "Background_Summary_2", length = 50)
	public String getBackgroundSummary2() {
		return backgroundSummary2;
	}
	/**See {@link #backgroundSummary2}. */
	public void setBackgroundSummary2(String backgroundSummary2) {
		this.backgroundSummary2 = backgroundSummary2;
	}

	/**See {@link #dprEpisodes}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dpr")
	public List<DprEpisode> getDprEpisodes() {
		return dprEpisodes;
	}
	/**See {@link #dprEpisodes}. */
	public void setDprEpisodes(List<DprEpisode> dprEpisodes) {
		this.dprEpisodes = dprEpisodes;
	}

	/**
	 * @return the first DprEpisode from the list of DprEpisode`s. This access is
	 * typically used for feature films, where only one DprEpisode will exist.
	 */
	@Transient
	public DprEpisode getDprEpisode() {
		if (getDprEpisodes() != null &&
				getDprEpisodes().size() > 0) {
			return getDprEpisodes().get(0);
		}
		return null;
	}


	@Column(name = "Sets", length = 1000)
	public String getSets() {
		return sets;
	}
	public void setSets(String sets) {
		this.sets = sets;
	}

	@Column(name = "Scene_Note", length = 1000)
	public String getSceneNote() {
		return sceneNote;
	}
	public void setSceneNote(String sceneNote) {
		this.sceneNote = sceneNote;
	}

	@Column(name = "Camera_Rolls", length = 100)
	public String getCameraRolls() {
		return cameraRolls;
	}
	public void setCameraRolls(String cameraRolls) {
		this.cameraRolls = cameraRolls;
	}

	/** See {@link #ndmEnd}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Ndm_End", length = 0)
	public Date getNdmEnd() {
		return ndmEnd;
	}
	/** See {@link #ndmEnd}. */
	public void setNdmEnd(Date ndmEnd) {
		this.ndmEnd = ndmEnd;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Crew_Call", length = 0)
	public Date getCrewCall() {
		return crewCall;
	}
	public void setCrewCall(Date crewCall) {
		this.crewCall = crewCall;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Shooting_Call", length = 0)
	public Date getShootingCall() {
		return shootingCall;
	}
	public void setShootingCall(Date shootingCall) {
		this.shootingCall = shootingCall;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Shot", length = 0)
	public Date getFirstShot() {
		return firstShot;
	}
	public void setFirstShot(Date firstShot) {
		this.firstShot = firstShot;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Meal_Begin", length = 0)
	public Date getFirstMealBegin() {
		return firstMealBegin;
	}
	public void setFirstMealBegin(Date firstMealBegin) {
		this.firstMealBegin = firstMealBegin;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Meal_End", length = 0)
	public Date getFirstMealEnd() {
		return firstMealEnd;
	}
	public void setFirstMealEnd(Date firstMealEnd) {
		this.firstMealEnd = firstMealEnd;
	}

	/** See {@link #lastManIn}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Man_In", length = 0)
	public Date getLastManIn() {
		return lastManIn;
	}
	/** See {@link #lastManIn}. */
	public void setLastManIn(Date lastManIn) {
		this.lastManIn = lastManIn;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Shot_After_1st_Meal", length = 0)
	public Date getFirstShotAfter1stMeal() {
		return firstShotAfter1stMeal;
	}
	public void setFirstShotAfter1stMeal(Date firstShotAfter1stMeal) {
		this.firstShotAfter1stMeal = firstShotAfter1stMeal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Second_Meal_Begin", length = 0)
	public Date getSecondMealBegin() {
		return secondMealBegin;
	}
	public void setSecondMealBegin(Date secondMealBegin) {
		this.secondMealBegin = secondMealBegin;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Second_Meal_End", length = 0)
	public Date getSecondMealEnd() {
		return secondMealEnd;
	}
	public void setSecondMealEnd(Date secondMealEnd) {
		this.secondMealEnd = secondMealEnd;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Shot_After_2nd_Meal", length = 0)
	public Date getFirstShotAfter2ndMeal() {
		return firstShotAfter2ndMeal;
	}
	public void setFirstShotAfter2ndMeal(Date firstShotAfter2ndMeal) {
		this.firstShotAfter2ndMeal = firstShotAfter2ndMeal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Camera_Wrap", length = 0)
	public Date getCameraWrap() {
		return cameraWrap;
	}
	public void setCameraWrap(Date cameraWrap) {
		this.cameraWrap = cameraWrap;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Man_Out", length = 0)
	public Date getLastManOut() {
		return lastManOut;
	}
	public void setLastManOut(Date lastManOut) {
		this.lastManOut = lastManOut;
	}

	@Column(name = "Overtime_Used_Prior", precision = 8, scale = 2)
	public BigDecimal getOvertimeUsedPrior() {
		return overtimeUsedPrior;
	}
	public void setOvertimeUsedPrior(BigDecimal overtimeUsedPrior) {
		this.overtimeUsedPrior = overtimeUsedPrior;
	}

	@Column(name = "Overtime_Used_Today", precision = 8, scale = 2)
	public BigDecimal getOvertimeUsedToday() {
		return overtimeUsedToday;
	}
	public void setOvertimeUsedToday(BigDecimal overtimeUsedToday) {
		this.overtimeUsedToday = overtimeUsedToday;
	}

	@Column(name = "Overtime_Used_Total", precision = 8, scale = 2)
	public BigDecimal getOvertimeUsedTotal() {
		return overtimeUsedTotal;
	}
	public void setOvertimeUsedTotal(BigDecimal overtimeUsedTotal) {
		this.overtimeUsedTotal = overtimeUsedTotal;
	}

	@Column(name = "Overtime_Remaining", precision = 8, scale = 2)
	public BigDecimal getOvertimeRemaining() {
		return overtimeRemaining;
	}
	public void setOvertimeRemaining(BigDecimal overtimeRemaining) {
		this.overtimeRemaining = overtimeRemaining;
	}

	@Column(name = "Sound_Card_Id_Num", length = 10)
	public String getSoundCardIdNum() {
		return soundCardIdNum;
	}
	public void setSoundCardIdNum(String soundCardIdNum) {
		this.soundCardIdNum = soundCardIdNum;
	}

	/**See {@link #generalNotes}. */
	@Column(name = "General_Notes", length = 5000)
	public String getGeneralNotes() {
		return generalNotes;
	}
	/**See {@link #generalNotes}. */
	public void setGeneralNotes(String generalNotes) {
		this.generalNotes = generalNotes;
	}

	@Column(name = "Equipment_Notes", length = 1000)
	public String getEquipmentNotes() {
		return equipmentNotes;
	}
	public void setEquipmentNotes(String equipmentNotes) {
		this.equipmentNotes = equipmentNotes;
	}

	@Column(name = "Production_Notes", length = 1000)
	public String getProductionNotes() {
		return productionNotes;
	}
	public void setProductionNotes(String productionNotes) {
		this.productionNotes = productionNotes;
	}

	@Column(name = "Crew_Notes", length = 1000)
	public String getCrewNotes() {
		return crewNotes;
	}
	public void setCrewNotes(String crewNotes) {
		this.crewNotes = crewNotes;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dpr")
	public Set<VehicleLog> getVehicleLogs() {
		return vehicleLogs;
	}
	public void setVehicleLogs(Set<VehicleLog> vehicleLogs) {
		this.vehicleLogs = vehicleLogs;
	}

	@OneToMany(cascade = {CascadeType.ALL,CascadeType.REMOVE},
			fetch = FetchType.LAZY, mappedBy = "dpr")
	@OrderBy("lineNumber")
	public List<ExtraTime> getExtraTimes() {
		return extraTimes;
	}
	public void setExtraTimes(List<ExtraTime> extraTimes) {
		this.extraTimes = extraTimes;
		standins = null;
		backgrounds = null;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dpr")
	public Set<CateringLog> getCateringLogs() {
		return cateringLogs;
	}
	public void setCateringLogs(Set<CateringLog> cateringLogs) {
		this.cateringLogs = cateringLogs;
		cateringLogsList = null; // Force refresh
	}

	/** See {@link #cateringLogsList}. */
	@Transient
	public List<CateringLog> getCateringLogsList() {
		if(cateringLogsList == null) {
			cateringLogsList = new ArrayList<>(cateringLogs);
		}
		return cateringLogsList;
	}
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dpr")
	@OrderBy("lineNumber")
	public List<DprScene> getDprScenes() {
		return dprScenes;
	}
	public void setDprScenes(List<DprScene> dprScenes) {
		this.dprScenes = dprScenes;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dprCast")
	@OrderBy("castId")
	public List<ReportTime> getCastTimeCards() {
		return castTimeCards;
	}
	public void setCastTimeCards(List<ReportTime> timeCards) {
		castTimeCards = timeCards;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dprCrew")
	public Set<ReportTime> getCrewTimeCards() {
		return crewTimeCards;
	}
	public void setCrewTimeCards(Set<ReportTime> timeCards) {
		crewTimeCards = timeCards;
	}

	@Transient
	public List<ExtraTime> getBackgrounds() {
		return  getExtraTimes();
	}
}
