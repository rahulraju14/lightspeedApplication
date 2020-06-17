package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;

/**
 * Mileage object -- zero or more Mileage entries may be associated with
 * a WeeklyTimecard.
 */
@Entity
@Table(name = "mileage_line")
@JsonIgnoreProperties({"id","mileage"}) // used by generateJsonSchema()
public class MileageLine extends PersistentObject<MileageLine> implements Cloneable {

	private static final Log log = LogFactory.getLog(MileageLine.class);

	/** id for serialization */
	private static final long serialVersionUID = -5451100966963581176L;

	// Fields

	/** The Mileage form associated with this mileage record.  Each form
	 * may have zero or more associated mileage records. */
	private Mileage mileage;
//	/** The (free-form) date field used prior to 2.2.4484. */
//	private String dateStr;
	/** The date this mileage was accrued. */
	private Date date;
	/** The destination or other notes regarding this mileage. */
	private String destination;
	/** The starting odometer value (#######.#). Must be non-negative. */
	private BigDecimal odometerStart;
	/** The ending odometer value (#######.#). Must be non-negative.*/
	private BigDecimal odometerEnd;
	/** The mileage traveled or being charged - may be negative. */
	private BigDecimal miles;
	/** True iff this entry is for taxable/personal (not allowable/business) mileage. */
	private boolean taxable;

	// Constructors

	/** default constructor */
	public MileageLine() {
	}

	/** minimal constructor */
	public MileageLine(Mileage mileage) {
		this.mileage = mileage;
	}

	// Property accessors

	/** See {@link #mileage}. */
	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Mileage_Id", nullable = false)
	public Mileage getMileage() {
		return mileage;
	}
	/** See {@link #mileage}. */
	public void setMileage(Mileage mileage) {
		this.mileage = mileage;
	}

	/** See {@link #date}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date", length = 10)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

//	/** See {@link #dateStr}. */
//	@Column(name = "Date_str", length = 20)
//	public String getDateStr() {
//		return dateStr;
//	}
//	/** See {@link #dateStr}. */
//	public void setDateStr(String date) {
//		dateStr = date;
//	}

	/** See {@link #destination}. */
	@Column(name = "Destination", length = 40)
	public String getDestination() {
		return destination;
	}
	/** See {@link #destination}. */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/** See {@link #odometerStart}. */
	@Column(name = "Odometer_Start", precision = 7, scale = 1)
	public BigDecimal getOdometerStart() {
		return odometerStart;
	}
	/** See {@link #odometerStart}. */
	public void setOdometerStart(BigDecimal odometerStart) {
		this.odometerStart = odometerStart;
	}

	/** See {@link #odometerEnd}. */
	@Column(name = "Odometer_End", precision = 7, scale = 1)
	public BigDecimal getOdometerEnd() {
		return odometerEnd;
	}
	/** See {@link #odometerEnd}. */
	public void setOdometerEnd(BigDecimal odometerEnd) {
		this.odometerEnd = odometerEnd;
	}

	/** See {@link #miles}. */
	@Column(name = "Miles", precision = 6, scale = 1)
	public BigDecimal getMiles() {
		return miles;
	}
	/** See {@link #miles}. */
	public void setMiles(BigDecimal miles) {
		this.miles = miles;
	}

	/** See {@link #taxable}. */
	@Column(name = "taxable", nullable = false)
	public boolean getTaxable() {
		return taxable;
	}
	/** See {@link #taxable}. */
	public void setTaxable(boolean taxable) {
		this.taxable = taxable;
	}

	@Override
	public MileageLine clone() {
		MileageLine mline;
		try {
			mline = (MileageLine)super.clone();
			mline.id = null;
		}
		catch (CloneNotSupportedException e) {
			log.error("MileageLine clone error: ", e);
			return null;
		}
		return mline;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void flatten(Exporter ex) {
//		ex.append(getId());
		ex.append(getDate());
		ex.append(getDestination());
		ex.append(getOdometerStart());
		ex.append(getOdometerEnd());
		ex.append(getMiles());
		ex.append(getTaxable());
	}

}
