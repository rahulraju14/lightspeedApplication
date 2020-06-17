/**
 * File: MobileApproverBean.java
 */
package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.MileageLine;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.popup.RejectPromptBean;

/**
 * Backing bean for the mobile timecard Approver Dashboard page.
 */
@ManagedBean
@ViewScoped
public class MobileApproverBean extends ApproverBase implements PopupHolder,  Serializable {
	/** */
	private static final long serialVersionUID = 4030233765136678245L;

	private static final Log log = LogFactory.getLog(MobileApproverBean.class);

	private static final String ATTR_MTC_TC_ID_LIST = Constants.ATTR_PREFIX + "mtcTimecardIdList";
//	private static final String ATTR_MTC_SELECTED_LIST = Constants.ATTR_PREFIX + "mtcSelectedList";
	public static final String ATTR_MTC_APPROVE_NOW_ID = Constants.ATTR_PREFIX + "mtcApproveNowId";
	private static final String ATTR_MTC_APPROVE_SELECTED = Constants.ATTR_PREFIX + "mtcApproveSelected";
	private static final String ATTR_MTC_APPROVE_SELECTED_ALERT = Constants.ATTR_PREFIX + "mtcApproveSelectedAlert";

	private static final int ACT_APPROVE = 12;
	private static final int ACT_REJECT = 13;
	private static final int ACT_SUBMIT_APPROVE = 14;

	/** End Date list contains SelectItem objects for available week-ending
	 *  dates. */
	private List<SelectItem> endDateList;

	/** True iff the "next" button should be enabled for the list of week-ending
	 * dates (i.e., timecards).  */
	private Boolean hasNextWeek;
	/** True iff the "previous" button should be enabled for the list of week-ending
	 * dates (i.e., timecards).  */
	private Boolean hasPrevWeek;

	/** True iff the "next" button should be enabled for the timecards.  */
	private Boolean hasNextTimecard;
	/** True iff the "previous" button should be enabled for the list of timecards. */
	private Boolean hasPrevTimecard;

	/** List of database ids of the WeeklyTimecards presented on the Approver Dashboard.
	 * The list is used by the Timecard Review page for "scrolling" forwards and backwards
	 * through the list of timecards.  The List is put in the Session for cross-page access. */
	private List<Integer> timecardIdList;

	/** Tracks all the selected timecards.  This a Map from the week-ending date to
	 * a List of Integer`s.  Each Integer is the database id of a WeeklyTimecard which
	 * has been selected (check-marked) by the user.  Each List corresponds to one
	 * week-ending date -- which is the map key to the List.
	 */
//	private Map<Date,List<Integer>> selectedIdList;

	/** The backing bean for the Reject prompt page. */
	private RejectPromptBean rejectBean;

	/** Backing field for the "approve now" check-mark box on the Timecard
	 * Review page. */
//	private boolean markApproval;

	/** True iff we are on the "approve" e-sign page as a result of the
	 * user clicking the "approve & sign now" button.  We're going to approve
	 * a single timecard, ignoring the "selected" (check-marked) list. */
	private boolean approveOne;

	/** The status of the currently displayed entry, for example, on the Timecard Review page,
	 *  which may be affected by the user viewing it, e.g., if the timecard is ready to be
	 *  approved/rejected by the current user or not. */
	private ApprovalStatus status;

	/** Name of the next approver for the current timecard, if any. */
	private String approverName;


	public MobileApproverBean() {
		super(WeeklyTimecard.SORTKEY_NAME);
		setWeekEndDate(SessionUtils.getDate(Constants.ATTR_APPROVER_DATE));
		if (getWeekEndDate() == null) {
			setWeekEndDate(TimecardUtils.calculateDefaultWeekEndDate());
			SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());
		}

		Integer tcId = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
		if (tcId == null) {
			// we may be here from "approve & sign now" button...
			tcId = SessionUtils.getInteger(ATTR_MTC_APPROVE_NOW_ID);
		}
		if (tcId != null) {
			weeklyTimecard = getWeeklyTimecardDAO().findById(tcId);
			if (weeklyTimecard != null) {
				setWeekEndDate(weeklyTimecard.getEndDate());
				SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());
				TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
			}
//			findMarkApprovalState();
		}
	}

	/**
	 * Action method for the "Add new entry" button on the Timecard Review
	 * mobile page. This adds a new line item to the existing Mileage Form.
	 *
	 * @return navigation string for the Mileage view/edit page.
	 */
	public String actionAddMileage() {
		MileageLine line = TimecardUtils.addMileageLine(weeklyTimecard);
		getWeeklyTimecardDAO().attachDirty(weeklyTimecard);
		SessionUtils.put(Constants.ATTR_MTC_MILEAGE_ID, line.getId());
		SessionUtils.put(Constants.ATTR_MTC_SHOW_MILES, true);
		SessionUtils.put(Constants.ATTR_HOURS_BACK_PAGE, "tcreviewm");
		return "mileagem";
	}

	/**
	 * Action method for the "Approve & Sign Now" button on the Timecard Review
	 * page. We just need to store the current timecard's id in the session,
	 * along with a flag so when the bean is re-instantiated on the new page, it
	 * will set up for a single timecard approval.
	 *
	 * @return null navigation string
	 */
	public String actionApproveNow() {
		SessionUtils.put(ATTR_MTC_APPROVE_NOW_ID, weeklyTimecard.getId());
		SessionUtils.put(Constants.ATTR_TIMECARD_ID,  weeklyTimecard.getId()); // needed for Back/Cancel; rev 4033
		SessionUtils.put(Constants.ATTR_APPROVE_BACK_PAGE, "tcreviewm");
		if (getvUser().getPin() == null) {
			SessionUtils.put(Constants.ATTR_PIN_NEXT_PAGE, "tcapprovem");
			return "createpinm";
		}
		return "tcapprovem";
	}

	/**
	 * Action method for the "Approve Selected" button on the Approver Dashboard
	 * mobile page. Make sure the list of selected timecards is current, and
	 * send the user to the "e-signature required" (confirmation) page. This
	 * also stores some session information so everything will get set up right
	 * when the bean is re-instantiated for the confirmation page.
	 *
	 * @return null navigation string
	 */
	public String actionApproveSelected() {
		// make sure all the selected timecards are still in "ready for approval" state
		String alertMsg = validateApproveList(getTimecardEntryList(), null);
		if (alertMsg == null) {
			return null;
		}

		SessionUtils.put(ATTR_MTC_APPROVE_NOW_ID, null); // ensure "approve & sign now" id is cleared
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, null); // we clear this as an extra check (see constructor)
		SessionUtils.put(ATTR_MTC_APPROVE_SELECTED, true);
		if (alertMsg.length() > 0) {
			SessionUtils.put(ATTR_MTC_APPROVE_SELECTED_ALERT, alertMsg);
		}
		else {
			SessionUtils.put(ATTR_MTC_APPROVE_SELECTED_ALERT, null);
		}
		SessionUtils.put(Constants.ATTR_APPROVE_BACK_PAGE, "appdashboardm");
		if (getvUser().getPin() == null) {
			SessionUtils.put(Constants.ATTR_PIN_NEXT_PAGE, "tcapprovem");
			return "createpinm";
		}
		return "tcapprovem";
	}

	/**
	 * Initialize the approvePromptBean when we are on the mobile Approval
	 * Confirmation page. We need to set up the ApprovePromptBean as it is used
	 * to populate fields on this page.
	 */
	private void approveSelectedSetup() {
		List<Integer> contactIdList = new ArrayList<Integer>();
		WeeklyTimecard wtc;
		for (TimecardEntry tce : getTimecardEntryList()) {
			if (tce.getStatus().isReady() && tce.getWeeklyTc().getMarkedForApproval()) {
				wtc = tce.getWeeklyTc();
				wtc = getWeeklyTimecardDAO().refresh(wtc);
				contactIdList.add(TimecardUtils.findNextApproverContactId(wtc));
			}
		}
		String alertMsg = SessionUtils.getString(ATTR_MTC_APPROVE_SELECTED_ALERT);
		SessionUtils.put(ATTR_MTC_APPROVE_SELECTED_ALERT, null); // clear from session
		ApproverUtils.approvePrompt(contactIdList, alertMsg, this, ACT_APPROVE, null);
	}

	/**
	 * Action method for the "e-sign" (ok) button on the "e-signature required"
	 * page (confirming approval). Called via the confirmOk method. The password
	 * and PIN have already been validated by the ApprovePromptBean.
	 *
	 * @return navigation string for the "approval done" mobile page, or null if
	 *         the approval fails.
	 */
	private String actionApproveOk() {
		String ret = "tcapprovedonem";
		try {
			if (approveOne) { // user did "approve & sign now" for one timecard
				SessionUtils.put(ATTR_MTC_APPROVE_NOW_ID, null); // ensure "approve & sign now" id is cleared
				Integer nextAppContactId = ApprovePromptBean.getInstance().getApproverContactId();
				WeeklyTimecard wtc = getWeeklyTimecardDAO().approve(weeklyTimecard, nextAppContactId);
				if (wtc == null) { // approval failed, timecard was not waiting for this user's approval
					MsgUtils.addFacesMessage("Approval.NotReady", FacesMessage.SEVERITY_ERROR);
					weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
					ret = null;
				}
				else {
					weeklyTimecard = wtc;
				}
				timecardEntryList = null; // make sure it gets re-created
			}
			else {
				getTimecardEntryList(); // make sure our list is current
				approveList();
//				clearSelectedIdList(getWeekEndDate());
			}
		}
		catch (Exception e) {
			ret = null;
			EventUtils.logError(e);
		}
		return ret;
	}

	/**
	 * Action method called when the user clicks the E-Sign (ok) button on the
	 * "submit and approve" mobile page.  The password and PIN have already been
	 * validated by the ApprovePromptBean.
	 *
	 * @return navigation string for the "approval done" mobile page.
	 */
	private String actionSubmitApproveOk() {
		weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
		if (weeklyTimecard.getApproverId() != null) {
			log.warn("Attempt to submit a timecard that was already submitted!");
			MsgUtils.addFacesMessage("Timecard.Submit.Submitted", FacesMessage.SEVERITY_ERROR);
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId()); // restore current item
			return null;
		}
		getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
		WeeklyTimecard wtc = getWeeklyTimecardDAO().submit(weeklyTimecard, true, null);
		if (wtc == null) {
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			Production prod = TimecardUtils.findProduction(weeklyTimecard);
			if (ApproverUtils.findFirstApprover(prod, project, weeklyTimecard.getDepartment(), null) == null) {
				MsgUtils.addFacesMessage("Timecard.NoApprovers.Text", FacesMessage.SEVERITY_WARN);
			}
			else {
				MsgUtils.addFacesMessage("Timecard.Submit.Failed", FacesMessage.SEVERITY_ERROR);
			}
			return null;
		}
		else {
			weeklyTimecard = wtc; // point to returned copy
		}
		return "tcapprovedonem";
	}

	private String actionSubmitApproveCancel() {
		log.debug("");
		getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
		String ret = SessionUtils.getString(Constants.ATTR_SUBMIT_BACK_PAGE, "appdashboardm");
		return ret;
	}

	private String actionApproveCancel() {
		String ret = SessionUtils.getString(Constants.ATTR_APPROVE_BACK_PAGE, "appdashboardm");
		return ret;
	}

	/**
	 * Action method for the Reject button on the Timecard Review page.
	 *
	 * @return navigation string to the Reject page.
	 */
	public String actionReject() {
		SessionUtils.put(Constants.ATTR_APPROVE_BACK_PAGE, "tcreviewm"); // Back/Cancel returns to Review
		return "tcrejectm";
	}

	/**
	 * Action method for the "Reject Selected" button on the Approver Dashboard
	 * mobile page.
	 *
	 * @return null navigation string
	 */
	public String actionRejectSelected() {
		for (TimecardEntry tce : getTimecardEntryList()) {
			if (tce.getWeeklyTc().getMarkedForApproval()) {
				weeklyTimecard = tce.getWeeklyTc();
				break;
			}
		}
		SessionUtils.put(Constants.ATTR_TIMECARD_ID,
				weeklyTimecard == null ? null : weeklyTimecard.getId());
//		clearSelectedIdList(getWeekEndDate());
		SessionUtils.put(Constants.ATTR_APPROVE_BACK_PAGE, "appdashboardm"); // Back/Cancel returns to dashboard
		return "tcrejectm";
	}

	/**
	 * Action method called via confirmOk, when the user clicks the "Reject"
	 * (ok) button on the Reject Timecard (confirmation) page.
	 *
	 * @return JSF navigation string
	 */
	private String actionRejectOk() {
		if (rejectBean == null) {
			rejectBean = RejectPromptBean.getInstance();
		}
		Integer approverId = rejectBean.getSelectedApproverId();
		Integer contactId = rejectBean.getSelectedContactId();
		weeklyTimecard = getWeeklyTimecardDAO().reject(weeklyTimecard, getvContact(), contactId,
				approverId, rejectBean.getComment());
		return "tcrejectdonem";
	}

	private String actionRejectCancel() {
		String ret = SessionUtils.getString(Constants.ATTR_APPROVE_BACK_PAGE, "tcreviewm");
		return ret;
	}

	/**
	 * Action method for the "Next" button on the Dashboard page, related to the
	 * list of week-ending dates available for the current user. If successful,
	 * the {@link #weeklyTimecard} and {@link #weekEndDate} fields are updated
	 * appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionNextWeekEndDate() {
		hasNextWeek = null;
		hasPrevWeek = null;
		int i = findWeekEndEntry();
		if (i > 0) {
			i--;
			setWeekEndDate((Date)getEndDateList().get(i).getValue());
			timecardEntryList = null;
			hasPrevWeek = true;
			SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());
		}
		return null;
	}

	/**
	 * Action method for the "Previous" button on the Dashboard page, related to
	 * the list of week-ending dates available for the current user. If
	 * successful, the {@link #weeklyTimecard} and {@link #weekEndDate} fields
	 * are updated appropriately.
	 *
	 * @return null navigation string
	 */
	public String actionPreviousWeekEndDate() {
		hasNextWeek = null;
		hasPrevWeek = null;
		int i = findWeekEndEntry();
		if (i < getEndDateList().size()-1) {
			i++;
			setWeekEndDate((Date)getEndDateList().get(i).getValue());
			timecardEntryList = null;
			hasNextWeek = true;
			SessionUtils.put(Constants.ATTR_APPROVER_DATE, getWeekEndDate());
		}
		return null;
	}

	/**
	 * Action method for the "next" (right-arrow) button on the Timecard review
	 * page, to go to the next available Timecard.
	 *
	 * @return null navigation string
	 */
	public String actionNextTimecard() {
		hasNextTimecard = null;
		hasPrevTimecard = null;
		status = null; // refresh if needed
		approverName = null; // refresh
		int ix = findTimecardListEntry();
		if (ix >= 0 && ix < timecardIdList.size()-1) {
			Integer id = timecardIdList.get(ix+1);
			weeklyTimecard = getWeeklyTimecardDAO().findById(id);
			SessionUtils.put(Constants.ATTR_TIMECARD_ID,
					weeklyTimecard == null ? null : weeklyTimecard.getId());
//			findMarkApprovalState();
			TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
		}
		return null;
	}

	/**
	 * Action method for the "previous" (left-arrow) button on the Timecard
	 * review page, to go to the next available Timecard.
	 *
	 * @return null navigation string
	 */
	public String actionPreviousTimecard() {
		hasNextTimecard = null;
		hasPrevTimecard = null;
		status = null; // refresh if needed
		approverName = null; // refresh
		int ix = findTimecardListEntry();
		if (ix > 0) {
			Integer id = timecardIdList.get(ix-1);
			weeklyTimecard = getWeeklyTimecardDAO().findById(id);
			SessionUtils.put(Constants.ATTR_TIMECARD_ID,
					weeklyTimecard == null ? null : weeklyTimecard.getId());
//			findMarkApprovalState();
			TimecardCalc.calculateWeeklyTotals(weeklyTimecard);
		}
		return null;
	}

	/**
	 * ValueChangeListener for the "selection" check boxes on the Approver Dashboard page.
	 * @param event contains old and new values
	 */
	public void listenSelectionChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (event.getNewValue() != null) {
				boolean checked = (Boolean)event.getNewValue();
				TimecardEntry tce = (TimecardEntry)ServiceFinder.getManagedBean("tcEntry");
				if (tce != null) {
					WeeklyTimecard wtc = tce.getWeeklyTc();
					if (wtc != null) {
						wtc.setMarkedForApproval(checked);
						getWeeklyTimecardDAO().attachDirty(wtc);
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for the "mark for approval" check box Timecard Review page.
	 * @param event contains old and new values
	 */
	public void listenMarkApproval(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (event != null) {
				boolean mark = (Boolean)event.getNewValue();
				weeklyTimecard.setMarkedForApproval(mark);
				getWeeklyTimecardDAO().attachDirty(weeklyTimecard);
			}
//			List<Integer> selList = getSelectedIdList(getWeekEndDate());
//			if (markApproval) {
//				selList.add(weeklyTimecard.getId()); // add this timecard
//			}
//			else {
//				selList.remove(weeklyTimecard.getId()); // remove it
//			}
//			selectedIdList.put(getWeekEndDate(), selList); // update the Map
//			SessionUtils.put(ATTR_MTC_SELECTED_LIST, selectedIdList); // update value in Session
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_APPROVE:
				res = actionApproveOk();
				break;
			case ACT_SUBMIT_APPROVE:
				res = actionSubmitApproveOk();
				break;
			case ACT_REJECT:
				res = actionRejectOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ACT_SUBMIT_APPROVE:
				res = actionSubmitApproveCancel();
				super.confirmCancel(action);
				break;
			case ACT_APPROVE:
				res = actionApproveCancel();
				super.confirmCancel(action);
				break;
			case ACT_REJECT:
				res = actionRejectCancel();
				super.confirmCancel(action);
				break;
			default:
				res = super.confirmCancel(action);
				break;
		}
		return res;
	}

	/**
	 * Create the list of timecards displayed on the Mobile Approver Dashboard page.
	 * It also updates the matching list of timecard database ids used by the
	 * Timecard Review page for scrolling through the "current" list of timecards.
	 *
	 * @see com.lightspeedeps.web.approver.ApproverBase#createTimecardList()
	 */
	@Override
	protected void createTimecardList() {
		createTimecardListAllDepts(getWeekEndDate(), true, null, null);

		ApproverUtils.calculateListApprovalStatus(timecardEntryList);
		sort();

//		List<Integer> selList = getSelectedIdList(getWeekEndDate());

		timecardIdList = new ArrayList<Integer>();
		Integer id;

		for (TimecardEntry tce : timecardEntryList) {
			TimecardCalc.calculateWeeklyTotals(tce.getWeeklyTc());
			id = tce.getWeeklyTc().getId();
			timecardIdList.add(id);
//			if (selList.contains(id)) {
//				tce.setChecked(true);
//			}
		}
		SessionUtils.put(ATTR_MTC_TC_ID_LIST, timecardIdList);
	}

	/**
	 * Determine flag settings that control enabled/disabled status of
	 * the scroll buttons for scrolling (left/right) through the list of timecards
	 * while on the Timecard Review page.
	 */
	private void calculateTimecardFlags() {
		hasNextTimecard = false;
		hasPrevTimecard = false;
		int ix = findTimecardListEntry();
		if (timecardIdList.size() < 2) {
			return;
		}
		if (ix >= 0) {
			if (ix < timecardIdList.size()-1) {
				hasNextTimecard = true;
			}
			if (ix > 0) {
				hasPrevTimecard = true;
			}
		}
	}

	/**
	 * Find the index of the currently selected/displayed timecard within the
	 * list of timecards last created for the Approval Dashboard page.
	 *
	 * @return The zero-origin index of the current WeeklyTimecard within the
	 *         list of database ids preserved in the timecardIdList (or in the
	 *         user's Session). Returns -1 if not found.
	 */
	private int findTimecardListEntry() {
		if (weeklyTimecard == null) {
			return -1;
		}
		return getTimecardIdList().indexOf(weeklyTimecard.getId());
	}

	/**
	 * Determine the proper settings of the "next" and "previous" flags for the
	 * week-ending-date list. These are used to enable and disable the "next"
	 * and "previous" buttons appropriately.
	 */
	private void calculateWeekFlags() {
		hasNextWeek = false;
		hasPrevWeek = false;
		if (getEndDateList().size() < 2) {
			return;
		}
		int i = findWeekEndEntry();
		// remember list is in reverse date order!
		if (i < getEndDateList().size()-1) {
			hasPrevWeek = true;
		}
		if (i > 0) {
			hasNextWeek = true;
		}
	}

	private int findWeekEndEntry() {
		int i = 0;
		for (SelectItem item : getEndDateList()) {
			if (((Date)item.getValue()).equals(getWeekEndDate())) {
				break;
			}
			i++;
		}
		return i;
	}

	/**
	 * Called from JSP to initialize for the Approver Dashboard page.
	 * Clears the "current timecard" setting out of our session, as it
	 * does not apply while on this page, and we don't want to "remember"
	 * a wrong value, e.g., one from a different week-ending date.
	 *
	 * @return false.
	 */
	public boolean getLoadDashboard() {
		SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
		return false;
	}

	/**
	 * Called from JSP to initialize for the "Submit & Approve" prompt page.
	 * @return false.
	 */
	public boolean getLoadSubmitApprove() {
		Integer tcId = SessionUtils.getInteger(ATTR_MTC_APPROVE_NOW_ID);
		if (tcId != null) { // yes!
			approveOne = true; // remember state for when user clicks "e-sign"
			List<Integer> contactIdList = new ArrayList<Integer>();
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			contactIdList.add(TimecardUtils.findNextNextApproverContactId(weeklyTimecard));
			ApproverUtils.approvePrompt(contactIdList, null, this, ACT_SUBMIT_APPROVE, null);
			SessionUtils.put(ATTR_MTC_APPROVE_NOW_ID, null);
		}
		return false;
	}

	/**
	 * Called from JSP to initialize for the Approve prompt page.
	 * @return false.
	 */
	public boolean getLoadApprove() {
		Integer tcId = SessionUtils.getInteger(ATTR_MTC_APPROVE_NOW_ID);
		if (tcId != null) { // yes! Single timecard to approve.
			approveOne = true; // remember state for when user clicks "e-sign"
			List<Integer> contactIdList = new ArrayList<Integer>();
			weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
			contactIdList.add(TimecardUtils.findNextApproverContactId(weeklyTimecard));
			ApproverUtils.approvePrompt(contactIdList, null, this, ACT_APPROVE, null);
			SessionUtils.put(ATTR_MTC_APPROVE_NOW_ID, null);
		}
		else if (SessionUtils.getBoolean(ATTR_MTC_APPROVE_SELECTED, false)) {
			 approveSelectedSetup();
			 SessionUtils.put(ATTR_MTC_APPROVE_SELECTED, null);
		}
		return false;
	}

	/**
	 * Called from JSP to initialize for the Reject prompt page.
	 * @return false
	 */
	public boolean getLoadReject() {
		if (rejectBean == null) {
			rejectBean = RejectPromptBean.getInstance();
			rejectBean.show(this, ACT_REJECT, null, weeklyTimecard);
		}
		return false;
	}

	/**
	 * Called from JSP to lock the current timecard. Used on the "Submit and
	 * Approve" page.
	 *
	 * @return false
	 */
	public boolean getLockIt() {
		if (weeklyTimecard != null && weeklyTimecard.getLockedBy() == null) {
			getWeeklyTimecardDAO().lock(weeklyTimecard, getvUser());
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getTimecardIdList() {
		if (timecardIdList == null) {
			timecardIdList = (List<Integer>)SessionUtils.get(ATTR_MTC_TC_ID_LIST);
			if (timecardIdList == null) {
				timecardIdList = new ArrayList<Integer>();
			}
		}
		return timecardIdList;
	}

	/**
	 * This method is called by the JSF framework when this bean is
	 * about to go 'out of scope', e.g., when the user is leaving the page
	 * or their session times out.  We use it to unlock the WeeklyTimecard
	 * to make it available again for editing.
	 */
	@PreDestroy
	public void dispose() {
		log.debug("");
		try {
			if (weeklyTimecard != null) {
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard); // prevent "non-unique object" failure in logout case
				if (weeklyTimecard != null && weeklyTimecard.getLockedBy() != null) {
					getWeeklyTimecardDAO().unlock(weeklyTimecard, getvUser().getId());
				}
			}
		}
		catch (Exception e) {
		}
	}

	/**
	 * @return The number of checked (selected) timecards in the list
	 * for the currently (or last) displayed week-ending date.
	 */
	public int getCheckedItemCount() {
		return getSelectedItemCount();
	}

	/** See {@link #status}. */
	public ApprovalStatus getStatus() {
		if (status == null) {
			status = ApproverUtils.findRelativeStatus(weeklyTimecard, SessionUtils.getCurrentContact());
		}
		return status;
	}

	/** See {@link #approverName}. */
	public String getApproverName() {
		if (approverName == null) {
			approverName = ApproverUtils.findApproverName(weeklyTimecard);
		}
		return approverName;
	}

//	/** See {@link #markApproval}. */
//	public boolean getMarkApproval() {
//		return markApproval;
//	}
//	/** See {@link #markApproval}. */
//	public void setMarkApproval(boolean approveNow) {
//		markApproval = approveNow;
//	}

	/** See {@link #hasNextWeek}. */
	public Boolean getHasNextWeek() {
		if (hasNextWeek == null) {
			calculateWeekFlags();
		}
		return hasNextWeek;
	}
	/** See {@link #hasNextWeek}. */
	public void setHasNextWeek(Boolean hasNextWeek) {
		this.hasNextWeek = hasNextWeek;
	}

	/** See {@link #hasPrevWeek}. */
	public Boolean getHasPrevWeek() {
		if (hasPrevWeek == null) {
			calculateWeekFlags();
		}
		return hasPrevWeek;
	}
	/** See {@link #hasPrevWeek}. */
	public void setHasPrevWeek(Boolean hasPrevWeek) {
		this.hasPrevWeek = hasPrevWeek;
	}

	/** See {@link #hasNextTimecard}. */
	public Boolean getHasNextTimecard() {
		if (hasNextTimecard == null) {
			calculateTimecardFlags();
		}
		return hasNextTimecard;
	}
	/** See {@link #hasNextTimecard}. */
	public void setHasNextTimecard(Boolean hasNextTimecard) {
		this.hasNextTimecard = hasNextTimecard;
	}

	/** See {@link #hasPrevTimecard}. */
	public Boolean getHasPrevTimecard() {
		if (hasPrevTimecard == null) {
			calculateTimecardFlags();
		}
		return hasPrevTimecard;
	}
	/** See {@link #hasPrevTimecard}. */
	public void setHasPrevTimecard(Boolean hasPrevTimecard) {
		this.hasPrevTimecard = hasPrevTimecard;
	}

	/** See {@link #endDateList}. */
	public List<SelectItem> getEndDateList() {
		if (endDateList == null) {
			endDateList = createEndDateList(getWeekEndDate(), false);
		}
		return endDateList;
	}
	/** See {@link #endDateList}. */
	public void setEndDateList(List<SelectItem> endDateList) {
		this.endDateList = endDateList;
	}

}
