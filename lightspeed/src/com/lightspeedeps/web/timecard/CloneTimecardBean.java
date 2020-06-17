/**
 * CloneTimecardBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This is the backing bean for the "Clone Timecards" mobile page and the
 * corresponding pop-up dialog for the desktop. It includes the functions of
 * prompting for and retrieving the user's selections of days, recipients, and
 * cloning options; and then performing the cloning operation.
 */
@ManagedBean
@ViewScoped
public class CloneTimecardBean extends PopupBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(CloneTimecardBean.class);

	/** FIELDS */

	/** The timecard that will be the source for the cloning operation. */
	private WeeklyTimecard weeklyTimecard;

	/** The week-ending date of the source timecard. */
	private Date weekEndingDate;

	/** The Production containing both the source and target timecards. */
	private final Production production = SessionUtils.getProduction();

	/** The current Project, which contains both the source and target timecards.
	 * Only used for Commercial productions. For TV & Feature productions, this
	 * will be null. */
	private Project project;

	/** Array of day names used for column headers on Timecard Review page. This is
	 * typically 'Sun' through 'Sat', but will change if the Project has set the
	 * 'Start of week' preference to something other than Sunday. */
	private String[] timecardDays;

	/** Clone: select all days checkbox value. */
	private boolean selectAllDays;

	/** Clone: select all recipient's timecards checkbox value. */
	private boolean selectAllTargets;

	/** Clone: individual daily flags, 0=Sunday, for check boxes. */
	private boolean[] selectDay;

	/** Copy the "Set" field when cloning (checkbox value). */
	private boolean cloneCopySet;

	/** Overwrite existing hour fields when cloning (checkbox value). */
	private boolean cloneOverwriteTimes;

	/** Set to True after the clone operation has been done, whether or
	 * not some timecards had errors. */
	private boolean done;
	private boolean errors;

	/** The number of successfully cloned timecards. */
	private int cloned;

	/** The list of (possible) recipient timecards presented to the user,
	 * from which they select the timecards that will be cloning targets. */
	private List<TimecardEntry> recipients;

	/** The (possibly empty) list of WeeklyTimecard.id values of timecards
	 * which were NOT cloned. */
	private List<Integer> errorIds;

	private List<TimecardEntry> errorEntries;

	WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();

	/**
	 * default constructor.
	 */
	public CloneTimecardBean() {
		log.debug("");

		if (production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
	}

	public static CloneTimecardBean getInstance() {
		return (CloneTimecardBean)ServiceFinder.findBean("cloneTimecardBean");
	}

	/**
	 * Initialize all our fields. It's necessary to do this each time the dialog
	 * box is displayed on the desktop since the bean is not re-instantiated for
	 * each display (as it is on mobile).
	 */
	private void init() {
		recipients = null;
		selectAllDays = false;
		selectAllTargets = false;
		cloneCopySet = false;
		cloneOverwriteTimes = true;
		done = false;
		errors = false;
		cloned = 0;
		selectDay = new boolean[7];
		timecardDays = null;
		errorIds = new ArrayList<>();
		errorEntries = new ArrayList<>();
		weekEndingDate = TimecardUtils.calculateLastDayOfCurrentWeek();

		Integer id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
		if (id != null) { // Have a specific time card to copy
			setWeeklyTimecard(WeeklyTimecardDAO.getInstance().findById(id));
			if (weeklyTimecard != null) {
				weekEndingDate = weeklyTimecard.getEndDate();
				if (! weeklyTimecard.getProdId().equals(production.getProdId())) {
					// User is in a production, but the timecard Id from the session doesn't match this production
					weeklyTimecard = null;
					SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
				}
			}
		}
		SessionUtils.put(Constants.ATTR_TC_CLONE_ERRORS, null);
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#show(com.lightspeedeps.web.popup.PopupHolder, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void show(PopupHolder holder, int act, String titleId, String buttonOkId, String buttonCancelId) {
		init();
		super.show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/**
	 * The action method for the "Cancel" button on the mobile Clone Timecards
	 * page.
	 *
	 * @return navigation string to the Weekly Payroll page.
	 */
	public String actionCancelMobileClone() {
		weeklyTimecardDAO.unlock(weeklyTimecard, SessionUtils.getCurrentUser().getId());
		return "pickdaym";
	}

	/**
	 * The action method for the "Clone" button on the mobile Clone Timecards
	 * page.
	 *
	 * @return navigation string to the "Clone results" page.
	 */
	public String actionMobileClone() {
		if (doClone()) {
			errorEntries.clear(); // not used for mobile
			return "clonedm"; // mobile "clone results" page
		}
		return null;
	}

	/**
	 * The action method for the "Clone" button on the desktop's
	 * "Clone Timecard" dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionClone() {
		if (doClone()) {
			done = true;
			if (errorIds.size() != 0) {
				errors = true;
			}
		}
		return null;
	}

	/**
	 * Perform the clone operation.
	 *
	 * @return True if the clone operation was at least attempted. False if
	 *         there was some user error regarding the selections made, e.g., no
	 *         days were selected to be copied. In this case, a Faces message
	 *         has already been issued.
	 */
	private boolean doClone() {
		if (! checkDays()) {
			MsgUtils.addFacesMessage("Timecard.Clone.Nodays", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		if (! checkRecipients() ) {
			MsgUtils.addFacesMessage("Timecard.Clone.Norecipients", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		boolean success;
		User user = SessionUtils.getCurrentUser();
		for (TimecardEntry tce : recipients) {
			if (tce.getChecked()) {
				WeeklyTimecard wtc = tce.getWeeklyTc();
				wtc = weeklyTimecardDAO.refresh(wtc);
				if (weeklyTimecardDAO.lock(wtc, user)) {
					success = copyData(weeklyTimecard, wtc, selectDay);
					if (! success) {
						weeklyTimecardDAO.evict(wtc); // seemed necessary; just 'refresh' wasn't enough
						wtc = weeklyTimecardDAO.refresh(wtc);
					}
					else {
						cloned++;
					}
					weeklyTimecardDAO.unlock(wtc, user.getId());
				}
				else {
					success = false;
				}
				if (! success) {
					errorIds.add(wtc.getId());
					errorEntries.add(tce);
					log.debug("clone skipped, timecard locked, target=" + wtc.toString());
				}
			}
		}
		SessionUtils.put(Constants.ATTR_TC_CLONE_ERRORS, errorIds);
		SessionUtils.put(Constants.ATTR_TC_CLONE_COUNT, cloned);
		return true;
	}

	/**
	 * Copies the cloned data from the source timecard to one target timecard,
	 * for the days indicated.
	 *
	 * @param wtcIn The input (source) timecard.
	 * @param wtcOut The output (target) timecard. This should already be
	 *            locked.
	 * @param selectDay The array of booleans indicating which days should be
	 *            copied. The 0th entry is for Sunday.
	 */
	private boolean copyData(WeeklyTimecard wtcIn, WeeklyTimecard wtcOut, boolean[] selectDay) {
		List<DailyTime> dailyIn = wtcIn.getDailyTimes();
		List<DailyTime> dailyOut = wtcOut.getDailyTimes();
		boolean success = true;

		for (int day = 0; day < 7; day++) {
			if (selectDay[day]) {
				DailyTime out = dailyOut.get(day);
				if (! out.getNoStartForm()) {
					DailyTime in = dailyIn.get(day);
					// "On-Call" times should always get recalculated
					out.setOnCallStart(null);
					out.setOnCallEnd(null);
					if (wtcOut.getAllowWorked()) {
						// target timecard has "allow worked" option
						if (cloneOverwriteTimes || out.getHours() == null) {
							// Ok to change target timecard...
							out.setWorked(false);
							out.setWorkZone(in.getWorkZone());
							out.setDayType(in.getDayType());
							out.setHours(null);
							out.setCallTime(null);
							out.setM1Out(null);
							out.setM1In(null);
							out.setM2Out(null);
							out.setM2In(null);
							out.setWrap(null);
							out.setMpvUser(null);
							if (in.reportedWork()) {
								// source timecard either checked "worked", or reported non-zero hours
								out.setWorked(true);
							}
						}
					}
					else { // Target timecard does NOT have "allow worked" option
						if (cloneOverwriteTimes || out.getDayType() == null) {
							out.setWorkZone(in.getWorkZone());
							out.setDayType(in.getDayType());
						}
						if (wtcIn.getAllowWorked() && in.getWorked()) {
							// do not make up hours where none exist.
						}
						else {
							if (cloneOverwriteTimes || out.getCallTime() == null) {
								out.setCallTime(in.getCallTime());
							}
							if (cloneOverwriteTimes || out.getM1Out() == null) {
								out.setM1Out(in.getM1Out());
							}
							if (cloneOverwriteTimes || out.getM1In() == null) {
								out.setM1In(in.getM1In());
							}
							if (cloneOverwriteTimes || out.getM2Out() == null) {
								out.setM2Out(in.getM2Out());
							}
							if (cloneOverwriteTimes || out.getM2In() == null) {
								out.setM2In(in.getM2In());
							}
							if (cloneOverwriteTimes || out.getWrap() == null) {
								out.setWrap(in.getWrap());
							}
							// Don't copy On-Call Start/End, they will be recomputed by Fill Jobs.
							if (cloneOverwriteTimes || StringUtils.isEmpty(out.getMpvUser())) {
								out.setMpvUser(in.getMpvUser());
							}
							if (cloneOverwriteTimes) {
								out.setNonDeductMeal(in.getNonDeductMeal());
								out.setNonDeductMeal2(in.getNonDeductMeal2());
							}
							if (cloneOverwriteTimes || out.getGrace1() == null) {
								out.setGrace1(in.getGrace1());
							}
							if (cloneOverwriteTimes || out.getGrace2() == null) {
								out.setGrace2(in.getGrace2());
							}
						}
					}
					if (cloneCopySet) {
						out.setAccountSet(in.getAccountSet());
					}
				}
				// Validate data, then calculate the hours of work for the changed day
				if (! TimecardCheck.validateAndCalcWorkDay(out, false)) {
					success = false;
					break;
				}
			}
		}
		if (success) { // Ok, update weekly totals and save the changes
			TimecardCalc.calculateWeeklyTotals(wtcOut);
			weeklyTimecardDAO.attachDirty(wtcOut);
		}
		return success;
	}

	/**
	 * @return True iff at least one recipient has been selected.
	 */
	private boolean checkRecipients() {
		for (TimecardEntry tce : recipients) {
			if (tce.getChecked()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return True iff at least one day has been selected.
	 */
	private boolean checkDays() {
		for (int i = 0; i < 7; i++) {
			if (selectDay[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates the list of timecards which may be targets of the clone
	 * operation.
	 *
	 * @return A (possibly empty) List of TimecardEntry`s, where each one
	 *         represents a WeeklyTimecard that the current user has either
	 *         approval authority or "timeKeeper" authority for.  The list
	 *         does NOT include the "current" (clone source) timecard.
	 */
	private List<TimecardEntry> createRecipientList() {

		if (weeklyTimecard == null) {
			return new ArrayList<>();
		}

		Contact contact = SessionUtils.getCurrentContact();
		String acct = null; // no filtering by recipients' account number

		// Ignore edit/view HTG permission when creating timecard list
		// ...except if pseudo-approver flag is on.
		boolean viewAll = AuthorizationBean.getInstance().getPseudoApprover();
		recipients = TimecardUtils.createTimecardListAllDepts(production, project, weekEndingDate, false,
				true, null, null, acct, contact, viewAll, true);

		// Remove submitted timecards and the clone source timecard from the list
		Integer id = weeklyTimecard.getId();
		for (Iterator<TimecardEntry> iter = recipients.iterator(); iter.hasNext(); ) {
			TimecardEntry tce = iter.next();
			if (tce.getWeeklyTc().getId().equals(id)) {
				iter.remove();
			}
			else if (! tce.getWeeklyTc().getSubmitable()) {
				// TC submitted -- remove from cloning target list
				iter.remove();
			}
		}
		Collections.sort(recipients, TimecardEntry.getNameComparator());

		return 	recipients;
	}

	/**
	 * ValueChangeListener method for the "Select All Days" checkbox on the Clone
	 * Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectAllDays(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			for (int i = 0; i < 7; i++) {
				selectDay[i] = checked;
			}
		}
	}

	/**
	 * ValueChangeListener method for all of the "select a day" checkboxes on the
	 * Clone Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectDay(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			if (! checked) {
				setSelectAllDays(false);
			}
		}
	}

	/**
	 * ValueChangeListener method for the "Select All Recipients" checkbox on the
	 * Clone Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectAllTargets(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			for (TimecardEntry tce : recipients) {
				tce.setChecked(checked);
			}
		}
	}

	/**
	 * ValueChangeListener method for all of the recipient selection checkboxes
	 * on the Clone Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectTarget(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			if (! checked) {
				setSelectAllTargets(false);
			}
		}
	}

	/**See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/**See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #recipients}. Note that this method provides
	 * for lazy initialization of the list. */
	public List<TimecardEntry> getRecipients() {
		if (recipients == null) {
			recipients = createRecipientList();
		}
		return recipients;
	}
	/**See {@link #recipients}. */
	public void setRecipients(List<TimecardEntry> recipients) {
		this.recipients = recipients;
	}

	/**See {@link #errorEntries}. */
	public List<TimecardEntry> getErrorEntries() {
		return errorEntries;
	}

	/**See {@link #errorEntries}. */
	public void setErrorEntries(List<TimecardEntry> errorEntry) {
		errorEntries = errorEntry;
	}

	/**See {@link #selectAllTargets}. */
	public boolean getSelectAllTargets() {
		return selectAllTargets;
	}
	/**See {@link #selectAllTargets}. */
	public void setSelectAllTargets(boolean selectAllTargets) {
		this.selectAllTargets = selectAllTargets;
	}

	/**See {@link #selectAllDays}. */
	public boolean getSelectAllDays() {
		if (selectDay == null) { // referenced by JSP to force initialization
			init();
		}
		return selectAllDays;
	}
	/**See {@link #selectAllDays}. */
	public void setSelectAllDays(boolean selectAllDays) {
		this.selectAllDays = selectAllDays;
	}

	/**See {@link #selectDay}. */
	public boolean[] getSelectDay() {
		if (selectDay == null) {
			init();
		}
		return selectDay;
	}

	/** See {@link #timecardDays}. */
	public String[] getTimecardDays() {
		if (timecardDays == null) {
			if (SessionUtils.isMobileUser()) {
				timecardDays = TimecardUtils.createTimecardDayLongLabels(TimecardUtils.findWeekEndDay());
			}
			else {
				timecardDays = TimecardUtils.createTimecardDayLabels();
			}
		}
		return timecardDays;
	}

	/**See {@link #cloneCopySet}. */
	public boolean getCloneCopySet() {
		return cloneCopySet;
	}
	/**See {@link #cloneCopySet}. */
	public void setCloneCopySet(boolean cloneCopySet) {
		this.cloneCopySet = cloneCopySet;
	}

	/**See {@link #cloneOverwriteTimes}. */
	public boolean getCloneOverwriteTimes() {
		return cloneOverwriteTimes;
	}
	/**See {@link #cloneOverwriteTimes}. */
	public void setCloneOverwriteTimes(boolean cloneOverwriteTimes) {
		this.cloneOverwriteTimes = cloneOverwriteTimes;
	}

	/**See {@link #done}. */
	public boolean getDone() {
		return done;
	}
	/**See {@link #done}. */
	public void setDone(boolean done) {
		this.done = done;
	}

	/**See {@link #cloned}. */
	public int getCloned() {
		return cloned;
	}
	/**See {@link #cloned}. */
	public void setCloned(int cloned) {
		this.cloned = cloned;
	}

	/**See {@link #errors}. */
	public boolean getErrors() {
		return errors;
	}
	/**See {@link #errors}. */
	public void setErrors(boolean errors) {
		this.errors = errors;
	}

}
