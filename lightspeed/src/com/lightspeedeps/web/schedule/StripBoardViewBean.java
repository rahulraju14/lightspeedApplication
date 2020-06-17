package com.lightspeedeps.web.schedule;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.StripBoardScene;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.View;

import java.io.Serializable;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Backing bean for the Strip Board Viewer page.
 * Most of the functionality is in the StripBoardBaseBean class, which is
 * also used by the StripBoardEditorBean class.
 * The main functionality added here is the selection list of stripboards,
 * allowing the user to switch between them easily.
 */
@ManagedBean
@ViewScoped
public class StripBoardViewBean extends StripBoardBase implements Serializable {
	/** */
	private static final long serialVersionUID = 6917696989439844994L;

	private static final Log log = LogFactory.getLog(StripBoardViewBean.class);

	private static final String SCROLL_SCHEDULED_STRIP_JS = "scrollToStrip";
	private static final String SCROLL_UNSCHEDULED_STRIP_JS = "scrollToUnscheduledStrip";

	private static final String ATTR_STRIPBOARD_VIEW_MINI_TAB = Constants.ATTR_CURRENT_MINI_TAB + "StripboardView";

	/** The zero-origin index of the page's currently selected tab.
	 * This is set via the &lt ice:panelTabSet selectedIndex=...> attribute. */
	private int selectedTab = 0;

	/** The stripboardList for the drop-down selection box. */
	private List<SelectItem> stripboardList;

	protected List<StripBoardScene> omittedList;

	/** The object bound to the "Ids" column on Stripboard View page. */
//	private HtmlDataTable unscheduledTable;

	/** True if the "Edit" button on the View page should be enabled. */
	private boolean enableEdit;

	public StripBoardViewBean() {
		log.debug("");
		omittedList = new ArrayList<StripBoardScene>(); // in case there's no stripboard
		initialize(); // initialize our super class AFTER we're constructed!
		enableEdit = true;
		if (unitIndex > 1) { // A unit other than the main unit is current
			setSelectedTab(unitIndex-1);
		}
		else {
			Integer tab = SessionUtils.getInteger(ATTR_STRIPBOARD_VIEW_MINI_TAB);
			if (tab != null) {
				setSelectedTab(tab);
			}
		}
		try {
			// For strip board drop down list
			List<Stripboard> boardList = getStripboardDAO().findByProperty("project", currentProject);
			stripboardList = new ArrayList<SelectItem>(boardList.size());
			for (Stripboard board : boardList) {
				stripboardList.add(new SelectItem(board.getId(),board.getDescription()));
			}
			String sceneNum = SessionUtils.getString(Constants.ATTR_SB_VIEW_SCENE_NUM);
			if (sceneNum != null) {
				scrollToScene(sceneNum);
				SessionUtils.put(Constants.ATTR_SB_VIEW_SCENE_NUM, null);
			}
			else {
				String viewDay = SessionUtils.getString(Constants.ATTR_SB_VIEW_DAY_NUMBER);
				if (viewDay != null) {
					scrollToDay(viewDay);
					SessionUtils.put(Constants.ATTR_SB_VIEW_DAY_NUMBER, null);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * valueChangeListener on the drop-down list of Stripboards
	 * @param event
	 */
	public void listenSelectedStripboardId(ValueChangeEvent event) {
		try {
			Integer selectedStripboardId = (Integer) event.getNewValue();
			log.debug("selected id=" + selectedStripboardId);
			stripboard = getStripboardDAO().findById(selectedStripboardId);
			selectUnit(currentProject.getMainUnit());
			setSelectedTab(0);
			setStripBoardData(stripboard);
//			ListView.addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError("Error in listenSelectedStripboardId: ", e);
		}
	}

	/**
	 * This method is responsible for creating both the unscheduled list, AND
	 * the omitted list.  We override the super classes method -- then call it
	 * to handle the unscheduled list, then do the omitted list here.
	 */
	@Override
	protected void createUnscheduledList() {
		log.debug("");
		super.createUnscheduledList();
		if (unScheduleList.size() > 0) {
			if (unScheduleList.get(unScheduleList.size()-1).getType() == StripType.END_STRIPBOARD) {
				unScheduleList.remove(unScheduleList.size()-1);
			}
		}
		List<Strip> list = getStripDAO().findByStatusAndStripboard(StripStatus.OMITTED, stripboard);
		omittedList = prepareStripTable(list);
		omittedStripsCount = breakdownStripCount;
		totalStripsCount += omittedStripsCount;
		log.debug("omit=" + omittedStripsCount + ", tot=" + totalStripsCount);
	}

	/**
	 * @see com.lightspeedeps.web.schedule.StripBoardBase#createScheduledList()
	 */
	@Override
	protected void createScheduledList() {
		super.createScheduledList();
		if (scheduleList.size() > 0) {
			if (scheduleList.get(scheduleList.size()-1).getType() == StripType.END_STRIPBOARD) {
				scheduleList.remove(scheduleList.size()-1);
			}
		}
	}

	@Override
	protected void createFixedStrip(StripBoardScene sb, Strip strip, Scene scn) {
		// no-op this function for Stripboard View
	}

	private void scrollToScene(String scn) {
		int ix;
		// Note that Omitted strips don't get a link, so we don't scan that list
		for (int unitIx = 1; unitIx < unitStripList.length; unitIx++ ) {
			List<StripBoardScene> sList = unitStripList[unitIx];
			if (sList != null) {
				ix = 0;
				for (StripBoardScene sb : sList) {
					if (scn.equals(sb.getSceneNumbers())) {
						scrollToRow(ix, true);
						sb.setSelected(true);
						setSelectedTab(unitIx-1); // tab #s are origin 0
						return;
					}
					ix++;
				}
			}
		}
		// Unscheduled list may not be sorted yet, but it must be sorted properly
		// so we get the right row index to match the requested scene number.
		Collections.sort(unScheduleList, getComparator());
		ix = 0;
		for (StripBoardScene sb : unScheduleList) {
			if (scn.equals(sb.getSceneNumbers())) {
				scrollToRow(ix, false);
				sb.setSelected(true);
				setSelectedTab(unitStripList.length-1); // tab #s are origin 0
				break;
			}
			ix++;
		}
	}

	/**
	 * Scroll to the end-of-day strip for shooting day # 'day'.
	 * @param dayNumStr The shooting day number EOD strip to be viewed.
	 */
	private void scrollToDay(String dayNumStr) {
		int dayNum = 0;
		try {
			dayNum = Integer.parseInt(dayNumStr);
			List<StripBoardScene> sList = unitStripList[unitIndex];
			if (sList != null) {
				int ix = 0;
				for (StripBoardScene sb : sList) {
					if (sb.getType() == StripType.END_OF_DAY) {
						if ( --dayNum <= 0) {
							scrollToRow(ix, true);
							sb.setSelected(true);
							break;
						}
					}
					ix++;
				}
			}
		}
		catch (NumberFormatException e) {
		}
		catch (Exception ex) {
			EventUtils.logError(ex);
		}
	}

	/**
	 * Scroll the stripboard view so that the specified row is within
	 * view.
	 * @param ix The zero-origin index of the row to be brought into view.
	 * @param scheduled True if the row is on one of the scheduled tabs, false if it
	 * is on the unscheduled tab.
	 */
	private void scrollToRow(int ix, boolean scheduled) {
		log.debug(ix);
		if (ix >= 0) {
			String width = ",18"; 	// Strip height in Thin mode
			if (getLayout().equals(SB_LAYOUT_THICK)) {
				width = ",33";		// Strip height in Thick mode
			}
			if (scheduled) {
				//log.debug("scroll to scheduled: "+ix);
				View.addJavascript(SCROLL_SCHEDULED_STRIP_JS +"(" + ix + width + ");");
			}
			else {
				//log.debug("scroll to unscheduled: "+ix);
				View.addJavascript(SCROLL_UNSCHEDULED_STRIP_JS + "(" + ix + width + ");");
			}
		}
	}

	private void setupTabs() {
		if (currentProject.getHasUnits()) {
			int uNum = unit.getNumber();
			if (selectedTab != 0 && selectedTab < currentProject.getUnits().size()) {
				unit = currentProject.getUnits().get(selectedTab);
			}
			else {
				unit = currentProject.getMainUnit();
			}
			if (uNum != unit.getNumber()) { // unit changed
				unit = UnitDAO.getInstance().refresh(unit);
				selectUnit(unit);
			}
			enableEdit = selectedTab < currentProject.getUnits().size();
		}
	}

	/**
	 * Action method for the "Make default" button on the Stripboard View page.
	 * Sets the project's default Stripboard to the currently displayed one.
	 *
	 * @return empty string (for navigation)
	 */
	public String actionMakeDefault() {
		log.debug("");
		try {
			ProjectDAO.getInstance().setStripboard(SessionUtils.getCurrentProject(), stripboard);
			HeaderViewBean.getInstance().setProject(SessionUtils.getCurrentProject()); // refresh
//			ListView.addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the Edit button on each line of the stripboard list.
	 * For mobile devices, displays a dialog box indicating that the editor is
	 * not available; otherwise, it returns the navigation string for the edit
	 * page.
	 *
	 * @return The appropriate Faces navigation string, either empty for mobile
	 *         devices, or the Stripboard Edit navigation value.
	 */
	public String actionEdit() {
		return StripBoardListBean.actionEditCheck();
	}

	/**
	 *  Sorts the list of unscheduled Strips (actually StripBoardSceneBeans).
	 */
	@Override
	protected void sort() {
		log.debug("sort column=" + sortColumnName);
		try {
			Collections.sort(unScheduleList, getComparator());
			Collections.sort(omittedList, getComparator());
//			getUnscheduledTable().getSavedChildren().clear(); // dropped panelSeries, not needed.
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** See {@link #selectedTab}. */
	public int getSelectedTab() {
		return selectedTab;
	}
	/** See {@link #selectedTab}. */
	public void setSelectedTab(int n) {
		log.debug(n);
//		if (selectedTab != n) {
//			ListView.addClientResize();
//		}
		selectedTab = n;
		SessionUtils.put(ATTR_STRIPBOARD_VIEW_MINI_TAB, selectedTab);
		setupTabs();
	}

	/**
	 * @return The stripboardList used in the drop-down box.
	 */
	/** See {@link #stripboardList}. */
	public List<SelectItem> getStripboardList() {
		return stripboardList;
	}
	/** See {@link #enableEdit}. */
	public boolean isEnableEdit() {
		return enableEdit;
	}
	/** See {@link #enableEdit}. */
	public void setEnableEdit(boolean enableEdit) {
		this.enableEdit = enableEdit;
	}

	/** See {@link #stripboardList}. */
	public void setStripboardList(List<SelectItem> stripboardList) {
		this.stripboardList = stripboardList;
	}

	public int getUnitCount() {
		return currentProject.getUnits().size();
	}

	/** See {@link #omittedList}. */
	public List<StripBoardScene> getOmittedList() {
		sortIfNeeded();
		return omittedList;
	}
	/** See {@link #omittedList}. */
	public void setOmittedList(List<StripBoardScene> omittedList) {
		this.omittedList = omittedList;
	}

//	/** See {@link #unscheduledTable}. */
//	public HtmlDataTable getUnscheduledTable() {
//		return unscheduledTable;
//	}
//	/** See {@link #unscheduledTable}. */
//	public void setUnscheduledTable(HtmlDataTable series) {
//		this.unscheduledTable = series;
//	}

	public String getFirstTabName() {
		if (getUnitCount() > 1) {
			return currentProject.getMainUnit().getName();
		}
		return "Scheduled";
	}

}
