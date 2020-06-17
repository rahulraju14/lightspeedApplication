package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.*;

import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the NY-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see FormStateW4Bean
 */
@ManagedBean
@ViewScoped
public class FormStateNYW4Bean extends FormStateW4Bean {


	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateNYW4Bean.class);

	// Marital Statuses
	private static final String MARITAL_STATUS_SINGLE = "s";
	private static final String MARITAL_STATUS_MARRIED = "m";
	private static final String MARITAL_STATUS_EXTRA_WITHOLDINGS = "w";

	/** Marital Status whether filing single (s), married(m) or married with extra withholding */
	private Boolean maritalStatus[] = new Boolean[3];

	/** Boolean array used for the selection of nyc resident radio buttons. */
	private Boolean nycResident[] = new Boolean[2];

	/** Boolean array used for the selection of yonkers resident radio buttons. */
	private Boolean yonkersResident[] = new Boolean[2];

	/** Boolean array used for the selection of dependent health insurance radio buttons. */
	private Boolean depHealthIns[] = new Boolean[2];

	/**
	 * default constructor
	 */
	public FormStateNYW4Bean() {
		super("FormStateNYW4Bean");
		resetBooleans();
	}

	public static FormStateNYW4Bean getInstance() {
		return (FormStateNYW4Bean)ServiceFinder.findBean("formStateNYW4Bean");
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		super.rowClicked(contactDocument);
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();
		if (form.getAddress() != null) {
			// Zip code validation for 5 digits
			if (! form.getAddress().isZipValid()) {
				MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
				isValid = false;
			}
		}
		setSaveValid(isValid);
		return isValid;
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getFirstAndInitial())) {
			isValid = issueErrorMessage("First Name and Middle Initial", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}

		if (form.getNycResident() == null) {
			isValid = issueErrorMessage("NYC Resident selection", false, "");
		}

		if (form.getYonkersResident() == null) {
			isValid = issueErrorMessage("Yonkers Resident selection", false, "");
		}

		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/** Fetches the stored values of the radio buttons for the form. */
	private void setUpBooleans() {
		fillArray(nycResident, form.getNycResident());
		fillArray(yonkersResident, form.getYonkersResident());
		fillArray(depHealthIns, form.getDependentHealthInsurance());
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

	@Override
	public String actionSave() {
		try {
			form.setNycResident(setChosenValue(nycResident));
			form.setYonkersResident(setChosenValue(yonkersResident));
			form.setDependentHealthInsurance(setChosenValue(depHealthIns));
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	@Override
	public String actionCancel() {
		setUpBooleans();
		setUpForm();
		return super.actionCancel();
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
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(nycResident, Boolean.FALSE);
		Arrays.fill(yonkersResident, Boolean.FALSE);
		Arrays.fill(depHealthIns, Boolean.FALSE);
		Arrays.fill(maritalStatus, Boolean.FALSE);
	}

	@Override
	protected void setUpForm() {
		super.setUpForm();
		if (form.getMarital() != null) {
			if (! StringUtils.isEmpty(form.getMarital())) {
				resetBooleans();
				if (form.getMarital().equals(MARITAL_STATUS_SINGLE)) {
					maritalStatus[0] = true;
				}
				else if (form.getMarital().equals(MARITAL_STATUS_MARRIED)) {
					maritalStatus[1] = true;
				}
				else if (form.getMarital().equals(MARITAL_STATUS_EXTRA_WITHOLDINGS)) {
					maritalStatus[2] = true;
				}
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
	 * Value change listener for NYC Resident radio buttons.
	 *
	 * @param evt
	 */
	public void listenNycResidentChange(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean)evt.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("nyResidentYes") && newVal) {
				nycResident[0] = true;
				nycResident[1] = false;
			}
			else if (id.equals("nyResidentNo") && newVal) {
				nycResident[0] = false;
				nycResident[1] = true;
			}
			form.setNycResident(newVal);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value change listener for Yonkers Resident radio buttons.
	 *
	 * @param evt
	 */
	public void listenYonkersResidentChange(ValueChangeEvent event) {
		try {
			refreshRadioButtons(event);
			String id = event.getComponent().getId();
			Boolean newVal = (Boolean)event.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("yonkersResidentYes") && newVal) {
				yonkersResident[0] = true;
				yonkersResident[1] = false;
			}
			else if (id.equals("yonkersResidentNo") && newVal) {
				yonkersResident[0] = false;
				yonkersResident[1] = true;
			}
			form.setYonkersResident(newVal);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Value change listener for dependent health insurance radio buttons.
	 *
	 * @param evt
	 */
	public void listenDepHealthInsChange(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			String id = evt.getComponent().getId();
			Boolean newVal = (Boolean)evt.getNewValue();
			LOG.debug("Id = " + id);
			LOG.debug("Value = " + newVal);
			if (id.equals("depHealthInsYes") && newVal) {
				depHealthIns[0] = true;
				depHealthIns[1] = false;
			}
			else if (id.equals("depHealthInsNo") && newVal) {
				depHealthIns[0] = false;
				depHealthIns[1] = true;
			}
			form.setDependentHealthInsurance(newVal);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** See {@link #nycResident}. */
	public Boolean[] getNycResident() {
		return nycResident;
	}

	/** See {@link #nycResident}. */
	public void setNycResident(Boolean[] nycResident) {
		this.nycResident = nycResident;
	}

	/** See {@link #yonkersResident}. */
	public Boolean[] getYonkersResident() {
		return yonkersResident;
	}

	/** See {@link #yonkersResident}. */
	public void setYonkersResident(Boolean[] yonkersResident) {
		this.yonkersResident = yonkersResident;
	}

	/** See {@link #depHealthIns}. */
	public Boolean[] getDepHealthIns() {
		return depHealthIns;
	}

	/** See {@link #depHealthIns}. */
	public void setDepHealthIns(Boolean[] depHealthIns) {
		this.depHealthIns = depHealthIns;
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
