package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.FormService;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;

/**
 * The superclass whose subclasses handle the UI for the various "state W4"
 * forms. This superclass includes logic that is common across all the state
 * W4s.
 * <p>
 * The methods that the subclasses will typically need to override are:
 * <ul>
 * <li>checkSaveValid - for validations upon Save;
 * <li>checkSubmitValid - for validations upon Sign & Submit;
 * <li>populateForm - to populate fields that are specific to the subclass'
 * form.
 * </ul>
 */
@ManagedBean
@ViewScoped
public class FormStateW4Bean extends StandardFormBean<FormStateW4> implements Serializable {

	/** serialization constant */
	private static final long serialVersionUID = - 6825029144953574102L;

	private static final Log LOG = LogFactory.getLog(FormStateW4Bean.class);

	// Marital Statuses
	public static final String MARITAL_STATUS_SINGLE = "s";
	public static final String MARITAL_STATUS_MARRIED = "m";
	public static final String MARITAL_STATUS_MARRIED_SEPARATE = "j";
	public static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";
	public static final String MARITAL_HEAD_HOUSEHOLD = "h";
	public static final String MARITAL_MILITARY_SPOUSE = "y";
	public static final String MARITAL_DISABLED = "d";
	public static final String MARITAL_STATUS_SEPERATED = "p";

	/** SelectItem list for the value of exempt i.e either true or false. */
	private List<SelectItem> exemptList = null;

	/**
	 * default constructor
	 */
	public FormStateW4Bean() {
		super("FormStateW4Bean");
	}

	/** constructor used by subclasses */
	public FormStateW4Bean(String classname) {
		super(classname);
	}

	public static FormStateW4Bean getInstance() {
		return (FormStateW4Bean)ServiceFinder.findBean("formStateW4Bean");
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		setContactDoc(contactDocument);
		setUpForm();
	}

	/**
	 * Method used to fetch the saved data for the selected form and to set that
	 * data in the Form instance.
	 */
	protected void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		if (relatedFormId != null) {
			form = getFormById(relatedFormId);
		}
		else {
			form = getBlankForm();
		}
		setForm(form);

		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}

		form.getAddress().getAddrLine1();

		if (relatedFormId == null) { // Call api to save the new form.
			populateForm(false); // Populate (auto-fill) new forms.
			form = (FormStateW4)FormService.getInstance().persistForm(form,
					PayrollFormType.CA_W4.getApiSaveUrl(), FormStateW4.class);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().merge(contactDoc);
		}
	}

	@Override
	public String actionEdit() {
		try {
			if (form != null && form.getAddress() == null) {
				form.setAddress(new Address());
			}
			calculateEditFlags(false);
			return super.actionEdit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String actionCancel() {
		refreshForm();

		return super.actionCancel();
	}

	/**
	 * Retrieve the form from the id associated with the form. Use micro-service
	 * to retrieve the form.
	 *
	 * LS-3060
	 */
	@Override
	public FormStateW4 getFormById(Integer id) {
		return (FormStateW4)FormService.getInstance().findById(id,
				PayrollFormType.CA_W4.getApiFindUrl(), FormStateW4.class);
	}

	@Override
	public FormStateW4 getBlankForm() {
		FormStateW4 formStateW4 = null;
		if (contactDoc != null) {
			formStateW4 = new FormStateW4(contactDoc.getFormType());
		}
		else {
			formStateW4 = new FormStateW4();
		}
		formStateW4.setAddress(new Address()); // LS-4141
		formStateW4.setMailingAddress(new Address()); // LS-4229
		formStateW4.setNonResidentAddress(new Address()); // LS-4237
		return formStateW4;
	}

	@Override
	public void refreshForm() {
		if (form != null) {
			form = (FormStateW4)FormService.getInstance().findById(form.getId(),
					PayrollFormType.CA_W4.getApiFindUrl(), FormStateW4.class);
		}
	}

	@Override
	public String actionSave() {
		try {
			FormService.getInstance().update(form, PayrollFormType.CA_W4.getApiUpdateUrl(),
					FormStateW4.class);

			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
//
//		// Any common save-time checks go here
//		// LS-3764
//		if (form.getAddress() != null && form.getFormType() != PayrollFormType.OH_W4) {
//			// Zip code validation for 5 digits
		if (form.getFormType() != PayrollFormType.OH_W4) {
			isValid = isZipValid();
		}

		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/**
	 * Method used to check the validity of fields in the form.
	 *
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;

		// Any common submit-time checks go here

		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFullName())) {
			isValid = issueErrorMessage("Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())) {
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}

		if (form.getFormType() != PayrollFormType.OH_W4) {
			isValid &= checkAddressValidMsg(form.getAddress());
			/*if (! isValid) {
				issueErrorMessage("Address", false, "");
			}*/
		}
		// This check is handled with additional conditions in FormStateCAW4Bean for CA_W4 form type and FormStateMAW4Bean MA_W4
		if (! form.getExempt() && ! form.getFormType().getAllowW4Exempt()) {
			if (form.getAllowances() == null) {
				isValid = issueErrorMessage("Allowances", false, "");
			}
		}

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * LS-4187
	 * @param Integer field Value
	 * @param string
	 * @return
	 */
	protected boolean checkZeroOne(Integer input, String field) {
		if (input == null || input == 0 || input == 1) {
			return true;
		}
		MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.ZeroOrOne", FacesMessage.SEVERITY_ERROR, field);

		return false;
	}

	/**
	 * LS-4026
	 * @param Integer field Value
	 * @param string
	 * @return
	 */
	protected boolean checkNegative(Integer input, String field) {
		if (input == null || (input != null && Integer.signum(input) != -1)) {
			return true;
		}
		MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.NonNegative", FacesMessage.SEVERITY_ERROR, field);

		return false;
	}

	/**
	 * LS-3997
	 *
	 * Validate a 5-digit zip code. Can be overridden by a sub-class if zip code
	 * validation is not needed.
	 */
	@Override
	protected boolean isZipValid() {
		boolean isValid = true;
		// Zip code validation for 5 digits
		if (! StringUtils.isEmpty(form.getAddress().getState())) {
			if (! form.getAddress().isZipValid()) {
				MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
				isValid = false;
			}
		}
		else {
			isValid = issueErrorMessage("State", false, "");
		}

		return isValid;
	}

	/**
	 * Auto-fill the State W-4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			populateForm(prompted);
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		User user = getContactDoc().getContact().getUser();
		user = getUserDAO().refresh(user);

		form.setFullName(user.getFullName());
		form.setFirstName(user.getFirstName());
		if (! StringUtils.isEmpty(user.getMiddleName())) { // LS-3711
			form.setMiddleInitial(user.getMiddleName().substring(0, 1));
			form.setFirstAndInitial(form.getFirstName() + " " + form.getMiddleInitial());
		}
		else {
			form.setMiddleInitial(null);
			form.setFirstAndInitial(form.getFirstName());
		}
		form.setLastName(user.getLastName());
		form.setSocialSecurity(user.getSocialSecurity());
		form.setDateofBirth(user.getBirthdate());
		if (user.getHomeAddress() != null) {
			form.getAddress().copyFrom(user.getHomeAddress());
		}
		else {
			// LS-3555 Clear out the fields if the user has not yet gone to
			// the My Account page to fill in the home address
			form.getAddress().clear();
		}
		if (form.getMailingAddress() != null) {
			if (user.getMailingAddress() != null) {
				form.getMailingAddress().copyFrom(user.getMailingAddress());
			}
			else {
				// LS-3555 Clear out the fields if the user has not yet gone to
				// the My Account page to fill in the mailing address
				form.getMailingAddress().clear();
			}
		}
	}

	/** See {@link #exemptList}. */
	public List<SelectItem> getExemptList() {
		if (exemptList == null) {
			exemptList = new ArrayList<>();
			exemptList.add(new SelectItem(false, "--"));
			exemptList.add(new SelectItem(true, "Exempt"));
		}
		return exemptList;
	}

	/** See {@link #exemptList}. */
	public void setExemptList(List<SelectItem> exemptList) {
		this.exemptList = exemptList;
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptCode(ValueChangeEvent event) {
		String exemptCode = (String)event.getNewValue();
		if (! StringUtils.isEmpty(exemptCode)) {
			form.setExempt(true);
		}
		else {
			form.setExempt(false);
		}
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus1(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);

		form.setExemptStatus1(exemptStatus);
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus2(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);

		form.setExemptStatus2(exemptStatus);
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus3(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);

		form.setExemptStatus3(exemptStatus);
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus4(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);
		form.setExemptStatus4(exemptStatus);
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus5(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);
		form.setExemptStatus5(exemptStatus);
	}

	/**
	 * set exempt value based on ExemptCode.
	 */
	public void listenExemptStatus6(ValueChangeEvent event) {
		Boolean exemptStatus = (event.getNewValue() != null ? (Boolean)event.getNewValue() : false);

		form.setExemptStatus6(exemptStatus);
	}
}
