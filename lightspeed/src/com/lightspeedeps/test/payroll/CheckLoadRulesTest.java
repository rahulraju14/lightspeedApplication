package com.lightspeedeps.test.payroll;

import java.util.List;

import com.lightspeedeps.dao.CallBackRuleDAO;
import com.lightspeedeps.dao.ContractRuleDAO;
import com.lightspeedeps.dao.GoldenRuleDAO;
import com.lightspeedeps.dao.GuaranteeRuleDAO;
import com.lightspeedeps.dao.HolidayListRuleDAO;
import com.lightspeedeps.dao.HolidayRuleDAO;
import com.lightspeedeps.dao.MpvRuleDAO;
import com.lightspeedeps.dao.OnCallRuleDAO;
import com.lightspeedeps.dao.OvertimeRuleDAO;
import com.lightspeedeps.dao.RestRuleDAO;
import com.lightspeedeps.dao.WeeklyRuleDAO;
import com.lightspeedeps.model.CallBackRule;
import com.lightspeedeps.model.ContractRule;
import com.lightspeedeps.model.GoldenRule;
import com.lightspeedeps.model.GuaranteeRule;
import com.lightspeedeps.model.HolidayListRule;
import com.lightspeedeps.model.HolidayRule;
import com.lightspeedeps.model.MpvRule;
import com.lightspeedeps.model.OnCallRule;
import com.lightspeedeps.model.OvertimeRule;
import com.lightspeedeps.model.RestRule;
import com.lightspeedeps.model.WeeklyRule;
import com.lightspeedeps.test.SpringMultiTestCase;

/**
 * This class tests to see if all the rules are load-able; basically
 * this ensures that any enum values in the database are valid.
 * <p>
 *
 */
public class CheckLoadRulesTest extends SpringMultiTestCase {


	public void testCallBackRules() throws Exception {
		List<CallBackRule> list = CallBackRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testContractRules() throws Exception {
		List<ContractRule> list = ContractRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testGoldenRules() throws Exception {
		List<GoldenRule> list = GoldenRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testGuaranteeRules() throws Exception {
		List<GuaranteeRule> list = GuaranteeRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testHolidayRules() throws Exception {
		List<HolidayRule> list = HolidayRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testHolidayListRules() throws Exception {
		List<HolidayListRule> list = HolidayListRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testMpvRules() throws Exception {
		List<MpvRule> list = MpvRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testOnCallRules() throws Exception {
		List<OnCallRule> list = OnCallRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testOvertimeRules() throws Exception {
		List<OvertimeRule> list = OvertimeRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testRestRules() throws Exception {
		List<RestRule> list = RestRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}

	public void testWeeklyRules() throws Exception {
		List<WeeklyRule> list = WeeklyRuleDAO.getInstance().findAll();
		assert(list.size() > 0);
	}



	/**
	 * This should be the last test method in the class, so that the JUnit test
	 * structure will run it after all the other tests. It calls the superclass
	 * method to clean up (release) the Spring/Hibernate environment.
	 *
	 * @throws Exception
	 */
	public void testSpringTearDown() throws Exception {
		springTearDown();
	}


}
