package com.lightspeedeps.web.converter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.common.CalendarUtils;

/**
 * Override the default DateTimeConverter to allow multiple styles of
 * Date formats to be entered in one control.
 */
@FacesConverter(value="lightspeed.MultiDateConverter")
public class MultiDateConverter extends javax.faces.convert.DateTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MultiDateConverter.class);

	/** The default output pattern */
	private static final String DEFAULT_PATTERN = "MMMM d, yyyy";

	/** The default list of allowable input patterns.  Note that patterns with "yy" MUST precede
	 * the corresponding pattern with "yyyy", otherwise the "yyyy" pattern will match first and
	 * parse the year as "00nn"! */
	private static final List<String> DEFAULT_PATTERNS =
			Arrays.asList("MMMM d, yy", "MMMM d, yyyy", "MMM d, yy", "MMM d, yyyy", "MM/d/yy", "MM/d/yyyy",
					"d MMM yy", "d MMM yyyy", "d MMM", "MM/d", "MMM d", "MM-d-yy", "MM-d-yyyy", "MM-d");

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {

		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}

		Date date = null;
		List<String> patterns = getPatterns(component);

		for (String pattern : patterns) {
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			sdf.setLenient(false); // Don't parse dates like 33-33-3333.

			try {
				date = sdf.parse(value);
				if (pattern.indexOf("yy") < 0) {
					// date was entered without a year
					Calendar calNow = new GregorianCalendar();
					Calendar cal = new GregorianCalendar();
					cal.setTime(date);
					cal.set(Calendar.YEAR, calNow.get(Calendar.YEAR)); // try using current year
					date = cal.getTime();
					int days = CalendarUtils.durationInDays(calNow.getTime(), date);
					// then keep date within +/- 6 months of current date...
					if (days > 182) { // too far in future, use prior year
						cal.set(Calendar.YEAR, calNow.get(Calendar.YEAR)-1);
						date = cal.getTime();
					}
					else if (days < -182) { // too far in past, use next year
						cal.set(Calendar.YEAR, calNow.get(Calendar.YEAR)+1);
						date = cal.getTime();
					}
					//log.debug(date);
				}
				break; // stop on first successful parse
			}
			catch (Exception ignore) {
				//log.debug(ignore.getLocalizedMessage() + ", pattern=" + pattern);
			}
		}

		if (date == null) {
			throw new ConverterException(new FacesMessage(
					"Invalid date format; please use one of " + patterns));
		}
		//log.debug("in=" + value + ", out=" + date);

		return date;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		if (value == null) {
			return "";
		}
		String pattern = getPattern(component);
		return new SimpleDateFormat(pattern).format((Date) value);
	}

	/**
	 * Load and return a 'patterns' attribute, if specified; otherwise return
	 * the built-in default set of patterns.
	 *
	 * @param component
	 * @return A List of date-format pattern strings.
	 */
	protected static List<String> getPatterns(UIComponent component) {
		List<String> patterns = new ArrayList<String>();

		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			String pattern = (String)component.getAttributes().get("pattern" + i);

			if (pattern != null) {
				patterns.add(pattern);
			}
			else {
				break;
			}
		}

		if (patterns.isEmpty()) {
			patterns = DEFAULT_PATTERNS;
//			throw new IllegalArgumentException(
//					"Please provide <f:attribute name=\"patternX\"> where X is the order number");
		}

		return patterns;
	}

	/**
	 * Load and return the 'pattern' attribute, if specified; otherwise return
	 * the built-in default pattern.
	 *
	 * @param component
	 * @return A date format string.
	 */
	protected static String getPattern(UIComponent component) {
		String pattern = (String)component.getAttributes().get("pattern");
		if (pattern == null) {
			pattern = DEFAULT_PATTERN;
		}
		return pattern;
	}

}
