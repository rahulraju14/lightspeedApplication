package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A custom JSF converter for converting telephone numbers for edit and display.
 * Numbers may be entered with any combination of digits, dashes, spaces, and
 * parentheses; all dashes are removed for storing the value. The value is
 * displayed as ....
 * <p/>
 * This class is used in the JSP as follows:
 *
 * <pre>
 * 	< ice:inputText value="#{item.phone}" >
 * 		< f:converter converterId="lightspeed.PhoneNumberorNAConverter" />
 * 	< /ice:inputText>
 * </pre>
 *
 */
@FacesConverter(value="lightspeed.PhoneNumberOrNAConverter")
public class PhoneNumberOrNAConverter extends PhoneNumberConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PhoneNumberOrNAConverter.class);

	/** Convert displayed/input String format, with dashes to plain numeric string. */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {

		if (arg2 != null) {
			arg2 = arg2.trim();
			if (arg2.equalsIgnoreCase("na") || arg2.equalsIgnoreCase("n/a")) {
				arg2 = "N/A";
				return arg2;
			}
		}
		return getNumberAsObject(arg2, "");
	}

}
