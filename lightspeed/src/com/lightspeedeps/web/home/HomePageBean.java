/*
 * HomePageBean.java - Backing bean for myHome.jsp
 */
package com.lightspeedeps.web.home;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.batch.DueOverdueCheck;
import com.lightspeedeps.dao.CallsheetDAO;
import com.lightspeedeps.dao.FilmStockDAO;
import com.lightspeedeps.dao.RealWorldElementDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.CallSheetUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for Home page (within a production). This is an abstract class,
 * and is instantiated as part of the MessageCenterBean class. Originally they
 * were separate classes, but making this a superclass eliminated a lot of
 * redundant code.
 * <p>
 * They could have been combined into one class, but the separation of classes
 * makes it easier to see which code applies to what: this class covers the left
 * side of the Home page and the default "Status" mini-tab. The
 * MessageCenterBean code covers the two "Notifications" mini-tabs.
 */
@ManagedBean
@ViewScoped
public abstract class HomePageBean  extends ListView implements Serializable {
	/** */
	private static final long serialVersionUID = 204759267237972756L;

	private static final Log log = LogFactory.getLog(HomePageBean.class);

	//protected static final int HOME_TAB = 0;
	protected static final int MY_MSGS_TAB = 1;
	protected static final int ALL_MSGS_TAB = 2;
	protected static final int PREFERENCES_TAB = 3;

	/** Maximum number of notifications/messages to display on the Status tab */
	private final static int MAX_MESSAGES_IN_HOME_STATUS = 4;

	private Integer selectedProjectId;
	private Integer totalMissingElements;
	private Integer totalMissingLocations;

	/** An array of the current user's call times (whether cast or crew)
	 * from the "current" Callsheet of each unit the current user has a role in,
	 * indexed by unit number.  Entries will be null if no call time for the
	 * current user is found in the current Callsheet or there is no current
	 * callsheet for this unit. */
	private Date[] yourCallTime;

	/** An array of "Crew call" times from the "current" Callsheet of
	 * each unit the current user has a role in, indexed by unit number. */
	private Date[] crewCallTime;

	/** An array of flags or messages, indexed by unit number. If the current
	 * user does NOT have a role in the corresponding unit, the value will be
	 * "N". If the user DOES have a role in the unit, the value is either "Y",
	 * or the The message to be displayed in place of the user's call time, in
	 * cases where (a) there is no "current" Callsheet for a given unit, or (b)
	 * the current user is not listed in the current Callsheet. (The
	 * corresponding entry in the {@link #yourCallTime} array will be null in
	 * these cases.) */
	private String[] callTimeMsg;

	/** Shooting end dates, indexed by unit. */
	private Date[] lastShootDate;

	/** Shooting status messages, indexed by unit. */
	private String[] shootMessage;

	// today's shooting location
	private RealWorldElement[] location;

	private transient Project project = null;
	private Dpr dpReport = null;
	protected Integer contactId;

	private transient AuthorizationBean authBean;

	private List<FilmStock> filmStockList = null;

	/** list of up to 4 most recent messages */
	private List<MessageInstance> notificationList = null;
	/** number of entries in notificationList */
	private int notificationCount = 0;

	private List<Dpr> dprCurrentList = null;
	private List<Date> dprRecentFour = null;
	private List<Dpr> dprReportDueTodayList = null;
	private List<Dpr> dprReportOverDueList = null;
	private List<ExhibitG> exgReportDueTodayList = null;
	private List<ExhibitG> exgReportOverDueList = null;

	private List<ReportStatusItem> reportStatusList;
	private List<ElementStatusItem> elementStatusList;

	private List<Double> castOverTimeList = null;
	private List<Double> crewOverTimeList = null;
	private List<Date> timeSheetDates = null;

	public HomePageBean(String sort, String prefix) {
		super(sort, prefix);
		log.debug("");
		pageLoad();
	}

	public static HomePageBean getInstance() {
		return (HomePageBean)ServiceFinder.findBean("messageCenterBean");
	}

	private void pageLoad() {
		filmStockList = new ArrayList<>();
		dprCurrentList = new ArrayList<>();
		dprRecentFour = new ArrayList<>();
		dprReportDueTodayList = new ArrayList<>();
		dprReportOverDueList = new ArrayList<>();
		exgReportDueTodayList = new ArrayList<>();
		exgReportOverDueList = new ArrayList<>();
		castOverTimeList = new ArrayList<>();
		crewOverTimeList = new ArrayList<>();
		timeSheetDates = new ArrayList<>();

		try {
			Contact contact = SessionUtils.getCurrentContact();
			if (contact == null) {
				log.warn("Null contact in HomePageBean.pageLoad(); usually due to session expiration.");
				//EventUtils.logError("Null contact in HomePageBean.pageLoad(); initialization terminated.");
				return;
			}
			contactId = contact.getId();
			setSelectedProjectId(getProject().getId());
			log.debug("project id = " + getProject().getId());

			Calendar today = Calendar.getInstance();
			CalendarUtils.setStartOfDay(today);

			int unitCount = getProject().getHighestUnitNumber() + 1;
			lastShootDate = new Date[unitCount];
			yourCallTime = new Date[unitCount];
			crewCallTime = new Date[unitCount];
			location = new RealWorldElement[unitCount];
			shootMessage = new String[unitCount];
			callTimeMsg = new String[unitCount];

			CallsheetDAO callsheetDAO = CallsheetDAO.getInstance();
			UserCallInfo userCallInfo = CallSheetUtils.getUserCallInfo();
			for (Unit unit : getProject().getUnits()) {
				int uNum = unit.getNumber();
				ScheduleUtils scheduleUtils = new ScheduleUtils(unit);
				shootMessage[uNum] = scheduleUtils.getShootDayMsg();
				lastShootDate[uNum] = scheduleUtils.getEndDate();
				UserCallUnit userCallUnit = userCallInfo.getCallUnit(unit);
				if (userCallUnit == null) {
					callTimeMsg[uNum] = "N"; // user is not a member of this Unit
				}
				else {
					callTimeMsg[uNum] = "Y";
					crewCallTime[uNum] = userCallUnit.getCrewCall();
					Date calltime = userCallUnit.getUserCall();
					yourCallTime[uNum] = calltime;
					Callsheet callsheet = null;
					if (userCallUnit.getCallsheetId() == null) {
						if (scheduleUtils.getEndDate().before(today.getTime())) {
							callTimeMsg[uNum] = MsgUtils.formatMessage("Home.CalltimeDone");
						}
						else {
							callTimeMsg[uNum] = MsgUtils.formatMessage("Home.CalltimeTBD");
						}
					}
					else {
						if (calltime == null) {
							callTimeMsg[uNum] = MsgUtils.formatMessage("Home.CalltimeNotListed");
						}
						callsheet = callsheetDAO.findById(userCallUnit.getCallsheetId());
					}
					location[uNum] = findLocation(callsheet, scheduleUtils.getCurrentShootDayNumber());
				}
			}



			Set<Dpr> dpReportSet = null;
			dpReportSet = getProject().getDprs();

			/*
			 * Helper Methods
			 */
			createNotificationList();
			createWorkStatus(dpReportSet);
			createElementStatus();
			createReportStatus();
			createMaterialsList();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private void createMaterialsList() {
		final FilmStockDAO filmStockDAO = FilmStockDAO.getInstance();
		List<FilmStock> films = filmStockDAO.findCurrentMaterials();
		filmStockList = new ArrayList<>();
		log.debug("file stock list size=" + films.size());
		// prevent lazyInitializationErrors (when switching to "Status" tab)
		for (FilmStock fs : films) {
			fs = fs.clone();
			filmStockList.add(fs);
			fs.getMaterial().getType();
			fs.getUsedTotal().getGross();
			// prevent later Hibernate error due to clearing material.production reference
			filmStockDAO.evict(fs.getMaterial());
			fs.getMaterial().setProduction(null);
		}
	}

	private void createElementStatus() {
		elementStatusList = null;
		if (getProject().getScript() != null) {
			try {
				elementStatusList = new ArrayList<>();
				ElementStatusItem bean;
				for (int i = 0; i < ScriptElementType.values().length; i++ ) {
					elementStatusList.add(null);
				}
				for (ScriptElementType type : ScriptElementType.values()) {
					bean = new ElementStatusItem(type);
					elementStatusList.set(type.ordinal(), bean);
				}
				ArrayList<Integer> checkId = new ArrayList<>();
				for (Scene scene : getProject().getScript().getScenes()) {
					for (ScriptElement scriptElement : scene.getScriptElements()) {
						addElement(scriptElement, checkId);
					}
					if (scene.getScriptElement() != null) { // the Set/Location
						addElement(scene.getScriptElement(), checkId);
					}
				}
				for (int i = 0; i < ScriptElementType.values().length; i++ ) {
					if (elementStatusList.get(i).getType() == ScriptElementType.N_A) {
						elementStatusList.remove(i);
						break;
					}
				}
				Collections.sort(elementStatusList);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
	}

	private void addElement(ScriptElement scriptElement, ArrayList<Integer> checkId) {
		//if ( scriptElement.getContact() != null
		//		&& scriptElement.getContact().getId().equals(contactId)) {
		// 9/2/2010 - don't filter based on responsible party -- count all required SEs.
			if ( ! checkId.contains(scriptElement.getId())) {
				checkId.add(scriptElement.getId());
				if (scriptElement.getRealElementRequired()) {
					int ix = scriptElement.getType().ordinal();
					ElementStatusItem bean = elementStatusList.get(ix);
					bean.setRequired(bean.getRequired()+1);
					if (scriptElement.getRequirementSatisfied()) {
						bean.setSelected(bean.getSelected()+1);
					}
				}
			}
		//}
	}

	/**
	 * Find the RealWorldElement for the shooting location in a callsheet.
	 *
	 * @param callsheet The callsheet used as the source of the shooting
	 *            location.
	 * @param shootDay The shooting day number.
	 * @return The RealWorldElement value for the first Location listed in the
	 *         given call sheet. If an element is not found, a dummy element is
	 *         created with its name set to an appropriate message, such as TBD.
	 */
	private RealWorldElement findLocation(Callsheet callsheet, int shootDay) {
		RealWorldElement location = null;
		if (callsheet != null) {
			List<SceneCall> scl = callsheet.getSceneCalls();
			if (scl.size() > 0) {
				SceneCall sc = scl.get(0);
				log.debug(sc);
				location = RealWorldElementDAO.getLocationElement(sc);
				if (location != null) {
					log.debug("found loc in callsheet: " + location.getName());
				}
			}
		}
		if (location == null) {
			location = new RealWorldElement();
			if (shootDay != 0) { // Still within production period
				location.setName(MsgUtils.formatMessage("Home.LocationTBD"));
			}
			else {
				location.setName(MsgUtils.formatMessage("Home.LocationNA"));
			}
		}
		if (location.getAddress() == null) {
			location.setAddress(new Address()); // makes JSP simpler
		}
		return location;
	}

	/**
	 * Populate the (short) list of notifications for the Status tab of the
	 * Home page.
	 */
	private void createNotificationList() {
		notificationList = new ArrayList<>();
		try {
			Contact contact = SessionUtils.getCurrentContact();
			if (contact != null) {
				List<MessageInstance> miList = contact.getMessageInstances();
				if (miList != null) {
					int count = 0;
					for (MessageInstance mi : miList) {
						if (mi.getSentMethod() == NotificationMethod.WEB) {
							mi = mi.clone();
							notificationList.add(mi);
							mi.getMessage().setNotification(null);
							if (++count == MAX_MESSAGES_IN_HOME_STATUS) {
								break;
							}
						}
					}
				}
			}
			setNotificationCount(notificationList.size());
		}
		catch (Exception e) {
			log.error(e);
		}
	}

	private void createReportStatus() {
		reportStatusList = new ArrayList<>();

		Calendar yesterdayCal  = Calendar.getInstance();
		CalendarUtils.setStartOfDay(yesterdayCal);
		yesterdayCal.add(Calendar.DATE, - 1);

		List<ReportRequirement> matchedReqs = new ArrayList<>();
		for (ReportRequirement reportRequirement : getProject().getReportRequirements()) {
			if ((reportRequirement.getRole() != null &&
						getAuthBean().hasRole(reportRequirement.getRole())) ||
					(reportRequirement.getContact() != null &&
						reportRequirement.getContact().getId().equals(contactId))) {
				log.debug("user matched report: " + reportRequirement);
				matchedReqs.add(reportRequirement);
			}
		}

		log.debug("user matched req's=" + matchedReqs.size());
		if (matchedReqs.size() == 0) {
			return;	// done!
		}

		// Find the earliest date any report might be required...
		ScheduleUtils dateHelper = new ScheduleUtils(getProject().getMainUnit());
		Date firstDate = dateHelper.getStartDate();
		Calendar cutOffCal = Calendar.getInstance();
		cutOffCal.add(Calendar.DATE, -7);	// limit of 1 week ago
		if (firstDate.before(cutOffCal.getTime())) {
			firstDate = cutOffCal.getTime();	// no point in going back more than that
		}

		final DueOverdueCheck check = (DueOverdueCheck)ServiceFinder.findBean("dueOverdueCheck");
		reportStatusList = check.findReportDueStatus(project, firstDate, matchedReqs);
		Collections.sort(reportStatusList);
	}

	private void createWorkStatus(Set<Dpr> dpReportSet) {
		// Find the most recent Approved DPR, plus the dates of up to
		// four of the most recent Approved DPRs.
		dprRecentFour.clear();
		dprCurrentList.clear();
		if (dpReportSet != null && dpReportSet.size() > 0) {
			try {
				Map<Date, Dpr> dprMap = new HashMap<>();
				Date[] dprDates = new Date[dpReportSet.size()];
				int dateCnt = 0;
				for (Dpr dpr : dpReportSet) {
					if (dpr.getStatus() == ReportStatus.APPROVED) {
						Date dprDate = dpr.getDate();
						dprMap.put(dprDate, dpr);
						dprDates[dateCnt++] = dprDate;
					}
				}
				if (dateCnt > 0) {
					Arrays.sort(dprDates, 0, dateCnt); // sort in Date order (oldest first)
					dprCurrentList.add(dprMap.get(dprDates[dateCnt - 1])); // save the most recent
					int j = Math.max(dateCnt-4,0);
					for (; j < dateCnt; j++) { // get up to 4 most recent Date entries
						dprRecentFour.add(dprDates[j]); // for page display
					}
				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
	}

	/**
	 * Method called when user selects a 'mini-tab'. Don't let user switch away
	 * from the Preferences mini-tab if it is in Edit mode, as that may lead to
	 * database issues, particularly if they were to delete one of their
	 * messages. (See bugzilla #764/rev 2691.)
	 *
	 * @see com.lightspeedeps.web.view.View#setSelectedTab(int)
	 */
	@Override
	public void setSelectedTab(int n) {
		log.debug(n);
		try {
			if (getSelectedTab() == PREFERENCES_TAB && n != PREFERENCES_TAB) {
				PreferencesBean pref = PreferencesBean.getInstance();
				if (pref.isEditMode()) {
					MsgUtils.addFacesMessage("Home.Preferences.SaveCancel", FacesMessage.SEVERITY_ERROR);
					return;
				}
			}
		}
		catch (Exception e) {
			log.error(e);
		}
		super.setSelectedTab(n);
	}

	/**
	 * Called from headerViewBean when the user has switched projects.  If we're already
	 * the current page, we need to refresh our data being displayed.
	 */
	public void projectChanged() {
		pageLoad();
//		addClientResize();
	}

	/** See {@link #authBean}. */
	private AuthorizationBean getAuthBean() {
		if (authBean == null) {
			authBean = AuthorizationBean.getInstance();
		}
		return authBean;
	}

	public Project getProject() {
		if (project == null) {
			project = SessionUtils.getCurrentProject();
		}
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #yourCallTime}. */
	public Date[] getYourCallTime() {
		return yourCallTime;
	}

	/** See {@link #crewCallTime}. */
	public Date[] getCrewCallTime() {
		return crewCallTime;
	}

	/** See {@link #callTimeMsg}. */
	public String[] getCallTimeMsg() {
		return callTimeMsg;
	}

	/** See {@link #location}. */
	public RealWorldElement[] getLocation() {
		return location;
	}

	public Integer getSelectedProjectId() {
		return selectedProjectId;
	}
	public void setSelectedProjectId(Integer selectedProjectId) {
		this.selectedProjectId = selectedProjectId;
	}

	/** See {@link #notificationList}. */
	public List<MessageInstance> getNotificationList() {
		if (notificationList == null) {
			createNotificationList();	// refresh required
		}
		return notificationList;
	}
	/** See {@link #notificationList}. */
	public void setNotificationList(List<MessageInstance> notificationList) {
		this.notificationList = notificationList;
	}

	/** See {@link #notificationCount}. */
	public int getNotificationCount() {
		return notificationCount;
	}
	/** See {@link #notificationCount}. */
	public void setNotificationCount(int notificationCount) {
		this.notificationCount = notificationCount;
	}

	public List<Dpr> getDprCurrentList() {
		return dprCurrentList;
	}
	public void setDprCurrentList(List<Dpr> dprCurrentList) {
		this.dprCurrentList = dprCurrentList;
	}

	public List<Date> getDprRecentFour() {
		return dprRecentFour;
	}
	public void setDprRecentFour(List<Date> dprrecentFour) {
		dprRecentFour = dprrecentFour;
	}

	public Dpr getDpReport() {
		return dpReport;
	}
	public void setDpReport(Dpr dpReport) {
		this.dpReport = dpReport;
	}

	public List<Dpr> getDprReportDueTodayList() {
		return dprReportDueTodayList;
	}
	public void setDprReportDueTodayList(List<Dpr> dprReportDueTodayList) {
		this.dprReportDueTodayList = dprReportDueTodayList;
	}

	public List<Dpr> getDprReportOverDueList() {
		return dprReportOverDueList;
	}
	public void setDprReportOverDueList(List<Dpr> dprReportOverDueList) {
		this.dprReportOverDueList = dprReportOverDueList;
	}

	public List<FilmStock> getFilmStockList() {
		return filmStockList;
	}
	public void setFilmStockList(List<FilmStock> filmStockList) {
		this.filmStockList = filmStockList;
	}

	public List<ExhibitG> getExgReportOverDueList() {
		return exgReportOverDueList;
	}
	public void setExgReportOverDueList(List<ExhibitG> exgReportOverDueList) {
		this.exgReportOverDueList = exgReportOverDueList;
	}

	public List<ExhibitG> getExgReportDueTodayList() {
		return exgReportDueTodayList;
	}
	public void setExgReportDueTodayList(List<ExhibitG> exgReportDueTodayList) {
		this.exgReportDueTodayList = exgReportDueTodayList;
	}

	/** See {@link #reportStatusList}. */
	public List<ReportStatusItem> getReportStatusList() {
		return reportStatusList;
	}
	/** See {@link #reportStatusList}. */
	public void setReportStatusList(List<ReportStatusItem> reportStatusList) {
		this.reportStatusList = reportStatusList;
	}

	/** See {@link #elementStatusList}. */
	public List<ElementStatusItem> getElementStatusList() {
		return elementStatusList;
	}

	/** See {@link #elementStatusList}. */
	public void setElementStatusList(List<ElementStatusItem> elementStatusList) {
		this.elementStatusList = elementStatusList;
	}

	public Integer getTotalMissingElements() {
		return totalMissingElements;
	}
	public void setTotalMissingElements(Integer totalMissingElements) {
		this.totalMissingElements = totalMissingElements;
	}

	public Integer getTotalMissingLocations() {
		return totalMissingLocations;
	}
	public void setTotalMissingLocations(Integer totalMissingLocations) {
		this.totalMissingLocations = totalMissingLocations;
	}

	/** See {@link #castOverTimeList}. */
	public List<Double> getCastOverTimeList() {
		return castOverTimeList;
	}
	/** See {@link #castOverTimeList}. */
	public void setCastOverTimeList(List<Double> castOverTimeList) {
		this.castOverTimeList = castOverTimeList;
	}

	public List<Double> getCrewOverTimeList() {
		return crewOverTimeList;
	}
	public void setCrewOverTimeList(List<Double> overTimeList) {
		crewOverTimeList = overTimeList;
	}

	public List<Date> getTimeSheetDates() {
		return timeSheetDates;
	}
	public void setTimeSheetDates(List<Date> timeSheetDates) {
		this.timeSheetDates = timeSheetDates;
	}

	/** See {@link #lastShootDate}. */
	public Date[] getLastShootDate() {
		return lastShootDate;
	}
	/** See {@link #lastShootDate}. */
	public void setLastShootDate(Date[] lastShootDate) {
		this.lastShootDate = lastShootDate;
	}

	/** See {@link #shootMessage}. */
	public String[] getShootMessage() {
		return shootMessage;
	}
	/** See {@link #shootMessage}. */
	public void setShootMessage(String[] shootMessage) {
		this.shootMessage = shootMessage;
	}

}
