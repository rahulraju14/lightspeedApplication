package com.lightspeedeps.web.onboard.form;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the KS-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateKSW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateKSW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateKSW4Bean() {
		super("FormStateKSW4Bean");
	}

	public static FormStateKSW4Bean getInstance() {
		return (FormStateKSW4Bean) ServiceFinder.findBean("formStateKSW4Bean");
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
		if (!form.isSingle() && !form.isUnionCoupleJoint()) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.CheckAllowanceRate", FacesMessage.SEVERITY_ERROR);
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
			form.getAddress().setAddrLine1(form.getAddress().getAddrLine1Line2());
			form.getAddress().setAddrLine2(null);
		}
		form.setCompleteAddress(form.getAddress().getCityStateZip());
	}

	// Add listner
	public void listenSingleMartialStatus(ValueChangeEvent event) {
		form.setSingle(false);
		form.setUnionCoupleJoint(true);
	}

	public void listenMarriedStatus(ValueChangeEvent event) {
		form.setSingle(true);
		form.setUnionCoupleJoint(false);
	}
	
	@Override
	/**
	 * KS does not have a specific Address zip field to check so
	 * check is fullAddress is empty.
	 */
	public boolean checkAddressValidMsg(Address address) {
		boolean isValid= true;
		if (address != null) {
			if (StringUtils.isEmpty(address.getAddrLine1Line2())) {
				isValid = issueErrorMessage("Address", false, "");
			}
		} else {
			isValid = issueErrorMessage("Address", false, "");
		}
		return isValid;
	} 	
}
