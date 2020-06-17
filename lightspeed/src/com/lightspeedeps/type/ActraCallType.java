package com.lightspeedeps.type;

public enum ActraCallType {

	WARDROBE_CALL	("Wardrobe Call", true, 1),
	REHEARSAL		("Rehearsal", true, 2),
	HOLDING_CALL	("Holding Call", true, 3),
	OTHER			("Other", false, 4)
	;
	
	private final String heading;
	
	private final boolean show;
	
	private final int index;
	
	ActraCallType(String heading, boolean show, int index) {
		this.heading = heading;
		this.show = show;
		this.index = index;
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

	public boolean getShow() {
		return show;
	}

	public int getIndex() {
		return index;
	}
	
	public static ActraCallType getTypeByIndex(int index) {
		for (ActraCallType type : values()) {
			if (index == type.getIndex()) {
				return type;
			}
		}
		return null;
	}
}
