package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.FormIndem;

public class FormIndemDAO extends BaseTypeDAO<FormIndem> {

	public static FormIndemDAO getInstance() {
		return (FormIndemDAO)getInstance("FormIndemDAO");
	}

	/**
	 * Find just the Federal id number from a FormIndem given the id. This
	 * is used by our Jasper report "scriptlet" code.
	 *
	 * @param id The database id of the FormIndem of interest.
	 * @return The unformatted 9-digit fedidNumber, or null if not found.
	 */
	@SuppressWarnings("unchecked")
	public String findFedidNumberById(Integer id) {
		String fedid = null;
		String query = "select fedidNumber from FormIndem " +
				" where id = ? ";
		List<String> strs = find(query, id);
		if (strs.size() > 0) {
			fedid = strs.get(0);
		}
		return fedid;
	}

}
