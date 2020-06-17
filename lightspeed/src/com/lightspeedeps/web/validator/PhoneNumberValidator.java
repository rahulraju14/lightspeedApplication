package com.lightspeedeps.web.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class containing a static phone number validation routine, which is also
 * registered as a JSF validation class, and used for that purpose on several
 * pages.
 */
@FacesValidator("lightspeed.phoneNumberValidator")
public class PhoneNumberValidator implements Validator {
	private static final Log log = LogFactory.getLog(PhoneNumberValidator.class);

	/** Pattern for North American phone numbers 1-nnn-nnn-nnnn, '1' and hyphens optional,
	 * area code may be in parentheses, and hyphens may be replaced by blanks or periods. */
	private static final String US_PHONE_NUM = "(?:\\+?1[-. ]?)?\\(?([2-9][0-9][0-9])\\)?[-.]?[ ]*([2-9][0-9][0-9])[-. ]?([0-9]{4})";
	private static final Pattern US_PHONE_PATTERN = Pattern.compile(US_PHONE_NUM);

	/** Pattern for any other phone number; starts with + or 0; contains numerics,
	 * and possibly parentheses, hyphens, periods and spaces. */
	private static final String OTHER_PHONE_NUM = "[+0][- .()0-9]{5,20}";
	private static final Pattern OTHER_PHONE_PATTERN = Pattern.compile(OTHER_PHONE_NUM);

	/** A non-US phone number needs this many numeric digits to be considered valid. */
	private static final int MIN_OTHER_DIGITS = 6;

	/** Creates a new instance of Phone Number Validator */
	public PhoneNumberValidator() {
	}

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) {
		/* retrieve the string value of the field */
		String phoneNumber = (String) value;

		boolean valid = isValid(phoneNumber);

		if (!valid) {
			// didn't work: component.getAttributes().put("value", "123-456-7890");
			// TODO can we change the value? (format it)
			FacesMessage message = new FacesMessage();
			message.setDetail("The phone number is not valid");
			message.setSummary("The phone number is not a valid format; it should be nnn-nnn-nnnn or +/0...(international)");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}
	}

	/**
	 * Validate that the input string is a valid (or at least reasonable) phone number. A null or
	 * empty argument returns false.
	 * <p>
	 * Numbers are first validated against a pattern for North American telephone numbers. See
	 * {@link #US_PHONE_NUM}. If that pattern is not matched, then a less restrictive pattern
	 * {@link #OTHER_PHONE_NUM} is used.
	 *
	 * @param phoneNumber The telephone number to be checked.
	 * @return True if the String is a reasonable telephone number.
	 */
	public static boolean isValid(String phoneNumber) {
		boolean bRet = false;
		if (phoneNumber != null) {
			phoneNumber = phoneNumber.trim();
			Matcher matcher = US_PHONE_PATTERN.matcher(phoneNumber);
			bRet = matcher.matches();
			if ( ! bRet) {
				matcher = OTHER_PHONE_PATTERN.matcher(phoneNumber);
				if (matcher.matches()) {
					// make sure it has enough digits
					int digits = 0;
					for (char c : phoneNumber.toCharArray()) {
						if (Character.isDigit(c)) {
							digits++;
						}
					}
					bRet = (digits >= MIN_OTHER_DIGITS);
				}
			}
		}
		log.debug("isValid(" + phoneNumber + ")=" + bRet);
		return bRet;
	}

	/**
	 * Cleans a phone number of punctuation and validates it.
	 *
	 * @param input The phone number, possibly including punctuation.
	 * @return Either: (1) null if the input was null, or an empty or blank
	 *         string; (2) an empty string ("") if the input was not a valid
	 *         phone number; (3) the phone number with punctuation removed,
	 *         unless it started with a "+" or "0"; (4) the phone number as
	 *         entered except trimmed of blanks, if it begins with a "+" or "0".
	 */
	public static String cleanNumber(String input) {
		if (input == null) {
			return null;
		}
		input = input.trim();
		if (input.length() == 0) {
			return null;
		}
		boolean valid = isValid(input);

		String phoneNumber = input.replaceAll("-|\\(|\\)| ", "");

		if (valid && (phoneNumber.charAt(0) == '+' || phoneNumber.charAt(0) == '0')) {
			phoneNumber = input; // return original input (trimmed) if leading "+" or "0"
		}
		if (! valid) {
			phoneNumber = ""; // return empty string to indicate error
		}
		return phoneNumber;
	}

	public static String formatted(String input) {
		String output = cleanNumber(input);

		if (output == null || output.length() == 0 || output.charAt(0) == '+' || output.charAt(0) == '0') {
			// return it as-is, no reformatting
			return input; // input was empty, invalid, or started with special character
		}

		if (output.length() == 10) {
			output = "(" + output.substring(0,3) + ") " + output.substring(3, 6) + "-" + output.substring(6);
		}
		else if (output.length() == 11 && output.charAt(0) == '1') {
			output = "1-" + output.substring(1,4) + "-" + output.substring(4, 7) + "-" + output.substring(7);
		}
		else {
			return input;
		}

		return output;
	}

}
