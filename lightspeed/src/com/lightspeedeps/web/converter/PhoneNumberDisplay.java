/**
 * File: PhoneNumberDisplay.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.convert.FacesConverter;

import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * A custom JSF converter for converting telephone numbers for edit and display.
 * Numbers may be entered with any combination of digits, dashes, spaces, and
 * parentheses; all dashes are removed for storing the value. The value is
 * displayed as "(aaa) nnn-nnnn" where 'aaa' is the area code. No error is thrown
 * regardless of the input value.
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is
 * used in the JSP as follows:
 *
 * <pre>
 * 	< ice:inputText value="#{item.phone}" >
 * 		< f:converter converterId="lightspeed.PhoneNumberDisplay" />
 * 	< /ice:inputText>
 * </pre>
 *
 */
@FacesConverter(value="lightspeed.PhoneNumberDisplay")
public class PhoneNumberDisplay extends PhoneNumberConverter {

	/**
	 * Convert displayed/input String format, with dashes to plain numeric
	 * string.
	 *
	 * @param arg2 The input argument (the user's input); may be null.
	 * @param msgId A message id to be used to generate an error message if the
	 *            input is not valid.
	 * @return The input with all dashes ("-") removed; if the length of the
	 *         string (without dashes) is zero, then null is returned. If arg2
	 *         is null, null is returned. For "invalid" phone numbers, the
	 *         original input is returned, and no error is thrown.
	 */
	@Override
	protected Object getNumberAsObject(String arg2, String msgId) {

		String idstring = PhoneNumberValidator.cleanNumber(arg2);

		if (idstring != null && idstring.length() == 0) { // not a valid phone number
			idstring = arg2.trim(); // return original input, trimmed
		}

		return idstring;
	}

}
