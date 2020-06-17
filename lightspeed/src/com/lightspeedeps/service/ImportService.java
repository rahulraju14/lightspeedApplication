/**
 * File: ImportService.java
 */
package com.lightspeedeps.service;

import java.io.EOFException;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.CreateUserContact;
import com.lightspeedeps.port.Importer;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.converter.FedIdConverter;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

/**
 * Provides methods used to import a tab-delimited file containing project and
 * contact information. There are two separate processes it supports:
 * <ul>
 * <li>Take a tab-delimited file and load it into our 'temporary' contact_import
 * table.
 * <li>Take the data in the contact_import table and, from each record, add,
 * replace, or delete the information for one crew person.
 */
public class ImportService extends BaseService {
	private static final Log LOG = LogFactory.getLog(ImportService.class);

	/** The filename selected by the user. */
	private String filename;

	/** The current production. */
	private Production production;

	/** Name of the packet to distribute. Used by the Web Service */
	private String packetName;

	/** The User matching the ContactImport record currently being processed. */
	private User user;

	/** Set to true during add of occupation if StartForm is needed. */
	private boolean needStart;

	private String creatingAccount;

	private Project project = null;

	private Project commProject = null;

	private String lastEpisode = ""; // episode code for last successful Project processed

	private final transient ContactDAO contactDAO = ContactDAO.getInstance();
	private final transient ContactImportDAO contactImportDAO = ContactImportDAO.getInstance();
	private final transient DepartmentDAO departmentDAO = DepartmentDAO.getInstance();
	private final transient ProjectDAO projectDAO = ProjectDAO.getInstance();
	private final transient RoleDAO roleDAO = RoleDAO.getInstance();
	private final transient UserDAO userDAO = UserDAO.getInstance();

	/** Default constructor. */
	public ImportService() {
	}

	/** Constructor to use when uploading a file into our temporary table. */
	public ImportService(String fname) {
		filename = fname;
	}

	/** Constructor used by Web Service to specify production -- as there is no
	 * "current production". */
	public ImportService(Production prod, String creatingAcct, String packetName) {
		production = prod;
		creatingAccount = creatingAcct;
		this.packetName = packetName;
	}

	/**
	 * Load the tab-delimited file (specified in the constructor) into the
	 * ContactImport table, and validate the data.
	 *
	 * @param msgs A List of messages. This method will add error and
	 *            informational messages to this list.
	 * @return True if no errors were encountered. False if any errors,
	 *         including invalid data, were found; messages will have been added
	 *         to 'msgs' for any issues.
	 */
	public boolean load(List<String> msgs) {
		LOG.debug("file=" + filename);
		production = SessionUtils.getProduction();
		String acct = SessionUtils.getCurrentUser().getAccountNumber();

		boolean ret = true;
		try {
			Importer imp = new Importer(filename);
			boolean ok = true;
			String action = imp.getString();
			ok = imp.next(); // discard first record - file descriptor
			int count = 1;
			try {
				while (ok) {
					action = imp.getString();
					if (action != null) {
						ContactImport cti = new ContactImport(production, acct);
						ActionType act = null;
						if (action.compareToIgnoreCase("REP") == 0) {
							act = ActionType.UPDATE;
						}
						else if (action.compareToIgnoreCase("ADD") == 0) {
							act = ActionType.CREATE;
						}
						else if (action.compareToIgnoreCase("DEL") == 0) {
							act = ActionType.DELETE;
						}
						else { // invalid action
							msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidAction", count, action));
							ok = false;
							break;
						}
						try {
							cti.setAction(act);
							cti.imports(imp);
						}
						catch (ParseException e) {
							// ignore, validation will catch later
						}
						catch (Exception e) {
							ok = false;
							ret = false;
							EventUtils.logError(e);
						}
						contactImportDAO.save(cti);
						ret &= validate(cti, count, msgs);
						ok = imp.next(); // discard any unused data in the record.
						count++;
					}
				}
			}
			catch (EOFException eof) {
				LOG.debug("EOF on import file");
			}
			catch (NumberFormatException nbr) {
				EventUtils.logError(nbr);
			}
			catch (IOException io) {
				EventUtils.logError(io);
			}
			imp.close();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			ret = false;
		}
		LOG.debug("load ended, ret=" + ret);
		return ret;
	}

	/**
	 * Validate a ContactImport instance. Checks for null or empty required
	 * fields and validates the syntax of the email address. Note that the
	 * import process has already done a "trim()" on all fields before they
	 * are stored in the ContactImport.
	 *
	 * @param cti The ContactImport to validate.
	 * @param count The 'record number' of this instance; may be used in
	 *            messages.
	 * @param msgs The List of messages to which validation error messages will
	 *            be added.
	 * @return True if no errors were encountered.
	 */
	private boolean validate(ContactImport cti, int count, List<String> msgs) {
		boolean valid = true;
		Date today = CalendarUtils.todaysDate();
		String episode = cti.getEpisodeCode();
		String email = cti.getEmailAddress();
		String name = cti.getFirstName();
		if (name == null) {
			name = "";
		}
		name += " ";
		if (cti.getLastName() != null) {
			name += cti.getLastName();
		}
		if (episode == null || episode.length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingEpisodeId", "", name));
			valid = false;
			episode = "*missing*";
		}
		if (email == null || email.length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingEmailAddress", episode, name));
			valid = false;
			email = "*missing*";
		}
		else if (! EmailValidator.isValidEmail(email)) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidEmailAddress", episode, name, email));
			valid = false;
		}
		if (name.length() < 2) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidName", episode, name, email));
			valid = false;
		}
		if (! StringUtils.isEmpty(cti.getPhone())) {
			String phone = PhoneNumberValidator.cleanNumber(cti.getPhone());
			if (phone.length() == 0) {
				msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidPhoneNumber", episode, name, cti.getPhone()));
				valid = false;
			}
		}
		if (cti.getSocialSecurity() != null && cti.getSocialSecurity().length() == 0) {
			// Import sets any invalid value to empty string. Null indicates no SSN was supplied.
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidSSN", episode, name));
			valid = false;
		}

		if (cti.getProjectStartDate() == null) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidStartDate", episode, name, email));
			valid = false;
		}
		else if (cti.getProjectedEndDate() != null && cti.getProjectedEndDate().before(cti.getProjectStartDate())) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidEndDate", episode, name, email));
			valid = false;
		}
		if (cti.getProjectedEndDate() != null && cti.getProjectedEndDate().before(today)) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidEndDate", episode, name, email));
			valid = false;
		}

		if (cti.getDepartment() == null || cti.getDepartment().length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingDepartment", episode, name));
			valid = false;
		}
		if (cti.getOccupation() == null || cti.getOccupation().length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingOccupation", episode, name));
			valid = false;
		}
		if (cti.getRateType() == null) {
			// Import sets any invalid rate-type to null; if no rate type was given, it
			// was set to the default (HOURLY).
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidRateType", episode, name));
			valid = false;
		}
		if (cti.getRate() != null && cti.getRate().signum() <= 0) {
			// Import sets any invalid rate to zero. Null indicates no rate was supplied.
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidRate", episode, name));
			valid = false;
		}

		// LS-3043 validation for new added fields
		if (! StringUtils.isEmpty(cti.getPayIndicator())) {
			if (! cti.getPayIndicator().equals("I") && ! cti.getPayIndicator().equals("C")) {
				msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidPayIndicator", episode,
						name));
				valid = false;
			}
			else if (cti.getPayIndicator().equals("C")) {
				if (StringUtils.isEmpty(cti.getFein())) {
					msgs.add(MsgUtils.formatMessage("Contact.Import.MissingFEIN", episode, name));
					valid = false;
				}
				if (StringUtils.isEmpty(cti.getTaxClassification())) {
					msgs.add(MsgUtils.formatMessage("Contact.Import.MissingTaxClassification",
							episode,
							name));
					valid = false;

				}
			}
		}
		String noFEIN = FedIdConverter.checkTaxId(cti.getFein());
		if ((! StringUtils.isEmpty(noFEIN)) && noFEIN.equals("Invalid")) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidFEIN", episode, name));
			valid = false;
		}

		if (! StringUtils.isEmpty(cti.getTaxClassification())) {
			if (! cti.getTaxClassification().equals("C") &&
					! cti.getTaxClassification().equals("S")) {
				msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidTaxClassification", episode,
						name));
				valid = false;
			}
		}

		if (! valid) {
			cti.setAction(ActionType.N_A);
			contactImportDAO.attachDirty(cti);
			//msgs.add(MsgUtils.formatMessage("", count, cti.toString()));
		}
		return valid;
	}

	/**
	 * Create Projects, Users, and Contacts from the data in the ContactImport
	 * table for the current Production.
	 *
	 * @param accessAllowed True if the new Contact's are to be given production
	 *            access.
	 * @param sendInvitation True if an email invitation should be sent to each
	 *            new Contact.
	 * @param msgs A List of messages; this method may add message strings to
	 *            this List.
	 *
	 * @return List of messages for any errors that were encountered.
	 */
	public List<String> createFromTable(Boolean accessAllowed, boolean sendInvitation, List<String> msgs) {
		production = SessionUtils.getProduction();
		boolean commercial = production.getType().isAicp();
		List<ContactImport> list = contactImportDAO.findByProperty("production", production);
		for (ContactImport cti : list) {
			processImportable(cti, accessAllowed, sendInvitation, commercial, msgs);
		}
		return msgs;
	}

	/**
	 * @param cti The Importable defining the user/contact to be processed.
	 *            Depending on the action specified, it may add, replace, or
	 *            delete a contact in the Production.
	 * @param accessAllowed True if the new Contact's are to be given production
	 *            access.
	 * @param sendInvitation True if an email invitation should be sent to each
	 *            new Contact.
	 * @param commercial True iff the production being updated is a Commercial
	 *            production type.
	 * @param msgs A List of messages; this method may add message strings to
	 *            this List.
	 */
	public User processImportable(Importable cti, Boolean accessAllowed, boolean sendInvitation,
			boolean commercial, List<String> msgs) {
		Department dept = null;
		Role role = null;
		Unit unit = null;
		user = null;
		if (cti.getAction() != ActionType.N_A) {
			if (! findUser(cti, msgs)) { // invalid email address
				return null;
			}
			// 'user' has been set based on email address (may be null)
			String title = cti.getEpisodeTitle();
			if (! title.equals(lastEpisode)) { // episode change, get new Project
				project = findOrCreateProject(cti, commercial, msgs);
				lastEpisode = title;
			}
			if (project != null) {
				if (commercial) {
					commProject = project;
				}
				if (cti.getAction() != ActionType.DELETE) {
					// Add or Replace actions...
					dept = findOrCreateDept(cti, commProject, msgs);
					if (dept != null) {
						role = findOrCreateRole(cti, dept, msgs);
						if (role != null) {
							unit = project.getMainUnit();
							Employment emp = new Employment(role, role.getName(), null);
							emp.setProject(commProject); // non-null only for commercial

							Contact ct = findOrCreateContact(cti, emp, unit, accessAllowed,
									sendInvitation, msgs);
							if (ct != null) {
								boolean isImport = (cti instanceof ContactImport);
								if (isImport) {
									ContactImport ctImport = (ContactImport)cti;
									if (production.getAllowOnboarding() && (ctImport.getRate() == null)) {
										// Skip Start form if Onboarding enabled, unless rate specified.
										needStart = false;
									}
									if (needStart) {
										createStart(ctImport, ct, emp, commercial, msgs);
									}
								}
								else { // Called by Web Service
									SessionUtils.setCurrentProject(project);
									// Distribute start packet if the packet exists. Used by Web Service.
									// If the packet name is not null or empty, check to see if the packet exists. If it
									// does, check to see if there are any documents associated with the packet.
									// If either of those conditions fail return the appropriate error message.
									if (packetName != null && ! packetName.isEmpty()) {
										List<Employment> newestEmploymentRecord = new ArrayList<>();
										List<String> errorMsgs;

										// Get the last employment record added.
										newestEmploymentRecord.add(ct.getEmployments().get(ct.getEmployments().size() - 1));

										errorMsgs = OnboardService.getInstance().deliverStarts(ApprovalStatus.OPEN, ct, true, packetName,
												newestEmploymentRecord, production, project);
										if (errorMsgs != null && ! errorMsgs.isEmpty()) {
											msgs.addAll(errorMsgs);
										}
									}
								}
							}
						}
					}
				}
				else {
					deleteMember(cti, project, commercial, msgs);
				}
			}
		}
		return user;
	}

	/**
	 * Find the Project specified in the ContactImport record; if not found,
	 * create it in the current Production.
	 *
	 * @param cti The ContactImport being processed.
	 * @param commercial True iff the current Production is a commercial production.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 * @return The Project matching the ContactImport episode code, either
	 *         previously existing or newly created.
	 */
	private Project findOrCreateProject(Importable cti, boolean commercial, List<String> msgs) {
		Project proj = null;
		List<Project> projects;
		if (cti instanceof CreateUserContact) { // Web Service code
			projects = projectDAO.findByProductionAndEpisode(production, cti.getEpisodeCode());
			if (projects.size() == 0) {
				projects = projectDAO.findByProductionAndTitle(production, cti.getEpisodeTitle());
			}
		}
		else { // Import via UI (on Projects page)
			projects = projectDAO.findByProductionAndTitle(production, cti.getEpisodeTitle());
		}
		if (projects.size() > 1) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MultipleProjectsFound", cti.getEpisodeCode()));
		}
		else if (projects.size() == 0) {
			if (cti.getAction() == ActionType.CREATE) {
				// create new project
				String title = cti.getEpisodeTitle();
				if (title.length() > 35) {
					title = title.substring(0, 35);
				}
				projects = projectDAO.findByProductionAndTitle(production, title);
				if (projects.size() > 0) {
					msgs.add(MsgUtils.formatMessage("Contact.Import.DuplicateTitle", cti.getEpisodeCode(), title));
					if (title.length() > 34-cti.getEpisodeCode().length()) {
						title = title.substring(0, 34-cti.getEpisodeCode().length());
					}
					title += "-" + cti.getEpisodeCode();
				}
				proj = new Project();
				proj.setTitle(title);
				String code = cti.getEpisodeCode();
				if (code.length() > 20) {
					code = code.substring(0, 20);
				}
				proj.setEpisode(code);
				if (cti.getProjectedEndDate() != null) {
					proj.setOriginalEndDate(cti.getProjectedEndDate());
				}
				else {
					Calendar cal = new GregorianCalendar();
					cal.setTime(cti.getProjectStartDate());
					cal.add(Calendar.MONTH, 1);
					proj.setOriginalEndDate(cal.getTime());
				}
				proj.setProduction(production);
				proj.setTimeZoneStr(production.getTimeZoneStr());
				Project sourceProject = SessionUtils.getCurrentProject();
				if (sourceProject == null) {
					sourceProject = production.getDefaultProject();
				}
				if (sourceProject == null) {
					sourceProject = production.getProjects().iterator().next();
				}
				sourceProject = projectDAO.refresh(sourceProject);
				boolean done = projectDAO.createNewProject(proj, sourceProject, cti.getProjectStartDate(), true, false, false,false, false, false);
				if (! done) { // Create failed for some reason
					proj = null;
					msgs.add(MsgUtils.formatMessage("Contact.Import.CreateProjectFailed", cti.getEpisodeCode()));
				}
				else {
					if (commercial) {
						PayrollPreference pref = proj.getPayrollPref();
						pref.setWorkCity(cti.getWorkCity());
						pref.setWorkState(cti.getWorkState());
						contactImportDAO.attachDirty(pref);
					}
				}
				proj = projectDAO.refresh(proj);
			}
			else {
				if (cti.getAction() == ActionType.DELETE) {
					msgs.add(MsgUtils.formatMessage("Contact.Import.MissingProject.Delete", cti.getEpisodeCode(),
							cti.getLastNameFirstName()));
				}
				else {
					msgs.add(MsgUtils.formatMessage("Contact.Import.MissingProject.Replace", cti.getEpisodeCode(),
							cti.getLastNameFirstName()));
				}
			}
		}
		else {
			proj = projects.get(0);
		}
		return proj;
	}

	/**
	 * Find the Department specified in the ContactImport record; if not found,
	 * create it in the specified Project as a custom (user-specified)
	 * department.
	 *
	 * @param cti The ContactImport being processed.
	 * @param proj The Project in which the Department should exist, if it is
	 *            a custom department.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 * @return The Department matching the ContactImport information, either
	 *         previously existing or newly created.
	 */
	private Department findOrCreateDept(Importable cti, Project proj, List<String> msgs) {
		Department dept = null;
		String dep = "";
		if (cti.getDepartment() != null) {
			dep = cti.getDepartment();
		}
		if (dep.length() == 0) {
			dept = departmentDAO.findById(Constants.DEFAULT_DEPARTMENT_ID);
		}
		else {
			dept = departmentDAO.findByProductionAndNameAny(production, proj, dep);
			if (dept == null) {
				if (cti.getAction() != ActionType.DELETE) {
					dept = new Department();
					dept.setName(dep);
					dept.setProject(proj);
					dept.setListPriority(departmentDAO.findMaxPriority(production) + 1);
					dept.setProduction(production);
					dept.setMask(Department.UNIQUE_DEPT_MASK);
					departmentDAO.save(dept);
				}
			}
		}
		return dept;
	}

	/**
	 * Find the role specified in the ContactImport record; if not found, create
	 * it in the specified Department.
	 *
	 * @param cti The ContactImport being processed.
	 * @param dept The Department in which the Role should exist.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 * @return The Role matching the ContactImport information, either
	 *         previously existing or newly created.
	 */
	private Role findOrCreateRole(Importable cti, Department dept, List<String> msgs) {
		Role role = null;
		String roleName = "";
		if (cti.getOccupation() != null) {
			roleName = cti.getOccupation();
		}
		if (roleName.length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingOccupation",
					cti.getEpisodeCode(), cti.getLastNameFirstName()));
		}
		else {
			role = roleDAO.findByProdDeptAndNameCustom(production, dept.getName(), roleName);
			if (role == null && cti.getAction() != ActionType.DELETE) {	// doesn't exist yet, so create it.
				role = roleDAO.addRole(roleName, dept);
			}
		}
		return role;
	}

	/**
	 * Find the Contact specified by the email address in the ContactImport
	 * record; if not found, create it as specified in the given ProjectMember.
	 * A User instance will also be created, if necessary.
	 *
	 * @param cti The ContactImport being processed.
	 * @param emp The Employment to be associated with the Contact being
	 *            created.
	 * @param accessAllowed True if the new Contact is to be given production access.
	 * @param sendInvitation True if an email invitation should be sent to the new Contact.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 * @return
	 */
	private Contact findOrCreateContact(Importable cti, Employment emp, Unit unit,
			Boolean accessAllowed, boolean sendInvitation, List<String> msgs) {
		Contact contact = null;
		needStart = false;
		if (user != null) {
			contact = contactDAO.findByUserProduction(user, production);
		}
		if (contact == null) { // new User, and/or new Contact (new to this Production)
			needStart = true;
			contact = new Contact();
			String email = cti.getEmailAddress();
			if (user == null) { // need to create User
				user = new User(production.getType().isCanadaTalent());
				user.setFirstName(cti.getFirstName());
				user.setLastName(cti.getLastName());
				user.setDisplayName(user.getFirstNameLastName());
				if (user.getDisplayName().length() < 1) {
					msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidName", email,
							cti.getEpisodeCode(), cti.getLastNameFirstName()));
					return null;
				}
				if (! StringUtils.isEmpty(cti.getPhone())) {
					user.setCellPhone(cti.getPhone()); // assume it's cell phone
					user.setPrimaryPhoneIndex(User.PRIMARY_PHONE_CELL);
				}
			}
			contact.setEmailAddress(email);
			contact.setDisplayName(user.getDisplayName()); // LS-516
			contact.setStatus(MemberStatus.PENDING);
			contact.setLoginAllowed(accessAllowed);
			contact.setRole(emp.getRole());
			emp.setContact(contact);
			emp.setDefRole(true); // first emp for this contact, must be default in project
			contact.getEmployments().add(emp);
			contact.setProduction(production);
			contact.setCreatedBy(creatingAccount);
//			if (! StringUtils.isEmpty(cti.getPhone())) {
//				contact.setCellPhone(cti.getPhone()); // assume it's cell phone
//				contact.setPrimaryPhoneIndex(User.PRIMARY_PHONE_CELL);
//			}
			ProjectMember pm = new ProjectMember(unit, emp);
			emp.getProjectMembers().add(pm);
			// Emp and pm will be saved via cascade from contact
			contactDAO.attachContactUser(contact, user, sendInvitation, null);
		}
		else { // have Contact, check if PM must be added.
			boolean found = false;
			if (! contact.getStatus().isActive()) {
				contact.setStatus(MemberStatus.PENDING);
				contact.setLoginAllowed(accessAllowed);
			}
			Employment oldEmp = null;
			boolean projectMatch = false; // true if find at least one existing emp in project
		empLoop:
			for (Employment ep : contact.getEmployments()) {
				boolean epProjectMatch = equalOrNull(ep.getProject(), emp.getProject());
				projectMatch |= epProjectMatch;
				if (ep.getRole().equals(emp.getRole()) && epProjectMatch) {
					oldEmp = ep;
					for (ProjectMember pm : ep.getProjectMembers()) {
						if (pm.getUnit() == null) {
							found = true;
							break empLoop;
						}
						else if (pm.getUnit().equals(unit)) {
							found = true;
							break empLoop;
						}
					}
				}
			}
			if (! found) { // need to add PM
				if (oldEmp == null) { // new role, use new Employment passed as arg
					emp.setContact(contact);
					contact.getEmployments().add(emp);
					if (contact.getEmployments().size() == 1) {
						contact.setRole(emp.getRole());
					}
					needStart = true;
				}
				else { // existing role, add PM to existing Employment
					emp = oldEmp;
					needStart = false;
				}
				if (! projectMatch) {
					// LS-516 set first employment in project as default
					emp.setDefRole(true);
				}
				// TODO what about default role settings?
				ProjectMember pm = new ProjectMember(unit, emp);
				emp.getProjectMembers().add(pm);
				if (cti.getAction() == ActionType.UPDATE) {
					// For "replace", first remove existing roles in this project/production
					Project proj = unit.getProject();
					contact.getEmployments().remove(emp);
					contact = removeRoles(contact, proj);
					contact.getEmployments().add(emp);
				}
				contactDAO.attachDirty(contact);
			}
		}
		return contact;
	}

	/**
	 * Create and save a new StartForm for the contact being imported.
	 *
	 * @param cti The ContactImport being processed.
	 * @param project The Project that matches the given ContactImport.
	 * @param contact The Contact matching the given ContactImport.
	 * @param emp The Employment matching the given ContactImport.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 */
	private void createStart(ContactImport cti, Contact contact, Employment emp, boolean commercial, List<String> msgs) {

		// NOTE: shouldn't come here for delete!

		String addSdFormType = StartForm.FORM_TYPE_NEW;
		Project proj = emp.getProjectMembers().get(0).getUnit().getProject();
		if (cti.getAction() == ActionType.UPDATE) {
			contact = contactDAO.removeStarts(contact, (commercial ? proj : null));
		}
		Date creation = new Date();
		Date effectiveStart = cti.getProjectStartDate();
		StartForm sf = StartFormService.createStartForm(addSdFormType, null, null, contact, emp,
				proj, creation, effectiveStart, null/*end*/, false/*copy*/, false/*no save*/);
		sf.setEffectiveEndDate(cti.getProjectedEndDate());
		sf.setJobNumber(cti.getEpisodeCode()); // Imported crew may have varying job#'s for same project!
		sf.setPhone(cti.getPhone());
		if (! StringUtils.isEmpty(cti.getWorkCity())) {
			// Note: A non-blank work city/state in the import will override the project's payroll preference.
			sf.setWorkCity(cti.getWorkCity());
			sf.setWorkState(cti.getWorkState());
		}
		if (cti.getSocialSecurity() != null) {
			sf.setSocialSecurity(cti.getSocialSecurity());
		}
		if (cti.getRateType() != null) {
			sf.setRateType(cti.getRateType());
			if (sf.getRateType() == EmployeeRateType.DAILY) {
				sf.setAllowWorked(true);
			}
		}
		else {
			sf.setRateType(EmployeeRateType.HOURLY);
		}
		if (cti.getRate() != null && cti.getRate().signum() > 0) {
			sf.setRate(cti.getRate());
		}
		if (cti.getGuarHours() != null && cti.getGuarHours().signum() > 0) {
			sf.setGuarHours(sf.getProd(), cti.getGuarHours());
		}
		// LS-3043
		if (cti.getPayIndicator() != null &&
				(cti.getPayIndicator().equals("I") || cti.getPayIndicator().equals("C"))) {
			if (cti.getPayIndicator().equals("I")) {
				sf.setPaidAs(PaidAsType.I);
			}
			else if (cti.getPayIndicator().equals("C")) {
				sf.setPaidAs(PaidAsType.LO);
			}
			sf.setLoanOutCorpName(cti.getLoanOutName());
			sf.setFederalTaxId(cti.getFein());
			if (! StringUtils.isEmpty(cti.getTaxClassification())) {
				sf.setTaxClassification(TaxClassificationType.valueOf(cti.getTaxClassification()));
			}
		}
		contactImportDAO.save(sf);
		if (production.getAllowOnboarding()) {
			StartFormDAO.getInstance().createContactDocument(sf, production);
		}
	}

	/**
	 * Find the User instance matching the given ContactImport's email address
	 * field, and set the {@link #user} field accordingly. It will be set to
	 * null if the email address is not an existing User.
	 *
	 * @param cti The ContactImport being processed.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated. If this method returns false, at least one
	 *            error message will have been added to the list.
	 * @return True iff the ContactImport's email address is syntactically
	 *         valid.
	 */
	private boolean findUser(Importable cti, List<String> msgs) {
		boolean retn = false;
		user = null;
		String email = cti.getEmailAddress();
		if (email == null || email.length() == 0) {
			msgs.add(MsgUtils.formatMessage("Contact.Import.MissingEmailAddress",
					cti.getEpisodeCode(), cti.getLastNameFirstName()));
		}
		else {
			if (! EmailValidator.isValidEmail(email)) {
				msgs.add(MsgUtils.formatMessage("Contact.Import.InvalidEmailAddress", email,
						cti.getEpisodeCode(), cti.getLastNameFirstName()));
			}
			else {
				user = userDAO.findOneByProperty(UserDAO.EMAIL_ADDRESS, email);
				retn = true; // valid email, whether or not user is found
			}
		}
		return retn;
	}

	/**
	 * Delete the person described in the import record, either from the
	 * Project, for a commercial production, or from the entire production for a
	 * TV/Feature production.
	 *
	 * @param cti The ContactImport being processed.
	 * @param proj The Project that matches the given ContactImport.
	 * @param commercial True iff the current Production is a commercial
	 *            Production.
	 * @param msgs A List of messages, to which this method may add any error
	 *            messages generated.
	 */
	private void deleteMember(Importable cti, Project proj, boolean commercial, List<String> msgs) {
		Contact contact = null;
		if (user != null) {
			contact = contactDAO.findByUserProduction(user, production);
			if (contact != null) {
				if (production.getType().getEpisodic() && proj != null) { // just delete from the specified Project
					int projectId = proj.getId();
					boolean doMsg = true;
					boolean doSave = false;
					for (Iterator<Employment> it = contact.getEmployments().iterator(); it.hasNext(); ) {
						Employment emp = it.next();
						Role role = emp.getRole();
						if (role.isProductionWide()) {
							msgs.add(MsgUtils.formatMessage("Contact.Import.CantDeleteAdmin",
									cti.getEpisodeCode(), cti.getLastNameFirstName(), cti.getEmailAddress()));
							doMsg = false;
						}
						else {
							for (Iterator<ProjectMember> pmIt = emp.getProjectMembers().iterator(); pmIt.hasNext(); ) {
								ProjectMember pm = pmIt.next();
								if (pm.getUnit().getProject().getId().equals(projectId)) {
									pmIt.remove();
									contactDAO.delete(pm);
									doSave = true;
								}
							}
							if (emp.getProjectMembers().size() == 0) {
								// No PM's left, so delete the Employment, too.
								it.remove();
								contactDAO.delete(emp);
								doSave = true;
							}
						}
					}
					if (doSave) {
						contact = contactDAO.actionSave(contact, false, contact.getEmployments());
					}
					else if (doMsg) {
						msgs.add(MsgUtils.formatMessage("Contact.Import.DeleteNoRoles",
								cti.getEpisodeCode(), cti.getLastNameFirstName(), cti.getEmailAddress()));
					}
					contact = contactDAO.removeStarts(contact, proj);
				}
				else { // Feature - delete Contact from Production
					contact.setStatus(MemberStatus.DELETED);
					// ensure User and Contact email's match, so we don't get problems during "re-invites"
					contact.setEmailAddress(contact.getUser().getEmailAddress());
					contactDAO.removeContact(contact);	// remove the Contact from this Production
					// remove Contact as a time-keeper if necessary...
					departmentDAO.updateTimeKeeper(contact);
				}
			}
			else {
				msgs.add(MsgUtils.formatMessage("Contact.Import.DeleteNotInProduction",
						cti.getEpisodeCode(), cti.getLastNameFirstName(), cti.getEmailAddress()));
			}
		}
		else {
			msgs.add(MsgUtils.formatMessage("Contact.Import.DeleteUnknownEmail",
					cti.getEpisodeCode(), cti.getLastNameFirstName(), cti.getEmailAddress()));
		}
	}

	/**
	 * Remove all the roles (ProjectMembers) that the given Contact has, either
	 * within the Production (for features), or within the given Project (for
	 * TV/Commercial).
	 *
	 * @param contact The Contact whose roles should be removed.
	 * @param proj The relevant Project for an episodic production.
	 * @return The same Contact, probably updated (may be a different instance).
	 */
	@SuppressWarnings("unused")
	private Contact removeRoles(Contact contact, Project proj) {
		Collection<Integer> deletedPms = new ArrayList<>();
		if (production.getType().getEpisodic() && proj != null) { // just delete from the specified Project
			int projectId = proj.getId();
			for (Iterator<Employment> iter = contact.getEmployments().iterator(); iter.hasNext(); ) {
				Employment emp = iter.next();
				Role role = emp.getRole(); // TODO
				if (! role.isProductionWide()) { // skip production-wide roles
					if (emp.getProject() == null) { // TV or Feature
						for (Iterator<ProjectMember> pmIt = emp.getProjectMembers().iterator(); pmIt.hasNext(); ) {
							ProjectMember pm = pmIt.next();
							if (pm.getUnit().getProject().getId().equals(projectId)) {
								pmIt.remove();
								contactDAO.delete(pm);
							}
						}
						if (emp.getProjectMembers().size() == 0) {
							// all membership was removed, so delete Employment if possible
							if (! StartFormDAO.getInstance().existsTimecardsForEmployment(emp)) {
								contactDAO.delete(emp);
							}
						}
					}
					else if (emp.getProject().getId().intValue() == projectId) {
						// have commercial entry to be deleted
						if (StartFormDAO.getInstance().existsTimecardsForEmployment(emp)) {
							// has timecards - not allowed to delete the Employment;
							for (ProjectMember pmIt : emp.getProjectMembers()) {
								// so just delete all the PM's for it.
								contactDAO.delete(pmIt);
							}
							emp.getProjectMembers().clear();
						}
						else {
							contactDAO.delete(emp);
							iter.remove();
						}
					}
				}
			}
		}
		else { // Feature - delete all PMs from Contact
			for (Employment emp : contact.getEmployments()) {
				contactDAO.delete(emp);
			}
			contact.getEmployments().clear();
		}
		contact = contactDAO.actionSave(contact, false, contact.getEmployments());
		return contact;
	}

	/**
	 * Compare two Project instances.
	 *
	 * @param proj1 One project to compare.
	 * @param proj2 The other project to compare.
	 * @return True if both are null, or if they are non-null and equal to each
	 *         other.
	 */
	private boolean equalOrNull(Project proj1, Project proj2) {
		if (proj1 == null && proj2 == null) {
			return true;
		}
		if (proj1 != null) {
			return proj1.equals(proj2);
		}
		return false;
	}

}
