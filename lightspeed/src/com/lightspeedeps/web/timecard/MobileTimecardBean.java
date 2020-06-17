/**
 * File: MobileTimecardBean.java
 */
package com.lightspeedeps.web.timecard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.approver.MobileApproverBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This bean backs many of the pages used in the mobile device
 * timecard data entry process.  It allows the user to easily
 * switch from one production to another, one week/ending date
 * to another, and so forth, and to enter their daily hours and
 * submit a finished timecard.
 * <p>
 * Since it supports multiple pages presenting different lists
 * of choices, many of the lists are lazy-initialized, so the
 * overhead of creating the list is only incurred if the page
 * being displayed requires that list.
 */
@ManagedBean
@ViewScoped
public class MobileTimecardBean extends TimecardEmpSelectBase implements Serializable {
	/** */
	private static final long serialVersionUID = - 680675544993754686L;

	private static final Log LOG = LogFactory.getLog(MobileTimecardBean.class);

	private static final String WEEKLY_PAYROLL_PAGE = "pickdaym";
	/** The current (logged in) User. */
	private User user;

	/** The Production currently selected by the user. The list of
	 * projects and/or timecards available will depend on this value. */
	private Production production;

	/** The database id of the Production selected by the user on the
	 * My Productions page.  Set via the JSP. */
	private Integer productionId;

	/** The database id of the Project selected by the user on the
	 * My Projects page.  Set via the JSP. */
	private Integer projectId;

	/** The List of projects that the current User has access to in the current
	 * Production. The Item.id value will be the Project.id value. */
	private transient List<Item> projects;

	/** The StartForm corresponding to the currently selected position
	 * (within the current production). */
	private StartForm startForm;

	/** The currently selected position (occupation) for which timecards
	 * may be selected. */
	private String position;

	/** The date whose DAILY timecard data is being displayed, if any. */
	private Date currentDate;

	/** The day-of-week number for today.  Used to highlight the current
	 * date in some UI situations. */
	private int todaysWeekDayNum;

	/** The day number of the current Production/Project's week-starting day,
	 * 1=Sunday, 7=Saturday. (Usual is 1, Sunday.) */
	private Integer weekStartDay;

	/** The week-ending date corresponding to the current (today's) week. */
	private Date todaysWeekEndDate;

	/** User-entered PIN for confirming a timecard Submit. */
	private String pin;

	/** User-entered password for confirming a timecard Submit. */
	private String password;

	/** The reason/type of submit chosen by the user. */
	private TimecardSubmitType submitType;

	/** The comment to add when a user submits a timecard for another
	 * person, with a submit type of "Other". */
	private String submitComment;

	/** True iff we are showing the timecard daily-selection section of the payroll page. */
	private boolean showDays;

	/** True iff we are showing the controls used to add a comment to a timecard.
	 * This flag is shared by multiple comment fields (on different pages). */
	private boolean showComment;

	/** True iff we are showing the Box Rental section of the payroll page. */
	private boolean showBoxRental;

	/** True iff we are showing the Mileage section of the payroll page. */
	private boolean showExpenses;

	/** True iff we are showing the Mileage section of the payroll page. */
	private boolean showMileage;

	/** True iff we are showing the Mileage section of the payroll page. */
	private boolean showStartInfo;

	/** True iff the SSN field should be displayed as editable. */
	private boolean showEditSsn;

	/** True iff the 'additional fields' section of the Daily Time page
	 * should be displayed. */
	private boolean showMoreDaily;

	/** True iff one or more Start Info fields are missing. */
	private boolean startInfoRequired;

	/** True iff one or more Start Info fields have changed between the saved copy
	 * of the timecard and the user input fields. */
	private boolean startInfoChanged;

	/** Field to save the timecard's SSN value, for comparison to see if the
	 * user has changed it. */
	private String saveSSN;

	/** Field to save the timecard's cityWorked value, for comparison to see if the
	 * user has changed it. */
	private String saveWorkCity;

	/** Field to save the timecard's stateWorked value, for comparison to see if the
	 * user has changed it. */
	private String saveWorkState;

	/** Field to save the timecard's paidAs value for comparison to see if the user
	 * has changed it. */
	private PaidAsType savePaidAs;

	/** Field to save the timecard's Federal Tax Id value for comparison to see if the user
	 * has changed it. */
	private String saveFederalCorpId;

	/** True iff we are in "edit mode" on the mileage fields on the
	 * Weekly Timecard page (which shows the list of days in a week). */
	private boolean editMileage;

	/** The MileageLine entry being displayed or edited on the weekly Mileage page. */
	private MileageLine mileageLine;

	/** The expense line item being displayed or edited on the Expense page. */
	private PayExpense payExpense;

	// Flags to control enabling/disabling the "next" and "previous" buttons.

	/** True iff the "next" button should be enabled for the list of days
	 * within a timecard. */
	private Boolean hasNextDay;
	/** True iff the "previous" button should be enabled for the list of days
	 * within a timecard. */
	private Boolean hasPrevDay;

	/** True iff the "next" button should be enabled for the list of week-ending
	 * dates (i.e., timecards).  */
	private Boolean hasNextWeek;
	/** True iff the "previous" button should be enabled for the list of week-ending
	 * dates (i.e., timecards).  */
	private Boolean hasPrevWeek;

	/** Flag used to control display of "Enter today's time" and "Current Timecard"
	 * buttons. True if user has a timecard for this week in any production. */
	private Boolean hasTimecard;

	/** A Calendar used in several routines, initialized to the application's timezone. */
	private Calendar tzCalendar;

	/** The default (and only) constructor. */
	public MobileTimecardBean() {
		loadValues();
		if (weeklyTimecard != null) {
			if (weeklyTimecard.getLockedBy() != null && getvUser().getId().equals(weeklyTimecard.getLockedBy())) {
				LOG.debug("lock found on in <init>, unlocking");
				getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
			}
		}
		showDays = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_TC, true);
		showBoxRental = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_BOX, false);
		showMileage = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_MILES, false);
		showStartInfo = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_START_INFO, false);
		showExpenses = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_EXPENSES, false);
		showMoreDaily = SessionUtils.getBoolean(Constants.ATTR_MTC_SHOW_MORE_DAILY, false);
	}

	/**
	 * Initialization code that needs to run BEFORE the superclass'
	 * initialization. So we override that init() method and call it after we've
	 * set up the required variables. This is because the superclass init() can
	 * invoke our {@link #createMobileWeeklyTimecardList(boolean)}.
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardEmpSelectBase#init()
	 */
	@Override
	protected void init() {
		user = SessionUtils.getCurrentUser();
		if (production == null) {
			production = getViewProduction();
		}
		tzCalendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		todaysWeekDayNum = tzCalendar.get(Calendar.DAY_OF_WEEK);

		todaysWeekEndDate = TimecardUtils.calculateLastDayOfCurrentWeek(getWeekEndDay());

		super.init();
	}

	/**
	 * Action method for the "enter today's time" button. This attempts to find
	 * the current day's entry, as related to the last-used production and
	 * position information.
	 *
	 * @return The appropriate JSF navigation string: an empty string if we
	 *         cannot find an appropriate timecard to edit, or the string to
	 *         navigate to the "enter hours" page if we do find a timecard.
	 */
	public String actionEnterToday() {
		String target = null;
		try {
			target = findCurrentWeek();
			if (target != null) {
				tzCalendar.setTime(new Date());
				CalendarUtils.setStartOfDay(tzCalendar);
				currentDate = tzCalendar.getTime();
				target = "hoursm"; // jump to "enter daily hours" page
				// Set the navigation string for the "Back" button on the hours page:
				SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
				if (getViewProject() != null) { // Commercial/AICP production
					// from Timecard List, "Back" should go to Projects page
					SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYPROJECTS_PAGE);
				}
				else { // from Timecard List, "Back" should go to productions list
					SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYTIMECARDS_PAGE);
				}
			}
			else {
				MsgUtils.addFacesMessage("Mobile.Timecard.NoToday", FacesMessage.SEVERITY_ERROR);
			}
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return target;
	}

	/**
	 * Action method for the "Current week" button. This attempts to determine
	 * the user's current production, and find a timecard for the current week,
	 * and display that on the Weekly Payroll page.
	 *
	 * @return Navigation string either for the Weekly Payroll page, or null
	 *         (staying on the current page) if the current week's timecard
	 *         could not be determined.
	 */
	public String actionCurrentWeek() {
		String target = null;
		try {
			target = findCurrentWeek();
			if (target != null) {
				if (getViewProject() != null) { // Commercial/AICP production
					// from Timecard List, "Back" should go to Projects page
					SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYPROJECTS_PAGE);
				}
				else { // from Timecard List, "Back" should go to productions list
					SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYTIMECARDS_PAGE);
				}
			}
			else {
				MsgUtils.addFacesMessage("Mobile.Timecard.NoCurrentWeek", FacesMessage.SEVERITY_ERROR);
			}
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return target;
	}

	/**
	 * Find the current (logged in) user's timecard for the current Production
	 * (if set) or the user's preferred Production (if no current Production is
	 * set), for the current week. Sets the production and weeklyTimecard fields
	 * if successful.
	 *
	 * @return Null if not found, otherwise the navigation string to jump to the
	 *         Weekly Payroll page.
	 */
	private String findCurrentWeek() {
		Integer id;
		weeklyTimecardList = null;
		tcUser = SessionUtils.getCurrentUser();
		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, tcUser.getId());
		SessionUtils.put(Constants.ATTR_CONTACT_ID, null); // not used in mobile; clear for safety
		currentDate = null;
		if (production == null) {
			// see if user has timecards for this week for just one production -- if so, use that.
			//Date weDate = TimecardUtils.calculateLastDayOfCurrentWeek();
			List<WeeklyTimecard> timecards = getWeeklyTimecardDAO().findByWeekEndDateAccount(
					todaysWeekEndDate, getvUser().getAccountNumber());
			if (timecards.size() == 1) {
				production = ProductionDAO.getInstance().findByProdId(timecards.get(0).getProdId());
			}
			if (production == null) {
				id = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID);
				if (id != null) {
					production = ProductionDAO.getInstance().findById(id);
				}
			}
			if (production == null) {
				// see if user belongs to just one production -- if so, use that.
				List<Production> prods = ProductionDAO.getInstance().findByUser(SessionUtils.getCurrentUser());
				if (prods.size() == 1) {
					production = prods.get(0);
				}
			}
			if (production != null) {
				if (enterProduction(production.getId())) { // try & log user in
					// set attribute so PageAuthenticatePhaseListener lets us in!
					SessionUtils.put(Constants.ATTR_ENTERING_PROD, 1);
				}
				else {
					production = null;
				}
			}
		}
		if (production != null) {
			if (startForm == null) {
				position = null;
				id = UserPrefBean.getInstance().getInteger(Constants.ATTR_MTC_START_FORM_ID);
				if (id != null) {
					startForm = StartFormDAO.getInstance().findById(id);
				}
			}
			if (startForm != null) {
				if (startForm.getContact().getUser().equals(getvUser()) &&
						startForm.getContact().getProduction().equals(production)) {
					position = startForm.getJobClass();
				}
				else {
					startForm = null;
				}
			}
		}
		String target = null; // JSF navigation string - default is stay on the same page
		WeeklyTimecard thisWeek = null;
		if (production != null) {
			thisWeek = findCurrentWeekTimecard();
			if (thisWeek != null) {
				weeklyTimecard = thisWeek;
				if (startForm == null) {
					position = weeklyTimecard.getOccupation();
					startForm = weeklyTimecard.getStartForm();
					UserPrefBean.getInstance().put(Constants.ATTR_MTC_START_FORM_ID,
							startForm == null ? null : startForm.getId());
				}
				loadTimecard();
				target = WEEKLY_PAYROLL_PAGE; // jump to "weekly timecard" page
			}
			if (startForm != null && startForm.getProject() != null) { // AICP production
				setViewProject(startForm.getProject());
				SessionUtils.setCurrentProject(getViewProject());
				SessionUtils.put(Constants.ATTR_LAST_PROJECT_ID, getViewProject().getId());
			}
		}
		return target;
	}

	/**
	 * Called to place the user into a Production.
	 *
	 * @param id The database id of the Production to be entered.
	 * @return True if the user has been placed into the Production; false if
	 *         not, which can occur if the user does not have access rights to
	 *         the production.
	 */
	private boolean enterProduction(Integer id) {
		startForm = null;
		position = null;
		weeklyTimecardList = null;
		currentDate = null;
		production = TimecardUtils.enterProductionForTimecards(id);
		selectProductionId(id);	// set production as the "Viewed" production, similar to My Timecards
		return (production != null);
	}

	/**
	 * Action method called when the user clicks on a project button in the
	 * My Projects page. The project.id has been set by a
	 * f:setPropertyActionListener in the JSP.
	 *
	 * @return Navigation string for the Timecard List page.
	 */
	public String actionSelectProject() {
		String ret = "timecardlistm";
		// Set the navigation string for the "Back" button on the hours page:
		SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYPROJECTS_PAGE);
		return ret;
	}

	/**
	 * Action method for the "Next" button related to the list of week-ending
	 * dates available for the current user. If successful, the {@link #weeklyTimecard} and
	 * {@link #currentDate} fields are updated appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionNextWeekEndDate() {
		try {
			hasNextWeek = null;
			hasPrevWeek = null;
			editMileage = false;
			int i = findWeekEndEntry();
			if (i > 0) {
				i--;
				weeklyTimecard = getWeeklyTimecardList().get(i);
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
				if (weeklyTimecard != null) {
					position = weeklyTimecard.getOccupation();
					loadTimecard();
					currentDate = weeklyTimecard.getEndDate();
					hasPrevWeek = true;
				}
			}
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Previous" button related to the list of week-ending
	 * dates available for the current user. If successful, the {@link #weeklyTimecard} and
	 * {@link #currentDate} fields are updated appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionPreviousWeekEndDate() {
		try {
			hasNextWeek = null;
			hasPrevWeek = null;
			editMileage = false;
			int i = findWeekEndEntry();
			if (i < getWeeklyTimecardList().size()-1) {
				i++;
				weeklyTimecard = getWeeklyTimecardList().get(i);
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
				if (weeklyTimecard != null) {
					position = weeklyTimecard.getOccupation();
					loadTimecard();
					currentDate = weeklyTimecard.getEndDate();
					hasNextWeek = true;
				}
			}
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Next" button related to the list of days within
	 * the current timecard. If successful, the {@link #currentDate} field is updated
	 * appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionNextDate() {
		addButtonClicked();
		try {
			DailyTime dt = getDailyTime();
			if (dt != null) {
				if (! TimecardCheck.validateAndCalcWorkDay(dt, true)) {
					// don't let user leave page with bad data
					return null;
				}
			}
			tzCalendar.setTime(currentDate);
			tzCalendar.add(Calendar.DAY_OF_MONTH, 1);
			currentDate = tzCalendar.getTime();
			calculateDayFlags();
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Previous" button related to the list of days within
	 * the current timecard. If successful, the {@link #currentDate} field is updated
	 * appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionPreviousDate() {
		addButtonClicked();
		try {
			DailyTime dt = getDailyTime();
			if (dt != null) {
				if (! TimecardCheck.validateAndCalcWorkDay(dt, true)) {
					// don't let user leave page with bad data
					return null;
				}
			}
			tzCalendar.setTime(currentDate);
			tzCalendar.add(Calendar.DAY_OF_MONTH, -1);
			currentDate = tzCalendar.getTime();
			calculateDayFlags();
			saveValues();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Enter" button on the timecard data-entry page.
	 * This (re)calculates the daily and weekly totals, and saves the current
	 * timecard. It also evicts and reloads the timecard, which has the effect
	 * of "normalizing" all the displayed numbers to two digits.
	 *
	 * @return navigation String to jump to the "Day List" page or null if there
	 *         was an error and we should remain on the same page.
	 */
	public String actionEnterTime() {
		addButtonClicked();
		String ret = null;
		try {
			DailyTime dt = getDailyTime();
			if (dt != null) {
				if (TimecardCheck.validateAndCalcWorkDay(dt, true)) {
					TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
					if (dt.getCallTime() != null || dt.getWrap() != null ||
							dt.getM1In() != null || dt.getM1Out() != null ||
							dt.getM2In() != null || dt.getM2Out() != null) {
						if (dt.getCallTime() == null) {
							MsgUtils.addFacesMessage("Timecard.Mobile.CallMissing", FacesMessage.SEVERITY_ERROR);
							return null;
						}
						if (dt.getWrap() == null) {
							MsgUtils.addFacesMessage("Timecard.Mobile.WrapMissing", FacesMessage.SEVERITY_ERROR);
							return null;
						}
						if ((dt.getM1In() == null && dt.getM1Out() != null) ||
								(dt.getM1In() != null && dt.getM1Out() == null) ||
								(dt.getM2In() == null && dt.getM2Out() != null) ||
								(dt.getM2In() != null && dt.getM2Out() == null)) {
							MsgUtils.addFacesMessage("Timecard.Mobile.MealTimeMissing", FacesMessage.SEVERITY_ERROR);
							return null;
						}
					}
					updateTimecard();
					getWeeklyTimecardDAO().evict(weeklyTimecard);
					weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
					ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Action method for the Create button on the list of timecards. The Create
	 * button is displayed when the displayed user's timecards for the current
	 * and/or next week do not exist, and the logged-in user has the permission
	 * to create timecards.
	 * <p>
	 * Upon entry, the session variables for week-end date and start form id
	 * should have been set via f:setPropertyActionListener tags in the JSP. The
	 * start form id will be the negative of its actual value, as this is how
	 * the JSP knows to generate a Create button.
	 *
	 * @return navigation string to "days" (Weekly Payroll) page if the create
	 *         is successful, otherwise a null navigation string.
	 */
	public String actionCreateTimecard() {
		String next = null;
		try {
			Integer id = SessionUtils.getInteger(Constants.ATTR_START_FORM_ID);
			if (id != null && id < 0) {
				id = -id;
				StartForm sf = StartFormDAO.getInstance().findById(id);
				if (sf != null) {	// should always exist!
					Date date = SessionUtils.getDate(Constants.ATTR_WEEKEND_DATE);
					if (date != null) {
						production = ProductionDAO.getInstance().refresh(production);
						weeklyTimecard = TimecardUtils.createTimecard(tcUser, production, date, sf, false);
						if (weeklyTimecard != null) {
							LOG.debug("timecard created, id=" + weeklyTimecard.getId());
							next = WEEKLY_PAYROLL_PAGE; // jump to new page
							currentDate = weeklyTimecard.getEndDate();
							saveValues(); // remember which timecard to show
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return next;
	}

	/**
	 * Action method for the "Use Box Rental form" on the Payroll page. Creates
	 * a new BoxRental object attached to the current timecard.
	 *
	 * @return navigation string for the Box Rental mobile page, or null if
	 *         there was an error so the user remains on the same page.
	 */
	@Override
	public String actionCreateBoxRental() {
		try {
			if (refreshTimecard()) {
				if (! lockOrMessage("BoxRental")) {
					return null;
				}
				super.actionCreateBoxRental();
				weeklyTimecard.setUpdated(new Date());
				weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
				SessionUtils.put(Constants.ATTR_MTC_SHOW_BOX, true);
				SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "boxrentalm";
	}

	/**
	 * Action method for the "Enter" button on the Box Rental mobile edit page.
	 * The timecard should already be locked by the current user. Save any
	 * changes, and unlock it.
	 *
	 * @return Navigation string to return to the prior page, which is typically
	 *         the Weekly Payroll page, but may be the Timecard Review page if
	 *         an approver is editing the timecard.
	 */
	public String actionEnterBoxRental() {
		addButtonClicked();
		String ret = null;
		try {
			BoxRental rental = weeklyTimecard.getBoxRental();
			if (rental != null) {
				if (! TimecardCheck.validateBoxRental(weeklyTimecard.getBoxRental().getAmount())) {
					return null;
				}
				StartForm sf = weeklyTimecard.getStartForm();
				sf = StartFormDAO.getInstance().refresh(sf);
				TimecardUtils.updatePayExpense(weeklyTimecard, PayCategory.BOX_RENT_NONTAX,
						rental.getAmount(), sf.getBoxRental());
				updateTimecard();
			}
			ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
			SessionUtils.put(Constants.ATTR_MTC_SHOW_BOX, true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Action method for the "Delete box rental form" on the Weekly Payroll
	 * page. Test and acquire lock, then display the delete prompt.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionDeleteBoxRental() {
		try {
			if (refreshTimecard()) {
				forceLazyInit();
				if (! lockOrMessage("BoxRental.Delete")) {
					return null;
				}
				setShowPopup(true);
				PopupBean.getInstance().show(this, ACT_DELETE_BOX_RENTAL,
						null, // no title used
						"BoxRental.Delete",
						"Confirm.Delete", "Confirm.Cancel");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when the user clicks Ok (or equivalent) to confirm
	 * deletion of the box rental form.
	 *
	 * @return null navigation string
	 */
	@Override
	protected String actionDeleteBoxRentalOk() {
		try {
			super.actionDeleteBoxRentalOk();
			if (deletedBoxId != null) {
				BoxRental boxRental = BoxRentalDAO.getInstance().findById(deletedBoxId);
				getWeeklyTimecardDAO().delete(boxRental);
				deletedBoxId = null;
				TimecardUtils.deletePayExpense(weeklyTimecard, PayCategory.BOX_RENT_NONTAX);
				TimecardUtils.deletePayExpense(weeklyTimecard, PayCategory.BOX_RENT_TAX);
				updateTimecard();
			}
			else {
				getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action button for the "Add new entry", within the "Expenses" section on
	 * the Weekly Payroll mobile page. Test and acquire the lock on the
	 * timecard, add a new PayExpense line item, and go to the Expenses page.
	 *
	 * @return navigation string for the Expenses mobile page, or null if there
	 *         was an error so the user remains on the same page.
	 */
	public String actionAddExpense() {
		try {
			if (! refreshTimecard()) {
				return null;
			}
			if (! lockOrMessage("ExpenseLine")) {
				return null;
			}
			int n = weeklyTimecard.getExpenseLines().size();
			PayExpense exp = new PayExpense(weeklyTimecard, (byte)(n+1));
			weeklyTimecard.getExpenseLines().add(exp);

			weeklyTimecard.setUpdated(new Date());
			weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);

			payExpense = weeklyTimecard.getExpenseLines().get(n);
			SessionUtils.put(Constants.ATTR_MTC_EXPENSE_ID, payExpense.getId());
			SessionUtils.put(Constants.ATTR_MTC_SHOW_EXPENSES, true);
			SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "expensem";
	}

	/**
	 * Action method for the "Enter" button on the Expenses edit page. Saves any
	 * updated values and unlocks the timecard.
	 *
	 * @return Navigation string to return to the prior page, which defaults to
	 *         the Weekly Payroll page, but may be the Timecard Review page.
	 */
	public String actionEnterExpense() {
		String ret = null;
		try {
			addButtonClicked();
			String str = checkValidExpenseFields();
			if (str != null ) {
				if (str.length() == 0) { // blank: error msg issued, stay on this page
					str = null;
				}
				else { // str = navigation string, delete the empty expense item
					actionDeleteExpenseOk();
				}
				return str;
			}
			PayExpense pe = getPayExpense();
			if (pe != null) {
				pe.calculateTotal();
				weeklyTimecard.getTotalExpenses();
				boolean valid = TimecardCheck.validateExpenseItems(weeklyTimecard);
				if (valid) {
					updateTimecard();
					ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * used when user click on back button of Add expense page
	 * returns the page url to be redirected
	 */
	public String actionBackPayExpense(){
		return actionEnterExpense();
	}

	/**
	 * @param event
	 * used when user click on logout button of AddExpenese page
	 */
	public void actionLogoutPayExpense(ActionEvent event){
		actionDeleteExpenseOk();
		HeaderViewBean.getInstance().actionLogout(event);
	}


	/**
	 * Validate Add expense fields
	 *
	 * @return the faces navigation string for the page to be redirected to if
	 *         all fields are empty; a blank string if some data items are filled in
	 *         but not the category; and null if a category has been selected.
	 */
	private String checkValidExpenseFields() {
		if (expCategory == null) {
			if (getPayExpense().getQuantity() == null && getPayExpense().getRate() == null && NumberUtils.isEmpty(getPayExpense().getTotal())) {
				return SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
			}
			else if (getPayExpense().getQuantity() != null || getPayExpense().getRate() != null || !NumberUtils.isEmpty(getPayExpense().getTotal())) {
				MsgUtils.addFacesMessage("Timecard.TimecardNotComplete.Mobile.ItemType", FacesMessage.SEVERITY_ERROR);
				return "";
			}
		}
		return null;
	}

	/**
	 * Action method called when the user clicks the Delete Entry button on the
	 * Weekly Expenses mobile page. If the entry is empty, delete it
	 * immediately; otherwise, display the confirmation prompt. If the Delete
	 * button is available, then the timecard should already be locked; we don't
	 * check it here.
	 *
	 * @return if deleted, the "back" navigation string; otherwise, a null
	 *         navigation string.
	 */
	public String actionDeleteExpense() {
		PayExpense pe = getPayExpense();
		if (pe != null && pe.getCategory() == null && pe.calculateTotal() == null) {
			return actionDeleteExpenseOk();
		}
		setShowPopup(true);
		PopupBean.getInstance().show(this, ACT_DELETE_EXPENSE_ENTRY,
				null, // no title used
				"Expense.DeleteEntry",
				"Confirm.Delete", "Confirm.Cancel");
		return null;
	}

	/**
	 * Action method called when the user clicks the Ok (or Delete) button on
	 * the confirmation fragment. This deletes one detail line from the Mileage
	 * Form, and recalculate the mileage totals. The database id of the line
	 * being deleted is in our session.
	 *
	 * @return navigation string for the prior page, which is in our session;
	 *         typically the mobile weekly payroll page, but could also be the
	 *         mobile timecard review page.
	 */
	private String actionDeleteExpenseOk() {
		String ret = null;
		try {
			PayExpense payExp = getPayExpense();
			if (payExp != null) {
				int ix = 0;
				for (PayExpense pe : weeklyTimecard.getExpenseLines()) {
					if (pe.equals(payExp)) {
						break;
					}
					ix++;
				}
				if (ix < weeklyTimecard.getExpenseLines().size()) {
					actionDeleteExpenseLine(ix);
					updateTimecard(); // save changes and unlock the TC
				}
				SessionUtils.put(Constants.ATTR_MTC_EXPENSE_ID, null);
			}
			ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Action method for the "Use Mileage form" on the Payroll page. Creates a
	 * new Mileage object and adds one blank line to it.
	 *
	 * @return navigation string for the mileage mobile page, or null if
	 *         there was an error so the user remains on the same page.
	 */
	@Override
	public String actionCreateMileageForm() {
		try {
			if (! refreshTimecard()) {
				return null;
			}
			if (! lockOrMessage("Mileage")) {
				return null;
			}
			super.actionCreateMileageForm();
			weeklyTimecard.setUpdated(new Date());
			weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
			SessionUtils.put(Constants.ATTR_MTC_SHOW_MILES, true);
			SessionUtils.put(Constants.ATTR_MTC_MILEAGE_ID, weeklyTimecard.getMileage().getMileageLines().get(0).getId());
			SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "mileagem";
	}

	/**
	 * Action button for the "Add new entry", within the "mileage" section on
	 * the Weekly Payroll mobile page. Test and acquire the lock on the
	 * timecard, add a new mileage line item, and go to the Mileage page.
	 *
	 * @return navigation string for the mileage mobile page, or null if there
	 *         was an error so the user remains on the same page.
	 * @see com.lightspeedeps.web.timecard.TimecardBase#actionAddMileage()
	 */
	@Override
	public String actionAddMileage() {
		try {
			if (! refreshTimecard()) {
				return null;
			}
			if (! lockOrMessage("MileageLine")) {
				return null;
			}
			mileageLine = TimecardUtils.addMileageLine(weeklyTimecard);
			weeklyTimecard.setUpdated(new Date());
			weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
			int size = weeklyTimecard.getMileage().getMileageLines().size();
			mileageLine = weeklyTimecard.getMileage().getMileageLines().get(size-1);

			SessionUtils.put(Constants.ATTR_MTC_MILEAGE_ID, mileageLine.getId());
			SessionUtils.put(Constants.ATTR_MTC_SHOW_MILES, true);
			SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "mileagem";
	}

	/**
	 * Action method for the "Enter" button on the Mileage edit page. Saves any
	 * updated values and unlocks the timecard.
	 *
	 * @return Navigation string to return to the prior page, which defaults to
	 *         the Weekly Payroll page, but may be the Timecard Review page.
	 */
	public String actionEnterMileage() {
		String ret = null;
		try {
			addButtonClicked();
			weeklyTimecard.getMileage().setMiles(null); // force re-totaling

			String str = checkValidMileageFields();
			if (str != null ) {
				if (str.length() == 0) { // blank: error msg issued, stay on this page
					str = null;
				}
				else { // str = navigation string, delete the empty mileage item
					actionDeleteMileageOk();
				}
				return str;
			}

			MileageLine mlgLine = getMileageLine();
			if (mlgLine != null) {
				// Validate mileage fields & generate any error messages
				boolean valid = TimecardCheck.validateMileageLine(mlgLine);
				if (valid) {
					TimecardUtils.updateMileageExpense(weeklyTimecard);
					updateTimecard();
					ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * returns the url to be redirected if all the field are not empty or empty
	 * */
	private String checkValidMileageFields() {
		if (getMileageLine().getDate() == null && StringUtils.isEmpty(getMileageLine().getDestination())
				&& NumberUtils.isEmpty(getMileageLine().getOdometerStart())
				&& NumberUtils.isEmpty(getMileageLine().getOdometerEnd())
				&& NumberUtils.isEmpty(getMileageLine().getMiles())) {
			return SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		else if (getMileageLine().getDate() == null || StringUtils.isEmpty(getMileageLine().getDestination())
				|| NumberUtils.isEmpty(getMileageLine().getMiles())) {
			MsgUtils.addFacesMessage("Timecard.TimecardNotComplete.Mobile.MileageLine", FacesMessage.SEVERITY_ERROR);
			return "";
		}
		return null;
	}

	/**
	 * Action method called when the user clicks the Delete Entry button on the
	 * Weekly Mileage mobile page. Display the confirmation prompt.  If the button
	 * is available, then the timecard should already be locked; we don't check
	 * it here.
	 *
	 * @return null navigation string.
	 */
	public String actionDeleteMileage() {
		setShowPopup(true);
		PopupBean.getInstance().show(this, ACT_DELETE_MILEAGE_ENTRY,
				null, // no title used
				"Mileage.DeleteEntry",
				"Confirm.Delete", "Confirm.Cancel");
		return null;
	}

	/**
	 * Action method called when the user clicks the Ok (or Delete) button on
	 * the confirmation fragment. This deletes one detail line from the Mileage
	 * Form, and recalculate the mileage totals. The database id of the line
	 * being deleted is in our session.
	 *
	 * @return navigation string for the prior page, which is in our session;
	 *         typically the mobile weekly payroll page, but could also be the
	 *         mobile timecard review page.
	 */
	private String actionDeleteMileageOk() {
		String ret = null;
		try {
			Mileage mileage = weeklyTimecard.getMileage();
			if (mileage != null) {
				MileageLine line = null;
				int id = SessionUtils.getInteger(Constants.ATTR_MTC_MILEAGE_ID, -1);
				for (MileageLine ml : mileage.getMileageLines()) {
					if (ml.getId() != null && ml.getId().intValue() == id) {
						line = ml; // found matching mileage line item
						break;
					}
				}
				if (line != null) {
					mileage.getMileageLines().remove(line);
					if (line.getId() != null) {
						getWeeklyTimecardDAO().delete(line);
					}
					weeklyTimecard.getMileage().setMiles(null); // force re-totaling
					TimecardUtils.updateMileageExpense(weeklyTimecard);
					weeklyTimecard.setUpdated(new Date());
					weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
					SessionUtils.put(Constants.ATTR_MTC_MILEAGE_ID, null);
				}
			}
			ret = SessionUtils.getString(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * Action method for the "Delete Mileage form" on the Payroll page.
	 * Just display the delete prompt.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionDeleteMileageForm() {
		if (refreshTimecard()) {
			forceLazyInit();
			if (! lockOrMessage("Mileage.Delete")) {
				return null;
			}
			setShowPopup(true);
			PopupBean.getInstance().show(this, ACT_DELETE_MILEAGE,
					null, // no title used
					"Mileage.Delete",
					"Confirm.Delete", "Confirm.Cancel");
		}
		return null;
	}

	/**
	 * Action method called when the user clicks Ok (or equivalent) after
	 * prompting to confirm deletion of the mileage form.
	 *
	 * @return null navigation string
	 */
	@Override
	protected String actionDeleteMileageFormOk() {
		super.actionDeleteMileageFormOk();
		if (deletedMileageId != null) {
			Mileage mileage = MileageDAO.getInstance().findById(deletedMileageId);
			getWeeklyTimecardDAO().delete(mileage);
			deletedMileageId = null;
		}
		TimecardUtils.deletePayExpense(weeklyTimecard, PayCategory.MILEAGE_NONTAX);
		TimecardUtils.deletePayExpense(weeklyTimecard, PayCategory.MILEAGE_TAX);
		updateTimecard();
		return null;
	}

	/**
	 * The action method for Logout from Add mileage form
	 */
	public void actionLogoutMileageForm(ActionEvent event) {
		actionDeleteMileageOk();
		HeaderViewBean.getInstance().actionLogout(event);
	}


	/**
	 * The action method for redirect back to the previous page from Add mileage form
	 */
	public String actionBackMileageForm() {
		return actionEnterMileage();
	}

	/**
	 * Action method for the "Enter" button in the "Start Information" section
	 * of the Weekly Payroll page. Saves any updated values.
	 *
	 * @return null Navigation string.
	 */
	public String actionEnterStartInfo() {
		try {
			addButtonClicked();
			if (getStartInfoChanged()) {
				weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
				StartFormService.updateStartFormRequiredFields(weeklyTimecard);
			}
			startInfoRequired = ! weeklyTimecard.getHasRequiredFields(checkPaidAs);
			saveWorkCity = weeklyTimecard.getCityWorked();
			saveWorkState = weeklyTimecard.getStateWorked();
			saveSSN = weeklyTimecard.getSocialSecurity();
			savePaidAs = weeklyTimecard.getPaidAs();
			saveFederalCorpId = weeklyTimecard.getFedCorpId();
			showEditSsn = StringUtils.isEmpty(saveSSN);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Timecard" button, which toggles the display
	 * of the daily entries on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleDays() {
		try {
			if (refreshTimecard()) {
				showDays = ! showDays;
				SessionUtils.put(Constants.ATTR_MTC_SHOW_TC, showDays);
				forceLazyInit();
				if (showDays) {
					TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
					addJavascript("scrollToHash('timecard');");
				}
				else {
					addJavascript("scrollToHash('default');");
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Expenses" button, which toggles the display
	 * of the expense information on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleExpenses() {
		if (refreshTimecard()) {
			showExpenses = ! showExpenses;
			SessionUtils.put(Constants.ATTR_MTC_SHOW_EXPENSES, showExpenses);
			if (showExpenses) {
				addJavascript("scrollToHash('expenses');");
			}
			else {
				addJavascript("scrollToHash('default');");
			}
		}
		return null;
	}

	/**
	 * Action method for the "Mileage" button, which toggles the display
	 * of the mileage information on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleMileage() {
		if (refreshTimecard()) {
			showMileage = ! showMileage;
			SessionUtils.put(Constants.ATTR_MTC_SHOW_MILES, showMileage);
			if (showMileage) {
				addJavascript("scrollToHash('mileage');");
			}
			else {
				addJavascript("scrollToHash('default');");
			}
		}
		return null;
	}

	/**
	 * Action method for the "Box Rental" button, which toggles the display
	 * of the box rental information on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleBoxRental() {
		if (refreshTimecard()) {
			showBoxRental = ! showBoxRental;
			SessionUtils.put(Constants.ATTR_MTC_SHOW_BOX, showBoxRental);
			if (showBoxRental) {
				SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
				addJavascript("scrollToHash('boxrental');");
			}
			else {
				addJavascript("scrollToHash('default');");
			}
		}
		return null;
	}

	/**
	 * Action method for the "Expenses" button, which toggles the display
	 * of the Expense table information on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleShowExpenses() {
		if (refreshTimecard()) {
			showExpenses = ! showExpenses;
			SessionUtils.put(Constants.ATTR_MTC_SHOW_EXPENSES, showExpenses);
			if (showExpenses) {
				addJavascript("scrollToHash('expenses');");
			}
			else {
				addJavascript("scrollToHash('default');");
			}
		}
		return null;
	}

	/**
	 * Action method for the "Start Information" button, which toggles the display
	 * of the three required Start Form-related fields on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleStartInfo() {
		weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
		if (refreshTimecard()) {
			showStartInfo = ! showStartInfo;
			SessionUtils.put(Constants.ATTR_MTC_SHOW_START_INFO, showStartInfo);
			if (showStartInfo) {
				addJavascript("scrollToHash('startinfo');");
				openStartInfo();
			}
			else {
				addJavascript("scrollToHash('default');");
			}
		}
		return null;
	}

	/**
	 * Action method for the "Show/Hide" SSN button, which toggles the enabled
	 * status of the SSN input field on the Weekly Payroll page.
	 *
	 * @return null navigation String
	 */
	public String actionToggleShowSsn() {
		if (weeklyTimecard == null) { // someone deleted it?
			actionTimecardMissing();
		}
		else {
			showEditSsn = ! showEditSsn;
		}
		return null;
	}

	/**
	 * Action method for the "Comment" button, which toggles the display of the
	 * Comment section on various pages - Daily Time, Box Rental, and Mileage.
	 *
	 * @return null navigation String
	 */
	public String actionToggleShowComment() {
		showComment = ! showComment;
		if (showComment) {
			addJavascript("scrollToHash('comments');");
		}
		else {
			addJavascript("scrollToHash('default');");
		}
		return null;
	}

	/**
	 * Action method for the "Additional Fields" button on the Daily Time page,
	 * which toggles the display of the grace fields, work city, state, etc.
	 *
	 * @return null navigation String
	 */
	public String actionToggleShowMoreDaily() {
		showMoreDaily = ! showMoreDaily;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_MORE_DAILY, showMoreDaily);
		if (showMileage) {
			addJavascript("scrollToHash('moredaily');");
		}
		else {
			addJavascript("scrollToHash('default');");
		}
		return null;
	}

	/**
	 * Action method for the "Add" (comment) button.
	 * @return null navigation string.
	 */
	@Override
	public String actionAddComment() {
		TimecardUtils.addComment(weeklyTimecard, getNewComment());
		weeklyTimecard.setUpdated(new Date());
		weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);
		setNewComment("");
		return null;
	}

	/**
	 * Action method for the "copy last week" on the Weekly Payroll
	 * page. Test and acquire lock, then display the Copy Week prompt.
	 *
	 * @return null navigation string
	 */
	public String actionCopyWeek() {
		try {
			int i = findWeekEndEntry();
			Date priorDate = TimecardUtils.calculatePriorWeekEndDate(weeklyTimecard.getEndDate());
			boolean found = false;
			for (; i < getWeeklyTimecardList().size(); i++) { // TODO TEST THIS !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				WeeklyTimecard wtc = getWeeklyTimecardList().get(i);
				if (wtc != null && wtc.getEndDate().equals(priorDate)) {
					found = true;
					break;
				}
			}
			if (! found) {
				MsgUtils.addFacesMessage("Timecard.PriorWeekNotFound", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (refreshTimecard()) {
				forceLazyInit();
				if (! lockOrMessage("CopyWeek")) {
					return null;
				}
				setShowPopup(true);
				PopupBean.getInstance().show(this, ACT_COPY_WEEK,
						null, // no title used
						"Timecard.CopyWeek",
						"Confirm.Copy", "Confirm.Cancel");
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called when user Ok's the prompt
	 * to copy last week's times into the current time card.
	 *
	 * @return null Navigation string
	 */
	private String actionCopyWeekOk() {
		actionCopyPrior();
		updateTimecard(); // unlock & update timecard in database
		return null;
	}

	/**
	 * Action method when user clicks Cancel to the prompt
	 * to copy last week's times into the current time card.
	 *
	 * @return null Navigation string.
	 */
	private String actionCopyWeekCancel() {
		getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
		TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
		forceLazyInit();
		return null;
	}

	/**
	 * Action method for the "Clone Timecard" button on the Weekly
	 * Payroll mobile page.
	 *
	 * @return The navigation string for the Clone Timecard page.
	 */
	public String actionGoClone() {
		return "clonem";
	}

	/**
	 * Action method for the Recall timecard button.
	 *
	 * @return null navigation string
	 */
	public String actionRecall() {
		try {
			if (refreshTimecard()) {
				forceLazyInit();
				if (! lockOrMessage("Recall")) {
					return null;
				}
				setShowPopup(true);
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_RECALL,
						"Timecard.RecallToEmployee.Mobile.");
				SimpleDateFormat oSdf = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);
				String param = oSdf.format(weeklyTimecard.getEndDate());
				bean.setMessage(MsgUtils.formatMessage("Timecard.RecallToEmployee.Mobile.Text", param));
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionRecallOk() {
		if (refreshTimecard()) {
			Integer newApproverContactId = getvContact().getId();
			if (weeklyTimecard.getUserAccount().equals(getvUser().getAccountNumber())) {
				newApproverContactId = null;
			}
			weeklyTimecard = getWeeklyTimecardDAO().recall(weeklyTimecard, newApproverContactId);
			statusChanged(weeklyTimecard);
		}
		return WEEKLY_PAYROLL_PAGE;
	}

	/**
	 * Action method for the "submit" button on the Timecard Review page.
	 *
	 * @return navigation string to go to either the "Confirm submit" or
	 *         "Create PIN" page.
	 */
	public String actionReviewSubmit() {
		SessionUtils.put(Constants.ATTR_SUBMIT_BACK_PAGE, "tcreviewm");
		if (refreshTimecard() && ! weeklyTimecard.getHasRequiredFields(checkPaidAs)) {
			if (! TimecardService.updateTimecardRequiredFields(weeklyTimecard)) {
				// LS-2727 & LS-2734 put up team specific message for Team clients.
				String msgId = "Timecard.TimecardNotComplete.Mobile.Review";
				if(isTeamPayroll()) {
					msgId = "Timecard.TimecardNotComplete.MobileTeam.Review";
				}
				MsgUtils.addFacesMessage(msgId, FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		return actionSubmitMobile();
	}

	/**
	 * Action method for the "Submit" button on the Weekly Payroll page. Check
	 * if the user has a PIN yet - if not, route to the Create PIN page;
	 * otherwise, route to the Confirm Submit page.
	 *
	 * @return navigation string to go to either the "Confirm submit" or
	 *         "Create PIN" page.
	 */
	public String actionGoSubmit() {
		SessionUtils.put(Constants.ATTR_SUBMIT_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		return actionSubmitMobile();
	}


	/**
	 * Action code for the Submit button on either the Timecard Review or Weekly
	 * Payroll pages.
	 *
	 * @return navigation string to go to either the "Confirm submit" or
	 *         "Create PIN" page.
	 */
	private String actionSubmitMobile() {
		if (! refreshTimecard()) {
			return null;
		}
		if (! weeklyTimecard.getHasRequiredFields(checkPaidAs)) {
			if (! TimecardService.updateTimecardRequiredFields(weeklyTimecard)) {
				setShowPopup(true);
				// LS-2727 & LS-2734 put up team specific message for Team clients.
				String bodyTextId = "Timecard.TimecardNotComplete.Mobile.Text";
				if(isTeamPayroll()) {
					bodyTextId = "Timecard.TimecardNotComplete.MobileTeam.Text";
				}
				PopupBean.getInstance().show(this, 0,
						"Timecard.TimecardNotComplete.Title",
						bodyTextId,
						"Confirm.OK", null); // no cancel button
				openStartInfo();
				forceLazyInit();
				return null;
			}
		}
		if (user.getPin() == null) {
			SessionUtils.put(Constants.ATTR_PIN_NEXT_PAGE, "submitm");
			return "createpinm";
		}
		if (! lockOrMessage("Submit.Text")) {
			return null;
		}
		String ret = "submitm";
		if (user.equals(tcUser)) {
			Integer id = TimecardUtils.findNextApproverContactId(weeklyTimecard);
			if (id != null && id.equals(SessionUtils.getCurrentContact().getId())) {
				// first approver = submitter; treat as approval to allow re-direct option;
				// set up for transfer to MobileApproverBean page
				SessionUtils.put(MobileApproverBean.ATTR_MTC_APPROVE_NOW_ID, weeklyTimecard.getId());
				SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId()); // need this for "Back"; rev 4032
				ret = "submitapprovem"; // go to "submit and approve" page
			}
		}
		getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
		return ret;
	}

	/**
	 * Action method for the "E-Sign" (confirm/submit) button on the timecard submit page.
	 * Validate the user's password and PIN, and (if valid) mark the timecard
	 * submitted.
	 *
	 * @return If successful, the navigation string for the "confirmation"
	 *         (submit completed) page. If not, a null navigation string is
	 *         returned.
	 */
	public String actionConfirmSubmit() {
		if (submitType == TimecardSubmitType.OTHER) {
			if (submitComment == null || submitComment.trim().length() < 3) {
				MsgUtils.addFacesMessage("Timecard.Submit.CommentRequired", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (StringUtils.isEmpty(getPin())) {
			MsgUtils.addFacesMessage("Timecard.Submit.PinBlank", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (StringUtils.isEmpty(getPassword())) {
			MsgUtils.addFacesMessage("Timecard.Submit.PasswordBlank", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (! getPin().equals(user.getPin()) ||
				! getPassword().equals(user.getPassword())) {
			MsgUtils.addFacesMessage("Timecard.Submit.PinError", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		String comment = null;
		TimecardSubmitType subType = null;
		if (! user.equals(tcUser)) {
			subType = submitType;
			if (submitType == TimecardSubmitType.OTHER) {
				comment = getSubmitComment();
			}
		}
		int ret = submit(false, subType, comment);
		String nav = null; // default to staying on the same page.
		switch (ret) {
			case SUBMIT_OK:
				nav = "confirmm"; // go to confirmation page
				break;
			case SUBMIT_FAILED_NO_APPROVER:
				// This will only happen if the approver(s) were removed while the "Submit"
				// page was being displayed, since we checked before we displayed that page.
				MsgUtils.addFacesMessage("Timecard.NoApprovers.Text", FacesMessage.SEVERITY_WARN);
				break;
			case SUBMIT_FAILED:
				MsgUtils.addFacesMessage("Timecard.Submit.Failed", FacesMessage.SEVERITY_ERROR);
				break;
			case SUBMIT_FAILED_SUBMITTED:
				MsgUtils.addFacesMessage("Timecard.Submit.Submitted", FacesMessage.SEVERITY_ERROR);
				break;
			case SUBMIT_FAILED_DELETED:
				MsgUtils.addFacesMessage("Timecard.Submit.Deleted", FacesMessage.SEVERITY_ERROR);
				break;
		}
		return nav;
	}

	/**
	 * Action method for the "Cancel" button on the Submit mobile page. Just
	 * unlocks the timecard.
	 *
	 * @return Navigation string for the Weekly Payroll page.
	 */
	public String actionCancelSubmit() {
		LOG.debug("");
		weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
		getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
		// even if the unlock fails, no error is reported to the user
		String ret = SessionUtils.getString(Constants.ATTR_SUBMIT_BACK_PAGE, WEEKLY_PAYROLL_PAGE);
		return ret;
	}

	/**
	 * Action method for the "Full Site" link on the mobile pages. We clear the
	 * "saved page" information, to prevent our normal code from trying to
	 * forward us back to a mobile page; and return the JSF navigation string
	 * for the desktop home, "My Productions".
	 *
	 * @return The "My Productions" (desktop) navigation string.
	 */
	public String actionFullSite() {
		SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, null);
		String ret = HeaderViewBean.MYPROD_MENU_PROD;
		LOG.debug("jump to full site: " + ret);
		return ret;
	}

	/**
	 * Called by an invisible button, linked to the user typing in the
	 * Work City field.  No action is necessary, but the button needs
	 * to trigger a method call to force an update/render cycle.
	 * @return Null navigation string.
	 */
	public String actionCityWorkedChange() {
		return null;
	}

	/**
	 * ValueChangeListener for the City Worked (workCity) field; this method is
	 * necessary to get a form submission to happen upon loss of focus in the
	 * field, but we don't need to do any other work here.
	 *
	 * @param event the change event created by the framework, containing the
	 *            old and new values
	 */
	public void listenCityWorkedChange(ValueChangeEvent event) {
		//
	}

	/**
	 * ValueChangeListener method for the input fields on the daily time entry page.
	 * Used to validate inputs and recalculate total(s).
	 *
	 * @see com.lightspeedeps.web.timecard.TimecardBase#listenDailyChange(javax.faces.event.ValueChangeEvent)
	 */
	@Override
	public void listenDailyChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			DailyTime dt = getDailyTime();
			if (dt != null) {
				TimecardCheck.validateAndCalcWorkDay(dt, true);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener method for the Grace fields, to see if they should be
	 * converted from minutes to decimal. Any entry greater than 1 is assumed to
	 * be minutes, and is converted to the equivalent (rounded) decimal
	 * fraction. Note that we expect this method to be called during the
	 * INVOKE_APPLICATION phase, so we can update the grace fields and the new
	 * values won't be overwritten by JSF.
	 *
	 * @param event the change event created by the framework, containing the
	 *            old and new values
	 */
	public void listenGraceChange(ValueChangeEvent event) {
		BigDecimal grace = checkGrace(getDailyTime().getGrace1());
		getDailyTime().setGrace1(grace);
		grace = checkGrace(getDailyTime().getGrace2());
		getDailyTime().setGrace2(grace);
	}

	/**
	 * Check an input value for Grace -- if greater than 1, assume it's minutes,
	 * so divide it by 60 to get equivalent time in hours. In any case, round it
	 * as appropriate for the production/user.
	 *
	 * @param grace The value to be checked as a reasonable "Grace" period. Null
	 *            is allowed.
	 * @return The rounded and possibly converted value. Returns null only if
	 *         input is null.
	 */
	private BigDecimal checkGrace(BigDecimal grace) {
		if (grace != null) {
			if (grace.compareTo(BigDecimal.ONE) >= 0) {
				grace = grace.divide(Constants.MINUTES_PER_HOUR, 3, RoundingMode.UP);
			}
			HourRoundingType roundingType = (HourRoundingType)SessionUtils.get(
					Constants.ATTR_TIME_ROUNDING_TYPE, HourRoundingType.TENTH);
			grace = NumberUtils.round(grace, roundingType, RoundingMode.UP);
		}
		return grace;
	}

	/**
	 * ValueChangeListener method for all Expense fields, including the
	 * drop-down category selection.
	 *
	 * @param event the change event created by the framework, containing the
	 *            old and new values
	 */
	@Override
	public void listenExpenseChange(ValueChangeEvent event) {
		try {
			if (weeklyTimecard != null) {
				PayExpense pe = getPayExpense();
				if (pe != null) {
					Object value = event.getNewValue();
					if (value instanceof PayCategory) {
						PayCategory pc = (PayCategory)value;
						if (pc != null) {
							pe.setCategory(pc.getLabel());
						}
					}
					// Validate fields & generate any error messages
					TimecardCheck.validateExpenseItems(weeklyTimecard);
					pe.calculateTotal();
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener method for all mileage fields.
	 * @param event the change event created by the framework, containing the
	 *            old and new values
	 */
	@Override
	public void listenMileageChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			MileageLine mlgLine = getMileageLine();
			if (mlgLine != null && weeklyTimecard!=null && weeklyTimecard.getMileage() != null) {
				if (mileageLine.getOdometerStart() != null && mileageLine.getOdometerEnd() != null) {
					mileageLine.setMiles(null);
				}
				// Validate mileage fields & generate any error messages
				TimecardCheck.validateMileageLine(mlgLine);
				weeklyTimecard.getMileage().setMiles(null); // force re-totaling
				TimecardUtils.updateMileageExpense(weeklyTimecard);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	//LS-3124
	public void listenMStartPaidAsChange(ValueChangeEvent event) {
		PaidAsType paidAsType = (PaidAsType)event.getNewValue();
		if (isTeamPayroll() && paidAsType.isLoanOut() && weeklyTimecard.getPaidAs() == null) {
			MsgUtils.addFacesMessage("Mobile.Timecard.MStartPaidAs", FacesMessage.SEVERITY_ERROR);
			checkPaidAs = true;
		}
		else {
			checkPaidAs = false;
		}
	}

	/**
	 * Called when user clicks Ok on a "confirmation popup" (which is
	 * NOT an actual popup in the mobile environment).
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		setShowPopup(false); // Close "pop-up" display
		switch(action) {
			case ACT_DELETE_MILEAGE_ENTRY:
				res = actionDeleteMileageOk();
				break;
			case ACT_DELETE_MILEAGE:
				res = actionDeleteMileageFormOk();
				break;
			case ACT_DELETE_BOX_RENTAL:
				res = actionDeleteBoxRentalOk();
				break;
			case ACT_DELETE_EXPENSE_ENTRY:
				res = actionDeleteExpenseOk();
				break;
			case ACT_COPY_WEEK:
				res = actionCopyWeekOk();
				break;
			case ACT_RECALL:
				res = actionRecallOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		forceLazyInit();	// 2.9.5557
		return res;
	}

	/**
	 * Called when user clicks Cancel on a "confirmation popup" (which is
	 * NOT an actual popup in the mobile environment).
	 * @see com.lightspeedeps.web.timecard.TimecardBase#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		setShowPopup(false);
		switch(action) {
			case ACT_DELETE_MILEAGE_ENTRY:
			case ACT_DELETE_MILEAGE:
			case ACT_DELETE_BOX_RENTAL:
				forceLazyInit();
				break;
			case ACT_COPY_WEEK:
				actionCopyWeekCancel();
				break;
			default:
				res = super.confirmCancel(action);
				break;
		}
		return res;
	}

	/**
	 * Refresh the current weeklyTimecard.
	 *
	 * @return True if refresh was successful, false if not. Typically it fails
	 *         only if the timecard has been deleted.
	 */
	private boolean refreshTimecard() {
		weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
		if (weeklyTimecard == null) { // someone deleted it?
			actionTimecardMissing();
			return false;
		}

		// Fixes LIE LS-1139
		forceLazyInit();

		return true;
	}

	/**
	 * Called by various action methods when the timecard we were viewing is no
	 * longer available -- typically after a refresh() call returns null. This
	 * can happen if another user deleted the timecard this user was viewing.
	 */
	private void actionTimecardMissing() {
		showDays = true;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_TC, showDays);
		showBoxRental = false;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_BOX, showBoxRental);
		showExpenses = false;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_EXPENSES, showExpenses);
		showMileage = false;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_MILES, showMileage);
		showStartInfo = false;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_START_INFO, showStartInfo);
		showMoreDaily = false;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_MORE_DAILY, showMoreDaily);
		addJavascript("scrollToHash('default');");
	}

	/**
	 * Set up for opening the "Start Information" section of the Weekly Payroll
	 * page. Saves copies of the fields to detect whether they have been changed
	 * or not.
	 */
	private void openStartInfo() {
		showStartInfo = true;
		SessionUtils.put(Constants.ATTR_MTC_SHOW_START_INFO, showStartInfo);
		saveWorkCity = weeklyTimecard.getCityWorked();
		saveWorkState = weeklyTimecard.getStateWorked();
		saveSSN = weeklyTimecard.getSocialSecurity();
		savePaidAs = weeklyTimecard.getPaidAs();
		saveFederalCorpId = weeklyTimecard.getFedCorpId();
		showEditSsn = StringUtils.isEmpty(saveSSN);
	}

	/**
	 * Recover our saved information from the Session, and load the appropriate
	 * database objects, e.g., Production, WeeklyTimecard.
	 */
	private void loadValues() {
		Production cProd = SessionUtils.getProduction();
		if (cProd != null && ! cProd.isSystemProduction()) {
			Integer id = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID);
			if (id != null) {
				production = ProductionDAO.getInstance().findById(id);
			}
			else { // can happen after a server restart when session is recovered
				production = cProd;
				UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROD_ID, production.getId());
			}

			if (production.getType().hasPayrollByProject()) {
				id = SessionUtils.getInteger(Constants.ATTR_LAST_PROJECT_ID);
				if (id != null) {
					Project project = ProjectDAO.getInstance().findById(id);
					setViewProject(project);
					// There's no need in mobile for the "current" project to be different
					// from the "viewed" project, so set current as well.
					SessionUtils.setCurrentProject(project);
				}
			}
			else {
				setViewProject(null);
			}
			setCommProject(getViewProject());

			id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
			weeklyTimecard = null;
			if (id != null) {
				weeklyTimecard = getWeeklyTimecardDAO().findById(id);
			}
			if (weeklyTimecard != null) {
				if (! weeklyTimecard.getProdId().equals(production.getProdId())) {
					weeklyTimecard = null;
					SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
				}
				else {
					loadTimecard();
					position = weeklyTimecard.getOccupation();
					startForm = weeklyTimecard.getStartForm();
					UserPrefBean.getInstance().put(Constants.ATTR_MTC_START_FORM_ID,
							startForm == null ? null : startForm.getId());
				}
			}
			else {
				startForm = null;
				position = null;
				id = UserPrefBean.getInstance().getInteger(Constants.ATTR_MTC_START_FORM_ID);
				if (id != null) {
					startForm = StartFormDAO.getInstance().findById(id);
					if (startForm != null) {
						position = startForm.getJobClass();
					}
				}
			}
			currentDate = SessionUtils.getDate(Constants.ATTR_MTC_DATE);
		}
		LOG.debug("prod=" + production + ", tc=" + weeklyTimecard +
				", pos=" + position + ", date=" + currentDate);
	}

	/**
	 * Prepare the current WeeklyTimecard for display -- force references to
	 * each DailyTime (to avoid LazyInitializationException`s), and calculate
	 * the daily and weekly totals. Also sets the {@link TimecardBase#editRaw}
	 * field based on the status of the timecard.
	 */
	private void loadTimecard() {
		for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
			dt.getDate();
			dt.setSelected(false);
			if (dt.getWeekDayNum() == todaysWeekDayNum && todaysWeekEndDate.equals(weeklyTimecard.getEndDate())) {
				dt.setSelected(true);
			}
			// Validate timecard & calculate the hours of work for each day
			TimecardCheck.validateAndCalcWorkDay(dt, true);
		}
		TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
		setShowCopyPrior(null); // force recalculation of value.
		editRaw = false;
		if (weeklyTimecard.getSubmitable() && // at employee level (OPEN or rejected to employee)
				production != null &&
				production.isWritable()) {
			editRaw = true;
		}
		calculateMobileAuth(weeklyTimecard); // sets the cloneAuth flag
		if (weeklyTimecard.getMileage() != null) {
			weeklyTimecard.getMileage().getMileageLines().size();
		}
		startInfoRequired = ! weeklyTimecard.getHasRequiredFields(checkPaidAs);
		saveWorkCity = weeklyTimecard.getCityWorked();
		saveWorkState = weeklyTimecard.getStateWorked();
		saveSSN = weeklyTimecard.getSocialSecurity();
		savePaidAs = weeklyTimecard.getPaidAs();
		saveFederalCorpId = weeklyTimecard.getFedCorpId();
		showEditSsn = StringUtils.isEmpty(saveSSN);
	}

	/**
	 * Attempt to lock the current timecard. If the lock fails, display an error
	 * message.
	 *
	 * @param msgType The 'additional' part of the message id for the error. The
	 *            message id used to look up the prompt is
	 *            <"Timecard.CardLocked." + msgType>.
	 * @return True iff the current WeeklyTimecard has been successfully locked
	 *         for the current user.
	 */
	protected boolean lockOrMessage(String msgType) {
		if (! getWeeklyTimecardDAO().lock(weeklyTimecard, getvUser())) {
			MsgUtils.addFacesMessage("Timecard.CardLocked." + msgType, FacesMessage.SEVERITY_ERROR);
			LOG.debug("action (" + msgType + ") prevented: locked by user #" + weeklyTimecard.getLockedBy());
			forceLazyInit();
			return false;
		}
		return true;
	}

	/**
	 * Pre-load fields to avoid LazyInitializationException errors.
	 * @see com.lightspeedeps.web.timecard.TimecardBase#forceLazyInit()
	 */
	@Override
	protected void forceLazyInit() {
		try {
			super.forceLazyInit();
			for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
				dt.getDate();
			}
			getPayrollPref(); // flags referenced during render.
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Save our current state to the user's HTTP Session.
	 */
	private void saveValues() {
		UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROD_ID,
				production == null ? null : production.getId());
		UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROJECT_ID,
				getViewProject() == null ? null : getViewProject().getId());
		UserPrefBean.getInstance().put(Constants.ATTR_MTC_START_FORM_ID,
				startForm == null ? null : startForm.getId());
		SessionUtils.put(Constants.ATTR_TIMECARD_ID,
				weeklyTimecard == null ? null : weeklyTimecard.getId());
		SessionUtils.put(Constants.ATTR_MTC_DATE, currentDate);
	}

	/**
	 * Unlock and update the weeklyTimecard in the database.
	 */
	private void updateTimecard() {
		LOG.debug("unlocking; old locked by=" + weeklyTimecard.getLockedBy());
		weeklyTimecard.setLockedBy(null);
		weeklyTimecard.setUpdated(new Date());
		weeklyTimecard = getWeeklyTimecardDAO().merge(weeklyTimecard);

	}

	/**
	 * Find a timecard belonging to the current user, for the current week, in
	 * the current "View production" and "View project" (which may be null).
	 *
	 * @return A timecard matching the requirements, or null if none found.
	 */
	private WeeklyTimecard findCurrentWeekTimecard() {
		WeeklyTimecard thisWeek = null;
		Production prod;
		Project project;
		String currentUserAcc;
		prod = getViewProduction();
		if (prod != null) {
			currentUserAcc = getvUser().getAccountNumber();
			project = getViewProject();
			List<WeeklyTimecard> tcList = getWeeklyTimecardDAO().findByWeekEndDateAccount(prod,
					project, todaysWeekEndDate, TimecardRange.WEEK, currentUserAcc, null);
			for (WeeklyTimecard card : tcList) {
				// pick first card, at least; if > 1, pick another if it matches occupation
				if (thisWeek == null ||
						(position != null && position.equals(card.getOccupation()))) {
					thisWeek = card;
				}
			}
		}
		return thisWeek;
	}

	/**
	 * Create the List of TimecardEntry`s used on the "week-end dates" mobile
	 * page. Each TimecardEntry corresponds to a WeeklyTimecard associated with
	 * the currently logged-in User, in the currently selected Production. The
	 * TimecardEntry for the current week is marked "selected" -- this setting
	 * is used on the mobile page to graphically highlight the entry.
	 *
	 * @return The TimecardEntry that corresponds to the current (today's) week.
	 *         This is used by the code that implements the "enter today's time"
	 *         functionality.
	 */
	@Override
	protected WeeklyTimecard createWeeklyTimecardList() {
		return createMobileWeeklyTimecardList(false);
	}

	/**
	 * Create the List of TimecardEntry`s used on the "week-end dates" mobile
	 * page. Each TimecardEntry corresponds to a WeeklyTimecard associated with
	 * the currently logged-in User, in the currently selected Production, and, for
	 * Commercial productions, in the currently selected Project. The
	 * TimecardEntry for the current week is marked "selected" -- this setting
	 * is used on the mobile page to graphically highlight the entry.
	 *
	 * @param addMissing True if entries should be added for missing timecards.
	 *            This is used for the "Timecard List" page, but not for the
	 *            "Weekly Payroll" page.
	 * @return The TimecardEntry that corresponds to the current (today's) week.
	 *         This is used by the code that implements the "enter today's time"
	 *         functionality.
	 */
	protected WeeklyTimecard createMobileWeeklyTimecardList(boolean addMissing) {
		WeeklyTimecard thisWeek = null;
		try {
			thisWeek = null;
			// Get list of tcUser's timecards that are accessible to logged-in user:
			weeklyTimecardList = createWeeklyTimecardListVisible();

			// Now find & mark current week's entry
			for (WeeklyTimecard card : weeklyTimecardList) {
				if (todaysWeekEndDate.equals(card.getEndDate())) {
					card.setSelected(true); // used to highlight current week
					if (thisWeek == null ||
							(position != null && position.equals(card.getOccupation()))) {
						thisWeek = card;
					}
				}
				else {
					card.setSelected(false);
				}
			}

			if (addMissing && getViewProduction() != null && production != null && production.getStatus().equals(AccessStatus.ACTIVE)) {
				// Now determine dates for timecards we'll create if they're missing
				Date currWkEnd = TimecardUtils.calculateLastDayOfCurrentWeek();
				Date prior = null;
				Calendar cal = Calendar.getInstance();
				prior = TimecardUtils.calculatePriorWeekEndDate(currWkEnd);
				//int weekday = cal.get(Calendar.DAY_OF_WEEK);
				//if (weekday < Calendar.WEDNESDAY) { // it's early in week
				//	prior = TimecardUtils.calculatePriorWeekEndDate(currWkEnd); // may add create button for last week
				//}
				// Then check, for each valid StartForm, if those timecards exist
				Contact contact = ContactDAO.getInstance().findByUserProduction(tcUser, production);
				List<StartForm> sds = StartFormDAO.getInstance()
						.findByContactProject(contact, getCommProject(), getViewProduction().getAllowOnboarding());
				for (StartForm sd : sds) {
					if (sd.getAllowTimecardCreate()) {
						// determine latest week-end date that we should show timecards for
						Date lastDate = TimecardUtils.calculateLastNewDate(sd, null);
						while (currWkEnd.before(sd.getAnyStartDate())) {
							// haven't reached the Start's start-date yet.
							cal.setTime(currWkEnd);
							cal.add(Calendar.DAY_OF_MONTH, 7); // try a week later
							currWkEnd = cal.getTime();
							if (currWkEnd.after(lastDate)) {
								break;
							}
						}
						if (! currWkEnd.after(lastDate)) {
							// current week should be displayed, add if it's not there
							// ... and continue adding up to 5, or until reach last valid W/E date.
							for (int i=1; i <= 5; i++) {
								if (currWkEnd.after(lastDate)) {
									break;
								}
								checkAddEmptyTimecard(sd, currWkEnd);
								cal.setTime(currWkEnd);
								cal.add(Calendar.DAY_OF_MONTH, 7);
								currWkEnd = cal.getTime();
							}
						}
						if (prior != null && (! prior.after(lastDate)) && (! prior.before(sd.getAnyStartDate()))) {
							// prior week may be displayed, add if it's not there
							checkAddEmptyTimecard(sd, prior);
						}
					}
				}
			}
			Collections.sort(weeklyTimecardList, tceComparator);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		return thisWeek;
	}

	/**
	 * Create the List of Item`s used on the "Projects" list on the My Projects
	 * page. The list includes all projects for which the current user has a
	 * Start Form, or in which the user is an approver (project or department
	 * level) or time-keeper. The list is sorted in descending start-date order.
	 * It sets the {@link #projects} field.
	 */
	private void createProjectList() {
		boolean prodApprover = ApproverUtils.isProdApprover(getvContact());
		List<Project> projList = new ArrayList<>();
		if (prodApprover) {
			projList.addAll(getViewProduction().getProjects());
		}
		else if (ApproverUtils.isAnyApprover(getvContact())) {
			for (Project proj : getViewProduction().getProjects()) {
				if (ApproverUtils.isApprover(getvContact(), proj)) {
					projList.add(proj);
				}
			}
		}
		if (! prodApprover) {
			// For non-production approvers, add...
			// ...projects for which the contact has a StartForm...
			List<Project> sfProjects = ProjectDAO.getInstance().findByStartFormContact(getvContact());
			if (projList.size() == 0) {
				projList = sfProjects;
			}
			else {
				for (Project proj : sfProjects) {
					if (! projList.contains(proj)) { // skip ones already in the list
						projList.add(proj);
					}
				}
			}
			// ...then add any projects for which the contact is a time-keeper.
			for (Project proj : getViewProduction().getProjects()) {
				if (! projList.contains(proj)) { // skip ones already in the list
					if (ApproverUtils.isTimeKeeper(getvContact(), proj)) {
						projList.add(proj);
					}
				}
			}
		}
		Collections.sort(projList, getProjectComparator());
		projects = new ArrayList<>();
		for (Project proj : projList) {
			if (proj.getStatus()==AccessStatus.ACTIVE) {
				projects.add(new Item(proj.getId(), proj.getTitle()));
			}
		}
	}

	/**
	 * @return The comparator used for sorting the Project list on the
	 *         "My Projects" mobile page. See {@link #projectComparator}.
	 */
	private static Comparator<Project> getProjectComparator() {
		return projectComparator;
	}

	/**
	 * The comparator used for sorting the list on the "My Projects" mobile
	 * page. This is used in conjunction with the Collections.sorts method to
	 * sort the Projects by start date, in descending order.
	 */
	private static Comparator<Project> projectComparator = new Comparator<Project>() {
		@Override
		public int compare(Project c1, Project c2) {
			return c1.compareTo(c2, Project.SORTKEY_START, false);
		}
	};


	/**
	 * See if our timecard list contains a timecard for the given date and
	 * occupation (Start form). If not, add an entry to the list which will
	 * generate a "Create" button for it on the mobile timecard list.
	 *
	 * @param sd The StartForm that generated the timecard.
	 * @param date The date of interest.
	 */
	private void checkAddEmptyTimecard(StartForm sd, Date date) {
		int id = sd.getId();
		boolean missing = true;
		for (WeeklyTimecard card : weeklyTimecardList) {
			if (card.getEndDate().equals(date) && card.getStartForm() != null &&
					card.getStartForm().getId() == id) {
				missing = false; // specified week-end date exists
				break;
			}
		}
		if (missing) { // the required timecard was not in the list; add it.
			WeeklyTimecard wtc = new WeeklyTimecard();
			wtc.setEndDate(date);
			wtc.setId(-id);	// JSP uses negative value as flag!
			// TODO use null status instead?
			wtc.setOccupation(sd.getJobClass());
			weeklyTimecardList.add(wtc);
		}
	}

	/**
	 * Sort comparator used for the Timecard List page.
	 */
	private static Comparator<WeeklyTimecard> tceComparator = new Comparator<WeeklyTimecard>() {
		@Override
		public int compare(WeeklyTimecard one, WeeklyTimecard two) {
			return one.compareTo(two, WeeklyTimecard.SORTKEY_DATE_JOB);
		}
	};

	/**
	 * Determine the proper settings of the "next" and "previous" flags for the
	 * week-ending-date list. These are used to enable and disable the "next"
	 * and "previous" buttons appropriately.
	 */
	private void calculateWeekFlags() {
		try {
			hasNextWeek = false;
			hasPrevWeek = false;
			if (getWeeklyTimecardList().size() < 2) {
				return;
			}
			int i = findWeekEndEntry();
			if (i < 0) { // couldn't find the week entry
				return;
			}
			// remember list is in reverse date order!
			if (i < getWeeklyTimecardList().size()-1) {
				hasPrevWeek = true;
			}
			if (i > 0) {
				hasNextWeek = true;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Find the entry in our weeklyTimecardList that matches the current
	 * timecard (weeklyTimecard field) and return its index.
	 *
	 * @return The index (origin 0) of the entry in weeklyTimecardList that is
	 *         identical to our weeklyTimecard field.  If not found, the index
	 *         value will be equal to the size of the list, i.e., out of
	 *         range.
	 */
	private int findWeekEndEntry() {
		if (weeklyTimecard == null || weeklyTimecard.getId() == null) {
			return -1;
		}
		int i = 0;
		for (WeeklyTimecard wtc : getWeeklyTimecardList()) {
			if (wtc != null && wtc.getId() != null &&
					wtc.getId().equals(weeklyTimecard.getId())) {
				break;
			}
			i++;
		}
		return i;
	}

	/**
	 * Determine the proper settings of the "next" and "previous" flags for the
	 * day list (within one timecard). These are used to enable and disable the "next"
	 * and "previous" buttons appropriately.
	 */
	private void calculateDayFlags() {
		hasNextDay = true;
		hasPrevDay = true;
		tzCalendar.setTime(currentDate);
		int wkday = tzCalendar.get(Calendar.DAY_OF_WEEK);
		if (wkday == getWeekEndDay()) {
			hasNextDay = false;
		}
		else if (wkday == getWeekStartDay()) {
			hasPrevDay = false;
		}
	}

	/**
	 * @return True if the current user has any timecard with a week-ending date
	 *         for this (the current) week.
	 */
	private boolean existsThisWeeksTimecard() {
		boolean bRet = false;
		Date weDate = TimecardUtils.calculateLastDayOfCurrentWeek();
		bRet = getWeeklyTimecardDAO().existsWeekEndDateAccount(weDate, getvUser().getAccountNumber());
		return bRet;
	}

	/**
	 * The Mobile timecard list will include special entries for non-existing timecards where
	 * we want to provide a "Create" button for the mobile user.
	 * @see com.lightspeedeps.web.timecard.TimecardEmpSelectBase#getPaddedWeeklyTimecardList()
	 */
	@Override
	public List<WeeklyTimecard> getPaddedWeeklyTimecardList() {
		if (weeklyTimecardList == null) {
			createMobileWeeklyTimecardList(true);
		}
		return weeklyTimecardList;
	}

	/** See {@link #currentDate}. */
	public Date getCurrentDate() {
		return currentDate;
	}
	/** See {@link #currentDate}. */
	public void setCurrentDate(Date currentDate) {
		this.currentDate = currentDate;
	}

	/**
	 * @return the appropriate DailyTime entry from the current WeeklyTimecard,
	 *         based on the value of the {@link #currentDate} field, which keeps track
	 *         of which day we should be displaying and/or updating.
	 */
	public DailyTime getDailyTime() {
		DailyTime dt = null;
		try {
			if (weeklyTimecard != null) {
				tzCalendar.setTime(currentDate);
				int daynum = tzCalendar.get(Calendar.DAY_OF_WEEK);
				if (getWeekEndDay() != Calendar.SATURDAY) {
					daynum -= getWeekEndDay();
					if (daynum <= 0) {
						daynum += 7;
					}
				}
				dt = weeklyTimecard.getDailyTimes().get(daynum-1);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return dt;
	}

	/** See {@link #weekEndDay}. */
	@Override
	public Integer getWeekEndDay() {
		if (weekEndDay == null) {
			weekEndDay = Calendar.SATURDAY;
			if (weeklyTimecard != null) {
				weekEndDay = weeklyTimecard.getWeekEndDay();
			}
			else {
				weekEndDay = TimecardUtils.findWeekEndDay(production, getViewProject());
			}
		}
		return weekEndDay;
	}

	/** See {@link #weekStartDay}. */
	public Integer getWeekStartDay() {
		if (weekStartDay == null) {
			weekStartDay = getWeekEndDay() - 6;
			if (weekStartDay < 1) {
				weekStartDay += 7;
			}
		}
		return weekStartDay;
	}
	/** See {@link #weekStartDay}. */
	public void setWeekStartDay(Integer weekStartDay) {
		this.weekStartDay = weekStartDay;
	}

	/**
	 * A method designed to be called by a JSP reference to test and set the
	 * lock on the current weeklyTimecard.
	 *
	 * @return True iff the lock is held -- either the weeklyTimecard was
	 *         already locked, or it has been successfully locked. False
	 *         indicates we were unable to lock it, presumably because another
	 *         user already has it locked.
	 */
	public boolean getLockIt() {
		if (weeklyTimecard != null && weeklyTimecard.getLockedBy() == null) {
			getWeeklyTimecardDAO().lock(weeklyTimecard, user);
			calculateRoundingType();
		}
		if (! getLocked()) {
			// if JSP tries to lock (by testing lockIt), and fails,
			// then prevent edit-raw access:
			editRaw = false;
		}
		else {
			forceLazyInit();
		}
		return false;
	}

	/**
	 * @return True iff the current weeklyTimecard is already locked by the
	 *         current user.
	 */
	public boolean getLocked() {
		if (weeklyTimecard != null && weeklyTimecard.getLockedBy() != null
				&& weeklyTimecard.getLockedBy().equals(user.getId())) {
			return true;
		}
		return false;
	}

	/**
	 * @return Either the title of the Production, or, for
	 * Commercials, the title of the current Project.
	 */
	public String getProdOrProjectTitle() {
		String str = null;
		if (production != null) {
			if (production.getType().hasPayrollByProject() &&
					getViewProject() != null) {
				str = getViewProject().getTitle();
			}
			else {
				str = production.getTitle();
			}
		}
		return str;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #productionId}. */
	public Integer getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(Integer productionId) {
		this.productionId = productionId;
	}

	/** See {@link #projectId}. */
	public Integer getProjectId() {
		return projectId;
	}
	/** See {@link #projectId}. */
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	/** See {@link #projects}. */
	public List<Item> getProjects() {
		if (projects == null) {
			createProjectList();
		}
		return projects;
	}
	/** See {@link #projects}. */
	public void setProjects(List<Item> projects) {
		this.projects = projects;
	}

	/** See {@link #position}. */
	public String getPosition() {
		return position;
	}
	/** See {@link #position}. */
	public void setPosition(String position) {
		this.position = position;
	}

	/** See {@link #showBoxRental}. */
	public boolean getShowBoxRental() {
		return showBoxRental;
	}

	/** See {@link #showExpenses}. */
	public boolean getShowExpenses() {
		return showExpenses;
	}

	/** See {@link #showMileage}. */
	public boolean getShowMileage() {
		return showMileage;
	}

	/** See {@link #showStartInfo}. */
	public boolean getShowStartInfo() {
		return showStartInfo;
	}

	/** See {@link #showEditSsn}. */
	public boolean getShowEditSsn() {
		return showEditSsn;
	}

	/** See {@link #showMoreDaily}. */
	public boolean getShowMoreDaily() {
		return showMoreDaily;
	}

	/** See {@link #startInfoRequired}. */
	public boolean getStartInfoRequired() {
		return startInfoRequired;
	}

	/** See {@link #startInfoChanged}. */
	public boolean getStartInfoChanged() {
		startInfoChanged = ! (
				StringUtils.compare(saveWorkCity, weeklyTimecard.getCityWorked()) == 0 &&
				StringUtils.compare(saveWorkState, weeklyTimecard.getStateWorked()) == 0 &&
				StringUtils.compare(saveSSN, weeklyTimecard.getSocialSecurity()) == 0
				);
		if (!startInfoChanged && getTeamPayroll()) {
			startInfoChanged = ! (savePaidAs != null && savePaidAs.equals(weeklyTimecard.getPaidAs()));
			if (savePaidAs != null && savePaidAs.isLoanOut() && !startInfoChanged) {
				startInfoChanged = ! (StringUtils.compare(saveFederalCorpId, weeklyTimecard.getFedCorpId()) == 0);
			}
		}
		return startInfoChanged;
	}

	/** See {@link #showDays}. */
	public boolean getShowDays() {
		return showDays;
	}
	/** See {@link #showDays}. */
	public void setShowDays(boolean showDays) {
		this.showDays = showDays;
	}

	/** See {@link #showComment}. */
	public boolean getShowComment() {
		return showComment;
	}
	/** See {@link #showComment}. */
	public void setShowComment(boolean showComment) {
		this.showComment = showComment;
	}

	/** See {@link #editMileage}. */
	public boolean getEditMileage() {
		return editMileage;
	}
	/** See {@link #editMileage}. */
	public void setEditMileage(boolean editMileage) {
		this.editMileage = editMileage;
	}

	/**
	 * Find the mileageLine currently displayed on the Mileage Form page.
	 * <p>
	 * See {@link #mileageLine}.
	 */
	public MileageLine getMileageLine() {
		if (mileageLine == null) {
			if (weeklyTimecard.getMileage() != null) {
				int id = SessionUtils.getInteger(Constants.ATTR_MTC_MILEAGE_ID, -1);
				for (MileageLine ml : weeklyTimecard.getMileage().getMileageLines()) {
					if (ml.getId().equals(id)) {
						mileageLine = ml;
						break;
					}
				}
			}
		}
		return mileageLine;
	}
	/** See {@link #mileageLine}. */
	public void setMileageLine(MileageLine mileageLine) {
		this.mileageLine = mileageLine;
	}

	/**
	 * Find the payExpense line item currently displayed on the Weekly Expenses
	 * page.
	 * <p>
	 * See {@link #payExpense}.
	 */
	public PayExpense getPayExpense() {
		if (payExpense == null) {
			if (weeklyTimecard.getExpenseLines() != null) {
				int id = SessionUtils.getInteger(Constants.ATTR_MTC_EXPENSE_ID, -1);
				for (PayExpense pe : weeklyTimecard.getExpenseLines()) {
					if (pe.getId().equals(id)) {
						payExpense = pe;
						expCategory = pe.getCategoryType();
						break;
					}
				}
			}
		}
		return payExpense;
	}
	/** See {@link #payExpense}. */
	public void setPayExpense(PayExpense payExpense) {
		this.payExpense = payExpense;
	}

	/**
	 * @see com.lightspeedeps.web.timecard.TimecardBase#getExpCategory()
	 */
	@Override
	public PayCategory getExpCategory() {
		getPayExpense();
		return expCategory;
	}

	/** See {@link #pin}. */
	public String getPin() {
		return pin;
	}
	/** See {@link #pin}. */
	public void setPin(String pin) {
		this.pin = pin;
	}

	/** See {@link #password}. */
	public String getPassword() {
		return password;
	}
	/** See {@link #password}. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** See {@link #submitType}. */
	public TimecardSubmitType getSubmitType() {
		return submitType;
	}
	/** See {@link #submitType}. */
	public void setSubmitType(TimecardSubmitType submitType) {
		this.submitType = submitType;
	}

	/** The drop-down list for the "submit reason", displayed
	 * when a user is submitting a timecard on behalf of someone else. */
	public List<SelectItem> getSubmitTypeDL() {
		if (getWeeklyTimecard().getUnderContract()) {
			return TimecardSubmitType.getTypeContractSelectList();
		}
		return TimecardSubmitType.getTypeDefaultSelectList();
	}

	/** See {@link #submitComment}. */
	public String getSubmitComment() {
		return submitComment;
	}
	/** See {@link #submitComment}. */
	public void setSubmitComment(String submitComment) {
		this.submitComment = submitComment;
	}

	/** See {@link #hasTimecard}. */
	public Boolean getHasTimecard() {
		if (hasTimecard == null) {
			hasTimecard = existsThisWeeksTimecard();
		}
		return hasTimecard;
	}
	/** See {@link #hasTimecard}. */
	public void setHasTimecard(Boolean hasTimecard) {
		this.hasTimecard = hasTimecard;
	}

	/** See {@link #hasNextDay}. */
	public Boolean getHasNextDay() {
		if (hasNextDay == null) {
			calculateDayFlags();
		}
		return hasNextDay;
	}
	/** See {@link #hasNextDay}. */
	public void setHasNextDay(Boolean hasNextDay) {
		this.hasNextDay = hasNextDay;
	}

	/** See {@link #hasPrevDay}. */
	public Boolean getHasPrevDay() {
		if (hasPrevDay == null) {
			calculateDayFlags();
		}
		return hasPrevDay;
	}
	/** See {@link #hasPrevDay}. */
	public void setHasPrevDay(Boolean hasPrevDay) {
		this.hasPrevDay = hasPrevDay;
	}

	/** See {@link #hasNextWeek}. */
	public Boolean getHasNextWeek() {
		if (hasNextWeek == null) {
			calculateWeekFlags();
		}
		return hasNextWeek;
	}
	/** See {@link #hasNextWeek}. */
	public void setHasNextWeek(Boolean hasNextWeek) {
		this.hasNextWeek = hasNextWeek;
	}

	/** See {@link #hasPrevWeek}. */
	public Boolean getHasPrevWeek() {
		if (hasPrevWeek == null) {
			calculateWeekFlags();
		}
		return hasPrevWeek;
	}
	/** See {@link #hasPrevWeek}. */
	public void setHasPrevWeek(Boolean hasPrevWeek) {
		this.hasPrevWeek = hasPrevWeek;
	}

	/** See {@link #todaysWeekEndDate}. */
	public Date getTodaysWeekEndDate() {
		return todaysWeekEndDate;
	}
	/** See {@link #todaysWeekEndDate}. */
	public void setTodaysWeekEndDate(Date todaysWeekEndDate) {
		this.todaysWeekEndDate = todaysWeekEndDate;
	}

	/**
	 * Invoke the base class' preDestroy method.
	 * @see com.lightspeedeps.web.timecard.TimecardBase#preDestroy()
	 */
	@Override
	@PreDestroy
	public void preDestroy() {
		LOG.debug("");
		super.preDestroy();
	}

}
