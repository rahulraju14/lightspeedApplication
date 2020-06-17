package com.lightspeedeps.type;

/**
 * Enum for the different type of pdf printing groupings for Onboarding documents.
 */
public enum PdfGroupingType {
	NONE("None"),
	DOC_TYPE("Document Type"),
	EMPLOYEE("Employee");

	private String label;

	PdfGroupingType(String label) {
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
