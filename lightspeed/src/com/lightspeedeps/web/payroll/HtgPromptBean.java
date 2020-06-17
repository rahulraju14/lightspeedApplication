/**
 * File: HtgPromptBean.java
 */
package com.lightspeedeps.web.payroll;

import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.timecard.TimecardSelectBase;

/**
 * This class extends TimecardSelectBase with facilities to select the various timecard HTG
 * processing options.
 */
@ManagedBean
@ViewScoped
public class HtgPromptBean extends TimecardSelectBase {
	/** */
	private static final long serialVersionUID = 4507600958965707930L;

	private static final Log log = LogFactory.getLog(HtgPromptBean.class);

	/**
	 * Flag to determine whether unsubmitted timecards will be included in the HTG calculation
	 */
	private boolean calculateUnsubmitted = false;

	/**
	 * The drop-down list of selections for Status values available as filters for timecards. NOTE:
	 * for HTG, we omit the "Void" choice!
	 */
	private static final List<SelectItem> STATUS_SELECT_HTG_DL = Arrays.asList(
			new SelectItem(STATUS_ALL, "All"),
			new SelectItem(STATUS_SUBMIT, "Submitted"),
			new SelectItem(STATUS_APPROVE, "Approved (Final)")
			);

	public HtgPromptBean() {
	}

	public static HtgPromptBean getInstance() {
		return (HtgPromptBean) ServiceFinder.findBean("htgPromptBean");
	}

	/**
	 * Display the Run HTG options dialog with the specified values. Note that the Strings are all
	 * message-ids, which will be looked up in the message resource file.
	 *
	 * @param full True iff all options should be presented to the user. When false, the options for
	 *            "whole department" and "all crew" are not presented in the dialog box.
	 * @param prod The Production containing the timecards to be printed.
	 * @param project The Project associated with the timecard(s) being printed, if any. This will
	 *            be non-null only for Commercial productions.
	 * @param dept The name of the department, which will be included in the dialog box text.
	 * @param batch The name of the batch to include in the print options.
	 * @param holder The object which is "calling" us, and will get the callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 * @param titleId The message-id of the title for the dialog box.
	 * @param msgId The message-id of the message to be shown in the dialog.
	 * @param buttonOkId The message-id of the text for the "OK" button.
	 * @param buttonCancelId The message-id of the text for the "Cancel" button.
	 */
	public void show(boolean full, Production prod, Project project, String dept, String batch, PopupHolder holder,
			int act, String titleId, String msgId, String buttonOkId, String buttonCancelId) {
		log.debug("action=" + act);
		super.show(prod, project, dept, batch, full, false, holder, act, titleId, msgId, buttonOkId, buttonCancelId);

		setRangeSelection(RANGE_ALL);
	}

	/**
	 * Action method for the Ok button on the confirmation dialog. This closes the dialog box, and
	 * calls our holder's confirmOk() method.
	 */
	@Override
	public String actionOk() {
		return super.actionOk();
	}

	@Override
	public void listenRangeSelection(ValueChangeEvent event) {
		super.listenRangeSelection(event);
		if (!allowAllWeeks && (rangeSelection.equals(RANGE_DEPT) || rangeSelection.equals(RANGE_BATCH))) {
			allowAllWeeks = true;
			getWeekEndDateDL().add(0, Constants.SELECT_ALL_DATES);
		}
	}

	/** See {@link #STATUS_SELECT_HTG_DL}. */
	@Override
	public List<SelectItem> getStatusSelectDL() {
		return STATUS_SELECT_HTG_DL;
	}

	public boolean getCalculateUnsubmitted() {
		return calculateUnsubmitted;
	}

	public void setCalculateUnsubmitted(boolean calculateUnsubmitted) {
		this.calculateUnsubmitted = calculateUnsubmitted;
	}

	public boolean isCalculateUnsubmitted() {
		return calculateUnsubmitted;
	}
}
