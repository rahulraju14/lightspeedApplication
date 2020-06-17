package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.PayJobDaily;
import com.lightspeedeps.test.SpringMultiTestCase;

/**
 * This class tests the TimecardService code that calculates overtime hours.
 * <p>
 * This does NOT test the determination of which rules to apply, it simply
 * applies a number of different rules to a variety of daily schedules and
 * compares the results to hand-calculated values to verify they match.
 *
 * @see com.lightspeedeps.model.OvertimeRule
 * @see com.lightspeedeps.service.PayJobService
 * @see com.lightspeedeps.service.TimecardService
 */
public class OvertimeRuleTest extends SpringMultiTestCase {

	private static final String DEFAULT_NP_RULE = "NP-NONE";
	private static final String DEFAULT_GOLD12_RULE = "GL-WK-12-2X";
	private static final String DEFAULT_GOLD14_RULE = "GL-WK-14-2X";

	// Each test case (method) consists of creating a "DailyTime" object with
	// the desired 	// times (call and wrap, plus either one or both meal out & in times), then
	// invoking a particular rule and comparing the resulting calculations of
	// breakdown times to the expected values.


	// Rule name: OT-6-15X
	// ==============================================================================

	public void testOT_6_15X_1() throws Exception {
		DailyTime dt = new DailyTime(6, 11.9);
		caseTest("OT-6-15X", DEFAULT_GOLD12_RULE, dt, 5.9, 0, 0);
	}

	public void testOT_6_15X_2() throws Exception {
		DailyTime dt = new DailyTime(6, 12);
		caseTest("OT-6-15X", DEFAULT_GOLD12_RULE, dt, 6, 0, 0);
	}

	public void testOT_6_15X_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12.1);
		caseTest("OT-6-15X", DEFAULT_GOLD12_RULE, dt, 6, 0.1, 0);
	}

	public void testOT_6_15X_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 21.5);
		caseTest("OT-6-15X", DEFAULT_GOLD12_RULE, dt, 6, 6, 3);
	}

	// ==============================================================================
	// End of OT-6-15X



	// Rule name: OT-8-15X
	// ==============================================================================

	public void testOT_8_15X_1() throws Exception {
		DailyTime dt = new DailyTime(8, 12, 12.5, 19, 19.5, 21);
		caseTest("OT-8-15X", DEFAULT_GOLD12_RULE, dt, 8, 4, 0);
	}

	public void testOT_8_15X_2() throws Exception {
		DailyTime dt = new DailyTime(6, 21);
		caseTest("OT-8-15X", DEFAULT_GOLD12_RULE, dt, 8, 4, 3);
	}

	public void testOT_8_15X_3() throws Exception {
		DailyTime dt = new DailyTime(8, 12, 12.5, 14);
		caseTest("OT-8-15X", DEFAULT_GOLD12_RULE, dt, 5.5, 0, 0);
	}


	// ==============================================================================
	// End of OT-8-15X



	// Rule name: OT-9.3-15X
	// ==============================================================================

	public void testOT_93_15X_1() throws Exception {
		DailyTime dt = new DailyTime(8, 17.2);
		caseTest("OT-9.3-15X", DEFAULT_GOLD12_RULE, dt, 9.2, 0, 0);
	}

	public void testOT_93_15X_2() throws Exception {
		DailyTime dt = new DailyTime(8, 17.3);
		caseTest("OT-9.3-15X", DEFAULT_GOLD12_RULE, dt, 9.3, 0, 0);
	}

	public void testOT_93_15X_3() throws Exception {
		DailyTime dt = new DailyTime(8, 17.4);
		caseTest("OT-9.3-15X", DEFAULT_GOLD12_RULE, dt, 9.3, 0.1, 0);
	}

	public void testOT_93_15X_4() throws Exception {
		DailyTime dt = new DailyTime(8, 16.3, 16.6, 23.3);
		caseTest("OT-9.3-15X", DEFAULT_GOLD12_RULE, dt, 9.3, 2.7, 3);
	}


	// ==============================================================================
	// End of OT-8-15X-93



	// Rule name: OT-8-15X-24
	// ==============================================================================

	public void testOT_8_15X_24_1() throws Exception {
		DailyTime dt = new DailyTime(8, 12, 12.5, 19, 19.5, 21);
		caseTest("OT-8-15X-24", DEFAULT_GOLD12_RULE, dt, 8, 4, 0);
	}

	public void testOT_8_15X_24_2() throws Exception {
		DailyTime dt = new DailyTime(18, 26);
		caseTest("OT-8-15X-24", DEFAULT_GOLD12_RULE, dt, 6, 2, 0);
	}

	public void testOT_8_15X_24_3() throws Exception {
		DailyTime dt = new DailyTime(12, 26);
		caseTest("OT-8-15X-24", DEFAULT_GOLD12_RULE, dt, 8, 4, 2);
	}


	// ==============================================================================
	// End of OT-8-15X-24



	// Rule name: OT-0-15X
	// ==============================================================================

	public void testOT_0_15X_1() throws Exception {
		DailyTime dt = new DailyTime(6, 21);
		caseTest("OT-0-15X", DEFAULT_GOLD12_RULE, dt, 0, 12, 3);
	}

	// ==============================================================================
	// End of OT-0-15X



	// Rule name: OT-12-15X
	// ==============================================================================

	public void testOT_12_15X_1() throws Exception {
		DailyTime dt = new DailyTime(8, 19.9);
		caseTest("OT-12-15X", DEFAULT_GOLD12_RULE, dt, 11.9, 0, 0);
	}

	public void testOT_12_15X_2() throws Exception {
		DailyTime dt = new DailyTime(8, 20);
		caseTest("OT-12-15X", DEFAULT_GOLD12_RULE, dt, 12, 0, 0);
	}

	public void testOT_12_15X_3() throws Exception {
		DailyTime dt = new DailyTime(8, 20.1);
		caseTest("OT-12-15X", DEFAULT_GOLD12_RULE, dt, 12, 0, 0.1);
	}

	public void testOT_12_15X_4() throws Exception {
		DailyTime dt = new DailyTime(6, 21);
		caseTest("OT-12-15X", DEFAULT_GOLD12_RULE, dt, 12, 0, 3);
	}


	// ==============================================================================
	// End of OT-12-15X



	// ==============================================================================
	// End of OT-8-15X-12-15X



	// Rule name: OT-14-15X *NEW*
	// ==============================================================================

	public void testOT_14_15X_1() throws Exception {
		DailyTime dt = new DailyTime(8, 21.9);
		caseTest("OT-14-15X", DEFAULT_GOLD14_RULE, dt, 13.9, 0, 0);
	}

	public void testOT_14_15X_2() throws Exception {
		DailyTime dt = new DailyTime(8, 22);
		caseTest("OT-14-15X", DEFAULT_GOLD14_RULE, dt, 14, 0, 0);
	}

	public void testOT_14_15X_3() throws Exception {
		DailyTime dt = new DailyTime(8, 22.1);
		caseTest("OT-14-15X", DEFAULT_GOLD14_RULE, dt, 14, 0, 0.1);
	}

	public void testOT_14_15X_4() throws Exception {
		DailyTime dt = new DailyTime(8, 23);
		caseTest("OT-14-15X", DEFAULT_GOLD14_RULE, dt, 14, 0, 1);
	}


	// ==============================================================================
	// End of OT-14-15X



	/**
	 * This should be the last test method in the class, so that the JUnit test
	 * structure will run it after all the other tests. It calls the superclass
	 * method to clean up (release) the Spring/Hibernate environment.
	 *
	 * @throws Exception
	 */
	public void testSpringTearDown() throws Exception {
		springTearDown();
	}

	// SUPPORTING METHODS -- NOT directly invoked by the JUnit test structure --

	/**
	 * Test applying a single rule-key value against a single DailyTime example,
	 * and see if the results match what's expected.
	 *
	 * @param otRuleKey The OT rule key (the rule "name").
	 * @param goldRuleKey The Gold-time rule key (the rule "name").
	 * @param dt The DailyTime entry to evaluate.
	 * @param expHours10 The expected 1.0x hours to be calculated.
	 * @param expHours15 The expected 1.5x hours to be calculated.
	 * @param expHoursGold The expected 2.0x hours to be calculated.
	 */
	private void caseTest(String otRuleKey, String goldRuleKey, DailyTime dt,
			double expHours10, double expHours15, double expHoursGold) {

		PayJobDaily pd = ServiceTestUtils.breakdownTest(otRuleKey, goldRuleKey, DEFAULT_NP_RULE, dt);

		assertTrue(message("1x", expHours10, pd.getHours10(), pd), compare(expHours10, pd.getHours10()));
		assertTrue(message("1.5x", expHours15, pd.getHours15(), pd), compare(expHours15, pd.getHours15()));
		assertTrue(message("2x", expHoursGold, pd.getHoursCust1(), pd), compare(expHoursGold, pd.getHoursCust1()));

	}

	private String message(String type, double expected, BigDecimal actual, PayJobDaily pd) {
		return type + " hrs: expected " + expected + ", calc'd "	+ actual +
				" (1x=" + pd.getHours10() +
				", 1.5x=" + pd.getHours15() +
				", 1x+10%=" + pd.getHours10Np1() +
				", 1.5x+10%=" + pd.getHours15Np1() +
				", 1x+20%=" + pd.getHours10Np2() +
				", 1.5x+20%=" + pd.getHours15Np2() + ")";
	}

	private boolean compare(double expected, BigDecimal actual) {
		if (actual == null) {
			return expected == 0;
		}
		return new BigDecimal(expected).setScale(2, RoundingMode.HALF_DOWN).compareTo(actual) == 0;
	}

}
