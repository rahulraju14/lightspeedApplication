package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This object extends the AccountGroup by adding Studio and Location (Distant)
 * hours. It is used for the 1.5x and 2.0x rate entries on the Start Form's
 * StartRateSet, since those entries have their rates calculated from the 1.0x
 * entries, so the rates do not need to be stored.
 */
@Embeddable
public class HoursGroup extends AccountCodes implements Cloneable {
	private static final Log log = LogFactory.getLog(HoursGroup.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** guaranteed hours for studio shoot */
	private BigDecimal studioHrs;

	/** guaranteed hours for distant (on location) shoot */
	private BigDecimal locHrs;

	// Constructors

	/** default constructor */
	public HoursGroup() {
	}

	/**
	 * Set the hour values to null.
	 */
	public void clearRates() {
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

	@Override
	public HoursGroup clone() throws CloneNotSupportedException {
		HoursGroup newObj;
		try {
			newObj = (HoursGroup)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newObj;
	}

}