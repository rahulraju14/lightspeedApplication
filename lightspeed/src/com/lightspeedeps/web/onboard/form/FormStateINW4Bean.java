package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the IN-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateINW4Bean extends FormStateW4Bean {

	/** serialization constant */
	private static final long serialVersionUID = 3381854672974017657L;
	private static final Log LOG = LogFactory.getLog(FormStateINW4Bean.class);
	/**
	 * default constructor
	 */
	public FormStateINW4Bean() {
		super("FormStateINW4Bean");
	}

	public static FormStateINW4Bean getInstance() {
		return (FormStateINW4Bean)ServiceFinder.findBean("formStateINW4Bean");
	}

	@Override
	protected void setUpForm() {
		super.setUpForm();
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();
		setSaveValid(isValid);
		return isValid;
	}

	/**
	 * Auto-fill the IN-W4 form.
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
		if (StringUtils.isEmpty(form.getCountyOfResidence())) {
			isValid = issueErrorMessage("Indiana County of Residence", false, "");
		}
		if (StringUtils.isEmpty(form.getCountyOfPrincipal())) {
			isValid = issueErrorMessage("Indiana County of Principal Employment", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
			form.getAddress().setAddrLine1(
					form.getAddress().getAddrLine1() + " " + form.getAddress().getAddrLine2());
			form.getAddress().setAddrLine2(null);
		}
	}

	/**
	 * listener for check box value change according to if employee is 65 or
	 * older or blind or spouse is 65 or older or blind not
	 *
	 * @param evt
	 */
	public void listenChkBoxChange(ValueChangeEvent evt) {
		Boolean newValue = (Boolean)evt.getNewValue();
		Boolean oldValue = (Boolean)evt.getOldValue();
		if (newValue) {
			if (form.getTotalBoxesChecked() != null) {
				form.setTotalBoxesChecked(form.getTotalBoxesChecked() + 1);
			}
			else {
				form.setTotalBoxesChecked(1);
			}
			if (form.getTotalExemptions() != null) {
				form.setTotalExemptions(form.getTotalExemptions() + 1);
			}
			else {
				form.setTotalExemptions(1);
			}
		}
		else if (oldValue && ! newValue) {
			if (form.getTotalBoxesChecked() != null) {
				form.setTotalBoxesChecked(form.getTotalBoxesChecked() - 1);
			}
			if (form.getTotalExemptions() != null) {
				form.setTotalExemptions(form.getTotalExemptions() - 1);
			}
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
			if (form.getSpouseExemptions() != null) {
				newValue = newValue + form.getSpouseExemptions();
			}
			if (form.getClaimedExemptions() != null) {
				newValue = newValue + form.getClaimedExemptions();
			}
			if (form.getTotalBoxesChecked() != null) {
				newValue = newValue + form.getTotalBoxesChecked();
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
			if (form.getPersonalExemptions() != null) {
				newValue = newValue + form.getPersonalExemptions();
			}
			if (form.getClaimedExemptions() != null) {
				newValue = newValue + form.getClaimedExemptions();
			}
			if (form.getTotalBoxesChecked() != null) {
				newValue = newValue + form.getTotalBoxesChecked();
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
	 * Listener for Dependent Exemptions value changes
	 *
	 * @param event
	 */
	public void listenClaimedExemptChange(ValueChangeEvent event) {
		Long newValue = null;
		if (event.getNewValue() != null) {
			newValue = (Long)event.getNewValue();
			if (form.getPersonalExemptions() != null) {
				newValue = newValue + form.getPersonalExemptions();
			}
			if (form.getSpouseExemptions() != null) {
				newValue = newValue + form.getSpouseExemptions();
			}
			if (form.getTotalBoxesChecked() != null) {
				newValue = newValue + form.getTotalBoxesChecked();
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