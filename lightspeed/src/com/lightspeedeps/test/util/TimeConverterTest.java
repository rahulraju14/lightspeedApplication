//	File Name:	TimeConverterTest.java
package com.lightspeedeps.test.util;

import javax.faces.convert.Converter;

import com.lightspeedeps.web.converter.ElapsedTimeConverter;
import com.lightspeedeps.web.converter.SecondsConverter;

import junit.framework.TestCase;

/**
 * A class for JUnit testing of JSF time converter methods.
 */
public class TimeConverterTest extends TestCase {

	Converter converter;

	/**
	 * Tests MinutesConverter.getAsObject() & getAsString().
	 * @throws Exception
	 */
	public void testMinutesConverterInput() throws Exception {

		converter = new ElapsedTimeConverter();

		// check illegal values for rejection
		testOneConversion( "x", -1, "#:##" );
		testOneConversion( ":", -1, "#:##" );
		testOneConversion( "1:1:1", -1, "#:##" );
		// check various legal input formats
		testOneConversion( "", null, "" );
		testOneConversion( "1", 1, "0:01" );
		testOneConversion( "59", 59, "0:59" );
		testOneConversion( ":59", 59, "0:59" );
		testOneConversion( ":123", 123, "2:03" );
		testOneConversion( "0:10", 10, "0:10" );
		testOneConversion( "1:23", 83, "1:23" );
		testOneConversion( "1:234", 294, "4:54" );
		testOneConversion( "2:5", 125, "2:05" );
		testOneConversion( "3:15", 195, "3:15" );

	}

	/**
	 * Tests SecondsConverter.getAsObject() & getAsString().
	 * @throws Exception
	 */
	public void testSecondsConverterInput() throws Exception {

		converter = new SecondsConverter();

		// check illegal values for rejection
		testOneConversion( "x", -1, "#:##:##" );
		testOneConversion( ":", -1, "#:##:##" );
		testOneConversion( "1:1:1:1", -1, "#:##:##" );
		// check various legal input formats
		testOneConversion( "", 0, "0:00:00" );
		testOneConversion( "1", 1, "0:00:01" );
		testOneConversion( "59", 59, "0:00:59" );
		testOneConversion( ":59", 59, "0:00:59" );
		testOneConversion( ":123", 123, "0:02:03" );
		testOneConversion( "0:10", 10, "0:00:10" );
		testOneConversion( "1:23", 83, "0:01:23" );
		testOneConversion( "1:234", 294, "0:04:54" );
		testOneConversion( "2:5", 125, "0:02:05" );
		testOneConversion( "3:15", 195, "0:03:15" );
		testOneConversion( "1:1:1", 3661, "1:01:01" );
		testOneConversion( "1:01:01", 3661, "1:01:01" );
		testOneConversion( "2:21:31", 8491, "2:21:31" );
		testOneConversion( "2:61:31", 10891, "3:01:31" );
		testOneConversion( "2:61:61", 10921, "3:02:01" );

	}

	/**
	 * Each test converts the first argument to a number of minutes, which
	 * should match the second argument; then converts that back to a String,
	 * which should match the third argument.
	 *
	 * @param input Any String which is a valid input format for our
	 *            MinutesConverter, including "mm", ":mm", or "h:mm", where 'mm'
	 *            may be one or more digits.
	 * @param minutes The total number of minutes that we expect the input
	 *            String to convert to.
	 * @param output The output we expect (e.g., for a report display) when the
	 *            'minutes' value is converted to a String.
	 */
	private void testOneConversion(String input, Integer minutes, String output) {

		assertEquals("input: `" + input + "`; ", minutes,
				converter.getAsObject(null, null, input));

		assertEquals("input: " + minutes + "; ", output,
				converter.getAsString(null, null, (Integer)minutes) );

	}
}
