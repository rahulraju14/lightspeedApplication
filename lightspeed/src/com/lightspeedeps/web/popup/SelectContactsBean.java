// SelectContactsBean.java
package com.lightspeedeps.web.popup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.DepartmentUtils;


/**
 * This is the backing bean for the "Select Contacts" popup, which is used for
 * selecting a list of Contact`s or Department`s to whom the user will send
 * reports. See jsp/common/selectcontacts.xhtml.  Note that the lists passed to
 * the JSP for display are lists of Item objects, rather than Contact or
 * Department objects.
 */
@ManagedBean
@ViewScoped
public class SelectContactsBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = 242940904672903407L;

	private static final Log log = LogFactory.getLog(SelectContactsBean.class);

	/** Setting for 'onlyOption' field (in drop-down); must match value in JSP! */
	private static final String ONLY_OPTION_UNIT = "U";
	/** Setting for 'onlyOption' field (in drop-down); must match value in JSP! */
	private static final String ONLY_OPTION_PROJECT = "P";

	/** Prefix of Item.key for entries representing Contact`s. This letter is greater
	 * than the one for a Department, so the department entries will sort first in the list. */
	private static final String KEY_CONTACT = "P";

	/** Prefix of Item.key for entries representing Department`s. This letter is less
	 * than the one for a Contact, so the department entries will sort first in the list. */
	private static final String KEY_DEPT = "D";

	// FIELDS

	/** Our caller; used for callback when the pop-up is closed. */
	private SelectContactsHolder holder;

	/** The list of entries representing people not yet added to the
	 * "selected" list. The Item.key field is used to distinguish between
	 * Contact and Department entries. */
	private List<Item> unselectedList;

	/** The list of entries representing people who have been "selected".
	 *  The Item.key field is used to distinguish between
	 * Contact and Department entries. */
	private List<Item> selectedList;

	/** The database id of the contact to be moved from the unselected
	 * list to the selected list.  Set via an f:setPropertyActionListener
	 * tag in the JSP */
	private Integer addId;

	/** The database id of the contact to be moved from the selected
	 * list to the unselected list.  Set via an f:setPropertyActionListener
	 * tag in the JSP */
	private Integer removeId;

	/** If true, only show Contacts associated with either the current project
	 * or the current Unit, depending on the setting of the 'onlyOption' field. */
	private boolean projectOnly;

	/** Selection for "project only" or "unit only" list of contacts. */
	private String onlyOption;

	/** The Unit, supplied by our caller, which may be used to filter the list
	 * of available contacts. */
	private Unit unit;

	/** True, iff popup is called for Approver Group. */
	private boolean isApproverGroup = false;

	/** True, iff popup is called to add Production Approvers. */
	private boolean isProductionApprover = false;

	/** A message to display in error situations. */
	private String errorMessage;

	/** True iff the user hit OK with a blank input field and inputRequired
	 * was true. */
	private boolean inputError;

	/* Constructor */
	public SelectContactsBean() {
		log.debug("Init");
		if (SessionUtils.getProduction().getType().getEpisodic()) {
			onlyOption = ONLY_OPTION_PROJECT;
		}
		else {
			onlyOption = ONLY_OPTION_UNIT;
		}
	}

	public static SelectContactsBean getInstance() {
		return (SelectContactsBean)ServiceFinder.findBean("selectContactsBean");
	}

	/**
	 * Create the initial selected and unselected lists.
	 */
	private void createLists() {
		log.debug("");
		Contact contact = SessionUtils.getCurrentContact();
		selectedList = new ArrayList<>();
		if (contact != null) {
			selectedList.add(makeItem(contact));
		}
		refreshList();
	}

	/**
	 * Create the list of departments and contacts to choose from.
	 */
	private void refreshList() {
		unselectedList = new ArrayList<>();

		// Get the department list and add to item list
		List<Department> deptList = DepartmentUtils.getDepartmentList();
		for (Department dp : deptList) {
			unselectedList.add(makeItem(dp));
		}

		List<Contact> contactList;
		// Get the appropriate list of contacts
		if (projectOnly) {
			if (onlyOption.equals(ONLY_OPTION_PROJECT) || unit == null) {
				contactList = ProjectMemberDAO.getInstance()
						.findByProjectDistinctContact(SessionUtils.getCurrentProject());
			}
			else {
				contactList = ProjectMemberDAO.getInstance().findByUnitDistinctContact(unit);
			}
		}
		else {
			contactList = ContactDAO.getInstance().findByProductionActive(SessionUtils.getProduction());
		}

		// Add the contacts to the item list
		for (Contact cn : contactList) {
			unselectedList.add(makeItem(cn));
		}
		// Remove people/departments already selected from the unselected list
		for (Item item : selectedList) {
			unselectedList.remove(item);
		}
		Collections.sort(unselectedList);
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * Strings are all message-ids, which will be looked up in the message
	 * resource file. This call does NOT specify a message text -- it should be
	 * set via the setMessage() call. This invocation is designed for cases in
	 * which the message text is not just a constant text.
	 *
	 * @param pholder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param u The Unit that may be used to restrict the list of Contacts
	 *            displayed. This may be null if the project does not have
	 *            multiple units, or if unit selection is not appropriate for
	 *            this particular report or other activity.
	 * @param titleId The message-id of the title for the dialog box.
	 */
	public void show(int act, SelectContactsHolder pholder, Unit u, String titleId) {
		log.debug("action=" + act);
		try {
			holder = pholder;
			unit = u;
			setAction(act);
			setTitle(MsgUtils.getMessage(titleId));
			//setButtonOkLabel(MsgUtils.getMessage(buttonOkId));
			//setButtonCancelLabel(MsgUtils.getMessage(buttonCancelId));
			createLists();
			setVisible(true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Display the dialog to select Approvers for the various lists of Approval Path.
	 * User can select Approvers from the list of given Approvers.
	 * @param act Action Id
	 * @param pholder The object which is "calling" us, and will get the
	 *        callbacks.
	 * @param titleId The message-id of the title for the dialog box.
	 * @param approverGroup true, if popup is for approver group.
	 */
	public void show(int act, SelectContactsHolder pholder, String titleId, boolean approverGroup, boolean productionApprover) {
		log.debug("action=" + act);
		try {
			holder = pholder;
			setAction(act);
			setTitle(MsgUtils.getMessage(titleId));
			setIsApproverGroup(approverGroup);
			setIsProductionApprover(productionApprover);
			setInputError(false);
			setErrorMessage(null);
			//setButtonOkLabel(MsgUtils.getMessage(buttonOkId));
			//setButtonCancelLabel(MsgUtils.getMessage(buttonCancelId));
			createLists();
			setVisible(true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * The Action method for one of the line-item Add buttons (e.g., "+" icon),
	 * indicating the given item should be moved from the unselected list to the
	 * selected list. Our 'addId' field will have been set already by an
	 * f:setPropertyActionListener to the id of the selected Item.
	 *
	 * @return Navigation string, which will be empty since there is no page
	 *         transition for this action.
	 */
	public String actionAdd() {
		log.debug(addId);
		if (addId != null) {
			Item temp = new Item(addId);
			int ix = unselectedList.indexOf(temp);
			if (ix >= 0) {
				temp = unselectedList.get(ix);
				unselectedList.remove(ix);
				selectedList.add(temp);
				Collections.sort(selectedList);
			}
		}
		return null;
	}

	/**
	 * The Action method for one of the line-item Remove buttons (e.g., "X" icon),
	 * indicating the given item should be moved from the selected list to the
	 * unselected list. Our 'removeId' field will have been set already by an
	 * f:setPropertyActionListener to the id of the selected Contact.
	 *
	 * @return Navigation string, which will be empty since there is no page
	 *         transition for this action.
	 */
	public String actionRemove() {
		log.debug(removeId);
		if (removeId != null) {
			Item temp = new Item(removeId);
			int ix = selectedList.indexOf(temp);
			if (ix >= 0) {
				temp = selectedList.get(ix);
				selectedList.remove(ix);
				unselectedList.add(temp);
				Collections.sort(unselectedList);
			}
		}
		return null;
	}

	/**
	 * Action method of the "Send" (or other "Ok"-type) button on the pop-up.
	 * Closes the popup (by setting the "visible" flag to false), and call our
	 * "holder" contactsSelected() method to process the request.
	 *
	 * @return Navigation string, which will be empty since there is no page
	 *         transition for this action.
	 */
	@Override
	public String actionOk() {
 		try {
			unselectedList.clear();
			if (holder != null) {
				log.debug("action=" + getAction() + ", # of contacts=" + selectedList.size());
				Set<Contact> contacts = new HashSet<>();
				Set<Integer> depts = new HashSet<>();
				ContactDAO contactDAO = ContactDAO.getInstance();
				for (Item item : selectedList) {
					Integer id = item.getId();
					if (id > 0) {
						contacts.add(contactDAO.findById(id));
					}
					else {
						depts.add(-id);
					}
				}
				contacts.addAll(contactDAO.findByDepartmentIds
						(depts, SessionUtils.getProduction(), SessionUtils.getCurrentProject()));
				holder.contactsSelected(getAction(), contacts);
				if (! getInputError()) {
					holder = null;
					selectedList.clear();
				}
			}
			if (! getInputError()) {
				setVisible(false);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The Action method of the Cancel button.
	 *
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 * @return Navigation string, which will be empty since there is no page
	 *         transition for this action.
	 */
	@Override
	public String actionCancel() {
		setVisible(false);
		unselectedList.clear(); // free up all references
		selectedList.clear();
		if (holder != null) {
			holder.contactsSelected(getAction(), null);
			holder = null;
		}
//		ListView.addClientResize();
		return null;
	}

	/**
	 * ChangeListener method for the checkbox that controls showing either all
	 * Contact`s or only Contact`s associated with the current project.
	 *
	 * @param event The ValueChangeEvent created by the framework; its newValue
	 *            property will be the new boolean setting.
	 */
	public void listenChangeProjectOnly(ValueChangeEvent event) {
		try {
			projectOnly = (Boolean)event.getNewValue();
			refreshList();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener method for the 'restrict contacts by' type
	 * drop-down (project or unit).
	 */
	public void listenChangeOnlyOption(ValueChangeEvent evt) {
		log.debug(evt.getNewValue());
		onlyOption = (String)evt.getNewValue();
		refreshList();
	}

	/**
	 * Create an entry for the display list of Item`s from the given Contact.
	 * The Item.selected flag is set to true, which is what the JSP uses to
	 * distinguish between Item entries representing Contacts versus those
	 * representing Departments.
	 *
	 * @param contact The Contact whose information will be displayed.
	 * @return A new Item instance referencing the given Contact.
	 */
	private Item makeItem(Contact contact) {
		Item item = new Item(contact.getId(), contact.getUser().getLastNameFirstName());
		item.setKey(KEY_CONTACT + item.getName());
		item.setSelected(true);
		return item;
	}

	/**
	 * Create an entry for the display list of Item`s from the given Department.
	 * Note that the Item.id field is set to the negative value of the
	 * Department.id. This is to eliminate the possibility of there being an
	 * Item.id for a Contact entry matching an Item.id for a department entry.
	 *
	 * @param dept The Department whose information will be displayed.
	 * @return A new Item instance referencing the given Department.
	 */
	private Item makeItem(Department dept) {
		Item item = new Item(-dept.getId(), dept.getName());
		item.setKey(KEY_DEPT + item.getName());
		return item;
	}

	// * * * accessors & mutators * * *

	/** See {@link #holder}. */
	public SelectContactsHolder getHolder() {
		return holder;
	}
	/** See {@link #holder}. */
	public void setHolder(SelectContactsHolder holder) {
		this.holder = holder;
	}

	/** See {@link #selectedList}. */
	public List<Item> getSelectedList() {
		return selectedList;
	}
	/** See {@link #selectedList}. */
	public void setSelectedList(List<Item> list) {
		selectedList = list;
	}

	/** See {@link #unselectedList}. */
	public List<Item> getUnselectedList() {
		return unselectedList;
	}
	/** See {@link #unselectedList}. */
	public void setUnselectedList(List<Item> list) {
		unselectedList = list;
	}

	/** See {@link #projectOnly}. */
	public boolean getProjectOnly() {
		return projectOnly;
	}
	/** See {@link #projectOnly}. */
	public void setProjectOnly(boolean projectOnly) {
		this.projectOnly = projectOnly;
	}

	/** See {@link #onlyOption}. */
	public String getOnlyOption() {
		return onlyOption;
	}
	/** See {@link #onlyOption}. */
	public void setOnlyOption(String onlyOption) {
		this.onlyOption = onlyOption;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/** See {@link #addId}. */
	public Integer getAddId() {
		return addId;
	}
	/** See {@link #addId}. */
	public void setAddId(Integer addedInterest) {
		addId = addedInterest;
	}

	/** See {@link #removeId}. */
	public Integer getRemoveId() {
		return removeId;
	}
	/** See {@link #removeId}. */
	public void setRemoveId(Integer removeId) {
		this.removeId = removeId;
	}

	/** See {@link #isApproverGroup}. */
	public boolean getIsApproverGroup() {
		return isApproverGroup;
	}
	/** See {@link #isApproverGroup}. */
	public void setIsApproverGroup(boolean isApproverGroup) {
		this.isApproverGroup = isApproverGroup;
	}

	/** See {@link #isApproverGroup}. */
	public boolean getIsProductionApprover() {
		return isProductionApprover;
	}
	/** See {@link #isApproverGroup}. */
	public void setIsProductionApprover(boolean isProductionApprover) {
		this.isProductionApprover = isProductionApprover;
	}

	/**
	 * Redisplay the popup with an error message. This method is useful to call
	 * from a holder's "confirmOk" method when an error is found in the input,
	 * and the program wishes to keep the pop-up displayed with an error
	 * message, to give the user an opportunity to correct the error.
	 *
	 * @param message The error message text to be displayed within the popup.
	 */
	public void displayError(String message) {
		setVisible(true);
		setErrorMessage(message);
		setInputError(true);
	}

	/** See {@link #errorMessage}. */
	public String getErrorMessage() {
		return errorMessage;
	}
	/** See {@link #errorMessage}. */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/** See {@link #inputError}. */
	public boolean getInputError() {
		return inputError;
	}
	/** See {@link #inputError}. */
	public void setInputError(boolean error) {
		inputError = error;
	}

}
