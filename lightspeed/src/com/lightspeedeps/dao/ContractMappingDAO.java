package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ContractMapping;
import com.lightspeedeps.model.StartForm;

/**
 * A data access object (DAO) providing persistence and search support for
 * ContractMapping entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ContractMapping
 */
public class ContractMappingDAO extends BaseTypeDAO<ContractMapping> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(ContractMappingDAO.class);

	//property constants
	public static final String UNION_KEY = "unionKey";
	public static final String CONTRACT_RATE_KEY = "contractRateKey";

	public static ContractMappingDAO getInstance() {
		return (ContractMappingDAO)getInstance("ContractMappingDAO");
	}

	@SuppressWarnings("unchecked")
	public List<ContractMapping> findByUnionKeyContractCode(String unionKey, String contractRateKey) {
		String query = "from ContractMapping where unionKey = ? and contractCode=?";
		Object[] values = {unionKey, contractRateKey};

		return find(query, values);
	}

	/**
	 * Find the GS Contract mapping entry corresponding to the given StartForm.
	 *
	 * @param sf The StartForm whose union and contract code will be used to
	 *            look up the matching GS contract mapping record.
	 * @return The matching ContractMapping entry, or null if either there's no
	 *         match, or if the given StartForm is null.
	 */
	public static ContractMapping findContractMapping(StartForm sf) {
		ContractMapping contractMapping = null;
		if (sf != null) {
			List<ContractMapping> contractMappings =
					getInstance().findByUnionKeyContractCode(sf.getUnionKey(), sf.getContractCode());
			if (! contractMappings.isEmpty()) {
				contractMapping = contractMappings.get(0);
			}
		}
		return contractMapping;
	}

}
