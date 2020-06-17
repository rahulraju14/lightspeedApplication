package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the CA-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateCAW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateCAW4Bean.class);

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];

	// Exempt popup action
		private static final int ACT_EXEMPT_SELECTED = 10;

	/**
	 * default constructor
	 */
	public FormStateCAW4Bean() {
		super("FormStateCAW4Bean");
		resetBooleans();

	}

	public static FormStateCAW4Bean getInstance() {
		return (FormStateCAW4Bean)ServiceFinder.findBean("formStateCAW4Bean");
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();
		if (form.getAddress() != null) {
			// Zip code validation for 5 digits
			if (! form.getAddress().isZipValid()) {
				MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
				isValid = false;
			}
		}
		if(!checkNegative(form.getAllowances(), "1")) {
			isValid = false;
		}
		if(!checkNegative(form.getAdditionalAmount(), "2")) {
			isValid = false;
		}

		setSaveValid(isValid);
		return isValid;
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		//LS-4476 check for Exempt and allowances field to show popup
		if ((form.getExempt() || form.isCertifiedForPenalty()) &&
				(form.getAllowances() != null || form.getAdditionalAmount() != null)) {
			PopupBean popupBean = PopupBean.getInstance();
			popupBean.setMessage(MsgUtils.getMessage("FormCAW4Bean.Exempt.Message"));
			popupBean.show(this, ACT_EXEMPT_SELECTED, "FormCAW4Bean.Exempt.Title",
					"FormCAW4Bean.Exempt.ok", "FormCAW4Bean.Exempt.cancel");
			return false;
		}
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if ((form.getAllowances() == null) && ! form.getExempt() &&
				! form.isCertifiedForPenalty()) {
			isValid = issueErrorMessage("Allowances", false, "");
		}

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		// ... add any non-standard fields here
		User user = getContactDoc().getContact().getUser();
		user = UserDAO.getInstance().refresh(user);
		if (user.getHomeAddress() != null) {
			form.getAddress().setAddrLine2(null);
			form.getAddress().setAddrLine1(user.getHomeAddress().getAddrLine1Line2());
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		resetBooleans();
		// Set marital status radio buttons for W4 2020
		String marital = form.getMarital();
		if (! StringUtils.isEmpty(marital)) {
			if (marital.equals(MARITAL_STATUS_SINGLE)) {
				maritalStatus[0] = true;
			}
			else if (marital.equals(MARITAL_STATUS_MARRIED)) {
				maritalStatus[1] = true;
			}
			else if (marital.equals(MARITAL_STATUS_EXTRA_WITHOLDINGS)) {
				maritalStatus[2] = true;
			}
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
			Boolean newVal = (Boolean)event.getNewValue();
			Integer index = null;
			if (newVal != null) {
				if (maritalStatus[0] && newVal) {
					form.setMarital("s");
					index = 0;
				}
				else if (maritalStatus[1] && newVal) {
					form.setMarital("m");
					index = 1;
				}
				else if (maritalStatus[2] && newVal) {
					form.setMarital("w");
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
	 * clear Additional amount on change.
	 *to additionalRadio
	 * @param event
	 */
	public void listenAdditionalRadioChange(ValueChangeEvent event) {
		try {
			form.setAdditionalAmount(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(maritalStatus, Boolean.FALSE);
	}

	/** See {@link #maritalStatus}. */
	public Boolean[] getMaritalStatus() {
		return maritalStatus;
	}

	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(Boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

	/*
	 * @see com.lightspeedeps.web.view.View#confirmOk(int)
	 *  used to make clear fields if holds values
	 * @param action Exempt selection YES On popup LS-4476
	 */
	@Override
	public String confirmOk(int action) {
		if (action == ACT_EXEMPT_SELECTED) {
			form.setAllowances(null);
			form.setAdditionalAmount(null);
			form.setExemptStatus1(true);
			if (! ContactFormBean.getInstance().getEditMode() && ! getEmpEdit()) {
				actionSave();
			}
			ContactFormBean.getInstance().actionSubmit();

		}
		return null;
	}

	/**
	 *Exempt set exemptStatus for enabled / disabled..
	 *LS-4476
	 * @param event
	 */
	public void listenExemptChange(ValueChangeEvent event) {
		boolean newVal = (boolean)event.getNewValue();
		if(!newVal && !form.isCertifiedForPenalty()) {
			form.setExemptStatus1(false);
		}

	}

	/**
	 * CertifiedForPenalty set exemptStatus for enabled / disabled.
	 *LS-4476
	 * @param event
	 */
	public void listenCertifiedForPenaltyChange(ValueChangeEvent event) {
		boolean newVal = (boolean)event.getNewValue();
		if (! newVal && ! form.getExempt()) {
			form.setExemptStatus1(false);
		}
	}

}
