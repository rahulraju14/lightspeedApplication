package com.lightspeedeps.web.schedule;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.StripBoardScene;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.script.SceneNumber;
import com.lightspeedeps.util.script.StripUtils;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.view.SortableList;

import java.util.*;

import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the super class for both StripBoardViewerBean and
 * StripBoardEditorBean.  It contains all the common functionality
 * of those classes.
 */
public class StripBoardBase extends SortableList {
	/** */
	private static final long serialVersionUID = 7913985744974365248L;

	private static final Log log = LogFactory.getLog(StripBoardBase.class);

	private static final int SB_WIDTH_LOC_STD = 450;
//	protected static final int SB_EDIT_WIDTH_LOC_ALT = 216; // right side (not currently used)
//	private static final int SB_EDIT_WIDTH_IDS = 91;
	private static final int SB_EDIT_WIDTH_SCENES = 75;
	private static final int SB_EDIT_WIDTH_DAY = 93;

	/** Max number of characters for various fields within the single String used
	 * to display a Strip on the Stripboard Editor page. */
	protected static final int SB_WIDTH_SCENES = 4; // scene numbers
	protected static final int SB_WIDTH_IE = 3;		// Int/Ext
	protected static final int SB_WIDTH_DN = 3;		// Day/Night
	protected static final int SB_WIDTH_SDAY = 6;	// Script Day
	protected static final int SB_WIDTH_LOC = 28;	// Location (Set)
	protected static final int SB_WIDTH_PAGES = 5;	// number of pages
	protected static final int SB_WIDTH_IDS = 8;	// cast IDs

	public static final String SB_LAYOUT_THIN = "THIN";
	public static final String SB_LAYOUT_THICK = "THICK";

	/** Default value for Stripboard layout style. */
	public final static String UDEF_STB_LAYOUT = SB_LAYOUT_THIN;

	protected static final String SB_STYLE_THIN = "h_a";
	protected static final String SB_STYLE_THICK = "h_b";

	/** The number of Strip's processed by the last call to prepareStripTable. */
	protected int breakdownStripCount;

	/** The total number of Strip`s with SCHEDULED status in all units
	 * of the current Stripboard.  See also unitScheduledCount[]. */
	protected int scheduledStripsCount;

	/** The number of Strip`s with OMITTED status in the current Stripboard. */
	protected int omittedStripsCount;

	/** The count of "total strips" to show in the display, which includes scheduled,
	 *  unscheduled, and omitted strips in the current Stripboard.  */
	protected int totalStripsCount;

	/** The database id of the currently selected Stripboard.  Used and set by
	 * the Stripboard drop-down on the View page.  Also stored in a session
	 * variable to control "jumping" to a particular stripboard view. */
	protected Integer stripboardId;

	protected String layout = SB_LAYOUT_THIN;

	protected Date projectStartDate;
	protected Date projectEndDate;

	/** Array of scheduled strips for the Main or only unit. */
	protected List<StripBoardScene> scheduleList;

	/** Array of unscheduled strips. */
	protected List<StripBoardScene> unScheduleList;

	/** Array of scheduled strips, indexed by unit number. Entry 0 is not used; entry 1
	 * is the same as "scheduleList" -- the strips for the Main or only unit. */
	protected List<StripBoardScene>[] unitStripList;

	/** Array of count of scheduled strips, per unit, for JSP usage. */
	private int[] unitScheduledCount;

	/** Array of unit names for JSP usage. */
	private String[] unitName;
	/** index of the data for the currently selected unit within the
	 * unitScheduledCount and unitStripList arrays. */
	protected int unitIndex;

	/** Map from unitNumber to a List of shooting dates for a particular
	 * unit within the current Stripboard. */
	private Map<Integer,List<Date>> shootDatesList;

	/** Map from unitNumber to the number of shooting days for a particular
	 * unit within the current Stripboard. */
	private Map<Integer,Integer> shootingDaysList;

	protected Stripboard stripboard;

	protected Project currentProject;
	protected Unit unit;
	protected Script script;

	private transient StripDAO stripDAO;
	private transient StripboardDAO stripboardDAO;

	public StripBoardBase() {
		super(Constants.SCENE_COLUMN_NAME);
		log.debug("");
	}

	public void initialize() {
		log.debug("");
		try {
			currentProject = SessionUtils.getCurrentProject();
			//log.debug("current project=" + currentProject);
			stripboardId = SessionUtils.getInteger(Constants.ATTR_STRIPBOARD_ID);
			if (stripboardId != null) {
				log.debug("StripboardId session value is not null: " + stripboardId);
				stripboard = getStripboardDAO().findById(stripboardId);
				if (stripboard == null || stripboard.getProject() != currentProject) {
					stripboardId = null;	// not usable
				}
			}
			if (stripboardId == null) {
				log.debug("StripboardId session value is null.");
				stripboard = currentProject.getStripboard();
				if (stripboard != null) {
					stripboardId = stripboard.getId();
				}
			}
			Integer unitId = SessionUtils.getInteger(Constants.ATTR_STRIPBOARD_UNIT_ID);
			if (unitId != null) {
				unit = UnitDAO.getInstance().findById(unitId);
				if (unit == null || ! unit.getProject().equals(currentProject)) {
					unit = currentProject.getMainUnit();
				}
			}
			else {
				unit = currentProject.getMainUnit();
			}
			selectUnit(unit);
			layout = UserPrefBean.getInstance().getString(Constants.UPREF_STB_LAYOUT, UDEF_STB_LAYOUT);
			setStripBoardData(stripboard);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void sort() {
		log.warn("Probable missing override for StripBoardBaseBean sub-class, this=" + this);
	}

	/**
	 * valueChangeListener on the drop-down list of layout styles
	 * (thick or thin)
	 * @param event
	 */
	public void listenSelectedLayout(ValueChangeEvent event) {
		try {
			String selectedLayout = (String) event.getNewValue();
			log.debug("selectedLayout=" + selectedLayout);
//			updateLayout(selectedLayout); // see rev 2708 - drop changes to strip styles
			setLayout(selectedLayout);
			UserPrefBean.getInstance().put(Constants.UPREF_STB_LAYOUT, layout);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

//	protected String updateLayout(String newLayout) {
//		setLayout(newLayout);
//		UserPrefs.setPreference(UserPrefs.UPREF_STB_LAYOUT, layout);
//		String style = layout.equals(SB_LAYOUT_THIN) ? SB_STYLE_THIN : SB_STYLE_THICK;
//		updateLayout( scheduleList, style);
//		updateLayout( unScheduleList, style);
//		return style; // for use by subclasses
//	}

	// rev 2708 - no longer change style of strip for Thin/Thick switch
//	protected void updateLayout(List<StripBoardSceneBean> beans, String style) {
//		if (beans != null) {
//			for (StripBoardSceneBean sbean : beans) {
//				sbean.setStyle(style);
//			}
//		}
//	}

	/**
	 * Sets up the lists of scheduled and unscheduled Strip`s for the
	 * given Stripboard
	 * @param board The Stripboard to be displayed.  May be null.
	 */
	protected void setStripBoardData(Stripboard board) {
		try {
			projectEndDate = currentProject.getOriginalEndDate();
			shootDatesList = new HashMap<Integer,List<Date>>();
			shootingDaysList = new HashMap<Integer,Integer>();
			if (board != null) {
				stripboardId = board.getId();
				createShootDatesList();
				createScheduledList();	// left (or top) list, scheduled strips
				createUnscheduledList();	// right (or bottom) list, unscheduled strips
			}
			else {
				stripboardId = -1;
				shootDatesList.put(1, new ArrayList<Date>()); // add empty list for main unit
				shootingDaysList.put(1, 1);
				scheduleList = new ArrayList<StripBoardScene>();
				unScheduleList = new ArrayList<StripBoardScene>();
			}
			log.debug("stripboard id=" + stripboardId);
		}
		catch (Exception e) {
			EventUtils.logError("Exception in setStripBoardData: ", e);
		}
	}

	protected void selectUnit(Unit pUnit) {
		unit = pUnit;
		projectStartDate = unit.getProjectSchedule().getStartDate();
		SessionUtils.put(Constants.ATTR_STRIPBOARD_UNIT_ID, unit.getId());
		unitIndex = unit.getProject().getUnits().indexOf(unit) + 1;
		if (unitIndex < 1) {
			log.error("unit not found in project list! Unit: " + unit.toString());
			unitIndex = 1;
		}
	}

	/**
	 * Build a list of shooting dates based on project start date,
	 * holidays, etc.  This is done once when we initialize, and then
	 * only done again if we (a) change to a different stripboard in
	 * the viewer, or (b) change the number of shooting days (in the editor).
	 */
	protected void createShootDatesList() {
		log.debug("");
		stripboard = getStripboardDAO().refresh(stripboard); // had lazy init error w/o this
		currentProject = ProjectDAO.getInstance().refresh(currentProject); // scheduleUtils got old info w/o this
		for (Unit tunit: currentProject.getUnits()) {
			ScheduleUtils shootDates = new ScheduleUtils(tunit, stripboard);
			shootDatesList.put(tunit.getNumber(), shootDates.getShootingDatesList());
			shootingDaysList.put(tunit.getNumber(), stripboard.getShootingDays(tunit));
		}
		List<Date> uDates = shootDatesList.get(unit.getNumber());
		if (uDates != null && uDates.size() > 0) {
			setProjectEndDate(uDates.get(uDates.size() - 1));
		}
		else {
			setProjectEndDate(projectStartDate);
		}
	}

	/**
	 * Does prepareStripTable of scheduled strips for every Unit, then add some
	 * end-of-day processing: filling in end-of-day total page lengths, and
	 * updating the Shooting Days count if necessary.
	 */
	@SuppressWarnings("unchecked")
	protected void createScheduledList() {
		log.debug("");
		stripboard = getStripboardDAO().refresh(stripboard); // needed for getUnitSbs() later
		unitStripList = new List[currentProject.getUnits().size()+1];
		unitScheduledCount = new int[unitStripList.length];
		unitName = new String[unitStripList.length];
		List<Strip> list = getStripDAO().findByUnitAndStripboard(currentProject.getMainUnit(), stripboard);
		scheduleList = prepareStripTable(list);
		unitStripList[1] = scheduleList;
		updateEndOfDay(scheduleList, currentProject.getMainUnit());
		scheduledStripsCount = breakdownStripCount;
		unitScheduledCount[1] = breakdownStripCount;
		if (currentProject.getHasUnits()) {
			int ix = 2;
			List<StripBoardScene> sblist;
			for (Unit unit : currentProject.getUnits()) {
				if (unit.getNumber() != 1) {
					unitName[ix] = unit.getName();
					list = getStripDAO().findByUnitAndStripboard(unit, stripboard);
					sblist = prepareStripTable(list);
					updateEndOfDay(sblist, unit);
					unitStripList[ix] = sblist;
					unitScheduledCount[ix] = breakdownStripCount;
					scheduledStripsCount += breakdownStripCount;
					ix++;
				}
			}
		}
	}

	protected void updateScheduledStripsCount(int amount) {
		unitScheduledCount[unitIndex] += amount;
		scheduledStripsCount += amount;
	}

	protected void createUnscheduledList() {
		log.debug("");
		List<Strip> list = getStripDAO().findByStatusAndStripboard(StripStatus.UNSCHEDULED, stripboard);
		if (list.size() == 0 ||
				list.get(list.size()-1).getType() != StripType.END_STRIPBOARD) {
			log.warn("unscheduled end-stripboard strip missing -- adding it");
			Strip strip = getStripboardDAO().addUnscheduledEndStrip(stripboard);
			list.add(strip);
		}
		unScheduleList = prepareStripTable(list);
		totalStripsCount = scheduledStripsCount + breakdownStripCount;
		log.debug("tot=" + totalStripsCount);
	}

	/**
	 * Create a List of StripBoardSceneBean, where each entry corresponds to
	 * a Strip in the provided stripList.
	 * @param stripList
	 * @return A List of StripBoardSceneBean; the list will be empty if the
	 * stripList is empty.  The return will never be null.
	 */
	protected List<StripBoardScene> prepareStripTable(List<Strip> stripList) {
		List<StripBoardScene> stripBoardSceneBeanList = new ArrayList<StripBoardScene>();
		try{
//			endOfDayCount = 0;
//			int eodLength = 0;
			breakdownStripCount = 0;	// count breakdown strips for caller

			StripBoardScene stripBoardBean;
			SceneDAO sceneDao = SceneDAO.getInstance();

			//Integer scriptId = currentProject.getScript().getId();
			log.debug("Project Start Date=" + projectStartDate + ", shooting days=" + shootDatesList.size());

			for (Strip strip : stripList) {
				Scene scn = null;
				if (strip.getType() == StripType.BREAKDOWN && getScript() != null) {
					scn = sceneDao.findByStripAndScript(strip, getScript());
				}
				if (strip.getType() != StripType.BREAKDOWN || scn != null) {
					// (do NOT create bean for Breakdown strips which have no Scene)
					stripBoardBean = new StripBoardScene();
					stripBoardBean.setOrderNumber(strip.getOrderNumber());
					stripBoardBean.setStatus(strip.getStatus());
					stripBoardBean.setId(strip.getId());
					stripBoardBean.setType(strip.getType());
//					stripBoardBean.setStyle(layout.equals(SB_LAYOUT_THIN) ? SB_STYLE_THIN : SB_STYLE_THICK);

					if (strip.getType() == StripType.BREAKDOWN) {
						breakdownStripCount++;
//						eodLength += strip.getLength();
						stripBoardBean.setSheetNumber(strip.getSheetNumber());
						stripBoardBean.setSynopsis(trimToWidth(scn.getSynopsis(), SB_WIDTH_LOC_STD));
						stripBoardBean.setSceneNumbers(trimToWidth(strip.getSceneNumbers(),SB_EDIT_WIDTH_SCENES));
						stripBoardBean.setSceneNumberList(strip.getScenes());
						stripBoardBean.setSeqNum(scn.getSequence());
						if (scn.getIeType() != null) {
							stripBoardBean.setIntExtType(scn.getIeType());
						}
						if (scn.getDnType() != null) {
							stripBoardBean.setDayNType(scn.getDnType());
						}
						stripBoardBean.setDayNumber((trimToWidth(scn.getScriptDay(),SB_EDIT_WIDTH_DAY)));
						stripBoardBean.setLength(strip.getLength());
						stripBoardBean.setPageLength(Scene.formatPageLength(strip.getLength()));

						if (scn.getScriptElement() != null) {
							stripBoardBean.setName(trimToWidth(scn.getScriptElement().getName(), SB_WIDTH_LOC_STD));
							stripBoardBean.setLocationId(scn.getScriptElement().getId()); // for links
						}
						else {
							stripBoardBean.setName(Constants.NO_SET_NAME);
						}

						// get ElementId's (cast ids) as array of Strings, for links in View screen
						createScriptElementIdList(stripBoardBean, scn.getScriptElements());
						// applying the color combinations
						StripColor color = StripUtils.getStripColor(scn.getIeType(), scn.getDnType());
						stripBoardBean.setColorClass("x" + color.getId());
						// Create fixed-layout strip text
						createFixedStrip(stripBoardBean, strip, scn);
					}
					else if (strip.getType() == StripType.BANNER) {
						if (strip.getComment() != null) {
							stripBoardBean.setComment(strip.getComment());
						}
						else {
							stripBoardBean.setComment(Constants.EMPTY_BANNER_TEXT);
						}
						stripBoardBean.setColorClass("xBANNER");
					}
					else if (strip.getType() == StripType.END_OF_DAY) {
						stripBoardBean.setColorClass("xEOD");
					}
					else { // end_stripboard type
						stripBoardBean.setColorClass("xEND_STRIPBOARD");
					}
					stripBoardSceneBeanList.add(stripBoardBean);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("Exception in prepareStripTable", e);
		}
		return stripBoardSceneBeanList;
	}

	/**
	 * This creates a single String with all the fields, padded as necessary,
	 * assuming a fixed-width font, to use on the Stripboard Editor page. This
	 * was done to minimize the number of <div>s on the editor page to maximize
	 * performance.
	 * <p>
	 * This is overridden by the View bean to avoid the extra processing, and so
	 * as not to overwrite the synopsis field!
	 *
	 * @param sb
	 * @param strip
	 */
	protected void createFixedStrip(StripBoardScene sb, Strip strip, Scene scn) {
		StringBuffer text = new StringBuffer();
		addPaddedField(text, strip.getSceneNumbers(), SB_WIDTH_SCENES, true);
		text.append(' ');
		addPaddedField(text, sb.getIntEType(), SB_WIDTH_IE, true);
		text.append(' ');
		addPaddedField(text, sb.getDayNType().getTinyLabel(), SB_WIDTH_DN, true);
		text.append(' ');
		addPaddedField(text, sb.getDayNumber(), SB_WIDTH_SDAY, true);
		text.append(' ');
		addPaddedField(text, sb.getName(), SB_WIDTH_LOC, false);
		text.append(' ');
		addPaddedField(text, sb.getPageLength(), SB_WIDTH_PAGES, true);
		text.append(' ');
		addPaddedField(text, StringUtils.getStringFromList(sb.getElementIdList()), SB_WIDTH_IDS, false);
		// the following can be used for debugging - displays the orderNumber on each strip
		// addPaddedField(text, ""+sb.getOrderNumber(), SB_WIDTH_IDS, false);
		log.debug("`"+text.toString()+"`");
		sb.setStripText(text.toString());

		text.setLength(0);
		addPaddedField(text, "", SB_WIDTH_SCENES + SB_WIDTH_IE + SB_WIDTH_DN + SB_WIDTH_SDAY + 4, false);
		addPaddedField(text, scn.getSynopsis(), SB_WIDTH_LOC, false);
		addPaddedField(text, "", SB_WIDTH_PAGES + SB_WIDTH_IDS + 2, false);
		sb.setSynopsis(text.toString());
	}

	private void addPaddedField(StringBuffer buff, String field, int width, boolean center) {
		if (field == null) {
			field = "";
		}
		int len = field.length();
		if (len > width) {
			buff.append(field.substring(0, width-2)).append("..");
		}
		else if (len < width) {
			if (center) {
				for (int i = 0; i < (width-len)/2; i++) {
					buff.append(' ');
				}
				len += (width-len)/2;
			}
			buff.append(field);
			for (int i = 0; i < (width-len); i++) {
				buff.append(' ');
			}
		}
		else {
			buff.append(field);
		}
	}

	/**
	 * Update all the "end of day" strips with the proper day number and
	 * shooting date. If the total number of shooting days is different than the
	 * value in the UnitStripboard, then the UnitStripboard will be updated.
	 * Note that the stripboard should be refreshed (if necessary) before
	 * calling this method, otherwise a Lazy init exception may occur in
	 * stripboard.getShootingDays().
	 *
	 * @param list The list of scene-beans whose end-of-day strips may need to
	 *            be updated.
	 */
	protected void updateEndOfDay(List<StripBoardScene> list, Unit pUnit) {
		int eodLength = 0;
		int endOfDayCount = 0;
		boolean haveStrip = false;
		// Make one pass of the list just to count the days
		for (StripBoardScene sBean : list) {
			if (sBean.getType() == StripType.END_OF_DAY) {
				endOfDayCount++;
			}
			else if (sBean.getType() == StripType.BREAKDOWN) {
				haveStrip = true;
			}
		}
		if (endOfDayCount == 0 && haveStrip) {
			endOfDayCount = 1;	// set minimum 1 shooting day even if no end-of-day strip
		}
		// If day count changed, save the updated info AND rebuild
		// the shooting dates list, which is used when formatting the strips.
		if (endOfDayCount != stripboard.getShootingDays(pUnit)) {
			log.debug("update Shooting days=" + endOfDayCount);
			UnitStripboard unitSb = stripboard.getUnitSb(pUnit);
			if (unitSb != null) {
				unitSb.setShootingDays(endOfDayCount);
				getStripboardDAO().attachDirty(unitSb);
			}
			createShootDatesList();	// Get current list!
		}
		endOfDayCount = 0;
		List<Date> shootDates = shootDatesList.get(pUnit.getNumber());
		for (StripBoardScene sBean : list) {
			if (sBean.getType() == StripType.END_OF_DAY) {
				endOfDayCount++;
				sBean.setPageLength(Scene.formatPageLength(eodLength));
				eodLength = 0;
				sBean.setComment(Constants.END_OF_DAY_TEXT + endOfDayCount);
				if (shootDates != null && shootDates.size() >= endOfDayCount) {
					// test should never fail since we rebuilt shooting dates list if necessary.
					sBean.setEndOfDayDate(shootDates.get(endOfDayCount - 1));
				}
			}
			else if (sBean.getType() == StripType.BREAKDOWN) {
				eodLength += sBean.getLength();
			}
		}
	}

	/**
	 * Creates a List of Strings, where each String is the cast id (ScriptElement.elementId) of one
	 * of the ScriptElements passed in the Set. The List is ordered in ascending logical order.
	 * If the scriptElementSet is empty, the returned List will be empty (but not null).
	 *
	 * (PREVIOUSLY the list was truncated, but that has been dropped...
	 * x The list may get truncated so that it will display properly in the available column width;
	 * x in this case, a final entry with a value of ".." is included in the list; the JSP looks for
	 * x that value to display an ellipsis.
	 * )
	 * @param scriptElementSet
	 */
	private void createScriptElementIdList(StripBoardScene sceneBean, Collection<ScriptElement> scriptElementSet) {
		// Create a sorted collection of elements with the elementId's as keys
		Map<Integer,ScriptElement> idMap = new TreeMap<Integer,ScriptElement>();
		String id;
		for (ScriptElement se : scriptElementSet) {
			if (se.getType() == ScriptElementType.CHARACTER) {
				if (se.getElementId() != null &&
						(id=se.getElementIdStr()) != null && id.length() > 0) {
					idMap.put(se.getElementId(), se);
				}
			}
		}

		// Now trim list so its display will fit in the available column width
		List<String> trimList = new ArrayList<String>();
		List<String> nameList = new ArrayList<String>();
		List<Integer> idList = new ArrayList<Integer>();
//		int width = SB_EDIT_WIDTH_IDS;
//		if (layout.equals(SB_LAYOUT_THICK)) {
//			width = (width * 2) - 5; // fudge factor for wrap
//		}
//		int last = -1;
		for (Integer key : idMap.keySet()) {
			ScriptElement se = idMap.get(key);
			id = se.getElementIdStr();
//			if (last >= 0) {
//				nameList.set(last, nameList.get(last) + ", " + id);
//			}
//			else {
//				if (width <= (7 * id.length())+3) { // next one won't fit
//					trimList.add(".."); // trailing ellipsis
//					last = nameList.size();
//					nameList.add(id);
//					//log.debug(trimList.size());
//				}
//				else {
					trimList.add(id);
					nameList.add(se.getName());
					idList.add(se.getId());
//					width -= (7 * id.length())+3; // 7 per character + 3 for space between ids
					//log.debug(width);
//				}
//			}
		}
		sceneBean.setElementIdList(trimList);
		sceneBean.setElementNameList(nameList);
		sceneBean.setElementDbIdList(idList);
	}

	/**
	 * Trim a string to a particular width. The edit screen's bean,
	 * StripBoardEditorBean, overrides this to be a no-op, since the Edit page
	 * no longer uses the individuals fields, only the single text String for
	 * all data.
	 */
	protected String trimToWidth(String text, int width) {
		return StringUtils.trimToWidth(text, width);
	}

	// Used in JSP to ensure that sort column values match what
	// the Java code compares against in the StripBoardSceneBean comparator.
	public String getSheetColumnName() {
		return Constants.SHEET_COLUMN_NAME;
	}
	public String getSceneColumnName() {
		return Constants.SCENE_COLUMN_NAME;
	}
	public String getIEColumnName() {
		return Constants.IE_COLUMN_NAME;
	}
	public String getDNColumnName() {
		return Constants.DN_COLUMN_NAME;
	}
	public String getScriptDayColumnName() {
		return Constants.SCRIPT_DAY_COLUMN_NAME;
	}
	public String getLocationColumnName() {
		return Constants.LOCATION_COLUMN_NAME;
	}
	public String getPageLengthColumnName() {
		return Constants.PAGES_COLUMN_NAME;
	}
	public String getIdsColumnName() {
		return Constants.ID_COLUMN_NAME;
	}

	//is default ascending
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		return true;
	}

	protected Comparator<StripBoardScene> getComparator() {
		//log.debug("");
		Comparator<StripBoardScene> comparator = new Comparator<StripBoardScene>() {
			@Override
			public int compare(StripBoardScene s1, StripBoardScene s2) {
				//log.debug("comparing sheet #s "+s1.getSheetNumber() + " and " + s2.getSheetNumber());
				if (s1.getType() == StripType.END_STRIPBOARD) {
					return 1;
				}
				else if (s2.getType() == StripType.END_STRIPBOARD) {
					return -1;
				}
				if (s1.getStatus() != s2.getStatus()) {
					if (s1.getStatus() == StripStatus.OMITTED) {
						return 1;
					}
					else {
						return -1;
					}
				}
				int ret = 0;
				if (sortColumnName == null) {
					//ret = 0;
				}
				else if (sortColumnName.equals(Constants.IE_COLUMN_NAME)) {
					ret = NumberUtils.compare(s1.getIntExtType().getSortOrder(), s2.getIntExtType().getSortOrder());
				}
				else if (sortColumnName.equals(Constants.ID_COLUMN_NAME)) {
					ret = compareIdList(s1.getElementIdList(), s2.getElementIdList());
				}
				else if (sortColumnName.equals(Constants.SCENE_COLUMN_NAME)) {
					ret = SceneNumber.compareSceneNumberLists(s1.getSceneNumbers(), s2.getSceneNumbers());
				}
				else if (sortColumnName.equals(Constants.DN_COLUMN_NAME)) {
					ret = NumberUtils.compare(s1.getDayNType().getSortOrder(), s2.getDayNType().getSortOrder());
				}
				else if (sortColumnName.equals(Constants.SCRIPT_DAY_COLUMN_NAME)) {
					ret = StringUtils.compare(s1.getDayNumber(), s2.getDayNumber());
				}
				else if (sortColumnName.equals(Constants.PAGES_COLUMN_NAME)) {
					ret = s1.getLength().compareTo(s2.getLength());
				}
				else if (sortColumnName.equals(Constants.LOCATION_COLUMN_NAME)) {
					ret = StringUtils.compare(s1.getName(), s2.getName());
				}
				else if (sortColumnName.equals(Constants.SHEET_COLUMN_NAME)) {
					ret = s1.getSheetNumber().compareTo(s2.getSheetNumber());
				}
				else {
					log.warn("unexpected sort column name='" + sortColumnName + "'");
				}
				if (ret == 0) {
					ret = NumberUtils.compare(s1.getSeqNum(), s2.getSeqNum());
				}
				return ascending ? ret : 0-ret;
			}
		};
		return comparator;
	}

	/**
	 * Compare two Lists of element-id (cast id) values, and return the standard
	 * -1/0/1 values for comparison. Handles null and empty Lists.
	 * @param list1 The first List of Strings.
	 * @param list2 The second List of Strings.
	 * @return A comparison result of -1, 0, or 1. The comparison proceeds as follows:
	 * <li> if both parameters are either null or an empty list, 0 is returned (null is considered
	 * equal to an empty list).
	 * <li> If the second parameter is null or an empty List, and the first contains at least one entry,
	 * return 1.
	 * <li> If the first parameter is null or an empty list, and the second contains at least one entry,
	 * return -1.
	 * <li> Begin comparing corresponding (by index) entries, and stop as soon as an inequality is found,
	 * and return that inequality (1 or -1).  A null entry, or reaching the end of a list, is considered
	 * less than any non-null entry.
	 * <li> When comparing two non-null entries, if they compare equal as Strings, they are considered equal;
	 * if not, we attempt to convert them both to integers, and, if successful, return the comparison
	 * of the two integer values.  If the conversion is not successful, the String (lexical) comparison
	 * value is returned.
	 */
	private static int compareIdList(List<String> list1, List<String> list2) {
		int ret = 0;
		if (list1 == null || list1.size() == 0) {
			if (list2 == null || list2.size() == 0) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else if (list2 == null || list2.size() == 0) {
			return 1;
		}
		// have two non-empty lists
		int last = Math.min(list1.size(), list2.size());
		for (int ix = 0; ix < last && ret == 0; ix++) {
			ret = StringUtils.compareNumeric(list1.get(ix), list2.get(ix));
		}
		if (ret == 0) {
			ret = NumberUtils.compare(list1.size(), list2.size());
		}
		return ret;
	}

	/**
	 * Refresh any database objects that might be "stale" -- not connected to
	 * our Hibernate session -- when we get control during some JSF phase.
	 */
	protected void refresh() {
		if (stripboard != null) {
			stripboard = getStripboardDAO().refresh(stripboard);
		}
	}

	/**
	 * Called as the ActionListener method on the "publish" button.
	 * @param evt
	 */
/*	public void actionPublish(ActionEvent evt) {
		try {
			stripboardId = stripboard.getId();
			log.debug("Stripboard id=" + stripboardId);
			if (stripboardId.equals(SessionUtils.getCurrentProject().getStripboard().getId())) {
				stripboard = getStripboardDAO().findById(stripboardId);
				ReportBean report = (ReportBean)ServiceFinder.findBean("reportBean");
				report.gnrtStripbrdReportForEmailSending(stripboard);
			}
		}
		catch (Exception e) {
			EventUtils.logError("publish failed: ", e);
		}
		return;
	}
*/

	/**
	 * @return the layout
	 */
	public String getLayout() {
		return layout;
	}
	/**
	 * @param layout the layout to set
	 */
	public void setLayout(String layout) {
		this.layout = layout;
	}

	/** See {@link #projectStartDate}. */
	public Date getProjectStartDate() {
		return projectStartDate;
	}
	/** See {@link #projectStartDate}. */
	public void setProjectStartDate(Date projectStartDate) {
		this.projectStartDate = projectStartDate;
	}

	/** See {@link #stripboardId}. */
	public Integer getStripboardId() {
		return stripboardId;
	}
	/** See {@link #stripboardId}. */
	public void setStripboardId(Integer id) {
		stripboardId = id;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public UnitStripboard getUnitSb() {
		return getStripboard().getUnitSb(getUnit());
	}

	public Map<Integer,Integer> getShootingDaysList() {
		return shootingDaysList;
	}

	/** See {@link #scheduledStripsCount}. */
	public Integer getScheduledStripsCount() {
		return scheduledStripsCount;
	}
	/** See {@link #scheduledStripsCount}. */
	public void setScheduledStripsCount(int scheduledStripsCount) {
		this.scheduledStripsCount = scheduledStripsCount;
	}

	/** See {@link #unitIndex}. */
	public int getUnitIndex() {
		return unitIndex;
	}
	/** See {@link #unitIndex}. */
	public void setUnitIndex(int unitIndex) {
		this.unitIndex = unitIndex;
	}

	/** See {@link #unitScheduledCount}. */
	public int[] getUnitScheduledCount() {
		return unitScheduledCount;
	}
	/** See {@link #unitScheduledCount}. */
	public void setUnitScheduledCount(int[] unitScheduledCount) {
		this.unitScheduledCount = unitScheduledCount;
	}

	/** See {@link #unitName}. */
	public String[] getUnitName() {
		return unitName;
	}
	/** See {@link #unitName}. */
	public void setUnitName(String[] unitName) {
		this.unitName = unitName;
	}

	/**
	 * @return The count of unscheduled Strip`s, which is computed by
	 *         subtracting the number of omitted Strip`s and scheduled Strip`s
	 *         from the total number of Strip`s.
	 */
	public int getUnscheduledStripsCount() {
		log.debug("tot=" + totalStripsCount + ", sch=" + scheduledStripsCount + ", omit=" + omittedStripsCount);
		return totalStripsCount - scheduledStripsCount - omittedStripsCount;
	}

	/** See {@link #totalStripsCount}. */
	public int getTotalStripsCount() {
		return totalStripsCount;
	}
	/** See {@link #totalStripsCount}. */
	public void setTotalStripsCount(int totalStripsCount) {
		this.totalStripsCount = totalStripsCount;
	}

	/** returns the number of omitted strips */
	public int getOmittedStripsCount() {
		return omittedStripsCount;
	}

	/** See {@link #projectEndDate}. */
	public Date getProjectEndDate() {
		return projectEndDate;
	}
	/** See {@link #projectEndDate}. */
	public void setProjectEndDate(Date projectEndDate) {
		this.projectEndDate = projectEndDate;
	}

	/**
	 * @return the scheduleList
	 */
	public List<StripBoardScene> getScheduleList() {
		return scheduleList;
	}
	/**
	 * @param scheduleList the scheduleList to set
	 */
	public void setScheduleList(List<StripBoardScene> scheduleList) {
		this.scheduleList = scheduleList;
	}

	/** See {@link #unitStripList}. */
	public List<StripBoardScene>[] getUnitStripList() {
		return unitStripList;
	}
	/** See {@link #unitStripList}. */
	public void setUnitStripList(List<StripBoardScene>[] unitStripList) {
		this.unitStripList = unitStripList;
	}

	/**
	 * @return the currentProject
	 */
	public Project getCurrentProject() {
		return currentProject;
	}
	/**
	 * @param currentProject the currentProject to set
	 */
	public void setCurrentProject(Project currentProject) {
		this.currentProject = currentProject;
	}

	/**
	 * @return the stripBoard
	 */
	public Stripboard getStripboard() {
		return stripboard;
	}
	/**
	 * @param pstripBoard the stripBoard to set
	 */
	public void setStripboard(Stripboard pstripBoard) {
		stripboard = pstripBoard;
	}

	/**
	 * @return the unScheduleList
	 */
	public List<StripBoardScene> getUnScheduleList() {
		// we only want to sortColumnName if the column or ordering has changed.
		sortIfNeeded();
		return unScheduleList;
	}
	/**
	 * @param unScheduleList the unScheduleList to set
	 */
	public void setUnScheduleList(List<StripBoardScene> unScheduleList) {
		this.unScheduleList = unScheduleList;
	}

	/** See {@link #script}. */
	public Script getScript() {
		if (script == null) {
			script = currentProject.getScript();
		}
		return script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	protected StripboardDAO getStripboardDAO() {
		if (stripboardDAO == null) {
			stripboardDAO = StripboardDAO.getInstance();
		}
		return stripboardDAO;
	}

	protected StripDAO getStripDAO() {
		if (stripDAO == null) {
			stripDAO = StripDAO.getInstance();
		}
		return stripDAO;
	}

}
