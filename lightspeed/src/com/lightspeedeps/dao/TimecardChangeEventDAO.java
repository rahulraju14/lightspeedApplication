package com.lightspeedeps.dao;

import com.lightspeedeps.model.TimecardChangeEvent;

public class TimecardChangeEventDAO extends BaseTypeDAO<TimecardChangeEvent>{

	public static TimecardChangeEventDAO getInstance() {
		return (TimecardChangeEventDAO)getInstance("TimecardChangeEventDAO");
	}
}
