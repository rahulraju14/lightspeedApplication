package com.lightspeedeps.type;

/**
 * Types of cast contracts.
 */
public enum CastContractType {
	NON_UNION("Non-Union"),
	A1_PRINCIPAL("A-1 Principal"),
	A2_EXTRA("A-2 Extra"),
	;

	private String label;

	CastContractType(String label) {
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
