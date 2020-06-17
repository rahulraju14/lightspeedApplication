// File Name: HeaderViewBean.java
package com.lightspeedeps.web.report;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ContactRole;
import com.lightspeedeps.type.OtherCallType;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.CallSheetUtils;
import com.lightspeedeps.web.contact.ContactRoleSelectBean;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.Scroller;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for Call Sheet View AND Call Sheet Edit pages.  Since we are
 * using ICEFaces extended-lifecycle, an instance of this bean will exist as
 * long as the user stays on the same page.  But when the user switches from
 * View to Edit, or Edit to View, then a new instance of the bean is created.
 */
@ManagedBean
@ViewScoped
public class CallSheetViewBean extends Scroller implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 6192033349682218070L;

	private static final Log log = LogFactory.getLog(CallSheetViewBean.class);

	/** Unique integer code used for PopupBean actions. */
	private final static int ACT_MAKE_FINAL = 10;
	private final static int ACT_ADD_CREW = 11;

	// Fields

	/** The Callsheet currently displayed. */
	private Callsheet callsheet;

	/** The Production the callsheet is for. */
	private Production production;

	/** The database id of the displayed callsheet, or the one just selected
	 * by the user from the date drop-down list. */
	private Integer callsheetId;

	/** The Unit associated with the current callsheet. */
	private Unit unit;

	/** Database id of the Department for which the user is adding a new
	 * Crew entry. */
	private Integer addDeptId;

	/** CallNote data for display & update by JSP, indexed by "section number". */
	private String callNotes[];

	/** true iff edit page is being displayed */
	private boolean editMode;

	/** True iff the "make final?" pop-up is to be displayed. */
	private boolean showMakeFinal;

	/** True iff user requested that call times be sent as part of Make Final process,
	 * or as a result of changes to a published call sheet. */
	private boolean sendCallTimes;

	/** True iff user requested that call sheet PDFs be sent as part of Make Final process,
	 * or as a result of changes to a published call sheet. */
	private boolean sendCallSheets;

	/** True iff user requested that location report PDFs be sent as part of Make Final process,
	 * or as a result of changes to a published call sheet. */
	private boolean sendLocationReports;

	/** True iff the "send change notifications" pop-up should be displayed. */
	private boolean showChanges;

	/** True iff the notifications pop-up should display the check-box for
	 * sending notifications to deleted cast and/or crew. */
	private boolean showChangedDeleted;

	/** True iff the notifications pop-up should display the check-box for
	 * sending notifications to newly added cast and/or crew. */
	private boolean showChangedAdded;

	/** True iff the notifications pop-up should display the check-box for
	 * sending notifications to cast and/or crew with changed call times. */
	private boolean showChangedCalltimes;

	/** True iff the notifications pop-up should display the check-box for
	 * sending notifications about changed locations. */
	private boolean showChangedLocations;

	/** True iff the notifications pop-up should display the check-box for
	 * sending notifications about changed scenes. */
	private boolean showChangedScenes;

	/** Backing field for the checkbox choosing whether or not to send
	 * notifications to deleted cast and/or crew. */
	private boolean sendToDeleted;

	/** Backing field for the checkbox choosing whether or not to send
	 * notifications to newly added cast and/or crew. */
	private boolean sendToAdded;

	/** Backing field for the checkbox choosing whether or not to send
	 * notifications about scene changes. */
	private boolean sendScenes;

	private boolean sendLocations;

	/** Backing field for the checkbox choosing whether or not to send
	 * a PDF of the updated call sheet to everyone on the call sheet. */
	private boolean sendPdf;

	/** The number of cast and/or crew deleted from the call sheet (in
	 * the current edit cycle). */
	private int numChangedDeleted;

	/** The number of cast and/or crew added to the call sheet (in
	 * the current edit cycle). */
	private int numChangedAdded;

	/** The number of cast and/or crew whose call time changed (in
	 * the current edit cycle). */
	private int numChangedCalltimes;

	/** A List of objects holding all the data necessary to send out
	 * all notifications generated during the current edit cycle. The data is
	 * filled in during the save processing, then used to generate notifications
	 * based on the current user's selection of which notifications they want
	 * delivered. */
	private List<ChangeNotice> changeNotices;

	/** True if the "Add crew call" pop-up is to be displayed. */
	private boolean showAddCrewCall;

	/** A Collection of the CrewCall's that have been deleted during the
	 * current edit session. */
	private Collection<CrewCall> removedCalls;

	/** All the CrewCall times that exist at the beginning of an edit
	 * session, kept as a Map from the Contact's id to their call time.
	 * Used during save processing to generate notifications for CrewCall
	 * entries that were added, deleted, or changed. */
	private Map<Integer,Date> oldCallTimes;

	/** The three List`s of DeptCall entries corresponding to the three columns
	 * of crew on the "back" page of the Callsheet. */
	@SuppressWarnings("unchecked")
	private List<DeptCall> deptCalls[] = new List[3];

	/** The catering information that gets displayed with the Catering
	 * crew department entry. */
	private CateringLog cateringLogs[] = new CateringLog[1];

	/** The drop-down list of dates of finalized (published) Callsheet`s, allowing
	 * the user to switch between displayed Callsheet`s. */
	private List<SelectItem> dateDL;

	private boolean showTimeOptions;
	private boolean optionCrewTime;

	private Date optionTime = new Date();

	/** True if the call sheet is still considered "current" for the purposes of
	 * sending notifications. */
	private boolean callsheetCurrent;

	/** Current date and time */
	private final Date currentTime = new Date(); // used in pushCallTimes

//	private Contact director;
//	private Contact producer;
//	private Contact lineProducer;

	/** The string which determines the header nav/sub-nav displayed while on the Call sheet view page. */
	private String headerTabId = HeaderViewBean.REPORTS_MENU_REPORTS;

	private transient CallsheetDAO callsheetDAO;
	private transient ContactDAO contactDAO;

	/** Constructor */
	public CallSheetViewBean() {
		log.debug("" + this);
		try {
			setScrollable(true);
			Project project = SessionUtils.getCurrentProject();
			callsheet = null;
			dateDL = new ArrayList<>();
			Integer id = SessionUtils.getInteger(Constants.ATTR_CALL_SHEET_ID);
			if (id != null) {
				callsheet = getCallsheetDAO().findById(id);
				if (callsheet != null && ! callsheet.getProject().getId().equals(project.getId())) {
					// different project - allow for cross-episode, if in same production
					Production prod = project.getProduction();
					if (! prod.getType().getCrossProject() ||
							! callsheet.getProject().getProduction().equals(prod)) {
						id = null;
						callsheet = null;
						SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, null);
					}
				}
			}
			if (callsheet == null) {
				log.debug("no callsheet id in session, finding default");
				id = CallSheetUtils.getCurrentCallsheetId();
				if (id != null) {
					callsheet = getCallsheetDAO().findById(id);
				}
				if (callsheet == null) {
					callsheet = findCallsheet();
				}
			}
			if (callsheet != null) {
				id = callsheet.getId();
				log.debug("got callsheet, id=" + id);
			}
			else {
				log.info("no callsheet available, project=" + project);
			}

			setHeaderTabId(SessionUtils.getString(Constants.ATTR_HEADER_TAB_ID, HeaderViewBean.SCHEDULE_MENU_CALLSHEET_VIEW));
			SessionUtils.put(Constants.ATTR_HEADER_TAB_ID,null);

			initForCallsheet();
			dateDL = createDateDL();
		}
		catch (Exception e) {
			EventUtils.logError("Initializing CallSheetViewBean failed Exception: ", e);
			callsheet = new Callsheet(); // blank out view
		}
	}

	private Callsheet findCallsheet() {
		Callsheet csheet = null;
		Contact user = SessionUtils.getCurrentContact();
		Project proj = user.getProject();
		List<Callsheet> list;
		for (Unit u : proj.getUnits()) {
			if (CallSheetUtils.getInUnit(user, u)) {
				csheet = null;
				list = findCallsheetsForUnit(u);
				if (list != null && list.size() > 0) {
					csheet = list.get(0);
					break;
				}
			}
		}
		return csheet;
	}

	/**
	 * Create the List of SelectItem`s containing the dates of the available
	 * Callsheet`s, including the requested one, which allows the user to switch
	 * between displayed Callsheet`s.  For non-privileged users, it only includes
	 * PUBLISHED callsheets.
	 *
	 * @return the non-null List as described above.
	 */
	private List<SelectItem> createDateDL() {
		List<SelectItem> dateList = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.MEDIUM_DATE_FORMAT);
		List<Callsheet> csList = findCallsheetsForUnit(unit);
		// for cross-project productions, the requested CS might not be in this unit;
		// keep track if it's been added.
		boolean foundCurrent = (callsheet == null || callsheetId == null); // skip checks if have no CS!
		for (Callsheet cs : csList) {
			if (! foundCurrent) {
				if (callsheetId.equals(cs.getId())) { // this is the one we want
					foundCurrent = true;
				}
				else if (cs.getDate().before(callsheet.getDate())) {
					// haven't found it yet, and should have, due to date order; add it.
					dateList.add(new SelectItem(callsheetId, sdf.format(callsheet.getDate())));
					foundCurrent = true;
				}
			}
			dateList.add(new SelectItem(cs.getId(), sdf.format(cs.getDate())));
		}
		if (! foundCurrent) {
			// never found it; happens if requested CS is earlier than all CS's in this unit
			dateList.add(new SelectItem(callsheetId, sdf.format(callsheet.getDate())));
		}
		return dateList;
	}

	/**
	 * Get a List of Callsheets that apply to the current Unit, in descending
	 * date order.
	 *
	 * @param u The Unit that the callsheets should be related to.
	 * @return A non-null, but possibly empty, List of Callsheet`s.
	 */
	private List<Callsheet> findCallsheetsForUnit(Unit u) {
		List<Callsheet> csList;
		if (AuthorizationBean.getInstance().hasPageField(Constants.PGKEY_VIEW_CS_LIST)) {
			csList = getCallsheetDAO().findByUnit(u);
		}
		else {
			csList = getCallsheetDAO().findByUnitAndStatus(u, ReportStatus.PUBLISHED);
		}
		return csList;
	}

	public void listenDate(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		if (id != null) {
			initForCallsheet(id);
		}
	}

	public void listenCustomRole(ValueChangeEvent evt) {
		//String role = (String)evt.getNewValue();
		checkBlankCrewCall();
	}

	private void checkBlankCrewCall() {
		return; // rev 2770 temporarily disable user-entered crew calls
/*		int n = callsheet.getDeptCalls().size();
		DeptCall dc = null;
		if (n > 0) {
			dc = callsheet.getDeptCalls().get(n-1);
		}
		if (n == 0 || dc.getDepartment() != null) {
			dc = CallSheetUtils.createUserDeptCall(callsheet);
			callsheet.getDeptCalls().add(dc);
			n = callsheet.getDeptCalls().size();
		}
		List<CrewCall> ccs = dc.getCrewCalls();
		if (ccs.size() == 0) {
			ccs.add(new CrewCall(dc, 0, null, "", "", null));
		}
		if (ccs.size() == 1) {
			ccs.add(new CrewCall(dc, ccs.get(0).getLineNumber()+1, null, "", "", null));
		}
		if (! StringUtils.isEmpty(ccs.get(ccs.size()-1).getRoleName())) {
			ccs.add(new CrewCall(dc, ccs.get(ccs.size()-1).getLineNumber()+1,
					null, "", "", null));
		}
*/	}

	private void initForCallsheet(Integer id) {
		log.debug("id=" + id);
		if (id != null) {
			callsheet = getCallsheetDAO().findById(id);
			if (callsheet != null) {
				SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, id);
			}
		}
		else {
			callsheet = null;
		}
		initForCallsheet();
//		ListView.addClientResize();
	}

	/**
	 * Initialize our fields that are dependent on which callsheet is being viewed.
	 */
	private void initForCallsheet() {
		removedCalls = new ArrayList<>();

		/* Get Other Call List By CallSheet ID */
		callNotes = new String[50];
		if (callsheet != null) {
			callsheetId = callsheet.getId();
			production = callsheet.getProject().getProduction();
			if (production.getLogo() != null) {
				production.getLogo().getContent(); // force initialization
			}
			Project project = SessionUtils.getCurrentProject();
			if (callsheet.getCateringLog() == null) { // a really old callsheet (prior to rev 4394)
				callsheet.setCateringLog(new CateringLog());
			}
			cateringLogs[0] = callsheet.getCateringLog();
			unit = UnitDAO.getInstance().findByProjectAndNumber(project,callsheet.getUnitNumber());
			// Determine dates for 'advance scene call' entries (based on shooting day number)
			// For callsheets created in v2.2, the date has already been calculated and stored
			if (callsheet.getAdvanceSceneCalls() != null && callsheet.getAdvanceSceneCalls().size() > 0
					&& callsheet.getAdvanceSceneCalls().get(0).getDate() == null) {
				ScheduleUtils scUtil = new ScheduleUtils(unit);
				int numdays = scUtil.getShootingDatesList().size();
				for (SceneCall sc : callsheet.getAdvanceSceneCalls()) {
					if (sc.getDayNumber() <= numdays) {
						sc.setDate(scUtil.getShootingDatesList().get(sc.getDayNumber()-1));
					}
				}
			}
			restoreScrollFromSession(); // try to maintain scrolled position
		}
		else {
			callsheetId = null;
			unit = SessionUtils.getCurrentProject().getMainUnit();
		}
//		if (SessionUtils.getBoolean(ATTR_CALLSHEET_EDIT, false)) {
//			setupEdit();
//		}
		if (callsheet != null) {
			for (CallNote cn : callsheet.getCallNotes()) {
				callNotes[cn.getSection()] = cn.getBody();
			}
			CallSheetUtils.createDeptColumns(callsheet, deptCalls);
		}
	}

	@Override
	public String actionEdit() {
		log.debug("");
		super.actionEdit();
		return HeaderViewBean.REPORTS_MENU_CALLSHEET_EDIT;
	}

	public boolean getLoadEdit() {
		if (! editMode) { // have not run Edit mode initialization yet...
			setupEdit();
			editMode = true; // remains true until we leave the Edit page
		}
		return false;
	}

	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		return HeaderViewBean.REPORTS_MENU_CALLSHEET_VIEW;
	}

	/**
	 * When we are displaying the Edit mode page, we need to modify and move
	 * some data items.
	 */
	private void setupEdit() {
		callsheet.setExecutives(StringUtils.editHtml(callsheet.getExecutives()));

		Set<CallNote> notes = callsheet.getCallNotes();
		for (CallNote cn : notes) {
			callNotes[cn.getSection()] = StringUtils.editHtml(cn.getBody());
		}
		// If some call note fields are unused, provide empty Strings for framework
		// to fill in during edit.
		for (int i : Constants.CN_SECTION_IN_USE) {
			if (callNotes[i] == null) {
				callNotes[i] = "";
			}
		}

		keepCallTimes(callsheet); // make copy of existing crew call times
		checkBlankCrewCall();

		// provide empty "other call" line items
		for (int i = 1900; i < 1902; i++) {
			OtherCall oc = new OtherCall(callsheet, OtherCallType.ATMOSPHERE, i, "", 0, callsheet.getCallTime());
			callsheet.getOtherCalls().add(oc);
		}
//		SessionUtils.put(ATTR_CALLSHEET_EDIT, false);
	}

	/**
	 * Finalize Callsheet - Action method on the "Make Final" button on the call
	 * sheet View page. Here we just put up either a confirmation dialog, when
	 * notifications are turned off, or the Make Final dialog that has option
	 * selections for the user.
	 */
	public String actionMakeFinal() {
		log.debug("");
		if (SessionUtils.getProduction().getNotify() &&
				SessionUtils.getCurrentProject().getNotifying()) {
			showMakeFinal = true;
		}
		else {
			PopupBean.getInstance().show(
					this, ACT_MAKE_FINAL,
					"CallSheet.MakeFinal.");
		}
		return null;
	}

	/**
	 * Action method for the Cancel button on the Make Final
	 * (with notifications) dialog pop-up.
	 */
	public String actionMakeFinalCancel() {
		showMakeFinal = false;
		return null;
	}

	/**
	 * Finalize Callsheet - Called from the confirmation dialog (put up after
	 * user pressed the "Make Final" button on the call sheet View page) for
	 * projects running WITHOUT notifications. (Projects with notifications
	 * turned on will use the {@link #actionMakeFinalOk()} method.)
	 */
	private String actionMakeFinalConfirm() {
		makeFinal(); // update status & archive a copy
		return null;
	}

	/**
	 * Finalize Callsheet - Action method for the Ok button on the "Make Final"
	 * pop-up that contains notification options. Called via JSP. This updates
	 * the status and archives the callsheet, and then, depending on the user's
	 * choices, may push Call time notifications to all personnel on the
	 * callsheet and/or do distribution of the call sheet document. This method
	 * is only called for projects with notifications turned on; if
	 * notifications are turned off, a simple confirmation dialog appears, and
	 * the OK is processed by the {@link #actionMakeFinalConfirm()} method.
	 */
	public String actionMakeFinalOk() {
		try {
			showMakeFinal = false;
			makeFinal(); // update status & archive a copy
			if (sendCallTimes) {
				pushCallTimes(); // send cast & crew call-time notifications
			}
			if (sendCallSheets) {
				// generate & email call sheets & optionally location reports
				PrintDailyReportBean rb = PrintDailyReportBean.getInstance();
				rb.setReportId(getCallsheet().getId());
				rb.actionPrintAndSendCallsheet(sendLocationReports);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Finalize Callsheet - This updates the report status to "Published" and
	 * archives a copy of the callsheet. These actions are always done when
	 * finalizing a callsheet, regardless of notification settings.
	 *
	 */
	private void makeFinal() {
		try {
			getCallsheet().setStatus(ReportStatus.PUBLISHED);
			getCallsheetDAO().attachDirty(getCallsheet());
			log.debug("Callsheet made final");
			// Archive a copy when the callsheet is first published:
			PrintDailyReportBean rb = PrintDailyReportBean.getInstance();
			rb.archiveCallsheetAsync(getCallsheet());
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public String actionOpenAddCrewCall() {
		showAddCrewCall = true;
		ContactRoleSelectBean selectBean = ContactRoleSelectBean.getInstance();
		selectBean.show(this, ACT_ADD_CREW, "Callsheet.AddCrew.Title", "Callsheet.AddCrew.Message");
		selectBean.setDepartmentId(addDeptId);
		selectBean.setUnitId(unit.getId());
		selectBean.setOmitContactRoleMap(createCrewRoleList());
		return null;
	}

	/**
	 * The Action method of the close/cancel button on the "add crew call"
	 * pop-up. Just closes the pop-up. Called from our confirmCancel() method.
	 *
	 * @return null navigation string
	 */
	private String actionCancelAddCrewCall() {
		showAddCrewCall = false;
		return null;
	}

	/**
	 * The Action method of the Add button on the "add crew call" pop-up. Called
	 * via our {@link #confirmOk(int)} method.
	 *
	 * @return null navigation string
	 */
	private String actionAddCrewCall() {
		try {
			ContactRoleSelectBean selectBean = ContactRoleSelectBean.getInstance();
			ContactRole newContact = selectBean.getNewContact();

			if (newContact == null) {
				MsgUtils.addFacesMessage("Callsheet.CrewContactMissing", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			showAddCrewCall = false;
			addDeptId = selectBean.getDepartmentId();
			log.debug("dept=" + addDeptId);
			boolean done = false;
			for (DeptCall dc : callsheet.getDeptCalls()) {
				if (dc.getDepartment().getId().equals(addDeptId)) {
					addCrewCall(newContact, dc);
					done = true;
					break;
				}
			}
			if (! done) {
				Department dept = DepartmentDAO.getInstance().findById(addDeptId);
				DeptCall dc = new DeptCall(callsheet, dept);
				log.debug("new dept=" + dept);
				addCrewCall(newContact, dc);
				callsheet.getDeptCalls().add(dc);
				Collections.sort(callsheet.getDeptCalls());
				CallSheetUtils.createDeptColumns(callsheet, deptCalls);
				// following code handles UI bug/issue - nested panelSeries weren't refreshed.
				CallSheetRequestBean crb = CallSheetRequestBean.getInstance();
//TODO Ice4		crb.getCrewTable0().getSavedChildren().clear();
				crb.getCrewTable0().getChildren().clear();
				crb.getCrewTable1().getChildren().clear();
				crb.getCrewTable2().getChildren().clear();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for the delete icon on the individual crew-call entries.
	 * The 'removeCrewCallId' and 'addDeptId' fields should already be set upon
	 * entry due to f:setPropertyActionListener tags in the JSP.
	 * @return null navigation string
	 */
	public String actionDeleteCrewCall(CrewCall selectedCrewCall) {
		try {
			DeptCall dc = selectedCrewCall.getDeptCall();

			if (dc != null) {
				if(addDeptId == null) {
					// dept id = null means it's the user-customized entry -- last one
					dc = callsheet.getDeptCalls().get(callsheet.getDeptCalls().size()-1);
				}
				List<CrewCall> calls = dc.getCrewCalls();
				calls.remove(selectedCrewCall);
				if (selectedCrewCall.getId() > 0) { // wasn't just added
					removedCalls.add(selectedCrewCall); // remember to delete it from database
				}
			}
			if (addDeptId == null) {
				checkBlankCrewCall();
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		return null;
	}

	/**
	 * Add a new CrewCall entry to the given DeptCall object.  The CrewCall
	 * data is filled in from instance fields 'addDeptId' and 'newContact'.
	 * @param newContact
	 * @param deptCall
	 */
	private void addCrewCall(ContactRole newContact, DeptCall deptCall) {
		CrewCall cc = new CrewCall();
		cc.setId(0-(int)(Math.random()*10000.0)); // negative value to track unsaved links
		Contact ct = getContactDAO().findById(newContact.getContactId());
		cc.setName(ct.getDisplayName());
		cc.setRoleName(newContact.getRoleName());
		cc.setContactId(newContact.getContactId());
		cc.setCount(1);
		cc.setTime(callsheet.getCallTime());
		cc.setDeptCall(deptCall);
		int lineNumber = 0;
		if (deptCall.getCrewCalls().size() > 0) {
			// get the last entry's line number, plus 1.
			lineNumber = deptCall.getCrewCalls().get(deptCall.getCrewCalls().size()-1).getLineNumber() + 1;
		}
		cc.setLineNumber(lineNumber);
		deptCall.getCrewCalls().add(cc);
		log.debug("added cc=" + cc);
	}

	/**
	 * Creates a Map containing all the crew members already listed in the
	 * current call sheet's crew-call area, for the purpose of removing them
	 * from proposed additional crew members in the add-crew-call pop-up.
	 *
	 * @return A Map< Integer, List< ContactRole >>, where the key is the department
	 *         id, and the value for each department is a list of ContactRole's
	 *         for the Contacts+Role listed on the callsheet within that department.
	 */
	private Map<Integer, List<ContactRole>> createCrewRoleList() {
		Map<Integer, List<ContactRole>> crewMap = new HashMap<>();
		for (DeptCall dc : callsheet.getDeptCalls()) {
			if (dc.getDepartment() != null) {
				List<ContactRole> cclist = new ArrayList<>();
				for (CrewCall cc : dc.getCrewCalls()) {
					cclist.add(new ContactRole(cc.getContactId(),cc.getRoleName()));
				}
				crewMap.put(dc.getDepartment().getId(), cclist);
			}
		}
		return crewMap;
	}

	/**
	 * The Action method for the "Save" button on the Call sheet Edit page. Save
	 * any changes, and put up the "notifications" dialog if appropriate.
	 * (Notifications may be generated if the call sheet is in the "Published"
	 * state.)
	 *
	 * @return navigation string for callsheet view page, or a null navigation
	 *         string if an error occurs so the user stays on the Edit page.
	 */
	@Override
	public String actionSave() {
		log.debug(""+this);
		super.actionSave();
		try {
			changeNotices = new ArrayList<>();
			showChangedLocations = false;
			showChangedScenes = false;
			numChangedDeleted = 0;
			numChangedAdded = 0;
			numChangedCalltimes = 0;
			save();
			SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, getCallsheet().getId());
			if (getCallsheet().getStatus() == ReportStatus.PUBLISHED) {
				PrintDailyReportBean rb = PrintDailyReportBean.getInstance();
				rb.archiveCallsheetAsync(getCallsheet());
			}
			if (changeNotices != null) {
				showChanges = true;
				showChangedDeleted = (numChangedDeleted != 0);
				showChangedAdded = (numChangedAdded != 0);
				showChangedCalltimes = (numChangedCalltimes != 0);
				return null;
			}
		}
		catch (Exception e) {
			EventUtils.logError("callsheet save failed: ", e);
			MsgUtils.addFacesMessage("Callsheet.SaveFailed", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		return HeaderViewBean.REPORTS_MENU_CALLSHEET_VIEW;
	}

	/**
	 * Action method for the "OK" button on the "Changes notifications" dialog.
	 * We will send notifications based on the check marks that the user set
	 * in the dialog box.
	 *
	 * @return navigation string for the Callsheet View page
	 */
	public String actionChangesOk() {
		showChanges = false;
		try {
			sendNotifications();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return HeaderViewBean.REPORTS_MENU_CALLSHEET_VIEW;
	}

	/**
	 * Save CallSheet.  Ensure time-of-day fields reflect proper date; update line-breaks
	 * in multi-line text fields.  Check if notifications need to be generated.
	 */
	private void save() {
		Date csDate = getCallsheet().getDate();
//		int n = csDate.getTimezoneOffset();
//		int m = callsheet.getTimeZone().getOffset(csDate.getTime()) / 60000;
		getCallsheet().setCallTime(CalendarUtils.sameTimeAs(getCallsheet().getCallTime(), csDate));
		getCallsheet().setShootTime(CalendarUtils.sameTimeAs(getCallsheet().getShootTime(), csDate));
		getCallsheet().setSunrise(CalendarUtils.sameDateAs(csDate, getCallsheet().getSunrise(), callsheet.getTimeZone()));
		getCallsheet().setSunset(CalendarUtils.sameDateAs(csDate, getCallsheet().getSunset(), callsheet.getTimeZone()));

		checkNullValues(); // change nulls to zeroes, etc. if null not allowed in db

		Calendar currentDate = new GregorianCalendar(ApplicationUtils.getTimeZoneStatic());
		// we want to compare call time to a time 'n' (24?) hours earlier...
		currentDate.add(Calendar.HOUR_OF_DAY, - Constants.CS_WITHIN_HOURS_DISTRIBUTE);
		callsheetCurrent = (callsheet.getCallTime() != null) && callsheet.getCallTime().after(currentDate.getTime());
		log.debug(callsheetCurrent + ", " + callsheet.getCallTime() + ", " + currentDate.getTime());

		CallSheetUtils.calculateTotalPages(getCallsheet());

		for (CastCall castCall : getCallsheet().getCastCalls()) {
			if (castCall.getPickup() != null) {
				castCall.setPickup(CalendarUtils.sameTimeAs(castCall.getPickup(), csDate));
			}
			if (castCall.getMakeup() != null) {
				castCall.setMakeup(CalendarUtils.sameTimeAs(castCall.getMakeup(), csDate));
			}
			if (castCall.getOnSet() != null) {
				castCall.setOnSet(CalendarUtils.sameTimeAs(castCall.getOnSet(), csDate));
			}
		}

		List<OtherCall> ocList = new ArrayList<>();
		int line = 0;
		for (OtherCall oc : callsheet.getOtherCalls()) {
			if (oc.getDescription().trim().length() > 0) {
				oc.setLineNumber(line);
				ocList.add(oc);
				if (oc.getTime() != null) {
					oc.setTime(CalendarUtils.sameTimeAs(oc.getTime(), csDate));
				}
				line++;
			}
		}
		callsheet.setOtherCalls(ocList);

		callsheet.setExecutives(StringUtils.saveHtml(callsheet.getExecutives()));

		// Compare callsheet's existing List of CallNotes to the array backing the UI
		String note;
		//log.debug("note cnt=" + callsheet.getCallNotes().size());
		for (Iterator<CallNote> iter = callsheet.getCallNotes().iterator(); iter.hasNext(); ) {
			CallNote cn = iter.next();
			note = callNotes[cn.getSection()];
			if (note != null && note.trim().length() > 0) {
				// existing entry; text may have changed, but is non-blank
				note = StringUtils.saveHtml(note);
				cn.setBody(note);
				//log.debug("note sect=" + cn.getSection() + ", text=" + note);
				callNotes[cn.getSection()] = null; // finished processing this entry
			}
			else { // used to have data, now it's blank -- delete it.
				iter.remove();
				//log.debug("note #"+cn.getSection());
				getCallsheetDAO().delete(cn);
			}
		}
		// Now loop to add any new notes to database & List
		for (int i : Constants.CN_SECTION_IN_USE) {
			if (callNotes[i] != null) { // did not exist in db before
				CallNote cn = createCallNote(i);
				if (cn != null) { // has some text, add to List
					callsheet.getCallNotes().add(cn);
				}
			}
		}

		// cascade didn't seem to work for deleting CrewCalls, so we do it directly.
		for (CrewCall cc : removedCalls) {
			getCallsheetDAO().delete(cc);
		}
		removedCalls.clear();

		for (Iterator<DeptCall> iter = getCallsheet().getDeptCalls().iterator(); iter.hasNext(); ) {
			DeptCall deptCall = iter.next();
			if (deptCall.getCrewCalls().size() == 0) {
				if (! deptCall.getDeptName().contains("Cater")) {
					// Do NOT delete Catering department (used to show CateringLog)
					iter.remove();
					// DeptCall instance will get deleted via cascade.
					// (explicit delete caused Hibernate errors; rev 2.2.4919)
				}
			}
			else {
				for (CrewCall crewCall : deptCall.getCrewCalls()) {
					if (crewCall.getTime() != null) {
						crewCall.setTime(CalendarUtils.sameTimeAs(crewCall.getTime(), csDate));
					}
					if (crewCall.getId() != null && crewCall.getId() < 0) { // temporary value
						crewCall.setId(null); // cascading save (persist) will set id
					}
				}
			}
		}

		// Check for notifications required...
		if (callsheetCurrent && (callsheet.getStatus() == ReportStatus.PUBLISHED)) {
			Callsheet oldCs = getCallsheetDAO().findById(callsheet.getId());
			checkCalltimeChanges(oldCs);
			checkSceneChanges(oldCs);
			getCallsheetDAO().evict(oldCs);
		}
		else { // ignore any change events & don't put up notifications dialog box
			changeNotices = null;
		}

		if (callsheet.getStatus() == ReportStatus.CREATED) {
			callsheet.setStatus(ReportStatus.UPDATED);
		}
		callsheet.setUpdated(new Date());

		getCallsheetDAO().merge(getCallsheet());
		Address addr = getCallsheet().getProject().getProduction().getAddress();
		addr = AddressDAO.getInstance().merge(addr);
	}

	/**
	 * Create a CallNote from the 'ix-th' entry in the callNotes field, unless
	 * that field is empty.
	 *
	 * @param ix
	 * @return Null if callNotes[ix] is empty (null, 0-length, or blanks), else
	 *         a CallNote containing the text from callNotes[ix], converted to
	 *         HTML.
	 */
	private CallNote createCallNote(int ix) {
		CallNote cn = null;
		if (callNotes[ix] != null && callNotes[ix].trim().length() > 0) {
			cn = new CallNote(callsheet, ix, "", 0, StringUtils.saveHtml(callNotes[ix]));
		}
		return cn;
	}

	/**
	 * Change null-valued fields that are not allowed to have null values in the
	 * database.
	 */
	private void checkNullValues() {
		for (OtherCall oc : callsheet.getOtherCalls()) {
			if (oc.getCount() == null) {
				oc.setCount(0);
			}
		}

	}

	/**
	 * Save a copy of the existing crew call times. The data will be used when saving
	 * to see if any notifications must be sent out for call time changes.
	 * @param oldCs
	 */
	private void keepCallTimes(Callsheet oldCs) {
		oldCallTimes = new HashMap<>(20);
		Integer contactId;
		for (DeptCall deptCall : oldCs.getDeptCalls()) {
			for (CrewCall crewCall : deptCall.getCrewCalls()) {
				if (crewCall.getTime() != null && crewCall.getName() != null &&
						crewCall.getName().trim().length() > 0) {
					contactId = findContactId(crewCall.getContactId(), crewCall.getName());
					if (contactId != null) {
						oldCallTimes.put(contactId, crewCall.getTime());
					}
				}
			}
		}
	}

	/**
	 * Check for call-time changes between the old callsheet and the new (unsaved)
	 * one.  Notifications are issued for changes in time for a user who is listed on
	 * both sheets, and for added or removed users.  Handles both cast and crew members.
	 * @param oldCs The previous version of the callsheet.
	 */
	private void checkCalltimeChanges(Callsheet oldCs) {
		if (oldCallTimes == null) {
			// may happen if a prior Save failed & user tries to Save again.
			return;
		}
		// Do the cast members first.
		// Loop through objects in old callsheet, putting into map for direct access
		Set<Integer> foundIds = new HashSet<>();
		Map<Integer,CastCall> oldCastTimes = new HashMap<>(20);
		Integer contactId;
		for (CastCall castCall : oldCs.getCastCalls()) {
			if (castCall.getOnSet() != null && castCall.getName() != null &&
					castCall.getName().trim().length() > 0) {
				contactId = findContactId(castCall.getContactId(), castCall.getName());
				if (contactId != null) {
					oldCastTimes.put(contactId, castCall);
				}
			}
		}
		CastCall oldCc;
		// Now loop on new cast; entry in both gets checked for time change; entry
		// not in old one gets sent "added" notification.
		for (CastCall castCall : getCallsheet().getCastCalls()) {
			if (castCall.getOnSet() != null && castCall.getName() != null) {
				String name = castCall.getName().trim();
				if (name.length() > 0) {
					contactId = findContactId(null, name);// force lookup by name
					if (contactId != null) {
						castCall.setContactId(contactId); // in case it changed
						oldCc = oldCastTimes.get(contactId);
						if (oldCc != null) {
							if ( (CalendarUtils.compare(oldCc.getPickup(), castCall.getPickup()) != 0) ||
									(CalendarUtils.compare(oldCc.getMakeup(), castCall.getMakeup()) != 0) ||
									(CalendarUtils.compare(oldCc.getOnSet(), castCall.getOnSet()) != 0) ) {
								// at least one of the times changed
								log.debug("Time change: cast name="+ name + ", new=" + castCall.toString() +
										", old=" + oldCc.toString());
								saveCastCalltimeNotification(castCall.getContactId(), castCall, false);
							}
							foundIds.add(contactId);
							oldCastTimes.remove(contactId); // so we know it was in both the old & new one
						}
						else if (! foundIds.contains(contactId)) {
							// the foundIds check prevents sending "Added" msg if someone is listed twice
							saveCastCalltimeNotification(contactId, castCall, true);
						}
					}
				}
			}
		}
		// Entries left in the map are ones that were in old, but NOT in new
		for (Integer id : oldCastTimes.keySet()) {
			// So send a "removed" notification...
			saveCastCalltimeNotification(id, null, false);
		}
		oldCastTimes.clear();
		oldCastTimes = null;
		foundIds.clear();

		// Now do the same for crew members
		Date oldDate;
		for (DeptCall deptCall : getCallsheet().getDeptCalls()) {
			//log.debug("dept: " + deptCall);
			for (CrewCall crewCall : deptCall.getCrewCalls()) {
				if (crewCall.getTime() != null && crewCall.getName() != null) {
					String name = crewCall.getName().trim();
					if (name.length() > 0) {
						contactId = findContactId(crewCall.getContactId(), name);
						if (contactId != null) {
							oldDate = oldCallTimes.get(contactId);
							if (oldDate != null) {
								if ( oldDate.getTime() != crewCall.getTime().getTime()) { // time changed
									log.debug("Time change: crew name="+ name + ", crewcall=" + crewCall.getTime() +
											", old time=" + oldDate);
									saveCrewCalltimeNotification(crewCall.getContactId(), crewCall, false);
								}
								foundIds.add(contactId);
								oldCallTimes.remove(contactId); // so we know it was in both the old & new one
							}
							else if (! foundIds.contains(contactId)) {
								// the foundIds check prevents sending "Added" msg if someone is listed twice
								saveCrewCalltimeNotification(contactId, crewCall, true);
							}
						}
					}
				}
			}
		}
		// Any entry left in "oldCallTimes" was deleted during editing
		for (Integer id : oldCallTimes.keySet()) {
			saveCrewCalltimeNotification(id, null, false); // crew removed from call sheet
		}
		oldCallTimes = null;
	}

	/**
	 * Check for scene changes and location changes between the old callsheet and the new (unsaved)
	 * one. Notifications are issued to all members listed on the call sheet (cast and crew).
	 *
	 * @param oldCs The previous version of the callsheet.
	 */
	private void checkSceneChanges(Callsheet oldCs) {
		log.debug("st=" + callsheet.getStatus());
		String oldScenes = "", newScenes = "";
		boolean locationChanged = false;
		int ix = 0;
		SceneCall newsc;
		Set<Integer> oldLocationIds = new HashSet<>();
		Set<Integer> newLocationIds = new HashSet<>();
		Integer locationId;

		// first get a complete list of all the Location ids from the old callsheet
		for (SceneCall sc : oldCs.getSceneCalls()) {
			if ((locationId=RealWorldElementDAO.getLocationId(sc)) != null) {
				oldLocationIds.add(locationId);
			}
		}

		// Now scan old & new in parallel, looking for scene changes, and gathering
		// and new location id's, too.
		for (SceneCall sc : oldCs.getSceneCalls()) {
			if (ix < callsheet.getSceneCalls().size()) {
				newsc = callsheet.getSceneCalls().get(ix++);
			}
			else {
				newsc = null;
			}
			log.debug("new=" + newsc + ", old=" + sc);
			if (newsc != null && (locationId=RealWorldElementDAO.getLocationId(newsc)) != null) {
				newLocationIds.add(locationId);
				if (! oldLocationIds.contains(locationId)) {
					log.debug("new locId=" + locationId);
					locationChanged = true;
				}
			}
			if (sc.getNumber() != null && sc.getNumber().trim().length() > 0) {
				oldScenes += sc.getNumber().trim() + ", ";
			}
			if (newsc != null && newsc.getNumber() != null && newsc.getNumber().trim().length() > 0) {
				newScenes += newsc.getNumber().trim() + ", ";
			}
		}
		// for scene list, check for any additional lines in new callsheet:
		for (; ix < callsheet.getSceneCalls().size(); ix++) {
			newsc = callsheet.getSceneCalls().get(ix);
			if (newsc.getNumber() != null && newsc.getNumber().trim().length() > 0) {
				newScenes += newsc.getNumber().trim() + ", ";
			}
		}
		log.debug("new scn=" + newScenes + ", old scn=" + oldScenes);
		if ( ! oldScenes.equalsIgnoreCase(newScenes)) {
			saveSceneNotification(oldScenes, newScenes);
		}
		if (locationChanged ||
				(newLocationIds.size() != oldLocationIds.size())) {
			saveLocationNotification(newLocationIds);
		}
	}

	/**
	 * send callsheet call-time change alert email/SMS for Cast or crew members
	 *
	 * @param contactId The contact Id of the cast member whose time has
	 *            changed.
	 * @param castCall The new castCall object, with the updated times. This
	 *            should be null if the contact has been dropped from the call
	 *            sheet.
	 * @param added True if this user was just added to the call sheet.
	 */
	private void saveCastCalltimeNotification(Integer contactId, CastCall castCall, boolean added) {
		log.debug("id=" + contactId);
		if (castCall == null) { // removed from call sheet
			Contact contact = findContact(contactId, null);
			if (notifyContact(contact)) {
				ChangeNotice cn = new ChangeNotice(contactId, null, false);
				changeNotices.add(cn);
				numChangedDeleted++;
//				no.castCalltimeChanged(contact, unit, callsheet.getDate(), null, null, null, false);
			}
		}
		else {
			if (castCall.getOnSet() != null) {
				if (callsheetCurrent) {
					Contact contact = findContact(castCall.getContactId(), castCall.getName());
					if (notifyContact(contact)) {
						ChangeNotice cn = new ChangeNotice(contactId,
								castCall.getPickup(), castCall.getMakeup(), castCall.getOnSet(), added);
						changeNotices.add(cn);
						if (added) {
							numChangedAdded++;
						}
						else {
							numChangedCalltimes++;
						}
//						no.castCalltimeChanged(contact, unit, callsheet.getDate(),
//								castCall.getPickup(), castCall.getMakeup(), castCall.getOnSet(), added);
					}
				}
			}
		}
	}

	/**
	 * send callsheet call-time change alert email/SMS for crew members
	 *
	 * @param contactId The database id of the contact to whom the notification
	 *            is being sent.
	 * @param crewCall The new CrewCall entry for the Contact; null if the user
	 *            has been removed from the callsheet.
	 * @param added True if this user was just added to the call sheet.
	 */
	private void saveCrewCalltimeNotification(Integer contactId, CrewCall crewCall, boolean added) {
		if (crewCall == null) { // removed from call sheet
			Contact contact = findContact(contactId, null);
			if (notifyContact(contact)) {
				ChangeNotice cn = new ChangeNotice(contactId, null, false);
				changeNotices.add(cn);
				numChangedDeleted++;
				//no.crewCalltimeChanged(contact, unit, callsheet.getDate(), null, false);
			}
		}
		else {
			if (callsheetCurrent) {
				Contact contact = findContact(crewCall.getContactId(), crewCall.getName());
				if (notifyContact(contact)) {
					if (added) {
						numChangedAdded++;
					}
					else {
						numChangedCalltimes++;
					}
					ChangeNotice cn = new ChangeNotice(contact.getId(), crewCall.getTime(), added);
					changeNotices.add(cn);
//					no.crewCalltimeChanged(contact, unit, callsheet.getDate(), callTime, added);
				}
			}
		}
	}

	/**
	 * Find a Contact based either on its id or on the contact name.
	 * @param contactId
	 * @param name
	 * @return the matching Contact, or null if not found.
	 */
	private static Contact findContact(Integer contactId, String name) {
		Contact contact = null;
		ContactDAO contactDAO = ContactDAO.getInstance();
		if (contactId != null) {
			contact = contactDAO.findById(contactId);
		}
		if (contact == null) {
			List<Contact> contactList = contactDAO.findByDisplayName(name);
			if (contactList.size() > 0) {
				contact = contactList.get(0);
			}
		}
		return contact;
	}

	/**
	 * Find a Contact's id if the given value is null.
	 *
	 * @param contactId Either a valid Contact id, or null.
	 * @param name The Contact's name.
	 * @return The given contactId value if it is not null; otherwise, if the
	 *         name is found in a database search, then the resulting Contact's
	 *         id. If the name is not found, null is returned.
	 */
	private Integer findContactId(Integer contactId, String name) {
		if (contactId == null) {
			List<Contact> contactList = getContactDAO().findByDisplayName(name);
			if (contactList.size() > 0) {
				contactId = contactList.get(0).getId();
			}
		}
		return contactId;
	}

	/**
	 * Send notifications to all callsheet contacts when one or more locations changed.
	 */
	private void saveLocationNotification(Collection<Integer> newLocationIds) {
		ChangeNotice cn = new ChangeNotice(newLocationIds);
		changeNotices.add(cn);
		showChangedLocations = true;
		//no.callsheetLocationChanged(getCallsheetContacts(getCallsheet()), unit, callsheet.getDate(), newLocationIds);
	}

	/**
	 * Send notifications to all callsheet contacts when the list of scenes
	 * being shot (as listed on this callsheet) has changed.
	 * @param oldScenes
	 * @param newScenes
	 */
	private void saveSceneNotification(String oldScenes, String newScenes) {
		// remove trailing ", " from scene lists
		if (oldScenes.length() >= 2) {
			oldScenes = oldScenes.substring(0, oldScenes.length()-2);
		}
		if (newScenes.length() >= 2) {
			newScenes = newScenes.substring(0, newScenes.length()-2);
		}
		ChangeNotice cn = new ChangeNotice(oldScenes, newScenes);
		changeNotices.add(cn);
		showChangedScenes = true;
		//no.callsheetScenesChanged(getCallsheetContacts(getCallsheet()), unit, callsheet.getDate(), oldScenes, newScenes);
	}

	/**
	 * Determine if the Contact is willing and eligible to receive
	 * notifications.
	 *
	 * @param contact The Contact of interest, which may be null.
	 * @return True iff the Contact is not null, and has preferences and
	 *         attributes set such that we will send him a call sheet change
	 *         notification.
	 */
	private boolean notifyContact(Contact contact) {
		if (contact != null &&
				contact.getNotifyForAlerts() && // he wants call sheet changes
				((contact.getNotifyByEmail() && ! StringUtils.isEmpty(contact.getEmailAddress()) )
						|| (contact.getNotifyByAsstEmail() && contact.getAssistant() != null) ) ) {
			// ...and has valid email information
			return true;
		}
		return false;
	}

	/**
	 * Send out the appropriate notifications, based on the changes observed during the
	 * last save, and controlled by the user's selection of which notifications they
	 * want delivered.
	 */
	private void sendNotifications() {
		DoNotification no = DoNotification.getInstance();
		for (ChangeNotice cn : changeNotices) {
			if (cn.oldScenes != null) { // scene change
				if (sendScenes) {
					no.callsheetScenesChanged(getCallsheetContacts(getCallsheet()), unit, callsheet.getDate(), cn.oldScenes, cn.newScenes);
				}
			}
			else if (cn.locationIds != null) { // location change
				if (sendLocations) {
					no.callsheetLocationChanged(getCallsheetContacts(getCallsheet()), unit, callsheet.getDate(), cn.locationIds);
				}
			}
			else if (cn.pkupTime != null) { // cast added or time changed
				if (cn.added) {
					if (sendToAdded) {
						Contact contact = findContact(cn.contactId, null);
						no.castCalltimeChanged(contact, unit, callsheet.getDate(),
								cn.pkupTime, cn.mkupTime, cn.onsetTime, true);
					}
				}
				else if (sendCallTimes) {
					Contact contact = findContact(cn.contactId, null);
					no.castCalltimeChanged(contact, unit, callsheet.getDate(),
							cn.pkupTime, cn.mkupTime, cn.onsetTime, cn.added);
				}
			}
			else if (cn.callTime == null) { // cast or crew deleted
				if (sendToDeleted) {
					Contact contact = findContact(cn.contactId, null);
					no.crewCalltimeChanged(contact, unit, callsheet.getDate(), null, cn.added);
				}
			}
			else { // crew added or time changed
				if (sendCallTimes || (cn.added && sendToAdded)) {
					Contact contact = findContact(cn.contactId, null);
					no.crewCalltimeChanged(contact, unit, callsheet.getDate(), cn.callTime, cn.added);
				}
			}
		}
		changeNotices.clear();
		if (sendPdf) {
			// generate & email call sheets & optionally location reports
			PrintDailyReportBean rb = PrintDailyReportBean.getInstance();
			rb.setReportId(getCallsheet().getId());
			rb.actionPrintAndSendCallsheet(sendLocationReports);
		}
	}

	/**
	 * Generate a Collection of unique Contact entries based on all the names
	 * listed in both the cast-call and crew-call sections of this callsheet.
	 *
	 * @return A Collection<Contact>; may be empty, but is never null.
	 */
	public static Collection<Contact> getCallsheetContacts(Callsheet cs) {
		Set<Contact> contacts = new HashSet<>(); // Use Set to eliminate duplicates
		Contact contact;
		for (CastCall castCall : cs.getCastCalls()) {
			contact = findContact(castCall.getContactId(), castCall.getName());
			if (contact != null) {
				contacts.add(contact);
			}
		}
		for (DeptCall deptCall : cs.getDeptCalls()) {
			for (CrewCall crewCall : deptCall.getCrewCalls()) {
				contact = findContact(crewCall.getContactId(), crewCall.getName());
				if (contact != null) {
					contacts.add(contact);
				}
			}
		}
		log.debug("count="+contacts.size());
		return contacts;
	}

	/**
	 * Called as part of "Make Final" processing, if the user has requested
	 * that call time notifications be sent.
	 */
	private void pushCallTimes() {
		log.debug("");
		DoNotification no = DoNotification.getInstance();
		pushCastCallTimes(no);
		pushCrewCallTimes(no);
		return;
	}

	/**
	 * Send individual call time notifications to every cast person listed in
	 * the call sheet. The contacts in the cast are first grouped by their call
	 * time, then one notification call is made for each distinct call time with
	 * a list of matching cast Contacts.
	 *
	 * @param no A DoNotification instance to use.
	 */
	private void pushCastCallTimes(DoNotification no) {
		log.debug("");
		/** For pushCalltimes, map times to a List of contacts */
		Map<String, List<Contact>> pushCallList = new TreeMap<>();

		for (CastCall castCall : getCallsheet().getCastCalls()) {
			addCastCallTime(pushCallList, castCall);
		}
		for (Entry<String, List<Contact>> entry : pushCallList.entrySet()) {
			no.castCalltimePublished(entry.getValue(), unit, entry.getKey());
		}
		return;
	}

	/**
	 * Add an entry for the given CastCall to the call-time Map. The key
	 * consists of the concatenated times for pickup, makeup, and on-set. This
	 * creates groups of cast members with identical call times. If all three
	 * times are null, no entry is added to the Map, and the cast member will
	 * not get a notification.
	 *
	 * @param pushCallList The Map being updated.
	 * @param cc The CastCall instance for one cast member.
	 */
	private void addCastCallTime(Map<String, List<Contact>> pushCallList, CastCall cc) {
		Date calltime = cc.getOnSet();
		if (calltime == null) {
			calltime = cc.getMakeup();
		}
		if (calltime == null) {
			calltime = cc.getPickup();
		}
		if (calltime == null) {
			return;
		}
		long onset = (cc.getOnSet()==null ? 0 : cc.getOnSet().getTime());
		long pickup = (cc.getPickup()==null ? 0 : cc.getPickup().getTime());
		long makeup = (cc.getMakeup()==null ? 0 : cc.getMakeup().getTime());
		if ((onset+pickup+makeup) != 0 && currentTime.before(calltime)) {
			String key = "" + pickup + ' ' + makeup + ' ' + onset;
			Contact contact = findContact(cc.getContactId(), cc.getName());
			if (contact != null && contact.getNotifyForAlerts()) {
				List<Contact> contacts = pushCallList.get(key);
				if (contacts == null) {
					contacts = new ArrayList<>();
					pushCallList.put(key, contacts);
				}
				contacts.add(contact);
			}
		}
	}

	/**
	 * Send call time notifications to every crew person listed in the call
	 * sheet. The contacts in the call sheet are first grouped by their call
	 * time, then one notification call is made for each distinct call time with
	 * a list of matching crew member Contacts.
	 *
	 * @param no A DoNotification instance to use.
	 */
	private void pushCrewCallTimes(DoNotification no) {
		log.debug("");
		/** For pushCalltimes, map times to list of contacts */
		Map<Date,Set<Contact>> pushCallList = new TreeMap<>();

		for (DeptCall deptCall : getCallsheet().getDeptCalls()) {
			for (CrewCall crewCall : deptCall.getCrewCalls()) {
				addCrewCallTime(pushCallList, crewCall);
			}
		}
		for (Entry<Date, Set<Contact>> entry : pushCallList.entrySet()) {
			no.crewCalltimePublished(entry.getValue(), unit, entry.getKey());
		}
		return;
	}

	private void addCrewCallTime(Map<Date,Set<Contact>> pushCallList, CrewCall crewCall ) {
		Date calltime = crewCall.getTime();
		if (calltime != null && currentTime.before(calltime)) {
			Contact contact = findContact(crewCall.getContactId(), crewCall.getName());
			if (contact != null && contact.getNotifyForAlerts()) {
				Set<Contact> contacts = pushCallList.get(calltime);
				if (contacts == null) {
					contacts = new HashSet<>();
					pushCallList.put(calltime, contacts);
				}
				contacts.add(contact);
			}
		}
	}

	public String actionCrewCallOptions() {
		log.debug("");
		setShowTimeOptions(true);
		setOptionTime(callsheet.getCallTime());
		setOptionCrewTime(true); // doing Crew Call time
		View.addFocus("changeTime");
		return null;
	}

	public String actionShootingCallOptions() {
		log.debug("");
		setShowTimeOptions(true);
		setOptionTime(callsheet.getShootTime());
		setOptionCrewTime(false); // doing Shooting time, not crew call time
		View.addFocus("changeTime");
		return null;
	}

	public String actionCopyTimes() {
		log.debug("copy time=" + optionTime + (optionCrewTime ? " to crew call" : " to cast call"));
		try {
			setShowTimeOptions(false);

			if (optionCrewTime) {
				changeCrewTimes(optionTime);
			}
			else {
				changeCastTimes(optionTime);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionCancelCopyTimes() {
		log.debug("");
		setShowTimeOptions(false);
		return null;
	}

	private void changeCrewTimes(Date date) {
		date = CalendarUtils.sameDateAs(callsheet.getDate(), date);
		for (DeptCall dc : callsheet.getDeptCalls()) {
			for (CrewCall cc : dc.getCrewCalls()) {
				cc.setTime(date);
			}
		}
		checkNullValues(); // change nulls to zeroes, etc. if null not allowed in db
		getCallsheetDAO().attachDirty(callsheet);
	}

	private void changeCastTimes(Date date) {
		date = CalendarUtils.sameDateAs(callsheet.getDate(), date);
		for (CastCall cc : callsheet.getCastCalls()) {
			if (WorkdayType.toValue(cc.getStatus()).isWork()) {
				cc.setOnSet(date);
			}
			// TODO what about other time fields??
		}
		checkNullValues(); // change nulls to zeroes, etc. if null not allowed in db
		getCallsheetDAO().attachDirty(callsheet);
	}

	/**
	 * Action method for the "Refresh" button within the Scenes table.
	 *
	 * @return null navigation string
	 */
	public String actionRefreshScenes() {
		try {
			unit = UnitDAO.getInstance().refresh(unit);
			Callsheet cs = CallSheetUtils.create(callsheet.getDate(), unit);
			if (cs != null) {
				List<SceneCall> addedSceneCalls = cs.getSceneCalls();
				List<SceneCall> oldlist = callsheet.getSceneCalls();
				oldlist.clear();
				oldlist.addAll(addedSceneCalls);
				for (SceneCall sc : addedSceneCalls) {
					sc.setCallsheetByCallsheetId(callsheet);
				}
			}
			else {
				MsgUtils.addFacesMessage("CallSheet.RefreshFailed", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Refresh" button within the Advance Scenes table.
	 *
	 * @return null navigation string
	 */
	public String actionRefreshAdvance() {
		try {
			unit = UnitDAO.getInstance().refresh(unit);
			Callsheet cs = CallSheetUtils.create(callsheet.getDate(), unit);
			if (cs != null) {
				List<SceneCall> addedSceneCalls = cs.getAdvanceSceneCalls();
				List<SceneCall> oldlist = callsheet.getAdvanceSceneCalls();
				oldlist.clear();
				oldlist.addAll(addedSceneCalls);
				for (SceneCall sc : addedSceneCalls) {
					sc.setCallsheetByAdvanceId(callsheet);
				}
			}
			else {
				MsgUtils.addFacesMessage("CallSheet.RefreshFailed", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Refresh" button within the Cast table.
	 *
	 * @return null navigation string
	 */
	public String actionRefreshCast() {
		try {
			unit = UnitDAO.getInstance().refresh(unit);
			Callsheet cs = CallSheetUtils.create(callsheet.getDate(), unit);
			if (cs != null) {
				List<CastCall> castCalls = cs.getCastCalls();
				List<CastCall> oldlist = callsheet.getCastCalls();
				oldlist.clear();
				oldlist.addAll(castCalls);
				for (CastCall sc : castCalls) {
					sc.setCallsheet(callsheet);
				}
			}
			else {
				MsgUtils.addFacesMessage("CallSheet.RefreshFailed", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Cancel clicked on one of our simple confirmation dialogs.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ACT_ADD_CREW:
				res = actionCancelAddCrewCall();
				break;
			case ACT_MAKE_FINAL: // "Make Final" confirmation pop-up cancelled.
				// no action required
				break;
		}
		return res;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_MAKE_FINAL:
				res = actionMakeFinalConfirm();
				break;
			case ACT_ADD_CREW:
				res = actionAddCrewCall();
				break;
		}
		return res;
	}

	/**See {@link #editMode}. */
	public boolean getEditMode() {
		return editMode;
	}
	/**See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/**
	 * @return the callsheet
	 */
	public Callsheet getCallsheet() {
		return callsheet;
	}

	/** See {@link #callsheetId}. */
	public Integer getCallsheetId() {
		return callsheetId;
	}
	/** See {@link #callsheetId}. */
	public void setCallsheetId(Integer callsheetId) {
		this.callsheetId = callsheetId;
	}

	/**See {@link #production}. */
	public Production getProduction() {
		return production;
	}
	/**See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #dateDL}. */
	public List<SelectItem> getDateDL() {
		return dateDL;
	}
	/** See {@link #dateDL}. */
	public void setDateDL(List<SelectItem> dateDL) {
		this.dateDL = dateDL;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/** See {@link #showTimeOptions}. */
	public boolean getShowTimeOptions() {
		return showTimeOptions;
	}
	/** See {@link #showTimeOptions}. */
	public void setShowTimeOptions(boolean showTimeOptions) {
		this.showTimeOptions = showTimeOptions;
	}

	/** See {@link #optionCrewTime}. */
	public boolean getOptionCrewTime() {
		return optionCrewTime;
	}
	/** See {@link #optionCrewTime}. */
	public void setOptionCrewTime(boolean optionCrewTime) {
		this.optionCrewTime = optionCrewTime;
	}

	/** See {@link #optionTime}. */
	public Date getOptionTime() {
		return optionTime;
	}
	/** See {@link #optionTime}. */
	public void setOptionTime(Date optionTime) {
		this.optionTime = optionTime;
	}

	/** See {@link #callNotes}. */
	public String[] getCallNotes() {
		return callNotes;
	}
	/** See {@link #callNotes}. */
	public void setCallNotes(String[] callNotes) {
		this.callNotes = callNotes;
	}

	/** See {@link #deptCalls}. */
	public List<DeptCall>[] getDeptCalls() {
		return deptCalls;
	}

	/** See {@link #deptCalls}. */
	public void setDeptCalls(List<DeptCall>[] deptCalls) {
		this.deptCalls = deptCalls;
	}

	/**See {@link #cateringLogs}. */
	public CateringLog[] getCateringLogs() {
		return cateringLogs;
	}

	/**See {@link #cateringLogs}. */
	public void setCateringLogs(CateringLog[] cateringLogs) {
		this.cateringLogs = cateringLogs;
	}

	/** See {@link #addDeptId}. */
	public Integer getAddDeptId() {
		return addDeptId;
	}
	/** See {@link #addDeptId}. */
	public void setAddDeptId(Integer addDeptId) {
		this.addDeptId = addDeptId;
	}

	/** See {@link #showAddCrewCall}. */
	public boolean getShowAddCrewCall() {
		return showAddCrewCall;
	}
	/** See {@link #showAddCrewCall}. */
	public void setShowAddCrewCall(boolean b) {
		showAddCrewCall = b;
	}

	/** See {@link #showMakeFinal}. */
	public boolean getShowMakeFinal() {
		return showMakeFinal;
	}
	/** See {@link #showMakeFinal}. */
	public void setShowMakeFinal(boolean showMakeFinal) {
		this.showMakeFinal = showMakeFinal;
	}

	/** See {@link #sendCallTimes}. */
	public boolean getSendCallTimes() {
		return sendCallTimes;
	}
	/** See {@link #sendCallTimes}. */
	public void setSendCallTimes(boolean sendCallTimes) {
		this.sendCallTimes = sendCallTimes;
	}

	/** See {@link #sendCallSheets}. */
	public boolean getSendCallSheets() {
		return sendCallSheets;
	}
	/** See {@link #sendCallSheets}. */
	public void setSendCallSheets(boolean sendCallSheets) {
		this.sendCallSheets = sendCallSheets;
	}

	/** See {@link #sendLocationReports}. */
	public boolean getSendLocationReports() {
		return sendLocationReports;
	}
	/** See {@link #sendLocationReports}. */
	public void setSendLocationReports(boolean sendLocationReports) {
		this.sendLocationReports = sendLocationReports;
	}

	/** See {@link #showChanges}. */
	public boolean getShowChanges() {
		return showChanges;
	}
	/** See {@link #showChanges}. */
	public void setShowChanges(boolean showChanges) {
		this.showChanges = showChanges;
	}

	/** See {@link #showChangedDeleted}. */
	public boolean getShowChangedDeleted() {
		return showChangedDeleted;
	}
	/** See {@link #showChangedDeleted}. */
	public void setShowChangedDeleted(boolean showChangedDeleted) {
		this.showChangedDeleted = showChangedDeleted;
	}

	/** See {@link #showChangedAdded}. */
	public boolean getShowChangedAdded() {
		return showChangedAdded;
	}
	/** See {@link #showChangedAdded}. */
	public void setShowChangedAdded(boolean showChangedAdded) {
		this.showChangedAdded = showChangedAdded;
	}

	/** See {@link #showChangedCalltimes}. */
	public boolean getShowChangedCalltimes() {
		return showChangedCalltimes;
	}
	/** See {@link #showChangedCalltimes}. */
	public void setShowChangedCalltimes(boolean showChangedCalltimes) {
		this.showChangedCalltimes = showChangedCalltimes;
	}

	/** See {@link #showChangedLocations}. */
	public boolean getShowChangedLocations() {
		return showChangedLocations;
	}
	/** See {@link #showChangedLocations}. */
	public void setShowChangedLocations(boolean showChangedLocations) {
		this.showChangedLocations = showChangedLocations;
	}

	/** See {@link #showChangedScenes}. */
	public boolean getShowChangedScenes() {
		return showChangedScenes;
	}
	/** See {@link #showChangedScenes}. */
	public void setShowChangedScenes(boolean showChangedScenes) {
		this.showChangedScenes = showChangedScenes;
	}

	/** See {@link #sendToDeleted}. */
	public boolean getSendToDeleted() {
		return sendToDeleted;
	}
	/** See {@link #sendToDeleted}. */
	public void setSendToDeleted(boolean sendToDeleted) {
		this.sendToDeleted = sendToDeleted;
	}

	/** See {@link #sendToAdded}. */
	public boolean getSendToAdded() {
		return sendToAdded;
	}
	/** See {@link #sendToAdded}. */
	public void setSendToAdded(boolean sendToAdded) {
		this.sendToAdded = sendToAdded;
	}

	/** See {@link #sendScenes}. */
	public boolean getSendScenes() {
		return sendScenes;
	}
	/** See {@link #sendScenes}. */
	public void setSendScenes(boolean sendScenes) {
		this.sendScenes = sendScenes;
	}

	/** See {@link #sendLocations}. */
	public boolean getSendLocations() {
		return sendLocations;
	}
	/** See {@link #sendLocations}. */
	public void setSendLocations(boolean sendLocations) {
		this.sendLocations = sendLocations;
	}

	/** See {@link #sendPdf}. */
	public boolean getSendPdf() {
		return sendPdf;
	}
	/** See {@link #sendPdf}. */
	public void setSendPdf(boolean sendPdf) {
		this.sendPdf = sendPdf;
	}

	/** See {@link #numChangedDeleted}. */
	public int getNumChangedDeleted() {
		return numChangedDeleted;
	}
	/** See {@link #numChangedDeleted}. */
	public void setNumChangedDeleted(int numChangedDeleted) {
		this.numChangedDeleted = numChangedDeleted;
	}

	/** See {@link #numChangedAdded}. */
	public int getNumChangedAdded() {
		return numChangedAdded;
	}
	/** See {@link #numChangedAdded}. */
	public void setNumChangedAdded(int numChangedAdded) {
		this.numChangedAdded = numChangedAdded;
	}

	/** See {@link #numChangedCalltimes}. */
	public int getNumChangedCalltimes() {
		return numChangedCalltimes;
	}
	/** See {@link #numChangedCalltimes}. */
	public void setNumChangedCalltimes(int numChangedCalltimes) {
		this.numChangedCalltimes = numChangedCalltimes;
	}

	/** See {@link #headerTabId}. */
	public String getHeaderTabId() {
		return headerTabId;
	}
	/** See {@link #headerTabId}. */
	public void setHeaderTabId(String headerTabId) {
		this.headerTabId = headerTabId;
	}

	private static class ChangeNotice {
		Integer contactId;
		Date callTime;
		Date pkupTime;
		Date mkupTime;
		Date onsetTime;
		boolean added;
		String oldScenes;
		String newScenes;
		Collection<Integer> locationIds;

		ChangeNotice(Integer id, Date pkuptime, Date mkuptime, Date onsettime, boolean add) {
			contactId = id;
			pkupTime = pkuptime;
			mkupTime = mkuptime;
			onsetTime = onsettime;
			added = add;
		}

		ChangeNotice(Integer id, Date calltime, boolean add) {
			contactId = id;
			callTime = calltime;
			added = add;
		}

		ChangeNotice(String oldscenes, String newscenes) {
			oldScenes = oldscenes;
			newScenes = newscenes;
		}

		ChangeNotice(Collection<Integer> newLocationIds) {
			locationIds = newLocationIds;
		}

	}

	private CallsheetDAO getCallsheetDAO() {
		if (callsheetDAO == null) {
			callsheetDAO = CallsheetDAO.getInstance();
		}
		return callsheetDAO;
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

}
