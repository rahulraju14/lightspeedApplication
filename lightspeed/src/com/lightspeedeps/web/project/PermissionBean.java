/**
 * File: PermissionBean.java
 */
package com.lightspeedeps.web.project;

import static com.lightspeedeps.object.TriStateCheckboxExt.CHECK_MIXED;
import static com.lightspeedeps.object.TriStateCheckboxExt.CHECK_NA;
import static com.lightspeedeps.object.TriStateCheckboxExt.CHECK_OFF;
//import com.icesoft.faces.facelets.component.tristatecheckbox.TriStateCheckbox;
import static com.lightspeedeps.object.TriStateCheckboxExt.CHECK_ON;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.ProjectMember;
import com.lightspeedeps.object.ControlHolder;
import com.lightspeedeps.object.TriStateCheckboxExt;
import com.lightspeedeps.type.Permission;
import com.lightspeedeps.type.PermissionCluster;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the "Permissions" page. See doc for {@link #redisplay()}
 * regarding scope issues. Also see {@link #getLoadPage()} regarding how we
 * force a re-instantiation of this bean upon entry to the Permissions page. As
 * a result of these methods, the code mostly acts like a ViewScoped bean.
 * <p>
 * This bean basically generates a two-dimensional array of checkboxes, in one
 * of three different styles. In all styles, the data is internally stored as
 * follows:
 * <ul>
 * <li>A List of PermRow`s - one PermRow represents the data for one
 * PermissionCluster and contains two PermRowPart`s, one for the View
 * permissions, and one for the Edit permissions.
 * <li>PermRowPart - This represents either an Edit or View "half" of a cluster
 * row, maintains the expand/collapse status, and contains multiple PermCluster
 * objects, one for each column that is currently displayed.
 * <li>PermCluster - holds the check-box settings for one "cell" in the display
 * -- the group of either View or Edit permission settings for one Department or
 * one person.
 * <li>PermSetting - represents one checkbox on the display.
 * </ul>
 * Each checkbox represent either a specific Permission, or a "rolled-up"
 * (summarized) value within a PermCluster.
 * <p>
 * The three styles mentioned above are (1) all roles for one person, (2) all
 * the users within one department, and (3) all the departments.  In the third
 * style, each column "summarizes" the permissions for everyone in a department.
 */
@ManagedBean
@SessionScoped
public class PermissionBean extends View implements ControlHolder, PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 8336621710107599255L;

	private static final Log log = LogFactory.getLog(PermissionBean.class);

	private static final int MAX_COL_VIEW = 10;


	/** True iff the current user has the LS Admin role. */
	private final boolean isAdmin;

	/** Include the Financial Permissions cluster in the display. */
	private final boolean includeFinance;

	/** Include the Onboarding Permissions cluster in the display. */
	private boolean includeOnboarding;

	/** Include all the clusters (not counting the Financial Permissions) in the display. */
	private final boolean includeNonFinance;

	/** Include scheduling Permission clusters. Used to remove clusters for Production Types such as tours */
	private final boolean includeScheduling;

	/** True iff this is the current production has On-boarding enabled. */
	private final boolean isOnboarding;

	/** The currently selected Contact for the "person" display style. */
	private Contact contact;

	/** The currently selected Contact for the "person" display style.
	 * This value is bound to the drop-down control for the contactDL List. */
	private Integer contactId;

	/** The currently selected Department for the "people-in-dept" style. */
	private Department department;

	/** The currently selected Department for the "people-in-dept" style.
	 * This value is bound to the drop-down control for the deptDL List. */
	private Integer departmentId;

	/** List of SelectItem`s of all the Department`s that the current contact is
	 * authorized to view; used when displaying the permissions for all members
	 * of a department. The value is Department.id and the label is
	 * Department.name. */
	private List<SelectItem> deptDL = new ArrayList<>();

	/** List of SelectItem`s containing Contact`s in the current production; used
	 * when displaying the permissions for a specific individual. This will be
	 * all contacts, except that LS Admins will be excluded if the current user
	 * is not an LS Admin. */
	private List<SelectItem> contactDL = new ArrayList<>();

	/** The List of all {@link PermRow} objects for the entire page. The contents
	 * of this List is constructed once during bean instantiation by {@link #createClusterHeadings()}. It does not
	 * depend on the display style. */
	private List<PermRow> rows = new ArrayList<>();

	/** The List of all {@link PermRow} objects which are NOT displayed. The
	 * contents of this List is constructed once during bean instantiation by
	 * {@link #createClusterHeadings()}. It does not depend on the display
	 * style. These clusters are maintained so that their corresponding
	 * permission mask bits are preserved when a Save is done. */
	private List<PermRow> hiddenRows = new ArrayList<>();

	/** A List of {@link Column} objects which contains most of the data
	 * displayed on the page, other than the left-hand row descriptions and +/-
	 * buttons. The contents of this List will change based on the display style
	 * and, possibly, the selected person or Department to be displayed. */
	private List<Column> columns = new ArrayList<>();

	/** The number of data columns in the current display; this does NOT
	 * include the left-most "permission names" column.   Used by JSP
	 * for loop control. */
	private int colCount;

	/** The current value of the horizontal column slider (scroll bar). */
	private int sliderValue;

	/** The PermissionCluster.id value of the row whose "expand" button was clicked. */
	private int expandId = -1;

	/** This is bound to the radio buttons selecting to view "People" or
	 * "Departments". */
	private int viewStyle = VIEW_STYLE_ONE_PERSON;
	/** View all the Role`s for one person. */
	private static final int VIEW_STYLE_ONE_PERSON = 0;
	/** View all people in one Department; a person with more than one Role in
	 * the Department will have a column for each of their distinct Role`s. */
	private static final int VIEW_STYLE_DEPT_MBRS = 1;
	/** View all Department`s -- each column displays a summary of the permissions
	 * for all existing Contact`s who have a Role within that Department. */
	private static final int VIEW_STYLE_ALL_DEPTS = 2;

	/** A Map from Department.id value to the set of permissions for that
	 * Department. The values are based on the ProjectMember permission masks in
	 * the database for the Contact`s in the current Production. Each entry is
	 * an array of 64 bytes, corresponding to the 64 possible permissions; each
	 * byte has the calculated checkbox value for the permission in that
	 * department.  These values are created once during bean instantiation, and
	 * are recalculated only when the user does a Save. */
	Map<Integer,byte[]> deptMasks = new HashMap<>();

	private final List<String> impliedPerms = new ArrayList<>();

	public PermissionBean() {
		super("Permission.");
		log.debug("");
		setScrollable(true);

		AuthorizationBean authBean = AuthorizationBean.getInstance();

		isAdmin = (authBean.isAdmin() || authBean.isLsCoord());
		includeFinance = authBean.hasPermission(getvContact(), SessionUtils.getCurrentProject(),
				Permission.EDIT_FINANCE_PERMISSIONS);
		includeNonFinance = authBean.hasPermission(getvContact(), SessionUtils.getCurrentProject(),
				Permission.VIEW_PRODUCTION_PREFERENCES);

		// Set the includeScheduling flag base on ProductionType.
		Production prod = SessionUtils.getProduction();

		if(prod.getType().isTours() || (! prod.getShowScriptTabs())) {
			includeScheduling = false;
		}
		else {
			includeScheduling = true;
		}

		includeOnboarding = includeFinance;
		isOnboarding = SessionUtils.getProduction().getAllowOnboarding();
		if (! isOnboarding) {
			includeOnboarding = false;
		}

		createDeptMasks(); // create department summary masks

		createUserDL(); // People (Contact) drop-down list
		createDeptDL(); // Department drop-down list

		createClusterHeadings(); // leftmost column - cluster & permission names

		// Init our department fields, in case user chooses Dept display style
		if (deptDL.size() > 0) {
			departmentId = (Integer)deptDL.get(0).getValue();
			department = DepartmentDAO.getInstance().findById(departmentId);
		}
		else {
			departmentId = 0;
		}

		// Ditto for contact fields
		contact = SessionUtils.getCurrentContact();
		contactId = contact.getId();

		createGrid();	// fill in the page using initial view settings
		setSliderValue(getSliderMin());
	}

	/**
	 * Action method for the "Edit" button.
	 * @see com.lightspeedeps.web.view.View#actionEdit()
	 */
	@Override
	public String actionEdit() {
		changeDisabled(false);
		return super.actionEdit();
	}

	/**
	 * Action method for the "Cancel" button.
	 * @see com.lightspeedeps.web.view.View#actionCancel()
	 */
	@Override
	public String actionCancel() {
		editMode = false; // set View mode, so createGrid makes the check-boxes disabled
		// the user may have checked/unchecked boxes, so we need to rebuild the grid
		createGrid();
		return super.actionCancel();
	}

	/**
	 * Action method for the "Save" button.
	 * @see com.lightspeedeps.web.view.View#actionSave()
	 */
	@Override
	public String actionSave() {
		try {
			save(false); // attempt save but prompt if approver(s) will lose View_HTG permission
		}
		catch (Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for one of the "+" or "-" buttons, which expands or
	 * collapses the list of Edit permissions. The identifier has been set via a
	 * f:setPropertyActionListener tag, and should match a PermissionCluster.id
	 * value (defined in the PermissionCluster enumeration). Whether to expand
	 * or collapse is simply a matter of toggling the current state.
	 *
	 * @return null navigation string
	 */
	public String actionExpandEdit() {
		if (expandId >= 0) {
			for (PermRow row : rows) {
				if (row.cluster.getId() == expandId) {
					row.parts[1].expand = ! row.parts[1].expand;
				}
			}
			expandId = -1;
		}
		return null;
	}

	/**
	 * Action method for one of the "+" or "-" buttons, which expands or
	 * collapses the list of View permissions. The identifier has been set via a
	 * f:setPropertyActionListener tag, and should match a PermissionCluster.id
	 * value (defined in the PermissionCluster enumeration). Whether to expand
	 * or collapse is simply a matter of toggling the current state.
	 *
	 * @return null navigation string
	 */
	public String actionExpandView() {
		if (expandId >= 0) {
			for (PermRow row : rows) {
				if (row.cluster.getId() == expandId) {
					row.parts[0].expand = ! row.parts[0].expand;
				}
			}
			expandId = -1;
		}
		return null;
	}

	/**
	 * valueChangeListener for the radio buttons that select the type of data
	 * being presented.
	 *
	 * @param event The ValueChangeEvent created by the framework.
	 */
	public void listenViewStyle(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (event.getNewValue() != null) {
				viewStyle = (Integer)event.getNewValue();
				createGrid();
				resetSliderAndData();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
	}

	/**
	 * valueChangeListener for the drop-down list of user names (Contact`s)
	 * available to choose from when the "One person" radio button is selected.
	 *
	 * @param event The ValueChangeEvent created by the framework. The newValue
	 *            field will be the Contact.id of the Contact to be displayed.
	 */
	public void listenContactDL(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (event.getNewValue() != null) { // got null value once - how?
				Integer id = Integer.parseInt(event.getNewValue().toString());
				log.debug("id=" + id);
				Contact c = ContactDAO.getInstance().findById(id);
				if (c != null) {
					contact = c;
					contactId = id;
					viewStyle = VIEW_STYLE_ONE_PERSON;
					createGrid();
					resetSliderAndData();
				}
			}
			else {
				log.warn("null event value? - evt=" + event);
			}
		}
		catch (Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}
	}

	/**
	 * valueChangeListener for the drop-down list of Department names available
	 * to choose from when the "One department" radio button is selected.
	 *
	 * @param event The ValueChangeEvent created by the framework. The newValue
	 *            field will be the Department.id of the Department to be
	 *            displayed.
	 */
	public void listenDeptDL(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			Integer id = (Integer)event.getNewValue();
			if (id != null && event.getOldValue() != null) {
				Department dept = DepartmentDAO.getInstance().findById(id);
				if (dept != null) {
					departmentId = id;
					department = dept;
					viewStyle = VIEW_STYLE_DEPT_MBRS;
					createGrid();
					resetSliderAndData();
				}
			}
		}
		catch (Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}
	}

	public void listenSlideEvent(ValueChangeEvent event) {
		event.getSource();

		int currentValue =  (int)event.getNewValue();

		if(sliderValue != currentValue) {
			sliderValue = currentValue;
			redisplay();
		}
	}

	/**
	 * Called when a tri-state check box is clicked. Depending on which box is
	 * clicked and the display style, we may have to propagate the setting down
	 * a column or across a row, or re-compute the "summary" box corresponding
	 * to the clicked box.
	 *
	 * @param event The event generated by the framework. Not used.
	 * @param id The id we set in the checkbox. This will be the direct holder
	 *            of the checkbox -- either a PermSetting or a PermCluster.
	 */
	@Override
	public void clicked(TriStateCheckboxExt checkBox, Object id) {
		log.debug("");
		try {
			PermSetting ps = null;
			PermCluster pc = null;
			byte chkValue;
			if (id instanceof PermCluster) { // cluster (roll-up) box changed
				pc = (PermCluster)id;
				if (pc.checkbox.isPartiallyChecked()) {
					pc.checkbox.setCheckValue(CHECK_ON);
				}
				chkValue = pc.checkbox.getCheckValue();
			}
			else { // individual (or possibly summary-across) box changed
				ps = (PermSetting)id;
				pc = ps.cluster;
				chkValue = ps.checkbox.getCheckValue();
			}
			impliedPerms.clear();

			clicked(pc, ps); // apply to all related boxes, including implied permissions

			if (impliedPerms.size() > 0) {
				if (impliedPerms.size() == 1) {
					if (chkValue == CHECK_ON) {
						MsgUtils.addFacesMessage("Permission.AddedOne", FacesMessage.SEVERITY_INFO, impliedPerms.get(0));
					}
					else {
						MsgUtils.addFacesMessage("Permission.RemovedOne", FacesMessage.SEVERITY_INFO, impliedPerms.get(0));
					}
				}
				else {
					if (chkValue == CHECK_ON) {
						MsgUtils.addFacesMessage("Permission.Added", FacesMessage.SEVERITY_INFO, impliedPerms.size());
					}
					else {
						MsgUtils.addFacesMessage("Permission.Removed", FacesMessage.SEVERITY_INFO, impliedPerms.size());
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
	}

	/**
	 * Called when a tri-state check box is clicked. Depending on which box is
	 * clicked and the display style, we may have to propagate the setting down
	 * a column or across a row, or re-compute the "summary" box corresponding
	 * to the clicked box.
	 *
	 * @param pc The PermCluster that either has the summary checkbox that was
	 *            clicked, or that contains the PermSetting (individual
	 *            Permission row) that was clicked.
	 * @param ps The PermSetting whose checkbox was clicked, or null if a
	 *            PermCluster checkbox was clicked.
	 */
	private void clicked(PermCluster pc, PermSetting ps) {
		pc.changed = true;
		if (viewStyle == VIEW_STYLE_ALL_DEPTS) {
			if (ps == null) { // view or edit summary box clicked
				rollDownValue(pc);
			}
			else { // ps != null: specific permission box clicked
				if (ps.checkbox.isPartiallyChecked()) {
					// these may not be clicked-to-mix
					ps.checkbox.setCheckValue(CHECK_ON);
				}
				checkImplied(pc, ps);
				rollUpSummary(pc);
			}
		}
		else if (viewStyle == VIEW_STYLE_ONE_PERSON) {
			if (ps == null) { // cluster value was changed - propagate down
				rollDownValue(pc);
			}
			else { // individual value was changed,
				checkImplied(pc, ps);
				rollUpSummary(pc); // so update View/Edit summary value
			}
		}
		else { // VIEW_STYLE_DEPT_MBRS (all people in one department)
			if (ps == null) { // view or edit summary box clicked
				// apply down the column, and also check if across required
				rollDownValue(pc);
				for (PermSetting setting : pc.getSettings()) {
					if (setting.rowSummary) { // left-most (dept summary) column was clicked
						rollRightValue(pc, setting);
					}
					else { // one person's column was clicked,
						rollLeftSummary(pc, setting); // so update dept summary
					}
				}
			}
			else { // ps != null -- specific permission box clicked
				if (ps.rowSummary) { // dept summary clicked - apply to all items in row
					if (ps.checkbox.isPartiallyChecked()) {
						// these may not be clicked-to-mix
						ps.checkbox.setCheckValue(CHECK_ON);
					}
					rollRightValue(pc,ps);
				}
				else { // one person's column was clicked,
					checkImplied(pc, ps);
					rollLeftSummary(pc, ps); // so update dept summary
				}
				rollUpSummary(pc); // Update View/Edit summary checkbox
			}
		}
	}

	/**
	 * Propagate the checkbox value setting in the given PermCluster
	 * down through all the PermSettings included within it.
	 * @param pc
	 */
	private void rollDownValue(PermCluster pc) {
		byte value = pc.checkbox.getCheckValue();
		for (PermSetting ps : pc.getSettings()) {
			if (value != ps.checkbox.getCheckValue()) {
				ps.checkbox.setCheckValue(value);
				if (! ps.rowSummary) {
					checkImplied(pc, ps);
					// don't do implied check for row summaries (dept),
					// it will get handled by rollRightValue.
				}
			}
		}
	}

	/**
	 * For the people-in-dept view - propagate a checkbox setting from the
	 * department summary column across all the personal columns. Given the
	 * PermCluster object, we know which "half row" of clusters to scan; within
	 * those clusters, we just match on the individual PermSetting mask to know
	 * which PermSetting checkboxes to update.
	 *
	 * @param sumCluster The PermCluster holding the PermSetting that changed. This
	 *            must be a department summary cluster -- the left-most cluster
	 *            in the people-in-dept view.
	 * @param sumPs The PermSetting that changed, whose checkbox value is to
	 *            be propagated across the same permission line for all people
	 *            in the department.
	 */
	private void rollRightValue(PermCluster sumCluster, PermSetting sumPs) {
		byte chkValue = sumPs.checkbox.getCheckValue();
		PermRow pr = rows.get(sumCluster.rowNumber);
		List<PermCluster> clusters = pr.parts[sumCluster.partNumber].clusters;
		for (int psNum = 0; psNum < sumCluster.settings.size(); psNum++) {
			PermSetting ps = sumCluster.settings.get(psNum);
			if (ps.mask == sumPs.mask) {
				// now we have the 'detail row' corresponding to the PermSetting given.
				for (int nc = 1; nc < clusters.size(); nc++) {
					// Propagate the given value across all the clusters.
					PermCluster pc = clusters.get(nc);
					PermSetting onePs = pc.settings.get(psNum);
					if (chkValue != onePs.checkbox.getCheckValue()) {
						pc.changed = true;
						onePs.checkbox.setCheckValue(chkValue);
						checkImplied(pc, onePs);
						rollUpSummary(pc); // recompute cluster summary value
					}
				}
				break; // once we process the matching PermSetting row, we're done.
			}
		}
	}

	/**
	 * Loop through the PermSettings within the given PermCluster and roll up
	 * the individual check settings into the cluster summary checkbox.
	 *
	 * @param pc The PermCluster whose summary value needs to be computed.
	 */
	private void rollUpSummary(PermCluster pc) {
		byte rollUp = CHECK_NA;
		for (PermSetting ps : pc.getSettings()) {
			if (rollUp == CHECK_NA) { // first entry
				rollUp = ps.checkbox.getCheckValue();
			}
			else if (rollUp != ps.checkbox.getCheckValue()) {
				rollUp = CHECK_MIXED;
				break;
			}
		}
		pc.checkbox.setCheckValue(rollUp);
	}

	/**
	 * For the people-in-dept view - a personal column changed, so recompute
	 * the department summary column, and the View/Edit summary of the department column.
	 * Given the PermCluster object, we know which "half row" (PermRowPart) of clusters
	 * to scan; within those clusters, we just match on the individual
	 * PermSetting mask to know which PermSetting detail row summarize.
	 *
	 * @param cluster The PermCluster holding the PermSetting that changed.
	 * @param setting The PermSetting that changed, whose checkbox value is to
	 *            be propagated across the same permission line for all people
	 *            in the department.
	 */
	private void rollLeftSummary(PermCluster cluster, PermSetting setting) {
		PermRow pr = rows.get(cluster.rowNumber);
		List<PermCluster> clusters = pr.parts[cluster.partNumber].clusters;
		PermCluster summaryPc = clusters.get(0); // holds the summary for the department
		byte across = setting.checkbox.getCheckValue();
		for (int psNum = 0; psNum < summaryPc.settings.size(); psNum++) {
			PermSetting ps = summaryPc.settings.get(psNum);
			if (ps.mask == setting.mask) {
				// now we have the 'detail row' corresponding to the PermSetting given.
				for (int nc = 1; nc < clusters.size(); nc++) {
					// Summarize the detail row across all the clusters.
					PermCluster pc = clusters.get(nc);
					PermSetting onePs = pc.settings.get(psNum);
					if (across != onePs.checkbox.getCheckValue()) {
						across = CHECK_MIXED;
						break; // once it's MIXED, no need to continue across
					}
				}
				ps.checkbox.setCheckValue(across);
				break; // once we process the matching PermSetting, we're done.
			}
		}
		// roll up the summary's detail rows into the View or Edit summary checkbox
		rollUpSummary(summaryPc);
	}

	/**
	 * The ps value changed - check implies/impliedBy permissions
	 * @param checkedPc
	 * @param checkedPs
	 */
	private void checkImplied(PermCluster checkedPc, PermSetting checkedPs) {
		byte chkValue = checkedPs.checkbox.getCheckValue();
		Permission perm = Permission.findByMask(checkedPs.mask);
		int colNum = checkedPc.columnNumber;
		long implyMask;
		if (checkedPs.checkbox.getCheckValue() == CHECK_ON) {
			implyMask = perm.getImpliesMask();
		}
		else {
			implyMask = perm.getImpliedByMask();
		}
		for (PermRow pr : rows) {
			for (int n = 0; n < 2; n++) {
				PermCluster pc = pr.parts[n].clusters.get(colNum);
				if ((pc.mask & implyMask) != 0) { // cluster contains an implied/implies perm.
					for (PermSetting ps : pc.settings) {
						if ((ps.mask & implyMask) != 0) { // this is an implied/implying perm.
							if (ps.checkbox.getCheckValue() != chkValue) {
								pc.changed = true;
								ps.checkbox.setCheckValue(chkValue);
								impliedPerms.add(Permission.findByMask(ps.mask).getName());
								if (viewStyle == VIEW_STYLE_DEPT_MBRS) {
									rollLeftSummary(pc, ps);
								}
								rollUpSummary(pc);
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Based on the ProjectMember permission masks in the database for the
	 * current Production, calculate the proper yes/no/mixed settings for all
	 * permissions across all departments. Each department will have an array of
	 * 64 bytes, corresponding to the 64 possible permissions; each byte will
	 * have the calculated checkbox value for the permission in that department.
	 * The arrays are stored in the 'deptMasks' Map field, keyed by department id.
	 */
	private void createDeptMasks() {
		List<ProjectMember> allPms = ProjectMemberDAO.getInstance()
				.findByProduction(SessionUtils.getProduction());
		List<Department> depts = DepartmentUtils.getDepartmentList();

		for (Department d : depts) {
			boolean foundRole = false;
			long maskAnd = -1; // all bits on
			long maskOr = 0; // all bits off
			int dpId = d.getId();
			for (ProjectMember pm : allPms) {
				if (pm.getEmployment().getDepartmentId() == null) { // transient - need to set it.
					pm.getEmployment().setDepartmentId(pm.getEmployment().getRole().getDepartment().getId());
				}
				if (dpId == pm.getEmployment().getDepartmentId().intValue()) {
					maskAnd &= pm.getEmployment().getPermissionMask();
					maskOr  |= pm.getEmployment().getPermissionMask();
					foundRole = true;
				}
			}
			if (! foundRole) {
				maskAnd = 0;
			}
			byte[] values = new byte[Permission.NUM_PERM_BITS+1];
			long mask = 1; // used to isolate & test each of the 64 permission bits in turn
			for (int i = 1; i <= Permission.NUM_PERM_BITS; i++) {
				if (! foundRole) {
					values[i] = CHECK_NA;
				}
				else if ((maskAnd & mask) != 0) {
					values[i] = CHECK_ON;
				}
				else if ((maskOr & mask) != 0) {
					values[i] = CHECK_MIXED;
				}
				else {
					values[i] = CHECK_OFF;
				}
				mask <<= 1;
			}
			deptMasks.put(dpId, values);
		}
	}

	/**
	 * Fill the {@link #rows} List with the allowed (displayed) set of PermRow
	 * objects, and the {@link #hiddenRows} List with the PermRow objects that
	 * will not be displayed.
	 * <p>
	 * Create the PermRow objects with the appropriate descriptive information
	 * to be displayed in the left-hand column of the Permissions page. This
	 * only needs to be done once -- it is the same for all display styles.
	 */
	private void createClusterHeadings() {
		for (PermissionCluster cluster : PermissionCluster.values()) {
			if (cluster == PermissionCluster.N_A) {
				break; // Skip the N/A entry and any entries after it
			}
			boolean display = false;
			// Filter out permission clusters based on includeScheduling flag
			if (! includeScheduling && (cluster == PermissionCluster.CALL_SHEETS_PRS ||
					cluster == PermissionCluster.CAST_TIMECARD ||
					cluster == PermissionCluster.PRODUCTION_ELEMENTS ||
					cluster == PermissionCluster.SCHEDULE ||
					cluster == PermissionCluster.SCRIPT
					)) {
				// display remains false
			}
			else if ((cluster == PermissionCluster.FINANCIAL_PERMISSIONS && includeFinance) ||
					(cluster == PermissionCluster.ONBOARDING && includeOnboarding) ||
					(cluster != PermissionCluster.FINANCIAL_PERMISSIONS &&
						cluster != PermissionCluster.ONBOARDING && includeNonFinance)) {
				display = true;
			}
			PermRow row = new PermRow();
			row.setCluster(cluster);
			// Each row (PermissionCluster) contains 2 row parts (view & edit)
			// Loop to get permission names for this cluster (same for all view styles)
			long mask = 1;
			for (int i = 1; i <= Permission.NUM_PERM_BITS; i++) {
				Permission perm = Permission.findById(i);
				if (displayPerm(perm)) {
					if ((cluster.getViewMask() & mask) != 0) {
						row.parts[0].names.add(perm.getName());
					}
					else if ((cluster.getEditMask() & mask) != 0) {
						row.parts[1].names.add(perm.getName());
					}
				}
				mask <<= 1;
			}
			if (display) {
				rows.add(row);
			}
			else {
				hiddenRows.add(row);
			}
		}
	}

	/**
	 * Change the 'disabled' flag on ALL our checkboxes to either true or false.
	 *
	 * @param b The new 'disabled' setting to be applied.
	 */
	private void changeDisabled(boolean b) {
		for (PermRow row : rows) {
			for (int partn = 0; partn < 2; partn++) {
				for (PermCluster pc : row.parts[partn].clusters) {
					if (pc.checkbox.getCheckValue() != CHECK_NA) {
						pc.checkbox.setDisabled(b);
						for (PermSetting ps : pc.settings) {
							ps.checkbox.setDisabled(b);
						}
					}
				}
			}
		}
	}

	/**
	 * Fill in all the data for the permission table display except for the
	 * left-most column, which was constructed earlier. On entry, the 'rows'
	 * field has all the necessary PermRow entries.
	 * <p>
	 * The table data will be generated based on the viewStyle and peopleStyle
	 * settings.
	 */
	private void createGrid() {
		List<Department> depts = DepartmentUtils.getDepartmentList();
		boolean disabled = ! editMode;
		columns.clear();
		List<ProjectMember> deptPeople = null;
		if (viewStyle == VIEW_STYLE_DEPT_MBRS) {
			// Need list of distinct contact+role entries for this display
			List<ProjectMember> pms = ProjectMemberDAO.getInstance().findByProductionAndDepartment(department);
			deptPeople = new ArrayList<>();
			Set<String> dupes = new HashSet<>();
			for (ProjectMember pm : pms) {
				String key = pm.getEmployment().getContact().getId() + "," + pm.getEmployment().getRole().getId();
				if (! dupes.contains(key)) {
					dupes.add(key);
					deptPeople.add(pm);
				}
			}
			if (department.getId() == Constants.DEPARTMENT_ID_LS_ADMIN ||
					department.getId() == Constants.DEPARTMENT_ID_DATA_ADMIN) {
				disabled = true;
			}
		}
		else if (viewStyle == VIEW_STYLE_ONE_PERSON) { // refresh Contact to avoid LazyInitExc.
			contact = ContactDAO.getInstance().refresh(contact);
		}

		// Loop through displayed rows generating data for each cluster
		createClusters(rows, depts, deptPeople, disabled, true);

		// Loop through hidden rows generating data for each cluster
		createClusters(hiddenRows, depts, deptPeople, disabled, false);

		colCount = columns.size();
	}

	/**
	 * Create the cluster information for each of the PermRow's passed.
	 *
	 * @param permRows The List of PermRow's being processed, which will either
	 *            be the displayed set or hidden set.
	 * @param depts The list of departments visible to the current user in this
	 *            production.
	 * @param deptPeople For the "members of a dept" style view, this list of
	 *            ProjectMember's to display (one column per person).
	 * @param disabled If the default setting of the check-boxes is disabled or
	 *            not (true = disabled); this parameter only applies to
	 *            VIEW_STYLE_DEPT_MBRS.
	 * @param firstRow True if we're being called with the list that includes
	 *            the first displayed row of data.
	 */
	private void createClusters(List<PermRow> permRows, List<Department> depts, List<ProjectMember> deptPeople,
			boolean disabled, boolean firstRow) {
		Column col;
		for (int rowNum = 0; rowNum < permRows.size(); rowNum++) {
			PermRow row = permRows.get(rowNum);
			PermissionCluster cluster = row.cluster;
			if (cluster.getName().equals("N/A")) { // ignore - don't display
				continue;
			}
			// Each row (PermissionCluster) contains 2 row parts (view & edit).
			// Clear existing data, which will get recreated
			row.parts[0].clusters.clear();
			row.parts[1].clusters.clear();
			if (row.cluster.getViewMask() == 0) {
				row.parts[0].hide = true;
			}
			if (row.cluster.getEditMask() == 0) {
				row.parts[1].hide = true;
			}
			int colNumber = 0;
			PermCluster pc;
			if (viewStyle == VIEW_STYLE_ALL_DEPTS) {
				for (Department dept : depts) {
					if ( ! isAdmin && dept.getId() == Constants.DEPARTMENT_ID_LS_ADMIN) {
						// if not an LS Admin, omit the "LS Admin" department from
						// the display of "all departments".
						continue;
					}
					if (firstRow) {
						col = new Column();
						col.setHeading1(dept.getName());
						col.departmentId = dept.getId();
						columns.add(col);
					}
					byte[] vals = deptMasks.get(dept.getId());
					disabled = ! editMode;
					if (dept.getId() == Constants.DEPARTMENT_ID_LS_ADMIN ||
							dept.getId() == Constants.DEPARTMENT_ID_DATA_ADMIN) {
						disabled = true;
					}
					// for each Dept (column) generate a PermCluster entry in each rowPart
					// First the View rowPart
					pc = createDeptPermCluster(cluster.getViewMask(), vals, disabled);
					pc.setPosition(rowNum, 0, colNumber);
					pc.columnNumber = colNumber;
					row.parts[0].clusters.add(pc);
					// now do Edit rowPart
					pc = createDeptPermCluster(cluster.getEditMask(), vals, disabled);
					pc.setPosition(rowNum, 1, colNumber);
					row.parts[1].clusters.add(pc);
					colNumber++;
				}
			}
			else if (viewStyle == VIEW_STYLE_ONE_PERSON) { // all roles for one person (Contact)
				if (contact == null) { // shouldn't happen
					contact = SessionUtils.getCurrentContact();
				}
				Set<Integer> roles = new HashSet<>();
				for (Employment emp : contact.getEmployments()) {
					if (! roles.contains(emp.getRole().getId())) { // skip duplicate Role`s
						roles.add(emp.getRole().getId());
						if (firstRow) {
							col = new Column();
							col.setHeading1(emp.getRole().getName());
							col.roleId = emp.getRole().getId();
							columns.add(col);
						}
						if (emp.getDepartmentId() == null) { // transient - need to set it.
							emp.setDepartmentId(emp.getRole().getDepartment().getId());
						}
						disabled = ! editMode;
						if (emp.getDepartmentId() == Constants.DEPARTMENT_ID_LS_ADMIN ||
								emp.getDepartmentId() == Constants.DEPARTMENT_ID_DATA_ADMIN) {
							disabled = true;
						}
						pc = createPersonPermCluster(cluster.getViewMask(), emp.getPermissionMask(), disabled);
						pc.setPosition(rowNum, 0, colNumber);
						row.parts[0].clusters.add(pc);
						pc = createPersonPermCluster(cluster.getEditMask(), emp.getPermissionMask(), disabled);
						pc.setPosition(rowNum, 1, colNumber);
						row.parts[1].clusters.add(pc);
						colNumber++;
					}
				}
			}
			else { // VIEW_STYLE_DEPT_MBRS - all users in one department
				if (firstRow) {
					col = new Column();
					col.setHeading1("<b>Dept:</b>");
					col.setHeading2(department.getName());
					col.departmentId = department.getId();
					columns.add(col);
				}
				pc = new PermCluster(cluster.getViewMask());
				pc.checkbox.setHolder(this);
				pc.setPosition(rowNum, 0, 0);
				row.parts[0].clusters.add(pc);
				pc = new PermCluster(cluster.getEditMask());
				pc.checkbox.setHolder(this);
				pc.setPosition(rowNum, 1, 0);
				row.parts[1].clusters.add(pc);
				if (deptPeople != null) {
					for (ProjectMember pm : deptPeople) {
						colNumber++;
						if (firstRow) {
							col = new Column();
							col.setHeading1(pm.getEmployment().getContact().getUser().getLastNameFirstName());
							col.setHeading2(pm.getEmployment().getRole().getName());
							col.contactId = pm.getEmployment().getContact().getId();
							col.roleId = pm.getEmployment().getRole().getId();
							columns.add(col);
						}
						pc = createPersonPermCluster(cluster.getViewMask(), pm.getEmployment().getPermissionMask(), disabled);
						pc.setPosition(rowNum, 0, colNumber);
						row.parts[0].clusters.add(pc);
						pc = createPersonPermCluster(cluster.getEditMask(), pm.getEmployment().getPermissionMask(), disabled);
						pc.setPosition(rowNum, 1, colNumber);
						row.parts[1].clusters.add(pc);
					}
				}
				createPermSummary(row.parts[0].clusters);
				createPermSummary(row.parts[1].clusters);
			}

			firstRow = false;
		}
	}

	/**
	 * Create one PermCluster object for the "all departments" view. This
	 * corresponds to "half" a row -- either all the View or all the Edit
	 * permission settings within a cluster row.
	 *
	 * @param clusterMask The mask consisting of the OR'ing of all the masks of
	 *            Permissions that appear within this half-cluster.
	 * @param vals The checkbox values of all Permissions.
	 * @param disabled If true, the checkboxes will all be disabled, e.g., for
	 *            View mode.
	 * @return the new PermCluster instance.
	 */
	private PermCluster createDeptPermCluster(long clusterMask, byte[] vals, boolean disabled) {
		PermCluster pc = new PermCluster(clusterMask);
		pc.checkbox.setHolder(this);
		pc.checkbox.setDisabled(disabled);
		long maskBit = 1;
		if (vals[0] == CHECK_NA) {
//			pc.disabled = true;
		}
		byte rollUp = CHECK_NA;
		for (int i = 1; i <= Permission.NUM_PERM_BITS; i++) {
			if ((clusterMask & maskBit) != 0) {
				Permission perm = Permission.findById(i);
				if (displayPerm(perm)) {
					PermSetting ps = new PermSetting(pc, vals[i]);
					ps.checkbox.setHolder(this);
					ps.checkbox.setDisabled(disabled);
					ps.mask = perm.getMask();
					pc.settings.add(ps);
					if (rollUp == CHECK_NA) {
						rollUp = ps.checkbox.getCheckValue();
					}
					else if (rollUp != ps.checkbox.getCheckValue()) {
						rollUp = CHECK_MIXED;
					}
				}
			}
			maskBit <<= 1;
		}
		pc.checkbox.setCheckValue(rollUp);
		return pc;
	}

	/**
	 * Create one PermCluster object for the "one person" view. This corresponds
	 * to "half" a row -- either all the View or all the Edit permission
	 * settings within a cluster row.
	 *
	 * @param clusterMask The mask consisting of the OR'ing of all the masks of
	 *            Permissions that appear within this half-cluster.
	 * @param roleMask The role mask of the currently selected person; this will
	 *            determine which checkboxes are checked.
	 * @param disabled If true, the checkboxes will all be disabled, e.g., for
	 *            View mode.
	 * @return the new PermCluster instance.
	 */
	private PermCluster createPersonPermCluster(long clusterMask, long roleMask, boolean disabled) {
		PermCluster pc = new PermCluster(clusterMask);
		pc.checkbox.setHolder(this);
		pc.checkbox.setDisabled(disabled);
		long maskBit = 1;
		byte rollUp = CHECK_NA;
		for (int i = 1; i <= Permission.NUM_PERM_BITS; i++) {
			if ((clusterMask & maskBit) != 0) {
				Permission perm = Permission.findById(i);
				if (displayPerm(perm)) {
					PermSetting ps = new PermSetting( pc,
							((maskBit & roleMask) != 0) ? CHECK_ON : CHECK_OFF);
					ps.checkbox.setHolder(this);
					ps.checkbox.setDisabled(disabled);
					ps.mask = perm.getMask();
					pc.settings.add(ps);
					if (rollUp == CHECK_NA) {
						rollUp = ps.checkbox.getCheckValue();
					}
					else if (rollUp != ps.checkbox.getCheckValue()) {
						rollUp = CHECK_MIXED;
					}
				}
			}
			maskBit <<= 1;
		}
		pc.checkbox.setCheckValue(rollUp);
		return pc;
	}

	private void createPermSummary(List<PermCluster> clusters) {
		PermCluster summary = clusters.get(0);
		long maskBit = 1;
		byte rollUp = CHECK_NA; // View or Edit summary
		int psNum = 0; // permSetting counter
		for (int i = 1; i <= Permission.NUM_PERM_BITS; i++) {
			if ((summary.mask & maskBit) != 0) {
				Permission perm = Permission.findById(i);
				if (displayPerm(perm)) {
					PermSetting ps = new PermSetting(summary, CHECK_NA);
					ps.checkbox.setHolder(this);
					ps.mask = perm.getMask();
					ps.rowSummary = true;
					summary.settings.add(ps);
					byte across = CHECK_NA;
					for (int nc = 1; nc < clusters.size(); nc++) {
						PermCluster pc = clusters.get(nc);
						PermSetting onePs = pc.settings.get(psNum);
						if (nc == 1) { // first entry
							across = onePs.checkbox.getCheckValue();
						}
						else if (across != onePs.checkbox.getCheckValue()) {
							across = CHECK_MIXED;
							break; // once it's MIXED, no need to continue across
						}
					}
					ps.checkbox.setCheckValue(across);
					if (rollUp == CHECK_NA) {
						rollUp = ps.checkbox.getCheckValue();
					}
					else if (rollUp != ps.checkbox.getCheckValue()) {
						rollUp = CHECK_MIXED;
					}
					psNum++;
				}
			}
			maskBit <<= 1;
		}
		summary.checkbox.setCheckValue(rollUp);
	}

	/**
	 * @param perm The Permission to be inspected.
	 * @return True if the given Permission (one row) should be displayed in the
	 *         matrix.
	 */
	private boolean displayPerm(Permission perm) {
		boolean ret = true;
		if (isOnboarding && perm == Permission.EDIT_START_FORMS) {
			ret = false; // ignore (do not display) this permission for onboarding
		}
//		else if ((! isAdmin) && perm == Permission.MANAGE_START_DOC_APPROVERS) {
//			ret = false; // Do not display this permission for non-LS Admin users
//		}
		return ret;
	}

	/**
	 * Called when the displayed style of data has changed, and we want to reset
	 * the slider to its minimum value, and force a refresh of the page.
	 */
	private void resetSliderAndData() {
		sliderValue = getSliderMin();
		redisplay();
	}

	/**
	 * Force a refresh of the page. This is necessary because of a bug in
	 * ICEfaces which causes data nested within multiple levels of UISeries
	 * structures to not be refreshed. In this case, we have ice:panelSeries
	 * components within ice:dataTables, and the displayed data would remain
	 * unchanged even though the underlying fields have changed.
	 * <p>
	 * To get around this, we have to clear the child components of most of the
	 * page. In ICEfaces 1.8 that was sufficient; but with the migration to
	 * ICEfaces 3.2 and JSF 2.1, that alone caused other errors, so we added the
	 * "forced navigation" back to the same page, which avoided those new
	 * errors. However, we don't want this navigation to re-instantiate the
	 * bean, which would happen for a ViewScoped bean, so we make this bean
	 * SessionScoped.
	 * <p>
	 * (The error most frequently encountered matched the symptoms of
	 * http://java.net/jira/browse/JAVASERVERFACES-2623, which was supposed to
	 * be fixed in JSF 2.1.18; however, the error still occurred with our code
	 * after installing the 2.1.18 jar.)
	 * <p>
	 * See also {@link #getLoadPage()} regarding scope issues.
	 */
	public void redisplay() {
		try {
//			PermissionRequestBean pb = PermissionRequestBean.getInstance();
//			if (pb.getPermTable() != null) {
//				pb.getPermTable().getChildren().clear();
//				pb.getPermTable().getHeader().getChildren().clear();
//			}
			log.debug("slider=" + sliderValue);
			HeaderViewBean.navigate(HeaderViewBean.PROJECT_MENU_PERMISSIONS);
		}
		catch (Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}
	}

	/**
	 * Save all changes to Permissions.
	 *
	 * @param promptOked True if the user was prompted already and said "Ok".
	 *            False if we have not checked approver status (and therefore
	 *            user has not been prompted).
	 */
	private void save(boolean promptOked) {
		log.debug("");
		boolean changed = false;

		for (int colnum = 0; colnum < columns.size(); colnum++) {
			Column col = columns.get(colnum);
			boolean colChanged = false;
			if (viewStyle != VIEW_STYLE_ALL_DEPTS) {
				if (colnum == 0	&& viewStyle == VIEW_STYLE_DEPT_MBRS) {
					continue; // ignore the first column in "people-in-dept" view
				}
				long colMask = Permission.ONLINE.getMask(); // everybody gets this!
				for (PermRow row : rows) {
					for (int partn = 0; partn < 2; partn++) {
						PermCluster pc = row.parts[partn].clusters.get(colnum);
						colChanged |= pc.changed;
						for (PermSetting ps : pc.settings) {
							if (ps.checkbox.isChecked()) {
								colMask |= ps.mask;
							}
						}
					}
				}
				if (colChanged) {
					changed = true;
					// Include existing permission bits for clusters that are not displayed. LS-1628
					for (PermRow row : hiddenRows) {
						for (int partn = 0; partn < 2; partn++) {
							PermCluster pc = row.parts[partn].clusters.get(colnum);
							for (PermSetting ps : pc.settings) {
								if (ps.checkbox.isChecked()) {
									colMask |= ps.mask;
								}
							}
						}
					}
					log.debug("col changed: #" + colnum);
					if (viewStyle == VIEW_STYLE_DEPT_MBRS) { // people-in-dept view
						// apply new mask to this column's Contact & Role
						saveContactMask(col.contactId, col.roleId, colMask);
					}
					else { // all roles of one person view
						// apply new mask to displayed/selected Contact and this column's Role
						saveContactMask(contact.getId(), col.roleId, colMask);
					}
				}
			}
			else { // View all departments: each column is one department
				// Determine which bits must be forced ON, and which ones forced OFF.
				// Boxes that are mixed are ignored - those bits won't be changed in individual permissions
				long onMask = 0, offMask = 0;
				for (PermRow row : rows) {
					for (int partn = 0; partn < 2; partn++) {
						PermCluster pc = row.parts[partn].clusters.get(colnum);
						if (pc.changed) {
							colChanged = true;
							for (PermSetting ps : pc.settings) {
								if (ps.checkbox.isChecked()) {
									onMask |= ps.mask;
								}
								else if (ps.checkbox.isUnchecked()) {
									offMask |= ps.mask;
								}
							}
						}
					}
				}
				if (colChanged) {
					changed = true;
					log.debug("col changed: #" + colnum);
					// apply mask changes to all projectMember/roles in this department
					saveDeptMask( col.departmentId, offMask, onMask);
				}
			}
		}

		if (changed) {
			createDeptMasks(); // These could change if user changed permissions; rebuild.
			for (PermRow row : rows) {
				for (int partn = 0; partn < 2; partn++) {
					for (PermCluster pc : row.parts[partn].clusters) {
						pc.changed = false;
					}
				}
			}
		}
		// Just in case we changed our own permissions, reload them:
		Contact current = SessionUtils.getCurrentContact();
		current = ContactDAO.getInstance().refresh(current);
		AuthorizationBean.getInstance().auth(current, true); // update role permissions
		changeDisabled(true); // return all checkboxes to disabled state
		super.actionSave();
	}

	/**
	 * Apply the given mask to all ProjectMember`s in the current Production
	 * whose Role falls within the given Department.
	 *
	 * @param deptId The database id of the Department of interest.
	 * @param offMask The permission mask of bits to be turned off.
	 * @param onMask The permission mask of bits to be turned on.
	 */
	private void saveDeptMask(Integer deptId, long offMask, long onMask) {
		Department dept = DepartmentDAO.getInstance().findById(deptId);
		log.debug("dept=" + dept.getName());
		log.debug(" ON mask: " + Permission.toBinaryString(onMask));
		log.debug("OFF mask: " + Permission.toBinaryString(offMask));
		offMask = ~ offMask;
		final ProjectMemberDAO projectMemberDAO = ProjectMemberDAO.getInstance();
		List<ProjectMember> pms = projectMemberDAO.findByProductionAndDepartment(dept);
		for (ProjectMember pm : pms) {
			log.debug("old mask: " + Permission.toBinaryString(pm.getEmployment().getPermissionMask()));
			long mask = pm.getEmployment().getPermissionMask() | onMask;
			mask &= offMask;
			log.debug("new mask: " + Permission.toBinaryString(mask));
			pm.getEmployment().setPermissionMask(mask);
			projectMemberDAO.attachDirty(pm);
		}
	}

	/**
	 * Apply the given mask to all the Employment entries for the given
	 * Contact where the Role matches the given role id.
	 *
	 * @param contId The database id of the Contact to be updated.
	 * @param roleId The database id of the Role to be matched against the
	 *            Employment.role values.
	 * @param mask The mask to be applied.
	 */
	private void saveContactMask(Integer contId, Integer roleId, long mask) {
		log.debug("contact id=" + contId + ", role id=" + roleId);
		final ContactDAO contactDAO = ContactDAO.getInstance();
		Contact ct = contactDAO.findById(contId);
		log.debug(" ct mask: " + Permission.toBinaryString(ct.getPermissionMask()));
		int rId = roleId;
		// Need to save any special Permissions that are assigned OUTSIDE of the Permission page and Roles
		long saveMasks = Permission.WRITE_ANY.getMask() + Permission.VIEW_ALL_PROJECTS.getMask()
				+ Permission.APPROVE_START_DOCS.getMask() + Permission.APPROVE_TIMECARD.getMask();
		for (Employment emp : ct.getEmployments()) {
			if (rId == emp.getRole().getId().intValue()) {
				log.debug("old mask: " + Permission.toBinaryString(emp.getPermissionMask()));
				long saveMasksInPm = emp.getPermissionMask() & saveMasks;
				emp.setPermissionMask(mask | saveMasksInPm);
				log.debug("new mask: " + Permission.toBinaryString(emp.getPermissionMask()));
				contactDAO.attachDirty(emp);
			}
		}
	}

	/**
	 * Create the list of user names (Contact`s) available in the drop-down when the "one person"
	 * radio button is selected.  Sets {@link #contactDL}.
	 */
	private void createUserDL() {
		//contactDL = ContactDAO.getInstance().createContactSelectList();
		List<Contact> list = new ArrayList<>();
		list.addAll(ContactDAO.getInstance().createAllContactsSet(true, true, isAdmin));
		contactDL = ContactDAO.getInstance().createContactSelectList(list, false);
	}

	/**
	 * Populate the drop-down list of Department`s displayed when the
	 * "members of a dept" radio button is selected.  Includes all departments in
	 * the current production for which the current user has access rights.
	 * Sets {@link #deptDL}.
	 */
	private void createDeptDL() {
		deptDL = DepartmentUtils.getDepartmentDL(SessionUtils.getCurrentContact());
	}

	/**
	 * This method is called from JSP every time the page is loaded or updated.
	 * It checks and updates a HeaderViewBean-maintained session variable, the
	 * 'prior tab index', to determine if we are actually coming to this page
	 * from another page. If so, we want to re-instantiate the PermissionBean so
	 * that it will get current data from the database, e.g., the list of cast &
	 * crew. We do this to simulate a ViewScoped bean.
	 * <p>
	 * See also {@link #redisplay()} regarding scope issues.
	 *
	 * @return false.
	 */
	public boolean getLoadPage() {
		int priorTab = SessionUtils.getInteger(Constants.ATTR_PRIOR_TAB_IX, -1);
		SessionUtils.put(Constants.ATTR_PRIOR_TAB_IX, null);
		//log.debug("prior tab=" + priorTab );
		if (priorTab >= 0) {
			// we came here from a different page, so force the bean to be re-instantiated!
			HttpSession session = SessionUtils.getHttpSession();
			String pattr = "permissionBean";
			Object obj = session.getAttribute(pattr);
			session.removeAttribute(pattr);
			log.debug("attribute (bean) removed = " + obj);
		}
		return false;
	}

	// * * * * *  GETTERS AND SETTERS  * * * * *

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	/** See {@link #viewStyle}. */
	public int getViewStyle() {
		return viewStyle;
	}
	/** See {@link #viewStyle}. */
	public void setViewStyle(int viewStyle) {
//		this.viewStyle = viewStyle;
	}

	/** See {@link #department}. */
	public Department getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(Department department) {
		this.department = department;
	}

	/** See {@link #departmentId}. */
	public Integer getDepartmentId() {
		return departmentId;
	}

	/** See {@link #departmentId}. */
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}

	/** See {@link #deptDL}. */
	public List<SelectItem> getDeptDL() {
		return deptDL;
	}
	/** See {@link #deptDL}. */
	public void setDeptDL(List<SelectItem> deptDL) {
		this.deptDL = deptDL;
	}

	/** See {@link #contactDL}. */
	public List<SelectItem> getContactDL() {
		return contactDL;
	}
	/** See {@link #contactDL}. */
	public void setContactDL(List<SelectItem> userDL) {
		contactDL = userDL;
	}

	/** See {@link #columns}. */
	public List<Column> getColumns() {
		return columns;
	}
	/** See {@link #columns}. */
	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	/** See {@link #colCount}. */
	public int getColCount() {
		return colCount;
	}
	/** See {@link #colCount}. */
	public void setColCount(int colCount) {
		this.colCount = colCount;
	}

	/** Determine the last column number that should be displayed.  Called
	 * by the JSP code. */
	public int getLastColNum() {
		return Math.min(sliderValue+MAX_COL_VIEW-1-getSliderMin(), colCount-1);
	}

	/** See {@link #sliderValue}. */
	public int getSliderValue() {
		return sliderValue;
	}
	/** See {@link #sliderValue}. */
	public void setSliderValue(int value) {
//		if (value != sliderValue) {
//			sliderValue = value;
//			redisplay();
//		}
	}

	/** Determine the minimum value for the horizontal slider
	 * control.  Called by the JSP code. */
	public int getSliderMin() {
		if (viewStyle == VIEW_STYLE_DEPT_MBRS) {
			return 1;
		}
		return 0;
	}

	/** See {@link #rows}. */
	public List<PermRow> getRows() {
		return rows;
	}
	/** See {@link #rows}. */
	public void setRows(List<PermRow> rows) {
		this.rows = rows;
	}

	/** See {@link #expandId}. */
	public int getExpandId() {
		return expandId;
	}
	/** See {@link #expandId}. */
	public void setExpandId(int expandId) {
		this.expandId = expandId;
	}


	// Local classes for holding display information

	/**
	 * Information about one column on the page. Depending on the view style,
	 * this may identify the department, or the role, or the contact and role
	 * represented by the data in the column.  It also holds the label(s) to
	 * be displayed on the page at the top of the column.
	 */
	static public class Column implements Serializable {
		/** */
		private static final long serialVersionUID = 7008428345123109036L;

		/** The Contact.id value of the person this Column represents. This is only
		 * set for VIEW_STYLE_DEPT_MBRS. */
		private Integer contactId;

		/** The Role.id value of the role this Column represents. This is set for
		 * VIEW_STYLE_DEPT_MBRS and VIEW_STYLE_ONE_PERSON. */
		private Integer roleId;

		/** The Department.id value for the Department this Column represents. This is
		 * set for the VIEW_STYLE_ALL_DEPTS, and for the *first* column in VIEW_STYLE_DEPT_MBRS. */
		private Integer departmentId;

		/** The 64-bit mask that will be calculated during save processing to be
		 * the setting of all the checkboxes in this column. This value may then
		 * be applied to the appropriate ProjectMember entries. */
//		private long mask;

		/** Set during save processing: True iff at least one of the clusters in
		 * this column changed, meaning that this column's mask must be
		 * calculated and applied to the database. */
//		private boolean changed;

		/** First line of heading for this column. */
		private String heading1;
		/** Second line of heading for this column (may be null). */
		private String heading2;

		/** See {@link #heading1}. */
		public String getHeading1() {
			return heading1;
		}
		/** See {@link #heading1}. */
		public void setHeading1(String heading1) {
			this.heading1 = heading1;
		}

		/** See {@link #heading2}. */
		public String getHeading2() {
			return heading2;
		}
		/** See {@link #heading2}. */
		public void setHeading2(String heading2) {
			this.heading2 = heading2;
		}
	}

	/**
	 * This represents one horizontal section ("row") representing all the
	 * information about one PermissionCluster. It contains two PermRowPart`s,
	 * one for the Edit settings and one for the View settings.
	 */
	static public class PermRow implements Serializable {
		/** */
		private static final long serialVersionUID = - 9014868818901361123L;
		private PermissionCluster cluster;
		private PermRowPart[] parts = new PermRowPart[2];

		private PermRow() {
			parts[0] = new PermRowPart();
			parts[1] = new PermRowPart();
		}

		/** See {@link #cluster}. */
		public PermissionCluster getCluster() {
			return cluster;
		}
		/** See {@link #cluster}. */
		public void setCluster(PermissionCluster cluster) {
			this.cluster = cluster;
		}

		/** See {@link #parts}. */
		public PermRowPart[] getParts() {
			return parts;
		}
		/** See {@link #parts}. */
		public void setParts(PermRowPart[] parts) {
			this.parts = parts;
		}
	}

	/**
	 * This represents either an Edit or View "half" of a cluster row. It has a
	 * list of the names of the individual Permission`s that it represents, and
	 * it tracks whether or not the list is expanded on the page. It contains
	 * multiple PermCluster objects, one for each column that is currently
	 * displayed.
	 * <p>
	 * The display of an entire PermRowPart will be suppressed if the particular
	 * cluster it is a part of has either no Edit or no View permissions.
	 */
	static public class PermRowPart implements Serializable {
		/** */
		private static final long serialVersionUID = 435934739124921739L;
		/** True iff this section should be displayed in expanded style -- with
		 * the individual Permission rows visible. */
		private boolean expand;
		/** True iff this PermRowPart should be hidden -- currently used only for
		 * the case where a View or Edit sub-group has no entries, such as the View
		 * part of the Special Permissions cluster. */
		private boolean hide;
		/** There is a PermCluster for each column of data.  In the "people-in-dept"
		 * view, the first (0 index) entry is the department summary. */
		private List<PermCluster> clusters = new ArrayList<>();
		/** The Permission names, which are displayed in 'expanded' mode. */
		private List<String> names = new ArrayList<>();

		/** See {@link #expand}. */
		public boolean getExpand() {
			return expand;
		}
		/** See {@link #expand}. */
		public void setExpand(boolean expand) {
			this.expand = expand;
		}

		/** See {@link #hide}. */
		public boolean getHide() {
			return hide;
		}
		/** See {@link #hide}. */
		public void setHide(boolean hide) {
			this.hide = hide;
		}

		/** See {@link #clusters}. */
		public List<PermCluster> getClusters() {
			return clusters;
		}
		/** See {@link #clusters}. */
		public void setClusters(List<PermCluster> clusters) {
			this.clusters = clusters;
		}

		/** See {@link #names}. */
		public List<String> getNames() {
			return names;
		}
		/** See {@link #names}. */
		public void setNames(List<String> names) {
			this.names = names;
		}
	}

	/**
	 * This holds the check-box settings for one "cell" in the display -- either the
	 * View or Edit permission settings for one Department or one person.
	 */
	static public class PermCluster implements Serializable {

		/** */
		private static final long serialVersionUID = - 747121927960915576L;

		/** Which "major" row this cluster is in, i.e., which PermissionCluster
		 * it relates to. */
		private int rowNumber;

		/** The part number, either 0 or 1, this cluster is in. Part 0 is for
		 * the View-related permissions, and part 1 is for the Edit-related
		 * permissions. */
		private int partNumber;

		/** The column number that this cluster is in within the displayed grid. */
		private int columnNumber;

		/** The bit mask which is the sum (OR) of all the masks of
		 * the PermSettings contained in this cluster.  */
		private long mask = 0;

		/** True if the user has changed any settings within this cell during the
		 * current edit cycle. */
		private boolean changed = false;

		/** The "summary" checkbox instance representing the rolled-up state of
		 * all the detail lines within this cluster. */
		private TriStateCheckboxExt checkbox = new TriStateCheckboxExt();

		/** Holds a PermSetting for each "detail line" within the cell, that is,
		 * for each individual permission within the cluster. */
		private List<PermSetting> settings = new ArrayList<>();

		public PermCluster() {
			checkbox.setId(this);
		}

		public PermCluster(long msk) {
			this();
			mask = msk;
		}

		void setPosition(int row, int part, int col) {
			rowNumber = row;
			partNumber = part;
			columnNumber = col;
		}

		/** See {@link #mask}. */
		public long getMask() {
			return mask;
		}
		/** See {@link #mask}. */
		public void setMask(long mask) {
			this.mask = mask;
		}

		/** See {@link #settings}. */
		public List<PermSetting> getSettings() {
			return settings;
		}
		/** See {@link #settings}. */
		public void setSettings(List<PermSetting> settings) {
			this.settings = settings;
		}

		/** See {@link #checkbox}. */
		public TriStateCheckboxExt getCheckbox() {
			return checkbox;
		}
		/** See {@link #checkbox}. */
		public void setCheckbox(TriStateCheckboxExt checkbox) {
			this.checkbox = checkbox;
		}

		public void listenCheckBoxChange(ValueChangeEvent event) {
			checkbox.listenChecked(event);
		}
	}

	/**
	 * Holds the information related to a single check-box on the page,
	 * representing either an individual permission, or the row-summary of
	 * permissions in the first column of the VIEW_STYLE_DEPT_MBRS style
	 * display. (View or Edit summary roll-ups do not use a PermSetting, but are
	 * kept in the {@link PermCluster} object.)
	 */
	static public class PermSetting implements Serializable {
		/** */
		private static final long serialVersionUID = 8437964711735177236L;

		/** The single-bit mask that defines the permission which this
		 * row represents. */
		private long mask = 0;

		/** The checkbox instance for this detail line. */
		private TriStateCheckboxExt checkbox = null;

		/** Which cluster we are in; used to backtrack when our checkbox is
		 * clicked. */
		private PermCluster cluster;

		/** True iff the checkbox is the row-summary (first column) for a department
		 * style entry. */
		private boolean rowSummary = false;

		public PermSetting() {
		}

		public PermSetting(PermCluster c, byte val) {
			checkbox = new TriStateCheckboxExt();
			init(c, val);
		}

		private void init(PermCluster c, byte val) {
			cluster = c;
			checkbox.setCheckValue(val);
			checkbox.setId(this);
		}

		/** See {@link #checkbox}. */
		public TriStateCheckboxExt getCheckbox() {
			return checkbox;
		}
		/** See {@link #checkbox}. */
		public void setCheckbox(TriStateCheckboxExt checkbox) {
			this.checkbox = checkbox;
		}

		public void listenCheckBoxChange(ValueChangeEvent event) {
			checkbox.listenChecked(event);
		}
	}

}
