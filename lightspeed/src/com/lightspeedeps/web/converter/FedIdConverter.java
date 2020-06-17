package com.lightspeedeps.web.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A custom JSF converter for converting Federal Tax Id numbers for edit and
 * display. Numbers may be entered with any combination of digits and dashes;
 * all dashes are removed for storing the value. The value is displayed as
 * "NN-NNNNNNN".
 * <p/>
 * As with many converters, this one will issue a message by throwing a
 * ConverterException if the input is invalid. Note, though, that such an error
 * will prevent the usual JSF life-cycle steps where Java validation of input
 * could be done. Therefore, any messages from Java validation code will not be
 * issued until the user fixes errors from this (or similar) converters.
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is
 * used in the JSP as follows:
 *
 * <pre>
	< ice:inputText value="#{item.federalTaxId}" >
		< f:converter converterId="lightspeed.FedIdConverter" />
	< /ice:inputText>
 * </pre>
 */
@FacesConverter(value="lightspeed.FedIdConverter")
public class FedIdConverter implements Converter {
	private static final Log log = LogFactory.getLog(FedIdConverter.class);

	/** Convert displayed/input String format, with dashes to plain numeric string. */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {

		return getTaxIdAsObject(arg2, "Convert.FederalId");
	}

	/** Convert plain numeric string to pretty display format with dashes. */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String str = validateOutput(arg2);

		if (str.length() == 9) {
			str = str.substring(0,2) + "-" + str.substring(2);
		}
		return str;
	}

	/**
	 * Convert displayed/input String format, with dashes to plain numeric
	 * string. Optional issue a message to the user via a ConverterException.
	 *
	 * @param arg2 The input argument (the user's input); may be null.
	 * @param msgId A message id to be used to generate an error message if the
	 *            input is not valid. If this is null, no message is issued --
	 *            meaning no ConverterException is thrown. (This allows the
	 *            normal JSF life-cycle to complete.)
	 * @return The input with dashes ("-"), vertical bars ("|") and blanks
	 *         removed; if the length of the string (without the punctuation) is
	 *         zero, then null is returned. If arg2 is null, null is returned.
	 */
	protected Object getTaxIdAsObject(String arg2, String msgId) {
		String idstring = StringUtils.cleanTaxId(arg2); // clean and validate
		if (idstring == null) {
			return null;
		}
		if (idstring.length() == 0) {
			idstring = arg2;
			if (msgId != null) {
				HeaderViewBean.getInstance().setMsgExists(true);
				String msg = MsgUtils.formatMessage(msgId, arg2);
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,msg,msg));
			}
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

	/**
	 * Check a string for being a valid tax identification number --
	 *  a U.S. Federal Tax Id Number.
	 *
	 * @param taxId The string that may be a tax id.
	 * @return The given tax id with punctuation (-, |) and spaces removed. If
	 *         null is returned, the given taxId was either null, empty, or only
	 *         contained punctuation and spaces. If a non empty string ("Invalid") is
	 *         returned, then the taxId field was not valid; that is, it (a)
	 *         contained non-numeric characters other than punctuation and
	 *         spaces, or (b) was not 9 numeric digits in length.
	 *         If a non empty string ("Valid") is
	 *         returned, then the taxId field is valid.
	 */
	public static String checkTaxId(String taxId) {
		String ret = "";
		String regex = "\\d+";
		if (taxId == null) {
			return null;
		}
		String idstring = taxId.replaceAll("-| ", "");
		//log.debug("Tax Id = " + idstring);
		if (idstring.length() == 0) {
			return null;
		}
		if (! StringUtils.isEmpty(idstring)) {
			if (idstring.length() == 9) {
				log.debug("9 Digits");
				if (idstring.matches(regex)) {
					log.debug("All are numeric values.");
					ret = "Valid";
				}
				else {
					log.debug("");
					ret = "Invalid";
				}
			}
			else {
				log.debug(idstring.length() + " Digits");
				ret = "Invalid";
			}
		}
		return ret;
	}

}
