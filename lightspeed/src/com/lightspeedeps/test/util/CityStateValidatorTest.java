package com.lightspeedeps.test.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.LocationUtils;

/**
 * Test the validateCityState utility method.
 */
public class CityStateValidatorTest extends SpringTestCase {

	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(CityStateValidatorTest.class);

	private int success = 0;

	public void testCityStateValidator() throws Exception {

		// Create a token and save it in "session"
		initializeAccessToken();

		// TEST cases INCLUDING Country

		// false test cases
		testOne( null, null, null, false ); // null should fail nicely
		testOne( "Burbank", null, null, false );
		testOne( "Burbank", "", null, false );
		testOne( "Burbank", "CA", null, false );
		testOne( null, "CA", null, false );
		testOne( "", "CA", null, false );
		testOne( "", "CA", "US", false );
		testOne( "No such city name", "CA", "US", false );
		testOne( "Burbank", "XY", "US", false );

		// true test cases
		testOne( "Burbank", "CA", "US", true );
		testOne( "Alameda", "CA", "US", true );
		testOne( "Wichita", "KS", "US", true );
		testOne( "Los Angeles", "CA", "US", true );
		testOne( "New York", "NY", "US", true );


		// TEST cases WITHOUT Country

		// false test cases
		testOne( null, null, false ); // null should fail nicely
		testOne( "Burbank", null, false );
		testOne( "Burbank", "", false );

		// true test cases
		testOne( "Burbank", "CA", true );
		testOne( "Wichita", "KS", true );
		testOne( "New York", "NY", true );

		System.out.println(success + " successful tests completed.");
	}

	/**
	 * Call the validateCityState with one set of parameters, including the country.
	 * @param city  The city name to check.
	 * @param state The state containing the city.
	 * @param country The country for the city/state.
	 * @param result The expected result (true or false).
	 */
	private void testOne(String city, String state, String country, boolean result) {
		boolean res = false;

		res = LocationUtils.validateCityState(city, state, country);

		assertEquals("city=`"+city+"`, state=`" + state + "`, country=" + country + "` ", result, res);

		success++;
	}

	/**
	 * Call the validateCityState with one set of parameters, without a country.
	 * @param city  The city name to check.
	 * @param state The state containing the city.
	 * @param result The expected result (true or false).
	 */
	private void testOne(String city, String state, boolean result) {
		boolean res = false;

		res = LocationUtils.validateCityState(city, state);

		assertEquals("city=`"+city+"`, state=`" + state + "` ", result, res);

		success++;
	}

}
