/**
 * File: PaidAsType.java
 */
package com.lightspeedeps.type;

/**
 *
 */
public enum PaidAsType {
	I("Individual"),
	LO("Loan-out")
	;

	private String label;

	PaidAsType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean isIndividual() {
		return this == PaidAsType.I;
	}

	public boolean isLoanOut() {
		return this == PaidAsType.LO;
	}
}
