//File Name:	StripBoardEditorBean.java
package com.lightspeedeps.web.schedule;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.ajax.AjaxBehavior;
import org.icefaces.ace.component.list.ACEList;
import org.icefaces.ace.event.ListSelectEvent;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.object.StripBoardScene;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.util.Disposable;
import com.lightspeedeps.web.util.Disposer;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * The backing class for the Stripboard Editor page.
 */
//@SuppressWarnings("all")
@ManagedBean
@ViewScoped
public class StripBoardEditBean extends StripBoardBase implements Serializable, Disposable {
	/** */
	private static final long serialVersionUID = - 6002064229750466221L;

	private static final Log log = LogFactory.getLog(StripBoardEditBean.class);

	private static final String FORCE_REFRESH = HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_EDIT;

	/** Text used in filter drop-down lists (int/ext and day/night) for "All" case. */
	private static final String FILTER_ALL = "All";
	private static final String SELECT_UNSET = "none";

	/* id of the main form */
	private static final String FORM_ID = "se:";

	/* component id for the list containing the Banner and EOD strips */
	private static final String BANNER_EOD_LIST = "bl";

	/* component id for the left-hand scheduled-strips list */
	private static final String SCHEDULED_LEFT_LIST = "sl";

	/* component id for the right-hand unscheduled-strips list */
	private static final String UNSCHEDULED_LIST = "us";

	/* component id for the right-hand scheduled strips list */
	private static final String SCHEDULED_RIGHT_LIST = "ss";

	// regex is roughly:       [[n1,n2]<,[n3,n4]>...]
	private final static String MOVE_REGEX = "\\[+(\\d+).*\\,(\\d+)\\]+";
	private final static Pattern MOVE_PATTERN  = Pattern.compile(MOVE_REGEX);

	// regex is roughly:       [" form : srcId ", [[n1,n2]<,[n3,n4]>...]
	private final static String MIGRATE_REGEX = "\\[\\\"\\w+:(\\w+)\\\"\\,\\[+(\\d+).*\\,(\\d+)\\]+";
	private final static Pattern MIGRATE_PATTERN  = Pattern.compile(MIGRATE_REGEX);

	// Constants used to identify a particular list in use -
	//   left, right-scheduled, or right-unscheduled
	private static final int LIST_ALL = 0;
	private static final int LIST_SCH = 1;
	private static final int LIST_UNSCH = 2;
	private static final int LIST_SCH_RT = 3;

	/** Value of drop-down Int/Ext filter selection in right panel: All/Int/Ext. */
	private String filterIE = FILTER_ALL; // default value

	/** Value of drop-down Day/Night filter selection in right panel: All/Day/Night. */
	private String filterDN = FILTER_ALL; // default value

	/** Value of drop-down list selector in right panel, "Scheduled" or "Unscheduled"  */
	private String selectedSchedule = STB_FILTER_UNSCHEDULED;
	private static final String STB_FILTER_SCHEDULED = "Scheduled";
	public static final String STB_FILTER_UNSCHEDULED = "Unscheduled";

	private String selectLeftScheduled = SELECT_UNSET;
	private String selectRightUnscheduled = SELECT_UNSET;
	private String selectRightScheduled = SELECT_UNSET;

	/** Input text field for editing banner content. */
	private String bannerText = "BANNER";

	private String omitValue = "0";
	private String stripStatus;
	private String oldStripboardName;

	private int leftEventType;
//	private int eventOldIndex;

	/** row index marking end of shift-click range */
	private int shiftIx = -1;
	/** Indicates which list the shiftIx value applies to. */
	private int shiftList = -1;

	/** List index of currently selected row.  Usually set by
	 * f:setPropertyActionListener in JSP. */
	private int selectedRow = -1;

	/** The index of the sceneBean representing the Banner strip that is
	 * currently being edited.  Set by showBannerPopup(), used by
	 * actionSaveBannerText(). */
	private int editBannerIndex = -1;

	/** True if Omitted strips are to be displayed when unscheduled strips are listed
	 * on the right side; value set by user via check-box on UI.  Note that this value may
	 * be true (but ignored) when the right-side list is showing scheduled strips.*/
	private boolean omittedFlag = false;

	//private boolean stripDrag = true;

	private boolean stripDisabled = false;
	/** The flag that, when true, causes the JSP to display the Edit Banner Text pop-up dialog. */
	private boolean showPopup = false;
	/** Set to true if calcInsertOrderNumber re-set all the Strip order numbers. */
	private boolean resequenced = false;

	/** The list of strips displayed on the right-hand list when it is the Scheduled ones.
	 * This list has its own copies of StripBoardSceneBeans -- they are not shared with
	 * the left-hand list. */
	private List<StripBoardScene> scheduleListRight = new ArrayList<>();

	private Set<StripBoardScene> selectedScheduledRightList = new HashSet<>();
	private Set<StripBoardScene> selectedScheduledLeftList = new HashSet<>();
	private Set<StripBoardScene> selectedUnscheduledList = new HashSet<>();

	/** The list of all Strip's available for display in the unscheduled Strip list.  This contrasts
	 * with 'unScheduleList', owned by StripBoardBaseBean, which may have a filtered (restricted)
	 * list, based on the user's currently selected options. */
	private List<StripBoardScene> unScheduleListAll = new ArrayList<>();

	/** A list of beans for all the currently omitted Strip's. */
	private List<StripBoardScene> omittedList = new ArrayList<>();
	private List<StripBoardScene> dragBannerList = new ArrayList<>();
	private List<StripBoardScene> dragEndOfDayList = new ArrayList<>();

	private AutoScheduleBean asBean;

	/** The last drag&drop operation received for the left-hand (scheduled) list. */
	DragDropOp ltOp = DragDropOp.INVALID;

	/** An enumeration of the possible types of drag&drop moves the user can do, within
	 * and between the lists of Strips on the edit page. */
	private enum MoveType {
		/** from scheduled to unscheduled */
		TO_UNSCHEDULED,
		/** from unscheduled to scheduled */
		TO_SCHEDULED,
		/** from right-hand scheduled to left-hand scheduled */
		SHIFT_LEFT_SCHEDULED,
		/** from left-hand scheduled to right-hand scheduled */
		SHIFT_RIGHT_SCHEDULED,
		/** moving within unscheduled (right-hand) list */
		IN_UNSCHEDULED,
		/** moving within left-hand (scheduled) list */
		IN_LEFT_SCHEDULED,
		/** moving within right-hand scheduled list */
		IN_RIGHT_SCHEDULED,
		/** from End-of-Day or Banner into Scheduled list */
		ADD_BANNER_EOD,
		/** e.g., dragging EOD to unscheduled, ... */
		REJECT,
		/**  no D&D at all, or strip stayed in same position */
		NONE,
		/**  */
		INVALID
	}

	/** An enumeration of the operations returned in SelectEvents. */
	private enum DragDropOp {
		ADD,
		REMOVE,
		MOVE,
		INVALID
	}

	Integer lockingUserId = null;

	//Project project = SessionUtils.getCurrentProject();

	private transient ProjectDAO projectDAO = ProjectDAO.getInstance();
	private final Disposer disposer = Disposer.getInstance();

	public StripBoardEditBean() {
		log.debug("");
		disposer.register(this);
		initialize(); // initialize our super class AFTER we're constructed!
		boolean locked = false;
		try {
			lockingUserId = SessionUtils.getCurrentUser().getId();
			locked = projectDAO.lock(SessionUtils.getCurrentProject(), SessionUtils.getCurrentUser());
		}
		catch (Exception e1) {
			EventUtils.logError("stripboard lock failed", e1);
		}
		if (locked) {
			scheduleList = getUnitStripList()[unitIndex];
			if (scheduleList.size() == 0 ||
					scheduleList.get(scheduleList.size()-1).getType() != StripType.END_STRIPBOARD) {
				log.warn("end-stripboard strip missing -- adding it");
				getStripboardDAO().addScheduledEndStrip(stripboard, getUnit());
				createScheduledList();
				scheduleList = getUnitStripList()[unitIndex];
			}
			try {
				createDragStrips();
				oldStripboardName = stripboard.getDescription();
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
		else {
			// edit lock failed - send to view page instead.
			HeaderViewBean.navigate(HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_VIEW);
		}
	}

	@Override
	protected void createScheduledList() {
		super.createScheduledList();
		scheduleList = getUnitStripList()[unitIndex];
	}

	/**
	 * Action method for the "Return" button on the Edit page.
	 * See if the user has changed the stripboard name, and if so, save
	 * the new value.
	 *
	 * @return The navigation string to the View page.
	 */
	public String actionReturn() {
		log.debug("");
		if (! stripboard.getDescription().equals(oldStripboardName)) {
			String description = new String(stripboard.getDescription());
			stripboard = getStripboardDAO().refresh(stripboard);
			stripboard.setDescription(description);
			getStripboardDAO().attachDirty(stripboard);
		}
		return HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_VIEW;
	}

	/**
	 * Action method for the "Schedule" button.  Sets up the AutoScheduleBean
	 * and lets it display the dialog box.
	 * @return null navigation String
	 */
	public String actionAutoSchedule() {
		log.debug("");
		asBean = AutoScheduleBean.getInstance();
		asBean.setScript(script);
		asBean.setStripboard(stripboard);
		asBean.setUnit(getUnit());
		if (! asBean.show()) { // may happen if no Script exists or ??
			MsgUtils.addFacesMessage("Stripboard.AutoSchedule.Failed", FacesMessage.SEVERITY_ERROR);
			asBean = null;
		}
		return null;
	}

	/**
	 * Action method for the "Continue" button on the auto-schedule dialog.
	 * @return null navigation String
	 */
	public String actionDoSchedule() {
		if (asBean != null) {
			asBean.schedule(); // this does all the work!!
		}
		refreshAll();
		return null;
	}

	public void listenSelectedScheduledLeft(ListSelectEvent evt) {
		log.debug("");
		ACEList a = (ACEList)evt.getSource();
		log.debug(a + ", row: " + a.getRowIndex());
		log.debug(evt.getObjects());
		if (evt.getObjects() != null) {
			for (Object obj : evt.getObjects()) {
				log.debug(obj.toString());
			}
		}
//		setEventOldIndex(a.getRowIndex());
		dumpLists();
	}

	public void listenSelectedScheduledRight(ListSelectEvent evt) {
		log.debug("");
		ACEList a = (ACEList)evt.getSource();
		log.debug(a + ", row: " + a.getRowIndex());
		log.debug(evt.getObjects());
	}

	public void listenSelectedUnscheduled(ListSelectEvent evt) {
		log.debug("");
		ACEList a = (ACEList)evt.getSource();
		log.debug(a + ", row: " + a.getRowIndex());
		log.debug(evt.getObjects());
	}


	public void listenSelectScheduledLeft(AjaxBehaviorEvent evt) {
		log.debug(evt.getComponent().getClientId());
		ACEList a = (ACEList)evt.getSource();
		log.debug(a + ", row: " + a.getRowIndex());
		AjaxBehavior b = (AjaxBehavior)evt.getBehavior();
		log.debug(b + ", hints: " + b.getHints());
		dumpLists();
		clearSelections();
	}

	public void listenMigrateScheduledLeft(AjaxBehaviorEvent evt) {
		log.debug("");
		try {
			String param = findImmigrationParam(SCHEDULED_LEFT_LIST);
			List<Integer> results = new ArrayList<>();
			String sourceId = parseMigrationParam(param, results);
			if (sourceId.equals(UNSCHEDULED_LIST)) {
				processDragAndDrop(MoveType.TO_SCHEDULED, results, false);
			}
			else if (sourceId.equals(SCHEDULED_RIGHT_LIST)) {
				processDragAndDrop(MoveType.SHIFT_LEFT_SCHEDULED, results, true);
			}
			else if (sourceId.equals(BANNER_EOD_LIST)) {
				processDragAndDrop(MoveType.ADD_BANNER_EOD, results, false);
				// TODO need to force Banner/EOD list refresh
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public void listenMigrateScheduledRight(AjaxBehaviorEvent evt) {
		log.debug("");
		try {
			String param = findImmigrationParam(SCHEDULED_RIGHT_LIST);
			List<Integer> results = new ArrayList<>();
			String sourceId = parseMigrationParam(param, results);
			if (sourceId.equals(SCHEDULED_LEFT_LIST)) {
				processDragAndDrop(MoveType.SHIFT_RIGHT_SCHEDULED, results, false);
			}
			else if (sourceId.equals(BANNER_EOD_LIST)) {
				processDragAndDrop(MoveType.ADD_BANNER_EOD, results, true);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public void listenMigrateUnscheduled(AjaxBehaviorEvent evt) {
		log.debug("");
		try {
			String param = findImmigrationParam(UNSCHEDULED_LIST);
			List<Integer> results = new ArrayList<>();
			String sourceId = parseMigrationParam(param, results);
			if (sourceId.equals(SCHEDULED_LEFT_LIST)) {
				processDragAndDrop(MoveType.TO_UNSCHEDULED, results, false);
			}
			else if (sourceId.equals(BANNER_EOD_LIST)) {
				processReject(null /*rtOp*/);
				createUnscheduledList();		// rebuild to remove it
//				processDragAndDrop(MoveType.ADD_BANNER_EOD, results, false);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
//		processMigrate(MoveType.TO_UNSCHEDULED);
	}

	/**
	 * data: selected left list; updated scheduled list;
	 * @param evt
	 */
	public void listenMoveScheduledLeft(AjaxBehaviorEvent evt) {
		try {
			String param = findReorderParam(SCHEDULED_LEFT_LIST);
			List<Integer> results = parseMoveParam(param);
			processDragAndDrop(MoveType.IN_LEFT_SCHEDULED, results, false);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public void listenMoveScheduledRight(AjaxBehaviorEvent evt) {
		log.debug("");
		try {
			String param = findReorderParam(SCHEDULED_RIGHT_LIST);
			List<Integer> results = parseMoveParam(param);
			processDragAndDrop(MoveType.IN_RIGHT_SCHEDULED, results, true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public void listenMoveUnscheduled(AjaxBehaviorEvent evt) {
		log.debug("");
		try {
			String param = findReorderParam(UNSCHEDULED_LIST);
			List<Integer> results = parseMoveParam(param);
			processDragAndDrop(MoveType.IN_UNSCHEDULED, results, false);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @param listId
	 * @return
	 */
	private String findReorderParam(String listId) {
		return findParam(FORM_ID + listId + Constants.ICEFACES_REORDER_PARAM );
	}

	/**
	 * @param listId
	 * @return
	 */
	private String findImmigrationParam(String listId) {
		return findParam(FORM_ID + listId + Constants.ICEFACES_IMMIGRATION_PARAM );
	}

	/**
	 * @param string
	 * @return
	 */
	private String findParam(String key) {
		ApplicationUtils.dumpParams(null);
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Map<String,String> requestMap = facesContext.getExternalContext().getRequestParameterMap();
		String param = requestMap.get(key);
		return param;
	}

	/**
	 * Parse a move parameter string, which contain an array of arrays of two
	 * elements e.g., [[2,3],[3,4]], representing the movement of the first
	 * element through multiple rows.
	 *
	 * @return List of two integers, being the first row and last row.
	 */
	private List<Integer> parseMoveParam(String param) {

		List<Integer> results = new ArrayList<>();

		log.debug("value=" + param);
		if (param.length() > 2) {
			Matcher m = MOVE_PATTERN.matcher(param);
			if (m.matches() && m.groupCount() == 2) {
				results.add(Integer.parseInt(m.group(1)));
				results.add(Integer.parseInt(m.group(2)));
			}
			else {
				log.debug(m.matches() + ", cnt=" + m.groupCount());
			}
		}
		return results;
	}

	/**
	 * Parse out the first and last row numbers from the migration parameters.
	 *
	 * @param param parameter string created by ICEfaces, pulled from session.
	 * @return component id of the source field; also updates 'results'
	 *         parameter by adding the first and last row numbers in the
	 *         parameter list.
	 */
	private String parseMigrationParam(String param, List<Integer> results) {
		// Drag row 3 on unscheduled, drop in slot 7 on left-scheduled:
		// se:sl_immigration=["se:us",[[3,7]]]

		Matcher m ;
		String sourceId = "";
		log.debug("value=" + param);
		if (param.length() > 2) {
			m = MIGRATE_PATTERN.matcher(param);
			if (m.matches() && m.groupCount() >= 3) {
				sourceId = m.group(1);
				results.add(Integer.parseInt(m.group(2)));
				results.add(Integer.parseInt(m.group(3)));
			}
			else {
				log.debug(m.matches() + ", cnt=" + m.groupCount());
			}
		}
		return sourceId;
	}

	private void clearSelections() {
		selectedScheduledLeftList.clear();
		selectedScheduledRightList.clear();
		selectedUnscheduledList.clear();
	}

	/**
	 * Listener on the Scheduled/Unscheduled radio button selection for
	 * the right-hand list of the edit page.
	 * @param event
	 */
	public void listenSelectedSchedule(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				selectedSchedule = event.getNewValue().toString();
				createUnscheduledList(); // rebuild right-hand list as appropriate
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
//		ListView.addClientResize();
	}

	/**
	 * Listener for the "Int/Ext" drop-down selection list.
	 * Updates the local filter value, then rebuilds the necessary data structures.
	 * @param event
	 */
	public void listenSelectedFilterIntExt(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				String ieValue = (String) event.getNewValue();
				log.debug("IE filter=" + ieValue);
				setFilterIE(ieValue);
				createUnscheduledList(); // rebuild right-hand list as appropriate
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Listener for the "Day/Night" drop-down selection list.
	 * Updates the local filter value, then rebuilds the necessary data structures.
	 * @param event
	 */
	public void listenSelectedFilterDayNight(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				String dnValue = (String) event.getNewValue();
				log.debug("DN filter=" + dnValue);
				setFilterDN(dnValue);
				createUnscheduledList(); // rebuild right-hand list as appropriate
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Listener on the check-box as to whether or not "omitted" strips are displayed.
	 * Updates the local filter value, then rebuilds the necessary data structures.
	 * @param event
	 */
	public void listenOmittedListener(ValueChangeEvent event) {
		try {
			Boolean omitVal =  (Boolean) event.getNewValue();
			log.debug("omit check=" + omitVal);
			setOmittedFlag(omitVal.booleanValue());
			//setDragData();
			//setStripBoardData(stripBoard);// re-do only the unscheduled list
			createUnscheduledList(); // rebuild right-hand list as appropriate
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The ActionListener method for the "Restore"  button, which changes an
	 * "omitted" strip back to unscheduled (not omitted), and positions it to the
	 * bottom of the list of unscheduled strips.
	 */
	public String actionRestore() {
		log.debug("unomit, selectRightUnscheduled=" + getSelectRightUnscheduled());
		refresh();	// refresh db objects
		boolean updated = false;
		if ( ! getSelectRightUnscheduled().equalsIgnoreCase(SELECT_UNSET)) {
			setSelectRightUnscheduled(SELECT_UNSET);
			StripBoardScene sBean;
			SceneDAO sceneDao = SceneDAO.getInstance();
			for (Iterator<StripBoardScene> iter = unScheduleList.iterator(); iter.hasNext(); ) {
				sBean = iter.next();
				if (sBean != null && sBean.getSelected()) {
					sBean.setSelected(false);
					if (sBean.getStatus() == StripStatus.OMITTED) {
						Strip strip = getStripDAO().findById(sBean.getId());
						sceneDao.updateScenesOmit(strip.getScenes(), getScript(), false);
						getStripDAO().restoreStrips(strip.getSceneNumbers(), currentProject);
						omittedStripsCount--;
						updated = true;
					}
				}
			}
			if (updated) {
				createUnscheduledList();
				updateLastSaved();
			}
		}
		return FORCE_REFRESH;
	}

	/**
	 * ActionListener method for "Omit" button. Change the selected strip(s) to
	 * Omitted status. End-of-day strips and banners are deleted.
	 *
	 * @param evt The action event object - not used.
	 */
	public String actionOmit() {
		log.debug("");
		try {
			refresh();	// refresh db objects
			StripBoardScene sBean = null;
			boolean updateEOD = false;
			boolean unsched = false;
			boolean rtSched = false; // Selected item to omit is in right-scheduled list
			List<StripBoardScene> beans = null;
			if ( ! getSelectRightUnscheduled().equalsIgnoreCase(SELECT_UNSET)) {
				beans = unScheduleList;
				unsched = true;
			}
			else if ( ! getSelectLeftScheduled().equalsIgnoreCase(SELECT_UNSET)) {
				beans = scheduleList;
				updateEOD = true;
			}
			else if ( ! getSelectRightScheduled().equalsIgnoreCase(SELECT_UNSET)) {
				beans = scheduleListRight;
				rtSched = true;
				updateEOD = true;
			}
			log.debug("lt=" + getSelectLeftScheduled() + ", rs=" + getSelectRightScheduled() + ", ru=" + getSelectRightUnscheduled());
			clearSelectStatus();
			if (beans != null) {
				int rowNumber = 0;
				for (Iterator<StripBoardScene> iter = beans.iterator(); iter.hasNext(); ) {
					sBean = iter.next();
					if (sBean.getSelected()) {
						sBean.setSelected(false);
						iter.remove();	// remove it from the display list
						if (unsched && (unScheduleListAll != null)) {
							// presumably filters are applied; update "All" list, also
							boolean removed = unScheduleListAll.remove(sBean);
							if (!removed) {
								EventUtils.logError("failed to remove bean, strip id=" + sBean.getId());
							}
						}
						if (rtSched) {
							// Also remove it from the left-hand scheduled list
							scheduleList.remove(rowNumber);
						}
						else if (! unsched && isRightListScheduled()) {
							// it's been removed from left-scheduled list; update right one, too
							scheduleListRight.remove(rowNumber);
						}
						omitStrip(sBean);
					}
					else {
						rowNumber++;
					}
				}
			}

			if (isOmittedFlag() & !isRightListScheduled()) { // rebuild right list
				createUnscheduledList(); // TODO could accomplish this with less processing.
			}
			if (updateEOD) {
				updateEndOfDay();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return FORCE_REFRESH;
	}

	/**
	 * Mark as Omitted the Strip corresponding to the given StripBoardSceneBean.
	 * @param sBean
	 */
	private void omitStrip(StripBoardScene sBean) {
		log.debug("strip id=" + sBean.getId());
		Strip strip = getStripDAO().findById(sBean.getId());
		if (strip == null) {
			// unnatural - concurrent user update?
			refreshAll();
		}
		else {
			if (strip.getType() == StripType.BREAKDOWN) {
				omittedStripsCount++;
				if (strip.getStatus() == StripStatus.SCHEDULED) {
					updateScheduledStripsCount(-1);
					scheduleChanged();
				}
				SceneDAO.getInstance().updateScenesOmit(strip.getScenes(), getScript(), true);
				getStripDAO().omitStrips(strip.getSceneNumbers(), currentProject);
				updateLastSaved();
			}
			else if (strip.getType() == StripType.END_STRIPBOARD) {
				// TODO ignore? Message?
			}
			else { // banner or end-of-day strips get deleted
				boolean removed = stripboard.getStrips().remove(strip);
				if (!removed) {
					EventUtils.logError("remove failed, strip id=" + strip.getId());
				}
				getStripDAO().delete(strip);
				updateLastSaved();
				//stripboard = getStripboardDAO().merge(stripboard);
				if (strip.getType() == StripType.END_OF_DAY) {
					scheduleChanged();
				}
			}
		}
	}

	/**
	 * Do preparation of the right-hand side list, whether unscheduled
	 * or otherwise.
	 * Calls the superclass method to do the standard unscheduled list,
	 * then, if necessary, does additional setup for omitted, filtered,
	 * or right-hand-scheduled list.
	 */
	@Override
	protected void createUnscheduledList() {
		log.debug("");
		super.createUnscheduledList();	// Do base class setup first; also sets "totalStripsCount"
		try {
			if (isRightListScheduled()) {
				// Make a copy of the left scheduled list for the right scheduled list
				scheduleListRight.clear();
				StripBoardScene newBean;
				for ( StripBoardScene s : scheduleList) {
					newBean = (StripBoardScene) s.clone();
					scheduleListRight.add(newBean);
				}
			}
			int insert = unScheduleList.size()-1; // insert stuff before the END_STRIPBOARD strip
			if (insert < 0) insert = 0; // shouldn't happen!
			List<Strip> list = getStripDAO().findByStatusAndStripboard(StripStatus.OMITTED, stripboard);
			omittedStripsCount = list.size();
			totalStripsCount += omittedStripsCount;
			if (noFilterApplied()) {
				unScheduleListAll = null;
				if (omittedFlag) {
					omittedList = prepareStripTable(list);
					unScheduleList.addAll(insert,omittedList);
				}
			}
			else { // apply filters to unscheduled list
				unScheduleListAll = unScheduleList;
				if (omittedFlag) {
					omittedList = prepareStripTable(list);
					unScheduleListAll.addAll(insert,omittedList);
				}
				unScheduleList = applyFilters(unScheduleListAll, filterIE, filterDN);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * prepare the strip table
	 */
	// Currently not necessary to override the base class functionality
/*	protected List<StripBoardSceneBean> prepareStripTable(List<Strip> stripList) {
		List<StripBoardSceneBean> list = super.prepareStripTable(stripList);
		return list;
	}
*/
	/**
	 * Sorts the list of Strip (actually StripBoardSceneBean) data.
	 * The comparator is defined in StripBoardBaseBean.
	 */
	@Override
	protected void sort() {
		log.debug("");
		if (getSortColumnName()==null || getSortColumnName().length()==0) {
			return; // nothing required
		}
		refresh();	// refresh db objects

		List<StripBoardScene> list;
		if (noFilterApplied()) {
			list = unScheduleList;
		}
		else {
			list = unScheduleListAll;
		}

		Collections.sort(list, getComparator());

		boolean updated = getStripDAO().updateStripOrderNums(list);
		if (updated) {
			scheduleChanged();
		}

		if ( ! noFilterApplied()) {
			unScheduleList = applyFilters(unScheduleListAll, filterIE, filterDN);
		}
		//setStripBoardData(stripBoard);
	}


	/**
	 * Action called via hidden commandButton when user clicks on a strip
	 * in the left-hand (scheduled) list.
	 * @return null navigation string
	 */
	public String actionSelectLeft() {
		clearSelect(scheduleListRight);
		clearSelect(unScheduleList);
		clearSelect(unScheduleListAll);
		if (shiftList != LIST_SCH) {
			shiftIx = -1;
			shiftList = LIST_SCH;
		}
		selectRows(getScheduleList(), selectedScheduledLeftList);
		return null;
	}

	/**
	 * Action called via hidden commandButton when user clicks on a strip
	 * in the right-hand list when it is unscheduled strips.
	 * @return null navigation string
	 */
	public String actionSelectUnscheduled() {
		clearSelect(scheduleList);
		clearSelect(scheduleListRight);
		if (shiftList != LIST_UNSCH) {
			shiftIx = -1;
			shiftList = LIST_UNSCH;
		}
		selectRows(getUnScheduleList(), selectedUnscheduledList);
		return null;
	}

	/**
	 * Action called via hidden commandButton when user clicks on a strip
	 * in the right-hand list when it is scheduled strips.
	 * @return null navigation string
	 */
	public String actionSelectScheduledRight() {
		clearSelect(scheduleList);
		clearSelect(unScheduleList);
		clearSelect(unScheduleListAll);
		if (shiftList != LIST_SCH_RT) {
			shiftIx = -1;
			shiftList = LIST_SCH_RT;
		}
		selectRows(getScheduleListRight(), selectedScheduledRightList);
		return null;
	}

	/**
	 * Set the "selected" flags within the given List of beans appropriately,
	 * based on the row clicked, and the Ctrl and Shift key settings.
	 * @param beans The list of strips in which the "click" occurred.
	 * @return the row index of the just-clicked row
	 */
	private int selectRows(List<StripBoardScene> beans, Set<StripBoardScene> selected) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Map<String,String> requestMap = facesContext.getExternalContext().getRequestParameterMap();
		String shiftCtrl = requestMap.get("se:shiftCtrl");
		String row = requestMap.get("se:selectedRow");
		log.debug("sel'd row=" + row + ", shift/ctrl=" + shiftCtrl);

		boolean shift = false;
		boolean ctrl = false;
		if (shiftCtrl != null && shiftCtrl.length() == 2) {
			shift = (shiftCtrl.charAt(0)=='1');
			ctrl = (shiftCtrl.charAt(1)=='1');
		}
		int rowIx = -1;
		try {
			rowIx = Integer.parseInt(row);
		}
		catch(Exception e) {
			log.error(e);
		}

		if (rowIx >= 0 && rowIx < beans.size()) {
			StripBoardScene bean = beans.get(rowIx);
			if (! ctrl && (! shift || shiftIx < 0)) {
				// select only this strip
				clearSelect(beans);
				bean.setSelected(true);
				shiftIx = rowIx;
			}
			else if (shift) {
				clearSelect(beans);
				int incr = (rowIx < shiftIx ? 1 : -1);
				for (int i = rowIx; ; i+=incr) {
					bean = beans.get(i);
					bean.setSelected(true);
					if (i == shiftIx) {
						break;
					}
				}
			}
			else if (ctrl) { // toggle this strip's setting
				bean.setSelected(! bean.getSelected());
			}
		}

		StringBuffer s = new StringBuffer("selected scenes: ");
		selected.clear();
		for (StripBoardScene b : beans) {
			if (b.getSelected()) {
				selected.add(b);
				s.append(b.getSceneNumbers()).append(" ");
			}
		}
		log.debug(s.toString());
		return rowIx;
	}

	/**
	 * Process the pair of drag & drop events generated from the edit page.
	 *
	 * @param mt The type of move being processed.
	 * @param results The parse results, consisting of the old (source) index
	 *            and new (target) index.
	 * @param scheduledEvent True if the (right-hand) event passed to us was for
	 *            the scheduled list.
	 */
	private void processDragAndDrop(MoveType mt, List<Integer> results, boolean scheduledEvent) {

		if (results.size() == 0 ) {
			return; // no-op: user dragged & dropped strip back where it started
		}
		else if (results.size() != 2 ) {
			EventUtils.logError("unexpected D&D parameters: " + mt + ", indices: " + results.toString());
		}

		int oldIndex = results.get(0);
		int newIndex = results.get(1);

		boolean updateEOD = false;
		boolean error = false;
//
		switch (mt) {
			case ADD_BANNER_EOD:
				error = processAddBannerEOD(newIndex, scheduledEvent /*&& rtOp == DragDropOp.ADD*/);
				break;
			case IN_UNSCHEDULED:
				processMoveInUnscheduled(oldIndex, newIndex);
				break;
			case IN_LEFT_SCHEDULED:
				processMoveInLeftScheduled(oldIndex, newIndex);
				break;
			case IN_RIGHT_SCHEDULED:
				updateEOD = processMoveInRightScheduled(oldIndex, newIndex);
				break;
			case TO_SCHEDULED:
				updateEOD = processToScheduled(newIndex);
				break;
			case TO_UNSCHEDULED:
				updateEOD = processToUnscheduled(newIndex);
				break;
			case SHIFT_LEFT_SCHEDULED:
				updateEOD = processShiftLeftScheduled(oldIndex, newIndex);
				break;
			case SHIFT_RIGHT_SCHEDULED:
				updateEOD = processShiftRightScheduled(oldIndex, newIndex);
				break;
			case NONE:	// no discernible change, but EOD/Banner could have switched
				if (dragBannerList != null && dragBannerList.get(0) != null
						&& dragBannerList.get(0).getType() != StripType.BANNER) {
					createDragStrips(); // so recreate drag source
				}
				break;
			case REJECT: // undo whatever user did
				processReject(null /*rtOp*/);
				break;
			case INVALID:
				error = true;
				break;
		}

		clearSelectStatus(); // reset hidden fields that track selections
		boolean seqError = ! validateLists();
		if (error || seqError) {
			EventUtils.logEvent(EventType.DATA_ERROR, "Unexpected pair of D&D events, or sequence error");
			if (log.isDebugEnabled()) {
				log.debug("scheduled list:");
				for (StripBoardScene bean2 : scheduleList) {
					log.debug(bean2);
				}
				log.debug("UNscheduled list:");
				for (StripBoardScene bean2 : unScheduleList) {
					log.debug(bean2);
				}
			}
			if (seqError) {
				getStripDAO().resequenceOrderNumbers(stripboard, unit);
			}
//			StripBoardRequestBean srb = StripBoardRequestBean.getInstance();
//			PanelPositioned scheduledPanel = srb.getScheduledPanel();
//			if (scheduledPanel != null && scheduledPanel.getChildren() != null) {
//				// clearing the children may resolve mismatches between our table & framework
//				scheduledPanel.getSavedChildren().clear();
//				/*
//				 * After the ICEfaces 3.2 upgrade, this second clear() caused "duplicate component"
//				 * errors during the RenderResponsePhase of the next refresh, so we have
//				 * commented it out. rev 4001.
//				 */
//				//scheduledPanel.getChildren().clear();
//			}
			refreshAll();
			updateEOD = true;
			MsgUtils.addFacesMessage("Stripboard.DragDropFailed", FacesMessage.SEVERITY_ERROR);
		}
		if (updateEOD) {
			updateEndOfDay();
		}
		updateLastSaved();
		ltOp = DragDropOp.INVALID;
	}

	/**
	 * Process a "rejected" drag & drop operation. This should be either a strip
	 * dragged from one of the lists and dropped into the "banner/EOD source"
	 * list, or a banner or EOD dropped into the unscheduled list.
	 *
	 * @param rtOp The right-hand list drag & drop operation.
	 */
	private void processReject(DragDropOp rtOp) {
		if (ltOp == DragDropOp.REMOVE) {
			// possibly scheduled strip dragged & dropped into "add banner" list
			createScheduledList();		// rebuild to fix removal
		}
		else if (rtOp == DragDropOp.REMOVE) {
			createUnscheduledList();		// rebuild right-hand list to fix removal
		}
		else if (rtOp == DragDropOp.ADD) { // dropped banner/EOD in unscheduled list
			createUnscheduledList();		// rebuild to remove it
		}
		createDragStrips();		// rebuild to remove added Strip or fix order
	}

	/**
	 * @param newIndex
	 * @return updateEOD - If true, the end-of-day strips need to be updated.
	 */
	private boolean processToUnscheduled(int newIndex) {
		StripBoardScene sBean;
		boolean updateEOD = false;
		sBean = unScheduleList.get(newIndex);
		if (newIndex == unScheduleList.size()-1) { // it was added past End strip
			unScheduleList.remove(newIndex);
			newIndex--;
			unScheduleList.add(newIndex, sBean);
		}
		boolean moveOthers = false;
		if (sBean.getType() != StripType.BREAKDOWN) {
			// For EOD/Banner, if dragged from scheduled, and others selected,
			// we need to process those, but not move the EODs & Banners.
			if (newIndex >= 0) {
				scheduleList.add(newIndex, sBean); // put bean back in left list
				if (hasMultipleSelects(scheduleList)) {
					moveOthers = true; // others selected, process them
				}
			}
			else { // presumably dragged from "New EOD/Banner" source
				createDragStrips(); // need to recreate source strip(s)
			}
			if (! moveOthers) {
				// nothing else to do; remove the bean (it was put there by framework)
				unScheduleList.remove(newIndex);
			}
		}
		if (sBean.getType() == StripType.BREAKDOWN || moveOthers) {
			// Strip dragged from left (scheduled) to right (unscheduled)
			updateEOD = true;
			if (isOmittedFlag() && !isRightListScheduled() && newIndex > 0) {
				// check for dropping in omitted area
				if (unScheduleList.get(newIndex-1).getStatus() == StripStatus.OMITTED) {
					unScheduleList.remove(newIndex); // doesn't belong here
					while(--newIndex > 0 &&
							unScheduleList.get(newIndex-1).getStatus() == StripStatus.OMITTED) {
					}
					unScheduleList.add(newIndex, sBean); // just above first omitted strip
				}
			}
			if (noFilterApplied()) {
				moveStrips(sBean, LIST_UNSCH, newIndex, StripStatus.UNSCHEDULED, LIST_SCH, -1); //4
			}
			else { // more complicated; find equivalent spot in "All" list & insert there
				if (newIndex == 0) {
					unScheduleListAll.add(0, sBean);
				}
				else { // find strip preceding drop point; then find that strip in "All" list
					StripBoardScene preBean = unScheduleList.get(newIndex-1);
					newIndex = unScheduleListAll.indexOf(preBean) + 1;
					unScheduleListAll.add(newIndex, sBean); // insert in matching spot
				}
				// adjust newly-inserted strip's orderNumber to match its position
				moveStrips(sBean, LIST_ALL, newIndex, StripStatus.UNSCHEDULED, LIST_SCH, -1); //5
				// then recreate filtered list from "all" list
				unScheduleList = applyFilters(unScheduleListAll, filterIE, filterDN);
			}
		}
		return updateEOD;
	}

	/**
	 * A Strip has been moved within the unscheduled list.
	 * @param oldIndex
	 * @param newIndex
	 */
	private void processMoveInUnscheduled(int oldIndex, int newIndex) {
		StripBoardScene sBean;
		sBean = unScheduleList.get(newIndex);
		if (newIndex == unScheduleList.size()-1) { // it was moved past End strip
			unScheduleList.remove(newIndex);
			newIndex--;		// move it up one position, just above End strip
			unScheduleList.add(newIndex, sBean);
		}
		if (sBean.getType() == StripType.END_STRIPBOARD) {
			sBean = unScheduleList.remove(newIndex);
			unScheduleList.add(sBean); // puts it back at bottom
		}
		else {
			if (isOmittedFlag() & !isRightListScheduled()) { // check for dropping in omitted area
				if (oldIndex > newIndex &&
						sBean.getStatus() == StripStatus.OMITTED &&
						unScheduleList.get(newIndex+1).getStatus() != StripStatus.OMITTED) {
					// Omitted strip was dragged up into un-omitted part of list
					unScheduleList.remove(newIndex); // doesn't belong here
					while(++newIndex < unScheduleList.size()-1 &&
							unScheduleList.get(newIndex).getStatus() != StripStatus.OMITTED) {
					}
					unScheduleList.add(newIndex, sBean); // just above first omitted strip
				}
				else if (oldIndex < newIndex &&
						sBean.getStatus() != StripStatus.OMITTED &&
						unScheduleList.get(newIndex-1).getStatus() == StripStatus.OMITTED) {
					// un-omitted strip was dragged down into omitted part of list
					unScheduleList.remove(newIndex); // doesn't belong here
					while(--newIndex > 0 &&
							unScheduleList.get(newIndex-1).getStatus() == StripStatus.OMITTED) {
					}
					unScheduleList.add(newIndex, sBean); // just above first omitted strip
				}
			}
			moveStrips(sBean, LIST_UNSCH, newIndex, sBean.getStatus(), LIST_UNSCH, 0); //6
		}
	}

	/**
	 * A strip has been dragged from the unscheduled list to the scheduled list.
	 * Position strip with proper order number in left strip, using the index
	 * value saved from the left-side drop, & mark it Scheduled.
	 * @param newIndex
	 *
	 * @return updateEOD True if the end-of-day strips should be updated.
	 */
	private boolean processToScheduled(int newIndex) {
		boolean updateEOD = false;
		StripBoardScene sBean = scheduleList.get(newIndex); // where the bean got dropped
		if (sBean.getType() == StripType.END_STRIPBOARD) {
			sBean = scheduleList.remove(newIndex); // remove from left list
			unScheduleList.add(sBean); // puts it back at right bottom
		}
		else {
			if (newIndex == scheduleList.size()-1) { // it was added past End strip
				sBean = scheduleList.remove(newIndex);
				newIndex--;
				scheduleList.add(newIndex, sBean);
			}
			if (noFilterApplied()) {
				moveStrips(sBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_UNSCH, 1);
			}
			else { // framework updated unscheduleList, we need to update unScheduleListAll
				unScheduleListAll.remove(sBean); // remove from 'all' list
				// moveStrips will remove any other moved strips
				moveStrips(sBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_ALL, 1);
				// then we need to recreate the filtered list
				unScheduleList = applyFilters(unScheduleListAll, filterIE, filterDN);
			}
			updateEOD = true;
		}
		return updateEOD;
	}

	/**
	 * @param list
	 * @return true if the given list has more than one bean marked 'selected'.
	 */
	private boolean hasMultipleSelects(List<StripBoardScene> list) {
		int selections = 0;
		for (StripBoardScene bean : list) {
			if (bean.getSelected()) {
				if (selections > 0) {
					log.debug("true");
					return true;
				}
				selections++;
			}
		}
		log.debug("false: " + selections);
		return false;
	}

	/**
	 * Process a drag from the left-hand scheduled list into the right-hand
	 * scheduled list.
	 *
	 * @param newIndex The target - where the strip was dropped into the
	 *            right-hand list.
	 * @return True if end-of-day values need to be updated.
	 */
	private boolean processShiftRightScheduled(int oldIndex, int newIndex) {
		boolean updateEOD = false;
		StripBoardScene extraBean;
		StripBoardScene sBean = scheduleListRight.get(newIndex);
		if (sBean.getType() == StripType.END_STRIPBOARD) {
			// not allowed to move -- put it back at bottom of left list
			sBean = scheduleListRight.remove(newIndex);
			scheduleList.add(sBean);
		}
		else {
			sBean.setSelected(false);
			// remove "duplicate" from right side, and save it to stuff back into left
			if (oldIndex == newIndex || oldIndex == newIndex-1) {
				// Dragged to equivalent position -- turn it into no-op, by removing
				// the duplicate from the right-hand side and putting it back
				// in the left-hand list.
				extraBean = scheduleListRight.remove(oldIndex);
				scheduleList.add(oldIndex, extraBean);
			}
			else {	// moved up or down via drag from left to right
				updateEOD = true;
				if (oldIndex < newIndex) { // moved down from left to right
					extraBean = scheduleListRight.remove(oldIndex);
					newIndex--;	// add position (on right) is one too high due to "remove"
					if (newIndex == scheduleListRight.size()-1) {
						// strip was dropped below the "end stripboard" marker; move it up
						scheduleListRight.remove(newIndex);
						newIndex--;
						scheduleListRight.add(newIndex, sBean);
					}
				}
				else { // moved up from left to right
					extraBean = scheduleListRight.remove(oldIndex+1); // dupe got moved down a notch
				}
				scheduleList.add(newIndex, extraBean); // restore bean removed by framework
				moveStrips(extraBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_SCH, 0); //1
			}
		}
		return updateEOD;
	}

	/**
	 * Process a drag within the right-hand list when it is the scheduled list.
	 * @param oldIndex The index where the strip originated.
	 * @param newIndex The index where the strip was dropped.
	 *
	 * @return True if the end-of-day strips need to be updated.
	 */
	private boolean processMoveInRightScheduled(int oldIndex, int newIndex) {
		boolean updateEOD = false;
		StripBoardScene sBean = scheduleListRight.get(newIndex);
		if (sBean.getType() == StripType.END_STRIPBOARD) {
			sBean = scheduleListRight.remove(newIndex);
			scheduleListRight.add(sBean); // puts it back at bottom
		}
		else {
			if (newIndex == scheduleListRight.size()-1) { // it was added past End strip
				scheduleListRight.remove(newIndex);
				newIndex--;		// move it up one position, just above End strip
				scheduleListRight.add(newIndex, sBean);
			}
			updateEOD = true;
			sBean.setSelected(false);
			// Make equivalent move of bean within left-hand list:
			StripBoardScene extraBean = scheduleList.remove(oldIndex);
			scheduleList.add(newIndex, extraBean);
			moveStrips(extraBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_SCH_RT, 0); //2
		}
		return updateEOD;
	}

	/**
	 * Process a drag from the right-hand scheduled list into the left-hand
	 * scheduled list.
	 *
	 * @param newIndex The index on the right-hand list where the strip was
	 *            dragged from.
	 * @return True if the end-of-day strips need to be updated.
	 */
	private boolean processShiftLeftScheduled(int oldIndex, int newIndex) {
		boolean updateEOD = false;
		// Position strip with proper order number in left strip, using the
		// index value saved from the left-side drop.
		StripBoardScene sBean, extraBean;
		if (oldIndex == newIndex || oldIndex == newIndex-1) {
			// remove "duplicate" from left side, and save it to stuff back into right
			extraBean = scheduleList.remove(oldIndex);
			// Dragged to equivalent position -- turn it into no-op, by
			// restoring the bean removed from right list.
			scheduleListRight.add(oldIndex, extraBean);
		}
		else {	// moved up or down via drag from right to left
			sBean = scheduleList.get(newIndex); // bean that was moved by framework
			if (sBean.getType() == StripType.END_STRIPBOARD) {
				scheduleList.remove(newIndex);
				scheduleListRight.add(sBean);
			}
			else {
				if (oldIndex < newIndex) { // moved down
					extraBean = scheduleList.remove(oldIndex);
					newIndex--;	// add position (on left) is one too high due to "remove"
					if (newIndex == scheduleList.size()-1) {
						// strip was dropped below the "end stripboard" marker; move it up
						scheduleList.remove(newIndex);
						newIndex--;
						scheduleList.add(newIndex, sBean);
					}
				}
				else {
					extraBean = scheduleList.remove(oldIndex+1);
				}
				scheduleListRight.add(newIndex, extraBean); // replace bean in matching right-hand position
				sBean = scheduleList.get(newIndex); // bean that was moved by framework
				moveStrips(sBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_SCH_RT, 0); //3
				updateEOD = true;
			}
		}
		return updateEOD;
	}

	private List<StripBoardScene> applyFilters(List<StripBoardScene> list,
					String filterIntExt, String filterDayNt) {
		log.debug("IE=" + filterIntExt + ", DN=" + filterDayNt);
		List<StripBoardScene> filteredList = new ArrayList<>();
		boolean matchIE, matchDN;

		filterIntExt = filterIntExt.substring(0, 1); // = A/I/E = All/Interior/Exterior
		boolean allIE = filterIntExt.equals("A");
		IntExtType ieType = null;
		if (! allIE) {
			ieType = (filterIntExt.equals("I") ? IntExtType.INTERIOR : IntExtType.EXTERIOR);
		}

		filterDayNt = filterDayNt.substring(0, 1); // = A/D/N = All/Day/Night
		boolean allDN = filterDayNt.equals("A");
		DayNightType dnType = null;
		if (! allDN) {
			dnType = (filterDayNt.equals("D") ? DayNightType.DAY : DayNightType.NIGHT);
		}
		if (list != null) {
			for (StripBoardScene stripBean : list) {
				if (stripBean.getType() == StripType.END_STRIPBOARD) {
					filteredList.add(stripBean);
				}
				else {
					matchIE = allIE || stripBean.getIntExtType().getFilterType() == ieType;
					matchDN = allDN || stripBean.getDayNType().getFilterType() == dnType;
					if (matchIE && matchDN) {
						filteredList.add(stripBean);
					}
				}
			}
		}
		return filteredList;
	}

	/**
	 * The edit page no longer uses the individual fields that are trimmed, so
	 * skip the extra processing and just return the entire text.
	 */
	@Override
	protected String trimToWidth(String text, int width) {
		return text;
	}

	// Dropped by rev 2708; style change handled in stripboardeditm.jsp
//	@Override
//	protected String updateLayout(String newLayout) {
//		String style = super.updateLayout(newLayout);
//		updateLayout( unScheduleListAll, style);
//		updateLayout( scheduleListRight, style);
//		return style;
//	}

//	@Override
//	protected void updateLayout(List<StripBoardSceneBean> beans, String style) {
//		if (beans != null) {
//			for (StripBoardSceneBean sbean : beans) {
//				sbean.setStyle(style);
//			}
//		}
//	}

	/**
	 * Process adding a Banner or End-of-day strip into a scheduled list.
	 *
	 * @param rightIndex The index of where the strip was dropped on the
	 *            right-hand list. Only used if 'addedToRight' is true.
	 * @param addedToRight True if the banner or EOD strip was added to the
	 *            right-hand scheduled list.
	 * @return True if there was an error in the operation.
	 */
	private boolean processAddBannerEOD(int newIndex, boolean addedToRight) {
		boolean error = false;
		StripBoardScene sBean;
		int listSize;
		if (addedToRight) {
			sBean = scheduleListRight.get(newIndex);
			listSize = scheduleListRight.size();
		}
		else {
			sBean = scheduleList.get(newIndex);
			listSize = scheduleList.size();
		}
		if (sBean.getType() != StripType.BREAKDOWN &&
				sBean.getType() != StripType.END_STRIPBOARD) {
			// Source was EOD or Banner strip -- from "Add" source;
			createDragStrips(); // recreate drag source
			if (newIndex == (listSize - 1)) { // it was added past End strip
				if (addedToRight) {
					scheduleListRight.remove(newIndex);
					newIndex--;
					scheduleListRight.add(newIndex, sBean);
				}
				else {
					scheduleList.remove(newIndex);
					newIndex--;
					scheduleList.add(newIndex, sBean);
				}
			}
			if (addedToRight) {
				scheduleList.add(newIndex, (StripBoardScene)sBean.clone());
			}
			else if (isRightListScheduled()) { // keep lists in sync
				scheduleListRight.add(newIndex, (StripBoardScene)sBean.clone());
			}
			insertBannerEodStrip(newIndex); // create Strip to match sBean
		}
		else {
			error = true;
		}
		return error;
	}

	/**
	 * Process moving a strip within the left-hand (scheduled) list.
	 * @param oldIndex
	 * @param newIndex
	 */
	private void processMoveInLeftScheduled(int oldIndex, int newIndex) {
		if (newIndex < 0) {
			EventUtils.logError("newIndex invalid, =" + newIndex);
			return;
		}
		if (oldIndex < 0) {
			EventUtils.logError("oldIndex invalid, =" + oldIndex);
			return;
		}

		// Strip was dragged up or down within the left-hand (scheduled) list
		StripBoardScene sBean = scheduleList.get(newIndex);
		if (sBean.getType() == StripType.END_STRIPBOARD) {
			// not allowed to move this, so don't change order numbers, & put it back at end
			sBean = scheduleList.remove(newIndex);
			scheduleList.add(sBean);
		}
		else { // update orderNumbers to match move
			if (newIndex == scheduleList.size()-1) { // it was added past End strip
				scheduleList.remove(newIndex);
				newIndex--;
				scheduleList.add(newIndex, sBean);
			}
			if (isRightListScheduled()) { // keep lists in sync
				// Make equivalent move of bean within right-hand list:
				StripBoardScene extraBean = scheduleListRight.remove(oldIndex);
				scheduleListRight.add(newIndex, extraBean);
			}
			moveStrips(sBean, LIST_SCH, newIndex, StripStatus.SCHEDULED, LIST_SCH, 0); //8
			updateEndOfDay();
		}
	}

	private void createDragStrips() {
		dragBannerList.clear(); // = new ArrayList<>();
		StripBoardScene sceneBean =
				new StripBoardScene(StripType.BANNER, Constants.DRAG_PANEL_MESSAGE, "");
		sceneBean.setColorClass("xBANNER");
		setStripDisabled(false);
		dragBannerList.add(sceneBean);

		sceneBean =
				new StripBoardScene(StripType.END_OF_DAY, Constants.DRAG_END_OF_DAY_MESSAGE, "END_OF_DAY");
		sceneBean.setColorClass("xEOD");
		dragBannerList.add(sceneBean);
	}

	/**
	 * Insert a new End-of-Day or Banner Strip for the scheduled list. The
	 * StripBoardSceneBean is already in the list, at the position given by
	 * 'index'. A new Strip is created, its status is set to the parameter
	 * given, and it is added to the database. End-of-day information is also
	 * updated.
	 * <p>
	 * If the display is showing a right-hand-scheduled list also, then that
	 * list is updated to match the left-hand scheduled list.
	 *
	 * @param index
	 */
	private void insertBannerEodStrip(int index) {
		StripBoardScene sBean = scheduleList.get(index);
		int orderNum = calcInsertOrderNumber(scheduleList, index, 1);
		sBean.setOrderNumber(orderNum);
		sBean.setStatus(StripStatus.SCHEDULED);
//		sBean.setStyle(layout.equals(SB_LAYOUT_THIN) ? SB_STYLE_THIN : SB_STYLE_THICK);
		if (isRightListScheduled()) { // need to update bean in right hand list, too
			scheduleListRight.get(index).setStatus(StripStatus.SCHEDULED);
			scheduleListRight.get(index).setOrderNumber(orderNum);
		}

		Strip strip = new Strip(orderNum, sBean.getType(), StripStatus.SCHEDULED, stripboard);
		strip.setUnitId(getUnit().getId());
		getStripDAO().save(strip); // adds to database & sets id field
		sBean.setId(strip.getId());
		if (resequenced) {	// insert required resequencing all Strip order numbers
			refreshAll();	// refresh stripboard & all lists
		}
		//markSelect(scheduleList, index);
		if (sBean.getType() == StripType.END_OF_DAY) {
			updateEndOfDay();
			scheduleChanged();
		}
		else {
			showBannerPopup(index);
		}
	}

	private void updateEndOfDay() {
		stripboard = getStripboardDAO().refresh(stripboard); // needed by updateEndOfDay()
		updateEndOfDay(scheduleList, getUnit());
		if (isRightListScheduled()) {
			updateEndOfDay(scheduleListRight, getUnit());
		}
	}

	/**
	 * Move a Strip and all the related selected strips. The Strip bean
	 * indicated by the index parameter has already been moved, either by the
	 * framework or by our caller. It just needs its orderNumber adjusted to
	 * match its new position. Any additional selected Strips in the source list
	 * must be removed from the source list and added to the target list, and
	 * have their orderNumber adjusted properly.
	 * <p>
	 * If the target list is the unscheduled list, and the Strip dragged by the
	 * user is an EOD or Banner, we'll remove it from the unscheduled list,
	 * but move all other selected Breakdown Strips.  Any other selected EOD
	 * or Banner Strips are ignored -- left in the scheduled list.
	 *
	 * @param sBean SceneBean for the strip being moved.
	 * @param targetListType The "type" of list (left/right/right-scheduled) to
	 *            which the strips are being moved.
	 * @param index The index within the list where the sceneBean now resides.
	 * @param status The (possibly new) status to be assigned to the strip.
	 * @param sourceListType The "type" of the source list -- where the strip
	 *            originated; this is used to gather any other selected strips
	 *            to be moved along with the dragged one.
	 * @param scheduleCountChange Value of 0/1/-1, indicating how much to change
	 *            the scheduledStripsCount for each Strip moved.
	 */
	private void moveStrips(StripBoardScene sBean, int targetListType, int index,
			StripStatus status, int sourceListType, int scheduleCountChange) {
		shiftIx = -1;	// require new shift-click base to be set
		boolean withinList = (targetListType == sourceListType); // true if source list = target list
		withinList |= (sourceListType == LIST_SCH_RT); // acts like same==target
		int oldOrder = sBean.getOrderNumber();
		sBean.setSelected(false);
		List<StripBoardScene> targetList = listFromType(targetListType);

		// The dragged strip was physically moved within/between the list(s) by the framework,
		// so we just need to adjust its orderNumber to match the new location:
		if (sBean.getType() == StripType.BREAKDOWN || status == StripStatus.SCHEDULED) {
			updateStripOrder(sBean, listFromType(targetListType), index, status, 1);
		}
		else {
			// EOD or Banners dragged to unscheduled list must be removed.
			targetList.remove(index);
			oldOrder = Integer.MAX_VALUE; // so re-insert of others won't increment index
		}
		int newOrder = sBean.getOrderNumber();
		boolean hadReseq = resequenced; // need to check any 'moveStrip' for resequenced flag

		List<StripBoardScene> sourceList = listFromType(sourceListType);
		if (sourceListType == LIST_SCH_RT || isRightListScheduled()) {
			// copy order number to right-side bean
			scheduleListRight.get(index).setOrderNumber(sBean.getOrderNumber());
		}

		// Make a List of all selected strips and remove them from the scheduled list

		List<StripBoardScene> moving = new ArrayList<>();
		List<StripBoardScene> movingRt = new ArrayList<>();
		StripBoardScene movedBean, rtBean;
		int rowIndex = 0; // the original row number of the bean we're testing/removing
		int sourceIx = 0; // the actual row number we're testing/removing
		for (Iterator<StripBoardScene> iter = sourceList.iterator(); iter.hasNext(); rowIndex++) {
			movedBean = iter.next();
			if (movedBean.getSelected()) {
				movedBean.setSelected(false);
				if (movedBean.getType() == StripType.BREAKDOWN || targetListType == LIST_SCH) {
					log.debug("row=" + rowIndex + ", srcIx=" + sourceIx + ", id=" +
							movedBean.getId() + ", scn=" + movedBean.getSceneNumbers());
					if (sourceListType == LIST_SCH_RT) {
						movingRt.add(movedBean);
						movedBean = scheduleList.remove(sourceIx);
					}
					moving.add(movedBean);
					iter.remove();
					if (sourceListType == LIST_SCH && isRightListScheduled()) {
						rtBean = scheduleListRight.remove(sourceIx);
						movingRt.add(rtBean);
					}
					if (withinList && (movedBean.getOrderNumber() < newOrder)) {
						// we're removing a row that was above the strip that was dragged/dropped,
						index--;	// so adjust the target row accordingly.
					}
				}
				else {
					sourceIx++;
				}
			}
			else {
				sourceIx++;
			}
		}

		updateScheduledStripsCount(scheduleCountChange);
		int moveCount = moving.size();
		if (moveCount > 0) {
			updateScheduledStripsCount(scheduleCountChange * moveCount);
			// Now re-insert them at the new location, and update their order numbers to match
			boolean looking = true;
			int ix = 0; // index of current strip within moving or movingRt list
			for (StripBoardScene bean : moving) {
				if ((bean.getOrderNumber() > oldOrder) && looking) {
					// this is the first extra strip that (was) past the dragged one
					index++; 		// bump index past the dragged one (it's here already)
					looking = false; // don't do this again
				}
				targetList.add(index, bean); // insert moved bean at right slot
				// update the orderNumber of the moved bean
				updateStripOrder(bean, listFromType(targetListType), index, status, moveCount);
				moveCount--; // used to calculate inserted orderNumber value
				hadReseq |= resequenced; // need to check any 'moveStrip' for resequenced flag
				if (targetListType == LIST_SCH && isRightListScheduled()) {
					rtBean = movingRt.get(ix);
					rtBean.setOrderNumber(bean.getOrderNumber());
					scheduleListRight.add(index,rtBean);
				}
				index++; // bump the insertion index
				ix++;	 // bump source index
			}
		}
		if (hadReseq) {	// At some point all the strips got new sequence numbers!
			refreshAll();	// so refresh stripboard & all lists
		}
	}

	/**
	 * Move a strip (& bean) within the given list by adjusting its order number to match its
	 * current position (as given by 'index') within the list. The status value is also updated as
	 * specified.
	 *
	 * @param sBean The sceneBean being moved.
	 * @param list  The list to which the bean has been moved.
	 * @param index The index within the list where the sceneBean now resides.
	 * @param status The (possibly new) status to be assigned to the strip.
	 * @param moveCount The estimated number of strips which will be moved into this same
	 *         position.
	 * @return boolean - true if move was successful; false if not -- this normally happens if the
	 *         order numbers of the strips becomes sequential and there is no room to insert the
	 *         moved strip. Issuing a second call to moveStrip will usually work, as the strips
	 *         should have been resequenced by the first (failing) call.
	 */
	private boolean updateStripOrder(StripBoardScene sBean, List<StripBoardScene> list,
			int index, StripStatus status, int moveCount) {
		log.debug("id=" + sBean.getId() + ", scn=" + sBean.getSceneNumbers() +
				", ix=" + index + ", " + status);
		int orderNum = calcInsertOrderNumber(list, index, moveCount);
		if (orderNum >= 0) {
			sBean.setOrderNumber(orderNum);
		}
		else {
			EventUtils.logError("StripboardEditorBean: no room for inserting a new orderNumber; resequence failed!");
			orderNum = ((index+1) * StripDAO.ORDER_INCREMENT) - 1;
			log.warn("trying new order number=" + orderNum);
			sBean.setOrderNumber(orderNum);
			if (! validateListOrder(list, status)) {
				log.debug("validate failed");
				resequenced = true; // force refresh of lists
			}
		}
		if (sBean.getStatus() == StripStatus.OMITTED && status != StripStatus.OMITTED) {
			omittedStripsCount--;  // un-omitting strip, decrement count
		}
		sBean.setStatus(status);
		Strip strip = getStripDAO().findById(sBean.getId());
		strip.setOrderNumber(orderNum);
		if (strip.getStatus() == StripStatus.OMITTED && status != StripStatus.OMITTED) {
			// changing from Omitted to something else; update Scene object as well
			SceneDAO sceneDao = SceneDAO.getInstance();
			Scene scene = sceneDao.findByStripAndScript(strip, getScript());
			if (scene != null) {
				scene.setOmitted(false);
				sceneDao.attachDirty(scene);
			}
		}
		strip.setStatus(status);
		if (status == StripStatus.SCHEDULED) {
			strip.setUnitId(getUnit().getId());
		}
		else {
			strip.setUnitId(null);
		}
		strip = getStripDAO().merge(strip);
		scheduleChanged();
		return true;
	}

	private List<StripBoardScene> listFromType(int listType) {
		switch (listType) {
			case LIST_ALL:
				return unScheduleListAll;
			case LIST_SCH:
				return scheduleList;
			case LIST_UNSCH:
				return unScheduleList;
			default:
				return scheduleListRight;
		}
	}

	/**
	 * Calculate an appropriate orderNumber for inserting a new strip into the given list, at the
	 * index point specified. The list is assumed to already include the item -- so its size must be
	 * at least 1.
	 *
	 * @param list The list containing the StripBoardSceneBean for the Strip being inserted.
	 * @param index The index (origin 0) of the StripBoardSceneBean that has been inserted.
	 * @param moveCount The (estimated) number of Strips which will be inserted into
	 *            this same location in the list (for a multi-strip move).
	 * @return The new orderNumber to be assigned to the Strip and its associated sceneBean.
	 *         <p>
	 *         NOTE: The caller must check the 'resequenced' instance boolean -- if true, after
	 *         updating the Strip's orderNumber, refreshAll() should be called to rebuild all the
	 *         sceneBean lists.
	 */
	private int calcInsertOrderNumber(List<StripBoardScene> list, int index, int moveCount) {
		resequenced = false;
		int order = calcInsertOrderNumberSub(list, index, moveCount);
		if (order < 0) {	// we re-set all the order numbers; try it once more...
			resequenced = true;
			for (StripBoardScene bean : list) {
				// copy the new Strip orderNumbers into the scene beans
				if (bean.getId() != null) {
					Strip s = getStripDAO().findById(bean.getId());
					if (s != null) {
						bean.setOrderNumber(s.getOrderNumber());
					}
				}
			}
			order = calcInsertOrderNumberSub(list, index, moveCount);
		}
		return order;
	}

	/**
	 * Calculate an appropriate orderNumber for inserting a new strip into the
	 * given list, at the index point specified. The list is assumed to already
	 * include the item -- so its size must be at least 1.
	 * <p>
	 * If the item is being inserted at the end of a list, the standard increment
	 * value will be used.  Otherwise, a number will be picked in the "gap" between
	 * the preceding and following strips.
	 * <p>
	 * If the moveCount parameter is > 1, then the new orderNumber is calculated
	 * so that number of strips will be evenly distributed in the target gap.
	 * Otherwise, the new orderNumber is calculated as the middle of the target gap.
	 *
	 * @param list The list containing the StripBoardSceneBean for the Strip
	 *            being inserted.
	 * @param index The index (origin 0) of the StripBoardSceneBean that has
	 *            been inserted.
	 * @param moveCount The (estimated) number of Strips which will be inserted into
	 *            this same location in the list (for a multi-strip move).
	 * @return The new orderNumber to be assigned to the Strip and its
	 *         associated sceneBean, or -1 if there was no room to insert a new
	 *         orderNumber, and the strips have been resequenced. In the latter
	 *         case, the caller should update the sceneBean's with the new
	 *         orderNumbers, and call this method again.
	 */
	private int calcInsertOrderNumberSub(List<StripBoardScene> list, int index, int moveCount) {
		int orderNum = 0;
		int nextNum = -1;
		int priorNum = 0;
		if (index >= list.size()-2) { // adding entry at end (before End-Stripboard entry)
			if (index > 0) {
				StripBoardScene sBean = list.get(index-1); // get previous last entry
				if (sBean.getStatus() == list.get(index).getStatus() ||
						list.get(index).getStatus() != StripStatus.OMITTED) {
					orderNum = sBean.getOrderNumber() + StripDAO.ORDER_INCREMENT;
				}
				else { // must be inserting (only) Omitted strip after un-omitted strip
					orderNum = StripDAO.ORDER_INCREMENT;
				}
			}
			else { // item being added is only one in list
				orderNum = StripDAO.ORDER_INCREMENT;
			}
		}
		else { // New one is not last in list; calculate an order number part way
			// between the items above & below the newly-inserted item.
			if (list.get(index+1).getStatus() != StripStatus.OMITTED ||
					list.get(index).getStatus() == StripStatus.OMITTED) {
				nextNum = list.get(index+1).getOrderNumber(); // get the one following
				if (index > 0 && list.get(index-1).getStatus() == list.get(index+1).getStatus()) {
					// there is one preceding, and it's the same status (omitted vs unscheduled)
					priorNum = list.get(index-1).getOrderNumber();
				}
				int gap = nextNum - priorNum;
				if (gap >= (2*StripDAO.ORDER_INCREMENT)) {
					orderNum = priorNum + StripDAO.ORDER_INCREMENT;
				}
				else {
					if (moveCount > 1 && gap > moveCount) {
						orderNum = priorNum + gap/(moveCount+1);
					}
					else {
						orderNum = priorNum + (gap+1)/2;
					}
				}
				if (orderNum >= list.get(index+1).getOrderNumber()) {
					log.debug("prev=" + priorNum + ", nxt=" + nextNum + ", FAILED=" + orderNum);
					orderNum = -1; // force re-sequencing
				}
			}
			else { // must be inserting the last unscheduled strip prior to omitted strip(s)
				orderNum = StripDAO.ORDER_INCREMENT;
				if (index > 0) { // is there a preceding one?
					orderNum += list.get(index-1).getOrderNumber();
				}
			}
		}
		log.debug("moveCnt=" + moveCount + ", prev=" + priorNum + ", nxt=" + nextNum + ", new=" + orderNum);
		if (orderNum < 0 ) {
			log.debug("no room for inserting a new orderNumber -- resequencing");
			getStripDAO().resequenceOrderNumbers(stripboard, unit);
			//log.debug("strip order numbers reset");
			orderNum = -1; // "flag" that we redid ordering
		}
		return orderNum;
	}

	private boolean validateLists() {
		boolean b1 = true;
		boolean b2 = true;
		if (log.isDebugEnabled()) {
			b1 = validateListOrder(scheduleList, StripStatus.SCHEDULED);
			b2 = validateListOrder(unScheduleList, StripStatus.UNSCHEDULED);
			log.debug("  validate: scheduled=" + b1 + ", unscheduled=" + b2);
		}
		return (b1 && b2);
	}

	private boolean validateListOrder(List<StripBoardScene> list, StripStatus status) {
		boolean valid = true;
		int lastOrder = 0;
		for (StripBoardScene bean : list) {
			if (bean.getStatus() != status) {
				// unscheduled list may end with omitted items - deal with it
				if (status == StripStatus.UNSCHEDULED &&
						bean.getStatus() == StripStatus.OMITTED) {
					status = StripStatus.OMITTED;
					lastOrder = 0;
				}
				else if (status == StripStatus.OMITTED &&
						bean.getStatus() == StripStatus.UNSCHEDULED &&
						bean.getType() == StripType.END_STRIPBOARD) {
					status = StripStatus.UNSCHEDULED;
				}

			}
			if (bean.getOrderNumber() <= lastOrder || bean.getStatus() != status) {
				valid = false;
				log.error("out of sequence; count=" + list.size() + ", lastOrder=" + lastOrder + ", next bean: " + bean);
				for (StripBoardScene bean2 : list) {
					log.debug(bean2);
				}
				break;
			}
			lastOrder = bean.getOrderNumber();
		}
		StripBoardScene bean = list.get(list.size()-1);
		if (bean.getType() != StripType.END_STRIPBOARD) {
			log.error("missing EndStripboard strip; count=" + list.size() + ", last bean=" + bean);
			valid = false;
		}
		return valid;
	}

	private void dumpLists() {
		dumpList("scheduled", scheduleList);
		dumpList("UNscheduled", unScheduleList);
		dumpList("sel'd left", selectedScheduledLeftList);
		dumpList("sel'd rt (sch)", selectedScheduledRightList);
		dumpList("sel'd rt (unsch)", selectedUnscheduledList);
	}

	private void dumpList(String title, Collection<StripBoardScene> list) {
		StringBuffer msg = new StringBuffer(title + ": ");
		for (StripBoardScene scn : list) {
			msg.append(scn.getSceneNumbers()).append(";");
		}
		log.debug(msg.toString());
	}

	private static int findRowNumberFromId(String elemId) {
		int row = -1;
		String[] parts = elemId.split(":");
		try {
			row = Integer.parseInt(parts[2]);
		}
		catch (Exception e) {
			EventUtils.logError("StripboardEditorBean, exception parsing, elemId=" + elemId);
		}
		log.debug("id=" + elemId + ", row=" + row);
		return row;
	}

	/**
	 * Refresh our data for display.  Used (at least) after the strips
	 * have had their order numbers resequenced by calcInsertOrderNumber.
	 */
	private void refreshAll() {
		refresh();	// refresh stripboard
		setStripBoardData(stripboard); // refresh all lists
		clearSelectStatus();
	}

	/**
	 * Save possibly changed description across refresh calls.
	 * @see com.lightspeedeps.web.schedule.StripBoardBase#refresh()
	 */
	@Override
	protected void refresh() {
		String description = new String(stripboard.getDescription());
		super.refresh();
		stripboard.setDescription(description);
	}

	/**
	 * Called when some change that will affect the schedule has been
	 *  posted to the database.
	 *  Currently this just marks the DooD data dirty, so it will get recreated
	 *  when necessary.
	 */
	private void scheduleChanged() {
		ProductionDood.markUnitDirty(unit); // DooD is probably out of date
	}

	/**
	 * Update the current Stripboard's "last saved" date to the current date/time.
	 */
	private void updateLastSaved() {
		stripboard.setLastSaved(new Date());
		stripboard.setUser(SessionUtils.getCurrentUser());
		getStripboardDAO().attachDirty(stripboard);
	}

	/**
	 * The ActionListener method on the "Save" button of the
	 * Edit Banner Text pop-up dialog.
	 * @return navigation string to the same page, to force lists to refresh.
	 */
	public String actionSaveBannerText() {
		log.debug("ix=" + editBannerIndex);
		try {
			refresh();	// refresh db objects
			String text = getBannerText();
			if (text.length() > 40) {
				// make sure long text will split into multiple lines automatically
				text = StringUtils.hyphenate(text);
			}
			int rowNumber = editBannerIndex; // findRowNumberFromId(getSelectLeftScheduled());
			if (rowNumber >= 0 && rowNumber < scheduleList.size()) {
				StripBoardScene sBean = scheduleList.get(rowNumber);
				if (sBean != null) {
					sBean.setComment(text);
					Strip strip = getStripDAO().findById(sBean.getId());
					if (strip != null) {
						strip.setComment(text);
						strip = getStripDAO().merge(strip);
					}
					if (isRightListScheduled()) {
						scheduleListRight.get(rowNumber).setComment(text);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		showPopup = false;
//		ListView.addClientResizeScroll(); // preserve scroll position
		return FORCE_REFRESH; // was null
	}

	/**
	 * ActionListener method to close the banner-text edit pop-up.
	 * @return navigation string to the same page, to force lists to refresh.
	 */
	public String actionCloseBannerPopup() {
		showPopup = false;
//		ListView.addClientResizeScroll(); // preserve scroll position
		return FORCE_REFRESH; // was null
	}

	/**
	 * ActionListener method to display the banner-text edit pop-up.
	 */
	public void actionShowBannerPopup(ActionEvent e) {
		log.debug("");
		if (! getSelectLeftScheduled().equals("none")) {
			int rowNumber = findRowNumberFromId(getSelectLeftScheduled());
			showBannerPopup(rowNumber);
		}
	}

	private void showBannerPopup(int index) {
		showPopup = true;
		StripBoardRequestBean srb = StripBoardRequestBean.getInstance();
		if (srb.getBannerText() != null) {
			// TODO
//			srb.getBannerText().requestFocus();
		}
		if (index < scheduleList.size()-1) { // exclude end strip
			editBannerIndex = index;
			StripBoardScene s = scheduleList.get(index);
			setBannerText(s.getComment());
		}
	}

	/**
	 * Set all the beans in the provided list to the un-selected state.
	 * @param beans
	 */
	private void clearSelect(Collection<StripBoardScene> beans) {
		if (beans != null) {
			for (StripBoardScene s: beans ) {
				s.setSelected(false);
			}
		}
	}

	/**
	 * Reset the hidden fields on the JSP which are used by JavaScript code to
	 * track selected rows.
	 */
	private void clearSelectStatus() {
		setSelectLeftScheduled(SELECT_UNSET);
		setSelectRightScheduled(SELECT_UNSET);
		setSelectRightUnscheduled(SELECT_UNSET);
	}

	/**
	 * @return the html element id of the left-hand row
	 * that is selected.
	 */
	public String getSelectLeftScheduled() {
		return selectLeftScheduled;
	}
	/**
	 * @param selectRowSchedule the selectRowSchedule to set
	 */
	public void setSelectLeftScheduled(String selectRowSchedule) {
		selectLeftScheduled = selectRowSchedule;
	}

	/**
	 * The text displayed in the "Edit banner" pop-up.
	 */
	public String getBannerText() {
		return bannerText;
	}
	public void setBannerText(String bannerText) {
		this.bannerText = bannerText;
	}

	/**
	 * @return the omitValue
	 */
	public String getOmitValue() {
		return omitValue;
	}
	/**
	 * @param omitValue the omitValue to set
	 */
	public void setOmitValue(String omitValue) {
		this.omitValue = omitValue;
	}

	/**
	 * @return The html element id of the row selected on the
	 * right-hand side list when it is the unscheduled list.
	 */
	public String getSelectRightUnscheduled() {
		return selectRightUnscheduled;
	}
	/**
	 * @param selectRightRowSchedule the selectRightRowSchedule to set
	 */
	public void setSelectRightUnscheduled(String selectRightRowSchedule) {
		selectRightUnscheduled = selectRightRowSchedule;
	}

	/**
	 * @return The stripDrag setting; used in JSP to test if drag should be enabled.
	 */
//	public boolean getStripDrag() {
//		return stripDrag;
//	}
	/**
	 * @param stripDrag the stripDrag to set
	 */
//	public void setStripDrag(boolean stripDrag) {
//		this.stripDrag = stripDrag;
//	}

	/**
	 * @return the stripDisabled
	 */
	public boolean getStripDisabled() {
		return stripDisabled;
	}
	/**
	 * @param stripDisabled the stripDisabled to set
	 */
	public void setStripDisabled(boolean stripDisabled) {
		this.stripDisabled = stripDisabled;
	}

	/**
	 * @return the selectRightScheduled
	 */
	public String getSelectRightScheduled() {
		return selectRightScheduled;
	}
	/**
	 * @param selectRightScheduled the selectRightScheduled to set
	 */
	public void setSelectRightScheduled(String selectRightScheduled) {
		this.selectRightScheduled = selectRightScheduled;
	}

	/** See {@link #selectedScheduledRightList}. */
	public Set<StripBoardScene> getSelectedScheduledRightList() {
		return selectedScheduledRightList;
	}
	/** See {@link #selectedScheduledRightList}. */
	public void setSelectedScheduledRightList(Set<StripBoardScene> selectedScheduledListRight) {
		this.selectedScheduledRightList = selectedScheduledListRight;
	}

	/** See {@link #selectedScheduledLeftList}. */
	public Set<StripBoardScene> getSelectedScheduledLeftList() {
		return selectedScheduledLeftList;
	}
	/** See {@link #selectedScheduledLeftList}. */
	public void setSelectedScheduledLeftList(Set<StripBoardScene> selectedScheduledListLeft) {
		this.selectedScheduledLeftList = selectedScheduledListLeft;
	}

	/** See {@link #selectedUnscheduledList}. */
	public Set<StripBoardScene> getSelectedUnscheduledList() {
		return selectedUnscheduledList;
	}
	/** See {@link #selectedUnscheduledList}. */
	public void setSelectedUnscheduledList(Set<StripBoardScene> selectedUnscheduledList) {
		this.selectedUnscheduledList = selectedUnscheduledList;
	}

	public int getLeftEventType() {
		return leftEventType;
	}
	public void setLeftEventType(int leftEventType) {
		this.leftEventType = leftEventType;
	}

//	public int getEventOldIndex() {
//		return eventOldIndex;
//	}
//	public void setEventOldIndex(int leftEventOldIndex) {
//		this.eventOldIndex = leftEventOldIndex;
//	}


	/**
	 * This is a hidden value on the edit page which tracks the
	 * strip.status value for the currently selected strip.
	 * @return the stripStatus
	 */
	public String getStripStatus() {
		return stripStatus;
	}
	/**
	 * @param stripStatus the stripStatus to set
	 */
	public void setStripStatus(String stripStatus) {
		this.stripStatus = stripStatus;
	}

	/**
	 * @return the dragBannerList
	 */
	public List<StripBoardScene> getDragBannerList() {
		return dragBannerList;
	}
	/**
	 * @param dragBannerList
	 *            the dragBannerList to set
	 */
	public void setDragBannerList(List<StripBoardScene> dragBannerList) {
		this.dragBannerList = dragBannerList;
	}

	/**
	 * @return the dragEndOfDayList
	 */
	public List<StripBoardScene> getDragEndOfDayList() {
		return dragEndOfDayList;
	}
	/**
	 * @param dragEndOfDayList
	 *            the dragEndOfDayList to set
	 */
	public void setDragEndOfDayList(List<StripBoardScene> dragEndOfDayList) {
		this.dragEndOfDayList = dragEndOfDayList;
	}

	public String getSelectedSchedule() {
		return selectedSchedule;
	}
	public void setSelectedSchedule(String str) {
		selectedSchedule = str;
	}
	/** True if the right-side list is showing Scheduled strips, rather
	 * than (the more typical) unscheduled strips. */
	public boolean isRightListScheduled() {
		return (getSelectedSchedule().equalsIgnoreCase(STB_FILTER_SCHEDULED));
	}

	public String getFilterIE() {
		return filterIE;
	}
	public void setFilterIE(String filterIE) {
		this.filterIE = filterIE;
	}

	public String getFilterDN() {
		return filterDN;
	}
	public void setFilterDN(String filterDN) {
		this.filterDN = filterDN;
	}

	private boolean noFilterApplied() {
		if (filterIE == null || filterDN == null) { // bizarre case?!
			return true;
		}
		return filterIE.equalsIgnoreCase(FILTER_ALL) && filterDN.equalsIgnoreCase(FILTER_ALL);
	}

	/** See {@link #omittedFlag}. */
	public boolean isOmittedFlag() {
		return omittedFlag;
	}
	/** See {@link #omittedFlag}. */
	public void setOmittedFlag(boolean omittedFlag) {
		this.omittedFlag = omittedFlag;
	}

	public List<StripBoardScene> getScheduleListRight() {
		return scheduleListRight;
	}
	public void setScheduleListRight(List<StripBoardScene> scheduleListRight) {
		this.scheduleListRight = scheduleListRight;
	}

	/**
	 * @return the showPopup
	 */
	public boolean isShowPopup() {
		return showPopup;
	}
	/**
	 * @param showPopup the showPopup to set
	 */
	public void setShowPopup(boolean showPopup) {
		this.showPopup = showPopup;
	}

	/** See {@link #selectedRow}. */
	public int getSelectedRow() {
		return selectedRow;
	}
	/** See {@link #selectedRow}. */
	public void setSelectedRow(int row) {
		//log.debug(row);
		selectedRow = row;
	}

	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		log.debug("");
		in.defaultReadObject();
		projectDAO = ProjectDAO.getInstance();
		log.debug("done");
	}

	/**
	 * This method is called by the JSF framework when this bean is about to go
	 * 'out of scope', e.g., when the user is leaving the page. Note that in JSF
	 * 2.1, this method is not called for session expiration, so we handle that
	 * case via the Disposable interface.
	 */
	@PreDestroy
	public void preDestroy() {
		log.debug("");
		if (disposer != null) {
			disposer.unregister(this);
		}
		dispose();
	}

	/**
	 * This method is called when this bean is about to go 'out of scope', e.g.,
	 * when the user is leaving the page or their session expires. We use it to
	 * unlock the stripboards to make them available again for editing.
	 */
	@Override
	public void dispose() {
		log.debug("");
		Project project = projectDAO.refresh(currentProject); // prevent "non-unique object" failure in logout case
		projectDAO.unlock(project, lockingUserId);
	}

}
