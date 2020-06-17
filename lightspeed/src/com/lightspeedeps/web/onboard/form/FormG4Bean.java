package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.FormG4DAO;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.FormG4;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.converter.FedIdConverter;

/**
 * Backing bean for the G-4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormG4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormG4Bean extends StandardFormBean<FormG4> implements Serializable {

	private static final long serialVersionUID = -7946685766441833286L;

	private static final Log LOG = LogFactory.getLog(FormG4Bean.class);

	/** String literal used to store the current user selection of radio buttons
	 * (Marital status) */
	private String maritalChoice = null;
	public static final String ITEM_A= "a";
	public static final String ITEM_B= "b";
	public static final String ITEM_C= "c";
	public static final String ITEM_D= "d";
	public static final String ITEM_E= "e";

	/** Boolean array used for the selection of marital status radio buttons. */
	private Boolean maritalStatus[] = new Boolean[5];

	public FormG4Bean() {
		super("FormG4Bean");
		Arrays.fill(maritalStatus, Boolean.FALSE);
	}

	public static FormG4Bean getInstance() {
		return (FormG4Bean) ServiceFinder.findBean("formG4Bean");
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		setContactDoc(contactDocument);
		setUpForm();
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
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String actionSave() {
		try {
			LOG.debug("Form G4");
			FormG4DAO.getInstance().attachDirty(form);
			return super.actionSave();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Arrays.fill(maritalStatus, Boolean.FALSE);
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		if (relatedFormId != null) {
			setForm(FormG4DAO.getInstance().findById(relatedFormId));
			if (form.getMaritalLetter() != null) {
				if (form.getMaritalLetter().equals("A")) {
					maritalStatus[0] = true;
				}
				else if (form.getMaritalLetter().equals("B")) {
					maritalStatus[1] = true;
				}
				else if (form.getMaritalLetter().equals("C")) {
					maritalStatus[2] = true;
				}
				else if (form.getMaritalLetter().equals("D")) {
					maritalStatus[3] = true;
				}
				else if (form.getMaritalLetter().equals("E")) {
					maritalStatus[4] = true;
				}
			}
		}
		else {
			setForm(new FormG4());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1();  // Prevents LIE when returning to form mini-tab
		if (form.getFullName() == null) {
			FormG4DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	/**
	 * Determine if a document is ready to be Approved.
	 *
	 * @return True if the necessary fields have been completed.
	 */
	@Override
	public boolean checkApproveValid() {
		boolean valid = true;
		LOG.debug("");
		FormG4 formG4 = FormG4DAO.getInstance().findById(contactDoc.getRelatedFormId());
		if (formG4 != null && ((formG4.getTotalAllowances() != null && formG4.getTotalAllowances() > 14) ||
				(formG4.getNoGAIncome() || formG4.getSameExemptStates()))) {
			LOG.debug("");
			if (StringUtils.isEmpty(formG4.getEmployerName())) {
				valid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Employer's Name");
			}
			if (StringUtils.isEmpty(formG4.getEmployerAddress())) {
				valid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Employer's Address");
			}
			if (StringUtils.isEmpty(formG4.getEmployerFEIN())) {
				valid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Employer's FEIN");
			}
			if (StringUtils.isEmpty(formG4.getEmployerWHNumber())) {
				valid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Employer's WH Number");
			}
		}
		return valid;
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		isValid = checkAddressValid(form.getAddress(), null);
		String FEIN =  FedIdConverter.checkTaxId(form.getEmployerFEIN());
		if ((! StringUtils.isEmpty(FEIN)) && FEIN.equals("Invalid")) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.ValidationMessage.FedIdNumber", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getNoGAIncome() && form.getSameExemptStates()) {
			isValid = false;
			MsgUtils.addFacesMessage("FormG4Bean.ValidationMessage.Exemption", FacesMessage.SEVERITY_ERROR);
		}
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		boolean exempt = false;
		if (form.getNoGAIncome() || form.getSameExemptStates()) {
			exempt = true;
		}
		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFullName())) {
			isValid = issueErrorMessage("Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())){
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}
		isValid &= checkAddressValidMsg(form.getAddress());
		if (! exempt) {
			if (form.getMaritalA() == null && form.getMaritalB() == null && form.getMaritalC() == null &&
					form.getMaritalD() == null && form.getMaritalE() == null ) {
				isValid = issueErrorMessage("Marital Status", false, "");
			}
			if (form.getDependents() == null) {
				isValid = issueErrorMessage("Dependents", false, "");
			}
			if (StringUtils.isEmpty(form.getMaritalLetter())) {
				isValid = issueErrorMessage("Marital Letter", false, "");
			}
			if (form.getTotalAllowances() == null) {
				isValid = issueErrorMessage("Total Allowances", false, "");
			}
		}
		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * Auto-fill the G4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			form.setFullName(user.getFullName());
			form.setSocialSecurity(user.getSocialSecurity());
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (user.getHomeAddress() != null) {
				form.getAddress().copyFrom(user.getHomeAddress());
				form.getAddress().setAddrLine2(null);
				form.getAddress().setAddrLine1(user.getHomeAddress().getAddrLine1Line2());
			}
//			form.setMaritalLetter(user.getW4Marital());
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Listener for the radio buttons of Marital Status.
	 *
	 * @param event Event generated by framework
	 */
	public void listenValueChange(ValueChangeEvent event) {
		refreshRadioButtons(event);
		Boolean newVal = (Boolean) event.getNewValue();
		LOG.debug("Value = " + newVal);
		if (maritalStatus[0] && newVal) {
			form.setMaritalA((byte)1);
			form.setMaritalLetter("A");
			form.setMaritalB((byte) 0);
			form.setMaritalC((byte) 0);
			form.setMaritalD((byte) 0);
			form.setMaritalE((byte) 0);
		}
		else if (maritalStatus[1] && newVal) {
			form.setMaritalB((byte)1);
			form.setMaritalLetter("B");
			form.setMaritalA((byte) 0);
			form.setMaritalC((byte) 0);
			form.setMaritalD((byte) 0);
			form.setMaritalE((byte) 0);
		}
		else if (maritalStatus[2] && newVal) {
			form.setMaritalC((byte)1);
			form.setMaritalLetter("C");
			form.setMaritalA((byte) 0);
			form.setMaritalB((byte) 0);
			form.setMaritalD((byte) 0);
			form.setMaritalE((byte) 0);
		}
		else if (maritalStatus[3] && newVal) {
			form.setMaritalD((byte)1);
			form.setMaritalLetter("D");
			form.setMaritalA((byte) 0);
			form.setMaritalB((byte) 0);
			form.setMaritalC((byte) 0);
			form.setMaritalE((byte) 0);
		}
		else if (maritalStatus[4] && newVal) {
			form.setMaritalE((byte)1);
			form.setMaritalLetter("E");
			form.setMaritalA((byte) 0);
			form.setMaritalB((byte) 0);
			form.setMaritalC((byte) 0);
			form.setMaritalD((byte) 0);
		}
	}

	/**
	 * Listener for the Exempt checkboxes of Item 8.
	 *
	 * @param event Event generated by framework
	 */
	public void listenExemptChecked(ValueChangeEvent event) {
		LOG.debug("");
		boolean newValue = (boolean) event.getNewValue();
		if (newValue) {
			LOG.debug("");
			form.setMaritalA(null);
			form.setMaritalB(null);
			form.setMaritalC(null);
			form.setMaritalD(null);
			form.setMaritalE(null);
			setMaritalChoice(null);
			form.setDependents(null);
			form.setAdditionalAllow(null);
			form.setAddtlAmount(null);
			//Worksheet
			form.setOver65(false);
			form.setBlind(false);
			form.setSpouseOver65(false);
			form.setSpouseBlind(false);
			form.setBoxesChecked(null);
			form.getStdDeduction();
			form.setFedEstimated(null);
			form.setGaStdDeduction(null);
			form.getSubtractDeductions();
			form.setAllowableDeductions(null);
			form.getTotalDeducts();
			form.setEstNonWage(null);
			form.getEstDeductions();
			form.getDividedDeductions();
			// Item 7
			form.setMaritalLetter(null);
			form.getTotalAllowances();
		}
	}

	/**
	 * Method called when user clicks Auto-fill button for Employer fields (for the Item 9 fields)
	 * of G4. This will populate/fill the employer fields in the form
	 * with the information of the currently logged in approver and current Production.
	 *
	 * @return null navigation String
	 */
	public void actionAutoFillEmployerFields() {
		try {
			LOG.debug(" ");
			Production production = getProduction();

			form.setEmployerName(production.getStudio());
			if (production.getAddress() != null && ! production.getAddress().trimIsEmpty()) { // Employer's Address
				form.setEmployerAddress(production.getAddress().getAddrLine1() + ", " + production.getAddress().getCityStateZip());
			}
			// Onboarding Preferences data
			PayrollPreference payrollPref;
			payrollPref = production.getPayrollPref();

			if (payrollPref != null) {
				payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
				form.setEmployerFEIN(payrollPref.getFedidNumber());
			}
			if (! getEditMode()) {
				actionSave();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormG4 getFormById(Integer id) {
		return FormG4DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormG4DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormG4 getBlankForm() {
		FormG4 formG4 = new FormG4();
		formG4.setAddress(new Address());
		return formG4;
	}

	/** See {@link #maritalChoice}. */
	public String getMaritalChoice() {
		return maritalChoice;
	}
	/** See {@link #maritalChoice}. */
	public void setMaritalChoice(String maritalChoice) {
		this.maritalChoice = maritalChoice;
	}

	/** See {@link #maritalStatus}. */
	public Boolean[] getMaritalStatus() {
		return maritalStatus;
	}
	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(Boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

}
