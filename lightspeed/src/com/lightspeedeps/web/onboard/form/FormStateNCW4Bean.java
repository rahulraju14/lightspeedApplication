package com.lightspeedeps.web.onboard.form;

import java.util.Arrays;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.CountryDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Country;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the NC-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateNCW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateNCW4Bean.class);

	/** Marital Status whether filing single (s), married(m) or married */
	private Boolean[] maritalStatus = new Boolean[3];
	List<Country> countryList;


	/**
	 * default constructor
	 */
	public FormStateNCW4Bean() {
		super("FormStateNCW4Bean");
		resetBooleans();

	}

	public static FormStateNCW4Bean getInstance() {
		return (FormStateNCW4Bean)ServiceFinder.findBean("formStateNCW4Bean");
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

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (StringUtils.isEmpty(form.getMarital())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		if (form.getAllowances() == null) {
			isValid = issueErrorMessage("Allowances", false, "");
		}
		if (form.getAddress() != null) {
			if (StringUtils.isEmpty(form.getAddress().getCounty())) {
				isValid = issueErrorMessage("County", false, "");
			}
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
			//set country name if not US
			countryList = getCountryList();
			if (countryList != null) {
				String countryName = countryList.get(0).getName();
				if (countryName.length() > 12) {
					countryName = countryList.get(0).getName().substring(0, 12);
				}
				form.setCountryName(countryName);
			}
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
		// Set marital status radio buttons for OR W4 2020
		resetBooleans();
		String marital = form.getMarital();
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
				else if (maritalStatus[2] && newVal) {
					form.setMarital(MARITAL_STATUS_EXTRA_WITHOLDINGS);
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

	public List<Country> getCountryList() {
		String country = form.getAddress().getCountry();
		if (countryList == null && country != null &&
				! country.equals(Constants.DEFAULT_COUNTRY_CODE)) {
			countryList = CountryDAO.getInstance().findByIsoCode(country);
		}
		else {
			form.setCountryName(null);
		}
		return countryList;
	}

}
