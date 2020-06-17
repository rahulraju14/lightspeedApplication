package com.lightspeedeps.web.timecard;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.view.facelets.component.UIRepeat;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.object.TimesheetEntry;
import com.lightspeedeps.service.TimecardService;
import com.lightspeedeps.service.WeeklyBatchService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.util.payroll.WeeklyBatchUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.payroll.EmailBatchPopupBean;
import com.lightspeedeps.web.popup.PinPromptBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.user.ChangePinBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.view.View;

@ManagedBean
@ViewScoped
public class TimesheetBean extends View implements Serializable {

	private static final long serialVersionUID = 7253267725555619545L;

	private static final Log LOG = LogFactory.getLog(TimesheetBean.class);

	/** Regular expression that matches the component ID for the Day Type drop-down list
	 *  entries; the embedded numeric field is the occurrence number.  Xhtml specifies
	 *  id='dayType', icefaces adds qualifiers. */
	private static final String DAY_TYPE_ID_REGEX = ".*:(\\d+):dayType.*";

	/**
	 * Regular expression that matches the component ID for the Country
	 * drop-down list entries; the embedded numeric field is the occurrence
	 * number. Xhtml specifies id='country', icefaces adds qualifiers.
	 */
	private static final String COUNTRY_ID_REGEX = ".*:(\\d+):country.*";

	/** Pattern for the {@link #DAY_TYPE_ID_REGEX} regular expression. */
	private static final Pattern DAY_TYPE_ID_PATTERN = Pattern.compile(DAY_TYPE_ID_REGEX);

	/** Pattern for the {@link #COUNTRY_ID_REGEX} regular expression. */
	private static final Pattern COUNTRY_ID_PATTERN = Pattern.compile(COUNTRY_ID_REGEX);

	/** List contains timesheet entries for the timecards corresponding to the selected week ending date. */
	private List<TimesheetEntry> timesheetEntryList;

	/** List contains timecards corresponding to the selected week ending date. */
	private List<WeeklyTimecard> timecardList;

	/** Week ending date seleted from the drop down, Payroll period end date. */
	private Date weekEndDate;

	/** Instance of timesheet currently selected. */
	private Timesheet timesheet;

	/** Instance of current Production. */
	private Production production;

	private final Calendar cal;

	/** Select item list of week ending dates. */
	private List<SelectItem> endDateList;

	/** Select item list of day types for Tours producton. */
	private List<SelectItem> dayTypeList = null;

	private List<String> shortDayNames = new ArrayList<>();

	/** array list for day names for monthly timesheet */
	private List<String> monShortDayNames = new ArrayList<>();

	private List<String> longDayNames = new ArrayList<>();

	/** Select item list for workplace work zone */
	private List<SelectItem> workplaceDL = null;

	/** The department selected in deptList. **/
	private Integer deptId = 0;

	/** Select item list of Departments. */
	private List<SelectItem> deptList;

	/** Workplace to let us know which employees we are editing, either touring or home */
	private WorkZone workplace = WorkZone.DL;

	/** Stores the total of all values in Total Wages column. */
	private BigDecimal totalWages;

	/** Stores the total of all values in PayCategory1 column. */
	private BigDecimal totalPayCategory1;

	/** Stores the total of all values in PayCategory2 column. */
	private BigDecimal totalPayCategory2;

	/** Stores the total of all values in PayCategory3 column. */
	private BigDecimal totalPayCategory3;

	/** Stores the total of all values in PayCategory4 column. */
	private BigDecimal totalPayCategory4;

	/** New Comment field to be appended to existing comments */
	private String newComment;

	private static final int ACT_SUBMIT = 11;

	private static final int ACT_ADD_TC = 12;

	private static final int ACT_UPDATE_FROM_STARTS = 13;

	private static final int ACT_RECALL = 14;

	private static final int ACT_CREATE_TS = 15;

	private static final int ACT_CREATE_NEXT_TS = 16;

	/** True if the "change PIN" diaLOG should be displayed. */
	private boolean showChangePin;

	/** True if showing all history. False if only signatures */
	private boolean showAllHistory;

	/** Instance of WeeklyTimecard to delete. */
	private WeeklyTimecard weeklyTcToDelete;

	/** Check for pay period onChange Event */
	private boolean isWeekEndChanged = false;

	private transient WeeklyTimecardDAO weeklyTimecardDAO;

	private transient TimesheetDAO timesheetDAO;

	private transient StartFormDAO startFormDAO;

	private transient AddTimecardsBean addTimecardsBean;

	private transient CreateTimesheetBean createTimesheetBean;

	private transient PayExpenseDAO payExpenseDAO;

	/** Collection used to hold the departments for which
	 * the current contact is an Approver. */
	Collection<Department> depts = null;

	/** True, if the current contact is a Production Approver. */
	private Boolean isProdApprover;

	/** True, if the current contact is a Department Approver. */
	private boolean isDeptApprover;

	/** Select item list of Reimbursements. */
	private List<SelectItem> payCategoryList;

	/** Whether to expand or collapse the table for this Timesheet first section*/
	private boolean expandTS1;

	/** Whether to expand or collapse the table for this Timesheet Second section*/
	private boolean expandTS2;

	/** used for display or hide the timesheet on UI */
	private boolean newTimeSheet = true;

	/** See {@link #newTimeSheet}. */
	public boolean getNewTimeSheet() {
		return newTimeSheet;
	}

	/** See {@link #newTimeSheet}. */
	public void setNewTimeSheet(boolean newTimeSheet) {
		this.newTimeSheet = newTimeSheet;
	}

	/** state values for tour's state drop down list, which excludes 'FO',
	 * but includes "HM" and "OT". - LS-2219 */
	private static final List<SelectItem> TOURS_STATE_LIST;
	static {
		TOURS_STATE_LIST = new ArrayList<>(ApplicationScopeBean.getInstance().getStateCodeProdDL());
		Iterator<SelectItem> iter = TOURS_STATE_LIST.iterator();
		for (; iter.hasNext();) {
			SelectItem item = iter.next();
			if (item.getLabel().equals(Constants.FOREIGN_FO_STATE)) {
				iter.remove();
			}
		}
		TOURS_STATE_LIST.add(Constants.TOURS_HOME_STATE_ITEM); // "HM"; LS-4129
		TOURS_STATE_LIST.add(Constants.FOREIGN_OT_STATE_ITEM);
	}


	public TimesheetBean() {
		super("Timesheet.");
		LOG.debug("TIMESHEET BEAN CONSTRUCTOR");
		cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		production = SessionUtils.getCurrentOrViewedProduction();
		if (production != null) {
			// to fetch the available timesheets
			List<Timesheet> timesheets = TimesheetDAO.getInstance()
					.getTimesheetsByProduction(production, "endDate asc");
			if (timesheets.size() > 0) {
				setNewTimeSheet(false);
				Timesheet tsheet = timesheets.get(timesheets.size() - 1);
				weekEndDate = tsheet.getEndDate();
				cal.setTime(weekEndDate);
				setUpTimesheetData();
				getTimesheetEntryList();
				getPayCategoryList();
				calculateTotals();
				showAllHistory = true;
				expandTS1 = true;
				expandTS2 = true;
			}
		}
	}

	/** Used to return the instance of the TimesheetBean */
	public static TimesheetBean getInstance() {
		LOG.debug("GET TIMESHEET BEAN");
		return (TimesheetBean)ServiceFinder.findBean("timesheetBean");
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.view.View#actionEdit()
	 */
	@Override
	public String actionEdit() {
		LOG.debug("ACTION EDIT TIMESHEET BEAN");
		// Recalling Timesheet from final approved state
		if (timesheet.getStatus() == ApprovalStatus.APPROVED) {
			PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_RECALL, "Timesheet.Recall.");
				bean.setMessage(MsgUtils.formatMessage("Timesheet.Recall.Text", getTimecardList().size()));
				return null;
		}
		else {
			return super.actionEdit();
		}
	}

	/**
	 * Action method for the OK button on the "Recall" confirmation diaLOG box
	 * when the timesheet is being recalled.
	 * Notifications will normally be issued during that step. After adjusting
	 * the timesheet, enter Edit mode.
	 *
	 * @return null navigation string
	 */
	private String actionRecallOk() {
		try {
			timesheet = getTimesheetDAO().refresh(timesheet);
			// Update timesheet
			timesheet.setStatus(ApprovalStatus.RECALLED);
			// update each timecard
			getTimecardList(); // ensure list exists
			for (int i=0; i < timecardList.size(); i++) {
				WeeklyTimecard wtc = timecardList.get(i);
				wtc = getWeeklyTimecardDAO().refresh(wtc);
				TimecardEventDAO.getInstance().createEvent(wtc, TimedEventType.RECALL, (short) 0, null);
				wtc.setStatus(ApprovalStatus.RECALLED);
				getWeeklyTimecardDAO().attachDirty(wtc);
				timecardList.set(i, wtc); // update list with refreshed copy
			}
			createTimesheetEntryList(); // update entry list with new timecard references
			TimesheetEventDAO.getInstance().createEvent(timesheet, getTimecardList().size(), TimedEventType.RECALL);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return super.actionEdit();
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.view.View#actionSave()
	 */
	@Override
	public String actionSave() {
		addButtonClicked();
		LOG.debug("ACTION SAVE TIMESHEET");
		try {
			boolean passed = true;
			// LS-2166 Timesheet State/City Validations
			passed = validateDayCityState();
			if (! passed) {
				return null;
			}
			boolean isCustom1 = timesheet.getPayCategory1().equals(PayCategory.CUSTOM);
			boolean isCustom2 = timesheet.getPayCategory2().equals(PayCategory.CUSTOM);
			boolean isCustom3 = timesheet.getPayCategory3().equals(PayCategory.CUSTOM);
			boolean isCustom4 = timesheet.getPayCategory4().equals(PayCategory.CUSTOM);

			//Validation for pay categories
			for (TimesheetEntry entry : getTimesheetEntryList()) {
				if (isCustom1) {
					if (entry.getPayCategory1Amount() != null && entry.getPayCategory1Amount().compareTo(BigDecimal.ZERO) != 0) {
						passed = false;
						break;
					}
				}
				if (isCustom2) {
					if (entry.getPayCategory2Amount() != null && entry.getPayCategory2Amount().compareTo(BigDecimal.ZERO) != 0) {
						passed = false;
						break;
					}
				}
				if (isCustom3) {
					if (entry.getPayCategory3Amount() != null && entry.getPayCategory3Amount().compareTo(BigDecimal.ZERO) != 0) {
						passed = false;
						break;
					}
				}
				if (isCustom4) {
					if (entry.getPayCategory4Amount() != null && entry.getPayCategory4Amount().compareTo(BigDecimal.ZERO) != 0) {
						passed = false;
						break;
					}
				}

			}

			if (! passed) {
				//message : 'Pay Category can not be blank' - if data is present in the time sheet column.
				MsgUtils.addFacesMessage("Timesheet.Error.EnteredValuesNoPayCategory",
						FacesMessage.SEVERITY_ERROR);
				return null;
			}

			Date date = new Date();
			boolean invalidAmounts = false;
			for (TimesheetEntry entry : getTimesheetEntryList()) {
				invalidAmounts = validateEntry(entry);
				if (invalidAmounts) {
					break;
				}
			}
			if (invalidAmounts) {
				MsgUtils.addFacesMessage("Timesheet.ValidateAmounts", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			else {
				LOG.debug("");
				for (TimesheetEntry entry : getTimesheetEntryList()) {
					saveEntry(entry, date);
				}
			}
			if (timesheet.getStatus() == null) {
				timesheet.setStatus(ApprovalStatus.OPEN);
			}
			getTimesheetDAO().attachDirty(timesheet);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return super.actionSave();
	}

	/** Validates the Daily Amounts and Pay Expense amounts
	 * @param entry TimesheetEntry
	 * @return
	 */
	private boolean validateEntry(TimesheetEntry entry) {
		LOG.debug("");
		BigDecimal maxValue = new BigDecimal(100000);
		for (BigDecimal amount : entry.getWeeklyTc().getDailyAmounts()) {
			if (amount != null && (amount.compareTo(maxValue) >= 0)) {
				return true;
			}
		}
		if (entry.getPayCategory1Amount() != null &&
				(entry.getPayCategory1Amount().compareTo(maxValue) >= 0)) {
			return true;
		}
		if (entry.getPayCategory2Amount() != null &&
				(entry.getPayCategory2Amount().compareTo(maxValue) >= 0)) {
			return true;
		}
		if (entry.getPayCategory3Amount() != null &&
				(entry.getPayCategory3Amount().compareTo(maxValue) >= 0)) {
			return true;
		}
		if (entry.getPayCategory4Amount() != null &&
				(entry.getPayCategory4Amount().compareTo(maxValue) >= 0)) {
			return true;
		}
		return false;
	}

	/**
	 * Save (persist) the timecard for the given entry.
	 *
	 * @param entry The timesheet entry containing the timecard to be saved.
	 * @param date The timestamp to put in the 'last updated' field of the
	 *            timecard.
	 */
	private void saveEntry(TimesheetEntry entry, Date date) {
		WeeklyTimecard wtc = entry.getWeeklyTc();
		StartForm sf = wtc.getStartForm();

		sf = getStartFormDAO().refresh(sf);

		WorkZone workZone = sf.getWorkZone();
		String city = null;
		String state = null;
		boolean foreign = false; // Any non-US countries?
		int i = 0;
		for (BigDecimal amt : wtc.getDailyAmounts()) {
			TimesheetDay tDay = timesheet.getTimesheetDays().get(i);
			DayType dayType;
			DailyTime dailyTime = wtc.getDailyTimes().get(i);

			if (workZone.isTouring()) {
				// Touring
				dayType = tDay.getTouringDayType();
				dailyTime.setCity(tDay.getTouringCity());
				dailyTime.setState(tDay.getTouringState());
				// LS-2172 Save the country code
				dailyTime.setCountry(tDay.getTouringCountryCode());
			}
			else {
				// Home
				dayType = tDay.getHomeDayType();
				dailyTime.setCity(tDay.getHomeCity());
				dailyTime.setState(tDay.getHomeState());
				// LS-2172 Save the country code
				dailyTime.setCountry(tDay.getHomeCountryCode());
			}

			if (city == null && ! StringUtils.isEmpty(dailyTime.getCity())) {
				city = dailyTime.getCity();
			}
			if (state == null && ! StringUtils.isEmpty(dailyTime.getState())) {
				state = dailyTime.getState();
			}
			if (! foreign && dailyTime.getCountry() != null &&
					! dailyTime.getCountry().equals("US")) {
				foreign = true;
			}

			dailyTime.setPayAmount(amt);
			dailyTime.setDayType(dayType);
			if (dayType != null && amt != null && amt.signum() != 0) {
				dailyTime.setWorked(true);
			}
			else {
				dailyTime.setWorked(false);
			}
			DailyTimeDAO.getInstance().attachDirty(dailyTime);
			i++;
		}

		// Set TC's work-city and work-state if not yet set
		if (city != null && StringUtils.isEmpty(wtc.getCityWorked())) {
			wtc.setCityWorked(city);
		}

		if (StringUtils.isEmpty(wtc.getStateWorked())) {
			if (state != null) {
				wtc.setStateWorked(state);
			}
			else if (foreign) {
				wtc.setStateWorked(Constants.FOREIGN_FO_STATE);
			}
		}
		if (StringUtils.isEmpty(wtc.getSocialSecurity())) {
			// LS-2230 update timecard with SSN from Start
			wtc.setSocialSecurity(sf.getSocialSecurity());
		}
		wtc.setUpdated(date);

		// Delete the current pay expense items and replace then with the
		// current selections. LS-
		wtc.getExpenseLines().clear();
		// Update timesheet Pay Category amount only when it is not set to Custom.
		if (! timesheet.getPayCategory1().equals(PayCategory.CUSTOM)) {
			updateExpense(wtc, timesheet.getPayCategory1(), entry.getPayCategory1Amount());
		}
		if (! timesheet.getPayCategory2().equals(PayCategory.CUSTOM)) {
			updateExpense(wtc, timesheet.getPayCategory2(), entry.getPayCategory2Amount());
		}
		if (! timesheet.getPayCategory3().equals(PayCategory.CUSTOM)) {
			updateExpense(wtc, timesheet.getPayCategory3(), entry.getPayCategory3Amount());
		}
		if (! timesheet.getPayCategory4().equals(PayCategory.CUSTOM)) {
			updateExpense(wtc, timesheet.getPayCategory4(), entry.getPayCategory4Amount());
		}

		getWeeklyTimecardDAO().attachDirty(wtc);
		entry.setWeeklyTc(wtc);



		//getWeeklyTimecardDAO().attachDirty(wtc);
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.view.View#actionCancel()
	 */
	@Override
	public String actionCancel() {
		LOG.debug("ACTION CANCEL TIMESHEET BEAN");

		// LS-2926 Refresh the time sheet days
		timesheet = getTimesheetDAO().refresh(timesheet);
		// LS-2930 Call refresh weekly timecards and TimesheetEntry pay category amounts.
		createTimesheetEntryList();

		calculateDailyTotals();
		return super.actionCancel();
	}

	/**
	 * Action method for the Add Comment button
	 * which adds a comment to the comment field.
	 * @return null navigation string
	 */
	public String actionAddComment() {
		try {
			if (newComment != null && newComment.trim().length() > 0) {
				Date date = new Date();
				DateFormat sdf = new SimpleDateFormat(", M/d/yy H:mm: ");
				String comment = timesheet.getComments();
				if (comment == null) {
					comment = "";
				}
				comment += "<b>" + getvUser().getFirstNameLastName()
						+ sdf.format(date) + "</b>" + newComment + "<br/>";
				timesheet.setComments(comment);
				newComment = "";
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
		addButtonClicked();
		return null;
	}

	/** Creates list of Timecards for the currently selected Week-Ending date.
	 * @return list of Timecards
	 */
	private List<WeeklyTimecard> createTimecardList() {
		LOG.debug(" createTimecardList ");
		timecardList = getWeeklyTimecardDAO().findByWeekEndDate(getProduction(), null, weekEndDate, null);
		LOG.debug("Timecard List size: " + timecardList.isEmpty());
		if (! timecardList.isEmpty()) {
			Iterator<WeeklyTimecard> itr = timecardList.iterator();
			while (itr.hasNext()) {
				WeeklyTimecard wtc = itr.next();
				if (wtc.getStatus() == ApprovalStatus.VOID) {
					LOG.debug("Removing VOID Timecard = " + wtc.getId());
					itr.remove();
				}
			}
		}
		return timecardList;
	}

	/** Creates list of TimesheetEntry for the currently selected Week-Ending date.
	 * @return TimesheetEntry
	 */
	private List<TimesheetEntry> createTimesheetEntryList() {
		timesheetEntryList = new ArrayList<>();
		try {
			TimesheetEntry entry;
			LOG.debug("createTimesheetEntryList");
			for (WeeklyTimecard wtc : getTimecardList()) {
				wtc = getWeeklyTimecardDAO().refresh(wtc);
				entry = new TimesheetEntry(wtc);
				if (wtc.getExpenseLines() == null || wtc.getExpenseLines().isEmpty()) { // For testing
					LOG.debug("");
//					TimecardUtils.createAllowances(wtc, wtc.getStartForm());
//					getWeeklyTimecardDAO().attachDirty(wtc);
				}
				if (wtc.getExpenseLines() != null) {
					LOG.debug("");
					for (PayExpense exp : wtc.getExpenseLines()) {
						PayCategory cat = PayCategory.toValue(exp.getCategory());

						if (cat.equals(timesheet.getPayCategory1())) {
							entry.setPayCategory1Amount(exp.getTotal());
						}
						else if (cat.equals(timesheet.getPayCategory2())) {
							entry.setPayCategory2Amount(exp.getTotal());
						}
						else if (cat.equals(timesheet.getPayCategory3())) {
							entry.setPayCategory3Amount(exp.getTotal());
						}
						else if (cat.equals(timesheet.getPayCategory4())) {
							entry.setPayCategory4Amount(exp.getTotal());
						}
					}
				}
				timesheetEntryList.add(entry);
			}
			Collections.sort(timesheetEntryList, TimesheetEntry.getNameComparator());
			//if timesheetEntryList size is zero then making endDateList drop-down empty LS-2435
			if (timesheetEntryList.isEmpty() && ! getIsWeekEndChanged()) {
				endDateList = new ArrayList<>();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return timesheetEntryList;
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 *
	 * @param event contains old and new values
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		try {
			if (getProduction() != null) {
				LOG.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
				if (event.getNewValue() != null) {
					weekEndDate = (Date) event.getNewValue();
					timecardList = null;
					timesheetEntryList = null;
					setIsWeekEndChanged(true);
					cal.setTime(weekEndDate);
					setUpTimesheetData();
					calculateTotals();
					getAddTimecardsBean().setStartFormList(null);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 * @param event contains old and new values
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		try {
			LOG.debug("new val = " + event.getNewValue() + ", ID ="
					+ event.getComponent().getId());
			if (event.getNewValue() != null) {
				createTimesheetEntryList();
				deptId = Integer.parseInt(event.getNewValue().toString());
				Iterator<TimesheetEntry> itr = timesheetEntryList.iterator();
				LOG.info("***List size : " + (timesheetEntryList != null ? timesheetEntryList.size() : 0));
				if (deptId > 0) {
					while (itr.hasNext()) {
						TimesheetEntry timesheetEntry = itr.next();
						if (timesheetEntry.getWeeklyTc() != null &&
								timesheetEntry.getWeeklyTc().getDepartment() != null &&
								(! timesheetEntry.getWeeklyTc().getDepartment().getId().equals(deptId))) {
							itr.remove();
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Find the right PayExpense line item and update it.
	 * @param payCat Category to match
	 * @param newValue New amount to store.
	 */
	private void updateExpense(WeeklyTimecard wtc, PayCategory payCat, BigDecimal newValue) {
		if (! payCat.equals(PayCategory.CUSTOM)) {
			boolean found = false;
			for (PayExpense exp : wtc.getExpenseLines()) {
				// We only want to do this if we are not in edit mode
				PayCategory cat = PayCategory.toValue(exp.getCategory());
				if (cat.equals(payCat)) {
					found = true;
					if (newValue == null && exp.getRate() == null) {
						break; // no change
					}
					if (newValue == null || exp.getRate() == null ||
							newValue.compareTo(exp.getRate()) != 0) {
						// value changed, update expense record
						exp.setRate(newValue);
						exp.setQuantity(BigDecimal.ONE);
						exp.calculateTotal();
						getPayExpenseDAO().attachDirty(exp);
					}
					break;
				}
			}
			if (! found) {
				byte lineNum = 1;
				if (wtc.getExpenseLines() != null && ! wtc.getExpenseLines().isEmpty()) {
				lineNum = wtc.getExpenseLines().get(wtc.getExpenseLines().size()-1).getLineNumber();
					lineNum++;
				}
				LOG.debug("NOT FOUND");
				PayExpense payExpense = new PayExpense(wtc, lineNum);
				payExpense.setRate(newValue);
				payExpense.setQuantity(BigDecimal.ONE);
				payExpense.setCategory(payCat.getLabel());
				payExpense.calculateTotal();
				getPayExpenseDAO().save(payExpense);
			LOG.debug("New pay expense for cat: " + payCat.getLabel() + ", " + payExpense.getId());
				wtc.getExpenseLines().add(payExpense);
			}
		}
	}

	/** Sets the Timesheet data in Location / Day type table for the currently selected
	 * Week-Ending date.
	 */
	private void setUpTimesheetData() {
		LOG.debug("");
		if (weekEndDate != null) {
			Map<String, Object> values = new HashMap<>();
			values.put("prodId", getProduction().getProdId());
			values.put("endDate", weekEndDate);
			timesheet = getTimesheetDAO().findOneByNamedQuery(Timesheet.GET_TIMESHEET_BY_PROD_ID_END_DATE, values);
			if (timesheet != null) {
				monShortDayNames = new ArrayList<>();
				shortDayNames = new ArrayList<>();
				longDayNames = new ArrayList<>();
				cal.setTime(timesheet.getStartDate());
				int numDays = timesheet.getNumDays();

				//Get the timesheet days for the corresponding dates of the week or month
				for (int i = 1; i <= numDays; i++) {
					if (timesheet.getPayPeriodType().isMonthly()) {
						monShortDayNames.add(cal.getDisplayName(Calendar.DAY_OF_WEEK,
								Calendar.SHORT, Constants.LOCALE_US));
					}
					else {
						shortDayNames.add(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,
								Constants.LOCALE_US));
					}
					// LS-2788 Getting Unexpected Error while Submitting TOURS timesheet in Alpha
					longDayNames.add(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG,
							Constants.LOCALE_US));
					// LS-2754 fixed One day gap between timesheets
					cal.add(Calendar.DAY_OF_WEEK, 1);
				}

				cal.setTime(weekEndDate);
				// Get the timesheet dates from the start date to end date
				for (int i = numDays - 1; i >= 0; i--) {
					TimesheetDay day = timesheet.getTimesheetDays().get(i);
					day = TimesheetDayDAO.getInstance().refresh(day);
					if (cal != null) {
						if (i != numDays - 1) {
							cal.add(Calendar.DAY_OF_MONTH, - 1); // backup one day
						}
						day.setDate(cal.getTime());
					}
				}
			}
		}
	}

	/**
	 * Value change listener for day type change event. May update default
	 * state/country values. Will copy pay amounts from StartForm to timesheet
	 * for all employees in sheet with matching touring rates.
	 *
	 * @param evt Event from framework.
	 */
	public void listenChangeDayType (ValueChangeEvent evt) {
		try {
			LOG.debug("");
			if (editMode && (evt.getNewValue() != null)) {
				DayType dayType = (DayType) evt.getNewValue();
				LOG.debug("DayType = " + dayType);
				// To get the row Index
				Matcher m = DAY_TYPE_ID_PATTERN.matcher(evt.getComponent().getClientId());
				int dayId = 0;
				if (m.matches()) {
					dayId = Integer.parseInt(m.group(1)); // the occurrence number generated by icefaces
				}
				LOG.debug("Day = " + dayId);
				//LS-1981 Time sheet - State field defaults to 'HM'
				setToursDefaultState(dayType, dayId);
				TimesheetDay timesheetDay = timesheet.getTimesheetDays().get(dayId);
				String countryCode = timesheetDay.getTouringCountryCode();
				//LS-2166 Timesheet State/City Validations
				setDefaultCityState(timesheetDay, countryCode, dayType, evt, dayId);
				LOG.debug("TimesheetDay = " + timesheetDay.getId());
				for (TimesheetEntry entry : getTimesheetEntryList()) {
					WeeklyTimecard wtc = entry.getWeeklyTc();
					StartForm sf = wtc.getStartForm();
					sf = getStartFormDAO().refresh(sf);

					// If the crew member start form work zone is not equal to the
					//  currently selected work zone, them skip this crew member.
					if (sf.getWorkZone() != workplace) {
						continue;
					}
					LOG.debug("StartForm = " + sf.getId());
					int index = - 1;
					BigDecimal payAmt = null;
					for (DailyTime dt : wtc.getDailyTimes()) {
						LOG.debug("DailyTime = " + dt.getId());
						index = index + 1;
						if (timesheetDay != null && dt.getDate().equals(timesheetDay.getDate())) {
							dt.setPayAmount(sf.getToursRate(dayType));
							dt.setDayType(dayType);
							payAmt = dt.getPayAmount();
							break;
						}
					}
					if (index >= 0 && payAmt != null) {
						for (int j = 0; j < wtc.getDailyAmounts().length; j++) {
							if (index == j) {
								LOG.debug("Index = " + index);
								wtc.getDailyAmounts()[j] = payAmt;
							}
						}
					}
					//wtc.setDailyAmounts(null); // force refresh of transient
					entry.setWeeklyTc(wtc);
					checkEventValidity(evt, TimecardFieldType.DT_DAY_TYPE.name(), wtc,
							TimecardFieldType.DT_DAY_TYPE, (dayId + 1));
				}
				calculateDailyTotals(); // update Total Wages column
				//}
//				LOG.debug("Day = " + (day+1));
//				for (WeeklyTimecard wtc : getTimecardList()) {
					// this was generating change events for timecards of both Home & Touring, not just the current 'workplace'
//					checkEventValidity(evt, TimecardFieldType.DT_DAY_TYPE.name(), wtc, TimecardFieldType.DT_DAY_TYPE, (day + 1));
//				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to check daytype and set default state to 'HM', LS-1981
	 *
	 * @param dayType
	 * @param index
	 */
	private void setToursDefaultState(DayType dayType, int index) {
		TimesheetDay timesheetDay = timesheet.getTimesheetDays().get(index);
		if (dayType.useHomeState()) {
			timesheetDay.setTouringState(Constants.TOURS_HOME_STATE);
		}
		else if (dayType == DayType.NONE || Constants.TOURS_HOME_STATE.equals(timesheetDay.getTouringState())) {
			timesheetDay.setTouringState(null);
		}
	}

	private void calculateTotals() {
		LOG.debug("");
		totalPayCategory1 = BigDecimal.ZERO;
		totalPayCategory2 = BigDecimal.ZERO;
		totalPayCategory3 = BigDecimal.ZERO;
		totalPayCategory4 = BigDecimal.ZERO;
		calculateDailyTotals();
		for (TimesheetEntry entry : getTimesheetEntryList()) {
			totalPayCategory1 =
					NumberUtils.safeAdd(totalPayCategory1, entry.getPayCategory1Amount());
			totalPayCategory2 =
					NumberUtils.safeAdd(totalPayCategory2, entry.getPayCategory2Amount());
			totalPayCategory3 =
					NumberUtils.safeAdd(totalPayCategory3, entry.getPayCategory3Amount());
			totalPayCategory4 =
					NumberUtils.safeAdd(totalPayCategory4, entry.getPayCategory4Amount());
		}
	}

	/**
	 * Calculate the total for the week on all timecards in the sheet, by adding
	 * up the daily amounts on the timecard. It also recalculates the {@link #totalWages} field.
	 * <p>
	 * This does not persist the updated timecards.
	 */
	private void calculateDailyTotals() {
		totalWages = BigDecimal.ZERO;
		for (TimesheetEntry entry : getTimesheetEntryList()) {
			if (timesheet.getPayPeriodType().isMonthly()) {
				calculateDailyTotalWages(entry.getWeeklyTc());
			}
			BigDecimal total = calculateDailyTotal(entry.getWeeklyTc());
			totalWages = totalWages.add(total);
		}
		setTotalWages(totalWages);
	}

	/**
	 * Calculate the total wages for the week on the given timecard, by adding
	 * up the daily amounts.
	 * <p>
	 * This does not persist the updated timecard.
	 *
	 * @return The daily total for the timecard; this does NOT include the
	 *         non-daily values such as per-diem.
	 */
	private BigDecimal calculateDailyTotal(WeeklyTimecard wtc) {
		BigDecimal grandTotal = BigDecimal.ZERO;
		for (BigDecimal amt : wtc.getDailyAmounts()) {
			grandTotal = NumberUtils.safeAdd(grandTotal, amt);
		}
		wtc.setAdjGtotal(grandTotal);
		return grandTotal;
	}

	/**
	 * Calculate the total wages on the monthly timecard for both sections
	 * separately, by adding up the daily amounts.
	 * <p>
	 * This does not persist the updated timecard.
	 *
	 */
	private void calculateDailyTotalWages(WeeklyTimecard wtc) {
		BigDecimal totalWages1 = BigDecimal.ZERO;
		BigDecimal totalWages2 = BigDecimal.ZERO;
		BigDecimal[] amounts = wtc.getDailyAmounts();

		for (int i = 0; i < amounts.length; i++) {
			if (i >= 15) {
				totalWages2 = NumberUtils.safeAdd(totalWages2, amounts[i]);
			}
			else {
				totalWages1 = NumberUtils.safeAdd(totalWages1, amounts[i]);
			}
		}
		wtc.setTotalWages1(totalWages1);
		wtc.setTotalWages2(totalWages2);
	}

	/** Listener for the fields of timesheet table.
	 * @param event
	 */
	public void listenValueChange(ValueChangeEvent event) {
		LOG.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
		try {
			if (event.getComponent() != null) {
				String id = event.getComponent().getId();
				calculateTotals();
				UIRepeat repeat = (UIRepeat)event.getComponent().findComponent("dailyAmounts");
				Integer rowNum = null;
				if (repeat != null) {
					// To get the row Index
					String rowId = repeat.getClientId();
					int rowIndex = rowId.length() - 14;
					char row = rowId.charAt(rowIndex);
					rowNum = Character.getNumericValue(row);
					LOG.debug("Row = " + rowNum);
					// To avoid java.lang.IndexOutOfBoundsException
					if (rowNum < getTimesheetEntryList().size()) {
						TimesheetEntry entry = getTimesheetEntryList().get(rowNum);
						if (id.startsWith("payAmount")) {
							// To get the column Index
							int dayNum = repeat.getIndex();
							LOG.debug("Day Number = " + dayNum);
							checkEventValidity(event, TimecardFieldType.DT_PAY_AMOUNT.name(), entry.getWeeklyTc(), TimecardFieldType.DT_PAY_AMOUNT, dayNum);
						}
						else if (id.startsWith("expense")) {
							int dayNum = Integer.parseInt(id.substring(id.length() - 1));
							LOG.debug("Day Number = " + dayNum);
							checkEventValidity(event, TimecardFieldType.EXP_QUANTITY.name() , entry.getWeeklyTc(), TimecardFieldType.EXP_QUANTITY, dayNum);
						}
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * ValueChangeListener for time sheet pay category drop-down list.
	 * Validate against other pay category dropdowns to m make sure there
	 * are not any duplicates selected.
	 *
	 * @param event contains value for selected pay category
	 */
	public void listenPayCategoryChange(ValueChangeEvent event) {
		String category = event.getComponent().getId();
		PayCategory categoryNewValue = (PayCategory)event.getNewValue();
		PayCategory categoryOldValue = (PayCategory)event.getOldValue();
		String payCatID1 = "";
		String payCatID2 = "";
		String payCatID3 = "";
		String payCatID4 = "";

		PayPeriodType type = timesheet.getPayPeriodType();
		if (type.isWeekly()) {
			payCatID1 = "w" + "payCategory1";
			payCatID2 = "w" + "payCategory2";
			payCatID3 = "w" + "payCategory3";
			payCatID4 = "w" + "payCategory4";
		}
		else if (type.isBiWeekly() || type.isSemiMonthly()) {
			payCatID1 = "bw" + "payCategory1";
			payCatID2 = "bw" + "payCategory2";
			payCatID3 = "bw" + "payCategory3";
			payCatID4 = "bw" + "payCategory4";

		}
		else if (type.isMonthly()) {
			payCatID1 = "mon" + "payCategory1";
			payCatID2 = "mon" + "payCategory2";
			payCatID3 = "mon" + "payCategory3";
			payCatID4 = "mon" + "payCategory4";
		}

		if (categoryNewValue != null) {
			HtmlSelectOneMenu category1 =
					(HtmlSelectOneMenu)event.getComponent().findComponent(payCatID1);
			Boolean iscustom1 = (category1.getValue() == null || category1.getValue().equals(PayCategory.CUSTOM));

			HtmlSelectOneMenu category2 =
					(HtmlSelectOneMenu)event.getComponent().findComponent(payCatID2);
			Boolean iscustom2 = (category2.getValue() == null || category2.getValue().equals(PayCategory.CUSTOM));

			HtmlSelectOneMenu category3 =
					(HtmlSelectOneMenu)event.getComponent().findComponent(payCatID3);
			Boolean iscustom3 = (category3.getValue() == null || category3.getValue().equals(PayCategory.CUSTOM));

			HtmlSelectOneMenu category4 =
					(HtmlSelectOneMenu)event.getComponent().findComponent(payCatID4);
			Boolean iscustom4 = (category4.getValue() == null || category4.getValue().equals(PayCategory.CUSTOM));

			boolean passed = true;
			//Validation for duplicate selection of pay category.
			if(category.equals(payCatID1)){
					if (! iscustom1) {
						if (categoryNewValue.equals(category2.getValue()) ||
								categoryNewValue.equals(category3.getValue()) ||
								categoryNewValue.equals(category4.getValue())) {
							category1.setValue(event.getOldValue());
							passed = false;
						}
					}
			}
			else if (category.equals(payCatID2)) {
					if (! iscustom2) {
						if (categoryNewValue.equals(category1.getValue()) ||
								categoryNewValue.equals(category3.getValue()) ||
								categoryNewValue.equals(category4.getValue())) {
							category2.setValue(event.getOldValue());
							passed = false;
						}
					}
			}
			else if (category.equals(payCatID3)) {
					if (! iscustom3) {
						if (categoryNewValue.equals(category1.getValue()) ||
								categoryNewValue.equals(category2.getValue()) ||
								categoryNewValue.equals(category4.getValue())) {
							category3.setValue(event.getOldValue());
							passed = false;
						}
					}
			}
			else if (category.equals(payCatID4)) {
					if (! iscustom4) {
						if (categoryNewValue.equals(category1.getValue()) ||
								categoryNewValue.equals(category2.getValue()) ||
								categoryNewValue.equals(category3.getValue())) {
							category4.setValue(event.getOldValue());
							passed = false;
						}
					}
			}
			else {
				passed = false;
			}
			if (! passed) {
				//message : 'Duplicate Pay Category Selected.'
				MsgUtils.addFacesMessage("Timesheet.Error.DuplicatePayCategories", FacesMessage.SEVERITY_ERROR);
			}
		}

		//Validation on pay category amount when user changes from a non-custom pay category
		// - to another non-custom pay category
		if ((categoryNewValue != null && !categoryNewValue.equals(PayCategory.CUSTOM))
				&& (categoryOldValue != null && !categoryOldValue.equals(PayCategory.CUSTOM))) {
			for (TimesheetEntry entry : getTimesheetEntryList()) {
				// LS-2891 to avoid 'Error 500'
				if (category.equals(payCatID1)) {
					if (entry.getPayCategory1Amount() != null) {
						entry.setPayCategory1Amount(BigDecimal.ZERO);
					}
					continue;
				}
				else if (category.equals(payCatID2)) {
					if (entry.getPayCategory2Amount() != null) {
						entry.setPayCategory2Amount(BigDecimal.ZERO);
					}
					continue;
				}
				else if (category.equals(payCatID3)) {
					if (entry.getPayCategory3Amount() != null) {
						entry.setPayCategory3Amount(BigDecimal.ZERO);
					}
					continue;
				}
				else if (category.equals(payCatID4)) {
					if (entry.getPayCategory4Amount() != null) {
						entry.setPayCategory4Amount(BigDecimal.ZERO);
					}
					continue;
				}
			}
		}
	}

	/**
	 * Listener for whether to collapse or expand the Timesheet table
	 *
	 * @param event
	 */
	public void listenToggleTimesheet(ActionEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		String id = event.getComponent().getId();
		if (id.equals("tsExpFirst") || id.equals("tsColFirst")) {
			expandTS1 = ! expandTS1;
		}
		else if (id.equals("tsExpSecond") || id.equals("tsColSecond")) {
			expandTS2 = ! expandTS2;
		}

	}

	/**
	 * Action method for the "Submit" button. Prompts user for PIN/password.
	 * <p>
	 * If the current user is also the first approver and they are submitting
	 * their own timecard, we will generate an automatic approval event for the
	 * timecard in addition to the Submit event (once they OK the prompt). In
	 * this case, the prompt diaLOG also includes the option to change the next
	 * approver.
	 *
	 * @return null navigation string
	 */
	public String actionSubmit() {
		try {
			LOG.debug("");
//			Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			for (TimesheetEntry ent : getTimesheetEntryList()) {
				// LS-2230 check timecard for missing SSN - update from Start
				WeeklyTimecard wtc = getWeeklyTimecardDAO().refresh(ent.getWeeklyTc());
				if (wtc != null && StringUtils.isEmpty(wtc.getSocialSecurity())) {
					StartForm sf = wtc.getStartForm();
					wtc.setSocialSecurity(sf.getSocialSecurity());
					getWeeklyTimecardDAO().attachDirty(wtc);
				}
			}

			//LS-1984 Timesheet submission mandatory field
			boolean isValid = true;
			int numDays = timesheet.getNumDays();
			for (int i = 0; i < numDays; i++) {
				TimesheetDay tDay = timesheet.getTimesheetDays().get(i);
				// Touring Check
				String city = tDay.getTouringCity();
				String state = tDay.getTouringState();
				String countryCode = tDay.getTouringCountryCode();
				DayType dayType = tDay.getTouringDayType();
				// LS-2924 Do not do validation for days where DayType has not been set.
				if (countryCode != null && countryCode.equals("US")  && dayType != null && dayType != DayType.NONE) {
					//LS-2062 Timesheet submission mandatory field --> City is not required if State = "HM"
					if ((city == null || city.isEmpty()) && !Constants.TOURS_HOME_STATE.equals(state)) {
						MsgUtils.addFacesMessage("Timesheet.ValidateSevenDays",	FacesMessage.SEVERITY_ERROR, "City", longDayNames.get(i));
						isValid = false;
					}
					if (state == null || state.trim().isEmpty()) {
						MsgUtils.addFacesMessage("Timesheet.ValidateSevenDays", FacesMessage.SEVERITY_ERROR, "State", longDayNames.get(i));
						isValid = false;
					}
					if (dayType == DayType.NONE) {
						MsgUtils.addFacesMessage("Timesheet.ValidateSevenDays", FacesMessage.SEVERITY_ERROR, "Day Type", longDayNames.get(i));
						isValid = false;
					}

				}
			}

			if (isValid) {
				if (SessionUtils.getProduction().getPayrollPref().getPayrollService() == null) {
					MsgUtils.addFacesMessage("Timesheet.NoPayrollService", FacesMessage.SEVERITY_ERROR);
					return null;
				}
				User currUser = SessionUtils.getCurrentUser(); // Note: don't use vUser, it may be stale!
				if (currUser.getPin() == null) {
					showChangePin = true;
					//changePinReason = PIN_REASON_SUBMIT;
					ChangePinBean.getInstance().show(this);
					addFocus("pin");
				}
				PinPromptBean bean = PinPromptBean.getInstance();
				bean.promptPin(this, ACT_SUBMIT, getMessagePrefix() + "PinSubmitSelf.");
				addFocus("submit");
				/*SubmitPromptBean bean = SubmitPromptBean.getInstance();
				bean.show(this, ACT_SUBMIT, getMessagePrefix()+"PinSubmitOther.", false);
				bean.setMessage(MsgUtils.formatMessage(getMessagePrefix()+"PinSubmitOther.Text"));
				addFocus("submitOther");*/
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_SUBMIT:
				actionSubmitOk(action);
				break;
			case ChangePinBean.ACT_PROMPT_PIN:
				setShowChangePin(false);
				break;
			case ACT_ADD_TC:
				actionAddTimecardsOk(action);
				break;
			case ACT_UPDATE_FROM_STARTS:
				actionUpdateFromStartsOk();
				break;
			case ACT_RECALL:
				actionRecallOk();
				break;
			case ACT_CREATE_TS:
				actionCreatetimesheetOk(action);
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/**
	 * Action method called (via confirmOk) when an employee has e-signed a
	 * document. This will submit the contact document to the first approver, if
	 * it finds a department approver then the document is submitted to first
	 * department approver in the chain and if not then it is forwarded to first
	 * production approver
	 *
	 * @param action The action code that was used when the prompt was displayed
	 * to the user.
	 * @return null navigation string.
	 */
	private String actionSubmitOk(int action) {
		int ret = 0;
		try {
			// build refreshed timecard list
			List<WeeklyTimecard> refreshList = new ArrayList<>();
			for (WeeklyTimecard weeklyTimecard : getTimecardList()) {
				weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
				if (weeklyTimecard == null) {
					LOG.warn("Attempt to submit a timecard that was deleted!");
					MsgUtils.addFacesMessage("Timesheet.Submit.Deleted", FacesMessage.SEVERITY_INFO);
					return null;
				}
				refreshList.add(weeklyTimecard);
			}
			timecardList = refreshList;

			// Calculate gross, generate daily breakdown
			List<TimecardMessage> msgs = new ArrayList<>();
			TimecardService.calculateMultipleHtg(timecardList, msgs);

			// Create APPROVE event for each timecard (unless already Approved)
			List<WeeklyTimecard> unsubmittedTimecardList = new ArrayList<>();
			List<WeeklyTimecard> submittedTimecardList = new ArrayList<>();
			for (WeeklyTimecard weeklyTimecard : getTimecardList()) {
				if (weeklyTimecard.getStatus().isFinal()) {
					LOG.debug("skipping previously approved timecard");
					unsubmittedTimecardList.add(weeklyTimecard);
					ret++;
					continue;
				}
				// mark each timecard approved & add signature event:
				WeeklyTimecard wtc = getWeeklyTimecardDAO().submitToursTimecard(weeklyTimecard);
				if (wtc != null) { // ok
					weeklyTimecard = wtc;
					submittedTimecardList.add(weeklyTimecard);
				}
				else {
					weeklyTimecard = getWeeklyTimecardDAO().refresh(weeklyTimecard);
					unsubmittedTimecardList.add(weeklyTimecard);
					ret++;
					continue;
				}
			}

			if (submittedTimecardList.size() == 0) {
				// No timecards in state to submit/approve
				if (ret == 0) {
					MsgUtils.addFacesMessage("Timesheet.Submit.None", FacesMessage.SEVERITY_INFO);
				}
				else {
					MsgUtils.addFacesMessage("Timesheet.Submit.AllIgnored", FacesMessage.SEVERITY_INFO, ret);
				}
				return null;
			}

			switch (ret) {
				case 0: // No timecards omitted -- all included in submission
					MsgUtils.addFacesMessage("Timesheet.SubmitOnBehalf.Done", FacesMessage.SEVERITY_INFO, submittedTimecardList.size());
					break;
				default:
					MsgUtils.addFacesMessage("Timesheet.Submit.Partial", FacesMessage.SEVERITY_INFO, submittedTimecardList.size(), ret);
					break;
			}

			// Add APPROVE event to Timesheet
			TimesheetEvent evt = TimesheetEventDAO.getInstance().createEvent(timesheet, submittedTimecardList.size(), TimedEventType.APPROVE);
			timesheet = getTimesheetDAO().refresh(timesheet);
			timesheet.getTimesheetEvents().add(evt);
			timesheet.setStatus(ApprovalStatus.APPROVED);
			getTimesheetDAO().attachDirty(timesheet);

//			Production prod = null;
			if (timesheet != null) {
				// Create Timesheet report to send to submitter
				Production prod = timesheet.getProduction();
				if (prod == null) {
					prod = SessionUtils.getNonSystemProduction();
				}
				Project project = null;
				if (prod.getType().isAicp()) {
					project = SessionUtils.getCurrentOrViewedProject();
				}

				// Create a WeeklyBatch in preparation for transmitting to payroll service
				WeeklyBatch batch =  WeeklyBatchUtils.addToursTimecardsToBatch(getTimecardList(), getWeekEndDate());
				if (batch != null) { // Ready to transmit the batch
					// Moved this block of code inside the if because report generation was creating a report with no
					// Daily time dollar amounts. LS-2235
					ReportBean report = ReportBean.getInstance();
					// Generate the report for the "Touring" day types & locations (not Home)
					String reportFile = report.generateToursTimesheet(timesheet.getId(), timesheet.getProdId(), null, WorkZone.DL.name(), true, false, timesheet.getNumDays());
					Contact submitter = SessionUtils.getCurrentContact(); // ContactDAO.getInstance().findByUserProduction(submitUser, prod);
					DoNotification.getInstance().timesheetSubmitted(project, timesheet.getEndDate(), submitter, reportFile);
					actionTransmit(batch, submittedTimecardList, reportFile);
				}
				else {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoBatches", FacesMessage.SEVERITY_INFO);
				}
			}
			timecardList = null;
			timesheetEntryList = null;
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Set up final parameters and call the service to transmit (or email) one
	 * or more batches and/or set of timecards to the payroll service.
	 *
	 * @param batch A single batch from which the timecards in 'cards' have been
	 *            selected.
	 * @param submittedTimecardList The timecards to be sent, when an entire
	 *            batch was not selected.
	 * @param reportFile The previously-created Timesheet report, to be included
	 *            with notification(s) sent as part of the transmit.
	 */
	private void actionTransmit(WeeklyBatch batch, List<WeeklyTimecard> submittedTimecardList, String reportFile) {
		WeeklyBatchService service = new WeeklyBatchService();
		PayrollService payrollService = SessionUtils.getProduction().getPayrollPref().getPayrollService();
		ServiceMethod method = payrollService.getSendBatchMethod();
		EmailBatchPopupBean transmitBean = EmailBatchPopupBean.getInstance();

		int count;
		if (submittedTimecardList != null && (! submittedTimecardList.isEmpty())) {
			count = service.transmitBatch(method, null, transmitBean, null, batch, submittedTimecardList, reportFile);
			if (count > 0) {
//				MsgUtils.addFacesMessage(msgPrefix + "SentOk", FacesMessage.SEVERITY_INFO, count);
			}
			else {
//				MsgUtils.addFacesMessage(msgPrefix + "Failed", FacesMessage.SEVERITY_ERROR);
			}
		}
	}

	/**
	 * Show all of the history events
	 * @return
	 */
	public String actionHistoryShowAll(){

		return null;
	}

	/**
	 * Show only the signature history events
	 * @return
	 */
	public String actionHistoryShowSignatures(){

		return null;
	}
	/*private void updateSentStatus(WeeklyBatch batch, int count) {
		WeeklyBatchDAO weeklyBatchDAO = WeeklyBatchDAO.getInstance();
		Date sent = new Date();
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			wtc = getWeeklyTimecardDAO().findById(wtc.getId());
			wtc.setTimeSent(sent);
			getWeeklyTimecardDAO().attachDirty(wtc);
		}
		weeklyBatchDAO.evict(batch);
		batch = weeklyBatchDAO.findById(batch.getId());
		batch.setSent(sent);
		batch.setTimecardsSent(count);
		weeklyBatchDAO.attachDirty(batch);
	}*/

	/**
	 * Called when the user Cancels one of our pop-up diaLOGs.
	 * @see com.lightspeedeps.web.view.View#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ChangePinBean.ACT_PROMPT_PIN:
				setShowChangePin(false);
				break;
			case ACT_ADD_TC:
				if (getAddTimecardsBean().getSelectAllTargets()) {
					getAddTimecardsBean().setSelectAllTargets(false);
					for (StartForm sf : getAddTimecardsBean().getStartFormList()) {
						sf.setChecked(false);
					}
				}
			default:
				LOG.debug("Cancel");
		}
		return res;
	}

	/**
	 * Action method for the "Print" button.
	 * @return null navigation string
	 */
	public String actionPrint() {
		try {
			if (timesheet != null) {
				boolean batched = (!timecardList.isEmpty() && timecardList.get(0).getWeeklyBatch() != null);
				ReportBean report = ReportBean.getInstance();
				report.generateToursTimesheet(timesheet.getId(), timesheet.getProdId(), null, workplace.getName(), batched, true, timesheet.getNumDays());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
	        MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to delete or void the Weeklytimecards based on their approval status.
	 * @return null navigation string.
	 */
	public String actionDeleteWeeklyTimecard() {
		try {
			if (weeklyTcToDelete != null) {
				LOG.debug("weeklyTcToDelete: " + weeklyTcToDelete.getId());
				weeklyTcToDelete = getWeeklyTimecardDAO().refresh(weeklyTcToDelete);
				if (weeklyTcToDelete != null) {
					getTimecardList().remove(weeklyTcToDelete);
					Iterator<TimesheetEntry> itr = timesheetEntryList.iterator();
					while (itr.hasNext()) {
						TimesheetEntry entry = itr.next();
						if (entry.getWeeklyTc().equals(weeklyTcToDelete)) {
							itr.remove();
							break;
						}
					}
					setTimesheetEntryList(timesheetEntryList);
					if (weeklyTcToDelete.getSubmitable()) {
						LOG.debug("Deleting weekly timecard=" + weeklyTcToDelete.getId());
						getWeeklyTimecardDAO().remove(weeklyTcToDelete);
					}
					else {
						LOG.debug("Voiding weekly timecard=" + weeklyTcToDelete.getId());
						weeklyTcToDelete.setStatus(ApprovalStatus.VOID);
						getWeeklyTimecardDAO().attachDirty(weeklyTcToDelete);
					}
					//timecardList = null;
					//timesheetEntryList = null;
				}
				getAddTimecardsBean().setStartFormList(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method to delete or void the timesheet when no employee been added.
	 * LS-2553
	 *
	 * @return null navigation string.
	 */
	public void actionDeleteTimesheet() {
		try {
			if (timesheet != null) {
				timesheet = getTimesheetDAO().refresh(timesheet);
				Iterator<SelectItem> itr = endDateList.iterator();
				while (itr.hasNext()) {
					SelectItem entry = itr.next();
					if (entry != null && entry.getValue().equals(timesheet.getEndDate())) {
						itr.remove();
						break;
					}
				}
				LOG.debug("Deleting timesheet=" + timesheet.getId());
				getTimesheetDAO().delete(timesheet);
				if (endDateList.size() > 0) {// set last date if endDateList is not empty
					weekEndDate = (Date)endDateList.get(endDateList.size() - 1).getValue();
					setUpTimesheetData();
					calculateTotals();
				}
				else {
					timesheet = null;
					endDateList = null;
					weekEndDate = null;
					setNewTimeSheet(true);
				}
				timecardList = null; //update timecardList
				timesheetEntryList = null;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method for the Add button. Results in displaying the
	 * "Add Timecard" diaLOG.
	 *
	 * @return null navigation string
	 */
	public String actionAddTimecard() {
		LOG.debug("");
		try {
			getAddTimecardsBean().setWeekEndingDate(timesheet.getEndDate());
			getAddTimecardsBean().setStartFormList(null);
			getAddTimecardsBean().setSfListReturn(true);
			getAddTimecardsBean().setStartDate(timesheet.getStartDate());
			getAddTimecardsBean().createRecipientList();
			if (getAddTimecardsBean().getStartFormList() != null && getAddTimecardsBean().getSfListReturn()) {
				getAddTimecardsBean().show(this, ACT_ADD_TC, null, null, null);
			}
			return null;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}
	/**
	 * Action method for the New Timesheet button. Results in displaying the
	 * "New Timesheet" diaLOG.
	 *
	 * @return null navigation string
	 */
	public String actionCreateTimesheet() {
		LOG.debug("");
		try {
			if (production != null) {
				Date weekStartDate = weekEndDate;
				getCreateTimesheetBean().setWeekStartingDate(weekStartDate);
				if (newTimeSheet) {
					//Create first timesheet
					getCreateTimesheetBean().show(this, ACT_CREATE_TS, null, null, null);
					return null;
				}
				else {
					// Create the next timeSheet starting with the date of last timesheet's end date + 1 day
					actionCreatetimesheetOk(ACT_CREATE_NEXT_TS);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called (via confirmOk) when a user clicks "Add" button
	 * on Timesheet page.
	 *
	 * @param action The action code that was used when the prompt was displayed
	 * to the user.
	 * @return null navigation string.
	 */
	private String actionAddTimecardsOk(int action) {
		try {
			LOG.debug("");
			List<StartForm> sfList = getAddTimecardsBean().getStartFormList();
			if (sfList != null && (! sfList.isEmpty())) {
				WeeklyTimecard newWtc = null;
				for (StartForm sf : sfList) {
					if (sf.getChecked()) {
						LOG.debug("Create timecard for start form= " + sf.getId());
						sf = StartFormDAO.getInstance().refresh(sf);
						sf.setChecked(false);
						int numdays = timesheet.getNumDays();
						newWtc = TimecardUtils.createTimecard(sf.getContact().getUser(),
								SessionUtils.getCurrentOrViewedProduction(), timesheet.getEndDate(),
								sf, false, numdays);
						if (newWtc != null) {
							for (int i = 0; i <= numdays - 1; i++) {
								LOG.debug("i  = " + i);
								TimesheetDay tsheetDay = timesheet.getTimesheetDays().get(i);
								DailyTime dt = newWtc.getDailyTimes().get(i);
								DayType dayType;

								if(sf.getWorkZone().isTouring()) {
									// Touring
									dayType = tsheetDay.getTouringDayType();
								}
								else {
									// Home
									dayType = tsheetDay.getHomeDayType();
								}

								if (tsheetDay != null && dayType != null && dt != null && dt.getDate().equals(tsheetDay.getDate())) {
									dt.setPayAmount(sf.getToursRate(dayType));
									dt.setDayType(dayType);
									dt.setWorked(true);
									DailyTimeDAO.getInstance().attachDirty(dt);
								}
							}
							if (newWtc.getExpenseLines() != null && ! newWtc.getExpenseLines().isEmpty()) {
								byte lineNum = newWtc.getExpenseLines().get(newWtc.getExpenseLines().size()-1).getLineNumber();
								lineNum++;
								LOG.debug("Creating Eeimburement Expense Line");
								PayExpense payExpense = new PayExpense(newWtc, lineNum);
								payExpense.setRate(BigDecimal.ZERO);
								payExpense.setQuantity(BigDecimal.ONE);
								payExpense.setCategory(PayCategory.REIMB.getLabel());
								payExpense.calculateTotal();
								PayExpenseDAO.getInstance().save(payExpense);
								LOG.debug("New pay expense for cat: " + payExpense.getCategory() + ", " + payExpense.getId());
								newWtc.getExpenseLines().add(payExpense);
							}
							calculateDailyTotal(newWtc);
							// Persist the adjusted grand total
							getWeeklyTimecardDAO().attachDirty(newWtc);
						}
					}
				}
				getAddTimecardsBean().setSelectAllTargets(false);
				getAddTimecardsBean().setStartFormList(null);
				timecardList = null;
				timesheetEntryList = null;
				if (newWtc != null) {
					if(timesheet.getId() == null) {
						// persist the (new) timesheet
						timesheet.setStatus(ApprovalStatus.OPEN);
						getTimesheetDAO().save(timesheet);
					}
				}
				getTimesheetEntryList(); // recreate list
				calculateTotals(); // ensure transients are calculated
				timesheet = getTimesheetDAO().refresh(timesheet);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Show the popup prompting the user to update values from the payroll start form.
	 * @return
	 */
	public String actionUpdateFromStarts() {
		PopupBean.getInstance().show(this, ACT_UPDATE_FROM_STARTS,
				"Timesheet.UpdateFromStarts.Title",
				"Timesheet.UpdateFromStarts.Text",
				"Confirm.OK", "Confirm.Cancel");
		return null;
	}

	/**
	 * Check if category matches with any of the pay categories.
	 * If so, update the existing pay expense
	 *
	 * @param cat PayCategory to match
	 * @param exp Get total expense
	 * @param entry Set pay category amount
	 */
	private void checkPayCategory(PayCategory cat, TimesheetEntry entry, BigDecimal amount) {
		boolean found = false;

		if (cat.equals(timesheet.getPayCategory1())) {
			entry.setPayCategory1Amount(amount);
			found = true;
		}
		else if (cat.equals(timesheet.getPayCategory2())) {
			entry.setPayCategory2Amount(amount);
			found = true;
		}
		else if (cat.equals(timesheet.getPayCategory3())) {
			entry.setPayCategory3Amount(amount);
			found = true;
		}
		else if (cat.equals(timesheet.getPayCategory4())) {
			entry.setPayCategory4Amount(amount);
			found = true;
		}

		if(found) {
			updateExpense(entry.getWeeklyTc(), cat, amount);
		}
	}

	/**
	 * Go through the list of timesheet entries and update the rates from their
	 * start forms.
	 *
	 * @return
	 */
	public String actionUpdateFromStartsOk() {
		if(timesheetEntryList != null && ! timesheetEntryList.isEmpty()) {
			for(TimesheetEntry entry : timesheetEntryList) {
				WeeklyTimecard wtc = entry.getWeeklyTc();
				// Get the latest Start form and update the WeeklyTimecard.
				StartForm sf = StartFormDAO.getInstance().refresh(wtc.getStartForm());
				wtc.setStartForm(sf);
				// Set the dailyAmount array to null to force recalc of the dailyAmounts.
				wtc.setDailyAmounts(null);
				// Iterate through the TimesheetDays and set the daily amount based on the TimesheetDays day type
				// and the employee's rate for that type.
				List<DailyTime> dailyTimes = wtc.getDailyTimes();
				List<TimesheetDay> timesheetDays = timesheet.getTimesheetDays();

				for(int dayIndex = 0; dayIndex < dailyTimes.size(); dayIndex++) {
					TimesheetDay tsd = timesheetDays.get(dayIndex);
					DailyTime dt = dailyTimes.get(dayIndex);

					// Get the dayType and find the associated rate.
					DayType dayType;
					if(sf.getWorkZone().isTouring()) {
						// Touring
						dayType = tsd.getTouringDayType();
					}
					else {
						// Home
						dayType = tsd.getHomeDayType();
					}
					BigDecimal rate = null;

					if(dayType != null) {
						switch (dayType) {
							case TPR: // Prep
								rate = sf.getToursPrepRate();
								break;
							case TPO: // Post
								rate = sf.getToursPostRate();
								break;
							case TSH: // Show
								rate = sf.getToursShowRate();
								break;
							case TTR: // Travel
								rate = sf.getToursTravelRate();
								break;
							case TDO: // Down
								rate = sf.getToursDownRate();
								break;
							case HOW: // Home - Work
								rate = sf.getToursHomeWorkRate();
								break;
							case HOO: // Home - Off
								rate = sf.getToursHomeOffRate();
								break;
							default:
								break;
						}
					}
					dt.setPayAmount(rate);
					if (! editMode) {
						DailyTimeDAO.getInstance().attachDirty(dt);
					}
				}
				checkPayCategory(PayCategory.PER_DIEM_TAX, entry, sf.getPerdiemTx().getAmt());
				checkPayCategory(PayCategory.PER_DIEM_NONTAX, entry, sf.getPerdiemNtx().getAmt());
				checkPayCategory(PayCategory.PER_DIEM_ADVANCE, entry, sf.getPerdiemAdv().getAmt());
				entry.setWeeklyTc(wtc);
			}
			calculateDailyTotals(); // update Total Wages column

			// If not in edit mode persist the changes so the values show up when printing.
			if(!editMode) {
				for(TimesheetEntry entry : timesheetEntryList) {
					WeeklyTimecard wtc = getWeeklyTimecardDAO().refresh(entry.getWeeklyTc());
					wtc.getExpenseLines().size();
					getWeeklyTimecardDAO().attachDirty(wtc);
				}
			}
		}

		return null;
	}

	/**Method checks whether event will be stored or not.
	 * @param event
	 */
	protected void checkEventValidity(ValueChangeEvent event, String description, WeeklyTimecard wtc, TimecardFieldType type, int lineNum) {
		String compId = "";
		try {
			compId = event.getComponent().getId();
			if (compId != null) {
				if (event.getOldValue() == null) {
					if (event.getNewValue() != null) {
						if (! event.getNewValue().toString().trim().isEmpty()) {
							// value changed - create entry
							saveChangeEvent(event, type, description, wtc, lineNum);
						}
					}
				}
				else {
					if(event.getOldValue().toString().isEmpty()) {
						if((event.getNewValue() != null) && (! event.getOldValue().equals(event.getNewValue()))) {
							// value changed - create entry
							saveChangeEvent(event, type, description, wtc, lineNum);
						}
					}
					else {
						if (type.getClazz() == String.class) {
							if ((event.getNewValue() == null) || (! event.getOldValue().equals(event.getNewValue()))) {
								// value changed - create entry
								saveChangeEvent(event, type, description, wtc, lineNum);
							}
						}
						else if (type.getClazz() == Boolean.class) {
							if ((event.getNewValue() == null) || ((Boolean)event.getOldValue()) != ((Boolean)event.getNewValue())) {
								// value changed - create entry
								saveChangeEvent(event, type, description, wtc, lineNum);
							}
						}
						else if (type.getClazz() == Byte.class) {
							if ((event.getNewValue() == null) || ((Byte)event.getOldValue()) != ((Byte)event.getNewValue())) {
								// value changed - create entry
								saveChangeEvent(event, type, description, wtc, lineNum);
							}
						}
						else if (type.getClazz() == BigDecimal.class) {
							LOG.debug("event.getOldValue() = " + event.getOldValue());
							LOG.debug("event.getNewValue() = " + event.getNewValue());
							if ((event.getNewValue() == null) || (((BigDecimal) event.getOldValue()).compareTo((BigDecimal)event.getNewValue()) != 0)) {
								LOG.debug("SAVE EVENT");
								// value changed - create entry
								saveChangeEvent(event, type, description, wtc, lineNum);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error("comp id=`" + compId + "`, new value=" + event.getNewValue());
			EventUtils.logError(e);
			// do NOT notify user of error -- it shouldn't affect their data/results.
		}
	}

	/** Method used to save the timecard change event for each event received by the listener.
	 * Method will be invoked every time when the user changes any number field for any number of times.
	 * @param event
	 * @param lineNum
	 * @param type2
	 */
	private void saveChangeEvent(ValueChangeEvent event, TimecardFieldType type, String description, WeeklyTimecard weeklyTimecard, int lineNum) {
		if (type != null && weeklyTimecard != null) {
			TimecardChangeEvent tcChange = new TimecardChangeEvent(getvUser(), TimedEventType.CHANGE, weeklyTimecard.getId(),
					type, lineNum, description, event.getOldValue(), event.getNewValue());
			LOG.debug("SAVING EVENT");
			TimecardChangeEventDAO.getInstance().save(tcChange);
		}
	}

	/** Value change listener for Country change event.
	 * @param evt
	 */
	public void listenChangeCountry(ValueChangeEvent evt) {
		try {
			LOG.debug("");
			if (evt.getNewValue() != null) {
				String countryIso = (String) evt.getNewValue();
				LOG.debug("countryIso = " + countryIso);
				int dayId = 0;
				// LS-2856
				Matcher m = COUNTRY_ID_PATTERN.matcher(evt.getComponent().getClientId());
				if (m.matches()) {
					dayId = Integer.parseInt(m.group(1)); // the occurrence number generated by icefaces
				}

				/*String id = evt.getComponent().getId();
				LOG.debug("id = " + id);
				int i;
				int day = 0;
				if ((i = id.indexOf('_')) > 0) {
					day = Integer.parseInt(id.substring(i + 1));
					LOG.debug("Day = " + day);*/

				TimesheetDay timesheetDay = timesheet.getTimesheetDays().get(dayId);
					LOG.debug("TimesheetDay = " + timesheetDay.getId());
					// LS-2166 Timesheet State/City Validations and LS-2765 passing changed country code
					setDefaultCityState(timesheetDay, countryIso,
						timesheetDay.getTouringDayType(), evt, dayId);
					if (! countryIso.equals("US")) {
						timesheetDay.setHomeCity(null);
						timesheetDay.setHomeState(null);
					}
				}
			//}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @return new Timesheet instance with sub-components created. None of the
	 *         created objects are persisted.
	 */
	private Timesheet createTimesheet(int numDays, Calendar calendar, PayPeriodType payPeriodType) {
		Timesheet tsheet = new Timesheet();

		tsheet.setProdId(getProduction().getProdId());
		tsheet.setEndDate(weekEndDate);
		tsheet.setStartDate(calendar.getTime());
		tsheet.setTimesheetDays(new ArrayList<TimesheetDay>(numDays));
		tsheet.setStatus(ApprovalStatus.OPEN);
		tsheet.setNumDays(numDays);
		tsheet.setPayPeriodType(payPeriodType);

		//To initialize Timesheet days
		for (int i = 0; i < numDays; i++) {
			TimesheetDay day = new TimesheetDay();
			// Fix to enable country code drop downs for initial view in edit mode.
//			if (workplace.isTouring()) {
//				LOG.debug("");
				day.setTouringCountryCode("US");
				day.setTouringDayType(DayType.NONE);
//			}
//			else {
//				LOG.debug("");
//				day.setHomeCountryCode("US");
//				day.setHomeDayType(DayType.NONE);
//			}
			day.setTimesheet(tsheet);
			day.setDate(calendar.getTime());
			calendar.add(Calendar.DAY_OF_MONTH, 1);

//			if (cal != null) { // 'cal' is current weekEndDate
//				if (i == 0) {
//					cal.add(Calendar.DAY_OF_MONTH, -6); // backup one day
//				}
//				else {
//					cal.add(Calendar.DAY_OF_MONTH, 1); // backup one day
//				}
//				day.setDate(cal.getTime());
//			}
			tsheet.getTimesheetDays().add(day);
		}
		LOG.debug("date=" + tsheet.getStartDate() + ", days=" + numDays + ", type=" + payPeriodType);
		return tsheet;
	}

	/**
	 * LS-2440 Calculate the pay period end date based on the type of Pay Period
	 * (Weekly, Bi-weekly Semi-monthly, Monthly)
	 *
	 * @param payPeriodType
	 * @param calendar
	 */
	private void calculatePayPeriodEnding(PayPeriodType payPeriodType, Calendar calendar) {
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		if (payPeriodType.isBiWeekly() || payPeriodType.isWeekly()) {
			calendar.add(Calendar.DAY_OF_MONTH, payPeriodType.getDays() - 1);
		}
		else if (payPeriodType.isSemiMonthly()) {
			// LS-2715 resolved issue for semi-monthly timesheet creation,
			// as it was taking 1st of the month by default irrespective of previous timesheet's end date
			if (dayOfMonth == 16) {
				calendar.set(Calendar.DAY_OF_MONTH,
						calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			}
			else {
				calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth + (payPeriodType.getDays() - 1));
			}
		}
		else if (payPeriodType.isMonthly()) {
			// If the starting date of the pay period is the first of the month,
			// then the week end date should be the last day of the month.
			if (dayOfMonth == 1) {
				calendar.set(Calendar.DAY_OF_MONTH,
						calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			}
			else {
				// Set the date to the next month and set the weekend date to start date minus 1
				calendar.add(Calendar.MONTH, 1);
				calendar.add(Calendar.DAY_OF_MONTH, - 1);
			}
		}
		weekEndDate = calendar.getTime();
	}

	/**
	 * LS-2440 Calculate the number of days for this timesheet based on the type
	 * of Pay Period (Weekly, Bi-weekly Semi-monthly, Monthly)
	 *
	 * @param payPeriod
	 * @param calendar
	 * @return
	 */
	private int calculateDaysForTimesheet(PayPeriodType payPeriodType, Calendar calendar) {
		int days = 0;
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

		if (payPeriodType.isBiWeekly() || payPeriodType.isWeekly()) {
			days = payPeriodType.getDays();
		}
		else if (payPeriodType.isSemiMonthly()) {
			// LS-2715 resolved issue for semi-monthly timesheet creation,
			// as it was taking 1st of the month by default irrespective of previous timesheet's end date
			if (dayOfMonth == 16) {
				// The first day will always be the 16th day of the month
				// so subtract 15 from the total number of days of the month
				// to get the number of days for this period. Set the week end date
				// to the last day of the month.
				int totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				days = totalDaysInMonth - payPeriodType.getDays();
			}
			else {
				days = payPeriodType.getDays();
			}
		}
		else if (payPeriodType.isMonthly()) {
			// If the start date is the first of the month, then
			// we get the number of days in the month.
			if (dayOfMonth == 1) {
				days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			}
			else {
				// Get the total number of days in the start date month
				// and subtract the day of the month from that.
				int totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - dayOfMonth;
				// Get the day of the month for the week ending and add that to the total days.
				Calendar calWeekEndDate =
						Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
				// LS-2715 resolved issue for monthly timesheet creation,
				// as it was having 1 day gap from previous timesheet's end date
				calWeekEndDate.setTime(calendar.getTime());
				days = totalDays + calWeekEndDate.get(Calendar.DAY_OF_MONTH);
			}
		}

		return days;
	}

	/** See {@link #timesheetEntryList}*/
	public List<TimesheetEntry> getTimesheetEntryList() {
		if (timesheetEntryList == null) {
			timesheetEntryList = createTimesheetEntryList();
		}
		return timesheetEntryList;
	}
	/** See {@link #timesheetEntryList}*/
	public void setTimesheetEntryList(List<TimesheetEntry> timesheetEntryList) {
		this.timesheetEntryList = timesheetEntryList;
	}

	/** See {@link #timecardList}*/
	public List<WeeklyTimecard> getTimecardList() {
		if (timecardList == null) {
			timecardList = createTimecardList();
		}
		return timecardList;
	}
	/** See {@link #timecardList}*/
	public void setTimecardList(List<WeeklyTimecard> timecardList) {
		this.timecardList = timecardList;
	}

	/** See {@link #weekEndDate}*/
	public Date getWeekEndDate() {
		return weekEndDate;
	}
	/** See {@link #weekEndDate}*/
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/**See {@link #dayTypeList}. */
	public List<SelectItem> getEndDateList() {
		if (endDateList == null || endDateList.isEmpty()) {
			endDateList = getTimesheetDAO().createWeekEndingList(production, "endDate desc");
		}
		return endDateList;
	}

	/** See {@link #workplaceDL}. */
	public List<SelectItem> getWorkplaceDL() {
		if(workplaceDL == null) {
			workplaceDL = new ArrayList<>();
			workplaceDL.add(new SelectItem( WorkZone.DL.getName(), "Touring"));
			workplaceDL.add(new SelectItem(WorkZone.ST.getName(), "Home"));
		}
		return workplaceDL;
	}

	/** See {@link #timesheet}*/
	public Timesheet getTimesheet() {
		return timesheet;
	}
	/** See {@link #timesheet}*/
	public void setTimesheet(Timesheet timesheet) {
		this.timesheet = timesheet;
	}

	/** See {@link #workplace}. */
	public WorkZone getWorkplace() {
		return workplace;
	}

	/** See {@link #workplace}. */
	public void setWorkplace(WorkZone workplace) {
		this.workplace = workplace;
	}

	/** See {@link #production}*/
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getNonSystemProduction();
		}
		return production;
	}
	/** See {@link #production}*/
	public void setProduction(Production production) {
		this.production = production;
	}

	/**See {@link #dayTypeList}. */
	public List<SelectItem> getDayTypeList() {
		if(dayTypeList == null) {
			createDayTypeList();
		}
		return dayTypeList;
	}

	/** See {@link #shortDayNames}. */
	public List<String> getShortDayNames() {
		return shortDayNames;
	}

	/** See {@link #longDayNames}. */
	public List<String> getLongDayNames() {
		return longDayNames;
	}

	/**See {@link #totalWages}. */
	public BigDecimal getTotalWages() {
		return totalWages;
	}
	/**See {@link #totalWages}. */
	public void setTotalWages(BigDecimal totalWages) {
		this.totalWages = totalWages;
	}

	/** See {@link #totalPayCategory1}. */
	public BigDecimal getTotalPayCategory1() {
		return totalPayCategory1;
	}

	/** See {@link #totalPayCategory1}. */
	public void setTotalPayCategory1(BigDecimal totalPayCategory1) {
		this.totalPayCategory1 = totalPayCategory1;
	}

	/** See {@link #totalPayCategory2}. */
	public BigDecimal getTotalPayCategory2() {
		return totalPayCategory2;
	}

	/** See {@link #totalPayCategory2}. */
	public void setTotalPayCategory2(BigDecimal totalPayCategory2) {
		this.totalPayCategory2 = totalPayCategory2;
	}

	/** See {@link #totalPayCategory3}. */
	public BigDecimal getTotalPayCategory3() {
		return totalPayCategory3;
	}

	/** See {@link #totalPayCategory3}. */
	public void setTotalPayCategory3(BigDecimal totalPayCategory3) {
		this.totalPayCategory3 = totalPayCategory3;
	}

	/** See {@link #totalPayCategory4}. */
	public BigDecimal getTotalPayCategory4() {
		return totalPayCategory4;
	}

	/** See {@link #totalPayCategory4}. */
	public void setTotalPayCategory4(BigDecimal totalPayCategory4) {
		this.totalPayCategory4 = totalPayCategory4;
	}

	/**See {@link #showChangePin}. */
	public boolean isShowChangePin() {
		return showChangePin;
	}
	/**See {@link #showChangePin}. */
	public void setShowChangePin(boolean showChangePin) {
		this.showChangePin = showChangePin;
	}

	/**See {@link #weeklyTcToDelete}. */
	public WeeklyTimecard getWeeklyTcToDelete() {
		return weeklyTcToDelete;
	}
	/**See {@link #weeklyTcToDelete}. */
	public void setWeeklyTcToDelete(WeeklyTimecard weeklyTcToDelete) {
		this.weeklyTcToDelete = weeklyTcToDelete;
	}

	/** See {@link #newComment}. */
	public String getNewComment() {
		return newComment;
	}

	/** See {@link #newComment}. */
	public void setNewComment(String newComment) {
		this.newComment = newComment;
	}

	/**See {@link #weeklyTimecardDAO}. */
	protected WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}

	/**See {@link #timesheetDAO}. */
	public TimesheetDAO getTimesheetDAO() {
		if (timesheetDAO == null) {
			timesheetDAO = TimesheetDAO.getInstance();
		}
		return timesheetDAO;
	}

	/**See {@link #addTimecardsBean}. */
	public AddTimecardsBean getAddTimecardsBean() {
		if (addTimecardsBean == null) {
			addTimecardsBean = AddTimecardsBean.getInstance();
		}
		return addTimecardsBean;
	}

	/** See {@link #createTimesheetBean}. */
	public CreateTimesheetBean getCreateTimesheetBean() {
		if (createTimesheetBean == null) {
			createTimesheetBean = CreateTimesheetBean.getInstance();
		}
		return createTimesheetBean;
	}

	/**See @link {@link #startFormDAO}	 */
	public StartFormDAO getStartFormDAO() {
		if(startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}

		return startFormDAO;
	}

	/**See @link {@link #payExpensDAO}	 */
	public PayExpenseDAO getPayExpenseDAO() {
		if(payExpenseDAO == null) {
			payExpenseDAO = PayExpenseDAO.getInstance();
		}

		return payExpenseDAO;
	}

	/** See {@link #showAllHistory}. */
	public boolean getShowAllHistory() {
		return showAllHistory;
	}

	/** See {@link #showAllHistory}. */
	public void setShowAllHistory(boolean showAllHistory) {
		this.showAllHistory = showAllHistory;
	}

	/**
	 * Create the DayTypeList based on whether the work zone is touring or home.
	 */
	private void createDayTypeList() {
		dayTypeList = new ArrayList<>();

		// Add blank first entry
		dayTypeList.add(new SelectItem(DayType.NONE.getName(), DayType.NONE.getLabel()));
		for (DayType daytype : DayType.values()) {
			if (daytype.isTours() && daytype != DayType.NONE) {
				//Removed to include "work" and "Off" Pay types for stationary staffs
				/*if(workplace.getName().equals("DL") && ! daytype.isTouringZone()) {
					continue;
				}
				else if (workplace.getName().equals("ST") && ! daytype.isToursHomeZone()) {
					continue;
				}*/
				dayTypeList.add(new SelectItem(daytype.getName(), daytype.getLabel()));
			}
		}
	}

	/** See {@link #deptId}. */
	public Integer getDeptId() {
		return deptId;
	}
	/** See {@link #deptId}. */
	public void setDeptId(Integer deptId) {
		this.deptId = deptId;
	}

	/** See {@link #deptList}. */
	public void setDeptList(List<SelectItem> deptList) {
		this.deptList = deptList;
	}
	/** See {@link #deptList}. */
	public List<SelectItem> getDeptList() {
		if (deptList == null || deptList.isEmpty()) {
			AuthorizationBean authBean = AuthorizationBean.getInstance();
			boolean userHasViewHtg = authBean.hasPageField(Constants.PGKEY_VIEW_HTG);

			if (userHasViewHtg || ApproverUtils.isProdApprover(getvContact())) {
				deptList = new ArrayList<>(DepartmentUtils.getDepartmentDataAdminDL(null, true));
			}
			else {
				// Find depts with this user as approver
				//Collection<Department> depts = ApproverUtils.createDepartmentsApproved(getvContact(), null);
				deptList = new ArrayList<>();
				for (Department dept : depts) {
					deptList.add(new SelectItem(dept.getId(), dept.getName()));
				}
			}
			deptList.add(0, new SelectItem(0, "All"));
			deptId = 0;
		}
		return deptList;
	}

	/** See {@link #isProdApprover}. */
	public boolean getIsProdApprover() {
		if (isProdApprover == null) {
			isProdApprover = ApproverUtils.isProdApprover(SessionUtils.getCurrentContact());
		}
		return isProdApprover;
	}

	/** See {@link #isDeptApprover}. */
	public boolean getIsDeptApprover() {
		if (depts != null && (! depts.isEmpty())) {
			isDeptApprover = true;
		}
		else {
			isDeptApprover = false;
		}
		return isDeptApprover;
	}
	/** See {@link #isDeptApprover}. */
	public void setIsDeptApprover(boolean isDeptApprover) {
		this.isDeptApprover = isDeptApprover;
	}

	/** See {@link #payCategoryList}. */
	public List<SelectItem> getPayCategoryList() {
		if (payCategoryList == null) {
			payCategoryList = new ArrayList<>();
			payCategoryList.add(new SelectItem(PayCategory.CUSTOM));
			payCategoryList.add(new SelectItem(PayCategory.REIMB, PayCategory.REIMB.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.PER_DIEM_NONTAX, PayCategory.PER_DIEM_NONTAX.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.PER_DIEM_TAX, PayCategory.PER_DIEM_TAX.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.SAL_ADVANCE_NONTAX, PayCategory.SAL_ADVANCE_NONTAX.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.PRE_TAX_401K, PayCategory.PRE_TAX_401K.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.ROTH, PayCategory.ROTH.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.PRE_TAX_INS3, PayCategory.PRE_TAX_INS3.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.PRE_TAX_INS2, PayCategory.PRE_TAX_INS2.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.BONUS, PayCategory.BONUS.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.MILEAGE_NONTAX, PayCategory.MILEAGE_NONTAX.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.CAN_TAX_DED, PayCategory.CAN_TAX_DED.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.INSURANCE, PayCategory.INSURANCE.getAbbreviation()));
			payCategoryList.add(new SelectItem(PayCategory.AUTO_EXP, PayCategory.AUTO_EXP.getAbbreviation()));
		}
		return payCategoryList;
	}

	/**
	 * method to set default timesheet city/state according to daytype and
	 * country code - LS-2166
	 *
	 * @param timesheetDay
	 * @param countryCode
	 * @param dayType
	 */
	private void setDefaultCityState(TimesheetDay timesheetDay, String countryCode,
			DayType dayType, ValueChangeEvent evt, int day) {

		if (dayType.equals(DayType.TSH) || dayType.equals(DayType.HOW)) {
			if (! countryCode.equals(Constants.DEFAULT_COUNTRY_CODE)) {
				timesheetDay.setTouringState(Constants.FOREIGN_OT_STATE);
				timesheetDay.setTouringCity(null);
			}
			else {
				// If coming from a foreign country, set the state to null
				// to remove the OT selection.
				String touringState = timesheetDay.getTouringState();
				if(touringState != null && touringState.equals(Constants.FOREIGN_OT_STATE)) {
					timesheetDay.setTouringState(null);
				}
			}
		}
		else if (dayType.equals(DayType.TPR) || dayType.equals(DayType.TPO)) {
			// Whether US or Foreign set state to HM. LS-2312
			timesheetDay.setTouringState(Constants.TOURS_HOME_STATE);
			timesheetDay.setTouringCity(null);
		}
		else if (dayType.equals(DayType.TR) || dayType.equals(DayType.TTR) ||
				dayType.equals(DayType.TDO) || dayType.equals(DayType.HOO)) {
			if (countryCode.equals(Constants.DEFAULT_COUNTRY_CODE)) {
				timesheetDay.setTouringState(Constants.TOURS_HOME_STATE);
			}
			else {
				timesheetDay.setTouringState(Constants.FOREIGN_OT_STATE);
			}
			timesheetDay.setTouringCity(null);
		}
		else if (dayType == DayType.NONE) {
			timesheetDay.setTouringCity(null);
		}
	}

	/**
	 * method to validate blank city/state according to day type and country
	 * code -LS-2166
	 * Validate that the city and state are a valid combination for US. LS-2287
	 * @return
	 */
	private boolean validateDayCityState() {
		boolean ret = true;

		for (int i = 0; i < 7; i++) {
			TimesheetDay tDay = timesheet.getTimesheetDays().get(i);
			String city = tDay.getTouringCity();
			String state = tDay.getTouringState();
			String countryCode = tDay.getTouringCountryCode();

			// Validate the city against the state for US and if state is not HM LS=2287.
			if(countryCode.equals(Constants.DEFAULT_COUNTRY_CODE) && city != null && !state.equals(Constants.TOURS_HOME_STATE)) {
				if(!LocationUtils.validateCityState(city, state)) {
					MsgUtils.addFacesMessage("Address.CityStateMisMatched",
							FacesMessage.SEVERITY_ERROR, city, state);
					ret = false;
					continue;
				}
			}
			DayType dayType = tDay.getTouringDayType();

			if (dayType.equals(DayType.TSH) || dayType.equals(DayType.HOW)) {
				// LS-2312 Only validate for US. Foreign will not have a city.
				if(state.equals(Constants.DEFAULT_COUNTRY_CODE)) {
					if (StringUtils.isEmpty(city)) {
						MsgUtils.addFacesMessage("Timesheet.Error.MissingCity",
								FacesMessage.SEVERITY_ERROR, dayType);
						return false;
					}
					if (StringUtils.isEmpty(state)) {
						MsgUtils.addFacesMessage("Timesheet.Error.MissingState",
								FacesMessage.SEVERITY_ERROR, dayType);
						return false;
					}
				}
			}
			else if (dayType.equals(DayType.TPR) || dayType.equals(DayType.TPO)) {
				// LS-2312 Skip validation for Post and Prep since the state always defaults
				// to HM for both US and Foreign.
				continue;
			}
			else if (dayType.equals(DayType.TR) || dayType.equals(DayType.TTR) ||
					dayType.equals(DayType.TDO) || dayType.equals(DayType.HOO)) {
				if (StringUtils.isEmpty(state)) {
					MsgUtils.addFacesMessage("Timesheet.Error.MissingState",
							FacesMessage.SEVERITY_ERROR, dayType);
					return false;
				}
			}
		}
		return ret;
	}

	/** LS-2219 In the State drop down FO should be replaced with OT */
	public List<SelectItem> getStateCodeDL() {
		return TOURS_STATE_LIST;
	}

	/** See {@link #isWeekEndChanged}. */
	public boolean getIsWeekEndChanged() {
		return isWeekEndChanged;
	}

	/** See {@link #isWeekEndChanged}. */
	public void setIsWeekEndChanged(boolean isWeekEndChanged) {
		this.isWeekEndChanged = isWeekEndChanged;
	}

	/** See {@link #monShortDayNames}. */
	public List<String> getMonShortDayNames() {
		return monShortDayNames;
	}

	/** See {@link #monShortDayNames}. */
	public void setMonShortDayNames(List<String> monShortDayNames) {
		this.monShortDayNames = monShortDayNames;
	}

	/** See {@link #expandTS1}. */
	public boolean getExpandTS1() {
		return expandTS1;
	}

	/** See {@link #expandTS1}. */
	public void setExpandTS1(boolean expandTS1) {
		this.expandTS1 = expandTS1;
	}

	/** See {@link #expandTS2}. */
	public boolean getExpandTS2() {
		return expandTS2;
	}

	/** See {@link #expandTS2}. */
	public void setExpandTS2(boolean expandTS2) {
		this.expandTS2 = expandTS2;
	}

	/**
	 * Action method called (via confirmOk) when a user clicks "New Timesheet"
	 * button on Timesheet page.
	 *
	 * @return null navigation string.
	 */
	private void actionCreatetimesheetOk(int action) {
		Calendar calendar = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		PayrollPreference pref = getProduction().getPayrollPref();
		pref = PayrollPreferenceDAO.getInstance().refresh(pref);
		PayPeriodType payPeriodType = pref.getPayPeriodType();
		Date startDate = null;

		if(action == ACT_CREATE_NEXT_TS) {
			List<Timesheet> timesheets =
					TimesheetDAO.getInstance().getTimesheetsByProduction(production,
							"endDate asc");
			Timesheet tsheet = timesheets.get(timesheets.size() - 1);
			calendar.setTime(tsheet.getEndDate());
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			startDate = calendar.getTime();
		}
		else {
			startDate = getCreateTimesheetBean().getWeekStartingDate();
			calendar.setTime(startDate);
		}

		// Set the week ending depending on the Pay Period Type
		calculatePayPeriodEnding(payPeriodType, calendar);

		// Reset calendar back to start date
		calendar.setTime(startDate);
		int numDays = calculateDaysForTimesheet(payPeriodType, calendar);

		timesheet = createTimesheet(numDays, calendar, payPeriodType);
		DateFormat df = new SimpleDateFormat(Constants.WEEK_END_DATE_FORMAT);
		// for immediately updating the payroll period end date's drop down list
		endDateList.add(0, new SelectItem(timesheet.getEndDate(), df.format(timesheet.getEndDate())));
		setNewTimeSheet(false);
		getTimesheetDAO().save(timesheet);
		//update timecardList
		timecardList = null;
		timesheetEntryList = null;
		weekEndDate = timesheet.getEndDate();
		// update timesheet day and dates for the new timesheet created
		setUpTimesheetData();
	}

}
