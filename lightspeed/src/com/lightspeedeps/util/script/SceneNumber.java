/**
 * File: SceneNumber.java
 */
package com.lightspeedeps.util.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class to provide parsing a scene-number string into its component
 * numeric and alphabetic parts.
 */
public class SceneNumber implements Comparable<SceneNumber> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SceneNumber.class);

	private static final Pattern SCENE_NUMBER_PATTERN = Pattern.compile("([a-zA-Z ]*)([0-9]*)([a-zA-Z0-9 ]*)");

	/** The entire (alphanumeric) scene number. */
	private String scene;

	/** The scene number string excluding the part that is considered the numeric
	 * part.  This will concatenate any prefix and suffix portions.  It may be empty,
	 * but not null. */
	private String alpha;

	/** The numeric portion of the scene number, which is either -1 or
	 * non-negative. If the scene string does not contain any digits, or does
	 * not parse as a valid scene number string, this value will be -1. If the
	 * string contains one or more groups of digits, this field will be the
	 * value of the first (contiguous) string of digits. */
	private int number = -1;


	public SceneNumber() {
	}

	public SceneNumber(String sceneNumber) {
		scene = sceneNumber;
	}

	/**
	 * Determine if the supplied string is a valid "scene number" format. It must consist of a
	 * numeric part, with optional leading and/or trailing alphabetic parts. No spaces or symbols
	 * are allowed. Valid examples are 25, D34, and 187AB. Invalid examples are BC, 1A2, and -12.
	 *
	 * @param sceneNumber The scene number string to be validated.
	 * @return True if the string is in a valid format, false otherwise.
	 */
	public static boolean isValidFormat(String sceneNumber) {
		if (sceneNumber.length() > 6) {
			return false;
		}
		Matcher match = SCENE_NUMBER_PATTERN.matcher(sceneNumber);
		return match.matches();
	}

	/**
	 * Breaks the alphanumeric scene number string into separate alphabetic
	 * and numeric parts.
	 */
	private void parse() {
		number = new Integer(-1);
		String prefix = "", suffix = "";
		if (scene != null && scene.length() > 0) {
			Matcher match = SCENE_NUMBER_PATTERN.matcher(scene);
			boolean bMatch = match.matches();
			String numberToConvert = scene;
			if (bMatch) {
				prefix = match.group(1).trim();
				numberToConvert = match.group(2).trim();
				suffix = match.group(3).trim();
			}
			if (numberToConvert.length() > 0) {
				try {
					int n = Integer.parseInt(numberToConvert);
					number = new Integer(n);
				}
				catch(Exception e) {}
			}
		}
		alpha = prefix + suffix;
	}

	/** See {@link #scene}. */
	public String getScene() {
		return scene;
	}
	/** See {@link #scene}. */
	public void setScene(String scene) {
		alpha = null; // force parsing if components are needed.
		this.scene = scene;
	}

	/** Returns the entire (alphanumeric) scene number. */
	@Override
	public String toString() {
		return scene;
	}

	/** See {@link #number}. */
	public int getNumber() {
		if (alpha == null) {
			parse();
		}
		return number;
	}
	/** See {@link #number}. */
	public void setNumber(int number) {
		this.number = number;
	}

	/** See {@link #alpha}. */
	public String getAlpha() {
		if (alpha == null) {
			parse();
		}
		return alpha;
	}
	/** See {@link #alpha}. */
	public void setAlpha(String alpha) {
		this.alpha = alpha;
	}

	/**
	 * Compares two scene numbers that may contain alphabetic prefixes or suffixes, and returns the
	 * standard -1/0/1 comparison value.  Any alphabetic portions are assumed to be upper case.
	 *
	 * @param s1 The first scene number string.
	 * @param s2 The second scene number string.
	 * @return If the strings are identical, returns 0. If both strings are purely numeric, then
	 *         returns the numeric comparison. Otherwise, the numeric portion of each string is
	 *         first compared, and, if not equal, that comparison value (1 or -1) is returned. If
	 *         the numeric portions are equal, then a string comparison is done of the alphabetic
	 *         portions. No distinction is made between prefixes and suffixes.
	 */
	public static int compareSceneNumbers(String s1, String s2) {
		int ret = 0;
		if (s1.compareTo(s2) == 0) {
			return 0;
		}
		try {
			// a shortcut that should be faster for numeric scene numbers
			int i1 = Integer.parseInt(s1);
			int i2 = Integer.parseInt(s2);
			ret = ( i1 == i2 ? 0 : i1 < i2 ? -1 : 1 );
		}
		catch (NumberFormatException e) {
			ret = (new SceneNumber(s1)).compareTo(new SceneNumber(s2));
		}
		//log.debug("comparing `" + s1 + "` and `" + s2 + "`, ret=" + ret);
		return ret;
	}

	@Override
	public int compareTo(SceneNumber sn2) {
		int ret = ( getNumber() == sn2.getNumber() ? 0 : getNumber() < sn2.getNumber() ? -1 : 1 );
		if (ret == 0) {
			ret = getAlpha().compareToIgnoreCase(sn2.getAlpha());
		}
		return ret;
	}

	public static int compareSceneNumberLists(String s1, String s2) {
		if (s1.compareTo(s2) == 0) {
			return 0;
		}
		if (s1.indexOf(',') < 0 ) {
			if (s2.indexOf(',') < 0) {
				return compareSceneNumbers(s1, s2);
			}
			String[] s2parts = s2.split(",");
			return compareSceneNumbers(s1,s2parts[0]);
		}
		String[] s1parts = s1.split(",");
		if (s2.indexOf(',') < 0) {
			return compareSceneNumbers(s1parts[0], s2);
		}
		String[] s2parts = s2.split(",");
		return compareSceneNumbers(s1parts[0], s2parts[0]);
	}

}
