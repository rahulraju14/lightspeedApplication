package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * This object extends the RateGroup by adding Studio and Location (Distant)
 * hours. It is used for several of the rate entries (e.g., hourly or weekly) on
 * the Start Form's StartRateSet.
 */
@Embeddable
public class RateHoursGroup extends RateGroup implements Cloneable {
	private static final Log log = LogFactory.getLog(RateHoursGroup.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** guaranteed hours for studio shoot */
	private BigDecimal studioHrs;

	/** guaranteed hours for distant (on location) shoot */
	private BigDecimal locHrs;

	// Constructors

	/** default constructor */
	public RateHoursGroup() {
	}

	/**
	 * Set the rate and hours fields to null.  Does not change account
	 * coding.
	 *
	 * @see com.lightspeedeps.model.RateGroup#clearRates()
	 */
	@Override
	public void clearRates() {
		super.clearRates();
		studioHrs = null;
		locHrs = null;
	}

	// Property accessors


	@Column(name = "Studio_Hrs", precision = 4, scale = 2)
	public BigDecimal getStudioHrs() {
		return studioHrs;
	}
	public void setStudioHrs(BigDecimal StudioHrs) {
		studioHrs = StudioHrs;
	}

	@Column(name = "Loc_Hrs", precision = 4, scale = 2)
	public BigDecimal getLocHrs() {
		return locHrs;
	}
	public void setLocHrs(BigDecimal LocHrs) {
		locHrs = LocHrs;
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
		ex.append(getStudioHrs());
		ex.append(getLocHrs());
		super.exportTabbed(ex);
	}

	@Override
	public RateHoursGroup clone() throws CloneNotSupportedException {
		RateHoursGroup newObj;
		try {
			newObj = (RateHoursGroup)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newObj;
	}

}