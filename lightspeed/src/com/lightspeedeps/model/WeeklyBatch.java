package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.WeeklyBatchStatus;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * WeeklyBatch entity. This implements a "batch" of timecards, which allows
 * a group of WeeklyTimecard objects to be manipulated as a unit by an accountant,
 * for example, for transmittal to a payroll service.
 */
@Entity
@Table(name = "weekly_batch")
@JsonIgnoreProperties({"handler", "hibernateLazyInitializer"}) // used by generateJsonSchema()
public class WeeklyBatch extends PersistentObject<WeeklyBatch>
		implements Comparable<WeeklyBatch>, Cloneable {
	//@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyBatch.class);

	private static final long serialVersionUID = 1L;

	// SORT KEYS

	public static final String SORTKEY_NAME = "name";
	public static final String SORTKEY_DATE = "date";
	private static final String SORTKEY_COUNT = "count";
	private static final String SORTKEY_GROSS = "gross";
	public static final String SORTKEY_SENT_DATE = "sentdate";
	private static final String SORTKEY_SENT = "sent";
	private static final String SORTKEY_EDIT = "edit";
	private static final String SORTKEY_FINAL = "final";
	private static final String SORTKEY_PAID = "paid";
	private static final String SORTKEY_STATUS = "status";
	private static final String SORTKEY_TC_STATUS = "tcstatus";
	public static final String SORTKEY_UPDATED = "updated";
	private static final String SORTKEY_JOB_NAME = "jobName";
	private static final String SORTKEY_JOB_NUMBER = "jobNumber";

	public static final int MAX_NAME_LENGTH = 40;

	// Fields

	/** The Production.prodId field for the Production that contains the
	 * timecards in this batch. This is included for the JSON output stream. */
	@Transient
	private String lsProductionId;

	/** The Production.payrollProdId field for the Production that contains the
	 * timecards in this batch. */
	private String prProductionId;

	/** The Production associated with this WeeklyBatch.  All the WeeklyTimecard`s
	 * in this batch will be from this same Production. */
	private Production production;

	/** The Project associated with this WeeklyBatch.  Only used for Commercial
	 * productions.  For TV & Feature productions, this will be null. */
	private Project project;

//	/** The ProductionBatch that was the 'template' for creating this
//	 * WeeklyBatch. */
//	private ProductionBatch productionBatch;

	/** The name of this WeeklyBatch.  This initially is copied from the
	 * ProductionBatch, but may later be edited by the user. */
	private String name;

	/** The week-ending date of the timecards in this WeeklyBatch. */
	private Date endDate;

	/** The current status of this WeeklyBatch. */
	private WeeklyBatchStatus status;

	/** True iff this batch is in a Commercial production, and it contains one
	 * or more timecards belonging to a Project different than this batch's
	 * Project. */
	private boolean aggregate;

	/** If true, then all the timecards in this batch should have the corresponding
	 * workersComp flag as true. If false, all the timecards should be false. LS-1535 */
	private boolean workersComp;

	/** True if this batch will be skipped by the external payroll service process. */
	private boolean skipFlag;

	/** The most recent time that any timecard in this batch was changed; i.e.,
	 * the max of all the WeeklyTimecard.updated values. */
	private Date lastUpdated;

	/** The last time this batch of timecards was sent to the payroll service. */
	private Date sent;

	/** When batch was processed by payroll service. (Team: updated by external process.) LS-1535 */
	private Date processed;

	/** When batch was 'interfaced' by payroll service. (Team: updated by external process.) LS-1535 */
	private Date interfaced;

	/** p\Payroll-specific Invoice Number for the batch.  (Team: updated by external process.) LS-1535 */
	private Integer invoiceNumber;

	/** Payroll-specific Project identifier (Team: updated by external process.) LS-1535 */
	private String payrollProjectId;

	/** The number of timecards in the current set that are marked as Sent. */
	private int timecardsSent;

	/** The number of timecards in the current set that are marked as Edit. */
	private int timecardsEdit;

	/** The number of timecards in the current set that are marked as Final. */
	private int timecardsFinal;

	/** The number of timecards in the current set that are marked as Paid. */
	private int timecardsPaid;

	/** The total, for all the timecards in this batch, of WeeklyTimecard.grandTotal.
	 * NOTE: this value may be out of date compared to the timecards, as we
	 * allow a WeeklyTimecard to be updated without forcing an update to the
	 * WeeklyBatch that contains it. The updateGross() method should be called
	 * before using this field. */
	private BigDecimal gross;

	/** The status of the "least complete" timecard out of the timecards in this batch. */
	private ApprovalStatus timecardStatus;

	/** The number of timecards in the batch at the time of sending.
	 * This field is defined simply to control the ordering in the JSON output. */
	@Transient
	private int timecardCount;

	// 4/13/2018 - Removing WeeklyBatchEvent - this was never used.
//	/** The collection of events describing the history of this batch. */
//	private List<WeeklyBatchEvent> weeklyBatchEvents = new ArrayList<WeeklyBatchEvent>(0);

	/** The collection of timecards contained in this batch. */
	private Set<WeeklyTimecard> timecards = new HashSet<>(0);

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** Used to track checkbox status on List page. */
	@Transient
	private boolean checked;

	/** The job name  in this batch. */
	private String jobName;

	/** The job code in this batch. */
	private String jobCode;

	/** The G/L Account Code in this batch. */
	private String glAccountCode;

	/** The Cost Center in this batch. */
	private String costCenter;

	/** The Comments in this batch. */
	private String comments;

	// Constructors

	/**
	 * Default constructor; the 'lastUpdated' field will be set to the current
	 * date & time.
	 */
	public WeeklyBatch() {
		status = WeeklyBatchStatus.OPEN;
		timecardStatus = ApprovalStatus.BATCH_INITIAL_STATUS;
		gross = BigDecimal.ZERO;
		lastUpdated = new Date();
	}

	/**
	 * Our usual constructor; the 'lastUpdated' field will be set to the current
	 * date & time.
	 *
	 * @param prod The Production this batch is part of.
	 * @param proj The Project this batch is part of -- null for TV and Feature
	 *            productions, non-null for Commercial productions.
	 * @param batchName The name to be assigned to the WeeklyBatch.
	 * @param weDate The week-ending date of the batch -- all timecards in the
	 *            batch have the same week-ending date.
	 */
	public WeeklyBatch(Production prod, Project proj, String batchName, Date weDate) {
		this();
		production = prod;
		project = proj;
		name = batchName;
		endDate = weDate;
		lsProductionId = production.getProdId();
	}

	// Property accessors and mutators

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #project}. */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/**See {@link #lsProductionId}. */
	@Transient
	public String getLsProductionId() {
		return getProduction().getProdId();
	}

	/**See {@link #prProductionId}. */
	@Transient
	public String getPrProductionId() {
		return prProductionId;
	}
	/**See {@link #prProductionId}. */
	public void setPrProductionId(String prProductionId) {
		this.prProductionId = prProductionId;
	}

//	@JsonIgnore
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Production_Batch_Id")
//	public ProductionBatch getProductionBatch() {
//		return productionBatch;
//	}
//	public void setProductionBatch(ProductionBatch productionBatch) {
//		this.productionBatch = productionBatch;
//	}

	@Column(name = "Name", nullable = false, length = 40)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Date", nullable = false, length = 10)
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date date) {
		endDate = date;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public WeeklyBatchStatus getStatus() {
		return status;
	}
	public void setStatus(WeeklyBatchStatus status) {
		this.status = status;
	}

	/** See {@link #aggregate}. */
	@JsonIgnore
	@Column(name = "Aggregate", nullable = false)
	public boolean getAggregate() {
		return aggregate;
	}
	/** See {@link #aggregate}. */
	public void setAggregate(boolean isAggregate) {
		aggregate = isAggregate;
	}

	/** See {@link #workersComp}. */
	@Column(name = "workers_comp", nullable = false)
	public boolean getWorkersComp() {
		return workersComp;
	}
	/** See {@link #workersComp}. */
	public void setWorkersComp(boolean workersComp) {
		this.workersComp = workersComp;
	}

	/** See {@link #skipFlag}. */
	@Column(name = "Skip_Flag", nullable = false)
	public boolean getSkipFlag() {
		return skipFlag;
	}
	/** See {@link #skipFlag}. */
	public void setSkipFlag(boolean skipFlag) {
		this.skipFlag = skipFlag;
	}

	/**See {@link #timecardStatus}. */
	@JsonIgnore
	@Enumerated(EnumType.STRING)
	@Column(name = "Timecard_Status", nullable = false, length = 30)
	public ApprovalStatus getTimecardStatus() {
		return timecardStatus;
	}
	/**See {@link #timecardStatus}. */
	public void setTimecardStatus(ApprovalStatus timecardStatus) {
		this.timecardStatus = timecardStatus;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Updated", nullable = false, length = 19)
	public Date getLastUpdated() {
		return lastUpdated;
	}
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Sent", length = 19)
	public Date getSent() {
		return sent;
	}
	public void setSent(Date sent) {
		this.sent = sent;
	}

	/** See {@link #processed}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "processed", length = 19)
	public Date getProcessed() {
		return processed;
	}
	/** See {@link #processed}. */
	public void setProcessed(Date processed) {
		this.processed = processed;
	}

	/** See {@link #interfaced}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "interfaced", length = 19)
	public Date getInterfaced() {
		return interfaced;
	}
	/** See {@link #interfaced}. */
	public void setInterfaced(Date interfaced) {
		this.interfaced = interfaced;
	}

	/** See {@link #invoiceNumber}. */
	@Column(name = "Invoice_Number")
	public Integer getInvoiceNumber() {
		return invoiceNumber;
	}
	/** See {@link #invoiceNumber}. */
	public void setInvoiceNumber(Integer invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	/** See {@link #payrollProjectId}. */
	@Column(name = "Payroll_Project_Id", nullable = true, length = 100)
	public String getPayrollProjectId() {
		return payrollProjectId;
	}
	/** See {@link #payrollProjectId}. */
	public void setPayrollProjectId(String projectName) {
		this.payrollProjectId = projectName;
	}

	/** The number of timecards in this batch. Included in JSON output file. */
	@Transient
	public int getTimecardCount() {
		return getTimecards().size();
	}

	/**See {@link #gross}. */
	@JsonIgnore
	@Column(name = "Gross", precision = 10, scale = 2)
	public BigDecimal getGross() {
		return gross;
	}
	/**See {@link #gross}. */
	public void setGross(BigDecimal gross) {
		this.gross = gross;
	}

	/**See {@link #timecardsSent}. */
	@JsonIgnore
	@Column(name = "Timecards_Sent")
	public int getTimecardsSent() {
		return timecardsSent;
	}
	/**See {@link #timecardsSent}. */
	public void setTimecardsSent(int timecardsSent) {
		this.timecardsSent = timecardsSent;
	}

	/**See {@link #timecardsEdit}. */
	@JsonIgnore
	@Column(name = "Timecards_Edit")
	public int getTimecardsEdit() {
		return timecardsEdit;
	}
	/**See {@link #timecardsEdit}. */
	public void setTimecardsEdit(int timecardsEdit) {
		this.timecardsEdit = timecardsEdit;
	}

	/**See {@link #timecardsFinal}. */
	@JsonIgnore
	@Column(name = "Timecards_Final")
	public int getTimecardsFinal() {
		return timecardsFinal;
	}
	/**See {@link #timecardsFinal}. */
	public void setTimecardsFinal(int timecardsFinal) {
		this.timecardsFinal = timecardsFinal;
	}

	/**See {@link #timecardsPaid}. */
	@JsonIgnore
	@Column(name = "Timecards_Paid")
	public int getTimecardsPaid() {
		return timecardsPaid;
	}
	/**See {@link #timecardsPaid}. */
	public void setTimecardsPaid(int timecardsPaid) {
		this.timecardsPaid = timecardsPaid;
	}

	// 4/13/2018 - Removing WeeklyBatchEvent - this was never used.
//	@JsonIgnore
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyBatch")
//	@OrderBy("date")
//	public List<WeeklyBatchEvent> getWeeklyBatchEvents() {
//		return weeklyBatchEvents;
//	}
//	public void setWeeklyBatchEvents(List<WeeklyBatchEvent> weeklyBatchEvents) {
//		this.weeklyBatchEvents = weeklyBatchEvents;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyBatch")
	public Set<WeeklyTimecard> getTimecards() {
		return timecards;
	}
	public void setTimecards(Set<WeeklyTimecard> weeklyTimeCards) {
		timecards = weeklyTimeCards;
	}

	/**See {@link #checked}. */
	@JsonIgnore
	@Transient
	public boolean getChecked() {
		return checked;
	}
	/**See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
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

	@JsonIgnore
	@Transient
	public boolean isUnBatched() {
		return (id == null || id < 0);
	}

	/** See {@link #jobName}. */
	@Column(name = "Job_Name")
	public String getJobName() {
		return jobName;
	}
	/** See {@link #jobName}. */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/** See {@link #jobCode}. */
	@Column(name = "Job_Code")
	public String getJobCode() {
		return jobCode;
	}
	/** See {@link #jobCode}. */
	public void setJobCode(String jobCode) {
		this.jobCode = jobCode;
	}

	/** See {@link #glAccountCode}. */
	@Column(name = "Gl_Account_Code")
	public String getGlAccountCode() {
		return glAccountCode;
	}
	/** See {@link #glAccountCode}. */
	public void setGlAccountCode(String glAccountCode) {
		this.glAccountCode = glAccountCode;
	}

	/** See {@link #costCenter}. */
	@Column(name = "Cost_Center")
	public String getCostCenter() {
		return costCenter;
	}
	/** See {@link #costCenter}. */
	public void setCostCenter(String costCenter) {
		this.costCenter = costCenter;
	}

	/** See {@link #comments}. */
	public String getComments() {
		return comments;
	}
	/** See {@link #comments}. */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * Update this batch's total gross value from the timecards which it
	 * contains. The gross value may be out of date compare to the timecards if
	 * this method is not called, as we allow a WeeklyTimecard to be updated
	 * without forcing an update to the WeeklyBatch that contains it.
	 */
	public void updateGross() {
		BigDecimal total = BigDecimal.ZERO;
		for (WeeklyTimecard wtc : getTimecards()) {
			if (wtc.getGrandTotal() != null) {
				total = total.add(wtc.getGrandTotal());
			}
		}
		NumberUtils.scaleTo2Places(total);
		setGross(total);
	}

//	public static Comparator<WeeklyBatch> nameComparator = new Comparator<WeeklyBatch>() {
//		@Override
//		public int compare(WeeklyBatch one, WeeklyBatch two) {
//			int ret = one.getName().compareTo(two.getName());
//			if (ret == 0) {
//				ret = one.getEndDate().compareTo(two.getEndDate());
//			}
//			return ret;
//		}
//	};

	/**
	 * Compare this instance with another instance of WeeklyBatch, based on the
	 * value of the particular field specified. Note that an "unbatched" batch
	 * should always sort to the top of the list!
	 *
	 * @param other The other WeeklyBatch to be compared against.
	 * @param sortField Which field is the primary sort.
	 * @param ascending True if the items are being sorted in ascending order,
	 *            false for descending order.
	 * @return standard -1/0/1 values for compareTo
	 */
	public int compareTo(WeeklyBatch other, String sortField, boolean ascending) {
		int ret;
		if (other == null) {
			ret = 1;
		}
		else if (isUnBatched()) {
			ret = -1;
		}
		else if (other.isUnBatched()) {
			ret = 1;
		}
		else {
			ret = compareTo(other, sortField);
			if (! ascending) {
				ret = 0-ret; // reverse the result for descending sort.
			}
		}
		return ret;
	}

	/**
	 * Compare this instance with another instance of WeeklyBatch, based on the
	 * value of the particular field specified. Note that an "unbatched" batch
	 * should always sort to the top of the list!
	 *
	 * @param other The WeeklyBatch to which this object should be compared.
	 * @param sortField The name of the field to compare on. This class defines
	 *            the acceptable field names as static final Strings.
	 * @return Standard compare result: negative/zero/positive
	 */
	public int compareTo(WeeklyBatch other, String sortField) {
		int ret;
		if (other == null) {
			ret = 1;
		}
		else if (isUnBatched()) {
			ret = -1;
		}
		else if (other.isUnBatched()) {
			ret = 1;
		}
		else if (sortField == null || sortField.equals(SORTKEY_DATE) ) {
			ret = CalendarUtils.compare(getEndDate(),other.getEndDate());
		}
		else if (sortField.equals(SORTKEY_NAME) ) {
			ret = StringUtils.compareIgnoreCase(getName(), other.getName());
		}
		else if (sortField.equals(SORTKEY_JOB_NAME) ) {
			ret = StringUtils.compareIgnoreCase(getProject().getTitle(), other.getProject().getTitle());
		}
		else if (sortField.equals(SORTKEY_JOB_NUMBER) ) {
			ret = StringUtils.compareIgnoreCase(getProject().getEpisode(), other.getProject().getEpisode());
		}
		else if (sortField.equals(SORTKEY_COUNT) ) {
			ret = NumberUtils.compare(getTimecardCount(), other.getTimecardCount());
		}
		else if (sortField.equals(SORTKEY_UPDATED) ) {
			ret = CalendarUtils.compare(getLastUpdated(), other.getLastUpdated());
		}
		else if (sortField.equals(SORTKEY_SENT_DATE) ) {
			ret = CalendarUtils.compare(getSent(), other.getSent());
		}
		else if (sortField.equals(SORTKEY_GROSS) ) {
			ret = NumberUtils.compare(getGross(), other.getGross());
		}
		else if (sortField.equals(SORTKEY_SENT) ) {
			ret = NumberUtils.compare(getTimecardsSent(), other.getTimecardsSent());
		}
		else if (sortField.equals(SORTKEY_EDIT) ) {
			ret = NumberUtils.compare(getTimecardsEdit(), other.getTimecardsEdit());
		}
		else if (sortField.equals(SORTKEY_FINAL) ) {
			ret = NumberUtils.compare(getTimecardsFinal(), other.getTimecardsFinal());
		}
		else if (sortField.equals(SORTKEY_PAID) ) {
			ret = NumberUtils.compare(getTimecardsPaid(), other.getTimecardsPaid());
		}
		else if (sortField.equals(SORTKEY_STATUS) ) {
			ret = getStatus().compareTo(other.getStatus());
		}
		else if (sortField.equals(SORTKEY_TC_STATUS) ) {
			ret = getTimecardStatus().compareTo(other.getTimecardStatus());
		}
		else { // should not happen!
			log.error("Unknown sort-field value specified");
			ret = 0;
		}

		if (ret == 0 && sortField != null && other != null && ! sortField.equals(SORTKEY_NAME)) {
			ret = StringUtils.compareIgnoreCase(getName(), other.getName());
		}

		return ret;
	}

	/**
	 * @param other
	 * @return Standard compare result: negative/zero/positive
	 */
	@Override
	public int compareTo(WeeklyBatch other) {
		if (other == null) {
			return 1;
		}
		return StringUtils.compareIgnoreCase(getName(), other.getName());
	}

	@Override
	public WeeklyBatch clone() {
		WeeklyBatch batch;
		try {
			batch = (WeeklyBatch)super.clone();
			batch.id = null;
//			batch.weeklyBatchEvents = new ArrayList<WeeklyBatchEvent>(0);
			batch.timecards = new HashSet<>(0);
		}
		catch (CloneNotSupportedException e) {
			log.error(e);
			return null;
		}
		return batch;
	}

}
