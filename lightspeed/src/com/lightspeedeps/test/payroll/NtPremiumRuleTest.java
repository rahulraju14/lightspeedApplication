package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.PayJobDaily;
import com.lightspeedeps.test.SpringMultiTestCase;

/**
 * This class tests the TimecardService code that calculates night-premium
 * hours.
 * <p>
 * This does NOT test the determination of which rules to apply, it simply
 * applies a number of different rules to a variety of daily schedules and
 * compares the results to hand-calculated values to verify they match.
 *
 * @see com.lightspeedeps.model.NtPremiumRule
 * @see com.lightspeedeps.service.NightPremiumService
 * @see com.lightspeedeps.service.TimecardService
 */
public class NtPremiumRuleTest extends SpringMultiTestCase {

	private static final String DEFAULT_GOLD_RULE = "GL-WK-12-2X";
	private static final String DEFAULT_OVERTIME_RULE = "OT-8-15X";

	// Each test case (method) consists of creating a "DailyTime" object with
	// the desired
	// times (call and wrap, plus either one or both meal out & in times), then
	// invoking a particular rule and comparing the resulting calculations of
	// penalty
	// counts and amount to the expected values.


	// Rule name: NP-LA-STD
	// ==============================================================================

	public void testNP_LA_STD1() throws Exception {
		DailyTime dt = new DailyTime(8, 12, 12.5, 19, 19.5, 21);
		npTestOne("NP-LA-STD", dt, 8, 3, 0, 0, 1, 0, 0);
	}

	public void testNP_LA_STD2() throws Exception {
		DailyTime dt = new DailyTime(15, 21, 21.5, 24);
		npTestOne("NP-LA-STD", dt, 5, 0, 0, 3, 0.5, 0, 0);
	}

	public void testNP_LA_STD3() throws Exception {
		DailyTime dt = new DailyTime(21, 29);
		npTestOne("NP-LA-STD", dt, 0, 0, 0, 0, 0, 8, 0);
	}

	public void testNP_LA_STD4() throws Exception {		// Group A
		DailyTime dt = new DailyTime(16, 22, 22.5, 24.5);
		npTestOne("NP-LA-STD", dt, 4, 0, 0, 4, 0, 0, 0);
	}

	public void testNP_LA_STD5() throws Exception {		// Group A
		DailyTime dt = new DailyTime(17, 23, 23.5, 29);
		npTestOne("NP-LA-STD", dt, 3, 0, 0, 5, 3.50, 0, 0);
	}

	public void testNP_LA_STD6() throws Exception {		// Group A
		DailyTime dt = new DailyTime(18, 24, 24.5, 32);
		npTestOne("NP-LA-STD", dt, 2, 0, 1.50, 6, 4, 0, 0);
	}

	public void testNP_LA_STD7() throws Exception {		// Group B
		DailyTime dt = new DailyTime(19.9, 25.9, 26.4, 28.4);
		npTestOne("NP-LA-STD", dt, 0.10, 0, 0, 7.90, 0, 0, 0); // 7.9 was in Gold, needs to be in 1.0x 10%NP
	}

	public void testNP_LA_STD8() throws Exception {		// Group B
		DailyTime dt = new DailyTime(20, 26, 26.5, 30.5);
		npTestOne("NP-LA-STD", dt, 0, 0, 0, 0, 0, 8, 2);
	}

	public void testNP_LA_STD9() throws Exception {		// Group B
		DailyTime dt = new DailyTime(20.1, 26.1, 26.6, 35);
		npTestOne("NP-LA-STD", dt, 0, 0, 2.40, 0, 0, 8, 4);
	}

	public void testNP_LA_STD10() throws Exception {	// Group C
		DailyTime dt = new DailyTime(3.9, 9.9, 10.4, 12.4);
		npTestOne("NP-LA-STD", dt, 0, 0, 0, 0, 0, 8, 0);
	}

	public void testNP_LA_STD11() throws Exception {	// Group C
		DailyTime dt = new DailyTime(4, 10, 10.5, 14);
		npTestOne("NP-LA-STD", dt, 6, 1.50, 0, 0, 0, 2.00, 0);
	}

	public void testNP_LA_STD12() throws Exception {	// Group C
		DailyTime dt = new DailyTime(4.1, 10.1, 10.6, 19); // 14.4 total hours worked
		npTestOne("NP-LA-STD", dt, 6.10, 4.0, 2.4, 0, 0, 1.9, 0); // was "6.10, 4.00, 0, 0, 0, 1.90, 0"
	}

	// ==============================================================================
	// End of NP-LA-STD


	// Rule name: NP-A-8-1-6
	// ==============================================================================
	public void testNP_A816_1() throws Exception {
		DailyTime dt = new DailyTime(15, 21, 21.5, 24);
		npTestOne("NP-A-8-1-6", dt, 5, 0, 0, 3, 0.5, 0, 0);
	}

	public void testNP_A816_2() throws Exception {
		DailyTime dt = new DailyTime(21, 29);
		npTestOne("NP-A-8-1-6", dt, 0, 0, 0, 4, 0, 4, 0);
	}

	public void testNP_A816_3() throws Exception {
		DailyTime dt = new DailyTime(23, 31);
		npTestOne("NP-A-8-1-6", dt, 1, 0, 0, 2, 0, 5, 0);
	}

	// ==============================================================================
	// End of NP-A-8-1-6


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
	 * @param ruleKey The NtPremiumRule key (the rule "name").
	 * @param dt
	 */
	private void npTestOne(String ruleKey, DailyTime dt,
			double expHours10, double expHours15, double expHoursGold,
			double expHours10Np1, double expHours15Np1, double expHours10Np2, double expHours15Np2) {

		PayJobDaily pd = ServiceTestUtils.nightPremiumTest(ruleKey, DEFAULT_OVERTIME_RULE, DEFAULT_GOLD_RULE, dt);

		assertTrue(message("1x", expHours10, pd.getHours10(), pd), compare(expHours10, pd.getHours10()));
		assertTrue(message("1.5x", expHours15, pd.getHours15(), pd), compare(expHours15, pd.getHours15()));
		assertTrue(message("2x", expHoursGold, pd.getHoursCust1(), pd), compare(expHoursGold, pd.getHoursCust1()));
		assertTrue(message("1x+10%", expHours10Np1, pd.getHours10Np1(), pd), compare(expHours10Np1, pd.getHours10Np1()));
		assertTrue(message("1.5x+10%", expHours15Np1, pd.getHours15Np1(), pd), compare(expHours15Np1, pd.getHours15Np1()));
		assertTrue(message("1x+20%", expHours10Np2, pd.getHours10Np2(), pd), compare(expHours10Np2, pd.getHours10Np2()));
		assertTrue(message("1.5x+20%", expHours15Np2, pd.getHours15Np2(), pd), compare(expHours15Np2, pd.getHours15Np2()));

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
