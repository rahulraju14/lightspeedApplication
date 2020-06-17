package com.lightspeedeps.type;

public enum TaxClassificationType {
	I		("Individual/sole proprietor or single-member LLC", 0),
	C		("C Corporation", 1),
	S		("S Corporation", 2),
	P		("Partnership", 3),
	T		("Trust/estate", 4),
	L		("Limited liability company", 5),
	O		("Other", 6);

	private final String heading;

	/** Index of the enum used by the radio buttons on the form. */
	private final int index;

	private TaxClassificationType(String head, int index) {
		heading = head;
		this.index = index;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

	/**See {@link #index}. */
	public int getIndex() {
		return index;
	}

	public boolean isLLC() {
		return this ==TaxClassificationType.L;
	}
}

