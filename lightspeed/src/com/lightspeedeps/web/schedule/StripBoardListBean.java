package com.lightspeedeps.web.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.ListView;

/**
 * The backing bean for the Strip Board Sets page -- the list
 * of Stripboards within the current Project.
 */
@ManagedBean
@ViewScoped
public class StripBoardListBean implements PopupHolder, Serializable {

	/** */
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(StripBoardListBean.class);

	protected static final int ACT_SET_DEFAULT_STRIPBOARD = 20;

	private List<Stripboard> stripboardList;
	private Project project;

	/** The Stripboard database id of the default Stripboard for this Project. Used
	 * to control the radio-button selection in the "Default" column of the list page. */
	private int currentStripboardId;

	/** A Map from Stripboard id to a List of number of shooting days, indexed by unitNumber.
	 * That is, shootingDays.get(sbId).get(unitNumber) returns the number of shooting
	 * days for the Stripboard with id 'sbId', and Unit with number 'unitNumber'.  In
	 * JSP this can be referenced as shootingDays[sbId][unitNumber]. */
	private Map<Integer, List<Integer>> shootingDays;

	/** The database id of the stripboard to be deleted. */
	private Integer removeId;

	/** The database id of the stripboard which should be made the default stripboard. */
	private Integer defaultId;


	/** Constructor */
	public StripBoardListBean() {
		log.debug("");
		initialize();
	}

	/**
	 * Initialization of stripboard list, the current "default" (in use) stripboard,
	 * the list of SelectItems for the radio buttons, etc.
	 */
	private void initialize() {
		log.debug("");
		try {
			project = SessionUtils.getCurrentProject();
			Stripboard stripboard = project.getStripboard();
			if (stripboard != null) {
				currentStripboardId = stripboard.getId();
			}
			else {
				currentStripboardId = -1;
			}
			stripboardList = StripboardDAO.getInstance().findByProperty("project", project);
			//log.debug("Strip Board list size -- >  "+stripboardList.size());
			shootingDays = new HashMap<>();
			for (Stripboard board : stripboardList) {
				board.setSelected(board.getId().equals(currentStripboardId));
				List<Integer> sbList = new ArrayList<>(10);
				for (int i=0; i <= project.getHighestUnitNumber(); i++) {
					sbList.add(null);
				}
				for (Unit u : project.getUnits()) {
					sbList.set(u.getNumber(), board.getShootingDays(u));
					//log.debug("u#=" + u.getNumber() + ", days=" + board.getShootingDays(u));
				}
				shootingDays.put(board.getId(), sbList);
				//log.debug("sb-id=" + board.getId() + ", map: " + sbList);
			}
		}
		catch (Exception e) {
			EventUtils.logError("Init StripBoardListBean failed", e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public String actionDelete() {
		log.debug("");
		PopupBean.getInstance().show(
				this, ListView.ACT_DELETE_ITEM, "Stripboard.Delete.");
		return null;
	}

	/**
	 *  Delete selected stripboard from the Database.
	 *  Called when user clicks the Delete icon on a row in the table.
	 */
	public String actionDeleteOk() {
		try {
			StripboardDAO stripboardDAO = StripboardDAO.getInstance();
			Stripboard stripboard = stripboardDAO.findById(removeId);
			stripboardDAO.remove(stripboard);
			initialize();
		}
		catch (Exception e) {
			EventUtils.logError("delete stripboard failed.", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the Edit button on each line of the stripboard list.
	 * For mobile devices, displays a dialog box indicating that the editor is
	 * not available; otherwise, it returns the navigation string for the edit
	 * page.
	 *
	 * @return The appropriate faces navigation string, either empty for mobile
	 *         devices, or the Stripboard Edit navigation value.
	 */
	public String actionEdit() {
		return actionEditCheck();
	}

	/**
	 * See {@link #actionEdit}. The code is placed in a static member so it is
	 * available to StripBoardViewBean without instantiating this object. (The
	 * non-static version is required for JSP-invoked Action methods.)
	 */
	public static String actionEditCheck() {
		log.debug("");
		if (SessionUtils.isPhoneUser()) {
			PopupBean.getInstance().show(null, 0,
					"Stripboard.NoMobileEdit.Title",
					"Stripboard.NoMobileEdit.Text",
					"Confirm.OK", null); // no cancel button
			return null;
		}
		else {
			Project proj = SessionUtils.getCurrentProject();
			if (proj.getStripsLockedBy() != null &&
					! proj.getStripsLockedBy().equals(SessionUtils.getCurrentUser().getId())) {
				PopupBean.getInstance().show(null, 0,
						"Stripboard.EditLocked.Title",
						"Stripboard.EditLocked.Text",
						"Confirm.OK", null); // no cancel button
				log.debug("edit prevented: locked by user #" + proj.getStripsLockedBy());
				return null;
			}
			else {
				HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_STRIPBOARD_IX);
				return HeaderViewBean.SCHEDULE_MENU_STRIPBOARD_EDIT;
			}
		}
	}

	/**
	 * Called when the user clicks Cancel in one of our confirmation dialog
	 * boxes.
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ACT_SET_DEFAULT_STRIPBOARD:
				res = defaultStripboardChangeCancel();
				break;
		}
		return res;
	}

	/**
	 * Called when the user clicks OK in one of our confirmation dialog
	 * boxes.  It checks the action value and calls the appropriate method to
	 * perform the task.
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_SET_DEFAULT_STRIPBOARD:
				res = defaultStripboardChangeOk();
				break;
			case ListView.ACT_DELETE_ITEM:
				res = actionDeleteOk();
				break;
		}
		return res;
	}

	/**
	 * Change Default Stripboard -- called when the user clicks on one of the
	 * radio buttons. This is the valueChangeListener specified on the set of
	 * radio buttons in the JSP.  We just put up a confirmation dialog and
	 * wait for the response (via {@link #confirmOk(int)} or {@link #confirmCancel(int)}).
	 *
	 * @param event The change event supplied by the framework. The 'newValue'
	 *            field has the database id of the stripboard whose radio-button
	 *            the user selected.
	 */
	public void defaultStripboardChange(ValueChangeEvent evt) {
//		if (! evt.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
//			// simpler to schedule event for later - after "setXxxx()" are called from framework
//			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
//			evt.queue();
//			return;
//		}
		if (evt.getNewValue() != null) {
			defaultId = (Integer) evt.getComponent().getAttributes().get("currentRow");
			Stripboard board = StripboardDAO.getInstance().findById(defaultId);
			if (board != null) {
				PopupBean conf = PopupBean.getInstance();
				conf.show(
						this, ACT_SET_DEFAULT_STRIPBOARD,
						"Stripboard.Default.Title",
						"Stripboard.Default.Ok",
						"Confirm.Cancel");
				String msg = MsgUtils.formatMessage("Stripboard.Default.Text",
						board.getDescription());
				conf.setMessage(msg);
			}
		}
	}

	/**
	 *  Change Default Stripboard -- called when the user clicks on one
	 *  of the radio buttons.
	 */
	public String defaultStripboardChangeOk() {
		try {
			log.debug("default id=" + defaultId);
			if (defaultId != null) {
				Stripboard board = StripboardDAO.getInstance().findById(defaultId);
				if (board != null) {
					ProjectDAO.getInstance().setStripboard(project, board);
					currentStripboardId = defaultId;
				}
				initialize();
			}
		}
		catch (Exception e) {
			EventUtils.logError("set default Stripboard failed.", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 *  Change Default Stripboard -- called when the user clicks on one
	 *  of the radio buttons.
	 */
	public String defaultStripboardChangeCancel() {
		try {
			// Tried this to fix radio buttons, but appears that rendering doesn't refresh them.
//			stripboardList = StripboardDAO.getInstance().findByProperty("project", project);
//			for (Stripboard board : stripboardList) {
//				board.setSelected(board.getId().equals(currentStripboardId));
//			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return HeaderViewBean.SCHEDULE_MENU_STRIPBOARDS; // TODO return null if we can get the radio buttons to refresh!!
		// the only way I could get the radio buttons to show proper settings after cancel was to refresh the whole page.
	}

	/**
	 * Create a new copy of the specified Stripboard.
	 * Called when the user clicks the Copy button/link in the stripboard table.
	 * The new copy will have a new set of Strips, identical to the original board.
	 * The "last-updated" user and timestamp fields in the new stripboard will
	 * reflect the current user and current date/time.
	 */
	public void copyStripboard(ActionEvent evt) {
		try {
			StripboardDAO stripboardDAO = StripboardDAO.getInstance();
			Stripboard stripboard;
			Integer id = (Integer) evt.getComponent().getAttributes().get("currentRow");
			stripboard = stripboardDAO.findById(id);
			log.debug(stripboard);

			// Start with a clone of the current one
			Stripboard replicate = (Stripboard)stripboard.clone();

			// update revision number
			replicate.setRevision(stripboardDAO.findMaxRevision(project) + 1);

			// Add revision number to description for clarity
			String text = stripboard.getDescription();
			text = text.replaceAll(" \\(Rev [0-9]+\\)", ""); // First remove old "Rev x" if it exists!
			replicate.setDescription(text + " (Rev " + replicate.getRevision() + ")");

			// Set last-updated values to current user and current date/time
			replicate.setLastSaved(new Date());
			replicate.setUser(SessionUtils.getCurrentUser());

			stripboardDAO.save(replicate); // Strips (and Strip Notes) are saved via cascade.

			initialize(); // re-initialize display list
		}
		catch (Exception e) {
			EventUtils.logError("StripBoardListBean copyStripboard failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public List<Stripboard> getStripboardList() {
		return stripboardList;
	}
	public void setStripboardList(List<Stripboard> stripboardList) {
		this.stripboardList = stripboardList;
	}

	/** See {@link #shootingDays}. */
	public Map<Integer, List<Integer>> getShootingDays() {
		return shootingDays;
	}

	/** See {@link #currentStripboardId}. */
	public int getCurrentStripboardId() {
		return currentStripboardId;
	}
	/** Don't let the JSP framework update this value -- we will prompt the
	 * user for confirmation (via ValueChangeEvent listener), and update
	 * it once the user "ok"s the change.
	 * @see #currentStripboardId */
	public void setCurrentStripboardId(int id) {
		log.debug("set id=" + id);
//		this.currentStripboardId = currentStripboardId;
	}

	/** Returns the number of stripboards in the current project. */
	public int getStripboardCount() {
		return stripboardList.size();
	}

	/** See {@link #removeId}. */
	public Integer getRemoveId() {
		return removeId;
	}
	/** See {@link #removeId}. */
	public void setRemoveId(Integer removeId) {
		this.removeId = removeId;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

}
