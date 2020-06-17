package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

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

/**
 * Backing bean for the MO-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateMOW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateMOW4Bean.class);
	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
	private static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";

	private static final String EXEMPT_STATUS1 = "m";
	private static final String EXEMPT_STATUS2 = "s";
	private static final String EXEMPT_STATUS3 = "i";

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];

	/** Exempt Status whether filing Exempt1 (m), Exempt2 (s) or Exempt3 (i) */
	private Boolean[] exemptStatus = new Boolean[3];


	/**
	 * default constructor
	 */
	public FormStateMOW4Bean() {
		super("FormStateMOW4Bean");
		resetBooleans();

	}

	public static FormStateMOW4Bean getInstance() {
		return (FormStateMOW4Bean)ServiceFinder.findBean("formStateMOW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (form.getExempt() && StringUtils.isEmpty(form.getExemptReason())) {
			isValid = issueErrorMessage("Exempt Reason", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
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
		// Set marital status radio buttons for OR W4 2020
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
		}

		String exempt = form.getExemptReason();
		if (! StringUtils.isEmpty(exempt)) {

			if (exempt.equals(EXEMPT_STATUS1)) {
				exemptStatus[0] = true;
			}
			else if (exempt.equals(EXEMPT_STATUS2)) {
				exemptStatus[1] = true;
			}
			else if (exempt.equals(EXEMPT_STATUS3)) {
				exemptStatus[2] = true;
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
	 * Set the correct radio button.
	 *
	 * @param event
	 */
	public void listenExemptStatusChange(ValueChangeEvent event) {
		try {
			refreshRadioButtons(event);
			Boolean newVal = (Boolean)event.getNewValue();
			Integer index = null;
			if (newVal != null) {
				if (exemptStatus[0] && newVal) {
					form.setExemptReason("m");
					index = 0;
				}
				else if (exemptStatus[1] && newVal) {
					form.setExemptReason("s");
					index = 1;
				}
				else if (exemptStatus[2] && newVal) {
					form.setExemptReason("i");
					index = 2;
				}

				for (int i = 0; i < exemptStatus.length; i++) {
					if (index != null && i != index) {
						exemptStatus[i] = false;
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
		Arrays.fill(exemptStatus, Boolean.FALSE);
	}

	/** See {@link #maritalStatus}. */
	public Boolean[] getMaritalStatus() {
		return maritalStatus;
	}

	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(Boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	/** See {@link #exemptStatus}. */
	public Boolean[] getExemptStatus() {
		return exemptStatus;
	}

	/** See {@link #exemptStatus}. */
	public void setExemptStatus(Boolean[] exemptStatus) {
		this.exemptStatus = exemptStatus;
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

}
