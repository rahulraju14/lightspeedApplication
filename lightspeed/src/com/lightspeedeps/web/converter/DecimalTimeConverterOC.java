/*
 * File: DecimalTimeConverter.java
 */
package com.lightspeedeps.web.converter;

import java.math.BigDecimal;
import java.util.regex.Matcher;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ReportTime;
import com.lightspeedeps.util.app.Constants;

/**
 * This class is designed to allow hh:mm style date entry into fields that are
 * stored as decimal time. The output style is always in decimal. The hh:mm
 * value is rounded DOWN to the earlier 1/10 of an hour increment, if necessary.
 * <p/>
 * It allows a wide variety of time inputs, such as 'a' for am and 'p' for pm,
 * or omitting am/pm entirely, using either 12- or 24-hour styles.
 */
@FacesConverter(value="lightspeed.DecimalTimeConverterOC")
public class DecimalTimeConverterOC extends DecimalTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DecimalTimeConverterOC.class);

	/**
	 * Called by the framework to convert a user's text input into the
	 * appropriate object to store, in this case, a BigDecimal value.
	 *
	 * @see javax.faces.convert.NumberConverter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		Object time = null;
		if (value != null && value.trim().length() > 0 &&
				(value.equalsIgnoreCase("oc") || value.equalsIgnoreCase("o/c"))) {
			time = ReportTime.OC_TIME;
		}
		else if (value == null || value.trim().length() == 0) {
			time = null;
		}
		else {
			time = super.getAsObject(context, component, value);
		}
		return time;
	}

	/**
	 * Called by the framework to convert an internal object (in this case,
	 * a BigDecimal time value) into the String to be displayed.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value instanceof BigDecimal) {
			if (((BigDecimal)value).signum() < 0) {
				return Constants.ON_CALL;
			}
		}
		passParameters(component);
		return super.getAsString(context, component, value);
	}

	/**
	 * Convert an hh:mm input into the equivalent decimal time value,
	 * rounding DOWN if necessary to the next 1/10 of an hour.
	 *
	 * @param m The Matcher that has already matched the user's input. This will
	 *            have the following groups: (1) hours, (3) minutes, (4) am/pm
	 *            indicator.  An 'am' indicator is ignored; a 'pm' indicator is
	 *            only used if the hour value is 1-12 (inclusive).
	 *
	 * @return The am/pm time converted to a BigDecimal value, with the minutes
	 *         converted to a decimal fraction as used in the timecard system.
	 */
	@Override
	protected BigDecimal convertTimeToDecimal(Matcher m) {
		return convertTimeToDecimal(m, false);
	}

}
