package com.lightspeedeps.web.approver;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DailyHotCostDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyHotCostsDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.HotCostsInput;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * Hot Costs Utility class to share common functionality.
 */
public class HotCostsUtils {
	private static final Log log = LogFactory.getLog(HotCostsUtils.class);

	// Day number constants
	/** Sunday day of the week number */
	public static final byte SUNDAY_NUM = 1;
	/** Monday day of the week number */
	public static final byte MONDAY_NUM = 2;
	/** Tuesday day of the week number */
	public static final byte TUESDAY_NUM = 3;
	/** Wednesday day of the week number */
	public static final byte WEDNESDAY_NUM = 4;
	/** Thursday day of the week number */
	public static final byte THURSDAY_NUM = 5;
	/** Friday day of the week number */
	public static final byte FRIDAY_NUM = 6;
	/** Saturday day of the week number */
	public static final byte SATURDAY_NUM = 7;

	/**
	 * Private constructor - no need to create instances of this class.
	 */
	private HotCostsUtils() {
	}

	/**
	 * Show the Hot Costs Import popup
	 *
	 * @param PopupHolder - Calling bean
	 * @param action - action to be performed.
	 */
	public static void showHotCostsImport(PopupHolder popupHolder, int action, String titleId) {
		HotCostsImportBean bean = HotCostsImportBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}

		bean.setSelectedImportType(HotCostsImportBean.IMPORT_TYPE_TIMECARD);
		bean.show(popupHolder, action, titleId);

		return;
	}

	/**
	 * Show the Hot Costs Import Budgeted Values popup
	 *
	 * @param PopupHolder - Calling bean
	 * @param action - action to be performed.
	 */
	@SuppressWarnings("unchecked")
	public static void showHotCostsImportBudgetedValues(PopupHolder popupHolder, int action, String titleId, String prodId) {
		HotCostsImportBean bean = HotCostsImportBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}

		List<Date>dayDates = DailyHotCostDAO.getInstance().find("select distinct date from DailyHotCost order by date desc");
		bean.setSelectedImportType(HotCostsImportBean.IMPORT_TYPE_BUDGETED_VALUES);
		bean.setDayDates(dayDates);

		bean.show(popupHolder, action, titleId);

		return;
	}

	/**
	 * Show the Hot Costs Import Messages popup Displays the messages showing the results of the
	 * import. Shows the number of imported timecards, number of timecards not imported and the
	 * reasons for those timecards not imported.
	 *
	 * @param importResults
	 * @param popupTitleId
	 */
	public static void showHotCostsImportMsgs(List<Object> importResults, String popupTitleId) {
		HotCostsImportMsgBean bean = HotCostsImportMsgBean.getInstance();

		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}

		bean.show(popupTitleId, importResults);

		return;
	}

	/**
	 * Show the re-rate popup
	 *
	 * @param PopupHolder - Calling bean
	 * @param action - action to be performed.
	 * @param dailyHotCost to re-rate
	 * @param dayOfWeek
	 * @param dayDate - Date of re-rate
	 */
	public static void showHotCostsReRate(PopupHolder popupHolder, int action, WeeklyHotCosts weeklyHotCosts, Byte dayOfWeekNum, Date dayDate) {
		HotCostsReRateBean bean = HotCostsReRateBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}

		StartForm sf =  weeklyHotCosts.getEmployment().getStartForm();

		if(sf == null) {
			return;
		}
		bean.setRateType(sf.getRateType());
		bean.setDailyHotCost(weeklyHotCosts.getDailyHotCosts().get(dayOfWeekNum - 1));
		bean.setWeeklyHotCosts(weeklyHotCosts);
		bean.show(popupHolder, sf, action);

	}

	/**
	 * Update one of the three sections on the Hot Costs Data Entry screen.
	 * Either the Time, MPV or Budgeted Values section.
	 *
	 * @param popupHolder
	 * @param bean
	 * @param action
	 * @param hotCostsInput
	 * @param deptWeeklyHotCostsList
	 * @param dayOfWeekNum
	 */
	public static void showUpdateHotCostsPopup(PopupHolder popupHolder, UpdateHotCostsBean bean, int action, HotCostsInput hotCostsInput, Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsList, Byte dayOfWeekNum) {
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}
		bean.setRecipients(null);

		bean.show(popupHolder, action, "HotCosts.UpdateBudgetedValues.Title");
	}

	/**
	 * Display list of possible members to delete from this daily hot costs.
	 *
	 * @param popupHolder
	 * @param action
	 * @param deptHWeeklyHotCostsList
	 * @param dayOfWeekNum
	 */
	public static void showDeleteHotCostsPopup(PopupHolder popupHolder, int action,  Map<Integer, DepartmentHotCostsEntry> deptHWeeklyHotCostsList, Byte dayOfWeekNum) {
		DeleteHotCostsBean bean = DeleteHotCostsBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}

		bean.setDayOfWeekNum(dayOfWeekNum);
		bean.setDeptWeeklyHotCostsList(deptHWeeklyHotCostsList);
		bean.setRecipients(null);

		bean.show(popupHolder, action, "HotCosts_DeleteTitle");
	}

	/**
	 * Display list of members that can be added to hot costs.
	 *
	 * @param popupHolder
	 * @param action
	 * @param eligibleEmployments
	 * @param deptWeeklyHotCostsList
	 * @param dayDate
	 * @param weekEndDate
	 */
	public static void showAddHotCostsPopup(PopupHolder popupHolder, int action, Map<Employment, WeeklyHotCosts> eligibleEmployments,
			Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsList, Date dayDate, Date weekEndDate) {

		AddHotCostsBean bean = AddHotCostsBean.getInstance();

		bean.setEligibleEmployments(eligibleEmployments);
		bean.setDeptWeeklyHotCostsList(deptWeeklyHotCostsList);
		bean.setDayDate(dayDate);
		bean.setWeekEndDate(weekEndDate);
		bean.setRecipients(null);
		bean.setSelectAllRecipients(false);

		bean.show(popupHolder, action);
	}


	public static void showCloneHotCostsBudgetedValuesPopup(PopupHolder popupHolder, Date workStartDate, Date copyBudgetFromDate, int action) {
		CloneHotCostsBudgetBean bean = CloneHotCostsBudgetBean.getInstance();

		bean.show(popupHolder, workStartDate, copyBudgetFromDate, action);
	}

	/**
	 * Import timecard daily time for the date selected.
	 *
	 * @param overwriteData flag to determine whether to overwrite existing daily Hot Costs data.
	 * @param deptHotCostsList - List of departments to import timecards from.
	 * @param production - Current Production
	 * @return
	 */
	public static List<Object> importFromTimecards(boolean overwriteData, Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap, Production production, Byte dayOfWeekNum) {
		List<Object> results = new ArrayList<>();
		List<String> outputMsgs = new ArrayList<>();
		int importedCnt = 0;
		int notImportedCnt = 0;

		Iterator<Entry<Integer, DepartmentHotCostsEntry>> itr;
		Set<Entry<Integer, DepartmentHotCostsEntry>> set;

		set = deptWeeklyHotCostsMap.entrySet();
		itr = set.iterator();
		while (itr.hasNext()) {
			int deptNotImported = 0;

			Entry<Integer, DepartmentHotCostsEntry> entrySet = itr.next();
			DepartmentHotCostsEntry deptEntry = entrySet.getValue();
			List<WeeklyHotCostsEntry> whcEntries = deptEntry.getWeeklyHotCostsWrappers();
			for (WeeklyHotCostsEntry whcEntry : whcEntries) {
				Employment emp = whcEntry.getWeeklyHotCosts().getEmployment();
				WeeklyHotCosts whc = whcEntry.getWeeklyHotCosts();
				WeeklyTimecard wtc = WeeklyTimecardDAO.getInstance().refresh(whcEntry.getWeeklyTimecard());

				if (wtc != null) {
					importedCnt++;
					//Force lazy init of daily time objects
					wtc.getDailyTimes().size();
					// Get the Daily Time associated with this timecard and copy its data to the Daily Hot Costs object.
					DailyTime dailyTime = wtc.getDailyTimes().get(dayOfWeekNum - 1);
					DailyHotCost dailyHotCost = whc.getDailyHotCosts().get(dayOfWeekNum - 1);
					TimecardCalc.calculateDailyHours(dailyTime);
					copyFromDailyTimeToDailyHotCosts(dailyTime, dailyHotCost, overwriteData, false);
					whc.getDailyHotCosts().set(dayOfWeekNum - 1, dailyHotCost);
				}
				else {
					deptNotImported++;
					notImportedCnt++;

					if (deptNotImported == 1) {
						// Add the Department name to the output
						outputMsgs.add(Constants.NEW_LINE + deptEntry.getDept().getName() + ":");
					}
					outputMsgs.add(Constants.NEW_LINE + MsgUtils.formatMessage("HotCosts.Import.Timecard.Not.Imported", emp.getContact().getDisplayName() + " - " + emp.getOccupation()));
				}
			}

			if (deptNotImported > 0) {
				// Add a line break
				outputMsgs.add(Constants.NEW_LINE);
			}
		}

		results.add(importedCnt);
		results.add(notImportedCnt);
		results.add(outputMsgs);

		return results;
	}

	// TODO Possibly implement for PR at a later date.
	@SuppressWarnings("unused")
	private void importFromPR() {

	}

	/**
	 * Copy data from the DailyTime object to the corresponding DailyHotCost object.
	 *
	 * @param dailyTime
	 * @param dailyHotCost
	 * @param overwriteData - Whether to overwrite existing data.
	 * @param copyBudgetedValues flag to determine whether we are process budgeted values.
	 */
	public static void copyFromDailyTimeToDailyHotCosts(DailyTime dailyTime, DailyHotCost dailyHotCost, boolean overwriteData, boolean copyBudgetedValues) {
		// DayNum
		if (overwriteData || dailyHotCost.getDayNum() == 0) {
			dailyHotCost.setDayNum(dailyTime.getDayNum());
		}
		// WorkDayNum
		if (overwriteData) {
			dailyHotCost.setWorkDayNum(dailyTime.getWorkDayNum());
		}
		// Date
		if (overwriteData || dailyHotCost.getDate() == null) {
			dailyHotCost.setDate(dailyTime.getDate());
		}
		// Call
		if (overwriteData || dailyHotCost.getCallTime() == null) {
			dailyHotCost.setCallTime(dailyTime.getCallTime());
		}
		// Meal 1 Out
		if (overwriteData || dailyHotCost.getM1Out() == null) {
			dailyHotCost.setM1Out(dailyTime.getM1Out());
		}
		// Meal 1 In
		if (overwriteData || dailyHotCost.getM1In() == null) {
			dailyHotCost.setM1In(dailyTime.getM1In());
		}
		// Meal 2 Out
		if (overwriteData || dailyHotCost.getM2Out() == null) {
			dailyHotCost.setM2Out(dailyTime.getM2Out());
		}
		// Meal 2 In
		if (overwriteData || dailyHotCost.getM2In() == null) {
			dailyHotCost.setM2In(dailyTime.getM2In());
		}
		// Wrap
		if (overwriteData || dailyHotCost.getWrap() == null) {
			dailyHotCost.setWrap(dailyTime.getWrap());
		}
		// On Call Start
		if (overwriteData || dailyHotCost.getOnCallStart() == null) {
			dailyHotCost.setOnCallStart(dailyTime.getOnCallStart());
		}
		// On Call End
		if (overwriteData || dailyHotCost.getOnCallEnd() == null) {
			dailyHotCost.setOnCallEnd(dailyTime.getOnCallEnd());
		}
		// Hours
		// We do not want to update the daily hours with the budgeted hours
		if(!copyBudgetedValues) {
			if (overwriteData || dailyHotCost.getHours() == null) {
				dailyHotCost.setHours(dailyTime.getHours());
			}
		}

		/*
		 * Will be used for talent // Mkup/Fitting From if (overwriteData ||
		 * dailyHotCost.getMkupFrom() == null) { dailyHotCost.setMkupFrom(dailyTime.getMkupFrom());
		 * } // Mkup/Fitting To if (overwriteData || dailyHotCost.getMkupTo() == null) {
		 * dailyHotCost.setMkupTo(dailyTime.getMkupTo()); } // TrvlToLoc From if (overwriteData ||
		 * dailyHotCost.getTrvlToLocFrom() == null) {
		 * dailyHotCost.setTrvlToLocFrom(dailyTime.getTrvlToLocFrom()); } // TrvlToLoc To if
		 * (overwriteData || dailyHotCost.getTrvlToLocTo() == null) {
		 * dailyHotCost.setTrvlToLocTo(dailyTime.getTrvlToLocTo()); } // TrvlFromLoc From if
		 * (overwriteData || dailyHotCost.getTrvlFromLocFrom() == null) {
		 * dailyHotCost.setTrvlFromLocFrom(dailyTime.getTrvlFromLocFrom()); } // TrvlFromLoc To if
		 * (overwriteData || dailyHotCost.getTrvlFromLocTo() == null) {
		 * dailyHotCost.setTrvlFromLocTo(dailyTime.getTrvlFromLocTo()); }
		 */
		// Worked
		if (overwriteData || dailyHotCost.getWorked()) {
			dailyHotCost.setWorked(dailyTime.getWorked());
		}
		// Work Zone
		if (overwriteData || dailyHotCost.getWorkZone() == null) {
			dailyHotCost.setWorkZone(dailyTime.getWorkZone());
		}
		// Day Type
		if (overwriteData || dailyHotCost.getDayType() == null) {
			dailyHotCost.setDayType(dailyTime.getDayType());
		}
		// Phase
		if (overwriteData || dailyHotCost.getPhase() == null) {
			dailyHotCost.setPhase(dailyTime.getPhase());
		}
		// No Start Form
		if (overwriteData || dailyHotCost.getNoStartForm()) {
			dailyHotCost.setNoStartForm(dailyTime.getNoStartForm());
		}
		// Opposite
		if (overwriteData || dailyHotCost.getOpposite()) {
			dailyHotCost.setOpposite(dailyTime.getOpposite());
		}
		// Off Production
		if (overwriteData || dailyHotCost.getOffProduction()) {
			dailyHotCost.setOffProduction(dailyTime.getOffProduction());
		}
		// Forced Call
		if (overwriteData || dailyHotCost.getForcedCall()) {
			dailyHotCost.setForcedCall(dailyTime.getForcedCall());
		}
		// Non-Deduct Meal
		if (overwriteData || dailyHotCost.getNonDeductMeal()) {
			dailyHotCost.setNonDeductMeal(dailyTime.getNonDeductMeal());
		}
		// Non-Deduct Meal 2
		if (overwriteData || dailyHotCost.getNonDeductMeal2()) {
			dailyHotCost.setNonDeductMeal2(dailyTime.getNonDeductMeal2());
		}
		// Non-Deduct Meal Payroll
		if (overwriteData || dailyHotCost.getNonDeductMealPayroll()) {
			dailyHotCost.setNonDeductMealPayroll(dailyTime.getNonDeductMealPayroll());
		}
		// Non-Deduct Meal 2 Payroll
		if (overwriteData || dailyHotCost.getNonDeductMeal2Payroll()) {
			dailyHotCost.setNonDeductMeal2Payroll(dailyTime.getNonDeductMeal2Payroll());
		}
		// NDB End
		if (overwriteData || dailyHotCost.getNdbEnd() == null) {
			dailyHotCost.setNdbEnd(dailyTime.getNdbEnd());
		}
		// NDM Start
		if (overwriteData || dailyHotCost.getNdmStart() == null) {
			dailyHotCost.setNdmStart(dailyTime.getNdmStart());
		}
		// NDM End
		if (overwriteData || dailyHotCost.getNdmEnd() == null) {
			dailyHotCost.setNdmEnd(dailyTime.getNdmEnd());
		}
		// Last Man In
		if (overwriteData || dailyHotCost.getLastManIn() == null) {
			dailyHotCost.setLastManIn(dailyTime.getLastManIn());
		}
		// MPV User
		if (overwriteData || dailyHotCost.getMpvUser() == null) {
			dailyHotCost.setMpvUser(dailyTime.getMpvUser());
		}
		// Grace 1
		if (overwriteData || dailyHotCost.getGrace1() == null) {
			dailyHotCost.setGrace1(dailyTime.getGrace1());
		}
		// Grace 2
		if (overwriteData || dailyHotCost.getGrace2() == null) {
			dailyHotCost.setGrace2(dailyTime.getGrace2());
		}
		// Camera Wrap
		if (overwriteData || dailyHotCost.getCameraWrap() == false) {
			dailyHotCost.setCameraWrap(dailyTime.getCameraWrap());
		}
		// French Hours
		if (overwriteData || dailyHotCost.getFrenchHours() == false) {
			dailyHotCost.setFrenchHours(dailyTime.getFrenchHours());
		}
		// Location Code (Account Location)
		if (overwriteData || dailyHotCost.getAccountLoc() == null) {
			dailyHotCost.setAccountLoc(dailyTime.getAccountLoc());
		}
		// Prod Episode (Account Major)
		if (overwriteData || dailyHotCost.getAccountMajor() == null) {
			dailyHotCost.setAccountMajor(dailyTime.getAccountMajor());
		}
		// Account Set
		if (overwriteData || dailyHotCost.getAccountSet() == null) {
			dailyHotCost.setAccountSet(dailyTime.getAccountSet());
		}
		// Account Free
		if (overwriteData || dailyHotCost.getAccountFree() == null) {
			dailyHotCost.setAccountFree(dailyTime.getAccountFree());
		}
		// Re-Rate
		if (overwriteData || dailyHotCost.getReRate() == false) {
			dailyHotCost.setReRate(dailyTime.getReRate());
		}
		// City
		if (overwriteData || dailyHotCost.getCity() == null) {
			dailyHotCost.setOffProduction(dailyTime.getOffProduction());
		}
		// State
		if (overwriteData || dailyHotCost.getState() == null) {
			dailyHotCost.setState(dailyTime.getState());
		}
		// MPV 1 Payroll
		if (overwriteData || dailyHotCost.getMpv1Payroll() == null) {
			dailyHotCost.setMpv1Payroll(dailyTime.getMpv1Payroll());
		}
		// MPV 2 Payroll
		if (overwriteData || dailyHotCost.getMpv2Payroll() == null) {
			dailyHotCost.setMpv2Payroll(dailyTime.getMpv2Payroll());
		}
		// MPV 3 Payroll
		if (overwriteData || dailyHotCost.getMpv3Payroll() == null) {
			dailyHotCost.setMpv3Payroll(dailyTime.getMpv3Payroll());
		}
		// Split By Percent
		if (overwriteData || dailyHotCost.getSplitByPercent()) {
			dailyHotCost.setSplitByPercent(dailyTime.getSplitByPercent());
		}
		// Split Start 2
		if (overwriteData || dailyHotCost.getSplit2() == null) {
			dailyHotCost.setSplit2(dailyTime.getSplit2());
		}
		// Split Start 3
		if (overwriteData || dailyHotCost.getSplit3() == null) {
			dailyHotCost.setSplit3(dailyTime.getSplit3());
		}
		// Job Num 1
		if (overwriteData || dailyHotCost.getJobNum1() == 0) {
			dailyHotCost.setJobNum1(dailyTime.getJobNum1());
		}
		// Job Num 2
		if (overwriteData || dailyHotCost.getJobNum2() == 0) {
			dailyHotCost.setJobNum2(dailyTime.getJobNum2());
		}
		// Job Num 3
		if (overwriteData || dailyHotCost.getJobNum3() == 0) {
			dailyHotCost.setJobNum3(dailyTime.getJobNum3());
		}
		// Account Set
		if (overwriteData || dailyHotCost.getAccountSet() == null) {
			dailyHotCost.setAccountSet(dailyTime.getAccountSet());
		}
		// Account Free
		if (overwriteData || dailyHotCost.getAccountFree() == null) {
			dailyHotCost.setAccountFree(dailyTime.getAccountFree());
		}
	}

	/**
	 * Copy data from the DailyHotCost object to the corresponding DailyTime object.
	 *
	 * @param dailyHotCost
	 * @param dailyTime
	 * @param overwriteData - Whether to overwrite existing data.
	 * @param copyBudgetedValues flag to determine whether we are process budgeted values.
	 */
	public static void copyFromDailyHotCostsToDailyTime(DailyHotCost dailyHotCost, DailyTime dailyTime, boolean overwriteData, boolean copyBudgetedValues) {
		// DayNum
		if (overwriteData || dailyTime.getDayNum() == 0) {
			dailyTime.setDayNum(dailyHotCost.getDayNum());
		}
		// WorkDayNum
		if (overwriteData) {
			dailyTime.setWorkDayNum(dailyHotCost.getWorkDayNum());
		}
		// Date
		if (overwriteData || dailyTime.getDate() == null) {
			dailyTime.setDate(dailyHotCost.getDate());
		}
		// Call
		if (overwriteData || dailyTime.getCallTime() == null) {
			dailyTime.setCallTime(dailyHotCost.getCallTime());
		}
		// Meal 1 Out
		if (overwriteData || dailyTime.getM1Out() == null) {
			dailyTime.setM1Out(dailyHotCost.getM1Out());
		}
		// Meal 1 In
		if (overwriteData || dailyTime.getM1In() == null) {
			dailyTime.setM1In(dailyHotCost.getM1In());
		}
		// Meal 2 Out
		if (overwriteData || dailyTime.getM2Out() == null) {
			dailyTime.setM2Out(dailyHotCost.getM2Out());
		}
		// Meal 2 In
		if (overwriteData || dailyTime.getM2In() == null) {
			dailyTime.setM2In(dailyHotCost.getM2In());
		}
		// Wrap
		if (overwriteData || dailyTime.getWrap() == null) {
			dailyTime.setWrap(dailyHotCost.getWrap());
		}
		// On Call Start
		if (overwriteData || dailyTime.getOnCallStart() == null) {
			dailyTime.setOnCallStart(dailyHotCost.getOnCallStart());
		}
		// On Call End
		if (overwriteData || dailyTime.getOnCallEnd() == null) {
			dailyTime.setOnCallEnd(dailyHotCost.getOnCallEnd());
		}
		// Hours
		if (overwriteData || dailyTime.getHours() == null) {
			if(copyBudgetedValues) {
				// Use the budgeted hours instead of the actual hours.
				dailyTime.setHours(dailyHotCost.getBudgetedHours());
			}
			else {
				dailyTime.setHours(dailyHotCost.getHours());
			}
		}
		/*
		 * Will be used for talent // Mkup/Fitting From if (overwriteData || dailyTime.getMkupFrom()
		 * == null) { dailyTime.setMkupFrom(dailyHotCost.getMkupFrom()); } // Mkup/Fitting To if
		 * (overwriteData || dailyTime.getMkupTo() == null) {
		 * dailyTime.setMkupTo(dailyHotCost.getMkupTo()); } // TrvlToLoc From if (overwriteData ||
		 * dailyTime.getTrvlToLocFrom() == null) {
		 * dailyTime.setTrvlToLocFrom(dailyHotCost.getTrvlToLocFrom()); } // TrvlToLoc To if
		 * (overwriteData || dailyTime.getTrvlToLocTo() == null) {
		 * dailyTime.setTrvlToLocTo(dailyHotCost.getTrvlToLocTo()); } // TrvlFromLoc From if
		 * (overwriteData || dailyTime.getTrvlFromLocFrom() == null) {
		 * dailyTime.setTrvlFromLocFrom(dailyHotCost.getTrvlFromLocFrom()); } // TrvlFromLoc To if
		 * (overwriteData || dailyTime.getTrvlFromLocTo() == null) {
		 * dailyTime.setTrvlFromLocTo(dailyHotCost.getTrvlFromLocTo()); }
		 */
		// Worked
		if (overwriteData || dailyTime.getWorked()) {
			dailyTime.setWorked(dailyHotCost.getWorked());
		}
		// Work Zone
		if (overwriteData || dailyTime.getWorkZone() == null) {
			dailyTime.setWorkZone(dailyHotCost.getWorkZone());
		}
		// Day Type
		if (overwriteData || dailyTime.getDayType() == null) {
			dailyTime.setDayType(dailyHotCost.getDayType());
		}
		// Phase
		if (overwriteData || dailyTime.getPhase() == null) {
			dailyTime.setPhase(dailyHotCost.getPhase());
		}
		// No Start Form
		if (overwriteData || dailyTime.getNoStartForm()) {
			dailyTime.setNoStartForm(dailyHotCost.getNoStartForm());
		}
		// Opposite
		if (overwriteData || dailyTime.getOpposite()) {
			dailyTime.setOpposite(dailyHotCost.getOpposite());
		}
		// Off Production
		if (overwriteData || dailyTime.getOffProduction()) {
			dailyTime.setOffProduction(dailyHotCost.getOffProduction());
		}
		// Forced Call
		if (overwriteData || dailyTime.getForcedCall()) {
			dailyTime.setForcedCall(dailyHotCost.getForcedCall());
		}
		// Non-Deduct Meal
		if (overwriteData || dailyTime.getNonDeductMeal()) {
			dailyTime.setNonDeductMeal(dailyHotCost.getNonDeductMeal());
		}
		// Non-Deduct Meal 2
		if (overwriteData || dailyTime.getNonDeductMeal2()) {
			dailyTime.setNonDeductMeal2(dailyHotCost.getNonDeductMeal2());
		}
		// Non-Deduct Meal Payroll
		if (overwriteData || dailyTime.getNonDeductMealPayroll()) {
			dailyTime.setNonDeductMealPayroll(dailyHotCost.getNonDeductMealPayroll());
		}
		// Non-Deduct Meal 2 Payroll
		if (overwriteData || dailyTime.getNonDeductMeal2Payroll()) {
			dailyTime.setNonDeductMeal2Payroll(dailyHotCost.getNonDeductMeal2Payroll());
		}
		// NDB End
		if (overwriteData || dailyTime.getNdbEnd() == null) {
			dailyTime.setNdbEnd(dailyHotCost.getNdbEnd());
		}
		// NDM Start
		if (overwriteData || dailyTime.getNdmStart() == null) {
			dailyTime.setNdmStart(dailyHotCost.getNdmStart());
		}
		// NDM End
		if (overwriteData || dailyTime.getNdmEnd() == null) {
			dailyTime.setNdmEnd(dailyHotCost.getNdmEnd());
		}
		// Last Man In
		if (overwriteData || dailyTime.getLastManIn() == null) {
			dailyTime.setLastManIn(dailyHotCost.getLastManIn());
		}
		// MPV User
		if (overwriteData || dailyTime.getMpvUser() == null) {
			dailyTime.setMpvUser(dailyHotCost.getMpvUser());
		}
		// Grace 1
		if (overwriteData || dailyTime.getGrace1() == null) {
			dailyTime.setGrace1(dailyHotCost.getGrace1());
		}
		// Grace 2
		if (overwriteData || dailyTime.getGrace2() == null) {
			dailyTime.setGrace2(dailyHotCost.getGrace2());
		}
		// Camera Wrap
		if (overwriteData || dailyTime.getCameraWrap()) {
			dailyTime.setCameraWrap(dailyHotCost.getCameraWrap());
		}
		// French Hours
		if (overwriteData || dailyTime.getFrenchHours()) {
			dailyTime.setFrenchHours(dailyHotCost.getFrenchHours());
		}
		// Location Code (Account Location)
		if (overwriteData || dailyTime.getAccountLoc() == null) {
			dailyTime.setAccountLoc(dailyHotCost.getAccountLoc());
		}
		// Prod Episode (Account Major)
		if (overwriteData || dailyTime.getAccountMajor() == null) {
			dailyTime.setAccountMajor(dailyHotCost.getAccountMajor());
		}
		// Account Set
		if (overwriteData || dailyTime.getAccountSet() == null) {
			dailyTime.setAccountSet(dailyHotCost.getAccountSet());
		}
		// Account Free
		if (overwriteData || dailyTime.getAccountFree() == null) {
			dailyTime.setAccountFree(dailyHotCost.getAccountFree());
		}
		// Re-Rate
		if (overwriteData || dailyTime.getReRate()) {
			dailyTime.setReRate(dailyHotCost.getReRate());
		}
		// City
		if (overwriteData || dailyTime.getCity() == null) {
			dailyTime.setOffProduction(dailyHotCost.getOffProduction());
		}
		// State
		if (overwriteData || dailyTime.getState() == null) {
			dailyTime.setState(dailyHotCost.getState());
		}
		// MPV 1 Payroll
		if (overwriteData || dailyTime.getMpv1Payroll() == null) {
			dailyTime.setMpv1Payroll(dailyHotCost.getMpv1Payroll());
		}
		// MPV 2 Payroll
		if (overwriteData || dailyTime.getMpv2Payroll() == null) {
			dailyTime.setMpv2Payroll(dailyHotCost.getMpv2Payroll());
		}
		// MPV 3 Payroll
		if (overwriteData || dailyTime.getMpv3Payroll() == null) {
			dailyTime.setMpv3Payroll(dailyHotCost.getMpv3Payroll());
		}
		// Split By Percent
		if (overwriteData || dailyTime.getSplitByPercent()) {
			dailyTime.setSplitByPercent(dailyHotCost.getSplitByPercent());
		}
		// Split Start 2
		if (overwriteData || dailyTime.getSplit2() == null) {
			dailyTime.setSplit2(dailyHotCost.getSplit2());
		}
		// Split Start 3
		if (overwriteData || dailyTime.getSplit3() == null) {
			dailyTime.setSplit3(dailyHotCost.getSplit3());
		}
		// Job Num 1
		if (overwriteData || dailyTime.getJobNum1() == 0) {
			dailyTime.setJobNum1(dailyHotCost.getJobNum1());
		}
		// Job Num 2
		if (overwriteData || dailyTime.getJobNum2() == 0) {
			dailyTime.setJobNum2(dailyHotCost.getJobNum2());
		}
		// Job Num 3
		if (overwriteData || dailyTime.getJobNum3() == 0) {
			dailyTime.setJobNum3(dailyHotCost.getJobNum3());
		}
		// Account Set
		if (overwriteData || dailyTime.getAccountSet() == null) {
			dailyTime.setAccountSet(dailyHotCost.getAccountSet());
		}
		// Account Free
		if (overwriteData || dailyTime.getAccountFree() == null) {
			dailyTime.setAccountFree(dailyHotCost.getAccountFree());
		}
	}

	/**
	 * Create a new WeeklyHotCost object
	 *
	 * @param employment
	 * @param weeklyTimecard. If there is a weekly timecard associated with this employment record, use the daily time
	 * values for the daily hot cost records.
	 * @param weekEndDate
	 * @param startForm
	 * @return
	 */
	public static WeeklyHotCosts createNewWeeklyHotCosts(Employment employment, WeeklyTimecard weeklyTimecard, Date weekEndDate, StartForm startForm) {
		WeeklyHotCosts whc = new WeeklyHotCosts();

		whc.setUserAccount(employment.getContact().getUser().getAccountNumber());
		whc.setProdId(employment.getContact().getProduction().getProdId());
		whc.setEndDate(weekEndDate);
		whc.setEmploymentId(employment.getId());
		whc.setDepartmentId(employment.getDepartmentId());
		whc.setDepartmentName(employment.getRole().getDepartment().getName());
		whc.setEmployment(employment);
		whc.setStartFormId(startForm.getId());
		whc.setStartForm(startForm);

		// Create the daily hot costs collection
		List<DailyHotCost> dailyHotCosts = new ArrayList<>();

		for (byte dayNum = 1; dayNum < 8; dayNum++) {
			Date date;
			Calendar weekEndCal = Calendar.getInstance();
			weekEndCal.setTime(weekEndDate);

			weekEndCal.set(Calendar.DAY_OF_WEEK, weekEndCal.getFirstDayOfWeek() + (dayNum - 1));
			date = weekEndCal.getTime();
			DailyHotCost dhc = new DailyHotCost(date);
			dhc.setDayNum(dayNum);

			dhc.setAccountLoc(startForm.getAccountLoc());
			dhc.setAccountFree(startForm.getAccountFree());
			dhc.setAccountMajor(startForm.getAccountMajor());
			dhc.setAccountSet(startForm.getAccountSet());
			dhc.setRate(startForm.getRate());

			if(weeklyTimecard != null) {
				// Use the daily time values from the timecard to create the daily hot cost objects.
				whc.setDailyRate(weeklyTimecard.getDailyRate());
				whc.setHourlyRate(weeklyTimecard.getHourlyRate());
				whc.setWeeklyRate(weeklyTimecard.getWeeklyRate());
//				DailyTime dt = weeklyTimecard.getDailyTimes().get(dayNum - 1);
//				copyFromDailyTimeToDailyHotCosts(dt, dhc, true, false);
			}
			else {
				whc.setDailyRate(startForm.getRate());
				whc.setHourlyRate(startForm.getRate());
				whc.setWeeklyRate(startForm.getRate());
			}

			dhc.setWeeklyHotCosts(whc);

			dailyHotCosts.add(dhc);
		}

		whc.setDailyHotCosts(dailyHotCosts);

		return whc;
	}

	/**
     * Create Department list to display
     */
    public static Map<Integer, DepartmentHotCostsEntry> createDepartmentHotCostsMap(Production production, Project project, Date weekEndDate, boolean includeDeptsWithoutEmployees) {
		log.debug("Starting");
    	List<Department> depts;
    	WeeklyHotCostsDAO whcDAO = WeeklyHotCostsDAO.getInstance();

		depts = DepartmentUtils.getDepartmentList();

		Map<Integer, DepartmentHotCostsEntry>deptWeeklyHotCostsList = new LinkedHashMap<>();

//		// Get all the Weekly HotCosts instances keyed by the employment record id.
//		employmentsWithWeeklyHotCosts = createEmploymentsWithWeeklyHotCosts();
//		// Get all employments with weekly time cards for the selected week end date.
//		employmentsWithWeeklyTimecards = createEmploymentsWithWeeklyTimecards();
		// Get all the employment records for each department
		Map<Integer, List<Employment>> deptEmps = createEmploymentsByDepartment(production, project);

		// Load all timecards for this week-ending date
		Map<String, WeeklyTimecard> allWtcMap = createWtcAcctMap(production, project, weekEndDate);
		// Load all start forms for this production/project
		Map<String, StartForm>allSfMap = createSfAcctMap(production, project);

		for (Department dept : depts) {
			int deptId = dept.getId();
			if (deptId == Constants.DEPARTMENT_ID_DATA_ADMIN || deptId == Constants.DEPARTMENT_ID_LS_ADMIN) {
				// Skip the LS Admin departments
				continue;
			}

			DepartmentHotCostsEntry deptHcEntry = new DepartmentHotCostsEntry();

			// Get the WeeklyHotCosts for this department. If none then continue
			// to the next department.
			deptHcEntry.setDept(dept);
			deptHcEntry.setExpand(true);
			deptHcEntry.setSummaryExpand(true);

			List<Employment>emps = deptEmps.get(deptId);
			if (emps != null && emps.size() > 0) {
				// See if they already have a WeeklyHotCost object attached to
				// this employment record and week end date.
				for (Employment emp : emps) {
					if (emp.getRole().isAdmin() || emp.getRole().isDataAdmin() || emp.getRole().isFinancialAdmin()) {
						continue;
					}
					String userAcct = emp.getContact().getUser().getAccountNumber();

					WeeklyHotCostsEntry whcWrapper = new WeeklyHotCostsEntry();

					// Get list Weekly HotCosts for this employment record. If null, create
					// a new WeeklyHotCosts object.
					String orderBy = " order by end_date DESC";

					StartForm sf = null;
					sf = findSfByAccountDept(allSfMap, userAcct, emp.getOccupation());
					if(sf == null) {
						continue;
					}
					WeeklyTimecard wtc = null;

					// Get the latest timecard for this employee
					wtc = findByAccountDept(allWtcMap, userAcct, dept.getName());

					whcWrapper.setWeeklyTimecard(wtc);
					List<WeeklyHotCosts>weeklyHotCostsList = whcDAO.findByUserAcctProdWeekEndDateEmployment(userAcct, production, project,weekEndDate, emp.getId(), orderBy);
					WeeklyHotCosts whc;

					if(weeklyHotCostsList != null && !weeklyHotCostsList.isEmpty()) {
						// Remove the first entry in case we need to go back to the previous
						// WeeklyHotCosts object to preset fields.
						whc = weeklyHotCostsList.remove(0);
						whc.getDailyHotCosts().size();
						whc.setEmployment(emp);
						whc.setStartFormId(sf.getId());
						whc.setStartForm(sf);

						whcWrapper.setWeeklyHotCosts(whc);
					}
					else {
						whc = HotCostsUtils.createNewWeeklyHotCosts(emp, wtc, weekEndDate, sf);
					}
					whcWrapper.setWeeklyHotCosts(whc);
					deptHcEntry.getWeeklyHotCostsWrappers().add(whcWrapper);
				}
			}
			else {
				if (! includeDeptsWithoutEmployees) {
					// Do not show departments that do not have employees
					continue;
				}
			}

			deptWeeklyHotCostsList.put(dept.getId(), deptHcEntry);
		}

		log.debug("* FINISHED HC dept map");
		return deptWeeklyHotCostsList;
	}

	/**
	 * @param production
	 * @param project
	 * @param weekEndDate
	 * @return
	 */
	private static Map<String, WeeklyTimecard> createWtcAcctMap(Production production, Project project, Date weekEndDate) {
		List<WeeklyTimecard> allWtcList = WeeklyTimecardDAO.getInstance().findByWeekEndDateAccountDept(production, project, weekEndDate,
				TimecardRange.WEEK, null, null, null, "endDate asc");
		HashMap<String, WeeklyTimecard> map = new HashMap<>(allWtcList.size()+1);
		for (WeeklyTimecard wtc : allWtcList) {
			map.put(wtc.getUserAccount() + "." + wtc.getDeptName(), wtc);
		}
		return map;
	}

	/**
	 * Create a map of start forms based on the Production or Project. If using a project, then
	 * this is probably a commercial production.
	 *
	 * @param production
	 * @param project
	 * @return
	 */
	private static Map<String, StartForm> createSfAcctMap(Production production, Project project) {
		List<StartForm> allSfList;

		if(project != null) {
			allSfList = StartFormDAO.getInstance().findByProject(project);
		}
		else {
			allSfList = StartFormDAO.getInstance().findByProduction(production);
		}

		HashMap<String, StartForm> map = new HashMap<>(allSfList.size()+1);
		for (StartForm sf : allSfList) {
			Contact ct = sf.getContact();
			User user = ct.getUser();
			Employment emp = sf.getEmployment();
			if(emp != null) {
				map.put(user.getAccountNumber() + "." + emp.getOccupation(), sf);
			}
		}
		return map;
	}

	/**
	 * Find a timecard with matching userAccount and department, within the
	 * collection provided.
	 * @param allWtcMap
	 * @param userAcct
	 * @param deptName
	 * @return
	 */
	private static WeeklyTimecard findByAccountDept(Map<String, WeeklyTimecard> allWtcMap,
			String userAcct, String deptName) {
		WeeklyTimecard w = allWtcMap.get(userAcct + "." + deptName);
		return w;
	}

	/**
	 * Find a start form with matching userAccount and department, within the
	 * collection provided.
	 * @param allSfMap
	 * @param userAcct
	 * @param deptName
	 * @return
	 */
	private static StartForm findSfByAccountDept(Map<String, StartForm> allSfMap,
			String userAcct, String deptName) {
		StartForm sf = allSfMap.get(userAcct + "." + deptName);
		return sf;
	}

	/**
	 * Create map up employees for each department.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Map<Integer, List<Employment>> createEmploymentsByDepartment(Production production, Project project) {
		Map<Integer, List<Employment>> deptEmps = new HashMap<>();
		List<Employment> emps = new ArrayList<>();
		List<Employment> empsByDept = new ArrayList<>();
		List<Object> sqlParams = new ArrayList<>();

		String sqlQuery = "select emp from Employment emp, Department dept, Contact ct, Role rl where rl.id = emp.role.id and dept.id = rl.department.id and ";
		sqlQuery += "ct.id = emp.contact.id and ct.production=? and (emp.project = ? or emp.project is null) order by dept.name";
		sqlParams.add(production);
		sqlParams.add(project);

		emps = EmploymentDAO.getInstance().find(sqlQuery, sqlParams.toArray());

		Integer currDeptId, newDeptId;
		Iterator<Employment> empItr = emps.iterator();
		currDeptId = emps.get(0).getDepartmentId();
		newDeptId = currDeptId;

		while (empItr.hasNext()) {
			Employment emp = empItr.next();

			newDeptId = emp.getDepartmentId();
			if (currDeptId != newDeptId) {
				deptEmps.put(currDeptId, empsByDept);
				empsByDept = new ArrayList<>();
				currDeptId = newDeptId;
			}
			empsByDept.add(emp);
		}
		// Add the last entry in.
		if(!empsByDept.isEmpty()) {
			deptEmps.put(currDeptId, empsByDept);
		}

		return deptEmps;
	}


	public static void updateHours(DailyHotCost dailyHotCost, boolean overwriteData) {
		DailyTime dt = new DailyTime();
		HotCostsUtils.copyFromDailyHotCostsToDailyTime(dailyHotCost, dt, overwriteData, false);
		dt.setHours(null);
		if (dt.getCallTime() != null && dt.getWorkEnd() != null) {
			BigDecimal hoursVal = dt.getWorkEnd().subtract(dt.getCallTime())
					.subtract(dt.getOnCallGap());
			if (dt.getM1In() != null && dt.getM1Out() != null) {
				hoursVal = hoursVal.subtract(dt.getM1In().subtract(dt.getM1Out()));
			}
			if (dt.getM2In() != null && dt.getM2Out() != null) {
				hoursVal = hoursVal.subtract(dt.getM2In().subtract(dt.getM2Out()));
			}
			dt.setHours(hoursVal);
		}

		HotCostsUtils.copyFromDailyTimeToDailyHotCosts(dt, dailyHotCost, overwriteData, false);
	}


	public static void updateBudgetedAmount(WeeklyHotCostsEntry weeklyHotCostsEntry, DailyHotCost dailyHotCost, BigDecimal budgetedHours) {
		WeeklyHotCosts whc = weeklyHotCostsEntry.getWeeklyHotCosts();
		StartForm sf = StartFormDAO.getInstance().refresh(whc.getStartForm());
		BigDecimal rate = sf.getRate();
		EmployeeRateType rateType = sf.getRateType();

		if(rate != null) {
			if(rateType == EmployeeRateType.HOURLY) {
				dailyHotCost.setBudgetedCost(rate.multiply(budgetedHours));
			}
			else if(rateType == EmployeeRateType.DAILY) {
				dailyHotCost.setBudgetedCost(rate);
			}
			else if(rateType == EmployeeRateType.WEEKLY) {
				BigDecimal dailyPercentage = Constants.DECIMAL_POINT_TWO;

				WeeklyTimecard wtc = weeklyHotCostsEntry.getWeeklyTimecard();

				// If the weekly timecard is not null, get the studioOrLoc value.
				// If not, use the Start Form.
				if((wtc != null && wtc.isDistantRate()) || whc.getStartForm().isDistantRate()) {
						dailyPercentage = new BigDecimal(.6667);
				}

				MathContext mt = new MathContext(6, RoundingMode.HALF_UP);
				dailyHotCost.setBudgetedCost(rate.multiply(dailyPercentage, mt));
			}
		}
	}
}
