package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * This object extends the AccountGroup by adding Studio and Location (Distant)
 * rates. It is the basis for rate table and allowance table entries in
 * a Start Form's StartRateSet.
 */
@Embeddable
@MappedSuperclass
public class RateGroup extends AccountCodes implements Cloneable {
	private static final Log log = LogFactory.getLog(RateGroup.class);

	private static final long serialVersionUID = 1L;


	/** Studio rate.  It is possible to have different rates for Studio vs Location. */
	private BigDecimal studio;

	/** Location (distant) rate.  It is possible to have different rates for Studio vs Location. */
	private BigDecimal loc;

	// Constructors

	/** default constructor */
	public RateGroup() {
	}

	/**
	 * Set the rate values to null. Does not change account
	 * coding.
	 */
	public void clearRates() {
		studio = null;
		loc = null;
	}

	// Property accessors/mutators

	/**See {@link #studio}. */
	@Column(name = "Studio", precision = 9, scale = 4)
	public BigDecimal getStudio() {
		return studio;
	}
	/**See {@link #studio}. */
	public void setStudio(BigDecimal Studio) {
		studio = Studio;
	}

	/**See {@link #loc}. */
	@Column(name = "Loc", precision = 9, scale = 4)
	public BigDecimal getLoc() {
		return loc;
	}
	/**See {@link #loc}. */
	public void setLoc(BigDecimal Loc) {
		loc = Loc;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	@Override
	public void exportTabbed(Exporter ex) {
		ex.append(getStudio());
		ex.append(getLoc());
		super.exportTabbed(ex);
	}

	@Override
	public RateGroup clone() throws CloneNotSupportedException {
		RateGroup newDoc;
		try {
			newDoc = (RateGroup)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newDoc;
	}

}