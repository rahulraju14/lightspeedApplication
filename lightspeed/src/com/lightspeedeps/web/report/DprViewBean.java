package com.lightspeedeps.web.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.DprDAO;
import com.lightspeedeps.dao.FilmStockDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ContactRole;
import com.lightspeedeps.object.DeptTime;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ReportRequirementsUtils;
import com.lightspeedeps.util.report.DprUtils;
import com.lightspeedeps.util.report.ReportTimeUtils;
import com.lightspeedeps.web.contact.ContactRoleSelectBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.Scroller;

/**
 * Backing bean for the DPR View and Edit pages.
 */
@ManagedBean
@ViewScoped
public class DprViewBean extends Scroller implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 2934965272722605592L;

	private static final Log log = LogFactory.getLog(DprViewBean.class);

	public static final String ATTR_NEW_DATE = Constants.ATTR_PREFIX + "newDprDate";
	public static final String ATTR_DPR_ID = Constants.ATTR_PREFIX + "dprid";
	public static final String ATTR_DPR_EDIT = Constants.ATTR_PREFIX + "dpredit";

	private static final int ACT_ADD_CREW = 11;
	private static final int ACT_CLEAR_CREW_TIMES = 12;
	private static final int ACT_IMPORT_CREW_TIMES = 13;

	private Dpr dpr;

	/** The database id of the displayed DPR, or the one just selected
	 * by the user from the date drop-down list. */
	private Integer dprId;

	/** The drop-down list of dates of DPR`s, allowing
	 * the user to switch between displayed reports. */
	private List<SelectItem> dateDL = new ArrayList<>();

	private List<FilmStock>filmStockList;
	private List<DeptTime> deptTime[];

	private Project project;

	private boolean editMode;

	/** Database id of the Department for which the user is adding a new
	 * Crew entry or deleting a crew entry. */
	private Integer addDeptId;

	/** A Collection of the CrewCall's that have been deleted during the
	 * current edit session. */
	private Collection<ReportTime> removedCalls;

	public DprViewBean() {
		log.debug("" + this);
		try {
			Integer id;
			setScrollable(true);
			project = SessionUtils.getCurrentProject();
			editMode = SessionUtils.getBoolean(ATTR_DPR_EDIT, false);
			DprDAO dprDAO = DprDAO.getInstance();
			Date dprDate = SessionUtils.getDate(ATTR_NEW_DATE);
			if (dprDate != null) {
				DprUtils util = new DprUtils();
				dpr = util.create(dprDate);
				SessionUtils.put(ATTR_NEW_DATE, null);
				if (dpr == null) {
					dpr = new Dpr();
					dpr.setDate(new Date());
					MsgUtils.addFacesMessage("Dpr.CreateFailed", FacesMessage.SEVERITY_ERROR);
				}
				SessionUtils.put("dpr", dpr);
				editMode = true;
			}
			else if ( (id = SessionUtils.getInteger(ATTR_DPR_ID)) != null) {
				dpr = dprDAO.findById(id);
			}
			else if ( (dpr=(Dpr)SessionUtils.get("dpr")) != null) {
			}
			else {
				// not normal; possibly user entered the URL directly
				List<Dpr> list = dprDAO.findByProject(project);
				if (list.size() > 0) { // pick most recent for this project
					dpr = list.get(0);
				}
			}
			if (dpr == null) {
				// not normal; possibly user entered the URL directly & no DPRs in project
				dpr = new Dpr();
				dpr.setDate(new Date());
				MsgUtils.addFacesMessage("Dpr.Missing", FacesMessage.SEVERITY_ERROR);
			}
//			SessionUtils.put(ATTR_DPR_EDIT, null);
			dateDL = createDateDL();

			initView(dpr);
		}
		catch (Exception e) {
			EventUtils.logError("DprViewBean failed, Exception: ", e);
		}
	}

	/**
	 * Do setup necessary to display the DPR page properly.
	 * @param dpr
	 */
	@SuppressWarnings("unchecked")
	private void initView(Dpr dpr) {
		Date dprDate = dpr.getDate();
		dprId = dpr.getId();
		FilmStockDAO filmStockDAO = FilmStockDAO.getInstance();

		// Get Media/film Stock data - all projects, latest info as of DPR's date
		filmStockList = filmStockDAO.findLatestThroughDate(dprDate);
		FilmMeasure fmZero = new FilmMeasure(0, 0, 0);
		for (FilmStock fs : filmStockList) {
			if (! fs.getDate().equals(dprDate)) { // not for today, so change totals, etc.
				fs.setInventoryPrior(fs.getInventoryTotal());
				fs.setInventoryUsedToday(0);
				fs.setInventoryReceived(0);
				fs.setUsedPrior(fs.getUsedTotal());
				fs.setUsedToday(fmZero);
				filmStockDAO.evict(fs); // don't contaminate pool with modified FS's.
				//log.debug("date: " + fs.getDate()
				//		+ ", pri waste="+ fs.getUsedPrior().getWaste()
				//		+ ", tot waste=" + fs.getUsedTotal().getWaste());
			}
		}

		// Get Crew data, split into 3 columns for back page layout
		deptTime = new List[3];
		DprUtils.createDeptColumns(dpr, deptTime);

		if (editMode) {
			initEdit();
		}
		restoreScrollFromSession(); // restore div's scroll position
	}

	/**
	 * Set up fields that require specific changes for Edit mode.
	 */
	private void initEdit() {
		// convert html breaks to new-lines for editing
		dpr.setProducer(StringUtils.editHtml(dpr.getProducer()));
		dpr.setContact1(StringUtils.editHtml(dpr.getContact1()));
		dpr.setContact2(StringUtils.editHtml(dpr.getContact2()));
		dpr.setContact3(StringUtils.editHtml(dpr.getContact3()));
		dpr.setContact4(StringUtils.editHtml(dpr.getContact4()));
		dpr.setGeneralNotes(StringUtils.editHtml(dpr.getGeneralNotes()));
		dpr.setCrewNotes(StringUtils.editHtml(dpr.getCrewNotes()));
		dpr.setEquipmentNotes(StringUtils.editHtml(dpr.getEquipmentNotes()));
		dpr.setProductionNotes(StringUtils.editHtml(dpr.getProductionNotes()));
		for (DprScene sc : dpr.getDprScenes()) {
			sc.setLocation(StringUtils.editHtml(sc.getLocation()));
		}
		removedCalls = new ArrayList<>();
		// provide empty Background line items
		for (int i = 900; i < 902; i++) {
			ExtraTime ex = new ExtraTime(dpr, i);
			dpr.getBackgrounds().add(ex);
		}
	}

	/**
	 * Approve the DPR.  Invoked via the ActionListener method on the Approve button of the
	 * DPR View page.  Update the DPR's status.
	 * @param evt
	 */
	public void actionApprove(ActionEvent evt) {
		log.debug("");
		try {
			DprDAO dprDAO = DprDAO.getInstance();
			dpr = dprDAO.findById(dpr.getId());
			getDpr().setStatus(ReportStatus.APPROVED);
			dprDAO.attachDirty(getDpr());
			log.debug("DPR Approved ");
		}
		catch (Exception e) {
			EventUtils.logError("approve failed: ", e);
		}
		return;
	}

	/**
	 * Reject the submitted DPR.  Invoked via the ActionListener method on the Reject button of the
	 * DPR View page.  Change the DPR's status to UPDATED.
	 * @param evt
	 */
	public void actionReject(ActionEvent evt) {
		try {
			DprDAO dprDAO = DprDAO.getInstance();
			dpr = dprDAO.findById(dpr.getId());
			getDpr().setStatus(ReportStatus.UPDATED);
			dprDAO.attachDirty(getDpr());
		}
		catch (Exception e) {
			EventUtils.logError("reject failed: ", e);
		}
		return;
	}

	/**
	 * Submit the DPR for approval. Invoked via the ActionListener method on the Submit button of
	 * the DPR View page. Update the DPR's status, then call DoNotification to send notifications.
	 *
	 * @param evt
	 */
	public void actionSubmit(ActionEvent evt) {
		log.debug("");
		try {
			DprDAO dprDAO = DprDAO.getInstance();
			dpr = dprDAO.findById(dpr.getId());
			getDpr().setStatus(ReportStatus.SUBMITTED);
			dprDAO.attachDirty(getDpr());
			log.debug("DPR submitted ");

			ReportRequirement requirement = ReportRequirementsUtils.findRequirement(ReportType.APPROVE_DPR);
			List<Contact> contactList = ReportRequirementsUtils.getContactList(requirement);
			if (contactList.size() > 0) {
				DoNotification no = DoNotification.getInstance();
				no.dprSubmitted(contactList, getDpr().getDate());
			}
		}
		catch (Exception e) {
			EventUtils.logError("submit failed: ", e);
		}
		return;// Constants.SUCCESS;
	}

	public String actionOpenAddCrew() {
		ContactRoleSelectBean selectBean = ContactRoleSelectBean.getInstance();
		selectBean.show(this, ACT_ADD_CREW, "Dpr.AddCrew.Title", "Dpr.AddCrew.Message");
		selectBean.setDepartmentId(addDeptId);
		selectBean.setUnitId(dpr.getProject().getMainUnit().getId());
		selectBean.setOmitContactRoleMap(createCrewRoleList());
		return null;
	}

	/**
	 * The Action method for the delete icon on the individual crew-call entries.
	 * The 'removeCrewCallId' and 'addDeptId' fields should already be set upon
	 * entry due to f:setPropertyActionListener tags in the JSP.
	 * @return null navigation string
	 */
	public String actionDeleteCrewTimeCard(ReportTime timecard) {
		try {
			if(timecard != null) {
				DeptTime dt = null;
				if(addDeptId != null) {
					outer:
					for (List<DeptTime> list : deptTime) {
						for (DeptTime dt2 : list) {
							if (dt2.getDepartment().getId().equals(addDeptId)) {
								dt = dt2;
								break outer;
							}
						}
					}
				}
				else {
					// dept id = null means it's the user-customized entry -- last one
					dt = deptTime[2].get(deptTime[2].size()-1);
				}

				List<ReportTime> cards = dt.getTimeCards();
				if(cards != null && !cards.isEmpty()) {
					cards.remove(timecard);
					if(timecard.getId() != null && timecard.getId() > 0) {
						removedCalls.add(timecard); // remember to delete it from database
					}

					boolean rem = dpr.getCrewTimeCards().remove(timecard);
					if (! rem) {
						log.warn("remove failed, timecard=" + timecard);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		return null;
	}

	/**
	 * Action method for the "clear times" button above the crew (back page)
	 * area. This just puts up a confirmation dialog.
	 *
	 * @return null navigation string
	 */
	public String actionClearCrewTimes() {
		PopupBean.getInstance().show(this, ACT_CLEAR_CREW_TIMES,
				"Dpr.ClearCrewTimes.Title", "Dpr.ClearCrewTimes.Message",
				"Confirm.Confirm", "Confirm.Cancel");
		return null;
	}

	/**
	 * The action method that clears all call and wrap times from the crew
	 * section (back page) of the current DPR. Note that the DPR is not saved at
	 * this time ... the user can still "undo" the clear action by using the
	 * main Cancel button.
	 *
	 * @return null navigation string
	 */
	private String actionClearCrewTimesOk() {
		for (ReportTime tc : dpr.getCrewTimeCards()) {
			tc.setCallTime(null);
			tc.setWrap(null);
			tc.setReportSet(null);
			tc.setDismissSet(null);
		}
		return null;
	}

	/**
	 * Action method for the "Import times" button above the crew section (back
	 * page) of the DPR. This puts up the prompting dialog to gather the user's
	 * options.
	 *
	 * @return null navigation string
	 */
	public String actionImportCrewTimes() {
		DprPopupBean.getInstance().showImportTimes(this, ACT_IMPORT_CREW_TIMES);
		return null;
	}

	/**
	 * The action method that handles importing times from a callsheet or
	 * WeeklyTimeCard`s into the current DPR's crew area.
	 *
	 * @return null navigation string
	 */
	private String actionImportCrewTimesOk() {
		DprPopupBean bean = DprPopupBean.getInstance();
		// gather the option settings from the pop-up dialog manager:
		boolean importCall = bean.getImportCallTime();
		boolean importWrap = bean.getImportWrapTime();
		boolean useSheet = bean.getCallTimeSource().equals(DprPopupBean.SOURCE_CALLSHEET);

		// import the data as requested
		if (importCall && useSheet) {
			ReportTimeUtils.updateCrewTimeCardsFromCallsheet(dpr);
			MsgUtils.addFacesMessage("Dpr.Import.Callsheet.Done", FacesMessage.SEVERITY_INFO);
		}
		if (importWrap || (importCall && !useSheet)) {
			ReportTimeUtils.updateCrewTimeCardsFromWtc(dpr, importCall && !useSheet, importWrap);
			if (importCall && !useSheet) {
				MsgUtils.addFacesMessage("Dpr.Import.Timecards.CallDone", FacesMessage.SEVERITY_INFO);
			}
			if (importWrap) {
				MsgUtils.addFacesMessage("Dpr.Import.Timecards.WrapDone", FacesMessage.SEVERITY_INFO);
			}
		}
		return null;
	}

	/**
	 * Currently unused (8/14/2012, rev 3312)
	 * Distribute the DPR for approval. Invoked via the Action method on the Distribute button of
	 * the DPR View page. Update the DPR's status, then call DoNotification to send notifications.
	 *
	 */
	@SuppressWarnings("unused")
	private String actionDistribute() {
/*		log.debug("");
		try {
			PrintDailyReportBean report = PrintDailyReportBean.getInstance();
			report.printAndSendDpr(dpr);
		}
		catch (Exception e) {
			EventUtils.logError("publish failed: ", e);
		}
*/		return null;
	}

	/**
	 * Action method for the "Edit" button on DPR View.
	 *
	 * @return The navigation string to jump to the DPR Edit page.
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		super.actionEdit();
		SessionUtils.put(ATTR_DPR_EDIT, true);
		return HeaderViewBean.REPORTS_MENU_DPR_EDIT;
	}

	/**
	 * Action method for the "Cancel" button on the edit mode page.
	 *
	 * @return Navigation string to jump to View mode page.
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		SessionUtils.put(ATTR_DPR_EDIT, null);
		SessionUtils.put("dpr", null);
		String navigate = "dprview";
		if (dpr != null) {
			SessionUtils.put(ATTR_DPR_ID, dpr.getId());
			if (dpr.getId() == null) { // newly created, never saved
				navigate = "reports";
			}
		}
		dpr = null; // will get reloaded if we navigate to view page
		return navigate;
	}

	/**
	 * ActionListener method for the "Save" button on the Edit DPR page.
	 * @param event
	 */
	public void actionSave(ActionEvent event) {
		log.debug("" + this);
		Date date = dpr.getDate();
		log.debug("DPR date -->>  "+date);
		if (dpr.getCrewCall() != null) {
			dpr.setCrewCall(CalendarUtils.sameDateAs(date, dpr.getCrewCall()));
		}
		if (dpr.getShootingCall() != null) {
			dpr.setShootingCall(CalendarUtils.sameDateAs(date, dpr.getShootingCall()));
		}
		if (dpr.getFirstShot() != null) {
			dpr.setFirstShot(CalendarUtils.sameDateAs(date, dpr.getFirstShot()));
		}
		if (dpr.getFirstMealBegin() != null) {
			dpr.setFirstMealBegin(CalendarUtils.sameDateAs(date, dpr.getFirstMealBegin()));
		}
		if (dpr.getFirstMealEnd() != null) {
			dpr.setFirstMealEnd(CalendarUtils.sameDateAs(date, dpr.getFirstMealEnd()));
		}
		if (dpr.getFirstShotAfter1stMeal() != null) {
			dpr.setFirstShotAfter1stMeal(CalendarUtils.sameDateAs(date, dpr.getFirstShotAfter1stMeal()));
		}
		if (dpr.getSecondMealBegin() != null) {
			dpr.setSecondMealBegin(CalendarUtils.sameDateAs(date, dpr.getSecondMealBegin()));
		}
		if (dpr.getSecondMealEnd() != null) {
			dpr.setSecondMealEnd(CalendarUtils.sameDateAs(date, dpr.getSecondMealEnd()));
		}
		if (dpr.getFirstShotAfter2ndMeal() != null) {
			dpr.setFirstShotAfter2ndMeal(CalendarUtils.sameDateAs(date, dpr.getFirstShotAfter2ndMeal()));
		}
		if (dpr.getCameraWrap() != null) {
			dpr.setCameraWrap(CalendarUtils.sameDateAs(date, dpr.getCameraWrap()));
		}
		if (dpr.getLastManOut() != null) {
			dpr.setLastManOut(CalendarUtils.sameDateAs(date, dpr.getLastManOut()));
		}

		for (Unit u : project.getUnits()) {
			if (! u.getActive()) { // null out inactive project's values (for Jasper reports)
				dpr.getDprDaysScheduled().getUnits()[u.getNumber()] = null;
				dpr.getDprDaysToDate().getUnits()[u.getNumber()] = null;
			}
			else { // set active units to zero if null
				if (dpr.getDprDaysScheduled().getUnits()[u.getNumber()] == null) {
					dpr.getDprDaysScheduled().getUnits()[u.getNumber()] = BigDecimal.ZERO; // DprDays.ZERO_DAYS;
				}
				if (dpr.getDprDaysToDate().getUnits()[u.getNumber()] == null) {
					dpr.getDprDaysToDate().getUnits()[u.getNumber()] = BigDecimal.ZERO; // DprDays.ZERO_DAYS;
				}
			}
		}
		dpr.getDprDaysScheduled().pushUnits();
		dpr.getDprDaysToDate().pushUnits();

		DprDAO dprDAO = DprDAO.getInstance();
		//log.debug("Extra time Set size --> " + dpr.getExtraTimes().size());
		int i = 0;
		for (Iterator<ExtraTime> iter = dpr.getExtraTimes().iterator(); iter.hasNext(); ) {
			ExtraTime et = iter.next();
			if (et.getCount() == null || et.getCount() <= 0) {
				et.setCount(-1);
				dprDAO.delete(et);
				iter.remove();
			}
			else {
				et.setLineNumber(i++);
			}
		}

		// convert new-lines to html breaks for display & printing
		dpr.setProducer(StringUtils.saveHtml(dpr.getProducer()));
		dpr.setContact1(StringUtils.saveHtml(dpr.getContact1()));
		dpr.setContact2(StringUtils.saveHtml(dpr.getContact2()));
		dpr.setContact3(StringUtils.saveHtml(dpr.getContact3()));
		dpr.setContact4(StringUtils.saveHtml(dpr.getContact4()));
		dpr.setGeneralNotes(StringUtils.saveHtml(dpr.getGeneralNotes()));
		dpr.setCrewNotes(StringUtils.saveHtml(dpr.getCrewNotes()));
		dpr.setEquipmentNotes(StringUtils.saveHtml(dpr.getEquipmentNotes()));
		dpr.setProductionNotes(StringUtils.saveHtml(dpr.getProductionNotes()));
		for (DprScene sc : dpr.getDprScenes()) {
			sc.setLocation(StringUtils.saveHtml(sc.getLocation()));
		}

		// Fix up crew TimeCards
		for (ReportTime tc : dpr.getCrewTimeCards()) {
			if (tc.getId() != null && tc.getId() < 0) { // temporary value
				tc.setId(null);
			}
		}
		if (removedCalls != null) {
			for (ReportTime tc : removedCalls) {
				dprDAO.delete(tc);
			}
		}

		dpr.setStatus(ReportStatus.UPDATED);
		dpr.setUpdated(new Date());
		dprDAO.merge(dpr);
		// daysScheduled, dprEpisodes, are updated via cascade
		SessionUtils.put(ATTR_DPR_ID, dpr.getId());
		SessionUtils.put("dpr", null);
		SessionUtils.put(ATTR_DPR_EDIT, null);
		super.actionSave(); // will save/restore scroll position
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_ADD_CREW:
				res = actionAddCrew();
				break;
			case ACT_CLEAR_CREW_TIMES:
				res = actionClearCrewTimesOk();
				break;
			case ACT_IMPORT_CREW_TIMES:
				res = actionImportCrewTimesOk();
				break;
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ACT_ADD_CREW:
			case ACT_CLEAR_CREW_TIMES:
			case ACT_IMPORT_CREW_TIMES:
				// no other action required
				break;
		}
		return res;
	}

	/**
	 * Value change listener for the date drop-down, which allows the user
	 * to select a different DPR to view, by date.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenDate(ValueChangeEvent evt) {
		log.debug("");
		try {
			Integer id = (Integer)evt.getNewValue();
			if (id != null) {
				// following code handles UI bug/issue - nested panelSeries weren't refreshed.
				CallSheetRequestBean crb = CallSheetRequestBean.getInstance();
//TODO Ice4		crb.getCrewTable0v().getSavedChildren().clear();
//				crb.getCrewTable1v().getSavedChildren().clear();
//				crb.getCrewTable2v().getSavedChildren().clear();
				crb.getCrewTable0v().getChildren().clear();
				crb.getCrewTable1v().getChildren().clear();
				crb.getCrewTable2v().getChildren().clear();
				// Load the requested DPR and set it up for display
				dpr = DprDAO.getInstance().findById(id);
				saveScroll(); // Save div scroll position; initView will restore it.
				initView(dpr);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Create the List of SelectItem`s containing the dates of the DPRs
	 * which allows the user to switch between displayed reports.
	 * @return List of SelectItem`s, where the value is the database id
	 * of the DPR, and the label is the formatted date.
	 */
	private List<SelectItem> createDateDL() {
		List<SelectItem> dateList = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.MEDIUM_DATE_FORMAT);
		List<Dpr> list = DprDAO.getInstance().findByProject(project);
		for (Dpr pr : list) {
			dateList.add(new SelectItem(pr.getId(), sdf.format(pr.getDate())));
		}
		return dateList;
	}

	/**
	 * The Action method of the Add button on the "add crew call" pop-up.
	 * @return null navigation string
	 */
	private String actionAddCrew() {
		try {
			ContactRoleSelectBean selectBean = ContactRoleSelectBean.getInstance();
			ContactRole newContact = selectBean.getNewContact();

			if (newContact == null) {
				MsgUtils.addFacesMessage("Callsheet.CrewContactMissing", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			addDeptId = selectBean.getDepartmentId();
			log.debug("dept=" + addDeptId);
			boolean done = false;
			outer:
			for (List<DeptTime> list : deptTime) {
				for (DeptTime dt : list) {
					if (dt.getDepartment().getId().equals(addDeptId)) {
						addCrew(newContact, dt);
						done = true;
						break outer;
					}
				}
			}
			if (! done) {
				Department dept = DepartmentDAO.getInstance().findById(addDeptId);
				DeptTime dt = new DeptTime(dept);
				log.debug("new dept=" + dept);
				addCrew(newContact, dt);
				DprUtils.createDeptColumns(dpr, deptTime);
				// following code handles UI bug/issue - nested panelSeries weren't refreshed.
				CallSheetRequestBean crb = CallSheetRequestBean.getInstance();
				if (crb.getCrewTable0() != null) {
					crb.getCrewTable0().getChildren().clear();
					crb.getCrewTable1().getChildren().clear();
					crb.getCrewTable2().getChildren().clear();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Add a new CrewCall entry to the given DeptCall object.  The CrewCall
	 * data is filled in from instance fields 'addDeptId' and 'newContact'.
	 * @param newContact
	 * @param deptCall
	 */
	private void addCrew(ContactRole newContact, DeptTime deptCall) {
		Contact ct = ContactDAO.getInstance().findById(newContact.getContactId());
		ReportTime tc = ReportTimeUtils.createCrewTimeCard(ct, 1, dpr, dpr.getCrewCall());
		tc.setId(0-(int)(Math.random()*10000.0)); // negative value to track unsaved links
		tc.setDepartment(deptCall.getDepartment());
		tc.setRole(newContact.getRoleName());
		tc.setListPriority((short)1); // TODO find proper role list priority?
		deptCall.getTimeCards().add(tc);
		dpr.getCrewTimeCards().add(tc);
		log.debug("added tc=" + tc);
	}

	/**
	 * Creates a Map containing all the crew members already listed in the
	 * current Dpr`s crew-call area, for the purpose of removing them
	 * from proposed additional crew members in the add-crew-call pop-up.
	 *
	 * @return A Map< Integer, List< ContactRole >>, where the key is the department
	 *         id, and the value for each department is a list of ContactRole's
	 *         for the Contacts+Role listed on the callsheet within that department.
	 */
	private Map<Integer, List<ContactRole>> createCrewRoleList() {
		Map<Integer, List<ContactRole>> crewMap = new HashMap<>();
		for (ReportTime tc : dpr.getCrewTimeCards()) {
			if (tc.getDepartment() != null) {
				List<ContactRole> cclist;
				cclist = crewMap.get(tc.getDepartment().getId());
				if (cclist == null) {
					cclist = new ArrayList<>();
					crewMap.put(tc.getDepartment().getId(), cclist);
				}
				cclist.add(new ContactRole(tc.getContact().getId(), tc.getRole()));
			}
		}
		return crewMap;
	}

	public Dpr getDpr() {
		return dpr;
	}
	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
	}

	/** See {@link #dprId}. */
	public Integer getDprId() {
		return dprId;
	}
	/** See {@link #dprId}. */
	public void setDprId(Integer dprId) {
		this.dprId = dprId;
	}

	/**
	 * Called from edit.jsf page during rendering to ensure we are in Edit mode.
	 * This fixes a problem that could happen if the user used the browser
	 * back/forward buttons to navigate to the Edit page.
	 *
	 * @return false
	 */
	public boolean getEdit() {
		if (! editMode) {
			editMode = true;
			SessionUtils.put(ATTR_DPR_EDIT, true);
			if (dpr == null) {
				dpr = new Dpr();
				dpr.setDate(new Date());
			}
			initEdit();
		}
		return false;
	}

	/**
	 * Called from view.jsf page during rendering to ensure we are not in Edit
	 * mode. This fixes a problem that could happen if the user used the browser
	 * back/forward buttons to navigate to the View page.
	 *
	 * @return false
	 */
	public boolean getView() {
		if (editMode) {
			editMode = false;
			SessionUtils.put(ATTR_DPR_EDIT, null);
		}
		return false;
	}

	/** See {@link #dateDL}. */
	public List<SelectItem> getDateDL() {
		return dateDL;
	}
	/** See {@link #dateDL}. */
	public void setDateDL(List<SelectItem> dateDL) {
		this.dateDL = dateDL;
	}

	/** See {@link #editMode}. */
	public boolean getEditMode() {
		return editMode;
	}
	/** See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/** See {@link #addDeptId}. */
	public Integer getAddDeptId() {
		return addDeptId;
	}
	/** See {@link #addDeptId}. */
	public void setAddDeptId(Integer addDeptId) {
		this.addDeptId = addDeptId;
	}

	public List<FilmStock> getFilmStockList() {
		return filmStockList;
	}
	public void setFilmStockList(List<FilmStock> filmStockList) {
		this.filmStockList = filmStockList;
	}

	/** See {@link #deptTime}. */
	public List<DeptTime>[] getDeptTime() {
		return deptTime;
	}
	/** See {@link #deptTime}. */
	public void setDeptTime(List<DeptTime>[] deptTime) {
		this.deptTime = deptTime;
	}

}