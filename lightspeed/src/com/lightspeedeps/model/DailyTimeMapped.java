/**
 * File: DailyTimeMapped.java
 */
package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.ProductionPhase;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;

/**
 * This class defines most of the fields for the DailyTime entity; it includes
 * those fields that are shared by the DailyTime and DailyHotCost entities. This
 * represents one day's information for the WeeklyTimecard. Currently a standard
 * WeeklyTimecard has exactly seven of these, but the model allows for more than
 * that, if "split days" were to be represented by extra DailyTime entries.
 * <p>
 * For Canadian productions, the ACTRA contract form includes a WeeklyTimecard
 * with 13 DailyTime entries.
 */
@MappedSuperclass
public class DailyTimeMapped extends PersistentObject<DailyTimeMapped> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(DailyTimeMapped.class);

	private static final long serialVersionUID = 7580385089910145564L;

	// Fields
	/** Which day number, 1(Sunday)-7(Saturday), this entity corresponds to.
	 * This is typically different than workDayNum. */
	private byte dayNum;

	/** The date of this daily entry. */
	private Date date;

	/** call (start) time, in decimal notation. Must be non-negative and
	 * less than 24. */
	private BigDecimal callTime;

	/** First meal start time, in decimal notation. Must be non-negative and
	 * less than 72. */
	private BigDecimal m1Out;

	/** First meal end time, in decimal notation. Must be non-negative and
	 * less than 72. */
	private BigDecimal m1In;

	/** Second meal start time, in decimal notation. Must be non-negative and
	 * less than 72. */
	private BigDecimal m2Out;

	/** Second meal end time, in decimal notation. Must be non-negative and
	 * less than 72. */
	private BigDecimal m2In;

	/** call (start) time, in decimal notation. Must be non-negative and
	 * less than 24. */
	private BigDecimal wrap;

	/** For non-union hourly employees, the beginning of "on call" time which
	 * will be paid; typically used as an alternative to guaranteed hours. */
	private BigDecimal onCallStart;

	/** For non-union hourly employees, the end of "on call" time which
	 * will be paid; typically used as an alternative to guaranteed hours. */
	private BigDecimal onCallEnd;

	/** Calculated number of worked hours, excluding meal times. */
	private BigDecimal hours;

	/** True iff this employee is reporting the day as "worked", rather than
	 * reporting specific hours.  Only allowed for a small set of occupations. */
	private boolean worked;

	/** area where the employee worked this day, e.g., Stage */
	private WorkZone workZone;

	/** Type of day; see enumeration. */
	private DayType dayType;

	/** Production work phase, i.e., prep, shoot, or wrap. Only used in Commercial
	 * Production`s. */
	private ProductionPhase phase;

	/** True iff there is no Start Form covering this day.  An employee may not
	 * enter hours for this day if this is true. */
	private boolean noStartForm;

	/** Day is opposite (of scheduled type?) */
//	private boolean opposite; // dropped - rev 2.9.5121

	/** Work for this day is off-production. */
	private boolean offProduction;

	/** True if forced call in effect. */
	private boolean forcedCall;

	/** True if a non-deductible meal (NDM) was served, typically breakfast.
	 * Sometimes annotated as "NDB": non-deductible breakfast. */
	private boolean nonDeductMeal;

	/** True if a second non-deductible meal (NDM) was served. */
	private boolean nonDeductMeal2;

	/** Payroll's copy: True if a non-deductible meal (NDM) was served, typically breakfast.
	 * Sometimes annotated as "NDB": non-deductible breakfast. */
	private boolean nonDeductMealPayroll;

	/** Payroll's copy: True if a second non-deductible meal (NDM) was served. */
	private boolean nonDeductMeal2Payroll;

	/** The end time, in decimal notation, of the NDB (non-deductible
	 * breakfast), if any. */
	private BigDecimal ndbEnd;

	/** The start time, in decimal notation, of the NDM (non-deductible
	 * meal, typically a lunch or dinner), if any. */
	private BigDecimal ndmStart;

	/** The end time, in decimal notation, of the NDM (non-deductible
	 * meal, typically a lunch or dinner), if any. */
	private BigDecimal ndmEnd;

	/** Last-man-in time, in decimal notation. */
	private BigDecimal lastManIn;

	/** employee-entered value for MPV (meal penalty violations) for the whole day. */
	private String mpvUser;

	/** Length of grace-1 period; typically 12. */
	private BigDecimal grace1;

	/** Length of grace-2 period; typically 12. */
	private BigDecimal grace2;

	/** True iff an extended meal period was called before wrap
	 * (typically 6.5 hours instead of 6 hours). */
	private boolean cameraWrap;

	/** True iff "French hours" were in effect for this day. */
	private boolean frenchHours;

	/** Location account code. */
	private String accountLoc;

	/** production # / episode -- "Major" account code */
	private String accountMajor;

	/** Account code - Set. */
	private String accountSet;

	/** Account code - Free. */
	private String accountFree;

	/** True if the employee is on a re-rate (different job) for this day. */
	private boolean reRate;

	/** City worked. */
	private String city;

	/** State worked. For U.S. states, this is the 2-letter standard
	 * postal (USPS) abbreviation. */
	private String state;

	/** Country worked - ISO code (2 letters).  If null, "US" is assumed. */
	private String country;

	/** The worked-day number, used to control, for example 6th-day and
	 * 7th-day rates.  This is usually NOT the same as dayNum! */
	private byte workDayNum;

	/** Calculated or payroll-entered value for MPV for the first meal of the day. */
	private Byte mpv1Payroll;

	/** Calculated or payroll-entered value for MPV for the second meal of the day
	 * plus any meal penalties for time between the second meal and wrap. */
	private Byte mpv2Payroll;

	/** Calculated value for MPV for the third period (after 2nd meal & before wrap) of the day. */
	private Byte mpv3Payroll;

	/** If true, treat split values in DailyTime as percentages, not hours. */
	private boolean splitByPercent;

	/** The sequence number (origin 1) of the first PayJob for this day's work. */
	private byte jobNum1 = 1;

	/** The time (in decimal notation) when the second PayJob takes effect, or null
	 * if the day is not split. */
	private BigDecimal splitStart2;

	/** The sequence number (origin 1) of the second PayJob for this day's work. */
	private byte jobNum2 = 1;

	/** The time (in decimal notation) when the third PayJob takes effect, or null
	 * if the day is not split into a third part. */
	private BigDecimal splitStart3;

	/** The sequence number (origin 1) of the third PayJob for this day's work. */
	private byte jobNum3 = 1;

	private Boolean opposite = false;

	private BigDecimal payAmount;

	/**
	 * Flag to disable the State dropdown. Usually use depending on
	 * the DayType selected for a hybrid production. LS-2163
	 */
	private boolean disableState;

	/**
	 * Flag to disable the City input field. Usually use depending on
	 * the DayType selected for a hybrid production. LS-2163
	 */
	private boolean disableCity;

	/**
	 * Flag to disable the Country dropdown. Usually use depending on
	 * the DayType selected for a hybrid production. LS-2163
	 */
	private boolean disableCountry;

	/** The day of the calendar week that this DailyTime represents, 1=Sunday,
	 * 7=Saturday. */
	@Transient
	private Byte weekDayNum;

	/** The calculated time of day or percentage of time assigned to the first
	 * job in a split.  Used during HTG calculations. */
	@Transient
	private BigDecimal split1;

	/** The calculated time of day or percentage of time assigned to the second
	 * job in a split.  Used during HTG calculations. */
	@Transient
	private BigDecimal split2;

	/** The calculated time of day or percentage of time assigned to the third
	 * job in a split.  Used during HTG calculations. */
	@Transient
	private BigDecimal split3;

	/** A bit-wise mask indicating which job numbers are referenced in this
	 * instances job splits.  Used in the Job table code when "auto splitting"
	 * a job number. */
	@Transient
	private int jobMask;

	/** The "group number" used in the Job table code when "auto splitting"
	 * a job number. */
	@Transient
	private int jobSplitGroup;

	/** Used for highlighting or other UI features. */
	@Transient
	private boolean selected;

	/** Boolean array to keep track of which phase radio button has been selected */
	@Transient
	private boolean[] phases = new boolean[4];

	/** Fields for model timecard weatherDay changes LS-4589 **/
	private boolean weatherDay;

	/** Fields for model timecard intimatesDay  **/
	private boolean intimatesDay;

	/** Fields for model timecard  comments **/
	private String comments;

	/** Fields for model timecard allowWeather check for DailyTime **/
	@Transient
	private boolean allowWeather = false;

	/** Fields for model timecard allowIntimates check for DailyTime**/
	@Transient
	private boolean allowIntimates = false ;


	// Property accessors

	public DailyTimeMapped() {

	}

	/** normal constructor */
//	public DailyTimeMapped(WeeklyTimecard weeklyTimecard, byte dayNum) {
//		this.weeklyTimecard = weeklyTimecard;
//		this.dayNum = dayNum;
//	}

//	/**
//	 * Constructor used primarily for JUnit testing, for a day with no meal times.
//	 */
//	public DailyTimeMapped(WorkZone zone, BigDecimal call, BigDecimal wrp) {
//		workZone = zone;
//		callTime = call;
//		wrap = wrp;
//		// Set various defaults appropriate for testing
//		dayNum = 2;	// Monday
//		dayType = DayType.WK;
//	}
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with a single meal.
//	 */
//	public DailyTimeMapped(WorkZone zone, BigDecimal call, BigDecimal m1out, BigDecimal m1in, BigDecimal wrp) {
//		this(zone, call, wrp);
//		m1Out = m1out;
//		m1In = m1in;
//	}
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with two meals.
//	 */
//	public DailyTimeMapped(WorkZone zone, BigDecimal call, BigDecimal m1out, BigDecimal m1in,
//			BigDecimal m2out, BigDecimal m2in, BigDecimal wrp) {
//		this(zone, call, m1out, m1in, wrp);
//		m2Out = m2out;
//		m2In = m2in;
//	}
//
//	// Constructors taking double's instead of BigDecimal's -- for ease of writing.
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with no meal times.
//	 */
//	public DailyTimeMapped(WorkZone zone, double callTime, double wrap) {
//		this(zone, decimal(callTime), decimal(wrap));
//	}
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with no meal times.
//	 */
//	public DailyTimeMapped(double callTime, double wrap) {
//		this(WorkZone.ZN, decimal(callTime), decimal(wrap));
//	}
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with a single meal.
//	 */
//	public DailyTimeMapped(double callTime, double m1start, double m1end, double wrap) {
//		this(WorkZone.ZN, decimal(callTime), decimal(m1start), decimal(m1end), decimal(wrap));
//	}
//
//	/**
//	 * Constructor used primarily for JUnit testing, for a day with two meals.
//	 */
//	public DailyTimeMapped(double callTime, double m1start,
//			double m1end, double m2start, double m2end, double wrap) {
//		this(WorkZone.ZN, decimal(callTime), decimal(m1start), decimal(m1end),
//				decimal(m2start), decimal(m2end), decimal(wrap));
//	}



	/** See {@link #dayNum} */
	@JsonIgnore
	@Column(name = "Day_Num", nullable = false)
	public byte getDayNum() {
		return dayNum;
	}
	/** See {@link #dayNum} */
	public void setDayNum(byte dayNum) {
		this.dayNum = dayNum;
	}

	@JsonIgnore
	@Transient
	public byte getWeekDayNum() {
		if (weekDayNum == null) {
			Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			cal.setTime(getDate());
			weekDayNum = (byte)cal.get(Calendar.DAY_OF_WEEK);
		}
		return weekDayNum;
	}

	@JsonIgnore
	@Transient
	public String getWeekDayName() {
		return Constants.WEEKDAY_NAMES.get(getWeekDayNum()-1);
	}

	/**See {@link #workDayNum}. */
	@Column(name = "Work_Day_Num", nullable = false)
	public byte getWorkDayNum() {
		return workDayNum;
	}
	/**See {@link #workDayNum}. */
	public void setWorkDayNum(byte workDayNum) {
		this.workDayNum = workDayNum;
	}

	/** See {@link #date} */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date", nullable = true, length = 10)
	public Date getDate() {
		return date;
	}
	/** See {@link #date} */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #callTime} */
	@Column(name = "Call_Time", precision = 4, scale = 2)
	public BigDecimal getCallTime() {
		return callTime;
	}
	/** See {@link #callTime} */
	public void setCallTime(BigDecimal callTime) {
		this.callTime = callTime;
	}

	/** See {@link #m1Out} */
	@Column(name = "M1_Out", precision = 4, scale = 2)
	public BigDecimal getM1Out() {
		return m1Out;
	}
	/** See {@link #m1Out} */
	public void setM1Out(BigDecimal m1Out) {
		this.m1Out = m1Out;
	}

	/** See {@link #m1In} */
	@Column(name = "M1_In", precision = 4, scale = 2)
	public BigDecimal getM1In() {
		return m1In;
	}
	/** See {@link #m1In} */
	public void setM1In(BigDecimal m1In) {
		this.m1In = m1In;
	}

	/** See {@link #m2Out} */
	@Column(name = "M2_Out", precision = 4, scale = 2)
	public BigDecimal getM2Out() {
		return m2Out;
	}
	/** See {@link #m2Out} */
	public void setM2Out(BigDecimal m2Out) {
		this.m2Out = m2Out;
	}

	/** See {@link #m2In} */
	@Column(name = "M2_In", precision = 4, scale = 2)
	public BigDecimal getM2In() {
		return m2In;
	}
	/** See {@link #m2In} */
	public void setM2In(BigDecimal m2In) {
		this.m2In = m2In;
	}

	/** See {@link #wrap} */
	@Column(name = "Wrap", precision = 4, scale = 2)
	public BigDecimal getWrap() {
		return wrap;
	}
	/** See {@link #wrap} */
	public void setWrap(BigDecimal wrap) {
		this.wrap = wrap;
	}

	/** See {@link #onCallStart}. */
	@Column(name = "On_Call_Start", precision = 4, scale = 2)
	public BigDecimal getOnCallStart() {
		return onCallStart;
	}
	/** See {@link #onCallStart}. */
	public void setOnCallStart(BigDecimal onCallStart) {
		this.onCallStart = onCallStart;
	}

	/** See {@link #onCallEnd}. */
	@Column(name = "On_Call_End", precision = 4, scale = 2)
	public BigDecimal getOnCallEnd() {
		return onCallEnd;
	}
	/** See {@link #onCallEnd}. */
	public void setOnCallEnd(BigDecimal onCallEnd) {
		this.onCallEnd = onCallEnd;
	}

	/**
	 * @return the end-of-working-day time, which is either On-Call End, if that
	 *         is specified, or Wrap.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getWorkEnd() {
		if (getOnCallEnd() != null && getOnCallStart() != null) {
			// On-Call End is not valid if On-call Start is not specified!
			return onCallEnd;
		}
		return getWrap();
	}

	/**
	 * @return The amount of time between Wrap and On-Call Start. If any of
	 *         Wrap, On-Call Start, or On-Call End are not specified, this
	 *         returns zero.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getOnCallGap() {
		BigDecimal gap = BigDecimal.ZERO;
		if (getOnCallStart() != null && getOnCallEnd() != null && getWrap() != null) {
			gap = getOnCallStart().subtract(getWrap());
		}
		return gap;
	}

	/** See {@link #hours} */
	@Column(name = "Hours", precision = 4, scale = 2)
	public BigDecimal getHours() {
		return hours;
	}
	/** See {@link #hours} */
	public void setHours(BigDecimal hours) {
		this.hours = hours;
	}

	/**See {@link #worked}. */
	@Column(name = "Worked", nullable = false)
	public boolean getWorked() {
		return worked;
	}
	/**See {@link #worked}. */
	public void setWorked(boolean worked) {
		this.worked = worked;
	}

	/** See {@link #workZone}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Work_Zone")
	public WorkZone getWorkZone() {
		return workZone;
	}
	/** See {@link #workZone}. */
	public void setWorkZone(WorkZone workZone) {
		this.workZone = workZone;
	}

	/** See {@link #dayType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Day_Type")
	public DayType getDayType() {
		return dayType;
	}
	/** See {@link #dayType}. */
	public void setDayType(DayType type) {
		if (type != dayType) {
		}
		dayType = type;
	}

	/** See {@link #phase}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Phase")
	public ProductionPhase getPhase() {
		return phase;
	}
	/** See {@link #phase}. */
	public void setPhase(ProductionPhase phase) {
		this.phase = phase;
	}

	/** See {@link #noStartForm}. */
	@JsonIgnore
	@Column(name = "No_Start_Form", nullable = false)
	public boolean getNoStartForm() {
		return noStartForm;
	}
	/** See {@link #noStartForm}. */
	public void setNoStartForm(boolean noStartForm) {
		this.noStartForm = noStartForm;
	}

//	/** See {@link #opposite} */
//	@Column(name = "Opposite", nullable = false)
//	public boolean getOpposite() {
//		return opposite;
//	}
//	/** See {@link #opposite} */
//	public void setOpposite(boolean opposite) {
//		this.opposite = opposite;
//	}

	/** See {@link #offProduction}. */
	@Column(name = "Off_Production", nullable = false)
	public boolean getOffProduction() {
		return offProduction;
	}
	/** See {@link #offProduction}. */
	public void setOffProduction(boolean offProd) {
		offProduction = offProd;
	}

	/** See {@link #forcedCall}. */
	@Column(name = "Forced_Call", nullable = false)
	public boolean getForcedCall() {
		return forcedCall;
	}
	/** See {@link #forcedCall}. */
	public void setForcedCall(boolean forcedCall) {
		this.forcedCall = forcedCall;
	}

	/** See {@link #nonDeductMeal}. */
	@Column(name = "Non_Deduct_Meal", nullable = false)
	public boolean getNonDeductMeal() {
		return nonDeductMeal;
	}
	/** See {@link #nonDeductMeal}. */
	public void setNonDeductMeal(boolean nonDeductMeal) {
		this.nonDeductMeal = nonDeductMeal;
	}

	/** See {@link #nonDeductMeal}. */
	@Column(name = "Non_Deduct_Meal2", nullable = false)
	public boolean getNonDeductMeal2() {
		return nonDeductMeal2;
	}
	/** See {@link #nonDeductMeal}. */
	public void setNonDeductMeal2(boolean nonDeductMeal) {
		nonDeductMeal2 = nonDeductMeal;
	}

	/**See {@link #nonDeductMealPayroll}. */
	@Column(name = "Non_Deduct_Meal_Payroll", nullable = false)
	public boolean getNonDeductMealPayroll() {
		return nonDeductMealPayroll;
	}
	/**See {@link #nonDeductMealPayroll}. */
	public void setNonDeductMealPayroll(boolean nonDeductMealPayroll) {
		this.nonDeductMealPayroll = nonDeductMealPayroll;
	}

	/**See {@link #nonDeductMeal2Payroll}. */
	@Column(name = "Non_Deduct_Meal2_Payroll", nullable = false)
	public boolean getNonDeductMeal2Payroll() {
		return nonDeductMeal2Payroll;
	}
	/**See {@link #nonDeductMeal2Payroll}. */
	public void setNonDeductMeal2Payroll(boolean nonDeductMeal2Payroll) {
		this.nonDeductMeal2Payroll = nonDeductMeal2Payroll;
	}

	/**See {@link #ndbEnd}. */
	@Column(name = "Ndb_End", precision = 4, scale = 2)
	public BigDecimal getNdbEnd() {
		return ndbEnd;
	}
	/**See {@link #ndbEnd}. */
	public void setNdbEnd(BigDecimal ndbEnd) {
		this.ndbEnd = ndbEnd;
	}

	/** See {@link #ndmStart}. */
	@Column(name = "Ndm_Start", precision = 4, scale = 2)
	public BigDecimal getNdmStart() {
		return ndmStart;
	}
	/** See {@link #ndmStart}. */
	public void setNdmStart(BigDecimal ndmStart) {
		this.ndmStart = ndmStart;
	}

	/** See {@link #ndmEnd} */
	@Column(name = "Ndm_End", precision = 4, scale = 2)
	public BigDecimal getNdmEnd() {
		return ndmEnd;
	}
	public void setNdmEnd(BigDecimal ndmEnd) {
		this.ndmEnd = ndmEnd;
	}

	/** See {@link #lastManIn}. */
	@Column(name = "Last_Man_In", precision = 4, scale = 2)
	public BigDecimal getLastManIn() {
		return lastManIn;
	}
	/** See {@link #lastManIn}. */
	public void setLastManIn(BigDecimal lastManIn) {
		this.lastManIn = lastManIn;
	}

	/** See {@link #mpvUser} */
	@Column(name = "Mpv_User", length = 10)
	public String getMpvUser() {
		return mpvUser;
	}
	/** See {@link #mpvUser} */
	public void setMpvUser(String mpvUser) {
		this.mpvUser = mpvUser;
	}

	/** See {@link #grace1} */
	@Column(name = "Grace1", precision = 3, scale = 2)
	public BigDecimal getGrace1() {
		return grace1;
	}
	/** See {@link #grace1} */
	public void setGrace1(BigDecimal grace1) {
		this.grace1 = grace1;
	}

	/** See {@link #grace2} */
	@Column(name = "Grace2", precision = 3, scale = 2)
	public BigDecimal getGrace2() {
		return grace2;
	}
	/** See {@link #grace2} */
	public void setGrace2(BigDecimal grace2) {
		this.grace2 = grace2;
	}

	/**See {@link #cameraWrap}. */
	@Column(name = "Camera_Wrap", nullable = false)
	public boolean getCameraWrap() {
		return cameraWrap;
	}
	/**See {@link #cameraWrap}. */
	public void setCameraWrap(boolean b) {
		cameraWrap = b;
	}

	/**See {@link #frenchHours}. */
	@Column(name = "French_Hours", nullable = false)
	public boolean getFrenchHours() {
		return frenchHours;
	}
	/**See {@link #frenchHours}. */
	public void setFrenchHours(boolean frenchHours) {
		this.frenchHours = frenchHours;
	}

	/** See {@link #accountLoc} */
	@Column(name = "Location_Code", length = 10)
	public String getAccountLoc() {
		return accountLoc;
	}
	/** See {@link #accountLoc} */
	public void setAccountLoc(String acct) {
		accountLoc = acct;
	}

	/** See {@link #accountMajor}. */
	@Column(name = "Prod_Episode", length = 10)
	public String getAccountMajor() {
		return accountMajor;
	}
	/** See {@link #accountMajor}. */
	public void setAccountMajor(String acct) {
		accountMajor = acct;
	}

	/** See {@link #accountSet}. */
	@Column(name = "Acct_Set", length = 10)
	public String getAccountSet() {
		return accountSet;
	}
	/** See {@link #accountSet}. */
	public void setAccountSet(String acctSet) {
		accountSet = acctSet;
	}

	/** See {@link #accountFree}. */
	@Column(name = "Acct_Free", length = 10)
	public String getAccountFree() {
		return accountFree;
	}
	/** See {@link #accountFree}. */
	public void setAccountFree(String accountFree) {
		this.accountFree = accountFree;
	}

	/** See {@link #reRate} */
	@Column(name = "Re_Rate", nullable = false)
	public boolean getReRate() {
		return reRate;
	}
	/** See {@link #reRate} */
	public void setReRate(boolean reRate) {
		this.reRate = reRate;
	}

	/** See {@link #city}. */
	@Column(name = "City", length = 50)
	public String getCity() {
		return city;
	}
	/** See {@link #city}. */
	public void setCity(String city) {
		this.city = city;
	}

	/** See {@link #state} */
	@Column(name = "State", length = 50)
	public String getState() {
		return state;
	}
	/** See {@link #state} */
	public void setState(String state) {
		this.state = state;
	}

	/** See {@link #country}. */
	@Column(name = "Country", length = 2)
	public String getCountry() {
		return country;
	}
	/** See {@link #country}. */
	public void setCountry(String country) {
		this.country = country;
	}

	/** See {@link #mpv1Payroll} */
	@Column(name = "Mpv1_Payroll")
	public Byte getMpv1Payroll() {
		return mpv1Payroll;
	}
	/** See {@link #mpv1Payroll} */
	public void setMpv1Payroll(Byte mpvPayroll) {
		mpv1Payroll = mpvPayroll;
	}

	/** See {@link #mpv2Payroll} */
	@Column(name = "Mpv2_Payroll")
	public Byte getMpv2Payroll() {
		return mpv2Payroll;
	}
	/** See {@link #mpv2Payroll} */
	public void setMpv2Payroll(Byte mpvPayroll) {
		mpv2Payroll = mpvPayroll;
	}

	/** See {@link #mpv3Payroll}. */
	@Column(name = "Mpv3_Payroll")
	public Byte getMpv3Payroll() {
		return mpv3Payroll;
	}
	/** See {@link #mpv3Payroll}. */
	public void setMpv3Payroll(Byte mpv3Payroll) {
		this.mpv3Payroll = mpv3Payroll;
	}

	/** See {@link #splitByPercent}. */
	@Column(name = "Split_By_Percent", nullable = false)
	public boolean getSplitByPercent() {
		return splitByPercent;
	}
	/** See {@link #splitByPercent}. */
	public void setSplitByPercent(boolean splitByPercent) {
		this.splitByPercent = splitByPercent;
	}

	/** See {@link #jobNum1} */
	@Column(name = "Job_Num1")
	public byte getJobNum1() {
		return jobNum1;
	}
	/** See {@link #jobNum1} */
	public void setJobNum1(byte jobNum1) {
		this.jobNum1 = jobNum1;
	}

	/** See {@link #splitStart2} */
	@Column(name = "Split_Start2",  precision = 5, scale = 2)
	public BigDecimal getSplitStart2() {
		return splitStart2;
	}
	/** See {@link #splitStart2} */
	public void setSplitStart2(BigDecimal splitEnd1) {
		splitStart2 = splitEnd1;
	}

	/** See {@link #jobNum2} */
	@Column(name = "Job_Num2")
	public byte getJobNum2() {
		return jobNum2;
	}
	/** See {@link #jobNum2} */
	public void setJobNum2(byte jobNum2) {
		this.jobNum2 = jobNum2;
	}

	/** See {@link #splitStart3} */
	@Column(name = "Split_Start3", precision = 5, scale = 2)
	public BigDecimal getSplitStart3() {
		return splitStart3;
	}
	/** See {@link #splitStart3} */
	public void setSplitStart3(BigDecimal splitEnd2) {
		splitStart3 = splitEnd2;
	}

	/** See {@link #jobNum3} */
	@Column(name = "Job_Num3")
	public byte getJobNum3() {
		return jobNum3;
	}
	/** See {@link #jobNum3} */
	public void setJobNum3(byte jobNum3) {
		this.jobNum3 = jobNum3;
	}

	/** See {@link #opposite} */
	@Column(name = "Opposite")
	public Boolean getOpposite() {
		return opposite;
	}
	/** See {@link #opposite} */
	public void setOpposite(Boolean opposite) {
		this.opposite = opposite;
	}

	/** See {@link #payAmount}. */
	@Column(name = "Pay_Amount", precision = 7, scale = 2)
	public BigDecimal getPayAmount() {
		return payAmount;
	}
	/** See {@link #payAmount}. */
	public void setPayAmount(BigDecimal payAmount) {
		this.payAmount = payAmount;
	}


	/** See {@link #diableState}. */
	@Column(name = "Disable_State")
	public boolean getDisableState() {
		return disableState;
	}

	/** See {@link #disableState}. */
	public void setDisableState(boolean disableState) {
		this.disableState = disableState;
	}

	/** See {@link #disableCity}. */
	@Column(name = "Disable_City")
	public boolean getDisableCity() {
		return disableCity;
	}

	/** See {@link #disableCity}. */
	public void setDisableCity(boolean disableCity) {
		this.disableCity = disableCity;
	}

	/** See {@link #disableCountry}. */
	@Column(name = "Disable_Country")
	public boolean getDisableCountry() {
		return disableCountry;
	}

	/** See {@link #disableCountry}. */
	public void setDisableCountry(boolean disableCountry) {
		this.disableCountry = disableCountry;
	}

	/** See {@link #selected}. */
	@JsonIgnore
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

//	/**See {@link #holidayOverlap}. */
//	@JsonIgnore
//	@Transient
//	public BigDecimal getHolidayOverlap() {
//		return holidayOverlap;
//	}
//	/**See {@link #holidayOverlap}. */
//	public void setHolidayOverlap(BigDecimal holidayOverlap) {
//		this.holidayOverlap = holidayOverlap;
//	}

	/**See {@link #split1}. */
	@JsonIgnore
	@Transient
	public BigDecimal getSplit1() {
		return split1;
	}
	/**See {@link #split1}. */
	public void setSplit1(BigDecimal split1) {
		this.split1 = split1;
	}

	/**See {@link #split2}. */
	@JsonIgnore
	@Transient
	public BigDecimal getSplit2() {
		return split2;
	}
	/**See {@link #split2}. */
	public void setSplit2(BigDecimal split2) {
		this.split2 = split2;
	}

	/**See {@link #split3}. */
	@JsonIgnore
	@Transient
	public BigDecimal getSplit3() {
		return split3;
	}
	/**See {@link #split3}. */
	public void setSplit3(BigDecimal split3) {
		this.split3 = split3;
	}

	/** See {@link #jobMask}. */
	@JsonIgnore
	@Transient
	public int getJobMask() {
		return jobMask;
	}
	/** See {@link #jobMask}. */
	public void setJobMask(int jobMask) {
		this.jobMask = jobMask;
	}

	/** See {@link #jobSplitGroup}. */
	@JsonIgnore
	@Transient
	public int getJobSplitGroup() {
		return jobSplitGroup;
	}
	/** See {@link #jobSplitGroup}. */
	public void setJobSplitGroup(int jobSplitGroup) {
		this.jobSplitGroup = jobSplitGroup;
	}

	/**
	 * @return True if the DayType's "banner" text should be shown in the
	 *         timecard view. When true, the user will not be able to enter
	 *         hours for this day.
	 */
	@Transient
	public boolean getShowBanner() {
		return (worked || (dayType != null && (dayType.isIdle() || dayType == DayType.HA)));
	}

	@Transient
	public char[] getBanner() {
		char[] text = {' '};
		if (worked) {
			text = DayType.WK.getBanner();
		}
		else if (dayType != null) {
			text = dayType.getBanner();
		}
		return text;
	}

	/** See {@link #phases}. */
	@Transient
	public boolean[] getPhases() {
		phases[0] = false;
		phases[1] = false;
		phases[2] = false;
		if (phase != null) {
			phases[phase.ordinal()] = true;
		}
		return phases;
	}

	/** See {@link #phases}. */
	@Transient
	public void setPhases(boolean[] inphases) {
		phases = inphases;
		if (phase == null || ! phases[phase.ordinal()]) {
			for (ProductionPhase ph : ProductionPhase.values()) {
				if (phases[ph.ordinal()]) {
					phase = ph;
					break;
				}
			}
		}
	}

	/** Converts a double to a decimal with scale of 2.
	 * @param value The double to convert to decimal.
	 * @return The given value as a BigDecimal with scale of 2, using "HALF_UP" rounding.
	 */
	protected static BigDecimal decimal(double value) {
		return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
	}

	/** See {@link #weatherDay}. */
	@Column(name = "Weather_Day")
	public boolean getWeatherDay() {
		return weatherDay;
	}
	/** See {@link #weatherDay}. */
	public void setWeatherDay(boolean weatherDay) {
		this.weatherDay = weatherDay;
	}

	/** See {@link #initimatesDay}. */
	@Column(name = "Intimates_Day")
	public boolean getIntimatesDay() {
		return intimatesDay;
	}
	/** See {@link #intimatesDay}. */
	public void setIntimatesDay(boolean intimatesDay) {
		this.intimatesDay = intimatesDay;
	}

	/** See {@link #comments. */
	@Column(name = "Comments")
	public String getComments() {
		return comments;
	}

	/** See {@link #comments. */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** See {@link #allowWeather. */
	@Transient
	public boolean getAllowWeather() {
		return allowWeather;
	}

	/** See {@link #allowWeather. */
	public void setAllowWeather(boolean allowWeather) {
		this.allowWeather = allowWeather;
	}

	/** See {@link #allowIntimates. */
	@Transient
	public boolean getAllowIntimates() {
		return allowIntimates;
	}

	/** See {@link #allowIntimates. */
	public void setAllowIntimates(boolean allowIntimates) {
		this.allowIntimates = allowIntimates;
	}
}
