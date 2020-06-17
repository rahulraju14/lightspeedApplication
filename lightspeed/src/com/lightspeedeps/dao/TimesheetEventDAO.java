package com.lightspeedeps.dao;

import java.util.Date;

import com.lightspeedeps.model.Timesheet;
import com.lightspeedeps.model.TimesheetEvent;
import com.lightspeedeps.type.TimedEventType;

public class TimesheetEventDAO extends SignedEventDAO<TimesheetEvent> {

	public static TimesheetEventDAO getInstance() {
		return (TimesheetEventDAO)getInstance("TimesheetEventDAO");
	}

	public TimesheetEvent createEvent(Timesheet timesheet, Integer timecardCount, TimedEventType type) {
		TimesheetEvent event = new TimesheetEvent();
		event.setTimesheet(timesheet);
		event.setTimecardCount(timecardCount);
		event.setType(type);
		event.setDate(new Date());
		initEvent(event);
		Integer savedEventId = save(event);
		TimesheetEvent savedEvent = findById(savedEventId);
		if (savedEvent != null) {
			return savedEvent;
		}
		return null;
	}

}
