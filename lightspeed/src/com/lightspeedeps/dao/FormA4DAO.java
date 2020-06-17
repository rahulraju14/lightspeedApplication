package com.lightspeedeps.dao;

import com.lightspeedeps.model.FormA4;

public class FormA4DAO extends BaseTypeDAO<FormA4> {

	public static FormA4DAO getInstance() {
		return (FormA4DAO) getInstance("FormA4DAO");
	}
}
