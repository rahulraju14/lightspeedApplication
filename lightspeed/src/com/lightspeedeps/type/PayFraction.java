/*	File Name:	PayFraction.java */
package com.lightspeedeps.type;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.lightspeedeps.util.app.Constants;

/**
 * Enumeration used in the PayRule object when a fraction or multiple
 * of some unit of pay is required to be coded.  For example, 1/3
 * of the employee's weekly pay, or 1.5 times their daily pay.
 */
public enum PayFraction {
	/** Compute 1/3 of Weekly Pay */
	W13			("1/3 Weekly Pay"),
	/** Compute 1/3 of Weekly Pay */
	W13S		("1/3 Weekly Scale Rate"),
	/** Compute 1/5 of Weekly Pay */
	W15			("1/5 Weekly Pay"),
	/** Compute 1/5 of Weekly Pay and pay in addition to regular pay. */
	W15A		("1/5 Weekly Pay, Additional"),
	/** Compute 1/5 of Weekly Scale Rate. */
	W15S		("1/5 Weekly Scale Rate"),
	/** Compute 1/6 of Weekly Pay */
	W16			("1/6 Weekly Pay"),
	/** Compute 1/6 of Weekly Pay and pay in addition to regular pay. */
	W16A		("1/6 Weekly Pay, Additional"),
	/** Compute 1/6 of Weekly Scale Rate */
	W16S		("1/6 Weekly Scale Rate"),
	/** Compute 1/6 of Weekly Scale Rate and pay in addition to regular pay */
	W16SA		("1/6 Weekly Scale Rate"),
	/** Compute 1/10 of Weekly Pay */
	W110		("1/10 Weekly Rate"),
	/** Compute 1/12 of Weekly Scale Rate */
	W112S		("1/12 Weekly Scale Rate"),
	/** Compute 2/5 of Weekly Pay */
	W25			("2/5 Weekly Pay"),
	/** Compute 1x  Daily Rate */
	D1			("1.0x Daily Pay"),
	/** Compute 2x Daily Rate */
	D2			("2.0x Daily Pay"),
	/** Compute 1.0x Table Rate (ContractRate table) */
	T1			("1.0x Table rate"),
	/** No holiday pay */
	N_A			("No Pay"),
	;

	private final String heading;

	PayFraction(String head) {
		heading = head;
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

	/**
	 * @param rate
	 * @return The value of the rate as modified by this PayFraction,
	 * computed with 2 decimal places, and rounded up.
	 */
	public BigDecimal evaluate(BigDecimal rate) {
		BigDecimal value = null;
		switch (this) {
			case W13:
			case W13S:
				value = rate.divide(Constants.DECIMAL_THREE, 6, RoundingMode.UP);
				break;
			case W15:
			case W15A:
			case W15S:
				value = rate.divide(Constants.DECIMAL_FIVE, 6, RoundingMode.UP);
				break;
			case W16:
			case W16A:
			case W16S:
			case W16SA:
				value = rate.divide(Constants.DECIMAL_SIX, 6, RoundingMode.UP);
				break;
			case W110:
				value = rate.divide(Constants.DECIMAL_10, 6, RoundingMode.UP);
				break;
			case W112S:
				value = rate.divide(Constants.DECIMAL_12, 6, RoundingMode.UP);
				break;
			case W25:
				value = rate.multiply(Constants.DECIMAL_POINT_FOUR); // 2/5 = x * 0.4
				break;
			case D1: // Daily * 1.0: assume input is Daily.
				// create new BigDecimal rather than reference to 'rate'
				value = rate.multiply(BigDecimal.ONE);
				break;
			case D2: // Daily * 2.0: assume input is Daily.
				// create new BigDecimal rather than reference to 'rate'
				value = rate.multiply(Constants.DECIMAL_TWO);
				break;
			case N_A:
				value = BigDecimal.ZERO;
				break;
		}
		return value;
	}

	public boolean isAdditional() {
		boolean add = false;
		switch(this) {
			case W15A:
			case W16A:
			case W16SA:
				add = true;
			case W13:
			case W13S:
			case W15:
			case W15S:
			case W16:
			case W16S:
			case W110:
			case W112S:
			case W25:
			case D1:
			case D2:
			case N_A:
				break;
		}
		return add;
	}

	/**
	 * @return True if this pay method is based on the employee's Daily rate
	 *         (instead of on an hourly rate).
	 */
	public boolean isDaily() {
		return (
				this == D1 ||
				this == D2
				);
	}

}
