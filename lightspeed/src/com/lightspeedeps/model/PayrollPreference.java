package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Type;

import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.Constants;

/**
 * PayrollPreference entity. This stores the payroll-related preferences
 * for either an entire Production (for TV & Features), or for a single
 * Project (for Commercials).
 */
@Entity
@Table(name = "payroll_preference")
public class PayrollPreference extends PersistentObject<PayrollPreference> implements Cloneable {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayrollPreference.class);

	// Fields
	private static final long serialVersionUID = 1;

	// Payroll-production settings
	// *** IMPORTANT - These values need to match the SelectionItem database values! ***

	public static final String DETAIL_TYPE_ONE_HOUR_SERIES = "H1";
	public static final String DETAIL_TYPE_PILOT = "P";
	public static final String DETAIL_TYPE_LONG_FORM = "LF";
	public static final String DETAIL_TYPE_OTHER = "O";
	public static final String DETAIL_TYPE_BASIC = "BA";
	public static final String SEASON_1 = "1";
	public static final String STARTED_2013 = "D";
	public static final String NEW_YORK_REGION = "N";

	/** The earliest valid "week ending" date for a timecard within this production. */
	private Date firstPayrollDate;

	//  * * *  PAYROLL FIELDS  * * *

	/** The payroll service to use for timecard services.  Null if none assigned. */
	private PayrollService payrollService;

	/** The identifier used by the payroll service for this Production. */
	private String payrollProdId;

	// Start Form default values ...

	/** Default "Work City" for new Start Forms. */
	private String workCity;

	/** Default "Work State" for new Start Forms. */
	private String workState;

	/** Zip code (postal code) where work will be done. */
	private String workZip;

	/** Default "Use Work & Overtime rules from State" for new Start Forms. */
	private String overtimeRule = Constants.STATE_WORKED_CODE;

	/** Default "Major Account Code" (Prod/episode#) for new Start Forms. */
	private String accountMajor;

	/** Mileage rate default for Start forms. Will be initialized to current year's
	 * federal mileage reimbursement rate. */
	private BigDecimal mileageRate = BigDecimal.ZERO;

	// Production type ...

	/** Indicates if this is a Major or Independent production. */
	private StudioType studioType;

	/** Indicates if this is a Theater(Film) or TV production. */
	private MediumType mediumType;

	/** Specifies a more detailed definition of the production type, which will
	 * be used to determine which occupation codes and/or rates are presented
	 * on the StartForm. The possible values are stored in the SelectionItem table.
	 * Currently used to distinguish between Pilots, 1/2-hr series, 1-hr series, and
	 * Long-form TV shows, or between Low-budget and regular features. */
	private String detailType;

	/** Indicates which season of a television series this production represents. This
	 * is sometimes used to determine the applicable payroll rates on the StartForm. The
	 * possible values are stored in the SelectionItem table. */
	private String tvSeason;

	/** Indicates the range into which this television series' start date falls. This
	 * is sometimes used to determine the applicable payroll rates on the StartForm.  The
	 * possible values are stored in the SelectionItem table. */
	private String tvEra;

	/** True if show is "Dramatic" as defined by the Videotape agreement; false for "Non-dramatic". */
	private boolean tvDramatic;

	/** True iff show is reality-information-entertainment/magazine format. This flag
	 * is used for the Videotape agreement. */
	private boolean tvRIEM;

	/** True iff show matches "Single Camera" requirement of Videotape agreement. */
	private boolean singleCamera;

	/** True iff show matches "made for Basic Cable" requirement of Videotape agreement. */
	private boolean basicCable;

	/** The production type as it relates to ASA union members. */
	private PayrollProductionType asaContract;

	private HourRoundingType hourRoundingType;

	/** True iff the production is shooting in L.A. or another "Production city".  Used,
	 * in particular, for determining Low Budget Tier 2/3 pay rates. */
	private boolean inProductionCity;

	/** Which region in the East coast area this production is being shot in. */
	private String nyRegion;

	/** The last day that "prep" (pre-production) rates apply. */
	private Date prepEndDate;

	// 		PAYROLL START (StartForm) OPTIONS

	/** If true, include "touring" options for Commercial crew productions.
	 * This includes "Touring rates" and "Workers comp" selection. */
	private boolean includeTouring;

	/** If true, the Model Release (Print) form will be used as the source of
	 * information for generating a StartForm; the StartForm (Payroll Start
	 * form) itself will not be visible as a form type.  LS-4506 */
	private boolean useModelRelease;

	// 		TIMECARD OPTIONS

	/** Which day of the week is the first day on timecards for this project;
	 *  1(Sunday)-7(Saturday).  Default is Sunday (1).  This value may only be
	 *  changed if there are no existing timecards within the associated project. */
	private Integer weekFirstDay = Constants.DEFAULT_WEEK_FIRST_DAY;

	/** Which day number within a timecard is the first work day of the production's
	 * work week. I.e., this is the index (1-7) within dailyTimes of the entry that
	 * matches the 'firstWorkWeekDay'.  If this project's timecards are the usual
	 * Sunday-through-Saturday (weekFirstDay=1), then 'firstWorkDayNum' == 'firstWorkWeekDay'. */
	@Transient
	private Integer firstWorkDayNum = null; // force evaluation on first access. LS-2515

	/** The week-day number (Sun=1, Sat=7) of the first day of the production's
	 * work week.  Specified in Payroll Preferences.  For TV/Feature this is typically
	 * Monday (=2); for Commercial we set it to match the 'weekFirstDay', so that the
	 * production week matches the timecard displayed. LS-2515 */
	private Integer firstWorkWeekDay = Constants.DEFAULT_WEEK_FIRST_DAY; // Prod week = timecard week; LS-2515

	/** True iff LS should automatically create timecards each week for
	 * completed Start Forms. */
	private boolean autoCreateTimecards = true;

	/** How many days in advance of the start of a payroll week the
	 * next week's timecards should be auto-created.  Valid values
	 * are from 0 to 6.  Zero will auto-create timecards on Sunday
	 * morning, 1 on Saturday morning, etc. */
	private byte createTimecardsAdvance = 1;

	/** True iff we should create timecards that are incomplete (missing
	 * work city, state, and SSN). */
	private boolean createIncompleteTimecards = false;

	/** Specifies the number of weeks into the future that is available for
	 * creating a new timecard. Default is one. */
	private byte maxWeeksInAdvance;

	/** True iff LS should automatically mark DayType`s as Holidays (during
	 * timecard creation), based on the internal calendar. */
	private boolean autoMarkHolidays = true;

	/** True iff Job tables and Pay Breakdown info is included with timecard
	 * transfers to the payroll service. */
	private boolean includeBreakdown = true;

	/** NOT CURRENTLY USED (as of 2.2.4731)
	 * When a weekly Box Rental form is NOT submitted AND the Start Form
	 * has a Box Rental amount, then IF this flag is true, add an expense
	 * table item for Box Rental using the Start Form amount. (If the flag
	 * is false, we won't add the expense table entry.) */
	private boolean useStartBoxRental;

	/** True iff employees may create box rental forms. */
	private boolean allowBoxRental;

	/** True iff employees may edit the Expense table. */
	private boolean allowEmployeeExpense = true;

	/** Indicates if payroll service is providing Worker's Comp insurance. Applies
	 * to all employees (Start forms & timecards) attached to the project. */
	private boolean workersComp = false;

	/** Use full HTG processing with union contract rules. When false, only
	 * state and federal rules are followed, and assigned contracts do not
	 * affect HTG calculations. This is always a Production setting, not by Project. */
	private boolean use30Htg = false;

	/** If true, the Full Timecard page will use an alternate style display for
	 * non-union hourly employees; this style includes "On Call" start and end
	 * columns, and eliminates some other columns. This is always a Production setting,
	 * not by Project. */
	private boolean showOnCallFields = false;

	/** When true, the MpvRule field 'guarHoursAfterMeal2' value will be ignored
	 * and will not affect paid hours. When false, that field may effect
	 * HTG calculations.  Typically set to true if a production always pays
	 * for meals. This is always a Production setting, not by Project. */
	private boolean ignoreGuarHrsAfterMeal2 = true;

	/** If true, rate selection when filling out Start Forms will use rates
	 * in effect one year prior to the start date in the Start Form. */
	private boolean usePriorYearRates = false;

	/** True iff LS should calculate the FLSA adjustments as part
	 * of "Auto-pay" processing. */
	private boolean calcFlsa = true;

	/** Determines whether timecards are calculated on Submit. */
	private boolean calcTimecardsOnSubmit;

	/** When true, the timecard creation process will copy the PayJob tables
	 * from the prior week's timecard to the newly-created timecard. */
	private boolean copyJobTables = false;

	/** When true, the timecard creation process will copy the Job split
	 * information from the prior week's timecard to the newly-created timecard. */
	private boolean copyJobSplits = false;

	/** True iff holidays for hourly workers should be paid as they occur.
	 * NOT CURRENTLY VISIBLE IN SETUP; commented out of PayBreakdown code 1/9/15 rev2.9.5055. */
	private boolean payHolidaysAsOccur;

	/** True iff the "Guaranteed" hours field on timecards should be hidden
	 * for non-Union employees. */
	private boolean hideTcGuarantee = true;

	/** True iff the red highlighting of PR/timecard discrepancies should
	 * be turned off on the Approver dashboard. */
	private boolean hidePrDiscrepancy = true;

	// 		BATCH SETTINGS

	/** If true, Batch Transfer actions will result in emails being sent
	 * with attached exports and printouts of timecards. */
	private boolean useEmail;

	/** The email address to use as the recipient of Batch Transfer actions
	 * if the {@link #useEmail} field is true. */
	private String batchEmailAddress;

	/** True iff generated WeeklyBatch names should include a week-ending suffix (or prefix). */
	private boolean includeWeSuffix = true;

	/** True iff the week-ending date is used as a batch prefix instead of suffix. */
	private boolean useWeAsPrefix;

	// Default WTPA Fields

	/** Production company's federal tax Id number. */
	private String fedidNumber;

	/** DBA (doing-business-as) name for production company. */
	private String dba;

	/** The regular pay day, 1=Sunday, 7=Saturday. */
	private Byte regularPayDay;

	/** The normal frequency of paycheck distribution, e.g., weekly,
	 * bi-weekly, etc. */
	private PaydayFrequency paydayFreq;

	/** */
	private String paydayFreqOther;

	/** */
	private Boolean noticeGivenAtHire = null;

	/** Paid Sick Leave */
	private CalifSickLeaveType sickLeaveType;

	private String sickLeaveReason;

	// 		ONBOARDING PREFERENCES

	/**  Mailing address for Onboarding / Preferences mini-tab */
	private Address mailingAddress;

	/** If true, Transfer actions will result in emails being sent
	 * with attached exports and printouts of documents. */
	private boolean useOnboardEmail;

	/** The email address to use as the recipient of Transfer actions
	 * if the {@link #useOnboardEmail} field is true. */
	private String onboardEmailAddress;

	/** The Employer-of-record value to be used when exporting data from
	 * this production to the Team payroll service. */
	private EmployerOfRecord teamEor;

	/** The Pdf grouping type value */
	private PdfGroupingType pdfGroupingType;

	/** Policy to enforce regarding attachments to the Form I-9;
	 * LS-2097: change default value to "FORBID". */
	private ExistencePolicy i9Attachment = ExistencePolicy.FORBID;

	/** Start of Production's "Benefit Year" - for benefits notification form */
	private Date benefitYearStart;

	/** End of Production's "Benefit Year" - for benefits notification form */
	private Date benefitYearEnd;

	/** Default "Work Country" for new Start Forms. */
	private String workCountry = Constants.DEFAULT_COUNTRY_CODE;

	/** If true, premium rate will be used for OT calculation. */
	private boolean usePremiumRate = true;

	/** Preference for payroll period length. Defaults to Weekly.*/
	private PayPeriodType payPeriodType = PayPeriodType.W;

	// Constructors

	/** default constructor */
	public PayrollPreference() {
		calcTimecardsOnSubmit = false;
		pdfGroupingType = PdfGroupingType.NONE;
	}

	// Property accessors

	/** See {@link #firstPayrollDate}. */
	@Column(name = "First_Payroll_Date", length = 0)
	public Date getFirstPayrollDate() {
		return firstPayrollDate;
	}
	/** See {@link #firstPayrollDate}. */
	public void setFirstPayrollDate(Date firstPayrollDate) {
		this.firstPayrollDate = firstPayrollDate;
	}

	/**See {@link #payrollService}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Payroll_Service_Id")
	public PayrollService getPayrollService() {
		return payrollService;
	}
	/**See {@link #payrollService}. */
	public void setPayrollService(PayrollService payrollService) {
		this.payrollService = payrollService;
	}

	/**See {@link #payrollProdId}. */
	@Column(name = "Payroll_Prod_Id", length = 30)
	public String getPayrollProdId() {
		return payrollProdId;
	}
	/**See {@link #payrollProdId}. */
	public void setPayrollProdId(String payrollId) {
		payrollProdId = payrollId;
	}

	/**See {@link #workCity}. */
	@Column(name = "Work_City", length = 50)
	public String getWorkCity() {
		return workCity;
	}
	/**See {@link #workCity}. */
	public void setWorkCity(String payrollWorkCity) {
		workCity = payrollWorkCity;
	}

	/**See {@link #workState}. */
	@Column(name = "Work_State", length = 50)
	public String getWorkState() {
		return workState;
	}
	/**See {@link #workState}. */
	public void setWorkState(String payrollWorkState) {
		workState = payrollWorkState;
	}

	/** See {@link #workZip}. */
	@Column(name = "Work_Zip", length = 10)
	public String getWorkZip() {
		return workZip;
	}
	/** See {@link #workZip}. */
	public void setWorkZip(String workZip) {
		this.workZip = workZip;
	}

	/** See {@link #overtimeRule}. */
	@Column(name = "Overtime_Rule", length = 50)
	public String getOvertimeRule() {
		return overtimeRule;
	}
	/** See {@link #overtimeRule}. */
	public void setOvertimeRule(String overtimeRule) {
		this.overtimeRule = overtimeRule;
	}

	/**See {@link #accountMajor}. */
	@Column(name = "Account_Major", length = 10)
	public String getAccountMajor() {
		return accountMajor;
	}
	/**See {@link #accountMajor}. */
	public void setAccountMajor(String payrollAccountMajor) {
		accountMajor = payrollAccountMajor;
	}

	/** See {@link #mileageRate}. */
	@Column(name = "Mileage_Rate", precision = 4, scale = 3)
	public BigDecimal getMileageRate() {
		return mileageRate;
	}
	/** See {@link #mileageRate}. */
	public void setMileageRate(BigDecimal mileageRate) {
		this.mileageRate = mileageRate;
	}

	/**See {@link #firstWorkWeekDay}. */
	@Column(name = "First_Work_Day", nullable = false)
	public Integer getFirstWorkWeekDay() {
		return firstWorkWeekDay;
	}
	/**See {@link #firstWorkWeekDay}. */
	public void setFirstWorkWeekDay(Integer day) {
		firstWorkDayNum = null; // force recalc
		this.firstWorkWeekDay = day;
	}

	/** See {@link #weekFirstDay}. */
	@Column(name = "Week_First_Day", nullable = false)
	public Integer getWeekFirstDay() {
		return weekFirstDay;
	}
	/** See {@link #weekFirstDay}. */
	public void setWeekFirstDay(Integer weekFirstDay) {
		firstWorkDayNum = null; // force recalc
		this.weekFirstDay = weekFirstDay;
	}

	/** See {@link #calcTimecardsOnSubmit}. */
	@Column(name="Calc_On_Submit", nullable = false)
	public Boolean getCalcTimecardsOnSubmit() {
		return calcTimecardsOnSubmit;
	}

	/** See {@link #calcTimecardsOnSubmit}. */
	public void setCalcTimecardsOnSubmit(Boolean calcTimecardsOnSubmit) {
		this.calcTimecardsOnSubmit = calcTimecardsOnSubmit;
	}

	/**See {@link #firstWorkDayNum}. */
	@Transient
	public Integer getFirstWorkDayNum() {
		if (firstWorkDayNum == null) {
			firstWorkDayNum = firstWorkWeekDay - weekFirstDay + 1;
			if (firstWorkDayNum < 1) {
				firstWorkDayNum += 7;
			}
		}
		return firstWorkDayNum;
	}

	/**
	 * @return The week-day number of the last day of the timecard week.
	 *         Calculated from the {@link #weekFirstDay} value.
	 */
	@Transient
	public Integer getWeekEndDay() {
		int wkEndDay = getWeekFirstDay() + 6;
		if (wkEndDay > 7) {
			wkEndDay -= 7;
		}
		return wkEndDay;
	}

	/**See {@link #studioType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Studio_Type", length = 30)
	public StudioType getStudioType() {
		return studioType;
	}
	/**See {@link #studioType}. */
	public void setStudioType(StudioType studioType) {
		this.studioType = studioType;
	}

	/**See {@link #mediumType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Medium_Type", length = 30)
	public MediumType getMediumType() {
		return mediumType;
	}
	/**See {@link #mediumType}. */
	public void setMediumType(MediumType mediumType) {
		this.mediumType = mediumType;
	}

	/**See {@link #detailType}. */
	@Column(name = "Detail_Type", length = 30)
	public String getDetailType() {
		return detailType;
	}
	/**See {@link #detailType}. */
	public void setDetailType(String productionDetailType) {
		detailType = productionDetailType;
	}

	/**See {@link #tvSeason}. */
	@Column(name = "TV_Season", length = 30)
	public String getTvSeason() {
		return tvSeason;
	}
	/**See {@link #tvSeason}. */
	public void setTvSeason(String productionTvSeason) {
		tvSeason = productionTvSeason;
	}

	/**See {@link #tvEra}. */
	@Column(name = "TV_Era", length = 30)
	public String getTvEra() {
		return tvEra;
	}
	/**See {@link #tvEra}. */
	public void setTvEra(String productionTvEra) {
		tvEra = productionTvEra;
	}

	/** See {@link #tvDramatic}. */
	@Column(name = "Tv_Dramatic", nullable = false)
	public boolean getTvDramatic() {
		return tvDramatic;
	}
	/** See {@link #tvDramatic}. */
	public void setTvDramatic(boolean tvDramatic) {
		this.tvDramatic = tvDramatic;
	}

	/** See {@link #tvRIEM}. */
	@Column(name = "Tv_Riem", nullable = false)
	public boolean getTvRIEM() {
		return tvRIEM;
	}
	/** See {@link #tvRIEM}. */
	public void setTvRIEM(boolean tvRIEM) {
		this.tvRIEM = tvRIEM;
	}

	/** See {@link #singleCamera}. */
	@Column(name = "Single_Camera", nullable = false)
	public boolean getSingleCamera() {
		return singleCamera;
	}
	/** See {@link #singleCamera}. */
	public void setSingleCamera(boolean singleCamera) {
		this.singleCamera = singleCamera;
	}

	/** See {@link #basicCable}. */
	@Column(name = "Basic_Cable", nullable = false)
	public boolean getBasicCable() {
		return basicCable;
	}
	/** See {@link #basicCable}. */
	public void setBasicCable(boolean basicCable) {
		this.basicCable = basicCable;
	}

	/**See {@link #asaContract}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Asa_Contract")
	public PayrollProductionType getAsaContract() {
		return asaContract;
	}
	/**See {@link #asaContract}. */
	public void setAsaContract(PayrollProductionType asaContract) {
		this.asaContract = asaContract;
	}

	/** See {@link #hourRoundingType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "hour_rounding")
	public HourRoundingType getHourRoundingType() {
		return hourRoundingType;
	}
	/** See {@link #hourRoundingType}. */
	public void setHourRoundingType(HourRoundingType timeRounding) {
		hourRoundingType = timeRounding;
	}

	/**See {@link #inProductionCity}. */
	@Column(name = "In_Production_City", nullable = false)
	public boolean getInProductionCity() {
		return inProductionCity;
	}
	/**See {@link #inProductionCity}. */
	public void setInProductionCity(boolean inProductionCity) {
		this.inProductionCity = inProductionCity;
	}

	/**See {@link #nyRegion}. */
	@Column(name = "Ny_Region", length = 30)
	public String getNyRegion() {
		return nyRegion;
	}
	/**See {@link #nyRegion}. */
	public void setNyRegion(String nyRegion) {
		this.nyRegion = nyRegion;
	}

	/**See {@link #prepEndDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Prep_End_Date", length = 0)
	public Date getPrepEndDate() {
		return prepEndDate;
	}
	/**See {@link #prepEndDate}. */
	public void setPrepEndDate(Date prepEndDate) {
		this.prepEndDate = prepEndDate;
	}

	/** See {@link #includeTouring}. */
	@Column(name = "Include_Touring", nullable = false)
	public boolean getIncludeTouring() {
		return includeTouring;
	}
	/** See {@link #includeTouring}. */
	public void setIncludeTouring(boolean includeTouring) {
		this.includeTouring = includeTouring;
	}

	/** See {@link #useModelRelease}. */
	@Column(name = "Use_Model_Release", nullable = false)
	public boolean getUseModelRelease() {
		return useModelRelease;
	}
	/** See {@link #useModelRelease}. */
	public void setUseModelRelease(boolean useModelRelease) {
		this.useModelRelease = useModelRelease;
	}

	/**See {@link #autoCreateTimecards}. */
	@Column(name = "Auto_Create_Timecards", nullable = false)
	public boolean getAutoCreateTimecards() {
		return autoCreateTimecards;
	}
	/**See {@link #autoCreateTimecards}. */
	public void setAutoCreateTimecards(boolean autoCreateTimecards) {
		this.autoCreateTimecards = autoCreateTimecards;
	}

	/**See {@link #createTimecardsAdvance}. */
	@Column(name = "Create_Timecards_Advance", nullable = false)
	public byte getCreateTimecardsAdvance() {
		return createTimecardsAdvance;
	}
	/**See {@link #createTimecardsAdvance}. */
	public void setCreateTimecardsAdvance(byte createTimecardsAdvance) {
		this.createTimecardsAdvance = createTimecardsAdvance;
	}

	/** See {@link #createIncompleteTimecards}. */
	@Column(name = "Create_Incomplete_Timecards", nullable = false)
	public boolean getCreateIncompleteTimecards() {
		return createIncompleteTimecards;
	}
	/** See {@link #createIncompleteTimecards}. */
	public void setCreateIncompleteTimecards(boolean createIncompleteTimecards) {
		this.createIncompleteTimecards = createIncompleteTimecards;
	}

	/** See {@link #maxWeeksInAdvance}. */
	@Column(name = "Max_Weeks_In_Advance", nullable = false)
	public byte getMaxWeeksInAdvance() {
		return maxWeeksInAdvance;
	}
	/** See {@link #maxWeeksInAdvance}. */
	public void setMaxWeeksInAdvance(byte extendCreate) {
		this.maxWeeksInAdvance = extendCreate;
	}

	/** See {@link #autoMarkHolidays}. */
	@Column(name = "Auto_Mark_Holidays", nullable = false)
	public boolean getAutoMarkHolidays() {
		return autoMarkHolidays;
	}
	/** See {@link #autoMarkHolidays}. */
	public void setAutoMarkHolidays(boolean autoFillHolidays) {
		autoMarkHolidays = autoFillHolidays;
	}

	/** See {@link #useEmail}. */
	@Column(name = "Use_Email", nullable = false)
	public boolean getUseEmail() {
		return useEmail;
	}
	/** See {@link #useEmail}. */
	public void setUseEmail(boolean useEmail) {
		this.useEmail = useEmail;
	}

	/** See {@link #batchEmailAddress}. */
	@Column(name = "Batch_Email_Address", length = 100)
	public String getBatchEmailAddress() {
		return batchEmailAddress;
	}
	/** See {@link #batchEmailAddress}. */
	public void setBatchEmailAddress(String batchEmailAddress) {
		this.batchEmailAddress = batchEmailAddress;
	}

	/**See {@link #includeBreakdown}. */
	@Column(name = "Include_Breakdown", nullable = false)
	public boolean getIncludeBreakdown() {
		return includeBreakdown;
	}
	/**See {@link #includeBreakdown}. */
	public void setIncludeBreakdown(boolean payrollIncludeBreakdown) {
		includeBreakdown = payrollIncludeBreakdown;
	}

	/**See {@link #useStartBoxRental}. */
	@Column(name = "Use_Start_Box_Rental", nullable = false)
	public boolean getUseStartBoxRental() {
		return useStartBoxRental;
	}
	/**See {@link #useStartBoxRental}. */
	public void setUseStartBoxRental(boolean payrollIncludeBoxRental) {
		useStartBoxRental = payrollIncludeBoxRental;
	}

	/**See {@link #allowBoxRental}. */
	@Column(name = "Allow_Box_Rental", nullable = false)
	public boolean getAllowBoxRental() {
		return allowBoxRental;
	}
	/**See {@link #allowBoxRental}. */
	public void setAllowBoxRental(boolean payrollAllowBoxRental) {
		allowBoxRental = payrollAllowBoxRental;
	}

	/** See {@link #allowEmployeeExpense}. */
	@Column(name = "Allow_Employee_Expense", nullable = false)
	public boolean getAllowEmployeeExpense() {
		return allowEmployeeExpense;
	}
	/** See {@link #allowEmployeeExpense}. */
	public void setAllowEmployeeExpense(boolean allowEmployeeExpense) {
		this.allowEmployeeExpense = allowEmployeeExpense;
	}

	/** See {@link #workersComp}. */
	@Column(name = "Workers_Comp", nullable = false)
	public boolean getWorkersComp() {
		return workersComp;
	}
	/** See {@link #workersComp}. */
	public void setWorkersComp(boolean workersComp) {
		this.workersComp = workersComp;
	}

	/** See {@link #use30Htg}. */
	@Column(name = "Use_30_Htg", nullable = false)
	public boolean getUse30Htg() {
		return use30Htg;
	}
	/** See {@link #use30Htg}. */
	public void setUse30Htg(boolean use30Htg) {
		this.use30Htg = use30Htg;
	}

	/** See {@link #usePriorYearRates}. */
	@Column(name = "Use_Prior_Year_Rates", nullable = false)
	public boolean getUsePriorYearRates() {
		return usePriorYearRates;
	}
	/** See {@link #usePriorYearRates}. */
	public void setUsePriorYearRates(boolean usePriorYearRates) {
		this.usePriorYearRates = usePriorYearRates;
	}

	/** See {@link #showOnCallFields}. */
	@Column(name = "Show_On_Call_Fields", nullable = false)
	public boolean getShowOnCallFields() {
		return showOnCallFields;
	}
	/** See {@link #showOnCallFields}. */
	public void setShowOnCallFields(boolean showOnCallFields) {
		this.showOnCallFields = showOnCallFields;
	}

	/** See {@link #ignoreGuarHrsAfterMeal2}. */
	@Column(name = "Ignore_Guar_Hrs_After_Meal2", nullable = false)
	public boolean getIgnoreGuarHrsAfterMeal2() {
		return ignoreGuarHrsAfterMeal2;
	}
	/** See {@link #ignoreGuarHrsAfterMeal2}. */
	public void setIgnoreGuarHrsAfterMeal2(boolean b) {
		ignoreGuarHrsAfterMeal2 = b;
	}

	/** See {@link #calcFlsa}. */
	@Column(name = "Calc_Flsa", nullable = false)
	public boolean getCalcFlsa() {
		return calcFlsa;
	}
	/** See {@link #calcFlsa}. */
	public void setCalcFlsa(boolean calcFlsa) {
		this.calcFlsa = calcFlsa;
	}

	/**See {@link #payHolidaysAsOccur}. */
	@Column(name = "Pay_Holidays_As_Occur", nullable = false)
	public boolean getPayHolidaysAsOccur() {
		return payHolidaysAsOccur;
	}
	/**See {@link #payHolidaysAsOccur}. */
	public void setPayHolidaysAsOccur(boolean payrollPayHolidaysAsOccur) {
		payHolidaysAsOccur = payrollPayHolidaysAsOccur;
	}

	/**See {@link #includeWeSuffix}. */
	@Column(name = "Include_We_Suffix", nullable = false)
	public boolean getIncludeWeSuffix() {
		return includeWeSuffix;
	}
	/**See {@link #includeWeSuffix}. */
	public void setIncludeWeSuffix(boolean payrollIncludeWeSuffix) {
		includeWeSuffix = payrollIncludeWeSuffix;
	}

	/**See {@link #useWeAsPrefix}. */
	@Column(name = "Use_We_As_Prefix", nullable = false)
	public boolean getUseWeAsPrefix() {
		return useWeAsPrefix;
	}
	/**See {@link #useWeAsPrefix}. */
	public void setUseWeAsPrefix(boolean b) {
		useWeAsPrefix = b;
	}

	/**See {@link #hideTcGuarantee}. */
	@Column(name = "Hide_Tc_Guarantee", nullable = false)
	public boolean getHideTcGuarantee() {
		return hideTcGuarantee;
	}
	/**See {@link #hideTcGuarantee}. */
	public void setHideTcGuarantee(boolean showTcGuarantee) {
		hideTcGuarantee = showTcGuarantee;
	}

	/**See {@link #hidePrDiscrepancy}. */
	@Column(name = "Hide_Pr_Discrepancy", nullable = false)
	public boolean getHidePrDiscrepancy() {
		return hidePrDiscrepancy;
	}
	/**See {@link #hidePrDiscrepancy}. */
	public void setHidePrDiscrepancy(boolean showPrDiscrepancy) {
		hidePrDiscrepancy = showPrDiscrepancy;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Fedid_Number", nullable = true, length = 1000)
	public String getFedidNumber() {
		return fedidNumber;
	}
	public void setFedidNumber(String fedidNumber) {
		this.fedidNumber = fedidNumber;
	}

	@Column(name = "Dba", nullable = true, length = 100)
	public String getDba() {
		return dba;
	}
	public void setDba(String dba) {
		this.dba = dba;
	}

	@Column(name = "Regular_Pay_Day" , nullable = true)
	public Byte getRegularPayDay() {
		return regularPayDay;
	}
	public void setRegularPayDay(Byte regularPayDay) {
		this.regularPayDay = regularPayDay;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Payday_Freq" , nullable = true, length = 10)
	public PaydayFrequency getPaydayFreq() {
		return paydayFreq;
	}
	public void setPaydayFreq(PaydayFrequency paydayFreq) {
		this.paydayFreq = paydayFreq;
	}

	@Column(name = "Payday_Freq_Other" , nullable = true, length = 30)
	public String getPaydayFreqOther() {
		return paydayFreqOther;
	}
	public void setPaydayFreqOther(String paydayFreqOther) {
		this.paydayFreqOther = paydayFreqOther;
	}

	@Column(name = "Notice_Given_At_Hire" , nullable = true)
	public Boolean getNoticeGivenAtHire() {
		return noticeGivenAtHire;
	}
	public void setNoticeGivenAtHire(Boolean noticeGivenAtHire) {
		this.noticeGivenAtHire = noticeGivenAtHire;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Sick_Leave_Type" , nullable = true, length = 10)
	public CalifSickLeaveType getSickLeaveType() {
		return sickLeaveType;
	}
	public void setSickLeaveType(CalifSickLeaveType sickLeaveType) {
		this.sickLeaveType = sickLeaveType;
	}

	@Column(name = "Sick_Leave_Reason" , nullable = true, length = 100)
	public String getSickLeaveReason() {
		return sickLeaveReason;
	}
	public void setSickLeaveReason(String sickLeaveReason) {
		this.sickLeaveReason = sickLeaveReason;
	}

	/** See {@link #mailingAddress}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Mailing_Address_Id")
	public Address getMailingAddress() {
		return mailingAddress;
	}
	/** See {@link #mailingAddress}. */
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}



//	@OneToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Approver_Id")
//	public Approver getApprover() {
//		return approver;
//	}
//	public void setApprover(Approver approver) {
//		this.approver = approver;
//	}

	/** See {@link #useOnboardEmail}. */
	@Column(name = "Use_Onboard_Email", nullable = false)
	public boolean isUseOnboardEmail() {
		return useOnboardEmail;
	}
	/** See {@link #useOnboardEmail}. */
	public void setUseOnboardEmail(boolean useOnboardEmail) {
		this.useOnboardEmail = useOnboardEmail;
	}

	/** See {@link #onboardEmailAddress}. */
	@Column(name = "Onboard_Email_Address", length = 100)
	public String getOnboardEmailAddress() {
		return onboardEmailAddress;
	}
	/** See {@link #onboardEmailAddress}. */
	public void setOnboardEmailAddress(String onboardEmailAddress) {
		this.onboardEmailAddress = onboardEmailAddress;
	}

	/** See {@link #copyJobTables}. */
	@Column(name = "Copy_Job_Tables", nullable = false)
	public boolean getCopyJobTables() {
		return copyJobTables;
	}
	/** See {@link #copyJobTables}. */
	public void setCopyJobTables(boolean copyJobTables) {
		this.copyJobTables = copyJobTables;
	}

	/** See {@link #copyJobSplits}. */
	@Column(name = "Copy_Job_Splits", nullable = false)
	public boolean getCopyJobSplits() {
		return copyJobSplits;
	}
	/** See {@link #copyJobSplits}. */
	public void setCopyJobSplits(boolean copyJobSplits) {
		this.copyJobSplits = copyJobSplits;
	}

	/** See {@link #teamEor}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Team_Eor", length = 20)
	public EmployerOfRecord getTeamEor() {
		return teamEor;
	}
	/** See {@link #teamEor}. */
	public void setTeamEor(EmployerOfRecord teamEor) {
		this.teamEor = teamEor;
	}

	/** See {@link #pdfGroupingType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pdf_group", length = 20)
	public PdfGroupingType getPdfGroupingType() {
		return pdfGroupingType;
	}
	/** See {@link #pdfGroupingType}. */
	public void setPdfGroupingType(PdfGroupingType pdfGroupingType) {
		this.pdfGroupingType = pdfGroupingType;
	}

	/** See {@link #i9Attachment}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "I9_Attachment", length = 20)
	public ExistencePolicy getI9Attachment() {
		return i9Attachment;
	}
	/** See {@link #i9Attachment}. */
	public void setI9Attachment(ExistencePolicy i9Attachment) {
		this.i9Attachment = i9Attachment;
	}

	/** See {@link #benefitYearEnd}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Benefit_Year_End")
	public Date getBenefitYearEnd() {
		return benefitYearEnd;
	}
	/** See {@link #benefitYearEnd}. */
	public void setBenefitYearEnd(Date benefitYearEnd) {
		this.benefitYearEnd = benefitYearEnd;
	}

	/** See {@link #benefitYearStart}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Benefit_Year_Start")
	public Date getBenefitYearStart() {
		return benefitYearStart;
	}
	/** See {@link #benefitYearStart}. */
	public void setBenefitYearStart(Date benefitYearStart) {
		this.benefitYearStart = benefitYearStart;
	}

	/**See {@link #workCountry}. */
	@Column(name = "Work_Country", length = 50)
	public String getWorkCountry() {
		return workCountry;
	}
        /**See {@link #workCountry}. */
	public void setWorkCountry(String workCountry) {
		this.workCountry = workCountry;
	}

	/** See {@link #usePremiumRate}. */
	@Column(name = "Use_Premium_Rate", nullable = false)
	public boolean getUsePremiumRate() {
		return usePremiumRate;
	}

	/** See {@link #usePremiumRate}. */
	public void setUsePremiumRate(boolean usePremiumRate) {
		this.usePremiumRate = usePremiumRate;
	}

	/** See {@link #payPeriodType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Pay_Period_Type", nullable = true, length = 10)
	public PayPeriodType getPayPeriodType() {
		return payPeriodType;
	}

	/** See {@link #payPeriodType}. */
	public void setPayPeriodType(PayPeriodType payPeriodType) {
		this.payPeriodType = payPeriodType;
	}

	/**
	 * @return True iff the given date is within the "prep" period for this
	 *         production, i.e., it is before or equal to the {@link #prepEndDate}.
	 *         Returns false if the prepEndDate is not set.
	 */
	@Transient
	public boolean getInPrep(Date date) {
		if (getPrepEndDate() == null) {
			return false;
		}
		return ! date.after(getPrepEndDate());
	}

	@Override
	public PayrollPreference clone() {
		PayrollPreference pref;
		try {
			pref = (PayrollPreference)super.clone();
			pref.id = null;
			pref.mailingAddress = null;
		}
		catch (CloneNotSupportedException e) {
			//log.error(e);
			return null;
		}
		return pref;
	}

	/**
	 * @return A copy of this object, including separate copies of all the
	 *         included data objects, such as Address. (This is
	 *         significantly different than the clone() method, which copies
	 *         only the primitive data items, and all referenced objects
	 *         are null in the returned copy.)
	 */
	public PayrollPreference deepCopy() {
		PayrollPreference newPref = clone();
		if (mailingAddress != null) {
			newPref.setMailingAddress(mailingAddress.clone());
		}
		return newPref;
	}

}
