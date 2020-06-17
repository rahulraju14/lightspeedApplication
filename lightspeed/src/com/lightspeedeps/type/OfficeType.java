/**
 * File: OfficeType.java
 */
package com.lightspeedeps.type;

/**
 * 
 */
public enum OfficeType {
	ACTRA("ACTRA Office"),
	UDA("UDA Office");
	
	private String label;
	
	OfficeType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}
