package com.lightspeedeps.model;

import javax.persistence.*;

import org.apache.commons.logging.*;

/**
 * PayBreakdown entity. Each instance is a detail line in the PayBreakdown
 * table within a WeeklyTimecard.
 */
@Entity
@Table(name = "pay_breakdown")
public class PayBreakdown extends PayBreakdownMapped {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayBreakdown.class);
	private static final long serialVersionUID = -3065030383864320410L;

	// Constructors

	/** default constructor */
	public PayBreakdown() {
	}

	/** minimal constructor */
	public PayBreakdown(WeeklyTimecard wtc, byte lineNumber) {
		super(wtc, lineNumber);
	}

}