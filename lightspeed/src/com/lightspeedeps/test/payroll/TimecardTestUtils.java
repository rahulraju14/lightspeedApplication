package com.lightspeedeps.test.payroll;

import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;

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
public class TimecardTestUtils  {

	private TimecardTestUtils() {
		// don't allow instantiation
	}

	/**
	 * Create a weekly timecard with Monday through Friday all identical to the
	 * given DailyTime. Saturday and Sunday will be empty (non-work) days.
	 *
	 * @param dt1 The DailyTime instance to be used as a model for Monday
	 *            through Friday.
	 * @return The newly created timecard instance.
	 */
	public static WeeklyTimecard makeTimecard(DailyTime dt1) {

		DailyTime dt2 = dt1.clone();
		DailyTime dt3 = dt1.clone();
		DailyTime dt4 = dt1.clone();
		DailyTime dt5 = dt1.clone();

		return makeTimecard(dt1, dt2, dt3, dt4, dt5);
	}

	/**
	 * Create a weekly timecard with Monday through Friday set to the given
	 * DailyTime`s. Saturday and Sunday will be empty (non-work) days.
	 *
	 * @param dt1 The DailyTime instance for Monday.
	 * @param dt2 The DailyTime instance for Tuesday.
	 * @param dt3 The DailyTime instance for Wednesday.
	 * @param dt4 The DailyTime instance for Thursday.
	 * @param dt5 The DailyTime instance for Friday.
	 * @return The newly created timecard instance.
	 */
	public static WeeklyTimecard makeTimecard(DailyTime dt1, DailyTime dt2,
			DailyTime dt3, DailyTime dt4, DailyTime dt5) {

		DailyTime dt0 = new DailyTime(null, (byte)1);
		DailyTime dt6 = new DailyTime(null, (byte)7);

		return makeTimecard(dt0, dt1, dt2, dt3, dt4, dt5, dt6);
	}


	/**
	 * Create a weekly timecard with Sunday through Saturday set to the given
	 * DailyTime`s.
	 *
	 * @param dt0 The DailyTime instance for Sunday.
	 * @param dt1 The DailyTime instance for Monday.
	 * @param dt2 The DailyTime instance for Tuesday.
	 * @param dt3 The DailyTime instance for Wednesday.
	 * @param dt4 The DailyTime instance for Thursday.
	 * @param dt5 The DailyTime instance for Friday.
	 * @param dt6 The DailyTime instance for Saturday.
	 * @return The newly created timecard instance.
	 */
	public static WeeklyTimecard makeTimecard(DailyTime dt0, DailyTime dt1, DailyTime dt2,
			DailyTime dt3, DailyTime dt4, DailyTime dt5, DailyTime dt6) {

		WeeklyTimecard wtc = TimecardUtils.createBlankTimecard();

		wtc.getDailyTimes().set(0, dt0);	// replace DailyTime
		wtc.getDailyTimes().set(1, dt1);	// replace DailyTime
		wtc.getDailyTimes().set(2, dt2);	// replace DailyTime
		wtc.getDailyTimes().set(3, dt3);	// replace DailyTime
		wtc.getDailyTimes().set(4, dt4);	// replace DailyTime
		wtc.getDailyTimes().set(5, dt5);	// replace DailyTime
		wtc.getDailyTimes().set(6, dt6);	// replace DailyTime

		byte daynum = 1;
		for (DailyTime dt : wtc.getDailyTimes()) {
			dt.setWeeklyTimecard(wtc);
			dt.setDayNum(daynum++);
			TimecardCalc.calculateDailyHours(dt); // calculate worked hours
		}

		return wtc;
	}

}
