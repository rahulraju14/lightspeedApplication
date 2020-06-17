package com.lightspeedeps.test.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.object.LatitudeLongitude;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;


/**
 * Test the Sunrise/Sunset calculations class. Note that this
 * does NOT test our zip-code lookup code.
 */
public class SunriseSunsetTest extends TestCase {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SunriseSunsetTest.class);

	/**
	 * Run a JUnit test.
	 */
	public void testSunriseSunset() {

		Calendar cal = Calendar.getInstance();
		boolean dst = true;

		// Calculate sunrise and sunset time for Philadelphia, PA, USA for today
		doCity("Philadelphia, PA", new LatitudeLongitude(39.9561, -75.1645), cal, "US/Eastern", dst);

		// Calculate sunrise and sunset time for Los Angeles, CA, USA for today
		doCity("Los Angeles, CA", new LatitudeLongitude(33.93, -118.40), cal, "US/Pacific", dst);

		doCity("Los Angeles, 90032", new LatitudeLongitude(34.08, -118.17), cal, "US/Pacific", dst);

		doCity("Santa Maria, 93454", new LatitudeLongitude(34.9545, -120.4324), cal, "US/Pacific", dst);

	}

	private static void doCity(String place, LatitudeLongitude latlong, Calendar cal, String tz, boolean dst) {
		doCity(place, latlong, cal, TimeZone.getTimeZone(tz));
	}

	private static void doCity(String place, LatitudeLongitude latlong, Calendar cal, TimeZone tz) {
		System.out.println("\n" + place);
		//System.out.println(" (" + latlong.toString() + ")");

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss zzz");
		Location location = new Location(latlong.getLatitude(), latlong.getLongitude());

		// create calculator
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, tz);

		// Get sunrise from calculator
		Calendar result = calculator.getOfficialSunriseCalendarForDate(cal);
		System.out.println("Sunrise= " + sdf.format(result.getTime()));

		// Get sunset from calculator
		result = calculator.getOfficialSunsetCalendarForDate(cal);
		System.out.println("Sunset = " + sdf.format(result.getTime()));
	}

}
