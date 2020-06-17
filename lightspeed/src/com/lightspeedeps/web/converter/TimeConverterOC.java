package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.Constants;

/**
 * This class provides a slight modification to our DateTimeConverter class --
 * it treats null values as "O/C" and vice-versa.
 * <p/>
 * Since it is built on our DateTimeConverter, it still allows the same variety
 * of time inputs, such as 'a' for am and 'p' for pm, or omitting am/pm
 * entirely.
 */
@FacesConverter(value="lightspeed.TimeConverterOC")
public class TimeConverterOC extends DateTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimeConverterOC.class);

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		Object obj = null;
		if (value != null) {
			value = value.trim().toUpperCase();
			if (value.length() > 0 && ! value.equals(getNullValue())) {
				if (value.equals("OC") || value.equals("O.C.")) {
					// treat same as null; will display "O/C"
				}
				else {
					passParameters(component);
					value = formatTime(value);
					obj = superGetAsObject(context, component, value);
				}
			}
		}
		return obj;
	}

	/**
	 * Convert the Object 'value' into a String representation for display.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		String str;
		if (value == null) {
			str = getNullValue();
		}
		else {
			str = super.getAsString(context, component, value);
		}
		return str;
	}

	protected String getNullValue() {
		return Constants.ON_CALL;
	}

}
