package com.lightspeedeps.type;

/**
 * Enumeration defining the "citizenship" categories defined on the US Federal I-9 form.
 */
public enum CitizenType {
	CITIZEN ("c"),
	NON_CITIZEN ("n"),
	PERM_RESIDENT ("p"),
	ALIEN ("a");

	/** The String used in radio-button lists (in html) to select
	 * the citizen type. */
	private String key;

	private CitizenType(String pkey) {
		key = pkey;
	}

	/** See {@link #key}. */
	public String getKey() {
		return key;
	}

	/**
	 * Finds the Enum value that corresponds to the given key text. That is, it
	 * checks the "getKey()" value of each Enum and compares it to the supplied
	 * text, looking for a match. This is different than the builtin valueOf()
	 * method, which compares against each Enum's name.
	 *
	 * @param key The text to match against the Enum label.
	 * @return The matching entry, or CITIZEN if no match is found.
	 */
	public static CitizenType fromKey(String key) {
		CitizenType citizen = CITIZEN;
		for (CitizenType cit : values()) {
			if (cit.getKey().equalsIgnoreCase(key)) {
				citizen = cit;
				break;
			}
		}
		return citizen;
	}

}
