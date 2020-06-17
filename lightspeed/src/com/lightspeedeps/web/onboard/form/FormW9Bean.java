package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.converter.FedIdConverter;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the W-9 form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormW9
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormW9Bean extends StandardFormBean<FormW9> implements Serializable {

	private static final long serialVersionUID = -466860256334174812L;

	private static final Log LOG = LogFactory.getLog(FormW9Bean.class);

	/** Boolean array used for the selection of Federal Tax Classification radio buttons. */
	private Boolean taxClassType[] = new Boolean[7];

	public FormW9Bean() {
		super("FormW9Bean");
		Arrays.fill(taxClassType, Boolean.FALSE);
	}

	public static FormW9Bean getInstance() {
		return (FormW9Bean) ServiceFinder.findBean("formW9Bean");
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
			LOG.debug("Form W9");
			for (TaxClassificationType type : TaxClassificationType.values()) {
				if (taxClassType[type.getIndex()]) {
					form.setCompanyType(type);
					break;
				}
			}
			FormW9DAO.getInstance().attachDirty(form);
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
		Arrays.fill(taxClassType, Boolean.FALSE);
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		if (relatedFormId != null) {
			setForm(FormW9DAO.getInstance().findById(relatedFormId));
			if (form.getCompanyType() != null) {
				int index = form.getCompanyType().getIndex();
				taxClassType[index] = true;
			}
		}
		else {
			setForm(getBlankForm());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab

		if (form.getFullName() == null) {
			FormW9DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	@Override
	public void submitted() {
		// LS-3659 Added Feature Flag for W-9 address unification functionality
		if(FF4JUtils.useFeature(FeatureFlagType.TTCO_W9_ADDR_UNIF_USER_PROFILE) && getIsTeamPayroll()) {
			// LS-3568 Update W9 information on my account tab, if W9 form is submitted.
			User user = contactDoc.getContact().getUser();
			refreshForm();

			// Update the address fields on the My Account tab so they
			// are in sync with the W-9 address values

			Address w9Address = form.getAddress();
			Address loanOutAddress = user.getLoanOutAddress() != null ? user.getLoanOutAddress() : new Address();
			Address loanOutMailingAddress = user.getLoanOutMailingAddress() != null ? user.getLoanOutMailingAddress() : new Address();
			Address originalLoanOutAddress = loanOutAddress.clone();
			Address originalLoanOutMailingAddress = loanOutMailingAddress.clone();

			if (! w9Address.equalsAddress(loanOutAddress)) {
				// create ActivityLog instance to record address change
				ActivityLog log = new ActivityLog(ActivityLog.TYPE_LOANOUT_ADDRESS_CHANGE, new Date());
				log.setUserAcct(user.getAccountNumber());
				log.setPermanentAddrChanged(true);
				if (user.getSameAsCorpAddr() || loanOutMailingAddress.isEmpty()) {
					log.setMailingAddrChanged(true);
				}
				ActivityLogDAO.getInstance().save(log);
			}

			// Set to the address from the W-9
			loanOutAddress.copyFrom(w9Address);

			// If loan out mailing address field is empty then update theloanOut mailing address
			// with the W-9 address and set the sameAsCorpAddr flag to true.
			if(loanOutMailingAddress.isEmpty()) {
				loanOutMailingAddress.copyFrom(w9Address);
				user.setSameAsCorpAddr(true);
			}
			else if(user.getSameAsCorpAddr()) {
				// If the sameAsCorpAddr flag is true, the
				// loan out mailing address also needs to be updated
				// to be in sync with the loan out address.
				loanOutMailingAddress.copyFrom(w9Address);
			}

			// LS-3486 LS-3729 Display popup to let user know that the field values
			// on My Account page have been updated.
			if (haveW9ValuesChanged(user, originalLoanOutAddress, originalLoanOutMailingAddress)) {
				user.setLoanOutAddress(loanOutAddress);
				user.setLoanOutMailingAddress(loanOutMailingAddress);
				updateUserW9Info(user);
				PopupBean popupBean = PopupBean.getInstance();
				popupBean.show(this, 0, "Form.MyAccount.Updated.Title", "Form.MyAccount.Updated.Text", "Confirm.OK", null);
			}
		}
	}

	/**
	 * LS-3729
	 *
	 * If any field values on the W-9 have changed, update any associated User fields
	 * @param user
	 * @param origAddr
	 * @param origMailingAddr
	 * @return
	 */
	private boolean haveW9ValuesChanged(User user, Address origAddr, Address origMailingAddr) {
		// Check to see if the Corp address or the mailing address has changed
		if(!origAddr.equalsAddress(user.getLoanOutAddress())) {
			return true;
		}
		else if(user.getSameAsCorpAddr() && !origMailingAddr.equalsAddress(user.getLoanOutMailingAddress())) {
			return true;
		}
		else if((StringUtils.isEmpty(user.getLoanOutCorpName()) && !StringUtils.isEmpty(form.getBusinessName()))
				|| (!StringUtils.isEmpty(user.getLoanOutCorpName()) && StringUtils.isEmpty(form.getBusinessName()))
				|| !form.getBusinessName().equals(user.getLoanOutCorpName())) {
			return true;
		}
		else if((StringUtils.isEmpty(user.getLlcType()) && !StringUtils.isEmpty(form.getTaxClassification()))
				|| (!StringUtils.isEmpty(user.getLlcType()) && StringUtils.isEmpty(form.getTaxClassification()))
				|| ((!StringUtils.isEmpty(form.getTaxClassification()) && !StringUtils.isEmpty(user.getLlcType())) // LS-3959 NPE fix.
						&& !form.getTaxClassification().equals(user.getLlcType()))) {
			return true;
		}
		else if((form.getCompanyType() != null && user.getTaxClassification() == null)
				|| (form.getCompanyType() == null && user.getTaxClassification() != null)
				|| form.getCompanyType() != user.getTaxClassification()) {
			return true;
		}
		else if(StringUtils.isEmpty(user.getSocialSecurity()) || !StringUtils.cleanTaxId(form.getSocialSecurity()).equals(StringUtils.cleanTaxId(user.getSocialSecurity()))) {
			return true;
		}
		else if(StringUtils.isEmpty(user.getFederalTaxId()) || !StringUtils.cleanTaxId(form.getFedidNumber()).equals(StringUtils.cleanTaxId(user.getFederalTaxId()))) {
			return true;
		}

		return false;
	}

	/**
	 * Auto-fill the W9 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			LOG.debug("");
			User user = getContactDoc().getContact().getUser();
			if (user != null) {
				user = getUserDAO().refresh(user);
				LOG.debug("Form id: " + getForm().getId());
				form.setFullName(user.getDisplayName());
				form.setSocialSecurity(user.getSocialSecurity());
				form.setFedidNumber(user.getFederalTaxId());
				form.setBusinessName(user.getLoanOutCorpName());
				// LS-3665 Auto fill tax classification type
				form.setCompanyType(user.getTaxClassification());
				// Set the radio buttons on the page
				Arrays.fill(taxClassType, Boolean.FALSE);
				for (TaxClassificationType type : TaxClassificationType.values()) {
					if (type == form.getCompanyType()) {
						taxClassType[type.getIndex()] = true;
					}
				}
				// LS-3665 Set the LLC type
				form.setTaxClassification(user.getLlcType());
				if (form.getAddress() == null) {
					form.setAddress(new Address());
				}
				if (user.getLoanOutAddress() != null && (! user.getLoanOutAddress().isEmpty())) {
					form.getAddress().copyFrom(user.getLoanOutAddress());
				}
				else if (user.getHomeAddress() != null && (! user.getHomeAddress().isEmpty())) {
					form.getAddress().copyFrom(user.getHomeAddress());
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
		boolean isTeamPayroll = false;

		Production prod = ProductionDAO.getInstance().refresh(contactDoc.getProduction());
		if (prod.getPayrollPref().getPayrollService() != null) {
			isTeamPayroll = prod.getPayrollPref().getPayrollService().getTeamPayroll();
		}
		LOG.debug("");
		if (StringUtils.isEmpty(form.getFullName())) {
			isValid = issueErrorMessage("Name", false, "");
		}

		if (form.getCompanyType() == null) {
			isValid = issueErrorMessage("Tax classification (line 3)", false, "");
		}
		if (form.getCompanyType() == TaxClassificationType.L && StringUtils.isEmpty(form.getTaxClassification())) {
			isValid = issueErrorMessage("LLC Tax Classification", false, "");
		}

		isValid &= checkAddressValidMsg(form.getAddress());

		boolean noSSN = StringUtils.isEmpty(StringUtils.cleanTaxId(form.getSocialSecurity()));
		String noFEIN =  FedIdConverter.checkTaxId(form.getFedidNumber());

		if ((! StringUtils.isEmpty(noFEIN)) && noFEIN.equals("Invalid")) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.ValidationMessage.FedIdNumber", FacesMessage.SEVERITY_ERROR);
		}

		// If Team is the payroll service validate that there are both SSN and FEIN numbers. LS-2152 LS-2153
		if (isTeamPayroll) {
			if (noSSN || StringUtils.isEmpty(noFEIN)) { // if SSN or FEIN# is missing - LS-2152
				isValid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage.SSNorFEINblank", FacesMessage.SEVERITY_ERROR);
			}
			// LS-2153 W-9 Tax Classification Validation
			if (isValid) {
				if (form.getCompanyType() != null) {
					if ((form.getCompanyType() == TaxClassificationType.L &&
							(! form.getTaxClassification().equals("C") &&
							! form.getTaxClassification().equals("S"))) ||
							(form.getCompanyType() != TaxClassificationType.L &&
							form.getCompanyType() != TaxClassificationType.C &&
							form.getCompanyType() != TaxClassificationType.S)) {
						PopupBean.getInstance().show(this, 0,
								"FormW9Bean.InvalidTaxClassification.Title",
								"FormW9Bean.InvalidTaxClassification.Text", "Confirm.OK", null);
						isValid = false;
					}
				}
			}
		}
		else { // SSN/FEIN checks for non-Team clients
			if (noSSN && StringUtils.isEmpty(noFEIN)) { // both empty
				isValid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage.BothSSNandFEINblank", FacesMessage.SEVERITY_ERROR);
			}
			if ((! noSSN) && (! StringUtils.isEmpty(noFEIN))) { // both specified
				isValid = false;
				MsgUtils.addFacesMessage("Form.ValidationMessage.BothSSNandFEINfilled", FacesMessage.SEVERITY_ERROR);
			}
			// This option is not available for Team clients.
			if (form.getCompanyType() == TaxClassificationType.O && StringUtils.isEmpty(form.getOtherTaxClassification())) {
				isValid = issueErrorMessage("Other Tax Classification", false, "");
			}
		}

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormW9 getFormById(Integer id) {
		return FormW9DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormW9DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormW9 getBlankForm() {
		FormW9 formW9 = new FormW9();
		formW9.setAddress(new Address());
		formW9.setVersion(FormW9.DEFAULT_W9_VERSION);
		return formW9;
	}

	/** Value change listener for Company type.
	 * @param event
	 */
	public void listenChangeCompanyType(ValueChangeEvent event) {
		refreshRadioButtons(event);
		try {
			TaxClassificationType taxType = null;
			for (TaxClassificationType type : TaxClassificationType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (taxClassType[type.getIndex()]) {
					taxType = type;
					form.setCompanyType(type);
					break;
				}
			}
			for (TaxClassificationType type : TaxClassificationType.values()) {
				if (type != form.getCompanyType()) {
					taxClassType[type.getIndex()] = false;
				}
			}
			if (! StringUtils.isEmpty(form.getTaxClassification()) && taxType != TaxClassificationType.L) {
				LOG.debug("Clear Tax Classification");
				form.setTaxClassification(null);
			}
			if (! StringUtils.isEmpty(form.getOtherTaxClassification()) && taxType != TaxClassificationType.O) {
				LOG.debug("Clear other Tax Classification");
				form.setOtherTaxClassification(null);
			}
		}
		catch (Exception e) {
			LOG.error("Error converting field id=" + event.getComponent().getId() + ": " + e);
		}
	}

	@Override
	public boolean checkSaveValid() {
		LOG.debug("");
		boolean isValid = true;

		Integer exemptPayeeCode = form.getExemptPayeeCode();
		if (exemptPayeeCode != null && (exemptPayeeCode < 1 || exemptPayeeCode > 13)) {
			isValid = false;
			MsgUtils.addFacesMessage("FormW9Bean.PayeeCodeValidationMessage", FacesMessage.SEVERITY_ERROR);
		}

		boolean validFACTA = true;
		String codeFATCA = null;
		if (! StringUtils.isEmpty(form.getfATCAReportingCode())) {
			form.setfATCAReportingCode(form.getfATCAReportingCode().trim().toUpperCase());
			codeFATCA = form.getfATCAReportingCode();
			validFACTA = (codeFATCA.length() == 1) &&
					(('A' <= codeFATCA.charAt(0) && codeFATCA.charAt(0) <= 'M'));
		}
		if (! validFACTA) {
			isValid = false;
			MsgUtils.addFacesMessage("FormW9Bean.FATCAValidationMessage", FacesMessage.SEVERITY_ERROR);
		}
		isValid = checkAddressValid(form.getAddress(), null);
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/**See {@link #taxClassType}. */
	public Boolean[] getTaxClassType() {
		return taxClassType;
	}
	/**See {@link #taxClassType}. */
	public void setTaxClassType(Boolean[] taxClassType) {
		this.taxClassType = taxClassType;
	}

	/**
	 * method to update W9 information on my account page when the W9 form is
	 * submitted. LS-3568
	 */
	public void updateUserW9Info(User user) {
		user = getUserDAO().refresh(user);

		user.setTaxClassification(form.getCompanyType());
		if (user.getLoanOutAddress() == null) {
			user.setLoanOutAddress(new Address());
		}
		user.getLoanOutAddress().copyFrom(form.getAddress()); // LS-3687
		user.setLlcType(form.getTaxClassification());
		user.setFederalTaxId(form.getFedidNumber());
		user.setLoanOutCorpName(form.getBusinessName());

		// LS-3666 Update the SSN if the SSN is blank on the My Account page
		if(StringUtils.isEmpty(user.getSocialSecurity()) && ! StringUtils.isEmpty(form.getSocialSecurity())) {
			user.setSocialSecurity(form.getSocialSecurity());
		}

		getUserDAO().attachDirty(user);
	}

	/** Listener invoked by ZIP code change which calls parent's method for
	 * further handling
	 * @param event
	 */
	public void listenZipCode(ValueChangeEvent event) {
		super.listenZipCode(event, form.getAddress());
	}
}
