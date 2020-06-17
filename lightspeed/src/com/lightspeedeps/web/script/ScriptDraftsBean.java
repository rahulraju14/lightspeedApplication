//	File Name:	ScriptDraftsBean.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.TextElementDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.TextElement;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the Scripts (Script Revision list) page.
 * Note that most of the right half of that page, where the script text
 * is displayed, is backed by ScriptPageBean.
 */
@ManagedBean
@ViewScoped
public class ScriptDraftsBean extends ListView implements Serializable {
	/** */
	private static final long serialVersionUID = 5839186046297053982L;

	private static final Log log = LogFactory.getLog(ScriptDraftsBean.class);

	private static final int TAB_DETAIL = 0;

	protected static final int ACT_DELETE_TEXT = 10;
	protected static final int ACT_MAKE_DEFAULT = 11;

	// Fields

	private List<Script> scriptList;
	private Project project;
	/** Currently selected item. */
	private Script script;

	/** The drop-down list used for the Compare scripts pop-up. */
	private List<SelectItem> compareDL;
	/** The id of the script selected by the user for comparison. */
	private Integer compareId;

	/** */
	private boolean showCompare;

	/** True iff the "Add Revision" dialog box should be displayed. */
	private boolean showAddNewScriptRev;

	private transient ScriptDAO scriptDAO;

	/* Constructor */
	public ScriptDraftsBean() {
		super(Script.SORT_REV, "ScriptList.");
		log.debug("Initializing ScriptDraftsBean instance");
		try {
			project = SessionUtils.getCurrentProject();
			refreshList();
			Integer id = SessionUtils.getInteger(Constants.ATTR_SCRIPT_LIST_ID);
			setupSelectedItem(id);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		project = ProjectDAO.getInstance().refresh(project);
		if (script != null) {
			script.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_SCRIPT_LIST_ID, null);
			script = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			script = getScriptDAO().findById(id);
			if (script == null || ! script.getProject().equals(project)) {
				script = null;
				id = findDefaultId();
				if (id != null) {
					script = getScriptDAO().findById(id);
				}
			}
			SessionUtils.put(Constants.ATTR_SCRIPT_LIST_ID, id);
		}
		ScriptPageBean.getInstance().setupScript(script);
		if (script != null) {
			script.setSelected(true);
			selectRowState(script);
//			int ix = scriptList.indexOf(script);
//			//log.debug("ix="+ix);
//			if (ix >= 0) { // ensure 'selectedRow' matches
//				setSelectedRow(ix);
//			}
			forceLazyInit();
		}
		compareDL = null;
	}

	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		if (SessionUtils.getCurrentProject().getScript() != null) {
			id = SessionUtils.getCurrentProject().getScript().getId();
		}
		else {
			List<Script> list = getItemList();
			if (list.size() > 0) {
				id = list.get(0).getId();
			}
		}
		return id;
	}

	@SuppressWarnings("unused")
	private void forceLazyInit() {
		boolean b = script.getSceneText();
		for (Script s : getScriptList()) {
			String str = s.getColorName().getName();
		}
	}

	/**
	 * Delete an item from the list. This is an Action method called by the Delete button. Here we just put up
	 * the confirmation dialog.
	 */
	@Override
	public String actionDelete() {
		PopupBean conf = PopupBean.getInstance();
		conf.show(
				this, ACT_DELETE_ITEM,
				"ScriptList.Delete.Title",
				"ScriptList.Delete.Ok",
				"Confirm.Cancel");
		String msg = MsgUtils.formatMessage("ScriptList.Delete.Text",
				script.getDescription(), script.getColorName().getName());
		conf.setMessage(msg);
		addClientResizeScroll();
		return null;
	}

	/**
	 * Delete the currently selected Script.  Called when user
	 * clicks the "Delete" button on the delete confirmation popup.
	 */
	@Override
	protected String actionDeleteOk() {
		try {
			scriptList.clear();	// clear our local copies
			project = ProjectDAO.getInstance().refresh(project);
			script = getScriptDAO().refresh(script);
			if (script != null) {
				// (It will be null if someone else deleted it in between! This happened once.)

				// save all the scene numbers for cleaning up strips...
				List<String> sceneNumbers = new ArrayList<>();
				for (Scene s : script.getScenes()) {
					sceneNumbers.add(s.getNumber());
				}

				//Import.setSQLiteSyncOff(); // speed up SQLite for offline version
				getScriptDAO().remove(project, script);
				// removed Script; plus ScriptElements if its the last script

				if (project.getScripts().size() == 0) {
					StripDAO.getInstance().deleteOrphanStrips(project);
				}
				else {
					deleteOrphanStrips(sceneNumbers); // delete strips that are no longer needed.
				}
			}
		}
		catch (Exception ex) {
			EventUtils.logError(ex);
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR,
					"Internal error - script could not be deleted.");
		}
		finally {
			//Import.setSQLiteSyncOn();
		}

		try {
			refreshList();
			setupSelectedItem(null);
			addClientResizeScroll();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for "Delete text" button - prompt the user.
	 *
	 * @return null navigation string
	 */
	public String actionDeleteText() {
		PopupBean conf = PopupBean.getInstance();
		conf.show(
				this, ACT_DELETE_TEXT,
				"ScriptList.DeleteText.Title",
				"ScriptList.DeleteText.Ok",
				"Confirm.Cancel");
		String msg = MsgUtils.formatMessage("ScriptList.DeleteText.Text",
				script.getDescription());
		conf.setMessage(msg);
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for "OK" confirmation to "Delete text" prompt. Delete all
	 * the TextElement records associated with the script.
	 *
	 * @return null navigation string
	 */
	private String actionDeleteTextOk() {
		try {
			TextElementDAO textElementDAO = TextElementDAO.getInstance();
			script = getScriptDAO().refresh(script);
			for (Scene scene : script.getScenes()) {
				log.debug("scene: "+scene.getId());
				List<TextElement> te = scene.getTextElements();
				scene.setTextElements(null);
				for (TextElement textElement : te) {
					//log.debug("text: "+textElement.getId());
					textElementDAO.delete(textElement);
				}
			}
			refreshList();
			setupSelectedItem(script.getId());
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for "Make Default" check-box - prompt the user.
	 */
	public void actionMakeDefault(ValueChangeEvent evt) {
		PopupBean conf = PopupBean.getInstance();
		conf.show(
				this, ACT_MAKE_DEFAULT,
				"ScriptList.Default.Title",
				"ScriptList.Default.Ok",
				"Confirm.Cancel");
		String msg = MsgUtils.formatMessage("ScriptList.Default.Text",
				script.getDescription());
		conf.setMessage(msg);
		addClientResizeScroll();
	}


	/**
	 * Action method for "OK" confirmation to "Make Default" prompt. Make the
	 * current script the default one -- the one used for the breakdown page,
	 * stripboard, etc.
	 *
	 * @return null navigation string
	 */
	private String actionMakeDefaultOk() {
		try {
			script = getScriptDAO().refresh(script);
			Project proj = SessionUtils.getCurrentProject();
			ProjectDAO projectDAO = ProjectDAO.getInstance();
			projectDAO.setScript(proj, script);
			projectDAO.attachDirty(proj);
			refreshList();
			setupSelectedItem(script.getId());
			MsgUtils.addFacesMessage("ScriptList.SetActive", FacesMessage.SEVERITY_INFO);
			addClientResizeScroll();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Add" button, to add a new Script Revision. This
	 * opens the dialog box and ensures the ImportScript bean is in an
	 * initialized state.
	 *
	 * @return null navigation string
	 */
	public String actionAdd() {
		showAddNewScriptRev = true;
		ImportScript importScript = ImportScript.getInstance();
		importScript.reset();
		addClientResizeScroll();
		addFocus("addScript");
		return null;
	}

	/**
	 * Action method for the Cancel button on the "Add Revision" pop-up dialog.
	 * This just closes the dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionCancelAdd() {
		log.debug("");
		showAddNewScriptRev = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "X" close icon button on the "Add Revision" pop-up dialog.
	 * This just closes the dialog box.
	 * @param evt Ajax event from the framework.
	 */
	public void actionCloseAdd(AjaxBehaviorEvent evt) {
		log.debug("");
		actionCancelAdd();
	}

	/**
	 * Action method for the "Continue" button on the "Add Revision" pop-up
	 * dialog.  This instantiates an ImportScript and uses that to continue
	 * the processing.
	 *
	 * @return A null navigation string if the dialog is left up, or a
	 *         navigation string to the first page of the "revision imported"
	 *         sequence.
	 */
	public String actionContinueAdd() {
		log.debug("");
		String ret = null;
		ImportScript importScript = ImportScript.getInstance();
		if (importScript.checkDescription()) {
			ret = importScript.actionImport();
			if (ret == null) { // leave dialog box up
				addJavascript("hideUploadProg();");
			}
			else {
				showAddNewScriptRev = false;
			}
		}
		else {
			log.debug("checkDescription failed");
			addJavascript("hideUploadProg();");
		}
		return ret;
	}

	public String actionOpenCompare() {
		showCompare = true;
		addClientResizeScroll();
		return null;
	}

	public String actionCancelCompare() {
		showCompare = false;
		addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "X" close icon button on the "Compare Scripts" pop-up dialog.
	 * This just closes the dialog box.
	 * @param evt Ajax event from the framework.
	 */
	public void actionCloseCompare(AjaxBehaviorEvent evt) {
		log.debug("");
		actionCancelCompare();
	}

	public String actionCompareScript() {
		try {
			showCompare = false;
			log.debug("compare id=" + script.getId() + " to id=" + compareId);
			SessionUtils.put(Constants.ATTR_SCRIPT_COMPARE_IMPORT, false);
			SessionUtils.put(Constants.ATTR_SCRIPT_COMPARE_LEFT_ID, script.getId());
			SessionUtils.put(Constants.ATTR_SCRIPT_COMPARE_RIGHT_ID, compareId);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "compare";
	}

	/**
	 * Action method for the "Save" button. Checks for invalid edit
	 * values and issues messages if necessary, or saves the updated
	 * Script object.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionSave()
	 */
	@Override
	public String actionSave() {
		log.debug("newEntry=" + newEntry);
		if (script.getDescription() == null || script.getDescription().trim().length() == 0) {
			MsgUtils.addFacesMessage("RealElement.BlankName", FacesMessage.SEVERITY_ERROR);
			setSelectedTab(TAB_DETAIL);
			return null;
		}
		try {
			script.setDescription(script.getDescription().trim());
			if (!newEntry) {
				getScriptDAO().attachDirty(script);
				updateItemInList(script);
				script.setSelected(true);
				forceLazyInit();
			}
			else {
				refreshList();
			}
			SessionUtils.put(Constants.ATTR_SCRIPT_LIST_ID, script.getId());
			script = getScriptDAO().refresh(script);
			ScriptPageBean.getInstance().setupScript(script);
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		if (script != null) {
			// Refresh our copy from the database
			script = getScriptDAO().findById(script.getId());
			updateItemInList(script);
			setupSelectedItem(script.getId());
		}
		super.actionCancel();
		return null;
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE_ITEM:
				res = actionDeleteOk();
				break;
			case ACT_DELETE_TEXT:
				res = actionDeleteTextOk();
				break;
			case ACT_MAKE_DEFAULT:
				res = actionMakeDefaultOk();
				break;
		}
		return res;
	}

	/**
	 * Set header menus for entering Script | Drafts page.
	 */
	public static void navigateToScriptDrafts() {
		HeaderViewBean.setMenu(HeaderViewBean.SUB_MENU_REVISIONS_IX);
	}

	@Override
	protected Comparator<Script> getComparator() {
		Comparator<Script> comparator = new Comparator<Script>() {
			@Override
			public int compare(Script c1, Script c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			if (item != null) {
				((Script)item).setSelected(b);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void refreshList() {
		try {
			project = ProjectDAO.getInstance().refresh(project);
			scriptList = getScriptDAO().findByProperty("project", project);
			setAscending(false);	// sort in descending revision order
			sort();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private void deleteOrphanStrips(List<String> sceneNumbers) {
		final SceneDAO sceneDAO = SceneDAO.getInstance();
		List<String> list = new ArrayList<>();
		for (String sceneNum : sceneNumbers) {
			if ( ! sceneDAO.existsSceneInProject(sceneNum, project)) {
				// No other scenes -- in any script -- have this scene number, so delete
				// any Strips that refer to this scene number.
				list.add(sceneNum);
			}
		}
		if (list.size() > 0) {
			StripDAO.getInstance().deleteOrphanStrips(list, project);
		}
	}

	/** Create the list of Script names presented in the Compare pop-up.
	 * See {@link #compareDL}. */
	private List<SelectItem> createCompareDL() {
		List<SelectItem> list = new ArrayList<>();
		if (script != null && scriptList != null) {
			int currentId = script.getId();
			for (Script s : scriptList) {
				if (s.getId() != currentId) {
					list.add(new SelectItem(s.getId(), s.getDescription() + " (" + s.getColorName().getName() + ")"));
				}
			}
		}
		return list;
	}

	/** See {@link #script}. */
	public Script getScript() {
		return script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	public List<Script> getScriptList() {
		return scriptList;
	}
	public void setScriptList(List<Script> scriptList) {
		this.scriptList = scriptList;
	}
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List getItemList() {
		return scriptList;
	}

	public int getListSize() {
		return scriptList.size();
	}

	public boolean getIsDefault() {
		return (script != null && project.getScript() != null &&
				script.getId().equals(project.getScript().getId()));
	}
	/** dummy method for JSP validation */
	public void setIsDefault(boolean b) {
		// no-op
	}

	/** See {@link #compareDL}. */
	public List<SelectItem> getCompareDL() {
		if (compareDL == null || compareDL.size() == 0) {
			compareDL = createCompareDL();
		}
		return compareDL;
	}
	/** See {@link #compareDL}. */
	public void setCompareDL(List<SelectItem> compareDL) {
		this.compareDL = compareDL;
	}

	/** See {@link #compareId}. */
	public Integer getCompareId() {
		return compareId;
	}
	/** See {@link #compareId}. */
	public void setCompareId(Integer compareId) {
		this.compareId = compareId;
	}

	/** See {@link #showCompare}. */
	public boolean getShowCompare() {
		return showCompare;
	}
	/** See {@link #showCompare}. */
	public void setShowCompare(boolean showCompare) {
		this.showCompare = showCompare;
	}

	public List<SelectItem> getColorNameList() {
		return ScriptUtils.getColorNameList();
	}

	/** See {@link #showAddNewScriptRev}. */
	public boolean getShowAddNewScriptRev() {
		return showAddNewScriptRev;
	}
	/** See {@link #showAddNewScriptRev}. */
	public void setShowAddNewScriptRev(boolean showAddNewScriptRev) {
		this.showAddNewScriptRev = showAddNewScriptRev;
	}

	private ScriptDAO getScriptDAO() {
		if (scriptDAO == null) {
			scriptDAO = ScriptDAO.getInstance();
		}
		return scriptDAO;
	}

}
