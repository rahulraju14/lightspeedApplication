package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.common.StringUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * ContractRule entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link BaseDAO} or {@link BaseTypeDAO}.
 * <p>
 * In addition, for DAOs of entities that subclass Rule, some common access
 * functions reside in {@link RuleDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ContractRule
 */
public class ContractRuleDAO extends RuleDAO<ContractRule> {
	private static final Log log = LogFactory.getLog(ContractRuleDAO.class);

	//property constants
	private static final String AGREEMENT = "agreement";
	private static final String UNION_KEY = "unionKey";
	private static final String OCC_CODE = "occCode";
	private static final String SCHEDULE = "schedule";
//	private static final String LOCALITY = "locality";
//	private static final String ON_PRODUCTION = "onProduction";
	private static final String MEDIUM = "medium";
//	private static final String DAY_TYPE = "dayType";
//	private static final String DAY_NUMBER = "dayNumber";
//	private static final String EXTRA_FILTERS = "extraFilters";
//	private static final String RULE_TYPE = "ruleType";
//	private static final String USE_RULE_KEY = "useRuleKey";
//	private static final String PRIORITY = "priority";


	public static ContractRuleDAO getInstance() {
		return (ContractRuleDAO)getInstance("ContractRuleDAO");
	}

	/**
	 * Find all the Contract Rules that apply to a given StartForm and
	 * WeeklyTimecard. The rules returned may include both system-wide and
	 * Production-specific ones.
	 *
	 * @param prod The Production whose rules will be used. If 'useContracts' is
	 *            true, this production will also determine the set of contracts
	 *            (and their rules) available.
	 * @param sf The StartForm of interest.
	 * @param wtc The WeeklyTimecard of interest.
	 * @param useContracts If false, only state and/or federal rules will be
	 *            returned. If true, then the StartForm's contract code will be
	 *            searched for in the list of contracts assigned to the given
	 *            production; if it is not found, an empty List will be
	 *            returned; if it is found, then rules matching that contract,
	 *            plus rules not requiring a contract, will be returned.
	 *            returned.
	 * @return A non-null, but possibly empty, List of rules to be applied to
	 *         the timecard, sorted in ascending order of priority. Note that
	 *         the list will need to be filtered by additional information for
	 *         each day of the week.
	 */
	@SuppressWarnings("unchecked")
	public List<ContractRule> findByStartFormTimecard(Production prod, StartForm sf, WeeklyTimecard wtc, boolean useContracts) {
		// Determine the list of contract codes to include
		String contractCodes;
		String slQuery = "";
		if (useContracts) { // Use contract-based rules filtering
			contractCodes = createContractCodes(prod, sf, wtc);
			if (contractCodes.length() == 0) {
				return new ArrayList<>();
			}
			slQuery = "";
			String sls = prod.getContractSideLetters().get(contractCodes);
			if (! StringUtils.isEmpty(sls)) {
				String[] slList = sls.split(",");
				for (int i = 0; i < slList.length; i++) {
					slQuery += ",'" + slList[i] + "'";
				}
				slQuery = slQuery.substring(1);
			}
			contractCodes = "'" + contractCodes + "'";
		}
		else { // Use only state/federal rules (for non-union)
			contractCodes = createStateCodes(prod, wtc);
		}
		String agreement = "";
		if (contractCodes.trim().length() > 0 ) {
			agreement = "(" + AGREEMENT + " in (" + contractCodes + ") and ( sideLetter is null ";
			if (slQuery.length() > 0) {
				agreement += " or sideLetter in (" + slQuery + ")";
			}
			agreement += ") ) or ";
		}
		agreement += AGREEMENT + " = 'N_A'";

		String schedule;
		String scheduleLike = "";
		if (wtc.isNonUnion()) {
			if (wtc.getAllowWorked()) {
				schedule = quote(ContractRule.SCHEDULE_NON_UNION_EXEMPT) + ",";
				if (wtc.getEmployeeRateType() == EmployeeRateType.DAILY) {
					schedule += quote(ContractRule.SCHEDULE_NON_UNION_DAILY) + ",";
				}
				else {
					schedule += quote(ContractRule.SCHEDULE_NON_UNION_WEEKLY) + ",";
				}
			}
			else {
				schedule = quote(ContractRule.SCHEDULE_NON_UNION_HOURLY) + ",";
			}
		}
		else {
			String cs = sf.getContractSchedule();
			schedule = quote(cs) + ",";
			scheduleLike = " or ( schedule like '%," + cs + "' )" +
					" or ( schedule like '" + cs + ",%' )" +
					" or ( schedule like '%," + cs + ",%' )";
				// or ( schedule like '%,A1' ) or ( schedule like 'A1,%' ) or ( schedule like '%,A1,%' )
		}

		String query = "from ContractRule where "
				+ " (" + PRODUCTION_ID + " = ? or " + PRODUCTION_ID + " = 1) and "
				+ " (" + agreement + ") and "
				+ " (" + UNION_KEY + " = ? or " 	+ UNION_KEY + " = 'N_A') and "
				+ " (" + OCC_CODE + " = ? or " 		+ OCC_CODE + " = 'N_A' or " + OCC_CODE + " like ? ) and "
				+ " ( (" + SCHEDULE + " in (" + schedule + " 'N_A') )"
						+ scheduleLike // optional "like" phrases for schedule match
						+ ") and "
				+ " (" + MEDIUM + " = ? or " 		+ MEDIUM + " =  'N_A' )"
				+ " order by rule_type, priority, id ";

		String likeOccCode = '%' + wtc.getLsOccCode() + '%';

		Object[] values = {prod.getId(), sf.getUnionKey(), wtc.getLsOccCode(), likeOccCode,
				prod.getPayrollPref().getMediumType() };

		List<ContractRule> rules = find(query, values);

		return rules;
	}

	/**
	 * Determine the "contract codes" string; this is the 3.0 version which DOES
	 * use the ContractProduction table. Contracts must be "assigned" to the
	 * production for this code to find the rules to be applied.
	 *
	 * @param prod The production to which the timecard belongs.
	 * @param sf The StartForm that applies to the timecard.
	 * @param wtc The timecard whose "contract codes" are to be determined.
	 * @return The string of contract codes applicable to this timecard and
	 *         StartForm.
	 */
	private String createContractCodes(Production prod, StartForm sf, WeeklyTimecard wtc) {
		String contractCodes = "";
		if (sf.getContractCode() != null && sf.getContractCode().length() > 0) { // Start has one
			for (Contract c : prod.getContracts()) {
				// only allowed if it matches one of the Production's codes...
				if (sf.getContractCode().equals(c.getContractCode())) {
					contractCodes = sf.getContractCode().trim();
					break;
				}
			}
		}
		if (contractCodes.length() == 0) {
			log.warn("No match (in production's contracts) for Contract code=" +
					sf.getContractCode());
		}
		return contractCodes;
	}

	/**
	 * Generate a list of state codes, for use in an SQL query, for those states
	 * that apply to the given StartForm and WeeklyTimecard.
	 *
	 * @param sf The StartForm of interest.
	 * @param wtc The WeeklyTimecard of interest.
	 * @return A non-null, but possibly empty, String which is a comma-delimited
	 *         list of state codes, each state code enclosed in single quotes
	 *         (primes). If the StartForm specifies using a particular state,
	 *         then the returned string will contain only that state code. If
	 *         the StartForm specifies using "worked state", then the returned
	 *         string will contain all the distinct state codes from the
	 *         timecard's {@link DailyTime#state} values and from the
	 *         {@link WeeklyTimecard#stateWorked} field.
	 */
	private String createStateCodes(Production prod, WeeklyTimecard wtc) {
		String codes = "";
		if (true /*wtc.isNonUnion()*/) { // changed for 2.9 so Union TCs can follow state law
			if (wtc.getUseStateWorked()) {
				if ( wtc.getStateWorked() != null) {
					codes = "," + quote(wtc.getStateWorked());
				}
				// get "worked states" for all days
				for (DailyTime dt : wtc.getDailyTimes()) {
					if (dt.getState() != null) {
						if (codes.indexOf(dt.getState()) < 0) {
							codes += "," + quote(dt.getState());
						}
					}
				}
			}
			else { // use a single specific state
				codes = "," + quote(wtc.getWageState());
			}
		}
		if (wtc.isNonUnion()) {
			for (Contract c : prod.getContracts()) {
				// only add contracts designated as "non-union"
				if (c.getContractCode().startsWith(Contract.CONTRACT_CODE_NON_UNION)) {
					codes += ",'" + c.getContractCode().trim() + "'";
				}
			}
		}
		if (codes.length() > 1) {
			codes = codes.substring(1); // remove leading ","
		}
		return codes;
	}

	/**
	 * @param text
	 * @return text enclosed in single quotes (primes)
	 */
	private String quote(String text) {
		return '\'' + text + '\'';
	}

//	/**
//	 * NOT CURRENTLY USED.
//	 * Find all the Contract Rules of a specific RuleType that apply to a given
//	 * StartForm and WeeklyTimecard. The rules returned may include both
//	 * system-wide and Production-specific ones.
//	 *
//	 * @param prod The Production whose rules will be used.
//	 * @param sf The StartForm of interest.
//	 * @param wtc The WeeklyTimecard of interest.
//	 * @param rType The RuleType of the desired rules.
//	 * @return A non-null, but possibly empty, List of rules to be applied to
//	 *         the timecard. Note that the list will need to be filtered by
//	 *         additional information for each day of the week.
//	 */
//	@SuppressWarnings({ "unchecked", "unused" })
//	private List<ContractRule> findByStartFormTimecardType(Production prod, StartForm sf, WeeklyTimecard wtc,
//			RuleType rType) {
//		String query = "from ContractRule where "
//				+ " (" + RULE_TYPE + " = ? ) and "
//				+ " (" + PRODUCTION_ID + " = ? or " + PRODUCTION_ID + " = 1) and "
//				+ " (" + AGREEMENT + " = ? or " 	+ AGREEMENT + " = 'N_A') and "
//				+ " (" + UNION_KEY + " = ? or " 	+ UNION_KEY + " = 'N_A') and "
//				+ " (" + OCC_CODE + " = ? or " 		+ OCC_CODE + " = 'N_A') and "
//				+ " (" + SCHEDULE + " = ? or " 		+ SCHEDULE + " = 'N_A') and "
//				+ " (" + MEDIUM + " = ? or " 		+ MEDIUM + " =  'N_A' )"
//				+ " order by rule_type, priority, id ";
//
//		Object[] values = {rType, prod.getId(),
//				sf.getContractCode(), sf.getUnionKey(), sf.getLsOccCode(), sf.getContractSchedule(),
//				prod.getPayrollPref().getMediumType() };
//
//		List<ContractRule> rules = find(query, values);
//
//		return rules;
//	}

}
