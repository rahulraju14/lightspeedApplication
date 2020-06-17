package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.DoodReport;

/**
 * A data access object (DAO) providing persistence and search support for
 * DoodReport entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DoodReport
 */

public class DoodReportDAO extends BaseTypeDAO<DoodReport> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DoodReportDAO.class);
	// property constants
	private static final String REPORT_ID = "reportId";
//	private static final String SEGMENT = "segment";
//	private static final String TYPE = "type";
//	private static final String TYPE_NAME = "typeName";
//	private static final String SEQUENCE = "sequence";
//	private static final String PERSON = "person";
//	private static final String CAST_ID = "castId";
//	private static final String ELEMENT_NAME = "elementName";
//	private static final String DAY1 = "day1";
//	private static final String DAY2 = "day2";
//	private static final String DAY3 = "day3";
//	private static final String DAY4 = "day4";
//	private static final String DAY5 = "day5";
//	private static final String DAY6 = "day6";
//	private static final String DAY7 = "day7";
//	private static final String DAY8 = "day8";
//	private static final String DAY9 = "day9";
//	private static final String DAY10 = "day10";
//	private static final String DAY11 = "day11";
//	private static final String DAY12 = "day12";
//	private static final String DAY13 = "day13";
//	private static final String DAY14 = "day14";
//	private static final String DAY15 = "day15";
//	private static final String DAY16 = "day16";
//	private static final String DAY17 = "day17";
//	private static final String DAY18 = "day18";
//	private static final String DAY19 = "day19";
//	private static final String DAY20 = "day20";
//	private static final String DAY21 = "day21";
//	private static final String WORK = "work";
//	private static final String HOLD = "hold";
//	private static final String TRAVEL = "travel";
//	private static final String HOLIDAY = "holiday";
//	private static final String TOTAL = "total";

	public static DoodReportDAO getInstance() {
		return (DoodReportDAO)getInstance("DoodReportDAO");
	}

	public List<DoodReport> findByReportId(Object reportId) {
		return findByProperty(REPORT_ID, reportId);
	}

//	public static DoodReportDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (DoodReportDAO) ctx.getBean("DoodReportDAO");
//	}

}