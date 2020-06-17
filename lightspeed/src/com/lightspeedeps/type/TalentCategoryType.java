package com.lightspeedeps.type;

public enum TalentCategoryType {
	
	NONE			("None")
	;
	
	private final String heading;
	
	TalentCategoryType(String heading) {
		this.heading = heading;
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

	/** Provides a String that can be used in JSP for ace:dataTable
	 * operations such as sorting and filtering. */
	public String getLabel() {
		return name();
	}


}
