//	File Name:	NumberUtils.java
package com.lightspeedeps.util.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.HourRoundingType;
import com.lightspeedeps.util.app.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A class for numeric utility methods; some of these, related to token
 * creation and parsing, are used by the SMS messaging facility.
 */
public class NumberUtils {
	private static final Log log = LogFactory.getLog(NumberUtils.class);

//	private static NumberUtils instance = null;

	private final static String ALPHAMERIC = "0123456789$@#*-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private final static int BASE = ALPHAMERIC.length();
	private final static int TOKEN_LENGTH = 4;
	private final static String TOKEN_PAD = "0000";
	private final static int MAX_TOKEN_NUMBER = (int)Math.pow(BASE, TOKEN_LENGTH);
	private final static int TOKEN_NUMBER_LENGTH = String.valueOf(MAX_TOKEN_NUMBER).length();
	private final static String NUMBER_PAD = "00000000000";

	private static int MAX_INPUT_NUMBER = 0;
	private static int INPUT_NUMBER_LENGTH = 0;

	static {
		/**
		 * We need to compute MAX_INPUT_NUMBER as the largest decimal value that
		 * will work with our token-making process.  Since we add a check digit that
		 * may be 0-9 as the second digit in the final number, this is a bit complicated.
		 * We compute the "normal" largest number that would work as if we were adding
		 * two decimal check digits at the right end.  From that we want to change all
		 * but the left-most digit to zero (done using modulo) then subtract 1.
		 * The comments below show the actual values for TOKEN_LENGTH=4 and BASE=67.
		 */
		int n = (MAX_TOKEN_NUMBER / 100) - 1;	// token=20151121; n = 201510
		int len = String.valueOf(n).length();	// len = 6
		int factor = (int)Math.pow(10, len-1);	// factor = 100000
		n = n - (n % factor);					// n = 200000
		MAX_INPUT_NUMBER = n - 1;				//   = 199999
		INPUT_NUMBER_LENGTH = String.valueOf(MAX_INPUT_NUMBER).length();
		log.debug("BASE="+BASE+", max token="+MAX_TOKEN_NUMBER+", max in="+MAX_INPUT_NUMBER);
	}

	private NumberUtils() {
	}

//	private static void instantiate() {
//		if (instance == null) {
//			instance = new NumberUtils();
//		}
//	}

	private static int getMaxInputNumber() {
//		instantiate();	// force static calculations
		return MAX_INPUT_NUMBER;
	}

	/**
	 * Compare two Integers and return the standard -1/0/1 value for comparison
	 * methods. Accepts null arguments.
	 *
	 * @param int1
	 * @param int2
	 * @return <li>-1 if int1 is null and int2 is not, or if int1 < int2.
	 * 			<li>0 if both are null, or if int1 = int2.
	 * 			<li>1 if int1 is not null and int2 is null, or if int1 > int2.
	 */
	public static int compare(Integer int1, Integer int2) {
		int ret;
		if (int1 == null) {
			if (int2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (int2 == null) {
			ret = 1;
		}
		else {
			ret = int1.compareTo(int2);
		}
		return ret;
	}

	/**
	 * Compare two Shorts and return the standard -1/0/1 value for comparison
	 * methods. Accepts null arguments.
	 *
	 * @param short1
	 * @param short2
	 * @return <li>-1 if short1 is null and short2 is not, or if short1 < short2.
	 * 			<li>0 if both are null, or if short1 = short2.
	 * 			<li>1 if short1 is not null and short2 is null, or if short1 > short2.
	 */
	public static int compare(Short short1, Short short2) {
		int ret;
		if (short1 == null) {
			if (short2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (short2 == null) {
			ret = 1;
		}
		else {
			ret = short1.compareTo(short2);
		}
		return ret;
	}

	/**
	 * Compare two Byte`s and return the standard -1/0/1 value for comparison
	 * methods. Accepts null arguments.
	 *
	 * @param byte1
	 * @param byte2
	 * @return <li>-1 if byte1 is null and byte2 is not, or if byte1 < byte2.
	 * 			<li>0 if both are null, or if byte1 = byte2.
	 * 			<li>1 if byte1 is not null and byte2 is null, or if byte1 > byte2.
	 */
	public static int compare(Byte byte1, Byte byte2) {
		int ret;
		if (byte1 == null) {
			if (byte2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (byte2 == null) {
			ret = 1;
		}
		else {
			ret = byte1.compareTo(byte2);
		}
		return ret;
	}

	/**
	 * Compare two Shorts and return the standard -1/0/1 value for comparison
	 * methods. Accepts null arguments.
	 *
	 * @param value1
	 * @param value2
	 * @return <li>-1 if value1 is null and value2 is not, or if value1 < value2.
	 * 			<li>0 if both are null, or if value1 = value2.
	 * 			<li>1 if value1 is not null and value2 is null, or if value1 > value2.
	 */
	public static int compare(BigDecimal value1, BigDecimal value2) {
		int ret;
		if (value1 == null) {
			if (value2 == null) {
				ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else if (value2 == null) {
			ret = 1;
		}
		else {
			ret = value1.compareTo(value2);
		}
		return ret;
	}

	/**
	 * Compares two int's and returns the standard -1, 0, 1 values for
	 * comparison.
	 *
	 * @param i1 The first int.
	 * @param i2 The second int.
	 * @return 0 if the int's are equal, -1 if the first is smaller than the
	 *         second, and 1 if the first is larger than the second.
	 */
	public static int compare(int i1, int i2) {
		return ( i1 == i2 ? 0 : i1 < i2 ? -1 : 1 );
	}

	/**
	 * Determine if a BigDecimal value is "empty": null or zero.
	 * @param num
	 * @return True iff 'num' is null or zero.
	 */
	public static boolean isEmpty(BigDecimal num) {
		return (num == null || (num.signum() == 0));
	}

	/**
	 * Round a decimal time based on the specified rounding type.
	 *
	 * @param time The BigDecimal time value to be rounded.
	 * @param roundingType The HourRoundingType which will control how the input
	 *            time gets rounded, e.g., to the nearest 1/10th or 1/4 hour.
	 *
	 * @return The time, rounded up to the appropriate interval based on the
	 *         given roundingType value.
	 */
	public static BigDecimal round(BigDecimal time, HourRoundingType roundingType, RoundingMode mode) {
		if (time == null) {
			return null;
		}
		if (roundingType == HourRoundingType.TENTH) {
			time = time.setScale(1, mode);
		}
		else {
			BigDecimal hours = time.setScale(0, RoundingMode.FLOOR);
			float minutes = (time.subtract(hours).multiply(Constants.MINUTES_PER_HOUR)).floatValue();
			float partMinutes = roundingType.getMinutes(); // e.g., 6 if rounding to 1/10th.
			int iMinutes = (int)Math.floor((minutes+(partMinutes-0.0001f)) / partMinutes);
			// minutes is now the integral number of 1/10ths of an hour, or 1/4ths or whatever.
			time = hours.setScale(2);
			if (iMinutes != 0) {
				time = time.add(new BigDecimal(iMinutes).divide(roundingType.getDivisor()));
			}
		}
		return time;
	}

	/**
	 * Safely add multiple BigDecimal or null values. Null values are ignored.
	 * If all inputs are null, then null is returned.
	 *
	 * @param args One or more BigDecimal values, any (or all) of which may be
	 *            null.
	 * @return Null iff all inputs are null, otherwise returns the sum of all
	 *         non-null inputs.
	 */
	public static BigDecimal safeAdd(BigDecimal... args) {
		BigDecimal total = BigDecimal.ZERO;
		boolean good = false;
		for (BigDecimal d : args) {
			if (d != null) {
				total = total.add(d);
				good = true; // at least one arg is non-null
			}
		}
		if (good) {
			return total;
		}
		else {
			return null;
		}
	}

	/**
	 * Safely add multiple Integer or null values. Null values are ignored.
	 * If all inputs are null, then null is returned.
	 *
	 * @param args One or more Integer values, any (or all) of which may be
	 *            null.
	 * @return Null iff all inputs are null, otherwise returns the sum of all
	 *         non-null inputs.
	 */
	public static Integer safeAdd(Integer... args) {
		int total = 0;
		boolean good = false;
		for (Integer d : args) {
			if (d != null) {
				total += d.intValue();
				good = true; // at least one arg is non-null
			}
		}
		if (good) {
			return total;
		}
		else {
			return null;
		}
	}

	/**
	 * Safely add multiple Byte or null values. Null values are ignored.
	 * If all inputs are null, then null is returned.
	 *
	 * @param args One or more Byte values, any (or all) of which may be
	 *            null.
	 * @return Null iff all inputs are null, otherwise returns the sum of all
	 *         non-null inputs.
	 */
	public static Byte safeAdd(Byte... args) {
		byte total = 0;
		boolean good = false;
		for (Byte d : args) {
			if (d != null) {
				total += d.byteValue();
				good = true; // at least one arg is non-null
			}
		}
		if (good) {
			return total;
		}
		else {
			return null;
		}
	}

	/**
	 * Safely multiple one or more BigDecimal values, any of which may be null.
	 *
	 * @param first The first BigDecimal argument, which may be null.
	 * @param args A series of additional BigDecimal values, any of which may be null.
	 * @return null iff any input is null, otherwise it returns the result of a
	 *         BigDecimal multiply of all the args.
	 */
	public static BigDecimal safeMultiply(BigDecimal first, BigDecimal... args) {
		BigDecimal total = null;
		if (first != null) {
			total = first;
			for (BigDecimal d : args) {
				if (d == null) {
					total = null;
					break;
				}
				total = total.multiply(d);
			}
		}
		return total;
	}

	/**
	 * Scale the given value to the number of decimal places appropriate for
	 * hourly timecard rates (minimum of 2, maximum of 4 decimal places).
	 *
	 * @param value The value to scale, or null.
	 * @return The scaled value, or null if 'value' is null.
	 */
	public static BigDecimal scaleHourlyRate(BigDecimal value) {
		return scaleTo(value, Constants.HOURLY_RATE_SCALE_MIN, Constants.HOURLY_RATE_SCALE_MAX);
	}

	/**
	 * Scale the given value to the 2 decimal places.
	 *
	 * @param value The value to scale, or null.
	 * @return The scaled value, or null if 'value' is null.
	 */
	public static BigDecimal scaleTo2Places(BigDecimal value) {
		return scaleTo(value, 0, 2);
	}

	/**
	 * Scale the given value to within the range of the number of decimal places
	 * specified.
	 *
	 * @param value The value to scale, or null.
	 * @param minPlaces The minimum number of decimal places.
	 * @param maxPlaces The maximum number of decimal places.
	 * @return The scaled value, or null if 'value' is null.
	 */
	public static BigDecimal scaleTo(BigDecimal value, int minPlaces, int maxPlaces) {
		if (value == null) {
			return null;
		}
		BigDecimal temp = value.stripTrailingZeros();
		if (temp.scale() < minPlaces) {
			temp = temp.setScale(minPlaces);
		}
		else if (temp.scale() > maxPlaces) {
			temp = temp.setScale(maxPlaces, RoundingMode.HALF_UP);
		}
		return temp;
	}

	/**
	 * Performs the MOD 10 checksum (Luhn Mod-10) validation.
	 * @param strNumber The number to be checked.  Must be non-null and
	 * only consist of numeric digits (0-9).
	 * @return True iff the number passes the checksum test.
	 */
	public static boolean validateChecksum(String strNumber) {
		// perform the MOD 10 checksum (Luhn Mod-10)
		boolean bRet = false;
		int qsum = 0;
		int mul = 1;
		int tproduct;
		String digit;
		try {
			for (int i = strNumber.length()-1; i >= 0; i--) {
				digit = strNumber.substring(i, i+1);
				tproduct = mul * Integer.parseInt(digit);
				if (tproduct >= 10) {
					qsum += (tproduct % 10) + 1;
				}
				else {
					qsum += tproduct;
				}
				//log.debug("mul="+mul+", dig="+digit+", tp="+tproduct+", qs="+qsum);
				mul = ((mul == 1) ? 2 : 1 );
			} // End for loop

			if ((qsum % 10) == 0) { // Did the mod check work?
				bRet = true;
			}
		}
		catch (NumberFormatException e) {
			bRet = false;
		}
		log.debug("str="+strNumber+" - "+bRet);
		return bRet;
	}

	public static int calculateCheckDigit(String strNumber) {
		int qsum = 0;
		int mul = 2;
		int tproduct;
		String digit;
		for (int i = strNumber.length()-1; i >= 0; i--) {
			digit = strNumber.substring(i, i+1);
			tproduct = mul * Integer.parseInt(digit);
			if (tproduct >= 10) {
				qsum += (tproduct % 10) + 1;
			}
			else {
				qsum += tproduct;
			}
			//log.debug("mul="+mul+", dig="+digit+", tp="+tproduct+", qs="+qsum);
			mul = ((mul == 1) ? 2 : 1 );
		} // End for loop
		qsum = qsum % 10;
		qsum = (qsum == 0 ? 0 : 10-qsum);
		log.debug("str="+strNumber+" - "+qsum);
		return qsum;
	}

	/**
	 * Given a positive numeric value, add 2 check digits and convert to our alphanumeric
	 * encoding.
	 * @param number The integer value to be turned into a token.  If the value exceeds
	 * 'MAX_NUMBER', then the number used will be 'number' modulo MAX_NUMBER.
	 *
	 * @return The String token corresponding to the number.  The 'parseToken'
	 * method is used to turn the token back into a number.  Returns an empty string
	 * if the 'number' is negative.
	 */
	public static String createToken(int number) {
		String token="";
		if (number >= 0) {
			number = number % getMaxInputNumber();	// modulo the largest allowed value
			String strNumber = NUMBER_PAD + String.valueOf(number); // pad on the left with 0's
			strNumber = strNumber.substring(strNumber.length()-INPUT_NUMBER_LENGTH); // and get fixed length
			int checkDigit = calculateCheckDigit(strNumber);
			int checkDigit2 = 9 - checkDigit;
			// Insert check digits as second digit and final digit
			strNumber = strNumber.substring(0, 1) + checkDigit + strNumber.substring(1) + checkDigit2;
			token = convertToAlpha(Integer.parseInt(strNumber));
			token = TOKEN_PAD + token;
			token = token.substring(token.length()-TOKEN_LENGTH);
		}
		log.debug("n="+number+", token="+token);
		return token;
	}

	/**
	 * Convert an int into an alphanumeric string using a base of "BASE", which
	 * is the length of the "ALPHAMERIC" static constant.
	 */
	private static String convertToAlpha(int number) {
		String str = "";
		int lognumber = number;
		int n;
		while (number > 0) {
			n = number % BASE;
			str = ALPHAMERIC.charAt(n) + str;
			number = (number-n) / BASE;
		}
		log.debug("n="+lognumber+", str="+str);
		return str;
	}

	/**
	 * Convert a string that was encoded using "convertToAlpha", back
	 * to an integer.
	 * @param str The String to convert.
	 * @return  The integer equivalent of 'str', or -1 if the String
	 * contains unrecognized characters.
	 */
	private static int convertToNumber(String str) {
		int result = 0;
		for (int i = 0; i < str.length(); i++ ) {
			result *= BASE;
			String s = str.substring(i, i+1);
			int n = ALPHAMERIC.indexOf(s);
			if (n >= 0) {
				result += n;
			}
			else {
				return -1;
			}
		}
		log.debug("s="+str+", n="+result);
		return result;
	}

	/**
	 * Given a token string, calculate the original number used to create it.
	 * This is done by converting the alphanumeric string into its decimal
	 * equivalent, then removing and validating the check digits.
	 * @param token
	 * @return The (non-negative) integer value corresponding to the token,
	 * or -1 if the token is not valid (invalid characters or check digits
	 * don't validate).
	 */
	public static int parseToken(String token) {
		int result = -1;
		result = convertToNumber(token);
		if (result >= 0) {
			String strNumber = NUMBER_PAD + String.valueOf(result); // pad on the left with 0's
			strNumber = strNumber.substring(strNumber.length()-TOKEN_NUMBER_LENGTH); // and get fixed length
			// Extract the two check digits
			String checkDigit1 = strNumber.substring(1, 2);
			String checkDigit2 = strNumber.substring(TOKEN_NUMBER_LENGTH-1); // last digit
			if (Integer.parseInt(checkDigit1) + Integer.parseInt(checkDigit2) == 9) {
				// remove the check digits from the input number
				strNumber = strNumber.substring(0, 1) + strNumber.substring(2, TOKEN_NUMBER_LENGTH-1);
				int checkDigit = calculateCheckDigit(strNumber);
				if (checkDigit == Integer.parseInt(checkDigit1)) { // check digit matches
					result = Integer.parseInt(strNumber);
				}
				else {
					result = -3; // wrong check digit
				}
			}
			else {
				result = -2; // two check digits don't total 9
			}
		}
		log.debug("token="+token+", n="+result);
		return result;
	}

}
