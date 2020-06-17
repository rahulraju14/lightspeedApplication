package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the AR-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateARW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = - 5646725257387640744L;
	private static final Log LOG = LogFactory.getLog(FormStateARW4Bean.class);

	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
	private static final String MARITAL_STATUS_HEAD_OF_HOUSEHOLD = "h";

	/** Marital Status whether filing single (s), married(m) or head of household */
	private Boolean[] maritalStatus = new Boolean[3];

	/** Boolean array used for the selection of low income rate yes or no. */
	private Boolean lowIncomeTax[] = new Boolean[2];

	/**
	 * default constructor
	 */
	public FormStateARW4Bean() {
		super("FormStateARW4Bean");
		resetBooleans();

	}

	public static FormStateARW4Bean getInstance() {
		return (FormStateARW4Bean)ServiceFinder.findBean("formStateARW4Bean");
	}

	@Override
	public String actionCancel() {
		setUpBooleans();
		setUpForm();
		return super.actionCancel();
	}

	@Override
	public String actionSave() {
		try {
			form.setLowIncomeTax(setChosenValue(lowIncomeTax));
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
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
	 * Auto-fill the State NC- W-4 form.
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
			else if (marital.equals(MARITAL_STATUS_HEAD_OF_HOUSEHOLD)) {
				maritalStatus[2] = true;
			}
		}
		setUpBooleans();
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
					form.setMarital("h");
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
	 * Value change listener for personal exemption selection.
	 *
	 * @param evt
	 */
	public void listenFirstExmeptChange(ValueChangeEvent evt) {
		boolean newValue = (boolean)evt.getNewValue();
		if (newValue) {
			form.setExemptStatus1(true);
			form.setExemptStatus2(false);
			form.setExemptStatus3(false);
			form.setSpouseExemptions(null);
			form.setClaimedExemptions(null);
		}
		else {
			form.setExemptStatus1(false);
			form.setPersonalExemptions(null);
		}
		if (form.getQualifiedDependents() != null) {
			form.setTotalExemptions(form.getQualifiedDependents());
		}
		else {
			form.setTotalExemptions(null);
		}
	}

	/**
	 * Value change listener for Spouse exemption selection.
	 *
	 * @param evt
	 */
	public void listenSecondExmeptChange(ValueChangeEvent evt) {
		boolean newValue = (boolean)evt.getNewValue();
		if (newValue) {
			form.setExemptStatus2(true);
			form.setExemptStatus1(false);
			form.setExemptStatus3(false);
			form.setPersonalExemptions(null);
			form.setClaimedExemptions(null);
		}
		else {
			form.setExemptStatus2(false);
			form.setSpouseExemptions(null);
		}
		if (form.getQualifiedDependents() != null) {
			form.setTotalExemptions(form.getQualifiedDependents());
		}
		else {
			form.setTotalExemptions(null);
		}
	}

	/**
	 * Value change listener for head of household with personal exemption
	 * selection
	 *
	 * @param evt
	 */
	public void listenThirdExmeptChange(ValueChangeEvent evt) {
		boolean newValue = (boolean)evt.getNewValue();
		if (newValue) {
			form.setExemptStatus3(true);
			form.setExemptStatus1(false);
			form.setExemptStatus2(false);
			form.setPersonalExemptions(null);
			form.setSpouseExemptions(null);
		}
		else {
			form.setExemptStatus3(false);
			form.setClaimedExemptions(null);
		}
		if (form.getQualifiedDependents() != null) {
			form.setTotalExemptions(form.getQualifiedDependents());
		}
		else {
			form.setTotalExemptions(null);
		}
	}

	/**
	 * Listener for Personal Exemptions value changes
	 *
	 * @param event
	 */
	public void listenPersonalExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() != null) {
			newValue = (Long)event.getNewValue();
			if (form.getQualifiedDependents() != null) {
				newValue = newValue + form.getQualifiedDependents();
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

	/**
	 * Listener for Spouse Exemptions value changes
	 *
	 * @param event
	 */
	public void listenSpouseExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() != null) {
			newValue = (Long)event.getNewValue();
			if (form.getQualifiedDependents() != null) {
				newValue = newValue + form.getQualifiedDependents();
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

	/**
	 * Listener for Claimed Exemptions value changes
	 *
	 * @param event
	 */
	public void listenClaimedExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() != null) {
			newValue = (Long)event.getNewValue();
			if (form.getQualifiedDependents() != null) {
				newValue = newValue + form.getQualifiedDependents();
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

	/**
	 * Listener for Qualified Dependent Exemptions value changes
	 *
	 * @param event
	 */
	public void listenDependentExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() != null) {
			newValue = (Long)event.getNewValue();
			if (form.getPersonalExemptions() != null) {
				newValue = newValue + form.getPersonalExemptions();
			}
			if (form.getSpouseExemptions() != null) {
				newValue = newValue + form.getSpouseExemptions();
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


	/**
	 * Value change listener for low income tax rate radio buttons.
	 *
	 * @param evt
	 */
	public void listenLowIncomeTaxChange(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean)evt.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("lowIncomeTaxYes") && newVal) {
				lowIncomeTax[0] = true;
				lowIncomeTax[1] = false;
			}
			else if (id.equals("lowIncomeTaxNo") && newVal) {
				lowIncomeTax[0] = false;
				lowIncomeTax[1] = true;
			}
			form.setLowIncomeTax(newVal);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (! form.getExemptStatus1() && ! form.getExemptStatus2() && ! form.getExemptStatus3()) {
			MsgUtils.addFacesMessage("Form.Exemptions.Claimed.Validation",
					FacesMessage.SEVERITY_ERROR);
			isValid = false;
		}
		if (form.getExemptStatus1() && form.getPersonalExemptions() == null) {
			isValid = issueErrorMessage("Exemption 1a", false, "");
		}
		if (form.getExemptStatus2() && form.getSpouseExemptions() == null) {
			isValid = issueErrorMessage("Exemption 1b", false, "");
		}
		if (form.getExemptStatus3() && form.getClaimedExemptions() == null) {
			isValid = issueErrorMessage("Exemption 1c", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/** Fetches the stored values of the radio buttons for the form. */
	private void setUpBooleans() {
		fillArray(lowIncomeTax, form.getLowIncomeTax());
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

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(maritalStatus, Boolean.FALSE);
		Arrays.fill(lowIncomeTax, Boolean.FALSE);
	}

	/** See {@link #maritalStatus}. */
	public Boolean[] getMaritalStatus() {
		return maritalStatus;
	}

	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(Boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	/** See {@link #lowIncomeTax}. */
	public Boolean[] getLowIncomeTax() {
		return lowIncomeTax;
	}

	/** See {@link #lowIncomeTax}. */
	public void setLowIncomeTax(Boolean[] lowIncomeTax) {
		this.lowIncomeTax = lowIncomeTax;
	}

}


