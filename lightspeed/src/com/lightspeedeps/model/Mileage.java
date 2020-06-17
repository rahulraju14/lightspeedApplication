package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Mileage object -- represents the Mileage Form, and may contain one or more
 * MileageLine entries. The Mileage form is associated with a specific
 * WeeklyTimecard.
 */
@Entity
@Table(name = "mileage")
@JsonIgnoreProperties({"id","weeklyTimecard"}) // used by generateJsonSchema()
public class Mileage extends PersistentObject<Mileage> implements Cloneable {

	private static final Log log = LogFactory.getLog(Mileage.class);

	/** id for serialization */
	private static final long serialVersionUID = -5451100966963581176L;

	// Fields

	/** The timecard associated with this mileage record.  Each timecard
	 * may have zero or more associated mileage records. */
	private WeeklyTimecard weeklyTimecard;

	/** employee's comments regarding this mileage form */
	private String comments;

	/** List of detail Mileage entries corresponding to this timecard. */
	private List<MileageLine> mileageLines = new ArrayList<>();

	@Transient
	private BigDecimal miles;

	// Constructors

	/** default constructor */
	public Mileage() {
	}

	/** minimal constructor */
	public Mileage(WeeklyTimecard weeklyTimeCard) {
		weeklyTimecard = weeklyTimeCard;
	}

	private void calculateMiles() {
		miles = BigDecimal.ZERO;
		for (MileageLine ml : getMileageLines()) {
			miles = NumberUtils.safeAdd(miles, ml.getMiles());
		}
	}

	/**
	 * @return the current total of all taxable (personal) mileage
	 * listed in the mileage form.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getTaxableMiles() {
		BigDecimal sum = BigDecimal.ZERO;
		for (MileageLine ml : getMileageLines()) {
			if (ml.getTaxable()) {
				sum = NumberUtils.safeAdd(sum, ml.getMiles());
			}
		}
		return sum;
	}

	/**
	 * @return the current total of all non-taxable (business) mileage
	 * listed in the mileage form.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getNonTaxableMiles() {
		BigDecimal sum = BigDecimal.ZERO;
		for (MileageLine ml : getMileageLines()) {
			if (! ml.getTaxable()) {
				sum = NumberUtils.safeAdd(sum, ml.getMiles());
			}
		}
		return sum;
	}

	// Property accessors

	/** See {@link #weeklyTimecard}. */
	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #comments}. */
	@Column(name = "Comments", length = 5000)
	public String getComments() {
		return comments;
	}
	/** See {@link #comments}. */
	public void setComments(String destination) {
		comments = destination;
	}

	/** See {@link #mileageLines}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "mileage")
	@OrderBy("id")
	public List<MileageLine> getMileageLines() {
		return mileageLines;
	}
	/** See {@link #mileageLines}. */
	public void setMileageLines(List<MileageLine> mileageLines) {
		this.mileageLines = mileageLines;
	}

	@JsonIgnore
	@Transient
	/** See {@link #miles}. */
	public BigDecimal getMiles() {
		if (miles == null) {
			calculateMiles();
		}
		return miles;
	}
	/** See {@link #miles}. */
	public void setMiles(BigDecimal miles) {
		this.miles = miles;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Mileage clone() {
		Mileage mile;
		try {
			mile = (Mileage)super.clone();
			mile.id = null;
			mile.weeklyTimecard = null; // only one Mileage object per timecard
			mile.miles = null; // transient
		}
		catch (CloneNotSupportedException e) {
			log.error("Mileage clone error: ", e);
			return null;
		}
		return mile;
	}


	/**
	 * @return A copy of this object, including separate copies of all the
	 *         included data objects, which encompasses all of the data! (This
	 *         is significantly different than the clone() method, in which all
	 *         the embedded objects in the returned copy are either the same
	 *         instances as those in the original, or null.)
	 */
	public Mileage deepCopy() {
		Mileage mile = clone();
		mile.mileageLines = new ArrayList<>();
		for (MileageLine ml : mileageLines) {
			MileageLine mline = ml.clone();
			mline.setMileage(mile);
			mile.mileageLines.add(mline);
		}
		return mile;
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
		ex.append(getComments());
		ex.append(Constants.CREW_CARDS_NUMBER_MILEAGE_LINES);

		// MileageLine entries - ones defined + fill to 7 (crew-call max)
		int n = 0;
		for (MileageLine ml : getMileageLines()) {
			ml.flatten(ex);
			n++;
			if (n >= Constants.CREW_CARDS_NUMBER_MILEAGE_LINES) {
				break;
			}
		}
		MileageLine ml = new MileageLine();
		for (int i = n; i < Constants.CREW_CARDS_NUMBER_MILEAGE_LINES; i++) {
			ml.flatten(ex);
		}

	}

}