package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.WeeklyHotCosts;

/**
 * A data access object (DAO) providing persistence and search support for DailyHotCosts entities.
 * Transaction control of the save(), update() and delete() operations can directly support Spring
 * container-managed transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how to configure it for
 * the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.DailyHotCost
 * @author MyEclipse Persistence Tools
 */
public class DailyHotCostDAO extends BaseTypeDAO<DailyHotCost> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DailyHotCostDAO.class);

	//property constants
	public static final String DAY_NUM = "dayNum";
	public static final String WORK_DAY_NUM = "workDayNum";
	public static final String CALL_TIME = "callTime";
	public static final String M1_OUT = "m1Out";
	public static final String M1_IN = "m1In";
	public static final String M2_OUT = "m2Out";
	public static final String M2_IN = "m2In";
	public static final String MK_UP_FROM = "mkUpFrom";
	public static final String MK_UP_TO = "mkUpTo";
	public static final String TRVL_TO_LOC_FROM = "trvlToLocFrom";
	public static final String TRVL_TO_LOC_TO = "trvlToLocTo";
	public static final String TRVL_FROM_LOC_FROM = "trvlFromLocFrom";
	public static final String TRVL_FROM_LOC_TO = "trvlFromLocTo";
	public static final String WRAP = "wrap";
	public static final String HOURS = "hours";
	public static final String WORKED = "worked";
	public static final String WORK_ZONE = "workZone";
	public static final String DAY_TYPE = "dayType";
	public static final String PHASE = "phase";
	public static final String NO_START_FORM = "noStartForm";
	public static final String OPPOSITE = "opposite";
	public static final String OFF_PRODUCTION = "offProduction";
	public static final String FORCED_CALL = "forcedCall";
	public static final String NON_DEDUCT_MEAL = "nonDeductMeal";
	public static final String NON_DEDUCT_MEAL2 = "nonDeductMeal2";
	public static final String NON_DEDUCT_MEAL_PAYROLL = "nonDeductMealPayroll";
	public static final String NON_DEDUCT_MEAL2_PAYROLL = "nonDeductMeal2Payroll";
	public static final String NDB_END = "ndbEnd";
	public static final String NDM_START = "ndmStart";
	public static final String NDM_END = "ndmEnd";
	public static final String LAST_MAN_IN = "lastManIn";
	public static final String MPV_USER = "mpvUser";
	public static final String GRACE1 = "grace1";
	public static final String GRACE2 = "grace2";
	public static final String CAMERA_WRAP = "cameraWrap";
	public static final String FRENCH_HOURS = "frenchHours";
	public static final String LOCATION_CODE = "locationCode";
	public static final String PROD_EPISODE = "prodEpisode";
	public static final String ACCT_SET = "acctSet";
	public static final String ACCT_FREE = "acctFree";
	public static final String RE_RATE = "reRate";
	public static final String OCC_CODE = "occCode";
	public static final String CITY = "city";
	public static final String STATE = "state";
	public static final String MPV1_PAYROLL = "mpv1Payroll";
	public static final String MPV2_PAYROLL = "mpv2Payroll";
	public static final String MPV3_PAYROLL = "mpv3Payroll";
	public static final String SPLIT_BY_PERCENT = "splitByPercent";
	public static final String JOB_NUM1 = "jobNum1";
	public static final String SPLIT_START2 = "splitStart2";
	public static final String JOB_NUM2 = "jobNum2";
	public static final String SPLIT_START3 = "splitStart3";
	public static final String JOB_NUM3 = "jobNum3";
	public static final String TALENT_INITIAL_ID = "talentInitialId";
	public static final String ACTUAL_COST = "actualCost";
	public static final String BUDGETED_HOURS = "budgetedHours";
	public static final String BUDGETED_COST = "budgetedCost";
	public static final String BUDGETED_MVPS = "budgetedMvps";
	public static final String BUDGETED_MVPS_COST = "budgetedMvpsCost";


	public static DailyHotCostDAO getInstance() {
		return (DailyHotCostDAO)getInstance("DailyHotCostDAO");
	}

	public List<DailyHotCost> findByDayNum(Object dayNum) {
		return findByProperty(DAY_NUM, dayNum);
	}

	public List<DailyHotCost> findByWorkDayNum(Object workDayNum) {
		return findByProperty(WORK_DAY_NUM, workDayNum);
	}

	public List<DailyHotCost> findByCallTime(Object callTime) {
		return findByProperty(CALL_TIME, callTime);
	}

	public List<DailyHotCost> findByM1Out(Object m1Out) {
		return findByProperty(M1_OUT, m1Out);
	}

	public List<DailyHotCost> findByM1In(Object m1In) {
		return findByProperty(M1_IN, m1In);
	}

	public List<DailyHotCost> findByM2Out(Object m2Out) {
		return findByProperty(M2_OUT, m2Out);
	}

	public List<DailyHotCost> findByM2In(Object m2In) {
		return findByProperty(M2_IN, m2In);
	}

	public List<DailyHotCost> findByMkUpFrom(Object mkUpFrom) {
		return findByProperty(MK_UP_FROM, mkUpFrom);
	}

	public List<DailyHotCost> findByMkUpTo(Object mkUpTo) {
		return findByProperty(MK_UP_TO, mkUpTo);
	}

	public List<DailyHotCost> findByTrvlToLocFrom(Object trvlToLocFrom) {
		return findByProperty(TRVL_TO_LOC_FROM, trvlToLocFrom);
	}

	public List<DailyHotCost> findByTrvlToLocTo(Object trvlToLocTo) {
		return findByProperty(TRVL_TO_LOC_TO, trvlToLocTo);
	}

	public List<DailyHotCost> findByTrvlFromLocFrom(Object trvlFromLocFrom) {
		return findByProperty(TRVL_FROM_LOC_FROM, trvlFromLocFrom);
	}

	public List<DailyHotCost> findByTrvlFromLocTo(Object trvlFromLocTo) {
		return findByProperty(TRVL_FROM_LOC_TO, trvlFromLocTo);
	}

	public List<DailyHotCost> findByWrap(Object wrap) {
		return findByProperty(WRAP, wrap);
	}

	public List<DailyHotCost> findByHours(Object hours) {
		return findByProperty(HOURS, hours);
	}

	public List<DailyHotCost> findByWorked(Object worked) {
		return findByProperty(WORKED, worked);
	}

	public List<DailyHotCost> findByWorkZone(Object workZone) {
		return findByProperty(WORK_ZONE, workZone);
	}

	public List<DailyHotCost> findByDayType(Object dayType) {
		return findByProperty(DAY_TYPE, dayType);
	}

	public List<DailyHotCost> findByPhase(Object phase) {
		return findByProperty(PHASE, phase);
	}

	public List<DailyHotCost> findByNoStartForm(Object noStartForm) {
		return findByProperty(NO_START_FORM, noStartForm);
	}

	public List<DailyHotCost> findByOpposite(Object opposite) {
		return findByProperty(OPPOSITE, opposite);
	}

	public List<DailyHotCost> findByOffProduction(Object offProduction) {
		return findByProperty(OFF_PRODUCTION, offProduction);
	}

	public List<DailyHotCost> findByForcedCall(Object forcedCall) {
		return findByProperty(FORCED_CALL, forcedCall);
	}

	public List<DailyHotCost> findByNonDeductMeal(Object nonDeductMeal) {
		return findByProperty(NON_DEDUCT_MEAL, nonDeductMeal);
	}

	public List<DailyHotCost> findByNonDeductMeal2(Object nonDeductMeal2) {
		return findByProperty(NON_DEDUCT_MEAL2, nonDeductMeal2);
	}

	public List<DailyHotCost> findByNonDeductMealPayroll(Object nonDeductMealPayroll) {
		return findByProperty(NON_DEDUCT_MEAL_PAYROLL, nonDeductMealPayroll);
	}

	public List<DailyHotCost> findByNonDeductMeal2Payroll(Object nonDeductMeal2Payroll) {
		return findByProperty(NON_DEDUCT_MEAL2_PAYROLL, nonDeductMeal2Payroll);
	}

	public List<DailyHotCost> findByNdbEnd(Object ndbEnd) {
		return findByProperty(NDB_END, ndbEnd);
	}

	public List<DailyHotCost> findByNdmStart(Object ndmStart) {
		return findByProperty(NDM_START, ndmStart);
	}

	public List<DailyHotCost> findByNdmEnd(Object ndmEnd) {
		return findByProperty(NDM_END, ndmEnd);
	}

	public List<DailyHotCost> findByLastManIn(Object lastManIn) {
		return findByProperty(LAST_MAN_IN, lastManIn);
	}

	public List<DailyHotCost> findByMpvUser(Object mpvUser) {
		return findByProperty(MPV_USER, mpvUser);
	}

	public List<DailyHotCost> findByGrace1(Object grace1) {
		return findByProperty(GRACE1, grace1);
	}

	public List<DailyHotCost> findByGrace2(Object grace2) {
		return findByProperty(GRACE2, grace2);
	}

	public List<DailyHotCost> findByCameraWrap(Object cameraWrap) {
		return findByProperty(CAMERA_WRAP, cameraWrap);
	}

	public List<DailyHotCost> findByFrenchHours(Object frenchHours) {
		return findByProperty(FRENCH_HOURS, frenchHours);
	}

	public List<DailyHotCost> findByLocationCode(Object locationCode) {
		return findByProperty(LOCATION_CODE, locationCode);
	}

	public List<DailyHotCost> findByProdEpisode(Object prodEpisode) {
		return findByProperty(PROD_EPISODE, prodEpisode);
	}

	public List<DailyHotCost> findByAcctSet(Object acctSet) {
		return findByProperty(ACCT_SET, acctSet);
	}

	public List<DailyHotCost> findByAcctFree(Object acctFree) {
		return findByProperty(ACCT_FREE, acctFree);
	}

	public List<DailyHotCost> findByReRate(Object reRate) {
		return findByProperty(RE_RATE, reRate);
	}

	public List<DailyHotCost> findByOccCode(Object occCode) {
		return findByProperty(OCC_CODE, occCode);
	}

	public List<DailyHotCost> findByCity(Object city) {
		return findByProperty(CITY, city);
	}

	public List<DailyHotCost> findByState(Object state) {
		return findByProperty(STATE, state);
	}

	public List<DailyHotCost> findByMpv1Payroll(Object mpv1Payroll) {
		return findByProperty(MPV1_PAYROLL, mpv1Payroll);
	}

	public List<DailyHotCost> findByMpv2Payroll(Object mpv2Payroll) {
		return findByProperty(MPV2_PAYROLL, mpv2Payroll);
	}

	public List<DailyHotCost> findByMpv3Payroll(Object mpv3Payroll) {
		return findByProperty(MPV3_PAYROLL, mpv3Payroll);
	}

	public List<DailyHotCost> findBySplitByPercent(Object splitByPercent) {
		return findByProperty(SPLIT_BY_PERCENT, splitByPercent);
	}

	public List<DailyHotCost> findByJobNum1(Object jobNum1) {
		return findByProperty(JOB_NUM1, jobNum1);
	}

	public List<DailyHotCost> findBySplitStart2(Object splitStart2) {
		return findByProperty(SPLIT_START2, splitStart2);
	}

	public List<DailyHotCost> findByJobNum2(Object jobNum2) {
		return findByProperty(JOB_NUM2, jobNum2);
	}

	public List<DailyHotCost> findBySplitStart3(Object splitStart3) {
		return findByProperty(SPLIT_START3, splitStart3);
	}

	public List<DailyHotCost> findByJobNum3(Object jobNum3) {
		return findByProperty(JOB_NUM3, jobNum3);
	}

	public List<DailyHotCost> findByTalentInitialId(Object talentInitialId) {
		return findByProperty(TALENT_INITIAL_ID, talentInitialId);
	}

	public List<DailyHotCost> findByActualCost(Object actualCost) {
		return findByProperty(ACTUAL_COST, actualCost);
	}

	public List<DailyHotCost> findByBudgetedHours(Object budgetedHours) {
		return findByProperty(BUDGETED_HOURS, budgetedHours);
	}

	public List<DailyHotCost> findByBudgetedCost(Object budgetedCost) {
		return findByProperty(BUDGETED_COST, budgetedCost);
	}

	public List<DailyHotCost> findByBudgetedMvps(Object budgetedMvps) {
		return findByProperty(BUDGETED_MVPS, budgetedMvps);
	}

	public List<DailyHotCost> findByBudgetedMvpsCost(Object budgetedMvpsCost) {
		return findByProperty(BUDGETED_MVPS_COST, budgetedMvpsCost);
	}

	public DailyHotCost findByWeeklyHotCostAndDate(WeeklyHotCosts whc, Date date) {
		String query = " from DailyHotCost where weeklyHotCosts = ? and date = ?";
		List<Object> objects = new ArrayList<>();

		objects.add(whc);
		objects.add(date);

		return findOne(query, objects.toArray());
	}

	/**
	 * Find the Daily Hot Costs associated for this Weekly Hot Costs
	 * @param weeklyHotCosts
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DailyHotCost> findByWeeklyHotCosts(WeeklyHotCosts weeklyHotCosts) {
		String query = " from DailyHotCost where weeklyHotCosts = ?";
		List<Object> objects = new ArrayList<>();

		objects.add(weeklyHotCosts);
		return find(query, objects);
	}

	/**
	 * Find the previous Daily Hot Costs object for this weekly hot costs
	 * @param weeklyHotCosts
	 * @param currentDate - Find Daily Hots Costs objects before this date for the week
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<DailyHotCost> findByWeeklyHotCostsPreviousDays(WeeklyHotCosts weeklyHotCosts, Date currentDate) {
		String query = " from DailyHotCost where weeklyHotCosts = ? and date < ? order by dayNum";
		List<Object> objects = new ArrayList<>();

		objects.add(weeklyHotCosts);
		objects.add(currentDate);

		return find(query, objects.toArray());
	}

	public static DailyHotCostDAO getFromApplicationContext(ApplicationContext ctx) {
		return (DailyHotCostDAO) ctx.getBean("DailyHotCostDAO");
	}
}