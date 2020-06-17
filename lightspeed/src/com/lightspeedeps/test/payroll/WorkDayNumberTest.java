package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * This class tests the TimecardService code that calculates the work-day
 * numbers for each day of an employee's timecard.
 * <p>
 * Each test consists of constructing a "week" with various DayType values and
 * comparing the results to hand-calculated values to verify they match.
 *
 * @see com.lightspeedeps.service.TimecardService
 */
public class WorkDayNumberTest extends SpringMultiTestCase {

	private enum Day {
		WORKED,
		HOURS,
		NONE;
	}

	private static final WorkZone[] ZONES_SL = {WorkZone.ZN, WorkZone.ZN, WorkZone.ZN, WorkZone.ZN, WorkZone.ZN, WorkZone.ZN, WorkZone.ZN};

	private static final Day ALL_WORKED[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
	private static final Day ALL_HOURS[] = {Day.HOURS, Day.HOURS, Day.HOURS, Day.HOURS, Day.HOURS, Day.HOURS, Day.HOURS};
//	private static final Day REG_WORKED_WEEK[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};

	private static String prodId;

	public WorkDayNumberTest() {
	}

	/*
	 * Each test case (method) consists of creating a list of day specs, then
	 * running the test, passing a list of expected "work day number" values.
	 * The "days" array indicates whether each day is marked "Worked", or has
	 * some non-zero number of hours set, or has nothing set, which should be
	 * interpreted as a "not worked" day.
	 */

	/** Tests with "Monday" as the first day of the work week. */
	public void testMon1() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{0,1,2,3,4,5,0});
	}

	public void testMon2() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{0,1,2,3,4,5,6});
	}

	public void testMon3() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 2, new int[]{7,1,2,3,4,5,6});
	}

	public void testMon4() throws Exception {
		wkdayTestOne(makeTimecard(ALL_HOURS, ZONES_SL), 2, new int[]{7,1,2,3,4,5,6});
	}

	public void testMon5() throws Exception {
		Day days[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{6,1,2,3,4,5,0});
	}

	/** Tests with "Sunday" as the first day of the work week. */
	public void testSun1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 1, new int[]{1,2,3,4,5,6,7});
	}

	public void testSun2() throws Exception {
		Day days[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 1, new int[]{1,2,3,4,5,0,6});
	}

	/** A test with Tuesday as the first day of the work week. */
	public void testTue1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_HOURS, ZONES_SL), 3, new int[]{6,7,1,2,3,4,5});
	}

	/** A test with Wednesday as the first day of the work week. */
	public void testWed1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 4, new int[]{5,6,7,1,2,3,4});
	}

	/** Tests with Thursday as the first day of the work week. */
	public void testThu1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 5, new int[]{4,5,6,7,1,2,3});
	}
	public void testThu2() throws Exception {
		Day days[] = {Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 5, new int[]{4,5,0,6,1,2,3});
	}
	public void testThu3() throws Exception {
		Day days[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 5, new int[]{4,5,6,0,1,2,3});
	}

	/** A test with Friday as the first day of the work week. */
	public void testFri1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 6, new int[]{3,4,5,6,7,1,2});
	}

	/** A test with Saturday as the first day of the work week. */
	public void testSat1() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 7, new int[]{2,3,4,5,6,7,1});
	}

	// *************************************************************************************
	//  Tests using flexible-6th or floating-week

	/** Tests with "Monday" as the first day of the work week. */
	public void testMon1a() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{0,1,2,3,4,5,0}, true, false);
	}

	public void testMon2a() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{0,1,2,3,4,5,6}, true, false);
	}

	public void testFloat1() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 2, new int[]{0,1,2,3,4,5,0}, true, true);
	}

	public void testFloat2() throws Exception {
		Day days[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 4, new int[]{0,1,2,3,4,5,6}, true, true);
	}

	public void testFloat3() throws Exception {
		wkdayTestOne(makeTimecard(ALL_WORKED, ZONES_SL), 6, new int[]{1,2,3,4,5,6,7}, true, true);
	}

	public void testFloat4() throws Exception {
		Day days[] = {Day.NONE, Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 1, new int[]{0,0,1,2,3,4,5}, true, true);
	}

	public void testFloat5() throws Exception {
		Day days[] = {Day.NONE, Day.NONE, Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED, Day.NONE};
		wkdayTestOne(makeTimecard(days, ZONES_SL), 5, new int[]{0,0,1,2,0,3,0}, true, true);
	}

	public void testFloat6() throws Exception {
		Day days1[] = {Day.NONE, Day.NONE, Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED, Day.NONE};
		Day days2[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE, Day.WORKED, Day.WORKED};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{4,5,1,2,0,3,4}, 4, 5);
	}

	public void testFloat7() throws Exception {
		Day days1[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED};
		Day days2[] = {Day.WORKED, Day.WORKED, Day.NONE, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{5,1,0,0,0,2,3}, 3, 6);
	}

	public void testFloat8() throws Exception {
		Day days1[] = {Day.NONE, Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		Day days2[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{0,6,1,0,0,2,3}, 3, 5);
	}

	public void testFloat9() throws Exception {
		Day days1[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED};
		Day days2[] = {Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{7,1,2,0,0,3,4}, 4, 6);
	}

	public void testFloat10() throws Exception {
		Day days1[] = {Day.NONE, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.WORKED, Day.NONE};
		Day days2[] = {Day.WORKED, Day.NONE, Day.NONE, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{6,0,0,0,0,1,2}, 2, 2);
	}

	public void testFloat11() throws Exception {
		Day days1[] = {Day.NONE, Day.NONE, Day.NONE, Day.WORKED, Day.WORKED, Day.NONE, Day.NONE};
		Day days2[] = {Day.NONE, Day.WORKED, Day.NONE, Day.NONE, Day.NONE, Day.NONE, Day.NONE};
		wkdayTestFloat(makeTimecard(days2, ZONES_SL), makeTimecard(days1, ZONES_SL), new int[]{0,3,0,0,0,0,0}, 3, 0);
	}


	// *************************************************************************************

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
	 * Test determining the work-day numbers for all the days in a given
	 * timecard, and see if the results match what's expected.
	 *
	 * @param weeklyTimecard The timecard whose work-day numbers should be
	 *            calculated. This will also be used as the
	 *            "prior week's timecard".
	 * @param weekdayNum The day number of the first work day in the
	 *            "production week"; 1=Sunday, 7=Saturday.
	 * @param dayNums the work-day number values expected in the calculated
	 *            timecard
	 */
	private void wkdayTestOne(WeeklyTimecard weeklyTimecard, int weekdayNum, int[] dayNums) {

		// Create a prior week's timecard
		WeeklyTimecard priorWtc = weeklyTimecard; // default code just uses the same timecard for prior week and current week.
//		priorWtc = makeTimecard(REG_WORKED_WEEK, ZONES_SL);

		ServiceTestUtils.calculateWorkDayNumbersTest(weeklyTimecard, priorWtc, weekdayNum, false, false);

		for (int i=0; i < 7; i++) {
			String weekday = Constants.WEEKDAY_NAMES.get(i);
			assertEquals("Day: "+weekday, dayNums[i], weeklyTimecard.getDailyTimes().get(i).getWorkDayNum());
		}

	}

	/**
	 * Test determining the work-day numbers for all the days in a given
	 * timecard, and see if the results match what's expected.
	 *
	 * @param weeklyTimecard The timecard whose work-day numbers should be
	 *            calculated. This will also be used as the
	 *            "prior week's timecard".
	 * @param weekdayNum The day number of the first work day in the
	 *            "production week"; 1=Sunday, 7=Saturday.
	 * @param dayNums the work-day number values expected in the calculated
	 *            timecard
	 * @param flexible6th True if the calculation should use the "flexible 6th day" method.
	 * @param floating True if the calculation should use the "floating work week" method.
	 */
	private void wkdayTestOne(WeeklyTimecard weeklyTimecard, int weekdayNum, int[] dayNums, boolean flexible6th, boolean floating) {

		// Create a prior week's timecard
		WeeklyTimecard priorWtc = weeklyTimecard; // default code just uses the same timecard for prior week and current week.
//		priorWtc = makeTimecard(REG_WORKED_WEEK, ZONES_SL);

		ServiceTestUtils.calculateWorkDayNumbersTest(weeklyTimecard, priorWtc, weekdayNum, flexible6th, floating);

		for (int i=0; i < 7; i++) {
			String weekday = Constants.WEEKDAY_NAMES.get(i);
			assertEquals("Day: "+weekday, dayNums[i], weeklyTimecard.getDailyTimes().get(i).getWorkDayNum());
		}

	}

	/**
	 * Test determining the work-day numbers for all the days in a given
	 * timecard, and see if the results match what's expected.
	 *
	 * @param weeklyTimecard The timecard whose work-day numbers should be
	 *            calculated.
	 * @param priorWtc the prior week's timecard.
	 * @param dayNums the work-day number values expected in the calculated timecard
	 * @param lastWorkDay the value expected upon return, in the timecard's lastWorkDayNum field.
	 * @param endWeekDay the value expected upon return, in the timecard's endingDayNum field.
	 */
	private void wkdayTestFloat(WeeklyTimecard weeklyTimecard, WeeklyTimecard priorWtc, int[] dayNums, int lastWorkDay, int endWeekDay) {

		// priorWtc.setEndingDayNum((byte)10); // use this to test floating-week code that calculates prior timecard also.
		ServiceTestUtils.calculateWorkDayNumbersTest(weeklyTimecard, priorWtc, 1, true, true);

		for (int i=0; i < 7; i++) {
			String weekday = Constants.WEEKDAY_NAMES.get(i);
			assertEquals("Day: "+weekday, dayNums[i], weeklyTimecard.getDailyTimes().get(i).getWorkDayNum());
		}
		assertEquals("Last work day number:", lastWorkDay, weeklyTimecard.getLastWorkDayNum());
		assertEquals("Ending week day number:", endWeekDay, weeklyTimecard.getEndingDayNum());

	}

	/**
	 * Create a test WeeklyTimecard object with the given values.
	 *
	 * @param daySettings An array of 7 entries, representing Sunday through
	 *            Saturday, where each enum determines the DayType assigned to
	 *            the corresponding day of the week.
	 * @param zones An array of 7 WorkZone values to be assigned to the
	 *            corresponding days of the week (Sunday through Saturday).
	 * @return A weeklyTimecard with the given values.
	 */
	private WeeklyTimecard makeTimecard(Day[] daySettings, WorkZone[] zones) {
		WeeklyTimecard wtc = TimecardUtils.createBlankTimecard();

		if (prodId == null) {
			prodId = ProductionDAO.getInstance().findById(1).getProdId();
		}
		wtc.setProdId(prodId);

		byte daysWorked = 0;
		byte endingDayNum = 0;
		for (int i=0; i < daySettings.length; i++) {
			DailyTime dt = wtc.getDailyTimes().get(i);
			dt.setWorkZone(zones[i]);
			switch (daySettings[i]) {
				case HOURS:
					dt.setHours(BigDecimal.TEN);
					dt.setDayType(DayType.WK);
					daysWorked++;
					if (endingDayNum == 0) {
						endingDayNum = (byte)(i+1); // first day worked
					}
					break;
				case WORKED:
					dt.setWorked(true);
					dt.setDayType(DayType.WK);
					daysWorked++;
					if (endingDayNum == 0) {
						endingDayNum = (byte)(i+1); // first day worked
					}
					break;
				case NONE:
					dt.setDayType(null);
					break;
			}
		}

		if (endingDayNum > 0) {
			endingDayNum = (byte)(8 - endingDayNum);
		}

		wtc.setLastWorkDayNum(daysWorked);
		wtc.setEndingDayNum(endingDayNum);

		return wtc;
	}
}
