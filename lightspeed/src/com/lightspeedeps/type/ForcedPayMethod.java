/*	File Name:	ForcedPayMethod.java */
package com.lightspeedeps.type;

import java.math.BigDecimal;

import com.lightspeedeps.util.app.Constants;

/**
 * Enumeration used in the PayRule object to indicate what method
 * should be used to pay "forced call" (rest invasion) time.
 */
public enum ForcedPayMethod {

			// PR,  INV,  Rate, InitHrs, Rate2, Desc
	/** Pay prevailing rate until next complete rest period */
	PR		(true, false, null, null, null, "Prevailing rate until next rest"),

	/** Pay prevailing rate for invaded hours only */
	PRI		(true, true, null, null, null, "Pay prevailing rate for invaded hours only"),

	/** Pay additional 1.0x the base rate for invaded hours */
	X1PI	(false, true, BigDecimal.ONE, null, null, "Additional 1.0x base rate for invaded hours"),

	/** Pay additional 1.0x the base rate for all worked hours */
	X1P		(false, false, BigDecimal.ONE, null, null, "Additional 1.0x base rate for all hours"),

	/** Pay additional 1.5x the base rate for invaded hours */
	X15PI	(false, true, Constants.DECIMAL_ONE_FIVE, null, null, "Additional 1.5x base rate for invaded hours"),

	/** Pay additional 1.5x the base rate for all worked hours */
	X15P	(false, false, Constants.DECIMAL_ONE_FIVE, null, null, "Additional 1.5x base rate for all hours"),

	/** Pay additional 2.25x the base rate for invaded hours */
	X225PI	(false, true, new BigDecimal("2.25"), null, null, "Additional 2.25x base rate for invaded hours"),

	/** Pay additional 2.25x the base rate for all worked hours */
	X225P	(false, false, new BigDecimal("2.25"), null, null, "Additional 2.25x base rate for all hours"),

	/** Pay additional 3.0x the base rate for invaded hours */
	X3PI	(false, true, Constants.DECIMAL_THREE, null, null, "Additional 3.0x base rate for invaded hours"),

	/** Pay additional 3.0x the base rate for all worked hours */
	X3P		(false, false, Constants.DECIMAL_THREE, null, null, "Additional 3.0x base rate for all hours"),

	/** Pay 1.5x the base rate for invaded hours up to 8 hours, then 2.25x to 12, then 3x */
	R15I8R225I12R3X	(false, true, Constants.DECIMAL_ONE_FIVE, Constants.DECIMAL_EIGHT, new BigDecimal("2.25"), "Pay 1.5x base rate for invaded hours up to 8 hours, 2.25x to 12, then 3x"),

	/** Pay 1.5x the base rate for all worked hours up to 8 hours, then 2.25x to 12, then 3x */
	R158R22512R3X	(false, false, Constants.DECIMAL_ONE_FIVE, Constants.DECIMAL_EIGHT, new BigDecimal("2.25"), "Pay 1.5x base rate for all hours up to 8 hours, 2.25x to 12. then 3x"),

	/** Pay prevailing rate + 1.0x the base rate for invaded hours */
	PRX1PI	(false, true, BigDecimal.ONE, null, null, "Prevailing rate plus 1.0x base rate for invaded hours"),

	/** Pay prevailing rate + 1.0x the base rate for all worked hours */
	PRX1P	(false, false, BigDecimal.ONE, null, null, "Prevailing rate plus 1.0x base rate for all hours"),

	/** Pay additional 0.5x of the Daily rate */
	X05DP	(false, false, new BigDecimal("0.5"), null, null, "Additional 1/2 of daily rate"),

	/** Pay additional 1.0x of the Daily rate */
	X10DP	(false, false, BigDecimal.ONE, null, null, "Additional 1x daily rate"),

	/** Pay additional 1.0x daily rate for each 5 hour period worked. */
	X10D5P	(false, false, BigDecimal.ONE.negate(), Constants.DECIMAL_FIVE, null, "One day's additional pay for each 5 hour period worked"),

	/** Pay additional 1.0x daily rate for each 5 hour period worked. */
	X10D6P	(false, false, BigDecimal.ONE.negate(), Constants.DECIMAL_SIX, null, "One day's additional pay for each 6 hour period worked"),

	/** Pay 2.0x for invaded hours only */
	R15I	(false, true, Constants.DECIMAL_ONE_FIVE, null, null, "Pay 1.5x for invaded hours only"),

	/** Pay 2.0x for all time until full rest period */
	R20		(false, false, Constants.DECIMAL_TWO, null, null, "Pay 2.0x for all time until full rest period"),

	/** Pay 2.0x for invaded hours only */
	R20I	(false, true, Constants.DECIMAL_TWO, null, null, "Pay 2.0x for invaded hours only"),

	/** Special: Pay 2x straight additional for first two invaded hours, then 3x Prevailing for remaining invaded hours. */
	X2I3PR	(false, true, Constants.DECIMAL_TWO, Constants.DECIMAL_THREE, Constants.DECIMAL_TWO, "Pay +2x for first 2 invaded hrs, and 3xP.R. for the rest"),

	/** Pay 2.25x for all time until full rest period */
	R225	(false, false, new BigDecimal("2.25"), null, null, "Pay 2.25x for all time until full rest period"),

	/** Pay 2.25x for invaded hours only */
	R225I	(false, true, new BigDecimal("2.25"), null, null, "Pay 2.25x for invaded hours only"),

	/** Pay 2.5x for all time until full rest period */
	R25		(false, false, new BigDecimal("2.5"), null, null, "Pay 2.5x for all time until full rest period"),

	/** Pay 2.5x for invaded hours only */
	R25I	(false, true, new BigDecimal("2.5"), null, null, "Pay 2.5x for invaded hours only"),

	/** Pay additional 3.0x for every 1/2 hour of invaded hours, in 1/2-hour increments */
	X3PI5	(false, true, Constants.DECIMAL_THREE, null, null, "Pay 3.0x per 1/2-hr, invaded hours only"),

	/** Pay additional fixed amount for every hour of invaded hours, in 1/2-hour increments */
	FXI5	(false, true, null, null, null, "Pay fixed amount per hour, invaded hours only, 1/2-hour increments"),

	/** Pay 3.0x for all time until full rest period */
	R30		(false, false, Constants.DECIMAL_THREE, null, null, "Pay 3.0x for all time until full rest period"),

	/** Pay 3.0x for invaded hours only */
	R30I	(false, true, Constants.DECIMAL_THREE, null, null, "Pay 3.0x for invaded hours only"),

	/** Pay 3.3x for all time until full rest period */
	R33		(false, false, new BigDecimal("3.3"), null, null, "Pay 3.3x for all time until full rest period"),

	/** Pay 3.3x for invaded hours only */
	R33I	(false, true, new BigDecimal("3.3"), null, null, "Pay 3.3x for invaded hours only"),

	/** Pay 4.95x for all time until full rest period */
	R495	(false, false, new BigDecimal("4.95"), null, null, "Pay 4.95x for all time until full rest period"),

	/** Pay 6.6x for all time until full rest period */
	R66		(false, false, new BigDecimal("6.6"), null, null, "Pay 6.6x for all time until full rest period"),
	;

	/** Text to display for this option. */
	private String heading;

	/** True if this method uses the "prevailing" pay rate. */
	private final boolean prevailing;

	/** True if this method only pays extra for the invaded hours; false means it
	 * will pay extra for all worked hours until the next full rest period. */
	private final boolean invaded;

	/** The multiplier to use; not applicable if {@link #prevailing} is true. */
	private final BigDecimal multiplier;

	/** */
	private final BigDecimal multiplier2;

	/** */
	private final BigDecimal initialHours;


	ForcedPayMethod(boolean prevail, boolean invade, BigDecimal rate, BigDecimal initHrs, BigDecimal rate2, String head) {
		prevailing = prevail;
		invaded = invade;
		multiplier = rate;
		multiplier2 = rate2;
		initialHours = initHrs;
		heading = head;
	}

	/** See {@link #prevailing}. */
	public boolean getPrevailing() {
		return prevailing;
	}

	/** See {@link #invaded}. */
	public boolean getInvaded() {
		return invaded;
	}

	/** See {@link #multiplier}. */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/** See {@link #initialHours}. */
	public BigDecimal getInitialHours() {
		return initialHours;
	}

	/** See {@link #multiplier2}. */
	public BigDecimal getMultiplier2() {
		return multiplier2;
	}

	/**
	 * @return True if this pay method is based on the employee's Daily rate
	 *         (instead of on an hourly rate).
	 */
	public boolean isDaily() {
		return (
				this == X05DP ||
				this == X10D5P ||
				this == X10D6P ||
				this == X10DP
				);
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

}
