package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * This class represents one line item in the StartForm's table of allowances.
 * It extends the {@link Allowance} class by adding a flag for a separate check indicator. It
 * is Embeddable, and is persisted as a part of the {@link StartForm} object.
 */
@Embeddable
@MappedSuperclass
public class AllowanceCheck extends Allowance implements Cloneable {
	private static final Log log = LogFactory.getLog(AllowanceCheck.class);

	private static final long serialVersionUID = 1L;

	/** True iff payment for this allowance should be on a separate check. */
	private boolean sepCheck = false;

	// Constructors

	/** default constructor */
	public AllowanceCheck() {
	}

	// Property accessors & mutators

	/**See {@link #sepCheck}. */
	@Column(name = "Sep_Check", nullable = false)
	public boolean getSepCheck() {
		return sepCheck;
	}
	/**See {@link #sepCheck}. */
	public void setSepCheck(boolean SepCheck) {
		sepCheck = SepCheck;
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
		ex.append(getSepCheck());
		super.exportTabbed(ex);
	}

	@Override
	public AllowanceCheck clone() throws CloneNotSupportedException {
		AllowanceCheck newDoc;
		try {
			newDoc = (AllowanceCheck)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newDoc;
	}

}