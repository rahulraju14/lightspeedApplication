package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Contract;
import com.lightspeedeps.type.ContractType;

/**
 * A data access object (DAO) providing persistence and search support for
 * Contract entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Contract
 * @author MyEclipse Persistence Tools
 */
public class ContractDAO extends BaseTypeDAO<Contract> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ContractDAO.class);

	//property constants
	public static final String NAME = "name";
	private static final String YEAR = "year";
	private static final String TYPE = "type";
	public static final String UNION_KEY = "unionKey";
//	private static final String CONTRACT_CODE = "contractCode";


	public static ContractDAO getInstance() {
		return (ContractDAO)getInstance("ContractDAO");
	}

//	public List<Contract> findByName(Object name) {
//		return findByProperty(NAME, name);
//	}
//
//	public List<Contract> findByYear(Object year) {
//		return findByProperty(YEAR, year);
//	}
//
//	public List<Contract> findByUnionCode(Object unionCode) {
//		return findByProperty(UNION_CODE, unionCode);
//	}
//
//	public List<Contract> findByContractCode(Object contractCode) {
//		return findByProperty(CONTRACT_CODE, contractCode);
//	}

	/**
	 * @return List of all the distinct year values contained in the Contract table.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> findAllYears() {
		String query = "select distinct year from Contract";

		List<Integer> list = find(query);
		return list;
	}

	/**
	 * Get a sorted list of contracts of a particular type.
	 *
	 * @param type The ContractType of interest.
	 * @return The List of Contract instances that match the given type, sorted
	 *         in name order.
	 */
	@SuppressWarnings("unchecked")
	public List<Contract> findByType(ContractType type) {
		String queryString = "from Contract where " +
				TYPE + " = ? " +
				" order by " + NAME;
		return find(queryString, type);
	}

	/**
	 * @param year The year of interest, e.g., 2015.
	 * @param includeSides True iff "side letter" type contracts should be
	 *            included in the returned list.
	 * @return The List of Contract instances that match the given year.
	 */
	@SuppressWarnings("unchecked")
	public List<Contract> findByYear(Integer year, boolean includeSides) {
		String queryString = "from Contract where " +
				YEAR + " = ? " ;
		queryString += " order by name ";
		return find(queryString, year);
	}

	/**
	 * @param year The year of interest, e.g., 2015.
	 * @param unionKey The UnionCode value to be matched.
	 * @param includeSides True iff "side letter" type contracts should be
	 *            included in the returned list.
	 * @return The List of Contract instances that match the given year and
	 *         Union code.
	 */
	@SuppressWarnings("unchecked")
	public List<Contract> findByYearUnion(Integer year, String unionKey, boolean includeSides) {
		String queryString = "from Contract where " +
				YEAR + " = ? and " +
				UNION_KEY + " = ? ";
		Object values[] = { year, unionKey };

		return find(queryString, values);
	}

}
