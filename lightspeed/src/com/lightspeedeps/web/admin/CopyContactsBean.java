package com.lightspeedeps.web.admin;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * Backing bean for the "Copy Data" mini-tab within the Admin / Misc page. This
 * supports:
 * <ul>
 * <li>copying Contact`s and their associated ProjectMember and StartForm
 * entities from a selected Production (the "source") into the current
 * Production (the "target").
 * <li>copying timecards from a selected Production into the current production.
 * </ul>
 */
@ManagedBean
@ViewScoped
public class CopyContactsBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 933732554170644305L;

	private static final Log log = LogFactory.getLog(CopyContactsBean.class);

	// Fields

	/** The List of Contact`s displayed, which (currently) is the list of eligible
	 * Contact`s to be copied from the source to the target. */
	private List<Contact> contacts;

	/** The List of SelectItem`s representing the possible source Production`s.
	 * The value of each item is the production.id, and the label is the production.title. */
	private List<SelectItem> productionDL;

	/** The database id of the selected source Production. Set to -1 when no Production
	 * has been selected. */
	private Integer sourceProdId = -1;

	/** The selected source Production. null if no Production has been selected. */
	private Production sourceProd;

	/** The source Project, which will be the default Project of the selected Production. */
	private Project sourceProject;

	/** For Commercial productions, the same as 'sourceProject'; for TV & Feature productions,
	 * null.  This value is used when methods need to retrieve data specific to a
	 * project for Commercial productions. */
	private Project sourceCommercialProject;

	/** The target Production, which is (currently) the user's current Production. */
	private Production targetProd;

	/** The target Project, which is (currently) the user's current Project. */
	private Project targetProject;

	/** For Commercial productions, the target project; for TV & Feature productions,
	 * null.  This value is used when methods need to make the data specific to a
	 * project for Commercial productions. */
	private Project targetCommercialProject;

	/** True iff the copy of timecards should overwrite existing timecards in the
	 * target Production. */
	private boolean overWriteTc;

	/** When copying timecards, only include ones with a week-ending date of this
	 * value or later. */
	private Date copyStartDate;

	/** When copying timecards, only include ones with a week-ending date of this
	 * value or earlier. */
	private Date copyEndDate;


	/* Constructor */
	public CopyContactsBean() {
		log.debug("");
		try {
			targetProd = SessionUtils.getNonSystemProduction();
			targetProject = SessionUtils.getCurrentProject();
			if (targetProd.getType().hasPayrollByProject()) {
				targetCommercialProject = targetProject;
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 */
	private void forceLazyInit() {
	}

	/**
	 * Action method for the "Import" button.
	 *
	 * @return null navigation string
	 */
	public String actionCopyContacts() {
		try {
			int count = copyContacts(sourceProject, targetProject, contacts);
			MsgUtils.addFacesMessage("Contact.Copy.Done", FacesMessage.SEVERITY_INFO, count);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Copy Timecards" button.
	 *
	 * @return null navigation string
	 */
	public String actionCopyTimecards() {
		try {
			copyTimecards(sourceProd, targetProd, copyStartDate, copyEndDate, overWriteTc);
//			MsgUtils.addFacesMessage("Timecard.Copy.Done", FacesMessage.SEVERITY_INFO, count);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Copy all timecards with matching StartForm`s from one production to
	 * another, within an optional range of dates.
	 *
	 * @param sourcePrd The Production with the original (source) timecards.
	 * @param targetPrd The Production to which copies of the source timecards
	 *            will be added.
	 * @param start Timecards with a week-ending date earlier than this value
	 *            will be ignored.
	 * @param end Timecards with a week-ending date later than this value will
	 *            be ignored.
	 * @param overWrite If true, existing timecards in the target production
	 *            will be replaced if a matching source timecard is encountered.
	 * @return The total number of timecards copied (added or replaced).
	 */
	private int copyTimecards(Production sourcePrd, Production targetPrd,
			Date start, Date end, boolean overWrite) {
		int count = 0;
		int matchedSf = 0;
		List<String> sourceAcctNums = UserDAO.getInstance().findAcctsByProduction(sourcePrd);
		List<String> targetAcctNums = UserDAO.getInstance().findAcctsByProduction(targetPrd);
		List<String> copyAcctNums = new ArrayList<>();
		for (String acct : sourceAcctNums) {
			if (targetAcctNums.contains(acct)) {
				copyAcctNums.add(acct);
			}
		}
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		for (String acct : copyAcctNums) {
			// get source contact by acctNumber
			Contact sc = ContactDAO.getInstance().findByAccountNumAndProduction(acct, sourcePrd);
			// get target contact by acctNumber
			Contact tc = ContactDAO.getInstance().findByAccountNumAndProduction(acct, targetPrd);
			// Get source StartForm`s by contact and optional Project (for Commercial productions).
			// Since this is admin/QA tool, don't bother filtering Starts by status or anything else.
			List<StartForm> sourceSfs = StartFormDAO.getInstance().findByContactProject(sc, sourceCommercialProject, false);

			// For each source startForm, copy the timecards it created & attach them to the matching target StartForm
			for (StartForm sf : sourceSfs) {
				// Find a matching StartForm belonging to the target contact
				StartForm targetSf = StartFormDAO.getInstance().findByContactPosition(tc, sf.getJobClass());
				if (targetSf != null) {
					matchedSf++;
					Project targetProj = targetSf.getProject();
					// get source timecards by StartForm
					List<WeeklyTimecard> sourceTcs = weeklyTimecardDAO.findByProperty(WeeklyTimecardDAO.START_FORM, sf);
					for (WeeklyTimecard wtc : sourceTcs) {
						if (end != null && wtc.getEndDate().after(end)) {
							continue; // timecard is later than specified cut-off date; skip it;
						}
						if (start != null && wtc.getEndDate().before(start)) {
							continue; // timecard is earlier than specified start date; skip it;
						}
						// see if there's any existing timecards in the target production that match the source
						List<WeeklyTimecard> oldTcs = weeklyTimecardDAO.
								findByWeekEndDateAccountOccupation(targetPrd, targetProj, wtc.getEndDate(), acct, wtc.getOccupation());
						if (overWrite || oldTcs.size() == 0) {
							// no match, or user said to overwrite existing ones...
							WeeklyTimecard newTc = wtc.deepCopyFor(targetPrd, targetCommercialProject); // copy the source
							newTc.setStartForm(targetSf);
							if (oldTcs.size() > 0) {
								WeeklyTimecard oldTc = oldTcs.get(0); // probably only 1
								if (oldTc.getAdjusted() != newTc.getAdjusted() && oldTcs.size() > 1) {
									// but if adjusted exists, could be either.
									// ** THIS CODE IS UNRELIABLE IF MORE THAN ONE ADJUSTED TC **
									oldTc = oldTcs.get(1);
								}
								weeklyTimecardDAO.delete(oldTc);
							}
							weeklyTimecardDAO.save(newTc);
							count++;
						}
					}
				}
				else {
					log.warn("no match in target for " + sf.getLastName() + " as " + sf.getJobClass());
				}
			}
		}
		MsgUtils.addFacesMessage("Timecard.Copy.Done", FacesMessage.SEVERITY_INFO, count, matchedSf);
		return count;
	}

	/**
	 * Copy a set of Contacts and their related ProjectMember and StartForm
	 * objects from one production to another.
	 *
	 * @param sourceProj The Project from which the Contact`s are being
	 *            copied.
	 * @param targetProj The Project to which copies of the Contact`s and
	 *            their ProjectMember and StartForm entities will be added.
	 * @param sourceCts The List of Contact entities to copy. Contact's that
	 *            represent users who are already members of the target
	 *            Production will not be copied.
	 * @return The number of Contact`s added to the target Project.
	 */
	private int copyContacts(Project sourceProj, Project targetProj, List<Contact> sourceCts) {
		ContactDAO contactDAO = ContactDAO.getInstance();
		ProjectDAO projectDAO = ProjectDAO.getInstance();
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		RoleDAO roleDAO = RoleDAO.getInstance();
		DepartmentDAO departmentDAO = DepartmentDAO.getInstance();
		int added = 0;

		targetProj = projectDAO.refresh(targetProj);
		sourceProj = projectDAO.refresh(sourceProj);

		/** The RoleGroup to use when creating custom Roles. */
		RoleGroup customRoleGroup = RoleGroupDAO.getInstance().findById(Constants.ROLE_GROUP_ID_CUSTOM_ROLES);
		/** The target Production, to which the new Contacts are being added. */
		Production targetPrd = targetProj.getProduction();
		/** The Unit in the target production to which new ProjectMember`s will be added. */
		Unit targetUnit = targetProj.getMainUnit();
		/** A mapping from the old StartForm.id field to the id of the copy of it added to the target production. */
		Map<Integer, Integer> startMap = new HashMap<>();

		/** The List of existing Contact`s within the target Production. */
		List<Contact> tcs = contactDAO.findByProperty(ContactDAO.PRODUCTION, targetPrd);

		/** The List of User`s who already belong to the target Production.  We will not try to
		 * create new Contact`s for these User`s. */
		Collection<User> users = new HashSet<>(tcs.size());

		for (Contact c : tcs) { // generate the list of Users in the target production.
			users.add(c.getUser());
		}

		for (Contact contact : sourceCts) { // loop through provided list of eligible Contact`s
			contact = contactDAO.refresh(contact);
			if (users.contains(contact.getUser())) {
				continue; // skip this contact - already in target production
			}
			Contact target = contact.clone();
			contactDAO.save(target);
			List<Employment> empList = new ArrayList<>();
			for (Employment emp : contact.getEmployments()) {
				// we only copy roles that are production-wide, or in the main (#1) unit
					Employment newEmployment = emp.clone();
					newEmployment.setContact(target);
					// 'targetCommercialProject' == null for TV/Feature, and 'targetProject' for commercial.
					newEmployment.setProject(targetCommercialProject);
					Department department = emp.getRole().getDepartment();
					if (department.isCustom()) {
						// See if the custom Department already exists in the target Production
						Department newDept = departmentDAO.
								findByProductionProjectDept(targetPrd, targetCommercialProject, department.getName());
						if (newDept == null) { // no -- need to add it.
							newDept = department.clone();
							newDept.setProduction(targetPrd);
							newDept.setProject(targetCommercialProject);
							departmentDAO.save(newDept);
						}
						department = newDept;
					}
//					newEmployment.setDepartment(department);

					if (emp.getRole().isCustom()) {
						// See if the custom Role already exists in the target Production and Department
						Role newRole = roleDAO.findByProdDeptAndNameCustom(targetPrd, department.getName(), emp.getRole().getName());
						if (newRole == null) { // no -- need to add it
							newRole = new Role(targetPrd, emp.getRole().getName(), customRoleGroup, department,
									emp.getRole().getListPriority());
							roleDAO.save(newRole);
						}
						newEmployment.setRole(newRole);
					}
					else {
						newEmployment.setRole(emp.getRole());
					}

					if (emp.getDefRole()) {
						target.setRole(newEmployment.getRole());
					}

					List<ProjectMember> pms = new ArrayList<>();
					newEmployment.setProjectMembers(pms);

					for (ProjectMember pm : emp.getProjectMembers()) {
						if (pm.getUnit() == null || pm.getUnit() == sourceProj.getMainUnit()) {
							ProjectMember newPm = pm.clone();
							newPm.setEmployment(newEmployment);
							if (pm.getUnit() != null) {
								newPm.setUnit(targetUnit);
							}
							pms.add(newPm); // will get saved via cascade from Employment
						}
					}
					EmploymentDAO.getInstance().save(newEmployment);
					empList.add(newEmployment);
					copyStartForms(targetProj, startFormDAO, targetPrd, startMap,
							emp, target, newEmployment);
					EmploymentDAO.getInstance().attachDirty(newEmployment);
			}
			// Did we create any projectMember entries? If so, save the new contact
			if (empList.size() > 0) {
				target.setProduction(targetPrd);
				target.setProject(targetProj);
				target.setEmployments(empList);
				target.setRole(empList.get(0).getRole());
				if (contact.getCastMember() != null) {
					RealWorldElement newCast = contact.getCastMember().clone();
					newCast.setProduction(targetPrd);
					newCast.setActor(target);
					contact.setCastMember(newCast);
				}
				// Note that PMs will be saved via Hibernate cascade
				contactDAO.attachDirty(target);

				added++;
			}
		}
		return added;
	}

	private void copyStartForms(Project targetProj, StartFormDAO startFormDAO,
			Production targetPrd, Map<Integer, Integer> startMap,
			Employment employment, Contact target, Employment newEmployment) {
		// Copy StartForm`s for this Contact
		List<StartForm> starts = startFormDAO.findByProperty("employment", employment);
		startMap.clear(); // only need to map current Contact's StartForms
		for (StartForm start : starts) {
			StartForm newSt = start.deepCopy(); // clone StartForm and all rate tables
			newSt.setContact(target);
			newSt.setProdCompany(targetPrd.getStudio());
			newSt.setProdTitle(targetPrd.getTitle());
			newSt.setProject(targetCommercialProject);
			newSt.setFormNumber(target.getId() + "-" + newSt.getSequence());
			newSt.setEmployment(newEmployment);
			if (start.getPriorFormId() != null) {
				// new "prior form" link -- look up replacement id in our map
				Integer newPriorId = startMap.get(start.getPriorFormId());
				newSt.setPriorFormId(newPriorId);
			}
			if (targetCommercialProject != null) {
				newSt.setJobName(targetProj.getTitle());
				newSt.setJobNumber(targetProj.getEpisode());
				newSt.setWorkCity(targetProj.getPayrollPref().getWorkCity());
				newSt.setWorkState(targetProj.getPayrollPref().getWorkState());
				newSt.setWorkZip(targetProj.getPayrollPref().getWorkZip()); // LS-2343
				newSt.setWorkCountry(targetProj.getPayrollPref().getWorkCountry());
				newSt.getEmployment().setWageState(targetProj.getPayrollPref().getOvertimeRule());
			}
			else {
				newSt.setWorkCity(targetPrd.getPayrollPref().getWorkCity());
				newSt.setWorkState(targetPrd.getPayrollPref().getWorkState());
				newSt.setWorkZip(targetPrd.getPayrollPref().getWorkZip()); // LS-2343
				newSt.setWorkCountry(targetPrd.getPayrollPref().getWorkCountry());
				newSt.getEmployment().setWageState(targetPrd.getPayrollPref().getOvertimeRule());
			}
			startFormDAO.save(newSt);
			newEmployment.getStartForms().add(newSt); // complete sf-employment relationship

			// keep track of pairs of old SF id and new SF id
			startMap.put(start.getId(), newSt.getId());
		}
	}

	/**
	 * Value Change Listener for the productions drop-down list.
	 */
	public void listenProduction(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				Integer id = (Integer)event.getNewValue();
				// Clear fields to disable the copy buttons on selecting "Select Production" from the drop down,
				// or if production is not found(?).
				sourceProdId = null;
				sourceProd = null;
				sourceProject = null;
				contacts = null;
				if (id > 0) {
					Production prod = ProductionDAO.getInstance().findById(id);
					if (prod != null) {
						sourceProdId = id;
						sourceProd = prod;
						sourceProject = sourceProd.getDefaultProject();
						if (sourceProd.getType().hasPayrollByProject()) {
							sourceCommercialProject = sourceProject;
						}
						else {
							sourceCommercialProject = null;
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @return The list of SelectItem`s representing Production`s which the
	 *         current user belongs to.
	 *         <p>
	 *         Note that for a user-enabled feature, this list would need
	 *         additional filtering based on the user's permissions within each
	 *         such Production.
	 */
	private List<SelectItem> createProductions() {
		List<Production> prods = ProductionDAO.getInstance().findByUser(SessionUtils.getCurrentUser());
		List<SelectItem> prodDL = new ArrayList<>();
		prodDL.add(new SelectItem(-1, "-- select production --"));
		for (Production prod : prods) {
			if (! prod.isSystemProduction()) {
				prodDL.add(new SelectItem(prod.getId(), prod.getTitle()));
			}
		}
		return prodDL;
	}

	/**
	 * @return A List of distinct Contact`s that exist within the default
	 *         Project of the currently selected source Production.
	 */
	private List<Contact> createContactList() {
		List<Contact> conts = new ArrayList<>();
		if (sourceProject != null) {
			List<ProjectMember> pms = ProjectMemberDAO.getInstance().findByProject(sourceProject);
			for (ProjectMember pm : pms) {
				Contact c = pm.getEmployment().getContact();
				if (! conts.contains(c)) {
					conts.add(c);
				}
			}
			Collections.sort(conts, contactNameComparator);
		}
		return conts;
	}

	/**
	 * A Comparator used for sorting the Contact entries by name.
	 */
	protected Comparator<Contact> contactNameComparator = new Comparator<Contact>()
		{
			@Override
			public int compare(Contact c1, Contact c2) {
				return c1.compareTo(c2, Contact.SORTKEY_NAME);
			}
		};

	/**See {@link #sourceProdId}. */
	public Integer getSourceProdId() {
		return sourceProdId;
	}
	/**See {@link #sourceProdId}. */
	public void setSourceProdId(Integer sourceProdId) {
		this.sourceProdId = sourceProdId;
	}

	/**See {@link #productionDL}. */
	public List<SelectItem> getProductionDL() {
		if (productionDL == null) {
			productionDL = createProductions();
		}
		return productionDL;
	}
	/**See {@link #productionDL}. */
	public void setProductionDL(List<SelectItem> productions) {
		productionDL = productions;
	}

	/**See {@link #sourceProd}. */
	public Production getSourceProd() {
		return sourceProd;
	}
	/**See {@link #sourceProd}. */
	public void setSourceProd(Production sourceProd) {
		this.sourceProd = sourceProd;
	}

	/**See {@link #sourceProject}. */
	public Project getSourceProject() {
		return sourceProject;
	}
	/**See {@link #sourceProject}. */
	public void setSourceProject(Project sourceProject) {
		this.sourceProject = sourceProject;
	}

	/**See {@link #contacts}. */
	public List<Contact> getContacts() {
		if (contacts == null) {
			contacts = createContactList();
		}
		return contacts;
	}
	/**See {@link #contacts}. */
	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	/**See {@link #copyStartDate}. */
	public Date getCopyStartDate() {
		return copyStartDate;
	}

	/**See {@link #copyStartDate}. */
	public void setCopyStartDate(Date copyStartDate) {
		this.copyStartDate = copyStartDate;
	}

	/**See {@link #copyEndDate}. */
	public Date getCopyEndDate() {
		return copyEndDate;
	}

	/**See {@link #copyEndDate}. */
	public void setCopyEndDate(Date copyEndDate) {
		this.copyEndDate = copyEndDate;
	}

	/**See {@link #overWriteTc}. */
	public boolean getOverWriteTc() {
		return overWriteTc;
	}
	/**See {@link #overWriteTc}. */
	public void setOverWriteTc(boolean overWriteTc) {
		this.overWriteTc = overWriteTc;
	}

}
