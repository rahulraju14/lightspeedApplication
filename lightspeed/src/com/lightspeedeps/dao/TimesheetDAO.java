package com.lightspeedeps.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;


public class TimesheetDAO extends ApprovableDAO<Timesheet> {

	private static final Log log = LogFactory.getLog(TimesheetDAO.class);

	public static TimesheetDAO getInstance() {
		return (TimesheetDAO)getInstance("TimesheetDAO");
	}

	@Override
	protected void createEvent(Approvable doc, Contact contact, Integer priorApproverId, byte outOfLine) {
		// no events are created for timesheets.
	}

	@Override
	protected void notifyReady(Approvable doc) {
		// No notifications are generated when a timesheet becomes 'ready'.
	}

	/**
	 * Creates a list of week-ending dates matching the existing timesheets in a
	 * tours production.
	 *
	 * @param production The production of interest.
	 * @param orderBy An optional SQL clause to be used for ordering the
	 *            results.
	 * @return A non-null (but possibly empty) list of SelectItem instances,
	 *         where the value is a Date, and the label is the formatted date.
	 */
	public List<SelectItem> createWeekEndingList(Production production, String orderBy) {
		List<SelectItem> weekEndingList = new ArrayList<>();
		List<Timesheet> timesheets = getTimesheetsByProduction(production, orderBy);

		DateFormat df = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);
		for(Timesheet ts : timesheets) {
			weekEndingList.add(new SelectItem(ts.getEndDate(), df.format(ts.getEndDate())));
		}
		return weekEndingList;
	}

	@SuppressWarnings("unchecked")
	public List<Timesheet> getTimesheetsByProduction(Production production, String orderBy) {
		String query = "from Timesheet ts where ts.prodId = ?";
		List<Timesheet> timesheets = new ArrayList<>();
		Object[] params = {production.getProdId()};

		try {
			if(orderBy != null) {
				query += " order by " + orderBy;
			}
			timesheets = find(query, params);
		}
		catch(Exception ex) {
			EventUtils.logError(ex);
			log.debug(ex);
		}

		return timesheets;
	}
}
