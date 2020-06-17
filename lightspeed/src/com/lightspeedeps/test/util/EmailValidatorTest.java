package com.lightspeedeps.test.util;

import com.lightspeedeps.web.validator.EmailValidator;

import junit.framework.TestCase;

public class EmailValidatorTest extends TestCase {

	private int success = 0;

	public void testEmailValidator() throws Exception {

		// tests for EmailValidator.isValidEmail()

		// false test cases
		emailTestOne( null, false ); // null should fail nicely
		emailTestOne( "@b.com", false ); // missing name (to left of "@")
		emailTestOne( "name@abc", false ); // missing period in domain name
		emailTestOne( "name@a-.tv", false ); // trailing hyphen in domain label
		emailTestOne( "name@-za.net", false ); // leading hyphen in domain label
		emailTestOne( "some blank@za.net", false ); // embedded blank in name
		emailTestOne( "name@a blank.net", false ); // embedded blank in domain
		emailTestOne( "name@a+bc.net", false ); // bad character in domain name
		emailTestOne( "name@n/ot.net", false ); // bad character in domain name
		emailTestOne( "name@abc.d,ef.net", false ); // bad character in domain name
		emailTestOne( "name@a-z.1a", false ); // tld contains numeric
		emailTestOne( "name@a-z1.a-c", false ); // tld contains hyphen
		emailTestOne( "name@a-z1..com", false ); // double period is invalid
		emailTestOne( "name..other@abc.com", false ); // double period is invalid

		// domain label too long (64):
		emailTestOne( "name@a234567890123456789012345678901234567890123456789012345678901234.com", false );

		// true test cases
		emailTestOne( "a@b.c", true ); // minimal valid
		emailTestOne( "any@typical.com", true );
		emailTestOne( "any-other.name@typical.longer.com", true );
		emailTestOne( "n1z3-34@my-name.other-56name.more2.3.4four.us", true ); // numerics & hyphens valid

		emailTestOne( "longest-valid-domain-label@a23456789012345678901234567890123456789012345678901234567890123.com", true );

		System.out.println(success + " successful tests completed.");
	}

	private void emailTestOne(String str, boolean result) {

		assertEquals("input=`"+str+"` ", result, EmailValidator.isValidEmail(str));
		success++;
	}

}
