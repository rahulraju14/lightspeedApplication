/**
 * File: FormMDW4Bean.java
 */
package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * LS-3764
 * 
 * Managed bean for adding MD W-4 specific functionality
 * @author GGeller
 */
@ManagedBean
@ViewScoped
public class FormStateMDW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = - 1103254744019253923L;
	private static final Log LOG = LogFactory.getLog(FormStateMDW4Bean.class);

	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
	private static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];

	/**
	 * default constructor
	 */
	public FormStateMDW4Bean() {
		super("FormStateMDW4Bean");
		resetBooleans();

	}

	public static FormStateMDW4Bean getInstance() {
		return (FormStateMDW4Bean)ServiceFinder.findBean("formStateMDW4Bean");
	}
	
	@Override
	/**
	 * LS-3997
	 * There is not a specific zip code field to validate so
	 * return true;
	 */
	protected boolean isZipValid() {
		return true;
	}
	
	public String actionCancel() {
		super.actionCancel();
		
		setUpForm();
		
		return null;
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
	
	@Override
	/**
	 * LS-3997
	 * MD does not have a specific Address zip field to check so
	 * check is fullAddress is empty.
	 */
	public boolean checkAddressValidMsg(Address address) {
		return !StringUtils.isEmpty(form.getCompleteAddress());
	}
	
	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();

		setSaveValid(isValid);
		return isValid;
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();

		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		
		// LS-4114 validate address and county fields
		if (StringUtils.isEmpty(form.getCompleteAddress())) {
			isValid = issueErrorMessage("Address", false, "");
		}
		
		if (form.getAddress() == null || StringUtils.isEmpty(form.getAddress().getCounty())) {
			isValid = issueErrorMessage("County of residence", false, "");
		}

		setSubmitValid(isValid);
		return isValid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		
		if(form.getAddress() != null) {
			form.setCompleteAddress(form.getAddress().getCompleteAddress());
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		
		// LS-4186 Clear out the marital radio buttons first then reset them based on the selected marital status.
		// If marital is null, then none of the radio buttons should be selected. 
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
}
