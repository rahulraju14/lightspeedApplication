package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * TimeCard entity.
 * <p>
 * NOTE: this is not used in the payroll system -- see the
 * {@link WeeklyTimecard} object for that.
 * <p>
 * Each object is the work time information for one person for one day. A set of
 * TimeCards is associated with a {@link Dpr} or {@link ExhibitG}. On the DPR,
 * it holds the in/out time information for the crew members on the "back page".
 * <p>
 * Some fields are used for all project members, and some are only for cast
 * members.
 */
@Entity
@Table(name = "time_card")
public class ReportTime extends PersistentObject<ReportTime> implements Cloneable {

	private static final long serialVersionUID = -3932266034898317027L;

	public static final String DTYPE_CREW = "TC";
	public static final String DTYPE_CAST = "CT";

	/** A negative value in the call or wrap time fields indicates "On-call". */
	public static final BigDecimal OC_TIME = new BigDecimal(-1);

	// Fields
	private Department department;
	private Contact contact;
//	private TimeSheet timeSheet;
	private Dpr dprCast;
	private Dpr dprCrew;
	private ExhibitG exhibitG;
	private Integer unitNumber = 1;
	private String dtype;
	private String role;
	private short listPriority;
	/** Element id (cast id) text string for this person's Character. */
	private String castIdStr;
	/** Element id (cast id), as numeric sort key, for this person's Character. */
	private Integer castId;
	private Boolean minor = new Boolean(false);

	/** The person's work status code, e.g., "W", "PWF". */
	private String dayType;

	/** Call time, for DPR. */
	private BigDecimal callTime;

	/** Wrap time, for DPR. If negative, display as "O/C" -- on-call. */
	private BigDecimal wrap;

	/** The date & time to report on-set; only used for Cast entries. */
	private Date reportSet;

	/** The date & time the person was dismissed from the set; only used for Cast entries. */
	private Date dismissSet;

	/** The date & time of the start of the first meal; also called "meal 1 out". */
	private Date firstMealStart;

	/** The date & time of the end of the first meal; also called "meal 1 in". */
	private Date firstMealEnd;

	/** The date & time of the start of the second meal; also called "meal 2 out". */
	private Date secondMealStart;

	/** The date & time of the end of the second meal; also called "meal 2 in". */
	private Date secondMealEnd;

	/** The date & time a cast member reported to make-up. */
	private Date reportMakeup;

	/** The date & time a cast member completed make-up. */
	private Date dismissMakeup;

	/** The date & time a non-deductible meal started. Null if there was no NDM. */
	private Date ndMealStart;

	/** The date & time the non-deductible meal ended. Null if there was no NDM. */
	private Date ndMealEnd;

	/** The date & time a cast member left for the location. */
	private Date leaveForLocation;

	/** The date & time a cast member arrived on location. */
	private Date arriveLocation;

	/** The date & time a cast member left the location (at the end of the day). */
	private Date leaveLocation;

	/** The date & time a cast member arrived at the studio (after
	 * leaving the location). */
	private Date arriveStudio;

	private Date stuntAdjust;

	private Integer wardrobeOutfits;

	/** meal period violations, a.m. */
	private String mpv1;

	/** meal period violations, p.m. */
	private String mpv2;

	private Byte forcedCall = Constants.FALSE;

	private Date tutoringHours;

	// Constructors

	/** default constructor */
	public ReportTime() {
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id")
	public Department getDepartment() {
		return department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id", nullable = false)
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Time_Sheet_Id")
//	public TimeSheet getTimeSheet() {
//		return this.timeSheet;
//	}
//	public void setTimeSheet(TimeSheet timeSheet) {
//		this.timeSheet = timeSheet;
//	}

	/** See {@link #dprCast}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Dpr_Cast_Id")
	public Dpr getDprCast() {
		return dprCast;
	}
	/** See {@link #dprCast}. */
	public void setDprCast(Dpr dprCast) {
		this.dprCast = dprCast;
	}

	/** See {@link #dprCrew}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Dpr_Crew_Id")
	public Dpr getDprCrew() {
		return dprCrew;
	}
	/** See {@link #dprCrew}. */
	public void setDprCrew(Dpr dprCrew) {
		this.dprCrew = dprCrew;
	}

	/** See {@link #exhibitG}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ExhibitG_Id")
	public ExhibitG getExhibitG() {
		return exhibitG;
	}
	/** See {@link #exhibitG}. */
	public void setExhibitG(ExhibitG exhibitG) {
		this.exhibitG = exhibitG;
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

	@Column(name = "DTYPE", length = 10)
	public String getDtype() {
		return dtype;
	}
	public void setDtype(String dtype) {
		this.dtype = dtype;
	}

	@Column(name = "Role", length = 50)
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}

	/** See {@link #listPriority}. */
	@Column(name = "List_Priority")
	public short getListPriority() {
		return listPriority;
	}
	/** See {@link #listPriority}. */
	public void setListPriority(short listPriority) {
		this.listPriority = listPriority;
	}

	/** See {@link #castIdStr}. */
	@Column(name = "CastId_Str", length = 10)
	public String getCastIdStr() {
		return castIdStr;
	}
	/** See {@link #castIdStr}. */
	public void setCastIdStr(String castIdStr) {
		this.castIdStr = castIdStr;
	}

	/** See {@link #castId}. */
	@Column(name = "CastId")
	public Integer getCastId() {
		return castId;
	}
	/** See {@link #castId}. */
	public void setCastId(Integer castId) {
		this.castId = castId;
	}

	@Column(name = "Minor", nullable = false)
	public Boolean getMinor() {
		return minor;
	}
	public void setMinor(Boolean minor) {
		this.minor = minor;
	}

	//@Enumerated(EnumType.STRING)
	@Column(name = "Day_Type", nullable = false, length = 30)
	public String getDayType() {
		return dayType;
	}
	public void setDayType(String dayType) {
		this.dayType = dayType;
	}

	/** See {@link #callTime}. */
	@Column(name = "Call_Time", precision = 4, scale = 2)
	public BigDecimal getCallTime() {
		if (callTime == null && getReportSet() != null) {
			return CalendarUtils.convertTimeToDecimal(reportSet, false);
		}
		return callTime;
	}
	/** See {@link #callTime}. */
	public void setCallTime(BigDecimal call) {
		callTime = call;
	}

	/** See {@link #wrap}. */
	@Column(name = "Wrap", precision = 4, scale = 2)
	public BigDecimal getWrap() {
		if (wrap == null && getDismissSet() != null) {
			return CalendarUtils.convertTimeToDecimal(dismissSet, true);
		}
		return wrap;
	}
	/** See {@link #wrap}. */
	public void setWrap(BigDecimal wrap) {
		this.wrap = wrap;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Report_Set", length = 0)
	public Date getReportSet() {
		return reportSet;
	}
	public void setReportSet(Date reportSet) {
		this.reportSet = reportSet;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Dismiss_Set", length = 0)
	public Date getDismissSet() {
		return dismissSet;
	}
	public void setDismissSet(Date dismissSet) {
		this.dismissSet = dismissSet;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Meal_Start", length = 0)
	public Date getFirstMealStart() {
		return firstMealStart;
	}
	public void setFirstMealStart(Date firstMealStart) {
		this.firstMealStart = firstMealStart;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "First_Meal_End", length = 0)
	public Date getFirstMealEnd() {
		return firstMealEnd;
	}
	public void setFirstMealEnd(Date firstMealEnd) {
		this.firstMealEnd = firstMealEnd;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Second_Meal_Start", length = 0)
	public Date getSecondMealStart() {
		return secondMealStart;
	}
	public void setSecondMealStart(Date secondMealStart) {
		this.secondMealStart = secondMealStart;
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
	@Column(name = "Report_Makeup", length = 0)
	public Date getReportMakeup() {
		return reportMakeup;
	}
	public void setReportMakeup(Date reportMakeup) {
		this.reportMakeup = reportMakeup;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Dismiss_Makeup", length = 0)
	public Date getDismissMakeup() {
		return dismissMakeup;
	}
	public void setDismissMakeup(Date dismissMakeup) {
		this.dismissMakeup = dismissMakeup;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ND_Meal_Start", length = 0)
	public Date getNdMealStart() {
		return ndMealStart;
	}
	public void setNdMealStart(Date ndMealStart) {
		this.ndMealStart = ndMealStart;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "ND_Meal_End", length = 0)
	public Date getNdMealEnd() {
		return ndMealEnd;
	}
	public void setNdMealEnd(Date ndMealEnd) {
		this.ndMealEnd = ndMealEnd;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Leave_For_Location", length = 0)
	public Date getLeaveForLocation() {
		return leaveForLocation;
	}
	public void setLeaveForLocation(Date leaveForLocation) {
		this.leaveForLocation = leaveForLocation;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Arrive_Location", length = 0)
	public Date getArriveLocation() {
		return arriveLocation;
	}
	public void setArriveLocation(Date arriveLocation) {
		this.arriveLocation = arriveLocation;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Leave_Location", length = 0)
	public Date getLeaveLocation() {
		return leaveLocation;
	}
	public void setLeaveLocation(Date leaveLocation) {
		this.leaveLocation = leaveLocation;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Arrive_Studio", length = 0)
	public Date getArriveStudio() {
		return arriveStudio;
	}
	public void setArriveStudio(Date arriveStudio) {
		this.arriveStudio = arriveStudio;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Stunt_Adjust", length = 0)
	public Date getStuntAdjust() {
		return stuntAdjust;
	}
	public void setStuntAdjust(Date stuntAdjust) {
		this.stuntAdjust = stuntAdjust;
	}

	@Column(name = "Wardrobe_Outfits")
	public Integer getWardrobeOutfits() {
		return wardrobeOutfits;
	}
	public void setWardrobeOutfits(Integer wardrobeOutfits) {
		this.wardrobeOutfits = wardrobeOutfits;
	}

	/** See {@link #mpv1}. */
	@Column(name = "mpv1")
	public String getMpv1() {
		return mpv1;
	}
	/** See {@link #mpv1}. */
	public void setMpv1(String mpv1) {
		this.mpv1 = mpv1;
	}

	/** See {@link #mpv2}. */
	@Column(name = "mpv2")
	public String getMpv2() {
		return mpv2;
	}
	/** See {@link #mpv2}. */
	public void setMpv2(String mpv2) {
		this.mpv2 = mpv2;
	}

	@Column(name = "Forced_Call", nullable = false)
	public Byte getForcedCall() {
		return forcedCall;
	}
	public void setForcedCall(Byte forcedCall) {
		this.forcedCall = forcedCall;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Tutoring_Hours", length = 0)
	public Date getTutoringHours() {
		return tutoringHours;
	}
	public void setTutoringHours(Date tutoringHours) {
		this.tutoringHours = tutoringHours;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + getId();
		s += ", role=" + getRole();
		s += "]";
		return s;
	}

	@Override
	public ReportTime clone() {
		try {
			ReportTime copy = (ReportTime)super.clone();
			copy.id = null; // it's a transient object
//			copy.timeSheet = null; // break any relationships
			copy.dprCast = null;
			copy.dprCrew = null;
			copy.exhibitG = null;
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

	public static Comparator<ReportTime> castIdComparator() {
		Comparator<ReportTime> comparator = new Comparator<ReportTime>() {
			@Override
			public int compare(ReportTime one, ReportTime two) {
				return NumberUtils.compare(one.getCastId(), two.getCastId());
			}
		};
		return comparator;
	}

	public static Comparator<ReportTime> priorityComparator = new Comparator<ReportTime>() {
		@Override
		public int compare(ReportTime one, ReportTime two) {
			return NumberUtils.compare(one.getListPriority(), two.getListPriority());
		}
	};

}