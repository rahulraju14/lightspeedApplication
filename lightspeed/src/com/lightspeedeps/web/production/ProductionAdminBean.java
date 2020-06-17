package com.lightspeedeps.web.production;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.model.table.LazyDataModel;
import org.icefaces.ace.model.table.SortCriteria;

import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.service.OnboardService;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.type.HourRoundingType;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.util.app.ChangeUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.Log4jConfigurator;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.EnumList;

/**
 * The backing bean for the Admin Production List/View/Edit page.
 */
@ManagedBean
@ViewScoped
public class ProductionAdminBean extends ProductionListBean implements Serializable {
	/** */
	private static final long serialVersionUID = 8318953827214642843L;

	private static final Log log = LogFactory.getLog(ProductionAdminBean.class);
//	private static final int TAB_DETAIL = 0;
//	private static final int TAB_ADMIN = 1;

	private static final ProductionType INITIAL_CATEGORY = ProductionType.FEATURE_FILM;

	/* Fields */

	/** The list of Production`s remaining after the user's filter,
	 * if any, has been applied. */
	private List<Production> filteredList;

	/** The drop-down selection list for type (or "All"). */
	private List<SelectItem> productionTypeDL;

	private int numProjects = 1;

	private String category = INITIAL_CATEGORY.name();

	/** The User that owns the currently selected Production. */
	private User owner;

	private List<SelectItem> payrollServices;

	private Integer payrollServiceId;

	/** Saves the type of the production when Edit mode is entered, so Save can
	 * determine if the type changed. */
	private ProductionType priorType = ProductionType.OTHER;

	private LazyDataModel<Production> testProductionList;

	/** True, if the user jumps to the Users page from Change List */
	private boolean isJumpEnabled = false;

	/** String literal used to hold the Production Title of the clicked user from the commandlink on Change List */
	private String productionTitle = null;

	/** Action code for popup confirmation/prompt dialog to change the allow Onboarding. */
	private static final int ACT_CHANGE_ONBOARDING = 20;

	private PayrollFormType selectedDocument;

	private List<SelectItem> documentList = null;

//	private static final String LOG_FILE_PATH = "/home/innoeye/linuxsoftware/apache-tomcat-6.0.43/webapps/lightspeed31/WEB-INF/classes/log4j.properties";

	/** Constructor */
	public ProductionAdminBean() {
		super(Production.SORTKEY_NAME, "ProductionAdmin.");
		log.debug("Init");
		try {
			productionTypeDL = new ArrayList<>( EnumList.getProductionTypeList() );
			productionTypeDL.add(0, Constants.GENERIC_ALL_ITEM);

			category = SessionUtils.getString(Constants.ATTR_PROD_CATEGORY, Constants.CATEGORY_ALL);
			changeCategory(getCategory(), false);

			initList();

			Integer id = null;
			String prodKey = SessionUtils.getString(Constants.ATTR_PRODUCTION_KEY);
			if (prodKey != null) {
				SessionUtils.put(Constants.ATTR_PRODUCTION_KEY, null); // only use it once
				Production prod = ProductionDAO.getInstance().findOneByProperty(ProductionDAO.PROD_ID, prodKey);
				if (prod != null) {
					id = prod.getId();
					if (id != null) {
						isJumpEnabled = true; // set boolean to true
						log.debug("jump enabled");
					}
				}
			}
			if (id == null) {
				id = SessionUtils.getInteger(Constants.ATTR_PRODUCTION_ID);
			}
			setupSelectedItem(id);
			if (isJumpEnabled) {
				productionTitle = production.getTitle();
			}
			scrollToRow();
			checkTab(); // restore last mini-tab in use

			payrollServices = new ArrayList<>();
			List<PayrollService> services = PayrollServiceDAO.getInstance().findAll();
			Collections.sort(services);
			payrollServices.add(new SelectItem(-1, "(no service assigned)"));
			for (PayrollService svc : services) {
				payrollServices.add(new SelectItem(svc.getId(), svc.getName()));
			}

		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@SuppressWarnings("serial")
	private void initList() {
		testProductionList = new LazyDataModel<Production>() {

			@Override
			public List<Production> load(int first, int pageSize, SortCriteria[] criteria, Map<String, String> filters) {

				ProductionType type = null;
				if (category.equals(Constants.CATEGORY_ALL)) {
					type = null;
				}
				else {
					type = ProductionType.valueOf(category);
				}

				String titleValue = filters.get("title");
				String studioValue = filters.get("studio");
				String ownerValue = filters.get("owningAccount");

				long count = getProductionDAO().findCountByFilter(titleValue, studioValue, ownerValue, type);
				setRowCount((int)count);

				filteredList = new ArrayList<>(getProductionDAO().findByFilter(titleValue, studioValue, ownerValue, pageSize, first, type));
				for (Production prod : filteredList) {
					log.debug("production list got >> "+prod.getTitle());
				}
				return filteredList;
			}
		};
	}

	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		owner = null;
		if (production != null) {
			production.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, null);
			production = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			production = getProductionDAO().findById(id);
			if (production == null) {
				id = findDefaultId();
				if (id != null) {
					production = getProductionDAO().findById(id);
				}
			}
			if ( ! getCategory().equals(Constants.CATEGORY_ALL) &&
					! getCategory().equals(production.getType().name())) {
				changeCategory(production.getType().name(), false);
			}
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, id);
		}
		else {
			log.debug("new");
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, null); // erase "new" flag
			production = new Production("new production"); // get instance with default values
			String type = SessionUtils.getString(Constants.ATTR_PROD_CATEGORY, ProductionType.OTHER.name());
			if (type.equals(Constants.CATEGORY_ALL)) {
				production.setType(ProductionType.OTHER);
			}
			else {
				production.setType(ProductionType.valueOf(type));
			}
			User user = SessionUtils.getCurrentUser();
			production.setOwningAccount(user.getAccountNumber());
		}
		if (production != null) {
			production.setSelected(true);
			if (production.getAddress() == null) {
				production.setAddress(new Address());
			}
			if (production.getPayrollPref().getPayrollService() != null) {
				payrollServiceId = production.getPayrollPref().getPayrollService().getId();
			}
			else {
				payrollServiceId = -1;
			}
			owner = UserDAO.getInstance()
					.findOneByProperty(UserDAO.ACCOUNT_NUMBER, production.getOwningAccount());
			calculateBillAmount();
			forceLazyInit();
		}
	}

	@Override
	protected void forceLazyInit() {
		super.forceLazyInit();
	}

	/**
	 * Action method for Edit button; save the number of projects, used in
	 * determining the available production types in the Type drop-down list.
	 *
	 * @see com.lightspeedeps.web.view.ListView#actionEdit()
	 */
	@Override
	public String actionEdit() {
		production = getProductionDAO().refresh(production);
		production.getPayrollPref().getAccountMajor(); // force init
		numProjects = production.getProjects().size();
		priorType = production.getType();
		return super.actionEdit();
	}

	/**
	 * @see com.lightspeedeps.web.production.ProductionListBean#actionSave()
	 */
	@Override
	public String actionSave() {
		if (production.getType() != priorType) {
			// in case type was changed to Commercial, verify each Project has its own PayrollPreference.
			PayrollPreference prodPref =
					PayrollPreferenceDAO.getInstance().refresh(production.getPayrollPref());
			if (production.getType().hasPayrollByProject()) { // changing to Commercial production
				prodPref.setHourRoundingType(HourRoundingType.FOURTH);	// set rounding default to 1/4-hour
				production.setDeptMaskB(Constants.ALL_DEPARTMENTS_MASK);
				for (Project proj : production.getProjects()) {
					PayrollPreference pref = proj.getPayrollPref();
					if (pref == null) { // only happens if non-Commercial changed to Commercial
						pref = prodPref.deepCopy();
						proj.setPayrollPref(pref);
						proj.setDeptMask(Constants.ALL_DEPARTMENTS_MASK);
						getProductionDAO().attachDirty(proj);
					}
				}
				if (production.getProjects().size() == 1) {
					Project p = production.getProjects().iterator().next();
					if (p.getTitle().equals("Episode 1") || 				// was TV default
							p.getTitle().equals(production.getTitle())) {	// was Feature or Tours default
						p.setTitle("Project 1");	// set Commercial default
					}
				}
			}
			else {
				prodPref.setHourRoundingType(HourRoundingType.TENTH);
			}
		}
		if (production.getOwningAccount() != null) { // remove extra whitespace
			production.setOwningAccount(production.getOwningAccount().trim());
			owner = UserDAO.getInstance()
					.findOneByProperty(UserDAO.ACCOUNT_NUMBER, production.getOwningAccount());
		}
		if (production.getType().isTours() && (! priorType.isTours())) {
			// Changed from non-Tours to Tours type, set mask
			log.debug("Changed Dept Mask");
			production.setDeptMaskB(Constants.TOURS_DEPARTMENTS_MASK);
		}
		return super.actionSave();
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		super.actionCancel();
		if (production != null) {
			setupSelectedItem(production.getId());
		}
		return null;
	}

	@Override
	public String actionDelete() {
		MsgUtils.addFacesMessage("ProductionAdmin.DisableDelete", FacesMessage.SEVERITY_INFO);
		return null;
	}

	/**
	 * Delete the selected Production from Database. This is called as a result
	 * of clicking "ok" on the delete confirmation pop-up.
	 * <p>
	 * NOTE: this REALLY deletes the production. If a "normal" user does a
	 * Delete operation from the My Productions page, it merely marks the
	 * Production "deleted", but does not remove it from the database.
	 */
	/*@Override
	public String actionDeleteOk() {
		try {
			production = getProductionDAO().findById(production.getId()); // refresh
			getProductionDAO().remove(production);
			SessionUtils.put(Constants.ATTR_PRODUCTION_ID, null);

			productionList.remove(production);
			filteredList = applyFilter(productionList);
			production = null;
			setupSelectedItem(getRowId(getSelectedRow()));
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}*/

	/**
	 * Action method called whenever a key-up event is detected in the filter
	 * text field. This is done in the JSP via a hidden button.
	 *
	 * @return null navigation string
	 */
	public String actionFilter() {
		if (productionList.size() > 0) {
			filteredList = applyFilter(productionList);
			int ix = filteredList.indexOf(production);
			if (ix >= 0) {	// current prod still in filtered list
				setSelectedRow(ix); // highlight the row
			}
			else {
				// current prod has been eliminated - setup a new one
				production.setSelected(false);
				Integer id = null;
				if (filteredList.size() > 0) {
					// just make the top entry current
					production = filteredList.get(0);
					setSelectedRow(0);
					id = production.getId();
				}
				else {
					// empty list, clear highlighting
					setSelectedRow(- 1);
				}
				// set up the newly selected item
				setupSelectedItem(id);
			}
		}
		else { // empty production list
			filter = "";
		}
		return null;
	}

	/**
	 * The Value Change Listener for the category (element type) selection
	 * drop-down list on the Details (right-hand) tab.  This is only available
	 * in Edit mode.
	 * @param evt
	 */
	public void changeEditCategory(ValueChangeEvent evt) {
		log.debug("");
		try {
			if (evt.getNewValue() != null) {
				if (! category.equals(Constants.CATEGORY_ALL)) {
					changeCategory( (String)evt.getNewValue(), false );
				}
			}
			else {
				log.warn("Null newValue in category change event: " + evt);
			}
//			addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
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
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * The Value Change Listener for the Payroll Service selection drop-down.
	 * @param evt
	 */
	public void listenChangePayroll(ValueChangeEvent evt) {
		Integer id = (Integer)evt.getNewValue();
		if (id != null) {
			if (id < 0) {
				production.getPayrollPref().setPayrollService(null);
			}
			else {
				PayrollService service = PayrollServiceDAO.getInstance().findById(id);
				if (service != null) {
					production.getPayrollPref().setPayrollService(service);
					// For Team Payroll Service default to calculate timecard on submit true.
					if(service.getTeamPayroll()) {
						production.getPayrollPref().setCalcTimecardsOnSubmit(true);
					}
					else {
						production.getPayrollPref().setCalcTimecardsOnSubmit(false);
					}
				}
			}
			payrollServiceId = id;
		}
	}

	/**
	 * Set a new category (or "All") as the type of item listed. This regenerates the list of
	 * elements listed. If the currently selected item is not in the new list, then select the first
	 * entry of the new list.
	 *
	 * @param type A ScriptElementType value, or "All".
	 */
	protected void changeCategory(String type, boolean pickElement) {
		if (getElement() != null && ! editMode) {
			getElement().setSelected(false); // we may end up switching
		}
		SessionUtils.put(Constants.ATTR_PROD_CATEGORY, type);
		category = type;
		if ( ! type.equals(Constants.CATEGORY_ALL)) {
			ProductionType t = ProductionType.valueOf(type);
			productionList = getProductionDAO().findByType(t);
		}
		else {
			productionList = getProductionDAO().findAll();
		}
		filteredList = applyFilter(productionList);
		setSelectedRow(-1);
		doSort();	// the new list may have been previously sorted by some other column
		if (pickElement) { // possibly select an element to view
			@SuppressWarnings("unchecked")
			List<Production> list = getItemList();
			if (getElement() != null) {
				int ix = list.indexOf(production);
				if (ix < 0) {
					log.debug(type + ", " + production.getType());
					if (list.size() > 0) {
						setupSelectedItem(list.get(0).getId());
					}
					else {
						setupSelectedItem(null); // clear View if nothing in list
					}
				}
				else {
					production = list.get(ix);
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
//		addClientResize();
	}

	@Override
	protected void refreshList() {
		changeCategory(getCategory(), true);
	}

	/**
	 * Apply the current filter text to the given list of Production`s, and return a
	 * filtered list. Note that if the filter results in no entries left, this
	 * method will shorten the filter one character at a time, until the
	 * resulting list has at least one contact in it.
	 *
	 * @param productions The list to be filtered.
	 * @return A non-null, but possibly empty, list of Production`s in which the
	 *         Production.title field contains the filter text (in any position,
	 *         ignoring case). Note that an empty list will only be returned if
	 *         the given list is empty.
	 */
	private List<Production> applyFilter(List<Production> productions) {
		List<Production> filtered;
		if (filter == null || filter.length() == 0) {
			filtered = productions;
		}
		else {
			filtered = new ArrayList<>();
			do {
				String filt = filter.toLowerCase();
				for (Production prod : productions) {
					prod.setSelected(false);
					if (prod.getTitle().toLowerCase().contains(filt)) {
						filtered.add(prod);
					}
				}
				if (filtered.size() == 0) {
					filter = filter.substring(0, filter.length()-1);
				}
			} while (filtered.size() == 0 && filter.length() > 0);
			if (filter.length() == 0) {
				filtered = productions;
			}
		}
		return filtered;
	}

//	public String getOrderStatus() {
//		return (getElement().getOrderStatus() == null ? "" : getElement().getOrderStatus().name());
//	}
//	public void setOrderStatus(String typeStr) {
//		getElement().setOrderStatus(OrderStatus.valueOf(typeStr));
//	}

	private List<SelectItem> createEditTypeDL() {
		List<SelectItem> list = new ArrayList<>();
		list.add(0, Constants.GENERIC_ALL_ITEM);
		if (getElement() != null) {
			list.add(1,new SelectItem(getElement().getType().name(),getElement().getType().getLabel()));
		}
		return list;
	}

	/**
	 * Create the drop-down list containing the eligible production
	 * types for this production.  Note that if a production has only a single
	 * project, it can be changed from an episodic to a feature film.
	 * @return The non-null List described above.
	 */
	private List<SelectItem> createProductionTypeDL() {
		if (newEntry || numProjects == 1) { // all types valid
			return EnumList.getProductionTypeList();
		}
		// otherwise, only episodic production types are valid...
		List<SelectItem> list = new ArrayList<>();
		for (SelectItem item : EnumList.getProductionTypeList()) {
			if (ProductionType.valueOf((String)item.getValue()).getEpisodic()) {
				list.add(item);
			}
		}
		return list;
	}

	/** Method to upgrade a non-onboarding production to onboarding and vice-versa.
	 * @return  null navigation string
	 */
	public String actionChangeOnboardingType() {
		try {
			boolean allowOnboard;
			Production selectedProduction = getElement();
			if (selectedProduction != null) {
				log.debug("...selectedProduction...." + selectedProduction.getTitle());
				allowOnboard = selectedProduction.getAllowOnboarding();
				log.debug("...allowOnboard...." + allowOnboard);
				PopupBean bean = PopupBean.getInstance();
				if (allowOnboard) {
					bean.show(this, ACT_CHANGE_ONBOARDING, "ProductionAdmin.DisableOnboarding.");
				}
				else {
					bean.show(this, ACT_CHANGE_ONBOARDING, "ProductionAdmin.EnableOnboarding.");
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_CHANGE_ONBOARDING:
				res = actionChangeOnboardingTypeOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/** Method to enable/disable onboard feature for the selected production.
	 * @return  null navigation string
	 */
	private String actionChangeOnboardingTypeOk() {
		try {
			boolean allowOnboard;
			Production selectedProduction = getElement();
			String msgString;
			if (selectedProduction != null) {
				log.debug("...selectedProduction...." + selectedProduction.getTitle());
				allowOnboard = selectedProduction.getAllowOnboarding();
				log.debug("...allowOnboard...." + allowOnboard);
				boolean success = false;
				if (allowOnboard) {
					selectedProduction.setAllowOnboarding(false);
					selectedProduction = ProductionDAO.getInstance().merge(selectedProduction);
					msgString = "disabled";
					if (! selectedProduction.getAllowOnboarding()) {
						success = true;
					}
				}
				else {
					OnboardService service = new OnboardService();
					selectedProduction = service.enableProductionOnboarding(selectedProduction);
					msgString = "enabled";
					if (selectedProduction.getAllowOnboarding()) {
						success = true;
					}
				}
				if (success) {
					MsgUtils.addFacesMessage("ProductionAdmin.SuccessMessage", FacesMessage.SEVERITY_INFO, msgString);
				}
				ChangeUtils.logChange(ChangeType.PRODUCTION, ActionType.UPDATE, selectedProduction,
						getvUser(), "Onboarding " + msgString);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to add new document to the selected production.
	 * @return  null navigation string
	 */
	public String actionAddDocument() {
		try {
			Production selectedProduction = getElement();
			if (selectedProduction != null && selectedProduction.getAllowOnboarding() &&
					getSelectedDocument() != null) {
				Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(selectedProduction);
				if (onboardFolder != null) {
					DocumentDAO documentDAO = DocumentDAO.getInstance();
					Map<String, Object> values = new HashMap<>();
					values.put(Document.NAME, getSelectedDocument().getName());
					values.put(Document.FOLDER_ID, onboardFolder.getId());
					Document payrollDocument = DocumentDAO.getInstance().findOneByNamedQuery(Document.GET_DOCUMENT_BY_NAME_FOLDER_ID, values);
					if (payrollDocument != null && payrollDocument.getStandard()) {
						MsgUtils.addFacesMessage("ProductionAdmin.DuplicateDocument", FacesMessage.SEVERITY_INFO);
					}
					else {
						documentDAO.saveStandardDocumentDocumentChain(onboardFolder, getSelectedDocument());
						MsgUtils.addFacesMessage("ProductionAdmin.DocumentDelivered", FacesMessage.SEVERITY_INFO);
					}
				}
			}
			else if (selectedDocument == null) {
				MsgUtils.addFacesMessage("ProductionAdmin.SelectDocument", FacesMessage.SEVERITY_INFO);
			}
			else {
				MsgUtils.addFacesMessage("ProductionAdmin.DocumentEnableOnboarding", FacesMessage.SEVERITY_INFO);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getItemList() {
		if (filteredList == null) {
			Map<String, String> filters = new HashMap<>();
			getTestProductionList().load(0, 2, null, filters);
		}
		return filteredList;
	}

	public List<SelectItem> getProductionTypeDL() {
		if (editMode) {
			return createEditTypeDL();
		}
		return productionTypeDL;
	}

	public List<SelectItem> getProductionSetTypeDL() {
		if (editMode) {
			return createProductionTypeDL();
		}
		return productionTypeDL;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	/** See {@link #owner}. */
	public User getOwner() {
		return owner;
	}
	/** See {@link #owner}. */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	/**See {@link #payrollServiceId}. */
	public Integer getPayrollServiceId() {
		return payrollServiceId;
	}
	/**See {@link #payrollServiceId}. */
	public void setPayrollServiceId(Integer payrollServiceId) {
		this.payrollServiceId = payrollServiceId;
	}

	/**See {@link #payrollServices}. */
	public List<SelectItem> getPayrollServices() {
		return payrollServices;
	}
	/**See {@link #payrollServices}. */
	public void setPayrollServices(List<SelectItem> payrollServices) {
		this.payrollServices = payrollServices;
	}

	/**See {@link #testProductionList}. */
	public LazyDataModel<Production> getTestProductionList() {
		return testProductionList;
	}
	/**See {@link #testProductionList}. */
	public void setTestProductionList(LazyDataModel<Production> testProductionList) {
		this.testProductionList = testProductionList;
	}

	/**See {@link #productionTitle}. */
	public String getProductionTitle() {
		return productionTitle;
	}
	/**See {@link #productionTitle}. */
	public void setProductionTitle(String productionTitle) {
		this.productionTitle = productionTitle;
	}

	/**See {@link #selectedDocument}. */
	public PayrollFormType getSelectedDocument() {
		return selectedDocument;
	}
	/**See {@link #selectedDocument}. */
	public void setSelectedDocument(PayrollFormType selectedDocument) {
		this.selectedDocument = selectedDocument;
	}

	/**See {@link #extraDocumentList}. */
	public List<SelectItem> getDocumentList() {
		if (documentList == null) {
			documentList = new ArrayList<>();
			documentList.add(new SelectItem(null, "(Select document)"));
			for (PayrollFormType form : PayrollFormType.values()) {
				if (form.isManuallyAdded()) {
					log.debug("form = " + form);
					documentList.add(new SelectItem(form, form.getName()));
				}
			}
		}
		return documentList;
	}
	/**See {@link #extraDocumentList}. */
	/*public void setDocumentList(List<PayrollFormType> documentList) {
		this.documentList = documentList;
	}*/

	/** Method used to change the log levels. */
	public void changeLogLevel() {
		String LogFilePath = SessionUtils.getRealPath("WEB-INF/classes/log4j.properties");
		//Configure logger service
		log.debug(LogFilePath);
		Log4jConfigurator.getInstance().initialize(LogFilePath);
	}

}
