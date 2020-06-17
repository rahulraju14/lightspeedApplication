package com.lightspeedeps.util.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;

/**
 * Static Date/Time utility class
 */
public class CalendarUtils {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CalendarUtils.class);

//	private final static String SQL_DATE_PATTERN = "yyyy-MM-dd";
//	private final static String SQL_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";
//	private static DateFormat SQL_DATE_FORMAT = new SimpleDateFormat(SQL_DATE_PATTERN);
//	private static DateFormat SQL_DATE_TIME_FORMAT = new SimpleDateFormat(SQL_DATE_TIME_PATTERN);

	/** Inhibit instances **/
	private CalendarUtils() {	}

	public static Calendar getInstance(final Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * Format the date according to the pattern param
	 *
	 * @param date The date to format. If null, returns an empty String ("").
	 * @param pattern The pattern to use (see SimpleDateFormat)
	 * @return The formatted date, or an empty String if 'date' is null.
	 */
	public static String formatDate(Date date, String pattern) {
		if (date == null) {
			return "";
		}
		return DateFormatUtils.format(date, pattern);
	}


	/**
	 * Try to parse the date with each of the pattern args
	 *
	 * @param date The date string to parse
	 * @param patterns (varargs) a list of string patterns to use (see SimpleDateFormat)
	 * @return A valid date if succesful
	 * @throws ParseException when none of the patterns was able to produce a valid date
	 */
	public static Date parseDate(String date, String... patterns) throws ParseException {
		return DateUtils.parseDate(date, patterns);
	}

	/**
	 * Try to parse a date string with the given pattern.
	 *
	 * @param strDate The textual date to be parsed.
	 * @param pattern The pattern to use, following java.util.Date conventions.
	 * @return The equivalent date value, or null if the parse fails.
	 */
	public static Date parseDate(String strDate, String pattern) {
		Date date = null;
		try {
			date = DateUtils.parseDate(strDate, new String[] {pattern});
		}
		catch (ParseException e) {
		}
		return date;
	}

	/**
	 * @return A Date object set to the current date, with time of day set to
	 *         midnight (12:00am)
	 */
	public static Date todaysDate() {
		Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		CalendarUtils.setStartOfDay(cal);
		return cal.getTime();
	}

	/**
	 * Calculate the number of 24-hour periods (days) between two given
	 * date/time values.
	 *
	 * @param from The start date of the range.
	 * @param until The end date of the range.
	 * @return A signed value indicating the number of 24-hour periods between
	 *         the two given date/time values, rounded down. So 2pm today until
	 *         1pm tomorrow is zero, but 2pm today until 3pm tomorrow is one.
	 *         The value will be negative if "until" is more than 24 hours before
	 *         the "from" date/time.
	 */
	public static int durationInDays(Date from, Date until) {
		long durInMillis = until.getTime() - from.getTime();
		return (int)Math.floor(durInMillis / 1000 / 60 / 60 / 24);
	}

	/**
	 * Calculate the number of 24-hour periods (days) between two given
	 * date/time values.
	 *
	 * @param from The start date of the range as a String.
	 * @param until The end date of the range as a String.
	 * @param pattern The date/time pattern which should be used to parse both
	 *            the from and until parameters.
	 * @return A signed value indicating the number of 24-hour periods between
	 *         the two given date/time values, rounded down. So 2pm today until
	 *         1pm tomorrow is zero, but 2pm today until 3pm tomorrow is one.
	 */
	public static int durationInDays(String from, String until, String pattern) throws ParseException {
		return durationInDays(parseDate(from, pattern), parseDate(until, pattern));
	}



	/**
	 * Set the calendar to the end of the year
	 * @param calendar
	 */
	public static void setEndOfYear(Calendar calendar) {
		calendar.set(Calendar.MONTH, 11);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		setEndOfDay(calendar);
	}


	/**
	 * Set the calendar to the start of the year
	 * @param calendar
	 */
	public static void setStartOfYear(Calendar calendar) {
		calendar.set(Calendar.MONTH, 0);
		setStartOfMonth(calendar);
	}



	/**
	 * Set the calendar to the end of month, and to the
	 * last millisecond of that day, 11:59:59.999pm.
	 * @param calendar
	 */
	public static void setEndOfMonth(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		setEndOfDay(calendar);
	}



	/**
	 * Set the calendar object to the start of the month,
	 * and to the start of the day, 12:00am.
	 * @param calendar
	 */
	public static void setStartOfMonth(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		setStartOfDay(calendar);
	}


	/**
	 * Set the calendar object to end of the week -- to the last
	 * day of the week, and the last millisecond of the day
	 * (11:59:59.999pm).
	 * @param calendar
	 */
	public static void setEndOfWeek(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek() + 6);
		setEndOfDay(calendar);
	}



	/**
	 * Set the calendar to the start of the week -- to the
	 * first day of the week, and to the start of the day, 12:00am
	 * @param calendar
	 */
	public static void setStartOfWeek(Calendar calendar) {
		calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
		setStartOfDay(calendar);
	}


	/**
	 * Set the Calendar object's time to the end of the day, to the nearest
	 * millisecond: 23:59:59.999.
	 *
	 * @param calendar Input and output parameter: it's time of day is set to
	 *            11:59:59.999pm; the date is not changed.
	 */
	public static void setEndOfDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		setEndOfHour(calendar);
	}

	/**
	 * Set the Calendar object's time to the start of day (12:00:00am).
	 *
	 * @param calendar Input and output parameter: it's time of day is set to
	 *            12:00:00am; the date is not changed.
	 */
	public static void setStartOfDay(Calendar calendar) {
		calendar.set(Calendar.HOUR_OF_DAY, 00);
		setStartOfHour(calendar);
	}

	/**
	 * Set calendar to start of hour
	 * @param calendar
	 */
	public static void setStartOfHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 00);
		calendar.set(Calendar.SECOND, 00);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.getTime(); //force compute
	}


	/**
	 * Set calendar to end of hour
	 *
	 * @param calendar
	 */
	public static void setEndOfHour(Calendar calendar) {
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		calendar.getTime(); //force compute
	}


	/**
	 * Test if test date is within start/end date range
	 *
	 */
	public static boolean dateWithinRange(Date testDate, Date start, Date end) {
		long lTest = testDate.getTime();
		long lStart = start.getTime();
		long lEnd = end.getTime();
		return lTest >= lStart && lTest <= lEnd;
	}

	/**
	 * Test if test date range spans the start/end range
	 * @param testDate1
	 * @param testDate2
	 * @param start
	 * @param end
	 * @return True if testDate1 is the same as or earlier than 'start' AND testDate2
	 * is the same as or later than 'end'.
	 */
	public static boolean datesSpanRange(Date testDate1, Date testDate2, Date start, Date end) {
		long lTest1 = testDate1.getTime();
		long lTest2 = testDate2.getTime();
		long lStart = start.getTime();
		long lEnd = end.getTime();
		return lTest1 <= lStart && lTest2 >= lEnd;
	}

	/**
	 * Compare two dates, allowing for null values for either one, and return
	 * the standard 1/0/-1 value for comparisons.
	 * @param date1 The first date.
	 * @param date2 The second date.
	 * @return
	 * <p>date1 == null and date2 == null: 0
	 * <p>date1 == null and date2 != null: -1
	 * <p>date1 != null and date2 == null: 1
	 * <p>date1 != null and date2 != null: date1.compareTo(date2).
	 */
	public static int compare( Date date1, Date date2) {
		int ret = 0;
		if (date1 == null) {
			if (date2 == null) {
				//ret = 0;
			}
			else {
				ret = -1;
			}
		}
		else {
			if (date2 == null) {
				ret = 1;
			}
			else {
				ret = date1.compareTo(date2);
			}
		}
		return ret;
	}

	/**
	 * Return the number of years that pass until the given date
	 * @param calendar
	 * @param date2
	 * @return The number of years between the 'calendar' year and
	 * the 'date2' year; -1 if date2 is an earlier date than 'calendar'.
	 */
	public static int yearsUntilDate(Calendar calendar, Date date2) {
		int i = -1;
		while(calendar.getTimeInMillis() < date2.getTime()) {
			i++;
			calendar.add(Calendar.YEAR, 1);
		}
		return i;
	}

	public static String dateFormatYYYYMMDD(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("yyyy-MM-dd");
		return sdf.format(date);
	}

//	/**
//	 * Returns an SQL-compatible date (without time) comparison string.
//	 *
//	 * @param date The Calendar whose date is needed in SQL format.
//	 * @return A String in the format 'yyyy-MM-dd', including the enclosing
//	 *         primes.
//	 */
//	public static String sqlDate(Calendar date) {
//		String s = "'" + SQL_DATE_FORMAT.format(date.getTime()) + "'";
//		return s;
//	}

//	/**
//	 * Returns an SQL-compatible date and time comparison string.
//	 *
//	 * @param date The Calendar whose timestamp is needed in SQL format.
//	 * @return A String in the format 'yyyy-MM-dd HH:mm', including the enclosing
//	 *         primes.
//	 */
//	public static String sqlDateTime(Calendar date) {
//		String s = "'" + SQL_DATE_TIME_FORMAT.format(date.getTime()) + "'";
//		return s;
//	}

//	public static String sayDayName(Date d) {
//		DateFormat f = new SimpleDateFormat("EEEE");
//		try {
//			return f.format(d);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//			return "";
//		}
//	}

	/** Use sameTimeAs(timeSource, dateSource); -- just reverse the arguments. */
//	@Deprecated
//	public static Date setDateForDB(Date date1, Date date2) {
//		return sameTimeAs(date2, date1);
//	}

	/**
	 * Create a Date value which has the same year-month-day as "dateSource",
	 * but has the same time of day as "timeSource".  In other words, make
	 * the "dateSource" have the "sameTimeAs" the "timeSource".
	 * @param timeSource Source for time-of-day (hour/min/second)
	 * @param dateSource Source for the day/month/year
	 * @return A Date object with attributes as described above.
	 */
	public static Date sameTimeAs(Date timeSource, Date dateSource) {
		if (timeSource == null || dateSource == null) {
			return null;
		}
		Calendar calDate = new GregorianCalendar();
		Calendar calTime = new GregorianCalendar();
		calDate.setTime(dateSource);
		calTime.setTime(timeSource);
		// copy the time of day from "calTime" (="timeSource") into "calDate"
		calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
		calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
		calDate.set(Calendar.SECOND, calTime.get(Calendar.SECOND));
		return calDate.getTime();
	}

	/**
	 * Create a Date value which has the same time-of-day as "newDate",
	 * but has the same month/day/year as "oldDate".  In other words, make
	 * the "newDate" have the "sameDateAs" the "oldDate" without changing its time.
	 * @param oldDate Source for date (year/month/day)
	 * @param newDate Source for the time of day
	 * @return A Date object with attributes as described above.
	 */
	public static Date sameDateAs(Date oldDate, Date newDate) {
		if (oldDate == null || newDate == null) {
			return null;
		}
		Calendar calNew = new GregorianCalendar();
		Calendar calOld = new GregorianCalendar();
		calNew.setTime(newDate);
		calOld.setTime(oldDate);
		// copy the date from "calOld" (="oldDate") into "calNew"
		calNew.set(Calendar.YEAR, calOld.get(Calendar.YEAR));
		calNew.set(Calendar.DAY_OF_YEAR, calOld.get(Calendar.DAY_OF_YEAR));
		return calNew.getTime();
	}

	public static Date sameDateAs(Date dateSource, Date timeSource, TimeZone tz) {
		if (dateSource == null || timeSource == null) {
			return null;
		}
		//log.debug(timeSource);
		// Use "pure" calendars that aren't affected by daylight savings
		Calendar calNew = new GregorianCalendar(tz);
		Calendar calDateSource = new GregorianCalendar(tz);
		calNew.setTime(timeSource);
		calDateSource.setTime(dateSource);
		// copy the date from "calDateSource" (="dateSource") into "calNew"
		calNew.set(Calendar.DAY_OF_YEAR, calDateSource.get(Calendar.DAY_OF_YEAR));
		calNew.set(Calendar.YEAR, calDateSource.get(Calendar.YEAR));
		//log.debug(calNew.getTime());
		return calNew.getTime();
	}

	/**
	 * Returns a Date object whose date is the same as the date supplied, and
	 * with the time of day set to the specified hours and minutes. Seconds and
	 * milliseconds are set to zero.
	 *
	 * @param date A Date object with the desired date for the returned value.
	 * @param hours The hours within the day (0-23) to set the time to.
	 * @param minutes The minutes within the hour (0-59) to set the time to.
	 * @return A Date object with the specified time of day and given date.
	 */
	public static Date setTime(Date date, int hours, int minutes) {
		if (date == null ) {
			return null;
		}
		Calendar calNew = new GregorianCalendar();
		calNew.setTime(date);
		// create a time value with date from "date" and time of day using
		// the hours & minutes parameters passed...
		calNew.set(Calendar.HOUR_OF_DAY, hours);
		calNew.set(Calendar.MINUTE, minutes);
		calNew.set(Calendar.SECOND, 0);
		calNew.set(Calendar.MILLISECOND, 0);
		//log.debug(calNew);
		return calNew.getTime();
	}


	public static String getPreviousWeekDateByDay(int firstDateDay,int currentDateDay) {
		   Calendar calc = Calendar.getInstance();
		   int difference = currentDateDay - firstDateDay;
		   if (currentDateDay>firstDateDay) {
			   calc.add(Calendar.DATE, -difference);
		   }
		   else {
			   calc.add(Calendar.DATE, difference);
		   }
		   String requiredDate = dateFormatYYYYMMDD(calc.getTime());
		   //log.debug("Required Date"+requiredDate);
		   return requiredDate;
	}

	/**
	 * Return the day number (1-7) corresponding to the given Date.
	 * @param date
	 */
	public static int getDayNumber(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return getDayNumber(cal);
	}

	/**
	 * Return the day number (1-7) corresponding to the
	 * date in the given Calendar.
	 * @param cal
	 */
	public static int getDayNumber(Calendar cal) {
		return cal.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * Convert a Date value to the equivalent decimal time of day in tenths of
	 * an hour.
	 *
	 * @param date The Date to be converted; only the time of day is used, not
	 *            the calendar day.
	 * @param roundUp ** As of rev 2.2.4899 this parameter is ignored -- times
	 *            are ALWAYS rounded up.
	 *            <p>
	 *            Previously we allowed it to act as follows: If true, the time
	 *            is rounded up to the nearest tenth of an hour. If false, the
	 *            time is rounded down to the nearest tenth of an hour.
	 * @return The time of day expressed as a value from 0 to 23.9, with a
	 *         maximum of one decimal place, or null if 'date' is null.
	 */
	@SuppressWarnings("deprecation")
	public static BigDecimal convertTimeToDecimal(Date date, boolean roundUp) {
		roundUp = true; // rev 2.2.4899 - times always round up to next tenth of an hour!
		BigDecimal hrd = null;
		if (date != null) {
			int hr = date.getHours();
			int min = date.getMinutes();
			if (roundUp) {
				min = (min+5) / 6;
			}
			else {
				min = min / 6;
			}
			// min is now the integral number of 1/10ths of an hour
			BigDecimal mind = new BigDecimal(min);
			hrd = new BigDecimal(hr).add(mind.divide(Constants.DECIMAL_10));
			//log.debug("date=" + date + ", hours=" + hrd);
		}
		return hrd;
	}

	/**
	 * Convert a Date value to the equivalent decimal time of day, with the
	 * specified number of decimal digits precision.
	 *
	 * @param date The Date to be converted; only the time of day is used, not
	 *            the calendar day.
	 * @param scale The number of decimal digits to preserve.
	 * @return The time of day expressed as a value from 0 to 23.999..., or null
	 *         if 'date' is null.
	 */
	@SuppressWarnings("deprecation")
	public static BigDecimal convertTimeToDecimal(Date date, int scale) {
		BigDecimal hrd = null;
		if (date != null) {
			int hr = date.getHours();
			int min = date.getMinutes();
			hrd = new BigDecimal(hr).add(
					new BigDecimal(min).divide(Constants.MINUTES_PER_HOUR, scale, RoundingMode.HALF_UP));
			//log.debug("date=" + date + ", hours=" + hrd);
		}
		return hrd;
	}

	@SuppressWarnings("deprecation")
	public static Date convertDecimalToTime(BigDecimal hours) {
		Date date = null;
		if (hours != null) {
			date = new Date();
			date.setHours(hours.intValue());
			hours = hours.multiply(Constants.MINUTES_PER_HOUR);
			date.setMinutes(hours.intValue() % 60);
			date.setSeconds(0);
		}
		//log.debug("date=" + date + ", hours=" + hours);
		return date;
	}

}
