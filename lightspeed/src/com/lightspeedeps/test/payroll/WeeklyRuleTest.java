package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.PayJob;
import com.lightspeedeps.model.PayJobDaily;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.type.WorkZone;

/**
 * This class tests the TimecardService code that calculates time breakdown
 * for various WeeklyRule cases.
 * <p>
 * This does NOT test the determination of which rules to apply, it simply
 * applies a number of different rules to a variety of daily schedules and
 * compares the results to hand-calculated values to verify they match.
 *
 * @see com.lightspeedeps.model.WeeklyRule
 * @see com.lightspeedeps.service.PayJobService
 * @see com.lightspeedeps.service.TimecardService
 */
public class WeeklyRuleTest extends SpringMultiTestCase {

	private static final String DEFAULT_NP_RULE = "NP-NONE";

	// Each test case (method) consists of creating a "DailyTime" object with
	// the desired 	// times (call and wrap, plus either one or both meal out & in times), then
	// invoking a particular rule and comparing the resulting calculations of
	// breakdown times to the expected values.


	// Rule name: WK-STD-40
	// ==============================================================================

	public void testWK_STD_40_1() throws Exception {
		WeeklyTimecard wtc = TimecardTestUtils.makeTimecard(makeDt(6, 19));
		caseTest("WK-STD-40", "OT-8-1X-15X", "GL-STD-12-2X", wtc,
				new double[]{0,0,0, 8,4,1, 8,4,1, 8,4,1, 8,4,1, 8,4,1, 0,0,0});
	}

	public void testWK_STD_40_2() throws Exception {
		WeeklyTimecard wtc = TimecardTestUtils.makeTimecard(
				makeDt(6, 18), makeDt(6, 20), makeDt(6, 19), makeDt(6, 12), makeDt(6, 15));
		caseTest("WK-STD-40", "OT-8-1X-15X", "GL-STD-12-2X", wtc,
				new double[]{0,0,0, 8,4,0, 8,4,2, 8,4,1, 8,0,0, 8,1,0, 0,0,0});
	}

	public void testWK_STD_40_3() throws Exception {
		WeeklyTimecard wtc = TimecardTestUtils.makeTimecard(
				makeDt(6, 18), makeDt(6, 18), makeDt(6, 20), makeDt(6, 19), makeDt(6, 12), makeDt(6, 15), makeDt(6, 15));
		caseTest("WK-STD-40", "OT-8-1X-15X", "GL-STD-12-2X", wtc,
				new double[]{8,4,0, 8,4,0, 8,4,2, 8,4,1, 8,0,0, 0,9,0, 0,9,0});
	}

	// ==============================================================================
	// End of WK-STD-40


	// Rule name: WK-CUME-54
	// ==============================================================================



	// ==============================================================================
	// End of WK-CUME-54


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
	 * Test applying a set of rule-key values against an entire weekly timecard
	 * example, and see if the results match what's expected.
	 *
	 * @param weeklyRuleKey The Weekly-Basic rule key (the rule "name").
	 * @param otRuleKey The OT rule key (the rule "name").
	 * @param goldRuleKey The Gold time rule key (the rule "name").
	 * @param wtc the WeeklyTimecard to be evaluated.
	 */
	private void caseTest(String weeklyRuleKey, String otRuleKey, String goldRuleKey, WeeklyTimecard wtc,
			double expected[]) {

		assertTrue("'expected' values array has " + expected.length + " values instead of 21.", expected.length == 21);

		ServiceTestUtils.breakdownTest(weeklyRuleKey, otRuleKey, goldRuleKey, DEFAULT_NP_RULE, wtc);

		PayJob pj = wtc.getPayJobs().get(0);
		int expix = 0;
		int daynum = 0;
		for (PayJobDaily pd : pj.getPayJobDailies()) {
			assertTrue(message("day:" + daynum + "-1x", expected[expix], pd.getHours10(), pd), compare(expected[expix], pd.getHours10()));
			expix++;
			assertTrue(message("day:" + daynum + "-1.5x", expected[expix], pd.getHours15(), pd), compare(expected[expix], pd.getHours15()));
			expix++;
			assertTrue(message("day:" + daynum + "-2x", expected[expix], pd.getHoursCust1(), pd), compare(expected[expix], pd.getHoursCust1()));
			expix++;
			daynum++;
		}

	}

	private String message(String type, double expected, BigDecimal actual, PayJobDaily pd) {
		return type + " hrs: expected " + expected + ", calc'd "	+ actual +
				" (1x=" + pd.getHours10() +
				", 1.5x=" + pd.getHours15() +
				", 2x=" + pd.getHoursCust1() +
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

	public static DailyTime makeDt(double callTime, double wrap) {
		return new DailyTime(WorkZone.ZN, callTime, wrap);
	}

}
