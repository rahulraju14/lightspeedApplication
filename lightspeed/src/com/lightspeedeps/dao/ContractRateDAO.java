package com.lightspeedeps.dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ContractRate;

/**
 * A data access object (DAO) providing persistence and search support for
 * ContractRate entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ContractRate
 */

public class ContractRateDAO extends BaseTypeDAO<ContractRate> {

	private static final Log LOG = LogFactory.getLog(ContractRateDAO.class);

	public static ContractRateDAO getInstance() {
		return (ContractRateDAO)getInstance("ContractRateDAO");
	}

	/**
	 * Find a specific rate from the table.
	 *
	 * @param contractCode The contract code of interest.
	 * @param rateKey The rate key of interest.
	 * @param category The category (performance category/occupation) of
	 *            interest.
	 * @return The rate matching the given values, or null if not found.
	 */
	public BigDecimal findRate(String contractCode, String rateKey, String category) {
		BigDecimal rate = null;
		Map<String, Object> values = new HashMap<>();
		values.put("contractCode", contractCode);
		values.put("rateKey", rateKey);
		values.put("category", category);
		List<ContractRate> list = findByNamedQuery(ContractRate.GET_RATE_BY_CONTRACT_KEY_AND_CATEGORY, values);
		if (list.size() > 0) {
			rate = list.get(0).getRate();
		}
		LOG.debug("rate=" + rate);
		return rate;
	}

}