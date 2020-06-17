//	File Name:	StripboardReportGen.java
package com.lightspeedeps.util.report;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripColorDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.StripboardReportDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.script.StripUtils;
import com.lightspeedeps.web.report.ReportBean;

/**
 * This class creates the records in the stripboard_report table which will be
 * used by Jasper to generate a stripboard report. A JUnit test for it is in in
 * the StripboardReportGenTest class.
 */
public class StripboardReportGen {
	private static final Log log = LogFactory.getLog(StripboardReportGen.class);

	private final DateFormat longDateFormat = new SimpleDateFormat("MMM d, yyyy");
	private final DateFormat shortDateFormat = new SimpleDateFormat("M/d");

	private transient StripboardReportDAO reportDAO;
	private transient StripDAO stripDAO;
	private transient SceneDAO sceneDAO;

	private Project project;
	private Script script;
	private Stripboard stripboard;
	private String reportId;

	/** A mapping from Unit id values to their corresponding Unit number. */
	private final Map<Integer, Integer> unitNumberMap = new HashMap<Integer, Integer>();

	/** A mapping from Unit id values to their corresponding Unit name. */
	private final Map<Integer, String> unitNameMap = new HashMap<Integer, String>();

	private int sequence;

	public StripboardReportGen() {
	}

	/**
	 * Generates all the rows of data required for a Stripboard report, and
	 * place them into the database. Examples of calls: <br>
	 * Full report in schedule order (does NOT include unscheduled strips): <br>
	 * &nbsp; generate("myreportid", true, null, null, 0, 0, false); <br>
	 * Full report in sheet order: <br>
	 * &nbsp; generate("myreportid", true, null, null, 0, 0, true); <br>
	 * Partial report by date range: <br>
	 * &nbsp; generate("myreportid", false, <start date>, <end date>, 0, 0,
	 * false); <br>
	 * Partial report by sheet numbers: <br>
	 * &nbsp; generate("myreportid", false, null, null, 10, 35, true); <br>
	 * <br>
	 *
	 * @param reptId The report_id field to be stored in all records of this
	 *            report.
	 *
	 * @param includeStrips The ReportBean button selection indicating which
	 *            Strip's to include in the report: scheduled, unscheduled, or
	 *            all.
	 *
	 * @param unit Which Unit's stripboard tab will be used to determine the set
	 *            of Strip's in the report. If null, then Strip's from all
	 *            Unit's will be included.
	 *
	 * @param startDate The starting (inclusive) date for a range of (scheduled)
	 *            strips to be included in the report. If a date range is
	 *            specified, both start and end dates must be supplied, and the
	 *            report must be ordered by schedule (not sheet number). Only
	 *            SCHEDULED Strip's are included.
	 *
	 * @param endDate The ending (inclusive) date for a range of (scheduled)
	 *            strips to be included in the report.
	 *
	 * @param orderByScene True: order the report by scene number. In this case,
	 *            the startDate and endDate fields must be null, because a date
	 *            range implies order by schedule.
	 *
	 * @param pProject The Project whose current Stripboard will be used to
	 *            generate the report.
	 *
	 * @return True if any report data was generated. False if the current
	 *         project does not have both a script and a stripboard created, or
	 *         if there was no data in the range specified.
	 *
	 * @throws IllegalArgumentException if the parameters are invalid or
	 *             conflicting.
	 */
	public boolean generate(String reptId, String includeStrips,
			Unit unit, Date startDate, Date endDate,
			boolean orderByScene, Project pProject ) throws IllegalArgumentException {
		log.debug("generation starting, params: "+ reptId + "/" + startDate + "/" + endDate + "/" +
				orderByScene + "/" + includeStrips + "/");

		project = pProject;
		if (unit == null) {
			unit = project.getMainUnit();
		}
		reportId = reptId;

		stripboard = project.getStripboard();
		script = project.getScript();
		if (stripboard == null || script == null) {
			return false;	// not enough data in project yet!
		}

		if ( (startDate != null && endDate == null) ||
				(startDate == null && endDate != null) ||
				(startDate != null && startDate.after(endDate)) ||
				(reptId == null) ||
				(orderByScene && startDate != null)
				) {
			throw new IllegalArgumentException(
					"params: "+ reptId + "/" + startDate + "/" + endDate + "/" +
					orderByScene+ "/" + includeStrips + "/");
		}

		for (Unit u : project.getUnits()) {
			unitNumberMap.put(u.getId(), u.getNumber());
			unitNameMap.put(u.getId(), u.getName());
		}

		boolean bRet;
		sequence = 1;
		if (includeStrips.equals(ReportBean.INCLUDE_STRIPS_SCHEDULED)) {
			bRet = generateScheduledStrips(unit, startDate, endDate); // this does it all
		}
		else if (includeStrips.equals(ReportBean.INCLUDE_STRIPS_UNSCHEDULED)) {
			bRet = generateUnscheduledStrips();

		}
		else {
			bRet = generateAllStrips(orderByScene);

		}
		log.debug("generation completed for " + reptId + ", ret=" + bRet);
		return bRet;
	}

	private boolean generateScheduledStrips(Unit unit, Date startDate, Date endDate) {
		Collection<Strip> strips;
		strips = getStripDAO().findByUnitAndStripboard(unit, stripboard);
		Calendar cal = new GregorianCalendar();
		cal.setTime(unit.getProjectSchedule().getStartDate());
		ScheduleUtils schedBean = new ScheduleUtils(unit);

		if (schedBean.findDayType(cal.getTime()) != WorkdayType.WORK) {
			cal = schedBean.findNextWorkDate(cal);
		}
		int shootDay = 1;
		int totalPages = 0;
		int stripCount = 0;
		for (Strip strip : strips) {
			// Accumulate & set page length for end-of-day strips
			if (strip.getType() == StripType.END_OF_DAY) {
				strip.setLength(totalPages);
				totalPages = 0;
			}

			if (strip.getType() != StripType.END_STRIPBOARD) {
				if (startDate != null) { // shooting date range - only Scheduled strips print
					if ( ! startDate.after(cal.getTime()) &&
							! endDate.before(cal.getTime())) {
						if (createStrip(strip, cal, shootDay)) {
							if (strip.getType() == StripType.BREAKDOWN) {
								totalPages += strip.getLength();
							}
							stripCount++;
						}
					}
				}
				else { // no range restriction
					if (createStrip(strip, cal, shootDay)) {
						if (strip.getType() == StripType.BREAKDOWN) {
							totalPages += strip.getLength();
						}
						stripCount++;
					}
				}
			}
			if (strip.getType() == StripType.END_OF_DAY) {
				shootDay++;
				cal = schedBean.findNextWorkDate(cal);
				if (cal == null) { // really shouldn't happen!
					EventUtils.logError("ScheduleUtils.getNextWorkDate returned null!");
					break;
				}
			}
		}
		return (stripCount > 0);
	}

	/**
	 * Generate the stripboard report records required for an "Unscheduled Strips"
	 * report.  This includes Omitted strips as well.
	 * @return true iff at least one Strip qualified to be in the report.
	 */
	private boolean generateUnscheduledStrips() {
		Collection<Strip> strips = getStripDAO().findByStatusAndStripboard(StripStatus.UNSCHEDULED, stripboard);
		List<Strip> omitted = getStripDAO().findByStatusAndStripboard(StripStatus.OMITTED, stripboard);
		strips.addAll(omitted);
		int stripCount = 0;
		for (Strip strip : strips) {
			if (strip.getType() != StripType.END_STRIPBOARD) {
				if (createStrip(strip, null, 1)) {
					stripCount++;
				}
			}
		}
		return (stripCount > 0);
	}

	/**
	 * Generate the stripboard report records required for an "All Strips"
	 * report. The report may have been requested in either Scene or Date order.
	 *
	 * @param orderByScene True if the generated report will be ordered by Scene
	 *            number, false if it will be ordered by shooting date.
	 * @return true iff at least one Strip qualified to be in the report.
	 */
	private boolean generateAllStrips(boolean orderByScene) {
		if (orderByScene) {
			return generateAllStripsByScene();
		}

		// Report is all Strips ordered by shooting date.
		// The data will be generated unit-by-unit for all scheduled Strips,
		// then the unscheduled and omitted Strips will be generated.
		Calendar cal = new GregorianCalendar();
		int stripCount = 0;

		for (Unit unit : project.getUnits()) {
			Collection<Strip> strips = getStripDAO().findByUnitAndStripboard(unit, stripboard);
			if (strips.size() > 0) {
				// Each unit has its own calendar - build the schedule.
				ScheduleUtils schedBean = new ScheduleUtils(unit);
				cal.setTime(unit.getProjectSchedule().getStartDate());
				if (schedBean.findDayType(cal.getTime()) != WorkdayType.WORK) {
					cal = schedBean.findNextWorkDate(cal);
				}
				int shootDay = 1;
				int totalPages = 0;
				for (Strip strip : strips) {
					// Accumulate & set page length for end-of-day strips
					if (strip.getType() == StripType.END_OF_DAY) {
						strip.setLength(totalPages);
						totalPages = 0;
					}

					if (strip.getType() != StripType.END_STRIPBOARD) {
						if (createStrip(strip, cal, shootDay)) {
							if (strip.getType() == StripType.BREAKDOWN) {
								totalPages += strip.getLength();
							}
							stripCount++;
						}
					}
					if (strip.getType() == StripType.END_OF_DAY) {
						shootDay++;
						cal = schedBean.findNextWorkDate(cal);
						if (cal == null) { // really shouldn't happen!
							IllegalArgumentException ex = new IllegalArgumentException("ScheduleUtils.findNextWorkDate() returned null");
							EventUtils.logError(ex);
							break;
						}
					}
				}
			}
		}

		// Now generate all the unscheduled & omitted strips
		boolean ret = generateUnscheduledStrips();

		return (ret || stripCount > 0);
	}

	private boolean generateAllStripsByScene() {
		Collection<Strip> strips = getStripDAO().findAllBreakdownStrips(stripboard);
		int stripCount = 0;
		for (Strip strip : strips) {
			if (createStrip(strip, null, 1)) {
				stripCount++;
			}
		}
		return (stripCount > 0);
	}

	/**
	 * Create the report strip corresponding to the supplied Strip, and save it to the database. If
	 * the strip is an "orphan" -- that is, it refers to a scene that does not exist in the current
	 * script -- then no report strip is added to the database.
	 * <p>
	 * The calendar and shootDay information is used to fill in end-of-day strip text.
	 *
	 * @param strip The Strip to be formatted.
	 *
	 * @param calendar Calendar representing the date of this strip. This is only referenced for
	 *            End-of-day strips and may be null for other strip types.
	 *
	 * @param shootDay The shooting day number associated with this strip. This is only used for
	 *            End-of-day strips.
	 *
	 */
	private boolean createStrip(Strip strip, Calendar calendar, int shootDay) {
		//log.debug("day=" + shootDay + ", strip=" + strip.getSceneNumbers() + ", " + strip.toString());
		StripboardReport report = new StripboardReport();
		report.setReportId(reportId);
		report.setSegment(1);
		report.setSequence(sequence++);
		report.setStatus(strip.getStatus().toString());
		if (strip.getStatus() == StripStatus.SCHEDULED) {
			report.setIStatus(unitNumberMap.get(strip.getUnitId()));
			report.setStatus(unitNameMap.get(strip.getUnitId()));
		}
		else if (strip.getStatus() == StripStatus.UNSCHEDULED) {
			report.setIStatus(StripboardReport.ISTATUS_UNSCHEDULED);
		}
		else {
			report.setIStatus(StripboardReport.ISTATUS_OMITTED);
		}
		report.setType(strip.getType().toString());
		report.setTextRgb(StripColorDAO.BLACK_RGB);
		switch (strip.getType()) {
			case BANNER :
				report.setLocation(strip.getComment());
				report.setBackgroundRgb(StripColorDAO.COLOR_BANNER_RGB);
				break;
			case END_OF_DAY:
				report.setPages(Scene.formatPageLength(strip.getLength()));
				report.setLocation("End of Day #"+shootDay);
				report.setScenes(shortDateFormat.format(calendar.getTime()));
				report.setCastIds(longDateFormat.format(calendar.getTime()));
				report.setBackgroundRgb(StripColorDAO.COLOR_END_OF_DAY_RGB);
				break;
			case BREAKDOWN:
				fillBreakdown(report, strip);
				if (report.getScenes() == null) {
					report = null;	// an "orphan strip", don't put in report
				}
				break;
			case END_STRIPBOARD:
				// ignore
				break;
		}
		boolean bRet = false;
		if (report != null) {
			getReportDAO().save(report);
			bRet = true;
		}
		return bRet;
	}

	/**
	 * Fills in the report object with information from the Strip, for BREAKDOWN
	 * type Strip objects.
	 * @param report The report strip to be filled in.
	 * @param strip The Strip object whose information is to be placed into the report strip.
	 */
	private void fillBreakdown(StripboardReport report, Strip strip) {
		report.setSheet(strip.getSheetNumber());
		List<Scene> sceneList = getSceneDAO().findByNumbersAndScript(strip.getScenes(), script);
		log.debug("scene count="+sceneList.size());
		if (sceneList != null && sceneList.size() > 0) {
			Scene scene = sceneList.get(0);
			if (scene != null) {
				report.setSynopsis(scene.getSynopsis());
				report.setScriptOrder(scene.getSequence());
				report.setDayNight(scene.getDnType().toString());
				report.setIntExt(scene.getShortIeType());
				StripColor stripColor = StripUtils.getStripColor(scene.getIeType(), scene.getDnType());
				if (stripColor != null) {
					report.setBackgroundRgb(stripColor.getBackgroundRgb());
					report.setTextRgb(stripColor.getTextRgb());
				}
				else {
					report.setBackgroundRgb(StripColorDAO.WHITE_RGB);
				}
				report.setScriptDay(scene.getScriptDay());
				if (scene.getScriptElement() != null) {
					report.setLocation(scene.getScriptElement().getName());
				}
				report.setPages(Scene.formatPageLength(strip.getLength()));
				report.setScenes(strip.getSceneNumbers());
				// get an ordered list of elementIds, including non-Character entries
				report.setCastIds(ScriptElementDAO.createElementIdString(getScriptElements(sceneList), false));
			}
		}
	}

	/**
	 * Given a collection of Scene objects, return a Set which is the union of
	 * all the (distinct) ScriptElements used by all the scenes.
	 * @param scenes The Collection of Scenes whose ScriptElements are to be gathered.
	 * @return A Set of all those scene's scriptElements.
	 */
	public static Set<ScriptElement> getScriptElements(Collection<Scene> scenes) {
		Set<ScriptElement> elements = new HashSet<ScriptElement>();
		for (Scene scene : scenes) {
			elements.addAll(scene.getScriptElements());
		}
		return elements;
	}

	/** See {@link #reportDAO}. */
	public StripboardReportDAO getReportDAO() {
		if (reportDAO == null) {
			reportDAO = StripboardReportDAO.getInstance();
		}
		return reportDAO;
	}

	/** See {@link #stripDAO}. */
	public StripDAO getStripDAO() {
		if (stripDAO == null) {
			stripDAO = StripDAO.getInstance();
		}
		return stripDAO;
	}

	/** See {@link #sceneDAO}. */
	public SceneDAO getSceneDAO() {
		if (sceneDAO == null) {
			sceneDAO = SceneDAO.getInstance();
		}
		return sceneDAO;
	}

}
