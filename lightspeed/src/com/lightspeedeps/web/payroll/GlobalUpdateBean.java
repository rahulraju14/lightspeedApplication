/**
 * GlobalUpdateBean.java
 */
package com.lightspeedeps.web.payroll;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.ProductionPhase;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This is the backing bean for the "Globally Update Timecards" pop-up dialog for the desktop. It
 * includes the functions of prompting for and retrieving the user's options; and then performing
 * the update operation.
 */
@ManagedBean
@ViewScoped
public class GlobalUpdateBean extends PopupBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(GlobalUpdateBean.class);

	/** FIELDS */

	/** The timecard that will be the source for the cloning operation. */
	private WeeklyTimecard weeklyTimecard;

	/** The week-ending date of the source timecard. */
	private Date weekEndDate;

	/** The Production containing both the source and target timecards. */
	private Production production = SessionUtils.getProduction();

	/**
	 * For a Commercial project, the current session's project. For TV/Feature, this is left null.
	 */
	private Project project;

	/**
	 * True iff we should overwrite existing (non-null/blank) data in timecards when we are updating
	 * them.
	 */
	private boolean overwriteData;

	/** True iff we should include Off-Production timecards when updating. */
	private boolean includeOffProduction;

	/** True if the episode code for Job #1 should be copied (it's non-blank). */
	private boolean copyEpicode1;
	/** True if the episode code for Job #2 should be copied (it's non-blank). */
	private boolean copyEpicode2;
	/** True if the episode code for Job #3 should be copied (it's non-blank). */
	private boolean copyEpicode3;
	/** True if the episode code for Job #4 should be copied (it's non-blank). */
	private boolean copyEpicode4;

	/** The episode code (accountMajor) value to put into Job #1 of target timecards. */
	private String epiCode1;
	/** The episode code (accountMajor) value to put into Job #2 of target timecards. */
	private String epiCode2;
	/** The episode code (accountMajor) value to put into Job #3 of target timecards. */
	private String epiCode3;
	/** The episode code (accountMajor) value to put into Job #4 of target timecards. */
	private String epiCode4;

	/**
	 * Set to True after the update operation has been done, whether or not some timecards had
	 * errors.
	 */
	private boolean done;

	/** Set to True if errors were encountered during the update process. */
	private boolean errors;

	/** Department to update timecards for. If 'All', update all departments. */
	private Integer deptId;

	/** The number of successfully updated timecards. */
	private int updated;

	/**
	 * The (possibly empty) list of WeeklyTimecard.id values of timecards which were NOT updated.
	 */
	private List<Integer> errorIds;

	private List<WeeklyTimecard> errorEntries;

	WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();

	/** List of departments for the dropdown */
	private List<SelectItem> departmentsDL;

	/**
	 * default constructor.
	 */
	public GlobalUpdateBean() {
		if (production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
	}

	public static GlobalUpdateBean getInstance() {
		return (GlobalUpdateBean) ServiceFinder.findBean("globalUpdateBean");
	}

	/**
	 * Initialize all our fields. It's necessary to do this each time the dialog box is displayed on
	 * the desktop since the bean is not re-instantiated for each display (as it is on mobile).
	 */
	private void init() {
		production = SessionUtils.getNonSystemProduction();
		weekEndDate = weeklyTimecard.getEndDate();
		overwriteData = false;
		includeOffProduction = false;
		done = false;
		errors = false;
		updated = 0;
		errorIds = new ArrayList<>();
		errorEntries = new ArrayList<>();
		departmentsDL = null;
		deptId = 0;
	}

	/**
	 * Method to use to display the Global Update dialog box.
	 *
	 * @param holder The caller of this popup.
	 * @param act The action code, which may be used in callbacks to the caller.
	 * @param prefix The message id prefix for looking up the title, message text, and button text
	 *            of the dialog box.
	 * @param timecard The WeeklyTimecard that holds the user's data items to be applied globally.
	 */
	public void show(PopupHolder holder, int act, String prefix, WeeklyTimecard timecard) {
		weeklyTimecard = timecard;
		init();
		super.show(holder, act, prefix + "Title", null, prefix + "Ok", "Confirm.Cancel");
	}

	/**
	 * The action method for the "Update" button on the desktop's "Update Timecard" dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionUpdate() {
		if (doUpdate()) {
			done = true;
			if (errorIds.size() != 0) {
				errors = true;
			}
		}
		return null;
	}

	/**
	 * Perform the update operation.
	 *
	 * @return True if the update operation was at least attempted.
	 */
	private boolean doUpdate() {
		boolean success;
		Contact contact = SessionUtils.getCurrentContact();
		List<WeeklyTimecard> timecards;

		if(deptId == 0) {
			timecards = WeeklyTimecardDAO.getInstance().findByWeekEndDate(production, project, weekEndDate, null);
		}
		else {
			timecards = weeklyTimecardDAO.findByWeekEndDept(production, project, weekEndDate, deptId, null);
		}

		epiCode1 = weeklyTimecard.getAccountMajor();
		epiCode2 = weeklyTimecard.getAccountFree();
		epiCode3 = weeklyTimecard.getAccountFree2();
		epiCode4 = weeklyTimecard.getAccountSub();

		copyEpicode1 = ! StringUtils.isEmpty(epiCode1);
		copyEpicode2 = ! StringUtils.isEmpty(epiCode2);
		copyEpicode3 = ! StringUtils.isEmpty(epiCode3);
		copyEpicode4 = ! StringUtils.isEmpty(epiCode4);

		// Filter the timecards, removing approved ones that do not qualify.
		// Approved TCs must be in this person's queue, or below them in the
		// queue.
		for (Iterator<WeeklyTimecard> iter = timecards.iterator(); iter.hasNext();) {
			WeeklyTimecard wtc = iter.next();
			if (!includeOffProduction && wtc.getOffProduction()) {
				iter.remove(); // we're not processing Off-Production timecards
				continue;
			}
			if (wtc.getSubmitable()) { // Available for Submit...
				// = un-submitted timecard or rejected back to employee = OK to
				// update
				continue;
			}
			ApprovalStatus stat = ApproverUtils.findRelativeStatus(wtc, contact);
			if (stat.isReady()) { // Current user is the next approver (it's in
				// their queue)
				continue; // so ok to update.
			}
			if (stat.isFinal() || stat.isPast()) {
				iter.remove(); // if final or past current user, then skip the
				// update
				continue;
			}
			// Must be submitted but not yet approved by current user or in
			// their queue.
			// Verify that the current user is in the approval chain.
			if (!ApproverUtils.isApprover(contact, project, wtc.getDepartment())) {
				iter.remove(); // not in approval chain ... skip the update.
			}
		}

		// Ok, ready to process all timecards remaining in the list.
		User user = SessionUtils.getCurrentUser();
		for (WeeklyTimecard wtc : timecards) {
			wtc = weeklyTimecardDAO.refresh(wtc);
			if (weeklyTimecardDAO.lock(wtc, user)) {
				success = copyData(weeklyTimecard, wtc);
				if (!success) {
					weeklyTimecardDAO.evict(wtc); // seemed necessary; just
					// 'refresh' wasn't enough
					wtc = weeklyTimecardDAO.refresh(wtc);
				}
				else {
					updated++;
				}
				weeklyTimecardDAO.unlock(wtc, user.getId());
			}
			else { // LOCK failure!
				success = false;
			}
			if (!success) {
				errorIds.add(wtc.getId());
				errorEntries.add(wtc);
				log.debug("update skipped, timecard locked, target=" + wtc.toString());
			}
		}
		return true;
	}

	/**
	 * Copies the data from the source (preferences) timecard to one target timecard.
	 *
	 * @param wtcIn The input (source) timecard.
	 * @param wtcOut The output (target) timecard. This should already be locked.
	 */
	private boolean copyData(WeeklyTimecard wtcIn, WeeklyTimecard wtcOut) {
		List<DailyTime> dailyIn = wtcIn.getDailyTimes();
		List<DailyTime> dailyOut = wtcOut.getDailyTimes();
		boolean success = true;

		int numJobs = 0;
		if (wtcOut.getPayJobs() != null) {
			numJobs = wtcOut.getPayJobs().size();
		}

		if (copyEpicode1) {
			if (overwriteData || StringUtils.isEmpty(wtcOut.getAccountMajor())) {
				// Set the episode (major) code in the timecard header
				wtcOut.setAccountMajor(epiCode1);
			}
			if (numJobs > 0) {
				if (overwriteData || StringUtils.isEmpty(wtcOut.getPayJobs().get(0).getAccountMajor())) {
					wtcOut.getPayJobs().get(0).setAccountMajor(epiCode1);
				}
			}
		}
		if (numJobs > 1) {
			if (copyEpicode2 && (overwriteData || StringUtils.isEmpty(wtcOut.getPayJobs().get(1).getAccountMajor()))) {
				wtcOut.getPayJobs().get(1).setAccountMajor(epiCode2);
			}
			if (numJobs > 2) {
				if (copyEpicode3 && (overwriteData || StringUtils.isEmpty(wtcOut.getPayJobs().get(2).getAccountMajor()))) {
					wtcOut.getPayJobs().get(2).setAccountMajor(epiCode3);
				}
				if (numJobs > 3) {
					if (copyEpicode4 && (overwriteData || StringUtils.isEmpty(wtcOut.getPayJobs().get(3).getAccountMajor()))) {
						wtcOut.getPayJobs().get(3).setAccountMajor(epiCode4);
					}
				}
			}
		}


		for (int day = 0; day < 7; day++) {
			DailyTime out = dailyOut.get(day);
			if (!out.getNoStartForm()) {
				DailyTime in = dailyIn.get(day);

				if (!wtcOut.getAllowWorked()) { // "allowed-work" means no MPVs
					// or NDMs
					if (in.getNonDeductMealPayroll()) {
						out.setNonDeductMealPayroll(true);
					}
					if (in.getNonDeductMeal2Payroll()) {
						out.setNonDeductMeal2Payroll(true);
					}
					if (in.getCameraWrap()) {
						out.setCameraWrap(true);
					}
					if (in.getFrenchHours()) {
						out.setFrenchHours(true);
					}
					if (overwriteData || out.getNdbEnd() == null) {
						out.setNdbEnd(in.getNdbEnd());
					}
					if (overwriteData || out.getNdmStart() == null) {
						out.setNdmStart(in.getNdmStart());
					}
					if (overwriteData || out.getNdmEnd() == null) {
						out.setNdmEnd(in.getNdmEnd());
					}
					if (overwriteData || out.getGrace1() == null) {
						out.setGrace1(in.getGrace1());
					}
					if (overwriteData || out.getGrace2() == null) {
						out.setGrace2(in.getGrace2());
					}
				}
				if (in.getWorkZone() != null && (overwriteData || out.getWorkZone() == null)) {
					// Don't set a null workZone in any case
					out.setWorkZone(in.getWorkZone());
				}
				if (in.getDayType() != null && (overwriteData || out.getDayType() == null)) {
					// Don't set a null dayType in any case
					out.setDayType(in.getDayType());
				}
				if (overwriteData || StringUtils.isEmpty(out.getAccountMajor())) {
					out.setAccountMajor(in.getAccountMajor());
				}
				if (in.getPhase() != null && in.getPhase() != ProductionPhase.N_A && (overwriteData || out.getPhase() == null)) {
					out.setPhase(in.getPhase());
					switch (in.getPhase()) {
						case P:
						case W:
							out.setAccountMajor(wtcOut.getAccountMajor());
							break;
						case S:
							out.setAccountMajor(wtcOut.getAccountDtl());
							break;
						case N_A:
							break;
					}
				}
				if (overwriteData || StringUtils.isEmpty(out.getCity())) {
					out.setCity(in.getCity());
				}
				if (overwriteData || StringUtils.isEmpty(out.getState())) {
					out.setState(in.getState());
				}
				if (overwriteData || StringUtils.isEmpty(out.getAccountLoc())) {
					out.setAccountLoc(in.getAccountLoc());
				}
				if (overwriteData || StringUtils.isEmpty(out.getAccountSet())) {
					out.setAccountSet(in.getAccountSet());
				}
				if (overwriteData || out.getLastManIn() == null) {
					out.setLastManIn(in.getLastManIn());
				}
			}
			// Validate data, then calculate the hours of work for the changed
			// day
			if (! TimecardCheck.validateAndCalcWorkDay(out, false)) {
				success = false;
				break;
			}
		}
		if (success) { // Ok, update weekly totals and save the changes
			TimecardCalc.calculateWeeklyTotals(wtcOut);
			weeklyTimecardDAO.attachDirty(wtcOut);
		}
		return success;
	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndingDate) {
		weekEndDate = weekEndingDate;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}

	/** See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}

	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #errorEntries}. */
	public List<WeeklyTimecard> getErrorEntries() {
		return errorEntries;
	}

	/** See {@link #errorEntries}. */
	public void setErrorEntries(List<WeeklyTimecard> errorEntry) {
		errorEntries = errorEntry;
	}

	/** See {@link #includeOffProduction}. */
	public boolean getIncludeOffProduction() {
		return includeOffProduction;
	}

	/** See {@link #includeOffProduction}. */
	public void setIncludeOffProduction(boolean selectAllTargets) {
		includeOffProduction = selectAllTargets;
	}

	/** See {@link #overwriteData}. */
	public boolean isOverwriteData() {
		return overwriteData;
	}

	/** See {@link #overwriteData}. */
	public void setOverwriteData(boolean selectAllDays) {
		overwriteData = selectAllDays;
	}

	/** See {@link #done}. */
	public boolean getDone() {
		return done;
	}

	/** See {@link #done}. */
	public void setDone(boolean done) {
		this.done = done;
	}

	/** See {@link #updated}. */
	public int getUpdated() {
		return updated;
	}

	/** See {@link #updated}. */
	public void setUpdated(int updated) {
		this.updated = updated;
	}

	/** See {@link #errors}. */
	public boolean getErrors() {
		return errors;
	}

	/** See {@link #errors}. */
	public void setErrors(boolean errors) {
		this.errors = errors;
	}

	/** See {@link #departmentsDL}. */
	public List<SelectItem> getDepartmentsDL() {
		if(departmentsDL == null) {
			departmentsDL = DepartmentUtils.getDepartmentCastCrewDL();
			departmentsDL.add(0, Constants.SELECT_ALL_IDS);
		}

		return departmentsDL;
	}

	/** See {@link #deptId}. */
	public Integer getDeptId() {
		return deptId;
	}

	/** See {@link #deptId}. */
	public void setDeptId(Integer deptId) {
		this.deptId = deptId;
	}

	/**
	 * Listen for changes in the Department select box.
	 * This value will determine which department timecards to update
	 *
	 * @param event
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		deptId = (Integer)event.getNewValue();
	}
}
