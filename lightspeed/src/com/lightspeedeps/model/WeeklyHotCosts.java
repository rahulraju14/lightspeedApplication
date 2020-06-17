package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

@Entity
@Table(name = "weekly_hot_costs")
public class WeeklyHotCosts extends PersistentObject<WeeklyHotCosts> {
	private static final long serialVersionUID = 1L;

	// Fields
	/** Id of the Start Form associated with this Employee */
	private Integer startFormId;
	/** Employee's User Accout Id */
	private String userAccount;
	/** Week End Date for this Weekly Hot Costs object */
	private Date endDate;
	/** Id of the Production associated with this Weekly Hot Costs object */
	private String prodId;
	/** The Total of Daily Hot Costs objects for this Weekly Hot Costs object */
	private BigDecimal grandTotal;
	/** Employee's Department Id */
	private Integer departmentId;
	/** Id of the Employment Record associated with Weekly Hot Costs object */
	private Integer employmentId;
	/** Name of the Employee's Department */
	private String departmentName;
	/** Employee's Daily Exempt Rate */
	private BigDecimal dailyRate;
	/** Employee's Hourly Rate */
	private BigDecimal hourlyRate;
	/** Employee's Weekly Exempt Rate */
	private BigDecimal weeklyRate;

	/** Do not persist the Employment object with the WeeklyHotCosts. Persisting the deleting an employment records
	 * with the WeeklyHotCosts creates a constraint db error when trying to delete the employment record.
	 */
	@Transient
	private Employment employment;

	/** Do not persist the StartForm object with the WeeklyHotCosts. Persisting the deleting an StartForm records
	 * with the WeeklyHotCosts creates a constraint db error when trying to delete the Occupation from the Crew Member.
	 */
	@Transient
	private StartForm startForm;

	/** List of Daily Hot Costs associated with the week. */
	private List<DailyHotCost> dailyHotCosts = new ArrayList<>(0);
	@Transient
	private BigDecimal totalWeeklyPayHours;
	@Transient
	private BigDecimal totalWeeklyWorkedHours;
	@Transient
	private BigDecimal totalWeeklyBudgetedHours;
	@Transient
	private BigDecimal totalWeeklyCost;
	@Transient
	private BigDecimal totalWeeklyBudgetedCost;

	// Constructors

	/** default constructor */
	public WeeklyHotCosts() {
		dailyRate = Constants.DECIMAL_ZERO;
		hourlyRate = Constants.DECIMAL_ZERO;
		weeklyRate = Constants.DECIMAL_ZERO;
		grandTotal = Constants.DECIMAL_ZERO;
	}

	/** minimal constructor */
	public WeeklyHotCosts(String userAccount, Date endDate, String prodId) {
		this.userAccount = userAccount;
		this.endDate = endDate;
		this.prodId = prodId;
	}

	/** See {@link #dailyRate}. */
	@Column(name = "Daily_Rate", precision = 10)
	public BigDecimal getDailyRate() {
		return dailyRate;
	}

	/** See {@link #dailyRate}. */
	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	/** See {@link #hourlyRate}. */
	@Column(name = "Hourly_Rate", precision = 10)
	public BigDecimal getHourlyRate() {
		return hourlyRate;
	}

	/** See {@link #hourlyRate}. */
	public void setHourlyRate(BigDecimal hourlyRate) {
		this.hourlyRate = hourlyRate;
	}

	/** See {@link #weeklyRate}. */
	@Column(name = "Weekly_Rate", precision = 10)
	public BigDecimal getWeeklyRate() {
		return weeklyRate;
	}

	/** See {@link #weeklyRate}. */
	public void setWeeklyRate(BigDecimal weeklyRate) {
		this.weeklyRate = weeklyRate;
	}

	/** See {@link #employmentId}. */
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Employment_Id")
	@Column(name = "Employment_Id")
	public Integer getEmploymentId() {
		return this.employmentId;
	}

	/** See {@link #employmentId}. */
	public void setEmploymentId(Integer employmentId) {
		this.employmentId = employmentId;
	}

	/** See {@link #employment}. */
	@Transient
	public Employment getEmployment() {
		return this.employment;
	}

	/** See {@link #employment}. */
	@Transient
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/** See {@link #userAccount}. */
	@Column(name = "User_Account", nullable = false, length = 20)
	public String getUserAccount() {
		return this.userAccount;
	}

	/** See {@link #userAccount}. */
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	/** See {@link #endDate}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "End_Date", nullable = false)
	public Date getEndDate() {
		return this.endDate;
	}

	/** See {@link #endDate}. */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #prodId}. */
	@Column(name = "Prod_Id", nullable = false, length = 10)
	public String getProdId() {
		return this.prodId;
	}

	/** See {@link #prodId}. */
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	/** See {@link #grandTotal}. */
	@Column(name = "Grand_Total", precision = 10)
	public BigDecimal getGrandTotal() {
		return this.grandTotal;
	}

	/** See {@link #grandTotal}. */
	public void setGrandTotal(BigDecimal grandTotal) {
		this.grandTotal = grandTotal;
	}

	/** See {@link #departmentId}. */
	@Column(name = "Department_Id")
	public Integer getDepartmentId() {
		return this.departmentId;
	}

	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/** See {@link #departmentName}. */
	@Column(name = "Department_Name")
	public String getDepartmentName() {
		return this.departmentName;
	}

	/** See {@link #departmentName}. */
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}


	/** See {@link #startFormId}. */
	@Column(name = "Start_Form_Id")
	public Integer getStartFormId() {
		return startFormId ;
	}

	/** See {@link #startFormId}. */
	public void setStartFormId(Integer startFormId) {
		this.startFormId = startFormId;
	}

	/** See {@link #dailyHotCosts}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "weeklyHotCosts")
	@OrderBy("dayNum")
	public List<DailyHotCost> getDailyHotCosts() {
		return dailyHotCosts;
	}

	/** See {@link #dailyHotCosts}. */
	public void setDailyHotCosts(List<DailyHotCost> dailyHotCosts) {
		this.dailyHotCosts = dailyHotCosts;
	}


	@Transient
	public BigDecimal getWeeklyCost() {
		totalWeeklyCost = Constants.DECIMAL_ZERO;
		for(DailyHotCost dhc : dailyHotCosts) {
			BigDecimal cost = dhc.getActualCost();
			totalWeeklyCost = NumberUtils.safeAdd(totalWeeklyCost, cost);
		}
		return totalWeeklyCost;
	}


	@Transient
	public BigDecimal getBudgetedWeeklyCost() {
		totalWeeklyBudgetedCost = Constants.DECIMAL_ZERO;

		for(DailyHotCost dhc : dailyHotCosts) {
			totalWeeklyBudgetedCost = NumberUtils.safeAdd(totalWeeklyBudgetedCost, dhc.getBudgetedCost());
//			totalWeeklyBudgetedCost = NumberUtils.safeAdd(totalWeeklyBudgetedCost, dhc.getBudgetedMpvCost());
		}
		return totalWeeklyBudgetedCost;
	}


	@Transient
	public BigDecimal getWeeklyWorkedHours() {
		totalWeeklyWorkedHours = Constants.DECIMAL_ZERO;

		for(DailyHotCost dhc : dailyHotCosts) {
			BigDecimal hours = dhc.getHours();
			if(hours != null) {
				totalWeeklyWorkedHours = NumberUtils.safeAdd(totalWeeklyWorkedHours, hours);
			}
		}

		// Calc the pay hours also.
		getWeeklyPayHours();

		return totalWeeklyWorkedHours;
	}

	@Transient
	public BigDecimal getWeeklyPayHours() {
		totalWeeklyPayHours = Constants.DECIMAL_ZERO;

		for(DailyHotCost dhc : dailyHotCosts) {
			BigDecimal hours = dhc.getPayHours();
			if(hours != null) {
				totalWeeklyPayHours = NumberUtils.safeAdd(totalWeeklyPayHours, hours);
			}
		}
		return totalWeeklyPayHours;
	}

	@Transient
	public BigDecimal getBudgetedWeeklyHours() {
		totalWeeklyBudgetedHours = Constants.DECIMAL_ZERO;
		for(DailyHotCost dhc : dailyHotCosts) {
			BigDecimal hours = dhc.getBudgetedHours();
			if(hours != null) {
				totalWeeklyBudgetedHours = NumberUtils.safeAdd(totalWeeklyBudgetedHours, hours);
			}
		}
		return totalWeeklyBudgetedHours;
	}

	@Transient
	public BigDecimal getHoursVariance() {
		return totalWeeklyPayHours.subtract(totalWeeklyBudgetedHours);
	}


	@Transient
	public BigDecimal getCostVariance() {
		return totalWeeklyCost.subtract(totalWeeklyBudgetedCost);
	}

	@Transient
	public StartForm getStartForm() {
		return startForm;
	}

	@Transient
	public void setStartForm(StartForm startForm) {
		this.startForm = startForm;
	}

	/*
	 * Returns whether or not this an exempt employee
	 */
	@Transient
	public Boolean getExemptEmployee() {
		Boolean isExempt = false;

		if(startForm != null) {
			EmployeeRateType rateType = startForm.getRateType();
			isExempt = (rateType == EmployeeRateType.DAILY || rateType == EmployeeRateType.WEEKLY);
		}

		return isExempt;
	}
}