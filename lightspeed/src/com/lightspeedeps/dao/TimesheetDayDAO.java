package com.lightspeedeps.dao;

import com.lightspeedeps.model.TimesheetDay;


public class TimesheetDayDAO extends BaseTypeDAO<TimesheetDay> {

	public static TimesheetDayDAO getInstance() {
		return (TimesheetDayDAO)getInstance("TimesheetDayDAO");
	}

}
