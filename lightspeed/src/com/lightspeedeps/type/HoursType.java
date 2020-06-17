/*	File Name:	HoursType.java */
package com.lightspeedeps.type;

/**
 * Enumeration used in the PayRule object to indicate how paid hours are
 * determined - elapsed or worked. This differentiation is normally only used
 * for calculating "golden" hours.
 */
public enum HoursType {
	/** Use Elapsed Hours */
	E		("Elapsed Hours"),
	/** Use Worked Hours */
	W		("Worked Hours"),
	/** Use Worked Hours for first break, Elapsed for second break */
	WE		("1=Worked Hours, 2=Elapsed Hours"),
	/** Use Worked Hours for first & second breaks, Elapsed for third break */
	WWE		("1/2=Worked Hours, 3=Elapsed Hours"),
	;

	private final String heading;

	HoursType(String head) {
		heading = head;
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

	/**
	 * @return True if the HoursType for all breaks is Elapsed hours.
	 */
	public boolean isAllElapsed() {
		boolean ret = false;
		switch(this) {
			case E:
				ret = true;
				break;
			case W:
			case WE:
			case WWE:
				ret = false;
				break;
		}
		return ret;
	}

	/**
	 * @return True if the HoursType for all breaks is Worked hours.
	 */
	public boolean isAllWorked() {
		boolean ret = false;
		switch(this) {
			case W:
				ret = true;
				break;
			case E:
			case WE:
			case WWE:
				ret = false;
				break;
		}
		return ret;
	}

	/**
	 * @return The HoursType (elapsed or worked) that applies to the first
	 *         period of hours (from the first break to the second break, if
	 *         any).
	 */
	public HoursType getBreak1() {
		HoursType breakType = W;
		switch(this) {
			case E:
				breakType = E;
				break;
			case W:
			case WE:
			case WWE:
				break;
		}
		return breakType;
	}

	/**
	 * @return The HoursType (elapsed or worked) that applies to the second
	 *         period of hours (from the second break to the third break, if
	 *         any).
	 */
	public HoursType getBreak2() {
		HoursType breakType = W;
		switch(this) {
			case E:
			case WE:
				breakType = E;
				break;
			case W:
			case WWE:
				break;
		}
		return breakType;
	}


	/**
	 * @return The HoursType (elapsed or worked) that applies to the third
	 *         period of hours (from the third break, if any, to the end of day.
	 */
	public HoursType getBreak3() {
		HoursType breakType = W;
		switch(this) {
			case E:
			case WE:
			case WWE:
				breakType = E;
				break;
			case W:
				break;
		}
		return breakType;
	}

}
