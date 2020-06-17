/**
 * File: ScriptPagesTest.java
 */
package com.lightspeedeps.test.script;

import junit.framework.TestCase;

import com.lightspeedeps.importer.ImportPdf;

/**
 * This class tests various Script Import processes. The existing test(s) are:
 * <ul>
 * <li>Header matching - tests the regular expressions that are used to
 * determine whether the first line on a page is a page header or not.
 * </ul>
 */
public class ScriptImportTest extends TestCase {

	private int success = 0;
	/**
	 * Test our code that identifies page headings from an import Script.
	 */
	public void testScriptHeaderMatching() throws Exception {

		// Lots of tests for expected false cases - NOT valid page headers
		testOne("", false);
		testOne("123 just some text", false);
		testOne("just some text1", false); // needs space before digit
		testOne("just some text12", false); // needs space before digit
		testOne("just some text1.5", false); // needs space before digit

		testOne("1234", false);  // can't have 4 digits
		testOne("1234.1", false); // can't have 4 digits
		testOne("12. 41", false); // can't have embedded blank
		testOne("2.193", false); // decimal part can't have 3 digits
		testOne("321Z-4219A", false); // can't have 4 digits
		testOne("3215-19A", false); // can't have 4 digits
		testOne("1-", false); // invalid trailing data
		testOne("just some text 12", false); // needs TWO blanks before numbers to be valid

		// Lots of tests for expected true cases - VALID page headers
		testOne("1", true);
		testOne("32", true);
		testOne("456", true);
		testOne("just some text  456", true);
		testOne("78.", true);
		testOne("9.1", true);
		testOne("just some text  9.1", true);
		testOne("2.19", true);
		testOne("More random text - and symbols,$*@# - U.S. too.  2.19", true);
		testOne("32.19", true);
		testOne("32-42A", true);
		testOne("just some text just some text  32-42A", true);
		testOne("97C-21A", true); // no check for range in right order
		testOne("32B-4", true);
		testOne("3-4", true);
		testOne("321Z-421A", true);

		System.out.println(success + " successful tests completed.");

	}

	private void testOne(String str, boolean result) {
		assertEquals("input=`"+str+"` ", result, ImportPdf.testHeaderMatch(str));
		success++;
	}

}
