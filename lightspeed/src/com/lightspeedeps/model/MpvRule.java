package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.RuleType;

/**
 * MpvRule entity.
 */
@Entity
@Table(name = "mpv_rule", uniqueConstraints = @UniqueConstraint(columnNames = {"Rule_Key", "Meal_Number"}))
public class MpvRule extends Rule implements java.io.Serializable {

	/** */
	private static final long serialVersionUID = 1L;
	// Fields

	/** Which meal this rule applies to. If zero, the rule applies to all meals.  Non-zero values
	 * of 1, 2, and 3 are supported.  (A few unions have different penalties for different meals.) */
	private Byte mealNumber;

	/** The unit of time that constitutes one meal penalty. The meal period
	 * violation is divided by this value (and rounded up) to determine
	 * the number of penalties. */
	private BigDecimal increment;

	/** The minimum number of hours between the call time and the first meal.
	 * If the first meal occurs before this period has elapsed, it is
	 * non-deductible. */
	private BigDecimal minTo1stMeal;

	/** The maximum number of hours allowed between call time and the first
	 * meal before a meal period violation occurs. */
	private BigDecimal maxTo1stMeal;

	/** This value is currently ignored by the program.  If a rule is added
	 * with a non-zero value, code changes will be required. */
	private BigDecimal minTo2ndMeal;

	/** The maximum number of hours allowed between the end of the first meal
	 * and either the next meal time or wrap time before a meal period violation occurs. */
	private BigDecimal maxTo2ndMeal;

	/** The minimum number of total work hours required in a day before a second
	 * meal can be deducted. */
	private BigDecimal minimumWorkToDeductMeal;

	/** The maximum number of hours between the time the 2nd meal is due and the
	 * wrap time that may elapse for a 2nd "walking" meal to be non-deductible.
	 * If the wrap time is later than this, the second meal does not qualify as
	 * an NDM, and meal penalties will apply from the time the 2nd meal was due.
	 * (Example: see 399 Drivers Non-AICP (Indie) contract, XVIII(c).) */
	private BigDecimal maxToWrapForNdm2;

	/** The minimum number of work hours to be paid after the end of the second meal. */
	private BigDecimal guarHoursAfterMeal2;

	/** The minimum length of a meal, in hours. If a meal is shorter than this, then it
	 * does not count as a meal for the purpose of computing MPVs, and the meal time will
	 * count as work time. */
	private BigDecimal minMealLength;

	/** The maximum length of a meal, in hours. If a deductible meal is longer than this,
	 * any meal time beyond this amount counts as work time (i.e., is not deducted). */
	private BigDecimal maxMealLength;

	/** The maximum number of hours that wrap can be extended beyond the
	 * "max to 2nd meal" if a second non-deductible meal (NDM) is declared.
	 * E.g., if this is 1.0, wrap can be delayed for 1 hour beyond the (usual) 6
	 * hour meal period. If this value is negative, it means that NDMs and NDBs
	 * are not allowed and will be ignored -- they are not counted as meals. */
	private BigDecimal maxNdmAdjust;

	/** The maximum length "grace" period, in hours, that may be claimed by the production. */
	private BigDecimal maxGrace;

	/** The maximum period, in hours, that wrap map be delayed without incurring a meal-period
	 * violation (MPV). */
	private BigDecimal mayExtendBy;

	/** The maximum number of meal penalties per day. */
	private byte maxMpPerDay;

	/** The dollar penalty for the first meal period violation. If zero, use one hour's
	 * pay at the prevailing rate (the employee's rate at the time of the violation). If
	 * negative, use this as a factor multiplied times the employee's straight hourly
	 * rate (but made positive, obviously!). */
	private BigDecimal mpv1Rate;

	/** The dollar penalty for the second meal period violation. If zero, use one hour's
	 * pay at the prevailing rate (the employee's rate at the time of the violation). If
	 * negative, use this as a factor multiplied times the employee's straight hourly
	 * rate (but made positive, obviously!). */
	private BigDecimal mpv2Rate;

	/** The dollar penalty for the third meal period violation. If zero, use one hour's
	 * pay at the prevailing rate (the employee's rate at the time of the violation). If
	 * negative, use this as a factor multiplied times the employee's straight hourly
	 * rate (but made positive, obviously!). */
	private BigDecimal mpv3Rate;

	/** The dollar penalty for the fourth meal period violation. If zero, use one hour's
	 * pay at the prevailing rate (the employee's rate at the time of the violation). If
	 * negative, use this as a factor multiplied times the employee's straight hourly
	 * rate (but made positive, obviously!). */
	private BigDecimal mpv4Rate;

	/** The dollar penalty for the fifth and subsequent meal period violations. If zero, use one hour's
	 * pay at the prevailing rate (the employee's rate at the time of the violation). If
	 * negative, use this as a factor multiplied times the employee's straight hourly
	 * rate (but made positive, obviously!). */
	private BigDecimal mpvOtherRate;

	/** The dollar amount by which each additional penalty after the 5th (mpvOtherRate) will be
	 * increased.  So the 5th will be (mpvOtherRate), the 6th will be (mpvOtherRate+rateIncrement), the
	 * 7th will be (mpvOtherRate+(2*rateIncrement)), and so forth. */
	private BigDecimal rateIncrement;

	// Constructors

	/** default constructor */
	public MpvRule() {
		setType(RuleType.MP);
	}

//	/** minimal constructor */
//	public MpvRule(String ruleKey) {
//		super(ruleKey);
//	}

	/**See {@link #mealNumber}. */
	@Column(name = "Meal_Number", nullable = false)
	public Byte getMealNumber() {
		return mealNumber;
	}
	/**See {@link #mealNumber}. */
	public void setMealNumber(Byte mealNumber) {
		this.mealNumber = mealNumber;
	}

	/**See {@link #increment}. */
	@Column(name = "Increment", precision = 3, scale = 2, nullable = false)
	public BigDecimal getIncrement() {
		return increment;
	}
	/**See {@link #increment}. */
	public void setIncrement(BigDecimal period) {
		increment = period;
	}

	@Column(name = "Min_To_1st_Meal", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinTo1stMeal() {
		return minTo1stMeal;
	}
	public void setMinTo1stMeal(BigDecimal minTo1stMeal) {
		this.minTo1stMeal = minTo1stMeal;
	}

	@Column(name = "Max_To_1st_Meal", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxTo1stMeal() {
		return maxTo1stMeal;
	}
	public void setMaxTo1stMeal(BigDecimal maxTo1stMeal) {
		this.maxTo1stMeal = maxTo1stMeal;
	}

	@Column(name = "Min_To_2nd_Meal", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinTo2ndMeal() {
		return minTo2ndMeal;
	}
	public void setMinTo2ndMeal(BigDecimal minTo2ndMeal) {
		this.minTo2ndMeal = minTo2ndMeal;
	}

	@Column(name = "Max_To_2nd_Meal", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxTo2ndMeal() {
		return maxTo2ndMeal;
	}
	public void setMaxTo2ndMeal(BigDecimal maxTo2ndMeal) {
		this.maxTo2ndMeal = maxTo2ndMeal;
	}

	/**See {@link #minimumWorkToDeductMeal}. */
	@Column(name = "Minimum_Work_To_Deduct_Meal", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinimumWorkToDeductMeal() {
		return minimumWorkToDeductMeal;
	}
	/**See {@link #minimumWorkToDeductMeal}. */
	public void setMinimumWorkToDeductMeal(BigDecimal minimumWorkToDeductMeal) {
		this.minimumWorkToDeductMeal = minimumWorkToDeductMeal;
	}

	/** See {@link #maxToWrapForNdm2}. */
	@Column(name = "Max_To_Wrap_For_Ndm2", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxToWrapForNdm2() {
		return maxToWrapForNdm2;
	}
	/** See {@link #maxToWrapForNdm2}. */
	public void setMaxToWrapForNdm2(BigDecimal minToWrapToDeductMeal2) {
		maxToWrapForNdm2 = minToWrapToDeductMeal2;
	}

	/**See {@link #guarHoursAfterMeal2}. */
	@Column(name = "Guar_Hours_After_Meal2", precision = 4, scale = 2, nullable = false)
	public BigDecimal getGuarHoursAfterMeal2() {
		return guarHoursAfterMeal2;
	}
	/**See {@link #guarHoursAfterMeal2}. */
	public void setGuarHoursAfterMeal2(BigDecimal guarHoursAfterMeal2) {
		this.guarHoursAfterMeal2 = guarHoursAfterMeal2;
	}

	@Column(name = "Min_Meal_Length", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMinMealLength() {
		return minMealLength;
	}
	public void setMinMealLength(BigDecimal minMealLength) {
		this.minMealLength = minMealLength;
	}

	@Column(name = "Max_Meal_Length", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxMealLength() {
		return maxMealLength;
	}
	public void setMaxMealLength(BigDecimal maxMealLength) {
		this.maxMealLength = maxMealLength;
	}

	@Column(name = "Max_Ndm_Adjust", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxNdmAdjust() {
		return maxNdmAdjust;
	}
	public void setMaxNdmAdjust(BigDecimal maxNdmAdjust) {
		this.maxNdmAdjust = maxNdmAdjust;
	}

	/**
	 * @return true if NDMs are allowed. They are allowed if the
	 *         {@link #maxNdmAdjust} field is not negative.
	 */
	@Transient
	public boolean getNdmAllowed() {
		return getMaxNdmAdjust().signum() >= 0;
	}

	/**See {@link #maxGrace}. */
	@Column(name = "Max_Grace", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMaxGrace() {
		return maxGrace;
	}
	/**See {@link #maxGrace}. */
	public void setMaxGrace(BigDecimal maxGrace) {
		this.maxGrace = maxGrace;
	}

	/**See {@link #mayExtendBy}. */
	@Column(name = "May_Extend_By", precision = 4, scale = 2, nullable = false)
	public BigDecimal getMayExtendBy() {
		return mayExtendBy;
	}
	/**See {@link #mayExtendBy}. */
	public void setMayExtendBy(BigDecimal mayExtendBy) {
		this.mayExtendBy = mayExtendBy;
	}

	/** See {@link #maxMpPerDay}. */
	@Column(name = "Max_Mp_Per_Day", nullable = false)
	public byte getMaxMpPerDay() {
		return maxMpPerDay;
	}
	/** See {@link #maxMpPerDay}. */
	public void setMaxMpPerDay(byte maxMpPerDay) {
		this.maxMpPerDay = maxMpPerDay;
	}

	@Column(name = "Mpv1_Rate", precision = 7, scale = 4, nullable = false)
	public BigDecimal getMpv1Rate() {
		return mpv1Rate;
	}
	public void setMpv1Rate(BigDecimal mpv1Rate) {
		this.mpv1Rate = mpv1Rate;
	}

	@Column(name = "Mpv2_Rate", precision = 7, scale = 4, nullable = false)
	public BigDecimal getMpv2Rate() {
		return mpv2Rate;
	}
	public void setMpv2Rate(BigDecimal mpv2Rate) {
		this.mpv2Rate = mpv2Rate;
	}

	@Column(name = "Mpv3_Rate", precision = 7, scale = 4, nullable = false)
	public BigDecimal getMpv3Rate() {
		return mpv3Rate;
	}
	public void setMpv3Rate(BigDecimal mpv3Rate) {
		this.mpv3Rate = mpv3Rate;
	}

	/** See {@link #mpv4Rate}. */
	@Column(name = "Mpv4_Rate", precision = 7, scale = 4, nullable = false)
	public BigDecimal getMpv4Rate() {
		return mpv4Rate;
	}
	/** See {@link #mpv4Rate}. */
	public void setMpv4Rate(BigDecimal mpv4Rate) {
		this.mpv4Rate = mpv4Rate;
	}

	@Column(name = "Mpv_Other_Rate", precision = 7, scale = 4, nullable = false)
	public BigDecimal getMpvOtherRate() {
		return mpvOtherRate;
	}
	public void setMpvOtherRate(BigDecimal mpvOtherRate) {
		this.mpvOtherRate = mpvOtherRate;
	}

	/** See {@link #rateIncrement}. */
	@Column(name = "Rate_Increment", precision = 7, scale = 4, nullable = false)
	public BigDecimal getRateIncrement() {
		return rateIncrement;
	}
	/** See {@link #rateIncrement}. */
	public void setRateIncrement(BigDecimal rateIncrement) {
		this.rateIncrement = rateIncrement;
	}

	@Override
	public String toAudit() {
		String s = super.toAudit();
		if (getMealNumber() != 0) {
			s += ", Meal=[" + getMealNumber() + "]";
		}
		s += ", Rates=[" +
				getMpv1Rate() + "," +
				getMpv2Rate() + "," +
				getMpv3Rate() + "," +
				getMpv4Rate() + "," +
				getMpvOtherRate() + "," +
				getRateIncrement() + "]";
		return s;
	}

}
