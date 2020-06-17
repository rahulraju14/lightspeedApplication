package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the OH-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateOHW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateOHW4Bean.class);


	/**
	 * default constructor
	 */
	public FormStateOHW4Bean() {
		super("FormStateOHW4Bean");
	}

	public static FormStateOHW4Bean getInstance() {
		return (FormStateOHW4Bean)ServiceFinder.findBean("formStateOHW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getCompleteAddress())) {
			isValid = issueErrorMessage("Home address and Zip Code", false, "");
		}
		if (StringUtils.isEmpty(form.getSchoolDisName())) {
			isValid = issueErrorMessage("Public school district of residence", false, "");
		}
		if (StringUtils.isEmpty(form.getSchoolDisNo())) {
			isValid = issueErrorMessage("School district no.", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * Auto-fill the OH-W4 form.
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
		if (form.getAddress() != null) {
			form.setCompleteAddress(form.getAddress().getCompleteAddress());
		}
	}

	@Override
	public void setUpForm() {
		super.setUpForm();
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
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				if (newValue > 0) {
					form.setClaimedExemptions(newValue.intValue());
				}
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getClaimedExemptions() != null) {
					oldValue = form.getClaimedExemptions() - oldValue;
				}
				if (oldValue > 0) {
					form.setClaimedExemptions(oldValue);
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
				if (form.getQualifiedDependents() != null) {
					newValue = newValue + form.getQualifiedDependents();
				}
				form.setClaimedExemptions(newValue.intValue());
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getClaimedExemptions() != null) {
					oldValue = form.getClaimedExemptions() - oldValue;
				}
				form.setClaimedExemptions(oldValue);
			}
		}

	}
	/**
	 * Listener for Dependent Exemptions value changes
	 *
	 * @param event
	 */
	public void listenDependentExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() == null) {
			form.setQualifiedDependents(null);
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

				form.setClaimedExemptions(newValue.intValue());
			}
			else if (event.getOldValue() != null) {
				Integer oldValue = (Integer)event.getOldValue();
				if (form.getClaimedExemptions() != null) {
					oldValue = form.getClaimedExemptions() - oldValue;
				}
				form.setClaimedExemptions(oldValue);
			}
		}
	}

	public boolean isAllowToAddExemptions() {
		if (form.getPersonalExemptions() == null && form.getSpouseExemptions() == null &&
				form.getQualifiedDependents() == null) {
			form.setClaimedExemptions(null);
			return false;
		}
		return true;
	}
	
	@Override
	/**
	 * LS-3997
	 * There is not a specific zip code field to validate so
	 * return true;
	 */
	protected boolean isZipValid() {
		return true;
	}
	
	@Override
	/**
	 * LS-3997
	 * MD does not have a specific Address zip field to check so
	 * check is fullAddress is empty.
	 */
	public boolean checkAddressValidMsg(Address address) {
		return !StringUtils.isEmpty(form.getCompleteAddress());
	}
	
}
