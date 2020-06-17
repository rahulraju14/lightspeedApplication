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
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A custom JSF converter for converting zip codes for edit and display. Numbers
 * may be entered with any combination of digits and dashes; all dashes are
 * removed for storing the value. The value is displayed as "NNNNN-NNNN".
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is
 * used in the JSP as follows:
 *
 * <pre>
	< ice:inputText value="#{item.zip}" >
		< f:converter converterId="lightspeed.ZipConverter" />
	< /ice:inputText>
 * </pre>
 *
 */
@FacesConverter(value = "lightspeed.ZipConverter")
public class ZipConverter implements Converter {
	private static final Log log = LogFactory.getLog(ZipConverter.class);

	/**
	 * Convert displayed/input String format, with dashes to plain numeric
	 * string.
	 */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		return getZipCodeAsObject(arg2, "Convert.Zip");
	}

	/** Convert plain numeric string to pretty display format with dashes. */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String str = validateOutput(arg2);

		if (str.length() > 5) {
			str = str.substring(0, 5) + "-" + str.substring(5);
		}
		return str;
	}

	protected Object getZipCodeAsObject(String arg2, String msgId) {
		String idstring = cleanZipCode(arg2); // clean and validate
		if (idstring == null) {
			return null;
		}
		if (idstring.length() == 0) {
			idstring = arg2;
			if (msgId != null) {
				HeaderViewBean.getInstance().setMsgExists(true);
				String msg = MsgUtils.formatMessage(msgId, arg2);
				throw new ConverterException(
						new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
			}
		}
		return idstring;
	}

	public static String cleanZipCode(String zip) {
		if (zip == null) {
			return null;
		}
		String idstring = zip.replaceAll("-| ", "");
		if (idstring.length() == 0) {
			return null;
		}
		int idNumber = - 1;
		if (idstring.length() >= 5) {
			try {
				idNumber = Integer.parseInt(idstring);
			}
			catch (NumberFormatException e) {
			}
		}
		if (idNumber == - 1) {
			idstring = "";
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
