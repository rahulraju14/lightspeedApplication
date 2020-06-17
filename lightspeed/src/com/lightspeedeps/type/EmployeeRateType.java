// File Name: EmployeeRateType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in StartForm to indicate how the employee's
 * pay is usually calculated - by hour, by day, or by week.
 */
public enum EmployeeRateType {
	HOURLY		("Hourly", "Hourly", "Hourly Rate"),
	DAILY		("Daily", "Daily / Exempt", "Daily Rate"),
	WEEKLY		("Weekly", "Weekly / Exempt", "Weekly Rate"),
	TABLE		("Table", "Table", "Table") // Rate will be pulled from Contract_Rate table
	;

	private final String heading;
	private final String longName;
	private final String description;
	
	private EmployeeRateType(String head, String longName, String description) {
		heading = head;
		this.longName = longName;
		this.description = description;
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

	/**
	 * LS-3825
	 * @return the long name printable version of this enumerated type. It can
	 * be used from jsp pages since it follows the bean accessor (getter) naming convention
	 */
	public String getLongName() {
		return longName;
	}
	
	/**
	 * LS-3825
	 * @return the description printable version of this enumerated type. It can
 	 * be used from jsp pages since it follows the bean accessor (getter) naming convention
	 */
	public String getDescription() {
		return description;
	}
}
