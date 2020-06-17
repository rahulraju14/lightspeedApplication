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
 * Backing bean for the IA-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateIAW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateIAW4Bean.class);


	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";

	/** Marital Status whether filing single (s), married(m)  */
	private Boolean[] maritalStatus = new Boolean[2];
	/**
	 * default constructor
	 */
	public FormStateIAW4Bean() {
		super("FormStateIAW4Bean");
		resetBooleans();
	}

	public static FormStateIAW4Bean getInstance() {
		return (FormStateIAW4Bean)ServiceFinder.findBean("formStateIAW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (Form.isEmptyNA(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (form.isCheckBox1() && Form.isEmptyNA(form.getLegalStateOfRes())) {
			isValid = issueErrorMessage("Domicile State", false, "");
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
		if (!StringUtils.isEmpty(marital)) {
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
					form.setMarital("s");
					index = 0;
				}
				else if (maritalStatus[1] && newVal) {
					form.setMarital("m");
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
	
	/**
	 * Value change listener for personalAllowances radio buttons.
	 *
	 * @param evt
	 */
	public void listenPersonalAllowancesChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;

			if (evt.getNewValue() == null) {
				form.setPersonalAllowances(null);
				if (evt.getOldValue() != null && autoCalculateAllowances()) {
					Integer oldValue = (Integer) evt.getOldValue();
					if (form.getAllowances().intValue() > 0) {
						oldValue = form.getAllowances() - oldValue;
					}
					if (oldValue > 0) {
						form.setAllowances(oldValue);
					}
				}
			} 
			else {
				if (evt.getNewValue() != null) {
					newValue = (Long) evt.getNewValue();
					if (form.getDependentAllowances() != null) {
						newValue = newValue + form.getDependentAllowances();
					}
					if (form.getDeductionAllowances() != null) {
						newValue = newValue + form.getDeductionAllowances();
					}
					if (form.getAdjustmentAllowances() != null) {
						newValue = newValue + form.getAdjustmentAllowances();
					}
					if (form.getChildDependentAllowances() != null) {
						newValue = newValue + form.getChildDependentAllowances();
					}
					if (newValue > 0) {
						form.setAllowances(newValue.intValue());
					}
				} 
				
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	
	/**
	 * Value change listener for DependentAllowances radio buttons.
	 *
	 * @param evt
	 */
	public void listenDependentAllowancesChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;

			if (evt.getNewValue() == null) {
				form.setDependentAllowances(null);
				if (evt.getOldValue() != null && autoCalculateAllowances()) {
					Integer oldValue = (Integer) evt.getOldValue();
					if (form.getAllowances().intValue() > 0) {
						oldValue = form.getAllowances() - oldValue;
					}
					if (oldValue > 0) {
						form.setAllowances(oldValue);
					}
				}
			} 
			else {
				if (evt.getNewValue() != null) {
					newValue = (Long) evt.getNewValue();
					if (form.getPersonalAllowances() != null) {
						newValue = newValue + form.getPersonalAllowances();
					}
					if (form.getDeductionAllowances() != null) {
						newValue = newValue + form.getDeductionAllowances();
					}
					if (form.getAdjustmentAllowances() != null) {
						newValue = newValue + form.getAdjustmentAllowances();
					}
					if (form.getChildDependentAllowances() != null) {
						newValue = newValue + form.getChildDependentAllowances();
					}
					if (newValue > 0) {
						form.setAllowances(newValue.intValue());
					}
				} 
				
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	/**
	 * Value change listener for DeductionAllowances radio buttons.
	 *
	 * @param evt
	 */
	public void listenDeductionAllowancesChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;

			if (evt.getNewValue() == null) {
				form.setDeductionAllowances(null);
				if (evt.getOldValue() != null && autoCalculateAllowances()) {
					Integer oldValue = (Integer) evt.getOldValue();
					if (form.getAllowances().intValue() > 0) {
						oldValue = form.getAllowances() - oldValue;
					}
					if (oldValue > 0) {
						form.setAllowances(oldValue);
					}
				}
			} 
			else {
				if (evt.getNewValue() != null) {
					newValue = (Long) evt.getNewValue();
					if (form.getPersonalAllowances() != null) {
						newValue = newValue + form.getPersonalAllowances();
					}
					if (form.getDependentAllowances() != null) {
						newValue = newValue + form.getDependentAllowances();
					}
					if (form.getAdjustmentAllowances() != null) {
						newValue = newValue + form.getAdjustmentAllowances();
					}
					if (form.getChildDependentAllowances() != null) {
						newValue = newValue + form.getChildDependentAllowances();
					}
					if (newValue > 0) {
						form.setAllowances(newValue.intValue());
					}
				} 
			
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	/**
	 * Value change listener for AdjustmentAllowances radio buttons.
	 *
	 * @param evt
	 */
	public void listenAdjustmentAllowancesChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;

			if (evt.getNewValue() == null) {
				form.setAdjustmentAllowances(null);
				if (evt.getOldValue() != null && autoCalculateAllowances()) {
					Integer oldValue = (Integer) evt.getOldValue();
					if (form.getAllowances().intValue() > 0) {
						oldValue = form.getAllowances() - oldValue;
					}
					if (oldValue > 0) {
						form.setAllowances(oldValue);
					}
				}
			} 
			else {
				if (evt.getNewValue() != null) {
					newValue = (Long) evt.getNewValue();
					if (form.getDependentAllowances() != null) {
						newValue = newValue + form.getDependentAllowances();
					}
					if (form.getPersonalAllowances() != null) {
						newValue = newValue + form.getPersonalAllowances();
					}
					if (form.getDeductionAllowances() != null) {
						newValue = newValue + form.getDeductionAllowances();
					}
					
					if (form.getChildDependentAllowances() != null) {
						newValue = newValue + form.getChildDependentAllowances();
					}
					if (newValue > 0) {
						form.setAllowances(newValue.intValue());
					}
				} 
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	
	/**
	 * Value change listener for ChildDependentAllowances radio buttons.
	 *
	 * @param evt
	 */
	public void listenChildDependentAllowancesChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;

			if (evt.getNewValue() == null) {
				form.setChildDependentAllowances(null);
				if (evt.getOldValue() != null && autoCalculateAllowances()) {
					Integer oldValue = (Integer) evt.getOldValue();
					if (form.getAllowances().intValue() > 0) {
						oldValue = form.getAllowances() - oldValue;
					}
					if (oldValue > 0) {
						form.setAllowances(oldValue);
					}
				}

			}
			else {
				if (evt.getNewValue() != null) {
					newValue = (Long) evt.getNewValue();
					if (form.getPersonalAllowances() != null) {
						newValue = newValue + form.getPersonalAllowances();
					}
					if (form.getDependentAllowances() != null) {
						newValue = newValue + form.getDependentAllowances();
					}
					if (form.getDeductionAllowances() != null) {
						newValue = newValue + form.getDeductionAllowances();
					}
					if (form.getAdjustmentAllowances() != null) {
						newValue = newValue + form.getAdjustmentAllowances();
					}
					if (newValue > 0) {
						form.setAllowances(newValue.intValue());
					}
				}
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * autoCalculateAllowances
	 */
	private boolean autoCalculateAllowances() {
		if (form.getPersonalAllowances() == null && form.getDependentAllowances() == null
				&& form.getDeductionAllowances() == null && form.getAdjustmentAllowances() == null
				&& form.getChildDependentAllowances() == null) {
			form.setAllowances(null);
			return false;
		}
		return true;

	}

	/**
	 * listener for SpouseResidency checkbox value change
	 *
	 * @param evt
	 */
	public void listenSpouseResidencyChange(ValueChangeEvent evt) {
		boolean value = (boolean)evt.getNewValue();
		if (! value) {
			form.setLegalStateOfRes(null);
		}
	}

}
