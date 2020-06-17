package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * StartRateSet entity. This object maintains all the pay rates that are
 * associated with a particular StartForm, for either Prep rates or Production
 * (shooting) rates.  Each StartForm will always have a StartRateSet for
 * production rates; the second StartRateSet, for prep rates, is optional.
 */
@Entity
@Table(name = "start_rate_set")
public class StartRateSet extends PersistentObject<StartRateSet> implements Cloneable {
	private static final Log log = LogFactory.getLog(StartRateSet.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** Hourly rates, guaranteed hours, & account codes */
	private RateHoursGroup hourly;

	/** 1.5x guaranteed hours, & account codes; rates are not stored,
	 * but are calculated from the hourly rates. */
	private HoursGroup x15;

	/** 2.0x guaranteed hours, & account codes; rates are not stored,
	 * but are calculated from the hourly rates. */
	private HoursGroup x20;

	/** Daily rates, guaranteed hours, & account codes */
	private RateHoursGroup daily;

	/** Weekly rates, guaranteed hours, & account codes */
	private RateHoursGroup weekly;

	/** Day 6 rates, guaranteed hours, & account codes */
	private RateHoursGroup day6;

	/** Day 7 rates, guaranteed hours, & account codes */
	private RateHoursGroup day7;

	/** Idle Day 6 rates, guaranteed hours, & account codes; does not
	 * include studio values, as they are not applicable. */
	private IdleRateGroup idleDay6;

	/** Idle Day 7 rates, guaranteed hours, & account codes; does not
	 * include studio values, as they are not applicable. */
	private IdleRateGroup idleDay7;

	// * * * AICP / Commercial production rate fields

	/** The Hours/day cutoff between regular time and the first OT rate. */
	private BigDecimal ot1AfterHours;

	/** The hourly OT rate for the first period after regular time. */
	private BigDecimal ot1Rate;

	/** The effective multiplier to compute the OT rate from the regular rate. */
	private BigDecimal ot1Multiplier;

	/** The Hours/day cutoff between regular time and the second OT rate. */
	private BigDecimal ot2AfterHours;

	/** The hourly OT rate for the second period after regular time. */
	private BigDecimal ot2Rate;

	/** The effective multiplier to compute the second OT rate from the regular rate. */
	private BigDecimal ot2Multiplier;

	/** The Hours/day cutoff between regular time and the third OT rate; this will
	 * typically only apply in states that mandate gold time, such as California. */
	private BigDecimal ot3AfterHours;

	/** The hourly OT rate for the third period after regular time. */
	private BigDecimal ot3Rate;

	/** The effective multiplier to compute the third OT rate from the regular rate. */
	private BigDecimal ot3Multiplier;


	// Constructors

	/** default constructor */
	public StartRateSet() {
	}

	/**
	 * Set all hours and rate values in this StartRateSet to null. Does not
	 * change account coding.
	 */
	public void clearRates() {
		getHourly().clearRates();
		getX15().clearRates();
		getX20().clearRates();
		getDaily().clearRates();
		getWeekly().clearRates();
		getDay6().clearRates();
		getDay7().clearRates();
		getIdleDay6().clearRates();
		getIdleDay7().clearRates();
		clearOtRates(); // LS-2567
	}

	/**
	 * Set the OT rate fields in this StartRateSet to null. LS-2567
	 */
	public void clearOtRates() {
		ot1AfterHours = null;
		ot1Rate = null;
		ot1Multiplier = null;
		ot2AfterHours = null;
		ot2Rate = null;
		ot2Multiplier = null;
		ot3AfterHours = null;
		ot3Rate = null;
		ot3Multiplier = null;
	}

	// Property accessors

	/**See {@link #hourly}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 	column = @Column(name = "Hourly_Rate_Studio") ),
		@AttributeOverride(name="studioHrs",column = @Column(name = "Hourly_Rate_Studio_Hrs") ),
		@AttributeOverride(name="loc", 		column = @Column(name = "Hourly_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Hourly_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Hourly_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Hourly_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Hourly_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Hourly_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Hourly_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Hourly_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Hourly_Rate_Acct_Free2") ),
	} )
	public RateHoursGroup getHourly() {
		if (hourly == null) {
			hourly = new RateHoursGroup();
		}
		return hourly;
	}
	/**See {@link #hourly}. */
	public void setHourly(RateHoursGroup hourly) {
		this.hourly = hourly;
	}

	/**See {@link #daily}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 	column = @Column(name = "Daily_Rate_Studio") ),
		@AttributeOverride(name="studioHrs",column = @Column(name = "Daily_Rate_Studio_Hrs") ),
		@AttributeOverride(name="loc", 		column = @Column(name = "Daily_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Daily_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Daily_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Daily_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Daily_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Daily_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Daily_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Daily_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Daily_Rate_Acct_Free2") ),
	} )
	public RateHoursGroup getDaily() {
		if (daily == null) {
			daily = new RateHoursGroup();
		}
		return daily;
	}
	/**See {@link #daily}. */
	public void setDaily(RateHoursGroup daily) {
		this.daily = daily;
	}

	/**See {@link #weekly}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 	column = @Column(name = "Weekly_Rate_Studio") ),
		@AttributeOverride(name="studioHrs",column = @Column(name = "Weekly_Rate_Studio_Hrs") ),
		@AttributeOverride(name="loc", 		column = @Column(name = "Weekly_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Weekly_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Weekly_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Weekly_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Weekly_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Weekly_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Weekly_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Weekly_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Weekly_Rate_Acct_Free2") ),
	} )
	public RateHoursGroup getWeekly() {
		if (weekly == null) {
			weekly = new RateHoursGroup();
		}
		return weekly;
	}
	/**See {@link #weekly}. */
	public void setWeekly(RateHoursGroup weekly) {
		this.weekly = weekly;
	}

	/**See {@link #x15}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studioHrs",column = @Column(name = "X15_Rate_Over_Studio_Hrs") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "X15_Rate_Over_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "X15_Rate_Over_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "X15_Rate_Over_Acct_Major") ),
		@AttributeOverride(name="dtl",	 	column = @Column(name = "X15_Rate_Over_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "X15_Rate_Over_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "X15_Rate_Over_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "X15_Rate_Over_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "X15_Rate_Over_Acct_Free2") ),
	} )
	public HoursGroup getX15() {
		if (x15 == null) {
			x15 = new HoursGroup();
		}
		return x15;
	}
	/**See {@link #x15}. */
	public void setX15(HoursGroup x15Rate) {
		x15 = x15Rate;
	}

	@Transient
	public BigDecimal getX15StudioRate() {
		return NumberUtils.scaleHourlyRate(NumberUtils.safeMultiply(getHourly().getStudio(),Constants.DECIMAL_ONE_FIVE));
	}
	@Transient
	public BigDecimal getX15LocRate() {
		return NumberUtils.scaleHourlyRate(NumberUtils.safeMultiply(getHourly().getLoc(),Constants.DECIMAL_ONE_FIVE));
	}

	/**See {@link #x20}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studioHrs",column = @Column(name = "X20_Rate_Over_Studio_Hrs") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "X20_Rate_Over_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "X20_Rate_Over_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "X20_Rate_Over_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "X20_Rate_Over_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "X20_Rate_Over_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "X20_Rate_Over_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "X20_Rate_Over_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "X20_Rate_Over_Acct_Free2") ),
	} )
	public HoursGroup getX20() {
		if (x20 == null) {
			x20 = new HoursGroup();
		}
		return x20;
	}
	/**See {@link #x20}. */
	public void setX20(HoursGroup x20Rate) {
		x20 = x20Rate;
	}

	@Transient
	public BigDecimal getX20StudioRate() {
		return NumberUtils.scaleHourlyRate(NumberUtils.safeMultiply(getHourly().getStudio(),Constants.DECIMAL_TWO));
	}
	@Transient
	public BigDecimal getX20LocRate() {
		return NumberUtils.scaleHourlyRate(NumberUtils.safeMultiply(getHourly().getLoc(),Constants.DECIMAL_TWO));
	}

	/**See {@link #day6}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 	column = @Column(name = "Day6_Rate_Studio") ),
		@AttributeOverride(name="studioHrs",column = @Column(name = "Day6_Rate_Studio_Hrs") ),
		@AttributeOverride(name="loc", 		column = @Column(name = "Day6_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Day6_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Day6_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Day6_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Day6_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Day6_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Day6_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Day6_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Day6_Rate_Acct_Free2") ),
	} )
	public RateHoursGroup getDay6() {
		if (day6 == null) {
			day6 = new RateHoursGroup();
		}
		return day6;
	}
	/**See {@link #day6}. */
	public void setDay6(RateHoursGroup day6) {
		this.day6 = day6;
	}

	/**See {@link #day7}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="studio", 	column = @Column(name = "Day7_Rate_Studio") ),
		@AttributeOverride(name="studioHrs",column = @Column(name = "Day7_Rate_Studio_Hrs") ),
		@AttributeOverride(name="loc", 		column = @Column(name = "Day7_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Day7_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Day7_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Day7_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Day7_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Day7_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Day7_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Day7_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Day7_Rate_Acct_Free2") ),
	} )
	public RateHoursGroup getDay7() {
		if (day7 == null) {
			day7 = new RateHoursGroup();
		}
		return day7;
	}
	/**See {@link #day7}. */
	public void setDay7(RateHoursGroup day7) {
		this.day7 = day7;
	}

	/**See {@link #idleDay6}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="loc", 		column = @Column(name = "Idle_Day6_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Idle_Day6_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Idle_Day6_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Idle_Day6_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Idle_Day6_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Idle_Day6_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Idle_Day6_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Idle_Day6_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Idle_Day6_Rate_Acct_Free2") ),
	} )
	public IdleRateGroup getIdleDay6() {
		if (idleDay6 == null) {
			idleDay6 = new IdleRateGroup();
		}
		return idleDay6;
	}
	/**See {@link #idleDay6}. */
	public void setIdleDay6(IdleRateGroup idleDay6) {
		this.idleDay6 = idleDay6;
	}

	/**See {@link #idleDay7}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="loc", 		column = @Column(name = "Idle_Day7_Rate_Loc") ),
		@AttributeOverride(name="locHrs", 	column = @Column(name = "Idle_Day7_Rate_Loc_Hrs") ),
		@AttributeOverride(name="aloc", 	column = @Column(name = "Idle_Day7_Rate_Acct_Loc") ),
		@AttributeOverride(name="major", 	column = @Column(name = "Idle_Day7_Rate_Acct_Major") ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Idle_Day7_Rate_Acct_Dtl") ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Idle_Day7_Rate_Acct_Sub") ),
		@AttributeOverride(name="set", 		column = @Column(name = "Idle_Day7_Rate_Acct_Set") ),
		@AttributeOverride(name="free", 	column = @Column(name = "Idle_Day7_Rate_Acct_Free") ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Idle_Day7_Rate_Acct_Free2") ),
	} )
	public IdleRateGroup getIdleDay7() {
		if (idleDay7 == null) {
			idleDay7 = new IdleRateGroup();
		}
		return idleDay7;
	}
	/**See {@link #idleDay7}. */
	public void setIdleDay7(IdleRateGroup idleDay7) {
		this.idleDay7 = idleDay7;
	}

	/** See {@link #ot1AfterHours}. */
	@Column(name = "Ot1_After_Hours", precision = 4, scale = 2)
	public BigDecimal getOt1AfterHours() {
		return ot1AfterHours;
	}
	/** See {@link #ot1AfterHours}. */
	public void setOt1AfterHours(BigDecimal ot1AfterHours) {
		this.ot1AfterHours = ot1AfterHours;
	}

	/** See {@link #ot1Rate}. */
	@Column(name = "Ot1_rate", precision = 9, scale = 4)
	public BigDecimal getOt1Rate() {
		return ot1Rate;
	}
	/** See {@link #ot1Rate}. */
	public void setOt1Rate(BigDecimal ot1Rate) {
		this.ot1Rate = ot1Rate;
	}

	/** See {@link #ot1Multiplier}. */
	@Column(name = "Ot1_Multiplier", precision = 8, scale = 6)
	public BigDecimal getOt1Multiplier() {
		return ot1Multiplier;
	}
	/** See {@link #ot1Multiplier}. */
	public void setOt1Multiplier(BigDecimal ot1Multiplier) {
		this.ot1Multiplier = ot1Multiplier;
	}

	/** See {@link #ot2AfterHours}. */
	@Column(name = "Ot2_After_Hours", precision = 4, scale = 2)
	public BigDecimal getOt2AfterHours() {
		return ot2AfterHours;
	}
	/** See {@link #ot2AfterHours}. */
	public void setOt2AfterHours(BigDecimal ot2AfterHours) {
		this.ot2AfterHours = ot2AfterHours;
	}

	/** See {@link #ot2Rate}. */
	@Column(name = "Ot2_rate", precision = 9, scale = 4)
	public BigDecimal getOt2Rate() {
		return ot2Rate;
	}
	/** See {@link #ot2Rate}. */
	public void setOt2Rate(BigDecimal ot2Rate) {
		this.ot2Rate = ot2Rate;
	}

	/** See {@link #ot2Multiplier}. */
	@Column(name = "Ot2_Multiplier", precision = 8, scale = 6)
	public BigDecimal getOt2Multiplier() {
		return ot2Multiplier;
	}
	/** See {@link #ot2Multiplier}. */
	public void setOt2Multiplier(BigDecimal ot2Multiplier) {
		this.ot2Multiplier = ot2Multiplier;
	}

	/** See {@link #ot3AfterHours}. */
	@Column(name = "Ot3_After_Hours", precision = 4, scale = 2)
	public BigDecimal getOt3AfterHours() {
		return ot3AfterHours;
	}
	/** See {@link #ot3AfterHours}. */
	public void setOt3AfterHours(BigDecimal ot3AfterHours) {
		this.ot3AfterHours = ot3AfterHours;
	}

	/** See {@link #ot3Rate}. */
	@Column(name = "Ot3_rate", precision = 9, scale = 4)
	public BigDecimal getOt3Rate() {
		return ot3Rate;
	}
	/** See {@link #ot3Rate}. */
	public void setOt3Rate(BigDecimal ot3Rate) {
		this.ot3Rate = ot3Rate;
	}

	/** See {@link #ot3Multiplier}. */
	@Column(name = "Ot3_Multiplier", precision = 8, scale = 6)
	public BigDecimal getOt3Multiplier() {
		return ot3Multiplier;
	}
	/** See {@link #ot3Multiplier}. */
	public void setOt3Multiplier(BigDecimal ot3Multiplier) {
		this.ot3Multiplier = ot3Multiplier;
	}

	/**
	 * @param ex
	 */
	public void exportTabbed(Exporter ex) {
		getHourly().exportTabbed(ex);
		getDaily().exportTabbed(ex);
		getWeekly().exportTabbed(ex);
		ex.append(getOt1AfterHours());
		ex.append(getOt1Rate());
		ex.append(getOt1Multiplier());
		ex.append(getOt2AfterHours());
		ex.append(getOt2Rate());
		ex.append(getOt2Multiplier());
		ex.append(getOt3AfterHours());
		ex.append(getOt3Rate());
		ex.append(getOt3Multiplier());
	}

	/**
	 * @return A copy of this object, including separate copies of all the
	 *         included data objects, which encompasses all of the data! (This
	 *         is significantly different than the clone() method, in which all
	 *         the embedded rate objects in the returned copy are the same
	 *         instances as those in the original.)
	 */
	public StartRateSet deepCopy() {
		StartRateSet newObj;
		try {
			newObj = clone();
			if (hourly != null) {
				newObj.hourly = hourly.clone();
			}
			if (x15 != null) {
				newObj.x15 = x15.clone();
			}
			if (x20 != null) {
				newObj.x20 = x20.clone();
			}
			if (daily != null) {
				newObj.daily = daily.clone();
			}
			if (weekly != null) {
				newObj.weekly = weekly.clone();
			}
			if (day6 != null) {
				newObj.day6 = day6.clone();
			}
			if (day7 != null) {
				newObj.day7 = day7.clone();
			}
			if (idleDay6 != null) {
				newObj.idleDay6 = idleDay6.clone();
			}
			if (idleDay7 != null) {
				newObj.idleDay7 = idleDay7.clone();
			}
		}
		catch (CloneNotSupportedException e) {
			log.error("StartRateSet clone error: ", e);
			return null;
		}
		return newObj;
	}

	/**
	 * For the purposes of copying this object, clone() should normally NOT be
	 * used, as all the embedded objects in the copy will refer to the same
	 * objects as the original. Use deepCopy() instead.
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public StartRateSet clone() {
		StartRateSet newObj;
		try {
			newObj = (StartRateSet)super.clone();
			newObj.id = null;
		}
		catch (CloneNotSupportedException e) {
			log.error("StartRateSet clone error: ", e);
			return null;
		}
		return newObj;
	}

}