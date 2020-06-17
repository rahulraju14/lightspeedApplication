package com.lightspeedeps.dao;

import com.lightspeedeps.model.DocChangeEvent;

public class DocChangeEventDAO extends BaseTypeDAO<DocChangeEvent>{

	public static DocChangeEventDAO getInstance() {
		return (DocChangeEventDAO)getInstance("DocChangeEventDAO");
	}

}
