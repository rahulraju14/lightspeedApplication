/**
 * File: FormStateWVW4Bean.java
 */
package com.lightspeedeps.web.onboard.form;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.FormStateW4;
import com.lightspeedeps.model.User;
import com.lightspeedeps.service.FormService;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Managed bean for WV W-4 form
 */
@ManagedBean
@ViewScoped
public class FormStateWVW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 5862731599702114928L;
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormStateWVW4Bean.class);

	public static FormStateWVW4Bean getInstance() {
		return (FormStateWVW4Bean)ServiceFinder.findBean("formStateWVW4Bean");
	}

	public FormStateWVW4Bean() {
		super("FormStateWVW4Bean");
	}

	/**
	 * Map the marital buttons to the proper constant value, then continue with
	 * normal save.
	 */
	@Override
	public String actionSave() {
		return super.actionSave();
	}

	public String actionCancel() {
		super.actionCancel();

		setUpForm();

		return null;
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
	public void setUpForm() {
		super.setUpForm();

	}

	public void listenCalcTotalExemptions(ValueChangeEvent event) {
		Integer value = NumberUtils.safeAdd(form.getPersonalExemptions(),
				form.getMarriedExemptions(), form.getQualifiedDependents());
		form.setTotalExemptions(value);
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();

		setSubmitValid(isValid);
		return isValid;
	}

	@Override
	protected boolean isZipValid() {
		boolean isValid = super.isZipValid();
		// Zip code validation for 5 digits
		if (! StringUtils.isEmpty(form.getNonResidentAddress().getState())) {
			if (! form.getNonResidentAddress().isZipValid()) {
				MsgUtils.addFacesMessage("Form.NonResidentAddress.ZipCode", FacesMessage.SEVERITY_ERROR);
				isValid = false;
			}
		}
		else {
			isValid = issueErrorMessage("State", false, "");
		}

		return isValid;
	}

	/**
	 * Method used to check the validity of fields in the form.
	 *
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();

		if (isValid) {
			// Show the first page signature
			form.setShowSecondSignature(false);
			FormService.getInstance().update(form, PayrollFormType.CA_W4.getApiUpdateUrl(),
					FormStateW4.class);
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * LS-4240 Method used to check the validity of fields in the form.
	 *
	 * @return isValid
	 */
	@Override
	public boolean checkSecondSubmitValid() {
		boolean isValid = true;

		if (StringUtils.isEmpty(form.getNonResidentFullName())) {
			isValid = issueErrorMessage("Non-resident Name", false, "");
		}
		if (StringUtils.isEmpty(form.getNonResidentSocialSecurity())) {
			isValid = issueErrorMessage("Non-resident Social Security Number", false, "");
		}
		if (StringUtils.isEmpty(form.getLegalStateOfRes())) {
			isValid = issueErrorMessage("Legal State of Residence field 1", false, "");
		}
		if (StringUtils.isEmpty(form.getCertifiedLegalStateOfRes())) {
			isValid = issueErrorMessage("Legal State of Residence field 2", false, "");
		}
		isValid &= checkSecondaryAddressValidMsg(form.getNonResidentAddress());

		if (isValid) {
			// Show the second page signature
			form.setShowSecondSignature(true);
			FormService.getInstance().update(form, PayrollFormType.CA_W4.getApiUpdateUrl(),
					FormStateW4.class);
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
	@Override
	protected boolean checkSecondaryAddressValidMsg(Address secondaryAddress) {
		boolean valid = true;
		if (secondaryAddress == null) {
			valid = issueErrorMessage("Non-Resident Address Fields", false, "");
		}
		else {
			if (StringUtils.isEmpty(secondaryAddress.getAddrLine1())) {
				valid = issueErrorMessage("Non-Resident Address", false, "");
			}
			if (StringUtils.isEmpty(secondaryAddress.getCity())) {
				valid = issueErrorMessage("Non-Resident City", false, "");
			}
			if (StringUtils.isEmpty(secondaryAddress.getState())) {
				valid = issueErrorMessage("Non-Resident State", false, "");
			}
			if (StringUtils.isEmpty(secondaryAddress.getZip())) {
				valid = issueErrorMessage("Non-Resident Zip Code", false, "");
			}
		}
		return valid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		super.populateForm(prompted);
		User user = getContactDoc().getContact().getUser();
		user = getUserDAO().refresh(user);

		form.setNonResidentFullName(user.getFullName());

		form.setNonResidentSocialSecurity(user.getSocialSecurity());
		form.setDateofBirth(user.getBirthdate());
		form.setNonResidentSocialSecurity(user.getSocialSecurity());
		if (user.getHomeAddress() != null) {
			form.getNonResidentAddress().copyFrom(user.getHomeAddress());
			form.getNonResidentAddress().setAddrLine1(user.getHomeAddress().getAddrLine1Line2());
		}
		else {
			// LS-3555 Clear out the fields if the user has not yet gone to
			// the My Account page to fill in the home address
			form.getNonResidentAddress().clear();
		}
		form.getAddress().setAddrLine1(user.getHomeAddress().getAddrLine1Line2());
	}
}
