package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.Exporter;

/**
 * This object represents a per-diem line item in the StartForm's table of
 * allowances. It extends the AccountGroup by adding a field for a payment
 * amount, and a weekly/daily flag. Unlike the RateGroup, per-diem entries only
 * have a single rate, as they only apply to distant (on location) shoots.
 */
@Embeddable
public class PerDiem extends AccountCodes implements Cloneable {
	private static final Log log = LogFactory.getLog(PerDiem.class);

	private static final long serialVersionUID = 1L;

	/** True if the rate(s) reflect a weekly amount; false if they reflect a daily amount.  */
	private boolean weekly = true;

	/** The per diem amount. */
	private BigDecimal amt;

	// Constructors

	/** default constructor */
	public PerDiem() {
	}

	// Property accessors & mutators

	/**See {@link #weekly}. */
	@Column(name = "Weekly", nullable = false)
	public boolean getWeekly() {
		return weekly;
	}
	/**See {@link #weekly}. */
	public void setWeekly(boolean flag) {
		weekly = flag;
	}

	/** Convenience method - negation of getWeekly().
	 * See {@link #weekly}. */
	@Transient
	@JsonIgnore
	public boolean getDaily() {
		return ! getWeekly();
	}

	/**See {@link #amt}. */
	@Column(name = "Amount", precision = 7, scale = 2)
	public BigDecimal getAmt() {
		return amt;
	}
	/**See {@link #amt}. */
	public void setAmt(BigDecimal dec) {
		amt = dec;
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
		ex.append(getWeekly());
		ex.append(getAmt());
		super.exportTabbed(ex);
	}

	@Override
	public PerDiem clone() throws CloneNotSupportedException {
		PerDiem newDoc;
		try {
			newDoc = (PerDiem)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newDoc;
	}

}
