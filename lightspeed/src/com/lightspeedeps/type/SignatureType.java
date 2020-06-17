package com.lightspeedeps.type;

public enum SignatureType {
	EMPLOYEE,
	APPROVER,
	ALL
	;

	/** Provides a String that can be used in JSP for ace:dataTable
	 * operations such as sorting and filtering. */
	public String getLabel() {
		return name();
	}
}
