/**
 * File: MultiDateOrNAConverter.java
 */
package com.lightspeedeps.web.converter;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Date converter to be used for String date values. This will allow
 * for N/A values to be entered as well.
 */
@FacesConverter(value="lightspeed.MultiDateOrNAConverter")
public class MultiDateOrNAConverter implements Converter {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(MultiDateOrNAConverter.class);

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return "";
		}
		if(checkForNA((String)value)) {
			return (String)value;
		}

		return (String)value;
	}

	/** Convert displayed/input String format, with dashes to plain numeric string. */
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		if (value != null) {
			// LS-4278  May not be a date so test for na or n/a.
			if (checkForNA(value)) {
				return "N/A";
			}
		}

		if(!isDateValid(value, "MM/dd/yyyy")) { // MultiDateConverter.getPattern(component)
			throw new ConverterException(new FacesMessage("Invalid date format. Please match the requested format or enter N/A"));
		}

		return value;

	}

	/**
	 * Validate value to see it is set to na o n/ to cover different variations of N/A
	 *
	 * @param value value to be evaluated
	 * @return
	 */
	private boolean checkForNA(String value) {
		value = value.trim();
		value = value.toLowerCase();

		if (value.indexOf("na") > -1 || value.indexOf("n/") > -1) {
			return true;
		}

		return false;
	}

	/**
	 * Check to see if this is a valid date.
	 *
	 * @param date String value representing a date.
	 * @param pattern how date will be parsed
	 * @return
	 */
	private boolean isDateValid(String date, String pattern) {
		String [] patterns = {pattern};
		Date validDate;
		try {
			validDate = DateUtils.parseDateStrictly(date, patterns);
		}
		catch (ParseException e) {
			return false;
		}

		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(validDate);
		if(cal.get(Calendar.YEAR) < 1900) {
			return false;
		}

		return true;
	}
}
