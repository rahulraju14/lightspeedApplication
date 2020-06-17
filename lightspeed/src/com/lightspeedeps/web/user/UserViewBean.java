package com.lightspeedeps.web.user;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlInputText;
import javax.faces.event.*;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.AddressInformation;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.util.*;
import com.lightspeedeps.web.view.ImageView;

/**
 * Backing bean for the My Account page.
 *<p>
 * The Delete action is actually invoked from the user/delete.jsp page, which
 * is reached via a link on the My Account page.
 *<p>
 * Some methods of the "ImageHolder" interface are implemented here to support
 * functions on the Image tab.
 */
@ManagedBean
@ViewScoped
public class UserViewBean extends ImageView implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 554082981281364705L;

	private static final Log LOG = LogFactory.getLog(UserViewBean.class);

	private static final int ACT_DELETE_USER = 11;

	private static final int TAB_DETAIL = 0;

	/** LS-3412 Alien Authorized to Work code */
	private static final String ALIEN_AUTH_CODE = "A";

	private User user;

	/** QST/GST Info */
	public static final String QST_GST_NOT_REGISTERED ="Not Registered";

	/** True if the "change PIN" dialog should be displayed. */
	private boolean showChangePin;

	private boolean hidden;

	/** Save the SSN at start of Edit, to see if it has changed when the
	 * user Saves.  LS-2510 */
	private String oldSsn;

	/** QST checkbox for User view  */
	private boolean qstChkbox= false;

	/** GST checkbox for User view  */
	private boolean gstChkbox= false;

	/** State/Province list for user Address */
	private List<SelectItem> userStateProvinceList;

	/** State/Province list for Agent Address */
	private List<SelectItem> agencyStateProvinceList;

	/** Selected Talent Agent */
	private Agent selectedAgent;

	/** used to save copy of home address prior to edit. LS-4382 */
	private Address saveHomeAddress;

	/** used to save copy of mailing address prior to edit. LS-4382 */
	private Address saveMailingAddress;

	/** used to save copy of loan-out permanent address prior to edit. LS-4382 */
	private Address saveLoanOutAddress;

	/** used to save copy of loan-out mailing address prior to edit. LS-4382 */
	private Address saveLoanOutMailingAddress;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	private transient AgentDAO agentDAO;
	private transient UserDAO userDAO;

	/** SelectItem's for the radio buttons for "primary phone" selection. Note that
	 * the 'value' fields must be Integer, not String, to match the class of Contact.primaryPhoneIndex*/
	private static final SelectItem[] PHONE_ITEMS = {
		new SelectItem(Integer.valueOf(0)," "),
		new SelectItem(Integer.valueOf(1)," "),
		new SelectItem(Integer.valueOf(2)," ")};

	/** SelectItem's for the radio buttons for "primary phone" selection. Note that
	 * the 'value' fields must be Integer, not String, to match the class of Contact.primaryPhoneIndex*/
	private static final SelectItem[] CANADA_PHONE_ITEMS = {
		new SelectItem(Integer.valueOf(1)," "),
		new SelectItem(Integer.valueOf(2)," ")};

	/** Default Constructor */
	public UserViewBean() {
		super("MyAccount.");
		LOG.debug("");
		setScrollable(true);
		try {
			user = SessionUtils.getCurrentUser();
			if (user == null) {
				// LS-1011 Happens if user logs out from My Account page
				user = new User(); // fake it
			}

			initView();
			initUserViewBean();
			isGstChecked();
			isQstChecked();
			checkTab(); // restore last mini-tab in use
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Initialize our fields based on the contact id we find in the Session.
	 */
	private void initView() {
		LOG.debug("");
		Integer id = user.getId();
		Integer selectedAgentId = null;

		setupSelectedItem(id);
		// Set up talent agents for Agents tab. Used for talent.
		// Look for the selected agent from the user agents list. If found use it's id to
		// pass to setupSelectedAgentItem.
		List<Agent> agents = user.getAgentsList();

		if(agents != null && !agents.isEmpty()) {
			for(Agent agent : agents) {
				if(agent.getSelected()) {
					selectedAgentId = agent.getId();
					break;
				}
			}
		}
		setupSelectedAgentItem(selectedAgentId);
	}

	/**
	 * Setup the specified id -- except that it should always be the current
	 * user! This code just follows the structure of other
	 * {@link com.lightspeedeps.web.view.ListImageView ListImageView}
	 * subclasses.
	 *
	 * @param id The database id of the User object to be displayed.
	 */
	protected void setupSelectedItem(Integer id) {
		setup();
		resetImages();
		user.setImageResources(null);
	}

	/**
	 * Setup the selected agent in the agents table LS-1869
	 * @param id
	 */
	private void setupSelectedAgentItem(Integer id) {
		if(selectedAgent != null) {
			selectedAgent.setSelected(false);
		}

		if(id != null) {
			selectedAgent = getAgentDAO().findById(id);
		}

		if(selectedAgent == null) {
			selectedAgent = new Agent();
			selectedAgent.setAgencyAddress(new Address(user.getShowCanada() && !user.getShowUS()));
		}

		// Reset the agents selected flag.
		List<Agent>agents  = user.getAgentsList();
		if(agents != null && !agents.isEmpty()) {
			for(Agent agent : user.getAgentsList()) {
				agent.setSelected(false);
			}
			selectedAgent.setSelected(true);
			int index = user.getAgentsList().indexOf(selectedAgent);
			user.getAgentsList().set(index, selectedAgent);

			user = getUserDAO().merge(user);

			// Set the selected row in the data table
			getStateMap().clear();
			RowState state = new RowState();
			state.setSelected(true);
			getStateMap().put(selectedAgent, state);
		}
	}

	/**
	 * We probably have a Contact object; initialize any fields that are
	 * necessary for display.
	 */
	private void setup() {
		user.initAddresses();
		forceLazyInit();
		setupTabs();
	}

	/**
	 * These fields are referenced on some tab (such as Role), and are not
	 * initialized when the Contact object is still in the session that obtained
	 * it.  We need to force Hibernate to load them while the Contact is
	 * still in the original session.  This applies to fields marked as Fetch=LAZY
	 * in the Contact object.
	 */
	private void forceLazyInit() {
		LOG.debug("");
		@SuppressWarnings("unused")
		String str;
		if (user.getHomeAddress() != null) {
			str = user.getHomeAddress().getAddrLine1();
		}
		if (user.getMailingAddress() != null) {
			str = user.getMailingAddress().getAddrLine1();
		}
		if (user.getLoanOutAddress() != null) {
			str = user.getLoanOutAddress().getAddrLine1();
		}
		// LS-3578
		if (user.getLoanOutMailingAddress() != null) {
			str = user.getLoanOutMailingAddress().getAddrLine1();
		}
		if (user.getAgencyAddress() != null) {
			str = user.getAgencyAddress().getAddrLine1();
		}
		if (user.getImages() != null) {
			for (Image image : user.getImages()) {
				str = image.getTitle();
			}
		}
		if(user.getAgentsList() != null && !user.getAgentsList().isEmpty()) {
			for(Agent agent : user.getAgentsList()) {
				if(agent.getAgencyAddress() != null) {
					agent.getAgencyAddress().getAddrLine1();
				}
			}
		}
	}

	/**
	 * The Action method of the "Edit" button on the User (account) page.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		LOG.debug("");
		try {
			user.initAddresses();
			oldSsn = user.getSocialSecurity();
			saveHomeAddress = user.getHomeAddress().clone();
			saveMailingAddress = user.getMailingAddress().clone();
			saveLoanOutAddress = user.getLoanOutAddress().clone();
			saveLoanOutMailingAddress = user.getLoanOutMailingAddress().clone();
			super.actionEdit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private void initUserViewBean() {
		String countryCode = user.getHomeAddress().getCountry();
		userStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL(countryCode);

		countryCode = user.getAgencyAddress().getCountry();
		agencyStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL(countryCode);
	}

	/**
	 * Action method for the Delete button on the "Delete account" page.
	 * This puts up the confirmation dialog box.
	 *
	 * @return null navigation string.
	 */
	public String actionDelete() {
		PopupBean.getInstance().show(
				this, ACT_DELETE_USER,
				"User.DeleteSelf.");
		//addClientResize();
		return null;
	}

	/**
	 * Action method for the Ok button on the Delete Account pop-up dialog. This
	 * does a "remove" of the user's account, which actually leaves it in the
	 * database, but with the email address changed and the status set to
	 * Deleted.
	 *
	 * @return null navigation string.
	 */
	public String actionDeleteOk() {
		try {
			if (user != null) {
				getUserDAO().remove(user);
				DoNotification.getInstance().userDeleted(user, true);
				user = null;
			}
			HeaderViewBean.logout();
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("User.DeleteFailed", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Delete the agent from the user's list of agents.
	 *
	 * @return
	 */
	public String actionDeleteAgent(ActionEvent evt) {
		UIData data= (UIData)evt.getComponent().findComponent("agentsTable");

		if(data != null) {
			Agent agent = (Agent)data.getRowData();

			user.getAgentsList().remove(agent);
			getAgentDAO().delete(agent);
			user = getUserDAO().merge(user);

			// If this agent is the selected agent in table,
			// clear the detail fields
			if(agent.getSelected()) {
				selectedAgent = new Agent();
			}

			LOG.debug("");
		}
		return null;
	}

	/**
	 *  Action method for the Save button.
	 *
	 * @return null navigation string.
	 */
	@Override
	public String actionSave() {
		try {
			if (! UserUtils.checkFields(user) ) { // update/clean/test various fields before saving
				setSelectedTab(TAB_DETAIL);
				return null;
			}
			checkAndLogAddressChange(); // Log any address changes. LS-4382
			user.setDisplayName(user.getFullName()); // include middle initial. LS-4744
			// LS-1745, changes for the last name
			if (user.getLastName() != null) {
				String name = DocumentService.checkSuffix(user.getLastName());
				user.setLastName(name);
			}
			commitImages();
			if (user.getSocialSecurity() != null) { // LS-2510 check for SSN change
				if (oldSsn == null || user.getSocialSecurity().compareTo(oldSsn) != 0) {
					user.setAgreeToTerms(false); // reset ESS validation flag if changed.
				}
			}
			else if (oldSsn != null) {
				user.setAgreeToTerms(false); // reset ESS validation flag if changed.
			}
			user = getUserDAO().merge(user); // LS-2502 Gender update; this fixes DuplicateKey error
			// refresh image resource list
			user.setImageResources(null);
			// Instantiate any of the address objects that are null.
			initView();

			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * If the user updated any address fields, generate one or two ActivityLog
	 * records. Separate records are generated for personal vs corporate
	 * addresses. LS-4382 LS-4574
	 */
	private void checkAndLogAddressChange() {
		try {
			boolean permAddrChange = ! saveHomeAddress.equalsAddressOrNull(user.getHomeAddress());
			boolean mailAddrChange = ! saveMailingAddress.equalsAddressOrNull(user.getMailingAddress());
			if (permAddrChange || mailAddrChange) {
				// create ActivityLog instance to record address change
				ActivityLog log = new ActivityLog(ActivityLog.TYPE_PERSONAL_ADDRESS_CHANGE, new Date());
				log.setUserAcct(user.getAccountNumber());
				log.setPermanentAddrChanged(permAddrChange); // LS-4574
				log.setMailingAddrChanged(mailAddrChange);
				ActivityLogDAO.getInstance().save(log);
			}
			permAddrChange = ! saveLoanOutAddress.equalsAddressOrNull(user.getLoanOutAddress());
			mailAddrChange = ! saveLoanOutMailingAddress.equalsAddressOrNull(user.getLoanOutMailingAddress());
			if (permAddrChange || mailAddrChange) {
				// create ActivityLog instance to record address change
				ActivityLog log = new ActivityLog(ActivityLog.TYPE_LOANOUT_ADDRESS_CHANGE, new Date());
				log.setUserAcct(user.getAccountNumber());
				log.setPermanentAddrChanged(permAddrChange); // LS-4574
				log.setMailingAddrChanged(mailAddrChange);
				ActivityLogDAO.getInstance().save(log);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public String actionSaveAgent() {
		selectedAgent.setDisplayName(selectedAgent.getFirstName() + " " + selectedAgent.getLastName());
//		selectedAgent.getUsersList().add(user);
		if(user.getAgentsList() == null) {
			user.setAgentsList(new ArrayList<Agent>());
		}
		if(selectedAgent.getId() == null) {
			getAgentDAO().save(selectedAgent);
			user.getAgentsList().add(selectedAgent);
		}
		else {
			selectedAgent = getAgentDAO().merge(selectedAgent);
		}

		user = getUserDAO().merge(user);
		setupSelectedAgentItem(selectedAgent.getId());

		return super.actionSave();
	}
	/**
	 * ValueChangeListener for GST Not Registered check box
	 * @param event
	 */
	public void listenGSTChange(ValueChangeEvent event) {
		if(event.getNewValue() != null && (boolean)event.getNewValue()) {
			HtmlInputText inputText1 = (HtmlInputText)event.getComponent().findComponent("gstNumber");
			inputText1.setValue(QST_GST_NOT_REGISTERED);
			user.setGstNumber(QST_GST_NOT_REGISTERED);
			inputText1.setDisabled(true);
			inputText1.setTransient(true);
		}
		else {
			HtmlInputText inputText1 = (HtmlInputText)event.getComponent().findComponent("gstNumber");
			inputText1.setValue("");
			inputText1.setTransient(true);
		 }
	}

	/**
	 * ValueChangeListener for QST Not Registered check box
	 * @param event
	 */
	public void listenQSTChange(ValueChangeEvent event) {
		if (event.getNewValue() != null && (boolean)event.getNewValue()) {
			HtmlInputText inputText2 = (HtmlInputText)event.getComponent().findComponent("qstNumber");
			inputText2.setValue(QST_GST_NOT_REGISTERED);
			user.setQstNumber(QST_GST_NOT_REGISTERED);
			inputText2.setDisabled(true);
			inputText2.setTransient(true);
		}
		else {
			HtmlInputText inputText2 = (HtmlInputText)event.getComponent().findComponent("qstNumber");
			inputText2.setValue("");
			inputText2.setTransient(true);
		}
	}

	/**
	 * Disable LLC type field and clear it if Limited liability company is not selected
	 * LS-2719
	 *
	 * @param event
	 */
	public void listenTaxClassificationChange(ValueChangeEvent event) {
		TaxClassificationType tc = (TaxClassificationType)event.getNewValue();
		HtmlInputText it = (HtmlInputText)event.getComponent().findComponent("llcType");

		if(it != null) {
			boolean disabled = false;

			if(tc == null || !tc.isLLC()) {
				disabled = true;
				it.setValue(null);
			}

			it.setDisabled(disabled);
		}
	}

	/** Respond to row clicks in the Agent table on the Agents tab.
	 *
	 * @param evt
	 */
	public void listenAgentRowClicked(SelectEvent evt) {
		// Only respond if not in edit mode.
		if(!editMode) {
			Object [] objs  = evt.getObjects();
			Agent agent = (Agent)objs[0];
			Integer agentId = null;

			if(agent != null) {
				agentId = agent.getId();
			}

			setupSelectedAgentItem(agentId);
			LOG.debug("");
		}
	}

	/**
	 * ValueChangeListener for Gender field when select 'Other' option LS-1958
	 *
	 * @param event
	 */
	public void listenGenderChange(ValueChangeEvent event) {
		GenderType gender = (GenderType)event.getNewValue();//LS-2502 Gender Field change
		if (gender != null) {
			if (gender.isOther()) {
				HtmlInputText inputText1 =
						(HtmlInputText)event.getComponent().findComponent("otherGenderDesc");
				inputText1.setValue("");
				inputText1.setTransient(true);
			}
			else {
				user.setOtherGenderDesc(null);
			}
			user.setGender(gender);
		}
	}

	/**
	 * LS-3412
	 * Listener to check changes in the Citizenship dropdown. If Alien Authorized to Work is
	 * selected, the Alien authorized to work country dropdown must be displayed. If any
	 * other value is selected, the alien authorized to work value must be erased.
	 * @param event
	 */
	public void listenCitizenshipChange(ValueChangeEvent event) {
		String citizenStatusCode = user.getCitizenStatus();

		if(!citizenStatusCode.equalsIgnoreCase(ALIEN_AUTH_CODE)) {
			user.setAlienAuthCountryCode(null);
		}
	}

	/**
	 * LS-3421
	 * Check if the home address is empty. If so, the sameAsHomeAddr
	 * variable will be set to false.
	 *
	 * @param event
	 */
	public void listenHomeAddressChange(ValueChangeEvent event) {
		if(user.getSameAsHomeAddr()) {
			// LS-3494 Update Mailing address fields with Home Address fields if
			// sameAsHomeAddr is true.
			Address mailingAddress = user.getMailingAddress();
			mailingAddress.copyFrom(user.getHomeAddress());
			if( user.getHomeAddress().isEmpty()) {
				user.setSameAsHomeAddr(false);
			}
		}
	}

	/**
	 * LS-3421
	 * Populate the mailingAddress fields with the values from the
	 * homeAddress fields if the value of the sameAsHomeAddr is true.
	 *
	 * @param event
	 */
	public void listenSameAsHomeAddrChange(ValueChangeEvent event) {
		Boolean sameAsAddr = (Boolean) event.getNewValue();

		if(sameAsAddr) {
			user.getMailingAddress().copyFrom(user.getHomeAddress());
		}
	}

	/**
	 * LS-3578
	 * Populate the loanOutMailingAddress fields with the values from the
	 * loanOutAddress fields if the value of the sameAsHomeAddr is true.
	 *
	 * @param event
	 */
	public void listenSameAsCorpAddrChange(ValueChangeEvent event) {
		Boolean sameAsAddr = (Boolean) event.getNewValue();

		if(sameAsAddr) {
			user.getLoanOutMailingAddress().copyFrom(user.getLoanOutAddress());
		}
	}

	/**
	 * LS-3578
	 * Check if the loan out address is empty. If so, the sameAsCorpAddr
	 * variable will be set to false.
	 *
	 * @param event
	 */
	public void listenLoanOutAddressChange(ValueChangeEvent event) {
		// LS-3578 Update Loan out Mailing address fields with loan out Address fields if
		// sameAsCorpAddr is true.
		if(user.getSameAsCorpAddr()) {
			Address loanOutMailingAddress = user.getLoanOutMailingAddress();
			loanOutMailingAddress.copyFrom(user.getLoanOutAddress());
			if( user.getLoanOutAddress().isEmpty()) {
				user.setSameAsCorpAddr(false);
			}
		}

	}

	/**
	 * check GST field when field is Not Registered
	 * then check box value is true else false
	 */
	public void isGstChecked() {
		if (QST_GST_NOT_REGISTERED.equalsIgnoreCase(user.getGstNumber())) {
			setGstChkbox(true);
		}
	}

	/**
	 * check QST field when field is Not Registered
	 * then check box value is true else false
	 */
	public void isQstChecked() {
		if (QST_GST_NOT_REGISTERED.equalsIgnoreCase(user.getQstNumber())) {
			setQstChkbox(true);
		}
	}

	/**
	 * Action method for the Save button on the Mobile account page.
	 * @return null navigation string
	 */
	public String actionSaveMobile() {
		try {
			if (! UserUtils.checkFields(user) ) { // update/clean/test various fields before saving
				setSelectedTab(TAB_DETAIL);
				return null;
			}
			user.setDisplayName(user.getFullName()); // include middle initial. LS-4744
			getUserDAO().attachDirty(user);
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "changes saved");

			// Instantiate any of the address objects that are null.
			initView();
			super.actionSave();
			return "myaccountm";
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		try {
			super.actionCancel();
			user = getUserDAO().refresh(user); // get old data back
			initView();	// re-display
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionAddAgent() {
		super.actionEdit();
		selectedAgent = new Agent();
		selectedAgent.setAgencyAddress(new Address((user.getShowCanada() && !user.getShowUS())));

		return null;
	}

	/**
	 * Action method for the "Change PIN" button.  Displays
	 * the "Change PIN" dialog.
	 *
	 * @return null navigation string
	 */
	public String actionOpenChangePin() {
		showChangePin = true;
		ChangePinBean.getInstance().show(this);
		addFocus("pin");
		maintainScrollPos();
		return null;
	}

	@Override
	protected void setupTabs() {
		//setupAssistantTab();
	}

	/**
	 * Change the states/provinces list for Canada Talent
	 * @param event
	 */
	public void listenCountryChange(ValueChangeEvent event) {
		String id = event.getComponent().getId();
		String countryCode = (String)event.getNewValue();

		if(id.contentEquals("userCountryId")) {
			userStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL().get(countryCode);
		}
		else if(id.contentEquals("agencyCountryId")) {
			agencyStateProvinceList = ApplicationScopeBean.getInstance().getStateCodeDL().get(countryCode);
		}
	}

	/**
	 * Callback from ImagePaginatorBean when user has confirmed to
	 * delete an image.
	 */
	@Override
	public void removeImage(Image image) {
		boolean rem = false;
		rem = user.getImages().remove(image);
		if (rem) {
			// Force refresh of list.
			user.setImageResources(null);
		}
		LOG.debug("image="+image+", removed="+rem);
	}

	/**
	 * When the user presses the Delete button to delete an image,
	 * the ImagePaginatorBean is called; it calls back to us at
	 * two methods: getImageList, to get the list of images currently
	 * displayed; and removeImage, for the image to be removed
	 * from this object's set of images.  The ImagePaginatorBean
	 * takes care of deleting the Image object from the database.
	 * @return List of Image
	 */
	@Override
	public List<Image> getImageList() {
		return user.getImages();
	}

	// * * * Support for "New Image" button * * *
	@Override
	public String actionOpenNewImage() {
		return super.actionOpenNewImage();
	}

	/**
	 * Callback from ImageAddBean when the user adds a new image to our object.
	 */
	@Override
	public void updateImage(Image image, String filename) {
		LOG.debug("");
		try {
			if (image != null) {
				image.setUser(user);
				user.getImages().add(image);
				user.setImageResources(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** See {@link #addedImages}. */
	@Override
	public Set<Image> getAddedImages() {
		return addedImages;
	}
	/** See {@link #addedImages}. */
	@Override
	public void setAddedImages(Set<Image> addedImages) {
		this.addedImages = addedImages;
	}

	/** Called by the PopupBean when the user clicks OK
	 * on a confirmation dialog. */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
			case ACT_DELETE_USER:
				res = actionDeleteOk();
				break;
			case ChangePinBean.ACT_PROMPT_PIN:
				// user OK'ed Change Pin dialog
				showChangePin = false;
				break;
			default:
				res = super.confirmOk(action);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		String res = null;
		switch(action) {
			case ChangePinBean.ACT_PROMPT_PIN:
				// user cancelled Change Pin dialog
				showChangePin = false;
			default:
				break;
		}
		return res;
	}

	/** See {@link #user}. */
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * @return the currently selected/displayed item - to match similar beans
	 */
	public User getElement() { // to simplify JSP
		return user;
	}

	/** Return the current element's name, for use in the title of the Add Image dialog */
	@Override
	public String getElementName() {
		return user.getDisplayName();
	}

	/** See {@link #showChangePin}. */
	public boolean getShowChangePin() {
		return showChangePin;
	}
	/** See {@link #showChangePin}. */
	public void setShowChangePin(boolean showChangePin) {
		this.showChangePin = showChangePin;
	}

	/** See {@link #hidden}. */
	public boolean getHidden() {
		return hidden;
	}
	/** See {@link #hidden}. */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	/** See {@link #qstChkbox}. */
	public boolean getQstChkbox() {
		return qstChkbox;
	}

	/** See {@link #qstChkbox}. */
	public void setQstChkbox(boolean qstChkbox) {
		this.qstChkbox = qstChkbox;
	}

	/** See {@link #gstChkbox}. */
	public boolean getGstChkbox() {
		return gstChkbox;
	}

	/** See {@link #gstChkbox}. */
	public void setGstChkbox(boolean gstChkbox) {
		this.gstChkbox = gstChkbox;
	}
	/** See {@link #PHONE_ITEMS}. */
	public SelectItem[] getPhoneItems() {
		if(user.getShowCanada()) {
			return CANADA_PHONE_ITEMS;
		}
		return PHONE_ITEMS;
	}

	public List<SelectItem> getImServiceTypeDL() {
		return EnumList.getImServiceTypeList();
	}

	/** See {@link com.lightspeedeps.util.app.Constants#CITIZEN_STATUS_ITEMS}. */
	public SelectItem[] getCitizenStatusItems() {
		if(user.getShowCanada()) {
			return Constants.CANADA_CITIZEN_STATUS_ITEMS;
		}
		if(!FF4JUtils.useFeature(FeatureFlagType.TTCO_ADDR_UNIF_USER_PROFILE)) {
			SelectItem[] oldValues= {
					new SelectItem("", "---"),
					new SelectItem("U", "U.S."),
					new SelectItem("A", "Res. Alien"),
					new SelectItem("O", "Other") };
			return oldValues;
		}
		return Constants.CITIZEN_STATUS_ITEMS;
	}

	@Override
	public void addEndingJavascript() {
		//addClientResize();
	}

	/** See {@link #userStateProvinceList}. */
	public List<SelectItem> getUserStateProvinceList() {
		return userStateProvinceList;
	}

	/** See {@link #userStateProvinceList}. */
	public void setUserStateProvinceList(List<SelectItem> userStateProvinceList) {
		this.userStateProvinceList = userStateProvinceList;
	}

	/** See {@link #agencyStateProvinceList}. */
	public List<SelectItem> getAgencyStateProvinceList() {
		return agencyStateProvinceList;
	}

	/** See {@link #agencyStateProvinceList}. */
	public void setAgencyStateProvinceList(List<SelectItem> agencyStateProvinceList) {
		this.agencyStateProvinceList = agencyStateProvinceList;
	}

	/** See {@link #selectedAgent}. */
	public Agent getSelectedAgent() {
		return selectedAgent;
	}

	/** See {@link #selectedAgent}. */
	public void setSelectedAgent(Agent selectedAgent) {
		this.selectedAgent = selectedAgent;
	}

	/** See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}

	/** See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	public AgentDAO getAgentDAO() {
		if(agentDAO == null) {
			agentDAO = AgentDAO.getInstance();
		}

		return agentDAO;
	}

	public UserDAO getUserDAO() {
		if(userDAO == null) {
			userDAO = UserDAO.getInstance();
		}

		return userDAO;
	}

	/**
	 * Check if the home address is empty. If so, the sameAsHomeAddr
	 * variable will be set to false and Auto fill City and State by given Input 'HomeAddressZip'
	 * from API
	 * @param event
	 */
	public void listenHomeAddressZipChange(ValueChangeEvent event) {

		String newZip = (String) event.getNewValue();
		user.getHomeAddress().setZip(newZip);
		if (user.getSameAsHomeAddr()) {// reused it as is existing functionality
			// LS-3494 Update Mailing address fields with Home Address fields if
			// sameAsHomeAddr is true.
			Address mailingAddress = user.getMailingAddress();
			mailingAddress.copyFrom(user.getHomeAddress());
			if (user.getHomeAddress().isEmpty()) {
				user.setSameAsHomeAddr(false);
			}
		}
		fillCityStateByZipCode(user.getHomeAddress());
	}

	/**
	 * Auto fill City and State by given Input 'MailingAddressZip' from API.
	 * @param event
	 */
	public void listenMailingAddressZipChange(ValueChangeEvent event) {
		String newZip = (String) event.getNewValue();
		user.getMailingAddress().setZip(newZip);
		fillCityStateByZipCode(user.getMailingAddress());
	}

	/**
	 * Auto fill City and State by given Input 'AgencyAddressZip' from API.
	 * @param event
	 */
	public void listenAgencyAddressZipChange(ValueChangeEvent event) {
		String newZip = (String) event.getNewValue();
		user.getAgencyAddress().setZip(newZip);
		fillCityStateByZipCode(user.getAgencyAddress());
	}

	/**
	 * Auto fill City and State by given Input 'LoanOutAddressZip' from API.
	 * @param event
	 */
	public void listenLoanOutAddressZipChange(ValueChangeEvent event) {
		String newZip = (String) event.getNewValue();
		user.getLoanOutAddress().setZip(newZip);
		if (user.getSameAsCorpAddr()) {// reused it as is existing functionality
			Address loanOutMailingAddress = user.getLoanOutMailingAddress();
			loanOutMailingAddress.copyFrom(user.getLoanOutAddress());
			if (user.getLoanOutAddress().isEmpty()) {
				user.setSameAsCorpAddr(false);
			}
		}
		fillCityStateByZipCode(user.getLoanOutAddress());
	}

	/**
	 * Auto fill City and State by given Input 'LoanOutMailingAddressZip' from API.
	 * @param event
	 */
	public void listenLoanOutMailingAddrZipChange(ValueChangeEvent event) {
		String newZip = (String)event.getNewValue();
		user.getLoanOutMailingAddress().setZip(newZip);
		fillCityStateByZipCode(user.getLoanOutMailingAddress());
	}

	/**
	 * Auto fill City and State by given Input 'AddressZip' from API
	 * this method is used for Home Address, Mailing Address,
	 * Agency address, Loan out Address, Loan out mailing address
	 * shows error messages if required for validation.
	 * includes :LS-4478,LS-4479,LS-4480,LS-4481,LS-4482.
	 * @param event
	 */
	public void fillCityStateByZipCode(Address address) {
		List<AddressInformation> list = new ArrayList<>();
		if (address.isZipValidIgnoreState()) {
			list = LocationUtils.getCityStateByZip(address.getZip());// API returns the List of cities,State by Zip.
			if (list != null && list.size() == 1) {
				list.get(0).setSelected(true);
				LocationUtils.setCityStateByZipCode(list, address);

			}
			else if (list != null && list.size() > 1) {
				// Show Popup for multiple Zipcode with City name
				ZipCitiesPopupBean bean = ZipCitiesPopupBean.getInstance();
				bean.show(this, 0, "PopupSelectZipcodeBean.ZipcodePopup.Title", "Confirm.Enter",
						"Confirm.Close");
				bean.setAddress(address);
				bean.setCityStateList(list);

			}
			else {
				// If List is empty zipcode have value show error message
				if (! StringUtils.isEmpty(address.getZip())) {
					MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
				}
				address.clearCityStateZip();
			}

		}
		else {
			// If Zipcode holds value show error message
			if (! StringUtils.isEmpty(address.getZip())) {
				MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
			}
			address.clearCityStateZip();
		}

	}
}
