package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Form;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the MS-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateMSW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateMSW4Bean.class);

	/**
	 * Marital Status whether filing single (s), married(m) or HouseHold(w) or
	 * Married/Seperated(p)
	 */
	private Boolean[] maritalStatus = new Boolean[4];

	/**
	 * default constructor
	 */
	public FormStateMSW4Bean() {
		super("FormStateMSW4Bean");
		resetBooleans();

	}

	public static FormStateMSW4Bean getInstance() {
		return (FormStateMSW4Bean) ServiceFinder.findBean("formStateMSW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (Form.isEmptyNA(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	@Override
	public boolean checkSaveValid() {

		boolean isValid = super.checkSaveValid();

		if (form.getExemptStatus1() && !checkNegative(form.getPersonalExemptions(), "1")) {
			isValid = false;
		}
		if (form.getExemptStatus2() && !checkNegative(form.getSpouseExemptions(), "2")) {
			isValid = false;
		}
		if (form.getExemptStatus3() && !checkNegative(form.getClaimedExemptions(), "2")) {
			isValid = false;
		}
		if (form.getExemptStatus4() && !checkNegative(form.getHeadOfHouseholdExemptions(), "3")) {
			isValid = false;
		}
		if (!checkNegative(form.getClaiming(), "4")) {
			isValid = false;
		}
		if (!checkNegative(form.getQualifiedDependents(), "4")) {
			isValid = false;
		}
		if (!checkNegative(form.getBlindnessExemption(), "5")) {
			isValid = false;
		}
		
		if (!checkNegative(form.getAdditionalAmount(), "7")) {
			isValid = false;
		}
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
		resetBooleans();
		// Set marital status radio buttons for MS W4 2020
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
			else if (marital.equals(MARITAL_STATUS_MARRIED_SEPARATE)) {
				maritalStatus[3] = true;
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
			Boolean newVal = (Boolean) event.getNewValue();
			Integer index = null;
			if (newVal != null) {
				if (maritalStatus[0] && newVal) {
					form.setMarital(MARITAL_STATUS_SINGLE);
					index = 0;
					form.setExemptStatus1(true);
					form.setExemptStatus2(false);
					form.setExemptStatus3(false);
					form.setExemptStatus4(false);
					form.setSpouseExemptions(null);
					form.setClaimedExemptions(null);
					form.setHeadOfHouseholdExemptions(null);

				} 
				else if (maritalStatus[1] && newVal) {
					form.setMarital(MARITAL_STATUS_MARRIED);
					index = 1;
					form.setExemptStatus1(false);
					form.setExemptStatus2(true);
					form.setExemptStatus3(false);
					form.setExemptStatus4(false);
					form.setPersonalExemptions(null);
					form.setClaimedExemptions(null);
					form.setHeadOfHouseholdExemptions(null);
				} 
				else if (maritalStatus[2] && newVal) {
					form.setMarital(MARITAL_STATUS_EXTRA_WITHOLDINGS);
					index = 2;
					form.setExemptStatus1(false);
					form.setExemptStatus2(false);
					form.setExemptStatus3(true);
					form.setExemptStatus4(false);
					form.setPersonalExemptions(null);
					form.setSpouseExemptions(null);
					form.setHeadOfHouseholdExemptions(null);
				} 
				else if (maritalStatus[3] && newVal) {
					form.setMarital(MARITAL_STATUS_MARRIED_SEPARATE);
					index = 3;
					form.setExemptStatus1(false);
					form.setExemptStatus2(false);
					form.setExemptStatus3(false);
					form.setExemptStatus4(true);
					form.setPersonalExemptions(null);
					form.setSpouseExemptions(null);
					form.setClaimedExemptions(null);
				} 
				else {
					form.setTotalExemptions(null);
				}

				for (int i = 0; i < maritalStatus.length; i++) {
					if (index != null && i != index) {
						maritalStatus[i] = false;
					}
				}
			}
		} catch (Exception e) {
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

	/**
	 * Listener for Personal Exemptions value changes
	 *
	 * @param event
	 */
	public void listenPersonalExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setPersonalExemptions(null);
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		} 
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (form.getBlindnessExemption() != null) {
					newValue = newValue + form.getBlindnessExemption();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
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
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		}
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (form.getBlindnessExemption() != null) {
					newValue = newValue + form.getBlindnessExemption();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
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
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		} 
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (form.getBlindnessExemption() != null) {
					newValue = newValue + form.getBlindnessExemption();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
			}
		}
	}

	/**
	 * Listener for Head of Household Exemptions value changes
	 *
	 * @param event
	 */
	public void listenHeadHouseExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setHeadOfHouseholdExemptions(null);
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		} 
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (form.getBlindnessExemption() != null) {
					newValue = newValue + form.getBlindnessExemption();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
			}
		}
	}

	/**
	 * Listener for Qualified Dependent Exemptions value changes
	 *
	 * @param event
	 */
	public void listenDependentsExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setQualifiedDependents(null);
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		} 
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getPersonalExemptions() != null) {
					newValue = newValue + form.getPersonalExemptions();
				}
				if (form.getSpouseExemptions() != null) {
					newValue = newValue + form.getSpouseExemptions();
				}
				if (form.getClaimedExemptions() != null) {
					newValue = newValue + form.getClaimedExemptions();
				}
				if (form.getHeadOfHouseholdExemptions() != null) {
					newValue = newValue + form.getHeadOfHouseholdExemptions();
				}
				if (form.getBlindnessExemption() != null) {
					newValue = newValue + form.getBlindnessExemption();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
			}
		}
	}

	/**
	 * Listener for Blindness Exemptions value changes
	 *
	 * @param event
	 */
	public void listenBlindnessExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setBlindnessExemption(null);
			if (event.getOldValue() != null && autoCalcTotalExempt()) {
				Integer oldValue = (Integer) event.getOldValue();
				if (form.getTotalExemptions() != null) {
					oldValue = form.getTotalExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setTotalExemptions(oldValue);
				}
			}
		} 
		else {
			if (event.getNewValue() != null) {
				newValue = (Long) event.getNewValue();
				if (form.getPersonalExemptions() != null) {
					newValue = newValue + form.getPersonalExemptions();
				}
				if (form.getSpouseExemptions() != null) {
					newValue = newValue + form.getSpouseExemptions();
				}
				if (form.getClaimedExemptions() != null) {
					newValue = newValue + form.getClaimedExemptions();
				}
				if (form.getHeadOfHouseholdExemptions() != null) {
					newValue = newValue + form.getHeadOfHouseholdExemptions();
				}
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (newValue > 0) {
					form.setTotalExemptions(newValue.intValue());
				}
			}
		}
	}
	
	/**
	 * autoCalcTotalExempt
	 * 
	 * @return true if one of field is not null
	 */
	private boolean autoCalcTotalExempt() {
		if (form.getPersonalExemptions() == null && form.getSpouseExemptions() == null
				&& form.getClaimedExemptions() == null && form.getHeadOfHouseholdExemptions() == null
				&& form.getQualifiedDependents() == null && form.getBlindnessExemption() == null) {
			form.setTotalExemptions(null);
			return false;
		}

		return true;
	}

}
