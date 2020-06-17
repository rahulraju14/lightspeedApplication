package com.lightspeedeps.test.util;

import com.lightspeedeps.web.validator.PhoneNumberValidator;

import junit.framework.TestCase;

public class PhoneNumberValidatorTest extends TestCase {

	private int success = 0;

	public void testPhoneNumberValidator() throws Exception {

		// tests for PhoneNumberValidator.isValid()

		// false test cases
		testOne( null, false ); // null should fail nicely
		testOne( "", false ); // empty
		testOne( "12345", false ); // too short
		testOne( "02345", false ); // too short
		testOne( "+23451", false ); // too short
		testOne( "22345678901", false ); // too long
		testOne( "223x4567890", false ); // alpha
		testOne( "2234567890x", false ); // alpha
		testOne( "223456789a", false ); // alpha
		testOne( " 2234567890x", false ); // alpha
		testOne( "2234567890 ", true ); // trailing blank is OK
		testOne( "(223(4567890", false ); // wrong paren
		testOne( "223-45-67890 ", false ); // misplaced hyphen
		testOne( "-2234567890", false ); // misplaced hyphen
		testOne( "2234567.890", false ); // misplaced period

		// true test cases
		testOne( "2234567890", true );
		testOne( "(323)4567890", true );
		testOne( "(423)456-7890", true );
		testOne( "1(523)4567890", true );
		testOne( "1-(623)4567890", true );
		testOne( "1-723-456-7890", true );
		testOne( "17234567890", true );
		testOne( "1-7234567890", true );
		testOne( "1 723 456 7890", true );
		testOne( "1.723-456 7890", true );

		System.out.println(success + " successful tests completed.");
	}

	private void testOne(String str, boolean result) {
		assertEquals("input=`"+str+"` ", result, PhoneNumberValidator.isValid(str));
		success++;
	}

}
