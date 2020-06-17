//	File Name:	ExpenseType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in WeeklyTimecard (timecard system) for the
 * expense/reimbursement table entries.
 */
public enum ExpenseType {
	// The order of the values determines the order in the drop-down lists,
	// for example, on the Breakdown Edit page.
	/** Expense type of Box Rental, non-taxable */
	BX	("Box", null, null, PayCategory.BOX_RENT_NONTAX), // rev 4217 - make box rental non-taxable by default
	/** Expense type of Per Diem, taxable */
	PD	("Per Diem", PayCategory.PER_DIEM_TAX, PayCategory.PER_DIEM_NONTAX, null),
	/** Expense type of Lodging, either taxable or non-taxable */
	LD	("Lodging", PayCategory.LODGING_TAX, PayCategory.LODGING_NONTAX, null),
	/** Expense type of "allowable" Mileage, non-taxable */
	AM	("Allow Miles", null, PayCategory.MILEAGE_NONTAX, null),
	/** Expense type of Mileage, taxable */
	TM	("Tax. Miles", PayCategory.MILEAGE_TAX, null, null),
	/** Expense type of Car Allowance */
	CR	("Car Allow", null, null, PayCategory.CAR_ALLOWANCE),
	/** Expense type of Other, with Pay Category of "Adjustment" */
	OT	("Other", null, null, PayCategory.ADJUSTMENT),
	/** Expense type of Advance, with Pay Category of "Adjustment" */
	A1	("Advance", null, null, PayCategory.ADJUSTMENT),
	/** Expense type of Advance, with Pay Category of "Adjustment" */
	A2	("Advance", null, null, PayCategory.ADJUSTMENT),
	/** Expense type of N/A or unknown */
	NA ("Unknown"),
	;

	/** The 'pretty' text to use when displaying this value. */
	private String label;
	private PayCategory taxable;
	private PayCategory nonTaxable;
	private PayCategory total;


	ExpenseType(String lab) {
		label = lab;
	}

	ExpenseType(String lab, PayCategory tax, PayCategory nontax, PayCategory tot) {
		this(lab);
		taxable = tax;
		nonTaxable = nontax;
		total = tot;
	}

	/**
	 * Returns "pretty" version, instead of enum name.
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return label;
	}

	/**
	 * A method that allows JSP to get the 'name' of the enum
	 * as a String.
	 * @return The same as this.name().
	 */
	public String getName() {
		return name();
	}

	/** See {@link #label}. */
	public String getLabel() {
		return label;
	}

	/**See {@link #taxable}. */
	public PayCategory getTaxable() {
		return taxable;
	}

	/**See {@link #nonTaxable}. */
	public PayCategory getNonTaxable() {
		return nonTaxable;
	}

	/**See {@link #total}. */
	public PayCategory getTotal() {
		return total;
	}

}
