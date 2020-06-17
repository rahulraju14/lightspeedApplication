package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.FormI9;

public class FormI9DAO extends BaseTypeDAO<FormI9> {

	public static FormI9DAO getInstance() {
		return (FormI9DAO)getInstance("FormI9DAO");
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
		String query = "select socialSecurity from FormI9 " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			ssn = strs.get(0);
		}
		return ssn;
	}

}
