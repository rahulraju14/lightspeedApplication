/**
 * AddTimecardsBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This is the backing bean for the "Add" button on Timesheet page.
 * It includes the functions of adding new Timecards for Tours Production.
 */
@ManagedBean
@ViewScoped
public class AddTimecardsBean extends PopupBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(AddTimecardsBean.class);

	/** FIELDS */

	/** The timecard that will be the source for the cloning operation. */
	private WeeklyTimecard weeklyTimecard;

	/** The week-ending date of the source timecard. */
	private Date weekEndingDate;

	private Date startDate;

	/** Select all recipient's timecards checkbox value. */
	private boolean selectAllTargets;

	/** The list of (possible) recipient timecards presented to the user,
	 * from which they select the timecards that will be cloning targets. */
	private List<TimecardEntry> recipients;

	/** The list of (possible) recipient startforms presented to the user,
	 * from which they select the startforms for which timecards will be created. */
	private List<StartForm> startFormList;

	/** True if the list of Startforms is created successfully. */
	private boolean sfListReturn = true;

	WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();

	/**
	 * default constructor.
	 */
	public AddTimecardsBean() {
		log.debug("");
	}

	public static AddTimecardsBean getInstance() {
		return (AddTimecardsBean)ServiceFinder.findBean("addTimecardsBean");
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#show(com.lightspeedeps.web.popup.PopupHolder, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void show(PopupHolder holder, int act, String titleId, String buttonOkId, String buttonCancelId) {
		log.debug("");
		super.show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/**
	 * Creates the list of timecards which may be targets of the clone
	 * operation.
	 *
	 * @return A (possibly empty) List of TimecardEntry`s, where each one
	 *         represents a WeeklyTimecard that the current user has either
	 *         approval authority or "timeKeeper" authority for.  The list
	 *         does NOT include the "current" (clone source) timecard.
	 */
	/*private List<TimecardEntry> createRecipientList() {

		if (weeklyTimecard == null) {
			return new ArrayList<TimecardEntry>();
		}

		Contact contact = SessionUtils.getCurrentContact();
		String acct = null; // no filtering by recipients' account number
		// Ignore edit/view HTG permission when creating timecard list
		// ...except if pseudo-approver flag is on.
		boolean viewAll = AuthorizationBean.getInstance().getPseudoApprover();
		recipients = TimecardUtils.createTimecardListAllDepts(production, project, weekEndingDate,
				true, null, null, acct, contact, viewAll, true);

		// Remove submitted timecards and the clone source timecard from the list
		Integer id = weeklyTimecard.getId();
		for (Iterator<TimecardEntry> iter = recipients.iterator(); iter.hasNext(); ) {
			TimecardEntry tce = iter.next();
			if (tce.getWeeklyTc().getId().equals(id)) {
				iter.remove();
			}
			else if (! tce.getWeeklyTc().getSubmitable()) {
				// TC submitted -- remove from cloning target list
				iter.remove();
			}
		}
		Collections.sort(recipients, TimecardEntry.getNameComparator());

		return 	recipients;
	}*/

	/**
	 * Creates the list of Start Forms for which a timecard may be created.
	 *
	 * @return A (possibly empty) List of StartForm`s.
	 */
	public List<StartForm> createRecipientList() {
		log.debug("");
		if (weekEndingDate == null) {
			return new ArrayList<>();
		}
		startFormList = new ArrayList<>();
		boolean isApprover = false;
		Collection<Department> depts = null;
		Contact currContact = SessionUtils.getCurrentContact();
		isApprover = ApproverUtils.isProdApprover(currContact);
		if (! isApprover) {
			depts = ApproverUtils.createDepartmentsApproved(currContact, null);
			// Then count all unapproved timecards for those departments
		}

		if (! SessionUtils.getCurrentOrViewedProduction().getType().equals(ProductionType.TOURS)) {
			startDate = TimecardUtils.calculatePriorWeekEndDate(weekEndingDate);
		}
		startFormList = StartFormDAO.getInstance().findByActiveDate(SessionUtils.getCurrentOrViewedProduction(), startDate, weekEndingDate);
		if (startFormList == null || startFormList.isEmpty()) {
			sfListReturn = false;
			log.debug("");
			MsgUtils.addFacesMessage("Timesheet.AddEmpty", FacesMessage.SEVERITY_INFO);
			return startFormList;
		}
		List<StartForm> tsheetSfList = new ArrayList<>();
		for (WeeklyTimecard wtc : TimesheetBean.getInstance().getTimecardList()) {
			tsheetSfList.add(wtc.getStartForm());
		}
		Iterator<StartForm> itr = startFormList.iterator();
		while (itr.hasNext()) {
			StartForm sf = itr.next();
			if (tsheetSfList.contains(sf) || ((! isApprover) && depts != null && (! depts.contains(sf.getEmployment().getDepartment())))) {
				log.debug("");
				itr.remove();
			}
			//TODO Re. issue #556, DH: On which condition do we need to remove the multiple startforms for a single employment.
		}
		if (startFormList == null || startFormList.isEmpty()) {
			log.debug("");
			sfListReturn = false;
			if (tsheetSfList.isEmpty()) {
				log.debug("");
				MsgUtils.addFacesMessage("Timesheet.AddEmpty", FacesMessage.SEVERITY_INFO);
				return startFormList;
			}
			else {
				log.debug("");
				MsgUtils.addFacesMessage("Timesheet.AddNone", FacesMessage.SEVERITY_INFO);
				return startFormList;
			}
		}
		if (startFormList != null && ! startFormList.isEmpty()) {
			Collections.sort(startFormList, StartForm.getNameComparator());
		}
		return startFormList;
	}

	/**
	 * ValueChangeListener method for the "Select All Recipients" checkbox on the
	 * Clone Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectAllTargets(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			for (StartForm sf : startFormList) {
				sf.setChecked(checked);
			}
		}
	}

	/**
	 * ValueChangeListener method for all of the recipient selection checkboxes
	 * on the Clone Timecard page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectTarget(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			if (! checked) {
				setSelectAllTargets(false);
			}
		}
	}

	/** See {@link #weekEndingDate}. */
	public Date getWeekEndingDate() {
		return weekEndingDate;
	}
	/** See {@link #weekEndingDate}. */
	public void setWeekEndingDate(Date weekEndingDate) {
		this.weekEndingDate = weekEndingDate;
	}

	/** See {@link #startDate}. */
	public Date getStartDate() {
		return startDate;
	}

	/** See {@link #startDate}. */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/**See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #recipients}. Note that this method provides
	 * for lazy initialization of the list. */
	public List<TimecardEntry> getRecipients() {
		if (recipients == null) {
			//recipients = createRecipientList();
		}
		return recipients;
	}
	/**See {@link #recipients}. */
	public void setRecipients(List<TimecardEntry> recipients) {
		this.recipients = recipients;
	}

	/**See {@link #startFormList}. */
	public List<StartForm> getStartFormList() {
		if (isVisible() && (startFormList == null || startFormList.isEmpty())) {
			startFormList = createRecipientList();
		}
		return startFormList;
	}
	/**See {@link #startFormList}. */
	public void setStartFormList(List<StartForm> startFormList) {
		this.startFormList = startFormList;
	}


	/**See {@link #selectAllTargets}. */
	public boolean getSelectAllTargets() {
		return selectAllTargets;
	}
	/**See {@link #selectAllTargets}. */
	public void setSelectAllTargets(boolean selectAllTargets) {
		this.selectAllTargets = selectAllTargets;
	}

	/**See {@link #sfListReturn}. */
	public boolean getSfListReturn() {
		return sfListReturn;
	}
	/**See {@link #sfListReturn}. */
	public void setSfListReturn(boolean sfListReturn) {
		this.sfListReturn = sfListReturn;
	}

}
