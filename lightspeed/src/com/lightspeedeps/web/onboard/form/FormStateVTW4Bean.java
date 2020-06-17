/**
 * File: FormStateVTW4Bean.java
 */
package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * 
 */
@ManagedBean
@ViewScoped
public class FormStateVTW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 5862731599702114928L;
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormStateVTW4Bean.class);

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[4];

	public static FormStateVTW4Bean getInstance() {
		return (FormStateVTW4Bean)ServiceFinder.findBean("formStateVTW4Bean");
	}
	
	public FormStateVTW4Bean() {
		super("FormStateVTW4Bean");
		resetBooleans();
	}
	
	/**
	 * Map the marital buttons to the proper constant value, then continue with
	 * normal save.
	 */
	@Override
	public String actionSave() {
		updateMaritalStatus();
		return super.actionSave();
	}
	
	public String actionCancel() {
		super.actionCancel();
		
		setUpForm();
		
		return null;
	}
	
	/**
	 * Update the current marital status
	 * 
	 * @return index of the marital status that was updated.
	 */
	private int updateMaritalStatus() {
		int index = -1;
		
		form.setMarital(null);
		if (maritalStatus[0]) {
			form.setMarital(MARITAL_STATUS_SINGLE);
			index = 0;
		}
		else if (maritalStatus[1]) {
			form.setMarital(MARITAL_STATUS_MARRIED);
			index = 1;
		}
		else if (maritalStatus[2]) {
			form.setMarital(MARITAL_STATUS_MARRIED_SEPARATE);
			index = 2;
		}
		else if (maritalStatus[3]) {
			form.setMarital(MARITAL_STATUS_EXTRA_WITHOLDINGS);
			index = 3;		
		}
		
		return index;
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
			else if (marital.equals(MARITAL_STATUS_MARRIED_SEPARATE)) {
				maritalStatus[2] = true;
			}
			else if (marital.equals(MARITAL_STATUS_EXTRA_WITHOLDINGS)) {
				maritalStatus[3] = true;
			}		
		}
	}

	public void listenCalcTotalAllowances(ValueChangeEvent event) {
		Integer value = NumberUtils.safeAdd(form.getPersonalExemptions(), form.getSpouseExemptions(),
				form.getQualifiedDependents(), form.getHeadOfHouseholdExemptions());
		form.setTotalExemptions(value);
	}
	
	boolean isValid = true;
	
	/**
	 * LS-4187
	 * 
	 * return true since we are not validating the zip code.
	 */
	@Override
	protected boolean isZipValid() {
		return true;
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();
		// LS-4187 Do not allow negative numbers and numbers greater than 1.
		if(!checkZeroOne(form.getPersonalExemptions(), "1")) {
			isValid = false;
		}
		if(!checkZeroOne(form.getSpouseExemptions(), "2")) {
			isValid = false;
		}
		if(form.getQualifiedDependents() != null && Integer.signum(form.getQualifiedDependents()) == -1) {
			MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.NonNegative", FacesMessage.SEVERITY_ERROR, "3");	
			isValid = false;
		}
		if(!checkZeroOne(form.getHeadOfHouseholdExemptions(), "4")) {
			isValid = false;
		}
		if(form.getAdditionalAmount() != null && Integer.signum(form.getAdditionalAmount()) == -1) {
			MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage.NonNegative", FacesMessage.SEVERITY_ERROR, "6");	
			isValid = false;
		}
	
		setSubmitValid(isValid);
		return isValid;
	}
	
	/**
	 * Method used to check the validity of fields in the form.
	 *
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		
		// Update the marital status
		updateMaritalStatus();
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last NameName", false, "");	
		}
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		// LS-4188
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		
		setSubmitValid(isValid);
		return isValid;
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
