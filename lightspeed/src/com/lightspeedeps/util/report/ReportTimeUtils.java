package com.lightspeedeps.util.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.CallsheetDAO;
import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DailyTimeDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.CastCall;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.CrewCall;
import com.lightspeedeps.model.DailyTime;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.DeptCall;
import com.lightspeedeps.model.Dpr;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.model.ReportTime;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.WorkdayType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * Utilities related to the original TimeCard objects, which are now used
 * only as part of the DPR and ExhibitG reports.
 */
public class ReportTimeUtils {
	private static final Log log = LogFactory.getLog(ReportTimeUtils.class);

	private ReportTimeUtils() {
	}

	/**
	 * Build a List of TimeCard objects for Cast members working on a particular
	 * date. The entries are created based on information from the given date's
	 * Call Sheet. This is used for the Exhibit G and the DPR.
	 *
	 * @param date The date of interest.
	 * @return A non-null (but possibly empty) List of TimeCard`s, one for each
	 *         cast member listed on any Callsheet for the given date. (There
	 *         can be one Callsheet for each Unit.)
	 */
	public static List<ReportTime> createCastTimecards(Date date) {
		List<ReportTime> timeCardCastList = new ArrayList<ReportTime>();

		List<Callsheet> sheets = CallsheetDAO.getInstance()
				.findByDateAndProject(date, SessionUtils.getCurrentProject());
		if (sheets.size() == 0) {
			return timeCardCastList;
		}
		// There may be multiple Callsheet`s if the project has multiple Unit`s.
		Map<Integer, ReportTime> enteredCastTimes = new HashMap<Integer, ReportTime>();
		for (Callsheet callsheet : sheets) {
			// For Cast
			for (CastCall cc : callsheet.getCastCalls()) {
				// Create timecard & add to 'enteredCastTimes':
				addCastTimecard(date, cc, enteredCastTimes, callsheet.getUnitNumber());
			}
		}
		timeCardCastList.addAll(enteredCastTimes.values());
		Collections.sort(timeCardCastList, ReportTime.castIdComparator());

		return timeCardCastList;
	}

	/**
	 * Build a Set of TimeCard objects for crew members working on a particular
	 * date. The entries are created based on information from the given date's
	 * Call Sheet. This is used for the DPR.
	 *
	 * @param dpr The Dpr the TimeCard's will be associated with.
	 * @return A non-null (but possibly empty) Set of TimeCard`s, one for each
	 *         crew member listed on any Callsheet for the given date. (There
	 *         can be one Callsheet for each Unit.)
	 */
	public static Set<ReportTime> createCrewTimeCards(Dpr dpr) {
		Set<ReportTime> timeCardCrewList = new HashSet<ReportTime>();
		List<Callsheet> sheets = CallsheetDAO.getInstance()
				.findByDateAndProject(dpr.getDate(), SessionUtils.getCurrentProject());
		if (sheets.size() == 0) {
			createCrewTimeCards(dpr, timeCardCrewList);
			return timeCardCrewList;
		}
		// There may be multiple Callsheet`s if the project has multiple Unit`s.
		ContactDAO contactDAO = ContactDAO.getInstance();
		for (Callsheet callsheet : sheets) {
			for (DeptCall dc : callsheet.getDeptCalls()) {
				if (dc.getDepartment().getShowOnDpr()) {
					for (CrewCall cc : dc.getCrewCalls()) {
						if (cc.getContactId() != null) {
							Contact c = contactDAO.findById(cc.getContactId());
							if (c != null) {
								ReportTime tc = createCrewTimeCard(c, 1, dpr, cc.getTime());
								tc.setDepartment(dc.getDepartment());
								tc.setRole(cc.getRoleName());
								tc.setListPriority(cc.getPriority());
								timeCardCrewList.add(tc);
							}
						}
					}
				}
			}
		}
		return timeCardCrewList;
	}

	/**
	 * Update an existing Dpr's set of TimeCards with the call time information
	 * from the matching Callsheet.
	 *
	 * @param dpr The Dpr to be updated.
	 * @return True iff at least one Callsheet was found that matches the given
	 *         Dpr within the current Project.
	 */
	public static boolean updateCrewTimeCardsFromCallsheet(Dpr dpr) {
		List<Callsheet> sheets = CallsheetDAO.getInstance()
				.findByDateAndProject(dpr.getDate(), SessionUtils.getCurrentProject());
		if (sheets.size() == 0) {
			return false;
		}
		// There may be multiple Callsheet`s if the project has multiple Unit`s.
		for (Callsheet callsheet : sheets) {
			for (ReportTime tc : dpr.getCrewTimeCards()) {
				Department dept = tc.getDepartment();
				findCrewCall:
				for (DeptCall dc : callsheet.getDeptCalls()) {
					if (dc.getDepartment().equals(dept)) {
						for (CrewCall cc : dc.getCrewCalls()) {
							if (tc.getContact().getId().equals(cc.getContactId())) {
								if (cc.getRoleName().equals(tc.getRole())) {
									tc.setCallTime(CalendarUtils.convertTimeToDecimal(cc.getTime(), false));
									break findCrewCall;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Update an existing Dpr's set of TimeCard`s with the times from the
	 * WeeklyTimecard`s created by the individual users or the department
	 * time-keepers. This is used only for the DPR.
	 *
	 * @param dpr The Dpr which will be associated with the created TimeCard`s.
	 * @return A non-null (but possibly empty) Set of TimeCard`s.
	 */
	public static int updateCrewTimeCardsFromWtc(Dpr dpr, boolean updateCall, boolean updateWrap) {
		int count = 0;
		ContactDAO contactDAO = ContactDAO.getInstance();
		Production prod = SessionUtils.getProduction();
		String prodId = prod.getProdId();
		List<DailyTime> dts;
		if (dpr.getProject().getProduction().getType().isAicp()) {
			dts = DailyTimeDAO.getInstance().findByDateAndProject(dpr.getDate(), dpr.getProject());
		}
		else {
			dts = DailyTimeDAO.getInstance().findByDateAndProdid(dpr.getDate(), prodId);
		}

		for (DailyTime dt : dts) {
			WeeklyTimecard wtc = dt.getWeeklyTimecard();
			Contact c = contactDAO.findByAccountNumAndProduction(wtc.getUserAccount(), prod);
			if (c != null) {
				ReportTime matched = null;
				boolean updated = false;
				for (ReportTime tc : dpr.getCrewTimeCards()) {
					if (tc.getContact().equals(c)) {
						matched = tc;
						if (tc.getRole().equals(wtc.getOccupation())) {
							updated = true;
							updateTimes(dt, tc, updateCall, updateWrap);
						}
					}
				}
				if ((! updated) && matched != null) {
					updateTimes(dt, matched, updateCall, updateWrap);
				}
				count++;
			}
		}
		return count;
	}

	/**
	 * @param dt
	 * @param tc
	 * @param updateCall
	 * @param updateWrap
	 */
	private static void updateTimes(DailyTime dt, ReportTime tc, boolean updateCall,
			boolean updateWrap) {
		if (dt.getWorked() && updateWrap) { // display as "O/C"
			tc.setCallTime(ReportTime.OC_TIME);
			tc.setWrap(null);
		}
		else {
			if (updateCall) {
				tc.setCallTime(dt.getCallTime());
			}
			if (updateWrap) {
				tc.setWrap(dt.getWrap());
			}
		}
	}

	/**
	 * Create a basic crew-style TimeCard from the information in the Contact
	 * provided.
	 *
	 * @param newContact The Contact related to this TimeCard object.
	 * @param unitNumber The number of the Unit with which this TimeCard is
	 *            associated.
	 * @param dpr The DPR with which this TimeCard is associated.
	 * @param callTime The crew call time to be set in the new TimeCard.
	 * @return The new TimeCard instance.
	 */
	public static ReportTime createCrewTimeCard(Contact newContact, Integer unitNumber, Dpr dpr, Date callTime) {
		ReportTime tc = createTimeCard(newContact, unitNumber);
		tc.setDprCrew(dpr);
		tc.setDtype(ReportTime.DTYPE_CREW);
		tc.setCallTime(CalendarUtils.convertTimeToDecimal(callTime, false));
		return tc;
	}

	/**
	 * Create a basic TimeCard from the information in the Contact
	 * provided.  This is information common to both crew and cast TimeCards.
	 * @param newContact
	 * @param unitNumber The number of the Unit with which this TimeCard is associated.
	 * @return The new TimeCard instance.
	 */
	public static ReportTime createTimeCard(Contact newContact, Integer unitNumber) {
		ReportTime tc = new ReportTime();
		tc.setContact(newContact);
		tc.setUnitNumber(unitNumber);
		tc.setMinor(newContact.getUser().getMinor());
		tc.setDayType(WorkdayType.WORK.asWorkStatus());
		return tc;
	}

	/**
	 * Create a TimeCard to match the given CastCall entry, then add it to the
	 * 'enteredTimes' Map, unless the same contact is already represented, in
	 * which case we will update it if the new entry has an earlier call time.
	 *
	 * @param castCall The CastCall entry from the Callsheet.
	 * @param enteredTimes A map from contact id's to TimeCard objects, allowing
	 *            us to keep track of which Contact`s have already had a
	 *            TimeCard generated for this Timesheet.
	 * @param unitNumber The number of the Unit associated with the Callsheet
	 *            that was the source of the given CastCall.
	 */
	private static void addCastTimecard(Date date, CastCall castCall, Map<Integer, ReportTime> enteredTimes,
			Integer unitNumber) {
		Contact contact = null;
		ReportTime tc;
		Project proj = SessionUtils.getCurrentProject();
		Unit unit = proj.getUnit(unitNumber);
		if (unit != null) {
			try {
				ContactDAO contactDAO = ContactDAO.getInstance();
				if (castCall.getContactId() != null) {
					contact = contactDAO.findById(castCall.getContactId());
				}
				if (contact == null && castCall.getName() != null && castCall.getName().length() > 0) {
					List<Contact> contactList = contactDAO.findByDisplayName(castCall.getName());
					if (contactList.size() > 0) {
						contact = contactList.get(0);
						log.debug("matched by name");
					}
				}
				if (contact != null) {
					log.debug("cast contact: " + contact.getUser().getLastNameFirstName());
					tc = createTimeCard(contact, unitNumber);
					tc.setDtype(ReportTime.DTYPE_CAST);
					tc.setReportSet(castCall.getOnSet());
					tc.setLeaveForLocation(castCall.getPickup());
					tc.setReportMakeup(castCall.getMakeup());
					tc.setRole(castCall.getCharacterName());
					tc.setCastIdStr(castCall.getActorIdStr());
					tc.setCastId(castCall.getActorId());
//					WorkdayType type = WorkdayType.toValue(castCall.getStatus());
//					if (type == WorkdayType.OFF || type == null) {
//						List<ScriptElement> characterScriptInst =
//								ScriptElementDAO.getInstance().findCharacterFromContact(contact, proj);
//						if (characterScriptInst.size() > 0) {
//							ScriptElement scriptElement = characterScriptInst.get(0);
//							ElementDood elementDood = scriptElement.getElementDood(unit);
//							type = elementDood.getStatus(date);
//							log.debug("type from script element DooD");
//						}
//					}
//					log.debug("type: " + type.toString());
					tc.setDayType(castCall.getStatus()); // just copy Callsheet status as-is; 2.2.4710
					if (! enteredTimes.containsKey(contact.getId())) {
						enteredTimes.put(contact.getId(), tc);
					}
					else { // duplicate entry, use earlier one
						ReportTime priorTc = enteredTimes.get(contact.getId());
						Date priorSet = priorTc.getReportSet();
						Date currentSet = castCall.getOnSet();
						if (priorSet != null) {
							if (currentSet != null && priorSet.after(currentSet)) {
								// current one is earlier, replace the prior one:
								enteredTimes.put(contact.getId(), tc);
							}
						}
						else if (currentSet != null) {
							// prior one had no OnSet time, this one does, so use it
							enteredTimes.put(contact.getId(), tc);
						}
					}
				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
	}

	/**
	 * Create all the DPR TimeCard entries (which will be used to populate the
	 * back page crew list on the DPR). The DepartmentDAO query used, only
	 * returns departments with 'showOnDpr' set to true, which automatically
	 * avoids LS Admin and any other "departments" which are not meant to be
	 * included on the DPR.
	 *
	 * @param dpr The DPR all the created TimeCard`s will be associated with.
	 * @param tcList The Set to which all the created TimeCard`s will be added.
	 */
	private static void createCrewTimeCards(Dpr dpr, Set<ReportTime> tcList) {
		Project project = dpr.getProject();
		Production prod = project.getProduction();
		if (! prod.getType().hasPayrollByProject()) {
			project = null; // don't pass project for TV/Feature, only for Commercial productions
		}
		List<Department> depts = DepartmentDAO.getInstance().findByProductionCompleteDpr(prod, project);
		for (Department dept : depts) {
			createCrewTimeCards(dpr, dept, tcList);
		}
	}

	/**
	 * Create the TimeCard entries for all the crew members in a specific
	 * department. All the call times are set to the current DPR's CallTime.
	 *
	 * @param dpr The DPR the created entries should be associated with.
	 * @param dept The Department whose members we should create TimeCard`s for.
	 * @param tcList The Set of TimeCard`s to which this routine will add all
	 *            the created TimeCard`s.
	 */
	private static void createCrewTimeCards(Dpr dpr, Department dept, Set<ReportTime> tcList) {
		Department pmDept = dept;
		if (dept.getStandardDeptId() != null) {
			// For standard departments, projectMembers always refer to the SYSTEM version of the Department
			pmDept = DepartmentDAO.getInstance().findById(dept.getStandardDeptId());
		}
		List<ProjectMember> members =
				ProjectMemberDAO.getInstance().findByProjectAndDepartment(dpr.getProject(), pmDept);
		for (ProjectMember mbr : members) {
			if (mbr.getEmployment().getRole().getListPriority() > 0) {
				Contact contact = mbr.getEmployment().getContact();
				ReportTime tc = createCrewTimeCard(contact, 1, dpr, dpr.getCrewCall());
				tc.setDepartment(dept);
				tc.setRole((mbr.getEmployment().getRole().getName()));
				tc.setListPriority((mbr.getEmployment().getRole().getListPriority().shortValue()));
				tcList.add(tc);
			}
		}
	}

	/**
	 * Create a List of TimeCard`s to be associated with a particular ExhibitG.
	 * The date of the given ExhibitG should already be set. The List of
	 * TimeCard`s is created using information from the CallSheet`s of the same
	 * date.
	 */
//	private static List<TimeCard> createCrewTimeCards(Date date, ExhibitG exhibitG) {
//		List<TimeCard> timeCardCrewList;
//		Project project = SessionUtils.getCurrentProject();
//
//		timeCardCrewList = new ArrayList<TimeCard>();
//
//		List<Callsheet> sheets = CallsheetDAO.getInstance().findByDateAndProject(date, project);
//		if (sheets.size() == 0) {
//			return timeCardCrewList;
//		}
//		// There may be multiple Callsheet`s if the project has multiple Unit`s.
//		Map<Integer, TimeCard> enteredCrewTimes = new HashMap<Integer, TimeCard>();
//		for (Callsheet callsheet : sheets) {
//			// For Crew
//			for (DeptCall dc : callsheet.getDeptCalls()) {
//				for (CrewCall cc : dc.getCrewCalls()) {
//					addCrewTimecard(cc, dc, enteredCrewTimes, callsheet.getUnitNumber());
//				}
//			}
//		}
//		timeCardCrewList.addAll(enteredCrewTimes.values());
//		for (TimeCard tc : timeCardCrewList) {
//			tc.setExhibitG(exhibitG);
//		}
//		return timeCardCrewList;
//	}

	/**
	 * Create a TimeCard to match the given CrewCall entry, then add it to the
	 * 'enteredTimes' Map, unless the same contact is already represented, in
	 * which case we will update it if the new entry has an earlier call time.
	 * @param castCall
	 * @param enteredTimes
	 */
//	private static void addCrewTimecard(CrewCall cc, DeptCall dc, Map<Integer, TimeCard> enteredTimes,
//			Integer unitNumber) {
//		Contact contact = null;
//		TimeCard tc;
//		try {
//			if (cc.getContactId() != null) {
//				contact = ContactDAO.getInstance().findById(cc.getContactId());
//			}
//			if (contact == null) {
//				List<Contact> contactList = ContactDAO.getInstance().findByDisplayName(cc.getName());
//				if (contactList.size() > 0) {
//					contact = contactList.get(0);
//				}
//			}
//			if (contact != null) {
//				log.debug("crew contact: " + contact.getUser().getLastNameFirstName());
//				tc = createTimeCard(contact, unitNumber);
//				tc.setDtype(TimeCard.DTYPE_CREW);
//				tc.setDepartment(dc.getDepartment());
//				tc.setRole(cc.getRoleName());
//				tc.setReportSet(cc.getTime());
//				if (! enteredTimes.containsKey(contact.getId())) {
//					enteredTimes.put(contact.getId(), tc);
//				}
//				else { // duplicate entry, use earlier one
//					TimeCard priorTc = enteredTimes.get(contact.getId());
//					if (priorTc.getReportSet().after(cc.getTime())) {
//						// current one is earlier, replace the prior one:
//						enteredTimes.put(contact.getId(), tc);
//					}
//				}
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//	}

}
