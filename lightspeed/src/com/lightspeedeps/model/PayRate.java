package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * PayRate entity. Holds hourly, daily, and weekly pay rates by occupation
 * code for all union positions.
 */
@Entity
@Table(name = "pay_rate",
		uniqueConstraints = @UniqueConstraint(columnNames = {"Start_Date", "Contract_Code", "Schedule", "Occ_Code" }))
public class PayRate extends PersistentObject<PayRate> implements Comparable<PayRate> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayRate.class);

	/** */
	private static final long serialVersionUID = 1L;

	/** Key indicating sort by occupation code */
	public static final String SORTKEY_OCC_CODE = "occCode";
	/** Key indicating sort by Schedule code */
	public static final String SORTKEY_SCHEDULE = "schedule";
	/** Key indicating sort by Occupation */
	public static final String SORTKEY_JOB = "job";
	/** Key indicating sort by Contract Code */
	public static final String SORTKEY_CONTRACT = "contract";
	/** Key indicating sort by guaranteed hours */
	public static final String SORTKEY_GUAR_HOURS = "guarHours";
	/** Key indicating sort by Hourly rate */
	public static final String SORTKEY_HOURLY_RATE = "hourlyRate";
	/** Key indicating sort by Daily rate */
	public static final String SORTKEY_DAILY_RATE = "dailyRate";
	/** Key indicating sort by Weekly rate */
	public static final String SORTKEY_WEEKLY_RATE = "weeklyRate";

	// Fields

	/** First effective date of these rates. */
	private Date startDate;

	/** Last effective date of these rates.*/
	private Date endDate;

	/** Indicates whether rate applies to Studio(S), Distant(D), or All(A) */
	private char locality;
	public final static char LOCALITY_STUDIO = 'S';
	public final static char LOCALITY_DISTANT = 'D';
	public final static char LOCALITY_ALL = 'A';

	/** A String denoting which contract and production type this pay rate
	 * relates to. */
	private String contractCode;

	/** The field used to search the PayRate table given a particular Occupation
	 * entry. This usually identifies an entry down to the union and any
	 * production-type restriction(s) that affects the rate, e.g., Indie vs
	 * Major, TV Season, etc. */
	private String contractRateKey;

	/** The original contract schedule code, typically alphabetic. */
	private String contractSchedule;

	/** The numeric contract schedule code, typically assigned by EP. */
	private String schedule;

	/** The Lightspeed-assigned Occupation Code for a specific occupation. These
	 * codes will match the "standard" Occ Code value unless that would cause a duplication.
	 * Also, Lightspeed may assign occupation codes for occupations that do not
	 * have an assigned code, e.g., occupations from the DGA contract. */
	private String occCode;

	/** Rate type - H-hourly, D-daily, W-weekly */
	private char rateType;

	/** The number of guaranteed hours. */
	private BigDecimal guarHours;

	/** The hourly rate for this occupation and schedule; may be null. */
	private BigDecimal hourlyRate;

	/** The daily rate for this occupation and schedule; may be null. */
	private BigDecimal dailyRate;

	/** The weekly rate for this occupation and schedule; may be null. */
	private BigDecimal weeklyRate;

	@Transient
	private String occupation;

	/** */
//	private String occRuleKey;

	// Constructors

	/** default constructor */
	public PayRate() {
	}

//	/** minimal constructor */
//	public PayRate(String contractCode, String schedule, String occCode) {
//		this.contractCode = contractCode;
//		this.schedule = schedule;
//		this.occCode = occCode;
//	}

	// Property accessors

	/**See {@link #startDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", nullable = false, length = 0)
	public Date getStartDate() {
		return startDate;
	}
	/**See {@link #startDate}. */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**See {@link #endDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", nullable = false, length = 0)
	public Date getEndDate() {
		return endDate;
	}
	/**See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**See {@link #locality}. */
	@Column(name = "Locality", length = 1)
	public char getLocality() {
		return locality;
	}
	/**See {@link #locality}. */
	public void setLocality(char locality) {
		this.locality = locality;
	}

	@Column(name = "Contract_Code", nullable = false, length = 20)
	public String getContractCode() {
		return contractCode;
	}
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}

//	/**See {@link #contractRuleKey}. */
//	@Column(name = "Contract_Rule_Key", nullable = false, length = 30)
//	public String getContractRuleKey() {
//		return contractRuleKey;
//	}
//	/**See {@link #contractRuleKey}. */
//	public void setContractRuleKey(String contractRuleKey) {
//		this.contractRuleKey = contractRuleKey;
//	}

	/** See {@link #contractRateKey}. */
	@Column(name = "Contract_Rate_Key", nullable = false, length = 30)
	public String getContractRateKey() {
		return contractRateKey;
	}
	/** See {@link #contractRateKey}. */
	public void setContractRateKey(String contractRateKey) {
		this.contractRateKey = contractRateKey;
	}

	/**See {@link #contractSchedule}. */
	@Column(name = "Contract_Schedule", length = 10)
	public String getContractSchedule() {
		return contractSchedule;
	}
	/**See {@link #contractSchedule}. */
	public void setContractSchedule(String contractSchedule) {
		this.contractSchedule = contractSchedule;
	}

	@Column(name = "Schedule", nullable = false, length = 10)
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	@Column(name = "Occ_Code", nullable = false, length = 10)
	public String getOccCode() {
		return occCode;
	}
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	@Transient
	public String getOccupation() {
		return occupation;
	}
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #rateType}. */
	@Column(name = "Rate_Type", nullable = false)
	public char getRateType() {
		return rateType;
	}
	/** See {@link #rateType}. */
	public void setRateType(char rateType) {
		this.rateType = rateType;
	}

	@Transient
	public EmployeeRateType getEmployeeRateType() {
		EmployeeRateType type = null;
		switch(getRateType()) {
			case 'H':
			default:
				type = EmployeeRateType.HOURLY;
				break;
			case 'D':
				type = EmployeeRateType.DAILY;
				break;
			case 'W':
				type = EmployeeRateType.WEEKLY;
				break;
		}
		return type;
	}

	/** True iff this entry (occ Code and schedule) defines a Weekly On-Call
	 * person -- one who does not report hours, just days worked. */
	@Transient
	public boolean getWeekly() {
		return rateType == 'W';
	}

	/**See {@link #guarHours}. */
	@Column(name = "Guar_Hours", precision = 5, scale = 2)
	public BigDecimal getGuarHours() {
		return guarHours;
	}
	/**See {@link #guarHours}. */
	public void setGuarHours(BigDecimal guarHours) {
		this.guarHours = guarHours;
	}

	@Column(name = "Hourly_Rate", precision = 9, scale = 4)
	public BigDecimal getHourlyRate() {
		return hourlyRate;
	}
	public void setHourlyRate(BigDecimal hourlyRate) {
		this.hourlyRate = hourlyRate;
	}

	@Column(name = "Daily_Rate", precision = 9, scale = 4)
	public BigDecimal getDailyRate() {
		return dailyRate;
	}
	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	@Column(name = "Weekly_Rate", precision = 8, scale = 2)
	public BigDecimal getWeeklyRate() {
		return weeklyRate;
	}
	public void setWeeklyRate(BigDecimal weeklyRate) {
		this.weeklyRate = weeklyRate;
	}

	/**
	 * @return True if at least one dollar rate (hour, daily, or weekly) is not
	 *         null. I.e., returns false only if all three rates are null.
	 */
	@Transient
	public boolean getHasRate() {
		return getHourlyRate() != null || getDailyRate() != null || getWeeklyRate() != null;
	}

	/**
	 * Compare this PayRate to another one by the default sort field, in
	 * ascending order.
	 *
	 * @param other The PayRate to which this instance will be compared.
	 * @return The standard -1/0/1 for Java compare operations.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(PayRate other) {
		int ret = 1;
		if (other != null) {
			return compareTo(other, null);
		}
		return ret;
	}

	/**
	 * Compare this PayRate to another one by the field specified, in either
	 * ascending or descending order.
	 *
	 * @param other The PayRate to which this instance will be compared.
	 * @param sortField The name of the sort field to compare on.
	 * @param ascending True for ascending sort, false for descending sort.
	 * @return The standard -1/0/1 for Java compare operations.
	 */
	public int compareTo(PayRate other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret));
	}

	/**
	 * Compare this PayRate to another one by the field specified, in ascending
	 * order.
	 *
	 * @param other The PayRate to which this instance will be compared.
	 * @param sortField The name of the sort field to compare on.
	 * @return The standard -1/0/1 for Java compare operations.
	 */
	public int compareTo(PayRate other, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by name (later)
		}
		else if (sortField.equals(SORTKEY_JOB) ) {
			ret = StringUtils.compareIgnoreCase(getOccupation(), other.getOccupation());
		}
		else if (sortField.equals(SORTKEY_SCHEDULE) ) {
			ret = StringUtils.compareIgnoreCase(getSchedule(), other.getSchedule());
		}
		else if (sortField.equals(SORTKEY_CONTRACT) ) {
			ret = StringUtils.compareIgnoreCase(getContractCode(), other.getContractCode());
		}
		else if (sortField.equals(SORTKEY_GUAR_HOURS) ) {
			ret = NumberUtils.compare(getGuarHours(), other.getGuarHours());
		}
		else if (sortField.equals(SORTKEY_HOURLY_RATE) ) {
			ret = NumberUtils.compare(getHourlyRate(), other.getHourlyRate());
		}
		else if (sortField.equals(SORTKEY_DAILY_RATE) ) {
			ret = NumberUtils.compare(getDailyRate(), other.getDailyRate());
		}
		else if (sortField.equals(SORTKEY_WEEKLY_RATE) ) {
			ret = NumberUtils.compare(getWeeklyRate(), other.getWeeklyRate());
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			// Default compare is by occCode
			ret = StringUtils.compareIgnoreCase(getOccCode(), other.getOccCode());
		}
		return ret;
	}

}
