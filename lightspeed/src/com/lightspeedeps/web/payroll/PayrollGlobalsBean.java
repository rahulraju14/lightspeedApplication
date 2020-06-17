package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.ProductionPhase;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Approver Dashboard "Globals" page. This page allows the
 * user (typically a payroll or production accountant) to apply global changes
 * to all the timecards within a Production for a given week-ending date.
 * <p>
 * Note that the user's data to be applied as global changes is saved in a
 * WeeklyTimecard entity. This entity is distinguished from "real" timecards in
 * two ways: first, it has no associated StartForm; secondly, the userAccount
 * string is set to a value unique to the production, and which will not match
 * any possible User.accountNumber.
 * <p>
 * Most of the production payroll preferences are backed by the
 * {@link PayrollSetupBean}.
 */
@ManagedBean
@ViewScoped
public class PayrollGlobalsBean extends View implements Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(PayrollGlobalsBean.class);

	private static final int ACT_UPDATE = 11;

	/** The current Production. */
	private Production production;

	/** The Project to which the viewed and updated globals applies.
	 * Only used for Commercial productions. For TV & Feature
	 * productions, this will be null. */
	private Project project;

	/** The timecard used to save the user's data override values. */
	private WeeklyTimecard weeklyTimecard;

	/** The week-ending date of the timecards to be updated.  Also set in
	 * the timecard used to save the data overrides. */
	private Date weekEndDate;

	/** The list of available week-ending dates, presented to the user
	 * in a drop-down list. */
	private List<SelectItem> endDateList;

	private final static List<SelectItem> phaseDL;

	static {
		phaseDL = EnumList.createEnumValueSelectList(ProductionPhase.class);
		phaseDL.add(0, new SelectItem(ProductionPhase.N_A, "No Change"));
	}

	/**
	 * default constructor
	 */
	public PayrollGlobalsBean() {
		super("Payroll.Globals.");
		log.debug("");

		getProduction();
		if (production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}

		// Use the W/E date last set here, or in the Approver Dashboard filter.
		// (If the filter was set to "All", use the default W/E date instead.)
		weekEndDate = SessionUtils.getDate(Constants.ATTR_APPROVER_DATE);
		if (weekEndDate == null || weekEndDate.equals(Constants.SELECT_ALL_DATE)
				 || weekEndDate.equals(Constants.SELECT_PRIOR_DATE)) {
			weekEndDate = TimecardUtils.calculateDefaultWeekEndDate();
		}
		findTimecard();

//		forceLazyInit();
	}

	@Override
	public String actionEdit() {
		if (weeklyTimecard.getId() != null ) {
			// refresh necessary to restore phase values
			weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
		}
		for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
			if (dt.getPhase() == null) {
				dt.setPhase(ProductionPhase.N_A);
			}
		}
		super.actionEdit();
		forceLazyInit();
		return null;
	}

	/**
	 * The action method for the "Save" button. Saves the user's data overrides
	 * (entered into the partial timecard display), plus the various production
	 * options.
	 *
	 * @see com.lightspeedeps.web.view.View#actionSave()
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		if (TimecardCheck.validateGlobalsData(weeklyTimecard)) {
			WeeklyTimecardDAO.getInstance().attachDirty(weeklyTimecard);
			return super.actionSave();
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode. Cleans up the
	 * state of the Production and WeeklyTimecard, and calls our superclass'
	 * actionCancel() method.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		try {
			super.actionCancel();
			if (getProduction() != null) {
				production = ProductionDAO.getInstance().refresh(production);
			}
			if (weeklyTimecard != null && weeklyTimecard.getId() != null) {
				weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for "Update Timecards" button; the just displays the
	 * pop-up dialog which will get confirmation and actually perform the
	 * update.
	 *
	 * @return null navigation string
	 */
	public String actionUpdate() {
		GlobalUpdateBean bean = GlobalUpdateBean.getInstance();
		if (weeklyTimecard.getId() != null ) {
			// refresh necessary to restore phase values
			weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
		}
		bean.show(this, ACT_UPDATE, getMessagePrefix()+"Update.", weeklyTimecard);
		return null;
	}

	/**
	 * Ensure any fields necessary for re-rendering later are loaded
	 * while our entities are still in the Hibernate session.
	 */
	private void forceLazyInit() {
		for (DailyTime dt : weeklyTimecard.getDailyTimes()) {
			dt.getPhase();
//			log.debug(dt.getPhase());
		}
	}

	/**
	 * ValueChangeListener method for all daily numeric fields.
	 * @param event contains old and new values
	 */
	public void listenDailyChange(ValueChangeEvent event) {
//		log.debug(event.getOldValue() + ", " + event.getNewValue());
		try {
			DailyTime dailyTime = (DailyTime)ServiceFinder.getManagedBean("dailyTime");
			// Issue validation error messages (if any)
			TimecardCheck.validateAndCalcWorkDay(dailyTime, true);
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener method for the week-ending date drop-down control.
	 * Switches to display the matching timecard.
	 *
	 * @param event contains old and new values
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		try {
			Date date = (Date)event.getNewValue();
			if (date != null) {
				weekEndDate = date;
				SessionUtils.put(Constants.ATTR_APPROVER_DATE, weekEndDate);
				findTimecard();

				// Refresh didn't help disappearing radio values.
//				weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);

				// clearEditFields (clearChildren) didn't help disappearing radio values.
//				if (weeklyTimecard.getId() != null) {
//					GlobalRequestBean bean = (GlobalRequestBean)ServiceFinder.findBean("globalRequestBean");
//					if (bean != null) {
//						HtmlPanelGroup panel = bean.getEditPanel();
//						if (panel != null) {
//							ListView.clearEditFields(panel);
//						}
//					}
//				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Either load the WeeklyTimecard that holds the Global Preferences for the
	 * selected week-ending date, or create a new one if it doesn't already
	 * exist.
	 */
	private void findTimecard() {
		String userAcct = createGlobalsAccount(getProduction());
		weeklyTimecard = WeeklyTimecardDAO.getInstance()
				.findGlobalByWeekEndDateAccount(production, weekEndDate, userAcct);
		if (weeklyTimecard != null) {
			forceLazyInit();
		}
		else {
			weeklyTimecard = createTimecard(weekEndDate);
		}
	}

	/**
	 * Create a new WeeklyTimecard to be used to store the user's global data
	 * settings.
	 *
	 * @return A new WeeklyTimecard entity which has NOT been saved to the
	 *         database.
	 */
	private WeeklyTimecard createTimecard(Date endDate) {
		WeeklyTimecard wtc = TimecardUtils.createBlankTimecard(endDate);

		wtc.setProdId(production.getProdId());
		wtc.setUserAccount(createGlobalsAccount(production));

		for (DailyTime dt : wtc.getDailyTimes()) {
			dt.setPhase(ProductionPhase.N_A);
		}
		return wtc;
	}

	/**
	 * Create the string used as the "user account" value in the WeeklyTimecard
	 * that holds the global data settings for the given Production.
	 *
	 * @param prod The Production the global settings relate to.
	 * @return The string to be used as the WeeklyTimecard.userAccount value.
	 *         This is constructed such that it will not match any "real" user
	 *         account value (i.e., User.accountNumber).
	 */
	private String createGlobalsAccount(Production prod) {
		String acct = "G_" + prod.getProdId();
		if (project != null) {
			acct += "_" + project.getId();
		}
		return acct;
	}

	// accessors and mutators

	/**See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getNonSystemProduction();
		}
		return production;
	}

	/**See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/**See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		if (weekEndDate == null) {
			weekEndDate = TimecardUtils.calculateDefaultWeekEndDate();
		}
		return weekEndDate;
	}
	/**See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** Contents of the drop-down selection list for "Work Zone" */
	public List<SelectItem> getWorkZoneDL() {
		return EnumList.getWorkZoneList();
	}

	/** Contents of the drop-down selection list for "Day Type" */
	public List<SelectItem> getDayTypeDL() {
		return EnumList.getDayTypeList();
	}

	public List<SelectItem> getPhaseDL() {
		return phaseDL;
	}

	/**See {@link #endDateList}. */
	public List<SelectItem> getEndDateList() {
		if (endDateList == null) {
			endDateList = TimecardUtils.createEndDateList(production, project, getWeekEndDate(), false, true);
		}
		return endDateList;
	}

	/**See {@link #endDateList}. */
	public void setEndDateList(List<SelectItem> endDateList) {
		this.endDateList = endDateList;
	}

	/**
	 * Major is used for Job 1 value on the global input page
	 *
	 * @return
	 */
	public String getJob1EpiCode() {
		return weeklyTimecard.getAccountMajor();
	}

	/**
	 *  Major is used for Job 1 value on the global input page
	 *
	 * @param job1EpiCode
	 */
	public void setJob1EpiCode(String job1EpiCode) {
		weeklyTimecard.setAccountMajor(job1EpiCode);
	}

	/**
	 *  Free is used for Job 2 value on the global input page
	 *
	 * @return
	 */
	public String getJob2EpiCode() {
		return weeklyTimecard.getAccountFree();
	}

	/**
	 * Free is used for Job 2 value on the global input page
	 *
	 * @param job3EpiCode
	 */
	public void setJob2EpiCode(String job2EpiCode) {
		weeklyTimecard.setAccountFree(job2EpiCode);
	}

	/**
	 * Free 2 is used for Job 3 value on the global input page
	 *
	 * @param job3EpiCode
	 */
	public String getJob3EpiCode() {
		return weeklyTimecard.getAccountFree2();
	}

	/**
	 * Free 2 is used for Job 3 value on the global input page
	 *
	 * @param job3EpiCode
	 */
	public void setJob3EpiCode(String job3EpiCode) {
		weeklyTimecard.setAccountFree2(job3EpiCode);
	}

	/**
	 * Sub is used for Job 4 value on the global input page
	 */
	public String getJob4EpiCode() {
		return weeklyTimecard.getAccountSub();
	}

	/**
	 * Sub is used for Job 4 value on the global input page
	 *
	 * @param job4EpiCode
	 */
	public void setJob4EpiCode(String job4EpiCode) {
		weeklyTimecard.setAccountSub(job4EpiCode);
	}
}
