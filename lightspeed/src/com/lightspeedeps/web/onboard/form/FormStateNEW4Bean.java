package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the NE-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateNEW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7778545119082625250L;
	private static final Log LOG = LogFactory.getLog(FormStateNEW4Bean.class);

	/** Marital Status whether filing single (s), married(m) */
	private Boolean[] maritalStatus = new Boolean[2];

	/**
	 * default constructor
	 */
	public FormStateNEW4Bean() {
		super("FormStateNEW4Bean");
		resetBooleans();
	}

	public static FormStateNEW4Bean getInstance() {
		return (FormStateNEW4Bean)ServiceFinder.findBean("formStateNEW4Bean");
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
		if (Form.isEmptyNA(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (StringUtils.isEmpty(form.getFirstAndInitial())) {
			isValid = issueErrorMessage("First Name and Initial", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * Auto-fill the IA-W4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			super.autoFillForm(prompted);
			populateForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
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
		// Set marital status radio buttons for IA W4 2020
		String marital = form.getMarital();
		if (! StringUtils.isEmpty(marital)) {
			if (marital.equals(MARITAL_STATUS_SINGLE)) {
				maritalStatus[0] = true;
			}
			else if (marital.equals(MARITAL_STATUS_MARRIED)) {
				maritalStatus[1] = true;
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
					form.setMarital(MARITAL_STATUS_SINGLE);
					index = 0;
				}
				else if (maritalStatus[1] && newVal) {
					form.setMarital(MARITAL_STATUS_MARRIED);
					index = 1;
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

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

}
