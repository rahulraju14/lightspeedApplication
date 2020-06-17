/*
 * File: DecimalTimeConverter.java
 */
package com.lightspeedeps.web.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.HourRoundingType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * This class is designed to allow hh:mm style date entry into fields that are
 * stored as decimal time. The output style is always in decimal. The hh:mm
 * value is rounded UP to the next 1/10 of an hour increment, if necessary.
 * <p/>
 * It allows a wide variety of time inputs, such as 'a' for am and 'p' for pm,
 * or omitting am/pm entirely, using either 12- or 24-hour styles.
 */
@FacesConverter(value="lightspeed.DecimalTimeConverter")
public class DecimalTimeConverter extends javax.faces.convert.NumberConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DecimalTimeConverter.class);

	/** A regular expression which defines all the variety of input styles we support
	 * for entering time of day; we accept up to "49" for hour for timecard usage.
	 * Groups created by pattern: (1)=Hours; (2)=unused; (3)=Minutes; (4)=suffix (am/pm). */
	private final static String TIME_PATTERN_RE = "([0-4]?[0-9])(:([0-5][0-9]))?[ ]*(A|AM|P|PM)?";

	/** A Pattern object built on our regular expression for allowed time-of-day inputs. */
	private final static Pattern TIME_PATTERN = Pattern.compile(TIME_PATTERN_RE);

	/** Indicates how the values will be rounded, e.g., to the next 1/10th or 1/4 of
	 * an hour.  Defaults to 1/10th of an hour; usually set via a Session attribute set
	 * when a Production is entered.  May be overridden by an f:attribute setting in JSP. */
	private HourRoundingType roundingType = null;

	/** True iff times entered in decimal format should be rounded. */
	private Boolean roundDecimal = null;

	/**
	 * Called by the framework to convert a user's text input into the
	 * appropriate object to store, in this case, a BigDecimal value.
	 *
	 * @see com.lightspeedeps.web.converter.DateTimeConverter#getAsObject(javax.faces.context.FacesContext,
	 *      javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		passParameters(component);
		BigDecimal time = null;
		if (value != null && value.trim().length() > 0) {
			value = value.trim();
			Matcher m = TIME_PATTERN.matcher(value.toUpperCase());
			if (m.matches()) {
				time = convertTimeToDecimal(m);
			}
			else if (value.indexOf(':') >= 0) {
				String msg = MsgUtils.formatMessage("Convert.Time", value);
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,msg,msg));
			}
			else {
				Object obj = super.getAsObject(context, component, value);
				if (obj instanceof BigDecimal) {
					time = (BigDecimal)obj;
				}
				else if (obj instanceof Long) {
					time = new BigDecimal((Long)obj);
				}
				else if (obj instanceof Double) {
					time = new BigDecimal((Double)obj);
				}
				if (getRoundDecimal()) {
					time = round(time);
				}
				else {
					if (time != null) {
						time = time.setScale(2, RoundingMode.HALF_UP);
					}
				}
			}
			//log.debug(time);
		}
		return time;
	}

	/**
	 * @see javax.faces.convert.NumberConverter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		passParameters(component);
		return super.getAsString(context, component, value);
	}

	/**
	 * Convert an hh:mm input into the equivalent decimal time value,
	 * rounding UP if necessary to the next 1/10 of an hour.
	 *
	 * @param m The Matcher that has already matched the user's input. This will
	 *            have the following groups: (1) hours, (3) minutes, (4) am/pm
	 *            indicator.  An 'am' indicator is ignored; a 'pm' indicator is
	 *            only used if the hour value is 1-12 (inclusive).
	 *
	 * @return The am/pm time converted to a BigDecimal value, with the minutes
	 *         converted to a decimal fraction as used in the timecard system.
	 */
	protected BigDecimal convertTimeToDecimal(Matcher m) {
		return convertTimeToDecimal(m, true);
	}

	/**
	 * Convert an hh:mm input into the equivalent decimal time value.
	 *
	 * @param m The Matcher that has already matched the user's input. This will
	 *            have the following groups: (1) hours, (3) minutes, (4) am/pm
	 *            indicator. An 'am' indicator is ignored; a 'pm' indicator is
	 *            only used if the hour value is 1-12 (inclusive).
	 * @param roundUp ** As of rev 2.2.4899 this parameter is ignored -- times
	 *            are ALWAYS rounded up.
	 *            <p>
	 *            Previously we allowed it to act as follows: If true, the the
	 *            minutes value should be rounded up when converted to 1/10's of
	 *            an hour; false if it should be rounded down.
	 *
	 * @return The am/pm time converted to a BigDecimal value, with the minutes
	 *         converted to a decimal fraction as used in the timecard system.
	 *         The fraction to which the value is rounded is based on the
	 *         {@link #roundingType} field, typically set via a Session
	 *         attribute.
	 */
	protected BigDecimal convertTimeToDecimal(Matcher m, boolean roundUp) {
		roundUp = true; // rev 2.2.4899 - times always round up to next tenth of an hour!
		BigDecimal time = BigDecimal.ZERO;
		try {
			int hr = Integer.parseInt(m.group(1)); // hours
			if (m.group(4) != null) { // Suffix of A, AM, P, or PM
				char suffix = m.group(4).charAt(0);
				if (hr < 12) {
					if (hr > 0 && suffix == 'P') {
						hr += 12;
					}
				}
				else if (hr == 12 && suffix == 'A') {
					hr = 0;
				}
			}
			int min = 0;
			if (m.group(3) != null) { // Minutes field exists
				min = Integer.parseInt(m.group(3));
				int partMinutes = getRoundingType().getMinutes(); // e.g., 6 if rounding to 1/10th.
				if (roundUp) {
					min = (min+(partMinutes-1)) / partMinutes;
				}
				else {
					min = min / partMinutes;
				}
			}
			// min is now the integral number of 1/10ths of an hour, or 1/4ths or whatever.
			time = new BigDecimal(hr);
			if (min != 0) {
				// the 'if' avoids looking up rounding type if no fractional hour entered!
				time = time.add(new BigDecimal(min).setScale(3).divide(getRoundingType().getDivisor(), RoundingMode.HALF_UP));
			}
		}
		catch (Exception e) {
		}
		return time;
	}

	/**
	 * Round a decimal time based on the current interval.
	 *
	 * @param time The BigDecimal time value to be rounded.
	 *
	 * @return The time, rounded up to the appropriate interval based on the
	 *         current {@link #roundingType} value.
	 */
	private BigDecimal round(BigDecimal time) {
		return NumberUtils.round(time, getRoundingType(), RoundingMode.HALF_UP);
	}

	/**
	 * If attributes are set for DateTimeConverter parameters, pass them along,
	 * otherwise pass our defaults (pattern designed for TimeSheet view & edit).
	 *
	 * @param component Typically a UIComponent corresponding to an inputText
	 *            field.
	 */
	protected void passParameters(UIComponent component) {
		String pattern = (String)component.getAttributes().get("pattern");
		if (pattern == null) {
			pattern = getDefaultPattern();
		}
		super.setPattern(pattern);
		// Determine rounding type...
		// use the following if we ever want to allow overriding in JSP via f:attribute
		// HourRoundingType round = (HourRoundingType)component.getAttributes().get("round");
	}

	/**
	 * Determine the current rounding type from the Session variable, typically
	 * set by the timecard editing code.
	 *
	 * @return the current rounding type, e.g., to nearest 1/10th of an hour.
	 */
	private HourRoundingType getRoundingType() {
		if (roundingType == null) {
			HourRoundingType round = (HourRoundingType)
					SessionUtils.get(Constants.ATTR_TIME_ROUNDING_TYPE, HourRoundingType.TENTH);
			if (round != null) {
				roundingType = round;
			}
		}
		return roundingType;
	}

	/** See {@link #roundDecimal}. */
	private boolean getRoundDecimal() {
		if (roundDecimal == null) {
			roundDecimal = SessionUtils.getBoolean(Constants.ATTR_ROUND_DECIMAL_TIME, false);
		}
		return roundDecimal;
	}


	/**
	 * @return The default input and output pattern for this converter. May be
	 *         overridden by subclasses to change the default pattern.
	 */
	protected String getDefaultPattern() {
		return "#0.0#";
	}

}
