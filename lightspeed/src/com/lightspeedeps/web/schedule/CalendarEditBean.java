package com.lightspeedeps.web.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.DragDropEvent;

import com.lightspeedeps.dao.DateEventDAO;
import com.lightspeedeps.dao.ProjectScheduleDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.DateEvent;
import com.lightspeedeps.model.ProjectSchedule;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.web.popup.PopupCheckboxBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for the Calendar Edit page.
 */
@ManagedBean
@ViewScoped
public class CalendarEditBean extends CalendarViewBean implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 2895887946862637749L;

	private static final Log log = LogFactory.getLog(CalendarEditBean.class);

	private static final int ACT_SAVE = 10;
	private static final int PATH_LENGTH = PATH_PREFIX.length();
	private String imagePath = PATH_PREFIX + SHOOTING_IMAGE;

	private ProjectSchedule schedule = new ProjectSchedule();

	/** List of DateEvents created by user's Drag&Drop or click actions. */
	private List<DateEvent> updMonthDaysList = new ArrayList<>();

	/** List of schedule change notifications to be sent. */
	private List<ScheduleChange> changes;


	public  CalendarEditBean() {
		log.debug("");
		editMode = true;	// so View code knows it's running in edit mode
		initialize();
	}

	@Override
	protected void initView() {
		log.debug("");
		// For Edit object, we want View initialization to run after our constructor gets
		// control back from View constructor, so that our object is fully constructed!
	}

	protected void initialize() {
		log.debug("");
		super.initView(); // Now it's OK for View bean to initialize everything
		getScheduleUtils().startEditMode();
	}

	// clean updated month days list - if multiple drag and drop done on single date
	public void cleanUpdMonthDaysList() {
		DateEvent dEventTmp1;
		DateEvent dEventTmp2;

		for (int incr1 = 0; incr1 < updMonthDaysList.size(); incr1++) {
			dEventTmp1 = updMonthDaysList.get(incr1);

			for (int incr2 = incr1 + 1; incr2 < updMonthDaysList.size(); incr2++) {
				dEventTmp2 = updMonthDaysList.get(incr2);
				if (dEventTmp1.getStart().equals(dEventTmp2.getStart())) {
					if (dEventTmp1.getId() != 0) {
						dEventTmp2.setId(dEventTmp1.getId());
					}
					updMonthDaysList.remove(incr1);
					incr1--;
					break;
				}
			}
		}
	}

	/**
	 * Action method for the "Save" button; save user's changes.
	 *
	 * @return Navigation string - null if the save failed and we should remain
	 *         on the Edit page, otherwise the navigation value for the View
	 *         page.
	 */
	public String actionSave() {
		boolean saveOk = false;
		changes = new ArrayList<>();
		try {
			project = SessionUtils.getCurrentProject();
			unit = UnitDAO.getInstance().refresh(unit);
			getScheduleUtils().setUnit(unit);  // give it the (possibly) new copy
			schedule = unit.getProjectSchedule();

			cleanUpdMonthDaysList();

			ProjectScheduleDAO projectScheduleDao = ProjectScheduleDAO.getInstance();
			// check days Off Project Schedule Template
			if (numberDaysOff() == 7) {
				MsgUtils.addFacesMessage("Schedule.NoWorkdays", FacesMessage.SEVERITY_WARN);
				return null;
			}
//			else if (numberDaysOff() == 0) {
//				message = "Select at least one day off";
//				FacesContext.getCurrentInstance().addMessage(null,
//						new FacesMessage(FacesMessage.SEVERITY_WARN, message, message));
//				return;
//			}
			else {
				//save start date
				if ( ! getStartDate().equals(schedule.getStartDate())) {
					schedule.setStartDate(getStartDate());
					schedule = projectScheduleDao.merge(schedule);
				}

				String daysOffDB = schedule.getDaysOff();
				log.debug("schedule days off=" + daysOffDB);
				String daysOff = getScheduleUtils().getWeeklyDaysOffAsString();
				if ( ! daysOff.equals(daysOffDB)) {
					getScheduleUtils().updateDaysOff(daysOffDB);
					schedule.setLastDaysOffChange(new Date()); // today
					saveWeekDayNotification(changes);
					schedule.setDaysOff(daysOff);
					schedule = projectScheduleDao.merge(schedule);
				}
			}

			//Save updated Calendar
			Iterator<DateEvent> itr = updMonthDaysList.iterator();
			DateEvent dEventOldType ;
			DateEvent dEventTmp;
			WorkdayType oldType;
			boolean updateSchedule = false;
			final DateEventDAO dateEventDao = DateEventDAO.getInstance();
			while (itr.hasNext()) {
				dEventTmp = itr.next();
				if (dEventTmp.getId() > 0) { // existing event changed
					dEventOldType = dateEventDao.findById(dEventTmp.getId());
					oldType = dEventOldType.getType();
					boolean regularOff = getScheduleUtils().isWeeklyDayOff(dEventTmp.getStart());
					log.debug("changing from " + oldType + " to " + dEventTmp.getType() + ", off=" + regularOff);
					if ((regularOff && dEventTmp.getType() == WorkdayType.OFF) ||
							( ! regularOff && dEventTmp.getType() == WorkdayType.WORK)) {
						boolean b = schedule.getDateEvents().remove(dEventOldType);
						if (!b) {
							EventUtils.logError("remove failed for id=" + dEventOldType.getId());
						}
						updateSchedule = true;
						dateEventDao.delete(dEventOldType);
					}
					else {
						dEventOldType.setType(dEventTmp.getType());
						dEventOldType = dateEventDao.merge(dEventOldType);
					}
					saveNotification(changes, dEventTmp, oldType);
				}
				else { // new event - save to database
					if (dEventTmp.getType() != WorkdayType.WORK ||
							getScheduleUtils().isWeeklyDayOff(dEventTmp.getStart())) {
						dEventTmp.setProjectSchedule(schedule);
						dateEventDao.save(dEventTmp);
						schedule.getDateEvents().add(dEventTmp);
						updateSchedule = true;
						log.debug("adding event=" + dEventTmp.getType());
						if (dEventTmp.getType() == WorkdayType.WORK ) {
							// changed from standard (weekly) day off to work-day
							saveNotification(changes, dEventTmp, WorkdayType.OFF);
						}
						else { // changed from work-day to some non-work-day type
							saveNotification(changes, dEventTmp, WorkdayType.WORK);
						}
					}
				}
			}
			if (updateSchedule) {
				schedule = projectScheduleDao.merge(schedule);
			}
			getScheduleUtils().endEditMode();
			saveOk = true;
			ProductionDood.markUnitDirty(unit); // DooD is probably out of date
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SaveFailed", FacesMessage.SEVERITY_ERROR);
		}
		HttpSession session = SessionUtils.getHttpSession();
		session.setAttribute("curMonth", getCurMonth());
		session.setAttribute("curYear", getCurYear());

		if (saveOk) {
			if (changes.size() == 0) {
				return HeaderViewBean.SCHEDULE_MENU_CALENDAR; // go to calendar view page
			}
			PopupCheckboxBean conf = PopupCheckboxBean.getInstance();
			conf.prompt(this, ACT_SAVE, "Schedule.Changed.", false, null);
		}
		return null; // remain on edit page
	}

	/**
	 * Send a "weekDayOff" change notification for any day in the next week
	 * whose status has changed.  This method assumes the existing scheduleUtils
	 * has the new settings, and the project.projectSchedule object still has
	 * the old settings.
	 */
	private void saveWeekDayNotification(List<ScheduleChange> changed) {
		Calendar checkDate = new GregorianCalendar();
		CalendarUtils.setStartOfDay(checkDate);
		boolean oldOffDay, newOffDay;
		int weekday;
		Date startDate = getScheduleUtils().getStartDate();
		// check each of the next 7 days; if it's "weeklyDayOff" status has changed, do notify.
		for (int i=0; i < 7; i++) {
			if (! checkDate.getTime().before(startDate)) {
				weekday = checkDate.get(Calendar.DAY_OF_WEEK);
				oldOffDay = unit.getProjectSchedule().isDayOff(weekday);
				newOffDay = getScheduleUtils().isWeeklyDayOff(checkDate.getTime());
				if (oldOffDay != newOffDay) {
					ScheduleChange sc = new ScheduleChange(
							checkDate.getTime(),  oldOffDay, newOffDay);
					changed.add(sc);
				}
			}
			checkDate.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	/**
	 * Given a new DateEvent, and the day-type of the date prior to the change,
	 * check if it is after the project start date and within the next week, and
	 * if so, send a notification to the appropriate users.
	 *
	 * @param dateEvent The new event. (The date is set within the DateEvent.)
	 * @param oldType The WorkdayType of that date prior to the new event being
	 *            set.
	 */
	private void saveNotification(List<ScheduleChange> changes,
			DateEvent dateEvent, WorkdayType oldType) {
		if (! dateEvent.getStart().before(getScheduleUtils().getStartDate())) {
			Calendar date = new GregorianCalendar();
			Calendar curDate = new GregorianCalendar();
			Calendar futureDate = new GregorianCalendar();

			futureDate.add(Calendar.DAY_OF_MONTH, 8);
			date.setTime(dateEvent.getStart());

			CalendarUtils.setStartOfDay(date);
			CalendarUtils.setStartOfDay(curDate);
			CalendarUtils.setStartOfDay(futureDate);

			if (date.after(curDate) && date.before(futureDate)) {
				ScheduleChange sc = new ScheduleChange(date.getTime(),
						oldType.getLabel(), dateEvent.getType().getLabel());
				changes.add(sc);
			}
		}
	}

	private void sendNotifications(List<ScheduleChange> changes) {
		DoNotification no = DoNotification.getInstance();
		for (ScheduleChange sc : changes) {
			if (sc.oldType == null) {
				no.calendarDayOffChanged(unit, sc.date, sc.oldOff, sc.newOff);
			}
			else {
				no.calendarDayTypeChanged(unit, sc.date, sc.oldType, sc.newType);
			}
		}
	}

	/**
	 * Cancel button clicked; actionListener method.
	 */
	public void cancel(ActionEvent avt) {
		HttpSession session = SessionUtils.getHttpSession();
		session.setAttribute("curMonth", getCurMonth());
		session.setAttribute("curYear", getCurYear());
		getScheduleUtils().endEditMode();
		//clearUpdMonthDaysList();
	}

	public void daysOffValidator(FacesContext facesContext, UIComponent component, Object obj) {

	}

	/**
	 * ActionListener method called when user clicks on a calendar date.
	 * We update the date to match the currently selected day-type
	 * (from the radio-button list).
	 *  ** No longer used **
	 * @param event
	 */
//	public void imageChangeDay(ActionEvent event) {
//		String strId = event.getComponent().getId();
//		if (strId.indexOf("imageChangeDay") != -1) {
//			String str = strId.substring(strId.indexOf("imageChangeDay"));
//			int dayNum = Integer.parseInt(String.valueOf(str.charAt(str.length() - 1)));
//			// dayNum is now the day number (1=Sun to 7=Sat) within the week of our target.
//			OneWeek week = (OneWeek) event.getComponent().getAttributes().get("monthDaysObj");
//			if ((imagePath != null) && (!imagePath.equalsIgnoreCase(""))) {
//				processNewEvent(week, dayNum, "../../" + imagePath);
//			}
//		}
//	}

	/**
	 * Called when some change has been made to the schedule -- different
	 * weekly days off, or date event added.  The result is to update
	 * the appropriate data structures so the display is revised.
	 */
	private void updateCalendar() {
		findEndDate();
		setWeekList(createMonthCalendar(getCurYear(), getCurMonth()));
	}

	public void daysOffListener(ValueChangeEvent evt) {
		int dayofWeek = Integer.parseInt(evt.getComponent().getAttributes().get("dayofWeek").toString());
		log.debug("day checked=" + dayofWeek);

		boolean off = evt.getNewValue().toString().equalsIgnoreCase("true");
		getScheduleUtils().getWeeklyDaysOff()[dayofWeek] = off;

		if (numberDaysOff() == 7) {
			MsgUtils.addFacesMessage("Schedule.NoWorkdays", FacesMessage.SEVERITY_WARN);
			getScheduleUtils().getWeeklyDaysOff()[dayofWeek] = false;
			return;
		}
		getScheduleUtils().getWeeklyDaysOff()[dayofWeek] = off;
		updateCalendar();
	}

	/**
	 * Counts the number of days marked off in "weeklyDaysOff".
	 */
	private int numberDaysOff() {
		int n = 0;
		for (int i=1; i < 8; i++) {
			if (getScheduleUtils().getWeeklyDaysOff()[i]) {
				n++;
			}
		}
		log.debug("days off=" + n);
		return n;
	}

	// Clear or remove all items from updMonthDaysList
	public void clearUpdMonthDaysList() {
		updMonthDaysList.clear();
	}

	/**
	 * Called for drag and drop functionality, when an image is dropped in the calendar.
	 * This figures out which day of the month the image was dropped on, and which image
	 * (event type) was dropped.
	 *
	 * @param event The DragEvent created by the Icefaces framework.  Some important
	 * fields we get from the event:
	 * <p>
	 * Event type: if this is not DndEvent.DROPPED, the event is ignored.
	 * <p>
	 * targetClientId: the component id, of the form "prscheditFrm:j_id154:2:dropDayNumber5";
	 * 		the trailing number gives the day of the week, from 1=Sun to 7=Sat.
	 * <p>
	 * targetDropValue: the OneWeek object corresponding to the week in which
	 * 		the drop target resides.  This is the 'var' object within the ice:datatable
	 * 		that loops through 'monthDaysList'.
	 * <p>
	 * targetDragValue: the image path, e.g., "../../images/holiday.jpg", set by
	 * 		the dragValue attribute in the panelGroup.  The file name (without the
	 * 		.jpg extension) must match one of the DateEvent types, meaning a
	 * 		WorkDayType enum value. (FRAGILE coding practice!!)
	 *
	 */
	public void dragListener(DragDropEvent event) {
		// TODO rewrite this so it's not dependent on the image file name!
		// drop event: when image dropped in calendar
//		if (event.getTargetClientId() != null && event.getEventType() == DndEvent.DROPPED
//				&& event.getTargetDragValue() != null && event.getTargetDropValue() != null) {
//			String strId = event.getTargetClientId();
//			if (strId.indexOf("Number") != -1) {
//				String str = strId.substring(strId.indexOf("Number")+6);
//				int dayNum = Integer.parseInt(String.valueOf(str));
//				// dayNum is now the day number (1=Sun to 7=Sat) within the week of our target.
//				OneWeek week = (OneWeek) event.getTargetDropValue(); // the target week
//				String dragVal = event.getTargetDragValue().toString();
//				log.debug("drag value=" + dragVal + ", day#=" + dayNum + ", week with Sunday date="
//						+ week.dayNumber[1] + ", client id=" + strId);
//				processNewEvent(week, dayNum, dragVal);
//			}
//		}
	}

	/**
	 * Listener called when user drags existing calendar event.  We process
	 * it if it drags it outside the calendar area & treat it as deleting
	 * that event -- changing the drag source to a shooting day.
	 * The event's component's id is of the form "prscheditFrm:j_id160:3:dayNumber4". The
	 * trailing digit indicates which day number (1-7) in the week was affected.
	 * @param event
	 */
	public void dragListenerCal(DragDropEvent event) {
		OneWeek week = new OneWeek();
		UIComponent comp = event.getComponent();
		week = (OneWeek) comp.getAttributes().get("monthDaysObj");
		String strId = comp.getId();
		if (strId.indexOf("Number") != -1) {
			String str = strId.substring(strId.indexOf("Number")+6);
			int dayNum = Integer.parseInt(String.valueOf(str));
			// dayNum is now the day number (1=Sun to 7=Sat) within the week of our target.
			if (true /*event.getEventType() == DndEvent.DRAG_CANCEL*/) { // TODO ice4
				// drag_cancel event: when image dragged out of calendar
				if (getScheduleUtils().isWeeklyDayOff(week.date[dayNum])) { // change to OFF
					processNewEvent(week, dayNum, PATH_PREFIX + OFF_IMAGE);
				}
				else { // change to SHOOTING (work day)
					processNewEvent(week, dayNum, PATH_PREFIX + SHOOTING_IMAGE);
				}
			}
		}
	}

	/**
	 * Process an event caused by a drag operation completing.
	 * @param week The OneWeek object representing the week containing the changed date.
	 * @param dayNum The day number, from 1 to 7, indicating the day within the week.
	 * @param imagePath The image filename dropped or removed; we extract the filename
	 * (without the extension) and use this as the WorkdayType.
	 */
	private void processNewEvent(OneWeek week, int dayNum, String imagePath) {
		week.imagePathDay[dayNum] = imagePath; // update page display
		String type = imagePath.substring(PATH_LENGTH, imagePath.lastIndexOf(".")).toUpperCase();
		if (type.startsWith("ICON_")) {
			type = type.substring(5);
		}
		DateEvent dateEvent = new DateEvent(schedule, week.date[dayNum], WorkdayType.valueOf(type));
		dateEvent.setId(week.dateEventIdDay[dayNum]);
		updMonthDaysList.add(dateEvent);
		getScheduleUtils().updateDaysOffList(dateEvent);
		updateCalendar(); // refresh our display
	}

	/**
	 * ValueChangeListener method for the "start date" calendar control.
	 */
	public void changeStartDate(ValueChangeEvent evt) {
		Date date = (Date)evt.getNewValue();
		log.debug("new date=" + date + "; existing date=" + getStartDate());
		setStartDate(date);
		updateCalendar(); // refresh our display
	}

	/**
	 * Called when the user clicks OK on one of our pop-up
	 * confirmation dialogs.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		log.debug(action);
		String res = null;
		switch(action) {
			case ACT_SAVE:
				PopupCheckboxBean conf = PopupCheckboxBean.getInstance();
				if (conf.getCheck()) {
					sendNotifications(changes);
				}
				res = HeaderViewBean.SCHEDULE_MENU_CALENDAR;
				break;
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		switch(action) {
			case ACT_SAVE:
				return HeaderViewBean.SCHEDULE_MENU_CALENDAR;
		}
		return null;
	}

	/**
	 * @return the updMonthDaysList
	 */
	public List<DateEvent> getUpdMonthDaysList() {
		return updMonthDaysList;
	}
	/**
	 * @param updMonthDaysList the updMonthDaysList to set
	 */
	public void setUpdMonthDaysList(List<DateEvent> updMonthDaysList) {
		this.updMonthDaysList = updMonthDaysList;
	}

	/**
	 * @return the imagePath
	 */
	public String getImagePath() {
		return imagePath;
	}
	public void setImagePath(String path) {
		imagePath = path;
	}

	/**
	 * @return the monthDateEvent
	 */
	public List<DateEvent> getMonthDateEvent() {
		return monthDateEvent;
	}
	/**
	 * @param monthDateEvent the monthDateEvent to set
	 */
	public void setMonthDateEvent(ArrayList<DateEvent> monthDateEvent) {
		this.monthDateEvent = monthDateEvent;
	}

	/**
	 * @return the weeklyDaysOff
	 */
	public boolean[] getWeeklyDaysOff() {
		return getScheduleUtils().getWeeklyDaysOff();
	}

	/**
	 * Data class to hold information on schedule changes that will be
	 * used to generate notifications, if the user indicates that such
	 * notifications should be sent.
	 */
	private static class ScheduleChange {
		Date date;
		String oldType;
		String newType;
		boolean oldOff;
		boolean newOff;

		ScheduleChange(Date dat, String oldT, String newT) {
			date = dat;
			oldType = oldT;
			newType = newT;
		}

		ScheduleChange(Date dat, boolean oldB, boolean newB) {
			date = dat;
			oldOff = oldB;
			newOff = newB;
		}

	}

}
