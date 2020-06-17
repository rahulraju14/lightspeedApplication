/*
 * File: DecimalTimeConverterDown.java
 */
package com.lightspeedeps.web.converter;

import java.math.BigDecimal;
import java.util.regex.Matcher;

import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class is designed to allow hh:mm style date entry into fields that are
 * stored as decimal time. The output style is always in decimal. The hh:mm
 * value is rounded DOWN to the earlier 1/10 of an hour increment, if necessary.
 * (Some productions use 1/4 hour increments; see HourRoundingType usage.)
 * <p/>
 * It allows a wide variety of time inputs, such as 'a' for am and 'p' for pm,
 * or omitting am/pm entirely, using either 12- or 24-hour styles.
 */
@FacesConverter(value="lightspeed.DecimalTimeConverterDown")
public class DecimalTimeConverterDown extends DecimalTimeConverter {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DecimalTimeConverterDown.class);

	/**
	 * Convert an hh:mm input into the equivalent decimal time value,
	 * rounding DOWN if necessary to the next 1/10 (or other fraction) of an hour.
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
