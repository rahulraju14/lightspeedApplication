package com.lightspeedeps.model;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;

import javax.faces.model.SelectItem;
import javax.persistence.*;

import org.apache.commons.logging.*;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.lightspeedeps.dao.*;
import com.lightspeedeps.port.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.util.payroll.OccupationUtils;

/**
 * WeeklyTimecard entity. This represents the payroll timecard for one person
 * for one week's work with a given role. A User may have only one of these per
 * Production for a given week for each StartForm they have in that Production.
 */

@Entity
@Table(name = "weekly_time_card")
public class WeeklyTimecard extends Approvable implements Comparable<WeeklyTimecard>, Cloneable {
	//@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyTimecard.class);

	private static final long serialVersionUID = 1L;

	/** Key to sort by date, adjust flag, last-name, first-name (the default compareTo sequence). */
	public static final String SORTKEY_DATE = "date";

	/** key to sort by descending date & ascending occupation; this is used for
	 * the mobile timecard list. */
	public static final String SORTKEY_DATE_JOB = "date_job";

	/** Key to sort by <last name>, <first name>, <date (reversed)>, adjust flag, occupation */
	public static final String SORTKEY_NAME = "name";

	/** Key to sort by the status field; this will be in the ordinal order,
	 * i.e., whatever order the WeeklyStatus enumeration is defined in.  Equal status
	 * values will then sort by the default (date, adjust, name). */
	public static final String SORTKEY_STATUS = "status";

	/** Key to sort by hours (ascending) and date (ascending) */
	public static final String SORTKEY_HOURS = "hours";

	/** Key to sort by Job Number (Commercial productions) */
	public static final String SORTKEY_JOB_NAME = "jobName";

	/** Key to sort by Job Number (Commercial productions) */
	public static final String SORTKEY_JOB_NUMBER = "jobNumber";

	/** Key to sort by all Account codes, in order of importance. */
	public static final String SORTKEY_ACCT = "acct";

	/** Key to sort by "Major" Account code, also referred to as "Prod/Episode#". */
	public static final String SORTKEY_MAJOR = "major";

	/** Key to sort by "Detail" Account code */
	public static final String SORTKEY_DTL = "dtl";

	/** Key to sort by "Sub" Account code */
	public static final String SORTKEY_SUB = "sub";

	/** Key to sort by "Set" Account code */
	public static final String SORTKEY_SET = "set";

	/** Key to sort by Account codes */
	public static final String SORTKEY_FREE = "free";

	/** Key to sort by Batch name */
	public static final String SORTKEY_BATCH = "batch";

	/** Key to sort by SSN */
	public static final String SORTKEY_SSN = "ssn";

	/** Key to sort by occupation name */
	public static final String SORTKEY_OCCUPATION = "occupation";

	/** Key to sort by occupation code */
	public static final String SORTKEY_OCC_CODE = "occCode";

	/** Key to sort by department name */
	public static final String SORTKEY_DEPT = "dept";

	/** Key to sort by Union number */
	public static final String SORTKEY_UNION = "union";

	/** Key to sort by Payroll Status */
	public static final String SORTKEY_PAY_STATUS = "payStatus";

	/** Key to sort by total gross pay */
	public static final String SORTKEY_GROSS = "gross";

	/** Key to sort by who the timecard is waiting for; handled by
	 * {@link com.lightspeedeps.object.TimecardEntry}, since that's where this
	 * "transient" value is kept. */
	public static final String SORTKEY_WAITING_FOR = "waiting";

	public static final String RATE_TYPE_STUDIO_TEXT = "Studio";
	public static final String RATE_TYPE_LOCATION_TEXT = "Distant";

	public static final SelectItem[] RATE_TYPE_SELECTION = {
			new SelectItem(StartForm.USE_STUDIO_RATE, WeeklyTimecard.RATE_TYPE_STUDIO_TEXT),
			new SelectItem(StartForm.USE_LOCATION_RATE, WeeklyTimecard.RATE_TYPE_LOCATION_TEXT) };


	// Fields

	/** Lightspeed account# (User.account) */
	private String userAccount;

	/** week ending date */
	private Date endDate;

	/** Used when transmitting the timecard in a batch to indicate if it is
	 * New, Updated, or Removed. */
	@Transient
	private String batchStatus = "";
	public final static String BATCH_STATUS_NEW = "N";
	public final static String BATCH_STATUS_SAME = "S";
	public final static String BATCH_STATUS_UPDATED = "U";
	public final static String BATCH_STATUS_REMOVED = "R";

	/** LS-1140: department whose timecards will have their straight-time hours output with a different pay-code */
	private static final String PB_MAPPED_DEPT = "cast";
	/** LS-1140: The replacement pay-code to be used for straight-time hours (normally X10_HOURS). */
	public static final String PB_MAPPED_STRAIGHT_TIME_CODE = "SESDAY";

	/** Enum WeeklyStatus (Open, submitted, etc) *//*
	private ApprovalStatus status;*/

	/** This is an "adjusted" timecard, which means there should be another one for
	 * the same week-end date and occupation (start form). */
	private boolean adjusted;

	/** True iff "HTG" data is locked (not available for editing) */
	private boolean htgLocked;

	/** True iff the approver in whose queue this timecard resides has marked
	 * the timecard "ready for approval".  This flag is reset to false
	 * whenever the timecard is removed from its current queue. */
	private boolean markedForApproval;

	/** True iff the employee's total reported hours differs from the
	 * total number of hours in the PayJob tables. */
	private boolean jobHoursDiffer;

	/** Last time this timecard was updated (saved). */
	private Date updated;

	/** The time this timecard was sent to the payroll service. This will be null
	 * if the timecard has not been transmitted yet. */
	private Date timeSent;

	/** The time this timecard was marked in payroll Edit status. This will be null
	 * if the timecard has not reached payroll edit status. */
	private Date timeEdit;

	/** The time this timecard was marked as Final. This will be null
	 * if the timecard has not reached Final status. */
	private Date timeFinal;

	/** The time this timecard was marked as Paid. This will be null
	 * if the timecard has not reached Paid status.*/
	private Date timePaid;

	/** The associated StartForm, which was used to initialize this timecard. */
	private StartForm startForm;

	/** The weekly timecard batch in which this timecard resides, if any.
	 * If null, this timecard is "unbatched". */
	private WeeklyBatch weeklyBatch;

	/** database id of the Approver identifying the next person to approve
	 * this timecard.  This will be null if (a) the timecard has not been submitted,
	 * (b) was rejected back to the employee, or (c) has gotten final approval. *//*
	private Integer approverId;*/

	/** The person's last name. Copied from the StartForm. */
	private String lastName;

	/** The person's first name. Copied from the StartForm. */
	private String firstName;

	/** The person's SSN (encrypted). Copied from the StartForm. */
	private String socialSecurity;

	/** The person's unique payroll id, other than SSN; not currently used. */
	private String payrollId;

	/** true if user signed a paper time card */
	private boolean signedPaper;

	/** foreign key to Image table, normally used to hold picture of
	 * scanned timecard, when a paper timecard was submitted. */
	private Image paperImage;

	/** The person's loan-out corporation name. Copied from the StartForm. */
	private String loanOutCorp;

	/** Lightspeed production id (Production.prodId) */
	private String prodId = "SYS";

	/** The production name. Copied from the StartForm. */
	private String prodName;

	/** The production company name. Copied from the StartForm. */
	private String prodCo;

	/** Job (project) name (for Commercials) */
	private String jobName;

	/** Job (project) number (for Commercials) */
	private String jobNumber;

	/** Person's department, taken from the StartForm that was used when
	 * creating this timecard.
	 * @see StartForm#department */
	private Department department;

	/** Allows direct access to the Department.id field.
	 * @see StartForm#department */
	private Integer departmentId;

	/** Name of person's normal department; maintained so the timecard
	 * could be presented independently of the production's (or LS) department table. */
	private String deptName;

	/** True iff user's job is off-production. Copied from the StartForm. */
	private boolean offProduction;

	/** True if user has signed contract; TC process will skip dept approval.
	 * Copied from the StartForm. */
	private boolean underContract;

	/** If true, employee may check "worked" w/o entering hours */
	private boolean allowWorked;

	/** Retirement plan code employee contributes to, if any. Codes are maintained
	 * in the SelectionItem table, with a type of "RETIRE". */
	private String retirementPlan;

	/** The person's occupation, also called Job Classification. For union employees,
	 * this should be the "official" union position name.  Copied from the StartForm. */
	private String occupation;

	/** The person's occupation code.  Copied from the StartForm. */
	private String occCode;

	/** The Lightspeed-assigned Occupation Code for a specific occupation. These
	 * codes will match {@link #occCode} value unless that would cause a duplication.
	 * Also, Lightspeed may assign occupation codes for occupations that do not
	 * have an assigned code, e.g., occupations from the DGA contract. */
	private String lsOccCode;

	/** Which state's overtime rules will apply. The value Constants.STATE_WORKED_CODE
	 * indicates to use the WorkState (either default, or DailyTime override). */
	private String wageState;

	/** The person's union number. Copied from the StartForm. */
	private String unionNumber;

	/** Work ending date, for ACA. */
	private Date acaEndWorkDate;
	/** ACA Special unpaid leave start date */

	private Date acaLeaveStartDate;
	/** ACA Special unpaid leave end date */

	private Date acaLeaveEndDate;

	/** Rates in effect: 'S' = Studio, 'L' = Location. Copied from the
	 * {@link StartForm#useStudioOrLoc} field. */
	private String rateType = StartForm.USE_STUDIO_RATE;

	/** Indicates if employee's pay is hourly, daily or weekly. */
	private EmployeeRateType employeeRateType;

	/** hourly or weekly rate.  Currency. Copied from the StartForm. */
	private BigDecimal rate;

	/** Hourly pay rate; currency. Copied from the StartForm. */
	private BigDecimal hourlyRate;

	/** Daily pay rate; currency. Copied from the StartForm. */
	private BigDecimal dailyRate;

	/** Weekly pay rate; currency. Copied from the StartForm. */
	private BigDecimal weeklyRate;

	/** guaranteed hours. Copied from the StartForm. */
	private BigDecimal guarHours;

	/** The state worked, typically 2-character postal code. The value applies to
	 * all days, but may be overridden on a daily bases by the {@link DailyTime#state}
	 * value. */
	private String stateWorked;

	/** city worked */
	private String cityWorked;

	/** Zip code (postal code) where work was done. */
	private String workZip;

	/** Country worked */
	private String workCountry;

	/** federal corporation id number */
	private String fedCorpId;

	/** state corporation id number */
	private String stateCorpId;

	/** The entire set of account codes for the PayJob */
	private AccountCodes account;

	/** free format text field */
	private String comments;

	/** free format text field */
	private String privateComments;

//	/** Total dollars assigned to Prep phase. */
//	private BigDecimal totalPrep;
//
//	/** Total dollars assigned to Shoot phase. */
//	private BigDecimal totalShoot;
//
//	/** Total dollars assigned to Wrap phase. */
//	private BigDecimal totalWrap;

	/** Adjustment grand total $ -- in Tours productions, this
	 * is used for the Total Wages: sum of daily pay amounts,
	 * but not including per diem/advance fields.  Not used
	 * for non-Tours productions. */
	private BigDecimal adjGtotal;

	/** Timecard total Wages for first 15 days for monthly timesheet $ */
	private BigDecimal totalWages1;

	/** Timecard total Wages for remaining days after first 15 days, for monthly timesheet $ */
	private BigDecimal totalWages2;

	/** Advance grand total $ - not currently used. */
	private BigDecimal advGtotal;

	/** Timecard grand total $ */
	private BigDecimal grandTotal;

	/** Total work hours for the week; displayed on several pages. */
	private BigDecimal totalHours;

	/** Total paid hours for the week; displayed on some dashboards. */
	private BigDecimal totalPaidHours;

	/** The last non-zero worked-day number (DailyTime.workDayNum) within this week; used
	 * to calculate worked-day numbers particularly when looking back to a prior week. Any
	 * value over 7 indicates uninitialized (not yet computed). */
	private byte lastWorkDayNum;

	/** The day number (1-7) within this employee's latest (rolling) work week
	 * that corresponds to the last day on the timecard (Saturday). For example,
	 * if the employee's work week (in this timecard) started on Wednesday, this
	 * value would be 4, regardless of the number of days worked since Wednesday.
	 * This value will be zero if the person reached the end of the prior rolling
	 * week, and did not have any worked days after that; for example, Thursday
	 * was the 7th (last) day of the prior rolling week, and they did not work
	 * Friday or Saturday. */
	private byte endingDayNum;

	/** Whether employee is to paid as an Individual or as a Loan-out. LS-2562 */
	private PaidAsType paidAs;

	/** List of events that have affected this timecard, e.g., approvals and rejections. */
	private List<TimecardEvent> timecardEvents = new ArrayList<>(0);

	/** List of DailyTime - daily hour and related info; always has 7 entries.
	 * Index 0 entry is 1st day of payroll week = Sunday; index 6 entry = Saturday.*/
	private List<DailyTime> dailyTimes = new ArrayList<>(0);

	/** List of PayJob entries - job breakdown. This will usually be empty until
	 * the HTG process begins; then it typically has a small number of entries,
	 * in the 1-3 range. A single entry is the most common case; a calculated
	 * timecard must have at least one entry. */
	private List<PayJob> payJobs = new ArrayList<>(0);

	/** List of PayBreakdown detail lines.  This will usually be empty until
	 * the HTG process begins; then it typically has a small number of entries,
	 * in the 5-10 range. */
	private List<PayBreakdown> payLines = new ArrayList<>(0);

	/** List of PayBreakdown detail lines, broken down by date as well as
	 * category/rate. This will usually be empty until the HTG process begins;
	 * then it typically has a small number of entries, in the 10-20 range. This
	 * table is typically displayed only to super-admin users. */
	private List<PayBreakdownDaily> payDailyLines = new ArrayList<>(0);

	/** A list of ExpenseLine items re-packaging a number of related
	 * individual fields from the WeeklyTimecard. */
	private List<PayExpense> expenseLines = new ArrayList<>(0);

	/** Mileage form corresponding to this timecard. */
	@JsonInclude(Include.NON_NULL)
	private Mileage mileage;

	/** Box Rental form corresponding to this timecard. */
	@JsonInclude(Include.NON_NULL)
	private BoxRental boxRental;

	/** List of Attachments for the corresponding Timecard. */
	private Set<Attachment> attachments = new HashSet<>(0);

	// * * * TRANSIENT FIELDS * * *

	/** True iff at least one day has "worked" checked, and no specific hours
	 * were reported for any day. This can only be true if {@link #allowWorked}
	 * is true. */
	@Transient
	private boolean allExempt;

	/** Total pay from "daily" split pay breakdown line items, {@link #payDailyLines}. */
	@Transient
	private BigDecimal grandDailyTotal;

	/** The 'Employer-of-record' (EOR) code for this timecard; determined from
	 * Payroll Start (StartForm) settings. */
	@Transient
	private String teamEor;

	@Transient
	private boolean mayUseOnCall;

	/** Total hours for the week from the DPR; displayed on the Approver
	 * Dashboard, in the DPR report section. */
	@Transient
	private BigDecimal totalDprHours;

	/** Total of MPV-1s for the week from the DPR; displayed on the Approver
	 * Dashboard, in the DPR report section. */
	@Transient
	private short totalDprMpv1;

	/** Total of MPV-2s for the week from the DPR; displayed on the Approver
	 * Dashboard, in the DPR report section. */
	@Transient
	private short totalDprMpv2;

	/** Total of all payroll MPV-1 values for the week. Displayed on the Dashboard. */
	@Transient
	private short totalMpv1;

	/** Total of all payroll MPV-2 values for the week. Displayed on the Dashboard. */
	@Transient
	private short totalMpv2;

	/** Total of all payroll MPV-3 values for the week. May be displayed on the Dashboard. */
	@Transient
	private short totalMpv3;

	@Transient
	private short totalMpvUser;

	/** The formatted version of {@link #timeSent} */
	@Transient
	private String timeSentFmtd = null;

	/** The formatted version of {@link #updated} */
	@Transient
	private String updatedFmtd = null;


	/** Pay breakdown lines deleted by the user in the current edit cycle.  These
	 * will be deleted from the database when the user does a Save. */
	@Transient
	private List<PayBreakdownMapped> deletedPayLines = new ArrayList<>(0);

//	/** Expense lines deleted by the user in the current edit cycle.  These
//	 * will be deleted from the database when the user does a Save. */
//	@Transient
//	private List<PayExpense> deletedExpenseLines = new ArrayList<PayExpense>();

	/** Used to hold the daily total hours worked, for display on the
	 * Approver Dashboard - Timecard Review tab. A negative value indicates that
	 * "Worked" was checked for that day (for an exempt employee). */
	@Transient
	private BigDecimal[] dailyHours;

	/** The starting date of this WeeklyTimecard. Calculated from the endDate. */
	@Transient
	private Date startDate;

	/** The day number (1=Sunday, 7=Saturday) of the last day on this timecard.
	 * Typically 7 (Sat) unless the Production or Project has changed the "Start of week"
	 * payroll preference. */
	@Transient
	private Integer weekEndDay;

	/** Used to display secure form of SSN (last 4 digits) */
	@Transient
	private String viewSSN;

	/** Database id of the Contact who is the next person to approve
	 * this timecard. null if not yet set (due to transient nature),
	 * or if approverId is null. *//*
	@Transient
	private Integer approverContactId;*/

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** Used to track status during an import operation. */
	@Transient
	@JsonIgnore
	private ImportStatus importStatus;

	/** Used to hold the daily pay amount, for use on the
	 * Tours Timesheet page. */
	@Transient
	private BigDecimal[] dailyAmounts;

	@Transient
	private List<SelectItem> cities = null;

	// Constructors
	/** default constructor */
	public WeeklyTimecard() {
	}

	/**
	 * Constructor to set non-null fields.
	 *
	 * @param date The week-ending date for this timecard.
	 */
	public WeeklyTimecard(Date date) {
		// set fields that are required to be non-null
		setEndDate(date);
		setProdId("SYS");
		setUserAccount("");
		setStatus(ApprovalStatus.OPEN);
		setFirstName("");
		setLastName("");
	}

	/**
	 *
	 * @see com.lightspeedeps.model.PersistentObject#getObjectType()
	 */
	@Override
	@Transient
	public ObjectType getObjectType() {
		return ObjectType.WT;
	}

	/** See {@link #userAccount}. */
	@Column(name = "User_Account", nullable = false, length = 20)
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	/** See {@link #endDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", nullable = false, length = 10)
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #status}. *//*
	@Override
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public ApprovalStatus getStatus() {
		return status;
	}
	@Override
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}*/

	/** See {@link #adjusted}. */
	@Column(name = "Adjusted", nullable = false)
	public boolean getAdjusted() {
		return adjusted;
	}
	/** See {@link #adjusted}. */
	public void setAdjusted(boolean adjusted) {
		this.adjusted = adjusted;
	}

	/** See {@link #htgLocked}. */
	@JsonIgnore
	@Column(name = "Htg_Locked", nullable = false)
	public boolean getHtgLocked() {
		return htgLocked;
	}
	/** See {@link #htgLocked}. */
	public void setHtgLocked(boolean htgLocked) {
		this.htgLocked = htgLocked;
	}

	/**See {@link #markedForApproval}. */
	@JsonIgnore
	@Column(name = "Marked_For_Approval", nullable = false)
	public boolean getMarkedForApproval() {
		return markedForApproval;
	}
	/**See {@link #markedForApproval}. */
	public void setMarkedForApproval(boolean markedForApproval) {
		this.markedForApproval = markedForApproval;
	}

	/**See {@link #jobHoursDiffer}. */
	@JsonIgnore
	@Column(name = "Job_Hours_Differ", nullable = false)
	public boolean getJobHoursDiffer() {
		return jobHoursDiffer;
	}
	/**See {@link #jobHoursDiffer}. */
	public void setJobHoursDiffer(boolean jobHoursDiffer) {
		this.jobHoursDiffer = jobHoursDiffer;
	}

	/**See {@link #updated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return updated;
	}
	/**See {@link #updated}. */
	public void setUpdated(Date updated) {
		this.updated = updated;
		updatedFmtd = null;
	}

	@Transient
	public String getUpdatedFmtd() {
		if (updatedFmtd == null) {
			DateFormat df = new SimpleDateFormat("M/d h:mm a");
			df.setTimeZone(SessionUtils.getProduction().getTimeZone());
			updatedFmtd = df.format(updated);
		}
		return updatedFmtd;
	}

	/**See {@link #batchStatus}. */
	@Transient
	public String getBatchStatus() {
		return batchStatus;
	}
	/**See {@link #batchStatus}. */
	public void setBatchStatus(String batchStatus) {
		this.batchStatus = batchStatus;
	}

	/**See {@link #timeSent}. */
	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Sent", length = 0)
	public Date getTimeSent() {
		return timeSent;
	}
	/**See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
		timeSentFmtd = null;
	}

	@Transient
	public String getTimeSentFmtd() {
		if (timeSentFmtd == null) {
			if (timeSent != null) {
				DateFormat df = new SimpleDateFormat(Constants.TRANSFER_TOOLTIP_SENT_DATE_FORMAT);
				df.setTimeZone(SessionUtils.getProduction().getTimeZone());
				timeSentFmtd = df.format(timeSent);
			}
			else {
				timeSentFmtd = "(not sent)";
			}
		}
		return timeSentFmtd;
	}

	/**See {@link #timeEdit}. */
	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Edit", length = 0)
	public Date getTimeEdit() {
		return timeEdit;
	}
	/**See {@link #timeEdit}. */
	public void setTimeEdit(Date timeEdit) {
		this.timeEdit = timeEdit;
	}

	/**See {@link #timeFinal}. */
	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Final", length = 0)
	public Date getTimeFinal() {
		return timeFinal;
	}
	/**See {@link #timeFinal}. */
	public void setTimeFinal(Date timeFinal) {
		this.timeFinal = timeFinal;
	}

	/**See {@link #timePaid}. */
	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Paid", length = 0)
	public Date getTimePaid() {
		return timePaid;
	}
	/**See {@link #timePaid}. */
	public void setTimePaid(Date timePaid) {
		this.timePaid = timePaid;
	}

	/** See {@link #startForm}. */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Start_Form_Id")
	public StartForm getStartForm() {
		return startForm;
	}
	/** See {@link #startForm}. */
	public void setStartForm(StartForm startForm) {
		this.startForm = startForm;
	}

	/**See {@link #weeklyBatch}. */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Batch_Id")
	public WeeklyBatch getWeeklyBatch() {
		return weeklyBatch;
	}
	/**See {@link #weeklyBatch}. */
	public void setWeeklyBatch(WeeklyBatch weeklyBatch) {
		this.weeklyBatch = weeklyBatch;
	}

	/** See {@link #approverId}. *//*
	@Override
	@JsonIgnore
	@Column(name = "Approver_Id")
	public Integer getApproverId() {
		return approverId;
	}
	*//** See {@link #approverId}. *//*
	@Override
	public void setApproverId(Integer approverContactId) {
		approverId = approverContactId;
		setApproverContactId(null); // force refresh
	}

	*//** See {@link #approverContactId}. *//*
	@Override
	@JsonIgnore
	@Transient
	public Integer getApproverContactId() {
		return approverContactId;
	}
	*//** See {@link #approverContactId}. *//*
	@Override
	public void setApproverContactId(Integer approverContactId) {
		this.approverContactId = approverContactId;
	}*/

	/** See {@link #lastName}. */
	@Column(name = "Last_Name", nullable = false, length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #firstName}. */
	@Column(name = "First_Name", nullable = false, length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@JsonIgnore
	@Transient
	public String getLastNameFirstName() {
		String s = "";
		if (getLastName() != null) {
			s = getLastName();
		}
		if (getFirstName() != null) {
			s += ", " + getFirstName();
		}
		return s;
	}

	/** returns the decrypted, 9-digit SSN, without formatting.*/
	@JsonIgnore
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Social_Security", length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}
	public void setSocialSecurity(String socialSecurity) {
		viewSSN = null;
		this.socialSecurity = socialSecurity;
	}

	/**
	 * @return The SSN formatted as nnn-nn-nnnn; used only for JSON export.
	 *         <p>
	 *         Note that screen (UI) formatting is handled by a Faces converter
	 *         class, SSNConverter.
	 */
	@Transient
	@JsonProperty("socialSecurity")
	public String getSocialSecurityFmtd() {
		String str = getSocialSecurity();
		if (str != null && str.length() == 9) {
			str = str.substring(0,3) + "-" + str.substring(3, 5) + "-" + str.substring(5);
		}
		return str;
	}

	/** See {@link #viewSSN}. */
	@JsonIgnore
	@Transient
	public String getViewSSN() {
		if (viewSSN == null && getSocialSecurity() != null && getSocialSecurity().length() > 8) {
			viewSSN = "###-##-" + getSocialSecurity().substring(getSocialSecurity().length()-4);
		}
		return viewSSN;
	}

	/** Returns the last 4 digits of the Social Security number. */
	@JsonIgnore
	@Transient
	public String getViewSSNmin() {
		String s = null;
		if ((s=getViewSSN()) != null) {
			if (s.length() > 5) {
				s = s.substring(s.length()-5);
			}
		}
		return s;
	}

	/** See {@link #payrollId}. */
	@JsonIgnore
	@Column(name = "Payroll_Id", length = 20)
	public String getPayrollId() {
		return payrollId;
	}
	public void setPayrollId(String userId) {
		payrollId = userId;
		viewSSN = null;
	}

	/** See {@link #signedPaper}. */
	@JsonIgnore
	@Column(name = "Signed_Paper", nullable = false)
	public boolean getSignedPaper() {
		return signedPaper;
	}
	/** See {@link #signedPaper}. */
	public void setSignedPaper(boolean signedPaper) {
		this.signedPaper = signedPaper;
	}

	/** See {@link #paperImage}. */
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Paper_Image_Id")
	public Image getPaperImage() {
		return paperImage;
	}
	/** See {@link #paperImage}. */
	public void setPaperImage(Image paperImage) {
		this.paperImage = paperImage;
	}

	/** See {@link #loanOutCorp}. */
	@Column(name = "Loan_Out_Corp", length = 100)
	public String getLoanOutCorp() {
		return loanOutCorp;
	}
	public void setLoanOutCorp(String loanOutCorp) {
		this.loanOutCorp = loanOutCorp;
	}

	/** See {@link #prodId}. */
	@JsonIgnore
	@Column(name = "Prod_Id", nullable = false, length = 10)
	public String getProdId() {
		return prodId;
	}
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	/** See {@link #prodName}. */
	@Column(name = "Prod_Name", length = 100)
	public String getProdName() {
		return prodName;
	}
	public void setProdName(String prodName) {
		this.prodName = prodName;
	}

	/** See {@link #prodCo}. */
	@Column(name = "Prod_Co", length = 100)
	public String getProdCo() {
		return prodCo;
	}
	public void setProdCo(String prodCo) {
		this.prodCo = prodCo;
	}

	/** See {@link #jobName}. */
	@Column(name = "Job_Name", length = 50)
	public String getJobName() {
		return jobName;
	}
	/** See {@link #jobName}. */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/** See {@link #jobNumber}. */
	@Column(name = "Job_Number", length = 20)
	public String getJobNumber() {
		return jobNumber;
	}
	/** See {@link #jobNumber}. */
	public void setJobNumber(String jobNumber) {
		this.jobNumber = jobNumber;
	}

	/** See {@link #department}. */
	@Override
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Department_Id")
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department department) {
		this.department = department;
	}

	@JsonIgnore
	@Column(name = "Department_Id", insertable=false, updatable=false)
	public Integer getDepartmentId() {
		return departmentId;
	}
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/** See {@link #deptName}. */
	@Column(name = "Dept_Name", length = 50)
	public String getDeptName() {
		return deptName;
	}
	/** See {@link #deptName}. */
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	/** See {@link #offProduction}. */
	@Column(name = "Off_Production", nullable = false)
	public boolean getOffProduction() {
		return offProduction;
	}
	/** See {@link #offProduction}. */
	public void setOffProduction(boolean offProduction) {
		this.offProduction = offProduction;
	}

	/** See {@link #underContract}. */
	@Override
	@Column(name = "Under_Contract", nullable = false)
	public boolean getUnderContract() {
		return underContract;
	}
	/** See {@link #underContract}. */
	public void setUnderContract(boolean underContract) {
		this.underContract = underContract;
	}

	/**See {@link #allowWorked}. */
	@Column(name = "Allow_Worked", nullable = false)
	public boolean getAllowWorked() {
		return allowWorked;
	}
	/**See {@link #allowWorked}. */
	public void setAllowWorked(boolean allowWorked) {
		this.allowWorked = allowWorked;
	}

	/** See {@link #retirementPlan}. */
	@Column(name = "retirement_plan", length = 50)
	public String getRetirementPlan() {
		return retirementPlan;
	}
	/** See {@link #retirementPlan}. */
	public void setRetirementPlan(String retirementPlan) {
		this.retirementPlan = retirementPlan;
	}

	/** See {@link #occupation}. */
	@Column(name = "Occupation", length = 100)
	public String getOccupation() {
		return occupation;
	}
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	@Transient
	@JsonIgnore
	public String getOccupationForButton() {
		if (occupation.length() > Constants.MAX_MOBILE_BUTTON_TEXT_LENGTH) {
			return StringUtils.trimToWord(occupation, Constants.MAX_MOBILE_BUTTON_TEXT_LENGTH);
		}
		return occupation;
	}

	/** See {@link #occCode}. */
	@Column(name = "Occ_Code", length = 20)
	public String getOccCode() {
		return occCode;
	}
	/** See {@link #occCode}. */
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	/**See {@link #lsOccCode}. */
	@Column(name = "Ls_Occ_Code", length = 10)
	public String getLsOccCode() {
		return lsOccCode;
	}
	/**See {@link #lsOccCode}. */
	public void setLsOccCode(String lSoccCode) {
		lsOccCode = lSoccCode;
	}

	/** See {@link #wageState}. */
	@Column(name = "Overtime_Rule", length = 10)
	public String getWageState() {
		return wageState;
	}
	/** See {@link #wageState}. */
	public void setWageState(String overtimeRule) {
		this.wageState = overtimeRule;
	}

	/**
	 * @return True if HTG should use the "State worked" for determining wage
	 *         rules to apply. Returns false if a specified state should be
	 *         used, regardless of state worked.
	 */
	@JsonIgnore
	@Transient
	public boolean getUseStateWorked() {
		if (getWageState() == null || getWageState().length() == 0 ||
				getWageState().equals(Constants.STATE_WORKED_CODE)) {
			return true;
		}
		return false;
	}

	/** See {@link #unionNumber}. */
	@Column(name = "Union_Number", length = 20)
	public String getUnionNumber() {
		return unionNumber;
	}
	/** See {@link #unionNumber}. */
	public void setUnionNumber(String unionNumber) {
		this.unionNumber = unionNumber;
	}

	/**
	 * @return True iff this timecard is for a non-union position. Note that it
	 *         is considered non-union if the Union field is empty or equal to
	 *         "N".
	 */
	@JsonIgnore
	@Transient
	public boolean isNonUnion() {
		if (getUnionNumber() == null || getUnionNumber().length() == 0 ||
				getUnionNumber().equals(Unions.NON_UNION)) {
			return true;
		}
		return false;
	}

	/** See {@link #acaEndWorkDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "ACA_End_Work_Date", length = 10)
	public Date getAcaEndWorkDate() {
		return acaEndWorkDate;
	}
	/** See {@link #acaEndWorkDate}. */
	public void setAcaEndWorkDate(Date acaEndWorkDate) {
		this.acaEndWorkDate = acaEndWorkDate;
	}

	/** See {@link #acaLeaveStartDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "ACA_Leave_Start", length = 10)
	public Date getAcaLeaveStartDate() {
		return acaLeaveStartDate;
	}
	/** See {@link #acaLeaveStartDate}. */
	public void setAcaLeaveStartDate(Date acaLeaveStartDate) {
		this.acaLeaveStartDate = acaLeaveStartDate;
	}

	/** See {@link #acaLeaveEndDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "ACA_Leave_End", length = 10)
	public Date getAcaLeaveEndDate() {
		return acaLeaveEndDate;
	}
	/** See {@link #acaLeaveEndDate}. */
	public void setAcaLeaveEndDate(Date acaLeaveEndDate) {
		this.acaLeaveEndDate = acaLeaveEndDate;
	}

	/** See {@link #rateType}. */
	@Column(name = "Rate_Type", length = 1)
	public String getRateType() {
		return rateType;
	}
	/** See {@link #rateType}. */
	public void setRateType(String rateType) {
		this.rateType = rateType;
	}

	@JsonIgnore
	@Transient
	public String getRateTypeText() {
		if (isStudioRate()) {
			return RATE_TYPE_STUDIO_TEXT;
		}
		else {
			return RATE_TYPE_LOCATION_TEXT;
		}
	}

	/**
	 * @return True if the Studio/Location rate selection field is set to
	 *         Location (Distant).
	 */
	@JsonIgnore
	@Transient
	public boolean isDistantRate() {
		return (getRateType() != null) && getRateType().equals(StartForm.USE_LOCATION_RATE);
	}

	/**
	 * @return True if the Studio/Location rate selection field is set to
	 *         Studio.
	 */
	@JsonIgnore
	@Transient
	public boolean isStudioRate() {
		return (getRateType() == null) || getRateType().equals(StartForm.USE_STUDIO_RATE);
	}

	/**
	 * @return The default WorkZone for this timecard, which is determined by
	 *         the {@link #rateType} field as either Studio or Distant
	 *         (Location).
	 */
	@JsonIgnore
	@Transient
	public WorkZone getDefaultZone() {
		return isStudioRate() ? WorkZone.SL : WorkZone.DL;
	}

	/**See {@link #employeeRateType}. */
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	@Column(name = "Employee_Rate_Type")
	public EmployeeRateType getEmployeeRateType() {
		return employeeRateType;
	}
	/**See {@link #employeeRateType}. */
	public void setEmployeeRateType(EmployeeRateType rateType) {
		employeeRateType = rateType;
	}

	/** See {@link #rate}. */
	@Column(name = "Rate", precision = 10, scale = 4)
	public BigDecimal getRate() {
		return rate;
	}
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	/**See {@link #hourlyRate}. */
	@JsonIgnore
	@Column(name = "Hourly_Rate", precision = 10, scale = 4)
	public BigDecimal getHourlyRate() {
		return hourlyRate;
	}
	/**See {@link #hourlyRate}. */
	public void setHourlyRate(BigDecimal hourlyRate) {
		this.hourlyRate = hourlyRate;
	}

	/**See {@link #dailyRate}. */
	@JsonIgnore
	@Column(name = "Daily_Rate", precision = 10, scale = 4)
	public BigDecimal getDailyRate() {
		return dailyRate;
	}
	/**See {@link #dailyRate}. */
	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	/**See {@link #weeklyRate}. */
	@JsonIgnore
	@Column(name = "Weekly_Rate", precision = 10, scale = 4)
	public BigDecimal getWeeklyRate() {
		return weeklyRate;
	}
	/**See {@link #weeklyRate}. */
	public void setWeeklyRate(BigDecimal weeklyRate) {
		this.weeklyRate = weeklyRate;
	}

	/** See {@link #guarHours}. */
	@Column(name = "Guar_Hours", precision = 4, scale = 2)
	public BigDecimal getGuarHours() {
		return guarHours;
	}
	public void setGuarHours(BigDecimal guarHours) {
		this.guarHours = guarHours;
	}

	/** See {@link #stateWorked}. */
	@Column(name = "State_Worked", length = 50)
	public String getStateWorked() {
		return stateWorked;
	}
	public void setStateWorked(String stateWorked) {
		this.stateWorked = stateWorked;
	}

	/** See {@link #cityWorked}. */
	@Column(name = "City_Worked", length = 50)
	public String getCityWorked() {
		return cityWorked;
	}
	/** See {@link #cityWorked}. */
	public void setCityWorked(String cityWorked) {
		this.cityWorked = cityWorked;
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

	/** See {@link #workCountry}. */
	@Column(name = "Work_Country", length = 100)
	public String getWorkCountry() {
		return workCountry;
	}
	/** See {@link #workCountry}. */
	public void setWorkCountry(String workCountry) {
		this.workCountry = workCountry;
	}

	/** See {@link #fedCorpId}. */
	@JsonIgnore
	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Fed_Corp_Id", nullable = true, length = 1000)
	public String getFedCorpId() {
		return fedCorpId;
	}
	public void setFedCorpId(String fedCorpId) {
		this.fedCorpId = fedCorpId;
	}

	/**
	 * @return The Loan-out corp's federal tax id formatted as nnn-nn-nnnn; used only for JSON export.
	 *         <p>
	 *         Note that screen (UI) formatting is handled by a Faces converter
	 *         class, SSNConverter.
	 */
	@Transient
	@JsonProperty("fedCorpId")
	public String getFedCorpIdFmtd() {
		String str = getFedCorpId();
		if (str != null && str.length() == 9) {
			str = str.substring(0,2) + "-" + str.substring(2);
		}
		return str;
	}

	/** See {@link #stateCorpId}. */
	@Column(name = "State_Corp_Id", length = 20)
	public String getStateCorpId() {
		return stateCorpId;
	}
	public void setStateCorpId(String stateCorpId) {
		this.stateCorpId = stateCorpId;
	}

	/** See {@link #account}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Account_Loc", length = 10) ),
		@AttributeOverride(name="major", 	column = @Column(name = "Account_Major", length = 10) ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Account_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Account_Sub", length = 10) ),
		@AttributeOverride(name="set", 		column = @Column(name = "Account_Set", length = 10) ),
		@AttributeOverride(name="free", 	column = @Column(name = "Free", length = 10) ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Free2", length = 10) ),
	} )
	public AccountCodes getAccount() {
		if (account == null) {
			account = new AccountCodes();
		}
		return account;
	}
	/** See {@link #account}. */
	public void setAccount(AccountCodes account) {
		this.account = account;
	}


	/** surrogate for account.aloc
	 * @see AccountCodes#getAloc() */
	@JsonIgnore
	@Transient
	public String getAccountLoc() {
		return getAccount().getAloc();
	}
	public void setAccountLoc(String code) {
		getAccount().setAloc(code);
	}

	/** surrogate for account.major
	 * @see AccountCodes#getMajor() */
	@JsonIgnore
	@Transient
	public String getAccountMajor() {
		return getAccount().getMajor();
	}
	public void setAccountMajor(String accountMajor) {
		getAccount().setMajor(accountMajor);
	}

	/** surrogate for account.dtl
	 * @see AccountCodes#getDtl() */
	@JsonIgnore
	@Transient
	public String getAccountDtl() {
		return getAccount().getDtl();
	}
	public void setAccountDtl(String accountDtl) {
		getAccount().setDtl(accountDtl);
	}

	/** surrogate for account.Sub
	 * @see AccountCodes#getSub() */
	@JsonIgnore
	@Transient
	public String getAccountSub() {
		return getAccount().getSub();
	}
	public void setAccountSub(String accountSub) {
		getAccount().setSub(accountSub);
	}

	/** surrogate for account.set
	 * @see AccountCodes#getSet() */
	@JsonIgnore
	@Transient
	public String getAccountSet() {
		return getAccount().getSet();
	}
	public void setAccountSet(String accountSet) {
		getAccount().setSet(accountSet);
	}

	/** surrogate for account.free
	 * @see AccountCodes#getFree() */
	@JsonIgnore
	@Transient
	public String getAccountFree() {
		return getAccount().getFree();
	}
	public void setAccountFree(String free) {
		getAccount().setFree(free);
	}

	/** surrogate for account.free2
	 * @see AccountCodes#getFree2() */
	@JsonIgnore
	@Transient
	public String getAccountFree2() {
		return getAccount().getFree2();
	}
	public void setAccountFree2(String free) {
		getAccount().setFree2(free);
	}

	/**
	 * Copy all of the account code fields from the given source into this
	 * object's corresponding account fields.
	 *
	 * @param ac The source of the account codes.
	 */
	@JsonIgnore
	@Transient
	public void setAccountCodes(AccountCodes ac) {
		getAccount().copyFrom(ac);
	}

	/** See {@link #comments}. */
	@Column(name = "Comments", length = 5000)
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** See {@link #privateComments}. */
	@Column(name = "Private_Comments", length = 5000)
	public String getPrivateComments() {
		return privateComments;
	}
	/** See {@link #privateComments}. */
	public void setPrivateComments(String privateComments) {
		this.privateComments = privateComments;
	}

	/** See {@link #mileage}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	public Mileage getMileage() {
		return mileage;
	}
	/** See {@link #mileage}. */
	public void setMileage(Mileage mileage) {
		this.mileage = mileage;
	}

	/** See {@link #boxRental}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	public BoxRental getBoxRental() {
		return boxRental;
	}
	/** See {@link #boxRental}. */
	public void setBoxRental(BoxRental boxRental) {
		this.boxRental = boxRental;
	}

//	/** See {@link #totalPrep}. */
//	@Column(name = "Total_prep", precision = 8, scale = 2)
//	public BigDecimal getTotalPrep() {
//		return totalPrep;
//	}
//	/** See {@link #totalPrep}. */
//	public void setTotalPrep(BigDecimal totalPrep) {
//		this.totalPrep = totalPrep;
//	}
//
//	/** See {@link #totalShoot}. */
//	@Column(name = "Total_shoot", precision = 8, scale = 2)
//	public BigDecimal getTotalShoot() {
//		return totalShoot;
//	}
//	/** See {@link #totalShoot}. */
//	public void setTotalShoot(BigDecimal totalShoot) {
//		this.totalShoot = totalShoot;
//	}
//
//	/** See {@link #totalWrap}. */
//	@Column(name = "Total_wrap", precision = 8, scale = 2)
//	public BigDecimal getTotalWrap() {
//		return totalWrap;
//	}
//	/** See {@link #totalWrap}. */
//	public void setTotalWrap(BigDecimal totalWrap) {
//		this.totalWrap = totalWrap;
//	}

	/** See {@link #adjGtotal}. */
	@JsonIgnore
	@Column(name = "Adj_Gtotal", precision = 10, scale = 2)
	public BigDecimal getAdjGtotal() {
		return adjGtotal;
	}
	/** See {@link #adjGtotal}. */
	public void setAdjGtotal(BigDecimal adjGtotal) {
		this.adjGtotal = adjGtotal;
	}

	/** See {@link #totalWages1}. */
	@JsonIgnore
	@Column(name = "Total_Wages1", precision = 10, scale = 2)
	public BigDecimal getTotalWages1() {
		return totalWages1;
	}
	/** See {@link #totalWages1}. */
	public void setTotalWages1(BigDecimal totalWages1) {
		this.totalWages1 = totalWages1;
	}

	/** See {@link #totalWages2}. */
	@JsonIgnore
	@Column(name = "Total_Wages2", precision = 10, scale = 2)
	public BigDecimal getTotalWages2() {
		return totalWages2;
	}
	/** See {@link #totalWages2}. */
	public void setTotalWages2(BigDecimal totalWages2) {
		this.totalWages2 = totalWages2;
	}

	/** See {@link #advGtotal}. */
	@JsonIgnore
	@Column(name = "Adv_Gtotal", precision = 10, scale = 2)
	public BigDecimal getAdvGtotal() {
		return advGtotal;
	}
	/** See {@link #advGtotal}. */
	public void setAdvGtotal(BigDecimal advGtotal) {
		this.advGtotal = advGtotal;
	}

	/** See {@link #grandTotal}. */
	@Column(name = "Grand_Total", precision = 10, scale = 2)
	public BigDecimal getGrandTotal() {
		return grandTotal;
	}
	/** See {@link #grandTotal}. */
	public void setGrandTotal(BigDecimal grandTotal) {
		this.grandTotal = grandTotal;
	}

	/** See {@link #totalHours}. */
	@Column(name = "Total_Hours", precision = 5, scale = 2)
	public BigDecimal getTotalHours() {
		if (totalHours == null) {
			totalHours = Constants.DECIMAL_ZERO; // 0.00
		}
		return totalHours;
	}
	/** See {@link #totalHours}. */
	public void setTotalHours(BigDecimal totalHours) {
		this.totalHours = totalHours;
	}

	/** See {@link #totalPaidHours}. */
	@Column(name = "Total_Paid_Hours", precision = 5, scale = 2)
	public BigDecimal getTotalPaidHours() {
		return totalPaidHours;
	}
	/** See {@link #totalPaidHours}. */
	public void setTotalPaidHours(BigDecimal totalPaidHours) {
		this.totalPaidHours = totalPaidHours;
	}

	/** See {@link #lastWorkDayNum}. */
	@Column(name = "Last_Work_Day_Num", nullable = false)
	public byte getLastWorkDayNum() {
		return lastWorkDayNum;
	}
	/** See {@link #lastWorkDayNum}. */
	public void setLastWorkDayNum(byte lastWorkDayNum) {
		this.lastWorkDayNum = lastWorkDayNum;
	}

	/** See {@link #endingDayNum}. */
	@Column(name = "Ending_Day_Num", nullable = false)
	public byte getEndingDayNum() {
		return endingDayNum;
	}
	/** See {@link #endingDayNum}. */
	public void setEndingDayNum(byte endingDayNum) {
		this.endingDayNum = endingDayNum;
	}

	/** See {@link #timecardEvents}. */
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("date")
	public List<TimecardEvent> getTimecardEvents() {
		return timecardEvents;
	}
	/** See {@link #timecardEvents}. */
	public void setTimecardEvents(List<TimecardEvent> timecardEvents) {
		this.timecardEvents = timecardEvents;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@Transient
	public List getEvents() {
		return getTimecardEvents();
	}

	/** See {@link #dailyTimes}. */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("dayNum")
	public List<DailyTime> getDailyTimes() {
		return dailyTimes;
	}
	/** See {@link #dailyTimes}. */
	public void setDailyTimes(List<DailyTime> dailyTimes) {
		this.dailyTimes = dailyTimes;
	}

	/** See {@link #payLines}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("lineNumber")
	public List<PayBreakdown> getPayLines() {
		return payLines;
	}
	/** See {@link #payLines}. */
	public void setPayLines(List<PayBreakdown> payLines) {
		this.payLines = payLines;
	}

	/** See {@link #payDailyLines}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("lineNumber")
	public List<PayBreakdownDaily> getPayDailyLines() {
		return payDailyLines;
	}
	/** See {@link #payDailyLines}. */
	public void setPayDailyLines(List<PayBreakdownDaily> payDailyLines) {
		this.payDailyLines = payDailyLines;
	}

	/** See {@link #deletedPayLines}. */
	@JsonIgnore
	@Transient
	public List<PayBreakdownMapped> getDeletedPayLines() {
		return deletedPayLines;
	}
	/** See {@link #deletedPayLines}. */
	public void setDeletedPayLines(List<PayBreakdownMapped> deletedPayLines) {
		this.deletedPayLines = deletedPayLines;
	}

	/** See {@link #expenseLines}. */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("lineNumber")
	public List<PayExpense> getExpenseLines() {
		return expenseLines;
	}
	/** See {@link #expenseLines}. */
	public void setExpenseLines(List<PayExpense> expenseLines) {
		this.expenseLines = expenseLines;
	}

	/** See {@link #payJobs}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	@OrderBy("jobNumber")
	public List<PayJob> getPayJobs() {
		return payJobs;
	}
	/** See {@link #payJobs}. */
	public void setPayJobs(List<PayJob> payJobs) {
		this.payJobs = payJobs;
	}

	/** See {@link #attachments}. */
	@JsonIgnore
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyTimecard")
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	/** See {@link #attachments}. */
	public void setAttachments(Set<Attachment> attachments) {
		this.attachments = attachments;
	}

	/** returns number of entries in the payJobs List -- used by JSP */
	@JsonIgnore
	@Transient
	public int getPayJobsSize() {
		return getPayJobs().size();
	}

	/** See {@link #dailyHours}.
	 * Note that this is accessed from JSP directly. */
	@JsonIgnore
	@Transient
	public BigDecimal[] getDailyHours() {
		if (dailyHours == null) {
			createDailyHours();
		}
		return dailyHours;
	}
//	/** See {@link #dailyHours}. */
//	public void setDailyHours(BigDecimal[] dailyHours) {
//		this.dailyHours = dailyHours;
//	}

	/** See {@link #startDate}. */
	@JsonIgnore
	@Transient
	public Date getStartDate() {
		if (startDate == null && endDate != null) {
			Calendar cal = CalendarUtils.getInstance(endDate);
			cal.add(Calendar.DAY_OF_WEEK, -6);
			startDate = cal.getTime();
		}
		return startDate;
	}

	/** See {@link #weekEndDay}. */
	@JsonIgnore
	@Transient
	public Integer getWeekEndDay() {
		if (weekEndDay == null) {
			DailyTime dt = getDailyTimes().get(6);
			Date date = dt.getDate();
			if (date != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(date);
				weekEndDay = cal.get(Calendar.DAY_OF_WEEK);
			}
			else {
				weekEndDay = Constants.DEFAULT_WEEK_END_DAY;
			}
		}
		return weekEndDay;
	}
//	/** See {@link #weekEndDay}. */
//	public void setWeekEndDay(Integer weekEndDay) {
//		this.weekEndDay = weekEndDay;
//	}

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

	/** See {@link #importStatus}. */
	@JsonIgnore
	@Transient
	public ImportStatus getImportStatus() {
		return importStatus;
	}
	/** See {@link #importStatus}. */
	public void setImportStatus(ImportStatus importStatus) {
		this.importStatus = importStatus;
	}

	/** See {@link #allExempt}. */
	@JsonIgnore
	@Transient
	public boolean getAllExempt() {
		return allExempt;
	}
	/** See {@link #allExempt}. */
	public void setAllExempt(boolean allExempt) {
		this.allExempt = allExempt;
	}

	/** See {@link #grandDailyTotal}. */
	@JsonIgnore
	@Transient
	public BigDecimal getGrandDailyTotal() {
		if (grandDailyTotal == null) {
			calculatePayBreakdownDaily();
		}
		return grandDailyTotal;
	}
	/** See {@link #grandDailyTotal}. */
	public void setGrandDailyTotal(BigDecimal grandDailyTotal) {
		this.grandDailyTotal = grandDailyTotal;
	}

	/** See {@link #paidAs}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Paid_As")
	public PaidAsType getPaidAs() {
		return paidAs;
	}

	/** See {@link #paidAs}. */
	public void setPaidAs(PaidAsType paidAs) {
		this.paidAs = paidAs;
	}

	/**
	 * @return True if the grand total matches the "daily pay breakdown" grand total.
	 */
	@JsonIgnore
	@Transient
	public boolean getGrandTotalsMatch() {
		return NumberUtils.compare(getGrandTotal(), getGrandDailyTotal()) == 0;
	}

	/** See {@link #mayUseOnCall}. */
	@JsonIgnore
	@Transient
	public boolean getMayUseOnCall() {
		return ((! getAllowWorked()) && isNonUnion() && getEmployeeRateType() == EmployeeRateType.HOURLY);
	}

	/** See {@link #totalDprHours}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalDprHours() {
		return totalDprHours;
	}
	/** See {@link #totalDprHours}. */
	public void setTotalDprHours(BigDecimal totalDprHours) {
		this.totalDprHours = totalDprHours;
	}

	/** See {@link #totalDprMpv1}. */
	@JsonIgnore
	@Transient
	public short getTotalDprMpv1() {
		return totalDprMpv1;
	}
	/** See {@link #totalDprMpv1}. */
	public void setTotalDprMpv1(short totalDprMpv) {
		totalDprMpv1 = totalDprMpv;
	}

	/** See {@link #totalDprMpv2}. */
	@JsonIgnore
	@Transient
	public short getTotalDprMpv2() {
		return totalDprMpv2;
	}
	/** See {@link #totalDprMpv2}. */
	public void setTotalDprMpv2(short totalDprMpv2) {
		this.totalDprMpv2 = totalDprMpv2;
	}

	/** See {@link #totalMpv1}. */
	@JsonIgnore
	@Transient
	public short getTotalMpv1() {
		return totalMpv1;
	}
	/** See {@link #totalMpv1}. */
	public void setTotalMpv1(short totalMpv) {
		totalMpv1 = totalMpv;
	}

	/** See {@link #totalMpv2}. */
	@JsonIgnore
	@Transient
	public short getTotalMpv2() {
		return totalMpv2;
	}
	/** See {@link #totalMpv2}. */
	public void setTotalMpv2(short totalMpv) {
		totalMpv2 = totalMpv;
	}

	/** See {@link #totalMpv2}. */
	@JsonIgnore
	@Transient
	public short getTotalMpv3() {
		return totalMpv3;
	}
	/** See {@link #totalMpv3}. */
	public void setTotalMpv3(short totalMpv) {
		totalMpv3 = totalMpv;
	}

	/** See {@link #totalMpvUser}. */
	@JsonIgnore
	@Transient
	public short getTotalMpvUser() {
		return totalMpvUser;
	}
	/** See {@link #totalMpvUser}. */
	public void setTotalMpvUser(short totalMpvUser) {
		this.totalMpvUser = totalMpvUser;
	}

	/** See {@link #dailyAmounts}.
	 * Note that this is accessed from JSP directly. */
	@JsonIgnore
	@Transient
	public BigDecimal[] getDailyAmounts() {
		if (dailyAmounts == null) {
			createDailyAmounts();
		}
		return dailyAmounts;
	}
	/** See {@link #dailyAmounts}. */
	public void setDailyAmounts(BigDecimal[] dailyAmounts) {
		this.dailyAmounts = dailyAmounts;
	}

	@JsonIgnore
	@Transient
	public BigDecimal getTotalExpenses() {
		return calculateExpenses();
	}

	/**
	 * Check if a timecard has all fields required for submission.
	 *
	 * @param checkPaidAs True if the PaidAs and associated loan-out corp id
	 *            should be included as required fields. LS-2737
	 * @return True iff all the required fields have been filled in with some
	 *         non-blank data. This checks for the fields required to submit a
	 *         timecard.
	 */
	@JsonIgnore
	@Transient
	public boolean getHasRequiredFields(boolean checkPaidAs) {

		boolean missingData =	// If ANY of these fields are empty, we're missing something
				StringUtils.isEmpty(getSocialSecurity()) ||
				StringUtils.isEmpty(getStateWorked()) ||
				StringUtils.isEmpty(getCityWorked());

		if (checkPaidAs && ! missingData) {
			boolean isTeamPayroll = false;
			Production prod = SessionUtils.getCurrentOrViewedProduction();
			PayrollPreference pf = prod.getPayrollPref();
			PayrollService ps = pf.getPayrollService();
			if (ps != null) {
				isTeamPayroll = ps.getTeamPayroll();
			}
			// LS-2727 & LS-2734. Validate for missing paid as or FEIN values.
			if (isTeamPayroll && ! prod.getType().isTours()) {
				missingData = (getPaidAs() == null );
			}
		}
		return ! missingData;
	}

	/**
	 * @return True iff no hours information, box rental, or mileage has been
	 * entered in this timecard.
	 */
	@JsonIgnore
	@Transient
	public boolean getEmpty() {
		if (getTotalMpv1() == 0 && getTotalMpv2() == 0 &&
				getTotalMpvUser() == 0 &&
				getTotalHours().signum() == 0 &&
				(getGrandTotal() == null || getGrandTotal().signum() == 0) &&
				(getBoxRental() == null || getBoxRental().getAmount() == null ||
						getBoxRental().getAmount().signum() == 0)) {
			if (getMileage() == null ||
					(getMileage().getNonTaxableMiles().signum() == 0 &&
					getMileage().getTaxableMiles().signum() == 0)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return True if the timecard is ready to transmit, based on the
	 * existence of certain fields, and a non-Void status.
	 */
	@JsonIgnore
	@Transient
	public boolean getTransmittable() {
		if (status == ApprovalStatus.VOID && timeSent == null) {
			return false;
		}
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				StringUtils.isEmpty(getFirstName()) ||
				StringUtils.isEmpty(getLastName()) ||
				StringUtils.isEmpty(getSocialSecurity()) ||
				StringUtils.isEmpty(getStateWorked()) ||
				StringUtils.isEmpty(getCityWorked()) ||
				StringUtils.isEmpty(getProdName()) ||
				StringUtils.isEmpty(getProdCo()) ||
				StringUtils.isEmpty(getOccupation());
		if (! missingData) {
			for (PayExpense exp : getExpenseLines()) {
				missingData |= (exp.getCategory() == null);
				missingData |= (exp.getTotal() == null);
			}
		}
		if (! missingData) {
			for (PayBreakdown pb : getPayLines()) {
				missingData |= (pb.getCategory() == null);
				missingData |= (pb.getTotal() == null);
			}
		}

		if (! missingData) {
			if(getPayLines().isEmpty()) {
				Production prod = ProductionDAO.getInstance().findOneByProperty("prodId", prodId);
				boolean isTeamPayroll = ProductionDAO.getInstance().findIsTeamPayroll(prod.getProdId());
				if(isTeamPayroll) {
					// If there are not any paybreakdown items for Team clients,
					// then we are missing data for calculations.
					if(payLines.isEmpty()) {
						missingData = true;
					}
				}
			}
		}

		return ! missingData;
	}

	/** See {@link #teamEor}. */
	@JsonIgnore
	@Transient
	public String getTeamEor() {
		return teamEor;
	}
	/** See {@link #teamEor}. */
	public void setTeamEor(String teamEor) {
		this.teamEor = teamEor;
	}

	/**
	 * Import data via an Importer into this instance.  This is currently
	 * used to load our timecards with data from another product.
	 *
	 * @param imp The Importer containing the data to be loaded.
	 * @throws IOException
	 */
	public void imports(Importer imp) throws IOException, ParseException {
		/* currently unused.  Comment out as part of LS-2737
		//setId(imp.get()); // already read & set by caller
		//setUserAccount(imp.get());
		setEndDate(imp.getDate());
		//setStatus(imp.get());
		//setHtgLocked(imp.get());
		//setApproverId(imp.get());
		setLastName(imp.getString());
		setFirstName(imp.getString());
		imp.getString(); // setSocialSecurity(imp.getString()); // ??
		//setPayrollId(imp.get());
		//setSignedPaper(imp.get());
		//setPaperImageId(imp.get());
		setLoanOutCorp(imp.getString());
		//setProdId(imp.get());
		setProdName(imp.getString());
		setProdCo(imp.getString());
		//setDepartmentId(imp.get());
		setDeptName(imp.getString());
		setOffProduction(imp.getBoolean());
		setUnderContract(imp.getBoolean());
		setOccupation(imp.getString());
		setOccCode(imp.getString());
		setUnionNumber(imp.getString());
		setRateType(imp.getString());
		setRate(imp.getBigDecimal());
		setGuarHours(imp.getBigDecimal());
		setStateWorked(imp.getString());
		setCityWorked(imp.getString());
		setFedCorpId(imp.getString());
		setStateCorpId(imp.getString());
		setAccountMajor(imp.getString());
		setAccountDtl(imp.getString());
		setComments(imp.getString());
		setPrivateComments(imp.getString());
//		setBoxAmt(imp.getBigDecimal());
//		setBoxAcctMajor(imp.getString());
//		setBoxAcctDtl(imp.getString());
//		setBoxAcctSet(imp.getString());
//		setPerdiemTx(imp.getBigDecimal());
//		setPerdiemNtx(imp.getBigDecimal());
//		setPerdiemTotal(imp.getBigDecimal());
//		setPerdiemAcctMajor(imp.getString());
//		setPerdiemAcctDtl(imp.getString());
//		setPerdiemAcctSet(imp.getString());
//		setPerdiemDays(imp.getByte());
//		setLodgeTx(imp.getBigDecimal());
//		setLodgeNtx(imp.getBigDecimal());
//		setLodgeTotal(imp.getBigDecimal());
//		setLodgeAcctMajor(imp.getString());
//		setLodgeAcctDtl(imp.getString());
//		setLodgeAcctSet(imp.getString());
//		setLodgeDays(imp.getByte());
//		setPersMilesNum(imp.getBigDecimal());
//		setPersMilesRate(imp.getBigDecimal());
//		setPersMilesTotal(imp.getBigDecimal());
//		setPersMilesAcctMajor(imp.getString());
//		setPersMilesAcctDtl(imp.getString());
//		setPersMilesAcctSet(imp.getString());
//		setBizMilesNum(imp.getBigDecimal());
//		setBizMilesRate(imp.getBigDecimal());
//		setBizMilesTotal(imp.getBigDecimal());
//		setBizMilesAcctMajor(imp.getString());
//		setBizMilesAcctDtl(imp.getString());
//		setBizMilesAcctSet(imp.getString());
//		setCarAllowAmt(imp.getBigDecimal());
//		setCarAllowAcctMajor(imp.getString());
//		setCarAllowAcctDtl(imp.getString());
//		setCarAllowAcctSet(imp.getString());
//		setOtherExpl(imp.getString());
//		setOtherAmt(imp.getBigDecimal());
//		setOtherAcctMajor(imp.getString());
//		setOtherAcctDtl(imp.getString());
//		setOtherAcctSet(imp.getString());
//		setAdv1Expl(imp.getString());
//		setAdv1Amt(imp.getBigDecimal());
//		setAdv1AcctMajor(imp.getString());
//		setAdv1AcctDtl(imp.getString());
//		setAdv1AcctSet(imp.getString());
//		setAdv2Expl(imp.getString());
//		setAdv2Amt(imp.getBigDecimal());
//		setAdv2AcctMajor(imp.getString());
//		setAdv2AcctDtl(imp.getString());
//		setAdv2AcctSet(imp.getString());
		setGrandTotal(imp.getBigDecimal());

		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		// Job definitions - 7 entries - some may be unused
		int n = 0;
		for (PayJob pj : getPayJobs()) {
			pj.setReferenced(false);
			if (n < Constants.CREW_CARDS_NUMBER_JOBS) {
				pj.imports(imp);
			}
			else { // TC has more PayJob`s than can be imported - discard them
				pj.setAccountMajor(null); // it will get cleaned up later
			}
			n++;
		}
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_JOBS; i++) {
			PayJob pj = new PayJob(this, (byte)(i+1));
			TimecardUtils.createPayJobDailies(pj, endDate);
			pj.imports(imp);
			pj.setReferenced(false);
			getPayJobs().add(pj);
		}

		for (DailyTime dt : getDailyTimes()) {
			dt.imports(imp); // load basic daily info
			// now load job/split info for each day
			importSplit(imp, dt, 1); // 1st split
			importSplit(imp, dt, 2);
			importSplit(imp, dt, 3);
		}

		// Now we can remove any PayJob entries that are empty and were
		// not referenced by any daily split, except we always keep at least 1.
		for (int i = getPayJobs().size()-1; i >= 1; i--) {
			PayJob pj = getPayJobs().get(i);
			if (! pj.getReferenced() && StringUtils.isEmpty(pj.getAccountMajor())) {
				getPayJobs().remove(pj);
				if (pj.getId() != null) {
					weeklyTimecardDAO.delete(pj);
				}
			}
			else {
				break; // stop removing once we find a non-empty or referenced job
			}
		}

		// Pay Breakdowns - ones defined + fill to 30 (crew-call max)
		n = 0;
		for (PayBreakdown pb : getPayLines()) {
			pb.imports(imp);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_PAYLINES) {
				break;
			}
		}
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_PAYLINES; i++) {
			PayBreakdown pb = new PayBreakdown(this, (byte)i);
			pb.imports(imp);
			if (! pb.isEmpty()) {
				getPayLines().add(pb);
			}
		}
		// Now delete any trailing empty PayBreakdown entries, or
		// any in excess of the max that we could have imported.
		for (int i = getPayLines().size()-1; i >= 0; i--) {
			PayBreakdown pb = getPayLines().get(i);
			if (pb.isEmpty() || i >=  Constants.CREW_CARDS_NUMBER_PAYLINES) {
				getPayLines().remove(i);
				weeklyTimecardDAO.delete(pb);
			}
			else {
				break;
			}
		}
	*/
	}

	/* currently unused.  Comment out as part of LS-2737
	private void importSplit(Importer imp, DailyTime dt, int splitNumber)
			throws EOFException, IOException {
		BigDecimal splitValue = null;
		if (splitNumber > 1) { // no split value for split #1
			splitValue = imp.getBigDecimal(); // read split value (hours or percentage)
		}
		String episode = imp.getString(); 	// read prod/episode#

		// Determine the job number & PayJob referenced based on the prod/episode#
		byte jobNum = 1; // default jobNumber if no match found
		byte jnMatch = 1;
		if (! StringUtils.isEmpty(episode)) {
			for (PayJob pj : getPayJobs()) {
				if (episode.equals(pj.getAccountMajor())) {
					jobNum = jnMatch;
					break;
				}
				jnMatch++;
			}
		}
		PayJob payJob = getPayJobs().get(jobNum-1);

		// Update the dailyTime job number & set split value (hours or percentage)
		switch (splitNumber) {
		case 1:
			dt.setJobNum1(jobNum);
			// split 1 does not have a split value
			break;
		case 2:
			dt.setJobNum2(jobNum);
			dt.setSplitStart2(splitValue);
			break;
		case 3:
			dt.setJobNum3(jobNum);
			dt.setSplitStart3(splitValue);
			break;
		}

		// Now load the PayJobDaily information into the selected PayJob
		if (((splitNumber == 1) && (dt.getHours() != null)
				&& (dt.getHours().signum() > 0)) // first split & day is not empty
				|| splitValue != null) {	// or 2nd/3rd split with values
			payJob.setReferenced(true);
			PayJobDaily pd = payJob.getPayJobDailies().get(dt.getDayNum()-1); // get matching day
			pd.imports(imp);	// load rest of split info (PayJobDaily)
		}
		else { // empty split (2nd or 3rd), or empty day (1st split and total hours = 0)
			new PayJobDaily().imports(imp);	// so just read data & discard it
		}
	}
	*/

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat, tab-delimited record designed to be loaded
	 * into Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public void exportCrewcards(Exporter ex, boolean payInfo) {
		ex.append(getPayrollKey());
		ex.append(getId());
		ex.append(getUserAccount());
		ex.append(getEndDate());
		ex.append(getAdjusted());
		//ex.append(getUpdated());
		//ex.append(getStatus());
		//ex.append(getHtgLocked());
		//ex.append(getApproverId());
		ex.append(getLastName());
		ex.append(getFirstName());
//		ex.append(getSocialSecurity());
		ex.append(getViewSSN());
		//ex.append(getPayrollId());
		//ex.append(getSignedPaper());
		//ex.append(getPaperImageId());
		ex.append(getLoanOutCorp());
		//ex.append(getProdId());
		ex.append(getProdName());
		ex.append(getProdCo());
		//ex.append(getDepartmentId());
		ex.append(getDeptName());
		ex.append(getOffProduction());
		ex.append(getUnderContract());
		ex.append(getOccupation());
		ex.append(getOccCode());
		ex.append(getUnionNumber());
		ex.append(getRateType()); // 'S'-studio / 'L'-location
		ex.append(getRate(), 4);
		ex.append(getGuarHours());
		ex.append(getStateWorked());
		ex.append(getCityWorked());
		ex.append(getFedCorpId());
		ex.append(getStateCorpId());
		ex.append(getAccountMajor());
		ex.append(getAccountDtl());
		ex.append(getAccountFree());
		ex.append(getAccountFree2());
		ex.append(StringUtils.saveHtml(getComments()));
		ex.append(StringUtils.saveHtml(getPrivateComments()));
		StartForm sf = getStartForm();
		boolean studio = (sf.isStudioRate());
		append(ex, studio, sf.getBoxRental(), true);
		append(ex, studio, sf.getCarAllow(), true);
		append(ex, studio, sf.getMealAllow(), true);
		append(ex, sf.getMealPenalty(), true);
		append(ex, studio, sf.getMealMoney(), true);
		append(ex, studio, sf.getMealMoneyAdv(), true);
		append(ex, sf.getPerdiemNtx(), true);
		append(ex, sf.getPerdiemTx(), true);
		append(ex, sf.getPerdiemAdv(), true);
		append(ex, sf.getFringe(), true);

		ex.append(payInfo ? getGrandTotal() : null);

		ex.append(Constants.CREW_CARDS_NUMBER_JOBS); // number of PayJob occurrences
		ex.append(Constants.CREW_CARDS_NUMBER_PAYLINES); // number of PayBreakdown occurrences

		// Job definitions - ones defined + fill to 7 (crew-call max)
		int n = 0;
		for (PayJob pj : getPayJobs()) {
			pj.exportCrewcards(ex);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_JOBS) {
				break;
			}
		}
		PayJob pj = new PayJob();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_JOBS; i++) {
			pj.exportCrewcards(ex);
		}

		for (DailyTime dt : getDailyTimes()) {
			dt.exportCrewcards(ex); // basic daily info
			// now generate job/split info for each day
			flattenSplit(ex, dt, dt.getJobNum1(), true, null, payInfo);
			flattenSplit(ex, dt, dt.getJobNum2(), false, dt.getSplitStart2(), payInfo);
			flattenSplit(ex, dt, dt.getJobNum3(), false, dt.getSplitStart3(), payInfo);
		}

		// Pay Breakdowns - ones defined + fill to 30 (crew-call max)
		n = 0;
		if (payInfo) {
			for (PayBreakdown pb : getPayLines()) {
				pb.exportCrewcards(ex);
				n++;
				if (n >= Constants.CREW_CARDS_NUMBER_PAYLINES) {
					break;
				}
			}
		}
		PayBreakdown pb = new PayBreakdown();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_PAYLINES; i++) {
			pb.exportCrewcards(ex);
		}

		// Signatures - ones defined + fill to 5
		n = 0;
		for (TimecardEvent tce : getTimecardEvents()) {
			if (tce.getType().getSigned()) {
				tce.flatten(ex);
				n++;
				if (n >= Constants.CREW_CARDS_NUMBER_SIGNATURES) {
					break;
				}
			}
		}
		TimecardEvent tce = new TimecardEvent();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_SIGNATURES; i++) {
			tce.flatten(ex);
		}

		// Export Mileage form data
		if (getMileage() != null) {
			getMileage().flatten(ex);
		}
		else {
			new Mileage().flatten(ex);
		}

		// Export Box Kit Rental form data
		if (getBoxRental() != null) {
			getBoxRental().flatten(ex);
		}
		else {
			new BoxRental().flatten(ex);
		}

		// Fields added for export specification 2.2.5

		// Rate Type - hourly/daily/weekly
		ex.append(sf.getRateType().name());

		// Weekly Rate
		ex.append(getWeeklyRate());

		// Added for export specification 2.2.6 - PayExpense table - rev 4844

		// Pay Expense - ones defined + fill to 10 (export file specification)
		n = 0;
		for (PayExpense px : getExpenseLines()) {
			px.exportCrewcards(ex);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_EXPENSELINES) {
				break;
			}
		}
		PayExpense px = new PayExpense();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_EXPENSELINES; i++) {
			px.exportCrewcards(ex);
		}

		// Added for export specification 2.2.7 - ACA fields & Cities(for 3.0) rev 4990

		// output "city, state" for each day; use timecard fields if daily fields are empty.
		for (DailyTime dt : getDailyTimes()) {
			String city = dt.getCity();
			if (city == null || city.trim().length() == 0) {
				city = getCityWorked();
				if (city == null) {
					city = "";
				}
			}
			city += ", ";
			if (dt.getState() != null && dt.getState().trim().length() > 0) {
				city += dt.getState();
			}
			else {
				city += getStateWorked();
			}
			ex.append(city);
		}

		ex.append(sf.getContact().getEmailAddress());

		if (sf.getEmploymentBasis() == null) {
			ex.append(" ");
		}
		else {
			ex.append(sf.getEmploymentBasis().name().substring(0, 1));
		}
		ex.append(getAcaEndWorkDate());
		ex.append(getAcaLeaveStartDate());
		ex.append(getAcaLeaveEndDate());

		// Added for export specification 2.9.1 - Job name/number, Work zone, etc. - rev 5121+5128

		ex.append(getJobName());
		ex.append(getJobNumber());
		ex.append(getWageState());
		ex.append(getRetirementPlan()); // Note that this is 1-letter code
		ex.append(getWorkZip());
		ex.append(sf.getBoxRental().getSepCheck());
		ex.append(sf.getBoxRental().getWeekly());
		ex.append(sf.getBoxRental().getTaxable());
		ex.append(sf.getCarAllow().getSepCheck());
		ex.append(sf.getCarAllow().getWeekly());
		ex.append(sf.getCarAllow().getTaxable());
		ex.append(sf.getMealAllow().getSepCheck());

		// For 2.9.1, PayJob export, weeklyRate replaced premiumRate (rarely used)
		// For 2.9.1, PayJob export, for AICP, jobAcctNumber replaces acctDtl & jobName replaces acctSet
		// For 2.9.1, PayBreakdown export, for AICP productions, jobNumber replaces acctDtl

		for (DailyTime dt : getDailyTimes()) {
			if (dt.getWorkZone() != null) {
				ex.append(dt.getWorkZone().name());
			}
			else {
				ex.append("");
			}
			if (dt.getPhase() != null) {
				ex.append(dt.getPhase().name());
			}
			else {
				ex.append("");
			}
			ex.append(dt.getNdbEnd(),1);
			ex.append(dt.getOffProduction());
			ex.append(dt.getCameraWrap());
			ex.append(dt.getFrenchHours());
			ex.append(dt.getAccountFree());
		}

	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products. This export is more comprehensive and more organized
	 * than the CrewCards export.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public void exportTabbed(Exporter ex, boolean payInfo) {
		exportTabbed( ex, payInfo, false, null);
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products. This export is more comprehensive and more organized
	 * than the CrewCards export.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 * @param isTeamPayroll True if file is being sent to a Team payroll service.
	 * @param batchId The weeklyBatch id number, which, if not null, will be put
	 *            into the 'acct_free2' data field in the export record.
	 */
	public void exportTabbed(Exporter ex, boolean payInfo, boolean isTeamPayroll, Integer batchId) {
		Production prod = SessionUtils.getNonSystemProduction();
		boolean tours = prod.getType().isTours();
		boolean isCast = getDeptName().equalsIgnoreCase(PB_MAPPED_DEPT); // LS-1140
		StartForm sf = getStartForm();
		boolean isModelRelease = sf.getModelRelease() != null;

		// Export GS Union, Local, and Contract Code; LS-1262
		String gsContractCode = null;
		String gsUnionType = null;
		if (isTeamPayroll) {
			ContractMapping contractMapping = ContractMappingDAO.findContractMapping(sf);
			if (contractMapping != null) {
				gsContractCode = contractMapping.getGsContractCode();
				gsUnionType = contractMapping.getGsUnionType();
			}
		}

//		ex.append(getPayrollKey());
		ex.append(getId());
		ex.append(getProdId());
		ex.append(getProdName());
		ex.append(getProdCo());
		ex.append(prod.getPayrollPref().getPayrollProdId());

		if (prod.getType().isAicp()) {
			Project pj = sf.getProject();
			if (pj != null) {
				// Use project settings to be more consistent. LS-289, rev 9184
				ex.append(pj.getTitle());
				ex.append(pj.getEpisode());
			}
			else {
				ex.append(getJobName());
				ex.append(getJobNumber());
			}
		}
		else {
			String title = getProdName();
			ex.append(title); // "job name"

			// For Team Clients with the production type of TV/Episodic or Feature, do not append the end time to
			// the job name. This was causing a new project to be create for each batch of timecards that
			// were transferred. SD-2991
			if(!isTeamPayroll) {
				if (title.length() > 14) { // want max of 20, at need room for 'mmddyy'.
					title = title.substring(0, 14);
				}
				Format timecardDateFormat = new SimpleDateFormat(Constants.WEEK_END_DATE_FILE_FORMAT);
				String date = timecardDateFormat.format(getEndDate());
				title += date;
			}
			ex.append(title); // "job number"
		}
		ex.append(getUserAccount());
		ex.append(getEndDate());
		ex.append(getAdjusted());
		ex.append(getBatchStatus());
		ex.appendDateTime(getUpdated());
		//ex.append(getStatus());
		//ex.append(getHtgLocked());
		//ex.append(getApproverId());
		ex.append(getLastName());
		ex.append(getFirstName());
		ex.append(getSocialSecurity()); // was getViewSSN()
		//ex.append(getPayrollId());
		//ex.append(getSignedPaper());
		//ex.append(getPaperImageId());
		ex.append(getLoanOutCorp());
		ex.append(StringUtils.cleanTaxId(getFedCorpId()));
		ex.append(getStateCorpId());
		//ex.append(getDepartmentId());
		ex.append(getDeptName());
		ex.append(getOffProduction());
		ex.append(UnionsDAO.findGsLocalNum(sf)); // LS-1262
		ex.append(getOccupation());
		String expOccCode;
		if (StringUtils.isEmpty(getOccCode())) {
			expOccCode = getLsOccCode();
		}
		else {
			expOccCode = getOccCode();
		}
		if (isTeamPayroll) {
			expOccCode = OccupationUtils.mapOccCode(expOccCode);
		}
		ex.append(expOccCode);

		ex.append(getUnionNumber());
		if (isTeamPayroll) { // LS-1262
			ex.append(gsUnionType);
		}
		else {
			ex.append(getWageState());
		}

		ex.append(getRetirementPlan()); // Note that this is 1-letter code
		ex.append(getRateType()); // 'S'-studio / 'L'-location
		ex.append(getRate(), 4);
		ex.append(getGuarHours());
		ex.append(getCityWorked());
		ex.append(getStateWorked());
		ex.append(getWorkZip());
		ex.append("Y");//LS-3189 - Indicate interface flag as "Y" for TTCO. "V" for TTCV and "I" for Flat file interfaces
		ex.append(getAccountMajor());
		ex.append(getAccountDtl());
		ex.append(getAccountSub());
		ex.append(getAccountSet());
		if (isTeamPayroll) {
			ex.append(gsContractCode);
		}
		else {
			ex.append(getAccountFree());
		}
		if (batchId == null) {
			ex.append(getAccountFree2());
		}
		else {
			ex.append(batchId);
		}
		ex.append(StringUtils.saveHtml(getComments()));
		ex.append(StringUtils.saveHtml(getPrivateComments()));

		boolean studio = (sf.isStudioRate());
		ex.append(sf.getRateType().name().substring(0, 1)); // Rate Type - hourly/daily/weekly
		ex.append(getWeeklyRate());
		append(ex, studio, sf.getBoxRental(), false);
		append(ex, studio, sf.getCarAllow(), false);
		append(ex, studio, sf.getMealAllow(), false);
		ex.append(sf.getMealAllow().getSepCheck());
		append(ex, sf.getMealPenalty(), false);
		append(ex, studio, sf.getMealMoney(), false);
		append(ex, studio, sf.getMealMoneyAdv(), false);
		append(ex, sf.getPerdiemNtx(), false);
		append(ex, sf.getPerdiemTx(), false);
		append(ex, sf.getPerdiemAdv(), false);
		append(ex, sf.getFringe(), false);

		// Added for export specification 2.2.7 - ACA fields
		ex.append(sf.getContact().getEmailAddress());
		if (sf.getEmploymentBasis() == null) {
			ex.append(" ");
		}
		else {
			ex.append(sf.getEmploymentBasis().name().substring(0, 1));
		}
		ex.append(getAcaEndWorkDate());
		ex.append(getAcaLeaveStartDate());
		ex.append(getAcaLeaveEndDate());

		ex.append(payInfo ? getGrandTotal() : null);

		ex.append(Constants.CREW_CARDS_NUMBER_JOBS); // number of PayJob occurrences
		ex.append(Constants.TABBED_NUMBER_PAYLINES); // number of PayBreakdown occurrences

		for (DailyTime dt : getDailyTimes()) {
			dt.exportTabbed(ex, tours); // basic daily info
		}

		// Job definitions - ones defined + fill to 7 (crew-call max)
		int n = 0;
		for (PayJob pj : getPayJobs()) {
			pj.exportTabbed(ex, payInfo);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_JOBS) {
				break;
			}
		}
		PayJob pj = new PayJob();
		PayJobDaily.createPayJobDailies(pj, getEndDate());
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_JOBS; i++) {
			pj.exportTabbed(ex, false);
		}

		// Pay Breakdowns - ones defined + fill to 60 (Team daily max)
		n = 0;
		if (payInfo) {
			if (getPayDailyLines().size() > 0) {
				// Have "daily" breakdown (typical for TEAM export client)
				for (PayBreakdownDaily pb : getPayDailyLines()) {
					pb.exportTabbed(ex, isCast, isModelRelease);
					n++;
					if (n >= Constants.TABBED_NUMBER_PAYLINES) {
						break;
					}
				}
			}
			else {
				for (PayBreakdown pb : getPayLines()) {
					ex.append(getEndDate()); // keep layout same as for PayBreakdownDaily; LS-1384
					pb.exportTabbed(ex, false, false);
					n++;
					if (n >= Constants.TABBED_NUMBER_PAYLINES) {
						break;
					}
				}
			}
		}
		PayBreakdownDaily pb = new PayBreakdownDaily(); // use PayBreakdownDaily, not PayBreakdown; LS-1384
		for (int i = n; i < Constants.TABBED_NUMBER_PAYLINES; i++) {
			pb.exportTabbed(ex, false, false);
		}

		// Signatures - ones defined + fill to 5
		n = 0;
		for (TimecardEvent tce : getTimecardEvents()) {
			if (tce.getType().getSigned()) {
				tce.flatten(ex);
				n++;
				if (n >= Constants.CREW_CARDS_NUMBER_SIGNATURES) {
					break;
				}
			}
		}
		TimecardEvent tce = new TimecardEvent();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_SIGNATURES; i++) {
			tce.flatten(ex);
		}

		// Export Mileage form data
		if (getMileage() != null) {
			getMileage().flatten(ex);
		}
		else {
			new Mileage().flatten(ex);
		}

		// Export Box Kit Rental form data
		if (getBoxRental() != null) {
			getBoxRental().flatten(ex);
		}
		else {
			new BoxRental().flatten(ex);
		}

		// Pay Expense - ones defined + fill to 10 (export file specification)
		n = 0;
		for (PayExpense px : getExpenseLines()) {
			px.exportTabbed(ex);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_EXPENSELINES) {
				break;
			}
		}
		PayExpense px = new PayExpense();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_EXPENSELINES; i++) {
			px.exportTabbed(ex);
		}

		// For Tours productions, append more fields...
		if (tours) {
			ex.append(2); // Current record layout version = 2 = Tours
			ex.append(getAdjGtotal()); // 'total wages' for Tours
			for (DailyTime dt : getDailyTimes()) {
				ex.append(dt.getCountry());
				ex.append(dt.getPayAmount());
			}
		}

}

	/**
	 * @param ex
	 * @param studio
	 * @param row
	 */
	private void append(Exporter ex, boolean studio, AllowanceCap row, boolean crewCards) {
		if (! crewCards) {
			ex.append(row.getWeekly());
			ex.append(row.getTaxable());
			ex.append(row.getSepCheck());
		}
		ex.append(row.getPaymentCap());
		append(ex, studio, (RateGroup)row, crewCards);
	}

	private void append(Exporter ex, boolean studio, RateGroup row, boolean crewCards) {
		if (studio) {
			ex.append(row.getStudio());
		}
		else {
			ex.append(row.getLoc());
		}
		append(ex, row, crewCards);
	}

	private void append(Exporter ex, PerDiem row, boolean crewCards) {
		ex.append(row.getAmt());
		append(ex, (AccountCodes)row, crewCards);
	}

	private void append(Exporter ex, AccountCodes row, boolean crewCards) {
		if (! crewCards) {
			ex.append(row.getAloc());
		}
		ex.append(row.getMajor());
		ex.append(row.getDtl());
		if (! crewCards) {
			ex.append(row.getSub());
		}
		ex.append(row.getSet());
		ex.append(row.getFree());
		ex.append(row.getFree2());
	}
	/**
	 * @return A string used by Crew Cards to uniquely identify this timecard
	 * during an export/import process.
	 */
	@Transient
	private String getPayrollKey() {
		Production prod = SessionUtils.getNonSystemProduction();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
		java.text.NumberFormat nf = new DecimalFormat("00000000");

		String str = getSocialSecurity();
		if (str != null && str.length() == 9) {
			str = str.substring(5);
		}
		else {
			str = "XXXX";
		}

		str = ApplicationUtils.getProductionPrefix() +
				nf.format(prod.getId()) +
				((getFirstName() + "AAAA").substring(0, 4) +
				(getLastName() + "AAAA").substring(0, 4)).toUpperCase() +
				str +
				sdf.format(getEndDate()) +
				(getAdjusted() ? "2" : "1"); // TODO timecard sequence # for CrewCards export if more than 1 adjusted TC

		return str;
	}

	/**
	 * Output the day-split info for one split. This is the info from one line
	 * of a PayJob (job table) -- pick the right PayJob, and then pick the day
	 * that matches the DailyTime parameter, and output it.
	 *
	 * @param ex The Exporter to use for output.
	 * @param dt The DailyTime whose split information we're outputting.
	 * @param jobNum The jobNumber, 1-7, assigned to this split.
	 * @param isFirstSplit True iff this is the first split for this day.
	 * @param splitValue The split value -- either a time or percent; this
	 *            should be null for the first split. If it is null for the 2nd
	 *            or 3rd split, that indicates there is no split data, and empty
	 *            fields should be output.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	private void flattenSplit(Exporter ex, DailyTime dt, byte jobNum,
			boolean isFirstSplit, BigDecimal splitValue, boolean payInfo) {
		if ((isFirstSplit && dt.getHours() != null
				&& dt.getHours().signum() > 0) // first split & day is not empty
				|| splitValue != null) {	// or 2nd/3rd split with values
			if (!isFirstSplit) {
				ex.append(splitValue);	// split value precedes 2nd & 3rd splits
			}
			int jobs = getPayJobs().size();
			PayJob pj;
			if (jobNum <= jobs) {
				pj = getPayJobs().get(jobNum-1); // this split's PayJob
			}
			else {
				pj = new PayJob(this, jobNum);
				PayJobDaily.createPayJobDailies(pj, endDate);
			}
			PayJobDaily pd = pj.getPayJobDailies().get(dt.getDayNum()-1); // get matching day
			ex.append(pj.getAccountMajor()); // episode# goes first (major account)
			pd.exportCrewcards(ex, payInfo);					// then rest of split info (PayJobDaily)
			ex.append(jobNum);			// 2.2.4740- replace mpv2 w/ job number
		}
		else { // empty split (2nd or 3rd), or empty day (1st split and total hours = 0)
			if (!isFirstSplit) {
				ex.append((BigDecimal)null);	// split value precedes 2nd & 3rd splits
			}
			ex.append((String)null);		// prod#/episode (major account)
			new PayJobDaily().exportCrewcards(ex, false);	// empty split
			ex.append(jobNum);			// 2.2.4740- replace mpv2 w/ job number
		}
	}

	private void createDailyHours() {
		dailyHours = new BigDecimal[getDailyTimes().size()];
		int index = 0;
		for (DailyTime dt : getDailyTimes()) {
			if (dt.getWorked()) {
				dailyHours[index++] = Constants.DECIMAL_NEG_1K;
			}
			else {
				dailyHours[index++] = dt.getHours();
			}
		}
	}

	/**
	 * Copy DailyTime.payAmount into our transient array for easier JSP access.
	 */
	private void createDailyAmounts() {
		dailyAmounts = new BigDecimal[getDailyTimes().size()];
		int index = 0;
		for (DailyTime dt : getDailyTimes()) {
			dailyAmounts[index++] = dt.getPayAmount();
		}
	}

	/**
	 * Add a PayExpense item to this timecard.
	 *
	 * @param lineNum The expense table line number to be assigned to the new
	 *            entry.
	 * @param type The ExpenseType of the entry to be added.
	 * @param tax The taxable amount; may be null.
	 * @param nontax The non-taxable amount; may be null.
	 * @param qty The value to put in the PayExpense quantity field.
	 * @param codes The account codes to be assigned if a new PayExpense is
	 *            created.
	 * @param name
	 * @return The updated line number.
	 */
	public byte addExpense(byte lineNum, ExpenseType type, BigDecimal tax, BigDecimal nontax,
			BigDecimal qty, AccountCodes codes, String name) {
		if (type.getTotal() != null) {
			if (tax != null) {
				lineNum = addExpense(tax, qty, type.getTotal(), lineNum, codes, name);
			}
		}
		else {
			lineNum = addExpense(nontax, qty, type.getNonTaxable(), lineNum, codes, name);
			lineNum = addExpense(tax,    qty, type.getTaxable(),    lineNum, codes, name);
		}
		return lineNum;
	}

	/**
	 * Add a new PayExpense item to this timecard.
	 *
	 * @param pRate The value to put in the PayExpense rate field
	 * @param qty The value to put in the PayExpense quantity (hours) field.
	 * @param pc The PayCategory name to put in the PayExpense category field,
	 *            if 'name' is null.
	 * @param lineNum The expense table line number to be assigned to the new
	 *            entry.
	 * @param codes The account codes to be assigned if a new PayExpense is
	 *            created.
	 * @param name The name to put in the Category
	 * @return The updated line number.
	 */
	public byte addExpense(BigDecimal pRate, BigDecimal qty, PayCategory pc, byte lineNum,
			AccountCodes codes, String name) {
		if (pc != null && pRate != null && qty != null) {
			PayExpense exp = new PayExpense(this, lineNum);
			if (name != null && name.trim().length() > 0) {
				name = name.trim();
				if (name.length() > 30) {
					name = name.substring(0, 30);
				}
				exp.setCategory(name);
			}
			else {
				exp.setCategory(pc.getLabel());
			}
			exp.setAccountLoc(codes.getAloc());
			exp.setAccountMajor(codes.getMajor());
			exp.setAccountDtl(codes.getDtl());
			exp.setAccountSet(codes.getSet());
			exp.setAccountSub(codes.getSub());
			exp.setAccountFree(codes.getFree());
			exp.setAccountFree2(codes.getFree2());
			if (qty.signum() != 0) { // if quantity is zero, leave field null.
				// This is designed so callers can deliberately add an entry with a blank quantity,
				// in particular for Start-Form-based allowances created during timecard creation.
				exp.setQuantity(qty);
			}
			exp.setRate(pRate);
			exp.setTotal(NumberUtils.scaleTo2Places(NumberUtils.safeMultiply(qty, pRate)));
			if (exp.getTotal() == null) {
				exp.setTotal(BigDecimal.ZERO);
			}

//			exp.setProdEpisode(prodEpisode);
			expenseLines.add(lineNum, exp);
			lineNum++;
		}
		return lineNum;
	}

	/**
	 * Calculate the individual totals for each expense/advance line item, and
	 * the sum of all the line items.
	 *
	 * @param wtc The timecard being calculated.
	 * @return The total of all the expense line items.
	 */
	public BigDecimal calculateExpenses() {
		BigDecimal total = BigDecimal.ZERO;
		for (PayExpense pe : getExpenseLines()) {
			total = NumberUtils.safeAdd(total, pe.calculateTotal());
		}
		return total;
	}

	/**
	 * Calculate the line totals for all the Pay Breakdown Daily line items, and set
	 * the 'grand total' to their sum. Note that labor items line totals are
	 * rounded to 4 decimal places, while non-labor line item totals are rounded
	 * to 2 decimal places. This ensures that when we're using hourly rates that
	 * were "backed into" from a weekly guarantee, we'll end up with the right
	 * labor (weekly) total. (rev 2.9.5386)
	 *
	 * @param wtc The timecard being calculated.
	 */
	public void calculatePayBreakdownDaily() {
		// Calculate pay breakdown DAILY detail line totals
		// Detail labor line totals are rounded to 4 decimal places.
		// Detail non-labor line totals are rounded to 2 decimal places.
		BigDecimal payTot = BigDecimal.ZERO;

		for (PayBreakdownDaily pb : getPayDailyLines()) {
			BigDecimal total = pb.calculateTotal();
			if (pb.getCategoryType().includedInTotal()) { // LS-2142
				payTot = payTot.add(total);
			}
		}
		// Daily Grand total -- round to 2 decimal places
		setGrandDailyTotal(NumberUtils.scaleTo2Places(payTot));
	}

	/**
	 * Default compareTo - uses database id for equality check, then week-end
	 * Date, followed by adjusted status, last name, first name, and occupation
	 * for comparison.
	 *
	 * @param other
	 * @return Standard compare result -1/0/1.
	 */
	@Override
	public int compareTo(WeeklyTimecard other) {
		int ret = 1;
		if (other != null) {
			if (getId().equals(other.getId())) {
				return 0; // same entity
			}
			ret = compareDateAdjusted(other);
			if (ret == 0) { // Same week-ending date & adjusted; check name & occupation
				ret = compareNameOccupation(other);
				if (ret == 0) {
					// identical fields, sort by id so order will be consistent from one sort to the next
					ret = id.compareTo(other.id);
				}
			}
		}
		return ret;
	}

	/**
	 * Compare using the specified field, with the specified ordering.
	 *
	 * @param other The WeeklyTimecard to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(WeeklyTimecard other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret)); // reverse the result for descending sort.
	}

	/**
	 * Compare using the specified field.
	 *
	 * @param other The WeeklyTimecard to compare to this one.
	 * @param sortField One of the statically defined sort-key values, or null
	 *            for the default sort.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(WeeklyTimecard other, String sortField) {
		int ret;
		if (other == null) {
			ret = 1;
		}
		else if (sortField == null || sortField.equals(SORTKEY_DATE) ) {
			ret = compareTo(other); // date/adjusted/name/occupation = default comparison
		}
		else if (sortField.equals(SORTKEY_STATUS) ) {
			ret = getStatus().compareTo(other.getStatus());
			if (ret == 0) { // for equal status, sort descending (reverse) by W/E date & adjusted
				ret = - compareDateAdjusted(other);
				if (ret == 0) { // then by name & occupation
					ret = compareNameOccupation(other);
				}
			}
		}
		else if (sortField.equals(SORTKEY_NAME) ) {
			ret = compareName(other);
			if (ret == 0) { // same name, compare date/adjusted status
				ret = - compareDateAdjusted(other);
				if (ret == 0) { // same, compare occupation
					ret = getOccupation().compareToIgnoreCase(other.getOccupation());
				}
			}
		}
		else if (sortField.equals(SORTKEY_ACCT)) {
			ret = compareAccount(other);
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_MAJOR)) {
			ret = StringUtils.compareNumeric(getAccountMajor(), other.getAccountMajor());
			if (ret == 0) { // then by name
				ret = compareTo(other, SORTKEY_DTL);
				if (ret == 0) { // then by name
					ret = compareTo(other, SORTKEY_SET);
				}
			}
		}
		else if (sortField.equals(SORTKEY_DTL)) {
			ret = StringUtils.compareNumeric(getAccountDtl(), other.getAccountDtl());
			if (ret == 0) { // then by name
				ret = compareTo(other, SORTKEY_SET);
			}
		}
		else if (sortField.equals(SORTKEY_SUB)) {
			ret = StringUtils.compareNumeric(getAccountSub(), other.getAccountSub());
			if (ret == 0) { // then by name
				ret = compareTo(other, SORTKEY_SUB);
			}
		}
		else if (sortField.equals(SORTKEY_SET)) {
			ret = StringUtils.compareNumeric(getAccountSet(), other.getAccountSet());
			if (ret == 0) { // then by defaults: date/adjusted/name/occupation
				ret = compareTo(other);
			}
		}
		else if (sortField.equals(SORTKEY_FREE)) {
			ret = StringUtils.compareNumeric(getAccountFree(), other.getAccountFree());
			if (ret == 0) { // then by defaults: date/adjusted/name/occupation
				ret = compareTo(other);
			}
		}
		else if (sortField.equals(SORTKEY_BATCH)) {
			ret = compareBatch(other);
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_DATE_JOB)) {
			ret = - getEndDate().compareTo(other.getEndDate()); // note reverse order by date!
			if (ret == 0) {
				ret = getOccupation().compareTo(other.getOccupation());
			}
		}
		else if (sortField.equals(SORTKEY_DEPT)) {
			ret = StringUtils.compareIgnoreCase(getDeptName(), other.getDeptName());
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_HOURS)) {
			ret = getTotalHours().compareTo(other.getTotalHours());
			if (ret == 0) { // hours equal
				ret = compareTo(other); // apply default sort: date/adjusted/name/occupation
			}
		}
		else if (sortField.equals(SORTKEY_GROSS)) {
			ret = NumberUtils.compare(getGrandTotal(), other.getGrandTotal());
			if (ret == 0) { // gross total equal
				ret = compareTo(other); // apply default sort: date/adjusted/name/occupation
			}
		}
		else if (sortField.equals(SORTKEY_JOB_NAME)) {
			ret = StringUtils.compareIgnoreCase(getJobName(), other.getJobName());
			if (ret == 0) { // job name equal
				ret = compareTo(other); // apply default sort: date/adjusted/name/occupation
			}
		}
		else if (sortField.equals(SORTKEY_JOB_NUMBER)) {
			ret = StringUtils.compareNumeric(getJobNumber(), other.getJobNumber());
			if (ret == 0) { // job number equal
				ret = compareTo(other); // apply default sort: date/adjusted/name/occupation
			}
		}
		else if (sortField.equals(SORTKEY_OCCUPATION)) {
			ret = StringUtils.compareIgnoreCase(getOccupation(), other.getOccupation());
			if (ret == 0) { // then by name
				ret = compareName(other);
			}
		}
		else if (sortField.equals(SORTKEY_OCC_CODE)) {
			ret = StringUtils.compareNumeric(getOccCode(), other.getOccCode());
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_SSN)) {
			ret = StringUtils.compare(getViewSSNmin(), other.getViewSSNmin());
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_UNION)) {
			ret = StringUtils.compareNumeric(getUnionNumber(),other.getUnionNumber());
			if (ret == 0) { // then by name & occupation
				ret = compareNameOccupation(other);
			}
		}
		else if (sortField.equals(SORTKEY_WAITING_FOR) ) {
			// already sorted by "waiting for" field by TimecardEntry; we just need to apply secondary sorts.
			ret = compareTo(other); // apply default sort: date/adjusted/name/occupation
		}
		else { // should not happen!
			log.error("Unknown sort-field value specified =`" + sortField + "`");
			ret = 0;
		}

		return ret;
	}

	/**
	 * Compare the account number fields of two StartForms.
	 * Primary key is Major, secondary is Detail.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareAccount(WeeklyTimecard other) {
		int ret = StringUtils.compare(getAccountMajor(),other.getAccountMajor());
		if (ret == 0) {
			ret = StringUtils.compare(getAccountDtl(), other.getAccountDtl());
			if (ret == 0) {
				ret = StringUtils.compare(getAccountSub(), other.getAccountSub());
				if (ret == 0) {
					ret = StringUtils.compare(getAccountSet(), other.getAccountSet());
				}
			}
		}
		return ret;
	}

	/**
	 * Compare the batch fields of two StartForms.
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	private int compareBatch(WeeklyTimecard other) {
		WeeklyBatch batch = getWeeklyBatch();
		WeeklyBatch otherBatch = other.getWeeklyBatch();
		if (batch == null) {
			if (otherBatch == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			return batch.compareTo(otherBatch);
		}
	}

	/**
	 * Compare the week-ending date and adjusted fields between this timecard
	 * and another one.
	 *
	 * @param other The other timecard.
	 * @return Standard compare result: negative/zero/positive. The dates are
	 *         compared normally, so a more recent date is greater. An adjusted
	 *         field value of true is greater than a false value.
	 */
	public int compareDateAdjusted(WeeklyTimecard other) {
		int ret = getEndDate().compareTo(other.getEndDate());
		if (ret == 0) { // Same week-ending date ... check "adjusted" status
			if (getAdjusted() != other.getAdjusted()) {
				// adjusted=true is "more" (sorts below when ascending sort)
				ret = (getAdjusted() ? 1 : -1);
			}
		}
		return ret;
	}

	/**
	 * Compare the lastName and firstName fields between this timecard and
	 * another one.
	 *
	 * @param other The other timecard.
	 * @return Standard compare result: negative/zero/positive. The names are
	 *         compared in case-sensitive fashion.
	 */
	private int compareName(WeeklyTimecard other) {
		int ret = getLastName().compareToIgnoreCase(other.getLastName());
		if (ret == 0) {
			ret = getFirstName().compareToIgnoreCase(other.getFirstName());
		}
		return ret;
	}

	/**
	 * Compare the lastName and firstName fields between this timecard and
	 * another one, and if their both equal, then compare the occupations.
	 *
	 * @param other The other timecard.
	 * @return Standard compare result: negative/zero/positive. The names and
	 *         occupations are compared in case-sensitive fashion.
	 */
	private int compareNameOccupation(WeeklyTimecard other) {
		int ret = compareName(other);
		if (ret == 0) {
			ret = StringUtils.compareIgnoreCase(getOccupation(), other.getOccupation());
		}
		return ret;
	}

	/**
	 * Generate the text for an SQL "order by" clause for the given sortCol.
	 *
	 * @param sortCol The column of interest; should be one of the static
	 *            literals defined in this class.
	 * @return A string appropriate for use in an SQL query as the "sort by"
	 *         string appropriate for the given sortCol.
	 */
	public static String getSqlSortKey(String sortCol, boolean ascending) {
		String key = "";
		String order = ascending ? " asc " : " desc ";
		String dateOrder = ascending ? " desc " : " asc "; // date uses reverse sort

		if (sortCol == null) {
			key = "";
		}
		else if (sortCol.equals(SORTKEY_DATE)) {
			// Date uses reverse order
			key = " End_Date ";
			key += dateOrder + ", ";
		}
		else {
			if (sortCol.equals(SORTKEY_NAME)) {
				key = null; // use default
			}
			else if (sortCol.equals(SORTKEY_ACCT) || sortCol.equals(SORTKEY_MAJOR)) {
				key = " account_major" + order + ", account_dtl" + order +
						", account_set" + order + ", free" + order + ", free2";
			}
			else if (sortCol.equals(SORTKEY_DTL)) {
				key = "account_dtl" + order + ", account_major" + order +
						", account_set" + order + ", free" + order + ", free2";
			}
			else if (sortCol.equals(SORTKEY_SET)) {
				key = " account_set";
						//+ order + ", account_major" + order +
						//", account_dtl" + order + ", free" + order + ", free2";
			}
			else if (sortCol.equals(SORTKEY_FREE)) {
				key = " free" + order + ", free2";
			}
			else if (sortCol.equals(SORTKEY_BATCH)) {
				key = "Weekly_Batch_Id"; // not by name, but at least together!
			}
			else if (sortCol.equals(SORTKEY_DEPT)) {
				key = " dept_name";
			}
			else if (sortCol.equals(SORTKEY_GROSS)) {
				key = " Grand_Total";
			}
			else if (sortCol.equals(SORTKEY_HOURS)) {
				key = null; // TODO can't sort transient value in SQL - Discrepancy report
			}
			else if (sortCol.equals(SORTKEY_JOB_NAME)) {
				key = " job_name";
			}
			else if (sortCol.equals(SORTKEY_JOB_NUMBER)) {
				key = " job_number";
			}
			else if (sortCol.equals(SORTKEY_OCCUPATION)) {
				key = " Occupation";
			}
			else if (sortCol.equals(SORTKEY_OCC_CODE)) {
				key = " Occ_Code";
			}
			else if (sortCol.equals(SORTKEY_SSN)) {
				key = null; // TODO can't sort encrypted field in SQL - Discrepancy report
			}
			else if (sortCol.equals(SORTKEY_STATUS)) {
				key = null; // TODO can't sort transient value in SQL - Discrepancy report
			}
			else if (sortCol.equals(SORTKEY_UNION)) {
				key = " Union_Number";
			}

			if (key == null) {
				key = "";
			}
			else {
				key += order + ", ";
			}
		}
		key += " last_name " + order + ", first_name" + order
				+ ", end_date" + dateOrder + ", Occupation" + order;
		return key;
	}

	/**
	 * Create a clone of this timecard. NOTE, though, that the clone will be in
	 * "OPEN" (not submitted) status, regardless of this instance's status, and
	 * will not have any signatures. One reason for this is that this is used
	 * for copying timecards from one production to another for testing
	 * purposes, and there's no need to try and copy the approval status or
	 * history.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public WeeklyTimecard clone() {
		WeeklyTimecard wtc;
		try {
			wtc = (WeeklyTimecard)super.clone();
			wtc.id = null;
			wtc.status = ApprovalStatus.OPEN;
			wtc.startForm = null;
			wtc.weeklyBatch = null;
			wtc.approverId = null;
			wtc.lockedBy = null;
			wtc.markedForApproval = false;
			wtc.paperImage = null;
			wtc.department = null;
			wtc.departmentId = null;
			wtc.timecardEvents = null;
			wtc.dailyTimes = null;
			wtc.payJobs = null;
			wtc.payLines = null;
			wtc.payDailyLines = null;
			wtc.expenseLines = null;
			wtc.boxRental = null;
			wtc.mileage = null;
			wtc.totalDprHours = null;
			wtc.totalDprMpv1 = 0;
			wtc.totalDprMpv2 = 0;
		}
		catch (CloneNotSupportedException e) {
			log.error("WeeklyTimecard clone error: ", e);
			return null;
		}
		return wtc;
	}

	/**
	 * @return A copy of this object, including separate copies of many of the
	 *         included data objects, which encompasses all of the data! (This
	 *         is significantly different than the clone() method, in which all
	 *         the embedded objects in the returned copy are either the same
	 *         instances as those in the original, or null.)
	 */
	public WeeklyTimecard deepCopy() {
		WeeklyTimecard wtc = clone();
		wtc.dailyTimes = new ArrayList<>();
		for (DailyTime dt : dailyTimes) {
			DailyTime ndt = dt.clone();
			ndt.setWeeklyTimecard(wtc);
			wtc.dailyTimes.add(ndt);
		}
//		wtc.payJobs = new ArrayList<PayJob>();
//		for (PayJob pj : payJobs) {
//			PayJob p = pj.clone();
//			wtc.payJobs.add(p);
//		}
//		wtc.payLines = new ArrayList<PayBreakdown>();
//		for (PayBreakdown pb : payLines) {
//			PayBreakdown p = pb.clone();
//			wtc.payLines.add(p);
//		}
		wtc.expenseLines = new ArrayList<>();
		for (PayExpense pe : expenseLines) {
			PayExpense p = pe.clone();
			p.setWeeklyTimecard(wtc);
			wtc.expenseLines.add(p);
		}
		if (boxRental != null) {
			wtc.boxRental = boxRental.clone();
			wtc.boxRental.setWeeklyTimecard(wtc);
		}

		if (mileage != null) {
			wtc.mileage = mileage.deepCopy();
			wtc.mileage.setWeeklyTimecard(wtc);
		}
		return wtc;
	}

	/**
	 * Create a deep copy of this timecard in preparation for adding it to a
	 * different Production.
	 *
	 * @param prod The Production to which the copy will be added.
	 * @param project
	 * @return A (deep) copy of this object, including separate copies of many
	 *         of the included data objects. Fields associated with or derived
	 *         from the owning Production are changed to relate to the given
	 *         Production parameter, e.g., the department reference and
	 *         production name.
	 */
	public WeeklyTimecard deepCopyFor(Production prod, Project project) {
		WeeklyTimecard wtc = deepCopy();
		wtc.prodId = prod.getProdId();
		wtc.prodName = prod.getTitle();
		wtc.prodCo = prod.getStudio();
		if (wtc.deptName != null) {
			wtc.department = DepartmentDAO.getInstance().findByProductionAndNameAny(prod, project, wtc.deptName);
			if (wtc.department != null) {
				wtc.departmentId = wtc.department.getId();
			}
		}
		if (prod.getPayrollPref().getAccountMajor() != null) {
			String oldAcctMajor = wtc.getAccountMajor();
			wtc.setAccountMajor(prod.getPayrollPref().getAccountMajor());
			for (PayExpense pe : wtc.expenseLines) {
				if (pe.getAccountMajor() != null && pe.getAccountMajor().equals(oldAcctMajor)) {
					pe.setAccountMajor(wtc.getAccountMajor());
				}
			}
		}
		return wtc;
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getFirstName()==null ? "null" : getFirstName());
		s += " " + (getLastName()==null ? "null" : getLastName());
		s += ", ssn=" + (getSocialSecurity()==null ? "null" : getViewSSN());
		s += ", occ=" + (getOccupation()==null ? "null" : getOccupation());
		s += ", date=" + (getEndDate()==null ? "null" : getEndDate());
//		s += ", major=" + (getAccountMajor()==null ? "null" : getAccountMajor());
//		s += ", dtl=" + (getAccountDtl()==null ? "null" : getAccountDtl());
		if (getLockedBy() != null) {
			s += ", *LOCKED*=" + getLockedBy();
		}
//		s += ", objId=" + super.hashCode();
		s += "]";
		return s;
	}

	@Transient
	@Override
	public Production getProduction() {
		return ProductionDAO.getInstance().findByProdId(getProdId());
	}

	/** See {@link #cities} */
	@Transient
	public void setCities(List<SelectItem> cities) {
		this.cities = cities;
	}

}
