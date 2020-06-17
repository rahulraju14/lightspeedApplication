package com.lightspeedeps.dao;

import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.object.ProjectBundle;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A data access object (DAO) providing persistence and search support for
 * Project entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Project
 */
public class ProjectDAO extends BaseTypeDAO<Project> {
	private static final Log log = LogFactory.getLog(ProjectDAO.class);
	// property constants
	private static final String TITLE = "title";
	private static final String PRODUCTION = "production";
	public static final String EPISODE = "episode";
//	private static final String STATUS = "status";
	private static final String STRIPS_LOCKED_BY = "stripsLockedBy";
//	private static final String NOTIFYING = "notifying";
//	private static final String SCRIPT_TEXT_ACCESSIBLE = "scriptTextAccessible";
//	private static final String FILE_REPOSITORY_ENABLED = "fileRepositoryEnabled";
//	private static final String ALL_USERS_READ_FILES = "allUsersReadFiles";
//	private static final String AUTO_REFRESH_CALL_SHEET = "autoRefreshCallSheet";
//	private static final String TIME_ZONE = "timeZone";
	public static final String APPROVER = "approver";

	public static ProjectDAO getInstance() {
		return (ProjectDAO)getInstance("ProjectDAO");
	}

	/*** Find all Projects for the current Production. */
	public List<Project> findByProduction() {
		return findByProduction(SessionUtils.getProduction());
	}

	/**
	 * Find all Projects for the specified Production.
	 *
	 * @param prod The Production of interest.
	 * @return A non-empty List of Projects (since every Production has at least
	 *         one project).
	 */
	public List<Project> findByProduction(Production prod) {
		return findByProperty(PRODUCTION, prod);
	}

	/**
	 * Find all Projects for the current Production and specified title.
	 *
	 * @param prod The Production of interest.
	 * @param title The project title to match on.
	 * @return A non-null, but possibly empty List of Projects. The list should
	 *         not contain more than one entry, since the title field should be
	 *         unique within a Production.
	 */
	public List<Project> findByProductionAndTitle(String title) {
		return findByProductionAndTitle(SessionUtils.getProduction(), title);
	}

	/**
	 * Find all Projects for the specified Production and title.
	 *
	 * @param prod The Production of interest.
	 * @param title The project title to match on.
	 * @return A non-null, but possibly empty List of Projects. The list should
	 *         not contain more than one entry, since the title field should be
	 *         unique within a Production.
	 */
	@SuppressWarnings("unchecked")
	public List<Project> findByProductionAndTitle(Production prod, String title) {
		Object[] values = {prod, title};
		return find("from Project where " +
				PRODUCTION + " = ? and " +
				TITLE + " = ?", values);
	}

	/**
	 * Find all Projects for the specified Production and episode (job number
	 * for commercials).
	 *
	 * @param prod The Production of interest.
	 * @param episode The desired episode (project id or commercial job number).
	 * @return A non-null, but possibly empty List of Projects.
	 */
	@SuppressWarnings("unchecked")
	public List<Project> findByProductionAndEpisode(Production prod, String episode) {
		Object[] values = {prod, episode};
		return find("from Project where " +
				PRODUCTION + " = ? and " +
				EPISODE + " = ?", values);
	}

	@SuppressWarnings("unchecked")
	public List<Project> findByStartFormContact(Contact contact) {
		String queryString = "select distinct p from StartForm sf, Project p where " +
				" sf.contact = ? and " +
				" sf.project = p ";
		return find(queryString, contact);
	}

	/**
	 * Add a new Project to the database, along with a number of required,
	 * related objects.
	 *
	 * @param newProject The new Project to be added.
	 * @param sourceProject An optional Project to be used as the source for
	 *            preferences and/or project members to copy to the new project.
	 *            This may be null.
	 * @param startDate The starting date (for shooting) to be put into the new
	 *            Project's schedule.
	 * @param copyPreferences True if project preferences (in particular, task
	 *            assignments) are to be copied from 'project' to 'newProject'.
	 * @param copyMembers True if the list of projectMembers from 'project'
	 *            should be duplicated and added for the new Project. If this is
	 *            false, and 'newProduction' is also false, then the current
	 *            user will be added as a ProjectMember to the new project, with
	 *            the same role as he has in the current Project.
	 * @param newProduction True if this Project is being created as part of a
	 *            new Production, in which case no ProjectMembers are created
	 *            for it.
	 * @param copyOnbPackets
	 * @param copyOnbApproversPaths
	 * @param copyTcApprovers
	 * @return The Project object, refreshed from the database after adding all
	 *         the related objects. This will include at least one Unit. If the
	 *         Production is a cross-project production (e.g., TV Series), then
	 *         the new Project will be given as many units (with matching names)
	 *         as the source Project.
	 */
	@Transactional
	public boolean createNewProject(Project newProject, Project sourceProject, Date startDate,
			boolean copyPreferences, boolean copyMembers, boolean newProduction,
			boolean copyTcApprovers, boolean copyOnbApproversPaths, boolean copyOnbPackets) {
		boolean bRet = false;
		try {
			log.debug("");
			Production prod = newProject.getProduction();
			int unitNum = 1;
			String unitName = Constants.DEFAULT_UNIT_NAME;
			boolean commercial = false; 	// true if commercial production
			if (sourceProject != null) {
				// copy the source project's first unit name and number
				unitNum = sourceProject.getUnits().get(0).getNumber();
				unitName = sourceProject.getUnits().get(0).getName();
				//Is this the reason for not copying startforms in new projects in other productions.
				if (prod.getType().isAicp()) {
					commercial = true;
				}
			}
			if(prod.getType().isCanadaTalent()) {
				CanadaProjectDetail canadaProjectDetail = new CanadaProjectDetail();
//DH 20190612	CanadaProjectDetailDAO.getInstance().save(canadaProjectDetail); // avoid creating CPD which will get orphaned immediately
				newProject.setCanadaProjectDetail(canadaProjectDetail);

			//LS-2356
				UdaProjectDetail udaProjectDetail = new UdaProjectDetail();
				//UdaProjectDetailDAO.getInstance().save(udaProjectDetail);
				newProject.setUdaProjectDetail(udaProjectDetail);
			}
			newProject.setSequence(findMaxSequence(prod) + 1);
			if (StringUtils.isEmpty(newProject.getEpisode())) {
				newProject.setEpisode("" + newProject.getSequence());
			}

			Unit unit = new Unit(newProject, unitNum, unitName);
			List<Unit> units = new ArrayList<>();
			units.add(unit);
			newProject.setUnits(units);
			ProjectSchedule schedule = new ProjectSchedule();
			schedule.setStartDate(startDate);
			unit.setProjectSchedule(schedule); // schedule is saved via cascade

			Date today = CalendarUtils.todaysDate();
			Contact contact = SessionUtils.getCurrentContact();

			// create ReportRequirement instances for the new project
			copyRequirements(newProject, sourceProject, copyPreferences, copyMembers,
					newProduction, contact, today);

			Map<Integer, Department> deptMap = null;
			Map<Integer, Employment> empMap = null;
			if (sourceProject != null) {
				if (commercial && (copyMembers || copyPreferences)) {
					// need to duplicate any custom-named departments for the new Project
					deptMap = copyDepartments(sourceProject, newProject);
				}
				if (copyPreferences) {
					if (prod.getType().isCanadaTalent()) {
						CanadaProjectDetail cpd;
						cpd = sourceProject.getCanadaProjectDetail().deepCopy();
						newProject.setCanadaProjectDetail(cpd);
						//LS-2356
						UdaProjectDetail upd;
						if (sourceProject != null && sourceProject.getUdaProjectDetail() != null) {
							upd = sourceProject.getUdaProjectDetail().deepCopy();
							newProject.setUdaProjectDetail(upd);
						}
					}
					newProject.setDeptMask(sourceProject.getDeptMask());
					newProject.setDaysHeldBeforeDrop(sourceProject.getDaysHeldBeforeDrop());
					newProject.setUseHoldDrop(sourceProject.getUseHoldDrop());
					newProject.setNotifying(sourceProject.getNotifying());
					if (sourceProject.getNotifying()) {
						newProject.setNotificationChanged(new Date());
					}
					newProject.setScriptTextAccessible(sourceProject.getScriptTextAccessible());
				}
				else { // if not copying preferences, use Production's department mask as default
					newProject.setDeptMask(prod.getDeptMaskB());
				}

				if (copyMembers) {
					empMap = copyProjectMembers(sourceProject, unit, deptMap, commercial);
				}
				else { // at least add current user
					ProjectMember member;
					List<ProjectMember> pms = ProjectMemberDAO.getInstance().findByContactAndProject(contact, sourceProject);
					if (pms.size() > 0) { // Only empty in some WebService cases
						member = pms.get(0);
						addNewMember(unit, contact, member);
					}
				}
			}

			// For Commercial productions, create a PayrollPreference instance
			if (prod.getType().hasPayrollByProject()) {
				PayrollPreference pref;
				if (copyPreferences && (sourceProject != null) && (sourceProject.getPayrollPref() != null)) {
					// source should always have a payroll pref!
					pref = sourceProject.getPayrollPref().deepCopy();
				}
				else { // not copying preferences
					pref = prod.getPayrollPref().deepCopy();
				}
				// LS-1709 set mileage rate to current year's IRS specification
				pref.setMileageRate(TimecardUtils.findMileageRate(new Date()));
				newProject.setPayrollPref(pref);
			}

			// Save the Project, and, via Hibernate cascade, the new ProjectMembers,
			// Departments, Roles, and PayrollPreference.
			save(newProject);

			if (commercial && copyMembers) { // need Starts copied to new project
				copyStarts(sourceProject, newProject, empMap, deptMap, startDate);
			}

			// For cross-project productions, duplicate all remaining units
			if (sourceProject != null && prod.getType().getCrossProject()) {
				boolean first = true;
				for (Unit un : sourceProject.getUnits()) {
					if (! first) {
						createUnit(newProject, un.getNumber(), startDate, un.getName(), today);
					}
					first = false;
				}
			}

			String desc = newProject.getTitle() + " (id=" + newProject.getId() + ")";
			ChangeUtils.logChange(ChangeType.PROJECT, ActionType.CREATE, newProject, desc);
			if (prod.getAllowOnboarding() && prod.getType().hasPayrollByProject()) {
				// Copy Approval Paths
				if (copyOnbApproversPaths && sourceProject != null) {
					log.debug("Copy Approval Paths");
					boolean ret = ApprovalPathDAO.getInstance().copyApprovalPaths(prod, sourceProject, newProject);
					if (! ret) {
						//message 'copying one or more default Approval Paths failed due to a system error'
						MsgUtils.addFacesMessage("ApprovalPath.CopyApprovalPathFailed", FacesMessage.SEVERITY_ERROR);
					}
				}
				else {
					log.debug("Create Approval Paths");
					boolean ret = ApprovalPathDAO.getInstance().generateApprovalPaths(prod, newProject);
					if (! ret) {
						//message 'creating one or more default Approval Paths failed due to a system error'
						MsgUtils.addFacesMessage("ApprovalPath.GenerateApprovalPathFailed", FacesMessage.SEVERITY_ERROR);
					}
				}
				// Copy Onboarding packets
				if (copyOnbPackets && sourceProject != null) {
					log.debug("Copy Onboarding packets");
					boolean ret = DocumentService.copyOnboardingPackets(prod, sourceProject, newProject);
					if (! ret) {
						//message 'copying one or more packets failed due to a system error'
						MsgUtils.addFacesMessage("ApprovalPath.CopyOnboardingPacketsFailed", FacesMessage.SEVERITY_ERROR);
					}
				}
			}
			// Copy Timecard Approvers
			if (copyTcApprovers && sourceProject != null) {
				log.debug("Copy Timecard Approvers");
				// Copy Project Approvers
				Approver approver = ApproverUtils.copyApproverChain(sourceProject);
				if (approver != null) {
					newProject.setApprover(approver);
					ProjectDAO.getInstance().merge(newProject);
				}
				else if (approver == null && sourceProject.getApprover() != null) {
					//message 'copying one or more Project approvers failed due to a system error'
					MsgUtils.addFacesMessage("ApprovalPath.CopyTimecardProjectApproversFailed", FacesMessage.SEVERITY_ERROR);
				}

				// Copy Department Approvers
				List<ApprovalAnchor> anchorList = ApprovalAnchorDAO.getInstance().findByProperty("project", sourceProject);
				boolean ret = true;
				for (ApprovalAnchor anchor : anchorList) {
					ApprovalAnchor newAnchor = new ApprovalAnchor();
					newAnchor.setProduction(anchor.getProduction());
					newAnchor.setProject(newProject);
					newAnchor.setDepartment(anchor.getDepartment());
					approver = null;
					approver = ApproverUtils.copyApproverChain(anchor);
					if (approver != null) {
						newAnchor.setApprover(approver);
					}
					Integer newId = ApprovalAnchorDAO.getInstance().save(newAnchor);
					log.debug("New anchor = " + newId);
					ret &= (newId != null);
					log.debug("Ret = " + ret);
				}
				if (! ret) {
					//message 'copying one or more default Department approvers failed due to a system error'
					MsgUtils.addFacesMessage("ApprovalPath.CopyTimecardDeptApproversFailed", FacesMessage.SEVERITY_ERROR);
				}
			}
			bRet = true;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * @param newProject The new Project to be added.
	 * @param sourceProject An optional Project to be used as the source for
	 *            preferences and/or project members to copy to the new project.
	 *            This may be null.
	 * @param copyPreferences True if project preferences (in particular, task
	 *            assignments) are to be copied from 'project' to 'newProject'.
	 * @param copyMembers True if the list of projectMembers from 'project'
	 *            should be duplicated and added for the new Project. If this is
	 *            false, and 'newProduction' is also false, then the current
	 *            user will be added as a ProjectMember to the new project, with
	 *            the same role as he has in the current Project.
	 * @param newProduction True if this Project is being created as part of a
	 *            new Production, in which case no ProjectMembers are created
	 *            for it.
	 * @param contact The current (logged-in User) Contact.
	 * @param today A Date object with the current date and time.
	 */
	private void copyRequirements(Project newProject, Project sourceProject,
			boolean copyPreferences, boolean copyMembers, boolean newProduction, Contact contact,
			Date today) {
		List<ReportRequirement> reportRequirements = null;
		if (sourceProject != null) {
			reportRequirements = sourceProject.getReportRequirements();
		}
		else {
			Project firstProject = findById(1);
			if (firstProject != null) {
				reportRequirements = firstProject.getReportRequirements();
			}
			else {
				EventUtils.logError("Missing default project (id=1) for ReportRequirement source.");
				reportRequirements = new ArrayList<>();
			}
		}

		List<ReportRequirement> newReportRequirements = new ArrayList<>();
		ReportRequirement newReq;
		Integer contactId = null;
		if (! copyMembers && contact != null) {
			contactId = contact.getId();
		}
		for (ReportRequirement requirement : reportRequirements) {
			if (requirement.getUnitNumber().intValue() == 1) {
				if (copyPreferences && (! newProduction) &&
						(copyMembers || requirement.getContact() == null ||
						requirement.getContact().getId().equals(contactId))) {
					newReq = new ReportRequirement( requirement.getRole(), requirement.getContact(),
							newProject, requirement.getType(), requirement.getDescription(),
							requirement.getFrequency(), today);
				}
				else {
					newReq = new ReportRequirement( null, null, // keep req's, but no responsible party
							newProject, requirement.getType(), requirement.getDescription(),
							requirement.getFrequency(), today);
				}
				newReportRequirements.add(newReq);
			}
		}
		newProject.setReportRequirements(newReportRequirements); // saved via cascade
	}

	/**
	 * Create new ProjectMember instances to match the ones on the existing
	 * project.
	 *
	 * @param sourceProject The old Project; the ProjectMember entries for the
	 *            "main" (first) unit of this project will be copied to the new
	 *            project.
	 * @param targetUnit The Unit to which the new ProjectMember entries will be
	 *            added.
	 * @param deptMap A mapping from old custom department ids to new
	 *            Departments. This will only have an entry for departments that
	 *            are "unique" -- custom-named departments -- which are the ones
	 *            that apply only to a single project within a commercial
	 *            production. This parameter is null for non-commercial
	 *            productions.
	 * @param commercial True if the production is a Commercial production.
	 * @return For Commercial productions, a Map from the id of an existing
	 *         Employment instance, to the new copy of the Employment matching
	 *         the target Unit/Project. For other production types, null is
	 *         returned.
	 */
	@SuppressWarnings("null")
	private Map<Integer, Employment> copyProjectMembers(Project sourceProject, Unit targetUnit, Map<Integer, Department> deptMap, boolean commercial) {
		// Copy project members from "source" project's main unit
		Set<ProjectMember> newMembers = new HashSet<>();
		Map<Integer, Employment> empMap = null;
		if (commercial) {
			empMap = new HashMap<>();
		}
		ProjectMember newMember;
		Set<ProjectMember> members = sourceProject.getMainUnit().getProjectMembers();
		for (ProjectMember member : members) {
			if (member.getUnit() != null ) { // skip if production-wide role
				Role role = member.getEmployment().getRole();
				if (deptMap != null && role.getDepartment().isUnique()) {
					Department dept = deptMap.get(role.getDepartment().getId());
					if (dept != null) { // it should be non-null!!
						Role newRole = role.clone();
						newRole.setProduction(role.getProduction());
						newRole.setDepartment(dept);
						role = newRole;
						// The new Role(s) will be saved (via cascade) when the Project is saved.
					}
				}
				Employment employment;
				if (commercial) {
					employment = new Employment(role, role.getName(), member.getEmployment().getContact(), member.getEmployment().getPermissionMask());
					employment.setProject(targetUnit.getProject());
					employment.setDefRole(member.getEmployment().getDefRole());
					save(employment);
					empMap.put(member.getEmployment().getId(), employment);
				}
				else {
					employment = member.getEmployment();
				}
				newMember = new ProjectMember(targetUnit, employment);
				newMembers.add(newMember);
			}
		}
		targetUnit.setProjectMembers(newMembers);
		return empMap;
	}

	/**
	 * Create copies of any unique (custom-named) projects for Commercial
	 * productions.
	 *
	 * @param sourceProject The existing Project whose custom departments should
	 *            be replicated.
	 * @param newProject The project to which the copies will be added.
	 * @return A mapping from old custom department ids to new Departments. This
	 *         will only have an entry for departments that are "unique" --
	 *         custom-named departments -- which are the ones that apply only to
	 *         a single project within a commercial production.
	 */
	private Map<Integer, Department> copyDepartments(Project sourceProject, Project newProject) {
		Map<Integer, Department> deptMap = new HashMap<>();
		Production prod = sourceProject.getProduction();
		List<Department> list = DepartmentDAO.getInstance().findByProductionCustom(prod, sourceProject);
		for (Department dept : list) {
			Department newdept = dept.clone();
			newdept.setProduction(prod);
			newdept.setProject(newProject);
			deptMap.put(dept.getId(), newdept);
			// The new department(s) will be saved (via cascade) when the Project is saved.
		}

		return deptMap;
	}

	/**
	 * Create copies of the StartForm`s from the source Project and add them for
	 * the new Project. This is only applicable to a Commercial Production.
	 *
	 * @param sourceProject The existing Project whose custom departments should
	 *            be replicated.
	 * @param newProject The project to which the copies will be added.
	 * @param empMap A mapping from old Employment ids to new instances of
	 *            Employment created earlier in the copy process. This is never
	 *            null.
	 * @param deptMap A mapping from old custom department ids to new
	 *            Departments. This will only have an entry for departments that
	 *            are "unique" -- custom-named departments -- which are the ones
	 *            that apply only to a single project within a commercial
	 *            production.
	 * @param startDate The workStartDate to be set in the new StartForm`s.
	 */
	private void copyStarts(Project sourceProject, Project newProject, Map<Integer, Employment> empMap,
			Map<Integer, Department> deptMap, Date startDate) {

		// Get all StartForm`s for the source project; later code will
		// filter this to only do ones that have an associated NEW Employment instance.
		List<StartForm> list = StartFormDAO.getInstance().findByProject(sourceProject);

		// Then figure out which ones are obsolete - superseded by a Change form
		Set<Integer> obsoleteSfIds = new HashSet<>();
		for (StartForm sf : list) {
			if (sf.getPriorFormId() != null) {
				// by definition, if an SF is a "prior form", it's obsolete
				obsoleteSfIds.add(sf.getPriorFormId());
			}
		}

		Date today = CalendarUtils.todaysDate();
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		Production prod = SessionUtils.getNonSystemProduction();
		Document payrollDocument = null;
		DocumentChain payrollChain = null;
		if (prod != null && prod.getAllowOnboarding() && !prod.getType().isTalent()) {
			payrollDocument = DocumentDAO.findPayrollDocument(prod);
			payrollChain = DocumentChainDAO.getInstance().findById(payrollDocument.getDocChainId());
		}
		for (StartForm sf : list) {
			if (! obsoleteSfIds.contains(sf.getId()) && sf.getEmployment() != null) {
				// Only copy the ones that are NOT obsolete.
				// Only copy ones with a related Employment instance.
				Integer empId = sf.getEmployment().getId();
				Employment emp = empMap.get(empId);
				if (emp != null) {
					// Only copy Starts related to an Employment that has been copied.
					StartForm newsf = sf.deepCopy(); // get rates, etc. (more than clone())
					newsf.setFormType(StartForm.FORM_TYPE_NEW);
					newsf.setProject(newProject);
					// set dates; leave hire date unchanged
					newsf.setCreationDate(today);
					newsf.setWorkStartDate(startDate);
					newsf.setWorkEndDate(null); // clear work end date - not required
					newsf.setEffectiveStartDate(null); // clear effective dates (not required)
					newsf.setEffectiveEndDate(null);
					newsf.setJobName(newProject.getTitle());
					newsf.setJobNumber(newProject.getEpisode());
					newsf.setEmployment(emp);
					/*
					DH - In copyProjectMembers() we copy only the project members of main unit,
						Set<ProjectMember> members = sourceProject.getMainUnit().getProjectMembers();
						So When an employment has two project members one for main unit and second
						for another unit therefore that employment will be in the empMap
						then it will copy startForms for both the project members,
						For the new startForm of other unit it will set the employment id of the original
						startForm therefore we are setting it with null.
					AM - I re-did this to only copy Starts that have an Employment in the empMap, and
						if so, associate the new Start with the new Employment. I hope this is right!
					*/
					// Role/dept info (in Employment) was already updated, in copyProjectMembers.
					int nextSeq = startFormDAO.findMaxSequence(sf.getContact()) + 1;
					newsf.setSequence(nextSeq);
					newsf.setFormNumber(sf.getContact().getId() + "-" + nextSeq);
					save(newsf);

					// Creating contact documents for each start form
					if (prod != null && prod.getAllowOnboarding() && payrollDocument != null) {
						startFormDAO.createContactDocument(newsf, payrollDocument, payrollChain, ApprovalStatus.PENDING);
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public int findMaxSequence(Production prod) {
		String queryString = "select max(sequence) from Project p where " +
				PRODUCTION + " = ? ";
		int sequence = 0;
		List<Object> list = find(queryString, prod);
		if (list != null && list.size() > 0 && list.get(0) instanceof Integer) {
			Integer iSeq = (Integer)list.get(0);
			if (iSeq != null) {
				sequence = iSeq.intValue();
			}
		}
		return sequence;
	}


	/**
	 * Creates one or more (new) Unit instances for the given Project, with a
	 * unit number one higher than the current highest unit number in the given
	 * Project, and also creates the necessary associated objects for each Unit
	 * created, including the ProjectSchedule, ReportRequirement`s,
	 * ProjectMember (if necessary), and UnitStripboard`s.
	 * <p>
	 * The currently logged in Contact will be made a member of all new Unit`s
	 * created. If the contact has a production-wide role (e.g., Prod Data
	 * Admin), then this happens "automatically". If the contact has a role
	 * which is not production-wide, then a new ProjectMember instance is
	 * created such that the contact will have a Role in the new Unit matching
	 * (an arbitrary) one of their Roles in the given Project.
	 *
	 * @param project The Project which is to have a new Unit added.
	 * @param startDate The startDate to place in the ProjectSchedule`s.
	 * @param name The name to assign to the Unit`s; if null, a name is
	 *            generated of the form "Unit#<n>".
	 * @return The newly-created Unit which was added to the given Project. Note
	 *         that if the Production (of the given Project) is a cross-project
	 *         type, e.g., a TELEVISION_SERIES, then a new Unit will be added to
	 *         EVERY Project in the Production.
	 */
	@Transactional
	public Unit createNewUnit(Project project, Date startDate, String name) {
		Unit unit = null;
		Production prod = project.getProduction();
		if (prod.getType().getCrossProject()) {
			unit = createAllUnits(project, startDate, name);
		}
		else {
			unit = createOneUnit(project, startDate, name);
		}
		return unit;
	}

	/**
	 * Creates (new) Unit instances for all Project`s that are in the Production
	 * of the given Project, with a unit number one higher than the current
	 * highest unit number in the given Project, and also creates the necessary
	 * associated objects for each Unit created, including the ProjectSchedule,
	 * ReportRequirement`s, ProjectMember (if necessary), and UnitStripboard`s.
	 *
	 * @param project The Project whose Production is to have a new Unit added;
	 *            all Project`s in the Production will have a new Unit created.
	 * @param startDate The startDate to place in the ProjectSchedule`s.
	 * @param name The name to assign to the Unit`s; if null, a name is
	 *            generated of the form "Unit#<n>".
	 * @return The newly-created Unit which was added to the given Project.
	 */
	private Unit createAllUnits(Project project, Date startDate, String name) {
		Unit rUnit = null;
		try {
			project = refresh(project);
			Production prod = project.getProduction();
			if (project.getUnits().size() >= Constants.MAX_UNITS) {
				return null;
			}
			int number = findMaxUnit(project) + 1;
			if (name == null) {
				name = "Unit#" + number;
			}
			prod = ProductionDAO.getInstance().refresh(prod);
			Date today = new Date();
			Set<Project> projects = prod.getProjects();
			for(Project proj : projects) {
				Unit unit = createUnit(proj, number, startDate, name, today);
				if (unit == null) {
					EventUtils.logError("Add Unit failed due to createUnit error.");
					break;
				}
				if (proj.equals(project)) {
					rUnit = unit;
				}
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			rUnit = null;
		}
		return rUnit;
	}

	/**
	 * Creates a (new) Unit instance for the given Project, with a unit number
	 * one higher than the current highest unit number in that Project, and also
	 * creates the necessary associated objects including the ProjectSchedule,
	 * ReportRequirement`s, ProjectMember (if necessary), and UnitStripboard`s.
	 *
	 * @param project The Project to which the Unit will be added.
	 * @param startDate The startDate to place in the ProjectSchedule.
	 * @param name The name to assign to the Unit; if null, a name is generated
	 *            of the form "Unit#<n>".
	 * @return The newly-created Unit, or null if there was an exception thrown.
	 */
	private Unit createOneUnit(Project project, Date startDate, String name) {
		Unit unit = null;
		try {
			attachDirty(project);
			project = refresh(project);
			if (project.getUnits().size() >= Constants.MAX_UNITS) {
				return null;
			}
			int number = findMaxUnit(project) + 1;
			if (name == null) {
				name = "Unit#" + number;
			}
			Date today = new Date();
			unit = createUnit(project, number, startDate, name, today);
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			unit = null;
		}
		return unit;
	}

	/**
	 * Creates a (new) Unit instance, with the specified unit number, for the
	 * given project, and also creates the necessary associated objects
	 * including the ProjectSchedule, ReportRequirement`s, ProjectMember (if
	 * necessary), and UnitStripboard`s.
	 * <p>
	 * The current (logged in) Contact will be added to the new Unit using a
	 * randomly selected role (if the contact has more than one role), unless
	 * the role selected is a production-wide role.
	 *
	 * @param project The Project to which the Unit will be added.
	 * @param number The unit number to be assigned to the new Unit.
	 * @param startDate The startDate to place in the ProjectSchedule.
	 * @param name The name to assign to the Unit; if null, a name is generated
	 * @param reportDate The date, usually today's date, to be placed in any
	 *            ReportRequirement instances created.
	 * @return The newly created Unit or null if there was an exception thrown.
	 */
	private Unit createUnit(Project project, int number, Date startDate, String name, Date reportDate) {
		Unit unit = null;
		try {
			Contact contact = SessionUtils.getCurrentContact();
			ProjectMember member = ProjectMemberDAO.getInstance()
					.findByContactAndProject(contact, project).get(0);
			//project = refresh(project);

			unit = new Unit(project, number, name);
			project.getUnits().add(unit);
			ProjectSchedule schedule = new ProjectSchedule();
			schedule.setStartDate(startDate);
			unit.setProjectSchedule(schedule); // schedule is saved via cascade

			List<ReportRequirement> newReportRequirements = new ArrayList<>();
			ReportRequirement newReq;
			for (ReportRequirement requirement : project.getReportRequirements()) {
				if (requirement.getType().isForUnit() && requirement.getUnitNumber()==1) {
					newReq = new ReportRequirement( null, null, // keep req's, but no responsible party
							project, requirement.getType(), requirement.getDescription(),
							requirement.getFrequency(), reportDate);
					newReq.setUnitNumber(number);
					newReportRequirements.add(newReq);
				}
			}
			project.getReportRequirements().addAll(newReportRequirements);

			addNewMember(unit, contact, member);

			// Create a new UnitStripboard for each Stripboard in the Project.
			for (Stripboard sb : project.getStripboards()) {
				UnitStripboard usb = new UnitStripboard(unit, sb);
				sb.getUnitSbs().add(usb);
				attachDirty(sb); // UnitStripboard saved via cascade
			}
			save(unit);
			attachDirty(project);
			ChangeUtils.logChange(ChangeType.UNIT, ActionType.CREATE, project,
					"" + unit.getNumber() + "=" + unit.getName());
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			unit = null;
		}
		return unit;
	}

	/**
	 * Create a new ProjectMember instance and add it to the given Unit, for the
	 * given Contact, with the same Role as the given ProjectMember. No database
	 * updates are done by this method.
	 * <p>
	 * A new collection of ProjectMember`s is created and attached to the given
	 * Unit. (The collection is initially empty.) The new ProjectMember entry is
	 * created and added to the collection, unless the supplied ProjectMember
	 * instance has a role which is production-wide. In that case, no new
	 * ProjectMember instance is created.
	 *
	 * @param unit The Unit to be given a ProjectMember, contained in a new
	 *            collection.
	 * @param contact The Contact to be used to create the new ProjectMember.
	 * @param member The role of this ProjectMember is used to create a new
	 *            ProjectMember, unless it is a production-wide role.
	 */
	public void addNewMember(Unit unit, Contact contact, ProjectMember member) {
		// Just add current user as a Unit project member
		Set<ProjectMember> newMembers = new HashSet<>();
		if (member != null && member.getUnit() != null ) { // skip if production-wide role
			Role role = member.getEmployment().getRole();
			String occupation = null;

			if(role != null) {
				occupation = role.getName();
			}

			Employment employment = new Employment(role, occupation, member.getEmployment().getContact(), member.getEmployment().getPermissionMask());
			employment.setProject(unit.getProject());
			ProjectMember newMember =
					new ProjectMember( unit, employment);
			newMembers.add(newMember);
		}
		unit.setProjectMembers(newMembers);
	}

	private int findMaxUnit(Project project) {
		int num = 0;
		for (Unit u : project.getUnits()) {
			if (u.getNumber() > num) {
				num = u.getNumber();
			}
		}
		return num;
	}

	/**
	 * Delete a project and any associated child objects. This also changes the default project for
	 * any user who had this project as their default. Associated objects that will be deleted are
	 * ProjectMembers, Notifications, ScriptElements, Scripts, Stripboards, Changes, Events,
	 * Callsheets, DPRs, ExhibitGs, TimeSheets, TimeCards, ReportRequirements and ProjectSchedule.
	 * Also, for Commercial productions, StartForms associated this project will be deleted.
	 *
	 * @param project The project to be completely removed from the database.
	 */
	@Transactional
	public void remove(Project project, boolean updateContacts) {
		log.debug("");
		// First update user's default project, & remove project memberships
		final ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
		List<ScriptElement> seList = scriptElementDAO.findByProject(project);
		final NotificationDAO notificationDAO = NotificationDAO.getInstance();
		List<Notification> noteList = notificationDAO.findByProperty("project", project);
		final ExhibitGDAO exhibitGDAO = ExhibitGDAO.getInstance();
		List<ExhibitG> exList = exhibitGDAO.findByProject(project);
		final CallsheetDAO callsheetDAO = CallsheetDAO.getInstance();
		List<Callsheet> csList = callsheetDAO.findByProject(project);

		// For Commercial productions, we may have project-specific custom departments...
		final DepartmentDAO departmentDAO = DepartmentDAO.getInstance();
		List<Department> dpList = departmentDAO.findByProperty(DepartmentDAO.PROJECT, project);

		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		List<StartForm> sfList = startFormDAO.findByProject(project);
		List<ContactDocument> cdList = ContactDocumentDAO.getInstance().findByProperty("project", project);
		Map<String, Object> values = new HashMap<>();
		values.put("production", project.getProduction());
		values.put("project", project);
		List<Packet> packetList = PacketDAO.getInstance().findByNamedQuery(Packet.GET_PACKET_LIST_BY_PRODUCTION_PROJECT, values);
		List<ApprovalPath> approvalPathList = ApprovalPathDAO.getInstance().findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, map("project", project));
		List<Notification> notificationList = NotificationDAO.getInstance().findByProperty("project", project);
		List<DprEpisode> dprEpisodeList = DprEpisodeDAO.getInstance().findByProject(project);

		if (updateContacts) {
			changeContactProject(project);
		}

		// Then delete all the ScriptElements in the project
		for (ScriptElement se : seList) {
			scriptElementDAO.delete(se);
		}
		seList = null;

		// Delete Notifications generated for this project
		for (Notification note : noteList) {
			notificationDAO.delete(note);
		}
		noteList = null;

		// Delete Exhibit G reports generated for this project
		for (ExhibitG item : exList) {
			delete(item);
		}
		exList = null;

		// Update Call Sheet reports linked to this project
		for (Callsheet cs : csList) {
			for (Project p : cs.getProjects()) {
				if (! p.equals(project)) {
					p.getCallsheets().remove(cs);
					attachDirty(p);
				}
			}
			delete(cs);
		}
		csList = null;
		project.getCallsheets().clear();

		for (DprEpisode de : dprEpisodeList) {
			Dpr dpr = de.getDpr();
			dpr.getDprEpisodes().remove(de);
			attachDirty(dpr);
			delete(de);
		}
		dprEpisodeList = null;

		// Get list of custom (unique) departments; these will be deleted shortly.
		// This is applicable only for Commercial productions.
		Set<Integer> deptIds = new HashSet<>(); // unique departments' ids.
		for (Department dp : dpList) {
			deptIds.add(dp.getId());
		}

		boolean aicp = project.getProduction().getType().isAicp();

		Set<Role> customRoles = new HashSet<>();
		if (aicp) {
			Set<Employment> emps = new HashSet<>();
			for (Unit u : project.getUnits()) {
				for (ProjectMember pm : u.getProjectMembers()) {
					emps.add(pm.getEmployment());
				}
			}

			for (Employment emp : emps) {
				Role role = emp.getRole();
				if (deptIds.contains(role.getDepartment().getId())) {
					// Role belongs to a unique department -- will need to be deleted
					customRoles.add(role);
				}
				delete(emp);
			}
			emps = null;
		}

		// Delete StartForms associate with this project
		// (the returned list will be empty for non-Commercial productions)
		for (StartForm sf : sfList) {
			delete(sf);
		}
		sfList = null;

		for (Unit u : project.getUnits()) {
			for (ProjectMember pm : u.getProjectMembers()) {
				delete(pm);
			}
			u.getProjectMembers().clear();
			delete(u);
		}

		// delete the custom roles, unique to this project (for Commercial productions)
		for (Role role : customRoles) {
			delete(role);
		}
		customRoles = null;

		// delete the unique Departments of this project (for Commercial productions)
		for (Department dp : dpList) {
			delete(dp);
		}
		dpList = null;
		deptIds = null;

		for (ContactDocument cd : cdList) {
			delete(cd);
		}

		for (Packet packet : packetList) {
			packet.setDocumentList(null);
			delete(packet);
		}

		for (ApprovalPath path : approvalPathList) {
			path.getDocumentChains().clear();
			delete(path);
		}

		for (Notification not : notificationList) {
			not.getMessages().clear();
			delete(not);
		}

		delete(project);
		// Note that Units, Scripts, Stripboards,
		// ... DPRs, ReportRequirements,
		// ... and ProjectSchedule are
		// ... automatically deleted by Hibernate via cascade.

		// We currently KEEP Changes and Events from the deleted project!

		log.debug("");
	}

	/**
	 * Delete a Unit from a Project, and take care of all the associated
	 * objects. Most are deleted, except for Strip`s associated with this Unit's
	 * Stripboard, which will be changed to unscheduled.
	 * <p>
	 * For cross-project productions, e.g., TV Series, ALL units that match the
	 * given unit number are removed (one from each existing Project).
	 *
	 * @param project The Project whose Unit is to be deleted.
	 * @param unit The Unit to be deleted.
	 */
	@Transactional
	public void removeUnit(Project project, Unit unit) {
		project = refresh(project); // make sure it's current
		unit = UnitDAO.getInstance().refresh(unit);
		int unitNumber = unit.getNumber();
		Production prod = project.getProduction();
		if (prod.getType().getCrossProject()) {
			Set<Project> projects = prod.getProjects();
			for(Project proj : projects) {
				unit = proj.getUnit(unitNumber);
				if (unit != null) {
					removeUnitImpl(proj, unit);
				}
			}
		}
		else {
			removeUnitImpl(project, unit);
		}
		project = refresh(project); // make sure it's current
	}

	/**
	 * Delete a Unit from a Project, and take care of all the associated objects.
	 * Most are deleted, except for Strip`s associated with this Unit's Stripboard,
	 * which will be changed to unscheduled.
	 * @param project
	 * @param unit
	 */
	private void removeUnitImpl(Project project, Unit unit) {
		log.debug("proj=" + project.getTitle() + ", unit=" + unit.getNumber());

		// Remove ReportRequirement's associated with the Unit being deleted.
//		ReportRequirementDAO rrDAO = ReportRequirementDAO.getInstance();
		for (Iterator<ReportRequirement> iter = project.getReportRequirements().iterator(); iter.hasNext(); ) {
			ReportRequirement rr = iter.next();
			if (rr.getUnitNumber().equals(unit.getNumber())) {
				log.debug("removing RptReq=" + rr.toString());
				iter.remove();
				delete(rr);
			}
		}

		// Remove UnitStripboard`s & change this Unit's Strip`s to unscheduled.
		StripDAO.getInstance().removeUnit(project, unit);

		// Delete any Callsheet`s for this Unit.
		List<Callsheet> sheets = CallsheetDAO.getInstance().findByProjectAndUnit(project, unit);
		for (Callsheet cs : sheets) {
			for (Project proj : cs.getProjects()) {
				proj.getCallsheets().remove(cs);
			}
			delete(cs);
		}

		delete(unit);
		// Note that projectMember objects, and ProjectSchedule are
		// ... automatically deleted by Hibernate via cascade.

		boolean b = project.getUnits().remove(unit);
		log.debug(b);
	}

	/**
	 * Update users' default projects -- if a user's default project is the one specified, change it
	 * to another project of which they are a member. We select the production default project, if
	 * they are a member, otherwise the project with the largest database id, as this should be the
	 * most recently created one. This method is typically called prior to deleting a project.
	 *
	 * @param dropProject
	 */
	private void changeContactProject(Project dropProject) {
		log.debug("proj=" + dropProject.toString());
		Set<Contact> contacts = dropProject.getContacts();
		if (contacts != null && contacts.size() > 0) {
			Integer defaultId = SessionUtils.getProduction().getDefaultProject().getId();
			for (Contact contact : contacts) {
				contact.setProject(null);
				Integer newId = -1;
				Project newProject = null;

				// TODO UNTESTED
				for (Iterator<Employment> iter = contact.getEmployments().iterator(); iter.hasNext(); ) {
					Employment emp = iter.next();
					if (emp.getProject() != null) {
						Project empProject = emp.getProject();
						if (empProject.getId().equals(dropProject.getId())) {
							// we have to remove the relevant PM from the user's ProjectMember collection,
							// otherwise the project delete will fail later with "object would be re-added" error.
							// TODO DH: Right or wrong?
							emp.getProjectMembers().clear();
							iter.remove();
						}
						else if (empProject.getId().equals(defaultId)) {
							newProject = empProject;
							newId = Integer.MAX_VALUE;
						}
						else if (empProject.getId() > newId && empProject.getStatus() != AccessStatus.OFFLINE) {
							newProject = empProject;
							newId = newProject.getId();
						}
					}
					else {
						newProject = SessionUtils.getProduction().getDefaultProject();
					}
				}
				/* Original Code with deprecated method
				 * for (Iterator<ProjectMember> iter = contact.getProjectMembers().iterator(); iter.hasNext(); ) {
					ProjectMember mbr = iter.next();
					if (mbr.getUnit() != null) {
						Project mbrProject = mbr.getUnit().getProject();
						if (mbrProject.getId().equals(dropProject.getId())) {
							// we have to remove the relevant PM from the user's ProjectMember collection,
							// otherwise the project delete will fail later with "object would be re-added" error.
							iter.remove();
						}
						else if (mbrProject.getId().equals(defaultId)) {
							newProject = mbrProject;
							newId = Integer.MAX_VALUE;
						}
						else if (mbrProject.getId() > newId && mbrProject.getStatus() != AccessStatus.OFFLINE) {
							newProject = mbrProject;
							newId = newProject.getId();
						}
					}
					else {
						newProject = SessionUtils.getProduction().getDefaultProject();
					}
				}*/
				contact.setProject(newProject);
				log.debug("User project changing: user=" + contact.getId() + ", proj=" + (newProject==null ? "null" : newProject.getId()));
				attachDirty(contact);
//				ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, newProject, contact.getUser(),
//						"changed project (due to delete); old project=" + dropProject.getTitle());
			}
			dropProject.setContacts(new HashSet<Contact>());
			attachDirty(dropProject);
		}
	}

	/**
	 * Change the "active" script in a project. When this happens we also (a)
	 * mark the DooD data as out-of-date, and (b) update Strip`s in all
	 * Stripboard`s in this project to reflect the "omitted" status of scenes in
	 * the new Script, and ensure any missing Strip`s (for new Scene`s) are created.
	 *
	 * @param project The project whose script is being set.
	 * @param script The new script value (may be null).
	 */
	@Transactional
	public void setScript(Project project, Script script) {
		log.debug("");
		project.setScript(script);
		ProductionDood.markProjectDirty(project);
		StripDAO stripDAO = StripDAO.getInstance();
		for (Stripboard stripboard : project.getStripboards()) {
			stripDAO.updateStripboard(stripboard, script);
		}
		ChangeUtils.logChange(ChangeType.SCRIPT, ActionType.UPDATE,
				project, "set default Script = " + (script == null ? "null" : script.getDescription()) );
	}

	/**
	 * Set the default Stripboard for a given project.
	 * @param project
	 * @param board
	 */
	@Transactional
	public void setStripboard(Project project, Stripboard board) {
		project = refresh(project);
		project.setStripboard(board);
		attachDirty(project);
		ProductionDood.markProjectDirty(project, true);
		ChangeUtils.logChange(ChangeType.STRIPBOARD, ActionType.UPDATE,
				project, board, "set default Stripboard");
	}

//	public static ProjectDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ProjectDAO) ctx.getBean("ProjectDAO");
//	}

	/**
	 * Change the Production's "Default project".  The value in the production object
	 * is changed, and the "current project" for numerous users is also changed.  The
	 * users affected are those who are members of the new default project, and whose
	 * existing current project is set to the old default project.
	 *
	 * @param project The new default project.
	 */
	@Transactional
	public void changeDefaultProject(Project project, Contact currentContact) {
		try {
			Production production = SessionUtils.getProduction();
			Project oldDefault = production.getDefaultProject();
			String oldProjectName = oldDefault.getTitle();
			Integer oldProjectId = oldDefault.getId();
			log.debug("** DEFAULT PROJECT CHANGE **, old=" + oldDefault.toString() +
					", new=" + project.toString() +
					", contact=" + (currentContact==null ? "null" : currentContact.getId()));
			oldDefault = null; // release it
			production.setDefaultProject(project);
			ProductionDAO.getInstance().attachDirty(production);
			production = null;

			boolean changeCurrent = false;
			UserDAO userDAO = UserDAO.getInstance();
			List<Contact> contacts = ProjectMemberDAO.getInstance().findByProjectDistinctContact(project);
			for (Contact contact : contacts) {
				if (contact.getProject() == null || contact.getProject().getId().equals(oldProjectId)
						|| contact.equals(currentContact)) {
					contact.setProject(project);
					userDAO.attachDirty(contact);
					// ChangeUtils.logChange(ChangeType.USER, ActionType.UPDATE, project, contact.getUser(),
					// 		"changed current/default project; old project=" + oldProjectName);
					if (contact.equals(currentContact)) {
						changeCurrent = true;
					}
				}
			}

			ChangeUtils.logChange(ChangeType.PRODUCTION, ActionType.UPDATE, project,
					"Set Production DEFAULT project; old=" + oldProjectName);
			if (changeCurrent) {
				SessionUtils.setCurrentProject(project);
				AuthorizationBean.getInstance().auth(currentContact); // update authorization map
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/**
	 * Takes a Project that has been deserialized from an import/export file and
	 * completely replaces the specified project with the new data. This is
	 * meant to be used when down-loading into the "offline" version of
	 * LightSPEED.
	 *
	 * @param bundle The ProjectBundle deserialized from the import file.
	 * @param oldProject The Project whose data should be replaced by the data
	 *            in the bundle.
	 * @return The Project from the bundle, or null if the load fails. Upon
	 *         return, this project has been persisted in the database.
	 */
	@Transactional
	public Project load(ProjectBundle bundle, Project oldProject) {
		Project project = bundle.getProject();
		try {
			log.debug("bundle: " + bundle);
			log.debug("old id=" + oldProject.getId());

			String priorTitle = oldProject.getTitle();
			clean(oldProject);
			evict(oldProject);
			project.setId(oldProject.getId());
			if (! project.getTitle().equals(priorTitle)) { // renaming
				// make sure new name doesn't duplicate another existing project.
				// This won't happen in the current "real" offline version, which
				// should only have one project, but maybe it will have more in
				// the future; also, our development version supports multiple
				// projects.
				if (findByProductionAndTitle(project.getTitle()).size() > 0) { // duplicate
					project.setTitle(project.getTitle()+"(2)");
				}
			}

			// Let the incoming Production object over-write the existing one.
			Production prod = SessionUtils.getProduction();
			project.getProduction().setId(prod.getId());
			if (project.getProduction().getAddress() != null) {
				if (prod.getAddress() != null) {
					// overwrite the existing Address object
					project.getProduction().getAddress().setId(prod.getAddress().getId());
					evict(prod.getAddress()); // remove old one from session
				}
				attachDirty(project.getProduction().getAddress());
			}
			evict(prod); // remove old one from session

			Set<Stripboard> saveboards = project.getStripboards();
			project.setStripboards(null);

			/* This next attach will:
			 * - update the Production object
			 * - update the Project
			 * - create/update the associated (new) Unit`s & ProjectSchedule`s
			 * - create/update Script(s), including all Scene`s, Page`s, & TextElement`s
			 */
			attachDirty(project.getProduction());

			// Save all the incoming ScriptElement`s
			for (ScriptElement se : bundle.getScriptElements()) {
				save(se);
			}

			// Patch up imported Stripboard, UnitStripboard, and Strip data
			project.setStripboards(saveboards);
			int highUnit = project.getHighestUnitNumber();
			for (Stripboard board : saveboards) {
				int ix = 0;
				for (UnitStripboard usb : board.getUnitSbs()) {
					usb.setUnit(project.getUnits().get(ix++));
				}
				for (Strip strip : board.getStrips()) {
					Integer unitNum = strip.getUnitId();
					//log.debug("unit#=" + (unitNum==null?"null":unitNum.intValue()));
					if (unitNum != null && unitNum > 0 && unitNum <= highUnit ) {
						Unit u = project.getUnit(unitNum);
						if (u != null) {
							strip.setUnitId(u.getId());
						}
						else {
							log.error("unmatched unit number=" + unitNum);
						}
					}
				}
			}
			attachDirty(project); // This will save (new) Stripboard`s and Strip`s.
			HeaderViewBean.reset(); // refresh project list, etc.
		}
		catch (Exception e) {
			log.error("exception: ",e);
			project = null;
		}

		return project;
	}

	@Transactional
	private void clean(Project project) {
		log.debug("");
		project.setScript(null);
		for (Script script : project.getScripts()) {
			delete(script);
		}
		project.setScripts(null);

		for (Stripboard board : project.getStripboards()) {
			delete(board);
		}
		project.setStripboards(null);

		for (Unit unit : project.getUnits()) {
			delete(unit);
		}
		project.setUnits(null);

		ScriptElementDAO.getInstance().deleteAll(project); // so remove all script elements
		attachDirty(project);
		flush();
	}

	/**
	 * Loads a Project from an export file into the online system. This is NOT a
	 * complete replacement of the project, but is piece-meal, and controlled by
	 * the various parameters passed.
	 *
	 * @param bundle The source for all the data to be used in updating the
	 *            Project.
	 * @param target The Project to be updated.
	 * @param loadScripts If true, scripts in the source bundle will be loaded
	 *            into the target project: ones that match by import date or
	 *            revision number will replace the corresponding ones, and any
	 *            others will be added. Scripts in the current project that
	 *            don't match ones in the source will be deleted.
	 * @param loadBreakdown If true, breakdown information - basically
	 *            scene-to-scriptElement relations - will be updated from the
	 *            source bundle.
	 * @param loadCalendar If true, load the objects related to the Calendar --
	 *            the ProjectSchedule and its DateEvents.
	 * @param loadElements If true, update the target Project's set of
	 *            ScriptElements to exactly match the set of ScriptElement`s in
	 *            the source bundle.
	 * @param loadStripboard If true, update the target Project's Stripboard`s
	 *            (and Strips) to exactly match those from the source bundle.
	 * @return A (possibly updated) reference to the target Project, or null if
	 *         the load failed.
	 */
	@Transactional
	public Project load(ProjectBundle bundle, Project target, boolean loadScripts, boolean loadBreakdown,
			boolean loadCalendar, boolean loadElements, boolean loadStripboard) {
		Project source = bundle.getProject();

		loadBreakdown = loadScripts; // TODO can we allow loadBreakdown to be different?

		try {
			log.debug("bundle: " + bundle);
			if (loadCalendar) {
				target.setOriginalEndDate(source.getOriginalEndDate());
			}
			if (loadScripts) {
				target.setScriptTextAccessible(source.getScriptTextAccessible());
			}

			loadUnits(source, target, loadCalendar);

			/** A List of all existing (target) ScriptElement`s */
			List<ScriptElement> seFinalSet = ScriptElementDAO.getInstance().findByProject(target);

			if (loadElements) {
				loadScriptElements(bundle.getScriptElements(), target, seFinalSet,
						(!loadScripts || !loadBreakdown) );
			}

			/* 'seFinalSet' is now a list of all "current" SE's - either target ones that also
			 * existed in source, or newly added ones.  Make sure incoming Scenes reference
			 * these versions of SE's, and not the ones in the source project.
			 * If we don't switch the references, we'd get "unsaved transients" errors. */

			target = processScripts(source, target, loadScripts, loadBreakdown, seFinalSet);

			if (loadStripboard) {
				target = loadStripboards(source, target);
			}

			attachDirty(target);
		}
		catch (Exception e) {
			log.error("exception: ",e);
			target = null;
		}

		return target;
	}

	/**
	 * Add or update the Unit objects from the source. If 'loadCalendar' is
	 * true, then the ProjectSchedule and associated DateEvents are also
	 * updated.
	 *
	 * @param source
	 * @param target
	 * @param loadCalendar
	 */
	private void loadUnits(Project source, Project target, boolean loadCalendar) {
		log.debug("loading units");

		final Set<Unit> copyUnits = new HashSet<>(target.getUnits());

		for (Unit unit : source.getUnits()) {
			// either add the source Unit, or update its counterpart
			Unit tUnit = target.getUnit(unit.getNumber());
			if (tUnit == null) { // new one
				unit.setProject(target);
				save(unit);
				target.getUnits().add(unit);
			}
			else { // matched - update existing Unit
				tUnit.merge(unit);
				if (loadCalendar) {
					for (DateEvent de : tUnit.getProjectSchedule().getDateEvents()) {
						delete(de);
					}
					tUnit.getProjectSchedule().merge(unit.getProjectSchedule());
				}
				attachDirty(tUnit);
				copyUnits.remove(tUnit); // remove matches
			}
		}

		// Any units left in 'copyUnits' were not in source, so delete from target
		for (Unit unit : copyUnits) {
			removeUnitImpl(target, unit);
		}
		attachDirty(target);

		if (loadCalendar) {
			log.debug("calendar loaded");
		}
		log.debug("units loaded");
	}

	/**
	 * Load the ScriptElement`s from the incoming source file, updating the
	 * matching entries in the target Project, adding ones that don't already
	 * exist there, and deleting ones that are in the target but not in the
	 * source.
	 * <p>
	 * Note that the contents of 'seFinalSet' may have been updated upon
	 *         return.
	 *
	 * @param sourceSet The collection of ScriptElements from the import file.
	 * @param target The project to be updated.
	 * @param seFinalSet An input/output parameter. On input, it is the
	 *            collection of ScriptElements that currently exist in the
	 *            target; on output, it is the (modified) collection of
	 *            ScriptElements that exist in the target at the end of this
	 *            update process.
	 * @param checkReferences False if the user is loading updated scripts and
	 *            breakdown information during this process. If this is true, we
	 *            won't delete ScriptElements that are not in the source, but
	 *            are still used in the existing Scenes.
	 */
	private void loadScriptElements(Collection<ScriptElement> sourceSet, Project target,
			List<ScriptElement> seFinalSet, boolean checkReferences) {
		log.debug("loading elements");

		// Make a copy of the current target set of elements
		final List<ScriptElement> seList = new ArrayList<>(seFinalSet);

		// Add new ones based on source vs existing set comparison.
		// We need to do this first (before updates)
		// so that all linked elements can be resolved to persisted objects.
		for (ScriptElement se : sourceSet) {
			se.setProject(target); // necessary for 'equals' to match!
			int ix = seFinalSet.indexOf(se); // (and indexOf will use equals)
			if (ix < 0) {	// new element - add to database & to 'seFinalSet'
				saveElementAndChildren(se, target, seFinalSet);
			}
		}

		// update existing ones based on incoming set
		for (ScriptElement se : sourceSet) {
			se.setProject(target);
			int ix = seList.indexOf(se);
			if (ix >= 0) {
				ScriptElement targetSe = seList.remove(ix);
				targetSe.merge(se); // copy primitive fields
				if (se.getChildElements() != null && se.getChildElements().size() > 0) {
					// need to handle linked element changes...
					if (targetSe.getChildElements() == null) {
						targetSe.setChildElements(new HashSet<ScriptElement>());
					}
					Set<ScriptElement> sourceChildren = se.getChildElements();
					Set<ScriptElement> targetChildren = targetSe.getChildElements();
					// find any linked elements in target that aren't in source & remove them:
					for (Iterator<ScriptElement> it = targetChildren.iterator(); it.hasNext(); ) {
						ScriptElement ch = it.next();
						if (! sourceChildren.contains(ch)) {
							it.remove();
						}
					}
					// If any linked elements in source & not in target, add them.
					for (ScriptElement ch : sourceChildren) {
						ch.setProject(target);
						if (! targetChildren.contains(ch)) {
							int chIx = seFinalSet.indexOf(ch);
							if (chIx >= 0) { // should be! Both old and new are in seFinalSet.
								ScriptElement targetCh = seFinalSet.get(chIx);
								targetChildren.add(targetCh);
							}
						}
					}
				}
				else { // source has no linked elements - make sure target is clear.
					targetSe.setChildElements(new HashSet<ScriptElement>());
				}
				attachDirty(targetSe);
			}
		}

		// Any elements left in seList are not in the import and should be deleted
		ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
		for (ScriptElement se : seList) {
			boolean doDelete = true;
			if (checkReferences) {
				// if the user is NOT importing script revisions, then we must
				// preserve any LOCATION ScriptElements referenced by existing Scenes.
				if (se.getType() == ScriptElementType.LOCATION) {
					if (scriptElementDAO.isLocationElementReferenced(se.getId())) {
						doDelete = false;
					}
				}
				else if (scriptElementDAO.isElementReferenced(se.getId())) {
					doDelete = false;
				}
			}
			if (doDelete) {
				delete(se);
				seFinalSet.remove(se);
			}
			// Any RealLink`s get deleted by cascade
		}
		log.debug("elements loaded");
	}

	/**
	 * Given a ScriptElement that is new -- not persisted yet and does not match
	 * an existing ScriptElement -- save it and add it to the 'finalSet', after
	 * first treating any of its childElement Set objects in a similar manner.
	 * So we recurse if necessary, on "new" children, and fix up the
	 * childElement Set so that it refers to the persisted (either new or
	 * existing) copies of ScriptElements.
	 *
	 * @param se The ScriptElement to be saved, after updating its children (if
	 *            necessary).
	 * @param target The project being updated.
	 * @param seFinalSet An input/output List. On entry, it has all the existing
	 *            ScriptElements; on exit, it will have at least one new
	 *            elements added -- the 'se' parameter object -- plus any
	 *            descendants of 'se' that were also added.
	 */
	private void saveElementAndChildren(ScriptElement se, Project target, List<ScriptElement> seFinalSet) {
		log.debug("SE id=" + se.getId());
		if (se.getChildElements() != null && se.getChildElements().size() > 0) {
			Set<ScriptElement> updatedChildren = new HashSet<>();
			for (ScriptElement ch : se.getChildElements()) {
				ch.setProject(target);
				int ix = seFinalSet.indexOf(ch);
				if (ix >= 0) { // child exists (either old, or already added by our caller)
					updatedChildren.add(seFinalSet.get(ix));
				}
				else { // child is new in source; save it
					saveElementAndChildren(ch, target, seFinalSet);
					updatedChildren.add(ch); // use it in child set
				}
			}
			se.setChildElements(updatedChildren);
		}
		save(se);
		seFinalSet.add(se);
	}

	/**
	 * Part of the Project-load process -- the incoming Project's scripts may
	 * replace or be added to the existing Project.
	 *
	 * @param source The incoming Project, from the file being loaded.
	 * @param target The existing Project, to be updated.
	 * @param loadScripts If true, Script`s in the source Project will be added
	 *            to the existing Project, or replace equivalent Script`s.
	 * @param loadBreakdown If true, the break-down information, i.e., the
	 *            association between ScriptElement`s and Scene`s, will be
	 *            loaded from the source into the target.
	 * @param seFinalSet The List of ScriptElement`s that will exist in the
	 *            updated target Project. Any Scene - to - ScriptElement
	 *            references should use these objects.
	 * @return The updated target Project. In some cases, this code needs to
	 *         flush all changes to the Project and load a new copy, so the
	 *         returned instance may be different from the passed instance.
	 */
	private Project processScripts(Project source, Project target, boolean loadScripts,
			boolean loadBreakdown, List<ScriptElement> seFinalSet) {
		log.debug("process scripts");

		final ScriptDAO scriptDAO = ScriptDAO.getInstance();

		if (loadScripts || loadBreakdown) {

			final List<Script> copyScripts = new ArrayList<>(target.getScripts());
			for (Script script : source.getScripts()) {
				// either add the source Script, or update its counterpart
				Script tScript;
				int ix = copyScripts.indexOf(script);
				if (ix < 0) {
					// no match by script import date; check revision #
					tScript = scriptDAO.findByRevisionAndProject(script.getRevisionNumber(), target);
					if (tScript != null && loadScripts) {
						// revision was reloaded - replace entire Script
						copyScripts.remove(tScript); // remove from collection so we don't try & delete it later
						delete(tScript);	// and delete it
						target.getScripts().remove(tScript); // drop from association, too
						target.setScript(null); // just in case
						attachDirty(target);
						flush(); // w/o flush, save of replacement failed with duplicate key error
						target = refresh(target);
						tScript = null; // indicates source Script should be added as new
					}
				}
				else {
					tScript = copyScripts.get(ix);
				}
				if (tScript == null) { // new Script
					if (loadScripts) {
						script.setProject(target);
						target.getScripts().add(script);
						for (Scene scene : script.getScenes()) {
							updateSceneSeRefs(scene, seFinalSet);
						}
						save(script); // Scenes saved via cascade
					}
				}
				else { // matched - update existing Script
					if (loadScripts) {
						tScript.merge(script);
					}
					if (loadBreakdown) {
						loadBreakdown(script, tScript, seFinalSet);
					}
					attachDirty(tScript);
					copyScripts.remove(tScript); // remove matches
				}
			}

			// Any scripts left in 'copyScripts' were not in source, so delete from target
			for (Script script : copyScripts) {
				scriptDAO.remove(target, script);
			}

			// make sure target project refers to same script as source
			if (source.getScript() != null) {
				int rev = source.getScript().getRevisionNumber();
				if (target.getScript() == null || target.getScript().getRevisionNumber() != rev) {
					Script s = scriptDAO.findByRevisionAndProject(rev, target);
					if (s != null) {
						setScript(target, s);
					}
				}
			}
			log.debug("breakdown loaded");
		}

		log.debug("scripts done");
		return target;
	}

	/**
	 * Update the target Script with all the breakdown information from the
	 * source Script, that is, adding any new Scene`s, removing any extra
	 * Scene`s (in target but not in source) and updating all the
	 * scene-to-scriptElement references.
	 *
	 * @param srcScript
	 * @param targetScript
	 * @param seFinalSet
	 */
	private void loadBreakdown(Script srcScript, Script targetScript, List<ScriptElement> seFinalSet) {
		Set<String> sourceSceneNums = new HashSet<>();
		for (Scene scn : srcScript.getScenes()) {
			sourceSceneNums.add(scn.getNumber());
			Scene tSc = SceneDAO.getInstance().findByScriptAndNumber(targetScript, scn.getNumber());
			if (tSc != null) {
				tSc.merge(scn);
				tSc.setScriptElements(scn.getScriptElements());
				tSc.setScriptElement(scn.getScriptElement()); // location
				updateSceneSeRefs(tSc, seFinalSet);
				attachDirty(tSc);
			}
			else {
				updateSceneSeRefs(scn, seFinalSet);
				save(scn);
			}
		}
		// delete extra scenes in target not in source
		for ( Iterator<Scene> it = targetScript.getScenes().iterator(); it.hasNext(); ) {
			Scene ts = it.next();
			if (! sourceSceneNums.contains(ts.getNumber())) {
				it.remove();
				delete(ts);
			}
		}

	}

	/**
	 * Load the source's Stripboards, UnitStripboards, and associated
	 * Strips into the target Project.
	 * @param source
	 * @param target
	 * @return A reference to the (possibly updated) target persisted instance.
	 */
	private Project loadStripboards(Project source, Project target) {
		log.debug("loading stripboards");
		// delete old stripboards & strips
		for (Stripboard st : target.getStripboards()) {
			delete(st); // Strips and unitStripboards are deleted via cascade
		}
		target.setStripboards(null);
		target.setStripboard(null);
		flush(); // w/o this, save of imported stripboards fails on duplicate key
		target = refresh(target);

		// save imported Stripboards
		target.setStripboards(source.getStripboards());
		int currBoardRev = source.getStripboard().getRevision();
		for (Stripboard st : source.getStripboards()) {
			// Patch up imported Stripboard, UnitStripboard, and Strip data
			st.setProject(target);
			if (st.getRevision() == currBoardRev) {
				target.setStripboard(st);
			}
			int ix = 0;
			for (UnitStripboard usb : st.getUnitSbs()) {
				usb.setUnit(target.getUnits().get(ix++));
			}
			// exported Strips have unit.number instead of unit.id -- fix them
			for (Strip strip : st.getStrips()) {
				Integer unitNum = strip.getUnitId();
				//log.debug("unit#=" + (unitNum==null?"null":unitNum.intValue()));
				if (unitNum != null) { // it's null for unscheduled Strips
					Unit u = target.getUnit(unitNum);
					if (u != null) {
						strip.setUnitId(u.getId());
					}
					else {
						log.error("unmatched unit number=" + unitNum);
					}
				}
			}
			save(st); // Strips and unitStripboards saved via cascade
		}
		log.debug("stripboards loaded");
		return target;
	}

	private void updateSceneSeRefs(Scene scene, List<ScriptElement> seFinalSet) {
		Set<ScriptElement> newSes = new HashSet<>();
		for (ScriptElement se : scene.getScriptElements()) {
			int i = seFinalSet.indexOf(se);
			if (i >= 0) {
				//log.debug("match="+i);
				newSes.add(seFinalSet.get(i));
			}
			else {
				log.warn("no match, se=" + se);
			}
		}
		scene.setScriptElements(newSes);

		if (scene.getScriptElement() != null) {
			int i = seFinalSet.indexOf(scene.getScriptElement());
			if (i >= 0) {
				//log.debug("loc match="+i);
				scene.setScriptElement(seFinalSet.get(i));
			}
			else {
				log.warn("no loc match, se=" + scene.getScriptElement());
			}
		}
	}

	/**
	 * Lock the specified Project's Stripboard`s (for editing) so no other User
	 * can edit them, and return True if successful.
	 *
	 * @param proj The Project to be locked.
	 * @param user The User who will "hold" the lock and is allowed to edit any
	 *            Stripboard`s in the Project.
	 * @return True if the User has successfully been given the lock, which
	 *         means that no other User currently had the lock and we
	 *         successfully updated the database with the lock information, or
	 *         the same User already had the lock.
	 */
	@Transactional
	public boolean lock(Project proj, User user) {
		boolean ret = false;
		try {
			if (proj.getStripsLockedBy() == null) {
				proj.setStripsLockedBy(user.getId());
				attachDirty(proj);
				ret = true;
			}
			else if (proj.getStripsLockedBy().equals(user.getId())) {
				ret = true;
			}
			log.debug("locked #" + proj.getId() + "=" + ret + ", by="+ user.getId());
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		return ret;
	}

	/**
	 * Unlock the Project's Stripboard`s so any User (with appropriate permission) can now
	 * edit them.  No error occurs if the Project was already unlocked.
	 * @param proj The Project to be unlocked.
	 */
	@Transactional
	public void unlock(Project proj, Integer userId) {
		log.debug("unlocking #" + proj.getId() + "; locked by=" + proj.getStripsLockedBy());
		if (proj.getStripsLockedBy() != null) {
			if (userId == null || userId.equals(proj.getStripsLockedBy())) {
				proj.setStripsLockedBy(null);
				attachDirty(proj);
			}
		}
	}

	/**
	 * Unlock all projects that are currently locked. Called during application
	 * startup, to clear any locks leftover from a prior execution, in case of a
	 * "hard" crash in which our normal methods did not clear the locks during
	 * shutdown (via StripboardEditBean.dispose()).
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void unlockAllLocked() {
		try {
			String query = "from Project where " + STRIPS_LOCKED_BY + " is not null";
			List<Project> locked = find(query);
			for (Project proj : locked) {
				unlock(proj, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * this method is only called in case a Project ends up corrupted (possibly
	 * due to delete failing), and has no Units left. This will create a viable
	 * Unit so that viewing (and maybe deletion) of the project won't fail.
	 * Code moved from Project class by LS-2737.
	 *
	 * @return the newly instantiated and persisted Unit.
	 */
	public static Unit fixMissingUnit(Project proj) {
		/* this code only executes in case a Project ends up corrupted (possibly due to delete failing),
		 * and has no Units left.  This will create a viable Unit so that viewing (and maybe
		 * deletion) of the project won't fail. */
		Unit u = new Unit(proj, 1, "Temp");
		ProjectSchedule schedule = new ProjectSchedule();
		schedule.setStartDate(new Date());
		u.setProjectSchedule(schedule); // schedule is saved via cascade
		UnitDAO.getInstance().save(u);
		proj.getUnits().add(u);
		EventUtils.logError("Project had no units - dummy Unit created!");
		return u;
	}

}
