package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayRate;
import com.lightspeedeps.util.app.Constants;

/**
 * A data access object (DAO) providing persistence and search support for
 * PayRate entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.PayRate
 */
public class PayRateDAO extends BaseTypeDAO<PayRate> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayRateDAO.class);

	//property constants
	private static final String START_DATE = "startDate";
	private static final String END_DATE = "endDate";
//	private static final String CONTRACT_CODE = "contractCode";
	private static final String CONTRACT_RATE_KEY = "contractRateKey";
//	private static final String SCHEDULE = "schedule";
	private static final String CONTRACT_SCHEDULE = "contractSchedule";
	private static final String OCC_CODE = "occCode";
//	private static final String LOCALITY = "Locality";
//	private static final String HOURLY_RATE = "hourlyRate";
//	private static final String DAILY_RATE = "dailyRate";
//	private static final String WEEKLY_RATE = "weeklyRate";

	public static PayRateDAO getInstance() {
		return (PayRateDAO)getInstance("PayRateDAO");
	}

//	/**
//	 * Creates a List of SelectItem entries showing contract Schedule types
//	 * available to the given occupation code as of a given date.
//	 *
//	 * @param date The date that the rate should include; if null, today's date
//	 *            is used.
//	 * @param lsOccCode The LS occupation code.
//	 * @return A non-null, but possibly empty, SelectItem list. The value field
//	 *         is the database id of the PayRate object, and the label is the
//	 *         payRate.schedule field.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<SelectItem> createScheduleDL(Date date, String lsOccCode) {
//		List<SelectItem> list = new ArrayList<SelectItem>();
//		String queryString = "from PayRate where " +
//				START_DATE + " <= ? and " +
//				END_DATE + ">= ? and " +
//				OCC_CODE + " = ? order by " + CONTRACT_SCHEDULE ;
//
//		if (date == null) {
//			date = new Date(); // use today's date
//		}
//		Object[] values = { date, date, lsOccCode };
//		List<PayRate> payRates = find(queryString, values);
//		if (payRates.size() == 0) { // try again without the date restriction
//			queryString = "from PayRate where " + OCC_CODE + " = ? order by " + CONTRACT_SCHEDULE ;
//			payRates = find(queryString, lsOccCode);
//		}
//		String lastSchedule = "zzzz";
//		for (PayRate payrate : payRates) {
//			// skip duplicate entries when building SelectItem list
//			if (! lastSchedule.equals(payrate.getContractSchedule())) {
//				if (Constants.USE_CONTRACT_SCHEDULES) {
//					list.add(new SelectItem(payrate.getId(), payrate.getContractSchedule()));
//				}
//				else {
//					list.add(new SelectItem(payrate.getId(), payrate.getSchedule()));
//				}
//				lastSchedule = payrate.getContractSchedule();
//			}
//		}
//		return list;
//	}

	/**
	 * Creates a List of SelectItem entries showing contract Schedule types
	 * available to the given occupation code when being paid under the
	 * specified contract.
	 *
	 * @param date The date that the rate should include; if null, today's date
	 *            is used.
	 * @param contractRateKey The code describing the contract plus relevant
	 *            production type details that affect the PayRate item(s)
	 *            selected. (More detailed than the "contract code".) If null,
	 *            this parameter is ignored.
	 * @param lsOccCode The LS occupation code.
	 * @return A non-null, but possibly empty, SelectItem list. The value field
	 *         is the database id of the PayRate object, and the label is the
	 *         payRate.schedule field.
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> createScheduleDL(Date date, String contractRateKey, String lsOccCode) {
		String queryString = "from PayRate where " +
				START_DATE + " <= ? and " +
				END_DATE + ">= ? and " +
				OCC_CODE + " = ? and " +
				CONTRACT_RATE_KEY + " = ? " +
				" order by " + CONTRACT_SCHEDULE ;

		if (date == null) {
			date = new Date(); // use today's date
		}
		Object[] values = { date, date, lsOccCode, contractRateKey };
		List<PayRate> payrates = find(queryString, values);
		if (payrates.size() == 0) { // try again without the date restriction
			queryString = "from PayRate where " + OCC_CODE + " = ? and " +
					CONTRACT_RATE_KEY + " = ? " +
					" order by " + CONTRACT_SCHEDULE ;
			Object[] values2 = { lsOccCode, contractRateKey };
			payrates = find(queryString, values2);
		}
		return createScheduleDL(payrates);
	}

	/**
	 * Create a drop-down list of Schedule values for non-union roles.
	 *
	 * @param contractRateKey The code describing the contract plus relevant
	 *            production type details that affect the PayRate item(s)
	 *            selected. (More detailed than the "contract code".)
	 * @return A non-null List of SelectItem`s, where the label is the schedule
	 *         name, and the value is the database id of a PayRate entry that
	 *         contains that schedule.
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> createNonUnionScheduleDL(String contractRateKey) {
		String queryString = "from PayRate where " +
				CONTRACT_RATE_KEY + " = ? ";

		Object[] values = { contractRateKey };
		queryString += " order by " + CONTRACT_SCHEDULE ;
		List<PayRate> payrates = find(queryString, values);
		return createScheduleDL(payrates);
	}

	/**
	 * Given a List of PayRate objects, build a corresponding List of
	 * SelectItem`s for a drop-down list.
	 *
	 * @param payrates The non-null List of PayRates.
	 * @return A non-null List of SelectItem`s, where the label is the schedule
	 *         name, and the value is the database id of a PayRate entry that
	 *         contains that schedule.
	 */
	private List<SelectItem> createScheduleDL(List<PayRate> payrates) {
		List<SelectItem> list = new ArrayList<SelectItem>();
		String lastSchedule = "zzzz";
		for (PayRate payrate : payrates) {
			// skip duplicate entries when building SelectItem list
			if (! lastSchedule.equals(payrate.getContractSchedule())) {
				if (Constants.USE_CONTRACT_SCHEDULES) {
					list.add(new SelectItem(payrate.getId(), payrate.getContractSchedule()));
				}
				else {
					list.add(new SelectItem(payrate.getId(), payrate.getSchedule()));
				}
				lastSchedule = payrate.getContractSchedule();
			}
		}
		return list;
	}

	/**
	 * Find the PayRate instances with the matching parameters currently in
	 * effect, that is, today's date is within the effective date range of the
	 * PayRate.
	 * <p>
	 * A pair of PayRates is returned, the first entry is for Studio rates and
	 * the second entry is for Distant (Location) rates.
	 *
	 * @param date The date that the rate should include; if null, today's date
	 *            is used.
	 * @param lsOccCode The LS occupation code (must be non-null).
	 * @param scheduleCode The schedule code (e.g., "01").
	 * @return The matching PayRate items, or null if no match is found.
	 */
	@SuppressWarnings("unchecked")
	public PayRate[] findByOccCodeAndSchedule(Date date, String lsOccCode, String scheduleCode) {
		String queryString = "from PayRate where " +
				START_DATE + " <= ? and " +
				END_DATE + ">= ? and " +
				OCC_CODE + " = ?  and " +
				CONTRACT_SCHEDULE + " = ? order by endDate, locality ";

		if (date == null) {
			date = new Date(); // use today's date
		}
		Object[] values = { date, date, lsOccCode, scheduleCode };
		List<PayRate> payRates = find(queryString, values);
		if (payRates.size() == 0) { // try again without the date restriction
			queryString = "from PayRate where " +
					OCC_CODE + " = ?  and " +
					CONTRACT_SCHEDULE + " = ? order by endDate, locality ";
			Object[] values2 = { lsOccCode, scheduleCode };
			payRates = find(queryString, values2);
		}
		PayRate[] payRate = createPayRatePair(payRates);
		return payRate;
	}

	/**
	 * Find the PayRate instances with the matching parameters currently in
	 * effect, that is, today's date is within the effective date range of the
	 * PayRate.
	 * <p>
	 * A pair of PayRates is returned, the first entry is for Studio rates and
	 * the second entry is for Distant (Location) rates.
	 *
	 * @param date The date that the rate should include; if null, today's date
	 *            is used.
	 * @param contractRateKey The code describing the contract plus relevant
	 *            production type details that affect the PayRate item(s)
	 *            selected. (More detailed than the "contract code".)
	 * @param lsOccCode The LS occupation code (must be non-null).
	 * @param scheduleCode The schedule code (e.g., "01").
	 * @return The matching PayRate items, or null if no match is found.
	 */
	@SuppressWarnings("unchecked")
	public PayRate[] findByContractOccCodeAndSchedule(Date date, String contractRateKey, String lsOccCode, String scheduleCode) {
		String queryString = "from PayRate where " +
				START_DATE + " <= ? and " +
				END_DATE + ">= ? and " +
				CONTRACT_RATE_KEY + " = ?  and " +
				OCC_CODE + " = ?  and " +
				CONTRACT_SCHEDULE + " = ? order by endDate, locality ";

		if (date == null) {
			date = new Date(); // use today's date
		}
		Object[] values = { date, date, contractRateKey, lsOccCode, scheduleCode };
		List<PayRate> payRates = find(queryString, values);
		if (payRates.size() == 0) { // try again without the date restriction
			queryString = "from PayRate where " +
					CONTRACT_RATE_KEY + " = ?  and " +
					OCC_CODE + " = ?  and " +
					CONTRACT_SCHEDULE + " = ? order by endDate, locality ";
			Object[] values2 = { contractRateKey, lsOccCode, scheduleCode };
			payRates = find(queryString, values2);
		}
		PayRate[] payRate = createPayRatePair(payRates);
		return payRate;
	}

	/**
	 * Find the PayRate instances with the matching parameters currently in
	 * effect, that is, today's date is within the effective date range of the
	 * PayRate.
	 * <p>
	 * A pair of PayRates is returned, the first entry is for Studio rates and
	 * the second entry is for Distant (Location) rates.
	 *
	 * @param date The date that the rate should include; if null, today's date
	 *            is used.
	 * @param contractRateKey The code describing the contract plus relevant
	 *            production type details that affect the PayRate item(s)
	 *            selected. (More detailed than the "contract code".)
	 * @param lsOccCode The LS occupation code (must be non-null).
	 * @return The matching PayRate items, or null if no match is found.
	 */
	@SuppressWarnings("unchecked")
	public PayRate[] findByContractAndOccCode(Date date, String contractRateKey, String lsOccCode) {
		String queryString = "from PayRate where " +
				START_DATE + " <= ? and " +
				END_DATE + ">= ? and " +
				OCC_CODE + " = ?  and " +
				CONTRACT_RATE_KEY + " = ? order by endDate, locality ";

		if (date == null) {
			date = new Date(); // use today's date
		}
		Object[] values = { date, date, lsOccCode, contractRateKey };
		List<PayRate> payRates = find(queryString, values);
		if (payRates.size() == 0) { // try again without the date restriction
			queryString = "from PayRate where " +
					OCC_CODE + " = ?  and " +
					CONTRACT_RATE_KEY + " = ? order by endDate, locality ";
			Object[] values2 = { lsOccCode, contractRateKey };
			payRates = find(queryString, values2);
		}
		PayRate[] payRate = createPayRatePair(payRates);
		return payRate;
	}

	/**
	 * Create a pair of PayRate`s in an array, representing the Studio and
	 * Distant rates.
	 *
	 * @param payRates The list of applicable PayRate values found in the
	 *            database.
	 * @return The pair of rates from the list that have the proper
	 *         Studio/Distant settings, or null if the supplied payRates list is
	 *         empty.
	 */
	private PayRate[] createPayRatePair(List<PayRate> payRates) {
		if (payRates.size() == 0) {
			return null;
		}
		PayRate[] payRatePair = new PayRate[2];
		for (PayRate pr : payRates) {
			if (pr.getLocality() == PayRate.LOCALITY_ALL) {
				payRatePair[0] = pr;
				payRatePair[1] = pr;
			}
			else if (pr.getLocality() == PayRate.LOCALITY_STUDIO) {
				payRatePair[0] = pr;
			}
			else if (pr.getLocality() == PayRate.LOCALITY_DISTANT) {
				payRatePair[1] = pr;
			}
		}
		return payRatePair;
	}

	/**
	 * Find PayRate`s by occupation code.
	 * @param occCode The occupation code of interest.
	 * @return A List of PayRate`s matching the given occupation code.
	 */
	public List<PayRate> findByOccCode(String occCode) {
		String queryString = "from PayRate where " +
				OCC_CODE + " = ? " +
				" order by " + OCC_CODE ;
		@SuppressWarnings("unchecked")
		List<PayRate> occupations = find(queryString, occCode);
		return occupations;
	}


	@SuppressWarnings("unchecked")
	public List<PayRate>findByContractDateScheduleLocality(String contractCode, Date date, String scheduleCode, char locality ) {
		String query;
		Object[] values = {contractCode, date, date, locality};

		query = "from PayRate where contractCode = ? and " +
				START_DATE + " <= ? and " + END_DATE + " >= ? and " +
				CONTRACT_SCHEDULE + " like '%" + scheduleCode.substring(0, 1) + "%' and (locality = ? or locality = 'A') order by occCode";

		return find(query, values);
	}

//	/**
//	 * Find PayRate`s by contract code, locality, and year.
//	 * @param contract The contract code to match.
//	 * @param locality The Locality to match (Studio/Distant/All).
//	 * @param year the year of interest -- the PayRate's returned must have a
//	 *            Start date sometime within the given year.
//	 * @return A List of PayRate`s matching the given contract code,locality and
//	 *         year.
//	 */
//	@SuppressWarnings("unchecked")
//	public List<PayRate> findByContractAndLocality(String contract, String locality, Integer year) {
//		String queryString = "from PayRate where " +
//				CONTRACT_RATE_KEY + " like ? and ( " +
//				LOCALITY + " = ? or " +
//				LOCALITY + " = '" + PayRate.LOCALITY_ALL + "') and " +
//				START_DATE + " >= ? and " + START_DATE + " <= ? " +
//				"order by " + OCC_CODE ;
//		Calendar cal = new GregorianCalendar();
//		cal.set(year, 0, 1);
//		Date date1 = cal.getTime();
//		cal.set(year, 11, 31);
//		Date date2 = cal.getTime();
//		Object[] values = { contract, locality, date1, date2 };
//		List<PayRate> rates = find(queryString, values);
//		return rates;
//	}

//	public static PayRateDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (PayRateDAO)ctx.getBean("PayRateDAO");
//	}

}
