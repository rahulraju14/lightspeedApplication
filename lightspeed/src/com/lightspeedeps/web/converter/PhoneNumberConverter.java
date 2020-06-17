package com.lightspeedeps.web.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * A custom JSF converter for converting telephone numbers for edit and display.
 * Numbers may be entered with any combination of digits, dashes, spaces, and
 * parentheses; all dashes are removed for storing the value. The value is
 * displayed as "(aaa) nnn-nnnn" where 'aaa' is the area code.  A conversion error
 * will be thrown if the phone number is in an invalid format (e.g., too few digits).
 * International numbers are accommodated by the user entering a leading 0 or +.
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is
 * used in the JSP as follows:
 *
 * <pre>
 * 	< ice:inputText value="#{item.phone}" >
 * 		< f:converter converterId="lightspeed.PhoneNumberConverter" />
 * 	< /ice:inputText>
 * </pre>
 *
 */
@FacesConverter(value="lightspeed.PhoneNumberConverter")
public class PhoneNumberConverter implements Converter {
	private static final Log log = LogFactory.getLog(PhoneNumberConverter.class);

	/** Convert displayed/input String format, with dashes to plain numeric string. */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {

		return getNumberAsObject(arg2, "");
	}

	/** Convert plain numeric string to pretty display format with dashes. */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String idstring = validateOutput(arg2);

		if (idstring.length() == 0 || idstring.charAt(0) == '+' || idstring.charAt(0) == '0') {
			// return it as-is, no reformatting
		}
		else if (idstring.length() == 10) {
			idstring = "(" + idstring.substring(0,3) + ") " + idstring.substring(3, 6) + "-" + idstring.substring(6);
		}
		else if (idstring.length() == 11 && idstring.charAt(0) == '1') {
			idstring = "1-" + idstring.substring(1,4) + "-" + idstring.substring(4, 7) + "-" + idstring.substring(7);
		}

		return idstring;
	}

	/**
	 * Convert displayed/input String format, with dashes to plain numeric
	 * string.
	 *
	 * @param arg2 The input argument (the user's input); may be null.
	 * @param msgId A message id to be used to generate an error message if the
	 *            input is not valid.
	 * @return The input with all dashes ("-") removed; if the length of the
	 *         string (without dashes) is zero, then null is returned. If arg2
	 *         is null, null is returned.
	 */
	protected Object getNumberAsObject(String arg2, String msgId) {

		String idstring = PhoneNumberValidator.cleanNumber(arg2);
		if (idstring == null) { // input was null, empty, or blanks
			return null;
		}

		if (idstring.length() == 0) { // not a valid phone number
			idstring = arg2.trim(); // return original input, trimmed
			HeaderViewBean.getInstance().setMsgExists(true);
			FacesMessage message = new FacesMessage();
			message.setDetail("The phone number is not a valid format");
			message.setSummary("The phone number is not a valid format; it should be nnn-nnn-nnnn or +/0...(international)");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ConverterException(message);
		}
		return idstring;
	}

	/**
	 * Verify that the object intended for output is a non-null String.
	 *
	 * @param arg2 The object to be output as an Id number string.
	 * @return The object arg2, cast as a String, if it is a String, otherwise
	 *         "" (an empty String).
	 */
	protected String validateOutput(Object arg2) {
		String str = "";
		if (arg2 instanceof String) {
			str = (String)arg2;
		}
		else {
			if (arg2 != null) {
				log.error("unable to convert: " + arg2.toString());
			}
			return "";
		}
		return str;
	}

}
