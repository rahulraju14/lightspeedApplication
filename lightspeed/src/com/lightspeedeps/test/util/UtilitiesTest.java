//	File Name:	UtilitiesTest.java
package com.lightspeedeps.test.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.report.CallSheetUtils;
import com.lightspeedeps.web.login.PasswordResetBean;

import junit.framework.TestCase;

public class UtilitiesTest extends TestCase {

	/**
	 * Tests for UtilHelper.stringFromList().
	 * @throws Exception
	 */
	public void testStringFromList() throws Exception {
		// tests for UtilHelper.stringFromList()
		List<String> stringList = new ArrayList<String>();

		stringFromListTest( null, "" );
		stringFromListTest( stringList, "");

		stringList.add("1");
		stringFromListTest( stringList, "1");

		stringList.add("2");
		stringFromListTest( stringList, "1, 2");

		stringList.add("5A");
		stringFromListTest( stringList, "1, 2, 5A");

		stringList.add("3");
		stringFromListTest( stringList, "1, 2, 5A, 3");

	}

	private void stringFromListTest(List<? extends Object> list, String result) {
		assertEquals("", result, StringUtils.getStringFromList(list));
	}

	public void testCreatePassword() throws Exception {
		// try some password creation
		for (int count = 0; count < 50; count++ ) {
			generatePassword();
		}
	}

	private void generatePassword() {
		String pw = PasswordResetBean.createPassword();
		System.out.println(pw);
		assertEquals("Unexpected length", 10, pw.length());
		assertEquals("Unexpected blank", -1, pw.indexOf(" "));
		assertNotSame("Two passwords in a row were equal!", pw, PasswordResetBean.createPassword());
	}

	public void testCleanFilename() {
		cleanFilenameTest("","_");
		cleanFilenameTest(" ","_");
		cleanFilenameTest("abc","abc");
		cleanFilenameTest("a b c","a b c");
		cleanFilenameTest("a, b c","a_ b c");
		cleanFilenameTest("a/b","a_b");
		cleanFilenameTest("a<>:\"/\\|?*b","a_________b");
		cleanFilenameTest(".","_");
		cleanFilenameTest("a.b.","a.b.");
		cleanFilenameTest(".ab","_ab");
		cleanFilenameTest(" ab","ab");
		cleanFilenameTest("ab ","ab");
		cleanFilenameTest(" ab ","ab");
	}

	private void cleanFilenameTest(String in, String out) {
		assertEquals("unexpected filename result", out, StringUtils.cleanFilename(in));
	}

	Callsheet cs1, cs2;
	int callTest;
	/**
	 * This method tests the CallSheetUtils.pickBetterCallsheet method with
	 * numerous combinations of callsheet times. It is designed to exercise all
	 * the "rules" used by that method for deciding which callsheet is "better"
	 * as the default callsheet to be displayed for a given user.
	 * <p>
	 * For these tests, we use a null user value, so this code does NOT exercise
	 * the "findCallTime()" method, which extracts a specific user's calltime
	 * from a callsheet.
	 */
	public void testPickBetterCallsheet() {
		callTest = 0;
		cs1 = new Callsheet();
		cs2 = new Callsheet();
		cs1.setId(1);	// set different ids to distinguish objects
		cs2.setId(2);
		cs1.setUnitNumber(1);
		cs2.setUnitNumber(2);

		Calendar now = Calendar.getInstance();
		Date date1, date2;
		date1 = now.getTime();
		date2 = now.getTime();

		pickBetterTest(null, date2, cs2);

		// equal dates prefers lower unit number
		pickBetterTest(date1, date2, cs1);

		cs1.setUnitNumber(3);	// now cs2 should be preferred
		pickBetterTest(date1, date2, cs2);

		// Minimum minutes used test
		date1 = calAdjust(now, Calendar.MINUTE, 3-Constants.CS_MINIMUM_MINUTES_USED);
		date2 = calAdjust(now, Calendar.MINUTE, 1);
		pickBetterTest(date1, date2, cs1);
		pickBetterTest(date2, date1, cs2);

		// Both in future - use closest to current time
		date1 = calAdjust(now, Calendar.HOUR, 12);
		date2 = calAdjust(now, Calendar.HOUR, 14);
		pickBetterTest(date1, date2, cs1);
		pickBetterTest(date2, date1, cs2);

		// One is past minimum-use minutes, the other is within "preferred" hours
		date1 = calAdjust(now, Calendar.MINUTE, -Constants.CS_MINIMUM_MINUTES_USED-1);
		date2 = calAdjust(now, Calendar.HOUR, 1);
		pickBetterTest(date1, date2, cs2);
		pickBetterTest(date2, date1, cs1);

		// One is past minimum-use minutes, the other is past "preferred" hours
		date1 = calAdjust(now, Calendar.MINUTE,  -Constants.CS_MINIMUM_MINUTES_USED-1);
		date2 = calAdjust(now, Calendar.HOUR, Constants.CS_WITHIN_HOURS_PREFERRED+1);
		pickBetterTest(date1, date2, cs1);
		pickBetterTest(date2, date1, cs2);

		// One is past preferred-use hours, the other is in the future (but not as close as first)
		date1 = calAdjust(now, Calendar.HOUR, -Constants.CS_PREFERRED_HOURS_USED-1);
		date2 = calAdjust(now, Calendar.HOUR, Constants.CS_PREFERRED_HOURS_USED+2);
		pickBetterTest(date1, date2, cs2);
		pickBetterTest(date2, date1, cs1);

	}

	/**
	 * Run one test of the pickBetterCallsheet() method by setting the cs1
	 * callsheet's date to 'dateA' and the cs2 callsheet's date to 'dateB', and
	 * testing for the returned callsheet being equal to 'result'.
	 * <p>
	 * Since a null User value is passed to pickBetterCallsheet(), it will
	 * always use the crew-call time in the callsheet for its comparisons.
	 */
	private void pickBetterTest(Date dateA, Date dateB, Callsheet result) {
		Callsheet cs;
		callTest++; // in case of failure, makes it a bit easier to figure out which case failed.
		if (dateA == null) { // special case test
			cs = CallSheetUtils.pickBetterCallsheet(null, cs2, null);
		}
		else {
			cs1.setCallTime(dateA);
			cs2.setCallTime(dateB);
			cs = CallSheetUtils.pickBetterCallsheet(cs1, cs2, null);
		}
		assertEquals("Test #" + callTest + ": comparing date " + dateA + " to date " + dateB, result, cs);
	}

	/** Return the Date value resulting from adding the given 'amount' to
	 * the specified 'field' of the given Calendar. */
	private Date calAdjust(Calendar cal, int field, int amount) {
		Calendar temp = (Calendar)cal.clone();
		temp.add(field, amount);
		return temp.getTime();
	}

}
