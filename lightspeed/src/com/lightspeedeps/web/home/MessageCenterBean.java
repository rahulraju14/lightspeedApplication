package com.lightspeedeps.web.home;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.MessageInstanceDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.MessageInstance;
import com.lightspeedeps.type.NotificationMethod;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for "Notifications" mini-tabs on the Home page. NOTE that code
 * is tied to the fact that the left (first) tab is "My Notifications" and the
 * second tab is "All Notifications".
 * <p>
 * This bean actually includes (via its superclass HomePageBean) the code to
 * manage the left side of the Home page and its "Status" mini-tab.
 */
@ManagedBean
@ViewScoped
public class MessageCenterBean extends HomePageBean implements Serializable {
	/** */
	private static final long serialVersionUID = 8411519004303489863L;

	private static final Log log = LogFactory.getLog(MessageCenterBean.class);

	private static final String SORT_DATETIME = "date/time";
	private static final String SORT_TO = "to"; // sort by recipient, for "All notifications"

	private String msgBody = null;
	private Integer targetMsgId = -1;
	private MessageInstance messageInstance;
	private List<MessageInstance> myMsgInstList;
	private List<MessageInstance> allMsgInstList;
	private String selectedGroup = "All";

	private transient MessageInstanceDAO msgInstanceDAO;

	public MessageCenterBean() {
		super(SORT_DATETIME, "MessageCenter.");
		log.debug("");
		checkTab(); // restore last selected mini-tab
	}

	/**
	 * Called (via jsp) when the user clicks on the Delete icon associated
	 * with a message.  Removes the message instance from the database.
	 */
	@Override
	protected String actionDeleteOk() {
		log.debug("");
		try {
//			addClientResize();
			getMessageInstanceDAO().remove(messageInstance);
			// selectedRow was set via f:setPropertyActionListener when user clicked a msg
			int ix = getSelectedRow();
			setMessageInstance(null);
			setAllMsgInstList(null);
			setMyMsgInstList(null);
			getCurrentList();	// refresh list
			sortIfNeeded();
			showPriorItem(ix);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * ActionListener method on the dataPaginators for the My Notifications
	 * and All Notifications lists.  We just need to push the resize()
	 * script call so the display containers will get sized properly.
	 * @param evt
	 */
	public void actionPagination(ActionEvent evt) {
//		addClientResize();
	}

	/**
	 * The actionListener method for individual message instance lines in the
	 * list. Shows the body of a selected MessageInstance, and marks it as
	 * Acknowledged if it was not already.
	 */
	public void showMessage(ActionEvent evt) {
		log.debug("showMessage");
		try {
			MessageInstance miIn = (MessageInstance) (evt.getComponent().getAttributes().get("msgInstance"));
			MessageInstance mi = getMessageInstanceDAO().findById(miIn.getId());
			if (mi == null) {
				// unusual -- probably another user deleted the message
				int ix = getItemList().indexOf(miIn); // find the original in our list
				if (ix >= 0) {
					getItemList().remove(ix);
				}
				showPriorItem(ix+1);
			}
			else {
				showMessage(mi);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Set up to display the specified message instance in the message area of
	 * the page. Also, if the current user is the message recipient, mark the
	 * message as "acknowledged".
	 *
	 * @param instance The MessageInstance to be displayed.
	 */
	private void showMessage(MessageInstance instance) {
		log.debug("showMessage");
		try {
			if (messageInstance != null) {
				messageInstance = MessageInstanceDAO.getInstance().refresh(messageInstance);
				messageInstance.setSelected(false); // un-select prior message
				updateItemInList(messageInstance);
			}
			int ix = getCurrentList().indexOf(instance);
			if (ix >= 0) {
				setSelectedRow(ix);
			}
			setMessageInstance(instance);
			if (instance != null) {
				instance.setSelected(true);
				setMsgBody(StringUtils.saveHtml(instance.getMessage().getBody()));
				if (instance.getAcknowledged() == 0 &&
						instance.getContact().getId().equals(contactId)) {
					instance.setAcknowledged(Constants.TRUE);
					instance.setAcknowledgedTime(new Date());
					getMessageInstanceDAO().attachDirty(instance);
					// replace instance in list with updated instance:
					updateItemInList(instance);
					// force refresh of home page list, since msg will be acknowledged...
					setNotificationList(null);
				}
				forceLazyInit();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Attempt to select and display the item prior to the "ix"th entry in our
	 * current list.
	 *
	 * @param ix The (zero-origin) index of an entry in the message list; this
	 *            method will attempt to display the one before this. If ix==0,
	 *            then it will display the zero-th entry, if that exists.
	 */
	private void showPriorItem(int ix) {
		List<MessageInstance> msgs = getCurrentList();
		MessageInstance mi = null;
		if (ix > 0) {
			ix--;	// aim to display prior message
		}
		if (ix >= msgs.size()) {
			ix = msgs.size() - 1;
		}
		if (ix >= 0) {
			mi = msgs.get(ix);
			if (mi != null) {
				showMessage(mi);
			}
		}
	}

	/**
	 * Display the MessageInstance identified by the given database id.
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(java.lang.Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		MessageInstance mi = getMessageInstanceDAO().findById(id);
		showMessage(mi);
	}

	/**
	 * Ensure any fields displayed for the current MessageInstance
	 * have been retrieved from the database.
	 */
	private void forceLazyInit() {
		if (messageInstance.getContact() != null) {
			messageInstance.getContact().getUser().getLastNameFirstName(); // force load of data
		}
		if (messageInstance.getMessage() != null &&
				messageInstance.getMessage().getNotification() != null) {
			messageInstance.getMessage().getNotification().getDate();
		}
	}

	/**
	 * User has clicked one of the radio buttons to select the types of messages
	 * to be displayed: Acknowledged, Unacknowledged, or All (both).
	 * @param event The Faces event created by the radio click.
	 */
/*	public void radioSelect(ValueChangeEvent event) {
		log.debug("");

		try {
			String radioVal = event.getNewValue().toString();
			if (radioVal.equalsIgnoreCase("All")) {
				setSelectedGroup("All");
			}
			else if (radioVal.equalsIgnoreCase("Acknowledged")) {
				setSelectedGroup("Acknowledged");
			}
			else {
				setSelectedGroup("UnAcknowledged");
			}
			getMessageData();

		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}
*/

	/**
	 * Retrieve the List of MessageInstances for the "My Notifications" tab.
	 */
	private void findMyMessageData() {
		log.debug("");
		setAllMsgInstList(null); // force refresh if return to "All Notifications" tab
		try {
			Contact contact = SessionUtils.getCurrentContact();
			// refer to rev 1029 or earlier for code to get unacknowledged msgs
			setMyMsgInstList(getMessageInstanceDAO().findByContactNotificationMethod(contact, NotificationMethod.WEB));
			forceSort(); // force sort
			clearSelect();
		}
		catch (Exception e) {
			setMyMsgInstList(new ArrayList<MessageInstance>()); // prevent multiple duplicate errors
			EventUtils.logError(e);
		}
	}

	/**
	 * Retrieve the List of MessageInstances for the "All Notifications" tab.
	 */
	private void findAllMessageData() {
		log.debug("");
		setMyMsgInstList(null); // force refresh if return to "My Notifications" tab
		try {
			setAllMsgInstList(getMessageInstanceDAO().findBySentViaAndProduction(NotificationMethod.WEB));
			log.debug(allMsgInstList.size());
			forceSort(); // force sort before display
			clearSelect();
			if (getSelectedTab() == ALL_MSGS_TAB) {
				for (MessageInstance mi : allMsgInstList) {
					if (mi.getContact() != null) {
						mi.getContact().getUser().getLastNameFirstName(); // force load of data
					}
				}

			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	@Override
	protected void setupTabs() {
		setSelectedRow(-1);
		setAllMsgInstList(null); // force refresh
		setMyMsgInstList(null);
		if (getSelectedTab() == MY_MSGS_TAB && targetMsgId >= 0) {
			// This handles jump from Home "Status" tab to My Notifications when the
			// user clicks on one of the "recent notifications" links.
			getItemList();	// fill our list first
			setSelectedRow(-1);
			MessageInstance instance = getMessageInstanceDAO().findById(targetMsgId);
			targetMsgId = -1;
			if (instance != null) {
				showMessage(instance);
			}
		}
		if (getSelectedTab() == MY_MSGS_TAB && SORT_TO.equals(getSortColumnName())) {
			setSortColumnName(SORT_DATETIME);
			setAscending(isDefaultAscending(SORT_DATETIME));
		}
	}

	private void clearSelect() {
		for (MessageInstance mi : getCurrentList()) {
			mi.setSelected(false);
		}
	}

	@Override
	protected Comparator<MessageInstance> getComparator() {
		Comparator<MessageInstance> comparator = new Comparator<MessageInstance>() {
			@Override
			public int compare(MessageInstance mi1, MessageInstance mi2) {
				mi1 = getMessageInstanceDAO().refresh(mi1);
				mi2 = getMessageInstanceDAO().refresh(mi2);
				return mi1.compareTo(mi2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * Determines the default ascending/descending order for the given column name.
	 *
	 * @param sortColumn Name of the column for which the default order is requested.
	 * @return whether sortColumn's default order is ascending or descending.
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (SORT_DATETIME.equals(sortColumn)) {
			return false;
		}
		return true;	// all columns default to ascending
	}

	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((MessageInstance)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Get the message instance whose text body is to be displayed below the
	 * list of messages.
	 */
	public MessageInstance getMessageInstance() {
		return messageInstance;
	}
	public void setMessageInstance(MessageInstance messageInstance) {
		this.messageInstance = messageInstance;
	}

	public String getMsgBody() {
		return msgBody;
	}
	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}

	/** See {@link #targetMsgId}. */
	public Integer getTargetMsgId() {
		return targetMsgId;
	}
	/** See {@link #targetMsgId}. */
	public void setTargetMsgId(Integer targetMsgId) {
		this.targetMsgId = targetMsgId;
	}

	protected List<MessageInstance> getMyMsgInstList() {
		if (myMsgInstList == null) {
			findMyMessageData();
		}
		return myMsgInstList;
	}
	protected void setMyMsgInstList(List<MessageInstance> myMsgInstList) {
		this.myMsgInstList = myMsgInstList;
	}

	protected List<MessageInstance> getAllMsgInstList() {
		if (allMsgInstList == null) {
			findAllMessageData();
		}
		return allMsgInstList;
	}
	protected void setAllMsgInstList(List<MessageInstance> allMsgInstList) {
		this.allMsgInstList = allMsgInstList;
	}

	@SuppressWarnings("unchecked")
	private List<MessageInstance> getCurrentList() {
		return getItemList();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List getItemList() {
		if (getSelectedTab() == MY_MSGS_TAB) {
			return getMyMsgInstList();
		}
		else if (getSelectedTab() == ALL_MSGS_TAB) {
			return getAllMsgInstList();
		}
		//log.debug("other tab active");
		return null;
	}

	public String getSelectedGroup() {
		return selectedGroup;
	}
	public void setSelectedGroup(String selectedGroup) {
		this.selectedGroup = selectedGroup;
	}

	private MessageInstanceDAO getMessageInstanceDAO() {
		if (msgInstanceDAO == null) {
			msgInstanceDAO = MessageInstanceDAO.getInstance();
		}
		return msgInstanceDAO;
	}

}
