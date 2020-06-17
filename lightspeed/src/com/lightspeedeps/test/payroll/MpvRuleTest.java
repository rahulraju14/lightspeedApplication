package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.lightspeedeps.dao.MpvRuleDAO;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.MpvRule;
import com.lightspeedeps.service.MpvService;
import com.lightspeedeps.service.PayBreakdownService;
import com.lightspeedeps.test.SpringMultiTestCase;

/**
 * This class tests the TimecardMpvService code that calculates the number of
 * MPVs and the associated penalty amount.
 * <p>
 * This does NOT test the determination of which rules to apply, it simply
 * applies a number of different rules to a variety of daily schedules and
 * compares the results to hand-calculated values to verify they match.
 *
 * @see com.lightspeedeps.model.MpvRule
 * @see com.lightspeedeps.service.MpvService
 */
public class MpvRuleTest extends SpringMultiTestCase {

	/**
	 * This will be used as the employee's "prevailing rate" for rules that need
	 * that value to compute the total meal penalty.
	 */
	BigDecimal prevailingRate = new BigDecimal(15);
	BigDecimal straightRate = new BigDecimal(10);

	// Each test case (method) consists of creating a "DailyTime" object with
	// the desired
	// times (call and wrap, plus either one or both meal out & in times), then
	// invoking a particular rule and comparing the resulting calculations of
	// penalty
	// counts and amount to the expected values.



	// Rule name: MP-LA
	// ==============================================================================

	public void testLA_STD1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-LA", dt, 0, 1, 0, 7.50);
	}

	public void testLA_STD2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-LA", dt, 0, 1, 0, 7.50);
	}

	public void testLA_STD3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-LA", dt, 0, 2, 0, 17.50);
	}

	public void testLA_STD4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-LA", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50);
	}

	public void testLA_STD11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-LA", dt, 3, 0, 0, 30.00);
	}

	public void testLA_STD12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-LA", dt, 4, 0, 0, 42.50);
	}

	public void testLA_STD13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-LA", dt, 4, 0, 0, 42.50);
	}

	public void testLA_STD14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-LA", dt, 5, 0, 0, 55.00);
	}

	public void testLA_STD15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-LA", dt, 6, 0, 0, 67.50);
	}

	public void testLA_STD16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50); // New
	}

	public void testLA_STD20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-LA", dt, 1, 0, 0, 7.50); // New
	}

	public void testLA_STD21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-LA", dt, 3, 1, 0, 37.50);
	}

	public void testLA_STD22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-LA", dt, 4, 1, 0, 50.00);
	}

	public void testLA_STD23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-LA", dt, 4, 1, 0, 50.00);
	}

	public void testLA_STD24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-LA", dt, 2, 2, 0, 35.00);
	}

	public void testLA_STD25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-LA", dt, 2, 3, 0, 47.50);
	}

	public void testLA_STD26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-LA", dt, 2, 5, 0, 72.50);
	}

	public void testLA_STD27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-LA", dt, 8, 2, 0, 110.00);
	}

	public void testLA_STD28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-LA", dt, 4, 5, 0, 97.50);
	}

	public void testLA_STD29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-LA", dt, 5, 3, 0, 85.00);
	}

	public void testLA_STD30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-LA", dt, 5, 7, 0, 135.00);
	}

	// ==============================================================================
	// End of MP-LA



	// Rule name: MP-LA-D
	// ==============================================================================

	public void testLA_STD_D_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-LA-D", dt, 0, 1, 0, 7.50);
	}

	public void testLA_STD_D_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-LA-D", dt, 0, 1, 0, 7.50);
	}

	public void testLA_STD_D_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-LA-D", dt, 0, 2, 0, 17.50);
	}

	public void testLA_STD_D_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD_D_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD_D_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_STD_D_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD_D_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD_D_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-LA-D", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_STD_D_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50);
	}

	public void testLA_STD_D_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-LA-D", dt, 3, 0, 0, 30.00);
	}

	public void testLA_STD_D_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-LA-D", dt, 4, 0, 0, 42.50);
	}

	public void testLA_STD_D_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-LA-D", dt, 4, 0, 0, 42.50);
	}

	public void testLA_STD_D_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-LA-D", dt, 5, 0, 0, 55.00);
	}

	public void testLA_STD_D_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-LA-D", dt, 6, 0, 0, 67.50);
	}

	public void testLA_STD_D_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD_D_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD_D_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testLA_STD_D_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50); // New
	}

	public void testLA_STD_D_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-LA-D", dt, 1, 0, 0, 7.50); // New
	}

	public void testLA_STD_D_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-LA-D", dt, 3, 1, 0, 37.50);
	}

	public void testLA_STD_D_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-LA-D", dt, 4, 1, 0, 50.00);
	}

	public void testLA_STD_D_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-LA-D", dt, 4, 1, 0, 50.00);
	}

	public void testLA_STD_D_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-LA-D", dt, 2, 2, 0, 35.00);
	}

	public void testLA_STD_D_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-LA-D", dt, 2, 3, 0, 47.50);
	}

	public void testLA_STD_D_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-LA-D", dt, 2, 5, 0, 72.50);
	}

	public void testLA_STD_D_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-LA-D", dt, 8, 2, 0, 110.00);
	}

	public void testLA_STD_D_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-LA-D", dt, 4, 5, 0, 97.50);
	}

	public void testLA_STD_D_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-LA-D", dt, 5, 3, 0, 85.00);
	}

	public void testLA_STD_D_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-LA-D", dt, 5, 7, 0, 135.00);
	}

	// ==============================================================================
	// End of MP-LA-D



	// Rule name: MP-LA-STG-MOW
	// ==============================================================================

	public void testLA_STG_MOW_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 1, 0, 8.50);
	}

	public void testLA_STG_MOW_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 1, 0, 8.50);
	}

	public void testLA_SG_MOW_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 2, 0, 19.50);
	}

	public void testLA_SG_MOW_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_SG_MOW_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_SG_MOW_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testLA_SG_MOW_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_SG_MOW_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_SG_MOW_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-LA-STG-MOW", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testLA_SG_MOW_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50);
	}

	public void testLA_SG_MOW_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-LA-STG-MOW", dt, 3, 0, 0, 33.00);
	}

	public void testLA_SG_MOW_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-LA-STG-MOW", dt, 4, 0, 0, 46.50);
	}

	public void testLA_SG_MOW_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-LA-STG-MOW", dt, 4, 0, 0, 46.50);
	}

	public void testLA_SG_MOW_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-LA-STG-MOW", dt, 5, 0, 0, 60.00);
	}

	public void testLA_SG_MOW_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 6, 0, 0, 73.50);
	}

	public void testLA_SG_MOW_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testLA_SG_MOW_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testLA_SG_MOW_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testLA_SG_MOW_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50); // New
	}

	public void testLA_SG_MOW_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-LA-STG-MOW", dt, 1, 0, 0, 8.50); // New
	}

	public void testLA_SG_MOW_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-LA-STG-MOW", dt, 3, 1, 0, 41.50);
	}

	public void testLA_SG_MOW_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-LA-STG-MOW", dt, 4, 1, 0, 55.00);
	}

	public void testLA_SG_MOW_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-LA-STG-MOW", dt, 4, 1, 0, 55.00);
	}

	public void testLA_SG_MOW_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-LA-STG-MOW", dt, 2, 2, 0, 39.00);
	}

	public void testLA_SG_MOW_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-LA-STG-MOW", dt, 2, 3, 0, 52.50);
	}

	public void testLA_SG_MOW_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-LA-STG-MOW", dt, 2, 5, 0, 79.50);
	}

	public void testLA_SG_MOW_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-LA-STG-MOW", dt, 8, 2, 0, 120.00);
	}

	public void testLA_SG_MOW_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-LA-STG-MOW", dt, 4, 5, 0, 106.50);
	}

	public void testLA_SG_MOW_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-LA-STG-MOW", dt, 5, 3, 0, 93.00);
	}

	public void testLA_SG_MOW_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-LA-STG-MOW", dt, 5, 7, 0, 147.00);
	}

	// ==============================================================================
	// End of MP-LA-SG-MOW



	// Rule name: MP-BC
	// ==============================================================================

	public void testBC_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-BC", dt, 0, 1, 0, 10.00);
	}

	public void testBC_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-BC", dt, 0, 1, 0, 10.00);
	}

	public void testBC_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-BC", dt, 0, 2, 0, 22.50);
	}

	public void testBC_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testBC_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testBC_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testBC_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testBC_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testBC_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-BC", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testBC_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00);
	}

	public void testBC_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-BC", dt, 3, 0, 0, 37.50);
	}

	public void testBC_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-BC", dt, 4, 0, 0, 52.50);
	}

	public void testBC_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-BC", dt, 4, 0, 0, 52.50);
	}

	public void testBC_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-BC", dt, 5, 0, 0, 67.50);
	}

	public void testBC_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-BC", dt, 6, 0, 0, 82.50);
	}

	public void testBC_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testBC_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testBC_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testBC_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00); // New
	}

	public void testBC_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-BC", dt, 1, 0, 0, 10.00); // New
	}

	public void testBC_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-BC", dt, 3, 1, 0, 47.50);
	}

	public void testBC_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-BC", dt, 4, 1, 0, 62.50);
	}

	public void testBC_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-BC", dt, 4, 1, 0, 62.50);
	}

	public void testBC_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-BC", dt, 2, 2, 0, 45.00);
	}

	public void testBC_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-BC", dt, 2, 3, 0, 60.00);
	}

	public void testBC_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-BC", dt, 2, 5, 0, 90.00);
	}

	public void testBC_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-BC", dt, 8, 2, 0, 135.00);
	}

	public void testBC_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-BC", dt, 4, 5, 0, 120.00);
	}

	public void testBC_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-BC", dt, 5, 3, 0, 105.00);
	}

	public void testBC_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-BC", dt, 5, 7, 0, 165.00);
	}

	// ==============================================================================
	// End of MP-BC



	// Rule name: MP-NY-I
	// ==============================================================================

	public void testNY_I_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-NY-I", dt, 0, 1, 0, 15.00);
	}

	public void testNY_I_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-NY-I", dt, 0, 1, 0, 15.00);
	}

	public void testNY_I_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-NY-I", dt, 0, 2, 0, 30.00);
	}

	public void testNY_I_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_I_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_I_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_I_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_I_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_I_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-NY-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_I_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00);
	}

	public void testNY_I_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-NY-I", dt, 3, 0, 0, 45.00);
	}

	public void testNY_I_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-NY-I", dt, 4, 0, 0, 60.00);
	}

	public void testNY_I_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-NY-I", dt, 4, 0, 0, 60.00);
	}

	public void testNY_I_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-NY-I", dt, 5, 0, 0, 75.00);
	}

	public void testNY_I_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-NY-I", dt, 6, 0, 0, 90.00);
	}

	public void testNY_I_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void testNY_I_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void testNY_I_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void testNY_I_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00); // New
	}

	public void testNY_I_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-NY-I", dt, 1, 0, 0, 15.00); // New
	}

	public void testNY_I_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-NY-I", dt, 3, 1, 0, 60.00);
	}

	public void testNY_I_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-NY-I", dt, 4, 1, 0, 75.00);
	}

	public void testNY_I_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-NY-I", dt, 4, 1, 0, 75.00);
	}

	public void testNY_I_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-NY-I", dt, 2, 2, 0, 60.00);
	}

	public void testNY_I_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-NY-I", dt, 2, 3, 0, 75.00);
	}

	public void testNY_I_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-NY-I", dt, 2, 5, 0, 105.00);
	}

	public void testNY_I_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-NY-I", dt, 8, 2, 0, 150.00);
	}

	public void testNY_I_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-NY-I", dt, 4, 5, 0, 135.00);
	}

	public void testNY_I_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-NY-I", dt, 5, 3, 0, 120.00);
	}

	public void testNY_I_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-NY-I", dt, 5, 7, 0, 180.00);
	}

	// ==============================================================================
	// End of MP-NY-I



	// Rule name: MP-161-I
	// ==============================================================================

	public void test161_I_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-161-I", dt, 0, 1, 0, 15.00);
	}

	public void test161_I_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-161-I", dt, 0, 1, 0, 15.00);
	}

	public void test161_I_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-161-I", dt, 0, 2, 0, 30.00);
	}

	public void test161_I_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_I_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_I_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_I_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_I_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_I_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-161-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_I_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00);
	}

	public void test161_I_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-161-I", dt, 3, 0, 0, 40.00);
	}

	public void test161_I_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-161-I", dt, 4, 0, 0, 55.00);
	}

	public void test161_I_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-161-I", dt, 4, 0, 0, 55.00);
	}

	public void test161_I_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-161-I", dt, 5, 0, 0, 70.00);
	}

	public void test161_I_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-161-I", dt, 6, 0, 0, 85.00);
	}

	public void test161_I_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test161_I_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test161_I_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test161_I_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00); // New
	}

	public void test161_I_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-161-I", dt, 1, 0, 0, 10.00); // New
	}

	public void test161_I_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-161-I", dt, 3, 1, 0, 55.00);
	}

	public void test161_I_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-161-I", dt, 4, 1, 0, 70.00);
	}

	public void test161_I_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-161-I", dt, 4, 1, 0, 70.00);
	}

	public void test161_I_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-161-I", dt, 2, 2, 0, 55.00);
	}

	public void test161_I_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-161-I", dt, 2, 3, 0, 70.00);
	}

	public void test161_I_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-161-I", dt, 2, 5, 0, 100.00);
	}

	public void test161_I_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-161-I", dt, 8, 2, 0, 145.00);
	}

	public void test161_I_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-161-I", dt, 4, 5, 0, 130.00);
	}

	public void test161_I_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-161-I", dt, 5, 3, 0, 115.00);
	}

	public void test161_I_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-161-I", dt, 5, 7, 0, 175.00);
	}

	// ==============================================================================
	// End of MP-161-I



	// Rule name: MP-161-M
	// ==============================================================================

	public void test161_M_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-161-M", dt, 0, 1, 0, 7.50);
	}

	public void test161_M_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-161-M", dt, 0, 1, 0, 7.50);
	}

	public void test161_M_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-161-M", dt, 0, 2, 0, 18.00);
	}

	public void test161_M_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_M_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_M_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test161_M_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_M_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_M_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-161-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test161_M_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50);
	}

	public void test161_M_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-161-M", dt, 3, 0, 0, 30.50);
	}

	public void test161_M_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-161-M", dt, 4, 0, 0, 43.00);
	}

	public void test161_M_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-161-M", dt, 4, 0, 0, 43.00);
	}

	public void test161_M_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-161-M", dt, 5, 0, 0, 55.50);
	}

	public void test161_M_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-161-M", dt, 6, 0, 0, 68.00);
	}

	public void test161_M_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test161_M_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test161_M_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test161_M_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50); // New
	}

	public void test161_M_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-161-M", dt, 1, 0, 0, 7.50); // New
	}

	public void test161_M_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-161-M", dt, 3, 1, 0, 38.00);
	}

	public void test161_M_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-161-M", dt, 4, 1, 0, 50.50);
	}

	public void test161_M_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-161-M", dt, 4, 1, 0, 50.50);
	}

	public void test161_M_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-161-M", dt, 2, 2, 0, 36.00);
	}

	public void test161_M_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-161-M", dt, 2, 3, 0, 48.50);
	}

	public void test161_M_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-161-M", dt, 2, 5, 0, 73.50);
	}

	public void test161_M_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-161-M", dt, 8, 2, 0, 111.00);
	}

	public void test161_M_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-161-M", dt, 4, 5, 0, 98.50);
	}

	public void test161_M_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-161-M", dt, 5, 3, 0, 86.00);
	}

	public void test161_M_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-161-M", dt, 5, 7, 0, 136.00);
	}

	// ==============================================================================
	// End of MP-161-M



	// Rule name: MP-600-I
	// ==============================================================================

	public void test600_I_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-600-I", dt, 0, 1, 0, 15.00);
	}

	public void test600_I_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-600-I", dt, 0, 1, 0, 15.00);
	}

	public void test600_I_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-600-I", dt, 0, 2, 0, 30.00);
	}

	public void test600_I_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test600_I_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test600_I_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test600_I_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test600_I_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test600_I_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-600-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test600_I_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00);
	}

	public void test600_I_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-600-I", dt, 3, 0, 0, 40.00);
	}

	public void test600_I_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-600-I", dt, 4, 0, 0, 55.00);
	}

	public void test600_I_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-600-I", dt, 4, 0, 0, 55.00);
	}

	public void test600_I_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-600-I", dt, 5, 0, 0, 70.00);
	}

	public void test600_I_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-600-I", dt, 6, 0, 0, 85.00);
	}

	public void test600_I_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test600_I_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test600_I_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void test600_I_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00); // New
	}

	public void test600_I_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-600-I", dt, 1, 0, 0, 10.00); // New
	}

	public void test600_I_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-600-I", dt, 3, 1, 0, 55.00);
	}

	public void test600_I_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-600-I", dt, 4, 1, 0, 70.00);
	}

	public void test600_I_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-600-I", dt, 4, 1, 0, 70.00);
	}

	public void test600_I_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-600-I", dt, 2, 2, 0, 55.00);
	}

	public void test600_I_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-600-I", dt, 2, 3, 0, 70.00);
	}

	public void test600_I_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-600-I", dt, 2, 5, 0, 100.00);
	}

	public void test600_I_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-600-I", dt, 8, 2, 0, 145.00);
	}

	public void test600_I_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-600-I", dt, 4, 5, 0, 130.00);
	}

	public void test600_I_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-600-I", dt, 5, 3, 0, 115.00);
	}

	public void test600_I_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-600-I", dt, 5, 7, 0, 175.00);
	}

	// ==============================================================================
	// End of MP-600-I



	// Rule name: MP-NY-M
	// ==============================================================================

	public void testNY_M_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-NY-M", dt, 0, 1, 0, 10.00);
	}

	public void testNY_M_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-NY-M", dt, 0, 1, 0, 10.00);
	}

	public void testNY_M_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-NY-M", dt, 0, 2, 0, 25.00);
	}

	public void testNY_M_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_M_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_M_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_M_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_M_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_M_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-NY-M", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_M_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00);
	}

	public void testNY_M_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-NY-M", dt, 3, 0, 0, 40.00);
	}

	public void testNY_M_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-NY-M", dt, 4, 0, 0, 55.00);
	}

	public void testNY_M_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-NY-M", dt, 4, 0, 0, 55.00);
	}

	public void testNY_M_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-NY-M", dt, 5, 0, 0, 70.00);
	}

	public void testNY_M_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-NY-M", dt, 6, 0, 0, 85.00);
	}

	public void testNY_M_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testNY_M_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testNY_M_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00); // 6.1 interval
	}

	public void testNY_M_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00); // New
	}

	public void testNY_M_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-NY-M", dt, 1, 0, 0, 10.00); // New
	}

	public void testNY_M_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-NY-M", dt, 3, 1, 0, 50.00);
	}

	public void testNY_M_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-NY-M", dt, 4, 1, 0, 65.00);
	}

	public void testNY_M_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-NY-M", dt, 4, 1, 0, 65.00);
	}

	public void testNY_M_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-NY-M", dt, 2, 2, 0, 50.00);
	}

	public void testNY_M_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-NY-M", dt, 2, 3, 0, 65.00);
	}

	public void testNY_M_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-NY-M", dt, 2, 5, 0, 95.00);
	}

	public void testNY_M_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-NY-M", dt, 8, 2, 0, 140.00);
	}

	public void testNY_M_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-NY-M", dt, 4, 5, 0, 125.00);
	}

	public void testNY_M_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-NY-M", dt, 5, 3, 0, 110.00);
	}

	public void testNY_M_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-NY-M", dt, 5, 7, 0, 170.00);
	}

	// ==============================================================================
	// End of MP-NY-M



	// Rule name: MP-NY-T
	// ==============================================================================

	public void testNY_T_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-NY-T", dt, 0, 1, 0, 7.50);
	}

	public void testNY_T_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-NY-T", dt, 0, 1, 0, 7.50);
	}

	public void testNY_T_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-NY-T", dt, 0, 2, 0, 16.00);
	}

	public void testNY_T_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_T_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_T_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testNY_T_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_T_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_T_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-NY-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testNY_T_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50);
	}

	public void testNY_T_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-NY-T", dt, 3, 0, 0, 34.50);
	}

	public void testNY_T_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-NY-T", dt, 4, 0, 0, 49.50);
	}

	public void testNY_T_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-NY-T", dt, 4, 0, 0, 49.50);
	}

	public void testNY_T_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-NY-T", dt, 5, 0, 0, 64.50);
	}

	public void testNY_T_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-NY-T", dt, 6, 0, 0, 79.50);
	}

	public void testNY_T_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testNY_T_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testNY_T_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void testNY_T_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50); // New
	}

	public void testNY_T_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-NY-T", dt, 1, 0, 0, 7.50); // New
	}

	public void testNY_T_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-NY-T", dt, 3, 1, 0, 42.00);
	}

	public void testNY_T_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-NY-T", dt, 4, 1, 0, 57.00);
	}

	public void testNY_T_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-NY-T", dt, 4, 1, 0, 57.00);
	}

	public void testNY_T_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-NY-T", dt, 2, 2, 0, 32.00);
	}

	public void testNY_T_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-NY-T", dt, 2, 3, 0, 50.50);
	}

	public void testNY_T_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-NY-T", dt, 2, 5, 0, 80.50);
	}

	public void testNY_T_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-NY-T", dt, 8, 2, 0, 125.50);
	}

	public void testNY_T_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-NY-T", dt, 4, 5, 0, 114.00);
	}

	public void testNY_T_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-NY-T", dt, 5, 3, 0, 99.00);
	}

	public void testNY_T_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-NY-T", dt, 5, 7, 0, 159.00);
	}

	// ==============================================================================
	// End of MP-NY-T



	// Rule name: MP-52-T
	// ==============================================================================

	public void test52_T_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-52-T", dt, 0, 1, 0, 8.50);
	}

	public void test52_T_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-52-T", dt, 0, 1, 0, 8.50);
	}

	public void test52_T_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-52-T", dt, 0, 2, 0, 19.50);
	}

	public void test52_T_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-52-T", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50);
	}

	public void test52_T_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-52-T", dt, 3, 0, 0, 33.00);
	}

	public void test52_T_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-52-T", dt, 4, 0, 0, 46.50);
	}

	public void test52_T_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-52-T", dt, 4, 0, 0, 46.50);
	}

	public void test52_T_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-52-T", dt, 5, 0, 0, 60.00);
	}

	public void test52_T_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-52-T", dt, 6, 0, 0, 73.50);
	}

	public void test52_T_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50); // New
	}

	public void test52_T_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-52-T", dt, 1, 0, 0, 8.50); // New
	}

	public void test52_T_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-52-T", dt, 3, 1, 0, 41.50);
	}

	public void test52_T_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-52-T", dt, 4, 1, 0, 55.00);
	}

	public void test52_T_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-52-T", dt, 4, 1, 0, 55.00);
	}

	public void test52_T_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-52-T", dt, 2, 2, 0, 39.00);
	}

	public void test52_T_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-52-T", dt, 2, 3, 0, 52.50);
	}

	public void test52_T_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-52-T", dt, 2, 5, 0, 79.50);
	}

	public void test52_T_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-52-T", dt, 8, 2, 0, 120.00);
	}

	public void test52_T_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-52-T", dt, 4, 5, 0, 106.50);
	}

	public void test52_T_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-52-T", dt, 5, 3, 0, 93.00);
	}

	public void test52_T_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-52-T", dt, 5, 7, 0, 147.00);
	}

	// ==============================================================================
	// End of MP-52-T



	// Rule name: MP-52-T-P
	// ==============================================================================

	public void test52_T_P_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-52-T-P", dt, 0, 1, 0, 8.50);
	}

	public void test52_T_P_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-52-T-P", dt, 0, 1, 0, 8.50);
	}

	public void test52_T_P_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-52-T-P", dt, 0, 2, 0, 19.50);
	}

	public void test52_T_P_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_P_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_P_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_T_P_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_P_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_P_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-52-T-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_T_P_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50);
	}

	public void test52_T_P_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-52-T-P", dt, 3, 0, 0, 38.00);
	}

	public void test52_T_P_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-52-T-P", dt, 4, 0, 0, 56.50);
	}

	public void test52_T_P_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-52-T-P", dt, 4, 0, 0, 56.50);
	}

	public void test52_T_P_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-52-T-P", dt, 5, 0, 0, 75.00);
	}

	public void test52_T_P_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-52-T-P", dt, 6, 0, 0, 93.50);
	}

	public void test52_T_P_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_P_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_P_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void test52_T_P_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50); // New
	}

	public void test52_T_P_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-52-T-P", dt, 1, 0, 0, 8.50); // New
	}

	public void test52_T_P_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-52-T-P", dt, 3, 1, 0, 46.50);
	}

	public void test52_T_P_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-52-T-P", dt, 4, 1, 0, 65.00);
	}

	public void test52_T_P_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-52-T-P", dt, 4, 1, 0, 65.00);
	}

	public void test52_T_P_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-52-T-P", dt, 2, 2, 0, 39.00);
	}

	public void test52_T_P_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-52-T-P", dt, 2, 3, 0, 57.50);
	}

	public void test52_T_P_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-52-T-P", dt, 2, 5, 0, 94.50);
	}

	public void test52_T_P_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-52-T-P", dt, 8, 2, 0, 150.00);
	}

	public void test52_T_P_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-52-T-P", dt, 4, 5, 0, 131.50);
	}

	public void test52_T_P_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-52-T-P", dt, 5, 3, 0, 113.00);
	}

	public void test52_T_P_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-52-T-P", dt, 5, 7, 0, 187.00);
	}

	// ==============================================================================
	// End of MP-52-T-P



	// Rule name: MP-52-F-P
	// ==============================================================================

	public void test52_F_P_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-52-F-P", dt, 0, 1, 0, 7.50);
	}

	public void test52_F_P_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-52-F-P", dt, 0, 1, 0, 7.50);
	}

	public void test52_F_P_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-52-F-P", dt, 0, 2, 0, 17.50);
	}

	public void test52_F_P_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_F_P_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_F_P_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test52_F_P_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_F_P_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_F_P_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-52-F-P", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test52_F_P_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50);
	}

	public void test52_F_P_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-52-F-P", dt, 3, 0, 0, 35.00);
	}

	public void test52_F_P_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-52-F-P", dt, 4, 0, 0, 52.50);
	}

	public void test52_F_P_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-52-F-P", dt, 4, 0, 0, 52.50);
	}

	public void test52_F_P_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-52-F-P", dt, 5, 0, 0, 70.00);
	}

	public void test52_F_P_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-52-F-P", dt, 6, 0, 0, 87.50);
	}

	public void test52_F_P_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test52_F_P_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test52_F_P_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50); // 6.1 interval
	}

	public void test52_F_P_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50); // New
	}

	public void test52_F_P_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-52-F-P", dt, 1, 0, 0, 7.50); // New
	}

	public void test52_F_P_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-52-F-P", dt, 3, 1, 0, 42.50);
	}

	public void test52_F_P_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-52-F-P", dt, 4, 1, 0, 60.00);
	}

	public void test52_F_P_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-52-F-P", dt, 4, 1, 0, 60.00);
	}

	public void test52_F_P_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-52-F-P", dt, 2, 2, 0, 35.00);
	}

	public void test52_F_P_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-52-F-P", dt, 2, 3, 0, 52.50);
	}

	public void test52_F_P_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-52-F-P", dt, 2, 5, 0, 87.50);
	}

	public void test52_F_P_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-52-F-P", dt, 8, 2, 0, 140.00);
	}

	public void test52_F_P_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-52-F-P", dt, 4, 5, 0, 122.50);
	}

	public void test52_F_P_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-52-F-P", dt, 5, 3, 0, 105.00);
	}

	public void test52_F_P_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-52-F-P", dt, 5, 7, 0, 175.00);
	}

	// ==============================================================================
	// End of MP-52-F-P



	// Rule name: MP-700
	// ==============================================================================

	public void test700_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-700", dt, 0, 1, 0, 5.00);
	}

	public void test700_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-700", dt, 0, 1, 0, 5.00);
	}

	public void test700_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-700", dt, 0, 2, 0, 10.00);
	}

	public void test700_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test700_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test700_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test700_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test700_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test700_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-700", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test700_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00);
	}

	public void test700_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-700", dt, 3, 0, 0, 15.00);
	}

	public void test700_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-700", dt, 4, 0, 0, 20.00);
	}

	public void test700_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-700", dt, 4, 0, 0, 20.00);
	}

	public void test700_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-700", dt, 5, 0, 0, 25.00);
	}

	public void test700_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-700", dt, 6, 0, 0, 30.00);
	}

	public void test700_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00); // 6.1 interval
	}

	public void test700_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00); // 6.1 interval
	}

	public void test700_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00); // 6.1 interval
	}

	public void test700_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00); // New
	}

	public void test700_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-700", dt, 1, 0, 0, 5.00); // New
	}

	public void test700_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-700", dt, 3, 1, 0, 20.00);
	}

	public void test700_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-700", dt, 4, 1, 0, 25.00);
	}

	public void test700_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-700", dt, 4, 1, 0, 25.00);
	}

	public void test700_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-700", dt, 2, 2, 0, 20.00);
	}

	public void test700_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-700", dt, 2, 3, 0, 25.00);
	}

	public void test700_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-700", dt, 2, 5, 0, 35.00);
	}

	public void test700_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-700", dt, 8, 2, 0, 50.00);
	}

	public void test700_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-700", dt, 4, 5, 0, 45.00);
	}

	public void test700_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-700", dt, 5, 3, 0, 40.00);
	}

	public void test700_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-700", dt, 5, 7, 0, 60.00);
	}

	// ==============================================================================
	// End of MP-700



	// Rule name: MP-829-I
	// ==============================================================================

	public void test829_I_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-829-I", dt, 0, 1, 0, 15.00);
	}

	public void test829_I_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-829-I", dt, 0, 1, 0, 15.00);
	}

	public void test829_I_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-829-I", dt, 0, 2, 0, 40.00);
	}

	public void test829_I_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test829_I_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test829_I_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void test829_I_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test829_I_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test829_I_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-829-I", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void test829_I_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00);
	}

	public void test829_I_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-829-I", dt, 3, 0, 0, 55.00);
	}

	public void test829_I_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-829-I", dt, 4, 0, 0, 70.00);
	}

	public void test829_I_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-829-I", dt, 4, 0, 0, 70.00);
	}

	public void test829_I_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-829-I", dt, 5, 0, 0, 85.00);
	}

	public void test829_I_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-829-I", dt, 6, 0, 0, 100.00);
	}

	public void test829_I_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void test829_I_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void test829_I_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00); // 6.1 interval
	}

	public void test829_I_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00); // New
	}

	public void test829_I_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-829-I", dt, 1, 0, 0, 15.00); // New
	}

	public void test829_I_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-829-I", dt, 3, 1, 0, 70.00);
	}

	public void test829_I_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-829-I", dt, 4, 1, 0, 85.00);
	}

	public void test829_I_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-829-I", dt, 4, 1, 0, 85.00);
	}

	public void test829_I_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-829-I", dt, 2, 2, 0, 80.00);
	}

	public void test829_I_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-829-I", dt, 2, 3, 0, 95.00);
	}

	public void test829_I_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-829-I", dt, 2, 5, 0, 125.00);
	}

	public void test829_I_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-829-I", dt, 8, 2, 0, 170.00);
	}

	public void test829_I_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-829-I", dt, 4, 5, 0, 155.00);
	}

	public void test829_I_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-829-I", dt, 5, 3, 0, 140.00);
	}

	public void test829_I_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-829-I", dt, 5, 7, 0, 200.00);
	}

	// ==============================================================================
	// End of MP-829-I



	// Rule name: MP-HBO-8
	// ==============================================================================

	public void testHBO_8_1() throws Exception {
		DailyTime dt = new DailyTime(8.5, 12, 12.5, 19, 19.5, 21);
		mpvTestOne("MP-HBO-8", dt, 0, 1, 0, 8.50);
	}

	public void testHBO_8_2() throws Exception {
		DailyTime dt = new DailyTime(1, 7, 7.6, 14);
		mpvTestOne("MP-HBO-8", dt, 0, 1, 0, 8.50);
	}

	public void testHBO_8_3() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 19.2);
		mpvTestOne("MP-HBO-8", dt, 0, 1, 0, 8.50);
	}

	public void testHBO_8_4() throws Exception {
		DailyTime dt = new DailyTime(6, 12, 12.5, 18);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testHBO_8_5() throws Exception {
		DailyTime dt = new DailyTime(7, 13, 13.5, 16);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testHBO_8_6() throws Exception {
		DailyTime dt = new DailyTime(8, 14, 14.5, 20.5);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 6.0 interval
	}

	public void testHBO_8_7() throws Exception {
		DailyTime dt = new DailyTime(9, 14.9, 15.4, 20.5);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testHBO_8_8() throws Exception {
		DailyTime dt = new DailyTime(5.5, 11.4, 11.9, 17.5);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testHBO_8_9() throws Exception {
		DailyTime dt = new DailyTime(9.9, 15.8, 16.3, 19.4);
		mpvTestOne("MP-HBO-8", dt, 0, 0, 0, 0); // 5.9 interval
	}

	public void testHBO_8_10() throws Exception {
		DailyTime dt = new DailyTime(5.5, 12, 12.5, 18.5);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50);
	}

	public void testHBO_8_11() throws Exception {
		DailyTime dt = new DailyTime(9.5, 17, 17.5, 23);
		mpvTestOne("MP-HBO-8", dt, 2, 0, 0, 21.00);
	}

	public void testHBO_8_12() throws Exception {
		DailyTime dt = new DailyTime(8, 16);
		mpvTestOne("MP-HBO-8", dt, 3, 0, 0, 33.50);
	}

	public void testHBO_8_13() throws Exception {
		DailyTime dt = new DailyTime(7, 14.7, 15.2, 20);
		mpvTestOne("MP-HBO-8", dt, 3, 0, 0, 33.50);
	}

	public void testHBO_8_14() throws Exception {
		DailyTime dt = new DailyTime(1.2, 9.4);
		mpvTestOne("MP-HBO-8", dt, 3, 0, 0, 33.50);
	}

	public void testHBO_8_15() throws Exception {
		DailyTime dt = new DailyTime(3.6, 12.5);
		mpvTestOne("MP-HBO-8", dt, 4, 0, 0, 46.00);
	}

	public void testHBO_8_16() throws Exception {
		DailyTime dt = new DailyTime(5.9, 12, 12.5, 17.5);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testHBO_8_17() throws Exception {
		DailyTime dt = new DailyTime(4.2, 10.3, 10.8, 16);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testHBO_8_18() throws Exception {
		DailyTime dt = new DailyTime(7, 13.1, 13.8, 18);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50); // 6.1 interval
	}

	public void testHBO_8_19() throws Exception {
		DailyTime dt = new DailyTime(6, 12.4, 12.9, 17.2);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50); // New
	}

	public void testHBO_8_20() throws Exception {
		DailyTime dt = new DailyTime(8.1, 14.4, 14.9, 19.2);
		mpvTestOne("MP-HBO-8", dt, 1, 0, 0, 8.50); // New
	}

	public void testHBO_8_21() throws Exception {
		DailyTime dt = new DailyTime(7.9, 15.2, 15.7, 22);
		mpvTestOne("MP-HBO-8", dt, 2, 1, 0, 29.50);
	}

	public void testHBO_8_22() throws Exception {
		DailyTime dt = new DailyTime(6.3, 14.2, 14.7, 21);
		mpvTestOne("MP-HBO-8", dt, 3, 1, 0, 42.00);
	}

	public void testHBO_8_23() throws Exception {
		DailyTime dt = new DailyTime(13, 21, 21.5, 28);
		mpvTestOne("MP-HBO-8", dt, 3, 1, 0, 42.00);
	}

	public void testHBO_8_24() throws Exception {
		DailyTime dt = new DailyTime(8, 14.9, 15.4, 22);
		mpvTestOne("MP-HBO-8", dt, 2, 1, 0, 29.50);
	}

	public void testHBO_8_25() throws Exception {
		DailyTime dt = new DailyTime(5, 12, 12.5, 19.6, 20.1, 22);
		mpvTestOne("MP-HBO-8", dt, 2, 2, 0, 42.00);
	}

	public void testHBO_8_26() throws Exception {
		DailyTime dt = new DailyTime(6, 13, 13.5, 22);
		mpvTestOne("MP-HBO-8", dt, 2, 4, 0, 67.00);
	}

	public void testHBO_8_27() throws Exception {
		DailyTime dt = new DailyTime(5.5, 15.4, 15.9, 22.7);
		mpvTestOne("MP-HBO-8", dt, 6, 2, 0, 92.00);
	}

	public void testHBO_8_28() throws Exception {
		DailyTime dt = new DailyTime(8.7, 16.3, 16.8, 25);
		mpvTestOne("MP-HBO-8", dt, 3, 3, 0, 67.00);
	}

	public void testHBO_8_29() throws Exception {
		DailyTime dt = new DailyTime(7, 15.2, 15.7, 23);
		mpvTestOne("MP-HBO-8", dt, 3, 2, 0, 54.50);
	}

	public void testHBO_8_30() throws Exception {
		DailyTime dt = new DailyTime(6.1, 14.2, 14.7, 24);
		mpvTestOne("MP-HBO-8", dt, 3, 5, 0, 92.00);
	}

	// ==============================================================================
	// End of MP-HBO-8


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
	 * @param ruleKey
	 *            The MpvRule key (the rule "name").
	 * @param dt
	 *            The DailyTime entity with call, wrap, and meal times set.
	 * @param amMpvCount
	 *            The expected number of MPVs for the first meal period.
	 * @param pmMpvCount
	 *            The expected number of MPVs for the second meal period.
	 * @param expectedPay
	 *            The expected total penalty for all MPVs.
	 */
	private void mpvTestOne(String ruleKey, DailyTime dt, int amMpvCount,
			int pmMpvCount, int wrapMpvCount, double expectedPay) {

		dt.setDate(new Date());
		BigDecimal calculatedPay = calculateMpvsTest(
				ruleKey, dt, straightRate, prevailingRate);

		int calculatedAmCount = dt.getMpv1Payroll() == null ? 0 : (int) dt
				.getMpv1Payroll();
		int calculatedPmCount = dt.getMpv2Payroll() == null ? 0 : (int) dt
				.getMpv2Payroll();
		int calculatedWrapCount = dt.getMpv3Payroll() == null ? 0 : (int) dt
				.getMpv3Payroll();

		assertEquals("Number of MPVs for A.M.", amMpvCount, calculatedAmCount);
		assertEquals("Number of MPVs for P.M.", pmMpvCount, calculatedPmCount);
		assertEquals("Number of MPVs for Wrap", wrapMpvCount, calculatedWrapCount);

		BigDecimal expectedPayDec = new BigDecimal(expectedPay);
		assertTrue("MPV pay: expected " + expectedPayDec + ", calc'd "
				+ calculatedPay, expectedPayDec.compareTo(calculatedPay) == 0);

	}

	/**
	 * Method used to test MPV rule evaluation. This may be called, for example,
	 * from a JUnit test class. It results in evaluating a single day,
	 * determining the number of MPVs and the total dollar value.
	 *
	 * @param ruleKey The rule name (key) to be used in the evaluation.
	 * @param dt The DailyTime object to be evaluated. The mpv1Payroll and
	 *            mpv2Payroll fields will be set as a result of the calculation.
	 * @param straightRate The employee's "straight rate", which may be used
	 *            in determining the total dollar value of the meal penalties if
	 *            the specified rule uses that method.
	 * @param prevailingRate The employee's "prevailing rate", which may be used
	 *            in determining the total dollar value of the meal penalties if
	 *            the specified rule uses that method.
	 * @return The total dollar value of the MPVs for the given DailyTime
	 *         settings.
	 */
	public static BigDecimal calculateMpvsTest(String ruleKey, DailyTime dt,
			BigDecimal straightRate, BigDecimal prevailingRate) {
		BigDecimal value = BigDecimal.ZERO;

		// get the rules using the supplied rule key
		List<MpvRule> mpvRules = MpvRuleDAO.getInstance().findByRuleKey(ruleKey);

		// Apply the rule(s) to the given DailyTime; fills in counts
		MpvService.applyMpvRule(dt, mpvRules, null);

		// Calculate the meal penalty amount
		value = PayBreakdownService.calculateMpvPenaltyTest(mpvRules,
				dt.getMpv1Payroll(), dt.getMpv2Payroll(),  dt.getMpv3Payroll(), straightRate, prevailingRate);

		return value;
	}

}
