/*	File Name:	HourRoundingType.java */
package com.lightspeedeps.type;

import java.math.BigDecimal;

/**
 * Enumeration used in the PayrollPreference object to indicate
 * how employee-entered times should be rounded.  Also used in
 * contract rules to indicate how calculated time (e.g., overtime)
 * should be rounded.
 */
public enum HourRoundingType {
	/** 1/10 of an hour - normal for most IATSE unions */
	TENTH	("1/10 Hour", new BigDecimal("0.1"), 6, 10),
	/** 1/4 of an hour - often used in Commercial productions */
	FOURTH	("1/4 Hour", new BigDecimal("0.25"), 15,  4),
//	/** 1/6 of an hour - for some Canadian unions - NOT CURRENTLY USED */
//	SIXTH	("1/6 Hour", new BigDecimal(0.1), 10,  6),
	/** not specified */
	N_A		("N/A", null, 1, 60),
	/** 1/2 of an hour - used in some Commercial contracts;
	 * do not include in preference drop-down. */
	HALF	("1/2 Hour", new BigDecimal("0.5"), 30,  2),
	/** Full-hour rounding - used in DGA commercial contract. */
	ONE		("1 Hour", BigDecimal.ONE, 60,  1),
	;

	/** Text for drop-downs or other display of value. */
	private final String heading;

	/** The fraction of an hour to which a value would be rounded. */
	private BigDecimal fraction;

	/** Number of minutes in the rounding interval, e.g.,
	 * 6 minutes in 1/10th of an hour. */
	private final int minutes;

	/** The number used to divide a number of fractional units to
	 * get the decimal equivalent.  E.g., 10 for 1/10ths of an hour. */
	private final BigDecimal divisor;

	HourRoundingType(String head, BigDecimal frac, int min, int divider) {
		heading = head;
		fraction = frac;
		minutes = min;
		divisor = new BigDecimal(divider);
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

	/** See {@link #fraction}. */
	public BigDecimal getFraction() {
		return fraction;
	}

	/** See {@link #minutes}. */
	public int getMinutes() {
		return minutes;
	}

	/** See {@link #divisor}. */
	public BigDecimal getDivisor() {
		return divisor;
	}

	/**
	 * Converts a fractional rounding value into the corresponding
	 * HourRoundingType.
	 *
	 * @param fractn A fractional value (greater than zero and less than one).
	 * @return The HourRoundingType whose rounding amount corresponds to the
	 *         given fraction.
	 */
	public static HourRoundingType convertFraction(BigDecimal fractn) {
		if (fractn == null) {
			return N_A;
		}
		if (HourRoundingType.TENTH.getFraction().compareTo(fractn) == 0) {
			return TENTH;
		}
		if (HourRoundingType.FOURTH.getFraction().compareTo(fractn) == 0) {
			return FOURTH;
		}
		if (HourRoundingType.HALF.getFraction().compareTo(fractn) == 0) {
			return HALF;
		}
		if (HourRoundingType.ONE.getFraction().compareTo(fractn) == 0) {
			return ONE;
		}

		return N_A;
	}

}
