package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.FormIndemDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.FormIndem;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.converter.FedIdConverter;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * Backing bean for the Indemnification form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormIndem
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormIndemBean extends StandardFormBean<FormIndem> implements Serializable {

	private static final long serialVersionUID = -3750840881138550084L;

	private static final Log LOG = LogFactory.getLog(FormW9Bean.class);

	public FormIndemBean() {
		super("FormIndemBean");
	}

	public static FormIndemBean getInstance() {
		return (FormIndemBean) ServiceFinder.findBean("formIndemBean");
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
			LOG.debug("Form Indemnification");
			FormIndemDAO.getInstance().attachDirty(form);
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
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		if (relatedFormId != null) {
			setForm(FormIndemDAO.getInstance().findById(relatedFormId));
		}
		else {
			setForm(new FormIndem());
		}
		if (getForm().getAddress() == null) {
			getForm().setAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (contactDoc.getRelatedFormId() == null) {
			FormIndemDAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	/**
	 * Auto-fill the Indemnification form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			LOG.debug("");
			Contact contact = getContactDoc().getContact();
			contact = ContactDAO.getInstance().refresh(contact);
			User user = contact.getUser();
			if (user != null) {
				user = UserDAO.getInstance().refresh(user);
				LOG.debug("Form id: " + getForm().getId());
				form.setCompanyName(user.getLoanOutCorpName());
				form.setWorkerName(user.getFirstNameLastName());
				form.setCorpOfficerName(user.getFirstNameLastName());
				form.setFullCompanyName(user.getLoanOutCorpName());
				form.setFedidNumber(user.getFederalTaxIdFmtd());
				form.setStateOfReg(user.getIncorporationState());
				if (form.getAddress() == null) {
					form.setAddress(new Address());
				}
				if (user.getLoanOutAddress() != null && (! user.getLoanOutAddress().isEmpty())) {
					form.getAddress().copyFrom(user.getLoanOutAddress());
				}
				form.setEmail(user.getEmailAddress());
				form.setTelephone(user.getPrimaryPhone());
			}
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		String blankField = null;
		if (StringUtils.isEmpty(form.getCompanyName())) {
			isValid = false;
			blankField = "Company";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		if (StringUtils.isEmpty(form.getWorkerName())) {
			isValid = false;
			blankField = "Worker Name";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		if (StringUtils.isEmpty(form.getCorpOfficerName())) {
			isValid = false;
			blankField = "Corporate Officer Name";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		if (StringUtils.isEmpty(form.getFullCompanyName())) {
			isValid = false;
			blankField = "Company Name";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		if (form.getFedidNumber() == null || StringUtils.cleanTaxId(form.getFedidNumber()) == null ||
				StringUtils.cleanTaxId(form.getFedidNumber()).isEmpty()) {
			isValid = false;
			blankField = "Federal ID Number";
			if (StringUtils.isEmpty(form.getFedidNumber()) || ! editMode) {
				// in edit mode, checkSaveValid already issue a message for this.
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
			}
		}
		if (StringUtils.isEmpty(form.getStateOfReg())) {
			isValid = false;
			blankField = "State of Registration";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}

		isValid &= checkAddressValidMsg(form.getAddress());

		if (StringUtils.isEmpty(form.getTelephone())) {
			isValid = false;
			blankField = "Telephone number";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		if (StringUtils.isEmpty(form.getEmail())) {
			isValid = false;
			blankField = "Email Address";
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		else if (! EmailValidator.isValidEmail(form.getEmail())) {
			isValid = false;
			MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage.Email", FacesMessage.SEVERITY_ERROR);
		}

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormIndem getFormById(Integer id) {
		return FormIndemDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormIndemDAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormIndem getBlankForm() {
		FormIndem formIndem = new FormIndem();
		formIndem.setAddress(new Address());
		return formIndem;
	}

	/**
	 * Action method for the "Print" button on the Start Forms mini-tab. Called
	 * via ContactFormBean.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionPrint() {
		try {
			if (contactDoc != null) {
				LOG.debug("");
				Integer id = contactDoc.getRelatedFormId();
				FormIndem formIndem = null;
				if (id != null) {
					formIndem = FormIndemDAO.getInstance().findById(id);
				}
				else {
					formIndem = new FormIndem();
					formIndem.setId(0); // this retrieves blank Indemnification record in database!
				}
				if (formIndem != null) {
					ReportBean report = ReportBean.getInstance();
					formIndem = FormIndemDAO.getInstance().refresh(formIndem);
					report.generateFormIndem(formIndem, null, contactDoc.getId(), contactDoc.getStatus().name());
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
	 * Method used to check the validity of fields in the form. Note that when
	 * this is called, FormIndem.trim() has already been called to trim and clean
	 * our fields.
	 *
	 * @return isValid: true if all validated fields are correct, false
	 *         otherwise. When false is returned, one or more error messages
	 *         have also been issued.
	 */
	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		//String field = "";
		String FEIN =  FedIdConverter.checkTaxId(form.getFedidNumber());
		if ((! StringUtils.isEmpty(FEIN)) && FEIN.equals("Invalid")) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.ValidationMessage.FedIdNumber", FacesMessage.SEVERITY_ERROR);
		}
		isValid &= checkAddressValid(form.getAddress(), null);
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

}
