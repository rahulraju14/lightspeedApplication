/**
 * File: FormStateHIW4Bean.java
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
 * Managed bean for handling HI State W-4 functionality
 */
@ManagedBean
@ViewScoped
public class FormStateHIW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = - 6235327712239741688L;
	private static final Log LOG = LogFactory.getLog(FormStateHIW4Bean.class);

	/** Marital Status whether filing single (s), married(m) or married separate (j) married single withholding(w) */
	private Boolean[] maritalStatus = new Boolean[5];

	public static FormStateHIW4Bean getInstance() {
		return (FormStateHIW4Bean)ServiceFinder.findBean("formStateHIW4Bean");
	}

	/**
	 * default constructor
	 */
	public FormStateHIW4Bean() {
		super("FormStateHIW4Bean");
		resetBooleans();

	}

	@Override
	public String actionCancel() {
		super.actionCancel();

		setUpForm();

		return null;
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
			form.setMarital(MARITAL_STATUS_EXTRA_WITHOLDINGS);
			index = 2;
		}
		else if (maritalStatus[3]) {
			form.setMarital(MARITAL_DISABLED);
			index = 3;		
		}
		else if (maritalStatus[4]) {
			form.setMarital(MARITAL_MILITARY_SPOUSE);
			index = 4;
		}
		
		return index;
	}
	
	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);

		Address addr = form.getAddress();
		if(addr != null) {
			addr.setAddrLine1(addr.getAddrLine1Line2());
			// 4209 Using addrLine2 because city field is not large enough
			// to hold 90 characters
			addr.setAddrLine2(addr.getCityStateZip());
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		// Set marital status radio buttons for W4 2020
		String marital = form.getMarital();
		resetBooleans();
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
			else if(marital.equals(MARITAL_DISABLED)) {
				maritalStatus[3] = true;
			}
			else if (marital.equals(MARITAL_MILITARY_SPOUSE)) {
				maritalStatus[4] = true;
			}
		}
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
		if(StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		
		if(!checkNegative(form.getAllowances(), "4")) {
			isValid = false;
		}
		if(!checkNegative(form.getAdditionalAmount(), "5")) {
			isValid = false;
		}
	
		setSubmitValid(isValid);
		return isValid;
	}
	
	/**
	 * LS-4209
	 * Method used to check whether the address line 1 or city fields are empty or 
	 * not in the form on action submit. This will issue error messages for any 
	 * required fields that are blank.
	 *
	 * @param address The Address object whose fields are to be validated.
	 *
	 * @return True if all the fields are valid to submit the form.
	 */
	protected boolean checkAddressValidMsg(Address address) {
		boolean isValid = true;
		
		if(address == null) {
			return false;
		}
		
		if(StringUtils.isEmpty(address.getAddrLine1()) ) {
			isValid = issueErrorMessage("Address", false, "");
		}

		if(StringUtils.isEmpty(address.getAddrLine2())) {
			isValid = issueErrorMessage("City, state and zip code", false, "");
		}
		
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
