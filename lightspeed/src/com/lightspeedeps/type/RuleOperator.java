/*	File Name:	RuleOperator.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the RuleTerm object to define the operation
 * to be performed between a specified field and a supplied literal value.
 */
public enum RuleOperator {
	/** Equals operation */
	EQ		("=", "equals"),

	/** Not-Equals operation */
	NE		("!=", "is not equal to"),

	/** Less-Than operation */
	LT		("<", "is less than"),

	/** Less-Than-or-Equal operation */
	LE		("<=", "is less than or equal to"),

	/** Greater-Than operation */
	GT		(">", "is greater than"),

	/** Greater-Than-or-Equal operation */
	GE		(">=", "is greater than or equal to"),

	/** 'Contains' operation, meant for String comparisons. */
	CO		("<>", "contains"),

	/** 'Member of' ('in') operation, meant for lists of Strings */
	ME		(":", "is a member of"),

	/** 'Not Member of' ('not in') operation, meant for lists of Strings */
	NME		("!:", "is not a member of"),

	/** Null or "is empty" operation, to test when a field has no value. */
	NL		("!!", "is empty."),
	;

	private final String heading;
	private String operator;

	RuleOperator(String oper, String head) {
		heading = head;
		operator = oper;
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

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/**See {@link #operator}. */
	public String getOperator() {
		return operator;
	}

	/**
	 * Finds the Enum value that corresponds to the given label text. That is,
	 * it checks the "getOperator()" value of each Enum and compares it to the
	 * supplied text, looking for a match. This is different than the built-in
	 * valueOf() method, which compares against each Enum's name.
	 *
	 * @param str The text to match against the Enum operator string.
	 * @return The matching entry, or null if no match is found.
	 */
	public static RuleOperator toValue(String str) {
		RuleOperator oper = null;
		for (RuleOperator ro : values()) {
			if (ro.getOperator().equals(str)) {
				oper = ro;
				break;
			}
		}
		return oper;
	}

}
