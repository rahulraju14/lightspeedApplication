package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class provides a slight modification to our DateTimeConverter class --
 * it overrides the defaultPattern to generate an output style of "h:mm AM". It
 * also adds a blank before the "AM/PM" indicator during getAsObject (input)
 * processing, so that the built-in getAsObject, which is called by our
 * DateTimeConverter, will parse the input without raising an error.
 * <p/>
 * Since it is built on our DateTimeConverter, it still allows the same variety
 * of time inputs, such as 'a' for am and 'p' for pm, or omitting am/pm
 * entirely.
 */
@FacesConverter(value="lightspeed.DateTimeConverterB")
public class DateTimeConverterB extends DateTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DateTimeConverterB.class);

	@Override
	protected String formatTime(String value) {
		//log.debug(value);
		value = super.formatTime(value);
		if (value.indexOf(' ') < 0) {
			value = value.replace("AM", " AM");
			value = value.replace("PM", " PM");
		}
		return value;
	}

	/**
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {

		passParameters(component);
		return superGetAsString(context, component, value);
	}

	@Override
	protected String getDefaultPattern() {
		return "h:mm a";
	}

}
