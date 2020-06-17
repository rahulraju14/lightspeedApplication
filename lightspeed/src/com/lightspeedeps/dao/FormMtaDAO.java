package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.FormMTA;

public class FormMtaDAO extends BaseTypeDAO<FormMTA> {

	public static FormMtaDAO getInstance() {
		return (FormMtaDAO)getInstance("FormMtaDAO");
	}

	/**
	 * Finds the value of the fiven field name from a FormMta corresponding to the given id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormMta of interest.
	 * @param fieldName The field name of the FormMta of interest.
	 * @return The unformatted 9-digit number.
	 */
	@SuppressWarnings("unchecked")
	public String findByIdField(Integer id, String fieldName) {
		String result = null;
		String query = "select "+ fieldName +" from FormMTA where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			result = strs.get(0);
		}
		return result;
	}

	/**
	 * Find just the social security number from a FormMta given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormMta of interest.
	 * @return The unformatted 9-digit SSN, or null if not found.
	 */
	/*@SuppressWarnings("unchecked")
	public String findSsnById(Integer id) {
		String ssn = null;
		String query = "select socialSecurity from FormMTA " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			ssn = strs.get(0);
		}
		return ssn;
	}*/

	/**
	 * Find just the Account number from a FormMta given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormMta of interest.
	 * @return The unformatted 9-digit SSN, or null if not found.
	 */
	/*@SuppressWarnings("unchecked")
	public String findAcctNumberById(Integer id) {
		String acctNumber = null;
		String query = "select accountNumber from FormMTA " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			acctNumber = strs.get(0);
		}
		return acctNumber;
	}*/

	/**
	 * Find just the Routing number from a FormMta given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormMta of interest.
	 * @return The unformatted 9-digit Routing number, or null if not found.
	 */
	/*@SuppressWarnings("unchecked")
	public String findRoutingNumberById(Integer id) {
		String acctNumber = null;
		String query = "select routingNumber from FormMTA " +
				" where id = ? ";
		System.out.println(query);
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			acctNumber = strs.get(0);
		}
		return acctNumber;
	}*/

}
