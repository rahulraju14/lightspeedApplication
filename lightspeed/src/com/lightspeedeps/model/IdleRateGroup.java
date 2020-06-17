package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object extends the RateGroup by adding Studio and Location (Distant)
 * hours. It is used for several of the rate entries (e.g., hourly or weekly) on
 * the Start Form's StartRateSet.
 */
@Embeddable
public class IdleRateGroup extends AccountCodes implements Cloneable {
	private static final Log log = LogFactory.getLog(IdleRateGroup.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** Location (distant) rate. Idle rate sets only have distant rates; they
	 * don't apply to Studio shoots. */
	private BigDecimal loc;

	/** guaranteed hours for distant (on location) shoot */
	private BigDecimal locHrs;

	// Constructors

	/** default constructor */
	public IdleRateGroup() {
	}

	/**
	 * Set the hours and rate values to null.
	 */
	public void clearRates() {
		loc = null;
		locHrs = null;
	}

	// Property accessors


	/**See {@link #loc}. */
	@Column(name = "Loc", precision = 9, scale = 4)
	public BigDecimal getLoc() {
		return loc;
	}
	/**See {@link #loc}. */
	public void setLoc(BigDecimal Loc) {
		loc = Loc;
	}

	@Column(name = "Loc_Hrs", precision = 4, scale = 2)
	public BigDecimal getLocHrs() {
		return locHrs;
	}
	public void setLocHrs(BigDecimal LocHrs) {
		locHrs = LocHrs;
	}

	@Override
	public IdleRateGroup clone() throws CloneNotSupportedException {
		IdleRateGroup newObj;
		try {
			newObj = (IdleRateGroup)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newObj;
	}

}