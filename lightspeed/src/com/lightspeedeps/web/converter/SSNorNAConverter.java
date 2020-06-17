package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A custom JSF converter for converting Social Security numbers for edit and display.
 * Numbers may be entered with any combination of digits and dashes; all dashes are removed
 * for storing the value.  The value is displayed as "NNN-NN-NNNN".
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is used
 * in the JSP as follows:
 * <pre>
	< ice:inputText value="#{item.ssn}" >
		< f:converter converterId="lightspeed.SSNConverter" />
	< /ice:inputText>
  </pre>
 *
 */
@FacesConverter(value="lightspeed.SSNorNAConverter")
public class SSNorNAConverter extends SSNConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SSNorNAConverter.class);

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
		return getTaxIdAsObject(arg2, "Convert.SSN");
	}

}
