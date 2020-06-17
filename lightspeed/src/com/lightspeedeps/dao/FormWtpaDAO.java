package com.lightspeedeps.dao;

import com.lightspeedeps.model.FormWTPA;

public class FormWtpaDAO extends BaseTypeDAO<FormWTPA>{

	public static FormWtpaDAO getInstance() {
		return (FormWtpaDAO)getInstance("FormWtpaDAO");
	}
}
