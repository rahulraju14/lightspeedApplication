/*	File Name:	PayCategory.java */
package com.lightspeedeps.type;

import java.math.BigDecimal;

import com.lightspeedeps.util.common.NumberUtils;

/**
 * Enumeration used in a PayBreakdown object to indicate the type of the line
 * item. These entries are the choices available to the user in the Pay
 * Breakdown table's Category column drop-down. The entries with a "true" value
 * for the "isExpense" field are available to the user in the Allowance/Expense
 * table drop-down.
 */
												//			 Full	Empl.
public enum PayCategory {				//		Mult, Labor, Expns, Expns, FLSA,  taxble, abbr
	CUSTOM				( "---",				null,  true, false, false, false, false, null),
	X10_HOURS			( "1.0x Hours",			1.0f,  true, false, false, false, false, null),
	X11_HOURS			( "1.1x Hours",			1.1f,  true, false, false, false, false, null),
	X12_HOURS			( "1.2x Hours",			1.2f,  true, false, false, false, false, null),
	X15_HOURS			( "1.5x Hours",			1.5f,  true, false, false, false, false, null),
	X20_HOURS			( "2.0x Hours",			2.0f,  true, false, false, false, false, null),
	X25_HOURS			( "2.5x Hours",			2.5f,  true, false, false, false, false, null),
	X30_HOURS			( "3.0x Hours",			3.0f,  true, false, false, false, false, null),
	X35_HOURS			( "3.5x Hours",			3.5f,  true, false, false, false, false, null),
	X375_HOURS			( "3.75x Hours",		3.75f, true, false, false, false, false, null),
	X40_HOURS			( "4.0x Hours",			4.0f,  true, false, false, false, false, null),
	X50_HOURS			( "5.0x Hours",			5.0f,  true, false, false, false, false, null),
	X10_HOURS_PREM		( "1.0x Hours (Prem)",	1.0f,  true, false, false, false, false, null),
	X15_HOURS_PREM		( "1.5x Hours (Prem)",	1.5f,  true, false, false, false, false, null),
	X20_HOURS_PREM		( "2.0x Hours (Prem)",	2.0f,  true, false, false, false, false, null),
	X25_HOURS_PREM		( "2.5x Hours (Prem)",	2.5f,  true, false, false, false, false, null),
	X30_HOURS_PREM		( "3.0x Hours (Prem)",	3.0f,  true, false, false, false, false, null),
	X35_HOURS_PREM		( "3.5x Hours (Prem)",	3.5f,  true, false, false, false, false, null),
	X375_HOURS_PREM		( "3.75x Hours (Prem)",	3.75f, true, false, false, false, false, null),
	X40_HOURS_PREM		( "4.0x Hours (Prem)",	4.0f,  true, false, false, false, false, null),
	X50_HOURS_PREM		( "5.0x Hours (Prem)",	5.0f,  true, false, false, false, false, null),
	DAY6				( "6th Day",			null,  true, false, false, false, false, null),
	DAY7				( "7th Day",			null,  true, false, false, false, false, null),
	TWO_CAMERAS			( "2 Cameras",			null,  true, true,  false, true,  true, null),
	ADVANCE_AFTER_NET	( "Advance after net",	null, false, true,  false, false, false, null),
	ADDL_PAY_LABOR		( "Addl. Pay - Labor",	null,  true, true,  true,  true,  true, null),
	ADJUSTMENT			( "Adjustment",			null, false, false, false, false, false, null),
	AGENT_FEE_NONTAX	( "Agent Fee - NonTax",	null, false, true,  false, false, false, null),
	AUTO_EXP			( "Auto Expense",		null, false, true,  true,  false, false, "AUT"),
	BONUS				( "Bonus",				null, false, true,  true,  false, false, "BON"),
	BOX_RENT_NONTAX		( "Box Rent - NonTax",	null, false, true,  false, false, false, null),
	BOX_RENT_TAX		( "Box Rent - Txbl",	null, false, true,  false, false, true, null),
	CAN_TAX_DED			( "Canada Tax deduction",null, false, true,  false, false, false, "CANADA"),
	CAR_TAXABLE			( "Car - Taxable",		null, false, true,  true,  true,  true, null),
	CAR_ALLOWANCE		( "Car Allowance",		null, false, true,  true,  true,  false, null),
	COMP_OF_ASGN		( "COA",				null,  true, false, false, false, false, null),
	DAILY_OT			( "Daily OT",			null,  true, false, false, false, false, null),
	DGA_WRAP_ALLOW		( "DGA Wrap Allow",		null, false, false, false, false, false, null),
	DISTANT_LOC_ALLOW	( "Dist Loc Allow",		null, false, true,  true,  false, false, null),
	DRUG_TEST			( "Drug Test", 			null,  true, false, false, false, false, null),
	FLAT_RATE			( "Flat Rate",			null,  true, false, false, false, false, null),
	FLSA				( "FLSA Adjustment",	null,  true, false, false, false, false, null),
	FORCE_CALL			( "Force Call",			null,  true, false, false, false, false, null),
//	FOREIGN_FED_TAX		( "Foreign Fed Tax",	null, false, true,  false, false, false), // removed by LS-1574
	GAS					( "Gas",				null, false, true,  true,  false, false, null),
	HEALTH_INSURANCE	( "Health Insur. ded.",	null, false, true,  true,  false, false, null),
	HIATUS_PAID			( "Hiatus, Paid", 		null,  true, false, false, false, false, null),
	HOLIDAY_PAID		( "Holiday Paid",		null,  true, false, false, false, false, null), // for non-union
	HOLIDAY_WKD			( "Holiday Worked",		null,  true, false, false, false, false, null), // for union
	HOLIDAY_NOT_WKD		( "Holiday Not Worked",	null,  true, false, false, false, false, null), // for union
	IDLE_PAY			( "Idle",				null,  true, false, false, false, false, null),
	INSURANCE			( "Insurance deduction",null, false, true,  true,  false, false, "INS"),
	LODGING_NONTAX		( "Lodging - NonTax",	null, false, true,  true,  false, false, null),
	LODGING_TAX			( "Lodging - Txbl",		null, false, true,  true,  false, true, null),
	LODGING_ADVANCE		( "Lodging Advance",	null, false, true,  true,  false, false, null),
	MEAL_ALLOWANCE		( "Meal Allowance",		null, false, true,  true,  false, false, null),
	MEAL_MONEY			( "Meal Money",			null, false, true,  true,  false, false, null),
	MM_PER_DIEM_ADVANCE	( "MM Per Diem Advance",null, false, true,  true,  false, false, null),
	MEAL_PENALTY		( "Meal Penalty",		null,  true, false, false, true,  false, null),
	MEET_GUARANTEE		( "Meet Guarantee",		null,  true, false, false, false, false, null),
	MILEAGE_NONTAX		( "Mileage - NonTax",	null, false, true,  false, false, false, "MIL"),
	MILEAGE_TAX			( "Mileage - Txbl",		null, false, true,  false, false, true, "MLG"), // export code set for LS-4591
//	NET_ZERO_ADV		( "Net Zero Advance",	null, false, true,  false, false, false), // removed by LS-1574
	NIGHT_PREM_10_1X	( "NP 10% on 1X Hrs",	1.0f,  true, false, false, true,  false, null),
	NIGHT_PREM_10_15X	( "NP 10% on 1.5X Hrs",	1.5f,  true, false, false, true,  false, null),
	NIGHT_PREM_20_1X	( "NP 20% on 1X Hrs",	1.0f,  true, false, false, true,  false, null),
	NIGHT_PREM_20_15X	( "NP 20% on 1.5X Hrs",	1.5f,  true, false, false, true,  false, null),
	OVERNIGHT			( "Overnight",			null,  true, false, false, false, false, null),
	PARKING_NONTAX		( "Parking - NonTax",	null, false, true,  true,  false, false, null),
	PARKING_TAX			( "Parking - Txbl",		null, false, true,  true,  false, true, null),
	PER_DIEM_NONTAX		( "Per Diem - NonTax",	null, false, true,  true,  false, false, "PER"),
	PER_DIEM_TAX		( "Per Diem - Txbl",	null, false, true,  true,  false, true, "TPD"),
	PER_DIEM_ADVANCE	( "Per Diem Advance",	null, false, true,  true,  false, false, null),
	PERSNL				( "Personal Time Off",	null, false, false, false, false, false, null),
	PRE_TAX_401K		( "Pre-Tax 401K",		null, false, true,  false, false, false, "401K_R"),
	PRE_TAX_INS3		( "Pre-Tax Insur (ded from inv)",null,false,true,  false, false, false, "Pre_Tx_Ins_ded"),
	PRE_TAX_INS2		( "Pre-Tax Insurance",	null, false, true,  false, false, false, "Pre_Tx_Ins"),
	PRODUCTION_FEE		( "Production Fee",		null, false, false, false, false, false, null),
	REIMB				( "Reimbursement - NonTax",null,false,true, true,  false, false, "REM"),
	REBTX				( "Reimbursement - Txbl",null,false, true,  true,  false, false, "REBTX"),	// added in LS-4591
	REST_INV			( "Rest Invasion",		null,  true, false, false, false, false, "REST"),	// added in LS-2241
	REUSE				( "Re-Use Fee",			null, false, false, false, false, false, null),		// added in LS-2142
	ROTH				( "Roth IRA",			null, false, true,  true,  false, false, "ROTH"),	// LS-1574; LS-2579
	SAFETY				( "Safety Training",	null,  true, false, false, false, false, null),
	SAL_ADVANCE_NONTAX	( "Sal Advance - NonTax",null,false, true,  true,  false, false, "ADI"),
	SAL_ADVANCE_TAX		( "Sal Advance - Txbl",	null, false, true,  true,  false, true, null),
	SCRIPT_PREP			( "Script - Prep",		null, false, false, false, false, false, null),
	SCRIPT_TIMING		( "Script - Timing",	null, false, false, false, false, false, null),
	SICK_PAY			( "Sick Pay",			null,  true, false, false, false, false, null),
	TRAVEL				( "Travel",				null, false, false, false, false, false, null),
	//TURN_AROUND			( "Turn Around",		null, false, false, false, false, false), // deleted r6219 5/20/2016
	VACATION			( "Vacation",			null, false, false, false, false, false, null),
	WEEKLY_OT			( "Weekly OT",			null,  true, false, false, false, false, null),
	WRAP_PAID			( "Wrap", 				null,  true, false, false, false, false, null),
	// Added to cover typical contract rates:
	X165_HOURS			( "1.65x Hours",		1.65f, true, false, false, false, false, null),
	X175_HOURS			( "1.75x Hours",		1.75f, true, false, false, false, false, null),
	X22_HOURS			( "2.2x Hours",			2.2f,  true, false, false, false, false, null),
	X233_HOURS			( "2.33x Hours",		2.33333f,true,false,false, false, false, null),
	X257_HOURS			( "2.57x Hours",		2.571f,true, false, false, false, false, null),
	X28_HOURS			( "2.8x Hours",			2.8f,  true, false, false, false, false, null),
	X45_HOURS			( "4.5x Hours",			4.5f,  true, false, false, false, false, null),
	HOLIDAY_WKD_10X		( "Holiday Worked@1.0x",1.0f,  true, false, false, false, false, null),
	HOLIDAY_WKD_15X		( "Holiday Worked@1.5x",1.5f,  true, false, false, false, false, null),
	HOLIDAY_WKD_175X	( "Holiday Worked@1.75x",1.75f,true, false, false, false, false, null),
	HOLIDAY_WKD_20X		( "Holiday Worked@2.0x",2.0f,  true, false, false, false, false, null),
	HOLIDAY_WKD_30X		( "Holiday Worked@3.0x",3.0f,  true, false, false, false, false, null),
	HOLIDAY_WKD_33X		( "Holiday Worked@3.3x",3.3f,  true, false, false, false, false, null),
	HOLIDAY_WKD_35X		( "Holiday Worked@3.5x",3.5f,  true, false, false, false, false, null),
	HOLIDAY_WKD_40X		( "Holiday Worked@4,0x",4.0f,  true, false, false, false, false, null),
	HOLIDAY_WKD_44X		( "Holiday Worked@4.4x",4.4f,  true, false, false, false, false, null),
	HOLIDAY_WKD_467X	( "Holiday Worked@4.67x",4.67f,true, false, false, false, false, null),
	HOLIDAY_WKD_50X		( "Holiday Worked@5.0x",5.0f,  true, false, false, false, false, null),
	;

	private String abbreviation;

	/** The text to display in the drop-down list of choices. */
	private final String heading;

	/** The value (if not null) to be put into the Multiplier field on the line
	 * item with this category.  Used primarily for labor items (e.g., 2x hours). */
	private BigDecimal multiplier;

	/** If true, this entry will be available to the user in the Expense/Reimbursement
	 * table drop-down for accountants (on Full Timecard page). */
	private boolean isExpense;

	/** If true, this entry will be available to employees in the Expense/Reimbursement
	 * table drop-down on the Basic Timecard and Mobile timecard pages.  There are some
	 * expense categories, such as Mileage and Box rental, that we don't want employees
	 * to use. */
	private boolean isEmplExpense;

	/** If true, this entry represents a labor cost/payment. */
	private boolean isLabor;

	/** True iff HTG should include this amount in FLSA calculations. */
	private boolean isFlsa;

	/** True iff this line item is considered taxable. */
	private boolean isTaxable;

	private PayCategory(String head, Float mult, boolean isLabr, boolean isExp, boolean isEmplExp, boolean flsa, boolean tax, String abb) {
		heading = head;
		isLabor = isLabr;
		isExpense = isExp;
		isEmplExpense = isEmplExp;
		isFlsa = flsa;
		isTaxable = tax;
		abbreviation = abb;
		if (mult != null) {
			multiplier = new BigDecimal(mult);
			multiplier = NumberUtils.scaleTo2Places(multiplier);
		}
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

	/** See {@link #multiplier}. */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/** See {@link #isLabor}. */
	public boolean getIsLabor() {
		return isLabor;
	}

	/**See {@link #isExpense}. */
	public boolean getIsExpense() {
		return isExpense;
	}

	/** See {@link #isEmplExpense}. */
	public boolean getIsEmplExpense() {
		return isEmplExpense;
	}

	/**See {@link #isFlsa}. */
	public boolean getIsFlsa() {
		return isFlsa;
	}

	/** See {@link #isTaxable}. */
	public boolean getIsTaxable() {
		return isTaxable;
	}

	/** See {@link #abbreviation}. */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * @return True if this category is included in the basis for
	 * calculating agency commission. LS-2142
	 */
	public boolean paysCommission() {
		return (getIsLabor() && !(this == MEAL_PENALTY)) ||
				this == REUSE;
	}

	/**
	 * @return True if this line item is included in the breakdown total. LS-2142
	 */
	public boolean includedInTotal() {
		return ! (this == AGENT_FEE_NONTAX);
	}

	/**
	 * @return True if this category allows negative values for multiplier, rate
	 *         or quantity. LS-3961
	 */
	public boolean getAllowsNegative() {
		return this == SAL_ADVANCE_NONTAX ||
				this == SAL_ADVANCE_TAX ||
				this == LODGING_ADVANCE ||
				this == PER_DIEM_ADVANCE;
	}

	/**
	 * @return True if this category should be included in the
	 * Reimbursements list for Model Release timecards.
	 */
	public boolean getForModelRelease() {
		return this == PER_DIEM_NONTAX ||
				this == PER_DIEM_TAX ||
				this == MILEAGE_NONTAX ||
				this == MILEAGE_TAX ||
				this == REIMB ||
				this == REBTX;
	}

	/**
	 * This overrides some existing AS400 pay codes for reimbursement categories
	 * when used on a Model-Release timecard.  LS-4664
	 *
	 * @return The appropriate Pay Code when applied to a Model Release
	 *         timecard.
	 */
	public String getModelReleaseCode() {
		if (this == REIMB) {
			return "REB";
		}
		else if (this == PER_DIEM_TAX) {
			return "PDT";
		}
		else if (this == MILEAGE_NONTAX) {
			return "ML1";
		}
		// All other categories use the "standard" pay codes
		return getAbbreviation();
	}

	/**
	 * Finds the Enum value that corresponds to the given label text. That is,
	 * it checks the "getLabel()" value of each Enum and compares it to the
	 * supplied text, looking for a match. This is different than the builtin
	 * valueOf() method, which compares against each Enum's name.
	 *
	 * @param str The text to match against the Enum label.
	 * @return The matching entry, or CUSTOM if no match is found.
	 */
	public static PayCategory toValue(String str) {
		PayCategory cat = CUSTOM;
		for (PayCategory pc : values()) {
			if (pc.getLabel().equalsIgnoreCase(str)) {
				cat = pc;
				break;
			}
		}
		return cat;
	}

}
