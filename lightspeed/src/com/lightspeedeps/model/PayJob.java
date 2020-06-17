package com.lightspeedeps.model;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.port.Importer;
import com.lightspeedeps.type.PayRateType;
import com.lightspeedeps.type.ProductionPhase;
import com.lightspeedeps.util.common.Filter;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * PayJob entity. This defines one "job table" entry for a
 * {@link WeeklyTimecard}. A {@link WeeklyTimecard} can have any number of
 * related PayJob objects, although the Crew-Cards product (which we interface
 * with) limits each timecard to 7 such jobs.
 * <p>
 * Each PayJob entity contains 7 {@link PayJobDaily} objects, one per day of the
 * week, which hold the detailed breakdown of how many hours at each possible
 * pay rate are assigned to this PayJob.
 */
@Entity
@Table(name = "pay_job")
@JsonIgnoreProperties({"id","weeklyTimecard"}) // used by generateJsonSchema()
public class PayJob extends PersistentObject<PayJob> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(WeeklyTimecard.class);

	private static final long serialVersionUID = -9076859029985288412L;

	private static BigDecimal DECIMAL_2 = new BigDecimal(2.0);
	private static BigDecimal DECIMAL_2_5 = new BigDecimal(2.5);
	private static BigDecimal DECIMAL_3 = new BigDecimal(3.0);
	private static BigDecimal DECIMAL_3_5 = new BigDecimal(3.5);

	static {
		DECIMAL_2 = NumberUtils.scaleTo(DECIMAL_2, 1, 1);
		DECIMAL_2_5 = NumberUtils.scaleTo(DECIMAL_2_5, 1, 1);
		DECIMAL_3 = NumberUtils.scaleTo(DECIMAL_3, 1, 1);
		DECIMAL_3_5 = NumberUtils.scaleTo(DECIMAL_3_5, 1, 1);
	}
	// Fields

	/** */
	private WeeklyTimecard weeklyTimecard;
	/** */
	private byte jobNumber;

	/** The entire set of account codes for the PayJob */
	private AccountCodes account;

	/** Job name, for commercial productions. */
	private String jobName;
	/** Job "number", for commercial productions. */
	private String jobAccountNumber;

	/** */
	private String occCode;

	/** */
	private BigDecimal rate;
	/** */
	private BigDecimal premiumRate;
	/** not used */
	private BigDecimal dailyRate;

	/** Used for exempt/on-call positions. */
	private BigDecimal weeklyRate;
	/** */
	private BigDecimal boxAmt;

	/** The type of data represented by custom column 1 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom1Type = PayRateType.H;

	/** The type of data represented by custom column 2 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom2Type = PayRateType.H;

	/** The type of data represented by custom column 3 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom3Type = PayRateType.H;

	/** The type of data represented by custom column 4 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom4Type = PayRateType.H;

	/** The type of data represented by custom column 5 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom5Type = PayRateType.H;

	/** The type of data represented by custom column 6 - regular or premium (hourly),
	 * daily, weekly, or fixed amount. */
	private PayRateType custom6Type = PayRateType.H;

	/** 1st custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult1 = DECIMAL_2;

	/** 2nd custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult2 = DECIMAL_2_5;

	/** 3rd custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult3 = DECIMAL_3;

	/** 4th custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult4 = DECIMAL_3_5;

	/** 5th custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult5 = BigDecimal.ZERO;

	/** 6th custom multiplier (in addition to 1.0/1.5). A positive multiplier
	 * means this is an hourly rate multiplier. A negative multiplier, with the
	 * 'premium' flag off, means this is a multiplier of the daily rate. A
	 * negative multiplier, with the 'premium' flag on, means this is a
	 * multiplier of the weekly rate. */
	private BigDecimal customMult6 = BigDecimal.ZERO;

	/** */
	private List<PayJobDaily> payJobDailies = new ArrayList<>(0);


	/** Total of all 1.0x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total10;

	/** Total of all 1.5x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total15;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust1;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust2;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust3;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust4;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust5;

	/** Total of all custMult.x hours in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal totalCust6;

	/** Total of all 1.0x Night Premium hours at 110% (or other %) in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total10Np1;

	/** Total of all 1.0x Night Premium hours at 120% (or other %) in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total10Np2;

	/** Total of all 1.5x Night Premium hours at 110% (or other %) in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total15Np1;

	/** Total of all 1.5x Night Premium hours at 120% (or other %) in this PayJob (total of 7 days) */
	@Transient
	private BigDecimal total15Np2;

	/** True iff at least one Night Premium field is non-zero. */
//	@Transient
//	private boolean hasNpHours;

	/** Total of all hours in this PayJob -- total of 7 days, all pay factors. */
	@Transient
	private BigDecimal totalHours;

	/** Total of all MPV1s (A.M.) in this PayJob -- total of 7 days. */
	@Transient
	private Byte totalMpv1;

	/** Total of all MPV2s (P.M.) in this PayJob -- total of 7 days. */
	@Transient
	private Byte totalMpv2;

	/** Total of all MPV3s (wrap) in this PayJob -- total of 7 days. */
	@Transient
	private Byte totalMpv3;

	/** Used in export processes - total days worked covered by this PayJob instance. */
	@Transient
	private short daysWorked;

	/** Used during import to determine which PayJob entries are used by
	 * the DailyTime splits. */
	@Transient
	private boolean referenced;

	// Constructors

	/** default constructor */
	public PayJob() {
		// If all fields are null, the JSP doesn't show the job header fields at all!
		setAccountLoc(""); // so set one field to empty instead of null.
	}

	/** minimal constructor */
	public PayJob(WeeklyTimecard weeklyTimecard, byte jobNumber) {
		this();
		this.weeklyTimecard = weeklyTimecard;
		this.jobNumber = jobNumber;
	}

	// Property accessors

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	@Column(name = "Job_Number", nullable = false)
	public byte getJobNumber() {
		return jobNumber;
	}
	public void setJobNumber(byte jobNumber) {
		this.jobNumber = jobNumber;
	}

	@Column(name = "Occ_Code", length = 20)
	public String getOccCode() {
		return occCode;
	}
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	@Column(name = "Rate", precision = 9, scale = 4)
	public BigDecimal getRate() {
		return rate;
	}
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	/** See {@link #premiumRate}. */
	@Column(name = "Premium_Rate", precision = 9, scale = 4)
	public BigDecimal getPremiumRate() {
		return premiumRate;
	}
	/** See {@link #premiumRate}. */
	public void setPremiumRate(BigDecimal premiumRate) {
		this.premiumRate = premiumRate;
	}

	/** See {@link #dailyRate}. */
	@Column(name = "Daily_Rate", precision = 9, scale = 4)
	public BigDecimal getDailyRate() {
		return dailyRate;
	}
	/** See {@link #dailyRate}. */
	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	/** See {@link #weeklyRate}. */
	@Column(name = "Weekly_Rate", precision = 8, scale = 2)
	public BigDecimal getWeeklyRate() {
		return weeklyRate;
	}
	/** See {@link #weeklyRate}. */
	public void setWeeklyRate(BigDecimal weeklyRate) {
		this.weeklyRate = weeklyRate;
	}

	@Column(name = "Box_Amt", precision = 8, scale = 2)
	public BigDecimal getBoxAmt() {
		return boxAmt;
	}
	public void setBoxAmt(BigDecimal boxAmt) {
		this.boxAmt = boxAmt;
	}

	/** See {@link #account}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Location_code", length = 10) ),
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

	/** See {@link #jobName}. */
	@Column(name = "Job_Name", length = 10)
	public String getJobName() {
		return jobName;
	}
	/** See {@link #jobName}. */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/** See {@link #jobAccountNumber}. */
	@Column(name = "Job_Account_Number", length = 20)
	public String getJobAccountNumber() {
		return jobAccountNumber;
	}
	/** See {@link #jobAccountNumber}. */
	public void setJobAccountNumber(String jobAccountNumber) {
		this.jobAccountNumber = jobAccountNumber;
	}

	@Column(name = "Custom_Mult1", precision = 8, scale = 6)
	public BigDecimal getCustomMult1() {
		return customMult1;
	}
	public void setCustomMult1(BigDecimal customMult) {
		customMult1 = customMult;
	}

	/**
	 * @return the formatted value of  {@link #customMult1} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult1Text() {
		return formatMultText(getCustomMult1(), getCustom1Type());
	}

	@Column(name = "Custom_Mult2", precision = 8, scale = 6)
	public BigDecimal getCustomMult2() {
		return customMult2;
	}
	public void setCustomMult2(BigDecimal customMult) {
		customMult2 = customMult;
	}

	/**
	 * @return the formatted value of  {@link #customMult2} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult2Text() {
		return formatMultText(getCustomMult2(), getCustom2Type());
	}

	@Column(name = "Custom_Mult3", precision = 8, scale = 6)
	public BigDecimal getCustomMult3() {
		return customMult3;
	}
	public void setCustomMult3(BigDecimal customMult) {
		customMult3 = customMult;
	}
	/**
	 * @return the formatted value of  {@link #customMult3} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult3Text() {
		return formatMultText(getCustomMult3(), getCustom3Type());
	}

	@Column(name = "Custom_Mult4", precision = 8, scale = 6)
	public BigDecimal getCustomMult4() {
		return customMult4;
	}
	public void setCustomMult4(BigDecimal customMult) {
		customMult4 = customMult;
	}
	/**
	 * @return the formatted value of  {@link #customMult4} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult4Text() {
		return formatMultText(getCustomMult4(), getCustom4Type());
	}

	@Column(name = "Custom_Mult5", precision = 8, scale = 6)
	public BigDecimal getCustomMult5() {
		return customMult5;
	}
	public void setCustomMult5(BigDecimal customMult) {
		customMult5 = customMult;
	}
	/**
	 * @return the formatted value of  {@link #customMult5} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult5Text() {
		return formatMultText(getCustomMult5(), getCustom5Type());
	}

	@Column(name = "Custom_Mult6", precision = 8, scale = 6)
	public BigDecimal getCustomMult6() {
		return customMult6;
	}
	public void setCustomMult6(BigDecimal customMult) {
		customMult6 = customMult;
	}
	/**
	 * @return the formatted value of  {@link #customMult6} for use
	 * as a column header.
	 */
	@JsonIgnore
	@Transient
	public String getCustomMult6Text() {
		return formatMultText(getCustomMult6(), getCustom6Type());
	}

	/** Create formatted text for column headers in the Job table */
	private static String formatMultText(BigDecimal mult, PayRateType payRateType) {
		if (mult == null) {
			return null;
		}
		String res = null;
		switch(payRateType) {
			case D: // Daily
			case W: // Weekly
			case F: // Fixed
			case T: // Table
				res = payRateType.getColumnText();
				break;
			case P: // Premium (hourly)
			case H: // Hourly (non-premium/regular)
				DecimalFormat fmt = new DecimalFormat("#0.0#");
				res = fmt.format(mult);
				// see if value has more than 2 non-zero decimal places
				BigDecimal rounded = mult.setScale(2, RoundingMode.FLOOR);
				if (rounded.subtract(mult).signum() != 0) {
					res += ".."; // more decimal places not shown
				}
				if (payRateType == PayRateType.P) {
					return res + "p";
				}
				break;
		}
		return res;
	}

	/** See {@link #custom1Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom1_Type", nullable=false)
	public PayRateType getCustom1Type() {
		return custom1Type;
	}
	/** See {@link #custom1Type}. */
	public void setCustom1Type(PayRateType custom1Type) {
		this.custom1Type = custom1Type;
	}

	/** See {@link #custom2Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom2_Type", nullable=false)
	public PayRateType getCustom2Type() {
		return custom2Type;
	}
	/** See {@link #custom2Type}. */
	public void setCustom2Type(PayRateType custom2Type) {
		this.custom2Type = custom2Type;
	}

	/** See {@link #custom3Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom3_Type", nullable=false)
	public PayRateType getCustom3Type() {
		return custom3Type;
	}
	/** See {@link #custom3Type}. */
	public void setCustom3Type(PayRateType custom3Type) {
		this.custom3Type = custom3Type;
	}

	/** See {@link #custom4Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom4_Type", nullable=false)
	public PayRateType getCustom4Type() {
		return custom4Type;
	}
	/** See {@link #custom4Type}. */
	public void setCustom4Type(PayRateType custom4Type) {
		this.custom4Type = custom4Type;
	}

	/** See {@link #custom5Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom5_Type", nullable=false)
	public PayRateType getCustom5Type() {
		return custom5Type;
	}
	/** See {@link #custom5Type}. */
	public void setCustom5Type(PayRateType custom5Type) {
		this.custom5Type = custom5Type;
	}

	/** See {@link #custom6Type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Custom6_Type", nullable=false)
	public PayRateType getCustom6Type() {
		return custom6Type;
	}
	/** See {@link #custom6Type}. */
	public void setCustom6Type(PayRateType custom6Type) {
		this.custom6Type = custom6Type;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "payJob")
	@OrderBy("dayNum")
	public List<PayJobDaily> getPayJobDailies() {
		return payJobDailies;
	}
	public void setPayJobDailies(List<PayJobDaily> payJobDailies) {
		this.payJobDailies = payJobDailies;
	}

	/** See {@link #total10}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal10() {
		if (total10 == null) {
			calculateTotals();
		}
		return total10;
	}
	/** See {@link #total10}. */
	public void setTotal10(BigDecimal total10) {
		this.total10 = total10;
	}

	/** See {@link #total15}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal15() {
		if (total15 == null) {
			calculateTotals();
		}
		return total15;
	}
	/** See {@link #total15}. */
	public void setTotal15(BigDecimal total15) {
		this.total15 = total15;
	}

	/** See {@link #totalCust1}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust1() {
		if (totalCust1 == null) {
			calculateTotals();
		}
		return totalCust1;
	}
	/** See {@link #totalCust1}. */
	public void setTotalCust1(BigDecimal total20) {
		totalCust1 = total20;
	}

	/** See {@link #totalCust2}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust2() {
		if (totalCust2 == null) {
			calculateTotals();
		}
		return totalCust2;
	}
	/** See {@link #totalCust2}. */
	public void setTotalCust2(BigDecimal total25) {
		totalCust2 = total25;
	}

	/** See {@link #totalCust3}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust3() {
		if (totalCust3 == null) {
			calculateTotals();
		}
		return totalCust3;
	}
	/** See {@link #totalCust3}. */
	public void setTotalCust3(BigDecimal total30) {
		totalCust3 = total30;
	}

	/** See {@link #totalCust4}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust4() {
		if (totalCust4 == null) {
			calculateTotals();
		}
		return totalCust4;
	}
	/** See {@link #totalCust4}. */
	public void setTotalCust4(BigDecimal totalCust) {
		totalCust4 = totalCust;
	}

	/** See {@link #totalCust5}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust5() {
		if (totalCust5 == null) {
			calculateTotals();
		}
		return totalCust5;
	}
	/** See {@link #totalCust5}. */
	public void setTotalCust5(BigDecimal totalCust5) {
		this.totalCust5 = totalCust5;
	}

	/** See {@link #totalCust6}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalCust6() {
		if (totalCust6 == null) {
			calculateTotals();
		}
		return totalCust6;
	}
	/** See {@link #totalCust6}. */
	public void setTotalCust6(BigDecimal totalCust6) {
		this.totalCust6 = totalCust6;
	}

	/**See {@link #total10Np1}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal10Np1() {
		return total10Np1;
	}
	/**See {@link #total10Np1}. */
	public void setTotal10Np1(BigDecimal total10Np1) {
		this.total10Np1 = total10Np1;
	}

	/**See {@link #total10Np2}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal10Np2() {
		return total10Np2;
	}
	/**See {@link #total10Np2}. */
	public void setTotal10Np2(BigDecimal total10Np2) {
		this.total10Np2 = total10Np2;
	}

	/**See {@link #total15Np1}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal15Np1() {
		return total15Np1;
	}
	/**See {@link #total15Np1}. */
	public void setTotal15Np1(BigDecimal total15Np1) {
		this.total15Np1 = total15Np1;
	}

	/**See {@link #total15Np2}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal15Np2() {
		return total15Np2;
	}
	/**See {@link #total15Np2}. */
	public void setTotal15Np2(BigDecimal total15Np2) {
		this.total15Np2 = total15Np2;
	}

	/* See {@link #hasNpHours}. */
	/** True iff at least one Night Premium field is non-zero. */
	@JsonIgnore
	@Transient
	public boolean getHasNpHours() {
		return  (getTotal10Np1() != null && getTotal10Np1().signum() != 0) ||
				(getTotal10Np2() != null && getTotal10Np2().signum() != 0) ||
				(getTotal15Np1() != null && getTotal15Np1().signum() != 0) ||
				(getTotal15Np2() != null && getTotal15Np2().signum() != 0);
	}
//	/**See {@link #hasNpHours}. */
//	public void setHasNpHours(boolean hasNpHours) {
//		this.hasNpHours = hasNpHours;
//	}

	/** See {@link #totalHours}. */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalHours() {
		if (totalHours == null) {
			calculateTotals();
		}
		return totalHours;
	}
	/** See {@link #totalHours}. */
	public void setTotalHours(BigDecimal totalHours) {
		this.totalHours = totalHours;
	}

	/** See {@link #totalMpv1}. */
	@JsonIgnore
	@Transient
	public Byte getTotalMpv1() {
		if (totalMpv1 == null) {
			calculateTotals();
		}
		return totalMpv1;
	}
	/** See {@link #totalMpv1}. */
	public void setTotalMpv1(Byte totalMpv1) {
		this.totalMpv1 = totalMpv1;
	}

	/** See {@link #totalMpv2}. */
	@JsonIgnore
	@Transient
	public Byte getTotalMpv2() {
		if (totalMpv2 == null) {
			calculateTotals();
		}
		return totalMpv2;
	}
	/** See {@link #totalMpv2}. */
	public void setTotalMpv2(Byte totalMpv2) {
		this.totalMpv2 = totalMpv2;
	}

	/** See {@link #totalMpv3}. */
	@JsonIgnore
	@Transient
	public Byte getTotalMpv3() {
		if (totalMpv3 == null) {
			calculateTotals();
		}
		return totalMpv3;
	}
	/** See {@link #totalMpv3}. */
	public void setTotalMpv3(Byte totalMpv3) {
		this.totalMpv3 = totalMpv3;
	}

	/** See {@link #referenced}. */
	@JsonIgnore
	@Transient
	public boolean getReferenced() {
		return referenced;
	}
	/** See {@link #referenced}. */
	public void setReferenced(boolean referenced) {
		this.referenced = referenced;
	}

	/** See {@link #daysWorked}. */
	@JsonIgnore
	@Transient
	public short getDaysWorked() {
		return daysWorked;
	}

	/**
	 * Calculate the total hours by rate (1.0, 1.5, etc.) by summing up the
	 * PayJobDaily entries.
	 */
	public void calculateTotals() {
		calculateTotals((Boolean)null);
	}

	/**
	 * Calculate the total hours by rate (1.0, 1.5, etc.) by summing up the
	 * PayJobDaily entries for the specified ProductionPhase(s).
	 *
	 * @param doShootPhase If true, total days that are marked either as
	 *            Shooting phase, or not marked as any phase. If false, total
	 *            days that are marked as either Prep or Wrap phase. If null,
	 *            total all days regardless of phase indication.
	 * @return The first non-blank account code from the Job table from a day
	 *         matching the requested type (shoot, or prep/wrap). May be null.
	 */
	public String calculateTotals(Boolean doShootPhase) {
		String acctCode = null;
		BigDecimal aTotal10 = BigDecimal.ZERO,
				aTotal15 = BigDecimal.ZERO,
				aTotalCust1 = BigDecimal.ZERO, // Custom rate hours...
				aTotalCust2 = BigDecimal.ZERO,
				aTotalCust3 = BigDecimal.ZERO,
				aTotalCust4 = BigDecimal.ZERO,
				aTotalCust5 = BigDecimal.ZERO,
				aTotalCust6 = BigDecimal.ZERO,
				aTotal10Np1 = BigDecimal.ZERO, // Night premium hours...
				aTotal10Np2 = BigDecimal.ZERO,
				aTotal15Np1 = BigDecimal.ZERO,
				aTotal15Np2 = BigDecimal.ZERO;
		Byte aTotalMpv1 = 0,
				aTotalMpv2 = 0,
				aTotalMpv3 = 0;

		List<DailyTime> dts = weeklyTimecard.getDailyTimes();
		int day = 0;
		boolean isShootDay;
		daysWorked = 0;

		for (PayJobDaily pjd : getPayJobDailies()) {
			isShootDay = dts.get(day).getPhase() == null || dts.get(day).getPhase() == ProductionPhase.S;
			if (doShootPhase == null ||
					(doShootPhase && isShootDay) ||
					((! doShootPhase) && (! isShootDay)) ) {
				aTotal10 = NumberUtils.safeAdd(aTotal10, pjd.getHours10());
				aTotal15 = NumberUtils.safeAdd(aTotal15, pjd.getHours15());
				aTotalCust1 = NumberUtils.safeAdd(aTotalCust1, pjd.getHoursCust1());
				aTotalCust2 = NumberUtils.safeAdd(aTotalCust2, pjd.getHoursCust2());
				aTotalCust3 = NumberUtils.safeAdd(aTotalCust3, pjd.getHoursCust3());
				aTotalCust4 = NumberUtils.safeAdd(aTotalCust4, pjd.getHoursCust4());
				aTotalCust5 = NumberUtils.safeAdd(aTotalCust5, pjd.getHoursCust5());
				aTotalCust6 = NumberUtils.safeAdd(aTotalCust6, pjd.getHoursCust6());
				aTotalMpv1 = NumberUtils.safeAdd(aTotalMpv1, pjd.getMpv1());
				aTotalMpv2 = NumberUtils.safeAdd(aTotalMpv2, pjd.getMpv2());
				aTotalMpv3 = NumberUtils.safeAdd(aTotalMpv3, pjd.getMpv3());
				aTotal10Np1 = NumberUtils.safeAdd(aTotal10Np1, pjd.getHours10Np1());
				aTotal10Np2 = NumberUtils.safeAdd(aTotal10Np2, pjd.getHours10Np2());
				aTotal15Np1 = NumberUtils.safeAdd(aTotal15Np1, pjd.getHours15Np1());
				aTotal15Np2 = NumberUtils.safeAdd(aTotal15Np2, pjd.getHours15Np2());
				if (pjd.getHasHours()) {
					daysWorked++;
				}
				if (acctCode == null && ! StringUtils.isEmpty(pjd.getAccountNumber())) {
					acctCode = pjd.getAccountNumber();
				}
			}
			day++;
		}

		setTotal10(aTotal10);
		setTotal15(aTotal15);
		setTotalCust1(aTotalCust1); // Custom rate hours...
		setTotalCust2(aTotalCust2);
		setTotalCust3(aTotalCust3);
		setTotalCust4(aTotalCust4);
		setTotalCust5(aTotalCust5);
		setTotalCust6(aTotalCust6);
		setTotal10Np1(aTotal10Np1); // Night premium hours...
		setTotal10Np2(aTotal10Np2);
		setTotal15Np1(aTotal15Np1);
		setTotal15Np2(aTotal15Np2);

		setTotalHours(NumberUtils.safeAdd(aTotal10, aTotal15,
				aTotalCust1, aTotalCust2, aTotalCust3, aTotalCust4, aTotalCust5, aTotalCust6,
				aTotal10Np1, aTotal10Np2, aTotal15Np1, aTotal15Np2));

		setTotalMpv1(aTotalMpv1);
		setTotalMpv2(aTotalMpv2);
		setTotalMpv3(aTotalMpv3);

		return acctCode;
	}

	/**
	 * Calculate the total hours by rate (1.0, 1.5, etc.) by summing up the
	 * PayJobDaily entries for the specified ProductionPhase(s).
	 *
	 * @param filter The Filter to be applied to each PayJobDaily (line item
	 *            within a Job table) to determine if it is included in the
	 *            totals.
	 */
	public void calculateTotals(Filter<PayJobDaily> filter) {
		BigDecimal aTotal10 = BigDecimal.ZERO,
				aTotal15 = BigDecimal.ZERO,
				aTotalCust1 = BigDecimal.ZERO, // Custom rate hours...
				aTotalCust2 = BigDecimal.ZERO,
				aTotalCust3 = BigDecimal.ZERO,
				aTotalCust4 = BigDecimal.ZERO,
				aTotalCust5 = BigDecimal.ZERO,
				aTotalCust6 = BigDecimal.ZERO,
				aTotal10Np1 = BigDecimal.ZERO, // Night premium hours...
				aTotal10Np2 = BigDecimal.ZERO,
				aTotal15Np1 = BigDecimal.ZERO,
				aTotal15Np2 = BigDecimal.ZERO;
		Byte aTotalMpv1 = 0,
				aTotalMpv2 = 0;
		daysWorked = 0;

		for (PayJobDaily pjd : getPayJobDailies()) {
			if (filter.filter(pjd)) {
				aTotal10 = NumberUtils.safeAdd(aTotal10, pjd.getHours10());
				aTotal15 = NumberUtils.safeAdd(aTotal15, pjd.getHours15());
				aTotalCust1 = NumberUtils.safeAdd(aTotalCust1, pjd.getHoursCust1());
				aTotalCust2 = NumberUtils.safeAdd(aTotalCust2, pjd.getHoursCust2());
				aTotalCust3 = NumberUtils.safeAdd(aTotalCust3, pjd.getHoursCust3());
				aTotalCust4 = NumberUtils.safeAdd(aTotalCust4, pjd.getHoursCust4());
				aTotalCust5 = NumberUtils.safeAdd(aTotalCust5, pjd.getHoursCust5());
				aTotalCust6 = NumberUtils.safeAdd(aTotalCust6, pjd.getHoursCust6());
				aTotalMpv1 = NumberUtils.safeAdd(aTotalMpv1, pjd.getMpv1());
				aTotalMpv2 = NumberUtils.safeAdd(aTotalMpv2, pjd.getMpv2());
				aTotal10Np1 = NumberUtils.safeAdd(aTotal10Np1, pjd.getHours10Np1());
				aTotal10Np2 = NumberUtils.safeAdd(aTotal10Np2, pjd.getHours10Np2());
				aTotal15Np1 = NumberUtils.safeAdd(aTotal15Np1, pjd.getHours15Np1());
				aTotal15Np2 = NumberUtils.safeAdd(aTotal15Np2, pjd.getHours15Np2());
				if (pjd.getHasHours()) {
					daysWorked++;
				}
			}
		}

		setTotal10(aTotal10);
		setTotal15(aTotal15);
		setTotalCust1(aTotalCust1); // Custom rate hours...
		setTotalCust2(aTotalCust2);
		setTotalCust3(aTotalCust3);
		setTotalCust4(aTotalCust4);
		setTotalCust5(aTotalCust5);
		setTotalCust6(aTotalCust6);
		setTotal10Np1(aTotal10Np1); // Night premium hours...
		setTotal10Np2(aTotal10Np2);
		setTotal15Np1(aTotal15Np1);
		setTotal15Np2(aTotal15Np2);

		setTotalHours(NumberUtils.safeAdd(aTotal10, aTotal15,
				aTotalCust1, aTotalCust2, aTotalCust3, aTotalCust4, aTotalCust5, aTotalCust6,
				aTotal10Np1, aTotal10Np2, aTotal15Np1, aTotal15Np2));

		setTotalMpv1(aTotalMpv1);
		setTotalMpv2(aTotalMpv2);
	}

	/**
	 * Import data via an Importer into this instance.
	 *
	 * @param imp The Importer containing the data to be loaded.
	 * @throws EOFException
	 * @throws IOException
	 */
	public void imports(Importer imp) throws EOFException, IOException {
		//setJobNumber(imp.getBigDecimal());
		setAccountLoc(imp.getString());
//		setProdEpisode(imp.getString());
		setAccountMajor(imp.getString());
		setAccountDtl(imp.getString());
		setAccountSub(imp.getString());
		setAccountSet(imp.getString());
		setOccCode(imp.getString());
		setRate(imp.getBigDecimal());
		setPremiumRate(imp.getBigDecimal());
		setDailyRate(imp.getBigDecimal());
		setWeeklyRate(null);				// NOT in current export!
		setBoxAmt(imp.getBigDecimal()); // not currently used
		setBoxAmt(null);				// ...so erase it.
		setCustomMult1(imp.getBigDecimal());
		setCustom1Type(imp.getBoolean() ? PayRateType.P : PayRateType.H);
		setCustomMult2(imp.getBigDecimal());
		setCustom2Type(imp.getBoolean() ? PayRateType.P : PayRateType.H);
		setCustomMult3(imp.getBigDecimal());
		setCustom3Type(imp.getBoolean() ? PayRateType.P : PayRateType.H);
		setCustomMult4(imp.getBigDecimal());
		setCustom4Type(imp.getBoolean() ? PayRateType.P : PayRateType.H);
		setAccountFree(imp.getString());
		setAccountFree2(imp.getString());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record which can be loaded into Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportCrewcards(Exporter ex) {
//		ex.append(getJobNumber());
		ex.append(getAccountLoc());
//		ex.append(getProdEpisode());
		ex.append(getAccountMajor());
		if (getJobAccountNumber() != null) { // AICP/Commercial production
			ex.append(getJobAccountNumber());
		}
		else {
			ex.append(getAccountDtl());
		}
		if (getJobName() != null) { // AICP/Commercial production
			ex.append(getJobName());
		}
		else {
			ex.append(getAccountSet());
		}
		ex.append(getAccountFree());
		ex.append(getAccountFree2());
		ex.append(getOccCode());
		ex.append(getRate(), 4);
		ex.append(getWeeklyRate(), 2); // was premiumRate; changed in 2.9.5128 to weeklyRate
		ex.append(getDailyRate(), 4);
		ex.append(getBoxAmt());
		ex.append(getCustomMult1());
		ex.append(getCustom1Type() == PayRateType.P);
		ex.append(getCustomMult2());
		ex.append(getCustom2Type() == PayRateType.P);
		ex.append(getCustomMult3());
		ex.append(getCustom3Type() == PayRateType.P);
		ex.append(getCustomMult4());
		ex.append(getCustom4Type() == PayRateType.P);
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products. This method is more comprehensive than the
	 * {@link #exportCrewcards(Exporter)} method.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public void exportTabbed(Exporter ex, boolean payInfo) {
		ex.append(getJobNumber());
		ex.append(getAccountLoc());
		ex.append(getAccountMajor());
		if (getJobAccountNumber() != null) { // AICP/Commercial production
			ex.append(getJobAccountNumber());
		}
		else {
			ex.append(getAccountDtl());
		}
		ex.append(getAccountSub());
		if (getJobName() != null) { // AICP/Commercial production
			ex.append(getJobName());
		}
		else {
			ex.append(getAccountSet());
		}
		ex.append(getAccountFree());
		ex.append(getAccountFree2());
		ex.append(getOccCode());
		ex.append(getRate(), 4);
		ex.append(getWeeklyRate(), 2); // was premiumRate; changed in 2.9.5128 to weeklyRate
		ex.append(getDailyRate(), 4);
		ex.append(getCustomMult1());
		ex.append(getCustom1Type() == PayRateType.P);
		ex.append(getCustomMult2());
		ex.append(getCustom2Type() == PayRateType.P);
		ex.append(getCustomMult3());
		ex.append(getCustom3Type() == PayRateType.P);
		ex.append(getCustomMult4());
		ex.append(getCustom4Type() == PayRateType.P);

		for (PayJobDaily pjd : getPayJobDailies()) {
			pjd.exportTabbed(ex, payInfo);
		}

	}

}