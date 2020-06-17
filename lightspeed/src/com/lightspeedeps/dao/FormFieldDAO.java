package com.lightspeedeps.dao;

import com.lightspeedeps.model.FormField;

public class FormFieldDAO extends BaseTypeDAO<FormField>{

	public static FormFieldDAO getInstance() {
		return (FormFieldDAO)getInstance("FormFieldDAO");
	}

}
