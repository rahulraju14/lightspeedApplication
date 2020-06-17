package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.FormDeposit;

public class FormDepositDAO extends BaseTypeDAO<FormDeposit>{

	public static FormDepositDAO getInstance() {
		return (FormDepositDAO)getInstance("FormDepositDAO");
	}

	/**
	 * Find just the social security number from a FormI9 given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormI9 of interest.
	 * @return The unformatted 9-digit SSN, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	public String findSsnById(Integer id) {
		String ssn = null;
		String query = "select socialSecurity from FormDeposit " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			ssn = strs.get(0);
		}
		return ssn;
	}
}
