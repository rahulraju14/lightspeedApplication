package com.lightspeedeps.util.common;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.Constants;

/**
 * A class to hold utility methods for Strings, including Enum-related
 * string conversions.
 *
 */
public class StringUtils {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StringUtils.class);

	/** A regular expression listing as individual possible characters (e.g., like "a|b"), all the
	 * single-byte characters that are invalid in Windows or Linux filenames. All the characters
	 * are escaped, even ones that wouldn't have to be for a regular expression, just cause it's
	 * easier to read that way!  The characters included are <>/\|:"?,*
	 * Note that comma is valid in filenames, but causes problems with our code because in some
	 * cases we generate lists of comma-separated filenames, and then parse them back out. */
	private static final String INVALID_FILENAME_CHARACTERS = "\\<|\\>|\\/|\\\\|\\||\\:|\\\"|\\?|\\,|\\*";

	private StringUtils() {
	}

	/**
	 * Convert a string to its corresponding Enum type value. In this case,
	 * the string should match one of the values from the "headings" array, which
	 * are (by LS conventions) the values returned by the toString() method.
	 * Therefore, for any enumerated value ee of the type 'enumtype',
	 * the following is true:
	 * <p>
	 * 		ee == EnumUtil.toValue( ee.toString(), headings, {enumtype.class} )
	 * <p>
	 * In a sense, toValue() is the inverse of toString(), in the same
	 * way that the (Java-defined) method valueOf() is the inverse of name().
	 * <p>
	 * The use of toValue() is ONLY necessary if an Enum class overrides toString(),
	 * and other code needs to convert this string value back into an enumeration value.
	 * <p>
	 * See {@link com.lightspeedeps.type.DayNightType} for an example of how this is used.
	 *
	 * @param s The string representing an enumerated value.
	 * @return The equivalent enumerated type value.
	 */
	public static <T extends Enum<T>> T toValue(String s, String[] headings, Class<T> c) {
		// There's no way, using generics, to call the "values()" method, so we
		// have this work-around using getEnumConstants().
		T[] enumList = c.getEnumConstants();
		T retValue = null; //enumList[0];
		for (int ix = 0; ix < headings.length; ix++) {
			if (s.equalsIgnoreCase(headings[ix])) {
				retValue = enumList[ix];
				break;
			}
		}
		return retValue;
	}

	/**
	 * Creates a single String from a List of Objects, by concatenating the results of
	 * each Object's toString(), together with ", " (comma and space) between each.  There is
	 * no trailing punctuation.  Returns an empty String (not null) if
	 * the List is empty or null.  Note that if an item within the List is
	 * null, it will appear in the output String as the text 'null'.
	 * @param list the list of Objects whose string value is to be concatenated.
	 * @return The concatenated list (never null).
	 */
	public static String getStringFromList(Collection<? extends Object> list) {
		return StringUtils.getStringFromList(list, ", ");
	}

	/**
	 * Creates a single String from a List of Objects, by concatenating the results of
	 * each Object's toString(), together with the given delimiter between each.  There is
	 * no trailing punctuation (delimiter).  Returns an empty String (not null) if
	 * the List is empty or null.  Note that if an item within the List is
	 * null, it will appear in the output String as the text 'null'.
	 * @param list the list of Objects whose string value is to be concatenated.
	 * @param delimiter the text to be placed following each value except the last.
	 * @return The concatenated list (never null).
	 */
	public static String getStringFromList(Collection<? extends Object> list, String delimiter) {
		if (list == null) {
			return "";
		}
		StringBuffer result = new StringBuffer(100);
		for (Object s : list) {
			if (s != null) { // skip null entries; rev 4063.
				result.append(s.toString()).append(delimiter);
			}
		}
		if (result.length() > delimiter.length()) {
			return result.substring(0, result.length() - delimiter.length());
		}
		return "";
	}

	protected static final int WIDTH_DEFAULT_LOWER_CHAR = 7;
	protected static final int WIDTH_DEFAULT_UPPER_CHAR = 9;
	protected static final int WIDTH_SKINNY_CHAR = 2;

	/**
	 * Shorten the supplied string, if necessary, so that the width of the text
	 * when displayed in a "typical" proportional font, will not exceed the
	 * width (in approximate pixel units) given.  The method used is a very rough
	 * approximation, taking into account only a few very narrow characters (such as 'i')
	 * and a few very wide characters (such as 'w') and assuming the rest are an
	 * average width.
	 *
	 * @param text The String of text to be truncated, if necessary.
	 * @param width The maximum width the text should have, in pixels, in a 10px
	 *            font.
	 * @return A string which should be less than the specified width when
	 *         displayed. If the text has been shortened at all, then an
	 *         ellipsis (...) will have been added to the end of the string.
	 */
	public static String trimToWidth(String text, int width) {
		if (text == null) {
			return null;
		}
		//width -= 4; // FUDGE FACTOR
		int i=0;
		char c;
		for ( ; i < text.length() && width >= WIDTH_DEFAULT_UPPER_CHAR; i++) {
			c = text.charAt(i);
			if (" \"'.,Iit/".indexOf(c) >= 0) {
				width -= WIDTH_SKINNY_CHAR;
			}
			else if (Character.isUpperCase(c) || ("wm".indexOf(c) >= 0)) {
				width -= WIDTH_DEFAULT_UPPER_CHAR;
			}
			else {
				width -= WIDTH_DEFAULT_LOWER_CHAR;
			}
		}
		//log.debug("str=" + text + ", w=" + width + ", i=" + i);
		if (i < text.length()) {
			text = text.substring(0, i-1).trim() + "...";
		}
		return text;
	}

	/**
	 * Shorten the supplied text, if necessary, so that the number of characters
	 * is less than or equal to the given length. Truncating the string at a
	 * space is given some preference, if it does not require removing more than
	 * 6 extra characters. If any truncation is done to the original string, an
	 * ellipsis (...) is added to the end. The 3 characters of the ellipsis must
	 * also fit within the specified length.
	 *
	 * @param text The String of text to be truncated, if necessary. If this is
	 *            null, then null is returned.
	 * @param length The maximum length of the returned String, in characters.
	 * @return A String which is no more than 'length' characters long. Returns
	 *         null iff the 'text' parameter is null.
	 */
	public static String trimToWord(String text, int length) {
		if (text == null) {
			return null;
		}
		if (text.length() > length) {
			text = text.substring(0, length - 3);
			int i = text.lastIndexOf(' ');
			if (i > text.length() - 7) { // arbitrary ... lose up to 6 chars to get word boundary
				text = text.substring(0, i+1);
			}
			text += "...";
		}
		return text;
	}

	/**
	 * @return True if the given String is null, or has zero length,
	 * or contains only whitespace characters.
	 */
	public static boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	/**
	 * Compares two String objects and returns the standard -1, 0, 1 values for
	 * comparison. Handles null parameters. A null value is considered NOT
	 * equal to an empty String.
	 *
	 * @param s1 The first String (or null) to be compared.
	 * @param s2 The second String (or null) to be compared.
	 * @return <li>0 if s1 and s2 are both null, or are equal String's. <li>1 if
	 *         s1 is lexically greater than s2, or if s1 is not null and s2 is
	 *         null. <li>-1 if s1 is lexically less than s2, or if s1 is null
	 *         and s2 is not null.
	 */
	public static int compare(String s1, String s2) {
		int ret;
		if (s1 == null) {
			if (s2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (s2 == null) {
			ret = 1;
		}
		else {
			ret = s1.compareTo(s2);
		}
		return ret;
	}

	/**
	 * Compares two String objects and returns the standard -1, 0, 1 values for
	 * comparison. Handles null parameters, AND a null String will be considered
	 * equal to an empty String.
	 *
	 * @param s1 The first String (or null) to be compared.
	 * @param s2 The second String (or null) to be compared.
	 * @return
	 * 		<li>0 if s1 and s2 are both null, or are equal String's, or one
	 *         is null and the other empty.
	 *         <li>1 if s1 is lexically greater than s2, or if s1 is not null
	 *         and not empty and s2 is null.
	 *         <li>-1 if s1 is lexically less than s2, or if s1 is null and s2
	 *         is not null and not empty.
	 */
	public static int compareNullEmpty(String s1, String s2) {
		int ret;
		if (s1 == null) {
			if (s2 == null || s2.length() == 0) {
				ret = 0; // Empty and null treated as equal
			}
			else {
				ret = -1;
			}
		}
		else if (s2 == null) {
			if (s1.length() == 0) {
				ret = 0; // Empty and null treated as equal
			}
			else {
				ret = 1;
			}
		}
		else {
			ret = s1.compareTo(s2);
		}
		return ret;
	}

	/**
	 * Compares two String objects when they may contain numeric values, and
	 * returns the standard -1, 0, 1 values for comparison. Handles null
	 * parameters. Note that the string comparison used (when one or both values
	 * are non-numeric) is case-insensitive.
	 *
	 * @param s1 The first String (or null) to be compared.
	 * @param s2 The second String (or null) to be compared.
	 * @return <li>0 if s1 and s2 are both null, or are equal String's. <li>1 if
	 *         s1 is not null and s2 is null <li>-1 if s1 is null and s2 is not
	 *         null <li>if s1 and s2 can be converted to integers, then the
	 *         comparison of the two int's is returned, otherwise: <li>1 if s1
	 *         is lexically greater than s2 <li>-1 if s1 is lexically less than
	 *         s2
	 */
	public static int compareNumeric(String s1, String s2) {
		int ret;
		if (s1 == null) {
			if (s2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (s2 == null) {
			ret = 1;
		}
		else {
			ret = s1.compareToIgnoreCase(s2);
			if (ret != 0) {
				boolean b1 = false, b2 = false;
				int i1 = 0, i2 = 0;
				try {
					i1 = Integer.parseInt(s1);
					b1 = true;
				}
				catch (NumberFormatException e) {
				}
				try {
					i2 = Integer.parseInt(s2);
					b2 = true;
					if (b1) {
						ret = ( i1 == i2 ? 0 : i1 < i2 ? -1 : 1 );
					}
					else { // second field is numeric, first is not
						ret = 1; // numbers compare less than strings
					}
				}
				catch (NumberFormatException e) {
				}
				if (b1 && (! b2)) { // first field is numeric, second is not
					ret = -1; // numbers compare less than strings
				}
			}
		}
		return ret;
	}

	public static int compareIgnoreCase(String s1, String s2) {
		int ret;
		if (s1 == null) {
			if (s2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (s2 == null) {
			ret = 1;
		}
		else {
			ret = s1.compareToIgnoreCase(s2);
		}
		return ret;
	}

	public static boolean equals(byte[] s1, byte[] s2) {
		if (s1 == s2) {
			return true;
		}
		return (compare(s1,s2) == 0);
	}

	public static int compare(byte[] s1, byte[] s2) {
		int ret;
		if (s1 == null) {
			if (s2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (s2 == null) {
			ret = 1;
		}
		else {
			ret = 0;
			if (s1 != s2) {
				int len = Math.min(s1.length, s2.length);
				for (int i=0; i < len && ret == 0; i++) {
					ret = NumberUtils.compare(s1[i], s2[i]);
				}
			   	if (ret == 0) {
			   		ret = NumberUtils.compare(s1.length, s2.length);
				}
			}
		}
		return ret;
	}

	/**
	 * Finds the number of occurrences of 'ch' within the String 'text'.
	 *
	 * @param text The String to search, or null.
	 * @param ch The character to look for.
	 * @return The number of times 'ch' appears in 'text'. Always returns a
	 *         non-negative value.  Returns 0 if 'text' is null.
	 */
	public static int countOf(String text, char ch) {
		int n = 0;
		if (text != null && text.length() > 0) {
			int ix = text.indexOf(ch);
			while (ix >= 0) {
				n++;
				ix = text.indexOf(ch, ix+1);
			}
		}
		return n;
	}

	/** Match any string of 40 or more characters that does not contain a blank or hyphen. */
	private final static String LONG_WORD_RE = "(.*)([^ -]{40,})(.*)";

	private final static Pattern LONG_WORD = Pattern.compile(LONG_WORD_RE);

	/**
	 * Take the given string and add hyphens ('-') to break up long strings of
	 * characters that have no intervening blanks or hyphens. The current
	 * setting looks for strings of 40+ non-blank characters, and inserts a
	 * hyphen 20 characters from the end, then repeats the process until there
	 * are no 40+ characters strings without either a hyphen or a blank.
	 *
	 * @param str The input string to check & modify.
	 * @return The resulting string, which may be identical to the input string,
	 *         or may have hyphens added.
	 */
	public static String hyphenate(String str) {
		String sub;
		Matcher m = LONG_WORD.matcher(str);
		while (m.matches()) {
			sub = m.group(2).substring(0, 20) + '-' + m.group(2).substring(21);
			str = m.group(1) + sub + m.group(3);
			m = LONG_WORD.matcher(str);
			//log.debug(str);
		}
		return str;
	}

	/**
	 * This removes extra blanks from a String by replacing any occurrence of 3
	 * or more blanks with only two blanks.
	 *
	 * @param text The String to be de-blanked.
	 * @return A String with all strings of blanks reduced to a maximum of two
	 *         blanks. It may return the same String.
	 */
	public static String removeBlankPadding(String text) {
		//log.debug("L=" + text.length() + ", txt=" + text);
		int ix = 0;
		while ( (ix = text.indexOf("   ", ix)) >= 0) {
			text = text.substring(0, ix) + text.substring(ix+1);
		}
		//log.debug("L=" + text.length() + ", txt=" + text);
		return text;
	}

	/**
	 * Given a string, make sure that it is usable as a filename in Linux or
	 * Windows by replacing characters as necessary.
	 *
	 * @param name
	 * @return The input string, with invalid characters replaced by
	 *         underscores, and leading and trailing spaces removed.
	 */
	public static String cleanFilename(String name) {
		// replace all filename characters invalid in Windows or Linux with underscores;
		// also, Windows won't allow starting with a blank, and ending with blank is just a bad idea!
		name = name.replaceAll(INVALID_FILENAME_CHARACTERS, "_").trim();
		if (name.startsWith(".")) { // Windows won't allow starting with period
			name = "_" + name.substring(1);
		}
		if (name.length() == 0) {
			name = "_"; // return something instead of nothing
		}
		return name;
	}

	/**
	 * Convert HTML < br/ > tags to new-line characters. This is typically done
	 * when converting a field saved with HTML (for display) into text suitable
	 * for editing in a < textArea > tag.
	 *
	 * @param text The String to be converted; may be null.
	 * @return A String with all occurrences of < br /> replaced by new-line
	 *         symbols, or null if 'text' is null.
	 */
	public static String editHtml(String text) {
		if (text != null) {
			text = text.replaceAll(Constants.HTML_BREAK, Constants.NEW_LINE);
		}
		return text;
	}

	/**
	 * Convert new-line characters to HTML < br/> tags. This is typically done
	 * when saving a field that has been edited in a < textArea> tag, where
	 * hitting Return creates new-line symbols in the data. To display the field
	 * properly (e.g., in <outputText> or a jasper report), we want to save it
	 * with < br/> tags instead.
	 *
	 * @param text The String to be converted.
	 * @return A String with all occurrences of new-line symbols replaced by <br/>
	 *         tags.
	 */
	public static String saveHtml(String text) {
		if (text != null) {
			// replace cr/lf pairs first
			text = text.replaceAll("\\r\\n", Constants.HTML_BREAK);
			// then any remaining standalone lf's
			text = text.replaceAll("\\n", Constants.HTML_BREAK);
		}
		return text;
	}

	/**
	 * Convert the HTML codes used by our rich editor into those favored by
	 * Jasper reports.
	 *
	 * @param text The text to be scanned for replacements.
	 * @return The updated text.
	 */
	public static String convertBoldItalic(String text) {
		if (text != null) {
			text = text.replaceAll("<strong>", "<b>");
			text = text.replaceAll("</strong>", "</b>");
			text = text.replaceAll("<em>", "<i>");
			text = text.replaceAll("</em>", "</i>");
		}
		return text;
	}

	/**
	 * Changes the bytes in the given String into printable Hex for debugging,
	 * etc.
	 *
	 * @param str The String to be changed into printable hex.
	 * @return A String which is twice as long as 'str', and which, when
	 *         printed, displays the hex value of each of the bytes in 'str'.
	 *         Note that this routine changes 'str' into a series of bytes using
	 *         String.getBytes(), which will use the platform's default
	 *         character set.
	 */
	public static String toHexString(String str) {
		byte[] bytes = str.getBytes();
		StringBuilder out = new StringBuilder();
		for (byte b: bytes) {
			out.append(String.format("%02X", b));
		}
		return out.toString();
	}

	/**
	 * Changes the bytes in the given byte[] into printable Hex for debugging,
	 * etc.
	 *
	 * @param bytes The byte array to be changed into printable hex.
	 * @return A String which is twice as long as 'bytes', and which, when
	 *         printed, displays the hex value of each of the bytes in 'bytes'.
	 */
	public static String toHexString(byte[] bytes) {
		StringBuilder out = new StringBuilder();
		for (byte b: bytes) {
			out.append(String.format("%02X", b));
		}
		return out.toString();
	}

	/** Convert plain numeric string to pretty display format with dashes.
	 * @param number The String to be output as an Id number string.
	 * @return Formatted String number, if it is not null, otherwise
	 *         returns null.
	 */
	public static String formatPhoneNumber(String number) {
		//String idstring = validateOutput(arg2);
		if (number != null) {
			if (number.length() == 0 || number.charAt(0) == '+' || number.charAt(0) == '0') {
				// return it as-is, no reformatting
			}
			else if (number.length() == 10) {
				number = "(" + number.substring(0,3) + ") " + number.substring(3, 6) + "-" + number.substring(6);
			}
			else if (number.length() == 11 && number.charAt(0) == '1') {
				number = "1-" + number.substring(1,4) + "-" + number.substring(4, 7) + "-" + number.substring(7);
			}
			return number;
		}
		else {
			return null;
		}
	}

	/** Pattern to match a "P.O. Box..." text, including common typos and misspellings.
	 * From http://stackoverflow.com/questions/28783154/java-regex-for-po-box-validation  by Ceekay 12/10/2015 */
	public static final Pattern US_PO_BOX_PATTERN =
			Pattern.compile("^box[^a-z]|([a-z]?p[-\\.\\s#>,\\?]*?(o|0|p)?o?l?[-\\.\\s#>,\\?]*?|(post|postal)\\s*(office)?)[\\s\\w]*?b(.|ox)(.*)?",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Determine if the given address string represents a PO Box (instead of a street
	 * address).
	 * @param addr The string to check.
	 * @return True if the string appears to be a PO Box reference.
	 */
	public static boolean isPoBox(String addr) {
		if (addr == null) {
			return false;
		}
		Matcher m = US_PO_BOX_PATTERN.matcher(addr);
		return (m.matches());
	}

	/**
	 * Check a string for being a valid tax identification number -- either a
	 * U.S. Social Security number, or a U.S. Federal Tax Id Number.
	 *
	 * @param taxId The string that may be a tax id.
	 * @return The given tax id with punctuation (-, |) and spaces removed. If
	 *         null is returned, the given taxId was either null, empty, or only
	 *         contained punctuation and spaces. If an empty string ("") is
	 *         returned, then the taxId field was not valid; that is, it (a)
	 *         contained non-numeric characters other than punctuation and
	 *         spaces, or (b) was not 9 numeric digits in length.
	 */
	public static String cleanTaxId(String taxId) {
		if (taxId == null) {
			return null;
		}
		String idstring = taxId.replaceAll("-| ", "");
		if (idstring.length() == 0) {
			return null;
		}
		int idNumber = -1;
		if (idstring.length() == 9) {
			try {
				idNumber = Integer.parseInt(idstring);
			}
			catch (NumberFormatException e) {
			}
		}
		if (idNumber == -1) {
			idstring = "";
		}
		return idstring;
	}

}
