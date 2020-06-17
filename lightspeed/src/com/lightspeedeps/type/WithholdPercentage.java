package com.lightspeedeps.type;

public enum WithholdPercentage {
	Per_08		("0.8%", 0),
	Per_13		("1.3%", 1),
	Per_18		("1.8%", 2),
	Per_27		("2.7%", 3),
	Per_36		("3.6%", 4),
	Per_42		("4.2%", 5),
	Per_51		("5.1%", 6),
	Per_Extra	("Extra Amount", 7);

	private final String heading;
	
	/** Index of the enum used by the radio buttons on the form. */
	private final int index;

	private WithholdPercentage(String head, int index) {
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
	
}
