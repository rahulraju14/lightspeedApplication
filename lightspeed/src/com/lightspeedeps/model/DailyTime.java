package com.lightspeedeps.model;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.port.Importer;
import com.lightspeedeps.type.ActraCallType;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.WorkZone;

/**
 * DailyTime entity. This represents one day's information for the
 * WeeklyTimecard. Note that most of the fields are defined in the
 * DailyTimeMapped class.
 * <p>
 * Currently a standard WeeklyTimecard has exactly seven of these, but the model
 * allows for more than that, if "split days" were to be represented by extra
 * DailyTime entries.
 * <p>
 * For Canadian productions, the ACTRA contract form includes a WeeklyTimecard
 * with 13 DailyTime entries.
 * @see DailyTimeMapped
 * @see WeeklyTimecard
 */
@Entity
@Table(name = "daily_time")
@JsonIgnoreProperties({"id","weeklyTimecard","dayNum","noStartForm"}) // used by generateJsonSchema()
public class DailyTime extends DailyTimeMapped implements Cloneable {

	private static final Log LOG = LogFactory.getLog(DailyTime.class);

	private static final long serialVersionUID = 1L;

	/** The associated timecard */
	private WeeklyTimecard weeklyTimecard;

	/** The database is of the associated weeklyTimecard. */
	private long weeklyId;

	/** Number of paid hours, calculated during HTG process. */
	private BigDecimal paidHours;

	/** Time talent starts going to location. Must be non-negative and less
	 * than 24  */
	private BigDecimal trvlToLocFrom;

	/** Time talent arrives at location. Must be non-negative and less
	 * than 24  */
	private BigDecimal trvlToLocTo;

	/** Time talent leaves location. Must be non-negative and less than 24  */
	private BigDecimal trvlFromLocFrom;

	/** Time talent arrives at their destination after leaving the location.
	 * Must be non-negative and less than 24  */
	private BigDecimal trvlFromLocTo;

	/** Date for ACTRA extra-call data */
	private Date date2;

	/** Call Time for ACTRA extra-call data */
	private BigDecimal callTime2;

	/** Wrap (finish) time for ACTRA extra-call data */
	private BigDecimal wrap2;

	/** Call type (activity) for ACTRA extra-call data */
	private ActraCallType callType;

	/** Is this a valid Daily Time entry. Currently used by Talent Contracts to
	 * validate timecards.
	 */
	@Transient
	private boolean isValidDay;

	/**
	 * city list for commercial timecard based on the state selected for each
	 * row
	 */
	@Transient
	private List<SelectItem> cities = null;

	// Constructors

	/** default constructor */
	public DailyTime() {
	}

	/** normal constructor */
	public DailyTime(WeeklyTimecard wtc, byte dayNum) {
		setWeeklyTimecard(wtc);
		setDayNum(dayNum);
	}
	/** See {@link #weeklyTimecard} */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard} */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #weeklyId}. */
	@Column(name = "Weekly_Id", updatable = false, insertable = false)
	public long getWeeklyId() {
		return weeklyId;
	}
	/** See {@link #weeklyId}. */
	public void setWeeklyId(long weeklyId) {
		this.weeklyId = weeklyId;
	}

	/** See {@link #paidHours}. */
	@Column(name = "Paid_Hours", precision = 4, scale = 2)
	public BigDecimal getPaidHours() {
		return paidHours;
	}

	/** See {@link #paidHours}. */
	public void setPaidHours(BigDecimal paidHours) {
		this.paidHours = paidHours;
	}

	/** See {@link #trvlToLocFrom}. */
	@Column(name = "Trvl_To_Loc_From", precision = 4, scale = 2)
	public BigDecimal getTrvlToLocFrom() {
		return trvlToLocFrom;
	}

	/** See {@link #trvlToLocFrom}. */
	public void setTrvlToLocFrom(BigDecimal trvlToLocFrom) {
		this.trvlToLocFrom = trvlToLocFrom;
	}

	/** See {@link #trvlToLocTo}. */
	@Column(name = "Trvl_To_Loc_To", precision = 4, scale = 2)
	public BigDecimal getTrvlToLocTo() {
		return trvlToLocTo;
	}

	/** See {@link #trvlToLocTo}. */
	public void setTrvlToLocTo(BigDecimal trvlToLocTo) {
		this.trvlToLocTo = trvlToLocTo;
	}

	/** See {@link #trvlFromLocFrom}. */
	@Column(name = "Trvl_From_Loc_From", precision = 4, scale = 2)
	public BigDecimal getTrvlFromLocFrom() {
		return trvlFromLocFrom;
	}

	/** See {@link #trvlFromLocFrom}. */
	public void setTrvlFromLocFrom(BigDecimal trvlFromLocFrom) {
		this.trvlFromLocFrom = trvlFromLocFrom;
	}

	/** See {@link #trvFromoLocTo}. */
	@Column(name = "Trvl_From_Loc_To", precision = 4, scale = 2)
	public BigDecimal getTrvlFromLocTo() {
		return trvlFromLocTo;
	}
	/** See {@link #trvFromoLocTo}. */
	public void setTrvlFromLocTo(BigDecimal trvFromoLocTo) {
		this.trvlFromLocTo = trvFromoLocTo;
	}

	/** See {@link #date2}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date2", nullable = true, length = 10)
	public Date getDate2() {
		return date2;
	}
	/** See {@link #date2}. */
	public void setDate2(Date date2) {
		this.date2 = date2;
	}

	/** See {@link #callTime2}. */
	@Column(name = "call_time2", precision = 4, scale = 2)
	public BigDecimal getCallTime2() {
		return callTime2;
	}
	/** See {@link #callTime2}. */
	public void setCallTime2(BigDecimal callTime2) {
		this.callTime2 = callTime2;
	}

	/** See {@link #wrap2}. */
	@Column(name = "wrap2", precision = 4, scale = 2)
	public BigDecimal getWrap2() {
		return wrap2;
	}
	/** See {@link #wrap2}. */
	public void setWrap2(BigDecimal wrap2) {
		this.wrap2 = wrap2;
	}

	/** See {@link #callType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "call_type")
	public ActraCallType getCallType() {
		return callType;
	}
	/** See {@link #callType}. */
	public void setCallType(ActraCallType callType) {
		this.callType = callType;
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with no meal times.
	 */
	public DailyTime(WorkZone zone, BigDecimal call, BigDecimal wrp) {
		setWorkZone(zone);
		setCallTime(call);
		setWrap(wrp);
		// Set various defaults appropriate for testing
		setDayNum((byte)2);	// Monday
		setDayType( DayType.WK);
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with a single meal.
	 */
	public DailyTime(WorkZone zone, BigDecimal call, BigDecimal m1out, BigDecimal m1in, BigDecimal wrp) {
		this(zone, call, wrp);
		setM1Out(m1out);
		setM1In(m1in);
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with two meals.
	 */
	public DailyTime(WorkZone zone, BigDecimal call, BigDecimal m1out, BigDecimal m1in,
			BigDecimal m2out, BigDecimal m2in, BigDecimal wrp) {
		this(zone, call, m1out, m1in, wrp);
		setM2Out(m2out);
		setM2In(m2in);
	}

	// Constructors taking double's instead of BigDecimal's -- for ease of writing.

	/**
	 * Constructor used primarily for JUnit testing, for a day with no meal times.
	 */
	public DailyTime(WorkZone zone, double callTime, double wrap) {
		this(zone, DailyTimeMapped.decimal(callTime), DailyTimeMapped.decimal(wrap));
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with no meal times.
	 */
	public DailyTime(double callTime, double wrap) {
		this(WorkZone.ZN, DailyTimeMapped.decimal(callTime), DailyTimeMapped.decimal(wrap));
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with a single meal.
	 */
	public DailyTime(double callTime, double m1start, double m1end, double wrap) {
		this(WorkZone.ZN, decimal(callTime), decimal(m1start), decimal(m1end), decimal(wrap));
	}

	/**
	 * Constructor used primarily for JUnit testing, for a day with two meals.
	 */
	public DailyTime(double callTime, double m1start,
			double m1end, double m2start, double m2end, double wrap) {
		this(WorkZone.ZN, decimal(callTime), decimal(m1start), decimal(m1end),
				decimal(m2start), decimal(m2end), decimal(wrap));
	}


	/**
	 * @return True iff the employee reported working on this day, either by
	 *         checking the "Worked" check-box, or by reporting hours.
	 */
	public boolean reportedWork() {
		return (getWorked() ||
				(getHours() != null && getHours().signum() > 0));
	}

	/** See {@link #isValidDay} */
	@Transient
	public boolean getIsValidDay() {
		return isValidDay;
	}

	/** See {@link #isValidDay} */
	@Transient
	public void setIsValidDay(boolean isValidDay) {
		this.isValidDay = isValidDay;
	}

	/** See {@link #cities} */
	@Transient
	public List<SelectItem> getCities() {
		return cities;
	}

	/** See {@link #cities} */
	@Transient
	public void setCities(List<SelectItem> cities) {
		this.cities = cities;
	}

	/**
	 * Clear all date/time values from this instance.
	 */
	public void erase() {
		eraseInputHours();
		setWorkZone(null);
		setDayType(null);
		setCallTime2(null);
		setWrap2(null);
		setCallType(null);
		setForcedCall(false);
		setNonDeductMeal(false);
		setNonDeductMeal2(false);
		setNonDeductMealPayroll(false);
		setNonDeductMeal2Payroll(false);
		setNdmStart(null);
		setNdmEnd(null);
		setLastManIn(null);
		setMpvUser(null);
		setGrace1(null);
		setGrace2(null);
		setAccountLoc(null);
		setAccountMajor(null);
		setAccountSet(null);
		setReRate(false);
		setState(null);
		setMpv1Payroll(null);
		setMpv2Payroll(null);
		setSplitByPercent(false);
	}

	/**
	 * Clear the fields used by the employee to enter their hours,
	 * i.e., start, stop, and meal times.
	 */
	public void eraseInputHours() {
		setCallTime(null);
		setM1Out(null);
		setM1In(null);
		setM2Out(null);
		setM2In(null);
		setWrap(null);
		setHours(null);
	}

	@Override
	public DailyTime clone() {
		DailyTime newdt;
		try {
			newdt = (DailyTime)super.clone();
			newdt.id = null;
			newdt.setSelected(false); // transient
		}
		catch (CloneNotSupportedException e) {
			LOG.error("DailyTime clone error: ", e);
			return null;
		}
		return newdt;
	}

	/**
	 * @see com.lightspeedeps.model.PersistentObject#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "date=" + (getDate()==null ? "null" : getDate());
		s += "]";
		return s;
	}

	/**
	 * Import data via an Importer into this instance.
	 *
	 * @param imp The Importer containing the data to be loaded.
	 * @throws EOFException
	 * @throws IOException
	 */
	public void imports(Importer imp) throws EOFException, IOException, ParseException {
		setDate(imp.getDate());
		setCallTime(imp.getBigDecimal());
		setM1Out(imp.getBigDecimal());
		setM1In(imp.getBigDecimal());
		setM2Out(imp.getBigDecimal());
		setM2In(imp.getBigDecimal());
		setWrap(imp.getBigDecimal());
		setHours(imp.getBigDecimal());

		WorkZone zone = null;
		try {
			zone = WorkZone.valueOf(imp.getString());
		}
		catch (IllegalArgumentException e) {
		}
		setWorkZone(zone);

		DayType type = null;
		try {
			type = DayType.valueOf(imp.getString());
		}
		catch (IllegalArgumentException e) {
		}
		setDayType(type);

		setForcedCall(imp.getBoolean());
		setNonDeductMeal(imp.getBoolean());
		setNonDeductMeal2(imp.getBoolean());
		setNonDeductMealPayroll(imp.getBoolean());
		setNonDeductMeal2Payroll(imp.getBoolean());
		setNdmStart(imp.getBigDecimal());
		setNdmEnd(imp.getBigDecimal());
		setLastManIn(imp.getBigDecimal());
		setMpvUser(imp.getString());
		setGrace1(imp.getBigDecimal());
		setGrace2(imp.getBigDecimal());
		setAccountLoc(imp.getString());
		setAccountMajor(imp.getString());
		setAccountSet(imp.getString());
		setReRate(imp.getBoolean());
		setState(imp.getString());
		setMpv1Payroll(imp.getByte());
		setMpv2Payroll(imp.getByte());
		setSplitByPercent(imp.getBoolean());
		// Other split info is restore by WeeklyTimecard

	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record for loading into Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportCrewcards(Exporter ex) {
		ex.append(getDate());
		ex.append(getCallTime());
		ex.append(getM1Out());
		ex.append(getM1In());
		ex.append(getM2Out());
		ex.append(getM2In());
		ex.append(getWrap());
		ex.append(getHours());
//		ex.append((getWorkZone() == null ? "?" : getWorkZone().name()));
		ex.append((getDayType() == null ? "?" : getDayType().name()));
		ex.append(getWorked()); // ex.append(getOpposite()); 2.2.4862 - replaced opposite w/ worked
		ex.append(getForcedCall());
		ex.append(getNonDeductMeal());
		ex.append(getNonDeductMeal2());
		ex.append(getNonDeductMealPayroll());
		ex.append(getNonDeductMeal2Payroll());
		ex.append(getNdmEnd());
		ex.append(getLastManIn());
		ex.append(getMpvUser());
		ex.append(getGrace1());
		ex.append(getGrace2());
		ex.append(getAccountLoc());
		ex.append(getAccountMajor());
		ex.append(getAccountSet());
		ex.append(getReRate());
		ex.append(getState());
		ex.append(getMpv1Payroll());
		ex.append(getMpv2Payroll());
		ex.append(getSplitByPercent());
		// Other split info is output by WeeklyTimecard
//		ex.append(getSplitStart());
//		ex.append(getJobNum1());
//		ex.append(getSplitEnd1());
//		ex.append(getJobNum2());
//		ex.append(getSplitEnd2());
//		ex.append(getJobNum3());
//		ex.append(getSplitEnd3());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param tours If true, the country field is exported; if false, it is not.
	 */
	public void exportTabbed(Exporter ex, boolean tours) {
		ex.append(getDate());
		ex.append(getCallTime());
		ex.append(getM1Out());
		ex.append(getM1In());
		ex.append(getM2Out());
		ex.append(getM2In());
		ex.append(getWrap());
		ex.append(getHours());
		ex.append((getDayType() == null ? "" : getDayType().name()));
		ex.append((getWorkZone() == null ? "" : getWorkZone().name()));
		ex.append(getWorked());
		ex.append(getForcedCall());
		ex.append(getNonDeductMeal());
		ex.append(getNonDeductMeal2());
		ex.append(getNonDeductMealPayroll());
		ex.append(getNonDeductMeal2Payroll());
		ex.append(getNdbEnd());
		ex.append(getNdmStart());
		ex.append(getNdmEnd());
		ex.append(getLastManIn());
		ex.append(getMpvUser());
		ex.append(getMpv1Payroll());
		ex.append(getMpv2Payroll());
		ex.append(getGrace1());
		ex.append(getGrace2());
		ex.append(getAccountLoc());
		ex.append(getAccountMajor());
		ex.append(getAccountSet());
		ex.append(getAccountFree());
		ex.append(getReRate());
		ex.append(getOffProduction());
		ex.append(getCameraWrap());
		ex.append(getFrenchHours());
		ex.append(getCity());
		ex.append(getState());
		ex.append((getPhase() == null ? "" : getPhase().name()));

		ex.append(getSplitByPercent());
		ex.append(getJobNum1());
		ex.append(getSplitStart2());
		ex.append(getJobNum2());
		ex.append(getSplitStart3());
		ex.append(getJobNum3());
	}

}