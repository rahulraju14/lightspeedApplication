/**
 * File: TimecardCreatorTest.java
 */
package com.lightspeedeps.test.payroll;

import java.util.Date;

import com.lightspeedeps.batch.TimecardCreator;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.test.SpringTestCase;


/**
 * Test the "TimecardCreator" class, which creates new timecards based
 * on existing StartForm records.  The class is normally run as a scheduled
 * task, e.g., on a daily basis, to ensure that all necessary timecards
 * have been created.
 */
public class TimecardCreatorTest extends SpringTestCase {

	TimecardCreator bean;

	public void testCreateTimecards() throws Exception {

		bean = (TimecardCreator)ctx.getBean("timecardCreator");

		Production prod = null;
		Date date = null;

//		date = new Date("June 21, 2014");
//		Integer id = 1973;
//		prod = ProductionDAO.getInstance().findById(id);

		int result = bean.execute(prod, true, false, date);
		if (result >= 0) { // any non-negative value is success
			result = 0;	// set for assert.
		}
		assertEquals("test failed", 0, result);

	}

}
