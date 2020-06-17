package com.lightspeedeps.test.util;

import com.lightspeedeps.model.Address;

import junit.framework.TestCase;

public class ZipCodeValidatorTest extends TestCase {

	private int success = 0;
	private Address addr;

	public void testZipCodeValidator() throws Exception {

		addr = new Address();

		// tests for Address.isValidZip()
		addr.setState("CA");

		// false test cases
		testOne( null, false ); // null should fail nicely
		testOne( "", false ); // empty
		testOne( "1", false ); // too short
		testOne( "1234-", false ); // too short
		testOne( "1234-1234", false ); // too short
		testOne( "1234-12345", false ); // wrong break point
		testOne( "0234", false ); // too short
		testOne( "+2345", false ); // bad data
		testOne( "+23451", false ); // bad data
		testOne( "223456-8901", false ); // too long
		testOne( "12345-", false ); // too short
		testOne( "12345-1", false ); // too short
		testOne( "12345-123", false ); // too short
		testOne( "12345-12345", false ); // too long
		testOne( "123456", false ); // too long
		testOne( "22345x6789", false ); // alpha
		testOne( "22345 6789", false ); // alpha
		testOne( "2234a", false ); // alpha
		testOne( " 1234", false ); // alpha
		testOne( "1234 ", false ); // alpha

		// true test cases
		testOne( "00000", true );
		testOne( "12345", true );
		testOne( "67890", true );
		testOne( "99999", true );
		testOne( "12345-6789", true );
		testOne( "00000-0000", true );
		testOne( "99999-9999", true );

		System.out.println(success + " successful tests completed.");
	}

	private void testOne(String str, boolean result) {
		addr.setZip(str);
		assertEquals("input=`"+str+"` ", result, addr.isZipValid());
		success++;
	}

}
