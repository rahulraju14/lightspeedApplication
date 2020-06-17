package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the W-4 form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormW4
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormW4Bean extends StandardFormBean<FormW4> implements Serializable {

	/** */
	private static final long serialVersionUID = -2954728620028138877L;

	private static final Log LOG = LogFactory.getLog(FormW4Bean.class);

	/** SelectItem list for the value of exempt i.e either true or false.*/
	private List<SelectItem> exemptList = null;

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];

	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
//	private static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";
	private static final String MARITAL_STATUS_HEAD_HOUSEHOLD = "h";

	// DAO class
	private transient StartFormDAO startFormDAO = null;

//	private boolean showExempt = false;

	public FormW4Bean() {
		super("FormW4Bean");
		resetBooleans();
	}

	public static FormW4Bean getInstance() {
		return (FormW4Bean) ServiceFinder.findBean("formW4Bean");
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormW4 getFormById(Integer id) {
		return FormW4DAO.getInstance().findById(id);
	}

	private StartFormDAO getStartFormDAO() {
		if(startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}

		return startFormDAO;
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormW4DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormW4 getBlankForm() {
		FormW4 formW4 = new FormW4();
		formW4.setAddress(new Address());
		formW4.setVersion(FormW4.DEFAULT_W4_VERSION);
		if (FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_2020)) {
			formW4.setVersion(FormW4.W4_VERSION_2020);
		}
		return formW4;
	}

	@Override
	public String actionEdit() {
		try {
			if (form.getAddress() == null) {
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
			LOG.debug("Form W4");
			if (form.getLastName() != null) {
				// LS-1745, changes for the last name
				String name = DocumentService.checkSuffix(form.getLastName());
				form.setLastName(name);
			}
			FormW4DAO.getInstance().attachDirty(form);
			return super.actionSave();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}


	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#compareFormAccountDetails(com.lightspeedeps.model.User)
	 */
	@Override
	public User compareFormAccountDetails(User user) {
		boolean valid = true;
		LOG.debug(" ");
		LOG.debug("Form id: " + getForm().getId());

		if (! StringUtils.isEmpty(form.getFirstName() ) && ! form.getFirstName().equals(user.getFirstName())) {
			user.setFirstName(form.getFirstName());
			valid = false;
		}
		if (! StringUtils.isEmpty(form.getLastName()) && ! form.getLastName().equals(user.getLastName())) {
			user.setLastName(form.getLastName());
			valid = false;
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && ! form.getSocialSecurity().equals(user.getSocialSecurity())) {
			user.setSocialSecurity(form.getSocialSecurity());
			valid = false;
		}
		if (form.getMarital() != null && ! form.getMarital().equals(user.getW4Marital())) {
			user.setW4Marital(form.getMarital());
			valid = false;
		}
		if (form.getNameDiffers() != user.getW4NameDiffers()) {
			user.setW4NameDiffers(form.getNameDiffers());
			valid = false;
		}
		if (form.getAllowances() != null && form.getAllowances() != user.getW4Allowances()) {
			user.setW4Allowances(form.getAllowances());
		}
		if (form.getAddtlAmount() != null && ! form.getAddtlAmount().equals(user.getW4AddtlAmount())) {
			user.setW4AddtlAmount(form.getAddtlAmount());
			valid = false;
		}
		if (form.getExempt() != user.getW4Exempt()) {
			user.setW4Exempt(form.getExempt());
			valid = false;
		}

		if (user.getHomeAddress() == null) {
			user.setHomeAddress(new Address());
		}
		// To avoid LIE
		user.setHomeAddress(AddressDAO.getInstance().refresh(user.getHomeAddress()));

		if (form.getAddress() != null && (! form.getAddress().isEmpty()) &&
				(! form.getAddress().equalsAddress(user.getHomeAddress()))) {
			user.getHomeAddress().copyFrom(form.getAddress());
			valid = false;
		}

		setValidData(valid);
		return super.compareFormAccountDetails(user);
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())){
			isValid = issueErrorMessage("Last Name", false, "");
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
		//LS-1843 Validate city/state for W4
		isValid &= LocationUtils.checkCityState(form.getAddress());

		// LS-3729
		if(FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_W9_REMOVE_SUBMIT_VALIDATION)) {
			if (!form.getExempt() && StringUtils.isEmpty(form.getMarital())){
				isValid = issueErrorMessage("Employee Marital Status", false, "");
			}
		}
		else {
			if (StringUtils.isEmpty(form.getMarital())){
				isValid = issueErrorMessage("Employee Marital Status", false, "");
			}
		}
		if ((form.getAllowances() == null) && ! form.getExempt()) {
			if(form.getVersion() != FormW4.W4_VERSION_2020) {//LS-2768
				isValid = issueErrorMessage("Total Allowances", false, "");
			}
		}

		isValid &= isValidSsn(form.getSocialSecurity());

		setSubmitValid(isValid);

		return super.checkSubmitValid();
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		boolean is2018style = is2018style();

		isValid &= checkAddressValid(form.getAddress(), null);

		if (form.getVersion() != FormW4.W4_VERSION_2020) {
			Integer allowancesValue = form.getAllowances();
			if (allowancesValue != null && allowancesValue > 0) {
				form.setExempt(false);
			}
			if (allowancesValue != null && (allowancesValue < 0 || allowancesValue > 99)) {
				isValid = false;
				MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.Allowances", FacesMessage.SEVERITY_ERROR);
			}
			if (form.getPersonalDependents() != null && form.getPersonalDependents() < 0) {
				isValid = false;
				MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.NonNegative", FacesMessage.SEVERITY_ERROR, "D");
			}

			isValid &= checkZeroOne(form.getPersonalSelf(), "A");
			isValid &= checkZeroOne(form.getPersonalMarried(), "B");
			isValid &= checkZeroOne(form.getPersonalSpouse(), (is2018style ? "D" : "C"));
			isValid &= checkZeroOne(form.getPersonalHoH(), (is2018style ? "C" : "E"));
		}

		if (is2018style) {
			if (form.getPersonalCare() != null && form.getPersonalCare() < 0) { // used for "Other credits" in 2018
				isValid = false;
				MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.NonNegative", FacesMessage.SEVERITY_ERROR, "G");
			}
		}
		else {
			isValid &= checkZeroOne(form.getPersonalCare(), "F");
		}
		setSaveValid(isValid);

		return super.checkSaveValid();
	}

	/**
	 * @param personalSelf
	 * @param string
	 * @return
	 */
	private boolean checkZeroOne(Byte input, String field) {
		if (input == null || input == 0 || input == 1) {
			return true;
		}
		MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.ZeroOrOne", FacesMessage.SEVERITY_ERROR, field);
		return false;
	}

	@Override
	public void rowClicked(ContactDocument contactDc) {
		setContactDoc(contactDc);
		//initScrollPos(0);
		setUpForm();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		if (relatedFormId != null) {
			setForm(FormW4DAO.getInstance().findById(relatedFormId));
		}
		else {
			setForm(getBlankForm());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		if (form.getFirstName() == null) {
			FormW4DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
		if (form.getAddress() != null) {
			form.getAddress().getAddrLine1(); // Fix LIE switching between mini-tabs. r9127
		}
		if (is2018style()) {
			form.setSumDeductions(NumberUtils.safeAdd(form.getNetDeductions(), form.getAdjustments()));
		}

		// Set marital status radio buttons for W4 2020
		String marital = form.getMarital();
		if(!StringUtils.isEmpty(marital)) {
			resetBooleans();
			if(marital.equals(MARITAL_STATUS_SINGLE)) {
				maritalStatus[0] = true;
			}
			else if(marital.equals(MARITAL_STATUS_MARRIED)) {
				maritalStatus[1] = true;
			}
			else if(marital.equals(MARITAL_STATUS_HEAD_HOUSEHOLD)) {
				maritalStatus[2] = true;
			}
		}

		// LS-3360 Populate the first date of employment from the payroll start if it exists.
		// If there are more than one Start form for this person in the project,
		// select the first one. Only set the date if it is null or the date has been changed on the
		// Payroll Start and the W-4 is not in Approved or Submitted status.
		List<StartForm> startForms = getStartFormDAO().findByContactProject(contactDoc.getContact(), contactDoc.getProject(), true, "sd.effectiveStartDate ASC");
		if(!startForms.isEmpty()) {
			StartForm sf = startForms.get(0);
			Date workStartDate = sf.getWorkStartDate();
			Date firstDateEmp = form.getFirstDateEmployment();
			if((firstDateEmp ==  null || CalendarUtils.compare(workStartDate, firstDateEmp) != 0)
					&& (contactDoc.getStatus() != ApprovalStatus.APPROVED && contactDoc.getStatus() != ApprovalStatus.SUBMITTED
					&& contactDoc.getStatus() != ApprovalStatus.RESUBMITTED)) {
				form.setFirstDateEmployment(workStartDate);
			}
			else {
				form.setFirstDateEmployment(firstDateEmp);
			}
			FormW4DAO.getInstance().attachDirty(form);
		}

		/*if (form.getAddExempt() != null && ! form.getAddExempt().isEmpty()) {
			showExempt = true;
		}*/

	}

	@Override
	public void submitted() {
		if (getIsTeamPayroll()) {
			refreshForm();
			if (FF4JUtils.useFeature(FeatureFlagType.TTCO_ADDR_UNIF_USER_PROFILE)) {
				User user = contactDoc.getContact().getUser();
				// LS-3485 Update the address fields on the My Account tab so they
				// are in sync with the W-4 address values
				Address w4Address = form.getAddress();
				Address homeAddress = user.getHomeAddress() != null ? user.getHomeAddress() : new Address();
				Address mailingAddress = user.getMailingAddress() != null ? user.getMailingAddress() : new Address();
				Address originalHomeAddress = homeAddress.clone();
				Address originalMailingAddress = mailingAddress.clone();

				if (! w4Address.equalsAddress(homeAddress)) {
					// create ActivityLog instance to record address change
					ActivityLog log = new ActivityLog(ActivityLog.TYPE_PERSONAL_ADDRESS_CHANGE, new Date());
					log.setPermanentAddrChanged(true); // LS-4574
					if (user.getSameAsHomeAddr() || mailingAddress.isEmpty()) {
						log.setMailingAddrChanged(true);
					}
					log.setUserAcct(user.getAccountNumber());
					ActivityLogDAO.getInstance().save(log);
				}

				// Set to the address from the W-4
				homeAddress.copyFrom(w4Address);

				// If mailing address field is empty then update the mailing address
				// with the W-4 address and set the sameAsHomeAddr flag to true.
				if (mailingAddress.isEmpty()) {
					mailingAddress.copyFrom(w4Address);
					user.setSameAsHomeAddr(true);
				}
				else if(user.getSameAsHomeAddr()) {
					// If the sameAsHomeAddr flag is true, the
					// mailing address also needs to be updated
					// to be in sync with the home address.
					mailingAddress.copyFrom(w4Address);
				}

				// LS-3486 LS-3729 Display popup to let user know that field values
				// on My Account page have been updated.
				if (haveW4ValuesChanged(user, originalHomeAddress, originalMailingAddress)) {
					user.setHomeAddress(homeAddress);
					user.setMailingAddress(mailingAddress);
					updateUserW4Info(user);
					PopupBean popupBean = PopupBean.getInstance();
					popupBean.show(this, 0, "Form.MyAccount.Updated.Title", "Form.MyAccount.Updated.Text", "Confirm.OK", null);
				}
			}

			if (FF4JUtils.useFeature(FeatureFlagType.TTCO_W4_EXEMPT_DISABLE)) {
				// LS-3720 If exempt is true clear out non-exempt fields
				if (form.getExempt()) {
					clearNonExemptFields();
					FormW4DAO.getInstance().attachDirty(form);
				}
			}
		}
	}

	/**
	 * If values have changed on the W-4 update any associated User fields.
	 * LS-3729
	 *
	 * @param user
	 * @param origAddr
	 * @param origMailingAddr
	 * @return
	 */
	private boolean haveW4ValuesChanged(User user, Address origAddr, Address origMailingAddr) {
		// Check to see if the Resident address or the mailing address has changed
		if((!origAddr.equalsAddress(user.getHomeAddress()) || !origMailingAddr.equalsAddress(user.getMailingAddress()))) {
			return true;
		}
		else if(user.getSameAsHomeAddr() && !origMailingAddr.equalsAddress(user.getMailingAddress())) {
			return true;
		}
		else if(!form.getFirstName().equals(user.getFirstName()) || !form.getLastName().equals(user.getLastName())) {
			return true;
		}
		else if (StringUtils.compareNullEmpty(user.getMiddleName(), form.getMiddleInitial()) != 0) {
			return true; // middle initial changed (one or the other might be null). LS-4793
		}
		else if((form.getMarital() == null && user.getW4Marital() != null) // LS-3934 Fix for NPE
				|| (form.getMarital() != null && user.getW4Marital() == null)) {
			return true;
		}
		else if(form.getMarital() != null && user.getW4Marital() != null
				&& !form.getMarital().equals(user.getW4Marital())) {
			// LS-3957 Fix for leaving condition early and causing false negative.
			return true;
		}
		else if(form.getMultipleJobs() != user.getMultipleJobs()) {
			return true;
		}
		else if(NumberUtils.compare(form.getOtherDependencyAmt(), user.getOtherDependencyAmt()) != 0) {
			return true;
		}
		else if(NumberUtils.compare(form.getChildDependencyAmt(), user.getChildDependencyAmt()) != 0) {
			return true;
		}
		else if(NumberUtils.compare(form.getOtherIncomeAmt(), user.getOtherIncomeAmt()) != 0) {
			return true;
		}
		else if(NumberUtils.compare(form.getDeductionsAmt(), user.getDeductionsAmt()) != 0) {
			return true;
		}
		else if(NumberUtils.compare(form.getExtraWithholdingAmt(), user.getExtraWithholdingAmt()) != 0) {
			return true;
		}
		else if(form.getExempt() != user.getW4Exempt()) {
			return true;
		}
		else if(StringUtils.isEmpty(user.getSocialSecurity()) || !StringUtils.cleanTaxId(form.getSocialSecurity()).equals(StringUtils.cleanTaxId(user.getSocialSecurity()))) {
			return true;
		}
		return false;
	}

	public void listenValueChange(ValueChangeEvent event) {
		FormFieldType type = null;
		Integer netDeduc = null;
		Integer adjust = null;
		if (form != null) {
			netDeduc = form.getNetDeductions();
			adjust = form.getAdjustments();
		}
		try {
			String cid = event.getComponent().getId();
			int ix = cid.indexOf("_1");
			if (ix > 0) {
				cid = cid.substring(0, ix);
			}
			type = FormFieldType.valueOf(cid);
			if (FormFieldType.W4_ADJUSTMENTS == type && event.getNewValue() != null) {
				adjust = ((Long)event.getNewValue()).intValue();
			}
			LOG.debug("netdeduc "+netDeduc);
			LOG.debug("adjust "+adjust);
			if (is2018style() || (form.getSumDeductions() == null)) {
				// only set it if 2018+, or it hasn't been set before (pre-2018, user may override value)
				form.setSumDeductions(NumberUtils.safeAdd(netDeduc, adjust));
				LOG.debug("sum deduct "+form.getSumDeductions());
			}
		}
		catch (Exception e) {
			LOG.error("Error converting field id=" + event.getComponent().getId() + ": " + e);
//			EventUtils.logError(e);
		}
	}

	/**
	 * Set the correct radio button.
	 *
	 * @param event
	 */
	public void listenMaritalStatusChange(ValueChangeEvent event) {
		try {

			refreshRadioButtons(event);
			Boolean newVal = (Boolean) event.getNewValue();
			Integer index = null;
			if(newVal != null) {
				if (maritalStatus[0] && newVal) {
					form.setMarital("s");
					index = 0;
				}
				else if (maritalStatus[1] && newVal) {
					form.setMarital("s");
					index = 1;
				}
				else if (maritalStatus[2] && newVal) {
					form.setMarital("h");
					index = 2;
				}

				for (int i = 0; i < maritalStatus.length; i++) {
					if (index != null && i != index) {
						maritalStatus[i] = false;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Auto-fill the W4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			form.setFirstName(user.getFirstName());
			form.setMiddleInitial(user.getMiddleName()); // LS-4793
			form.setLastName(user.getLastName());
			form.setSocialSecurity(user.getSocialSecurity());
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (user.getHomeAddress() != null) {
				form.getAddress().copyFrom(user.getHomeAddress());
			}
			else {
				// LS-3555 Clear out the fields if the user has not yet gone to
				// the My Account page to fill in the home address
				form.getAddress().clear();
			}

			form.setMarital(user.getW4Marital());
			form.setNameDiffers(user.getW4NameDiffers());
			form.setAllowances(user.getW4Allowances());
			form.setAddtlAmount(user.getW4AddtlAmount());
			form.setExempt(user.getW4Exempt());
			// LS-3453 Updates to My Account > W4 Info > Multiple Jobs
			form.setMultipleJobs(user.getMultipleJobs());
			// LS-3453 Updates to My Account > W4 Info > Claim Dependents
			form.setChildDependencyAmt(user.getChildDependencyAmt());
			form.setOtherDependencyAmt(user.getOtherDependencyAmt());
			// LS-3529 Fixed null pointer exception by using NumberUtils.safeAdd
			form.setTotalDependencyAmt(NumberUtils.safeAdd(form.getChildDependencyAmt(), form.getOtherDependencyAmt()));
			// LS-3454 Updates to My Account > W4 Info > Other Adjustments
			form.setOtherIncomeAmt(user.getOtherIncomeAmt());
			form.setDeductionsAmt(user.getDeductionsAmt());
			form.setExtraWithholdingAmt(user.getExtraWithholdingAmt());
			// LS-3244 If there is a start form set First Date of Employment
			// If there are more than one Start form for this person in the project,
			// select the first one.
			List<StartForm> startForms = getStartFormDAO().findByContactProject(contactDoc.getContact(), contactDoc.getProject(), true, "sd.effectiveStartDate ASC");
			if(!startForms.isEmpty()) {
				StartForm sf = startForms.get(0);
				form.setFirstDateEmployment(sf.getWorkStartDate());
			}

			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(maritalStatus, Boolean.FALSE);
	}

	/**
	 * @return True iff this form is version 2018 or later.
	 */
	private boolean is2018style() {
		// assume (for now) that 2019 will follow 2018 layout/values...
		if (form != null) {
			return form.getVersion().byteValue() >= FormW4.W4_VERSION_2018;
		}
		return true;
	}

	/**See {@link #exemptList}. */
	public List<SelectItem> getExemptList() {
		if (exemptList == null) {
			exemptList = new ArrayList<>();
			exemptList.add(new SelectItem(false, "--"));
			exemptList.add(new SelectItem(true, "Exempt"));
		}
		return exemptList;
	}
	/**See {@link #exemptList}. */
	public void setExemptList(List<SelectItem> exemptList) {
		this.exemptList = exemptList;
	}

	/** See {@link #maritalStatus}. */
	public Boolean[] getMaritalStatus() {
		return maritalStatus;
	}

	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(Boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}


	/**
	 * Listener method for the calculate dependent amount.
	 */
	public void listenChildDependentAmtChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= ChildDependentAmount");
		Integer newValue = null;
		if (null == evt.getNewValue()) {
			form.setTotalDependencyAmt(form.getOtherDependencyAmt());
		}
		else {
			newValue = Integer.parseInt(evt.getNewValue().toString());
			form.setChildDependencyAmt(newValue);
			form.setTotalDependencyAmt(newValue);
			if (null != form.getOtherDependencyAmt()) {
				form.setTotalDependencyAmt(form.getChildDependencyAmt() + form.getOtherDependencyAmt());
			}
		}
	}

	/**
	 * Listener method for the calculate dependent amount.
	 */
	public void listenOtherDependentAmtChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= OtherDependentAmount");
		Integer newValue = null;
		if (null == evt.getNewValue()) {
				form.setTotalDependencyAmt(form.getChildDependencyAmt());
		}
		else {
			newValue = Integer.parseInt(evt.getNewValue().toString());
			form.setOtherDependencyAmt(newValue);
			form.setTotalDependencyAmt(newValue);
			if (null != form.getChildDependencyAmt()) {
				form.setTotalDependencyAmt(form.getChildDependencyAmt() + form.getOtherDependencyAmt());
			}
		}
	}

	/**
	 * Listener method for the calculate joblower value and set to total job count amount.
	 */
	public void listenWs3JobLowerChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= ws3JobLowerAmount");
		Integer newValue = null;
		if (null == evt.getNewValue()) {
			form.setThreeJobsHighLowTaxableWages(null);
			if (null != form.getThreeJobsHighTaxableWages()) {
				form.setThreeJobsTotalTaxableWages(form.getThreeJobsHighTaxableWages());
			}
		}
		else {
			newValue = Integer.parseInt(evt.getNewValue().toString());
			form.setThreeJobsHighLowTaxableWages(newValue);
			if (null != form.getThreeJobsHighLowTaxableWages()) {
				form.setThreeJobsTotalTaxableWages(form.getThreeJobsHighLowTaxableWages()
						+ ((form.getThreeJobsHighTaxableWages() != null) ? form.getThreeJobsHighTaxableWages() : 0));
			}
		}
		form.setDeductionsTotal(((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0)
				+ ((form.getDeductionsSubTotal() != null) ? form.getDeductionsSubTotal() : 0));
	}

	/**
	 * Listener method for the calculate jobhigher value and set to total job count amount.
	 */
	public void listenWs3JobHigherChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= ws3JobHigherAmount");
		Integer newValue = null;
		if (null == evt.getNewValue()) {
			form.setThreeJobsHighTaxableWages(null);
			if (null != form.getThreeJobsHighLowTaxableWages()) {
				form.setThreeJobsTotalTaxableWages(
						(form.getThreeJobsHighLowTaxableWages() != null) ? form.getThreeJobsHighLowTaxableWages() : 0);
				form.setDeductionsTotal(((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0)
						+ ((form.getDeductionsSubTotal() != null) ? form.getDeductionsSubTotal() : 0));
			}
		}
		else {
			newValue = Integer.parseInt(evt.getNewValue().toString());
			form.setThreeJobsHighTaxableWages(newValue);
			if (null != form.getThreeJobsHighLowTaxableWages()) {
				form.setThreeJobsTotalTaxableWages(form.getThreeJobsHighTaxableWages()
						+ ((form.getThreeJobsHighLowTaxableWages() != null) ? form.getThreeJobsHighLowTaxableWages()
								: 0));
			}
			form.setDeductionsTotal(((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0)
					+ ((form.getDeductionsSubTotal() != null) ? form.getDeductionsSubTotal() : 0));
		}
	}

	/**
	 * Listener method for the calculate deduction estimate value based on below rule.
	 * [IF line 1 is greater than line 2, then line 3 = line 2- line 1] . IF line 1 is smaller than line 2, then line 3 = 0]
	 */
	public void listenWsDeductEstChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= wsDeductEstimate");
		if (null == evt.getNewValue()) {
			form.setDeductionsSubTotal(0);
			form.setDeductionsTotal(((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
		}
		else{

			form.setEstItemizedDeductions(Integer.parseInt(evt.getNewValue().toString()));
			form.setDeductionsSubTotal(Integer.parseInt(evt.getNewValue().toString()));
			if (null != form.getFilingStatusAmt()) {
				if (form.getEstItemizedDeductions() > form.getFilingStatusAmt()) {
					form.setDeductionsSubTotal(form.getEstItemizedDeductions() - form.getFilingStatusAmt());
					form.setDeductionsTotal(form.getDeductionsSubTotal()
							+ ((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
				}
				else {
					form.setDeductionsSubTotal(0);
					form.setDeductionsTotal(
							((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
				}
			}
		}

	}

	/**
	 * Listener method for the calculate deduction Filing type value based on below rule.
	 * [IF line 1 is greater than line 2, then line 3 = line 2- line 1] . IF line 1 is smaller than line 2, then line 3 = 0]
	 */
	public void listenFilingStatusAmtChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= WsDeductFilingType");
		if (null == evt.getNewValue()) {
			form.setFilingStatusAmt(null);
			form.setDeductionsSubTotal(
					((form.getEstItemizedDeductions() != null) ? form.getEstItemizedDeductions() : 0));
			form.setDeductionsTotal(form.getDeductionsSubTotal()
					+ ((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
		}
		else {
			form.setDeductionsSubTotal(0);
			form.setFilingStatusAmt(Integer.parseInt(evt.getNewValue().toString()));
			if (null != form.getEstItemizedDeductions()) {
				if (form.getEstItemizedDeductions() > form.getFilingStatusAmt()) {
					form.setDeductionsSubTotal(form.getEstItemizedDeductions() - form.getFilingStatusAmt());
					form.setDeductionsTotal(form.getDeductionsSubTotal()
							+ ((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
				} else {
					form.setDeductionsSubTotal(0);
					form.setDeductionsTotal(
							((form.getOtherDeductibleAmts() != null) ? form.getOtherDeductibleAmts() : 0));
				}
			}
		}

	}

	/**
	 * Listener method for the calculate total deduction Filing type value based on below rule.
	 * total deduction is equal to deduction sub total and deduction from other estimate;
	 */
	public void listenWsDeductOtherEstChange(ValueChangeEvent evt) {
		LOG.debug("Listener, action= WsDeductOtherEstimate");
		if (null == evt.getNewValue()) {
			form.setOtherDeductibleAmts(null);
			form.setDeductionsTotal(((form.getDeductionsSubTotal() != null) ? form.getDeductionsSubTotal() : 0));
		}
		else {
			form.setOtherDeductibleAmts(Integer.parseInt(evt.getNewValue().toString()));
			form.setDeductionsTotal(form.getOtherDeductibleAmts()
					+ ((form.getDeductionsSubTotal() != null) ? form.getDeductionsSubTotal() : 0));
		}
	}

	/**
	 * method to update w4 information on my account page when the w4 form is
	 * submitted. LS-3449
	 */
	public void updateUserW4Info(User user) {
		user = getUserDAO().refresh(user);
		user.setW4Exempt(form.getExempt());
		if (! form.getExempt()) {
			user.setW4Marital(form.getMarital());
			user.setMultipleJobs(form.getMultipleJobs());
			user.setChildDependencyAmt(form.getChildDependencyAmt());
			user.setOtherDependencyAmt(form.getOtherDependencyAmt());
			user.setOtherIncomeAmt(form.getOtherIncomeAmt());
			user.setDeductionsAmt(form.getDeductionsAmt());
			user.setExtraWithholdingAmt(form.getExtraWithholdingAmt());
			user.setFirstName(form.getFirstName());
			user.setMiddleName(form.getMiddleInitial());
			user.setLastName(form.getLastName());
		}
		getUserDAO().attachDirty(user);
	}

	/**
	 * If Exempt is selected, the Non-Exempt fields should be set to null.
	 * LS-3720
	 */
	public void clearNonExemptFields() {
		form.setMarital(null);
		form.setChildDependencyAmt(null);
		form.setOtherDependencyAmt(null);
		form.setTotalDependencyAmt(null);
		form.setOtherIncomeAmt(null);
		form.setDeductionsAmt(null);
		form.setExtraWithholdingAmt(null);
		form.setMultipleJobs(false);
	}

	/** Listener invoked by ZIP code change which calls parent's method for
	 * further handling
	 * @param event
	 */
	public void listenZipCode(ValueChangeEvent event) {
		super.listenZipCode(event, form.getAddress());
	}
}
