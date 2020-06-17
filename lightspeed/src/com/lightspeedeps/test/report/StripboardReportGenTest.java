//	File Name:	StripboardReportGenTest.java
package com.lightspeedeps.test.report;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.util.report.StripboardReportGen;
import com.lightspeedeps.web.report.ReportBean;

/**
 * A class to test the StripboardReportGen class.
 * This will generate data into the stripboard_report table for multiple reports, and also
 * attempt calls to the generator which should throw invalid parameter exceptions.
 *
 * If the generator throws the appropriate exceptions, debug log messages are written to
 * document that.  If the generator does NOT throw the proper exceptions, then this code
 * will throw a RunTime exception.
 *
 * To run all the tests, simply invoke the static method:
 * 		StripboardReportGenTest.testGenerate();
 *
 */
public class StripboardReportGenTest extends SpringMultiTestCase {
	private static final Log log = LogFactory.getLog(StripboardReportGenTest.class);

	public void testGenerate() {
		// *********** TEST CODE to generate Stripboard Report data ************
		StripboardReportGen gen = new StripboardReportGen();// (StripboardReportGen)ServiceFinder.findBean("StripboardReportGen");
		DateFormat df = new SimpleDateFormat("yyMMddHHmss");
		Date startDate = null;
		Date endDate = null;
		String reptId;
		boolean bRet;

		Project project = ProjectDAO.getInstance().findById(3);

		reptId = "all-sched"+df.format(new Date());
		bRet = gen.generate(reptId, ReportBean.INCLUDE_STRIPS_SCHEDULED,
				null, // UNIT
				startDate, endDate,
				false /*orderBySheet*/,
				project );
		assertTrue("generate returned false", bRet);

		reptId = "all-sheet"+df.format(new Date());
		bRet = gen.generate(reptId, ReportBean.INCLUDE_STRIPS_SCHEDULED,
				null, // UNIT
				startDate, endDate,
				true /*orderBySheet*/ ,
				project );
		assertTrue("generate returned false", bRet);

		startDate = project.getMainUnit().getProjectSchedule().getStartDate();
		Calendar cal = new GregorianCalendar();
		cal.setTime(startDate);
		cal.add(Calendar.DAY_OF_MONTH, 5);
		endDate = cal.getTime();
		reptId = "day1to5-"+df.format(new Date());
		bRet = gen.generate(reptId, ReportBean.INCLUDE_STRIPS_SCHEDULED,
				null, // UNIT
				startDate, endDate,
				false /*orderBySheet*/ ,
				project );
		assertTrue("generate returned false", bRet);

		// Test parameter validation

		// - fail due to only start date supplied
		bRet = testFailure(gen, "fail-1-"+df.format(new Date()),
				startDate, null, project,
				false /*orderBySheet*/ );
		assertTrue("testFailure 1 returned false", bRet);

		// - fail due to "orderBySheet"=true and dates supplied
		bRet = testFailure(gen, "fail-2-"+df.format(new Date()),
				startDate, endDate, project,
				true /*orderBySheet*/ );
		assertTrue("testFailure 2 returned false", bRet);

		// - fail due to only end date supplied
		bRet = testFailure(gen, "fail-3-"+df.format(new Date()),
				null, endDate, project,
				true /*orderBySheet*/ );
		assertTrue("testFailure 3 returned false", bRet);

		// - fail due to start date later than end date
		bRet = testFailure(gen, "fail-4-"+df.format(new Date()),
				endDate, startDate, project,
				true /*orderBySheet*/ );
		assertTrue("testFailure 4 returned false", bRet);

		// ** end DooD report test code **
	}

	private static boolean testFailure(StripboardReportGen gen,
			String reptId,
			Date startDate, Date endDate,
			Project project,
			boolean orderBySheet ) {
		try {
			gen.generate(reptId, ReportBean.INCLUDE_STRIPS_SCHEDULED,
					null,
					startDate, endDate, orderBySheet,
					project );
		}
		catch (IllegalArgumentException e) {
			log.debug("Stripboard report generation properly caught parameter error for test "+reptId);
			return true;
		}
		log.error("Stripboard report generation did not catch parameter error for test "+reptId);
		return false;
	}

}
