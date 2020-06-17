package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.BankAccountType;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.popup.PinPromptBean;

/**
 * Backing bean for the Direct Deposit form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormDeposit
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormDepositBean extends StandardFormBean<FormDeposit> implements Serializable {

	private static final long serialVersionUID = -6452682772614777530L;

	private static final Log LOG = LogFactory.getLog(FormDepositBean.class);

	private static final int ACT_INITIAL = 20;

	/** True if initial for current account is clicked and false for second account.*/
	private boolean initial;

	/** Boolean array used for the selection of Currect Account type radio buttons. */
	private Boolean currentAcctType[] = new Boolean[2];

	/** Boolean array used for the selection of Second Account type radio buttons. */
	private Boolean secondAcctType[] = new Boolean[2];

	/** Boolean array used for the selection of Old Account type radio buttons. */
	private Boolean oldAcctType[] = new Boolean[2];

	public FormDepositBean() {
		super("FormDepositBean.");
		form = new FormDeposit(); // TEST
		resetBooleans();
	}

	public static FormDepositBean getInstance() {
		return (FormDepositBean) ServiceFinder.findBean("formDepositBean");
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(currentAcctType, Boolean.FALSE);
		Arrays.fill(secondAcctType, Boolean.FALSE);
		Arrays.fill(oldAcctType, Boolean.FALSE);
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		setContactDoc(contactDocument);
		setUpForm();
	}

	@Override
	public String actionEdit() {
		try {
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
			LOG.debug("Form Direct Deposit");
			for (BankAccountType type : BankAccountType.values()) {
				if (currentAcctType[type.getIndex()]) {
					form.setAccountType1(type);
				}
				if (secondAcctType[type.getIndex()]) {
					form.setAccountType2(type);
				}
				if (oldAcctType[type.getIndex()]) {
					form.setOldAccountType(type);
				}
			}
			clearUnusedFields();
			form = FormDepositDAO.getInstance().merge(form);
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
		resetBooleans();
		Integer relatedFormId = contactDoc.getRelatedFormId();
		if (relatedFormId != null) {
			setForm(FormDepositDAO.getInstance().findById(relatedFormId));
			if (form.getAccountType1() != null) {
				int index = form.getAccountType1().getIndex();
				currentAcctType[index] = true;
			}
			if (form.getAccountType2() != null) {
				int index = form.getAccountType2().getIndex();
				secondAcctType[index] = true;
			}
			if (form.getOldAccountType() != null) {
				int index = form.getOldAccountType().getIndex();
				oldAcctType[index] = true;
			}
		}
		else {
			setForm(new FormDeposit());
		}
		/*if (form.getAccountType1() == null) {
			form.setAccountType1(BankAccountType.CHK);
		}
		if (form.getAccountType2() == null) {
			form.setAccountType2(BankAccountType.CHK);
		}
		if (form.getOldAccountType() == null) {
			form.setOldAccountType(BankAccountType.CHK);
		}*/
		// Fill in defaults
		if (form != null && form.getEmployeeName() == null) {
			Production prod = getProduction();
			if (prod != null) {
				form.setClientName(prod.getStudio());
			}
			if (contactDoc.getRelatedFormId() == null) {
				FormDepositDAO.getInstance().attachDirty(form);
				contactDoc.setRelatedFormId(form.getId());
				ContactDocumentDAO.getInstance().attachDirty(contactDoc);
			}
		}
	}

	/** Value change listener for Current Bank Account Type radio buttons.
	 * @param evt
	 */
	public void listenCurrentAccountType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (BankAccountType type : BankAccountType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (currentAcctType[type.getIndex()]) {
					form.setAccountType1(type);
					break;
				}
			}
			for (BankAccountType type : BankAccountType.values()) {
				if (type != form.getAccountType1()) {
					currentAcctType[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for Second Bank Account Type radio buttons.
	 * @param evt
	 */
	public void listenSecondAccountType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (BankAccountType type : BankAccountType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (secondAcctType[type.getIndex()]) {
					form.setAccountType2(type);
					break;
				}
			}
			for (BankAccountType type : BankAccountType.values()) {
				if (type != form.getAccountType2()) {
					secondAcctType[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for Old Bank Account Type radio buttons.
	 * @param evt
	 */
	public void listenOldAccountType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (BankAccountType type : BankAccountType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (oldAcctType[type.getIndex()]) {
					form.setOldAccountType(type);
					break;
				}
			}
			for (BankAccountType type : BankAccountType.values()) {
				if (type != form.getOldAccountType()) {
					oldAcctType[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}


	@Override
	public FormDeposit getFormById(Integer id) {
		return FormDepositDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormDepositDAO.getInstance().refresh(form);
		}
	}

	@Override
	public FormDeposit getBlankForm() {
		return new FormDeposit();
	}

	public String actionInitial() {
		LOG.debug("Form Direct Deposit action Initial");
		LOG.debug("value of initial " + initial);
		boolean isValid = true;
		String blankField = null;
		if (initial) {
			if (form.getAccountType1() == null) {
				isValid = false;
				blankField = "Account Type";
			}
			else if (form.getAccountNumber1() == null || form.getAccountNumber1().isEmpty()) {
				isValid = false;
				blankField = "Account Number";
			}
			else if (form.getaBARouting1() == null || form.getaBARouting1().isEmpty()) {
				isValid = false;
				blankField = "ABA Routing";
			}
			else if (form.getBankName1() == null || form.getBankName1().isEmpty()) {
				isValid = false;
				blankField = "Bank Name";
			}
		}
		else {
			if (form.getAccountType2() == null) {
				isValid = false;
				blankField = "Account Type";
			}
			else if (form.getAccountNumber2() == null || form.getAccountNumber2().isEmpty()) {
				isValid = false;
				blankField = "Account Number";
			}
			else if (form.getaBARouting2() == null || form.getaBARouting2().isEmpty()) {
				isValid = false;
				blankField = "ABA Routing";
			}
			else if (form.getBankName2() == null || form.getBankName2().isEmpty()) {
				isValid = false;
				blankField = "Bank Name";
			}
		}
		if (! isValid) {
			MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage",
					FacesMessage.SEVERITY_ERROR, blankField, (initial ? "Current" : "Second"));
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
		LOG.debug("Form Direct Deposit action Initial Ok");
		// Create a ContactDocEvent for Initial action
		ContactDocEvent evt = ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.INITIAL);
//		contactDoc.getContactDocEvents().add(evt);
		if (initial) {
			LOG.debug("");
			form.setBankInitial1(evt);
		}
		else {
			LOG.debug("");
			form.setBankInitial2(evt);
		}
		//TODO DH: Creating problem if user clicks cancel button. So what to do with Initial event if user clicks cancel?
		FormDepositDAO.getInstance().attachDirty(form);
		return null;
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		User currentUser = SessionUtils.getCurrentUser();
		String accountType = null;
		clearUnusedFields(); // erase fields unrelated to action setting
		if (StringUtils.isEmpty(form.getEmployeeName())) {
			isValid = issueErrorMessage("Employee Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())){
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		else if (currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}
		if (form.getFirstBank() || form.getChangeAccount()) {
			//Required fields for current bank account details
			accountType = "Current";
			if (form.getAccountType1() == null) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Account Type", accountType);
			}
			if (StringUtils.isEmpty(form.getAccountNumber1())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Account Number", accountType);
			}
			if (StringUtils.isEmpty(form.getaBARouting1())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "ABA Routing", accountType);
			}
			if (StringUtils.isEmpty(form.getBankName1())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Bank Name", accountType);
			}
			if (form.getBankInitial1() == null) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Bank Initial", accountType);
			}
			if (form.getChangeAccount()) {
				accountType = "Change";
				// Required fields for Change bank account details
				if (form.getOldAccountType() == null) {
					isValid = false;
					MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Old Account Type", accountType);
				}
				if (StringUtils.isEmpty(form.getOldAccountNumber())) {
					isValid = false;
					MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Old Account Number", accountType);
				}
				if (StringUtils.isEmpty(form.getOldABARouting())) {
					isValid = false;
					MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Old ABA Routing", accountType);
				}
				if (StringUtils.isEmpty(form.getOldBankName())) {
					isValid = false;
					MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Old Bank Name", accountType);
				}
			}
		}
		if (form.getSecondBank()) { // Note: could have both 1st & second checked
			accountType = "Second";
			// Required fields for Second bank account details
			if (form.getBankAmount2() == null) { // LS-3477
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account Deposit amount", accountType);
			}
			if (form.getAccountType2() == null) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account Type", accountType);
			}
			if (StringUtils.isEmpty(form.getAccountNumber2())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account Number", accountType);
			}
			if (StringUtils.isEmpty(form.getaBARouting2())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account ABA Routing", accountType);
			}
			if (StringUtils.isEmpty(form.getBankName2())) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account Bank Name", accountType);
			}
			if (form.getBankInitial2() == null) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, "Second Account Bank Initial", accountType);
			}
		}

		isValid &= isValidSsn(form.getSocialSecurity());
		setSubmitValid(isValid);
		/*if (! isValid) {
			if (accountType != null) {
				MsgUtils.addFacesMessage("FormDepositBean.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField, accountType);
			}
			else {
				MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
			}
		}*/
		return super.checkSubmitValid();
	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenChangeDirectDepositType (ValueChangeEvent evt) {
		try {
			LOG.debug("");
			if (evt.getNewValue() != null) {
				String id = evt.getComponent().getId();
				LOG.debug("ID = " + id);
				if (form.getFirstBank() && id.equals("first")) {
					form.setChangeAccount(false);
					form.setStopDeposit(false);
				}
				else if (form.getSecondBank() && id.equals("second")) {
					form.setChangeAccount(false);
					form.setStopDeposit(false);
				}
				else if (form.getChangeAccount() && id.equals("change")) {
					form.setFirstBank(false);
					form.setSecondBank(false);
					form.setStopDeposit(false);
				}
				else if (form.getStopDeposit()  && id.equals("stop")) {
					form.setFirstBank(false);
					form.setSecondBank(false);
					form.setChangeAccount(false);
				}
				// for now, try clearing just when submitted.
//				clearUnusedFields();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Clear out the fields on the form that are irrelevant based
	 * on the action settings selected by the user.
	 */
	private void clearUnusedFields() {
		if (form.getFirstBank() || form.getSecondBank()) {
			clearOldAccount();
			if (! form.getFirstBank()) {
				clearAccount1();
			}
			if (! form.getSecondBank()) {
				clearAccount2();
			}
		}
		else if (form.getChangeAccount()) {
			// Keep account 1 and "old"
			clearAccount2();
		}
		else if (form.getStopDeposit()) {
			// clear everything
			clearAccount1();
			clearAccount2();
			clearOldAccount();
		}
	}

	/**
	 * Clear the fields in the "Current Bank Account" section
	 * of the form.
	 */
	private void clearAccount1() {
		form.setAccountType1(null);
		form.setAccountName1(null);
		form.setAccountNumber1(null);
		form.setaBARouting1(null);
		form.setBankName1(null);
		form.setBankPhone1(null);
		form.setBankAddress1(null);
	}

	/**
	 * Clear the fields in the "Second Bank Account" section
	 * of the form.
	 */
	private void clearAccount2() {
		form.setBankAmount2(null); // LS-3477
		form.setAccountType2(null);
		form.setAccountName2(null);
		form.setAccountNumber2(null);
		form.setaBARouting2(null);
		form.setBankName2(null);
		form.setBankPhone2(null);
		form.setBankAddress2(null);
	}

	/**
	 * Clear the fields in the "Change Bank Account" section
	 * of the form.
	 */
	private void clearOldAccount() {
		form.setOldAccountType(null);
		form.setOldAccountName(null);
		form.setOldAccountNumber(null);
		form.setOldABARouting(null);
		form.setOldBankName(null);
	}

	/**
	 * Auto-fill the Deposit form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			LOG.debug("");
			fillFields();
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Fill Deposit form fields from other sources, either as user's
	 * request (Auto-fill), or upon initial viewing.
	 */
	private void fillFields() {
		User user = contactDoc.getContact().getUser();
		if (user != null) {
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			form.setEmployeeName(user.getDisplayName());
			form.setSocialSecurity(user.getSocialSecurity());
			form.setEmailAddress(user.getEmailAddress());
			Production prod = getProduction();
			if (prod != null) {
				form.setClientName(prod.getStudio());
			}
			form.setEmployeePhone(user.getPrimaryPhone());
		}
	}

	@Override
	public boolean checkSaveValid() {
		LOG.debug("");
		boolean isValid = true;
		String abaRouting = null;
		String accountType = null;
		if ((form.getFirstBank() || form.getSecondBank()) && (form.getChangeAccount() || form.getStopDeposit())) {
			isValid = false;
			MsgUtils.addFacesMessage("FormDepositBean.OptionsValidationMessage", FacesMessage.SEVERITY_ERROR);
		}
		else if (form.getChangeAccount() && form.getStopDeposit()) {
			isValid = false;
			MsgUtils.addFacesMessage("FormDepositBean.OptionsValidationMessage", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getFirstBank() && form.getaBARouting1() != null && (! form.getaBARouting1().isEmpty())) {
			if (form.getaBARouting1().length() != 9) {
				isValid = false;
				abaRouting = form.getaBARouting1();
				accountType = "Current";
				MsgUtils.addFacesMessage("FormDepositBean.ABARoutingValidationMessage", FacesMessage.SEVERITY_ERROR, abaRouting, accountType);
			}
		}
		if (form.getSecondBank() && form.getaBARouting2() != null && (! form.getaBARouting2().isEmpty())) {
			if (form.getaBARouting2().length() != 9) {
				isValid = false;
				abaRouting = form.getaBARouting2();
				accountType = "Second";
				MsgUtils.addFacesMessage("FormDepositBean.ABARoutingValidationMessage", FacesMessage.SEVERITY_ERROR, abaRouting, accountType);
			}
		}
		if (form.getBankAmount2() != null) {
			if (form.getBankPercent2() &&
					(form.getBankAmount2().compareTo(Constants.DECIMAL_100) > 0 || form.getBankAmount2().signum() <= 0)) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.PercentInvalid", FacesMessage.SEVERITY_ERROR);
			}
			if (! form.getBankPercent2() && form.getBankAmount2().signum() <= 0) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.AmountInvalid", FacesMessage.SEVERITY_ERROR);
			}
			if (! form.getBankPercent2() && form.getBankAmount2().compareTo(Constants.DECIMAL_1_MILLION) >= 0) {
				isValid = false;
				MsgUtils.addFacesMessage("FormDepositBean.AmountInvalid", FacesMessage.SEVERITY_ERROR);
			}
		}
		if (form.getChangeAccount() && form.getOldABARouting() != null && (! form.getOldABARouting().isEmpty())) {
			if (form.getOldABARouting().length() != 9) {
				isValid = false;
				abaRouting = form.getOldABARouting();
				accountType = "Change";
				MsgUtils.addFacesMessage("FormDepositBean.ABARoutingValidationMessage", FacesMessage.SEVERITY_ERROR, abaRouting, accountType);
			}
		}
		setSaveValid(isValid);
		/*if (! isValid) {
			if (accountType != null) {
				MsgUtils.addFacesMessage("FormDepositBean.ABARoutingValidationMessage", FacesMessage.SEVERITY_ERROR, abaRouting, accountType);
			}
		}*/
		return super.checkSaveValid();
	}

	/**See {@link #initial}. */
	public boolean getInitial() {
		return initial;
	}
	/**See {@link #initial}. */
	public void setInitial(boolean initial) {
		this.initial = initial;
	}

	/**See {@link #currentAcctType}. */
	public Boolean[] getCurrentAcctType() {
		return currentAcctType;
	}
	/**See {@link #currentAcctType}. */
	public void setCurrentAcctType(Boolean[] currentAcctType) {
		this.currentAcctType = currentAcctType;
	}

	/**See {@link #secondAcctType}. */
	public Boolean[] getSecondAcctType() {
		return secondAcctType;
	}
	/**See {@link #secondAcctType}. */
	public void setSecondAcctType(Boolean[] secondAcctType) {
		this.secondAcctType = secondAcctType;
	}

	/**See {@link #oldAcctType}. */
	public Boolean[] getOldAcctType() {
		return oldAcctType;
	}
	/**See {@link #oldAcctType}. */
	public void setOldAcctType(Boolean[] oldAcctType) {
		this.oldAcctType = oldAcctType;
	}

}
