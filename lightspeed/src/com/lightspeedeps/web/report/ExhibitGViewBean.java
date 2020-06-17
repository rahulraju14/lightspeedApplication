package com.lightspeedeps.web.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.CallsheetDAO;
import com.lightspeedeps.dao.ExhibitGDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.ReportTimeDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ExhibitG;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ReportTime;
import com.lightspeedeps.model.SceneCall;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.type.ReportStatus;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.ReportTimeUtils;
import com.lightspeedeps.web.contact.RoleSelectBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Backing bean for ExhibitG View and Edit pages. This includes code to support editing
 * the list of Cast time entries.
 */
@ManagedBean
@ViewScoped
public class ExhibitGViewBean implements Serializable {
	/** */
	private static final long serialVersionUID = 6569472028705514843L;

	private static final Log log = LogFactory.getLog(ExhibitGViewBean.class);

	/** Session attribute used to store the database id of the current Exhibit G. */
	private static final String ATTR_EXHIBITG_ID = Constants.ATTR_PREFIX + "exhibitgid";

	/** Session attribute used to indicate we're in Edit mode */
	/*package*/ static final String ATTR_EXHIBITG_EDIT = Constants.ATTR_PREFIX + "exhibitgedit";

	/*package*/ static final String ATTR_EXHIBITG_NEW_DATE = Constants.ATTR_PREFIX + "newExhibitGDate";

	/** The ExhibitG object currently displayed. */
	private ExhibitG exhibitG;

	/** The current project. */
	private Project project;

	/** True iff the display is in Edit mode. Used by the JSP to determine which
	 * controls are output. */
	private boolean editMode;

	/** True when the "add cast" pop-up should be displayed. */
	private boolean showAddCast = false;

	/** The contact associated with the "Add cast" pop-up. */
	private Contact newContact;

	/** The database id of the cast line item (ReportTime) to be deleted. */
	private Integer removeId;

	/** The ReportTime`s that have been deleted by the user during the current
	 * edit cycle.  These are not deleted from the database until the user
	 * does a Save. */
	private final List<ReportTime> removedCards = new ArrayList<>();


	public ExhibitGViewBean() {
		log.debug("Initializing ExhibitGViewBean instance");
		try {
			project = SessionUtils.getCurrentProject();
			editMode = SessionUtils.getBoolean(ATTR_EXHIBITG_EDIT, false);
			SessionUtils.put(ATTR_EXHIBITG_EDIT, null);
			initView();
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
	}

	/**
	 * Initialization
	 */
	private void initView() {
		Date date;
		if ( (date = (Date)SessionUtils.get(ATTR_EXHIBITG_NEW_DATE)) != null) {
			// invoked (from Reports page) to create a new ExhibitG.
			SessionUtils.put(ATTR_EXHIBITG_NEW_DATE, null);
			createExhibitG(date);
		}
		else {
			// View or Edit an existing ExhibitG.
			Integer id;
			if ( (id = SessionUtils.getInteger(ATTR_EXHIBITG_ID)) != null) {
				exhibitG = ExhibitGDAO.getInstance().findById(id);
				sort();
			}
		}
	}

	/**
	 * Action method for the "Edit" button on View page.
	 *
	 * @return The navigation string to jump to the Edit page.
	 */
	public String actionEdit() {
		log.debug("");
		SessionUtils.put(ATTR_EXHIBITG_ID, exhibitG.getId());
		SessionUtils.put(ATTR_EXHIBITG_EDIT, true);
		return "exhibitGedit";
	}

	/**
	 * Action method for the "Cancel" button on the edit mode page.
	 *
	 * @return Navigation string to jump to View mode page.
	 */
	public String actionCancel() {
		log.debug("");
		if (exhibitG.getId() == null) {
			return "reports";
		}
		else {
			return "exhibitGview";
		}
	}

	/**
	 * Action Listener method for "Save" button on the Edit page.
	 * @param event
	 */
	public String actionSave() {
		log.debug("");
		try {
			ReportTimeDAO timeCardDAO = ReportTimeDAO.getInstance();
			for (ReportTime tc : removedCards) {
				if (tc.getId() != null) {
					tc = timeCardDAO.refresh(tc);
					timeCardDAO.delete(tc);
				}
			}
			removedCards.clear();
			for (ReportTime timeCard : exhibitG.getTimeCards()) {
				if (timeCard.getId() != null && timeCard.getId() < 0) {
					timeCard.setId(null);
				}
				setDate(timeCard, true);
			}
			exhibitG.setStatus(ReportStatus.UPDATED);
			exhibitG.setUpdated(new Date());
			timeCardDAO.attachDirty(getExhibitG());
			SessionUtils.put(ATTR_EXHIBITG_ID, exhibitG.getId());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return HeaderViewBean.REPORTS_MENU_EXHIBITG_VIEW;
	}

	/**
	 * Action method for the "Add" button, which displays the
	 * "Add Cast" pop-up dialog, managed by the RoleSelectBean.
	 * @return null navigation string
	 */
	public String actionShowAddCast() {
		try {
			RoleSelectBean selectBean = RoleSelectBean.getInstance();
			selectBean.setDepartmentId(Constants.DEPARTMENT_ID_CAST);
			selectBean.setOmitContactMap(createCastMap());
			refreshProject();
			showAddCast = true;
//			ListView.addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/** Close the "Add cast" dialog */
	public String actionCloseAddCast() {
		showAddCast = false;
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Action method of the "Add" button on the "Add Cast" pop-up dialog.
	 * Create a ReportTime for the specified Contact, and add it to
	 * the TimeSheet's collection.  It will get saved when the user
	 * clicks Save for the overall Edit session.
	 * @return null navigation string
	 */
	public String actionAddCast() {
		try {
			if (newContact == null) {
				MsgUtils.addFacesMessage("TimeSheet.ContactMissing", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			RoleSelectBean selectBean = RoleSelectBean.getInstance();
			Integer unitId = selectBean.getUnitId();
			Unit unit = UnitDAO.getInstance().findById(unitId);
			ReportTime tc = ReportTimeUtils.createTimeCard(newContact, unit.getNumber());
			tc.setId(0-(int)(Math.random()*100000.0)); // negative value to track unsaved links
			tc.setExhibitG(exhibitG);
			tc.setDtype(ReportTime.DTYPE_CAST);
			exhibitG.getTimeCards().add(tc);
			List<ScriptElement> characters =
					ScriptElementDAO.getInstance().findCharacterFromContact(newContact, project);
			if (characters.size() > 0) {
				ScriptElement scriptElement = characters.get(0);
				ElementDood elementDood = scriptElement.getElementDood(unit);
				WorkdayType dayType = elementDood.getStatus(exhibitG.getDate());
				if (dayType.isWork()) {
					tc.setDayType(dayType.asWorkStatus());
				}
				tc.setRole(scriptElement.getName());
				tc.setCastIdStr(scriptElement.getElementIdStr());
				tc.setCastId(scriptElement.getElementId());
			}
			sort();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		actionCloseAddCast();
		return null;
	}

	/**
	 * Delete the selected cast ReportTime entry.  The 'removeId' field
	 * should have already been set via an f:setPropertyActionListener.
	 * We only remove the ReportTime from the TimeSheet's list; the actual
	 * database delete will happen when the user hits Save.
	 */
	public String actionDeleteCast() {
		try {
			if (removeId != null) {
				removeTimeCard(removeId);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Find a ReportTime in the current timesheet's collection, whose id matches
	 * the value provided, and remove it from the collection.  The ReportTime
	 * found is also added to the 'removedCards' collection for actual database
	 * deletion during Save.
	 * @param id
	 */
	private void removeTimeCard(Integer id) {
		ReportTime timeCard;
		for (Iterator<ReportTime> iter = exhibitG.getTimeCards().iterator(); iter.hasNext(); ) {
			timeCard = iter.next();
			if (timeCard.getId().equals(id)) {
				iter.remove();
				if (timeCard.getId() >= 0) {
					removedCards.add(timeCard);
				}
				break;
			}
		}
	}

	private void createExhibitG(Date date) {
		log.debug("create for " + date);

		exhibitG = new ExhibitG(project, ReportStatus.CREATED, date, false);

		exhibitG.setTimeCards(ReportTimeUtils.createCastTimecards(date));
		for (ReportTime tc : exhibitG.getTimeCards()) {
			tc.setExhibitG(exhibitG);
		}

		// Get Location from same date's Callsheet
		Callsheet callsheet = CallsheetDAO.getInstance().findByDateAndProjectMain(date, project);
		if (callsheet != null) {
			List<SceneCall> sceneSet;
			sceneSet = callsheet.getSceneCalls();
			if (sceneSet != null && sceneSet.size() > 0) {
				log.debug("Scene Set Size " + sceneSet.size());
				exhibitG.setLocation(sceneSet.iterator().next().getLocation());
			}
		}

		// Getting Day off
		ScheduleUtils su = new ScheduleUtils(project.getMainUnit());
		WorkdayType dtype = su.findDayType(date);
		exhibitG.setDayOff(dtype == WorkdayType.OFF);

		ExhibitG prior = ExhibitGDAO.getInstance().findPrior(date, project);
		if (prior != null) {
			exhibitG.setType(prior.getType());
			exhibitG.setTitle(prior.getTitle());
			exhibitG.setCompany(prior.getCompany());
			exhibitG.setContactName(prior.getContactName());
			exhibitG.setContactPhone(prior.getContactPhone());
			exhibitG.setProductionNumber(prior.getProductionNumber());
		}
		else {
			log.debug("no prior exhibit G");
			Production production = SessionUtils.getProduction();
			exhibitG.setTitle(production.getTitle());
			exhibitG.setCompany(production.getStudio());
			switch (production.getType()) {
				case FEATURE_FILM:
				case TELEVISION_SERIES:
				case TELEVISION_MOVIE:
					exhibitG.setType(production.getType());
					break;
				case TV_COMMERCIALS:
				case TOURS:
				default:
					exhibitG.setType(ProductionType.OTHER);
			}
		}
		sort();
	}

	/**
	 * Create a mapping of departments to Lists of contact ids for those
	 * contacts already included in the cast portion of the timesheet. Since
	 * they are all cast, there's only a single department in the map.
	 * @return the non-null Map as described above.
	 */
	private Map<Integer, List<Integer>> createCastMap() {
		Map<Integer, List<Integer>> castMap = new HashMap<>();
		List<Integer> castList= new ArrayList<>();
		for (ReportTime tc : exhibitG.getTimeCards()) {
			castList.add(tc.getContact().getId());
		}
		castMap.put(Constants.DEPARTMENT_ID_CAST, castList);
		return castMap;
	}

	private void refreshProject() {
		project = ProjectDAO.getInstance().refresh(project);
		for (Unit u : project.getUnits()) {
			u.getName();
		}
	}

	/**
	 * Sort the time cards in alphabetic order, for display.
	 */
	private void sort() {
		Collections.sort(exhibitG.getTimeCards(), alphaComparator);
	}

	private static final Comparator<ReportTime> alphaComparator = new Comparator<ReportTime>() {
		@Override
		public int compare(ReportTime e1, ReportTime e2) {
			int x = NumberUtils.compare(e1.getUnitNumber(), e2.getUnitNumber());
			if (x == 0) {
				if (e1.getContact() != null) {
					x = e1.getContact().compareTo(e2.getContact());
				}
				else if (e2.getContact() == null) {
					x = 0;
				}
				else {
					x = -1;
				}
			}
			return x;
		}
	};

	/**
	 * Make sure the Date fields within the given ReportTime have the correct
	 * calendar date (matching the exhibitG), rather than an arbitrary value
	 * (e.g., today) set by the framework when the user enters a time value into
	 * an empty field.
	 *
	 * @param timeCard The ReportTime to update.
	 * @param isCast True if this is a Cast-related ReportTime. (Currently always
	 *            true, as the crew card functionality was moved to the separate
	 *            weekly time-card system.)
	 */
	private void setDate(ReportTime timeCard, boolean isCast) {
		Date timeSheetDate = exhibitG.getDate();
		if (timeCard.getReportSet() != null) {
			timeCard.setReportSet(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getReportSet()));
		}
		if (timeCard.getDismissSet() != null) {
			timeCard.setDismissSet(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getDismissSet()));
		}
		if (timeCard.getFirstMealStart() != null) {
			timeCard.setFirstMealStart(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getFirstMealStart()));
		}
		if (timeCard.getFirstMealEnd() != null) {
			timeCard.setFirstMealEnd(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getFirstMealEnd()));
		}
		if (timeCard.getSecondMealStart() != null) {
			timeCard.setSecondMealStart(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getSecondMealStart()));
		}
		if (timeCard.getSecondMealEnd() != null) {
			timeCard.setSecondMealEnd(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getSecondMealEnd()));
		}

		if (isCast) {
			if (timeCard.getReportMakeup() != null) {
				timeCard.setReportMakeup(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getReportMakeup()));
			}
			if (timeCard.getDismissMakeup() != null) {
				timeCard.setDismissMakeup(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getDismissMakeup()));
			}
			if (timeCard.getNdMealStart() != null) {
				timeCard.setNdMealStart(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getNdMealStart()));
			}
			if (timeCard.getLeaveForLocation() != null) {
				timeCard.setLeaveForLocation(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getLeaveForLocation()));
			}
			if (timeCard.getArriveLocation() != null) {
				timeCard.setArriveLocation(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getArriveLocation()));
			}
			if (timeCard.getLeaveLocation() != null) {
				timeCard.setLeaveLocation(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getLeaveLocation()));
			}
			if (timeCard.getArriveStudio() != null) {
				timeCard.setArriveStudio(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getArriveStudio()));
			}
			if (timeCard.getStuntAdjust() != null) {
				timeCard.setStuntAdjust(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getStuntAdjust()));
			}
			if (timeCard.getTutoringHours() != null) {
				timeCard.setTutoringHours(CalendarUtils.sameDateAs(timeSheetDate, timeCard.getTutoringHours()));
			}
		}
	}

	/** See {@link #exhibitG}. */
	public ExhibitG getExhibitG() {
		return exhibitG;
	}
	/** See {@link #exhibitG}. */
	public void setExhibitG(ExhibitG exhibitG) {
		this.exhibitG = exhibitG;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #editMode}. */
	public boolean getEditMode() {
		return editMode;
	}
	/** See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/** See {@link #showAddCast}. */
	public boolean getShowAddCast() {
		return showAddCast;
	}
	/** See {@link #showAddCast}. */
	public void setShowAddCast(boolean showAddCast) {
		this.showAddCast = showAddCast;
	}

	/** See {@link #newContact}. */
	public Contact getNewContact() {
		return newContact;
	}
	/** See {@link #newContact}. */
	public void setNewContact(Contact newContact) {
		this.newContact = newContact;
	}

	/** See {@link #removeId}. */
	public Integer getRemoveId() {
		return removeId;
	}
	/** See {@link #removeId}. */
	public void setRemoveId(Integer removeId) {
		this.removeId = removeId;
	}

}
