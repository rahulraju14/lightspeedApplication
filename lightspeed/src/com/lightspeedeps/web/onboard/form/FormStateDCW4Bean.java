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
 * Backing bean for the DC-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateDCW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateDCW4Bean.class);

	/**
	 * Marital Status whether filing single (s), married(m) or HouseHold(w) or
	 * Married/Seperated(p) or MarriedDomestic(d)
	 */
	private Boolean[] maritalStatus = new Boolean[5];

	/** Boolean array used for the selection of dcDistColumbia radio buttons. */
	private Boolean dcDistColumbia[] = new Boolean[2];

	/** Boolean array used for the selection of dcFulltimeStud radio buttons. */
	private Boolean dcFulltimeStud[] = new Boolean[2];


	/**
	 * default constructor
	 */
	public FormStateDCW4Bean() {
		super("FormStateDCW4Bean");
		resetBooleans();

	}

	public static FormStateDCW4Bean getInstance() {
		return (FormStateDCW4Bean) ServiceFinder.findBean("formStateDCW4Bean");
	}

	@Override
	public boolean checkSaveValid() {
		return true;
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (Form.isEmptyNA(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (Form.isEmptyNA(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (Form.isEmptyNA(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (dcDistColumbia[0] && Form.isEmptyNA(form.getLegalStateOfRes())) {
			isValid = issueErrorMessage("Domicile State", false, "");
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
		resetBooleans();
		// Set marital status radio buttons for DC W4 2020
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
			else if (marital.equals(MARITAL_MILITARY_SPOUSE)) {
				maritalStatus[4] = true;
			}
		}

		setUpBooleans();
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.onboard.form.FormStateW4Bean#actionSave()
	 */
	@Override
	public String actionSave() {

		form.setRadioButton1(setChosenValue(dcDistColumbia));
		form.setRadioButton2(setChosenValue(dcFulltimeStud));
		return  super.actionSave();
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
				}
				else if (maritalStatus[1] && newVal) {
					form.setMarital(MARITAL_STATUS_MARRIED);
					index = 1;
				}
				else if (maritalStatus[2] && newVal) {
					form.setMarital(MARITAL_STATUS_EXTRA_WITHOLDINGS);
					index = 2;
				}
				else if (maritalStatus[3] && newVal) {
					form.setMarital(MARITAL_STATUS_MARRIED_SEPARATE);
					index = 3;
				}
				else if (maritalStatus[4] && newVal) {
					form.setMarital(MARITAL_MILITARY_SPOUSE);
					index = 4;
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
		Arrays.fill(dcDistColumbia, Boolean.FALSE);
		Arrays.fill(dcFulltimeStud, Boolean.FALSE);
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
		setUpBooleans();
		setUpForm();
		return super.actionCancel();
	}

	/**
	 * Value change listener for dcDistColumbia radio buttons.
	 *
	 * @param evt
	 */
	public void listenDcDistColumbiaChange(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean) evt.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("dcDistColumbiaYes") && newVal) {
				dcDistColumbia[0] = true;
				dcDistColumbia[1] = false;
			}
			else if (id.equals("dcDistColumbiaNo") && newVal) {
				dcDistColumbia[0] = false;
				dcDistColumbia[1] = true;
			}
			 form.setRadioButton1(newVal);
		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value change listener for dcFulltimeStud radio buttons.
	 *
	 * @param evt
	 */
	public void listenDcFulltimeStudChange(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean) evt.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("dcFulltimeStudYes") && newVal) {
				dcFulltimeStud[0] = true;
				dcFulltimeStud[1] = false;
			}
			else if (id.equals("dcFulltimeStudNo") && newVal) {
				dcFulltimeStud[0] = false;
				dcFulltimeStud[1] = true;
			}
			form.setRadioButton2(newVal);
		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}
	/** See {@link #dcDistColumbia}. */
	public Boolean[] getDcDistColumbia() {
		return dcDistColumbia;
	}

	/** See {@link #dcDistColumbia}. */
	public void setDcDistColumbia(Boolean[] dcDistColumbia) {
		this.dcDistColumbia = dcDistColumbia;
	}

	/** See {@link #dcFulltimeStud}. */
	public Boolean[] getDcFulltimeStud() {
		return dcFulltimeStud;
	}

	/** See {@link #dcFulltimeStud}. */
	public void setDcFulltimeStud(Boolean[] dcFulltimeStud) {
		this.dcFulltimeStud = dcFulltimeStud;
	}


	/**
	 * Value change listener for dcSectionA radio buttons.
	 *
	 * @param evt
	 */
	public void listenSectionAChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;
			if (evt.getNewValue() != null) {
				newValue = (Long) evt.getNewValue();
				if (form.getAllowancesSectionB() != null) {
					newValue = newValue + form.getAllowancesSectionB();
				}
				if (newValue > 0) {
					form.setAllowancesSectionTotal(newValue.intValue());
				}
			}
			else if (evt.getOldValue() != null) {
				Integer oldValue = (Integer) evt.getOldValue();
				if (form.getAllowancesSectionTotal() != null) {
					oldValue = form.getAllowancesSectionTotal() - oldValue;
				}
				if (oldValue > 0) {
					form.setAllowancesSectionTotal(oldValue);
				}
			}
			if (newValue == null && form.getAllowancesSectionB() == null) {
				form.setAllowancesSectionTotal(null);
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value change listener for dcSectionB radio buttons.
	 *
	 * @param evt
	 */
	public void listenSectionBChange(ValueChangeEvent evt) {
		try {
			Long newValue = null;
			if (evt.getNewValue() != null) {
				newValue = (Long) evt.getNewValue();
				if (form.getAllowancesSectionA() != null) {
					newValue = newValue + form.getAllowancesSectionA();
				}
				if (newValue > 0) {
					form.setAllowancesSectionTotal(newValue.intValue());
				}
			}
			else if (evt.getOldValue() != null) {
				Integer oldValue = (Integer) evt.getOldValue();
				if (form.getAllowancesSectionTotal() != null) {
					oldValue = form.getAllowancesSectionTotal() - oldValue;
				}
				if (oldValue > 0) {
					form.setAllowancesSectionTotal(oldValue);
				}
			}
			if (form.getAllowancesSectionA() == null && newValue == null) {
				form.setAllowancesSectionTotal(null);
			}

		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}


	/**
	 * Sets the value of boolean fields chosen from their given radio buttons on
	 * the form.
	 *
	 * @param radioButton Boolean Array used for the field.
	 * @return
	 */
	private Boolean setChosenValue(Boolean[] radioButton) {
		if (radioButton != null) {
			if (radioButton[0]) {
				return true;
			}
			else if (radioButton[1]) {
				return false;
			}
		}
		// If none of them is selected.
		return null;
	}

	/**
	 * Utility method used to set the value of radio button for the form field.
	 *
	 * @param booleanArray Array of Boolean used for the Form field.
	 * @param formField
	 */
	private void fillArray(Boolean[] booleanArray, Boolean formField) {
		if (formField == null) {
			booleanArray[0] = false;
			booleanArray[1] = false;
		}
		else if (formField) {
			booleanArray[0] = true;
			booleanArray[1] = false;
		}
		else if (! formField) {
			booleanArray[0] = false;
			booleanArray[1] = true;
		}
	}

	/** Fetches the stored values of the radio buttons for the form. */
	private void setUpBooleans() {
		fillArray(dcDistColumbia, form.isRadioButton1());
		fillArray(dcFulltimeStud, form.isRadioButton2());
	}


}
