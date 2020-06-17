/**
 * File: TimecardUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.text.*;
import java.util.*;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.service.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.web.approver.*;
import com.lightspeedeps.web.login.LoginBean;

/**
 * Utility methods related to Timecards and the timecard
 * creation and approval process.
 */
public class TimecardUtils {
	private static final Log log = LogFactory.getLog(TimecardUtils.class);

	// Typical amount of days on a timecard
	private static final int STANDARD_NUM_TIMECARD_DAYS = 7;

	private TimecardUtils() {
	}

	/**
	 * Processing for the "Add" button on the mileage form, which adds a new
	 * blank MileageLine entry to the bottom of the mileage form. It sets the date to
	 * today's date.  Used from Basic Timecard, Mobile Mileage page and
	 * Mobile Timecard Review page.
	 * @return The MileageLine instance that was created.
	 */
	public static MileageLine addMileageLine(WeeklyTimecard wtc) {
		Mileage mileage = wtc.getMileage();
		if (mileage == null) {
			mileage = new Mileage(wtc);
			wtc.setMileage(mileage);
		}
		if (mileage.getMileageLines() == null) {
			mileage.setMileageLines(new ArrayList<MileageLine>());
		}
		MileageLine line = new MileageLine(mileage);
		Date date = CalendarUtils.todaysDate();
		if (date.after(wtc.getEndDate())) {
			date = wtc.getEndDate();
		}
		else {
			Date prior = TimecardUtils.calculatePriorWeekEndDate(wtc.getEndDate());
			if (! date.after(prior)) {
				Calendar cal = CalendarUtils.getInstance(prior);
				cal.add(Calendar.DAY_OF_MONTH, 1);
				date = cal.getTime();
			}
		}
		line.setDate(date);
		mileage.getMileageLines().add(line);
		return line;
	}

	/**
	 * Add the given comment to the timecard's Comment field, along with the
	 * user's name, and the current date & time, with appropriate HTML
	 * formatting codes included.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param comment The comment to be added; if this is null, or an empty or
	 *            all-blank String, the timecard is not changed.
	 */
	public static void addComment(WeeklyTimecard wtc, String comment) {
		comment = formatComment(comment);
		if (comment != null) {
			if (wtc.getComments() != null) {
				comment = wtc.getComments() + comment;
			}
			wtc.setComments(comment);
		}
	}

	public static String formatComment(String comment) {
		String result = null;
		if (comment != null && comment.trim().length() > 0) {
			comment = StringUtils.saveHtml(comment.trim());
			Date date = new Date();
			DateFormat sdf = new SimpleDateFormat(", M/d/yy H:mm: ");
			result = "<b>" + SessionUtils.getCurrentUser().getFirstNameLastName()
					+ sdf.format(date) + "</b>" + comment + "<br/>";
		}
		return result;
	}

	/**
	 * Create the list of "week-end dates" that the user will be able to choose
	 * from in the drop-down list on the desktop Approval Dashboard or via the
	 * left/right arrows on the mobile time card pages, or in the Print
	 * Timecards pop-up's date selection, or in the Run HTG pop-up's date
	 * selection.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore all timecards
	 *            Project affiliation in determining date range; should be
	 *            non-null only for Commercial productions.
	 * @param listDate The date that the current timecard-list is generated
	 *            from; we need to be sure this date is in the list of week-end
	 *            dates. It might not be if the "first payroll date" in the
	 *            Production is set too late.
	 * @param allProjects If true then include dates that cover timecards for
	 *            all projects instead of just the current project. This
	 *            parameter is only applicable to Commercial productions; it is
	 *            ignored for TV/Feature productions.
	 * @param useWkEndDayPref If true, the given Production & Project's
	 *            'week-ending day' will be used to determine the dates in the
	 *            list. If false, the dates in the list will always be Saturday
	 *            dates.
	 * @return A non-null List of SelectItem`s; in each SelectItem, the value is
	 *         a Date object, and the label is the corresponding mm/dd/yyyy
	 *         representation.
	 */
	public static List<SelectItem> createEndDateList(Production prod, Project project,
			Date listDate, boolean allProjects, boolean useWkEndDayPref) {

		int advWeeks = 1;
		if (prod != null) {
			PayrollPreference pref = prod.getPayrollPref();
			advWeeks = pref.getMaxWeeksInAdvance();
		}
		else if (project != null) {
			PayrollPreference pref = project.getPayrollPref();
			advWeeks = pref.getMaxWeeksInAdvance();
		}

		int wkEndDay = Calendar.SATURDAY; // default to Saturday
		if (useWkEndDayPref) {
			wkEndDay = findWeekEndDay(prod, project);
		}

		List<SelectItem> dateList = new ArrayList<>();
		Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());

		Date lastDate = calculateLastDayOfCurrentWeek(wkEndDay); // Set end date as last date in week, with time=12:00am
		Calendar lastCal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		lastCal.setTime(lastDate);

		int daysExtend = (advWeeks - 1) * 7;
		if (cal.get(Calendar.DAY_OF_WEEK) > Calendar.WEDNESDAY) { // next week-end is < 10 days away
			daysExtend += 7;
		}
		if (daysExtend > 0) {
			lastCal.add(Calendar.DAY_OF_MONTH, daysExtend);
		}

		Date startDate;
		if (prod == null) { // on My Timecards page
			startDate = listDate;
			if (startDate == null) { // shouldn't happen
				startDate = lastDate;
			}
		}
		else {
			if (project != null) { // Commercial production
				if (allProjects) {
					startDate = WeeklyTimecardDAO.getInstance().findFirstDateByProduction(prod);
				}
				else {
					startDate = WeeklyTimecardDAO.getInstance().findFirstDateByProject(project);
				}
			}
			else {
				startDate = prod.getPayrollPref().getFirstPayrollDate();
			}
			if (listDate != null
					&& (startDate == null || listDate.before(startDate))
					&& ! listDate.equals(Constants.SELECT_ALL_DATE)
					&& ! listDate.equals(Constants.SELECT_PRIOR_DATE)) {
				startDate = listDate;
			}
			if (startDate == null) {
				startDate = prod.getStartDate();
			}
		}
		if (startDate.before(Constants.JAN_1_2000)) {
			// fix very sporadic issue of 'zero' date showing up in list
			startDate = Constants.JAN_1_2000;
		}
		cal.setTime(startDate);
		CalendarUtils.setStartOfDay(cal);
		if (cal.get(Calendar.DAY_OF_WEEK) != wkEndDay) {
			startDate = calculateWeekEndDate(startDate, wkEndDay);
			log.debug("W/E day was updated to " + wkEndDay + " instead of: " + cal.get(Calendar.DAY_OF_WEEK) + "; date=" + startDate);
			cal.setTime(startDate);
		}

		DateFormat df = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);
		while (! cal.after(lastCal)) {
			Date wedate = cal.getTime();
			dateList.add(0, new SelectItem(wedate, df.format(wedate)));
			cal.add(Calendar.DAY_OF_MONTH, 7);
		}
		return dateList;
	}

	/**
	 * Create SelectItem`s for all Approvers who are valid targets for a
	 * "Reject" operation, and add them to the given list.
	 *
	 * @param appr The Approvable [WeeklyTimecard or ContactDocument] being rejected.
	 * @param stop The Approver.id value, if found in the approval chain, will
	 *            stop further entries in the chain from being added to the
	 *            List. The purpose of this is so a caller can pass the
	 *            ApproverId of the current user, and only approvers up to, but
	 *            not including him, will be in the "rejection target" list. If
	 *            null, then all approvers in the chain will be added to the
	 *            list.
	 * @param stopAccountNum The User.accountNumber value of the current User.
	 *            This is used when reviewing the approval TimecardEvent`s. When
	 *            an Approval event is found with this account number, no
	 *            further events are processed, since those approvals probably
	 *            do not represent valid "reject" recipients.
	 * @param approvalPathId
	 * @return A non-null List of Contact`s for all Approvers who are valid
	 *         targets for a "Reject" operation for the given approvable, from the
	 *         first approver up to, but not including, the approver identified
	 *         by the 'stop' parameter. Any "out of line" approvers who are
	 *         identified through approval events (in the TimecardEvent`s) are
	 *         also added to the list.
	 *         <p>
	 *         The 'approvers' parameter is also updated with the Approver
	 *         objects that correspond with the Contact objects in the returned
	 *         List.
	 */
	@SuppressWarnings("rawtypes")
	public static List<Contact> createRejectionList(Approvable appr, Integer stop,
			String stopAccountNum, List<Approver> approvers, Integer approvalPathId) {
		log.debug("");
		Production prod = findProduction(appr);
		Project project = findProject(prod, appr);
		List<Contact> contacts = new ArrayList<>();
		if (appr == null) {
			return contacts;
		}
		// First scan department approval chain
		boolean done = false;
		Department department;
		if(approvalPathId != null) {
			ContactDocument cd = (ContactDocument) appr;
			department = cd.getEmployment().getDepartment();
		}
		else {
			department = appr.getDepartment();
		}
		if (department != null) {
			ApprovalAnchorMapped anchor;
			if (approvalPathId == null) { // path will null in case of timecards so we find ApprovalAnchor
				// get root of department approval chain
				anchor = ApprovalAnchorDAO.getInstance().findByProductionProjectDept(prod, project, department);
			}
			else { // for documents approval path will not be null hence we find approval path anchor
				anchor = ApproverUtils.getApprovalPathAnchor(project, department, approvalPathId);
			}
			if (anchor != null) {
				//ApproverGroup Changes
				if (approvalPathId != null) {
					ApproverGroup group = ((ApprovalPathAnchor) anchor).getApproverGroup();
					if (group != null) {
						if (group.getUsePool() && (! stop.equals((-1)*group.getId()))) {
							log.debug("");
							for (Contact con : group.getGroupApproverPool()) {
								contacts.add(con);
								approvers.add(null); // we don't have the Approver object, but keep the lists "in sync".
							}
						}
						//Fix for an issue if group follows pool approval method.
						//Document is in Dept pool
						else if (group.getUsePool() && (stop.equals((-1)*group.getId()))) {
							log.debug("");
							done = true;
						}
						else {
							Approver approver = group.getApprover();
							while (approver != null) {
								if (stop != null && approver.getId().equals(stop)) {
									done = true;
									break;
								}
								approvers.add(approver);
								contacts.add(approver.getContact());
								approver = approver.getNextApprover();
							}
						}
					}
				}
				else {
					Approver approver = anchor.getFirstApprover();
					while (approver != null) { //b
						if (stop != null && approver.getId().equals(stop)) {
							done = true;
							break;
						}
						approvers.add(approver);
						contacts.add(approver.getContact());
						approver = approver.getNextApprover();
					}
				}
			}
		}
		//Document is in Production pool
		if (stop != null && stop < 0 && approvalPathId != null && approvalPathId == (-(stop))) {
			done = true;
			log.debug("");
		}

		if ((! done) && (project != null) && approvalPathId == null) {
			// "stop" item not found, continue with adding entries from project approver chain
			Approver approver = project.getApprover();
			while (approver != null) {
				if (stop != null && approver.getId().equals(stop)) {
					done = true;
					break;
				}
				approvers.add(approver);
				contacts.add(approver.getContact());
				approver = approver.getNextApprover();
			}
		}

		if (! done) {
			// "stop" item not found, continue with adding entries from production approver chain
			Approver approver = null;
			if(approvalPathId != null) {
				ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
				if(! path.getUsePool()) {
					approver = path.getApprover();
				}
				else {
					log.debug("");
					if (path.getUseFinalApprover() && path.getFinalApprover() != null && stop != null &&
							stop.equals(path.getFinalApprover().getId()) && path.getApproverPool() != null) {
						log.debug("");
						for (Contact con : path.getApproverPool()) {
							contacts.add(con);
							approvers.add(null); // we don't have the Approver object, but keep the lists "in sync".
						}
						done = true;
					}
				}
			}
			else {
				approver = prod.getApprover();
			}
			while (approver != null) {
				if (stop != null && approver.getId().equals(stop)) {
					done = true;
					break;
				}
				approvers.add(approver);
				contacts.add(approver.getContact());
				approver = approver.getNextApprover();
			}
		}

		if (! done) {
			// never found "stop", it must be out-of-line approver. Let's clean up
			// the approver list by removing anyone who isn't in the Event list
			// of approvers.

			// first get list of account number of all who have approved.
			List<String> accounts = new ArrayList<>();
			for (SignedEvent evt : appr.getEvents()) {
				if (evt.getType() == TimedEventType.APPROVE) {
					accounts.add(evt.getUserAccount());
				}
			}
			// Now remove from our lists anyone not in list of actual approvers
			for (int ix = contacts.size()-1; ix >= 0; ix-- ) {
				Contact ct = contacts.get(ix);
				if (! accounts.contains(ct.getUser().getAccountNumber())) {
					contacts.remove(ix);
					approvers.remove(ix);
				}
			}
		}

		if (approvalPathId == null) { // following code is for Timecards only (out-of-line approvers)
			// now check for any contacts who have approved this timecard but aren't in the list
			// yet (typically out-of-line approvers), and add to the list.
			ContactDAO contactDAO = ContactDAO.getInstance();
			for (SignedEvent evt : appr.getEvents()) {
				// examine all Approval events
				if (evt.getType() == TimedEventType.APPROVE) {
					if (stopAccountNum != null && stopAccountNum.equals(evt.getUserAccount())) {
						// this is the current user's approval event; don't add anyone after this to the list
						break;
					}
					Contact contact = contactDAO.findByAccountNumAndProduction(evt.getUserAccount(), prod);
					if (contact != null && (! contacts.contains(contact))) {
						contacts.add(contact);
						approvers.add(null); // we don't have the Approver object, but keep the lists "in sync".
					}
				}
			}
		}
		return contacts;
	}

	/**
	 * Create a list of Contact`s to receive the Reject notification. This
	 * includes all approvers in the current chain between (but not including)
	 * the person the timecard is being sent to and the current approver (the
	 * person doing the Reject). For example, if the approval chain consists of
	 * A-B-C-D-E, and A, B, C, and D have approved it, but E rejects it, sending
	 * it back to B, then C & D are placed in this list of recipients. (B gets a
	 * separate notification with different text.)
	 *
	 * @param appr The Approvable [WeeklyTimecard or ContactDocument] being rejected.
	 * @param sendTo The recipient of the rejected approvable; only approvers
	 *            following this person in the approval chain may be added to
	 *            the returned List. If this is null, approvers will be added to
	 *            the list beginning with the first departmental approver.
	 * @param oldApproverId The Approver.id value of the current Approver entry
	 *            (which should identify the current user, who is doing the
	 *            Reject operation). Only approvers up to, but not including,
	 *            this person in the approval chain may be added to the returned
	 *            List. If this is null, contacts will be added until the end of
	 *            the production approval chain is reached.
	 * @return A non-null, but possibly empty, List of Contact`s as described
	 *         above.
	 */
	public static Collection<Contact> createRejectNotify(Approvable appr, Contact sendTo, Integer oldApproverId, ApprovalPath approvalPath) {
		log.debug("");
		Set<Contact> list = new HashSet<>();
		Production prod = findProduction(appr);
		Project project = findProject(prod, appr);
		// Start with departmental approvers
		Department dept;
		if(approvalPath != null) {
			ContactDocument cd = (ContactDocument) appr;
			dept = cd.getEmployment().getDepartment();
		}
		else {
			dept = appr.getDepartment();
		}
		boolean stopFound = ApproverUtils.createDeptApproverContactList(prod, project, dept, list, sendTo,
				oldApproverId, false, approvalPath);
		if (! stopFound) {
			if (list.size() > 0) { // found "starting point" already,
				sendTo = null;	// so begin adding with start of production list
			}
			if (project != null && approvalPath == null) { // Add project approvers, if any.
				stopFound = ApproverUtils.createApproverContactList(project.getApprover(), list, sendTo,
						oldApproverId, false);
			}
			if (! stopFound) {
				if (list.size() > 0) { // found "starting point" already,
					sendTo = null;	// so begin adding with start of production list
				}
				// Then add Production approvers
				if(approvalPath != null) {
					if (approvalPath.getUsePool()) {
						if (approvalPath.getUseFinalApprover() && approvalPath.getFinalApprover() != null &&
								(oldApproverId == null || oldApproverId.equals(approvalPath.getFinalApprover().getId()))) {
							list.addAll(approvalPath.getApproverPool());
						}
					}
					else {
						ApproverUtils.createApproverContactList(approvalPath.getApprover(), list, sendTo,
								oldApproverId, false);
					}
				}
				else {
					ApproverUtils.createApproverContactList(prod.getApprover(), list, sendTo,
							oldApproverId, false);
				}
			}
		}
		return list;
	}

	/**
	 * Create a list of Contact`s to receive the Recall notification. This
	 * includes all approvers in the current chain after the person the timecard
	 * is being sent to and up to and including the current approver. For
	 * example, if the approval chain consists of A-B-C-D-E, and A, B, C, and D
	 * have approved it, but B recalls it, then C, D, and E are placed in this
	 * list of recipients. (E is included, being the current approver.)
	 *
	 * @param wtc The WeeklyTimecard/ContactDocument being recalled.
	 * @param recaller The recipient of the timecard -- the person doing the 'recall'.
	 * @param oldApproverId The Approver.id value of the current Approver entry.
	 * @param path The ApprovalPath followed by the ContactDocument being recalled.
	 * @return A non-null, but possibly empty, List of Contact`s as described
	 *         above.
	 */
	public static List<Contact> createRecallNotify(Approvable wtc, Contact recaller, Integer oldApproverId, ApprovalPath path) {
		List<Contact> list = new ArrayList<>();
		Production prod = findProduction(wtc);
		Project project = findProject(prod, wtc);
		Department dept;
		if(path != null) {
			ContactDocument cd = (ContactDocument) wtc;
			dept = cd.getEmployment().getDepartment();
		}
		else {
			dept = wtc.getDepartment();
		}
		boolean stopFound = ApproverUtils.createDeptApproverContactList(prod, project, dept, list, recaller,
				oldApproverId, true, path);
		if (! stopFound) {
			if (list.size() > 0) { // found "starting point" already,
				recaller = null;	// so begin adding with start of production list
			}
			if (project != null && path == null) {
				stopFound = ApproverUtils.createApproverContactList(project.getApprover(), list, recaller,
						oldApproverId, true);
			}
			if (! stopFound) {
				if (list.size() > 0) { // found "starting point" already,
					recaller = null;	// so begin adding with start of production list
				}
				if (path != null) {
					// Pool Case
					if (path.getUsePool()) {
						if (oldApproverId != null && oldApproverId < 0) {
							list.addAll(path.getApproverPool());
						}
						else if (path.getUseFinalApprover() && path.getFinalApprover() != null &&
								(oldApproverId == null || oldApproverId.equals(path.getFinalApprover().getId()))) {
							list.add(path.getFinalApprover().getContact());
							list.addAll(path.getApproverPool());
						}
					}
					else {
						stopFound = ApproverUtils.createApproverContactList(path.getApprover(), list, recaller,
								oldApproverId, true);
					}
				}
				else {
					stopFound = ApproverUtils.createApproverContactList(prod.getApprover(), list, recaller,
							oldApproverId, true);
				}
			}
		}
		return list;
	}

	/**
	 * Create list of time cards available to this person, based on the current
	 * values of weekEndDate, departmentId, and batchId.
	 *
	 * @param bean Usually the calling bean, which supports the interface to
	 *            provide information about the timecards to be selected.
	 * @param filter A FilterBean which will supply values to filter the
	 *            resulting list of timecards.
	 * @param updateTotals If true, TimecardCalc.calculateWeeklyTotals() will be
	 *            called for each timecard in the list.
	 * @return A non-null, but possibly empty, list of TimecardEntry`s, each
	 *         containing a WeeklyTimecard.
	 */
	public static List<TimecardEntry> createTimecardList(TimecardListHolder bean,
			FilterBean filter, boolean updateTotals) {
		if (filter.getSelectFilterValue() != null) {
			FilterType f = filter.getFilterType();
			if (f == FilterType.DEPT) {
				bean.setBatchId(0);
				bean.setDepartmentId(Integer.valueOf(filter.getSelectFilterValue()));
				bean.setStatusFilter(ApprovableStatusFilter.ALL);
			}
			else if (f == FilterType.BATCH) {
				bean.setBatchId(Integer.valueOf(filter.getSelectFilterValue()));
				bean.setDepartmentId(0);
				bean.setStatusFilter(ApprovableStatusFilter.ALL);
			}
			else if (f == FilterType.STATUS) {
				bean.setBatchId(0);
				bean.setDepartmentId(0);
				try {
					bean.setStatusFilter(ApprovableStatusFilter.valueOf(filter.getSelectFilterValue()));
				}
				catch (Exception e) { // can happen when switching from one Filter-by to another.
					filter.setSelectFilterValue(ApprovableStatusFilter.ALL.name());
					bean.setStatusFilter(ApprovableStatusFilter.ALL);
				}
			}
		}
		if (bean.getDepartmentId() == null) {
			bean.setDepartmentId(0);
			SessionUtils.put(Constants.ATTR_APPROVER_DEPT, 0);
		}
		WeeklyBatch batch = null;
		boolean anyBatch = false;
		if (bean.getBatchId() == null || bean.getBatchId() == 0) {
			bean.setBatchId(0);
			anyBatch = true;
		}
		else if (bean.getBatchId() != Constants.SELECT_BATCH_NONE_ID) {
			batch = WeeklyBatchDAO.getInstance().findById(bean.getBatchId());
		}

		Project project = bean.getProject();
		if (bean.getShowAllProjects()) { // For Commercial productions, include all projects
			project = null;	// do this by setting the 'selected' project to null.
		}
		else {
			project = ProjectDAO.getInstance().refresh(project);
		}

		List<TimecardEntry> tceList;
		Contact contact = bean.getvContact();
		if (contact == null) { // may happen on My Timecards pages, particularly Full TC view
			contact = ContactDAO.getInstance().findByUserProduction(SessionUtils.getCurrentUser(), bean.getProduction());
		}

		// force the bean's W/E date to be a Saturday date, for the purpose of selecting a week's range of timecards
		Date satDate = bean.getWeekEndDate();
		satDate = TimecardUtils.calculateWeekEndDate(satDate, Calendar.SATURDAY);
		if (bean.getDepartmentId() == 0) { // "All departments" is selected
			tceList = TimecardUtils.createTimecardListAllDepts(bean.getProduction(), project, satDate,
					true, // include all timecards ending within the week
					anyBatch, batch, bean.getStatusFilter(), bean.getEmployeeAccount(), contact, bean.getUserHasViewHtg(), false);
		}
		else {	// just one department - no batch or Status selection is supported
			tceList = TimecardUtils.createTimecardListOneDept(bean.getProduction(), project,
					satDate, bean.getDepartmentId(), bean.getEmployeeAccount(),
					contact, bean.getUserHasViewHtg());
		}

		if (updateTotals) { // update relative status and weekly totals for each timecard
			ApproverUtils.calculateListApprovalStatus(tceList);
			for (TimecardEntry tce : tceList) {
				TimecardCalc.calculateWeeklyTotals(tce.getWeeklyTc());
			}
		}

		bean.sortTimecards(tceList);
		return tceList;
	}

	/**
	 * Create a list of TimecardEntry`s (representing WeeklyTimecard`s) for the
	 * given date, for all departments that the specified Contact is permitted
	 * to approve, or (optionally) is a time-keeper for.
	 *
	 * @param prod The Production containing the timecards.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param weDate The week-ending date of interest, or null if timecards of
	 *            all week-ending dates should be included.
	 * @param allOfWeek If true, include timecards with week-ending dates
	 *            any time during the specified week, i.e., whose week-ending
	 *            date is equal to 'weDate' or within the preceding 6 days.
	 * @param anyBatch If true, then a timecard with any batch (or no batch) is
	 *            allowed, and the 'batch' parameter is ignored. If false, then
	 *            the 'batch' parameter is respected.
	 * @param batch The WeeklyBatch that the timecard should belong to, or null
	 *            if only un-batched timecards are to be included.
	 * @param statusFilter The WeeklyStatusFilter value to be applied as a
	 *            filter against the timecards' WeeklyStatus value, or null if
	 *            no filtering by status is desired.
	 * @param acct The WeeklyTimecard.userAccount value of the desired
	 *            timecards, or null if all userAccount values are acceptable.
	 * @param vContact The Contact object of the user viewing the timecards.
	 * @param userHasViewHtg True iff the current user has the View-HTG
	 *            privilege.
	 * @param includeTimekeepers If true, then the returned list will also
	 *            include timecards for those departments for which the given
	 *            contact is a time-keeper.
	 * @return A non-null, but possibly empty, list of TimecardEntry objects
	 *         matching the given parameters.
	 */
	public static List<TimecardEntry> createTimecardListAllDepts(Production prod, Project project, Date weDate, boolean allOfWeek,
			boolean anyBatch, WeeklyBatch batch, ApprovableStatusFilter statusFilter, String acct, Contact vContact,
			boolean userHasViewHtg, boolean includeTimekeepers) {
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		List<TimecardEntry> tceList = new ArrayList<>();
		boolean allDepts = true;
		TimecardRange prior = TimecardRange.EXACT; // assume exact match to given date
		if (allOfWeek) {
			prior = TimecardRange.WEEK;
		}
		String sqlStatus = null;
		if (vContact == null) { // may happen on "My Timecards" pages
			return tceList;
		}
		if (weDate != null) {
			if (weDate.equals(Constants.SELECT_ALL_DATE)) {
				weDate = null;
				prior = TimecardRange.ANY;
			}
			else if (weDate.equals(Constants.SELECT_PRIOR_DATE)) {
				weDate = calculateDefaultWeekEndDate();
				prior = TimecardRange.PRIOR;
			}
		}
		if (acct != null && acct.equals(Constants.CATEGORY_ALL)) {
			acct = null;
		}
		if (statusFilter != null && statusFilter != ApprovableStatusFilter.ALL) {
			sqlStatus = statusFilter.sqlFilter();
		}
		vContact = ContactDAO.getInstance().refresh(vContact);
		if (userHasViewHtg || ApproverUtils.isProdOrProjectApprover(vContact, project)) {
			if (weDate == null) {
				if (anyBatch) {
					tceList = weeklyTimecardDAO.findByUserAccountTCE(prod, project, acct, sqlStatus);
				}
				else {
					tceList = weeklyTimecardDAO.findByUserAccountBatchTCE(prod, project, acct, batch, sqlStatus);
				}
			}
			else {
				if (anyBatch) {
					tceList = weeklyTimecardDAO.findByWeekEndDateAccountTCE(prod, project, weDate, prior, acct, sqlStatus);
				}
				else {
					tceList = weeklyTimecardDAO.findByWeekEndDateAccountBatchTCE(prod, project, weDate, prior, acct, batch, sqlStatus);
				}
			}
		}
		else {
			allDepts = false;
			// Find depts with this user as approver
			Collection<Department> depts = ApproverUtils.createDepartmentsApproved(vContact, project);
			if (includeTimekeepers) {
				// find any additional departments for which the contact is a "time keeper"
				List<Department> tkDeps = DepartmentDAO.getInstance().findStdDeptsByContact(vContact, project);
				for (Department dp : tkDeps) {
					if (! depts.contains(dp)) { // avoid adding duplicates
						depts.add(dp);
					}
				}
			}
			// Then get all timecardEntry`s for those departments
			for (Department dept : depts) {
				if (weDate == null) {
					if (anyBatch) {
						tceList.addAll(weeklyTimecardDAO.findByUserAccountDepartmentTCE(prod, project, acct, dept.getName(), sqlStatus));
					}
					else {
						tceList.addAll(weeklyTimecardDAO.findByUserAccountBatchDepartmentTCE(prod, project, acct, batch, dept.getName(), sqlStatus));
					}
				}
				else {
					if (anyBatch) {
						tceList.addAll(weeklyTimecardDAO.findByWeekEndDateAccountDeptTCE(prod, project, weDate, prior, acct, dept.getName(), sqlStatus));
					}
					else {
						tceList.addAll(weeklyTimecardDAO.findByWeekEndDateAccountBatchDeptTCE(prod, project, weDate, prior, acct, batch, dept.getName(), sqlStatus));
					}
				}
			}
		}

		if (acct == null && ! allDepts) {
			// No user account filter, and not all departments were included, so we need to
			// add any timecards in the current user's approval chain which may not have been selected yet.
			List<TimecardEntry> extra;
			if (anyBatch) {
				extra = weeklyTimecardDAO.findByWeekEndDateContactTCE(prod, project, weDate, prior, vContact, sqlStatus);
			}
			else {
				extra = weeklyTimecardDAO.findByWeekEndDateContactBatchTCE(prod, project, weDate, prior, batch, vContact, sqlStatus);
			}
			log.debug("out of line approvals=" + extra.size());
			for (TimecardEntry tce : extra) {
				if (! tceList.contains(tce)) {
					tceList.add(tce);
				}
			}
		}

		filterTimecardListStatus(tceList, statusFilter, vContact);
		return tceList;
	}

	/**
	 * Remove timecard entries from a given list, based on their status and the
	 * statusFilter value provided.
	 *
	 * @param tceList The existing list of TimecardEntry`s to be filtered.
	 * @param statusFilter The value a timecard must match to remain in the
	 *            list.
	 * @param vContact The Contact who is viewing the list; this value may be
	 *            used to determine the status of the timecard relative to the
	 *            person, e.g., if it is waiting for their approval.
	 */
	private static void filterTimecardListStatus(List<TimecardEntry> tceList,
			ApprovableStatusFilter statusFilter, Contact vContact) {
		if (statusFilter == ApprovableStatusFilter.READY) {
			// We need to determine status values relative to current viewer
			for (TimecardEntry tce : tceList) {
				switch(tce.getStatus()) {
					case APPROVED:
					case RECALLED:
					case REJECTED:
					case RESUBMITTED:
					case SUBMITTED:
						// these cases need to be evaluated relative to the viewer
						tce.setStatus(ApproverUtils.findRelativeStatus(tce.getWeeklyTc(), vContact));
						break;
					default:
						// Other values can be ignored.
						break;
				}
			}
			for (Iterator<TimecardEntry> iter = tceList.iterator(); iter.hasNext(); ) {
				TimecardEntry tce = iter.next();
				switch(tce.getStatus()) {
					case APPROVED_READY:
					case RECALLED_READY:
					case REJECTED_READY:
					case RESUBMITTED_READY:
						// these cases should be included
						break;
					default:
						// everything else should be removed
						iter.remove();
						break;
				}
			}
		}
		else if (statusFilter == ApprovableStatusFilter.SUBMITTED) {
			// Remove employee-level TCs. Ones that were submitted then rejected
			// will have been included by the SQL query.
			for (Iterator<TimecardEntry> iter = tceList.iterator(); iter.hasNext(); ) {
				TimecardEntry tce = iter.next();
				if (tce.getWeeklyTc().getApproverId() == null) {
					iter.remove();
				}
			}
		}
		else if (statusFilter == ApprovableStatusFilter.NOT_SUBMITTED) {
			// Remove non-employee-level TCs. Ones that were approved then rejected
			// to some other approver will have been included by the SQL query.
			for (Iterator<TimecardEntry> iter = tceList.iterator(); iter.hasNext(); ) {
				TimecardEntry tce = iter.next();
				if (tce.getWeeklyTc().getApproverId() != null) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Fill the timecardEntryList when a single department is selected. Uses
	 * current getWeekEndDate() and getEmployeeAccount() values. Note that any
	 * batchId or status selection is ignored -- we don't support filtering by
	 * both department and another filter (batch or status) simultaneously.
	 *
	 * @param prod The Production containing the timecards.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param weDate The week-ending date of interest, or SELECT_ALL_DATE if timecards of
	 *            all week-ending dates should be included.
	 * @param deptId The database id of the selected Department.
	 * @param acct The WeeklyTimecard.userAccount value of the desired
	 *            timecards, or null if all userAccount values are acceptable.
	 * @param vContact The Contact object of the user viewing the timecards.
	 * @param userHasViewHtg True iff the current user has the View-HTG
	 *            privilege.
	 * @return A non-null, but possibly empty, list of TimecardEntry objects
	 *         matching the given parameters.
	 */
	public static List<TimecardEntry> createTimecardListOneDept(Production prod, Project project, Date weDate,
			Integer deptId, String acct, Contact vContact, boolean userHasViewHtg) {
		Department currDept = DepartmentDAO.getInstance().findById(deptId);
		String departmentName = currDept.getName();
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		TimecardRange prior = TimecardRange.EXACT; // assume exact match to given date

		// Find any out-of-line approvals (not in normal chain)
		List<TimecardEntry> extra;
		List<TimecardEntry> timecardEntryList;
		if (weDate.equals(Constants.SELECT_ALL_DATE)) {
			weDate = null;
			prior = TimecardRange.ANY;
			extra = weeklyTimecardDAO
					.findByContactDeptAccountTCE(prod, project, vContact, departmentName, acct);
		}
		else {
			if (weDate.equals(Constants.SELECT_PRIOR_DATE)) {
				weDate = calculateDefaultWeekEndDate();
				prior = TimecardRange.PRIOR;
			}
			extra = weeklyTimecardDAO
					.findByWeekEndDateContactDeptAccountTCE(prod, project, weDate, prior, null, vContact, departmentName, acct);
		}
		log.debug("out of line approvals=" + extra.size());

		if (userHasViewHtg || ApproverUtils.isDeptApprover(vContact, project, currDept)
				|| ApproverUtils.isProdOrProjectApprover(vContact, project)) {
			if (weDate == null) {
				// get all timecards for this department -- regardless of date
				if (acct == null) {
					timecardEntryList = weeklyTimecardDAO.findByDepartmentTCE(prod, project, departmentName);
				}
				else {
					timecardEntryList = weeklyTimecardDAO
							.findByUserAccountDepartmentTCE(prod, project, acct, departmentName, null);
				}
			}
			else {
				// get all timecards for this department and date
				if (acct == null) {
					timecardEntryList = weeklyTimecardDAO
							.findByWeekEndDateDeptTCE(prod, project, weDate, prior, departmentName);
				}
				else {
					timecardEntryList = weeklyTimecardDAO
							.findByWeekEndDateAccountDeptTCE(prod, project, weDate, prior, acct, departmentName, null);
				}
			}
			for (TimecardEntry tce : extra) { // then add any out-of-line TC's not yet in list
				if (! timecardEntryList.contains(tce)) {
					timecardEntryList.add(tce);
				}
			}
		}
		else { // not normal approver for this department, just show out-of-line entries
			timecardEntryList = extra;
		}
		return timecardEntryList;
	}

	public static WeeklyTimecard createTimecard(User user, Production prod, Date endDate,
			StartForm sd, boolean adjusted) {
		return createTimecard(user, prod, endDate, sd, adjusted, STANDARD_NUM_TIMECARD_DAYS);
	}

	/**
	 * Creates, and saves to the database, a new WeeklyTimecard (and associated
	 * DailyTime objects) for the given User and Production, with the given
	 * week-ending date.
	 *
	 * @param user The user this WeeklyTimecard is for.
	 * @param prod The production this WeeklyTimecard is for.
	 * @param endDate The week-end date to be assigned to the new
	 *            WeeklyTimecard.
	 * @param sd the StartFormth this WeeklyTimecard is for -- used to determine
	 *            Role/department information.
	 * @param adjusted true iff the new WeeklyTimecard is to be marked as an
	 *            "Adjusted" timecard.
	 * @param numDays number of daily time instances to create for this
	 *            timecard. LS-2440
	 * @return the new WeeklyTimecard instance.
	 */
	public static WeeklyTimecard createTimecard(User user, Production prod,
			Date endDate, StartForm sd, boolean adjusted, int numDays) {
		if (user == null || sd == null || prod == null) {
			return null;
		}
		if (endDate.getTime() == 0) {
			return null;
		}
		WeeklyTimecard wtc = null;
		try {
			wtc = new WeeklyTimecard();
			wtc.setEndDate(endDate); // Set end date as Saturday date, with time=12:00am
			wtc.setStatus(ApprovalStatus.OPEN);
			wtc.setAdjusted(adjusted);

			// * Copy USER fields
			wtc.setUserAccount(user.getAccountNumber()); // associate with Lightspeed user account

			// * Copy PRODUCTION fields
			wtc.setProdId(prod.getProdId());	// associate it with current production

			// * Copy START-DOC fields
			wtc.setStartForm(sd);
			updateTimecardFromStart(wtc, sd, prod);

			createExpenseLines(wtc, sd);

//2.2.4937	boolean noSf[] = findMissingStartFormDates(sd, endDate);

			createDailyTimes(wtc, prod, sd.getWorkState(), sd.getWorkCity(), sd.getWorkCountry(),
					numDays); // LS-2156
			log.debug("");
			copyJobTableJobSplits(endDate, sd, wtc);
			log.debug("");

			//if tours
			if (prod.getType().isTours()) {
				createAllowances(wtc, sd);
			}

			wtc.setUpdated(new Date());
			WeeklyBatchUtils.addToBatch(wtc);
			if (wtc.getId() == null) { // wasn't saved by batch creation
				WeeklyTimecardDAO.getInstance().save(wtc); // add to database
			}
			//log.debug(wtc);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/**
	 * Create a timecard to be associated with an ACTRA Contract form.
	 * @param user
	 * @param prod
	 * @param endDate
	 * @param sd
	 * @return
	 */
	public static WeeklyTimecard createActraTimecard(User user, Production prod,
			Date endDate, StartForm sd) {
		if (user == null || sd == null || prod == null) {
			return null;
		}
		if (endDate.getTime() == 0) {
			return null;
		}
		WeeklyTimecard wtc = null;

		try {
			wtc = new WeeklyTimecard(endDate);
			wtc.setUserAccount(user.getAccountNumber()); // associate with Lightspeed user account

			// Set the daily times to null
			// ACTRA Contract can contain up to 13 daily time instances
			// Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			// calendar.setTime(endDate);
			//calendar.add(Calendar.DAY_OF_YEAR, -6);
			List<DailyTime> dailyTimes = new ArrayList<>();
			for (byte i = 1; i <= FormActraContract.ACTRA_NUM_TIME_ENTRIES; i++) {
				DailyTime dt = new DailyTime(wtc, i);
				//dt.setDate(calendar.getTime());
				dailyTimes.add(dt);
				//calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			wtc.setDailyTimes(dailyTimes); // Add List<DailyTime> to weeklyTimecard

			// * Copy PRODUCTION fields
			wtc.setProdId(prod.getProdId());	// associate it with current production

			// * Copy START-DOC fields
			wtc.setStartForm(sd);
			updateTimecardFromStart(wtc, sd, prod);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/** Method used to copy job tables & job splits from the prior timecard.
	 * @param endDate WeekEnd date of the new Timecard to be created.
	 * @param sf StarForm for the new Timecard to be created.
	 * @param wtc new WeeklyTimecard instance to be created.
	 */
	public static void copyJobTableJobSplits(Date endDate, StartForm sf, WeeklyTimecard wtc) {
		log.debug("");
		Date priorWkEndDate = TimecardUtils.calculatePriorWeekEndDate(endDate);
		PayrollPreference pref = null;
		List<WeeklyTimecard> wtcList = WeeklyTimecardDAO.getInstance().findByWeekEndDateStartForm(priorWkEndDate, sf);
		if (wtcList != null && ! wtcList.isEmpty()) {
			WeeklyTimecard weeklyTc = wtcList.get(0);
			log.debug("weeklyTc = " + weeklyTc.getId());
			pref = findPref(weeklyTc, sf);
			if (pref.getCopyJobTables()) {
				log.debug("");
				if (weeklyTc.getPayJobs() != null && weeklyTc.getPayJobs().size() > 0) {
					for (PayJob oldPay : weeklyTc.getPayJobs()) {
						log.debug("OLD pay = " + oldPay.getId());
						PayJob payJob = new PayJob();
						payJob.setWeeklyTimecard(wtc);
						payJob.setJobNumber(oldPay.getJobNumber());
						payJob.getAccount().copyFrom(oldPay.getAccount());
						payJob.setJobAccountNumber(oldPay.getJobAccountNumber());
						payJob.setJobName(oldPay.getJobName());
						payJob.setOccCode(oldPay.getOccCode());
						payJob.setRate(oldPay.getRate());
						payJob.setDailyRate(oldPay.getDailyRate());
						payJob.setWeeklyRate(oldPay.getWeeklyRate());
						PayJobDaily.createPayJobDailies(payJob, wtc.getEndDate());
						wtc.getPayJobs().add(payJob);
						log.debug("added payjob #" + payJob.getJobName() + ", " + payJob.getJobNumber());
					}
				}
			}
			if (pref.getCopyJobSplits()) {
				log.debug("");
				if (weeklyTc.getDailyTimes() != null && ! weeklyTc.getDailyTimes().isEmpty() &&
						wtc.getDailyTimes() != null && ! wtc.getDailyTimes().isEmpty()) {
					for(int i = 0; i < wtc.getDailyTimes().size(); i++) {
						DailyTime dt = weeklyTc.getDailyTimes().get(i);
						DailyTime newDt = wtc.getDailyTimes().get(i);
						log.debug("OLD DT = " + dt.getId());
						newDt.setSplitByPercent(dt.getSplitByPercent());
						newDt.setJobNum1(dt.getJobNum1());
						newDt.setJobNum2(dt.getJobNum2());
						newDt.setJobNum3(dt.getJobNum3());
						newDt.setSplitStart2(dt.getSplitStart2());
						newDt.setSplitStart3(dt.getSplitStart3());
						wtc.getDailyTimes().set(i, newDt);
					}
				}
			}
		}
	}

	/**
	 * @param wtc
	 * @param sd
	 */
	public static void createAllowances(WeeklyTimecard wtc, StartForm sd) {
		byte lineNum = 1; // assign sequential line numbers
		// Note: If amount fields in StartForm are null, no payExpense item is created.
		log.debug("");
		if (sd.getPerdiemTx().getAmt() != null) {
			PayExpense payExpense = new PayExpense(wtc, lineNum++);
			payExpense.setRate(sd.getPerdiemTx().getAmt());
			payExpense.setQuantity(BigDecimal.ONE);
			payExpense.setCategory(PayCategory.PER_DIEM_TAX.getLabel());
			payExpense.calculateTotal();
			wtc.getExpenseLines().add(payExpense);
		}

		// same for sd.getPerdiemNtx(), as PayCategory.PER_DIEM_NONTAX
		if (sd.getPerdiemNtx().getAmt() != null) {
			PayExpense payExpense = new PayExpense(wtc, lineNum++);
			payExpense.setRate(sd.getPerdiemNtx().getAmt());
			payExpense.setQuantity(BigDecimal.ONE);
			payExpense.setCategory(PayCategory.PER_DIEM_NONTAX.getLabel());
			payExpense.calculateTotal();
			wtc.getExpenseLines().add(payExpense);
		}

		// sd.getPerdiemReimb(), as PayCategory.PER_DIEM_REIMB
//		if (sd.getPerdiemReimb().getAmt() != null) {
//			PayExpense payExpense = new PayExpense(wtc, lineNum++);
//			payExpense.setRate(sd.getPerdiemReimb().getAmt());
//			payExpense.setQuantity(BigDecimal.ONE);
//			payExpense.setCategory(PayCategory.PER_DIEM_REIMB.getLabel());
//			payExpense.calculateTotal();
//			wtc.getExpenseLines().add(payExpense);
//		}

		// sd.getPerdiemAdv(), as PayCategory.PER_DIEM_ADVANCE
		if (sd.getPerdiemAdv().getAmt() != null) {
			PayExpense payExpense = new PayExpense(wtc, lineNum++);
			payExpense.setRate(sd.getPerdiemAdv().getAmt());
			payExpense.setQuantity(BigDecimal.ONE);
			payExpense.setCategory(PayCategory.PER_DIEM_ADVANCE.getLabel());
			payExpense.calculateTotal();
			wtc.getExpenseLines().add(payExpense);
		}
		//WeeklyTimecardDAO.getInstance().attachDirty(wtc);
	}

	/**
	 * Update the given timecard with information from its matching StartForm.
	 * This is typically invoked by a user when the StartForm has been updated,
	 * and they want to bring the timecard in sync with those changes, without
	 * having to delete and recreate the timecard.
	 *
	 * @param wtc The timecard to be updated.
	 * @param production The Production to which the timecard belongs.
	 */
	public static void updateTimecardFromStart(WeeklyTimecard wtc, Production production) {
		StartForm sd = wtc.getStartForm();
		sd = StartFormDAO.getInstance().refresh(sd);

		updateTimecardFromStart(wtc, sd, production);

		// We no longer create expense table Box Rental entry.
//2.9.5416	updateBoxRentalExpense(wtc, false, false);

		PayJob pj = null;
		if (wtc.getPayJobs() != null && wtc.getPayJobs().size() > 0) {
			pj = wtc.getPayJobs().get(0);
			pj.setRate(wtc.getHourlyRate());
			pj.setDailyRate(wtc.getDailyRate());
			pj.setWeeklyRate(wtc.getWeeklyRate());
			pj.setAccountCodes(wtc.getAccount());
		}

		if (production.getType().isAicp()) {
			for (DailyTime dt : wtc.getDailyTimes()) {
				ProductionPhase phase = dt.getPhase();
				if (phase != null) {
					switch (phase) {
						case P:
						case W:
							dt.setAccountMajor(wtc.getAccountMajor());
							break;
						case S:
							dt.setAccountMajor(wtc.getAccountDtl());
							break;
						case N_A:
							break;
					}
				}
			}
		}

	}

	/**
	 * Update the given WeeklyTimecard with all the relevant information from
	 * the given StartForm. Used during timecard creation and also by the
	 * "Refresh" or "Update" function available to an end user who has edit
	 * rights to a timecard.
	 *
	 * @param wtc The timecard to be updated.
	 * @param sd The StartForm to be used as the data source.
	 * @param prod The Production containing the timecard.
	 */
	private static void updateTimecardFromStart(WeeklyTimecard wtc, StartForm sd, Production prod) {
		wtc.setFirstName(sd.getFirstName());
		wtc.setLastName(sd.getLastName());
		if (prod.getAllowOnboarding()) {
			Employment emp = sd.getEmployment();
			if (emp != null) {
				log.debug("");
				wtc.setOffProduction(emp.getOffProduction());
				wtc.setAccountCodes(emp.getAccount()); // copy all account codes
			}
		}
		else {
			log.debug("");
			wtc.setOffProduction(sd.getOffProduction());
			wtc.setAccountCodes(sd.getAccount()); // copy all account codes
		}
		wtc.setSocialSecurity(sd.getSocialSecurity());
		wtc.setCityWorked(sd.getWorkCity());
		wtc.setStateWorked(sd.getWorkState());
		wtc.setWorkCountry(sd.getWorkCountry()); // LS-2156, LS-2344
		wtc.setWorkZip(sd.getWorkZip());
		wtc.setProdCo(sd.getProdCompany());
		wtc.setJobName(sd.getJobName());
		wtc.setJobNumber(sd.getJobNumber());
		wtc.setProdName(sd.getProdTitle());
		wtc.setRetirementPlan(sd.getRetirementPlan());
		wtc.setOccupation(sd.getJobClass());
		wtc.setOccCode(sd.getOccupationCode());
		wtc.setLsOccCode(sd.getLsOccCode());

		// LS-2741
		wtc.setPaidAs(sd.getPaidAs());
		if (sd.getPaidAs() != PaidAsType.I) { // Only copy loanout info if not Individual. LS-3007
			wtc.setLoanOutCorp(sd.getLoanOutCorpName());
			wtc.setFedCorpId(sd.getFederalTaxId());
			wtc.setStateCorpId(sd.getStateTaxId());
		}

		if (sd.getEmployment() != null) {
			wtc.setUnderContract(sd.getEmployment().getSkipDeptApprovers());
			wtc.setWageState(sd.getEmployment().getWageState());
			wtc.setDepartment(sd.getEmployment().getDepartment());
			// note - if we don't set the departmentId field directly,
			// then getDepartmentId() returns null on this cached object.
			// (getDepartmentId() works on objects retrieved from the db)
			wtc.setDepartmentId(sd.getEmployment().getDepartment().getId()); // fixed, rev 2.0.3605
			wtc.setDeptName(sd.getEmployment().getDepartment().getName());
		}
		else {
			// This should only happen for "legacy" (3.0) SF's that were not associated
			// with a role (ProjectMember) at the time of the 3.1 conversion.
			wtc.setUnderContract(sd.getSkipDeptApproval());
		}
		wtc.setUnionNumber(sd.getUnionLocalNum());
		//wtc.setAccountCodes(sd.getAccount()); // copy all account codes
		WorkZone zone = WorkZone.SL; // default to Studio Location
		if (sd.isDistantRate()) {
			zone = WorkZone.DL; // Distant Location
		}

		boolean allowedWork = wtc.getAllowWorked();
		wtc.setAllowWorked(sd.getAllowWorked());
		if (allowedWork && ! wtc.getAllowWorked()) {
			// Was exempt, but not any more!
			// Have to un-check "worked", else no way to enter hours.
			for (DailyTime dt : wtc.getDailyTimes()) {
				if (dt.getWorked()) {
					dt.setWorked(false);
					dt.setHours(null);
				}
			}
			TimecardCalc.calculateWeeklyTotals(wtc);
		}

		fillRates(wtc, sd, prod, zone, null);
		if (zone.isStudio()) {
			wtc.setRateType(StartForm.USE_STUDIO_RATE);
		}
		else {
			wtc.setRateType(StartForm.USE_LOCATION_RATE);
		}
	}


	/**
	 * Duplicate all the existing timecards from the given Production with a
	 * given week-ending date into a new set of timecards with the given target
	 * week-ending date.
	 *
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param wkSourceDate The week-ending date of the existing timecards which
	 *            will be copied.
	 * @param wkTargetDate The week-ending date to set in the new copies of the
	 *            timecards.
	 * @param overWrite If true, the new copies may replace existing timecards.
	 *            If false, timecards with the target week-ending date will not
	 *            be replaced or changed.
	 * @param retro If true, this is a "retro" operation: mark the new copy as
	 *            an "Adjusted" timecard, ignore the "overWrite" flag, set the
	 *            status of the copies to SUBMITTED, update the timecard from the
	 *            relevant StartForm, and set the approverId to the given
	 *            parameter.
	 * @return A text message indicating the number of timecards created.
	 */
	public static String createDuplicateTimecards(Production prod, Project project,
			Date wkSourceDate, Date wkTargetDate, boolean overWrite, boolean retro) {
		/** The number of timecards created for the current production. */
		int timecardCount = 0;

		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();

		List<WeeklyTimecard> wtcs = weeklyTimecardDAO.findByWeekEndDate(prod, project, wkSourceDate, null);

		log.debug("w/e source date=" + wkSourceDate + ", w/e target date=" + wkTargetDate +
				", source TCs=" + wtcs.size() + ", prod=" + prod.getTitle());

		Date wkPriorDate = calculatePriorWeekEndDate(wkTargetDate);

		Integer approver = null;
		if (retro) {
			approver = prod.getApprover().getId();
		}

		boolean copy;
		for ( WeeklyTimecard wtc : wtcs ) {
			copy = true;
			if (retro) {
				copy = false;
				if (wtc.getGrandTotal() != null && wtc.getGrandTotal().signum() != 0) {
					// Only retro if original timecard was calculated.
					StartForm sf = wtc.getStartForm();
					copy = ! sf.getUnionLocalNum().equals("NonU");
					// Only retro if StartForm is set to a union.
				}
			}
			if (copy) {
				timecardCount += createDuplicateTimecard(wtc, wkTargetDate, wkPriorDate, prod, overWrite, retro, approver);
			}
		}

		String message = "";
		if (timecardCount > 0) {
			String date = new SimpleDateFormat("M/d/yyyy").format(wkTargetDate);
			message += "\n" + prod.getTitle() + ": " + timecardCount + " timecards created for W/E " + date;
		}
		else {
			message = "No timecards created";
		}
		return message;
	}

	/**
	 * Duplicate one timecard by creating a new timecard with the given target
	 * week-ending date that has all the same employee-entered information as
	 * the original.
	 *
	 * @param wtc The timecard to be copied.
	 * @param date The week-ending date to set in the new copy of the timecard.
	 * @param priorDate The week-ending date of the week prior to 'date'.
	 * @param prod The Production containing both the source and copy timecards.
	 * @param overWrite If true, the new copy may replace an existing timecard.
	 *            If false, and a timecard already exists with the target
	 *            week-ending date, then a duplicate will not be created.
	 * @param retro If true, this is a "retro" operation: mark the new copy as
	 *            an "Adjusted" timecard, ignore the "overWrite" flag, set the
	 *            status of the copy to SUBMITTED, update the timecard from the
	 *            relevant StartForm, and set the approverId to the given
	 *            parameter.
	 * @param approver The id of the Approver who will be the next approver of
	 *            the timecard copy created.
	 * @return The number of timecards created - either zero or one! A copy will
	 *         not be created if (a) the target timecard already exists, or (b)
	 *         the StartForm related to the original timecard has an effective
	 *         end-date following the given target week-ending date, or (c) the
	 *         StartForm has an effective starting date prior to the first day
	 *         of the target timecard.
	 */
	private static int createDuplicateTimecard(WeeklyTimecard wtc, Date date, Date priorDate,
			Production prod, boolean overWrite, boolean retro, Integer approver) {
		int count = 0;
		StartForm sf = wtc.getStartForm();

		if (sf.getEffectiveOrStartDate() != null && ! sf.getEffectiveOrStartDate().after(date)) {
			Date endDate = sf.getEarliestEndDate();
			if (sf.getEmployment() != null && sf.getEmployment().getEndDate() != null) {
				// Employment end date takes precedence over StartForm. 3.2.8076
				endDate = sf.getEmployment().getEndDate();
			}
			if (endDate == null || endDate.after(priorDate)) {
				WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
				boolean generate = true;

				if (! retro) {
					List<WeeklyTimecard> oldTcs = weeklyTimecardDAO.findByWeekEndDateStartForm(
							date, sf);
					if (oldTcs.size() > 0) {
						if (overWrite) {
							for (WeeklyTimecard tc : oldTcs) {
								weeklyTimecardDAO.delete(tc);
							}
						}
						else {
							generate = false;
						}
					}
				}

				if (generate) {
					User user = sf.getContact().getUser();
					WeeklyTimecard newWtc = createTimecard(user, prod, date, sf, false);
					if (newWtc != null) {
						TimecardUtils.copyEmpData(wtc, newWtc, true, retro);
						if (retro) {
							newWtc.setAdjusted(true);
							updateTimecardFromStart(newWtc, prod);
							newWtc.setStatus(ApprovalStatus.SUBMITTED);
							newWtc.setApproverId(approver);
							// SKIP for now - creating a single negative entry for total gross.
//							byte lineNumber = (byte)newWtc.getExpenseLines().size();
//							AccountCodes codes = wtc.getAccount();
//							newWtc.addExpense(wtc.getGrandTotal(), BigDecimal.ONE.negate(),
//									PayCategory.SAL_ADVANCE_NONTAX, lineNumber, codes, null);
						}
						weeklyTimecardDAO.attachDirty(newWtc);
						count = 1;
					}
				}
			}
		}

		return count;
	}

	/**
	 * Create an almost-empty timecard. Used in JUnit tests.
	 *
	 * @return The newly-created WeeklyTimecard, which includes a set of
	 *         DailyTime objects. The week-ending date will be set to the
	 *         current default week-ending date.
	 */
	public static WeeklyTimecard createBlankTimecard() {
		Date endDate = TimecardUtils.calculateDefaultWeekEndDate();
		WeeklyTimecard wtc = createBlankTimecard(endDate);
		return wtc;
	}

	/**
	 * Create an almost-empty timecard with the specified W/E date.
	 *
	 * @param endDate The week-ending date to store in the WeeklyTimecard.
	 * @return The newly-created WeeklyTimecard, which includes a set of
	 *         DailyTime objects.
	 */
	public static WeeklyTimecard createBlankTimecard(Date endDate) {

		WeeklyTimecard wtc = new WeeklyTimecard(endDate);

		createDailyTimes(wtc, null, null, null, null, STANDARD_NUM_TIMECARD_DAYS);

		return wtc;
	}

	/**
	 * Create the List of DailyTime entries for the given timecard. This
	 * includes determining which days are holidays and setting the DayType
	 * appropriately.
	 * <p>
	 * Days which do not have a valid StartForm (see 'noSf' parameter) will have
	 * the DailyTime.noStartForm field set to true.
	 * <p>
	 * Upon exit, the supplied timecard will have its dailyTimes field set to a
	 * new List of new DailyTime objects, with appropriate dates, etc.,
	 * initialized.
	 *
	 * @param wtc The timecard being created, where the DailyTime`s will be
	 *            stored. Also the source of the week-end date for the
	 *            DailyTime`s.
	 * @param prod The Production that the timecard belongs to. This may be null
	 *            when a "blank" timecard is being created -- typically for
	 *            JUnit tests.
	 * @param workState The default work state (e.g., CA)
	 */
	private static void createDailyTimes(WeeklyTimecard wtc, Production prod, String workState,
			String workCity, String workCountry, int numDays) {

		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		Date endDate = wtc.getEndDate();
		calendar.setTime(endDate);
		calendar.add(Calendar.DAY_OF_YEAR, -(numDays-1));

		RuleService ruleService = null;
		HolidayListRule holidays = null;
		HolidayRule holidayRule = null;
		AuditEvent event = new AuditEvent();
		if (prod != null && // normal timecard being created (not "blank")
				prod.getPayrollPref().getAutoMarkHolidays() &&
				(! wtc.isNonUnion())) { // and it's a union timecard. (No holidays are marked for non-union)
			// Determine the HolidayListRule in effect
			try {
				ruleService = new RuleService(wtc, prod);
				holidays = ruleService.findHolidayListRule(event);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			if (holidays == null) {
				holidays = HolidayListRuleDAO.getInstance().findDefaultRule();
			}
		}

		// Create a DailyTime object for each day of the week
		List<DailyTime> dailyTimes = new ArrayList<>();
		for (byte i = 1; i < numDays + 1; i++) {
			DailyTime dt = new DailyTime(wtc, i);
			dt.setDate(calendar.getTime()); // Set a day for a week
			dt.setState(workState);
			dt.setCity(workCity);
			dt.setCountry(workCountry);
			dailyTimes.add(dt);
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		wtc.setDailyTimes(dailyTimes); // Add List<DailyTime> to weeklyTimecard

		if (holidays != null) {
			DayType nextDayType = null;
			for (DailyTime dt : dailyTimes) {
				// set DayType to default, unless prior day was a shifted holiday
				dt.setDayType(nextDayType);
				nextDayType = null;
				// Determine if this date is a holiday for anybody.
				HolidayType hType = DateEventDAO.getInstance().findSystemHolidayType(dt.getDate());
				// If so, is the holiday one that applies to this employee?
				if (hType != null && holidays.includesHoliday(hType)) {
					dt.setDayType(DayType.HW); // this ignores rules explicitly set to "holiday paid"
					if (ruleService != null) {
						ruleService.prepareDailyContractRules(dt, event);
						holidayRule = ruleService.findHolidayRule(event);
					}
					DayType holidayType = DayType.HP; // default is "Holiday Paid" for unions
					if (holidayRule != null && ! holidayRule.getPaid()) {
						holidayType = DayType.HU; // rule says not to pay.
					}
					dt.setDayType(holidayType); // assume holiday is celebrated when it occurs
					if (holidayRule != null) {
						log.debug("rule=" + holidayRule.getRuleKey() + ", type=" + holidayType);
						byte wkdaynum = dt.getWeekDayNum();
						if (wkdaynum == 1 && holidayRule.getSundayAsMonday()) { // Sunday holiday, move to Monday
							nextDayType = holidayType; // prep so Monday will get holiday type
							dt.setDayType(null);	// change Sunday back to default DayType
						}
						else if (wkdaynum == 7 && holidayRule.getSaturdayAsFriday()) { // Saturday holiday, move to Friday
							// Friday is assumed to be valid work day (no check on StartForm start date)
							byte daynum = dt.getDayNum();
							if (daynum > 1) {
								dailyTimes.get(daynum-2).setDayType(holidayType); // make prior day the holiday
							}
							dt.setDayType(null);  // reset Saturday to default DayType
						}
					}
				}
			}
		}
	}

	/**
	 * Return a String array with the appropriate short day names, based on the
	 * current Production's & Project's week-ending day preference.
	 */
	public static String[] createTimecardDayLabels() {
		return createTimecardDayLabels(TimecardUtils.findWeekEndDay());
	}

	/**
	 * Create an array of short day-names.
	 *
	 * @param day The week-ending day number (1=Sunday, 7=Saturday).
	 * @return a String array with the appropriate short day names, based on the
	 *         given week-ending day value.
	 */
	public static String[] createTimecardDayLabels(int day) {
		String[] timecardDays = new String[7];
		for (int i=0; i < 7; i++) {
			if (day > 6) day -= 7;
			timecardDays[i] = Constants.WEEKDAY_NAMES.get(day++);
		}
		return timecardDays;
	}

	/**
	 * Create an array of long day-names.
	 *
	 * @param day The week-ending day number (1=Sunday, 7=Saturday).
	 * @return a String array with the appropriate long day names, based on the
	 *         given week-ending day value.
	 */
	public static String[] createTimecardDayLongLabels(int day) {
		String[] timecardDays = new String[7];
		for (int i=0; i < 7; i++) {
			if (day > 6) day -= 7;
			timecardDays[i] = Constants.WEEKDAY_LONG_NAMES.get(day++);
		}
		return timecardDays;
	}

	/**
	 * Update the DayType values in the DailyTime`s within the given timecard.
	 * Any blank (null) DayType entries will be set to the default DayType
	 * (Work). Also, any days worked that have a DayType of Holiday Paid or
	 * Unpaid will be changed to Holiday Worked.
	 * <p>
	 * Any Idle days will have their work zone set to Distant.
	 * <p>
	 * Note that this method does NOT save the updated timecard to the database.
	 *
	 * @param wtc The timecard to be updated.
	 */
	public static void fillDayTypes(WeeklyTimecard wtc) {
		boolean useWorkForHoliday = false; // LS-2366
		if (wtc.isNonUnion()) {
			useWorkForHoliday = SessionUtils.getBoolean(Constants.ATTR_IS_TTC_PROD, false);
		}
		for (DailyTime dt : wtc.getDailyTimes()) {
			// Only change days that have valid hours or "worked" checked
			if (dt.reportedWork()) {
				if (dt.getDayType() == null || dt.getDayType() == DayType.HA) {
					// no day type set yet, or "HOA" (conflicts with reported work); LS-2189
					dt.setDayType(DayType.WK);		// set to default: Worked
				}
				// Day was worked -- check & update Holiday day types
				else if (dt.getDayType() == DayType.HP || dt.getDayType() == DayType.HU) {
					dt.setDayType(DayType.HW);
					if (useWorkForHoliday) { // LS-2366: Team non-union doesn't allow HoliWork,
						dt.setDayType(DayType.WK); // so use Work instead.
					}
				}
			}
			if (dt.getDayType() != null && dt.getDayType().isIdle() &&
					(dt.getWorkZone() == null || dt.getWorkZone().isStudio())) {
				// Force Idle days to be treated as Distant. rev 2.9.5732
				dt.setWorkZone(WorkZone.DL);
			}
		}
	}

	/**
	 * Based on the given day type, set the timecard's rate and guaranteed hours
	 * from the appropriate fields in the StartForm. The timecard's car
	 * allowance will also be set if an amount has been set in the StartForm.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param sd The StartForm providing the rates and amounts.
	 * @param prod The Production containing the timecard.
	 * @param zone The workZone - either distant or studio.
	 * @param usePrep True iff Prep rates should be used regardless of the
	 *            week-ending date in the timecard.
	 */
	public static void fillRates(WeeklyTimecard wtc, StartForm sd, Production prod, WorkZone zone, Boolean usePrep) {
		StartRateSet rates = sd.getProd();
		if (sd.getHasPrepRates() && (sd.getPrep() != null) && (prod != null)) {
			Date prep = prod.getPayrollPref().getPrepEndDate();
			Date priorDate = TimecardUtils.calculatePriorWeekEndDate(wtc.getEndDate());
//			if ( usePrep || (prep != null && ! prep.before(wtc.getEndDate()))) {
			if (usePrep != null) {
				if (usePrep) {
					rates = sd.getPrep();
				}
			}
			else if (prep != null && prep.after(priorDate)) {
				rates = sd.getPrep();
			}
		}
		wtc.setEmployeeRateType(sd.getRateType());

		RateHoursGroup group;
		if (sd.getRateType() == EmployeeRateType.WEEKLY) {
			group = rates.getWeekly();
		}
		else if (sd.getRateType() == EmployeeRateType.HOURLY) {
			group = rates.getHourly();
		}
		else {
			group = rates.getDaily();
		}
		if (zone.equals(WorkZone.DL)) { // Distant/Location
			wtc.setRate(group.getLoc());
			wtc.setGuarHours(group.getLocHrs());

			wtc.setHourlyRate(rates.getHourly().getLoc());
			wtc.setDailyRate(rates.getDaily().getLoc());
			wtc.setWeeklyRate(rates.getWeekly().getLoc());
		}
		else { // Studio
			wtc.setRate(group.getStudio());
			wtc.setGuarHours(group.getStudioHrs());

			wtc.setHourlyRate(rates.getHourly().getStudio());
			wtc.setDailyRate(rates.getDaily().getStudio());
			wtc.setWeeklyRate(rates.getWeekly().getStudio());
		}

		if (zone.isStudio()) {
			updatePayExpense(wtc, PayCategory.CAR_ALLOWANCE, sd.getCarAllow().getStudio(), sd.getCarAllow());
		}
		else if (zone.isDistant()) {
			updatePayExpense(wtc, PayCategory.CAR_ALLOWANCE, sd.getCarAllow().getLoc(), sd.getCarAllow());
		}
	}

	/**
	 * Add expense line items to the given timecard for those items that are
	 * included in the given StartForm, including Car allowance and Per Diem.
	 *
	 * @param wtc The WeeklyTimecard to which the expense items will be added.
	 * @param sf The StartForm that specifies the amounts for various expense
	 *            items, if any.
	 */
	private static void createExpenseLines(WeeklyTimecard wtc, StartForm sf) {
		wtc.setExpenseLines(new ArrayList<PayExpense>());
		byte linenum = 0;
		BigDecimal nullDec = null;

		// 2.9.5416 - omit box rental
		if (sf.isDistantRate()) {
			linenum =  wtc.addExpense(linenum, ExpenseType.CR, sf.getCarAllow().getLoc(), null,
					(sf.getCarAllow().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					sf.getCarAllow(), null);

			linenum = wtc.addExpense(sf.getMealAllow().getLoc(),
					(sf.getMealAllow().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MEAL_ALLOWANCE, linenum, sf.getMealAllow(), null);

			linenum = wtc.addExpense(sf.getMealMoney().getLoc(),
					(sf.getMealMoney().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MEAL_MONEY, linenum, sf.getMealMoney(), null);

			linenum = wtc.addExpense(sf.getMealMoneyAdv().getLoc(),
					(sf.getMealMoneyAdv().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MM_PER_DIEM_ADVANCE, linenum, sf.getMealMoneyAdv(), null);

		}
		else {
			linenum =  wtc.addExpense(linenum, ExpenseType.CR, sf.getCarAllow().getStudio(), null,
					(sf.getCarAllow().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					sf.getCarAllow(), null);

			linenum = wtc.addExpense(sf.getMealAllow().getStudio(),
					(sf.getMealAllow().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MEAL_ALLOWANCE, linenum, sf.getMealAllow(), null);

			linenum = wtc.addExpense(sf.getMealMoney().getStudio(),
					(sf.getMealMoney().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MEAL_MONEY, linenum, sf.getMealMoney(), null);

			linenum = wtc.addExpense(sf.getMealMoneyAdv().getStudio(),
					(sf.getMealMoneyAdv().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.MM_PER_DIEM_ADVANCE, linenum, sf.getMealMoneyAdv(), null);
		}
		if (! wtc.getProduction().getType().isTours()) {
			linenum = wtc.addExpense(linenum, ExpenseType.PD, sf.getPerdiemTx().getAmt(), nullDec,
					(sf.getPerdiemTx().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					sf.getPerdiemTx(), null);

			linenum = wtc.addExpense(linenum, ExpenseType.PD, nullDec, sf.getPerdiemNtx().getAmt(),
					(sf.getPerdiemNtx().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					sf.getPerdiemNtx(), null);

			linenum = wtc.addExpense(sf.getPerdiemAdv().getAmt(),
					(sf.getPerdiemAdv().getWeekly() ? BigDecimal.ONE : BigDecimal.ZERO),
					PayCategory.PER_DIEM_ADVANCE, linenum, sf.getPerdiemAdv(), null);
		}
		if (sf.getModelRelease() != null) {
			Integer mrId = sf.getModelRelease().getId();
			FormModelRelease modelRelease = (FormModelRelease)FormService.getInstance().findById(mrId,
					PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
			linenum = wtc.addExpense(modelRelease.getNonTaxableTotal(), BigDecimal.ONE, PayCategory.REIMB, linenum, sf.getAccount(), null);
			linenum = wtc.addExpense(modelRelease.getTaxableTotal(), BigDecimal.ONE, PayCategory.REBTX, linenum, sf.getAccount(), null);
		}
	}

	/**
	 * Determine which days, if any, within one payroll week do not have a
	 * StartForm covering them. If the given StartForm does not cover the entire
	 * week specified, then the database is checked for other StartForms for the
	 * same user, production, and occupation to determine, for each day in the
	 * week, whether or not there is a StartForm valid for that day.
	 *
	 * @param sf The StartForm assigned to the WeeklyTimecard being analyzed.
	 *            While this would normally cover some part of the week being
	 *            checked, that is not required for this method to work.
	 * @param endDate The week-ending date for the week to be checked.
	 * @return A boolean array where each entry represents a day of the week,
	 *         with True meaning there is NO StartForm for that day, False
	 *         meaning there IS a StartForm for that day (or, for now, a day
	 *         preceding that day -- we're not enforcing end dates currently).
	 */
/*	private static boolean[] findMissingStartFormDates(StartForm sf, Date endDate) {
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		calendar.setTime(endDate);
		calendar.add(Calendar.DAY_OF_YEAR, -6);
		Date startDate = calendar.getTime();

		boolean noSf[];
		if (sf.getEffectiveOrStartDate().after(startDate) ||
				//sf.getEffectiveEndDate().before(endDate) ||
				sf.getWorkStartDate().after(startDate)
				//sf.getWorkEndDate().before(endDate)
				) {
			// This StartForm starts after the beginning of the week; determine if
			// there are days in this week not covered by ANY StartForm.
			noSf = new boolean[]{true,true,true,true,true,true,true}; // assume NO days covered

			List<StartForm> forms = StartFormDAO.getInstance().
					findByActiveDateContactJob(startDate, endDate, sf.getContact(), sf.getJobClass());
			Date checkDate =  startDate;
			for (int i = 0; i <= 6; i++) {
				for (StartForm form : forms) {
					if ((! form.getEffectiveOrStartDate().after(checkDate)) &&
							//(! form.getEffectiveEndDate().before(checkDate)) &&
							(! form.getWorkStartDate().after(checkDate))
							//(! form.getWorkEndDate().before(checkDate))
							) {
						noSf[i] = false; // have a StartForm that starts prior to or on this day in week
						// NOTE: for now we aren't enforcing end-dates
						break; // don't need to check other forms
					}
				}
				calendar.add(Calendar.DAY_OF_MONTH, 1);
				checkDate = calendar.getTime();
			}
		}
		else { // StartForm starts before or on the start of this week ... no detailed checks needed.
			// Return array showing all days valid
			noSf = new boolean[]{false,false,false,false,false,false,false};
		}
		return noSf;
	}
*/

	/**
	 * Copy all fields (optionally excluding DayType) that may be entered by an
	 * employee from one timecard to another timecard. Also, if either the
	 * source or target have a Holiday DayType specified for a particular day,
	 * then the information for that day is only copied if the 'doHoliday'
	 * parameter is true.
	 * <p>
	 * The purpose of this method is to copy data from one timecard to another
	 * timecard for the same employee, but a different week-ending date. This is
	 * why we don't copy holiday days, as they are unlikely to match from one
	 * week to another. This is in contrast to the CloneTimecardBean code that
	 * is designed for copying one employee's data into a different employee's
	 * timecard for the SAME week-ending date.
	 *
	 * @param fromTc Source timecard that data is being copied from.
	 * @param toTc Target timecard that data is being copied to.
	 * @param doDayType The DayType value is copied from the old timecard to the
	 *            target timecard iff this value is true.
	 * @param doHolidays If true, employee data occurring on holidays is also
	 *            copied. This is designed for use in a "retro" situation, where
	 *            the copy is for the same week as the original.
	 */
	public static void copyEmpData(WeeklyTimecard fromTc, WeeklyTimecard toTc,
			boolean doDayType, boolean doHolidays) {

		// Copy daily hours
		int ix = 0;
		for (DailyTime fromDt : fromTc.getDailyTimes()) {
			DailyTime toDt = toTc.getDailyTimes().get(ix++);
			if (doHolidays ||
					((fromDt.getDayType() == null || ! fromDt.getDayType().isHoliday()) &&
					(toDt.getDayType() == null || ! toDt.getDayType().isHoliday()))) {
				toDt.setWorked(fromDt.getWorked());
				if (doDayType) {
					toDt.setWorkZone(fromDt.getWorkZone());
					toDt.setDayType(fromDt.getDayType());
				}
				toDt.setCallTime(fromDt.getCallTime());
				toDt.setM1Out(fromDt.getM1Out());
				toDt.setM1In(fromDt.getM1In());
				toDt.setM2Out(fromDt.getM2Out());
				toDt.setM2In(fromDt.getM2In());
				toDt.setWrap(fromDt.getWrap());
				// Don't copy On-Call Start/End, they will be recomputed by Fill Jobs.
				toDt.setMpvUser(fromDt.getMpvUser());
				toDt.setHours(fromDt.getHours());
				toDt.setNonDeductMeal(fromDt.getNonDeductMeal());
				toDt.setNonDeductMeal2(fromDt.getNonDeductMeal2());
			}
		}

		// Copy Box Rental info...
		if (fromTc.getBoxRental() != null) {
			BoxRental fromBox = fromTc.getBoxRental();
			BoxRental box = toTc.getBoxRental();
			if (box == null) {
				box = fromBox.clone();
				box.setWeeklyTimecard(toTc);
				toTc.setBoxRental(box);
			}
			else {
				box.setAmount(fromBox.getAmount());
				box.setComments(fromBox.getComments());
				box.setInventoryEdit(fromBox.getInventoryEdit()); // sets 'inventory' too
				box.setInventoryOnFile(fromBox.getInventoryOnFile());
				box.setMatchesStart(fromBox.getMatchesStart());
			}
		}

		// Mileage info is NOT copied

		TimecardCalc.calculateWeeklyTotals(toTc);
	}

	/**
	 * Creates a new PayJob entity, complete with 7 PayJobDaily embedded
	 * objects. Account codes and rates are copied from the timecard into the
	 * PayJob.
	 *
	 * @param wtc The WeeklyTimecard this new PayJob will be associated with.
	 * @param jobNum The "job number" (origin 1) to assign to this new PayJob.
	 * @return The newly created, transient, PayJob entity. Note that it has NOT
	 *         been added to the database.
	 */
	public static PayJob createPayJob(WeeklyTimecard wtc, byte jobNum) {
		StartForm sd = wtc.getStartForm();
		sd = StartFormDAO.getInstance().refresh(sd);

		PayJob pj = new PayJob(wtc, jobNum);
		pj.setAccountCodes(wtc.getAccount());
		pj.setJobAccountNumber(wtc.getJobNumber());
		pj.setJobName(wtc.getJobName());
		pj.setOccCode(sd.getOccupationCode());
//		if (jobNum == 1) {
			pj.setRate(wtc.getHourlyRate());
			pj.setDailyRate(wtc.getDailyRate());
			pj.setWeeklyRate(wtc.getWeeklyRate());
//		}
		PayJobDaily.createPayJobDailies(pj, wtc.getEndDate());
		return pj;
	}

	/**
	 * Called to place the user into a Production. Note that the user may
	 * have been deleted from the production, but will still be allowed "in",
	 * so they can access their timecards.
	 *
	 * @param id The database id of the Production to be entered.
	 * @return The Production object if the user has been placed into the
	 *         Production; null if not, which can occur if the user does not
	 *         belong to the production.
	 */
	public static Production enterProductionForTimecards(Integer id) {
		Production production = ProductionDAO.getInstance().findById(id);
		if (production != null) {
			Contact contact = ContactDAO.getInstance()
					.findByUserProduction(SessionUtils.getCurrentUser(), production);
			if (contact != null) {
				SessionUtils.setCurrentContact(contact);
				SessionSetUtils.setProduction(production);
				if ( ! LoginBean.checkProjectAccess(contact)) {
					log.debug("No normal access; timecard access allowed");
				}
			}
			else {
				production = null;
			}
		}
		return production;
	}

	/**
	 * Determine if the given User ever Approved the given WeeklyTimecard.
	 * @param wtc The WeeklyTimecard to be checked.
	 * @param user The User's whose signature we are looking for.
	 * @return True iff there is an Approve event for this WeeklyTimecard signed
	 * by the specified User.
	 */
	public static boolean findDidApprove(WeeklyTimecard wtc, User user) {
		String acct = user.getAccountNumber();
		for (TimecardEvent evt : wtc.getTimecardEvents()) {
			if (evt.getType() == TimedEventType.APPROVE && evt.getUserAccount().equals(acct)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine the contact.id value of the user who is next in line to approve
	 * the currently displayed WeeklyTimecard.
	 *
	 * @return The Contact.id of the user whose is supposed to approve the
	 *         current timecard. Returns null if no approver exists.
	 */
	public static Integer findApproverContactId(WeeklyTimecard weeklyTimecard) {
		if (weeklyTimecard.getApproverContactId() == null) {
			if (weeklyTimecard.getApproverId() == null) {
				return null;
			}
			Approver app = ApproverDAO.getInstance().findById(weeklyTimecard.getApproverId());
			if (app != null && app.getContact() != null) {
				weeklyTimecard.setApproverContactId(app.getContact().getId());
			}
		}
		return weeklyTimecard.getApproverContactId();
	}

	/**
	 * Given a WeeklyTimecard, find the Contact of the next approver in
	 * the chain -- either the department chain, or the production chain.
	 *
	 * @param wtc The WeeklyTimecard of interest. The next approver is
	 *            determined based on the value of the current approver in this
	 *            object.
	 * @return The next approver's Contact value, or null if no additional
	 *         approvers are on the chain (the current one is the last
	 *         approver).
	 */
	public static Contact findNextApproverContact(WeeklyTimecard wtc) {
		Approver app = findNextApprover(wtc, null);
		if (app != null) {
			return app.getContact();
		}
		return null;
	}

	/**
	 * Given a Approvable, find the Contact.id value of the next approver in
	 * the chain -- either the department chain, or the production chain.
	 *
	 * @param appr The approvable of interest. The next approver is
	 *            determined based on the value of the current approver in this
	 *            object.
	 * @return The next approver's Contact.id value, or null if no additional
	 *         approvers are on the chain (the current one is the last
	 *         approver).
	 */
	public static Integer findNextApproverContactId(Approvable appr) {
		Integer id = null;
		Approver app = findNextApprover(appr, null);
		if (app != null && app.getContact() != null) {
			id = app.getContact().getId();
		}
		return id;
	}

	/**
	 * Find the Contact.id value of the Approver AFTER the next Approver for the
	 * given WeeklyTimecard.
	 *
	 * @param wtc The WeeklyTimecard of interest. The next approver is
	 *            determined based on the value of the current approver in this
	 *            object.
	 * @return The Contact.id value of the Approver after the next Approver for
	 *         this timecard, or null if there are less than two additional
	 *         approvers in the chain.
	 */
	public static Integer findNextNextApproverContactId(WeeklyTimecard wtc) {
		Integer id = null;
		Approver app = findNextNextApprover(wtc);
		if (app != null && app.getContact() != null) {
			id = app.getContact().getId();
		}
		return id;
	}

	/**
	 * Find the Approver AFTER the next Approver for the given WeeklyTimecard.
	 *
	 * @param wtc The WeeklyTimecard of interest. The next approver is
	 *            determined based on the value of the current approver in this
	 *            object.
	 * @return The Approver after the next Approver for this timecard, or null
	 *         if there are less than two additional approvers in the chain.
	 */
	private static Approver findNextNextApprover(WeeklyTimecard wtc) {
		if (wtc.getStatus() == ApprovalStatus.APPROVED) {
			return null; // all done
		}
		Integer currentAppId = wtc.getApproverId();
		Department dept = wtc.getDepartment();
		Production prod = findProduction(wtc);
		Project project = findProject(prod, wtc);
		Approver app = ApproverUtils.findApproverAfter(prod, project, currentAppId, dept, null);
		if (app != null) {
			return ApproverUtils.findApproverAfter(prod, project, app.getId(), dept, null);
		}
		return app;
	}

	/**
	 * Find the next Approver for the given Approvable object.
	 *
	 * @param appr The Approvable of interest. The next approver is
	 *            determined based on the value of the current approver in this
	 *            object.
	 * @param approvalPathId The id of the ApprovalPath being followed. Will be
	 *            null for timecards, and not null for other Approvable's.
	 * @return The next Approver for this  Approvable object, or null if no additional
	 *         approvers are in the chain.
	 */
	public static Approver findNextApprover(Approvable appr, Integer approvalPathId) {
		log.debug("path id "+approvalPathId);
		if (appr.getStatus() == ApprovalStatus.APPROVED) {
			return null; // all done
		}
		Integer currentAppId = appr.getApproverId();
		/*if(currentAppId != null) {
			if (currentAppId < 0) {
				Approver approver = new Approver();
//				approver.setId((-1)*approvalPathId);
				// Anshaj: if the above caused problems due to null approvalPathId,
				// isn't the following statement equivalent? :
				approver.setId(currentAppId);
				return approver;
			}
		}*/
		Department dept;
		if(approvalPathId != null) {
			ContactDocument cd = (ContactDocument) appr;
			dept = cd.getEmployment().getDepartment();
		}
		else {
			dept = appr.getDepartment();
		}
		Production prod = findProduction(appr);
		Project project = findProject(prod, appr);
		return ApproverUtils.findApproverAfter(prod, project, currentAppId, dept, approvalPathId);
	}

	/**
	 * Determine if a given Contact follows Approvable's [WeeklyTimecard or ContactDocument] current approver, that
	 * is, it appears anywhere later in the approval chain.
	 *
	 * @param appr The Approvable of interest.
	 * @param contact The Contact we are searching for in the approval chain.
	 * @param path The ApprovalPath followed by the ContactDocument being recalled.
	 * @return True iff the given Contact comes 'later' in the approval chain
	 *         than the current approver of the given Approvable object.
	 */
	public static boolean followsCurrentApprover(Approvable appr, Project project, Contact contact, ApprovalPath path) {
		log.debug("");
		Department dept = null;
		Boolean isDeptPool = null;
		if (path != null) {
			ContactDocument cd = (ContactDocument) appr;
			dept = cd.getEmployment().getDepartment();
			isDeptPool = appr.getIsDeptPool();
		}
		else {
			dept = appr.getDepartment();
		}
		return ApproverUtils.followsCurrentApprover(appr.getApproverId(), contact, project, dept, path, isDeptPool);
	}

	/**
	 * Determine if a given Contact precedes a Approvable's [WeeklyTimecard or ContactDocument] current approver.
	 *
	 * @param appr The Approvable of interest.
	 * @param contact The Contact we are searching for in the approval chain.
	 * @param path The ApprovalPath followed by the ContactDocument being recalled.
	 * @return True iff the given Contact comes 'earlier' in the approval chain
	 *         than the current approver of the given Approvable object.
	 */
	public static boolean precedesCurrentApprover(Approvable appr, Project project, Contact contact, ApprovalPath path) {
		Department dept = null;
		Boolean isDeptPool = null;
		if (path != null) {
			ContactDocument cd = (ContactDocument) appr;
			dept = cd.getEmployment().getDepartment();
			isDeptPool = appr.getIsDeptPool();
		}
		else {
			dept = appr.getDepartment();
		}
		return ApproverUtils.precedesCurrentApprover( appr.getApproverId(), contact, project, dept, path, isDeptPool);
	}

	/**
	 * Determine the PayrollPreference instance currently in effect.
	 *
	 * @param sf A StartForm which may be used to determine the Project or
	 *            Production of interest.
	 * @return For a commercial Production, the PayrollPreference for the
	 *         Project related to the given StartForm. For a non-commercial
	 *         Production, the PayrollPreference associated with either the
	 *         current Production, if any, or else the Production that owns the
	 *         given StartForm.
	 */
	public static PayrollPreference findPreference(StartForm sf) {
		PayrollPreference pref;
		if (sf.getProject() == null) { // TV/Feature
			Production prod = findProduction(sf);
			pref = prod.getPayrollPref();
		}
		else { // Commercial
			pref = sf.getProject().getPayrollPref();
		}
		return pref;
	}

	/**
	 * Determine the "current" production, either from the environment, or from
	 * the given StartForm. This technique is necessary for the "My Timecard"
	 * page, where the current production may be null, or the System production,
	 * which is not what we're interested in.
	 *
	 * @param sf The StartForm being manipulated or viewed.
	 * @return The current production, if it is not the System Production,
	 *         otherwise the Production to which the given StartForm belongs.
	 */
	public static Production findProduction(StartForm sf) {
		Production prod = SessionUtils.getNonSystemProduction();
		if (prod == null) {
			prod = sf.getContact().getProduction();
		}
		return prod;
	}

	/**
	 * Determine the "current" production, either from the environment, or from
	 * the given approvable. This technique is necessary for the "My Timecard"
	 * page, where the current production may be null, or the System production,
	 * which is not what we're interested in.
	 *
	 * @param appr The Approvable being manipulated or viewed.
	 * @return The current production, if it is not the System Production,
	 *         otherwise the Production to which the given timecard belongs.
	 */
	public static Production findProduction(Approvable appr) {
		Production prod = SessionUtils.getNonSystemProduction();
		if (prod == null) {
			prod = appr.getProduction();
		}
		return prod;
	}

	/**
	 * Determine the "current" production, either from the environment, or from
	 * the given production_id (String) value.
	 *
	 * @param prodId The prod_id value of the desired production; this is the
	 *            Production.prodId String value, not the Production.id integer.
	 * @return The current production, if it is not the System Production,
	 *         otherwise the Production to which the given timecard belongs.
	 */
	public static Production findProduction(String prodId) {
		Production prod = SessionUtils.getNonSystemProduction();
		if (prod == null) {
			prod = ProductionDAO.getInstance().findByProdId(prodId);
		}
		return prod;
	}

	/**
	 * Determine the Project associated with a given approvable. In a TV/Feature
	 * production, this will be null; in a Commercial production timecard, the
	 * project is determined from the related StartForm.
	 *
	 * @param prod The current Production, which should be the production
	 *            containing the timecard.
	 * @param appr The Approvable of interest.
	 * @return The Project associated with the give timecard.
	 */
	public static Project findProject(Production prod, Approvable appr) {
		Project project = null;
		if (prod.getType().hasPayrollByProject()) {
			if (appr == null) { // unusual
				project = SessionUtils.getCurrentProject();
			}
			else {
				if (appr instanceof WeeklyTimecard) {
					project = ((WeeklyTimecard) appr).getStartForm().getProject();
				}
				else {
					project = ((ContactDocument) appr).getProject();
				}
			}
		}
		return project;
	}

	/**
	 * Find all the User`s who have a crew Role in the current production, or
	 * have a Start Form in the current production.  In addition, if the Production
	 * has the castUseCrewTc option set to true, then User`s with cast Roles are
	 * also included.
	 *
	 * @return A non-null List of User`s matching the criteria above.
	 */
	public static List<User> findTimecardUsers() {
		List<User> users = new ArrayList<>();
		Production prod = SessionUtils.getNonSystemProduction();
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		List<Contact> contacts = ContactDAO.getInstance().findByProperty(ContactDAO.PRODUCTION, prod);
		for (Contact c : contacts) {
			boolean added = false;
			if (c.getEmployments().size() > 0) {
				for (Employment emp : c.getEmployments()) {
					if (emp.getRole().isCrewTc() ||
							(prod.getCastUseCrewTc() && emp.getRole().isCastOrStunt())) {
						users.add(c.getUser());
						added = true;
						break; // only add a user once
					}
				}
				if (! added) {
					// add someone if they have a StartForm (even if they no longer have a crew role)
					if (startFormDAO.existsContact(c)) {
						users.add(c.getUser());
					}
				}
			}
			else { // either a deleted contact, or all their roles were deleted
				if (startFormDAO.existsContact(c)) {
					users.add(c.getUser());
				}
			}
		}
		return users;
	}

	/**
	 * Calculate the number of days in a given timecard that were reported as
	 * worked by the employee, either by having entered hours for a day, or
	 * having checked the "worked" box.
	 *
	 * @param wtc The timecard in question.
	 * @return The number of days worked, from 0 to 7.
	 */
	public static int calculateDaysWorked(WeeklyTimecard wtc) {
		int n = 0;
		for (DailyTime dt : wtc.getDailyTimes()) {
			// Only count days that have valid hours or "worked"
			if (dt.reportedWork()) {
				n++;
			}
		}
		return n;
	}

	/**
	 * Determine if any day in a weeklyTimecard is a holiday.
	 *
	 * @param wtc The timecard to inspect.
	 * @return True iff at least one day in the timecard is a holiday DayType.
	 */
	public static boolean calculateHasHoliday(WeeklyTimecard wtc) {
		boolean ret = false;
		for (DailyTime dt : wtc.getDailyTimes()) {
			if (dt.getDayType() != null && dt.getDayType().isHoliday()) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	/**
	 * Determine if a given timecard has a mixture of Studio and Distant work
	 * days.
	 *
	 * @param wtc The timecard in question.
	 * @return True iff the days worked consist of a mixture of Studio and
	 *         Distant types.
	 */
	public static boolean calculateIsMixedWeek(WeeklyTimecard wtc) {
		boolean ret = false;

		/** The default day type to use for non-area types (like Holiday) */
		WorkZone defZone = wtc.getDefaultZone();

		/** The day type (studio or distant) of all the work days inspected so far. */
		WorkZone zone = null;

		for (DailyTime dt : wtc.getDailyTimes()) {
			// Only inspect days that have valid hours or "worked"
			if (dt.reportedWork()) {
				if (dt.getWorkZone() != null) {
					if (zone == null) { // this is our first work day
						zone = dt.getWorkZone();
					}
					else { // see if this day matches prior day(s)
						if (dt.getWorkZone().isStudio() && zone.isDistant()) {
							ret = true; // no, this is studio & prior was distant: mixed week
							break;
						}
						else if (dt.getWorkZone().isDistant() && zone.isStudio()) {
							ret = true; // no, this is distant, and prior was studio: mixed week
							break;
						}
					}
				}
				else { // today's zone is unknown
					if (zone == null) { // this is our first work day
						zone = defZone; // treat it as the default day type
					}
					else { // not our first work day
						if (zone != defZone) { // prior day(s) don't match default type
							ret = true;	// so this is a mixed week.
							break;
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * @return the Date of the last *payroll* day of the current week for the
	 *         current Production and Project with time stamp 00:00:00. This is
	 *         typically the Saturday date, but will be different if the
	 *         production has altered the payroll week-ending day preference.
	 */
	public static Date calculateLastDayOfCurrentWeek() {
		return calculateLastDayOfCurrentWeek(findWeekEndDay());
	}

	/**
	 * Calculate the Date of the last *payroll* day of the current week.
	 *
	 * @param wkEndDay The week-ending day number to use to calculate the last
	 *            Date of the payroll week. 1=Sunday, 7=Saturday.
	 * @return the Date of the last *payroll* day of the current week, e.g.,
	 *         SATURDAY, with time stamp 00:00:00, using the given week-day
	 *         number to determine the last day.
	 */
	public static Date calculateLastDayOfCurrentWeek(int wkEndDay) {
		// Get calendar set to current date and time with application's time-zone
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());

		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
		// Set the calendar to Saturday (or other week-end-day) of the current week
		calendar.set(Calendar.DAY_OF_WEEK, wkEndDay);
		if (wkEndDay < weekDay) { // Calendar was shifted back, not forward.
			calendar.add(Calendar.DAY_OF_WEEK, 7);
		}
		// Set time to beginning of day (12:00:00 AM)
		CalendarUtils.setStartOfDay(calendar);

		Date date = calendar.getTime();

		//log.debug("week end date = " + date); // e.g - Sat Feb 11 00:00:00 PST 2012
		return date;
	}


	/**
	 * @return The week-ending date to be used as the default to display on
	 *         various dashboard pages. If the current day is Sunday through
	 *         Thursday, the default is last week's end date, since the
	 *         dashboard users -- accountants and approvers -- will most likely
	 *         be working on last week's timecards. If the day is Friday or
	 *         Saturday, then the default will be this week's end date.
	 */
	public static Date calculateDefaultWeekEndDate() {
		return calculateDefaultWeekEndDate(findWeekEndDay());
	}

	/**
	 * @return The week-ending date to be used as the default to display on
	 *         various dashboard pages. If the current day is Sunday through
	 *         Thursday, the default is last week's end date, since the
	 *         dashboard users -- accountants and approvers -- will most likely
	 *         be working on last week's timecards. If the day is Friday or
	 *         Saturday, then the default will be this week's end date.
	 */
	public static Date calculateDefaultWeekEndDate(int wkEndDay) {
		// Get calendar set to current date and time with application's time-zone
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		Calendar today = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());

		// remember current day of week
		int day = today.get(Calendar.DAY_OF_WEEK);

		calendar.set(Calendar.DAY_OF_WEEK, wkEndDay);
		if (calendar.before(today)) {
			calendar.add(Calendar.DAY_OF_MONTH, 7);
		}
		// Set time to beginning of day (12:00:00 AM)
		CalendarUtils.setStartOfDay(calendar);

		// backup to prior week if not within 1 day of week-end day
		if (day == wkEndDay || day == wkEndDay-1 ||
				(day == 7 && wkEndDay == 1 )) {
			// today is last day of week, or prior day,
			// so keep this W/E day as the default
		}
		else {
			// today is earlier in week, so backup default
			// ...to W/E date of prior week.
			calendar.add(Calendar.DAY_OF_MONTH, -7);
		}

		Date date = calendar.getTime();
		log.debug("wkEndDay = " + wkEndDay + ", week end date = " + date); // e.g - Sat Feb 11 00:00:00 PST 2012
		return date;
	}

	/**
	 * Find the *payroll* week-end date corresponding to a given date for the
	 * current Production and/or Project.
	 *
	 * @param date The date of any day during the week for which we are to find
	 *            the week-end date. The time portion (within the day) of this
	 *            parameter is irrelevant.
	 * @return The Date of the payroll "week-end" day, typically SATURDAY, with
	 *         time stamp 00:00:00, for the week that contains the given Date.
	 *         The day may NOT be a Saturday, as the current Production or
	 *         Project may have set the week-ending day to something other than
	 *         Saturday.
	 */
	public static Date calculateWeekEndDate(Date date) {
		return calculateWeekEndDate(date, findWeekEndDay());
	}

	/**
	 * Find the *payroll* week-end date corresponding to a given date for the
	 * specified Production and Project.
	 *
	 * @param date The date of any day during the week for which we are to find
	 *            the week-end date. The time portion (within the day) of this
	 *            parameter is irrelevant.
	 * @param prod The Production whose week-end date is to be calculated; if
	 *            null, the current Production is used.
	 * @param proj The Project whose week-end date is to be calculated; only
	 *            used for Commercial productions. If null, the current Project
	 *            is used.
	 * @return The payroll "week-end" date of the week, typically SATURDAY, with
	 *         time stamp 00:00:00, for the payroll week that contains the given
	 *         Date. The day may NOT be a Saturday, as the current Production or
	 *         Project may have set the week-ending day to something other than
	 *         Saturday.
	 */
	public static Date calculateWeekEndDate(Date date, Production prod, Project proj) {
		int wkEndDay;
		if (prod == null) { // use current Production & Project
			wkEndDay = findWeekEndDay();
		}
		else {
			wkEndDay = findWeekEndDay(prod, proj);
		}
		return calculateWeekEndDate(date, wkEndDay);
	}

	/**
	 * Find the *payroll* week-end date corresponding to a given date assuming
	 * the given 'week-ending day' value (typically 7=Saturday).
	 *
	 * @param date The date of any day during the week for which we are to find
	 *            the week-end date. The time portion (within the day) of this
	 *            parameter is irrelevant.  If this is null, then null is returned.
	 * @param wkEndDay The week-ending day number (1=Sunday, 7=Saturday) to use
	 *            when calculating the Date of the last day of the payroll week.
	 * @return The payroll "week-end" date of the week, typically SATURDAY, with
	 *         time stamp 00:00:00, for the payroll week that contains the given
	 *         Date. The day may NOT be a Saturday; the day will match the given
	 *         wkEndDay parameter.  Returns null if 'date' is null.
	 */
	public static Date calculateWeekEndDate(Date date, int wkEndDay) {
		if (date == null) {
			return null;
		}
		// Get calendar set to application's time-zone
		Calendar calDate = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		calDate.setTime(date); // set the given date

		int weekDay = calDate.get(Calendar.DAY_OF_WEEK);
		// Set the calendar to Saturday (actually, week-end-day preference) of the given week
		calDate.set(Calendar.DAY_OF_WEEK, wkEndDay);
		if (wkEndDay < weekDay) {
			calDate.add(Calendar.DAY_OF_WEEK, 7);
		}
		// Set time to beginning of day (12:00:00 AM)
		CalendarUtils.setStartOfDay(calDate);

		date = calDate.getTime();
		//log.debug("week end date = " + date); // e.g - Sat Feb 11 00:00:00 PST 2012
		return date;
	}

	/**
	 * Return one week prior to the provided Date.
	 *
	 * @param weDate An existing week-end Date.
	 * @return The week-end Date of the prior week, exactly seven days prior to
	 *         the supplied Date.
	 */
	public static Date calculatePriorWeekEndDate(Date weDate) {
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		calendar.setTime(weDate);
		calendar.add(Calendar.DAY_OF_YEAR, -7);
		weDate = calendar.getTime();
		return weDate;
	}

	/**
	 * Return one week after the provided Date.
	 *
	 * @param weDate An existing week-end Date.
	 * @return The week-end Date of the following week, exactly seven days after
	 *         the supplied Date.
	 */
	/*package*/ static Date calculateNextWeekEndDate(Date weDate) {
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		calendar.setTime(weDate);
		calendar.add(Calendar.DAY_OF_YEAR, 7);
		weDate = calendar.getTime();
		return weDate;
	}

	/**
	 * Determine the last week-end date that is valid for the given StartForm,
	 * or, if there is no end date in the StartForm, then the last week-end date
	 * that occurs within 14 days past today's date (the default), or farther if
	 * the production has set the Payroll Preference for "max weeks in advance"
	 * to more than one.
	 *
	 * @param sd The StartForm whose end date is to be determined; if null, an
	 *            exception will be thrown.
	 * @param pref The PayrollPreference to use for determining the number of
	 *            weeks in advance to push the 'last w/e date'. May be null.
	 * @return The week-end date as described above; never returns null.
	 */
	private static Date calculateLastWeekEndDate(StartForm sd, PayrollPreference pref) {
		Calendar cal = Calendar.getInstance();
		if (sd.getEmployment() != null && sd.getEmployment().getEndDate() != null) {
			// Employment endDate takes precedence over StartForm (#541, 3.2.8076)
			cal.setTime(sd.getEmployment().getEndDate());
		}
		else if (sd.getEarliestEndDate() != null) {
			cal.setTime(sd.getEarliestEndDate());
		}
		else {
			if (pref == null) {
				// note: only use production preference for max weeks
				pref = findProduction(sd).getPayrollPref();
			}
			// Note that caller will do final determination; this value should be at
			// least high enough (late enough) to encompass the final date desired.
			int days = (pref.getMaxWeeksInAdvance() * 7) + 3;
			cal.add(Calendar.DAY_OF_MONTH, days);
		}
		CalendarUtils.setStartOfDay(cal); // set to midnight
		Date actualLast = cal.getTime();
		cal.set(Calendar.DAY_OF_WEEK, findWeekEndDay(sd)); // shift if week-start day is not Saturday
		Date lastDate = cal.getTime(); // the adjusted W/E date
		if (lastDate.before(actualLast)) { // week-start day shifted W/E date earlier,
			cal.add(Calendar.DAY_OF_MONTH, 7); // so push it one week later
			lastDate = cal.getTime();
		}
		return lastDate;
	}

	/**
	 * Determine the last timecard W/E date that the user should be allowed to
	 * create for the given StartForm, considering any end-dates in the Start,
	 * and payroll preferences for 'timecard may be created <n> weeks in advance'.
	 *
	 * @param sd The StartForm whose effective-end or work-end will be checked
	 *            to determine the last valid W/E date.
	 * @param pref The PayrollPreference to use; if null, the appropriate one
	 *            will be found based on the StartForm supplied. This is needed
	 *            to determine the maximum number of weeks in advance that a
	 *            timecard may be created.
	 * @return the last W/E date for a timecard that the user should be allowed
	 *         to create for the given StartForm. This date will be appropriate
	 *         for the production or project's week-start day.
	 */
	public static Date calculateLastNewDate(StartForm sd, PayrollPreference pref) {
		if (pref == null) {
			// note: only use Production preference for max weeks
			pref = findProduction(sd).getPayrollPref();
		}
		// Get the last week-end date to show, based on ending dates in StartForm
		Date lastDate = TimecardUtils.calculateLastWeekEndDate(sd, pref);
		//log.debug(sdfDebug.format(lastDate));

		Calendar cal = Calendar.getInstance(); // get calendar with current date/time
		CalendarUtils.setStartOfDay(cal); // set calendar time to midnight

		int daysExtend = ((pref.getMaxWeeksInAdvance()-1) * 7) + 3;
		cal.add(Calendar.DAY_OF_MONTH, daysExtend);
		Date extDate = cal.getTime();

		cal.set(Calendar.DAY_OF_WEEK, findWeekEndDay(sd)); // Saturday or floating W/E day
		if (extDate.after(cal.getTime())) {
			// the W/E day setting caused the date to be too early,
			cal.add(Calendar.DAY_OF_MONTH, 7); // so push it later one week
		}

		//log.debug(sdfDebug.format(cal.getTime()));
		if (lastDate.after(cal.getTime())) {
			// Return the EARLIER of SF's last-date and advanced date
			lastDate = cal.getTime();
		}
		log.debug(lastDate);
		return lastDate;
	}

	/**
	 * Find the current Production or Project's preference for weekEndDay, which
	 * defaults to Saturday.
	 *
	 * @return The week-ending day specified in the Production's or Project's
	 *         payroll preferences. (1=Sunday, 7=Saturday)
	 *
	 */
	public static int findWeekEndDay() {
		Integer wkDay = SessionUtils.getInteger(Constants.ATTR_TC_WEEK_END_DAY);
		if (wkDay == null) {
			wkDay = Constants.DEFAULT_WEEK_END_DAY;
			Production prod = SessionUtils.getNonSystemProduction();
			if (prod != null) {
				wkDay = findWeekEndDay(prod, null);
			}
			SessionUtils.put(Constants.ATTR_TC_WEEK_END_DAY, wkDay);
		}
		return wkDay;
	}

	/**
	 * Determine the week-ending day specified in the preferences for the given
	 * Production, and possibly Project.
	 *
	 * @param prod The Production of interest.
	 * @param proj The Project of interest. This is only referenced for
	 *            Commercial productions. If it is null, then the session's
	 *            current Project setting is used.
	 * @return The week-ending day specified in the Production's or Project's
	 *         payroll preferences. (1=Sunday, 7=Saturday)
	 */
	public static Integer findWeekEndDay(StartForm sf) {
		Project proj = sf.getProject();
		if (proj == null) {
			return findWeekEndDay();
		}
		PayrollPreference pref = proj.getPayrollPref();
		return pref.getWeekEndDay();
	}

	/**
	 * Determine the week-ending day specified in the preferences for the given
	 * Production, and possibly Project.
	 *
	 * @param prod The Production of interest.
	 * @param proj The Project of interest. This is only referenced for
	 *            Commercial productions. If it is null, then the session's
	 *            current Project setting is used.
	 * @return The week-ending day specified in the Production's or Project's
	 *         payroll preferences. (1=Sunday, 7=Saturday)
	 */
	public static Integer findWeekEndDay(Production prod, Project proj) {
		Integer wkDay;
		PayrollPreference pref;
		if (prod == null) {
			return Calendar.SATURDAY; // default
		}
		if (prod.getType().isAicp()) {
			if (proj == null) {
				proj = SessionUtils.getCurrentProject();
				if (proj == null) { // possibly mobile
					proj = prod.getProjects().iterator().next();
				}
			}
			pref = proj.getPayrollPref();
		}
		else {
			pref = prod.getPayrollPref();
		}
		wkDay = pref.getWeekEndDay();
		return wkDay;
	}

	/**
	 * Get the appropriate PayrollPreference given a timecard and startForm.
	 * @param wtc
	 * @param sf
	 * @return
	 */
	private static PayrollPreference findPref(WeeklyTimecard wtc, StartForm sf) {
		PayrollPreference pref = null;
		if (sf.getProject() == null) { // TV/Feature
			Production prod = findProduction(wtc);
			pref = prod.getPayrollPref();
		}
		else { // Commercial
			pref = sf.getProject().getPayrollPref();
		}
		return pref;
	}

	private static Map<Integer, BigDecimal> YEAR_MILEAGE = new HashMap<>();
	static {
		// TODO load mileage rates from database
		YEAR_MILEAGE.put(2014, new BigDecimal("0.56"));
		YEAR_MILEAGE.put(2015, new BigDecimal("0.575"));
		YEAR_MILEAGE.put(2016, new BigDecimal("0.54"));
		YEAR_MILEAGE.put(2017, new BigDecimal("0.535"));
		YEAR_MILEAGE.put(2018, new BigDecimal("0.545"));
		YEAR_MILEAGE.put(2019, new BigDecimal("0.58"));
		YEAR_MILEAGE.put(2020, new BigDecimal("0.575")); // add 2020 rate
		YEAR_MILEAGE.put(0, new BigDecimal("0.575")); // default if we can't find requested entry
	}

	/**
	 * Find the standard IRS rate for mileage reimbursement for the given date.
	 *
	 * @param year The date of interest.
	 * @return The mileage rate for the given date.
	 */
	public static BigDecimal findMileageRate(Date year) {
		Calendar cal = CalendarUtils.getInstance(year);
		Integer yr = cal.get(Calendar.YEAR);
		BigDecimal rate = YEAR_MILEAGE.get(yr);
		if (rate == null) { // not found?
			rate = YEAR_MILEAGE.get(0); // use default (latest?)
		}
		return rate;
	}

	/**
	 * Update line items in the PayExpense table. The Box Rental entry will be
	 * updated by the updateBoxRentalExpense() method; other entries will be
	 * updated only if they do not have the "quantity" field filled in. They
	 * must also be for a category that matches a Start Form allowance/expense
	 * entry. The Start Form item's setting of Weekly vs Daily will be used to
	 * determine the quantity -- 1 for a weekly setting, or the number of days
	 * worked for a daily setting.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param useBox If true, the Box rental form amount will override any value
	 *            in the Start Form.
	 * @param submitting True iff the timecard is in the process of being
	 *            submitted. Only used for the Box Rental update. When this is
	 *            true, and pro-rating is necessary, the pro-rating calculation
	 *            uses the number of days indicated as having been worked on the
	 *            timecard. If this flag is false, and the timecard has not been
	 *            submitted, then the pro-rating will use either 5 days or the
	 *            number of days worked, which ever is larger, to calculate the
	 *            daily box rental rate.
	 */
	public static void updateExpenseItems(WeeklyTimecard wtc, boolean useBox, boolean submitting) {
		StartForm sf = wtc.getStartForm();
		sf = StartFormDAO.getInstance().refresh(sf);
		int days = TimecardCalc.calculateNumberOfWorkDays(wtc);
		if (days < 5 && (wtc.getStatus() == ApprovalStatus.OPEN) && (! submitting)) {
			days = 5;
		}
		updateBoxRentalExpense(wtc, sf, days, useBox, submitting);

		BigDecimal dDays = new BigDecimal(days);

		for (PayExpense pe : wtc.getExpenseLines()) {
			if (pe.getQuantity() == null) {
				PayCategory pc = pe.getCategoryType();
				if (pc != null && pc != PayCategory.CUSTOM) {
					boolean weekly = sf.getWeekly(pc);
					if (weekly) {
						pe.setQuantity(BigDecimal.ONE);
					}
					else {
						pe.setQuantity(dDays);
					}
					pe.calculateTotal();
				}
			}
		}
	}

	/**
	 * Update the Box Rental entry in the Expenses table to match the value in
	 * the Start Form. This also sets the "matches Start" flag in the Box rental
	 * form, if that form exists.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param useBox If true, the Box rental form amount will override any value
	 *            in the Start Form.
	 * @param submitting True iff the timecard is in the process of being
	 *            submitted. When this is true, and pro-rating is necessary, the
	 *            pro-rating calculation uses the number of days indicated as
	 *            having been worked on the timecard. If this flag is false, and
	 *            the timecard has not been submitted, then the pro-rating will
	 *            use either 5 days or the number of days worked, which ever is
	 *            larger, to calculate the daily box rental rate.
	 */
	public static void updateBoxRentalExpense(WeeklyTimecard wtc, boolean useBox, boolean submitting) {
		StartForm sf = wtc.getStartForm();
		sf = StartFormDAO.getInstance().refresh(sf);
		int days = TimecardCalc.calculateNumberOfWorkDays(wtc);
		if (days < 5 && (wtc.getStatus() == ApprovalStatus.OPEN) && (! submitting)) {
			days = 5;
		}
		updateBoxRentalExpense(wtc, sf, days, useBox, submitting);
	}


	/**
	 * Update the Box Rental entry in the Expenses table to match the value in
	 * the Start Form. This also sets the "matches Start" flag in the Box rental
	 * form, if that form exists.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param sf The StartForm for the timecard, refreshed.
	 * @param useBox If true, the Box rental form amount will override any value
	 *            in the Start Form.
	 * @param submitting True iff the timecard is in the process of being
	 *            submitted. When this is true, and pro-rating is necessary, the
	 *            pro-rating calculation uses the number of days indicated as
	 *            having been worked on the timecard. If this flag is false, and
	 *            the timecard has not been submitted, then the pro-rating will
	 *            use either 5 days or the number of days worked, which ever is
	 *            larger, to calculate the daily box rental rate.
	 * @param days The number of days to fill in as the quantity, if the box
	 *            amount is determined to be a daily (not weekly) value.
	 */
	private static void updateBoxRentalExpense(WeeklyTimecard wtc, StartForm sf, int days, boolean useBox, boolean submitting) {
		BigDecimal amount = null;
		BigDecimal dailyAmount = null;
		BigDecimal boxAmount = null;
		boolean weekly = true;

		if (wtc.getBoxRental() != null) {
			boxAmount = wtc.getBoxRental().getAmount();
		}
		if (useBox) {
			if (wtc.getBoxRental() != null) {
				amount = boxAmount;
			}
		}
		else {
			// see if box rental amounts are on Start Form
			if (wtc.isStudioRate()) {
				amount = sf.getBoxRental().getStudio();
			}
			else {
				amount = sf.getBoxRental().getLoc();
			}
			if (amount == null) {
				amount = boxAmount;
			}
			else {
				// using StartForm amount
				weekly = sf.getBoxRental().getWeekly();
				if (! weekly) {
					dailyAmount = amount;
					amount = amount.multiply(Constants.DECIMAL_FIVE);
				}
			}
		}

		updateBoxRentalMatch(wtc, amount);

		if (amount != null && amount.signum() != 0) {
			if (weekly) {
				updatePayExpense(wtc, PayCategory.BOX_RENT_NONTAX,
						BigDecimal.ONE, amount, sf.getBoxRental());
			}
			else {
				updatePayExpense(wtc, PayCategory.BOX_RENT_NONTAX,
						new BigDecimal(days), dailyAmount, sf.getBoxRental());
			}
		}
	}

	/**
	 * Update the Box Rental form's "matches start form" flag, and return the
	 * match setting.
	 *
	 * @param wtc The timecard being checked.
	 * @return True if there is NO box rental form, or if there is a box rental
	 *         form and the amount on it matches the amount on the associated
	 *         Start Form's Box rental field.
	 */
	public static boolean updateBoxRentalMatch(WeeklyTimecard wtc) {
		boolean match = true;
		if (wtc.getBoxRental() != null) {
			BigDecimal amount;
			StartForm sf = wtc.getStartForm();
			sf = StartFormDAO.getInstance().refresh(sf);

			// first see if box rental amounts are on Start Form
			if (wtc.isStudioRate()) {
				amount = sf.getBoxRental().getStudio();
			}
			else {
				amount = sf.getBoxRental().getLoc();
			}
			boolean weekly = sf.getBoxRental().getWeekly();
			if (! weekly && amount != null) {
				amount = amount.multiply(Constants.DECIMAL_FIVE);
			}

			match = updateBoxRentalMatch(wtc, amount);
		}
		return match;
	}

	/**
	 * Update the Box Rental form's "matches start form" flag, and return the
	 * match setting.
	 *
	 * @param wtc The timecard being checked.
	 * @param amount The box rental amount specified on the Start Form.
	 * @return True if there is NO box rental form, or if there is a box rental
	 *         form and the amount on it matches the amount on the associated
	 *         Start Form's Box rental field.
	 */
	private static boolean updateBoxRentalMatch(WeeklyTimecard wtc, BigDecimal amount) {
		boolean match = true; // returns true if no Box Rental form exists
		if (wtc.getBoxRental() != null) {
			match = false;
			if (amount == null || amount.signum() == 0) {
				if (wtc.getBoxRental().getAmount() == null ||
						wtc.getBoxRental().getAmount().signum() == 0) {
					match = true;
				}
			}
			else {
				// Start Form has box amount; compare to Box form amount
				if ((wtc.getBoxRental().getAmount() != null) &&
						(amount.compareTo(wtc.getBoxRental().getAmount()) == 0)) {
					match = true;
				}
			}
			wtc.getBoxRental().setMatchesStart(match);
		}
		return match;
	}


	/**
	 * Update the Mileage entries in the Expenses table to match the value in
	 * the Mileage form.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 */
	public static void updateMileageExpense(WeeklyTimecard wtc) {
		StartForm sf = wtc.getStartForm();
		sf = StartFormDAO.getInstance().refresh(sf);
		PayrollPreference pref = findPref(wtc, sf);
		if (wtc.getMileage().getNonTaxableMiles().signum() == 0) {
			deletePayExpense(wtc, PayCategory.MILEAGE_NONTAX);
		}
		else {
			updatePayExpense(wtc, PayCategory.MILEAGE_NONTAX,
 					wtc.getMileage().getNonTaxableMiles(), pref.getMileageRate(), sf.getCarAllow());
		}
		if (wtc.getMileage().getTaxableMiles().signum() == 0) {
			deletePayExpense(wtc, PayCategory.MILEAGE_TAX);
		}
		else {
			updatePayExpense(wtc, PayCategory.MILEAGE_TAX,
					wtc.getMileage().getTaxableMiles(), pref.getMileageRate(), sf.getCarAllow());
		}
	}

	/**
	 * Update the expenses table in a timecard to have a total of the given
	 * amount. If a PayExpense item is not found that matches the given
	 * PayCategory, a new PayExpense will be created.
	 *
	 * @param wtc The WeeklyTimecard of interest.
	 * @param category The PayCategory whose total is to be updated.
	 * @param total The new total to be put into the PayExpense.
	 * @param codes The account codes to be assigned if a new PayExpense is
	 *            created.
	 */
	public static void updatePayExpense(WeeklyTimecard wtc, PayCategory category,
			BigDecimal total, Allowance codes) {
		if (total != null && total.signum() != 0) {
			if (codes.getWeekly()) {
				updatePayExpense(wtc, category, BigDecimal.ONE, total, codes);
			}
			else {
				updatePayExpense(wtc, category, Constants.DECIMAL_FIVE, total, codes);
			}
		}
	}

	/**
	 * Update the expenses table in a timecard to have the specified quantity
	 * and rate, with the total calculated from those values. If a PayExpense
	 * item is not found that matches the given PayCategory, a new PayExpense
	 * will be created.
	 *
	 * @param wtc The WeeklyTimecard to be updated.
	 * @param category The PayCategory whose entry is to be updated.
	 * @param quantity The quantity to be put into the PayExpense.
	 * @param rate The rate to be put into the PayExpense.
	 * @param codes The account codes to be assigned if a new PayExpense is
	 *            created.
	 */
	public static void updatePayExpense(WeeklyTimecard wtc, PayCategory category,
			BigDecimal quantity, BigDecimal rate, AccountCodes codes) {
		PayExpense pe = findOrCreatePayExpense(wtc, category, codes);
		pe.setQuantity(quantity);
		if (rate != null) {
			pe.setRate(rate);
			pe.setTotal(NumberUtils.scaleTo2Places(NumberUtils.safeMultiply(quantity, rate)));
			if (pe.getTotal() == null) {
				pe.setTotal(BigDecimal.ZERO);
			}
		}
	}

	/**
	 * Delete a particular line item from a timecard's Expenses table if the
	 * matching category is found. If the line item is not found, no action is
	 * taken and no error condition is raised.
	 *
	 * @param wtc The timecard to be updated.
	 * @param category The PayCategory of the line item to be deleted.
	 */
	public static void deletePayExpense(WeeklyTimecard wtc, PayCategory category) {
		PayExpense pe;
		Iterator<PayExpense> iter = wtc.getExpenseLines().iterator();
		while ( iter.hasNext() ) {
			pe = iter.next();
			if (pe.getCategory() != null && pe.getCategory().equals(category.getLabel())) {
				iter.remove();
				break;
			}
		}
	}

	/**
	 * Find a PayExpense line item within a timecard that has the given
	 * category, or, if not found, create a new one, setting the provided
	 * account information, and add it to the timecard.
	 *
	 * @param wtc The WeeklyTimecard of interest.
	 * @param category The PayCategory to be searched for.
	 * @param codes The account codes to be assigned if a new PayExpense is
	 *            created.
	 * @return The matching PayExpense line item, which may be a new instance.
	 */
	private static PayExpense findOrCreatePayExpense(WeeklyTimecard wtc, PayCategory category,
			AccountCodes codes) {
		byte lineNumber = 0;
		for (PayExpense pay : wtc.getExpenseLines()) {
			if (pay.getCategory() == null) {
				break;
			}
			if (pay.getCategory().equals(category.getLabel())) {
				return pay;
			}
			lineNumber++;
		}
		PayExpense pe = new PayExpense(wtc, lineNumber);
		wtc.getExpenseLines().add(lineNumber, pe);
		pe.setCategory(category.getLabel());
		pe.setAccountCodes(codes);
		renumberExpenseLines(wtc);  // just to be safe!
		return pe;
	}

	/**
	 * Renumber the 'lineNumber' fields of the existing PayExpense entries.
	 *
	 * @param weeklyTimecard The timecard whose lines are to be renumbered.
	 */
	public static void renumberExpenseLines(WeeklyTimecard weeklyTimecard) {
		byte i = 0;
		for (PayExpense pb : weeklyTimecard.getExpenseLines()) {
			pb.setLineNumber(i++);
		}
	}

	/**
	 * @param prod The Production of interest.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param startDate The start date of the timecards to be deleted.
	 * @param endDate The end date of the timecards to be deleted.
	 * @param onlyCurrentProject If true, only the timecards for current Project will be deleted.
	 *            If false, timecards for all the projects will be deleted in a commercial Production.
	 * @return  A text message indicating the number of timecards deleted.
	 */
	public static String deleteTimecards(Production prod, Project project,
			Date startDate, Date endDate, boolean onlyCurrentProject) {

		try {
			/** The number of timecards deleted for the current production. */
			int timecardCount = 0;

			WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
			List<WeeklyTimecard> wtcs = new ArrayList<>();

			if (onlyCurrentProject) {
				wtcs = weeklyTimecardDAO.findTimecardsBetweenDates(prod, project, startDate, endDate);
			}
			else {
				wtcs = weeklyTimecardDAO.findTimecardsBetweenDates(prod, null, startDate, endDate);
			}

			log.debug("w/e Start date=" + startDate + ", w/e End date=" + endDate +
					", TCs=" + wtcs.size() + ", prod=" + prod.getTitle());

			for ( WeeklyTimecard wtc : wtcs ) {
				timecardCount++;
				weeklyTimecardDAO.remove(wtc);
			}

			String message = "";
			if (timecardCount > 0) {
				message = "" + timecardCount + " Timecards deleted";
			}
			else {
				message = "No Timecards deleted";
			}
			return message;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Check to see if this timecard is missing any payment information that would prevent if
	 * from being calculated.
	 *
	 * @param weeklyTimecard
	 * @return
	 */
	public static boolean checkForMissingPaymentInfo(WeeklyTimecard weeklyTimecard) {
		boolean missingInfo = false;

		if (weeklyTimecard.getPayLines() == null || weeklyTimecard.getPayLines().isEmpty()) {
			// No pay breakdown items.
			missingInfo = true;
		}
		else if ((weeklyTimecard.getGrandTotal() == null || weeklyTimecard.getGrandTotal().signum() < 1)
				&& (weeklyTimecard.getHourlyRate() == null || weeklyTimecard.getHourlyRate().signum() < 1)
				&& (weeklyTimecard.getDailyRate() == null || weeklyTimecard.getDailyRate().signum() < 1)
				&& (weeklyTimecard.getWeeklyRate() == null || weeklyTimecard.getWeeklyRate().signum() < 1)) {
			// If the grand total is 0 and the timecard rate is 0. Should not happen.
			missingInfo = true;
		}

		return missingInfo;
	}

	/**
	 * Create a timecard to be associated with an Uda Contract form.
	 * @param user
	 * @param prod
	 * @param endDate
	 * @param sd
	 * @return
	 */
	public static WeeklyTimecard createUdaTimecard(User user, Production prod,
			Date endDate, StartForm sd) {
		if (user == null || sd == null || prod == null) {
			return null;
		}
		if (endDate.getTime() == 0) {
			return null;
		}
		WeeklyTimecard wtc = null;

		try {
			wtc = new WeeklyTimecard(endDate);
			wtc.setUserAccount(user.getAccountNumber()); // associate with Lightspeed user account

			// Set the daily times to null
			// UDA Contract can contain up to $ daily time instances
			// Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			// calendar.setTime(endDate);
			//calendar.add(Calendar.DAY_OF_YEAR, -6);
			List<DailyTime> dailyTimes = new ArrayList<>();
			for (byte i = 1; i <= FormUDAContract.UDA_NUM_TIME_ENTRIES; i++) {
				DailyTime dt = new DailyTime(wtc, i);
				//dt.setDate(calendar.getTime());
				dailyTimes.add(dt);
				//calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			wtc.setDailyTimes(dailyTimes); // Add List<DailyTime> to weeklyTimecard

			// * Copy PRODUCTION fields
			wtc.setProdId(prod.getProdId());	// associate it with current production

			// * Copy START-DOC fields
			wtc.setStartForm(sd);
			updateTimecardFromStart(wtc, sd, prod);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			wtc = null;
		}
		return wtc;
	}

	/*
	 * LS-2643
	 * Show warning msg when there is no attachment for expenses
	 * it's check for expense category as below and return
	 * .1.Expenses (Box Rental- NonTax, Gas, Lodging- NonTax, Mileage- NonTax, Parking- NonTax, Reimbursement) are entered on the timecard screen.
		2. No attachment is uploaded and saved
		3. Timecard is saved If the above occur the follow message should appear upon saving the timecard
	 */
	public static boolean isExpenseWithoutAttach(WeeklyTimecard weeklyTimecard) {
		// LS-2857. When checking for mileage and box rental, see if a mileage or box rental form
		// exists. Check the total of the form against the payexpense. If the totals do not match
		// then put up error message.
		BoxRental br = weeklyTimecard.getBoxRental();
		Mileage mf = weeklyTimecard.getMileage();
		for (PayExpense payExpense : weeklyTimecard.getExpenseLines()) {
			if ((payExpense.getCategoryType() == PayCategory.BOX_RENT_NONTAX && (br == null || br.getAmount() == null || br.getAmount().compareTo(payExpense.getTotal()) != 0)) ||
					payExpense.getCategoryType() == PayCategory.GAS ||
					payExpense.getCategoryType() == PayCategory.LODGING_NONTAX ||
					(payExpense.getCategoryType() == PayCategory.MILEAGE_NONTAX&& (mf == null || mf.getNonTaxableMiles().compareTo(payExpense.getQuantity()) != 0))  ||
					payExpense.getCategoryType() == PayCategory.PARKING_NONTAX ||
					payExpense.getCategoryType() == PayCategory.REIMB) {
				return true;
			}
		}
		return false;
	}

}
