//	File Name:	DoodReportGen.java
package com.lightspeedeps.util.report;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DoodReportDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.ScheduleUtils;

import static com.lightspeedeps.type.WorkdayType.*;

/**
 * A class for generating the Day-out-of-Days (DooD) information to
 * fill the dood_report table.
 */
public class DoodReportGen implements Serializable {
	/** */
	private static final long serialVersionUID = 106522358374365465L;

	private static final Log log = LogFactory.getLog(DoodReportGen.class);

	private final DateFormat monthFormat = new SimpleDateFormat("MMM"); // short month name
	private final DateFormat weekdayFormat = new SimpleDateFormat("EEE"); // short weekday name

	private final DoodReportDAO rptDAO;
	private Project project;
	private ScheduleUtils schedBean;
	private Date projectStartDate;
	private Date reportStartDate;
	private String reportId;
	private int shootingDays;
	private int dateBreaks = 0; // number of 3-week sections required
	private boolean categoryBreak;
	private Map<String,Integer> typeSegmentMap;

	public DoodReportGen() {
		rptDAO = DoodReportDAO.getInstance();
	}

	/**
	 * Generate all the records needed to create a DooD report.  The records
	 * will all have the given reportId.
	 *
	 * @param reptId The reportId value for all the records in this report.
	 *
	 * @param catBreak  True: Do a page break each time the category
	 * (ScriptElementType) changes.
	 *
	 * @return False iff the project does not have an active Stripboard.
	 */
	public boolean generate(String reptId, Unit unit, boolean catBreak) {
		log.debug("generation starting for report "+reptId);
		reportId = reptId;
		categoryBreak = catBreak;
		typeSegmentMap = new HashMap<String,Integer>(20);
		project = SessionUtils.getCurrentProject();

		// force update of DooD information in case anything changed
		ProductionDood.updateProjectDood(unit);

		// Get project start date...
		projectStartDate = unit.getProjectSchedule().getStartDate();
		// ...then backup to prior Sunday for start of report output
		Calendar cal = new GregorianCalendar();
		cal.setTime(projectStartDate);
		if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
			cal.add(Calendar.DAY_OF_MONTH, Calendar.SUNDAY-cal.get(Calendar.DAY_OF_WEEK));
		}
		reportStartDate = cal.getTime();

		if (project.getStripboard()==null) {
			return false; // can't do a DooD without a stripboard
		}
		shootingDays = project.getStripboard().getShootingDays(unit);
		schedBean = new ScheduleUtils(unit);

		final ScriptElementDAO seDAO = ScriptElementDAO.getInstance();
		final ContactDAO contactDAO = ContactDAO.getInstance();
		Contact contact;
		int lastSegment = 0;
		if (!categoryBreak) {
			dateBreaks = createHeaders(0, "Heading");	// create header records for report;
			// the returned 'last segment' value also equals the number of date breaks.
		}

		int segment = 0;
		for ( ScriptElementType type : ScriptElementType.values()) {
			if (type != ScriptElementType.N_A) {
				typeSegmentMap.put(type.toRwString(), segment+1); // save starting segment (page) for each type
				if (categoryBreak) {
					lastSegment = createHeaders(segment, type.toRwString());
					dateBreaks = lastSegment - segment;
				}
				// For each element type, we need to create as many subheadings as there
				// are date-break (3-week) sections.
				createSubHeads(segment, type, dateBreaks);
				segment = lastSegment;
			}
		}

		// Get all the Script elements for this project
		List<ScriptElement> seList = seDAO.findByProject(project);

		DoodReport item;
		ElementDood eDood;
		for (ScriptElement se : seList) {
			eDood = se.getElementDood(unit);
			item = new DoodReport();
			if (eDood.getWorkDays() > 0) { // skip elements with zero working days
				//log.debug("have element, workdays ="+eDood.getWorkDays());
				item.setReportId(reportId);
				item.setFromElementDood(eDood); // copy days worked, start/end, etc.
				item.setElementName(se.getName()); // ScriptElement name is Character name
				item.setScriptElementId(se.getId()); // ScriptElement Id (GnG)
				item.setTypeName(se.getType().toRwString());
				item.setType(se.getType().ordinal());
				if (se.getElementId() != null) {
					item.setCastIdNum(se.getElementId());
					item.setCastId(se.getElementIdStr());
				}
				if (se.getType()==ScriptElementType.CHARACTER) {
					contact = contactDAO.findContactFromCharacter(se);
					if (contact != null) {
						item.setPerson(contact.getDisplayName());
					}
				}
				setDays(item, eDood); // sets daily status values & saves item(s), one per segment.
			}
		}
		log.debug("generation finished for report "+reptId);
		return true;
	}

	/**
	 * Set the daily status for the element passed (eDood), for all days, from the
	 * project start date until the end of shooting.  This will save one item
	 * (doodReport record) for each print segment.  The segment number and sequence
	 * numbers are set here; all other fields should be set in the item before this
	 * method is called.
	 *
	 * @param item The report item (row) with day counts, start/end dates, etc., all filled in.
	 * @param eDood The ElementDood corresponding to the element being displayed on this row.
	 */
	private void setDays(DoodReport item, ElementDood eDood) {
		Date date;
		int daynum;
		int doneDays = 0;
		int sequence = 1;
		WorkdayType dayType;

		Calendar cal = new GregorianCalendar();
		cal.setTime(reportStartDate);
		//schedBean.createDaysOffList();

		int firstSegment = 1;
		Integer oseg = typeSegmentMap.get(item.getTypeName());
		if (oseg != null) {
			firstSegment = oseg.intValue();
		}
		int segment = firstSegment;

		for ( ; doneDays < shootingDays; segment++) {
			if (segment > firstSegment) {  // need a new item for each segment (next 21 days)
				item = newCopy(item);
			}
			item.setSegment(segment);
			item.setSequence(sequence);
			// Cycle through the calendar until we get enough shooting days
			for (daynum = 1; daynum <= 21; daynum++ ) {
				date = cal.getTime();
				dayType = schedBean.findDayType(date);
				if ( ! date.before(projectStartDate)) {
					item.setDay(daynum, eDood.getStatus(date).asWorkStatus());
					if (dayType.isWork()) {
						doneDays++;
					}
					else { // for non-work days, set a code for Jasper to format cell
						if (dayType == HOLIDAY) {
							item.setDay(daynum, "1");
						}
						else if (dayType == OFF) {
							item.setDay(daynum, "2");
						}
						else { // should be Travel
							item.setDay(daynum, "3");
						}
					}
				}
				else if (dayType == OFF) {
					item.setDay(daynum, "2");
				}
				else if (dayType == PREP && schedBean.isWeeklyDayOff(date)) {
					item.setDay(daynum, "2");
				}
				nextDay(cal); // update calendar to next day
			}
			rptDAO.save(item); // This row is done
		}
	}

	/**
	 * Update the given calendar to the next sequential day.  If the month
	 * changes, also call the schedule bean to update the month's list of daily status.
	 * @param cal The calendar to be updated.
	 */
	private void nextDay(Calendar cal) {
		int currentMonth = cal.get(Calendar.MONTH);
		cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
		if (currentMonth != cal.get(Calendar.MONTH)) {
			// when the month changes, we need to rebuild the schedule info...
			currentMonth = cal.get(Calendar.MONTH);
			//schedBean.createMonthDaysOffList(cal.get(Calendar.YEAR), currentMonth);
		}
	}

	/**
	 * Create the three header rows required for each "segment" of 21 days.
	 * The first header needs the starting month name, and the days of the
	 * month filled in.  The second header needs the shooting day numbers,
	 * plus indicators for "off" and Holiday days.
	 * If page breaks are being done for every category type, then we need
	 * a set of 3 headers for each of those.
	 */
	private int createHeaders(int segment, String typename) {
		Date date;
		WorkdayType dayType;
		DoodReport header1, header2, header3;
		int daynum;
		int doneDays = 0;

		Calendar cal = new GregorianCalendar();
		cal.setTime(reportStartDate);
		//schedBean.createMonthDaysOffList(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));

		for ( ; doneDays < shootingDays; ) {
			segment++;
			header1 = newHeader(segment, -3, typename, monthFormat.format(cal.getTime()));
			header2 = newHeader(segment, -2, typename, "");
			header3 = newHeader(segment, -1, typename, "Shoot day:");

			// Cycle through the calendar for all 21 days in each segment
			for (daynum = 1; daynum <= 21; daynum++ ) {
				date = cal.getTime();
				header1.setDay(daynum, ""+cal.get(Calendar.DAY_OF_MONTH));
				header2.setDay(daynum, weekdayFormat.format(date));
				dayType = schedBean.findDayType(date);
				if ( ! date.before(projectStartDate)) {
					if (dayType.isWork()) {
						if (doneDays < shootingDays) {
							doneDays++;
							header3.setDay(daynum, ""+doneDays);
						}
					}
					else {
						if (dayType == HOLIDAY) {
							header3.setDay(daynum, "H");
						}
						else if (dayType == COMPANY_TRAVEL ||
								dayType == OTHER_TRAVEL) {
							header3.setDay(daynum, "T");
						}
						else {
							header3.setDay(daynum, "off");
						}
					}
				}
				else if (dayType == OFF) {
					header3.setDay(daynum, "off");
				}
				else if (dayType == PREP && schedBean.isWeeklyDayOff(date)) {
					header3.setDay(daynum, "off");
				}
				nextDay(cal); // update calendar to next day
			}
			rptDAO.save(header1); // all 3 headers are done for this segment - save them
			rptDAO.save(header2);
			rptDAO.save(header3);
		}
		return segment;
	}

	/**
	 * Generate a pair of rows for an element type, for as many date breaks as
	 * specified.  The pair consists of a sub-heading row and a trailing "spacer"
	 * row.  The sub-heading row has the type-name in the "person" field.
	 * @param segment The segment this subhead belongs to.
	 * @param type  The ScriptElementType we're creating the subhead for.
	 * @param breaks The number of copies to make -- each date section (of 3 weeks)
	 * 		requires one of these pairs for each element type.
	 */
	private void createSubHeads(int segment, ScriptElementType type, int breaks) {
		DoodReport header1;

		for (int i = 1; i <= breaks; i++) {
			segment++;
			// build & save subheading row
			header1 = newHeader(segment, 0, type.toRwString(), "");
			header1.setType(type.ordinal());
			header1.setPerson(type.toRwString());
			rptDAO.save(header1);

			// build & save blank trailing separator row
			header1 = newHeader(segment, 999999, type.toRwString(), "");
			header1.setType(type.ordinal());
			rptDAO.save(header1); // save blank separator row
		}

	}

	/**
	 * Instantiate a new header row, setting all the standard values, and setting
	 * the passed parameters into the appropriate row fields.
	 * @param segment Current segment number, 1...n
	 * @param seq Sequence number for this header, 1...n
	 * @param name String to be placed in the "elementName" column.
	 * @return New DoodReport instance set up as a header row.
	 */
	private DoodReport newHeader(int segment, int seq, String typename, String name) {
		DoodReport header;
		header = new DoodReport();
		header.setReportId(reportId);
		header.setSegment(segment);
		header.setType(-1);
		header.setTypeName(typename);
		header.setSequence(seq);
		header.setElementName(name);
		return header;
	}

	/**
	 * Creates a new instance of a DoodReport object, with most of the fields
	 * set to the same values as the given DoodReport object. Fields NOT copied
	 * are the segment, sequence, and day1-day21.
	 *
	 * @param item The object to be copied.
	 * @return A new instance which is largely a copy of the supplied object.
	 */
	private DoodReport newCopy(DoodReport item) {
		DoodReport newdr = new DoodReport();
		newdr.setReportId(item.getReportId());

		newdr.setCastIdNum(item.getCastIdNum());
		newdr.setCastId(item.getCastId());
		newdr.setElementName(item.getElementName());
		newdr.setPerson(item.getPerson());
		newdr.setType(item.getType());
		newdr.setTypeName(item.getTypeName());

		newdr.setFinish(item.getFinish());
		newdr.setStart(item.getStart());

		newdr.setHold(item.getHold());
		newdr.setHoliday(item.getHoliday());
		newdr.setTravel(item.getTravel());
		newdr.setTotal(item.getTotal());
		newdr.setWork(item.getWork());

		return newdr;
	}


	/**
	 * Generate a pseudo-report consisting of just ScriptElement records for all
	 * the elements in the given Project, with their Start and Finish work
	 * dates. These records are used in a Jasper report sub-query for an Element
	 * report requested with Unit=All.
	 *
	 * @param reptId The reportId to be set in all the report records created.
	 * @param project The project whose ScriptElements will be included in the
	 *            report records.
	 * @return True if the report (set of element records) was generated
	 *         successfully.  False is only returned if (a) the project has
	 *         no script, (b) the project has no stripboard, or (c) a runtime
	 *         error occurs.
	 */
	public boolean generateElementDateReport(String reptId, Project project) {
		boolean bRet = false;
		try {
			if (project.getScript() == null || project.getStripboard() == null) {
				return false; // can't do a DooD without a script & stripboard
			}
			// create map<scene number,shooting date>
			Map<String,Date> sceneDateMap = createSceneDateMap(project);

			// Get all the Script elements for this project
			List<ScriptElement> seList = ScriptElementDAO.getInstance().findByProject(project);
			Integer scriptId = project.getScript().getId();

			Date firstDate;
			Date lastDate;
			Date scDate;
			// for each element E in project
			for (ScriptElement se : seList) {
				firstDate = null;
				lastDate = null;
				SortedSet<Date> dates = new TreeSet<Date>();
				// for all Scene containing E in current script
				for (Scene sc : se.getScenes()) {
					// add Scene's shooting date to Set for each one
					if (sc.getScript() != null && sc.getScript().getId().equals(scriptId)) {
						if ((scDate = sceneDateMap.get(sc.getNumber())) != null) {
							dates.add(scDate);
						}
					}
				}
				// for all Scene where E is the Scene's location (set)
				for (Scene sc : se.getScenesForLocation()) {
					// getScript() should never be null, but we saw cases where it was :(
					if (sc.getScript() != null && sc.getScript().getId().equals(scriptId)) {
						if ((scDate = sceneDateMap.get(sc.getNumber())) != null) {
							dates.add(scDate);
						}
					}
				}
				// save E, 1st-date, last-date (if it has any dates)
				if (dates.size() > 0) {
					//   find earliest date & latest date from Set of dates
					firstDate = dates.first();
					lastDate = dates.last();
					DoodReport item = new DoodReport();
					item.setReportId(reptId);
					item.setScriptElementId(se.getId()); // ScriptElement Id (GnG)
					item.setStart(firstDate);
					item.setFinish(lastDate);
					// other items only needed for debugging:
					item.setElementName(se.getName());
					rptDAO.save(item);
				}
			}
			bRet = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		return bRet;
	}


	/**
	 * Creates a mapping from Scene numbers (Strings) to the Date on which the
	 * Scene will be shot.
	 *
	 * @return A non-null, but possibly empty, Map with an entry for each Scene
	 *         that appears in a scheduled Strip in the current Stripboard.
	 *         Scenes that are not scheduled will not have an entry in the map.
	 */
	private Map<String,Date> createSceneDateMap(Project project) {
		// create empty map
		Map<String,Date> map = new HashMap<String,Date>(100);

		Stripboard stripboard = project.getStripboard();
		if (stripboard == null) {
			return map;
		}

		StripDAO stripDAO = StripDAO.getInstance();

		// for each unit U in project
		for (Unit unit : project.getUnits()) {
			// get 1st shooting date for U
			ScheduleUtils schedule = new ScheduleUtils(unit);
			Date shootDate = schedule.findShootingDay(0); // first shoot date or project start date
			int shootDay = 1;
			// get List<Strip> for U in shooting order
			List<Strip> strips = stripDAO.findByUnitAndStripboard(unit, stripboard);
			// for each Strip ST
			for (Strip strip : strips) {
				// if ST = end of day, get next shooting date
				if (strip.getType() == StripType.END_OF_DAY) {
					shootDate = schedule.findShootingDay(++shootDay);
				}
				// for each scene in ST
				for (String scNum : strip.getScenes()) {
					// add pair of scene number, shooting date
					map.put(scNum, shootDate);
				}
			}
		}
		if (log.isDebugEnabled()) {
			for (Entry<String,Date> e : map.entrySet()) {
				log.debug(e.getKey() + ": " + e.getValue());
			}
		}
		return map;
	}

}
