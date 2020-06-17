package com.lightspeedeps.web.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.TaxWageAllocationFormDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.model.TaxWageAllocationRow;
import com.lightspeedeps.model.TaxWageAllocationRowTemplate;
import com.lightspeedeps.model.User;
import com.lightspeedeps.service.DocumentTransferService;
import com.lightspeedeps.type.MemberStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TaxWageAllocationUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.report.ReportQueries;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.view.ListView;

/**
 * Manages the display, adding and editing of the Tax and Wage Allocation data
 * for Tours. Will primarily be used for end of year.
 */
@ManagedBean
@ViewScoped
public class TaxWageAllocationBean extends ListView implements PopupHolder {
//	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog("TaxWageAllocationBean");
	private static final long serialVersionUID = 1L;

	// SIT Alphanumeric types
//	private static final String ZERO = "ZERO";
//	private static final String CALC = "CALC";
//	private static final String FORCED_AMT = "FORCED AMOUNT";

	// Tax Year range.
	private static final int TAX_YEARS_START = 2016;
	private static final int TAX_YEARS_END = 2028;

	// LS-3116 and LS-3117
	private static final int SIT_TOTAL_WAGES_LIMIT = 15;
	private static final int SIT_FIVE_PERCENT_LIMIT = 5;
	private static final int SIT_NINE_PERCENT_LIMIT = 9;
	private static final double SIT_TAX_PERCENTAGE = .05;
	private static final double NY_SIT_TAX_PERCENTAGE = .09;
	private static final double FIT_TAX_PERCENTAGE = .15;
	private static final String STATE_INCOME_TAX = "SIT";
	private static final String FEDERAL_INCOME_TAX = "FIT";
	private static final String NY_STATE_CODE = "NY";

	// Actions
	private static final int ACTION_CREATE = 10;
	private static final int ACTION_DELETE = 11;
	private static final int ACTION_SELECT_PREVIOUS_ROW = 12;
	private static final int ACTION_TRANSMIT = 13;
	private static final int ACTION_CREATE_REVISION = 14;

	// DAO classes
//	private transient TaxWageAllocationRowDAO taxWageAllocationRowDAO;

	/** List of each row for the Tax/Wage table */
//	private List<TaxWageAllocationRow> taxWageAllocationFormRows;
	/** Current allocation form initially it will be blank */
	private TaxWageAllocationForm allocationForm;
	/** List of possible frequency types for dropdown */
	private List<SelectItem> frequencyDL;
	/** List of Tax Years to generate the form for. */
	private List<SelectItem> taxYearsDL;
	/** List of form revisions */
	private List<SelectItem> revisionsDL;
	/** Associated Production */
	private Production production;
	/** If resident of Washington DC. */
	private boolean isDC;
	/** If New York (NY) show the New York City checkbox */
//	private Boolean isNewYorkState;
	/** If the resident city is New York City. */
	private Boolean isNewYorkCity;
	/** Whether to show the New revisions button. It should only be shown when in edit
	 * mode and all of the revisions have been transmitted.
	 */
	private boolean showNewRevisionBtn;
	/**
	 * Total of wages input per individual city/state. This must match the total
	 * wages entered at the top of the page or an error message is generated.
	 */
	private BigDecimal sumOfIndividualWages;
	/**
	 * Total of City/State taxes input for individual City/State. Total taxes
	 * cannot exceed total wages.
	 */
	private BigDecimal sumOfIndividualTaxes;
	/** List of allocation forms for this production */
	private List<TaxWageAllocationForm> allocationForms;
	/** List of wages for each row of the table */
	private List<BigDecimal> indivWages;
	/** List of taxes for each individual row. */
	private List<BigDecimal> indivTaxes;
	/** List of contacts for this production */
	private List<Contact> contacts;
	/** List of form revisions for this contact and production */
	private List<TaxWageAllocationForm> revisions;
	/**
	 * New allocation form contact. This can be an existing contact in the
	 * production or a new one. Used when user clicks on "Create Form" bttn.
	 */
	private Contact newFormContact;;
	/** Number of the row selected */
	private int rowNum;
	private int prevRowNum;
	/** Contact selected from list. */
	private ContactAllocationForms selectedFormWrapper;
	/** Index of selected revision. */
	private Integer revisionIndex;
	/** Id of the new Allocation Form revision. If it is 0, then we are creating a new blank form,
	 * otherwise we create a new Allocation Form as a deep copy of an existing form based on this
	 * id.
	 */
	private Integer newRevisionId;
	/**
	 * Collection Contact wrapper containing all of the associated
	 * Allocation Forms.
	 */
	private List<ContactAllocationForms> contactForms;
	/** Show the delete dialog box */
	private boolean showDeleteDialog;
	/** Show the transmit dialog box */
	private boolean showTransmitDialog;
	/** Show the warning dialog box */
	private boolean showWarningDialog;
	/**
	 * To determine whether tax liability has been met.
	 * Tax liability is not met in the following conditions:
	 * 1) If FIT equals 0, "ZERO" or is less than 15% of total federal gross wages.
	 * 2) If SIT amount equals 0 or "Zero"
	 * 3) If state of residence is CA and there are wages allocation to CA, SIT must be
	 *    at least 5%.
	 */
	private boolean taxLiabilityMet;
	/** Message to display on transfer popup if tax liability has not been met. */
	private String transferLiabilityNotMetMsg;
	/** Soft warning messages to display on the Transmit popup*/
	private Set<String> errorMessages = new HashSet<>();

	/** used to get tax wage allocation current row */
	private TaxWageAllocationRow taxWagesRow;

	// DAO classes
	private transient ContactDAO contactDAO;
	private transient TaxWageAllocationFormDAO taxWageAllocationFormDAO;
	private transient UserDAO userDAO;
	private transient ProductionDAO productionDAO;

	public static TaxWageAllocationBean getInstance() {
		return (TaxWageAllocationBean)ServiceFinder.findBean("taxWageAllocationBean");
	}

	public TaxWageAllocationBean() {
		super(TaxWageAllocationForm.SORTKEY_NAME, "Tours.Allocation");
		setup();
	}

	/** See {@link #contactForms}. */
	public List<ContactAllocationForms> getContactForms() {
		return contactForms;
	}

	/** See {@link #contactForms}. */
	public void setContactForms(List<ContactAllocationForms> contactForms) {
		this.contactForms = contactForms;
	}

//
//	/** See {@link #taxWageAllocationFormRows}. */
//	public List<TaxWageAllocationRow> getTaxWageAllocationFormRows() {
//		if(taxWageAllocationFormRows == null) {
//			createTaxWageAllocationRows();
//		}
//		return taxWageAllocationFormRows;
//	}
//
//	/** See {@link #taxWageAllocationFormRows}. */
//	public void setTaxWageAllocationFormRows(List<TaxWageAllocationRow> taxWageAllocationFormRows) {
//		this.taxWageAllocationFormRows = taxWageAllocationFormRows;
//	}
//
	/** See {@link #allocationForm}. */
	public TaxWageAllocationForm getAllocationForm() {
		return allocationForm;
	}

	/** See {@link #allocationForm}. */
	public void setAllocationForm(TaxWageAllocationForm allocationForm) {
		this.allocationForm = allocationForm;
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}

	/** See {@link #revisionIndex}. */
	public Integer getRevisionIndex() {
		return revisionIndex;
	}

	/** See {@link #revisionIndex}. */
	public void setRevisionIndex(Integer revisionIndex) {
		this.revisionIndex = revisionIndex;
	}

	/** See {@link #newRevisionId}. */
	public Integer getNewRevisionId() {
		return newRevisionId;
	}

	/** See {@link #newRevisionId}. */
	public void setNewRevisionId(Integer newRevisionId) {
		this.newRevisionId = newRevisionId;
	}

	/** See {@link #taxLiabilityMet}. */
	public boolean getTaxLiabilityMet() {
		return taxLiabilityMet;
	}

	/** See {@link #transferLiabilityNotMetMsg}. */
	public String getTransferLiabilityNotMetMsg() {
		return transferLiabilityNotMetMsg;
	}

	/** See {@link #frequencyDL}. */
	public List<SelectItem> getFrequencyDL() {
		if (frequencyDL == null) {
			frequencyDL = EnumList.getTaxWageAllocationFrequencyList();
		}
		return frequencyDL;
	}

	/** See {@link #taxYearsDL}. */
	public List<SelectItem> getTaxYearsDL() {
		if (taxYearsDL == null) {
			taxYearsDL = TaxWageAllocationUtils.getTaxYearsDL(TAX_YEARS_START, TAX_YEARS_END,
					Calendar.getInstance().get(Calendar.YEAR));
		}

		return taxYearsDL;
	}

	/** See {@link #revisionsDL}. */
	public List<SelectItem> getRevisionsDL() {
		if (revisionsDL == null) {
			createRevisionsDL();
		}
		return revisionsDL;
	}

	/** See {@link #isNewYorkState}. */
	public Boolean getIsNewYorkState() {
		String stateCode = allocationForm.getResidentState();
		return (stateCode != null && stateCode.equals("NY"));
	}

	/** See {@link #isNewYorkState}. */
//	public void setIsNewYorkState(Boolean isNewYorkState) {
//		this.isNewYorkState = isNewYorkState;
//	}

	/** See {@link #isNewYorkCity}. */
	public Boolean getIsNewYorkCity() {
		return isNewYorkCity;
	}

	/** See {@link #isNewYorkCity}. */
	public void setIsNewYorkCity(Boolean isNewYorkCity) {
		this.isNewYorkCity = isNewYorkCity;
	}

	/** See {@link #isDC}. */
	public boolean getIsDC() {
		return isDC;
	}

	/** See {@link #showNewRevisionBtn}. */
	public boolean getShowNewRevisionBtn() {
		return showNewRevisionBtn;
	}

	/** See {@link #sumOfIndividualWages}. */
	public BigDecimal getSumOfIndividualWages() {
		return sumOfIndividualWages;
	}

	/** See {@link #sumOfIndividualWages}. */
	public void setSumOfIndividualWages(BigDecimal sumOfIndividualWages) {
		this.sumOfIndividualWages = sumOfIndividualWages;
	}

	/** See {@link #newFormContact}. */
	public void setNewFormContact(Contact newFormContact) {
		this.newFormContact = newFormContact;
	}

	/** See {@link #showDeleteDialog}. */
	public boolean getShowDeleteDialog() {
		return showDeleteDialog;
	}

	/** See {@link #showTransmitDialog}. */
	public boolean getShowTransmitDialog() {
		return showTransmitDialog;
	}

	/** See {@link #allocationForms}. */
	public List<TaxWageAllocationForm> getAllocationForms() {
		return allocationForms;
	}

	/** See {@link #allocationForms}. */
	public void setAllocationForms(List<TaxWageAllocationForm> allocationForms) {
		this.allocationForms = allocationForms;
	}

	/** See {@link #contacts}. */
	public List<Contact> getContacts() {
		return contacts;
	}

	/** See {@link #contacts}. */
	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	/** See {@link #errorMessage}. */
	public Set<String> getErrorMessages() {
		return errorMessages;
	}

	/** See {@link #taxWageAllocationFormDAO}. */
	private TaxWageAllocationFormDAO getTaxWageAllocationFormDAO() {
		if (taxWageAllocationFormDAO == null) {
			taxWageAllocationFormDAO = TaxWageAllocationFormDAO.getInstance();
		}
		return taxWageAllocationFormDAO;
	}

	/** See {@link #contactDAO}. */
	private ContactDAO getContactDAO() {
		if (contactDAO == null) {
			contactDAO = ContactDAO.getInstance();
		}
		return contactDAO;
	}

	/** See {@link #userDAO}. */
	private UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

	private ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}

		return productionDAO;
	}

	/** Setup of this instance */
	private void setup() {
		frequencyDL = null;
		production = SessionUtils.getProduction();
		isNewYorkCity = false;
		isDC = false;
		taxLiabilityMet = true;
		rowNum = 0;
		allocationForm = null;
		revisionsDL = null;
		revisionIndex = 0;
		transferLiabilityNotMetMsg = MsgUtils.getMessage("Tours.Allocation.Error.Need.Liability.Letter");
		// Get the list of allocation forms for this production.
		forceLazyInit();
		refreshList();
		Integer selectedId = SessionUtils.getInteger(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID);

		if(selectedId == null) {
			selectedId = -1;
		}
		setupSelectedItem(selectedId);

		restoreSortOrder();
		setScrollable(true);
		restoreScrollFromSession(); // try to maintain scrolled position
//		checkTab();

		scrollToRow(selectedFormWrapper);
	}

	/**
	 * Initialize the allocation form variables.
	 */
	private void initAllocationForm() {
		sumOfIndividualWages = Constants.DECIMAL_ZERO;
		sumOfIndividualTaxes = Constants.DECIMAL_ZERO;
		indivWages = new ArrayList<>();
		indivTaxes = new ArrayList<>();

		// If no updated date, then this is a new form
		if (allocationForm.getUpdated() == null) {
			// Create list of individual wages initialized to 0.00
			// Create list of individual taxes initialized to 0.00
			for (int i = 0; i < allocationForm.getAllocationFormRows().size(); i++) {
				indivWages.add(Constants.DECIMAL_ZERO);
				indivTaxes.add(Constants.DECIMAL_ZERO);
			}
		}
		else {
			// Existing form
			for (TaxWageAllocationRow row : allocationForm.getAllocationFormRows()) {
				BigDecimal wages = row.getWages();
				String taxes = row.getCalculateTax();

				if (wages == null) {
					wages = Constants.DECIMAL_ZERO;
					indivWages.add(Constants.DECIMAL_ZERO);
				}
				else {
					indivWages.add(wages);
				}
				sumOfIndividualWages = sumOfIndividualWages.add(wages);

				if (taxes != null && NumberUtils.isNumber(taxes)) {
					sumOfIndividualTaxes = sumOfIndividualTaxes.add(new BigDecimal(taxes));
				}
			}
		}
	}

	/**
	 * Determine which element we are supposed to view. If the id given is null
	 * or invalid, we try to display the "default" element.
	 *
	 * @param id
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);

		allocationForm = null;
		if (selectedFormWrapper != null) {
			selectedFormWrapper.setSelected(false);
		}

		if (id == null) {
			id = findDefaultId();
		}

		if (id == null) {
			rowNum = 0;
			return;
		}

		if (selectedFormWrapper == null && id == -1) {
			if (contactForms != null && ! contactForms.isEmpty()) {
				selectedFormWrapper = contactForms.get(rowNum);
			}
		}
		else {
			Contact ct = getContactDAO().findById(id);

			// Find the ContactForm wrapper for this new person from the list
			// and assign it to selectedFormWrapper.
			for(ContactAllocationForms formsWrapper : contactForms) {
				if(ct.equals(formsWrapper.getContact())) {
					selectedFormWrapper = formsWrapper;
					break;
				}
			}
//			selectedFormWrapper = new ContactAllocationForms();
//			selectedFormWrapper.setContact(ct);
//			List<TaxWageAllocationForm> forms =
//					getTaxWageAllocationFormDAO().findByContact(ct, "id desc");
//			selectedFormWrapper.setForms(forms);

		}

		if (selectedFormWrapper != null) {
			selectedFormWrapper.setSelected(true);
			// Set the selected row state for the datatable.
			RowState state = new RowState();
			state.setSelected(true);

			getStateMap().clear();
			getStateMap().put(selectedFormWrapper, state);

			revisionsDL = null;
			revisionIndex = 0;
			refreshRevisionsList();
			createRevisionsDL();

			if (allocationForm != null) {
				forceLazyInit();
				initAllocationForm();

				// Make sure the New York City check box is in the correct state.
				String city = allocationForm.getResidentCity();
				isNewYorkCity = (city != null && city.toLowerCase().equals(Constants.NEW_YORK_CITY));

				// Check if resident state is Washington DC
				if(allocationForm.getResidentState() == null) {
					isDC = false;
				}
				else {
					isDC = allocationForm.getResidentState().equals(Constants.DC_STATE);
				}
			}
		}
	}

	/**
	 * Return the id of the item that resides in the n'th row of the currently
	 * displayed list.
	 *
	 * @param row
	 * @return Returns null only if the list is empty.
	 */
	protected Integer getRowId(int row) {

		Object item = getRowItem(row);
		return (item == null ? null : ((ContactAllocationForms)item).getContact().getId());
	}

	/**
	 * RowSelector selectionListener method: invoked when user selects a row in
	 * the Project list. It is IMPERATIVE that the RowSelector tag include the
	 * 'immediate="false"' attribute for proper operation. Otherwise, in Edit
	 * mode the input fields do not get refreshed with data from the newly
	 * selected object.
	 *
	 * @param event The selection event, which includes the row number. We don't
	 *            use this information.
	 */
	@Override
	public void rowSelected(SelectEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}

		UIData ud = (UIData)event.getComponent().findComponent("empList");
		String baseId = ud.getClientId();
		Integer id;
		String index;

		HttpServletRequest request = SessionUtils.getHttpRequest();
		index = request.getParameter(baseId + Constants.ICEFACES_SELECTED_ROW);
		rowNum = Integer.parseInt(index);
		id = getRowId(rowNum);
		setupSelectedItem(id);

		editMode = false;
	}

	@Override
	public void listenRowClicked(SelectEvent event) {
		if (editMode) {
			PopupBean bean = PopupBean.getInstance();
			this.setSelectedRow(prevRowNum);
			bean.setMessage(MsgUtils.getMessage("Tours.Allocation.Error.FormInEditMode.Text"));
			bean.show(this, ACTION_SELECT_PREVIOUS_ROW, null, "Confirm.OK", null);
			this.setSelectedRow(prevRowNum);
			rowNum = prevRowNum;
		}
		log.debug("");
	}

	public void processEvent() {
		log.debug("");
	}

	@Override
	public String actionEdit() {
		forceLazyInit();

		return super.actionEdit();
	}

	@Override
	public String actionSave() {
		try {
			super.actionSave();
			allocationForm.setUpdated(new Date());
			allocationForm.setUpdatedBy(getvUser().getAccountNumber());
			String residentCity =  allocationForm.getResidentCity();
			String residentState = allocationForm.getResidentState();

			// If DC and/or NYC have values and the resident state is not DC and the resident city is not NYC,
			// we need to clear the wage and SIT values
			for(TaxWageAllocationRow row : allocationForm.getAllocationFormRows()) {
				if(row.getRowTemplate().getIsNewYorkCity()) {
					// In case they have changed the resident state to something other than NY or the state is NY
					// and the resident city was changed from NYC to something else we need to erase any values for
					// the NYC line.
					if(residentState == null || residentCity == null ||
							! residentState.equals(Constants.NEW_YORK_STATE) || ! residentCity.equalsIgnoreCase(Constants.NEW_YORK_CITY)) {
						row.setWages(null);
						row.setCalculateTax(null);
					}
				}
			}
			allocationForm = getTaxWageAllocationFormDAO().merge(allocationForm);
			initAllocationForm();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		addButtonClicked();
		return null;
	}

	/**
	 * Display popup for creating new allocation form contact.
	 *
	 * @return
	 */
	public String actionCreateForm() {
		CreateAllocationFormBean bean = CreateAllocationFormBean.getInstance();
		addButtonClicked();

		newFormContact = new Contact();
		newFormContact.setUser(new User());

		// Create a list of contacts in this production that are not LS Admins
		contacts = new ArrayList<>();
		List<Contact> prodContacts = getContactDAO().findByProductionActive(production);

		if (prodContacts != null && ! prodContacts.isEmpty()) {
			// Remove ls admin from list
			for (Contact contact : prodContacts) {
				boolean notAdmin = true;

				List<Employment> emps = contact.getEmployments();

				if (emps != null && ! emps.isEmpty()) {
					for (Employment emp : emps) {
						if (emp.getRole().isAdmin()) {
							// Skip the ls admin
							notAdmin = false;
							break;
						}
					}
				}
				if (notAdmin) {
					contacts.add(contact);
				}
			}
		}
		Collections.sort(contacts);
		bean.setNewFormContact(newFormContact);
		bean.setFirstName(null);
		bean.setLastName(null);
		bean.setSelectedContactId(- 1);
		bean.setContacts(contacts);
		bean.setAddNewContact(true);
		bean.setDisableAddBttn(true);
		bean.setContactAllocFormExists(false);
		bean.setContactsDL(null);
		bean.show(this, ACTION_CREATE, "Tours.Allocation.CreateForm.Title", "Confirm.OK",
				"Confirm.Cancel");
//		addFocus("empSelect");
		// Set the focus to the employee dropdown.
		// Icefaces 4.2 upgrade
//		com.icesoft.faces.context.effects.JavascriptContext.applicationFocus(FacesContext.getCurrentInstance(), "allocation:empSelect");

		return null;
	}

	/**
	 * Create the new Allocation Form
	 *
	 * @return
	 */
	private String actionCreateFormOk() {
		try {
			if (newFormContact == null) {
				return null; // Fixes LS-1447
			}
			production = getProductionDAO().refresh(production);
			production.getProjects().size();

			if (newFormContact.getId() == null) {
				// This is a new contact so persist to database before creating the new form.
				String fakeEmailAddress;
				fakeEmailAddress = EmailValidator.makeFakeEmail(newFormContact.getUser().getFirstName(), newFormContact.getUser().getLastName());

				newFormContact.getUser().setEmailAddress(fakeEmailAddress);
				User user = getUserDAO().createUser(newFormContact.getUser(), getvUser());
				user.getSocialSecurity();

				newFormContact.setProduction(production);
				newFormContact.setUser(user);
				newFormContact.setUserId(user.getId());
				newFormContact.setStatus(MemberStatus.PENDING);
				newFormContact.setCreatedBy(getvUser().getAccountNumber());
				newFormContact.setProject(production.getDefaultProject());
				newFormContact.setDisplayName(user.getDisplayName());
				newFormContact.setEmailAddress(fakeEmailAddress);

				getContactDAO().save(newFormContact);

			}
			allocationForm = TaxWageAllocationUtils.createNewAllocationForm("1", production,
					newFormContact, getvUser());
			getTaxWageAllocationFormDAO().save(allocationForm);

			contactForms = null;
			refreshList();

			SessionUtils.put(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID, newFormContact.getId());
			setupSelectedItem(newFormContact.getId());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return HeaderViewBean.PAYROLL_WAGE_TAX_ALLOCATION;
	}

	/**
	 * Put up the Create Revision popup
	 * @return
	 */
	public String actionCreateRevision() {
		CreateAllocationFormRevisionBean bean = CreateAllocationFormRevisionBean.getInstance();

		newRevisionId = new Integer(0);
		// Pass the current list of revisions to the popup. Used to populate the dropdown.
		bean.setRevisions(revisions);
		bean.setSelectedFormId(newRevisionId);
		bean.setRevisionsDL(null);
		bean.setTxt1(MsgUtils.formatMessage("Tours.Allocation.Revision.Txt1", allocationForm.getContact().getDisplayName()));

		bean.show(this, ACTION_CREATE_REVISION, "Tours.Allocation.CreateRevision.Title", "Tours_Allocation_CreateRevision_Ok", "Confirm.Cancel");
		// Set the focus to the revisions dropdown.
		//Icefaces 4.2 upgrade
//		JavascriptContext.applicationFocus(FacesContext.getCurrentInstance(), "allocation:revisions");

		return null;
	}

	/**
	 * Create a new allocation form revision based on the returned id. If the id is 0 then we create a new blank form. If
	 * not, we create a new form that is a deep copy of the selected revision.
	 * @return
	 */
	private String actionCreateRevisionOk() {
		try {
			if(newRevisionId.equals(0)) {
				allocationForm = TaxWageAllocationUtils.createNewAllocationForm("1", production, allocationForm.getContact(), getvUser());
			}
			else {
				TaxWageAllocationForm form = getTaxWageAllocationFormDAO().findById(newRevisionId);
				allocationForm = form.deepCopy();
				allocationForm.setTransmitted(false);
				allocationForm.setCreated(new Date());
				allocationForm.setCreatedBy(getvUser().getAccountNumber());
				allocationForm.setUpdated(new Date());
				allocationForm.setUpdatedBy(getvUser().getAccountNumber());
			}

			getTaxWageAllocationFormDAO().save(allocationForm);
			// Refresh the entries for the employee list so we get the correct count for Revisions for
			// each employee. The employee who this revision was created for was not having their revision
			// count updated.
			refreshList();
			initAllocationForm();
			refreshRevisionsList();
			createRevisionsDL();
			SessionUtils.put(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID, allocationForm.getContact().getId());
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return HeaderViewBean.PAYROLL_WAGE_TAX_ALLOCATION;
	}

	/**
	 * Display the Delete Form popup
	 */
	@Override
	public String actionDelete() {
		Contact contact = getContactDAO().refresh(allocationForm.getContact());
		String name = contact.getUser().getDisplayName();

		PopupBean deletePopup = PopupBean.getInstance();
		deletePopup.show(this, ACTION_DELETE, "Tours_Allocation_DeleteForm_Title",
				"Tours_Allocation_DeleteForm_Delete_Ok", "Confirm.Cancel");
		deletePopup.setMessage(MsgUtils.formatMessage("Tours_Allocation_DeleteForm_Text", name));

		showDeleteDialog = true;

		return null;
	}

	/**
	 * Delete the current allocation form as long as it has not been
	 * transmitted.
	 */
	@Override
	public String actionDeleteOk() {
		try {
			if (allocationForm != null && allocationForm.getId() != null) {
				getTaxWageAllocationFormDAO().delete(allocationForm);
				// See if this contact still has associated allocation forms.
				List<TaxWageAllocationForm> forms = getTaxWageAllocationFormDAO().findByContact(selectedFormWrapper.getContact(), null);
				if(forms == null || forms.isEmpty()) {
					// This contact has no more associated forms so they will removed from the
					// list of contacts with allocation forms. Set the row num to 0.
					rowNum = 0;
				}
				selectedFormWrapper = null;
				refreshList();
				if(contactForms != null && !contactForms.isEmpty()) {
					setupSelectedItem(-1);

					refreshRevisionsList();
					createRevisionsDL();
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
	 * Reset allocation form back default values.
	 */
	@Override
	public String actionCancel() {
		super.actionCancel();
		allocationForm = getTaxWageAllocationFormDAO().refresh(allocationForm);
		editMode = false;

		sumOfIndividualWages = Constants.DECIMAL_ZERO;
		sumOfIndividualTaxes = Constants.DECIMAL_ZERO;
		SessionUtils.put(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID, allocationForm.getContact().getId());

		return HeaderViewBean.PAYROLL_WAGE_TAX_ALLOCATION;
	}

	public String actionPrint() {
		try {
			Project project = ProjectDAO.getInstance().refresh(production.getDefaultProject());
			String sqlQuery = ReportQueries.taxWageAllocationQuery;
			ReportUtils.generateTaxWageAllocationReport(project,
					ReportBean.JASPER_TOURS_TAX_WAGE_ALLOCATION, sqlQuery, null, true, false,
					allocationForm.getId(), sumOfIndividualWages);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return null;
	}

	public String actionTransmit() {
		try{
			// If the form is not valid, do not transmit.
			if (! validateForm()) {
				return null;
			}
			PopupBean bean = PopupBean.getInstance();

			bean.show(this, ACTION_TRANSMIT, "Tours_Allocation_Transmit_Title",
					"Tours_Allocation_Transmit_Ok", "Confirm.Cancel");
			bean.setMessage(MsgUtils.getMessage("Tours_Allocation_Transmit_Message"));

			showTransmitDialog = true;
		}
		catch(Exception e) {
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}

		return null;
	}

	private String actionTransmitOk() {
		// Create the data file and transmit it to the AS400.
		// Also send notification to the batch email address from Payroll
		// Preferences and attach a pdf of the form.
		try {
			Project project = production.getDefaultProject();

			project = ProjectDAO.getInstance().refresh(project);
			project.getContacts().size();
			allocationForm = getTaxWageAllocationFormDAO().refresh(allocationForm);
			allocationForm.setTimeSent(new Date());
			boolean ret = DocumentTransferService.getInstance()
					.transmitTaxWageAllocationForm(allocationForm, project);

			if (ret) {
				if (allocationForm != null) {
					if (! allocationForm.getTransmitted()) {
						// If this form has not been previously transmitted, then we need
						// to set the transmitted form.
						allocationForm.setTransmitted(true);
						allocationForm.setRevisionDate(new Date());

						revisionIndex = 0;

						// Make sure the New York City check box is in the correct state.
						String city = allocationForm.getResidentCity();
						isNewYorkCity = (city != null && city.toLowerCase().equals(Constants.NEW_YORK_CITY));
						refreshRevisionsList();
						createRevisionsDL();
					}
					getTaxWageAllocationFormDAO().attachDirty(allocationForm);
				}
			}
			else {
				// No payroll service assigned.
				MsgUtils.addFacesMessage("Tours.Allocation.Error.Missing.PayrollService",
						FacesMessage.SEVERITY_ERROR);
			}
			SessionUtils.put(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID, allocationForm.getContact().getId());
		}
		catch(Exception e) {
			log.debug("Possibly no Payroll Service assigned.\n" + e);
			MsgUtils.addGenericErrorMessage();
			EventUtils.logError(e);
		}

		return HeaderViewBean.PAYROLL_WAGE_TAX_ALLOCATION;
	}

	/**
	 * If NY is selected, show the New York City checkbox.
	 *
	 * @param event
	 */
	public void listenResidentStateChange(ValueChangeEvent event) {
		String state = (String)event.getNewValue();

		if (state != null) {
			allocationForm.setResidentState(state);

			UIInput inputText =
					(UIInput)event.getComponent().findComponent("residentCity");
			HtmlSelectBooleanCheckbox checkBox = (HtmlSelectBooleanCheckbox)event.getComponent()
					.findComponent("newYorkCityCheckBox");

			if (state.equals("NY")) {
//				isNewYorkCity = false;
			}
			else {
				if (isNewYorkCity) {
					// Coming from New York City so clear the city input.
					inputText.setValue("");
				}
//				isNewYorkState = false;
				isNewYorkCity = false;
			}
			if(state.equals(Constants.DC_STATE)) {
				isDC = true;
			}
			else {
				isDC = false;
			}

			// Clear out SIT fields for NYC or DC if they are not selected.
			for(TaxWageAllocationRow row : allocationForm.getAllocationFormRows()) {
				TaxWageAllocationRowTemplate template = row.getRowTemplate();
				String templateCity = template.getCity();
				String templateStateCode = template.getStateCode();
				if (templateStateCode.equals(Constants.NEW_YORK_STATE) && templateCity != null) {
					row.setCalculateTax(null);
				}
			}
			((HtmlInputText)inputText).setDisabled(false);
			checkBox.setValue(false);
		}
	}

	/**
	 * If checked, CALCULATE is inserted into SIT cell for this row and is
	 * disabled. If unchecked the SIT cell is blank and enabled.
	 *
	 * @param event
	 */
	public void listenCalculateCheckBoxChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData twData = (UIData)event.getComponent().findComponent("twTable");

		if (twData != null) {
			TaxWageAllocationRow rowData = (TaxWageAllocationRow)twData.getRowData();

			HtmlSelectBooleanCheckbox cb =
					(HtmlSelectBooleanCheckbox)event.getComponent().findComponent("calcTax");
			HtmlInputText inputText =
					(HtmlInputText)event.getComponent().findComponent("indivTaxes");
			if (cb != null) {
				Boolean calculateTax = (Boolean)event.getNewValue();
				String value = "CALCULATE";
				if (! calculateTax) {
					value = "";
				}
				else {
					// Check the value of the input text component.
					String inputValue = (String)inputText.getValue();
					if (NumberUtils.isNumber(inputValue)) {
						sumOfIndividualTaxes =
								sumOfIndividualTaxes.subtract(new BigDecimal(inputValue));
					}

				}
				if (rowData != null) {
					rowData.setCalculateTax(value);
				}
				inputText.setValue(value);
			}
		}
	}

	/**
	 * If checked, CALCULATE is inserted into SIT cell for this row and is
	 * disabled. If unchecked the SIT cell is blank and enabled.
	 *
	 * @param event
	 */
	public void listenCalculateFITCheckBoxChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		UIData twData = (UIData)event.getComponent().findComponent("twTable");

		if (twData != null) {

			HtmlInputText inputText = (HtmlInputText)event.getComponent().findComponent("fitTax");
			Boolean calculateTax = (Boolean)event.getNewValue();
			String value = (String)inputText.getValue();
			if (calculateTax) {
				value = "CALCULATE";
			}

			inputText.setValue(value);
			allocationForm.setCalculateFit(value);
		}
	}

	/**
	 * Only for New York state. If checked, add "New York City" to the city
	 * input field and disable it. If unchecked, clear out the input field and
	 * enable it.
	 *
	 * @param event
	 */
	public void listenNewYorkCityCheckChange(ValueChangeEvent event) {
		isNewYorkCity = (Boolean)event.getNewValue();

		if (isNewYorkCity != null) {
			HtmlInputText inputText =
					(HtmlInputText)event.getComponent().findComponent("residentCity");

			if (isNewYorkCity) {
				inputText.setValue(MsgUtils.getMessage("Tours_Allocation_Resident_New_York_City"));
				inputText.setDisabled(true);
			}
			else {
				inputText.setValue("");
				inputText.setDisabled(false);
			}
		}
	}

	/**
	 * Change to the Total Wage field.
	 *
	 * @param event
	 */
//	public void listenTotalWageChange(ValueChangeEvent event) {
//		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
//			// simpler to schedule event for later - after "setXxxx()" are called from framework
//			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
//			event.queue();
//			return;
//		}
//
//		BigDecimal oldValue = (BigDecimal)event.getOldValue();
//		BigDecimal newValue = (BigDecimal)event.getNewValue();
//
//		log.debug(oldValue);
//	}

	/**
	 * Change has been made to an individual wage field so we need to
	 * recalculate the running total wage field.
	 *
	 * @param event
	 */
	public void listenIndivdualWageChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		HtmlInputText comp = (HtmlInputText)event.getComponent().findComponent("indivWages");

		BigDecimal oldValue = (BigDecimal)event.getOldValue();
		BigDecimal newValue = (BigDecimal)event.getNewValue();

		if(oldValue != null) {
			oldValue = oldValue.setScale(2, RoundingMode.HALF_UP);
		}
		if(newValue != null) {
			newValue = newValue.setScale(2, RoundingMode.HALF_UP);
			comp.setValue(newValue);
		}

		// The first time entering a cell without data, the oldValue will be null.
		if (oldValue == null) {
			if (newValue != null) {
				double dNew = newValue.doubleValue();

				if (dNew < 0.0) {
					HtmlInputText inputText =
							(HtmlInputText)event.getComponent().findComponent("indivWages");
					// Do not allow negative numbers;
					inputText.setValue(BigDecimal.ZERO);
					inputText.setOnfocus("indivWages");
					return;
				}
				// Add to the running wage total.
				sumOfIndividualWages = sumOfIndividualWages.add(newValue);
			}
		}
		else if (oldValue != null) {
			if (newValue == null || newValue.equals(BigDecimal.ZERO)) {
				// If the user has removed the entry or set it to 0,
				// then subtract the previous amount from the running total.
				sumOfIndividualWages = sumOfIndividualWages.subtract(oldValue);
			}
			else if (newValue != null) {
				double dOld = oldValue.doubleValue();
				double dNew = newValue.doubleValue();

				if (dNew < 0.0) {
					HtmlInputText inputText =
							(HtmlInputText)event.getComponent().findComponent("indivWages");
					// Do not allow negative numbers;
					inputText.setValue(BigDecimal.ZERO);
					inputText.setOnfocus("indivWages");
					return;
				}
				else if (dOld == dNew) {
					// Framework seems to call the listener twice so if old and new values
					// are the same, just return;
					return;
				}
				else if (dNew < dOld) {
					sumOfIndividualWages =
							sumOfIndividualWages.subtract(oldValue.subtract(newValue));
				}
				else {
					sumOfIndividualWages = sumOfIndividualWages.add(newValue.subtract(oldValue));
				}
			}

		}
	}

	/**
	 * Change has been made to an individual tax field so we need to recalculate
	 * the running total wage field.
	 *
	 * @param event
	 */
	public void listenIndivdualTaxChange(ValueChangeEvent event) {
		//selected row used to check for entered amount is below 5% and 9%
		taxWagesRow = (TaxWageAllocationRow)event.getComponent().getAttributes().get("selectedRow");

		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		boolean isNewValueNumber = false;
		boolean isOldValueNumber = false;
		String oldValue = null;
		String newValue = null;
		if (event.getOldValue() != null) {
			oldValue = event.getOldValue().toString().replace(",", "");
			isOldValueNumber = NumberUtils.isNumber(oldValue);
		}
		if (event.getNewValue() != null) {
			newValue = event.getNewValue().toString().replace(",", "");
			isNewValueNumber = NumberUtils.isNumber(newValue);
		}

		// The first time entering a cell without data, the oldValue will be null.
		if (isOldValueNumber) {
			if (! isNewValueNumber) {
				sumOfIndividualTaxes = sumOfIndividualTaxes.subtract(new BigDecimal(oldValue));
			}
		}

		if (isNewValueNumber) {
			BigDecimal nValue = new BigDecimal(newValue);
			double dNew = nValue.doubleValue();
			if (isOldValueNumber) {
				BigDecimal oValue = new BigDecimal(oldValue);
				double dOld = oValue.doubleValue();

				if (dOld == dNew) {
					// Framework calls this method twice. The first call includes values from the
					// previous call. So if the old value and new value are the same, skip this call.
					return;
				}
				if (dOld < dNew) {
					sumOfIndividualTaxes = sumOfIndividualTaxes.add(nValue.subtract(oValue));
				}
				else {
					// If new value is 0.00 subtract the full amount of the old value. If not,
					// subtract the difference between the old value and the new value.
					if (dNew == 0D) {
						sumOfIndividualTaxes = sumOfIndividualTaxes.subtract(oValue);
					}
					else {
						sumOfIndividualTaxes =
								sumOfIndividualTaxes.subtract(oValue.subtract(nValue));
					}
				}
			}
			else {
				sumOfIndividualTaxes = sumOfIndividualTaxes.add(nValue);
			}
			// SIT amount validation for 5% rule (for all states except NY) and 9% rule (for NY state).
			validateSITAmount(taxWagesRow.getCalculateTax(), taxWagesRow.getWages());
		}
	}

	/**
	 * listener for entered SIT amount change and validation
	 *
	 * @param event
	 */
	public void listenSITAmountChange(ValueChangeEvent event) {
			String sitAmt = event.getNewValue().toString();
			validateSITAmount(sitAmt, getAllocationForm().getTotalWages());
	}

	/**
	 * It shows popup if the amount entered for SIT is lower than 5% (for All
	 * state) or 9%(for NY) of the amount entered in the corresponding wage
	 * field
	 *
	 * @param calAmt entered SIT amount
	 * @param wagesAmt wages amount
	 */
	public void validateSITAmount(String calAmt, BigDecimal wagesAmt) {
		if (calAmt != null) {
			calAmt = calAmt.replace(",", "");
			if (NumberUtils.isNumber(calAmt) && wagesAmt != null && wagesAmt.doubleValue() > 0.0) {
				double calcSit = Double.parseDouble(calAmt);
				NumberFormat numFormat = NumberFormat.getNumberInstance();
				numFormat.setGroupingUsed(true);
				numFormat.setMaximumFractionDigits(2);
				numFormat.setMinimumFractionDigits(2);
				BigDecimal calTaxAmt = new BigDecimal(calcSit);
				calAmt = numFormat.format(calcSit);
				BigDecimal val = calTaxAmt.divide(wagesAmt, 4, RoundingMode.HALF_UP);
				int percent = SIT_TOTAL_WAGES_LIMIT;
				String msgArg1 = STATE_INCOME_TAX;
				boolean showDialog = false;
				errorMessages = new HashSet<>();
				if (taxWagesRow != null) {
					String rowStateCode = taxWagesRow.getRowTemplate().getStateCode();
					String rowCity = taxWagesRow.getRowTemplate().getCity();
					boolean rowIsCanada = rowStateCode != null &&
							rowStateCode.equals(Constants.CANADA_STATE_CODE);
					boolean rowIsNY =
							rowStateCode != null && rowStateCode.equals(Constants.NEW_YORK_STATE);
					boolean rowIsNYC = (rowIsNY &&
							(rowCity != null && rowCity.equalsIgnoreCase(Constants.NEW_YORK_CITY)));

					if ((! rowIsCanada && val.doubleValue() < SIT_TAX_PERCENTAGE) ||
							(rowIsNY && ! rowIsNYC && val.doubleValue() < NY_SIT_TAX_PERCENTAGE)) {
						if (rowIsNY && ! rowIsNYC) {
							percent = SIT_NINE_PERCENT_LIMIT;
							msgArg1 = NY_STATE_CODE + " " + STATE_INCOME_TAX;
						}
						else {
							percent = SIT_FIVE_PERCENT_LIMIT;
						}
						showDialog = true;
						taxWagesRow.setCalculateTax(calAmt);
					}
				}
				else if (val.doubleValue() < FIT_TAX_PERCENTAGE) {
					showDialog = true;
					if (allocationForm != null) {
						allocationForm.setCalculateFit(calAmt);
						msgArg1 = FEDERAL_INCOME_TAX;
					}

				}
				if (showDialog) {
					// show tax wages allocation table popup
					PopupBean bean = PopupBean.getInstance();
					bean.show(this, 0, "Tours.Allocation.Error.SIT.Warning.Title", "Confirm.Close",
							null);
					bean.setMessage(MsgUtils.getMessage("Tours.Allocation.Error.Msg"));
					errorMessages.add(MsgUtils.formatMessage(
							"Tours.Allocation.Error.SIT.BelowLimit.Msg", msgArg1, percent));
					showWarningDialog = true;
					taxWagesRow = null;

				}
			}
		}
	}


	/**
	 *
	 * @param event
	 */
	public void listenRevisionChange(ValueChangeEvent event) {
		Integer index = (Integer)event.getNewValue();

		if (revisions != null && ! revisions.isEmpty()) {
			allocationForm = revisions.get(index);
			forceLazyInit();
			initAllocationForm();
		}
	}


	public void listenTotalWageChange(ValueChangeEvent event) {
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		BigDecimal oldValue = (BigDecimal)event.getOldValue();
		BigDecimal newValue = (BigDecimal)event.getNewValue();

		if(oldValue == null || newValue == null || (oldValue.compareTo(newValue) != 0)) {
			initAllocationForm();
		}
	}

	/**
	 * Validate whether this form can be transmitted.
	 * Note: Occasions where FIT and/or SIT tax liabilities are not met,
	 * these are flagged as such and do not prevent transferring of this form.
	 * A informational message will be presented to the user that the tax
	 * liability has not been met.
	 *
	 * @return
	 */
	private boolean validateForm() {
		taxLiabilityMet = true;
		boolean passed = true;
		BigDecimal totalWages = allocationForm.getTotalWages();
		String residentCity = allocationForm.getResidentCity();
		String residentState = allocationForm.getResidentState();
		String ssn = allocationForm.getSocialSecurity();
		boolean hasFIT;
		boolean fitIsNumeric = false;
		List<String> hardStopErrMsgs = new ArrayList<>();

		errorMessages = new HashSet<>();
		if (residentCity != null) {
			residentCity = residentCity.trim().toLowerCase();
		}
		else {
			residentCity = "";
		}
		if (residentState != null) {
			residentState = residentState.trim();
		}
		else {
			residentState = "";
		}

		if ((residentCity.isEmpty()) && ! residentState.equals(Constants.DC_STATE)) {
			passed = false;
			hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.ResidentCity.Empty"));
			residentCity = "";
		}

		if(residentState.isEmpty()) {
			passed = false;
			hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.ResidentState.Empty"));

		}
		if(ssn == null || ssn.isEmpty()) {
			passed = false;
			hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.SSN.Empty"));

		}

//		List<TaxWageAllocationRow> rows = allocationForm.getAllocationFormRows();

//		if (totalWages == null) {
//			passed = false;
//			MsgUtils.addFacesMessage("Tours.Allocation.Error.MissingTotalWages",
//					FacesMessage.SEVERITY_ERROR);
//		}
		String calculatedFIT = allocationForm.getCalculateFit();
		hasFIT = (calculatedFIT != null && !calculatedFIT.isEmpty());

		if(totalWages != null) {
			// Test whether the Total Wages entered matchings the running total.
			if (sumOfIndividualWages.doubleValue() != allocationForm.getTotalWages().doubleValue()) {
				NumberFormat numFormat = NumberFormat.getNumberInstance();

				numFormat.setGroupingUsed(true);
				numFormat.setMaximumFractionDigits(2);
				passed = false;
				hardStopErrMsgs.add(MsgUtils.formatMessage("Tours.Allocation.Error.TotalWagesNotEqualSummaryTotal",
						numFormat.format(allocationForm.getTotalWages().doubleValue()), numFormat.format(sumOfIndividualWages.doubleValue())));
			}
			BigDecimal fitAmount = Constants.DECIMAL_ZERO;
			boolean calcTaxPercentage = false;

			// If calculatedFIT is numeric value, remove formatting characters.
			if(hasFIT) {
				calculatedFIT = calculatedFIT.replace(",", "");
				calculatedFIT = calculatedFIT.replace("$", "");
			}
			if (NumberUtils.isNumber(calculatedFIT) && totalWages.signum() > 0) {
				calcTaxPercentage = true;
				fitAmount = new BigDecimal(calculatedFIT);
				fitIsNumeric = true;
			}

			// Must have FIT amount or if ZERO
			if(fitIsNumeric && fitAmount.signum() == 0) {
				calcTaxPercentage = false;
				errorMessages.add(MsgUtils.getMessage("Tours.Allocation.Error.MustHaveFITAmount"));
				hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.MustHaveFITAmount"));
			}

			if (calcTaxPercentage) {
				BigDecimal taxPercentage = fitAmount.divide(totalWages, 4, RoundingMode.HALF_UP);

				if (taxPercentage.doubleValue() < .15D || calculatedFIT.equalsIgnoreCase("ZERO")) {
					taxLiabilityMet = false;
					errorMessages.add(MsgUtils.getMessage("Tours.Allocation.Error.FIT.BelowLimit"));
				}
			}
			// Test whether total accumulated taxes are greater than total accumulated wages for the form.
			if (sumOfIndividualTaxes.doubleValue() > sumOfIndividualWages.doubleValue()) {
//				passed = false;
//				hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.TotalSIT.GreaterThan.TotalStateWages"));
			}
			// If there are wages and FIT is blank do not allow transmit.
			if(!hasFIT) {
				passed = false;
				hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.MustHaveFITAmount"));
			}
		}
		else {
			hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.MissingTotalWages"));
			passed = false;
		}


		// 1. Check for rows that have a tax amount that is greater than the wage amount. This is only allowed if this is
		// the state of resident.
		// 2. If the city of residence is NYC, then there has to be a dollar amount entered for
		// taxes
		// Some of the errors we do not want to show by state if multiple occurrences. So if more than one instance of the
		// error, we only put up one generic message.
		List<TaxWageAllocationRow> rows = allocationForm.getAllocationFormRows();
		boolean missingWagesForSitNonResident = false;
		boolean sitCannotExceedWages = false;
		boolean missingWagesForSITResident = false;
		// Only show SIT is zero warning once if applies to multiple states.
		boolean isSITZero = false;
		// Only show message for missing SIT once.
		boolean isSITMissing = false;

		for (TaxWageAllocationRow row : rows) {
			BigDecimal wages = row.getWages();
			String taxes = row.getCalculateTax();
			String rowCity = row.getRowTemplate().getCity();
			String rowStateCode = row.getRowTemplate().getStateCode();
			String msgArg1;
			BigDecimal taxAmount = Constants.DECIMAL_ZERO;
			BigDecimal sitPercentage = Constants.DECIMAL_ZERO;

			// Tests for wages.
			boolean areThereWages = (wages != null);
			// Tests for taxes.
			boolean areThereTaxes = (taxes != null && !taxes.isEmpty());
			boolean areTaxesNumeric = false;
			boolean areTaxesEmpty = true;
			boolean rowIsNYC = ((rowStateCode != null && rowStateCode.equals(Constants.NEW_YORK_STATE)) && (rowCity != null && rowCity.equalsIgnoreCase(Constants.NEW_YORK_CITY)));
			boolean isResidentState = (rowStateCode != null && rowStateCode.equals(residentState));

			// If there is an error, is it for a city or a state?
			if (rowCity != null) {
				rowCity = rowCity.toLowerCase();
				msgArg1 = row.getRowTemplate().getCity();
			}
			else {
				msgArg1 = row.getRowTemplate().getState();
			}

			if (areThereTaxes) {
				areTaxesEmpty = (taxes.isEmpty());
				// Remove any formatting characters
				taxes = taxes.replace("$", "");
				taxes = taxes.replace(",", "");
				areTaxesNumeric = NumberUtils.isNumber(taxes);

				if (areTaxesNumeric) {
					taxAmount = new BigDecimal(taxes);
				}
			}

			if (areThereTaxes) {
				String msgArg;
				/* The validations for NY state (9% rule) */
				if (rowStateCode.equals("NY") && ! rowIsNYC && areTaxesNumeric && areThereWages &&
						wages.signum() > 0) {
					sitPercentage = taxAmount.divide(wages, 4, RoundingMode.HALF_UP);
					if (sitPercentage.doubleValue() < NY_SIT_TAX_PERCENTAGE) {
						msgArg = NY_STATE_CODE + " " + STATE_INCOME_TAX;
						errorMessages.add(
								MsgUtils.formatMessage("Tours.Allocation.Error.SIT.BelowLimit.Msg",
										msgArg, SIT_NINE_PERCENT_LIMIT));
					}
				}
				/* The validations for for all other states (5% rule) except for NY */
				else if (areTaxesNumeric && areThereWages && wages.signum() > 0) {
					sitPercentage = taxAmount.divide(wages, 4, RoundingMode.HALF_UP);
					if (sitPercentage.doubleValue() < SIT_TAX_PERCENTAGE) {
						msgArg = STATE_INCOME_TAX;
						errorMessages.add(
								MsgUtils.formatMessage("Tours.Allocation.Error.SIT.BelowLimit.Msg",
										msgArg, SIT_FIVE_PERCENT_LIMIT));
					}
				}
				if (! areTaxesEmpty && ! rowStateCode.equals(Constants.CANADA_STATE_CODE)) {
					if (! sitCannotExceedWages) {
						// If this is not a row that has the tax amount non-editable
						// and there is tax amount filled in without a wage amount or the
						// tax amount > wage amount flag as an error.
						if (row.getRowTemplate().getCalculateTaxEditable()) {
							if (areThereWages && areTaxesNumeric &&
									(wages.compareTo(taxAmount) == - 1 && ! isResidentState)) {
								passed = false;
								sitCannotExceedWages = true;
								hardStopErrMsgs.add(MsgUtils.formatMessage(
										"Tours.Allocation.Error.RowTaxesExceedWages", msgArg1));
							}
						}
					}
					// Warning if SIT is 0.00 or "ZERO" client must provide letter to Team.
					if (areTaxesNumeric &&
							(taxAmount.signum() == 0 || taxes.equalsIgnoreCase("ZERO"))) {
						if (isSITZero == false) {
							hardStopErrMsgs.add(MsgUtils
									.formatMessage("Tours.Allocation.Error.SIT.Zero", msgArg1));
							errorMessages
									.add(MsgUtils.getMessage("Tours.Allocation.Error.SIT.Zero"));
							// Only show message once.
							isSITZero = true;
						}

					}
				}
			}
			else {
				if(residentCity != null && residentState != null && residentCity.equalsIgnoreCase(Constants.NEW_YORK_CITY)
						&& residentState.equals(Constants.NEW_YORK_STATE) && rowIsNYC) {
					// Hard stop for NYC if no taxes entered. LS-1511
					passed = false;
					hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.NYC.Residents.MissingSIT"));
				}
			}

			if (!areThereWages) {
				// Non-Residents of state with SIT must have wages except for DC and NYC
				if(!missingWagesForSitNonResident) {
					// Skip for NYC since SIT only applies to residents.
					if ((! rowStateCode.equals(residentState) && ! rowIsNYC) && areThereTaxes &&
							areTaxesNumeric) {
						missingWagesForSitNonResident = true;
						passed = false;
						hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.SIT.NoWages.Nonresident"));
					}
				}

				if(!missingWagesForSITResident) {
					// The resident state can have SIT without wages.
					if(rowStateCode.equals(residentState) && areThereTaxes) {
						missingWagesForSITResident = true;
						errorMessages.add(MsgUtils.formatMessage("Tours.Allocation.Error.SIT.NoWages.ResidentState", rowStateCode));
						hardStopErrMsgs.add(MsgUtils.formatMessage("Tours.Allocation.Error.SIT.NoWages.ResidentState", rowStateCode));
					}
				}
			}
			else {
				if (! isSITMissing && areTaxesEmpty && ! rowIsNYC) {
					// This applies for all states except when the row is DC and this is the resident state.
					// For DC non-residents cannot add SIT.
					// Ignore for NYC - LS-1511
					// Only show the message once.
					hardStopErrMsgs.add(MsgUtils.getMessage("Tours.Allocation.Error.SIT.Zero"));
					errorMessages.add(MsgUtils.getMessage("Tours.Allocation.Error.SIT.Zero"));
//					passed = false;
					isSITMissing = true;
				}
			}
		}

		if(!passed) {
			// Only show these messages if validation has failed. We do not need to display
			// when there only soft warnings
			MsgUtils.addFacesMessageText(hardStopErrMsgs, FacesMessage.SEVERITY_ERROR);
		}
		return passed;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;

		switch (action) {
			case ACTION_CREATE:
				res = actionCreateFormOk();
				break;
			case ACTION_DELETE:
				res = actionDeleteOk();
				break;
			case ACTION_SELECT_PREVIOUS_ROW:
//				selectPreviousRow();
				break;
			case ACTION_TRANSMIT:
				showTransmitDialog = false;
				res = actionTransmitOk();
				break;
			case ACTION_CREATE_REVISION:
				res = actionCreateRevisionOk();
				break;
		}

		return res;
	}

	@Override
	public String confirmCancel(int action) {
		showDeleteDialog = false;
		showTransmitDialog = false;
		SessionUtils.put(Constants.ATTR_SELECTED_ALLOC_CONTACT_ID, allocationForm.getContact().getId());

		return HeaderViewBean.PAYROLL_WAGE_TAX_ALLOCATION;
	}

	@Override
	protected void setSelected(Object item, boolean b) {
//		((ContactAllocationForms)item).getContact().setSelected(b);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List getItemList() {
		return contactForms;
	}

	@Override
	protected Comparator<ContactAllocationForms> getComparator() {
		Comparator<ContactAllocationForms> comparator = new Comparator<ContactAllocationForms>() {
			@Override
			public int compare(ContactAllocationForms one, ContactAllocationForms two) {
				return one.compareTo(two, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * Refresh the list of wrapper classes
	 */
	@Override
	protected void refreshList() {
		String orderBy = "order by ct.user.lastName, ct.user.firstName";
		List<Contact> prodContacts =
				getTaxWageAllocationFormDAO().findContactsWithFormsByProduction(production, orderBy);
		contactForms = new ArrayList<>();

		if (prodContacts != null && ! prodContacts.isEmpty()) {
			// Remove ls admin from list
			for (Contact contact : prodContacts) {
				ContactAllocationForms cf = new ContactAllocationForms();
				List<TaxWageAllocationForm> forms =
						getTaxWageAllocationFormDAO().findByContact(contact, "id desc");
				cf.setContact(contact);
				cf.setForms(forms);

				contactForms.add(cf);
			}
		}

		doSort();
	}

	/**
	 * Refresh the revision dropdown after the current form has been transmitted
	 * or changing to a different person.
	 */
	private void refreshRevisionsList() {
		if (selectedFormWrapper != null) {
			revisions = getTaxWageAllocationFormDAO().findRevisionsByContactProd(production,
					selectedFormWrapper.getContact(), "order by id DESC");
		}
	}

	private void forceLazyInit() {
		if (allocationForm != null) {
			allocationForm = getTaxWageAllocationFormDAO().refresh(allocationForm);
			allocationForm.getAllocationFormRows().size();
			for (TaxWageAllocationRow row : allocationForm.getAllocationFormRows()) {
				row.getRowTemplate();
			}
			Contact contact = getContactDAO().refresh(allocationForm.getContact());
			allocationForm.setContact(contact);
			getUserDAO().refresh(contact.getUser());
		}
	}

	/**
	 * Create the revisions select items for revisions dropdown.
	 */
	private void createRevisionsDL() {
		revisionsDL = new ArrayList<>();
		showNewRevisionBtn = true;
		if (revisions != null && ! revisions.isEmpty()) {
			// If the first item in the revisions list has not been transmitted,
			// mark it as the current form in the DL list.
			TaxWageAllocationForm form = revisions.get(0);
			String revDate;

			if (! form.getTransmitted()) {
				revDate = "(Current)";
				showNewRevisionBtn = false;
			}
			else {
				revDate = CalendarUtils.formatDate(form.getRevisionDate(), "MM/dd/yyyy hh:mm aaa");
			}
			revisionsDL.add(new SelectItem(0, revDate));

			allocationForm = form;

			for (int i = 1; i < revisions.size(); i++) {
				form = revisions.get(i);

				revDate = CalendarUtils.formatDate(form.getRevisionDate(), "MM/dd/yyyy hh:mm aaa");

				revisionsDL.add(new SelectItem(new Integer(i), revDate));
			}

//			// If the first item in the revisions collection has not been transmitted,
//			// we start the index at 1 to get the remaining items from revisions.
//			// If the first item has been transmitted, we create a copy of the first item
//			// and add that to the revisionsDL first so we need to set the index to 0
//			// to retrieve the remaining items.

//			TaxWageAllocationForm form = revisions.get(0);
//			if(form != null) {
//				if(form.getTransmitted()) {
//					// All of this contact's forms have been transmitted,
//					// create a new form by cloning the most recent form.
//					allocationForm = form.deepCopy();
//					allocationForm.setTransmitted(false);
//					revisions.add(0, allocationForm);
//				}
//				else {
//					allocationForm = form;
//				}
//
//				revisionsDL.add(new SelectItem(new Integer(0), "(Current)"));
//
//				// Build the rest of the list.
//				for(int i = 1; i < revisions.size(); i++) {
//					form = revisions.get(i);
//
//					String revDate = CalendarUtils.formatDate(form.getRevisionDate(), "MM/dd/yyyy hh:mm aaa");
//
//					revisionsDL.add(new SelectItem(new Integer(i), revDate));
//				}
//			}
//		}
//		else {
//			// This is a new record with no revisions so add the default value.
//			revisionsDL.add(new SelectItem(new Integer(0), "(Current)"));
//		}
		}
	}

	/**
	 * Get the first default id
	 *
	 * @return id
	 */
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
	 * To be used to populate the Contact with Allocation Forms list. This list
	 * will be displayed on the left side of the Tax Wage Allocation page.
	 */
	public class ContactAllocationForms implements Comparable<ContactAllocationForms> {
		/** Contact that has allocation forms */
		private Contact contact;
		/** List of associated allocation forms */
		private List<TaxWageAllocationForm> forms;
		/** If this component is selected */
		private boolean selected;

		public static final String SORTKEY_NAME = "name";

		public ContactAllocationForms() {

		}

		/** See {@link #contact}. */
		public Contact getContact() {
			return contact;
		}

		/** See {@link #contact}. */
		public void setContact(Contact contact) {
			this.contact = contact;
		}

		/** See {@link #forms}. */
		public List<TaxWageAllocationForm> getForms() {
			return forms;
		}

		/** See {@link #forms}. */
		public void setForms(List<TaxWageAllocationForm> forms) {
			this.forms = forms;
		}

		/** See {@link #selected}. */
		public boolean getSelected() {
			return selected;
		}

		/** See {@link #selected}. */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public int getCount() {
			return forms.size();
		}


		@Override
		public int compareTo(ContactAllocationForms o) {
			return 0;
		}

		public int compareTo(ContactAllocationForms other, String sortField, boolean ascending) {
			int ret = compareTo(other, sortField);
			return (ascending ? ret : (0 - ret));
		}

		public int compareTo(ContactAllocationForms other, String sortField) {
			int ret = 0;
			if (sortField == null) {
				// sort by title (later)
			}
			else if (sortField.equals(SORTKEY_NAME)) {
				ret = StringUtils.compareIgnoreCase(getContact().getUser().getLastNameFirstName(),
						other.getContact().getUser().getLastNameFirstName());
			}

			if (ret == 0) { // unsorted, or specified column compared equal
				ret = compareTo(other);
			}
			return ret;
		}
	}

	/**
	 * @return the taxWagesRow
	 */
	public TaxWageAllocationRow getTaxWagesRow() {
		return taxWagesRow;
	}

	/**
	 * @param taxWagesRow the taxWagesRow to set
	 */
	public void setTaxWagesRow(TaxWageAllocationRow taxWagesRow) {
		this.taxWagesRow = taxWagesRow;
	}

	/** See {@link #showWarningDialog}. */
	public boolean getShowWarningDialog() {
		return showWarningDialog;
	}

	/** See {@link #showWarningDialog}. */
	public void setShowWarningDialog(boolean showWarningDialog) {
		this.showWarningDialog = showWarningDialog;
	}

	/**
	 * method to change background color for the FIT cell when FIT amount is
	 * less than 5% than wage amount in case of FEDERAL WITHHOLDING.
	 *
	 * @return styleClassName style class name to be applied on FIT amount cell
	 */
	public String changeFitBgColor() {
		String styleClassName = "";
		if (getAllocationForm() != null) {
			String calculateTax = getAllocationForm().getCalculateFit();
			if (calculateTax != null && NumberUtils.isNumber(calculateTax.replace(",", "")) &&
				getAllocationForm().getTotalWages() != null &&
				getAllocationForm().getTotalWages().doubleValue() > 0.0) {
				BigDecimal calFitAmt =
						new BigDecimal(calculateTax.replace(",", ""));
			BigDecimal totalWages = getAllocationForm().getTotalWages();
			BigDecimal val = calFitAmt.divide(totalWages, 4, RoundingMode.HALF_UP);
			if (val.doubleValue() < .15D) {
				styleClassName = "BGCOLUMN";
				return styleClassName;
			}
		}
		}

		return styleClassName;
	}

	/**
	 * method to change background color for the SIT cell when SIT amount is
	 * less than 5% for all state and 9% for NY than wage amount.
	 *
	 * @return styleClassName style class name to be applied on SIT amount cell
	 */
	public String changeTaxBgColor(TaxWageAllocationRow row) {
		String styleClassName = "";
		if (row != null) {
			String calculateTax = row.getCalculateTax();
			if (calculateTax != null && NumberUtils.isNumber(calculateTax.replace(",", "")) &&
					row.getWages() != null && row.getWages().doubleValue() > 0.0) {
				String rowCity = row.getRowTemplate().getCity();
				BigDecimal totalWages = row.getWages();
				BigDecimal val = new BigDecimal(calculateTax.replace(",", "")).divide(totalWages, 4,
						RoundingMode.HALF_UP);
				String rowStateCode = row.getRowTemplate().getStateCode();
				boolean rowIsCanada =
						rowStateCode != null && rowStateCode.equals(Constants.CANADA_STATE_CODE);
				boolean rowIsNY =
						rowStateCode != null && rowStateCode.equals(Constants.NEW_YORK_STATE);
				boolean rowIsNYC = (rowIsNY &&
						(rowCity != null && rowCity.equalsIgnoreCase(Constants.NEW_YORK_CITY)));

				if ((rowIsNY && ! rowIsNYC && val.doubleValue() < NY_SIT_TAX_PERCENTAGE) ||
						(! rowIsCanada && val.doubleValue() < SIT_TAX_PERCENTAGE)) {
					styleClassName = "BGCOLUMN";
					return styleClassName;
				}
			}

		}

		return styleClassName;
	}

}
