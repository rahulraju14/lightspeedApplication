package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocEventDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.FormMtaDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.ContactDocEvent;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.FormMTA;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.type.TrustAccountType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.popup.PinPromptBean;
import com.lightspeedeps.web.report.ReportBean;

/**
 * Backing bean for the MTA form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormMTA
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormMtaBean extends StandardFormBean<FormMTA> implements Serializable {

	private static final long serialVersionUID = -2316864899814730076L;

	private static final Log LOG = LogFactory.getLog(FormMtaBean.class);

	/** Boolean to used to hold the value of checkbox for "Minor lives in CA". */
	private boolean nativeCa;

	/** Boolean to used to hold the value of checkbox for "Minor lives in NY". */
	private boolean nativeNy;

	private static final int ACT_INITIAL = 20;

	/** Boolean array used for the selection of Trust Account type radio buttons. */
	private Boolean trustAcctType[] = new Boolean[3];

	public FormMtaBean() {
		super("FormMtaBean.");
		Arrays.fill(trustAcctType, Boolean.FALSE);
	}

	public static FormMtaBean getInstance() {
		return (FormMtaBean) ServiceFinder.findBean("formMtaBean");
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
			if (form != null && form.getBankAddress() == null) {
				form.setBankAddress(new Address());
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
			LOG.debug("Form MTA");
			if (form.getNoTrustAccount()) {
				setAccountInfoAsNull();
			}
			else {
				for (TrustAccountType type : TrustAccountType.values()) {
					if (trustAcctType[type.getIndex()]) {
						form.setTrustAccountType(type);
						break;
					}
				}
			}
			FormMtaDAO.getInstance().attachDirty(form);
			return super.actionSave();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	/**
	 * set Acc info as null when No trust account is checked
	 */
	private void setAccountInfoAsNull() {
		form.setAccountName(null);
		form.setTrusteeName(null);
		form.setTrustAccountType(null);
		form.setBankName(null);
		form.setBankAddress(new Address());
		form.setRoutingNumber(null);
		form.setAccountNumber(null);
		form.setBankRepresentative(null);
		form.setBankPhone(null);
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Arrays.fill(trustAcctType, Boolean.FALSE);
		Integer relatedFormId = contactDoc.getRelatedFormId();
		if (relatedFormId != null) {
			setForm(FormMtaDAO.getInstance().findById(relatedFormId));
			if (form.getTrustAccountType() != null) {
				int index = form.getTrustAccountType().getIndex();
				trustAcctType[index] = true;
			}
		}
		else {
			setForm(new FormMTA());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (form.getBankAddress() == null) {
			form.setBankAddress(new Address());
		}
		form.getBankAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (form.getFirstName() == null) {
			FormMtaDAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	/**
	 * Auto-fill the MTA form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			LOG.debug("");
			User user = getContactDoc().getContact().getUser();
			if (user != null) {
				user = UserDAO.getInstance().refresh(user);
				LOG.debug("Form id: " + getForm().getId());
				/* Child's Info.
				 * form.setFirstName(user.getFirstName());
				form.setMiddleName(user.getMiddleName());
				form.setLastName(user.getLastName());
				form.setDateOfBirth(user.getBirthdate());*/
				form.setTrusteeName(user.getFirstNameLastName());
				form.setParentName(user.getFirstNameLastName());
				form.setParentPhone(user.getPrimaryPhone());
				if (form.getAddress() == null) {
					form.setAddress(new Address());
				}
				if (user.getHomeAddress() != null) {
					form.getAddress().copyFrom(user.getHomeAddress());
				}
				//form.setSocialSecurity(user.getSocialSecurity());
				form.setEmailAddress(user.getEmailAddress());
				Production prod = getProduction();
				if (prod != null) {
					form.setProductionCompany(prod.getTitle());
					form.setProject(SessionUtils.getCurrentOrViewedProject().getEpisode());
				}
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
		//User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())){
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		// Form SSN is not the user's SSN - it's the minor's SSN - skip compare for now.
//		else if (currentUser != null &&
//				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
//				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
//			isValid = issueErrorMessage("", false, ".SocialSecurity");
//		}
		if (! form.getNoTrustAccount()) {
			if (StringUtils.isEmpty(form.getAccountName())) {
				isValid = issueErrorMessage("Account Name", false, "");
			}
			if (StringUtils.isEmpty(form.getTrusteeName())) {
				isValid = issueErrorMessage("Trustee Name", false, "");
			}
			if (form.getTrustAccountType() == null) {
				isValid = issueErrorMessage("Trust Account Type", false, "");
			}
			if (StringUtils.isEmpty(form.getBankName())) {
				isValid = issueErrorMessage("Bank Name", false, "");
			}
			isValid &= checkAddressValidMsg(form.getAddress());
			/*if ((blankField = checkAddressValid(form.getAddress())) != null) {
				isValid = false;
				blankField = "Bank " + blankField;
			}*/
			if (StringUtils.isEmpty(form.getRoutingNumber())) {
				isValid = issueErrorMessage("Routing Number", false, "");
			}
			if (StringUtils.isEmpty(form.getAccountNumber())) {
				isValid = issueErrorMessage("Account Number", false, "");
			}
			if (StringUtils.isEmpty(form.getBankRepresentative())) {
				isValid = issueErrorMessage("Bank Representative", false, "");
			}
			if (StringUtils.isEmpty(form.getBankPhone())) {
				isValid = issueErrorMessage("Bank Phone", false, "");
			}
		}
		if (StringUtils.isEmpty(form.getParentName())) {
			isValid = issueErrorMessage("Parent Name", false, "");
		}
		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		BigDecimal trustPercent  = form.getTrustPercent();
		if (trustPercent != null && trustPercent.intValue() != 0 &&
				((trustPercent.compareTo(new BigDecimal(15)) == -1) || (trustPercent.compareTo(new BigDecimal(100)) == 1))) {
			isValid = false;
			MsgUtils.addFacesMessage("FormMtaBean.ValidationMessage", FacesMessage.SEVERITY_ERROR);
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && (StringUtils.cleanTaxId(form.getSocialSecurity()) == null ||
				StringUtils.cleanTaxId(form.getSocialSecurity()).isEmpty())) {
			isValid = false;
			MsgUtils.addFacesMessage("StartForm.BadSocial", FacesMessage.SEVERITY_ERROR);
		}
		isValid &= checkAddressValid(form.getAddress(), "MinorZipCode");
		isValid &= checkAddressValid(form.getBankAddress(), "BankZipCode");
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	public boolean getHasRequiredFields() {
		boolean missingData =	// If ANY of these fields are empty, we're missing something
				form.getNativeState() == null ||
				StringUtils.isEmpty(form.getFirstName()) ||
				StringUtils.isEmpty(form.getLastName()) ||
				form.getDateOfBirth() == null ||
				StringUtils.isEmpty(form.getSocialSecurity()) ||
				form.getAddress().isEmpty() ||
				form.getTrustPercent() == null ||
				StringUtils.isEmpty(form.getAccountName()) ||
				StringUtils.isEmpty(form.getTrusteeName()) ||
				form.getTrustAccountType() == null ||
				StringUtils.isEmpty(form.getBankName()) ||
				form.getBankAddress().isEmpty() ||
				StringUtils.isEmpty(form.getRoutingNumber()) ||
				StringUtils.isEmpty(form.getAccountNumber());
		LOG.debug(" Has required felds : " + (! missingData));
		return ! missingData;
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormMTA getFormById(Integer id) {
		return FormMtaDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormMtaDAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormMTA getBlankForm() {
		FormMTA formMTA = new FormMTA();
		formMTA.setAddress(new Address());
		formMTA.setBankAddress(new Address());
		return formMTA;
	}

	/** Value change listener for minor lives in CA/NY Checkboxes.
	 * @param event
	 */
	public void listenChangeNativeState(ValueChangeEvent event) {
		try {
			LOG.debug("new val = " + event.getNewValue());
			if (event.getNewValue() != null) {
				String id = event.getComponent().getId();
				LOG.debug("ID = " + id);
				if (nativeCa && id.equals("Ca")) {
					nativeNy = false;
					form.setNativeState("Ca");
				}
				else if (nativeNy && id.equals("Ny")) {
					nativeCa = false;
					form.setNativeState("Ny");
				}
				else {
					form.setNativeState("null");
				}
				form.setTrustAccountType(null);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
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
				FormMTA formMTA = null;
				if (id != null) {
					formMTA = FormMtaDAO.getInstance().findById(id);
				}
				else {
					formMTA = new FormMTA();
					formMTA.setId(0); // this retrieves blank MTA record in database!
				}
				if (formMTA != null) {
					ReportBean report = ReportBean.getInstance();
					formMTA = FormMtaDAO.getInstance().refresh(formMTA);
					report.generateFormMta(formMTA, null, contactDoc.getId(), contactDoc.getStatus().name());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
	        MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionInitial() {
		LOG.debug("Form MTA action Initial");
		boolean isValid = true;
		BigDecimal trustPercent  = form.getTrustPercent();
		if (trustPercent == null || NumberUtils.isEmpty(trustPercent) || trustPercent.equals(BigDecimal.ZERO) ||
				((trustPercent.compareTo(new BigDecimal(15)) == -1) || (trustPercent.compareTo(new BigDecimal(100)) == 1))) {
			isValid = false;
		}
		if (! isValid) {
			MsgUtils.addFacesMessage("FormMtaBean.ValidationMessage", FacesMessage.SEVERITY_ERROR);
		}
		else {
			PinPromptBean bean = PinPromptBean.getInstance();
			bean.promptPin(this, ACT_INITIAL, getMessagePrefix() + "PinSubmitSelf.");
			bean.setPinOnly(true);
			bean.setPassword(getvUser().getPassword());
			bean.setTitle(MsgUtils.getMessage("FormDepositBean.PinPrompt.Initials.Title"));
			bean.setMessage(MsgUtils.getMessage("FormDepositBean.PinPrompt.Initials.Message"));
		}
		return null;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_INITIAL:
				res = actionInitialOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	public String actionInitialOk() {
		LOG.debug("Form MTA action Initial Ok");
		// Create a ContactDocEvent for Initial action
		ContactDocEvent evt = ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.INITIAL);
//		contactDoc.getContactDocEvents().add(evt);
		form.setTrustInitial(evt);
		return null;
	}

	/** Value change listener for Trust Account Type radio buttons.
	 * @param evt
	 */
	public void listenTrustAccountType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (TrustAccountType type : TrustAccountType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (trustAcctType[type.getIndex()]) {
					form.setTrustAccountType(type);
					break;
				}
			}
			for (TrustAccountType type : TrustAccountType.values()) {
				if (type != form.getTrustAccountType()) {
					trustAcctType[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**See {@link #nativeCa}. */
	public boolean getNativeCa() {
		return nativeCa;
	}
	/**See {@link #nativeCa}. */
	public void setNativeCa(boolean nativeCa) {
		this.nativeCa = nativeCa;
	}

	/**See {@link #nativeNy}. */
	public boolean getNativeNy() {
		return nativeNy;
	}
	/**See {@link #nativeNy}. */
	public void setNativeNy(boolean nativeNy) {
		this.nativeNy = nativeNy;
	}

	/**See {@link #trustAcctType}. */
	public Boolean[] getTrustAcctType() {
		return trustAcctType;
	}
	/**See {@link #trustAcctType}. */
	public void setTrustAcctType(Boolean[] trustAcctType) {
		this.trustAcctType = trustAcctType;
	}

}
