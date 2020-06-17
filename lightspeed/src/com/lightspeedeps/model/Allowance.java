package com.lightspeedeps.model;

import java.io.Serializable;

import javax.faces.model.SelectItem;
import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.Exporter;

/**
 * This class represents one line item in the StartForm's table of allowances.
 * It extends the {@link RateGroup} class by adding a field to indicate if the allowance
 * amount is daily or weekly. It is Embeddable, and is persisted as a part of
 * the {@link StartForm} object.
 */
@Embeddable
@MappedSuperclass
public class Allowance extends RateGroup implements Serializable, Cloneable {
	private static final Log log = LogFactory.getLog(Allowance.class);

	private static final long serialVersionUID = 1L;

	/** A SelectItem array to be used for choosing the weekly/daily setting. */
	public static final SelectItem[] WEEKLY_CHOICE = {
		new SelectItem(Boolean.TRUE, "Weekly"),
		new SelectItem(Boolean.FALSE, "Daily")
	};

	/** True if the rate(s) reflect a weekly amount; false if they reflect a daily amount.  */
	private boolean weekly = true;

	// Constructors

	/** default constructor */
	public Allowance() {
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
		super.exportTabbed(ex);
	}

	@Override
	public Allowance clone() throws CloneNotSupportedException {
		Allowance newDoc;
		try {
			newDoc = (Allowance)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("clone error: ", e);
			throw e;
		}
		return newDoc;
	}

}