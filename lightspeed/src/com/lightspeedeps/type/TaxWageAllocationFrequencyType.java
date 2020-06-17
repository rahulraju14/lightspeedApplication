/**
 * File: TaxWageAllocationFrequencyType.java
 */
package com.lightspeedeps.type;

/**
 * The types of tax wage allocation frequencies.
 */
public enum TaxWageAllocationFrequencyType {
	ANNUAL("Annual"),
	QUARTERLY("Quarterly"),
	MONTHLY("Monthly"),
	SEMI_MONTHLY("Semi-Monthly"),
	BI_WEEKLY("Bi-Weekly"),
	WEEKLY("Weekly")
	;
	
	private String label;
	
	TaxWageAllocationFrequencyType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String toString() {
		return getLabel();
	}
}
