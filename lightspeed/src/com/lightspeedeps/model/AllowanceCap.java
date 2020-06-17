package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * This object represents one line item in the StartForm's table of allowances.
 * It extends the {@link AllowanceCheck} class by adding a field for a payment
 * cap (maximum) and a flag for taxable vs non-taxable. It is Embeddable, and is
 * persisted as a part of the {@link StartForm} object.
 */
@Embeddable
public class AllowanceCap extends AllowanceCheck implements Cloneable {
	private static final Log log = LogFactory.getLog(AllowanceCap.class);

	private static final long serialVersionUID = 1L;

	/** Maximum amount payable for this allowance over the course of a production. */
	private BigDecimal paymentCap;

	/** True if this allowance is taxable, false if non-taxable. */
	private boolean taxable = false;

	// Constructors

	/** default constructor */
	public AllowanceCap() {
	}

	// Property accessors

	/**See {@link #paymentCap}. */
	@Column(name = "Payment_Cap", precision = 7, scale = 2)
	public BigDecimal getPaymentCap() {
		return paymentCap;
	}
	/**See {@link #paymentCap}. */
	public void setPaymentCap(BigDecimal paymentCap) {
		this.paymentCap = paymentCap;
	}

	/** See {@link #taxable}. */
	@Column(name = "Taxable", nullable = false)
	public boolean getTaxable() {
		return taxable;
	}
	/** See {@link #taxable}. */
	public void setTaxable(boolean taxable) {
		this.taxable = taxable;
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
		ex.append(getPaymentCap());
		ex.append(getTaxable());
		super.exportTabbed(ex);
	}

	@Override
	public AllowanceCap clone() throws CloneNotSupportedException {
		AllowanceCap newDoc;
		try {
			newDoc = (AllowanceCap)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newDoc;
	}

}