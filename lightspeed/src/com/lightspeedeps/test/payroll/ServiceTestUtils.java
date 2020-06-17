/**
 * ServiceTestUtils.java
 */
package com.lightspeedeps.test.payroll;

import java.util.ArrayList;
import java.util.List;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.GoldenRuleDAO;
import com.lightspeedeps.dao.GuaranteeRuleDAO;
import com.lightspeedeps.dao.HolidayRuleDAO;
import com.lightspeedeps.dao.NtPremiumRuleDAO;
import com.lightspeedeps.dao.OvertimeRuleDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.WeeklyRuleDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContractRule;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.PayJob;
import com.lightspeedeps.model.PayJobDaily;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.service.HtgData;
import com.lightspeedeps.service.PayJobService;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.RuleType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * A class to hold utility methods for the JUnit payroll tests.
 */
public class ServiceTestUtils {

	/**
	 * private constructor - all methods are static
	 */
	private ServiceTestUtils() {
	}

	/**
	 * A method used by JUnit tests to check the
	 * {@link PayJobService#calculateWorkDayNumbers(WeeklyTimecard, HtgData)} functionality.
	 *
	 * @param wtc The WeeklyTimecard whose day numbers are to be filled in.
	 * @param weekdayNum The sequential day number (Sun=1) that is the first day
	 *            of the "producer's work-week". Typically '2' for a Mon-Fri
	 *            work week.
	 */
	public static void calculateWorkDayNumbersTest(WeeklyTimecard wtc, WeeklyTimecard priorWtc, int weekdayNum, boolean flexible, boolean floating) {
		TimecardService service = new TimecardService(wtc);

		service.getHtg().preference.setFirstWorkWeekDay(weekdayNum);
		service.getHtg().priorTimecard = priorWtc;
		service.getHtg().setFlexible6thDay(flexible || floating);
		service.getHtg().setFloatingWeek(floating);

		PayJobService.calculateWorkDayNumbers(wtc, service.getHtg());
	}

	/**
	 * Test a Night-Premium rule.
	 *
	 * @param npRuleKey The name(key) of the Night Premium rule to be tested.
	 * @param otRuleKey The name(key) of the Overtime rule to be used.
	 * @param goldRuleKey The name(key) of the Golden rule to be used.
	 * @param dt The DailyTime entry whose hours will be analyzed.
	 * @return A PayJobDaily showing the breakdown of the day's hours.
	 */
	public static PayJobDaily nightPremiumTest(String npRuleKey, String otRuleKey,
			String goldRuleKey, DailyTime dt) {

		return breakdownTest(otRuleKey, goldRuleKey, npRuleKey, dt);
	}

	/**
	 * Test a set of breakdown rules against one day's data.
	 *
	 * @param otRuleKey The name(key) of the Overtime rule to be used.
	 * @param goldRuleKey The name(key) of the Golden rule to be used.
	 * @param npRuleKey The name(key) of the Night Premium rule to be tested.
	 * @param dt The DailyTime entry whose hours will be analyzed.
	 * @return A PayJobDaily showing the breakdown of the day's hours.
	 */
	public static PayJobDaily breakdownTest(String otRuleKey, String goldRuleKey,
			String npRuleKey, DailyTime dt) {

		dt.setWorkDayNum((byte)1);
		TimecardCalc.calculateDailyHours(dt); // calculate worked hours

		WeeklyTimecard wtc = TimecardUtils.createBlankTimecard();
		Production prod = ProductionDAO.getInstance().findById(Constants.SYSTEM_PRODUCTION_ID);
		if (prod != null) {
			wtc.setProdId(prod.getProdId());
		}
		wtc.getDailyTimes().set(1, dt);	// replace Monday's DailyTime with the one supplied
		dt.setWeeklyTimecard(wtc);
		StartForm sf = createTestStartForm(prod);
		wtc.setStartForm(sf);

		// Create a single PayJob entry to be filled in
		createTestPayJob(wtc);

		TimecardService tcService = new TimecardService(wtc);

		PayJobService pjService = new PayJobService(tcService.getHtg(), null);
		pjService.setUnitTest(true);

		// get the rules using the supplied rule keys
		pjService.setGoldenRule(GoldenRuleDAO.getInstance().findOneByRuleKey(goldRuleKey));
		pjService.setGuaranteeRule(GuaranteeRuleDAO.getInstance().findOneByRuleKey("GT-NONE"));
		pjService.setHolidayRule(HolidayRuleDAO.getInstance().findOneByRuleKey("HO-STD-P"));
		pjService.setNpRule(NtPremiumRuleDAO.getInstance().findOneByRuleKey(npRuleKey));
		pjService.setOvertimeRule(OvertimeRuleDAO.getInstance().findOneByRuleKey(otRuleKey));
		pjService.setWeeklyRule(WeeklyRuleDAO.getInstance().findOneByRuleKey("WK-STD-40"));

		// Apply the rule(s) to the given DailyTime; fills in hours
		pjService.fillPayJobDaily(dt);

		// return the "Monday" daily entry from the PayJob
		return wtc.getPayJobs().get(0).getPayJobDailies().get(1);
	}

	/**
	 * Generate a new StartForm for testing.
	 *
	 * @param prod The Production with which the StartForm should be associated.
	 * @return A new StartForm for testing; returns null if no Employment
	 *         instances exist.
	 */
	private static StartForm createTestStartForm(Production prod) {
		List<Contact> contacts = ContactDAO.getInstance().findByProductionActive(prod);
		Contact c = contacts.get(0);
		StartForm sf = null;
		if (c.getEmployments().size() > 0) {
			StartFormService.createFirstStartForm(c, c.getEmployments().iterator().next());
		}
		return sf;
	}

	/**
	 * Test a set of breakdown rules against a full timecard. It will create a
	 * PayJob showing the breakdown of the week's hours and add it to the given
	 * timecard. It also sets WeeklyTimecard.jobHoursDiffer.
	 *
	 * @param weeklyRuleKey The name(key) of the Weekly rule to be used.
	 * @param otRuleKey The name(key) of the Overtime rule to be used.
	 * @param goldRuleKey The name(key) of the Golden rule to be used.
	 * @param npRuleKey The name(key) of the Night Premium rule to be tested.
	 * @param wtc The timecard whose hours will be analyzed.
	 */
	public static void breakdownTest(String weeklyRuleKey, String otRuleKey, String goldRuleKey,
			String npRuleKey, WeeklyTimecard wtc) {

		Production prod = ProductionDAO.getInstance().findById(Constants.SYSTEM_PRODUCTION_ID);
		if (prod != null) {
			wtc.setProdId(prod.getProdId());
		}

		// Create a single PayJob entry to be filled in
		createTestPayJob(wtc);

		TimecardService tcService = new TimecardService(wtc);

		PayJobService pjService = new PayJobService(tcService.getHtg(), null);
		pjService.setUnitTest(true);

		List<ContractRule> contractRules = new ArrayList<>();
		String DEF_CB_RULE = "CB-4-15X";
		String DEF_HOLIDAY_RULE = "HO-STD-P";
		String DEF_MPV_RULE = "MP-LA";
		String DEF_REST_RULE = "RST-8-PR";

		ContractRule cr;
		cr = new ContractRule(RuleType.GL, goldRuleKey);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.NP, npRuleKey);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.OT, otRuleKey);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.BA, weeklyRuleKey);
		contractRules.add(cr);

		// Add default rules
		cr = new ContractRule(RuleType.CB, DEF_CB_RULE);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.HO, DEF_HOLIDAY_RULE);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.MP, DEF_MPV_RULE);
		contractRules.add(cr);
		cr = new ContractRule(RuleType.RS, DEF_REST_RULE);
		contractRules.add(cr);

		tcService.getHtg().ruleService.setContractRules(contractRules );

		// Apply the rule(s) to the given DailyTime; fills in hours
		pjService.fillPayJobsValid();
	}

	/**
	 * Create a PayJob, initialize it with a set of PayJobDaily entries, and make
	 * it the only PayJob in the given timecard.
	 *
	 * @param wtc The timecard to which the PayJob will be added.
	 */
	private static void createTestPayJob(WeeklyTimecard wtc) {
		// Create a single PayJob entry to be filled in
		PayJob pj = new PayJob(wtc, (byte)1);
		PayJobDaily.createPayJobDailies(pj, wtc.getEndDate());

		List<PayJob> payJobs = new ArrayList<>();
		payJobs.add(pj);
		wtc.setPayJobs(payJobs);
	}

}
