//	File Name:	ElementDood.java
package com.lightspeedeps.dood;

import static com.lightspeedeps.type.WorkdayType.COMPANY_TRAVEL;
import static com.lightspeedeps.type.WorkdayType.DROP;
import static com.lightspeedeps.type.WorkdayType.FINISH;
import static com.lightspeedeps.type.WorkdayType.HOLD;
import static com.lightspeedeps.type.WorkdayType.HOLIDAY;
import static com.lightspeedeps.type.WorkdayType.NOT_NEEDED;
import static com.lightspeedeps.type.WorkdayType.OFF;
import static com.lightspeedeps.type.WorkdayType.OTHER_TRAVEL;
import static com.lightspeedeps.type.WorkdayType.PICKUP;
import static com.lightspeedeps.type.WorkdayType.PICKUP_FINISH;
import static com.lightspeedeps.type.WorkdayType.START;
import static com.lightspeedeps.type.WorkdayType.START_DROP;
import static com.lightspeedeps.type.WorkdayType.START_FINISH;
import static com.lightspeedeps.type.WorkdayType.WORK;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.object.DatePair;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * ElementDood represents the information usually shown on one row of a DooD
 * report. That is, it contains the work-day status for one script element (most
 * commonly a character) for all the calendar dates of one unit, from the unit
 * schedule's start date, through enough days to include the number of shooting
 * days specified in the current Stripboard.
 * <p>
 * For RealWorldElements, a "project-level" ElementDood will be created (if necessary)
 * to display the full date range of the element's usage across all Unit`s.
 */
public class ElementDood implements Serializable {
	/** */
	private static final long serialVersionUID = - 1513524139616774352L;

	private static final Log log = LogFactory.getLog(ElementDood.class);

	/** The format of the key used in the doodMap.  This is only used internally. */
	private static final String DATE_KEY_FORMAT = "yyyyMMdd";
	@Transient
	private final SimpleDateFormat oSdf = new SimpleDateFormat(DATE_KEY_FORMAT);

	/**
	 * The ScriptElement that this DooD is tracking.  This reference is null'd after
	 * all date status values have been computed, so the storage can be released.
	 */
	@Transient
	private ScriptElement scriptElement;

	/** The scriptElement id, for internal use & debugging. */
	private final Integer scriptElementId;

	/**
	 * The doodMap is keyed by date in yyyyMMdd format (as a String).  There should
	 * be one entry for every day of the project where the item's status is not "OFF".
	 * (Dates with OFF status are not stored in the map.)
	 */
	private HashMap<String,WorkdayType> doodMap = null;

	/**
	 * Number of days this element is working, that is, it appears in a
	 * scene assigned to a shooting day. This will not include HOLD, TRAVEL,
	 * or HOLIDAY days. */
	private int workDays = 0;

	/**
	 * The number of travel days that occur between this element's first day of
	 * work and last day of work. This includes both "COMPANY_TRAVEL" and
	 * "OTHER_TRAVEL" days. (Holidays that occur during the project's scheduled
	 * shooting period, but before this element's first work day, or after its
	 * last work day, will not be included.)
	 */
	private int travelDays = 0;

	/**
	 * The number of days this element is on HOLD status -- days when not
	 * working, but between WORK days, and not including HOLIDAY or TRAVEL days.
	 * If the actor assigned to this element is eligible for DROP/PICKUP, and
	 * the number of consecutive days off is sufficient, the DooD logic will
	 * assign a DROP day and a PICKUP day. In this case, the number of HOLD days
	 * will not include those days between the DROP and PICKUP, and those days
	 * will be marked OFF (display as blank on the DooD report).
	 * <p>
	 * Only applies if "Hold Allowed" is set for the Contact associated with
	 * this script element, or if "DEFAULT_HOLD_ALLOWED" is set to 'true' in the
	 * system parameters.
	 */
	private int holdDays = 0;

	/**
	 * The number of holiday days that occur between this element's
	 * first day of work and last day of work.  (Holidays that occur during
	 * the project's scheduled shooting period, but before this element's
	 * first work day, or after its last work day, will not be included.)
	 */
	private int holidayDays = 0;

	/**
	 * The number of scenes in which this ScriptElement appears,
	 * based on the project's current script, regardless of whether the
	 * scenes have been scheduled or not.
	 */
	private int scriptOccurs = 0;

	/**
	 * The number of *scheduled* scenes in which this ScriptElement appears,
	 * based on the project's current script and current stripboard.
	 */
	private int scheduledOccurs = 0;

	/**
	 * The list of scene numbers in which this element appears.
	 * Used in the Script Element List screen.
	 */
	private List<String> scriptSceneList = new ArrayList<>();

	/**
	 * The list of *scheduled* scene numbers in which this element appears.
	 */
	private List<String> scheduledSceneList = new ArrayList<>();

	/** The date of the first day this element is not "OFF". */
	private Date firstWorkDate = null;

	/** The date of the last day this element is not "OFF". */
	private Date lastWorkDate = null;

	/** True iff, for a RealWorldElement, one or more dates in a 'blackout'
	 * range conflict with scheduled dates. */
	private boolean dateFlag = false;

	/** For a RealWorldElement, a date in a 'blackout' range that conflicts with
	 * a scheduled date. Typically the earliest date that conflicts, but that's
	 * only guaranteed if the RWE is linked to a single ScriptElement and only
	 * appears in a single Unit. */
	private Date conflictDate = null;

	/** If conflictDate is not null, this will be set to a message appropriate
	 * for roll-over text on the appropriate UI component, such as a highlighted
	 * date, or flag icon. */
	private String conflictMsg = null;

	/** List of date ranges that this script element may be on Hold.  Used
	 * for selecting a given hold range for character elements.  */
	private List<DatePair> holdDates;

	/**
	 * The normal constructor, used by ProjectDood.
	 * @param se the ScriptElement to be tracked.
	 */
	public ElementDood(ScriptElement se) {
		scriptElement = se;
		scriptElementId = se.getId();
		doodMap = new HashMap<>();
	}

	/**
	 * This constructor is used primarily (only?) for the "crew" ElementDood,
	 * which is used to find out a day's work status for any/all crew members.
	 * @param elemId the equivalent of ScriptElement.id; in the case of the Crew
	 * object, this is a value which will not match the real id of any Script Element.
	 */
	public ElementDood(Integer elemId) {
		scriptElement = null;
		scriptElementId = elemId;
		doodMap = new HashMap<>();
	}

	/** This is called iteratively when building a new ElementDood,
	 * in ascending date order.  It keeps track of the first WORK date
	 * and changes the status to "START"; and tracks the last date with
	 * WORK status, so that the "FINISH" status can be set by setLastStatus().
	 * @param date - The date whose status is to be recorded.
	 * @param status - The status for that date.
	 */
	public void setNextStatus(Date date, WorkdayType status) {
		String dateKey = oSdf.format(date);

		if (status.equals(WORK)) {
			if (firstWorkDate == null) { // our first WORK date
				status = START; // so set the status appropriately
				firstWorkDate = date;
			}
			lastWorkDate = date;
		}
		setStatus(dateKey, status);
		//log.debug("id=" + scriptElementId + ", datekey=" + dateKey + ", status=" + status + ", first=" + firstWorkDate + ", last=" + lastWorkDate + ", size=" + doodMap.size());
	}

	/**
	 * This should be called once after all calls to setNextStatus() have
	 * been done.  It will finalize the status values, setting the FINISH
	 * status for the last WORK day, and inserting HOLD, DROP & PICKUP
	 * values if appropriate.
	 */
	public void setLastStatus(boolean updateDrops) {
		log.debug("id=" + scriptElementId + ", first=" + firstWorkDate + ", last=" + lastWorkDate + ", size=" + doodMap.size());
		workDays = 0;
		travelDays = 0;
		holdDays = 0;
		holidayDays = 0;
		holdDates = null;

		/** The dateKey (yyyyMMdd) of the first day this element is not "OFF". */
		String firstWorkDateKey = null;
		if (firstWorkDate != null) {
			firstWorkDateKey = oSdf.format(firstWorkDate);
		}
		/** The dateKey (yyyyMMdd) of the last day this element is not "OFF". */
		String lastWorkDateKey = null;
		if (lastWorkDate != null) {
			lastWorkDateKey = oSdf.format(lastWorkDate);
		}

		if (lastWorkDateKey != null) {
			if (lastWorkDateKey.equals(firstWorkDateKey)) { // single-work-day case
				setStatus(lastWorkDateKey, START_FINISH);
			}
			else {
				setStatus(lastWorkDateKey, FINISH);
			}
		}

		if (scriptElement != null && firstWorkDateKey != null) {
			boolean isCharacter = scriptElement.getType().equals(ScriptElementType.CHARACTER);
			if (isCharacter) {
				holdDates = new ArrayList<>();
			}
			// only do DROP/PICKUP for Characters who worked at least once!
			if (firstWorkDateKey.equals(lastWorkDateKey)) {
				// take care of the single-work-day case
				workDays = 1;
			}
			else {
				calculateDays(firstWorkDateKey, lastWorkDateKey, isCharacter, updateDrops);
			}
		}

		// release our reference now that we're finished with it.
		scriptElement = null;
	}

	/**
	 * Calculate number of workdays and travel days for all elements; calculate hold days
	 * for Characters and do hold/drop calculations for Characters.
	 * @param firstWorkDateKey
	 * @param lastWorkDateKey
	 * @param isCharacter
	 * @param updateDrops
	 */
	private void calculateDays(String firstWorkDateKey, String lastWorkDateKey, boolean isCharacter, boolean updateDrops) {
		try {
			Project project = SessionUtils.getCurrentProject();
			boolean doHolds = isCharacter &&
					project.getUseHoldDrop();
				// don't restrict by 'scriptElement.getDropPickupAllowed()', as the user
				// may be changing this (in Edit mode on Script Element page), but not yet saved.
			int consecutiveHold = 0;
			Date holdStart = null, holdEnd = null;
			Date date = oSdf.parse(firstWorkDateKey);
			Calendar cal = new GregorianCalendar();
			cal.setTime(date);
			String dateKey = firstWorkDateKey;
			// First change all 'not-needed' values between START and FINISH to HOLD
			while (dateKey.compareTo(lastWorkDateKey) < 0) {
				cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
				date = cal.getTime();
				dateKey = oSdf.format(date);
				WorkdayType type = getStatus(dateKey);
				if (type == OFF) {
					// ignore; don't stop counting "consecutive" hold days
					if (holdStart == null) {
						holdStart = date;
					}
				}
				else if (type == NOT_NEEDED) {
					if (doHolds) {
						setStatus(dateKey, HOLD);
						holdDays++;
						if (holdStart == null) {
							holdStart = date;
						}
						consecutiveHold++;
						holdEnd = date;
					}
					else {
						setStatus(dateKey, OFF);
					}
				}
				else if (type.isWork()) {
					workDays++;
					// work ends a period of off (or hold) days...
					if (consecutiveHold > 0) {
						DatePair dp = new DatePair(holdStart, holdEnd);
						holdDates.add(dp);
						consecutiveHold = 0;
					}
					holdStart = null;
				}
				else if (type == COMPANY_TRAVEL
						|| type == OTHER_TRAVEL) {
					travelDays++;
					if (holdStart == null) {
						holdStart = date;
					}
				}
				else if (type == HOLIDAY) {
					holidayDays++;
					if (holdStart == null) {
						holdStart = date;
					}
				}
			}
			workDays++; // for the last work day, not included in loop
			if (consecutiveHold > 0) {
				DatePair dp = new DatePair(holdStart, holdEnd);
				holdDates.add(dp);
			}
		}
		catch (ParseException e) {
		}

		if (isCharacter) { // Do drop/pickup calculations
			Date maxHoldStart = null;
			if (holdDates.size() > 0) {
				boolean matched = (scriptElement.getDropToUse() == null);
				int maxConsecutiveHold = 0;
				for (DatePair dp : holdDates) {
					if (scriptElement.getDaysHeldBeforeDrop() != null &&  dp.getSpan() >= scriptElement.getDaysHeldBeforeDrop()) {
						if (dp.getSpan() > maxConsecutiveHold) {
							maxHoldStart = dp.getStartDate();
							maxConsecutiveHold = dp.getSpan();
						}
						if ((! matched) && scriptElement.getDropToUse().equals(dp.getStartDate())) {
							matched = true;
						}
					}
				}
				if (matched && ! updateDrops) {
					maxHoldStart = scriptElement.getDropToUse();
				}
				if (maxHoldStart != null) {
					// Apply the drop/pickup to the longest hold, & decrement the total
					// ... number of Hold days (for the DooD report).
					holdDays -= applyDropPickup(maxHoldStart);
				}
			}
			if (maxHoldStart == null && scriptElement.getDropToUse() != null) {
				// no periods qualify - turn off current selection
				ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
				scriptElement = scriptElementDAO.refresh(scriptElement);
				scriptElement.setDropToUse(null);
				scriptElementDAO.attachDirty(scriptElement);
			}
		}
	}

	/**
	 * Change the contiguous group of "HOLD" status days beginning at "start" date to
	 * "OFF" days instead.  Add the "DROP" status to the working day prior
	 * to this date, and add the "PICKUP" status to the next working day
	 * after the contiguous set of HOLD days.
	 * Note that a "contiguous" string of HOLD days may include normal "OFF"
	 * days, too.
	 * @param start The starting date of the period of Hold days which are to
	 * be changed to Off days.  The period may include other non-working days.
	 * The period is considered to extend from 'start' until the next work day
	 * is found.
	 * @return The number of hold days that were changed to Off days.
	 */
	private int applyDropPickup(Date start) {
		log.debug("start="+start);
		if (CalendarUtils.compare(start, scriptElement.getDropToUse()) != 0) {
			ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
			scriptElement = scriptElementDAO.refresh(scriptElement);
			scriptElement.setDropToUse(start);
			scriptElementDAO.attachDirty(scriptElement);
		}
		Date date = start;
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		String dateKey= oSdf.format(date);
		WorkdayType status;
		int holdDaysChanged = 0;

		while ( (status=getStatus(dateKey)) == HOLD ||
				status == OFF || status == HOLIDAY ||
				status == COMPANY_TRAVEL || status == OTHER_TRAVEL ) {
			if (status == HOLD) {
				setStatus(dateKey, OFF);
				holdDaysChanged++;
			}
			cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
			date = cal.getTime();
			dateKey = oSdf.format(date);
		}
		if (getStatus(dateKey) == FINISH) {
			setStatus(dateKey, PICKUP_FINISH);
		}
		else {
			setStatus(dateKey, PICKUP);
		}
		// now set DROP date
		cal.setTime(start);
		cal.add(Calendar.DAY_OF_MONTH, -1); // get previous day
		date = cal.getTime();
		dateKey = oSdf.format(date);
		if (getStatus(dateKey) == START) {
			setStatus(dateKey, START_DROP);
		}
		else {
			setStatus(dateKey, DROP);
		}
		return holdDaysChanged;
	}

	/**
	 * Get the work day status for the given date, for the ScriptElement
	 * associated with this object. Returns OFF if the status has not been set
	 * for the date requested. Never returns null.
	 *
	 * @param date - the Date of interest
	 * @return - the WorkdayType, e.g., WORK, OFF, START, etc., for the
	 *         requested date.
	 */
	public WorkdayType getStatus(Date date) {
		return getStatus(oSdf.format(date));
	}

	private WorkdayType getStatus(String dateKey) {
		WorkdayType status = null;
		if (doodMap != null) {
			status = doodMap.get(dateKey);
		}
		if (status == null) {
			status = OFF;
		}
		return status;
	}

	/**
	 * Set the status for a single date; if status is "OFF", the entry
	 * is actually removed from the Map, as this is the default returned
	 * value when we don't find an entry.
	 * @param dateKey - Map key in YYYYmmDD style
	 * @param status - WorkdayType status to set
	 */
	private void setStatus(String dateKey, WorkdayType status) {
		if (status.equals(OFF)) { // we don't store OFF status
			doodMap.remove(dateKey);
		}
		else {
			doodMap.put(dateKey, status);
		}
	}

	/**
	 * Returns a reasonably complete dump of the DooD settings for the ScriptElement
	 * being tracked.
	 */
	@Override
	public String toString() {
		String s = "";

		s += "id " + scriptElementId + " from " + firstWorkDate
			+ " to " + lastWorkDate + ", in scenes " + getScriptScenes() + "\n";
		s += "#days: W=" + workDays + ", Hd=" + holdDays + ", Hol=" + holidayDays + ", T=" + travelDays;
		if (doodMap != null && firstWorkDate != null && lastWorkDate != null) {
			try {
				Date date = firstWorkDate;
				Calendar cal = new GregorianCalendar();
				cal.setTime(date);
				String firstWorkDateKey, lastWorkDateKey;
				firstWorkDateKey = oSdf.format(firstWorkDate);
				lastWorkDateKey = oSdf.format(lastWorkDate);

				String dateKey = firstWorkDateKey;
				// list each date from first thru last, with its status
				while (dateKey.compareTo(lastWorkDateKey) <= 0) {
					s += " " + dateKey + ":" + doodMap.get(dateKey) + " ";
					cal.add(Calendar.DAY_OF_MONTH, 1); // get next day
					date = cal.getTime();
					dateKey = oSdf.format(date);
				}
			}
			catch (RuntimeException e) {
				log.error("DooD toString error", e);
				s += "*RUNTIME EXCEPTION*";
			}
		}
		return s;
	}

	/** See {@link #firstWorkDate}. */
	public Date getFirstWorkDate() {
		return firstWorkDate;
	}
	/** See {@link #firstWorkDate}. */
	public void setFirstWorkDate(Date firstWorkDate) {
		this.firstWorkDate = firstWorkDate;
	}

	/** See {@link #lastWorkDate}. */
	public Date getLastWorkDate() {
		return lastWorkDate;
	}
	/** See {@link #lastWorkDate}. */
	public void setLastWorkDate(Date lastWorkDate) {
		this.lastWorkDate = lastWorkDate;
	}

	/** See {@link #dateFlag}. */
	public boolean getDateFlag() {
		return dateFlag;
	}
	/** See {@link #dateFlag}. */
	public void setDateFlag(boolean dateFlag) {
		this.dateFlag = dateFlag;
	}

	/** See {@link #conflictDate}. */
	public Date getConflictDate() {
		return conflictDate;
	}
	/** See {@link #conflictDate}. */
	public void setConflictDate(Date conflictDate) {
		this.conflictDate = conflictDate;
	}

	/** See {@link #conflictMsg}. */
	public String getConflictMsg() {
		return conflictMsg;
	}
	/** See {@link #conflictMsg}. */
	public void setConflictMsg(String conflictMsg) {
		this.conflictMsg = conflictMsg;
	}

	/** See {@link #workDays}. */
	public int getWorkDays() {
		return workDays;
	}
	/** See {@link #workDays}. */
	public void setWorkDays(int workDays) {
		this.workDays = workDays;
	}

	/** See {@link #travelDays}. */
	public int getTravelDays() {
		return travelDays;
	}
	/** See {@link #travelDays}. */
	public void setTravelDays(int travelDays) {
		this.travelDays = travelDays;
	}

	/** See {@link #holdDays}. */
	public int getHoldDays() {
		return holdDays;
	}
	/** See {@link #holdDays}. */
	public void setHoldDays(int holdDays) {
		this.holdDays = holdDays;
	}

	/** See {@link #holidayDays}. */
	public int getHolidayDays() {
		return holidayDays;
	}
	/** See {@link #holidayDays}. */
	public void setHolidayDays(int holidayDays) {
		this.holidayDays = holidayDays;
	}

	/** See {@link #scriptOccurs}. */
	public int getScriptOccurs() {
		return scriptOccurs;
	}
	/** See {@link #scriptOccurs}. */
	public void setScriptOccurs(int occurs) {
		scriptOccurs = occurs;
	}

	/** See {@link #scheduledOccurs}. */
	public int getScheduledOccurs() {
		return scheduledOccurs;
	}
	/** See {@link #scheduledOccurs}. */
	public void setScheduledOccurs(int scheduledOccurs) {
		this.scheduledOccurs = scheduledOccurs;
	}

	/** See {@link #holdDates}. */
	public List<DatePair> getHoldDates() {
		return holdDates;
	}
	/** See {@link #holdDates}. */
	public void setHoldDates(List<DatePair> holdDates) {
		this.holdDates = holdDates;
	}

	/**
	 * @return A list of the scene numbers of all the scenes in which this
	 * element appears.  The list does not require or reflect any stripboard information.
	 */
	public List<String> getScriptSceneList() {
		return scriptSceneList;
	}
	public void setScriptSceneList(List<String> sceneList) {
		scriptSceneList = sceneList;
	}

	/**
	 * @return A string containing a list of all the scenes in which this
	 * element appears, each separated by a comma and space. E.g., "2, 5, 6, 8".
	 * The list does not require or reflect any stripboard information.
	 */
	public String getScriptScenes() {
		return StringUtils.getStringFromList(scriptSceneList);
	}

	/**
	 * @return A List of the scene numbers of the *scheduled* scenes in which
	 * this element appears.
	 */
	public List<String> getScheduledSceneList() {
		return scheduledSceneList;
	}
	public void setScheduledSceneList(List<String> sceneList) {
		scheduledSceneList = sceneList;
	}

	/**
	 * @return A string containing a list of the scene numbers of the *scheduled* scenes in which
	 * this element appears, each separated by a comma and space.  E.g., "2, 5, 8".
	 */
	public String getScheduledScenes() {
		return StringUtils.getStringFromList(scheduledSceneList);
	}

	public int getTotalDays() {
		return holdDays + holidayDays + travelDays + workDays;
	}
}
