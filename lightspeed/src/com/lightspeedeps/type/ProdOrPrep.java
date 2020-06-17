/**
 * File: ProdOrPrep.java
 */
package com.lightspeedeps.type;

/**
 * Enum used in HTG (PayJobService) to indicate whether the current
 * weeklyTimecard is using Prod rates, Prep rates, or a mix of the two.
 */
public enum ProdOrPrep {
	/** Week is all Production rate */
	PROD,

	/** Week is all Prep rate */
	PREP,

	/** Week is split: "Prep end" date falls during the week. Note that
	 * days following that date might still be set to Prep via phase or
	 * DayType. */
	SPLIT,

	/** Week has mix of Prep/Shoot due to phase (commercial) or DayType,
	 * and Prep End date is either not specified, or does NOT fall during
	 * the week. */
	MIXED
}
