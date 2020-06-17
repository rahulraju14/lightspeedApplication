package com.lightspeedeps.web.converter;

import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ApplicationUtils;

/**
 * Override the default DateTimeConverter to allow shorter format output for
 * "PM" and "AM". It also provides more flexible input options:
 * <ul>
 * <li>omitting the minutes, which implies ':00'</li>
 * <li>shortening 'am' to 'a' and 'pm' to 'p'</li>
 * <li>making a blank between the time and am/pm optional</li>
 * <li>entering the time in 24-hour notation</li>
 * </ul>
 *  This is designed particularly for use on the
 * Time Sheet view and edit pages (timesheetview.jsp) and may be used on other
 * pages as well.
 */
@FacesConverter(value="lightspeed.DateTimeConverter")
public class DateTimeConverter extends javax.faces.convert.DateTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DateTimeConverter.class);

	/** A regular expression which defines all the variety of input styles we support
	 * for entering time of day. */
	private final static String TIME_PATTERN_RE = "[0-2]?[0-9](:[0-5][0-9])?[ ]*(A|AM|P|PM)?";

	/** A Pattern object built on our regular expression for allowed time-of-day inputs. */
	private final static Pattern TIME_PATTERN = Pattern.compile(TIME_PATTERN_RE);

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		if (value != null && value.trim().length() > 0) {
			passParameters(component);
			value = formatTime(value);
		}
		// set the 'type', so that in case of an input error, the converter will use
		// the time-based message id: javax.faces.converter.DateTimeConverter.TIME
		setType("time");
		Object obj = super.getAsObject(context, component, value);
//		if (obj != null) {
//			int n = ((Date)obj).getHours();
//			log.debug(n);
//		}
		return obj;
	}

	public Object superGetAsObject(FacesContext context, UIComponent component, String value) {
		return super.getAsObject(context, component, value);
	}

	/**
	 * This method accepts our many varied time formats and returns a "standard"
	 * format of "hh:mma" (where a = AM or PM).
	 *
	 * @param value A non-null user entered time string, including the standard
	 *            format, plus optional blank before AM/PM, any case am/pm, or
	 *            just 'a' or 'p', or no am/pm indicator, and hours in either
	 *            12- or 24-hour format, and :mm is optional.
	 * @return A "standard" format time string as hh:mma (where a = AM or PM).
	 */
	protected String formatTime(String value) {
		value = value.toUpperCase().trim();
		if (TIME_PATTERN.matcher(value).matches()) { // just skip our changes if it's not valid
			if (value.indexOf("AM") == -1 && value.indexOf("PM") == -1) {
				if (value.indexOf('A') > 0) {
					value = value.replace(" A", "A");
					value = value.replace("A", "AM");
				}
				else if (value.indexOf('P') > 0) {
					value = value.replace(" P", "P");
					value = value.replace("P", "PM");
				}
				else { // no am/pm indicator - assume AM unless hour > 12
					int hour=0, ix=0;
					try {
						if ( (ix=value.indexOf(':')) == -1) {
							hour = Integer.parseInt(value);
						}
						else {
							hour = Integer.parseInt(value.substring(0,ix));
						}
					}
					catch (NumberFormatException e) {
					}
					if (hour == 0) {
						value = "12" + (ix==-1 ? ":00" : value.substring(ix)) + "AM";
					}
					else if (hour < 13) {
						value += "AM";
					}
					else if (hour < 24) {
						value = "" + (hour-12) + (ix==-1 ? ":00" : value.substring(ix)) + "PM";
					}
				}
			}
			else if (value.indexOf(' ') > 0) { // remove embedded blank
				value = value.replace(" AM", "AM");
				value = value.replace(" PM", "PM");
			}
			if (value.indexOf(':') == -1) {
				value = value.replace("AM", ":00AM");
				value = value.replace("PM", ":00PM");
			}
		}
		return value;
	}

	/**
	 * Take standard converter's output and replace "AM" with 'a' and
	 * "PM" with 'p'.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {

		passParameters(component);
		String s = super.getAsString(context, component, value);

		//log.debug("s=" + s);

		int i = s.indexOf("PM");
		if (i > -1) {
			s = s.replace("PM", "p");
		}
		else {
			i = s.indexOf("AM");
			if (i > -1) {
				s = s.replace("AM", "a");
			}
		}
		return s;
	}

	/**
	 * This method allows our subclasses to call the built-in "getAsString" and bypass
	 * our implementation.
	 */
	public String superGetAsString(FacesContext context, UIComponent component, Object value) {
		return super.getAsString(context, component, value);
	}

	/**
	 * If attributes are set for DateTimeConverter parameters, pass them
	 * along, otherwise pass our defaults (pattern designed for TimeSheet view & edit).
	 * @param component
	 */
	protected void passParameters(UIComponent component) {
		String pattern = (String)component.getAttributes().get("pattern");
		if (pattern == null) {
			pattern = getDefaultPattern();
		}
		super.setPattern(pattern);

		TimeZone timezone = null;
		String tz = (String)component.getAttributes().get("timeZone");
		if (tz != null) {
			timezone = TimeZone.getTimeZone(tz);
		}
		if (timezone == null) {
			timezone = ApplicationUtils.getTimeZoneStatic();
			//log.debug("using default tz=" + timezone);
		}
		super.setTimeZone(timezone);
	}

	protected String getDefaultPattern() {
		return "h:mma";
	}

}
