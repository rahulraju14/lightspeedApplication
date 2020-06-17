package com.lightspeedeps.web.util;

import com.lightspeedeps.util.app.SessionUtils;

/**
 * A class for saving and retrieving user preferences.
 * For now these are saved in the session, but they could be
 * migrated to the database in the future (e.g., saved at
 * Logout).
 */
public class UserPrefs {

	// These stripboard "preferences" are currently not saved or restored...
//	/** User's preferred value for Stripboard sort selection in unscheduled list. */
//	public final static String UPREF_STB_SORT = "SBST";
//	/** Default value for Stripboard sort selection in unscheduled list. */
//	public final static String UDEF_STB_SORT = Constants.SHEET_COLUMN_NAME;
//
//	/** User's preferred value for Stripboard list selection in right panel. */
//	public final static String UPREF_STB_SCHED = "SBSC";
//	/** Default value for Stripboard list selection in right panel. */
//	public final static String UDEF_STB_SCHED = StripBoardEditBean.STB_FILTER_UNSCHEDULED;

	public static void put(String prefName, Object prefValue) {
		SessionUtils.put(prefName, prefValue);
	}

	public static Object get(String prefName) {
		Object prefValue = SessionUtils.get(prefName);
		return prefValue;
	}

	public static String getString(String prefName) {
		String prefValue = (String)get(prefName);
		return prefValue;
	}

	public static String getString(String prefName, String defaultValue) {
		String prefValue = getString(prefName);
		if (prefValue == null) {
			prefValue = defaultValue;
		}
		return prefValue;
	}

	public static Integer getInteger(String prefName) {
		Integer prefValue = (Integer)get(prefName);
		return prefValue;
	}

	public static Integer getInteger(String prefName, Integer defaultValue) {
		Integer prefValue = getInteger(prefName);
		if (prefValue == null) {
			prefValue = defaultValue;
		}
		return prefValue;
	}

	public static Boolean getBoolean(String prefName) {
		Boolean prefValue = (Boolean)get(prefName);
		return prefValue;
	}

	public static Boolean getBoolean(String prefName, Boolean defaultValue) {
		Boolean prefValue = getBoolean(prefName);
		if (prefValue == null) {
			prefValue = defaultValue;
		}
		return prefValue;
	}

}
