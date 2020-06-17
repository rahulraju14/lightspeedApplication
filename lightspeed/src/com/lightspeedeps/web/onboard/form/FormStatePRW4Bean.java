/**
 * File: FormStatePRW4Bean.java
 */
package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.FormStateW4;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * 
 */
@ManagedBean
@ViewScoped
public class FormStatePRW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = - 2264042248176102390L;
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormStatePRW4Bean.class);
	
	// Personal exemptions radio buttons
	boolean [] maritalStatus = new boolean[5];
	// Veteran Exemptions
	boolean [] vetExemptions = new boolean[2];
	
	public static FormStatePRW4Bean getInstance() {
		return (FormStatePRW4Bean)ServiceFinder.findBean("formStatePRW4Bean");
	}
	
	/** Default constructor */
	public FormStatePRW4Bean() {
		super("FormStatePRW4Bean");
		resetBooleans();
	}

	/** See {@link #maritalStatus}. */
	public boolean[] getMaritalStatus() {
		return maritalStatus;
	}

	/** See {@link #maritalStatus}. */
	public void setMaritalStatus(boolean[] maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	/** See {@link #vetExemptions}. */
	public boolean[] getVetExemptions() {
		return vetExemptions;
	}

	/** See {@link #vetExemptions}. */
	public void setVetExemptions(boolean[] vetExemptions) {
		this.vetExemptions = vetExemptions;
	}
	
	/**
	 * Map the marital buttons to the proper constant value, then continue with
	 * normal save.
	 */
	@Override
	public String actionSave() {
		updateMaritalStatus();
		updateVeteranExemptionStatus();
		return super.actionSave();
	}
	
	public String actionCancel() {
		super.actionCancel();
		
		setUpForm();
		
		return null;
	}
	
	public void listenCalcTotalAllowances(ValueChangeEvent event) {
		Integer value = NumberUtils.safeAdd(form.getHomeMortgageInterestAllowance(), form.getCharitableContributationAllowance(),
				form.getMedicalExpensesAllowance(), form.getStudentLoanInterestAllowance(), form.getGovPensionContributionsAllowance(),
				form.getIndividRetirementAcctAllowances(), form.getEducationContributionAllowance(),
				form.getHealthSavingAcctAllowance(), form.getCasualtyResidenceLossAllowance(), form.getPersonalPropertyLossAllowance());
		form.setDeductionAmount(value);
		
	}
	
	/**
	 * Clear out the withholding amount and withhold percent fields
	 * if the Authorize Employer to withhold per pay period is unchecked
	 */
	public void listenEmployerAuthChange(ValueChangeEvent event) {
		Boolean value = (Boolean)event.getNewValue();
		
		if(value == null || value == false) {
			form.setEmployerWithholdPayPeriodAmt(null);
			form.setEmployerWithholdPayPeriodPercent(null);
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
		updateVeteranExemptionStatus();
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last NameName", false, "");	
		}
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		// LS-4188
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		
		// Validate date of birth fields
		if(!checkNegative(form.getDateOfBirthMonth(), "Date of Birth Month")) {
			isValid = false;
		}
		if(!checkNegative(form.getDateOfBirthDay(), "Date of Birth Day")) {
			isValid = false;
		}
		if(!checkNegative(form.getDateOfBirthDay(), "Date of Birth Year")) {
			isValid = false;
		}
		
		// Validate number of dependents
		if(!checkNegative(form.getQualifiedDependents(), "Complete exemption dependents")) {
			isValid = false;
		}
		if(!checkNegative(form.getJointCustodyDependents(), "Joint custody dependents")) {
			isValid = false;
		}
		
		// Validate Allowances
		if(!checkNegative(form.getHomeMortgageInterestAllowance(), "1 (a)")) {
			isValid = false;
		}
		if(!checkNegative(form.getCharitableContributationAllowance(), "1 (b)")) {
			isValid = false;
		}
		if(!checkNegative(form.getMedicalExpensesAllowance(), "1 (c)")) {
			isValid = false;
		}
		if(!checkNegative(form.getStudentLoanInterestAllowance(), "1 (d)")) {
			isValid = false;
		}		
		if(!checkNegative(form.getGovPensionContributionsAllowance(), "1 (e)")) {
			isValid = false;
		}
		if(!checkNegative(form.getIndividRetirementAcctAllowances(), "1 (f)")) {
			isValid = false;
		}		
		if(!checkNegative(form.getEducationContributionAllowance(), "1 (g)")) {
			isValid = false;
		}
		if(!checkNegative(form.getHealthSavingAcctAllowance(), "1 (h)")) {
			isValid = false;
		}		
		if(!checkNegative(form.getCasualtyResidenceLossAllowance(), "1 (i)")) {
			isValid = false;
		}
		if(!checkNegative(form.getPersonalPropertyLossAllowance(), "1 (j)")) {
			isValid = false;
		}

		if(!checkNegative(form.getTotalAllowancesAmt(), "2")) {
			isValid = false;
		}
		if(!checkNegative(form.getAllowances(), "3")) {
			isValid = false;
		}	
		
		// Validate Employer Withholding Pay Period amounts
		if(!checkNegative(form.getEmployerWithholdPayPeriodAmt(), "Employer Withholding Pay Period Amount")) {
			isValid = false;
		}
		if(!checkNegative(form.getEmployerWithholdPayPeriodPercent(), "Employer Withholding Pay Period Percentage")) {
			isValid = false;
		}			
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * Method used to check whether the address fields are empty or not in the
	 * form on action submit. This will issue error messages for any required
	 * fields that are blank.
	 *
	 * @param address The Address object whose fields are to be validated.
	 *
	 * @return True if all the fields are valid to submit the form.
	 */
	protected boolean checkAddressValidMsg(Address address) {
		boolean valid = true;
		if (address == null) {
			valid = issueErrorMessage("Address Fields", false, "");
		}
		else {
			if (StringUtils.isEmpty(form.getCompleteAddress())) {
				valid = issueErrorMessage("Address", false, "");
			}
		}
		return valid;
	}
	
	/**
	 * return true since we are not validating the zip code.
	 */
	@Override
	protected boolean isZipValid() {
		return true;
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(maritalStatus, Boolean.FALSE);
		Arrays.fill(vetExemptions, Boolean.FALSE);
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		
		// Set marital status radio buttons for W4 2020
		String marital = form.getMarital();
		if (! StringUtils.isEmpty(marital)) {
			if (marital.equals(FormStateW4.MARITAL_STATUS_SINGLE)) {
				maritalStatus[0] = true;
			}
			else if (marital.equals(FormStateW4.MARITAL_STATUS_SINGLE_NONE)) {
				maritalStatus[1] = true;
			}
			else if (marital.equals(FormStateW4.MARITAL_STATUS_MARRIED)) {
				maritalStatus[2] = true;
			}
			else if (marital.equals(FormStateW4.MARITAL_STATUS_MARRIED_SEPARATE)) {
				maritalStatus[3] = true;
			}
			else if (marital.equals(FormStateW4.MARITAL_STATUS_MARRIED_NONE)) {
				maritalStatus[4] = true;
			}
		}	
		// Setup Veteran Exemptions;
		String vetExemptionStatus = form.getVetExemptionStatus();
		if (! StringUtils.isEmpty(vetExemptionStatus)) {
			if (vetExemptionStatus.equals(FormStateW4.VET_EXEMPTION)) {
				vetExemptions[0] = true;
			}
			else if (marital.equals(FormStateW4.VET_NONE_EXEMPTION)) {
				vetExemptions[1] = true;
			}
		}
	}
	
	/**
	 * Auto-fill the State W-4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			populateForm(prompted);
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}
	
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		
		if(form.getAddress() != null) {
			form.setCompleteAddress(form.getAddress().getCompleteAddress());
		}
		if(form.getMailingAddress() != null) {
			form.setCompleteMailingAddress(form.getMailingAddress().getCompleteAddress());
		}
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
			form.setMarital(FormStateW4.MARITAL_STATUS_SINGLE_NONE);
			index = 1;
		}
		else if (maritalStatus[2]) {
			form.setMarital(FormStateW4.MARITAL_STATUS_MARRIED);
			index = 2;
		}
		else if (maritalStatus[3]) {
			form.setMarital(FormStateW4.MARITAL_STATUS_MARRIED_SEPARATE);
			index = 3;		
		}
		else if (maritalStatus[4]) {
			form.setMarital(FormStateW4.MARITAL_STATUS_MARRIED_NONE);
			index = 4;		
		}

		return index;
	}
	
	/**
	 * Update the current marital status
	 * 
	 * @return index of the marital status that was updated.
	 */
	private int updateVeteranExemptionStatus() {
		int index = -1;
		
		form.setVetExemptionStatus(null);
		if (vetExemptions[0]) {
			form.setVetExemptionStatus(FormStateW4.VET_EXEMPTION);
			index = 0;
		}
		else if (vetExemptions[1]) {
			form.setVetExemptionStatus(FormStateW4.VET_NONE_EXEMPTION);
			index = 1;
		}

		return index;
	}
}
