package com.lightspeedeps.web.production;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.web.login.LoginBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.util.*;

/**
 * The backing bean for the My Production List/View/Edit desktop page, and the
 * Mobile "My Productions" page, where the entries in the main list are actually
 * the result of Contact objects associating the current user with a particular
 * Production.
 */
@ManagedBean
@ViewScoped
public class ProductionContactBean extends ProductionListBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3500191133983515118L;

	private static final Log LOG = LogFactory.getLog(ProductionContactBean.class);

	/** The index of our "Detail" mini-tab (0). */
	private static final int TAB_CONTACT_DETAIL = 0;

	private static final int ACT_DECLINE = 10;
	private static final int ACT_ACCEPT = 11;
	private static final int ACT_DEACTIVATE = 12;
	private static final int ACT_REACTIVATE = 13;
	private static final int ACT_UNSUBSCRIBE = 14;
	private static final int ACT_UPGRADE = 15;
	private static final int ACT_DELETE_SECOND_PROD = 20;

	/** Number of entries in the production list above which the JSP should add
	 * pagination controls.  This value should match the "rows=" attribute on the
	 * dataTable tag for the production list. */
	private static int PAGINATE_SIZE = 200;
	{
		PAGINATE_SIZE = ApplicationScopeBean.getInstance().getIsBeta() ? 50 : 200;
	}

	/* Variables*/

	/** The list of elements currently displayed, based on the category
	 * selected by the user, and any filter text setting. */
	private List<Contact> contactList;

	/** If a category other than "All" has been chosen, then this list will have
	 * all the Contacts that match the category, regardless of the 'filter'
	 * setting. */
	private List<Contact> selectedContactList;

	/** The list of all contact/production entries possible to display. */
	private List<Contact> allContactList;

	/** The Contact object associated with the currently selected list entry. */
	private Contact contact;

	/** The name on the User's account for the owner of the currently
	 * selected Production. */
	private String owner;

	/** Database id value of the {@link Contact} corresponding to the production title
	 * clicked to activate entry into the production. */
	private Integer contactId;

	/** Database id value of the currently selected production. (Used in mobile page.) */
	private Integer productionId;

	/** Title (name) of the currently selected production. (Used in mobile page.) */
	private String productionTitle;

	/** The currently selected category of Productions to be displayed:
	 * Active, Inactive, or All. */
	private String category = Constants.CATEGORY_ALL;

	/** True iff the current user has Prod Data Admin privilege on the currently
	 * selected line item. */
	private boolean isProdAdmin;

	/** True if the "Accept invitation" dialog is to be displayed. */
//	boolean showAccept;

	/** Setting of "include touring" flag when Edit is clicked.  If this changes from
	 * false to true (at time of Save), we initialize the department mask. */
	private boolean priorIncludeTouring;

	/** Setting of I9 attachment policy when Edit is clicked. If this has been changed
	 * when Save is done, a Changes event is generated.
	 */
	private ExistencePolicy priorI9policy;

	/** True iff the displayed list of productions should be paginated. */
	private boolean paginate;

	/** Backing field for the "copy address" checkbox on the Accept Invitation dialog. */
	private boolean copyAddress;
	/** True iff the "copy address" checkbox should be enabled. */
	private boolean enableCopyAddress;

	/** Backing field for the "copy home phone" checkbox on the Accept Invitation dialog. */
	private boolean copyHomePhone;
	/** True iff the "copy home phone" checkbox should be enabled. */
	private boolean enableCopyHomePhone;

	/** Backing field for the "copy business phone" checkbox on the Accept Invitation dialog. */
	private boolean copyBusinessPhone;
	/** True iff the "copy business phone" checkbox should be enabled. */
	private boolean enableCopyBusinessPhone;

	/** Backing field for the "copy cell phone" checkbox on the Accept Invitation dialog. */
	private boolean copyCellPhone;
	/** True iff the "copy cell phone" checkbox should be enabled. */
	private boolean enableCopyCellPhone;

	/** Backing field for the "copy image" checkbox on the Accept Invitation dialog. */
	private boolean copyImage;
	/** True iff the "copy image" checkbox should be enabled. */
	private boolean enableCopyImage;

	/** The Employer list to be displayed on UI */
	private static final List<SelectItem> employerOfRecordList = createEorSelectList();

	/** The PDF grouping list to be displayed on UI */
	private static final List<SelectItem> pdfGroupingTypeList = EnumList.createEnumValueSelectList(PdfGroupingType.class);

	/** The PDF grouping list to be displayed on UI */
	private static final List<SelectItem> existencePolicyList = EnumList.createEnumValueSelectList(ExistencePolicy.class);

	private transient ContactDAO contactDAO = ContactDAO.getInstance();

	/** Check User and set constant accordingly to HeaderText*/
	private String myProductionHeaderTitle;
	private String productionHeaderTitle;

	/**text to Production Detail tab for I-9 disclosure. */
	private String i9DisclosureText;



	/** Constructor */
	public ProductionContactBean() {
		super(Production.SORTKEY_START, "Production.");
		LOG.debug("Init");
		try {
			if (HeaderViewBean.getInstance().getMobile()) {
				category = Constants.CAT_ACTIVE; // Mobile page list defaults to "Active"
			}
			else {
				category = Constants.CATEGORY_ALL; // Desktop defaults to "All"
			}
			category = UserPrefBean.getInstance().getString(Constants.ATTR_MYPROD_CATEGORY, category);
			filter = null; //UserPrefBean.getInstance().getString(Constants.ATTR_PROD_FILTER, null);
			if (! refreshList(false)) { // some production expired
				refreshList(false);		// so rebuild lists
			}
			restoreSortOrder(); // restore the user's last-used sort column and order

			Integer id = SessionUtils.getInteger(Constants.ATTR_SELECT_CONTACT_ID);
			if (id != null) {
				contact = getContactDAO().findById(id);
				if (contact == null || ! selectedContactList.contains(contact) ) {
					// contact probably was deleted from production
					id = null;
					SessionUtils.put(Constants.ATTR_SELECT_CONTACT_ID, null);
				}
			}
			if (id == null) { // probably just logging into LightSPEED
				id = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID);
				if (id != null) {
					Production prod = getProductionDAO().findById(id);
					id = null;
					if (prod != null) {
						contact = getContactDAO().findByUserProduction(getvUser(), prod);
						if (contact != null && selectedContactList.contains(contact)) {
							id = contact.getId();
						}
					}
				}
			}
			setupSelectedItem(id);
			scrollToRow();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ValueChangeListener for include touring option selection , LS-1955
	 *
	 * @param event
	 */
	public void listenTouringOptionChange(ValueChangeEvent event) {
		if (event != null) {
			boolean select = (Boolean)event.getNewValue();
			if (select) {
				production.getPayrollPref().setTeamEor(null);
			}
		}
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		LOG.debug("id=" + id);
		if (contact != null) {
			contact.setSelected(false);
		}
		imageList = null;
		setImageResources(null);
		resetImages();
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_SELECT_CONTACT_ID, null);
			contact = null;
			production = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			contact = getContactDAO().findById(id);
			if (contact == null) {
				id = findDefaultId();
				if (id != null) {
					contact = getContactDAO().findById(id);
				}
			}
//			if ( ! getCategory().equals(CATEGORY_ALL) &&
//					! getCategory().equals(production.getType().name())) {
//				changeCategory(, false);
//			}
			SessionUtils.put(Constants.ATTR_SELECT_CONTACT_ID, id);
		}
		else {
			contact = new Contact();
			production = new Production(""); // get instance with default values
			contact.setProduction(production);
		}

		isProdAdmin = false;
		if (contact != null) {
			contact.setSelected(true);
			getStateMap().clear();
			selectRowState(contact);
			contactId = contact.getId();
			production = contact.getProduction();
			if (production != null) {
				if (production.getAddress() == null) {
					production.setAddress(new Address());
				}
				User user = UserDAO.getInstance().findOneByProperty(
						UserDAO.ACCOUNT_NUMBER, production.getOwningAccount());
				if (user != null) {
					owner = user.getFirstNameLastName();
				}
				else {
					owner = "";
				}
				isProdAdmin = ProjectMemberDAO.getInstance().isContactProductionAdmin(contact);
				if (!isProdAdmin) {
					if (contact.getUser().getEmailAddress().endsWith("@theteamcompanies.com")) {
						isProdAdmin = true;
					}
				}
				calculateBillAmount();
				UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROD_ID, production.getId());
				//LS-2100 Add text to Production Detail tab for I-9 disclosure check
				if (production.getPayrollPref().getI9Attachment() == ExistencePolicy.REQUIRE) {
					setI9DisclosureText(MsgUtils.getMessage("FormI9Bean.I9DisclosureText"));
				}
				else {
					setI9DisclosureText(null);
				}
				if (SessionUtils.getCurrentUser().getShowUS()) {
					setMyProductionHeaderTitle(Constants.HEADER_TEXT_FOR_US);
					setProductionHeaderTitle(Constants.STANDARD_PRODUCTION_TEXT);
				}
				else {
					setMyProductionHeaderTitle(Constants.HEADER_TEXT_FOR_CANADA);
					setProductionHeaderTitle(Constants.CANADA_PRODUCTION_TEXT);
				}
			}
			forceLazyInit();
		}
	}

	/**
	 * @see com.lightspeedeps.web.production.ProductionListBean#findDefaultId()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Integer findDefaultId() {
		Integer id = null;
		List<Contact> list = getItemList();
		if (list.size() > 0) {
			id = list.get(0).getId();
		}
		return id;
	}

	/**
	 * Ensure all items we need for render phase are initialized.
	 * @see com.lightspeedeps.web.production.ProductionListBean#forceLazyInit()
	 */
	@Override
	protected void forceLazyInit() {
		super.forceLazyInit();
		try {
			if (contact.getHomeAddress() != null) {
				contact.getHomeAddress().getAddrLine1();
			}
		}
		catch (Exception e) {
			LOG.warn("error ignored");
		}
	}

	/**
	 * @see com.lightspeedeps.web.production.ProductionListBean#setSelected(java.lang.Object, boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Contact)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * If no item is currently selected, find the current element in
	 * the main (left-hand) list, select it, and send a JavaScript
	 * command to scroll the list so that it is visible.
	 */
	@Override
	protected void scrollToRow() {
		scrollToRow(getContact());
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		LOG.debug("");
		if (contact == null || getElement() == null || isNewEntry()) {
			production = null;
			contact = null;
		}
		else {
			contact = getContactDAO().refresh(contact);
		}
		super.actionCancel();
		if (contact != null) {
			setupSelectedItem(contact.getId());
		}
		return null;
	}

	/**
	 * The action method when 'Delete' is clicked on the FIRST confirmation
	 * pop-up.  Here we put up the second confirmation dialog.
	 */
	@Override
	public String actionDeleteOk() {
		if (production.getStatus() == AccessStatus.ACTIVE) {
			// we shouldn't be able to get here for an Active production!
			MsgUtils.addFacesMessage("Production.Delete.NotAllowed", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		PopupBean conf = PopupBean.getInstance();
		conf.show(this, ACT_DELETE_SECOND_PROD,
				"Production.Delete2.Title",
				"Production.Delete2.Ok",
				"Production.Delete2.Cancel");
		String msg = MsgUtils.formatMessage("Production.Delete2.Text",
				production.getTitle());
		conf.setMessage(msg);
		return null;
	}

	/**
	 * Action method for the "Delete" button on the second confirmation
	 * pop-up.
	 * Delete the selected Production -- this really only marks the Production
	 * with "deleted" status; the data remains in the database.
	 */
	public String actionDeleteSecondOk() {
		try {
			production = getProductionDAO().findById(production.getId()); // refresh
			getProductionDAO().pseudoDelete(production); // just change status!
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, null);
			SessionUtils.put(Constants.ATTR_SELECT_CONTACT_ID, null);
			ChangeUtils.logChange(ChangeType.PRODUCTION, ActionType.DELETE, production,
					getvUser(), production.getSku() + ": " + production.getTitle());

			refreshList(true);
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The Value Change Listener for the category (production type or status)
	 * selection drop-down list on the production list (left-hand side) display.
	 *
	 * @param evt The event created by the framework.
	 */
	public void selectedCategory(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				changeCategory( (String)evt.getNewValue(), ! editMode);
			}
			else {
				LOG.warn("Null newValue in category change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Set a new category (or "All") as the type of item listed. This regenerates the list of
	 * elements listed. If the currently selected item is not in the new list, then select the first
	 * entry of the new list.
	 *
	 * @param type A ScriptElementType value, or "All".
	 */
	protected boolean changeCategory(String type, boolean pickElement) {
		boolean bRet = true;
		if (contact != null && ! editMode) {
			contact.setSelected(false); // we may end up switching
		}
		if (selectedContactList != null) {
			for (Contact c : selectedContactList) {
				c.setSelected(false);
			}
		}
		for (Contact c : allContactList) {
			c.setSelected(false);
		}
		UserPrefBean.getInstance().put(Constants.ATTR_MYPROD_CATEGORY, type);
		category = type;
		if (type.equals(Constants.CATEGORY_ALL)) {
			selectedContactList = allContactList;
		}
		else {
			selectedContactList = new ArrayList<>();
			boolean showActive = type.equals(Constants.CAT_ACTIVE);
			for (Contact c : allContactList) {
				if ( (c.getProduction().getStatus()==AccessStatus.ACTIVE) == showActive) {
					selectedContactList.add(c);
				}
			}
		}
//		paginate = contactList.size() > PAGINATE_SIZE;
		contactList = applyFilter(selectedContactList);
		setSelectedRow(-1);
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<Contact> list = getItemList();
			if (contact != null) {
				int ix = list.indexOf(contact);
				if (ix < 0) {
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
						setSelectedRow(0);
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					contact = list.get(ix);
					contact.setSelected(true);
					production = contact.getProduction();	// rev 2.2.4828, fix LazyInitExc
					production = getProductionDAO().refresh(production); // fix LIE 2.9.5102
					forceLazyInit(); // refresh referenced data
				}
			}
			else {
				// no current element & not doing "Add"; if list has entries, view the first
				if (list.size() > 0) {
					setupSelectedItem(list.get(0).getId());
					setSelectedRow(0);
				}
			}
		}
//		addClientResize();
		return bRet;
	}

	@Override
	protected void refreshList() {
		if (! refreshList(true)) { // some production expired
			refreshList(true);		// so rebuild lists
		}
	}

	private boolean refreshList(boolean pickElement) {
		boolean bRet = true;
		allContactList = getContactDAO().findByUserActive(SessionUtils.getCurrentUser());
		paginate = allContactList.size() > PAGINATE_SIZE;
		Calendar expireCal = Calendar.getInstance();
		CalendarUtils.setStartOfDay(expireCal);
		Date expireDate = expireCal.getTime();
		for (Iterator<Contact> iter = allContactList.iterator(); iter.hasNext(); ) {
			Contact c = iter.next();
			if (c.getStatus() == MemberStatus.DECLINED ||
					c.getStatus() == MemberStatus.DELETED) {
				iter.remove();
			}
			else {
				Production p = c.getProduction();
				if (p.getId().equals(Constants.SYSTEM_PRODUCTION_ID)) {
					iter.remove();
				}
				else {
					if (paginate) {
						// don't look up schedule date or day status, too expensive
						p.setScheduleStartDate(p.getStartDate());
						p.setDayStatus("*");
					}
					if (p.getDayStatus() == null) {
						p.setDayStatus(calculateDayStatus(p));
					}
					if (p.getEndDate() != null && expireDate.after(p.getEndDate())
							&& p.getStatus() != AccessStatus.EXPIRED_ACTIVE
							&& p.getStatus() != AccessStatus.EXPIRED_ARCHIVED) {
						AccessStatus status = AccessStatus.EXPIRED_ACTIVE;
						if (p.getStatus() ==  AccessStatus.ARCHIVED) {
							status =  AccessStatus.EXPIRED_ARCHIVED;
						}
						getProductionDAO().expire(p, status);
						bRet = false; // status changed, rebuild list
					}
					if (p.isBillable() && p.getNextBillDate() != null && expireDate.after(p.getNextBillDate())) {
						Calendar bill = CalendarUtils.getInstance(p.getNextBillDate());
						bill.add(Calendar.MONTH, 1);
						p.setNextBillDate(bill.getTime());
						p = getProductionDAO().merge(p);
					}
//					forceLazyInit(c);
					p.getDefaultProject().getEpisode(); // needed for rendering when multi-page in use
				}
			}
		}
		LOG.debug("iterate done");
		if (bRet) {
			changeCategory(getCategory(), pickElement);
		}
		return bRet;
	}

	/**
	 * Action method called whenever a key-up event is detected in the filter
	 * text field. This is done in the JSP via a hidden button.
	 *
	 * @return null navigation string
	 */
	public String actionFilter() {
		if (allContactList.size() > 0) {
			if (category.equals(Constants.CATEGORY_ALL)) {
				contactList = applyFilter(allContactList);
			}
			else {
				contactList = applyFilter(selectedContactList);
			}
			int ix = contactList.indexOf(contact);
			if (ix >= 0) {	// current prod still in filtered list
				setSelectedRow(ix); // highlight the row
			}
			else {
				// current prod has been eliminated - setup a new one
				production.setSelected(false);
				Integer id = null;
				if (contactList.size() > 0) {
					// just make the top entry current
					contact = contactList.get(0);
					setSelectedRow(0);
					id = contact.getId();
				}
				else {
					// empty list, clear highlighting
					setSelectedRow(- 1);
				}
				// set up the newly selected item
				setupSelectedItem(id);
			}
		}
		else { // empty list
			filter = "";
		}
		return null;
	}

	/**
	 * Action method for the "Accept" button. Prompts the user to add contact
	 * information to the production, if appropriate.
	 *
	 * @return null navigation string
	 */
	public String actionAccept() {
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				copyAddress = false;
				copyHomePhone = false;
				copyBusinessPhone = false;
				copyCellPhone = false;

				//User user = contact.getUser();

				/*if (user.getHomeAddress() != null && ! user.getHomeAddress().isEmpty()) {
					enableCopyAddress = ! user.getHomeAddress().equalsAddress(contact.getHomeAddress());
				}
				else {
					enableCopyAddress = false;
				}

				if (StringUtils.hasText(user.getHomePhone())) {
					enableCopyHomePhone = ! user.getHomePhone().equals(contact.getHomePhone());
				}
				else {
					enableCopyHomePhone = false;
				}
				if (StringUtils.hasText(user.getBusinessPhone())) {
					enableCopyBusinessPhone = ! user.getBusinessPhone().equals(contact.getBusinessPhone());
				}
				else {
					enableCopyBusinessPhone = false;
				}

				if (StringUtils.hasText(user.getCellPhone())) {
					enableCopyCellPhone = ! user.getCellPhone().equals(contact.getCellPhone());
				}
				else {
					enableCopyCellPhone = false;
				}

				if (user.getImageCount() == 0) {
					enableCopyImage = false;
				}
				else {
					enableCopyImage = true;
				}

				if ( enableCopyAddress || enableCopyHomePhone ||
						enableCopyBusinessPhone || enableCopyCellPhone ||
						enableCopyImage) {
					showAccept = true;
				}
				else {
					PopupBean conf = PopupBean.getInstance();
					conf.show(this, ACT_ACCEPT,
							"Production.Accept.Title",
							"Production.Accept.Ok",
							"Confirm.Cancel");
					String msg = MsgUtils.formatMessage("Production.Accept.Text",
							production.getTitle());
					conf.setMessage(msg);
//					addClientResize();
				}*/

				PopupBean conf = PopupBean.getInstance();
				conf.show(this, ACT_ACCEPT,
						"Production.Accept.Title",
						"Production.Accept.Ok",
						"Confirm.Cancel");
				String msg;
				// LS-2098 Add message to Accept popup reflecting I-9 policy
				if (! production.getType().isCanadaTalent() &&
						production.getPayrollPref().getI9Attachment() == ExistencePolicy.REQUIRE &&
						(contact.getRole().isFinancialAdmin() || contact.getRole().isDataAdmin())) {
					msg = MsgUtils.formatMessage(
							"Production.Accept.I9ReqSupportDocs.Text",
							production.getTitle());
				}
				else {
					msg = MsgUtils.formatMessage("Production.Accept.Text",
							production.getTitle());
				}
				conf.setMessage(msg);

			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return null;
	}

	/**
	 * Action method when user clicks OK on the Accept Invitation
	 * dialog box.
	 * @return null navigation string.
	 */
	public String actionAcceptOk() {
//		showAccept = false;
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				if (contact != null) {
					/*User user = contact.getUser();
					try {
						if (copyAddress && user.getHomeAddress() != null) {
							if (contact.getHomeAddress() == null) {
								Address addr = user.getHomeAddress().clone();
								contact.setHomeAddress(addr);
							}
							else {
								contact.getHomeAddress().copyFrom(user.getHomeAddress());
								getContactDAO().attachDirty(contact.getHomeAddress());
							}
						}
						if (copyHomePhone) {
							contact.setHomePhone(user.getHomePhone());
						}
						if (copyBusinessPhone) {
							contact.setBusinessPhone(user.getBusinessPhone());
						}
						if (copyCellPhone) {
							contact.setCellPhone(user.getCellPhone());
						}
						if (copyImage) {
							if (user.getImageCount() > 0) {
								Image image = user.getImages().get(0).clone();
								image.setUser(null);
								if (contact.getCastMember() != null) {
									RealWorldElement cast = contact.getCastMember();
									image.setRealWorldElement(cast);
									if (cast.getImages() == null) {
										cast.setImages(new ArrayList<Image>());
									}
									cast.getImages().add(image);
									getContactDAO().attachDirty(cast);
								}
								else {
									image.setContact(contact);
									if (contact.getImages() == null) {
										contact.setImages(new ArrayList<Image>());
									}
									contact.getImages().add(image);
								}
							}
						}
					}
					catch (Exception e) {
						EventUtils.logError(e);
					}*/
					if (contact.getLoginAllowed() && (production.getStatus() == AccessStatus.ACTIVE)) {
						contact.setStatus(MemberStatus.ACCEPTED);
					}
					else {
						contact.setStatus(MemberStatus.NO_ACCESS);
					}
					contact = getContactDAO().merge(contact);
					refreshList(true); // select 1st item in current list
					//MsgUtils.addFacesMessage("ProdSelect.Accepted", FacesMessage.SEVERITY_INFO);
					notifyAccepted();
				}
			}
			refreshList(true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method when user clicks Accept on the Accept Invitation
	 * mobile page.
	 * @return navigation string to return to production list
	 */
	public String actionAcceptMobile() {
//		showAccept = false;
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				if (contact != null) {
					if (contact.getLoginAllowed() && (production.getStatus() == AccessStatus.ACTIVE)) {
						contact.setStatus(MemberStatus.ACCEPTED);
					}
					else {
						contact.setStatus(MemberStatus.NO_ACCESS);
					}
					contact = getContactDAO().merge(contact);
					notifyAccepted();
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return "myproductionsm";
	}

	private void notifyAccepted() {
		if (contact.getCreatedBy() != null) {
			List<User> list = UserDAO.getInstance().findByProperty(UserDAO.ACCOUNT_NUMBER,
					contact.getCreatedBy());
			if (list.size() > 0) {
				User inviter = list.get(0);
				if (inviter != null) {
					Contact toContact = ContactDAO.getInstance()
							.findByUserProduction(inviter, production);
					if (toContact != null) {
						DoNotification.getInstance().invitationAccepted(toContact,
								contact.getUser(), production);
					}
				}
			}
		}
	}

	/**
	 * Action method for the "Cancel" button on the "Accept Invitation"
	 * dialog box.
	 * @return null navigation string
	 */
	public String actionCancelAccept() {
//		showAccept = false;
		forceLazyInit();
//		addClientResize();
		return null;
	}

	/**
	 * Action method for the "Block" button on an individual Production entry.
	 * The 'contactId' field should be set already from a
	 * f:setPropertyActionListener tag.
	 *
	 * @return null navigation string
	 */
	public String actionBlock() {
		LOG.debug("");
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				if (contact != null) {
					contact.setStatus(MemberStatus.BLOCKED);
					contact = getContactDAO().merge(contact);
					refreshList(true); // select 1st item in current list
					MsgUtils.addFacesMessage("ProdSelect.Blocked", FacesMessage.SEVERITY_INFO);
				}
			}
			refreshList(true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Unblock" button on an individual Production entry.
	 * The 'contactId' field should be set already from a
	 * f:setPropertyActionListener tag.
	 *
	 * @return null navigation string
	 */
	public String actionUnblock() {
		LOG.debug("");
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				if (contact != null) {
					contact.setStatus(MemberStatus.DECLINED);
					contact = getContactDAO().merge(contact);
					refreshList(true); // select 1st item in current list
					MsgUtils.addFacesMessage("ProdSelect.Unblocked", FacesMessage.SEVERITY_INFO);
				}
			}
			refreshList(true);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Save the "include touring" flag when entering Edit mode.
	 * @see com.lightspeedeps.web.production.ProductionListBean#actionEdit()
	 */
	@Override
	public String actionEdit() {
		priorIncludeTouring = production.getPayrollPref().getIncludeTouring();
		priorI9policy = production.getPayrollPref().getI9Attachment();
		return super.actionEdit();
	}

	/**
	 * action method used for saving representative email address for commercial production
	 * where the project are active
	 *
	 * @see com.lightspeedeps.web.production.ProductionListBean#actionSave()
	 */
	@Override
	public String actionSave() {
		if (production.getType().hasPayrollByProject() && production.getStatus() == AccessStatus.ACTIVE) {
			String onboardingEmail = production.getPayrollPref().getOnboardEmailAddress();
			String batchEmail = production.getPayrollPref().getBatchEmailAddress();
			List<Project> projects = ProjectDAO.getInstance().findByProduction(production);
			boolean usePremiumRate = production.getPayrollPref().getUsePremiumRate();
			for (Project project : projects){
				if (project.getStatus().equals(AccessStatus.ACTIVE)) {
					PayrollPreference preference =  project.getPayrollPref();
					// Only propagate onboarding and batch email address if they have a value.
					// Also if there is a value, set the check box on the Onboarding and Payroll Preferences
					// pages that allows the email addresses to be used for onboarding and batch transfers
					if (onboardingEmail != null && ! onboardingEmail.isEmpty()) {
						preference.setOnboardEmailAddress(onboardingEmail);
						preference.setUseOnboardEmail(true);
					}
					if (batchEmail != null && ! batchEmail.isEmpty()) {
						preference.setBatchEmailAddress(batchEmail);
						preference.setUseEmail(true);
					}
					if (projects.size() == 1) {
						// LS-2283 if 'new' production, set project's premium rate, too.
						preference.setUsePremiumRate(usePremiumRate);
					}
				}
			}
			if ((! priorIncludeTouring) && production.getPayrollPref().getIncludeTouring()) {
				// user turned on the "include touring" setting
				production.setDeptMaskB(Constants.HYBRID_DEPARTMENTS_MASK);
				if (projects.size() == 1) {
					// new production, set project mask, too.
					Project project = projects.get(0);
					project.setDeptMask(Constants.HYBRID_DEPARTMENTS_MASK);
					getProductionDAO().attachDirty(project);
				}
			}
			if (priorI9policy != production.getPayrollPref().getI9Attachment()) {
				// LS-2097 - I-9 attachment policy changed - record a change event
				ChangeUtils.logChange(ChangeType.PAYROLL, ActionType.UPDATE, production, SessionUtils.getCurrentUser(),
						"I-9 attachment setting changed to: " + production.getPayrollPref().getI9Attachment().name());
			}
		}
		return super.actionSave();
	}

	/**
	 * Action method for the "Decline" button on an individual Production entry.
	 * This button appears when a user has been invited to a Production. The
	 * 'contactId' field should be set already from a
	 * f:setPropertyActionListener tag.
	 *
	 * @return null navigation string
	 */
	public String actionDecline() {
		try {
			LOG.debug(contactId);
			if (contactId != null) {
				contact = getContactDAO().findById(contactId);
				if (contact != null && contact.getProduction() != null) {
					PopupBean conf = PopupBean.getInstance();
					conf.show(this, ACT_DECLINE,
							"ProdSelect.Decline.Title",
							"ProdSelect.Decline.Ok",
							"Confirm.Cancel");
					String msg = MsgUtils.formatMessage("ProdSelect.Decline.Text",
							contact.getProduction().getTitle());
					conf.setMessage(msg);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The action method for the OK button on the "Decline Invitation"
	 * confirmation prompt dialog box. The Contact's entry in the production is
	 * changed from PENDING status to DECLINED status. The 'contactId' field
	 * should be set from the prior entry to the actionDecline method.
	 */
	private String actionDeclineOk() {
		if (contactId != null) {
			contact = getContactDAO().findById(contactId);
			if (contact != null) {
				contact.setStatus(MemberStatus.DECLINED);
				notifyDeclined();
				getContactDAO().removeContact(contact);
				refreshList(true);
			}
		}
		return null;
	}

	/**
	 * The action method for the OK button on the "Decline Invitation"
	 * confirmation prompt dialog box. The Contact's entry in the production is
	 * changed from PENDING status to DECLINED status. The 'contactId' field
	 * should be set from the prior entry to the actionDecline method.
	 */
	public String actionDeclineMobile() {
		if (contactId != null) {
			contact = getContactDAO().findById(contactId);
			if (contact != null) {
				contact.setStatus(MemberStatus.DECLINED);
				notifyDeclined();
				getContactDAO().removeContact(contact);
			}
		}
		return "myproductionsm";
	}

	private void notifyDeclined() {
		if (contact.getCreatedBy() != null) {
			List<User> list = UserDAO.getInstance().findByProperty(UserDAO.ACCOUNT_NUMBER, contact.getCreatedBy());
			if (list.size() > 0) {
				User inviter = list.get(0);
				if (inviter != null) {
					Contact toContact = ContactDAO.getInstance().findByUserProduction(inviter, production);
					DoNotification.getInstance().invitationDeclined(toContact,
							contact.getUser(), production);
				}
			}
		}
	}

	/**
	 * Action method for the "Archive/Deactivate" button on an individual Production
	 * entry.
	 *
	 * @return null navigation string
	 */
	public String actionDeactivate() {
		try {
			if (production != null) {
				PopupBean conf = PopupBean.getInstance();
				conf.show(this, ACT_DEACTIVATE,
						"Production.Deactivate.Title",
						"Production.Deactivate.Ok",
						"Confirm.Cancel");
				String msg = MsgUtils.formatMessage("Production.Deactivate.Text",
						production.getTitle());
				conf.setMessage(msg);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The action method for the OK button on the "Archive/Deactivate" confirmation
	 * prompt dialog box. The 'contactId' field should be set from the prior
	 * entry to the actionDeactivate method.  The production will be set
	 * to INACTIVE status.
	 */
	private String actionDeactivateOk() {
		if (production != null) {
			production = getProductionDAO().refresh(production);
			//productionDAO.expire(production, AccessStatus.ARCHIVED);
			production.setStatus(AccessStatus.ARCHIVED);
			production.setSmsEnabled(false);
			production = getProductionDAO().merge(production);
			ChangeUtils.logChange(ChangeType.PRODUCTION, ActionType.UPDATE, production,
					getvUser(), production.getSku() + ": " + production.getTitle() + " [ARCHIVED]");
			refreshList(true);
		}
		return null;
	}

	/**
	 * Action method for the "Unarchive/Reactivate" button on an individual Production
	 * entry. The 'contactId' field should be set already from a
	 * f:setPropertyActionListener tag.
	 *
	 * @return null navigation string
	 */
	public String actionReactivate() {
		try {
			if (production != null) {

					// TODO check for expired production!

					PopupBean conf = PopupBean.getInstance();
					conf.show(this, ACT_REACTIVATE,
							"Production.Reactivate.Title",
							"Production.Reactivate.Ok",
							"Confirm.Cancel");
					String msg = MsgUtils.formatMessage("Production.Reactivate.Text",
							production.getTitle());
					conf.setMessage(msg);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * The action method for the OK button on the "Unarchive/Reactivate" confirmation
	 * prompt dialog box. The 'contactId' field should be set from the prior
	 * entry to the actionDeactivate method.  The production will be set
	 * to INACTIVE status.
	 */
	private String actionReactivateOk() {
		if (production != null) {
			production = getProductionDAO().refresh(production);
			production.setStatus(AccessStatus.ACTIVE);
			Product product = ProductDAO.getInstance().findOneByProperty(ProductDAO.SKU, production.getSku());
			if (product != null) {
				production.setSmsEnabled(product.getSmsEnabled());
			}
			production = getProductionDAO().merge(production);
			refreshList(true);
		}
		return null;
	}

	public String actionUpgrade() {
		try {
			if (production != null) {
				String phoneExt = Constants.LS_SUPPORT_PHONE + ", " + Constants.LS_MARKETING_EXT;
				if(SessionUtils.isTTCOnline()) {
					phoneExt = Constants.TTC_ONLINE_SUPPORT_PHONE;
				}
				String msg  = null;

				if (production.getType().getEpisodic()) {
					PopupBean conf = PopupBean.getInstance();

					msg = MsgUtils.formatMessage("Production.Upgrade.TV.Text", phoneExt);
					conf.show(this, 0,
							"Production.Upgrade.TV.Title",
							"Confirm.Close",
							null);
					conf.setMessage(msg);
				}
				else {
					PopupBean conf = PopupBean.getInstance();

					msg = MsgUtils.formatMessage("Production.Upgrade.Feature.Text", phoneExt);
					conf.show(this, ACT_UPGRADE,
							"Production.Upgrade.Feature.Title",
							"Production.Upgrade.Feature.Ok",
							"Confirm.Close");
					conf.setMessage(msg);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private String actionUpgradeOk() {
		Product product = ProductDAO.getInstance().findOneByProperty(ProductDAO.SKU, Constants.UPGRADE_SKU);
		if (product != null && production != null) {
			SessionUtils.put(Constants.ATTR_SELECTED_PRODUCT_ID, product.getId());
			SessionUtils.put(Constants.ATTR_UPGRADE_PRODUCTION_ID, production.getId());
			return "createproduction";
		}
		return null;
	}

	public String actionResubscribe() {
		Product product = ProductDAO.getInstance().findOneByProperty(ProductDAO.SKU, production.getSku());
		if (product != null && production != null) {
			SessionUtils.put(Constants.ATTR_SELECTED_PRODUCT_ID, product.getId());
			SessionUtils.put(Constants.ATTR_UPGRADE_PRODUCTION_ID, production.getId());
			return "createproduction";
		}
		return null;
	}

	public String actionUnsubscribe() {
		try {
			if (production != null) {
				PopupBean conf = PopupBean.getInstance();
				conf.show(this, ACT_UNSUBSCRIBE,
						"Production.Unsubscribe.Title",
						"Production.Unsubscribe.Text",
						"Production.Unsubscribe.Ok",
						"Confirm.Cancel");
//				String msg = MsgUtils.formatMessage("Production.Unsubscribe.Text",
//						production.getTitle());
//				conf.setMessage(msg);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * User clicked "OK" on the Unsubscribe pop-up.  Expire the production, and
	 * notify Lightspeed administration of the cancellation.
	 */
	private String actionUnsubscribeOk() {
		try {
			if (production != null) {
				production.setEndDate(production.getNextBillDate());
				production = getProductionDAO().merge(production);
				DoNotification.getInstance().productionUnsubscribed(production);
				refreshList(true);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method when a user clicks on the name of a Production, which sends
	 * them to the Home page for the production. Note that the JSF for each
	 * commandLink (row) includes:<br>
	 * <f:setPropertyActionListener value="#{item.id}" target=
	 * "#{productionContactBean.contactId}"/> <br>
	 * which will set our {@link #contactId} field to the {@link Contact#id}
	 * value of the Contact that corresponds to the production entry clicked.
	 *
	 * @return navigation string, to My Home, or empty if the logon failed, or
	 *         this method has already done a navigateUrl to the appropriate
	 *         page. Refactored by ESS-1513.
	 */
	public String actionLogon() {
		LOG.debug("");
		try {
			if (contactId != null) {
				contact = getContactDAO().findById(contactId); // check status - deleted? no-access?
				String url = SessionUtils.getString(Constants.ATTR_SAVED_PAGE_INFO);
				String ret = SessionUtils.logIntoProduction(contact, url); // ESS-1513
				if (ret == null) {
					refreshList(true); // production membership must have changed
					MsgUtils.addFacesMessage("ProdSelect.NoAccess", FacesMessage.SEVERITY_ERROR);
					ret = null;
				}
				else if (ret.length() == 0) { // navigation was done to URL
					ret = null; // so JSF shouldn't do any routing.
				}
				return ret; // null, or JSF navigation string to "home"
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called to place a mobile user into a Production when they click on the
	 * button with a production name. The productionId field was set by the JSP
	 * code.
	 *
	 * @return The appropriate navigation string -- empty if there was an error
	 *         and the user should remain on this page, else the string to
	 *         navigate to the "Proceed" page.
	 */
	public String actionEnterMobileProduction() {
		boolean ret = false;
		String nav = null;
		if (productionId != null) {
			SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force tcUser to default
			SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
			production = getProductionDAO().findById(productionId);
			if (production != null) {
				if (production.getOrderStatus().equals(OrderStatus.PENDING)) {
					// Payment has not been completed
					MsgUtils.addFacesMessage("ProdSelect.ProdPending", FacesMessage.SEVERITY_ERROR);
				}
				else {
					Contact ct = ContactDAO.getInstance()
							.findByUserProduction(SessionUtils.getCurrentUser(), production);
					if (ct != null) {
						if (ct.getStatus() == MemberStatus.ACCEPTED) {
							SessionUtils.setCurrentContact(ct);
							SessionSetUtils.setProduction(production);
							ret = LoginBean.checkProjectAccess(ct);
							if (! ret) {
								SessionUtils.setCurrentContact(null);
								SessionSetUtils.setProduction(null);
								MsgUtils.addFacesMessage("ProdSelect.NoAccess", FacesMessage.SEVERITY_ERROR);
							}
						}
						else if (ct.getStatus() == MemberStatus.NO_ACCESS) {
							// Active member, no access privileges"
							MsgUtils.addFacesMessage("ProdSelect.NoAccess", FacesMessage.SEVERITY_ERROR);
						}
						else if (ct.getStatus() == MemberStatus.NO_ROLES) {
							// Active member, no roles currently assigned
							MsgUtils.addFacesMessage("ProdSelect.NoRoles", FacesMessage.SEVERITY_ERROR);
						}
						else if (ct.getStatus() == MemberStatus.PENDING) {
							if (! production.getStatus().equals(AccessStatus.ACTIVE)) {
								// production status does not allow accept/decline action
								MsgUtils.addFacesMessage("ProdSelect.NotActive", FacesMessage.SEVERITY_ERROR);
							}
							else {
								// Accept or Decline the invitation...
								productionTitle = production.getTitle();
								SessionUtils.put(Constants.ATTR_SELECT_CONTACT_ID, ct.getId());
								nav = "acceptm"; // navigate to the mobile accept/decline page
							}
						}
						else { // BLOCKED or DECLINED or unknown
							// Inactive - invitation refused
							MsgUtils.addFacesMessage("ProdSelect.Declined", FacesMessage.SEVERITY_ERROR);
						}
					}
				}
			}
		}
		if (ret) {
			// set attribute so PageAuthenticatePhaseListener lets us in!
			SessionUtils.put(Constants.ATTR_ENTERING_PROD, 1);
			nav = "productionm";
		}
		return nav;
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_ACCEPT:
					res = actionAcceptOk();
					break;
				case ACT_DECLINE:
					res = actionDeclineOk();
					break;
				case ACT_DELETE_SECOND_PROD:
					res = actionDeleteSecondOk();
					break;
				case ACT_DEACTIVATE:
					res = actionDeactivateOk();
					break;
				case ACT_REACTIVATE:
					res = actionReactivateOk();
					break;
				case ACT_UNSUBSCRIBE:
					res = actionUnsubscribeOk();
					break;
				case ACT_UPGRADE:
					res = actionUpgradeOk();
					break;
				default:
					res = super.confirmOk(action);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return res;
	}

	/**
	 * Determines the default ascending/descending order for the given column name.
	 *
	 * @param sortColumn Name of the column for which the default order is requested.
	 * @return whether sortColumn's default order is ascending or descending.
	 */
	@Override
	public boolean isDefaultAscending(String sortColumn) {
		if (sortColumn.equals(Production.SORTKEY_START)) {
			return false;  // start-date - default to descending order
		}
		return true;	// all other columns default to ascending
	}

	/**
	 * Apply the current filter text to the given list of contacts, and return a
	 * filtered list. Note that if the filter results in no entries left, this
	 * method will shorten the filter one character at a time, until the
	 * resulting list has at least one contact in it.
	 *
	 * @param contacts The list to be filtered.
	 * @return A non-null, but possibly empty, list of contacts in which the
	 *         Production.title field contains the filter text (in any position,
	 *         ignoring case). Note that an empty list will only be returned if
	 *         the given list is empty.
	 */
	private List<Contact> applyFilter(List<Contact> contacts) {
		List<Contact> filtered;
		if (filter == null || filter.length() == 0) {
			filtered = contacts;
		}
		else {
			filtered = new ArrayList<>();
			do {
				String filt = filter.toLowerCase();
				for (Contact ct : contacts) {
					ct.setSelected(false);
					if (ct.getProduction().getTitle().toLowerCase().contains(filt)) {
						filtered.add(ct);
					}
				}
				if (filtered.size() == 0) {
					filter = filter.substring(0, filter.length()-1);
				}
			} while (filtered.size() == 0 && filter.length() > 0);
			if (filter.length() == 0) {
				filtered = contacts;
			}
		}
//		UserPrefBean.getInstance().put(Constants.ATTR_PROD_FILTER, filter);
		return filtered;
	}

	/**
	 * Determine the shooting day/status information to be displayed.
	 *
	 * @param production The production of interest.
	 * @return A String representing the shooting status of the first Unit of
	 *         the production's default project. This may be "Post" if shooting
	 *         is completed, or "n / m" where n is the shooting day number (0 if
	 *         shooting hasn't started) and m is the number of shooting days in
	 *         the schedule.
	 */
	private static String calculateDayStatus(Production production) {
		String status = null;
		Project useProject = production.getDefaultProject();
		if (useProject != null) {
			Unit unit = useProject.getMainUnit();
			ProjectSchedule schedule = unit.getProjectSchedule();
			Date today = new Date();
			ScheduleUtils su = new ScheduleUtils(unit);
			if (! su.getEndDate().after(today)) {
				status = "Post"; // production is finished
			}
			else {
				if (schedule.getStartDate().after(today)) {
					status = "0"; // production hasn't started yet
				}
				else {
					int n = su.getCurrentShootDayNumber();
					if (n == 0) { // a non-working day, get the prior workday's number
						Calendar prev = su.findPreviousWorkDate(Calendar.getInstance());
						if (prev != null) {
							n = su.findShootingDayNumber(prev.getTime());
						}
					}
					status = "" + n;
				}
				status += " / " + su.getShootingDatesList().size();
			}
		}
		return status;
	}

	/**
	 * Create the drop-down list of selections for the Employer Of Record.
	 * @return List of SelectItem's.
	 */
	private static List<SelectItem> createEorSelectList() {
		List<SelectItem> list = new ArrayList<>();
		SelectItem selectitem;
		list.add(new SelectItem(null, "(none)"));
		for (EmployerOfRecord p : EmployerOfRecord.values()) {
			if (p == EmployerOfRecord.NONE) {
				break;
			}
			selectitem = new SelectItem(p, p.getPrompt());
			list.add(selectitem);
		}
		return list;
	}

//	private ProductionPhase calculatePhase(Production production) {
//		ProductionPhase phase = ProductionPhase.UNKNOWN;
//		Project useProject = production.getDefaultProject();
//		if (useProject != null) {
//			Unit unit = useProject.getMainUnit();
//			ProjectSchedule schedule = unit.getProjectSchedule();
//			Date today = new Date();
//			if (schedule.getStartDate().after(today)) {
//				phase = ProductionPhase.PRE_PRODUCTION;
//			}
//			else {
//				ScheduleUtils su = new ScheduleUtils(unit);
//				phase = su.getPhase();
//			}
//		}
//		return phase;
//	}

	/**
	 * Save our sort column in the User preferences, so it will be saved across
	 * logout/login.
	 *
	 *
	 * @see com.lightspeedeps.web.view.ListView#setSortColumnName(java.lang.String)
	 */
	@Override
	public void setSortColumnName(String name) {
		UserPrefBean.getInstance().put(ATTR_SORT_NAME_PREFIX + getSortAttributePrefix(), name);
		super.setSortColumnName(name);
	}

	/**
	 * Called when sorting our list -- make sure that we don't get
	 * LazyInitializationExceptions due to the key value being stale.
	 *
	 * @see com.lightspeedeps.web.view.ListView#sort()
	 */
	@Override
	protected void sort() {
		LOG.debug("");
		ProductionDAO pd = ProductionDAO.getInstance();
		for (Contact c : contactList) {
			Production p = c.getProduction();
			try {
				p.getScheduleStartDate();
			}
			catch (Exception e) {
				p = pd.refresh(p);
				p.getScheduleStartDate();
				c.setProduction(p);
			}
		}
		super.sort();
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Comparator getComparator() {
		String col = getSortColumnName();
		if (col.equalsIgnoreCase(Production.SORTKEY_NAME) ||
				col.equalsIgnoreCase(Production.SORTKEY_START)) {
			Comparator<Contact> comparator = new Comparator<Contact>() {
				@Override
				public int compare(Contact c1, Contact c2) {
					return c1.getProduction().compareTo(c2.getProduction(), getSortColumnName(), isAscending());
				}
			};
			return comparator;
		}
		else {
			Comparator<Contact> comparator = new Comparator<Contact>() {
				@Override
				public int compare(Contact c1, Contact c2) {
					return c1.compareTo(c2, getSortColumnName(), isAscending());
				}
			};
			return comparator;
		}
	}

	public boolean getIsOwner() {
		if (production != null && contact != null &&
				contact.getUser().getAccountNumber().equals(production.getOwningAccount())) {
			return true;
		}
		return false;
	}

	/**See {@link #TAB_CONTACT_DETAIL}. */
	@Override
	public int getTabDetail() {
		return TAB_CONTACT_DETAIL;
	}

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #owner}. */
	public String getOwner() {
		return owner;
	}
	/** See {@link #owner}. */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	public List<SelectItem> getProductionStatusDL() {
		return Constants.PRODUCTION_STATUS_DL;
	}

	public List<SelectItem> getProductionSetTypeDL() {
		return Constants.PRODUCTION_STATUS_DL;
	}

	public List<SelectItem> getEmployerOfRecordList() {
		return employerOfRecordList;
	}

	public List<SelectItem> getExistencePolicyDL() {
		return existencePolicyList;
	}

	public List<SelectItem> getPdfGroupingTypeList() {
		return pdfGroupingTypeList;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getItemList() {
		return contactList;
//		return productionList;
	}

	/** See {@link #contactId}. */
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer logonId) {
		contactId = logonId;
	}

	/** See {@link #productionId}. */
	public Integer getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(Integer productionId) {
		this.productionId = productionId;
	}

	/** See {@link #productionTitle}. */
	public String getProductionTitle() {
		return productionTitle;
	}
	/** See {@link #productionTitle}. */
	public void setProductionTitle(String productionTitle) {
		this.productionTitle = productionTitle;
	}

	/** See {@link #isProdAdmin}. */
	public boolean getProdAdmin() {
		return isProdAdmin;
	}
	/** See {@link #isProdAdmin}. */
	public void setProdAdmin(boolean isProdAdmin) {
		this.isProdAdmin = isProdAdmin;
	}

//	/** See {@link #showAccept}. */
//	public boolean getShowAccept() {
//		return showAccept;
//	}
//	/** See {@link #showAccept}. */
//	public void setShowAccept(boolean showAccept) {
//		this.showAccept = showAccept;
//	}

	/** See {@link #pAGINATE_SIZE}.  For JSP access. */
	public int getPaginateSize() {
		return PAGINATE_SIZE;
	}
	public void setPaginateSize(int p) {
		// setter is just here to satisfy JSP; not used.
	}

	/**See {@link #paginate}. */
	public boolean getPaginate() {
		return paginate;
	}
	/**See {@link #paginate}. */
	public void setPaginate(boolean paginate) {
		this.paginate = paginate;
	}

	/** See {@link #copyAddress}. */
	public boolean getCopyAddress() {
		return copyAddress;
	}
	/** See {@link #copyAddress}. */
	public void setCopyAddress(boolean copyAddress) {
		this.copyAddress = copyAddress;
	}

	/** See {@link #copyHomePhone}. */
	public boolean getCopyHomePhone() {
		return copyHomePhone;
	}
	/** See {@link #copyHomePhone}. */
	public void setCopyHomePhone(boolean copyHomePhone) {
		this.copyHomePhone = copyHomePhone;
	}

	/** See {@link #copyBusinessPhone}. */
	public boolean getCopyBusinessPhone() {
		return copyBusinessPhone;
	}
	/** See {@link #copyBusinessPhone}. */
	public void setCopyBusinessPhone(boolean copyBusinessPhone) {
		this.copyBusinessPhone = copyBusinessPhone;
	}

	/** See {@link #copyCellPhone}. */
	public boolean getCopyCellPhone() {
		return copyCellPhone;
	}
	/** See {@link #copyCellPhone}. */
	public void setCopyCellPhone(boolean copyCellPhone) {
		this.copyCellPhone = copyCellPhone;
	}

	/** See {@link #copyImage}. */
	public boolean getCopyImage() {
		return copyImage;
	}
	/** See {@link #copyImage}. */
	public void setCopyImage(boolean copyImage) {
		this.copyImage = copyImage;
	}

	/** See {@link #enableCopyAddress}. */
	public boolean getEnableCopyAddress() {
		return enableCopyAddress;
	}
	/** See {@link #enableCopyAddress}. */
	public void setEnableCopyAddress(boolean enableCopyAddress) {
		this.enableCopyAddress = enableCopyAddress;
	}

	/** See {@link #enableCopyHomePhone}. */
	public boolean getEnableCopyHomePhone() {
		return enableCopyHomePhone;
	}
	/** See {@link #enableCopyHomePhone}. */
	public void setEnableCopyHomePhone(boolean enableCopyHomePhone) {
		this.enableCopyHomePhone = enableCopyHomePhone;
	}

	/** See {@link #enableCopyBusinessPhone}. */
	public boolean getEnableCopyBusinessPhone() {
		return enableCopyBusinessPhone;
	}
	/** See {@link #enableCopyBusinessPhone}. */
	public void setEnableCopyBusinessPhone(boolean enableCopyBusinessPhone) {
		this.enableCopyBusinessPhone = enableCopyBusinessPhone;
	}

	/** See {@link #enableCopyCellPhone}. */
	public boolean getEnableCopyCellPhone() {
		return enableCopyCellPhone;
	}
	/** See {@link #enableCopyCellPhone}. */
	public void setEnableCopyCellPhone(boolean enableCopyCellPhone) {
		this.enableCopyCellPhone = enableCopyCellPhone;
	}

	/** See {@link #enableCopyImage}. */
	public boolean getEnableCopyImage() {
		return enableCopyImage;
	}
	/** See {@link #enableCopyImage}. */
	public void setEnableCopyImage(boolean enableCopyImage) {
		this.enableCopyImage = enableCopyImage;
	}

	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	/** See {@link #myProductionHeaderTitle}. */
	public String getMyProductionHeaderTitle() {
		return myProductionHeaderTitle;
	}

	/** See {@link #myProductionHeaderTitle}. */
	public void setMyProductionHeaderTitle(String myProductionHeaderTitle) {
		this.myProductionHeaderTitle = myProductionHeaderTitle;
	}

	/** See {@link #productionHeaderTitle}. */
	public String getProductionHeaderTitle() {
		return productionHeaderTitle;
	}

	/** See {@link #productionHeaderTitle}. */
	public void setProductionHeaderTitle(String productionHeaderTitle) {
		this.productionHeaderTitle = productionHeaderTitle;
	}

	/** See {@link #i9DesclosureText}. */
	public String getI9DisclosureText() {
		return i9DisclosureText;
	}

	/** See {@link #i9DesclosureText}. */
	public void setI9DisclosureText(String i9DisclosureText) {
		this.i9DisclosureText = i9DisclosureText;
	}

}
