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
 * Backing bean for the WI-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateWIW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateWIW4Bean.class);
	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
	private static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];


	/**
	 * default constructor
	 */
	public FormStateWIW4Bean() {
		super("FormStateWIW4Bean");
		resetBooleans();

	}

	public static FormStateWIW4Bean getInstance() {
		return (FormStateWIW4Bean)ServiceFinder.findBean("formStateWIW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (form.getDateofBirth() == null) {
			isValid = issueErrorMessage("Date Of Birth", false, "");
		}
		if (form.getDateofHire() == null) {
			isValid = issueErrorMessage("Date Of Hire", false, "");
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
		if (! StringUtils.isEmpty(marital)) {
			resetBooleans();
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
	 * Listener for Personal Exemptions value changes
	 *
	 * @param event
	 */
	public void listenPersonalExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setPersonalExemptions(null);
		}
		if (isAllowToAddExemptions()) {
			if (event.getNewValue() != null) {
				newValue = (Long)event.getNewValue();
				if (form.getSpouseExemptions() != null) {
					newValue = newValue + form.getSpouseExemptions();
				}
				if (form.getClaimedExemptions() != null) {
					newValue = newValue + form.getClaimedExemptions();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		}
	}

	/**
	 * Listener for Spouse Exemptions value changes
	 *
	 * @param event
	 */
	public void listenSpouseExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setSpouseExemptions(null);
		}
		if (isAllowToAddExemptions()) {
			if (event.getNewValue() != null) {
				newValue = (Long)event.getNewValue();
				if (form.getPersonalExemptions() != null) {
					newValue = newValue + form.getPersonalExemptions();
				}
				if (form.getClaimedExemptions() != null) {
					newValue = newValue + form.getClaimedExemptions();
				}
				form.setTotalExemptions(newValue.intValue());
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				form.setTotalExemptions(oldValue);
			}
		}
	}

	/**
	 * Listener for Claimed Exemptions value changes
	 *
	 * @param event
	 */
	public void listenClaimedExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setClaimedExemptions(null);
		}
		if (isAllowToAddExemptions()) {
			if (event.getNewValue() != null) {
				newValue = (Long)event.getNewValue();
				if (form.getPersonalExemptions() != null) {
					newValue = newValue + form.getPersonalExemptions();
				}
				if (form.getSpouseExemptions() != null) {
					newValue = newValue + form.getSpouseExemptions();
				}
				form.setTotalExemptions(newValue.intValue());
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				form.setTotalExemptions(oldValue);
			}
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

	public boolean isAllowToAddExemptions() {
		if (form.getPersonalExemptions() == null && form.getSpouseExemptions() == null &&
				form.getClaimedExemptions() == null) {
			form.setTotalExemptions(null);
			return false;
		}
		return true;
	}

	@Override
	public String actionCancel() {
		setUpForm();
		return super.actionCancel();
	}

}
