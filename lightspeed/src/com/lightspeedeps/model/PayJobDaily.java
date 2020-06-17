package com.lightspeedeps.model;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.port.Importer;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * PayJobDaily entity. Each of these represents one detail line in a specific
 * "job table", which is represented by the {@link PayJob} object. The
 * {@link #dayNum} field ties this object to a particular detail line. Each
 * {@link PayJob} should have exactly 7 PayJobDaily objects related to it, via
 * the {@link #payJob} reference.
 */
@Entity
@Table(name = "pay_job_daily")
@JsonIgnoreProperties({"id","payJob","date"}) // used by generateJsonSchema()
public class PayJobDaily extends PersistentObject<PayJobDaily> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayJobDaily.class);

	private static final long serialVersionUID = -5405892781679916561L;

	// Fields

	/** The PayJob object of which this PayJobDaily is a part. */
	private PayJob payJob;

	/** The day number, from 1 (Sunday) to 7 (Saturday) that this
	 * PayJobDaily corresponds to. */
	private byte dayNum;

	/** The date of this object; the dates of the 7 PayJobDaily objects
	 * in a PayJob should match the seven days of the week in the WeeklyTimecard
	 * this is tied to. */
	private Date date;

	/** The account number, only used for Commercial productions. */
	private String accountNumber;

	/** The number of hours charged at 1.0 x the hourly rate. */
	private BigDecimal hours10;

	/** The number of hours charged at 1.5 x the hourly rate. */
	private BigDecimal hours15;

	/** The number of hours charged at the custom multiplier 1 x the hourly rate. */
	private BigDecimal hoursCust1;

	/** The number of hours charged at the custom multiplier 2 x the hourly rate. */
	private BigDecimal hoursCust2;

	/** The number of hours charged at the custom multiplier 3 x the hourly rate. */
	private BigDecimal hoursCust3;

	/** The number of hours charged at the custom multiplier 4 x the hourly rate. */
	private BigDecimal hoursCust4;

	/** The number of hours charged at the custom multiplier 54 x the hourly rate. */
	private BigDecimal hoursCust5;

	/** The number of hours charged at the custom multiplier 6 x the hourly rate. */
	private BigDecimal hoursCust6;

	/** The number of Night Premium straight (1.0x) hours, paid at 1.1 x the hourly rate. */
	private BigDecimal hours10Np1;

	/** The number of Night Premium straight (1.0x) hours, paid at 1.2 x the hourly rate. */
	private BigDecimal hours10Np2;

	/** The number of Night Premium overtime (1.5x) hours, paid at 1.1 x the overtime hourly rate. */
	private BigDecimal hours15Np1;

	/** The number of Night Premium overtime (1.5x) hours, paid at 1.2 x the overtime hourly rate. */
	private BigDecimal hours15Np2;

	/** The number of MPVs in the AM charged to this day and job. */
	private Byte mpv1;

	/** The number of MPVs in the PM charged to this day and job. */
	private Byte mpv2;

	/** The number of MPVs for the period after the second meal charged to this day and job. */
	private Byte mpv3;

	/** True if this instance has hours from a "special" day type; used for filtering entries
	 * when calculating totals by account number. */
	@Transient
	private boolean isSpecial;

	// Constructors

	/** default constructor */
	public PayJobDaily() {
	}

	/** minimal constructor */
	public PayJobDaily(PayJob payJob, byte dayNum, Date date, Byte mpv1, Byte mpv2) {
		this.payJob = payJob;
		this.dayNum = dayNum;
		this.date = date;
		this.mpv1 = mpv1;
		this.mpv2 = mpv2;
	}

	// Property accessors

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Job_Id", nullable = false)
	public PayJob getPayJob() {
		return payJob;
	}
	public void setPayJob(PayJob payJob) {
		this.payJob = payJob;
	}

	@Column(name = "Day_Num", nullable = false)
	public byte getDayNum() {
		return dayNum;
	}
	public void setDayNum(byte dayNum) {
		this.dayNum = dayNum;
	}

	@JsonIgnore
	@Temporal(TemporalType.DATE)
	@Column(name = "Date", nullable = false, length = 10)
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #accountNumber}. */
	@Column(name = "Account_Number", length = 10)
	public String getAccountNumber() {
		return accountNumber;
	}
	/** See {@link #accountNumber}. */
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	@Column(name = "Hours_10", precision = 4, scale = 2)
	public BigDecimal getHours10() {
		return hours10;
	}
	public void setHours10(BigDecimal hours10) {
		this.hours10 = hours10;
	}

	@Column(name = "Hours_15", precision = 4, scale = 2)
	public BigDecimal getHours15() {
		return hours15;
	}
	public void setHours15(BigDecimal hours15) {
		this.hours15 = hours15;
	}

	@Column(name = "Hours_Cust1", precision = 4, scale = 2)
	public BigDecimal getHoursCust1() {
		return hoursCust1;
	}
	public void setHoursCust1(BigDecimal hours20) {
		hoursCust1 = hours20;
	}

	/** See {@link #hoursCust2}. */
	@Column(name = "Hours_Cust2", precision = 4, scale = 2)
	public BigDecimal getHoursCust2() {
		return hoursCust2;
	}
	/** See {@link #hoursCust2}. */
	public void setHoursCust2(BigDecimal hours25) {
		hoursCust2 = hours25;
	}

	@Column(name = "Hours_Cust3", precision = 4, scale = 2)
	public BigDecimal getHoursCust3() {
		return hoursCust3;
	}
	public void setHoursCust3(BigDecimal hours30) {
		hoursCust3 = hours30;
	}

	@Column(name = "Hours_Cust4", precision = 4, scale = 2)
	public BigDecimal getHoursCust4() {
		return hoursCust4;
	}
	public void setHoursCust4(BigDecimal hoursCust) {
		hoursCust4 = hoursCust;
	}

	/** See {@link #hoursCust5}. */
	@Column(name = "Hours_Cust5", precision = 4, scale = 2)
	public BigDecimal getHoursCust5() {
		return hoursCust5;
	}
	/** See {@link #hoursCust5}. */
	public void setHoursCust5(BigDecimal hoursCust5) {
		this.hoursCust5 = hoursCust5;
	}

	/** See {@link #hoursCust6}. */
	@Column(name = "Hours_Cust6", precision = 4, scale = 2)
	public BigDecimal getHoursCust6() {
		return hoursCust6;
	}
	/** See {@link #hoursCust6}. */
	public void setHoursCust6(BigDecimal hoursCust6) {
		this.hoursCust6 = hoursCust6;
	}

	/**See {@link #hours10Np1}. */
	@Column(name = "Hours_10Np1", precision = 4, scale = 2)
	public BigDecimal getHours10Np1() {
		return hours10Np1;
	}
	/**See {@link #hours10Np1}. */
	public void setHours10Np1(BigDecimal hours10Np1) {
		this.hours10Np1 = hours10Np1;
	}

	/**See {@link #hours10Np2}. */
	@Column(name = "Hours_10Np2", precision = 4, scale = 2)
	public BigDecimal getHours10Np2() {
		return hours10Np2;
	}
	/**See {@link #hours10Np2}. */
	public void setHours10Np2(BigDecimal hours10Np2) {
		this.hours10Np2 = hours10Np2;
	}

	/**See {@link #hours15Np1}. */
	@Column(name = "Hours_15Np1", precision = 4, scale = 2)
	public BigDecimal getHours15Np1() {
		return hours15Np1;
	}
	/**See {@link #hours15Np1}. */
	public void setHours15Np1(BigDecimal hours15Np1) {
		this.hours15Np1 = hours15Np1;
	}

	/**See {@link #hours15Np2}. */
	@Column(name = "Hours_15Np2", precision = 4, scale = 2)
	public BigDecimal getHours15Np2() {
		return hours15Np2;
	}
	/**See {@link #hours15Np2}. */
	public void setHours15Np2(BigDecimal hours15Np2) {
		this.hours15Np2 = hours15Np2;
	}

	@Column(name = "Mpv1")
	public Byte getMpv1() {
		return mpv1;
	}
	public void setMpv1(Byte mpv1) {
		this.mpv1 = mpv1;
	}

	@Column(name = "Mpv2")
	public Byte getMpv2() {
		return mpv2;
	}
	public void setMpv2(Byte mpv2) {
		this.mpv2 = mpv2;
	}

	/** See {@link #mpv3}. */
	@Column(name = "Mpv3")
	public Byte getMpv3() {
		return mpv3;
	}
	/** See {@link #mpv3}. */
	public void setMpv3(Byte mpv3) {
		this.mpv3 = mpv3;
	}

	/** True iff at least one Night Premium field is non-zero. */
	@JsonIgnore
	@Transient
	public boolean getHasNpHours() {
		return  (getHours10Np1() != null && getHours10Np1().signum() != 0) ||
				(getHours10Np2() != null && getHours10Np2().signum() != 0) ||
				(getHours15Np1() != null && getHours15Np1().signum() != 0) ||
				(getHours15Np2() != null && getHours15Np2().signum() != 0);
	}

	@JsonIgnore
	@Transient
	public boolean getHasHours() {
		return (getHasNpHours() ||
				(getHours10() != null && getHours10().signum() != 0) ||
				(getHours15() != null && getHours15().signum() != 0) ||
				(getHoursCust1() != null && getHoursCust1().signum() != 0) ||
				(getHoursCust2() != null && getHoursCust2().signum() != 0) ||
				(getHoursCust3() != null && getHoursCust3().signum() != 0) ||
				(getHoursCust4() != null && getHoursCust4().signum() != 0) ||
				(getHoursCust5() != null && getHoursCust5().signum() != 0) ||
				(getHoursCust6() != null && getHoursCust6().signum() != 0) );
	}

	/**
	 * @return The total of all the hours in this PayJobDaily instance. Returns
	 *         zero if all entries are null.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalHours() {
		BigDecimal total = NumberUtils.safeAdd(getHours10(), getHours15(),
				getHours10Np1(), getHours10Np2(), getHours15Np1(), getHours15Np2(),
				getHoursCust1(), getHoursCust2(), getHoursCust3(), getHoursCust4(), getHoursCust5(), getHoursCust6());
		if (total == null) {
			return BigDecimal.ZERO;
		}
		return total;
	}

	/**
	 * @return The total of all the 1.0x & 1.5x hours, including Night Premiums,
	 *         in this PayJobDaily instance. Returns zero if all entries are
	 *         null.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getTotalNonCustomHours() {
		BigDecimal total = NumberUtils.safeAdd(getHours10(), getHours15(),
				getHours10Np1(), getHours10Np2(), getHours15Np1(), getHours15Np2());
		if (total == null) {
			return BigDecimal.ZERO;
		}
		return total;
	}

	/** See {@link #isSpecial}. */
	@JsonIgnore
	@Transient
	public boolean getIsSpecial() {
		return isSpecial;
	}
	/** See {@link #isSpecial}. */
	public void setIsSpecial(boolean isSpecial) {
		this.isSpecial = isSpecial;
	}

	/**
	 * Set all of this instances fields to null.
	 */
	public void eraseValues() {
		setHours10(null);
		setHours15(null);
		setHoursCust1(null);
		setHoursCust2(null);
		setHoursCust3(null);
		setHoursCust4(null);
		setHoursCust5(null);
		setHoursCust6(null);
		setHours10Np1(null);
		setHours10Np2(null);
		setHours15Np1(null);
		setHours15Np2(null);
		setMpv1(null);
		setMpv2(null);
		setMpv3(null);
		setIsSpecial(false);
	}

	/**
	 * Import data via an Importer into this instance.
	 *
	 * @param imp The Importer containing the data to be loaded.
	 * @throws EOFException
	 * @throws IOException
	 */
	public void imports(Importer imp) throws EOFException, IOException {
		//setDayNum(imp.getByte());
		//setDate(imp.getDate());
		setHours10(imp.getBigDecimal());
		setHours15(imp.getBigDecimal());
		setHoursCust1(imp.getBigDecimal());
		setHoursCust2(imp.getBigDecimal());
		setHoursCust3(imp.getBigDecimal());
		setHoursCust4(imp.getBigDecimal());
		setMpv1(imp.getByte());
		setMpv2(imp.getByte());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record which can be loaded into Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public void exportCrewcards(Exporter ex, boolean payInfo) {
		//ex.append(getDayNum());
		//ex.append(getDate());
		if (payInfo) {
			ex.append(getHours10());
			ex.append(getHours15());
			ex.append(getHoursCust1());
			ex.append(getHoursCust2());
			ex.append(getHoursCust3());
			ex.append(getHoursCust4());
			ex.append(getMpv1());
			//	ex.append(getMpv2());	// temporarily remove. 2.2.4740
		}
		else {
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((Byte)null);
		}
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record which can be loaded into other
	 * programs. This method is more comprehensive than
	 * {@link #exportCrewcards(Exporter,boolean)}.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public void exportTabbed(Exporter ex, boolean payInfo) {
		ex.append(getDayNum());
		ex.append(getAccountNumber());
		if (payInfo) {
			ex.append(getHours10());
			ex.append(getHours15());
			ex.append(getHoursCust1());
			ex.append(getHoursCust2());
			ex.append(getHoursCust3());
			ex.append(getHoursCust4());
			ex.append(getHours10Np1());
			ex.append(getHours10Np2());
			ex.append(getHours15Np1());
			ex.append(getHours15Np2());
			ex.append(getMpv1());
			ex.append(getMpv2());
		}
		else {
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((BigDecimal)null);
			ex.append((Byte)null);
			ex.append((Byte)null);
		}
	}

	/**
	 * Create the 7 PayJobDaily entries required by a PayJob.
	 *
	 * @param pj The PayJob to hold the entries.
	 * @param wedate The week-ending date of this PayJob. The individual
	 *            PayJobDaily entries will be assigned the seven dates ending
	 *            with this date.
	 */
	public static void createPayJobDailies(PayJob pj, Date wedate) {
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		calendar.setTime(wedate);
		calendar.add(Calendar.DAY_OF_YEAR, -6);
		List<PayJobDaily> payJobDailies = pj.getPayJobDailies();
		for (byte day = 1; day < 8; day++) {
			payJobDailies.add(new PayJobDaily(pj, day, calendar.getTime(), null, null));
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
	}

}
