/**
 * ApproverHtg.java
 */
package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.type.ApprovableStatusFilter;
import com.lightspeedeps.type.TimecardSelectionType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.payroll.HtgPromptBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class supports the Run HTG function (for multiple timecards) from the Approver Dashboard. It
 * displays the options dialog and invokes the necessary timecard function(s).
 */
public class ApproverHtg {
	private static final Log log = LogFactory.getLog(ApproverHtg.class);

	/**
	 * Private constructor - no need to create instances of this class.
	 */
	private ApproverHtg() {
	}

	/**
	 * Display the Run HTG pop-up dialog, with appropriate messaging and options.
	 *
	 * @param wtc The current WeeklyTimecard, which may be selected by the user for HTG calculation.
	 *            Its department name will also be used for the "print current department" message
	 *            in the pop-up.
	 * @param weDate The week-ending date that the report may be related to. Optional. This will be
	 *            ignored if wtc is not null.
	 * @param userHasViewHtg If this is true, the print options dialog box will include options to
	 *            run HTG on all timecards and all departments. If false, the options for
	 *            "whole department" and "all crew" are not presented in the dialog box.
	 * @param holder The PopupHolder to be used for the call-back when the user clicks Calculate/Ok.
	 * @param action The action code to be returned to the holder as a call-back parameter.
	 */
	public static void showHtgOptions(WeeklyTimecard wtc, Date weDate, Date filterDate,
			boolean userHasViewHtg, PopupHolder holder, int action) {
		HtgPromptBean bean = HtgPromptBean.getInstance();
		if (bean.isVisible()) { // probably double-clicked
			log.debug("ignoring double-click");
			return;
		}
		String dept = "";
		String batch = "";
		String msg = "";
		if (wtc != null) {
			dept = wtc.getDeptName();
			if (wtc.getWeeklyBatch() != null) {
				batch = wtc.getWeeklyBatch().getName();
			}
			msg = "(" + wtc.getFirstName() + " " + wtc.getLastName() + " - " +
					wtc.getOccupation() + ")";
		}
		Production prod = TimecardUtils.findProduction(wtc);
		Project project = TimecardUtils.findProject(prod, wtc);
		bean.show(userHasViewHtg, prod, project, dept, batch, holder, action, "Timecard.HTG.Title", null,
				"Timecard.HTG.Ok", "Confirm.Cancel");
		if (wtc != null) {
			bean.setWeekEndDate(wtc.getEndDate());
		}
		else if (weDate != null) {
			bean.setWeekEndDate(weDate);
		}
		bean.setFilterDate(filterDate);
		bean.setMessage(msg);
	}

	/**
	 * @param wtc
	 * @param msgs
	 *
	 */
	public static List<Integer> runHtg(WeeklyTimecard wtc, List<TimecardMessage> msgs) {
		HtgPromptBean bean = HtgPromptBean.getInstance();
		List<Integer> results = new ArrayList<Integer>();
		String statusFilter;
		boolean calculateUnsubmitted = bean.getCalculateUnsubmitted();
		TimecardSelectionType select = bean.getRangeSelectionType();

		wtc = WeeklyTimecardDAO.getInstance().refresh(wtc);
		if (wtc == null || (!calculateUnsubmitted) && select == TimecardSelectionType.CURRENT
				&& wtc.getSubmitable()) {
			// Timecard was deleted or trying to process an individual's timecard that is unsubmitted
			// and calculateUnsubmitted is false.
			results.add(0); // timecard selected
			results.add(0); // timecard calculated

			return results;
		}
		Date wtcDate = bean.getWeekEndDate();
		boolean allDates = wtcDate.equals(Constants.SELECT_ALL_DATE);
		if (allDates) {
			wtcDate = wtc.getEndDate();
		}

		switch (select) {
			case BATCH:
			case UNBATCHED:
			case ALL:
				// don't allow "all dates" with range of Batch, Unbatched, or
				// All, unless user has
				// special privilege
				if (!AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_ALL_TIMECARDS)) {
					allDates = false;
				}
				break;
			default:
		}

		statusFilter = ApprovableStatusFilter.SUBMITTED.sqlFilter();

		if (calculateUnsubmitted) {
			statusFilter += ", " + ApprovableStatusFilter.NOT_SUBMITTED.sqlFilter();
		}

		Production prod = SessionUtils.getNonSystemProduction();
		if (prod == null) {
			prod = ProductionDAO.getInstance().findByProdId(wtc.getProdId());
		}
		Project project = wtc.getStartForm().getProject();

		results = TimecardService.calculateMultipleHtg(wtc, SessionUtils.getCurrentContact(),
				prod, project, select, allDates, wtcDate, null, msgs, statusFilter);
		log.debug("timecards selected and successfully processed" + results);

		return results;
	}

}
