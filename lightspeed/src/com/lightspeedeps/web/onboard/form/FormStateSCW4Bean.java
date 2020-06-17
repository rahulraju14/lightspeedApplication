package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.*;

import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the SC-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateSCW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateSCW4Bean.class);

	/**
	 * Marital Status whether filing single (s), married(m) or HouseHold(w) or
	 * Married/Seperated(p)
	 */
	private Boolean[] maritalStatus = new Boolean[3];
	/**
	 * default constructor
	 */
	public FormStateSCW4Bean() {
		super("FormStateSCW4Bean");
		resetBooleans();
	}

	public static FormStateSCW4Bean getInstance() {
		return (FormStateSCW4Bean) ServiceFinder.findBean("formStateSCW4Bean");
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

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		updateMaritalStatus();
		if (StringUtils.isEmpty(form.getFirstAndInitial())) {
			isValid = issueErrorMessage("First Name and Middle Initial", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (Form.isEmptyNA(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if(form.getExempt() && !form.isCheckBox2() && !form.isCheckBox3()) {
			isValid = issueErrorMessage("Checkboxes of #7", false, "");
		}
		if(form.isCheckBox3() && StringUtils.isEmpty(form.getLegalStateOfRes())) {
			isValid = issueErrorMessage("State of Domicile", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
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
			// to hold 75 characters
			addr.setAddrLine2(addr.getCityStateZip());
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		resetBooleans();
		// Set marital status radio buttons for SC W4 2020
		String marital = form.getMarital();
		if (!StringUtils.isEmpty(marital)) {
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
	 * /** Update the current marital status
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

		return index;
	}

	/**
	 * LS-4209 Method used to check whether the address line 1 or city fields
	 * are empty or not in the form on action submit. This will issue error
	 * messages for any required fields that are blank.
	 *
	 * @param address The Address object whose fields are to be validated.
	 *
	 * @return True if all the fields are valid to submit the form.
	 */
	@Override
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

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

	/**
	 * listener for checkbox value change
	 *
	 * @param evt
	 */
	public void listenResidenceChange(ValueChangeEvent evt) {
		boolean value = (boolean)evt.getNewValue();
		if (! value) {
			form.setLegalStateOfRes(null);
		}
	}

	/**
	 * listener for Exempt Dropdown value change
	 *
	 * @param evt
	 */
	public void listenExemptChange(ValueChangeEvent evt) {
		boolean value = (boolean)evt.getNewValue();
		if (! value) {
			form.setCheckBox2(false);
			form.setCheckBox3(false);
			form.setLegalStateOfRes(null);
		}
	}

}
