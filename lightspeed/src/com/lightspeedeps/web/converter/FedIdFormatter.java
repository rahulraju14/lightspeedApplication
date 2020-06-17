package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A custom JSF converter for converting Federal Tax Id numbers for edit and
 * display. Numbers may be entered with any combination of digits and dashes;
 * all dashes are removed for storing the value. The value is displayed as
 * "NN-NNNNNNN".
 * <p/>
 * Unlike FedIdConverter, this converter will NOT throw an exception when an
 * invalid Federal ID number is entered. It just handles formatting the output,
 * and stripping the dash(es) from the input. It is expected that the validation
 * will be done later. The reason for this is that other fields can be validated
 * in Java on the same input cycle.
 * <p/>
 * This class is assigned a "converter id" in the faces-config.xml file, and is
 * used in the JSP as follows:
 *
 * <pre>
	< ice:inputText value="#{item.federalTaxId}" >
		< f:converter converterId="lightspeed.FedIdFormatter" />
	< /ice:inputText>
 * </pre>
 */
@FacesConverter(value="lightspeed.FedIdFormatter")
public class FedIdFormatter extends FedIdConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FedIdFormatter.class);

	/** Convert displayed/input String format, with dashes to plain numeric string. */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {

		return getTaxIdAsObject(arg2, null); // convert without errors issued
	}

}
