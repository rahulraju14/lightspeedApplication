package com.lightspeedeps.web.user;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.model.table.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.UserStatus;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.view.ListImageView;

/**
 * The backing bean for the (Admin) User List/View/Edit page.
 */
@ManagedBean
@ViewScoped
public class UserListBean extends ListImageView implements Serializable {
	/** */
	private static final long serialVersionUID = 6168559493951180934L;

	private static final Log log = LogFactory.getLog(UserListBean.class);

	private static final int TAB_DETAIL = 0;
	//private static final int TAB_IMAGES = 1;

	/* Fields */

	/** The currently selected category, or "All" */
	private String category;

	/** The full (un-filtered) list of User`s to be displayed. */
	private List<User> userList;

//	/** The filtered list of User`s to display. */
//	private List<User> filteredList;

	/** The drop-down selection list for type (or "All"). */
	private List<SelectItem> userStatusDL;

	/** The currently viewed/selected User. */
	private User user;

	/** The filter text used to limit the list of Productions displayed.  Only
	 * available if the (un-filtered) list is long enough to cause pagination. */
	protected String filter;

	/** SelectItem's for the radio buttons for "primary phone" selection.  Note that
	 * the 'value' fields must be Integer, not String, to match the class of Contact.primaryPhoneIndex*/
	private static final SelectItem[] phoneItems = {
		new SelectItem(new Integer(0)," "),
		new SelectItem(new Integer(1)," "),
		new SelectItem(new Integer(2)," ") };

	private boolean primaryPhone[] = new boolean[3];

	/** List of productions to which the currently selected user. */
	private List<Production> productionList;

	/** List of all the currently selected user's timecards, across all productions. */
	private List<WeeklyTimecard> timecardList;

	/** The date/time that the currently selected user last logged into the application.*/
	private Date lastLogin;

	private transient UserDAO userDAO;

	/** The list of lazy loaded users, to be shown on the UI to get the specified number
	 * of users according to current page */
	private LazyDataModel<User> testUserList;

	/** True, if the user jumps to the Users page from Change List */
	private boolean isJumpEnabled = false;

	/** String literal used to hold the Email Address of the clicked user from the commandlink on Change List */
	private String userEmailAddr = null;

	/** Constructor */
	public UserListBean() {
		super(User.SORTKEY_NAME, "User.");
		log.debug("Init");
		try {
			filter = "";
			userStatusDL = new ArrayList<>( EnumList.getUserStatusList() );
			userStatusDL.add(0, Constants.GENERIC_ALL_ITEM);

			category = SessionUtils.getString(Constants.ATTR_USER_CATEGORY, Constants.CATEGORY_ALL);
			changeCategory(getCategory(), false);

			initUserList();

			Integer id = null;
			String account = SessionUtils.getString(Constants.ATTR_USER_LIST_ACCOUNT);
			if (account != null) {
				SessionUtils.put(Constants.ATTR_USER_LIST_ACCOUNT, null); // only use it once
				User owner = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, account);
				if (owner != null) {
					id = owner.getId();
				}
			}
			if (id == null) {
				id = SessionUtils.getInteger(Constants.ATTR_USER_LIST_ID);
				if (id != null) {
					isJumpEnabled = true; // set a boolean true
				}
			}
			setupSelectedItem(id);
			if (isJumpEnabled) { // if true add the filter value
				userEmailAddr = user.getEmailAddress();
			}
			scrollToRow();
			checkTab(); // restore last mini-tab in use
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (user != null) {
			user.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_USER_LIST_ID, null);
			user = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			user = getUserDAO().findById(id);
			if (user == null) {
				id = findDefaultId();
				if (id != null) {
					user = getUserDAO().findById(id);
				}
			}
			if ( ! getCategory().equals(Constants.CATEGORY_ALL) &&
					! getCategory().equals(user.getStatus().name())) {
				changeCategory(user.getStatus().name(), false);
			}
			SessionUtils.put(Constants.ATTR_USER_LIST_ID, id);
		}
		else {
			log.debug("new User");
			SessionUtils.put(Constants.ATTR_USER_LIST_ID, null); // erase "new" flag
			user = new User();
			user.setStatus(UserStatus.PENDING);
		}
		resetImages();
		productionList = null;
		timecardList = null;
		lastLogin = null;
		if (user != null) {
			user.setSelected(true);
			user.initAddresses();
			primaryPhone[0] = user.getPrimaryPhoneIndex() == 0;
			primaryPhone[1] = user.getPrimaryPhoneIndex() == 1;
			primaryPhone[2] = user.getPrimaryPhoneIndex() == 2;

			getStateMap().clear(); // clear ICEfaces selections
			selectRowState(user);  // set new ICEfaces selection

			forceLazyInit();
		}
	}

	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<User> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	private void forceLazyInit() {
		@SuppressWarnings("unused")
		int i = getElement().getImages().size();
		if (user.getHomeAddress() != null) {
			user.getHomeAddress().getAddrLine1();
		}
		if (user.getMailingAddress() != null) {
			user.getMailingAddress().getAddrLine1();
		}
		if (user.getLoanOutAddress() != null) {
			user.getLoanOutAddress().getAddrLine1();
		}
		// LS-3578
		if (user.getLoanOutMailingAddress() != null) {
			user.getLoanOutMailingAddress().getAddrLine1();
		}
		if (user.getAgencyAddress() != null) {
			user.getAgencyAddress().getAddrLine1();
		}

	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((User)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * The Action method of the "Edit" button on the User List page.
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		try {
			super.actionEdit();
			user.initAddresses();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Save User -Action method of "Save" button
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		if (! UserUtils.checkFields(user) ) { // update/clean/test various fields before saving
			setSelectedTab(TAB_DETAIL);
			return null;
		}
		try {
			getElement().setEmailAddress(getElement().getEmailAddress().trim());
			user.setDisplayName(user.getFullName());
			if (primaryPhone[0]) {
				user.setPrimaryPhoneIndex(0);
			}
			else if (primaryPhone[1]) {
				user.setPrimaryPhoneIndex(1);
			}
			else if (primaryPhone[2]) {
				user.setPrimaryPhoneIndex(2);
			}
			commitImages();
			if (!newEntry) {
				user = getUserDAO().update(user, getAddedImages());
				updateItemInList(user);
				user.setSelected(true);
				forceLazyInit();
			}
			else {
				user = getUserDAO().update(user, getAddedImages());
				refreshList();
				scrollToRow();
			}
			SessionUtils.put(Constants.ATTR_USER_LIST_ID, user.getId());
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
		super.actionCancel();
		if (getElement() == null || isNewEntry()) {
			user = null;
			changeCategory(getCategory(), true); // select 1st item in current list
			//setupSelectedItem(null);
		}
		else {
			user = getUserDAO().refresh(user);
			setupSelectedItem(getElement().getId());
		}
		return null;
	}

	/**
	 * Delete selected User and all related Contact objects. Note that the
	 * objects are not removed from the database, but merely changed to a
	 * Deleted status.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionDeleteOk()
	 */
	@Override
	public String actionDeleteOk() {
		try {
			getUserDAO().remove(user);
			SessionUtils.put(Constants.ATTR_USER_LIST_ID, null);
			userList.remove(user);
			DoNotification.getInstance().userDeleted(user, false);
			user = null;
			setupSelectedItem(getRowId(getSelectedRow()));
			//addClientResize();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("User.DeleteFailed", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * @return The list of images owned by the current element.  This is used
	 * by the ImagePaginatorBean and ImageAddBean when adding and removing
	 * elements.
	 */
	@Override
	public List<Image> getImageList() {
		return getElement().getImages();
	}

	/**
	 * The Value Change Listener for the category (element type) selection
	 * drop-down list on the element list (left-hand side) display.
	 * @param evt
	 */
	public void selectedCategory(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				changeCategory( (String)evt.getNewValue(), ! editMode);
			}
			else {
				log.warn("Null newValue in category change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Set a new category (or "All") as the type of item listed. This regenerates the list of
	 * elements listed. If the currently selected item is not in the new list, then select the first
	 * entry of the new list.
	 *
	 * @param type A UserStatus value, or "All".
	 */
	private void changeCategory(String type, boolean pickElement) {
		if (getElement() != null && ! editMode) {
			getElement().setSelected(false); // we may end up switching
		}
		SessionUtils.put(Constants.ATTR_USER_CATEGORY, type);
		category = type;
		if (pickElement) { // possibly select an element to view
			userList = null;
			@SuppressWarnings("unchecked")
			List<User> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(user);
				if (ix < 0) {
					log.debug(type + ", " + user.getStatus());
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					user = list.get(ix);
					getElement().setSelected(true);
					forceLazyInit(); // refresh referenced data
				}
			}
			else {
				// no current element & not doing "Add"; if list has entries, view the first
				if (list.size() > 0) {
					setupSelectedItem(list.get(0).getId());
				}
			}
		}
		//addClientResize();
	}

	@Override
	protected void refreshList() {
		changeCategory(getCategory(), true);
	}

	/**
	 * If no item is currently selected, find the current element in
	 * the main (left-hand) list, select it, and send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	private void scrollToRow() {
		scrollToRow(getElement());
	}

	/**
	 * Return the id of the item that resides in the n'th row of the
	 * currently displayed list.
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {
		Object item;
		return ((item=getRowItem(row)) == null ? null : ((User)item).getId());
	}

	@Override
	protected Comparator<User> getComparator() {
		Comparator<User> comparator = new Comparator<User>() {
			@Override
			public int compare(User c1, User c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * Get a List of all the Production`s that the currently selected user
	 * belongs to (is a member of).
	 *
	 * @return A non-null, but possibly empty, List as described above.
	 */
	private List<Production> createProductionList() {
		List<Production> list = ProductionDAO.getInstance().findByUser(getElement());
		return list;
	}

	/**
	 * Get a List of all the WeeklyTimecard`s owned by the selected user, across
	 * all Production`s.
	 *
	 * @return A non-null, but possibly empty, list of WeeklyTimecard`s.
	 */
	private List<WeeklyTimecard> createTimecardList() {
		List<WeeklyTimecard> list = WeeklyTimecardDAO.getInstance()
				.findByUserAccount(null, null, user.getAccountNumber(), null);
		for (WeeklyTimecard wtc : list) {
			TimecardCalc.calculateWeeklyTotals(wtc);
		}
		return list;
	}

	/**
	 * Creates the list of User status values available in edit mode, which is
	 * only "All", or the type of the currently selected user. That way,
	 * whichever one is chosen, the currently selected user will still be
	 * included in the list.
	 *
	 * @return List of status drop-down SelectItem's consisting of "All" plus
	 *         the currently selected item's status.
	 */
	private List<SelectItem> createEditTypeDL() {
		List<SelectItem> list = new ArrayList<>();
		list.add(0, Constants.GENERIC_ALL_ITEM);
		if (getElement() != null) {
			list.add(1,new SelectItem(getElement().getStatus().name(),getElement().getStatus().getLabel()));
		}
		return list;
	}

	@SuppressWarnings("serial")
	private void initUserList() {

		testUserList = new LazyDataModel<User>() {

			@Override
			public List<User> load(int first, int pageSize, final SortCriteria[] criteria,
					final Map<String, String> filters) {
				UserStatus status = null;
				if (category.equals(Constants.CATEGORY_ALL)) {
					status = UserStatus.DELETED;
				}
				else {
					status = UserStatus.valueOf(category);
				}
				String firstName = filters.get("firstName");
				String lastName = filters.get("lastName");
				String emailAddress = filters.get("emailAddress");

				long count = getUserDAO().findCountByFilter("firstName", firstName, "lastName",
						lastName, "emailAddress", emailAddress, status);

				setRowCount((int)count);

				userList = new ArrayList<>(getUserDAO().findByFilter("firstName", firstName, "lastName", lastName, "emailAddress", emailAddress, status, pageSize, first));
//				for (User usr : userList) {
//					log.debug("user name retrieved>>>>>> " + usr.getFirstNameLastName());
//				}
				return userList;
			}
		};
	}

	/** See {@link #user}. */
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User usr) {
		user = usr;
	}

	/** See {@link #filter}. */
	public String getFilter() {
		return filter;
	}
	/** See {@link #filter}. */
	public void setFilter(String filter) {
		this.filter = filter;
	}

	/** See {@link #user}. */
	public User getElement() {
		return user;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return user.getDisplayName();
	}

	/** See {@link #userList}. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		if (userList == null) {
			Map<String, String> filters = new HashMap<>();
			getTestUserList().load(0, 15, null, filters);
		}
		return userList;
//		return filteredList;
	}

	public String getStatus() {
		return (getElement().getStatus() == null ? "" : getElement().getStatus().name());
	}
	public void setStatus(String typeStr) {
		getElement().setStatus(UserStatus.valueOf(typeStr));
	}

	/** See {@link #phoneItems}. */
	public SelectItem[] getPhoneItems() {
		return phoneItems;
	}

	/** See {@link #primaryPhone}. */
	public boolean[] getPrimaryPhone() {
		return primaryPhone;
	}
	/** See {@link #primaryPhone}. */
	public void setPrimaryPhone(boolean[] primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public List<SelectItem> getUserStatusDL() {
		if (editMode) {
			return createEditTypeDL();
		}
		return userStatusDL;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	/** See {@link #lastLogin}. */
	public Date getLastLogin() {
		if (lastLogin == null) {
			lastLogin = EventDAO.getInstance().findLastLoginByUser(user);
		}
		return lastLogin;
	}
	/** See {@link #lastLogin}. */
	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	/** See {@link #timecardList}. */
	public List<WeeklyTimecard> getTimecardList() {
		if (timecardList == null) {
			timecardList = createTimecardList();
		}
		return timecardList;
	}
	/** See {@link #timecardList}. */
	public void setTimecardList(List<WeeklyTimecard> timecardList) {
		this.timecardList = timecardList;
	}

	/** See {@link #productionList}. */
	public List<Production> getProductionList() {
		if (productionList == null) {
			productionList = createProductionList();
		}
		return productionList;
	}
	/** See {@link #productionList}. */
	public void setProductionList(List<Production> productions) {
		productionList = productions;
	}

	private UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

	/**See {@link #testUserList}. */
	public LazyDataModel<User> getTestUserList() {
		return testUserList;
	}
	/**See {@link #testUserList}. */
	public void setTestUserList(LazyDataModel<User> testUserList) {
		this.testUserList = testUserList;
	}

	/**See {@link #userEmailAddr}. */
	public String getUserEmailAddr() {
		return userEmailAddr;
	}
	/**See {@link #userEmailAddr}. */
	public void setUserEmailAddr(String userEmailAddr) {
		this.userEmailAddr = userEmailAddr;
	}

}
